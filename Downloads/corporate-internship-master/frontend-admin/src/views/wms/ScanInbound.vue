<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>看板扫码入库</h2>
    </div>

    <!-- 模式切换 -->
    <el-tabs v-model="currentMode" type="card" class="mode-tabs" style="margin-bottom: 20px">
      <el-tab-pane label="📷 单件扫码入库" name="single" />
      <el-tab-pane label="🚀 批量快速入库" name="batch" />
    </el-tabs>

    <!-- 单件扫码模式 -->
    <template v-if="currentMode === 'single'">
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

        <el-card shadow="never" class="location-card">
          <template #header>
            <div class="card-header">
              <span>✅ 快速入库</span>
            </div>
          </template>
          <el-form>
            <el-alert
              v-if="kanbanInfo"
              :title="`仓库：${kanbanInfo.warehouseName || kanbanInfo.warehouseCode || '自动分配'}`"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom: 16px"
            />
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :disabled="!kanbanInfo || !inboundQuantity || inboundQuantity <= 0"
                @click="confirmInbound"
                :loading="submitting"
                style="width: 100%"
              >
                <el-icon><Check /></el-icon>
                确认入库 ({{ inboundQuantity || 0 }})
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧/下方：手动输入 + 看板信息 -->
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
            :title="'✅ 看板验证成功'"
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
                  <span class="label">供应商：</span>
                  <span class="value">{{ kanbanInfo.supplierName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">看板总数量：</span>
                  <span class="value highlight">{{ kanbanInfo.quantity }}</span>
                </div>
                <div class="info-item">
                  <span class="label">已入库数量：</span>
                  <span class="value">{{ kanbanInfo.receivedQuantity || 0 }}</span>
                </div>
                <div class="info-item">
                  <span class="label">剩余数量：</span>
                  <span class="value highlight-warning">{{ remainingQuantity }}</span>
                </div>
                <div class="info-item">
                  <span class="label">入库单号：</span>
                  <span class="value">{{ kanbanInfo.orderNo }}</span>
                </div>
                <div class="info-item">
                  <span class="label">看板号：</span>
                  <span class="value code">{{ kanbanInfo.kanbanNo }}</span>
                </div>
              </div>
              
              <el-divider style="margin: 12px 0">入库数量</el-divider>
              <div class="quantity-input-wrapper">
                <el-input-number
                  v-model="inboundQuantity"
                  :min="1"
                  :max="remainingQuantity"
                  size="large"
                  controls-position="right"
                  style="width: 100%"
                  @change="validateQuantity"
                />
                <div class="quantity-hint">
                  可入库范围：1 ~ {{ remainingQuantity }}
                  <span v-if="remainingQuantity === 0" class="hint-warning">
                    ⚠️ 该看板已全部入库完成
                  </span>
                  <span v-else class="hint-info">
                    💡 可以多次分批入库，每次输入不同数量
                  </span>
                </div>
              </div>
            </template>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>

    <!-- 入库记录 -->
    <el-card shadow="never" class="history-card" v-if="inboundHistory.length > 0">
      <template #header>
        <div class="card-header">
          <span>📋 入库记录</span>
          <el-tag type="info" size="small">共 {{ inboundHistory.length }} 次入库</el-tag>
        </div>
      </template>
      <el-table :data="inboundHistory" stripe size="small" max-height="200">
        <el-table-column prop="index" label="#" width="50" type="index" />
        <el-table-column prop="quantity" label="入库数量" width="120" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="time" label="入库时间" min-width="180" />
        <el-table-column prop="remainingAfter" label="剩余数量" width="120" />
      </el-table>
    </el-card>
    </template>

    <!-- 批量快速入库模式 -->
    <template v-if="currentMode === 'batch'">
      <el-card shadow="never" class="batch-card">
        <template #header>
          <div class="card-header">
            <span>🚀 批量快速入库</span>
          </div>
        </template>

        <!-- 显示未入库看板统计 -->
        <el-alert
          v-if="batchKanbanList.length > 0"
          :title="`共有 ${batchKanbanList.length} 个看板待入库，已选择 ${selectedKanbanNos.length} 个`"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />

        <el-alert
          v-else
          title="当前没有待入库的看板"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />

        <!-- 批量操作按钮 -->
        <el-space style="margin-bottom: 16px" wrap>
          <el-button
            type="success"
            size="large"
            :disabled="selectedKanbanNos.length === 0 || batchSubmitting"
            @click="batchInboundSelected"
            :loading="batchSubmitting"
          >
            <el-icon><Finished /></el-icon>
            一键入库已选择 ({{ selectedKanbanNos.length }})
          </el-button>
          <el-button
            type="primary"
            size="large"
            :disabled="batchKanbanList.length === 0 || batchSubmitting"
            @click="selectAll"
          >
            <el-icon><Select /></el-icon>
            全选
          </el-button>
          <el-button
            size="large"
            :disabled="batchKanbanList.length === 0"
            @click="clearSelection"
          >
            清空选择
          </el-button>
          <el-button
            size="large"
            @click="loadPendingKanbans"
            :loading="loadingKanbans"
          >
            <el-icon><Refresh /></el-icon>
            刷新列表
          </el-button>
        </el-space>

        <!-- 看板列表 -->
        <el-table
          :data="batchKanbanList"
          stripe
          style="margin-top: 16px"
          max-height="500"
          size="small"
          @selection-change="handleSelectionChange"
          ref="batchTableRef"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="kanbanNo" label="看板号" width="180" />
          <el-table-column prop="partName" label="零件名称" width="150" />
          <el-table-column prop="partCode" label="零件号" width="120" />
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="warehouseName" label="仓库" width="100" />
          <el-table-column prop="orderNo" label="入库单号" width="150" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="info">待入库</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Check, Finished, Select, Refresh } from '@element-plus/icons-vue'
import { validateKanban, scanInbound, getInboundOrderList, getInboundOrderDetail } from '../../api/wms'
import Scanner from '../../components/Scanner.vue'

const scanInput = ref(null)
const currentMode = ref('single') // single | batch
const scanCode = ref('')
const kanbanInfo = ref(null)
const submitting = ref(false)
const inboundQuantity = ref(null)

// 入库历史记录
const inboundHistory = ref([])

// 批量入库相关
const batchKanbanList = ref([])
const batchSubmitting = ref(false)
const selectedKanbanNos = ref([])
const batchTableRef = ref(null)
const loadingKanbans = ref(false)

// 计算剩余数量
const remainingQuantity = computed(() => {
  if (!kanbanInfo.value) return 0
  const total = kanbanInfo.value.quantity || 0
  const received = kanbanInfo.value.receivedQuantity || 0
  return Math.max(0, total - received)
})

// 验证数量
const validateQuantity = () => {
  if (!kanbanInfo.value) return
  
  const maxQty = remainingQuantity.value
  if (inboundQuantity.value > maxQty) {
    inboundQuantity.value = maxQty
    ElMessage.warning(`入库数量不能超过剩余数量 ${maxQty}`)
  }
  if (inboundQuantity.value < 1) {
    inboundQuantity.value = 1
  }
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

  const res = await validateKanban(scanCode.value)
  if (res.code === 200) {
    const data = res.data
    // 添加已入库数量到看板信息中
    data.receivedQuantity = data.receivedQuantity || 0
    kanbanInfo.value = data
    
    // 重置入库数量为剩余数量
    inboundQuantity.value = remainingQuantity.value
    
    // 清空入库历史
    inboundHistory.value = []
    
    ElMessage.success(`看板验证成功，剩余 ${remainingQuantity.value} 个可入库`)
    scanCode.value = ''
    nextTick(() => {
      scanInput.value?.focus()
    })
  } else {
    ElMessage.error(res.message || '看板无效或已使用')
    kanbanInfo.value = null
    inboundQuantity.value = null
    inboundHistory.value = []
  }
}

const confirmInbound = async () => {
  if (!kanbanInfo.value) {
    ElMessage.warning('请先扫描看板')
    return
  }

  const quantity = inboundQuantity.value
  if (!quantity || quantity <= 0) {
    ElMessage.warning('请输入入库数量')
    return
  }

  if (quantity > remainingQuantity.value) {
    ElMessage.warning(`入库数量不能超过剩余数量 ${remainingQuantity.value}`)
    return
  }

  submitting.value = true
  try {
    const res = await scanInbound({
      kanbanNo: kanbanInfo.value.kanbanNo,
      locationCode: '',  // 后端会从看板自动获取库位
      quantity: quantity
    })
    
    if (res.code === 200) {
      // 记录入库历史
      inboundHistory.value.push({
        quantity: quantity,
        operator: res.data?.operator || 'system',
        time: new Date().toLocaleString(),
        remainingAfter: remainingQuantity.value - quantity
      })
      
      ElMessage.success(`入库成功：${kanbanInfo.value.partName} x ${quantity}`)
      
      // 更新看板信息中的已入库数量
      kanbanInfo.value.receivedQuantity = (kanbanInfo.value.receivedQuantity || 0) + quantity
      
      // 检查是否全部入库完成
      if (remainingQuantity.value === 0) {
        ElMessage.success(`🎉 看板 ${kanbanInfo.value.kanbanNo} 已全部入库完成！`)
        // 可选：自动重置看板信息
        // kanbanInfo.value = null
        // inboundQuantity.value = null
        // locationCode.value = ''
      } else {
        // 重置入库数量为剩余数量
        inboundQuantity.value = remainingQuantity.value
        ElMessage.info(`剩余 ${remainingQuantity.value} 个可继续入库`)
      }
    } else {
      ElMessage.error(res.message || '入库失败')
    }
  } catch (error) {
    console.error('入库错误:', error)
    ElMessage.error('入库失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
    nextTick(() => {
      scanInput.value?.focus()
    })
  }
}

// 加载待入库的看板列表
const loadPendingKanbans = async () => {
  loadingKanbans.value = true
  try {
    // 查询所有状态的入库单
    const res = await getInboundOrderList({ 
      page: 1,
      pageSize: 100
    })
    
    if (res.code === 200 && res.data) {
      const allOrders = res.data.list || res.data || []
      console.log('所有入库单:', allOrders)
      
      // 打印所有入库单的状态
      console.log('入库单状态统计:', allOrders.map(o => ({
        orderNo: o.orderNo,
        status: o.status,
        warehouseName: o.warehouseName
      })))
      
      // 过滤出待入库和部分入库的订单（支持小写和大写）
      const pendingOrders = allOrders.filter(o => {
        const status = (o.status || '').toLowerCase()
        return status === 'pending' || status === 'partial' || 
               status === 'created' || status === 'in_progress'
      })
      
      console.log('待入库/部分入库的订单:', pendingOrders)
      console.log('待入库订单数量:', pendingOrders.length)
      
      // 获取这些入库单的看板
      const allKanbans = []
      for (const order of pendingOrders) {
        try {
          const orderNo = order.order_no || order.orderNo
          console.log(`加载入库单 ${orderNo} 的看板...`)
          
          // 通过入库单详情获取看板
          const detailRes = await getInboundOrderDetail(orderNo)
          console.log(`入库单 ${orderNo} 详情:`, detailRes)
          
          if (detailRes.code === 200 && detailRes.data) {
            // 使用kanbans数组，但过滤掉转包看板
            const kanbans = detailRes.data.kanbans || []
            
            console.log(`入库单 ${orderNo} 的所有看板:`, kanbans)
            console.log(`入库单 ${orderNo} 的看板数量:`, kanbans.length)
            
            // 过滤：只保留原始看板（没有parentKanbanNo的）
            const originalKanbans = kanbans.filter(k => {
              const parentNo = k.parent_kanban_no || k.parentKanbanNo
              const isOriginal = !parentNo || parentNo === '' || parentNo === null
              
              if (!isOriginal) {
                console.log(`排除转包看板: ${k.kanban_no || k.kanbanNo}, parent: ${parentNo}`)
              }
              
              return isOriginal
            })
            
            console.log(`入库单 ${orderNo} 的原始看板:`, originalKanbans)
            
            // 再过滤出未入库的
            const pendingKanbans = originalKanbans.filter(k => {
              const status = (k.status || '').toLowerCase().trim()
              const kanbanNo = k.kanban_no || k.kanbanNo
              
              console.log(`看板 ${kanbanNo} 状态: "${k.status}" -> 小写: "${status}"`)
              
              // 排除已入库的状态
              const isCompleted = status === 'in_stock' || 
                                 status === 'completed' || 
                                 status === '入库完成' ||
                                 status === '已入库' ||
                                 status === 'scanned' ||
                                 status === '已扫码'
              
              if (isCompleted) {
                console.log(`排除已入库看板: ${kanbanNo}, 状态: ${status}`)
              }
              
              return !isCompleted
            })
            
            console.log(`入库单 ${orderNo} 待入库的原始看板:`, pendingKanbans)
            console.log(`入库单 ${orderNo} 待入库看板数量:`, pendingKanbans.length)
            
            // 转换为前端格式
            const formattedKanbans = pendingKanbans.map(k => ({
              kanbanNo: k.kanban_no || k.kanbanNo,
              partName: k.part_name || k.partName,
              partCode: k.part_code || k.partCode,
              quantity: k.quantity || 0,
              warehouseName: order.warehouse_name || order.warehouseName,
              warehouseCode: order.warehouse_code || order.warehouseCode,
              orderNo: orderNo,
              status: 'PENDING'
            }))
            
            allKanbans.push(...formattedKanbans)
          }
        } catch (error) {
          console.error(`加载入库单 ${order.order_no || order.orderNo} 详情失败:`, error)
        }
      }
      
      batchKanbanList.value = allKanbans
      console.log('待入库看板列表:', batchKanbanList.value)
      console.log('待入库看板数量:', batchKanbanList.value.length)
      
      if (allKanbans.length === 0) {
        ElMessage.info('当前没有待入库的看板，请先创建入库单')
      } else {
        ElMessage.success(`加载了 ${allKanbans.length} 个待入库看板`)
      }
    }
  } catch (error) {
    console.error('加载待入库看板失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loadingKanbans.value = false
  }
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedKanbanNos.value = selection.map(item => item.kanbanNo)
}

// 全选
const selectAll = () => {
  if (batchTableRef.value) {
    batchKanbanList.value.forEach(row => {
      batchTableRef.value.toggleRowSelection(row, true)
    })
  }
}

// 清空选择
const clearSelection = () => {
  if (batchTableRef.value) {
    batchTableRef.value.clearSelection()
  }
}

// 批量入库 - 已选择的看板
const batchInboundSelected = async () => {
  if (selectedKanbanNos.value.length === 0) {
    ElMessage.warning('请先选择要入库的看板')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认一键入库已选择的 ${selectedKanbanNos.value.length} 个看板？`,
      '批量入库确认',
      { type: 'warning' }
    )

    batchSubmitting.value = true
    let successCount = 0
    let failCount = 0

    // 获取已选择的看板数据
    const selectedKanbans = batchKanbanList.value.filter(k => 
      selectedKanbanNos.value.includes(k.kanbanNo)
    )

    for (const kanban of selectedKanbans) {
      try {
        await scanInbound({
          kanbanNo: kanban.kanbanNo,
          locationCode: '',  // 后端会从看板自动获取库位
          quantity: kanban.quantity
        })
        successCount++
      } catch (e) {
        failCount++
        console.error(`看板 ${kanban.kanbanNo} 入库失败:`, e)
      }
    }

    ElMessage.success(`批量入库完成！成功 ${successCount} 个，失败 ${failCount} 个`)
    
    // 重新加载待入库看板列表
    await loadPendingKanbans()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量入库失败:', error)
      ElMessage.error('批量入库失败')
    }
  } finally {
    batchSubmitting.value = false
  }
}

onMounted(() => {
  // 加载待入库看板列表
  loadPendingKanbans()
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
  justify-content: space-between;
}

.scanner-card,
.location-card,
.input-card {
  margin-bottom: 20px;
}

.history-card {
  margin-top: 20px;
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
  min-width: 100px;
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

.info-item .value.highlight-warning {
  color: #e6a23c;
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

/* 数量输入样式 */
.quantity-input-wrapper {
  margin-top: 8px;
}

.quantity-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.quantity-hint .hint-warning {
  color: #f56c6c;
}

.quantity-hint .hint-info {
  color: #409eff;
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
  .location-card,
  .input-card {
    margin-bottom: 16px;
  }

  .card-header {
    font-size: 15px;
  }

  :deep(.el-form-item__label) {
    font-size: 14px;
  }

  :deep(.el-input__inner) {
    font-size: 16px;
  }

  :deep(.el-button) {
    min-height: 44px;
    font-size: 16px;
  }

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

  .kanban-alert :deep(.el-alert__content) {
    width: 100%;
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