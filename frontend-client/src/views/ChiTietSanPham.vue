<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/format'
import { addToCart } from '@/utils/cartStorage'
import { getProductById, MOCK_PRODUCTS_CATALOG } from '@/utils/productsCatalog.js'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const product = ref(null)
const variants = ref([])
const relatedProducts = ref([])

// Lựa chọn biến thể
const selectedColorId = ref(null)
const selectedSizeId = ref(null)
const quantity = ref(1)
const activeImgIndex = ref(0)
const allImages = ref([])
const activeTab = ref('desc')

// Toast notification
const toastMessage = ref('')
const showToast = ref(false)

const triggerToast = (msg) => {
  toastMessage.value = msg
  showToast.value = true
  setTimeout(() => {
    showToast.value = false
  }, 2200)
}

const loadProductData = async (id) => {
  loading.value = true
  try {
    let prodData = null
    let varData = []
    try {
      const [resProd, resVar] = await Promise.all([
        api.get(`/api/san-pham/${id}`),
        api.get(`/api/chi-tiet-san-pham/by-san-pham/${id}`)
      ])
      prodData = resProd.data
      varData = resVar.data || []
    } catch (e) {
      console.warn('Lỗi tải sản phẩm từ API:', e)
    }

    if (!prodData || prodData.trangThai === 0) {
      product.value = null
      variants.value = []
      return
    }

    product.value = prodData
    variants.value = varData

    const imgs = []
    if (product.value?.anhDaiDien) imgs.push(product.value.anhDaiDien)
    if (product.value?.hinhAnhs && Array.isArray(product.value.hinhAnhs)) {
      imgs.push(...product.value.hinhAnhs)
    }
    variants.value.forEach(v => {
      if (v.anhDaiDien && !imgs.includes(v.anhDaiDien)) {
        imgs.push(v.anhDaiDien)
      }
    })
    if (imgs.length === 0) {
      imgs.push('/images/products/default_tshirt.jpg')
    }
    allImages.value = imgs
    activeImgIndex.value = 0

    if (variants.value.length > 0) {
      const inStock = variants.value.find(v => (v.soLuong || 0) > 0) || variants.value[0]
      selectedColorId.value = inStock.idMauSac
      selectedSizeId.value = inStock.idKichCo
    }

    loadRelated()
  } catch (err) {
    console.error('Lỗi tải chi tiết sản phẩm:', err)
  } finally {
    loading.value = false
  }
}

const loadRelated = async () => {
  try {
    const res = await api.get('/api/san-pham', { params: { page: 0, size: 8 } })
    const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    relatedProducts.value = list.filter(p => p && p.tenSanPham && p.trangThai === 1 && p.id !== product.value?.id).slice(0, 4)
  } catch (e) {
    relatedProducts.value = []
  }
}

const prevImage = () => {
  if (activeImgIndex.value > 0) {
    activeImgIndex.value--
  } else {
    activeImgIndex.value = allImages.value.length - 1
  }
}

const nextImage = () => {
  if (activeImgIndex.value < allImages.value.length - 1) {
    activeImgIndex.value++
  } else {
    activeImgIndex.value = 0
  }
}

const availableColors = computed(() => {
  const map = new Map()
  variants.value.forEach(v => {
    if (v.idMauSac && !map.has(v.idMauSac)) {
      map.set(v.idMauSac, {
        id: v.idMauSac,
        name: v.tenMauSac || 'Tiêu chuẩn',
        hex: v.maHex || '#496883'
      })
    }
  })
  return Array.from(map.values())
})

const availableSizes = computed(() => {
  const map = new Map()
  variants.value.forEach(v => {
    if (v.idKichCo && !map.has(v.idKichCo)) {
      const matched = variants.value.find(item => item.idKichCo === v.idKichCo && item.idMauSac === selectedColorId.value)
      const stock = matched ? (matched.soLuong || 0) : 0
      map.set(v.idKichCo, {
        id: v.idKichCo,
        name: v.tenKichCo || 'Freesize',
        stock: stock
      })
    }
  })
  return Array.from(map.values())
})

