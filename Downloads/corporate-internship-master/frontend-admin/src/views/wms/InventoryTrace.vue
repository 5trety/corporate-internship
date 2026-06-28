<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>库存追溯</h2>
    </div>

    <!-- 全部流水 -->
    <el-card shadow="never" class="trace-card">
      <template #header>
        <div class="card-header">
          <span>📋 全部流水</span>
          <el-tag type="info">通过条件筛选查看入库/出库记录</el-tag>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-form" style="margin-bottom: 16px">
        <el-form-item label="流水类型">
          <el-select
            v-model="traceParams.actionType"
            placeholder="全部"
            clearable
            style="width: 120px"
            @change="loadTraceData"
          >
            <el-option label="全部" value="" />
            <el-option label="入库" value="INBOUND" />
            <el-option label="出库" value="OUTBOUND" />
            <el-option label="转包拆分" value="TRANSFER_SPLIT" />
            <el-option label="转包合并" value="TRANSFER_MERGE" />
            <el-option label="封存" value="SEAL" />
            <el-option label="解封" value="UNSEAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="零件号">
          <el-input
            v-model="traceParams.partCode"
            placeholder="输入零件号"
            clearable
            style="width: 160px"
            @clear="loadTraceData"
            @keyup.enter="loadTraceData"
          />
        </el-form-item>
        <el-form-item label="看板号">
          <el-input
            v-model="traceParams.kanbanNo"
            placeholder="输入看板号"
            clearable
            style="width: 180px"
            @clear="loadTraceData"
            @keyup.enter="loadTraceData"
          />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="traceParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="loadTraceData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTraceData">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetTraceSearch">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="traceData"
        v-loading="traceLoading"
        stripe
      >
        <el-table-column prop="traceNo" label="追溯号" width="200" show-overflow-tooltip />
        <el-table-column prop="kanbanNo" label="看板号" width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ row.kanbanNo }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="单据号" width="180" />
        <el-table-column prop="partCode" label="零件号" width="120" />
        <el-table-column prop="partName" label="零件名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="90" align="center" />
        <el-table-column prop="warehouseCode" label="仓库" width="120" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="getActionTypeColor(row.actionType)" 
              size="small"
            >
              {{ getActionLabel(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="actionTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              type="primary" 
              size="small"
              @click="goToLifecycle(row.kanbanNo)"
            >
              <el-icon><Link /></el-icon>
              生命周期
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="tracePage"
        v-model:page-size="tracePageSize"
        :total="traceTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTraceData"
        @current-change="loadTraceData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Link } from '@element-plus/icons-vue'
import { getTraceList } from '../../api/wms'

const router = useRouter()

// ==================== 全部流水 ====================
const traceData = ref([])
const traceLoading = ref(false)
const tracePage = ref(1)
const tracePageSize = ref(20)
const traceTotal = ref(0)
const traceParams = ref({
  actionType: '',  // 全部/INBOUND/OUTBOUND
  partCode: '',
  kanbanNo: '',
  dateRange: null
})

const loadTraceData = async () => {
  traceLoading.value = true
  try {
    const params = {
      page: tracePage.value,
      pageSize: tracePageSize.value,
      partCode: traceParams.value.partCode,
      kanbanNo: traceParams.value.kanbanNo
    }

    // 只有选择了具体的类型才传actionType
    if (traceParams.value.actionType) {
      params.actionType = traceParams.value.actionType
    }

    if (traceParams.value.dateRange && traceParams.value.dateRange.length === 2) {
      params.startDate = traceParams.value.dateRange[0]
      params.endDate = traceParams.value.dateRange[1]
    }

    const res = await getTraceList(params)
    if (res.code === 200) {
      traceData.value = res.data?.list || []
      traceTotal.value = res.data?.total || 0
    }
  } catch (error) {
    console.error('加载流水记录失败:', error)
    ElMessage.error('加载流水记录失败')
  } finally {
    traceLoading.value = false
  }
}

const resetTraceSearch = () => {
  traceParams.value = {
    actionType: '',
    partCode: '',
    kanbanNo: '',
    dateRange: null
  }
  tracePage.value = 1
  loadTraceData()
}

// ==================== 跳转到生命周期页面 ====================
const goToLifecycle = (kanbanNo) => {
  // 跳转到看板生命周期页面，并传递看板号参数
  router.push({
    path: '/inventory-trace/lifecycle',
    query: { kanbanNo: kanbanNo }
  })
}

// ==================== 工具函数 ====================
const getStatusType = (status) => {
  const typeMap = {
    'scanned': 'warning',
    'stored': 'success',
    'outbound': 'info',
    'sealed': 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusLabel = (status) => {
  const labelMap = {
    'scanned': '入库中',
    'stored': '已入库',
    'outbound': '出库中',
    'sealed': '已封存'
  }
  return labelMap[status] || status
}

const getActionLabel = (actionType) => {
  const labelMap = {
    'INBOUND': '入库',
    'OUTBOUND': '出库',
    'TRANSFER': '转包',
    'TRANSFER_SPLIT': '转包拆分',
    'TRANSFER_MERGE': '转包合并',
    'SEAL': '封存',
    'UNSEAL': '解封'
  }
  return labelMap[actionType] || actionType
}

const getActionTypeColor = (actionType) => {
  const colorMap = {
    'INBOUND': 'success',
    'OUTBOUND': 'warning',
    'TRANSFER': 'primary',
    'TRANSFER_SPLIT': 'primary',
    'TRANSFER_MERGE': 'primary',
    'SEAL': 'danger',
    'UNSEAL': 'info'
  }
  return colorMap[actionType] || 'info'
}

const getTimelineType = (actionType) => {
  const typeMap = {
    'INBOUND': 'success',
    'OUTBOUND': 'warning',
    'TRANSFER': 'primary',
    'SEAL': 'danger',
    'UNSEAL': 'info'
  }
  return typeMap[actionType] || 'info'
}

onMounted(() => {
  loadTraceData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.lifecycle-header {
  margin-bottom: 20px;
}

:deep(.el-timeline-item__timestamp) {
  font-weight: 500;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
}
</style>
