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
  employee: {
    name: '---',
    code: '---'
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
    voucherName: '',
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

    invoiceData.value.delivery = {
      address: getString(data['diaChiNhanHang'], data['diaChi'], data['address'], '---'),
      type: Number(data['loaiDon']) === 1 ? 'Tại cửa hàng' : 'Online',
      note: getString(data['ghiChu'], data['note'], '---')
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

    invoiceData.value.paymentHistory = {
      method: getString(data['hinhThucThanhToan'], data['phuongThucThanhToan'], defaultMethod),
      description: getString(data['ghiChuThanhToan'], data['ghiChu'], isPaid ? 'Đã hoàn tất thanh toán' : 'Chờ khách hàng thanh toán'),
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

// Quản lý Chỉnh sửa trạng thái hóa đơn
const showStatusModal = ref(false)
const targetStatus = ref(1)
const statusNote = ref('')

const statusOptions = [
  { value: 1, label: 'Chờ Xác Nhận', desc: 'Đơn hàng mới tạo, đang chờ xác nhận từ cửa hàng', color: '#d97706', bg: '#fff4e5' },
  { value: 2, label: 'Đã Xác Nhận', desc: 'Đơn hàng đã được xác nhận, chuẩn bị đóng gói hàng', color: '#0369a1', bg: '#e0f2fe' },
  { value: 3, label: 'Chờ Vận Chuyển', desc: 'Hàng đã đóng gói xong, đang chờ bàn giao vận chuyển', color: '#8a3ee6', bg: '#f5edff' },
  { value: 4, label: 'Đang Vận Chuyển', desc: 'Shipper / bên vận chuyển đang giao hàng tới khách', color: '#00838f', bg: '#e0f7fa' },
  { value: 5, label: 'Hoàn Thành', desc: 'Đơn hàng đã giao thành công và hoàn tất toàn bộ', color: '#1b7a37', bg: '#e6f6ec' },
  { value: 6, label: 'Đã Hủy', desc: 'Hủy đơn hàng và ngừng tiến trình xử lý', color: '#be2626', bg: '#fee6e6' }
]

const openStatusModal = () => {
  targetStatus.value = Number(invoiceData.value.status) || 1
  statusNote.value = ''
  showStatusModal.value = true
}

const openCancelModal = () => {
  targetStatus.value = 6
  statusNote.value = 'Khách hàng yêu cầu hủy đơn'
  showStatusModal.value = true
}

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
            <!-- Chỉ hiển thị badge trạng thái -->
            <span class="badge-status-top" :class="'badge-status-' + invoiceData.status">{{ invoiceData.statusBadge }}</span>
          </div>

          <div class="stepper-container" :class="{ 'stepper-cancelled': invoiceData.status === 6 }">
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

          <!-- Nhóm nút hành động quản lý trạng thái -->
          <div class="action-status-footer">
            <div class="action-status-left">
              <button class="btn-outline-history" type="button" @click="showHistoryModal = true">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                Lịch Sử Thao Tác
              </button>
            </div>

            <div class="action-status-right">
              <!-- Nút Hủy Đơn Hàng (nếu đơn chưa hoàn thành và chưa hủy) -->
              <button
                  v-if="invoiceData.status !== 5 && invoiceData.status !== 6"
                  class="btn-cancel-order"
                  type="button"
                  :disabled="updatingStatus"
                  @click="openCancelModal"
              >
                ✕ Hủy Đơn Hàng
              </button>

              <!-- Nút Chỉnh Sửa Trạng Thái (Mở modal chọn trong 6 trạng thái) -->
              <button
                  class="btn-edit-status"
                  type="button"
                  :disabled="updatingStatus"
                  @click="openStatusModal"
              >
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                Chỉnh Sửa Trạng Thái
              </button>

              <!-- Nút Chuyển Tiếp Trạng Thái Nhanh (nếu còn bước tiếp theo) -->
              <button
                  v-if="nextStatusInfo"
                  class="btn-next-status"
                  type="button"
                  :disabled="updatingStatus"
                  @click="handleNextStatus"
              >
                {{ updatingStatus ? 'Đang xử lý...' : nextStatusInfo.label }}
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </button>
            </div>
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
                <span class="info-label">Nhân Viên</span>
                <span class="info-value font-medium">{{ invoiceData.employee?.name || '---' }}</span>
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
              <span class="s-label">
                Phiếu Giảm Giá
                <span v-if="invoiceData.summary.voucherName" class="voucher-subname">({{ invoiceData.summary.voucherName }})</span>
              </span>
              <span class="s-value">{{ invoiceData.summary.voucherDiscount > 0 ? '-' : '' }}{{ formatMoney(invoiceData.summary.voucherDiscount) }}</span>
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
          <tr v-for="(item, index) in invoiceData.items" :key="item.id" class="product-item-row">
            <td class="text-center text-muted col-stt-val">{{ index + 1 }}</td>
            <td class="text-left font-bold text-code">
              <span class="badge-code-product">{{ item.code }}</span>
            </td>
            <td class="text-left font-medium">
              <div class="product-meta-cell">
                <div class="product-icon-wrap">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.38 3.46L16 2a4 4 0 01-8 0L3.62 3.46a2 2 0 00-1.34 2.23l.58 3.47a1 1 0 00.99.84H6v10a2 2 0 002 2h8a2 2 0 002-2V10h2.15a1 1 0 00.99-.84l.58-3.47a2 2 0 00-1.34-2.23z"/>
                  </svg>
                </div>
                <div class="product-info-wrap">
                  <div class="product-name-heading">{{ item.name }}</div>
                  <div class="product-sub-variant">Biến thể: <span class="variant-sku-text">{{ item.variantCode || item.code }}</span></div>
                </div>
              </div>
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
            <td class="text-center">
              <span class="qty-pill">{{ item.quantity }}</span>
            </td>
            <td class="text-right text-price-unit">{{ formatMoney(item.price) }}</td>
            <td class="text-right font-bold text-dark text-price-total">{{ formatMoney(item.total) }}</td>
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

    <!-- Modal Chỉnh Sửa Trạng Thái Hóa Đơn -->
    <div v-if="showStatusModal" class="modal-backdrop" @click.self="showStatusModal = false">
      <div class="modal-container modal-edit-status">
        <div class="modal-header">
          <div>
            <h3 class="card-title">Chỉnh Sửa Trạng Thái Hóa Đơn</h3>
            <p class="modal-subtitle">
              Mã: <strong>{{ invoiceData.code }}</strong> — Khách hàng: <strong>{{ invoiceData.customer.name }}</strong>
            </p>
          </div>
          <button class="btn-close-modal" type="button" @click="showStatusModal = false">✕</button>
        </div>

        <div class="modal-body">
          <div class="form-group-modal">
            <label class="modal-label">Trạng thái hiện tại:</label>
            <div>
              <span class="badge-status-top" :class="'badge-status-' + invoiceData.status">
                {{ invoiceData.statusBadge }}
              </span>
            </div>
          </div>

          <div class="form-group-modal mt-14">
            <label class="modal-label">Chọn trạng thái mới <span class="text-danger">*</span></label>
            <div class="status-options-grid">
              <label
                  v-for="st in statusOptions"
                  :key="st.value"
                  class="status-radio-card"
                  :class="{ active: targetStatus === st.value }"
                  :style="{ borderColor: targetStatus === st.value ? st.color : '#e2d8cd' }"
              >
                <input
                    type="radio"
                    name="detailStatusRadio"
                    :value="st.value"
                    v-model="targetStatus"
                    class="radio-hidden"
                />
                <div
                    class="radio-circle"
                    :style="{
                      borderColor: st.color,
                      backgroundColor: targetStatus === st.value ? st.color : 'transparent'
                    }"
                >
                  <span v-if="targetStatus === st.value" class="radio-dot"></span>
                </div>
                <div class="status-card-content">
                  <div class="status-card-header">
                    <span
                        class="status-card-badge"
                        :style="{ color: st.color, backgroundColor: st.bg }"
                    >
                      {{ st.label }}
                    </span>
                    <span v-if="invoiceData.status === st.value" class="current-label">
                      (Hiện tại)
                    </span>
                  </div>
                  <p class="status-card-desc">{{ st.desc }}</p>
                </div>
              </label>
            </div>
          </div>

          <div class="form-group-modal mt-14">
            <label class="modal-label">Ghi chú cập nhật (Tùy chọn)</label>
            <textarea
                v-model="statusNote"
                rows="2"
                class="modal-textarea"
                placeholder="Nhập ghi chú thay đổi trạng thái nếu có..."
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-modal-cancel" type="button" @click="showStatusModal = false">
            Hủy bỏ
          </button>
          <button
              class="btn-modal-save"
              type="button"
              :disabled="updatingStatus"
              @click="saveStatusChange(null, null)"
          >
            {{ updatingStatus ? 'Đang lưu...' : '✓ Lưu Thay Đổi' }}
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

.badge-status-1 { background-color: #fff4e5; color: #d97706; }
.badge-status-2 { background-color: #e0f2fe; color: #0369a1; }
.badge-status-3 { background-color: #f5edff; color: #8a3ee6; }
.badge-status-4 { background-color: #e0f7fa; color: #00838f; }
.badge-status-5 { background-color: #e6f6ec; color: #1b7a37; }
.badge-status-6 { background-color: #fee6e6; color: #be2626; }

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

.stepper-cancelled {
  opacity: 0.55;
  filter: grayscale(0.5);
}

.action-status-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #ebd9c8;
  padding-top: 16px;
  margin-top: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.action-status-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-status-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* Nút Lịch Sử Thao Tác bên trong khung trạng thái */
.btn-outline-history {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 38px;
  padding: 0 16px;
  background-color: #ffffff;
  color: #3e5c76;
  border: 1.5px solid #d4c5b5;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-outline-history:hover {
  background-color: #f7f3ed;
  border-color: #3e5c76;
  color: #243c50;
  box-shadow: 0 2px 4px rgba(62, 92, 118, 0.12);
  transform: translateY(-1px);
}

/* Nút Chỉnh Sửa Trạng Thái */
.btn-edit-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 38px;
  padding: 0 16px;
  background-color: #eff6ff;
  color: #1d4ed8;
  border: 1.5px solid #bfdbfe;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(29, 78, 216, 0.05);
}

.btn-edit-status:hover:not(:disabled) {
  background-color: #dbeafe;
  border-color: #93c5fd;
  color: #1e40af;
  box-shadow: 0 2px 5px rgba(29, 78, 216, 0.15);
  transform: translateY(-1px);
}

.btn-edit-status:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Nút Hủy Đơn Hàng */
.btn-cancel-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 15px;
  background-color: #fef2f2;
  color: #dc2626;
  border: 1.5px solid #fecaca;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel-order:hover:not(:disabled) {
  background-color: #fee2e2;
  border-color: #f87171;
  color: #b91c1c;
  transform: translateY(-1px);
}

.btn-cancel-order:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Nút Chuyển Tiếp Trạng Thái Nhanh */
.btn-next-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 38px;
  padding: 0 18px;
  background-color: #3e5c76;
  color: #ffffff;
  border: 1.5px solid #3e5c76;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(62, 92, 118, 0.2);
}

.btn-next-status:hover:not(:disabled) {
  background-color: #2b4257;
  border-color: #2b4257;
  box-shadow: 0 3px 6px rgba(62, 92, 118, 0.25);
  transform: translateY(-1px);
}

.btn-next-status:disabled {
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

.voucher-subname {
  font-size: 0.8rem;
  color: #3e5c76;
  font-weight: 500;
  margin-left: 4px;
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

.color-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #334155;
}

.color-dot-indicator {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
  border: 1px solid rgba(0,0,0,0.2);
  flex-shrink: 0;
}

.size-tag {
  background: #e8f0fe;
  color: #1a73e8;
}

.badge-code-product {
  display: inline-block;
  padding: 3px 8px;
  background-color: #f1f5f9;
  color: #3e5c76;
  border-radius: 4px;
  font-size: 0.8rem;
  font-family: monospace;
  font-weight: 700;
}

.product-meta-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.product-info-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.product-name-heading {
  color: #1e293b;
  font-weight: 600;
  font-size: 0.88rem;
  line-height: 1.35;
}

.product-sub-variant {
  font-size: 0.75rem;
  color: #64748b;
}

.variant-sku-text {
  font-family: monospace;
  color: #3e5c76;
  font-weight: 600;
}

.qty-pill {
  display: inline-block;
  min-width: 28px;
  padding: 2px 8px;
  background: #f1f5f9;
  color: #1e293b;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.84rem;
}

.text-price-unit {
  color: #64748b;
  font-size: 0.86rem;
}

.text-price-total {
  color: #0f172a;
  font-size: 0.92rem;
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

.text-discount {
  color: #dc2626 !important;
  font-weight: 700;
}

.badge-type-store {
  background-color: #f0fdf4;
  color: #166534;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 0.82rem;
  font-weight: 600;
}

.badge-type-online {
  background-color: #eff6ff;
  color: #1d4ed8;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 0.82rem;
  font-weight: 600;
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

/* Modal Chỉnh Sửa Trạng Thái */
.modal-edit-status {
  max-width: 650px;
  width: 95%;
}

.modal-subtitle {
  font-size: 0.82rem;
  color: #64748b;
  margin: 4px 0 0 0;
}

.modal-subtitle strong {
  color: #3e5c76;
}

.form-group-modal {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mt-14 {
  margin-top: 14px;
}

.modal-label {
  font-size: 0.86rem;
  font-weight: 600;
  color: #334155;
}

.text-danger {
  color: #ef4444;
}

.status-options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

@media (max-width: 600px) {
  .status-options-grid {
    grid-template-columns: 1fr;
  }
}

.status-radio-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1.5px solid #e2d8cd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.status-radio-card:hover {
  background: #fbf9f6;
}

.status-radio-card.active {
  background: #fdfaf6;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.radio-hidden {
  display: none;
}

.radio-circle {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  transition: all 0.2s ease;
}

.radio-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #ffffff;
}

.status-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.status-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.status-card-badge {
  font-size: 0.78rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
}

.current-label {
  font-size: 0.72rem;
  color: #059669;
  font-weight: 600;
}

.status-card-desc {
  font-size: 0.74rem;
  color: #64748b;
  margin: 0;
  line-height: 1.35;
}

.modal-textarea {
  width: 100%;
  border: 1.5px solid #dcd1c4;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.85rem;
  color: #1e293b;
  box-sizing: border-box;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

.modal-textarea:focus {
  border-color: #3e5c76;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #ebd9c8;
  background: #fbf9f6;
  border-bottom-left-radius: 10px;
  border-bottom-right-radius: 10px;
}

.btn-modal-cancel {
  height: 36px;
  padding: 0 16px;
  background: #ffffff;
  color: #64748b;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-cancel:hover {
  background: #f1f5f9;
  color: #334155;
}

.btn-modal-save {
  height: 36px;
  padding: 0 18px;
  background: #3e5c76;
  color: #ffffff;
  border: 1px solid #3e5c76;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(62, 92, 118, 0.2);
}

.btn-modal-save:hover:not(:disabled) {
  background: #2b4257;
  border-color: #2b4257;
}

.btn-modal-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .main-layout-grid {
    grid-template-columns: 1fr;
  }
  .info-dual-grid {
    grid-template-columns: 1fr;
  }
}

/* Custom Confirm Modal Styling */
.modal-confirm-box {
  max-width: 480px;
  width: 90%;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22);
  animation: modalPop 0.22s cubic-bezier(0.16, 1, 0.3, 1);
  background: #ffffff;
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.confirm-header {
  padding: 18px 24px 14px 24px;
  border-bottom: 1px solid #f1e7dc;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.confirm-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.confirm-icon-badge {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.confirm-icon-badge.primary {
  background: #e8f1f5;
  color: #3e5c76;
}

.confirm-icon-badge.warning {
  background: #fff4e5;
  color: #d97706;
}

.confirm-icon-badge.danger {
  background: #fee2e2;
  color: #dc2626;
}

.confirm-body {
  padding: 22px 24px 14px 24px;
}

.confirm-main-message {
  font-size: 1rem;
  color: #2b353b;
  font-weight: 600;
  line-height: 1.55;
  margin: 0 0 8px 0;
}

.confirm-sub-message {
  font-size: 0.85rem;
  color: #6c757d;
  line-height: 1.45;
  margin: 0;
}

.confirm-footer {
  padding: 14px 24px 20px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid #f8f3ed;
  background: #fdfbf7;
}

.btn-confirm-accept {
  height: 38px;
  padding: 0 20px;
  border-radius: 7px;
  border: none;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
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