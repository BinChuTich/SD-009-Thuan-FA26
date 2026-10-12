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
    { id: 1, name: 'Hóa đơn chờ', time: '', done: false, active: true, pending: false },
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
  paymentHistory: {
    method: 'Tiền mặt',
    description: 'Thanh toán tại quầy - Tiền mặt',
    time: '',
    status: 'Đã thanh toán',
    amount: 0,
    transCode: ''
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

// Dữ liệu danh mục chuẩn từ cơ sở dữ liệu FF_Tshirt để tra cứu chính xác
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
  3: { code: 'NV003', name: 'Lê Văn Bán Hàng', role: 'Nhân viên bán hàng' },
  'NV001': { code: 'NV001', name: 'Nguyễn Văn Admin', role: 'Quản trị viên' },
  'NV002': { code: 'NV002', name: 'Trần Thị Quản Lý', role: 'Quản lý' },
  'NV003': { code: 'NV003', name: 'Lê Văn Bán Hàng', role: 'Nhân viên bán hàng' },
  'admin': { code: 'NV001', name: 'Nguyễn Văn Admin', role: 'Quản trị viên' },
  'quanly01': { code: 'NV002', name: 'Trần Thị Quản Lý', role: 'Quản lý' },
  'nvbanhang': { code: 'NV003', name: 'Lê Văn Bán Hàng', role: 'Nhân viên bán hàng' }
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
  3: { code: 'KH003', name: 'Lê Văn Cường', phone: '0933333333', email: 'cuong@gmail.com' },
  'KH001': { code: 'KH001', name: 'Nguyễn Văn An', phone: '0911111111', email: 'an@gmail.com' },
  'KH002': { code: 'KH002', name: 'Trần Thị Bình', phone: '0922222222', email: 'binh@gmail.com' },
  'KH003': { code: 'KH003', name: 'Lê Văn Cường', phone: '0933333333', email: 'cuong@gmail.com' },
  'khachhang01': { code: 'KH001', name: 'Nguyễn Văn An', phone: '0911111111', email: 'an@gmail.com' },
  'khachhang02': { code: 'KH002', name: 'Trần Thị Bình', phone: '0922222222', email: 'binh@gmail.com' },
  'khachhang03': { code: 'KH003', name: 'Lê Văn Cường', phone: '0933333333', email: 'cuong@gmail.com' }
}

