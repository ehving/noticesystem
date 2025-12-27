export enum DatabaseType {
  MYSQL = 'MYSQL',
  PG = 'PG',
  SQLSERVER = 'SQLSERVER'
}

export const DatabaseTypeMap: Record<DatabaseType, string> = {
  [DatabaseType.MYSQL]: 'MySQL',
  [DatabaseType.PG]: 'PostgreSQL',
  [DatabaseType.SQLSERVER]: 'SQL Server'
}

export const DB_OPTIONS = Object.values(DatabaseType).map((db) => ({
  value: db,
  label: DatabaseTypeMap[db]
}))