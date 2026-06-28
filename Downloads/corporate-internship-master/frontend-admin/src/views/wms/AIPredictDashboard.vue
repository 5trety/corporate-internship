<template>
  <div class="ai-predict-dashboard">
    <!-- 预警汇总卡片 -->
    <el-row :gutter="20" class="summary-cards">
      <el-col :xs="24" :sm="8">
        <el-card class="warning-card shortage" shadow="hover">
          <div class="card-content">
            <div class="card-icon">⚠️</div>
            <div class="card-info">
              <h3>缺货预警</h3>
              <div class="count">{{ summary.shortage?.count || 0 }}</div>
              <p>高风险: <span class="high">{{ summary.shortage?.high || 0 }}</span></p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="8">
        <el-card class="warning-card obsolete" shadow="hover">
          <div class="card-content">
            <div class="card-icon">📦</div>
            <div class="card-info">
              <h3>呆滞预警</h3>
              <div class="count">{{ summary.obsolete?.count || 0 }}</div>
              <p>高风险: <span class="high">{{ summary.obsolete?.high || 0 }}</span></p>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="8">
        <el-card class="warning-card fluctuation" shadow="hover">
          <div class="card-content">
            <div class="card-icon">📊</div>
            <div class="card-info">
              <h3>波动预警</h3>
              <div class="count">{{ summary.fluctuation?.count || 0 }}</div>
              <p>高风险: <span class="high">{{ summary.fluctuation?.high || 0 }}</span></p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 零件列表 -->
    <el-card class="parts-list" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="title">所有零件监控</span>
          <el-input 
            v-model="searchText" 
            placeholder="搜索零件号或名称" 
            style="width: 250px"
            clearable
            prefix-icon="Search"
          />
        </div>
      </template>
      
      <el-table :data="partsList" v-loading="loading" stripe>
        <el-table-column prop="partCode" label="零件号" width="120" fixed />
        <el-table-column prop="partName" label="零件名称" width="150" />
        <el-table-column prop="currentStock" label="当前库存" width="100" align="center" />
        <el-table-column label="预警状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.hasWarning" :type="row.riskLevel === 'HIGH' ? 'danger' : 'warning'" effect="dark">
              有预警 ({{ row.warningCount }})
            </el-tag>
            <el-tag v-else type="success" effect="dark">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警详情" width="180">
          <template #default="{ row }">
            <div v-if="row.hasWarning" class="warning-details">
              <el-tag 
                v-for="(w, idx) in row.warnings" 
                :key="idx"
                :type="getRiskTypeColor(w.riskType)" 
                size="small"
                class="warning-tag"
              >
                {{ getRiskTypeName(w.riskType) }}
              </el-tag>
            </div>
            <span v-else class="no-warning">-</span>
          </template>
        </el-table-column>
        <el-table-column label="建议" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.hasWarning && row.warnings.length > 0" class="recommendation">
              {{ row.warnings[0].recommendation }}
            </span>
            <span v-else class="no-warning">库存正常</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewTrend(row.partCode)">
              趋势
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && partsList.length === 0" description="暂无零件数据" />
    </el-card>

    <!-- 趋势图对话框 -->
    <el-dialog 
      v-model="trendDialogVisible" 
      :title="`${currentPartCode} - 出入库趋势预测（7天）`" 
      width="1000px"
      :close-on-click-modal="false"
    >
      <div v-loading="trendLoading" class="trend-chart-container">
        <div ref="trendChartRef" class="trend-chart"></div>
      </div>
      <template #footer>
        <el-button @click="trendDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { aiPredictAPI } from '@/api/aiPredict'
import * as echarts from 'echarts'

// 响应式数据
const summary = ref({})
const warnings = ref([])
const allParts = ref([])
const searchText = ref('')
const loading = ref(false)
const trendDialogVisible = ref(false)
const trendLoading = ref(false)
const trendChartRef = ref(null)
const currentPartCode = ref('')
let trendChart = null

