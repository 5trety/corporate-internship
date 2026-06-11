<template>
  <el-container class="admin-shell">
    <!-- 移动端顶部导航栏 -->
    <el-header v-if="isMobile" class="mobile-header">
      <el-icon class="menu-toggle" @click="drawerVisible = !drawerVisible">
        <Fold v-if="!drawerVisible" />
        <Expand v-else />
      </el-icon>
      <div class="mobile-title">
        <p class="eyebrow">MENU TABS</p>
        <h1>{{ currentTitle }}</h1>
        <!-- 调试信息 -->
        <small style="color: #999; font-size: 10px;">菜单数: {{ menus.length }}</small>
      </div>
      <div class="header-actions">
        <el-tag effect="plain" type="success" size="small">{{ user?.username || '用户' }}</el-tag>
      </div>
    </el-header>

    <!-- 移动端侧边抽屉 -->
    <el-drawer
      v-if="isMobile"
      v-model="drawerVisible"
      direction="ltr"
      size="248px"
      :with-header="false"
      class="mobile-drawer"
    >
      <el-aside class="admin-aside">
        <div class="brand">
          <div class="brand-mark">M</div>
          <div>
            <strong>后台管理系统</strong>
            <span>菜单与标签页</span>
          </div>
        </div>
        <AppMenu :menus="menus" />
      </el-aside>
    </el-drawer>

    <!-- PC端侧边栏 -->
    <el-aside v-if="!isMobile" width="248px" class="admin-aside">
      <div class="brand">
        <div class="brand-mark">M</div>
        <div>
          <strong>后台管理系统</strong>
          <span>菜单与标签页</span>
        </div>
      </div>
      <AppMenu :menus="menus" />
    </el-aside>

    <el-container>
      <el-header v-if="!isMobile" class="admin-header">
        <div>
          <p class="eyebrow">MENU TABS</p>
          <h1>{{ currentTitle }}</h1>
        </div>

        <div class="header-actions">
          <el-tag effect="plain" type="success">{{ user?.username || '用户' }}</el-tag>
          <el-button :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <div class="tabs-bar" v-if="!isMobile">
        <el-tabs
          v-model="activeTab"
          type="card"
          closable
          @tab-change="handleTabChange"
          @tab-remove="handleTabRemove"
        >
          <el-tab-pane
            v-for="tab in tabs"
            :key="tab.path"
            :label="tab.name"
            :name="tab.path"
            :closable="tabs.length > 1"
          />
        </el-tabs>
      </div>

      <el-main class="admin-main">
        <el-alert
          v-if="menuError"
          class="menu-alert"
          :title="menuError"
          type="warning"
          show-icon
          :closable="false"
        />
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchButton, Fold, Expand } from '@element-plus/icons-vue'
import { fetchMenus, logout } from '../api/auth'
import { clearUser, getSavedUser } from '../stores/auth'
import { findMenuByPath, firstReachableMenu } from '../utils/menu'
import AppMenu from '../components/AppMenu.vue'

const route = useRoute()
const router = useRouter()
const user = ref(getSavedUser())
const menus = ref([])
const tabs = ref([])
const activeTab = ref('')
const menuError = ref('')
const drawerVisible = ref(false)
const isMobile = ref(false)

// 检测设备类型
function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  loadMenus()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})

const currentTitle = computed(() => {
  // 移动端直接从路由元信息获取标题
  if (isMobile.value) {
    return route.meta.title || '后台管理'
  }
  // PC端从标签页获取
  return tabs.value.find((tab) => tab.path === activeTab.value)?.name || '后台管理'
})

function openTab(path) {
  const source = findMenuByPath(menus.value, path)
  if (!source) return

  // PC端：保存标签页
  if (!isMobile.value) {
    if (!tabs.value.some((tab) => tab.path === source.path)) {
      tabs.value.push(source)
    }
    activeTab.value = source.path
  }
  
  // 移动端选择后关闭抽屉
  drawerVisible.value = false
}

async function loadMenus() {
  const result = await fetchMenus()
  if (result.code !== 200) {
    menuError.value = result.message || '菜单加载失败'
    clearUser()
    router.replace('/login')
    return
  }

  menus.value = result.data || []
  console.log('✅ 菜单数据加载成功:', menus.value)
  console.log('📱 是否移动端:', isMobile.value)
  
  const firstPath = firstReachableMenu(menus.value)
  openTab(route.path === '/' ? firstPath : route.path)

  if (route.path === '/') {
    router.replace(firstPath)
  }
}

function handleTabChange(path) {
  if (path !== route.path) {
    router.push(path)
  }
}

function handleTabRemove(path) {
  if (tabs.value.length === 1) return

  const index = tabs.value.findIndex((tab) => tab.path === path)
  tabs.value = tabs.value.filter((tab) => tab.path !== path)

  if (path === activeTab.value) {
    const nextTab = tabs.value[index] || tabs.value[index - 1] || tabs.value[0]
    router.push(nextTab.path)
  }
}

async function handleLogout() {
  await logout()
  clearUser()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

watch(
  () => route.path,
  (path) => openTab(path)
)
</script>
