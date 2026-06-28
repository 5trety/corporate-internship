<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑入库单' : '创建入库单' }}</h2>
      <el-button @click="$router.back()">
        <el-icon><Back /></el-icon>
        返回
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px" class="responsive-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="入库类型" prop="inboundType">
              <el-select v-model="formData.inboundType" style="width: 100%">
                <el-option label="采购入库" value="采购入库" />
                <el-option label="退货入库" value="退货入库" />
                <el-option label="生产入库" value="生产入库" />
                <el-option label="调拨入库" value="调拨入库" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="供应商" prop="supplierCode">
              <el-select 
                v-model="formData.supplierCode" 
                filterable 
                style="width: 100%" 
                @change="onSupplierChange"
                placeholder="请选择供应商"
              >
                <el-option 
                  v-for="s in suppliers" 
                  :key="s.supplierCode" 
                  :label="s.supplierName" 
                  :value="s.supplierCode" 
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="目标仓库" prop="warehouseCode">
              <el-select v-model="formData.warehouseCode" style="width: 100%" placeholder="请选择仓库">
                <el-option 
                  v-for="w in warehouses" 
                  :key="w.warehouse_code" 
                  :label="w.warehouse_name" 
                  :value="w.warehouse_code" 
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>

      <el-divider>入库明细</el-divider>

      <div class="detail-toolbar">
        <el-button type="primary" @click="openPartSelector">
          <el-icon><Plus /></el-icon>
          添加零件
        </el-button>
        <el-button type="success" @click="downloadExcel">
          <el-icon><Download /></el-icon>
          下载Excel
        </el-button>
        <el-tag v-if="formData.supplierCode" type="success" size="small">
          当前供应商：{{ getSupplierName(formData.supplierCode) }}
        </el-tag>
        <el-tag v-else type="warning" size="small">
          请先选择供应商
        </el-tag>
        <el-tag v-if="formData.details.length > 0" type="info" size="small">
          共 {{ formData.details.length }} 项
        </el-tag>
      </div>

      <el-table :data="formData.details" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="partCode" label="零件号" width="180" />
        <el-table-column prop="partName" label="零件名称" width="200" />
        <el-table-column prop="expectedQuantity" label="预期数量" width="140">
          <template #default="{ row }">
            <el-input-number 
              v-model="row.expectedQuantity" 
              :min="1" 
              @change="calcBoxes(row)" 
              style="width: 100%" 
            />
          </template>
        </el-table-column>
        <el-table-column prop="packagingCapacity" label="包装容量" width="100" />
        <el-table-column prop="expectedBoxes" label="预期箱数" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeDetail($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 汇总信息 -->
      <div v-if="formData.details.length > 0" class="summary-row">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="总数量">{{ formData.totalQuantity }}</el-descriptions-item>
          <el-descriptions-item label="总箱数">{{ formData.totalBoxes }}</el-descriptions-item>
          <el-descriptions-item label="零件种类">{{ formData.details.length }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="form-footer">
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
          保存入库单
        </el-button>
        <el-button size="large" @click="$router.back()">取消</el-button>
      </div>
    </el-card>

    <!-- 零件选择弹窗 - 批量选择 -->
    <el-dialog 
      v-model="partSelectorVisible" 
      title="选择零件" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="selector-header">
        <div class="selector-info">
          <el-tag type="success" size="large">
            供应商：{{ getSupplierName(formData.supplierCode) }}
          </el-tag>
          <span class="selected-count">已选 {{ selectedParts.length }} 个零件</span>
        </div>
        <div class="selector-search">
          <el-input
            v-model="partSearchKeyword"
            placeholder="搜索零件号或名称"
            clearable
            style="width: 250px"
            prefix-icon="Search"
          />
        </div>
      </div>

      <el-table
        ref="partTableRef"
        :data="filteredParts"
        v-loading="partLoading"
        stripe
        @selection-change="handleSelectionChange"
        max-height="400"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" min-width="180" />
        <el-table-column prop="packagingCapacity" label="包装容量" width="100" align="center" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="price" label="单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ row.price || 0 }}
          </template>
        </el-table-column>
      </el-table>

      <div class="selector-footer">
        <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选</el-checkbox>
        <span class="selected-info">已选 {{ selectedParts.length }} 个零件</span>
        <div>
          <el-button @click="partSelectorVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAddParts" :disabled="selectedParts.length === 0">
            确认添加 ({{ selectedParts.length }})
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Plus, Download } from '@element-plus/icons-vue'
import { createInboundOrder, updateInboundOrder, getInboundOrderDetail } from '../../api/wms'
import { getSupplierList, getWarehouseList, getPartList } from '../../api/wms'
import * as XLSX from 'xlsx'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const suppliers = ref([])
const warehouses = ref([])
const allParts = ref([])
const partLoading = ref(false)

// 零件选择器状态
const partSelectorVisible = ref(false)
const selectedParts = ref([])
const partSearchKeyword = ref('')
const selectAll = ref(false)
const partTableRef = ref(null)

const formData = reactive({
  inboundType: '采购入库',
  supplierCode: '',
  warehouseCode: '',
  remark: '',
  totalQuantity: 0,
  totalBoxes: 0,
  details: []
})

const rules = {
  inboundType: [{ required: true, message: '请选择入库类型' }],
  supplierCode: [{ required: true, message: '请选择供应商' }],
  warehouseCode: [{ required: true, message: '请选择目标仓库' }]
}

// 过滤后的零件列表
const filteredParts = computed(() => {
  if (!partSearchKeyword.value) {
    return allParts.value
  }
  const keyword = partSearchKeyword.value.toLowerCase()
  return allParts.value.filter(p => 
    p.partCode.toLowerCase().includes(keyword) || 
    p.partName.toLowerCase().includes(keyword)
  )
})

const getSupplierName = (code) => {
  const supplier = suppliers.value.find(s => s.supplierCode === code)
  return supplier ? supplier.supplierName : code
}

const calcTotal = () => {
  formData.totalQuantity = formData.details.reduce((sum, d) => sum + (d.expectedQuantity || 0), 0)
  formData.totalBoxes = formData.details.reduce((sum, d) => sum + (d.expectedBoxes || 0), 0)
}

const calcBoxes = (row) => {
  const capacity = row.packagingCapacity > 0 ? row.packagingCapacity : 1
  row.expectedBoxes = Math.ceil((row.expectedQuantity || 0) / capacity)
  calcTotal()
}

const removeDetail = (index) => {
  formData.details.splice(index, 1)
  calcTotal()
}

const onSupplierChange = async () => {
  selectedParts.value = []
  selectAll.value = false
  
  if (formData.supplierCode) {
    partLoading.value = true
    try {
      const res = await getPartList(formData.supplierCode)
      if (res.code === 200) {
        allParts.value = res.data || []
      }
    } finally {
      partLoading.value = false
    }
  } else {
    allParts.value = []
  }
}

// ==================== Excel下载功能 ====================

/**
 * 下载当前入库单明细为Excel文件
 */
const downloadExcel = () => {
  if (formData.details.length === 0) {
    ElMessage.warning('入库明细为空，请先添加零件')
    return
  }

  // 准备导出数据
  const exportData = formData.details.map((item, index) => ({
    '序号': index + 1,
    '零件号': item.partCode,
    '零件名称': item.partName,
    '预期数量': item.expectedQuantity || 0,
    '包装容量': item.packagingCapacity || 0,
    '预期箱数': item.expectedBoxes || 0,
    '单位': item.unit || '个'
  }))

  // 创建Excel工作簿
  const workbook = XLSX.utils.book_new()
  
  // 创建表头样式（使用worksheet的列宽和行高，Excel单元格样式需要额外处理）
  const worksheet = XLSX.utils.json_to_sheet(exportData)
  
  // 设置列宽
  worksheet['!cols'] = [
    { wch: 8 },   // 序号
    { wch: 20 },  // 零件号
    { wch: 30 },  // 零件名称
    { wch: 12 },  // 预期数量
    { wch: 12 },  // 包装容量
    { wch: 12 },  // 预期箱数
    { wch: 10 }   // 单位
  ]

  // 添加工作表
  XLSX.utils.book_append_sheet(workbook, worksheet, '入库明细')
  
  // 添加汇总信息到另一个工作表（可选）
  const summaryData = [
    { '项目': '入库单类型', '值': formData.inboundType },
    { '项目': '供应商', '值': getSupplierName(formData.supplierCode) },
    { '项目': '目标仓库', '值': getWarehouseName(formData.warehouseCode) },
    { '项目': '总数量', '值': formData.totalQuantity },
    { '项目': '总箱数', '值': formData.totalBoxes },
    { '项目': '零件种类', '值': formData.details.length },
    { '项目': '备注', '值': formData.remark || '-' }
  ]
  
  const summarySheet = XLSX.utils.json_to_sheet(summaryData)
  summarySheet['!cols'] = [
    { wch: 15 },
    { wch: 30 }
  ]
  XLSX.utils.book_append_sheet(workbook, summarySheet, '汇总信息')

  // 生成文件名
  const timestamp = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/[/:]/g, '-')
  
  const fileName = `入库单_${getSupplierName(formData.supplierCode)}_${timestamp}.xlsx`
  
  // 下载文件
  XLSX.writeFile(workbook, fileName)
  ElMessage.success(`Excel导出成功：${fileName}`)
}

