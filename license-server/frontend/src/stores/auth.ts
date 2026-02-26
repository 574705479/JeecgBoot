import { defineStore } from 'pinia'
import { ref } from 'vue'
import { request } from '../utils/request'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  function clearTokens() {
    accessToken.value = ''
    refreshToken.value = ''
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  async function login(username: string, password: string) {
    const res = await request.post('/admin/auth/login', { username, password })
    if (res.data.code === 200) {
      const { accessToken: at, refreshToken: rt } = res.data.data
      setTokens(at, rt)
      return true
    }
    throw new Error(res.data.message || '登录失败')
  }

  async function logout() {
    try {
      await request.post('/admin/auth/logout', { refreshToken: refreshToken.value })
    } catch {
      // ignore
    }
    clearTokens()
    router.push('/login')
  }

  async function refreshAccessToken() {
    try {
      const res = await request.post('/admin/auth/refresh', {
        refreshToken: refreshToken.value,
      })
      if (res.data.code === 200) {
        const { accessToken: at, refreshToken: rt } = res.data.data
        setTokens(at, rt)
        return at
      }
    } catch {
      // ignore
    }
    clearTokens()
    router.push('/login')
    return null
  }

  return { accessToken, refreshToken, login, logout, refreshAccessToken, clearTokens }
})
