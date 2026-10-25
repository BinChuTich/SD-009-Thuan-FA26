<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import {
  cartItems,
  addToCart,
  updateCartQuantity,
  removeFromCart,
  removeMultipleFromCart,
  clearCart,
  saveCart
} from '@/utils/luuTruGioHang.js'
import { getMergedProducts } from '@/utils/danhMucSanPham.js'
import { showAppConfirm, showAppAlert, showAppToast } from '@/utils/thongBaoDialog.js'
import { FALLBACK_PROVINCES, loadAdministrativeData, getWardsForProvince } from '@/utils/diaGioiHanhChinh.js'
import ModalDangNhap from '@/components/BocCuc/ModalDangNhap.vue'
import TheSanPham from '@/components/DungChung/TheSanPham.vue'
import { currentClientUser, isClientLoggedIn, loginClient } from '@/utils/taiKhoanKhachHang.js'
import { notifyOrderSuccess, fetchNotifications } from '@/utils/thongBao.js'

const router = useRouter()

// ==========================================
// 1. QUẢN LÝ ĐĂNG NHẬP / MODAL TÀI KHOẢN
// ==========================================
const showLoginModal = ref(false)
const authInitialView = ref('login')

const openLogin = () => {
  authInitialView.value = 'login'
  showLoginModal.value = true
}

const openGoogleLogin = () => {
  authInitialView.value = 'google'
  showLoginModal.value = true
}

const handleLoginSuccess = (userData) => {
  if (userData) {
    if (!customerName.value) customerName.value = userData.name || ''
    if (!customerPhone.value) customerPhone.value = userData.phone || ''
    if (!customerEmail.value) customerEmail.value = userData.email || ''
  }
}

// ==========================================
// 2. FORM THÔNG TIN NGƯỜI NHẬN & ĐỊA CHỈ 2 CẤP MỚI
// ==========================================
const customerSalutation = ref('Anh')
const customerName = ref(currentClientUser.value?.name || '')
const customerPhone = ref(currentClientUser.value?.phone || '')
const customerEmail = ref(currentClientUser.value?.email || '')
const selectedProvince = ref('')
const selectedWard = ref('')
const streetAddress = ref('')

const hasOtherReceiver = ref(false)
const otherReceiverGender = ref('Nam')
const otherReceiverName = ref('')
const otherReceiverPhone = ref('')

const hasOrderNote = ref(false)
const orderNote = ref('')

const hasVatInvoice = ref(false)
const vatCompanyName = ref('')
const vatTaxCode = ref('')
const vatCompanyAddress = ref('')

const agreedTerms = ref(true)

const provincesList = ref(FALLBACK_PROVINCES.map(name => ({ name })))
const currentWards = ref([])

const errors = ref({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  selectedProvince: '',
  selectedWard: '',
  streetAddress: ''
})

const clearError = (field) => {
  if (errors.value[field]) {
    errors.value[field] = ''
  }
}

const reloadWards = async (provName) => {
  if (!provName) {
    currentWards.value = []
    selectedWard.value = ''
    return
  }
  const wards = await getWardsForProvince(provName)
  currentWards.value = wards
  // Không tự động chọn phường/xã, để khách hàng tự chọn
  if (selectedWard.value && !wards.some(w => w.name === selectedWard.value)) {
    selectedWard.value = ''
  }
}

const onProvinceChange = async () => {
  clearError('selectedProvince')
  if (selectedProvince.value) {
    await reloadWards(selectedProvince.value)
  }
  updateGhnShipping()
}

const fullDeliveryAddress = computed(() => {
  const parts = [
    streetAddress.value,
    selectedWard.value,
    selectedProvince.value
  ].filter(Boolean)
  return parts.join(', ') || 'Chưa có địa chỉ cụ thể'
})

// ==========================================
// 3. PHƯƠNG THỨC THANH TOÁN (COD & VNPAY)
// KHÔNG CÓ MOMO & SHOPEEPAY, KHÔNG CÓ ĐỔI TRẢ
// ==========================================
const paymentMethod = ref('COD') // 'COD' | 'VNPAY'

// ==========================================
// 4. QUẢN LÝ SẢN PHẨM TRONG GIỎ HÀNG
// ==========================================
const selectedItemIds = ref(cartItems.value.map(i => i.variantId))

watch(cartItems, (newItems) => {
  // Tự động giữ các ID hợp lệ
  const validIds = new Set(newItems.map(i => i.variantId))
  selectedItemIds.value = selectedItemIds.value.filter(id => validIds.has(id))
  if (selectedItemIds.value.length === 0 && newItems.length > 0) {
    selectedItemIds.value = newItems.map(i => i.variantId)
  }
}, { deep: true })

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

const activeCheckoutItems = computed(() => {
  return cartItems.value.filter(item => selectedItemIds.value.includes(item.variantId))
})

const selectedSubtotal = computed(() => {
  return activeCheckoutItems.value.reduce((sum, item) => {
    const p = item.discountPrice > 0 ? item.discountPrice : item.price
    return sum + p * item.quantity
  }, 0)
})

const freeshipThreshold = 250000 // Mua từ 250.000đ được Freeship (Ảnh 2)

const remainingForFreeship = computed(() => {
  return Math.max(0, freeshipThreshold - selectedSubtotal.value)
})

const freeshipProgressPercent = computed(() => {
  if (selectedSubtotal.value <= 0) return 0
  return Math.min(100, Math.round((selectedSubtotal.value / freeshipThreshold) * 100))
})

const handleQtyChange = (variantId, delta, currentQty, maxStock) => {
  const limit = maxStock != null ? Number(maxStock) : 99
  if (limit <= 0 && delta > 0) return
  const newQty = currentQty + delta
  if (newQty <= 0) {
    handleRemove(variantId)
    return
  }
  if (limit > 0 && newQty > limit) return
  updateCartQuantity(variantId, newQty)
}

const handleRemove = async (variantId, name = '') => {
  const ok = await showAppConfirm(`Bạn có chắc muốn xóa sản phẩm ${name ? '"' + name + '"' : ''} khỏi giỏ hàng?`, 'Xóa sản phẩm')
  if (ok) {
    removeFromCart(variantId)
    selectedItemIds.value = selectedItemIds.value.filter(id => id !== variantId)
  }
}

const handleClearCart = async () => {
  const ok = await showAppConfirm('Bạn có chắc chắn muốn xóa tất cả sản phẩm khỏi giỏ hàng?', 'Dọn sạch giỏ hàng')
  if (ok) {
    clearCart()
    selectedItemIds.value = []
  }
}

// ==========================================
// 5. MÃ GIẢM GIÁ (CHỈ FILL MÃ CÔNG KHAI)
// ==========================================
const voucherInput = ref('')
const appliedVoucher = ref(null)
const availableVouchers = ref([])

const isVoucherPersonal = (v) => {
  if (!v) return false
  if (v.hinhThuc) {
    const h = String(v.hinhThuc).toUpperCase()
    if (h.includes('CÁ NHÂN') || h.includes('CA NHAN') || h === 'PERSONAL') return true
    if (h.includes('CÔNG KHAI') || h.includes('CONG KHAI') || h === 'PUBLIC') return false
  }
  return Array.isArray(v.idKhachHangList) && v.idKhachHangList.length > 0
}

const fetchPublicVouchers = async () => {
  try {
    const res = await api.get('/api/phieu-giam-gia')
    const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    const now = new Date().toISOString()
    availableVouchers.value = list.filter(v => {
      // Chỉ fill các mã CÔNG KHAI
      if (isVoucherPersonal(v)) return false
      if (v.trangThai != null && Number(v.trangThai) !== 1) return false
      if (v.soLuongSuDung != null && v.soLuongSuDung <= 0) return false
      if (v.ngayBatDau && v.ngayBatDau > now) return false
      if (v.ngayKetThuc && v.ngayKetThuc < now) return false
      return true
    })
    autoApplyBestVoucher()
  } catch (e) {
    console.warn('Lỗi tải phiếu giảm giá công khai:', e)
  }
}

