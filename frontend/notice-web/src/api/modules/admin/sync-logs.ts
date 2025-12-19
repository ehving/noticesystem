import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { MpPage } from '@/types/page'
import type { SyncLog, SyncLogVo, SyncLogDailyReportVo, SyncAction, SyncEntityType, SyncStatus } from '@/types/models/sync-log'
import type { DatabaseType } from '@/types/enums/db'

const currentDb = () => useAdminStore().activeDb

export const pageSyncLogs = (vo: SyncLogVo): Promise<MpPage<SyncLog>> => {
  return http.post<MpPage<SyncLog>>('/admin/sync-logs/page', vo, { params: { db: currentDb() } })
}

export const getSyncLogDetail = (id: string): Promise<SyncLog> => {
  return http.get<SyncLog>(`/admin/sync-logs/${id}`, { params: { db: currentDb() } })
}

export const retrySyncLog = (id: string): Promise<string> => {
  return http.post<string>(`/admin/sync-logs/${id}/retry`, undefined, { params: { db: currentDb() } })
}

export const cleanSyncLogs = (db: DatabaseType | 'ALL', retainDays?: number, maxCount?: number): Promise<string> => {
  return http.post<string>('/admin/sync-logs/clean', undefined, { params: { db, retainDays, maxCount } })
}

export const listStatuses = (): Promise<SyncStatus[]> => {
  return http.get<SyncStatus[]>('/admin/sync-logs/statuses')
}

export const listActions = (): Promise<SyncAction[]> => {
  return http.get<SyncAction[]>('/admin/sync-logs/actions')
}

export const listEntityTypes = (): Promise<SyncEntityType[]> => {
  return http.get<SyncEntityType[]>('/admin/sync-logs/entity-types')
}

export const dailyReport = (vo: SyncLogVo): Promise<SyncLogDailyReportVo[]> => {
  return http.post<SyncLogDailyReportVo[]>('/admin/sync-logs/daily-report', vo, { params: { db: currentDb() } })
}
