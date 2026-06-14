<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>出库单管理</h2>
      <el-button type="primary" @click="$router.push('/wms-outbound/outbound-order/create')">
        <el-icon><Plus /></el-icon>
        创建出库单
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="待出库" value="pending" />
            <el-option label="部分出库" value="partial" />
            <el-option label="已完成" value="completed" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户编码">
          <el-input v-model="filterCustomerCode" placeholder="请输入客户编码" clearable @change="loadData" style="width: 150px" />
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
        <el-table-column prop="order_no" label="出库单号" width="180" />
        <el-table-column prop="outbound_type" label="出库类型" width="120" />
        <el-table-column prop="customer_name" label="客户名称" width="150" />
        <el-table-column prop="customer_code" label="客户编码" width="120" />
        <el-table-column prop="total_quantity" label="总数量" width="100" />
        <el-table-column prop="shipped_quantity" label="已出库" width="100" />
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.total_quantity > 0 ? Math.round(row.shipped_quantity / row.total_quantity * 100) : 0" 
              :stroke-width="8" 
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_by" label="创建人" width="120" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <el-button link type="primary" @click="editOrder(row)" v-if="row.status !== 'completed'">编辑</el-button>
            <el-button link type="success" @click="openScanOutbound(row)" v-if="row.status !== 'completed'">扫码出库</el-button>
            <el-button link type="primary" @click="openPrintOutbound(row)" v-if="row.status !== 'completed'">打印</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-if="row.status === 'pending'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-cards" v-loading="loading">
        <el-card v-for="row in tableData" :key="row.order_no" shadow="hover" class="order-card">
          <div class="card-header">
            <div class="order-no">{{ row.order_no }}</div>
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">出库类型：</span>
              <span>{{ row.outbound_type }}</span>
            </div>
            <div class="info-row">
              <span class="label">客户：</span>
              <span>{{ row.customer_name }}</span>
            </div>
            <div class="info-row">
              <span class="label">数量：</span>
              <span>{{ row.shipped_quantity }} / {{ row.total_quantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">进度：</span>
              <el-progress :percentage="row.total_quantity > 0 ? Math.round(row.shipped_quantity / row.total_quantity * 100) : 0" :stroke-width="6" style="flex: 1" />
            </div>
            <div class="info-row">
              <span class="label">创建时间：</span>
              <span>{{ row.created_at }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button size="small" type="primary" @click="editOrder(row)" v-if="row.status !== 'completed'">编辑</el-button>
            <el-button size="small" type="success" @click="openScanOutbound(row)" v-if="row.status !== 'completed'">出库</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="row.status === 'pending'">删除</el-button>
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
    <el-dialog title="出库单详情" v-model="detailVisible" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ detailData.order_no }}</el-descriptions-item>
        <el-descriptions-item label="出库类型">{{ detailData.outbound_type }}</el-descriptions-item>
        <el-descriptions-item label="客户编码">{{ detailData.customer_code }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detailData.customer_name }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouse_name || '默认仓库' }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detailData.total_quantity }}</el-descriptions-item>
        <el-descriptions-item label="已出库">{{ detailData.shipped_quantity }}</el-descriptions-item>
        <el-descriptions-item label="总箱数">{{ detailData.total_boxes }}</el-descriptions-item>
        <el-descriptions-item label="已出库箱数">{{ detailData.shipped_boxes }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detailData.created_by }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.created_at }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>出库明细</el-divider>
      <el-table :data="detailDetails" stripe size="small">
        <el-table-column prop="part_code" label="零件号" width="150" />
        <el-table-column prop="part_name" label="零件名称" />
        <el-table-column prop="expected_quantity" label="预期数量" width="100" />
        <el-table-column prop="shipped_quantity" label="已出库" width="100" />
        <el-table-column prop="packaging_capacity" label="包装容量" width="100" />
        <el-table-column prop="expected_boxes" label="预期箱数" width="100" />
        <el-table-column prop="shipped_boxes" label="已出库箱数" width="100" />
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.expected_quantity > 0 ? Math.round(row.shipped_quantity / row.expected_quantity * 100) : 0" 
              :stroke-width="6" 
            />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 打印出库单弹窗 -->
    <el-dialog title="打印出库单" v-model="printDialogVisible" width="900px">
      <el-table :data="printItems" stripe>
        <el-table-column prop="part_code" label="零件号" width="150" />
        <el-table-column prop="part_name" label="零件名称" width="150" />
        <el-table-column prop="expected_quantity" label="预期数量" width="80" />
        <el-table-column prop="shipped_quantity" label="已出库" width="80" />
        <el-table-column label="二维码" width="120">
          <template #default="{ row }">
            <img v-if="row.qrCodeImage" :src="row.qrCodeImage" style="width: 80px; height: 80px;" />
            <span v-else>生成中...</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="printSingleItem(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 20px; text-align: center">
        <el-button type="primary" @click="generateAndPrintAll">批量生成并打印</el-button>
      </div>
      <template #footer>
        <el-button @click="printDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 扫码出库弹窗 -->
    <el-dialog title="扫码出库" v-model="scanDialogVisible" width="900px">
      <el-alert
        title="先进先出(FIFO)说明"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        系统会根据入库时间自动推荐应出库的看板，优先出库早入库的物料。
      </el-alert>

      <el-form :inline="true" class="scan-form">
        <el-form-item label="扫码输入">
          <el-input 
            v-model="scanInput" 
            placeholder="请扫描或输入看板号" 
            @keyup.enter="handleScan"
            style="width: 300px"
          >
            <template #append>
              <el-button @click="handleScan">扫码</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <el-divider>推荐出库看板（FIFO）</el-divider>
      <el-table :data="fifoKanbans" stripe size="small">
        <el-table-column prop="kanban_no" label="看板号" width="180" />
        <el-table-column prop="part_code" label="零件号" width="150" />
        <el-table-column prop="part_name" label="零件名称" />
        <el-table-column prop="current_quantity" label="库存数量" width="100" />
        <el-table-column prop="inbound_time" label="入库时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleScanKanban(row)">扫码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="scanDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { 
  getOutboundOrderList, 
  getOutboundOrderDetail, 
  deleteOutboundOrder,
  printOutboundOrder,
  validateKanbanForOutbound,
  scanOutbound,
  getFIFOKanbans
} from '../../api/wms'
import QRCode from 'qrcode'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filterStatus = ref('')
const filterCustomerCode = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const detailData = ref({})
const detailDetails = ref([])

