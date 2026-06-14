<template>
  <div class="wms-page trace-page">
    <div class="page-header">
      <h2>库存追溯</h2>
    </div>

    <el-card shadow="never">
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="看板号">
          <el-input
            v-model="queryForm.kanbanNo"
            placeholder="请输入看板号"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="零件编码">
          <el-input
            v-model="queryForm.partCode"
            placeholder="请输入零件编码"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="queryForm.actionType"
            placeholder="请选择"
            clearable
            style="width: 150px"
          >
            <el-option label="全部" value="" />
            <el-option label="入库" value="INBOUND" />
            <el-option label="出库" value="OUTBOUND" />
            <el-option label="移库" value="MOVE" />
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
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 统计信息 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :xs="12" :sm="6">
            <div class="stat-item">
              <div class="stat-icon inbound">
                <el-icon><Bottom /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalInbound }}</div>
                <div class="stat-label">总入库数量</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-item">
              <div class="stat-icon outbound">
                <el-icon><Top /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalOutbound }}</div>
                <div class="stat-label">总出库数量</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-item">
              <div class="stat-icon move">
                <el-icon><Right /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ stats.totalMove }}</div>
                <div class="stat-label">总移库次数</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="stat-item">
              <div class="stat-icon trace">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ pagination.total }}</div>
                <div class="stat-label">追溯记录数</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        border
        stripe
        v-loading="loading"
        @row-click="handleRowClick"
        class="trace-table"
      >
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="trace_no" label="追溯号" width="200" />
        <el-table-column prop="kanban_no" label="看板号" width="180">
          <template #default="{ row }">
            <el-link type="primary" @click.stop="handleViewKanbanTrace(row.kanban_no)">
              {{ row.kanban_no }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="action_type_text" label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getActionTypeTag(row.action_type)">
              {{ row.action_type_text || getActionText(row.action_type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="part_code" label="零件编码" width="150" />
        <el-table-column prop="part_name" label="零件名称" width="200" />
        <el-table-column prop="quantity" label="数量" width="100" align="right">
          <template #default="{ row }">
            <span :class="getQuantityClass(row.action_type)">
              {{ row.action_type === 'OUTBOUND' ? '-' : '+' }}{{ row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="location_code" label="库位" width="120" align="center" />
        <el-table-column prop="operator" label="操作人" width="100" align="center" />
        <el-table-column prop="action_time" label="操作时间" width="180" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 看板追溯详情对话框 -->
    <el-dialog
      v-model="kanbanDialogVisible"
      title="看板流转记录"
      width="900px"
      :destroy-on-close="true"
    >
      <div v-loading="kanbanLoading">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="看板号">
            <strong>{{ currentKanban.kanban_no }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="零件编码">
            {{ currentKanban.part_code }}
          </el-descriptions-item>
          <el-descriptions-item label="零件名称">
            {{ currentKanban.part_name }}
          </el-descriptions-item>
          <el-descriptions-item label="当前库位">
            {{ currentKanban.location_code }}
          </el-descriptions-item>
        </el-descriptions>

        <h4>流转历史</h4>
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in kanbanTraces"
            :key="index"
            :timestamp="item.action_time"
            :type="getActionTypeTag(item.action_type)"
            placement="top"
          >
            <el-card shadow="never">
              <div class="trace-item">
                <div class="trace-header">
                  <el-tag :type="getActionTypeTag(item.action_type)">
                    {{ item.action_type_text || getActionText(item.action_type) }}
                  </el-tag>
                  <span class="trace-quantity">
                    {{ item.action_type === 'OUTBOUND' ? '-' : '+' }}{{ item.quantity }}
                  </span>
                </div>
                <div class="trace-detail">
                  <span v-if="item.location_code">库位: {{ item.location_code }}</span>
                  <span v-if="item.operator">操作人: {{ item.operator }}</span>
                  <span v-if="item.remark">{{ item.remark }}</span>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-if="kanbanTraces.length === 0" description="暂无流转记录" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  Refresh,
  Bottom,
  Top,
  Right,
  Document
} from '@element-plus/icons-vue'
import { getTraceList, getTraceByKanban } from '../../api/wms'

const loading = ref(false)
const tableData = ref([])
const dateRange = ref([])
const kanbanDialogVisible = ref(false)
const kanbanLoading = ref(false)
const currentKanban = ref({})
const kanbanTraces = ref([])

const queryForm = reactive({
  kanbanNo: '',
  partCode: '',
  actionType: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const stats = reactive({
  totalInbound: 0,
  totalOutbound: 0,
  totalMove: 0
})

const getActionTypeTag = (type) => {
  const types = {
    INBOUND: 'success',
    OUTBOUND: 'danger',
    MOVE: 'warning'
  }
  return types[type] || 'info'
}

const getActionText = (type) => {
  const texts = {
    INBOUND: '入库',
    OUTBOUND: '出库',
    MOVE: '移库'
  }
  return texts[type] || type
}

const getQuantityClass = (type) => {
  return type === 'OUTBOUND' ? 'quantity-out' : 'quantity-in'
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(queryForm, {
    kanbanNo: '',
    partCode: '',
    actionType: ''
  })
  dateRange.value = []
  handleSearch()
}

const loadData = async () => {
  loading.value = true

  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    if (queryForm.kanbanNo) {
      params.kanbanNo = queryForm.kanbanNo
    }
    if (queryForm.partCode) {
      params.partCode = queryForm.partCode
    }
    if (queryForm.actionType) {
      params.actionType = queryForm.actionType
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await getTraceList(params)

    if (res.code === 200) {
      tableData.value = res.data?.list || []
      pagination.total = res.data?.total || 0

      // 计算统计数据
      calculateStats()
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const calculateStats = () => {
  stats.totalInbound = 0
  stats.totalOutbound = 0
  stats.totalMove = 0

  tableData.value.forEach(item => {
    if (item.action_type === 'INBOUND') {
      stats.totalInbound += item.quantity || 0
    } else if (item.action_type === 'OUTBOUND') {
      stats.totalOutbound += item.quantity || 0
    } else if (item.action_type === 'MOVE') {
      stats.totalMove += 1
    }
  })
}

const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

const handleRowClick = (row) => {
  // 可以在这里添加行点击事件
}

const handleViewKanbanTrace = async (kanbanNo) => {
  currentKanban.value = {
    kanban_no: kanbanNo
  }
  kanbanDialogVisible.value = true
  kanbanLoading.value = true

  try {
    const res = await getTraceByKanban(kanbanNo)
    if (res.code === 200) {
      kanbanTraces.value = res.data || []

      // 更新看板基本信息
      if (kanbanTraces.value.length > 0) {
        const first = kanbanTraces.value[0]
        currentKanban.value.part_code = first.part_code
        currentKanban.value.part_name = first.part_name
        currentKanban.value.location_code = first.location_code
      }
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败')
  } finally {
    kanbanLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.trace-page {
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
}

.stats-section {
  margin-bottom: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  border-radius: 8px;
  margin-bottom: 10px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.stat-icon.inbound {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
}

.stat-icon.outbound {
  background: linear-gradient(135deg, #f56c6c 0%, #fab6b6 100%);
}

.stat-icon.move {
  background: linear-gradient(135deg, #e6a23c 0%, #f5c56b 100%);
}

.stat-icon.trace {
  background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.trace-table {
  margin-bottom: 20px;
}

.quantity-in {
  color: #67c23a;
  font-weight: bold;
}

.quantity-out {
  color: #f56c6c;
  font-weight: bold;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.trace-item {
  padding: 10px 0;
}

.trace-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.trace-quantity {
  font-size: 20px;
  font-weight: bold;
}

.trace-detail {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #606266;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .search-form :deep(.el-form-item) {
    display: block;
    margin-bottom: 12px;
  }

  .stats-section {
    padding: 10px;
  }

  .stat-item {
    padding: 12px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .stat-value {
    font-size: 20px;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}
</style>
