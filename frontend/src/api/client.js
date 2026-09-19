import axios from 'axios'
import { ElMessage } from 'element-plus'
import { flattenErrorMessage } from './errorMessage'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const payload = error.response?.data
    const status = error.response?.status
    const msg = flattenErrorMessage(status, payload, error.message)
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      if (window.location.pathname.includes('/login')) {
        // Stay on the login page, but still show why (e.g. bad credentials).
        ElMessage.error(msg)
      } else {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default api
