export enum SyncLogStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  CONFLICT = 'CONFLICT',
  ERROR = 'ERROR',
}

export enum ConflictStatus {
  OPEN = 'OPEN',
  RESOLVED = 'RESOLVED',
  IGNORED = 'IGNORED',
}

export enum ConflictType {
  MISSING = 'MISSING',
  MISMATCH = 'MISMATCH',
}

export enum SyncEntityType {
  USER = 'USER',
  ROLE = 'ROLE',
  DEPT = 'DEPT',
  NOTICE = 'NOTICE',
  NOTICE_TARGET_DEPT = 'NOTICE_TARGET_DEPT',
  NOTICE_READ = 'NOTICE_READ',
  SYNC_LOG = 'SYNC_LOG',
  SYNC_CONFLICT = 'SYNC_CONFLICT',
  SYNC_CONFLICT_ITEM = 'SYNC_CONFLICT_ITEM',
}
