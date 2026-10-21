<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import {
  cartItems,
  updateCartQuantity,
  removeFromCart,
  clearCart,
  addToCart,
  saveCart,
  setCheckoutItems,
  setCheckoutVoucher
} from '@/utils/luuTruGioHang.js'
import { getMergedProducts } from '@/utils/danhMucSanPham.js'
import BangSanPhamGioHang from '@/components/GioHang/BangSanPhamGioHang.vue'
import TongKetGioHang from '@/components/GioHang/TongKetGioHang.vue'
import TheSanPham from '@/components/DungChung/TheSanPham.vue'

const router = useRouter()

// Sản phẩm gợi ý khi giỏ hàng trống hoặc chân trang
const recommendedProducts = ref([])

const loadRecommendedProducts = async () => {
  try {
    const res = await api.get('/api/san-pham', {
      params: { page: 0, size: 8, trangThai: 1 }
    })
    const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    recommendedProducts.value = getMergedProducts(list).slice(0, 4)
  } catch (e) {
    console.warn('Lỗi tải gợi ý sản phẩm:', e)
  }
}

// Lựa chọn checkbox từng sản phẩm
const selectedItemIds = ref(cartItems.value.map(i => i.variantId))

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

const handleQtyChange = (variantId, delta, currentQty, maxStock) => {
  const newQty = currentQty + delta
  if (newQty <= 0) {
    handleRemove(variantId)
    return
  }
  if (maxStock && newQty > maxStock) return
  updateCartQuantity(variantId, newQty)
}

const handleSetQuantity = (variantId, newQty, maxStock) => {
  let q = parseInt(newQty, 10)
  if (isNaN(q) || q < 1) q = 1
  if (maxStock && q > maxStock) q = maxStock
  updateCartQuantity(variantId, q)
}

const handleRemove = (variantId, name = '') => {
  if (confirm(`Bạn có chắc muốn xóa sản phẩm ${name ? '"' + name + '"' : ''} khỏi giỏ hàng?`)) {
    removeFromCart(variantId)
    selectedItemIds.value = selectedItemIds.value.filter(id => id !== variantId)
  }
}

const handleRemoveSelected = () => {
  if (!selectedItemIds.value.length) return
  if (confirm(`Xác nhận xóa ${selectedItemIds.value.length} sản phẩm đã chọn?`)) {
    selectedItemIds.value.forEach(id => removeFromCart(id))
    selectedItemIds.value = []
  }
}

const handleClearCart = () => {
  if (confirm('Bạn có chắc chắn muốn xóa tất cả sản phẩm khỏi giỏ hàng?')) {
    clearCart()
    selectedItemIds.value = []
  }
}

// Voucher khuyến mãi
const voucherInput = ref('')
const appliedVoucher = ref(null)

const handleApplyVoucher = () => {
  const code = voucherInput.value.trim().toUpperCase()
  if (!code) return
  if (code === 'FFVIP10' || code === 'SALE10') {
    const disc = Math.max(30000, Math.round(selectedSubtotal.value * 0.1))
    appliedVoucher.value = { code, discount: disc }
    voucherInput.value = ''
  } else if (code === 'FREESHIP') {
    appliedVoucher.value = { code, discount: 30000 }
    voucherInput.value = ''
  } else {
    alert('Mã giảm giá không hợp lệ hoặc đã hết lượt sử dụng!')
  }
}

const handleApplyQuickVoucher = (code) => {
  voucherInput.value = code
  handleApplyVoucher()
}

const handleRemoveVoucher = () => {
  appliedVoucher.value = null
}

// Tính tiền
const selectedSubtotal = computed(() => {
  return cartItems.value
    .filter(item => selectedItemIds.value.includes(item.variantId))
    .reduce((sum, item) => {
      const p = item.discountPrice > 0 ? item.discountPrice : item.price
      return sum + p * item.quantity
    }, 0)
})

const shippingFee = computed(() => {
  if (selectedSubtotal.value === 0) return 0
  if (selectedSubtotal.value >= 499000 || appliedVoucher.value?.code === 'FREESHIP') return 0
  return 30000
})

const voucherDiscount = computed(() => {
  if (!appliedVoucher.value) return 0
  return Math.min(appliedVoucher.value.discount, selectedSubtotal.value)
})

const finalTotal = computed(() => {
  return Math.max(0, selectedSubtotal.value - voucherDiscount.value + shippingFee.value)
})

const goToCheckout = () => {
  if (!selectedItemIds.value.length) {
    alert('Vui lòng tích chọn ít nhất 1 sản phẩm để tiến hành thanh toán!')
    return
  }
  const itemsToCheckout = cartItems.value.filter(item => selectedItemIds.value.includes(item.variantId))
  setCheckoutItems(itemsToCheckout)
  setCheckoutVoucher(appliedVoucher.value)
  router.push('/thanh-toan')
}