// 获取仓库名称
const getWarehouseName = (code) => {
  const warehouse = warehouses.value.find(w => w.warehouse_code === code)
  return warehouse ? warehouse.warehouse_name : code
}

// 打开零件选择器
const openPartSelector = () => {
  if (!formData.supplierCode) {
    ElMessage.warning('请先选择供应商')
    return
  }
  
  if (allParts.value.length === 0) {
    onSupplierChange()
  }
  
  selectedParts.value = []
  selectAll.value = false
  partSearchKeyword.value = ''
  
  partSelectorVisible.value = true
  
  nextTick(() => {
    if (partTableRef.value) {
      partTableRef.value.clearSelection()
    }
  })
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedParts.value = selection
  if (filteredParts.value.length > 0) {
    selectAll.value = selection.length === filteredParts.value.length
  }
}

// 全选/取消全选
const toggleSelectAll = (val) => {
  if (partTableRef.value) {
    if (val) {
      filteredParts.value.forEach(row => {
        partTableRef.value.toggleRowSelection(row, true)
      })
    } else {
      partTableRef.value.clearSelection()
    }
  }
}

// 确认添加零件
const confirmAddParts = () => {
  if (selectedParts.value.length === 0) {
    ElMessage.warning('请至少选择一个零件')
    return
  }

  const existingCodes = new Set(formData.details.map(d => d.partCode))
  const duplicateParts = selectedParts.value.filter(p => existingCodes.has(p.partCode))
  
  if (duplicateParts.length > 0) {
    ElMessage.warning(`零件 ${duplicateParts.map(p => p.partCode).join(', ')} 已存在，请勿重复添加`)
    return
  }

  selectedParts.value.forEach(part => {
    const capacity = part.packagingCapacity > 0 ? part.packagingCapacity : 1
    formData.details.push({
      partCode: part.partCode,
      partName: part.partName,
      expectedQuantity: capacity,
      packagingCapacity: capacity,
      expectedBoxes: 1,
      unit: part.unit || '个'
    })
  })

  calcTotal()
  partSelectorVisible.value = false
  ElMessage.success(`成功添加 ${selectedParts.value.length} 个零件`)
}

