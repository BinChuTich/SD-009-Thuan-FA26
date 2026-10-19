<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/format'
import {
  cartItems,
  cartSubtotal,
  updateCartQuantity,
  removeFromCart,
  clearCart,
  addToCart
} from '@/utils/cartStorage'
import { MOCK_PRODUCTS_CATALOG } from '@/utils/productsCatalog.js'

const router = useRouter()

// Sản phẩm gợi ý khi giỏ hàng trống
const recommendedProducts = ref(MOCK_PRODUCTS_CATALOG.slice(0, 4))

const handleQuickAddToCart = (product) => {
  const variant = product.variants && product.variants.length > 0
    ? product.variants[0]
    : {
        id: product.id * 100,
        tenMauSac: 'Xanh Navy',
        tenKichCo: 'L',
        soLuong: 50,
        gia: product.minGia,
        giaSauGiam: product.minGiaSauGiam || product.minGia
      }
  addToCart(product, variant, 1)
  selectedItemIds.value = cartItems.value.map(i => i.variantId)
}

// Lựa chọn checkbox từng sản phẩm hoặc chọn tất cả
const selectedItemIds = ref([])

// Mặc định chọn tất cả khi mới vào
selectedItemIds.value = cartItems.value.map(i => i.variantId)

const isAllSelected = computed(() => {
  return cartItems.value.length > 0 && selectedItemIds.value.length === cartItems.value.length
})

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedItemIds.value = []
  } else {
    selectedItemIds.value = cartItems.value.map(i => i.variantId)
  }
}

// Tính toán theo các mặt hàng được tick chọn
const selectedCount = computed(() => {
  return cartItems.value
    .filter(i => selectedItemIds.value.includes(i.variantId))
    .reduce((sum, i) => sum + i.quantity, 0)
})

const selectedSubtotal = computed(() => {
  return cartItems.value
    .filter(i => selectedItemIds.value.includes(i.variantId))
    .reduce((sum, i) => {
      const pr = i.discountPrice > 0 ? i.discountPrice : i.price
      return sum + pr * i.quantity
    }, 0)
})

// Tiến trình Freeship (Ngưỡng 500.000đ)
const freeShipTarget = 500000
const freeShipPercent = computed(() => {
  if (selectedSubtotal.value >= freeShipTarget) return 100
  return Math.min(100, Math.round((selectedSubtotal.value / freeShipTarget) * 100))
})
const freeShipRemaining = computed(() => {
  return Math.max(0, freeShipTarget - selectedSubtotal.value)
})

// Voucher giảm giá
const voucherInput = ref('')
const appliedVoucher = ref(null)
const voucherMessage = ref({ type: '', text: '' })

const handleApplyVoucher = (codeToApply) => {
  const code = (codeToApply || voucherInput.value).trim().toUpperCase()
  if (!code) {
    voucherMessage.value = { type: 'error', text: 'Vui lòng nhập mã giảm giá!' }
    return
  }

  if (code === 'GIAM30K') {
    if (selectedSubtotal.value < 300000) {
      voucherMessage.value = { type: 'error', text: 'Mã GIAM30K áp dụng cho đơn từ 300.000đ' }
      return
    }
    appliedVoucher.value = { code: 'GIAM30K', discount: 30000, desc: 'Giảm 30.000đ' }
    voucherMessage.value = { type: 'success', text: 'Áp dụng mã GIAM30K thành công!' }
    voucherInput.value = ''
  } else if (code === 'GIAM50K') {
    if (selectedSubtotal.value < 500000) {
      voucherMessage.value = { type: 'error', text: 'Mã GIAM50K áp dụng cho đơn từ 500.000đ' }
      return
    }
    appliedVoucher.value = { code: 'GIAM50K', discount: 50000, desc: 'Giảm 50.000đ' }
    voucherMessage.value = { type: 'success', text: 'Áp dụng mã GIAM50K thành công!' }
    voucherInput.value = ''
  } else if (code === 'FREESHIP') {
    appliedVoucher.value = { code: 'FREESHIP', discount: 30000, desc: 'Miễn phí vận chuyển 30.000đ' }
    voucherMessage.value = { type: 'success', text: 'Áp dụng mã FREESHIP thành công!' }
    voucherInput.value = ''
  } else {
    voucherMessage.value = { type: 'error', text: 'Mã voucher không hợp lệ hoặc đã hết hạn!' }
  }
}

