<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
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
import { formatCurrency } from '@/utils/dinhDang.js'
import { currentClientUser, isClientLoggedIn } from '@/utils/taiKhoanKhachHang.js'
import SoDiaChiGiaoHang from '@/components/ThanhToan/SoDiaChiGiaoHang.vue'
import PhuongThucThanhToan from '@/components/ThanhToan/PhuongThucThanhToan.vue'
import XemLaiDonHang from '@/components/ThanhToan/XemLaiDonHang.vue'

const router = useRouter()

// 1. Địa chỉ đã lưu (tải động theo tài khoản người dùng đăng nhập)
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const isCustomAddress = ref(false)

// Form người nhận
const customerName = ref('')
const customerPhone = ref('')
const customerEmail = ref('')
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
  if (addr.name) customerName.value = addr.name
  if (addr.phone) customerPhone.value = addr.phone
  selectedProvince.value = addr.province || ''
  selectedDistrict.value = addr.district || ''
  selectedWard.value = addr.ward || ''
  streetAddress.value = addr.street || ''
}

const selectCustomAddress = () => {
  selectedAddressId.value = null
  isCustomAddress.value = true
  streetAddress.value = ''
  selectedProvince.value = ''
  selectedDistrict.value = ''
  selectedWard.value = ''
}

const handleOpenLogin = () => {
  window.dispatchEvent(new CustomEvent('open-client-login'))
}

// Tải thông tin người nhận & danh sách địa chỉ đã lưu
const loadCustomerData = async () => {
  if (isClientLoggedIn.value && currentClientUser.value) {
    customerName.value = currentClientUser.value.name || ''
    customerPhone.value = currentClientUser.value.phone || ''
    customerEmail.value = currentClientUser.value.email || ''

    const customerId = currentClientUser.value.id
    if (customerId) {
      try {
        const res = await api.get(`/api/khach-hang/${customerId}/dia-chi`)
        const list = Array.isArray(res.data) ? res.data : []
        if (list && list.length > 0) {
          savedAddresses.value = list.map(item => ({
            id: item.id,
            name: currentClientUser.value.name || '',
            phone: currentClientUser.value.phone || '',
            address: [item.diaChiCuThe, item.phuong, item.huyen, item.thanhPho].filter(Boolean).join(', '),
            province: item.thanhPho || '',
            district: item.huyen || '',
            ward: item.phuong || '',
            street: item.diaChiCuThe || '',
            macDinh: !!item.macDinh
          }))

          const defaultAddr = savedAddresses.value.find(a => a.macDinh) || savedAddresses.value[0]
          if (defaultAddr) {
            onSelectSavedAddress(defaultAddr)
          }
          return
        }
      } catch (err) {
        console.warn('Không thể tải sổ địa chỉ khách hàng:', err)
      }
    }

    // Đã đăng nhập nhưng chưa có địa chỉ lưu
    savedAddresses.value = []
    selectedAddressId.value = null
    isCustomAddress.value = true
    selectedProvince.value = ''
    selectedDistrict.value = ''
    selectedWard.value = ''
    streetAddress.value = ''
  } else {
    // Khách hàng mua hàng khi không đăng nhập:
    // Để trống toàn bộ để khách tự nhập bên thông tin người nhận
    savedAddresses.value = []
    selectedAddressId.value = null
    isCustomAddress.value = true
    customerName.value = ''
    customerPhone.value = ''
    customerEmail.value = ''
    selectedProvince.value = ''
    selectedDistrict.value = ''
    selectedWard.value = ''
    streetAddress.value = ''
  }
}

onMounted(() => {
  loadCustomerData()
  loadVouchers()
})

onUnmounted(() => {
  stopPaymentPolling()
})

watch(isClientLoggedIn, () => {
  loadCustomerData()
})

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

// Quản lý Voucher & Khuyến mãi Online
const availableVouchers = ref([])
const showVoucherModal = ref(false)
const appliedVoucher = ref(getCheckoutVoucher())

