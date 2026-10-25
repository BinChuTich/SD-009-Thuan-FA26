<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/api.js'
import { setCurrentUser, getCurrentUser } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const usernameError = ref('')
const passwordError = ref('')

onMounted(() => {
  // Nếu đã đăng nhập thì tự chuyển vào trang tương ứng
  const user = getCurrentUser()
  if (user) {
    if (user.maVaiTro === 'ADMIN' || user.role === 'Quản trị viên') {
      router.replace('/thong-ke')
    } else {
      router.replace('/ban-hang')
    }
  }
})

const handleLogin = async () => {
  errorMessage.value = ''
  usernameError.value = ''
  passwordError.value = ''

  let hasError = false
  if (!username.value.trim()) {
    usernameError.value = 'Vui lòng nhập tên tài khoản hoặc email!'
    hasError = true
  }
  if (!password.value) {
    passwordError.value = 'Vui lòng nhập mật khẩu!'
    hasError = true
  }
  if (hasError) return

  loading.value = true
  try {
    const res = await api.post('/api/nhan-vien/login', {
      tenTaiKhoan: username.value.trim(),
      matKhau: password.value
    })

    if (res.data) {
      const loggedUser = setCurrentUser(res.data)
      try { sessionStorage.removeItem('dismissed_mo_ca_' + loggedUser.id); } catch(e) {}
      const redirectUrl = route.query.redirect

      if (redirectUrl) {
        router.push(redirectUrl)
      } else if (loggedUser.maVaiTro === 'ADMIN' || loggedUser.role === 'Quản trị viên') {
        router.push('/thong-ke')
      } else {
        router.push('/ban-hang')
      }
    }
  } catch (err) {
    console.error('Đăng nhập thất bại:', err)
    if (err.response && err.response.data) {
      errorMessage.value = err.response.data.message || err.response.data.error || 'Tài khoản hoặc mật khẩu không chính xác!'
    } else {
      errorMessage.value = 'Không thể kết nối đến máy chủ hoặc tài khoản không đúng!'
    }
  } finally {
    loading.value = false
  }
}

const handleQuickLoginAdmin = () => {
  username.value = 'admin'
  password.value = '123456'
  handleLogin()
}

// Logic Quên mật khẩu
const showForgotModal = ref(false)
const forgotEmail = ref('')
const forgotEmailError = ref('')
const forgotLoading = ref(false)
const forgotMessage = ref('')
const forgotError = ref('')

const openForgotPasswordModal = () => {
  forgotEmail.value = ''
  forgotEmailError.value = ''
  forgotMessage.value = ''
  forgotError.value = ''
  showForgotModal.value = true
}

const closeForgotPasswordModal = () => {
  if (forgotLoading.value) return
  showForgotModal.value = false
}