const autoApplyBestVoucher = () => {
  if (selectedSubtotal.value <= 0 || !availableVouchers.value || availableVouchers.value.length === 0) {
    return
  }

  let bestVoucher = null
  let maxDiscount = 0

  availableVouchers.value.forEach(v => {
    const minSpend = Number(v.hoaDonToiThieu || 0)
    if (selectedSubtotal.value >= minSpend) {
      const disc = calculateDiscountForVoucher(v, selectedSubtotal.value)
      if (disc > maxDiscount) {
        maxDiscount = disc
        bestVoucher = v
      }
    }
  })

  if (bestVoucher && maxDiscount > 0) {
    appliedVoucher.value = {
      id: bestVoucher.id,
      code: bestVoucher.maPhieuGiamGia,
      name: bestVoucher.tenPhieuGiamGia,
      discount: maxDiscount,
      minSpend: Number(bestVoucher.hoaDonToiThieu || 0),
      raw: bestVoucher,
      isAuto: true
    }
  } else if (appliedVoucher.value?.isAuto) {
    // Nếu mã tự áp dụng không còn đủ điều kiện thì hủy
    const minSpend = Number(appliedVoucher.value.minSpend || 0)
    if (selectedSubtotal.value < minSpend) {
      appliedVoucher.value = null
    }
  }
}

const calculateDiscountForVoucher = (voucher, subtotal) => {
  if (!voucher || subtotal <= 0) return 0
  const minSpend = Number(voucher.hoaDonToiThieu || 0)
  if (subtotal < minSpend) return 0

  let disc = 0
  if (Number(voucher.loaiPhieuGiamGia) === 1) {
    const percent = Number(voucher.giaTriGiamGia || 0)
    disc = Math.round((subtotal * percent) / 100)
    const maxDiscount = Number(voucher.giamToiDa || 0)
    if (maxDiscount > 0) {
      disc = Math.min(disc, maxDiscount)
    }
  } else {
    disc = Number(voucher.giaTriGiamGia || 0)
  }
  return Math.min(disc, subtotal)
}

const handleApplyVoucher = async (codeToApply) => {
  const code = (codeToApply || voucherInput.value || '').trim().toUpperCase()
  if (!code) return
  if (selectedSubtotal.value <= 0) {
    await showAppAlert('Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng trước khi áp dụng mã giảm giá!', 'Chưa chọn sản phẩm', 'warning')
    return
  }

  const voucher = availableVouchers.value.find(v => (v.maPhieuGiamGia || '').toUpperCase() === code)
  if (!voucher) {
    await showAppAlert(`Mã giảm giá '${code}' không hợp lệ hoặc không có trong hệ thống!`, 'Mã giảm giá', 'error')
    return
  }

  const minSpend = Number(voucher.hoaDonToiThieu || 0)
  if (selectedSubtotal.value < minSpend) {
    await showAppAlert(`Đơn hàng chưa đủ điều kiện! Mã ${voucher.maPhieuGiamGia} yêu cầu giá trị đơn tối thiểu từ ${formatCurrency(minSpend)}.`, 'Chưa đủ điều kiện', 'warning')
    return
  }

  const disc = calculateDiscountForVoucher(voucher, selectedSubtotal.value)
  appliedVoucher.value = {
    id: voucher.id,
    code: voucher.maPhieuGiamGia,
    name: voucher.tenPhieuGiamGia,
    discount: disc,
    minSpend: minSpend,
    raw: voucher
  }
  voucherInput.value = ''
}

const applyCouponDirectly = (voucher) => {
  if (appliedVoucher.value?.id === voucher.id) {
    // Nếu bấm lại vào mã đang dùng thì hủy áp dụng
    appliedVoucher.value = null
    return
  }
  handleApplyVoucher(voucher.maPhieuGiamGia)
}

const handleRemoveVoucher = () => {
  appliedVoucher.value = null
}

const suggestedCodes = computed(() => {
  const codes = []
  // Chỉ lấy các mã voucher CÔNG KHAI thực tế từ database của Admin
  if (availableVouchers.value && availableVouchers.value.length > 0) {
    availableVouchers.value.forEach(v => {
      if (v.maPhieuGiamGia && !codes.includes(v.maPhieuGiamGia)) {
        codes.push(v.maPhieuGiamGia)
      }
    })
  }
  return codes
})

const applySuggestedCode = (code) => {
  if (appliedVoucher.value?.code === code) {
    handleRemoveVoucher()
    return
  }
  voucherInput.value = code
  handleApplyVoucher(code)
}

// Cập nhật lại số tiền giảm khi tổng tiền hàng thay đổi
watch(selectedSubtotal, (newSub) => {
  if (newSub <= 0) {
    appliedVoucher.value = null
    return
  }
  // Tự động tìm và áp dụng phiếu giảm giá có mức giảm tốt nhất
  autoApplyBestVoucher()
})

