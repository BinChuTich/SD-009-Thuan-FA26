<script setup>
import { ref, watch, onMounted } from 'vue'
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

// Các màn hình: 'login' | 'register' | 'forgot' | 'google'
const currentView = ref(props.initialView || 'login')
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

// Form liên kết Google / Gmail (Không dùng tài khoản định sẵn)
const googleInputEmail = ref('')

// Form cấp lại mật khẩu
const forgotEmailOrPhone = ref('')
const forgotOtpCode = ref('')
const forgotNewPassword = ref('')
const isForgotOtpSent = ref(false)

// Quản lý các tài khoản Google đã đăng nhập thực tế trên máy / thiết bị này
const deviceGoogleAccounts = ref([])
const showCustomGoogleInput = ref(false)

const loadDeviceAccounts = () => {
  try {
    const rawList = localStorage.getItem('fftshirt_device_google_accounts')
    let list = rawList ? JSON.parse(rawList) : []
    
    // Nếu chưa có tài khoản nào được lưu trên máy này, khởi tạo danh sách tài khoản tiện ích
    if (!list || list.length === 0) {
      list = [
        { name: 'Cười Ngắm Hoa', email: 'cuoinguangamhoa@gmail.com', avatarBg: '#10b981' },
        { name: 'Dương Ngọc Chiến', email: 'duongngocchien2003@gmail.com', avatarBg: '#3b82f6' },
        { name: 'Chiến Dương Ngọc', email: '2duong0ngoc0chien3@gmail.com', avatarBg: '#f59e0b' }
      ]
      localStorage.setItem('fftshirt_device_google_accounts', JSON.stringify(list))
    }

    // Kiểm tra thêm tài khoản client_user hiện tại nếu đăng nhập qua Google
    const rawUser = localStorage.getItem('client_user')
    if (rawUser) {
      const user = JSON.parse(rawUser)
      if (user && user.email && user.provider === 'google' && !list.some(a => a.email.toLowerCase() === user.email.toLowerCase())) {
        list.unshift({ email: user.email, name: user.name || user.email.split('@')[0], avatarBg: '#ef4444' })
      }
    }

    deviceGoogleAccounts.value = list
  } catch (e) {
    deviceGoogleAccounts.value = []
  }
}

const saveDeviceAccount = (acc) => {
  try {
    let list = deviceGoogleAccounts.value.filter(a => a.email.toLowerCase() !== acc.email.toLowerCase())
    list.unshift(acc)
    if (list.length > 6) list = list.slice(0, 6)
    deviceGoogleAccounts.value = list
    localStorage.setItem('fftshirt_device_google_accounts', JSON.stringify(list))
  } catch (e) {}
}

const selectAccountAndLogin = async (email, name) => {
  if (!email) return
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await api.post('/api/khach-hang/dang-nhap-google', {
      email: email.trim().toLowerCase(),
      name: name || email.split('@')[0]
    })

    if (res.data?.success && res.data?.customer) {
      const customer = res.data.customer
      const displayName = customer.tenKhachHang || customer.hoVaTen || name || email.split('@')[0]
      const userData = {
        id: customer.id,
        name: displayName,
        phone: customer.soDienThoai || '',
        email: customer.email || email,
        maKhachHang: customer.maKhachHang || '',
        provider: 'google',
        avatarText: displayName.charAt(0).toUpperCase(),
        avatarBg: '#ea4335'
      }

      saveDeviceAccount({ email, name: displayName, avatarBg: '#3b82f6' })
      loginClient(userData)
      emit('login-success', userData)
      closeModal()
      showAppToast(`Xin chào ${displayName}! Đã đăng nhập bằng Google thành công.`, 'success')
      return
    } else {
      errorMessage.value = res.data?.message || 'Không thể đăng nhập với tài khoản này.'
    }
  } catch (err) {
    console.error('Lỗi đăng nhập tài khoản Google:', err)
    errorMessage.value = err.response?.data?.message || 'Đăng nhập không thành công. Vui lòng thử lại!'
  } finally {
    loading.value = false
  }
}

const removeDeviceAccount = (emailToRemove, event) => {
  if (event) event.stopPropagation()
  deviceGoogleAccounts.value = deviceGoogleAccounts.value.filter(
    a => a.email.toLowerCase() !== emailToRemove.toLowerCase()
  )
  try {
    localStorage.setItem('fftshirt_device_google_accounts', JSON.stringify(deviceGoogleAccounts.value))
  } catch (e) {}
}

