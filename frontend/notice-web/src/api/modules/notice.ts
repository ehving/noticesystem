import http from '@/api/http'
import type { Notice } from '@/types/models/notice'
import type { MpPage } from '@/types/page'

export const pageNotices = (params: {
  pageNo: number
  pageSize: number
  keyword?: string
  level?: string
}): Promise<MpPage<Notice>> => {
  return http.get<MpPage<Notice>>('/notices/page', { params })
}

export const getNoticeDetail = (id: string): Promise<Notice> => {
  return http.get<Notice>(`/notices/${id}`)
}
