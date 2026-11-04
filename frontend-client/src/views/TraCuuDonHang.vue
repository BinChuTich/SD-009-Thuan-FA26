<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const route = useRoute()

const searchCode = ref('')
const searchPhone = ref('')
const loading = ref(false)
const order = ref(null)
const orderItems = ref([])
const orderLogs = ref([])
const errorMsg = ref('')
const showSuccessToast = ref(false)

const triggerSuccess = () => {
  showSuccessToast.value = true
  setTimeout(() => {
    showSuccessToast.value = false
  }, 3500)
}

const loadOrderDetails = async (maHoaDon) => {
  try {
    const res = await api.get(`/api/hoa-don/code/${maHoaDon}/chi-tiet`)
    orderItems.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('Lỗi tải sản phẩm của hóa đơn:', e)
    orderItems.value = []
  }
}

const loadOrderLogs = async (maHoaDon) => {
  try {
    const res = await api.get(`/api/hoa-don/code/${maHoaDon}/lich-su`)
    const logs = Array.isArray(res.data) ? res.data : []
    logs.sort((a, b) => new Date(b.thoiGian || 0) - new Date(a.thoiGian || 0))
    orderLogs.value = logs
  } catch (e) {
    console.error('Lỗi tải lịch sử tác động đơn:', e)
    orderLogs.value = []
  }
}

const handleSearch = async () => {
  errorMsg.value = ''
  order.value = null
  orderItems.value = []
  orderLogs.value = []
  showSuccessToast.value = false

  const code = (searchCode.value || '').trim()
  const phone = (searchPhone.value || '').trim()

  if (!code && !phone) {
    errorMsg.value = 'Vui lòng nhập Mã đơn hàng hoặc Số điện thoại để tra cứu!'
    return
  }

  loading.value = true
  try {
    let foundOrder = null

    // 1. Nếu có mã đơn hàng, thử tìm trực tiếp bằng code
    if (code) {
      try {
        const resCode = await api.get(`/api/hoa-don/code/${code}`)
        if (resCode.data && resCode.data.maHoaDon) {
          // Nếu người dùng nhập cả SĐT, kiểm tra xem có khớp không
          if (phone) {
            const oPhone = (resCode.data.soDienThoaiKhachHang || resCode.data.soDienThoaiHienThi || '').trim()
            if (oPhone && oPhone !== phone) {
              errorMsg.value = `Số điện thoại "${phone}" không khớp với mã đơn hàng "${code}". Vui lòng kiểm tra lại!`
              loading.value = false
              return
            }
          }
          foundOrder = resCode.data
        }
      } catch (e) {
        // Tiếp tục quét danh sách nếu get theo code 404
      }
    }

    // 2. Nếu chưa tìm thấy hoặc chỉ nhập SĐT, quét danh sách đơn hàng
    if (!foundOrder) {
      const resAll = await api.get('/api/hoa-don')
      const list = Array.isArray(resAll.data) ? resAll.data : []
      // Sắp xếp đơn mới nhất lên đầu
      list.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))

      foundOrder = list.find(h => {
        const hCode = (h.maHoaDon || '').trim().toLowerCase()
        const hPhone = (h.soDienThoaiKhachHang || h.soDienThoaiHienThi || '').trim()

        if (code && phone) {
          return hCode === code.toLowerCase() && hPhone === phone
        } else if (code) {
          return hCode === code.toLowerCase()
        } else if (phone) {
          return hPhone === phone
        }
        return false
      })
    }

    if (foundOrder) {
      order.value = foundOrder
      await Promise.all([
        loadOrderDetails(foundOrder.maHoaDon),
        loadOrderLogs(foundOrder.maHoaDon)
      ])
      triggerSuccess()
    } else {
      errorMsg.value = `Không tìm thấy đơn hàng nào với thông tin bạn cung cấp. Vui lòng kiểm tra lại!`
    }
  } catch (err) {
    console.error('Lỗi tra cứu đơn hàng:', err)
    errorMsg.value = 'Hệ thống đang bận hoặc không thể kết nối. Vui lòng thử lại sau.'
  } finally {
    loading.value = false
  }
}

