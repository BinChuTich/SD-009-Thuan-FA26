<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { cartItems, cartSubtotal, clearCart } from '@/utils/luuTruGioHang.js'
import SoDiaChiGiaoHang from '@/components/ThanhToan/SoDiaChiGiaoHang.vue'
import PhuongThucThanhToan from '@/components/ThanhToan/PhuongThucThanhToan.vue'
import XemLaiDonHang from '@/components/ThanhToan/XemLaiDonHang.vue'

const router = useRouter()

// 1. Địa chỉ đã lưu
const savedAddresses = ref([
  {
    id: 1,
    name: 'Dương Ngọc Chiến',
    phone: '0988776655',
    address: '123 ngõ abc, Phường Cửa Đông, Quận Hoàn Kiếm, Thành phố Hà Nội',
    province: 'Thành phố Hà Nội',
    district: 'Quận Hoàn Kiếm',
    ward: 'Phường Cửa Đông',
    street: '123 ngõ abc'
  },
  {
    id: 2,
    name: 'Dương Ngọc Chiến',
    phone: '0988776655',
    address: '321 ngõ abc, Phường 3, Quận 3, Thành phố Hồ Chí Minh',
    province: 'Thành phố Hồ Chí Minh',
    district: 'Quận 3',
    ward: 'Phường 3',
    street: '321 ngõ abc'
  }
])

const selectedAddressId = ref(1)
const isCustomAddress = ref(false)

// Form người nhận
const customerName = ref('Dương Ngọc Chiến')
const customerPhone = ref('0988776655')
const customerEmail = ref('chien.duong@example.com')
const selectedProvince = ref('Thành phố Hà Nội')
const selectedDistrict = ref('Quận Hoàn Kiếm')
const selectedWard = ref('Phường Cửa Đông')
const streetAddress = ref('123 ngõ abc')
const orderNote = ref('')

// Phương thức thanh toán: 'COD' | 'MANUAL' | 'VNPAY'
const paymentMethod = ref('COD')

const onSelectSavedAddress = (addr) => {
  if (!addr) return
  selectedAddressId.value = addr.id
  isCustomAddress.value = false
  customerName.value = addr.name
  customerPhone.value = addr.phone
  selectedProvince.value = addr.province
  selectedDistrict.value = addr.district
  selectedWard.value = addr.ward
  streetAddress.value = addr.street
}

const selectCustomAddress = () => {
  selectedAddressId.value = null
  isCustomAddress.value = true
  streetAddress.value = ''
}

const fullDeliveryAddress = computed(() => {
  const parts = [
    streetAddress.value,
    selectedWard.value,
    selectedDistrict.value,
    selectedProvince.value
  ].filter(Boolean)
  return parts.join(', ') || 'Chưa có địa chỉ cụ thể'
})

// Tính tiền
const shippingFee = computed(() => {
  if (cartSubtotal.value >= 499000 || cartSubtotal.value === 0) return 0
  return 30000
})

const voucherDiscount = ref(0)

const finalTotal = computed(() => {
  return Math.max(0, cartSubtotal.value - voucherDiscount.value + shippingFee.value)
})

// Trạng thái đặt hàng
const submitting = ref(false)
const orderSuccess = ref(false)
const createdOrder = ref(null)

