<script setup>
import { ref, watch } from 'vue'
import api from '@/api'
import { loginClient } from '@/utils/taiKhoanKhachHang.js'
import { showAppToast } from '@/utils/thongBaoDialog.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  initialView: {
    type: String,
    default: 'login'
  }
})

const emit = defineEmits(['update:modelValue', 'login-success'])

// Các màn hình: 'login' | 'register' | 'forgot'
const currentView = ref(props.initialView === 'google' ? 'login' : (props.initialView || 'login'))
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const showPassword = ref(false)
const agreedPrivacy = ref(true)

// Form đăng nhập mật khẩu
const loginForm = ref({
  phoneOrEmail: '',
  password: ''
})

// Form đăng ký tài khoản
const registerForm = ref({
  fullName: '',
  phone: '',
  email: '',
  password: ''
})

// Lỗi validate từng ô của form đăng ký
const registerErrors = ref({
  fullName: '',
  phone: '',
  email: '',
  password: '',
  agreedPrivacy: ''
})

const clearRegisterErrors = () => {
  registerErrors.value = {
    fullName: '',
    phone: '',
    email: '',
    password: '',
    agreedPrivacy: ''
  }
}

const validateRegisterField = (field) => {
  if (field === 'fullName') {
    const val = registerForm.value.fullName.trim()
    if (!val) {
      registerErrors.value.fullName = 'Vui lòng nhập họ và tên của bạn!'
    } else if (val.length < 2) {
      registerErrors.value.fullName = 'Họ và tên phải có ít nhất 2 ký tự!'
    } else {
      registerErrors.value.fullName = ''
    }
  }

  if (field === 'phone') {
    const val = registerForm.value.phone.trim()
    if (!val) {
      registerErrors.value.phone = 'Vui lòng nhập số điện thoại!'
    } else if (!/^0[0-9]{9}$/.test(val)) {
      registerErrors.value.phone = 'Số điện thoại không hợp lệ (phải bắt đầu bằng số 0 và có đúng 10 chữ số)!'
    } else {
      // Nếu trước đó đang có lỗi không phải do trùng thì xóa lỗi định dạng
      if (registerErrors.value.phone && !registerErrors.value.phone.includes('đã được sử dụng')) {
        registerErrors.value.phone = ''
      }
    }
  }

  if (field === 'email') {
    const val = registerForm.value.email.trim()
    if (!val) {
      registerErrors.value.email = 'Vui lòng nhập địa chỉ email!'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
      registerErrors.value.email = 'Email không hợp lệ (ví dụ: hotro@gmail.com)!'
    } else {
      // Nếu trước đó đang có lỗi không phải do trùng thì xóa lỗi định dạng
      if (registerErrors.value.email && !registerErrors.value.email.includes('đã tồn tại')) {
        registerErrors.value.email = ''
      }
    }
  }

  if (field === 'password') {
    const val = registerForm.value.password
    if (!val) {
      registerErrors.value.password = 'Vui lòng nhập mật khẩu!'
    } else if (val.length < 6) {
      registerErrors.value.password = 'Mật khẩu phải chứa ít nhất 6 ký tự!'
    } else {
      registerErrors.value.password = ''
    }
  }

  if (field === 'agreedPrivacy') {
    if (!agreedPrivacy.value) {
      registerErrors.value.agreedPrivacy = 'Vui lòng đồng ý với chính sách bảo mật trước khi đăng ký!'
    } else {
      registerErrors.value.agreedPrivacy = ''
    }
  }
}

// Kiểm tra trùng lặp Số điện thoại ngay khi nhập xong
const checkDuplicatePhone = async () => {
  const val = registerForm.value.phone.trim()
  if (!val || !/^0[0-9]{9}$/.test(val)) return false
  try {
    const res = await api.get('/api/khach-hang/check-phone', { params: { phone: val } })
    if (res.data?.exists) {
      registerErrors.value.phone = 'Số điện thoại này đã được sử dụng trong hệ thống!'
      return true
    } else {
      if (registerErrors.value.phone === 'Số điện thoại này đã được sử dụng trong hệ thống!') {
        registerErrors.value.phone = ''
      }
      return false
    }
  } catch (e) {
    return false
  }
}

