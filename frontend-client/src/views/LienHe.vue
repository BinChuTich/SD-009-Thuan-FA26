<script setup>
import { ref } from 'vue'
import api from '@/api'

const contactForm = ref({
  name: '',
  phone: '',
  email: '',
  message: ''
})

const submitting = ref(false)

// Validation errors
const nameError = ref('')
const phoneError = ref('')
const messageError = ref('')

// Modal notification states
const showSuccessModal = ref(false)
const showErrorModal = ref(false)
const errorDialogMessage = ref('')
const submittedInfo = ref({
  name: '',
  phone: '',
  email: '',
  time: ''
})

// Kiểm tra định dạng số điện thoại Việt Nam (10 số, đầu 03, 05, 07, 08, 09)
const validatePhone = () => {
  const phone = contactForm.value.phone ? contactForm.value.phone.trim() : ''
  if (!phone) {
    phoneError.value = 'Vui lòng nhập số điện thoại!'
    return false
  }
  const phoneRegex = /^(03|05|07|08|09)\d{8}$/
  if (!phoneRegex.test(phone)) {
    phoneError.value = 'Số điện thoại không hợp lệ (cần 10 chữ số bắt đầu bằng 03, 05, 07, 08 hoặc 09)!'
    return false
  }
  phoneError.value = ''
  return true
}

// Xử lý tự động loại bỏ ký tự không phải số và giới hạn 10 số
const onPhoneInput = (e) => {
  const clean = e.target.value.replace(/\D/g, '').slice(0, 10)
  contactForm.value.phone = clean
  if (phoneError.value) {
    if (clean.length === 10) {
      validatePhone()
    } else {
      phoneError.value = ''
    }
  }
}

const validateAll = () => {
  let valid = true
  if (!contactForm.value.name.trim()) {
    nameError.value = 'Vui lòng nhập họ và tên của bạn!'
    valid = false
  } else {
    nameError.value = ''
  }

  if (!validatePhone()) {
    valid = false
  }

  if (!contactForm.value.message.trim()) {
    messageError.value = 'Vui lòng nhập nội dung cần hỗ trợ!'
    valid = false
  } else {
    messageError.value = ''
  }

  return valid
}

const submitContact = async () => {
  if (!validateAll()) {
    return
  }

  submitting.value = true
  try {
    // 1. Gửi trực tiếp qua backend SMTP về fftshirt.vn@gmail.com
    await api.post('/api/lien-he/gui-tin-nhan', {
      name: contactForm.value.name.trim(),
      phone: contactForm.value.phone.trim(),
      email: contactForm.value.email.trim(),
      message: contactForm.value.message.trim()
    })

    // Lưu thông tin hiển thị trên bảng thông báo thành công
    const now = new Date()
    const formattedTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')} ngày ${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`

    submittedInfo.value = {
      name: contactForm.value.name.trim(),
      phone: contactForm.value.phone.trim(),
      email: contactForm.value.email.trim(),
      time: formattedTime
    }

    // Hiển thị modal thông báo thành công thay cho alert trình duyệt
    showSuccessModal.value = true

    // Reset form
    contactForm.value = { name: '', phone: '', email: '', message: '' }
    nameError.value = ''
    phoneError.value = ''
    messageError.value = ''
  } catch (err) {
    console.warn('API gửi mail liên hệ gặp lỗi, mở bảng thông báo dự phòng:', err)
    const backendMsg = err.response?.data?.message
    errorDialogMessage.value = backendMsg || 'Hệ thống gửi thư tự động tạm thời gián đoạn kết nối. Bạn có thể mở ứng dụng Email để gửi yêu cầu trực tiếp tới fftshirt.vn@gmail.com.'
    showErrorModal.value = true
  } finally {
    submitting.value = false
  }
}

