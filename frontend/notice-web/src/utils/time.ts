import dayjs from 'dayjs'
/**展示时间用**/
export const formatDateTime = (val?: string | null): string => {
  if (!val) return '-'
  const d = dayjs(val)
  if (!d.isValid()) return '-'
  return d.format('YYYY-MM-DD HH:mm')
}
/**
 * ✅ 请求参数用：把 Date / dayjs / string 统一转成后端能吃的 LocalDateTime 字符串
 * 推荐格式：YYYY-MM-DDTHH:mm:ss
 */
export const toLocalDateTimeParam = (val: any): string | undefined => {
  if (!val) return undefined

  if (typeof val === 'string') {
    // 兼容 "2025-12-17 00:00" / "2025-12-17 00:00:00" / "2025-12-17"
    let s = val.trim()
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) s = s + 'T00:00:00'
    else if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(s)) s = s.replace(' ', 'T') + ':00'
    else if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(s)) s = s.replace(' ', 'T')

    const d = dayjs(s)
    return d.isValid() ? d.format('YYYY-MM-DDTHH:mm:ss') : undefined
  }


  // dayjs 对象
  if (val?.isValid && typeof val.isValid === 'function') {
    return val.isValid() ? val.format('YYYY-MM-DDTHH:mm:ss') : undefined
  }

  // Date 对象
  if (val instanceof Date) {
    const d = dayjs(val)
    return d.isValid() ? d.format('YYYY-MM-DDTHH:mm:ss') : undefined
  }

  // 兜底：某些适配层会是 { $d: Date }
  if (val?.$d instanceof Date) {
    const d = dayjs(val.$d)
    return d.isValid() ? d.format('YYYY-MM-DDTHH:mm:ss') : undefined
  }

  return undefined
}
