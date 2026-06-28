<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>扫码出库</h2>
    </div>

    <!-- 模式切换 -->
    <el-tabs v-model="currentMode" type="card" class="mode-tabs" style="margin-bottom: 20px">
      <el-tab-pane label="📷 单件扫码出库" name="single" />
      <el-tab-pane label="🚀 批量快速出库" name="batch" />
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
                  :label="`${order.orderNo} - ${order.customerName || order.outboundType}`"
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
                  <span class="label">看板数量：</span>
                  <span class="value highlight">{{ kanbanInfo.quantity }}</span>
                </div>
                <div class="info-item" v-if="kanbanInfo.remainingOrderQuantity !== undefined">
                  <span class="label">订单剩余：</span>
                  <span class="value highlight-warning">{{ kanbanInfo.remainingOrderQuantity }}</span>
                </div>
                <div class="info-item">
                  <span class="label">客户：</span>
                  <span class="value">{{ kanbanInfo.customerName || kanbanInfo.supplierName || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="label">看板号：</span>
                  <span class="value code">{{ kanbanInfo.kanbanNo }}</span>
                </div>
                <div class="info-item">
                  <span class="label">仓库：</span>
                  <span class="value">{{ kanbanInfo.warehouseName || kanbanInfo.warehouseCode || '-' }}</span>
                </div>
                <div class="info-item" v-if="kanbanInfo.warehouseName">
                  <span class="label">仓库：</span>
                  <span class="value">{{ kanbanInfo.warehouseName }}</span>
                </div>
              </div>

              <!-- 出库数量输入 -->
              <el-divider style="margin: 12px 0">出库数量</el-divider>
              <div class="quantity-input-wrapper">
                <el-input-number
                  v-model="outboundQuantity"
                  :min="1"
                  :max="maxOutboundQuantity"
                  size="large"
                  controls-position="right"
                  style="width: 100%"
                  @change="validateQuantity"
                />
                <div class="quantity-hint">
                  可出库范围：1 ~ {{ maxOutboundQuantity }}
                  <span v-if="maxOutboundQuantity === 0" class="hint-warning">
                    ⚠️ 该看板已全部出库完成
                  </span>
                  <span v-else class="hint-info">
                    💡 可以多次分批出库，每次输入不同数量
                  </span>
                </div>
              </div>
            </template>
          </el-alert>
        </el-card>

        <!-- 确认出库按钮 -->
        <el-card shadow="never" class="action-card" v-if="kanbanInfo">
          <template #header>
            <div class="card-header">
              <span>✅ 确认出库</span>
            </div>
          </template>
          <el-form>
            <el-alert
              v-if="!orderInfo"
              title="请先选择并加载出库单"
              type="warning"
              show-icon
              :closable="false"
              style="margin-bottom: 12px"
            />
            <el-form-item>
              <el-button
                type="success"
                size="large"
                @click="confirmOutbound"
                :loading="submitting"
                :disabled="!outboundQuantity || outboundQuantity <= 0"
                style="width: 100%"
              >
                <el-icon><Check /></el-icon>
                确认出库 ({{ outboundQuantity || 0 }})
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
    </template>

    <!-- 批量快速出库模式 -->
    <template v-if="currentMode === 'batch'">
      <el-card shadow="never" class="batch-card">
        <template #header>
          <div class="card-header">
            <span>🚀 批量快速出库</span>
          </div>
        </template>

        <!-- 显示未出库看板统计 -->
        <el-alert
          v-if="batchOrderList.length > 0"
          :title="`共有 ${batchOrderList.length} 个出库单待出库，已选择 ${selectedOrderNos.length} 个`"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        />

        <el-alert
          v-else
          title="当前没有待出库的出库单"
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
            :disabled="selectedOrderNos.length === 0 || batchSubmitting"
            @click="batchOutboundSelected"
            :loading="batchSubmitting"
          >
            <el-icon><Finished /></el-icon>
            一键出库已选择 ({{ selectedOrderNos.length }})
          </el-button>
          <el-button
            type="primary"
            size="large"
            :disabled="batchOrderList.length === 0 || batchSubmitting"
            @click="selectAll"
          >
            <el-icon><Select /></el-icon>
            全选
          </el-button>
          <el-button
            size="large"
            :disabled="batchOrderList.length === 0"
            @click="clearSelection"
          >
            清空选择
          </el-button>
          <el-button
            size="large"
            @click="loadPendingOrders"
            :loading="loadingKanbans"
          >
            <el-icon><Refresh /></el-icon>
            刷新列表
          </el-button>
        </el-space>

        <!-- 出库单列表 -->
        <el-table
          :data="batchOrderList"
          stripe
          style="margin-top: 16px"
          max-height="500"
          size="small"
          @selection-change="handleSelectionChange"
          ref="batchTableRef"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="orderNo" label="出库单号" width="180" />
          <el-table-column prop="outboundType" label="出库类型" width="120" />
          <el-table-column prop="customerName" label="客户名称" width="150" />
          <el-table-column prop="warehouseName" label="仓库" width="100" />
          <el-table-column prop="totalQuantity" label="总数量" width="100" align="center" />
          <el-table-column prop="outboundQuantity" label="已出库" width="100" align="center" />
          <el-table-column label="进度" width="120">
            <template #default="{ row }">
              <el-progress 
                :percentage="Math.round((row.outboundQuantity || 0) / (row.totalQuantity || 1) * 100)" 
                :stroke-width="12"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'PENDING'" type="info">待出库</el-tag>
              <el-tag v-else type="warning">部分出库</el-tag>
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
import { validateOutboundKanban, validateOutboundOrder, scanOutbound, getOutboundOrderList, getOutboundOrderDetail, getFifoInventoryList } from '../../api/wms'
import Scanner from '../../components/Scanner.vue'

const currentMode = ref('single') // single | batch
const scanInput = ref(null)
const scanCode = ref('')
const kanbanInfo = ref(null)
const orderNo = ref('')
const orderInfo = ref(null)
const pendingOrders = ref([])
const submitting = ref(false)
const outboundQuantity = ref(1)

// 批量出库相关
const batchOrderList = ref([])
const batchSubmitting = ref(false)
const selectedOrderNos = ref([])
const batchTableRef = ref(null)
const loadingKanbans = ref(false)

// ============================================
// Computed: Max outbound quantity
// ============================================
const maxOutboundQuantity = computed(() => {
  if (!kanbanInfo.value) return 0
  const kanbanQty = Number(kanbanInfo.value.quantity) || 0
  const orderRemaining = Number(kanbanInfo.value.remainingOrderQuantity) || kanbanQty
  return Math.min(kanbanQty, orderRemaining)
})

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

const validateQuantity = () => {
  if (!kanbanInfo.value) return
  const maxQty = maxOutboundQuantity.value
  if (outboundQuantity.value > maxQty) {
    outboundQuantity.value = maxQty
    ElMessage.warning(`出库数量不能超过 ${maxQty}`)
  }
  if (outboundQuantity.value < 1) {
    outboundQuantity.value = 1
  }
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
      outboundQuantity.value = 1
      return
    }
    kanbanInfo.value = scannedKanban
    // Set default quantity to max available
    outboundQuantity.value = maxOutboundQuantity.value
    ElMessage.success('看板验证成功')
    scanCode.value = ''
    nextTick(() => {
      scanInput.value?.focus()
    })
  } else {
    ElMessage.error(res.message || '看板无效或已使用')
    kanbanInfo.value = null
    outboundQuantity.value = 1
  }
}

