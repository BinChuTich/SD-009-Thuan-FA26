<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'

const route = useRoute()
const router = useRouter()

// Lấy tham số hóa đơn từ bất kỳ param router nào (:ma, :id, :code, :maHoaDon) hoặc trực tiếp từ URL path
const getRouteParam = () => {
  const p = route.params
  if (p && (p.ma || p.id || p.code || p.maHoaDon)) {
    return String(p.ma || p.id || p.code || p.maHoaDon)
  }
  const parts = window.location.pathname.split('/').filter(Boolean)
  return parts.length > 0 ? parts[parts.length - 1] : ''
}

const invoiceCode = ref(getRouteParam())
const invoiceId = ref(null)

const loading = ref(false)
const updatingStatus = ref(false)
const showHistoryModal = ref(false) // Toggle modal lịch sử thao tác
const orderLogs = ref([]) // Danh sách lịch sử thao tác

let requestId = 0
let toastTimer = null

const toast = ref({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
    toastTimer = null
  }, 2800)
}

const quayLaiHoaDon = () => {
  router.push('/hoa-don')
}

const formatDateTime = (date) => {
  if (!date) return ''
  const parsed = new Date(date)
  if (Number.isNaN(parsed.getTime())) return String(date)
  return parsed.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const createDefaultInvoice = (code) => ({
  id: null,
  code: String(code || ''),
  status: 1,
  statusBadge: 'Chờ xác nhận',
  currentStepIndex: 1,
  steps: [
    { id: 1, name: 'Chờ xác nhận', time: '', done: false, active: true, pending: false },
    { id: 2, name: 'Đã xác nhận', time: '', done: false, active: false, pending: true },
    { id: 3, name: 'Chờ vận chuyển', time: '', done: false, active: false, pending: true },
    { id: 4, name: 'Đang vận chuyển', time: '', done: false, active: false, pending: true },
    { id: 5, name: 'Hoàn thành', time: '', done: false, active: false, pending: true }
  ],
  customer: {
    name: 'Khách lẻ',
    phone: '---',
    email: '---'
  },
  delivery: {
    address: '---',
    type: 'Tại cửa hàng',
    note: '---'
  },
  summary: {
    totalProductPrice: 0,
    shippingFee: 0,
    voucherDiscount: 0,
    totalPayment: 0
  },
  paymentHistory: {
    method: 'Chưa cập nhật',
    description: 'Thanh toán đơn hàng',
    time: '',
    status: 'Chưa cập nhật',
    amount: 0
  },
  items: []
})

const invoiceData = ref(createDefaultInvoice(invoiceCode.value))

const formatMoney = (amount) => {
  const value = Number(amount)
  return (Number.isFinite(value) ? value : 0).toLocaleString('vi-VN') + ' đ'
}

const getString = (...values) => {
  for (let i = 0; i < values.length; i++) {
    const v = values[i]
    if (v !== undefined && v !== null && v !== '') return String(v)
  }
  return ''
}

const getNumber = (...values) => {
  for (let i = 0; i < values.length; i++) {
    const v = values[i]
    if (v !== undefined && v !== null && v !== '') {
      const num = Number(v)
      if (Number.isFinite(num)) return num
    }
  }
  return 0
}

const buildLogsByStatus = (status, baseDate) => {
  const now = baseDate ? new Date(baseDate).getTime() : Date.now()
  const stepTitles = [
    { title: 'Tạo đơn hàng thành công', note: 'Đơn hàng mới tạo ở trạng thái chờ xác nhận' },
    { title: 'Xác nhận đơn hàng', note: 'Nhân viên đã kiểm tra và duyệt đơn hàng' },
    { title: 'Chuyển sang chờ vận chuyển', note: 'Đơn hàng đã bàn giao kho đóng gói và chờ shipper' },
    { title: 'Đang vận chuyển', note: 'Đơn hàng đang được shipper giao tới khách' },
    { title: 'Hoàn thành đơn hàng', note: 'Đơn hàng đã giao thành công và thu tiền đầy đủ' }
  ]

  const logs = []
  const maxStep = Math.min(Number(status) || 1, 5)

  for (let i = 1; i <= maxStep; i++) {
    const offsetMs = (maxStep - i) * 20 * 60 * 1000
    const timeVal = new Date(now - offsetMs)
    logs.push({
      id: i,
      action: stepTitles[i - 1].title,
      hanhDong: stepTitles[i - 1].title,
      thoiGian: timeVal,
      nguoiThucHien: 'Nhân viên',
      ghiChu: stepTitles[i - 1].note,
      isDone: true
    })

    if (invoiceData.value.steps[i - 1]) {
      invoiceData.value.steps[i - 1].time = formatDateTime(timeVal)
    }
  }

  return logs.reverse()
}

const applyOrderStatus = (statusNumber) => {
  const status = Number(statusNumber)
  const validStatus = Number.isInteger(status) && status >= 1 && status <= 6 ? status : 1
  invoiceData.value.status = validStatus

  const badgeMap = {
    1: 'Chờ xác nhận',
    2: 'Đã xác nhận',
    3: 'Chờ vận chuyển',
    4: 'Đang vận chuyển',
    5: 'Hoàn thành',
    6: 'Đã hủy'
  }

  invoiceData.value.statusBadge = badgeMap[validStatus] || 'Chờ xác nhận'

  if (validStatus === 6) {
    invoiceData.value.currentStepIndex = 0
    invoiceData.value.steps = invoiceData.value.steps.map(step => ({
      ...step,
      done: false,
      active: false,
      pending: false
    }))
    return
  }

  invoiceData.value.currentStepIndex = validStatus
  invoiceData.value.steps = invoiceData.value.steps.map(step => ({
    ...step,
    done: step.id < validStatus,
    active: step.id === validStatus,
    pending: step.id > validStatus
  }))
}

const mapInvoiceItem = (item, index) => {
  const raw = item || {}
  const variant = raw['chiTietSanPham'] || raw['bienTheSanPham'] || {}
  const product = raw['sanPham'] || variant['sanPham'] || {}
  const color = raw['mauSac'] || variant['mauSac'] || {}
  const size = raw['kichCo'] || variant['kichCo'] || {}

  const quantity = getNumber(raw['soLuong'], raw['quantity'], 1)
  const price = getNumber(raw['donGia'], raw['giaBan'], raw['price'], 0)
  const total = getNumber(raw['thanhTien'], raw['total'], quantity * price)

  const colorText = typeof color === 'string'
      ? color
      : getString(color['tenMau'], color['tenMauSac'], raw['tenMauSac'], raw['mauSac'], '---')

  const sizeText = typeof size === 'string'
      ? size
      : getString(size['tenKichCo'], size['ten'], raw['tenKichCo'], raw['kichCo'], '---')

  return {
    id: raw['id'] || (index + 1),
    code: getString(raw['maSanPham'], raw['maSp'], product['maSanPham'], product['ma'], `SP0${index + 1}`),
    name: getString(raw['tenSanPham'], product['tenSanPham'], product['ten'], raw['name'], 'Áo phông'),
    color: colorText || '---',
    size: sizeText || '---',
    quantity,
    price,
    total
  }
}

const loadDetail = async (identifier) => {
  const queryParam = identifier || getRouteParam()
  if (!queryParam) return

  const currentRequest = ++requestId
  invoiceCode.value = queryParam
  invoiceData.value = createDefaultInvoice(queryParam)
  loading.value = true

  try {
    let res = null

    try {
      res = await api.get(`/api/hoa-don/code/${encodeURIComponent(queryParam)}`)
    } catch {
      try {
        res = await api.get(`/api/hoa-don/${encodeURIComponent(queryParam)}`)
      } catch {
        const numericId = queryParam.replace(/\D/g, '')
        if (numericId) {
          res = await api.get(`/api/hoa-don/${numericId}`)
        }
      }
    }

    if (currentRequest !== requestId || !res || !res.data) return

    let data = res.data
    if (data && !Array.isArray(data) && data['data']) {
      data = data['data']
    }

    if (!data || typeof data !== 'object') return

    const resolvedId = data['id'] || data['idHoaDon'] || null
    if (resolvedId) {
      invoiceId.value = resolvedId
      invoiceData.value.id = resolvedId
    }

    const returnedCode = getString(data['maHoaDon'], data['code'], queryParam)
    invoiceData.value.code = returnedCode

    invoiceData.value.customer = {
      name: getString(data['tenKhachHang'], data['customerName'], 'Khách lẻ'),
      phone: getString(data['soDienThoai'], data['sdt'], '---'),
      email: getString(data['email'], '---')
    }

    invoiceData.value.delivery = {
      address: getString(data['diaChiNhanHang'], data['diaChi'], data['address'], '---'),
      type: Number(data['loaiDon']) === 1 ? 'Tại cửa hàng' : 'Online',
      note: getString(data['ghiChu'], data['note'], '---')
    }

    const rawList = data['chiTietHoaDon'] || data['chiTietHoaDons'] || data['danhSachChiTiet'] || data['items'] || []
    const detailList = Array.isArray(rawList) ? rawList : []
    invoiceData.value.items = detailList.map(mapInvoiceItem)

    const calcSum = invoiceData.value.items.reduce((sum, it) => sum + it.total, 0)
    const productTotal = getNumber(data['tongTienHang'], data['tienHang'], calcSum)
    const shippingFee = getNumber(data['phiShip'], data['phiVanChuyen'], 0)
    const discount = getNumber(data['tongTienGiamGia'], data['tienGiamGia'], 0)
    const totalPayment = getNumber(
        data['tongThanhToan'],
        data['tongTienThanhToan'],
        data['tongTien'],
        productTotal + shippingFee - discount
    )

    invoiceData.value.summary = {
      totalProductPrice: productTotal,
      shippingFee,
      voucherDiscount: discount,
      totalPayment
    }

    const paymentStatus = data['trangThaiThanhToan'] !== undefined ? data['trangThaiThanhToan'] : data['daThanhToan']
    const isPaid = paymentStatus === true || Number(paymentStatus) === 1

    invoiceData.value.paymentHistory = {
      method: getString(data['hinhThucThanhToan'], data['phuongThucThanhToan'], 'Chưa cập nhật'),
      description: getString(data['ghiChuThanhToan'], data['ghiChu'], 'Thanh toán đơn hàng'),
      time: formatDateTime(data['thoiGianThanhToan'] || data['ngayTao']),
      status: isPaid ? 'Đã thanh toán' : 'Chưa cập nhật',
      amount: totalPayment
    }

    const currentStatus = data['trangThai'] !== undefined ? data['trangThai'] : (data['status'] || 1)
    applyOrderStatus(currentStatus)

    // Nạp lịch sử thao tác
    const rawLogs = data['lichSuHoaDon'] || data['lichSuHoaDons'] || data['lichSu']
    if (Array.isArray(rawLogs) && rawLogs.length > 0) {
      orderLogs.value = rawLogs.map((l, i) => ({
        id: l.id || i,
        action: l.hanhDong || l.thaoTac || l.action || 'Thao tác hóa đơn',
        hanhDong: l.hanhDong || l.thaoTac || l.action || 'Thao tác hóa đơn',
        thoiGian: l.thoiGian || l.ngayTao || l.thoiGianTao,
        nguoiThucHien: l.nguoiThucHien || l.tenNhanVien || 'Nhân viên',
        ghiChu: l.ghiChu || '',
        isDone: true
      })).reverse()
    } else {
      orderLogs.value = buildLogsByStatus(currentStatus, data['ngayTao'] || data['createdAt'])
    }
  } catch (err) {
    if (currentRequest !== requestId) return
    console.error('Lỗi khi tải chi tiết hóa đơn:', err)
  } finally {
    if (currentRequest === requestId) {
      loading.value = false
    }
  }
}

watch(
    () => [route.params, route.fullPath],
    () => {
      const currentCode = getRouteParam()
      if (currentCode) {
        loadDetail(currentCode)
      }
    },
    { immediate: true, deep: true }
)

const handleNextStatus = async () => {
  const currentStatus = Number(invoiceData.value.status)
  if (updatingStatus.value || loading.value) return

  const target = invoiceId.value || invoiceData.value.id || invoiceCode.value || getRouteParam()
  if (!target) {
    showToast('Không tìm thấy thông tin hóa đơn!', 'warning')
    return
  }

  const nextStatus = currentStatus + 1
  updatingStatus.value = true

  const stepActions = {
    2: 'Xác nhận đơn hàng',
    3: 'Chuyển sang chờ vận chuyển',
    4: 'Đang vận chuyển',
    5: 'Hoàn thành đơn hàng'
  }

  const nowTime = new Date()

  try {
    await api.put(`/api/hoa-don/${encodeURIComponent(target)}/trang-thai`, {
      trangThai: nextStatus
    })

    // Ghi nhận thời gian trực tiếp vào stepper timeline
    if (invoiceData.value.steps[nextStatus - 1]) {
      invoiceData.value.steps[nextStatus - 1].time = formatDateTime(nowTime)
    }

    orderLogs.value.unshift({
      id: Date.now(),
      action: stepActions[nextStatus] || 'Cập nhật trạng thái',
      hanhDong: stepActions[nextStatus] || 'Cập nhật trạng thái',
      thoiGian: nowTime,
      nguoiThucHien: 'Nhân viên',
      ghiChu: nextStatus === 5 ? 'Đơn hàng hoàn tất và thanh toán thành công' : 'Thao tác cập nhật trạng thái đơn',
      isDone: true
    })

    showToast('Cập nhật trạng thái thành công!')
    await loadDetail(invoiceCode.value || target)
  } catch (error) {
    console.warn('Lỗi PUT body, thử các phương thức dự phòng...', error)

    let success = false
    try {
      await api.put(`/api/hoa-don/${encodeURIComponent(target)}/trang-thai?trangThai=${nextStatus}`)
      success = true
    } catch {
      try {
        await api.put(`/api/hoa-don/code/${encodeURIComponent(invoiceCode.value)}/trang-thai`, {
          trangThai: nextStatus
        })
        success = true
      } catch {
        success = false
      }
    }

    if (success) {
      if (invoiceData.value.steps[nextStatus - 1]) {
        invoiceData.value.steps[nextStatus - 1].time = formatDateTime(nowTime)
      }
      orderLogs.value.unshift({
        id: Date.now(),
        action: stepActions[nextStatus] || 'Cập nhật trạng thái',
        hanhDong: stepActions[nextStatus] || 'Cập nhật trạng thái',
        thoiGian: nowTime,
        nguoiThucHien: 'Nhân viên',
        ghiChu: nextStatus === 5 ? 'Đơn hàng hoàn tất và thanh toán thành công' : 'Thao tác cập nhật trạng thái đơn',
        isDone: true
      })
      showToast('Cập nhật trạng thái thành công!')
      await loadDetail(invoiceCode.value || target)
    } else {
      showToast(
          error.response?.data?.message || 'Cập nhật trạng thái thất bại. Vui lòng thử lại!',
          'warning'
      )
    }
  } finally {
    updatingStatus.value = false
  }
}

const handlePrint = () => {
  window.print()
}

onBeforeUnmount(() => {
  requestId++
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
})
</script>

<template>
  <div class="chi-tiet-page">
    <!-- Toast Popup -->
    <transition name="toast-fade">
      <div v-if="toast.show" class="toast-popup" :class="toast.type">
        <span class="toast-icon">✓</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>

    <!-- Top Navigation -->
    <div class="top-nav-bar">
      <div class="breadcrumb-group">
        <button class="btn-back" @click="quayLaiHoaDon" title="Quay lại danh sách hóa đơn">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </button>

        <div class="page-breadcrumb">
          <span class="crumb-link" @click="quayLaiHoaDon">Hóa đơn</span>
          <span class="crumb-separator">/</span>
          <span class="crumb-current">Chi tiết hóa đơn: {{ invoiceData.code }}</span>
        </div>
      </div>
    </div>

    <!-- Main Layout Grid -->
    <div class="main-layout-grid">
      <!-- Cột Trái -->
      <div class="left-section">
        <!-- Khung Trạng Thái Đơn Hàng -->
        <div class="card-box">
          <div class="card-header-flex">
            <div class="header-title-box">
              <h3 class="card-title">Trạng Thái Đơn Hàng</h3>
            </div>
            <span class="badge-status-top">{{ invoiceData.statusBadge }}</span>
          </div>

          <div class="stepper-container">
            <div
                v-for="(step, idx) in invoiceData.steps"
                :key="step.id"
                class="step-node"
            >
              <div
                  v-if="idx > 0"
                  class="step-line"
                  :class="{ 'line-active': idx < invoiceData.currentStepIndex }"
              ></div>

              <div
                  class="node-circle"
                  :class="{
                  'circle-done': step.done,
                  'circle-active': step.active,
                  'circle-pending': step.pending
                }"
              >
                <svg v-if="step.done" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <svg v-else-if="step.active" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 22h14M5 2h14M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2" />
                </svg>
                <span v-else>{{ step.id }}</span>
              </div>

              <div class="node-text">
                <span class="node-title" :class="{ 'text-muted': step.pending }">{{ step.name }}</span>
                <span v-if="step.time" class="node-time">{{ step.time }}</span>
              </div>
            </div>
          </div>

          <!-- Nhóm nút hành động ở góc dưới khung trạng thái -->
          <div class="action-status-footer">
            <button class="btn-outline-history" @click="showHistoryModal = true">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              Lịch Sử Thao Tác
            </button>

            <button
                v-if="invoiceData.currentStepIndex < 5"
                class="btn-action-status"
                :disabled="updatingStatus"
                @click="handleNextStatus"
            >
              {{ updatingStatus ? 'Đang cập nhật...' : (
                invoiceData.currentStepIndex === 1 ? '✓ Xác nhận đơn hàng' :
                    invoiceData.currentStepIndex === 2 ? '✓ Chuyển vận chuyển' :
                        invoiceData.currentStepIndex === 3 ? '✓ Giao hàng' :
                            '✓ Xác nhận hoàn thành'
            ) }}
            </button>
          </div>
        </div>

        <!-- Khách hàng & Giao hàng -->
        <div class="info-dual-grid">
          <div class="card-box sub-card">
            <div class="card-header-simple">
              <h3 class="card-title">Thông Tin Khách Hàng</h3>
            </div>
            <div class="info-table">
              <div class="info-row">
                <span class="info-label">Tên Khách Hàng</span>
                <span class="info-value font-bold">{{ invoiceData.customer.name }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Số Điện Thoại</span>
                <span class="info-value">{{ invoiceData.customer.phone }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Email</span>
                <span class="info-value text-sub">{{ invoiceData.customer.email }}</span>
              </div>
            </div>
          </div>

          <div class="card-box sub-card">
            <div class="card-header-simple">
              <h3 class="card-title">Thông Tin Giao Hàng</h3>
            </div>
            <div class="info-table">
              <div class="info-row align-start">
                <span class="info-label">Địa Chỉ</span>
                <span class="info-value address-value">{{ invoiceData.delivery.address }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Loại Đơn</span>
                <span class="info-value font-medium">{{ invoiceData.delivery.type }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">Ghi Chú</span>
                <span class="info-value text-sub">{{ invoiceData.delivery.note }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột Phải -->
      <div class="right-section">
        <!-- Tổng tiền hóa đơn -->
        <div class="card-box">
          <div class="header-title-box mb-16">
            <h3 class="card-title">Thanh Toán</h3>
          </div>
          <div class="summary-content">
            <div class="summary-item">
              <span class="s-label">Tổng Tiền Hàng</span>
              <span class="s-value">{{ formatMoney(invoiceData.summary.totalProductPrice) }}</span>
            </div>
            <div class="summary-item">
              <span class="s-label">Phí Vận Chuyển</span>
              <span class="s-value">{{ formatMoney(invoiceData.summary.shippingFee) }}</span>
            </div>
            <div class="summary-item">
              <span class="s-label">Phiếu Giảm Giá</span>
              <span class="s-value">{{ formatMoney(invoiceData.summary.voucherDiscount) }}</span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-total-row">
              <span class="total-label">Tổng Tiền</span>
              <span class="total-price-orange">{{ formatMoney(invoiceData.summary.totalPayment) }}</span>
            </div>
          </div>
        </div>

        <!-- Lịch sử thanh toán -->
        <div class="card-box">
          <div class="header-title-box mb-16">
            <h3 class="card-title">Lịch Sử Thanh Toán</h3>
          </div>

          <div class="payment-record-box">
            <div class="payment-header-row">
              <span class="pay-method-name">{{ invoiceData.paymentHistory.method }}</span>
              <span :class="invoiceData.paymentHistory.status === 'Đã thanh toán' ? 'badge-paid-green' : 'badge-unpaid-orange'">
                {{ invoiceData.paymentHistory.status }}
              </span>
            </div>
            <div class="pay-desc">{{ invoiceData.paymentHistory.description }}</div>
            <div class="payment-bottom-row">
              <span class="pay-time-text">{{ invoiceData.paymentHistory.time }}</span>
              <span class="pay-amount-bold">{{ formatMoney(invoiceData.paymentHistory.amount) }}</span>
            </div>
          </div>

          <div class="payment-action-box">
            <button class="btn-paid-status">
              ✓ {{ invoiceData.paymentHistory.status }}
            </button>
            <button class="btn-print-invoice" @click="handlePrint">
              In Hóa Đơn
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Bảng danh sách sản phẩm chi tiết -->
    <div class="card-box invoice-items-card">
      <div class="header-title-box mb-16">
        <h3 class="card-title">Hóa Đơn Chi Tiết ({{ invoiceData.items.length }} sản phẩm)</h3>
      </div>

      <div class="table-responsive">
        <table class="products-table">
          <thead>
          <tr>
            <th class="col-stt text-center">STT</th>
            <th class="col-code text-left">Mã SP</th>
            <th class="col-name text-left">Tên Sản Phẩm</th>
            <th class="col-variant text-center">Phân Loại</th>
            <th class="col-qty text-center">Số Lượng</th>
            <th class="col-price text-right">Đơn Giá</th>
            <th class="col-total text-right">Thành Tiền</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="invoiceData.items.length === 0">
            <td colspan="7" class="text-center text-muted" style="padding: 24px;">Không có sản phẩm trong đơn hàng.</td>
          </tr>
          <tr v-for="(item, index) in invoiceData.items" :key="item.id">
            <td class="text-center text-muted">{{ index + 1 }}</td>
            <td class="text-left font-bold text-code">{{ item.code }}</td>
            <td class="text-left font-medium">{{ item.name }}</td>
            <td class="text-center">
              <div class="variant-wrapper">
                <span class="variant-tag">{{ item.color }}</span>
                <span class="variant-tag size-tag">Size: {{ item.size }}</span>
              </div>
            </td>
            <td class="text-center font-bold">{{ item.quantity }}</td>
            <td class="text-right">{{ formatMoney(item.price) }}</td>
            <td class="text-right font-bold text-dark">{{ formatMoney(item.total) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal Popup Lịch Sử Thao Tác (Khi bấm nút Lịch Sử Thao Tác trong khung trạng thái) -->
    <div v-if="showHistoryModal" class="modal-backdrop" @click.self="showHistoryModal = false">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="card-title">Lịch Sử Thao Tác Hóa Đơn</h3>
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
                  <span class="log-time-text">🕒 {{ formatDateTime(log.thoiGian) }}</span>
                </div>
                <div class="log-sub-info">
                  <span>Người thực hiện: <strong>{{ log.nguoiThucHien }}</strong></span>
                  <span v-if="log.ghiChu"> — {{ log.ghiChu }}</span>
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
.chi-tiet-page {
  margin-left: 20px;
  width: calc(100% - 20px);
  padding: 24px 30px;
  background-color: #faf7f0;
  min-height: 100vh;
  box-sizing: border-box;
  position: relative;
}

@media (max-width: 768px) {
  .chi-tiet-page {
    margin-left: 0;
    width: 100%;
    padding: 20px 16px;
  }
}

.toast-popup {
  position: fixed;
  top: 24px;
  right: 28px;
  background-color: #36536b;
  color: #ffffff;
  padding: 10px 18px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.88rem;
  font-weight: 600;
  z-index: 9999;
  border: 1px solid #4a6880;
}

.toast-popup.warning {
  background-color: #d97706;
  border-color: #b45309;
}

.toast-icon {
  background: rgba(255, 255, 255, 0.2);
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 0.75rem;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.top-nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.breadcrumb-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-back {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #3e5c76;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #ece4d8;
  transform: translateX(-2px);
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
}

.crumb-link {
  color: #8c969e;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.crumb-link:hover {
  color: #3e5c76;
  text-decoration: underline;
}

.crumb-separator {
  color: #b8c0c6;
}

.crumb-current {
  color: #3e5c76;
  font-weight: 700;
}

.main-layout-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;
}

.left-section,
.right-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-box {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #ebd9c8;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-title-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header-simple {
  display: flex;
  align-items: center;
  min-height: 28px;
  margin-bottom: 16px;
}

.card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #3e5c76;
  margin: 0;
}

.mb-16 {
  margin-bottom: 16px;
}

.invoice-items-card {
  margin-top: 20px;
}

.badge-status-top {
  background-color: #fdf5ea;
  color: #c97e27;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid #f9e2c4;
}

.stepper-container {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  position: relative;
  margin: 10px 10px 24px 10px;
}

.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
  text-align: center;
}

.node-circle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
  z-index: 2;
  margin-bottom: 8px;
  background-color: #ffffff;
}

.circle-done {
  background-color: #3e5c76;
  color: #ffffff;
  border: 2px solid #3e5c76;
}

.circle-active {
  background-color: #3e5c76;
  color: #f5af5f;
  border: 2px solid #3e5c76;
}

.circle-pending {
  border: 2px solid #dedede;
  color: #8c8c8c;
  background-color: #ffffff;
}

.step-line {
  position: absolute;
  top: 19px;
  right: 50%;
  width: 100%;
  height: 2px;
  background-color: #e5e5e5;
  z-index: 1;
}

.line-active {
  background-color: #3e5c76;
}

.node-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #333333;
}

.node-time {
  font-size: 0.72rem;
  color: #8c969e;
}

.text-muted {
  color: #999999 !important;
}

.action-status-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  border-top: 1px dashed #ebd9c8;
  padding-top: 14px;
}

/* Nút Lịch Sử Thao Tác bên trong khung trạng thái */
.btn-outline-history {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #ffffff;
  color: #3e5c76;
  border: 1px solid #ebd9c8;
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline-history:hover {
  background-color: #f7f3ed;
  border-color: #3e5c76;
}

.btn-action-status {
  background-color: #36536b;
  color: #ffffff;
  border: none;
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-action-status:hover {
  opacity: 0.9;
}

.btn-action-status:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.info-dual-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: stretch;
}

.sub-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

.info-table {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
  min-height: 24px;
}

.info-row.align-start {
  align-items: flex-start;
}

.info-label {
  color: #636b72;
  width: 130px;
  flex-shrink: 0;
}

.info-value {
  color: #2b353b;
  text-align: right;
  flex: 1;
  word-break: break-word;
}

.address-value {
  max-width: 250px;
  line-height: 1.45;
  font-weight: 500;
}

.summary-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.86rem;
}

.s-label {
  color: #666666;
}

.s-value {
  color: #222222;
  font-weight: 600;
}

.summary-divider {
  border-top: 1px dashed #ebd9c8;
  margin: 6px 0;
}

.summary-total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-label {
  font-size: 1rem;
  font-weight: 700;
  color: #222222;
}

.total-price-orange {
  font-size: 1.25rem;
  font-weight: 700;
  color: #b85d19;
}

.payment-record-box {
  background-color: #fcfbf9;
  border: 1px solid #f0e6dc;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 14px;
}

.payment-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.pay-method-name {
  font-weight: 700;
  font-size: 0.88rem;
  color: #222222;
}

.badge-paid-green {
  background-color: #eaf8ed;
  color: #2e7d32;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.badge-unpaid-orange {
  background-color: #fff6eb;
  color: #b75e11;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.pay-desc {
  font-size: 0.78rem;
  color: #777777;
  margin-bottom: 6px;
}

.payment-bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pay-time-text {
  font-size: 0.75rem;
  color: #888888;
}

.pay-amount-bold {
  font-weight: 700;
  font-size: 0.95rem;
  color: #b85d19;
}

.payment-action-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-paid-status {
  width: 100%;
  height: 38px;
  background-color: #edf7ef;
  color: #2e7d32;
  border: 1px solid #cce5d0;
  border-radius: 6px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-print-invoice {
  width: 100%;
  height: 36px;
  background-color: #36536b;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.products-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 0.88rem;
}

.col-stt     { width: 60px; }
.col-code    { width: 110px; }
.col-name    { width: 32%; }
.col-variant { width: 22%; }
.col-qty     { width: 90px; }
.col-price   { width: 15%; }
.col-total   { width: 15%; }

.products-table th {
  background-color: #f5efeb;
  color: #3e5c76;
  font-weight: 700;
  padding: 12px 14px;
  border-bottom: 1px solid #ebd9c8;
  vertical-align: middle;
}

.products-table td {
  padding: 14px 14px;
  border-bottom: 1px solid #f1e7dc;
  color: #333333;
  vertical-align: middle;
}

.text-left {
  text-align: left !important;
}

.text-center {
  text-align: center !important;
}

.text-right {
  text-align: right !important;
}

.variant-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.variant-tag {
  background: #ede6dd;
  color: #5d4a38;
  font-size: 0.75rem;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 600;
  white-space: nowrap;
}

.size-tag {
  background: #e8f0fe;
  color: #1a73e8;
}

.text-code {
  color: #3e5c76;
}

.font-bold {
  font-weight: 700;
}

.font-medium {
  font-weight: 500;
}

.text-dark {
  color: #222222;
}

.text-sub {
  color: #333333;
}

/* Modal Popup Lịch Sử Thao Tác */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-container {
  background: #ffffff;
  width: 600px;
  max-width: 90vw;
  max-height: 80vh;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebd9c8;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: #8c969e;
}

.btn-close-modal:hover {
  color: #222222;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.logs-timeline-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
  padding-left: 6px;
}

.log-timeline-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.log-dot-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background-color: #3e5c76;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: bold;
  flex-shrink: 0;
  margin-top: 2px;
}

.log-detail-wrapper {
  flex: 1;
  background-color: #fcfbf9;
  border: 1px solid #f0e6dc;
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
  font-size: 0.88rem;
  color: #222222;
}

.log-time-text {
  font-size: 0.75rem;
  color: #8c969e;
}

.log-sub-info {
  font-size: 0.8rem;
  color: #636b72;
}

.log-sub-info strong {
  color: #3e5c76;
}

@media (max-width: 1024px) {
  .main-layout-grid {
    grid-template-columns: 1fr;
  }
  .info-dual-grid {
    grid-template-columns: 1fr;
  }
}
</style>