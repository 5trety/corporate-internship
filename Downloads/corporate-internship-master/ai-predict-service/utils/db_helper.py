"""
数据库连接辅助类
"""
from sqlalchemy import create_engine, text
import pandas as pd
from typing import Dict, Optional
import config


class DatabaseHelper:
    """数据库连接助手"""
    
    def __init__(self):
        """初始化数据库连接池"""
        self.engine = create_engine(
            f"mysql+pymysql://{config.MYSQL_USER}:{config.MYSQL_PASSWORD}"
            f"@{config.MYSQL_HOST}:{config.MYSQL_PORT}/{config.MYSQL_DATABASE}",
            pool_size=10,
            max_overflow=20,
            pool_recycle=3600,
            pool_pre_ping=True
        )
    
    def query_dataframe(self, sql: str, params: Optional[Dict] = None) -> pd.DataFrame:
        """
        执行SQL查询返回DataFrame
        
        Args:
            sql: SQL查询语句
            params: 查询参数
            
        Returns:
            pd.DataFrame: 查询结果
        """
        with self.engine.connect() as conn:
            return pd.read_sql(text(sql), conn, params=params)
    
    def execute(self, sql: str, params: Optional[Dict] = None) -> int:
        """
        执行SQL语句（INSERT/UPDATE/DELETE）
        
        Args:
            sql: SQL语句
            params: 参数
            
        Returns:
            int: 影响的行数
        """
        with self.engine.connect() as conn:
            result = conn.execute(text(sql), params or {})
            conn.commit()
            return result.rowcount


# 全局数据库实例
db = DatabaseHelper()
