"""
特征工程模块 - 提取时间序列特征
"""
import pandas as pd
import numpy as np
from typing import Tuple


class FeatureEngineer:
    """特征工程"""
    
    @staticmethod
    def extract_features(df: pd.DataFrame) -> pd.DataFrame:
        """
        提取时间序列特征
        
        Args:
            df: 包含action_date和total_quantity的DataFrame
            
        Returns:
            pd.DataFrame: 添加特征后的DataFrame
        """
        if df.empty:
            return df
        
        df = df.copy()
        df['action_date'] = pd.to_datetime(df['action_date'])
        
        # 时间特征
        df['day_of_week'] = df['action_date'].dt.dayofweek
        df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)
        df['day_of_month'] = df['action_date'].dt.day
        df['month'] = df['action_date'].dt.month
        
        # 滞后特征（前1天、前3天、前7天）
        for lag in [1, 3, 7]:
            df[f'lag_{lag}'] = df['total_quantity'].shift(lag)
        
        # 移动平均（3天、7天）
        df['ma_3'] = df['total_quantity'].rolling(window=3, min_periods=1).mean()
        df['ma_7'] = df['total_quantity'].rolling(window=7, min_periods=1).mean()
        
        # 趋势斜率（差分）
        df['trend'] = df['total_quantity'].diff()
        df['trend_2'] = df['total_quantity'].diff(2)
        
        # 波动率（标准差）
        df['std_7'] = df['total_quantity'].rolling(window=7, min_periods=1).std()
        
        # 填充NaN
        df = df.fillna(0)
        
        return df
    
    @staticmethod
    def prepare_training_data(df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.Series]:
        """
        准备训练数据
        
        Args:
            df: 包含特征的DataFrame
            
        Returns:
            Tuple: (X训练特征, y训练标签)
        """
        feature_columns = [
            'day_of_week', 'is_weekend', 'day_of_month', 'month',
            'lag_1', 'lag_3', 'lag_7',
            'ma_3', 'ma_7', 'trend', 'trend_2', 'std_7'
        ]
        
        # 确保所有特征列都存在
        available_columns = [col for col in feature_columns if col in df.columns]
        
        X = df[available_columns]
        y = df['total_quantity']
        
        return X, y
    
    @staticmethod
    def prepare_prediction_features(last_rows: pd.DataFrame, predict_days: int = 7) -> pd.DataFrame:
        """
        准备预测特征
        
        Args:
            last_rows: 最后的历史数据行
            predict_days: 预测天数
            
        Returns:
            pd.DataFrame: 预测特征
        """
        if last_rows.empty:
            return pd.DataFrame()
        
        # 生成未来日期
        last_date = pd.to_datetime(last_rows['action_date'].max())
        future_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=predict_days, freq='D')
        
        # 创建预测特征DataFrame
        predict_df = pd.DataFrame({'action_date': future_dates})
        predict_df['day_of_week'] = predict_df['action_date'].dt.dayofweek
        predict_df['is_weekend'] = predict_df['day_of_week'].isin([5, 6]).astype(int)
        predict_df['day_of_month'] = predict_df['action_date'].dt.day
        predict_df['month'] = predict_df['action_date'].dt.month
        
        # 使用历史数据的统计值填充滞后特征
        last_row = last_rows.iloc[-1]
        predict_df['lag_1'] = last_row.get('total_quantity', 0)
        predict_df['lag_3'] = last_row.get('ma_3', last_row.get('total_quantity', 0))
        predict_df['lag_7'] = last_row.get('ma_7', last_row.get('total_quantity', 0))
        predict_df['ma_3'] = last_row.get('ma_3', 0)
        predict_df['ma_7'] = last_row.get('ma_7', 0)
        predict_df['trend'] = last_row.get('trend', 0)
        predict_df['trend_2'] = last_row.get('trend_2', 0)
        predict_df['std_7'] = last_row.get('std_7', 0)
        
        return predict_df
