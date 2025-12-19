export interface MpPage<T> {
  records: T[]
  total: number
  current: number
  size: number
}
