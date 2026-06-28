# test_db.py
import pymysql
from config import get_settings

settings = get_settings()

try:
    conn = pymysql.connect(
        host=settings.mysql_host,
        port=settings.mysql_port,
        user=settings.mysql_user,
        password=settings.mysql_password,
        database=settings.mysql_database
    )
    print("✅ 数据库连接成功！")
    conn.close()
except Exception as e:
    print(f"❌ 数据库连接失败：{e}")
    print("请检查 .env 文件配置是否正确")