// Mở ứng dụng mail nếu cần dự phòng
const openFallbackMailClient = () => {
  const subject = encodeURIComponent(`[FF T-Shirt Hỗ Trợ] Yêu cầu từ ${contactForm.value.name || submittedInfo.value.name} (${contactForm.value.phone || submittedInfo.value.phone})`)
  const body = encodeURIComponent(
    `Họ và tên: ${contactForm.value.name || submittedInfo.value.name}\n` +
    `Số điện thoại: ${contactForm.value.phone || submittedInfo.value.phone}\n` +
    `Email khách hàng: ${contactForm.value.email || submittedInfo.value.email || 'Không cung cấp'}\n` +
    `Nội dung hỗ trợ:\n${contactForm.value.message || 'Cần hỗ trợ về đơn hàng và sản phẩm'}`
  )
  window.open(`mailto:fftshirt.vn@gmail.com?subject=${subject}&body=${body}`, '_blank')
  showErrorModal.value = false
}
</script>

<template>
  <div class="contact-page">
    <!-- 1. HERO BANNER VỚI LOGO THƯƠNG HIỆU & MÀU CHUẨN ADMIN -->
    <div class="contact-hero">
      <div class="container-client">
        <!-- LOGO THƯƠNG HIỆU CHUẨN: 3 TIA SÁNG + FF + T-shirt -->
        <div class="hero-logo-box">
          <div class="brand-logo-wrapper">
            <div class="sparkles">
              <span class="ray ray-1"></span>
              <span class="ray ray-2"></span>
              <span class="ray ray-3"></span>
            </div>
            <div class="logo-content">
              <div class="logo-name">
                <span class="f-white">F</span>
                <span class="f-gold">F</span>
              </div>
              <div class="logo-sub">T-shirt</div>
            </div>
          </div>
        </div>

        <h1 class="page-title">Liên Hệ Với FF T-SHIRT</h1>
        <p class="page-sub">Chúng tôi luôn lắng nghe và sẵn sàng giải đáp mọi thắc mắc của quý khách hàng</p>
      </div>
    </div>

    <!-- 2. NỘI DUNG GỌN GÀNG: THÔNG TIN HỖ TRỢ & FORM GỬI TIN NHẮN -->
    <div class="container-client content-grid">
      <!-- Cột trái: Thông tin liên hệ cơ bản -->
      <div class="contact-info-col">
        <h3>Thông Tin Hỗ Trợ Khách Hàng</h3>
        <p class="lead">Đừng ngần ngại liên hệ với chúng tôi bất cứ khi nào bạn cần tư vấn về sản phẩm, chọn size áo hoặc theo dõi đơn hàng.</p>

        <div class="info-list">
          <div class="info-item">
            <span class="icon">📍</span>
            <div>
              <strong>Địa chỉ cửa hàng:</strong>
              <p>Tòa nhà FPT Polytechnic, Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</p>
            </div>
          </div>

          <div class="info-item">
            <span class="icon">📞</span>
            <div>
              <strong>Hotline chăm sóc khách hàng:</strong>
              <p>1900 6868 - 0388.888.888 (8:00 - 22:00 hàng ngày)</p>
            </div>
          </div>

          <div class="info-item">
            <span class="icon">✉️</span>
            <div>
              <strong>Email hỗ trợ:</strong>
              <p><a href="mailto:fftshirt.vn@gmail.com" class="support-email-link">fftshirt.vn@gmail.com</a></p>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột phải: Form gửi tin nhắn gọn gàng -->
      <div class="contact-form-col">
        <div class="form-card">
          <h3>Gửi Tin Nhắn Cho Chúng Tôi</h3>
          <form @submit.prevent="submitContact" novalidate>
            <!-- Họ và tên -->
            <div class="form-group">
              <label>Họ và tên của bạn <span class="required-star">*</span></label>
              <input
                v-model="contactForm.name"
                @input="nameError = ''"
                type="text"
                placeholder="Nguyễn Văn A"
                class="form-control"
                :class="{ 'is-invalid': nameError }"
              />
              <p v-if="nameError" class="field-error">
                <span class="err-icon">⚠️</span> {{ nameError }}
              </p>
            </div>

            <!-- Số điện thoại (Strict validation 10 số VN) -->
            <div class="form-group">
              <label>Số điện thoại <span class="required-star">*</span></label>
              <input
                :value="contactForm.phone"
                @input="onPhoneInput"
                @blur="validatePhone"
                type="tel"
                maxlength="10"
                placeholder="Ví dụ: 0912 345 678"
                class="form-control"
                :class="{ 'is-invalid': phoneError }"
              />
              <p v-if="phoneError" class="field-error">
                <span class="err-icon">⚠️</span> {{ phoneError }}
              </p>
            </div>

            <!-- Email (tuỳ chọn) -->
            <div class="form-group">
              <label>Email <span class="optional-hint">(Không bắt buộc)</span></label>
              <input
                v-model="contactForm.email"
                type="email"
                placeholder="email@example.com"
                class="form-control"
              />
            </div>

            <!-- Nội dung hỗ trợ -->
            <div class="form-group">
              <label>Nội dung cần hỗ trợ <span class="required-star">*</span></label>
              <textarea
                v-model="contactForm.message"
                @input="messageError = ''"
                rows="4"
                placeholder="Nhập câu hỏi hoặc yêu cầu của bạn..."
                class="form-control"
                :class="{ 'is-invalid': messageError }"
              ></textarea>
              <p v-if="messageError" class="field-error">
                <span class="err-icon">⚠️</span> {{ messageError }}
              </p>
            </div>

            <button type="submit" class="btn-submit" :disabled="submitting">
              <span v-if="submitting" class="spinner"></span>
              {{ submitting ? 'Đang gửi thông tin...' : 'Gửi Thông Tin →' }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- BẢNG THÔNG BÁO GỬI THÀNH CÔNG (CUSTOM MODAL ĐẸP THAY THẾ ALERT TRÌNH DUYỆT) -->
    <Teleport to="body">
      <div
        v-if="showSuccessModal"
        class="contact-modal-backdrop"
        @click.self="showSuccessModal = false"
      >
        <div class="contact-modal-dialog">
          <!-- Nút đóng X góc phải -->
          <button class="modal-close-btn" @click="showSuccessModal = false" title="Đóng">✕</button>

          <!-- Icon Thành Công Đẹp Mắt -->
          <div class="modal-success-badge">
            <svg class="check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>

          <h3 class="modal-dialog-title">Gửi Tin Nhắn Thành Công!</h3>
          <p class="modal-dialog-subtitle">
            Cảm ơn quý khách <strong>{{ submittedInfo.name }}</strong> đã tin tưởng liên hệ với FF T-Shirt.
          </p>

          <!-- Bảng tóm tắt thông tin gửi -->
          <div class="modal-info-summary">
            <div class="summary-line">
              <span class="label">Người liên hệ:</span>
              <span class="val font-bold">{{ submittedInfo.name }}</span>
            </div>
            <div class="summary-line">
              <span class="label">Số điện thoại:</span>
              <span class="val font-bold text-phone">{{ submittedInfo.phone }}</span>
            </div>
            <div v-if="submittedInfo.email" class="summary-line">
              <span class="label">Email khách:</span>
              <span class="val">{{ submittedInfo.email }}</span>
            </div>
            <div class="summary-line highlight-email-line">
              <span class="label">Email tiếp nhận:</span>
              <span class="val text-gold font-bold">fftshirt.vn@gmail.com</span>
            </div>
            <div class="summary-line">
              <span class="label">Thời gian gửi:</span>
              <span class="val">{{ submittedInfo.time }}</span>
            </div>
          </div>

          <!-- Thông báo CSKH -->
          <div class="modal-notice-banner">
            <span class="notice-icon">💬</span>
            <p>Hệ thống đã chuyển tiếp thông tin tới hòm thư CSKH. Đội ngũ tư vấn sẽ liên hệ lại qua số điện thoại <strong>{{ submittedInfo.phone }}</strong> trong thời gian sớm nhất.</p>
          </div>

          <button class="modal-confirm-btn" @click="showSuccessModal = false">
            Đã Hiểu & Đóng Thông Báo
          </button>
        </div>
      </div>
    </Teleport>

    <!-- BẢNG THÔNG BÁO LỖI / DỰ PHÒNG MAILTO (KHÔNG DÙNG ALERT) -->
    <Teleport to="body">
      <div
        v-if="showErrorModal"
        class="contact-modal-backdrop"
        @click.self="showErrorModal = false"
      >
        <div class="contact-modal-dialog">
          <button class="modal-close-btn" @click="showErrorModal = false" title="Đóng">✕</button>

          <div class="modal-error-badge">
            <span class="err-cross">✕</span>
          </div>

          <h3 class="modal-dialog-title text-danger">Thông Báo Gửi Tin Nhắn</h3>
          <p class="modal-dialog-subtitle">{{ errorDialogMessage }}</p>

          <div class="modal-notice-banner">
            <span class="notice-icon">✉️</span>
            <p>Bạn cũng có thể mở trực tiếp ứng dụng Email của thiết bị để gửi nội dung hỗ trợ tới hòm thư <strong>fftshirt.vn@gmail.com</strong>.</p>
          </div>

          <div class="modal-btn-group">
            <button class="modal-btn-mailto" @click="openFallbackMailClient">
              Mở Ứng Dụng Email
            </button>
            <button class="modal-confirm-btn btn-secondary-close" @click="showErrorModal = false">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.contact-page {
  background-color: #f8fafc;
  min-height: 80vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

/* 1. HERO BANNER */
.contact-hero {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #2b394a 100%);
  color: #ffffff;
  padding: 50px 0 45px;
  text-align: center;
  border-bottom: 2px solid #ecd8af;
}

/* LOGO THƯƠNG HIỆU: CHUẨN FREDOKA + 3 TIA SÁNG + FF */
.hero-logo-box {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}

.brand-logo-wrapper {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  font-family: 'Fredoka', sans-serif;
  user-select: none;
}

.sparkles {
  position: absolute;
  top: -10px;
  left: -10px;
  width: 28px;
  height: 28px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d2a764;
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(210, 167, 100, 0.7);
}

.sparkles .ray-1 {
  width: 3.5px;
  height: 11px;
  top: 0;
  right: 5px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 3.5px;
  height: 11px;
  top: 4px;
  left: 4px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 3.5px;
  height: 11px;
  bottom: 0px;
  left: -2px;
  transform: rotate(-80deg);
}

.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 3px 10px rgba(0, 0, 0, 0.35));
}

