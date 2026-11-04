<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency, formatDateTime } from '@/utils/dinhDang.js'
import { showAppConfirm, showAppAlert } from '@/utils/thongBaoDialog.js'
import { currentClientUser, isClientLoggedIn } from '@/utils/taiKhoanKhachHang.js'
import ModalDangNhap from '@/components/BocCuc/ModalDangNhap.vue'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const orders = ref([])
const selectedOrder = ref(null)
const selectedOrderItems = ref([])
const orderLogs = ref([])
const activeTab = ref('all') // all, pending, shipping, completed, cancelled
const searchQuery = ref('')
const showAuthModal = ref(false)

const handleLoginSuccess = () => {
  showAuthModal.value = false
  loadOrders()
}

watch(() => currentClientUser.value, () => {
  loadOrders()
})
const isPosOrder = (o) => {
  if (!o) return false
  const addr = (o.diaChiNhanHang || '').trim().toLowerCase()
  const hasDeliveryAddress = addr && !addr.includes('tại quầy') && !addr.includes('tai quay') && !addr.includes('tại cửa hàng') && !addr.includes('tai cua hang') && addr !== 'bán tại quầy'
  const hasShipFee = Number(o.phiVanChuyen || o.phiShip || 0) > 0
  const hasCarrier = Boolean(o.donViVanChuyen && o.donViVanChuyen !== '—' && !o.donViVanChuyen.toLowerCase().includes('tại quầy'))
  if (hasDeliveryAddress || hasShipFee || hasCarrier) {
    return false
  }
  return o.loaiDon === 1 || addr.includes('tại quầy') || addr.includes('tai quay') || addr.includes('tại cửa hàng') || !addr

}

const getOrderSubtotal = (o) => {
  if (!o) return 0
  return Number(o.tongTien || 0)
}

const getOrderCampaignDiscount = (o) => {
  if (!o) return 0
  const items = orderItemsMap.value[o.maHoaDon] || []
  let totalCampaignDiscount = 0
  items.forEach(it => {
    const originPrice = Number(it.idChiTietSanPham?.giaBan || it.giaGoc || 0)
    const salePrice = Number(it.donGia || it.giaDuocTinh || 0)
    const qty = Number(it.soLuong || 1)
    if (originPrice > salePrice && salePrice > 0) {
      totalCampaignDiscount += (originPrice - salePrice) * qty
    }
  })
  return totalCampaignDiscount
}

const getOrderOriginalSubtotal = (o) => {
  if (!o) return 0
  const campDiscount = getOrderCampaignDiscount(o)
  const actualSubtotal = getOrderSubtotal(o)
  return actualSubtotal + campDiscount
}

const getOrderShippingFee = (o) => {
  if (!o || isPosOrder(o)) return 0
  return Number(o.phiShip || o.phiVanChuyen || 0)
}

const getOrderFinalTotal = (o) => {
  if (!o) return 0
  if (o.tienSauGiamGia != null && Number(o.tienSauGiamGia) >= 0) {
    return Number(o.tienSauGiamGia)
  }
  return getOrderSubtotal(o) + getOrderShippingFee(o)
}

const getOrderDiscount = (o) => {
  if (!o) return 0
  if (o.tongTienGiamGia != null && Number(o.tongTienGiamGia) > 0) {
    return Number(o.tongTienGiamGia)
  }
  const subtotal = getOrderSubtotal(o)
  const ship = getOrderShippingFee(o)
  const finalTotal = getOrderFinalTotal(o)
  const diff = subtotal + ship - finalTotal
  return diff > 0 ? diff : 0
}

const orderItemsMap = ref({}) // Cache: { [maHoaDon]: itemsArray }
const expandedOrders = ref({}) // Trạng thái mở rộng danh sách sản phẩm theo mã đơn { [maHoaDon]: boolean }

const toggleExpandOrder = async (orderCode) => {
  if (!orderCode) return
  if (!orderItemsMap.value[orderCode] || orderItemsMap.value[orderCode].length === 0) {
    try {
      const res = await api.get(`/api/hoa-don/code/${orderCode}/chi-tiet`)
      orderItemsMap.value[orderCode] = Array.isArray(res.data) ? res.data : []
    } catch (e) {
      console.error('Lỗi tải sản phẩm đơn hàng', e)
    }
  }
  expandedOrders.value[orderCode] = !expandedOrders.value[orderCode]
}

const resolveProductImg = (it) => {
  const raw = it.anhDaiDien || it.anh || it.hinhAnh || it.idChiTietSanPham?.idSanPham?.anh || it.idChiTietSanPham?.anh
  return resolveProductImageUrl(raw)
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  return `http://localhost:8080${raw.startsWith('/') ? '' : '/'}${raw}`
}

const onImgError = (e) => {
  e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER
}

const loadItemsForList = async (orderList) => {
  if (!orderList || orderList.length === 0) return
  const toFetch = orderList.slice(0, 25).filter(o => o.maHoaDon && !orderItemsMap.value[o.maHoaDon])
  if (toFetch.length === 0) return

  await Promise.all(toFetch.map(async (o) => {
    try {
      const res = await api.get(`/api/hoa-don/code/${o.maHoaDon}/chi-tiet`)
      orderItemsMap.value[o.maHoaDon] = Array.isArray(res.data) ? res.data : []
    } catch (e) {
      orderItemsMap.value[o.maHoaDon] = []
    }
  }))
}

// Đã lược bỏ đánh giá

// Countdown 5 phút tự động hủy cho đơn chưa thanh toán VNPAY
const countdownSeconds = ref(0)

const calculateRemainingSeconds = (order) => {
  if (!order || !order.ngayTao || !isVnPayOrderObj(order) || order.trangThaiThanhToan === 1 || order.trangThai !== 1) {
    return 0
  }
  const createdMs = new Date(order.ngayTao).getTime()
  if (isNaN(createdMs)) return 0
  const expiryMs = createdMs + 5 * 60 * 1000 // 5 phút tự động hủy
  const now = Date.now()
  return Math.max(0, Math.floor((expiryMs - now) / 1000))
}

let isCancellingExpired = false
const updateCountdown = async () => {
  if (!selectedOrder.value) return
  const remaining = calculateRemainingSeconds(selectedOrder.value)
  countdownSeconds.value = remaining

  // Nếu đã quá 5 phút mà đơn vẫn chưa thanh toán (trạng thái 1)
  if (remaining <= 0 && isVnPayOrder.value && selectedOrder.value.trangThai === 1 && selectedOrder.value.trangThaiThanhToan !== 1) {
    if (isCancellingExpired) return
    isCancellingExpired = true
    try {
      await api.post(`/api/hoa-don/code/${selectedOrder.value.maHoaDon}/cancel-expired-vnpay`)
      selectedOrder.value.trangThai = 6
      await loadOrders()
    } catch (e) {
      console.warn('Auto cancel error:', e)
    } finally {
      isCancellingExpired = false
    }
  }
}
let timerInterval = null

// Toast thông báo
const showPendingToast = ref(false)
const showSuccessToast = ref(false)
const successToastMsg = ref('')

