export interface UserAdminListVo {
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  status?: number
  roleId?: string
  roleName?: string
  deptId?: string
  deptName?: string
  lastLoginTime?: string
  createTime?: string
}

export interface UserAdminPageVo {
  pageNo: number
  pageSize: number
  keyword?: string
  roleId?: string
  deptId?: string
  status?: number
}

export interface UserAdminCreateVo {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roleId?: string
  deptId?: string
  status?: number
  sourceDb?: string
}

export interface UserAdminUpdateVo {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  roleId?: string
  deptId?: string
  status?: number
  sourceDb?: string
}

export interface UserAdminResetPasswordVo {
  newPassword: string
  sourceDb?: string
}