// Kiểm tra trùng lặp Email ngay khi nhập xong
let emailDebounceTimer = null
const checkDuplicateEmail = async () => {
  const val = registerForm.value.email.trim()
  if (!val || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return false
  try {
    const res = await api.get('/api/khach-hang/check-email', { params: { email: val } })
    if (res.data?.exists) {
      registerErrors.value.email = 'Email này đã tồn tại trong hệ thống!'
      return true
    } else {
      if (registerErrors.value.email === 'Email này đã tồn tại trong hệ thống!') {
        registerErrors.value.email = ''
      }
      return false
    }
  } catch (e) {
    return false
  }
}

const handlePhoneInput = () => {
  registerForm.value.phone = registerForm.value.phone.replace(/\D/g, '').slice(0, 10)
  validateRegisterField('phone')
  if (registerForm.value.phone.length === 10 && /^0[0-9]{9}$/.test(registerForm.value.phone)) {
    checkDuplicatePhone()
  }
}

const handlePhoneBlur = () => {
  validateRegisterField('phone')
  if (registerForm.value.phone.length === 10 && /^0[0-9]{9}$/.test(registerForm.value.phone)) {
    checkDuplicatePhone()
  }
}

const handlePhoneKeypress = (e) => {
  if (!/[0-9]/.test(e.key)) {
    e.preventDefault()
  }
}

const handleEmailInput = () => {
  validateRegisterField('email')
  const val = registerForm.value.email.trim()
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
    clearTimeout(emailDebounceTimer)
    emailDebounceTimer = setTimeout(() => {
      checkDuplicateEmail()
    }, 400)
  }
}

const handleEmailBlur = () => {
  validateRegisterField('email')
  const val = registerForm.value.email.trim()
  if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
    checkDuplicateEmail()
  }
}

const validateAllRegister = async () => {
  validateRegisterField('fullName')
  validateRegisterField('phone')
  validateRegisterField('email')
  validateRegisterField('password')
  validateRegisterField('agreedPrivacy')

  if (!registerErrors.value.phone && /^0[0-9]{9}$/.test(registerForm.value.phone.trim())) {
    await checkDuplicatePhone()
  }
  if (!registerErrors.value.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.value.email.trim())) {
    await checkDuplicateEmail()
  }

  return !registerErrors.value.fullName &&
         !registerErrors.value.phone &&
         !registerErrors.value.email &&
         !registerErrors.value.password &&
         !registerErrors.value.agreedPrivacy
}

// Form cấp lại mật khẩu
const forgotEmailOrPhone = ref('')
const forgotOtpCode = ref('')
const forgotNewPassword = ref('')
const isForgotOtpSent = ref(false)

const resetRegisterForm = () => {
  registerForm.value = {
    fullName: '',
    phone: '',
    email: '',
    password: ''
  }
  clearRegisterErrors()
}

const resetLoginForm = () => {
  loginForm.value = {
    phoneOrEmail: '',
    password: ''
  }
}

watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    currentView.value = props.initialView === 'google' ? 'login' : (props.initialView || 'login')
    errorMessage.value = ''
    successMessage.value = ''
    resetRegisterForm()
    resetLoginForm()
  }
})

watch(() => props.initialView, (newView) => {
  if (newView) {
    currentView.value = newView === 'google' ? 'login' : newView
    if (newView === 'register') {
      resetRegisterForm()
    }
  }
})

const closeModal = () => {
  emit('update:modelValue', false)
  errorMessage.value = ''
  successMessage.value = ''
  currentView.value = 'login'
  isForgotOtpSent.value = false
  resetRegisterForm()
  resetLoginForm()
}

const goToView = (viewName) => {
  currentView.value = viewName === 'google' ? 'login' : viewName
  errorMessage.value = ''
  successMessage.value = ''
  if (viewName === 'register') {
    resetRegisterForm()
  } else if (viewName === 'login') {
    resetLoginForm()
  }
}

// 1. XỬ LÝ ĐĂNG NHẬP MẬT KHẨU
const handleLogin = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  const identifier = loginForm.value.phoneOrEmail.trim()
  const password = loginForm.value.password

  if (!identifier) {
    errorMessage.value = 'Vui lòng nhập Email hoặc Số điện thoại của bạn!'
    return
  }
  if (!password) {
    errorMessage.value = 'Vui lòng nhập mật khẩu tài khoản!'
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
      showAppToast(`Xin chào ${displayName}! Đăng nhập thành công.`, 'success')
    } else {
      errorMessage.value = res.data?.message || 'Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản hoặc mật khẩu!'
    }
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Đăng nhập thất bại! Vui lòng kiểm tra lại tài khoản và mật khẩu.'
  } finally {
    loading.value = false
  }
}

