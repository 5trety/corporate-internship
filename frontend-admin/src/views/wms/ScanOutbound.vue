<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>扫码出库</h2>
    </div>

    <el-row :gutter="20" class="scan-layout">
      <!-- 左侧/上方：摄像头扫码 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="scanner-card">
          <template #header>
            <div class="card-header">
              <span>📷 摄像头扫码</span>
            </div>
          </template>
          <Scanner @decode="onScanDecode" />
        </el-card>

        <el-card shadow="never" class="order-card">
          <template #header>
            <div class="card-header">
              <span>📋 出库单信息</span>
            </div>
          </template>
          <el-form>
            <el-form-item label="出库单号">
              <el-select
                v-model="orderNo"
                placeholder="请选择出库单"
                filterable
                style="width: 100%"
                :disabled="!!orderInfo"
                size="large"
              >
                <el-option
                  v-for="order in pendingOrders"
                  :key="order.orderNo"
                  :label="`${order.orderNo} - ${order.outboundType}`"
                  :value="order.orderNo"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :disabled="!orderNo"
                @click="loadOrderInfo"
                style="width: 100%"
              >
                <el-icon><Search /></el-icon>
                加载出库单
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧/下方：手动输入 + 出库信息 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="input-card">
          <template #header>
            <div class="card-header">
              <span>⌨️ 手动输入</span>
            </div>
          </template>
          <el-form @submit.prevent="handleScan">
            <el-form-item label="扫描看板二维码">
              <el-input
                v-model="scanCode"
                placeholder="请扫描或输入看板号"
                size="large"
                ref="scanInput"
                @keyup.enter="handleScan"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" @click="handleScan" style="width: 100%">
                <el-icon><Search /></el-icon>
                验证看板
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="kanbanInfo"
            title="✅ 看板验证成功"
            type="success"
            show-icon
            :closable="false"
            class="kanban-alert"
          >
            <template #default>
              <div class="kanban-info">
                <div class="info-item">
                  <span class="label">零件号：</span>
                  <span class="value">{{ kanbanInfo.partCode }}</span>
                </div>
                <div class="info-item">
                  <span class="label">零件名称：</span>
                  <span class="value">{{ kanbanInfo.partName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">数量：</span>
                  <span class="value highlight">{{ kanbanInfo.quantity }}</span>
                </div>
                <div class="info-item">
                  <span class="label">供应商：</span>
                  <span class="value">{{ kanbanInfo.supplierName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">看板号：</span>
                  <span class="value code">{{ kanbanInfo.kanbanNo }}</span>
                </div>
                <div class="info-item">
                  <span class="label">库位：</span>
                  <span class="value">{{ kanbanInfo.locationCode }}</span>
                </div>
              </div>
            </template>
          </el-alert>
        </el-card>

        <el-card shadow="never" class="action-card" v-if="orderInfo && kanbanInfo">
          <template #header>
            <div class="card-header">
              <span>✅ 确认出库</span>
            </div>
          </template>
          <el-form>
            <el-form-item label="出库数量">
              <el-input-number
                v-model="outboundQuantity"
                :min="1"
                :max="availableOutboundQuantity || 1"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="success"
                size="large"
                @click="confirmOutbound"
                :loading="submitting"
                style="width: 100%"
              >
                <el-icon><Check /></el-icon>
                确认出库 (FIFO)
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Check } from '@element-plus/icons-vue'
import { validateOutboundKanban, validateOutboundOrder, scanOutbound, getOutboundOrderList } from '../../api/wms'
import Scanner from '../../components/Scanner.vue'

const scanInput = ref(null)
const scanCode = ref('')
const kanbanInfo = ref(null)
const orderNo = ref('')
const orderInfo = ref(null)
const pendingOrders = ref([])
const outboundQuantity = ref(1)
const submitting = ref(false)

const toNumber = (value) => Number(value || 0)

const getDetailPartCode = (detail) => detail?.part_code || detail?.partCode
const getDetailExpectedQuantity = (detail) => toNumber(detail?.expected_quantity ?? detail?.expectedQuantity)
const getDetailShippedQuantity = (detail) => toNumber(detail?.shipped_quantity ?? detail?.shippedQuantity)

const findOrderDetail = (partCode) => {
  const details = orderInfo.value?.details || []
  return details.find(detail => getDetailPartCode(detail) === partCode)
}

const getRemainingQuantity = (detail) => {
  if (!detail) return 0
  return getDetailExpectedQuantity(detail) - getDetailShippedQuantity(detail)
}

const getAvailableQuantity = (kanban) => {
  const stockQuantity = toNumber(kanban?.quantity)
  const detail = findOrderDetail(kanban?.partCode)
  if (!detail) return stockQuantity
  return Math.max(0, Math.min(stockQuantity, getRemainingQuantity(detail)))
}

const availableOutboundQuantity = computed(() => getAvailableQuantity(kanbanInfo.value))

const clampOutboundQuantity = () => {
  const maxQuantity = availableOutboundQuantity.value
  if (maxQuantity <= 0) {
    outboundQuantity.value = 1
    return
  }
  const nextQuantity = Math.max(1, toNumber(outboundQuantity.value) || 1)
  outboundQuantity.value = Math.min(nextQuantity, maxQuantity)
}

const ensureKanbanMatchesOrder = (kanban) => {
  if (!orderInfo.value || !kanban) return true

  if (kanban.orderNo && kanban.orderNo !== orderInfo.value.orderNo) {
    ElMessage.error(`看板属于出库单 ${kanban.orderNo}，请切换到对应出库单`)
    return false
  }

  const detail = findOrderDetail(kanban.partCode)
  if (!detail) {
    ElMessage.error(`零件 ${kanban.partCode} 不在当前出库单明细中`)
    return false
  }

  const remainingQuantity = getRemainingQuantity(detail)
  if (remainingQuantity <= 0) {
    ElMessage.error(`零件 ${kanban.partCode} 已完成出库，请扫描其他零件`)
    return false
  }

  return true
}

// 摄像头扫码回调
const onScanDecode = (result) => {
  if (result) {
    scanCode.value = result
    handleScan()
  }
}

const handleScan = async () => {
  if (!scanCode.value.trim()) {
    ElMessage.warning('请输入看板号')
    return
  }

  const res = await validateOutboundKanban(scanCode.value, orderNo.value)
  if (res.code === 200) {
    const scannedKanban = res.data
    if (scannedKanban.orderNo && !orderNo.value) {
      orderNo.value = scannedKanban.orderNo
      await loadOrderInfo({ silent: true })
    }
    if (!ensureKanbanMatchesOrder(scannedKanban)) {
      kanbanInfo.value = null
      return
    }
    kanbanInfo.value = scannedKanban
    outboundQuantity.value = getAvailableQuantity(scannedKanban)
    clampOutboundQuantity()
    ElMessage.success('看板验证成功')
    scanCode.value = ''
    nextTick(() => {
      scanInput.value?.focus()
    })
  } else {
    ElMessage.error(res.message || '看板无效或已使用')
    kanbanInfo.value = null
  }
}

const loadPendingOrders = async () => {
  try {
    const responses = await Promise.all([
      getOutboundOrderList({ status: 'pending', page: 1, pageSize: 100 }),
      getOutboundOrderList({ status: 'partial', page: 1, pageSize: 100 })
    ])
    pendingOrders.value = responses
      .filter(res => res.code === 200)
      .flatMap(res => res.data?.list || [])
  } catch (error) {
    console.error('加载出库单失败:', error)
  }
}

const loadOrderInfo = async (options = {}) => {
  const silent = options.silent === true
  if (!orderNo.value) {
    ElMessage.warning('请选择出库单')
    return
  }
  
  try {
    const res = await validateOutboundOrder(orderNo.value)
    if (res.code === 200) {
      const order = res.data
      if (order) {
        orderInfo.value = order
        if (!silent) {
          ElMessage.success('出库单加载成功')
        }
        if (kanbanInfo.value && !ensureKanbanMatchesOrder(kanbanInfo.value)) {
          kanbanInfo.value = null
          outboundQuantity.value = 1
        } else {
          clampOutboundQuantity()
        }
      } else {
        ElMessage.error('出库单不存在')
      }
    } else {
      orderInfo.value = null
      if (!silent) {
        ElMessage.error(res.message || '出库单无效或已完成')
      }
    }
  } catch (error) {
    console.error('加载出库单失败:', error)
    if (!silent) {
      ElMessage.error('加载出库单失败')
    }
  }
}

const confirmOutbound = async () => {
  if (!orderInfo.value) {
    ElMessage.warning('请先加载出库单')
    return
  }
  if (!kanbanInfo.value) {
    ElMessage.warning('请先扫描看板')
    return
  }
  if (!outboundQuantity.value || outboundQuantity.value <= 0) {
    ElMessage.warning('请输入出库数量')
    return
  }
  if (!ensureKanbanMatchesOrder(kanbanInfo.value)) {
    return
  }
  clampOutboundQuantity()

  submitting.value = true
  try {
    const res = await scanOutbound({
      orderNo: orderInfo.value.orderNo,
      kanbanNo: kanbanInfo.value.kanbanNo,
      partCode: kanbanInfo.value.partCode,
      quantity: outboundQuantity.value
    })
    
    if (res.code === 200) {
      ElMessage.success(`出库成功：${kanbanInfo.value.partName} x ${outboundQuantity.value}`)
      kanbanInfo.value = null
      outboundQuantity.value = 1
      scanCode.value = ''
      
      // 重新加载待出库订单列表
      await loadPendingOrders()
      await loadOrderInfo({ silent: true })
    } else {
      ElMessage.error(res.message || '出库失败')
    }
  } catch (error) {
    console.error('出库错误:', error)
    ElMessage.error('出库失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
    nextTick(() => {
      scanInput.value?.focus()
    })
  }
}

onMounted(() => {
  loadPendingOrders()
  nextTick(() => {
    scanInput.value?.focus()
  })
})
</script>

<style scoped>
.wms-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
}

.scan-layout {
  display: flex;
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.scanner-card,
.order-card,
.input-card,
.action-card {
  margin-bottom: 20px;
}

.kanban-alert {
  margin-top: 20px;
}

.kanban-info {
  padding: 12px 0;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e4e7ed;
  font-size: 14px;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #909399;
  min-width: 90px;
  font-weight: 500;
}

.info-item .value {
  color: #303133;
  flex: 1;
}

.info-item .value.highlight {
  color: #67c23a;
  font-weight: 700;
  font-size: 16px;
}

.info-item .value.code {
  font-family: 'Courier New', monospace;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .wms-page {
    padding: 12px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .scan-layout {
    flex-direction: column;
    gap: 16px;
  }

  .scanner-card,
  .order-card,
  .input-card,
  .action-card {
    margin-bottom: 16px;
  }

  .card-header {
    font-size: 15px;
  }

  /* 表单标签优化 */
  :deep(.el-form-item__label) {
    font-size: 14px;
  }

  /* 输入框和按钮优化 */
  :deep(.el-input__inner) {
    font-size: 16px;
  }

  :deep(.el-button) {
    min-height: 44px;
    font-size: 16px;
  }

  /* 看板信息优化 */
  .info-item {
    padding: 10px 0;
    font-size: 15px;
  }

  .info-item .label {
    min-width: 80px;
    font-size: 14px;
  }

  .info-item .value {
    font-size: 15px;
  }

  .info-item .value.highlight {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .wms-page {
    padding: 8px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .info-item .label {
    min-width: 70px;
    font-size: 13px;
  }

  .info-item .value {
    font-size: 14px;
  }
}
</style>
