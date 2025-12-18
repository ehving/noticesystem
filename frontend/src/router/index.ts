import {
  createRouter,
  createWebHistory,
  type NavigationGuardNext,
  type RouteLocationNormalized,
  type RouteRecordNormalized,
} from 'vue-router';
import { useAuthStore } from '../stores/auth';
import AdminLayout from '../layouts/AdminLayout.vue';
import UserLayout from '../layouts/UserLayout.vue';
import AdminDeptsView from '../views/admin/AdminDeptsView.vue';
import AdminNoticesView from '../views/admin/AdminNoticesView.vue';
import AdminSyncLogsView from '../views/admin/AdminSyncLogsView.vue';
import AdminUsersView from '../views/admin/AdminUsersView.vue';
import LoginView from '../views/LoginView.vue';
import NoticeDetailView from '../views/NoticeDetailView.vue';
import NoticesView from '../views/NoticesView.vue';
import ProfileView from '../views/ProfileView.vue';
import RegisterView from '../views/RegisterView.vue';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    requiresAdmin?: boolean;
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: LoginView,
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: RegisterView,
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: UserLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/notices',
        },
        {
          path: 'notices',
          name: 'Notices',
          component: NoticesView,
          meta: { requiresAuth: true },
        },
        {
          path: 'notices/:id',
          name: 'NoticeDetail',
          component: NoticeDetailView,
          meta: { requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: ProfileView,
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: '/admin/notices',
        },
        {
          path: 'notices',
          name: 'AdminNotices',
          component: AdminNoticesView,
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: AdminUsersView,
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'depts',
          name: 'AdminDepts',
          component: AdminDeptsView,
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'sync-logs',
          name: 'AdminSyncLogs',
          component: AdminSyncLogsView,
          meta: { requiresAuth: true, requiresAdmin: true },
        },
      ],
    },
  ],
});

const redirectToLogin = (to: RouteLocationNormalized, next: NavigationGuardNext): void => {
  next({ path: '/login', query: { redirect: to.fullPath } });
};

router.beforeEach(async (to: RouteLocationNormalized, _from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const authStore = useAuthStore();
  const requiresAuth = to.matched.some((record: RouteRecordNormalized) => record.meta.requiresAuth !== false);
  const requiresAdmin = to.matched.some((record: RouteRecordNormalized) => record.meta.requiresAdmin);

  const hasToken = Boolean(authStore.token);

  if (requiresAuth && !hasToken) {
    redirectToLogin(to, next);
    return;
  }

  if (hasToken && !authStore.profile) {
    try {
      await authStore.fetchProfile();
    } catch (error) {
      authStore.logout();
      redirectToLogin(to, next);
      return;
    }
  }

  if (requiresAdmin && authStore.profile?.roleName !== '管理员') {
    window.alert('仅管理员可以访问管理端');
    next('/');
    return;
  }

  next();
});

export default router;
