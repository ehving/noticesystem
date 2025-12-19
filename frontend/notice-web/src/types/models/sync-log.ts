import type { DatabaseType } from '@/types/enums/db'

export type SyncStatus = 'SUCCESS' | 'FAILED' | string
export type SyncAction = string
export type SyncEntityType = string

export interface SyncLog {
  id: string
  entityType: SyncEntityType
  entityId?: string
  action: SyncAction
  sourceDb: DatabaseType | string
  targetDb: DatabaseType | string
  status: SyncStatus
  retryCount?: number
  errorMsg?: string
  createTime?: string
  updateTime?: string
}

export interface SyncLogVo {
  pageNo: number
  pageSize: number
  entityType?: SyncEntityType
  entityId?: string
  action?: SyncAction
  sourceDb?: DatabaseType | 'ALL'
  targetDb?: DatabaseType | 'ALL'
  status?: SyncStatus
  beginTime?: string
  endTime?: string
}

export interface SyncLogDailyReportVo {
  statDate: string
  sourceDb: string
  targetDb: string
  totalCount: number
  successCount: number
  failedCount: number
  failedRate: number
}