const handleRemoveVoucher = () => {
  appliedVoucher.value = null
  voucherMessage.value = { type: '', text: '' }
}

const voucherDiscountAmount = computed(() => {
  return appliedVoucher.value ? appliedVoucher.value.discount : 0
})

const shippingFee = computed(() => {
  if (selectedSubtotal.value === 0) return 0
  if (selectedSubtotal.value >= freeShipTarget) return 0
  if (appliedVoucher.value && appliedVoucher.value.code === 'FREESHIP') return 0
  return 30000
})

const finalTotal = computed(() => {
  if (selectedSubtotal.value === 0) return 0
  const sub = selectedSubtotal.value - voucherDiscountAmount.value + shippingFee.value
  return Math.max(0, sub)
})

const handleQtyChange = (variantId, delta, currQty, maxStock) => {
  const next = currQty + delta
  if (next >= 1 && next <= (maxStock || 99)) {
    updateCartQuantity(variantId, next)
  }
}

const handleRemove = (variantId, name) => {
  if (confirm(`Bạn có chắc muốn xóa "${name}" khỏi giỏ hàng?`)) {
    removeFromCart(variantId)
    selectedItemIds.value = selectedItemIds.value.filter(id => id !== variantId)
  }
}

const handleRemoveSelected = () => {
  if (selectedItemIds.value.length === 0) return
  if (confirm(`Bạn có chắc muốn xóa ${selectedItemIds.value.length} sản phẩm đã chọn?`)) {
    selectedItemIds.value.forEach(id => removeFromCart(id))
    selectedItemIds.value = []
  }
}

const proceedToCheckout = () => {
  if (selectedItemIds.value.length === 0) {
    alert('Vui lòng chọn ít nhất 1 sản phẩm để thanh toán!')
    return
  }
  router.push('/thanh-toan')
}
</script>

