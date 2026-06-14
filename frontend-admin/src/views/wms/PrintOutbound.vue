<template>
  <div class="wms-page print-page">
    <div class="page-header">
      <h2>出库单打印</h2>
      <div class="header-actions">
        <el-button @click="$router.back()">
          <el-icon><Back /></el-icon>
          返回
        </el-button>
        <el-button type="primary" @click="handlePrint">
          <el-icon><Printer /></el-icon>
          打印
        </el-button>
      </div>
    </div>

    <div v-loading="loading" ref="printRef">
      <el-card shadow="never" class="print-card">
        <!-- 打印头部 -->
        <div class="print-header">
          <h1>出库单</h1>
          <div class="header-info">
            <span class="order-no">单号：{{ orderData.order_no }}</span>
            <span class="print-date">打印时间：{{ printDate }}</span>
          </div>
        </div>

        <!-- 基本信息 -->
        <el-descriptions :column="3" border class="info-section">
          <el-descriptions-item label="出库类型">
            {{ orderData.outbound_type }}
          </el-descriptions-item>
          <el-descriptions-item label="客户编码">
            {{ orderData.customer_code }}
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ orderData.customer_name }}
          </el-descriptions-item>
          <el-descriptions-item label="仓库">
            {{ orderData.warehouse_code || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ orderData.created_by || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ orderData.created_at }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 明细表格 -->
        <el-table :data="details" border class="detail-section">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="part_code" label="零件号" width="150" />
          <el-table-column prop="part_name" label="零件名称" width="200" />
          <el-table-column prop="expected_quantity" label="数量" width="100" align="right" />
          <el-table-column prop="packaging_capacity" label="包装容量" width="100" align="center" />
          <el-table-column prop="expected_boxes" label="箱数" width="100" align="center" />
          <el-table-column prop="unit" label="单位" width="80" align="center">
            <template #default>
              <span>个</span>
            </template>
          </el-table-column>
          <el-table-column label="二维码" width="150" align="center">
            <template #default="{ row }">
              <div v-if="row.qrImage" class="qr-code">
                <img :src="row.qrImage" alt="二维码" />
              </div>
              <el-tag v-else type="info" size="small">生成中...</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 汇总信息 -->
        <div class="summary-section">
          <el-row>
            <el-col :span="8">
              <div class="summary-item">
                <span class="label">总数量：</span>
                <span class="value">{{ orderData.total_quantity || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item">
                <span class="label">总箱数：</span>
                <span class="value">{{ orderData.total_boxes || 0 }}</span>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item">
                <span class="label">明细行数：</span>
                <span class="value">{{ details.length }}</span>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 备注 -->
        <div class="remark-section" v-if="orderData.remark">
          <strong>备注：</strong>{{ orderData.remark }}
        </div>

        <!-- 签名区域 -->
        <div class="signature-section">
          <el-row :gutter="40">
            <el-col :span="8">
              <div class="signature-item">
                <div class="signature-line">制单人：___________</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="signature-item">
                <div class="signature-line">审核人：___________</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="signature-item">
                <div class="signature-line">收货人：___________</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 打印预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="打印预览"
      width="800px"
      :destroy-on-close="true"
    >
      <div class="preview-container" ref="previewRef">
        <div v-html="previewHtml"></div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">取消</el-button>
        <el-button type="primary" @click="doPrint">打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Printer } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { printOutboundOrder } from '../../api/wms'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const previewVisible = ref(false)
const previewHtml = ref('')
const printRef = ref(null)
const previewRef = ref(null)

const orderData = ref({})
const details = ref([])
const printDate = ref('')

const generateQRCodes = async () => {
  for (const detail of details.value) {
    if (detail.qrContent) {
      try {
        const qrImage = await QRCode.toDataURL(detail.qrContent, {
          width: 120,
          margin: 1,
          color: {
            dark: '#000000',
            light: '#ffffff'
          }
        })
        detail.qrImage = qrImage
      } catch (error) {
        console.error('生成二维码失败:', error)
      }
    }
  }
}

const loadData = async () => {
  loading.value = true
  const orderNo = route.params.orderNo

  try {
    const res = await printOutboundOrder(orderNo)

    if (res.code === 200) {
      orderData.value = res.data?.order || {}
      details.value = res.data?.details || []

      // 生成二维码
      await generateQRCodes()
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handlePrint = async () => {
  previewVisible.value = true

  await nextTick()

  // 生成打印预览 HTML
  previewHtml.value = `
    <style>
      @media print {
        body { margin: 0; padding: 20px; }
        .print-header { text-align: center; margin-bottom: 20px; }
        .print-header h1 { font-size: 24px; margin: 0 0 10px 0; }
        .header-info { font-size: 14px; color: #666; }
        .info-section { margin-bottom: 20px; }
        .detail-section { margin-bottom: 20px; }
        .summary-section { padding: 10px; background: #f5f7fa; margin-bottom: 20px; }
        .remark-section { margin-bottom: 20px; padding: 10px; border: 1px solid #ddd; }
        .signature-section { margin-top: 40px; }
        .signature-item { margin-bottom: 20px; }
        .signature-line { font-size: 14px; }
      }
    </style>
    <div class="print-content">
      <div class="print-header">
        <h1>出库单</h1>
        <div class="header-info">
          <span>单号：${orderData.value.order_no}</span>
          <span>打印时间：${printDate.value}</span>
        </div>
      </div>
      <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
        <tr style="background: #f5f7fa;">
          <th style="padding: 10px; border: 1px solid #ddd;">零件号</th>
          <th style="padding: 10px; border: 1px solid #ddd;">零件名称</th>
          <th style="padding: 10px; border: 1px solid #ddd;">数量</th>
          <th style="padding: 10px; border: 1px solid #ddd;">包装容量</th>
          <th style="padding: 10px; border: 1px solid #ddd;">箱数</th>
          <th style="padding: 10px; border: 1px solid #ddd;">单位</th>
        </tr>
        ${details.value.map(d => `
          <tr>
            <td style="padding: 10px; border: 1px solid #ddd;">${d.part_code}</td>
            <td style="padding: 10px; border: 1px solid #ddd;">${d.part_name}</td>
            <td style="padding: 10px; border: 1px solid #ddd; text-align: right;">${d.expected_quantity}</td>
            <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">${d.packaging_capacity}</td>
            <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">${d.expected_boxes}</td>
            <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">个</td>
          </tr>
        `).join('')}
      </table>
      <div style="padding: 10px; background: #f5f7fa; margin-bottom: 20px;">
        <strong>汇总：</strong>总数量 ${orderData.value.total_quantity || 0}，总箱数 ${orderData.value.total_boxes || 0}
      </div>
      ${orderData.value.remark ? `<div style="margin-bottom: 20px;"><strong>备注：</strong>${orderData.value.remark}</div>` : ''}
      <div style="margin-top: 40px;">
        <div style="display: inline-block; width: 30%;">制单人：___________</div>
        <div style="display: inline-block; width: 30%;">审核人：___________</div>
        <div style="display: inline-block; width: 30%;">收货人：___________</div>
      </div>
    </div>
  `
}

const doPrint = () => {
  const printContent = previewRef.value?.innerHTML
  if (!printContent) {
    ElMessage.error('打印内容为空')
    return
  }

  const newWindow = window.open('', '_blank')
  if (newWindow) {
    newWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>出库单打印 - ${orderData.value.order_no}</title>
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { padding: 20px; font-family: 'Microsoft YaHei', Arial, sans-serif; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
            th, td { border: 1px solid #ddd; padding: 10px; }
            th { background: #f5f7fa; font-weight: normal; }
            @media print {
              body { padding: 0; }
            }
          </style>
        </head>
        <body>
          ${printContent}
        </body>
      </html>
    `)
    newWindow.document.close()
    newWindow.focus()
    setTimeout(() => {
      newWindow.print()
      newWindow.close()
    }, 500)
  } else {
    ElMessage.error('无法打开打印窗口，请检查浏览器设置')
  }

  previewVisible.value = false
}

onMounted(() => {
  printDate.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
  loadData()
})
</script>

<style scoped>
.print-page {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.print-card {
  max-width: 1200px;
  margin: 0 auto;
}

.print-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #303133;
}

.print-header h1 {
  font-size: 28px;
  margin: 0 0 15px 0;
}

.header-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.info-section {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.qr-code {
  display: inline-block;
}

.qr-code img {
  width: 80px;
  height: 80px;
}

.summary-section {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary-item {
  text-align: center;
}

.summary-item .label {
  font-size: 14px;
  color: #909399;
}

.summary-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-left: 8px;
}

.remark-section {
  padding: 15px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  margin-bottom: 20px;
}

.signature-section {
  margin-top: 50px;
}

.signature-item {
  text-align: center;
}

.signature-line {
  font-size: 14px;
  color: #606266;
}

.preview-container {
  max-height: 600px;
  overflow-y: auto;
}

/* 打印样式 */
@media print {
  .page-header,
  .el-dialog__footer {
    display: none !important;
  }

  .print-card {
    box-shadow: none !important;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
  }

  .header-actions .el-button {
    width: 100%;
  }

  .print-header h1 {
    font-size: 20px;
  }

  .summary-section {
    padding: 10px;
  }

  .summary-item .value {
    font-size: 18px;
  }
}
</style>
