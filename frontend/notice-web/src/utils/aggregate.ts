export interface CountItem {
  name: string
  count: number
}

export const aggregateCount = <T>(
  list: T[],
  picker: (item: T) => string | undefined | null
): CountItem[] => {
  const map = new Map<string, number>()
  list.forEach((item) => {
    const key = picker(item)
    if (!key) return
    map.set(key, (map.get(key) || 0) + 1)
  })
  return Array.from(map.entries()).map(([name, count]) => ({ name, count }))
}
