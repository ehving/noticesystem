import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { Role } from '@/types/models/role'

const currentDb = () => useAdminStore().activeDb

export const listRoles = (): Promise<Role[]> => {
  return http.get<Role[]>('/admin/roles', { params: { db: currentDb() } })
}
