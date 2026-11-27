<script setup>
import { ref } from 'vue'
import api from '@/api'
import { storeRules } from '@/data/quyDinhCuaHang.js'

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
    <!-- TRUNG TÂM HỖ TRỢ & FORM LIÊN HỆ -->
    <div class="container-client content-grid support-center-grid">
      <aside class="contact-info-col support-panel">
        <span class="support-kicker">FF T-SHIRT CARE</span>
        <h2>Chúng tôi có thể giúp gì cho bạn?</h2>
        <p class="lead">Gửi yêu cầu cho đội ngũ hỗ trợ khi bạn cần tư vấn sản phẩm, chọn size, thanh toán hoặc kiểm tra đơn hàng.</p>

        <a href="mailto:fftshirt.vn@gmail.com" class="primary-contact-card">
          <span class="primary-contact-icon">✉</span>
          <span><small>Email tiếp nhận hỗ trợ</small><strong>fftshirt.vn@gmail.com</strong></span>
          <b>↗</b>
        </a>

        <div class="support-benefits">
          <div><span>✓</span><p><strong>Tiếp nhận rõ ràng</strong><small>Thông tin được gửi trực tiếp tới bộ phận CSKH.</small></p></div>
          <div><span>✓</span><p><strong>Hỗ trợ đúng đơn</strong><small>Cung cấp mã đơn để được kiểm tra nhanh hơn.</small></p></div>
          <div><span>✓</span><p><strong>Bảo mật thông tin</strong><small>Dữ liệu chỉ được dùng để xử lý yêu cầu hỗ trợ.</small></p></div>
        </div>

        <div class="support-quick-links">
          <router-link to="/tra-cuu"><span>🚚</span><div><strong>Tra cứu đơn hàng</strong><small>Kiểm tra trạng thái vận chuyển</small></div><b>›</b></router-link>
          <router-link to="/don-hang"><span>📦</span><div><strong>Đơn hàng của tôi</strong><small>Xem chi tiết đơn đã đặt</small></div><b>›</b></router-link>
        </div>
      </aside>

      <div class="contact-form-col">
        <div class="form-card">
          <div class="form-card-head"><div><span class="form-kicker">GỬI YÊU CẦU</span><h3>Liên hệ với FF T-Shirt</h3><p>Điền thông tin bên dưới, chúng tôi sẽ tiếp nhận yêu cầu của bạn.</p></div><span class="form-head-icon">💬</span></div>
          <form class="contact-form-grid" @submit.prevent="submitContact" novalidate>
            <!-- Họ và tên -->
            <div class="form-group half-field">
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
            <div class="form-group half-field">
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
            <div class="form-group full-field">
              <label>Email <span class="optional-hint">(Không bắt buộc)</span></label>
              <input
                v-model="contactForm.email"
                type="email"
                placeholder="email@example.com"
                class="form-control"
              />
            </div>

            <!-- Nội dung hỗ trợ -->
            <div class="form-group full-field">
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

            <button type="submit" class="btn-submit full-field" :disabled="submitting">
              <span v-if="submitting" class="spinner"></span>
              <span>{{ submitting ? 'Đang gửi thông tin...' : 'Gửi yêu cầu hỗ trợ' }}</span><b v-if="!submitting">→</b>
            </button>
            <p class="form-privacy-note full-field">🔒 Thông tin của bạn được bảo mật và chỉ sử dụng để hỗ trợ yêu cầu này.</p>
          </form>
        </div>
      </div>
    </div>

    <!-- Quy định được tổng hợp từ các luồng nghiệp vụ đang chạy trong dự án -->
    <section class="container-client store-rules-section">
      <div class="rules-heading">
        <span class="rules-kicker">THÔNG TIN CẦN BIẾT</span>
        <h2>Quy định của cửa hàng</h2>
        <p>Vui lòng đọc các quy định dưới đây trước khi đặt hàng để quá trình mua sắm và hỗ trợ được thuận tiện.</p>
      </div>

      <div class="rules-grid">
        <router-link v-for="rule in storeRules" :key="rule.slug" :to="`/lien-he/quy-dinh/${rule.slug}`" class="rule-card">
          <div class="rule-card-title">
            <span class="rule-icon">{{ rule.icon }}</span>
            <div><h3>{{ rule.title }}</h3><small>Quy định {{ rule.items.length }} nội dung chính</small></div>
          </div>
          <p class="rule-summary">{{ rule.summary }}</p>
          <span class="rule-detail-link">Xem quy định chi tiết <b>→</b></span>
        </router-link>
      </div>

      <div class="rules-support-note">
        <span>💬</span>
        <p><strong>Cần hỗ trợ thêm?</strong> Hãy gửi nội dung qua biểu mẫu phía trên và cung cấp mã đơn hàng (nếu có). FF T-Shirt sẽ kiểm tra theo thông tin thực tế của đơn hàng.</p>
      </div>
    </section>

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