onMounted(() => {
  loadDeviceAccounts()
})

const handleLoginWithAccount = (acc) => {
  if (acc && acc.email) {
    googleInputEmail.value = acc.email
    handleGoogleLoginSubmit()
  }
}

watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    currentView.value = props.initialView || 'login'
    errorMessage.value = ''
    successMessage.value = ''
    googleInputEmail.value = ''
    showCustomGoogleInput.value = false
    loadDeviceAccounts()
  }
})

watch(() => props.initialView, (newView) => {
  if (newView) {
    currentView.value = newView
  }
})

const closeModal = () => {
  emit('update:modelValue', false)
  errorMessage.value = ''
  successMessage.value = ''
  currentView.value = 'login'
  isForgotOtpSent.value = false
}

const goToView = (viewName) => {
  currentView.value = viewName
  errorMessage.value = ''
  successMessage.value = ''
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

            localStorage.setItem('fftshirt_last_google_account', JSON.stringify({
        email: userData.email,
        name: displayName
      }))
      rememberedGoogleAccount.value = {
        email: userData.email,
        name: displayName
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

// 2. XỬ LÝ LIÊN KẾT ĐĂNG NHẬP BẰNG TÀI KHOẢN GOOGLE
const handleInstantGoogleLogin = () => {
  goToView('google')
}

const handleGoogleLoginSubmit = async () => {
  errorMessage.value = ''
  let email = googleInputEmail.value.trim().toLowerCase()
  if (!email) {
    errorMessage.value = 'Vui lòng nhập Email hoặc tên tài khoản Google của bạn!'
    return
  }
  if (!email.includes('@')) {
    email += '@gmail.com'
  }
  await selectAccountAndLogin(email, email.split('@')[0])
}

// 3. XỬ LÝ ĐĂNG KÝ TÀI KHOẢN MỚI
const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''

  const name = registerForm.value.fullName.trim()
  const phone = registerForm.value.phone.trim()
  const email = registerForm.value.email.trim()
  const pass = registerForm.value.password

  if (!name || name.length < 2) {
    errorMessage.value = 'Vui lòng nhập họ và tên của bạn!'
    return
  }
  if (!phone || !/^(0[3|5|7|8|9])+([0-9]{8})$/.test(phone)) {
    errorMessage.value = 'Số điện thoại không hợp lệ (gồm 10 chữ số đúng định dạng Việt Nam)!'
    return
  }
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errorMessage.value = 'Vui lòng nhập đúng định dạng Email!'
    return
  }
  if (!pass || pass.length < 6) {
    errorMessage.value = 'Mật khẩu phải chứa ít nhất 6 ký tự!'
    return
  }
  if (!agreedPrivacy.value) {
    errorMessage.value = 'Vui lòng đồng ý với chính sách bảo mật trước khi đăng ký!'
    return
  }

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

      loginClient(userData)
      emit('login-success', userData)
      closeModal()
      showAppToast(`🎉 Chào mừng ${userData.name}! Bạn đã đăng ký tài khoản thành công.`, 'success')
    } else {
      errorMessage.value = 'Không thể tạo tài khoản. Vui lòng thử lại sau!'
    }
  } catch (err) {
    console.error('Lỗi đăng ký:', err)
    errorMessage.value = err.response?.data?.message || 'Email hoặc số điện thoại này đã được đăng ký trên hệ thống!'
  } finally {
    loading.value = false
  }
}