// 2. XỬ LÝ ĐĂNG KÝ TÀI KHOẢN MỚI
const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  const isValid = await validateAllRegister()
  if (!isValid) {
    return
  }

  const name = registerForm.value.fullName.trim()
  const phone = registerForm.value.phone.trim()
  const email = registerForm.value.email.trim()
  const pass = registerForm.value.password

  loading.value = true
  try {
    const res = await api.post('/api/khach-hang', {
      tenKhachHang: name,
      hoVaTen: name,
      soDienThoai: phone,
      email: email,
      matKhau: pass,
      trangThai: 1
    })

    if (res.data && res.data.id) {
      const createdCustomer = res.data
      const userData = {
        id: createdCustomer.id,
        name: createdCustomer.tenKhachHang || name,
        phone: createdCustomer.soDienThoai || phone,
        email: createdCustomer.email || email,
        maKhachHang: createdCustomer.maKhachHang || '',
        provider: 'local'
      }

      resetRegisterForm()
      loginClient(userData)
      emit('login-success', userData)
      closeModal()
      showAppToast(`🎉 Chào mừng ${userData.name}! Bạn đã đăng ký tài khoản thành công.`, 'success')
    } else {
      errorMessage.value = 'Không thể tạo tài khoản. Vui lòng thử lại sau!'
    }
  } catch (err) {
    console.error('Lỗi đăng ký:', err)
    const backendMsg = err.response?.data?.message || err.response?.data || ''
    const msgStr = typeof backendMsg === 'string' ? backendMsg : JSON.stringify(backendMsg)

    if (/email/i.test(msgStr)) {
      registerErrors.value.email = 'Email này đã tồn tại trong hệ thống!'
      errorMessage.value = ''
    } else if (/số điện thoại|sử dụng|phone/i.test(msgStr)) {
      registerErrors.value.phone = 'Số điện thoại này đã được sử dụng trong hệ thống!'
      errorMessage.value = ''
    } else {
      errorMessage.value = msgStr || 'Đăng ký tài khoản không thành công. Vui lòng thử lại!'
    }
  } finally {
    loading.value = false
  }
}

// 3. XỬ LÝ CẤP LẠI MẬT KHẨU
const handleSendForgotRequest = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  const inputVal = forgotEmailOrPhone.value.trim()

  if (!inputVal) {
    errorMessage.value = 'Vui lòng nhập Email hoặc Số điện thoại để cấp lại mật khẩu!'
    return
  }

  loading.value = true
  try {
    if (inputVal.includes('@')) {
      const res = await api.post('/api/khach-hang/gui-otp', { email: inputVal })
      if (res.data?.success) {
        isForgotOtpSent.value = true
        successMessage.value = `Mã xác thực OTP đã được gửi về email ${inputVal}. Vui lòng kiểm tra hòm thư!`
      } else {
        errorMessage.value = res.data?.message || 'Không thể gửi mã xác nhận. Vui lòng thử lại!'
      }
    } else {
      successMessage.value = `Yêu cầu cấp lại mật khẩu cho số điện thoại ${inputVal} đã được ghi nhận. Vui lòng liên hệ Hotline nếu cần hỗ trợ khẩn cấp!`
    }
  } catch (e) {
    errorMessage.value = 'Không thể gửi yêu cầu cấp lại mật khẩu lúc này. Vui lòng thử lại sau!'
  } finally {
    loading.value = false
  }
}

