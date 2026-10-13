<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()

/* ==========================================================
   1. QUẢN LÝ HÓA ĐƠN CHỜ (Mặc định có 1 hóa đơn, tối đa 5)
========================================================== */
const createEmptyInvoice = (id, code) => ({
  id,
  code,
  cart: [],
  customerId: null,
  customerName: '',
  customerPhone: '',
  isDelivery: false,
  deliveryAddress: '',
  shippingFee: 0,
  voucher: null,
  paymentMethod: 'TIEN_MAT', // TIEN_MAT | CHUYEN_KHOAN | KET_HOP
  customerPay: null,
  note: ''
})

const existingInvoiceCodes = ref(new Set())

const waitingInvoices = ref([
  createEmptyInvoice(1, 'HD001')
])

const activeInvoiceId = ref(1)

// Hóa đơn hiện tại đang chọn
const currentInvoice = computed(() => {
  return waitingInvoices.value.find(inv => inv.id === activeInvoiceId.value) || waitingInvoices.value[0]
})

// Sinh mã hóa đơn tiếp theo đảm bảo KHÔNG TRÙNG với danh sách hóa đơn trong CSDL
const generateNextCode = () => {
  const activeCodes = waitingInvoices.value.map(i => i.code?.toUpperCase()).filter(Boolean)
  let maxNum = 0

  // Quét toàn bộ mã hóa đơn đã có trong CSDL (danh sách hóa đơn)
  for (const c of existingInvoiceCodes.value) {
    if (c && typeof c === 'string' && c.toUpperCase().startsWith('HD')) {
      const n = parseInt(c.substring(2), 10)
      if (!isNaN(n) && n > maxNum) {
        maxNum = n
      }
    }
  }

  // Quét các mã hóa đơn chờ đang mở trên giao diện
  for (const c of activeCodes) {
    if (c && typeof c === 'string' && c.toUpperCase().startsWith('HD')) {
      const n = parseInt(c.substring(2), 10)
      if (!isNaN(n) && n > maxNum) {
        maxNum = n
      }
    }
  }

  let nextNum = Math.max(maxNum + 1, 1)
  let candidate = `HD${String(nextNum).padStart(3, '0')}`

  while (existingInvoiceCodes.value.has(candidate) || activeCodes.includes(candidate)) {
    nextNum++
    candidate = `HD${String(nextNum).padStart(3, '0')}`
  }

  return candidate
}

// Lấy danh sách mã hóa đơn từ CSDL để đảm bảo không trùng
const fetchExistingInvoiceCodes = async () => {
  try {
    const res = await api.get('/api/hoa-don')
    const list = Array.isArray(res.data) ? res.data : []
    const codes = new Set()
    list.forEach(item => {
      const code = item.maHoaDon || item.code
      if (code) {
        codes.add(code.trim().toUpperCase())
      }
    })
    existingInvoiceCodes.value = codes

    // Kiểm tra và gán mã không trùng cho các hóa đơn chờ hiện có
    waitingInvoices.value.forEach(inv => {
      if (codes.has(inv.code.toUpperCase())) {
        inv.code = generateNextCode()
      }
    })
  } catch (err) {
    console.warn('Không tải được danh sách mã hóa đơn từ backend:', err)
  }
}

// Thêm hóa đơn chờ mới (tối đa 5 hóa đơn)
const addWaitingInvoice = () => {
  if (waitingInvoices.value.length >= 5) {
    alert('Chỉ được tạo tối đa 5 hóa đơn chờ cùng lúc!')
    return
  }

  const newId = Date.now()
  const newCode = generateNextCode()
  waitingInvoices.value.push(createEmptyInvoice(newId, newCode))
  activeInvoiceId.value = newId
}

// Chuyển tab hóa đơn
const selectInvoice = (id) => {
  activeInvoiceId.value = id
}

// Hủy / làm mới hóa đơn chờ hiện tại
const removeCurrentInvoice = () => {
  if (waitingInvoices.value.length === 1) {
    const confirmClear = confirm(`Bạn có muốn làm mới và xóa toàn bộ sản phẩm của ${currentInvoice.value.code}?`)
    if (confirmClear) {
      const idx = waitingInvoices.value.findIndex(inv => inv.id === activeInvoiceId.value)
      waitingInvoices.value[idx] = createEmptyInvoice(activeInvoiceId.value, currentInvoice.value.code)
    }
    return
  }

  const confirmDelete = confirm(`Bạn có chắc muốn hủy hóa đơn ${currentInvoice.value.code}?`)
  if (!confirmDelete) return

  const idx = waitingInvoices.value.findIndex(inv => inv.id === activeInvoiceId.value)
  waitingInvoices.value = waitingInvoices.value.filter(inv => inv.id !== activeInvoiceId.value)
  activeInvoiceId.value = waitingInvoices.value[Math.max(0, idx - 1)].id
}

/* ==========================================================
   2. DANH SÁCH SẢN PHẨM & TỒN KHO & QUÉT BARCODE
========================================================== */
const showProductModal = ref(false)
const searchProductText = ref('')
const barcodeInput = ref('')
const isLoadingProducts = ref(false)

// Danh mục mẫu dự phòng chuẩn hóa theo CSDL FF T-Shirt
const fallbackProducts = [
  {
    id: 1,
    code: 'SP001',
    variantCode: 'CTSP01',
    name: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    color: 'Đen',
    size: 'S',
    stock: 45,
    price: 199000,
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 2,
    code: 'SP001',
    variantCode: 'CTSP02',
    name: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    color: 'Đen',
    size: 'M',
    stock: 20,
    price: 199000,
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 3,
    code: 'SP001',
    variantCode: 'CTSP03',
    name: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    color: 'Trắng',
    size: 'M',
    stock: 35,
    price: 199000,
    image: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 4,
    code: 'SP002',
    variantCode: 'CTSP04',
    name: 'Áo Polo Nam Cổ Bẻ Cao Cấp',
    color: 'Đen',
    size: 'M',
    stock: 25,
    price: 299000,
    image: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 5,
    code: 'SP002',
    variantCode: 'CTSP05',
    name: 'Áo Polo Nam Cổ Bẻ Cao Cấp',
    color: 'Xanh navy',
    size: 'L',
    stock: 18,
    price: 299000,
    image: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 6,
    code: 'SP003',
    variantCode: 'CTSP06',
    name: 'Áo Phông Unisex In Hình Graphic',
    color: 'Trắng',
    size: 'L',
    stock: 30,
    price: 250000,
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
  }
]

const productList = ref([...fallbackProducts])

