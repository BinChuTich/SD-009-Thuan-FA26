<script setup>
import { ref, computed, onMounted, watch } from 'vue'
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

const goodsTotal = computed(() => {
  if (!order.value) return 0
  if (orderItems.value && orderItems.value.length > 0) {
    return orderItems.value.reduce((sum, it) => sum + (it.thanhTien || ((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1))), 0)
  }
  return Number(order.value.tongTien || 0)
})

const discountAmount = computed(() => {
  if (!order.value) return 0
  if (order.value.tongTienGiamGia && Number(order.value.tongTienGiamGia) > 0) {
    return Number(order.value.tongTienGiamGia)
  }
  const diff = (goodsTotal.value + shippingFee.value) - finalTotal.value
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

onMounted(() => {
  if (route.query.ma || route.query.code || route.query.sdt || route.query.phone) {
    if (route.query.ma || route.query.code) searchCode.value = route.query.ma || route.query.code
    if (route.query.sdt || route.query.phone) searchPhone.value = route.query.sdt || route.query.phone
    handleSearch()
  }
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
        
        <!-- TRƯỜNG HỢP 1: ĐƠN TẠI QUẦY -->
        <div class="order-stepper-box" v-if="isPosOrder && order.trangThai !== 6">
          <div class="stepper-track">
            <div class="step-point" :class="{ completed: order.trangThai >= 1 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
              </div>
              <span class="step-name">Tạo đơn tại quầy</span>
              <span class="step-sub">{{ getDeliveryStepTime(1) }}</span>
            </div>
            <div class="step-line" :class="{ active: order.trangThai >= 2 }"></div>

            <div class="step-point" :class="{ completed: order.trangThaiThanhToan === 1 || order.trangThai >= 2, current: order.trangThaiThanhToan !== 1 && order.trangThai < 5 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect>
                  <line x1="1" y1="10" x2="23" y2="10"></line>
                </svg>
              </div>
              <span class="step-name">Thanh toán</span>
              <span class="step-sub">{{ getDeliveryStepTime(2) }}</span>
            </div>
            <div class="step-line" :class="{ active: order.trangThai >= 5 }"></div>

            <div class="step-point" :class="{ completed: order.trangThai === 5, current: order.trangThai === 5 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
              </div>
              <span class="step-name">Hoàn tất tại quầy</span>
              <span class="step-sub">{{ getDeliveryStepTime(5) }}</span>
            </div>
          </div>
        </div>

        <!-- TRƯỜNG HỢP 2: ĐƠN GIAO HÀNG / ONLINE (5 BƯỚC CHUẨN) -->
        <div class="order-stepper-box" v-else-if="order.trangThai !== 6">
          <div class="stepper-track">
            <!-- Step 1: Chờ xác nhận -->
            <div class="step-point" :class="{ completed: order.trangThai >= 1 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <path d="M16 10a4 4 0 0 1-8 0"></path>
                </svg>
              </div>
              <span class="step-name">Chờ xác nhận</span>
              <span class="step-sub">{{ getDeliveryStepTime(1) }}</span>
            </div>
            <div class="step-line" :class="{ active: order.trangThai >= 2 }"></div>

            <!-- Step 2: Đã xác nhận -->
            <div class="step-point" :class="{ completed: order.trangThai >= 2, current: order.trangThai === 2 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                  <polyline points="9 15 11 17 15 13"></polyline>
                </svg>
              </div>
              <span class="step-name">Đã xác nhận</span>
              <span class="step-sub">{{ getDeliveryStepTime(2) }}</span>
            </div>
            <div class="step-line" :class="{ active: order.trangThai >= 3 }"></div>

            <!-- Step 3: Chờ vận chuyển -->
            <div class="step-point" :class="{ completed: order.trangThai >= 3, current: order.trangThai === 3 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                  <line x1="12" y1="22.08" x2="12" y2="12"></line>
                </svg>
              </div>
              <span class="step-name">Chờ vận chuyển</span>
              <span class="step-sub">{{ getDeliveryStepTime(3) }}</span>
            </div>
            <div class="step-line" :class="{ active: order.trangThai >= 4 }"></div>

            <!-- Step 4: Đang vận chuyển -->
            <div class="step-point" :class="{ completed: order.trangThai >= 4, current: order.trangThai === 4 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="1" y="3" width="15" height="13"></rect>
                  <polygon points="16 8 20 8 23 11 23 16 16 16 8"></polygon>
                  <circle cx="5.5" cy="18.5" r="2.5"></circle>
                  <circle cx="18.5" cy="18.5" r="2.5"></circle>
                </svg>
              </div>
              <span class="step-name">Đang vận chuyển</span>
              <span class="step-sub">{{ getDeliveryStepTime(4) }}</span>
            </div>
            <div class="step-line" :class="{ active: order.trangThai >= 5 }"></div>

            <!-- Step 5: Giao thành công -->
            <div class="step-point" :class="{ completed: order.trangThai === 5, current: order.trangThai === 5 }">
              <div class="step-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
              </div>
              <span class="step-name">Giao thành công</span>
              <span class="step-sub">{{ getDeliveryStepTime(5) }}</span>
            </div>
          </div>
        </div>

        <!-- Banner đơn đã hủy -->
        <div v-else class="cancelled-banner-box">
          <span class="c-icon">🚫</span>
          <div>
            <strong>Đơn hàng đã được Hủy trên hệ thống</strong>
            <p>{{ isVnPay(order) ? 'Số tiền thanh toán qua VNPAY đã được xác nhận hoàn lại trả quý khách.' : 'Đơn hàng chưa giao và bạn không phải chi trả bất kỳ khoản phí nào.' }}</p>
          </div>
        </div>

        <!-- THÔNG TIN VẬN CHUYỂN & GIAO HÀNG ONLINE (GHN) -->
        <div class="online-shipping-detail-box" v-if="!isPosOrder">
          <div class="shipping-box-header">
            <div class="carrier-brand">
              <span class="carrier-icon">🚚</span>
              <div>
                <strong class="carrier-title">Đơn vị vận chuyển: {{ order.donViVanChuyen || 'Giao Hàng Nhanh (GHN)' }}</strong>
                <span class="carrier-badge">GHN Express</span>
              </div>
            </div>
            <div class="tracking-code-wrap" v-if="order.maVanDon">
              <span class="trk-label">Mã vận đơn:</span>
              <span class="trk-code">#{{ order.maVanDon }}</span>
            </div>
          </div>

          <div class="shipping-info-grid">
            <div class="shipping-info-item">
              <span class="item-label">Người nhận hàng:</span>
              <strong class="item-value">{{ order.tenKhachHang || order.tenKhachHangHienThi || 'Khách hàng' }}</strong>
              <span class="item-sub" v-if="order.soDienThoaiKhachHang || order.soDienThoaiHienThi">
                📞 {{ order.soDienThoaiKhachHang || order.soDienThoaiHienThi }}
              </span>
            </div>
            <div class="shipping-info-item">
              <span class="item-label">Địa chỉ giao hàng:</span>
              <span class="item-value">{{ order.diaChiNhanHang || 'Nhận tại địa chỉ' }}</span>
            </div>
            <div class="shipping-info-item">
              <span class="item-label">Trạng thái vận chuyển:</span>
              <strong class="item-value text-shipping">
                {{ order.trangThai === 1 ? 'Chờ Shop xác nhận & đóng gói' :
                   order.trangThai === 2 ? 'Shop đã duyệt đơn - Đang chuẩn bị hàng' :
                   order.trangThai === 3 ? 'Đang bàn giao shipper GHN' :
                   order.trangThai === 4 ? 'Shipper GHN đang trên đường giao tới bạn' :
                   order.trangThai === 5 ? 'Đã giao hàng thành công' : 'Đơn đã hủy' }}
              </strong>
            </div>
          </div>
        </div>

        <!-- HAI BẢNG SONG SONG TRÊN CÙNG 1 DÒNG: THÔNG TIN GIAO NHẬN & CHI TIẾT THANH TOÁN -->
        <div class="order-double-panels-grid">
          <!-- BẢNG 1: THÔNG TIN NHẬN HÀNG -->
          <div class="panel-box-item">
            <div class="panel-box-header">
              <h4 class="panel-box-title">📍 Thông tin nhận hàng</h4>
            </div>
            <div class="panel-box-content">
              <div class="panel-field-row">
                <span class="field-label">Khách hàng:</span>
                <strong class="field-value">{{ order.tenKhachHang || order.tenKhachHangHienThi || 'Khách hàng' }}</strong>
              </div>
              <div class="panel-field-row">
                <span class="field-label">Số điện thoại:</span>
                <span class="field-value">{{ order.soDienThoaiKhachHang || order.soDienThoaiHienThi || '—' }}</span>
              </div>
              <div class="panel-field-row">
                <span class="field-label">Địa chỉ nhận:</span>
                <span class="field-value">{{ order.diaChiNhanHang || '—' }}</span>
              </div>
              <div class="panel-divider"></div>
              <div class="panel-field-row">
                <span class="field-label">Phương thức:</span>
                <strong class="field-value text-dark">{{ order.phuongThucThanhToan || (isVnPay(order) ? 'Cổng thanh toán VNPAY' : 'Tiền mặt') }}</strong>
              </div>
              <div class="panel-field-row">
                <span class="field-label">Trạng thái:</span>
                <span
                  class="field-badge-status"
                  :class="order.trangThai === 6 ? 'badge-green' : (order.trangThaiThanhToan === 1 ? 'badge-green' : 'badge-orange')"
                >
                  {{ order.trangThai === 6 ? 'Đã nhận hoàn tiền' : (order.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán') }}
                </span>
              </div>
            </div>
          </div>

          <!-- BẢNG 2: CHI TIẾT THANH TOÁN -->
          <div class="panel-box-item">
            <div class="panel-box-header">
              <h4 class="panel-box-title">💳 Chi tiết thanh toán</h4>
            </div>
            <div class="panel-box-content">
              <div class="calc-field-row">
                <span class="calc-label">Tổng tiền hàng</span>
                <span class="calc-val">{{ formatCurrency(goodsTotal) }}</span>
              </div>
              <div v-if="discountAmount > 0" class="calc-field-row text-green">
                <span class="calc-label">Giảm giá [{{ order.maPhieuGiamGia || 'VOUCHER' }}]</span>
                <span class="calc-val">- {{ formatCurrency(discountAmount) }}</span>
              </div>
              <div class="calc-field-row">
                <template v-if="!isPosOrder">
                  <span class="calc-label">Phí vận chuyển ({{ order.donViVanChuyen || "GHN" }})</span>
                  <span class="calc-val ship-val">
                    <img v-if="shippingFee > 0" src="https://cdn.haitrieu.com/wp-content/uploads/2022/05/Logo-GHN-Orange.png" alt="GHN" class="ghn-mini-logo" />
                    + {{ shippingFee > 0 ? formatCurrency(shippingFee) : '0 ₫' }}
                  </span>
                </template>
                <template v-else>
                  <span class="calc-label">Hình thức nhận</span>
                  <span class="tag-pos-mini">Mua &amp; nhận tại quầy (0 ₫)</span>
                </template>
              </div>
              <div class="panel-divider"></div>
              <div class="calc-field-row total-calc-row">
                <span class="total-label-text">Thành tiền</span>
                <strong class="total-number-big">{{ formatCurrency(finalTotal) }}</strong>
              </div>
            </div>
          </div>
        </div>

        <!-- SẢN PHẨM TRONG ĐƠN -->
        <div class="info-section-box">
          <h4 class="sec-title">🛍️ SẢN PHẨM TRONG ĐƠN ({{ orderItems.length }})</h4>
          
          <div v-if="orderItems.length === 0" class="items-loading-box">
            Đang tải thông tin sản phẩm...
          </div>

          <div v-else class="items-table-list">
            <div v-for="it in orderItems" :key="it.id" class="order-product-row">
              <img
                :src="resolveProductImg(it)"
                @error="onImgError"
                alt="Sản phẩm"
                class="product-thumb"
              />
              <div class="product-info-col">
                <span class="prod-name">{{ it.tenSanPham || it.idChiTietSanPham?.idSanPham?.tenSanPham || 'Áo thun FF' }}</span>
                <span class="prod-meta">Màu: {{ it.tenMauSac || it.idChiTietSanPham?.idMauSac?.tenMauSac || 'Mặc định' }} | Size: {{ it.tenKichCo || it.idChiTietSanPham?.idKichCo?.tenKichCo || 'M' }}</span>
                <span class="prod-qty">Số lượng: x{{ it.soLuong }}</span>
              </div>
              <div class="product-price-col">
                <span class="price-val">{{ formatCurrency(it.thanhTien || ((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1))) }}</span>
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
  justify-content: space-between;
  position: relative;
}

.step-point {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 140px;
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
  height: 3px;
  background: #e2e8f0;
  margin: -24px 8px 0;
  position: relative;
  z-index: 1;
}

.step-line.active {
  background: #d2a764;
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
</style>
