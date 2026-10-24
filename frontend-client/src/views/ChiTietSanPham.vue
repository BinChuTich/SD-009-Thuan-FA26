<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { addToCart, setCheckoutItems, cartItems } from '@/utils/luuTruGioHang.js'
import { getProductById, MOCK_PRODUCTS_CATALOG } from '@/utils/danhMucSanPham.js'
import ThuVienHinhAnh from '@/components/ChiTietSanPham/ThuVienHinhAnh.vue'
import ThongTinMoTa from '@/components/ChiTietSanPham/ThongTinMoTa.vue'
import SanPhamLienQuan from '@/components/ChiTietSanPham/SanPhamLienQuan.vue'

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

    if (!prodData) {
      prodData = getProductById(id)
      if (prodData && prodData.variants) {
        varData = prodData.variants
      }
    }

    product.value = prodData
    variants.value = varData

    try {
      const resActive = await api.get('/api/dot-giam-gia/active-products-map')
      if (resActive.data && resActive.data[id]) {
        const dot = resActive.data[id]
        if (prodData) {
          prodData.coGiamGia = true
          prodData.tenDotGiamGia = dot.tenDotGiamGia
          prodData.phanTramGiam = dot.phanTramGiam
          prodData.ngayKetThucDotGiamGia = dot.ngayKetThuc
        }
      }
    } catch (e) {}

    // Chuẩn bị ảnh
    const imgs = []
    if (prodData?.anhDaiDien) imgs.push(prodData.anhDaiDien)
    if (Array.isArray(prodData?.hinhAnhs)) {
      prodData.hinhAnhs.forEach(img => {
        if (!imgs.includes(img)) imgs.push(img)
      })
    }
    if (varData && varData.length > 0) {
      varData.forEach(v => {
        if (v.anhDaiDien && !imgs.includes(v.anhDaiDien)) {
          imgs.push(v.anhDaiDien)
        }
      })
    }
    if (imgs.length === 0) {
      imgs.push('/images/products/default_tshirt.jpg')
    }
    allImages.value = imgs
    activeImgIndex.value = 0

    // Mặc định chọn biến thể đầu tiên còn hàng
    if (varData.length > 0) {
      const firstAvailable = varData.find(v => (v.soLuongTon || v.soLuong || 0) > 0) || varData[0]
      selectedColorId.value = firstAvailable.idMauSac
      selectedSizeId.value = firstAvailable.idKichCo
    }

    // Tải sản phẩm liên quan
    try {
      const resRelated = await api.get('/api/san-pham', {
        params: { page: 0, size: 6, trangThai: 1 }
      })
      const relatedList = resRelated.data?.content || (Array.isArray(resRelated.data) ? resRelated.data : [])
      relatedProducts.value = relatedList.filter(p => p.id != id)
    } catch (e) {
      relatedProducts.value = MOCK_PRODUCTS_CATALOG.filter(p => p.id != id).slice(0, 4)
    }
  } catch (err) {
    console.error('Lỗi khởi tạo chi tiết sản phẩm:', err)
  } finally {
    loading.value = false
  }
}

// Biến thể hiện tại
const currentVariant = computed(() => {
  if (!variants.value.length) return null
  return variants.value.find(
    v => v.idMauSac === selectedColorId.value && v.idKichCo === selectedSizeId.value
  ) || null
})

// Số lượng tồn
const currentStock = computed(() => {
  if (!currentVariant.value) return 0
  return currentVariant.value.soLuongTon != null
    ? currentVariant.value.soLuongTon
    : (currentVariant.value.soLuong || 0)
})

// Giá bán
const currentPrice = computed(() => {
  if (currentVariant.value) {
    if (product.value?.coGiamGia && currentVariant.value.giaSauGiam) {
      return currentVariant.value.giaSauGiam
    }
    return currentVariant.value.giaBan || currentVariant.value.gia || 0
  }
  return product.value?.minGiaSauGiam || product.value?.minGia || product.value?.giaBan || 0
})

