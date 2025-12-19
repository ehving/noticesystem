import http from '@/api/http'
import type { DeptAdminVo } from '@/types/models/dept-admin'

export const listDepts = (params?: { name?: string; status?: number }): Promise<DeptAdminVo[]> => {
  return http.get<DeptAdminVo[]>('/admin/dept/list', { params })
}

export const getDept = (id: string): Promise<DeptAdminVo> => {
  return http.get<DeptAdminVo>(`/admin/dept/${id}`)
}

export const createDept = (data: DeptAdminVo): Promise<DeptAdminVo> => {
  return http.post<DeptAdminVo>('/admin/dept', data)
}

export const updateDept = (id: string, data: DeptAdminVo): Promise<DeptAdminVo> => {
  return http.put<DeptAdminVo>(`/admin/dept/${id}`, data)
}

export const deleteDept = (id: string): Promise<string> => {
  return http.delete<string>(`/admin/dept/${id}`)
}

export const updateDeptStatus = (id: string, status: number): Promise<string> => {
  return http.put<string>(`/admin/dept/${id}/status`, undefined, { params: { status } })
}