// Gọi API lấy toàn bộ biến thể sản phẩm đang bán
const fetchProductVariants = async () => {
  try {
    isLoadingProducts.value = true
    const res = await api.get('/api/chi-tiet-san-pham/filter', {
      params: { trangThai: 1, size: 200 }
    })
    const content = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    if (content.length > 0) {
      productList.value = content.map((item, idx) => ({
        id: item.id || (idx + 1),
        code: item.maSanPham || item.maChiTietSanPham || `SP00${idx + 1}`,
        variantCode: item.maChiTietSanPham || `CTSP0${idx + 1}`,
        name: item.tenSanPham || 'Áo Phông Cao Cấp',
        color: item.tenMauSac || 'Mặc định',
        size: item.tenKichCo || 'Freesize',
        stock: item.soLuong != null ? Number(item.soLuong) : 50,
        price: item.giaBan != null ? Number(item.giaBan) : 199000,
        image: item.anhDaiDien || fallbackProducts[idx % fallbackProducts.length].image
      }))
    }
  } catch (e) {
    console.warn('Không tải được danh sách CTSP từ API, sử dụng danh mục mẫu chuẩn:', e)
  } finally {
    isLoadingProducts.value = false
  }
}

const filteredProducts = computed(() => {
  if (!searchProductText.value) return productList.value
  const kw = searchProductText.value.toLowerCase().trim()
  return productList.value.filter(p =>
    (p.name && p.name.toLowerCase().includes(kw)) ||
    (p.code && p.code.toLowerCase().includes(kw)) ||
    (p.variantCode && p.variantCode.toLowerCase().includes(kw)) ||
    (p.color && p.color.toLowerCase().includes(kw)) ||
    (p.size && p.size.toLowerCase().includes(kw))
  )
})

const selectProduct = (prod) => {
  if (prod.stock <= 0) {
    alert('Sản phẩm đã hết hàng trong kho!')
    return
  }

  const exist = currentInvoice.value.cart.find(i => i.id === prod.id)
  if (exist) {
    if (exist.quantity >= prod.stock) {
      alert(`Trong kho chỉ còn tối đa ${prod.stock} sản phẩm!`)
      return
    }
    exist.quantity++
  } else {
    currentInvoice.value.cart.push({
      ...prod,
      quantity: 1
    })
  }
}

// Xử lý quét barcode / nhập mã nhanh
const handleBarcodeScan = () => {
  const code = barcodeInput.value?.trim().toLowerCase()
  if (!code) return
  const found = productList.value.find(p =>
    (p.code && p.code.toLowerCase() === code) ||
    (p.variantCode && p.variantCode.toLowerCase() === code) ||
    String(p.id) === code
  )
  if (found) {
    selectProduct(found)
    barcodeInput.value = ''
  } else {
    alert(`Không tìm thấy sản phẩm có mã "${barcodeInput.value}"!`)
  }
}

const updateQuantity = (item, delta) => {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    removeFromCart(item)
    return
  }
  if (newQty > item.stock) {
    alert(`Số lượng trong kho chỉ còn tối đa ${item.stock}!`)
    return
  }
  item.quantity = newQty
}

const handleQuantityInput = (item, event) => {
  let val = parseInt(event.target.value, 10)
  if (isNaN(val) || val <= 0) {
    item.quantity = 1
  } else if (val > item.stock) {
    alert(`Số lượng trong kho chỉ còn tối đa ${item.stock}!`)
    item.quantity = item.stock
  } else {
    item.quantity = val
  }
}

const removeFromCart = (item) => {
  currentInvoice.value.cart = currentInvoice.value.cart.filter(i => i.id !== item.id)
}

/* ==========================================================
   3. TÌM KIẾM & THÊM NHANH KHÁCH HÀNG
========================================================== */
const customerSearchResults = ref([])
const showCustomerDropdown = ref(false)
const showQuickAddCustomerModal = ref(false)

const newCustomerForm = ref({
  tenKhachHang: '',
  soDienThoai: '',
  email: '',
  diaChi: ''
})
const newCustomerErrors = ref({})

const fallbackCustomers = [
  { id: 1, maKhachHang: 'KH001', tenKhachHang: 'Nguyễn Văn An', soDienThoai: '0911111111', diaChi: 'Hà Nội' },
  { id: 2, maKhachHang: 'KH002', tenKhachHang: 'Trần Thị Bình', soDienThoai: '0922222222', diaChi: 'Đà Nẵng' },
  { id: 3, maKhachHang: 'KH003', tenKhachHang: 'Lê Văn Cường', soDienThoai: '0933333333', diaChi: 'TP. Hồ Chí Minh' }
]

let searchCustomerTimer = null
const onCustomerInput = () => {
  clearTimeout(searchCustomerTimer)
  const query = currentInvoice.value.customerPhone?.trim()
  if (!query) {
    customerSearchResults.value = []
    showCustomerDropdown.value = false
    return
  }

  searchCustomerTimer = setTimeout(async () => {
    try {
      const res = await api.get('/api/khach-hang', {
        params: { keyword: query, size: 5 }
      })
      const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
      if (list.length > 0) {
        customerSearchResults.value = list
      } else {
        customerSearchResults.value = fallbackCustomers.filter(c =>
          c.tenKhachHang.toLowerCase().includes(query.toLowerCase()) ||
          c.soDienThoai.includes(query)
        )
      }
      showCustomerDropdown.value = customerSearchResults.value.length > 0
    } catch (e) {
      customerSearchResults.value = fallbackCustomers.filter(c =>
        c.tenKhachHang.toLowerCase().includes(query.toLowerCase()) ||
        c.soDienThoai.includes(query)
      )
      showCustomerDropdown.value = customerSearchResults.value.length > 0
    }
  }, 250)
}

const selectCustomer = (cust) => {
  currentInvoice.value.customerId = cust.id
  currentInvoice.value.customerName = cust.tenKhachHang
  currentInvoice.value.customerPhone = cust.soDienThoai
  if (cust.diaChi && !currentInvoice.value.deliveryAddress) {
    currentInvoice.value.deliveryAddress = cust.diaChi
  }
  showCustomerDropdown.value = false
}

const clearSelectedCustomer = () => {
  currentInvoice.value.customerId = null
  currentInvoice.value.customerName = ''
  currentInvoice.value.customerPhone = ''
  currentInvoice.value.deliveryAddress = ''
}

