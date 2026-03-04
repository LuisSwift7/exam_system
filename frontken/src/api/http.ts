import axios from 'axios'
import { useAuthStore } from '../stores/auth'

let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

export const http = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return res
      const err = new Error(body.message || '请求失败') as Error & { code?: number }
      err.code = body.code
      return Promise.reject(err)
    }
    return res
  },
  async (err) => {
    const auth = useAuthStore()
    const originalRequest = err.config
    
    if (err.response?.data?.code === 3001 && !originalRequest._retry) {
      if (isRefreshing) {
        // Wait for token refresh to complete
        return new Promise((resolve) => {
          refreshSubscribers.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(http(originalRequest))
          })
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        const refreshed = await auth.refreshTokens()
        if (refreshed) {
          // Retry original request
          originalRequest.headers.Authorization = `Bearer ${auth.accessToken}`
          // Resolve all pending requests
          refreshSubscribers.forEach(cb => cb(auth.accessToken))
          refreshSubscribers = []
          return http(originalRequest)
        } else {
          // Refresh failed, logout
          auth.logout()
          return Promise.reject(err)
        }
      } finally {
        isRefreshing = false
      }
    }
    
    return Promise.reject(err)
  },
)

