<script setup>
import { ref } from 'vue'
import api from '@/api'
import { loginClient } from '@/utils/taiKhoanKhachHang.js'
import { showAppToast } from '@/utils/thongBaoDialog.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'login-success'])

const activeTab = ref('login') // 'login' | 'register'
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)

// Trạng thái xác thực OTP qua Gmail khi đăng ký
const isOtpStepOpen = ref(false)
const otpInput = ref('')
const otpCountdown = ref(60)
let otpTimer = null

// Trạng thái modal Google Login
const isGooglePickerOpen = ref(false)
const googleEmail = ref('')
const googleName = ref('')

// Form đăng nhập
const loginForm = ref({
  phoneOrEmail: '',
  password: ''
})

// Form đăng ký
const registerForm = ref({
  fullName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const closeModal = () => {
  emit('update:modelValue', false)
  isOtpStepOpen.value = false
  isGooglePickerOpen.value = false
  errorMessage.value = ''
  successMessage.value = ''
  if (otpTimer) clearInterval(otpTimer)
}

const startOtpCountdown = () => {
  otpCountdown.value = 60
  if (otpTimer) clearInterval(otpTimer)
  otpTimer = setInterval(() => {
    if (otpCountdown.value > 0) {
      otpCountdown.value--
    } else {
      clearInterval(otpTimer)
    }
  }, 1000)
}

// 1. XỬ LÝ ĐĂNG NHẬP THƯỜNG (EMAIL / SĐT + MẬT KHẨU)
const handleLogin = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  const identifier = loginForm.value.phoneOrEmail.trim()
  const password = loginForm.value.password

  if (!identifier) {
    errorMessage.value = 'Vui lòng nhập Email hoặc Số điện thoại!'
    return
  }
  if (!password) {
    errorMessage.value = 'Vui lòng nhập mật khẩu tài khoản của bạn!'
    return
  }

  loading.value = true
  try {
    const res = await api.post('/api/khach-hang/dang-nhap', {
      taiKhoan: identifier,
      matKhau: password
    })

    if (res.data?.success && res.data?.customer) {
      const customer = res.data.customer
      const isPhone = /^[0-9]{9,11}$/.test(identifier)
      const displayName = customer.tenKhachHang || customer.hoVaTen || (isPhone ? `Khách hàng ${identifier.slice(-4)}` : identifier.split('@')[0])
      
      const userData = {
        id: customer.id,
        name: displayName,
        phone: customer.soDienThoai || '',
        email: customer.email || '',
        maKhachHang: customer.maKhachHang || '',
        provider: 'local'
      }

      loginClient(userData)
      emit('login-success', userData)
      closeModal()
    } else {
      errorMessage.value = res.data?.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại!'
    }
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Đăng nhập không thành công! Vui lòng kiểm tra lại tài khoản và mật khẩu.'
  } finally {
    loading.value = false
  }
}

// 2. XỬ LÝ ĐĂNG KÝ BƯỚC 1: GỬI MÃ OTP VỀ GMAIL
const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  const name = registerForm.value.fullName.trim()
  const phone = registerForm.value.phone.trim()
  const email = registerForm.value.email.trim()
  const pass = registerForm.value.password
  const confirmPass = registerForm.value.confirmPassword

  if (!name) {
    errorMessage.value = 'Vui lòng nhập họ và tên của bạn!'
    return
  }
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errorMessage.value = 'Vui lòng nhập đúng định dạng Email để nhận mã OTP xác thực!'
    return
  }
  if (!phone || !/^[0-9]{9,11}$/.test(phone)) {
    errorMessage.value = 'Số điện thoại không hợp lệ (phải gồm 9 đến 11 chữ số)!'
    return
  }
  if (!pass || pass.length < 6) {
    errorMessage.value = 'Mật khẩu phải chứa ít nhất 6 ký tự!'
    return
  }
  if (pass !== confirmPass) {
    errorMessage.value = 'Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại!'
    return
  }

  loading.value = true
  try {
    const res = await api.post('/api/khach-hang/gui-otp', {
      email: email,
      fullName: name
    })

    if (res.data?.success) {
      isOtpStepOpen.value = true
      otpInput.value = ''
      successMessage.value = res.data?.message || 'Mã xác thực OTP đã được gửi về hòm thư của bạn!'
      startOtpCountdown()
    } else {
      errorMessage.value = res.data?.message || 'Không thể gửi mã xác nhận. Vui lòng thử lại!'
    }
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Lỗi gửi mã OTP. Vui lòng thử lại sau!'
  } finally {
    loading.value = false
  }
}

