import type { DatabaseType } from '../enums/db'
import type { SyncEntityType, SyncLogStatus } from '../enums/sync'

export enum SyncAction {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE'
}

export interface SyncLog {
  id: string
  entityType: SyncEntityType
  entityId: string
  action: SyncAction
  sourceDb: DatabaseType
  targetDb: DatabaseType
  status: SyncLogStatus
  errorMsg: string
  retryCount: number
  createTime: string
  updateTime: string
}

export interface SyncLogVo {
  pageNo?: number
  pageSize?: number
  entityType?: SyncEntityType
  entityId?: string
  action?: SyncAction
  sourceDb?: DatabaseType
  targetDb?: DatabaseType
  status?: string
  beginTime?: string
  endTime?: string
}

export interface SyncLogDailyReportVo {
  statDate: string
  sourceDb: DatabaseType
  targetDb: DatabaseType
  totalCount: number
  successCount: number
  failedCount: number
  conflictCount: number
  errorCount: number
}