/* LOGO THUONG HIEU CHUAN: F XANH NAVY + F VANG GOLD + T-SHIRT XANH NAVY + 3 TIA SANG */
.brand-logo-wrapper {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  font-family: 'Fredoka', -apple-system, sans-serif;
  user-select: none;
}

.sparkles {
  position: absolute;
  top: -12px;
  left: -7px;
  width: 32px;
  height: 32px;
  pointer-events: none;
}

.sparkles .sparkle-aura {
  position: absolute;
  bottom: 2px;
  right: 6px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 252, 235, 0.95) 0%, rgba(251, 191, 36, 0.8) 45%, rgba(217, 119, 6, 0) 75%);
  animation: fireworkAuraTracking 2.4s ease-in-out infinite;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d2a764;
  border-radius: 99px;
  transform-origin: center center;
  animation: fireworkSparkleTracking 2.4s ease-in-out infinite;
}

.sparkles .ray-1 {
  --rot: 12deg;
  width: 4px;
  height: 12px;
  top: 1px;
  right: 6px;
  transform: rotate(12deg);
  animation-delay: 0s;
}

.sparkles .ray-2 {
  --rot: -42deg;
  width: 4px;
  height: 12px;
  top: 5px;
  left: 5px;
  transform: rotate(-42deg);
  animation-delay: 0.35s;
}

.sparkles .ray-3 {
  --rot: -78deg;
  width: 4px;
  height: 12px;
  bottom: 1px;
  left: 0px;
  transform: rotate(-78deg);
  animation-delay: 0.7s;
}

@keyframes fireworkAuraTracking {
  0%, 100% {
    opacity: 0.15;
    transform: scale(0.75);
  }
  35% {
    opacity: 0.95;
    transform: scale(1.35);
    filter: drop-shadow(0 0 5px #f59e0b) drop-shadow(0 0 12px #fbbf24);
  }
  65% {
    opacity: 0.35;
    transform: scale(0.95);
  }
}

@keyframes fireworkSparkleTracking {
  0%, 100% {
    opacity: 0.35;
    transform: rotate(var(--rot, 0deg)) scale(0.88);
    background-color: #d1a868;
    box-shadow: 0 0 2px rgba(209, 168, 104, 0.4);
  }
  25% {
    opacity: 0.8;
    transform: rotate(var(--rot, 0deg)) scale(1.15);
    background-color: #fde68a;
    box-shadow: 0 0 8px #f59e0b, 0 0 14px rgba(245, 158, 11, 0.7);
  }
  45% {
    opacity: 1;
    transform: rotate(var(--rot, 0deg)) scale(1.38);
    background-color: #ffffff;
    box-shadow: 0 0 8px #ffffff, 0 0 16px #fbbf24, 0 0 26px #f59e0b;
    filter: drop-shadow(0 0 5px #ffffff);
  }
  65% {
    opacity: 0.85;
    transform: rotate(var(--rot, 0deg)) scale(1.18);
    background-color: #fcd34d;
    box-shadow: 0 0 8px #f59e0b;
  }
  85% {
    opacity: 0.45;
    transform: rotate(var(--rot, 0deg)) scale(0.95);
    background-color: #d1a868;
  }
}

.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 6px 16px rgba(0, 0, 0, 0.75)) drop-shadow(0 2px 4px rgba(0, 0, 0, 0.6));
}

.logo-name {
  display: flex;
  font-family: 'Fredoka', -apple-system, sans-serif;
  font-size: 48px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -2px;
}

