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

        <!-- 递归渲染子菜单 -->
        <template v-for="child in item.children" :key="child.id">
          <el-sub-menu v-if="child.children?.length" :index="String(child.id)">
            <template #title>
              <el-icon><component :is="resolveIcon(child.icon || 'Document')" /></el-icon>
              <span>{{ child.name }}</span>
            </template>
            <el-menu-item
              v-for="grandChild in child.children"
              :key="grandChild.id"
              :index="grandChild.path"
            >
              <el-icon><component :is="resolveIcon(grandChild.icon || 'Document')" /></el-icon>
              <span>{{ grandChild.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="child.path">
            <el-icon><component :is="resolveIcon(child.icon || 'Document')" /></el-icon>
            <span>{{ child.name }}</span>
          </el-menu-item>
        </template>
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
import { 
  Document, Grid, House, Key, Setting, User, Box, Goods, Plus, Camera, Search,
  DataBoard, TrendCharts, List, Upload, Download, Refresh
} from '@element-plus/icons-vue'
import { watch } from 'vue'

const props = defineProps({
  menus: {
    type: Array,
    required: true
  }
})

const route = useRoute()

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
  User,
  Box,
  Goods,
  Plus,
  Camera,
  Search,
  DataBoard,
  TrendCharts,
  List,
  Upload,
  Download,
  Refresh
}

function resolveIcon(name) {
  return iconMap[name] || Document
}
</script>