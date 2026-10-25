<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency, formatDateTime } from '@/utils/dinhDang.js'
import { showAppConfirm, showAppAlert } from '@/utils/thongBaoDialog.js'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const orders = ref([])
const selectedOrder = ref(null)
const selectedOrderItems = ref([])
const orderLogs = ref([])
const activeTab = ref('all') // all, pending, shipping, completed, cancelled
const searchQuery = ref('')
const orderItemsMap = ref({}) // Cache: { [maHoaDon]: itemsArray }

const resolveProductImg = (it) => {
  const raw = it.anhDaiDien || it.anh || it.hinhAnh || it.idChiTietSanPham?.idSanPham?.anh || it.idChiTietSanPham?.anh
  if (!raw) return 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=120&q=80'
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  return `http://localhost:8080${raw.startsWith('/') ? '' : '/'}${raw}`
}

const onImgError = (e) => {
  e.target.src = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=120&q=80'
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

// Modal đánh giá sản phẩm
const showReviewModal = ref(false)
const reviewingItem = ref(null)
const reviewRating = ref(5)
const reviewComment = ref('')

// Countdown 5 phút tự động hủy cho đơn chưa thanh toán VNPAY
const countdownSeconds = ref(299)
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
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
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

const currentUserName = computed(() => {
  try {
    const raw = localStorage.getItem('user')
    if (raw) {
      const u = JSON.parse(raw)
      return u.tenTaiKhoan || u.tenKhachHang || u.hoVaTen || 'thaont'
    }
    const un = localStorage.getItem('username')
    if (un) return un
  } catch (e) {}
  if (selectedOrder.value?.tenKhachHang) {
    return selectedOrder.value.tenKhachHang.toLowerCase().replace(/\s+/g, '')
  }
  return 'thaont'
})

const currentUserFullName = computed(() => {
  try {
    const raw = localStorage.getItem('user')
    if (raw) {
      const u = JSON.parse(raw)
      return u.tenKhachHang || u.hoVaTen || 'Nguyễn Thu Thảo'
    }
  } catch (e) {}
  return selectedOrder.value?.tenKhachHang || 'Nguyễn Thu Thảo'
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
  loading.value = true
  try {
    const res = await api.get('/api/hoa-don')
    const list = Array.isArray(res.data) ? res.data : []
    list.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))
    orders.value = list
    loadItemsForList(list)

    // Chỉ tự động mở chi tiết nếu có query code trong URL (?code=HDxxx hoặc ?ma=HDxxx)
    const targetCode = route.query.code || route.query.ma
    if (targetCode && list.length > 0) {
      const found = list.find(o => o.maHoaDon === targetCode)
      if (found) {
        viewOrderDetail(found)
      }
    }
  } catch (err) {
    console.error('Lỗi tải danh sách đơn hàng:', err)
  } finally {
    loading.value = false
  }
}