// Bóc tách dữ liệu sản phẩm chi tiết
const mapInvoiceItem = (item, index) => {
  const raw = item || {}
  // Nhận diện linh hoạt trường liên kết: idChiTietSanPham hoặc chiTietSanPham
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
      : getString(
          color['tenMauSac'],
          variant['tenMauSac'],
          raw['tenMauSac'],
          color['tenMau'],
          matchedCatalog['color'],
          '---'
      )

  const colorHex = getString(color['maHex'], matchedCatalog['hex'], '#2b353b')

  const sizeText = typeof size === 'string'
      ? size
      : getString(
          size['tenKichCo'],
          variant['tenKichCo'],
          raw['tenKichCo'],
          size['ten'],
          matchedCatalog['size'],
          '---'
      )

  const productName = getString(
      product['tenSanPham'],
      variant['tenSanPham'],
      raw['tenSanPham'],
      matchedCatalog['name'],
      product['ten'],
      raw['name'],
      'Áo Phông Nam Basic Cotton'
  )

  const productCode = getString(
      product['maSanPham'],
      variant['maSanPham'],
      raw['maSanPham'],
      matchedCatalog['code'],
      variantCode,
      `SP00${index + 1}`
  )

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

    // 1. Khách hàng: lấy từ bảng khóa ngoại KhachHang và catalog DB
    const khachHangObj = data['idKhachHang'] || data['khachHang'] || {}
    const rawCustomerName = getString(
        khachHangObj['tenKhachHang'],
        data['tenKhachHangHienThi'],
        data['tenKhachHang'],
        data['customerName'],
        khachHangObj['taiKhoan'],
        'Khách lẻ'
    )
    const customerCatalog = DB_CUSTOMER_CATALOG[rawCustomerName] || DB_CUSTOMER_CATALOG[data['idKhachHang']] || DB_CUSTOMER_CATALOG[data['soDienThoaiKhachHang']] || {}

    invoiceData.value.customer = {
      name: rawCustomerName !== 'Khách lẻ' ? rawCustomerName : (customerCatalog['name'] || 'Khách lẻ'),
      phone: getString(
        khachHangObj['soDienThoai'],
        data['soDienThoaiHienThi'],
        data['soDienThoaiKhachHang'],
        data['soDienThoai'],
        customerCatalog['phone'],
        data['sdt'],
        '---'
      ),
      email: getString(
        khachHangObj['email'],
        data['email'],
        customerCatalog['email'],
        '---'
      )
    }

    // 2. Nhân viên: lấy từ bảng khóa ngoại NhanVien và catalog DB
    const nhanVienObj = data['idNhanVien'] || data['nhanVien'] || {}
    const employeeCatalog = DB_EMPLOYEE_CATALOG[data['idNhanVien']] || DB_EMPLOYEE_CATALOG[data['nguoiTao']] || DB_EMPLOYEE_CATALOG[nhanVienObj['maNhanVien']] || {}

    let employeeName = '---'
    let employeeCode = '---'

    if (nhanVienObj['tenNhanVien']) {
      employeeName = nhanVienObj['tenNhanVien']
      employeeCode = nhanVienObj['maNhanVien'] || '---'
    } else if (employeeCatalog['name']) {
      employeeName = employeeCatalog['name']
      employeeCode = employeeCatalog['code'] || '---'
    } else if (nhanVienObj['tenTaiKhoan']) {
      employeeName = nhanVienObj['tenTaiKhoan']
    } else if (data['tenNhanVien']) {
      employeeName = data['tenNhanVien']
    } else if (Number(data['loaiDon']) === 1) {
      employeeName = data['nguoiTao'] || 'Nhân viên bán hàng'
    } else {
      employeeName = 'Đơn Online'
    }

    invoiceData.value.employee = {
      name: employeeName,
      code: employeeCode
    }

    // 3. Phiếu giảm giá: lấy từ bảng khóa ngoại PhieuGiamGia và catalog DB
    const pggObj = data['idPhieuGiamGia'] || data['phieuGiamGia'] || {}
    const voucherCatalog = DB_VOUCHER_CATALOG[data['idPhieuGiamGia']] || DB_VOUCHER_CATALOG[pggObj['maPhieuGiamGia']] || DB_VOUCHER_CATALOG[pggObj['id']] || {}
    const voucherName = pggObj['tenPhieuGiamGia'] || voucherCatalog['name'] || data['tenPhieuGiamGia'] || pggObj['maPhieuGiamGia'] || (data['idPhieuGiamGia'] === 1 ? 'Voucher Giảm 50k (PGG01)' : '')

    invoiceData.value.createTime = formatDateTime(data['ngayTao'] || data['createdAt'])
    invoiceData.value.lastUpdated = formatDateTime(data['ngayCapNhat'] || data['updatedAt'] || data['thoiGianThanhToan'] || data['ngayTao'] || data['createdAt'])

    invoiceData.value.delivery = {
      address: getString(data['diaChiNhanHang'], data['diaChi'], data['address'], '—'),
      type: Number(data['loaiDon']) === 1 ? 'Tại quầy' : 'Online',
      carrier: getString(data['donViVanChuyen'], data['tenDonViVanChuyen'], data['carrier'], '—'),
      receiveType: Number(data['loaiDon']) === 1 ? 'Nhận tại quầy' : 'Giao hàng tận nơi',
      note: getString(data['ghiChu'], data['note'], '—')
    }

    // 1. Lấy danh sách sản phẩm có sẵn trong hóa đơn cha
    let rawList = data['chiTietHoaDon'] || data['chiTietHoaDons'] || data['danhSachChiTiet'] || data['items'] || []

    // 2. Nếu hóa đơn cha chưa kèm chi tiết, tự động gọi API con vừa bổ sung ở backend
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

    // Tự động tính toán tổng tiền chuẩn xác từ tất cả sản phẩm trong hóa đơn chi tiết
    const calcSum = invoiceData.value.items.reduce((sum, it) => sum + it.total, 0)
    const productTotal = calcSum > 0 ? calcSum : getNumber(data['tongTienHang'], data['tienHang'], data['tongTien'], 0)
    const shippingFee = getNumber(data['phiShip'], data['phiVanChuyen'], 0)

    // Xác định số tiền giảm giá từ phiếu giảm giá (nếu có)
    let discount = 0
    if (pggObj && (pggObj.giaTriGiamGia != null || pggObj.giamToiDa != null)) {
      const gtri = Number(pggObj.giaTriGiamGia || 0)
      if (Number(pggObj.loaiPhieuGiamGia) === 2) {
        // Giảm theo %
        const percentVal = (productTotal * gtri) / 100
        const maxVal = pggObj.giamToiDa ? Number(pggObj.giamToiDa) : Infinity
        discount = Math.min(percentVal, maxVal)
      } else {
        // Giảm tiền cố định
        discount = gtri
      }
    } else if (voucherCatalog && voucherCatalog['discount']) {
      discount = Number(voucherCatalog['discount'])
    } else if (data['tongTienGiamGia'] != null || data['tienGiamGia'] != null) {
      discount = getNumber(data['tongTienGiamGia'], data['tienGiamGia'], 0)
    } else if (data['tongTien'] && data['tienSauGiamGia'] && Number(data['tongTien']) > Number(data['tienSauGiamGia'])) {
      discount = Number(data['tongTien']) - Number(data['tienSauGiamGia'])
    }

    // Giảm giá không vượt quá tổng tiền hàng
    discount = Math.min(discount, productTotal)

    // Tổng tiền thanh toán chuẩn = Tổng tiền hàng + Phí ship - Giảm giá
    const totalPayment = Math.max(0, productTotal + shippingFee - discount)

    invoiceData.value.summary = {
      totalProductPrice: productTotal,
      shippingFee,
      voucherDiscount: discount,
      voucherName,
      totalPayment
    }

    const paymentStatus = data['trangThaiThanhToan'] !== undefined ? data['trangThaiThanhToan'] : data['daThanhToan']
    const isPaid = paymentStatus === true || Number(paymentStatus) === 1
    const defaultMethod = Number(data['loaiDon']) === 1 ? 'Tiền mặt' : 'Chuyển khoản ngân hàng'
    const transCode = data['maGiaoDich'] || data['transactionCode'] || `POS-REQ-ae60211a-a1b2-48a5-8bed-${(returnedCode || '090').toLowerCase()}`
    const noteThanhToan = data['ghiChuThanhToan'] || (Number(data['loaiDon']) === 1 ? `Thanh toán tại quầy - ${defaultMethod} | Khách đưa: ${totalPayment.toFixed(2)}` : `Thanh toán trực tuyến - ${defaultMethod}`)

    invoiceData.value.paymentHistory = {
      method: getString(data['hinhThucThanhToan'], data['phuongThucThanhToan'], defaultMethod),
      description: noteThanhToan,
      time: formatDateTime(data['thoiGianThanhToan'] || data['ngayTao']),
      status: isPaid ? 'Đã thanh toán' : 'Chưa thanh toán',
      amount: totalPayment,
      transCode
    }

    const currentStatus = data['trangThai'] !== undefined ? data['trangThai'] : (data['status'] || 1)
    applyOrderStatus(currentStatus)

    // Nạp lịch sử thao tác
    const rawLogs = data['lichSuHoaDon'] || data['lichSuHoaDons'] || data['lichSu']

    if (Array.isArray(rawLogs) && rawLogs.length > 0) {
      orderLogs.value = rawLogs.map((l, i) => ({
        id: l.id || i,
        action: l.hanhDong || l.ghiChu || l.thaoTac || l.action || 'Thao tác hóa đơn',
        hanhDong: l.hanhDong || l.ghiChu || l.thaoTac || l.action || 'Thao tác hóa đơn',
        thoiGian: l.thoiGian || l.ngayTao || l.thoiGianTao,
        nguoiThucHien: l.nguoiThucHien || l.tenNhanVien || (l.vaiTroNguoiThucHien ? `${l.vaiTroNguoiThucHien}` : 'Nhân viên'),
        ghiChu: l.ghiChu || '',
        isDone: true
      })).reverse()
    } else {
      orderLogs.value = buildLogsByStatus(currentStatus, data['ngayTao'] || data['createdAt'])
    }

    // Đồng bộ mốc thời gian hiển thị cho các bước đã đạt đến
    const createTimeStr = formatDateTime(data['ngayTao'] || data['createdAt'])
    if (invoiceData.value.steps[0] && !invoiceData.value.steps[0].time) {
      invoiceData.value.steps[0].time = createTimeStr
    }

    if (orderLogs.value && orderLogs.value.length > 0) {
      orderLogs.value.forEach(log => {
        const act = (log.action || log.hanhDong || '').toLowerCase()
        const tStr = formatDateTime(log.thoiGian)
        if (act.includes('tạo') || act.includes('chờ xác nhận')) {
          if (invoiceData.value.steps[0]) invoiceData.value.steps[0].time = tStr
        } else if (act.includes('xác nhận') && !act.includes('chờ')) {
          if (invoiceData.value.steps[1]) invoiceData.value.steps[1].time = tStr
        } else if (act.includes('chờ vận chuyển') || act.includes('kho') || act.includes('đóng gói')) {
          if (invoiceData.value.steps[2]) invoiceData.value.steps[2].time = tStr
        } else if (act.includes('đang vận chuyển') || act.includes('giao')) {
          if (invoiceData.value.steps[3]) invoiceData.value.steps[3].time = tStr
        } else if (act.includes('hoàn thành')) {
          if (invoiceData.value.steps[4]) invoiceData.value.steps[4].time = tStr
        }
      })
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

// Quản lý Modal "Chỉnh sửa đơn hàng" (Giao diện 2 Tabs)
const showStatusModal = ref(false)
const editModalTab = ref('donHang') // 'donHang' | 'khachHang'
const targetStatus = ref(1)
const statusNote = ref('')

const editCustomerForm = ref({
  name: '',
  phone: '',
  address: '',
  note: ''
})

const statusOptions = [
  { value: 1, label: 'Chờ Xác Nhận', desc: 'Đơn hàng mới tạo, đang chờ xác nhận từ cửa hàng', color: '#d97706', bg: '#fff4e5' },
  { value: 2, label: 'Đã Xác Nhận', desc: 'Đơn hàng đã được xác nhận, chuẩn bị đóng gói hàng', color: '#0369a1', bg: '#e0f2fe' },
  { value: 3, label: 'Chờ Vận Chuyển', desc: 'Hàng đã đóng gói xong, đang chờ bàn giao vận chuyển', color: '#8a3ee6', bg: '#f5edff' },
  { value: 4, label: 'Đang Vận Chuyển', desc: 'Shipper / bên vận chuyển đang giao hàng tới khách', color: '#00838f', bg: '#e0f7fa' },
  { value: 5, label: 'Hoàn Thành', desc: 'Đơn hàng đã giao thành công và hoàn tất toàn bộ', color: '#1b7a37', bg: '#e6f6ec' },
  { value: 6, label: 'Đã Hủy', desc: 'Hủy đơn hàng và ngừng tiến trình xử lý', color: '#be2626', bg: '#fee6e6' }
]

// Tính toán các trạng thái có thể chuyển tiếp theo đúng quy trình
const selectableStatusOptions = computed(() => {
  const cur = Number(invoiceData.value.status) || 1
  const isAtStore = invoiceData.value.delivery?.type === 'Tại quầy' || Number(invoiceData.value.delivery?.loaiDon) === 1
  const options = []

  const currentOpt = statusOptions.find(o => o.value === cur)
  if (currentOpt) {
    options.push({ value: cur, label: `${currentOpt.label} (hiện tại)` })
  }

  if (isAtStore) {
    if (cur === 1) {
      options.push({ value: 5, label: 'Hoàn thành (tiếp theo)' })
      options.push({ value: 6, label: 'Hủy đơn hàng' })
    }
  } else {
    if (cur === 1) {
      options.push({ value: 2, label: 'Đã xác nhận (tiếp theo)' })
      options.push({ value: 6, label: 'Hủy đơn hàng' })
    } else if (cur === 2) {
      options.push({ value: 3, label: 'Chờ vận chuyển (tiếp theo)' })
      options.push({ value: 6, label: 'Hủy đơn hàng' })
    } else if (cur === 3) {
      options.push({ value: 4, label: 'Đang vận chuyển (tiếp theo)' })
      options.push({ value: 6, label: 'Hủy đơn hàng' })
    } else if (cur === 4) {
      options.push({ value: 5, label: 'Hoàn thành (tiếp theo)' })
    }
  }

  return options
})

const hasNextStatus = computed(() => {
  return selectableStatusOptions.value.length > 1
})

const openStatusModal = () => {
  editModalTab.value = 'donHang'
  targetStatus.value = Number(invoiceData.value.status) || 1
  statusNote.value = ''
  editCustomerForm.value = {
    name: invoiceData.value.customer?.name !== 'Khách lẻ' ? invoiceData.value.customer?.name : '',
    phone: invoiceData.value.customer?.phone !== '—' && invoiceData.value.customer?.phone !== '---' ? invoiceData.value.customer?.phone : '',
    address: invoiceData.value.delivery?.address !== '—' && invoiceData.value.delivery?.address !== '---' ? invoiceData.value.delivery?.address : '',
    note: invoiceData.value.delivery?.note !== '—' && invoiceData.value.delivery?.note !== '---' ? invoiceData.value.delivery?.note : ''
  }
  showStatusModal.value = true
}

const openCancelModal = () => {
  targetStatus.value = 6
  statusNote.value = 'Khách hàng yêu cầu hủy đơn'
  showStatusModal.value = true
}

const handleSaveEditModal = async () => {
  if (editModalTab.value === 'donHang') {
    const newStatus = Number(targetStatus.value)
    if (newStatus === Number(invoiceData.value.status)) {
      showToast('Trạng thái đơn hàng không thay đổi!')
      showStatusModal.value = false
      return
    }
    await saveStatusChange(newStatus, `Chuyển trạng thái đơn hàng`)
  } else {
    const id = invoiceId.value || invoiceData.value.id
    if (!id) {
      showToast('Không tìm thấy ID hóa đơn để cập nhật!', 'warning')
      return
    }
    updatingStatus.value = true
    try {
      await api.put(`/api/hoa-don/${id}`, {
        tenKhachHang: editCustomerForm.value.name,
        soDienThoaiKhachHang: editCustomerForm.value.phone,
        diaChiNhanHang: editCustomerForm.value.address,
        ghiChu: editCustomerForm.value.note
      })
      showToast('Đã lưu thông tin khách hàng thành công!')
      showStatusModal.value = false
      const code = invoiceCode.value || invoiceData.value.code
      if (code) {
        await loadDetail(code)
      }
    } catch (err) {
      showToast(err.response?.data?.message || 'Cập nhật thông tin khách hàng thất bại!', 'warning')
    } finally {
      updatingStatus.value = false
    }
  }
}

// Quản lý bộ lọc sản phẩm trong hóa đơn chi tiết
const productFilter = ref({
  search: '',
  category: '',
  color: '',
  size: '',
  sort: ''
})

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
      if (it.name.toLowerCase().includes('áo phông') || it.name.toLowerCase().includes('t-shirt')) set.add('Áo Phông')
      else if (it.name.toLowerCase().includes('polo')) set.add('Áo Polo')
      else if (it.name.toLowerCase().includes('sơ mi')) set.add('Áo Sơ Mi')
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
    list = list.filter(it =>
      it.code?.toLowerCase().includes(q) ||
      it.variantCode?.toLowerCase().includes(q) ||
      it.name?.toLowerCase().includes(q)
    )
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
  if (productFilter.value.sort === 'price-asc') {
    list.sort((a, b) => a.price - b.price)
  } else if (productFilter.value.sort === 'price-desc') {
    list.sort((a, b) => b.price - a.price)
  } else if (productFilter.value.sort === 'qty-desc') {
    list.sort((a, b) => b.quantity - a.quantity)
  }
  return list
})

