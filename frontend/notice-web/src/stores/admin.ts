import { defineStore } from 'pinia'
import type { DatabaseType } from '@/types/enums/db'

const DB_KEY = 'NOTICE_ADMIN_DB'

interface AdminState {
  activeDb: DatabaseType
  changeTick: number
}

export const useAdminStore = defineStore('admin', {
  state: (): AdminState => ({
    activeDb: (localStorage.getItem(DB_KEY) as DatabaseType) || 'MYSQL',
    changeTick: 0,
  }),
  actions: {
    setDb(db: DatabaseType) {
      this.activeDb = db
      localStorage.setItem(DB_KEY, db)
      this.changeTick += 1
    },
  },
})
