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
      // WMS入库管理
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
          }
        ]
      },
      // WMS出库管理
      {
        path: 'wms-outbound',
        name: 'wms-outbound',
        meta: { title: 'WMS出库管理' },
        children: [
          {
            path: 'outbound-order/list',
            name: 'outbound-order-list',
            component: () => import('../views/wms/OutboundOrderList.vue'),
            meta: { title: '出库单列表' }
          },
          {
            path: 'outbound-order/create',
            name: 'outbound-order-create',
            component: () => import('../views/wms/OutboundOrderForm.vue'),
            meta: { title: '创建出库单' }
          },
          {
            path: 'outbound-order/edit/:orderNo',
            name: 'outbound-order-edit',
            component: () => import('../views/wms/OutboundOrderForm.vue'),
            meta: { title: '编辑出库单' }
          },
          {
            path: 'scan',
            name: 'scan-outbound',
            component: () => import('../views/wms/ScanOutbound.vue'),
            meta: { title: '扫码出库' }
          }
        ]
      },
      // 实时库存总览
      {
        path: 'current-inventory',
        name: 'current-inventory',
        component: () => import('../views/wms/CurrentInventory.vue'),
        meta: { title: '实时库存总览' }
      },
      // 库存追溯 - 2个子页面
      {
        path: 'inventory-trace',
        name: 'inventory-trace',
        component: () => import('../views/wms/InventoryTrace.vue'),
        meta: { title: '库存追溯' }
      },
      {
        path: 'inventory-trace/lifecycle',
        name: 'inventory-trace-lifecycle',
        component: () => import('../views/wms/InventoryTraceLifecycle.vue'),
        meta: { title: '看板生命周期' }
      },
      // 转包管理
      {
        path: 'transfer/list',
        name: 'transfer-list',
        component: () => import('../views/wms/TransferOrderList.vue'),
        meta: { title: '转包管理' }
      },
      // 封存管理
      {
        path: 'sealed-kanban',
        name: 'sealed-kanban',
        component: () => import('../views/wms/SealedKanbanList.vue'),
        meta: { title: '封存看板管理' }
      },
      // AI需求预测
      {
        path: 'ai-predict',
        name: 'ai-predict',
        component: () => import('../views/wms/AIPredictDashboard.vue'),
        meta: { title: 'AI需求预测' }
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
