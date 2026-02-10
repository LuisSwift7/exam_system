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
    token: localStorage.getItem('access_token') || '',
    me: null as Me | null,
  }),
  actions: {
    async fetchMe() {
      const res = await http.get('/api/me')
      this.me = res.data.data as Me
      return this.me
    },
    setToken(token: string) {
      this.token = token
      localStorage.setItem('access_token', token)
    },
    logout() {
      this.token = ''
      this.me = null
      localStorage.removeItem('access_token')
    },
  },
})

