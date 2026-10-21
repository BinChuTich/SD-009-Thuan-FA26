<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { cartItems, cartSubtotal, clearCart } from '@/utils/luuTruGioHang.js'
import { formatCurrency } from '@/utils/dinhDang.js'
import SoDiaChiGiaoHang from '@/components/ThanhToan/SoDiaChiGiaoHang.vue'
import PhuongThucThanhToan from '@/components/ThanhToan/PhuongThucThanhToan.vue'
import XemLaiDonHang from '@/components/ThanhToan/XemLaiDonHang.vue'

const router = useRouter()

// 1. Địa chỉ đã lưu (Theo chuẩn 2 cấp hành chính mới: Tỉnh/Thành phố & Phường/Xã)
const savedAddresses = ref([
  {
    id: 1,
    name: 'Dương Ngọc Chiến',
    phone: '0988776655',
    address: '123 ngõ abc, Phường Hoàn Kiếm, Thành phố Hà Nội',
    province: 'Thành phố Hà Nội',
    district: '',
    ward: 'Phường Hoàn Kiếm',
    street: '123 ngõ abc'
  },
  {
    id: 2,
    name: 'Dương Ngọc Chiến',
    phone: '0988776655',
    address: '321 ngõ abc, Phường Bến Nghé, Thành phố Hồ Chí Minh',
    province: 'Thành phố Hồ Chí Minh',
    district: '',
    ward: 'Phường Bến Nghé',
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
const selectedDistrict = ref('')
const selectedWard = ref('Phường Hoàn Kiếm')
const streetAddress = ref('123 ngõ abc')
const orderNote = ref('')

// Phương thức thanh toán: 'COD' | 'VNPAY'
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
const emptyCartWarning = ref(false)
const copySuccess = ref(false)

// Đối tượng lưu trữ lỗi của từng trường nhập liệu (Báo đỏ bên dưới)
const errors = ref({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  selectedProvince: '',
  selectedWard: '',
  streetAddress: ''
})

const clearFieldError = (fieldName) => {
  if (errors.value[fieldName]) {
    errors.value[fieldName] = ''
  }
}

const copyOrderCode = (code) => {
  if (!code) return
  navigator.clipboard.writeText(code).then(() => {
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  })
}

const validateForm = () => {
  let isValid = true
  errors.value = {
    customerName: '',
    customerPhone: '',
    customerEmail: '',
    selectedProvince: '',
    selectedWard: '',
    streetAddress: ''
  }

  // 1. Kiểm tra họ tên
  const name = (customerName.value || '').trim()
  if (!name) {
    errors.value.customerName = 'Vui lòng điền họ và tên người nhận'
    isValid = false
  } else if (name.length < 2) {
    errors.value.customerName = 'Họ và tên người nhận phải có ít nhất 2 ký tự'
    isValid = false
  }

  // 2. Kiểm tra số điện thoại (10 số, bắt đầu bằng 03, 05, 07, 08, 09)
  const phone = (customerPhone.value || '').trim()
  const phoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/
  if (!phone) {
    errors.value.customerPhone = 'Vui lòng điền số điện thoại người nhận'
    isValid = false
  } else if (!phoneRegex.test(phone)) {
    errors.value.customerPhone = 'Số điện thoại không hợp lệ (gồm 10 chữ số, ví dụ: 0988776655)'
    isValid = false
  }

  // 3. Kiểm tra email
  const email = (customerEmail.value || '').trim()
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!email) {
    errors.value.customerEmail = 'Vui lòng điền địa chỉ email để nhận hóa đơn điện tử'
    isValid = false
  } else if (!emailRegex.test(email)) {
    errors.value.customerEmail = 'Định dạng email không hợp lệ (ví dụ: contact@gmail.com)'
    isValid = false
  }

  // 4. Kiểm tra Tỉnh / Thành phố
  if (!selectedProvince.value || !selectedProvince.value.trim()) {
    errors.value.selectedProvince = 'Vui lòng chọn Tỉnh/Thành phố'
    isValid = false
  }

  // 5. Kiểm tra Phường / Xã
  if (!selectedWard.value || !selectedWard.value.trim()) {
    errors.value.selectedWard = 'Vui lòng chọn Phường/Xã'
    isValid = false
  }

  // 6. Kiểm tra Địa chỉ cụ thể
  if (!streetAddress.value || !streetAddress.value.trim()) {
    errors.value.streetAddress = 'Vui lòng nhập địa chỉ cụ thể (số nhà, ngõ/ngách, tên đường)'
    isValid = false
  }

  return isValid
}

const handlePlaceOrder = async () => {
  emptyCartWarning.value = false

  // Kiểm tra giỏ hàng
  if (cartItems.value.length === 0) {
    emptyCartWarning.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }

  // Validate form - Báo đỏ bên dưới trường nào bị thiếu/sai
  if (!validateForm()) {
    setTimeout(() => {
      const firstErr = document.querySelector('.input-error')
      if (firstErr) {
        firstErr.scrollIntoView({ behavior: 'smooth', block: 'center' })
        firstErr.focus()
      }
    }, 100)
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

    const res = await api.post('/api/hoa-don/dat-hang-online', payload)
    const savedOrder = res.data
    createdOrder.value = savedOrder

    // Xóa giỏ hàng khi đơn hàng đã được khởi tạo
    clearCart()

    // Nếu chọn thanh toán qua cổng VNPAY -> Mở popup nhỏ & chuyển trang chính sang /don-hang như video
    if (paymentMethod.value === 'VNPAY') {
      const vnpPayload = {
        orderCode: savedOrder.maHoaDon,
        amount: finalTotal.value,
        orderInfo: `Thanh toan don hang ${savedOrder.maHoaDon}`
      }
      const vnpRes = await api.post('/api/vnpay/create-payment', vnpPayload)
      if (vnpRes.data && vnpRes.data.paymentUrl) {
        // Mở cửa sổ con (popup window) ở giữa màn hình
        const width = 850
        const height = 750
        const left = Math.max(0, (window.screen.width - width) / 2)
        const top = Math.max(0, (window.screen.height - height) / 2)
        window.open(
          vnpRes.data.paymentUrl,
          'VNPayPaymentPopup',
          `width=${width},height=${height},top=${top},left=${left},resizable=yes,scrollbars=yes,status=yes`
        )

        // Chuyển trang chính sang trang Đơn hàng kèm thông báo chờ thanh toán
        sessionStorage.setItem('vnpay_pending_toast', 'true')
        router.push(`/don-hang?code=${savedOrder.maHoaDon}`)
        return
      }
    }

    // Nếu là COD: Hiển thị ngay màn hình ĐẶT HÀNG THÀNH CÔNG chuyên nghiệp, rõ ràng
    orderSuccess.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (err) {
    console.error('Lỗi đặt hàng:', err)
    alert(err.response?.data?.message || 'Không thể hoàn tất đơn hàng. Vui lòng thử lại sau!')
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
      <!-- CẢNH BÁO GIỎ HÀNG TRỐNG -->
      <div v-if="emptyCartWarning" class="empty-cart-alert">
        <span class="alert-icon">⚠️</span>
        <div class="alert-content">
          <strong>Giỏ hàng của bạn đang trống!</strong>
          <p>Vui lòng thêm sản phẩm vào giỏ hàng trước khi xác nhận đặt hàng.</p>
        </div>
        <router-link to="/cua-hang" class="btn-shop-now-alert">Đến Cửa Hàng &rarr;</router-link>
      </div>

      <!-- MÀN HÌNH ĐẶT HÀNG THÀNH CÔNG -->
      <div v-if="orderSuccess" class="order-success-card">
        <div class="success-icon-wrap">
          <svg class="checkmark-svg" viewBox="0 0 52 52">
            <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none"/>
            <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
          </svg>
        </div>
        <h2 class="success-title">🎉 Đặt Hàng Thành Công!</h2>
        <p class="success-note">
          Cảm ơn quý khách <strong>{{ customerName }}</strong> đã tin tưởng mua sắm tại <strong>FF T-Shirt</strong>.<br />
          Đơn hàng của bạn đã được ghi nhận trên hệ thống và nhân viên sẽ liên hệ xác nhận trong thời gian sớm nhất.
        </p>

        <!-- Thẻ tóm tắt thông tin đơn hàng vừa đặt -->
        <div class="success-order-box">
          <div class="order-code-row">
            <div>
              <span class="label">Mã đơn hàng:</span>
              <strong class="code-val">#{{ createdOrder?.maHoaDon }}</strong>
            </div>
            <button class="btn-copy-code" @click="copyOrderCode(createdOrder?.maHoaDon)">
              {{ copySuccess ? '✓ Đã chép' : '📋 Sao chép mã' }}
            </button>
          </div>

          <div class="order-summary-details">
            <div class="sum-row">
              <span class="sum-label">👤 Người nhận:</span>
              <span class="sum-val font-semibold">{{ customerName }} • {{ customerPhone }}</span>
            </div>
            <div class="sum-row">
              <span class="sum-label">📧 Email:</span>
              <span class="sum-val">{{ customerEmail }}</span>
            </div>
            <div class="sum-row">
              <span class="sum-label">📍 Địa chỉ nhận hàng:</span>
              <span class="sum-val">{{ fullDeliveryAddress }}</span>
            </div>
            <div class="sum-row">
              <span class="sum-label">💳 Phương thức:</span>
              <span class="sum-val font-semibold">
                {{ paymentMethod === 'VNPAY' ? 'Cổng thanh toán VNPAY' : 'Thanh toán khi nhận hàng (COD)' }}
              </span>
            </div>
            <div class="sum-row">
              <span class="sum-label">💵 Tổng tiền thanh toán:</span>
              <strong class="sum-val text-price">{{ formatCurrency(finalTotal) }}</strong>
            </div>
          </div>

          <!-- Ghi chú nhận hàng COD -->
          <div v-if="paymentMethod === 'COD'" class="delivery-tip-box">
            🚚 <strong>Lưu ý nhận hàng:</strong> Đơn hàng sẽ được nhân viên FF T-Shirt gọi điện xác nhận và gửi tới bạn. Quý khách vui lòng kiểm tra sản phẩm và thanh toán tiền mặt trực tiếp cho shipper khi nhận hàng.
          </div>
        </div>

        <div class="success-actions">
          <router-link :to="`/don-hang?code=${createdOrder?.maHoaDon}`" class="btn-view-order">
            📄 Xem Chi Tiết Đơn Hàng
          </router-link>
          <router-link to="/cua-hang" class="btn-back-home">
            🛍️ Tiếp Tục Mua Sắm &rarr;
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
            :errors="errors"
            v-model:customer-name="customerName"
            v-model:customer-phone="customerPhone"
            v-model:customer-email="customerEmail"
            v-model:selected-province="selectedProvince"
            v-model:selected-ward="selectedWard"
            v-model:street-address="streetAddress"
            :full-delivery-address="fullDeliveryAddress"
            @selectSavedAddress="onSelectSavedAddress"
            @selectCustomAddress="selectCustomAddress"
            @clearError="clearFieldError"
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

/* Empty cart banner */
.empty-cart-alert {
  background: #fef2f2;
  border: 1.5px solid #fecaca;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  color: #991b1b;
}

.empty-cart-alert .alert-icon {
  font-size: 28px;
}

.empty-cart-alert .alert-content {
  flex: 1;
}

.empty-cart-alert .alert-content strong {
  display: block;
  font-size: 15px;
  margin-bottom: 2px;
}

.empty-cart-alert .alert-content p {
  margin: 0;
  font-size: 13px;
  color: #b91c1c;
}

.btn-shop-now-alert {
  background: #dc2626;
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}

/* Đặt hàng thành công */
.order-success-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 50px 30px;
  text-align: center;
  max-width: 650px;
  margin: 20px auto;
  box-shadow: 0 10px 30px rgba(73, 104, 131, 0.1);
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.success-icon-wrap {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkmark-svg {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: block;
  stroke-width: 3;
  stroke: #10b981;
  stroke-miterlimit: 10;
  animation: scaleCircle 0.3s ease-in-out both;
}

.checkmark-circle {
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  stroke-width: 3;
  stroke-miterlimit: 10;
  stroke: #10b981;
  fill: #ecfdf5;
  animation: strokeCircle 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

.checkmark-check {
  transform-origin: 50% 50%;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  stroke: #10b981;
  animation: strokeCheck 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.5s forwards;
}

@keyframes strokeCircle {
  100% { stroke-dashoffset: 0; }
}
@keyframes strokeCheck {
  100% { stroke-dashoffset: 0; }
}
@keyframes scaleCircle {
  0%, 100% { transform: none; }
  50% { transform: scale3d(1.1, 1.1, 1); }
}

.success-title {
  font-size: 26px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 10px;
}

.success-note {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.success-order-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 22px;
  margin: 0 auto 28px;
  text-align: left;
}

.order-code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 14px;
  border-bottom: 1.5px dashed #cbd5e1;
  margin-bottom: 14px;
}

.order-code-row .label {
  font-size: 13px;
  color: #64748b;
  margin-right: 8px;
}

.code-val {
  font-size: 20px;
  color: #e11d48;
  font-weight: 800;
}

.btn-copy-code {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  color: #334155;
  transition: all 0.2s;
}

.btn-copy-code:hover {
  background: #f1f5f9;
  border-color: #496883;
  color: #496883;
}

.order-summary-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13.5px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.4;
}

.sum-label {
  color: #64748b;
  font-weight: 600;
  flex-shrink: 0;
}

.sum-val {
  color: #1e293b;
  text-align: right;
}

.font-semibold {
  font-weight: 600;
}

.text-price {
  color: #dc2626;
  font-size: 17px;
  font-weight: 800;
}

.delivery-tip-box {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 12px 14px;
  margin-top: 16px;
  font-size: 12.5px;
  color: #166534;
  line-height: 1.5;
}

.delivery-tip-box strong {
  color: #15803d;
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
  padding: 13px 24px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: background 0.2s;
}

.btn-view-order:hover {
  background: #364f66;
}

.btn-back-home {
  background: #d2a764;
  color: #1e293b;
  padding: 13px 24px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: background 0.2s;
}

.btn-back-home:hover {
  background: #c3944b;
}

@media (max-width: 992px) {
  .checkout-split-layout {
    grid-template-columns: 1fr;
  }
}
</style>
