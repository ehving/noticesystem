import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { MpPage } from '@/types/page'
import type { NoticeAdmin, NoticeAdminPageVo, NoticeAdminSaveVo } from '@/types/models/notice-admin'

const currentDb = () => useAdminStore().activeDb

export const pageAdminNotices = (data: NoticeAdminPageVo): Promise<MpPage<NoticeAdmin>> => {
  return http.post<MpPage<NoticeAdmin>>('/admin/notices/page', data, { params: { db: currentDb() } })
}

export const createNotice = (data: NoticeAdminSaveVo): Promise<string> => {
  return http.post<string>('/admin/notices', { ...data, sourceDb: currentDb() })
}

export const updateNotice = (id: string, data: NoticeAdminSaveVo): Promise<string> => {
  return http.put<string>(`/admin/notices/${id}`, { ...data, sourceDb: currentDb() })
}

export const recallNotice = (id: string): Promise<string> => {
  return http.post<string>(`/admin/notices/${id}/recall`, undefined, { params: { sourceDb: currentDb() } })
}
