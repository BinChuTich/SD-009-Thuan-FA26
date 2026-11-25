import axios from 'axios'

const hostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost'
const configuredBackend = String(import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')
const backendOrigin = configuredBackend || `http://${hostname}:8080`

const api = axios.create({
  baseURL: backendOrigin
})

export default api

