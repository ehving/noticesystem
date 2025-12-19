import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { NoticeReadUserVo } from '@/types/models/notice-read'
import type { MpPage } from '@/types/page'

const currentDb = () => useAdminStore().activeDb

export const getReadCount = (noticeId: string): Promise<number> => {
  return http.get<number>(`/admin/notices/${noticeId}/read-count`, { params: { db: currentDb() } })
}

export const pageNoticeReads = (
  noticeId: string,
  params: { pageNo: number; pageSize: number }
): Promise<MpPage<NoticeReadUserVo>> => {
  return http.get<MpPage<NoticeReadUserVo>>(`/admin/notices/${noticeId}/reads`, {
    params: { ...params, db: currentDb() },
  })
}
