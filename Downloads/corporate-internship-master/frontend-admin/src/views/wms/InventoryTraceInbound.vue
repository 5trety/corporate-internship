<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>入库历史</h2>
      <el-tag type="success" size="large">仅显示入库记录</el-tag>
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

      <!-- PC端表格 - 只显示入库记录 (INBOUND) -->
      <el-table
        :data="traceData"
        v-loading="traceLoading"
        stripe
        class="desktop-table"
        @row-click="viewLifecycle"
        style="cursor: pointer"
      >
        <el-table-column prop="traceNo" label="追溯号" width="190" show-overflow-tooltip />
        <el-table-column prop="kanbanNo" label="看板号" width="190" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="入库单号" width="160" show-overflow-tooltip />
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="quantity" label="数量" width="90" align="center" />
        <el-table-column prop="locationCode" label="库位" width="120" />
        <el-table-column prop="actionType" label="类型" width="100">
          <template #default>
            <el-tag type="success" size="small">
              <el-icon><Upload /></el-icon>
              入库
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="actionTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="viewLifecycle(row)">
              <el-icon><View /></el-icon>
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
          @click="viewLifecycle(row)"
        >
          <div class="card-header">
            <div class="trace-info">
              <span class="trace-no">{{ row.traceNo }}</span>
              <span class="order-no">{{ row.orderNo }}</span>
            </div>
            <el-tag type="success" size="small">
              <el-icon><Upload /></el-icon>
              入库
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
              <span class="highlight-success">{{ row.quantity }}</span>
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
            <el-button size="small" type="primary" @click.stop="viewLifecycle(row)">
              <el-icon><View /></el-icon>
              查看生命周期
            </el-button>
          </div>
        </el-card>
        <el-empty v-if="traceData.length === 0 && !traceLoading" description="暂无入库记录" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, View, Upload } from '@element-plus/icons-vue'
import { getTraceList } from '../../api/wms'

const router = useRouter()

// ============================================
// State
// ============================================
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

// ============================================
// Load inbound trace data
// ============================================
const loadTraceData = async () => {
  traceLoading.value = true
  try {
    // 只查询入库记录 (INBOUND)
    const res = await getTraceList({
      page: tracePage.value,
      pageSize: tracePageSize.value,
      actionType: 'INBOUND',  // ✅ 正确：入库历史只显示 INBOUND 记录
      partCode: traceParams.partCode,
      kanbanNo: traceParams.kanbanNo,
      startDate: traceParams.dateRange?.[0],
      endDate: traceParams.dateRange?.[1]
    })
    
    if (res.code === 200) {
      traceData.value = res.data?.list || []
      traceTotal.value = res.data?.total || 0
      console.log(`📥 加载入库历史成功: ${traceData.value.length} 条记录`)
    } else {
      ElMessage.error(res.message || '加载入库历史失败')
    }
  } catch (error) {
    console.error('❌ 加载入库历史失败:', error)
    ElMessage.error('加载入库历史失败')
  } finally {
    traceLoading.value = false
  }
}

// ============================================
// Reset search filters
// ============================================
const resetTraceSearch = () => {
  traceParams.partCode = ''
  traceParams.kanbanNo = ''
  traceParams.dateRange = null
  tracePage.value = 1
  loadTraceData()
}

// ============================================
// Navigate to lifecycle page
// ============================================
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

// ============================================
// Lifecycle
// ============================================
onMounted(() => {
  loadTraceData()
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

/* 表格行悬停效果 */
.el-table__body-wrapper tbody tr:hover {
  background-color: #f5f7fa !important;
  cursor: pointer;
}

/* 移动端卡片样式 */
.mobile-cards {
  display: none;
}

.trace-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.trace-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
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

.info-row .highlight-success {
  color: #67c23a;
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

/* 响应式 - 移动端 */
@media (max-width: 768px) {
  .wms-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
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

  .info-row {
    font-size: 13px;
  }

  .info-row .label {
    min-width: 60px;
  }

  .trace-no {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .wms-page {
    padding: 8px;
  }

  .trace-section {
    padding: 8px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .info-row {
    font-size: 12px;
  }

  .info-row .label {
    min-width: 55px;
  }

  .card-body {
    margin-bottom: 8px;
  }

  .card-actions .el-button {
    font-size: 12px;
    padding: 6px 10px;
  }
}
</style>