const loadPendingOrders = async () => {
  try {
    const responses = await Promise.all([
      getOutboundOrderList({ status: 'pending', page: 1, pageSize: 100 }),
      getOutboundOrderList({ status: 'partial', page: 1, pageSize: 100 })
    ])
    const orders = responses
      .filter(res => res.code === 200)
      .flatMap(res => res.data?.list || [])
    
    // 单件模式使用
    pendingOrders.value = orders
    
    // 批量模式使用
    batchOrderList.value = orders
    
    console.log('加载出库单:', orders.length, '个')
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
          validateQuantity()
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

  console.log('出库信息:', {
    orderNo: orderInfo.value.orderNo,
    kanbanNo: kanbanInfo.value.kanbanNo,
    partCode: kanbanInfo.value.partCode,
    quantity: outboundQuantity.value
  })
  
  if (!ensureKanbanMatchesOrder(kanbanInfo.value)) {
    return
  }

  submitting.value = true
  try {
    const res = await scanOutbound({
      orderNo: orderInfo.value.orderNo,
      kanbanNo: kanbanInfo.value.kanbanNo,
      partCode: kanbanInfo.value.partCode,
      quantity: outboundQuantity.value
    })
    
    console.log('出库响应:', res)
    
    if (res.code === 200) {
      ElMessage.success(`出库成功：${kanbanInfo.value.partName} x ${outboundQuantity.value}`)
      
      // Check if the order is now complete
      const remainingAfter = res.data?.remainingOrderQuantity
      if (remainingAfter !== undefined && remainingAfter <= 0) {
        ElMessage.success('🎉 该出库单已全部完成！')
        // Reset everything
        kanbanInfo.value = null
        orderInfo.value = null
        orderNo.value = ''
        outboundQuantity.value = 1
        scanCode.value = ''
        await loadPendingOrders()
      } else {
        // Update remaining quantity in kanbanInfo
        if (remainingAfter !== undefined) {
          kanbanInfo.value.remainingOrderQuantity = remainingAfter
          // If the kanban quantity is now 0, reset
          if (remainingAfter <= 0) {
            kanbanInfo.value = null
            outboundQuantity.value = 1
          } else {
            // Update max quantity
            outboundQuantity.value = Math.min(maxOutboundQuantity.value, remainingAfter)
          }
        }
        // Reload pending orders
        await loadPendingOrders()
        await loadOrderInfo({ silent: true })
      }
      
      scanCode.value = ''
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

// ============================================
// 批量出库功能
// ============================================

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedOrderNos.value = selection.map(item => item.orderNo)
}

// 全选
const selectAll = () => {
  if (batchTableRef.value) {
    batchOrderList.value.forEach(row => {
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

// 批量出库 - 已选择的出库单
const batchOutboundSelected = async () => {
  if (selectedOrderNos.value.length === 0) {
    ElMessage.warning('请先选择要出库的出库单')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认一键出库已选择的 ${selectedOrderNos.value.length} 个出库单？`,
      '批量出库确认',
      { type: 'warning' }
    )

    batchSubmitting.value = true
    let successCount = 0
    let failCount = 0

    // 处理每个出库单
    for (const orderNo of selectedOrderNos.value) {
      try {
        console.log(`========== 处理出库单: ${orderNo} ==========`)
        
        // 获取出库单详情，找到对应的入库看板
        const detailRes = await getOutboundOrderDetail(orderNo)
        console.log(`出库单详情响应:`, detailRes)
        
        if (detailRes.code === 200 && detailRes.data && detailRes.data.details) {
          const details = detailRes.data.details
          const warehouseCode = detailRes.data.order?.warehouse_code  // ✅ 获取出库单的仓库代码
          console.log(`出库单明细:`, details)
          console.log(`出库单仓库: ${warehouseCode}`)
          
          // 对每个明细项进行出库
          let allDetailsSuccess = true  // 标记所有明细是否都成功
          
          for (const detail of details) {
            const partCode = detail.part_code || detail.partCode
            const expectedQuantity = detail.expected_quantity || detail.quantity || 0
            const shippedQuantity = detail.shipped_quantity || 0  // ✅ 获取已出库数量
            const remainingQtyInOrder = expectedQuantity - shippedQuantity  // ✅ 计算剩余数量
            
            console.log(`明细项 - 零件号: ${partCode}, 预期: ${expectedQuantity}, 已出库: ${shippedQuantity}, 剩余: ${remainingQtyInOrder}`)
            
            // ✅ 如果这个零件已经完成出库，跳过
            if (remainingQtyInOrder <= 0) {
              console.log(`✅ 零件 ${partCode} 已经完成出库，跳过`)
              continue
            }
            
            if (partCode) {
              // 先查询FIFO推荐的入库看板（传入仓库代码）
              console.log(`查询零件 ${partCode} 在仓库 ${warehouseCode} 的FIFO库存...`)
              const fifoRes = await getFifoInventoryList(partCode, warehouseCode)
              
              if (fifoRes.code === 200 && fifoRes.data && fifoRes.data.length > 0) {
                const fifoKanbans = fifoRes.data
                console.log(`FIFO推荐看板:`, fifoKanbans)
                
                // 按FIFO顺序出库（使用剩余数量，不是预期数量）
                let remainingQty = remainingQtyInOrder  // ✅ 使用剩余数量
                let totalOutboundQty = 0
                
                for (const fifoItem of fifoKanbans) {
                  if (remainingQty <= 0) {
                    console.log(`✅ 零件 ${partCode} 出库完成`)
                    break
                  }
                  
                  const kanbanNo = fifoItem.kanban_no || fifoItem.kanbanNo
                  const availableQty = fifoItem.quantity || fifoItem.available_quantity || 0
                  const outboundQty = Math.min(remainingQty, availableQty)
                  
                  console.log(`准备出库 - 看板: ${kanbanNo}, 可用: ${availableQty}, 出库: ${outboundQty}`)
                  
                  if (kanbanNo && outboundQty > 0) {
                    try {
                      const outboundRes = await scanOutbound({
                        kanbanNo: kanbanNo,
                        orderNo: orderNo,
                        partCode: partCode,
                        quantity: outboundQty
                      })
                      
                      console.log(`出库响应:`, outboundRes)
                      
                      if (outboundRes.code === 200) {
                        console.log(`✅ 看板 ${kanbanNo} 出库成功，数量: ${outboundQty}`)
                        remainingQty -= outboundQty
                        totalOutboundQty += outboundQty
                      } else {
                        console.warn(`⚠️ 看板 ${kanbanNo} 出库失败: ${outboundRes.message}`)
                        // 如果是“已完成出库”错误，说明其他并发请求已经出库完成
                        if (outboundRes.message && outboundRes.message.includes('已完成出库')) {
                          console.log(`✅ 零件 ${partCode} 已被其他请求出库完成，跳过此零件`)
                          remainingQty = 0  // 标记为完成
                          totalOutboundQty = expectedQuantity - shippedQuantity  // 计算出库数量
                          break  // 跳出看板循环
                        }
                        // 其他错误，继续下一个看板
                      }
                    } catch (err) {
                      console.warn(`⚠️ 看板 ${kanbanNo} 出库API调用失败:`, err.message || err)
                      // 不抛出错误，继续下一个看板
                    }
                  }
                }
                
                console.log(`零件 ${partCode} 出库结果: 剩余需要 ${remainingQtyInOrder}, 实际出库 ${totalOutboundQty}, 还差 ${remainingQty}`)
                
                if (remainingQty > 0) {
                  console.warn(`⚠️ 零件 ${partCode} 出库不完整，还差 ${remainingQty} 个`)
                  allDetailsSuccess = false  // 标记有零件未完全出库
                }
              } else {
                console.error(`❌ 零件 ${partCode} 没有可用的库存看板`)
                allDetailsSuccess = false  // 标记有零件没有库存
              }
            } else {
              console.warn(`跳过明细项：缺少partCode`, detail)
            }
          }
          
          // 只有所有明细项都成功出库，才标记为成功
          if (allDetailsSuccess) {
            successCount++
            console.log(`✅ 出库单 ${orderNo} 所有零件出库完成`)
          } else {
            failCount++
            console.warn(`⚠️ 出库单 ${orderNo} 部分零件出库失败`)
          }
        } else {
          console.error(`出库单 ${orderNo} 详情数据不完整:`, detailRes)
          throw new Error('出库单详情数据不完整')
        }
      } catch (e) {
        failCount++
        console.error(`❌ 出库单 ${orderNo} 出库失败:`, e)
      }
    }

    ElMessage.success(`批量出库完成！成功 ${successCount} 个出库单，失败 ${failCount} 个`)
    
    // 重新加载待出库出库单列表
    console.log('重新加载出库单列表...')
    await loadPendingOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量出库失败:', error)
      ElMessage.error('批量出库失败')
    }
  } finally {
    batchSubmitting.value = false
  }
}

onMounted(() => {
  // 加载待出库出库单列表
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
  justify-content: space-between;
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
  .order-card,
  .input-card,
  .action-card {
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