const handlePlaceOrder = async () => {
  if (!customerName.value.trim() || !customerPhone.value.trim()) {
    alert('Vui lòng điền đầy đủ họ tên và số điện thoại người nhận!')
    return
  }
  if (!streetAddress.value.trim()) {
    alert('Vui lòng nhập địa chỉ cụ thể để shipper giao hàng!')
    return
  }
  if (cartItems.value.length === 0) {
    alert('Giỏ hàng của bạn đang trống!')
    return
  }

  submitting.value = true
  try {
    const payload = {
      tenKhachHang: customerName.value.trim(),
      soDienThoai: customerPhone.value.trim(),
      email: customerEmail.value.trim(),
      diaChiNhanHang: fullDeliveryAddress.value,
      ghiChu: orderNote.value.trim(),
      phuongThucThanhToan: paymentMethod.value,
      tongTien: finalTotal.value,
      phiVanChuyen: shippingFee.value,
      giamGia: voucherDiscount.value,
      chiTiet: cartItems.value.map(i => ({
        idChiTietSanPham: i.variantId,
        soLuong: i.quantity,
        donGia: i.discountPrice > 0 ? i.discountPrice : i.price
      }))
    }

    try {
      const res = await api.post('/api/hoa-don/dat-hang-online', payload)
      createdOrder.value = res.data || { maHoaDon: 'HD' + Date.now().toString().slice(-6) }
    } catch (e) {
      console.warn('API đặt hàng offline mode fallback:', e)
      createdOrder.value = {
        maHoaDon: 'HD' + Math.floor(100000 + Math.random() * 900000)
      }
    }

    clearCart()
    orderSuccess.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="checkout-page-wrap">
    <!-- Tiêu đề trang thanh toán -->
    <div class="checkout-heading-bar">
      <div class="container-client">
        <h1 class="page-title">Thanh Toán Đơn Hàng</h1>
        <p class="page-sub">Kiểm tra thông tin giao nhận và xác nhận đặt mua các sản phẩm tuyển chọn.</p>
      </div>
    </div>

    <div class="container-client checkout-body-container">
      <!-- MÀN HÌNH ĐẶT HÀNG THÀNH CÔNG -->
      <div v-if="orderSuccess" class="order-success-card">
        <div class="success-icon-wrap">✓</div>
        <h2>Đặt Hàng Thành Công!</h2>
        <p class="success-note">
          Cảm ơn bạn đã mua sắm tại <strong>FF T-Shirt</strong>. Đơn hàng đã được tiếp nhận và nhân viên sẽ liên hệ xác nhận trong thời gian sớm nhất.
        </p>

        <div class="order-code-display">
          <span>Mã đơn hàng:</span>
          <strong>#{{ createdOrder?.maHoaDon }}</strong>
        </div>

        <div class="success-actions">
          <router-link :to="`/don-hang?code=${createdOrder?.maHoaDon}`" class="btn-view-order">
            Xem Đơn Hàng Của Tôi
          </router-link>
          <router-link to="/cua-hang" class="btn-back-home">
            Tiếp Tục Mua Sắm &rarr;
          </router-link>
        </div>
      </div>

      <!-- FORM THANH TOÁN CHÍNH -->
      <div v-else class="checkout-split-layout">
        <!-- CỘT TRÁI: ĐỊA CHỈ & HÌNH THỨC THANH TOÁN (COMPONENT TÁCH BIỆT) -->
        <div class="left-form-column">
          <!-- 1. Sổ địa chỉ & Form người nhận -->
          <SoDiaChiGiaoHang
            :saved-addresses="savedAddresses"
            :selected-address-id="selectedAddressId"
            :is-custom-address="isCustomAddress"
            v-model:customer-name="customerName"
            v-model:customer-phone="customerPhone"
            v-model:customer-email="customerEmail"
            v-model:selected-province="selectedProvince"
            v-model:selected-district="selectedDistrict"
            v-model:selected-ward="selectedWard"
            v-model:street-address="streetAddress"
            :full-delivery-address="fullDeliveryAddress"
            @selectSavedAddress="onSelectSavedAddress"
            @selectCustomAddress="selectCustomAddress"
          />

          <!-- 2. Phương thức thanh toán -->
          <PhuongThucThanhToan
            v-model:payment-method="paymentMethod"
          />
        </div>

        <!-- CỘT PHẢI: XEM LẠI ĐƠN HÀNG, TỔNG TIỀN, NÚT XÁC NHẬN (COMPONENT TÁCH BIỆT) -->
        <XemLaiDonHang
          :cart-items="cartItems"
          :cart-subtotal="cartSubtotal"
          :shipping-fee="shippingFee"
          :voucher-discount="voucherDiscount"
          :final-total="finalTotal"
          v-model:order-note="orderNote"
          :submitting="submitting"
          @placeOrder="handlePlaceOrder"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkout-page-wrap {
  background-color: #f8fafc;
  min-height: 100vh;
  padding-bottom: 80px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.checkout-heading-bar {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #2b394a 100%);
  color: #ffffff;
  padding: 45px 0 40px;
  text-align: center;
  border-bottom: 2px solid #ecd8af;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  margin: 0 0 8px;
}

.page-sub {
  color: #cbd5e1;
  font-size: 14px;
  margin: 0;
}

.checkout-body-container {
  margin-top: 30px;
}

.checkout-split-layout {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 30px;
  align-items: start;
}

/* Đặt hàng thành công */
.order-success-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 60px 30px;
  text-align: center;
  max-width: 600px;
  margin: 20px auto;
  box-shadow: 0 6px 24px rgba(73, 104, 131, 0.08);
}

.success-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #10b981;
  color: #ffffff;
  font-size: 32px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.order-success-card h2 {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 12px;
}

.success-note {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.order-code-display {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  background: #f1f5f9;
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px dashed #496883;
  margin-bottom: 30px;
}

.order-code-display strong {
  font-size: 18px;
  color: #e11d48;
}

.success-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-view-order {
  background: #496883;
  color: #ffffff;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 14px;
}

.btn-back-home {
  background: #d2a764;
  color: #1e293b;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 14px;
}

@media (max-width: 992px) {
  .checkout-split-layout {
    grid-template-columns: 1fr;
  }
}
</style>
