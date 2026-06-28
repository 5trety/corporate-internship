"""
AI需求预测服务配置
"""
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 数据库配置
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.getenv("MYSQL_PORT", "3306"))
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "your_password")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "backend_db")

# 预警阈值（固定规则）
SHORTAGE_THRESHOLD_DAYS = 7      # 缺货阈值：7天
OBSOLETE_THRESHOLD_DAYS = 30     # 呆滞阈值：30天
SAFETY_STOCK_MULTIPLIER = 1.5    # 安全库存倍数

# 预测配置
PREDICT_DAYS = 7                 # 预测天数
MIN_HISTORY_DAYS = 14            # 最小历史数据天数
MODEL_ENSEMBLE_WEIGHTS = {
    'moving_avg': 0.3,
    'linear_reg': 0.3,
    'random_forest': 0.4
}

# 服务配置
SERVICE_HOST = os.getenv("SERVICE_HOST", "0.0.0.0")
SERVICE_PORT = int(os.getenv("SERVICE_PORT", "8001"))
