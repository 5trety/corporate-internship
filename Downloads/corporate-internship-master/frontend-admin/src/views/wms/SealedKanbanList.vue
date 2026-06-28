<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>封存看板管理</h2>
    </div>

    <!-- 封存看板列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>封存看板列表</span>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="看板号">
          <el-input v-model="queryForm.kanbanNo" placeholder="请输入看板号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="sealedKanbans" v-loading="loading" stripe>
        <el-table-column prop="kanbanNo" label="看板号" width="200" />
        <el-table-column prop="partCode" label="零件号" width="150" />
        <el-table-column prop="partName" label="零件名称" width="200" />
        <el-table-column prop="quantity" label="数量" width="100" align="center" />
        <el-table-column prop="supplierName" label="供应商" width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="danger">已封存</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleUnseal(row)">
              解封
            </el-button>
            <el-button type="primary" size="small" @click="viewHistory(row.kanbanNo)">
              历史
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && sealedKanbans.length === 0" description="暂无封存看板" />
    </el-card>

    <!-- 解封对话框 -->
    <el-dialog v-model="unsealDialogVisible" title="解封看板" width="500px">
      <el-alert
        title="确认要解封该看板吗？"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />
      
      <el-form :model="unsealForm" label-width="100px">
        <el-form-item label="看板号">
          <el-input v-model="unsealForm.kanbanNo" disabled />
        </el-form-item>
        <el-form-item label="零件号">
          <el-input v-model="unsealForm.partCode" disabled />
        </el-form-item>
        <el-form-item label="零件名称">
          <el-input v-model="unsealForm.partName" disabled />
        </el-form-item>
        <el-form-item label="数量">
          <el-input v-model="unsealForm.quantity" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="unsealForm.remark" type="textarea" :rows="3" placeholder="请输入解封原因" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="unsealDialogVisible = false">取消</el-button>
        <el-button type="success" @click="confirmUnseal">确认解封</el-button>
      </template>
    </el-dialog>

    <!-- 封存历史对话框 -->
    <el-dialog v-model="historyDialogVisible" title="封存历史" width="800px">
      <el-table :data="sealHistory" stripe>
        <el-table-column prop="kanbanNo" label="看板号" width="200" />
        <el-table-column prop="action" label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.action === 'SEAL'" type="danger">封存</el-tag>
            <el-tag v-else type="success">解封</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column prop="createdAt" label="操作时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKanbanList, unsealKanban, getSealHistory } from '@/api/wms'

const loading = ref(false)
const sealedKanbans = ref([])
const unsealDialogVisible = ref(false)
const historyDialogVisible = ref(false)
const sealHistory = ref([])

const queryForm = ref({
  kanbanNo: ''
})

const unsealForm = ref({
  kanbanNo: '',
  partCode: '',
  partName: '',
  quantity: 0,
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getKanbanList({
      isSealed: 1,
      kanbanNo: queryForm.value.kanbanNo || undefined
    })
    
    if (res.code === 200) {
      const list = res.data.list || res.data || []
      // 映射下划线命名为驼峰命名
      sealedKanbans.value = list.map(item => ({
        kanbanNo: item.kanban_no || item.kanbanNo,
        partCode: item.part_code || item.partCode,
        partName: item.part_name || item.partName,
        quantity: item.quantity,
        supplierCode: item.supplier_code || item.supplierCode,
        supplierName: item.supplier_name || item.supplierName,
        warehouseCode: item.warehouse_code || item.warehouseCode,
        warehouseName: item.warehouse_name || item.warehouseName,
        status: item.status,
        isSealed: item.is_sealed || item.isSealed
      }))
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleUnseal = (row) => {
  unsealForm.value = {
    kanbanNo: row.kanbanNo,
    partCode: row.partCode,
    partName: row.partName,
    quantity: row.quantity,
    remark: ''
  }
  unsealDialogVisible.value = true
}

const confirmUnseal = async () => {
  try {
    const res = await unsealKanban({
      kanbanNo: unsealForm.value.kanbanNo,
      remark: unsealForm.value.remark
    })
    
    if (res.code === 200) {
      ElMessage.success('解封成功')
      unsealDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '解封失败')
    }
  } catch (error) {
    ElMessage.error('解封失败')
  }
}

const viewHistory = async (kanbanNo) => {
  try {
    const res = await getSealHistory({ kanbanNo })
    if (res.code === 200) {
      const list = res.data || []
      // 映射下划线命名为驼峰命名
      sealHistory.value = list.map(item => ({
        kanbanNo: item.kanban_no || item.kanbanNo,
        action: item.action,
        operator: item.operator,
        remark: item.remark,
        createdAt: item.created_at || item.createdAt
      }))
      historyDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取历史失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.query-form {
  margin-bottom: 20px;
}
</style>