// Đồng bộ số lượng tồn kho thực tế từ backend khi vào giỏ hàng
const syncCartStock = async () => {
  if (!cartItems.value || cartItems.value.length === 0) return
  let updated = false
  await Promise.all(
    cartItems.value.map(async (item) => {
      try {
        if (!item.variantId) return
        const res = await api.get(`/api/chi-tiet-san-pham/${item.variantId}`)
        if (res.data) {
          const freshStock = res.data.soLuong != null 
            ? res.data.soLuong 
            : (res.data.soLuongTon != null ? res.data.soLuongTon : 0)
          if (item.maxStock !== freshStock) {
            item.maxStock = freshStock
            updated = true
          }
          if (freshStock > 0 && item.quantity > freshStock) {
            item.quantity = freshStock
            updated = true
          }
        }
      } catch (e) {
        // Ignored
      }
    })
  )
  if (updated) {
    saveCart()
  }
}

onMounted(() => {
  syncCartStock()
  loadRecommendedProducts()
})
</script>

<template>
  <div class="cart-page-wrap">
    <!-- Tiêu đề trang giỏ hàng -->
    <div class="cart-hero-header">
      <div class="container-client">
        <h1 class="cart-title">Giỏ Hàng Của Bạn</h1>
        <p class="cart-subtitle">
          Quản lý sản phẩm đã chọn, áp dụng mã ưu đãi và tiến hành đặt hàng tiện lợi.
        </p>
      </div>
    </div>

    <div class="container-client cart-body-box">
      <!-- Khi giỏ hàng trống -->
      <div v-if="cartItems.length === 0" class="empty-cart-view">
        <div class="empty-cart-icon">🛒</div>
        <h2>Giỏ hàng của bạn đang trống</h2>
        <p>Hãy khám phá thêm các mẫu áo phông nam cao cấp mới nhất của chúng tôi!</p>
        <router-link to="/cua-hang" class="btn-shop-now">
          Khám Phá Cửa Hàng Ngay &rarr;
        </router-link>

        <!-- Gợi ý sản phẩm -->
        <div class="recommended-section">
          <h3>Gợi ý sản phẩm thịnh hành</h3>
          <div class="recom-grid">
            <TheSanPham
              v-for="p in recommendedProducts"
              :key="p.id"
              :product="p"
            />
          </div>
        </div>
      </div>

      <!-- Khi có sản phẩm trong giỏ hàng -->
      <div v-else class="cart-layout-grid">
        <!-- 1. BẢNG DANH SÁCH MÓN HÀNG -->
        <BangSanPhamGioHang
          :cart-items="cartItems"
          v-model:selected-item-ids="selectedItemIds"
          :is-all-selected="isAllSelected"
          @toggleSelectAll="toggleSelectAll"
          @qtyChange="handleQtyChange"
          @setQty="handleSetQuantity"
          @removeItem="handleRemove"
          @removeSelected="handleRemoveSelected"
          @clearAll="handleClearCart"
        />

        <!-- 2. THẺ TÓM TẮT ĐƠN HÀNG & THANH TOÁN -->
        <TongKetGioHang
          :selected-count="selectedItemIds.length"
          :selected-subtotal="selectedSubtotal"
          :shipping-fee="shippingFee"
          :voucher-discount="voucherDiscount"
          :final-total="finalTotal"
          v-model:voucher-input="voucherInput"
          :applied-voucher="appliedVoucher"
          @applyVoucher="handleApplyVoucher"
          @applyQuickVoucher="handleApplyQuickVoucher"
          @removeVoucher="handleRemoveVoucher"
          @checkout="goToCheckout"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page-wrap {
  background-color: #f8fafc;
  min-height: 100vh;
  padding-bottom: 80px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.cart-hero-header {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #2b394a 100%);
  color: #ffffff;
  padding: 45px 0 40px;
  text-align: center;
  border-bottom: 2px solid #ecd8af;
}

.container-client {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.cart-title {
  font-size: 30px;
  font-weight: 800;
  margin: 0 0 8px;
}

.cart-subtitle {
  color: #cbd5e1;
  font-size: 14px;
  margin: 0;
}

.cart-body-box {
  margin-top: 30px;
}

.cart-layout-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 30px;
  align-items: start;
}

/* Empty cart */
.empty-cart-view {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 60px 30px;
  text-align: center;
}

.empty-cart-icon {
  font-size: 50px;
  margin-bottom: 16px;
}

.empty-cart-view h2 {
  font-size: 22px;
  font-weight: 800;
  margin: 0 0 10px;
}

.empty-cart-view p {
  color: #64748b;
  margin-bottom: 24px;
}

.btn-shop-now {
  display: inline-block;
  background: #d2a764;
  color: #1e293b;
  font-weight: 700;
  padding: 12px 28px;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
}

.btn-shop-now:hover {
  background: #e0b875;
}

.recommended-section {
  margin-top: 50px;
  text-align: left;
}

.recommended-section h3 {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 20px;
}

.recom-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 992px) {
  .cart-layout-grid {
    grid-template-columns: 1fr;
  }
  .recom-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 580px) {
  .recom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
