<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import {
  cartItems,
  addToCart,
  updateCartQuantity,
  removeFromCart,
  removeMultipleFromCart,
  clearCart,
  saveCart,
  resolveProductImageUrl,
  NEUTRAL_PRODUCT_PLACEHOLDER
} from '@/utils/luuTruGioHang.js'
import { getMergedProducts } from '@/utils/danhMucSanPham.js'
import { showAppConfirm, showAppAlert, showAppToast } from '@/utils/thongBaoDialog.js'
import { FALLBACK_PROVINCES, loadAdministrativeData, getWardsForProvince } from '@/utils/diaGioiHanhChinh.js'
import ModalDangNhap from '@/components/BocCuc/ModalDangNhap.vue'
import TheSanPham from '@/components/DungChung/TheSanPham.vue'
import { currentClientUser, isClientLoggedIn, loginClient } from '@/utils/taiKhoanKhachHang.js'
import { notifyOrderSuccess, fetchNotifications } from '@/utils/thongBao.js'

const router = useRouter()
const route = useRoute()

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
  authInitialView.value = 'login'
  showLoginModal.value = true
}

// Quản lý sổ địa chỉ của khách hàng đăng nhập
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const addressModalVisible = ref(false)
const showAddressForm = ref(false)
const editingAddressId = ref(null)
const loadingAddresses = ref(false)
const savingAddress = ref(false)

const addrForm = ref({
  thanhPho: '',
  phuong: '',
  huyen: '',
  diaChiCuThe: '',
  macDinh: false
})
const addrWardsList = ref([])

const onAddrProvinceChange = async () => {
  addrForm.value.phuong = ''
  addrWardsList.value = []
  if (addrForm.value.thanhPho) {
    addrWardsList.value = await getWardsForProvince(addrForm.value.thanhPho)
  }
}

const openAddressModal = () => {
  addressModalVisible.value = true
  showAddressForm.value = false
  editingAddressId.value = null
}

const closeAddressModal = () => {
  addressModalVisible.value = false
  showAddressForm.value = false
  editingAddressId.value = null
}

const openAddAddressForm = () => {
  editingAddressId.value = null
  addrForm.value = {
    thanhPho: selectedProvince.value || '',
    phuong: '',
    huyen: '',
    diaChiCuThe: '',
    macDinh: savedAddresses.value.length === 0
  }
  if (addrForm.value.thanhPho) {
    onAddrProvinceChange()
  } else {
    addrWardsList.value = []
  }
  showAddressForm.value = true
}

const cancelAddressForm = () => {
  showAddressForm.value = false
  editingAddressId.value = null
}

const editAddress = async (addr) => {
  editingAddressId.value = addr.id
  addrForm.value = {
    thanhPho: addr.thanhPho || '',
    phuong: addr.phuong || '',
    huyen: addr.huyen || '',
    diaChiCuThe: addr.diaChiCuThe || '',
    macDinh: !!addr.macDinh
  }
  if (addr.thanhPho) {
    addrWardsList.value = await getWardsForProvince(addr.thanhPho)
  } else {
    addrWardsList.value = []
  }
  showAddressForm.value = true
}

const selectAddressForCheckout = async (addr) => {
  if (!addr) return
  selectedAddressId.value = addr.id
  streetAddress.value = addr.diaChiCuThe || ''
  selectedProvince.value = addr.thanhPho || ''
  clearError('streetAddress')
  clearError('selectedProvince')

  if (addr.thanhPho) {
    await reloadWards(addr.thanhPho)
    selectedWard.value = addr.phuong || ''
    clearError('selectedWard')
  }
  updateGhnShipping()
}

const loadCustomerAddresses = async (customerId) => {
  if (!customerId) return
  loadingAddresses.value = true
  try {
    const res = await api.get(`/api/khach-hang/${customerId}/dia-chi`)
    const list = Array.isArray(res.data) ? res.data : []
    savedAddresses.value = list

    if (list.length > 0) {
      const currentSelected = list.find(a => a.id === selectedAddressId.value)
      const defaultAddr = currentSelected || list.find(a => a.macDinh) || list[0]
      if (defaultAddr && (!streetAddress.value || !selectedProvince.value || !selectedAddressId.value)) {
        await selectAddressForCheckout(defaultAddr)
      }
    }
  } catch (err) {
    console.warn('Lỗi tải danh sách địa chỉ:', err)
  } finally {
    loadingAddresses.value = false
  }
}

const saveAddress = async () => {
  if (!addrForm.value.thanhPho || !addrForm.value.thanhPho.trim()) {
    return showAppAlert('Vui lòng chọn Tỉnh / Thành phố!', 'Thiếu thông tin', 'warning')
  }
  if (!addrForm.value.phuong || !addrForm.value.phuong.trim()) {
    return showAppAlert('Vui lòng chọn Phường / Xã!', 'Thiếu thông tin', 'warning')
  }
  if (!addrForm.value.diaChiCuThe || !addrForm.value.diaChiCuThe.trim()) {
    return showAppAlert('Vui lòng nhập địa chỉ cụ thể (số nhà, ngõ, tên đường...)!', 'Thiếu thông tin', 'warning')
  }

  const customerId = currentClientUser.value?.id
  if (!customerId) return

  savingAddress.value = true
  try {
    const payload = {
      thanhPho: addrForm.value.thanhPho.trim(),
      huyen: addrForm.value.huyen ? addrForm.value.huyen.trim() : '',
      phuong: addrForm.value.phuong.trim(),
      diaChiCuThe: addrForm.value.diaChiCuThe.trim(),
      macDinh: addrForm.value.macDinh
    }

    let saved = null
    if (editingAddressId.value) {
      const res = await api.put(`/api/khach-hang/${customerId}/dia-chi/${editingAddressId.value}`, payload)
      saved = res.data
      showAppToast('Cập nhật địa chỉ thành công!', 'success')
    } else {
      const res = await api.post(`/api/khach-hang/${customerId}/dia-chi`, payload)
      saved = res.data
      showAppToast('Thêm địa chỉ mới thành công!', 'success')
    }

    showAddressForm.value = false
    await loadCustomerAddresses(customerId)
  await fetchPublicVouchers()

    if (saved) {
      await selectAddressForCheckout(saved)
    }
  } catch (err) {
    console.error('Lỗi lưu địa chỉ:', err)
    showAppAlert(err?.response?.data?.message || 'Không thể lưu địa chỉ khách hàng.', 'Lỗi lưu địa chỉ', 'error')
  } finally {
    savingAddress.value = false
  }
}

const setAsDefault = async (addressId) => {
  const customerId = currentClientUser.value?.id
  if (!customerId) return
  try {
    await api.put(`/api/khach-hang/${customerId}/dia-chi/${addressId}/mac-dinh`)
    showAppToast('Đã đặt làm địa chỉ mặc định!', 'success')
    await loadCustomerAddresses(customerId)
  await fetchPublicVouchers()
  } catch (err) {
    showAppAlert(err?.response?.data?.message || 'Không thể đặt địa chỉ mặc định.', 'Lỗi', 'error')
  }
}

const deleteAddress = async (addressId) => {
  const confirmed = await showAppConfirm('Bạn có chắc chắn muốn xóa địa chỉ này khỏi danh sách không?', 'Xác nhận xóa địa chỉ')
  if (!confirmed) return

  const customerId = currentClientUser.value?.id
  if (!customerId) return
  try {
    await api.delete(`/api/khach-hang/${customerId}/dia-chi/${addressId}`)
    showAppToast('Đã xóa địa chỉ thành công!', 'success')
    await loadCustomerAddresses(customerId)
  await fetchPublicVouchers()
    if (selectedAddressId.value === addressId) {
      selectedAddressId.value = null
      streetAddress.value = ''
      selectedProvince.value = ''
      selectedWard.value = ''
    }
  } catch (err) {
    showAppAlert(err?.response?.data?.message || 'Không thể xóa địa chỉ.', 'Lỗi', 'error')
  }
}

const syncCustomerProfileAndAddresses = async (forceSync = false) => {
  if (!isClientLoggedIn.value || !currentClientUser.value) {
    return
  }

  // Tự động điền họ tên, số điện thoại, email từ thông tin đăng nhập
  if (forceSync || !customerName.value) {
    customerName.value = currentClientUser.value.name || ''
  }
  if (forceSync || !customerEmail.value) {
    customerEmail.value = currentClientUser.value.email || ''
  }
  if (forceSync || !customerPhone.value) {
    customerPhone.value = currentClientUser.value.phone || ''
  }

  const customerId = currentClientUser.value.id
  if (!customerId) return

  try {
    const custRes = await api.get(`/api/khach-hang/${customerId}`)
    if (custRes.data) {
      if ((!customerPhone.value || forceSync) && custRes.data.soDienThoai) {
        customerPhone.value = custRes.data.soDienThoai
      }
      if ((!customerName.value || forceSync) && custRes.data.tenKhachHang) {
        customerName.value = custRes.data.tenKhachHang
      }
      if ((!customerEmail.value || forceSync) && custRes.data.email) {
        customerEmail.value = custRes.data.email
      }
      if (custRes.data.maKhachHang && !currentClientUser.value.maKhachHang) {
        currentClientUser.value.maKhachHang = custRes.data.maKhachHang
      }
    }
  } catch (e) {
    console.warn('Không thể lấy chi tiết khách hàng:', e)
  }

  await loadCustomerAddresses(customerId)
  await fetchPublicVouchers()
}