const handleForgotPassword = async () => {
  forgotEmailError.value = ''
  forgotError.value = ''
  forgotMessage.value = ''

  if (!forgotEmail.value.trim()) {
    forgotEmailError.value = 'Vui lòng nhập địa chỉ email của bạn!'
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(forgotEmail.value.trim())) {
    forgotEmailError.value = 'Địa chỉ email không đúng định dạng (VD: example@domain.com)!'
    return
  }

  forgotLoading.value = true
  try {
    const res = await api.post('/api/nhan-vien/forgot-password', {
      email: forgotEmail.value.trim()
    })
    forgotMessage.value = res.data?.message || 'Mật khẩu mới đã được gửi về email của bạn. Vui lòng kiểm tra hộp thư!'
    forgotEmail.value = ''
  } catch (err) {
    console.error('Quên mật khẩu thất bại:', err)
    if (err.response && err.response.data) {
      const msg = err.response.data.message || err.response.data.error || 'Không tìm thấy tài khoản nhân viên khớp với email này!'
      forgotEmailError.value = msg
    } else {
      forgotError.value = 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau ít phút!'
    }
  } finally {
    forgotLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card-box">
      <!-- Header logo chuẩn nhận diện FF-Tshirt -->
      <div class="login-header">
        <div class="logo-wrapper">
          <div class="sparkles">
            <span class="ray ray-1"></span>
            <span class="ray ray-2"></span>
            <span class="ray ray-3"></span>
          </div>
          <div class="logo-content">
            <div class="logo-name">
              <span class="f-blue">F</span>
              <span class="f-gold">F</span>
            </div>
            <div class="logo-sub">T-shirt</div>
          </div>
        </div>
        <h2 class="login-title">HỆ THỐNG QUẢN LÝ BÁN HÀNG</h2>
      </div>

      <!-- Thông báo lỗi nếu có -->
      <div v-if="errorMessage" class="login-error-alert">
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Form đăng nhập -->
      <form class="login-form" @submit.prevent="handleLogin" novalidate>
        <div class="form-group" :class="{ 'has-error': usernameError }">
          <label class="form-label">Tài khoản / Email</label>
          <div class="input-wrapper">
            <input
              v-model="username"
              type="text"
              class="form-input"
              :class="{ 'input-error': usernameError }"
              placeholder="Nhập tên tài khoản hoặc email..."
              autocomplete="username"
              @input="usernameError = ''"
              :disabled="loading"
              autofocus
            />
          </div>
          <div v-if="usernameError" class="field-error-text">
            <span>{{ usernameError }}</span>
          </div>
        </div>

        <div class="form-group" :class="{ 'has-error': passwordError }">
          <div class="form-label-row">
            <label class="form-label">Mật khẩu</label>
            <a href="javascript:void(0)" class="link-forgot-pass" @click="openForgotPasswordModal">
              Quên mật khẩu?
            </a>
          </div>
          <div class="input-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input has-toggle"
              :class="{ 'input-error': passwordError }"
              placeholder="Nhập mật khẩu..."
              autocomplete="current-password"
              @input="passwordError = ''"
              :disabled="loading"
            />
            <button
              type="button"
              class="btn-toggle-eye"
              @click="showPassword = !showPassword"
              tabindex="-1"
              title="Ẩn/hiện mật khẩu"
            >
              {{ showPassword ? 'Ẩn' : 'Hiện' }}
            </button>
          </div>
          <div v-if="passwordError" class="field-error-text">
            <span>{{ passwordError }}</span>
          </div>
        </div>

        <button type="submit" class="btn-submit-login" :disabled="loading">
          <span v-if="loading" class="spinner-inline"></span>
          <span>{{ loading ? 'Đang xác thực...' : 'Đăng nhập vào hệ thống' }}</span>
        </button>
      </form>

      <div class="login-footer">
        © 2026 FF-Tshirt Fashion Store. Hệ thống quản lý bán hàng.
      </div>
    </div>

    <!-- Modal Cấp lại mật khẩu -->
    <transition name="modal-fade">
      <div v-if="showForgotModal" class="forgot-modal-backdrop" @click.self="closeForgotPasswordModal">
        <div class="forgot-modal-card">
          <button class="btn-close-modal" @click="closeForgotPasswordModal" :disabled="forgotLoading" title="Đóng">&times;</button>

          <div class="forgot-modal-header">
            <h3 class="forgot-modal-title">CẤP LẠI MẬT KHẨU</h3>
            <p class="forgot-modal-desc">
              Nhập địa chỉ email nhân viên hoặc quản trị viên của bạn. Hệ thống sẽ tạo mật khẩu mới và gửi trực tiếp về email này.
            </p>
          </div>

          <!-- Alert thông báo lỗi -->
          <div v-if="forgotError" class="modal-alert alert-danger">
            <span>{{ forgotError }}</span>
          </div>

          <!-- Alert thông báo thành công -->
          <div v-if="forgotMessage" class="modal-alert alert-success">
            <span>{{ forgotMessage }}</span>
          </div>

          <!-- Form nhập email -->
          <form @submit.prevent="handleForgotPassword" class="forgot-modal-form" novalidate>
            <div class="form-group" :class="{ 'has-error': forgotEmailError }">
              <label class="form-label">Email tài khoản</label>
              <div class="input-wrapper">
                <input
                  v-model="forgotEmail"
                  type="text"
                  class="form-input"
                  :class="{ 'input-error': forgotEmailError }"
                  placeholder="Nhập email nhân viên đã đăng ký..."
                  @input="forgotEmailError = ''"
                  autocomplete="email"
                  :disabled="forgotLoading"
                  autofocus
                />
              </div>
              <div v-if="forgotEmailError" class="field-error-text">
                <span>{{ forgotEmailError }}</span>
              </div>
            </div>

            <div class="forgot-actions">
              <button
                type="submit"
                class="btn-submit-forgot"
                :disabled="forgotLoading"
              >
                <span v-if="forgotLoading" class="spinner-inline"></span>
                <span>{{ forgotLoading ? 'Đang gửi email cấp lại...' : 'Gửi lại tài khoản & mật khẩu' }}</span>
              </button>

              <button
                type="button"
                class="btn-cancel-forgot"
                @click="closeForgotPasswordModal"
                :disabled="forgotLoading"
              >
                Quay lại đăng nhập
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Fredoka:wght@600;700&display=swap');

.login-page {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f7f5ef;
  background-image: 
    radial-gradient(circle at 50% 25%, #ffffff 0%, #f7f5ef 65%, #ece7dc 100%);
  padding: 1.5rem;
  box-sizing: border-box;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.login-card-box {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 18px;
  border: 1.5px solid #ecd8af;
  box-shadow: 0 12px 36px rgba(73, 104, 131, 0.1), 0 2px 6px rgba(210, 167, 100, 0.15);
  padding: 2.25rem 2.25rem 1.85rem;
  box-sizing: border-box;
  animation: fadeInDown 0.28s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', sans-serif;
  margin-bottom: 0.5rem;
}

.sparkles {
  position: absolute;
  top: -8px;
  left: -10px;
  width: 28px;
  height: 28px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 3px;
}

.sparkles .ray-1 {
  width: 4px;
  height: 11px;
  top: 0;
  right: 5px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 4px;
  height: 11px;
  top: 5px;
  left: 4px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 4px;
  height: 11px;
  bottom: 0px;
  left: -3px;
  transform: rotate(-80deg);
}

.logo-content {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  filter: drop-shadow(0 3px 6px rgba(73, 104, 131, 0.16));
}

.logo-name {
  display: flex;
  font-size: 50px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -2px;
}

.f-blue {
  color: #496883;
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.logo-sub {
  color: #496883;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  margin-left: 5px;
  margin-bottom: 3px;
  letter-spacing: -0.3px;
}

.login-title {
  font-size: 1.18rem;
  font-weight: 800;
  color: #304b60;
  margin: 0.75rem 0 0;
  letter-spacing: 0.4px;
}

.login-error-alert {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 0.7rem 0.95rem;
  border-radius: 9px;
  font-size: 0.86rem;
  font-weight: 500;
  margin-bottom: 1.25rem;
  text-align: center;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.05rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-label {
  font-size: 0.84rem;
  font-weight: 600;
  color: #496883;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1.5px solid #e9e5db;
  border-radius: 9px;
  font-size: 0.9rem;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input.has-toggle {
  padding-right: 50px;
}

.form-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.form-input.input-error {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}

.form-input.input-error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
}

.field-error-text {
  font-size: 0.77rem;
  color: #dc2626;
  font-weight: 500;
  margin-top: 4px;
  line-height: 1.35;
  animation: errorSlideDown 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes errorSlideDown {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.btn-toggle-eye {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  color: #496883;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  display: flex;
  align-items: center;
  border-radius: 6px;
  user-select: none;
  transition: all 0.15s ease;
}

.btn-toggle-eye:hover {
  color: #2c3e50;
  background-color: #f1ede7;
}

.btn-submit-login {
  height: 44px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 9px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.22);
  margin-top: 0.4rem;
  font-family: inherit;
}

.btn-submit-login:hover:not(:disabled) {
  background: #365066;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.32);
}

.btn-submit-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner-inline {
  width: 17px;
  height: 17px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  text-align: center;
  font-size: 0.74rem;
  color: #9ca3af;
  margin-top: 1.75rem;
}

/* Quên mật khẩu styles */
.form-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.link-forgot-pass {
  font-size: 0.8rem;
  color: #496883;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s ease;
}

.link-forgot-pass:hover {
  color: #d2a764;
  text-decoration: underline;
}

/* Modal Quên mật khẩu */
.forgot-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  z-index: 9999;
}

.forgot-modal-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 18px;
  padding: 2rem 2.2rem;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.16);
  border: 1.5px solid #ece8df;
  animation: modalScaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalScaleIn {
  from { opacity: 0; transform: scale(0.95) translateY(-8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.btn-close-modal {
  position: absolute;
  top: 14px;
  right: 16px;
  background: none;
  border: none;
  font-size: 1.6rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  color: #1e293b;
  background: #f1f5f9;
}

.forgot-modal-header {
  text-align: center;
  margin-bottom: 1.25rem;
}

.forgot-modal-title {
  margin: 0 0 0.45rem;
  font-size: 1.15rem;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: 0.3px;
}

.forgot-modal-desc {
  margin: 0;
  font-size: 0.83rem;
  color: #64748b;
  line-height: 1.5;
}

.modal-alert {
  padding: 10px 14px;
  border-radius: 9px;
  font-size: 0.82rem;
  margin-bottom: 1.2rem;
  line-height: 1.45;
  text-align: center;
}

.alert-danger {
  background: #fef2f2;
  border: 1px solid #fee2e2;
  color: #dc2626;
}

.alert-success {
  background: #f0fdf4;
  border: 1px solid #dcfce7;
  color: #16a34a;
}

.forgot-modal-form {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.forgot-actions {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-top: 0.3rem;
}

.btn-submit-forgot {
  height: 44px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 9px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
  font-family: inherit;
}

.btn-submit-forgot:hover:not(:disabled) {
  background: #365066;
  transform: translateY(-1px);
}

.btn-submit-forgot:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-cancel-forgot {
  height: 38px;
  background: transparent;
  color: #64748b;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-cancel-forgot:hover:not(:disabled) {
  background: #f8fafc;
  color: #334155;
  border-color: #cbd5e1;
}

/* Modal Transition */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
