# AI需求预测与库存预警系统

基于机器学习的智能库存预测与预警微服务，与WMS系统独立部署。

## 功能特性

✅ **机器学习集成预测**
- 移动平均（30%）、线性回归（30%）、随机森林（40%）三种模型
- 自动加权集成，提高预测精度
- **固定7天**出入库趋势预测

✅ **智能预警系统**
- **缺货预警**：预测7天出库量 > 当前库存（使用出库量计算）
- **呆滞预警**：库存可维持时间 > 30天（使用平均日出库量计算）
- **波动预警**：检测出入库总量异常波动和趋势变化

✅ **可视化趋势图（ECharts交互式图表）**
- 前端ECharts渲染，支持鼠标悬停显示数据
- 双Y轴显示：左轴-出入库数量，右轴-当前库存
- 四条线分离显示：入库线、出库线、当前库存线、AI预测线
- 自动补齐7天内所有日期（无数据日期显示为0）

✅ **预警看板**
- 实时显示所有有库存的零件
- 按风险等级排序
- 提供处理建议
- 库存数据与WMS实时库存总览一致（过滤已封存看板）

## 快速开始

### 1. 环境要求

- **Python 3.8+**（已验证兼容Python 3.14）
- MySQL 5.7+
- 已部署的WMS系统（提供数据）

### 2. 安装

**Windows:**
```bash
# 双击运行启动脚本
start.bat

# 或手动执行
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

### 3. 配置

复制 `.env.example` 为 `.env` 并修改数据库配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件：
```env
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=your_password  # 修改为你的密码
MYSQL_DATABASE=backend_db
```

### 4. 启动服务

```bash
# Windows
start.bat

# Linux/Mac
./start.sh