// Chỉ hiển thị các bước trạng thái đã đạt đến ("ấn đến đâu mới hiện trạng thái đến đó")
const visibleSteps = computed(() => {
  const isAtStore = invoiceData.value.delivery?.type === 'Tại quầy' || Number(invoiceData.value.delivery?.loaiDon) === 1
  const curStatus = Number(invoiceData.value.status) || 1
  const createTime = invoiceData.value.createTime || invoiceData.value.steps[0]?.time || ''
  const finishTime = invoiceData.value.lastUpdated || invoiceData.value.paymentHistory?.time || createTime

  if (isAtStore) {
    // Đơn tại quầy: Hóa đơn chờ -> Hoàn thành
    if (curStatus === 6) {
      return [
        { id: 1, name: 'Hóa đơn chờ', time: createTime, done: true, active: false, icon: 'hourglass' },
        { id: 6, name: 'Đã hủy đơn', time: finishTime, done: false, active: true, isCancelled: true, icon: 'cancelled' }
      ]
    }
    if (curStatus === 5) {
      return [
        { id: 1, name: 'Hóa đơn chờ', time: createTime, done: true, active: false, icon: 'hourglass' },
        { id: 2, name: 'Hoàn thành', time: finishTime, done: false, active: true, isCompleted: true, icon: 'flag' }
      ]
    }
    return [
      { id: 1, name: 'Hóa đơn chờ', time: createTime, done: false, active: true, icon: 'hourglass' }
    ]
  }

  // Đơn Online
  if (curStatus === 6) {
    const list = []
    const step1 = invoiceData.value.steps.find(s => s.id === 1) || { id: 1, name: 'Chờ xác nhận', time: createTime }
    list.push({ ...step1, time: createTime, done: true, active: false, pending: false, icon: 'hourglass' })

    if (orderLogs.value && orderLogs.value.length > 0) {
      const logTitles = orderLogs.value.map(l => (l.action || l.hanhDong || '').toLowerCase())
      if (logTitles.some(t => t.includes('xác nhận') && !t.includes('chờ xác nhận'))) {
        const step2 = invoiceData.value.steps.find(s => s.id === 2) || { id: 2, name: 'Đã xác nhận', time: '' }
        list.push({ ...step2, done: true, active: false, pending: false, icon: 'check' })
      }
      if (logTitles.some(t => t.includes('chờ vận chuyển') || t.includes('bàn giao') || t.includes('đóng gói'))) {
        const step3 = invoiceData.value.steps.find(s => s.id === 3) || { id: 3, name: 'Chờ vận chuyển', time: '' }
        list.push({ ...step3, done: true, active: false, pending: false, icon: 'box' })
      }
      if (logTitles.some(t => t.includes('đang vận chuyển') || t.includes('giao'))) {
        const step4 = invoiceData.value.steps.find(s => s.id === 4) || { id: 4, name: 'Đang vận chuyển', time: '' }
        list.push({ ...step4, done: true, active: false, pending: false, icon: 'truck' })
      }
    }

    const cancelLog = orderLogs.value?.find(l => (l.action || l.hanhDong || '').toLowerCase().includes('hủy'))
    const cancelTime = cancelLog?.thoiGian ? formatDateTime(cancelLog.thoiGian) : (finishTime || '')

    list.push({
      id: 6,
      name: 'Đã hủy đơn',
      time: cancelTime,
      done: false,
      active: true,
      isCancelled: true,
      icon: 'cancelled'
    })
    return list
  }

  // Online bình thường: chỉ hiện các bước đã hoàn thành hoặc đang ở bước đó
  return invoiceData.value.steps
    .filter(s => s.id <= curStatus)
    .map(s => ({
      ...s,
      icon: s.id === 1 ? 'hourglass' : (s.id === 5 ? 'flag' : (s.id === 4 ? 'truck' : (s.id === 3 ? 'box' : 'check')))
    }))
})

