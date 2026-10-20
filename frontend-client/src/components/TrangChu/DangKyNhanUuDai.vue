<script setup>
import { ref } from 'vue'

const newsletterEmail = ref('')
const newsletterSuccess = ref(false)

const subscribeNewsletter = () => {
  if (newsletterEmail.value && newsletterEmail.value.includes('@')) {
    newsletterSuccess.value = true
  }
}
</script>

<template>
  <section class="newsletter-vip-section">
    <div class="container-newsletter">
      <div class="newsletter-card-inner">
        <div class="newsletter-content text-center">
          <span class="vip-badge">🎁 ĐẶC QUYỀN THÀNH VIÊN</span>
          <h2 class="newsletter-title">Nhận Ngay Voucher Giảm 10% Cho Đơn Hàng Đầu Tiên</h2>
          <p class="newsletter-sub">Đăng ký email để nhận mã ưu đãi độc quyền, cập nhật các mẫu áo mới nhất và tham gia ngày hội VIP Member hàng tháng</p>
          
          <div v-if="!newsletterSuccess" class="newsletter-form">
            <input
              v-model="newsletterEmail"
              type="email"
              placeholder="Nhập địa chỉ email của bạn..."
              class="newsletter-input"
            />
            <button class="btn-newsletter-submit" @click="subscribeNewsletter">
              Đăng Ký Ngay
            </button>
          </div>

          <div v-else class="newsletter-success-box">
            <span class="check-icon">✓</span>
            <p>
              Chúc mừng bạn! Mã voucher 10% của bạn là:
              <strong class="voucher-code">FFCHAOHE10</strong>
            </p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.newsletter-vip-section {
  padding: 60px 0 70px;
  background: #ffffff;
}

.container-newsletter {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.newsletter-card-inner {
  background: linear-gradient(135deg, #1e293b 0%, #334e68 100%);
  border-radius: 20px;
  padding: 50px 30px;
  color: #ffffff;
  box-shadow: 0 15px 35px rgba(30, 41, 59, 0.2);
}

.text-center {
  text-align: center;
}

.vip-badge {
  display: inline-block;
  background: rgba(210, 167, 100, 0.2);
  color: #d2a764;
  border: 1px solid rgba(210, 167, 100, 0.4);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
  padding: 4px 14px;
  border-radius: 20px;
  margin-bottom: 14px;
}

.newsletter-title {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 12px;
  color: #ffffff;
}

.newsletter-sub {
  font-size: 14px;
  color: #cbd5e1;
  max-width: 600px;
  margin: 0 auto 28px;
  line-height: 1.6;
}

.newsletter-form {
  display: flex;
  max-width: 520px;
  margin: 0 auto;
  gap: 10px;
}

.newsletter-input {
  flex: 1;
  padding: 14px 20px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: #ffffff;
  color: #1e293b;
  font-size: 14px;
  outline: none;
}

.newsletter-input:focus {
  border-color: #d2a764;
}

.btn-newsletter-submit {
  background: #d2a764;
  color: #1e293b;
  font-weight: 800;
  font-size: 14px;
  padding: 14px 26px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.25s;
  white-space: nowrap;
}

.btn-newsletter-submit:hover {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(210, 167, 100, 0.35);
}

.newsletter-success-box {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(210, 167, 100, 0.4);
  padding: 16px 24px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
}

.check-icon {
  font-size: 26px;
  color: #4ade80;
  font-weight: bold;
}

.voucher-code {
  background: #d2a764;
  color: #ffffff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 600px) {
  .newsletter-card-inner {
    padding: 30px 16px;
  }
  .newsletter-title {
    font-size: 20px;
  }
  .newsletter-form {
    flex-direction: column;
  }
}
</style>