.f-blue {
  color: #496883;
  font-weight: 800;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.9),
    0 3px 6px rgba(0, 0, 0, 0.75),
    0 6px 14px rgba(0, 0, 0, 0.55),
    0 0 2px rgba(255, 255, 255, 0.45);
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.7)) drop-shadow(0 0 1px rgba(255, 255, 255, 0.35));
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
  font-weight: 800;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.85),
    0 3px 6px rgba(0, 0, 0, 0.7),
    0 6px 14px rgba(0, 0, 0, 0.5),
    0 0 2px rgba(210, 167, 100, 0.8);
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.65));
}

.logo-sub {
  font-family: 'Fredoka', -apple-system, sans-serif;
  color: #496883;
  font-size: 17px;
  font-weight: 800;
  line-height: 1;
  margin-left: 7px;
  margin-bottom: 3px;
  letter-spacing: -0.3px;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.9),
    0 2px 5px rgba(0, 0, 0, 0.7),
    0 0 2px rgba(255, 255, 255, 0.35);
  filter: drop-shadow(0 2px 5px rgba(0, 0, 0, 0.65));
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
  max-width: 1440px;
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

/* QUY ĐỊNH CỬA HÀNG */
.support-center-grid{grid-template-columns:minmax(0,.88fr) minmax(520px,1.12fr);gap:24px;align-items:stretch}.support-panel{position:relative;overflow:hidden;padding:32px;border-radius:20px;background:linear-gradient(145deg,#172235,#263d52 62%,#36566f);color:#fff;box-shadow:0 18px 42px rgba(23,34,53,.18)}.support-panel::after{content:'';position:absolute;right:-95px;bottom:-125px;width:280px;height:280px;border-radius:50%;background:rgba(255,255,255,.045)}.support-kicker,.form-kicker{display:inline-block;color:#d2a764;font-size:10px;font-weight:900;letter-spacing:1.3px}.support-panel h2{max-width:480px;margin:8px 0 11px;color:#fff;font-size:27px;line-height:1.25}.support-panel .lead{max-width:560px;margin-bottom:22px;color:#cbd5e1;line-height:1.7}.primary-contact-card{position:relative;z-index:1;display:grid;grid-template-columns:48px 1fr 18px;align-items:center;gap:12px;padding:15px;border:1px solid rgba(255,255,255,.14);border-radius:14px;background:rgba(255,255,255,.09);color:#fff;text-decoration:none;transition:.2s}.primary-contact-card:hover{transform:translateY(-2px);background:rgba(255,255,255,.14)}.primary-contact-icon{width:46px;height:46px;display:grid;place-items:center;border-radius:12px;background:#d2a764;color:#172235;font-size:22px}.primary-contact-card small{display:block;margin-bottom:3px;color:#b9c8d5;font-size:10.5px}.primary-contact-card strong{font-size:14px}.primary-contact-card>b{color:#d2a764;font-size:18px}.support-benefits{position:relative;z-index:1;display:grid;gap:13px;margin-top:22px}.support-benefits>div{display:flex;align-items:flex-start;gap:10px}.support-benefits>div>span{width:23px;height:23px;display:grid;place-items:center;flex:0 0 23px;border-radius:50%;background:rgba(210,167,100,.18);color:#f0c979;font-size:11px;font-weight:900}.support-benefits p{margin:0}.support-benefits strong{display:block;color:#f8fafc;font-size:12px}.support-benefits small{display:block;margin-top:2px;color:#aebfcd;font-size:10.5px;line-height:1.45}.support-quick-links{position:relative;z-index:1;display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:24px}.support-quick-links>a{display:grid;grid-template-columns:30px 1fr 9px;align-items:center;gap:8px;padding:12px;border:1px solid rgba(255,255,255,.1);border-radius:11px;background:rgba(15,23,42,.22);color:#fff;text-decoration:none}.support-quick-links strong{display:block;font-size:11px}.support-quick-links small{display:block;margin-top:2px;color:#9fb1c0;font-size:9px}.support-quick-links b{color:#d2a764;font-size:18px}.contact-form-col,.form-card{height:100%}.form-card{padding:30px;border:1px solid #dbe4ec;border-radius:20px;box-shadow:0 15px 38px rgba(38,61,82,.1)}.form-card-head{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;margin-bottom:24px;padding-bottom:18px;border-bottom:1px solid #edf2f7}.form-card-head h3{margin:5px 0 5px;font-size:22px}.form-card-head p{margin:0;color:#64748b;font-size:11.5px}.form-head-icon{width:46px;height:46px;display:grid;place-items:center;flex:0 0 46px;border-radius:13px;background:#edf3f7;font-size:22px}.contact-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 14px}.half-field{grid-column:span 1}.full-field{grid-column:1/-1}.form-group{margin-bottom:15px}.form-group label{margin-bottom:7px;font-size:11.5px}.form-control{min-height:46px;padding:12px 14px;border-color:#d4dee7;border-radius:10px;background:#f8fafc}.form-control::placeholder{color:#9aa9b8}.form-control:focus{border-color:#496883;background:#fff;box-shadow:0 0 0 4px rgba(73,104,131,.1)}textarea.form-control{min-height:112px;line-height:1.55}.btn-submit{min-height:48px;margin-top:1px;border-radius:11px;background:linear-gradient(135deg,#d2a764,#e3ba78);box-shadow:0 8px 18px rgba(181,140,76,.25)}.btn-submit span{flex:1;text-align:center}.btn-submit b{font-size:19px}.form-privacy-note{margin:10px 0 0;text-align:center;color:#94a3b8;font-size:10px}
.store-rules-section {
  margin-top: 58px;
}

.rules-heading {
  text-align: center;
  max-width: 760px;
  margin: 0 auto 26px;
}

.rules-kicker {
  display: inline-block;
  margin-bottom: 7px;
  color: #b58c4c;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.2px;
}

.rules-heading h2 {
  margin: 0 0 9px;
  color: #172235;
  font-size: 28px;
  font-weight: 800;
}

.rules-heading p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.rule-card {
  display: block;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 5px 18px rgba(15, 23, 42, 0.045);
  color: inherit;
  text-decoration: none;
  transition: transform .22s ease, border-color .22s ease, box-shadow .22s ease;
}

.rule-card:hover {
  transform: translateY(-4px);
  border-color: #d2a764;
  box-shadow: 0 13px 28px rgba(38, 61, 82, .12);
}

.rule-card-title {
  display: flex;
  align-items: center;
  gap: 11px;
  padding-bottom: 13px;
  margin-bottom: 12px;
  border-bottom: 1px solid #edf2f7;
}

.rule-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  flex: 0 0 38px;
  border-radius: 10px;
  background: #edf3f7;
  font-size: 19px;
}

.rule-card h3 {
  margin: 0;
  color: #263d52;
  font-size: 15px;
  font-weight: 800;
}

.rule-card-title small {
  display: block;
  margin-top: 3px;
  color: #94a3b8;
  font-size: 10.5px;
}

.rule-summary {
  min-height: 42px;
  margin: 0 0 18px;
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.6;
}

.rule-detail-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #496883;
  font-size: 12px;
  font-weight: 800;
}

