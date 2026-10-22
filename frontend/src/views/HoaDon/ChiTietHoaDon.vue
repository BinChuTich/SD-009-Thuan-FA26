<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'
import { generateQrDataUrl, generateQrSvg } from '@/utils/qrcode.js'
import { getCurrentUser } from '@/utils/auth.js'

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
const showHistoryModal = ref(false)
const hasAutoPrinted = ref(false)
const orderLogs = ref([])

// Lấy thông tin người thực hiện thao tác đúng theo tài khoản đăng nhập hiện tại
const getLogActor = (log) => {
  const currentAuth = getCurrentUser()
  const currentAuthName = currentAuth?.tenNhanVien || currentAuth?.tenTaiKhoan || ''

  const raw = log?.nguoiThucHien || log?.tenNhanVien || ''
  const isGeneric = !raw || raw.trim() === '' || raw === 'Nhân viên bán hàng' || raw === 'Nhân viên'
  if (isGeneric) {
    if (currentAuthName) return currentAuthName
    if (invoiceData.value?.employee?.name && invoiceData.value.employee.name !== 'Nhân viên bán hàng' && invoiceData.value.employee.name !== 'Đơn Online') {
      return invoiceData.value.employee.name
    }
    return currentAuthName || 'Nhân viên bán hàng'
  }
  return raw
}

// Cập nhật trạng thái thanh toán
const showPaymentModal = ref(false)
const updatingPaymentStatus = ref(false)
const paymentForm = ref({
  trangThaiThanhToan: 1, // 1: Đã thanh toán, 0: Chưa thanh toán
  phuongThuc: 'Tiền mặt',
  maGiaoDich: '',
  ghiChu: ''
})

const openPaymentModal = () => {
  if (Number(invoiceData.value.status) === 5 || Number(invoiceData.value.status) === 6) {
    showToast('Đơn hàng đã hoàn thành hoặc đã hủy, không thể sửa trạng thái thanh toán!', 'warning')
    return
  }
  const isPaid = invoiceData.value.paymentHistory?.status === 'Đã thanh toán'
  paymentForm.value = {
    trangThaiThanhToan: isPaid ? 1 : 0,
    phuongThuc: invoiceData.value.paymentHistory?.method || (Number(invoiceData.value.delivery?.type === 'Tại quầy') ? 'Tiền mặt' : 'Chuyển khoản ngân hàng'),
    maGiaoDich: invoiceData.value.paymentHistory?.transCode || '',
    ghiChu: invoiceData.value.paymentHistory?.description || ''
  }
  showPaymentModal.value = true
}

const savePaymentStatusChange = async () => {
  const id = invoiceId.value || invoiceData.value.id
  const code = invoiceCode.value || invoiceData.value.code
  if (!id && !code) return

  updatingPaymentStatus.value = true
  try {
    const payload = {
      trangThaiThanhToan: Number(paymentForm.value.trangThaiThanhToan),
      hinhThucThanhToan: paymentForm.value.phuongThuc,
      maGiaoDich: paymentForm.value.maGiaoDich ? paymentForm.value.maGiaoDich.trim() : undefined,
      ghiChuThanhToan: paymentForm.value.ghiChu ? paymentForm.value.ghiChu.trim() : undefined
    }

    let success = false
    let lastErr = null

    // 1. Thử gọi PUT /api/hoa-don/{id}/thanh-toan
    if (id) {
      try {
        await api.put(`/api/hoa-don/${id}/thanh-toan`, payload)
        success = true
      } catch (err) {
        lastErr = err
        console.warn('Lỗi PUT /api/hoa-don/{id}/thanh-toan:', err)
      }
    }

    // 2. Thử gọi PUT /api/hoa-don/code/{code}/thanh-toan
    if (!success && code) {
      try {
        await api.put(`/api/hoa-don/code/${encodeURIComponent(code)}/thanh-toan`, payload)
        success = true
      } catch (err) {
        lastErr = err
        console.warn('Lỗi PUT /api/hoa-don/code/{code}/thanh-toan:', err)
      }
    }

    // 3. Fallback cập nhật hóa đơn chung PUT /api/hoa-don/{id}
    if (!success && id) {
      try {
        await api.put(`/api/hoa-don/${id}`, {
          trangThaiThanhToan: payload.trangThaiThanhToan,
          hinhThucThanhToan: payload.hinhThucThanhToan,
          maGiaoDich: payload.maGiaoDich,
          ghiChuThanhToan: payload.ghiChuThanhToan
        })
        success = true
      } catch (err) {
        lastErr = err
        console.warn('Lỗi fallback PUT /api/hoa-don/{id}:', err)
      }
    }

    if (success) {
      showToast(payload.trangThaiThanhToan === 1 ? 'Đã cập nhật trạng thái "Đã thanh toán" thành công!' : 'Đã cập nhật trạng thái "Chưa thanh toán" thành công!')
      showPaymentModal.value = false
      if (code) {
        await loadDetail(code)
      } else if (id) {
        await loadDetail(id)
      }
    } else {
      const msg = lastErr?.response?.data?.message || lastErr?.message || 'Cập nhật trạng thái thanh toán thất bại!'
      showToast(msg, 'warning')
    }
  } catch (e) {
    showToast('Lỗi khi cập nhật trạng thái thanh toán!', 'warning')
  } finally {
    updatingPaymentStatus.value = false
  }
}

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

const quayLaiBanHang = () => {
  router.push('/ban-hang')
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
  createTime: '',
  lastUpdated: '',
  steps: [
    { id: 1, name: 'Chờ xác nhận', time: '', done: false, active: true, pending: false },
    { id: 2, name: 'Đã xác nhận', time: '', done: false, active: false, pending: true },
    { id: 3, name: 'Chờ vận chuyển', time: '', done: false, active: false, pending: true },
    { id: 4, name: 'Đang vận chuyển', time: '', done: false, active: false, pending: true },
    { id: 5, name: 'Hoàn thành', time: '', done: false, active: false, pending: true }
  ],
  customer: {
    name: 'Khách lẻ',
    code: '—',
    phone: '—',
    email: '—'
  },
  employee: {
    name: '---',
    code: 'NV003'
  },
  delivery: {
    address: '—',
    type: 'Tại quầy',
    carrier: '—',
    receiveType: 'Nhận tại quầy',
    note: '—'
  },
  summary: {
    totalProductPrice: 0,
    shippingFee: 0,
    voucherDiscount: 0,
    voucherName: '',
    totalPayment: 0
  },
  voucher: {
    hasVoucher: false,
    code: '',
    name: '',
    type: '',
    isPercent: false,
    rawValue: 0,
    valueText: '',
    maxDiscount: 0,
    minOrderValue: 0,
    discountAmount: 0
  },
  paymentHistory: {
    method: 'Tiền mặt',
    description: '',
    time: '',
    status: 'Đã thanh toán',
    amount: 0,
    transCode: ''
  },
  items: []
})

const invoiceData = ref(createDefaultInvoice(invoiceCode.value))

// Xác định loại đơn hàng trực tuyến (Online / Giao hàng)
const isOnlineDelivery = computed(() => {
  return invoiceData.value.delivery?.type !== 'Tại quầy' && Number(invoiceData.value.delivery?.loaiDon) !== 1
})

// Đơn hàng online đang ở trạng thái Chờ xác nhận (status = 1): CHƯA IN HÓA ĐƠN
const isOnlineWaitingConfirm = computed(() => {
  return isOnlineDelivery.value && Number(invoiceData.value.status) === 1
})

// Điều kiện in hóa đơn: Đơn online chờ xác nhận là CHƯA in hóa đơn, đơn đã hủy không in hóa đơn
const canPrintInvoice = computed(() => {
  if (isOnlineWaitingConfirm.value) return false
  if (Number(invoiceData.value.status) === 6) return false
  return true
})

// =========================================================================
// MÃ QR HÓA ĐƠN (OFFLINE SVG TỨC THÌ TRÊN MẪU IN, KHÔNG PHỤ THUỘC MẠNG)
// =========================================================================
const invoiceQrDataUrl = computed(() => {
  const code = invoiceData.value?.code || 'HD'
  return generateQrDataUrl(code, { margin: 1 })
})

const downloadInvoiceQr = () => {
  const code = invoiceData.value?.code || invoiceCode.value || 'HD'
  try {
    const svgString = generateQrSvg(code, { margin: 2 })
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const img = new Image()

    const svgBlob = new Blob([svgString], { type: 'image/svg+xml;charset=utf-8' })
    const url = URL.createObjectURL(svgBlob)

    img.onload = () => {
      canvas.width = 500
      canvas.height = 500
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, 500, 500)
      ctx.drawImage(img, 0, 0, 500, 500)
      URL.revokeObjectURL(url)

      const pngUrl = canvas.toDataURL('image/png')
      const link = document.createElement('a')
      link.href = pngUrl
      link.download = `QR_${code}.png`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      showToast(`Đã tải xuống mã QR của hóa đơn ${code}!`)
    }
    img.src = url
  } catch (err) {
    console.error('Lỗi tải mã QR:', err)
    const link = document.createElement('a')
    link.href = invoiceQrDataUrl.value
    link.download = `QR_${code}.svg`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    showToast(`Đã tải xuống mã QR của hóa đơn ${code}!`)
  }
}

const getProductQrUrl = (item) => {
  const code = item?.variantCode || item?.code || `SP-${item?.id || '01'}`
  return generateQrDataUrl(code, { margin: 1 })
}

const formatMoney = (amount) => {
  const value = Number(amount)
  return (Number.isFinite(value) ? value : 0).toLocaleString('vi-VN') + ' đ'
}

// Tính toán chuỗi phần trăm giảm giá của phiếu giảm giá
const discountPercentText = computed(() => {
  if (invoiceData.value.voucher?.isPercent && invoiceData.value.voucher?.rawValue > 0) {
    return `${invoiceData.value.voucher.rawValue}%`
  }
  if (invoiceData.value.voucher?.valueText && invoiceData.value.voucher.valueText.includes('%')) {
    const matched = invoiceData.value.voucher.valueText.match(/\d+(\.\d+)?%/)
    if (matched) return matched[0]
  }
  const discount = Number(invoiceData.value.summary?.voucherDiscount) || 0
  const productTotal = Number(invoiceData.value.summary?.totalProductPrice) || 0
  if (discount > 0 && productTotal > 0) {
    const pct = Math.round((discount / productTotal) * 100)
    if (pct > 0) return `${pct}%`
  }
  return ''
})

// Tên hiển thị của phiếu giảm giá
const voucherDisplayName = computed(() => {
  const v = invoiceData.value.voucher
  if (!v) return ''
  const name = v.name || invoiceData.value.summary?.voucherName || ''
  if (!name || name === '—' || name === 'Không sử dụng' || name === 'Phiếu giảm giá') {
    return ''
  }
  return name
})

// Danh sách chi tiết các khoản thanh toán (dành cho thanh toán kết hợp trên màn hình & mẫu in)
const printSubTransactions = computed(() => {
  const trans = invoiceData.value?.paymentHistory?.transactions
  const curMethod = String(invoiceData.value?.paymentHistory?.method || '')
  if (curMethod.toLowerCase().includes('kết hợp') && Array.isArray(trans) && trans.length > 1) {
    // Sắp xếp chuyển khoản lên trước, tiền mặt phía sau giống ảnh mẫu
    return [...trans]
        .sort((a, b) => {
          const isAck = a.method?.toLowerCase().includes('chuyển khoản') || a.method?.toLowerCase().includes('ngân hàng')
          const isBck = b.method?.toLowerCase().includes('chuyển khoản') || b.method?.toLowerCase().includes('ngân hàng')
          if (isAck && !isBck) return -1
          if (!isAck && isBck) return 1
          return 0
        })
        .map(t => {
          let label = t.method || 'Thanh toán'
          if (label.toLowerCase().includes('chuyển khoản') || label.toLowerCase().includes('ngân hàng')) {
            label = 'Chuyển khoản'
          } else if (label.toLowerCase().includes('tiền mặt')) {
            label = 'Tiền mặt'
          }
          return {
            id: t.id,
            label,
            amount: t.amount
          }
        })
  }

  // Fallback: nếu phương thức là Kết hợp mà danh sách chưa kịp load, trích xuất từ ghi chú
  const method = String(invoiceData.value?.paymentHistory?.method || '')
  const note = String(invoiceData.value?.ghiChu || invoiceData.value?.note || invoiceData.value?.paymentHistory?.description || '')
  if (method.toLowerCase().includes('kết hợp') || note.toLowerCase().includes('thanh toán kết hợp')) {
    const list = []
    const transferMatch = note.match(/Chuyển khoản\s*[:\s]?\s*([0-9.,]+)/i)
    const cashMatch = note.match(/Tiền mặt\s*[:\s]?\s*([0-9.,]+)/i)
    if (transferMatch) {
      const val = Number(transferMatch[1].replace(/[^0-9]/g, ''))
      if (val > 0) list.push({ id: 'ck', label: 'Chuyển khoản', amount: val })
    }
    if (cashMatch) {
      const val = Number(cashMatch[1].replace(/[^0-9]/g, ''))
      if (val > 0) list.push({ id: 'cash', label: 'Tiền mặt', amount: val })
    }
    if (list.length > 0) return list
  }

  return []
})

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