.logo-name {
  display: flex;
  font-size: 50px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -2px;
}

.f-white {
  color: #ffffff;
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.logo-sub {
  color: #d2a764;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  margin-left: 7px;
  margin-bottom: 3px;
  letter-spacing: -0.4px;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  margin: 0 0 10px;
  color: #ffffff;
}

.page-sub {
  color: #cbd5e1;
  font-size: 15px;
  max-width: 650px;
  margin: 0 auto;
  line-height: 1.5;
}

.container-client {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

/* 2. CONTENT GRID */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  margin-top: 50px;
  align-items: start;
}

.contact-info-col h3 {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 12px;
}

.lead {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 28px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.04);
}

.info-item .icon {
  font-size: 22px;
  background: #eaf1f4;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-item strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
  font-weight: 700;
}

.info-item p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.support-email-link {
  color: #496883;
  text-decoration: none;
  font-weight: 700;
  transition: color 0.2s;
}

.support-email-link:hover {
  color: #d2a764;
  text-decoration: underline;
}

/* FORM CARD */
.form-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 6px 20px rgba(73, 104, 131, 0.06);
}

.form-card h3 {
  margin: 0 0 20px;
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 6px;
}

.required-star {
  color: #ef4444;
  font-weight: 800;
  margin-left: 2px;
}

