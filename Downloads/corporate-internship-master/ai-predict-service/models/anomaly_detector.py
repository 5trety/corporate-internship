"""
Anomaly detector for inventory flow warnings.
"""
import numpy as np
import pandas as pd
from typing import Dict


class AnomalyDetector:
    """Detect abnormal inventory flow patterns."""

    @staticmethod
    def _prepare_quantities(historical_data: pd.DataFrame) -> pd.Series:
        ordered_data = historical_data.copy()
        if 'action_date' in ordered_data.columns:
            ordered_data = ordered_data.sort_values('action_date')
        return pd.to_numeric(ordered_data['total_quantity'], errors='coerce').fillna(0)

    @staticmethod
    def _direction_changes(quantities: pd.Series) -> int:
        values = quantities.astype(float).to_numpy()
        deltas = np.diff(values)
        signs = []

        for delta in deltas:
            if abs(delta) < 1e-9:
                continue
            signs.append(1 if delta > 0 else -1)

        return sum(
            1
            for previous, current in zip(signs, signs[1:])
            if previous != current
        )

    @staticmethod
    def detect_volatility_anomaly(
        historical_data: pd.DataFrame,
        threshold_std: float = 2.0
    ) -> Dict:
        """
        Detect repeated up-and-down volatility.

        A single late spike is treated as a spike, not a fluctuation warning.
        Fluctuation requires multiple direction reversals plus enough amplitude.
        """
        if historical_data.empty or len(historical_data) < 7:
            return {
                'is_anomaly': False,
                'anomaly_type': None,
                'severity': 'LOW',
                'details': '数据不足'
            }

        quantities = AnomalyDetector._prepare_quantities(historical_data)
        mean_val = quantities.mean()
        std_val = quantities.std()

        if mean_val <= 0 or std_val <= 0:
            return {
                'is_anomaly': False,
                'anomaly_type': None,
                'severity': 'LOW',
                'details': '波动正常',
                'mean': float(mean_val),
                'std': float(std_val)
            }

        direction_changes = AnomalyDetector._direction_changes(quantities)
        range_ratio = (quantities.max() - quantities.min()) / mean_val
        variation_coefficient = std_val / mean_val
        range_threshold = max(1.0, threshold_std * 0.5)

        if direction_changes >= 3 and range_ratio >= range_threshold:
            severity = (
                'HIGH' if variation_coefficient >= 0.8
                else 'MEDIUM' if variation_coefficient >= 0.5
                else 'LOW'
            )
            return {
                'is_anomaly': True,
                'anomaly_type': 'VOLATILITY_SWING',
                'severity': severity,
                'details': f'检测到有起有伏的连续波动，方向变化{direction_changes}次，振幅比 {range_ratio:.2f}',
                'mean': float(mean_val),
                'std': float(std_val),
                'directionChanges': int(direction_changes),
                'rangeRatio': float(range_ratio),
                'variationCoefficient': float(variation_coefficient)
            }

        return {
            'is_anomaly': False,
            'anomaly_type': None,
            'severity': 'LOW',
            'details': '波动正常',
            'mean': float(mean_val),
            'std': float(std_val),
            'directionChanges': int(direction_changes),
            'rangeRatio': float(range_ratio),
            'variationCoefficient': float(variation_coefficient)
        }

    @staticmethod
    def detect_trend_change(
        historical_data: pd.DataFrame,
        window_short: int = 3,
        window_long: int = 7
    ) -> Dict:
        """
        Detect trend changes using short and long moving averages.
        """
        if historical_data.empty or len(historical_data) < window_long:
            return {
                'trend_change': False,
                'direction': None,
                'strength': 0
            }

        quantities = AnomalyDetector._prepare_quantities(historical_data)

        ma_short = quantities.rolling(window=window_short).mean()
        ma_long = quantities.rolling(window=window_long).mean()

        if len(ma_short) >= 2 and len(ma_long) >= 2:
            current_diff = ma_short.iloc[-1] - ma_long.iloc[-1]
            previous_diff = ma_short.iloc[-2] - ma_long.iloc[-2]

            if previous_diff < 0 and current_diff > 0:
                return {
                    'trend_change': True,
                    'direction': 'UP',
                    'strength': float(abs(current_diff) / ma_long.iloc[-1]) if ma_long.iloc[-1] > 0 else 0,
                    'details': '检测到上升趋势（金叉）'
                }
            if previous_diff > 0 and current_diff < 0:
                return {
                    'trend_change': True,
                    'direction': 'DOWN',
                    'strength': float(abs(current_diff) / ma_long.iloc[-1]) if ma_long.iloc[-1] > 0 else 0,
                    'details': '检测到下降趋势（死叉）'
                }

        return {
            'trend_change': False,
            'direction': None,
            'strength': 0,
            'details': '趋势稳定'
        }
