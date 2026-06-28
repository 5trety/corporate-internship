"""
趋势分析服务 - 生成预测趋势和可视化图表
"""
import pandas as pd
import numpy as np
import matplotlib
matplotlib.use('Agg')  # 无GUI环境
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from matplotlib.ticker import MaxNLocator
import io
import base64
from typing import Dict, Tuple
import config
from models.predictor import EnsemblePredictor
from services.data_service import data_service


class TrendService:
    """趋势分析服务"""
    
    def __init__(self):
        self.predictor = EnsemblePredictor()

    def _build_daily_trend_data(self, historical_data: pd.DataFrame, part_code: str) -> pd.DataFrame:
        """聚合最近7天入库、出库和库存数据。"""
        daily_inbound = historical_data[historical_data['action_type'] == 'INBOUND'].groupby('action_date').agg({
            'total_quantity': 'sum'
        }).reset_index()
        daily_inbound.columns = ['action_date', 'inbound']

        daily_outbound = historical_data[historical_data['action_type'].isin(['OUTBOUND', 'TRANSFER'])].groupby('action_date').agg({
            'total_quantity': 'sum'
        }).reset_index()
        daily_outbound.columns = ['action_date', 'outbound']

        daily_data = pd.merge(daily_inbound, daily_outbound, on='action_date', how='outer').fillna(0)
        daily_data['action_date'] = pd.to_datetime(daily_data['action_date'])
        daily_data = daily_data.sort_values('action_date')

        if 'inbound' not in daily_data.columns:
            daily_data['inbound'] = 0
        if 'outbound' not in daily_data.columns:
            daily_data['outbound'] = 0

        today = pd.Timestamp.now().normalize()
        date_range = pd.date_range(end=today, periods=7, freq='D')
        daily_data = daily_data.set_index('action_date').reindex(date_range, fill_value=0)
        daily_data.index.name = 'action_date'
        daily_data = daily_data.reset_index()

        inventory_sql = """
        SELECT SUM(ci.quantity) as current_stock
        FROM current_inventory ci
        LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no
        WHERE ci.part_code = :part_code
          AND ci.quantity > 0
          AND (k.kanban_no IS NULL OR k.is_sealed = 0)
        """
        from utils.db_helper import db
        stock_df = db.query_dataframe(inventory_sql, {'part_code': part_code})
        current_stock = int(stock_df['current_stock'].iloc[0]) if not stock_df.empty and stock_df['current_stock'].iloc[0] else 0

        total_net_flow = (daily_data['inbound'] - daily_data['outbound']).sum()
        first_day_inventory = current_stock - total_net_flow

        daily_data['net_flow'] = daily_data['inbound'] - daily_data['outbound']
        daily_data['inventory'] = first_day_inventory + daily_data['net_flow'].cumsum()

        return daily_data

    def _build_prediction_points(
        self,
        daily_data: pd.DataFrame,
        source_column: str,
        predict_days: int
    ) -> Tuple[list, float, Dict[str, float], str]:
        train_data = daily_data[['action_date', source_column]].rename(columns={source_column: 'total_quantity'})
        mape_scores = self.predictor.train(train_data)

        prediction_result = self.predictor.predict(train_data, predict_days=predict_days)
        predicted_values = np.asarray(prediction_result['ensemble'], dtype=float)
        confidence = float(prediction_result.get('confidence', 0))
        prediction_mode = prediction_result.get('predictionMode', 'ensemble')
        std_values = predicted_values * (1 - confidence) * 0.5

        last_date = daily_data['action_date'].max()
        predicted_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=predict_days, freq='D')

        predicted_list = []
        for i, date in enumerate(predicted_dates):
            predicted_list.append({
                'date': date.strftime('%Y-%m-%d'),
                'predicted': round(float(predicted_values[i]), 1),
                'upper_bound': round(float(predicted_values[i] + std_values[i]), 1),
                'lower_bound': round(float(max(0, predicted_values[i] - std_values[i])), 1),
                'confidence': confidence
            })

        return predicted_list, confidence, mape_scores, prediction_mode

    def generate_prediction_trend(
        self,
        part_code: str,
        predict_days: int = 7
    ) -> Dict:
        """
        生成预测趋势数据

        Args:
            part_code: 零件号
            predict_days: 预测天数

        Returns:
            Dict: 趋势数据
        """
        # 获取历史数据（7天内）
        historical_data = data_service.get_part_history(part_code, days=7)

        if historical_data.empty:
            return {
                'success': False,
                'message': '无历史数据',
                'historical': [],
                'predicted': [],
                'predictionMeta': {
                    'label': 'AI需求预测（出库量）',
                    'source': 'outbound',
                    'mode': 'none'
                }
            }

        daily_data = self._build_daily_trend_data(historical_data, part_code)

        outbound_points, outbound_confidence, outbound_mape, outbound_mode = self._build_prediction_points(
            daily_data, 'outbound', predict_days
        )

        # 构建返回数据
        historical_list = []
        for _, row in daily_data.iterrows():
            historical_list.append({
                'date': row['action_date'].strftime('%Y-%m-%d'),
                'inbound': float(row['inbound']),
                'outbound': float(row['outbound']),
                'inventory': float(row['inventory'])
            })

        return {
            'success': True,
            'partCode': part_code,
            'confidence': outbound_confidence,
            'mapeScores': {k: round(v, 3) for k, v in outbound_mape.items()},
            'historical': historical_list,
            'predicted': outbound_points,
            'predictionMeta': {
                'label': 'AI需求预测（出库量）',
                'source': 'outbound',
                'mode': outbound_mode
            }
        }
    
    def generate_trend_chart(
        self,
        part_code: str,
        predict_days: int = 7,
        width: int = 12,
        height: int = 6
    ) -> str:
        """
        生成趋势图并返回Base64编码
        
        Args:
            part_code: 零件号
            predict_days: 预测天数
            width: 图片宽度
            height: 图片高度
            
        Returns:
            str: Base64编码的图片
        """
        # 获取历史数据（分开入库和出库，7天内）
        historical_data = data_service.get_part_history(part_code, days=7)
        
        if historical_data.empty:
            return ''
        
        # 按日期和类型聚合（包含TRANSFER）
        daily_inbound = historical_data[historical_data['action_type'] == 'INBOUND'].groupby('action_date').agg({
            'total_quantity': 'sum'
        }).reset_index()
        daily_inbound.columns = ['action_date', 'inbound']
        
        daily_outbound = historical_data[historical_data['action_type'].isin(['OUTBOUND', 'TRANSFER'])].groupby('action_date').agg({
            'total_quantity': 'sum'
        }).reset_index()
        daily_outbound.columns = ['action_date', 'outbound']
        
        # 合并入库和出库数据
        daily_data = pd.merge(daily_inbound, daily_outbound, on='action_date', how='outer').fillna(0)
        daily_data['action_date'] = pd.to_datetime(daily_data['action_date'])
        daily_data = daily_data.sort_values('action_date')
        
        # 确保inbound和outbound列存在
        if 'inbound' not in daily_data.columns:
            daily_data['inbound'] = 0
        if 'outbound' not in daily_data.columns:
            daily_data['outbound'] = 0
        
        # 补齐7天内的所有日期（缺失的日期填充0）
        today = pd.Timestamp.now().normalize()
        date_range = pd.date_range(end=today, periods=7, freq='D')
        daily_data = daily_data.set_index('action_date').reindex(date_range, fill_value=0)
        daily_data.index.name = 'action_date'
        daily_data = daily_data.reset_index()
        
        # 获取真实当前库存（从 current_inventory 表）
        inventory_sql = """
        SELECT SUM(ci.quantity) as current_stock
        FROM current_inventory ci
        LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no
        WHERE ci.part_code = :part_code
          AND ci.quantity > 0
          AND (k.kanban_no IS NULL OR k.is_sealed = 0)
        """
        from utils.db_helper import db
        stock_df = db.query_dataframe(inventory_sql, {'part_code': part_code})
        current_stock = int(stock_df['current_stock'].iloc[0]) if not stock_df.empty and stock_df['current_stock'].iloc[0] else 0
        
        # 正向计算历史库存：从第一天开始累加
        total_net_flow = (daily_data['inbound'] - daily_data['outbound']).sum()
        first_day_inventory = current_stock - total_net_flow
        
        daily_data['net_flow'] = daily_data['inbound'] - daily_data['outbound']
        daily_data['inventory'] = first_day_inventory + daily_data['net_flow'].cumsum()
        
        # 训练模型（使用总流量）
        daily_data['total'] = daily_data['inbound'] + daily_data['outbound']
        train_data = daily_data[['action_date', 'total']].rename(columns={'total': 'total_quantity'})
        mape_scores = self.predictor.train(train_data)
        
        # 预测
        prediction_result = self.predictor.predict(train_data, predict_days=predict_days)
        predicted_values = prediction_result['ensemble']
        confidence = prediction_result['confidence']
        
        # 计算置信区间
        std_values = predicted_values * (1 - confidence) * 0.5
        
        # 生成预测日期
        last_date = daily_data['action_date'].max()
        predicted_dates = pd.date_range(start=last_date + pd.Timedelta(days=1), periods=predict_days, freq='D')
        
        # 设置中文字体
        plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS']
        plt.rcParams['axes.unicode_minus'] = False
        
        # 创建图表和双Y轴（强制使用双Y轴）
        fig, ax1 = plt.subplots(figsize=(width, height))
        ax2 = ax1.twinx()
        
        dates = daily_data['action_date']
        
        # 左Y轴 - 入库/出库/预测
        ax1.set_ylabel('出入库数量', fontsize=12, fontweight='bold', color='#333')
        ax1.plot(dates, daily_data['inbound'], 
                label='入库量', marker='o', color='#4CAF50', 
                linewidth=2, markersize=5, alpha=0.8)
        ax1.plot(dates, daily_data['outbound'],
                label='出库量', marker='s', color='#FF9800',
                linewidth=2, markersize=5, alpha=0.8)
        
        # 预测线
        if len(predicted_dates) > 0:
            ax1.plot(predicted_dates, predicted_values,
                    label='AI预测', marker='x', color='#F44336',
                    linewidth=2.5, markersize=8, linestyle='--', alpha=0.9)
            ax1.fill_between(predicted_dates, 
                          predicted_values - std_values,
                          predicted_values + std_values,
                          alpha=0.2, color='#F44336', label='置信区间')
        
        # 右Y轴 - 库存
        ax2.set_ylabel('当前库存', fontsize=12, fontweight='bold', color='#2196F3')
        ax2.plot(dates, daily_data['inventory'],
                label='当前库存', marker='^', color='#2196F3',
                linewidth=3, markersize=7, alpha=0.9)
        ax2.tick_params(axis='y', labelcolor='#2196F3')
        
        # 合并图例
        lines1, labels1 = ax1.get_legend_handles_labels()
        lines2, labels2 = ax2.get_legend_handles_labels()
        ax1.legend(lines1 + lines2, labels1 + labels2, 
                  loc='upper left', fontsize=9, framealpha=0.9, ncol=2)
        
        # 设置Y轴范围
        ax1.set_ylim(bottom=0)
        ax2.set_ylim(bottom=0)
        
        # 设置图表样式
        ax1.set_xlabel('日期', fontsize=12, fontweight='bold')
        ax1.set_title(f'零件 {part_code} 出入库趋势预测（{predict_days}天）', 
                     fontsize=14, fontweight='bold', pad=20)
        ax1.grid(True, alpha=0.3, linestyle='--')
        
        # 格式化X轴日期
        ax1.xaxis.set_major_formatter(mdates.DateFormatter('%m-%d'))
        ax1.xaxis.set_major_locator(mdates.DayLocator(interval=max(1, len(dates)//10)))
        plt.xticks(rotation=45)
        
        # 设置Y轴整数刻度
        ax1.yaxis.set_major_locator(MaxNLocator(integer=True))
        ax2.yaxis.set_major_locator(MaxNLocator(integer=True))
        
        # 添加信息框
        current_stock = daily_data['inventory'].iloc[-1]
        avg_daily_inbound = daily_data['inbound'].mean()
        avg_daily_outbound = daily_data['outbound'].mean()
        
        info_text = f'当前库存: {current_stock:,.0f}\n日均入库: {avg_daily_inbound:,.0f}\n日均出库: {avg_daily_outbound:,.0f}\n预测置信度: {confidence:.0%}'
        ax1.text(0.02, 0.98, info_text, transform=ax1.transAxes,
                fontsize=9, verticalalignment='top',
                bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.9))
        
        # 调整布局
        plt.tight_layout()
        
        # 转换为Base64
        buf = io.BytesIO()
        plt.savefig(buf, format='png', dpi=100, bbox_inches='tight', 
                   facecolor='white', edgecolor='none')
        buf.seek(0)
        img_base64 = base64.b64encode(buf.read()).decode('utf-8')
        plt.close()
        
        return img_base64
    
    def get_dashboard_summary(self) -> Dict:
        """
        获取预警看板汇总数据
        
        Returns:
            Dict: 汇总数据
        """
        from services.warning_service import warning_service
        
        warnings = warning_service.generate_all_warnings()
        
        # 统计各类型预警数量
        shortage_warnings = [w for w in warnings if w['riskType'] == 'SHORTAGE']
        obsolete_warnings = [w for w in warnings if w['riskType'] == 'OBSOLETE']
        fluctuation_warnings = [w for w in warnings if w['riskType'] == 'FLUCTUATION']
        
        # 统计高风险数量
        shortage_high = len([w for w in shortage_warnings if w['riskLevel'] == 'HIGH'])
        obsolete_high = len([w for w in obsolete_warnings if w['riskLevel'] == 'HIGH'])
        fluctuation_high = len([w for w in fluctuation_warnings if w['riskLevel'] == 'HIGH'])
        
        # 获取总库存信息
        inventory_summary = data_service.get_part_inventory_summary()
        total_parts = len(inventory_summary)
        total_stock = int(inventory_summary['total_stock'].sum()) if not inventory_summary.empty else 0
        
        return {
            'shortage': {
                'count': len(shortage_warnings),
                'high': shortage_high,
                'medium': len([w for w in shortage_warnings if w['riskLevel'] == 'MEDIUM']),
                'low': len([w for w in shortage_warnings if w['riskLevel'] == 'LOW'])
            },
            'obsolete': {
                'count': len(obsolete_warnings),
                'high': obsolete_high,
                'medium': len([w for w in obsolete_warnings if w['riskLevel'] == 'MEDIUM']),
                'low': len([w for w in obsolete_warnings if w['riskLevel'] == 'LOW'])
            },
            'fluctuation': {
                'count': len(fluctuation_warnings),
                'high': fluctuation_high,
                'medium': len([w for w in fluctuation_warnings if w['riskLevel'] == 'MEDIUM']),
                'low': len([w for w in fluctuation_warnings if w['riskLevel'] == 'LOW'])
            },
            'inventory': {
                'totalParts': total_parts,
                'totalStock': total_stock
            },
            'totalWarnings': len(warnings),
            'highRiskWarnings': shortage_high + obsolete_high + fluctuation_high
        }


# 全局趋势服务实例
trend_service = TrendService()
