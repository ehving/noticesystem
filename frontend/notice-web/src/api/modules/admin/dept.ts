import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type {DeptAdminVo, DeptTreeVo} from '@/types/models/dept-admin'
import type {DeptOptionVo} from "@/types/models/dept.ts";

const BASE_URL = '/admin/dept'

const currentDb = () => useAdminStore().activeDb

export const listDepts = (params?: { name?: string; status?: number }): Promise<DeptAdminVo[]> => {
  return http.get<DeptAdminVo[]>(`${BASE_URL}/list`, { params: { ...params, db: currentDb() } })
}

export const getDept = (id: string): Promise<DeptAdminVo> => {
  return http.get<DeptAdminVo>(`${BASE_URL}/${id}`, { params: { db: currentDb() } })
}

export const getDeptTree = (): Promise<DeptTreeVo[]> => {
  return http.get<DeptTreeVo[]>(`${BASE_URL}/tree`, {
    params: { db: currentDb() },
  })
}

export const createDept = (data: DeptAdminVo): Promise<DeptAdminVo> => {
  return http.post<DeptAdminVo>(`${BASE_URL}`, data, { params: { db: currentDb() } })
}

export const updateDept = (id: string, data: DeptAdminVo): Promise<DeptAdminVo> => {
  return http.put<DeptAdminVo>(`${BASE_URL}/${id}`, data, { params: { db: currentDb() } })
}

export const deleteDept = (id: string): Promise<string> => {
  return http.delete<string>(`${BASE_URL}/${id}`, { params: { db: currentDb() } })
}

export const updateDeptStatus = (id: string, status: number): Promise<string> => {
  return http.put<string>(`${BASE_URL}/${id}/status`, undefined, { params: { db: currentDb(), status } })
}

export const getDeptParentOptions= (params?: { ChildDeptId?: string|undefined }):Promise<DeptOptionVo[]> => {
  return http.get<DeptOptionVo[]>(`${BASE_URL}/parent-options`, {
    params: { ...params, db: currentDb() }
  });
}