watch(currentClientUser, async (newVal) => {
  if (newVal) {
    await syncCustomerProfileAndAddresses()
  } else {
    await resetCustomerFormToDefault()
    // Hoàn tất đặt lại trạng thái mặc định
  }
})

const handleLoginSuccess = async (userData) => {
  if (userData) {
    await syncCustomerProfileAndAddresses(true)
    await fetchPublicVouchers()
  }
}

const _oldLoginHelper = (userData) => {
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
const currentStep = ref('cart') // 'cart' | 'checkout'

const goToCheckoutStep = () => {
  if (cartItems.value.length === 0) {
    showAppToast('Giỏ hàng của bạn đang trống!', 'warning')
    return
  }
  if (selectedItemIds.value.length === 0) {
    showAppToast('Vui lòng chọn ít nhất 1 sản phẩm để tiến hành thanh toán!', 'warning')
    return
  }
  currentStep.value = 'checkout'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const backToCartStep = () => {
  currentStep.value = 'cart'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const resolveProductImg = (item) => {
  const raw = item?.image || item?.anh || item?.hinhAnh || item?.anhDaiDien
  return resolveProductImageUrl(raw)
}
const disabledOldResolve = () => {
  const raw = item?.image || item?.anh || item?.hinhAnh
  if (!raw) return 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200&q=80'
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  return `http://localhost:8080${raw.startsWith('/') ? '' : '/'}${raw}`
}

const onImgError = (e) => {
  e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER
}

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
const allVouchers = ref([])

const voucherTab = ref('ALL') // 'ALL' | 'PERSONAL' | 'PUBLIC'

const customerHasAccessToVoucher = (v) => {
  if (!v) return false
  if (!isVoucherPersonal(v)) return true // Mã công khai

  // Mã cá nhân: bắt buộc phải có tài khoản đăng nhập
  if (!isClientLoggedIn.value || !currentClientUser.value) {
    return false
  }

  const userId = currentClientUser.value.id
  const userName = (currentClientUser.value.name || '').trim().toLowerCase()
  const userCode = (currentClientUser.value.maKhachHang || '').trim().toLowerCase()
  const userEmail = (currentClientUser.value.email || '').trim().toLowerCase()
  const userPhone = (currentClientUser.value.phone || '').trim().toLowerCase()

  // 1. Kiểm tra qua idKhachHangList (Long/Number/String)
  if (Array.isArray(v.idKhachHangList) && v.idKhachHangList.length > 0) {
    if (userId && v.idKhachHangList.some(id => String(id) === String(userId))) {
      return true
    }
  }

  // 2. Kiểm tra qua danhSachKhachHang (tên khách, mã khách KHxxx, email, sđt)
  if (Array.isArray(v.danhSachKhachHang) && v.danhSachKhachHang.length > 0) {
    const match = v.danhSachKhachHang.some(item => {
      if (!item) return false
      const s = String(item).trim().toLowerCase()
      return (userName && s === userName) ||
             (userCode && s === userCode) ||
             (userEmail && s === userEmail) ||
             (userPhone && s === userPhone) ||
             (userName && (s.includes(userName) || userName.includes(s)))
    })
    if (match) return true
  }

  return false
}

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
    allVouchers.value = list
    const nowTime = Date.now()
    const isValidDate = (d) => d && !isNaN(new Date(d).getTime())
    availableVouchers.value = list.filter(v => {
      // Chỉ fill các mã CÔNG KHAI
      // Nếu là mã cá nhân, chỉ giữ lại nếu khách hàng hiện tại được tặng
      if (isVoucherPersonal(v)) {
        return customerHasAccessToVoucher(v)
      }
      if (v.trangThai != null && Number(v.trangThai) !== 1) return false
      if (v.soLuongSuDung != null && v.soLuongSuDung <= 0) return false
      if (isValidDate(v.ngayBatDau) && new Date(v.ngayBatDau).getTime() > nowTime) return false
      if (isValidDate(v.ngayKetThuc) && new Date(v.ngayKetThuc).getTime() < nowTime) return false
      return true
    })
    // Khách hàng bấm áp dụng thì mới áp dụng mã
  } catch (e) {
    console.warn('Lỗi tải phiếu giảm giá công khai:', e)
  }
}

const autoApplyBestVoucher = () => {
  return // Đã tắt tự động áp dụng mã, chỉ áp dụng khi khách bấm
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

const personalVouchersCount = computed(() => {
  return availableVouchers.value.filter(v => isVoucherPersonal(v)).length
})

const publicVouchersCount = computed(() => {
  return availableVouchers.value.filter(v => !isVoucherPersonal(v)).length
})

const hasPersonalVouchers = computed(() => {
  return personalVouchersCount.value > 0
})

// Tính toán mã giảm giá tốt nhất / phù hợp nhất với đơn hàng hiện tại
const calculatedBestVoucherId = computed(() => {
  if (!availableVouchers.value || availableVouchers.value.length === 0) return null
  const sub = Number(selectedSubtotal.value || 0)

  // 1. Tìm trong các mã thỏa mãn điều kiện đơn tối thiểu của giỏ hàng hiện tại
  const eligibleVouchers = availableVouchers.value.filter(v => {
    const minSpend = Number(v.hoaDonToiThieu || 0)
    return sub <= 0 || sub >= minSpend
  })

  if (eligibleVouchers.length > 0) {
    const sorted = [...eligibleVouchers].sort((a, b) => {
      if (sub > 0) {
        const discA = calculateDiscountForVoucher(a, sub)
        const discB = calculateDiscountForVoucher(b, sub)
        if (discB !== discA) return discB - discA
      }
      return Number(a.hoaDonToiThieu || 0) - Number(b.hoaDonToiThieu || 0)
    })
    return sorted[0]?.id || null
  }

  // 2. Nếu đơn hàng chưa đủ điều kiện cho mã nào, gợi ý mã có mức yêu cầu đơn thấp nhất
  const fallbackSorted = [...availableVouchers.value].sort((a, b) => {
    return Number(a.hoaDonToiThieu || 0) - Number(b.hoaDonToiThieu || 0)
  })
  return fallbackSorted[0]?.id || null
})

// Đưa phiếu giảm giá tốt nhất lên đầu danh sách
const sortedAvailableVouchers = computed(() => {
  if (!availableVouchers.value || availableVouchers.value.length === 0) return []
  const sub = Number(selectedSubtotal.value || 0)

  let list = [...availableVouchers.value]
  if (voucherTab.value === 'PERSONAL') {
    list = list.filter(v => isVoucherPersonal(v))
  } else if (voucherTab.value === 'PUBLIC') {
    list = list.filter(v => !isVoucherPersonal(v))
  }

  return list.sort((a, b) => {
    const isPersonalA = isVoucherPersonal(a) ? 1 : 0
    const isPersonalB = isVoucherPersonal(b) ? 1 : 0

    // Ở tab Tất cả, ưu tiên đưa phiếu cá nhân của khách lên trước
    const bestId = calculatedBestVoucherId.value
    if (a.id === bestId) return -1
    if (b.id === bestId) return 1
    if (false) {
      return isPersonalB - isPersonalA
    }
    // 1. Nếu giỏ hàng có tiền, ưu tiên phiếu đủ điều kiện và mức giảm thực tế cao nhất
    if (sub > 0) {
      const discA = calculateDiscountForVoucher(a, sub)
      const discB = calculateDiscountForVoucher(b, sub)
      if (discB !== discA) return discB - discA
    }

    // 2. So sánh tiềm năng giảm giá
    const valA = Number(a.giaTriGiamGia || 0)
    const valB = Number(b.giaTriGiamGia || 0)
    if (valB !== valA) return valB - valA

    // 3. Ưu tiên điều kiện đơn tối thiểu nhỏ hơn
    return Number(a.hoaDonToiThieu || 0) - Number(b.hoaDonToiThieu || 0)
  })
})

const bestVoucherId = computed(() => {
  if (!sortedAvailableVouchers.value || sortedAvailableVouchers.value.length === 0) return null
  return calculatedBestVoucherId.value
})

const handleApplyVoucher = async (codeToApply) => {
  const code = (codeToApply || voucherInput.value || '').trim().toUpperCase()
  if (!code) return
  if (selectedSubtotal.value <= 0) {
    await showAppAlert('Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng trước khi áp dụng mã giảm giá!', 'Chưa chọn sản phẩm', 'warning')
    return
  }

  let voucher = availableVouchers.value.find(v => (v.maPhieuGiamGia || '').toUpperCase() === code)
  if (!voucher) {
    voucher = allVouchers.value.find(v => (v.maPhieuGiamGia || '').toUpperCase() === code)
    if (voucher && isVoucherPersonal(voucher)) {
      const currentUserId = currentClientUser.value?.id
      const hasAccess = customerHasAccessToVoucher(voucher)
      if (!hasAccess) {
        await showAppAlert('Mã giảm giá này là mã riêng tư và không áp dụng cho tài khoản của bạn!', 'Mã không hợp lệ', 'error')
        return
      }
    }
  }
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
  if (sortedAvailableVouchers.value && sortedAvailableVouchers.value.length > 0) {
    sortedAvailableVouchers.value.forEach(v => {
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
  // Chỉ cập nhật lại số tiền giảm nếu khách ĐÃ ÁP DỤNG mã trước đó
  if (appliedVoucher.value && appliedVoucher.value.raw) {
    const minSpend = Number(appliedVoucher.value.minSpend || 0)
    if (newSub < minSpend) {
      appliedVoucher.value = null
    } else {
      appliedVoucher.value.discount = calculateDiscountForVoucher(appliedVoucher.value.raw, newSub)
    }
  }
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

async function resetCustomerFormToDefault() {
  customerName.value = ''
  customerPhone.value = ''
  customerEmail.value = ''
  streetAddress.value = ''
  selectedProvince.value = ''
  selectedWard.value = ''
  currentWards.value = []
  savedAddresses.value = []
  selectedAddressId.value = null
  addressModalVisible.value = false
  showAddressForm.value = false
  voucherTab.value = 'ALL'
  voucherInput.value = ''

  if (appliedVoucher.value && isVoucherPersonal(appliedVoucher.value.raw)) {
    appliedVoucher.value = null
  }

  errors.value = {
    customerName: '',
    customerPhone: '',
    customerEmail: '',
    selectedProvince: '',
    selectedWard: '',
    streetAddress: ''
  }

  calculatedGhnFee.value = 30000
  ghnLeadTime.value = ''

  await fetchPublicVouchers()
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
const showConfirmOrderModal = ref(false)
const orderSuccess = ref(false)
const createdOrder = ref(null)
const copySuccess = ref(false)

// Quản lý trạng thái chờ thanh toán VNPAY
const showVnpayWaitingModal = ref(false)
const vnpayWaitingOrder = ref(null)
const vnpayPaymentUrl = ref('')
const vnpayChecking = ref(false)
let vnpayPollTimer = null
let vnpayMessageHandler = null

const stopVnpayTracking = () => {
  if (vnpayPollTimer) {
    clearInterval(vnpayPollTimer)
    vnpayPollTimer = null
  }
  if (vnpayMessageHandler) {
    window.removeEventListener('message', vnpayMessageHandler)
    vnpayMessageHandler = null
  }
}

const openVnpayPopup = (url) => {
  const targetUrl = url || vnpayPaymentUrl.value
  if (!targetUrl) return
  const width = 850
  const height = 750
  const left = Math.max(0, (window.screen.width - width) / 2)
  const top = Math.max(0, (window.screen.height - height) / 2)
  window.open(
    targetUrl,
    'VNPayPaymentPopup',
    `width=${width},height=${height},top=${top},left=${left},resizable=yes,scrollbars=yes,status=yes`
  )
}

const onVnpaySuccess = (orderCode) => {
  stopVnpayTracking()
  showVnpayWaitingModal.value = false
  showAppToast(`Thanh toán VNPAY thành công cho đơn hàng #${orderCode}!`, 'success')
  if (currentClientUser.value?.id) {
    router.push(`/don-hang?code=${orderCode}`)
  } else {
    router.push(`/tra-cuu?code=${orderCode}`)
  }
}

const checkVnpayStatus = async (isManual = false) => {
  const code = vnpayWaitingOrder.value?.maHoaDon
  if (!code) return
  if (isManual) vnpayChecking.value = true
  try {
    const res = await api.get(`/api/hoa-don/code/${code}`)
    if (res.data && Number(res.data.trangThaiThanhToan) === 1) {
      onVnpaySuccess(code)
      return
    }
    if (isManual) {
      showAppToast('Hệ thống chưa ghi nhận thanh toán. Vui lòng thử lại sau khi hoàn tất chuyển khoản!', 'info')
    }
  } catch (err) {
    if (isManual) {
      showAppToast('Chưa thể kiểm tra trạng thái lúc này. Vui lòng thử lại sau!', 'warning')
    }
  } finally {
    if (isManual) vnpayChecking.value = false
  }
}

const skipVnpayWaitingAndNavigate = () => {
  const code = vnpayWaitingOrder.value?.maHoaDon
  stopVnpayTracking()
  showVnpayWaitingModal.value = false
  if (code) {
    if (currentClientUser.value?.id) {
      router.push(`/don-hang?code=${code}`)
    } else {
      router.push(`/tra-cuu?code=${code}`)
    }
  }
}

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

  showConfirmOrderModal.value = true
}

const executePlaceOrder = async () => {
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
        showConfirmOrderModal.value = false
        const width = 850
        const height = 750
        const left = Math.max(0, (window.screen.width - width) / 2)
        const top = Math.max(0, (window.screen.height - height) / 2)
        window.open(
          vnpRes.data.paymentUrl,
          'VNPayPaymentPopup',
          `width=${width},height=${height},top=${top},left=${left},resizable=yes,scrollbars=yes,status=yes`
        )
        vnpayPaymentUrl.value = vnpRes.data.paymentUrl
        vnpayWaitingOrder.value = savedOrder
        showVnpayWaitingModal.value = true
        stopVnpayTracking()
        vnpayMessageHandler = (event) => {
          if (event.data && (event.data.type === 'VNPAY_PAID_SUCCESS' || event.data.type === 'VNPAY_PAYMENT_RESULT')) {
            if (event.data.type === 'VNPAY_PAID_SUCCESS' || event.data.success) {
              onVnpaySuccess(savedOrder.maHoaDon)
            }
          }
        }
        window.addEventListener('message', vnpayMessageHandler)
        vnpayPollTimer = setInterval(() => {
          checkVnpayStatus(false)
        }, 2500)
        return /* VNPAY_WAITING_REPLACE */
        if (currentClientUser.value?.id) {
          router.push(`/don-hang?code=${savedOrder.maHoaDon}`)
        } else {
          router.push(`/tra-cuu?code=${savedOrder.maHoaDon}`)
        }
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
    showConfirmOrderModal.value = false
    showAppToast(`Đặt hàng thành công! Mã đơn hàng #${savedOrder.maHoaDon}`, 'success')

    // Nhảy sang luôn phần chi tiết đơn hàng đó
    if (currentClientUser.value?.id) {
      router.push(`/don-hang?code=${savedOrder.maHoaDon}`)
    } else {
      router.push(`/tra-cuu?code=${savedOrder.maHoaDon}`)
    }
    return
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
        // Tự động đồng bộ đúng ảnh thật của biến thể từ CSDL
        const realVariantImg = res.data.anhDaiDien || res.data.hinhAnh || ''
        if (realVariantImg) {
          if (item.image !== realVariantImg) {
            item.image = realVariantImg
            updated = true
          }
        } else if (item.image && (item.image.includes('unsplash') || item.image.includes('default_tshirt'))) {
          item.image = ''
          updated = true
        }

        const freshPrice = Number(res.data.giaBan != null ? res.data.giaBan : item.price)
        const hasDiscount = Boolean(res.data.coGiamGia && res.data.giaSauGiam && Number(res.data.giaSauGiam) < freshPrice)
        const freshDiscountPrice = hasDiscount ? Number(res.data.giaSauGiam) : freshPrice

        if (item.price !== freshPrice || item.discountPrice !== freshDiscountPrice || Boolean(item.coGiamGia) !== hasDiscount) {
          item.price = freshPrice
          item.discountPrice = freshDiscountPrice
          item.coGiamGia = hasDiscount
          item.phanTramGiam = res.data.phanTramGiam != null ? Number(res.data.phanTramGiam) : 0
          item.tenDotGiamGia = res.data.tenDotGiamGia || ''
          updated = true
        }
        if (false) {
        // sync image done
          updated = true
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
  if (route.query.step === 'checkout') {
    currentStep.value = 'checkout'
  }
  syncCartStock()
  if (typeof window !== 'undefined') {
    window.addEventListener('client-auth-change', () => {
      if (currentClientUser.value) {
        syncCustomerProfileAndAddresses()
      } else {
        resetCustomerFormToDefault()
      }
    })
  }
  await syncCustomerProfileAndAddresses()
  loadRecommendedProducts()
  fetchPublicVouchers()

  const allData = await loadAdministrativeData()
  if (allData && allData.length > 0) {
    provincesList.value = allData
  }
  await reloadWards(selectedProvince.value)
  updateGhnShipping()
})

onUnmounted(() => {
  stopVnpayTracking()
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
          <button class="btn-primary-action" @click="currentClientUser?.id ? router.push(`/don-hang?code=${createdOrder?.maHoaDon}`) : router.push(`/tra-cuu?code=${createdOrder?.maHoaDon}`)">
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
      <!-- TRƯỜNG HỢP 1: GIỎ HÀNG TRỐNG -->
      <div v-if="cartItems.length === 0" class="empty-cart-card">
        <div class="empty-icon">🛒</div>
        <h3 class="empty-title">Giỏ hàng của bạn đang trống</h3>
        <p class="empty-sub">Hãy khám phá các mẫu áo phông phong cách mới nhất để bổ sung vào giỏ hàng nhé!</p>
        <router-link to="/cua-hang" class="btn-goto-shop">Khám phá cửa hàng ngay</router-link>
      </div>

      <!-- TRƯỜNG HỢP 2: BƯỚC 1 - GIAO DIỆN GIỎ HÀNG (MATCH ẢNH MẪU) -->
      <div v-else-if="currentStep === 'cart'" class="cart-step-wrapper">
        <!-- Tiêu đề lãng mạn Giỏ Hàng -->
        <div class="cart-step-hero">
          <h1 class="cart-fancy-title">Giỏ Hàng</h1>
          <p class="cart-fancy-sub">Kiểm tra sản phẩm, số lượng trước khi tiến hành đặt hàng</p>
        </div>

        <!-- Bố cục 2 cột: Bảng sản phẩm & Tóm tắt -->
        <div class="cart-step-layout">
          <!-- BẢNG SẢN PHẨM BÊN TRÁI -->
          <div class="cart-step-table-card">
            <!-- Header bảng -->
            <div class="table-header-row">
              <div class="col-head col-prod">
                <label class="custom-checkbox-label">
                  <input
                    type="checkbox"
                    :checked="isAllSelected"
                    @change="toggleSelectAll"
                  />
                  <span class="custom-check-box"></span>
                </label>
                <span class="head-text">SẢN PHẨM</span>
              </div>
              <div class="col-head col-price">ĐƠN GIÁ</div>
              <div class="col-head col-discount">GIẢM GIÁ</div>
              <div class="col-head col-qty">SỐ LƯỢNG</div>
              <div class="col-head col-total">TỔNG TIỀN</div>
            </div>

            <!-- Danh sách từng sản phẩm -->
            <div class="table-items-body">
              <div
                v-for="item in cartItems"
                :key="item.variantId"
                class="cart-item-table-row"
              >
                <!-- Checkbox + Ảnh + Tên + Size + Tồn kho -->
                <div class="col-cell col-prod">
                  <label class="custom-checkbox-label">
                    <input
                      type="checkbox"
                      :value="item.variantId"
                      v-model="selectedItemIds"
                    />
                    <span class="custom-check-box"></span>
                  </label>

                  <img
                    :src="resolveProductImg(item)"
                    :alt="item.productName"
                    class="prod-table-pic"
                    @error="onImgError"
                  />

                  <div class="prod-table-details">
                    <h4 class="prod-table-name">
                      {{ item.productName }} <span v-if="item.color">({{ item.color }})</span>
                    </h4>
                    <div class="prod-table-badges" v-if="item.size">
                      <span class="badge-size-tag">Size: {{ item.size }}</span>
                    </div>
                    <span class="prod-table-stock">Còn lại: {{ item.maxStock || 100 }}</span>
                  </div>
                </div>

                <!-- Đơn giá: Giá hiện tại + Giá gốc gạch ngang -->
                <div class="col-cell col-price">
                  <div class="price-stack">
                    <span class="price-sale">{{ formatCurrency(item.discountPrice > 0 ? item.discountPrice : item.price) }}</span>
                    <span v-if="item.price > item.discountPrice && item.discountPrice > 0" class="price-origin">
                      {{ formatCurrency(item.price) }}
                    </span>
                  </div>
                </div>

                <!-- Giảm giá: Badge xanh % -->
                <div class="col-cell col-discount">
                  <span
                    v-if="item.price > item.discountPrice && item.discountPrice > 0"
                    class="discount-percent-badge"
                  >
                    🏷️ {{ Math.round((item.price - item.discountPrice) / item.price * 100) }}%
                  </span>
                  <span v-else class="text-dash">—</span>
                </div>

                <!-- Số lượng: Nút - và + -->
                <div class="col-cell col-qty">
                  <div class="qty-pill-box">
                    <button
                      type="button"
                      class="qty-pill-btn"
                      :disabled="item.quantity <= 1"
                      @click="handleQtyChange(item.variantId, -1, item.quantity, item.maxStock)"
                    >−</button>
                    <span class="qty-pill-val">{{ item.quantity }}</span>
                    <button
                      type="button"
                      class="qty-pill-btn"
                      :disabled="item.maxStock != null && item.quantity >= item.maxStock"
                      @click="handleQtyChange(item.variantId, 1, item.quantity, item.maxStock)"
                    >+</button>
                  </div>
                </div>

                <!-- Tổng tiền + Nút xóa thùng rác -->
                <div class="col-cell col-total">
                  <span class="item-line-total">
                    {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
                  </span>
                  <button
                    type="button"
                    class="btn-trash-row"
                    title="Xóa sản phẩm"
                    @click="handleRemove(item.variantId, item.productName)"
                  >
                    🗑️
                  </button>
                </div>
              </div>
            </div>

            <!-- Footer: Link chọn thêm sản phẩm khác -->
            <div class="table-footer-row">
              <router-link to="/cua-hang" class="link-add-more">
                ← Chọn thêm sản phẩm khác
              </router-link>
            </div>
          </div>

          <!-- CARD TÓM TẮT ĐƠN HÀNG BÊN PHẢI -->
          <div class="cart-step-summary-card">
            <h3 class="summary-card-title">TÓM TẮT ĐƠN HÀNG</h3>
            <div class="summary-card-body">
              <div class="sum-row-item">
                <span class="sum-label">Tổng sản phẩm chọn:</span>
                <strong class="sum-val">{{ activeCheckoutItems.reduce((acc, it) => acc + it.quantity, 0) }}</strong>
              </div>
              <div class="sum-row-item">
                <span class="sum-label">Tạm tính:</span>
                <span class="sum-val font-semibold">{{ formatCurrency(selectedSubtotal) }}</span>
              </div>
              <div class="sum-card-sep"></div>
              <div class="sum-row-item sum-row-total">
                <span class="sum-total-label">Thành tiền</span>
                <strong class="sum-total-value">{{ formatCurrency(selectedSubtotal) }}</strong>
              </div>
              <button
                type="button"
                class="btn-action-proceed"
                :disabled="selectedItemIds.length === 0"
                @click="goToCheckoutStep"
              >
                TIẾN HÀNH THANH TOÁN
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- TRƯỜNG HỢP 3: BƯỚC 2 - TIẾN HÀNH THANH TOÁN (GIAO DIỆN HIỆN TẠI) -->
      <div v-else class="checkout-step-container">
        <!-- Thanh điều hướng quay lại Giỏ hàng -->
        <div class="checkout-back-bar" v-if="false">
          <button type="button" class="btn-step-back-cart" @click="backToCartStep">
            ← Quay lại giỏ hàng (Chỉnh sửa số lượng &amp; sản phẩm)
          </button>
          <div class="checkout-flow-breadcrumbs">
            <span class="flow-step clickable" @click="backToCartStep">1. Giỏ hàng</span>
            <span class="flow-sep">›</span>
            <span class="flow-step current">2. Tiến hành thanh toán</span>
          </div>
        </div>

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
              <!-- hidden google button -->
              <button v-if="false" type="button" class="btn-google-cart-quick" @click="openGoogleLogin">
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
            <div class="panel-header-with-action">
              <h3 class="panel-section-title">Thông tin giao hàng</h3>
              <button

                type="button"
                v-if="false"
                class="btn-manage-addr-trigger"
                @click="openAddressModal"
                title="Xem và quản lý tất cả các địa chỉ đã lưu"
              >
                📍 Sổ địa chỉ ({{ savedAddresses.length }})
              </button>
            </div>

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


            <!-- THANH THAO TÁC ĐỊA CHỈ CHO KHÁCH HÀNG ĐÃ ĐĂNG NHẬP (CÓ NÚT THÊM ĐỊA CHỈ MỚI) -->
            <div v-if="isClientLoggedIn" class="address-book-toolbar mt-3">
              <div class="abt-left">
                <span v-if="savedAddresses.length > 0" class="abt-info">
                  <span class="abt-dot">✓</span> Địa chỉ đang chọn: <strong>{{ savedAddresses.find(a => a.id === selectedAddressId)?.diaChiCuThe || streetAddress || 'Mặc định' }}</strong>
                </span>
                <span v-else class="abt-info text-muted">
                  Bạn chưa có địa chỉ nào được lưu
                </span>
              </div>
              <div class="abt-right">
                <button
                  type="button"
                  v-if="false"
                  class="btn-add-new-addr-action"
                  @click="openAddAddressForm(); addressModalVisible = true"
                >
                  + Thêm địa chỉ mới
                </button>
                <button

                  type="button"
                  class="btn-open-addr-list-action"
                  @click="openAddressModal"
                >
                  Đổi địa chỉ{{ savedAddresses.length > 0 ? ` (${savedAddresses.length})` : '' }}
                </button>
              </div>
            </div>
            <div v-if="false" class="delivery-schedule-notice mt-2">


            </div>
          </div>

          <!-- 3. Đơn vị vận chuyển (GHN Express thiết kế siêu gọn nhẹ) -->
          <div class="section-card-panel shipping-panel-slim mt-3" v-if="false">
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
              </label><div style="display:none"><div>
                </div>
              </div>

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
              </label><div style="display:none"><div>
                </div>
              </div>
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
              <span class="checkout-items-title">Sản phẩm thanh toán ({{ activeCheckoutItems.length }})</span><div style="display:none">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleSelectAll"
                />

              </div>
              <button type="button" class="btn-edit-cart-link" @click="backToCartStep" title="Quay lại giỏ hàng để chọn lại hoặc thay đổi">
                ✏️ Thay đổi sản phẩm
              </button>
            </div>

            <div v-if="activeCheckoutItems.length > 0" class="cart-items-list-scroll">
              <div
                v-for="item in activeCheckoutItems"
                :key="item.variantId"
                class="cart-item-row"
              >
                <!-- Checkbox chọn item -->
                <div class="item-checkbox" style="display:none">
                  <input
                    type="checkbox"
                    :value="item.variantId"
                    v-model="selectedItemIds"
                  />
                </div>

                <!-- Ảnh sản phẩm -->
                <div class="item-thumb-box">
                  <img :src="resolveProductImg(item)" :alt="item.productName" @error="onImgError" />
                </div>

                <!-- Thông tin & Stepper -->
                <div class="item-details-col">
                  <h4 class="item-product-name">{{ item.productName }}</h4>
                  <div class="item-variant-pill">
                    {{ item.color }} / Size {{ item.size }}
                  </div>

                  <div class="item-qty-price-row">
                    <!-- Nút số lượng -->
                    <span class="checkout-static-qty">Số lượng: <strong>{{ item.quantity }}</strong></span><div class="qty-stepper-box" style="display:none">
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
                  class="btn-del-single" style="display:none !important;"
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
              <h4 class="cei-title">Chưa có sản phẩm nào để thanh toán</h4>
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
                placeholder="Nhập mã giảm giá..."
                class="form-input-text voucher-text-input"
                @keyup.enter="handleApplyVoucher()"
              />
              <button
                type="button"
                class="btn-apply-voucher"
                @click="handleApplyVoucher()"
                :disabled="!voucherInput.trim()"
              >
                Áp dụng
              </button>
            </div>

            <!-- GỢI Ý MÃ GIẢM GIÁ (Chỉ hiển thị khi có mã công khai từ Admin DB) -->
            <div v-if="false" class="suggested-vouchers-row">
              <span class="sv-label">Gợi ý mã:</span>
              <div class="sv-tags-wrap">
                <button
                  v-for="code in suggestedCodes"
                  :key="code"
                  type="button"
                  class="btn-suggest-code-tag"
                  :class="{
                    active: appliedVoucher?.code === code,
                    'is-personal-tag': isVoucherPersonal(availableVouchers.find(v => v.maPhieuGiamGia === code))
                  }"
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

                    <span class="av-best-badge" v-if="appliedVoucher.id === bestVoucherId">Tốt nhất</span>
                  </div>
                  <span class="av-disc">Đã giảm -{{ formatCurrency(appliedVoucher.discount) }}</span>
                </div>
              </div>
              <button type="button" class="btn-remove-v" @click="handleRemoveVoucher" title="Bỏ áp dụng mã">
                ✕
              </button>
            </div>

            <!-- Bộ lọc phân loại phiếu: Tất cả | Cá nhân | Công khai -->
            <div v-if="hasPersonalVouchers" class="voucher-filter-tabs">
              <button
                type="button"
                class="btn-v-tab"
                :class="{ active: voucherTab === 'ALL' }"
                @click="voucherTab = 'ALL'"
              >
                Tất cả ({{ availableVouchers.length }})
              </button>
              <button
                type="button"
                class="btn-v-tab btn-v-tab-personal"
                :class="{ active: voucherTab === 'PERSONAL' }"
                @click="voucherTab = 'PERSONAL'"
              >
                Dành riêng cho bạn ({{ personalVouchersCount }})
              </button>
              <button
                type="button"
                class="btn-v-tab"
                :class="{ active: voucherTab === 'PUBLIC' }"
                @click="voucherTab = 'PUBLIC'"
              >
                Toàn sàn ({{ publicVouchersCount }})
              </button>
            </div>

            <!-- Danh sách thẻ voucher CÔNG KHAI & CÁ NHÂN từ Admin -->
            <div v-if="sortedAvailableVouchers.length > 0" class="public-coupons-carousel">
              <div
                v-for="v in sortedAvailableVouchers"
                :key="v.id"
                class="coupon-card-ticket"
                :class="{ active: appliedVoucher?.id === v.id }"
                @click="applyCouponDirectly(v)"
              >
                <div class="ticket-left">
                  <div style="display: flex; align-items: center; gap: 6px;">
                    <div class="ticket-badge">{{ v.maPhieuGiamGia }}</div>

                    <span v-if="v.id === bestVoucherId" class="av-best-badge" style="margin-left: 0;">Tốt nhất</span>
                  </div>
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
            <div v-if="false" class="referral-code-link-row">
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

    </div>

    <!-- Modal Đăng nhập / Đăng ký nếu khách muốn đăng nhập -->
    <ModalDangNhap
      v-model="showLoginModal"
      :initial-view="authInitialView"
      @login-success="handleLoginSuccess"
    />

    <!-- POPUP XÁC NHẬN ĐƠN HÀNG TRƯỚC KHI ĐẶT -->
    <Teleport to="body">
      <div v-if="showConfirmOrderModal" class="client-confirm-order-backdrop" @click.self="showConfirmOrderModal = false">
        <div class="client-confirm-order-card animate-pop">
          <!-- Header Modal -->
          <div class="confirm-modal-header">
            <div class="confirm-header-left">
              <span class="confirm-header-icon">📋</span>
              <div>
                <h3 class="confirm-modal-title">Xác nhận thông tin đơn hàng</h3>
                <p class="confirm-modal-sub">Vui lòng kiểm tra lại thông tin trước khi hoàn tất đặt hàng</p>
              </div>
            </div>
            <button class="confirm-close-btn" @click="showConfirmOrderModal = false" title="Đóng">&times;</button>
          </div>

          <!-- Body Modal -->
          <div class="confirm-modal-body">
            <!-- Box 1: Thông tin khách hàng & Giao hàng -->
            <div class="confirm-info-box">
              <h4 class="confirm-box-title">
                <span>📍 Thông tin người nhận & Giao hàng</span>
              </h4>
              <div class="confirm-meta-grid">
                <div class="confirm-meta-item">
                  <span class="c-label">Họ và tên:</span>
                  <strong class="c-val">{{ customerName }}</strong>
                </div>
                <div class="confirm-meta-item">
                  <span class="c-label">Số điện thoại:</span>
                  <strong class="c-val">{{ customerPhone }}</strong>
                </div>
                <div class="confirm-meta-item full-row" v-if="customerEmail">
                  <span class="c-label">Email:</span>
                  <span class="c-val">{{ customerEmail }}</span>
                </div>
                <div class="confirm-meta-item full-row">
                  <span class="c-label">Địa chỉ nhận hàng:</span>
                  <span class="c-val font-medium">{{ fullDeliveryAddress }}</span>
                </div>
                <div class="confirm-meta-item full-row">
                  <span class="c-label">Hình thức thanh toán:</span>
                  <span class="c-val font-semibold text-primary">
                    {{ paymentMethod === 'VNPAY' ? 'Cổng thanh toán trực tuyến VNPAY' : 'Thanh toán tiền mặt khi nhận hàng (COD)' }}
                  </span>
                </div>
                <div class="confirm-meta-item full-row">
                  <span class="c-label">Đơn vị vận chuyển:</span>
                  <span class="c-val font-semibold">Giao Hàng Nhanh (GHN Express)</span>
                </div>
              </div>
            </div>

            <!-- Box 2: Tóm tắt sản phẩm đặt mua -->
            <div class="confirm-info-box">
              <h4 class="confirm-box-title">
                <span>🛍️ Sản phẩm đặt mua ({{ activeCheckoutItems.length }})</span>
              </h4>
              <div class="confirm-items-list">
                <div v-for="item in activeCheckoutItems" :key="item.variantId" class="confirm-item-row">
                  <img :src="resolveProductImg(item)" :alt="item.productName" class="confirm-item-img" @error="onImgError" />
                  <div class="confirm-item-details">
                    <div class="confirm-item-name" :title="item.productName">{{ item.productName }}</div>
                    <div class="confirm-item-attr">
                      <span v-if="item.colorName">Màu: {{ item.colorName }}</span>
                      <span v-if="item.sizeName">Size: {{ item.sizeName }}</span>
                      <span class="confirm-item-qty">x{{ item.quantity }}</span>
                    </div>
                  </div>
                  <div class="confirm-item-price">
                    {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
                  </div>
                </div>
              </div>
            </div>

            <!-- Box 3: Tóm tắt chi phí & Tổng tiền -->
            <div class="confirm-info-box confirm-pricing-box">
              <h4 class="confirm-box-title">
                <span>💰 Tóm tắt thanh toán</span>
              </h4>
              <div class="confirm-fee-rows">
                <div class="confirm-fee-line">
                  <span>Tổng tiền hàng:</span>
                  <strong>{{ formatCurrency(selectedSubtotal) }}</strong>
                </div>
                <div class="confirm-fee-line">
                  <span>Phí ship (vận chuyển):</span>
                  <strong>{{ shippingFee === 0 ? 'Miễn phí' : formatCurrency(shippingFee || 30000) }}</strong>
                </div>
                <div class="confirm-fee-line text-green" v-if="voucherDiscount > 0">
                  <span>Giảm giá (Phiếu {{ appliedVoucher?.code }}):</span>
                  <strong>-{{ formatCurrency(voucherDiscount) }}</strong>
                </div>
                <div class="confirm-fee-line text-muted" v-else>
                  <span>Giảm giá:</span>
                  <span>0 đ</span>
                </div>
                <div class="confirm-fee-line confirm-total-line">
                  <span class="total-label">TỔNG CỘNG:</span>
                  <strong class="total-value">{{ formatCurrency(finalTotal) }}</strong>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer Actions -->
          <div class="confirm-modal-footer">
            <button type="button" class="btn-confirm-cancel" :disabled="submitting" @click="showConfirmOrderModal = false">
              Quay lại kiểm tra
            </button>
            <button type="button" class="btn-confirm-submit" :disabled="submitting" @click="executePlaceOrder">
              <span v-if="submitting">Đang xử lý đặt hàng...</span>
              <span v-else>✓ Xác nhận đặt hàng</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL ĐANG CHỜ THANH TOÁN VNPAY -->
    <Teleport to="body">
      <div v-if="showVnpayWaitingModal" class="vnpay-waiting-modal-backdrop">
        <div class="vnpay-waiting-modal-card animate-pop">
          <!-- Header -->
          <div class="vnpay-waiting-header">
            <div class="vnpay-header-logo-row">
              <span class="vnpay-badge-logo">VNPAY</span>
              <span class="vnpay-waiting-title">Đang Chờ Bạn Thanh Toán</span>
            </div>
            <button class="vnpay-close-btn" @click="skipVnpayWaitingAndNavigate" title="Xem đơn hàng">&times;</button>
          </div>

          <!-- Body -->
          <div class="vnpay-waiting-body">
            <!-- Animated Spinner / Waiting Graphic -->
            <div class="vnpay-waiting-graphic-box">
              <div class="vnpay-pulse-ring"></div>
              <div class="vnpay-center-icon">
                <span class="vnpay-icon-text">💳</span>
              </div>
            </div>

            <h4 class="vnpay-status-heading">Vui lòng hoàn tất thanh toán trên cổng VNPAY</h4>
            <p class="vnpay-status-desc">
              Cửa sổ cổng thanh toán VNPAY đã được mở để bạn quét mã QR hoặc chuyển khoản ngân hàng. 
              <strong>Hệ thống sẽ tự động chuyển sang trang chi tiết đơn hàng ngay khi bạn thanh toán thành công!</strong>
            </p>

            <!-- Order info snapshot -->
            <div class="vnpay-order-snapshot" v-if="vnpayWaitingOrder">
              <div class="vnpay-snap-row">
                <span class="snap-label">Mã đơn hàng:</span>
                <strong class="snap-val">#{{ vnpayWaitingOrder.maHoaDon }}</strong>
              </div>
              <div class="vnpay-snap-row">
                <span class="snap-label">Số tiền cần thanh toán:</span>
                <strong class="snap-val text-gold">{{ formatCurrency(vnpayWaitingOrder.tienSauGiamGia || finalTotal) }}</strong>
              </div>
              <div class="vnpay-snap-row">
                <span class="snap-label">Trạng thái:</span>
                <span class="snap-badge-pending">
                  <span class="blink-dot"></span> Đang chờ chuyển khoản...
                </span>
              </div>
            </div>

            <div class="vnpay-helper-tip">
              💡 <em>Nếu trình duyệt chặn cửa sổ popup hoặc bạn đã lỡ tay đóng tab VNPAY, hãy bấm <strong>"Mở lại trang thanh toán"</strong> bên dưới.</em>
            </div>
          </div>

          <!-- Footer Actions -->
          <div class="vnpay-waiting-footer">
            <button
              type="button"
              class="btn-vnpay-reopen"
              @click="openVnpayPopup(vnpayPaymentUrl)"
            >
              🔄 Mở lại trang thanh toán VNPAY
            </button>
            <button
              type="button"
              class="btn-vnpay-check"
              :disabled="vnpayChecking"
              @click="checkVnpayStatus(true)"
            >
              <span v-if="vnpayChecking">⏳ Đang kiểm tra...</span>
              <span v-else>✓ Tôi đã chuyển khoản xong</span>
            </button>
            <button
              type="button"
              class="btn-vnpay-skip"
              @click="skipVnpayWaitingAndNavigate"
            >
              Xem chi tiết đơn hàng &rarr;
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- MODAL QUẢN LÝ ĐỊA CHỈ KHÁCH HÀNG (GIỐNG 100% ẢNH MẪU 2 VÀ 3) -->
    <Teleport to="body">
      <div v-if="addressModalVisible" class="client-addr-modal-backdrop" @click.self="closeAddressModal">
        <div class="client-addr-modal-card animate-pop">
          <div class="client-addr-modal-header">
            <h3 class="client-addr-modal-title">
              Quản lý địa chỉ: {{ currentClientUser?.name }}
            </h3>
            <button class="client-addr-close-btn" @click="closeAddressModal" title="Đóng">&times;</button>
          </div>

          <div class="client-addr-modal-body">
            <!-- Thanh tác vụ trên: số lượng + nút Thêm địa chỉ mới -->
            <div class="addr-top-actions-bar">
              <span class="addr-total-count">
                Tổng cộng: {{ savedAddresses.length }} địa chỉ
              </span>
              <button
                v-if="!showAddressForm"
                type="button"
                class="btn-primary-addr-sm"
                @click="openAddAddressForm"
              >
                + Thêm địa chỉ mới
              </button>
            </div>

            <!-- FORM THÊM / SỬA ĐỊA CHỈ (ĐÚNG THEO ẢNH MẪU 3) -->
            <div v-if="showAddressForm" class="addr-inline-form-box">
              <h4 class="addr-form-box-title">
                {{ editingAddressId ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}
              </h4>
              <div class="addr-form-grid-2">
                <div class="addr-form-field">
                  <label>Tỉnh / Thành phố <span class="req-star">*</span></label>
                  <select
                    v-model="addrForm.thanhPho"
                    @change="onAddrProvinceChange"
                    class="addr-form-select"
                  >
                    <option value="" disabled>-- Chọn Tỉnh / Thành phố --</option>
                    <option v-for="p in provincesList" :key="p.name || p" :value="p.name || p">
                      {{ p.name || p }}
                    </option>
                  </select>
                </div>

                <div class="addr-form-field">
                  <label>Phường / Xã <span class="req-star">*</span></label>
                  <select
                    v-model="addrForm.phuong"
                    :disabled="!addrForm.thanhPho"
                    class="addr-form-select"
                  >
                    <option value="" disabled>-- Chọn Phường / Xã --</option>
                    <option v-for="w in addrWardsList" :key="w.name || w" :value="w.name || w">
                      {{ w.name || w }}
                    </option>
                  </select>
                </div>

                <div class="addr-form-field full-width">
                  <label>Địa chỉ cụ thể <span class="req-star">*</span></label>
                  <input
                    v-model="addrForm.diaChiCuThe"
                    type="text"
                    placeholder="Số nhà, ngõ, tên đường..."
                    class="addr-form-input"
                  />
                </div>

                <div class="addr-form-field full-width">
                  <label class="addr-checkbox-label">
                    <input type="checkbox" v-model="addrForm.macDinh" />
                    <span>Đặt làm địa chỉ mặc định</span>
                  </label>
                </div>
              </div>

              <div class="addr-form-button-row">
                <button type="button" class="btn-cancel-addr" @click="cancelAddressForm">
                  Hủy
                </button>
                <button type="button" class="btn-save-addr" :disabled="savingAddress" @click="saveAddress">
                  {{ savingAddress ? 'Đang lưu...' : 'Lưu địa chỉ' }}
                </button>
              </div>
            </div>

            <!-- DANH SÁCH ĐỊA CHỈ HIỆN CÓ CỦA KHÁCH HÀNG (ĐÚNG THEO ẢNH MẪU 2) -->
            <div class="addr-items-scroll-list">
              <div v-if="loadingAddresses" class="addr-empty-state">
                Đang tải danh sách địa chỉ...
              </div>
              <div v-else-if="!savedAddresses.length && !showAddressForm" class="addr-empty-state">
                Khách hàng chưa có địa chỉ nào. Nhấn "+ Thêm địa chỉ mới" để tạo.
              </div>
              <div
                v-for="addr in savedAddresses"
                :key="addr.id"
                class="addr-item-card"
                :class="{ 'is-default': addr.macDinh, 'is-active-choice': selectedAddressId === addr.id }"
              >
                <div class="addr-card-main-col">
                  <div class="addr-card-header-row">
                    <span v-if="addr.macDinh" class="addr-default-badge">Mặc định</span>
                    <span class="addr-code-tag">{{ addr.maDiaChi }}</span>
                    <span v-if="selectedAddressId === addr.id" class="addr-active-choice-badge">✓ Đang chọn nhận hàng</span>
                  </div>
                  <div class="addr-card-address-text">
                    <strong>{{ addr.diaChiCuThe }}</strong>, {{ [addr.phuong, addr.huyen, addr.thanhPho].filter(Boolean).join(', ') }}
                  </div>
                </div>

                <div class="addr-card-btn-actions">
                  <button
                    type="button"
                    class="btn-text-act btn-select-addr"
                    @click="selectAddressForCheckout(addr); closeAddressModal()"
                  >
                    Giao đến đây
                  </button>
                  <button
                    v-if="!addr.macDinh"
                    type="button"
                    class="btn-text-act btn-set-default"
                    @click="setAsDefault(addr.id)"
                  >
                    Đặt làm mặc định
                  </button>
                  <button
                    type="button"
                    class="btn-text-act btn-edit-addr"
                    @click="editAddress(addr)"
                  >
                    Sửa
                  </button>
                  <button
                    v-if="!addr.macDinh"
                    type="button"
                    class="btn-text-act btn-del-addr"
                    @click="deleteAddress(addr.id)"
                  >
                    Xóa
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
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

/* Cart step styling */

/* ======================================================== */
/* BƯỚC 1: GIAO DIỆN GIỎ HÀNG CHUẨN THIẾT KẾ ẢNH MẪU        */
/* ======================================================== */
.cart-step-wrapper {
  margin-top: 10px;
}

.cart-step-hero {
  text-align: center;
  margin-bottom: 30px;
}

.cart-fancy-title {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.3px;
  margin: 0 0 6px;
  line-height: 1.15;
}

.cart-fancy-sub {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.cart-step-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 28px;
  align-items: start;
}

@media (max-width: 1080px) {
  .cart-step-layout {
    grid-template-columns: 1fr;
  }
}

.cart-step-table-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.table-header-row {
  display: grid;
  grid-template-columns: 2.2fr 1fr 0.9fr 1fr 1.1fr;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  font-size: 12.5px;
  font-weight: 800;
  color: #475569;
  letter-spacing: 0.5px;
}

.table-header-row .col-head {
  display: flex;
  align-items: center;
}

.col-prod {
  gap: 12px;
}

.col-price {
  justify-content: flex-start;
}

.col-discount {
  justify-content: center;
}

.col-qty {
  justify-content: center;
}

.col-total {
  justify-content: flex-end;
}

.custom-checkbox-label {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.custom-checkbox-label input {
  display: none;
}

.custom-check-box {
  width: 19px;
  height: 19px;
  border: 2px solid #cbd5e1;
  border-radius: 4px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.custom-checkbox-label input:checked + .custom-check-box {
  background: #496883;
  border-color: #496883;
}

.custom-checkbox-label input:checked + .custom-check-box::after {
  content: '✓';
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
}

.table-items-body {
  display: flex;
  flex-direction: column;
}

.cart-item-table-row {
  display: grid;
  grid-template-columns: 2.2fr 1fr 0.9fr 1fr 1.1fr;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.2s;
}

.cart-item-table-row:hover {
  background: #f8fafc;
}

.cart-item-table-row:last-child {
  border-bottom: none;
}

.col-cell {
  display: flex;
  align-items: center;
}

.col-cell.col-prod {
  gap: 14px;
}

.prod-table-pic {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eef2f6;
  background: #f8fafc;
  flex-shrink: 0;
}

.prod-table-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prod-table-name {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.35;
}

.prod-table-badges {
  display: flex;
  gap: 6px;
}

.badge-size-tag {
  display: inline-block;
  padding: 2px 7px;
  font-size: 12px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-weight: 600;
}

.prod-table-stock {
  font-size: 12px;
  color: #94a3b8;
}

.price-stack {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.price-sale {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.price-origin {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
}

.discount-percent-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: #ecfdf5;
  color: #15803d;
  border: 1px solid #bbf7d0;
  border-radius: 5px;
  font-size: 12.5px;
  font-weight: 700;
}

.text-dash {
  color: #d6d3d1;
  font-size: 14px;
}

.qty-pill-box {
  display: inline-flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  overflow: hidden;
}

.qty-pill-btn {
  width: 28px;
  height: 28px;
  background: #f8fafc;
  border: none;
  font-size: 15px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.qty-pill-btn:hover:not(:disabled) {
  background: #e2e8f0;
  color: #0f172a;
}

.qty-pill-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.qty-pill-val {
  width: 32px;
  text-align: center;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
}

.col-cell.col-total {
  gap: 14px;
  justify-content: flex-end;
}

.item-line-total {
  font-size: 15px;
  font-weight: 800;
  color: #c2410c;
}

.btn-trash-row {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-trash-row:hover {
  color: #dc2626;
  transform: scale(1.15);
}

.table-footer-row {
  padding: 16px 20px;
  background: #ffffff;
  border-top: 1px solid #f5f1ed;
}

.link-add-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: #64748b;
  text-decoration: none;
  transition: color 0.15s;
}

.link-add-more:hover {
  color: #475569;
  text-decoration: underline;
}

.cart-step-summary-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.02);
  position: sticky;
  top: 90px;
}

.summary-card-title {
  margin: 0 0 18px;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
}

.summary-card-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sum-row-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #64748b;
}

.sum-val {
  color: #0f172a;
}

.sum-card-sep {
  height: 1px;
  background: #f1f5f9;
  margin: 4px 0;
}

.sum-row-total {
  font-size: 15px;
}

.sum-total-label {
  font-weight: 700;
  color: #1e293b;
}

.sum-total-value {
  font-size: 21px;
  font-weight: 800;
  color: #c2410c;
}

.btn-action-proceed {
  width: 100%;
  padding: 15px 20px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.6px;
  cursor: pointer;
  margin-top: 10px;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.btn-action-proceed:hover:not(:disabled) {
  background: #34485e;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.35);
}

.btn-action-proceed:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.checkout-back-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  padding: 14px 20px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  flex-wrap: wrap;
  gap: 12px;
}

.btn-step-back-cart {
  background: none;
  border: none;
  font-size: 13.5px;
  font-weight: 700;
  color: #6d4c41;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  transition: color 0.15s;
}

.btn-step-back-cart:hover {
  color: #1e293b;
  letter-spacing: -0.3px;
  text-decoration: underline;
}

.checkout-flow-breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.flow-step {
  color: #94a3b8;
  font-weight: 600;
}

.flow-step.clickable {
  color: #6d4c41;
  cursor: pointer;
}

.flow-step.clickable:hover {
  text-decoration: underline;
}

.flow-step.current {
  color: #1e293b;
  font-weight: 800;
}

.flow-sep {
  color: #cbd5e1;
}

@media (max-width: 768px) {
  .table-header-row {
    display: none;
  }
  .cart-item-table-row {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 16px;
  }
  .col-cell.col-price,
  .col-cell.col-discount,
  .col-cell.col-qty,
  .col-cell.col-total {
    justify-content: space-between;
  }
  .col-cell.col-price::before {
    content: 'Đơn giá:';
    font-size: 13px;
    color: #64748b;
  }
  .col-cell.col-discount::before {
    content: 'Giảm giá:';
    font-size: 13px;
    color: #64748b;
  }
  .col-cell.col-qty::before {
    content: 'Số lượng:';
    font-size: 13px;
    color: #64748b;
  }
  .col-cell.col-total::before {
    content: 'Tổng tiền:';
    font-size: 13px;
    color: #64748b;
  }
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

.checkout-items-title {
  font-size: 14.5px;
  font-weight: 800;
  color: #0f172a;
}

.btn-edit-cart-link {
  background: none;
  border: none;
  color: #b88d4c;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.checkout-static-qty {
  font-size: 13px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 3px 10px;
  border-radius: 6px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.checkout-static-qty strong {
  color: #0f172a;
  font-weight: 800;
}

.btn-edit-cart-link:hover {
  color: #93652b;
  background-color: #fefce8;
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
  align-items: stretch;
}

.voucher-text-input {
  flex: 1;
  height: 38px;
  padding: 0 12px !important;
  font-size: 13.5px !important;
  font-weight: 600;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #0f172a;
  text-transform: uppercase;
  box-sizing: border-box;
}

.voucher-text-input::placeholder {
  text-transform: none;
  font-weight: 400;
  color: #94a3b8;
  letter-spacing: normal;
}

.btn-apply-voucher {
  height: 38px;
  background: #425b76;
  color: #ffffff;
  border: none;
  padding: 0 16px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-apply-voucher:hover:not(:disabled) {
  background: #34485e;
}

.btn-apply-voucher:disabled {
  background: #94a3b8;
  opacity: 0.6;
  cursor: not-allowed;
}

.old-voucher-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.old-voucher-text-input {
  flex: 1;
  text-transform: uppercase;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.old-btn-apply-voucher {
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
.av-personal-badge {
  font-size: 11px;
  font-weight: 700;
  color: #7c3aed;
  background: #f3e8ff;
  border: 1px solid #ddd6fe;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 6px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

/* Tabs phân loại voucher */
.voucher-filter-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  margin-bottom: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.btn-v-tab {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.btn-v-tab:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-v-tab.active {
  background: #1e293b;
  color: #ffffff;
  border-color: #1e293b;
}

.btn-v-tab-personal {
  color: #7c3aed;
  border-color: #ddd6fe;
  background: #fdf4ff;
}

.btn-v-tab-personal:hover {
  background: #fae8ff;
  color: #6b21a8;
}

.btn-v-tab-personal.active {
  background: #7c3aed;
  color: #ffffff;
  border-color: #7c3aed;
}

.btn-suggest-code-tag.is-personal-tag {
  border-color: #c084fc;
  color: #7c3aed;
  background: #faf5ff;
}

.btn-suggest-code-tag.is-personal-tag:hover {
  background: #f3e8ff;
  border-color: #a855f7;
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

/* =========================================================
   STYLE CHO SỔ ĐỊA CHỈ & MODAL QUẢN LÝ ĐỊA CHỈ KHÁCH HÀNG
   ========================================================= */
.panel-header-with-action {
  display: block;
  margin-bottom: 12px;
}

.panel-header-with-action .panel-section-title {
  margin-bottom: 0;
}

.btn-manage-addr-trigger {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 6px 14px;
  font-size: 0.84rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.btn-manage-addr-trigger:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.address-book-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  background: #f8fafc;
  border: 1px dashed #94a3b8;
  border-radius: 10px;
  padding: 10px 14px;
}

.abt-left {
  font-size: 0.85rem;
  color: #334155;
}

.abt-dot {
  color: #16a34a;
  font-weight: 700;
  margin-right: 3px;
}

.btn-add-new-addr-action {
  background: #425b76;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 0.83rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-add-new-addr-action:hover {
  background: #34485e;
}

.btn-open-addr-list-action {
  background: #ffffff;
  color: #425b76;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 0.83rem;
  font-weight: 600;
  cursor: pointer;
  margin-left: 8px;
  transition: all 0.2s;
}

.btn-open-addr-list-action:hover {
  background: #f1f5f9;
}

/* Modal Quản lý địa chỉ khách hàng (Teleport) */
.client-addr-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background-color: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.client-addr-modal-card {
  width: 100%;
  max-width: 680px;
  max-height: 90vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalScaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalScaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.client-addr-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #f1ece4;
  background-color: #fcfbf9;
}

.client-addr-modal-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #2c445a;
  margin: 0;
}

.client-addr-close-btn {
  background: none;
  border: none;
  font-size: 1.7rem;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.client-addr-close-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.client-addr-modal-body {
  padding: 20px 24px;
  overflow-y: auto;
}

.addr-top-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.addr-total-count {
  font-size: 0.95rem;
  font-weight: 600;
  color: #475569;
}

.btn-primary-addr-sm {
  background: #425b76;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary-addr-sm:hover {
  background: #34485e;
}

/* Box form thêm/sửa địa chỉ */
.addr-inline-form-box {
  background-color: #fdfbf7;
  border: 1px solid #eedecb;
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 20px;
}

.addr-form-box-title {
  font-size: 1rem;
  font-weight: 700;
  color: #2c445a;
  margin: 0 0 14px 0;
}

.addr-form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.addr-form-field {
  display: flex;
  flex-direction: column;
}

.addr-form-field.full-width {
  grid-column: span 2;
}

.addr-form-field label {
  font-size: 0.84rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.addr-form-select,
.addr-form-input {
  width: 100%;
  height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 0.92rem;
  color: #0f172a;
  background-color: #ffffff;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.addr-form-select:focus,
.addr-form-input:focus {
  border-color: #425b76;
}

.addr-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  margin-top: 4px;
}

.addr-checkbox-label input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: #425b76;
}

.addr-form-button-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn-cancel-addr {
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-addr:hover {
  background-color: #f1f5f9;
}

.btn-save-addr {
  height: 38px;
  padding: 0 20px;
  border-radius: 8px;
  border: none;
  background-color: #425b76;
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save-addr:hover {
  background-color: #34485e;
}

.btn-save-addr:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Danh sách địa chỉ dạng thẻ */
.addr-items-scroll-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.addr-empty-state {
  text-align: center;
  padding: 32px 16px;
  color: #64748b;
  font-size: 0.92rem;
}

.addr-item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background-color: #ffffff;
  transition: all 0.2s;
}

.addr-item-card.is-default {
  border-color: #425b76;
  background-color: #f8fafc;
}

.addr-item-card.is-active-choice {
  box-shadow: 0 0 0 2px #3b82f6;
}

.addr-card-main-col {
  flex: 1;
}

.addr-card-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.addr-default-badge {
  background-color: #e0f2fe;
  color: #0369a1;
  border: 1px solid #7dd3fc;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 7px;
}

.addr-code-tag {
  font-size: 0.82rem;
  color: #94a3b8;
  font-weight: 600;
}

.addr-active-choice-badge {
  background-color: #dcfce7;
  color: #15803d;
  border: 1px solid #86efac;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 7px;
}

.addr-card-address-text {
  font-size: 0.92rem;
  color: #334155;
  line-height: 1.45;
}

.addr-card-btn-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.btn-text-act {
  background: none;
  border: none;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.btn-select-addr {
  background-color: #eff6ff;
  color: #2563eb;
}

.btn-select-addr:hover {
  background-color: #dbeafe;
}

.btn-set-default {
  color: #425b76;
}

.btn-set-default:hover {
  background-color: #f1f5f9;
}

.btn-edit-addr {
  color: #425b76;
}

.btn-edit-addr:hover {
  background-color: #f1f5f9;
}

.btn-del-addr {
  color: #dc2626;
}

.btn-del-addr:hover {
  background-color: #fee2e2;
}

/* ======================================================== */
/* MODAL XÁC NHẬN ĐƠN HÀNG (POPUP TRƯỚC KHI ĐẶT) */
/* ======================================================== */
.client-confirm-order-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background-color: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.client-confirm-order-card {
  width: 100%;
  max-width: 620px;
  max-height: 90vh;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalScaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.confirm-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #f1f5f9;
  background-color: #fafbfc;
}

.confirm-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.confirm-header-icon {
  font-size: 1.6rem;
}

.confirm-modal-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.confirm-modal-sub {
  font-size: 0.83rem;
  color: #64748b;
  margin: 2px 0 0 0;
}

.confirm-close-btn {
  background: none;
  border: none;
  font-size: 1.7rem;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.confirm-close-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
}

.confirm-modal-body {
  padding: 18px 22px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.confirm-info-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
}

.confirm-box-title {
  font-size: 0.92rem;
  font-weight: 700;
  color: #334155;
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.confirm-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 14px;
}

.confirm-meta-item {
  display: flex;
  flex-direction: column;
  font-size: 0.88rem;
}

.confirm-meta-item.full-row {
  grid-column: span 2;
}

.c-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 2px;
}

.c-val {
  color: #1e293b;
  word-break: break-word;
}

.confirm-items-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 150px;
  overflow-y: auto;
  padding-right: 4px;
}

.confirm-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  background: #ffffff;
  border: 1px solid #edf2f7;
  border-radius: 8px;
}

.confirm-item-img {
  width: 42px;
  height: 42px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background-color: #f1f5f9;
}

.confirm-item-details {
  flex: 1;
  min-width: 0;
}

.confirm-item-name {
  font-size: 0.86rem;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.confirm-item-attr {
  font-size: 0.76rem;
  color: #64748b;
  display: flex;
  gap: 8px;
  margin-top: 2px;
}

.confirm-item-qty {
  font-weight: 700;
  color: #2563eb;
}

.confirm-item-price {
  font-size: 0.88rem;
  font-weight: 700;
  color: #0f172a;
}

.confirm-pricing-box {
  background: #fdfbf7;
  border-color: #eedecb;
}

.confirm-fee-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.confirm-fee-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
  color: #334155;
}

.confirm-fee-line.text-green strong {
  color: #16a34a;
}

.confirm-total-line {
  padding-top: 10px;
  margin-top: 4px;
  border-top: 1px dashed #cbd5e1;
}

.confirm-total-line .total-label {
  font-size: 0.98rem;
  font-weight: 700;
  color: #1e293b;
}

.confirm-total-line .total-value {
  font-size: 1.25rem;
  font-weight: 800;
  color: #c2410c;
}

.confirm-modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 16px 22px;
  border-top: 1px solid #f1f5f9;
  background-color: #fafbfc;
}

.btn-confirm-cancel {
  height: 40px;
  padding: 0 18px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-cancel:hover {
  background-color: #f1f5f9;
}

.btn-confirm-submit {
  height: 40px;
  padding: 0 22px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #d08344 0%, #b86b2d 100%);
  color: #ffffff;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(208, 131, 68, 0.25);
}

.btn-confirm-submit:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.btn-confirm-submit:disabled,
.btn-confirm-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* VNPAY WAITING MODAL */
.vnpay-waiting-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.vnpay-waiting-modal-card {
  width: 100%;
  max-width: 520px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.35);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.vnpay-waiting-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #005baa;
  color: #ffffff;
}

.vnpay-header-logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.vnpay-badge-logo {
  background: #ffffff;
  color: #005baa;
  font-weight: 900;
  font-size: 0.85rem;
  padding: 3px 8px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.vnpay-waiting-title {
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.vnpay-close-btn {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 1.5rem;
  cursor: pointer;
  line-height: 1;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.vnpay-close-btn:hover {
  opacity: 1;
}

.vnpay-waiting-body {
  padding: 24px 22px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.vnpay-waiting-graphic-box {
  position: relative;
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vnpay-pulse-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 4px solid #005baa;
  border-top-color: transparent;
  animation: vnpay-spin 1.2s linear infinite;
}

@keyframes vnpay-spin {
  to {
    transform: rotate(360deg);
  }
}

.vnpay-center-icon {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: #eff6ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vnpay-icon-text {
  font-size: 1.8rem;
}

.vnpay-status-heading {
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.vnpay-status-desc {
  font-size: 0.88rem;
  color: #475569;
  line-height: 1.5;
  margin-bottom: 16px;
}

.vnpay-order-snapshot {
  width: 100%;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 14px;
  text-align: left;
}

.vnpay-snap-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  font-size: 0.88rem;
}

.vnpay-snap-row + .vnpay-snap-row {
  border-top: 1px dashed #e2e8f0;
}

.snap-label {
  color: #64748b;
}

.snap-val {
  color: #1e293b;
  font-weight: 700;
}

.snap-badge-pending {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #fef3c7;
  color: #92400e;
  font-weight: 600;
  font-size: 0.82rem;
  padding: 2px 8px;
  border-radius: 999px;
}

.blink-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f59e0b;
  animation: vnpay-pulse 1s ease infinite alternate;
}

@keyframes vnpay-pulse {
  from { opacity: 0.3; }
  to { opacity: 1; }
}

.vnpay-helper-tip {
  font-size: 0.8rem;
  color: #64748b;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 4px;
}

.vnpay-waiting-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 20px;
  background: #fafbfc;
  border-top: 1px solid #f1f5f9;
  width: 100%;
}

.btn-vnpay-reopen {
  width: 100%;
  height: 42px;
  background: #005baa;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-vnpay-reopen:hover {
  background: #004b8d;
}

.btn-vnpay-check {
  width: 100%;
  height: 40px;
  background: #ffffff;
  color: #059669;
  border: 1.5px solid #10b981;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-vnpay-check:hover:not(:disabled) {
  background: #ecfdf5;
}

.btn-vnpay-check:disabled {
  opacity: 0.6;
  cursor: wait;
}

.btn-vnpay-skip {
  background: transparent;
  border: none;
  color: #64748b;
  font-size: 0.85rem;
  cursor: pointer;
  padding: 6px;
  text-decoration: underline;
  transition: color 0.2s;
}

.btn-vnpay-skip:hover {
  color: #1e293b;
}

</style>
