"""
预警服务 - 检测缺货、呆滞、波动风险
"""
import pandas as pd
import config
from typing import Dict, List, Optional
from models.predictor import EnsemblePredictor
from models.anomaly_detector import AnomalyDetector
from services.data_service import data_service


class WarningService:
    """预警服务"""
    
    def __init__(self):
        self.predictor = EnsemblePredictor()
        self.anomaly_detector = AnomalyDetector()
    
    def check_shortage_risk(
        self,
        part_code: str,
        part_name: str,
        current_stock: int,
        predicted_demand: float,
        confidence: float
    ) -> Dict:
        """
        检查缺货风险
        
        Args:
            part_code: 零件号
            part_name: 零件名称
            current_stock: 当前库存
            predicted_demand: 预测7天需求量
            confidence: 预测置信度
            
        Returns:
            Dict: 缺货风险信息
        """
        safety_stock = predicted_demand * config.SAFETY_STOCK_MULTIPLIER
        
        if current_stock < predicted_demand:
            risk_level = "HIGH"
            days_remaining = current_stock / (predicted_demand / 7) if predicted_demand > 0 else 999
        elif current_stock < safety_stock:
            risk_level = "MEDIUM"
            days_remaining = current_stock / (predicted_demand / 7) if predicted_demand > 0 else 999
        else:
            risk_level = "LOW"
            days_remaining = 999
        
        return {
            'partCode': part_code,
            'partName': part_name,
            'riskType': 'SHORTAGE',
            'riskLevel': risk_level,
            'currentStock': current_stock,
            'predictedDemand7d': round(predicted_demand, 1),
            'safetyStock': round(safety_stock, 1),
            'daysRemaining': round(days_remaining, 1),
            'confidence': confidence,
            'recommendation': self._generate_shortage_recommendation(risk_level, days_remaining)
        }

    @staticmethod
    def _complete_daily_series(daily_data: pd.DataFrame, days: int = 7) -> pd.DataFrame:
        """补齐固定天数的每日数据，缺失日期按0处理。"""
        if daily_data.empty:
            return daily_data

        completed = daily_data.copy()
        completed['action_date'] = pd.to_datetime(completed['action_date']).dt.normalize()

        today = pd.Timestamp.now().normalize()
        date_range = pd.date_range(end=today, periods=days, freq='D')

        completed = completed.set_index('action_date').reindex(date_range, fill_value=0)
        completed.index.name = 'action_date'
        return completed.reset_index()
    
    def check_obsolete_risk(
        self,
        part_code: str,
        part_name: str,
        current_stock: int,
        avg_daily_usage: float
    ) -> Optional[Dict]:
        """
        检查呆滞风险
        
        Args:
            part_code: 零件号
            part_name: 零件名称
            current_stock: 当前库存
            avg_daily_usage: 平均日用量
            
        Returns:
            Optional[Dict]: 呆滞风险信息（无风险返回None）
        """
        if avg_daily_usage <= 0:
            # 无使用记录，检查是否完全呆滞
            if current_stock > 0:
                return {
                    'partCode': part_code,
                    'partName': part_name,
                    'riskType': 'OBSOLETE',
                    'riskLevel': 'HIGH',
                    'currentStock': current_stock,
                    'avgDailyUsage': 0,
                    'daysOfStock': 999,
                    'recommendation': '该零件无任何出入库记录，建议核实是否已停产'
                }
            return None
        
        days_of_stock = current_stock / avg_daily_usage
        
        if days_of_stock > config.OBSOLETE_THRESHOLD_DAYS:
            risk_level = 'HIGH' if days_of_stock > 60 else 'MEDIUM'
            return {
                'partCode': part_code,
                'partName': part_name,
                'riskType': 'OBSOLETE',
                'riskLevel': risk_level,
                'currentStock': current_stock,
                'avgDailyUsage': round(avg_daily_usage, 2),
                'daysOfStock': round(days_of_stock, 1),
                'recommendation': self._generate_obsolete_recommendation(risk_level, days_of_stock)
            }
        
        return None
    
    def check_fluctuation_risk(
        self,
        part_code: str,
        part_name: str,
        historical_data: pd.DataFrame
    ) -> Optional[Dict]:
        """
        检查波动风险
        
        Args:
            part_code: 零件号
            part_name: 零件名称
            historical_data: 历史数据
            
        Returns:
            Optional[Dict]: 波动风险信息
        """
        # 检测波动异常
        volatility_result = self.anomaly_detector.detect_volatility_anomaly(historical_data)
        
        if volatility_result['is_anomaly']:
            return {
                'partCode': part_code,
                'partName': part_name,
                'riskType': 'FLUCTUATION',
                'riskLevel': volatility_result['severity'],
                'currentStock': 0,  # 不需要库存信息
                'details': volatility_result['details'],
                'mean': round(volatility_result.get('mean', 0), 2),
                'std': round(volatility_result.get('std', 0), 2),
                'recommendation': '出入库量波动异常，建议关注供应链稳定性'
            }
        
        # 检测趋势变化
        trend_result = self.anomaly_detector.detect_trend_change(historical_data)
        
        if trend_result['trend_change'] and trend_result['strength'] > 0.3:
            return {
                'partCode': part_code,
                'partName': part_name,
                'riskType': 'FLUCTUATION',
                'riskLevel': 'MEDIUM' if trend_result['strength'] < 0.5 else 'HIGH',
                'currentStock': 0,
                'details': trend_result['details'],
                'trendDirection': trend_result['direction'],
                'trendStrength': round(trend_result['strength'], 2),
                'recommendation': f'检测到{trend_result["direction"]}趋势，建议调整采购策略'
            }
        
        return None
    
    def generate_all_warnings(self) -> List[Dict]:
        """
        生成所有零件的预警信息
        
        Returns:
            List[Dict]: 预警列表
        """
        warnings = []
        
        # 获取所有有库存的零件
        inventory_summary = data_service.get_part_inventory_summary()
        
        for _, row in inventory_summary.iterrows():
            part_code = row['part_code']
            part_name = row['part_name']
            current_stock = int(row['total_stock'])
            
            try:
                # 获取历史数据（7天内）
                historical_data = data_service.get_part_history(part_code, days=7)
                
                # 检查呆滞风险（即使无历史数据也要检查）
                if historical_data.empty:
                    # 有库存但无任何出入库记录，属于严重呆滞
                    obsolete_warning = self.check_obsolete_risk(part_code, part_name, current_stock, 0)
                    if obsolete_warning:
                        warnings.append(obsolete_warning)
                    continue
                
                # 聚合每日总量（用于波动预警）
                daily_total = historical_data.groupby('action_date').agg({
                    'total_quantity': 'sum'
                }).reset_index()
                
                # 聚合每日出库量（用于缺货预测和呆滞预警）
                outbound_only = historical_data[historical_data['action_type'] == 'OUTBOUND'].groupby('action_date').agg({
                    'total_quantity': 'sum'
                }).reset_index()
                outbound_daily = self._complete_daily_series(outbound_only, days=7)
                
                # 训练预测模型（使用出库量预测未来出库需求）
                if outbound_daily.empty:
                    # 无出库记录，预测需求为0
                    predicted_demand_7d = 0
                    confidence = 0
                else:
                    mape_scores = self.predictor.train(outbound_daily)
                    
                    # 预测7天出库需求
                    prediction_result = self.predictor.predict(outbound_daily, predict_days=7)
                    predicted_demand_7d = float(prediction_result['ensemble'].sum())
                    confidence = prediction_result['confidence']
                
                # 检查缺货风险（基于预测的出库量）
                shortage_warning = self.check_shortage_risk(
                    part_code, part_name, current_stock, predicted_demand_7d, confidence
                )
                if shortage_warning['riskLevel'] in ['HIGH', 'MEDIUM']:
                    warnings.append(shortage_warning)
                
                # 检查呆滞风险（使用平均日出库量，按7天平均）
                total_outbound_7d = outbound_daily['total_quantity'].sum() if not outbound_daily.empty else 0
                avg_daily_outbound = total_outbound_7d / 7  # 按7天平均，而不是按有出库的天数平均
                obsolete_warning = self.check_obsolete_risk(
                    part_code, part_name, current_stock, avg_daily_outbound
                )
                if obsolete_warning:
                    warnings.append(obsolete_warning)
                
                # 检查波动风险（使用出入库总量）
                fluctuation_warning = self.check_fluctuation_risk(
                    part_code, part_name, daily_total
                )
                if fluctuation_warning:
                    warnings.append(fluctuation_warning)
                
            except Exception as e:
                print(f"零件 {part_code} 预警分析失败: {e}")
                continue
        
        # 按风险等级排序
        risk_level_order = {'HIGH': 0, 'MEDIUM': 1, 'LOW': 2}
        warnings.sort(key=lambda x: risk_level_order.get(x['riskLevel'], 3))
        
        return warnings
    
    def get_part_warnings(self, part_code: str) -> List[Dict]:
        """
        获取指定零件的预警信息
        
        Args:
            part_code: 零件号
            
        Returns:
            List[Dict]: 预警列表
        """
        all_warnings = self.generate_all_warnings()
        return [w for w in all_warnings if w['partCode'] == part_code]
    
    def _generate_shortage_recommendation(self, risk_level: str, days_remaining: float) -> str:
        """生成缺货建议"""
        if risk_level == 'HIGH':
            if days_remaining < 3:
                return f'紧急！库存仅够{days_remaining:.1f}天，建议立即采购'
            return f'库存紧张，预计{days_remaining:.1f}天后缺货，建议尽快采购'
        elif risk_level == 'MEDIUM':
            return f'库存偏低，建议补充库存至安全水位'
        return '库存充足'
    
    def _generate_obsolete_recommendation(self, risk_level: str, days_of_stock: float) -> str:
        """生成呆滞建议"""
        if risk_level == 'HIGH':
            if days_of_stock > 90:
                return f'严重呆滞！库存可维持{days_of_stock:.0f}天，建议停止采购并促销处理'
            return f'库存呆滞风险，建议减少采购频率'
        return f'库存周转较慢，建议关注使用趋势'


# 全局预警服务实例
warning_service = WarningService()
