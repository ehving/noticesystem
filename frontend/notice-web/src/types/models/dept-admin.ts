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

export interface DeptTreeVo {
  id: string
  name: string
  parentId?: string | null
  sortOrder?: number | null
  status?: number | null
  children?: DeptTreeVo[]
}

