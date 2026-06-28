import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd


class RecordingPredictor:
    def __init__(self):
        self.trained_series = []

    def train(self, historical_data):
        values = [float(value) for value in historical_data["total_quantity"].tolist()]
        self.trained_series.append(values)
        return {"moving_avg": 0.1, "linear_reg": 0.2, "random_forest": 0.3}

    def predict(self, historical_data, predict_days=7):
        last_value = float(historical_data["total_quantity"].iloc[-1])
        return {
            "ensemble": np.array([last_value] * predict_days),
            "confidence": 0.91,
        }


class TrendPredictionModesTest(unittest.TestCase):
    def test_trend_response_only_forecasts_outbound_demand(self):
        from services.trend_service import TrendService

        today = pd.Timestamp.now().normalize()
        rows = []
        for day_offset in range(7):
            action_date = today - pd.Timedelta(days=6 - day_offset)
            rows.extend(
                [
                    {
                        "action_date": action_date,
                        "action_type": "INBOUND",
                        "total_quantity": 10 + day_offset,
                    },
                    {
                        "action_date": action_date,
                        "action_type": "OUTBOUND",
                        "total_quantity": 4 + day_offset,
                    },
                ]
            )

        class FakeDataService:
            def get_part_history(self, part_code, days=7):
                return pd.DataFrame(rows)

        class FakeDb:
            def query_dataframe(self, sql, params=None):
                return pd.DataFrame([{"current_stock": 100}])

        predictor = RecordingPredictor()
        service = TrendService()
        service.predictor = predictor

        with patch("services.trend_service.data_service", FakeDataService()), patch("utils.db_helper.db", FakeDb()):
            result = service.generate_prediction_trend("0001", predict_days=2)

        self.assertTrue(result["success"])
        self.assertEqual(result["predictionMeta"]["label"], "AI需求预测（出库量）")
        self.assertNotIn("predictions", result)
        self.assertNotIn("totalFlow", result["historical"][-1])
        self.assertEqual(result["predicted"][0]["predicted"], 10.0)

        self.assertEqual(predictor.trained_series, [[4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]])


if __name__ == "__main__":
    unittest.main()
