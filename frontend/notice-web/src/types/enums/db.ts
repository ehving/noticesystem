export type DatabaseType = 'MYSQL' | 'PG' | 'SQLSERVER'

export const DB_OPTIONS: { label: string; value: DatabaseType }[] = [
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'PostgreSQL', value: 'PG' },
  { label: 'SQL Server', value: 'SQLSERVER' },
]
