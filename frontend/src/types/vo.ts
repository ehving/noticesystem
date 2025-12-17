import type { DatabaseType, NoticeLevel, NoticeStatus, SyncAction, SyncEntityType } from './enums';

export interface DeptOptionVo {
  id: string;
  name: string;
  parentId: string;
}

export interface DeptTreeVo {
  id: string;
  name: string;
  parentId: string;
  sortOrder: number;
  status: number;
  children: DeptTreeVo[];
}

export interface DeptVo {
  id: string;
  name: string;
  parentId: string;
  parentName: string;
  description: string;
  sortOrder: number;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface UserLoginVo {
  username: string;
  password: string;
}

export interface UserRegisterVo {
  username: string;
  password: string;
  deptId: string;
  nickname: string;
  email: string;
  phone: string;
}

export interface UserProfileVo {
  id: string;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  avatar: string;
  status: number;
  roleId: string;
  roleName: string;
  deptId: string;
  deptName: string;
  lastLoginTime: string;
  createTime: string;
  updateTime: string;
}

export interface UserUpdateProfileVo {
  nickname: string;
  email: string;
  phone: string;
  avatar: string;
}

export interface UserUpdatePasswordVo {
  oldPassword: string;
  newPassword: string;
}

export interface UserAdminPageVo {
  pageNo?: number;
  pageSize?: number;
  keyword?: string;
  roleId?: string;
  deptId?: string;
  status?: number;
}

export interface UserAdminCreateVo {
  username: string;
  password: string;
  nickname: string;
  email: string;
  phone: string;
  avatar: string;
  roleId: string;
  deptId: string;
  status?: number;
  sourceDb?: DatabaseType;
}

export interface UserAdminUpdateVo {
  nickname: string;
  email: string;
  phone: string;
  avatar: string;
  roleId: string;
  deptId: string;
  status?: number;
  sourceDb?: DatabaseType;
}

export interface UserAdminResetPasswordVo {
  newPassword: string;
  sourceDb?: DatabaseType;
}

export interface UserAdminListVo {
  id: string;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  status: number;
  roleId: string;
  roleName: string;
  deptId: string;
  deptName: string;
  lastLoginTime: string;
  createTime: string;
}

export interface NoticeAdminPageVo {
  pageNo?: number;
  pageSize?: number;
  keyword?: string;
  status?: NoticeStatus;
  level?: NoticeLevel;
  publisherId?: string;
  startTime?: string;
  endTime?: string;
}

export interface NoticeAdminSaveVo {
  id?: string;
  title: string;
  content: string;
  level: NoticeLevel;
  status: NoticeStatus;
  publishTime?: string;
  expireTime?: string;
  targetDeptIds: string[];
  sourceDb?: DatabaseType;
}

export interface SyncLogVo {
  pageNo?: number;
  pageSize?: number;
  entityType?: SyncEntityType;
  entityId?: string;
  action?: SyncAction;
  sourceDb?: DatabaseType;
  targetDb?: DatabaseType;
  status?: string;
  beginTime?: string;
  endTime?: string;
}

export interface SyncLogDailyReportVo {
  statDate: string;
  sourceDb: string;
  targetDb: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  failedRate: number;
}

export interface NoticeReadUserVo {
  userId: string;
  username: string;
  nickname: string;
  deptId: string;
  deptName: string;
  readTime: string;
  deviceType: string;
}