// ==========================================
// 6. TÍNH PHÍ VẬN CHUYỂN GHN & THỜI GIAN GIAO HÀNG
// ==========================================
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
      toWardName: selectedWard.value || '',
      weight: Math.max(200, activeCheckoutItems.value.length * 200),
      insuranceValue: selectedSubtotal.value
    })
    const fee = feeRes.data?.fee != null ? feeRes.data.fee : feeRes.data?.total
    if (fee != null) {
      calculatedGhnFee.value = Number(fee)
    }
  } catch (e) {
    console.warn('GHN fee fallback:', e)
  }

  try {
    const ltRes = await api.post('/api/ghn/leadtime', {
      toProvinceName: selectedProvince.value,
      toWardCode: selectedWard.value || '',
      toWardName: selectedWard.value || ''
    })
    const lt = ltRes.data?.leadtimeFormatted || ltRes.data?.formattedDate
    if (lt) {
      ghnLeadTime.value = lt
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

// ==========================================
// 7. TÍNH TOÁN TỔNG KẾT TÀI CHÍNH
// ==========================================
const hasEnteredAddress = computed(() => {
  return !!(selectedProvince.value && selectedWard.value)
})

const shippingFee = computed(() => {
  if (appliedVoucher.value?.code === 'FREESHIP') return 0
  if (!hasEnteredAddress.value) return null
  return calculatedGhnFee.value || 30000
})

const voucherDiscount = computed(() => {
  if (!appliedVoucher.value) return 0
  return Math.min(appliedVoucher.value.discount || 0, selectedSubtotal.value)
})

const finalTotal = computed(() => {
  if (activeCheckoutItems.value.length === 0) return 0
  const ship = shippingFee.value || 0
  return Math.max(0, selectedSubtotal.value - voucherDiscount.value + ship)
})

// ==========================================
// 8. ĐẶT HÀNG & THANH TOÁN
// ==========================================
const submitting = ref(false)
const orderSuccess = ref(false)
const createdOrder = ref(null)
const copySuccess = ref(false)

const copyOrderCode = (code) => {
  if (!code) return
  navigator.clipboard.writeText(code)
  copySuccess.value = true
  setTimeout(() => {
    copySuccess.value = false
  }, 2500)
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

  const name = (customerName.value || '').trim()
  if (!name) {
    errors.value.customerName = 'Vui lòng nhập họ và tên của bạn'
    isValid = false
  } else if (name.length < 2) {
    errors.value.customerName = 'Họ và tên người nhận phải có ít nhất 2 ký tự'
    isValid = false
  }

  const phone = (customerPhone.value || '').trim()
  const phoneRegex = /^(0[3|5|7|8|9])+([0-9]{8})$/
  if (!phone) {
    errors.value.customerPhone = 'Vui lòng nhập số điện thoại'
    isValid = false
  } else if (!phoneRegex.test(phone)) {
    errors.value.customerPhone = 'Số điện thoại không hợp lệ (gồm 10 chữ số)'
    isValid = false
  }

  const email = (customerEmail.value || '').trim()
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!email) {
    errors.value.customerEmail = 'Vui lòng nhập email nhận hóa đơn'
    isValid = false
  } else if (!emailRegex.test(email)) {
    errors.value.customerEmail = 'Email không hợp lệ (ví dụ: contact@gmail.com)'
    isValid = false
  }

  if (!selectedProvince.value || !selectedProvince.value.trim()) {
    errors.value.selectedProvince = 'Vui lòng chọn Tỉnh/Thành phố'
    isValid = false
  }

  if (!selectedWard.value || !selectedWard.value.trim()) {
    errors.value.selectedWard = 'Vui lòng chọn Phường/Xã'
    isValid = false
  }

  if (!streetAddress.value || !streetAddress.value.trim()) {
    errors.value.streetAddress = 'Vui lòng nhập địa chỉ cụ thể (số nhà, ngõ, tên đường)'
    isValid = false
  }

  return isValid
}

const handlePlaceOrder = async () => {
  if (activeCheckoutItems.value.length === 0) {
    await showAppAlert('Vui lòng tích chọn ít nhất 1 sản phẩm để tiến hành đặt hàng!', 'Chưa chọn sản phẩm', 'warning')
    return
  }

  if (!validateForm()) {
    setTimeout(() => {
      const firstErr = document.querySelector('.input-field-error')
      if (firstErr) {
        firstErr.scrollIntoView({ behavior: 'smooth', block: 'center' })
        firstErr.focus()
      }
    }, 100)
    return
  }

  const outOfStockItems = activeCheckoutItems.value.filter(i => (i.maxStock != null ? i.maxStock : 99) <= 0)
  if (outOfStockItems.length > 0) {
    await showAppAlert(`Sản phẩm "${outOfStockItems[0].productName}" hiện đã hết hàng trong kho. Vui lòng bỏ chọn sản phẩm này!`, 'Sản phẩm hết hàng', 'warning')
    return
  }

  submitting.value = true
  try {
    const subtotal = selectedSubtotal.value
    const ship = shippingFee.value ?? 30000
    const disc = voucherDiscount.value
    const payTotal = Math.max(1, Math.round(Number(finalTotal.value)))
    const purchasedIds = activeCheckoutItems.value.map(i => i.variantId)

    // Họ tên & SĐT người nhận (hỗ trợ danh xưng Anh/Chị & người nhận thay thế nếu có)
    const salutationPrefix = customerSalutation.value ? `${customerSalutation.value} ` : ''
    const baseCustomerName = customerName.value.trim()
    const fullMainCustomerName = (baseCustomerName.startsWith('Anh ') || baseCustomerName.startsWith('Chị '))
      ? baseCustomerName
      : salutationPrefix + baseCustomerName

    const finalReceiverName = customerName.value.trim()
    const finalReceiverPhone = customerPhone.value.trim()
    let noteContent = ''

    const payload = {
      idKhachHang: currentClientUser.value?.id || null,
      idPhieuGiamGia: appliedVoucher.value?.id || null,
      tenKhachHang: finalReceiverName,
      soDienThoai: finalReceiverPhone,
      email: customerEmail.value.trim(),
      diaChiNhanHang: fullDeliveryAddress.value,
      ghiChu: noteContent.trim(),
      phuongThucThanhToan: paymentMethod.value,
      donViVanChuyen: 'Giao Hàng Nhanh (GHN)',
      tongTien: subtotal,
      phiVanChuyen: ship,
      giamGia: disc,
      chiTiet: activeCheckoutItems.value.map(i => ({
        idChiTietSanPham: i.variantId,
        soLuong: i.quantity,
        donGia: i.discountPrice > 0 ? i.discountPrice : i.price
      }))
    }

    const res = await api.post('/api/hoa-don/dat-hang-online', payload)
    const savedOrder = res.data
    createdOrder.value = savedOrder

    // Nếu chọn VNPAY
    if (paymentMethod.value === 'VNPAY') {
      const vnpAmount = Math.max(10000, Math.round(Number(savedOrder?.tienSauGiamGia || payTotal)))
      const vnpPayload = {
        orderCode: savedOrder.maHoaDon,
        amount: vnpAmount,
        orderInfo: `Thanh toan don hang ${savedOrder.maHoaDon}`
      }
      const vnpRes = await api.post('/api/vnpay/create-payment', vnpPayload)
      if (vnpRes.data && vnpRes.data.paymentUrl) {
        removeMultipleFromCart(purchasedIds)
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
        router.push(`/don-hang?code=${savedOrder.maHoaDon}`)
        return
      }
    }

    // Gửi email xác nhận đặt hàng kèm link theo dõi đơn hàng
    try {
      if (customerEmail.value && customerEmail.value.trim().includes('@')) {
        api.post(`/api/hoa-don/code/${savedOrder.maHoaDon}/send-email`, {
          email: customerEmail.value.trim()
        }).catch(e => console.warn('Gửi email theo dõi đơn hàng:', e))
      }
    } catch (e) {}

    // Kích hoạt thông báo đặt hàng thành công riêng cho tài khoản
    try {
      notifyOrderSuccess(savedOrder)
      fetchNotifications()
    } catch (e) {}

    // Nếu là COD
    removeMultipleFromCart(purchasedIds)
    orderSuccess.value = true
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (err) {
    console.error('Lỗi đặt hàng:', err)
    await showAppAlert(err.response?.data?.message || 'Không thể hoàn tất đơn hàng. Vui lòng thử lại sau!', 'Lỗi đặt hàng', 'error')
  } finally {
    submitting.value = false
  }
}

// ==========================================
// 9. ĐỒNG BỘ TỒN KHO & GỢI Ý KHI RỖNG
// ==========================================
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


const loadSampleProductToCart = async () => {
  try {
    const res = await api.get('/api/san-pham', { params: { page: 0, size: 5, trangThai: 1 } })
    const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    if (list.length > 0) {
      const prod = list[0]
      const resVar = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${prod.id}`)
      const vars = Array.isArray(resVar.data) ? resVar.data : []
      if (vars.length > 0) {
        const v = vars.find(i => (i.soLuong || 0) > 0) || vars[0]
        addToCart(prod, v, 1)
        selectedItemIds.value = [v.id]
        updateGhnShipping()
      }
    }
  } catch (e) {
    console.warn('Lỗi nạp sản phẩm mẫu vào giỏ:', e)
  }
}

const syncCartStock = async () => {
  if (!cartItems.value || cartItems.value.length === 0) {
    return
  }
  let updated = false
  const validItems = []

  for (const item of cartItems.value) {
    if (!item.variantId) continue
    try {
      const res = await api.get(`/api/chi-tiet-san-pham/${item.variantId}`)
      if (res.data) {
        const freshStock = res.data.soLuong != null 
          ? res.data.soLuong 
          : (res.data.soLuongTon != null ? res.data.soLuongTon : 0)
        item.maxStock = freshStock
        if (freshStock <= 0) {
          item.maxStock = 0
          selectedItemIds.value = selectedItemIds.value.filter(id => id !== item.variantId)
        } else if (item.quantity > freshStock) {
          item.quantity = freshStock
        }
        validItems.push(item)
      }
    } catch (err) {
      if (err.response && err.response.status === 404) {
        updated = true // Purge old DB invalid variant
      } else {
        validItems.push(item)
      }
    }
  }

  if (updated || validItems.length !== cartItems.value.length) {
    cartItems.value = validItems
    saveCart()
  }

  // Giỏ hàng trống giữ nguyên, không tự động thêm sản phẩm mẫu
}

onMounted(async () => {
  syncCartStock()
  loadRecommendedProducts()
  fetchPublicVouchers()

  const allData = await loadAdministrativeData()
  if (allData && allData.length > 0) {
    provincesList.value = allData
  }
  await reloadWards(selectedProvince.value)
  updateGhnShipping()
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('vi-VN')
  } catch (e) {
    return dateStr
  }
}
</script>

<template>
  <div class="cart-checkout-page">
    <!-- MODAL ĐẶT HÀNG THÀNH CÔNG (CHO COD) -->
    <div v-if="orderSuccess" class="order-success-modal-backdrop">
      <div class="order-success-card">
        <div class="success-icon-badge">✓</div>
        <h2 class="success-title">🎉 Đặt Hàng Thành Công!</h2>
        <p class="success-sub">
          Cảm ơn bạn <strong>{{ customerName }}</strong> đã mua sắm tại <strong>FF T-Shirt</strong>.<br />
          Đơn hàng của bạn đã được ghi nhận trên hệ thống và nhân viên sẽ liên hệ xác nhận sớm nhất.
        </p>

        <div class="success-summary-box">
          <div class="code-copy-row">
            <div>
              <span class="label">Mã đơn hàng:</span>
              <strong class="code-val">#{{ createdOrder?.maHoaDon }}</strong>
            </div>
            <button class="btn-copy-mini" @click="copyOrderCode(createdOrder?.maHoaDon)">
              {{ copySuccess ? '✓ Đã chép' : '📋 Sao chép' }}
            </button>
          </div>

          <div class="summary-meta-grid">
            <div class="meta-row">
              <span class="m-label">👤 Người nhận:</span>
              <strong class="m-val">{{ customerName }} • {{ customerPhone }}</strong>
            </div>
            <div class="meta-row">
              <span class="m-label">📍 Địa chỉ nhận hàng:</span>
              <span class="m-val">{{ fullDeliveryAddress }}</span>
            </div>
            <div class="meta-row">
              <span class="m-label">💳 Phương thức:</span>
              <span class="m-val font-semibold">{{ paymentMethod === 'VNPAY' ? 'Cổng thanh toán VNPAY' : 'Thanh toán khi nhận hàng (COD)' }}</span>
            </div>
            <div class="meta-row">
              <span class="m-label">🚚 Vận chuyển:</span>
              <span class="m-val font-semibold">Giao Hàng Nhanh (GHN Express)</span>
            </div>
            <div class="meta-row meta-total-row">
              <span class="m-label">Tổng thanh toán:</span>
              <strong class="m-val text-gold">{{ formatCurrency(finalTotal) }}</strong>
            </div>
          </div>

          <div class="email-sent-success-box" v-if="customerEmail">
            <span class="ess-icon">✉️</span>
            <div class="ess-text">
              Hệ thống đã gửi email xác nhận đặt hàng thành công kèm đường link theo dõi đơn hàng đến:
              <strong>{{ customerEmail }}</strong>. Bạn có thể kiểm tra hòm thư bất kỳ lúc nào!
            </div>
          </div>
        </div>

        <div class="success-actions-row">
          <button class="btn-primary-action" @click="router.push(`/don-hang?code=${createdOrder?.maHoaDon}`)">
            📦 Theo dõi đơn hàng
          </button>
          <router-link to="/cua-hang" class="btn-secondary-action">
            🛍️ Tiếp tục mua sắm
          </router-link>
        </div>
      </div>
    </div>

        <div class="container-client page-body-container">
      <!-- BỐ CỤC 2 CỘT CHUẨN COOLMATE (LUÔN HIỂN THỊ ĐẦY ĐỦ FORM & GIỎ HÀNG) -->
      <div class="checkout-split-grid">
        <!-- ============================================== -->
        <!-- CỘT TRÁI: THÔNG TIN KHÁCH HÀNG & GIAO HÀNG & PHƯƠNG THỨC -->
        <!-- ============================================== -->
        <div class="left-delivery-column">
          <!-- 1. Banner tài khoản đăng nhập -->
          <div class="auth-helper-banner">
            <div v-if="isClientLoggedIn" class="logged-in-user-tag">
              <span class="user-icon">👤</span>
              <span>Đang đặt hàng với tài khoản: <strong>{{ currentClientUser?.name }}</strong> ({{ currentClientUser?.email || currentClientUser?.phone }})</span>
            </div>
            <div v-else class="guest-auth-actions">
              <span class="auth-ask">Đã có tài khoản?</span>
              <button type="button" class="btn-auth-link" @click="openLogin">Đăng nhập</button>
              <span class="auth-sep-pipe">|</span>
              <button type="button" class="btn-google-cart-quick" @click="openGoogleLogin">
                <svg viewBox="0 0 24 24" width="18" height="18">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
                </svg>
                <span>Tiếp tục với Google</span>
              </button>
            </div>
          </div>

          <!-- 2. Form Thông tin người nhận hàng (Đầy đủ tiêu đề rõ ràng từng trường) -->
          <div class="section-card-panel">
            <h3 class="panel-section-title">Thông tin giao hàng</h3>

            <div class="form-salutation-name-phone-row">
              <div class="form-group-field field-fullname">
                <label class="checkout-field-label">Họ và tên <span class="req-star">*</span></label>
                <input
                  type="text"
                  v-model="customerName"
                  placeholder="Nhập họ tên của bạn"
                  class="form-input-text"
                  :class="{ 'input-field-error': errors.customerName }"
                  @input="clearError('customerName')"
                />
                <span v-if="errors.customerName" class="field-error-text">{{ errors.customerName }}</span>
              </div>

              <div class="form-group-field field-phone">
                <label class="checkout-field-label">Số điện thoại <span class="req-star">*</span></label>
                <input
                  type="tel"
                  v-model="customerPhone"
                  placeholder="Nhập số điện thoại"
                  class="form-input-text"
                  :class="{ 'input-field-error': errors.customerPhone }"
                  @input="clearError('customerPhone')"
                />
                <span v-if="errors.customerPhone" class="field-error-text">{{ errors.customerPhone }}</span>
              </div>
            </div>

            <div class="form-group-field mt-3">
              <label class="checkout-field-label">Email <span class="req-star">*</span></label>
              <input
                type="email"
                v-model="customerEmail"
                placeholder="Nhập email của bạn"
                class="form-input-text"
                :class="{ 'input-field-error': errors.customerEmail }"
                @input="clearError('customerEmail')"
              />
              <span v-if="errors.customerEmail" class="field-error-text">{{ errors.customerEmail }}</span>
            </div>

            <!-- ĐỊA CHỈ GIAO HÀNG (Có tiêu đề rõ ràng từng ô) -->
            <div class="form-group-field mt-3">
              <label class="checkout-field-label">Địa chỉ chi tiết (Số nhà, ngõ, tên đường...) <span class="req-star">*</span></label>
              <input
                type="text"
                v-model="streetAddress"
                placeholder="Nhập địa chỉ (số nhà, ngõ, tên đường...)"
                class="form-input-text"
                :class="{ 'input-field-error': errors.streetAddress }"
                @input="clearError('streetAddress')"
              />
              <span v-if="errors.streetAddress" class="field-error-text">{{ errors.streetAddress }}</span>
            </div>

            <div class="form-grid-2 mt-3">
              <div class="form-group-field">
                <label class="checkout-field-label">Tỉnh / Thành phố <span class="req-star">*</span></label>
                <select
                  v-model="selectedProvince"
                  @change="onProvinceChange"
                  class="form-input-select"
                  :class="{ 'input-field-error': errors.selectedProvince }"
                >
                  <option value="" disabled selected>-- Chọn Tỉnh / Thành phố --</option>
                  <option v-for="p in provincesList" :key="p.name || p" :value="p.name || p">
                    {{ p.name || p }}
                  </option>
                </select>
                <span v-if="errors.selectedProvince" class="field-error-text">{{ errors.selectedProvince }}</span>
              </div>

              <div class="form-group-field">
                <label class="checkout-field-label">Phường / Xã <span class="req-star">*</span></label>
                <select
                  v-model="selectedWard"
                  @change="clearError('selectedWard'); updateGhnShipping()"
                  class="form-input-select"
                  :disabled="!selectedProvince"
                  :class="{ 'input-field-error': errors.selectedWard }"
                >
                  <option value="" disabled selected>-- Chọn Phường / Xã --</option>
                  <option v-for="w in currentWards" :key="w.name || w" :value="w.name || w">
                    {{ w.name || w }}
                  </option>
                </select>
                <span v-if="errors.selectedWard" class="field-error-text">{{ errors.selectedWard }}</span>
              </div>
            </div>

            <!-- Giờ giao hàng của đơn vị vận chuyển -->
            <div class="delivery-schedule-notice mt-2">
              <span class="ds-icon">🕒</span>
              <span>Đơn hàng được giao trong giờ hành chính (thứ 2 đến thứ 7). <a href="javascript:void(0)" class="link-detail-sub" @click="showAppAlert('Đơn vị vận chuyển GHN Express giao hàng từ 8h00 - 18h00 từ thứ 2 đến thứ 7 hàng tuần.', 'Thời gian giao hàng', 'info')">Xem chi tiết</a></span>
            </div>
          </div>

          <!-- 3. Đơn vị vận chuyển (GHN Express thiết kế siêu gọn nhẹ) -->
          <div class="section-card-panel shipping-panel-slim mt-3">
            <h3 class="panel-section-title shipping-title-slim">Hình thức vận chuyển</h3>
            <div class="shipping-carrier-compact">
              <div class="scc-left">
                <input type="radio" checked readonly class="scc-radio" />
                <img src="/img/ghn-brand-logo.png" alt="GHN Express" class="scc-logo" />
                <span class="scc-title">Giao Hàng Nhanh (GHN)</span>
              </div>
              <div class="scc-right">
                <span class="scc-price">
                  <template v-if="!hasEnteredAddress">
                    <span class="scc-price-pending">Chưa có địa chỉ</span>
                  </template>
                  <template v-else-if="shippingFee === 0">
                    MIỄN PHÍ
                  </template>
                  <template v-else>
                    {{ formatCurrency(shippingFee) }}
                  </template>
                </span>
              </div>
            </div>
          </div>

          <!-- 4. Phương thức thanh toán (COD & VNPAY) - KHÔNG CÓ MOMO & SHOPEEPAY -->
          <div class="section-card-panel mt-4">
            <h3 class="panel-section-title">Phương thức thanh toán</h3>

            <div class="payment-methods-stack">
              <!-- Lựa chọn 1: COD -->
              <label class="payment-choice-card" :class="{ active: paymentMethod === 'COD' }">
                <div class="choice-radio-wrap">
                  <input type="radio" value="COD" v-model="paymentMethod" name="payMethod" />
                </div>
                <div class="choice-icon-wrap">
                  <img src="/img/cod-truck.png" alt="COD" class="cod-truck-logo-img" />
                </div>
                <div class="choice-info-wrap">
                  <strong class="choice-title">Thanh toán khi nhận hàng (COD)</strong>
                  <span class="choice-desc">Quý khách thanh toán tiền mặt trực tiếp cho nhân viên giao nhận khi nhận hàng.</span>
                </div>
              </label>

              <!-- Lựa chọn 2: VNPAY -->
              <label class="payment-choice-card" :class="{ active: paymentMethod === 'VNPAY' }">
                <div class="choice-radio-wrap">
                  <input type="radio" value="VNPAY" v-model="paymentMethod" name="payMethod" />
                </div>
                <div class="choice-icon-wrap">
                  <img src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg" alt="VNPAY" class="vnpay-logo-img" />
                </div>
                <div class="choice-info-wrap">
                  <strong class="choice-title">Cổng thanh toán trực tuyến VNPAY</strong>
                  <span class="choice-desc">Quét mã VNPAY-QR qua ứng dụng Ngân hàng hoặc thẻ ATM / Internet Banking / Visa.</span>
                </div>
              </label>
            </div>
          </div>
        </div>

        <!-- ============================================== -->
        <!-- CỘT PHẢI: SẢN PHẨM GIỎ HÀNG & VOUCHER & TỔNG KẾT TÀI CHÍNH -->
        <!-- ============================================== -->
        <div class="right-summary-column">
          <!-- 1. Danh sách sản phẩm trong giỏ -->
          <div class="summary-section-box">
            <div class="cart-items-header-bar">
              <label class="select-all-label">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleSelectAll"
                />
                <span>Tất cả sản phẩm ({{ cartItems.length }})</span>
              </label>
              <button type="button" class="btn-clear-all" @click="handleClearCart" title="Xóa toàn bộ giỏ hàng">
                🗑️ Xóa tất cả
              </button>
            </div>

            <div v-if="cartItems.length > 0" class="cart-items-list-scroll">
              <div
                v-for="item in cartItems"
                :key="item.variantId"
                class="cart-item-row"
              >
                <!-- Checkbox chọn item -->
                <div class="item-checkbox">
                  <input
                    type="checkbox"
                    :value="item.variantId"
                    v-model="selectedItemIds"
                  />
                </div>

                <!-- Ảnh sản phẩm -->
                <div class="item-thumb-box">
                  <img :src="item.image || '/images/products/default_tshirt.jpg'" :alt="item.productName" />
                </div>

                <!-- Thông tin & Stepper -->
                <div class="item-details-col">
                  <h4 class="item-product-name">{{ item.productName }}</h4>
                  <div class="item-variant-pill">
                    {{ item.color }} / Size {{ item.size }}
                  </div>

                  <div class="item-qty-price-row">
                    <!-- Nút số lượng -->
                    <div class="qty-stepper-box">
                      <button
                        type="button"
                        class="btn-step"
                        @click="handleQtyChange(item.variantId, -1, item.quantity, item.maxStock)"
                        :disabled="item.quantity <= 1"
                      >
                        -
                      </button>
                      <span class="qty-val">{{ item.quantity }}</span>
                      <button
                        type="button"
                        class="btn-step"
                        @click="handleQtyChange(item.variantId, 1, item.quantity, item.maxStock)"
                        :disabled="item.maxStock != null && item.quantity >= item.maxStock"
                      >
                        +
                      </button>
                    </div>

                    <!-- Giá tiền -->
                    <div class="item-price-val">
                      <strong class="price-current">
                        {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
                      </strong>
                    </div>
                  </div>
                </div>

                <!-- Nút xóa 1 dòng -->
                <button
                  type="button"
                  class="btn-del-single"
                  @click="handleRemove(item.variantId, item.productName)"
                  title="Xóa sản phẩm này"
                >
                  ✕
                </button>
              </div>
            </div>

            <!-- Khối trạng thái khi giỏ hàng trống (Match style Coolmate) -->
            <div v-else class="cart-empty-inline-box">
              <div class="cei-icon">🛒</div>
              <h4 class="cei-title">Giỏ hàng của bạn đang trống</h4>
              <p class="cei-sub">Chưa có sản phẩm nào được chọn để thanh toán.</p>
              <div class="cei-buttons">
                <router-link to="/cua-hang" class="btn-cei-shop">
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="#d2a764" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="btn-shop-svg-icon">
                    <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <path d="M16 10a4 4 0 0 1-8 0"></path>
                  </svg>
                  <span>Khám Phá Sản Phẩm Ngay</span>
                  <span class="btn-cei-arrow">&rarr;</span>
                </router-link>
                
              </div>
            </div>
          </div>

          <!-- 2. Khối Mã giảm giá (Chỉ fill mã công khai) -->
          <div class="summary-section-box mt-3">
            <h4 class="box-mini-title">Mã giảm giá</h4>

            <!-- Ô nhập voucher thủ công -->
            <div class="voucher-input-action-row">
              <input
                type="text"
                v-model="voucherInput"
                placeholder="Nhập mã giảm giá"
                class="form-input-text voucher-text-input"
                @keyup.enter="handleApplyVoucher()"
              />
              <button
                type="button"
                class="btn-apply-voucher"
                @click="handleApplyVoucher()"
                :disabled="!voucherInput.trim()"
              >
                ÁP DỤNG
              </button>
            </div>

            <!-- GỢI Ý MÃ GIẢM GIÁ (Chỉ hiển thị khi có mã công khai từ Admin DB) -->
            <div v-if="suggestedCodes.length > 0" class="suggested-vouchers-row">
              <span class="sv-label">Gợi ý mã:</span>
              <div class="sv-tags-wrap">
                <button
                  v-for="code in suggestedCodes"
                  :key="code"
                  type="button"
                  class="btn-suggest-code-tag"
                  :class="{ active: appliedVoucher?.code === code }"
                  @click="applySuggestedCode(code)"
                >
                  {{ code }}
                </button>
              </div>
            </div>

            <!-- Badge khi đã áp dụng thành công -->
            <div v-if="appliedVoucher" class="applied-voucher-chip">
              <div class="av-left">
                <span class="av-ico">🎟️</span>
                <div>
                  <div class="av-code-badge-row" style="display: flex; align-items: center; gap: 6px;">
                    <strong>Mã: {{ appliedVoucher.code }}</strong>
                    <span class="av-best-badge" v-if="appliedVoucher.isAuto">Tốt nhất</span>
                  </div>
                  <span class="av-disc">Đã giảm -{{ formatCurrency(appliedVoucher.discount) }}</span>
                </div>
              </div>
              <button type="button" class="btn-remove-v" @click="handleRemoveVoucher" title="Bỏ áp dụng mã">
                ✕
              </button>
            </div>

            <!-- Danh sách thẻ voucher CÔNG KHAI từ Admin (Match style Coolmate) -->
            <div v-if="availableVouchers.length > 0" class="public-coupons-carousel">
              <div
                v-for="v in availableVouchers"
                :key="v.id"
                class="coupon-card-ticket"
                :class="{ active: appliedVoucher?.id === v.id }"
                @click="applyCouponDirectly(v)"
              >
                <div class="ticket-left">
                  <div class="ticket-badge">{{ v.maPhieuGiamGia }}</div>
                  <div class="ticket-name">
                    {{ Number(v.loaiPhieuGiamGia) === 1 ? `Giảm ${v.giaTriGiamGia}%` : `Giảm ${formatCurrency(v.giaTriGiamGia)}` }}
                    <span v-if="v.hoaDonToiThieu && Number(v.hoaDonToiThieu) > 0">
                      • Đơn từ {{ formatCurrency(v.hoaDonToiThieu) }}
                    </span>
                  </div>
                  <div class="ticket-hsd" v-if="v.ngayKetThuc">
                    HSD: {{ formatDate(v.ngayKetThuc) }}
                  </div>
                </div>
                <div class="ticket-right">
                  <button type="button" class="btn-ticket-use" :class="{ applied: appliedVoucher?.id === v.id }">
                    {{ appliedVoucher?.id === v.id ? '✓ Đang dùng' : 'Áp dụng' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Link mã giới thiệu (Ảnh 5) -->
            <div class="referral-code-link-row">
              <a href="javascript:void(0)" class="referral-link" @click="showAppAlert('Tính năng giới thiệu bạn bè nhận ưu đãi sẽ sớm được cập nhật!', 'Mã giới thiệu', 'info')">
                Bạn có mã giới thiệu? &rarr;
              </a>
            </div>
          </div>

          


          <!-- 4. Tóm tắt đơn hàng & Nút Mua ngay (Ảnh 4 & Ảnh 5) -->
          <div class="summary-section-box mt-3 final-totals-box">
            <h4 class="box-mini-title">Tóm tắt đơn hàng</h4>

            <div class="totals-lines">
              <div class="t-line">
                <span>Tạm tính ({{ activeCheckoutItems.length }} sản phẩm)</span>
                <strong>{{ formatCurrency(selectedSubtotal) }}</strong>
              </div>

              <div class="t-line ghn-ship-line">
                <span class="ghn-ship-label">
                  Phí vận chuyển
                  <img src="/img/ghn-brand-logo.png" alt="GHN" class="ghn-inline-brand-logo" />
                </span>
                <span class="ship-cost">
                  <template v-if="!hasEnteredAddress">
                    <span class="text-muted" style="font-size: 13px; font-weight: 500; font-style: italic;">Chưa chọn địa chỉ</span>
                  </template>
                  <template v-else-if="shippingFee === 0">
                    MIỄN PHÍ
                  </template>
                  <template v-else>
                    {{ formatCurrency(shippingFee) }}
                  </template>
                </span>
              </div>

              <div v-if="voucherDiscount > 0" class="t-line discount-highlight">
                <span>Giảm giá [{{ appliedVoucher?.code }}]:</span>
                <strong class="text-green">-{{ formatCurrency(voucherDiscount) }}</strong>
              </div>

              <div class="t-line grand-total-line">
                <span class="grand-label">TỔNG CỘNG:</span>
                <strong class="grand-value text-gold">{{ formatCurrency(finalTotal) }}</strong>
              </div>
            </div>

            <!-- Nút hành động chính ĐẶT HÀNG chuẩn màu thương hiệu (Ảnh 1 & Ảnh 4) -->
            <button
              type="button"
              class="btn-place-order-main btn-gold-buy-now"
              :disabled="submitting || activeCheckoutItems.length === 0"
              @click="handlePlaceOrder"
            >
              <span v-if="submitting">Đang xử lý đơn hàng...</span>
              <span v-else>Đặt hàng</span>
            </button>

            
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Đăng nhập / Đăng ký nếu khách muốn đăng nhập -->
    <ModalDangNhap
      v-model="showLoginModal"
      :initial-view="authInitialView"
      @login-success="handleLoginSuccess"
    />
  </div>
</template>

<style scoped>
.cart-checkout-page {
  background-color: #f8fafc;
  min-height: 100vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

/* 1. Header Banner màu sắc thương hiệu website */
.page-hero-banner {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #2b394a 100%);
  color: #ffffff;
  padding: 38px 0 34px;
  text-align: center;
  border-bottom: 2px solid #ecd8af;
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 6px;
  color: #ffffff;
}

.hero-subtitle {
  color: #cbd5e1;
  font-size: 14px;
  margin: 0;
}

.page-body-container {
  margin-top: 28px;
}

/* 2. Trường hợp giỏ hàng trống */
.empty-cart-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 60px 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 14px;
}

.empty-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
}

.btn-goto-shop {
  display: inline-block;
  background: #d2a764;
  color: #0f172a;
  font-weight: 800;
  font-size: 15px;
  padding: 13px 32px;
  border-radius: 10px;
  text-decoration: none;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.4);
  transition: all 0.25s;
}

.btn-goto-shop:hover {
  background: #e0b875;
  transform: translateY(-2px);
}

.recom-section {
  margin-top: 50px;
  text-align: left;
  border-top: 1px solid #e2e8f0;
  padding-top: 30px;
}

.recom-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 18px;
}

.recom-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

/* 3. Bố cục 2 cột chính */
.checkout-split-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 28px;
  align-items: start;
}

@media (max-width: 1024px) {
  .checkout-split-grid {
    grid-template-columns: 1fr;
  }
}

/* CỘT TRÁI */
.left-delivery-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.auth-helper-banner {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 18px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
}

.logged-in-user-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: #334155;
}

.guest-auth-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  flex-wrap: wrap;
}

.btn-auth-link {
  background: none;
  border: none;
  color: #0284c7;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}

.section-card-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.panel-section-title {
  font-size: 17px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 640px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}

.form-group-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group-field label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.req {
  color: #ef4444;
}

.form-input-text,
.form-input-select {
  width: 100%;
  padding: 11px 14px;
  font-size: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background-color: #ffffff;
  color: #0f172a;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input-text:focus,
.form-input-select:focus {
  outline: none;
  border-color: #d2a764;
  box-shadow: 0 0 0 3px rgba(210, 167, 100, 0.2);
}

.input-field-error {
  border-color: #ef4444 !important;
  background-color: #fffaf0;
}

.field-error-text {
  font-size: 11.5px;
  color: #ef4444;
  font-weight: 600;
}

.mt-2 { margin-top: 8px; }
.mt-3 { margin-top: 14px; }
.mt-4 { margin-top: 20px; }

/* Tùy chọn mở rộng */
.form-checkbox-options {
  border-top: 1px dashed #e2e8f0;
  padding-top: 16px;
}

.checkbox-line-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: #334155;
  cursor: pointer;
  font-weight: 600;
}

.sub-field-expand {
  margin-top: 10px;
  margin-left: 24px;
}

/* PHƯƠNG THỨC THANH TOÁN */
.payment-methods-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-choice-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #ffffff;
}

.payment-choice-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.payment-choice-card.active {
  border-color: #d2a764;
  background: #fffdfa;
  box-shadow: 0 2px 10px rgba(210, 167, 100, 0.15);
}

.choice-radio-wrap input {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #d2a764;
}

.choice-icon-wrap {
  font-size: 24px;
  display: flex;
  align-items: center;
}

.vnpay-logo-img {
  height: 22px;
  object-fit: contain;
}

.choice-info-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.choice-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.choice-desc {
  font-size: 12.5px;
  color: #64748b;
}

/* CỘT PHẢI */
.right-summary-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary-section-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}

.box-mini-title {
  font-size: 15.5px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 14px;
}

/* Danh sách sản phẩm trong giỏ */
.cart-items-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 14px;
}

.select-all-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  cursor: pointer;
}

.btn-clear-all {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-clear-all:hover {
  color: #ef4444;
}

.cart-empty-inline-box {
  padding: 36px 20px;
  text-align: center;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  margin: 10px 0;
}
.cei-icon {
  font-size: 38px;
  margin-bottom: 8px;
}
.cei-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 6px;
}
.cei-sub {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 16px;
}
.cei-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}
.btn-cei-shop {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 22px;
  background: #0f172a;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.12);
}
.btn-cei-shop:hover {
  background: #1e293b;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.2);
}
.btn-shop-svg-icon {
  stroke: #d2a764;
  flex-shrink: 0;
}
.btn-cei-arrow {
  color: #d2a764;
  font-weight: 800;
  margin-left: 2px;
  transition: transform 0.2s;
}
.btn-cei-shop:hover .btn-cei-arrow {
  transform: translateX(3px);
}
.btn-cei-sample {
  padding: 9px 18px;
  background: linear-gradient(135deg, #d2a764 0%, #b88d4c 100%);
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(210, 167, 100, 0.35);
  transition: all 0.2s;
}
.btn-cei-sample:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(210, 167, 100, 0.45);
}

.cart-items-list-scroll {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}

.cart-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
}

.cart-item-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.item-checkbox input {
  width: 17px;
  height: 17px;
  cursor: pointer;
  accent-color: #d2a764;
}

.item-thumb-box img {
  width: 58px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.item-details-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-product-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.35;
}

.item-variant-pill {
  font-size: 11.5px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 7px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}

.item-qty-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.qty-stepper-box {
  display: inline-flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
}

.btn-step {
  width: 26px;
  height: 26px;
  border: none;
  background: none;
  cursor: pointer;
  font-weight: 700;
  font-size: 14px;
  color: #334155;
  transition: background 0.15s;
}

.btn-step:hover:not(:disabled) {
  background: #f1f5f9;
}

.btn-step:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.qty-val {
  min-width: 26px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.price-current {
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
}

.btn-del-single {
  background: none;
  border: none;
  color: #cbd5e1;
  font-size: 16px;
  cursor: pointer;
  transition: color 0.2s;
  padding: 4px;
}

.btn-del-single:hover {
  color: #ef4444;
}

/* VOUCHER / MÃ GIẢM GIÁ */
.voucher-input-action-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.voucher-text-input {
  flex: 1;
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.btn-apply-voucher {
  background: #1e293b;
  color: #ffffff;
  border: none;
  padding: 0 18px;
  border-radius: 8px;
  font-weight: 800;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-apply-voucher:hover:not(:disabled) {
  background: #0f172a;
}

.btn-apply-voucher:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.applied-voucher-chip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.av-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}

.av-disc {
  display: block;
  font-size: 11.5px;
  color: #15803d;
  font-weight: 700;
}

.btn-remove-v {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 14px;
  cursor: pointer;
  font-weight: 700;
}

/* CAROUSEL THẺ VOUCHER CÔNG KHAI */
.public-coupons-carousel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 2px;
}

.coupon-card-ticket {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 10px 14px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.coupon-card-ticket:hover {
  background: #f8fafc;
  border-color: #d2a764;
}

.coupon-card-ticket.active {
  background: #fffdf5;
  border: 1.5px solid #d2a764;
}

.ticket-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ticket-badge {
  font-size: 12.5px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.5px;
}

.ticket-name {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
}

.ticket-hsd {
  font-size: 11px;
  color: #94a3b8;
}

.btn-ticket-use {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #1e293b;
  font-size: 11.5px;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-ticket-use:hover {
  background: #e2e8f0;
}

.btn-ticket-use.applied {
  background: #10b981;
  color: #ffffff;
  border-color: #10b981;
}

/* GHN VẬN CHUYỂN */
.ghn-official-badge {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ghn-official-logo {
  height: 38px;
  width: auto;
  object-fit: contain;
  border-radius: 6px;
  background: #ffffff;
  padding: 2px 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.ghn-title-group {
  display: flex;
  flex-direction: column;
}
.ghn-brand-main {
  font-size: 14px;
  font-weight: 800;
  color: #00467f;
  letter-spacing: 0.3px;
  line-height: 1.2;
}
.ghn-brand-sub {
  font-size: 11px;
  color: #ea580c;
  font-weight: 600;
  margin-top: 2px;
}

.ghn-shipping-summary {
  background: #f8fafc;
}

.ghn-brand-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ghn-badge-tag {
  font-size: 12.5px;
  font-weight: 800;
  color: #ff6600;
}

.ghn-leadtime-badge {
  font-size: 12.5px;
  color: #0f172a;
}

.ghn-note-text {
  font-size: 11.5px;
  color: #64748b;
  margin: 4px 0 0;
}

/* TỔNG KẾT TÀI CHÍNH */
.totals-lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 16px;
  margin-bottom: 18px;
}

.t-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #475569;
}

.ship-cost {
  font-weight: 700;
  color: #0f172a;
}

.text-green {
  color: #15803d;
}

.grand-total-line {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px dashed #cbd5e1;
}

.grand-label {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.grand-value {
  font-size: 20px;
  font-weight: 900;
}

.text-gold {
  color: #c59b27;
}

.btn-place-order-main {
  width: 100%;
  padding: 16px 0;
  background: #d2a764;
  border: none;
  color: #0f172a;
  font-size: 15.5px;
  font-weight: 800;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.4);
  letter-spacing: 0.3px;
}

.btn-place-order-main:hover:not(:disabled) {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.5);
}

.btn-place-order-main:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.security-commitment {
  text-align: center;
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

/* MODAL ĐẶT HÀNG THÀNH CÔNG */
.order-success-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  backdrop-filter: blur(4px);
}

.order-success-card {
  background: #ffffff;
  border-radius: 16px;
  max-width: 580px;
  width: 100%;
  padding: 36px 30px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0,0,0,0.25);
  animation: popIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.success-icon-badge {
  width: 60px;
  height: 60px;
  background: #10b981;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto 16px;
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.35);
}

.success-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 8px;
}

.success-sub {
  font-size: 14px;
  color: #475569;
  margin: 0 0 24px;
  line-height: 1.5;
}

.success-summary-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  text-align: left;
  margin-bottom: 24px;
}

.code-copy-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px dashed #cbd5e1;
  margin-bottom: 12px;
}

.code-val {
  font-size: 15px;
  color: #0284c7;
  margin-left: 6px;
}

.btn-copy-mini {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.email-sent-success-box {
  margin-top: 14px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13.5px;
  color: #166534;
  line-height: 1.5;
}
.ess-icon {
  font-size: 20px;
  flex-shrink: 0;
}
.summary-meta-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13.5px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
}

.m-label {
  color: #64748b;
}

.m-val {
  color: #0f172a;
  text-align: right;
  max-width: 65%;
}

.meta-total-row {
  margin-top: 6px;
  padding-top: 8px;
  border-top: 1px dashed #cbd5e1;
}

.success-actions-row {
  display: flex;
  gap: 14px;
}

.btn-primary-action {
  flex: 1;
  padding: 13px 0;
  background: #d2a764;
  color: #0f172a;
  border: none;
  font-weight: 800;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary-action:hover {
  background: #e0b875;
}

.btn-secondary-action {
  flex: 1;
  padding: 13px 0;
  background: #f1f5f9;
  color: #334155;
  text-decoration: none;
  font-weight: 800;
  font-size: 14px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-secondary-action:hover {
  background: #e2e8f0;
}

/* Freeship Progress Bar (Ảnh 2) */
.freeship-progress-card {
  background: #fffdfa;
  border: 1px solid #ecd8af;
  border-radius: 12px;
  padding: 14px 18px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.freeship-info-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 8px;
}

.fs-truck-icon {
  font-size: 18px;
}

.fs-bold {
  color: #0f172a;
  font-weight: 800;
  text-decoration: underline;
}

.fs-success {
  color: #166534;
  font-weight: 700;
}

.freeship-track {
  width: 100%;
  height: 6px;
  background-color: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.freeship-bar {
  height: 100%;
  background: linear-gradient(90deg, #d4a373 0%, #3e5c76 100%);
  border-radius: 999px;
  transition: width 0.4s ease;
}

/* Bằng chứng xã hội / Social Proof (Ảnh 5) */
.social-proof-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #475569;
  padding: 10px 0 2px;
  font-style: italic;
}

.sp-icon {
  font-size: 15px;
}

/* Gợi ý mã giảm giá (Ảnh 3) */
.suggested-vouchers-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  margin-bottom: 12px;
}

.sv-label {
  font-size: 13px;
  color: #3e5c76;
  font-weight: 600;
}

.sv-tags-wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.btn-suggest-code-tag {
  background: #f0f7ff;
  border: 1px dashed #60a5fa;
  border-radius: 6px;
  padding: 3px 10px;
  font-size: 12.5px;
  font-weight: 700;
  color: #1d4ed8;
  letter-spacing: 0.3px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-suggest-code-tag:hover {
  background: #dbeafe;
  border-color: #2563eb;
  transform: translateY(-1px);
}

.btn-suggest-code-tag.active {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.referral-code-link-row {
  margin-top: 10px;
  text-align: right;
}

.referral-link {
  font-size: 12.5px;
  color: #0284c7;
  font-weight: 600;
  text-decoration: none;
}

.referral-link:hover {
  text-decoration: underline;
}

/* Form bổ sung chuẩn Coolmate */
.user-policy-consent-row {
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 14px;
}

.consent-checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12.5px;
  color: #475569;
  cursor: pointer;
  line-height: 1.4;
}

.consent-checkbox-label input {
  margin-top: 2px;
  accent-color: #d2a764;
}

.link-privacy-inline {
  color: #0284c7;
  text-decoration: underline;
  font-weight: 600;
}

.form-salutation-name-phone-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.field-salutation {
  width: 95px;
  flex-shrink: 0;
}

.select-salutation-box {
  padding: 11px 10px;
  font-weight: 700;
}

.field-fullname {
  flex: 1;
}

.field-phone {
  flex: 1;
}

@media (max-width: 640px) {
  .form-salutation-name-phone-row {
    flex-direction: column;
  }
  .field-salutation {
    width: 100%;
  }
}

.field-sub-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 4px;
  display: block;
}

.delivery-schedule-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
}

.link-detail-sub {
  color: #0284c7;
  text-decoration: underline;
  font-weight: 600;
}

.gender-radio-row {
  display: flex;
  gap: 18px;
  align-items: center;
  margin-bottom: 6px;
}

.radio-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  color: #334155;
  cursor: pointer;
  font-weight: 600;
}

.radio-inline input {
  accent-color: #d2a764;
}

.vat-info-icon {
  font-size: 12px;
  cursor: help;
}

.vat-fields-box {
  background: #f8fafc;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.mb-2 {
  margin-bottom: 8px;
}

/* Nút Mua ngay (Ảnh 4) */
.btn-gold-buy-now {
  background: #d4a373 !important;
  color: #0f172a !important;
  font-weight: 800 !important;
  font-size: 17px !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 14px rgba(212, 163, 115, 0.4) !important;
  letter-spacing: 0.5px !important;
}

.btn-gold-buy-now:hover:not(:disabled) {
  background: #e0b875 !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(212, 163, 115, 0.55) !important;
}

.auth-sep-pipe {
  color: #cbd5e1;
  font-size: 14px;
  margin: 0 4px;
}

.btn-google-cart-quick {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  padding: 6px 14px;
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.btn-google-cart-quick:hover {
  background-color: #f8fafc;
  border-color: #94a3b8;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}

.cod-truck-badge-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ghn-price-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.ghn-price-label {
  font-size: 13px;
  color: #475569;
}

.ghn-price-val {
  font-size: 14px;
  color: #0f172a;
  font-weight: 800;
}


.ghn-ship-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ghn-ship-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 14px;
}

.ghn-inline-brand-logo {
  height: 16px;
  width: auto;
  object-fit: contain;
  vertical-align: middle;
}

.cod-truck-logo-img {
  width: 38px;
  height: 28px;
  object-fit: contain;
  display: block;
}


/* TIÊU ĐỀ TRƯỜNG NHẬP (LABELS) */
.checkout-field-label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
  letter-spacing: -0.1px;
}
.req-star {
  color: #ef4444;
  margin-left: 2px;
  font-weight: 800;
}

/* ĐƠN VỊ VẬN CHUYỂN SIÊU GỌN NHẸ, TỐI GIẢN CHUẨN THƯƠNG HIỆU GHN */
.shipping-panel-slim {
  padding: 13px 18px !important;
}
.shipping-title-slim {
  font-size: 14.5px !important;
  margin-bottom: 8px !important;
  font-weight: 800;
}
.shipping-carrier-compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  transition: all 0.2s ease;
}
.shipping-carrier-compact:hover {
  border-color: #cbd5e1;
}
.scc-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.scc-radio {
  accent-color: #0f172a;
  width: 15px;
  height: 15px;
  cursor: pointer;
  margin: 0;
}
.scc-logo {
  height: 18px;
  width: auto;
  object-fit: contain;
}
.scc-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
}
.scc-right {
  display: flex;
  align-items: center;
}
.scc-price {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}
.scc-price-pending {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  font-style: italic;
}
.av-best-badge {
  font-size: 11px;
  font-weight: 700;
  color: #166534;
  background: #dcfce7;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 6px;
}

</style>
