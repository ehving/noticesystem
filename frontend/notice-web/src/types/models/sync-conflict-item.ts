import type { DatabaseType } from '@/types/enums/db'

export interface SyncConflictItem {
  id: string
  dbType: DatabaseType
  existsFlag: number
  rowHash: string
  rowVersion: string
  rowUpdateTime: string
  lastCheckedAt: string
  rowJson?: string//新增,储存这个冲突在这个类型的数据库里的具体json
}