const nextStatusInfo = computed(() => {
  const cur = Number(invoiceData.value.status)
  switch (cur) {
    case 1:
      return { nextStatus: 2, label: 'Xác Nhận Đơn Hàng' }
    case 2:
      return { nextStatus: 3, label: 'Chờ Vận Chuyển' }
    case 3:
      return { nextStatus: 4, label: 'Giao Vận Chuyển' }
    case 4:
      return { nextStatus: 5, label: 'Hoàn Thành Đơn' }
    default:
      return null
  }
})

const saveStatusChange = async (newStatusOverride = null, customNote = null) => {
  const newStatus = Number(newStatusOverride !== null ? newStatusOverride : targetStatus.value)
  const note = customNote !== null ? customNote : statusNote.value.trim()
  const id = invoiceId.value || invoiceData.value.id
  const code = invoiceCode.value || invoiceData.value.code

  if (!newStatus || newStatus < 1 || newStatus > 6) {
    showToast('Vui lòng chọn trạng thái hợp lệ!', 'warning')
    return
  }

  updatingStatus.value = true
  try {
    let success = false

    // 1. Cố gắng cập nhật qua ID
    if (id) {
      try {
        await api.put(`/api/hoa-don/${id}/trang-thai`, {
          trangThai: newStatus,
          ghiChu: note || undefined
        })
        success = true
      } catch (err) {
        console.warn('Cập nhật theo ID thất bại, sẽ thử cập nhật theo mã:', err)
      }
    }

    // 2. Nếu chưa thành công hoặc không có ID, cập nhật qua mã hóa đơn
    if (!success && code) {
      try {
        await api.put(`/api/hoa-don/code/${encodeURIComponent(code)}/trang-thai`, {
          trangThai: newStatus,
          ghiChu: note || undefined
        })
        success = true
      } catch (err) {
        console.error('Cập nhật theo mã thất bại:', err)
      }
    }

    if (success) {
      const option = statusOptions.find(o => o.value === newStatus)
      showToast(`Đã cập nhật trạng thái hóa đơn thành "${option ? option.label : newStatus}"!`)
      showStatusModal.value = false
      if (code) {
        await loadDetail(code)
      }
    } else {
      showToast('Cập nhật trạng thái thất bại. Vui lòng thử lại!', 'warning')
    }
  } catch (error) {
    showToast(error.response?.data?.message || 'Cập nhật trạng thái thất bại!', 'warning')
  } finally {
    updatingStatus.value = false
  }
}

// Quản lý Modal Xác Nhận Tùy Chỉnh (thay thế triệt để window.confirm / alert của trình duyệt)
const confirmModal = ref({
  show: false,
  title: 'Xác Nhận Thao Tác',
  message: '',
  subText: '',
  confirmText: 'Đồng ý',
  cancelText: 'Hủy bỏ',
  type: 'primary',
  onConfirm: null
})

const openConfirmModal = ({
  title = 'Xác Nhận Thao Tác',
  message = '',
  subText = '',
  confirmText = 'Đồng ý',
  cancelText = 'Hủy bỏ',
  type = 'primary',
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
    onConfirm
  }
}

const closeConfirmModal = () => {
  confirmModal.value.show = false
}

const handleConfirmAccept = async () => {
  const cb = confirmModal.value.onConfirm
  confirmModal.value.show = false
  if (cb) {
    await cb()
  }
}

const handleNextStatus = () => {
  if (!nextStatusInfo.value || updatingStatus.value) return
  const info = nextStatusInfo.value
  openConfirmModal({
    title: 'Xác Nhận Chuyển Trạng Thái',
    message: `Bạn có chắc chắn muốn chuyển trạng thái đơn hàng sang "${info.label}" không?`,
    subText: 'Hệ thống sẽ cập nhật trạng thái đơn hàng và lưu mốc thời gian vào lịch sử thao tác.',
    confirmText: 'Đồng ý chuyển',
    cancelText: 'Hủy bỏ',
    type: 'primary',
    onConfirm: async () => {
      await saveStatusChange(info.nextStatus, `Chuyển tiếp trạng thái: ${info.label}`)
    }
  })
}

