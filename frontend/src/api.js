import axios from 'axios'

const hostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost'
const backendOrigin = (hostname === '127.0.0.1' || hostname === 'localhost')
  ? `http://${hostname}:8080`
  : `http://${hostname}:8080`

const api = axios.create({
  baseURL: backendOrigin
})

// Tự động đính kèm thông tin tài khoản người dùng đang đăng nhập vào request headers
api.interceptors.request.use((config) => {
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