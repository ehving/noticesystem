import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { MpPage } from '@/types/page'
import type {
  UserAdminListVo,
  UserAdminPageVo,
  UserAdminCreateVo,
  UserAdminUpdateVo,
  UserAdminResetPasswordVo,
} from '@/types/models/user-admin'

const withDb = () => {
  const adminStore = useAdminStore()
  return adminStore.activeDb
}

export const pageUsers = (params: UserAdminPageVo): Promise<MpPage<UserAdminListVo>> => {
  return http.post<MpPage<UserAdminListVo>>('/admin/users/page', params, { params: { db: withDb() } })
}

export const getUserDetail = (id: string): Promise<UserAdminListVo> => {
  return http.get<UserAdminListVo>(`/admin/users/${id}`, { params: { db: withDb() } })
}

export const createUser = (data: UserAdminCreateVo): Promise<string> => {
  return http.post<string>('/admin/users', { ...data, sourceDb: withDb() })
}

export const updateUser = (id: string, data: UserAdminUpdateVo): Promise<string> => {
  return http.put<string>(`/admin/users/${id}`, { ...data, sourceDb: withDb() })
}

export const resetPassword = (id: string, data: UserAdminResetPasswordVo): Promise<string> => {
  return http.put<string>(`/admin/users/${id}/reset-password`, { ...data, sourceDb: withDb() })
}

export const deleteUser = (id: string): Promise<string> => {
  return http.delete<string>(`/admin/users/${id}`, { params: { sourceDb: withDb() } })
}
