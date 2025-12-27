import type { ConflictStatus, ConflictType, SyncEntityType } from '@/types/enums/sync'
import type { DatabaseType } from '@/types/enums/db'
import type { SyncConflictItem } from './sync-conflict-item'

export interface SyncConflictListVo {
  id: string
  entityType: SyncEntityType
  entityId: string
  status: ConflictStatus
  conflictType: ConflictType
  firstSeenAt?: string
  lastSeenAt?: string
  lastCheckedAt?: string
  lastNotifiedAt?: string
  notifyCount?: number
  resolutionSourceDb?: DatabaseType
  resolutionNote?: string
  resolvedAt?: string
  items?: SyncConflictItem[]
}

export interface SyncConflictDetailVo {
  id: string
  entityType: SyncEntityType
  entityId: string
  status: ConflictStatus
  conflictType: ConflictType
  firstSeenAt?: string
  lastSeenAt?: string
  lastCheckedAt?: string
  lastNotifiedAt?: string
  notifyCount?: number
  resolutionSourceDb?: DatabaseType
  resolutionNote?: string
  resolvedAt?: string
  createTime?: string
  updateTime?: string
  items: SyncConflictItem[]
}

export interface SyncConflictQuery {
  pageNo?: number
  pageSize?: number
  beginTime?: string
  endTime?: string
  entityType?: SyncEntityType
  status?: ConflictStatus | string
  conflictType?: ConflictType | string
  sourceDb?: DatabaseType
  limitToOpenOnly?: boolean
}

export interface SyncConflictResolvePayload {
  sourceDb: DatabaseType
  note?: string
}


