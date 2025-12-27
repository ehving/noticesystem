import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'
import { getToken, clearToken } from '@/utils/auth'
import * as message from '@/utils/message'
import type { Result } from '@/types/result'
import { toLocalDateTimeParam } from '@/utils/time'

type NavigateFn = (path: string) => void

let navigate: NavigateFn | null = null



export const setNavigate = (fn: NavigateFn) => {
  navigate = fn
}

const loginKeywords = ['未登录', '登录已失效', '请重新登录', '登录用户不存在']

const redirectToLogin = () => {
  clearToken()
  if (navigate) {
    navigate('/login')
    return
  }
  window.location.assign('/login')
}

const isRecord = (val: unknown): val is Record<string, unknown> => {
  return typeof val === 'object' && val !== null
}

const isResultShape = (payload: unknown): payload is Result<unknown> => {
  return isRecord(payload) && 'code' in payload && 'msg' in payload
}

const normalizeDateTimeKeys = (obj: any) => {
  if (!obj || typeof obj !== 'object') return

  // 数组
  if (Array.isArray(obj)) {
    obj.forEach(normalizeDateTimeKeys)
    return
  }

  for (const key of Object.keys(obj)) {
    const val = obj[key]

    // 只要遇到 beginTime/endTime 就规范化
    if (key === 'beginTime' || key === 'endTime') {
      const s = toLocalDateTimeParam(val)
      // 只在能转成功时覆盖，避免把奇怪值变成 undefined 导致过滤失效
      if (s) obj[key] = s
      continue
    }

    // 继续递归
    if (val && typeof val === 'object') {
      normalizeDateTimeKeys(val)
    }
  }
}


const extractMsg = (payload: unknown): string | undefined => {
  if (typeof payload === 'string') return payload
  if (isRecord(payload)) {
    const msg = payload.msg ?? payload.message
    if (typeof msg === 'string') return msg
  }
  return undefined
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = token
  }

  //  全局统一把 beginTime/endTime 转成 LocalDateTime 可解析的 ISO 字符串
  if (config.data && typeof config.data === 'object') {
    normalizeDateTimeKeys(config.data)
  }
  if (config.params && typeof config.params === 'object') {
    normalizeDateTimeKeys(config.params)
  }

  return config
})


instance.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      redirectToLogin()
    }
    return Promise.reject(error)
  }
)

const request = async <T>(config: AxiosRequestConfig): Promise<T> => {
  try {
    const response = await instance.request<unknown, AxiosResponse<unknown>>(config)
    const payload = response.data

    if (isResultShape(payload)) {
      const res: Result<unknown> = payload
      const msg = typeof res.msg === 'string' ? res.msg : ''

      if (res.code === 0) {
        return res.data as T
      }

      const hitLogin = loginKeywords.some((kw) => msg.includes(kw))
      if (hitLogin) {
        redirectToLogin()
        throw new Error(msg || '未登录')
      }

      message.error(msg || '请求失败')
      throw new Error(msg || '请求失败')
    }

    // 非 Result 结构，直接返回原始数据（例如文件/纯文本）
    return payload as T
  } catch (err) {
    if (!axios.isAxiosError(err)) {
      // 已在上方处理过业务错误，不重复弹窗
      throw err
    }

    const status = err.response?.status
    if (status === 401) {
      redirectToLogin()
      throw err
    }

    const backendMsg = extractMsg(err.response?.data) ?? err.message ?? '请求异常'
    message.error(backendMsg)
    throw err
  }
}

const http = {
  request,
  get<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'GET' })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, data, method: 'POST' })
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, data, method: 'PUT' })
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>({ ...config, url, method: 'DELETE' })
  },
}

export default http
