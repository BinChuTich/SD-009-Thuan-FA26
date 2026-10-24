<script setup>
import { ref } from 'vue'
import api from '@/api'
import { loginClient } from '@/utils/taiKhoanKhachHang.js'

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
const showPassword = ref(false)

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
  password: ''
})

const closeModal = () => {
  emit('update:modelValue', false)
  errorMessage.value = ''
}

// Xử lý đăng nhập
const handleLogin = async () => {
  errorMessage.value = ''
  const identifier = loginForm.value.phoneOrEmail.trim()
  if (!identifier) {
    errorMessage.value = 'Vui lòng nhập Số điện thoại hoặc Email!'
    return
  }
  if (!loginForm.value.password) {
    errorMessage.value = 'Vui lòng nhập mật khẩu!'
    return
  }

  loading.value = true
  try {
    // 1. Thử tra cứu khách hàng trong cơ sở dữ liệu theo từ khóa SĐT hoặc Email
    let customer = null
    try {
      const res = await api.get('/api/khach-hang', {
        params: { keyword: identifier, size: 5 }
      })
      const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
      if (list && list.length > 0) {
        // Tìm bản ghi khớp chính xác SĐT hoặc Email
        customer = list.find(c => 
          (c.soDienThoai && c.soDienThoai.trim() === identifier) ||
          (c.email && c.email.trim().toLowerCase() === identifier.toLowerCase())
        ) || list[0]
      }
    } catch (e) {
      console.warn('Tra cứu khách hàng từ backend không phản hồi, dùng thông tin nhập:', e)
    }

    const userData = {
      id: customer?.id || Date.now(),
      name: customer?.hoVaTen || customer?.tenKhachHang || identifier.split('@')[0],
      phone: customer?.soDienThoai || (identifier.match(/^[0-9]+$/) ? identifier : '0988776655'),
      email: customer?.email || (identifier.includes('@') ? identifier : `${identifier}@customer.com`),
      maKhachHang: customer?.maKhachHang || ''
    }

    loginClient(userData)
    emit('login-success', userData)
    closeModal()
  } catch (err) {
    errorMessage.value = 'Đăng nhập không thành công. Vui lòng kiểm tra lại thông tin!'
  } finally {
    loading.value = false
  }
}

// Xử lý đăng ký tài khoản
const handleRegister = async () => {
  errorMessage.value = ''
  const name = registerForm.value.fullName.trim()
  const phone = registerForm.value.phone.trim()
  const email = registerForm.value.email.trim()
  const pass = registerForm.value.password

  if (!name) {
    errorMessage.value = 'Vui lòng nhập họ và tên của bạn!'
    return
  }
  if (!phone || !/^[0-9]{9,11}$/.test(phone)) {
    errorMessage.value = 'Số điện thoại không hợp lệ (phải từ 9-11 chữ số)!'
    return
  }
  if (!pass || pass.length < 6) {
    errorMessage.value = 'Mật khẩu phải chứa ít nhất 6 ký tự!'
    return
  }

  loading.value = true
  try {
    // Thử tạo khách hàng vào hệ thống backend nếu khả dụng
    let customerId = null
    try {
      const res = await api.post('/api/khach-hang', {
        hoVaTen: name,
        soDienThoai: phone,
        email: email || `${phone}@customer.com`,
        trangThai: 1
      })
      if (res.data?.id) {
        customerId = res.data.id
      }
    } catch (apiErr) {
      console.warn('Backend tạo khách hàng bỏ qua hoặc có cảnh báo:', apiErr)
    }

    const userData = {
      id: customerId || Date.now(),
      name: name,
      phone: phone,
      email: email || `${phone}@customer.com`
    }

    loginClient(userData)
    emit('login-success', userData)
    closeModal()
  } catch (err) {
    errorMessage.value = 'Không thể tạo tài khoản. Vui lòng thử lại sau!'
  } finally {
    loading.value = false
  }
}

// Điền nhanh tài khoản demo
const fillDemoAccount = () => {
  loginForm.value.phoneOrEmail = '0988776655'
  loginForm.value.password = '123456'
  handleLogin()
}
</script>

