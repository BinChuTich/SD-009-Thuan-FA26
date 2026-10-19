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
    <div class="contact-hero">
      <div class="container-client">
        <h1 class="page-title">Liên Hệ Với FF T-SHIRT</h1>
        <p class="page-sub">Chúng tôi luôn lắng nghe và sẵn sàng giải đáp mọi thắc mắc của quý khách hàng</p>
      </div>
    </div>

    <div class="container-client content-grid">
      <div class="contact-info-col">
        <h3>Thông Tin Hỗ Trợ Khách Hàng</h3>
        <p class="lead">Đừng ngần ngại liên hệ với chúng tôi bất cứ khi nào bạn cần tư vấn về sản phẩm, size áo hoặc đơn hàng.</p>

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
            <button type="submit" class="btn-submit">Gửi Thông Tin</button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contact-page {
  background-color: #faf8f6;
  min-height: 80vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

.contact-hero {
  background: #3b2c25;
  color: #ffffff;
  padding: 50px 0;
  text-align: center;
}

.page-title {
  font-family: 'Playfair Display', serif;
  font-size: 32px;
  margin: 0 0 10px;
}

.page-sub {
  color: #d7ccc8;
  font-size: 15px;
  margin: 0;
}

.container-client {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  margin-top: 50px;
}

.contact-info-col h3 {
  font-size: 22px;
  color: #2b2320;
  margin: 0 0 12px;
}

.lead {
  color: #666;
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
}

.info-item .icon {
  font-size: 22px;
  background: #ede8e3;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-item strong {
  display: block;
  font-size: 14px;
  color: #2b2320;
  margin-bottom: 4px;
}

.info-item p {
  margin: 0;
  font-size: 13px;
  color: #555;
}

.form-card {
  background: #ffffff;
  border: 1px solid #ede8e3;
  border-radius: 14px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
}

.form-card h3 {
  margin: 0 0 20px;
  font-size: 18px;
  color: #2b2320;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #4b3d36;
  margin-bottom: 6px;
}

.form-control {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  box-sizing: border-box;
}

.btn-submit {
  width: 100%;
  padding: 12px 0;
  background: #533e35;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-submit:hover {
  background: #423029;
}

@media (max-width: 860px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>

