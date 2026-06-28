<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>转包管理</h2>
      <el-button type="primary" @click="showTransferDialog">
        <el-icon><Plus /></el-icon>
        新建转包
      </el-button>
    </div>

    <!-- 转包单列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>转包单列表</span>
          <el-button type="primary" @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="transferOrders" v-loading="loading" stripe>
        <el-table-column prop="order_no" label="转包单号" width="180" />
        <el-table-column prop="transfer_type" label="转包类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.transfer_type === 'split'" type="warning">向下拆分</el-tag>
            <el-tag v-else type="success">向上合并</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="from_kanban_no" label="源看板号" width="200" />
        <el-table-column prop="to_kanban_nos" label="目标看板号" width="200" />
        <el-table-column prop="original_quantity" label="原始数量" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'completed'" type="success">已完成</el-tag>
            <el-tag v-else type="info">待处理</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && transferOrders.length === 0" description="暂无转包单" />
    </el-card>

    <!-- 新建转包对话框 -->
    <el-dialog v-model="transferDialogVisible" :title="transferForm.transferType === 'split' ? '向下拆分' : '向上合并'" width="600px">
      <el-form :model="transferForm" label-width="120px">
        <el-form-item label="转包类型">
          <el-radio-group v-model="transferForm.transferType">
            <el-radio label="split">向下拆分（1个→多个）</el-radio>
            <el-radio label="merge">向上合并（多个→1个）</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 拆分模式 -->
        <template v-if="transferForm.transferType === 'split'">
          <el-form-item label="源看板号">
            <el-input v-model="transferForm.fromKanbanNo" placeholder="请输入源看板号" />
          </el-form-item>
          <el-form-item label="拆分数量">
            <div v-for="(qty, index) in transferForm.newQuantities" :key="index" class="quantity-input">
              <el-input-number v-model="transferForm.newQuantities[index]" :min="1" style="width: 100%" />
              <el-button type="danger" @click="removeQuantity(index)" :disabled="transferForm.newQuantities.length <= 2">
                删除
              </el-button>
            </div>
            <el-button type="primary" @click="addQuantity" style="margin-top: 10px">
              添加数量
            </el-button>
          </el-form-item>
        </template>

        <!-- 合并模式 -->
        <template v-else>
          <el-form-item label="源看板号列表">
            <div v-for="(kanban, index) in transferForm.fromKanbanNos" :key="index" class="kanban-input">
              <el-input v-model="transferForm.fromKanbanNos[index]" placeholder="请输入看板号" />
              <el-button type="danger" @click="removeKanban(index)" :disabled="transferForm.fromKanbanNos.length <= 2">
                删除
              </el-button>
            </div>
            <el-button type="primary" @click="addKanban" style="margin-top: 10px">
              添加看板
            </el-button>
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeTransfer">执行转包</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="转包单详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="转包单号">{{ detailData.order_no }}</el-descriptions-item>
        <el-descriptions-item label="转包类型">
          <el-tag v-if="detailData.transfer_type === 'split'" type="warning">向下拆分</el-tag>
          <el-tag v-else type="success">向上合并</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="源看板号">{{ detailData.from_kanban_no }}</el-descriptions-item>
        <el-descriptions-item label="目标看板号">{{ detailData.to_kanban_nos }}</el-descriptions-item>
        <el-descriptions-item label="原始数量">{{ detailData.original_quantity }}</el-descriptions-item>
        <el-descriptions-item label="新数量">{{ detailData.new_quantities }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="detailData.status === 'completed'" type="success">已完成</el-tag>
          <el-tag v-else type="info">待处理</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailData.operator }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detailData.created_at }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTransferOrderList, splitKanban, mergeKanban } from '@/api/wms'

const loading = ref(false)
const transferOrders = ref([])
const transferDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailData = ref({})

const transferForm = ref({
  transferType: 'split',
  fromKanbanNo: '',
  fromKanbanNos: ['', ''],
  newQuantities: [0, 0],
  remark: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTransferOrderList({
      page: 1,
      size: 100
    })
    
    if (res.code === 200) {
      transferOrders.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const showTransferDialog = () => {
  transferForm.value = {
    transferType: 'split',
    fromKanbanNo: '',
    fromKanbanNos: ['', ''],
    newQuantities: [0, 0],
    remark: ''
  }
  transferDialogVisible.value = true
}

const addQuantity = () => {
  transferForm.value.newQuantities.push(0)
}

const removeQuantity = (index) => {
  transferForm.value.newQuantities.splice(index, 1)
}

const addKanban = () => {
  transferForm.value.fromKanbanNos.push('')
}

const removeKanban = (index) => {
  transferForm.value.fromKanbanNos.splice(index, 1)
}

const executeTransfer = async () => {
  try {
    if (transferForm.value.transferType === 'split') {
      // 向下拆分
      if (!transferForm.value.fromKanbanNo) {
        ElMessage.warning('请输入源看板号')
        return
      }
      const totalQty = transferForm.value.newQuantities.reduce((sum, q) => sum + q, 0)
      if (totalQty === 0) {
        ElMessage.warning('请输入拆分数量')
        return
      }

      const res = await splitKanban({
        fromKanbanNo: transferForm.value.fromKanbanNo,
        newQuantities: transferForm.value.newQuantities
      })

      if (res.code === 200) {
        ElMessage.success('拆分成功')
        transferDialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.message || '拆分失败')
      }
    } else {
      // 向上合并
      const validKanbans = transferForm.value.fromKanbanNos.filter(k => k.trim())
      if (validKanbans.length < 2) {
        ElMessage.warning('请至少输入2个看板号')
        return
      }

      const res = await mergeKanban({
        fromKanbanNos: validKanbans
      })

      if (res.code === 200) {
        ElMessage.success('合并成功')
        transferDialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.message || '合并失败')
      }
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const viewDetail = (row) => {
  detailData.value = row
  detailDialogVisible.value = true
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

.quantity-input,
.kanban-input {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.quantity-input .el-input-number,
.kanban-input .el-input {
  flex: 1;
}
</style>
