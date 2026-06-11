import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'system/user',
        name: 'user-manage',
        component: () => import('../views/UserManageView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'role-manage',
        component: () => import('../views/RoleManageView.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'tabs',
        name: 'tabs-demo',
        component: () => import('../views/InfoCenterView.vue'),
        meta: { title: '标签页示例' }
      },
      // ==================== WMS入库管理模块 ====================
      {
        path: 'wms',
        name: 'wms',
        meta: { title: 'WMS入库管理' },
        children: [
          {
            path: 'supplier',
            name: 'supplier-manage',
            component: () => import('../views/wms/SupplierManage.vue'),
            meta: { title: '供应商管理' }
          },
          {
            path: 'part',
            name: 'part-manage',
            component: () => import('../views/wms/PartManage.vue'),
            meta: { title: '零件管理' }
          },
          {
            path: 'inbound-order/list',
            name: 'inbound-order-list',
            component: () => import('../views/wms/InboundOrderList.vue'),
            meta: { title: '入库单列表' }
          },
          {
            path: 'inbound-order/create',
            name: 'inbound-order-create',
            component: () => import('../views/wms/InboundOrderForm.vue'),
            meta: { title: '创建入库单' }
          },
          {
            path: 'inbound-order/edit/:orderNo',
            name: 'inbound-order-edit',
            component: () => import('../views/wms/InboundOrderForm.vue'),
            meta: { title: '编辑入库单' }
          },
          {
            path: 'scan',
            name: 'scan-inbound',
            component: () => import('../views/wms/ScanInbound.vue'),
            meta: { title: '扫码入库' }
          },
          {
            path: 'trace',
            name: 'inventory-trace',
            component: () => import('../views/wms/InventoryTrace.vue'),
            meta: { title: '库存追溯' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login' && isAuthenticated()) {
    return '/dashboard'
  }

  return true
})

export default router