// 打印相关
const printDialogVisible = ref(false)
const printItems = ref([])
const currentPrintOrder = ref(null)

// 扫码出库相关
const scanDialogVisible = ref(false)
const scanInput = ref('')
const fifoKanbans = ref([])
const currentScanOrder = ref(null)

const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'partial': return 'primary'
    case 'completed': return 'success'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'pending': return '待出库'
    case 'partial': return '部分出库'
    case 'completed': return '已完成'
    default: return status
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: filterStatus.value,
      customerCode: filterCustomerCode.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
    const res = await getOutboundOrderList(params)
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
  filterCustomerCode.value = ''
  dateRange.value = []
  page.value = 1
  loadData()
}

const viewDetail = async (row) => {
  const res = await getOutboundOrderDetail(row.order_no)
  if (res.code === 200) {
    detailData.value = res.data?.order || {}
    detailDetails.value = res.data?.details || []
    detailVisible.value = true
  }
}

const editOrder = (row) => {
  router.push(`/wms-outbound/outbound-order/edit/${row.order_no}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该出库单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteOutboundOrder(row.order_no)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 打印出库单
const openPrintOutbound = async (row) => {
  currentPrintOrder.value = row
  const res = await printOutboundOrder(row.order_no)
  if (res.code === 200) {
    const details = res.data?.details || []
    printItems.value = details.map(d => ({
      ...d,
      qrCodeImage: null
    }))
    printDialogVisible.value = true
    
    // 自动生成二维码
    setTimeout(() => generateAllQRCodes(), 100)
  } else {
    ElMessage.error(res.message || '获取打印数据失败')
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

const generateAllQRCodes = async () => {
  for (const item of printItems.value) {
    if (!item.qrCodeImage && item.qrContent) {
      item.qrCodeImage = await generateQRCode(item.qrContent)
    }
  }
}

const generateAndPrintAll = async () => {
  if (!currentPrintOrder.value) return
  
  const loadingMsg = ElMessage.info({ message: '正在生成二维码...', duration: 0 })
  
  try {
    await generateAllQRCodes()
    loadingMsg.close()
    
    // 打印所有出库项
    const printWindow = window.open('', '_blank')
    printWindow.document.write(`
      <html>
        <head>
          <title>打印出库单</title>
          <style>
            body { font-family: Arial, sans-serif; padding: 20px; }
            .outbound-card {
              border: 2px solid #333;
              padding: 20px;
              margin-bottom: 20px;
              width: 350px;
              page-break-after: always;
            }
            .outbound-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; }
            .outbound-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; }
            .outbound-row { margin: 8px 0; white-space: nowrap; }
            .outbound-label { font-weight: bold; display: inline-block; width: 80px; margin-right: 10px; }
            .outbound-value { display: inline-block; }
            .qr-code { text-align: center; margin: 15px 0; }
            .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; }
          </style>
        </head>
        <body>
          ${printItems.value.map((item) => `
            <div class="outbound-card">
              <div class="outbound-title">出库看板</div>
              <div class="outbound-code">${item.qrContent || '扫码出库'}</div>
              <div class="outbound-row"><span class="outbound-label">零件号：</span><span class="outbound-value">${item.part_code}</span></div>
              <div class="outbound-row"><span class="outbound-label">零件名称：</span><span class="outbound-value">${item.part_name}</span></div>
              <div class="outbound-row"><span class="outbound-label">出库数量：</span><span class="outbound-value">${item.expected_quantity}</span></div>
              <div class="outbound-row"><span class="outbound-label">客户：</span><span class="outbound-value">${currentPrintOrder.value.customer_name}</span></div>
              <div class="outbound-row"><span class="outbound-label">出库单号：</span><span class="outbound-value">${currentPrintOrder.value.order_no}</span></div>
              <div class="qr-code">
                <img src="${item.qrCodeImage}" style="width: 100px; height: 100px;" alt="二维码">
                <div style="font-size: 10px; margin-top: 5px;">扫码出库</div>
              </div>
              <div class="footer">生成时间：${new Date().toLocaleString()}</div>
            </div>
          `).join('')}
        </body>
      </html>
    `)
    printWindow.document.close()
    printWindow.print()
  } catch (error) {
    loadingMsg.close()
    console.error('生成二维码失败:', error)
    ElMessage.error('生成二维码失败: ' + (error.message || '未知错误'))
  }
}

const printSingleItem = async (row) => {
  if (!currentPrintOrder.value) return
  
  let qrImage = row.qrCodeImage
  if (!qrImage && row.qrContent) {
    qrImage = await generateQRCode(row.qrContent)
  }
  
  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <html>
      <head>
        <title>打印出库看板</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; }
          .outbound-card {
            border: 2px solid #333;
            padding: 20px;
            width: 350px;
            margin: 0 auto;
          }
          .outbound-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; }
          .outbound-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; }
          .outbound-row { margin: 8px 0; white-space: nowrap; }
          .outbound-label { font-weight: bold; display: inline-block; width: 80px; margin-right: 10px; }
          .outbound-value { display: inline-block; }
          .qr-code { text-align: center; margin: 15px 0; }
          .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; }
        </style>
      </head>
      <body>
        <div class="outbound-card">
          <div class="outbound-title">出库看板</div>
          <div class="outbound-code">${row.qrContent || '扫码出库'}</div>
          <div class="outbound-row"><span class="outbound-label">零件号：</span><span class="outbound-value">${row.part_code}</span></div>
          <div class="outbound-row"><span class="outbound-label">零件名称：</span><span class="outbound-value">${row.part_name}</span></div>
          <div class="outbound-row"><span class="outbound-label">出库数量：</span><span class="outbound-value">${row.expected_quantity}</span></div>
          <div class="outbound-row"><span class="outbound-label">客户：</span><span class="outbound-value">${currentPrintOrder.value.customer_name}</span></div>
          <div class="outbound-row"><span class="outbound-label">出库单号：</span><span class="outbound-value">${currentPrintOrder.value.order_no}</span></div>
          <div class="qr-code">
            <img src="${qrImage}" style="width: 100px; height: 100px;" alt="二维码">
            <div style="font-size: 10px; margin-top: 5px;">扫码出库</div>
          </div>
          <div class="footer">生成时间：${new Date().toLocaleString()}</div>
        </div>
      </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.print()
}

// 扫码出库
const openScanOutbound = async (row) => {
  currentScanOrder.value = row
  scanInput.value = ''
  fifoKanbans.value = []
  
  // 加载FIFO推荐看板
  const res = await getFIFOKanbans({ orderNo: row.order_no })
  if (res.code === 200) {
    fifoKanbans.value = res.data || []
  }
  
  scanDialogVisible.value = true
}

const handleScan = async () => {
  if (!scanInput.value.trim()) {
    ElMessage.warning('请输入看板号')
    return
  }
  
  await performOutbound(scanInput.value.trim())
  scanInput.value = ''
}

const handleScanKanban = async (row) => {
  await performOutbound(row.kanban_no)
}

const performOutbound = async (kanbanNo) => {
  try {
    // 先验证看板
    const validateRes = await validateKanbanForOutbound(kanbanNo)
    if (validateRes.code !== 200) {
      ElMessage.error(validateRes.message || '看板验证失败')
      return
    }
    
    // 执行出库
    const outboundRes = await scanOutbound({
      kanbanNo: kanbanNo,
      orderNo: currentScanOrder.value.order_no
    })
    
    if (outboundRes.code === 200) {
      ElMessage.success(`出库成功！看板号：${kanbanNo}`)
      
      // 刷新FIFO列表
      const fifoRes = await getFIFOKanbans({ orderNo: currentScanOrder.value.order_no })
      if (fifoRes.code === 200) {
        fifoKanbans.value = fifoRes.data || []
      }
      
      // 刷新列表
      loadData()
    } else {
      ElMessage.error(outboundRes.message || '出库失败')
    }
  } catch (error) {
    console.error('出库失败:', error)
    ElMessage.error('出库失败: ' + (error.message || '未知错误'))
  }
}

onMounted(() => {
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
}
.card-header .order-no {
  font-weight: bold;
  font-size: 14px;
}
.card-body {
  margin-bottom: 12px;
}
.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}
.info-row .label {
  color: #909399;
  margin-right: 8px;
  flex-shrink: 0;
}
.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 响应式 */
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
  .search-form :deep(.el-form-item) {
    display: block;
    margin-bottom: 12px;
  }
}

.scan-form {
  margin-bottom: 20px;
}
</style>