const handlePrint = () => {
  const originalTitle = document.title
  // Đặt tên trang để khi chọn "Lưu dưới dạng PDF" (Save as PDF), trình duyệt tự đặt tên tệp là Hoa_Don_HDxxx.pdf
  document.title = `Hoa_Don_${invoiceData.value.code || 'FF_Tshirt'}`
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

    <!-- Top Meta Information Card (3 columns x 2 rows gọn gàng, phân cách tinh tế) -->
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
      </div>
    </div>

    <!-- Main Layout Grid (Left 1.8fr, Right 1fr) -->
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
          </div>

          <div class="stepper-track-wrap">
            <div class="stepper-bg-line"></div>
            <div
                v-for="step in visibleSteps"
                :key="step.id"
                class="stepper-step-item"
                :class="{ 'step-item-cancelled': step.isCancelled }"
            >
              <div
                  class="step-circle-icon"
                  :class="{
                    'circle-blue': !step.isCancelled,
                    'circle-red': step.isCancelled
                  }"
              >
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
                <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>

              <div class="step-item-title" :class="{ 'text-danger': step.isCancelled }">{{ step.name }}</div>
              <div v-if="step.time" class="step-item-time">{{ step.time }}</div>
            </div>
          </div>

          <div class="stepper-footer-right">
            <button class="btn-history-outline" type="button" @click="showHistoryModal = true">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              Lịch sử thao tác
            </button>
          </div>
        </div>

        <!-- Khung 2: Khách hàng & Giao hàng (Dual Grid) -->
        <div class="info-dual-grid">
          <!-- Thông tin khách hàng -->
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
                <span class="info-row-val font-bold">{{ invoiceData.customer.name }}</span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Mã khách hàng</span>
                <span class="info-row-val">{{ invoiceData.customer.code || '—' }}</span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Số điện thoại</span>
                <span class="info-row-val">{{ invoiceData.customer.phone || '—' }}</span>
              </div>
              <div class="info-list-row">
                <span class="info-row-label">Email</span>
                <span class="info-row-val">{{ invoiceData.customer.email || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- Thông tin giao hàng -->
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
        <!-- Khung 3: Tổng kết thanh toán -->
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
          </div>

          <div class="summary-body">
            <div class="summary-line">
              <span class="s-label">Tổng tiền hàng</span>
              <span class="s-val">{{ formatMoney(invoiceData.summary.totalProductPrice) }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Giảm giá</span>
              <span class="s-val">{{ invoiceData.summary.voucherDiscount > 0 ? '-' : '- ' }}{{ formatMoney(invoiceData.summary.voucherDiscount) }}</span>
            </div>
            <div class="summary-divider"></div>
            <div class="summary-total-line">
              <span class="total-label-caps">TỔNG TIỀN</span>
              <span class="total-price-blue">{{ formatMoney(invoiceData.summary.totalPayment) }}</span>
            </div>
          </div>
        </div>

        <!-- Khung 4: Lịch sử thanh toán -->
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

            <div class="pay-time-trans">
              <span class="time-clock-icon">🕒</span> {{ invoiceData.paymentHistory.time }} • {{ invoiceData.paymentHistory.transCode }}
            </div>

            <div class="pay-amount-row">
              <span class="pay-label-sub">Số tiền</span>
              <span class="pay-amount-bold">{{ formatMoney(invoiceData.paymentHistory.amount) }}</span>
            </div>

            <div class="pay-note-sub">{{ invoiceData.paymentHistory.description }}</div>
          </div>

          <div class="payment-actions-col">
            <button class="btn-blue-action" type="button" @click="handlePrint">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 6 2 18 2 18 9"></polyline>
                <path d="M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2"></path>
                <rect x="6" y="14" width="12" height="8"></rect>
              </svg>
              In hóa đơn
            </button>

            <button class="btn-blue-action" type="button" :disabled="updatingStatus" @click="openStatusModal">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              Chỉnh sửa đơn hàng
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Khung 5 (Full Width): Danh sách sản phẩm -->
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

      <!-- Toolbar bộ lọc sản phẩm (5 dropdowns / inputs) -->
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
          <select v-model="productFilter.category" class="form-control-tb">
            <option value="">Tất cả loại</option>
            <option v-for="cat in availableCategories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </div>

        <div class="toolbar-item">
          <select v-model="productFilter.color" class="form-control-tb">
            <option value="">Tất cả màu sắc</option>
            <option v-for="col in availableColors" :key="col" :value="col">{{ col }}</option>
          </select>
        </div>

        <div class="toolbar-item">
          <select v-model="productFilter.size" class="form-control-tb">
            <option value="">Tất cả kích cỡ</option>
            <option v-for="s in availableSizes" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>

        <div class="toolbar-item">
          <select v-model="productFilter.sort" class="form-control-tb">
            <option value="">Mặc định</option>
            <option value="price-asc">Giá tăng dần</option>
            <option value="price-desc">Giá giảm dần</option>
            <option value="qty-desc">Số lượng giảm dần</option>
          </select>
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
            <td class="text-left font-bold text-code">{{ item.code }}</td>
            <td class="text-left font-medium">
              <div class="product-name-heading">{{ item.name }}</div>
              <div class="product-sub-variant">Biến thể: {{ item.variantCode || item.code }}</div>
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

    <!-- Modal Chỉnh Sửa Đơn Hàng (Thiết kế 2 Tab chuẩn theo mẫu giao diện) -->
    <div v-if="showStatusModal" class="modal-backdrop" @click.self="showStatusModal = false">
      <div class="modal-container modal-edit-order">
        <div class="modal-header modal-header-clean">
          <h3 class="modal-title-clean">Chỉnh sửa đơn hàng</h3>
          <button class="btn-close-modal" type="button" @click="showStatusModal = false">✕</button>
        </div>

        <!-- Thanh chuyển Tab: Thông tin đơn hàng & Thông tin khách hàng -->
        <div class="modal-tabs-bar">
          <button
              type="button"
              class="modal-tab-btn"
              :class="{ active: editModalTab === 'donHang' }"
              @click="editModalTab = 'donHang'"
          >
            Thông tin đơn hàng
          </button>
          <button
              type="button"
              class="modal-tab-btn"
              :class="{ active: editModalTab === 'khachHang' }"
              @click="editModalTab = 'khachHang'"
          >
            Thông tin khách hàng
          </button>
        </div>

        <div class="modal-body modal-body-order">
          <!-- Tab 1: Thông tin đơn hàng -->
          <div v-if="editModalTab === 'donHang'" class="tab-donhang-content">
            <div class="order-meta-cards-row">
              <div class="meta-mini-card">
                <span class="mini-card-label">Mã đơn hàng</span>
                <span class="mini-card-val">{{ invoiceData.code }}</span>
              </div>
              <div class="meta-mini-card">
                <span class="mini-card-label">Ngày tạo</span>
                <span class="mini-card-val">{{ invoiceData.createTime }}</span>
              </div>
            </div>

            <div class="form-group-modal mt-14">
              <label class="modal-label">Trạng thái</label>
              <div class="custom-select-wrapper">
                <select
                    v-model="targetStatus"
                    class="modal-select-field"
                    :disabled="!hasNextStatus"
                >
                  <option
                      v-for="st in selectableStatusOptions"
                      :key="st.value"
                      :value="st.value"
                  >
                    {{ st.label }}
                  </option>
                </select>
              </div>
              <p class="status-help-text">
                Chỉ được chuyển sang trạng thái kế tiếp theo đúng quy trình của đơn hàng.
              </p>

              <div v-if="!hasNextStatus" class="status-alert-box info">
                <span class="alert-icon">ⓘ</span>
                <span>Đơn hàng hiện không còn trạng thái kế tiếp để cập nhật.</span>
              </div>
            </div>
          </div>

          <!-- Tab 2: Thông tin khách hàng -->
          <div v-else class="customer-edit-grid">
            <div class="form-group-modal">
              <label class="modal-label">Tên khách hàng</label>
              <input
                  type="text"
                  v-model="editCustomerForm.name"
                  class="modal-input-field"
                  placeholder="Nhập tên khách hàng..."
              />
            </div>

            <div class="form-group-modal">
              <label class="modal-label">Số điện thoại</label>
              <input
                  type="text"
                  v-model="editCustomerForm.phone"
                  class="modal-input-field"
                  placeholder="Nhập số điện thoại..."
              />
            </div>

            <div class="form-group-modal">
              <label class="modal-label">Địa chỉ nhận hàng</label>
              <input
                  type="text"
                  v-model="editCustomerForm.address"
                  class="modal-input-field"
                  placeholder="Nhập địa chỉ nhận hàng..."
              />
            </div>

            <div class="form-group-modal">
              <label class="modal-label">Ghi chú</label>
              <textarea
                  v-model="editCustomerForm.note"
                  rows="3"
                  class="modal-textarea-field"
                  placeholder="Nhập ghi chú cho đơn hàng..."
              ></textarea>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-modal-cancel" type="button" @click="showStatusModal = false">
            Hủy
          </button>
          <button
              class="btn-modal-save"
              type="button"
              :disabled="updatingStatus"
              @click="handleSaveEditModal"
          >
            {{ updatingStatus ? 'Đang lưu...' : '✓ Lưu' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Xác Nhận Thao Tác (Thay thế confirm() của localhost) -->
    <transition name="modal-fade">
      <div v-if="confirmModal.show" class="modal-backdrop" @click.self="closeConfirmModal">
        <div class="modal-container modal-confirm-box">
          <div class="modal-header confirm-header">
            <div class="confirm-title-group">
              <span class="confirm-icon-badge" :class="confirmModal.type">
                <svg v-if="confirmModal.type === 'danger'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <path d="M12 16v-4"></path>
                  <path d="M12 8h.01"></path>
                </svg>
              </span>
              <div>
                <h3 class="card-title">{{ confirmModal.title }}</h3>
              </div>
            </div>
            <button class="btn-close-modal" type="button" @click="closeConfirmModal">✕</button>
          </div>

          <div class="modal-body confirm-body">
            <p class="confirm-main-message">{{ confirmModal.message }}</p>
            <p v-if="confirmModal.subText" class="confirm-sub-message">{{ confirmModal.subText }}</p>
          </div>

          <div class="modal-footer confirm-footer">
            <button class="btn-modal-cancel" type="button" @click="closeConfirmModal">
              {{ confirmModal.cancelText }}
            </button>
            <button
                class="btn-confirm-accept"
                :class="confirmModal.type"
                type="button"
                :disabled="updatingStatus"
                @click="handleConfirmAccept"
            >
              {{ updatingStatus ? 'Đang xử lý...' : confirmModal.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ============================================ -->
    <!-- ============================================ -->
    <!-- MẪU IN HÓA ĐƠN CHUẨN A4 DÀNH CHO XUẤT FILE PDF -->
    <!-- ============================================ -->
    <!-- MẪU IN HÓA ĐƠN ĐƠN GIẢN, TINH TẾ, THANH LỊCH (MINIMALIST INVOICE) -->
    <div class="printable-invoice">
      <!-- Header: Tên Cửa Hàng & Tiêu Đề -->
      <div class="print-header-top">
        <div class="print-store-brand">
          <div class="print-brand-name">FF T-SHIRT FASHION</div>
          <div class="print-store-meta">
            123 Đường Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội<br/>
            Hotline: 0988.888.888 — Website: fftshirt.vn
          </div>
        </div>
        <div class="print-invoice-title-block">
          <h1 class="print-main-title">HÓA ĐƠN BÁN HÀNG</h1>
          <div class="print-meta-code">Mã HĐ: <strong>{{ invoiceData.code }}</strong></div>
          <div class="print-meta-date">Ngày lập: {{ invoiceData.paymentHistory?.time || (invoiceData.createTime + ' ' + invoiceData.createDate) }}</div>
        </div>
      </div>

      <div class="print-divider"></div>

      <!-- Thông Tin Khách Hàng & Đơn Hàng (2 Cột Tinh Gọn, Không Đóng Hộp Rườm Rà) -->
      <div class="print-info-row">
        <div class="print-info-col">
          <div class="print-data-line">
            <span class="p-label">Khách hàng:</span>
            <span class="p-val font-bold">{{ invoiceData.customer.name }}</span>
          </div>
          <div class="print-data-line" v-if="invoiceData.customer.phone && invoiceData.customer.phone !== '—' && invoiceData.customer.phone !== '---'">
            <span class="p-label">Điện thoại:</span>
            <span class="p-val">{{ invoiceData.customer.phone }}</span>
          </div>
          <div class="print-data-line" v-if="invoiceData.delivery.address && invoiceData.delivery.type !== 'Tại quầy' && invoiceData.delivery.type !== 'Tại cửa hàng'">
            <span class="p-label">Địa chỉ nhận:</span>
            <span class="p-val">{{ invoiceData.delivery.address }}</span>
          </div>
        </div>

        <div class="print-info-col">
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
            <span class="p-val">{{ invoiceData.delivery.note }}</span>
          </div>
        </div>
      </div>

      <!-- Bảng Sản Phẩm Đơn Giản, Thanh Lịch -->
      <table class="print-table">
        <thead>
          <tr>
            <th style="width: 40px;" class="text-center">#</th>
            <th style="width: 95px;" class="text-left">Mã SP</th>
            <th class="text-left">Tên sản phẩm</th>
            <th style="width: 120px;" class="text-center">Phân loại</th>
            <th style="width: 50px;" class="text-center">SL</th>
            <th style="width: 110px;" class="text-right">Đơn giá</th>
            <th style="width: 120px;" class="text-right">Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(it, idx) in invoiceData.items" :key="it.id || idx">
            <td class="text-center">{{ idx + 1 }}</td>
            <td class="text-left">{{ it.code }}</td>
            <td class="text-left font-medium">{{ it.name }}</td>
            <td class="text-center text-muted">{{ it.color }} / {{ it.size }}</td>
            <td class="text-center font-bold">{{ it.quantity }}</td>
            <td class="text-right">{{ formatMoney(it.price) }}</td>
            <td class="text-right font-bold">{{ formatMoney(it.total) }}</td>
          </tr>
          <tr v-if="!invoiceData.items || invoiceData.items.length === 0">
            <td colspan="7" class="text-center" style="padding: 16px; color: #64748b;">Không có sản phẩm nào trong hóa đơn</td>
          </tr>
        </tbody>
      </table>

      <!-- Khối Thanh Toán & Tổng Tiền Tinh Gọn (Không Đóng Hộp Rườm Rà) -->
      <div class="print-bottom-summary">
        <div class="print-payment-info">
          <div class="print-data-line">
            <span class="p-label">Thanh toán:</span>
            <span class="p-val font-bold">{{ invoiceData.paymentHistory.method }}</span>
          </div>
          <div class="print-data-line">
            <span class="p-label">Trạng thái:</span>
            <span class="p-val">{{ invoiceData.paymentHistory.status }}</span>
          </div>
        </div>

        <div class="print-calc-block">
          <div class="calc-row">
            <span>Tổng tiền hàng:</span>
            <span>{{ formatMoney(invoiceData.summary.totalProductPrice) }}</span>
          </div>
          <div v-if="invoiceData.summary.voucherDiscount > 0" class="calc-row">
            <span>Giảm giá:</span>
            <span>-{{ formatMoney(invoiceData.summary.voucherDiscount) }}</span>
          </div>
          <div v-if="invoiceData.summary.shippingFee > 0" class="calc-row">
            <span>Phí vận chuyển:</span>
            <span>{{ formatMoney(invoiceData.summary.shippingFee) }}</span>
          </div>
          <div class="calc-divider"></div>
          <div class="calc-row total-row">
            <span>TỔNG CỘNG:</span>
            <span class="total-money">{{ formatMoney(invoiceData.summary.totalPayment) }}</span>
          </div>
        </div>
      </div>

      <!-- Chữ Ký Xác Nhận -->
      <div class="print-signatures-row">
        <div class="sig-block">
          <div class="sig-role">Người Mua Hàng</div>
          <div class="sig-note">(Ký, ghi rõ họ tên)</div>
          <div class="sig-blank-space"></div>
          <div class="sig-name">{{ invoiceData.customer.name }}</div>
        </div>
        <div class="sig-block">
          <div class="sig-role">Người Bán Hàng</div>
          <div class="sig-note">(Ký, ghi rõ họ tên)</div>
          <div class="sig-blank-space"></div>
          <div class="sig-name">{{ invoiceData.employee?.name || 'Nhân viên bán hàng' }}</div>
        </div>
      </div>

      <!-- Lời Cảm Ơn Chân Trang -->
      <div class="print-footer-section">
        <div class="print-thanks">Cảm ơn quý khách đã tin tưởng và mua sắm tại FF T-Shirt!</div>
        <div class="print-policy">Hỗ trợ đổi trả size/màu trong 7 ngày kèm hóa đơn này. Hotline: 0988.888.888</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Ẩn mẫu in hóa đơn trên màn hình giao diện web */
.printable-invoice {
  display: none;
}

.chi-tiet-page {
  width: 100%;
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
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
  background-color: #3e5c76;
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

/* 1. Top Navigation Bar */
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
  font-size: 0.88rem;
}

.crumb-parent {
  color: #647074;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s ease;
}

.crumb-parent:hover {
  color: #3e5c76;
  text-decoration: underline;
}

.crumb-separator {
  color: #b0b7bd;
}

.crumb-current {
  color: #3e5c76;
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
  color: #3e5c76;
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
  font-size: 0.88rem;
}

/* 2. Top Meta Info Card (3 columns - gọn gàng, tinh tế) */
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
  font-size: 0.84rem;
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
  color: #3e5c76;
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
  color: #3e5c76;
  border: 1px solid #e2d8cd;
}

/* 3. Main Layout Grid (Left 1.8fr, Right 1fr) */
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

/* Card Box General */
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
  color: #3e5c76;
}

.title-icon {
  color: #3e5c76;
  flex-shrink: 0;
}

/* Left Card: Trạng thái đơn hàng (Stepper) */
.card-order-status {
  padding-bottom: 14px;
}

.stepper-track-wrap {
  position: relative;
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  padding: 16px 20px 10px;
}

.stepper-bg-line {
  position: absolute;
  top: 35px;
  left: 20%;
  right: 20%;
  height: 3px;
  background-color: #3e5c76;
  z-index: 1;
  border-radius: 2px;
}

.stepper-step-item {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-width: 130px;
}

.step-circle-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.circle-blue {
  background-color: #3e5c76;
  color: #ffffff;
  box-shadow: 0 0 0 4px #e5edf3;
}

.circle-red {
  background-color: #ef4444;
  color: #ffffff;
  box-shadow: 0 0 0 4px #fee2e2;
}

.step-item-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #2b353b;
  margin-bottom: 2px;
}

.step-item-time {
  font-size: 0.75rem;
  color: #8c969e;
}

.stepper-footer-right {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.btn-history-outline {
  background: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  padding: 6px 14px;
  font-size: 0.82rem;
  font-weight: 600;
  color: #3e5c76;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.15s ease;
}

.btn-history-outline:hover {
  background: #f7f5ef;
  border-color: #3e5c76;
  color: #243c50;
}

/* Left Row: Thông tin khách hàng & Giao hàng */
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

/* Right Card: Tổng kết thanh toán */
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
  color: #3e5c76;
}

/* Right Card: Lịch sử thanh toán */
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
  color: #3e5c76;
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
  color: #3e5c76;
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

/* 4. Bottom Card: Danh sách sản phẩm */
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
  height: 36px;
  border: 1px solid #d5c8b8;
  border-radius: 7px;
  padding: 0 10px;
  font-size: 0.84rem;
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
  font-size: 0.84rem;
}

.products-table-styled thead th {
  background: #f5efeb;
  border-bottom: 2px solid #ebd9c8;
  color: #3e5c76;
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
  color: #3e5c76;
  font-weight: 700;
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
  color: #3e5c76;
}

.color-dot-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  border: 1px solid rgba(0, 0, 0, 0.15);
}

.text-price-total {
  color: #3e5c76;
  font-weight: 700;
}

/* Utilities */
.font-bold {
  font-weight: 700;
}

.font-medium {
  font-weight: 500;
}

.text-center {
  text-align: center;
}

.text-left {
  text-align: left;
}

.text-right {
  text-align: right;
}

.text-muted {
  color: #8c969e !important;
}

.text-danger {
  color: #dc2626 !important;
}

/* Modal Popup Lịch Sử Thao Tác */
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
}

.btn-close-modal:hover {
  background: #f0e6dc;
  color: #3e5c76;
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
  color: #3e5c76;
  margin: 0;
}

/* Modal Lịch Sử Thao Tác */
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
  font-size: 0.85rem;
  color: #2b353b;
}