const originalPrice = computed(() => {
  if (currentVariant.value) {
    return currentVariant.value.giaBan || currentVariant.value.gia || 0
  }
  return product.value?.minGia || product.value?.giaBan || 0
})

const hasDiscount = computed(() => {
  return product.value?.coGiamGia === true || (originalPrice.value > currentPrice.value)
})


const campaignRemainingTime = ref('')
let campaignTimer = null

const updateCampaignTimer = () => {
  const dateStr = product.value?.ngayKetThucDotGiamGia
  if (!dateStr) {
    campaignRemainingTime.value = ''
    return
  }
  const target = new Date(dateStr).getTime()
  const diff = target - Date.now()
  if (diff <= 0) {
    campaignRemainingTime.value = 'Đã kết thúc'
    return
  }
  const d = Math.floor(diff / (1000 * 60 * 60 * 24))
  const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)

  if (d > 0) {
    campaignRemainingTime.value = `${d} ngày ${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  } else {
    campaignRemainingTime.value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
}

const discountPercent = computed(() => {
  if (product.value?.phanTramGiam) return Math.round(product.value.phanTramGiam)
  if (originalPrice.value > currentPrice.value) {
    return Math.round(((originalPrice.value - currentPrice.value) / originalPrice.value) * 100)
  }
  return 0
})

// Danh sách màu sắc và kích cỡ có sẵn
const availableColors = computed(() => {
  const map = new Map()
  variants.value.forEach(v => {
    if (v.idMauSac && !map.has(v.idMauSac)) {
      map.set(v.idMauSac, {
        id: v.idMauSac,
        name: v.tenMauSac || 'Màu tiêu chuẩn',
        code: v.maMau || '#496883'
      })
    }
  })
  return Array.from(map.values())
})

const availableSizes = computed(() => {
  const map = new Map()
  variants.value.forEach(v => {
    if (v.idKichCo && !map.has(v.idKichCo)) {
      const matchVariant = variants.value.find(
        item => item.idKichCo === v.idKichCo && item.idMauSac === selectedColorId.value
      )
      const stock = matchVariant ? (matchVariant.soLuongTon ?? matchVariant.soLuong ?? 0) : 0
      map.set(v.idKichCo, {
        id: v.idKichCo,
        name: v.tenKichCo || 'Freesize',
        stock
      })
    }
  })
  return Array.from(map.values())
})

watch(currentStock, (newStock) => {
  if (newStock <= 0) {
    quantity.value = 0
  } else if (quantity.value <= 0) {
    quantity.value = 1
  } else if (quantity.value > newStock) {
    quantity.value = newStock
  }
}, { immediate: true })

const increaseQty = () => {
  if (currentStock.value <= 0) return
  if (quantity.value < currentStock.value) {
    quantity.value++
  }
}

const decreaseQty = () => {
  if (currentStock.value <= 0) {
    quantity.value = 0
    return
  }
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
  const itemToBuy = cartItems.value.find(i => i.variantId === currentVariant.value.id)
  if (itemToBuy) {
    setCheckoutItems([{
      ...itemToBuy,
      quantity: quantity.value
    }])
  }
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
    <!-- Toast Popup thông báo -->
    <div v-if="showToast" class="toast-popup">
      <span class="t-check">✓</span>
      <span>{{ toastMessage }}</span>
    </div>

    <div class="container-client page-inner">
      <div class="top-back-row">
        <button class="btn-back-catalog" @click="router.push('/cua-hang')">
          &larr; Quay lại danh sách sản phẩm
        </button>
      </div>

      <div v-if="loading" class="detail-loading-box">
        <div class="spinner"></div>
        <p>Đang tải thông tin chi tiết sản phẩm...</p>
      </div>

      <div v-else-if="product" class="product-main-view">
        <div class="detail-grid-layout">
          <!-- 1. CỘT TRÁI: KHUNG ẢNH (COMPONENT THƯ VIỆN HÌNH ẢNH) -->
          <ThuVienHinhAnh
            :images="allImages"
            v-model:active-index="activeImgIndex"
            :product-name="product.tenSanPham"
          />

          <!-- 2. CỘT PHẢI: THÔNG TIN SẢN PHẨM & CHỌN BIẾN THỂ -->
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
              <span class="rating-count">(18 đánh giá hài lòng)</span>
            </div>

            <!-- Banner Đợt Giảm Giá Đang Hoạt Động -->
            <div class="flash-deal-banner" v-if="product?.coGiamGia && product?.ngayKetThucDotGiamGia && campaignRemainingTime">
              <div class="flash-left">
                <span class="flash-fire">🔥</span>
                <span class="flash-camp-name">{{ product.tenDotGiamGia || 'ƯU ĐÃI ĐẶC BIỆT' }}</span>
                <span class="flash-tag" v-if="discountPercent > 0">-{{ discountPercent }}%</span>
              </div>
              <div class="flash-right">
                <span class="flash-clock">⏰</span>
                <span>Kết thúc trong: <strong>{{ campaignRemainingTime }}</strong></span>
              </div>
            </div>

            <div class="pricing-headline">
              <span class="main-red-price">{{ formatCurrency(currentPrice) }}</span>
              <template v-if="hasDiscount && originalPrice > currentPrice">
                <span class="struck-price">{{ formatCurrency(originalPrice) }}</span>
                <span v-if="discountPercent > 0" class="discount-tag-box">-{{ discountPercent }}%</span>
              </template>
            </div>

            <!-- Khối tùy chọn biến thể -->
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
                <div class="quantity-stepper-box" :class="{ 'stepper-disabled': currentStock <= 0 }">
                  <button class="step-btn" :disabled="currentStock <= 0 || quantity <= 1" @click="decreaseQty">-</button>
                  <span class="qty-display">{{ currentStock <= 0 ? 0 : quantity }}</span>
                  <button class="step-btn" :disabled="currentStock <= 0 || quantity >= currentStock" @click="increaseQty">+</button>
                </div>
                <span class="stock-helper-text" :class="{ 'out-of-stock-text': currentStock <= 0 }">
                  {{ !currentVariant ? 'Vui lòng chọn màu và size' : (currentStock > 0 ? `Còn ${currentStock} sản phẩm` : 'Đã hết hàng') }}
                </span>
              </div>
            </div>

            <!-- 2 Nút thao tác mang màu #496883 & #d2a764 -->
            <div class="action-buttons-pair">
              <button class="btn-add-to-cart" :disabled="currentStock <= 0" @click="handleAddToCart">
                {{ currentStock <= 0 ? 'Tạm hết hàng' : 'Thêm vào giỏ hàng' }}
              </button>
              <button class="btn-buy-now-solid" :disabled="currentStock <= 0" @click="handleBuyNow">
                {{ currentStock <= 0 ? 'Hết hàng' : 'Mua ngay' }}
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
                <span>Đổi trả dễ dàng trong 7 ngày</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 3. TABS MÔ TẢ CHI TIẾT (COMPONENT THÔNG TIN MÔ TẢ) -->
        <ThongTinMoTa :product="product" />

        <!-- 4. SẢN PHẨM LIÊN QUAN (COMPONENT SẢN PHẨM LIÊN QUAN) -->
        <SanPhamLienQuan :related-products="relatedProducts" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-detail-page {
  background-color: #f8fafc;
  min-height: 100vh;
  padding: 30px 0 80px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.top-back-row {
  margin-bottom: 24px;
}

.btn-back-catalog {
  background: transparent;
  border: none;
  color: #496883;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}

.btn-back-catalog:hover {
  color: #d2a764;
}

/* Detail Grid Layout */
.detail-grid-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 50px;
  background: #ffffff;
  padding: 40px;
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 6px 24px rgba(73, 104, 131, 0.06);
}

.product-meta-col {
  display: flex;
  flex-direction: column;
}

.meta-product-title {
  font-size: 26px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 10px;
  line-height: 1.35;
}

.sku-brand-line {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
}

.sku-brand-line .sep {
  margin: 0 8px;
  color: #cbd5e1;
}

.rating-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 18px;
}

.rating-num {
  font-weight: 800;
  color: #d2a764;
}

.stars-stars {
  color: #d2a764;
  font-size: 15px;
}

.rating-count {
  font-size: 12px;
  color: #94a3b8;
}


.flash-deal-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #fef2f2 0%, #fff7ed 100%);
  border: 1px solid #fed7aa;
  border-radius: 10px;
  padding: 10px 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}
.flash-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.flash-camp-name {
  font-size: 13px;
  font-weight: 800;
  color: #991b1b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.flash-tag {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 4px;
}
.flash-right {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9a3412;
}
.flash-right strong {
  font-variant-numeric: tabular-nums;
  color: #c2410c;
  font-weight: 800;
}

.pricing-headline {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 24px;
}

.main-red-price {
  font-size: 28px;
  font-weight: 900;
  color: #e11d48;
}

.struck-price {
  font-size: 16px;
  color: #94a3b8;
  text-decoration: line-through;
}

.discount-tag-box {
  background: #fee2e2;
  color: #e11d48;
  font-weight: 800;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
}

/* Options grey box */
.options-grey-box {
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.option-row {
  margin-bottom: 16px;
}

.option-row:last-child {
  margin-bottom: 0;
}

.option-row.align-center {
  display: flex;
  align-items: center;
  gap: 16px;
}

.option-label {
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.pill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-pill {
  padding: 7px 16px;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}

.option-pill:hover:not(:disabled) {
  border-color: #d2a764;
  color: #1e293b;
}

.option-pill.selected {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.option-pill.out-stock {
  opacity: 0.4;
  text-decoration: line-through;
  cursor: not-allowed;
}

.quantity-stepper-box {
  display: inline-flex;
  align-items: center;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  overflow: hidden;
}

.step-btn {
  width: 36px;
  height: 36px;
  background: #f1f5f9;
  border: none;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  color: #1e293b;
  transition: background 0.2s;
}

.step-btn:hover:not(:disabled) {
  background: #e2e8f0;
}

.step-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.qty-display {
  width: 44px;
  text-align: center;
  font-weight: 800;
  font-size: 14px;
}

.stock-helper-text {
  font-size: 13px;
  color: #64748b;
}

/* Action buttons */
.action-buttons-pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.btn-add-to-cart {
  padding: 14px 0;
  background: #ffffff;
  border: 2px solid #496883;
  color: #496883;
  font-size: 15px;
  font-weight: 800;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-to-cart:hover {
  background: #eaf1f4;
}

.btn-buy-now-solid {
  padding: 14px 0;
  background: #d2a764;
  border: none;
  color: #1e293b;
  font-size: 15px;
  font-weight: 800;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.4);
}

.btn-buy-now-solid:hover {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.5);
}

.btn-add-to-cart:disabled,
.btn-buy-now-solid:disabled {
  opacity: 0.5 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
  transform: none !important;
  box-shadow: none !important;
}

.quantity-stepper-box.stepper-disabled {
  opacity: 0.6;
  background: #f1f5f9;
}

.stock-helper-text.out-of-stock-text {
  color: #ef4444;
  font-weight: 700;
}

.guarantee-perks-row {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #64748b;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

.perk-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.perk-divider {
  color: #cbd5e1;
}

/* Toast popup */
.toast-popup {
  position: fixed;
  top: 85px;
  right: 25px;
  background: #1e293b;
  color: #ffffff;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  z-index: 9999;
  border-left: 4px solid #10b981;
}

.t-check {
  color: #10b981;
  font-weight: 900;
}

.detail-loading-box {
  background: #ffffff;
  padding: 80px 20px;
  text-align: center;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

.spinner {
  width: 44px;
  height: 44px;
  border: 3px solid #e2e8f0;
  border-top-color: #d2a764;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .detail-grid-layout {
    grid-template-columns: 1fr;
    gap: 30px;
    padding: 24px;
  }
}

@media (max-width: 580px) {
  .action-buttons-pair {
    grid-template-columns: 1fr;
  }
}
</style>