<template>
  <div class="cart-page-wrap">
    <div class="container-client cart-body-container">
      <!-- TIÊU ĐỀ TRANG KHI CÓ SẢN PHẨM -->
      <div class="cart-title-row" v-if="cartItems.length > 0">
        <div class="title-with-icon">
          <div class="cart-brand-badge">
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#496883" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
          </div>
          <div>
            <h1 class="main-page-title">Giỏ Hàng Của Bạn</h1>
            <p class="main-page-subtitle">Quản lý và kiểm tra danh sách áo phông đã chọn trước khi thanh toán</p>
          </div>
        </div>

        <div class="cart-count-pill">
          <span>{{ cartItems.length }}</span> mặt hàng trong giỏ
        </div>
      </div>

      <!-- THANH TIẾN TRÌNH FREESHIP TOÀN QUỐC -->
      <div v-if="cartItems.length > 0" class="freeship-banner-card">
        <div class="freeship-header">
          <div class="fs-text">
            <span v-if="freeShipRemaining > 0">
              🚚 Mua thêm <strong>{{ formatCurrency(freeShipRemaining) }}</strong> để được <strong>FREESHIP toàn quốc!</strong>
            </span>
            <span v-else class="fs-unlocked">
              🎉 <strong>Chúc mừng!</strong> Bạn đã đủ điều kiện nhận <strong>Miễn Phí Vận Chuyển Toàn Quốc</strong>!
            </span>
          </div>
          <span class="fs-pct">{{ freeShipPercent }}%</span>
        </div>
        <div class="fs-progress-track">
          <div class="fs-progress-fill" :style="{ width: `${freeShipPercent}%` }"></div>
        </div>
      </div>

      <!-- GIỎ HÀNG TRỐNG TINH GỌN, ĐẸP MẮT -->
      <div v-if="cartItems.length === 0" class="empty-cart-card">
        <div class="clean-empty-icon-wrap">
          <div class="icon-circle-bg">
            <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="#496883" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="9" cy="20" r="1.5"></circle>
              <circle cx="18" cy="20" r="1.5"></circle>
              <path d="M1 1h4l2.68 12.8a2 2 0 0 0 2 1.6h9.72a2 2 0 0 0 2-1.6L23 5H6"></path>
            </svg>
          </div>
        </div>

        <h3 class="empty-title">Giỏ hàng của bạn đang trống</h3>
        <p class="empty-desc">Hiện chưa có sản phẩm nào trong giỏ hàng của bạn.</p>

        <router-link to="/cua-hang" class="btn-primary-explore">
          Khám Phá Cửa Hàng Ngay &rarr;
        </router-link>
      </div>

      <!-- BẢNG GIỎ HÀNG VÀ TÓM TẮT ĐƠN HÀNG HIỆN ĐẠI -->
      <div v-else class="cart-layout-grid">
        <!-- CỘT TRÁI: BẢNG SẢN PHẨM -->
        <div class="cart-table-panel">
          <div class="table-card">
            <!-- Header hàng cột -->
            <div class="cart-table-head">
              <div class="th-prod">
                <label class="checkbox-custom-label">
                  <input
                    type="checkbox"
                    :checked="isAllSelected"
                    @change="toggleSelectAll"
                    class="custom-chk"
                  />
                  <span>TẤT CẢ SẢN PHẨM ({{ cartItems.length }})</span>
                </label>
              </div>
              <div class="th-unit">ĐƠN GIÁ</div>
              <div class="th-discount">ƯU ĐÃI</div>
              <div class="th-qty">SỐ LƯỢNG</div>
              <div class="th-sum">THÀNH TIỀN</div>
              <div class="th-action">
                <button
                  v-if="selectedItemIds.length > 0"
                  class="btn-remove-selected-mini"
                  @click="handleRemoveSelected"
                  title="Xóa các mục đã chọn"
                >
                  Xóa
                </button>
              </div>
            </div>

            <!-- Danh sách từng dòng sản phẩm -->
            <div class="cart-table-rows">
              <div
                v-for="item in cartItems"
                :key="item.variantId"
                class="cart-item-row"
                :class="{ 'item-selected': selectedItemIds.includes(item.variantId) }"
              >
                <!-- Cột check + ảnh + tên + size + tag màu -->
                <div class="td-prod">
                  <input
                    type="checkbox"
                    :value="item.variantId"
                    v-model="selectedItemIds"
                    class="custom-chk"
                  />
                  <div class="item-thumb-box">
                    <img
                      :src="item.image || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200&q=80'"
                      :alt="item.productName"
                      class="item-img"
                    />
                  </div>
                  <div class="item-name-col">
                    <router-link :to="`/san-pham/${item.productId}`" class="name-link">
                      {{ item.productName }}
                    </router-link>
                    <div class="item-variant-pills">
                      <span class="var-pill color-pill">
                        <span class="color-dot"></span> {{ item.color }}
                      </span>
                      <span class="var-pill size-pill">Size: {{ item.size }}</span>
                    </div>
                    <span class="stock-status-tag">
                      <span class="stock-dot"></span> Còn hàng trong kho
                    </span>
                  </div>
                </div>

                <!-- Cột Đơn giá -->
                <div class="td-unit">
                  <span class="cur-price">{{ formatCurrency(item.discountPrice > 0 ? item.discountPrice : item.price) }}</span>
                  <span v-if="item.discountPrice > 0" class="old-price">{{ formatCurrency(item.price) }}</span>
                </div>

                <!-- Cột Giảm giá tag -->
                <div class="td-discount">
                  <span class="badge-tag-sale">Giảm 30%</span>
                </div>

                <!-- Cột Số lượng Stepper [ - ] 1 [ + ] -->
                <div class="td-qty">
                  <div class="stepper-wrap">
                    <button
                      class="step-btn"
                      :disabled="item.quantity <= 1"
                      @click="handleQtyChange(item.variantId, -1, item.quantity, item.maxStock)"
                      title="Giảm 1"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                      </svg>
                    </button>
                    <span class="step-num">{{ item.quantity }}</span>
                    <button
                      class="step-btn"
                      :disabled="item.quantity >= (item.maxStock || 99)"
                      @click="handleQtyChange(item.variantId, 1, item.quantity, item.maxStock)"
                      title="Tăng 1"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <line x1="12" y1="5" x2="12" y2="19"></line>
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                      </svg>
                    </button>
                  </div>
                </div>

                <!-- Cột Tổng tiền của dòng -->
                <div class="td-sum">
                  <span class="item-line-total">
                    {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
                  </span>
                </div>

                <!-- Cột Nút xóa với SVG thùng rác cao cấp -->
                <div class="td-action">
                  <button
                    class="btn-trash-action"
                    title="Xóa khỏi giỏ hàng"
                    @click="handleRemove(item.variantId, item.productName)"
                  >
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                      <line x1="10" y1="11" x2="10" y2="17"></line>
                      <line x1="14" y1="11" x2="14" y2="17"></line>
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Chân bảng: Tiếp tục mua sắm & Xóa hết -->
            <div class="cart-bottom-actions-row">
              <router-link to="/cua-hang" class="link-continue-shop">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="19" y1="12" x2="5" y2="12"></line>
                  <polyline points="12 19 5 12 12 5"></polyline>
                </svg>
                <span>Tiếp tục chọn thêm sản phẩm khác</span>
              </router-link>

              <button
                v-if="selectedItemIds.length > 0"
                class="btn-clear-selection"
                @click="handleRemoveSelected"
              >
                Xóa {{ selectedItemIds.length }} sản phẩm đã chọn
              </button>
            </div>
          </div>
        </div>

        <!-- CỘT PHẢI: THẺ TÓM TẮT ĐƠN HÀNG SANG TRỌNG -->
        <aside class="cart-summary-panel">
          <div class="summary-card-modern">
            <div class="summary-header">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              <h3 class="summary-card-title">TÓM TẮT ĐƠN HÀNG</h3>
            </div>

            <!-- KHỐI VOUCHER KHUYẾN MÃI -->
            <div class="voucher-box-section">
              <label class="voucher-lbl">MÃ GIẢM GIÁ / VOUCHER</label>
              
              <div v-if="!appliedVoucher" class="voucher-input-group">
                <input
                  type="text"
                  v-model="voucherInput"
                  placeholder="Nhập mã voucher..."
                  @keyup.enter="handleApplyVoucher()"
                  class="voucher-input"
                />
                <button class="btn-apply-voucher" @click="handleApplyVoucher()">
                  Áp Dụng
                </button>
              </div>

              <!-- Thẻ voucher đã áp dụng -->
              <div v-else class="applied-voucher-badge">
                <div class="av-info">
                  <span class="av-icon">🎟️</span>
                  <div>
                    <strong>{{ appliedVoucher.code }}</strong>
                    <span>-{{ formatCurrency(appliedVoucher.discount) }}</span>
                  </div>
                </div>
                <button class="btn-remove-voucher" @click="handleRemoveVoucher" title="Bỏ voucher">✕</button>
              </div>

              <!-- Thông báo voucher -->
              <p v-if="voucherMessage.text" class="voucher-status-msg" :class="voucherMessage.type">
                {{ voucherMessage.text }}
              </p>

              <!-- Gợi ý voucher nhanh -->
              <div class="quick-vouchers-strip" v-if="!appliedVoucher">
                <span class="qv-tag" @click="handleApplyVoucher('GIAM30K')">GIAM30K (-30k)</span>
                <span class="qv-tag" @click="handleApplyVoucher('GIAM50K')">GIAM50K (-50k)</span>
              </div>
            </div>

            <div class="summary-divider"></div>

            <!-- CHI TIẾT TÍNH TOÁN -->
            <div class="calc-lines-wrap">
              <div class="calc-line">
                <span class="lbl">Sản phẩm đã chọn:</span>
                <span class="val font-semibold">{{ selectedCount }} chiếc</span>
              </div>

              <div class="calc-line">
                <span class="lbl">Tạm tính:</span>
                <span class="val font-semibold">{{ formatCurrency(selectedSubtotal) }}</span>
              </div>

              <div class="calc-line discount-line" v-if="voucherDiscountAmount > 0">
                <span class="lbl">Voucher giảm giá:</span>
                <span class="val text-green">-{{ formatCurrency(voucherDiscountAmount) }}</span>
              </div>

              <div class="calc-line">
                <span class="lbl">Phí vận chuyển:</span>
                <span class="val" :class="{ 'text-free': shippingFee === 0 }">
                  {{ shippingFee === 0 ? 'Miễn phí (Toàn quốc)' : formatCurrency(shippingFee) }}
                </span>
              </div>
            </div>

            <div class="summary-divider"></div>

            <!-- TỔNG THANH TOÁN -->
            <div class="total-amount-box">
              <div class="total-text-col">
                <span class="lbl-total">TỔNG THANH TOÁN</span>
                <span class="sub-vat">(Đã bao gồm VAT nếu có)</span>
              </div>
              <span class="val-total-highlight">{{ formatCurrency(finalTotal) }}</span>
            </div>

            <!-- NÚT TIẾN HÀNH THANH TOÁN -->
            <button
              class="btn-proceed-checkout"
              :disabled="selectedItemIds.length === 0"
              @click="proceedToCheckout"
            >
              <span>TIẾN HÀNH THANH TOÁN</span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </button>

            <!-- KHỐI CAM KẾT CHUẨN THƯƠNG HIỆU -->
            <div class="cart-trust-badges">
              <div class="trust-item">
                <span class="t-icon">🛡️</span>
                <span>100% Cotton cao cấp chính hãng</span>
              </div>
              <div class="trust-item">
                <span class="t-icon">⚡</span>
                <span>Giao hàng hỏa tốc 2 - 4 ngày</span>
              </div>
              <div class="trust-item">
                <span class="t-icon">🔄</span>
                <span>Đổi size miễn phí trong 7 ngày</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page-wrap {
  background-color: #f8fafc;
  min-height: 85vh;
  padding-bottom: 80px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1260px;
  margin: 0 auto;
  padding: 0 1.5rem;
}


/* 2. Tiêu đề trang */
.cart-body-container {
  margin-top: 32px;
}

.cart-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cart-brand-badge {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #ffffff;
  border: 1.5px solid #ecd8af;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.1);
  flex-shrink: 0;
}

.main-page-title {
  font-size: 26px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 4px;
  letter-spacing: -0.5px;
}

.main-page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.cart-count-pill {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: #64748b;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.cart-count-pill span {
  font-weight: 800;
  color: #496883;
}

/* 3. Banner Freeship */
.freeship-banner-card {
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  border: 1.5px solid #dbeafe;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.05);
}

.freeship-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #1e3a8a;
  margin-bottom: 8px;
}

