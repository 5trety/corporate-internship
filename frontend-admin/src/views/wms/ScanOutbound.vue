<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>扫码出库</h2>
      <el-button @click="$router.back()">
        <el-icon><Back /></el-icon>
        返回
      </el-button>
    </div>

    <el-card shadow="never">
      <el-alert
        title="先进先出(FIFO)说明"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        系统会根据入库时间自动推荐应出库的看板，优先出库早入库的物料。
        请扫描看板上的二维码或输入看板号进行出库操作。
      </el-alert>

      <!-- 扫码输入区域 -->
      <div class="scan-input-section">
        <el-form :inline="true" class="scan-form">
          <el-form-item label="看板号" style="flex: 1">
            <el-input 
              v-model="scanInput" 
              placeholder="请扫描或输入看板号" 
              size="large"
              @keyup.enter="handleScan"
              ref="scanInputRef"
              style="width: 100%"
            >
              <template #append>
                <el-button @click="handleScan" size="large">扫码出库</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-form>
      </div>

      <!-- 出库结果展示 -->
      <div v-if="lastOutbound" class="outbound-result">
        <el-divider>最近出库记录</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="看板号">{{ lastOutbound.kanbanNo }}</el-descriptions-item>
          <el-descriptions-item label="出库数量">{{ lastOutbound.outboundQuantity }}</el-descriptions-item>
          <el-descriptions-item label="零件号">{{ lastOutbound.partCode }}</el-descriptions-item>
          <el-descriptions-item label="零件名称">{{ lastOutbound.partName }}</el-descriptions-item>
          <el-descriptions-item label="追溯号">{{ lastOutbound.traceNo }}</el-descriptions-item>
          <el-descriptions-item label="出库时间">{{ lastOutbound.outboundTime }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- FIFO推荐看板列表 -->
      <el-divider>FIFO推荐出库看板</el-divider>
      
      <div class="fifo-info">
        <el-alert
          title="系统会根据入库时间（FIFO原则）自动推荐应先出库的看板"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>

      <el-table :data="fifoKanbans" stripe v-loading="loadingFIFO" max-height="500">
        <el-table-column prop="kanban_no" label="看板号" width="180">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.kanban_no }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="part_code" label="零件号" width="150" />
        <el-table-column prop="part_name" label="零件名称" />
        <el-table-column prop="current_quantity" label="库存数量" width="100">
          <template #default="{ row }">
            <el-tag type="success">{{ row.current_quantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inbound_time" label="入库时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.inbound_time) }}
          </template>
        </el-table-column>
        <el-table-column label="库位" width="120">
          <template #default="{ row }">
            {{ row.location_code || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleScanKanban(row)"
              :disabled="row.status !== 'stored'"
            >
              扫码出库
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="fifoKanbans.length === 0 && !loadingFIFO" description="暂无待出库看板" />

      <!-- 所有库存看板 -->
      <el-divider>所有库存看板</el-divider>
      
      <el-table :data="allKanbans" stripe v-loading="loadingAll" max-height="400">
        <el-table-column prop="kanban_no" label="看板号" width="180">
          <template #default="{ row }">
            <el-tag :type="row.status === 'stored' ? 'primary' : 'info'">{{ row.kanban_no }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="part_code" label="零件号" width="150" />
        <el-table-column prop="part_name" label="零件名称" />
        <el-table-column prop="current_quantity" label="库存" width="80">
          <template #default="{ row }">
            <el-tag :type="row.current_quantity > 0 ? 'success' : 'info'">{{ row.current_quantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getKanbanStatusType(row.status)">{{ getKanbanStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleScanKanban(row)"
              :disabled="row.status !== 'stored' || row.current_quantity <= 0"
            >
              扫码
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 出库数量确认弹窗 -->
    <el-dialog title="确认出库数量" v-model="confirmDialogVisible" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="看板号">{{ currentKanban?.kanban_no }}</el-descriptions-item>
        <el-descriptions-item label="零件号">{{ currentKanban?.part_code }}</el-descriptions-item>
        <el-descriptions-item label="零件名称">{{ currentKanban?.part_name }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ currentKanban?.current_quantity }}</el-descriptions-item>
      </el-descriptions>
      
      <el-form style="margin-top: 20px">
        <el-form-item label="出库数量">
          <el-input-number 
            v-model="outboundQuantity" 
            :min="1" 
            :max="currentKanban?.current_quantity || 1" 
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmOutbound" :loading="confirming">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { 
  validateKanbanForOutbound, 
  scanOutbound, 
  getFIFOKanbans,
  getKanbanList
} from '../../api/wms'

const router = useRouter()
const scanInput = ref('')
const scanInputRef = ref(null)
const loadingFIFO = ref(false)
const loadingAll = ref(false)
const fifoKanbans = ref([])
const allKanbans = ref([])
const lastOutbound = ref(null)

// 出库确认相关
const confirmDialogVisible = ref(false)
const confirming = ref(false)
const currentKanban = ref(null)
const outboundQuantity = ref(1)

const getKanbanStatusType = (status) => {
  switch (status) {
    case 'pending': return 'warning'
    case 'stored': return 'success'
    case 'outbound': return 'info'
    default: return ''
  }
}

const getKanbanStatusText = (status) => {
  switch (status) {
    case 'pending': return '待入库'
    case 'stored': return '已入库'
    case 'outbound': return '已出库'
    default: return status
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  try {
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    return timeStr
  }
}

const loadFIFOKanbans = async () => {
  loadingFIFO.value = true
  try {
    const res = await getFIFOKanbans({})
    if (res.code === 200) {
      fifoKanbans.value = res.data || []
    }
  } finally {
    loadingFIFO.value = false
  }
}

const loadAllKanbans = async () => {
  loadingAll.value = true
  try {
    const res = await getKanbanList({ status: '', page: 1, pageSize: 100 })
    if (res.code === 200) {
      allKanbans.value = res.data?.list || []
    }
  } finally {
    loadingAll.value = false
  }
}

const handleScan = async () => {
  if (!scanInput.value.trim()) {
    ElMessage.warning('请输入看板号')
    return
  }
  
  await performOutbound(scanInput.value.trim())
  scanInput.value = ''
  
  // 重新聚焦输入框
  nextTick(() => {
    scanInputRef.value?.focus()
  })
}

const handleScanKanban = (row) => {
  currentKanban.value = row
  outboundQuantity.value = Math.min(row.current_quantity, 1)
  confirmDialogVisible.value = true
}

const confirmOutbound = async () => {
  if (!currentKanban.value) return
  
  confirming.value = true
  try {
    const res = await scanOutbound({
      kanbanNo: currentKanban.value.kanban_no,
      outboundQuantity: outboundQuantity.value
    })
    
    if (res.code === 200) {
      ElMessage.success('出库成功！')
      
      // 更新最近出库记录
      lastOutbound.value = {
        kanbanNo: currentKanban.value.kanban_no,
        partCode: currentKanban.value.part_code,
        partName: currentKanban.value.part_name,
        outboundQuantity: outboundQuantity.value,
        traceNo: res.data?.traceNo || '',
        outboundTime: new Date().toLocaleString()
      }
      
      // 刷新列表
      await Promise.all([loadFIFOKanbans(), loadAllKanbans()])
      
      confirmDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '出库失败')
    }
  } finally {
    confirming.value = false
  }
}

const performOutbound = async (kanbanNo) => {
  try {
    // 先验证看板
    const validateRes = await validateKanbanForOutbound(kanbanNo)
    if (validateRes.code !== 200) {
      ElMessage.error(validateRes.message || '看板验证失败')
      return
    }
    
    const kanbanData = validateRes.data
    
    // 询问出库数量
    currentKanban.value = {
      kanban_no: kanbanNo,
      part_code: kanbanData.part_code,
      part_name: kanbanData.part_name,
      current_quantity: kanbanData.current_quantity
    }
    outboundQuantity.value = kanbanData.current_quantity
    confirmDialogVisible.value = true
    
  } catch (error) {
    console.error('扫码出库失败:', error)
    ElMessage.error('扫码出库失败: ' + (error.message || '未知错误'))
  }
}

onMounted(async () => {
  // 自动聚焦扫码输入框
  nextTick(() => {
    scanInputRef.value?.focus()
  })
  
  // 加载数据
  await Promise.all([
    loadFIFOKanbans(),
    loadAllKanbans()
  ])
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
.scan-input-section {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}
.scan-form {
  display: flex;
  align-items: center;
}
.scan-form :deep(.el-form-item) {
  flex: 1;
  margin-bottom: 0;
}
.outbound-result {
  margin-bottom: 20px;
}
.fifo-info {
  margin-bottom: 16px;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .page-header .el-button {
    width: 100%;
  }
  
  .scan-input-section {
    padding: 12px;
  }
  
  .el-table {
    font-size: 13px;
  }
}
</style>
