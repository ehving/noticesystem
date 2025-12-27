import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { MpPage } from '@/types/page'
import type { NoticeAdmin, NoticeAdminPageVo, NoticeAdminSaveVo,NoticeAdminRowVo, NoticeTargetsResp } from '@/types/models/notice-admin'

const currentDb = () => useAdminStore().activeDb

// 分页
export const pageAdminNotices = (data: NoticeAdminPageVo): Promise<MpPage<NoticeAdmin>> => {
  return http.post<MpPage<NoticeAdmin>>('/admin/notices/page', data, { params: { db: currentDb() } })
}

// 新建（只会创建草稿，publishTime 可选表示“定时”）
export const createNotice = (data: NoticeAdminSaveVo): Promise<string> => {
  return http.post<string>('/admin/notices', { ...data, sourceDb: currentDb() })
}

// 更新（只允许草稿/撤回更新）
export const updateNotice = (id: string, data: NoticeAdminSaveVo): Promise<string> => {
  return http.put<string>(`/admin/notices/${id}`, { ...data, sourceDb: currentDb() })
}

//  发布（立即发布）
export const publishNotice = (id: string): Promise<string> => {
  return http.post<string>(`/admin/notices/${id}/publish`, undefined, { params: { db: currentDb() } })
}

//  撤回（你后端如果是 /recall 且参数名 db，就这样写）
export const recallNotice = (id: string): Promise<string> => {
  return http.post<string>(`/admin/notices/${id}/recall`, undefined, { params: { db: currentDb() } })
}

//  删除公告
export const deleteNotice = (id: string): Promise<string> => {
  return http.delete<string>(`/admin/notices/${id}`, {
    params: { db: currentDb() },
  })
}

// 带范围的分页（新接口）
export const pageAdminNoticesWithScope = (
  data: NoticeAdminPageVo
): Promise<MpPage<NoticeAdminRowVo>> => {
  return http.post<MpPage<NoticeAdminRowVo>>('/admin/notices/pageWithScope', data, {
    params: { db: currentDb() },
  })
}

// 查询某条公告的完整可见范围（弹窗用）
export const getNoticeTargets = (noticeId: string): Promise<NoticeTargetsResp> => {
  return http.get<NoticeTargetsResp>(`/admin/notices/${noticeId}/targets`, {
    params: { db: currentDb() },
  })
}
