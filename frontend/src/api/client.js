import axios from 'axios'
import { ElMessage } from 'element-plus'

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

function flattenErrorMessage(status, payload, fallback) {
  // BUG: any 400 becomes the same generic copy, including authz failures.
  if (status === 400) {
    return '输入不合法'
  }
  if (status === 403) {
    // unreachable while backend maps FORBIDDEN -> 400
    return '输入不合法'
  }
  return payload?.message || fallback || '请求失败'
}

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
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default api
