"""
集成预测模型 - 移动平均、线性回归、随机森林
"""
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_percentage_error
from typing import Dict, Tuple
import config
from models.feature_engineer import FeatureEngineer


class EnsemblePredictor:
    """集成预测器"""
    
    def __init__(self):
        """初始化模型"""
        self.models = {
            'moving_avg': None,
            'linear_reg': LinearRegression(),
            'random_forest': RandomForestRegressor(
                n_estimators=100,
                max_depth=10,
                random_state=42,
                n_jobs=-1
            )
        }
        self.weights = config.MODEL_ENSEMBLE_WEIGHTS
        self.feature_engineer = FeatureEngineer()
    
    def _moving_avg_predict(self, historical_data: pd.DataFrame, predict_days: int = 7) -> np.ndarray:
        """
        移动平均预测
        
        Args:
            historical_data: 历史数据
            predict_days: 预测天数
            
        Returns:
            np.ndarray: 预测值数组
        """
        if historical_data.empty:
            return np.zeros(predict_days)
        
        # 计算7天移动平均
        ma_7 = historical_data['total_quantity'].rolling(window=7, min_periods=1).mean()
        avg_value = ma_7.iloc[-1]
        
        # 移动平均预测：返回固定平均值（不考虑趋势）
        # 趋势由线性回归和模式识别处理
        predictions = np.full(predict_days, avg_value)
        
        return predictions

    @staticmethod
    def _recent_values(historical_data: pd.DataFrame) -> np.ndarray:
        if historical_data.empty or 'total_quantity' not in historical_data.columns:
            return np.array([])

        ordered_data = historical_data.copy()
        if 'action_date' in ordered_data.columns:
            ordered_data = ordered_data.sort_values('action_date')

        return pd.to_numeric(
            ordered_data['total_quantity'],
            errors='coerce'
        ).fillna(0).tail(7).to_numpy(dtype=float)

    @staticmethod
    def _direction_changes(values: np.ndarray) -> int:
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

    def _pattern_aware_predict(
        self,
        historical_data: pd.DataFrame,
        predict_days: int = 7
    ) -> Tuple[np.ndarray, str]:
        """
        模式识别预测（仅在数据充足时启用）
        
        Args:
            historical_data: 历史数据
            predict_days: 预测天数
            
        Returns:
            Tuple: (预测值数组, 模式名称)
        """
        values = self._recent_values(historical_data)
        if len(values) < 7:
            return np.array([]), ''

        mean_value = float(np.mean(values))
        if mean_value <= 0:
            return np.array([]), ''

        direction_changes = self._direction_changes(values)
        range_ratio = float((np.max(values) - np.min(values)) / mean_value)

        # 波动模式：提高阈值，避免稀疏数据误判
        # 需要至少4次方向变化 且 极差/均值>=1.5（原来3次和1.0）
        if direction_changes >= 4 and range_ratio >= 1.5:
            pattern = np.roll(values, -1)
            return np.maximum(0, np.resize(pattern, predict_days)), 'fluctuating'

        # 趋势分析
        x = np.arange(len(values))
        slope = float(np.polyfit(x, values, 1)[0])
        diffs = np.diff(values)
        rising_steps = int(np.sum(diffs > 0))
        falling_steps = int(np.sum(diffs < 0))

        # 上升模式：需要至少6步上升（原来5步）
        if slope >= 1.0 and rising_steps >= 6:  # 提高阈值
            future = values[-1] + slope * np.arange(1, predict_days + 1)
            return np.maximum(0, future), 'rising'

        # 下降模式：需要至少6步下降（原来5步）
        if slope <= -1.0 and falling_steps >= 6:  # 提高阈值
            future = values[-1] + slope * np.arange(1, predict_days + 1)
            return np.maximum(0, future), 'falling'

        return np.array([]), ''
    
    def train(self, historical_data: pd.DataFrame) -> Dict[str, float]:
        """
        训练集成模型
        
        Args:
            historical_data: 历史数据DataFrame
            
        Returns:
            Dict: 各模型的MAPE评估结果
        """
        if historical_data.empty or len(historical_data) < 7:
            return {'moving_avg': 0, 'linear_reg': 0, 'random_forest': 0}
        
        # 提取特征
        df_featured = self.feature_engineer.extract_features(historical_data)
        X, y = self.feature_engineer.prepare_training_data(df_featured)
        
        # 分割训练集和验证集
        split_idx = int(len(X) * 0.8)
        X_train, X_val = X.iloc[:split_idx], X.iloc[split_idx:]
        y_train, y_val = y.iloc[:split_idx], y.iloc[split_idx:]
        
        mape_scores = {}
        
        # 训练线性回归
        try:
            self.models['linear_reg'].fit(X_train, y_train)
            y_pred_lr = self.models['linear_reg'].predict(X_val)
            mape_scores['linear_reg'] = mean_absolute_percentage_error(y_val, np.maximum(0, y_pred_lr))
        except Exception as e:
            print(f"线性回归训练失败: {e}")
            mape_scores['linear_reg'] = 1.0
        
        # 训练随机森林
        try:
            self.models['random_forest'].fit(X_train, y_train)
            y_pred_rf = self.models['random_forest'].predict(X_val)
            mape_scores['random_forest'] = mean_absolute_percentage_error(y_val, np.maximum(0, y_pred_rf))
        except Exception as e:
            print(f"随机森林训练失败: {e}")
            mape_scores['random_forest'] = 1.0
        
        # 移动平均评估
        try:
            y_pred_ma = self._moving_avg_predict(historical_data.iloc[:split_idx], len(y_val))
            mape_scores['moving_avg'] = mean_absolute_percentage_error(y_val, np.maximum(0, y_pred_ma))
        except Exception as e:
            print(f"移动平均评估失败: {e}")
            mape_scores['moving_avg'] = 1.0
        
        return mape_scores
    
    def predict(self, historical_data: pd.DataFrame, predict_days: int = 7) -> Dict:
        """
        集成预测
        
        Args:
            historical_data: 历史数据
            predict_days: 预测天数
            
        Returns:
            Dict: 预测结果
        """
        if historical_data.empty:
            return {
                'predictions': {'moving_avg': [], 'linear_reg': [], 'random_forest': []},
                'ensemble': np.zeros(predict_days),
                'confidence': 0,
                'predictionMode': 'empty',
                'dataLength': 0,
                'activeDays': 0,
                'weights': {}
            }
        
        predictions = {}
        
        # 统计7天内有多少天有实际数据（非零数据）
        if 'total_quantity' in historical_data.columns:
            active_days = (historical_data['total_quantity'] > 0).sum()
        else:
            active_days = len(historical_data)
        
        total_days = len(historical_data)  # 总天数（包括0）
        
        # 移动平均预测（始终可用）
        predictions['moving_avg'] = self._moving_avg_predict(historical_data, predict_days)
        
        # 准备预测特征
        df_featured = self.feature_engineer.extract_features(historical_data)
        predict_features = self.feature_engineer.prepare_prediction_features(df_featured, predict_days)
        
        # 智能降级策略：基于活跃天数（有数据的天数）决定使用哪些模型
        use_complex_models = active_days >= 3  # 至少3天有数据才使用复杂模型
        
        if not predict_features.empty and use_complex_models:
            feature_columns = [
                'day_of_week', 'is_weekend', 'day_of_month', 'month',
                'lag_1', 'lag_3', 'lag_7',
                'ma_3', 'ma_7', 'trend', 'trend_2', 'std_7'
            ]
            available_columns = [col for col in feature_columns if col in predict_features.columns]
            X_predict = predict_features[available_columns]
            
            # 线性回归预测
            try:
                predictions['linear_reg'] = np.maximum(0, self.models['linear_reg'].predict(X_predict))
            except:
                predictions['linear_reg'] = predictions['moving_avg'].copy()
            
            # 随机森林预测
            try:
                predictions['random_forest'] = np.maximum(0, self.models['random_forest'].predict(X_predict))
            except:
                predictions['random_forest'] = predictions['moving_avg'].copy()
        else:
            # 数据不足，降级为移动平均
            predictions['linear_reg'] = predictions['moving_avg'].copy()
            predictions['random_forest'] = predictions['moving_avg'].copy()
        
        # 动态权重调整（基于活跃天数，不是总天数）
        if active_days >= 5:
            # 数据丰富（7天中有5天有数据）：使用标准权重
            weights = self.weights
        elif active_days >= 3:
            # 数据适中（7天中有3-4天有数据）：降低随机森林权重
            weights = {
                'moving_avg': 0.5,
                'linear_reg': 0.3,
                'random_forest': 0.2
            }
        else:
            # 数据稀疏（7天中只有1-2天有数据）：主要依赖移动平均
            weights = {
                'moving_avg': 0.7,
                'linear_reg': 0.2,
                'random_forest': 0.1
            }
        
        # 加权集成
        ensemble_pred = np.zeros(predict_days)
        for name, weight in weights.items():
            ensemble_pred += predictions[name] * weight
        
        # 计算置信度（基于各模型预测的一致性）
        confidence = self._calculate_confidence(predictions)
        prediction_mode = 'ensemble'

        # 模式识别增强（仅在活跃天数足够时启用）
        if active_days >= 4:
            pattern_pred, pattern_mode = self._pattern_aware_predict(historical_data, predict_days)
            if len(pattern_pred) == predict_days:
                predictions['pattern'] = pattern_pred
                # 模式预测权重：60%模式 + 40%集成
                ensemble_pred = pattern_pred * 0.6 + ensemble_pred * 0.4
                confidence = max(confidence, 0.75)
                prediction_mode = pattern_mode

        return {
            'predictions': predictions,
            'ensemble': ensemble_pred,
            'confidence': confidence,
            'predictionMode': prediction_mode,
            'dataLength': total_days,
            'activeDays': int(active_days),
            'weights': weights
        }
    
    def _calculate_confidence(self, predictions: Dict[str, np.ndarray]) -> float:
        """
        计算预测置信度
        
        Args:
            predictions: 各模型预测结果
            
        Returns:
            float: 置信度（0-1）
        """
        if not predictions:
            return 0.0
        
        # 将所有预测值堆叠
        pred_array = np.array([p for p in predictions.values() if len(p) > 0])
        
        if len(pred_array) < 2:
            return 0.5
        
        # 计算变异系数（越低越一致）
        mean_pred = np.mean(pred_array, axis=0)
        std_pred = np.std(pred_array, axis=0)
        
        # 避免除以0
        mean_pred = np.where(mean_pred == 0, 1, mean_pred)
        cv = std_pred / mean_pred
        
        # 转换为置信度（CV越低，置信度越高）
        confidence = max(0, 1 - np.mean(cv))
        
        return round(float(confidence), 2)
