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

      <el-divider>出库明细</el-divider>

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
        <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">保存出库单</el-button>
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
import { createOutboundOrder, updateOutboundOrder, getOutboundOrderDetail } from '../../api/wms'
import { getWarehouseList, getPartList } from '../../api/wms'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)
const warehouses = ref([])
const partOptions = ref([])

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

const searchParts = async (query) => {
  if (query) {
    const res = await getPartList()
    if (res.code === 200) {
      partOptions.value = (res.data || []).filter(p =>
        p.partCode.includes(query) || p.partName.includes(query)
      )
    }
  } else {
    const res = await getPartList()
    if (res.code === 200) {
      partOptions.value = res.data || []
    }
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
        shippedQuantity: d.shippedQuantity,
        packagingCapacity: d.packagingCapacity,
        expectedBoxes: d.expectedBoxes,
        unit: d.unit
      }))
    })
    const res2 = await getPartList()
    if (res2.code === 200) {
      partOptions.value = res2.data || []
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
      res = await updateOutboundOrder(route.params.orderNo, formData)
    } else {
      res = await createOutboundOrder(formData)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
      router.push('/wms/outbound-order/list')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载基础数据
  const warehouseRes = await getWarehouseList()
  if (warehouseRes.code === 200) warehouses.value = warehouseRes.data || []

  const partRes = await getPartList()
  if (partRes.code === 200) partOptions.value = partRes.data || []

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
</style>
