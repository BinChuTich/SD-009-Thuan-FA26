<template>
  <section class="editorial-story-section">
    <div class="container-story">
      <div class="story-grid">
        <div class="story-text-col">
          <span class="story-tag">CÂU CHUYỆN THƯƠNG HIỆU</span>
          <h2 class="story-heading">Định Hình Phong Cách Áo Phông Việt</h2>
          <p class="story-p">
            Tại <strong>FF T-Shirt</strong>, chúng tôi tin rằng một chiếc áo phông tốt không chỉ nằm ở kiểu dáng bắt mắt, mà cốt lõi nằm ở chất lượng sợi bông tự nhiên và độ hoàn thiện tỉ mỉ trên từng đường kim mũi chỉ.
          </p>
          <p class="story-p">
            Không sử dụng sợi pha rẻ tiền gây bí bách hay bai dão sau vài lần giặt, mỗi sản phẩm FF T-Shirt được tuyển chọn từ nguồn bông hữu cơ cao cấp, kết hợp công nghệ dệt nén 260GSM đem lại độ đứng form hoàn hảo và thoáng khí suốt ngày dài.
          </p>
          
          <div class="story-stats-row">
            <div class="stat-item">
              <strong>100%</strong>
              <span>Cotton Tự Nhiên</span>
            </div>
            <div class="stat-item">
              <strong>50+</strong>
              <span>Lần Giặt Chống Nhão</span>
            </div>
            <div class="stat-item">
              <strong>60</strong>
              <span>Ngày Đổi Size Tận Nhà</span>
            </div>
          </div>

          <router-link to="/gioi-thieu" class="story-link-btn">
            Tìm Hiểu Thêm Về Chúng Tôi &rarr;
          </router-link>
        </div>

        <div class="story-media-col">
          <div class="img-composition">
            <img
              src="https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&auto=format&fit=crop&q=80"
              alt="FF T-Shirt Craftsmanship"
              class="comp-img-main"
              loading="lazy"
            />
            <div class="comp-badge-box">
              <span class="badge-num">2026</span>
              <span class="badge-lbl">Tiêu chuẩn may đo hiện đại</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.editorial-story-section {
  padding: 70px 0;
  background-color: #ffffff;
}

.container-story {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.story-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 50px;
  align-items: center;
}

.story-tag {
  display: inline-block;
  background: #f1f5f9;
  color: #496883;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 12px;
}

.story-heading {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 18px;
  line-height: 1.3;
}

.story-p {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
  margin: 0 0 16px;
}

.story-stats-row {
  display: flex;
  gap: 28px;
  margin: 20px 0 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-item strong {
  font-size: 22px;
  font-weight: 900;
  color: #496883;
}

.stat-item span {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

.story-link-btn {
  display: inline-block;
  color: #496883;
  font-weight: 700;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s;
}

.story-link-btn:hover {
  color: #d2a764;
  transform: translateX(4px);
}

.img-composition {
  position: relative;
  width: 100%;
  height: 380px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
}

.comp-img-main {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comp-badge-box {
  position: absolute;
  bottom: 20px;
  left: 20px;
  background: rgba(30, 41, 59, 0.9);
  backdrop-filter: blur(8px);
  color: #ffffff;
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px solid rgba(210, 167, 100, 0.4);
}

.badge-num {
  display: block;
  font-size: 18px;
  font-weight: 900;
  color: #d2a764;
}

.badge-lbl {
  font-size: 11px;
  color: #cbd5e1;
}

@media (max-width: 992px) {
  .story-grid {
    grid-template-columns: 1fr;
    gap: 36px;
  }
  .img-composition {
    height: 300px;
  }
}
</style>

