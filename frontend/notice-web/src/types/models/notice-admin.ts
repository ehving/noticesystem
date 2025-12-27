import type {DatabaseType} from "@/types/enums/db.ts";
import type {NoticeStatus} from "@/types/enums/notice.ts";
import type {NoticeLevel} from "@/types/enums/notice.ts";

export type NoticeScopeType = 'GLOBAL' | 'DEPT'

export interface NoticeAdmin {
  id: string
  title: string
  content: string
  level: NoticeLevel
  status: NoticeStatus
  publisherId?: string
  publishTime?: string
  expireTime?: string
  viewCount?: number
  createTime?: string
  updateTime?: string
}

export interface NoticeAdminPageVo {
  pageNo: number
  pageSize: number
  keyword?: string
  status?: NoticeStatus
  level?: NoticeLevel
  publisherId?: string
  startTime?: string
  endTime?: string
}

export interface NoticeAdminSaveVo {
  id?: string
  title: string
  content: string
  level: NoticeLevel
  status: NoticeStatus
  publishTime?: string
  expireTime?: string
  targetDeptIds?: string[]
  sourceDb?: DatabaseType
}

/** 后端 NoticeAdminRowVo */
export interface NoticeAdminRowVo {
  notice: NoticeAdmin
  scopeType: NoticeScopeType
  targetDeptCount: number
  targetDeptNamesPreview: string[]
  targetDeptIds: string[]
}

/** 后端 /{noticeId}/targets 返回 */
export interface NoticeTargetsResp {
  scopeType: NoticeScopeType
  deptIds: string[]
  depts: Array<{ id: string; name: string }>
}
