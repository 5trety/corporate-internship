<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>{{ title }}</h2>
    </div>

    <section class="trace-section">
      <!-- 搜索栏 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="零件号">
          <el-input
            v-model="traceParams.partCode"
            placeholder="零件号"
            clearable
            style="width: 160px"
            @clear="loadTraceData"
            @keyup.enter="loadTraceData"
          />
        </el-form-item>
        <el-form-item label="看板号">
          <el-input
            v-model="traceParams.kanbanNo"
            placeholder="看板号"
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

      <!-- PC端表格 -->
      <el-table
        :data="traceData"
        v-loading="traceLoading"
        stripe
        class="desktop-table"
        @row-click="openTraceDetail"
        style="cursor: pointer"
      >
        <el-table-column prop="traceNo" label="追溯号" width="190" show-overflow-tooltip />
        <el-table-column prop="kanbanNo" label="看板号" width="190" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="单据号" width="160" show-overflow-tooltip />
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="90" align="center" />
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
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openTraceDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button 
              link 
              type="success" 
              size="small" 
              @click.stop="viewLifecycle(row)"
              :disabled="!row.kanbanNo"
            >
              <el-icon><Refresh /></el-icon>
              生命周期
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-cards" v-loading="traceLoading">
        <el-card
          v-for="row in traceData"
          :key="row.traceNo"
          shadow="hover"
          class="trace-card"
          @click="openTraceDetail(row)"
        >
          <div class="card-header">
            <div class="trace-info">
              <span class="trace-no">{{ row.traceNo }}</span>
              <span class="order-no">{{ row.orderNo }}</span>
            </div>
            <el-tag :type="getActionTagType(row.actionType)" size="small">
              {{ getActionLabel(row.actionType) }}
            </el-tag>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">看板号：</span>
              <span class="code">{{ row.kanbanNo }}</span>
            </div>
            <div class="info-row">
              <span class="label">零件号：</span>
              <span>{{ row.partCode }}</span>
            </div>
            <div class="info-row">
              <span class="label">零件名称：</span>
              <span>{{ row.partName }}</span>
            </div>
            <div class="info-row">
              <span class="label">数量：</span>
              <span class="highlight">{{ row.quantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">库位：</span>
              <span>{{ row.locationCode }}</span>
            </div>
            <div class="info-row">
              <span class="label">操作人：</span>
              <span>{{ row.operator }}</span>
            </div>
            <div class="info-row">
              <span class="label">操作时间：</span>
              <span>{{ row.actionTime }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" type="primary" @click.stop="openTraceDetail(row)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button size="small" type="success" @click.stop="viewLifecycle(row)">
              <el-icon><Refresh /></el-icon>
              生命周期
            </el-button>
          </div>
        </el-card>
        <el-empty v-if="traceData.length === 0 && !traceLoading" description="暂无追溯记录" />
      </div>

      <!-- 分页 -->
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

    <!-- 追溯详情抽屉 -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="`追溯详情 - ${currentDetail?.traceNo || ''}`"
      size="600px"
      destroy-on-close
      class="trace-drawer"
    >
      <template v-if="currentDetail">
        <div class="drawer-header">
          <div class="drawer-title">
            <span class="trace-id">{{ currentDetail.traceNo }}</span>
            <el-tag :type="getActionTagType(currentDetail.actionType)" size="small">
              {{ getActionLabel(currentDetail.actionType) }}
            </el-tag>
          </div>
          <div class="drawer-subtitle">{{ currentDetail.actionTime }}</div>
        </div>

        <el-divider />

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="看板号">
            <span class="code">{{ currentDetail.kanbanNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="单据号">{{ currentDetail.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="零件号">{{ currentDetail.partCode }}</el-descriptions-item>
          <el-descriptions-item label="零件名称">{{ currentDetail.partName }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentDetail.supplierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数量">
            <span class="highlight">{{ currentDetail.quantity }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="库位">{{ currentDetail.locationCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentDetail.operator || '-' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="kanbanDetail">
          <el-divider>关联看板信息</el-divider>
          <el-descriptions :column="2" border size="small" class="kanban-detail">
            <el-descriptions-item label="看板号">
              <span class="code">{{ kanbanDetail.kanbanNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small">{{ getKanbanStatusLabel(kanbanDetail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="零件号">{{ kanbanDetail.partCode }}</el-descriptions-item>
            <el-descriptions-item label="零件名称">{{ kanbanDetail.partName }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ kanbanDetail.quantity }}</el-descriptions-item>
            <el-descriptions-item label="箱数">{{ kanbanDetail.boxCount }}</el-descriptions-item>
            <el-descriptions-item label="库位">{{ kanbanDetail.locationName || kanbanDetail.locationCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ kanbanDetail.createdAt }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <div class="drawer-actions">
          <el-button 
            type="primary" 
            @click="goToLifecycle(currentDetail.kanbanNo)"
            :disabled="!currentDetail.kanbanNo"
          >
            <el-icon><Refresh /></el-icon>
            查看完整生命周期
          </el-button>
          <el-button @click="detailDrawerVisible = false">
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, View, Refresh, Close } from '@element-plus/icons-vue'
import { getTraceList, getKanbanList } from '../../api/wms'

const props = defineProps({
  type: {
    type: String,
    default: 'all' // 'all', 'inbound', 'outbound'
  },
  title: {
    type: String,
    default: '全部流水'
  }
})

const router = useRouter()

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

const detailDrawerVisible = ref(false)
const currentDetail = ref(null)
const kanbanDetail = ref(null)

const currentActionType = computed(() => {
  if (props.type === 'inbound') return 'INBOUND'
  if (props.type === 'outbound') return 'OUTBOUND'
  return ''
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

const openTraceDetail = async (row) => {
  currentDetail.value = row
  detailDrawerVisible.value = true

  if (row.kanbanNo) {
    try {
      const res = await getKanbanList({ kanbanNo: row.kanbanNo })
      if (res.code === 200 && res.data?.length > 0) {
        const kanban = res.data[0]
        kanbanDetail.value = {
          kanbanNo: kanban.kanbanNo || kanban.kanban_no,
          partCode: kanban.partCode || kanban.part_code,
          partName: kanban.partName || kanban.part_name,
          quantity: kanban.quantity,
          boxCount: kanban.boxCount || kanban.box_count,
          status: kanban.status,
          locationCode: kanban.locationCode || kanban.location_code,
          locationName: kanban.locationName || kanban.location_name,
          createdAt: kanban.createdAt || kanban.created_at
        }
      } else {
        kanbanDetail.value = null
      }
    } catch (error) {
      console.error('获取看板详情失败:', error)
      kanbanDetail.value = null
    }
  } else {
    kanbanDetail.value = null
  }
}

const viewLifecycle = (row) => {
  if (!row.kanbanNo) {
    ElMessage.warning('该记录没有关联看板')
    return
  }
  goToLifecycle(row.kanbanNo)
}

const goToLifecycle = (kanbanNo) => {
  if (!kanbanNo) {
    ElMessage.warning('该记录没有关联看板')
    return
  }
  detailDrawerVisible.value = false
  router.push({
    path: '/inventory-trace/lifecycle',
    query: { kanbanNo: kanbanNo }
  })
}

onMounted(() => {
  loadTraceData()
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

.el-table__body-wrapper tbody tr:hover {
  background-color: #f5f7fa !important;
  cursor: pointer;
}

/* 抽屉样式 */
.trace-drawer :deep(.el-drawer__header) {
  padding: 16px 20px;
  margin-bottom: 0;
  border-bottom: 1px solid #e4e7ed;
}

.trace-drawer :deep(.el-drawer__body) {
  padding: 20px;
}

.drawer-header {
  margin-bottom: 8px;
}

.drawer-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trace-id {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
}

.drawer-subtitle {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.detail-descriptions {
  margin-bottom: 16px;
}

.detail-descriptions .highlight {
  color: #e6a23c;
  font-weight: 600;
  font-size: 16px;
}

.detail-descriptions .code {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.kanban-detail {
  margin-bottom: 16px;
}

.drawer-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.drawer-actions .el-button {
  flex: 1;
}

/* 移动端卡片样式 */
.mobile-cards {
  display: none;
}

.trace-card {
  margin-bottom: 12px;
  cursor: pointer;
}

.trace-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.trace-info {
  display: flex;
  flex-direction: column;
}

.trace-no {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.order-no {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.card-body {
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 14px;
}

.info-row .label {
  color: #909399;
  min-width: 70px;
  flex-shrink: 0;
}

.info-row .highlight {
  color: #e6a23c;
  font-weight: 600;
}

.info-row .code {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  word-break: break-all;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.card-actions .el-button {
  flex: 1;
}

/* 响应式 */
@media (max-width: 768px) {
  .wms-page {
    padding: 12px;
  }

  .trace-section {
    padding: 12px;
  }

  .desktop-table {
    display: none;
  }

  .mobile-cards {
    display: block;
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

  .trace-drawer :deep(.el-drawer) {
    width: 100% !important;
  }

  .trace-drawer :deep(.el-drawer__body) {
    padding: 16px;
  }

  .drawer-actions {
    flex-direction: column;
  }

  .drawer-actions .el-button {
    width: 100%;
  }
}
</style>