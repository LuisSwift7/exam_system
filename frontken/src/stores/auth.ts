import { defineStore } from 'pinia'
import { http } from '../api/http'

type Me = {
  id: number
  username: string
  realName: string | null
  roleCode: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('access_token') || '',
    refreshToken: localStorage.getItem('refresh_token') || '',
    me: null as Me | null,
  }),
  actions: {
    async fetchMe(): Promise<Me | null> {
      try {
        const res = await http.get('/api/me')
        this.me = res.data.data as Me
        return this.me
      } catch (error: any) {
        if (error?.response?.data?.code === 3001) {
          // Token expired, try to refresh
          const refreshed = await this.refreshTokens()
          if (refreshed) {
            return this.fetchMe()
          } else {
            this.logout()
            throw error
          }
        }
        throw error
      }
    },
    setTokens(accessToken: string, refreshToken: string) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      localStorage.setItem('access_token', accessToken)
      localStorage.setItem('refresh_token', refreshToken)
    },
    async refreshTokens(): Promise<boolean> {
      try {
        const res = await http.post('/api/auth/refresh', {
          refreshToken: this.refreshToken
        })
        const { accessToken, refreshToken } = res.data.data
        this.setTokens(accessToken, refreshToken)
        return true
      } catch (error) {
        console.error('Token refresh failed:', error)
        return false
      }
    },
    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.me = null
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
    },
  },
})