.log-time-text {
  font-size: 0.75rem;
  color: #8c969e;
}

.log-sub-info {
  font-size: 0.78rem;
  color: #647074;
}

/* Modal Chỉnh Sửa Đơn Hàng (2 Tabs Layout) */
.modal-edit-order {
  max-width: 530px;
  width: 92%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
  background: #ffffff;
  border: 1px solid #ebd9c8;
}

.modal-header-clean {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px 14px;
  background: #ffffff;
  border-bottom: none;
}

.modal-title-clean {
  font-size: 1.05rem;
  font-weight: 700;
  color: #2b353b;
  margin: 0;
}

/* Tabs Bar */
.modal-tabs-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
  border-bottom: 1px solid #ebe5df;
  background: #ffffff;
}

.modal-tab-btn {
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 8px 0 10px 0;
  font-size: 0.88rem;
  font-weight: 500;
  color: #78858f;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: -1px;
}

.modal-tab-btn:hover {
  color: #3e5c76;
}

.modal-tab-btn.active {
  color: #3e5c76;
  font-weight: 700;
  border-bottom: 2px solid #3e5c76;
}

.modal-body-order {
  padding: 18px 20px 22px;
  background: #ffffff;
}

/* Tab 1: Đơn Hàng */
.order-meta-cards-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 6px;
}