const loadVouchers = async () => {
  try {
    const res = await api.get('/api/phieu-giam-gia')
    const list = Array.isArray(res.data) ? res.data : []
    const now = new Date()
    availableVouchers.value = list.filter(v => {
      if (v.trangThai != null && Number(v.trangThai) !== 1) return false
      if (v.soLuongSuDung != null && Number(v.soLuongSuDung) <= 0) return false
      if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false
      if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false
      return true
    })
  } catch (err) {
    console.warn('Lỗi tải danh sách voucher:', err)
  }
}

function isPercentVoucher(v) {
  if (!v) return false
  return v.loaiPhieuGiamGia === 1 || v.loaiPhieuGiamGia === '1' || v.loaiPhieuGiamGia === 'PHAN_TRAM' || v.loaiPhieuGiamGia === '%'
}

function calculateVoucherDiscount(v, subtotal) {
  if (!v || subtotal <= 0) return 0
  if (v.hoaDonToiThieu && subtotal < Number(v.hoaDonToiThieu)) return 0

  if (isPercentVoucher(v)) {
    const percent = Number(v.giaTriGiamGia || 0)
    let amount = (subtotal * percent) / 100
    if (v.giamToiDa && Number(v.giamToiDa) > 0 && amount > Number(v.giamToiDa)) {
      amount = Number(v.giamToiDa)
    }
    return Math.min(amount, subtotal)
  } else {
    return Math.min(Number(v.giaTriGiamGia || 0), subtotal)
  }
}

function formatVoucherDiscountLabel(v) {
  if (!v) return ''
  if (isPercentVoucher(v)) {
    let lbl = `${v.giaTriGiamGia}%`
    if (v.giamToiDa && Number(v.giamToiDa) > 0) {
      lbl += ` (tối đa ${formatCurrency(v.giamToiDa)})`
    }
    return lbl
  }
  return formatCurrency(v.giaTriGiamGia)
}

const voucherDiscount = computed(() => {
  if (!appliedVoucher.value) return 0
  if (appliedVoucher.value.discount != null && appliedVoucher.value.giaTriGiamGia == null) {
    return Math.min(appliedVoucher.value.discount, checkoutSubtotal.value)
  }
  return calculateVoucherDiscount(appliedVoucher.value, checkoutSubtotal.value)
})

const betterVoucherSuggestion = computed(() => {
  const subtotal = checkoutSubtotal.value
  if (!availableVouchers.value || availableVouchers.value.length === 0 || subtotal <= 0) return null
  const currentDiscount = voucherDiscount.value

  const candidates = availableVouchers.value.filter(v => {
    const minOrder = Number(v.hoaDonToiThieu || 0)
    if (minOrder <= subtotal) return false

    const potentialDiscount = calculateVoucherDiscount(v, minOrder)
    return potentialDiscount > currentDiscount
  })

  if (candidates.length === 0) return null

  candidates.sort((a, b) => {
    const diffA = Number(a.hoaDonToiThieu || 0) - subtotal
    const diffB = Number(b.hoaDonToiThieu || 0) - subtotal
    if (diffA !== diffB) return diffA - diffB
    const discA = calculateVoucherDiscount(a, Number(a.hoaDonToiThieu || 0))
    const discB = calculateVoucherDiscount(b, Number(b.hoaDonToiThieu || 0))
    return discB - discA
  })

  const bestCandidate = candidates[0]
  const neededAmount = Math.max(0, Number(bestCandidate.hoaDonToiThieu || 0) - subtotal)
  const potentialDiscount = calculateVoucherDiscount(bestCandidate, Number(bestCandidate.hoaDonToiThieu || 0))

  return {
    voucher: bestCandidate,
    code: bestCandidate.maPhieuGiamGia,
    neededAmount,
    potentialDiscount,
    discountLabel: formatVoucherDiscountLabel(bestCandidate)
  }
})