// 计算属性 - 显示所有零件，合并预警信息
const partsList = computed(() => {
  // 将预警信息转为Map方便查找
  const warningMap = {}
  warnings.value.forEach(w => {
    if (!warningMap[w.partCode]) {
      warningMap[w.partCode] = []
    }
    warningMap[w.partCode].push(w)
  })
  
  // 合并所有零件和预警信息
  return allParts.value.map(part => {
    const partWarnings = warningMap[part.partCode] || []
    const hasWarning = partWarnings.length > 0
    const highRiskWarning = partWarnings.find(w => w.riskLevel === 'HIGH')
    
    return {
      ...part,
      hasWarning,
      warningCount: partWarnings.length,
      riskLevel: highRiskWarning ? 'HIGH' : (partWarnings.length > 0 ? 'MEDIUM' : 'NONE'),
      warnings: partWarnings
    }
  }).filter(part => {
    // 搜索过滤
    if (!searchText.value) return true
    const search = searchText.value.toLowerCase()
    return part.partCode.toLowerCase().includes(search) || 
           (part.partName && part.partName.toLowerCase().includes(search))
  })
})

// 方法
const loadSummary = async () => {
  try {
    const res = await aiPredictAPI.getDashboardSummary()
    if (res.code === 200) {
      summary.value = res.data
    }
  } catch (error) {
    console.error('加载汇总数据失败:', error)
  }
}

const loadWarnings = async () => {
  loading.value = true
  try {
    const res = await aiPredictAPI.getAllWarnings()
    if (res.code === 200) {
      warnings.value = res.data || []
    }
  } catch (error) {
    console.error('加载预警信息失败:', error)
  } finally {
    loading.value = false
  }
}

const loadAllParts = async () => {
  try {
    const res = await aiPredictAPI.getAllParts()
    if (res.code === 200) {
      allParts.value = res.data || []
    }
  } catch (error) {
    console.error('加载零件列表失败:', error)
  }
}

const viewTrend = async (partCode) => {
  currentPartCode.value = partCode
  trendDialogVisible.value = true
  trendLoading.value = true
  
  try {
    const res = await aiPredictAPI.getPredictionTrend(partCode)
    if (res.code === 200 && res.data) {
      await nextTick()
      renderTrendChart(res.data)
    } else {
      ElMessage.warning('无历史数据，无法生成趋势图')
      trendDialogVisible.value = false
    }
  } catch (error) {
    ElMessage.error('获取趋势图失败')
    console.error(error)
    trendDialogVisible.value = false
  } finally {
    trendLoading.value = false
  }
}

