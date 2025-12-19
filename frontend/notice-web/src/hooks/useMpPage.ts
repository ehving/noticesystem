import { reactive } from 'vue'
import type { MpPage } from '@/types/page'

export const useMpPage = () => {
  const pagination = reactive({
    pageNo: 1,
    pageSize: 10,
    total: 0,
  })

  const setFromResponse = <T>(page: MpPage<T>) => {
    pagination.total = page.total || 0
    pagination.pageNo = page.current || pagination.pageNo
    pagination.pageSize = page.size || pagination.pageSize
  }

  const changeSize = (size: number, reload: () => void) => {
    pagination.pageSize = size
    pagination.pageNo = 1
    reload()
  }

  const changePage = (page: number, reload: () => void) => {
    pagination.pageNo = page
    reload()
  }

  const resetPage = (reload: () => void) => {
    pagination.pageNo = 1
    reload()
  }

  return {
    pagination,
    setFromResponse,
    changeSize,
    changePage,
    resetPage,
  }
}
