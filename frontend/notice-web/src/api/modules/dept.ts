import http from '@/api/http'
import type { DeptOptionVo } from '@/types/models/dept'

export const listDeptOptions = (): Promise<DeptOptionVo[]> => {
  return http.get<DeptOptionVo[]>('/dept/options')
}
