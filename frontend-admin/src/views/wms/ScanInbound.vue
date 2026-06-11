<template>
  <div class="wms-page">
    <div class="page-header">
      <h2>看板扫码入库</h2>
    </div>

    <el-row :gutter="20" class="scan-layout">
      <!-- 左侧/上方：摄像头扫码 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="scanner-card">
          <template #header>
            <div class="card-header">
              <span>📷 摄像头扫码</span>
            </div>
          </template>
          <Scanner @decode="onScanDecode" />
        </el-card>

        <el-card shadow="never" class="location-card">
          <template #header>
            <div class="card-header">
              <span>📍 选择库位</span>
            </div>
          </template>
          <el-form>
            <el-form-item label="目标库位">
              <el-select
                v-model="locationCode"
                placeholder="请选择库位"
                filterable
                style="width: 100%"
                :disabled="!kanbanInfo"
                size="large"
              >
                <el-option
                  v-for="loc in locations"
                  :key="loc.location_code"
                  :label="`${loc.location_code} - ${loc.location_name}`"
                  :value="loc.location_code"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :disabled="!kanbanInfo || !locationCode"
                @click="confirmInbound"
                :loading="submitting"
                style="width: 100%"
              >
                <el-icon><Check /></el-icon>
                确认入库
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧/下方：手动输入 + 看板信息 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="input-card">
          <template #header>
            <div class="card-header">
              <span>⌨️ 手动输入</span>
            </div>
          </template>
          <el-form @submit.prevent="handleScan">
            <el-form-item label="扫描看板二维码">
              <el-input
                v-model="scanCode"
                placeholder="请扫描或输入看板号"
                size="large"
                ref="scanInput"
                @keyup.enter="handleScan"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" @click="handleScan" style="width: 100%">
                <el-icon><Search /></el-icon>
                验证看板
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="kanbanInfo"
            title="✅ 看板验证成功"
            type="success"
            show-icon
            :closable="false"
            class="kanban-alert"
          >
            <template #default>
              <div class="kanban-info">
                <div class="info-item">
                  <span class="label">零件号：</span>
                  <span class="value">{{ kanbanInfo.partCode }}</span>
                </div>
                <div class="info-item">
                  <span class="label">零件名称：</span>
                  <span class="value">{{ kanbanInfo.partName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">供应商：</span>
                  <span class="value">{{ kanbanInfo.supplierName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">数量：</span>
                  <span class="value highlight">{{ kanbanInfo.quantity }}</span>
                </div>
                <div class="info-item">
                  <span class="label">入库单号：</span>
                  <span class="value">{{ kanbanInfo.orderNo }}</span>
                </div>
                <div class="info-item">
                  <span class="label">看板号：</span>
                  <span class="value code">{{ kanbanInfo.kanbanNo }}</span>
                </div>
              </div>
            </template>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Check } from '@element-plus/icons-vue'
import { validateKanban, scanInbound } from '../../api/wms'
import { getLocationList } from '../../api/wms'
import Scanner from '../../components/Scanner.vue'

const scanInput = ref(null)
const scanCode = ref('')
const kanbanInfo = ref(null)
const locationCode = ref('')
const locations = ref([])
const submitting = ref(false)

// 摄像头扫码回调
const onScanDecode = (result) => {
  if (result) {
    scanCode.value = result
    handleScan()
  }
}

const handleScan = async () => {
  if (!scanCode.value.trim()) {
    ElMessage.warning('请输入看板号')
    return
  }

  const res = await validateKanban(scanCode.value)
  if (res.code === 200) {
    kanbanInfo.value = res.data
    ElMessage.success('看板验证成功')
    scanCode.value = ''
    nextTick(() => {
      scanInput.value?.focus()
    })
  } else {
    ElMessage.error(res.message || '看板无效或已使用')
    kanbanInfo.value = null
    locationCode.value = ''
  }
}

const confirmInbound = async () => {
  if (!kanbanInfo.value) {
    ElMessage.warning('请先扫描看板')
    return
  }
  if (!locationCode.value) {
    ElMessage.warning('请选择库位')
    return
  }

  submitting.value = true
  try {
    const res = await scanInbound({
      kanbanNo: kanbanInfo.value.kanbanNo,
      locationCode: locationCode.value
    })
    if (res.code === 200) {
      ElMessage.success(`入库成功：${kanbanInfo.value.partName} x ${kanbanInfo.value.quantity}`)
      kanbanInfo.value = null
      locationCode.value = ''
    } else {
      ElMessage.error(res.message || '入库失败')
    }
  } catch (error) {
    console.error('入库错误:', error)
    ElMessage.error('入库失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
    nextTick(() => {
      scanInput.value?.focus()
    })
  }
}

const loadLocations = async () => {
  try {
    const res = await getLocationList()
    if (res.code === 200) {
      locations.value = res.data || []
    }
  } catch (error) {
    console.error('加载库位失败:', error)
  }
}

onMounted(() => {
  loadLocations()
  nextTick(() => {
    scanInput.value?.focus()
  })
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
}

.scan-layout {
  display: flex;
  gap: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.scanner-card,
.location-card,
.input-card {
  margin-bottom: 20px;
}

.kanban-alert {
  margin-top: 20px;
}

.kanban-info {
  padding: 12px 0;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e4e7ed;
  font-size: 14px;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #909399;
  min-width: 90px;
  font-weight: 500;
}

.info-item .value {
  color: #303133;
  flex: 1;
}

.info-item .value.highlight {
  color: #67c23a;
  font-weight: 700;
  font-size: 16px;
}

.info-item .value.code {
  font-family: 'Courier New', monospace;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .wms-page {
    padding: 12px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .scan-layout {
    flex-direction: column;
    gap: 16px;
  }

  .scanner-card,
  .location-card,
  .input-card {
    margin-bottom: 16px;
  }

  .card-header {
    font-size: 15px;
  }

  /* 表单标签优化 */
  :deep(.el-form-item__label) {
    font-size: 14px;
  }

  /* 输入框和按钮优化 */
  :deep(.el-input__inner) {
    font-size: 16px;
  }

  :deep(.el-button) {
    min-height: 44px;
    font-size: 16px;
  }

  /* 看板信息优化 */
  .info-item {
    padding: 10px 0;
    font-size: 15px;
  }

  .info-item .label {
    min-width: 80px;
    font-size: 14px;
  }

  .info-item .value {
    font-size: 15px;
  }

  .info-item .value.highlight {
    font-size: 18px;
  }

  .kanban-alert {
    :deep(.el-alert__content) {
      width: 100%;
    }
  }
}

@media (max-width: 480px) {
  .wms-page {
    padding: 8px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .info-item .label {
    min-width: 70px;
    font-size: 13px;
  }

  .info-item .value {
    font-size: 14px;
  }
}
</style>