.meta-mini-card {
  background: #fbfaf7;
  border: 1px solid #ebd9c8;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mini-card-label {
  font-size: 0.74rem;
  color: #78858f;
  font-weight: 500;
}

.mini-card-val {
  font-size: 0.95rem;
  color: #2b353b;
  font-weight: 700;
  word-break: break-all;
}

.custom-select-wrapper {
  position: relative;
  width: 100%;
}

.modal-select-field {
  width: 100%;
  height: 42px;
  border: 1px solid #d5c8b8;
  border-radius: 8px;
  padding: 0 34px 0 12px;
  font-size: 0.88rem;
  color: #2b353b;
  background-color: #fcfbf9;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%233e5c76' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 14px;
}

.modal-select-field:focus {
  border-color: #3e5c76;
  box-shadow: 0 0 0 2px rgba(62, 92, 118, 0.15);
}

.modal-select-field:disabled {
  background-color: #f7f5ef;
  color: #78858f;
  cursor: not-allowed;
  opacity: 0.9;
}

.status-help-text {
  font-size: 0.76rem;
  color: #78858f;
  margin: 6px 0 12px 0;
  line-height: 1.4;
}

.status-alert-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.82rem;
  line-height: 1.4;
}

.status-alert-box.info {
  background: #f0f7fb;
  border: 1px solid #c9e3f3;
  color: #1e6288;
}

.alert-icon {
  font-size: 0.95rem;
  line-height: 1;
  flex-shrink: 0;
}

/* Tab 2: Khách Hàng */
.customer-edit-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-input-field {
  width: 100%;
  height: 38px;
  border: 1px solid #d5c8b8;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 0.88rem;
  color: #2b353b;
  background-color: #ffffff;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.modal-input-field:focus {
  border-color: #3e5c76;
  box-shadow: 0 0 0 2px rgba(62, 92, 118, 0.15);
}