const formatTimer = (sec) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`
}

const formatLogTime = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

const getDeliveryStepTime = (stepNum) => {
  if (!selectedOrder.value) return ''
  const currentStatus = Number(selectedOrder.value.trangThai || 1)
  if (currentStatus < stepNum) {
    return ''
  }

  if (stepNum === 1) {
    if (orderLogs.value && orderLogs.value.length > 0) {
      const createLogs = orderLogs.value.filter(l => {
        const text = ((l.hanhDong || '') + ' ' + (l.ghiChu || '')).toLowerCase()
        return Number(l.trangThai) === 1 || text.includes('tạo') || text.includes('đặt hàng')
      })
      if (createLogs.length > 0) {
        const earliest = createLogs[createLogs.length - 1]
        if (earliest && earliest.thoiGian) return formatLogTime(earliest.thoiGian)
      }
    }
    return formatLogTime(selectedOrder.value.ngayTao)
  }

  if (orderLogs.value && orderLogs.value.length > 0) {
    const matched = orderLogs.value.find(l => {
      if (Number(l.trangThai) === stepNum) return true
      const text = ((l.hanhDong || '') + ' ' + (l.ghiChu || '')).toLowerCase()
      if (stepNum === 2 && (text.includes('xác nhận') || text.includes('duyệt'))) return true
      if (stepNum === 3 && (text.includes('chờ vận chuyển') || text.includes('đóng gói') || text.includes('chuẩn bị'))) return true
      if (stepNum === 4 && (text.includes('đang vận chuyển') || text.includes('đang giao'))) return true
      if (stepNum === 5 && (text.includes('hoàn thành') || text.includes('giao thành công') || text.includes('đã nhận'))) return true
      return false

    })
    if (matched && matched.thoiGian) {
      return formatLogTime(matched.thoiGian)
    }
  }

  if (currentStatus === stepNum && selectedOrder.value.ngayCapNhat) {
    return formatLogTime(selectedOrder.value.ngayCapNhat)
  }

  return ''
}

const showHistoryModal = ref(false)

const voucherDisplayName = computed(() => {
  const o = selectedOrder.value
  if (!o) return ''
  return o.idPhieuGiamGia?.tenPhieuGiamGia || o.maPhieuGiamGia || o.idPhieuGiamGia?.maPhieuGiamGia || ''
})

const clientTimelineSteps = computed(() => {
  const o = selectedOrder.value
  if (!o) return []
  const pureStore = isPosOrder(o)
  const curStatus = Number(o.trangThai || 1)
  const createTime = o.ngayTao ? formatLogTime(o.ngayTao) : ''
  const finishTime = o.ngayCapNhat ? formatLogTime(o.ngayCapNhat) : createTime

  // 1. Đơn đã hủy (status = 6)
  if (curStatus === 6) {
    return [
      { id: 1, name: pureStore ? 'Tạo đơn tại quầy' : 'Chờ xác nhận', time: getDeliveryStepTime(1) || createTime, done: true, active: false, pending: false, icon: 'hourglass' },
      { id: 6, name: 'Đã hủy đơn', time: finishTime, done: false, active: true, pending: false, isCancelled: true, icon: 'cancelled' }
    ]
  }

  // 2. Đơn tại quầy
  if (pureStore) {
    const storeSteps = [
      { id: 1, name: 'Tạo đơn tại quầy', time: getDeliveryStepTime(1) || createTime, done: curStatus >= 2, active: curStatus === 1, pending: false, icon: 'hourglass' },
      { id: 2, name: 'Đã xác nhận', time: curStatus >= 2 ? (getDeliveryStepTime(2) || finishTime) : '', done: curStatus >= 5, active: curStatus === 2, pending: curStatus < 2, icon: 'check' },
      { id: 5, name: 'Hoàn thành', time: curStatus >= 5 ? (getDeliveryStepTime(5) || finishTime) : '', done: curStatus >= 5, active: curStatus >= 5, pending: curStatus < 5, icon: 'flag' }
    ]
    if (curStatus === 1) {
      return storeSteps.slice(0, 2)
    }
    return storeSteps
  }

  // 3. Đơn hàng Online: Hiện trạng thái đã tới + duy nhất 1 trạng thái tiếp theo (gợi ý)
  const maxStepToShow = Math.min(curStatus + 1, 5)

  const stepsDef = [
    { id: 1, name: 'Chờ xác nhận', icon: 'hourglass' },
    { id: 2, name: 'Đã xác nhận', icon: 'check' },
    { id: 3, name: 'Chờ vận chuyển', icon: 'box' },
    { id: 4, name: 'Đang vận chuyển', icon: 'truck' },
    { id: 5, name: 'Hoàn thành', icon: 'flag' }
  ]

  return stepsDef
    .filter(s => s.id <= maxStepToShow)
    .map(s => {
      const isDone = s.id < curStatus
      const isActive = s.id === curStatus
      const isPending = s.id > curStatus
      return {
        ...s,
        done: isDone,
        active: isActive,
        pending: isPending,
        time: s.id <= curStatus ? (getDeliveryStepTime(s.id) || (s.id === 1 ? createTime : (s.id === curStatus ? finishTime : ''))) : ''
      }
    })
  })

const visibleOnlineSteps = computed(() => {
  const o = selectedOrder.value
  if (!o) return []
  const curStatus = Number(o.trangThai || 1)

  // 1. Đơn đã hủy (status = 6)
  if (curStatus === 6) {
    return [
      { id: 1, name: 'Chờ xác nhận', time: getDeliveryStepTime(1), completed: true, current: false },
      { id: 6, name: 'Đã hủy đơn', time: formatLogTime(o.ngayCapNhat || o.ngayTao), completed: false, current: true, isCancelled: true }
    ]
  }

  // 3. Đơn online bình thường: chỉ hiện các bước đã tới + duy nhất 1 bước tiếp theo liền kề (pending)
  const maxStepToShow = Math.min(curStatus + 1, 5)

  const stepsDef = [
    { id: 1, name: 'Chờ xác nhận' },
    { id: 2, name: 'Đã xác nhận' },
    { id: 3, name: 'Chờ vận chuyển' },
    { id: 4, name: 'Đang vận chuyển' },
    { id: 5, name: 'Giao thành công' }
  ]

  return stepsDef
    .filter(s => s.id <= maxStepToShow)
    .map(s => {
      const isCompleted = s.id <= curStatus
      const isCurrent = s.id === curStatus
      const isPending = s.id > curStatus
      return {
        ...s,
        time: s.id <= curStatus ? getDeliveryStepTime(s.id) : '',
        completed: isCompleted,
        current: isCurrent,
        pending: isPending
      }
    })
})

const getActorName = (log) => {
  if (log.vaiTroNguoiThucHien) return log.vaiTroNguoiThucHien
  if (log.nguoiThucHien) return log.nguoiThucHien
  return 'Hệ thống'
}

const getActorClass = (log) => {
  const actor = (log.vaiTroNguoiThucHien || log.nguoiThucHien || '').toLowerCase()
  if (actor.includes('quản trị') || actor.includes('admin') || actor.includes('nhân viên') || actor.includes('vnpay') || actor.includes('hệ thống')) {
    return 'actor-admin'
  }
  return 'actor-customer'
}

const currentUserName = computed(() => {
  if (currentClientUser.value?.name) return currentClientUser.value.name
  if (selectedOrder.value?.tenKhachHang) {
    return selectedOrder.value.tenKhachHang
  }
  return 'Khách hàng'
})

const currentUserFullName = computed(() => {
  if (currentClientUser.value?.name) return currentClientUser.value.name
  if (selectedOrder.value?.tenKhachHang) {
    return selectedOrder.value.tenKhachHang
  }
  return 'Khách hàng'
})

const isVnPayOrderObj = (o) => {
  if (!o) return false
  if (o.isVnPay === true) return true
  const pt = (o.phuongThucThanhToan || o.ghiChu || '').toUpperCase()
  return pt.includes('VNPAY')
}

const isVnPayOrder = computed(() => isVnPayOrderObj(selectedOrder.value))

// Counts cho từng tab
const pendingCount = computed(() => orders.value.filter(o => o.trangThai === 1).length)
const shippingCount = computed(() => orders.value.filter(o => o.trangThai === 2 || o.trangThai === 3 || o.trangThai === 4).length)
const completedCount = computed(() => orders.value.filter(o => o.trangThai === 5).length)
const cancelledCount = computed(() => orders.value.filter(o => o.trangThai === 6).length)

const filteredOrders = computed(() => {
  if (!currentClientUser.value && !route.query.code && !route.query.ma) {
    return []
  }

  let list = orders.value

  if (activeTab.value === 'pending') list = list.filter(o => o.trangThai === 1)
  else if (activeTab.value === 'shipping') list = list.filter(o => o.trangThai === 2 || o.trangThai === 3 || o.trangThai === 4)
  else if (activeTab.value === 'completed') list = list.filter(o => o.trangThai === 5)
  else if (activeTab.value === 'cancelled') list = list.filter(o => o.trangThai === 6)

  if (searchQuery.value && searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(o =>
      (o.maHoaDon && o.maHoaDon.toLowerCase().includes(q)) ||
      (o.tenKhachHang && o.tenKhachHang.toLowerCase().includes(q)) ||
      (o.soDienThoaiKhachHang && o.soDienThoaiKhachHang.includes(q))
    )
  }
  return list
})

watch(
  filteredOrders,
  (newList) => {
    if (newList && newList.length > 0) {
      loadItemsForList(newList)
    }
  },
  { immediate: true }
)

const handleBackToList = () => {
  selectedOrder.value = null
  selectedOrderItems.value = []
  orderLogs.value = []
  if (route.query.code || route.query.ma) {
    router.replace({ path: '/don-hang' })
  }
}

const loadOrders = async () => {
  const targetCode = route.query.code || route.query.ma

  // 1. Nếu có mã đơn trên URL: Tra cứu trực tiếp đơn đó
  if (targetCode) {
    if (!currentClientUser.value) {
      router.replace(`/tra-cuu?code=${targetCode}`)
      return
    }
    loading.value = true
    try {
      const res = await api.get(`/api/hoa-don/code/${targetCode}`)
      if (res.data && res.data.maHoaDon) {
        orders.value = [res.data]
        viewOrderDetail(res.data)
      }
    } catch (e) {
      console.warn('Không tìm thấy đơn theo mã:', e)
    } finally {
      loading.value = false
    }
    return
  }

  // 2. Nếu CHƯA ĐĂNG NHẬP: Tuyệt đối không tải đơn hàng của người khác
  if (!currentClientUser.value) {
    orders.value = []
    loading.value = false
    return
  }

  // 3. Nếu ĐÃ ĐĂNG NHẬP: Tải và lọc riêng đơn của khách hàng này
  loading.value = true
  try {
    const res = await api.get('/api/hoa-don')
    const list = Array.isArray(res.data) ? res.data : []
    list.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))

    const uid = currentClientUser.value.id
    const uPhone = currentClientUser.value.phone?.trim()
    const uEmail = currentClientUser.value.email?.trim()?.toLowerCase()

    const myOrders = list.filter(o => {
      const isOnline = Number(o.loaiDon) === 2 || (o.ghiChu && (o.ghiChu.toLowerCase().includes('website') || o.ghiChu.toLowerCase().includes('online')))
      if (!isOnline) return false
      const orderCustId = o.idKhachHang?.id ?? (typeof o.idKhachHang === 'number' || typeof o.idKhachHang === 'string' ? Number(o.idKhachHang) : null)
      return Boolean(orderCustId && Number(orderCustId) === Number(uid))



    })

    orders.value = myOrders
    loadItemsForList(myOrders)
  } catch (err) {
    console.error('Lỗi tải danh sách đơn hàng:', err)
  } finally {
    loading.value = false
  }
}

const viewOrderDetail = async (order) => {
  selectedOrder.value = order
  updateCountdown()
  selectedOrderItems.value = []
  orderLogs.value = []
  window.scrollTo({ top: 0, behavior: 'smooth' })

  try {
    const res = await api.get(`/api/hoa-don/code/${order.maHoaDon}/chi-tiet`)
    selectedOrderItems.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('Lỗi tải chi tiết sản phẩm:', e)
  }

  try {
    const resLogs = await api.get(`/api/hoa-don/code/${order.maHoaDon}/lich-su`)
    const logs = Array.isArray(resLogs.data) ? resLogs.data : []
    logs.sort((a, b) => new Date(b.thoiGian || 0) - new Date(a.thoiGian || 0))
    orderLogs.value = logs
  } catch (e) {
    console.error('Lỗi tải lịch sử đơn:', e)
  }
}

const getStatusLabel = (status) => {
  switch (status) {
    case 1: return { text: 'CHỜ XÁC NHẬN', class: 'status-pending' }
    case 2: return { text: 'ĐÃ XÁC NHẬN', class: 'status-confirmed' }
    case 3: return { text: 'CHỜ VẬN CHUYỂN', class: 'status-shipping' }
    case 4: return { text: 'ĐANG VẬN CHUYỂN', class: 'status-shipping' }
    case 5: return { text: 'HOÀN THÀNH', class: 'status-done' }
    case 6: return { text: 'ĐÃ HỦY', class: 'status-cancelled' }
    default: return { text: 'CHỜ XỬ LÝ', class: 'status-pending' }
  }
}

const handleCancelOrder = async () => {
  if (!selectedOrder.value) return
  const ok = await showAppConfirm(`Bạn có chắc chắn muốn hủy đơn hàng #${selectedOrder.value.maHoaDon}?`, 'Hủy đơn hàng')
  if (ok) {
    try {
      await api.put(`/api/hoa-don/${selectedOrder.value.id}/trang-thai`, {
        trangThai: 6,
        ghiChu: 'Khách hàng yêu cầu hủy đơn trên website'
      })
      selectedOrder.value.trangThai = 6
      await showAppAlert('Đã hủy đơn hàng thành công!', 'Hủy đơn hàng', 'success')
      loadOrders()
      viewOrderDetail(selectedOrder.value)
    } catch (e) {
      await showAppAlert('Không thể hủy đơn hàng vào lúc này. Vui lòng liên hệ hotline hỗ trợ!', 'Lỗi hủy đơn', 'error')
    }
  }
}