const renderTrendChart = (data) => {
  if (!data || !trendChartRef.value) {
    return
  }

  // 销毁旧图表
  if (trendChart) {
    trendChart.dispose()
  }
  
  // 初始化图表
  trendChart = echarts.init(trendChartRef.value)
  
  const { historical = [], predicted = [] } = data
  const predictionLabel = 'AI需求预测（出库量）'
  
  // 处理历史数据
  const histDates = historical.map(h => h.date)
  const histInbound = historical.map(h => h.inbound || 0)
  const histOutbound = historical.map(h => h.outbound || 0)
  const histInventory = historical.map(h => h.inventory)
  
  // 处理预测数据
  const predDates = predicted.map(p => p.date)
  const predValues = predicted.map(p => p.predicted)
  const predUpper = predicted.map(p => p.upper_bound)
  const predLower = predicted.map(p => p.lower_bound)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: { backgroundColor: '#6a7985' }
      },
      formatter: function(params) {
        let result = params[0].axisValue + '<br/>'
        params.forEach(param => {
          if (param.value !== null && param.value !== undefined) {
            result += `${param.marker}${param.seriesName}: ${param.value.toLocaleString()}<br/>`
          }
        })
        return result
      }
    },
    legend: {
      data: ['入库量', '出库量', '当前库存', predictionLabel],
      top: 10
    },
    grid: {
      left: '3%',
      right: '3%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: [...histDates, ...predDates],
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '出入库数量',
      position: 'left',
      axisLabel: {
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '入库量',
        type: 'line',
        data: [...histInbound, ...new Array(predDates.length).fill(null)],
        smooth: true,
        itemStyle: { color: '#4CAF50' },
        symbol: 'circle',
        symbolSize: 6
      },
      {
        name: '出库量',
        type: 'line',
        data: [...histOutbound, ...new Array(predDates.length).fill(null)],
        smooth: true,
        itemStyle: { color: '#FF9800' },
        symbol: 'circle',
        symbolSize: 6
      },
      {
        name: '当前库存',
        type: 'line',
        data: [...histInventory, ...new Array(predDates.length).fill(null)],
        smooth: true,
        itemStyle: { color: '#2196F3' },
        symbol: 'triangle',
        symbolSize: 8,
        lineStyle: { width: 3 }
      },
      {
        name: predictionLabel,
        type: 'line',
        data: [...new Array(histDates.length).fill(null), ...predValues],
        smooth: true,
        itemStyle: { color: '#F44336' },
        symbol: 'diamond',
        symbolSize: 8,
        lineStyle: { type: 'dashed', width: 3 },
        markArea: {
          data: [
            [
              {
                xAxis: histDates.length - 1,
                itemStyle: { color: 'rgba(244, 67, 54, 0.1)' }
              },
              {
                xAxis: histDates.length + predDates.length - 1
              }
            ]
          ]
        }
      }
    ]
  }
  
  trendChart.setOption(option)
  
  // 窗口调整时重绘
  window.addEventListener('resize', () => {
    trendChart?.resize()
  })
}

const getRiskTypeColor = (type) => {
  const colors = {
    'SHORTAGE': 'danger',
    'OBSOLETE': 'warning',
    'FLUCTUATION': 'info'
  }
  return colors[type] || ''
}

const getRiskTypeName = (type) => {
  const names = {
    'SHORTAGE': '缺货',
    'OBSOLETE': '呆滞',
    'FLUCTUATION': '波动'
  }
  return names[type] || type
}

const getRiskLevelColor = (level) => {
  const colors = {
    'HIGH': 'danger',
    'MEDIUM': 'warning',
    'LOW': 'success'
  }
  return colors[level] || ''
}

const getRiskLevelName = (level) => {
  const names = {
    'HIGH': '高',
    'MEDIUM': '中',
    'LOW': '低'
  }
  return names[level] || level
}

// 生命周期
onMounted(() => {
  loadSummary()
  loadWarnings()
  loadAllParts()
})
</script>

<style scoped>
.ai-predict-dashboard {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.summary-cards {
  margin-bottom: 20px;
}

.warning-card {
  cursor: pointer;
  transition: all 0.3s;
}

.warning-card:hover {
  transform: translateY(-5px);
}

.warning-card.shortage {
  border-left: 4px solid #f56c6c;
}

.warning-card.obsolete {
  border-left: 4px solid #e6a23c;
}

.warning-card.fluctuation {
  border-left: 4px solid #909399;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.card-icon {
  font-size: 48px;
}

.card-info h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #606266;
}

.card-info .count {
  font-size: 36px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.card-info p {
  margin: 10px 0 0 0;
  font-size: 14px;
  color: #909399;
}

.card-info .high {
  color: #f56c6c;
  font-weight: bold;
}

.info-cards {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item .label {
  font-size: 14px;
  color: #606266;
}

.info-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.parts-list {
  margin-top: 20px;
}

.warning-details {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.warning-tag {
  margin: 0;
}

.no-warning {
  color: #909399;
}

.recommendation {
  color: #606266;
  font-size: 13px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.trend-chart-container {
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.trend-chart {
  width: 100%;
  height: 500px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-predict-dashboard {
    padding: 10px;
  }
  
  .card-content {
    flex-direction: column;
    text-align: center;
  }
  
  .card-icon {
    font-size: 36px;
  }
  
  .card-info .count {
    font-size: 28px;
  }
}
</style>
