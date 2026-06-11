<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>零件管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增零件
      </el-button>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="供应商">
          <el-select v-model="filterSupplierCode" placeholder="请选择供应商" clearable @change="loadData">
            <el-option v-for="s in suppliers" :key="s.supplierCode" :label="s.supplierName" :value="s.supplierCode" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="partCode" label="零件号" width="180" />
        <el-table-column prop="partName" label="零件名称" width="200" />
        <el-table-column prop="supplierName" label="供应商" width="150" />
        <el-table-column prop="packagingCapacity" label="包装容量" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="price" label="单价" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="零件号" prop="partCode">
          <el-input v-model="formData.partCode" :disabled="!!formData.id" placeholder="请输入零件号" />
        </el-form-item>
        <el-form-item label="零件名称" prop="partName">
          <el-input v-model="formData.partName" placeholder="请输入零件名称" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplierCode">
          <el-select v-model="formData.supplierCode" placeholder="请选择供应商" style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.supplierCode" :label="s.supplierName" :value="s.supplierCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="包装容量">
          <el-input-number v-model="formData.packagingCapacity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="formData.unit" placeholder="个/箱/套" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="formData.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="重量">
          <el-input-number v-model="formData.weight" :min="0" :precision="2" style="width: 100%" />
          <span style="margin-left: 10px">kg</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getPartList, savePart, deletePart } from '../../api/wms'
import { getSupplierList } from '../../api/wms'

const loading = ref(false)
const tableData = ref([])
const suppliers = ref([])
const filterSupplierCode = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('新增零件')
const formRef = ref(null)

const formData = reactive({
  id: null,
  partCode: '',
  partName: '',
  supplierCode: '',
  packagingCapacity: 0,
  unit: '个',
  price: 0,
  weight: 0
})

const rules = {
  partCode: [{ required: true, message: '请输入零件号', trigger: 'blur' }],
  partName: [{ required: true, message: '请输入零件名称', trigger: 'blur' }],
  supplierCode: [{ required: true, message: '请选择供应商', trigger: 'change' }]
}

const loadSuppliers = async () => {
  const res = await getSupplierList()
  if (res.code === 200) {
    suppliers.value = res.data || []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPartList(filterSupplierCode.value)
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  filterSupplierCode.value = ''
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增零件'
  Object.assign(formData, {
    id: null,
    partCode: '',
    partName: '',
    supplierCode: '',
    packagingCapacity: 0,
    unit: '个',
    price: 0,
    weight: 0
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑零件'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除零件"${row.partName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deletePart(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  const res = await savePart(formData)
  if (res.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } else {
    ElMessage.error(res.message || '保存失败')
  }
}

onMounted(() => {
  loadSuppliers()
  loadData()
})
</script>