const payingVnpay = ref(false)
const openPayModal = async () => {
  if (!selectedOrder.value || !selectedOrder.value.maHoaDon) return
  payingVnpay.value = true
  try {
    const payAmount = Math.max(1, Math.round(Number(selectedOrder.value.tienSauGiamGia || selectedOrder.value.tongTienSauGiam || selectedOrder.value.tongTien || 0)))
    const res = await api.post('/api/vnpay/create-payment', {
      orderCode: selectedOrder.value.maHoaDon,
      amount: payAmount,
      orderInfo: `Thanh toan don hang ${selectedOrder.value.maHoaDon}`
    })
    if (res.data && res.data.paymentUrl) {
      const w = 850
      const h = 750
      const left = (window.screen.width - w) / 2
      const top = (window.screen.height - h) / 2
      window.open(
        res.data.paymentUrl,
        'VNPayPaymentPopup',
        `width=${w},height=${h},top=${top},left=${left},scrollbars=yes,status=no,resizable=yes`
      )
    }
  } catch (e) {
    await showAppAlert('Không thể kết nối tới cổng thanh toán VNPAY lúc này. Vui lòng thử lại sau!', 'Cổng thanh toán VNPAY', 'error')
  } finally {
    payingVnpay.value = false
  }
}

const quickPayOrder = (o) => {
  selectedOrder.value = o
  openPayModal()
}



const onMessageReceived = (event) => {
  if (event.data && (event.data.type === 'VNPAY_PAID_SUCCESS' || event.data.type === 'VNPAY_PAYMENT_RESULT')) {
    loadOrders()
    if (selectedOrder.value) {
      viewOrderDetail(selectedOrder.value)
    }
    if (event.data.type === 'VNPAY_PAID_SUCCESS') {
      showPendingToast.value = false
      showSuccessToast.value = true
      successToastMsg.value = `Thanh toán thành công cho đơn hàng #${event.data.orderCode || selectedOrder.value?.maHoaDon || ''} qua VNPAY!`
      setTimeout(() => {
        showSuccessToast.value = false
      }, 5000)
    }
  }
}

let autoRefreshTimer = null
let isRefreshingSilently = false

const refreshCurrentDataSilently = async () => {
  if (isRefreshingSilently) return
  isRefreshingSilently = true
  try {
    if (selectedOrder.value && selectedOrder.value.maHoaDon) {
      const code = selectedOrder.value.maHoaDon
      const prevStatus = Number(selectedOrder.value.trangThai)
      const prevUpdatedAt = selectedOrder.value.ngayCapNhat

      const [resOrder, resLogs] = await Promise.all([
        api.get(`/api/hoa-don/code/${code}`).catch(() => null),
        api.get(`/api/hoa-don/code/${code}/lich-su`).catch(() => null)
      ])

      if (resOrder && resOrder.data && resOrder.data.maHoaDon) {
        const newOrder = resOrder.data
        if (selectedOrder.value && selectedOrder.value.maHoaDon === code) {
          selectedOrder.value = newOrder

          if (Number(newOrder.trangThai) !== prevStatus || newOrder.ngayCapNhat !== prevUpdatedAt) {
            try {
              const resItems = await api.get(`/api/hoa-don/code/${code}/chi-tiet`)
              if (Array.isArray(resItems.data)) {
                selectedOrderItems.value = resItems.data
              }
            } catch (_) {}
          }

          const idx = orders.value.findIndex(o => o.maHoaDon === code)
          if (idx !== -1) {
            orders.value[idx] = newOrder
          }
        }
      }

      if (resLogs && Array.isArray(resLogs.data)) {
        const logs = resLogs.data
        logs.sort((a, b) => new Date(b.thoiGian || 0) - new Date(a.thoiGian || 0))
        orderLogs.value = logs
      }
    } else if (currentClientUser.value) {
      const res = await api.get('/api/hoa-don')
      const list = Array.isArray(res.data) ? res.data : []
      list.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))

      const uid = currentClientUser.value.id
      const myOrders = list.filter(o => {
        const isOnline = Number(o.loaiDon) === 2 || (o.ghiChu && (o.ghiChu.toLowerCase().includes('website') || o.ghiChu.toLowerCase().includes('online')))
        if (!isOnline) return false
        const orderCustId = o.idKhachHang?.id ?? (typeof o.idKhachHang === 'number' || typeof o.idKhachHang === 'string' ? Number(o.idKhachHang) : null)
        return Boolean(orderCustId && Number(orderCustId) === Number(uid))
      })
      orders.value = myOrders
    }
  } catch (_) {
  } finally {
    isRefreshingSilently = false
  }
}

const onVisibilityChange = () => {
  if (!document.hidden) {
    refreshCurrentDataSilently()
  }
}

const onWindowFocus = () => { refreshCurrentDataSilently(); return;
  if (selectedOrder.value && selectedOrder.value.trangThaiThanhToan !== 1) {
    loadOrders()
  }
}

