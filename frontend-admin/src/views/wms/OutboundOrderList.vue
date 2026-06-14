<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>出库单管理</h2>
      <el-button type="primary" @click="$router.push('/wms/outbound-order/create')">
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
        <el-table-column prop="orderNo" label="出库单号" width="180" />
        <el-table-column prop="outboundType" label="出库类型" width="120" />
        <el-table-column prop="customerName" label="客户" width="150" />
        <el-table-column prop="totalQuantity" label="总数量" width="100" />
        <el-table-column prop="shippedQuantity" label="已出库" width="100" />
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.shippedQuantity / row.totalQuantity * 100)" :stroke-width="8" />
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
            <el-button link type="success" @click="openPrintKanban(row)" v-if="row.status !== 'completed'">打印看板</el-button>
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
              <span class="label">出库类型：</span>
              <span>{{ row.outboundType }}</span>
            </div>
            <div class="info-row">
              <span class="label">客户：</span>
              <span>{{ row.customerName }}</span>
            </div>
            <div class="info-row">
              <span class="label">数量：</span>
              <span>{{ row.shippedQuantity }} / {{ row.totalQuantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">进度：</span>
              <el-progress :percentage="Math.round(row.shippedQuantity / row.totalQuantity * 100)" :stroke-width="6" style="flex: 1" />
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
    <el-dialog title="出库单详情" v-model="detailVisible" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="出库单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="出库类型">{{ detailData.outboundType }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detailData.customerName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detailData.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="已出库">{{ detailData.shippedQuantity }}</el-descriptions-item>
        <el-descriptions-item label="总箱数">{{ detailData.totalBoxes }}</el-descriptions-item>
        <el-descriptions-item label="已出库箱数">{{ detailData.shippedBoxes }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag :type="getStatusType(detailData.status)">{{ detailData.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>出库明细</el-divider>
      <el-table :data="detailDetails" stripe size="small">
        <el-table-column prop="part_code" label="零件号" width="180" />
        <el-table-column prop="part_name" label="零件名称" />
        <el-table-column prop="expected_quantity" label="预期数量" width="100" />
        <el-table-column prop="shipped_quantity" label="已出库" width="100" />
        <el-table-column prop="expected_boxes" label="预期箱数" width="100" />
        <el-table-column prop="shipped_boxes" label="已出库箱数" width="100" />
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.shipped_quantity / row.expected_quantity * 100)" :stroke-width="6" />
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 打印看板弹窗 -->
    <el-dialog title="打印出库看板" v-model="kanbanDialogVisible" width="900px">
      <el-table :data="kanbanItems" stripe>
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" width="150" />
        <el-table-column prop="expectedQuantity" label="数量" width="80" />
        <el-table-column prop="expectedBoxes" label="箱数" width="80" />
        <el-table-column label="看板号" width="220">
          <template #default="{ row }">
            <el-input v-model="row.kanbanNo" placeholder="自动生成" disabled />
          </template>
        </el-table-column>
        <el-table-column label="二维码" width="120">
          <template #default="{ row }">
            <img v-if="row.qrCodeImage" :src="row.qrCodeImage" style="width: 80px; height: 80px;" />
            <span v-else>生成中...</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="printSingleKanban(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 20px; text-align: center">
        <el-button type="primary" @click="generateAndPrintAll">批量生成并打印</el-button>
      </div>
      <template #footer>
        <el-button @click="kanbanDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getOutboundOrderList, getOutboundOrderDetail, deleteOutboundOrder, printKanban } from '../../api/wms'
import QRCode from 'qrcode'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filterStatus = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const detailData = ref({})
const detailDetails = ref([])

// 看板相关
const kanbanDialogVisible = ref(false)
const kanbanItems = ref([])
const currentOrder = ref(null)

const getStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'partial': return 'primary'
    case 'completed': return 'success'
    default: return 'info'
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: filterStatus.value,
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
  dateRange.value = []
  page.value = 1
  loadData()
}

const viewDetail = async (row) => {
  const res = await getOutboundOrderDetail(row.orderNo)
  if (res.code === 200) {
    detailData.value = res.data?.order || {}
    detailDetails.value = res.data?.details || []
    detailVisible.value = true
  }
}

// 编辑出库单
const editOrder = (row) => {
  router.push(`/wms/outbound-order/edit/${row.orderNo}`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除出库单"${row.orderNo}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteOutboundOrder(row.orderNo)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

const openPrintKanban = async (row) => {
  currentOrder.value = row
  // 获取出库单详情
  const res = await getOutboundOrderDetail(row.orderNo)
  if (res.code === 200) {
    const details = res.data?.details || []
    console.log('出库单详情:', details)
    // 使用后端返回的下划线格式字段名
    kanbanItems.value = details.map(d => ({
      partCode: d.part_code || d.partCode,
      partName: d.part_name || d.partName,
      expectedQuantity: d.expected_quantity || d.expectedQuantity,
      expectedBoxes: d.expected_boxes || d.expectedBoxes || 1,
      kanbanNo: '',
      qrCodeImage: null
    }))
    kanbanDialogVisible.value = true
  } else {
    ElMessage.error('获取出库单详情失败')
  }
}

// 生成单个二维码
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

const generateAndPrintAll = async () => {
  if (!currentOrder.value) return

  console.log('准备生成看板的kanbanItems:', kanbanItems.value)

  const items = kanbanItems.value.map(item => ({
    partCode: item.partCode,
    partName: item.partName,
    quantity: item.expectedQuantity,
    boxCount: item.expectedBoxes || 1
  }))

  console.log('发送到后端的数据:', {
    orderNo: currentOrder.value.orderNo,
    items: items
  })

  // 显示加载提示
  const loadingMsg = ElMessage.info({ message: '正在生成看板和二维码...', duration: 0 })

  try {
    const res = await printKanban({
      orderNo: currentOrder.value.orderNo,
      items: items
    })

    if (res.code === 200) {
      const kanbans = res.data || []
      ElMessage.success(`成功生成 ${kanbans.length} 个看板`)

      // 更新看板号并生成二维码
      for (let i = 0; i < kanbanItems.value.length; i++) {
        if (kanbans[i]) {
          kanbanItems.value[i].kanbanNo = kanbans[i].kanbanNo
          const qrText = kanbans[i].kanbanNo
          kanbanItems.value[i].qrCodeImage = await generateQRCode(qrText)
        }
      }

      loadingMsg.close()

      // 打印所有看板
      const printWindow = window.open('', '_blank')
      printWindow.document.write(`
        <html>
          <head>
            <title>打印出库看板</title>
            <style>
              body { font-family: Arial, sans-serif; padding: 20px; }
              .kanban-card {
                border: 2px solid #333;
                padding: 20px;
                margin-bottom: 20px;
                width: 350px;
                page-break-after: always;
              }
              .kanban-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; }
              .kanban-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; }
              .kanban-row { margin: 8px 0; white-space: nowrap; }
              .kanban-label { font-weight: bold; display: inline-block; width: 70px; margin-right: 10px; }
              .kanban-value { display: inline-block; }
              .qr-code { text-align: center; margin: 15px 0; }
              .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; }
            </style>
          </head>
          <body>
            ${kanbanItems.value.map((item, index) => `
              <div class="kanban-card">
                <div class="kanban-title">出库看板</div>
                <div class="kanban-code">${item.kanbanNo}</div>
                <div class="kanban-row"><span class="kanban-label">零件号：</span><span class="kanban-value">${item.partCode}</span></div>
                <div class="kanban-row"><span class="kanban-label">零件名称：</span><span class="kanban-value">${item.partName}</span></div>
                <div class="kanban-row"><span class="kanban-label">数量：</span><span class="kanban-value">${item.expectedQuantity}</span></div>
                <div class="kanban-row"><span class="kanban-label">客户：</span><span class="kanban-value">${currentOrder.value.customerName}</span></div>
                <div class="kanban-row"><span class="kanban-label">出库单号：</span><span class="kanban-value">${currentOrder.value.orderNo}</span></div>
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
    } else {
      loadingMsg.close()
      ElMessage.error(res.message || '生成看板失败')
    }
  } catch (error) {
    loadingMsg.close()
    console.error('生成看板失败:', error)
    ElMessage.error('生成看板失败: ' + (error.message || '未知错误'))
  }
}

const printSingleKanban = async (row) => {
  if (!row.kanbanNo) {
    ElMessage.warning('请先批量生成看板')
    return
  }

  // 如果还没有二维码，先生成
  let qrImage = row.qrCodeImage
  if (!qrImage) {
    const qrText = row.kanbanNo
    qrImage = await generateQRCode(qrText)
  }

  // 打印单个看板
  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <html>
      <head>
        <title>打印出库看板 - ${row.kanbanNo}</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 20px; }
          .kanban-card {
            border: 2px solid #333;
            padding: 20px;
            width: 350px;
            margin: 0 auto;
          }
          .kanban-title { font-size: 18px; font-weight: bold; text-align: center; margin-bottom: 10px; }
          .kanban-code { font-size: 12px; color: #666; text-align: center; margin-bottom: 15px; word-break: break-all; }
          .kanban-row { margin: 8px 0; white-space: nowrap; }
          .kanban-label { font-weight: bold; display: inline-block; width: 70px; margin-right: 10px; }
          .kanban-value { display: inline-block; }
          .qr-code { text-align: center; margin: 15px 0; }
          .footer { text-align: center; font-size: 10px; color: #999; margin-top: 10px; }
        </style>
      </head>
      <body>
        <div class="kanban-card">
          <div class="kanban-title">出库看板</div>
          <div class="kanban-code">${row.kanbanNo}</div>
          <div class="kanban-row"><span class="kanban-label">零件号：</span><span class="kanban-value">${row.partCode}</span></div>
          <div class="kanban-row"><span class="kanban-label">零件名称：</span><span class="kanban-value">${row.partName}</span></div>
          <div class="kanban-row"><span class="kanban-label">数量：</span><span class="kanban-value">${row.expectedQuantity}</span></div>
          <div class="kanban-row"><span class="kanban-label">客户：</span><span class="kanban-value">${currentOrder.value?.customerName}</span></div>
          <div class="kanban-row"><span class="kanban-label">出库单号：</span><span class="kanban-value">${currentOrder.value?.orderNo}</span></div>
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
