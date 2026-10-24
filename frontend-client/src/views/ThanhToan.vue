<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import {
  cartItems,
  cartSubtotal,
  clearCart,
  getCheckoutItems,
  getCheckoutVoucher,
  removeMultipleFromCart
} from '@/utils/luuTruGioHang.js'
import { currentClientUser, isClientLoggedIn } from '@/utils/taiKhoanKhachHang.js'
import { formatCurrency } from '@/utils/dinhDang.js'
import { showAppAlert } from '@/utils/thongBaoDialog.js'
import SoDiaChiGiaoHang from '@/components/ThanhToan/SoDiaChiGiaoHang.vue'
import PhuongThucThanhToan from '@/components/ThanhToan/PhuongThucThanhToan.vue'
import XemLaiDonHang from '@/components/ThanhToan/XemLaiDonHang.vue'

const router = useRouter()

// 1. Địa chỉ đã lưu (Khách hàng tự điền địa chỉ thật hoặc lấy từ tài khoản)
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const isCustomAddress = ref(true)

// Form người nhận (mặc định lấy theo tài khoản đăng nhập nếu có, ngược lại để trống để khách hàng tự điền)
const customerName = ref(currentClientUser.value?.name || '')
const customerPhone = ref(currentClientUser.value?.phone || '')
const customerEmail = ref(currentClientUser.value?.email || '')
const selectedProvince = ref('')
const selectedDistrict = ref('')
const selectedWard = ref('')
const streetAddress = ref('')
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

// Danh sách sản phẩm thanh toán (hỗ trợ chọn lọc từ giỏ hàng hoặc mặc định toàn bộ)
const activeCheckoutItems = computed(() => {
  const custom = getCheckoutItems()
  if (custom && custom.length > 0) return custom
  return cartItems.value
})

const checkoutSubtotal = computed(() => {
  return activeCheckoutItems.value.reduce((sum, item) => {
    const p = item.discountPrice != null && item.discountPrice > 0 ? item.discountPrice : item.price
    return sum + p * item.quantity
  }, 0)
})

const appliedVoucher = ref(getCheckoutVoucher())

const voucherDiscount = computed(() => {
  if (!appliedVoucher.value) return 0
  return Math.min(appliedVoucher.value.discount || 0, checkoutSubtotal.value)
})

// Dịch vụ GHN Express tính phí và thời gian giao hàng tự động
const calculatedGhnFee = ref(30000)
const ghnLeadTime = ref('')
const ghnLoading = ref(false)

const updateGhnShipping = async () => {
  if (!selectedProvince.value) return
  ghnLoading.value = true
  try {
    const feeRes = await api.post('/api/ghn/fee', {
      toProvinceName: selectedProvince.value,
      toWardCode: selectedWard.value || '',
      weight: Math.max(200, activeCheckoutItems.value.length * 200),
      insuranceValue: checkoutSubtotal.value
    })
    if (feeRes.data?.total != null) {
      calculatedGhnFee.value = Number(feeRes.data.total)
    }
  } catch (e) {
    console.warn('GHN fee fallback:', e)
  }

  try {
    const ltRes = await api.post('/api/ghn/leadtime', {
      toProvinceName: selectedProvince.value,
      toWardCode: selectedWard.value || ''
    })
    if (ltRes.data?.formattedDate) {
      ghnLeadTime.value = ltRes.data.formattedDate
    }
  } catch (e) {
    console.warn('GHN leadtime fallback:', e)
  } finally {
    ghnLoading.value = false
  }
}

watch([selectedProvince, selectedWard], () => {
  updateGhnShipping()
})

onMounted(() => {
  if (selectedProvince.value) {
    updateGhnShipping()
  }
})

// Tính tiền
const shippingFee = computed(() => {
  if (checkoutSubtotal.value >= 499000 || checkoutSubtotal.value === 0 || appliedVoucher.value?.code === 'FREESHIP') return 0
  return calculatedGhnFee.value || 30000
})