const currentVariant = computed(() => {
  if (!variants.value.length) return null
  return variants.value.find(v => v.idMauSac === selectedColorId.value && v.idKichCo === selectedSizeId.value) || null
})

const currentStock = computed(() => {
  return currentVariant.value ? (currentVariant.value.soLuong || 0) : 0
})

const hasDiscount = computed(() => {
  if (currentVariant.value) {
    return !!(currentVariant.value.coGiamGia && currentVariant.value.giaSauGiam && Number(currentVariant.value.giaSauGiam) < Number(currentVariant.value.giaBan))
  }
  return !!(product.value?.coGiamGia && product.value?.minGiaSauGiam && Number(product.value.minGiaSauGiam) < Number(product.value.minGia))
})

const discountPercent = computed(() => {
  if (currentVariant.value && currentVariant.value.phanTramGiam) {
    return Math.round(currentVariant.value.phanTramGiam)
  }
  if (product.value?.phanTramGiam) {
    return Math.round(product.value.phanTramGiam)
  }
  if (hasDiscount.value && originalPrice.value > 0) {
    return Math.round((1 - currentPrice.value / originalPrice.value) * 100)
  }
  return 0
})

const currentPrice = computed(() => {
  if (currentVariant.value) {
    if (currentVariant.value.coGiamGia && currentVariant.value.giaSauGiam) {
      return Number(currentVariant.value.giaSauGiam)
    }
    return Number(currentVariant.value.giaBan || 0)
  }
  if (product.value?.coGiamGia && product.value?.minGiaSauGiam) {
    return Number(product.value.minGiaSauGiam)
  }
  return Number(product.value?.minGia || 0)
})

const originalPrice = computed(() => {
  if (currentVariant.value) {
    return Number(currentVariant.value.giaBan || 0)
  }
  return Number(product.value?.minGia || 0)
})

const increaseQty = () => {
  if (quantity.value < (currentStock.value || 99)) {
    quantity.value++
  }
}

const decreaseQty = () => {
  if (quantity.value > 1) {
    quantity.value--
  }
}

const handleAddToCart = () => {
  if (!currentVariant.value) {
    triggerToast('Vui lòng chọn màu sắc và kích cỡ!')
    return
  }
  if (currentStock.value <= 0) {
    triggerToast('Biến thể này hiện đã hết hàng trong kho!')
    return
  }

  const ok = addToCart(product.value, currentVariant.value, quantity.value)
  if (ok) {
    triggerToast(`Đã thêm ${quantity.value} sản phẩm vào giỏ hàng!`)
  }
}

const handleBuyNow = () => {
  if (!currentVariant.value) {
    triggerToast('Vui lòng chọn màu sắc và kích cỡ!')
    return
  }
  if (currentStock.value <= 0) {
    triggerToast('Biến thể này hiện đã hết hàng trong kho!')
    return
  }

  addToCart(product.value, currentVariant.value, quantity.value)
  router.push('/thanh-toan')
}

watch(selectedColorId, (newColorId) => {
  const match = variants.value.find(v => v.idMauSac === newColorId && v.anhDaiDien)
  if (match && match.anhDaiDien) {
    const idx = allImages.value.indexOf(match.anhDaiDien)
    if (idx !== -1) activeImgIndex.value = idx
  }
})