// Helpers định dạng thời gian
const formatLogTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

const getDeliveryStepTime = (stepNum) => {
  if (!order.value) return ''
  const currentStatus = Number(order.value.trangThai || 1)
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
    return formatLogTime(order.value.ngayTao)
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

  if (currentStatus === stepNum && order.value.ngayCapNhat) {
    return formatLogTime(order.value.ngayCapNhat)
  }

  return ''
}

const showHistoryModal = ref(false)

const voucherDisplayName = computed(() => {
  const o = order.value
  if (!o) return ''
  return o.idPhieuGiamGia?.tenPhieuGiamGia || o.maPhieuGiamGia || o.idPhieuGiamGia?.maPhieuGiamGia || ''
})

const clientTimelineSteps = computed(() => {
  const o = order.value
  if (!o) return []
  const pureStore = isPosOrder.value
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
  const o = order.value
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

// Countdown 5 phút tự động hủy cho đơn chưa thanh toán VNPAY
const countdownSeconds = ref(0)
let timerInterval = null

const formatTimer = (sec) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`
}

const calculateRemainingSeconds = (o) => {
  if (!o || !o.ngayTao || !isVnPay(o) || o.trangThaiThanhToan === 1 || o.trangThai !== 1) {
    return 0
  }
  const createdMs = new Date(o.ngayTao).getTime()
  if (isNaN(createdMs)) return 0
  const expiryMs = createdMs + 5 * 60 * 1000
  const now = Date.now()
  return Math.max(0, Math.floor((expiryMs - now) / 1000))
}

let isCancellingExpired = false
const updateCountdown = async () => {
  if (!order.value) return
  const remaining = calculateRemainingSeconds(order.value)
  countdownSeconds.value = remaining

  if (remaining <= 0 && isVnPay(order.value) && order.value.trangThai === 1 && order.value.trangThaiThanhToan !== 1) {
    if (isCancellingExpired) return
    isCancellingExpired = true
    try {
      await api.post(`/api/hoa-don/code/${order.value.maHoaDon}/cancel-expired-vnpay`)
      order.value.trangThai = 6
      await handleSearch()
    } catch (e) {
      console.warn('Auto cancel error:', e)
    } finally {
      isCancellingExpired = false
    }
  }
}

const openPayModal = async () => {
  if (!order.value || !order.value.maHoaDon) return
  try {
    const payAmount = Math.max(1, Math.round(Number(order.value.tienSauGiamGia || order.value.tongTienSauGiam || order.value.tongTien || 0)))
    const res = await api.post('/api/vnpay/create-payment', {
      orderCode: order.value.maHoaDon,
      amount: payAmount,
      orderInfo: `Thanh toan don hang ${order.value.maHoaDon}`
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
    alert('Không thể kết nối tới cổng thanh toán VNPAY lúc này. Vui lòng thử lại sau!')
  }
}

const isVnPay = (o) => {
  if (!o) return false
  const pt = (o.phuongThucThanhToan || o.ghiChu || '').toUpperCase()
  return pt.includes('VNPAY') || Boolean(o.isVnPay)
}

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

const resolveProductImg = (it) => {
  const raw = it.anhDaiDien || it.anh || it.hinhAnh || it.idChiTietSanPham?.idSanPham?.anh || it.idChiTietSanPham?.anh
  return resolveProductImageUrl(raw)
}

const onImgError = (e) => {
  e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER
}

const isPosOrder = computed(() => {
  if (!order.value) return false
  const addr = (order.value.diaChiNhanHang || '').trim().toLowerCase()
  const hasDeliveryAddress = addr && !addr.includes('tại quầy') && !addr.includes('tai quay') && !addr.includes('tại cửa hàng') && !addr.includes('tai cua hang') && addr !== 'bán tại quầy'
  const hasShipFee = Number(order.value.phiVanChuyen || order.value.phiShip || 0) > 0
  const hasCarrier = Boolean(order.value.donViVanChuyen && order.value.donViVanChuyen !== '—' && !order.value.donViVanChuyen.toLowerCase().includes('tại quầy'))
  if (hasDeliveryAddress || hasShipFee || hasCarrier) {
    return false
  }
  return order.value.loaiDon === 1 || addr.includes('tại quầy') || addr.includes('tai quay') || addr.includes('tại cửa hàng') || !addr
})

// Tính toán tài chính
const shippingFee = computed(() => {
  if (!order.value) return 0
  if (isPosOrder.value) return 0
  return Number(order.value.phiVanChuyen || order.value.phiShip || 0)
})

const finalTotal = computed(() => {
  if (!order.value) return 0
  return Number(order.value.tienSauGiamGia || order.value.tongTien || 0)
})

const campaignDiscount = computed(() => {
  if (!orderItems.value || orderItems.value.length === 0) return 0
  let total = 0
  orderItems.value.forEach(it => {
    const originPrice = Number(it.idChiTietSanPham?.giaBan || it.giaGoc || 0)
    const salePrice = Number(it.donGia || it.giaDuocTinh || 0)
    const qty = Number(it.soLuong || 1)
    if (originPrice > salePrice && salePrice > 0) {
      total += (originPrice - salePrice) * qty
    }
  })
  return total
})

const actualSubtotal = computed(() => {
  if (!order.value) return 0
  if (orderItems.value && orderItems.value.length > 0) {
    return orderItems.value.reduce((sum, it) => sum + (it.thanhTien || ((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1))), 0)
  }
  return Number(order.value.tongTien || 0)
})

const goodsTotal = computed(() => {
  return actualSubtotal.value + campaignDiscount.value
})

const discountAmount = computed(() => {
  if (!order.value) return 0
  if (order.value.tongTienGiamGia && Number(order.value.tongTienGiamGia) > 0) {
    return Number(order.value.tongTienGiamGia)
  }
  const diff = (actualSubtotal.value + shippingFee.value) - finalTotal.value
  return diff > 0 ? diff : 0
})

watch(
  () => route.query,
  (q) => {
    if (q.ma || q.code || q.sdt || q.phone) {
      if (q.ma || q.code) searchCode.value = q.ma || q.code
      if (q.sdt || q.phone) searchPhone.value = q.sdt || q.phone
      handleSearch()
    }
  },
  { immediate: true }
)

let autoRefreshTimer = null
let isRefreshingSilently = false

const refreshTrackingSilently = async () => {
  if (!order.value || !order.value.maHoaDon || isRefreshingSilently) return
  isRefreshingSilently = true
  const code = order.value.maHoaDon
  const prevStatus = Number(order.value.trangThai)
  const prevUpdatedAt = order.value.ngayCapNhat

  try {
    const [resOrder, resLogs] = await Promise.all([
      api.get(`/api/hoa-don/code/${code}`).catch(() => null),
      api.get(`/api/hoa-don/code/${code}/lich-su`).catch(() => null)
    ])

    if (resOrder && resOrder.data && resOrder.data.maHoaDon) {
      if (order.value && order.value.maHoaDon === code) {
        order.value = resOrder.data
        if (Number(resOrder.data.trangThai) !== prevStatus || resOrder.data.ngayCapNhat !== prevUpdatedAt) {
          try {
            const resItems = await api.get(`/api/hoa-don/code/${code}/chi-tiet`)
            if (Array.isArray(resItems.data)) {
              orderItems.value = resItems.data
            }
          } catch (_) {}
        }
      }
    }

    if (resLogs && Array.isArray(resLogs.data)) {
      const logs = resLogs.data
      logs.sort((a, b) => new Date(b.thoiGian || 0) - new Date(a.thoiGian || 0))
      orderLogs.value = logs
    }
  } catch (_) {
  } finally {
    isRefreshingSilently = false
  }
}

const onVisibilityChange = () => {
  if (!document.hidden) {
    refreshTrackingSilently()
  }
}

onMounted(() => {
  autoRefreshTimer = setInterval(refreshTrackingSilently, 2500)
  timerInterval = setInterval(updateCountdown, 1000)
  window.addEventListener('focus', refreshTrackingSilently)
  document.addEventListener('visibilitychange', onVisibilityChange)
  if (route.query.ma || route.query.code || route.query.sdt || route.query.phone) {
    if (route.query.ma || route.query.code) searchCode.value = route.query.ma || route.query.code
    if (route.query.sdt || route.query.phone) searchPhone.value = route.query.sdt || route.query.phone
    handleSearch()
  }
})
onUnmounted(() => {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  if (timerInterval) clearInterval(timerInterval)
  window.removeEventListener('focus', refreshTrackingSilently)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<template>
  <div class="tracking-page-wrap">
    <!-- TOAST THÔNG BÁO TÌM THẤY ĐƠN -->
    <transition name="toast-slide">
      <div v-if="showSuccessToast" class="success-top-toast">
        <span class="toast-check-icon">✓</span>
        <div class="toast-text-box">
          <strong>Thành công</strong>
          <p>Đã tìm thấy đơn hàng #{{ order?.maHoaDon }}</p>
        </div>
      </div>
    </transition>

    <!-- TIÊU ĐỀ TRANG -->
    <div class="tracking-hero-header">
      <div class="container-client text-center">
        <h1 class="tracking-title">Tra Cứu Đơn Hàng</h1>
        <p class="tracking-subtitle">Theo dõi hành trình vận chuyển và thông tin đơn hàng nhanh chóng, chính xác</p>
      </div>
    </div>

    <div class="container-client tracking-main-container">
      <!-- THANH TÌM KIẾM 2 TRƯỜNG -->
      <div class="search-input-card">
        <form @submit.prevent="handleSearch" class="search-two-fields-form">
          <div class="form-field-col">
            <label class="field-label">
              Mã đơn hàng <span class="req">*</span>
            </label>
            <input
              v-model="searchCode"
              type="text"
              placeholder="Nhập Mã Hóa Đơn (VD: HD019)"
              class="track-input"
            />
          </div>

          <div class="form-field-col">
            <label class="field-label">
              Số điện thoại đặt hàng <span class="req">*</span>
            </label>
            <input
              v-model="searchPhone"
              type="text"
              placeholder="Nhập số điện thoại đặt hàng"
              class="track-input"
            />
          </div>

          <div class="form-btn-col">
            <button type="submit" class="btn-track-submit" :disabled="loading">
              <span class="btn-icon">🔍</span> {{ loading ? 'Đang tìm...' : 'Tìm' }}
            </button>
          </div>
        </form>

        <div v-if="errorMsg" class="track-err-box">
          ⚠️ {{ errorMsg }}
        </div>
      </div>

      <!-- KẾT QUẢ TRA CỨU CHI TIẾT (GIỐNG 100% GIAO DIỆN XEM CHI TIẾT ĐƠN HÀNG) -->
      <div v-if="order" class="tracking-result-box">
        
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
                <span class="badge-status-top" :class="'badge-status-' + order.trangThai">
                  {{ getStatusText(order.trangThai) }}
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
                      {{ order.tenKhachHang || 'Khách hàng' }}
                    </span>
                  </div>
                  <div class="info-list-row">
                    <span class="info-row-label">Số điện thoại</span>
                    <span class="info-row-val font-mono">
                      {{ order.soDienThoaiKhachHang || '—' }}
                    </span>
                  </div>
                  <div class="info-list-row">
                    <span class="info-row-label">Email</span>
                    <span class="info-row-val">
                      {{ order.emailKhachHang || order.email || '—' }}
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
                    <span class="info-row-val">{{ order.diaChiNhanHang || 'Nhận tại cửa hàng' }}</span>
                  </div>
                  <div class="info-list-row">
                    <span class="info-row-label">Đơn vị vận chuyển</span>
                    <span class="info-row-val">{{ order.donViVanChuyen || (isPosOrder ? 'Tại quầy' : 'Giao Hàng Nhanh (GHN)') }}</span>
                  </div>
                  <div class="info-list-row">
                    <span class="info-row-label">Ghi chú</span>
                    <span class="info-row-val">{{ order.ghiChu || (order.maVanDon ? 'Mã vận đơn: #' + order.maVanDon : '—') }}</span>
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
                <span v-if="discountAmount > 0" class="badge-voucher-applied-small">
                  ĐÃ DÙNG VOUCHER
                </span>
                <span v-else class="badge-no-voucher-small">
                  KHÔNG DÙNG VOUCHER
                </span>
              </div>

              <div class="summary-body">
                <div class="summary-line">
                  <span class="s-label">Tổng tiền hàng</span>
                  <span class="s-val">{{ formatCurrency(goodsTotal) }}</span>
                </div>
                <div v-if="campaignDiscount > 0" class="summary-line summary-discount-row">
                  <span class="s-label">Đợt giảm giá</span>
                  <span class="s-val text-discount">-{{ formatCurrency(campaignDiscount) }}</span>
                </div>

                <!-- Dòng Giảm giá -->
                <div class="summary-line summary-discount-row">
                  <span class="s-label discount-label-with-code">
                    <span>Giảm giá</span>
                    <span v-if="voucherDisplayName" class="inline-voucher-pill">
                      {{ voucherDisplayName }}
                    </span>
                  </span>
                  <span class="s-val" :class="{ 'text-discount': discountAmount > 0 }">
                    {{ discountAmount > 0 ? '-' : '- ' }}{{ formatCurrency(discountAmount) }}
                  </span>
                </div>

                <div v-if="shippingFee > 0" class="summary-line">
                  <span class="s-label">Phí vận chuyển</span>
                  <span class="s-val">+{{ formatCurrency(shippingFee) }}</span>
                </div>

                <div class="summary-divider"></div>

                <div class="summary-total-line">
                  <span class="total-label-caps">TỔNG TIỀN</span>
                  <span class="total-price-blue">{{ formatCurrency(finalTotal) }}</span>
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
                  <span class="badge-status-pill" :class="order.trangThai === 6 ? 'pill-paid' : (order.trangThaiThanhToan === 1 ? 'pill-paid' : 'pill-unpaid')">
                    <span class="pill-dot">●</span> {{ order.trangThai === 6 ? 'Đã hoàn tiền' : (order.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán') }}
                  </span>
                </div>

                <div class="pay-method-title">{{ order.phuongThucThanhToan || (isVnPayOrder ? 'Cổng thanh toán VNPAY' : 'Thanh toán khi nhận hàng (COD)') }}</div>
                <div class="pay-time-trans" v-if="order.ngayTao">
                  {{ formatDateTime(order.ngayTao) }}
                </div>

                <div class="pay-amount-row">
                  <span class="pay-label-sub">Số tiền</span>
                  <span class="pay-amount-bold">{{ formatCurrency(finalTotal) }}</span>
                </div>
              </div>

              <div class="payment-actions-col" v-if="isVnPayOrder && order.trangThaiThanhToan !== 1 && order.trangThai === 1">
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
            <div class="product-count-sub">{{ orderItems.length }} sản phẩm</div>
          </div>

          <div v-if="orderItems.length === 0" class="items-loading-box">
            Đang tải thông tin sản phẩm...
          </div>

          <div v-else class="items-table-list">
            <div v-for="it in orderItems" :key="it.id" class="order-product-row">
              <div class="product-thumb-wrap">
                <img
                  :src="resolveProductImg(it)"
                  @error="onImgError"
                  alt="Sản phẩm"
                  class="product-thumb"
                />
              </div>
              <div class="product-info-col">
                <span class="prod-name">{{ it.tenSanPham || it.idChiTietSanPham?.idSanPham?.tenSanPham || 'Áo thun FF' }}</span>
                <div class="prod-meta-badges">
                  <span class="meta-tag code-tag font-mono" style="background:#e0f2fe; color:#0369a1; border:1px solid #bae6fd; font-weight:700;" v-if="it.maChiTietSanPham || it.idChiTietSanPham?.maChiTietSanPham">
                    Mã: {{ it.maChiTietSanPham || it.idChiTietSanPham?.maChiTietSanPham }}
                  </span>
                  <span class="meta-tag color-tag" v-if="it.tenMauSac || it.idChiTietSanPham?.idMauSac?.tenMauSac">Màu: {{ it.tenMauSac || it.idChiTietSanPham?.idMauSac?.tenMauSac }}</span>
                  <span class="meta-tag size-tag" v-if="it.tenKichCo || it.idChiTietSanPham?.idKichCo?.tenKichCo">Size: {{ it.tenKichCo || it.idChiTietSanPham?.idKichCo?.tenKichCo }}</span>
                  <span class="meta-tag qty-tag">Số lượng: x{{ it.soLuong }}</span>
                </div>
              </div>
              <div class="product-price-col">
                <span class="price-val">{{ formatCurrency(it.thanhTien || ((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1))) }}</span>
                <span class="unit-price-sub" v-if="it.soLuong > 1">
                  {{ formatCurrency((it.thanhTien || ((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1))) / it.soLuong) }}/cái
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

      </div>
    </div>
  </div>
</template>

<style scoped>
.tracking-page-wrap {
  background-color: #f8fafc;
  min-height: 85vh;
  padding: 32px 0 60px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
  position: relative;
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

.text-center {
  text-align: center;
}

/* Hero Header */
.tracking-hero-header {
  margin-bottom: 24px;
}

.tracking-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.tracking-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* Toast */
.success-top-toast {
  position: fixed;
  top: 85px;
  right: 28px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 99999;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.28s ease;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

.toast-check-icon {
  width: 24px;
  height: 24px;
  background: #16a34a;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
}

.toast-text-box strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.toast-text-box p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748b;
}

/* Search Box Card */
.search-input-card {
  background: #ffffff;
  border: 1px solid #eef2f6;
  border-radius: 14px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
  margin-bottom: 24px;
}

.search-two-fields-form {
  display: flex;
  gap: 18px;
  align-items: flex-end;
}

.form-field-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field-col .field-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.form-field-col .req {
  color: #ef4444;
}

.track-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.track-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.form-btn-col {
  display: flex;
  align-items: flex-end;
}

.btn-track-submit {
  height: 44px;
  padding: 0 28px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
  box-shadow: 0 3px 10px rgba(73, 104, 131, 0.2);
}

.btn-track-submit:hover:not(:disabled) {
  background: #364f66;
  transform: translateY(-1px);
}

.btn-track-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.track-err-box {
  margin-top: 14px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  border-radius: 8px;
  font-size: 13.5px;
}

@media (max-width: 768px) {
  .search-two-fields-form {
    flex-direction: column;
    align-items: stretch;
  }
  .btn-track-submit {
    width: 100%;
    justify-content: center;
  }
}

/* Stepper Box - Match DonHang.vue */
.order-stepper-box {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px 28px;
  border: 1px solid #eef2f6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  margin-bottom: 22px;
}

.stepper-track {
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

.step-icon {
  width: 44px;
  height: 44px;
  background: #f1f5f9;
  border: 2px solid #cbd5e1;
  color: #94a3b8;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.step-point.completed .step-icon {
  background: #496883;
  border-color: #496883;
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.25);
}

.step-point.current .step-icon {
  background: #d2a764;
  border-color: #d2a764;
  color: #ffffff;
  animation: pulseStep 1.8s infinite;
}

@keyframes pulseStep {
  0% { box-shadow: 0 0 0 0 rgba(210, 167, 100, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(210, 167, 100, 0); }
  100% { box-shadow: 0 0 0 0 rgba(210, 167, 100, 0); }
}

.step-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #64748b;
}

.step-point.completed .step-name {
  color: #496883;
}

.step-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
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

.step-line.active {
  background: #d2a764;
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

/* Cancelled Banner */
.cancelled-banner-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #991b1b;
  margin-bottom: 22px;
}

.cancelled-banner-box .c-icon {
  font-size: 26px;
}

/* Shipping Banner (GHN) */
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

/* Double Panels Grid (Thông tin nhận hàng & Chi tiết thanh toán) */
.order-double-panels-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: stretch;
  margin-bottom: 22px;
}

.panel-box-item {
  background: #ffffff;
  border: 1px solid #eef2f6;
  border-radius: 12px;
  padding: 22px 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
}

.panel-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-box-title {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #496883;
  letter-spacing: 0.2px;
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

.field-value.text-dark {
  font-weight: 700;
  color: #0f172a;
}

.panel-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 3px 0;
}

.field-badge-status {
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
}

.field-badge-status.badge-green {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.field-badge-status.badge-orange {
  background: #fff7ed;
  color: #ea580c;
  border: 1px solid #fed7aa;
}

.calc-field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13.5px;
  color: #64748b;
}

.calc-label {
  font-size: 13px;
}

.calc-val {
  color: #1e293b;
  font-weight: 600;
}

.text-green {
  color: #059669;
}

.ship-val {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ghn-mini-logo {
  height: 16px;
  width: auto;
  object-fit: contain;
  vertical-align: middle;
}

.tag-pos-mini {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.total-calc-row {
  margin-top: auto;
  padding-top: 8px;
}

.total-label-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.total-number-big {
  font-size: 20px;
  font-weight: 800;
  color: #496883;
}

@media (max-width: 868px) {
  .order-double-panels-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .stepper-track {
    overflow-x: auto;
    padding-bottom: 8px;
  }
}

/* Info Section Box (Sản phẩm trong đơn & Lịch sử) */
.info-section-box {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #eef2f6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
  margin-bottom: 22px;
}

.mt-4 {
  margin-top: 16px;
}

.sec-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 800;
  color: #496883;
}

.items-loading-box {
  padding: 20px 0;
  text-align: center;
  color: #94a3b8;
  font-style: italic;
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
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.order-product-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.product-thumb {
  width: 58px;
  height: 58px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #e2e8f0;
}

.product-info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.prod-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
}

.prod-meta, .prod-qty {
  font-size: 11.5px;
  color: #64748b;
}

.product-price-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.price-val {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

/* History Timeline */
.order-history-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 13px;
  line-height: 1.5;
}

.history-time {
  color: #64748b;
  font-size: 12px;
  min-width: 140px;
  flex-shrink: 0;
  font-family: monospace;
}

.history-actor {
  font-weight: 700;
  font-size: 12.5px;
  flex-shrink: 0;
}

.actor-admin {
  color: #dc2626;
}

.actor-customer {
  color: #496883;
}

.history-desc {
  color: #1e293b;
  flex: 1;
}
/* Warning timer */
.timer-warning-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  color: #991b1b;
  margin-bottom: 20px;
}

.timer-icon {
  font-size: 26px;
  flex-shrink: 0;
}

.timer-info-content {
  flex: 1;
}

.timer-info-content strong {
  display: block;
  font-size: 14px;
}

.countdown-val {
  color: #dc2626;
  font-weight: 800;
  font-size: 16px;
}

.timer-info-content p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #b91c1c;
}

.btn-pay-now {
  background: #496883;
  color: #ffffff;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 3px 8px rgba(73, 104, 131, 0.25);
  flex-shrink: 0;
}

.btn-pay-now:hover {
  background: #364f66;
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
.sec-count-badge {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
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
