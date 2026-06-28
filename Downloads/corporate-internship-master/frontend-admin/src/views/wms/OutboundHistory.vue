<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>出库历史</h2>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="零件号">
          <el-input v-model="filterPartCode" placeholder="请输入零件号" clearable style="width: 180px" />
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
        <el-table-column prop="traceNo" label="追溯号" width="200" />
        <el-table-column prop="kanbanNo" label="看板号" width="200" />
        <el-table-column prop="orderNo" label="出库单号" width="180" />
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" width="200" />
        <el-table-column prop="supplierName" label="供应商" width="150" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="actionTime" label="出库时间" width="180" />
      </el-table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-cards" v-loading="loading">
        <el-card v-for="row in tableData" :key="row.traceNo" shadow="hover" class="trace-card">
          <div class="card-header">
            <div class="trace-no">{{ row.traceNo }}</div>
            <el-tag type="danger" size="small">出库</el-tag>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">出库单号：</span>
              <span>{{ row.orderNo }}</span>
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
              <span class="label">供应商：</span>
              <span>{{ row.supplierName }}</span>
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
              <span class="label">出库时间：</span>
              <span>{{ row.actionTime }}</span>
            </div>
          </div>
        </el-card>
        <el-empty v-if="tableData.length === 0 && !loading" description="暂无出库记录" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTraceList } from '../../api/wms'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filterPartCode = ref('')
const dateRange = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      partCode: filterPartCode.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
    const res = await getTraceList(params)
    if (res.code === 200) {
      // 只筛选出库记录
      const allTraces = res.data?.list || []
      tableData.value = allTraces.filter(t => t.actionType === 'OUTBOUND')
      total.value = tableData.value.length
    }
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  filterPartCode.value = ''
  dateRange.value = []
  page.value = 1
  loadData()
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

.trace-card {
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

.trace-no {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
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

.info-row .highlight {
  color: #f56c6c;
  font-weight: 600;
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
  .search-form :deep(.el-date-picker),
  .search-form :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
