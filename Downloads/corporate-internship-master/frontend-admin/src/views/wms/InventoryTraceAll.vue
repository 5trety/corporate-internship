<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>全部流水</h2>
    </div>

    <section class="trace-section">
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

      <el-table
        :data="traceData"
        v-loading="traceLoading"
        stripe
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
            <el-tag :type="row.actionType === 'INBOUND' ? 'success' : 'warning'" size="small">
              {{ row.actionType === 'INBOUND' ? '入库' : '出库' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="actionTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewLifecycle(row)">
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
      />
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { getTraceList } from '../../api/wms'

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

const loadTraceData = async () => {
  traceLoading.value = true
  try {
    const res = await getTraceList({
      page: tracePage.value,
      pageSize: tracePageSize.value,
      partCode: traceParams.partCode,
      kanbanNo: traceParams.kanbanNo,
      startDate: traceParams.dateRange?.[0],
      endDate: traceParams.dateRange?.[1]
    })
    if (res.code === 200) {
      traceData.value = res.data?.list || []
      traceTotal.value = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '加载流水数据失败')
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

const viewLifecycle = (row) => {
  if (!row.kanbanNo) {
    ElMessage.warning('该记录没有关联看板')
    return
  }
  router.push({
    path: '/inventory-trace/lifecycle',
    query: { kanbanNo: row.kanbanNo }
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
</style>