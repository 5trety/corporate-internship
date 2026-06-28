<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>看板生命周期</h2>
    </div>

    <section class="trace-section">
      <el-form :inline="true" class="search-form">
        <el-form-item label="看板号">
          <el-input
            v-model="lifecycleKanbanNo"
            placeholder="请输入看板号"
            clearable
            style="width: 300px"
            @keyup.enter="loadLifecycle"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLifecycle">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="clearLifecycle">
            <el-icon><RefreshLeft /></el-icon>
            清空
          </el-button>
        </el-form-item>
      </el-form>

      <el-empty 
        v-if="!lifecycleData && !lifecycleLoading" 
        description="请输入看板号查询生命周期"
      />

      <div v-loading="lifecycleLoading" class="lifecycle-container" v-if="lifecycleLoading">
        <div style="height: 200px;"></div>
      </div>

      <template v-if="lifecycleData && !lifecycleLoading">
        <!-- 看板基本信息 -->
        <el-descriptions :column="2" border class="lifecycle-summary">
          <el-descriptions-item label="看板号">
            <span class="code">{{ lifecycleData.kanban.kanban_no }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getKanbanStatusType(lifecycleData.kanban.status)">
              {{ getKanbanStatusLabel(lifecycleData.kanban.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="零件号">{{ lifecycleData.kanban.part_code }}</el-descriptions-item>
          <el-descriptions-item label="零件名称">{{ lifecycleData.kanban.part_name }}</el-descriptions-item>
          <!-- 动态显示：出库显示客户，入库显示供应商 -->
          <el-descriptions-item :label="isOutbound ? '客户' : '供应商'">
            {{ getDisplayName() }}
          </el-descriptions-item>
          <el-descriptions-item label="仓库">
            {{ lifecycleData.kanban.warehouse_name || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="看板数量">{{ lifecycleData.kanban.quantity }}</el-descriptions-item>
          <el-descriptions-item label="当前库存">
            <span class="highlight">{{ lifecycleCurrentQuantity }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 流水记录 -->
        <el-divider>流水记录</el-divider>

        <div v-if="sortedTraces.length > 0" class="timeline-wrapper">
          <el-timeline class="lifecycle-timeline">
            <el-timeline-item
              v-for="(item, index) in sortedTraces"
              :key="item.id || item.trace_no || index"
              :timestamp="formatTimestamp(item.action_time)"
              :type="getActionTagType(item.action_type)"
              placement="top"
              size="large"
              :hollow="true"
            >
              <div class="timeline-item">
                <div class="timeline-title">
                  <el-tag :type="getActionTagType(item.action_type)" size="small">
                    <el-icon v-if="getActionIcon(item.action_type) === 'Upload'"><Upload /></el-icon>
                    <el-icon v-else-if="getActionIcon(item.action_type) === 'Download'"><Download /></el-icon>
                    <el-icon v-else><Refresh /></el-icon>
                    {{ getActionLabel(item.action_type) }}
                  </el-tag>
                  <span class="timeline-order">单据：{{ item.order_no || '-' }}</span>
                  <!-- 出库记录显示被扣减的入库看板号 -->
                  <span v-if="item.action_type === 'OUTBOUND' && item.inbound_kanban_no" class="timeline-inbound-kanban">
                    <el-icon><Collection /></el-icon>
                    扣减入库看板：{{ item.inbound_kanban_no }}
                  </span>
                </div>
                <div class="timeline-details">
                  <div class="timeline-line">
                    <span class="timeline-label">零件：</span>
                    <span>{{ item.part_code }}</span>
                    <span class="timeline-separator">|</span>
                    <span class="timeline-label">数量：</span>
                    <span class="timeline-quantity">{{ item.quantity }}</span>
                  </div>
                  <div class="timeline-line">
                    <span class="timeline-label">{{ isOutbound ? '客户' : '供应商' }}：</span>
                    <span>{{ getTraceDisplayName(item) }}</span>
                    <span class="timeline-separator">|</span>
                    <span class="timeline-label">仓库：</span>
                    <span>{{ item.warehouse_code || item.warehouse_name || '-' }}</span>
                  </div>
                  <div class="timeline-line">
                    <span class="timeline-label">操作人：</span>
                    <span>{{ item.operator || '-' }}</span>
                  </div>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <el-empty v-else description="该看板暂无流水记录" />
      </template>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Upload, Download, Collection } from '@element-plus/icons-vue'
import { getKanbanLifecycle } from '../../api/wms'

const route = useRoute()
const router = useRouter()

const lifecycleKanbanNo = ref('')
const lifecycleData = ref(null)
const lifecycleLoading = ref(false)

// ============================================
// Computed: Check if this is an outbound kanban
// ============================================
const isOutbound = computed(() => {
  if (!lifecycleData.value?.kanban) return false
  const orderNo = lifecycleData.value.kanban.order_no
  return orderNo && orderNo.startsWith('SO-')
})

// ============================================
// Computed: Current inventory quantity
// ============================================
const lifecycleCurrentQuantity = computed(() => {
  if (!lifecycleData.value) return 0
  if (lifecycleData.value.currentInventory && lifecycleData.value.currentInventory.length > 0) {
    return lifecycleData.value.currentInventory.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  }
  return 0
})

// ============================================
// Computed: Sorted traces (newest first)
// ============================================
const sortedTraces = computed(() => {
  if (!lifecycleData.value?.traces) return []
  return [...lifecycleData.value.traces].sort((a, b) => {
    const timeA = a.action_time ? new Date(a.action_time).getTime() : 0
    const timeB = b.action_time ? new Date(b.action_time).getTime() : 0
    return timeB - timeA
  })
})

// ============================================
// Helper: Get display name for kanban
// ============================================
const getDisplayName = () => {
  if (!lifecycleData.value?.kanban) return '-'
  const kanban = lifecycleData.value.kanban
  if (isOutbound.value) {
    return kanban.customer_name || kanban.supplier_name || '-'
  }
  return kanban.supplier_name || '-'
}

// ============================================
// Helper: Get action type label
// ============================================
const getActionLabel = (type) => {
  if (type === 'INBOUND') return '入库'
  if (type === 'OUTBOUND') return '出库'
  if (type === 'TRANSFER_SPLIT') return '转包拆分'
  if (type === 'TRANSFER_MERGE') return '转包合并'
  if (type === 'TRANSFER') return '转包'
  return type || '-'
}

// ============================================
// Helper: Get action tag type
// ============================================
const getActionTagType = (type) => {
  if (type === 'INBOUND') return 'success'
  if (type === 'OUTBOUND') return 'warning'
  if (type === 'TRANSFER_SPLIT') return 'primary'
  if (type === 'TRANSFER_MERGE') return 'success'
  if (type === 'TRANSFER') return 'info'
  return 'info'
}

// ============================================
// Helper: Get action icon
// ============================================
const getActionIcon = (type) => {
  if (type === 'INBOUND') return 'Upload'
  if (type === 'OUTBOUND') return 'Download'
  if (type === 'TRANSFER_SPLIT' || type === 'TRANSFER_MERGE') return 'Refresh'
  return 'Document'
}

// ============================================
// Helper: Get display name for trace item
// ============================================
const getTraceDisplayName = (item) => {
  if (!item) return '-'
  if (isOutbound.value) {
    return item.customer_name || item.supplier_name || '-'
  }
  return item.supplier_name || '-'
}

// ============================================
// Helper: Format timestamp
// ============================================
const formatTimestamp = (timestamp) => {
  if (!timestamp) return '-'
  try {
    const date = new Date(timestamp)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    return timestamp
  }
}

// ============================================
// Helper: Get kanban status label
// ============================================
const getKanbanStatusLabel = (status) => {
  const labels = {
    pending: '待入库',
    scanned: '已扫码',
    stored: '在库',
    outbound: '已出库'
  }
  return labels[status] || status || '-'
}

// ============================================
// Helper: Get kanban status tag type
// ============================================
const getKanbanStatusType = (status) => {
  const types = {
    pending: 'warning',
    scanned: 'info',
    stored: 'success',
    outbound: 'danger'
  }
  return types[status] || 'info'
}

// ============================================
// Load lifecycle data
// ============================================
const loadLifecycle = async () => {
  const kanbanNo = lifecycleKanbanNo.value.trim()
  if (!kanbanNo) {
    ElMessage.warning('请输入看板号')
    return
  }

  lifecycleLoading.value = true
  lifecycleData.value = null

  try {
    console.log('🔍 查询看板生命周期:', kanbanNo)
    const res = await getKanbanLifecycle(kanbanNo)
    console.log('📥 响应数据:', res)
    
    if (res.code === 200) {
      if (res.data?.kanban) {
        const traceCount = res.data.traces?.length || 0
        const status = res.data.kanban.status
        const isOutboundOrder = res.data.kanban.order_no?.startsWith('SO-')
        
        lifecycleData.value = {
          kanban: res.data.kanban || {},
          currentInventory: res.data.currentInventory || [],
          traces: res.data.traces || []
        }
        
        ElMessage.success(`找到看板 ${kanbanNo} 的生命周期数据 (${traceCount} 条记录，${isOutboundOrder ? '出库' : '入库'}单)`)
      } else {
        ElMessage.warning('未找到该看板')
      }
    } else {
      ElMessage.error(res.message || '查询失败')
    }
  } catch (error) {
    console.error('❌ 查询生命周期失败:', error)
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    lifecycleLoading.value = false
  }
}

// ============================================
// Clear lifecycle data
// ============================================
const clearLifecycle = () => {
  // 跳转回全部流水页面
  router.push('/inventory-trace')
}

// ============================================
// Check route params on mount
// ============================================
onMounted(() => {
  if (route.query.kanbanNo) {
    lifecycleKanbanNo.value = route.query.kanbanNo
    loadLifecycle()
  }
})
</script>

<style scoped>
.wms-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
}

.trace-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
}

.search-form {
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 0;
}

.lifecycle-container {
  min-height: 200px;
}

.lifecycle-summary {
  margin-bottom: 20px;
}

.lifecycle-summary .code {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.lifecycle-summary .highlight {
  color: #e6a23c;
  font-weight: 600;
  font-size: 16px;
}

.timeline-wrapper {
  padding: 8px 0;
}

.lifecycle-timeline {
  padding: 0;
}

.timeline-item {
  background: #fafbfc;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 8px;
}

.timeline-item:hover {
  background: #f0f2f5;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.timeline-order {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.timeline-inbound-kanban {
  font-size: 13px;
  color: #67c23a;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f0f9ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.timeline-details {
  padding-left: 4px;
}

.timeline-line {
  color: #606266;
  line-height: 1.8;
  font-size: 14px;
}

.timeline-label {
  color: #909399;
}

.timeline-separator {
  color: #dcdfe6;
  margin: 0 8px;
}

.timeline-quantity {
  font-weight: 600;
  color: #303133;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .wms-page {
    padding: 12px;
  }

  .trace-section {
    padding: 12px;
  }

  .search-form {
    display: block;
  }

  .search-form :deep(.el-form-item) {
    margin-bottom: 12px;
    width: 100%;
  }

  .search-form :deep(.el-input) {
    width: 100% !important;
  }

  .timeline-item {
    padding: 10px 12px;
  }

  .timeline-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .timeline-line {
    font-size: 13px;
    line-height: 1.6;
  }

  .lifecycle-summary :deep(.el-descriptions__body .el-descriptions__table) {
    display: block;
  }

  .lifecycle-summary :deep(.el-descriptions__body .el-descriptions__table .el-descriptions__row) {
    display: flex;
    flex-direction: column;
  }

  .lifecycle-summary :deep(.el-descriptions__body .el-descriptions__table .el-descriptions__cell) {
    display: flex;
    padding: 6px 10px;
  }
}

@media (max-width: 480px) {
  .wms-page {
    padding: 8px;
  }

  .trace-section {
    padding: 8px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .timeline-line {
    font-size: 12px;
  }

  .timeline-item {
    padding: 8px 10px;
  }
}
</style>