const openQuickAddModal = () => {
  newCustomerForm.value = {
    tenKhachHang: '',
    soDienThoai: currentInvoice.value.customerPhone || '',
    email: '',
    diaChi: currentInvoice.value.deliveryAddress || ''
  }
  newCustomerErrors.value = {}
  showQuickAddCustomerModal.value = true
}

const submitQuickAddCustomer = async () => {
  newCustomerErrors.value = {}
  if (!newCustomerForm.value.tenKhachHang?.trim()) {
    newCustomerErrors.value.tenKhachHang = 'Vui lòng nhập tên khách hàng'
  }
  const phone = newCustomerForm.value.soDienThoai?.trim() || ''
  if (!phone) {
    newCustomerErrors.value.soDienThoai = 'Vui lòng nhập số điện thoại'
  } else if (!/^(0[3|5|7|8|9])[0-9]{8}$/.test(phone)) {
    newCustomerErrors.value.soDienThoai = 'Số điện thoại phải gồm 10 chữ số hợp lệ'
  }

  if (Object.keys(newCustomerErrors.value).length > 0) return

  try {
    const res = await api.post('/api/khach-hang', {
      tenKhachHang: newCustomerForm.value.tenKhachHang.trim(),
      soDienThoai: phone,
      email: newCustomerForm.value.email?.trim() || null,
      diaChiCuThe: newCustomerForm.value.diaChi?.trim() || null,
      trangThai: 1
    })
    const created = res.data || {}
    currentInvoice.value.customerId = created.id || Date.now()
    currentInvoice.value.customerName = created.tenKhachHang || newCustomerForm.value.tenKhachHang.trim()
    currentInvoice.value.customerPhone = created.soDienThoai || phone
    if (newCustomerForm.value.diaChi) {
      currentInvoice.value.deliveryAddress = newCustomerForm.value.diaChi
    }
    showQuickAddCustomerModal.value = false
    alert('Thêm mới khách hàng thành công!')
  } catch (err) {
    currentInvoice.value.customerId = Date.now()
    currentInvoice.value.customerName = newCustomerForm.value.tenKhachHang.trim()
    currentInvoice.value.customerPhone = phone
    if (newCustomerForm.value.diaChi) {
      currentInvoice.value.deliveryAddress = newCustomerForm.value.diaChi
    }
    showQuickAddCustomerModal.value = false
    alert('Đã áp dụng thông tin khách hàng cho đơn!')
  }
}

/* ==========================================================
   4. QUẢN LÝ PHIẾU GIẢM GIÁ (VOUCHER)
========================================================== */
const availableVouchers = ref([
  { id: 1, maPhieuGiamGia: 'PGG01', tenPhieuGiamGia: 'Voucher Giảm 50k', loaiPhieuGiamGia: 1, giaTriGiamGia: 50000, hoaDonToiThieu: 200000 },
  { id: 2, maPhieuGiamGia: 'PGG02', tenPhieuGiamGia: 'Voucher Giảm 15% khách mới', loaiPhieuGiamGia: 2, giaTriGiamGia: 15, giamToiDa: 100000, hoaDonToiThieu: 150000 }
])

const fetchVouchers = async () => {
  try {
    const res = await api.get('/api/phieu-giam-gia')
    const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    if (list.length > 0) {
      availableVouchers.value = list.filter(v => Number(v.trangThai) === 1 || v.trangThai == null)
    }
  } catch (e) {
    console.warn('Không tải được phiếu giảm giá từ API, sử dụng voucher mẫu:', e)
  }
}

const formattedVouchers = computed(() => {
  return availableVouchers.value.map(v => ({
    id: v.id,
    label: `${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${Number(v.loaiPhieuGiamGia) === 2 ? `-${v.giaTriGiamGia}%` : `-${formatMoney(v.giaTriGiamGia)}`})`
  }))
})

const selectedVoucherId = computed({
  get: () => currentInvoice.value?.voucher?.id || '',
  set: (val) => {
    if (!currentInvoice.value) return
    if (!val) {
      currentInvoice.value.voucher = null
    } else {
      currentInvoice.value.voucher = availableVouchers.value.find(v => v.id === val || String(v.id) === String(val)) || null
    }
  }
})

const voucherDiscount = computed(() => {
  if (!currentInvoice.value || !currentInvoice.value.voucher) return 0
  const v = currentInvoice.value.voucher
  const subtotal = totalAmount.value

  if (v.hoaDonToiThieu && subtotal < Number(v.hoaDonToiThieu)) {
    return 0
  }

  if (Number(v.loaiPhieuGiamGia) === 2) {
    const percent = Number(v.giaTriGiamGia || 0)
    let amount = (subtotal * percent) / 100
    if (v.giamToiDa && amount > Number(v.giamToiDa)) {
      amount = Number(v.giamToiDa)
    }
    return Math.min(amount, subtotal)
  } else {
    return Math.min(Number(v.giaTriGiamGia || 0), subtotal)
  }
})

