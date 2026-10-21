<script setup>
</script>

<template>
  <div class="intro-page">
    <div class="intro-hero">
      <div class="container-client">
        <h1 class="intro-title">Về Chúng Tôi — FF T-SHIRT</h1>
        <p class="intro-sub">
          Định hình phong cách thời trang phái mạnh bằng sự tối giản, tinh tế và chất lượng vượt trội.
        </p>
      </div>
    </div>

    <div class="container-client content-section">
      <div class="story-grid">
        <div class="story-text">
          <span class="sec-tag">CÂU CHUYỆN THƯƠNG HIỆU</span>
          <h2>Hành Trình Kiến Tạo Chiếc Áo Phông Hoàn Hảo</h2>
          <p>
            Được ra đời từ đam mê thời trang thường ngày, <strong>FF T-SHIRT</strong> luôn tâm niệm rằng: 
            <em>"Một chiếc áo phông tốt không chỉ cần đẹp mắt, mà phải mang lại sự tự tin, thoải mái suốt 24 giờ trong ngày."</em>
          </p>
          <p>
            Chúng tôi tỉ mỉ trong từng đường kim mũi chỉ, lựa chọn sợi bông Cotton tự nhiên 100% không pha tạp, 
            kết hợp công nghệ dệt định lượng 230gsm cao cấp để áo luôn giữ chuẩn form dáng sau nhiều lần giặt mà không hề bai dão hay co rút.
          </p>
          <div class="stats-row">
            <div class="stat-item">
              <strong>100%</strong>
              <span>Cotton Tự Nhiên</span>
            </div>
            <div class="stat-item">
              <strong>50.000+</strong>
              <span>Khách hàng hài lòng</span>
            </div>
            <div class="stat-item">
              <strong>7 Ngày</strong>
              <span>Đổi trả tận nơi</span>
            </div>
          </div>
        </div>

        <div class="story-image">
          <img
            src="https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&q=80"
            alt="Xưởng may FF T-Shirt"
            class="rounded-img"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.intro-page {
  background-color: #faf8f6;
  min-height: 80vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

.intro-hero {
  background: #3b2c25;
  color: #ffffff;
  padding: 60px 0;
  text-align: center;
}

.intro-title {
  font-family: 'Be Vietnam Pro', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-weight: 800;
  letter-spacing: -0.01em;
  font-size: 34px;
  margin: 0 0 12px;
}

.intro-sub {
  color: #d7ccc8;
  font-size: 16px;
  max-width: 650px;
  margin: 0 auto;
}

.container-client {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.content-section {
  margin-top: 50px;
}

.story-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 48px;
  align-items: center;
}

.sec-tag {
  color: #8d6e63;
  font-weight: 700;
  font-size: 12px;
  letter-spacing: 1.5px;
}

.story-text h2 {
  font-size: 28px;
  color: #2b2320;
  margin: 10px 0 20px;
}

.story-text p {
  color: #555;
  line-height: 1.7;
  font-size: 15px;
  margin-bottom: 16px;
}

.stats-row {
  display: flex;
  gap: 32px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e0d8d3;
}

.stat-item strong {
  display: block;
  font-size: 26px;
  color: #3b2c25;
}

.stat-item span {
  font-size: 13px;
  color: #8c7a72;
}

.rounded-img {
  width: 100%;
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1);
}

@media (max-width: 860px) {
  .story-grid {
    grid-template-columns: 1fr;
  }
}
</style>