onMounted(() => {
  loadOrders()
  autoRefreshTimer = setInterval(refreshCurrentDataSilently, 2500)
  timerInterval = setInterval(() => {
    if (true) {
      updateCountdown()


    }
  }, 1000)

  const codToast = sessionStorage.getItem('order_cod_success_toast')
  if (codToast) {
    sessionStorage.removeItem('order_cod_success_toast')
    showSuccessToast.value = true
    successToastMsg.value = codToast
    setTimeout(() => {
      showSuccessToast.value = false
    }, 6000)
  }

  if (sessionStorage.getItem('vnpay_pending_toast') === 'true') {
    sessionStorage.removeItem('vnpay_pending_toast')
    showPendingToast.value = true
    setTimeout(() => {
      showPendingToast.value = false
    }, 6000)
  }

  window.addEventListener('message', onMessageReceived)
  window.addEventListener('focus', onWindowFocus)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  window.removeEventListener('message', onMessageReceived)
  window.removeEventListener('focus', onWindowFocus)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="orders-page-shell">
    <!-- TOAST THÔNG BÁO GIAO DỊCH CHỜ / THÀNH CÔNG -->
    <transition name="toast-fade">
      <div v-if="showPendingToast" class="floating-toast toast-warning">
        <div class="toast-icon">⚠️</div>
        <div class="toast-body">
          <div class="toast-title">Thông báo thanh toán</div>
          <div class="toast-desc">Giao dịch chưa hoàn tất! Đơn hàng được lưu ở trạng thái Chờ thanh toán.</div>
        </div>
        <button class="toast-close" @click="showPendingToast = false">×</button>
      </div>
    </transition>

    <transition name="toast-fade">
      <div v-if="showSuccessToast" class="floating-toast toast-success">
        <div class="toast-icon">✅</div>
        <div class="toast-body">
          <div class="toast-title">Thanh toán thành công!</div>
          <div class="toast-desc">{{ successToastMsg }}</div>
        </div>
        <button class="toast-close" @click="showSuccessToast = false">×</button>
      </div>
    </transition>

    <div class="container-client">
      <div class="orders-layout">
        <!-- NỘI DUNG CHÍNH -->
        <main class="order-content-main">
          <!-- VIEW KHI CHƯA ĐĂNG NHẬP: CHỈ HIỂN THỊ 2 NÚT HÀNH ĐỘNG (Ảnh 2) -->
          <div v-if="!isClientLoggedIn" class="auth-gate-wrapper">
            <div class="auth-gate-card">
              <div class="auth-gate-icon">📦</div>
              <h2 class="auth-gate-title">Đăng nhập để xem đơn hàng</h2>
              <p class="auth-gate-desc">
                Vui lòng đăng nhập tài khoản khách hàng để theo dõi lịch sử và trạng thái các đơn hàng của bạn.
              </p>
              <div class="auth-gate-actions">
                <button type="button" class="btn-auth-gate-login" @click="showAuthModal = true">
                  Đăng Nhập Tài Khoản
                </button>
                <router-link to="/tra-cuu" class="btn-auth-gate-track">
                  Tra cứu nhanh bằng mã đơn &amp; SĐT &rarr;
                </router-link>
              </div>
            </div>
          </div>

          <!-- KHI ĐÃ ĐĂNG NHẬP -->
          <template v-else>
          <!-- ============================================== -->
          <!-- VIEW 1: DANH SÁCH ĐƠN HÀNG (KHI CHƯA CHỌN ĐƠN) -->
          <!-- ============================================== -->
          <!-- VIEW 1: DANH SÁCH ĐƠN HÀNG -->
          <div v-if="!selectedOrder" class="orders-list-view">
            <!-- Header Trang Danh Sách & Tìm Kiếm -->
            <div class="list-hero-header">
              <div class="header-titles">
                <h1 class="page-title">Đơn Hàng Của Bạn</h1>
                <p class="page-subtitle">Theo dõi trạng thái và lịch sử tất cả các đơn hàng</p>
              </div>

              <!-- Thanh tìm kiếm nhanh -->
              <div class="header-search-wrap" v-if="orders.length > 0">
                <div class="search-input-pill">
                  <span class="search-icon-svg">🔍</span>
                  <input
                    v-model="searchQuery"
                    type="text"
                    placeholder="Tìm theo Mã đơn (VD: HD026), tên người nhận..."
                    class="search-inner-input"
                  />
                  <button v-if="searchQuery" type="button" class="btn-clear-search" @click="searchQuery = ''">✕</button>
                </div>
              </div>
            </div>

            <!-- Thanh lọc Tab trạng thái phong cách mới hiện đại -->
            <div class="order-filter-nav">
              <button :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">
                Tất cả <span class="nav-count">{{ orders.length }}</span>
              </button>
              <button :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'">
                Chờ xác nhận <span class="nav-count" v-if="pendingCount > 0">{{ pendingCount }}</span>
              </button>
              <button :class="{ active: activeTab === 'shipping' }" @click="activeTab = 'shipping'">
                Đang giao <span class="nav-count" v-if="shippingCount > 0">{{ shippingCount }}</span>
              </button>
              <button :class="{ active: activeTab === 'completed' }" @click="activeTab = 'completed'">
                Hoàn thành <span class="nav-count" v-if="completedCount > 0">{{ completedCount }}</span>
              </button>
              <button :class="{ active: activeTab === 'cancelled' }" @click="activeTab = 'cancelled'">
                Đã hủy <span class="nav-count" v-if="cancelledCount > 0">{{ cancelledCount }}</span>
              </button>
            </div>

            <!-- Trạng thái Loading -->
            <div v-if="loading" class="loading-state-card">
              <div class="spinner-blue"></div>
              <p>Đang tải danh sách đơn hàng...</p>
            </div>

            <!-- Danh sách rỗng -->
            <div v-else-if="filteredOrders.length === 0" class="empty-orders-card">
              <div class="empty-icon">📦</div>
              <h3>Không tìm thấy đơn hàng nào</h3>
              <p>{{ orders.length === 0 ? 'Bạn chưa có đơn hàng nào tại cửa hàng.' : 'Không có đơn hàng nào khớp với điều kiện lọc hiện tại.' }}</p>
              <router-link to="/cua-hang" class="btn-primary-action">Khám phá sản phẩm ngay</router-link>
            </div>

            <!-- Danh sách các Card đơn hàng phong cách mới: Đẹp, Trực quan, Có ảnh sản phẩm -->
            <div v-else class="orders-cards-stack">
              <div
                v-for="o in filteredOrders"
                :key="o.id"
                class="order-summary-card"
              >
                <!-- Dòng 1: Mã đơn & Ngày & Loại đơn & Trạng thái -->
                <div class="card-head-row">
                  <div class="code-time-col">
                    <span class="order-code-badge">#{{ o.maHoaDon }}</span>
                    <span class="dot-sep">•</span>
                    <span class="order-created-time">{{ formatDateTime(o.ngayTao) }}</span>
                    <span class="order-type-tag" :class="o.loaiDon === 2 ? 'tag-online' : 'tag-pos'">
                      {{ o.loaiDon === 2 ? 'Online' : 'Tại quầy' }}
                    </span>
                  </div>

                  <span class="status-pill" :class="getStatusLabel(o.trangThai).class">
                    <span class="pill-dot"></span>
                    <span>{{ getStatusLabel(o.trangThai).text }}</span>
                  </span>
                </div>

                <!-- Dòng 2: Preview sản phẩm thực tế của đơn hàng -->
                <div class="card-products-preview">
                  <div
                    v-if="orderItemsMap[o.maHoaDon] && orderItemsMap[o.maHoaDon].length > 0"
                    class="preview-items-list"
                  >
                    <!-- Sản phẩm đầu tiên (luôn hiển thị) -->
                    <div class="preview-single-item">
                      <div class="preview-thumb-box">
                        <img
                          :src="resolveProductImg(orderItemsMap[o.maHoaDon][0])"
                          :alt="orderItemsMap[o.maHoaDon][0].tenSanPham"
                          class="preview-pic"
                          @error="onImgError"
                        />
                        <span class="badge-num">x{{ orderItemsMap[o.maHoaDon][0].soLuong || 1 }}</span>
                      </div>

                      <div class="preview-item-info">
                        <h4 class="preview-item-title">
                          {{ orderItemsMap[o.maHoaDon][0].tenSanPham || orderItemsMap[o.maHoaDon][0].idChiTietSanPham?.idSanPham?.tenSanPham || 'Áo Polo Nam FF' }}
                        </h4>
                        <div class="preview-item-variants">
                          <span class="tag-variant tag-sku" v-if="orderItemsMap[o.maHoaDon][0].maChiTietSanPham || orderItemsMap[o.maHoaDon][0].idChiTietSanPham?.maChiTietSanPham">
                            Mã: <strong>{{ orderItemsMap[o.maHoaDon][0].maChiTietSanPham || orderItemsMap[o.maHoaDon][0].idChiTietSanPham?.maChiTietSanPham }}</strong>
                          </span>
                          <span class="tag-variant">Màu: <strong>{{ orderItemsMap[o.maHoaDon][0].tenMauSac || 'Tiêu chuẩn' }}</strong></span>
                          <span class="tag-variant">Size: <strong>{{ orderItemsMap[o.maHoaDon][0].tenKichCo || 'M' }}</strong></span>
                        </div>
                      </div>

                      <div class="preview-item-price">
                        {{ formatCurrency((orderItemsMap[o.maHoaDon][0].giaDuocTinh || orderItemsMap[o.maHoaDon][0].donGia || o.tongTien) * (orderItemsMap[o.maHoaDon][0].soLuong || 1)) }}
                      </div>
                    </div>

                    <!-- Danh sách các sản phẩm còn lại khi click sổ ra -->
                    <transition name="preview-expand">
                      <div v-if="expandedOrders[o.maHoaDon]" class="preview-expanded-container">
                        <div
                          v-for="(it, itIdx) in orderItemsMap[o.maHoaDon].slice(1)"
                          :key="it.id || itIdx"
                          class="preview-single-item preview-extra-item"
                        >
                          <div class="preview-thumb-box">
                            <img
                              :src="resolveProductImg(it)"
                              :alt="it.tenSanPham"
                              class="preview-pic"
                              @error="onImgError"
                            />
                            <span class="badge-num">x{{ it.soLuong || 1 }}</span>
                          </div>

                          <div class="preview-item-info">
                            <h4 class="preview-item-title">
                              {{ it.tenSanPham || it.idChiTietSanPham?.idSanPham?.tenSanPham || 'Áo Polo Nam FF' }}
                            </h4>
                            <div class="preview-item-variants">
                              <span class="tag-variant tag-sku" v-if="it.maChiTietSanPham || it.idChiTietSanPham?.maChiTietSanPham">
                                Mã: <strong>{{ it.maChiTietSanPham || it.idChiTietSanPham?.maChiTietSanPham }}</strong>
                              </span>
                              <span class="tag-variant">Màu: <strong>{{ it.tenMauSac || 'Tiêu chuẩn' }}</strong></span>
                              <span class="tag-variant">Size: <strong>{{ it.tenKichCo || 'M' }}</strong></span>
                            </div>
                          </div>

                          <div class="preview-item-price">
                            {{ formatCurrency((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1)) }}
                          </div>
                        </div>
                      </div>
                    </transition>

                    <!-- Nút bấm Xổ ra / Thu gọn sản phẩm -->
                    <div
                      v-if="orderItemsMap[o.maHoaDon].length > 1"
                      class="preview-more-badge cursor-pointer"
                      :class="{ 'is-expanded': expandedOrders[o.maHoaDon] }"
                      @click.stop="toggleExpandOrder(o.maHoaDon)"
                      :title="expandedOrders[o.maHoaDon] ? 'Thu gọn danh sách sản phẩm' : 'Nhấn để xem toàn bộ ' + orderItemsMap[o.maHoaDon].length + ' sản phẩm trong đơn này'"
                    >
                      <template v-if="!expandedOrders[o.maHoaDon]">
                        <span>+ {{ orderItemsMap[o.maHoaDon].length - 1 }} sản phẩm khác trong đơn này</span>
                        <span class="badge-arrow">▼</span>
                      </template>
                      <template v-else>
                        <span>▲ Thu gọn danh sách ({{ orderItemsMap[o.maHoaDon].length }} sản phẩm)</span>
                      </template>
                    </div>
                  </div>

                  <div v-else class="preview-placeholder">
                    <span class="placeholder-icon">🛍️</span>
                    <span class="placeholder-text">Kiện hàng thời trang FF T-Shirt</span>
                  </div>
                </div>

                <!-- Dòng 3: Địa chỉ nhận hàng -->
                <div class="card-shipping-line">
                  <span class="ship-icon">📍</span>
                  <div class="ship-info-text">
                    <strong class="rcp-name">{{ o.tenKhachHang || o.tenKhachHangHienThi || 'Khách hàng' }}</strong>
                    <span class="rcp-phone" v-if="o.soDienThoaiKhachHang || o.soDienThoaiHienThi">({{ o.soDienThoaiKhachHang || o.soDienThoaiHienThi }})</span>
                    <span class="rcp-addr">— {{ o.diaChiNhanHang || 'Nhận tại cửa hàng' }}</span>
                  </div>
                </div>

                <!-- Dòng 4: Chân Card: Thanh toán + Tổng tiền + Nút hành động -->
                <div class="card-foot-row">
                  <div class="foot-payment-state">
                    <span class="pay-icon">💳</span>
                    <span class="pay-text">{{ o.phuongThucThanhToan || (isVnPayOrderObj(o) ? 'VNPAY' : 'COD') }}</span>
                    <span class="pipe-sep">•</span>
                    <span class="pay-status-pill" :class="o.trangThai === 6 ? 'text-green' : (o.trangThaiThanhToan === 1 ? 'text-green' : 'text-orange')">
                      {{ o.trangThai === 6 ? 'Đã hoàn tiền' : (o.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán') }}
                    </span>
                  </div>

                  <div class="foot-actions-and-total">
                    <div class="foot-total-price">
                      <span class="lbl-total">Thành tiền:</span>
                      <strong class="val-total">{{ formatCurrency(getOrderFinalTotal(o)) }}</strong>
                    </div>

                    <div class="foot-btn-group">
                      <button
                        v-if="isVnPayOrderObj(o) && o.trangThaiThanhToan !== 1 && o.trangThai === 1"
                        class="btn-quick-pay"
                        @click.stop="quickPayOrder(o)"
                      >
                        💳 Thanh toán ngay
                      </button>

                      <button
                        class="btn-view-order-detail"
                        @click="viewOrderDetail(o)"
                      >
                        Xem chi tiết
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- ============================================== -->
          <!-- VIEW 2: CHI TIẾT ĐƠN HÀNG (KHI CHỌN 1 ĐƠN)    -->
          <!-- ============================================== -->
          <div v-else class="order-detail-card">
            <!-- Thanh quay lại & Mã đơn -->
            <div class="detail-top-nav">
              <button class="btn-back-to-list" @click="handleBackToList">
                <span class="arrow">←</span>
                <span>Quay lại danh sách đơn hàng</span>
              </button>
              <div class="order-id-status">
                <span class="code">MÃ ĐƠN: #{{ selectedOrder.maHoaDon }}</span>
                <span class="bar-sep">|</span>
                <span class="status-badge" :class="getStatusLabel(selectedOrder.trangThai).class">
                  {{ getStatusLabel(selectedOrder.trangThai).text }}
                </span>
              </div>
            </div>

            <!-- ==================================================== -->
            <!-- BỐ CỤC 2 CỘT CHUẨN ĐỒNG BỘ 100% VỚI CHI TIẾT HÓA ĐƠN ADMIN -->
            <!-- ==================================================== -->
            <div class="main-layout-grid">
              <!-- ============================================== -->
              <!-- CỘT TRÁI: TRẠNG THÁI ĐƠN HÀNG + KHÁCH HÀNG & GIAO HÀNG -->
              <!-- ============================================== -->
              <div class="left-section">
                <!-- KHUNG 1: TRẠNG THÁI ĐƠN HÀNG -->
                <div class="card-box status-card card-order-status">
                  <div class="card-header-styled">
                    <div class="card-title-styled">
                      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                        <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
                      </svg>
                      Trạng thái đơn hàng
                    </div>
                    <span class="badge-status-top" :class="'badge-status-' + selectedOrder.trangThai">
                      {{ getStatusLabel(selectedOrder.trangThai).text }}
                    </span>
                  </div>

                  <!-- Stepper timeline: Đồng bộ các bước chuẩn Admin -->
                  <div class="stepper-track-wrap">
                    <div
                      v-for="(step, idx) in clientTimelineSteps"
                      :key="step.id"
                      class="stepper-step-item"
                      :class="{
                        'step-item-done': step.done,
                        'step-item-active': step.active,
                        'step-item-pending': step.pending,
                        'step-item-cancelled': step.isCancelled
                      }"
                    >
                      <!-- Vạch nối -->
                      <div
                        v-if="idx > 0"
                        class="step-connector-line"
                        :class="{
                          'line-active': !step.pending && !step.isCancelled,
                          'line-cancelled': step.isCancelled
                        }"
                      ></div>

                      <!-- Vòng tròn icon -->
                      <div
                        class="step-circle-icon"
                        :class="{
                          'circle-done': step.done,
                          'circle-active': step.active && !step.isCancelled,
                          'circle-pending': step.pending,
                          'circle-red': step.isCancelled
                        }"
                      >
                        <!-- Icon đồng hồ cát (chờ xác nhận) -->
                        <svg v-if="step.icon === 'hourglass'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2">
                          <path d="M5 22h14M5 2h14M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2" />
                        </svg>
                        <!-- Icon check (đã xác nhận) -->
                        <svg v-else-if="step.icon === 'check'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                        <!-- Icon box (chờ vận chuyển) -->
                        <svg v-else-if="step.icon === 'box'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                          <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                          <line x1="12" y1="22.08" x2="12" y2="12"></line>
                        </svg>
                        <!-- Icon truck (đang vận chuyển) -->
                        <svg v-else-if="step.icon === 'truck'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                          <rect x="1" y="3" width="15" height="13"></rect>
                          <polygon points="16 8 20 8 23 11 23 16 16 16 8"></polygon>
                          <circle cx="5.5" cy="18.5" r="2.5"></circle>
                          <circle cx="18.5" cy="18.5" r="2.5"></circle>
                        </svg>
                        <!-- Icon flag (hoàn thành) -->
                        <svg v-else-if="step.icon === 'flag'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"></path>
                          <line x1="4" y1="22" x2="4" y2="15"></line>
                        </svg>
                        <!-- Icon cancelled (đã hủy) -->
                        <svg v-else-if="step.icon === 'cancelled'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                          <line x1="18" y1="6" x2="6" y2="18"></line>
                          <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                      </div>

                      <!-- Tên & thời gian bước -->
                      <div
                        class="step-item-title"
                        :class="{
                          'title-active': step.active && !step.isCancelled,
                          'title-done': step.done,
                          'title-pending': step.pending,
                          'text-danger': step.isCancelled
                        }"
                      >
                        {{ step.name }}
                      </div>
                      <div v-if="step.time" class="step-item-time">{{ step.time }}</div>
                    </div>
                  </div>

                  <!-- Nhóm nút hành động ở góc dưới khung trạng thái -->
                  <div class="stepper-footer-actions">
                    <div class="left-action-buttons">
                      <button class="btn-history-outline" type="button" @click="showHistoryModal = true">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                          <circle cx="12" cy="12" r="10"></circle>
                          <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                        Lịch sử thao tác
                      </button>
                    </div>

                    <div class="right-action-buttons">
                      <!-- Nút Hủy đơn nếu ở trạng thái Chờ xác nhận (1) -->
                      <button
                        v-if="selectedOrder.trangThai === 1"
                        class="btn-cancel-action"
                        type="button"
                        @click="handleCancelOrder"
                      >
                        ✕ Hủy đơn
                      </button>
                    </div>
                  </div>
                </div>

                <!-- KHUNG 2: KHÁCH HÀNG & GIAO HÀNG (2 CỘT DUAL GRID) -->
                <div class="info-dual-grid">
                  <!-- Card Thông tin khách hàng -->
                  <div class="card-box sub-info-card">
                    <div class="card-header-styled">
                      <div class="card-title-styled">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                          <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"></path>
                          <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        Thông tin khách hàng
                      </div>
                    </div>
                    <div class="info-list">
                      <div class="info-list-row">
                        <span class="info-row-label">Tên khách hàng</span>
                        <span class="info-row-val font-bold">
                          {{ selectedOrder.tenKhachHang || selectedOrder.tenKhachHangHienThi || 'Khách hàng' }}
                        </span>
                      </div>
                      <div class="info-list-row">
                        <span class="info-row-label">Số điện thoại</span>
                        <span class="info-row-val font-mono">
                          {{ selectedOrder.soDienThoaiKhachHang || selectedOrder.soDienThoaiHienThi || '—' }}
                        </span>
                      </div>
                      <div class="info-list-row">
                        <span class="info-row-label">Email</span>
                        <span class="info-row-val">
                          {{ selectedOrder.emailKhachHang || selectedOrder.email || currentClientUser?.email || '—' }}
                        </span>
                      </div>
                    </div>
                  </div>

                  <!-- Card Thông tin giao hàng -->
                  <div class="card-box sub-info-card">
                    <div class="card-header-styled">
                      <div class="card-title-styled">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                          <path d="M12 2a8 8 0 00-8 8c0 5.25 8 12 8 12s8-6.75 8-12a8 8 0 00-8-8z"></path>
                          <circle cx="12" cy="10" r="3"></circle>
                        </svg>
                        Thông tin giao hàng
                      </div>
                    </div>
                    <div class="info-list">
                      <div class="info-list-row">
                        <span class="info-row-label">Địa chỉ giao hàng</span>
                        <span class="info-row-val">{{ selectedOrder.diaChiNhanHang || 'Nhận tại cửa hàng' }}</span>
                      </div>
                      <div class="info-list-row">
                        <span class="info-row-label">Đơn vị vận chuyển</span>
                        <span class="info-row-val">{{ selectedOrder.donViVanChuyen || (isPosOrder(selectedOrder) ? 'Tại quầy' : 'Giao Hàng Nhanh (GHN)') }}</span>
                      </div>
                      <div class="info-list-row">
                        <span class="info-row-label">Ghi chú</span>
                        <span class="info-row-val">{{ selectedOrder.ghiChu || (selectedOrder.maVanDon ? 'Mã vận đơn: #' + selectedOrder.maVanDon : '—') }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ============================================== -->
              <!-- CỘT PHẢI: TỔNG KẾT THANH TOÁN + LỊCH SỬ THANH TOÁN -->
              <!-- ============================================== -->
              <div class="right-section">
                <!-- Khung: Tổng kết thanh toán -->
                <div class="card-box summary-card">
                  <div class="card-header-styled">
                    <div class="card-title-styled">
                      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                        <rect x="2" y="4" width="20" height="18" rx="2" ry="2"></rect>
                        <line x1="16" y1="2" x2="16" y2="6"></line>
                        <line x1="8" y1="2" x2="8" y2="6"></line>
                        <line x1="2" y1="10" x2="22" y2="10"></line>
                      </svg>
                      Tổng kết thanh toán
                    </div>
                    <span v-if="getOrderDiscount(selectedOrder) > 0" class="badge-voucher-applied-small">
                      ĐÃ DÙNG VOUCHER
                    </span>
                    <span v-else class="badge-no-voucher-small">
                      KHÔNG DÙNG VOUCHER
                    </span>
                  </div>

                  <div class="summary-body">
                    <div class="summary-line">
                      <span class="s-label">Tổng tiền hàng</span>
                      <span class="s-val">{{ formatCurrency(getOrderOriginalSubtotal(selectedOrder)) }}</span>
                    </div>
                    <div v-if="getOrderCampaignDiscount(selectedOrder) > 0" class="summary-line summary-discount-row">
                      <span class="s-label">Đợt giảm giá</span>
                      <span class="s-val text-discount">-{{ formatCurrency(getOrderCampaignDiscount(selectedOrder)) }}</span>
                    </div>

                    <!-- Dòng Giảm giá -->
                    <div class="summary-line summary-discount-row">
                      <span class="s-label discount-label-with-code">
                        <span>Giảm giá</span>
                        <span v-if="voucherDisplayName" class="inline-voucher-pill">
                          {{ voucherDisplayName }}
                        </span>
                      </span>
                      <span class="s-val" :class="{ 'text-discount': getOrderDiscount(selectedOrder) > 0 }">
                        {{ getOrderDiscount(selectedOrder) > 0 ? '-' : '- ' }}{{ formatCurrency(getOrderDiscount(selectedOrder)) }}
                      </span>
                    </div>

                    <div v-if="getOrderShippingFee(selectedOrder) > 0" class="summary-line">
                      <span class="s-label">Phí vận chuyển</span>
                      <span class="s-val">+{{ formatCurrency(getOrderShippingFee(selectedOrder)) }}</span>
                    </div>

                    <div class="summary-divider"></div>

                    <div class="summary-total-line">
                      <span class="total-label-caps">TỔNG TIỀN</span>
                      <span class="total-price-blue">{{ formatCurrency(getOrderFinalTotal(selectedOrder)) }}</span>
                    </div>
                  </div>
                </div>

                <!-- Khung: Lịch sử thanh toán -->
                <div class="card-box payment-card">
                  <div class="card-header-styled">
                    <div class="card-title-styled">
                      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                        <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
                        <line x1="1" y1="10" x2="23" y2="10"></line>
                      </svg>
                      Lịch sử thanh toán
                    </div>
                  </div>

                  <div class="payment-inner-box">
                    <div class="pay-top-row">
                      <span class="pay-label-sub">Phương thức thanh toán</span>
                      <span class="badge-status-pill" :class="selectedOrder.trangThai === 6 ? 'pill-paid' : (selectedOrder.trangThaiThanhToan === 1 ? 'pill-paid' : 'pill-unpaid')">
                        <span class="pill-dot">●</span> {{ selectedOrder.trangThai === 6 ? 'Đã hoàn tiền' : (selectedOrder.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán') }}
                      </span>
                    </div>

                    <div class="pay-method-title">{{ selectedOrder.phuongThucThanhToan || (isVnPayOrder ? 'Cổng thanh toán VNPAY' : 'Thanh toán khi nhận hàng (COD)') }}</div>
                    <div class="pay-time-trans" v-if="selectedOrder.ngayTao">
                      {{ formatDateTime(selectedOrder.ngayTao) }}
                    </div>

                    <div class="pay-amount-row">
                      <span class="pay-label-sub">Số tiền</span>
                      <span class="pay-amount-bold">{{ formatCurrency(getOrderFinalTotal(selectedOrder)) }}</span>
                    </div>
                  </div>

                  <div class="payment-actions-col" v-if="isVnPayOrder && selectedOrder.trangThaiThanhToan !== 1 && selectedOrder.trangThai === 1">
                    <button
                      class="btn-quick-pay-lg"
                      type="button"
                      @click="openPayModal"
                    >
                      💳 Thanh toán ngay trực tuyến (VNPAY)
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- ============================================== -->
            <!-- KHUNG 5: DANH SÁCH SẢN PHẨM                   -->
            <!-- ============================================== -->
            <div class="card-box products-card">
              <div class="card-header-styled">
                <div class="card-title-styled">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                    <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"></path>
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                    <line x1="12" y1="22.08" x2="12" y2="12"></line>
                  </svg>
                  Danh sách sản phẩm
                </div>
                <div class="product-count-sub">{{ selectedOrderItems.length }} sản phẩm</div>
              </div>

              <div v-if="selectedOrderItems.length === 0" class="items-loading-box">
                Đang tải thông tin sản phẩm...
              </div>

              <div v-else class="items-table-list">
                <div v-for="it in selectedOrderItems" :key="it.id" class="order-product-row">
                  <div class="product-thumb-wrap">
                    <img
                      :src="resolveProductImg(it)"
                      @error="onImgError"
                      alt="Sản phẩm"
                      class="product-thumb"
                    />
                  </div>
                  <div class="product-info-col">
                    <span class="prod-name">{{ it.tenSanPham || 'Áo thun FF' }}</span>
                    <div class="prod-meta-badges">
                      <span class="meta-tag code-tag font-mono" style="background:#e0f2fe; color:#0369a1; border:1px solid #bae6fd; font-weight:700;" v-if="it.maChiTietSanPham || it.idChiTietSanPham?.maChiTietSanPham">
                        Mã: {{ it.maChiTietSanPham || it.idChiTietSanPham?.maChiTietSanPham }}
                      </span>
                      <span class="meta-tag color-tag" v-if="it.tenMauSac">Màu: {{ it.tenMauSac }}</span>
                      <span class="meta-tag size-tag" v-if="it.tenKichCo">Size: {{ it.tenKichCo }}</span>
                      <span class="meta-tag qty-tag">Số lượng: x{{ it.soLuong }}</span>
                    </div>
                  </div>
                  <div class="product-price-col">
                    <span class="price-val">{{ formatCurrency(it.giaDuocTinh || it.donGia || 0) }}</span>
                    <span class="unit-price-sub" v-if="it.soLuong > 1">
                      {{ formatCurrency((it.giaDuocTinh || it.donGia || 0) / it.soLuong) }}/cái
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Modal Lịch sử thao tác -->
            <div v-if="showHistoryModal" class="modal-backdrop" @click.self="showHistoryModal = false">
              <div class="modal-container">
                <div class="modal-header">
                  <h3 class="card-title">Lịch Sử Thao Tác Đơn Hàng</h3>
                  <button class="btn-close-modal" @click="showHistoryModal = false">✕</button>
                </div>
                <div class="modal-body">
                  <div v-if="orderLogs.length === 0" class="text-center text-muted" style="padding: 20px;">
                    Chưa có lịch sử thao tác nào được ghi nhận.
                  </div>
                  <div v-else class="logs-timeline-list">
                    <div v-for="(log, idx) in orderLogs" :key="log.id || idx" class="log-timeline-row">
                      <div class="log-dot-badge">✓</div>
                      <div class="log-detail-wrapper">
                        <div class="log-top-line">
                          <span class="log-action-text font-bold">{{ log.hanhDong || log.action }}</span>
                          <span class="log-time-text">{{ formatDateTime(log.thoiGian) }}</span>
                        </div>
                        <div class="log-sub-info">
                          <span>Người thực hiện: <strong>{{ getActorName(log) }}</strong></span>
                          <span v-if="log.ghiChu && log.ghiChu.trim() !== String(log.hanhDong || log.action || '').trim()"> — {{ log.ghiChu }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- FOOTER HÀNH ĐỘNG CỦA CHI TIẾT (ĐẨY NÚT HỦY XUỐNG DƯỚI & LÀM ĐẸP NÚT TIẾP TỤC MUA SẮM) -->
            <div class="detail-bottom-actions">
              <div class="bottom-actions-left">
                <button class="btn-back-outline" @click="handleBackToList">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="19" y1="12" x2="5" y2="12"></line>
                    <polyline points="12 19 5 12 12 5"></polyline>
                  </svg>
                  <span>Quay lại danh sách đơn hàng</span>
                </button>
              </div>

              <div class="bottom-actions-right">
                <!-- Nút Hủy Đơn Hàng (Đẩy xuống dưới theo yêu cầu - Ảnh 1) -->
                <button
                  type="button"
                  class="btn-cancel-order"
                  v-if="selectedOrder.trangThai === 1"
                  @click="handleCancelOrder"
                  title="Hủy đơn hàng này"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                  </svg>
                  <span>Hủy đơn hàng</span>
                </button>

                <!-- Nút Tiếp Tục Mua Sắm (Logo Vector SVG Thời Trang Đẹp Mắt - Ảnh 2) -->
                <router-link to="/cua-hang" class="btn-shop-more">
                  <svg class="btn-shop-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <path d="M16 10a4 4 0 0 1-8 0"></path>
                  </svg>
                  <span>Tiếp tục mua sắm</span>
                </router-link>
              </div>
            </div>
          </div>
        
          </template>
        </main>
      </div>
    </div>
  </div>
    <!-- Modal Đăng nhập / Đăng ký tài khoản -->
    <ModalDangNhap
      v-model="showAuthModal"
      @login-success="handleLoginSuccess"
    />
  </template>

<style scoped>
.orders-page-shell {
  background-color: #f8fafc;
  min-height: 85vh;
  padding: 32px 0 60px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

@media (min-width: 1600px) {
  .container-client {
    max-width: 1540px;
    padding: 0 2rem;
  }
}

@media (max-width: 640px) {
  .container-client {
    padding: 0 1rem;
  }
}

/* Floating toasts */
.floating-toast {
  position: fixed;
  top: 85px;
  right: 28px;
  background: #ffffff;
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
  z-index: 99999;
  border-left: 4px solid #16a34a;
  max-width: 380px;
}

.toast-warning {
  border-left-color: #f59e0b;
}

.toast-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.toast-title {
  font-weight: 700;
  font-size: 13.5px;
  color: #1e293b;
}

.toast-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.toast-close {
  background: none;
  border: none;
  font-size: 18px;
  color: #94a3b8;
  cursor: pointer;
  margin-left: auto;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* Orders Layout */
.orders-layout {
  width: 100%;
}

.order-content-main {
  width: 100%;
}

/* ============================================== */
/* VIEW 1: DANH SÁCH ĐƠN HÀNG (PHONG CÁCH MỚI)    */
/* ============================================== */
.orders-list-view {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-hero-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.page-title {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 28px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0 0 6px;
  letter-spacing: -0.01em;
}

.page-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin: 0;
}

.header-search-wrap {
  flex-shrink: 0;
}

.search-input-pill {
  position: relative;
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 999px;
  padding: 0 14px;
  width: 320px;
  height: 40px;
  transition: all 0.2s ease;
}

.search-input-pill:focus-within {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.search-icon-svg {
  font-size: 13px;
  color: #94a3b8;
  margin-right: 8px;
}

.search-inner-input {
  border: none;
  outline: none;
  background: transparent;
  width: 100%;
  font-size: 13px;
  color: #1e293b;
}

.btn-clear-search {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
}

/* Thanh Tabs Lọc Trạng Thái */
.order-filter-nav {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 6px;
  border-bottom: 1px solid #eef2f6;
}

.order-filter-nav button {
  padding: 9px 18px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.order-filter-nav button:hover {
  border-color: #d2a764;
  color: #d2a764;
}

.order-filter-nav button.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 3px 8px rgba(73, 104, 131, 0.25);
}

.nav-count {
  background: rgba(0, 0, 0, 0.08);
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 11px;
}

.order-filter-nav button.active .nav-count {
  background: #d2a764;
  color: #ffffff;
}

/* Card Đơn Hàng Mới */
.orders-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.order-summary-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #eef2f6;
  padding: 20px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: all 0.2s ease;
}

.order-summary-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.card-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.code-time-col {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
}

.order-code-badge {
  font-weight: 800;
  color: #2c3e50;
  font-size: 14.5px;
}

.dot-sep {
  color: #cbd5e1;
}

.order-created-time {
  font-size: 12.5px;
  color: #64748b;
  font-family: monospace;
}

.order-type-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.tag-online {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.tag-pos {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 800;
  padding: 5px 12px;
  border-radius: 999px;
  letter-spacing: 0.3px;
}

.pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.status-pending {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}
.status-pending .pill-dot { background: #d97706; }

.status-confirmed {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
.status-confirmed .pill-dot { background: #3b82f6; }

.status-shipping {
  background: #f5f3ff;
  color: #6d28d9;
  border: 1px solid #ddd6fe;
}
.status-shipping .pill-dot { background: #8b5cf6; }

.status-done {
  background: #ecfdf5;
  color: #15803d;
  border: 1px solid #a7f3d0;
}
.status-done .pill-dot { background: #10b981; }

.status-cancelled {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}
.status-cancelled .pill-dot { background: #ef4444; }

/* Preview sản phẩm trong card */
.card-products-preview {
  padding: 4px 0;
}

.preview-single-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.preview-thumb-box {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  flex-shrink: 0;
}

.preview-pic {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-thumb-box .badge-num {
  position: absolute;
  top: 0;
  right: 0;
  background: #496883;
  color: #ffffff;
  font-size: 9.5px;
  font-weight: 800;
  padding: 1px 5px;
  border-bottom-left-radius: 4px;
}

.preview-item-info {
  flex: 1;
}

.preview-item-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 5px;
}

.preview-item-variants {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-variant {
  background: #f8fafc;
  border: 1px solid #eef2f6;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  color: #64748b;
}

.tag-variant strong {
  color: #334155;
}

.preview-item-price {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  flex-shrink: 0;
}

.tag-variant.tag-sku {
  background: #eff6ff;
  border-color: #dbeafe;
  color: #1d4ed8;
}

.tag-variant.tag-sku strong {
  color: #1e40af;
  font-family: monospace;
}

.preview-expanded-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}

.preview-single-item.preview-extra-item {
  padding-left: 2px;
}

.preview-more-badge {
  margin-top: 10px;
  font-size: 12.5px;
  color: #2563eb;
  font-weight: 600;
  background: #f0f7ff;
  border: 1px solid #bfdbfe;
  padding: 5px 12px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  user-select: none;
  transition: all 0.2s ease;
}

.preview-more-badge.cursor-pointer {
  cursor: pointer;
}

.preview-more-badge.cursor-pointer:hover {
  background: #dbeafe;
  color: #1d4ed8;
  border-color: #93c5fd;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.1);
  transform: translateY(-1px);
}

.preview-more-badge.is-expanded {
  background: #f1f5f9;
  color: #475569;
  border-color: #cbd5e1;
}

.preview-more-badge.is-expanded:hover {
  background: #e2e8f0;
  color: #1e293b;
  box-shadow: none;
  transform: none;
}

.preview-more-badge .badge-arrow {
  font-size: 10px;
  margin-left: 2px;
  transition: transform 0.2s ease;
}

.preview-expand-enter-active,
.preview-expand-leave-active {
  transition: all 0.25s ease-out;
}

.preview-expand-enter-from,
.preview-expand-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.preview-placeholder {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
  color: #64748b;
}

/* Dòng địa chỉ */
.card-shipping-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 12.5px;
  color: #64748b;
  background: #fbfcfd;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.ship-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.ship-info-text strong {
  color: #1e293b;
}

.ship-phone {
  margin: 0 4px;
  color: #475569;
}

.ship-addr {
  color: #64748b;
}

/* Chân Card */
.card-foot-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  flex-wrap: wrap;
  gap: 12px;
}

.foot-payment-state {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #64748b;
}

.pay-status-pill {
  font-weight: 700;
  font-size: 12px;
}

.foot-actions-and-total {
  display: flex;
  align-items: center;
  gap: 18px;
}

.foot-total-price {
  text-align: right;
}

.lbl-total {
  font-size: 12px;
  color: #64748b;
  margin-right: 6px;
}

.val-total {
  font-size: 17.5px;
  font-weight: 800;
  color: #496883;
}

.foot-btn-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-track-action {
  background: #ffffff;
  color: #496883;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-track-action:hover {
  background: #f8fafc;
  border-color: #496883;
}

.btn-quick-pay {
  background: #d2a764;
  color: #ffffff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-quick-pay:hover {
  background: #c99a4e;
}

.btn-view-order-detail {
  background: #496883;
  color: #ffffff;
  border: none;
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
}

.btn-view-order-detail:hover {
  background: #364f66;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .list-hero-header {
    flex-direction: column;
    align-items: stretch;
  }
  .search-input-pill {
    width: 100%;
  }
  .card-foot-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .foot-actions-and-total {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }
}

/* ============================================== */
/* VIEW 2: CHI TIẾT ĐƠN HÀNG                      */
/* ============================================== */
.order-detail-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid #eef2f6;
}

.btn-back-to-list {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fdfaf3;
  border: 1px solid #d2a764;
  color: #496883;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back-to-list:hover {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.order-id-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.order-id-status .code {
  font-weight: 800;
  color: #496883;
}

.bar-sep {
  color: #cbd5e1;
}

.status-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 20px;
}

/* ========================================================
   STEPPER TIẾN ĐỘ ĐƠN HÀNG (HIỆN ĐẠI & TINH TẾ - ẢNH 4)
   ======================================================== */
.order-stepper-box {
  background: #ffffff;
  border-radius: 14px;
  padding: 24px 28px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px -4px rgba(15, 23, 42, 0.05);
  margin-bottom: 20px;
}

.stepper-track {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
  overflow-x: auto;
  padding-bottom: 6px;
}

.step-point {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 140px;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}

.step-icon {
  width: 46px;
  height: 46px;
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  color: #94a3b8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.step-point.completed .step-icon {
  background: #1e293b;
  border-color: #1e293b;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(30, 41, 59, 0.22);
}

.step-point.current .step-icon {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  border-color: #d97706;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.35);
  animation: pulseStepModern 2.2s infinite;
}

@keyframes pulseStepModern {
  0% { box-shadow: 0 0 0 0 rgba(217, 119, 6, 0.45); }
  70% { box-shadow: 0 0 0 10px rgba(217, 119, 6, 0); }
  100% { box-shadow: 0 0 0 0 rgba(217, 119, 6, 0); }
}

.step-point.step-cancelled .step-icon,
.step-point.step-failed .step-icon {
  background: #ef4444 !important;
  border-color: #ef4444 !important;
  color: #ffffff !important;
  animation: none !important;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3) !important;
}

.step-line {
  flex: 1;
  max-width: 140px;
  min-width: 50px;
  height: 3px;
  background: #e2e8f0;
  margin: -24px 8px 0;
  position: relative;
  z-index: 1;
  border-radius: 999px;
  transition: all 0.3s ease;
}

.step-line.active {
  background: #1e293b;
}

.step-line.line-failed,
.step-line.line-cancelled {
  background: #fca5a5 !important;
}

.step-name {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  line-height: 1.35;
}

.step-point.completed .step-name {
  color: #0f172a;
}

.step-point.current .step-name {
  color: #b45309;
}

.step-point.step-cancelled .step-name,
.step-point.step-failed .step-name {
  color: #dc2626 !important;
}

.step-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 3px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

/* ========================================================
   HAI BẢNG SONG SONG: THÔNG TIN GIAO NHẬN & CHI TIẾT THANH TOÁN
   ======================================================== */
.order-double-panels-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: stretch;
  margin-bottom: 20px;
}

.panel-box-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px 22px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}

.panel-box-item:hover {
  border-color: #cbd5e1;
}

.panel-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf2f7;
}

.panel-title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pin-icon-bg {
  background: #ffe4e6;
  color: #e11d48;
}

.card-icon-bg {
  background: #eff6ff;
  color: #2563eb;
}

.panel-box-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.panel-box-content {
  display: flex;
  flex-direction: column;
  gap: 11px;
  flex: 1;
}

.panel-field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13.5px;
  line-height: 1.5;
}

.field-label {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.field-value {
  color: #1e293b;
  text-align: right;
  max-width: 65%;
  word-break: break-word;
}

.field-value.font-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  letter-spacing: 0.02em;
}

.field-value.addr-val {
  font-weight: 500;
  line-height: 1.4;
}

.field-badge-status {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 999px;
}

.badge-green {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.badge-orange {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}

.panel-divider {
  border-top: 1px dashed #e2e8f0;
  margin: 4px 0;
}

.calc-field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13.5px;
  color: #475569;
}

.calc-label {
  color: #64748b;
  font-size: 13px;
}

.calc-val {
  font-weight: 600;
  color: #1e293b;
}

.calc-field-row.text-green .calc-val {
  color: #16a34a;
}

.ship-val {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.ghn-mini-logo {
  height: 14px;
  object-fit: contain;
}

.total-calc-row {
  padding-top: 4px;
}

.total-label-text {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.total-number-big {
  font-size: 1.25rem;
  font-weight: 800;
  color: #b45309;
}

/* ========================================================
   KHỐI SẢN PHẨM TRONG ĐƠN (ẢNH 3 - SANG TRỌNG, HIỆN ĐẠI)
   ======================================================== */
.info-section-box {
  background: #ffffff;
  border-radius: 14px;
  padding: 22px 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  margin-bottom: 20px;
}

.sec-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.sec-title-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sec-icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: #fef3c7;
  color: #d97706;
  border: 1px solid #fde68a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sec-title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.02em;
}

.sec-count-badge {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}

.items-table-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-product-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: #ffffff;
  border: 1px solid #edf2f7;
  border-radius: 12px;
  transition: all 0.18s ease;
}

.order-product-row:hover {
  border-color: #cbd5e1;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.03);
}

.product-thumb-wrap {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background-color: #f8fafc;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prod-name {
  font-size: 14.5px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.prod-meta-badges {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.meta-tag {
  font-size: 11.5px;
  padding: 2px 7px;
  border-radius: 6px;
  font-weight: 500;
}

.color-tag, .size-tag {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.qty-tag {
  background: #eff6ff;
  border: 1px solid #dbeafe;
  color: #2563eb;
  font-weight: 700;
}

.product-price-col {
  text-align: right;
  flex-shrink: 0;
}

.price-val {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.unit-price-sub {
  display: block;
  font-size: 11.5px;
  color: #94a3b8;
  margin-top: 2px;
}

/* ========================================================
   FOOTER HÀNH ĐỘNG CỦA CHI TIẾT (ĐẨY NÚT HỦY XUỐNG DƯỚI & LOGO NÚT TIẾP TỤC)
   ======================================================== */
.detail-bottom-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  padding-top: 8px;
  flex-wrap: wrap;
}

.bottom-actions-left {
  display: flex;
  align-items: center;
}

.bottom-actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-back-outline {
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  color: #475569;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-back-outline:hover {
  background: #f1f5f9;
  color: #0f172a;
  border-color: #94a3b8;
  transform: translateY(-1px);
}

.btn-cancel-order {
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  background: #ffffff;
  border: 1.5px solid #fecaca;
  color: #dc2626;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(220, 38, 38, 0.06);
}

.btn-cancel-order:hover {
  background: #fef2f2;
  border-color: #ef4444;
  color: #b91c1c;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);
}

.btn-cancel-order:active {
  transform: scale(0.97);
}

.btn-shop-more {
  height: 42px;
  padding: 0 22px;
  border-radius: 10px;
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  color: #ffffff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.32);
}

.btn-shop-more:hover {
  background: linear-gradient(135deg, #b45309 0%, #92400e 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(217, 119, 6, 0.42);
}

.btn-shop-more:active {
  transform: translateY(0);
}

.btn-shop-icon {
  transition: transform 0.2s ease;
}

.btn-shop-more:hover .btn-shop-icon {
  transform: translateY(-1px) scale(1.1);
}

/* Empty & Loading cards */
.empty-orders-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  border: 1px solid #eef2f6;
}

.empty-icon {
  font-size: 42px;
  margin-bottom: 12px;
}

.empty-orders-card h3 {
  font-family: 'Be Vietnam Pro', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 6px;
}

.empty-orders-card p {
  color: #64748b;
  font-size: 13px;
  margin: 0 0 18px;
}

.btn-primary-action {
  display: inline-block;
  background: #496883;
  color: #ffffff;
  text-decoration: none;
  padding: 10px 24px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 13px;
  transition: all 0.2s;
}

.btn-primary-action:hover {
  background: #364f66;
}

.loading-state-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 36px 20px;
  text-align: center;
  color: #64748b;
  font-size: 13px;
  border: 1px solid #eef2f6;
}

.spinner-blue {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Modals */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-card {
  background: #ffffff;
  width: 90%;
  max-width: 440px;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.modal-header h3 {
  font-family: 'Be Vietnam Pro', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  margin: 0;
  font-size: 17px;
  color: #496883;
  font-weight: 800;
}

.btn-close {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #64748b;
}

.review-product-head {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.review-product-head strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
}

.review-product-head p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748b;
}

.star-rating-box {
  margin-bottom: 16px;
}

.star-rating-box label {
  display: block;
  font-size: 12.5px;
  color: #334155;
  margin-bottom: 6px;
  font-weight: 600;
}

.stars-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.star-icon {
  font-size: 24px;
  color: #cbd5e1;
  cursor: pointer;
  transition: transform 0.15s, color 0.15s;
}

.star-icon.filled {
  color: #d2a764;
}

.star-icon:hover {
  transform: scale(1.15);
}

.rating-text {
  font-size: 12.5px;
  font-weight: 700;
  color: #d2a764;
  margin-left: 8px;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 12.5px;
  color: #334155;
  margin-bottom: 6px;
  font-weight: 600;
}

.form-control {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
}

.form-control:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.btn-submit-review {
  width: 100%;
  background: #496883;
  color: #ffffff;
  border: none;
  padding: 11px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit-review:hover {
  background: #364f66;
}

@media (max-width: 900px) {
  .stepper-track {
    flex-direction: column !important;
    align-items: flex-start !important;
    gap: 16px !important;
  }
  .step-line {
    display: none !important;
  }
  .step-point {
    width: 100% !important;
    flex-direction: row !important;
    gap: 12px !important;
    text-align: left !important;
  }
  .orders-layout {
    grid-template-columns: 1fr;
  }
  .unused-stepper {
  display: flex;
  align-items: center;
  justify-content: flex-start !important;
  position: relative;
}

.step-point {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 130px !important;
  flex-shrink: 0 !important;
  position: relative;
  z-index: 2;
}

.step-line {
  flex: 1;
  max-width: 140px !important;
  min-width: 50px !important;
  height: 3px;
  background: #e2e8f0;
  margin: -24px 8px 0;
  position: relative;
  z-index: 1;
}

.step-point.step-cancelled .step-icon,
.step-point.step-failed .step-icon {
  background: #ef4444 !important;
  border-color: #ef4444 !important;
  color: #ffffff !important;
  animation: none !important;
}

.step-point.step-cancelled .step-name,
.step-point.step-failed .step-name {
  color: #dc2626 !important;
}

.step-line.line-failed,
.step-line.line-cancelled {
  background: #fca5a5 !important;
}

  .unused-stepper {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .step-line {
    display: none;
  }
  .step-point {
    width: 100%;
    flex-direction: row;
    gap: 12px;
    text-align: left;
  }
  .step-icon {
    margin-bottom: 0;
  }
  .video-summary-grid {
    grid-template-columns: 1fr;
  }
  .card-foot-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .foot-actions-group {
    width: 100%;
    justify-content: flex-end;
  }
}

.tag-pos-mini {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #0f766e;
  background: #f0fdfa;
  border: 1px solid #ccfbf1;
  padding: 3px 10px;
  border-radius: 6px;
}

/* ONLINE SHIPPING DETAIL BOX (GHN) */
.online-shipping-detail-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 22px;
}
.shipping-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px dashed #cbd5e1;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}
.carrier-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}
.carrier-icon {
  font-size: 24px;
}
.carrier-title {
  font-size: 15px;
  color: #1e293b;
  font-weight: 700;
  display: block;
}
.carrier-badge {
  display: inline-block;
  background: #ff6600;
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 4px;
  letter-spacing: 0.5px;
  margin-top: 3px;
}
.tracking-code-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 5px 12px;
  border-radius: 6px;
}
.trk-label {
  font-size: 13px;
  color: #64748b;
}
.trk-code {
  font-size: 13px;
  font-weight: 700;
  color: #0284c7;
}
.shipping-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}
.shipping-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.item-label {
  font-size: 12px;
  color: #64748b;
}
.item-value {
  font-size: 14px;
  color: #1e293b;
}
.item-sub {
  font-size: 13px;
  color: #475569;
}
.text-shipping {
  color: #059669;
}


/* ======================================================== */
/* MÀN HÌNH YÊU CẦU ĐĂNG NHẬP ĐƠN HÀNG                      */
/* ======================================================== */
.auth-gate-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 16px 80px;
}

.auth-gate-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 32px 28px;
  max-width: 440px;
  width: 100%;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

.auth-gate-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.auth-gate-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.auth-gate-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 22px;
}

.auth-gate-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-auth-gate-login {
  width: 100%;
  padding: 14px 20px;
  background: #000000;
  color: #ffffff;
  border: none;
  border-radius: 9999px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
}

.btn-auth-gate-login:hover {
  background: #1e293b;
  transform: translateY(-2px);
}

.btn-auth-gate-track {
  display: inline-block;
  padding: 10px;
  font-size: 13.5px;
  font-weight: 600;
  color: #496883;
  text-decoration: none;
  transition: color 0.15s;
}

.btn-auth-gate-track:hover {
  color: #1e293b;
  text-decoration: underline;
}


/* ==================================================== */
/* ĐỒNG BỘ GIAO DIỆN VỚI CHI TIẾT HÓA ĐƠN ADMIN         */
/* ==================================================== */
.main-layout-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1024px) {
  .main-layout-grid {
    grid-template-columns: 1fr;
  }
}

.left-section,
.right-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-box {
  background: #ffffff;
  border: 1px solid #ebd9c8;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.card-header-styled {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title-styled {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  font-weight: 700;
  color: #496883;
}

.title-icon {
  color: #496883;
  flex-shrink: 0;
}

.badge-status-top {
  font-size: 0.8rem;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 16px;
  border: 1px solid transparent;
}

.badge-status-1 { background-color: #fff4e5; color: #d97706; border-color: #fde0be; }
.badge-status-2 { background-color: #e0f2fe; color: #0369a1; border-color: #bfe5fd; }
.badge-status-3 { background-color: #f5edff; color: #8a3ee6; border-color: #e6d4fc; }
.badge-status-4 { background-color: #e0f7fa; color: #00838f; border-color: #b5eff5; }
.badge-status-5 { background-color: #e6f6ec; color: #1b7a37; border-color: #c4ecd1; }
.badge-status-6 { background-color: #fee6e6; color: #be2626; border-color: #facdcd; }

/* Stepper Timeline */
.card-order-status {
  padding-bottom: 16px;
}

.stepper-track-wrap {
  position: relative;
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  padding: 16px 20px 10px;
  margin: 10px 0 20px 0;
  gap: 50px;
  overflow-x: auto;
}

.stepper-step-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-width: 110px;
}

.step-connector-line {
  position: absolute;
  top: 19px;
  right: 50%;
  width: calc(100% + 50px);
  height: 2px;
  background-color: #dbe2e8;
  z-index: 1;
}

.step-connector-line.line-active {
  background-color: #496883 !important;
}

.step-connector-line.line-cancelled {
  background-color: #ef4444 !important;
}

.step-circle-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  margin-bottom: 8px;
  background-color: #ffffff;
  transition: all 0.25s ease;
}

.circle-done {
  background-color: #496883;
  color: #ffffff;
  border: 2px solid #496883;
}

.circle-active {
  background-color: #496883;
  color: #ffffff;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 4px rgba(73, 104, 131, 0.25);
}

.circle-pending {
  background-color: #ffffff;
  color: #a8b3bc;
  border: 2px dashed #cfd8df;
}

.circle-red {
  background-color: #ef4444;
  color: #ffffff;
  border: 2px solid #ef4444;
  box-shadow: 0 0 0 4px #fee2e2;
}

.step-item-title {
  font-size: 0.83rem;
  font-weight: 600;
  margin-bottom: 2px;
  color: #8c969e;
}

.step-item-title.title-done,
.step-item-title.title-active {
  color: #496883;
  font-weight: 700;
}

.step-item-title.title-pending {
  color: #8c969e;
}

.step-item-time {
  font-size: 0.72rem;
  color: #9aa4ac;
}

.stepper-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #ebd9c8;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-history-outline {
  background: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.83rem;
  font-weight: 600;
  color: #496883;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
}

.btn-history-outline:hover {
  background: #f7f5ef;
  border-color: #496883;
  color: #243c50;
}

.btn-cancel-action {
  background: #ffffff;
  color: #dc2626;
  border: 1.5px solid #fca5a5;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.83rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-cancel-action:hover {
  background: #fee2e2;
  border-color: #dc2626;
}

/* Info Dual Grid */
.info-dual-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 640px) {
  .info-dual-grid {
    grid-template-columns: 1fr;
  }
}

.sub-info-card {
  padding: 16px 18px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.82rem;
  padding-bottom: 6px;
  border-bottom: 1px dashed #f0e6dc;
}

.info-list-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-row-label {
  color: #647074;
}

.info-row-val {
  color: #2b353b;
  text-align: right;
}

/* Summary Card */
.summary-card {
  padding: 16px 20px;
}

.summary-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
}

.s-label {
  color: #647074;
}

.s-val {
  color: #2b353b;
  font-weight: 600;
}

.summary-divider {
  height: 1px;
  background-color: #ebd9c8;
  margin: 4px 0;
}

.summary-total-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 4px;
}

.total-label-caps {
  font-size: 0.85rem;
  font-weight: 700;
  color: #2b353b;
  letter-spacing: 0.5px;
}

.total-price-blue {
  font-size: 1.25rem;
  font-weight: 800;
  color: #496883;
}

.badge-voucher-applied-small {
  font-size: 0.7rem;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 10px;
  background-color: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
  letter-spacing: 0.4px;
}

.badge-no-voucher-small {
  font-size: 0.7rem;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 10px;
  background-color: #f8fafc;
  color: #94a3b8;
  border: 1px solid #e2e8f0;
}

.summary-discount-row {
  margin-top: 2px;
}

.discount-label-with-code {
  display: flex;
  align-items: center;
  gap: 6px;
}

.inline-voucher-pill {
  font-family: inherit;
  font-weight: 700;
  font-size: 0.76rem;
  background-color: #fee2e2;
  color: #b91c1c;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #fecaca;
  display: inline-block;
  line-height: 1.3;
}

.text-discount {
  color: #dc2626 !important;
  font-weight: 700 !important;
}

/* Payment Card */
.payment-card {
  padding: 16px 20px;
}

.payment-inner-box {
  background: #fcfbf9;
  border: 1px solid #f0e6dc;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 14px;
}

.pay-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.pay-label-sub {
  font-size: 0.78rem;
  color: #647074;
}

.badge-status-pill {
  font-size: 0.74rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.pill-paid {
  background: #eaf8ed;
  color: #23783a;
  border: 1px solid #c2ebd0;
}

.pill-unpaid {
  background: #fef3c7;
  color: #d97706;
  border: 1px solid #fde68a;
}

.pill-dot {
  font-size: 0.5rem;
}

.pay-method-title {
  font-size: 0.92rem;
  font-weight: 700;
  color: #496883;
  margin-bottom: 4px;
}

.pay-time-trans {
  font-size: 0.75rem;
  color: #8c969e;
  margin-bottom: 8px;
}

.pay-amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.pay-amount-bold {
  font-size: 0.95rem;
  font-weight: 700;
  color: #496883;
}

.btn-quick-pay-lg {
  width: 100%;
  background: #2563eb;
  color: #ffffff;
  border: 1px solid #1d4ed8;
  border-radius: 8px;
  padding: 9px 16px;
  font-size: 0.86rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
}

.btn-quick-pay-lg:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

/* Products Card */
.products-card {
  padding: 20px;
  margin-bottom: 16px;
}

.product-count-sub {
  font-size: 0.82rem;
  color: #647074;
  font-weight: 600;
}

.items-table-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 10px;
}

.order-product-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 14px;
  background: #fafaf9;
  border: 1px solid #ebd9c8;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.order-product-row:hover {
  background: #f5f2ec;
  border-color: #d5c8b8;
}

.product-thumb-wrap {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  flex-shrink: 0;
}

.product-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info-col {
  flex: 1;
}

.prod-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  display: block;
  margin-bottom: 6px;
}

.prod-meta-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-tag {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
}

.meta-tag.code-tag {
  background: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
  font-weight: 700;
}

.product-price-col {
  text-align: right;
  flex-shrink: 0;
}

.price-val {
  font-size: 1rem;
  font-weight: 700;
  color: #496883;
  display: block;
}

.unit-price-sub {
  font-size: 0.78rem;
  color: #8c969e;
}

/* Modal history */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.modal-container {
  background: #ffffff;
  border-radius: 12px;
  max-width: 620px;
  width: 100%;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #64748b;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.btn-close-modal:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.logs-timeline-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.log-timeline-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.log-dot-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #496883;
  color: #ffffff;
  font-size: 0.7rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.log-detail-wrapper {
  flex: 1;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 14px;
}

.log-top-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.log-action-text {
  font-size: 0.86rem;
  color: #1e293b;
}

.log-time-text {
  font-size: 0.76rem;
  color: #64748b;
}

.log-sub-info {
  font-size: 0.78rem;
  color: #64748b;
}

</style>
