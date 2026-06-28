import asyncio
import unittest
from unittest.mock import patch

import pandas as pd
from fastapi import HTTPException


def _make_daily_series(values):
    dates = pd.date_range(end=pd.Timestamp.now().normalize(), periods=len(values), freq="D")
    return pd.DataFrame(
        {
            "action_date": dates,
            "total_quantity": values,
        }
    )


class DataServiceContractsTest(unittest.TestCase):
    def test_get_all_parts_includes_trace_only_part_with_zero_stock(self):
        from services.data_service import DataService

        captured = {}

        class FakeDb:
            def query_dataframe(self, sql, params=None):
                captured["sql"] = sql
                return pd.DataFrame(
                    [
                        {
                            "partCode": "0001",
                            "partName": "零件A",
                            "currentStock": 0,
                        }
                    ]
                )

        with patch("services.data_service.db", FakeDb()):
            result = DataService().get_all_parts()

        self.assertEqual(
            result,
            [{"partCode": "0001", "partName": "零件A", "currentStock": 0}],
        )
        self.assertIn("inventory_trace", captured["sql"])
        self.assertNotIn("stock.current_stock IS NOT NULL", captured["sql"])

    def test_inventory_summary_includes_trace_only_part_with_zero_stock(self):
        from services.data_service import DataService

        captured = {}

        class FakeDb:
            def query_dataframe(self, sql, params=None):
                captured["sql"] = sql
                return pd.DataFrame(
                    [
                        {
                            "part_code": "0001",
                            "part_name": "零件A",
                            "total_stock": 0,
                            "kanban_count": 0,
                            "warehouse_count": 0,
                        }
                    ]
                )

        with patch("services.data_service.db", FakeDb()):
            result = DataService().get_part_inventory_summary()

        self.assertEqual(len(result), 1)
        self.assertEqual(result.iloc[0]["total_stock"], 0)
        self.assertIn("inventory_trace", captured["sql"])

    def test_inventory_summary_includes_current_inventory_without_kanban_master_row(self):
        from services.data_service import DataService

        captured = {}

        class FakeDb:
            def query_dataframe(self, sql, params=None):
                captured["sql"] = sql
                return pd.DataFrame(
                    [
                        {
                            "part_code": "0001",
                            "part_name": "闆朵欢A",
                            "total_stock": 3,
                            "kanban_count": 3,
                            "warehouse_count": 1,
                        }
                    ]
                )

        with patch("services.data_service.db", FakeDb()):
            result = DataService().get_part_inventory_summary()

        self.assertEqual(result.iloc[0]["total_stock"], 3)
        self.assertIn("LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no", captured["sql"])
        self.assertIn("(k.kanban_no IS NULL OR k.is_sealed = 0)", captured["sql"])


class AppContractsTest(unittest.TestCase):
    def test_parts_endpoint_accepts_list_result_from_data_service(self):
        import app as ai_app

        class FakeDataService:
            def get_all_parts(self):
                return [{"partCode": "0001", "partName": "零件A", "currentStock": 0}]

        async def run_endpoint():
            with patch.object(ai_app, "data_service", FakeDataService()):
                endpoint = next(
                    route.endpoint
                    for route in ai_app.app.routes
                    if getattr(route, "path", None) == "/api/parts"
                )
                return await endpoint()

        try:
            result = asyncio.run(run_endpoint())
        except HTTPException as exc:
            self.fail(f"/api/parts should not raise HTTPException: {exc.detail}")

        self.assertEqual(result["code"], 200)
        self.assertEqual(result["data"][0]["partCode"], "0001")


class WarningContractsTest(unittest.TestCase):
    def test_zero_stock_part_with_recent_outbound_has_shortage_warning(self):
        from services.warning_service import WarningService

        class FakeDataService:
            def get_part_inventory_summary(self):
                return pd.DataFrame(
                    [
                        {
                            "part_code": "0001",
                            "part_name": "零件A",
                            "total_stock": 0,
                            "kanban_count": 0,
                            "warehouse_count": 0,
                        }
                    ]
                )

            def get_part_history(self, part_code, days=7):
                return pd.DataFrame(
                    [
                        {
                            "action_date": pd.Timestamp.now().normalize(),
                            "action_type": "OUTBOUND",
                            "total_quantity": 5,
                            "transaction_count": 1,
                        }
                    ]
                )

        with patch("services.warning_service.data_service", FakeDataService()):
            warnings = WarningService().generate_all_warnings()

        self.assertEqual(len(warnings), 1)
        self.assertEqual(warnings[0]["riskType"], "SHORTAGE")
        self.assertEqual(warnings[0]["riskLevel"], "HIGH")
        self.assertGreater(warnings[0]["predictedDemand7d"], 0)


class AnomalyDetectorContractsTest(unittest.TestCase):
    def test_single_late_spike_is_not_volatility_anomaly(self):
        from models.anomaly_detector import AnomalyDetector

        dates = pd.date_range(end=pd.Timestamp.now().normalize(), periods=7, freq="D")
        historical_data = pd.DataFrame(
            {
                "action_date": dates,
                "total_quantity": [5, 5, 5, 5, 5, 5, 30],
            }
        )

        result = AnomalyDetector.detect_volatility_anomaly(historical_data)

        self.assertFalse(result["is_anomaly"])

    def test_alternating_high_low_series_is_volatility_anomaly(self):
        from models.anomaly_detector import AnomalyDetector

        dates = pd.date_range(end=pd.Timestamp.now().normalize(), periods=7, freq="D")
        historical_data = pd.DataFrame(
            {
                "action_date": dates,
                "total_quantity": [5, 22, 4, 24, 5, 20, 6],
            }
        )

        result = AnomalyDetector.detect_volatility_anomaly(historical_data)

        self.assertTrue(result["is_anomaly"])
        self.assertEqual(result["anomaly_type"], "VOLATILITY_SWING")


class PredictorContractsTest(unittest.TestCase):
    @staticmethod
    def _predict(values):
        from models.predictor import EnsemblePredictor

        historical_data = _make_daily_series(values)
        predictor = EnsemblePredictor()
        predictor.train(historical_data)
        return predictor.predict(historical_data, predict_days=7)["ensemble"]

    @staticmethod
    def _direction_changes(values):
        deltas = [
            current - previous
            for previous, current in zip(values, values[1:])
            if abs(current - previous) > 1e-9
        ]
        signs = [1 if delta > 0 else -1 for delta in deltas]
        return sum(
            1
            for previous, current in zip(signs, signs[1:])
            if previous != current
        )

    def test_rising_history_predicts_rising_demand(self):
        predictions = self._predict([4, 6, 8, 10, 12, 14, 16])

        self.assertGreaterEqual(predictions[-1] - predictions[0], 6)
        self.assertGreaterEqual(self._direction_changes(predictions), 0)

    def test_falling_history_predicts_falling_demand(self):
        predictions = self._predict([18, 16, 14, 12, 10, 8, 6])

        self.assertLessEqual(predictions[-1] - predictions[0], -4)

    def test_fluctuating_history_predicts_fluctuating_demand(self):
        predictions = self._predict([5, 22, 4, 24, 5, 20, 6])

        self.assertGreaterEqual(self._direction_changes(predictions), 3)
        self.assertGreaterEqual(max(predictions) - min(predictions), 10)


if __name__ == "__main__":
    unittest.main()