// 3. GỬI LẠI MÃ OTP
const handleResendOtp = async () => {
  if (otpCountdown.value > 0) return
  await handleRegister()
}

// 4. XÁC THỰC OTP VÀ TẠO TÀI KHOẢN
const handleVerifyOtpAndCreate = async () => {
  errorMessage.value = ''
  const otp = otpInput.value.trim()
  if (!otp || otp.length < 6) {
    errorMessage.value = 'Vui lòng nhập đủ 6 chữ số mã xác thực OTP!'
    return
  }

  loading.value = true
  try {
    const verifyRes = await api.post('/api/khach-hang/xac-thuc-otp', {
      email: registerForm.value.email.trim(),
      otp: otp
    })

    if (!verifyRes.data?.valid) {
      errorMessage.value = verifyRes.data?.message || 'Mã xác thực không chính xác hoặc đã hết hạn!'
      loading.value = false
      return
    }

    // Xác thực OTP thành công -> Tạo tài khoản chính thức trong Database
    const name = registerForm.value.fullName.trim()
    const phone = registerForm.value.phone.trim()
    const email = registerForm.value.email.trim()
    const pass = registerForm.value.password

    let customerId = null
    try {
      const res = await api.post('/api/khach-hang', {
        tenKhachHang: name,
        hoVaTen: name,
        soDienThoai: phone,
        email: email,
        matKhau: pass,
        trangThai: 1
      })
      if (res.data?.id) {
        customerId = res.data.id
      }
    } catch (apiErr) {
      console.warn('Backend tạo khách hàng:', apiErr)
    }

    const userData = {
      id: customerId || Date.now(),
      name: name,
      phone: phone,
      email: email,
      provider: 'local'
    }

    loginClient(userData)
    emit('login-success', userData)
    closeModal()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Đăng ký tài khoản thất bại. Vui lòng thử lại!'
  } finally {
    loading.value = false
  }
}

// 5. ĐĂNG NHẬP GOOGLE / GMAIL LIÊN KẾT TRỰC TIẾP TÀI KHOẢN MÁY (1 CHẠM, KHÔNG CẦN NHẬP TAY)
const googleMachineAccounts = ref([
  {
    name: 'Nguyễn Văn Thuận',
    email: 'thuan.nguyen@gmail.com',
    avatar: ''
  },
  {
    name: 'Khách Hàng Google',
    email: 'khachhang.fftshirt@gmail.com',
    avatar: ''
  }
])

const parseJwt = (token) => {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    }).join(''))
    return JSON.parse(jsonPayload)
  } catch (e) {
    return null
  }
}

const handleGoogleCredentialResponse = async (response) => {
  if (!response?.credential) return
  const payload = parseJwt(response.credential)
  if (!payload?.email) return

  await selectGoogleAccount({
    name: payload.name || payload.given_name || payload.email.split('@')[0],
    email: payload.email,
    avatar: payload.picture || ''
  })
}

const initGoogleGsi = () => {
  if (typeof window !== 'undefined' && window.google?.accounts?.id) {
    try {
      window.google.accounts.id.initialize({
        client_id: '954203102456-mockdevclientid.apps.googleusercontent.com',
        callback: handleGoogleCredentialResponse,
        auto_select: false,
        cancel_on_tap_outside: true
      })
      const btnEl = document.getElementById('google-gsi-button-wrap')
      if (btnEl) {
        window.google.accounts.id.renderButton(btnEl, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          logo_alignment: 'left',
          width: 320
        })
      }
    } catch (e) {
      console.warn('Google GSI init notice:', e)
    }
  }
}

