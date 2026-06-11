<template>
  <main class="not-found">
    <el-result title="页面不存在" sub-title="请从左侧菜单重新选择页面">
      <template #icon>
        <el-icon size="64"><Warning /></el-icon>
      </template>
      <template #extra>
        <el-button type="primary" @click="$router.replace('/dashboard')">返回工作台</el-button>
      </template>
    </el-result>
  </main>
</template>

<script setup>
import { Warning } from '@element-plus/icons-vue'
</script>
