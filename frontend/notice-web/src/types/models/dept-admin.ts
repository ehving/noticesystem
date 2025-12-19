export interface DeptAdminVo {
  id?: string
  name: string
  parentId?: string | null
  parentName?: string
  description?: string
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
}
