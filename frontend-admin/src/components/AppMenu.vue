<template>
  <el-menu
    class="app-menu"
    :default-active="route.path"
    router
    unique-opened
    background-color="transparent"
  >
    <template v-for="item in menus" :key="item.id">
      <el-sub-menu v-if="item.children?.length" :index="String(item.id)">
        <template #title>
          <el-icon><component :is="resolveIcon(item.icon || 'Grid')" /></el-icon>
          <span>{{ item.name }}</span>
        </template>

        <el-menu-item
          v-for="child in item.children"
          :key="child.id"
          :index="child.path"
        >
          <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
          <span>{{ child.name }}</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-else :index="item.path">
        <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
        <span>{{ item.name }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { Document, Grid, House, Key, Setting, User } from '@element-plus/icons-vue'
import { watch } from 'vue'

const props = defineProps({
  menus: {
    type: Array,
    required: true
  }
})

const route = useRoute()

// 监听菜单数据变化
watch(() => props.menus, (newMenus) => {
  console.log('📋 AppMenu 收到菜单数据:', newMenus)
  console.log('📋 菜单数量:', newMenus?.length)
}, { immediate: true, deep: true })

const iconMap = {
  Document,
  Grid,
  House,
  Key,
  Setting,
  User
}

function resolveIcon(name) {
  return iconMap[name] || Document
}
</script>
