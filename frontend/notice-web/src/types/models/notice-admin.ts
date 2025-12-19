export type NoticeStatus = 'DRAFT' | 'PUBLISHED' | 'RECALLED'
export type NoticeLevel = 'NORMAL' | 'IMPORTANT' | 'URGENT'

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
  sourceDb?: string
}
