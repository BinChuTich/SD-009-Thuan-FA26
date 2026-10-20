<script setup>
import { ref } from 'vue'

const contactForm = ref({
  name: '',
  phone: '',
  email: '',
  message: ''
})

const submitContact = () => {
  alert('Cảm ơn bạn đã liên hệ! FF T-Shirt sẽ phản hồi lại cho bạn sớm nhất có thể.')
  contactForm.value = { name: '', phone: '', email: '', message: '' }
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

    <!-- 2. NỘI DUNG GỌN GÀNG: THÔNG TIN HỖ TRỢ & FORM GỬI TIN NHẮN (ĐÃ BỎ MAP) -->
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
              <p>support@fftshirt.vn / cskh@fftshirt.vn</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột phải: Form gửi tin nhắn gọn gàng -->
      <div class="contact-form-col">
        <div class="form-card">
          <h3>Gửi Tin Nhắn Cho Chúng Tôi</h3>
          <form @submit.prevent="submitContact">
            <div class="form-group">
              <label>Họ và tên của bạn</label>
              <input v-model="contactForm.name" required type="text" placeholder="Nguyễn Văn A" class="form-control" />
            </div>
            <div class="form-group">
              <label>Số điện thoại</label>
              <input v-model="contactForm.phone" required type="tel" placeholder="0912 345 678" class="form-control" />
            </div>
            <div class="form-group">
              <label>Email</label>
              <input v-model="contactForm.email" type="email" placeholder="email@example.com" class="form-control" />
            </div>
            <div class="form-group">
              <label>Nội dung cần hỗ trợ</label>
              <textarea v-model="contactForm.message" required rows="4" placeholder="Nhập câu hỏi hoặc yêu cầu của bạn..." class="form-control"></textarea>
            </div>
            <button type="submit" class="btn-submit">Gửi Thông Tin &rarr;</button>
          </form>
        </div>
      </div>
    </div>
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
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 6px;
}

.form-control {
  width: 100%;
  padding: 11px 14px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
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
}

.btn-submit:hover {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.45);
}

@media (max-width: 860px) {
  .content-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}
</style>
