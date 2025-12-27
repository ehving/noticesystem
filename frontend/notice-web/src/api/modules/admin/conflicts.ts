import http from '@/api/http'
import { useAdminStore } from '@/stores/admin'
import type { MpPage } from '@/types/page'
import type {
  SyncConflictListVo,
  SyncConflictDetailVo,
  SyncConflictQuery,
  SyncConflictResolvePayload,
} from '@/types/models/sync-conflict'
import type { DatabaseType } from '@/types/enums/db'
import type {AggVo} from "@/types/models/agg.ts";
import type {SyncConflictAggBy} from "@/types/enums/agg-by.ts";

const BASE_URL = '/admin/sync-conflicts'
const currentDb = () => useAdminStore().activeDb

export const pageConflicts = (query: SyncConflictQuery): Promise<MpPage<SyncConflictListVo>> => {
  return http.post<MpPage<SyncConflictListVo>>(`${BASE_URL}/page`, query, { params: { db: currentDb() } })
}

export const getConflictDetail = (conflictId: string): Promise<SyncConflictDetailVo> => {
  return http.get<SyncConflictDetailVo>(`${BASE_URL}/${conflictId}`, { params: { db: currentDb() } })
}

export const resolveConflict = (conflictId: string, payload: SyncConflictResolvePayload): Promise<string> => {
  return http.put<string>(`${BASE_URL}/${conflictId}/resolve`, payload, {params: { db: currentDb() }, })// 冲突单所在库
}

export const ignoreConflict = (conflictId: string, note?: string): Promise<string> => {
  return http.put<string>(`${BASE_URL}/${conflictId}/ignore`, { note }, { params: { db: currentDb() } })
}

export const reopenConflict = (conflictId: string, note?: string): Promise<string> => {
  return http.put<string>(`${BASE_URL}/${conflictId}/reopen`, { note }, { params: { db: currentDb() } })
}

export const recheckConflict = (conflictId: string): Promise<string> => {
  return http.put<string>(`${BASE_URL}/${conflictId}/recheck`, undefined, { params: { db: currentDb() } })
}

export const runRecheckOpen = (limit?: number): Promise<number> => {
  return http.post<number>(`${BASE_URL}/tasks/recheck-open`, undefined, { params: { db: currentDb(), limit } })
}

export const runNotify = (limit?: number): Promise<number> => {
  return http.post<number>(`${BASE_URL}/tasks/notify`, undefined, { params: { db: currentDb(), limit } })
}

export const resolveByDb = (conflictId: string, sourceDb: DatabaseType, note?: string): Promise<string> => {
  return resolveConflict(conflictId, { sourceDb, note })
}

export const aggConflicts = (
  begin: string | undefined,
  end: string | undefined,
  by: SyncConflictAggBy,
  filter?: Partial<SyncConflictQuery>
): Promise<AggVo[]> => {
  return http.post<AggVo[]>(
    `${BASE_URL}/agg`,
    filter ?? {}, //  body：SyncConflictQueryVo（可空，后端 @RequestBody(required=false)）
    {
      params: {
        db: currentDb(), //  query
        by,              //  query
        begin,           //  query（注意名字是 begin/end）
        end,
      },
    }
  )
}


