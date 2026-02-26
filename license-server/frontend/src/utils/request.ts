import axios from 'axios'
import { message } from 'ant-design-vue'

export const request = axios.create({
  timeout: 30000,
})

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.data?.code === 401) {
      handleUnauthorized(response.config)
    }
    return response
  },
  async (error) => {
    if (error.response?.status === 401) {
      return handleUnauthorized(error.config)
    }
    message.error(error.response?.data?.message || error.message || '请求失败')
    return Promise.reject(error)
  },
)

async function handleUnauthorized(originalConfig: any) {
  if (isRefreshing) {
    return new Promise((resolve) => {
      pendingRequests.push((token: string) => {
        originalConfig.headers.Authorization = `Bearer ${token}`
        resolve(request(originalConfig))
      })
    })
  }

  isRefreshing = true
  const refreshToken = localStorage.getItem('refreshToken')

  if (!refreshToken) {
    redirectToLogin()
    return Promise.reject(new Error('No refresh token'))
  }

  try {
    const res = await axios.post('/admin/auth/refresh', { refreshToken })
    if (res.data.code === 200) {
      const { accessToken, refreshToken: newRT } = res.data.data
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', newRT)
      pendingRequests.forEach((cb) => cb(accessToken))
      pendingRequests = []
      originalConfig.headers.Authorization = `Bearer ${accessToken}`
      return request(originalConfig)
    }
  } catch {
    // refresh failed
  } finally {
    isRefreshing = false
  }

  redirectToLogin()
  return Promise.reject(new Error('Token refresh failed'))
}

function redirectToLogin() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}
