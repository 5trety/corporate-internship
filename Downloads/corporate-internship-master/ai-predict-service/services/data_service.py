"""
数据服务层 - 获取历史和实时数据
"""
import pandas as pd
from utils.db_helper import db
from typing import List, Dict


class DataService:
    """数据服务"""
    
    def get_part_history(self, part_code: str, days: int = 30) -> pd.DataFrame:
        """
        获取零件历史出入库数据
        
        Args:
            part_code: 零件号
            days: 历史天数（默认30天）
            
        Returns:
            pd.DataFrame: 历史数据
        """
        sql = """
        SELECT 
            DATE(action_time) as action_date,
            action_type,
            SUM(quantity) as total_quantity,
            COUNT(*) as transaction_count
        FROM inventory_trace
        WHERE part_code = :part_code
          AND action_time >= DATE_SUB(NOW(), INTERVAL :days DAY)
        GROUP BY DATE(action_time), action_type
        ORDER BY action_date
        """
        return db.query_dataframe(sql, {'part_code': part_code, 'days': days})
    
    def get_current_inventory(self) -> pd.DataFrame:
        """
        获取当前库存快照
        
        Returns:
            pd.DataFrame: 当前库存数据
        """
        sql = """
        SELECT 
            ci.kanban_no,
            ci.part_code,
            k.part_name,
            ci.warehouse_code,
            w.warehouse_name,
            ci.quantity,
            ci.created_at
        FROM current_inventory ci
        LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no
        LEFT JOIN warehouse w ON ci.warehouse_code = w.warehouse_code
        WHERE ci.quantity > 0
          AND (k.kanban_no IS NULL OR k.is_sealed = 0)
        """
        return db.query_dataframe(sql)
    
    def get_all_part_master_data(self) -> pd.DataFrame:
        """
        获取所有零件列表
        
        Returns:
            pd.DataFrame: 零件列表
        """
        sql = """
        SELECT DISTINCT 
            part_code,
            part_name
        FROM part
        WHERE status = 1
        ORDER BY part_code
        """
        return db.query_dataframe(sql)
    
    def get_part_inventory_summary(self) -> pd.DataFrame:
        """
        获取零件库存汇总（按零件号聚合）
        
        Returns:
            pd.DataFrame: 零件库存汇总
        """
        sql = """
        SELECT 
            p.part_code,
            p.part_name,
            COALESCE(stock.total_stock, 0) as total_stock,
            COALESCE(stock.kanban_count, 0) as kanban_count,
            COALESCE(stock.warehouse_count, 0) as warehouse_count
        FROM part p
        LEFT JOIN (
            SELECT
                ci.part_code,
                SUM(ci.quantity) as total_stock,
                COUNT(DISTINCT ci.kanban_no) as kanban_count,
                COUNT(DISTINCT ci.warehouse_code) as warehouse_count
            FROM current_inventory ci
            LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no
            WHERE ci.quantity > 0
              AND (k.kanban_no IS NULL OR k.is_sealed = 0)
            GROUP BY ci.part_code
        ) stock ON p.part_code = stock.part_code
        LEFT JOIN (
            SELECT DISTINCT part_code
            FROM inventory_trace
            WHERE part_code IS NOT NULL
        ) trace ON p.part_code = trace.part_code
        WHERE p.status = 1
          AND (COALESCE(stock.total_stock, 0) > 0 OR trace.part_code IS NOT NULL)
        ORDER BY total_stock DESC, p.part_code
        """
        return db.query_dataframe(sql)
    
    def get_part_daily_usage(self, part_code: str, days: int = 30) -> pd.DataFrame:
        """
        获取零件每日出库量
        
        Args:
            part_code: 零件号
            days: 统计天数
            
        Returns:
            pd.DataFrame: 每日出库数据
        """
        sql = """
        SELECT 
            DATE(action_time) as action_date,
            SUM(quantity) as daily_outbound
        FROM inventory_trace
        WHERE part_code = :part_code
          AND action_type = 'OUTBOUND'
          AND action_time >= DATE_SUB(NOW(), INTERVAL :days DAY)
        GROUP BY DATE(action_time)
        ORDER BY action_date
        """
        return db.query_dataframe(sql, {'part_code': part_code, 'days': days})
    
    def get_all_parts(self) -> list:
        """
        获取所有零件（包括有库存或有出入库记录的）
        
        Returns:
            list: 零件列表
        """
        sql = """
        SELECT DISTINCT 
            p.part_code as partCode,
            p.part_name as partName,
            COALESCE(stock.current_stock, 0) as currentStock
        FROM part p
        LEFT JOIN (
            SELECT ci.part_code, SUM(ci.quantity) as current_stock
            FROM current_inventory ci
            LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no
            WHERE ci.quantity > 0
              AND (k.kanban_no IS NULL OR k.is_sealed = 0)
            GROUP BY ci.part_code
        ) stock ON p.part_code = stock.part_code
        LEFT JOIN (
            SELECT DISTINCT part_code
            FROM inventory_trace
            WHERE part_code IS NOT NULL
        ) trace ON p.part_code = trace.part_code
        WHERE p.status = 1
          AND (COALESCE(stock.current_stock, 0) > 0 OR trace.part_code IS NOT NULL)
        ORDER BY p.part_code
        """
        df = db.query_dataframe(sql)
        return df.to_dict('records')


# 全局数据服务实例
data_service = DataService()
