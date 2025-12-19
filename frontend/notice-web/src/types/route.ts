export type AppRole = '管理员' | '普通用户' | string

export interface AppRouteMeta {
  requiresAuth?: boolean
  roles?: AppRole[]
  title?: string
  desktopOnly?: boolean
  mobileOnly?: boolean
}