.fs-unlocked {
  color: #059669;
}

.fs-pct {
  font-weight: 800;
  color: #496883;
  font-size: 12px;
}

.fs-progress-track {
  width: 100%;
  height: 7px;
  background: #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.fs-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #496883 0%, #d2a764 100%);
  border-radius: 10px;
  transition: width 0.4s ease;
}

/* 4. Giỏ hàng trống tinh gọn & thanh lịch */
.empty-cart-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 56px 28px;
  text-align: center;
  max-width: 480px;
  margin: 40px auto;
  box-shadow: 0 4px 20px rgba(73, 104, 131, 0.05);
}

.clean-empty-icon-wrap {
  margin-bottom: 22px;
}

.icon-circle-bg {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  color: #496883;
  transition: transform 0.25s ease, background 0.25s ease;
}

.icon-circle-bg:hover {
  transform: scale(1.06);
  background: #e8f0f8;
}

.empty-title {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px;
  letter-spacing: -0.3px;
}

.empty-desc {
  color: #64748b;
  font-size: 14px;
  margin: 0 0 24px;
}

.btn-primary-explore {
  background: #496883;
  color: #ffffff;
  padding: 12px 28px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 700;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.22);
}

.btn-primary-explore:hover {
  background: #364f66;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(73, 104, 131, 0.32);
}

