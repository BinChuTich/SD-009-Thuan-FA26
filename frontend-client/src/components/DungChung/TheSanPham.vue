<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  targetColor: {
    type: String,
    default: ''
  }
})

const router = useRouter()

const localColor = ref('')

watch(() => props.targetColor, (newVal) => {
  localColor.value = newVal || ''
}, { immediate: true })

const activeColor = computed(() => localColor.value || props.targetColor || '')

const handleImgError = (e) => {
  e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER
}

const displayImage = computed(() => {
  const chosen = activeColor.value
  if (chosen && props.product?.colorImageMap) {
    const target = chosen.toLowerCase()
    const matchedKey = Object.keys(props.product.colorImageMap).find(k =>
      k.toLowerCase().includes(target) || target.includes(k.toLowerCase())
    )
    if (matchedKey && props.product.colorImageMap[matchedKey]) {
      return props.product.colorImageMap[matchedKey]
    }
  }
  return props.product?.variantImage
    || props.product?.anhDaiDien
    || (props.product?.hinhAnhs && props.product.hinhAnhs[0])
})

const COLOR_HEX_MAP = {
  'đen': '#1a1a1a',
  'trắng': '#ffffff',
  'xám': '#94a3b8',
  'ghi': '#94a3b8',
  'xanh navy': '#1e3a8a',
  'navy': '#1e3a8a',
  'xanh': '#2563eb',
  'đỏ': '#dc2626',
  'vàng': '#eab308',
  'be': '#d4b996',
  'nâu': '#78350f'
}

const getColorHex = (name) => {
  const n = (name || '').toLowerCase()
  for (const [k, v] of Object.entries(COLOR_HEX_MAP)) {
    if (n.includes(k)) return v
  }
  return '#cbd5e1'
}

const availableColors = computed(() => {
  const list = props.product?.colors || props.product?.allColors || []
  if (Array.isArray(list) && list.length > 0) return list
  if (props.product?.colorImageMap) return Object.keys(props.product.colorImageMap)
  return []
})

const selectLocalColor = (color) => {
  localColor.value = color
}

const goToDetail = () => {
  if (props.product?.id) {
    const color = activeColor.value || props.product?.targetColor
    if (color) {
      router.push({
        path: `/san-pham/${props.product.id}`,
        query: { color }
      })
    } else {
      router.push(`/san-pham/${props.product.id}`)
    }
  }
}
</script>

<template>
  <div class="the-san-pham-card" @click="goToDetail">
    <div class="card-img-holder">
      <img
        :src="resolveProductImageUrl(displayImage)"
        :alt="product.tenSanPham"
        @error="handleImgError"
        loading="lazy"
      />
      <!-- Tag màu sắc đang hiển thị -->
      <span v-if="activeColor" class="target-color-pill">
        🎨 Màu {{ activeColor }}
      </span>
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
      <div class="card-top-row">
        <span class="card-cat-badge">{{ product.tenDanhMuc || 'Áo Phông Nam' }}</span>
        <span class="card-sold-badge">
          Đã bán {{ product.daBan || product.soLuongDaBan || 0 }}
        </span>
      </div>

      <h3 class="product-name" :title="product.tenSanPham">{{ product.tenSanPham }}</h3>

      <!-- Bảng chọn màu nhanh trên thẻ sản phẩm -->
      <div v-if="availableColors.length > 1" class="card-color-swatches" @click.stop>
        <div class="swatches-dots">
          <span
            v-for="color in availableColors"
            :key="color"
            class="color-dot"
            :class="{ active: (activeColor || '').toLowerCase() === color.toLowerCase() }"
            :style="{ backgroundColor: getColorHex(color) }"
            :title="`Chọn màu ${color}`"
            @click="selectLocalColor(color)"
          />
        </div>
        <span v-if="activeColor" class="active-color-name">
          {{ activeColor }}
        </span>
      </div>
      
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
  min-width: 0;
  height: 100%;
}

.the-san-pham-card:hover {
  transform: translateY(-4px);
  border-color: #d2a764;
  box-shadow: 0 10px 24px rgba(73, 104, 131, 0.12);
}

.card-img-holder {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 5;
  background: linear-gradient(145deg, #f8fafc, #eef2f6);
  overflow: hidden;
}

.card-img-holder img {
  width: 100%;
  height: 100%;
  object-fit: contain;
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

.best-seller-badge-gold {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #d2a764;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(210, 167, 100, 0.35);
  z-index: 2;
}

.card-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 4px;
}

.card-sold-badge {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
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
  margin: 0 0 6px;
  line-height: 1.4;
  height: 2.8em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* CHẤM MÀU TRÊN THẺ */
.card-color-swatches {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 2px 0;
}

.swatches-dots {
  display: flex;
  align-items: center;
  gap: 5px;
}

.color-dot {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.color-dot:hover {
  transform: scale(1.25);
}

.color-dot.active {
  transform: scale(1.3);
  box-shadow: 0 0 0 2px #ffffff, 0 0 0 3.5px #d2a764;
}

.active-color-name {
  font-size: 11.5px;
  color: #475569;
  font-weight: 600;
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
  max-width: 48%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.target-color-pill {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(30, 41, 59, 0.92);
  color: #fce7c8;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(4px);
  z-index: 2;
  border: 1px solid rgba(210, 167, 100, 0.4);
}

@media (hover: none) {
  .the-san-pham-card:hover {
    transform: none;
    box-shadow: none;
  }

  .the-san-pham-card:hover .card-img-holder img {
    transform: none;
  }

  .quick-hover-action {
    display: none;
  }
}

@media (max-width: 720px) {
  .the-san-pham-card {
    border-radius: 10px;
  }

  .card-caption {
    padding: 10px;
  }

  .discount-badge-red,
  .best-seller-badge-gold {
    top: 7px;
    font-size: 9px;
    padding: 3px 6px;
  }

  .discount-badge-red {
    left: 7px;
  }

  .best-seller-badge-gold {
    right: 7px;
  }

  .card-top-row {
    margin-bottom: 3px;
  }

  .card-cat-badge,
  .card-sold-badge {
    font-size: 9px;
  }

  .card-cat-badge {
    max-width: 58%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-sold-badge {
    white-space: nowrap;
  }

  .product-name {
    font-size: 12px;
    line-height: 1.35;
    height: 2.7em;
    margin-bottom: 5px;
  }

  .card-color-swatches {
    gap: 5px;
    margin-bottom: 6px;
    overflow: hidden;
  }

  .swatches-dots {
    gap: 4px;
  }

  .color-dot {
    width: 12px;
    height: 12px;
    flex: 0 0 12px;
  }

  .active-color-name {
    display: none;
  }

  .price-container {
    gap: 5px;
    margin-bottom: 6px;
    flex-wrap: wrap;
  }

  .price-red,
  .price-normal {
    font-size: 13px;
  }

  .price-struck {
    font-size: 10px;
  }

  .card-footer-tags {
    padding-top: 6px;
    font-size: 9px;
  }

  .material-tag {
    max-width: 52%;
  }

  .target-color-pill {
    left: 7px;
    bottom: 7px;
    max-width: calc(100% - 14px);
    padding: 3px 6px;
    font-size: 9px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 390px) {
  .card-caption {
    padding: 8px;
  }

  .card-sold-badge,
  .material-tag {
    display: none;
  }

  .card-cat-badge {
    max-width: 100%;
  }
}
</style>
