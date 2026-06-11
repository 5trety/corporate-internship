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
              <el-select v-model="formData.supplierCode" filterable style="width: 100%" @change="onSupplierChange">
                <el-option v-for="s in suppliers" :key="s.supplierCode" :label="s.supplierName" :value="s.supplierCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8">
            <el-form-item label="目标仓库" prop="warehouseCode">
              <el-select v-model="formData.warehouseCode" style="width: 100%">
                <el-option v-for="w in warehouses" :key="w.warehouse_code" :label="w.warehouse_name" :value="w.warehouse_code" />
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
        <el-button type="primary" size="small" @click="addDetail">
          <el-icon><Plus /></el-icon>
          添加零件
        </el-button>
      </div>

      <el-table :data="formData.details" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="partCode" label="零件号" width="180">
          <template #default="{ row, $index }">
            <el-select v-model="row.partCode" filterable remote reserve-keyword :remote-method="searchParts" @change="(val) => onPartChange(val, $index)">
              <el-option v-for="p in partOptions" :key="p.partCode" :label="`${p.partCode} - ${p.partName}`" :value="p.partCode" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="partName" label="零件名称" width="200">
          <template #default="{ row }">
            <span>{{ row.partName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectedQuantity" label="预期数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.expectedQuantity" :min="1" @change="calcBoxes(row)" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column prop="packagingCapacity" label="包装容量" width="100">
          <template #default="{ row }">
            <span>{{ row.packagingCapacity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expectedBoxes" label="预期箱数" width="100">
          <template #default="{ row }">
            <span>{{ row.expectedBoxes }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80">
          <template #default="{ row }">
            <span>{{ row.unit || '个' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeDetail($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="form-footer">
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">保存入库单</el-button>
        <el-button size="large" @click="$router.back()">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Plus } from '@element-plus/icons-vue'
import { createInboundOrder, updateInboundOrder, getInboundOrderDetail } from '../../api/wms'
import { getSupplierList, getWarehouseList, getPartList } from '../../api/wms'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const suppliers = ref([])
const warehouses = ref([])
const partOptions = ref([])

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

const calcTotal = () => {
  formData.totalQuantity = formData.details.reduce((sum, d) => sum + (d.expectedQuantity || 0), 0)
  formData.totalBoxes = formData.details.reduce((sum, d) => sum + (d.expectedBoxes || 0), 0)
}

const calcBoxes = (row) => {
  if (row.packagingCapacity > 0) {
    row.expectedBoxes = Math.ceil(row.expectedQuantity / row.packagingCapacity)
  }
  calcTotal()
}

const addDetail = () => {
  formData.details.push({
    partCode: '',
    partName: '',
    expectedQuantity: 0,
    packagingCapacity: 0,
    expectedBoxes: 0,
    unit: '个'
  })
}

const removeDetail = (index) => {
  formData.details.splice(index, 1)
  calcTotal()
}

const onSupplierChange = async () => {
  // 清空零件选项，重新加载
  partOptions.value = []
  if (formData.supplierCode) {
    const res = await getPartList(formData.supplierCode)
    if (res.code === 200) {
      partOptions.value = res.data || []
    }
  }
}

const searchParts = async (query) => {
  if (query) {
    const res = await getPartList(formData.supplierCode)
    if (res.code === 200) {
      partOptions.value = (res.data || []).filter(p =>
        p.partCode.includes(query) || p.partName.includes(query)
      )
    }
  } else {
    await onSupplierChange()
  }
}

const onPartChange = (partCode, index) => {
  const part = partOptions.value.find(p => p.partCode === partCode)
  if (part) {
    formData.details[index].partName = part.partName
    formData.details[index].packagingCapacity = part.packagingCapacity
    formData.details[index].unit = part.unit
    formData.details[index].expectedBoxes = Math.ceil(formData.details[index].expectedQuantity / part.packagingCapacity)
  }
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
        receivedQuantity: d.receivedQuantity,
        packagingCapacity: d.packagingCapacity,
        expectedBoxes: d.expectedBoxes,
        unit: d.unit
      }))
    })
    await onSupplierChange()
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
    if (!detail.partCode) {
      ElMessage.warning('请完善零件信息')
      return
    }
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
  // 加载基础数据
  const [supplierRes, warehouseRes] = await Promise.all([
    getSupplierList(),
    getWarehouseList()
  ])
  if (supplierRes.code === 200) suppliers.value = supplierRes.data || []
  if (warehouseRes.code === 200) warehouses.value = warehouseRes.data || []

  // 检查是否为编辑模式
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
}
.form-footer {
  margin-top: 30px;
  text-align: center;
}

/* 移动端表单优化 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .page-header .el-button {
    width: 100%;
  }
  
  .responsive-form {
    /* 移动端减小标签宽度 */
  }
  
  .responsive-form :deep(.el-form-item__label) {
    width: 80px !important;
    font-size: 14px;
  }
  
  .responsive-form :deep(.el-form-item__content) {
    margin-left: 80px !important;
  }
  
  /* 明细表格横向滚动 */
  .el-table {
    font-size: 13px;
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
}

@media (max-width: 480px) {
  .responsive-form :deep(.el-form-item__label) {
    width: 70px !important;
  }
  
  .responsive-form :deep(.el-form-item__content) {
    margin-left: 70px !important;
  }
}
</style>