.optional-hint {
  font-size: 12px;
  font-weight: normal;
  color: #94a3b8;
  margin-left: 4px;
}

.form-control {
  width: 100%;
  padding: 11px 14px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13.5px;
  font-family: inherit;
  box-sizing: border-box;
  color: #1e293b;
  background: #f8fafc;
  transition: all 0.2s ease;
}

.form-control:focus {
  outline: none;
  border-color: #496883;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.form-control.is-invalid {
  border-color: #ef4444;
  background-color: #fef2f2;
}

.form-control.is-invalid:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
}

.field-error {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: #dc2626;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 5px;
}

.err-icon {
  font-size: 13px;
}

.btn-submit {
  width: 100%;
  padding: 13px 0;
  background: #d2a764;
  color: #1e293b;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-submit:hover:not(:disabled) {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.45);
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(30, 41, 59, 0.3);
  border-top-color: #1e293b;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ==========================================================================
   BẢNG THÔNG BÁO MODAL TÙY BIẾN (CUSTOM MODAL NOTIFICATION)
   ========================================================================== */
.contact-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: backdropFade 0.22s ease-out;
}

@keyframes backdropFade {
  from { opacity: 0; }
  to { opacity: 1; }
}

.contact-modal-dialog {
  background: #ffffff;
  border-radius: 20px;
  max-width: 480px;
  width: 100%;
  padding: 34px 28px 28px;
  box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.35), 0 0 0 1px rgba(0, 0, 0, 0.05);
  position: relative;
  text-align: center;
  animation: popIn 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-close-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.modal-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* Icon Huy Hiệu Thành Công */
