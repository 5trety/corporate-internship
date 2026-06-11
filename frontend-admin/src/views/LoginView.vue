<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <p class="eyebrow">LOGIN</p>
        <h1>后台管理系统</h1>
        <p>请登录后进入系统。</p>
      </div>

      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <div class="form-title">
          <h2>账号登录</h2>
          <span>请输入用户名和密码</span>
        </div>

        <el-form-item prop="username">
          <el-input v-model="form.username" :prefix-icon="User" placeholder="用户名" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            placeholder="密码"
            show-password
          />
        </el-form-item>

        <el-button
          class="login-button"
          type="primary"
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录系统
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { login } from '../api/auth'
import { saveUser } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true

  try {
    const result = await login(form.username, form.password)
    if (result.code !== 200) {
      ElMessage.error(result.message || '登录失败')
      return
    }

    saveUser(result.data)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>
