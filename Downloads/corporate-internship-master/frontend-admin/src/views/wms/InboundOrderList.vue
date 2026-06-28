<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>入库单管理</h2>
      <el-button type="primary" @click="$router.push('/wms/inbound-order/create')">
        <el-icon><Plus /></el-icon>
        创建入库单
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="待入库" value="pending" />
            <el-option label="部分入库" value="partial" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="filterSupplierCode" placeholder="请选择供应商" clearable @change="loadData">
            <el-option v-for="s in suppliers" :key="s.supplierCode" :label="s.supplierName" :value="s.supplierCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- PC端表格 -->
      <el-table :data="tableData" v-loading="loading" stripe class="desktop-table">
        <el-table-column prop="orderNo" label="入库单号" width="180" />
        <el-table-column prop="inboundType" label="入库类型" width="120" />
        <el-table-column prop="supplierName" label="供应商" width="150" />
        <el-table-column prop="totalQuantity" label="总数量" width="100" />
        <el-table-column prop="receivedQuantity" label="已入库" width="100" />
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.receivedQuantity / row.totalQuantity * 100)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <el-button link type="primary" @click="editOrder(row)" v-if="row.status !== 'completed'">编辑</el-button>
            <el-button link type="primary" @click="openPrintKanban(row)" v-if="row.status !== 'completed'">打印看板</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-if="row.status !== 'completed'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-cards" v-loading="loading">
        <el-card v-for="row in tableData" :key="row.orderNo" shadow="hover" class="order-card">
          <div class="card-header">
            <div class="order-no">{{ row.orderNo }}</div>
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">入库类型：</span>
              <span>{{ row.inboundType }}</span>
            </div>
            <div class="info-row">
              <span class="label">供应商：</span>
              <span>{{ row.supplierName }}</span>
            </div>
            <div class="info-row">
              <span class="label">数量：</span>
              <span>{{ row.receivedQuantity }} / {{ row.totalQuantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">进度：</span>
              <el-progress :percentage="Math.round(row.receivedQuantity / row.totalQuantity * 100)" :stroke-width="6" style="flex: 1" />
            </div>
            <div class="info-row">
              <span class="label">创建时间：</span>
              <span>{{ row.createdAt }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button size="small" type="primary" @click="editOrder(row)" v-if="row.status !== 'completed'">编辑</el-button>
            <el-button size="small" type="success" @click="openPrintKanban(row)" v-if="row.status !== 'completed'">打印</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="row.status !== 'completed'">删除</el-button>
          </div>
        </el-card>
        <el-empty v-if="tableData.length === 0 && !loading" description="暂无数据" />
      </div>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog title="入库单详情" v-model="detailVisible" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="入库类型">{{ detailData.inboundType }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detailData.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detailData.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="已入库">{{ detailData.receivedQuantity }}</el-descriptions-item>
        <el-descriptions-item label="总箱数">{{ detailData.totalBoxes }}</el-descriptions-item>
        <el-descriptions-item label="已入库箱数">{{ detailData.receivedBoxes }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getStatusType(detailData.status)">{{ detailData.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>入库明细</el-divider>
      <el-table :data="detailDetails" stripe size="small">
        <el-table-column prop="partCode" label="零件号" width="180" />
        <el-table-column prop="partName" label="零件名称" />
        <el-table-column prop="expectedQuantity" label="预期数量" width="100" />
        <el-table-column prop="receivedQuantity" label="已入库" width="100" />
        <el-table-column prop="expectedBoxes" label="预期箱数" width="100" />
        <el-table-column prop="receivedBoxes" label="已入库箱数" width="100" />
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.expectedQuantity > 0 ? Math.floor(row.receivedQuantity / row.expectedQuantity * 100) : 0" 
              :stroke-width="6" 
            />
          </template>
        </el-table-column>
      </el-table>

      <el-divider>看板信息</el-divider>
      <el-table :data="detailKanbans" stripe size="small" v-if="detailKanbans.length">
        <el-table-column prop="kanbanNo" label="看板号" width="200" />
        <el-table-column prop="partName" label="零件名称" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="二维码" width="120">
          <template #default="{ row }">
            <img v-if="row.qrCodeImage" :src="row.qrCodeImage" style="width: 80px; height: 80px;" />
            <span v-else>生成中...</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getKanbanStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无看板" />
    </el-dialog>

    <!-- 打印看板弹窗 -->
    <el-dialog title="打印看板" v-model="kanbanDialogVisible" width="1000px">
      <el-table :data="expandedKanbanItems" stripe>
        <el-table-column prop="partCode" label="零件号" width="120" />
        <el-table-column prop="partName" label="零件名称" width="140" />
        <el-table-column prop="boxLabel" label="箱号" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.boxLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="本箱数量" width="100" />
        <el-table-column prop="kanbanNo" label="看板号" width="250">
          <template #default="{ row }">
            <span style="font-family: monospace; font-size: 12px;">{{ row.kanbanNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="二维码" width="140">
          <template #default="{ row }">
            <img v-if="row.qrCodeImage" :src="row.qrCodeImage" style="width: 70px; height: 70px;" />
            <span v-else style="color: #909399; font-size: 12px;">生成中...</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="printSingleBox(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div style="margin-top: 20px; text-align: center; display: flex; gap: 12px; justify-content: center; flex-wrap: wrap;">
        <el-button type="primary" @click="generateAndPrintAll">
          <el-icon><Printer /></el-icon>
          批量生成并打印 ({{ totalBoxCount }} 箱)
        </el-button>
        <el-button @click="kanbanDialogVisible = false">关闭</el-button>
      </div>
      <div style="margin-top: 8px; text-align: center; color: #909399; font-size: 12px;">
        共 {{ totalBoxCount }} 个看板（每个物理箱一个独立看板）
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Printer } from '@element-plus/icons-vue'
import { getInboundOrderList, getInboundOrderDetail, deleteInboundOrder, printKanban } from '../../api/wms'
import { getSupplierList } from '../../api/wms'
import QRCode from 'qrcode'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const suppliers = ref([])
const filterStatus = ref('')
const filterSupplierCode = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const detailData = ref({})
const detailDetails = ref([])
const detailKanbans = ref([])

// 看板相关
const kanbanDialogVisible = ref(false)
const kanbanItems = ref([])
const currentOrder = ref(null)

// ============================================
// COMPUTED: Expand kanban items to show each box as a row
// ============================================
const expandedKanbanItems = computed(() => {
  const result = []
  kanbanItems.value.forEach((item) => {
    const boxCount = item.expectedBoxes || 1
    const totalQuantity = item.expectedQuantity || 0
    const packagingCapacity = item.packagingCapacity || totalQuantity // 如果没有包装容量，使用总数量
    
    if (item.boxes && item.boxes.length > 0) {
      item.boxes.forEach((box) => {
        result.push({
          partCode: item.partCode,
          partName: item.partName,
          boxLabel: `${box.boxNumber}/${box.totalBoxes}`,
          quantity: box.quantity,
          kanbanNo: box.kanbanNo,
          qrCodeImage: box.qrCodeImage,
          _item: item,
          _box: box
        })
      })
    } else {
      // 按照包装容量分配数量：前几箱满箱，最后一箱剩余
      let remainingQty = totalQuantity
      for (let i = 1; i <= boxCount; i++) {
        // 如果不是最后一箱，使用包装容量；最后一箱使用剩余数量
        const boxQuantity = (i < boxCount) ? Math.min(packagingCapacity, remainingQty) : remainingQty
        remainingQty -= boxQuantity
        
        result.push({
          partCode: item.partCode,
          partName: item.partName,
          boxLabel: `${i}/${boxCount}`,
          quantity: boxQuantity,
          kanbanNo: '自动生成',
          qrCodeImage: null,
          _item: item,
          _box: null
        })
      }
    }
  })
  return result
})

const totalBoxCount = computed(() => {
  let count = 0
  kanbanItems.value.forEach((item) => {
    count += (item.expectedBoxes || 1)
  })
  return count
})

const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'partial': return 'primary'
    case 'completed': return 'success'
    default: return 'info'
  }
}

const getKanbanStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'scanned': return 'info'
    case 'stored': return 'success'
    case 'outbound': return 'primary'
    default: return 'info'
  }
}

const loadSuppliers = async () => {
  const res = await getSupplierList()
  if (res.code === 200) {
    suppliers.value = res.data || []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: filterStatus.value,
      supplierCode: filterSupplierCode.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
    const res = await getInboundOrderList(params)
    if (res.code === 200) {
      tableData.value = res.data?.list || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  filterStatus.value = ''
  filterSupplierCode.value = ''
  dateRange.value = []
  page.value = 1
  loadData()
}

const viewDetail = async (row) => {
  const res = await getInboundOrderDetail(row.orderNo)
  if (res.code === 200) {
    const order = res.data?.order || {}
    detailData.value = {
      orderNo: order.order_no,
      inboundType: order.inbound_type,
      supplierName: order.supplier_name,
      warehouseName: order.warehouse_name,
      totalQuantity: order.total_quantity,
      receivedQuantity: order.received_quantity,
      totalBoxes: order.total_boxes,
      receivedBoxes: order.received_boxes,
      status: order.status,
      statusText: order.status_text || getStatusText(order.status),
      remark: order.remark,
      createdBy: order.created_by,
      createdAt: order.created_at
    }
    detailDetails.value = (res.data?.details || []).map(detail => ({
      partCode: detail.part_code,
      partName: detail.part_name,
      expectedQuantity: detail.expected_quantity || 0,
      receivedQuantity: detail.received_quantity || 0,
      expectedBoxes: detail.expected_boxes || 0,
      receivedBoxes: detail.received_boxes || 0,
      packagingCapacity: detail.packaging_capacity || 0,
      unit: detail.unit || '个'
    }))
    const kanbans = res.data?.kanbans || []
    for (const kanban of kanbans) {
      if (kanban.kanban_no || kanban.kanbanNo) {
        const qrText = kanban.kanban_no || kanban.kanbanNo
        kanban.qrCodeImage = await QRCode.toDataURL(qrText, { width: 100, margin: 1 })
        kanban.kanbanNo = kanban.kanban_no
        kanban.partName = kanban.part_name
        kanban.statusText = getStatusText(kanban.status)
      }
    }
    detailKanbans.value = kanbans
    detailVisible.value = true
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'pending': return '待入库'
    case 'scanned': return '已扫码'
    case 'stored': return '已上架'
    case 'outbound': return '已出库'
    default: return status || '未知'
  }
}

const editOrder = (row) => {
  router.push(`/wms/inbound-order/edit/${row.orderNo}`)
}

const openPrintKanban = async (row) => {
  currentOrder.value = row
  const res = await getInboundOrderDetail(row.orderNo)
  if (res.code === 200) {
    const details = res.data?.details || []
    kanbanItems.value = details.map(d => ({
      partCode: d.part_code || d.partCode,
      partName: d.part_name || d.partName,
      expectedQuantity: d.expected_quantity || d.expectedQuantity || 0,
      expectedBoxes: d.expected_boxes || d.expectedBoxes || 1,
      packagingCapacity: d.packaging_capacity || d.packagingCapacity || 0, // 包装容量
      kanbanNo: '',
      qrCodeImage: null,
      boxes: []
    }))
    kanbanDialogVisible.value = true
  } else {
    ElMessage.error('获取入库单详情失败')
  }
}

const generateQRCode = async (text) => {
  try {
    return await QRCode.toDataURL(text, {
      width: 120,
      margin: 1,
      errorCorrectionLevel: 'M'
    })
  } catch (err) {
    console.error('生成二维码失败:', err)
    return null
  }
}

// ============================================
// Generate and print all kanban cards - SAVE TO DATABASE FIRST
// ============================================
const generateAndPrintAll = async () => {
  if (!currentOrder.value) {
    ElMessage.warning('请先选择订单')
    return
  }

  const loadingMsg = ElMessage.info({ message: '正在生成看板和二维码...', duration: 0 })

  try {
    // ============================================
    // FIX: Clear existing boxes BEFORE generating new ones
    // ============================================
    kanbanItems.value.forEach((item) => {
      item.boxes = []
      item.kanbanNo = ''
      item.qrCodeImage = null
    })

    // STEP 1: Build all box items with unique kanban numbers
    const allBoxItems = []
    kanbanItems.value.forEach((item) => {
      const boxCount = item.expectedBoxes || 1
      const totalQuantity = item.expectedQuantity || 0
      const packagingCapacity = item.packagingCapacity || totalQuantity // 包装容量
      
      // 按照包装容量分配数量
      let remainingQty = totalQuantity
      for (let i = 1; i <= boxCount; i++) {
        // 如果不是最后一箱，使用包装容量；最后一箱使用剩余数量
        const boxQuantity = (i < boxCount) ? Math.min(packagingCapacity, remainingQty) : remainingQty
        remainingQty -= boxQuantity
        
        const uniqueKanbanNo = `KAN-${Date.now()}-${item.partCode}-${i}`
        
        allBoxItems.push({
          partCode: item.partCode,
          partName: item.partName,
          expectedQuantity: boxQuantity,
          quantity: boxQuantity,
          boxNumber: i,
          totalBoxes: boxCount,
          kanbanNo: uniqueKanbanNo,
          supplierName: currentOrder.value?.supplierName || ''
        })
      }
    })

    console.log('📦 Generated box items:', allBoxItems)

    if (allBoxItems.length === 0) {
      ElMessage.warning('没有需要生成的看板')
      loadingMsg.close()
      return
    }

    // STEP 2: Save kanbans to database via API
    const saveItems = allBoxItems.map(item => ({
      partCode: item.partCode,
      partName: item.partName,
      supplierCode: currentOrder.value?.supplierCode || '',
      quantity: item.quantity,
      boxCount: 1,
      kanbanNo: item.kanbanNo
    }))

    console.log('💾 Saving to database:', saveItems)

    const saveResult = await printKanban({
      orderNo: currentOrder.value.orderNo,
      items: saveItems
    })

    console.log('📥 Save result:', saveResult)

    if (saveResult.code !== 200) {
      ElMessage.error('保存看板到数据库失败: ' + saveResult.message)
      loadingMsg.close()
      return
    }

    // STEP 3: Get the saved kanbans from response
    const savedKanbans = saveResult.data || []
    console.log('✅ Saved kanbans:', savedKanbans)

    if (savedKanbans.length === 0) {
      ElMessage.error('没有看板被保存到数据库')
      loadingMsg.close()
      return
    }

    // STEP 4: Generate QR codes and store in kanbanItems
    let boxIndex = 0
    for (const item of kanbanItems.value) {
      const boxCount = item.expectedBoxes || 1
      const totalQuantity = item.expectedQuantity || 0
      const packagingCapacity = item.packagingCapacity || totalQuantity // 包装容量
      // Clear boxes array again to be safe
      item.boxes = []
      
      // 按照包装容量分配数量
      let remainingQty = totalQuantity
      for (let i = 0; i < boxCount; i++) {
        if (boxIndex < savedKanbans.length) {
          const saved = savedKanbans[boxIndex]
          const qrImage = await generateQRCode(saved.kanbanNo)
          
          // 如果不是最后一箱，使用包装容量；最后一箱使用剩余数量
          const boxQuantity = (i < boxCount - 1) ? Math.min(packagingCapacity, remainingQty) : remainingQty
          remainingQty -= boxQuantity
          
          item.boxes.push({
            boxNumber: i + 1,
            totalBoxes: boxCount,
            kanbanNo: saved.kanbanNo,
            qrCodeImage: qrImage,
            quantity: boxQuantity
          })
          boxIndex++
        }
      }
    }

    console.log('✅ Updated kanbanItems with QR codes:', kanbanItems.value)

    // STEP 5: Print all cards
    const printWindow = window.open('', '_blank')
    if (!printWindow) {
      ElMessage.error('请允许弹出窗口')
      loadingMsg.close()
      return
    }

    let allCardsHtml = ''
    
    for (const item of kanbanItems.value) {
      for (const box of item.boxes) {
        const sequenceLabel = `${box.boxNumber}/${box.totalBoxes}`
        
        allCardsHtml += `
          <div class="kanban-card">
            <div class="kanban-title">📦 入库看板 ${sequenceLabel}</div>
            <div class="kanban-code">${box.kanbanNo}</div>
            <div class="kanban-row"><span class="kanban-label">零件号：</span><span class="kanban-value">${item.partCode}</span></div>
            <div class="kanban-row"><span class="kanban-label">零件名称：</span><span class="kanban-value">${item.partName}</span></div>
            <div class="kanban-row"><span class="kanban-label">总数量：</span><span class="kanban-value">${item.expectedQuantity}</span></div>
            <div class="kanban-row"><span class="kanban-label">本箱数量：</span><span class="kanban-value">${box.quantity}</span></div>
            <div class="kanban-row"><span class="kanban-label">供应商：</span><span class="kanban-value">${currentOrder.value?.supplierName || ''}</span></div>
            <div class="kanban-row"><span class="kanban-label">入库单号：</span><span class="kanban-value">${currentOrder.value?.orderNo || ''}</span></div>
            <div class="kanban-row"><span class="kanban-label">箱号：</span><span class="kanban-value">${sequenceLabel}</span></div>
            <div class="qr-code">
              <img src="${box.qrCodeImage}" style="width: 120px; height: 120px;" alt="二维码">
              <div style="font-size: 10px; margin-top: 5px;">扫码入库 - 箱 ${sequenceLabel}</div>
            </div>
            <div class="footer">生成时间：${new Date().toLocaleString()}</div>
          </div>
        `
      }
    }

    printWindow.document.write(`
      <html>
        <head>
          <title>打印入库看板 (${allBoxItems.length} 箱)</title>
          <style>
            body { 
              font-family: Arial, sans-serif; 
              padding: 20px; 
              background: #f0f0f0;
            }
            .kanban-card {
              border: 2px solid #333;
              padding: 20px;
              margin: 0 auto 20px auto;
              width: 350px;
              page-break-after: always;
              background: #fff;
              border-radius: 8px;
              box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            }
            .kanban-title { 
              font-size: 18px; 
              font-weight: bold; 
              text-align: center; 
              margin-bottom: 10px;
              color: #409eff;
            }
            .kanban-code { 
              font-size: 12px; 
              color: #666; 
              text-align: center; 
              margin-bottom: 15px; 
              word-break: break-all;
              background: #f5f7fa;
              padding: 4px 8px;
              border-radius: 4px;
              font-family: monospace;
            }
            .kanban-row { 
              margin: 8px 0; 
              white-space: nowrap; 
              padding: 4px 0;
              border-bottom: 1px dashed #eee;
            }
            .kanban-row:last-child { border-bottom: none; }
            .kanban-label { 
              font-weight: bold; 
              display: inline-block; 
              width: 80px; 
              margin-right: 10px;
              color: #606266;
            }
            .kanban-value { display: inline-block; color: #303133; font-weight: 600; }
            .qr-code { 
              text-align: center; 
              margin: 15px 0;
              padding: 10px;
              background: #fafbfc;
              border-radius: 8px;
            }
            .qr-code img { border: 1px solid #ddd; border-radius: 4px; }
            .footer { 
              text-align: center; 
              font-size: 10px; 
              color: #999; 
              margin-top: 10px;
              padding-top: 10px;
              border-top: 1px solid #eee;
            }
            @media print {
              .kanban-card {
                page-break-after: always;
                break-inside: avoid;
                box-shadow: none;
                border: 2px solid #000;
              }
              body { background: #fff; padding: 10px; }
            }
          </style>
        </head>
        <body>
          ${allCardsHtml}
          <script>
            window.onload = function() {
              window.print();
            }
          <\/script>
        </body>
      </html>
    `)
    printWindow.document.close()
    
    loadingMsg.close()
    ElMessage.success(`成功生成并保存 ${allBoxItems.length} 个看板到数据库`)
    
  } catch (error) {
    loadingMsg.close()
    console.error('生成看板失败:', error)
    ElMessage.error('生成看板失败: ' + (error.message || '未知错误'))
  }
}

// ============================================
// Print single box
// ============================================
const printSingleBox = async (row) => {
  if (!row._box || !row._box.kanbanNo) {
    ElMessage.warning('请先生成看板')
    return
  }
  
  const box = row._box
  const item = row._item
  const sequenceLabel = `${box.boxNumber}/${box.totalBoxes}`
  
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.error('请允许弹出窗口')
    return
  }
  
  printWindow.document.write(`
    <html>
      <head>
        <title>打印看板 - ${item.partCode} 箱 ${sequenceLabel}</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; background: #f0f0f0; }
          .kanban-card {
            border: 2px solid #333;
            padding: 20px;
            margin: 0 auto;
            width: 350px;
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          }
          .kanban-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; color: #409eff; }
          .kanban-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; background: #f5f7fa; padding: 4px 8px; border-radius: 4px; font-family: monospace; }
          .kanban-row { margin: 8px 0; white-space: nowrap; padding: 4px 0; border-bottom: 1px dashed #eee; }
          .kanban-row:last-child { border-bottom: none; }
          .kanban-label { font-weight: bold; display: inline-block; width: 80px; margin-right: 10px; color: #606266; }
          .kanban-value { display: inline-block; color: #303133; font-weight: 600; }
          .qr-code { text-align: center; margin: 15px 0; padding: 10px; background: #fafbfc; border-radius: 8px; }
          .qr-code img { border: 1px solid #ddd; border-radius: 4px; }
          .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee; }
          @media print {
            .kanban-card { box-shadow: none; border: 2px solid #000; }
            body { background: #fff; padding: 10px; }
          }
        </style>
      </head>
      <body>
        <div class="kanban-card">
          <div class="kanban-title">📦 入库看板 ${sequenceLabel}</div>
          <div class="kanban-code">${box.kanbanNo}</div>
          <div class="kanban-row"><span class="kanban-label">零件号：</span><span class="kanban-value">${item.partCode}</span></div>
          <div class="kanban-row"><span class="kanban-label">零件名称：</span><span class="kanban-value">${item.partName}</span></div>
          <div class="kanban-row"><span class="kanban-label">总数量：</span><span class="kanban-value">${item.expectedQuantity}</span></div>
          <div class="kanban-row"><span class="kanban-label">本箱数量：</span><span class="kanban-value">${box.quantity}</span></div>
          <div class="kanban-row"><span class="kanban-label">供应商：</span><span class="kanban-value">${currentOrder.value?.supplierName || ''}</span></div>
          <div class="kanban-row"><span class="kanban-label">入库单号：</span><span class="kanban-value">${currentOrder.value?.orderNo || ''}</span></div>
          <div class="kanban-row"><span class="kanban-label">箱号：</span><span class="kanban-value">${sequenceLabel}</span></div>
          <div class="qr-code">
            <img src="${box.qrCodeImage}" style="width: 120px; height: 120px;" alt="二维码">
            <div style="font-size: 10px; margin-top: 5px;">扫码入库 - 箱 ${sequenceLabel}</div>
          </div>
          <div class="footer">生成时间：${new Date().toLocaleString()}</div>
        </div>
        <script>
          window.onload = function() { window.print(); }
        <\/script>
      </body>
    </html>
  `)
  printWindow.document.close()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除入库单"${row.orderNo}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteInboundOrder(row.orderNo)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

onMounted(() => {
  loadSuppliers()
  loadData()
})
</script>

<style scoped>
.wms-page {
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
.search-form {
  margin-bottom: 20px;
}

/* 移动端卡片样式 */
.mobile-cards {
  display: none;
}

.order-card {
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.order-no {
  font-size: 15px;
  font-weight: 600;
  color: #409eff;
}

.card-body {
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-row .label {
  color: #909399;
  min-width: 80px;
  flex-shrink: 0;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  flex-wrap: wrap;
}

.card-actions .el-button {
  flex: 1;
  min-width: 60px;
}

/* 移动端显示卡片，隐藏表格 */
@media (max-width: 768px) {
  .desktop-table {
    display: none;
  }
  
  .mobile-cards {
    display: block;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .page-header .el-button {
    width: 100%;
  }
  
  .search-form {
    display: flex;
    flex-direction: column;
  }
  
  .search-form :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 12px;
    width: 100%;
  }
  
  .search-form :deep(.el-select),
  .search-form :deep(.el-date-picker) {
    width: 100% !important;
  }
}
</style>