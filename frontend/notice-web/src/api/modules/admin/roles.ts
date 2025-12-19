import http from '@/api/http'
import type { Role } from '@/types/models/role'

export const listRoles = (): Promise<Role[]> => {
  return http.get<Role[]>('/admin/roles')
}
