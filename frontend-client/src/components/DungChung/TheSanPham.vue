<script setup>
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const handleImgError = (e) => {
  e.target.src = '/images/products/default_tshirt.jpg'
}

const goToDetail = () => {
  if (props.product?.id) {
    router.push(`/san-pham/${props.product.id}`)
  }
}
</script>

<template>
  <div class="the-san-pham-card" @click="goToDetail">
    <div class="card-img-holder">
      <img
        :src="product.anhDaiDien || (product.hinhAnhs && product.hinhAnhs[0]) || '/images/products/default_tshirt.jpg'"
        :alt="product.tenSanPham"
        @error="handleImgError"
        loading="lazy"
      />
      <!-- Tag giảm giá -->
      <span v-if="product.coGiamGia && product.phanTramGiam > 0" class="discount-badge-red">
        -{{ Math.round(product.phanTramGiam) }}%
      </span>
      <!-- Nút xem nhanh khi hover -->
      <div class="quick-hover-action">
        <span>Xem chi tiết &rarr;</span>
      </div>
    </div>

    <div class="card-caption">
      <span class="card-cat-badge">{{ product.tenDanhMuc || 'Áo Phông Nam' }}</span>
      <h3 class="product-name" :title="product.tenSanPham">{{ product.tenSanPham }}</h3>
      
      <div class="price-container">
        <template v-if="product.coGiamGia && product.minGiaSauGiam && product.minGiaSauGiam < product.minGia">
          <span class="price-red">
            {{ formatCurrency(product.minGiaSauGiam) }}
          </span>
          <span class="price-struck">
            {{ formatCurrency(product.minGia) }}
          </span>
        </template>
        <template v-else>
          <span class="price-normal">
            {{ formatCurrency(product.minGia || product.giaBan || 0) }}
          </span>
        </template>
      </div>

      <div class="card-footer-tags">
        <span
          class="stock-tag"
          :class="{ 'out-of-stock': (product.tongSoLuong != null ? product.tongSoLuong : (product.soLuongTon || 0)) <= 0 }"
        >
          {{ (product.tongSoLuong != null ? product.tongSoLuong : (product.soLuongTon || 0)) > 0 ? 'Còn hàng' : 'Tạm hết hàng' }}
        </span>
        <span v-if="product.tenChatLieu || product.chatLieu" class="material-tag">
          {{ product.tenChatLieu || product.chatLieu }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.the-san-pham-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
}

.the-san-pham-card:hover {
  transform: translateY(-4px);
  border-color: #d2a764;
  box-shadow: 0 10px 24px rgba(73, 104, 131, 0.12);
}

.card-img-holder {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1.1;
  background-color: #f1f5f9;
  overflow: hidden;
}

.card-img-holder img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.the-san-pham-card:hover .card-img-holder img {
  transform: scale(1.05);
}

.discount-badge-red {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #e11d48;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(225, 29, 72, 0.35);
  z-index: 2;
}

.quick-hover-action {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(30, 41, 59, 0.85), transparent);
  padding: 24px 12px 10px;
  display: flex;
  justify-content: center;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.25s ease;
}

.quick-hover-action span {
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  background: rgba(210, 167, 100, 0.9);
  padding: 4px 12px;
  border-radius: 20px;
}

.the-san-pham-card:hover .quick-hover-action {
  opacity: 1;
  transform: translateY(0);
}

.card-caption {
  padding: 14px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.card-cat-badge {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
  line-height: 1.4;
  height: 2.8em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price-container {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: auto;
  margin-bottom: 8px;
}

.price-red {
  color: #e11d48;
  font-size: 16px;
  font-weight: 800;
}

.price-struck {
  color: #94a3b8;
  font-size: 12px;
  text-decoration: line-through;
}

.price-normal {
  color: #1e293b;
  font-size: 16px;
  font-weight: 800;
}

.card-footer-tags {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  padding-top: 8px;
  border-top: 1px dashed #f1f5f9;
}

.stock-tag {
  color: #16a34a;
  font-weight: 600;
}

.stock-tag.out-of-stock {
  color: #dc2626;
}

.material-tag {
  color: #64748b;
}
</style>

