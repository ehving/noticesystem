export type DatabaseType = 'MYSQL' | 'PG' | 'SQLSERVER';

export type SyncAction = 'CREATE' | 'UPDATE' | 'DELETE';

export type SyncEntityType =
  | 'USER'
  | 'ROLE'
  | 'DEPT'
  | 'NOTICE'
  | 'NOTICE_READ'
  | 'NOTICE_TARGET_DEPT'
  | 'SYNC_LOG'
  | (string & {});

export type NoticeStatus = 'DRAFT' | 'PUBLISHED' | 'RECALLED';

export type NoticeLevel = 'NORMAL' | 'IMPORTANT' | 'URGENT';