const viewOrderDetail = async (order) => {
  selectedOrder.value = order
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
    case 3: return { text: 'ĐANG CHUẨN BỊ', class: 'status-shipping' }
    case 4: return { text: 'ĐANG GIAO HÀNG', class: 'status-shipping' }
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

const openReview = (item) => {
  reviewingItem.value = item
  reviewRating.value = 5
  reviewComment.value = ''
  showReviewModal.value = true
}

const submitReview = () => {
  showAppAlert('Cảm ơn bạn đã gửi đánh giá 5 sao cho sản phẩm của FF T-Shirt!', 'Đánh giá sản phẩm', 'success')
  showReviewModal.value = false
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

const onWindowFocus = () => {
  if (selectedOrder.value && selectedOrder.value.trangThaiThanhToan !== 1) {
    loadOrders()
  }
}

onMounted(() => {
  loadOrders()
  timerInterval = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
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
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
  window.removeEventListener('message', onMessageReceived)
  window.removeEventListener('focus', onWindowFocus)
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
          <!-- ============================================== -->
          <!-- VIEW 1: DANH SÁCH ĐƠN HÀNG (KHI CHƯA CHỌN ĐƠN) -->
          <!-- ============================================== -->
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
                          <span class="tag-variant">Màu: <strong>{{ orderItemsMap[o.maHoaDon][0].tenMauSac || 'Tiêu chuẩn' }}</strong></span>
                          <span class="tag-variant">Size: <strong>{{ orderItemsMap[o.maHoaDon][0].tenKichCo || 'M' }}</strong></span>
                        </div>
                      </div>

                      <div class="preview-item-price">
                        {{ formatCurrency((orderItemsMap[o.maHoaDon][0].giaDuocTinh || orderItemsMap[o.maHoaDon][0].donGia || o.tongTien) * (orderItemsMap[o.maHoaDon][0].soLuong || 1)) }}
                      </div>
                    </div>

                    <div v-if="orderItemsMap[o.maHoaDon].length > 1" class="preview-more-badge">
                      <span>+ {{ orderItemsMap[o.maHoaDon].length - 1 }} sản phẩm khác trong đơn này</span>
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
                      <strong class="val-total">{{ formatCurrency(o.tienSauGiamGia || o.tongTien) }}</strong>
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

            <!-- TRÌNH THEO DÕI TIẾN ĐỘ ĐƠN HÀNG (STEPPER CỰC KỲ DỄ HIỂU) -->
            <div class="order-stepper-box" v-if="selectedOrder.trangThai !== 6">
              <div class="stepper-track">
                <!-- Step 1: Đặt hàng -->
                <div class="step-point" :class="{ completed: selectedOrder.trangThai >= 1 }">
                  <div class="step-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                      <line x1="3" y1="6" x2="21" y2="6"></line>
                      <path d="M16 10a4 4 0 0 1-8 0"></path>
                    </svg>
                  </div>
                  <span class="step-name">Đặt hàng thành công</span>
                  <span class="step-sub">{{ formatLogTime(selectedOrder.ngayTao) }}</span>
                </div>
                <div class="step-line" :class="{ active: selectedOrder.trangThai >= 2 }"></div>

                <!-- Step 2: Đã xác nhận -->
                <div class="step-point" :class="{ completed: selectedOrder.trangThai >= 2, current: selectedOrder.trangThai === 2 }">
                  <div class="step-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                      <polyline points="14 2 14 8 20 8"></polyline>
                      <polyline points="9 15 11 17 15 13"></polyline>
                    </svg>
                  </div>
                  <span class="step-name">Đã xác nhận</span>
                  <span class="step-sub">{{ selectedOrder.trangThai >= 2 ? 'Đã duyệt đơn' : 'Chờ xác nhận' }}</span>
                </div>
                <div class="step-line" :class="{ active: selectedOrder.trangThai >= 3 }"></div>

                <!-- Step 3: Đang giao hàng -->
                <div class="step-point" :class="{ completed: selectedOrder.trangThai >= 4, current: selectedOrder.trangThai === 3 || selectedOrder.trangThai === 4 }">
                  <div class="step-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="1" y="3" width="15" height="13"></rect>
                      <polygon points="16 8 20 8 23 11 23 16 16 16 8"></polygon>
                      <circle cx="5.5" cy="18.5" r="2.5"></circle>
                      <circle cx="18.5" cy="18.5" r="2.5"></circle>
                    </svg>
                  </div>
                  <span class="step-name">Đang vận chuyển</span>
                  <span class="step-sub">{{ selectedOrder.trangThai >= 4 ? 'Đang giao tận nơi' : 'Đang đóng gói' }}</span>
                </div>
                <div class="step-line" :class="{ active: selectedOrder.trangThai >= 5 }"></div>

                <!-- Step 4: Hoàn thành -->
                <div class="step-point" :class="{ completed: selectedOrder.trangThai === 5, current: selectedOrder.trangThai === 5 }">
                  <div class="step-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                      <polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                  </div>
                  <span class="step-name">Giao thành công</span>
                  <span class="step-sub">{{ selectedOrder.trangThai === 5 ? 'Đã nhận hàng' : 'Dự kiến 2-3 ngày' }}</span>
                </div>
              </div>
            </div>

            <!-- Banner đơn đã hủy -->
            <div v-else class="cancelled-banner-box">
              <span class="c-icon">🚫</span>
              <div>
                <strong>Đơn hàng đã được Hủy trên hệ thống</strong>
                <p>{{ isVnPayOrder ? 'Số tiền thanh toán qua VNPAY đã được xác nhận hoàn lại trả quý khách.' : 'Đơn hàng chưa giao và bạn không phải chi trả bất kỳ khoản phí nào.' }}</p>
              </div>
            </div>

            <!-- KHỐI THÔNG TIN THANH TOÁN & TỔNG KẾT TÀI CHÍNH 2 CỘT (MATCH VIDEO) -->
            <div class="video-summary-grid">
              <!-- Cột trái: Phương thức & Tình trạng thanh toán -->
              <div class="video-summary-col-left">
                <div class="v-line">
                  <span class="v-label">Phương thức thanh toán</span>
                  <span class="v-val bold">{{ selectedOrder.phuongThucThanhToan || (isVnPayOrder ? 'Thanh toán qua VNPAY' : 'Thanh toán khi nhận hàng (COD)') }}</span>
                </div>
                <div class="v-line mt-3">
                  <span class="v-label">Tình trạng thanh toán</span>
                  <span
                    class="v-val bold"
                    :class="selectedOrder.trangThai === 6 ? 'text-green' : (selectedOrder.trangThaiThanhToan === 1 ? 'text-green' : 'text-orange')"
                  >
                    {{ selectedOrder.trangThai === 6 ? 'Đã nhận hoàn tiền' : (selectedOrder.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán') }}
                  </span>
                </div>
              </div>

              <!-- Cột phải: Tổng tiền, giảm giá, ship, thành tiền -->
              <div class="video-summary-col-right">
                <div class="v-sum-row">
                  <span>Tổng tiền hàng</span>
                  <span>{{ formatCurrency(selectedOrder.tongTien - (selectedOrder.phiShip || selectedOrder.phiVanChuyen || 0) + (selectedOrder.tongTienGiamGia || 0)) }}</span>
                </div>
                <div v-if="selectedOrder.tongTienGiamGia > 0" class="v-sum-row text-green">
                  <span>Giảm giá [{{ selectedOrder.maPhieuGiamGia || selectedOrder.idPhieuGiamGia?.maPhieuGiamGia || 'VOUCHER' }}]</span>
                  <span>- {{ formatCurrency(selectedOrder.tongTienGiamGia) }}</span>
                </div>
                <div class="v-sum-row">
                  <span>Phí vận chuyển ({{ selectedOrder.donViVanChuyen || "GHN" }})</span>
                  <span class="ship-val">
                    <img v-if="(selectedOrder.phiShip || selectedOrder.phiVanChuyen) > 0" src="https://cdn.haitrieu.com/wp-content/uploads/2022/05/Logo-GHN-Orange.png" alt="GHN" class="ghn-mini-logo" />
                    + {{ (selectedOrder.phiShip || selectedOrder.phiVanChuyen) > 0 ? formatCurrency(selectedOrder.phiShip || selectedOrder.phiVanChuyen) : '0 ₫' }}
                  </span>
                </div>
                <div class="v-sum-row v-total-row">
                  <span>Thành tiền</span>
                  <strong class="total-big-val">{{ formatCurrency(selectedOrder.tongTien) }}</strong>
                </div>
              </div>
            </div>



            <!-- Cảnh báo đếm ngược thanh toán VNPAY (nếu chưa thanh toán) -->
            <div
              v-if="isVnPayOrder && selectedOrder.trangThaiThanhToan !== 1 && selectedOrder.trangThai === 1"
              class="timer-warning-box"
            >
              <div class="timer-icon">⏰</div>
              <div class="timer-info-content">
                <strong>Đơn hàng sẽ tự động hủy sau: <span class="countdown-val">{{ formatTimer(countdownSeconds) }}</span></strong>
                <p>Vui lòng thanh toán trực tuyến ngay để đơn hàng không bị hệ thống tự động hủy!</p>
              </div>
              <button class="btn-pay-now" @click="openPayModal">
                💳 Thanh toán ngay
              </button>
            </div>

            <!-- KHỐI THÔNG TIN NHẬN HÀNG -->
            <div class="info-section-box">
              <div class="sec-title-row">
                <h4 class="sec-title">📍 Thông tin nhận hàng</h4>
                <button class="btn-cancel-link" v-if="selectedOrder.trangThai === 1" @click="handleCancelOrder">
                  🚫 Hủy đơn hàng
                </button>
              </div>
              <div class="sec-content">
                <div class="row-text">
                  <span class="label">Khách hàng:</span>
                  <strong>{{ selectedOrder.tenKhachHang || selectedOrder.tenKhachHangHienThi || 'Khách hàng' }}</strong>
                </div>
                <div class="row-text">
                  <span class="label">SĐT:</span>
                  <span>{{ selectedOrder.soDienThoaiKhachHang || selectedOrder.soDienThoaiHienThi || '—' }}</span>
                </div>
                <div class="row-text">
                  <span class="label">Địa chỉ:</span>
                  <span>{{ selectedOrder.diaChiNhanHang || '—' }}</span>
                </div>
              </div>
            </div>

            <!-- KHỐI SẢN PHẨM TRONG ĐƠN -->
            <div class="info-section-box">
              <h4 class="sec-title">🛍️ SẢN PHẨM TRONG ĐƠN ({{ selectedOrderItems.length }})</h4>
              
              <div v-if="selectedOrderItems.length === 0" class="items-loading-box">
                Đang tải thông tin sản phẩm...
              </div>

              <div v-else class="items-table-list">
                <div v-for="it in selectedOrderItems" :key="it.id" class="order-product-row">
                  <img
                    :src="it.anh || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200&q=80'"
                    alt="Sản phẩm"
                    class="product-thumb"
                  />
                  <div class="product-info-col">
                    <span class="prod-name">{{ it.tenSanPham || 'Áo thun FF' }}</span>
                    <span class="prod-meta">Màu: {{ it.tenMauSac || 'Mặc định' }} | Size: {{ it.tenKichCo || 'M' }}</span>
                    <span class="prod-qty">Số lượng: x{{ it.soLuong }}</span>
                  </div>
                  <div class="product-price-col">
                    <span class="price-val">{{ formatCurrency(it.giaDuocTinh || it.donGia || 0) }}</span>
                    <button
                      v-if="selectedOrder.trangThai === 5"
                      class="btn-review-item"
                      @click="openReview(it)"
                    >
                      ⭐ Đánh giá
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Footer hành động của Chi tiết -->
            <div class="detail-bottom-actions">
              <button class="btn-back-outline" @click="handleBackToList">
                ← Quay lại danh sách đơn hàng
              </button>
              <router-link to="/cua-hang" class="btn-shop-more">
                🛍️ Tiếp tục mua sắm
              </router-link>
            </div>
          </div>
        </main>
      </div>
    </div>

    <!-- MODAL ĐÁNH GIÁ SẢN PHẨM -->
    <div v-if="showReviewModal" class="modal-overlay" @click.self="showReviewModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Đánh giá sản phẩm</h3>
          <button class="btn-close" @click="showReviewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="review-product-head">
            <strong>{{ reviewingItem?.tenSanPham || 'Áo thun FF' }}</strong>
            <p>Phân loại: {{ reviewingItem?.tenMauSac }} / {{ reviewingItem?.tenKichCo }}</p>
          </div>

          <div class="star-rating-box">
            <label>Chất lượng phục vụ & sản phẩm:</label>
            <div class="stars-row">
              <span
                v-for="s in 5"
                :key="s"
                class="star-icon"
                :class="{ filled: s <= reviewRating }"
                @click="reviewRating = s"
              >
                ★
              </span>
              <span class="rating-text">
                {{ reviewRating === 5 ? 'Tuyệt vời' : reviewRating === 4 ? 'Hài lòng' : 'Bình thường' }}
              </span>
            </div>
          </div>

          <div class="form-group">
            <label>Chia sẻ cảm nhận của bạn:</label>
            <textarea
              v-model="reviewComment"
              rows="3"
              placeholder="Chất vải rất mịn, thoáng mát, đường may tỉ mỉ, đóng gói đẹp..."
              class="form-control"
            ></textarea>
          </div>

          <button class="btn-submit-review" @click="submitReview">
            Gửi đánh giá
          </button>
        </div>
      </div>
    </div>
  </div>
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

.preview-more-badge {
  margin-top: 8px;
  font-size: 12px;
  color: #496883;
  font-weight: 600;
  background: #f8fafc;
  padding: 4px 10px;
  border-radius: 6px;
  display: inline-block;
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

/* STEPPER TIẾN ĐỘ ĐƠN HÀNG (CỰC KỲ TRỰC QUAN & DỄ HIỂU) */
.order-stepper-box {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px 28px;
  border: 1px solid #eef2f6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
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

.cancelled-banner-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #991b1b;
}

.cancelled-banner-box .c-icon {
  font-size: 26px;
}

.cancelled-banner-box strong {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}

.cancelled-banner-box p {
  margin: 0;
  font-size: 12.5px;
  color: #b91c1c;
}

/* 2-column Video Summary Box matching step_10456_media_0.png */
.video-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  padding: 22px 24px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.video-summary-col-left {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.v-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.v-label {
  font-size: 12.5px;
  color: #64748b;
}

.v-val {
  font-size: 14px;
  color: #1e293b;
}

.v-val.bold {
  font-weight: 700;
}

.video-summary-col-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.v-sum-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ship-val {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ghn-mini-logo {
  height: 14px;
  object-fit: contain;
}

.v-total-row {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px solid #eef2f6;
  font-size: 15px;
  color: #1e293b;
}

.total-big-val {
  font-size: 19px;
  font-weight: 800;
  color: #496883;
}

/* History Box matching step_10456_media_0.png */
.info-section-box {
  background: #ffffff;
  border-radius: 12px;
  padding: 22px;
  border: 1px solid #eef2f6;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.sec-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sec-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 800;
  color: #496883;
  letter-spacing: 0.2px;
  font-family: 'Be Vietnam Pro', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.sec-title-row .sec-title {
  margin-bottom: 0;
}

.btn-cancel-link {
  background: transparent;
  border: none;
  color: #dc2626;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-link:hover {
  text-decoration: underline;
}

.sec-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

.row-text {
  display: flex;
  gap: 8px;
}

.row-text .label {
  color: #64748b;
  min-width: 90px;
}

.text-green { color: #16a34a; }
.text-orange { color: #d97706; }

.order-history-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
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
  min-width: 120px;
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

.history-empty-text {
  font-size: 13px;
  color: #94a3b8;
  padding: 8px 0;
  font-style: italic;
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

/* Products table in detail */
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

.btn-review-item {
  background: #d2a764;
  color: #ffffff;
  border: none;
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-review-item:hover {
  background: #c99a4e;
}

.detail-bottom-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.btn-back-outline {
  background: #ffffff;
  border: 1.5px solid #496883;
  color: #496883;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back-outline:hover {
  background: #496883;
  color: #ffffff;
}

.btn-shop-more {
  background: #d2a764;
  color: #ffffff;
  text-decoration: none;
  padding: 10px 22px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-shop-more:hover {
  background: #c99a4e;
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
  .orders-layout {
    grid-template-columns: 1fr;
  }
  .stepper-track {
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
</style>
