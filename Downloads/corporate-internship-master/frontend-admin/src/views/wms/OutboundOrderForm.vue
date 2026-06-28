<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑出库单' : '创建出库单' }}</h2>
      <el-button @click="$router.back()">
        <el-icon><Back /></el-icon>
        返回
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px" class="responsive-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="出库类型" prop="outboundType">
              <el-select v-model="formData.outboundType" style="width: 100%">
                <el-option label="销售出库" value="销售出库" />
                <el-option label="退货出库" value="退货出库" />
                <el-option label="生产领料" value="生产领料" />
                <el-option label="调拨出库" value="调拨出库" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="客户编码" prop="customerCode">
              <el-input v-model="formData.customerCode" placeholder="请输入客户编码" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="formData.customerName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="仓库" prop="warehouseCode">
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

      <el-divider>出库明细</el-divider>

      <div class="detail-toolbar">
        <el-button type="primary" @click="openPartSelector">
          <el-icon><Plus /></el-icon>
          添加零件
        </el-button>
        <el-tag v-if="formData.warehouseCode" type="success" size="small">
          仓库：{{ getWarehouseName(formData.warehouseCode) }}
        </el-tag>
        <el-tag v-else type="warning" size="small">
          请先选择仓库
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

      <div class="form-footer">
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
          保存出库单
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
            仓库：{{ getWarehouseName(formData.warehouseCode) }}
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
        <el-table-column prop="availableStock" label="可用库存" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.availableStock > 0 ? 'success' : 'danger'" size="small">
              {{ row.availableStock || 0 }}
            </el-tag>
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
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Plus } from '@element-plus/icons-vue'
import { createOutboundOrder, updateOutboundOrder, getOutboundOrderDetail } from '../../api/wms'
import { getWarehouseList, getPartList, getWarehousePartStock } from '../../api/wms'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
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
  outboundType: '销售出库',
  customerCode: '',
  customerName: '',
  warehouseCode: '',
  remark: '',
  totalQuantity: 0,
  totalBoxes: 0,
  details: []
})

const rules = {
  outboundType: [{ required: true, message: '请选择出库类型' }],
  customerName: [{ required: true, message: '请输入客户名称' }],
  warehouseCode: [{ required: true, message: '请选择仓库' }]
}

// 过滤后的零件列表
const filteredParts = computed(() => {
  let parts = allParts.value
  if (partSearchKeyword.value) {
    const keyword = partSearchKeyword.value.toLowerCase()
    parts = parts.filter(p => 
      p.partCode.toLowerCase().includes(keyword) || 
      p.partName.toLowerCase().includes(keyword)
    )
  }
  return parts
})

const getWarehouseName = (code) => {
  const warehouse = warehouses.value.find(w => w.warehouse_code === code)
  return warehouse ? warehouse.warehouse_name : code
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

const onWarehouseChange = async () => {
  selectedParts.value = []
  selectAll.value = false
  
  // 清空已选零件（切换仓库时）
  if (formData.details && formData.details.length > 0) {
    formData.details = []
    calcTotal()
  }
  
  if (formData.warehouseCode) {
    partLoading.value = true
    try {
      // 获取指定仓库的零件库存
      const stockRes = await getWarehousePartStock(formData.warehouseCode)
      if (stockRes.code === 200) {
        const stockList = stockRes.data || []
        
        // 转换为零件列表格式
        allParts.value = stockList.map(stock => ({
          partCode: stock.part_code,
          partName: stock.part_name,
          packagingCapacity: stock.packaging_capacity,
          availableStock: stock.available_stock || 0
        }))
        
        if (allParts.value.length === 0) {
          ElMessage.warning('该仓库暂无库存零件')
        }
      } else {
        allParts.value = []
        ElMessage.error(stockRes.message || '获取仓库零件库存失败')
      }
    } catch (error) {
      console.error('获取仓库零件库存失败:', error)
      allParts.value = []
      ElMessage.error('获取仓库零件库存失败')
    } finally {
      partLoading.value = false
    }
  } else {
    allParts.value = []
  }
}

// 监听仓库变化，自动刷新零件列表
watch(() => formData.warehouseCode, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    onWarehouseChange()
  }
})

// 打开零件选择器
const openPartSelector = async () => {
  if (!formData.warehouseCode) {
    ElMessage.warning('请先选择仓库')
    return
  }
  
  // 每次都重新加载零件库存（支持切换仓库后实时更新）
  await onWarehouseChange()
  
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

  // 检查库存是否充足
  const insufficientStock = selectedParts.value.filter(p => p.availableStock <= 0)
  if (insufficientStock.length > 0) {
    ElMessage.warning(`零件 ${insufficientStock.map(p => p.partCode).join(', ')} 库存不足`)
    return
  }

  selectedParts.value.forEach(part => {
    const capacity = part.packagingCapacity > 0 ? part.packagingCapacity : 1
    formData.details.push({
      partCode: part.partCode,
      partName: part.partName,
      expectedQuantity: Math.min(capacity, part.availableStock || capacity),
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
  const res = await getOutboundOrderDetail(orderNo)
  if (res.code === 200) {
    const order = res.data?.order || {}
    Object.assign(formData, {
      outboundType: order.outboundType,
      customerCode: order.customerCode,
      customerName: order.customerName,
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
    if (formData.warehouseCode) {
      await onWarehouseChange()
    }
    calcTotal()
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()

  if (formData.details.length === 0) {
    ElMessage.warning('请添加出库明细')
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
      res = await updateOutboundOrder(route.params.orderNo, formData)
    } else {
      res = await createOutboundOrder(formData)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
      router.push('/wms-outbound/outbound-order/list')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const warehouseRes = await getWarehouseList()
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