/* 5. Grid Layout */
.cart-layout-grid {
  display: grid;
  grid-template-columns: 1fr 370px;
  gap: 28px;
  align-items: flex-start;
}

/* 6. Bảng sản phẩm */
.table-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(73, 104, 131, 0.05);
}

.cart-table-head {
  display: grid;
  grid-template-columns: 3.2fr 1.2fr 1fr 1.3fr 1.3fr 42px;
  padding: 16px 24px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-size: 12px;
  font-weight: 800;
  color: #496883;
  letter-spacing: 0.6px;
  align-items: center;
}

.checkbox-custom-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.custom-chk {
  width: 18px;
  height: 18px;
  accent-color: #496883;
  cursor: pointer;
}

.btn-remove-selected-mini {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 4px;
  text-decoration: underline;
}

/* Từng dòng sản phẩm */
.cart-table-rows {
  display: flex;
  flex-direction: column;
}

.cart-item-row {
  display: grid;
  grid-template-columns: 3.2fr 1.2fr 1fr 1.3fr 1.3fr 42px;
  padding: 20px 24px;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  transition: background-color 0.15s;
}

.cart-item-row:last-child {
  border-bottom: none;
}

.cart-item-row:hover {
  background-color: #fafcff;
}

.cart-item-row.item-selected {
  background-color: #fbfdff;
}