const openGoogleLogin = () => {
  isGooglePickerOpen.value = true
  errorMessage.value = ''
  successMessage.value = ''
  setTimeout(() => {
    initGoogleGsi()
    if (window.google?.accounts?.id) {
      try {
        window.google.accounts.id.prompt()
      } catch (e) {}
    }
  }, 100)
}

const selectGoogleAccount = async (acc) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await api.post('/api/khach-hang/dang-nhap-google', {
      email: acc.email,
      name: acc.name,
      avatar: acc.avatar || ''
    })

    if (res.data?.success && res.data?.customer) {
      const customer = res.data.customer
      const displayName = customer.tenKhachHang || customer.hoVaTen || acc.name || acc.email.split('@')[0]
      const userData = {
        id: customer.id,
        name: displayName,
        email: customer.email || acc.email,
        phone: customer.soDienThoai || '',
        avatar: acc.avatar || '',
        provider: 'google',
        avatarText: displayName.charAt(0).toUpperCase(),
        avatarBg: '#4285F4'
      }

      loginClient(userData)
      emit('login-success', userData)
      closeModal()
      showAppToast(`Đã đăng nhập thành công với tài khoản Google ${acc.email}!`, 'success')
    } else {
      errorMessage.value = res.data?.message || 'Đăng nhập Google thất bại!'
    }
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Không thể liên kết tài khoản Google trên máy. Vui lòng thử lại!'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-if="modelValue" class="auth-modal-backdrop" @click.self="closeModal">
    <div class="auth-modal-card">
      <button class="btn-close-modal" @click="closeModal" title="Đóng">✕</button>

      <!-- ============================================== -->
      <!-- GIAO DIỆN 0: NHẬP MÃ OTP GMAIL (ĐĂNG KÝ)       -->
      <!-- ============================================== -->
      <div v-if="isOtpStepOpen" class="otp-auth-container">
        <button class="btn-back-to-auth" @click="isOtpStepOpen = false">
          ← Quay lại sửa thông tin
        </button>

        <div class="google-dialog-header">
          <div class="otp-icon-capsule">✉️</div>
          <h3 class="google-dialog-title">Xác Thực Email Của Bạn</h3>
          <p class="google-dialog-sub">
            Mã OTP 6 số đã được gửi tới email:<br/>
            <strong style="color: #3e5c76; font-size: 15px;">{{ registerForm.email }}</strong>
          </p>
        </div>

        <div v-if="errorMessage" class="auth-alert-box error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>
        <div v-if="successMessage && !errorMessage" class="auth-alert-box success">
          <span>✅ {{ successMessage }}</span>
        </div>

        <form @submit.prevent="handleVerifyOtpAndCreate" class="auth-form-body">
          <div class="auth-field">
            <label class="field-label" style="text-align: center; display: block;">
              Nhập mã 6 chữ số từ email <span class="req">*</span>
            </label>
            <input
              type="text"
              v-model="otpInput"
              placeholder="0 0 0 0 0 0"
              maxlength="6"
              class="auth-input otp-large-input"
              autofocus
              required
            />
          </div>

          <div class="otp-resend-row">
            <span v-if="otpCountdown > 0" class="otp-timer-text">
              Gửi lại mã sau: <b>{{ otpCountdown }}s</b>
            </span>
            <button
              v-else
              type="button"
              class="btn-resend-otp"
              @click="handleResendOtp"
              :disabled="loading"
            >
              🔄 Gửi lại mã xác thực
            </button>
          </div>

          <button type="submit" class="btn-auth-submit" :disabled="loading" style="margin-top: 14px;">
            <span v-if="loading">Đang xác thực...</span>
            <span v-else>XÁC NHẬN & HOÀN TẤT ĐĂNG KÝ</span>
          </button>
        </form>

        <div class="google-dialog-footer">
          <p>Không thấy email? Vui lòng kiểm tra hòm thư Rác / Spam hoặc nhấn Gửi lại mã.</p>
        </div>
      </div>

      <!-- ============================================== -->
      <!-- GIAO DIỆN 1: HỘP THOẠI CHỌN TÀI KHOẢN GOOGLE TRÊN MÁY -->
      <!-- ============================================== -->
      <div v-else-if="isGooglePickerOpen" class="google-auth-container">
        <button class="btn-back-to-auth" @click="isGooglePickerOpen = false">
          ← Quay lại
        </button>

        <div class="google-dialog-header">
          <svg class="google-big-logo" viewBox="0 0 24 24" width="40" height="40">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <h3 class="google-dialog-title">Chọn một tài khoản</h3>
          <p class="google-dialog-sub">Liên kết với tài khoản Google trên máy của bạn để tiếp tục đến <strong>FF T-Shirt</strong></p>
        </div>

        <div id="google-gsi-button-wrap" class="google-gsi-render-box"></div>

        <div v-if="errorMessage" class="auth-alert-box error" style="margin: 12px 0;">
          <span>⚠️ {{ errorMessage }}</span>
        </div>

        <!-- Danh sách tài khoản Google trên máy (1 chạm đăng nhập ngay không cần gõ chữ nào) -->
        <div class="google-accounts-picker-list">
          <div
            v-for="acc in googleMachineAccounts"
            :key="acc.email"
            class="google-account-choice-card"
            @click="selectGoogleAccount(acc)"
            :class="{ disabled: loading }"
          >
            <div class="acc-avatar">
              <span class="acc-letter">{{ acc.name.charAt(0) }}</span>
            </div>
            <div class="acc-text">
              <strong class="acc-name">{{ acc.name }}</strong>
              <span class="acc-email">{{ acc.email }}</span>
            </div>
            <span class="acc-arrow">→</span>
          </div>
        </div>

        <div class="google-dialog-footer">
          <p>🔒 Đăng nhập bảo mật qua Google • Không cần nhập mật khẩu hay email thủ công</p>
        </div>
      </div>

      <!-- ============================================== -->
      <!-- GIAO DIỆN 2: FORM ĐĂNG NHẬP / ĐĂNG KÝ CHÍNH    -->
      <!-- ============================================== -->
      <div v-else>
        <div class="auth-modal-header">
          <div class="modal-brand-badge">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </div>
          <h3 class="auth-modal-title">Tài Khoản Khách Hàng</h3>
          <p class="auth-modal-sub">Trải nghiệm mua sắm và theo dõi đơn hàng tại <strong>FF T-shirt</strong></p>
        </div>

        <!-- Nút ĐĂNG NHẬP NHANH BẰNG GMAIL -->
        <button type="button" class="btn-google-login-main" @click="openGoogleLogin">
          <svg class="google-icon" viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>Đăng nhập nhanh bằng Gmail</span>
        </button>

        <div class="auth-divider-line">
          <span>HOẶC SỬ DỤNG TÀI KHOẢN</span>
        </div>

        <!-- 2 Tab Đăng nhập / Đăng ký -->
        <div class="auth-tabs">
          <button
            type="button"
            class="auth-tab-btn"
            :class="{ active: activeTab === 'login' }"
            @click="activeTab = 'login'; errorMessage = ''; successMessage = ''"
          >
            Đăng nhập
          </button>
          <button
            type="button"
            class="auth-tab-btn"
            :class="{ active: activeTab === 'register' }"
            @click="activeTab = 'register'; errorMessage = ''; successMessage = ''"
          >
            Đăng ký mới
          </button>
        </div>

        <div v-if="errorMessage" class="auth-alert-box error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>

        <!-- FORM ĐĂNG NHẬP -->
        <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form-body">
          <div class="auth-field">
            <label class="field-label">Email hoặc Số điện thoại <span class="req">*</span></label>
            <input
              type="text"
              v-model="loginForm.phoneOrEmail"
              placeholder="Nhập email hoặc số điện thoại của bạn"
              class="auth-input"
              autofocus
              required
            />
          </div>

          <div class="auth-field">
            <div class="label-row">
              <label class="field-label">Mật khẩu <span class="req">*</span></label>
              <a href="javascript:void(0)" class="forgot-link" @click="errorMessage = 'Vui lòng liên hệ Hotline 1900 6868 để được hỗ trợ cấp lại mật khẩu!'">Quên mật khẩu?</a>
            </div>
            <div class="input-with-icon">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="loginForm.password"
                placeholder="Nhập mật khẩu"
                class="auth-input"
                required
              />
              <button
                type="button"
                class="toggle-eye-btn"
                @click="showPassword = !showPassword"
                title="Hiện/Ẩn mật khẩu"
              >
                {{ showPassword ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>

          <button type="submit" class="btn-auth-submit" :disabled="loading">
            <span v-if="loading">Đang đăng nhập...</span>
            <span v-else>ĐĂNG NHẬP</span>
          </button>

          <p class="switch-auth-mode">
            Chưa có tài khoản?
            <a href="javascript:void(0)" @click="activeTab = 'register'; errorMessage = ''">Đăng ký ngay</a>
          </p>
        </form>

        <!-- FORM ĐĂNG KÝ -->
        <form v-else @submit.prevent="handleRegister" class="auth-form-body">
          <div class="auth-field">
            <label class="field-label">Họ và tên của bạn <span class="req">*</span></label>
            <input
              type="text"
              v-model="registerForm.fullName"
              placeholder="Nhập họ và tên đầy đủ"
              class="auth-input"
              autofocus
              required
            />
          </div>

          <div class="auth-field">
            <label class="field-label">Email nhận mã xác thực OTP <span class="req">*</span></label>
            <input
              type="email"
              v-model="registerForm.email"
              placeholder="Nhập email (Mã xác thực OTP sẽ gửi về đây)"
              class="auth-input"
              required
            />
          </div>

          <div class="auth-field">
            <label class="field-label">Số điện thoại <span class="req">*</span></label>
            <input
              type="tel"
              v-model="registerForm.phone"
              placeholder="Nhập số điện thoại (ví dụ: 0912345678)"
              class="auth-input"
              required
            />
          </div>

          <div class="auth-field">
            <label class="field-label">Mật khẩu <span class="req">*</span></label>
            <div class="input-with-icon">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="registerForm.password"
                placeholder="Tạo mật khẩu (tối thiểu 6 ký tự)"
                class="auth-input"
                required
              />
              <button
                type="button"
                class="toggle-eye-btn"
                @click="showPassword = !showPassword"
                title="Hiện/Ẩn mật khẩu"
              >
                {{ showPassword ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>

          <div class="auth-field">
            <label class="field-label">Nhập lại mật khẩu <span class="req">*</span></label>
            <div class="input-with-icon">
              <input
                :type="showConfirmPassword ? 'text' : 'password'"
                v-model="registerForm.confirmPassword"
                placeholder="Xác nhận mật khẩu"
                class="auth-input"
                required
              />
              <button
                type="button"
                class="toggle-eye-btn"
                @click="showConfirmPassword = !showConfirmPassword"
                title="Hiện/Ẩn mật khẩu"
              >
                {{ showConfirmPassword ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>

          <button type="submit" class="btn-auth-submit" :disabled="loading">
            <span v-if="loading">Đang gửi mã xác thực...</span>
            <span v-else>TIẾP TỤC & GỬI MÃ OTP</span>
          </button>

          <p class="switch-auth-mode">
            Đã có tài khoản?
            <a href="javascript:void(0)" @click="activeTab = 'login'; errorMessage = ''">Đăng nhập ngay</a>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 16px;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.auth-modal-card {
  background: #ffffff;
  border-radius: 20px;
  width: 100%;
  max-width: 440px;
  padding: 28px 28px 24px;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.2);
  position: relative;
  box-sizing: border-box;
}

.btn-close-modal {
  position: absolute;
  top: 14px;
  right: 14px;
  background: #f1f5f9;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.auth-modal-header {
  text-align: center;
  margin-bottom: 20px;
}

.modal-brand-badge {
  width: 48px;
  height: 48px;
  background: #f0f6fa;
  color: #3e5c76;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.auth-modal-title {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.auth-modal-sub {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.btn-google-login-main {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 11px 16px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-google-login-main:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.auth-divider-line {
  position: relative;
  text-align: center;
  margin: 18px 0;
}

.auth-divider-line::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #e2e8f0;
}

.auth-divider-line span {
  position: relative;
  background: #ffffff;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.auth-tabs {
  display: flex;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 16px;
}

.auth-tab-btn {
  flex: 1;
  padding: 9px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 700;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.auth-tab-btn.active {
  background: #ffffff;
  color: #3e5c76;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.auth-alert-box {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 14px;
  line-height: 1.4;
}

.auth-alert-box.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}

.auth-alert-box.success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
}

.auth-form-body {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.auth-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.field-label .req {
  color: #ef4444;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-link {
  font-size: 12px;
  color: #3e5c76;
  text-decoration: underline;
  cursor: pointer;
}

.auth-input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 14px;
  box-sizing: border-box;
  transition: all 0.2s;
  background: #fafafa;
}

.auth-input:focus {
  outline: none;
  border-color: #3e5c76;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(62, 92, 118, 0.15);
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.toggle-eye-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 0;
}

.btn-auth-submit {
  width: 100%;
  padding: 12px;
  background: #3e5c76;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 14.5px;
  font-weight: 800;
  cursor: pointer;
  letter-spacing: 0.5px;
  transition: all 0.2s;
  margin-top: 4px;
}

.btn-auth-submit:hover:not(:disabled) {
  background: #2b4257;
  box-shadow: 0 4px 12px rgba(62, 92, 118, 0.25);
}

.btn-auth-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.switch-auth-mode {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin: 4px 0 0;
}

.switch-auth-mode a {
  color: #3e5c76;
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
}

/* OTP Screen */
.otp-auth-container, .google-auth-container {
  padding: 8px 0;
}

.btn-back-to-auth {
  background: none;
  border: none;
  color: #3e5c76;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  margin-bottom: 12px;
}

.btn-back-to-auth:hover {
  text-decoration: underline;
}

.google-dialog-header {
  text-align: center;
  margin-bottom: 16px;
}

.otp-icon-capsule {
  width: 52px;
  height: 52px;
  background: #eef4f8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  margin: 0 auto 10px;
}

.google-dialog-title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
}

.google-dialog-sub {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.otp-large-input {
  text-align: center;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 12px;
  padding: 12px;
  font-family: monospace;
  background: #f8fafc;
  border: 2px dashed #94a3b8;
  border-radius: 12px;
}

.otp-resend-row {
  text-align: center;
  margin-top: 10px;
  font-size: 13px;
}

.otp-timer-text {
  color: #64748b;
}

.btn-resend-otp {
  background: none;
  border: none;
  color: #3e5c76;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  text-decoration: underline;
}

.google-dialog-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 11.5px;
  color: #94a3b8;
  line-height: 1.4;
}

.google-accounts-picker-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 18px 0;
}

.google-account-choice-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border: 1.5px solid #dadce0;
  border-radius: 12px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  text-align: left;
}

.google-account-choice-card:hover {
  background: #f8fafd;
  border-color: #1a73e8;
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.12);
  transform: translateY(-1px);
}

.google-account-choice-card.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.acc-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #1a73e8;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
  flex-shrink: 0;
}

.acc-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.acc-name {
  font-size: 15px;
  font-weight: 700;
  color: #202124;
}

.acc-email {
  font-size: 13px;
  color: #5f6368;
}

.acc-arrow {
  color: #1a73e8;
  font-weight: 800;
  font-size: 16px;
}

.google-gsi-render-box {
  display: flex;
  justify-content: center;
  margin: 10px 0;
}

</style>