// Xử lý xác thực OTP đổi mật khẩu mới
const handleResetPasswordWithOtp = async () => {
  errorMessage.value = ''
  const email = forgotEmailOrPhone.value.trim()
  const otp = forgotOtpCode.value.trim()
  const newPass = forgotNewPassword.value.trim()

  if (!otp || otp.length < 6) {
    errorMessage.value = 'Vui lòng nhập đủ 6 chữ số mã OTP!'
    return
  }
  if (!newPass || newPass.length < 6) {
    errorMessage.value = 'Mật khẩu mới phải có ít nhất 6 ký tự!'
    return
  }

  loading.value = true
  try {
    const vRes = await api.post('/api/khach-hang/xac-thuc-otp', { email, otp })
    if (vRes.data?.valid) {
      showAppToast('Xác thực thành công! Mật khẩu của bạn đã được cập nhật.', 'success')
      loginForm.value.phoneOrEmail = email
      loginForm.value.password = newPass
      goToView('login')
      successMessage.value = 'Đã cập nhật mật khẩu mới! Vui lòng đăng nhập lại.'
    } else {
      errorMessage.value = vRes.data?.message || 'Mã xác thực không chính xác hoặc đã hết hạn!'
    }
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Xác thực OTP thất bại!'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-if="modelValue" class="auth-modal-backdrop" @click.self="closeModal">
    <div class="auth-modal-card">
      <!-- Nút đóng đen bo tròn góc trên bên phải -->
      <button class="btn-close-corner" @click="closeModal" title="Đóng">✕</button>

      <!-- ======================================================== -->
      <!-- MÀN HÌNH 1: ĐĂNG NHẬP                                    -->
      <!-- ======================================================== -->
      <div v-if="currentView === 'login'">
        <div class="auth-header-clean">
          <h2 class="auth-modal-title">Đăng nhập</h2>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>
        <div v-if="successMessage && !errorMessage" class="auth-alert-pill success">
          <span>✅ {{ successMessage }}</span>
        </div>

        <form @submit.prevent="handleLogin" class="coolmate-form-body" autocomplete="off">
          <div class="form-field-group">
            <label class="form-input-label">Email hoặc Số điện thoại <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="text"
                v-model="loginForm.phoneOrEmail"
                placeholder="Nhập email hoặc số điện thoại..."
                class="form-pill-input"
                autocomplete="off"
                autofocus
                required
              />
            </div>
          </div>

          <div class="form-field-group">
            <label class="form-input-label">Mật khẩu <span class="req-star">*</span></label>
            <div class="pill-input-field with-eye-wrap">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="loginForm.password"
                placeholder="Nhập mật khẩu..."
                class="form-pill-input"
                autocomplete="current-password"
                required
              />
              <button
                type="button"
                class="btn-eye-toggle"
                @click="showPassword = !showPassword"
                title="Hiện/Ẩn mật khẩu"
              >
                <svg v-if="!showPassword" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </button>
            </div>
          </div>

          <button type="submit" class="btn-pill-submit" :disabled="loading">
            <span v-if="loading">Đang đăng nhập...</span>
            <span v-else>ĐĂNG NHẬP</span>
          </button>
        </form>

        <div class="auth-bottom-links-row">
          <button type="button" class="btn-link-action" @click="goToView('register')">
            Đăng ký tài khoản mới
          </button>
          <button type="button" class="btn-link-action" @click="goToView('forgot')">
            Quên mật khẩu
          </button>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- MÀN HÌNH 2: ĐĂNG KÝ TÀI KHOẢN                            -->
      <!-- ======================================================== -->
      <div v-else-if="currentView === 'register'">
        <div class="auth-header-clean">
          <h2 class="auth-modal-title">Đăng ký tài khoản</h2>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>

        <form @submit.prevent="handleRegister" class="coolmate-form-body" novalidate autocomplete="off">
          <div class="form-field-group">
            <label class="form-input-label">Họ và tên <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="text"
                v-model="registerForm.fullName"
                placeholder="Nhập họ và tên..."
                class="form-pill-input"
                :class="{ 'input-error': registerErrors.fullName }"
                autocomplete="off"
                @input="validateRegisterField('fullName')"
                @blur="validateRegisterField('fullName')"
                autofocus
              />
            </div>
            <span v-if="registerErrors.fullName" class="field-error-text">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              {{ registerErrors.fullName }}
            </span>
          </div>

          <div class="form-field-group">
            <label class="form-input-label">Số điện thoại <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="tel"
                v-model="registerForm.phone"
                placeholder="Nhập số điện thoại (10 chữ số)..."
                class="form-pill-input"
                :class="{ 'input-error': registerErrors.phone }"
                maxlength="10"
                autocomplete="off"
                @input="handlePhoneInput"
                @keypress="handlePhoneKeypress"
                @blur="handlePhoneBlur"
              />
            </div>
            <span v-if="registerErrors.phone" class="field-error-text">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              {{ registerErrors.phone }}
            </span>
          </div>

          <div class="form-field-group">
            <label class="form-input-label">Email <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="email"
                v-model="registerForm.email"
                placeholder="Nhập địa chỉ email..."
                class="form-pill-input"
                :class="{ 'input-error': registerErrors.email }"
                autocomplete="off"
                @input="handleEmailInput"
                @blur="handleEmailBlur"
              />
            </div>
            <span v-if="registerErrors.email" class="field-error-text">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              {{ registerErrors.email }}
            </span>
          </div>

          <div class="form-field-group">
            <label class="form-input-label">Mật khẩu <span class="req-star">*</span></label>
            <div class="pill-input-field with-eye-wrap">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="registerForm.password"
                placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)..."
                class="form-pill-input"
                :class="{ 'input-error': registerErrors.password }"
                autocomplete="new-password"
                @input="validateRegisterField('password')"
                @blur="validateRegisterField('password')"
              />
              <button
                type="button"
                class="btn-eye-toggle"
                @click="showPassword = !showPassword"
                title="Hiện/Ẩn mật khẩu"
              >
                <svg v-if="!showPassword" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </button>
            </div>
            <span v-if="registerErrors.password" class="field-error-text">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              {{ registerErrors.password }}
            </span>
          </div>

          <div class="form-field-group">
            <label class="privacy-consent-row">
              <input type="checkbox" v-model="agreedPrivacy" @change="validateRegisterField('agreedPrivacy')" />
              <span>Tôi đã đọc và đồng ý với <a href="javascript:void(0)" class="link-inline" @click="showAppToast('Thông tin được lưu trữ bảo mật cho mục đích mua sắm.', 'info')">chính sách bảo mật</a></span>
            </label>
            <span v-if="registerErrors.agreedPrivacy" class="field-error-text">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              {{ registerErrors.agreedPrivacy }}
            </span>
          </div>

          <button type="submit" class="btn-pill-submit" :disabled="loading">
            <span v-if="loading">Đang đăng ký...</span>
            <span v-else>ĐĂNG KÝ TÀI KHOẢN</span>
          </button>
        </form>

        <div class="auth-bottom-links-row">
          <button type="button" class="btn-link-action" @click="goToView('login')">
            Đã có tài khoản? Đăng nhập
          </button>
          <button type="button" class="btn-link-action" @click="goToView('forgot')">
            Quên mật khẩu
          </button>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- MÀN HÌNH 3: CẤP LẠI MẬT KHẨU                             -->
      <!-- ======================================================== -->
      <div v-else-if="currentView === 'forgot'" class="forgot-pwd-wrap">
        <div class="auth-header-clean">
          <h2 class="auth-modal-title">Cấp lại mật khẩu</h2>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>
        <div v-if="successMessage && !errorMessage" class="auth-alert-pill success">
          <span>✅ {{ successMessage }}</span>
        </div>

        <form v-if="!isForgotOtpSent" @submit.prevent="handleSendForgotRequest" class="coolmate-form-body">
          <div class="form-field-group">
            <label class="form-input-label">Email hoặc Số điện thoại <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="text"
                v-model="forgotEmailOrPhone"
                placeholder="Nhập email hoặc số điện thoại của bạn..."
                class="form-pill-input"
                autofocus
                required
              />
            </div>
          </div>

          <div class="cloudflare-success-box">
            <div class="cf-left">
              <span class="cf-check">✓</span>
              <strong class="cf-status">Success!</strong>
            </div>
            <div class="cf-brand">
              <span class="cf-logo-name">🔒 CLOUDFLARE</span>
              <span class="cf-links">Privacy • Help</span>
            </div>
          </div>

          <button type="submit" class="btn-pill-submit" :disabled="loading">
            <span v-if="loading">Đang gửi yêu cầu...</span>
            <span v-else>Gửi yêu cầu OTP</span>
          </button>
        </form>

        <form v-else @submit.prevent="handleResetPasswordWithOtp" class="coolmate-form-body">
          <div class="form-field-group">
            <label class="form-input-label">Mã xác thực OTP (6 chữ số) <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="text"
                v-model="forgotOtpCode"
                placeholder="Nhập mã OTP 6 số..."
                maxlength="6"
                class="form-pill-input text-center font-bold"
                autofocus
                required
              />
            </div>
          </div>

          <div class="form-field-group">
            <label class="form-input-label">Mật khẩu mới <span class="req-star">*</span></label>
            <div class="pill-input-field">
              <input
                type="password"
                v-model="forgotNewPassword"
                placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)..."
                class="form-pill-input"
                required
              />
            </div>
          </div>

          <button type="submit" class="btn-pill-submit" :disabled="loading">
            <span v-if="loading">Đang cập nhật...</span>
            <span v-else>XÁC NHẬN ĐỔI MẬT KHẨU</span>
          </button>
        </form>

        <div class="auth-bottom-links-row justify-center mt-3">
          <button type="button" class="btn-link-action" @click="goToView('login')">
            ← Quay lại Đăng nhập
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Lớp mờ hậu cảnh */
.auth-modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.72);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 16px;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Thẻ Modal chuẩn giao diện với màu & thương hiệu FF */
.auth-modal-card {
  position: relative;
  background-color: #ffffff;
  width: 100%;
  max-width: 460px;
  border-radius: 20px;
  padding: 30px 28px 24px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.35);
  border: 1px solid #e2e8f0;
  animation: scaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

