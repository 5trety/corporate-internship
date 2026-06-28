<template>
  <div class="scanner-container">
    <div class="scanner-video-wrapper">
      <video ref="videoRef" autoplay playsinline class="scanner-video"></video>
      <canvas ref="canvasRef" style="display: none;"></canvas>
      <div class="scanner-overlay">
        <div class="scanner-frame"></div>
      </div>
    </div>
    <div class="scanner-actions">
      <el-button type="primary" size="small" @click="startScan" :disabled="scanning">
        <el-icon><VideoCamera /></el-icon> 开启摄像头
      </el-button>
      <el-button type="danger" size="small" @click="stopScan" :disabled="!scanning">
        <el-icon><Close /></el-icon> 关闭摄像头
      </el-button>
    </div>
    <div v-if="scanResult" class="scan-result">
      <el-alert :title="`扫描结果：${scanResult}`" type="success" show-icon :closable="false" size="small" />
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { BrowserMultiFormatReader } from '@zxing/library'
import { VideoCamera, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['decode'])

const videoRef = ref(null)
const canvasRef = ref(null)
const scanning = ref(false)
const scanResult = ref('')
let codeReader = null

const startScan = async () => {
  try {
    codeReader = new BrowserMultiFormatReader()
    const videoInputDevices = await codeReader.listVideoInputDevices()

    if (videoInputDevices.length === 0) {
      ElMessage.error('未找到摄像头设备')
      return
    }

    // 优先使用后置摄像头（如果有）
    let deviceId = videoInputDevices[0]?.deviceId
    if (videoInputDevices.length > 1) {
      deviceId = videoInputDevices[videoInputDevices.length - 1]?.deviceId
    }

    scanning.value = true
    scanResult.value = ''

    await codeReader.decodeFromVideoDevice(deviceId, videoRef.value, (result, err) => {
      if (result) {
        const text = result.getText()
        scanResult.value = text
        emit('decode', text)
        // 扫描成功后自动停止
        setTimeout(() => {
          stopScan()
        }, 1500)
      }
      if (err && scanning.value) {
        // 扫码中，继续等待
      }
    })
  } catch (error) {
    console.error('启动摄像头失败:', error)
    ElMessage.error('启动摄像头失败，请检查权限设置')
    scanning.value = false
  }
}

const stopScan = () => {
  if (codeReader) {
    codeReader.reset()
    codeReader = null
  }
  scanning.value = false
  if (videoRef.value && videoRef.value.srcObject) {
    const tracks = videoRef.value.srcObject.getTracks()
    tracks.forEach(track => track.stop())
    videoRef.value.srcObject = null
  }
}

onUnmounted(() => {
  stopScan()
})
</script>

<style scoped>
.scanner-container {
  width: 100%;
}

.scanner-video-wrapper {
  position: relative;
  width: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.scanner-video {
  width: 100%;
  height: auto;
  display: block;
}

.scanner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.scanner-frame {
  width: 70%;
  height: 40%;
  border: 2px solid #00ff00;
  border-radius: 8px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { border-color: #00ff00; box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5); }
  50% { border-color: #00ff88; box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.4); }
  100% { border-color: #00ff00; box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5); }
}

.scanner-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.scan-result {
  margin-top: 12px;
}
</style>