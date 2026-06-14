<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>库存追溯</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部流水" name="all" />
      <el-tab-pane label="入库历史" name="inbound" />
      <el-tab-pane label="出库历史" name="outbound" />
      <el-tab-pane label="当前库存" name="inventory" />
      <el-tab-pane label="看板生命周期" name="lifecycle" />
    </el-tabs>

    <section v-if="traceTabs.includes(activeTab)" class="trace-section">
      <el-form :inline="true" class="search-form">
        <el-form-item label="零件号">
          <el-input v-model="traceParams.partCode" placeholder="零件号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="看板号">
          <el-input v-model="traceParams.kanbanNo" placeholder="看板号" clearable style="width: 180px" />
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

      <el-table :data="traceData" v-loading="traceLoading" stripe>
        <el-table-column prop="traceNo" label="追溯号" width="190" show-overflow-tooltip />
        <el-table-column prop="kanbanNo" label="看板号" width="190" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="单据号" width="160" show-overflow-tooltip />
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column prop="actionType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.actionType)" size="small">
              {{ getActionLabel(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="actionTime" label="操作时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="tracePage"
        v-model:page-size="tracePageSize"
        :total="traceTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTraceData"
        @current-change="loadTraceData"
      />
    </section>

    <section v-else-if="activeTab === 'inventory'" class="trace-section">
      <el-row :gutter="16" class="summary-row">
        <el-col :xs="24" :sm="12" :md="8">
          <el-statistic title="总库存数量" :value="inventorySummary.total_quantity || 0" />
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <el-statistic title="有效看板数" :value="inventorySummary.total_count || 0" />
        </el-col>
      </el-row>

      <el-form :inline="true" class="search-form">
        <el-form-item label="零件号">
          <el-input v-model="inventoryParams.partCode" placeholder="零件号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="库位">
          <el-select v-model="inventoryParams.locationCode" placeholder="全部" clearable style="width: 160px">
            <el-option
              v-for="loc in locations"
              :key="loc.location_code"
              :label="loc.location_name"
              :value="loc.location_code"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadInventoryData">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetInventorySearch">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="inventoryData" v-loading="inventoryLoading" stripe>
        <el-table-column prop="kanban_no" label="看板号" width="210" show-overflow-tooltip />
        <el-table-column prop="part_code" label="零件号" width="150" />
        <el-table-column prop="part_name" label="零件名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="supplier_name" label="供应商" width="150" show-overflow-tooltip />
        <el-table-column prop="quantity" label="当前库存" width="110" />
        <el-table-column prop="location_name" label="库位" width="130" />
        <el-table-column prop="created_at" label="入库时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="queryLifecycle(row.kanban_no)">生命周期</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="inventoryPage"
        v-model:page-size="inventoryPageSize"
        :total="inventoryTotal"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadInventoryData"
        @current-change="loadInventoryData"
      />
    </section>

    <section v-else class="trace-section">
      <el-form :inline="true" class="search-form">
        <el-form-item label="看板号">
          <el-input
            v-model="lifecycleKanbanNo"
            placeholder="请输入看板号"
            clearable
            style="width: 260px"
            @keyup.enter="loadLifecycle"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLifecycle">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
      </el-form>

      <el-empty v-if="!lifecycleData" description="暂无看板生命周期数据" />

      <template v-else>
        <el-descriptions :column="2" border class="lifecycle-summary">
          <el-descriptions-item label="看板号">{{ lifecycleData.kanban.kanban_no }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag>{{ getKanbanStatusLabel(lifecycleData.kanban.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="零件号">{{ lifecycleData.kanban.part_code }}</el-descriptions-item>
          <el-descriptions-item label="零件名称">{{ lifecycleData.kanban.part_name }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ lifecycleData.kanban.supplier_name }}</el-descriptions-item>
          <el-descriptions-item label="库位">
            {{ lifecycleData.kanban.location_name || lifecycleData.kanban.location_code || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="看板数量">{{ lifecycleData.kanban.quantity }}</el-descriptions-item>
          <el-descriptions-item label="当前库存">
            {{ lifecycleCurrentQuantity }}
          </el-descriptions-item>
        </el-descriptions>

        <el-timeline class="lifecycle-timeline">
          <el-timeline-item
            v-for="item in lifecycleData.traces"
            :key="item.id"
            :timestamp="item.action_time"
            :type="item.action_type === 'INBOUND' ? 'success' : 'warning'"
            placement="top"
          >
            <div class="timeline-item">
              <div class="timeline-title">{{ getActionLabel(item.action_type) }}</div>
              <div class="timeline-line">
                {{ item.order_no }} / {{ item.part_code }} / 数量 {{ item.quantity }}
              </div>
              <div class="timeline-line">
                库位 {{ item.location_code || '-' }}，操作人 {{ item.operator || '-' }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-if="lifecycleData.traces.length === 0" description="暂无流水记录" />
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import { getCurrentInventory, getKanbanLifecycle, getLocationList, getTraceList } from '../../api/wms'

const activeTab = ref('all')
const traceTabs = ['all', 'inbound', 'outbound']

const traceLoading = ref(false)
const traceData = ref([])
const traceTotal = ref(0)
const tracePage = ref(1)
const tracePageSize = ref(20)
const traceParams = reactive({
  partCode: '',
  kanbanNo: '',
  dateRange: null
})

const inventoryLoading = ref(false)
const inventoryData = ref([])
const inventoryTotal = ref(0)
const inventoryPage = ref(1)
const inventoryPageSize = ref(20)
const inventorySummary = ref({})
const inventoryParams = reactive({
  partCode: '',
  locationCode: ''
})
const locations = ref([])

const lifecycleKanbanNo = ref('')
const lifecycleData = ref(null)
const lifecycleLoading = ref(false)

const currentActionType = computed(() => {
  if (activeTab.value === 'inbound') return 'INBOUND'
  if (activeTab.value === 'outbound') return 'OUTBOUND'
  return ''
})

const lifecycleCurrentQuantity = computed(() => {
  if (!lifecycleData.value) return 0
  return lifecycleData.value.currentInventory.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
})

const getActionLabel = (type) => {
  if (type === 'INBOUND') return '入库'
  if (type === 'OUTBOUND') return '出库'
  return type || '-'
}

const getActionTagType = (type) => {
  if (type === 'INBOUND') return 'success'
  if (type === 'OUTBOUND') return 'warning'
  return 'info'
}

const getKanbanStatusLabel = (status) => {
  const labels = {
    pending: '待入库',
    scanned: '已扫码',
    stored: '在库',
    outbound: '已出库'
  }
  return labels[status] || status || '-'
}

const loadTraceData = async () => {
  traceLoading.value = true
  try {
    const res = await getTraceList({
      page: tracePage.value,
      pageSize: tracePageSize.value,
      actionType: currentActionType.value,
      partCode: traceParams.partCode,
      kanbanNo: traceParams.kanbanNo,
      startDate: traceParams.dateRange?.[0],
      endDate: traceParams.dateRange?.[1]
    })
    if (res.code === 200) {
      traceData.value = res.data?.list || []
      traceTotal.value = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '加载追溯流水失败')
    }
  } finally {
    traceLoading.value = false
  }
}

const resetTraceSearch = () => {
  traceParams.partCode = ''
  traceParams.kanbanNo = ''
  traceParams.dateRange = null
  tracePage.value = 1
  loadTraceData()
}

const loadInventoryData = async () => {
  inventoryLoading.value = true
  try {
    const res = await getCurrentInventory({
      page: inventoryPage.value,
      pageSize: inventoryPageSize.value,
      partCode: inventoryParams.partCode,
      locationCode: inventoryParams.locationCode
    })
    if (res.code === 200) {
      inventoryData.value = res.data?.list || []
      inventoryTotal.value = res.data?.total || 0
      inventorySummary.value = res.data?.summary || {}
    } else {
      ElMessage.error(res.message || '加载当前库存失败')
    }
  } finally {
    inventoryLoading.value = false
  }
}

const resetInventorySearch = () => {
  inventoryParams.partCode = ''
  inventoryParams.locationCode = ''
  inventoryPage.value = 1
  loadInventoryData()
}

const loadLocations = async () => {
  const res = await getLocationList()
  if (res.code === 200) {
    locations.value = res.data || []
  }
}

const loadLifecycle = async () => {
  const kanbanNo = lifecycleKanbanNo.value.trim()
  if (!kanbanNo) {
    ElMessage.warning('请输入看板号')
    return
  }

  lifecycleLoading.value = true
  try {
    const res = await getKanbanLifecycle(kanbanNo)
    if (res.code === 200) {
      lifecycleData.value = {
        kanban: res.data?.kanban || {},
        currentInventory: res.data?.currentInventory || [],
        traces: res.data?.traces || []
      }
    } else {
      lifecycleData.value = null
      ElMessage.error(res.message || '看板生命周期查询失败')
    }
  } finally {
    lifecycleLoading.value = false
  }
}

const queryLifecycle = (kanbanNo) => {
  lifecycleKanbanNo.value = kanbanNo
  activeTab.value = 'lifecycle'
  loadLifecycle()
}

const handleTabChange = () => {
  if (traceTabs.includes(activeTab.value)) {
    tracePage.value = 1
    loadTraceData()
    return
  }

  if (activeTab.value === 'inventory') {
    loadInventoryData()
  }
}

onMounted(() => {
  loadLocations()
  loadTraceData()
  loadInventoryData()
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

.summary-row {
  margin-bottom: 16px;
}

.lifecycle-summary {
  margin-bottom: 20px;
}

.lifecycle-timeline {
  margin-top: 12px;
}

.timeline-item {
  padding: 4px 0;
}

.timeline-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.timeline-line {
  color: #606266;
  line-height: 1.7;
}

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

  .search-form :deep(.el-input),
  .search-form :deep(.el-select),
  .search-form :deep(.el-date-editor) {
    width: 100% !important;
  }
}
</style>