// 4. XỬ LÝ CẤP LẠI MẬT KHẨU
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
    <div class="auth-modal-card" :class="{ 'google-modal-card': currentView === 'google' }">
      <!-- Nút đóng đen bo tròn góc trên bên phải -->
      <button class="btn-close-corner" @click="closeModal" title="Đóng">✕</button>

      <!-- ======================================================== -->
      <!-- MÀN HÌNH 1: ĐĂNG NHẬP                                    -->
      <!-- ======================================================== -->
      <div v-if="currentView === 'login'">
        <div class="auth-header-clean">
          <h2 class="auth-modal-title">Đăng nhập</h2>
          <p class="auth-modal-subtitle">Nhập email hoặc số điện thoại để tiếp tục mua sắm</p>
        </div>

        <!-- Nút TIẾP TỤC VỚI GOOGLE (Mở bảng chọn tài khoản Google chuẩn Coolmate) -->
        <button type="button" class="btn-google-action btn-google-instant" :disabled="loading" @click="goToView('google')">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>Tiếp tục với Google</span>
        </button>

        <div class="auth-divider-line">
          <span>Hoặc đăng nhập với mật khẩu</span>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>
        <div v-if="successMessage && !errorMessage" class="auth-alert-pill success">
          <span>✅ {{ successMessage }}</span>
        </div>

        <form @submit.prevent="handleLogin" class="coolmate-form-body">
          <div class="pill-input-field">
            <input
              type="text"
              v-model="loginForm.phoneOrEmail"
              placeholder="Email/SĐT của bạn"
              class="form-pill-input"
              autofocus
              required
            />
          </div>

          <div class="pill-input-field with-eye-wrap">
            <input
              :type="showPassword ? 'text' : 'password'"
              v-model="loginForm.password"
              placeholder="Mật khẩu"
              class="form-pill-input"
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
      <!-- MÀN HÌNH 2: ĐĂNG NHẬP GOOGLE (CHỌN TÀI KHOẢN CHUẨN COOLMATE) -->
      <!-- ======================================================== -->
      <div v-else-if="currentView === 'google'" class="gac-container">
        <!-- Top Google Header -->
        <div class="gac-top-bar">
          <div class="gac-google-brand">
            <svg viewBox="0 0 24 24" width="22" height="22">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
            </svg>
            <span class="gac-google-title">Đăng nhập bằng Google</span>
          </div>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error my-2">
          <span>⚠️ {{ errorMessage }}</span>
        </div>
        <div v-if="successMessage && !errorMessage" class="auth-alert-pill success my-2">
          <span>✅ {{ successMessage }}</span>
        </div>

        <!-- Chế độ 1: Danh sách Chọn tài khoản thực tế trên máy -->
        <div v-if="!showCustomGoogleInput" class="gac-main-grid">
          <!-- Cột trái: Thương hiệu & Tiêu đề -->
          <div class="gac-left-col">
            <div class="gac-store-badge">
              <span class="gac-store-box">FF</span>
              <span class="gac-store-sub">T-SHIRT</span>
            </div>
            <h2 class="gac-heading">Chọn tài khoản</h2>
            <p class="gac-subheading">Tiếp tục tới <span class="gac-app-name">FF T-Shirt Store</span></p>
          </div>

          <!-- Cột phải: Danh sách tài khoản thực tế trên máy -->
          <div class="gac-right-col">
            <div class="gac-account-list">
              <template v-for="(acc, aIdx) in deviceGoogleAccounts" :key="acc.email">
                <div class="gac-account-row">
                  <button
                    type="button"
                    class="gac-account-item"
                    :disabled="loading"
                    @click="selectAccountAndLogin(acc.email, acc.name)"
                  >
                    <div class="gac-avatar" :style="{ backgroundColor: acc.avatarBg || '#3b82f6' }">
                      <span>{{ (acc.name || acc.email || 'G').charAt(0).toUpperCase() }}</span>
                    </div>
                    <div class="gac-account-info">
                      <strong class="gac-account-name">{{ acc.name || acc.email.split('@')[0] }}</strong>
                      <span class="gac-account-email">{{ acc.email }}</span>
                    </div>
                  </button>
                  <button
                    type="button"
                    class="btn-remove-device-acc"
                    @click="removeDeviceAccount(acc.email, $event)"
                    title="Xóa tài khoản này khỏi máy"
                  >
                    ✕
                  </button>
                </div>
                <div class="gac-item-divider"></div>
              </template>

              <!-- Nút: Sử dụng một tài khoản khác -->
              <button
                type="button"
                class="gac-account-item gac-other-item"
                @click="showCustomGoogleInput = true; googleInputEmail = ''"
              >
                <div class="gac-avatar gac-avatar-other">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </div>
                <div class="gac-account-info">
                  <span class="gac-other-text">Sử dụng một tài khoản khác</span>
                </div>
              </button>

              <div class="gac-item-divider"></div>
            </div>

            <!-- Ghi chú điều khoản & chính sách quyền riêng tư chuẩn Google (Ảnh 2) -->
            <p class="gac-disclaimer">
              Trước khi sử dụng FF T-Shirt Store, bạn có thể xem <a href="#" class="gac-link">Chính sách quyền riêng tư</a> và <a href="#" class="gac-link">Điều khoản dịch vụ</a> của ứng dụng này.
            </p>
          </div>
        </div>

        <!-- Chế độ 2: Nhập tài khoản Google -->
        <div v-else class="gac-manual-box">
          <div class="gac-manual-header">
            <h3 class="gac-manual-title">Đăng nhập tài khoản Google</h3>
            <p class="gac-manual-sub">Nhập email hoặc số điện thoại Google của bạn để liên kết đăng nhập</p>
          </div>

          <form @submit.prevent="handleGoogleLoginSubmit" class="coolmate-form-body">
            <div class="google-outline-input-box">
              <label class="google-floating-label">Email hoặc số điện thoại Google</label>
              <input
                type="text"
                v-model="googleInputEmail"
                class="google-outline-input"
                placeholder="VD: cuoinguangamhoa@gmail.com"
                autofocus
              />
            </div>

            <div class="google-helper-note">
              <span>Hệ thống liên kết trực tiếp với tài khoản Google để đăng nhập an toàn, nhanh chóng.</span>
            </div>

            <div class="google-actions-row">
              <button
                type="button"
                class="btn-google-create"
                @click="showCustomGoogleInput = false"
              >
                ← Quay lại danh sách tài khoản
              </button>
              
              <button type="submit" class="btn-google-next" :disabled="loading">
                <span v-if="loading">Đang kết nối...</span>
                <span v-else>Tiếp theo</span>
              </button>
            </div>
          </form>
        </div>

        <!-- Chân trang chuẩn giao diện Google (Ảnh 2) -->
        <div class="gac-footer-row">
          <div class="gac-lang-btn">
            <span>Tiếng Việt</span>
            <span class="gac-caret">▾</span>
          </div>
          <div class="gac-foot-links">
            <a href="#" class="gac-foot-item">Trợ giúp</a>
            <a href="#" class="gac-foot-item">Quyền riêng tư</a>
            <a href="#" class="gac-foot-item">Điều khoản</a>
          </div>
        </div>

        <div class="auth-bottom-links-row justify-center mt-3">
          <button type="button" class="btn-link-action" @click="goToView('login')">
            ← Quay lại Đăng nhập bằng mật khẩu
          </button>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- MÀN HÌNH 3: ĐĂNG KÝ TÀI KHOẢN                            -->
      <!-- ======================================================== -->
      <div v-else-if="currentView === 'register'">
        <div class="auth-header-clean">
          <h2 class="auth-modal-title">Đăng ký tài khoản</h2>
          <p class="auth-modal-subtitle">Tạo tài khoản để nhận ngay ưu đãi mua sắm</p>
        </div>

        <button type="button" class="btn-google-action" @click="goToView('google')">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>Đăng ký nhanh với Google</span>
        </button>

        <div class="auth-divider-line">
          <span>Hoặc điền biểu mẫu đăng ký</span>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>

        <form @submit.prevent="handleRegister" class="coolmate-form-body">
          <div class="pill-grid-2">
            <input
              type="text"
              v-model="registerForm.fullName"
              placeholder="Họ và tên"
              class="form-pill-input"
              autofocus
              required
            />
            <input
              type="tel"
              v-model="registerForm.phone"
              placeholder="Số điện thoại"
              class="form-pill-input"
              required
            />
          </div>

          <div class="pill-input-field">
            <input
              type="email"
              v-model="registerForm.email"
              placeholder="Email"
              class="form-pill-input"
              required
            />
          </div>

          <div class="pill-input-field with-eye-wrap">
            <input
              :type="showPassword ? 'text' : 'password'"
              v-model="registerForm.password"
              placeholder="Mật khẩu (tối thiểu 6 ký tự)"
              class="form-pill-input"
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

          <label class="privacy-consent-row">
            <input type="checkbox" v-model="agreedPrivacy" />
            <span>Tôi đã đọc và đồng ý với <a href="javascript:void(0)" class="link-inline" @click="showAppToast('Thông tin được lưu trữ bảo mật cho mục đích mua sắm.', 'info')">chính sách bảo mật</a></span>
          </label>

          <button type="submit" class="btn-pill-submit" :disabled="loading">
            <span v-if="loading">Đang đăng ký...</span>
            <span v-else>ĐĂNG KÝ TÀI KHOẢN</span>
          </button>
        </form>

        <div class="auth-bottom-links-row">
          <button type="button" class="btn-link-action" @click="goToView('login')">
            Đăng nhập
          </button>
          <button type="button" class="btn-link-action" @click="goToView('forgot')">
            Quên mật khẩu
          </button>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- MÀN HÌNH 4: CẤP LẠI MẬT KHẨU                             -->
      <!-- ======================================================== -->
      <div v-else-if="currentView === 'forgot'" class="forgot-pwd-wrap">
        <div class="auth-header-clean">
          <h2 class="auth-modal-title">Cấp lại mật khẩu</h2>
          <p class="auth-modal-subtitle">Nhập email hoặc số điện thoại để nhận mã xác thực OTP</p>
        </div>

        <div v-if="errorMessage" class="auth-alert-pill error">
          <span>⚠️ {{ errorMessage }}</span>
        </div>
        <div v-if="successMessage && !errorMessage" class="auth-alert-pill success">
          <span>✅ {{ successMessage }}</span>
        </div>

        <form v-if="!isForgotOtpSent" @submit.prevent="handleSendForgotRequest" class="coolmate-form-body">
          <div class="fieldset-pill-box">
            <span class="fieldset-pill-legend">Email/SĐT của bạn</span>
            <input
              type="text"
              v-model="forgotEmailOrPhone"
              placeholder=""
              class="form-fieldset-input"
              autofocus
              required
            />
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
            <span v-else>Gửi yêu cầu</span>
          </button>
        </form>

        <form v-else @submit.prevent="handleResetPasswordWithOtp" class="coolmate-form-body">
          <div class="pill-input-field">
            <input
              type="text"
              v-model="forgotOtpCode"
              placeholder="Nhập mã OTP 6 số"
              maxlength="6"
              class="form-pill-input text-center font-bold"
              autofocus
              required
            />
          </div>

          <div class="pill-input-field">
            <input
              type="password"
              v-model="forgotNewPassword"
              placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)"
              class="form-pill-input"
              required
            />
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
  max-width: 440px;
  border-radius: 24px;
  padding: 34px 30px 28px;
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
  margin-bottom: 18px;
}

