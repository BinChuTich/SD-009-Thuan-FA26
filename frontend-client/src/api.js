import axios from 'axios'

const configuredBackend = String(import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
// Mặc định gọi cùng origin để Vite/reverse proxy chuyển /api về backend.
// Không ghép hostname:8080 vì máy khách ở mạng khác không thể truy cập cổng
// backend nội bộ, khiến nút đặt hàng chờ vô hạn.
const backendOrigin = configuredBackend || ''

const api = axios.create({
  baseURL: backendOrigin,
  timeout: 20000
})

export default api

