import type { RouteRecordRaw } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
      roles: ['管理员'],
      title: '管理端',
      desktopOnly: true,
    },
    children: [
      {
        path: '',
        redirect: '/admin/reports',
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/Users.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '用户管理', desktopOnly: true },
      },
      {
        path: 'depts',
        name: 'admin-depts',
        component: () => import('@/views/admin/Depts.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '部门管理', desktopOnly: true },
      },
      {
        path: 'roles',
        name: 'admin-roles',
        component: () => import('@/views/admin/Roles.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '角色列表', desktopOnly: true },
      },
      {
        path: 'notices',
        name: 'admin-notices',
        component: () => import('@/views/admin/Notices.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '公告管理', desktopOnly: true },
      },
      {
        path: 'sync-logs',
        name: 'admin-sync-logs',
        component: () => import('@/views/admin/SyncLogs.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '同步日志', desktopOnly: true },
      },
      {
        path: 'reports',
        name: 'admin-reports',
        component: () => import('@/views/admin/Reports.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '可视化报表', desktopOnly: true },
      },
      {
        path: 'conflicts',
        name: 'admin-conflicts',
        component: () => import('@/views/admin/Conflicts.vue'),
        meta: { requiresAuth: true, roles: ['管理员'], title: '冲突管理', desktopOnly: true },
      },
    ],
  },
]
