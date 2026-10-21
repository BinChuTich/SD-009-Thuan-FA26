<script setup>
const customerReviews = [
  {
    id: 1,
    name: 'Đặng Tuấn Anh',
    tag: 'Khách hàng thân thiết',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&fit=crop&q=80',
    product: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    rating: 5,
    comment: 'Chất vải 100% Cotton sờ vào cực kỳ mịn tay và dày dặn. Mình giặt máy 4-5 lần rồi mà cổ áo không hề bị bai dão như mấy áo trước đây mua ở shop khác. Rất đáng tiền!'
  },
  {
    id: 2,
    name: 'Nguyễn Văn Hoàng',
    tag: 'Khách hàng văn phòng',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&fit=crop&q=80',
    product: 'Áo Polo Nam Cổ Bẻ Cao Cấp',
    rating: 5,
    comment: 'Áo Polo mặc đi làm công sở cực kỳ lịch sự và chuẩn form. Phần vải mắt chim thoáng khí tốt nên ngồi văn phòng hay ra ngoài cà phê đều rất dễ chịu.'
  },
  {
    id: 3,
    name: 'Trần Minh Quân',
    tag: 'Người yêu thời trang',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&fit=crop&q=80',
    product: 'Áo Phông Unisex In Hình Graphic',
    rating: 5,
    comment: 'Hình in sắc nét và bền, form rộng unisex mặc phối với quần short hay quần túi hộp bao đẹp. Giao hàng nhanh, shipper còn cho kiểm tra áo kỹ mới thu tiền.'
  }
]
</script>

<template>
  <section class="reviews-section">
    <div class="container-reviews">
      <div class="section-title-wrapper text-center">
        <span class="badge-sub-title">TRẢI NGHIỆM THỰC TẾ</span>
        <h2 class="script-title">Khách Hàng Nói Gì Về FF T-Shirt?</h2>
        <p class="script-subtitle">Hơn 10.000+ khách hàng trên toàn quốc đã tin tưởng và đồng hành cùng sản phẩm của chúng tôi</p>
      </div>

      <div class="reviews-grid">
        <div
          v-for="rev in customerReviews"
          :key="rev.id"
          class="review-card"
        >
          <div class="rev-stars">★★★★★</div>
          <p class="rev-comment">"{{ rev.comment }}"</p>
          
          <div class="rev-product-purchased">
            <span class="bag-icon">🛍️</span>
            <span>Đã mua: <strong>{{ rev.product }}</strong></span>
          </div>

          <div class="rev-user-profile">
            <img :src="rev.avatar" :alt="rev.name" class="rev-avatar" loading="lazy" />
            <div class="rev-user-details">
              <strong class="rev-name">{{ rev.name }}</strong>
              <span class="rev-tag">✓ {{ rev.tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.reviews-section {
  padding: 64px 0;
  background: #ffffff;
}

.container-reviews {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 20px;
}

.section-title-wrapper {
  margin-bottom: 36px;
}

.text-center {
  text-align: center;
}

.badge-sub-title {
  display: inline-block;
  background: #f1f5f9;
  color: #496883;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 8px;
}

.script-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 6px;
}

.script-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.reviews-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.review-card {
  background: #f8fafc;
  border-radius: 14px;
  padding: 26px 24px;
  border: 1px solid #eef2f6;
  display: flex;
  flex-direction: column;
  transition: all 0.25s;
}

.review-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.rev-stars {
  color: #f59e0b;
  font-size: 16px;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.rev-comment {
  font-size: 13px;
  color: #334155;
  line-height: 1.6;
  margin: 0 0 16px;
  flex: 1;
  font-style: italic;
}

.rev-product-purchased {
  background: #ffffff;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
}

.rev-product-purchased strong {
  color: #496883;
}

.rev-user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rev-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.rev-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  display: block;
}

.rev-tag {
  font-size: 11px;
  color: #16a34a;
  font-weight: 600;
}

@media (max-width: 992px) {
  .reviews-grid {
    grid-template-columns: 1fr;
  }
}
</style>

