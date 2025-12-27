export const isMobileByWidth = (): boolean => typeof window !== 'undefined' && window.innerWidth < 1024

export const getDeviceType = (): 'mobile' | 'desktop' => (isMobileByWidth() ? 'mobile' : 'desktop')

export const onDeviceChange = (cb: () => void): (() => void) => {
  let timer: number | undefined
  const handler = () => {
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => cb(), 150)
  }
  window.addEventListener('resize', handler)
  return () => {
    window.removeEventListener('resize', handler)
    if (timer) window.clearTimeout(timer)
  }
}