const loadEditData = async () => {
  const orderNo = route.params.orderNo
  const res = await getInboundOrderDetail(orderNo)
  if (res.code === 200) {
    const order = res.data?.order || {}
    Object.assign(formData, {
      inboundType: order.inboundType,
      supplierCode: order.supplierCode,
      warehouseCode: order.warehouseCode,
      remark: order.remark,
      details: (res.data?.details || []).map(d => ({
        partCode: d.partCode,
        partName: d.partName,
        expectedQuantity: d.expectedQuantity,
        packagingCapacity: d.packagingCapacity,
        expectedBoxes: d.expectedBoxes,
        unit: d.unit
      }))
    })
    if (formData.supplierCode) {
      await onSupplierChange()
    }
    calcTotal()
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()

  if (formData.details.length === 0) {
    ElMessage.warning('请添加入库明细')
    return
  }

  for (const detail of formData.details) {
    if (!detail.expectedQuantity || detail.expectedQuantity <= 0) {
      ElMessage.warning('请输入预期数量')
      return
    }
  }

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateInboundOrder(route.params.orderNo, formData)
    } else {
      res = await createInboundOrder(formData)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
      router.push('/wms/inbound-order/list')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const [supplierRes, warehouseRes] = await Promise.all([
    getSupplierList(),
    getWarehouseList()
  ])
  if (supplierRes.code === 200) suppliers.value = supplierRes.data || []
  if (warehouseRes.code === 200) warehouses.value = warehouseRes.data || []

  if (route.params.orderNo) {
    isEdit.value = true
    await loadEditData()
  }
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
.detail-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.summary-row {
  margin-top: 16px;
}
.form-footer {
  margin-top: 30px;
  text-align: center;
}

/* 选择器样式 */
.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.selector-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.selected-count {
  color: #606266;
  font-size: 14px;
}

.selector-search {
  display: flex;
  align-items: center;
}

.selector-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.selected-info {
  color: #409eff;
  font-weight: 500;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .page-header .el-button {
    width: 100%;
  }
  
  .form-footer {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  .form-footer .el-button {
    width: 100%;
    margin: 0;
  }

  .selector-header {
    flex-direction: column;
    align-items: stretch;
  }

  .selector-search .el-input {
    width: 100% !important;
  }

  .selector-footer {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>