<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>实时库存总览</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="8" :md="6">
        <el-statistic title="总库存数量" :value="inventorySummary.total_quantity || 0" />
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-statistic title="零件种类" :value="inventorySummary.part_types || 0">
          <template #suffix>
            <span style="font-size:14px;color:#909399">种</span>
          </template>
        </el-statistic>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-statistic title="占用仓库数" :value="inventorySummary.warehouse_count || 0">
          <template #suffix>
            <span style="font-size:14px;color:#909399">个</span>
          </template>
        </el-statistic>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6">
        <el-statistic title="看板总数" :value="inventorySummary.kanban_count || 0">
          <template #suffix>
            <span style="font-size:14px;color:#909399">个</span>
          </template>
        </el-statistic>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" class="search-form">
        <el-form-item label="零件号">
          <el-input
            v-model="filter.partCode"
            placeholder="请输入零件号"
            clearable
            style="width: 160px"
            @clear="handleFilterChange"
            @keyup.enter="handleFilterChange"
          />
        </el-form-item>
        <el-form-item label="零件名称">
          <el-input
            v-model="filter.partName"
            placeholder="请输入零件名称"
            clearable
            style="width: 160px"
            @clear="handleFilterChange"
            @keyup.enter="handleFilterChange"
          />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select
            v-model="filter.warehouseCode"
            placeholder="全部仓库"
            clearable
            style="width: 150px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="wh in warehouses"
              :key="wh.warehouse_code"
              :label="wh.warehouse_name"
              :value="wh.warehouse_code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select
            v-model="filter.supplierCode"
            placeholder="全部供应商"
            clearable
            style="width: 150px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="s in suppliers"
              :key="s.supplierCode"
              :label="s.supplierName"
              :value="s.supplierCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- PC端表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        class="desktop-table"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="part_code" label="零件号" width="150" sortable="custom" />
        <el-table-column prop="part_name" label="零件名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="supplier_name" label="供应商" width="180" show-overflow-tooltip />
        <el-table-column prop="quantity" label="当前库存" width="120" sortable="custom" align="center">
          <template #default="{ row }">
            <el-tag :type="getStockLevel(row.quantity)" size="small" effect="dark">
              {{ row.quantity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouse_name" label="仓库" width="130" />
        <el-table-column prop="kanban_no" label="看板号" width="210" show-overflow-tooltip />
        <el-table-column prop="created_at" label="入库时间" width="170" sortable="custom" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewTrace(row)">
              <el-icon><View /></el-icon>
              追溯
            </el-button>
            <el-button link type="danger" size="small" @click="handleSeal(row)">
              <el-icon><Lock /></el-icon>
              封存
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 移动端卡片列表 -->
      <div class="mobile-cards" v-loading="loading">
        <el-card
          v-for="row in tableData"
          :key="row.kanban_no"
          shadow="hover"
          class="inventory-card"
        >
          <div class="card-header">
            <div class="part-info">
              <span class="part-code">{{ row.part_code }}</span>
              <span class="part-name">{{ row.part_name }}</span>
            </div>
            <el-tag :type="getStockLevel(row.quantity)" size="small" effect="dark">
              {{ row.quantity }}
            </el-tag>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="label">供应商：</span>
              <span>{{ row.supplier_name || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">仓库：</span>
              <span>{{ row.warehouse_name || row.warehouse_code || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">看板号：</span>
              <span class="code">{{ row.kanban_no }}</span>
            </div>
            <div class="info-row">
              <span class="label">入库时间：</span>
              <span>{{ row.created_at }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" type="primary" @click="viewTrace(row)">
              <el-icon><View /></el-icon>
              查看追溯
            </el-button>
            <el-button size="small" type="danger" @click="handleSeal(row)">
              <el-icon><Lock /></el-icon>
              封存
            </el-button>
          </div>
        </el-card>

        <el-empty v-if="tableData.length === 0 && !loading" description="暂无库存数据" />
      </div>

      <!-- 分页 -->
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

    <!-- 封存对话框 -->
    <el-dialog v-model="sealDialogVisible" title="封存看板" width="500px">
      <el-alert
        title="确认要封存该看板吗？"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />
      
      <el-form :model="sealForm" label-width="100px">
        <el-form-item label="看板号">
          <el-input v-model="sealForm.kanbanNo" disabled />
        </el-form-item>
        <el-form-item label="零件号">
          <el-input v-model="sealForm.partCode" disabled />
        </el-form-item>
        <el-form-item label="零件名称">
          <el-input v-model="sealForm.partName" disabled />
        </el-form-item>
        <el-form-item label="数量">
          <el-input v-model="sealForm.quantity" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="sealForm.remark" type="textarea" :rows="3" placeholder="请输入封存原因（可选）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="sealDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmSeal">确认封存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, View, Lock } from '@element-plus/icons-vue'
import { getCurrentInventory, getWarehouseList, getSupplierList } from '../../api/wms'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const warehouses = ref([])
const suppliers = ref([])

// 排序状态
const sortField = ref('')
const sortOrder = ref('')

const filter = reactive({
  partCode: '',
  partName: '',
  warehouseCode: '',
  supplierCode: ''
})

const inventorySummary = reactive({
  total_quantity: 0,
  part_types: 0,
  warehouse_count: 0,
  kanban_count: 0
})

// 库存水平标签
const getStockLevel = (quantity) => {
  const qty = Number(quantity) || 0
  if (qty === 0) return 'danger'
  if (qty < 10) return 'warning'
  if (qty < 50) return 'info'
  return 'success'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      partCode: filter.partCode || undefined,
      warehouseCode: filter.warehouseCode || undefined
    }

    const res = await getCurrentInventory(params)
    console.log('📊 API Response:', res)
    
    if (res.code === 200) {
      let data = res.data?.list || []

      // 前端筛选供应商
      if (filter.supplierCode) {
        data = data.filter(item => item.supplier_code === filter.supplierCode)
      }

      // 前端筛选零件名称
      if (filter.partName) {
        const keyword = filter.partName.toLowerCase()
        data = data.filter(item =>
          item.part_name?.toLowerCase().includes(keyword)
        )
      }

      tableData.value = data
      total.value = res.data?.total || data.length
      
      // ============================================
      // FIX: 使用后端返回的全部数据统计（而不是当前页数据）
      // ============================================
      const summary = res.data?.summary || {}
      inventorySummary.total_quantity = summary.total_quantity || 0
      inventorySummary.kanban_count = summary.total_count || 0
      inventorySummary.part_types = summary.part_types || 0
      inventorySummary.warehouse_count = summary.warehouse_count || 0  // ✅ 修复：使用warehouse_count而不是location_count
      
      console.log('📊 Summary from backend (all data):', summary)
    } else {
      ElMessage.error(res.message || '加载库存数据失败')
    }
  } catch (error) {
    console.error('加载库存失败:', error)
    ElMessage.error('加载库存数据失败')
  } finally {
    loading.value = false
  }
}

// 筛选变化处理（带防抖）
const handleFilterChange = () => {
  clearTimeout(window._filterTimer)
  window._filterTimer = setTimeout(() => {
    page.value = 1
    loadData()
  }, 300)
}

// 重置筛选
const resetFilter = () => {
  filter.partCode = ''
  filter.partName = ''
  filter.warehouseCode = ''
  filter.supplierCode = ''
  sortField.value = ''
  sortOrder.value = ''
  page.value = 1
  loadData()
}

// 排序
const handleSortChange = ({ prop, order }) => {
  sortField.value = prop
  sortOrder.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  loadData()
}

// 查看追溯 - 跳转到库存追溯生命周期页面
const viewTrace = (row) => {
  if (!row.kanban_no) {
    ElMessage.warning('该记录没有关联看板号')
    return
  }
  
  console.log('Navigating to lifecycle with kanbanNo:', row.kanban_no)
  
  router.push({
    path: '/inventory-trace/lifecycle',
    query: { kanbanNo: row.kanban_no }
  })
}

// ============================================
// 封存功能
// ============================================
const sealDialogVisible = ref(false)
const sealForm = reactive({
  kanbanNo: '',
  partCode: '',
  partName: '',
  quantity: 0,
  remark: ''
})

const handleSeal = (row) => {
  sealForm.kanbanNo = row.kanban_no
  sealForm.partCode = row.part_code
  sealForm.partName = row.part_name
  sealForm.quantity = row.quantity
  sealForm.remark = ''
  sealDialogVisible.value = true
}

const confirmSeal = async () => {
  try {
    const { sealKanban } = await import('../../api/wms')
    const res = await sealKanban({
      kanbanNo: sealForm.kanbanNo,
      remark: sealForm.remark
    })
    
    if (res.code === 200) {
      ElMessage.success('封存成功')
      sealDialogVisible.value = false
      loadData() // 刷新列表
    } else {
      ElMessage.error(res.message || '封存失败')
    }
  } catch (error) {
    console.error('封存失败:', error)
    ElMessage.error('封存失败: ' + error.message)
  }
}

// 加载库位和供应商下拉选项
const loadOptions = async () => {
  try {
    const [whRes, supRes] = await Promise.all([
      getWarehouseList(),
      getSupplierList()
    ])
    if (whRes.code === 200) warehouses.value = whRes.data || []
    if (supRes.code === 200) suppliers.value = supRes.data || []
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

onMounted(() => {
  loadOptions()
  loadData()
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
  font-size: 24px;
  color: #303133;
}

/* 统计卡片 */
.summary-row {
  margin-bottom: 20px;
}

.summary-row .el-col {
  margin-bottom: 12px;
}

.summary-row .el-statistic {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.summary-row .el-statistic:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.summary-row .el-statistic :deep(.el-statistic__number) {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

/* 筛选卡片 */
.filter-card {
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 0;
}

/* 表格卡片 */
.table-card {
  margin-bottom: 20px;
}

/* 库存标签 */
.el-tag.is-effect-dark[type="danger"] {
  background: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
}

.el-tag.is-effect-dark[type="warning"] {
  background: #e6a23c;
  border-color: #e6a23c;
  color: #fff;
}

.el-tag.is-effect-dark[type="info"] {
  background: #909399;
  border-color: #909399;
  color: #fff;
}

.el-tag.is-effect-dark[type="success"] {
  background: #67c23a;
  border-color: #67c23a;
  color: #fff;
}

/* 移动端卡片样式 */
.mobile-cards {
  display: none;
}

.inventory-card {
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

.part-info {
  display: flex;
  flex-direction: column;
}

.part-code {
  font-size: 15px;
  font-weight: 600;
  color: #409eff;
}

.part-name {
  font-size: 13px;
  color: #606266;
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

/* 移动端显示卡片，隐藏表格 */
@media (max-width: 768px) {
  .wms-page {
    padding: 12px;
  }

  .page-header h2 {
    font-size: 20px;
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
  .search-form :deep(.el-select) {
    width: 100% !important;
  }

  .summary-row .el-col {
    width: 50%;
  }

  .summary-row .el-statistic {
    padding: 12px 16px;
  }

  .summary-row .el-statistic :deep(.el-statistic__number) {
    font-size: 22px;
  }

  .summary-row .el-statistic :deep(.el-statistic__title) {
    font-size: 13px;
  }

  .filter-card :deep(.el-card__body) {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .wms-page {
    padding: 8px;
  }

  .summary-row .el-col {
    width: 50%;
  }

  .summary-row .el-statistic {
    padding: 10px 12px;
  }

  .summary-row .el-statistic :deep(.el-statistic__number) {
    font-size: 18px;
  }

  .part-code {
    font-size: 14px;
  }

  .info-row {
    font-size: 13px;
  }

  .info-row .label {
    min-width: 60px;
  }

  .filter-card :deep(.el-card__body) {
    padding: 12px;
  }
}
</style>