.auth-modal-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 6px;
  letter-spacing: -0.3px;
}

.auth-modal-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

/* Nút Google Action */
.btn-google-action {
  width: 100%;
  height: 46px;
  background-color: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.btn-google-action:hover {
  background-color: #f8fafc;
  border-color: #94a3b8;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0,0,0,0.08);
}

/* Vạch phân cách 'Hoặc' */
.auth-divider-line {
  position: relative;
  text-align: center;
  margin: 16px 0 16px;
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
  font-size: 12.5px;
  color: #64748b;
  font-weight: 600;
}

/* ======================================= */
/* MÀN HÌNH GOOGLE CHUẨN THIẾT KẾ GOOGLE   */
/* ======================================= */
.google-remembered-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1.5px solid #4285f4;
  background: #f8faff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 10px;
  margin-bottom: 6px;
}
.google-remembered-card:hover {
  background: #edf3ff;
  border-color: #2b6cb0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(66, 133, 244, 0.15);
}
.gra-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ea4335;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}
.gra-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  text-align: left;
}
.gra-name {
  font-size: 14px;
  color: #0f172a;
  font-weight: 700;
}
.gra-email {
  font-size: 12px;
  color: #64748b;
}
.btn-gra-quick {
  background: #4285f4;
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.btn-gra-quick:hover {
  background: #3367d6;
}

.google-alt-link-row {
  text-align: center;
  margin-top: 6px;
  margin-bottom: 12px;
}
.btn-alt-google {
  background: none;
  border: none;
  font-size: 12px;
  color: #4285f4;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
}
.btn-alt-google:hover {
  color: #1a73e8;
}
.mb-3 {
  margin-bottom: 12px !important;
}
.my-3 {
  margin-top: 12px !important;
  margin-bottom: 12px !important;
}

.google-auth-pane {
  text-align: center;
}

.google-logo-head {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.google-pane-title {
  font-size: 22px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 4px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.google-pane-sub {
  font-size: 14px;
  color: #5f6368;
  margin: 0 0 16px;
}

.google-outline-input-box {
  position: relative;
  border: 1.5px solid #dadce0;
  border-radius: 6px;
  padding: 10px 14px;
  text-align: left;
  transition: border-color 0.2s;
  background: #ffffff;
}

.google-outline-input-box:focus-within {
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8;
}

.google-floating-label {
  display: block;
  font-size: 12px;
  color: #5f6368;
  margin-bottom: 2px;
}

.google-outline-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 15px;
  color: #202124;
  background: transparent;
  padding: 0;
}

.google-helper-note {
  font-size: 12px;
  color: #5f6368;
  text-align: left;
  line-height: 1.45;
  margin-top: 4px;
}

.google-actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

.btn-google-create {
  background: none;
  border: none;
  color: #1a73e8;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  padding: 8px 0;
  transition: opacity 0.2s;
}

.btn-google-create:hover {
  text-decoration: underline;
}

.btn-google-next {
  background-color: #1a73e8;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 9px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, box-shadow 0.2s;
}

.btn-google-next:hover:not(:disabled) {
  background-color: #1557b0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
}

.btn-google-next:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Thông báo lỗi / thành công dạng pill */
.auth-alert-pill {
  padding: 9px 16px;
  border-radius: 999px;
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
  gap: 12px;
}

.pill-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.pill-input-field {
  position: relative;
}

/* Ô nhập bo tròn toàn phần */
.form-pill-input {
  width: 100%;
  height: 48px;
  border-radius: 999px;
  border: 1.5px solid #e2e8f0;
  padding: 0 20px;
  font-size: 14px;
  color: #0f172a;
  background-color: #ffffff;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.form-pill-input:focus {
  outline: none;
  border-color: #d4a373;
  box-shadow: 0 0 0 3px rgba(212, 163, 115, 0.25);
}

.form-pill-input::placeholder {
  color: #94a3b8;
  font-size: 14px;
}

.with-eye-wrap .form-pill-input {
  padding-right: 48px;
}

.btn-eye-toggle {
  position: absolute;
  right: 16px;
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
  padding: 2px 4px;
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

/* Nút Submit bo tròn ĐEN / VÀNG thương hiệu */
.btn-pill-submit {
  width: 100%;
  height: 48px;
  border-radius: 999px;
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
  padding: 0 4px;
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

/* ======================================= */
/* MÀN HÌNH 4: CẤP LẠI MẬT KHẨU            */
/* ======================================= */
.fieldset-pill-box {
  position: relative;
  border: 1.5px solid #71717a;
  border-radius: 999px;
  padding: 4px 18px;
  margin-bottom: 6px;
}

.fieldset-pill-legend {
  position: absolute;
  top: -10px;
  left: 20px;
  background: #ffffff;
  padding: 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: #3f3f46;
}

.form-fieldset-input {
  width: 100%;
  height: 40px;
  border: none;
  outline: none;
  font-size: 15px;
  color: #0f172a;
  background: transparent;
}

.cloudflare-success-box {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  border-radius: 4px;
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0 14px;
}

.cf-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cf-check {
  width: 24px;
  height: 24px;
  background: #16a34a;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 900;
}

.cf-status {
  font-size: 15px;
  color: #0f172a;
}

.cf-brand {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.25;
}

.cf-logo-name {
  font-size: 10.5px;
  font-weight: 800;
  color: #f97316;
  letter-spacing: 0.5px;
}

.cf-links {
  font-size: 9.5px;
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

/* ======================================================== */
/* GOOGLE ACCOUNT CHOOSER CHUẨN THIẾT KẾ GOOGLE (ẢNH 1)      */
/* ======================================================== */
.google-modal-card {
  max-width: 760px !important;
  width: 95% !important;
  padding: 24px 32px 20px !important;
  border-radius: 28px !important;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.28) !important;
}

.gac-container {
  display: flex;
  flex-direction: column;
}

.gac-top-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.gac-google-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.gac-google-title {
  font-size: 14px;
  font-weight: 500;
  color: #3c4043;
  letter-spacing: -0.1px;
}

.gac-main-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 36px;
  padding-bottom: 24px;
}

.gac-left-col {
  display: flex;
  flex-direction: column;
}

.gac-store-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 16px;
}

.gac-store-box {
  background: #000000;
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
  padding: 4px 8px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.gac-store-sub {
  font-size: 11px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: 1px;
}

.gac-heading {
  font-size: 32px;
  font-weight: 400;
  color: #1f1f1f;
  margin: 0 0 10px;
  line-height: 1.25;
  letter-spacing: -0.5px;
}

.gac-subheading {
  font-size: 14.5px;
  color: #444746;
  margin: 0;
  line-height: 1.4;
}

.gac-app-name {
  color: #0b57d0;
  font-weight: 600;
}

.gac-right-col {
  display: flex;
  flex-direction: column;
}

.gac-account-list {
  display: flex;
  flex-direction: column;
}

.gac-account-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  background: transparent;
  border: none;
  border-radius: 12px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease-in-out;
}

.gac-account-item:hover {
  background-color: #f0f4f9;
}

.gac-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  flex-shrink: 0;
}

.gac-avatar-c1 {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.gac-avatar-c2 {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.gac-avatar-recent {
  background: linear-gradient(135deg, #ea4335 0%, #c5221f 100%);
}

.gac-avatar-other {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.gac-account-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.gac-account-name {
  font-size: 14.5px;
  font-weight: 600;
  color: #1f1f1f;
  line-height: 1.25;
}

.gac-account-email {
  font-size: 12.5px;
  color: #444746;
}

.gac-other-text {
  font-size: 14px;
  font-weight: 500;
  color: #1f1f1f;
}

.gac-item-divider {
  height: 1px;
  background-color: #e0e2ec;
  width: 100%;
}

.gac-disclaimer {
  font-size: 12px;
  color: #444746;
  line-height: 1.55;
  margin: 16px 0 0;
  padding: 0 4px;
}

.gac-link {
  color: #0b57d0;
  text-decoration: none;
}

.gac-link:hover {
  text-decoration: underline;
}

.gac-manual-box {
  padding: 10px 0 20px;
}

.gac-manual-header {
  margin-bottom: 18px;
  text-align: left;
}

.gac-manual-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 0 0 6px;
}

.gac-manual-sub {
  font-size: 14px;
  color: #444746;
  margin: 0;
}

.gac-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #e0e2ec;
  padding-top: 14px;
  margin-top: 8px;
  font-size: 12px;
  color: #444746;
}

.gac-lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
}

.gac-lang-btn:hover {
  background: #f0f4f9;
}

.gac-caret {
  font-size: 10px;
}

.gac-foot-links {
  display: flex;
  align-items: center;
  gap: 18px;
}

.gac-foot-item {
  color: #444746;
  text-decoration: none;
  font-size: 12px;
}

.gac-foot-item:hover {
  color: #1f1f1f;
  text-decoration: underline;
}

@media (max-width: 640px) {
  .google-modal-card {
    padding: 20px 18px !important;
  }
  .gac-main-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .gac-heading {
    font-size: 26px;
  }
}


.gac-unified-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.gac-heading-group {
  text-align: left;
  margin-bottom: 6px;
}

.gac-title-text {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 6px;
}

.gac-sub-text {
  font-size: 13.5px;
  color: #64748b;
  margin: 0;
}

.text-cool-brand {
  color: #d2a764;
}

.btn-google-trigger-large {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 18px;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  text-align: left;
}

.btn-google-trigger-large:hover {
  border-color: #4285f4;
  background: #f8fafc;
  box-shadow: 0 4px 14px rgba(66, 133, 244, 0.15);
  transform: translateY(-1px);
}

.google-logo-box {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.trigger-label-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.trigger-primary {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.trigger-secondary {
  font-size: 12px;
  color: #64748b;
}

.trigger-arrow {
  font-size: 18px;
  color: #4285f4;
  font-weight: 800;
}

.google-official-slot-wrapper {
  display: flex;
  justify-content: center;
  margin: 4px 0;
  min-height: 40px;
}

.google-official-btn-slot {
  display: inline-block;
}

.gac-account-row {
  display: flex;
  align-items: center;
  position: relative;
}

.btn-remove-device-acc {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #94a3b8;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 50%;
  margin-right: 6px;
  transition: all 0.15s;
}

.btn-remove-device-acc:hover {
  background: #f1f5f9;
  color: #ef4444;
}

.gac-avatar-dyn-0 {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.gac-avatar-dyn-1 {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.gac-avatar-dyn-2 {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

</style>
