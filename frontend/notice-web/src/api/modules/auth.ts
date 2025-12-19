import http from '@/api/http'
import type { UserProfileVo } from '@/types/models/user'

export const login = (data: { username: string; password: string }): Promise<string> => {
  return http.post<string>('/user/login', data)
}

export const register = (data: {
  username: string
  password: string
  deptId?: string
  nickname?: string
  email?: string
  phone?: string
}): Promise<string> => {
  return http.post<string>('/user/register', data)
}

export const getProfile = (): Promise<UserProfileVo> => {
  return http.get<UserProfileVo>('/user/profile')
}

export const updateProfile = (data: {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}): Promise<string> => {
  return http.put<string>('/user/profile', data)
}

export const updatePassword = (data: { oldPassword: string; newPassword: string }): Promise<string> => {
  return http.put<string>('/user/password', data)
}