.td-prod {
  display: flex;
  align-items: center;
  gap: 14px;
}

.item-thumb-box {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  flex-shrink: 0;
}

.item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.25s;
}

.item-thumb-box:hover .item-img {
  transform: scale(1.06);
}

.item-name-col {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.name-link {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  text-decoration: none;
  line-height: 1.4;
  transition: color 0.15s;
}

.name-link:hover {
  color: #496883;
}

.item-variant-pills {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.var-pill {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #475569;
  font-weight: 600;
}

.color-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.color-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #d2a764;
}

.stock-status-tag {
  font-size: 11px;
  color: #059669;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.stock-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: #059669;
}

.td-unit {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cur-price {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
}

.old-price {
  font-size: 11.5px;
  color: #94a3b8;
  text-decoration: line-through;
}

.badge-tag-sale {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
  padding: 3px 9px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 800;
}

/* Stepper số lượng */
.stepper-wrap {
  display: inline-flex;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.step-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: #f8fafc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #496883;
  transition: all 0.15s;
}

.step-btn:hover:not(:disabled) {
  background: #e2e8f0;
  color: #1e293b;
}

.step-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.step-num {
  width: 32px;
  text-align: center;
  font-weight: 800;
  font-size: 13px;
  line-height: 30px;
  color: #1e293b;
}

.item-line-total {
  font-size: 14.5px;
  font-weight: 800;
  color: #496883;
}

/* Nút xóa thùng rác */
.btn-trash-action {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-trash-action:hover {
  color: #ef4444;
  background: #fee2e2;
  transform: scale(1.1);
}

/* Chân bảng */
.cart-bottom-actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.link-continue-shop {
  font-size: 13px;
  font-weight: 700;
  color: #496883;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.link-continue-shop:hover {
  color: #d2a764;
  transform: translateX(-3px);
}

.btn-clear-selection {
  background: none;
  border: none;
  font-size: 12.5px;
  font-weight: 600;
  color: #ef4444;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.btn-clear-selection:hover {
  background: #fee2e2;
}

/* 7. Summary Card bên phải */
.summary-card-modern {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(73, 104, 131, 0.06);
  position: sticky;
  top: 90px;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #496883;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.summary-card-title {
  font-size: 15px;
  font-weight: 800;
  color: #496883;
  letter-spacing: 0.5px;
  margin: 0;
}

/* Voucher box */
.voucher-box-section {
  margin-bottom: 18px;
}

.voucher-lbl {
  display: block;
  font-size: 11px;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.6px;
  margin-bottom: 8px;
}

.voucher-input-group {
  display: flex;
  gap: 8px;
}

.voucher-input {
  flex: 1;
  padding: 9px 12px;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  text-transform: uppercase;
  outline: none;
  transition: border-color 0.2s;
}

.voucher-input:focus {
  border-color: #496883;
}

.btn-apply-voucher {
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-apply-voucher:hover {
  background: #364f66;
}

.applied-voucher-badge {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 8px 12px;
  border-radius: 8px;
}

.av-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}

.av-info strong {
  color: #15803d;
  display: block;
}

.av-info span {
  font-size: 11px;
  color: #166534;
}

.btn-remove-voucher {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.btn-remove-voucher:hover {
  color: #ef4444;
  background: #fee2e2;
}

.voucher-status-msg {
  font-size: 11px;
  margin: 6px 0 0;
  font-weight: 600;
}

.voucher-status-msg.error {
  color: #ef4444;
}

.voucher-status-msg.success {
  color: #16a34a;
}

.quick-vouchers-strip {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.qv-tag {
  font-size: 10.5px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 12px;
  background: #f1f5f9;
  color: #496883;
  border: 1px dashed #cbd5e1;
  cursor: pointer;
  transition: all 0.15s;
}

.qv-tag:hover {
  background: #e2e8f0;
  border-color: #496883;
}

.summary-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 16px 0;
}

/* Các dòng tính toán */
.calc-lines-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.calc-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #64748b;
}

.calc-line .val {
  color: #1e293b;
}

.font-semibold {
  font-weight: 700;
}

.text-green {
  color: #16a34a;
  font-weight: 700;
}

.text-free {
  color: #059669;
  font-weight: 700;
}

/* Hộp tổng tiền */
.total-amount-box {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22px;
}

.total-text-col {
  display: flex;
  flex-direction: column;
}

.lbl-total {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: 0.3px;
}

.sub-vat {
  font-size: 10.5px;
  color: #94a3b8;
}

.val-total-highlight {
  color: #dc2626;
  font-size: 22px;
  font-weight: 800;
}

/* Nút đặt hàng */
.btn-proceed-checkout {
  width: 100%;
  padding: 14px 0;
  background: linear-gradient(135deg, #496883 0%, #364f66 100%);
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 800;
  letter-spacing: 0.6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.25s;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.28);
}

.btn-proceed-checkout:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(73, 104, 131, 0.4);
}

.btn-proceed-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* Cam kết dịch vụ */
.cart-trust-badges {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: #64748b;
}

.t-icon {
  font-size: 14px;
}

/* Responsive */
@media (max-width: 960px) {
  .cart-layout-grid {
    grid-template-columns: 1fr;
  }
  .summary-card-modern {
    position: static;
  }
}

@media (max-width: 768px) {
  .cart-nav-steps {
    display: none;
  }
  .cart-table-head {
    display: none;
  }
  .cart-item-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
    position: relative;
  }
  .td-prod {
    width: calc(100% - 40px);
  }
  .td-action {
    position: absolute;
    top: 16px;
    right: 16px;
  }
  .td-unit, .td-discount {
    display: inline-block;
  }
  .td-qty {
    margin-left: auto;
  }
  .td-sum {
    width: 100%;
    text-align: right;
    padding-top: 8px;
    border-top: 1px dashed #f1f5f9;
  }
}
</style>
