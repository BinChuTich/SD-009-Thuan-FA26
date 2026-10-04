import axios from 'axios'

const hostname = typeof window !== 'undefined' ? window.location.hostname : 'localhost'
const backendOrigin = (hostname === '127.0.0.1' || hostname === 'localhost')
  ? `http://${hostname}:8080`
  : `http://${hostname}:8080`

const api = axios.create({
  baseURL: backendOrigin,
  headers: {
    'Content-Type': 'application/json'
  }
})

export default api