.modal-success-badge {
  width: 68px;
  height: 68px;
  background: #ecfdf5;
  border: 3px solid #10b981;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
  color: #10b981;
  box-shadow: 0 0 0 8px rgba(16, 185, 129, 0.12);
}

.check-icon {
  width: 34px;
  height: 34px;
}

.modal-error-badge {
  width: 68px;
  height: 68px;
  background: #fef2f2;
  border: 3px solid #ef4444;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
  color: #ef4444;
  font-size: 32px;
  font-weight: 800;
  box-shadow: 0 0 0 8px rgba(239, 68, 68, 0.12);
}

.modal-dialog-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 8px;
}

.modal-dialog-title.text-danger {
  color: #dc2626;
}

.modal-dialog-subtitle {
  font-size: 14px;
  color: #475569;
  line-height: 1.55;
  margin: 0 0 20px;
}

/* Tóm tắt thông tin gửi trong modal */
.modal-info-summary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 18px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13.5px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed #e2e8f0;
  padding-bottom: 6px;
}

.summary-line:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.summary-line .label {
  color: #64748b;
  font-size: 13px;
}

.summary-line .val {
  color: #1e293b;
  font-weight: 500;
}

.font-bold {
  font-weight: 700 !important;
}

.text-phone {
  color: #0284c7 !important;
  font-family: monospace;
  font-size: 14px;
}

.text-gold {
  color: #b58c4c !important;
}

.highlight-email-line {
  background: #fffbeb;
  margin: 0 -8px;
  padding: 6px 8px;
  border-radius: 6px;
  border-bottom: none;
}

/* Khối thông báo CSKH */
.modal-notice-banner {
  background: #f1f5f9;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
  text-align: left;
  margin-bottom: 22px;
}

.notice-icon {
  font-size: 18px;
  line-height: 1.2;
}

.modal-notice-banner p {
  margin: 0;
  font-size: 12.5px;
  color: #475569;
  line-height: 1.5;
}

.modal-confirm-btn {
  width: 100%;
  padding: 12px 0;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.modal-confirm-btn:hover {
  background: #395267;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.35);
}

.modal-btn-group {
  display: flex;
  gap: 12px;
}

.modal-btn-mailto {
  flex: 1;
  padding: 12px 0;
  background: #d2a764;
  color: #1e293b;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-btn-mailto:hover {
  background: #e0b875;
}

.btn-secondary-close {
  flex: 1;
  background: #64748b;
}

.btn-secondary-close:hover {
  background: #475569;
}

@media (max-width: 860px) {
  .content-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}
</style>