.modal-textarea-field {
  width: 100%;
  border: 1px solid #d5c8b8;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 0.88rem;
  color: #2b353b;
  background-color: #ffffff;
  box-sizing: border-box;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.modal-textarea-field:focus {
  border-color: #3e5c76;
  box-shadow: 0 0 0 2px rgba(62, 92, 118, 0.15);
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
  transition: all 0.15s ease;
}

.btn-modal-cancel:hover {
  background: #f7f5ef;
  color: #3e5c76;
  border-color: #3e5c76;
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
  transition: all 0.15s ease;
  box-shadow: 0 2px 6px rgba(62, 92, 118, 0.25);
}

.btn-modal-save:hover:not(:disabled) {
  background: #2b4257;
  border-color: #243742;
}

.btn-modal-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Custom Confirm Modal Styling */
.modal-confirm-box {
  max-width: 480px;
  width: 90%;
  border-radius: 12px;
  border: 1px solid #ebd9c8;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  animation: modalPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  background: #ffffff;
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
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
  flex-shrink: 0;
}

.confirm-icon-badge.primary {
  background: #eaf1f4;
  color: #3e5c76;
}

.confirm-icon-badge.warning {
  background: #fef3c7;
  color: #d97706;
}

.confirm-icon-badge.danger {
  background: #fee2e2;
  color: #dc2626;
}

.confirm-body {
  padding: 20px;
}

.confirm-main-message {
  font-size: 0.95rem;
  color: #2b353b;
  font-weight: 600;
  line-height: 1.5;
  margin: 0 0 6px 0;
}

.confirm-sub-message {
  font-size: 0.82rem;
  color: #647074;
  line-height: 1.45;
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
  transition: all 0.15s ease;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-confirm-accept.primary {
  background-color: #3e5c76;
  box-shadow: 0 2px 6px rgba(62, 92, 118, 0.25);
}

.btn-confirm-accept.primary:hover:not(:disabled) {
  background-color: #2b4257;
}

.btn-confirm-accept.danger {
  background-color: #dc2626;
  box-shadow: 0 2px 6px rgba(220, 38, 38, 0.25);
}

.btn-confirm-accept.danger:hover:not(:disabled) {
  background-color: #b91c1c;
}

.btn-confirm-accept:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>

<!-- ======================================================== -->
<!-- ĐỊNH DẠNG IN HÓA ĐƠN CHUẨN A4 XUẤT RA FILE PDF BẰNG BROWSER PRINT -->
<!-- ======================================================== -->
<style>
@media print {
  @page {
    size: A4 portrait;
    margin: 15mm 15mm 15mm 15mm;
  }

  html, body {
    margin: 0 !important;
    padding: 0 !important;
    background: #ffffff !important;
    color: #111827 !important;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }

  /* Ẩn TOÀN BỘ mọi thành phần trong body */
  body * {
    visibility: hidden !important;
  }

  /* Ẩn triệt để tất cả layout giao diện web của ứng dụng */
  aside,
  .sidebar,
  header,
  .top-header,
  .top-nav-bar,
  .main-layout-grid,
  .left-section,
  .right-section,
  .invoice-items-card,
  .card-box,
  .modal-backdrop,
  .modal-container,
  .toast-popup,
  .action-status-footer,
  .stepper-container,
  .payment-action-box,
  .chi-tiet-page > :not(.printable-invoice) {
    display: none !important;
  }

  .app-shell,
  .main-content,
  .page-body,
  .chi-tiet-page {
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    min-height: auto !important;
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;
    display: block !important;
  }

  /* CHỈ HIỂN THỊ DUY NHẤT vùng Hóa Đơn Bán Hàng */
  .printable-invoice,
  .printable-invoice * {
    visibility: visible !important;
  }

  .printable-invoice {
    display: block !important;
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    box-sizing: border-box !important;
    background: #ffffff !important;
    color: #111827 !important;
    font-size: 13px !important;
    line-height: 1.5 !important;
  }

  /* Header Hóa Đơn */
  .print-header-top {
    display: flex !important;
    justify-content: space-between !important;
    align-items: flex-start !important;
    padding-bottom: 8px !important;
  }

  .print-store-brand {
    flex: 1 !important;
  }

  .print-brand-name {
    font-size: 18px !important;
    font-weight: 800 !important;
    color: #111827 !important;
    letter-spacing: 0.5px !important;
    margin-bottom: 2px !important;
  }

  .print-store-meta {
    font-size: 11.5px !important;
    color: #4b5563 !important;
    line-height: 1.45 !important;
  }

  .print-invoice-title-block {
    text-align: right !important;
    flex-shrink: 0 !important;
  }

  .print-main-title {
    font-size: 20px !important;
    font-weight: 800 !important;
    color: #111827 !important;
    margin: 0 0 3px 0 !important;
    letter-spacing: 0.5px !important;
  }

  .print-meta-code {
    font-size: 13px !important;
    color: #111827 !important;
    margin-bottom: 2px !important;
  }

  .print-meta-date {
    font-size: 11.5px !important;
    color: #6b7280 !important;
  }

  .print-divider {
    height: 1.5px !important;
    background: #111827 !important;
    margin: 8px 0 16px 0 !important;
  }

  /* Thông tin khách hàng & Đơn hàng (Đơn giản, không đóng hộp rườm rà) */
  .print-info-row {
    display: grid !important;
    grid-template-columns: 1fr 1fr !important;
    gap: 32px !important;
    margin-bottom: 20px !important;
  }

  .print-info-col {
    display: flex !important;
    flex-direction: column !important;
    gap: 4px !important;
  }

  .print-data-line {
    font-size: 12.5px !important;
    display: flex !important;
    gap: 8px !important;
    line-height: 1.4 !important;
  }

  .p-label {
    color: #6b7280 !important;
    min-width: 95px !important;
    flex-shrink: 0 !important;
  }

  .p-val {
    color: #111827 !important;
  }

  .p-val.font-bold {
    font-weight: 600 !important;
  }

  /* Bảng sản phẩm tinh giản: Chỉ có đường kẻ ngang, không ô lưới dọc */
  .print-table {
    width: 100% !important;
    border-collapse: collapse !important;
    margin-bottom: 20px !important;
    font-size: 12.5px !important;
  }

  .print-table th {
    border-top: 1.5px solid #111827 !important;
    border-bottom: 1.5px solid #111827 !important;
    border-left: none !important;
    border-right: none !important;
    padding: 8px 6px !important;
    font-size: 11.5px !important;
    font-weight: 700 !important;
    color: #111827 !important;
    text-transform: uppercase !important;
    background: transparent !important;
  }

  .print-table td {
    border-bottom: 1px solid #e5e7eb !important;
    border-top: none !important;
    border-left: none !important;
    border-right: none !important;
    padding: 8px 6px !important;
    vertical-align: middle !important;
    color: #111827 !important;
  }

  .print-table .text-center {
    text-align: center !important;
  }

  .print-table .text-left {
    text-align: left !important;
  }

  .print-table .text-right {
    text-align: right !important;
  }

  .print-table .font-bold {
    font-weight: 700 !important;
  }

  .print-table .font-medium {
    font-weight: 500 !important;
  }

  .print-table .text-muted {
    color: #6b7280 !important;
  }

  .print-table tr {
    page-break-inside: avoid !important;
  }

  /* Khối tổng kết thanh toán (Gọn gàng, không đóng khung hộp) */
  .print-bottom-summary {
    display: flex !important;
    justify-content: space-between !important;
    align-items: flex-start !important;
    margin-bottom: 28px !important;
    page-break-inside: avoid !important;
    gap: 20px !important;
  }

  .print-payment-info {
    display: flex !important;
    flex-direction: column !important;
    gap: 4px !important;
  }

  .print-calc-block {
    width: 290px !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 5px !important;
  }

  .calc-row {
    display: flex !important;
    justify-content: space-between !important;
    font-size: 12.5px !important;
    color: #374151 !important;
  }

  .calc-divider {
    height: 1px !important;
    background: #111827 !important;
    margin: 4px 0 !important;
  }

  .calc-row.total-row {
    font-size: 14px !important;
    font-weight: 800 !important;
    color: #111827 !important;
    padding-top: 2px !important;
  }

  .total-money {
    font-size: 15px !important;
    font-weight: 800 !important;
    color: #111827 !important;
  }

  /* Chữ ký */
  .print-signatures-row {
    display: grid !important;
    grid-template-columns: 1fr 1fr !important;
    gap: 60px !important;
    text-align: center !important;
    margin-bottom: 28px !important;
    page-break-inside: avoid !important;
  }

  .sig-block {
    display: flex !important;
    flex-direction: column !important;
    align-items: center !important;
  }

  .sig-role {
    font-size: 12.5px !important;
    font-weight: 700 !important;
    color: #111827 !important;
    text-transform: uppercase !important;
  }

  .sig-note {
    font-size: 11px !important;
    color: #6b7280 !important;
    font-style: italic !important;
    margin-top: 2px !important;
  }

  .sig-blank-space {
    height: 55px !important;
  }

  .sig-name {
    font-size: 12.5px !important;
    font-weight: 600 !important;
    color: #111827 !important;
  }

  /* Lời cảm ơn chân trang */
  .print-footer-section {
    text-align: center !important;
    border-top: 1px dashed #d1d5db !important;
    padding-top: 12px !important;
    page-break-inside: avoid !important;
  }

  .print-thanks {
    font-size: 12px !important;
    font-weight: 600 !important;
    color: #374151 !important;
    margin-bottom: 3px !important;
  }

  .print-policy {
    font-size: 11px !important;
    color: #6b7280 !important;
  }
}
</style>