const finalTotal = computed(() => {
  return Math.max(0, checkoutSubtotal.value - voucherDiscount.value + shippingFee.value)
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

const goToOrderDetail = () => {
  orderSuccess.value = false
  if (createdOrder.value?.maHoaDon) {
    router.push(`/don-hang?code=${createdOrder.value.maHoaDon}`)
  } else {
    router.push('/don-hang')
  }
}

const goToShop = () => {
  orderSuccess.value = false
  router.push('/cua-hang')
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
    errors.value.customerPhone = 'Số điện thoại không hợp lệ (gồm 10 chữ số)'
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
  if (activeCheckoutItems.value.length === 0) {
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

  // Kiểm tra an toàn: nếu có sản phẩm hết hàng thì chặn đặt hàng
  const outOfStockItems = activeCheckoutItems.value.filter(i => (i.maxStock != null ? i.maxStock : 99) <= 0)
  if (outOfStockItems.length > 0) {
    await showAppAlert(`Sản phẩm "${outOfStockItems[0].productName}" hiện đã hết hàng trong kho. Vui lòng quay lại giỏ hàng để cập nhật!`, 'Sản phẩm hết hàng', 'warning')
    return
  }

  submitting.value = true
  try {
    const checkoutAmount = Math.round(Number(finalTotal.value))
    const purchasedIds = activeCheckoutItems.value.map(i => i.variantId)

    const payload = {
      tenKhachHang: customerName.value.trim(),
      soDienThoai: customerPhone.value.trim(),
      email: customerEmail.value.trim(),
      diaChiNhanHang: fullDeliveryAddress.value,
      ghiChu: orderNote.value.trim(),
      phuongThucThanhToan: paymentMethod.value,
      donViVanChuyen: 'Giao Hàng Nhanh (GHN)',
      tongTien: checkoutAmount,
      phiVanChuyen: shippingFee.value,
      giamGia: voucherDiscount.value,
      chiTiet: activeCheckoutItems.value.map(i => ({
        idChiTietSanPham: i.variantId,
        soLuong: i.quantity,
        donGia: i.discountPrice > 0 ? i.discountPrice : i.price
      }))
    }

    const res = await api.post('/api/hoa-don/dat-hang-online', payload)
    const savedOrder = res.data
    createdOrder.value = savedOrder

    // Nếu chọn thanh toán qua cổng VNPAY -> Mở popup nhỏ & chuyển trang chính sang /don-hang
    if (paymentMethod.value === 'VNPAY') {
      const vnpAmount = Math.max(1, Math.round(Number(savedOrder?.tienSauGiamGia || savedOrder?.tongTien || checkoutAmount)))
      const vnpPayload = {
        orderCode: savedOrder.maHoaDon,
        amount: vnpAmount,
        orderInfo: `Thanh toan don hang ${savedOrder.maHoaDon}`
      }
      const vnpRes = await api.post('/api/vnpay/create-payment', vnpPayload)
      if (vnpRes.data && vnpRes.data.paymentUrl) {
        // Chỉ xóa giỏ hàng khi tạo liên kết thanh toán VNPAY thành công
        removeMultipleFromCart(purchasedIds)
        sessionStorage.removeItem('fftshirt_checkout_items')
        sessionStorage.removeItem('fftshirt_checkout_voucher')

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

    // Nếu là COD: Xóa giỏ hàng và hiển thị ngay màn hình ĐẶT HÀNG THÀNH CÔNG chuyên nghiệp
    removeMultipleFromCart(purchasedIds)
    sessionStorage.removeItem('fftshirt_checkout_items')
    sessionStorage.removeItem('fftshirt_checkout_voucher')
    orderSuccess.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (err) {
    console.error('Lỗi đặt hàng:', err)
    await showAppAlert(err.response?.data?.message || 'Không thể hoàn tất đơn hàng. Vui lòng thử lại sau!', 'Lỗi đặt hàng', 'error')
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

      <!-- BẢNG THÔNG BÁO (POPUP MODAL) ĐẶT HÀNG THÀNH CÔNG -->
      <transition name="modal-fade">
        <div
          v-if="orderSuccess"
          class="order-modal-backdrop"
          @click.self="goToOrderDetail"
        >
          <div class="order-modal-dialog">
            <button class="modal-close-x" @click="goToOrderDetail" title="Đóng">&times;</button>

            <!-- Huy hiệu tích xanh animated -->
            <div class="success-icon-wrap">
              <svg class="checkmark-svg" viewBox="0 0 52 52">
                <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none"/>
                <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
              </svg>
            </div>

            <h2 class="modal-success-title">🎉 Đặt Hàng Thành Công!</h2>
            <p class="modal-success-subtitle">
              Cảm ơn quý khách <strong>{{ customerName }}</strong> đã mua sắm tại <strong>FF T-Shirt</strong>.<br />
              Đơn hàng của bạn đã được ghi nhận trên hệ thống và nhân viên sẽ liên hệ xác nhận trong thời gian sớm nhất.
            </p>

            <!-- Thẻ tóm tắt thông tin đơn hàng vừa đặt -->
            <div class="modal-summary-box">
              <div class="modal-code-row">
                <div>
                  <span class="label">Mã đơn hàng:</span>
                  <strong class="code-val">#{{ createdOrder?.maHoaDon }}</strong>
                </div>
                <button class="btn-copy-mini" @click="copyOrderCode(createdOrder?.maHoaDon)">
                  {{ copySuccess ? '✓ Đã chép' : '📋 Sao chép mã' }}
                </button>
              </div>

              <div class="modal-summary-details">
                <div class="sum-row">
                  <span class="sum-label">👤 Người nhận:</span>
                  <span class="sum-val font-semibold">{{ customerName }} • {{ customerPhone }}</span>
                </div>
                <div class="sum-row" v-if="customerEmail">
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
                <div class="sum-row sum-total-row">
                  <span class="sum-label">💵 Tổng tiền thanh toán:</span>
                  <strong class="sum-val text-price">{{ formatCurrency(finalTotal) }}</strong>
                </div>
              </div>

              <!-- Ghi chú nhận hàng COD -->
              <div v-if="paymentMethod === 'COD'" class="modal-delivery-tip">
                🚚 <strong>Lưu ý nhận hàng:</strong> Đơn hàng sẽ được nhân viên FF T-Shirt gọi điện xác nhận và gửi tới bạn. Quý khách vui lòng kiểm tra sản phẩm và thanh toán tiền mặt trực tiếp cho shipper khi nhận hàng.
              </div>
            </div>

            <div class="modal-action-buttons">
              <button class="btn-modal-primary" @click="goToOrderDetail">
                📄 Xem Chi Tiết Đơn Hàng
              </button>
              <button class="btn-modal-secondary" @click="goToShop">
                🛍️ Tiếp Tục Mua Sắm &rarr;
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- FORM THANH TOÁN CHÍNH -->
      <div class="checkout-split-layout">
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
          :cart-items="activeCheckoutItems"
          :cart-subtotal="checkoutSubtotal"
          :shipping-fee="shippingFee"
          :voucher-discount="voucherDiscount"
          :final-total="finalTotal"
          :ghn-lead-time="ghnLeadTime"
          :ghn-loading="ghnLoading"
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
  max-width: 1440px;
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

/* =========================================================
   BẢNG THÔNG BÁO (POPUP MODAL) ĐẶT HÀNG THÀNH CÔNG
========================================================= */
.order-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  overflow-y: auto;
}

.order-modal-dialog {
  background: #ffffff;
  border-radius: 20px;
  max-width: 540px;
  width: 100%;
  padding: 32px 26px 26px;
  box-shadow: 0 25px 60px rgba(73, 104, 131, 0.25), 0 0 0 1px rgba(0, 0, 0, 0.04);
  position: relative;
  text-align: center;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-close-x {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 34px;
  height: 34px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: all 0.2s ease;
}

.modal-close-x:hover {
  background: #e2e8f0;
  color: #1e293b;
}

/* Huy hiệu tích xanh */
.success-icon-wrap {
  width: 68px;
  height: 68px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkmark-svg {
  width: 64px;
  height: 64px;
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

.modal-success-title {
  font-family: 'Be Vietnam Pro', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 24px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 8px;
  letter-spacing: -0.01em;
}

.modal-success-subtitle {
  color: #64748b;
  font-size: 13.5px;
  line-height: 1.55;
  margin: 0 0 20px;
}

.modal-summary-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  margin: 0 auto 22px;
  text-align: left;
}

.modal-code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1.5px dashed #cbd5e1;
  margin-bottom: 12px;
}

.modal-code-row .label {
  font-size: 13px;
  color: #64748b;
  margin-right: 6px;
}

.modal-code-row .code-val {
  font-size: 19px;
  color: #496883;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.btn-copy-mini {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  color: #334155;
  transition: all 0.2s;
}

.btn-copy-mini:hover {
  background: #f1f5f9;
  border-color: #496883;
  color: #496883;
}

.modal-summary-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  line-height: 1.45;
}

.sum-label {
  color: #64748b;
  font-weight: 600;
  flex-shrink: 0;
}

.sum-val {
  color: #1e293b;
  text-align: right;
  word-break: break-word;
}

.sum-total-row {
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
  margin-top: 4px;
}

.font-semibold {
  font-weight: 600;
}

.text-price {
  color: #496883;
  font-size: 17px;
  font-weight: 800;
}

.modal-delivery-tip {
  background: #fdfaf3;
  border: 1px solid #ecd8af;
  border-radius: 8px;
  padding: 10px 12px;
  margin-top: 14px;
  font-size: 12px;
  color: #78350f;
  line-height: 1.5;
}

.modal-delivery-tip strong {
  color: #92400e;
}

.modal-action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-modal-primary {
  flex: 1;
  min-width: 200px;
  background: #496883;
  color: #ffffff;
  padding: 12px 18px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13.5px;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.btn-modal-primary:hover {
  background: #364f66;
  transform: translateY(-1px);
}

.btn-modal-secondary {
  flex: 1;
  min-width: 170px;
  background: #fdfaf3;
  color: #d2a764;
  border: 1.5px solid #d2a764;
  padding: 12px 18px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13.5px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.btn-modal-secondary:hover {
  background: #d2a764;
  color: #ffffff;
  transform: translateY(-1px);
}

/* Modal Transition */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .order-modal-dialog {
  animation: popIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.modal-fade-leave-active .order-modal-dialog {
  animation: popIn 0.2s cubic-bezier(0.16, 1, 0.3, 1) reverse;
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(14px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@media (max-width: 992px) {
  .checkout-split-layout {
    grid-template-columns: 1fr;
  }
}
</style>
