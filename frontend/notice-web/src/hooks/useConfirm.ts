import { ElMessageBox } from 'element-plus'

export const useConfirm = () => {
  const confirm = async (message: string, title = '提示'): Promise<boolean> => {
    try {
      await ElMessageBox.confirm(message, title, { type: 'warning' })
      return true
    } catch {
      return false
    }
  }

  return { confirm }
}