const buildLogsByStatus = (status, baseDate, isAtStore = false, options = {}) => {
  const statusNum = Number(status) || 1
  const currentAuth = getCurrentUser()
  const authStaffName = currentAuth?.tenNhanVien || currentAuth?.tenTaiKhoan || ''
  const employeeName = (authStaffName && authStaffName !== 'Nhân viên bán hàng') ? authStaffName : (options.employeeName && options.employeeName !== '---' && options.employeeName !== 'Đơn Online')
      ? options.employeeName
      : (invoiceData.value.employee?.name && invoiceData.value.employee?.name !== '---' && invoiceData.value.employee?.name !== 'Đơn Online' ? invoiceData.value.employee.name : 'Nhân viên')
  const baseTimeMs = baseDate ? new Date(baseDate).getTime() : Date.now()
  const baseTimeVal = new Date(baseTimeMs)

  if (isAtStore) {
    const logs = []
    if (statusNum === 6) {
      let cancelTime = options.finishDate ? new Date(options.finishDate) : null
      if (!cancelTime || Number.isNaN(cancelTime.getTime())) {
        cancelTime = new Date(baseTimeMs)
      }
      logs.push({
        id: 2,
        action: 'Hủy đơn hàng',
        hanhDong: 'Hủy đơn hàng',
        thoiGian: cancelTime,
        nguoiThucHien: employeeName,
        ghiChu: options.note || 'Hóa đơn tại quầy đã bị hủy bỏ',
        isDone: true
      })
      logs.push({
        id: 1,
        action: 'Tạo hóa đơn tại quầy',
        hanhDong: 'Tạo hóa đơn tại quầy',
        thoiGian: baseTimeVal,
        nguoiThucHien: employeeName,
        ghiChu: 'Hóa đơn được tạo tại quầy thu ngân',
        isDone: true
      })
      return logs
    }

    if (statusNum === 5) {
      let finishTime = options.finishDate ? new Date(options.finishDate) : null
      let createTime = baseTimeVal
      if (finishTime && !Number.isNaN(finishTime.getTime()) && finishTime.getTime() > baseTimeMs) {
        createTime = baseTimeVal
      } else {
        finishTime = new Date(baseTimeMs)
        createTime = new Date(baseTimeMs - 3 * 60 * 1000)
      }

      logs.push({
        id: 2,
        action: 'Hoàn thành đơn hàng',
        hanhDong: 'Hoàn thành đơn hàng',
        thoiGian: finishTime,
        nguoiThucHien: employeeName,
        ghiChu: 'Khách hàng đã thanh toán và nhận hàng tại quầy thành công',
        isDone: true
      })
      logs.push({
        id: 1,
        action: 'Tạo hóa đơn tại quầy',
        hanhDong: 'Tạo hóa đơn tại quầy',
        thoiGian: createTime,
        nguoiThucHien: employeeName,
        ghiChu: 'Hóa đơn được tạo tại quầy thu ngân',
        isDone: true
      })
      return logs
    }

    if (statusNum >= 2 && statusNum <= 4) {
      let updateTime = options.finishDate ? new Date(options.finishDate) : new Date(baseTimeMs)
      logs.push({
        id: 2,
        action: 'Cập nhật trạng thái hóa đơn',
        hanhDong: 'Cập nhật trạng thái hóa đơn',
        thoiGian: updateTime,
        nguoiThucHien: employeeName,
        ghiChu: options.note || 'Đang xử lý đơn hàng tại quầy',
        isDone: true
      })
      logs.push({
        id: 1,
        action: 'Tạo hóa đơn tại quầy',
        hanhDong: 'Tạo hóa đơn tại quầy',
        thoiGian: baseTimeVal,
        nguoiThucHien: employeeName,
        ghiChu: 'Hóa đơn được tạo tại quầy thu ngân',
        isDone: true
      })
      return logs
    }

    logs.push({
      id: 1,
      action: 'Tạo hóa đơn tại quầy',
      hanhDong: 'Tạo hóa đơn tại quầy',
      thoiGian: baseTimeVal,
      nguoiThucHien: employeeName,
      ghiChu: 'Hóa đơn được tạo tại quầy thu ngân, đang chờ thanh toán',
      isDone: true
    })
    return logs
  }

  const stepTitles = [
    { title: 'Tạo đơn hàng thành công', note: 'Đơn hàng mới tạo ở trạng thái chờ xác nhận' },
    { title: 'Xác nhận đơn hàng', note: 'Nhân viên đã kiểm tra và duyệt đơn hàng' },
    { title: 'Chuyển sang chờ vận chuyển', note: 'Đơn hàng đã bàn giao kho đóng gói và chờ shipper' },
    { title: 'Đang vận chuyển', note: 'Đơn hàng đang được shipper giao tới khách' },
    { title: 'Hoàn thành đơn hàng', note: 'Đơn hàng đã giao thành công và thu tiền đầy đủ' }
  ]

  if (statusNum === 7) {
    const logs = []
    const failedTime = options.finishDate ? new Date(options.finishDate) : new Date(baseTimeMs)
    logs.push({
      id: 7,
      action: 'Giao hàng thất bại',
      hanhDong: 'Giao hàng thất bại',
      thoiGian: failedTime,
      nguoiThucHien: employeeName,
      ghiChu: options.note || 'Giao hàng không thành công, chờ xử lý giao lại',
      isDone: true
    })
    for (let i = 4; i >= 1; i--) {
      const offsetMs = (5 - i) * 20 * 60 * 1000
      const timeVal = new Date(baseTimeMs - offsetMs)
      logs.push({
        id: i,
        action: stepTitles[i - 1].title,
        hanhDong: stepTitles[i - 1].title,
        thoiGian: timeVal,
        nguoiThucHien: employeeName,
        ghiChu: stepTitles[i - 1].note,
        isDone: true
      })
      if (invoiceData.value.steps[i - 1]) {
        invoiceData.value.steps[i - 1].time = formatDateTime(timeVal)
      }
    }
    return logs
  }

  const logs = []
  const maxStep = Math.min(statusNum, 5)

  for (let i = 1; i <= maxStep; i++) {
    const offsetMs = (maxStep - i) * 20 * 60 * 1000
    const timeVal = new Date(baseTimeMs - offsetMs)
    logs.push({
      id: i,
      action: stepTitles[i - 1].title,
      hanhDong: stepTitles[i - 1].title,
      thoiGian: timeVal,
      nguoiThucHien: employeeName,
      ghiChu: stepTitles[i - 1].note,
      isDone: true
    })

    if (invoiceData.value.steps[i - 1]) {
      invoiceData.value.steps[i - 1].time = formatDateTime(timeVal)
    }
  }

  return logs.reverse()
}

