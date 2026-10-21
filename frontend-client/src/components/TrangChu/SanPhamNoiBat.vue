<script setup>
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'

const router = useRouter()

const props = defineProps({
  products: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  categoryTabs: {
    type: Array,
    default: () => []
  },
  selectedCategoryTab: {
    type: String,
    default: 'ALL'
  }
})

const emit = defineEmits(['update:selectedCategoryTab'])

const handleImgError = (e) => {
  e.target.src = '/images/products/default_tshirt.jpg'
}

const goToProduct = (id) => {
  router.push(`/san-pham/${id}`)
}
</script>

<template>
  <section class="featured-section" id="san-pham-noi-bat">
    <div class="container-featured">
      <div class="section-title-wrapper text-center">
        <span class="badge-sub-title">THIẾT KẾ ĐƯỢC YÊU THÍCH</span>
        <h2 class="script-title">Sản Phẩm Nổi Bật</h2>
        <p class="script-subtitle">Bộ sưu tập áo phông bán chạy nhất mùa này với chất liệu Cotton cao cấp</p>
      </div>

      <!-- Tab lọc danh mục nhanh ngay trên trang chủ -->
      <div class="home-cat-tabs">
        <button
          v-for="tab in categoryTabs"
          :key="tab.id"
          :class="{ active: selectedCategoryTab === tab.id }"
          @click="emit('update:selectedCategoryTab', tab.id)"
        >
          {{ tab.name }}
        </button>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải bộ sưu tập...</p>
      </div>

      <div v-else-if="products.length > 0" class="boutique-product-grid four-cols">
        <div
          v-for="p in products"
          :key="p.id"
          class="boutique-card"
          @click="goToProduct(p.id)"
        >
          <div class="card-media">
            <img
              :src="p.anhDaiDien || (p.hinhAnhs && p.hinhAnhs[0]) || '/images/products/default_tshirt.jpg'"
              :alt="p.tenSanPham"
              @error="handleImgError"
              loading="lazy"
            />
            <span v-if="p.coGiamGia && p.phanTramGiam > 0" class="tag-percent">
              -{{ Math.round(p.phanTramGiam) }}%
            </span>
          </div>

          <div class="card-details">
            <span class="card-category-badge">{{ p.tenDanhMuc || 'Áo phông' }}</span>
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
  max-width: 1200px;
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

.home-cat-tabs {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}

.home-cat-tabs button {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.home-cat-tabs button:hover {
  border-color: #cbd5e1;
  color: #1e293b;
}

.home-cat-tabs button.active {
  background: #496883;
  border-color: #496883;
  color: #ffffff;
  box-shadow: 0 3px 8px rgba(73, 104, 131, 0.25);
}

.boutique-product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
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
}

.boutique-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(73, 104, 131, 0.12);
  border-color: #cbd5e1;
}

.card-media {
  position: relative;
  width: 100%;
  padding-top: 105%;
  overflow: hidden;
  background: #f1f5f9;
}

.card-media img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
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

@media (max-width: 992px) {
  .boutique-product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .boutique-product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .card-details { padding: 12px; }
  .product-title-text { font-size: 13px; min-height: 36px; }
}
</style>