<template>
  <div v-if="modelValue" class="auth-modal-backdrop" @click.self="closeModal">
    <div class="auth-modal-card">
      <!-- Nút đóng góc phải -->
      <button class="btn-close-modal" @click="closeModal" title="Đóng">✕</button>

      <!-- Logo & Tiêu đề -->
      <div class="auth-modal-header">
        <div class="modal-brand-badge">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <h3 class="auth-modal-title">Tài Khoản Khách Hàng</h3>
        <p class="auth-modal-sub">Trải nghiệm mua sắm và theo dõi đơn hàng tại FF T-shirt</p>
      </div>

      <!-- Tab chuyển đổi Đăng nhập / Đăng ký -->
      <div class="auth-tabs">
        <button
          type="button"
          class="auth-tab-btn"
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'; errorMessage = ''"
        >
          Đăng nhập
        </button>
        <button
          type="button"
          class="auth-tab-btn"
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'; errorMessage = ''"
        >
          Đăng ký mới
        </button>
      </div>

      <!-- Thông báo lỗi nếu có -->
      <div v-if="errorMessage" class="auth-alert-box">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- 1. FORM ĐĂNG NHẬP -->
      <form v-if="activeTab === 'login'" @submit.prevent="handleLogin" class="auth-form-body">
        <div class="auth-field">
          <label class="field-label">Số điện thoại hoặc Email <span class="req">*</span></label>
          <div class="input-with-icon">
            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <input
              type="text"
              v-model="loginForm.phoneOrEmail"
              placeholder="VD: 0988776655 hoặc email..."
              class="auth-input"
              autofocus
            />
          </div>
        </div>

        <div class="auth-field">
          <div class="label-row">
            <label class="field-label">Mật khẩu <span class="req">*</span></label>
            <a href="javascript:void(0)" class="forgot-link" @click="fillDemoAccount">Dùng tài khoản mẫu</a>
          </div>
          <div class="input-with-icon">
            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <input
              :type="showPassword ? 'text' : 'password'"
              v-model="loginForm.password"
              placeholder="Nhập mật khẩu..."
              class="auth-input"
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
          <span v-else>ĐĂNG NHẬP NGAY</span>
        </button>

        <div class="quick-demo-box">
          <span class="demo-hint">Đăng nhập nhanh không cần mật khẩu:</span>
          <button type="button" class="btn-quick-chip" @click="fillDemoAccount">
            ⚡ Khách hàng: Dương Ngọc Chiến (0988776655)
          </button>
        </div>
      </form>

      <!-- 2. FORM ĐĂNG KÝ -->
      <form v-else @submit.prevent="handleRegister" class="auth-form-body">
        <div class="auth-field">
          <label class="field-label">Họ và tên của bạn <span class="req">*</span></label>
          <div class="input-with-icon">
            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <input
              type="text"
              v-model="registerForm.fullName"
              placeholder="VD: Nguyễn Văn A"
              class="auth-input"
              autofocus
            />
          </div>
        </div>

        <div class="auth-field">
          <label class="field-label">Số điện thoại <span class="req">*</span></label>
          <div class="input-with-icon">
            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
            </svg>
            <input
              type="tel"
              v-model="registerForm.phone"
              placeholder="VD: 0988123456"
              class="auth-input"
            />
          </div>
        </div>

        <div class="auth-field">
          <label class="field-label">Email (Tùy chọn)</label>
          <div class="input-with-icon">
            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
            <input
              type="email"
              v-model="registerForm.email"
              placeholder="VD: email@example.com"
              class="auth-input"
            />
          </div>
        </div>

        <div class="auth-field">
          <label class="field-label">Mật khẩu <span class="req">*</span></label>
          <div class="input-with-icon">
            <svg class="field-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <input
              :type="showPassword ? 'text' : 'password'"
              v-model="registerForm.password"
              placeholder="Tối thiểu 6 ký tự..."
              class="auth-input"
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
          <span v-if="loading">Đang xử lý...</span>
          <span v-else>ĐĂNG KÝ TÀI KHOẢN</span>
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.auth-modal-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 18px;
  padding: 28px 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  border: 1px solid #e2e8f0;
  animation: scaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.94) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.btn-close-modal {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
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
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #fdfbf7;
  border: 2px solid #ecd8af;
  color: #496883;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.auth-modal-title {
  font-size: 18px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 4px;
}

.auth-modal-sub {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.auth-tabs {
  display: flex;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 4px;
  margin-bottom: 18px;
}

.auth-tab-btn {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: transparent;
  color: #64748b;
  font-weight: 700;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.auth-tab-btn.active {
  background: #ffffff;
  color: #496883;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.auth-alert-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 14px;
}

.auth-form-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-link {
  font-size: 11px;
  color: #d2a764;
  font-weight: 600;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.req {
  color: #ef4444;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.field-icon {
  position: absolute;
  left: 12px;
  color: #94a3b8;
  pointer-events: none;
}

.auth-input {
  width: 100%;
  height: 42px;
  padding: 0 40px 0 38px;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.auth-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.toggle-eye-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
}

.btn-auth-submit {
  margin-top: 4px;
  width: 100%;
  height: 44px;
  background: #d2a764;
  color: #1e293b;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(210, 167, 100, 0.35);
}

.btn-auth-submit:hover:not(:disabled) {
  background: #e0b875;
  transform: translateY(-1px);
}

.btn-auth-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.quick-demo-box {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.demo-hint {
  font-size: 11px;
  color: #64748b;
}

.btn-quick-chip {
  padding: 7px 10px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #496883;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.btn-quick-chip:hover {
  background: #fdfbf7;
  border-color: #d2a764;
  color: #b45309;
}
</style>