const selectVoucher = (v) => {
  if (checkoutSubtotal.value < Number(v.hoaDonToiThieu || 0)) {
    alert(`Đơn hàng chưa đạt mức tối thiểu ${formatCurrency(v.hoaDonToiThieu)} để áp dụng mã ${v.maPhieuGiamGia}!`)
    return
  }
  appliedVoucher.value = {
    id: v.id,
    code: v.maPhieuGiamGia,
    name: v.tenPhieuGiamGia,
    loaiPhieuGiamGia: v.loaiPhieuGiamGia,
    giaTriGiamGia: v.giaTriGiamGia,
    giamToiDa: v.giamToiDa,
    hoaDonToiThieu: v.hoaDonToiThieu,
    discount: calculateVoucherDiscount(v, checkoutSubtotal.value)
  }
  showVoucherModal.value = false
}

const removeAppliedVoucher = () => {
  appliedVoucher.value = null
  sessionStorage.removeItem('fftshirt_checkout_voucher')
}

// Tính tiền
const shippingFee = computed(() => {
  if (checkoutSubtotal.value >= 499000 || checkoutSubtotal.value === 0 || appliedVoucher.value?.code === 'FREESHIP') return 0
  return 30000
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
    router.push(`/tra-cuu?code=${createdOrder.value.maHoaDon}`)
  } else {
    router.push('/tra-cuu')
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
    errors.value.customerPhone = 'Số điện thoại không hợp lệ (gồm 10 chữ số, ví dụ: 0988776655)'
    isValid = false
  }

  // 3. Kiểm tra email
  const email = (customerEmail.value || '').trim()
  const emailRegex = /^[^s@]+@[^s@]+.[^s@]+$/
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

// Modal Xác nhận đơn hàng trước khi hoàn tất
const showClientConfirmModal = ref(false)

// Modals Thanh toán Online (VietQR, MoMo)
const showVietQrModal = ref(false)
const showMoMoModal = ref(false)
const qrOrderCode = ref('')
const qrAmount = ref(0)
const qrCodeUrl = ref('')
const momoPayUrl = ref('')
let paymentPollTimer = null

const stopPaymentPolling = () => {
  if (paymentPollTimer) {
    clearInterval(paymentPollTimer)
    paymentPollTimer = null
  }
}

const startPaymentPolling = (maHoaDon) => {
  stopPaymentPolling()
  paymentPollTimer = setInterval(async () => {
    try {
      const res = await api.get(`/api/payment/check-status/${maHoaDon}`)
      if (res.data && res.data.daThanhToan) {
        stopPaymentPolling()
        showVietQrModal.value = false
        showMoMoModal.value = false
        orderSuccess.value = true
      }
    } catch (e) {}
  }, 3000)
}

const handleConfirmTransferManual = () => {
  stopPaymentPolling()
  showVietQrModal.value = false
  showMoMoModal.value = false
  orderSuccess.value = true
}

const handleOpenConfirmModal = () => {
  emptyCartWarning.value = false

  if (activeCheckoutItems.value.length === 0) {
    emptyCartWarning.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }

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

  const outOfStockItems = activeCheckoutItems.value.filter(i => (i.maxStock != null ? i.maxStock : 99) <= 0)
  if (outOfStockItems.length > 0) {
    alert(`Sản phẩm "${outOfStockItems[0].productName}" hiện đã hết hàng trong kho. Vui lòng quay lại giỏ hàng để cập nhật!`)
    return
  }

  showClientConfirmModal.value = true
}

const handleConfirmPlaceOrder = async () => {
  if (submitting.value) return
  submitting.value = true

  try {
    const checkoutAmount = Math.round(Number(finalTotal.value))
    const purchasedIds = activeCheckoutItems.value.map(i => i.variantId)

    const payload = {
      idKhachHang: isClientLoggedIn.value && currentClientUser.value?.id ? currentClientUser.value.id : null,
      tenKhachHang: customerName.value.trim(),
      soDienThoai: customerPhone.value.trim(),
      email: customerEmail.value.trim(),
      diaChiNhanHang: fullDeliveryAddress.value,
      ghiChu: orderNote.value.trim(),
      phuongThucThanhToan: paymentMethod.value,
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
    showClientConfirmModal.value = false

    // Dọn dẹp giỏ hàng
    removeMultipleFromCart(purchasedIds)
    sessionStorage.removeItem('fftshirt_checkout_items')
    sessionStorage.removeItem('fftshirt_checkout_voucher')

    // 1. Cổng VNPAY (Chuyển khoản / Quét QR)
    if (paymentMethod.value === 'VNPAY') {
      const vnpAmount = Math.max(1, Math.round(Number(savedOrder?.tienSauGiamGia || savedOrder?.tongTien || checkoutAmount)))
      const vnpPayload = {
        orderCode: savedOrder.maHoaDon,
        amount: vnpAmount,
        orderInfo: `Thanh toan don hang ${savedOrder.maHoaDon}`
      }
      const vnpRes = await api.post('/api/vnpay/create-payment', vnpPayload)
      if (vnpRes.data && vnpRes.data.paymentUrl) {
        const width = 850
        const height = 750
        const left = Math.max(0, (window.screen.width - width) / 2)
        const top = Math.max(0, (window.screen.height - height) / 2)
        window.open(
          vnpRes.data.paymentUrl,
          'VNPayPaymentPopup',
          `width=${width},height=${height},top=${top},left=${left},resizable=yes,scrollbars=yes,status=yes`
        )
        sessionStorage.setItem('vnpay_pending_toast', 'true')
        router.push(`/tra-cuu?code=${savedOrder.maHoaDon}`)
        return
      }
    }

    // 2. Mặc định COD
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
            :is-logged-in="isClientLoggedIn"
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
            @openLoginModal="handleOpenLogin"
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
          :applied-voucher="appliedVoucher"
          :voucher-suggestion="betterVoucherSuggestion"
          :final-total="finalTotal"
          v-model:order-note="orderNote"
          :submitting="submitting"
          @placeOrder="handleOpenConfirmModal"
          @openVoucherModal="showVoucherModal = true"
          @removeVoucher="removeAppliedVoucher"
        />
      </div>
    </div>

    <!-- ==========================================================
         MODAL 1: CHỌN MÃ GIẢM GIÁ (VOUCHER)
    ========================================================== -->
    <transition name="modal-fade">
      <div v-if="showVoucherModal" class="order-modal-backdrop" @click.self="showVoucherModal = false">
        <div class="voucher-modal-dialog">
          <div class="vmodal-header">
            <h3 class="vmodal-title">🎟️ Chọn Phiếu Giảm Giá</h3>
            <button class="modal-close-x" @click="showVoucherModal = false">&times;</button>
          </div>

          <div class="vmodal-body">
            <div v-if="availableVouchers.length === 0" class="vmodal-empty">
              <span>Hiện tại chưa có mã giảm giá nào đang áp dụng.</span>
            </div>

            <div v-else class="vmodal-list">
              <div
                v-for="v in availableVouchers"
                :key="v.id"
                class="vmodal-item"
                :class="{
                  'is-active': appliedVoucher && (appliedVoucher.id === v.id || appliedVoucher.code === v.maPhieuGiamGia),
                  'is-disabled': checkoutSubtotal < Number(v.hoaDonToiThieu || 0)
                }"
              >
                <div class="vitem-left">
                  <div class="vitem-tag">{{ isPercentVoucher(v) ? '%' : 'VND' }}</div>
                  <div class="vitem-info">
                    <div class="vitem-code-row">
                      <strong class="vitem-code">{{ v.maPhieuGiamGia }}</strong>
                      <span class="vitem-discount-badge">Giảm {{ formatVoucherDiscountLabel(v) }}</span>
                    </div>
                    <div class="vitem-name">{{ v.tenPhieuGiamGia }}</div>
                    <div class="vitem-condition">
                      Đơn tối thiểu: <strong>{{ formatCurrency(v.hoaDonToiThieu || 0) }}</strong>
                    </div>
                  </div>
                </div>

                <div class="vitem-right">
                  <button
                    v-if="appliedVoucher && (appliedVoucher.id === v.id || appliedVoucher.code === v.maPhieuGiamGia)"
                    type="button"
                    class="btn-vitem-applied"
                    disabled
                  >
                    ✓ Đang dùng
                  </button>
                  <button
                    v-else-if="checkoutSubtotal >= Number(v.hoaDonToiThieu || 0)"
                    type="button"
                    class="btn-vitem-apply"
                    @click="selectVoucher(v)"
                  >
                    Áp dụng
                  </button>
                  <span v-else class="vitem-not-eligible">
                    Thiếu {{ formatCurrency(Number(v.hoaDonToiThieu || 0) - checkoutSubtotal) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- ==========================================================
         MODAL 2: BƯỚC XÁC NHẬN ĐƠN HÀNG TRƯỚC KHI ĐẶT
    ========================================================== -->
    <transition name="modal-fade">
      <div v-if="showClientConfirmModal" class="order-modal-backdrop" @click.self="showClientConfirmModal = false">
        <div class="confirm-modal-dialog">
          <div class="cmodal-header">
            <h3 class="cmodal-title">📋 Xác Nhận Thông Tin Đơn Hàng</h3>
            <button class="modal-close-x" @click="showClientConfirmModal = false">&times;</button>
          </div>

          <div class="cmodal-body">
            <!-- Thông tin khách hàng & Giao hàng -->
            <div class="cmodal-section">
              <h4 class="csection-title">📍 Địa chỉ nhận hàng</h4>
              <div class="cinfo-card">
                <div><strong>Người nhận:</strong> {{ customerName }} • {{ customerPhone }}</div>
                <div v-if="customerEmail"><strong>Email:</strong> {{ customerEmail }}</div>
                <div><strong>Địa chỉ:</strong> {{ fullDeliveryAddress }}</div>
                <div v-if="orderNote"><strong>Ghi chú:</strong> {{ orderNote }}</div>
              </div>
            </div>

            <!-- Danh sách sản phẩm -->
            <div class="cmodal-section">
              <h4 class="csection-title">🛍️ Sản phẩm đặt mua ({{ activeCheckoutItems.length }})</h4>
              <div class="citems-list">
                <div v-for="item in activeCheckoutItems" :key="item.variantId" class="citem-row">
                  <img :src="item.image || '/images/products/default_tshirt.jpg'" class="citem-thumb" />
                  <div class="citem-info">
                    <div class="citem-name">{{ item.productName }}</div>
                    <div class="citem-meta">{{ item.color }} / Size {{ item.size }} × {{ item.quantity }}</div>
                  </div>
                  <div class="citem-price">
                    {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Phương thức thanh toán & Tổng tiền -->
            <div class="cmodal-section">
              <h4 class="csection-title">💳 Thanh toán</h4>
              <div class="cpricing-box">
                <div class="cp-row">
                  <span>Phương thức:</span>
                  <strong class="text-primary font-semibold">
                    {{ paymentMethod === 'COD' ? 'Thanh toán tiền mặt khi nhận hàng (COD)' : 'Cổng thanh toán VNPAY (Chuyển khoản / Quét QR)' }}
                  </strong>
                </div>
                <div class="cp-row">
                  <span>Tạm tính:</span>
                  <span>{{ formatCurrency(checkoutSubtotal) }}</span>
                </div>
                <div class="cp-row">
                  <span>Phí vận chuyển:</span>
                  <span>{{ shippingFee === 0 ? 'MIỄN PHÍ' : formatCurrency(shippingFee) }}</span>
                </div>
                <div v-if="voucherDiscount > 0" class="cp-row text-success">
                  <span>Voucher giảm giá ({{ appliedVoucher?.code }}):</span>
                  <span>-{{ formatCurrency(voucherDiscount) }}</span>
                </div>
                <div class="cp-divider"></div>
                <div class="cp-row cp-total">
                  <span>TỔNG THANH TOÁN:</span>
                  <strong class="total-highlight">{{ formatCurrency(finalTotal) }}</strong>
                </div>
              </div>
            </div>
          </div>

          <div class="cmodal-footer">
            <button
              type="button"
              class="btn-cmodal-cancel"
              :disabled="submitting"
              @click="showClientConfirmModal = false"
            >
              ← Quay lại
            </button>
            <button
              type="button"
              class="btn-cmodal-confirm"
              :disabled="submitting"
              @click="handleConfirmPlaceOrder"
            >
              {{ submitting ? 'Đang hoàn tất đơn hàng...' : '✓ Xác Nhận & Đặt Hàng' }}
            </button>
          </div>
        </div>
      </div>
    </transition>


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

/* ==========================================================
   MODAL STYLES: VOUCHER, CONFIRM, VIETQR, MOMO
========================================================== */
.voucher-modal-dialog,
.confirm-modal-dialog,
.qr-modal-dialog {
  background: #ffffff;
  border-radius: 16px;
  width: 90%;
  max-width: 580px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.confirm-modal-dialog {
  max-width: 640px;
}

.vmodal-header,
.cmodal-header,
.qrmodal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.momo-header {
  background: #a50064;
  color: #ffffff;
}
.momo-header .qrmodal-title {
  color: #ffffff;
}
.momo-header .modal-close-x {
  color: #ffffff;
}

.vmodal-title,
.cmodal-title,
.qrmodal-title {
  font-size: 17px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.vmodal-body,
.cmodal-body,
.qrmodal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vmodal-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.vmodal-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  transition: all 0.2s;
}

.vmodal-item.is-active {
  border-color: #22c55e;
  background: #f0fdf4;
}

.vmodal-item.is-disabled {
  opacity: 0.6;
  background: #f8fafc;
}

.vitem-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vitem-tag {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #fef3c7;
  color: #b45309;
  font-weight: 800;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vitem-code-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.vitem-code {
  font-size: 14px;
  color: #0f172a;
}

.vitem-discount-badge {
  font-size: 11px;
  font-weight: 700;
  background: #fee2e2;
  color: #dc2626;
  padding: 2px 6px;
  border-radius: 4px;
}

.vitem-name {
  font-size: 12px;
  color: #475569;
}

.vitem-condition {
  font-size: 11px;
  color: #64748b;
}

.btn-vitem-apply {
  background: #1e293b;
  color: #ecd8af;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}

.btn-vitem-applied {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
}

.vitem-not-eligible {
  font-size: 11px;
  color: #94a3b8;
}

.csection-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.cinfo-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 13px;
  color: #334155;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.citems-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 180px;
  overflow-y: auto;
}

.citem-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px solid #f1f5f9;
}

.citem-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  object-fit: cover;
}

.citem-info {
  flex: 1;
}

.citem-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.citem-meta {
  font-size: 11.5px;
  color: #64748b;
}

.citem-price {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.cpricing-box {
  background: #fafaf9;
  border: 1px solid #e2d9cc;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.cp-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cp-divider {
  height: 1px;
  background: #e2d9cc;
  margin: 4px 0;
}

.cp-total {
  font-weight: 800;
  font-size: 15px;
}

.total-highlight {
  color: #dc2626;
  font-size: 17px;
}

.cmodal-footer,
.qrmodal-footer {
  padding: 14px 20px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cmodal-cancel {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 10px 18px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.btn-cmodal-confirm {
  background: #d2a764;
  color: #1e293b;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(210, 167, 100, 0.4);
}

.btn-cmodal-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.qr-instruction {
  text-align: center;
  font-size: 13.5px;
  color: #475569;
  margin: 0;
}

.qr-image-wrapper {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.momo-border {
  border-color: #fbcfe8;
  background: #fff1f2;
}

.qr-img {
  width: 210px;
  height: 210px;
  object-fit: contain;
}

.qr-details-table {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.qrd-row {
  display: flex;
  justify-content: space-between;
}

.code-val {
  font-family: monospace;
  background: #e2e8f0;
  padding: 1px 6px;
  border-radius: 4px;
}

.qr-waiting-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 14px;
  color: #1d4ed8;
  font-size: 12.5px;
  font-weight: 600;
}

.spinner-mini {
  width: 16px;
  height: 16px;
  border: 2px solid #bfdbfe;
  border-top-color: #1d4ed8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn-qr-done {
  width: 100%;
  background: #16a34a;
  color: #ffffff;
  border: none;
  padding: 12px 0;
  border-radius: 8px;
  font-weight: 800;
  font-size: 14.5px;
  cursor: pointer;
}

.btn-momo-open {
  display: block;
  text-align: center;
  background: #a50064;
  color: #ffffff;
  padding: 10px 0;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 13.5px;
}
</style>
