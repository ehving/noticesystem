import dayjs from 'dayjs'

export const formatDateTime = (val?: string | null): string => {
  if (!val) return '-'
  const d = dayjs(val)
  if (!d.isValid()) return '-'
  return d.format('YYYY-MM-DD HH:mm')
}