/* ==========================================================
   5. TÍNH TOÁN TIỀN TỆ & TIỀN THỪA
========================================================== */
const totalQuantity = computed(() => {
  if (!currentInvoice.value) return 0
  return currentInvoice.value.cart.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  if (!currentInvoice.value) return 0
  return currentInvoice.value.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const shippingFee = computed(() => {
  if (!currentInvoice.value || !currentInvoice.value.isDelivery) return 0
  return Number(currentInvoice.value.shippingFee || 30000)
})

const finalTotal = computed(() => {
  return Math.max(0, totalAmount.value + shippingFee.value - voucherDiscount.value)
})

const changeAmount = computed(() => {
  if (!currentInvoice.value) return 0
  if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') return 0
  const pay = Number(currentInvoice.value.customerPay || 0)
  return Math.max(0, pay - finalTotal.value)
})

const setExactPayment = () => {
  currentInvoice.value.customerPay = finalTotal.value
}

const addQuickCash = (delta) => {
  const current = Number(currentInvoice.value.customerPay || 0)
  currentInvoice.value.customerPay = current + delta
}

watch(() => currentInvoice.value?.paymentMethod, (newVal) => {
  if (newVal === 'CHUYEN_KHOAN') {
    currentInvoice.value.customerPay = finalTotal.value
  }
})

watch(() => currentInvoice.value?.isDelivery, (isDel) => {
  if (isDel && (!currentInvoice.value.shippingFee || currentInvoice.value.shippingFee === 0)) {
    currentInvoice.value.shippingFee = 30000
  }
})

const formatMoney = (val) => {
  return Number(val || 0).toLocaleString('vi-VN') + ' đ'
}

/* ==========================================================
   6. THANH TOÁN ĐƠN HÀNG (POS CHECKOUT)
========================================================== */
const isProcessing = ref(false)

const handleCheckout = async () => {
  if (currentInvoice.value.cart.length === 0) {
    alert('Giỏ hàng trống! Vui lòng chọn sản phẩm trước khi thanh toán.')
    return
  }

  if (currentInvoice.value.isDelivery) {
    if (!currentInvoice.value.customerPhone?.trim()) {
      alert('Vui lòng nhập số điện thoại người nhận khi chọn giao hàng!')
      return
    }
    if (!currentInvoice.value.deliveryAddress?.trim()) {
      alert('Vui lòng nhập địa chỉ nhận hàng khi chọn giao hàng!')
      return
    }
  }

  if (currentInvoice.value.customerPhone?.trim()) {
    const phone = currentInvoice.value.customerPhone.trim()
    if (!/^(0[3|5|7|8|9])[0-9]{8}$/.test(phone)) {
      alert('Số điện thoại không đúng định dạng 10 số (VD: 0912345678)!')
      return
    }
  }

  if (currentInvoice.value.paymentMethod === 'TIEN_MAT') {
    const pay = Number(currentInvoice.value.customerPay || 0)
    if (pay < finalTotal.value) {
      alert(`Khách thanh toán chưa đủ! Cần thanh toán: ${formatMoney(finalTotal.value)} (Còn thiếu: ${formatMoney(finalTotal.value - pay)})`)
      return
    }
  }

  try {
    isProcessing.value = true

    const payload = {
      maHoaDon: currentInvoice.value.code,
      loaiDon: currentInvoice.value.isDelivery ? 2 : 1,
      idKhachHang: currentInvoice.value.customerId || null,
      idPhieuGiamGia: currentInvoice.value.voucher?.id || null,
      tenKhachHang: currentInvoice.value.customerName?.trim() || 'Khách lẻ',
      soDienThoaiKhachHang: currentInvoice.value.customerPhone?.trim() || '',
      diaChiNhanHang: currentInvoice.value.isDelivery ? currentInvoice.value.deliveryAddress?.trim() : 'Bán tại quầy',
      tongTien: totalAmount.value,
      tongTienGiamGia: voucherDiscount.value,
      phiShip: shippingFee.value,
      trangThai: currentInvoice.value.isDelivery ? 1 : 5,
      trangThaiThanhToan: 1,
      phuongThucThanhToan: currentInvoice.value.paymentMethod === 'TIEN_MAT' ? 'Tiền mặt' : (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN' ? 'Chuyển khoản' : 'Kết hợp'),
      ghiChu: currentInvoice.value.note?.trim() || (currentInvoice.value.isDelivery ? 'Đơn giao hàng từ POS' : 'Thanh toán tại quầy'),
      chiTietList: currentInvoice.value.cart.map(item => ({
        sanPhamId: item.id,
        idChiTietSanPham: item.id,
        soLuong: item.quantity,
        giaDuocTinh: item.price,
        donGia: item.price
      }))
    }

    let res = null
    try {
      res = await api.post('/api/hoa-don/pos', payload)
    } catch (posErr) {
      console.warn('API /api/hoa-don/pos chưa khả dụng, fallback /api/hoa-don:', posErr)
      res = await api.post('/api/hoa-don', payload)
    }

    const savedCode = res?.data?.maHoaDon || currentInvoice.value.code
    if (savedCode) {
      existingInvoiceCodes.value.add(savedCode.toUpperCase())
    }
    alert(`Thanh toán đơn hàng ${savedCode} thành công!`)

    // Reset hoặc chuyển tab hóa đơn
    if (waitingInvoices.value.length === 1) {
      waitingInvoices.value = [createEmptyInvoice(Date.now(), generateNextCode())]
      activeInvoiceId.value = waitingInvoices.value[0].id
    } else {
      const remaining = waitingInvoices.value.filter(inv => inv.id !== activeInvoiceId.value)
      waitingInvoices.value = remaining
      activeInvoiceId.value = remaining[0].id
    }

    // Chuyển hướng tới chi tiết hóa đơn vừa tạo
    router.push(`/hoa-don/${savedCode}`)
  } catch (error) {
    console.error('Lỗi khi thanh toán đơn hàng:', error)
    alert('Lỗi khi kết nối hệ thống thanh toán: ' + (error.response?.data?.message || error.message))
  } finally {
    isProcessing.value = false
  }
}

onMounted(async () => {
  await fetchExistingInvoiceCodes()
  fetchProductVariants()
  fetchVouchers()
})
</script>

<template>
  <div class="pos-page-wrapper">
    <!-- TIÊU ĐỀ TRANG -->
    <div class="page-breadcrumb">
      <router-link to="/ban-hang" class="crumb-parent">Quản lý bán hàng</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Bán hàng tại quầy</span>
    </div>

    <!-- KHUNG CHÍNH 2 CỘT -->
    <div class="pos-grid-container">
      <!-- ==================== CỘT TRÁI (HÓA ĐƠN CHỜ + GIỎ HÀNG) ==================== -->
      <div class="pos-col-left">
        <!-- Card 1: Hóa đơn chờ -->
        <div class="pos-panel-card">
          <div class="waiting-bar-top">
            <button class="btn-create-invoice" @click="addWaitingInvoice">
              + Thêm hóa đơn chờ
            </button>
            <span class="counter-text">{{ waitingInvoices.length }}/5 hóa đơn</span>
          </div>

          <!-- Danh sách tab hóa đơn chờ -->
          <div class="tabs-scroll-wrapper">
            <div
                v-for="inv in waitingInvoices"
                :key="inv.id"
                class="inv-tab-pill"
                :class="{ active: activeInvoiceId === inv.id }"
                @click="selectInvoice(inv.id)"
            >
              <span>{{ inv.code }}</span>
              <span class="inv-tab-count" v-if="inv.cart.length > 0">
                ({{ inv.cart.reduce((sum, item) => sum + item.quantity, 0) }})
              </span>
            </div>
          </div>
        </div>

        <!-- Card 2: Giỏ hàng -->
        <div class="pos-panel-card cart-panel">
          <div class="cart-head-bar">
            <div class="cart-title-wrap">
              <span class="cart-title-text">Giỏ hàng</span>
              <span class="cart-count-badge">{{ totalQuantity }} sản phẩm</span>
            </div>

            <div class="cart-actions-right">
              <!-- Ô nhập/quét Barcode nhanh -->
              <div class="barcode-input-wrapper">
                <input
                    type="text"
                    v-model="barcodeInput"
                    @keyup.enter="handleBarcodeScan"
                    placeholder="Quét Barcode / Mã..."
                    class="barcode-quick-input"
                />
                <button class="btn-scan-barcode" @click="handleBarcodeScan" title="Thêm theo mã">
                  <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none">
                    <path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2"></path>
                    <line x1="8" y1="12" x2="16" y2="12"></line>
                  </svg>
                </button>
              </div>

              <!-- Nút Chọn sản phẩm từ danh mục -->
              <button class="btn-open-select" @click="showProductModal = true">
                + Chọn sản phẩm
              </button>
            </div>
          </div>

          <!-- Bảng Giỏ hàng -->
          <div class="cart-table-container">
            <table class="pos-custom-table">
              <thead>
              <tr>
                <th style="width: 50px;">STT</th>
                <th style="width: 120px;">Mã SP</th>
                <th>Tên sản phẩm</th>
                <th style="width: 100px;">Màu sắc</th>
                <th style="width: 80px;">Kích cỡ</th>
                <th style="width: 130px; text-align: center;">Số lượng</th>
                <th style="width: 120px; text-align: right;">Đơn giá</th>
                <th style="width: 70px; text-align: center;">Thao tác</th>
              </tr>
              </thead>
              <tbody>
              <tr v-if="currentInvoice.cart.length === 0">
                <td colspan="8" class="empty-cart-message">
                  Giỏ hàng trống. Vui lòng bấm <b>"+ Chọn sản phẩm"</b> hoặc nhập mã để thêm hàng.
                </td>
              </tr>

              <tr v-else v-for="(item, idx) in currentInvoice.cart" :key="item.id">
                <td class="text-muted">{{ idx + 1 }}</td>
                <td class="code-highlight">{{ item.variantCode || item.code }}</td>
                <td class="font-600">{{ item.name }}</td>
                <td>{{ item.color }}</td>
                <td>{{ item.size }}</td>
                <td class="text-center">
                  <div class="pos-qty-box">
                    <button class="qty-btn" @click="updateQuantity(item, -1)">-</button>
                    <input
                        type="number"
                        :value="item.quantity"
                        @change="handleQuantityInput(item, $event)"
                        class="qty-input"
                        min="1"
                        :max="item.stock"
                    />
                    <button class="qty-btn" @click="updateQuantity(item, 1)">+</button>
                  </div>
                </td>
                <td class="text-right font-price">{{ formatMoney(item.price) }}</td>
                <td class="text-center">
                  <button class="btn-trash-row" @click="removeFromCart(item)" title="Xóa khỏi giỏ">
                    🗑️
                  </button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ==================== CỘT PHẢI (KHÁCH HÀNG & THANH TOÁN) ==================== -->
      <div class="pos-col-right">
        <!-- 1. Card Khách hàng -->
        <div class="pos-panel-card right-card">
          <div class="cust-card-header">
            <h4 class="card-bold-title" style="margin: 0;">Khách hàng</h4>
            <span v-if="!currentInvoice.customerId && !currentInvoice.customerName" class="badge-guest-pill">Mặc định: Khách lẻ</span>
          </div>

          <!-- Đã chọn khách hàng cụ thể -->
          <div v-if="currentInvoice.customerId || currentInvoice.customerName" class="selected-customer-pill" style="margin-top: 10px;">
            <div class="cust-info">
              <span class="cust-name">👤 {{ currentInvoice.customerName }}</span>
              <span class="cust-phone" v-if="currentInvoice.customerPhone">({{ currentInvoice.customerPhone }})</span>
            </div>
            <button class="btn-clear-cust" @click="clearSelectedCustomer" title="Đổi sang Khách lẻ">✕ Bỏ chọn</button>
          </div>

          <!-- Tìm kiếm khách hàng -->
          <div v-else class="customer-search-relative" style="margin-top: 10px;">
            <div class="customer-search-row">
              <input
                  type="text"
                  v-model="currentInvoice.customerPhone"
                  @input="onCustomerInput"
                  @focus="onCustomerInput"
                  placeholder="Nhập tên hoặc SĐT (để trống: Khách lẻ)..."
                  class="pos-form-input"
              />
              <button class="btn-add-customer-quick" @click="openQuickAddModal" title="Thêm mới khách hàng">+</button>
            </div>

            <!-- Autocomplete danh sách khách hàng tìm thấy -->
            <div v-if="showCustomerDropdown && customerSearchResults.length > 0" class="customer-dropdown-menu">
              <div
                  v-for="c in customerSearchResults"
                  :key="c.id"
                  class="customer-dropdown-item"
                  @click="selectCustomer(c)"
              >
                <div class="c-name">{{ c.tenKhachHang }}</div>
                <div class="c-meta">{{ c.soDienThoai }} <span v-if="c.maKhachHang">({{ c.maKhachHang }})</span></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 2. Card Thông tin đơn hàng & Thanh toán -->
        <div class="pos-panel-card right-card checkout-panel">
          <!-- Hàng tiêu đề & Switch giao hàng -->
          <div class="order-header-toggle">
            <span class="order-title-brand">Thông tin đơn hàng</span>
            <label class="delivery-switch-wrap">
              <span>Giao hàng</span>
              <input type="checkbox" v-model="currentInvoice.isDelivery" />
              <span class="switch-ui"></span>
            </label>
          </div>

          <!-- Nhập địa chỉ & phí ship khi bật Giao hàng -->
          <div v-if="currentInvoice.isDelivery" class="delivery-box-expand">
            <div class="pos-form-group">
              <label class="group-label-gray">Địa chỉ nhận hàng <span class="text-danger">*</span></label>
              <input
                  type="text"
                  v-model="currentInvoice.deliveryAddress"
                  placeholder="Nhập số nhà, tên đường, phường xã..."
                  class="pos-form-input"
              />
            </div>
            <div class="pos-form-group">
              <label class="group-label-gray">Phí vận chuyển (đ)</label>
              <input
                  type="number"
                  v-model.number="currentInvoice.shippingFee"
                  placeholder="30000"
                  class="pos-form-input"
              />
            </div>
          </div>

          <!-- Chọn phiếu giảm giá (Voucher) -->
          <div class="pos-form-group">
            <label class="group-label-gray">Phiếu giảm giá (Voucher)</label>
            <Select2Combobox
              v-model="selectedVoucherId"
              :options="formattedVouchers"
              value-key="id"
              label-key="label"
              placeholder="-- Không áp dụng voucher --"
              all-label="-- Không áp dụng voucher --"
              search-placeholder="Tìm kiếm mã hoặc tên voucher..."
            />
          </div>

          <!-- Dòng tóm tắt tiền tệ -->
          <div class="pos-summary-line">
            <span class="line-label">Tổng tiền hàng ({{ totalQuantity }} SP):</span>
            <span class="line-value font-bold-dark">{{ formatMoney(totalAmount) }}</span>
          </div>

          <div v-if="voucherDiscount > 0" class="pos-summary-line discount-line">
            <span class="line-label">Giảm giá voucher:</span>
            <span class="line-value text-discount-bold">- {{ formatMoney(voucherDiscount) }}</span>
          </div>

          <div v-if="currentInvoice.isDelivery" class="pos-summary-line">
            <span class="line-label">Phí vận chuyển:</span>
            <span class="line-value">+ {{ formatMoney(shippingFee) }}</span>
          </div>

          <div class="pos-summary-line total-highlight-line">
            <span class="line-label total-label">Khách cần trả:</span>
            <span class="line-value total-value">{{ formatMoney(finalTotal) }}</span>
          </div>

          <!-- Hình thức thanh toán -->
          <div class="pos-form-group">
            <label class="group-label-gray">Hình thức thanh toán</label>
            <div class="payment-radio-group">
              <label class="radio-item">
                <input type="radio" value="TIEN_MAT" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Tiền mặt</span>
              </label>
              <label class="radio-item">
                <input type="radio" value="CHUYEN_KHOAN" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Chuyển khoản</span>
              </label>
              <label class="radio-item">
                <input type="radio" value="KET_HOP" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Kết hợp</span>
              </label>
            </div>
          </div>

          <!-- Khách thanh toán -->
          <div class="pos-form-group">
            <div class="pay-input-header">
              <label class="group-label-gray">Khách thanh toán</label>
              <button
                  type="button"
                  class="btn-exact-pay"
                  @click="setExactPayment"
              >
                Đủ tiền ({{ formatMoney(finalTotal) }})
              </button>
            </div>

            <input
                type="number"
                v-model="currentInvoice.customerPay"
                placeholder="Nhập số tiền khách đưa..."
                class="pos-form-input input-money"
            />

            <!-- Các nút gợi ý tiền nhanh -->
            <div class="quick-cash-row">
              <button type="button" class="btn-cash-chip" @click="addQuickCash(50000)">+50k</button>
              <button type="button" class="btn-cash-chip" @click="addQuickCash(100000)">+100k</button>
              <button type="button" class="btn-cash-chip" @click="addQuickCash(200000)">+200k</button>
              <button type="button" class="btn-cash-chip" @click="addQuickCash(500000)">+500k</button>
            </div>
          </div>

          <!-- Tiền thừa trả khách -->
          <div class="pos-summary-line">
            <span class="line-label">Tiền thừa trả khách:</span>
            <div class="change-val-wrap">
              <span class="line-value text-change-bold">{{ formatMoney(changeAmount) }}</span>
              <span
                  v-if="currentInvoice.paymentMethod === 'TIEN_MAT' && (currentInvoice.customerPay || 0) < finalTotal"
                  class="text-missing-warning"
              >
                (Thiếu {{ formatMoney(finalTotal - (currentInvoice.customerPay || 0)) }})
              </span>
            </div>
          </div>

          <!-- Ghi chú thanh toán -->
          <div class="pos-form-group">
            <label class="group-label-gray">Ghi chú đơn hàng</label>
            <textarea
                rows="2"
                v-model="currentInvoice.note"
                placeholder="Ghi chú thêm nếu cần..."
                class="pos-form-textarea"
            ></textarea>
          </div>

          <!-- 2 Nút cuối: Hủy hóa đơn & Thanh toán -->
          <div class="checkout-actions-bottom">
            <button class="btn-cancel-bill" @click="removeCurrentInvoice">
              Hủy hóa đơn
            </button>
            <button
                class="btn-submit-checkout"
                :disabled="isProcessing || currentInvoice.cart.length === 0"
                @click="handleCheckout"
            >
              {{ isProcessing ? 'Đang xử lý...' : 'Thanh toán' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== POPUP CHỌN SẢN PHẨM ==================== -->
    <div v-if="showProductModal" class="pos-modal-backdrop" @click.self="showProductModal = false">
      <div class="pos-modal-card">
        <div class="modal-top">
          <h3>Chọn sản phẩm biến thể áo phông</h3>
          <button class="btn-modal-close" @click="showProductModal = false">✕</button>
        </div>

        <div class="modal-search-box">
          <input
              type="text"
              v-model="searchProductText"
              placeholder="Tìm theo tên áo phông, mã CTSP, màu sắc, size..."
              class="pos-form-input"
          />
        </div>

        <div class="modal-product-list">
          <div
              v-for="p in filteredProducts"
              :key="p.id"
              class="modal-product-card"
              :class="{ 'card-out-of-stock': p.stock <= 0 }"
              @click="p.stock > 0 && selectProduct(p)"
          >
            <img :src="p.image" :alt="p.name" class="modal-p-img" />
            <div class="modal-p-details">
              <div class="modal-p-name">{{ p.name }}</div>
              <div class="modal-p-meta">
                <span>Mã: <b>{{ p.variantCode || p.code }}</b></span> |
                <span>Màu: {{ p.color }}</span> |
                <span>Size: {{ p.size }}</span>
              </div>
              <div class="modal-p-price-row">
                <span class="modal-p-price">{{ formatMoney(p.price) }}</span>
                <span class="modal-p-stock" :class="{ 'stock-zero': p.stock <= 0 }">
                  {{ p.stock > 0 ? `Còn lại: ${p.stock}` : 'Hết hàng' }}
                </span>
              </div>
            </div>
            <button
                class="btn-mini-add"
                :disabled="p.stock <= 0"
            >
              {{ p.stock > 0 ? '+ Thêm' : 'Hết' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== POPUP THÊM NHANH KHÁCH HÀNG ==================== -->
    <div v-if="showQuickAddCustomerModal" class="pos-modal-backdrop" @click.self="showQuickAddCustomerModal = false">
      <div class="pos-modal-card quick-customer-card">
        <div class="modal-top">
          <h3>Thêm nhanh khách hàng mới</h3>
          <button class="btn-modal-close" @click="showQuickAddCustomerModal = false">✕</button>
        </div>

        <div class="modal-body-form">
          <div class="pos-form-group">
            <label class="group-label-gray">Tên khách hàng <span class="text-danger">*</span></label>
            <input
                type="text"
                v-model="newCustomerForm.tenKhachHang"
                placeholder="Nhập họ và tên..."
                class="pos-form-input"
                :class="{ 'input-error': newCustomerErrors.tenKhachHang }"
            />
            <span v-if="newCustomerErrors.tenKhachHang" class="error-text">{{ newCustomerErrors.tenKhachHang }}</span>
          </div>

          <div class="pos-form-group">
            <label class="group-label-gray">Số điện thoại <span class="text-danger">*</span></label>
            <input
                type="text"
                v-model="newCustomerForm.soDienThoai"
                placeholder="Nhập số điện thoại 10 số (VD: 0912345678)..."
                class="pos-form-input"
                :class="{ 'input-error': newCustomerErrors.soDienThoai }"
            />
            <span v-if="newCustomerErrors.soDienThoai" class="error-text">{{ newCustomerErrors.soDienThoai }}</span>
          </div>

          <div class="pos-form-group">
            <label class="group-label-gray">Email</label>
            <input
                type="email"
                v-model="newCustomerForm.email"
                placeholder="Nhập email nếu có..."
                class="pos-form-input"
            />
          </div>

          <div class="pos-form-group">
            <label class="group-label-gray">Địa chỉ</label>
            <input
                type="text"
                v-model="newCustomerForm.diaChi"
                placeholder="Nhập địa chỉ khách hàng..."
                class="pos-form-input"
            />
          </div>
        </div>

        <div class="modal-actions-bottom">
          <button class="btn-cancel-modal" @click="showQuickAddCustomerModal = false">Đóng</button>
          <button class="btn-submit-modal" @click="submitQuickAddCustomer">Lưu khách hàng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================
   TỔNG THỂ THEO BẢNG MÀU DỰ ÁN FF T-SHIRT
========================================================== */
.pos-page-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  color: #3d4a50;
  box-sizing: border-box;
}

/* Header */
.page-breadcrumb {
  margin-bottom: 16px;
  font-size: 0.88rem;
}

.crumb-parent {
  color: #8a9292;
  text-decoration: none;
}

.crumb-parent:hover {
  color: #496883;
}

.crumb-separator {
  margin: 0 8px;
  color: #c4c4c4;
}

.crumb-current {
  color: #496883;
  font-weight: 600;
}

/* Grid Layout 2 Cột */
.pos-grid-container {
  display: grid;
  grid-template-columns: 1fr 390px;
  gap: 18px;
  align-items: start;
}

.pos-col-left, .pos-col-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Thẻ Card */
.pos-panel-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.03);
  padding: 16px 18px;
}

/* ==================== CỘT TRÁI ==================== */
/* Hóa đơn chờ */
.waiting-bar-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.btn-create-invoice {
  background-color: #ffffff;
  border: 1px solid #d6d0c3;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #496883;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-create-invoice:hover {
  background-color: #f7f5ef;
  border-color: #496883;
}

.counter-text {
  font-size: 0.8rem;
  color: #8a9292;
}

.tabs-scroll-wrapper {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.inv-tab-pill {
  background-color: #f7f5ef;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #647074;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid transparent;
  transition: all 0.2s;
  white-space: nowrap;
}

.inv-tab-pill.active {
  background-color: #eaf1f4;
  color: #496883;
  border-color: #496883;
}

.inv-tab-count {
  font-size: 0.75rem;
  color: #c94a29;
}

/* Giỏ hàng */
.cart-head-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}

.cart-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cart-title-text {
  font-size: 1rem;
  font-weight: 700;
  color: #496883;
}

.cart-count-badge {
  background-color: #fff4e5;
  color: #c94a29;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #f5d6a6;
}

.cart-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.barcode-input-wrapper {
  display: flex;
  align-items: center;
}

.barcode-quick-input {
  width: 145px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid #d6d0c3;
  border-right: none;
  border-radius: 6px 0 0 6px;
  font-size: 0.82rem;
  outline: none;
}

.barcode-quick-input:focus {
  border-color: #496883;
}

.btn-scan-barcode {
  width: 34px;
  height: 34px;
  border-radius: 0 6px 6px 0;
  border: 1px solid #d6d0c3;
  background: #ffffff;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-scan-barcode:hover {
  background-color: #f7f5ef;
}

.btn-open-select {
  background-color: #496883;
  border: 1px solid #496883;
  color: #ffffff;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-open-select:hover {
  opacity: 0.9;
}

/* Bảng Giỏ hàng */
.cart-table-container {
  overflow-x: auto;
}

.pos-custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.pos-custom-table thead th {
  background-color: #f7f5ef;
  color: #496883;
  font-weight: 600;
  padding: 10px 12px;
  border-bottom: 1px solid #e9e5db;
  text-align: left;
}

.pos-custom-table tbody td {
  padding: 10px 12px;
  border-bottom: 1px solid #e9e5db;
  color: #555;
  vertical-align: middle;
}

.empty-cart-message {
  text-align: center;
  padding: 40px !important;
  color: #8a9292;
  font-size: 0.9rem;
}

.code-highlight {
  font-weight: 600;
  color: #496883;
}

.font-600 {
  font-weight: 600;
}

.font-price {
  font-weight: 600;
  color: #c94a29;
}

.pos-qty-box {
  display: inline-flex;
  align-items: center;
  border: 1px solid #d6d0c3;
  border-radius: 4px;
  overflow: hidden;
}

.qty-btn {
  width: 26px;
  height: 26px;
  border: none;
  background: #f7f5ef;
  cursor: pointer;
  font-weight: 600;
  color: #496883;
}

.qty-btn:hover {
  background: #e9e5db;
}

.qty-input {
  width: 36px;
  height: 26px;
  border: none;
  text-align: center;
  font-weight: 600;
  font-size: 0.85rem;
  outline: none;
}

.btn-trash-row {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.btn-trash-row:hover {
  opacity: 1;
}

/* ==================== CỘT PHẢI ==================== */
.card-bold-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #496883;
  margin: 0;
}

.cust-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge-guest-pill {
  font-size: 0.75rem;
  background-color: #f7f5ef;
  color: #496883;
  padding: 2px 8px;
  border-radius: 10px;
  border: 1px solid #d6d0c3;
  font-weight: 600;
}

.customer-search-relative {
  position: relative;
}

.customer-search-row {
  display: flex;
  gap: 8px;
}

.btn-add-customer-quick {
  width: 38px;
  height: 38px;
  border: 1px solid #496883;
  background-color: #eaf1f4;
  color: #496883;
  border-radius: 6px;
  font-size: 1.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.selected-customer-pill {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #eaf1f4;
  border: 1px solid #d2e1e9;
  border-radius: 6px;
  padding: 8px 12px;
}

.cust-name {
  font-weight: 600;
  color: #496883;
  font-size: 0.88rem;
  margin-right: 6px;
}

.cust-phone {
  font-size: 0.82rem;
  color: #647074;
}

.btn-clear-cust {
  background: none;
  border: none;
  color: #c94a29;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-clear-cust:hover {
  text-decoration: underline;
}

.customer-dropdown-menu {
  position: absolute;
  top: 42px;
  left: 0;
  right: 0;
  background: #ffffff;
  border: 1px solid #d6d0c3;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 100;
  max-height: 180px;
  overflow-y: auto;
}

.customer-dropdown-item {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.15s;
}

.customer-dropdown-item:hover {
  background-color: #f7f5ef;
}

.c-name {
  font-weight: 600;
  font-size: 0.86rem;
  color: #333;
}

.c-meta {
  font-size: 0.78rem;
  color: #8a9292;
}

/* Checkout Panel */
.order-header-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.order-title-brand {
  font-size: 0.95rem;
  font-weight: 700;
  color: #496883;
}

/* Switch giao hàng */
.delivery-switch-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: #647074;
  cursor: pointer;
}

.delivery-switch-wrap input {
  display: none;
}

.switch-ui {
  width: 36px;
  height: 20px;
  background-color: #d6d0c3;
  border-radius: 20px;
  position: relative;
  transition: all 0.2s;
}

.switch-ui::after {
  content: '';
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  top: 2px;
  left: 2px;
  transition: all 0.2s;
}

.delivery-switch-wrap input:checked + .switch-ui {
  background-color: var(--blue, #496883);
}

.delivery-switch-wrap input:checked + .switch-ui::after {
  transform: translateX(16px);
}

.delivery-box-expand {
  margin-bottom: 12px;
  padding: 10px;
  background: #f7f5ef;
  border-radius: 6px;
  border: 1px solid #e9e5db;
}

.pos-summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 0.88rem;
  color: #647074;
}

.discount-line {
  color: #2e7d32;
}

.text-discount-bold {
  font-weight: 700;
  color: #2e7d32;
}

.total-highlight-line {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #d6d0c3;
  margin-bottom: 14px;
}

.total-label {
  font-size: 0.95rem;
  font-weight: 700;
  color: #333;
}

.total-value {
  font-size: 1.15rem;
  font-weight: 800;
  color: #c94a29;
}

.font-bold-dark {
  font-weight: 700;
  color: #333;
}

.text-change-bold {
  font-weight: 700;
  color: #2e7d32;
  font-size: 0.95rem;
}

.change-val-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.text-missing-warning {
  font-size: 0.76rem;
  color: #c94a29;
  font-weight: 600;
}

.pos-form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 12px;
}

.group-label-gray {
  font-size: 0.82rem;
  color: #647074;
  font-weight: 600;
}

.payment-radio-group {
  display: flex;
  gap: 14px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #333;
  cursor: pointer;
}

.radio-item input[type="radio"] {
  accent-color: #496883;
  cursor: pointer;
}

.pay-input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-exact-pay {
  background: none;
  border: none;
  color: #496883;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
}

.quick-cash-row {
  display: flex;
  gap: 6px;
  margin-top: 4px;
}

.btn-cash-chip {
  background-color: #f7f5ef;
  border: 1px solid #d6d0c3;
  border-radius: 4px;
  padding: 3px 8px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #496883;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cash-chip:hover {
  background-color: #eaf1f4;
  border-color: #496883;
}

.pos-form-input, .pos-form-textarea, .pos-form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d6d0c3;
  border-radius: 6px;
  font-size: 0.88rem;
  outline: none;
  box-sizing: border-box;
  background-color: #ffffff;
  color: #333;
  transition: border-color 0.2s;
}

.pos-form-input:focus, .pos-form-textarea:focus, .pos-form-select:focus {
  border-color: #496883;
}

.input-money {
  font-weight: 700;
  font-size: 0.95rem;
}

.input-error {
  border-color: #e53935 !important;
}

.error-text {
  font-size: 0.76rem;
  color: #e53935;
}

.text-danger {
  color: #e53935;
}

/* 2 Nút hành động cuối */
.checkout-actions-bottom {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.btn-cancel-bill {
  flex: 1;
  height: 40px;
  background-color: #e9e5db;
  color: #647074;
  border: none;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-bill:hover {
  background-color: #ddd7ca;
}

.btn-submit-checkout {
  flex: 1;
  height: 40px;
  background-color: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit-checkout:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-submit-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ==================== POPUP MODAL ==================== */
.pos-modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.pos-modal-card {
  background: #ffffff;
  width: 90%;
  max-width: 640px;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  padding: 20px;
  max-height: 82vh;
  display: flex;
  flex-direction: column;
}

.quick-customer-card {
  max-width: 480px;
}

.modal-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.modal-top h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #496883;
  font-weight: 700;
}

.btn-modal-close {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: #647074;
}

.modal-search-box {
  margin-bottom: 14px;
}

.modal-product-list {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 55vh;
}

.modal-product-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e9e5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-product-card:hover:not(.card-out-of-stock) {
  border-color: #496883;
  background-color: #f7f5ef;
}

.card-out-of-stock {
  opacity: 0.55;
  background-color: #f9f9f9;
  cursor: not-allowed;
}

.modal-p-img {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
}

.modal-p-details {
  flex: 1;
}

.modal-p-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
}

.modal-p-meta {
  font-size: 0.78rem;
  color: #8a9292;
  margin: 2px 0 4px;
}

.modal-p-price-row {
  display: flex;
  gap: 14px;
}

.modal-p-price {
  font-weight: 700;
  color: #c94a29;
  font-size: 0.88rem;
}

.modal-p-stock {
  font-size: 0.78rem;
  color: #2e7d32;
  font-weight: 600;
}

.stock-zero {
  color: #c94a29 !important;
}

.btn-mini-add {
  background-color: #eaf1f4;
  color: #496883;
  border: 1px solid #496883;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-mini-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: #ccc;
  color: #999;
  background-color: #f0f0f0;
}

.modal-body-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.modal-actions-bottom {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn-cancel-modal {
  padding: 8px 16px;
  background-color: #e9e5db;
  color: #647074;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit-modal {
  padding: 8px 16px;
  background-color: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.text-center { text-align: center; }
.text-right { text-align: right; }
.text-muted { color: #8a9292; }

@media (max-width: 1024px) {
  .pos-grid-container {
    grid-template-columns: 1fr;
  }
}
</style>