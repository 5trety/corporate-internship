<template>
  <div class="wms-page outbound-detail">
    <div class="page-header">
      <h2>出库单详情</h2>
      <el-button @click="$router.back()">
        <el-icon><Back /></el-icon>
        返回
      </el-button>
    </div>

    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag :type="getStatusType(orderData.status)" size="large">
              {{ getStatusText(orderData.status) }}
            </el-tag>
          </div>
        </template>

        <el-descriptions :column="3" border>
          <el-descriptions-item label="出库单号">
            <strong>{{ orderData.order_no }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="出库类型">
            {{ orderData.outbound_type }}
          </el-descriptions-item>
          <el-descriptions-item label="客户编码">
            {{ orderData.customer_code }}
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ orderData.customer_name }}
          </el-descriptions-item>
          <el-descriptions-item label="仓库">
            {{ orderData.warehouse_name }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ orderData.created_by || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ orderData.created_at }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ orderData.updated_at }}
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ orderData.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 出库进度 -->
        <div class="progress-section">
          <h4>出库进度</h4>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8">
              <div class="progress-item">
                <div class="progress-label">数量进度</div>
                <div class="progress-value">
                  <span class="shipped">{{ orderData.shipped_quantity || 0 }}</span>
                  <span class="separator">/</span>
                  <span class="total">{{ orderData.total_quantity || 0 }}</span>
                  <span class="unit">件</span>
                </div>
                <el-progress
                  :percentage="getQuantityProgress()"
                  :status="getProgressStatus(orderData.status)"
                  :stroke-width="12"
                />
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="progress-item">
                <div class="progress-label">箱数进度</div>
                <div class="progress-value">
                  <span class="shipped">{{ orderData.shipped_boxes || 0 }}</span>
                  <span class="separator">/</span>
                  <span class="total">{{ orderData.total_boxes || 0 }}</span>
                  <span class="unit">箱</span>
                </div>
                <el-progress
                  :percentage="getBoxesProgress()"
                  :status="getProgressStatus(orderData.status)"
                  :stroke-width="12"
                />
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="progress-item">
                <div class="progress-label">明细进度</div>
                <div class="progress-value">
                  <span class="shipped">{{ completedParts }}</span>
                  <span class="separator">/</span>
                  <span class="total">{{ details.length }}</span>
                  <span class="unit">种零件</span>
                </div>
                <el-progress
                  :percentage="getPartsProgress()"
                  :status="getProgressStatus(orderData.status)"
                  :stroke-width="12"
                />
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 出库明细 -->
      <el-card class="detail-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>出库明细</span>
            <div class="header-actions">
              <el-button type="primary" @click="handlePrint">
                <el-icon><Printer /></el-icon>
                打印出库单
              </el-button>
              <el-button
                type="success"
                @click="handleScanOutbound"
                v-if="orderData.status !== 'completed'"
              >
                <el-icon><Scanner /></el-icon>
                扫码出库
              </el-button>
            </div>
          </div>
        </template>

        <el-table :data="details" border stripe>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="part_code" label="零件号" width="150" />
          <el-table-column prop="part_name" label="零件名称" width="200" />
          <el-table-column label="数量" width="200">
            <template #default="{ row }">
              <div class="quantity-cell">
                <span class="shipped">{{ row.shipped_quantity || 0 }}</span>
                <span class="separator">/</span>
                <span class="expected">{{ row.expected_quantity }}</span>
                <span class="unit">件</span>
              </div>
              <el-progress
                :percentage="getDetailProgress(row)"
                :stroke-width="8"
                :show-text="false"
              />
            </template>
          </el-table-column>
          <el-table-column prop="packaging_capacity" label="包装容量" width="100" align="center">
            <template #default="{ row }">
              {{ row.packaging_capacity || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="箱数" width="200">
            <template #default="{ row }">
              <div class="quantity-cell">
                <span class="shipped">{{ row.shipped_boxes || 0 }}</span>
                <span class="separator">/</span>
                <span class="expected">{{ row.expected_boxes }}</span>
                <span class="unit">箱</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center">
            <template #default>
              <el-tag size="small">个</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getDetailStatusType(row)" size="small">
                {{ getDetailStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 汇总信息 -->
        <div class="summary-section">
          <el-row :gutter="20">
            <el-col :xs="12" :sm="6">
              <div class="summary-item">
                <span class="label">总零件种数</span>
                <span class="value">{{ details.length }}</span>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="summary-item">
                <span class="label">总数量</span>
                <span class="value">{{ orderData.total_quantity || 0 }}</span>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="summary-item">
                <span class="label">已出库数量</span>
                <span class="value shipped">{{ orderData.shipped_quantity || 0 }}</span>
              </div>
            </el-col>
            <el-col :xs="12" :sm="6">
              <div class="summary-item">
                <span class="label">剩余数量</span>
                <span class="value remaining">{{ remainingQuantity }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 流转记录 -->
      <el-card class="trace-card" shadow="never" v-if="traces.length > 0">
        <template #header>
          <span>流转记录</span>
        </template>

        <el-timeline>
          <el-timeline-item
            v-for="(trace, index) in traces"
            :key="index"
            :timestamp="trace.action_time"
            :type="getTraceType(trace.action_type)"
            placement="top"
          >
            <el-card shadow="never">
              <h4>{{ trace.action_type_text || getActionText(trace.action_type) }}</h4>
              <p>
                <strong>零件：</strong>{{ trace.part_code }} - {{ trace.part_name }}<br />
                <strong>数量：</strong>{{ trace.quantity }}<br />
                <strong>操作人：</strong>{{ trace.operator }}<br />
                <strong v-if="trace.remark">备注：</strong>{{ trace.remark }}
              </p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Printer, Scanner } from '@element-plus/icons-vue'
import { getOutboundOrderDetail, getTraceByKanban } from '../../api/wms'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orderData = ref({})
const details = ref([])
const traces = ref([])

const completedParts = computed(() => {
  return details.value.filter(d => d.shipped_quantity >= d.expected_quantity).length
})

const remainingQuantity = computed(() => {
  return (orderData.value.total_quantity || 0) - (orderData.value.shipped_quantity || 0)
})

const getStatusType = (status) => {
  const types = {
    pending: 'warning',
    partial: 'primary',
    completed: 'success'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    pending: '待出库',
    partial: '部分出库',
    completed: '已完成'
  }
  return texts[status] || status
}

const getProgressStatus = (status) => {
  if (status === 'completed') return 'success'
  if (status === 'partial') return 'primary'
  return ''
}

const getQuantityProgress = () => {
  if (!orderData.value.total_quantity) return 0
  return Math.round((orderData.value.shipped_quantity || 0) / orderData.value.total_quantity * 100)
}

const getBoxesProgress = () => {
  if (!orderData.value.total_boxes) return 0
  return Math.round((orderData.value.shipped_boxes || 0) / orderData.value.total_boxes * 100)
}

const getPartsProgress = () => {
  if (!details.value.length) return 0
  return Math.round(completedParts.value / details.value.length * 100)
}

const getDetailProgress = (row) => {
  if (!row.expected_quantity) return 0
  return Math.round((row.shipped_quantity || 0) / row.expected_quantity * 100)
}

const getDetailStatusType = (row) => {
  if (row.shipped_quantity >= row.expected_quantity) return 'success'
  if (row.shipped_quantity > 0) return 'warning'
  return 'info'
}

const getDetailStatusText = (row) => {
  if (row.shipped_quantity >= row.expected_quantity) return '已完成'
  if (row.shipped_quantity > 0) return '部分出库'
  return '待出库'
}

const getTraceType = (type) => {
  const types = {
    INBOUND: 'success',
    OUTBOUND: 'primary',
    MOVE: 'warning'
  }
  return types[type] || 'info'
}

const getActionText = (type) => {
  const texts = {
    INBOUND: '入库',
    OUTBOUND: '出库',
    MOVE: '移库'
  }
  return texts[type] || type
}

const loadData = async () => {
  loading.value = true
  const orderNo = route.params.orderNo

  try {
    const res = await getOutboundOrderDetail(orderNo)
    if (res.code === 200) {
      orderData.value = res.data?.order || {}
      details.value = res.data?.details || []

      // 加载追溯记录
      await loadTraces()
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadTraces = async () => {
  try {
    // 获取所有涉及该出库单的追溯记录
    // 这里简化处理，实际可能需要后端提供专门的接口
    const kanbanNos = new Set()
    // 如果有看板号信息，可以查询
    for (const detail of details.value) {
      // 暂时留空，后续可以通过后端接口获取
    }
  } catch (error) {
    console.error('加载追溯记录失败:', error)
  }
}

const handlePrint = () => {
  router.push(`/wms-outbound/print/${orderData.value.order_no}`)
}

const handleScanOutbound = () => {
  router.push(`/wms-outbound/scan-outbound?orderNo=${orderData.value.order_no}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.outbound-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.info-card {
  margin-bottom: 20px;
}

.progress-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.progress-section h4 {
  margin: 0 0 16px 0;
}

.progress-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.progress-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.progress-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 12px;
}

.progress-value .shipped {
  color: #409eff;
}

.progress-value .separator {
  color: #c0c4cc;
  margin: 0 4px;
}

.progress-value .total {
  color: #303133;
}

.progress-value .unit {
  font-size: 14px;
  color: #909399;
  margin-left: 4px;
}

.detail-card {
  margin-bottom: 20px;
}

.quantity-cell {
  font-size: 16px;
  margin-bottom: 4px;
}

.quantity-cell .shipped {
  color: #409eff;
  font-weight: bold;
}

.quantity-cell .separator {
  color: #c0c4cc;
  margin: 0 4px;
}

.quantity-cell .expected {
  color: #303133;
  font-weight: bold;
}

.quantity-cell .unit {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.summary-section {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-item {
  text-align: center;
}

.summary-item .label {
  display: block;
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.summary-item .value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.summary-item .value.shipped {
  color: #409eff;
}

.summary-item .value.remaining {
  color: #e6a23c;
}

.trace-card {
  margin-top: 20px;
}

.trace-card h4 {
  margin: 0 0 10px 0;
}

.trace-card p {
  margin: 0;
  line-height: 1.8;
  color: #606266;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions .el-button {
    width: 100%;
  }
}
</style>
