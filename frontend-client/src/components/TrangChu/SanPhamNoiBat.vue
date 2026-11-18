<script setup>
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const router = useRouter()

const props = defineProps({
  products: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const handleImgError = (e) => {
  e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER
}

const goToProduct = (id) => {
  router.push(`/san-pham/${id}`)
}
</script>

<template>
  <section class="featured-section" id="san-pham-noi-bat">
    <div class="container-featured">
      <div class="section-title-wrapper text-center">
        <span class="badge-sub-title">TOP BÁN CHẠY NHẤT</span>
        <h2 class="script-title">Sản Phẩm Nổi Bật</h2>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải bộ sưu tập...</p>
      </div>

      <div v-else-if="products.length > 0" class="boutique-product-grid">
        <div
          v-for="p in products"
          :key="p.id"
          class="boutique-card"
          @click="goToProduct(p.id)"
        >
          <div class="card-media">
            <img
              :src="resolveProductImageUrl(p.anhDaiDien || (p.hinhAnhs && p.hinhAnhs[0]))"
              :alt="p.tenSanPham"
              @error="handleImgError"
              loading="lazy"
            />
            <span v-if="p.coGiamGia && p.phanTramGiam > 0" class="tag-percent">
              -{{ Math.round(p.phanTramGiam) }}%
            </span>
            <span v-if="(p.daBan || p.soLuongDaBan || 0) > 0" class="tag-best-seller">
              Bán chạy
            </span>
          </div>

          <div class="card-details">
            <div class="card-details-top">
              <span class="card-category-badge">{{ p.tenDanhMuc || 'Áo phông' }}</span>
              <span class="card-sold-count">
                <svg class="cart-sold-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C3.74 11.846 4.632 14 6.414 14H15a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 3H6.28l-.24-1.04A1 1 0 005.064 1H3z" />
                </svg>
                Đã bán {{ p.daBan || p.soLuongDaBan || 0 }}
              </span>
            </div>
            <h3 class="product-title-text">{{ p.tenSanPham }}</h3>
            <div class="product-pricing">
              <template v-if="p.coGiamGia && p.minGiaSauGiam && p.minGiaSauGiam < p.minGia">
                <span class="price-current">{{ formatCurrency(p.minGiaSauGiam) }}</span>
                <span class="price-struck">{{ formatCurrency(p.minGia) }}</span>
              </template>
              <template v-else>
                <span class="price-current">{{ formatCurrency(p.minGia || 0) }}</span>
              </template>
            </div>
            <button class="btn-detail-link">Xem chi tiết</button>
          </div>
        </div>
      </div>

      <div v-else class="empty-category-notice">
        <p>Chưa có sản phẩm nào thuộc danh mục này.</p>
      </div>

      <div class="view-all-center-btn">
        <router-link to="/cua-hang" class="btn-view-all-shop">
          Khám Phá Toàn Bộ Bộ Sưu Tập Áo Phông Tại Cửa Hàng &rarr;
        </router-link>
      </div>
    </div>
  </section>
</template>

<style scoped>
.featured-section {
  padding: 60px 0 65px;
  background-color: #ffffff;
  scroll-margin-top: 80px;
}

.container-featured {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 20px;
}

.section-title-wrapper {
  margin-bottom: 28px;
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


.boutique-product-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1280px) {
  .boutique-product-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .boutique-product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
  }
}

.boutique-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eef2f6;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.boutique-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(73, 104, 131, 0.12);
  border-color: #cbd5e1;
}

.card-media {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 5;
  overflow: hidden;
  background: #f1f5f9;
}

.card-media img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform 0.4s ease;
}

.boutique-card:hover .card-media img {
  transform: scale(1.06);
}

.tag-percent {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 4px;
}

.card-details {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tag-best-seller {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #d2a764;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(210, 167, 100, 0.35);
  letter-spacing: 0.2px;
}

.card-details-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.card-sold-count {
  font-size: 11.5px;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.cart-sold-icon {
  width: 12px;
  height: 12px;
  color: #d2a764;
}

.card-category-badge {
  font-size: 11px;
  color: #496883;
  font-weight: 700;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.product-title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 10px;
  line-height: 1.4;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-pricing {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.price-current {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.price-struck {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
}

.btn-detail-link {
  margin-top: auto;
  width: 100%;
  background: #f8fafc;
  color: #496883;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.boutique-card:hover .btn-detail-link {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.view-all-center-btn {
  text-align: center;
  margin-top: 36px;
}

.btn-view-all-shop {
  display: inline-block;
  background: #f8fafc;
  color: #496883;
  border: 1.5px solid #cbd5e1;
  padding: 12px 28px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.25s;
}

.btn-view-all-shop:hover {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.2);
}

.loading-state, .empty-category-notice {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 680px) {
  .featured-section { padding: 34px 0 42px; }
  .container-featured { padding: 0 12px; }
  .section-title-wrapper { margin-bottom: 18px; }
  .script-title { font-size: 22px; }
  .boutique-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
  .boutique-card { border-radius: 10px; box-shadow: 0 2px 8px rgba(15, 23, 42, .05); }
  .card-details { padding: 10px; }
  .card-details-top { gap: 4px; margin-bottom: 4px; }
  .card-category-badge, .card-sold-count { font-size: 9.5px; }
  .cart-sold-icon { display: none; }
  .product-title-text { font-size: 12.5px; min-height: 35px; margin-bottom: 7px; }
  .product-pricing { gap: 5px; margin-bottom: 0; flex-wrap: wrap; }
  .price-current { font-size: 14px; }
  .price-struck { font-size: 10.5px; }
  .btn-detail-link { display: none; }
  .tag-best-seller, .tag-percent { top: 7px; font-size: 9.5px; padding: 3px 6px; }
  .tag-best-seller { right: 7px; }
  .tag-percent { left: 7px; }
  .view-all-center-btn { margin-top: 24px; }
  .btn-view-all-shop { width: 100%; padding: 11px 14px; font-size: 12px; box-sizing: border-box; }
}
</style>

