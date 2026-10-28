/**
 * Dịch vụ tích hợp Google Identity Services (GIS) API chính thức
 * Hỗ trợ:
 * 1. google.accounts.id (renderButton, prompt One Tap, decode JWT ID Token)
 * 2. google.accounts.oauth2 (initTokenClient -> popup chọn tài khoản Google thật của trình duyệt)
 */
import api from '@/api'
import { loginClient } from '@/utils/taiKhoanKhachHang.js'

export const GOOGLE_CLIENT_ID =
  (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_GOOGLE_CLIENT_ID) ||
  '1035099307409-mvi41m3e498c1kbb8k1m8i69u0ksg42g.apps.googleusercontent.com'

/**
 * Giải mã Google JWT Token để lấy thông tin tài khoản Google
 */
export function decodeGoogleJwt(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (err) {
    console.error('Lỗi decode Google JWT:', err)
    return null
  }
}

/**
 * Đăng nhập / liên kết hồ sơ Google với hệ thống backend FF T-Shirt
 */
export async function authenticateGoogleProfileWithBackend(profile) {
  if (!profile || !profile.email) {
    throw new Error('Không nhận được email từ tài khoản Google!')
  }

  const email = profile.email.toLowerCase().trim()
  const displayName = profile.name || profile.given_name || email.split('@')[0]
  const avatarUrl = profile.picture || ''

  const res = await api.post('/api/khach-hang/dang-nhap-google', {
    email: email,
    name: displayName,
    avatar: avatarUrl
  })

  if (res.data?.success && res.data?.customer) {
    const customer = res.data.customer
    const userData = {
      id: customer.id,
      name: customer.tenKhachHang || customer.hoVaTen || displayName,
      phone: customer.soDienThoai || '',
      email: customer.email || email,
      maKhachHang: customer.maKhachHang || '',
      provider: 'google',
      avatarUrl: avatarUrl,
      avatarText: displayName.charAt(0).toUpperCase(),
      avatarBg: '#ea4335'
    }

    loginClient(userData)
    return userData
  } else {
    throw new Error(res.data?.message || 'Không thể xác thực tài khoản Google với hệ thống.')
  }
}

/**
 * Khởi tạo Google Identity Services và vẽ nút Google chính thức vào phần tử DOM
 */
export function renderOfficialGoogleButton(containerElement, onLoginSuccess, onLoginError) {
  if (!containerElement) return false
  if (!window.google?.accounts?.id) {
    console.warn('Google Identity Services chưa sẵn sàng.')
    return false
  }

  try {
    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: async (response) => {
        try {
          if (!response?.credential) throw new Error('Không nhận được credential từ Google!')
          const profile = decodeGoogleJwt(response.credential)
          const userData = await authenticateGoogleProfileWithBackend(profile)
          if (onLoginSuccess) onLoginSuccess(userData)
        } catch (err) {
          if (onLoginError) onLoginError(err)
        }
      },
      auto_select: false
    })

    window.google.accounts.id.renderButton(containerElement, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      shape: 'pill',
      logo_alignment: 'left',
      width: containerElement.clientWidth ? Math.min(containerElement.clientWidth, 340) : 280
    })

    return true
  } catch (err) {
    console.error('Lỗi render nút Google:', err)
    return false
  }
}

/**
 * Mở cửa sổ popup chọn tài khoản Google chuẩn OAuth2 của Google
 */
export function launchGoogleOAuthPopup({ onSuccess, onError }) {
  if (!window.google?.accounts?.oauth2) {
    if (onError) onError(new Error('Google Identity Services chưa tải xong trên trình duyệt!'))
    return
  }

  try {
    const client = window.google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: 'email profile openid',
      prompt: 'select_account',
      callback: async (tokenResponse) => {
        if (tokenResponse.error) {
          console.warn('Google OAuth popup response error:', tokenResponse)
          if (onError) onError(new Error(tokenResponse.error_description || tokenResponse.error))
          return
        }

        try {
          // Lấy thông tin tài khoản Google thật từ Google API
          const userInfoRes = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
            headers: { Authorization: `Bearer ${tokenResponse.access_token}` }
          })

          if (!userInfoRes.ok) {
            throw new Error('Không thể tải thông tin tài khoản từ Google!')
          }

          const googleProfile = await userInfoRes.json()
          const userData = await authenticateGoogleProfileWithBackend(googleProfile)
          if (onSuccess) onSuccess(userData)
        } catch (err) {
          if (onError) onError(err)
        }
      }
    })

    client.requestAccessToken({ prompt: 'select_account' })
  } catch (err) {
    console.error('Lỗi mở popup Google:', err)
    if (onError) onError(err)
  }
}