@keyframes scaleUp {
  from { transform: scale(0.94); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

/* Nút tắt đen góc trên bên phải */
.btn-close-corner {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 30px;
  height: 30px;
  background-color: #000000;
  color: #ffffff;
  border: 2px solid #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(0,0,0,0.3);
  transition: transform 0.2s ease;
  z-index: 2;
}

.btn-close-corner:hover {
  transform: scale(1.1);
}

/* Header tiêu đề gọn gàng */
.auth-header-clean {
  text-align: center;
  margin-bottom: 20px;
}

.auth-modal-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.3px;
}

/* Nhóm trường nhập liệu và nhãn Label */
.form-field-group {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.form-input-label {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
  text-align: left;
}

.req-star {
  color: #ef4444;
  margin-left: 2px;
  font-weight: 700;
}

/* Thông báo lỗi / thành công dạng pill */
.auth-alert-pill {
  padding: 9px 16px;
  border-radius: 10px;
  font-size: 13px;
  margin-bottom: 14px;
  text-align: center;
  font-weight: 600;
}

.auth-alert-pill.error {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.auth-alert-pill.success {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

/* Form Body */
.coolmate-form-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pill-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.pill-input-field {
  position: relative;
}

/* Ô nhập chuẩn giao diện form hiện đại */
.form-pill-input {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  border: 1.5px solid #cbd5e1;
  padding: 0 14px;
  font-size: 14px;
  color: #0f172a;
  background-color: #ffffff;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.form-pill-input:focus {
  outline: none;
  border-color: #0f172a;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.08);
}

.form-pill-input.input-error {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}

.form-pill-input.input-error:focus {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
}

.field-error-text {
  font-size: 12.5px;
  color: #ef4444;
  margin-top: 5px;
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
  text-align: left;
  line-height: 1.35;
}

.field-error-text svg {
  flex-shrink: 0;
}

.form-pill-input::placeholder {
  color: #94a3b8;
  font-size: 13.5px;
}

.with-eye-wrap .form-pill-input {
  padding-right: 44px;
}

.btn-eye-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

/* Checkbox chính sách bảo mật */
.privacy-consent-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  padding: 2px 0;
}

.privacy-consent-row input {
  width: 16px;
  height: 16px;
  accent-color: #0f172a;
  cursor: pointer;
}

.link-inline {
  color: #1d4ed8;
  text-decoration: underline;
  font-weight: 600;
}

/* Nút Submit chuẩn giao diện */
.btn-pill-submit {
  width: 100%;
  height: 46px;
  border-radius: 8px;
  background-color: #000000;
  color: #ffffff;
  border: none;
  font-size: 14.5px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
  letter-spacing: 0.5px;
  margin-top: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.18);
}

.btn-pill-submit:hover:not(:disabled) {
  background-color: #d4a373;
  color: #0f172a;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(212, 163, 115, 0.4);
}

.btn-pill-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Hai liên kết chân form trái & phải */
.auth-bottom-links-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
  padding: 0 2px;
}

.btn-link-action {
  background: none;
  border: none;
  color: #1d4ed8;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.btn-link-action:hover {
  text-decoration: underline;
  color: #0f172a;
}

/* Cloudflare box ở quên mật khẩu */
.cloudflare-success-box {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  border-radius: 6px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 8px;
}

.cf-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cf-check {
  width: 22px;
  height: 22px;
  background: #16a34a;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 900;
}

.cf-status {
  font-size: 14px;
  color: #0f172a;
}

.cf-brand {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.cf-logo-name {
  font-size: 10px;
  font-weight: 800;
  color: #f97316;
  letter-spacing: 0.5px;
}

.cf-links {
  font-size: 9px;
  color: #71717a;
}

.justify-center {
  justify-content: center !important;
}

.text-center {
  text-align: center;
}

.font-bold {
  font-weight: 800;
}

.mt-3 {
  margin-top: 14px;
}
</style>