watch(() => route.params.id, (newId) => {
  if (newId) {
    loadProductData(newId)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})

onMounted(() => {
  if (route.params.id) {
    loadProductData(route.params.id)
  }
})
</script>

<template>
  <div class="product-detail-page">
    <div v-if="showToast" class="toast-popup">
      <span class="t-check">✓</span>
      <span>{{ toastMessage }}</span>
    </div>

    <div class="container-client page-inner">
      <div class="top-back-row">
        <button class="btn-back-catalog" @click="router.push('/cua-hang')">
          &larr; Quay lại danh sách
        </button>
      </div>

      <div v-if="loading" class="detail-loading-box">
        <div class="spinner"></div>
        <p>Đang tải thông tin chi tiết sản phẩm...</p>
      </div>

      <div v-else-if="product" class="product-main-view">
        <div class="detail-grid-layout">
          <!-- CỘT TRÁI: KHUNG ẢNH VỚI MŨI TÊN CHUYỂN SLIDE (MATCH frame_detail_1.jpg) -->
          <div class="image-showcase-col">
            <div class="main-slider-frame">
              <img
                :src="allImages[activeImgIndex]"
                :alt="product.tenSanPham"
                class="current-slide-img"
              />

              <button class="slider-arrow arrow-left" @click="prevImage" title="Ảnh trước">
                ‹
              </button>
              <button class="slider-arrow arrow-right" @click="nextImage" title="Ảnh kế">
                ›
              </button>
            </div>

            <div v-if="allImages.length > 1" class="thumb-strip">
              <div
                v-for="(img, idx) in allImages"
                :key="idx"
                class="thumb-tile"
                :class="{ active: activeImgIndex === idx }"
                @click="activeImgIndex = idx"
              >
                <img :src="img" :alt="'Thumbnail ' + idx" />
              </div>
            </div>
          </div>

          <!-- CỘT PHẢI: THÔNG TIN SẢN PHẨM & LỰA CHỌN BIẾN THỂ -->
          <div class="product-meta-col">
            <h1 class="meta-product-title">{{ product.tenSanPham }}</h1>

            <div class="sku-brand-line">
              <span>Mã SKU: {{ currentVariant?.maSku || 'Vui lòng chọn size/màu' }}</span>
              <span class="sep">|</span>
              <span>Thương hiệu: <strong>{{ product.tenThuongHieu || 'FF T-Shirt' }}</strong></span>
            </div>

            <div class="rating-line">
              <span class="rating-num">5.0</span>
              <div class="stars-stars">★★★★★</div>
              <span class="rating-count">(18 đánh giá)</span>
            </div>

            <div class="pricing-headline">
              <span class="main-red-price">{{ formatCurrency(currentPrice) }}</span>
              <template v-if="hasDiscount && originalPrice > currentPrice">
                <span class="struck-price">{{ formatCurrency(originalPrice) }}</span>
                <span v-if="discountPercent > 0" class="discount-tag-box">-{{ discountPercent }}%</span>
              </template>
            </div>

            <!-- Khối tùy chọn biến thể (MATCH frame_detail_1.jpg) -->
            <div class="options-grey-box">
              <div class="option-row">
                <label class="option-label">Màu Sắc</label>
                <div class="pill-group">
                  <button
                    v-for="c in availableColors"
                    :key="c.id"
                    class="option-pill"
                    :class="{ selected: selectedColorId === c.id }"
                    @click="selectedColorId = c.id"
                  >
                    {{ c.name }}
                  </button>
                </div>
              </div>

              <div class="option-row">
                <label class="option-label">Kích Cỡ</label>
                <div class="pill-group">
                  <button
                    v-for="s in availableSizes"
                    :key="s.id"
                    class="option-pill"
                    :class="{
                      selected: selectedSizeId === s.id,
                      'out-stock': s.stock <= 0
                    }"
                    :disabled="s.stock <= 0"
                    @click="selectedSizeId = s.id"
                  >
                    {{ s.name }}
                  </button>
                </div>
              </div>

              <div class="option-row align-center">
                <label class="option-label">Số Lượng</label>
                <div class="quantity-stepper-box">
                  <button class="step-btn" :disabled="quantity <= 1" @click="decreaseQty">-</button>
                  <span class="qty-display">{{ quantity }}</span>
                  <button class="step-btn" :disabled="quantity >= (currentStock || 99)" @click="increaseQty">+</button>
                </div>
                <span class="stock-helper-text">
                  {{ currentVariant ? `Còn ${currentStock} sản phẩm` : 'Vui lòng chọn màu và size' }}
                </span>
              </div>
            </div>

            <!-- 2 Nút thao tác mang màu #496883 & #d2a764 -->
            <div class="action-buttons-pair">
              <button class="btn-add-to-cart" @click="handleAddToCart">
                Thêm vào giỏ hàng
              </button>
              <button class="btn-buy-now-solid" @click="handleBuyNow">
                Mua ngay
              </button>
            </div>

            <!-- Dòng cam kết -->
            <div class="guarantee-perks-row">
              <div class="perk-item">
                <span class="perk-icon">📞</span>
                <span>Hỗ trợ khách hàng 24/7</span>
              </div>
              <div class="perk-divider">|</div>
              <div class="perk-item">
                <span class="perk-icon">🛡️</span>
                <span>Bảo hiểm thời trang / Đổi trả dễ dàng</span>
              </div>
            </div>
          </div>
        </div>

        <!-- TABS MÔ TẢ CHI TIẾT SẢN PHẨM PHÍA DƯỚI -->
        <div class="product-tabs-section">
          <div class="tabs-header-bar">
            <button
              class="tab-title-btn"
              :class="{ active: activeTab === 'desc' }"
              @click="activeTab = 'desc'"
            >
              Mô tả chi tiết sản phẩm
            </button>
            <button
              class="tab-title-btn"
              :class="{ active: activeTab === 'specs' }"
              @click="activeTab = 'specs'"
            >
              Thông số kỹ thuật
            </button>
            <button
              class="tab-title-btn"
              :class="{ active: activeTab === 'policy' }"
              @click="activeTab = 'policy'"
            >
              Chính sách đổi trả &amp; bảo hành
            </button>
          </div>

          <div class="tab-body-pane">
            <div v-if="activeTab === 'desc'" class="pane-content">
              <h3>Đặc điểm &amp; Chất liệu</h3>
              <p>{{ product.moTa || 'Sản phẩm áo phông nam cao cấp FF T-Shirt được làm từ 100% sợi cotton tự nhiên định lượng 230gsm, đảm bảo độ đứng form, không bị xù lông hay bai dão sau nhiều lần giặt.' }}</p>
              <ul>
                <li>Khả năng thấm hút mồ hôi tối ưu, đem lại sự mát mẻ và khô thoáng suốt cả ngày dài.</li>
                <li>Đường may móc xích tỉ mỉ theo tiêu chuẩn xuất khẩu.</li>
                <li>Thiết kế tối giản dễ dàng phối cùng quần short, quần jeans hoặc quần âu thanh lịch.</li>
              </ul>
            </div>

            <div v-if="activeTab === 'specs'" class="pane-content">
              <table class="specs-table-simple">
                <tbody>
                  <tr><td>Chất liệu</td><td>{{ product.tenChatLieu || '100% Cotton 2 chiều' }}</td></tr>
                  <tr><td>Kiểu dáng cổ</td><td>{{ product.tenCoAo || 'Cổ tròn co giãn' }}</td></tr>
                  <tr><td>Tay áo</td><td>{{ product.tenTayAo || 'Ngắn tay' }}</td></tr>
                  <tr><td>Họa tiết</td><td>{{ product.tenHoaTiet || 'Trơn / Tối giản' }}</td></tr>
                  <tr><td>Thương hiệu</td><td>{{ product.tenThuongHieu || 'FF T-Shirt' }}</td></tr>
                  <tr><td>Xuất xứ</td><td>{{ product.tenXuatXu || 'Việt Nam' }}</td></tr>
                </tbody>
              </table>
            </div>

            <div v-if="activeTab === 'policy'" class="pane-content">
              <h3>Chính sách chăm sóc khách hàng đặc quyền</h3>
              <p>FF T-Shirt cam kết quyền lợi tối đa cho mọi đơn hàng online:</p>
              <ul>
                <li>Đổi trả miễn phí trong vòng <strong>7 ngày</strong> kể từ khi nhận hàng.</li>
                <li>Shipper hỗ trợ đổi size tận nhà, bạn không cần phải ra bưu cục gửi hàng.</li>
                <li>Kiểm tra hàng trước khi thanh toán tiền mặt (COD).</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- SẢN PHẨM CÙNG BỘ SƯU TẬP -->
        <div v-if="relatedProducts.length > 0" class="related-products-box">
          <h2 class="related-title">Sản Phẩm Cùng Bộ Sưu Tập</h2>
          <div class="related-grid-3">
            <div
              v-for="rp in relatedProducts"
              :key="rp.id"
              class="rel-card"
              @click="router.push(`/san-pham/${rp.id}`)"
            >
              <div class="rel-img">
                <img :src="rp.anhDaiDien || '/images/products/default_tshirt.jpg'" :alt="rp.tenSanPham" />
              </div>
              <div class="rel-caption">
                <h4>{{ rp.tenSanPham }}</h4>
                <span class="rel-price">{{ formatCurrency(rp.minGiaSauGiam || rp.minGia || 0) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-product-box">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#94a3b8" stroke-width="1.8" style="margin-bottom: 12px;">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <h3 style="font-size: 1.25rem; font-weight: 600; color: #1e293b; margin-bottom: 8px;">Sản phẩm không khả dụng</h3>
        <p style="color: #64748b; font-size: 0.95rem; margin-bottom: 20px;">Sản phẩm này hiện không tồn tại hoặc đã ngừng kinh doanh trên hệ thống.</p>
        <button class="btn-back-catalog" @click="router.push('/cua-hang')">Khám phá sản phẩm khác &rarr;</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-detail-page {
  background-color: #f8fafc;
  min-height: 85vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.25rem;
}

.page-inner {
  padding-top: 20px;
}

.toast-popup {
  position: fixed;
  top: 80px;
  right: 24px;
  background: #496883;
  color: #ffffff;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 25px rgba(73, 104, 131, 0.25);
  z-index: 99999;
}

.t-check {
  background: #16a34a;
  color: #fff;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.top-back-row {
  margin-bottom: 20px;
}

.btn-back-catalog {
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 700;
  color: #496883;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}

.btn-back-catalog:hover {
  color: #d2a764;
}

/* Grid layout */
.detail-grid-layout {
  display: grid;
  grid-template-columns: 1fr 1.05fr;
  gap: 40px;
  align-items: flex-start;
  background: #ffffff;
  padding: 32px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

/* Cột trái: Ảnh */
.image-showcase-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.main-slider-frame {
  position: relative;
  width: 100%;
  padding-top: 120%;
  background: #f8fafc;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.current-slide-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slider-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e2e8f0;
  color: #496883;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.slider-arrow:hover {
  background: #ffffff;
  color: #d2a764;
  transform: translateY(-50%) scale(1.08);
}

.arrow-left { left: 12px; }
.arrow-right { right: 12px; }

.thumb-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
}

.thumb-tile {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  flex-shrink: 0;
}

.thumb-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-tile.active {
  border-color: #496883;
}

/* Cột phải: Thông tin */
.product-meta-col {
  display: flex;
  flex-direction: column;
}

.meta-product-title {
  font-size: 26px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 10px;
  line-height: 1.3;
}

.sku-brand-line {
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.sku-brand-line .sep {
  color: #cbd5e1;
}

.sku-brand-line strong {
  color: #496883;
}

.rating-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-bottom: 16px;
}

.rating-num {
  font-weight: 700;
  color: #1e293b;
}

.stars-stars {
  color: #f59e0b;
  letter-spacing: 2px;
}

.rating-count {
  color: #94a3b8;
}

.pricing-headline {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.main-red-price {
  color: #ef4444;
  font-size: 26px;
  font-weight: 800;
}

.struck-price {
  color: #94a3b8;
  font-size: 16px;
  text-decoration: line-through;
}

.discount-tag-box {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 4px;
}

/* Khối tùy chọn xám nhạt */
.options-grey-box {
  background-color: #f8fafc;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 1px solid #e2e8f0;
}

.option-row {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.option-row.align-center {
  align-items: center;
}

.option-label {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  width: 70px;
  flex-shrink: 0;
  padding-top: 4px;
}

.option-row.align-center .option-label {
  padding-top: 0;
}

.pill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-pill {
  padding: 6px 16px;
  border-radius: 4px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.option-pill:hover {
  border-color: #496883;
}

.option-pill.selected {
  border-color: #496883;
  background: #496883;
  color: #ffffff;
}

.option-pill.out-stock {
  opacity: 0.35;
  cursor: not-allowed;
  text-decoration: line-through;
}

.quantity-stepper-box {
  display: flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  background: #ffffff;
  overflow: hidden;
}

.step-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #496883;
}

.step-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.qty-display {
  width: 36px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
}

.stock-helper-text {
  font-size: 12px;
  color: #64748b;
}

/* 2 Action buttons */
.action-buttons-pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 22px;
}

.btn-add-to-cart {
  background: #ffffff;
  color: #496883;
  border: 1.5px solid #496883;
  padding: 12px 0;
  border-radius: 6px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-to-cart:hover {
  background: #f1f5f9;
}

.btn-buy-now-solid {
  background: #496883;
  color: #ffffff;
  border: none;
  padding: 12px 0;
  border-radius: 6px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.btn-buy-now-solid:hover {
  background: #364f66;
}

/* Cam kết */
.guarantee-perks-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
  font-size: 12px;
  color: #64748b;
}

.perk-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.perk-icon {
  font-size: 16px;
}

.perk-divider {
  color: #cbd5e1;
}

/* Tabs */
.product-tabs-section {
  margin-top: 36px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.tabs-header-bar {
  display: flex;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  overflow-x: auto;
}

.tab-title-btn {
  padding: 14px 24px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  border-bottom: 2.5px solid transparent;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-title-btn.active {
  color: #496883;
  border-bottom-color: #d2a764;
  background: #ffffff;
  font-weight: 700;
}

.tab-body-pane {
  padding: 28px;
}

.pane-content h3 {
  margin: 0 0 12px;
  font-size: 17px;
  color: #1e293b;
}

.pane-content p, .pane-content ul {
  font-size: 13.5px;
  line-height: 1.7;
  color: #475569;
}

.specs-table-simple {
  width: 100%;
  max-width: 600px;
  border-collapse: collapse;
}

.specs-table-simple td {
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.specs-table-simple td:first-child {
  font-weight: 600;
  color: #64748b;
  width: 160px;
}

/* Related */
.related-products-box {
  margin-top: 48px;
}

.related-title {
  font-size: 22px;
  font-weight: 800;
  color: #496883;
  margin-bottom: 20px;
  text-align: center;
}

.related-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.rel-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s;
}

.rel-card:hover {
  transform: translateY(-4px);
  border-color: #d2a764;
}

.rel-img {
  width: 100%;
  padding-top: 120%;
  position: relative;
  background: #f8fafc;
}

.rel-img img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rel-caption {
  padding: 12px;
}

.rel-caption h4 {
  margin: 0 0 6px;
  font-size: 13px;
  color: #1e293b;
}

.rel-price {
  color: #ef4444;
  font-weight: 700;
  font-size: 14px;
}

/* RESPONSIVE CHO ĐIỆN THOẠI & TABLET */
@media (max-width: 900px) {
  .detail-grid-layout {
    grid-template-columns: 1fr;
    padding: 20px;
    gap: 24px;
  }
  .related-grid-3 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .action-buttons-pair {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .meta-product-title {
    font-size: 20px;
  }
  .main-red-price {
    font-size: 22px;
  }
  .guarantee-perks-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .perk-divider {
    display: none;
  }
}
</style>
