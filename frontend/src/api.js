import axios from 'axios'
import { isInViewMode } from '@/utils/auth.js'

const hostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost'
const backendOrigin = (hostname === '127.0.0.1' || hostname === 'localhost')
  ? `http://${hostname}:8080`
  : `http://${hostname}:8080`

const api = axios.create({
  baseURL: backendOrigin
})

// Tự động đính kèm thông tin tài khoản người dùng đang đăng nhập vào request headers
api.interceptors.request.use((config) => {
  // Nếu nhân viên đang ở Chế độ xem: Chặn các thao tác thay đổi dữ liệu
  const method = (config.method || 'get').toLowerCase()
  if (['post', 'put', 'delete', 'patch'].includes(method)) {
    const url = config.url || ''
    const isWhitelisted = url.includes('/api/nhan-vien/login') ||
                          url.includes('/api/nhan-vien/logout') ||
                          url.includes('/api/nhan-vien/forgot-password') ||
                          url.includes('/api/lich-su-ca/mo-ca') ||
                          url.includes('/api/lich-su-ca/ca-hien-tai') ||
                          url.includes('/api/lich-su-ca/ca-dong-gan-nhat') ||
                          url.includes('/api/lich-lam-viec/hom-nay')

    if (!isWhitelisted && isInViewMode()) {
      const msg = 'Bạn đang ở Chế độ xem (Chưa vào ca làm việc). Không thể thực hiện thao tác này!'
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('view-mode-blocked', { detail: { message: msg, url } }))
      }
      return Promise.reject(new Error(msg))
    }
  }

  try {
    const raw = localStorage.getItem('ff_current_user')
    if (raw) {
      const user = JSON.parse(raw)
      if (user && user.id != null) {
        config.headers['X-Current-User-Id'] = String(user.id)
      }
      if (user && user.tenTaiKhoan) {
        config.headers['X-Current-User-Account'] = user.tenTaiKhoan
      }
    }
  } catch (e) {}
  return config
})

export default api