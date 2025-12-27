import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { MpPage } from '@/types/page'
import type { SyncLog, SyncLogVo, SyncLogDailyReportVo, SyncAction } from '@/types/models/sync-log'
import type {SyncEntityType, SyncLogStatus} from '@/types/enums/sync.ts'
import type { DatabaseType } from '@/types/enums/db'
import type {SyncLogAggBy} from "@/types/enums/agg-by.ts";
import type {AggVo} from "@/types/models/agg.ts";

const BASE_URL ='/admin/sync-logs'

const currentDb = () => useAdminStore().activeDb

export const pageSyncLogs = (vo: SyncLogVo): Promise<MpPage<SyncLog>> => {
  return http.post<MpPage<SyncLog>>(`${BASE_URL}/page`, vo, { params: { db: currentDb() } })
}

export const getSyncLogDetail = (id: string): Promise<SyncLog> => {
  return http.get<SyncLog>(`${BASE_URL}/${id}`, { params: { db: currentDb() } })
}

export const retrySyncLog = (id: string): Promise<string> => {
  return http.post<string>(`${BASE_URL}/${id}/retry`, undefined, { params: { db: currentDb() } })
}

export const cleanSyncLogs = (db: DatabaseType | 'ALL', retainDays?: number, maxCount?: number): Promise<string> => {
  return http.post<string>(`${BASE_URL}/clean`, undefined, { params: { db, retainDays, maxCount } })
}

export const listStatuses = (): Promise<SyncLogStatus[]> => {
  return http.get<SyncLogStatus[]>(`${BASE_URL}/statuses`)
}

export const listActions = (): Promise<SyncAction[]> => {
  return http.get<SyncAction[]>(`${BASE_URL}/actions`)
}

export const listEntityTypes = (): Promise<SyncEntityType[]> => {
  return http.get<SyncEntityType[]>(`${BASE_URL}/entity-types`)
}

export const dailyReport = (vo: SyncLogVo): Promise<SyncLogDailyReportVo[]> => {
  return http.post<SyncLogDailyReportVo[]>(`${BASE_URL}/daily-report`, vo, { params: { db: currentDb() } })
}

export const aggLogs = (
  begin: string | undefined,
  end: string | undefined,
  by: SyncLogAggBy,
  filter?: Partial<SyncLogVo>
): Promise<AggVo[]> => {
  return http.post<AggVo[]>(
    `${BASE_URL}/agg`,
    filter ?? {}, // ✅ body：SyncLogVo（可空）
    {
      params: {
        db: currentDb(),
        by,
        begin,
        end,
      },
    }
  )
}