.rule-detail-link b {
  font-size: 18px;
  transition: transform .2s ease;
}

.rule-card:hover .rule-detail-link b {
  transform: translateX(4px);
}

.rule-card ul {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.rule-card li {
  position: relative;
  padding-left: 17px;
  color: #526273;
  font-size: 12.5px;
  line-height: 1.55;
}

.rule-card li::before {
  content: '';
  position: absolute;
  top: 7px;
  left: 1px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d2a764;
}

.rules-support-note {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 18px;
  padding: 16px 18px;
  border: 1px solid #cfdae4;
  border-radius: 12px;
  background: #eef4f7;
}

.rules-support-note > span {
  font-size: 20px;
}

.rules-support-note p {
  margin: 0;
  color: #41566a;
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 860px) {
  .content-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .store-rules-section {
    margin-top: 40px;
  }

  .rules-grid {
    grid-template-columns: 1fr;
  }

  .rules-heading h2 {
    font-size: 23px;
  }

  .support-center-grid{grid-template-columns:1fr}.support-panel{padding:24px}.contact-form-grid{grid-template-columns:1fr}.half-field,.full-field{grid-column:1}.support-quick-links{grid-template-columns:1fr 1fr}
}

@media(max-width:560px){.support-panel{padding:21px 16px;border-radius:16px}.support-panel h2{font-size:22px}.support-quick-links{grid-template-columns:1fr}.form-card{padding:21px 15px;border-radius:16px}.form-card-head h3{font-size:19px}.form-head-icon{width:40px;height:40px;flex-basis:40px}.content-grid{margin-top:20px;padding-inline:10px}}
</style>
