export type NoticeLevel = 'NORMAL' | 'IMPORTANT' | 'URGENT'

export interface Notice {
  id: string
  title: string
  content: string
  level: NoticeLevel
  status?: string
  publisherId?: string
  publishTime?: string
  expireTime?: string
  viewCount?: number
  createTime?: string
  updateTime?: string
}
