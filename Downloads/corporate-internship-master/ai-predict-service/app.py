"""
AI需求预测与库存预警系统 - FastAPI主应用
"""
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import config
from services.warning_service import warning_service
from services.trend_service import trend_service
from services.data_service import data_service

# 创建FastAPI应用
app = FastAPI(
    title="AI需求预测与库存预警系统",
    description="基于机器学习的库存预测与预警服务",
    version="1.0.0"
)

# CORS配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 数据模型
class PredictionResponse(BaseModel):
    success: bool
    partCode: Optional[str] = None
    confidence: Optional[float] = None
    historical: list = []
    predicted: list = []
    message: Optional[str] = None


class WarningItem(BaseModel):
    partCode: str
    partName: str
    riskType: str
    riskLevel: str
    currentStock: int
    recommendation: str


class DashboardSummary(BaseModel):
    shortage: dict
    obsolete: dict
    fluctuation: dict
    inventory: dict
    totalWarnings: int
    highRiskWarnings: int


# ==================== API接口 ====================

@app.get("/")
async def root():
    """根路径 - 服务信息"""
    return {
        "service": "AI需求预测与库存预警系统",
        "version": "1.0.0",
        "status": "running",
        "endpoints": [
            "/api/warning/all",
            "/api/warning/part/{part_code}",
            "/api/predict/trend/{part_code}",
            "/api/chart/trend/{part_code}",
            "/api/dashboard/summary"
        ]
    }


@app.get("/api/warning/all")
async def get_all_warnings():
    """
    获取所有预警信息
    
    Returns:
        List[Dict]: 预警列表，按风险等级排序
    """
    try:
        warnings = warning_service.generate_all_warnings()
        return {
            "code": 200,
            "message": "success",
            "data": warnings,
            "total": len(warnings)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取预警信息失败: {str(e)}")


@app.get("/api/parts/all")
async def get_all_parts_for_monitor():
    """
    获取所有有数据的零件列表
    
    Returns:
        List[Dict]: 零件列表
    """
    try:
        parts = data_service.get_all_parts()
        return {
            "code": 200,
            "message": "success",
            "data": parts,
            "total": len(parts)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取零件列表失败: {str(e)}")


@app.get("/api/warning/part/{part_code}")
async def get_part_warnings(part_code: str):
    """
    获取指定零件的预警信息
    
    Args:
        part_code: 零件号
        
    Returns:
        List[Dict]: 该零件的预警列表
    """
    try:
        warnings = warning_service.get_part_warnings(part_code)
        return {
            "code": 200,
            "message": "success",
            "data": warnings,
            "total": len(warnings)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取零件预警失败: {str(e)}")


@app.get("/api/predict/trend/{part_code}")
async def get_prediction_trend(part_code: str):
    """
    获取零件预测趋势数据（固定7天）
    
    Args:
        part_code: 零件号
        
    Returns:
        Dict: 历史和预测趋势数据
    """
    try:
        trend_data = trend_service.generate_prediction_trend(part_code, predict_days=7)
        
        if not trend_data['success']:
            return {
                "code": 404,
                "message": trend_data['message'],
                "data": None
            }
        
        return {
            "code": 200,
            "message": "success",
            "data": trend_data
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取预测趋势失败: {str(e)}")


@app.get("/api/chart/trend/{part_code}")
async def get_trend_chart(part_code: str):
    """
    获取趋势图（Base64编码的PNG图片，固定7天）
    
    Args:
        part_code: 零件号
        
    Returns:
        Dict: Base64编码的图片数据
    """
    try:
        img_base64 = trend_service.generate_trend_chart(part_code, predict_days=7)
        
        if not img_base64:
            return {
                "code": 404,
                "message": "无历史数据，无法生成趋势图",
                "data": None
            }
        
        return {
            "code": 200,
            "message": "success",
            "data": {
                "image": img_base64,
                "contentType": "image/png"
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成趋势图失败: {str(e)}")


@app.get("/api/dashboard/summary")
async def get_dashboard_summary():
    """
    获取预警看板汇总数据
    
    Returns:
        Dict: 汇总统计信息
    """
    try:
        summary = trend_service.get_dashboard_summary()
        return {
            "code": 200,
            "message": "success",
            "data": summary
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取看板汇总失败: {str(e)}")


@app.get("/api/parts")
async def get_all_parts():
    """
    获取所有零件列表
    
    Returns:
        List[Dict]: 零件列表
    """
    try:
        parts = data_service.get_all_parts()
        return {
            "code": 200,
            "message": "success",
            "data": parts
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取零件列表失败: {str(e)}")


@app.get("/api/inventory/summary")
async def get_inventory_summary():
    """
    获取库存汇总信息
    
    Returns:
        List[Dict]: 零件库存汇总
    """
    try:
        summary = data_service.get_part_inventory_summary()
        return {
            "code": 200,
            "message": "success",
            "data": summary.to_dict('records')
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取库存汇总失败: {str(e)}")


@app.get("/health")
async def health_check():
    """健康检查接口"""
    return {
        "status": "healthy",
        "service": "ai-predict-service",
        "timestamp": "2026-06-26T14:30:00Z"
    }


# 启动服务
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app:app",
        host=config.SERVICE_HOST,
        port=config.SERVICE_PORT,
        reload=True,
        log_level="info"
    )