const applyOrderStatus = (statusNumber, isAtStore = false) => {
  const status = Number(statusNumber)
  const validStatus = Number.isInteger(status) && status >= 1 && status <= 7 ? status : 1
  invoiceData.value.status = validStatus

  const badgeMap = {
    1: isAtStore ? 'Hóa đơn chờ' : 'Chờ xác nhận',
    2: 'Đã xác nhận',
    3: 'Chờ vận chuyển',
    4: 'Đang vận chuyển',
    5: 'Hoàn thành',
    6: 'Đã hủy',
    7: 'Giao hàng thất bại'
  }

  invoiceData.value.statusBadge = badgeMap[validStatus] || (isAtStore ? 'Hóa đơn chờ' : 'Chờ xác nhận')

  if (validStatus === 7) {
    invoiceData.value.currentStepIndex = 7
    invoiceData.value.steps = invoiceData.value.steps.map(step => ({
      ...step,
      done: step.id <= 4,
      active: false,
      pending: false
    }))
    return
  }

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

  if (validStatus === 5) {
    invoiceData.value.currentStepIndex = 5
    invoiceData.value.steps = invoiceData.value.steps.map(step => ({
      ...step,
      done: true,
      active: step.id === 5,
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

// Danh mục chuẩn tra cứu
const DB_CTSP_CATALOG = {
  1: { code: 'SP001', variantCode: 'CTSP01', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Đen', hex: '#000000', size: 'S', price: 199000 },
  2: { code: 'SP001', variantCode: 'CTSP02', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Đen', hex: '#000000', size: 'M', price: 199000 },
  3: { code: 'SP001', variantCode: 'CTSP03', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Trắng', hex: '#FFFFFF', size: 'M', price: 199000 },
  4: { code: 'SP002', variantCode: 'CTSP04', name: 'Áo Polo Nam Cổ Bẻ Cao Cấp', color: 'Đen', hex: '#000000', size: 'M', price: 299000 },
  5: { code: 'SP002', variantCode: 'CTSP05', name: 'Áo Polo Nam Cổ Bẻ Cao Cấp', color: 'Xanh navy', hex: '#000080', size: 'L', price: 299000 },
  6: { code: 'SP003', variantCode: 'CTSP06', name: 'Áo Phông Unisex In Hình Graphic', color: 'Trắng', hex: '#FFFFFF', size: 'L', price: 250000 },
  'CTSP01': { code: 'SP001', variantCode: 'CTSP01', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Đen', hex: '#000000', size: 'S', price: 199000 },
  'CTSP02': { code: 'SP001', variantCode: 'CTSP02', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Đen', hex: '#000000', size: 'M', price: 199000 },
  'CTSP03': { code: 'SP001', variantCode: 'CTSP03', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Trắng', hex: '#FFFFFF', size: 'M', price: 199000 },
  'CTSP04': { code: 'SP002', variantCode: 'CTSP04', name: 'Áo Polo Nam Cổ Bẻ Cao Cấp', color: 'Đen', hex: '#000000', size: 'M', price: 299000 },
  'CTSP05': { code: 'SP002', variantCode: 'CTSP05', name: 'Áo Polo Nam Cổ Bẻ Cao Cấp', color: 'Xanh navy', hex: '#000080', size: 'L', price: 299000 },
  'CTSP06': { code: 'SP003', variantCode: 'CTSP06', name: 'Áo Phông Unisex In Hình Graphic', color: 'Trắng', hex: '#FFFFFF', size: 'L', price: 250000 },
  'CTHD01': { code: 'SP001', variantCode: 'CTSP01', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Đen', hex: '#000000', size: 'S', price: 199000 },
  'CTHD02': { code: 'SP001', variantCode: 'CTSP02', name: 'Áo Phông Nam Basic Cotton Cổ Tròn', color: 'Đen', hex: '#000000', size: 'M', price: 199000 },
  'CTHD03': { code: 'SP002', variantCode: 'CTSP04', name: 'Áo Polo Nam Cổ Bẻ Cao Cấp', color: 'Đen', hex: '#000000', size: 'M', price: 299000 }
}

const DB_EMPLOYEE_CATALOG = {
  1: { code: 'NV001', name: 'Nguyễn Văn Admin', role: 'Quản trị viên' },
  2: { code: 'NV002', name: 'Trần Thị Quản Lý', role: 'Quản lý' },
  3: { code: 'NV003', name: 'Lê Văn Bán Hàng', role: 'Nhân viên bán hàng' }
}

const DB_VOUCHER_CATALOG = {
  1: { code: 'PGG01', name: 'Voucher Giảm 50k', discount: 50000 },
  2: { code: 'PGG02', name: 'Voucher Giảm 15% cho khách mới', discountPercent: 15 },
  'PGG01': { code: 'PGG01', name: 'Voucher Giảm 50k', discount: 50000 },
  'PGG02': { code: 'PGG02', name: 'Voucher Giảm 15% cho khách mới', discountPercent: 15 }
}

const DB_CUSTOMER_CATALOG = {
  1: { code: 'KH001', name: 'Nguyễn Văn An', phone: '0911111111', email: 'an@gmail.com' },
  2: { code: 'KH002', name: 'Trần Thị Bình', phone: '0922222222', email: 'binh@gmail.com' },
  3: { code: 'KH003', name: 'Lê Văn Cường', phone: '0933333333', email: 'cuong@gmail.com' }
}

const mapInvoiceItem = (item, index) => {
  const raw = item || {}
  const variant = raw['idChiTietSanPham'] || raw['chiTietSanPham'] || raw['bienTheSanPham'] || {}
  const product = variant['idSanPham'] || variant['sanPham'] || raw['idSanPham'] || raw['sanPham'] || {}
  const color = variant['idMauSac'] || variant['mauSac'] || raw['idMauSac'] || raw['mauSac'] || {}
  const size = variant['idKichCo'] || variant['kichCo'] || raw['idKichCo'] || raw['kichCo'] || {}

  const variantCode = getString(variant['maChiTietSanPham'], raw['maChiTietSanPham'], raw['maHoaDonChiTiet'], '')
  const variantId = variant['id'] || raw['idChiTietSanPham']
  const matchedCatalog = DB_CTSP_CATALOG[variantCode] || DB_CTSP_CATALOG[variantId] || DB_CTSP_CATALOG[raw['maHoaDonChiTiet']] || {}

  const quantity = getNumber(raw['soLuong'], raw['quantity'], 1)
  const price = getNumber(raw['donGia'], raw['giaBan'], variant['giaBan'], matchedCatalog['price'], raw['price'], 0)
  const total = getNumber(raw['thanhTien'], raw['total'], quantity * price)

  const colorText = typeof color === 'string'
      ? color
      : getString(color['tenMauSac'], variant['tenMauSac'], raw['tenMauSac'], color['tenMau'], matchedCatalog['color'], '---')

  const colorHex = getString(color['maHex'], matchedCatalog['hex'], '#2b353b')

  const sizeText = typeof size === 'string'
      ? size
      : getString(size['tenKichCo'], variant['tenKichCo'], raw['tenKichCo'], size['ten'], matchedCatalog['size'], '---')

  const productName = getString(product['tenSanPham'], variant['tenSanPham'], raw['tenSanPham'], matchedCatalog['name'], product['ten'], raw['name'], 'Áo Phông Nam Basic Cotton')
  const productCode = getString(product['maSanPham'], variant['maSanPham'], raw['maSanPham'], matchedCatalog['code'], variantCode, `SP00${index + 1}`)

  return {
    id: raw['id'] || (index + 1),
    code: productCode,
    variantCode: variantCode || matchedCatalog['variantCode'] || `CTSP0${index + 1}`,
    name: productName,
    color: colorText,
    colorHex,
    size: sizeText,
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

    // Khách hàng
    const khachHangObj = (data['idKhachHang'] && typeof data['idKhachHang'] === 'object') ? data['idKhachHang'] : (data['khachHang'] && typeof data['khachHang'] === 'object' ? data['khachHang'] : {})
    const rawCustomerName = getString(data['tenKhachHang'], data['tenKhachHangHienThi'], khachHangObj['tenKhachHang'], data['customerName'], khachHangObj['taiKhoan'], 'Khách lẻ')
    const customerCatalog = (rawCustomerName === 'Khách lẻ' && data['idKhachHang'] && typeof data['idKhachHang'] !== 'object') ? (DB_CUSTOMER_CATALOG[data['idKhachHang']] || {}) : {}

    invoiceData.value.customer = {
      name: rawCustomerName !== 'Khách lẻ' ? rawCustomerName : (customerCatalog['name'] || 'Khách lẻ'),
      phone: getString(data['soDienThoaiKhachHang'], data['soDienThoaiHienThi'], khachHangObj['soDienThoai'], data['soDienThoai'], customerCatalog['phone'], data['sdt'], '---'),
      email: getString(khachHangObj['email'], data['email'], customerCatalog['email'], '---')
    }

    // Nhân viên
    const nhanVienObj = data['idNhanVien'] || data['nhanVien'] || {}
    const employeeCatalog = DB_EMPLOYEE_CATALOG[data['idNhanVien']] || DB_EMPLOYEE_CATALOG[data['nguoiTao']] || {}
    const currentAuth = getCurrentUser()
    const authStaffName = currentAuth?.tenNhanVien || currentAuth?.tenTaiKhoan || ''

    let employeeName = nhanVienObj['tenNhanVien']
      || (data['nguoiTao'] && !data['nguoiTao'].includes('Nhân viên bán hàng') ? data['nguoiTao'] : null)
      || employeeCatalog['name']
      || authStaffName
      || (Number(data['loaiDon']) === 1 ? (authStaffName || 'Nhân viên bán hàng') : 'Đơn Online')
    let employeeCode = nhanVienObj['maNhanVien'] || employeeCatalog['code'] || '---'

    invoiceData.value.employee = {
      name: employeeName,
      code: employeeCode
    }

    // Phiếu giảm giá
    const rawPgg = data['idPhieuGiamGia'] || data['phieuGiamGia']
    let pggObj = {}
    let pggId = null

    if (rawPgg && typeof rawPgg === 'object') {
      pggObj = rawPgg
      pggId = pggObj.id || null
    } else if (rawPgg != null) {
      pggId = rawPgg
    }

    const voucherCatalog = DB_VOUCHER_CATALOG[pggId] || DB_VOUCHER_CATALOG[pggObj['maPhieuGiamGia']] || DB_VOUCHER_CATALOG[data['maPhieuGiamGia']] || {}
    let voucherCode = getString(pggObj['maPhieuGiamGia'], data['maPhieuGiamGia'], voucherCatalog['code'], (pggId === 1 || pggId === '1' ? 'PGG01' : (pggId === 2 || pggId === '2' ? 'PGG02' : '')))
    let voucherName = getString(pggObj['tenPhieuGiamGia'], data['tenPhieuGiamGia'], voucherCatalog['name'])
    if (!voucherName) {
      if (voucherCode === 'PGG01' || pggId === 1 || pggId === '1') voucherName = 'Voucher Giảm 50k'
      else if (voucherCode === 'PGG02' || pggId === 2 || pggId === '2') voucherName = 'Voucher Giảm 15% cho khách mới'
      else voucherName = voucherCode
    }
    let loaiPgg = pggObj['loaiPhieuGiamGia'] !== undefined ? Number(pggObj['loaiPhieuGiamGia']) : (voucherCatalog['discountPercent'] ? 1 : 0)
    let isPercent = loaiPgg === 1 || (pggObj.loaiPhieuGiamGia == null && Number(pggObj.giaTriGiamGia || 0) > 0 && Number(pggObj.giaTriGiamGia || 0) <= 100)
    let rawVal = Number(pggObj['giaTriGiamGia'] || voucherCatalog['discountPercent'] || voucherCatalog['discount'] || 0)
    let maxDiscountVal = getNumber(pggObj['giamToiDa'], voucherCatalog['maxDiscount'], 0)
    let minOrderVal = getNumber(pggObj['hoaDonToiThieu'], voucherCatalog['minOrder'], 0)

    if (pggId && (!voucherCode || !voucherName || voucherName === voucherCode || !rawVal)) {
      try {
        const pggRes = await api.get(`/api/phieu-giam-gia/${pggId}`)
        if (pggRes && pggRes.data) {
          const fetchedPgg = pggRes.data
          voucherCode = voucherCode || fetchedPgg.maPhieuGiamGia
          voucherName = fetchedPgg.tenPhieuGiamGia || voucherName || voucherCode
          loaiPgg = fetchedPgg.loaiPhieuGiamGia !== undefined ? Number(fetchedPgg.loaiPhieuGiamGia) : loaiPgg
          isPercent = loaiPgg === 1
          rawVal = Number(fetchedPgg.giaTriGiamGia || rawVal)
          maxDiscountVal = getNumber(fetchedPgg.giamToiDa, maxDiscountVal)
          minOrderVal = getNumber(fetchedPgg.hoaDonToiThieu, minOrderVal)
        }
      } catch (e) {
        console.warn('Không thể tải thông tin phiếu giảm giá phụ trợ:', e)
      }
    }

    invoiceData.value.createTime = formatDateTime(data['ngayTao'] || data['createdAt'])
    invoiceData.value.lastUpdated = formatDateTime(data['ngayCapNhat'] || data['updatedAt'] || data['thoiGianThanhToan'] || data['ngayTao'] || data['createdAt'])

    invoiceData.value.delivery = {
      address: getString(data['diaChiNhanHang'], data['diaChi'], data['address'], '—'),
      type: Number(data['loaiDon']) === 1 ? 'Tại quầy' : 'Online',
      carrier: getString(data['donViVanChuyen'], data['tenDonViVanChuyen'], data['carrier'], '—'),
      receiveType: Number(data['loaiDon']) === 1 ? 'Nhận tại quầy' : 'Giao hàng tận nơi',
      note: getString(data['ghiChu'], data['note'], '—')
    }

    // Danh sách sản phẩm
    let rawList = data['chiTietHoaDon'] || data['chiTietHoaDons'] || data['danhSachChiTiet'] || data['items'] || []

    if (!Array.isArray(rawList) || rawList.length === 0) {
      try {
        let itemsRes = null
        if (resolvedId) {
          itemsRes = await api.get(`/api/hoa-don/${resolvedId}/chi-tiet`)
        } else {
          itemsRes = await api.get(`/api/hoa-don/code/${encodeURIComponent(queryParam)}/chi-tiet`)
        }
        if (itemsRes && Array.isArray(itemsRes.data)) {
          rawList = itemsRes.data
        }
      } catch (e) {
        console.warn('Không gọi được API chi tiết sản phẩm riêng:', e)
      }
    }

    const detailList = Array.isArray(rawList) ? rawList : []
    invoiceData.value.items = detailList.map(mapInvoiceItem)

    // Tính toán tổng tiền
    const calcSum = invoiceData.value.items.reduce((sum, it) => sum + it.total, 0)
    const productTotal = calcSum > 0 ? calcSum : getNumber(data['tongTienHang'], data['tienHang'], data['tongTien'], 0)
    const shippingFee = getNumber(data['phiShip'], data['phiVanChuyen'], 0)

    let discount = 0
    if (rawVal > 0) {
      if (isPercent) {
        const percentVal = (productTotal * rawVal) / 100
        const maxVal = maxDiscountVal > 0 ? maxDiscountVal : Infinity
        discount = Math.min(percentVal, maxVal)
      } else {
        discount = rawVal
      }
    } else if (data['tongTienGiamGia'] != null || data['tienGiamGia'] != null) {
      discount = getNumber(data['tongTienGiamGia'], data['tienGiamGia'], 0)
    }

    const dbTongTien = getNumber(data['tongTienHang'], data['tienHang'], data['tongTien'], 0)
    const dbTienSauGiam = getNumber(data['tienSauGiamGia'], 0)
    if (discount <= 0 && dbTongTien > 0 && dbTienSauGiam > 0 && dbTongTien > dbTienSauGiam) {
      discount = dbTongTien - dbTienSauGiam
    }

    discount = Math.min(discount, productTotal)
    const totalPayment = Math.max(0, productTotal + shippingFee - discount)

    const hasVoucher = Boolean((voucherCode && voucherCode !== '—') || (voucherName && voucherName !== 'Không sử dụng') || discount > 0)

    invoiceData.value.voucher = {
      hasVoucher,
      code: voucherCode || (hasVoucher ? 'VOUCHER' : '—'),
      name: voucherName || (hasVoucher ? 'Phiếu giảm giá' : 'Không sử dụng'),
      type: isPercent ? 'Giảm theo phần trăm (%)' : 'Giảm tiền trực tiếp (VNĐ)',
      isPercent,
      rawValue: rawVal,
      valueText: isPercent ? `${rawVal}%` : (rawVal > 0 ? formatMoney(rawVal) : (discount > 0 ? formatMoney(discount) : '')),
      maxDiscount: maxDiscountVal,
      minOrderValue: minOrderVal,
      discountAmount: discount
    }

    invoiceData.value.summary = {
      totalProductPrice: productTotal,
      shippingFee,
      voucherDiscount: discount,
      voucherName: voucherName || (hasVoucher ? voucherCode : ''),
      totalPayment
    }

    const paymentStatus = data['trangThaiThanhToan'] !== undefined ? data['trangThaiThanhToan'] : data['daThanhToan']
    const isPaid = paymentStatus === true || Number(paymentStatus) === 1
    const defaultMethod = Number(data['loaiDon']) === 1 ? 'Tiền mặt' : 'Chuyển khoản ngân hàng'
    const transCode = data['maGiaoDich'] || data['transactionCode'] || ''
    const noteThanhToan = data['ghiChuThanhToan'] || ''

    let paymentMethodResolved = getString(data['hinhThucThanhToan'], data['phuongThucThanhToan'], defaultMethod)
    let paymentTransactions = []

    try {
      let payRes = null
      if (resolvedId) {
        payRes = await api.get(`/api/hoa-don/${resolvedId}/thanh-toan`)
      } else if (returnedCode) {
        payRes = await api.get(`/api/hoa-don/code/${encodeURIComponent(returnedCode)}/thanh-toan`)
      }
      if (payRes && Array.isArray(payRes.data) && payRes.data.length > 0) {
        paymentTransactions = payRes.data.map(p => ({
          id: p.id,
          method: p.tenPhuongThuc || p.idPhuongThuc?.tenPhuongThuc || 'Thanh toán',
          amount: Number(p.soTien || 0),
          time: formatDateTime(p.thoiGian),
          code: p.maGiaoDich || ''
        }))
        const validTrans = paymentTransactions.filter(p => Number(p.amount) > 0)
        const targetList = validTrans.length > 0 ? validTrans : paymentTransactions

        const hasCash = targetList.some(p => {
          const m = (p.method || '').toLowerCase()
          return m.includes('tiền mặt') || m.includes('tien mat')
        })
        const hasTransfer = targetList.some(p => {
          const m = (p.method || '').toLowerCase()
          return m.includes('chuyển khoản') || m.includes('ngân hàng') || m.includes('chuyen khoan') || m.includes('vnpay')
        })

        const transferFull = targetList.some(p => {
          const m = (p.method || '').toLowerCase()
          const isCk = m.includes('chuyển khoản') || m.includes('ngân hàng') || m.includes('chuyen khoan') || m.includes('vnpay')
          return isCk && (Number(p.amount) >= totalPayment * 0.95 || (targetList.length >= 2 && Number(p.amount) === Number(targetList[0].amount)))
        })
        const noteStr = String(data['ghiChu'] || data['note'] || '').toLowerCase()
        const isExplicitCombinedNote = noteStr.includes('thanh toán kết hợp')
        const rawPt = String(data['phuongThucThanhToan'] || data['hinhThucThanhToan'] || '').toLowerCase()

        if (transferFull && !isExplicitCombinedNote && !rawPt.includes('kết hợp')) {
          paymentMethodResolved = 'Chuyển khoản'
        } else if (hasCash && hasTransfer && (isExplicitCombinedNote || rawPt.includes('kết hợp'))) {
          paymentMethodResolved = 'Kết hợp'
        }

        else if (hasTransfer) {
          paymentMethodResolved = 'Chuyển khoản'
        } else if (hasCash) {
          paymentMethodResolved = 'Tiền mặt'
        } else if (targetList.length > 0) {
          { paymentMethodResolved = targetList[0].method || 'Chuyển khoản'
          }


        }
      }
    } catch (e) {
      console.warn('Không tải được danh sách thanh toán chi tiết:', e)
    }

    invoiceData.value.paymentHistory = {
      method: paymentMethodResolved,
      description: noteThanhToan,
      time: formatDateTime(data['thoiGianThanhToan'] || data['ngayTao']),
      status: isPaid ? 'Đã thanh toán' : 'Chưa thanh toán',
      amount: totalPayment,
      transCode,
      transactions: paymentTransactions
    }

    const noteText = String(data['ghiChu'] || data['note'] || '')
    let currentStatus = data['trangThaiHienThi'] !== undefined ? data['trangThaiHienThi'] : (data['trangThai'] !== undefined ? data['trangThai'] : (data['status'] || 1))
    if (Number(currentStatus) === 6) {
      if (noteText.toLowerCase().includes('tối đa 3 lần') || noteText.toLowerCase().includes('hủy đơn do giao lại')) {
        currentStatus = 6
      } else if (noteText.toLowerCase().includes('giao hàng thất bại')) {
        currentStatus = 7
      }
    }
    const isAtStore = Number(data['loaiDon']) === 1 || data['loaiDon'] === 'Tại quầy' || invoiceData.value.delivery?.type === 'Tại quầy'
    applyOrderStatus(currentStatus, isAtStore)

    // Lịch sử thao tác
    let rawLogs = data['lichSuHoaDon'] || data['lichSuHoaDons'] || data['lichSu']
    if (!Array.isArray(rawLogs) || rawLogs.length === 0) {
      try {
        let logsRes = null
        if (resolvedId) {
          logsRes = await api.get(`/api/hoa-don/${resolvedId}/lich-su`)
        } else if (returnedCode) {
          logsRes = await api.get(`/api/hoa-don/code/${encodeURIComponent(returnedCode)}/lich-su`)
        }
        if (logsRes && Array.isArray(logsRes.data) && logsRes.data.length > 0) {
          rawLogs = logsRes.data
        }
      } catch (e) {
        console.warn('Không gọi được API lịch sử hóa đơn:', e)
      }
    }

    if (Array.isArray(rawLogs) && rawLogs.length > 0) {
      let mappedLogs = rawLogs.map((l, i) => ({
        id: l.id || i,
        action: l.hanhDong || l.ghiChu || l.thaoTac || l.action || 'Thao tác hóa đơn',
        hanhDong: l.hanhDong || l.ghiChu || l.thaoTac || l.action || 'Thao tác hóa đơn',
        thoiGian: l.thoiGian || l.ngayTao || l.thoiGianTao,
        nguoiThucHien: getLogActor(l),
        ghiChu: l.ghiChu || '',
        isDone: true
      }))
      orderLogs.value = mappedLogs
    } else {
      orderLogs.value = buildLogsByStatus(currentStatus, data['ngayTao'] || data['createdAt'], isAtStore, {
        finishDate: data['thoiGianThanhToan'] || data['ngayCapNhat'] || data['updatedAt'],
        employeeName,
        note: data['ghiChu']
      })
    }

    const createTimeStr = formatDateTime(data['ngayTao'] || data['createdAt'])
    if (invoiceData.value.steps[0] && !invoiceData.value.steps[0].time) {
      invoiceData.value.steps[0].time = createTimeStr
    }
  } catch (err) {
    if (currentRequest !== requestId) return
    console.error('Lỗi khi tải chi tiết hóa đơn:', err)
  } finally {
    if (currentRequest === requestId) {
      loading.value = false
      // Tự động in hóa đơn nếu chuyển hướng từ thanh toán (query param autoPrint=true)
      if ((route.query?.autoPrint === 'true' || route.query?.autoPrint === '1') && !hasAutoPrinted.value) {
        if (!isOnlineWaitingConfirm.value && Number(invoiceData.value.status) !== 6) {
          hasAutoPrinted.value = true
          setTimeout(() => {
            handlePrint()
          }, 500)
        }
      }
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

// =========================================================================
// STEPPER: HIỂN THỊ CÁC BƯỚC ĐÃ TỚI + DUY NHẤT 1 BƯỚC TIẾP THEO LIỀN KỀ (PENDING)
// =========================================================================
const visibleSteps = computed(() => {
  const isAtStore = invoiceData.value.delivery?.type === 'Tại quầy' || Number(invoiceData.value.delivery?.loaiDon) === 1
  const curStatus = Number(invoiceData.value.status) || 1
  const createTime = invoiceData.value.createTime || invoiceData.value.steps[0]?.time || ''
  const finishTime = invoiceData.value.lastUpdated || invoiceData.value.paymentHistory?.time || createTime

  // Đơn đã hủy (status = 6)
  if (curStatus === 6) {
    return [
      { id: 1, name: isAtStore ? 'Hóa đơn chờ' : 'Chờ xác nhận', time: createTime, done: true, active: false, pending: false, icon: 'hourglass' },
      { id: 6, name: 'Đã hủy đơn', time: finishTime, done: false, active: true, pending: false, isCancelled: true, icon: 'cancelled' }
    ]
  }

  // Đơn giao hàng thất bại (status = 7)
  if (curStatus === 7) {
    return [
      { id: 1, name: 'Chờ xác nhận', time: createTime, done: true, active: false, pending: false, icon: 'hourglass' },
      { id: 2, name: 'Đã xác nhận', done: true, active: false, pending: false, icon: 'check' },
      { id: 3, name: 'Chờ vận chuyển', done: true, active: false, pending: false, icon: 'box' },
      { id: 4, name: 'Đang vận chuyển', done: true, active: false, pending: false, icon: 'truck' },
      { id: 7, name: 'Giao hàng thất bại', time: finishTime, done: false, active: true, pending: false, isFailed: true, icon: 'failed' }
    ]
  }

  // Đơn tại quầy
  if (isAtStore) {
    return [
      { id: 1, name: 'Hóa đơn chờ', time: createTime, done: curStatus >= 2, active: curStatus === 1, pending: false, icon: 'hourglass' },
      { id: 2, name: 'Đã xác nhận', time: curStatus >= 2 ? finishTime : '', done: curStatus >= 5, active: curStatus === 2, pending: curStatus < 2, icon: 'check' },
      { id: 5, name: 'Hoàn thành', time: curStatus >= 5 ? finishTime : '', done: curStatus >= 5, active: curStatus >= 5, pending: curStatus < 5, icon: 'flag' }
    ]
  }

  // Đơn Online: Lấy các bước <= curStatus VÀ CỘNG THÊM 1 BƯỚC TIẾP THEO (curStatus + 1)
  const maxStepToShow = Math.min(curStatus + 1, 5)

  return invoiceData.value.steps
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
          icon: s.id === 1 ? 'hourglass' : (s.id === 5 ? 'flag' : (s.id === 4 ? 'truck' : (s.id === 3 ? 'box' : 'check')))
        }
      })
})

// Thông tin trạng thái kế tiếp
const nextStatusInfo = computed(() => {
  const isAtStore = invoiceData.value.delivery?.type === 'Tại quầy' || Number(invoiceData.value.delivery?.loaiDon) === 1
  const cur = Number(invoiceData.value.status)
  if (isAtStore) {
    if (cur === 1) return { nextStatus: 2, label: 'Xác Nhận Đơn Hàng' }
    if (cur === 2) return { nextStatus: 5, label: 'Hoàn Thành Đơn Hàng' }
    return null
  }
  switch (cur) {
    case 1:
      return { nextStatus: 2, label: 'Xác Nhận Đơn Hàng' }
    case 2:
      return { nextStatus: 3, label: 'Chờ Vận Chuyển' }
    case 3:
      return { nextStatus: 4, label: 'Giao Vận Chuyển' }
    case 4:
      return { nextStatus: 5, label: 'Hoàn Thành Đơn' }
    case 7:
      return null
    default:
      return null
  }
})

const saveStatusChange = async (newStatusOverride = null, customNote = null, customerEmail = null) => {
  const newStatus = Number(newStatusOverride !== null ? newStatusOverride : 1)
  const note = customNote !== null ? customNote : ''
  const id = invoiceId.value || invoiceData.value.id
  const code = invoiceCode.value || invoiceData.value.code

  if (!newStatus || newStatus < 1 || newStatus > 7) {
    showToast('Vui lòng chọn trạng thái hợp lệ!', 'warning')
    return
  }

  updatingStatus.value = true
  const currentAuth = getCurrentUser()
  const authStaffName = currentAuth?.tenNhanVien || currentAuth?.tenTaiKhoan || 'Nhân viên bán hàng'
  try {
    let success = false
    let lastError = null

    const payload = {
      trangThai: newStatus,
      nguoiThucHien: authStaffName,
      nguoiCapNhat: authStaffName,
      ghiChu: note || undefined,
      email: customerEmail ? customerEmail.trim() : undefined
    }
    const emailQuery = customerEmail ? `&email=${encodeURIComponent(customerEmail.trim())}` : ''

    // Bước 1: Thử gọi PUT /api/hoa-don/{id}/trang-thai
    if (id) {
      try {
        await api.put(`/api/hoa-don/${id}/trang-thai?trangThai=${newStatus}${emailQuery}`, payload)
        success = true
      } catch (err) {
        lastError = err
        console.warn('Cập nhật theo ID /trang-thai thất bại, thử cập nhật thực thể:', err)
      }
    }

    // Bước 2: Thử gọi PUT /api/hoa-don/{id} (cập nhật trực tiếp thực thể)
    if (!success && id) {
      try {
        await api.put(`/api/hoa-don/${id}`, payload)
        success = true
      } catch (err) {
        lastError = err
        console.warn('Cập nhật qua PUT /api/hoa-don/{id} thất bại:', err)
      }
    }

    // Bước 3: Thử gọi theo mã hóa đơn /api/hoa-don/code/{code}/trang-thai
    if (!success && code) {
      try {
        await api.put(`/api/hoa-don/code/${encodeURIComponent(code)}/trang-thai?trangThai=${newStatus}${emailQuery}`, payload)
        success = true
      } catch (err) {
        lastError = err
        console.error('Cập nhật theo mã thất bại:', err)
      }
    }

    if (success) {
      const isAtStore = invoiceData.value.delivery?.type === 'Tại quầy' || Number(invoiceData.value.delivery?.loaiDon) === 1
      applyOrderStatus(newStatus, isAtStore)
      invoiceData.value.status = newStatus
      const option = statusOptions.find(o => o.value === newStatus)
      const labelText = option ? option.label : (newStatus === 7 ? 'Giao hàng thất bại' : newStatus)
      if (customerEmail && newStatus === 2) {
        showToast(`Đã xác nhận đơn hàng và gửi email thông báo tới ${customerEmail}!`)
      } else {
        showToast(`Đã cập nhật trạng thái hóa đơn thành "${labelText}"!`)
      }
      if (code) {
        await loadDetail(code)
      } else if (id) {
        await loadDetail(id)
      }
      // Tự động in hóa đơn ngay khi đơn hàng chuyển sang trạng thái "Đã xác nhận" (status 2)
      // hoặc trạng thái "Hoàn thành" (status 5) đối với đơn tại quầy
      const isStoreOrder = invoiceData.value.delivery?.type === 'Tại quầy' || Number(invoiceData.value.delivery?.loaiDon) === 1
      if (newStatus === 2 || (newStatus === 5 && isStoreOrder)) {
        setTimeout(() => {
          handlePrint()
        }, 500)
      }
    } else {
      const msg = lastError?.response?.data?.message || lastError?.message || 'Cập nhật trạng thái thất bại. Vui lòng thử lại!'
      showToast(msg, 'warning')
    }
  } catch (error) {
    showToast(error.response?.data?.message || 'Cập nhật trạng thái thất bại!', 'warning')
  } finally {
    updatingStatus.value = false
  }
}

// Modal Xác Nhận Tùy Chỉnh
const confirmModal = ref({
  show: false,
  title: 'Xác Nhận Thao Tác',
  message: '',
  subText: '',
  confirmText: 'Đồng ý',
  cancelText: 'Hủy bỏ',
  type: 'primary',
  showPrintPdfOption: false,
  printPdf: true,
  showReasonInput: false,
  reasonPlaceholder: 'Nhập lý do...',
  reason: '',
  showEmailInput: false,
  sendEmail: true,
  customerEmail: '',
  onConfirm: null
})

const openConfirmModal = ({
                            title = 'Xác Nhận Thao Tác',
                            message = '',
                            subText = '',
                            confirmText = 'Đồng ý',
                            cancelText = 'Hủy bỏ',
                            type = 'primary',
                            showPrintPdfOption = false,
                            printPdf = true,
                            showReasonInput = false,
                            reasonPlaceholder = 'Nhập lý do...',
                            showEmailInput = false,
                            sendEmail = true,
                            customerEmail = '',
                            onConfirm = null
                          }) => {
  confirmModal.value = {
    show: true,
    title,
    message,
    subText,
    confirmText,
    cancelText,
    type,
    showPrintPdfOption,
    printPdf,
    showReasonInput,
    reasonPlaceholder,
    reason: '',
    showEmailInput,
    sendEmail,
    customerEmail,
    onConfirm
  }
}

const closeConfirmModal = () => {
  confirmModal.value.show = false
}

const handleConfirmAccept = async () => {
  const cb = confirmModal.value.onConfirm
  const shouldPrint = confirmModal.value.printPdf && confirmModal.value.showPrintPdfOption
  const emailInput = confirmModal.value.customerEmail ? confirmModal.value.customerEmail.trim() : ''
  const willSendEmail = confirmModal.value.sendEmail && confirmModal.value.showEmailInput
  if (willSendEmail && emailInput && !emailInput.includes('@')) {
    showToast('Vui lòng nhập định dạng email hợp lệ (VD: khachhang@gmail.com)!', 'warning')
    return
  }
  confirmModal.value.show = false
  if (cb) {
    await cb()
    // Nếu chưa được in tự động từ saveStatusChange (tránh in 2 lần cho trạng thái 2)
    if (shouldPrint && Number(invoiceData.value.status) !== 2) {
      setTimeout(() => {
        handlePrint()
      }, 400)
    }
  }
}

// Gửi lại email xác nhận đơn hàng chủ động
const sendingEmail = ref(false)
const handleSendConfirmationEmail = async (targetEmail = null) => {
  const id = invoiceId.value || invoiceData.value.id
  const code = invoiceCode.value || invoiceData.value.code
  const email = targetEmail || (invoiceData.value.customer?.email && invoiceData.value.customer?.email !== '---' && invoiceData.value.customer?.email !== '—' ? invoiceData.value.customer.email.trim() : null)
  if (!email || !email.includes('@')) {
    showToast('Khách hàng chưa có địa chỉ email hợp lệ để gửi thông báo!', 'warning')
    return
  }
  sendingEmail.value = true
  try {
    let success = false
    if (id) {
      try {
        await api.post(`/api/hoa-don/${id}/send-email`, { email })
        success = true
      } catch (err) {}
    }
    if (!success && code) {
      try {
        await api.post(`/api/hoa-don/code/${encodeURIComponent(code)}/send-email`, { email })
        success = true
      } catch (err) {}
    }
    if (success) {
      showToast(`Đã gửi email xác nhận đơn hàng tới ${email} thành công!`)
    } else {
      showToast('Gửi email xác nhận thất bại. Vui lòng kiểm tra lại!', 'warning')
    }
  } catch (e) {
    showToast('Lỗi khi gửi email xác nhận đơn hàng!', 'warning')
  } finally {
    sendingEmail.value = false
  }
}

// Bấm chuyển trạng thái tiếp theo từ nút / nốt tròn
const handleNextStatus = () => {
  if (!nextStatusInfo.value || updatingStatus.value) return
  const info = nextStatusInfo.value
  const cur = Number(invoiceData.value.status)
  const isConfirming = cur === 1 || info.nextStatus === 2
  const isOnlineWaiting = isOnlineDelivery.value && cur === 1
  const existingEmail = (invoiceData.value.customer?.email && invoiceData.value.customer?.email !== '---' && invoiceData.value.customer?.email !== '—')
      ? invoiceData.value.customer.email.trim()
      : ''
  openConfirmModal({
    title: isConfirming ? 'Xác Nhận Đơn Hàng' : 'Xác Nhận Chuyển Trạng Thái',
    message: isConfirming
        ? `Bạn có chắc chắn muốn xác nhận đơn hàng "${invoiceData.value.code}" không?`
        : `Bạn có chắc chắn muốn chuyển trạng thái đơn hàng sang "${info.label}" không?`,
    subText: isConfirming
        ? (isOnlineWaiting
            ? 'Hệ thống sẽ cập nhật trạng thái đơn hàng sang "Đã xác nhận" và gửi email thông báo chi tiết cho khách hàng.'
            : 'Hệ thống sẽ cập nhật trạng thái đơn hàng, gửi email xác nhận và hỗ trợ xuất in hóa đơn (PDF).')
        : 'Hệ thống sẽ cập nhật trạng thái đơn hàng và mở bước tiếp theo trên tiến trình.',
    confirmText: isConfirming ? 'Xác nhận đơn' : 'Đồng ý chuyển',
    cancelText: 'Hủy bỏ',
    type: 'primary',
    showPrintPdfOption: isConfirming && !isOnlineWaiting,
    printPdf: !isOnlineWaiting,
    showEmailInput: isConfirming,
    sendEmail: true,
    customerEmail: existingEmail,
    onConfirm: async () => {
      const emailToSend = (confirmModal.value.showEmailInput && confirmModal.value.sendEmail && confirmModal.value.customerEmail)
          ? confirmModal.value.customerEmail.trim()
          : null
      await saveStatusChange(info.nextStatus, `Chuyển tiếp trạng thái: ${info.label}`, emailToSend)
    }
  })
}

// Đếm số lần đã thực hiện giao lại đơn hàng từ lịch sử thao tác hoặc ghi chú
const deliveryRetryCount = computed(() => {
  let count = 0
  if (orderLogs.value && orderLogs.value.length > 0) {
    count = orderLogs.value.filter(l => {
      const act = (l.hanhDong || l.action || l.ghiChu || '').toLowerCase()
      return act.includes('giao lại')
    }).length
  }
  const note = String(invoiceData.value.delivery?.note || '').toLowerCase()
  const mRetry = note.match(/giao lại.*lần\s*(\d+)/i)
  if (mRetry && mRetry[1]) {
    count = Math.max(count, parseInt(mRetry[1], 10))
  }
  const mFail = note.match(/thất bại.*lần\s*(\d+)/i)
  if (mFail && mFail[1]) {
    const failNum = parseInt(mFail[1], 10)
    if (failNum > 1) {
      count = Math.max(count, failNum - 1)
    }
  }
  return Math.min(3, Math.max(0, count))
})

const maxDeliveryRetries = 3

// Đơn hàng có thể giao lại tiếp không (tối đa 3 lần)
const canRetryDelivery = computed(() => {
  return Number(invoiceData.value.status) === 7 && deliveryRetryCount.value < maxDeliveryRetries
})

const isMaxRetriesReached = computed(() => {
  return deliveryRetryCount.value >= maxDeliveryRetries
})

// Bấm đánh dấu giao hàng thất bại
const handleFailedDelivery = () => {
  if (updatingStatus.value) return
  const retries = deliveryRetryCount.value
  const isLastAttempt = retries >= maxDeliveryRetries // Đã giao lại đủ 3 lần mà lần này vẫn thất bại

  openConfirmModal({
    title: isLastAttempt ? 'Đã Giao Lại 3 Lần Thất Bại - Hủy Đơn' : 'Xác Nhận Giao Hàng Thất Bại',
    message: isLastAttempt
        ? `Đơn hàng "${invoiceData.value.code}" đã được giao lại tối đa 3 lần nhưng không thành công. Hệ thống sẽ tự động HỦY ĐƠN HÀNG.`
        : `Đánh dấu đơn hàng "${invoiceData.value.code}" giao hàng không thành công (Đã giao lại: ${retries}/3 lần)?`,
    subText: isLastAttempt
        ? 'Theo quy định, đơn hàng giao lại tối đa 3 lần thất bại sẽ được chuyển sang trạng thái "Đã hủy".'
        : (retries === maxDeliveryRetries - 1
            ? 'LƯU Ý: Đơn hàng đã ở lần giao lại thứ 3 (tối đa). Nếu thất bại lần này, đơn hàng sẽ phải hủy.'
            : 'Vui lòng nhập lý do giao hàng thất bại (khách từ chối nhận, không liên lạc được, sai địa chỉ...).'),
    confirmText: isLastAttempt ? 'Xác nhận hủy đơn' : 'Xác nhận thất bại',
    cancelText: 'Quay lại',
    type: 'danger',
    showReasonInput: true,
    reasonPlaceholder: 'Nhập lý do giao hàng thất bại (VD: Khách không nghe máy sau 3 lần gọi)...',
    onConfirm: async () => {
      const reason = confirmModal.value.reason?.trim() || 'Giao hàng không thành công'
      if (isLastAttempt) {
        await saveStatusChange(6, `Hủy đơn do giao lại tối đa 3 lần thất bại: ${reason}`)
      } else {
        await saveStatusChange(7, `Giao hàng thất bại: ${reason}`)
      }
    }
  })
}

// Bấm giao lại đơn hàng sau thất bại
const handleRetryDelivery = () => {
  if (updatingStatus.value) return
  const nextRetry = deliveryRetryCount.value + 1
  if (nextRetry > maxDeliveryRetries) {
    showToast('Đơn hàng đã được giao lại tối đa 3 lần. Chỉ có thể hủy đơn!', 'warning')
    return
  }
  openConfirmModal({
    title: 'Xác Nhận Giao Lại Đơn Hàng',
    message: `Chuyển đơn hàng "${invoiceData.value.code}" về trạng thái "Đang vận chuyển" để giao lại lần ${nextRetry}/3?`,
    subText: `Quy định: Đơn hàng chỉ được giao lại tối đa 3 lần (đây là lần ${nextRetry}/3). Sau 3 lần nếu vẫn thất bại, đơn hàng sẽ tự động hủy.`,
    confirmText: `Xác nhận giao lại (Lần ${nextRetry}/3)`,
    cancelText: 'Hủy bỏ',
    type: 'primary',
    onConfirm: async () => {
      await saveStatusChange(4, `Tiến hành giao lại đơn hàng (Lần ${nextRetry}/3)`)
    }
  })
}

// Bấm xác nhận thanh toán hóa đơn (đơn online chờ xác nhận thì không in hóa đơn)
const handleConfirmPaymentAndPrint = () => {
  if (updatingStatus.value) return
  const isOnlineWaiting = isOnlineWaitingConfirm.value
  openConfirmModal({
    title: isOnlineWaiting ? 'Xác Nhận Thanh Toán' : 'Xác Nhận Thanh Toán Hóa Đơn',
    message: `Xác nhận hóa đơn "${invoiceData.value.code}" đã hoàn tất thanh toán?`,
    subText: isOnlineWaiting
        ? 'Hệ thống sẽ cập nhật trạng thái "Đã thanh toán" cho đơn hàng (chưa in hóa đơn do đơn online đang chờ xác nhận).'
        : 'Hệ thống sẽ cập nhật trạng thái "Đã thanh toán" và tự động mở hộp thoại in hóa đơn.',
    confirmText: isOnlineWaiting ? 'Xác nhận thanh toán' : 'Xác nhận & In hóa đơn',
    cancelText: 'Hủy bỏ',
    type: 'primary',
    onConfirm: async () => {
      const id = invoiceId.value || invoiceData.value.id
      const code = invoiceCode.value || invoiceData.value.code
      if (!id && !code) return
      updatingStatus.value = true
  const currentAuth = getCurrentUser()
  const authStaffName = currentAuth?.tenNhanVien || currentAuth?.tenTaiKhoan || 'Nhân viên bán hàng'
      try {
        let success = false
        if (id) {
          try {
            await api.put(`/api/hoa-don/${id}`, {
              trangThaiThanhToan: 1
            })
            success = true
          } catch (e) {
            console.warn('Cập nhật thanh toán ID lỗi:', e)
          }
        }
        if (!success && code) {
          try {
            await api.put(`/api/hoa-don/code/${encodeURIComponent(code)}`, {
              trangThaiThanhToan: 1
            })
            success = true
          } catch (e) {
            console.warn('Cập nhật thanh toán mã lỗi:', e)
          }
        }
        if (success) {
          showToast('Xác nhận thanh toán thành công!')
          if (code) await loadDetail(code)
          if (!isOnlineWaitingConfirm.value) {
            setTimeout(() => {
              handlePrint()
            }, 500)
          }
        } else {
          showToast('Không thể cập nhật thanh toán, vui lòng thử lại!', 'warning')
        }
      } catch (err) {
        showToast('Cập nhật thanh toán thất bại!', 'warning')
      } finally {
        updatingStatus.value = false
      }
    }
  })
}

// Bấm trực tiếp vào nốt tròn trên thanh tiến trình
const handleStepClick = (step) => {
  if (!step || updatingStatus.value || Number(invoiceData.value.status) === 5 || Number(invoiceData.value.status) === 6) return
  // Nếu ấn vào bước tiếp theo đang chờ
  if (step.pending && nextStatusInfo.value && step.id === nextStatusInfo.value.nextStatus) {
    handleNextStatus()
  }
}

// Bấm hủy đơn (chỉ cho phép ở trạng thái 1: Chờ xác nhận và 7: Giao hàng thất bại; từ "Đã xác nhận" trở đi không còn nút hủy)
const handleCancelQuick = () => {
  const st = Number(invoiceData.value.status)
  if ((st !== 1 && st !== 7) || updatingStatus.value) return
  openConfirmModal({
    title: 'Xác Nhận Hủy Đơn Hàng',
    message: `Bạn có chắc chắn muốn hủy đơn hàng ${invoiceData.value.code} không?`,
    subText: 'Hành động này sẽ dừng tiến trình và không thể hoàn tác.',
    confirmText: 'Xác nhận hủy đơn',
    cancelText: 'Quay lại',
    type: 'danger',
    showReasonInput: true,
    reasonPlaceholder: 'Nhập lý do hủy đơn hàng...',
    onConfirm: async () => {
      const reason = confirmModal.value.reason?.trim() || 'Hủy đơn hàng từ giao diện chi tiết'
      await saveStatusChange(6, reason)
    }
  })
}

const statusOptions = [
  { value: 1, label: 'Chờ Xác Nhận' },
  { value: 2, label: 'Đã Xác Nhận' },
  { value: 3, label: 'Chờ Vận Chuyển' },
  { value: 4, label: 'Đang Vận Chuyển' },
  { value: 5, label: 'Hoàn Thành' },
  { value: 6, label: 'Đã Hủy' },
  { value: 7, label: 'Giao Hàng Thất Bại' }
]

// Bộ lọc sản phẩm
const productFilter = ref({ search: '', category: '', color: '', size: '', sort: '' })
const sortOptions = [
  { value: 'price-asc', label: 'Giá tăng dần' },
  { value: 'price-desc', label: 'Giá giảm dần' },
  { value: 'qty-desc', label: 'Số lượng giảm dần' }
]

const availableColors = computed(() => {
  const set = new Set()
  invoiceData.value.items.forEach(it => {
    if (it.color && it.color !== '---' && it.color !== '—') set.add(it.color)
  })
  return Array.from(set)
})

const availableSizes = computed(() => {
  const set = new Set()
  invoiceData.value.items.forEach(it => {
    if (it.size && it.size !== '---' && it.size !== '—') set.add(it.size)
  })
  return Array.from(set)
})

const availableCategories = computed(() => {
  const set = new Set()
  invoiceData.value.items.forEach(it => {
    if (it.name) {
      if (it.name.toLowerCase().includes('áo phông')) set.add('Áo Phông')
      else if (it.name.toLowerCase().includes('polo')) set.add('Áo Polo')
      else set.add('Áo Phông Nam')
    }
  })
  if (set.size === 0) set.add('Áo Phông Nam')
  return Array.from(set)
})

const filteredProductItems = computed(() => {
  let list = [...invoiceData.value.items]
  if (productFilter.value.search) {
    const q = productFilter.value.search.trim().toLowerCase()
    list = list.filter(it => it.code?.toLowerCase().includes(q) || it.name?.toLowerCase().includes(q) || it.variantCode?.toLowerCase().includes(q))
  }
  if (productFilter.value.category) {
    const cat = productFilter.value.category.toLowerCase()
    list = list.filter(it => it.name?.toLowerCase().includes(cat))
  }
  if (productFilter.value.color) {
    list = list.filter(it => it.color === productFilter.value.color)
  }
  if (productFilter.value.size) {
    list = list.filter(it => it.size === productFilter.value.size)
  }
  if (productFilter.value.sort === 'price-asc') list.sort((a, b) => a.price - b.price)
  else if (productFilter.value.sort === 'price-desc') list.sort((a, b) => b.price - a.price)
  else if (productFilter.value.sort === 'qty-desc') list.sort((a, b) => b.quantity - a.quantity)
  return list
})

const handlePrint = () => {
  if (isOnlineWaitingConfirm.value) {
    showToast('Đơn hàng online đang chờ xác nhận là chưa in hóa đơn!', 'warning')
    return
  }
  if (Number(invoiceData.value.status) === 6) {
    showToast('Đơn hàng đã hủy, không thể in hóa đơn!', 'warning')
    return
  }
  const originalTitle = document.title
  document.title = `Hoa_Don_${invoiceData.value.code || 'HD001'}`
  window.print()
  setTimeout(() => {
    document.title = originalTitle
  }, 1200)
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

    <!-- Top Navigation Bar -->
    <div class="top-nav-bar">
      <div class="breadcrumb-nav">
        <router-link to="/hoa-don" class="crumb-parent">Hóa đơn</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Chi tiết hóa đơn</span>
      </div>

      <div class="top-actions-right">
        <button class="btn-top-nav" type="button" @click="quayLaiBanHang">
          <span class="nav-arrow">←</span> Quay lại bán hàng tại quầy
        </button>
        <button class="btn-top-nav" type="button" @click="quayLaiHoaDon">
          <span class="nav-arrow">←</span> Danh sách hóa đơn
        </button>
      </div>
    </div>

    <!-- Top Meta Information Card -->
    <div class="card-box meta-info-card">
      <div class="meta-info-grid">
        <div class="meta-info-col col-1">
          <div class="meta-row">
            <span class="meta-label">Mã Đơn Hàng:</span>
            <span class="meta-value meta-code-blue">{{ invoiceData.code }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Cập Nhật Gần Nhất:</span>
            <span class="meta-value">{{ invoiceData.lastUpdated || invoiceData.createTime || '---' }}</span>
          </div>
        </div>

        <div class="meta-info-col col-2">
          <div class="meta-row">
            <span class="meta-label">Ngày Tạo:</span>
            <span class="meta-value">{{ invoiceData.createTime || '---' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Loại đơn:</span>
            <span class="meta-value">
              <span class="badge-loai-don">{{ invoiceData.delivery.type || 'Tại quầy' }}</span>
              <span v-if="isOnlineWaitingConfirm" class="badge-unprinted-pill" title="Đơn hàng online chờ xác nhận là chưa in hóa đơn">Chưa in HĐ</span>
            </span>
          </div>
        </div>

        <div class="meta-info-col col-3">
          <div class="meta-row">
            <span class="meta-label">Tạo bởi:</span>
            <span class="meta-value">{{ invoiceData.employee?.code || invoiceData.employee?.name || 'NV003' }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Hình thức nhận:</span>
            <span class="meta-value">{{ invoiceData.delivery.receiveType || 'Nhận tại quầy' }}</span>
          </div>
        </div>

        <!-- Cột Mã QR Hóa Đơn -->
        <div class="meta-info-col col-qr" v-if="false">
          <div
              class="meta-qr-container"
              :title="'Nhấp để tải mã QR hóa đơn ' + (invoiceData.code || '') + ' về máy'"
              @click="downloadInvoiceQr"
          >
            <img :src="invoiceQrDataUrl" alt="Mã QR Hóa Đơn" class="meta-qr-img" />
          </div>
        </div>
      </div>
    </div>

    <!-- Main Layout Grid -->
    <div class="main-layout-grid">
      <!-- Cột Trái -->
      <div class="left-section">
        <!-- Khung 1: Trạng thái đơn hàng -->
        <div class="card-box card-order-status">
          <div class="card-header-styled">
            <div class="card-title-styled">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" class="title-icon">
                <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
              </svg>
              Trạng thái đơn hàng
            </div>
            <span class="badge-status-top" :class="'badge-status-' + invoiceData.status">
              {{ invoiceData.statusBadge }}
            </span>
          </div>

          <!-- Thanh tiến trình Stepper (Hiện các bước đã tới + bước tiếp theo) -->
          <div class="stepper-track-wrap">
            <div class="stepper-step-item"
                 v-for="(step, idx) in visibleSteps"
                 :key="step.id"
                 :class="{
                   'step-item-done': step.done,
                   'step-item-active': step.active,
                   'step-item-pending': step.pending,
                   'step-item-cancelled': step.isCancelled,
                   'step-item-failed': step.isFailed
                 }">
              <!-- Vạch nối: Sáng xanh nếu bước hiện tại không phải pending -->
              <div v-if="idx > 0" class="step-connector-line" :class="{ 'line-active': !step.pending && !step.isCancelled && !step.isFailed, 'line-failed': step.isFailed, 'line-cancelled': step.isCancelled }"></div>

              <!-- Vòng tròn nốt: Click vào bước pending để kích hoạt chuyển sang bước đó -->
              <div class="step-circle-icon"
                   :class="{
                     'circle-done': step.done,
                     'circle-active': step.active && !step.isCancelled && !step.isFailed,
                     'circle-pending': step.pending,
                     'circle-red': step.isCancelled,
                     'circle-failed': step.isFailed,
                     'clickable': step.pending || step.isFailed
                   }"
                   :title="step.pending ? 'Nhấn để chuyển sang ' + step.name : (step.isFailed ? 'Giao hàng thất bại - Nhấn để xử lý' : step.name)"
                   @click="handleStepClick(step)">
                <svg v-if="step.icon === 'hourglass'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2">
                  <path d="M5 22h14M5 2h14M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2" />
                </svg>
                <svg v-else-if="step.icon === 'flag'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"></path>
                  <line x1="4" y1="22" x2="4" y2="15"></line>
                </svg>
                <svg v-else-if="step.icon === 'truck'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="1" y="3" width="15" height="13"></rect>
                  <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
                  <circle cx="5.5" cy="18.5" r="2.5"></circle>
                  <circle cx="18.5" cy="18.5" r="2.5"></circle>
                </svg>
                <svg v-else-if="step.icon === 'box'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                  <line x1="12" y1="22.08" x2="12" y2="12"></line>
                </svg>
                <svg v-else-if="step.icon === 'cancelled'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
                <svg v-else-if="step.icon === 'failed'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>

              <div class="step-item-title" :class="{ 'title-active': step.active && !step.isCancelled && !step.isFailed, 'title-done': step.done, 'title-pending': step.pending, 'text-danger': step.isCancelled || step.isFailed }">
                {{ step.name }}
              </div>
              <div v-if="step.time" class="step-item-time">{{ step.time }}</div>
            </div>
          </div>

          <!-- Banner thông báo khi giao hàng thất bại kèm số lần giao lại -->
          <div v-if="Number(invoiceData.status) === 7" class="retry-delivery-banner" :class="{ 'banner-limit-reached': isMaxRetriesReached }">
            <div class="banner-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <div class="banner-body">
              <div class="banner-title">
                <strong>Giao hàng không thành công</strong> &nbsp;•&nbsp;
                <span class="banner-pill">Đã giao lại {{ deliveryRetryCount }}/3 lần</span>
              </div>
              <div class="banner-desc">
                <template v-if="canRetryDelivery">
                  Đơn hàng được giao lại tối đa 3 lần (còn lại <strong>{{ 3 - deliveryRetryCount }}</strong> lần giao lại). Bạn có thể chọn <strong>"Giao lại đơn hàng"</strong> hoặc <strong>"Hủy đơn"</strong>.
                </template>
                <template v-else>
                  Đơn hàng đã được giao lại tối đa <strong>3/3 lần</strong> nhưng không thành công. Đơn hàng bắt buộc phải chuyển sang <strong>"Hủy đơn"</strong>.
                </template>
              </div>
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
              <!-- Nút chuyển tiếp trạng thái kế tiếp (VD: Xác nhận đơn & In hóa đơn) -->
              <button
                  v-if="nextStatusInfo"
                  class="btn-next-action"
                  :class="{ 'btn-confirm-highlight': nextStatusInfo.nextStatus === 2 }"
                  type="button"
                  :disabled="updatingStatus"
                  @click="handleNextStatus"
              >
                {{ nextStatusInfo.nextStatus === 2 ? (isOnlineDelivery ? '✓ Xác nhận đơn hàng' : '✓ Xác nhận đơn & In hóa đơn') : ('✓ ' + nextStatusInfo.label) }}
              </button>

              <!-- Nút Đánh dấu giao hàng thất bại khi đơn đang vận chuyển -->
              <button v-if="Number(invoiceData.status) === 4" class="btn-failed-action" type="button" :disabled="updatingStatus" @click="handleFailedDelivery">
                ✕ Giao hàng thất bại
              </button>

              <!-- Nút Giao lại đơn hàng khi đơn đã bị đánh dấu thất bại và chưa vượt quá 3 lần -->
              <button
                  v-if="Number(invoiceData.status) === 7 && canRetryDelivery"
                  class="btn-retry-action"
                  type="button"
                  :disabled="updatingStatus"
                  @click="handleRetryDelivery"
              >
                ↻ Giao lại đơn hàng (Lần {{ deliveryRetryCount + 1 }}/3)
              </button>

              <!-- Huy hiệu cảnh báo khi đã giao lại tối đa 3 lần -->
              <span
                  v-if="Number(invoiceData.status) === 7 && isMaxRetriesReached"
                  class="badge-retry-limit-exceeded"
              >
                ⚠️ Đã giao lại tối đa 3/3 lần - Bắt buộc hủy đơn
              </span>

              <!-- Nút Hủy đơn (chỉ hiển thị ở trạng thái Chờ xác nhận (1) và Giao hàng thất bại (7); từ Đã xác nhận (2) trở đi không còn nút hủy) -->
              <button v-if="Number(invoiceData.status) === 1 || Number(invoiceData.status) === 7" class="btn-cancel-action" type="button" :disabled="updatingStatus" @click="handleCancelQuick">
                ✕ Hủy đơn
              </button>
            </div>
          </div>
        </div>

        <!-- Khung 2: Khách hàng & Giao hàng -->
        <div class="info-dual-grid">
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
                  {{ invoiceData.customer.name }}
                  <span v-if="invoiceData.customer.name === 'Khách lẻ'" class="badge-guest-tag">Khách lẻ</span>
                </span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Mã khách hàng</span>
                <span class="info-row-val" :class="{ 'text-muted': invoiceData.customer.name === 'Khách lẻ' }">
                  {{ invoiceData.customer.name === 'Khách lẻ' ? 'Khách vãng lai' : (invoiceData.customer.code || '—') }}
                </span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Số điện thoại</span>
                <span class="info-row-val" :class="{ 'text-muted': !invoiceData.customer.phone || invoiceData.customer.phone === '---' || invoiceData.customer.phone === '—' }">
                  {{ (!invoiceData.customer.phone || invoiceData.customer.phone === '---' || invoiceData.customer.phone === '—') ? (invoiceData.customer.name === 'Khách lẻ' ? 'Không có (Mua tại quầy)' : '—') : invoiceData.customer.phone }}
                </span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Email</span>
                <div class="email-val-wrap">
                  <span class="info-row-val" :class="{ 'text-muted': !invoiceData.customer.email || invoiceData.customer.email === '---' || invoiceData.customer.email === '—' }">
                    {{ invoiceData.customer.email || '—' }}
                  </span>
                  <button
                      v-if="invoiceData.customer.email && invoiceData.customer.email.includes('@')"
                      type="button"
                      class="btn-send-email-pill"
                      :disabled="sendingEmail"
                      @click="handleSendConfirmationEmail()"
                      title="Gửi lại email xác nhận đơn hàng"
                  >
                    {{ sendingEmail ? 'Đang gửi...' : '✉ Gửi lại mail' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

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
                <span class="info-row-val">{{ invoiceData.delivery.address || '—' }}</span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Đơn vị vận chuyển</span>
                <span class="info-row-val">{{ invoiceData.delivery.carrier || '—' }}</span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Ghi chú</span>
                <span class="info-row-val">{{ invoiceData.delivery.note || '—' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột Phải -->
      <div class="right-section">
        <!-- Khung: Tổng kết thanh toán (Hiển thị chi tiết phiếu giảm giá bên trong) -->
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
            <span v-if="invoiceData.voucher?.hasVoucher || invoiceData.summary.voucherDiscount > 0" class="badge-voucher-applied-small">
              ĐÃ DÙNG VOUCHER
            </span>
            <span v-else class="badge-no-voucher-small">
              KHÔNG DÙNG VOUCHER
            </span>
          </div>

          <div class="summary-body">
            <div class="summary-line">
              <span class="s-label">Tổng tiền hàng</span>
              <span class="s-val">{{ formatMoney(invoiceData.summary.totalProductPrice) }}</span>
            </div>

            <div v-if="invoiceData.summary.shippingFee > 0" class="summary-line">
              <span class="s-label">Phí vận chuyển</span>
              <span class="s-val">+{{ formatMoney(invoiceData.summary.shippingFee) }}</span>
            </div>

            <!-- Dòng Giảm giá -->
            <div class="summary-line summary-discount-row">
              <span class="s-label discount-label-with-code">
                <span>Giảm giá</span>
                <span v-if="voucherDisplayName" class="inline-voucher-pill">
                  {{ voucherDisplayName }}
                </span>
              </span>
              <span class="s-val" :class="{ 'text-discount': invoiceData.summary.voucherDiscount > 0 }">
                {{ invoiceData.summary.voucherDiscount > 0 ? '-' : '- ' }}{{ formatMoney(invoiceData.summary.voucherDiscount) }}
              </span>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-total-line">
              <span class="total-label-caps">TỔNG TIỀN</span>
              <span class="total-price-blue">{{ formatMoney(invoiceData.summary.totalPayment) }}</span>
            </div>
          </div>
        </div>

        <!-- Khung 5: Lịch sử thanh toán -->
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
              <span class="badge-status-pill" :class="invoiceData.paymentHistory.status === 'Đã thanh toán' ? 'pill-paid' : 'pill-unpaid'">
                <span class="pill-dot">●</span> {{ invoiceData.paymentHistory.status }}
              </span>
            </div>

            <div class="pay-method-title">{{ invoiceData.paymentHistory.method }}</div>
            <div class="pay-time-trans" v-if="invoiceData.paymentHistory.time">
              {{ invoiceData.paymentHistory.time }}
            </div>

            <div class="pay-amount-row">
              <span class="pay-label-sub">Số tiền</span>
              <span class="pay-amount-bold">{{ formatMoney(invoiceData.paymentHistory.amount) }}</span>
            </div>

            <!-- Chi tiết các khoản thanh toán khi thanh toán kết hợp -->
            <div v-if="printSubTransactions.length > 0" class="multi-payment-subbox" style="margin-top: 10px; padding-top: 8px; border-top: 1px dashed #d6d0c3;">
              <div v-for="t in printSubTransactions" :key="t.id || t.label" class="multi-pay-line" style="display: flex; justify-content: space-between; align-items: center; font-size: 0.82rem; margin-bottom: 4px;">
                <span style="color: #647074;">{{ t.label }}:</span>
                <span style="font-weight: 700; color: #333;">{{ formatMoney(t.amount) }}</span>
              </div>
            </div>
          </div>

          <div class="payment-actions-col">
            <!-- Nút Xác nhận thanh toán & In hóa đơn khi đơn chưa thanh toán (chỉ hiện khi chưa hoàn thành và chưa hủy) -->
            <button
                v-if="Number(invoiceData.status) !== 5 && Number(invoiceData.status) !== 6 && invoiceData.paymentHistory.status !== 'Đã thanh toán'"
                class="btn-pay-confirm-action"
                type="button"
                :disabled="updatingStatus"
                @click="handleConfirmPaymentAndPrint"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              {{ isOnlineWaitingConfirm ? 'Xác nhận thanh toán' : 'Xác nhận thanh toán & In hóa đơn' }}
            </button>


            <!-- Nút In hóa đơn - khả dụng khi đã xác nhận hoặc đơn tại quầy (đơn online chờ xác nhận là CHƯA in hóa đơn) -->
            <button
                v-if="canPrintInvoice"
                class="btn-blue-action"
                type="button"
                @click="handlePrint"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 6 2 18 2 18 9"></polyline>
                <path d="M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2"></path>
                <rect x="6" y="14" width="12" height="8"></rect>
              </svg>
              In hóa đơn
            </button>

            <!-- Khối thông báo Chưa in hóa đơn khi đơn hàng online ở trạng thái Chờ xác nhận -->
            <div
                v-else-if="isOnlineWaitingConfirm"
                class="unprinted-invoice-notice"
            >
              <span class="unprinted-icon">ℹ</span> Chưa in hóa đơn (Chờ xác nhận)
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Khung 5: Danh sách sản phẩm -->
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
        <div class="product-count-sub">
          {{ filteredProductItems.length }}/{{ invoiceData.items.length }} sản phẩm
        </div>
      </div>

      <!-- Toolbar bộ lọc sản phẩm -->
      <div class="product-filters-toolbar">
        <div class="toolbar-item search-item">
          <div class="search-input-box">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
                v-model="productFilter.search"
                type="text"
                placeholder="Mã SPCT, SKU hoặc tên sản phẩm..."
                class="form-control-tb input-search"
            />
          </div>
        </div>

        <div class="toolbar-item">
          <Select2Combobox
              v-model="productFilter.category"
              :options="availableCategories"
              placeholder="Tất cả loại"
              all-label="Tất cả loại"
              search-placeholder="Tìm loại sản phẩm..."
          />
        </div>

        <div class="toolbar-item">
          <Select2Combobox
              v-model="productFilter.color"
              :options="availableColors"
              placeholder="Tất cả màu sắc"
              all-label="Tất cả màu sắc"
              search-placeholder="Tìm màu sắc..."
          />
        </div>

        <div class="toolbar-item">
          <Select2Combobox
              v-model="productFilter.size"
              :options="availableSizes"
              placeholder="Tất cả kích cỡ"
              all-label="Tất cả kích cỡ"
              search-placeholder="Tìm kích cỡ..."
          />
        </div>

        <div class="toolbar-item">
          <Select2Combobox
              v-model="productFilter.sort"
              :options="sortOptions"
              value-key="value"
              label-key="label"
              placeholder="Mặc định"
              all-label="Mặc định"
              search-placeholder="Tìm sắp xếp..."
          />
        </div>
      </div>

      <!-- Bảng danh sách sản phẩm -->
      <div class="table-responsive">
        <table class="products-table-styled">
          <thead>
          <tr>
            <th class="col-stt text-center" style="width: 50px;">STT</th>
            <th class="col-code text-left" style="width: 120px;">Mã SP</th>
            <th class="col-name text-left">Tên Sản Phẩm</th>
            <th class="col-variant text-center" style="width: 150px;">Phân Loại</th>
            <th class="col-qty text-center" style="width: 90px;">Số Lượng</th>
            <th class="col-price text-right" style="width: 130px;">Đơn Giá</th>
            <th class="col-total text-right" style="width: 140px;">Thành Tiền</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="filteredProductItems.length === 0">
            <td colspan="7" class="text-center text-muted" style="padding: 24px;">Không có sản phẩm nào phù hợp với bộ lọc.</td>
          </tr>
          <tr v-for="(item, index) in filteredProductItems" :key="item.id" class="product-item-row">
            <td class="text-center text-muted">{{ index + 1 }}</td>
            <td class="text-left font-bold text-code">
              <div>{{ item.variantCode || item.code }}</div>
              <div v-if="item.variantCode && item.code && item.variantCode !== item.code" class="product-parent-code font-mono">({{ item.code }})</div>
            </td>
            <td class="text-left font-medium">
              <div class="product-name-heading">{{ item.name }}</div>
            </td>
            <td class="text-center">
              <div class="variant-wrapper">
                <span class="variant-tag color-tag">
                  <span class="color-dot-indicator" :style="{ backgroundColor: item.colorHex || '#2b353b' }"></span>
                  {{ item.color }}
                </span>
                <span class="variant-tag size-tag">Size: {{ item.size }}</span>
              </div>
            </td>
            <td class="text-center">{{ item.quantity }}</td>
            <td class="text-right">{{ formatMoney(item.price) }}</td>
            <td class="text-right font-bold text-price-total">{{ formatMoney(item.total) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal Popup Lịch Sử Thao Tác -->
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
                  <span class="log-time-text">{{ formatDateTime(log.thoiGian) }}</span>
                </div>
                <div class="log-sub-info">
                  <span>Người thực hiện: <strong>{{ getLogActor(log) }}</strong></span>
                  <span v-if="log.ghiChu"> — {{ log.ghiChu }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Xác Nhận Thao Tác -->
    <transition name="modal-fade">
      <div v-if="confirmModal.show" class="modal-backdrop" @click.self="closeConfirmModal">
        <div class="modal-container modal-confirm-box">
          <div class="modal-header confirm-header">
            <div class="confirm-title-group">
              <span class="confirm-icon-badge" :class="confirmModal.type">
                <svg v-if="confirmModal.type === 'danger'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <path d="M12 16v-4"></path>
                  <path d="M12 8h.01"></path>
                </svg>
              </span>
              <h3 class="card-title">{{ confirmModal.title }}</h3>
            </div>
            <button class="btn-close-modal" type="button" @click="closeConfirmModal">✕</button>
          </div>

          <div class="modal-body confirm-body">
            <p class="confirm-main-message">{{ confirmModal.message }}</p>
            <p v-if="confirmModal.subText" class="confirm-sub-message">{{ confirmModal.subText }}</p>

            <!-- Nhập email khách hàng khi xác nhận đơn hàng -->
            <div v-if="confirmModal.showEmailInput" class="confirm-email-box">
              <label class="confirm-checkbox-label">
                <input type="checkbox" v-model="confirmModal.sendEmail" />
                <span class="checkbox-text font-bold">✉ Gửi email thông báo xác nhận đơn hàng cho khách</span>
              </label>
              <div v-if="confirmModal.sendEmail" class="email-input-subgroup">
                <label class="modal-label">Địa chỉ email nhận thông báo <span class="text-danger">*</span></label>
                <input
                    type="email"
                    v-model="confirmModal.customerEmail"
                    placeholder="Nhập email khách hàng (VD: khachhang@gmail.com)..."
                    class="modal-input-field"
                    maxlength="100"
                />
                <p class="email-guide-text">Hệ thống sẽ tự động gửi email xác nhận chi tiết đơn hàng cho khách hàng qua địa chỉ email này.</p>
              </div>
            </div>

            <!-- Lựa chọn in PDF khi xác nhận -->
            <div v-if="confirmModal.showPrintPdfOption" class="confirm-print-option-box">
              <label class="confirm-checkbox-label">
                <input type="checkbox" v-model="confirmModal.printPdf" />
                <span class="checkbox-text font-bold">📄 In hóa đơn (PDF) ngay sau khi xác nhận thành công</span>
              </label>
              <p class="checkbox-subtext">Hệ thống sẽ tự động mở hộp thoại in / xuất file PDF hóa đơn chuẩn A4.</p>
            </div>

            <!-- Nhập lý do (Hủy đơn hoặc Giao hàng thất bại) -->
            <div v-if="confirmModal.showReasonInput" class="confirm-reason-box">
              <label class="modal-label">Lý do chi tiết <span class="text-danger">*</span></label>
              <textarea v-model="confirmModal.reason" rows="2" class="modal-textarea-field" :placeholder="confirmModal.reasonPlaceholder"></textarea>
            </div>
          </div>

          <div class="modal-footer confirm-footer">
            <button class="btn-modal-cancel" type="button" @click="closeConfirmModal">{{ confirmModal.cancelText }}</button>
            <button class="btn-confirm-accept" :class="confirmModal.type" type="button" :disabled="updatingStatus" @click="handleConfirmAccept">
              {{ updatingStatus ? 'Đang xử lý...' : (
                confirmModal.showEmailInput && confirmModal.sendEmail && confirmModal.customerEmail
                    ? (confirmModal.showPrintPdfOption && confirmModal.printPdf ? '✓ Xác nhận, Gửi mail & In PDF' : '✓ Xác nhận & Gửi mail')
                    : (confirmModal.showPrintPdfOption && confirmModal.printPdf ? '✓ Xác nhận & In PDF' : confirmModal.confirmText)
            ) }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MẪU IN HÓA ĐƠN CHUẨN A4 / POS -->
    <div class="printable-invoice">
      <!-- HEADER CỬA HÀNG & TIÊU ĐỀ HÓA ĐƠN -->
      <div class="print-header-top">
        <div class="print-store-brand">
          <div class="print-brand-badge-row">
            <span class="print-logo-icon">FF</span>
            <span class="print-brand-name">FF T-SHIRT STORE</span>
          </div>
          <div class="print-store-meta">
            <span>Hotline: 0988.888.888 &nbsp;•&nbsp; Website: fftshirt.vn</span>
          </div>
        </div>

        <div class="print-invoice-title-block">
          <h1 class="print-main-title">HÓA ĐƠN BÁN HÀNG</h1>
          <div class="print-meta-code">Mã HĐ: <strong>{{ invoiceData.code }}</strong></div>
          <div class="print-meta-date">Ngày lập: {{ invoiceData.paymentHistory?.time || (invoiceData.createTime + ' ' + invoiceData.createDate) }}</div>
        </div>
      </div>

      <div class="print-divider"></div>

      <!-- KHỐI THÔNG TIN KHÁCH HÀNG & GIAO DỊCH -->
      <div class="print-info-grid">
        <div class="print-info-box">
          <div class="print-box-title">THÔNG TIN KHÁCH HÀNG</div>
          <div class="print-data-line">
            <span class="p-label">Khách hàng:</span>
            <span class="p-val font-bold">{{ invoiceData.customer.name }}</span>
          </div>
          <div class="print-data-line" v-if="invoiceData.customer.phone && invoiceData.customer.phone !== '—' && invoiceData.customer.phone !== '---' && !invoiceData.customer.phone.includes('Mua tại quầy')">
            <span class="p-label">Điện thoại:</span>
            <span class="p-val">{{ invoiceData.customer.phone }}</span>
          </div>
          <div class="print-data-line" v-if="invoiceData.delivery.address && invoiceData.delivery.type !== 'Tại quầy' && invoiceData.delivery.type !== 'Tại cửa hàng'">
            <span class="p-label">Địa chỉ nhận:</span>
            <span class="p-val">{{ invoiceData.delivery.address }}</span>
          </div>
        </div>

        <div class="print-info-box">
          <div class="print-box-title">THÔNG TIN ĐƠN HÀNG</div>
          <div class="print-data-line">
            <span class="p-label">Hình thức:</span>
            <span class="p-val font-bold">{{ invoiceData.delivery.type || 'Tại quầy' }}</span>
          </div>
          <div class="print-data-line">
            <span class="p-label">Thu ngân:</span>
            <span class="p-val">{{ invoiceData.employee?.name || 'Nhân viên bán hàng' }}</span>
          </div>
          <div class="print-data-line" v-if="invoiceData.delivery.note && invoiceData.delivery.note !== '—' && invoiceData.delivery.note !== '---'">
            <span class="p-label">Ghi chú:</span>
            <span class="p-val italic">{{ invoiceData.delivery.note }}</span>
          </div>
        </div>
      </div>

      <!-- BẢNG CHI TIẾT SẢN PHẨM -->
      <table class="print-table">
        <thead>
        <tr>
          <th style="width: 45px;" class="text-center">STT</th>
          <th style="width: 105px;" class="text-left">Mã SP</th>
          <th class="text-left">Tên sản phẩm</th>
          <th style="width: 125px;" class="text-center">Phân loại</th>
          <th style="width: 50px;" class="text-center">SL</th>
          <th style="width: 120px;" class="text-right">Đơn giá</th>
          <th style="width: 130px;" class="text-right">Thành tiền</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(it, idx) in invoiceData.items" :key="it.id || idx">
          <td class="text-center">{{ idx + 1 }}</td>
          <td class="text-left">
            <span class="font-mono font-bold">{{ it.variantCode || it.code }}</span>
            <span v-if="it.variantCode && it.code && it.variantCode !== it.code" class="print-sub-code font-mono"> ({{ it.code }})</span>
          </td>
          <td class="text-left font-medium">{{ it.name }}</td>
          <td class="text-center">{{ it.color }} / {{ it.size }}</td>
          <td class="text-center font-bold">{{ it.quantity }}</td>
          <td class="text-right">{{ formatMoney(it.price) }}</td>
          <td class="text-right font-bold">{{ formatMoney(it.total) }}</td>
        </tr>
        </tbody>
      </table>

      <!-- TỔNG KẾT & THANH TOÁN -->
      <div class="print-bottom-summary">
        <div class="print-payment-info">
          <div class="print-box-title">PHƯƠNG THỨC THANH TOÁN</div>
          <div class="print-payment-layout">
            <div class="print-payment-details">
              <div class="print-pay-method-name">{{ invoiceData.paymentHistory.method }}</div>
              <div class="print-pay-timestamp" v-if="invoiceData.paymentHistory.time">{{ invoiceData.paymentHistory.time }}</div>

              <div class="print-pay-line-row">
                <span class="print-pay-txt">Số tiền</span>
                <span class="print-pay-val-bold">{{ formatMoney(invoiceData.paymentHistory.amount || invoiceData.summary.totalPayment) }}</span>
              </div>

              <!-- Chi tiết các khoản thanh toán con (Chuyển khoản / Tiền mặt) giống hệt ảnh mẫu -->
              <template v-if="printSubTransactions.length > 0">
                <div class="print-pay-dashed-line"></div>
                <div v-for="t in printSubTransactions" :key="t.id || t.label" class="print-pay-subitem-row">
                  <span class="print-pay-subitem-label">{{ t.label }}:</span>
                  <span class="print-pay-subitem-val">{{ formatMoney(t.amount) }}</span>
                </div>
              </template>
            </div>
            <!-- Bỏ mã QR ở phần in hóa đơn -->
            <div class="print-payment-qr" v-if="false">
              <img
                  :src="invoiceQrDataUrl"
                  alt="Mã QR Hóa Đơn"
                  class="print-qr-img"
              />
            </div>
          </div>
        </div>

        <div class="print-calc-block">
          <div class="calc-row">
            <span>Tổng tiền hàng:</span>
            <span class="font-bold">{{ formatMoney(invoiceData.summary.totalProductPrice) }}</span>
          </div>
          <div v-if="invoiceData.summary.voucherDiscount > 0" class="calc-row discount-row">
            <span>Giảm giá ({{ voucherDisplayName || invoiceData.voucher?.code || 'Phiếu giảm giá' }}):</span>
            <span class="font-bold text-discount">-{{ formatMoney(invoiceData.summary.voucherDiscount) }}</span>
          </div>
          <div v-if="invoiceData.summary.shippingFee > 0" class="calc-row">
            <span>Phí vận chuyển:</span>
            <span>+{{ formatMoney(invoiceData.summary.shippingFee) }}</span>
          </div>
          <div class="calc-divider"></div>
          <div class="calc-row total-row">
            <span>TỔNG CỘNG:</span>
            <span class="total-money">{{ formatMoney(invoiceData.summary.totalPayment) }}</span>
          </div>
        </div>
      </div>

      <!-- KHỐI CHỮ KÝ XÁC NHẬN -->
      <div class="print-signatures-row">
        <div class="signature-col">
          <span class="sig-title">Người mua hàng</span>
          <span class="sig-sub">(Ký, ghi rõ họ tên)</span>
          <div class="sig-space"></div>
        </div>
        <div class="signature-col">
          <span class="sig-title">Người bán hàng / Thu ngân</span>
          <span class="sig-sub">(Ký, đóng dấu nếu có)</span>
          <div class="sig-space"></div>
          <div class="sig-name" v-if="invoiceData.employee?.name">{{ invoiceData.employee?.name }}</div>
        </div>
      </div>

      <!-- LỜI CẢM ƠN & CHÍNH SÁCH ĐỔI TRẢ -->
      <div class="print-footer-notes">
        <p class="thank-you-msg">♥ Cảm ơn Quý khách đã mua sắm tại FF T-Shirt Store! ♥</p>
        <p class="policy-note">Quý khách vui lòng kiểm tra kỹ hàng hóa và hóa đơn trước khi rời quầy. Hỗ trợ đổi hàng trong 7 ngày với sản phẩm còn nguyên tem mác.</p>
      </div>
    </div>

    <!-- Modal Sửa Trạng Thái Thanh Toán -->
    <transition name="modal-fade">
      <div v-if="showPaymentModal" class="modal-backdrop" @click.self="showPaymentModal = false">
        <div class="modal-container modal-payment-box">
          <div class="modal-header">
            <div class="header-title-box">
              <h3 class="card-title">Cập nhật trạng thái thanh toán</h3>
              <p class="header-sub-text">Hóa đơn: <strong>{{ invoiceData.code }}</strong></p>
            </div>
            <button class="btn-close-modal" type="button" @click="showPaymentModal = false">✕</button>
          </div>

          <div class="modal-body">
            <!-- Chọn trạng thái thanh toán -->
            <div class="form-group-modal">
              <label class="modal-label">Trạng thái thanh toán <span class="text-danger">*</span></label>
              <div class="payment-status-radio-group">
                <label class="payment-radio-option" :class="{ 'selected': Number(paymentForm.trangThaiThanhToan) === 1 }">
                  <input type="radio" v-model="paymentForm.trangThaiThanhToan" :value="1" />
                  <span class="radio-pill-circle green">✓</span>
                  <div class="radio-label-info">
                    <span class="radio-main-title font-bold text-success">Đã thanh toán</span>
                    <span class="radio-sub-desc">Đã thu đủ tiền từ khách hàng</span>
                  </div>
                </label>

                <label class="payment-radio-option" :class="{ 'selected': Number(paymentForm.trangThaiThanhToan) === 0 }">
                  <input type="radio" v-model="paymentForm.trangThaiThanhToan" :value="0" />
                  <span class="radio-pill-circle orange">⏳</span>
                  <div class="radio-label-info">
                    <span class="radio-main-title font-bold text-warning">Chưa thanh toán</span>
                    <span class="radio-sub-desc">Đang công nợ / Chờ khách thanh toán</span>
                  </div>
                </label>
              </div>
            </div>

            <!-- Phương thức thanh toán -->
            <div class="form-group-modal mt-14">
              <label class="modal-label">Phương thức thanh toán <span class="text-danger">*</span></label>
              <select v-model="paymentForm.phuongThuc" class="modal-select-field">
                <option value="Tiền mặt">Tiền mặt</option>
                <option value="Chuyển khoản ngân hàng">Chuyển khoản ngân hàng</option>
                <option value="Quẹt thẻ POS">Quẹt thẻ POS</option>
                <option value="VNPay">Cổng VNPay</option>
                <option value="MoMo">Ví MoMo</option>
              </select>
            </div>

            <!-- Mã giao dịch -->
            <div class="form-group-modal mt-14">
              <label class="modal-label">Mã giao dịch / Mã tham chiếu <span class="text-optional">(tùy chọn)</span></label>
              <input type="text" v-model="paymentForm.maGiaoDich" placeholder="VD: POS-00921, VNPAY123456..." class="modal-input-field" maxlength="100" />
            </div>

            <!-- Ghi chú thanh toán -->
            <div class="form-group-modal mt-14">
              <label class="modal-label">Ghi chú thanh toán <span class="text-optional">(tùy chọn)</span></label>
              <textarea v-model="paymentForm.ghiChu" rows="2" placeholder="Nhập ghi chú thanh toán..." class="modal-textarea-field" maxlength="255"></textarea>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-modal-cancel" type="button" @click="showPaymentModal = false">Hủy</button>
            <button class="btn-modal-save" type="button" :disabled="updatingPaymentStatus" @click="savePaymentStatusChange">
              {{ updatingPaymentStatus ? 'Đang lưu...' : '✓ Cập nhật thanh toán' }}
            </button>
          </div>
        </div>
      </div>
    </transition>



  </div>
</template>

<style scoped>
.printable-invoice {
  display: none;
}

.chi-tiet-page {
  width: 100%;
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  box-sizing: border-box;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #2b353b;
}

@media (max-width: 768px) {
  .chi-tiet-page {
    padding: 1rem 1rem 2rem;
  }
}

/* Toast Notifications */
.toast-popup {
  position: fixed;
  top: 24px;
  right: 28px;
  background-color: var(--blue, #496883);
  color: #ffffff;
  padding: 10px 18px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.95rem;
  font-weight: 600;
  z-index: 9999;
}

.toast-popup.warning {
  background-color: #d97706;
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

/* Top Navigation Bar */
.top-nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
}

.crumb-parent {
  color: #647074;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s ease;
}

.crumb-parent:hover {
  color: var(--blue, #496883);
  text-decoration: underline;
}

.crumb-separator {
  color: #b0b7bd;
}

.crumb-current {
  color: var(--blue, #496883);
  font-weight: 700;
}

.top-actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-top-nav {
  background: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--blue, #496883);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
  user-select: none;
}

.btn-top-nav:hover {
  background: #f7f5ef;
  border-color: #3e5c76;
  color: #243c50;
}

.nav-arrow {
  font-size: 0.95rem;
}

/* Meta Info Card */
.meta-info-card {
  margin-bottom: 16px;
  padding: 12px 20px;
  background: #ffffff;
  border: 1px solid #ebd9c8;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.meta-info-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr 1fr;
  align-items: center;
  gap: 0;
}

@media (max-width: 900px) {
  .meta-info-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}

.meta-info-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 2px 24px;
}

.meta-info-col.col-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 16px 0 24px;
}

.meta-qr-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 4px;
  cursor: pointer;
}

.meta-qr-img {
  width: 66px;
  height: 66px;
  border: 1px solid #d5c8b8;
  border-radius: 6px;
  padding: 2px;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.2s ease;
}

.meta-qr-img:hover {
  transform: scale(2.2);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.22);
  z-index: 100;
  position: relative;
  border-color: #3e5c76;
}

.meta-qr-img:active {
  transform: scale(1.95);
}

.meta-qr-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: #647074;
}

.meta-info-col:not(:last-child) {
  border-right: 1px solid #ebd9c8;
}

.meta-info-col:first-child {
  padding-left: 4px;
}

.meta-info-col:last-child {
  padding-right: 4px;
}

@media (max-width: 900px) {
  .meta-info-col {
    padding: 0;
    border-right: none !important;
    border-bottom: 1px solid #ebd9c8;
    padding-bottom: 8px;
  }
  .meta-info-col:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.meta-row {
  display: flex;
  align-items: center;
  font-size: 0.95rem;
  line-height: 1.4;
}

.meta-info-col.col-1 .meta-label {
  width: 135px;
  flex-shrink: 0;
}

.meta-info-col.col-2 .meta-label {
  width: 75px;
  flex-shrink: 0;
}

.meta-info-col.col-3 .meta-label {
  width: 110px;
  flex-shrink: 0;
}

.meta-label {
  color: #78858f;
  font-size: 0.82rem;
  font-weight: 500;
}

.meta-value {
  color: #2b353b;
  font-weight: 600;
  font-size: 0.85rem;
}

.meta-code-blue {
  color: var(--blue, #496883);
  font-weight: 700;
  font-size: 0.92rem;
  letter-spacing: 0.3px;
}

.badge-loai-don {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
  background: #f4efe9;
  color: var(--blue, #496883);
  border: 1px solid #e2d8cd;
}

.badge-unprinted-pill {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  background-color: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
  margin-left: 6px;
  display: inline-flex;
  align-items: center;
}

/* Main Layout Grid */
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
  color: var(--blue, #496883);
}

.title-icon {
  color: var(--blue, #496883);
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
.badge-status-7 { background-color: #fef2f2; color: #dc2626; border-color: #fecaca; }

/* ==================================================== */
/* Stepper Timeline: Hiện các bước đã tới + bước tiếp theo */
/* ==================================================== */
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
}

.stepper-step-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-width: 110px;
}

/* Vạch nối */
.step-connector-line {
  position: absolute;
  top: 19px;
  right: 50%;
  width: calc(100% + 50px);
  height: 2px;
  background-color: #dbe2e8; /* Màu xám mờ dẫn tới bước tiếp theo */
  z-index: 1;
}

.step-connector-line.line-active {
  background-color: var(--blue, #496883) !important;
}

.step-connector-line.line-failed {
  background-color: #dc2626 !important;
}

.step-connector-line.line-cancelled {
  background-color: #ef4444 !important;
}

/* Nốt tròn */
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

.step-circle-icon.clickable {
  cursor: pointer;
}

.step-circle-icon.clickable:hover {
  transform: scale(1.08);
  border-color: var(--blue, #496883);
}

/* 1. Bước đã xong */
.circle-done {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border: 2px solid var(--blue, #496883);
}

/* 2. Bước hiện tại */
.circle-active {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 4px rgba(73, 104, 131, 0.25);
}

/* 3. Bước tiếp theo (Pending) */
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

.circle-failed {
  background-color: #dc2626;
  color: #ffffff;
  border: 2px solid #dc2626;
  box-shadow: 0 0 0 4px #fee2e2;
}

.step-item-title {
  font-size: 0.83rem;
  font-weight: 600;
  margin-bottom: 2px;
}

.step-item-title.title-done,
.step-item-title.title-active {
  color: var(--blue, #496883);
  font-weight: 700;
}

.step-item-title.title-pending {
  color: #8c969e;
}

.step-item-time {
  font-size: 0.72rem;
  color: #9aa4ac;
}

/* Thanh footer chứa nút */
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

.left-action-buttons {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.right-action-buttons {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.btn-history-outline {
  background: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.83rem;
  font-weight: 600;
  color: var(--blue, #496883);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
}

.btn-history-outline:hover {
  background: #f7f5ef;
  border-color: var(--blue, #496883);
  color: #243c50;
}

.btn-print-outline {
  background: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.83rem;
  font-weight: 600;
  color: var(--blue, #496883);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
}

.btn-print-outline:hover {
  background: #f7f5ef;
  border-color: var(--blue, #496883);
  color: #243c50;
}

.btn-next-action {
  background: var(--blue, #496883);
  color: #ffffff;
  border: 1.5px solid var(--blue, #496883);
  border-radius: 8px;
  padding: 7px 16px;
  font-size: 0.83rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
  transition: all 0.2s ease;
}

.btn-next-action:hover:not(:disabled) {
  background: #2b4257;
  border-color: #243742;
  transform: translateY(-1px);
}

.btn-next-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-next-action.btn-confirm-highlight {
  background: #16a34a !important;
  border-color: #15803d !important;
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.3) !important;
}

.btn-next-action.btn-confirm-highlight:hover:not(:disabled) {
  background: #15803d !important;
  border-color: #166534 !important;
}

.btn-failed-action {
  background: #fff5f5;
  color: #dc2626;
  border: 1.5px solid #fecaca;
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

.btn-failed-action:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #dc2626;
}

.btn-retry-action {
  background: #eff6ff;
  color: #2563eb;
  border: 1.5px solid #bfdbfe;
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

.btn-retry-action:hover:not(:disabled) {
  background: #dbeafe;
  border-color: #2563eb;
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

.btn-cancel-action:hover:not(:disabled) {
  background: #fee2e2;
  border-color: #dc2626;
}

.badge-retry-limit-exceeded {
  background: #fef2f2;
  color: #991b1b;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  border: 1.5px solid #fecaca;
  display: inline-flex;
  align-items: center;
}

.retry-delivery-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: #fff8f8;
  border: 1.5px solid #fecaca;
  border-radius: 8px;
  padding: 12px 16px;
  margin: 10px 20px 6px;
  transition: all 0.2s ease;
}

.retry-delivery-banner.banner-limit-reached {
  background: #fef2f2;
  border-color: #f87171;
}

.retry-delivery-banner .banner-icon {
  color: #dc2626;
  flex-shrink: 0;
  margin-top: 1px;
}

.retry-delivery-banner .banner-body {
  font-size: 0.83rem;
  color: #4b5563;
  line-height: 1.45;
}

.retry-delivery-banner .banner-title {
  color: #991b1b;
  font-size: 0.85rem;
  margin-bottom: 3px;
}

.retry-delivery-banner .banner-pill {
  background: #fee2e2;
  color: #991b1b;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.76rem;
  font-weight: 700;
}

.btn-cancel-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

/* Right Section Summary & Payment */
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
  color: var(--blue, #496883);
}

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
}

.pill-dot {
  font-size: 0.5rem;
}

.pay-method-title {
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--blue, #496883);
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
  color: var(--blue, #496883);
}

.pay-note-sub {
  font-size: 0.76rem;
  color: #647074;
}

.payment-actions-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-blue-action {
  background: #3e5c76;
  color: #ffffff;
  border: 1px solid #3e5c76;
  border-radius: 8px;
  height: 38px;
  font-size: 0.86rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(62, 92, 118, 0.25);
  transition: all 0.15s ease;
}

.btn-blue-action:hover:not(:disabled) {
  background: #2b4257;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(62, 92, 118, 0.35);
  transform: translateY(-1px);
}

.btn-blue-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-pay-confirm-action {
  background: #16a34a;
  color: #ffffff;
  border: 1px solid #15803d;
  border-radius: 8px;
  height: 38px;
  font-size: 0.86rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(22, 163, 74, 0.25);
  transition: all 0.15s ease;
}

.btn-pay-confirm-action:hover:not(:disabled) {
  background: #15803d;
  border-color: #166534;
  box-shadow: 0 4px 10px rgba(22, 163, 74, 0.35);
  transform: translateY(-1px);
}

.btn-pay-confirm-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.unprinted-invoice-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  background-color: #f8fafc;
  border: 1.5px dashed #cbd5e1;
  border-radius: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  text-align: center;
}

.unprinted-icon {
  color: #d97706;
  font-weight: bold;
}

/* Products Card */
.products-card {
  padding: 20px;
}

.product-count-sub {
  font-size: 0.82rem;
  color: #647074;
  font-weight: 600;
}

.product-filters-toolbar {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
}

@media (max-width: 900px) {
  .product-filters-toolbar {
    grid-template-columns: 1fr 1fr;
  }
  .toolbar-item.search-item {
    grid-column: span 2;
  }
}

.toolbar-item {
  width: 100%;
}

.search-input-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: #8c969e;
  pointer-events: none;
}

.form-control-tb {
  height: 38px;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 0.95rem;
  color: #2b353b;
  background: #ffffff;
  outline: none;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.15s ease;
}

.form-control-tb:focus {
  border-color: #3e5c76;
  box-shadow: 0 0 0 2px rgba(62, 92, 118, 0.15);
}

.input-search {
  padding-left: 32px;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.products-table-styled {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.products-table-styled thead th {
  background-color: #f7f5ef;
  border-bottom: 2px solid #e9e5db;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--blue, #496883);
  font-weight: 700;
  padding: 11px 12px;
  font-size: 0.82rem;
  white-space: nowrap;
}

.products-table-styled tbody td {
  padding: 12px;
  border-bottom: 1px solid #f1e7dc;
  color: #333333;
  vertical-align: middle;
}

.product-item-row:hover {
  background: #fcfaf7;
}

.text-code {
  color: var(--blue, #496883);
  font-weight: 700;
}

.product-parent-code {
  color: #78858f;
  font-size: 0.74rem;
  font-weight: normal;
  margin-top: 2px;
}

.product-name-heading {
  font-weight: 600;
  color: #2b353b;
  margin-bottom: 2px;
}

.product-sub-variant {
  font-size: 0.74rem;
  color: #647074;
}

.variant-wrapper {
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.variant-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #f7f5ef;
  border: 1px solid #ebd9c8;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.74rem;
  color: var(--blue, #496883);
}

.color-dot-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  border: 1px solid rgba(0, 0, 0, 0.15);
}

.text-price-total {
  color: var(--blue, #496883);
  font-weight: 700;
}

.font-bold { font-weight: 700; }
.font-medium { font-weight: 500; }
.text-center { text-align: center; }
.text-left { text-align: left; }
.text-right { text-align: right; }
.text-muted { color: #8c969e !important; }
.text-danger { color: #dc2626 !important; }

/* Modals */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(43, 53, 59, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(2px);
}

.modal-container {
  background: #ffffff;
  width: 600px;
  max-width: 90vw;
  max-height: 85vh;
  border-radius: 10px;
  border: 1px solid #ebd9c8;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebd9c8;
  background: #fcfaf7;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: #647074;
}

.btn-close-modal:hover {
  color: var(--blue, #496883);
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--blue, #496883);
  margin: 0;
}

/* Timeline */
.logs-timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-timeline-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.log-dot-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: var(--blue, #496883);
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

.log-action-text { font-size: 0.85rem; color: #2b353b; }
.log-time-text { font-size: 0.75rem; color: #8c969e; }
.log-sub-info { font-size: 0.78rem; color: #647074; }

.modal-input-field {
  width: 100%;
  height: 38px;
  border: 1px solid #d5c8b8;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 0.95rem;
  box-sizing: border-box;
  outline: none;
}

.modal-textarea-field {
  width: 100%;
  border: 1px solid #d5c8b8;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 0.95rem;
  box-sizing: border-box;
  resize: vertical;
  outline: none;
}

.form-group-modal {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mt-14 {
  margin-top: 14px;
}

.modal-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #334155;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #ebd9c8;
  background: #fcfaf7;
}

.btn-modal-cancel {
  height: 36px;
  padding: 0 16px;
  background: #ffffff;
  color: #647074;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-modal-save {
  height: 36px;
  padding: 0 18px;
  background: #3e5c76;
  color: #ffffff;
  border: 1px solid #3e5c76;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-modal-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Confirm Modal */
.modal-confirm-box {
  max-width: 480px;
  width: 90%;
  border-radius: 12px;
  animation: modalPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPop {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.confirm-header {
  padding: 16px 20px;
  border-bottom: 1px solid #ebd9c8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fcfaf7;
}

.confirm-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.confirm-icon-badge {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.confirm-icon-badge.primary { background: #eaf1f4; color: #3e5c76; }
.confirm-icon-badge.danger { background: #fee2e2; color: #dc2626; }

.confirm-body {
  padding: 20px;
}

.confirm-main-message {
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0 0 6px 0;
}

.confirm-sub-message {
  font-size: 0.82rem;
  color: #647074;
  margin: 0;
}

.confirm-footer {
  padding: 14px 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid #ebd9c8;
  background: #fcfaf7;
}

.btn-confirm-accept {
  height: 36px;
  padding: 0 18px;
  border-radius: 8px;
  border: none;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  color: #ffffff;
}

.btn-confirm-accept.primary { background-color: #3e5c76; }
.btn-confirm-accept.danger { background-color: #dc2626; }

/* Guest Customer & Edit Field Extras */
.badge-guest-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 7px;
  background: #f4efe9;
  color: #78858f;
  border: 1px solid #e2d8cd;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  vertical-align: middle;
}

.guest-toggle-row {
  background: #fbfaf7;
  border: 1px dashed #d5c8b8;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.checkbox-custom-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  color: #2b353b;
  cursor: pointer;
}

.guest-note-text {
  font-size: 0.76rem;
  color: #78858f;
  margin-left: 24px;
}

.input-disabled {
  background-color: #f7f5ef !important;
  color: #8c969e !important;
  cursor: not-allowed;
  border-color: #e5dec3 !important;
}

.field-error-text {
  color: #dc2626;
  font-size: 0.76rem;
  margin-top: 2px;
}

/* Confirm Modal Options */
.confirm-print-option-box {
  margin-top: 14px;
  padding: 12px 14px;
  background: #f0f7fb;
  border: 1.5px solid #bae6fd;
  border-radius: 8px;
}

.confirm-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.86rem;
  color: #0369a1;
  cursor: pointer;
}

.checkbox-subtext {
  display: block;
  margin: 4px 0 0 24px;
  font-size: 0.76rem;
  color: #64748b;
}

.confirm-reason-box {
  margin-top: 14px;
}

.confirm-email-box {
  margin-top: 14px;
  padding: 12px 14px;
  background: #fbf7f4;
  border: 1.5px solid #ebd9c8;
  border-radius: 8px;
}

.confirm-email-box .confirm-checkbox-label {
  color: #7b5943;
}

.email-input-subgroup {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.email-guide-text {
  font-size: 0.76rem;
  color: #8c7664;
  margin: 2px 0 0;
  line-height: 1.4;
}

.email-val-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-send-email-pill {
  background: #fdf8f4;
  border: 1px solid #d5c8b8;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #7b5943;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.btn-send-email-pill:hover:not(:disabled) {
  background: #7b5943;
  color: #ffffff;
  border-color: #7b5943;
}

.btn-send-email-pill:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Badge trạng thái voucher trong Header Tổng kết thanh toán */
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

.inline-percent-pill {
  font-weight: 800;
  font-size: 0.74rem;
  background-color: #ecfdf5;
  color: #059669;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid #a7f3d0;
}

/* Modal Sửa Trạng Thái Thanh Toán */
.modal-payment-box {
  max-width: 460px;
  width: 95%;
}

.payment-status-radio-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 6px;
}

.payment-radio-option {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.payment-radio-option input[type="radio"] {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.payment-radio-option:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.payment-radio-option.selected {
  background: #ffffff;
  border-color: #3e5c76;
  box-shadow: 0 0 0 2px rgba(62, 92, 118, 0.15);
}

.radio-pill-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 800;
  flex-shrink: 0;
}

.radio-pill-circle.green {
  background: #dcfce7;
  color: #15803d;
}

.radio-pill-circle.orange {
  background: #fef3c7;
  color: #b45309;
}

.radio-label-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.radio-main-title {
  font-size: 0.86rem;
}

.radio-sub-desc {
  font-size: 0.72rem;
  color: #64748b;
}

.text-warning {
  color: #b45309;
}

.print-payment-layout {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.print-payment-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.print-pay-method-name {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1e3a5f;
  margin-bottom: 2px;
}

.print-pay-timestamp {
  font-size: 0.82rem;
  color: #64748b;
  margin-bottom: 6px;
}

.print-pay-line-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.95rem;
  color: #475569;
  margin-bottom: 2px;
}

.print-pay-txt {
  color: #475569;
}

.print-pay-val-bold {
  font-weight: 700;
  color: #0f172a;
  font-size: 0.92rem;
}

.print-pay-dashed-line {
  border-top: 1px dashed #cbd5e1;
  margin: 6px 0;
  width: 100%;
}

.print-pay-subitem-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.95rem;
  color: #475569;
  margin-bottom: 3px;
}

.print-pay-subitem-label {
  color: #475569;
}

.print-pay-subitem-val {
  font-weight: 700;
  color: #0f172a;
}

.print-payment-qr { display: none !important;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.print-qr-img {
  width: 68px;
  height: 68px;
  object-fit: contain;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  padding: 2px;
  background: #ffffff;
}

/* Printable Invoice Styles & Media Print */
.printable-invoice {
  display: none;
}

@media print {
  @page {
    size: A4 portrait;
    margin: 8mm 12mm 10mm 12mm;
  }

  /* Reset trang chi tiết để hóa đơn chiếm toàn bộ khổ giấy in */
  .chi-tiet-page {
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    background: #ffffff !important;
    min-height: auto !important;
    position: static !important;
  }

  /* Ẩn triệt để toàn bộ các khối chức năng quản trị */
  .toast-popup,
  .top-nav-bar,
  .meta-info-card,
  .card-order-status,
  .main-layout-grid,
  .info-dual-grid,
  .products-card,
  .modal-backdrop,
  .sidebar,
  .top-header,
  header,
  .chi-tiet-page > *:not(.printable-invoice) {
    display: none !important;
    height: 0 !important;
    width: 0 !important;
    margin: 0 !important;
    padding: 0 !important;
    overflow: hidden !important;
  }

  .unused-dummy-hide {
    display: flex;
    visibility: hidden !important;
  }

  /* Khung hóa đơn in */
  .printable-invoice {
    display: block !important;
    position: static !important;
    width: 100% !important;
    max-width: 100% !important;
    margin: 0 auto !important;
    padding: 4px 0 !important;
    background: #ffffff !important;
    color: #111827 !important;
    font-size: 13px !important;
    box-sizing: border-box !important;
  }

  .print-header-top {
    margin-bottom: 8px !important;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    /* compact header */
  }

  .print-brand-badge-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
  }

  .print-logo-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    background-color: #3e5c76 !important;
    color: #ffffff !important;
    font-weight: 800;
    font-size: 15px;
    border-radius: 6px;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .print-brand-name {
    font-size: 1.35rem;
    font-weight: 800;
    color: #111827;
    letter-spacing: 0.5px;
  }

  .print-store-meta {
    font-size: 0.82rem;
    color: #4b5563;
    line-height: 1.5;
  }

  .print-invoice-title-block {
    text-align: right;
  }

  .print-main-title {
    font-size: 1.55rem;
    font-weight: 800;
    color: #1e3a5f !important;
    margin: 0 0 4px 0;
    letter-spacing: 0.5px;
  }

  .print-meta-code {
    font-size: 0.95rem;
    color: #1f2937;
  }

  .print-meta-date {
    font-size: 0.82rem;
    color: #4b5563;
    margin-top: 2px;
  }

  .print-divider {
    margin: 8px 0 10px 0 !important;
    height: 2px;
    background-color: #111827 !important;
    margin: 12px 0 16px 0;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .print-info-grid {
    margin-bottom: 10px !important;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 18px;
  }

  .print-info-box {
    padding: 12px 14px;
    background-color: #f9fafb !important;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .print-box-title {
    font-size: 0.78rem;
    font-weight: 700;
    color: #4b5563;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 2px;
    border-bottom: 1px dashed #d1d5db;
    padding-bottom: 4px;
  }

  .print-data-line {
    font-size: 0.85rem;
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  .p-label {
    color: #6b7280;
    width: 95px;
    flex-shrink: 0;
  }

  .p-val {
    color: #111827;
  }

  .p-val.italic {
    font-style: italic;
    color: #4b5563;
  }

  .print-table {
    margin-bottom: 10px !important;
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 18px;
  }

  .print-table th {
    background-color: #f3f4f6 !important;
    color: #111827;
    font-weight: 700;
    border: 1px solid #d1d5db;
    padding: 8px 10px;
    font-size: 0.95rem;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .print-table td {
    border: 1px solid #e5e7eb;
    padding: 8px 10px;
    font-size: 0.95rem;
    color: #1f2937;
  }

  .font-mono {
    font-family: monospace, Courier, sans-serif;
  }

  .print-bottom-summary {
    margin-bottom: 12px !important;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 24px;
    margin-top: 8px;
    margin-bottom: 24px;
  }

  .print-payment-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 14px;
    background: #f9fafb !important;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .print-payment-layout {
    display: flex !important;
    justify-content: space-between !important;
    align-items: flex-start !important;
    gap: 14px !important;
  }

  .print-payment-details {
    display: flex !important;
    flex-direction: column !important;
    gap: 2px !important;
    flex: 1 !important;
  }

  .print-pay-method-name {
    font-size: 1.05rem !important;
    font-weight: 700 !important;
    color: #1e3a5f !important;
    margin-bottom: 2px !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-timestamp {
    font-size: 0.82rem !important;
    color: #64748b !important;
    margin-bottom: 6px !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-line-row {
    display: flex !important;
    justify-content: space-between !important;
    align-items: center !important;
    font-size: 0.88rem !important;
    color: #475569 !important;
    margin-bottom: 2px !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-txt {
    color: #475569 !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-val-bold {
    font-weight: 700 !important;
    color: #0f172a !important;
    font-size: 0.92rem !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-dashed-line {
    border-top: 1px dashed #cbd5e1 !important;
    margin: 6px 0 !important;
    width: 100% !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-subitem-row {
    display: flex !important;
    justify-content: space-between !important;
    align-items: center !important;
    font-size: 0.84rem !important;
    color: #475569 !important;
    margin-bottom: 3px !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-subitem-label {
    color: #475569 !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-pay-subitem-val {
    font-weight: 700 !important;
    color: #0f172a !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .print-payment-qr { display: none !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    flex-shrink: 0 !important;
  }

  .print-payment-qr .print-qr-img {
    width: 68px !important;
    height: 68px !important;
    object-fit: contain !important;
    border: 1px solid #94a3b8 !important;
    border-radius: 4px !important;
    padding: 2px !important;
    background: #ffffff !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  .text-success {
    color: #166534;
  }

  .text-discount {
    color: #b91c1c;
  }

  .print-calc-block {
    width: 310px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 14px;
    background: #f9fafb !important;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .calc-row {
    display: flex;
    justify-content: space-between;
    font-size: 0.86rem;
    color: #374151;
  }

  .calc-divider {
    height: 1px;
    background-color: #d1d5db;
    margin: 4px 0;
  }

  .calc-row.total-row {
    font-size: 1.1rem;
    font-weight: 800;
    color: #111827;
    margin-top: 2px;
  }

  .total-money {
    color: #111827;
  }

  /* Khối chữ ký */
  .print-signatures-row {
    margin-top: 10px !important;
    display: flex;
    justify-content: space-around;
    text-align: center;
    margin-top: 20px;
    page-break-inside: avoid;
  }

  .signature-col {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 200px;
  }

  .sig-title {
    font-weight: 700;
    font-size: 0.92rem;
    color: #111827;
  }

  .sig-sub {
    font-size: 0.78rem;
    font-style: italic;
    color: #6b7280;
    margin-top: 2px;
  }

  .sig-space {
    height: 40px;
  }

  .sig-name {
    font-weight: 600;
    font-size: 0.85rem;
    color: #111827;
  }

  /* Lời cảm ơn và chính sách */
  .print-footer-notes {
    margin-top: 14px;
    text-align: center;
    border-top: 1px dashed #d1d5db;
    padding-top: 14px;
    page-break-inside: avoid;
  }

  .thank-you-msg {
    font-size: 0.92rem;
    font-weight: 700;
    color: #111827;
    margin: 0 0 4px 0;
  }

  .policy-note {
    font-size: 0.78rem;
    color: #6b7280;
    font-style: italic;
    margin: 0;
  }

  .print-sub-code {
    font-size: 0.76rem !important;
    color: #4b5563 !important;
    font-weight: normal !important;
  }
}
</style>