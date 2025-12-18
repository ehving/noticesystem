import axios, {
  AxiosHeaders,
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import { clearToken, getToken } from '../utils/storage';
import type { Result } from '../types/common';

type HttpRequestConfig<T = any> = AxiosRequestConfig<T> & { raw?: boolean };

type HttpInstance = AxiosInstance;

const redirectToLogin = (): void => {
  if (typeof window !== 'undefined') {
    window.location.href = '/login';
  }
};

const AUTH_FAILURE_KEYWORDS = [
  '未登录',
  '登录已失效',
  '未登录或登录已失效',
  '登录用户不存在',
  '权限不足',
  '无权限',
  '仅管理员',
];

const isAuthFailureMessage = (msg: unknown): msg is string =>
  typeof msg === 'string' && AUTH_FAILURE_KEYWORDS.some((keyword) => msg.includes(keyword));

const handleAuthFailure = (message?: string): Promise<never> => {
  clearToken();
  redirectToLogin();
  return Promise.reject(new Error(message || 'Unauthorized'));
};

const http: HttpInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    if (config.headers instanceof AxiosHeaders) {
      config.headers.set('Authorization', token);
    } else {
      // @ts-ignore
      config.headers = { ...(config.headers ?? {}), Authorization: token };
    }
  }
  return config;
});

http.interceptors.response.use(
  (response: AxiosResponse<Result<any>>) => {
    const config = response.config as HttpRequestConfig;

    if (response.status === 401 || response.status === 403) {
      const statusMessage = response.status === 403 ? 'Forbidden' : 'Unauthorized';
      return handleAuthFailure(statusMessage);
    }

    const data = response.data;
    const isResultShape =
      data && typeof data === 'object' && 'code' in data && 'msg' in data && 'data' in data;

    if (isResultShape) {
      if (data.code === 1) {
        if (isAuthFailureMessage(data.msg)) {
          return handleAuthFailure(data.msg);
        }
        return Promise.reject(new Error(data.msg || 'Request failed'));
      }
      if (config.raw) {
        return data;
      }
      return data.data;
    }

    return data;
  },
  (error: AxiosError) => {
    const status = error.response?.status;
    const responseData = error.response?.data as Result<unknown> | undefined;

    if (status === 401 || status === 403) {
      const statusMessage = status === 403 ? 'Forbidden' : 'Unauthorized';
      return handleAuthFailure(statusMessage);
    }

    if (responseData?.code === 1 && isAuthFailureMessage(responseData.msg)) {
      return handleAuthFailure(responseData.msg);
    }
    return Promise.reject(error);
  },
);

export const request = <T = any>(config: HttpRequestConfig): Promise<T> => http.request<any, T>(config);

export const requestRaw = <T = any>(config: HttpRequestConfig): Promise<Result<T>> => {
  const mergedConfig: HttpRequestConfig = { ...config, raw: true };
  return http.request<any, Result<T>>(mergedConfig);
};

export default http;