# 或直接运行
uvicorn app:app --host 0.0.0.0 --port 8001 --reload
```

服务启动后访问：
- **API文档**: http://localhost:8001/docs
- **健康检查**: http://localhost:8001/health

## API接口

### 1. 获取所有预警

```http
GET /api/warning/all
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "partCode": "PART001",
      "partName": "零件A",
      "riskType": "SHORTAGE",
      "riskLevel": "HIGH",
      "currentStock": 50,
      "predictedDemand7d": 120.5,
      "daysRemaining": 2.9,
      "recommendation": "紧急！库存仅够2.9天，建议立即采购"
    }
  ],
  "total": 1
}
```

### 2. 获取零件预测趋势

```http
GET /api/predict/trend/PART001
```

**参数:**
- `part_code`: 零件号（路径参数）

**响应:**
返回历史和预测数据，固定7天

### 3. 获取趋势图

```http
GET /api/chart/trend/PART001
```

**参数:**
- `part_code`: 零件号（路径参数）

**响应:**
```json
{
  "code": 200,
  "data": {
    "image": "iVBORw0KGgoAAAANSUhEUgAA...",
    "contentType": "image/png"
  }
}
```

**注意:** 固定返回7天预测趋势图

### 4. 获取预警看板汇总

```http
GET /api/dashboard/summary
```

**响应:**
```json
{
  "code": 200,
  "data": {
    "shortage": {"count": 5, "high": 2, "medium": 2, "low": 1},
    "obsolete": {"count": 3, "high": 1, "medium": 1, "low": 1},
    "fluctuation": {"count": 2, "high": 0, "medium": 1, "low": 1},
    "inventory": {"totalParts": 50, "totalStock": 10000},
    "totalWarnings": 10,
    "highRiskWarnings": 3
  }
}
```

### 5. 获取所有零件

```http
GET /api/parts/all
```

**响应:**
```json
{
  "code": 200,
  "data": [
    {
      "partCode": "6666",
      "partName": "零件A",
      "currentStock": 100
    }
  ]
}
```

**说明:** 返回所有有库存的零件（current_inventory.quantity > 0）

## 预警规则

### 缺货预警（SHORTAGE）

**数据口径**：使用**出库量**（action_type='OUTBOUND'）计算

| 风险等级 | 条件 | 建议 |
|---------|------|------|
| HIGH | 当前库存 < 预测7天出库量 | 立即采购，库存告急 |
| MEDIUM | 预测7天出库量 ≤ 库存 < 安全库存 | 尽快补充，避免断货 |

**安全库存** = 预测7天出库量 × 1.5

**计算公式**：
```python
predicted_7d_outbound = sum(预测7天每日出库量)
days_remaining = current_stock / avg_daily_outbound  # 平均日出库量
```

### 呆滞预警（OBSOLETE）

**数据口径**：只检查有库存的零件（current_inventory.quantity > 0）

| 风险等级 | 条件 | 建议 |
|---------|------|------|
| HIGH | 库存可维持 > 60天 | 停止采购，促销处理 |
| MEDIUM | 30天 < 库存可维持 ≤ 60天 | 减少采购频率 |

**计算公式**：
```python
avg_daily_outbound = total_outbound / 30  # 近30天平均日出库量
days_to_consume = current_stock / avg_daily_outbound  # 库存可维持天数
```

### 波动预警（FLUCTUATION）

| 风险等级 | 条件 | 建议 |
|---------|------|------|
| HIGH | Z分数 > 3.0 | 关注供应链稳定性 |
| MEDIUM | Z分数 > 2.5 或 趋势变化强度 > 50% | 调整采购策略 |

## 技术架构

```
ai-predict-service/
├── app.py                   # FastAPI主应用
├── config.py                # 配置管理
├── requirements.txt         # Python依赖
├── models/                  # AI预测模型
│   ├── predictor.py         # 集成预测引擎（移动平均+线性回归+随机森林）
│   ├── feature_engineer.py  # 特征工程（时间特征、滞后特征、移动平均）
│   └── anomaly_detector.py  # 异常检测（波动异常、趋势变化）
├── services/                # 业务服务层
│   ├── data_service.py      # 数据获取服务（直连MySQL）
│   ├── warning_service.py   # 预警服务（缺货/呆滞/波动）
│   └── trend_service.py     # 趋势分析服务（预测+可视化）
├── utils/                   # 工具函数
│   └── db_helper.py         # 数据库连接池
└── .env                     # 环境变量配置
```

## 预测模型说明

### 智能降级策略

系统根据**7天内活跃天数**（有实际出入库数据的天数）自动调整模型权重：

| 7天内活跃天数 | 移动平均 | 线性回归 | 随机森林 | 说明 |
|--------------|---------|---------|---------|------|
| ≥5天 | 30% | 30% | 40% | 数据丰富，复杂模型为主 |
| 3-4天 | 50% | 30% | 20% | 数据适中，降低随机森林 |
| 1-2天 | 70% | 20% | 10% | 数据稀疏，主要依赖移动平均 |

**注意**：系统固定查询过去7天数据，根据这7天内有多少天有实际数据来调整策略

### 1. 移动平均（Moving Average）
- **权重**: 30%-70%（动态调整）
- **原理**: 基于历史7天平均值预测，考虑趋势调整
- **适用**: 所有场景，数据不足时的降级方案

### 2. 线性回归（Linear Regression）
- **权重**: 20%-30%（动态调整）
- **原理**: 学习时间特征与数量的线性关系
- **特征**: 星期、月份、是否周末、滞后特征、移动平均、趋势斜率
- **适用**: 有趋势性变化的场景

### 3. 随机森林（Random Forest）
- **权重**: 10%-40%（动态调整）
- **原理**: 集成100棵决策树，捕捉非线性关系
- **参数**: 最大深度10，使用所有CPU核心
- **适用**: 复杂模式、多维度特征、数据充足时

### 模式识别增强（数据≥7天启用）

当检测到明显模式时，使用 **60%模式预测 + 40%集成预测** 的混合策略：

| 模式 | 判定条件 | 预测方法 |
|------|---------|---------|
| 波动模式 | 方向变化≥4次 且 极差/均值≥1.5 | 周期循环预测 |
| 上升模式 | 斜率≥1.0 且 上升步数≥6 | 线性外推 |
| 下降模式 | 斜率≤-1.0 且 下降步数≥6 | 线性外推 |

**注意**：模式识别阈值已提高，避免稀疏数据误判

### 特征工程

提取的特征包括：
- **时间特征**: 星期、月份、是否周末、月份中的第几天
- **滞后特征**: 前1天、前3天、前7天的出入库量
- **统计特征**: 3天/7天移动平均、趋势斜率、二次趋势、7天标准差

## 前端集成

### 1. 添加路由

在 `frontend-admin/src/router/index.js` 中已添加：

```javascript
{
  path: 'ai-predict',
  name: 'ai-predict',
  component: () => import('../views/wms/AIPredictDashboard.vue'),
  meta: { title: 'AI需求预测' }
}
```

### 2. 添加菜单

在 `backend-demo/src/main/java/com/example/backend/controller/MenuController.java` 中已添加：

```java
Map<String, Object> aiPredict = new LinkedHashMap<>();
aiPredict.put("id", 12);
aiPredict.put("name", "AI需求预测");
aiPredict.put("path", "/ai-predict");
aiPredict.put("icon", "TrendCharts");
aiPredict.put("component", "AIPredictDashboard");
menus.add(aiPredict);
```

### 3. 访问页面

启动前端服务后，菜单中会出现 **"AI需求预测"** 入口。

## 常见问题

### Q1: 启动时提示“数据库连接失败”
**A:** 检查 `.env` 文件中的数据库配置是否正确，确保MySQL服务已启动。

### Q2: 预测结果为0或无数据
**A:** 确保零件有足够的历史出入库记录（至少14天）。

### Q3: 趋势图无法显示
**A:** 检查Python是否安装了 `matplotlib` 库，中文字体是否正常。

### Q4: 前端调用API跨域错误
**A:** FastAPI已配置CORS允许所有来源，如仍有问题检查防火墙设置。

### Q5: Excel导入零件号匹配失败
**A:** Excel可能将数字零件号（如6666）解析为Number类型，系统已自动转换为String类型进行匹配。确保Excel列名为“零件号”、“零件名称”、“数量”。

### Q6: 为什么只显示有库存的零件？
**A:** 系统只监控有库存的零件（current_inventory.quantity > 0），无库存的零件不需要预警。

### Q7: 预测天数可以修改吗？
**A:** 不可以。系统统一固定为7天预测，这是经过验证的最佳实践，确保数据新颗度和计算性能。

### Q8: 只有7天历史数据，预测准确吗？
**A:** 系统固定查询过去7天数据，根据这7天内**活跃天数**（有实际出入库的天数）自动调整：
- **≥5天活跃**：使用完整复杂模型（随机森林40%）
- **3-4天活跃**：降低随机森林权重，提高移动平均
- **1-2天活跃**：主要依赖移动平均（70%）
- 模式识别也需要至少4天活跃才启用

### Q9: 为什么我的零件预测出现“5, 0, 0, 63, 0, 0”这种模式？
**A:** 这是因为检测到了“波动模式”，系统认为这是周期性数据。已优化：
- 提高波动模式判定阈值（从3次变化提高到4次）
- 提高极差/均值阈值（从1.0提高到1.5）
- 模式预测改为混合策略（60%模式 + 40%集成），不再完全覆盖

## 性能优化建议

1. **定时预测**: 使用cron定时任务，每天凌晨自动预测所有零件
2. **缓存机制**: 缓存预测结果，减少重复计算
3. **数据库索引**: 为 `inventory_trace` 表的 `part_code` 和 `action_time` 添加索引
4. **异步处理**: 使用Celery异步任务队列处理大量零件预测

## 生产部署

### 使用Gunicorn + Uvicorn

```bash
pip install gunicorn

gunicorn app:app \
  -w 4 \
  -k uvicorn.workers.UvicornWorker \
  -b 0.0.0.0:8001 \
  --access-logfile access.log \
  --error-logfile error.log
```

### 使用Docker

```dockerfile
FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8001

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
```

## 许可证

MIT License

## 联系方式

如有问题或建议，请联系开发团队。
