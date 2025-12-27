export interface UserProfileVo {
  id: string
  username: string
  nickname: string | null
  email: string | null
  phone: string | null
  avatar: string | null
  status: number
  roleId: string | null
  roleName: string | null
  deptId: string | null
  deptName: string | null
  lastLoginTime: string | null
  createTime: string | null
  updateTime: string | null
}

