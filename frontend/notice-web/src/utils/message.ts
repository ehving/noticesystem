import { ElMessage } from 'element-plus'

const show = (type: 'success' | 'error' | 'info', message: string) => {
  ElMessage({
    type,
    message,
    showClose: true,
    duration: 2000,
  })
}

export const success = (msg: string) => show('success', msg)
export const error = (msg: string) => show('error', msg)
export const info = (msg: string) => show('info', msg)
