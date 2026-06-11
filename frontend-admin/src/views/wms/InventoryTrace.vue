<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>库存追溯</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 追溯记录 - 驼峰格式 -->
      <el-tab-pane label="追溯记录" name="trace">
        <el-card shadow="never">
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
              <el-button type="primary" @click="loadTraceData">搜索</el-button>
              <el-button @click="resetTraceSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="traceData" v-loading="traceLoading" stripe>
            <el-table-column prop="traceNo" label="追溯号" width="180" />
            <el-table-column prop="kanbanNo" label="看板号" width="180" />
            <el-table-column prop="orderNo" label="入库单号" width="150" />
            <el-table-column prop="partCode" label="零件号" width="150" />
            <el-table-column prop="partName" label="零件名称" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="locationCode" label="库位" width="100" />
            <el-table-column prop="actionType" label="操作类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.actionType === 'INBOUND' ? 'success' : 'warning'" size="small">
                  {{ row.actionType === 'INBOUND' ? '入库' : (row.actionType === 'OUTBOUND' ? '出库' : row.actionType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="100" />
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
        </el-card>
      </el-tab-pane>

      <!-- 当前库存 - 下划线格式 -->
      <el-tab-pane label="当前库存" name="inventory">
        <el-card shadow="never">
          <el-row :gutter="20" class="summary-row">
            <el-col :span="8">
              <el-statistic title="总库存数量" :value="inventorySummary.total_quantity || 0" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="总看板数" :value="inventorySummary.total_count || 0" />
            </el-col>
          </el-row>

          <el-divider />

          <el-form :inline="true" class="search-form">
            <el-form-item label="零件号">
              <el-input v-model="inventoryParams.partCode" placeholder="零件号" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="库位">
              <el-select
                v-model="inventoryParams.locationCode"
                placeholder="全部"
                clearable
                style="width: 140px"
              >
                <el-option
                  v-for="loc in locations"
                  :key="loc.location_code"
                  :label="loc.location_name"
                  :value="loc.location_code"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadInventoryData">搜索</el-button>
              <el-button @click="resetInventorySearch">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="inventoryData" v-loading="inventoryLoading" stripe>
            <el-table-column prop="kanban_no" label="看板号" width="220" show-overflow-tooltip />
            <el-table-column prop="part_code" label="零件号" width="150" />
            <el-table-column prop="part_name" label="零件名称" />
            <el-table-column prop="supplier_name" label="供应商" width="150" />
            <el-table-column prop="quantity" label="数量" width="100" />
            <el-table-column prop="location_name" label="库位" width="120" />
            <el-table-column prop="created_at" label="入库时间" width="180" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="showKanbanTrace(row.kanban_no)">追溯</el-button>
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
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 看板追溯弹窗 - 使用驼峰格式 -->
    <el-dialog :title="`看板追溯 - ${currentKanbanNo}`" v-model="traceDialogVisible" width="800px">
      <el-timeline>
        <el-timeline-item
          v-for="item in kanbanTrace"
          :key="item.id"
          :timestamp="item.actionTime"
          placement="top"
          :type="item.actionType === 'INBOUND' ? 'success' : 'warning'"
        >
          <el-card>
            <h4>{{ item.actionType === 'INBOUND' ? '入库' : (item.actionType === 'OUTBOUND' ? '出库' : item.actionType) }}</h4>
            <p>零件：{{ item.partCode }} - {{ item.partName }}</p>
            <p>数量：{{ item.quantity }}</p>
            <p>库位：{{ item.locationCode }}</p>
            <p>操作人：{{ item.operator }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="kanbanTrace.length === 0" description="暂无追溯记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTraceList, getCurrentInventory, getTraceByKanban } from '../../api/wms'
import { getLocationList } from '../../api/wms'

const activeTab = ref('trace')

// 追溯记录
const traceLoading = ref(false)
const traceData = ref([])
const traceTotal = ref(0)
const tracePage = ref(1)
const tracePageSize = ref(20)
const traceParams = reactive({
  partCode: '',
  kanbanNo: '',
  dateRange: []
})

// 库存
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

// 追溯弹窗
const traceDialogVisible = ref(false)
const currentKanbanNo = ref('')
const kanbanTrace = ref([])

const loadTraceData = async () => {
  traceLoading.value = true
  try {
    const params = {
      page: tracePage.value,
      pageSize: tracePageSize.value,
      partCode: traceParams.partCode,
      kanbanNo: traceParams.kanbanNo,
      startDate: traceParams.dateRange?.[0],
      endDate: traceParams.dateRange?.[1]
    }
    const res = await getTraceList(params)
    if (res.code === 200) {
      traceData.value = res.data?.list || []
      traceTotal.value = res.data?.total || 0
    }
  } finally {
    traceLoading.value = false
  }
}

const resetTraceSearch = () => {
  traceParams.partCode = ''
  traceParams.kanbanNo = ''
  traceParams.dateRange = []
  tracePage.value = 1
  loadTraceData()
}

const loadInventoryData = async () => {
  inventoryLoading.value = true
  try {
    const params = {
      page: inventoryPage.value,
      pageSize: inventoryPageSize.value,
      partCode: inventoryParams.partCode,
      locationCode: inventoryParams.locationCode
    }
    const res = await getCurrentInventory(params)
    if (res.code === 200) {
      inventoryData.value = res.data?.list || []
      inventoryTotal.value = res.data?.total || 0
      inventorySummary.value = res.data?.summary || {}
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

const showKanbanTrace = async (kanbanNo) => {
  if (!kanbanNo) {
    ElMessage.warning('看板号不存在')
    return
  }
  currentKanbanNo.value = kanbanNo
  const res = await getTraceByKanban(kanbanNo)
  if (res.code === 200) {
    kanbanTrace.value = res.data || []
    traceDialogVisible.value = true
  } else {
    ElMessage.error(res.message || '获取追溯记录失败')
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
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
}
.search-form {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.search-form .el-form-item {
  margin-bottom: 0;
}
.summary-row {
  margin-bottom: 20px;
}
</style>