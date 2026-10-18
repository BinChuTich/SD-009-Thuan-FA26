<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()

/* ==========================================================
   TOAST & MODAL SYSTEM (Thay thế hoàn toàn alert/confirm)
========================================================== */
const toast = ref({
  show: false,
  message: '',
  type: 'success'
})
let toastTimer = null
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
    toastTimer = null
  }, 2800)
}

const showConfirmModal = ref(false)
const confirmModalData = ref({
  title: '',
  message: '',
  onConfirm: () => {}
})
const openConfirm = (title, message, onConfirm) => {
  confirmModalData.value = {
    title,
    message,
    onConfirm: () => {
      showConfirmModal.value = false
      onConfirm()
    }
  }
  showConfirmModal.value = true
}

/* ==========================================================
   1. QUẢN LÝ HÓA ĐƠN CHỜ TRÊN CSDL (Tối đa 5 hóa đơn)
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

const waitingInvoices = ref([])
const activeInvoiceId = ref(null)
const isLoadingInvoices = ref(false)

// Hóa đơn hiện tại đang chọn
const currentInvoice = computed(() => {
  if (waitingInvoices.value.length === 0) {
    return createEmptyInvoice(null, '---')
  }
  return waitingInvoices.value.find(inv => inv.id === activeInvoiceId.value) || waitingInvoices.value[0]
})

// Tải danh sách hóa đơn chờ từ CSDL
const loadWaitingInvoices = async () => {
  try {
    isLoadingInvoices.value = true
    let res = null
    try {
      res = await api.get('/api/hoa-don/waiting')
    } catch (err) {
      console.warn('API /api/hoa-don/waiting chưa hỗ trợ, fallback /api/hoa-don:', err)
      const allRes = await api.get('/api/hoa-don')
      const all = Array.isArray(allRes.data) ? allRes.data : []
      res = {
        data: all.filter(h => Number(h.loaiDon) === 1 && Number(h.trangThai) === 1 && (h.trangThaiThanhToan == null || Number(h.trangThaiThanhToan) === 0))
      }
    }

    let list = Array.isArray(res?.data) ? res.data : []

    // Nếu chưa có bất kỳ hóa đơn chờ nào trong CSDL, tự động tạo mới 1 hóa đơn chờ
    if (list.length === 0) {
      try {
        const createRes = await api.post('/api/hoa-don/waiting')
        if (createRes?.data) {
          list = [createRes.data]
        }
      } catch (err) {
        // Fallback tạo qua /api/hoa-don
        const createRes = await api.post('/api/hoa-don', {
          loaiDon: 1,
          trangThai: 1,
          trangThaiThanhToan: 0,
          tenKhachHang: 'Khách lẻ',
          diaChiNhanHang: 'Bán tại quầy',
          tongTien: 0,
          phiVanChuyen: 0,
          tienSauGiamGia: 0,
          ghiChu: 'Hóa đơn chờ tại quầy'
        })
        if (createRes?.data) {
          list = [createRes.data]
        }
      }
    }

    // Giới hạn tối đa 5 hóa đơn chờ
    list = list.slice(0, 5)

    // Tải chi tiết sản phẩm cho từng hóa đơn chờ
    const loadedTabs = await Promise.all(list.map(async (inv) => {
      let cartItems = []
      try {
        const detailRes = await api.get(`/api/hoa-don/${inv.id}/chi-tiet`)
        const items = Array.isArray(detailRes.data) ? detailRes.data : []
        cartItems = items.map(ct => {
          const ctsp = ct.idChiTietSanPham || {}
          const sp = ctsp.idSanPham || {}
          const ms = ctsp.idMauSac || {}
          const kc = ctsp.idKichCo || {}
          return {
            id: ctsp.id || ct.id,
            chiTietHoaDonId: ct.id,
            code: sp.maSanPham || ctsp.maChiTietSanPham || '',
            variantCode: ctsp.maChiTietSanPham || '',
            name: sp.tenSanPham || 'Áo phông',
            color: ms.tenMau || ms.tenMauSac || ct.tenMauSac || 'Mặc định',
            size: kc.tenKichCo || ct.tenKichCo || 'Freesize',
            stock: ctsp.soLuong != null ? Number(ctsp.soLuong) : 50,
            price: ct.donGia != null ? Number(ct.donGia) : (ctsp.giaBan != null ? Number(ctsp.giaBan) : 0),
            quantity: ct.soLuong || 1,
            image: ct.anhDaiDien || ctsp.anhDaiDien || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
          }
        })
      } catch (e) {
        console.warn(`Không tải được chi tiết cho HĐ ${inv.maHoaDon}:`, e)
      }

      const rawCust = inv.tenKhachHang
      const isKhachLe = !rawCust || rawCust.trim().toLowerCase() === 'khách lẻ'

      return {
        id: inv.id,
        code: inv.maHoaDon,
        cart: cartItems,
        customerId: inv.idKhachHang?.id || null,
        customerName: isKhachLe ? '' : rawCust,
        customerPhone: inv.soDienThoaiKhachHang || '',
        isDelivery: Number(inv.loaiDon) === 2,
        deliveryAddress: (inv.diaChiNhanHang && inv.diaChiNhanHang !== 'Bán tại quầy') ? inv.diaChiNhanHang : '',
        shippingFee: Number(inv.phiVanChuyen || 0),
        voucher: inv.idPhieuGiamGia || null,
        paymentMethod: 'TIEN_MAT',
        customerPay: null,
        note: (inv.ghiChu && inv.ghiChu !== 'Hóa đơn chờ tại quầy') ? inv.ghiChu : ''
      }
    }))

    waitingInvoices.value = loadedTabs
    if (loadedTabs.length > 0) {
      activeInvoiceId.value = loadedTabs[0].id
    }
  } catch (err) {
    console.error('Lỗi khi tải hóa đơn chờ từ CSDL:', err)
    showToast('Lỗi khi tải danh sách hóa đơn chờ từ hệ thống!', 'warning')
  } finally {
    isLoadingInvoices.value = false
  }
}

// Thêm hóa đơn chờ mới (lưu thẳng vào CSDL, tối đa 5 hóa đơn)
const addWaitingInvoice = async () => {
  if (waitingInvoices.value.length >= 5) {
    showToast('Chỉ được tạo tối đa 5 hóa đơn chờ cùng lúc!', 'warning')
    return
  }

  try {
    let newInv = null
    try {
      const res = await api.post('/api/hoa-don/waiting')
      newInv = res.data
    } catch {
      const res = await api.post('/api/hoa-don', {
        loaiDon: 1,
        trangThai: 1,
        trangThaiThanhToan: 0,
        tenKhachHang: 'Khách lẻ',
        diaChiNhanHang: 'Bán tại quầy',
        tongTien: 0,
        phiVanChuyen: 0,
        tienSauGiamGia: 0,
        ghiChu: 'Hóa đơn chờ tại quầy'
      })
      newInv = res.data
    }

    if (newInv) {
      const newTab = {
        id: newInv.id,
        code: newInv.maHoaDon,
        cart: [],
        customerId: null,
        customerName: '',
        customerPhone: '',
        isDelivery: false,
        deliveryAddress: '',
        shippingFee: 0,
        voucher: null,
        paymentMethod: 'TIEN_MAT',
        customerPay: null,
        note: ''
      }
      waitingInvoices.value.push(newTab)
      activeInvoiceId.value = newInv.id
      showToast(`Đã tạo hóa đơn chờ ${newInv.maHoaDon}!`)
    }
  } catch (err) {
    console.error('Lỗi khi tạo hóa đơn chờ:', err)
    showToast('Không thể tạo hóa đơn chờ mới trong CSDL!', 'warning')
  }
}

// Chuyển tab hóa đơn
const selectInvoice = (id) => {
  activeInvoiceId.value = id
}

// Hủy hóa đơn chờ cụ thể
const confirmRemoveInvoice = (inv) => {
  if (!inv) return
  openConfirm(
    'Xác nhận hủy hóa đơn',
    `Bạn có chắc chắn muốn hủy hóa đơn chờ "${inv.code}" không? Hóa đơn này sẽ được xóa hoàn toàn khỏi cơ sở dữ liệu.`,
    async () => {
      try {
        if (inv.id) {
          await api.delete(`/api/hoa-don/${inv.id}`)
        }
        waitingInvoices.value = waitingInvoices.value.filter(i => i.id !== inv.id)
        showToast(`Đã hủy thành công hóa đơn ${inv.code}!`)

        // Nếu đã xóa hết, tự động tạo mới 1 hóa đơn chờ trên DB
        if (waitingInvoices.value.length === 0) {
          await addWaitingInvoice()
        } else {
          if (activeInvoiceId.value === inv.id) {
            activeInvoiceId.value = waitingInvoices.value[0].id
          }
        }
      } catch (err) {
        console.error('Lỗi khi hủy hóa đơn:', err)
        showToast('Không thể hủy hóa đơn khỏi cơ sở dữ liệu!', 'warning')
      }
    }
  )
}

// Nút Hủy hóa đơn ở góc dưới bên phải
const removeCurrentInvoice = () => {
  if (currentInvoice.value) {
    confirmRemoveInvoice(currentInvoice.value)
  }
}

/* ==========================================================
   2. DANH SÁCH SẢN PHẨM & TỒN KHO & QUÉT BARCODE
========================================================== */
const showProductModal = ref(false)
const searchProductText = ref('')
const barcodeInput = ref('')
const isLoadingProducts = ref(false)
const productList = ref([])

// Gọi API lấy toàn bộ biến thể sản phẩm đang bán (HOÀN TOÀN TỪ CSDL, KHÔNG DÙNG MOCK)
const fetchProductVariants = async () => {
  try {
    isLoadingProducts.value = true
    const res = await api.get('/api/chi-tiet-san-pham/filter', {
      params: { trangThai: 1, size: 300 }
    })
    const content = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    productList.value = content.map((item, idx) => ({
      id: item.id,
      sanPhamId: item.idSanPham?.id || item.id,
      code: item.idSanPham?.maSanPham || item.maSanPham || item.maChiTietSanPham || `SP00${idx + 1}`,
      variantCode: item.maChiTietSanPham || `CTSP0${idx + 1}`,
      name: item.idSanPham?.tenSanPham || item.tenSanPham || 'Áo Phông Cao Cấp',
      color: item.idMauSac?.tenMau || item.idMauSac?.tenMauSac || item.tenMauSac || 'Mặc định',
      size: item.idKichCo?.tenKichCo || item.tenKichCo || 'Freesize',
      stock: item.soLuong != null ? Number(item.soLuong) : 0,
      price: item.giaBan != null ? Number(item.giaBan) : 0,
      image: item.anhDaiDien || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
    }))
  } catch (e) {
    console.error('Không tải được danh sách CTSP từ CSDL:', e)
    productList.value = []
  } finally {
    isLoadingProducts.value = false
  }
}

// Tự động đồng bộ giỏ hàng hóa đơn chờ với CSDL (debounced)
let syncCartTimer = null
const syncCurrentCartToDb = () => {
  clearTimeout(syncCartTimer)
  if (!currentInvoice.value || !currentInvoice.value.id) return
  syncCartTimer = setTimeout(async () => {
    try {
      const itemsPayload = currentInvoice.value.cart.map(item => ({
        idChiTietSanPham: item.id,
        sanPhamId: item.id,
        soLuong: item.quantity,
        donGia: item.price,
        giaDuocTinh: item.price
      }))
      await api.post(`/api/hoa-don/${currentInvoice.value.id}/sync-cart`, itemsPayload)
    } catch (err) {
      console.warn('Đồng bộ giỏ hàng với CSDL:', err)
    }
  }, 400)
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
    showToast('Sản phẩm đã hết hàng trong kho!', 'warning')
    return
  }

  const exist = currentInvoice.value.cart.find(i => i.id === prod.id)
  if (exist) {
    if (exist.quantity >= prod.stock) {
      showToast(`Trong kho chỉ còn tối đa ${prod.stock} sản phẩm!`, 'warning')
      return
    }
    exist.quantity++
  } else {
    currentInvoice.value.cart.push({
      ...prod,
      quantity: 1
    })
  }

  syncCurrentCartToDb()
  showToast(`Đã thêm "${prod.name} (${prod.color} - ${prod.size})" vào giỏ hàng!`)
}

// Quét barcode / nhập mã nhanh
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
    showToast(`Không tìm thấy sản phẩm có mã "${barcodeInput.value}" trong kho!`, 'warning')
  }
}

const updateQuantity = (item, delta) => {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    removeFromCart(item)
    return
  }
  if (newQty > item.stock) {
    showToast(`Số lượng trong kho chỉ còn tối đa ${item.stock}!`, 'warning')
    return
  }
  item.quantity = newQty
  syncCurrentCartToDb()
}

const handleQuantityInput = (item, event) => {
  let val = parseInt(event.target.value, 10)
  if (isNaN(val) || val <= 0) {
    item.quantity = 1
  } else if (val > item.stock) {
    showToast(`Số lượng trong kho chỉ còn tối đa ${item.stock}!`, 'warning')
    item.quantity = item.stock
  } else {
    item.quantity = val
  }
  syncCurrentCartToDb()
}

const removeFromCart = (item) => {
  currentInvoice.value.cart = currentInvoice.value.cart.filter(i => i.id !== item.id)
  syncCurrentCartToDb()
  showToast('Đã xóa sản phẩm khỏi giỏ hàng!')
}

/* ==========================================================
   3. TÌM KIẾM & THÊM NHANH KHÁCH HÀNG (HOÀN TOÀN CSDL)
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
        params: { keyword: query, size: 8 }
      })
      const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
      customerSearchResults.value = list
      showCustomerDropdown.value = list.length > 0
    } catch (e) {
      customerSearchResults.value = []
      showCustomerDropdown.value = false
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

  // Kiểm tra số voucher cá nhân dành riêng cho khách hàng này
  const personalCount = availableVouchers.value.filter(v => isVoucherPersonal(v) && isVoucherForCustomer(v, cust.id)).length
  if (personalCount > 0) {
    showToast(`Đã chọn "${cust.tenKhachHang}" (Có ${personalCount} phiếu giảm giá cá nhân khả dụng)!`)
  } else {
    showToast(`Đã chọn khách hàng "${cust.tenKhachHang}"!`)
  }
}

const clearSelectedCustomer = () => {
  currentInvoice.value.customerId = null
  currentInvoice.value.customerName = ''
  currentInvoice.value.customerPhone = ''
  currentInvoice.value.deliveryAddress = ''

  // Nếu hóa đơn đang áp dụng phiếu cá nhân thì hủy áp dụng
  if (currentInvoice.value.voucher && isVoucherPersonal(currentInvoice.value.voucher)) {
    const vCode = currentInvoice.value.voucher.maPhieuGiamGia
    currentInvoice.value.voucher = null
    showToast(`Đã chuyển sang Khách lẻ và hủy phiếu cá nhân "${vCode}"!`, 'warning')
  } else {
    showToast('Đã chuyển sang Khách lẻ!')
  }
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
    newCustomerErrors.value.tenKhachHang = 'Vui lòng nhập họ và tên khách hàng'
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
    currentInvoice.value.customerId = created.id
    currentInvoice.value.customerName = created.tenKhachHang || newCustomerForm.value.tenKhachHang.trim()
    currentInvoice.value.customerPhone = created.soDienThoai || phone
    if (newCustomerForm.value.diaChi) {
      currentInvoice.value.deliveryAddress = newCustomerForm.value.diaChi
    }
    showQuickAddCustomerModal.value = false
    showToast('Đã thêm mới khách hàng vào cơ sở dữ liệu!')
  } catch (err) {
    console.error('Lỗi khi thêm khách hàng:', err)
    showToast(err.response?.data?.message || 'Không thể lưu khách hàng vào CSDL!', 'warning')
  }
}

/* ==========================================================
   4. QUẢN LÝ PHIẾU GIẢM GIÁ (CÔNG KHAI & CÁ NHÂN TỪ CSDL)
========================================================== */
const availableVouchers = ref([])

// Helper: Phân biệt phiếu Cá nhân vs phiếu Công khai
const isVoucherPersonal = (v) => {
  if (!v) return false
  if (v.hinhThuc) {
    const h = String(v.hinhThuc).trim().toLowerCase()
    if (h.includes('nhân') || h.includes('nhan')) return true
    if (h.includes('khai')) return false
  }
  if (Array.isArray(v.idKhachHangList) && v.idKhachHangList.length > 0) return true
  if (Array.isArray(v.danhSachKhachHang) && v.danhSachKhachHang.length > 0) return true
  if (Number(v.soKhachHang || 0) > 0) return true
  return false
}

// Helper: Kiểm tra voucher có được phép áp dụng cho khách hàng hiện tại hay không
const isVoucherForCustomer = (v, customerId) => {
  if (!v) return false
  if (!isVoucherPersonal(v)) return true // Phiếu công khai: áp dụng cho tất cả
  if (!customerId) return false // Phiếu cá nhân: yêu cầu phải có khách hàng thành viên
  if (Array.isArray(v.idKhachHangList) && v.idKhachHangList.length > 0) {
    return v.idKhachHangList.some(id => String(id) === String(customerId))
  }
  return false
}

const fetchVouchers = async () => {
  try {
    const res = await api.get('/api/phieu-giam-gia')
    const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    availableVouchers.value = list.filter(v => Number(v.trangThai) === 1 || v.trangThai == null)

    // Đồng bộ lại object voucher đầy đủ cho các tab hóa đơn nếu đã có voucher
    waitingInvoices.value.forEach(inv => {
      if (inv.voucher && inv.voucher.id) {
        const fullV = availableVouchers.value.find(v => v.id === inv.voucher.id)
        if (fullV) {
          if (isVoucherPersonal(fullV) && !isVoucherForCustomer(fullV, inv.customerId)) {
            inv.voucher = null
          } else {
            inv.voucher = fullV
          }
        }
      }
    })
  } catch (e) {
    console.warn('Không tải được phiếu giảm giá từ API:', e)
    availableVouchers.value = []
  }
}

// 1 = Giảm theo % | 2 = Giảm theo tiền mặt (VNĐ)
const isPercentVoucher = (v) => {
  if (!v) return false
  const loai = Number(v.loaiPhieuGiamGia)
  if (loai === 1) return true
  if (loai === 2) return false
  const val = Number(v.giaTriGiamGia || 0)
  return val > 0 && val <= 100
}

const formatVoucherDiscountLabel = (v) => {
  if (isPercentVoucher(v)) {
    return `-${v.giaTriGiamGia}%`
  }
  return `-${formatMoney(v.giaTriGiamGia)}`
}

// Thống kê số lượng phiếu công khai & cá nhân
const publicVouchers = computed(() => {
  return availableVouchers.value.filter(v => !isVoucherPersonal(v))
})
const publicVouchersCount = computed(() => publicVouchers.value.length)

const personalVouchers = computed(() => {
  return availableVouchers.value.filter(v => isVoucherPersonal(v))
})
const personalVouchersCount = computed(() => personalVouchers.value.length)

const customerPersonalVouchers = computed(() => {
  const custId = currentInvoice.value?.customerId
  if (!custId) return []
  return availableVouchers.value.filter(v => isVoucherPersonal(v) && isVoucherForCustomer(v, custId))
})
const customerPersonalCount = computed(() => customerPersonalVouchers.value.length)

// Danh sách voucher hiển thị trong combobox (phân loại rõ ràng [Công khai] và [Cá nhân])
const formattedVouchers = computed(() => {
  const currentCustId = currentInvoice.value?.customerId

  if (currentCustId) {
    const personalForCust = []
    const publicList = []
    const otherPersonal = []

    availableVouchers.value.forEach(v => {
      if (!isVoucherPersonal(v)) {
        publicList.push({
          id: v.id,
          label: `[Công khai] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`
        })
      } else if (isVoucherForCustomer(v, currentCustId)) {
        personalForCust.push({
          id: v.id,
          label: `[Cá nhân] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`
        })
      } else {
        otherPersonal.push({
          id: v.id,
          label: `[Cá nhân - KH khác] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`
        })
      }
    })

    return [...personalForCust, ...publicList, ...otherPersonal]
  } else {
    const publicList = []
    const personalList = []

    availableVouchers.value.forEach(v => {
      if (!isVoucherPersonal(v)) {
        publicList.push({
          id: v.id,
          label: `[Công khai] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`
        })
      } else {
        personalList.push({
          id: v.id,
          label: `[Cá nhân - Cần chọn KH] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`
        })
      }
    })

    return [...publicList, ...personalList]
  }
})

const selectedVoucherId = computed({
  get: () => currentInvoice.value?.voucher?.id || '',
  set: (val) => {
    if (!currentInvoice.value) return
    if (!val) {
      currentInvoice.value.voucher = null
      return
    }

    const found = availableVouchers.value.find(v => v.id === val || String(v.id) === String(val))
    if (!found) {
      currentInvoice.value.voucher = null
      return
    }

    // Kiểm tra tính hợp lệ nếu chọn phiếu cá nhân
    if (isVoucherPersonal(found)) {
      if (!currentInvoice.value.customerId) {
        showToast(`Phiếu "${found.maPhieuGiamGia}" là phiếu CÁ NHÂN. Vui lòng chọn khách hàng thành viên trước khi áp dụng!`, 'warning')
        const prev = currentInvoice.value.voucher
        currentInvoice.value.voucher = null
        nextTick(() => {
          currentInvoice.value.voucher = prev
        })
        return
      }

      if (!isVoucherForCustomer(found, currentInvoice.value.customerId)) {
        const targetNames = (found.danhSachKhachHang && found.danhSachKhachHang.length > 0)
          ? found.danhSachKhachHang.join(', ')
          : 'khách hàng khác'
        showToast(`Phiếu "${found.maPhieuGiamGia}" là phiếu CÁ NHÂN chỉ dành riêng cho: ${targetNames}!`, 'warning')
        const prev = currentInvoice.value.voucher
        currentInvoice.value.voucher = null
        nextTick(() => {
          currentInvoice.value.voucher = prev
        })
        return
      }

      currentInvoice.value.voucher = found
      showToast(`Đã áp dụng phiếu cá nhân "${found.maPhieuGiamGia}" cho khách hàng ${currentInvoice.value.customerName}!`)
    } else {
      currentInvoice.value.voucher = found
      showToast(`Đã áp dụng phiếu giảm giá công khai "${found.maPhieuGiamGia}"!`)
    }
  }
})

const voucherDiscount = computed(() => {
  if (!currentInvoice.value || !currentInvoice.value.voucher) return 0
  const v = currentInvoice.value.voucher
  const subtotal = totalAmount.value

  // Nếu là phiếu cá nhân mà khách hàng không hợp lệ -> không tính giảm giá
  if (isVoucherPersonal(v) && !isVoucherForCustomer(v, currentInvoice.value.customerId)) {
    return 0
  }

  if (v.hoaDonToiThieu && subtotal < Number(v.hoaDonToiThieu)) {
    return 0
  }

  if (isPercentVoucher(v)) {
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

// Tự động mặc định số tiền khách đưa bằng tổng tiền cần thanh toán
watch(
  finalTotal,
  (newTotal, oldTotal) => {
    if (!currentInvoice.value) return
    const curPay = currentInvoice.value.customerPay
    // Nếu chưa nhập tiền, hoặc bằng 0, hoặc trước đó đang bằng đúng oldTotal thì tự động cập nhật theo newTotal
    if (curPay === null || curPay === undefined || curPay === '' || curPay === 0 || (oldTotal != null && Number(curPay) === Number(oldTotal))) {
      currentInvoice.value.customerPay = newTotal > 0 ? newTotal : null
    }
  },
  { immediate: true }
)

// Khi chuyển tab hóa đơn, nếu hóa đơn đó chưa có số tiền khách đưa thì tự động điền bằng finalTotal
watch(
  () => currentInvoice.value?.id,
  () => {
    if (!currentInvoice.value) return
    const curPay = currentInvoice.value.customerPay
    if (curPay === null || curPay === undefined || curPay === '' || curPay === 0) {
      if (finalTotal.value > 0) {
        currentInvoice.value.customerPay = finalTotal.value
      }
    }
  },
  { immediate: true }
)

watch(() => currentInvoice.value?.paymentMethod, (newVal) => {
  if (newVal === 'CHUYEN_KHOAN') {
    currentInvoice.value.customerPay = finalTotal.value
  } else if (newVal === 'TIEN_MAT') {
    const curPay = currentInvoice.value.customerPay
    if (curPay === null || curPay === undefined || curPay === '' || curPay === 0) {
      currentInvoice.value.customerPay = finalTotal.value > 0 ? finalTotal.value : null
    }
  }
})

watch(() => currentInvoice.value?.isDelivery, (isDel) => {
  if (isDel && (!currentInvoice.value.shippingFee || currentInvoice.value.shippingFee === 0)) {
    currentInvoice.value.shippingFee = 30000
  }
})

// Tự động kiểm tra và hủy phiếu cá nhân nếu đổi hoặc xóa khách hàng trên hóa đơn
watch(
  () => currentInvoice.value?.customerId,
  (newCustId, oldCustId) => {
    if (newCustId === oldCustId) return
    const curVoucher = currentInvoice.value?.voucher
    if (curVoucher && isVoucherPersonal(curVoucher)) {
      if (!newCustId || !isVoucherForCustomer(curVoucher, newCustId)) {
        currentInvoice.value.voucher = null
        if (!newCustId) {
          showToast(`Đã hủy phiếu giảm giá cá nhân "${curVoucher.maPhieuGiamGia}" vì chuyển sang Khách lẻ!`, 'warning')
        } else {
          showToast(`Đã hủy phiếu giảm giá cá nhân "${curVoucher.maPhieuGiamGia}" vì không thuộc khách hàng mới!`, 'warning')
        }
      }
    }
  }
)

const formatMoney = (val) => {
  return Number(val || 0).toLocaleString('vi-VN') + ' đ'
}

/* ==========================================================
   6. THANH TOÁN ĐƠN HÀNG (POS CHECKOUT)
========================================================== */
const isProcessing = ref(false)

const handleCheckout = async () => {
  if (currentInvoice.value.cart.length === 0) {
    showToast('Giỏ hàng trống! Vui lòng chọn sản phẩm trước khi thanh toán.', 'warning')
    return
  }

  // Kiểm tra tính hợp lệ của phiếu giảm giá trước khi thanh toán
  if (currentInvoice.value.voucher) {
    const v = currentInvoice.value.voucher
    if (isVoucherPersonal(v) && !isVoucherForCustomer(v, currentInvoice.value.customerId)) {
      showToast(`Phiếu giảm giá "${v.maPhieuGiamGia}" là phiếu CÁ NHÂN không thuộc khách hàng này!`, 'warning')
      return
    }
    if (v.hoaDonToiThieu && totalAmount.value < Number(v.hoaDonToiThieu)) {
      showToast(`Đơn hàng chưa đạt mức tối thiểu ${formatMoney(v.hoaDonToiThieu)} để áp dụng voucher "${v.maPhieuGiamGia}"!`, 'warning')
      return
    }
  }

  if (currentInvoice.value.isDelivery) {
    if (!currentInvoice.value.customerName?.trim() || currentInvoice.value.customerName.trim().toLowerCase() === 'khách lẻ') {
      showToast('Đơn giao hàng bắt buộc phải có tên người nhận cụ thể (không được để "Khách lẻ")!', 'warning')
      return
    }
    if (!currentInvoice.value.customerPhone?.trim()) {
      showToast('Vui lòng nhập số điện thoại người nhận khi chọn giao hàng!', 'warning')
      return
    }
    if (!currentInvoice.value.deliveryAddress?.trim()) {
      showToast('Vui lòng nhập địa chỉ nhận hàng khi chọn giao hàng!', 'warning')
      return
    }
  }

  if (currentInvoice.value.customerPhone?.trim()) {
    const phone = currentInvoice.value.customerPhone.trim()
    if (!/^(0[3|5|7|8|9])[0-9]{8}$/.test(phone)) {
      showToast('Số điện thoại không đúng định dạng 10 số (VD: 0912345678)!', 'warning')
      return
    }
  }

  if (currentInvoice.value.paymentMethod === 'TIEN_MAT') {
    const pay = Number(currentInvoice.value.customerPay || 0)
    if (pay < finalTotal.value) {
      showToast(`Khách thanh toán chưa đủ! Cần thanh toán: ${formatMoney(finalTotal.value)} (Còn thiếu: ${formatMoney(finalTotal.value - pay)})`, 'warning')
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
    showToast(`Thanh toán đơn hàng ${savedCode} thành công!`)

    // Chuyển tab hóa đơn hoặc tạo mới nếu là hóa đơn duy nhất
    const completedId = currentInvoice.value.id
    waitingInvoices.value = waitingInvoices.value.filter(inv => inv.id !== completedId)

    if (waitingInvoices.value.length === 0) {
      await addWaitingInvoice()
    } else {
      activeInvoiceId.value = waitingInvoices.value[0].id
    }

    // Cập nhật lại tồn kho sản phẩm hiển thị trên client
    fetchProductVariants()

    // Chuyển hướng tới chi tiết hóa đơn vừa tạo và tự động in hóa đơn với mẫu chuẩn FF T-SHIRT STORE
    setTimeout(() => {
      router.push({ path: `/hoa-don/${savedCode}`, query: { autoPrint: 'true' } })
    }, 400)
  } catch (error) {
    console.error('Lỗi khi thanh toán đơn hàng:', error)
    showToast('Lỗi khi thanh toán: ' + (error.response?.data?.message || error.message), 'warning')
  } finally {
    isProcessing.value = false
  }
}

onMounted(async () => {
  await loadWaitingInvoices()
  fetchProductVariants()
  fetchVouchers()
})
</script>

<template>
  <div class="pos-page-wrapper">
    <!-- TOAST THÔNG BÁO TỔNG THỂ -->
    <transition name="toast-fade">
      <div v-if="toast.show" class="toast-popup" :class="toast.type">
        <span class="toast-icon">{{ toast.type === 'warning' ? '⚠️' : '✓' }}</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>

    <!-- MODAL XÁC NHẬN HỦY / THAO TÁC HÓA ĐƠN -->
    <div v-if="showConfirmModal" class="pos-modal-backdrop" @click.self="showConfirmModal = false">
      <div class="pos-modal-card confirm-modal-card">
        <div class="confirm-modal-header">
          <span class="confirm-icon">⚠️</span>
          <h4>{{ confirmModalData.title }}</h4>
        </div>
        <p class="confirm-modal-desc">{{ confirmModalData.message }}</p>
        <div class="confirm-modal-actions">
          <button class="btn-modal-back" @click="showConfirmModal = false">Hủy bỏ</button>
          <button class="btn-modal-danger" @click="confirmModalData.onConfirm">Xác nhận</button>
        </div>
      </div>
    </div>

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
            <button class="btn-create-invoice" @click="addWaitingInvoice" :disabled="isLoadingInvoices">
              + Thêm hóa đơn chờ
            </button>
            <span class="counter-text">{{ waitingInvoices.length }}/5 hóa đơn</span>
          </div>

          <!-- Danh sách tab hóa đơn chờ -->
          <div class="tabs-scroll-wrapper">
            <div v-if="isLoadingInvoices" class="tab-loading-indicator">
              <span>Đang tải hóa đơn chờ từ CSDL...</span>
            </div>
            <div
                v-else
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
              <button
                class="btn-close-tab"
                @click.stop="confirmRemoveInvoice(inv)"
                title="Hủy hóa đơn này"
              >✕</button>
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
            <div v-if="showCustomerDropdown" class="customer-dropdown-menu">
              <template v-if="customerSearchResults.length > 0">
                <div
                    v-for="c in customerSearchResults"
                    :key="c.id"
                    class="customer-dropdown-item"
                    @click="selectCustomer(c)"
                >
                  <div class="c-name">{{ c.tenKhachHang }}</div>
                  <div class="c-meta">{{ c.soDienThoai }} <span v-if="c.maKhachHang">({{ c.maKhachHang }})</span></div>
                </div>
              </template>
              <div v-else class="customer-dropdown-empty">
                <span>Không tìm thấy khách hàng trong CSDL.</span>
                <button type="button" class="btn-quick-create-cust" @click="openQuickAddModal">+ Thêm mới</button>
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
            <div class="pos-form-group" v-if="!currentInvoice.customerId">
              <label class="group-label-gray">Tên người nhận hàng <span class="text-danger">*</span></label>
              <input
                  type="text"
                  v-model="currentInvoice.customerName"
                  placeholder="Nhập họ tên người nhận (không để Khách lẻ)..."
                  class="pos-form-input"
              />
            </div>
            <div class="pos-form-group" v-if="!currentInvoice.customerId">
              <label class="group-label-gray">Số điện thoại người nhận <span class="text-danger">*</span></label>
              <input
                  type="text"
                  v-model="currentInvoice.customerPhone"
                  placeholder="VD: 0987654321..."
                  class="pos-form-input"
              />
            </div>
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
          <div class="pos-form-group voucher-group-wrapper">
            <div class="voucher-label-row">
              <label class="group-label-gray">Phiếu giảm giá (Voucher)</label>
              <div class="voucher-stat-badges">
                <span class="badge-v-stat public-stat" title="Phiếu giảm giá công khai áp dụng cho mọi khách hàng">
                  {{ publicVouchersCount }} công khai
                </span>
                <span
                  class="badge-v-stat personal-stat"
                  :class="{ 'has-personal': customerPersonalCount > 0 }"
                  :title="currentInvoice.customerId ? `Có ${customerPersonalCount} phiếu cá nhân của khách hàng này` : `Có ${personalVouchersCount} phiếu cá nhân trong hệ thống`"
                >
                  {{ currentInvoice.customerId ? `${customerPersonalCount} của KH` : `${personalVouchersCount} cá nhân` }}
                </span>
              </div>
            </div>

            <Select2Combobox
              v-model="selectedVoucherId"
              :options="formattedVouchers"
              value-key="id"
              label-key="label"
              placeholder="-- Không áp dụng voucher --"
              all-label="-- Không áp dụng voucher --"
              search-placeholder="Tìm kiếm mã hoặc tên voucher (Công khai / Cá nhân)..."
            />

            <!-- Huy hiệu hiển thị chi tiết khi áp dụng voucher -->
            <div v-if="currentInvoice.voucher" class="selected-voucher-badge-box">
              <div v-if="isVoucherPersonal(currentInvoice.voucher)" class="v-pill-personal">
                <div class="v-pill-content">
                  <div class="v-pill-title">
                    <span class="pill-tag pill-tag-personal">CÁ NHÂN</span>
                    <strong>{{ currentInvoice.voucher.tenPhieuGiamGia }}</strong> ({{ currentInvoice.voucher.maPhieuGiamGia }})
                  </div>
                  <div class="v-pill-sub">
                    Dành riêng cho: <strong>{{ currentInvoice.customerName || 'Khách hàng thành viên' }}</strong> • Mức giảm: <strong class="text-accent-red">{{ formatVoucherDiscountLabel(currentInvoice.voucher) }}</strong>
                  </div>
                </div>
              </div>
              <div v-else class="v-pill-public">
                <div class="v-pill-content">
                  <div class="v-pill-title">
                    <span class="pill-tag pill-tag-public">CÔNG KHAI</span>
                    <strong>{{ currentInvoice.voucher.tenPhieuGiamGia }}</strong> ({{ currentInvoice.voucher.maPhieuGiamGia }})
                  </div>
                  <div class="v-pill-sub">
                    Áp dụng cho mọi khách hàng • Mức giảm: <strong class="text-accent-red">{{ formatVoucherDiscountLabel(currentInvoice.voucher) }}</strong>
                  </div>
                </div>
              </div>
            </div>

            <small
              v-if="currentInvoice.voucher && currentInvoice.voucher.hoaDonToiThieu && totalAmount < Number(currentInvoice.voucher.hoaDonToiThieu)"
              class="voucher-min-alert"
            >
              Đơn hàng tối thiểu {{ formatMoney(currentInvoice.voucher.hoaDonToiThieu) }} để áp dụng voucher này (Hiện tại: {{ formatMoney(totalAmount) }})
            </small>

            <div v-if="!currentInvoice.customerId && personalVouchersCount > 0" class="voucher-guest-hint">
              <em>Đang là Khách lẻ. Hãy chọn khách hàng để mở khóa các phiếu giảm giá cá nhân.</em>
            </div>
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
                @focus="$event.target.select()"
                placeholder="Nhập số tiền khách đưa..."
                class="pos-form-input input-money"
            />

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
          <div v-if="isLoadingProducts" class="modal-loading-box">
            <div class="spinner-dot"></div>
            <span>Đang tải danh sách sản phẩm từ kho CSDL...</span>
          </div>
          <div v-else-if="filteredProducts.length === 0" class="empty-product-box">
            <div style="font-size: 2.2rem; margin-bottom: 8px;">📦</div>
            <p style="font-weight: 600; color: #496883; margin: 0 0 6px;">{{ searchProductText ? 'Không tìm thấy sản phẩm phù hợp với từ khóa!' : 'Chưa có biến thể sản phẩm nào trong cơ sở dữ liệu.' }}</p>
            <p style="font-size: 0.85rem; color: #8a9292; margin: 0 0 12px;">Dữ liệu sản phẩm được đồng bộ trực tiếp từ CSDL hệ thống.</p>
            <router-link to="/san-pham/them" class="btn-create-product-link" v-if="!searchProductText">
              + Thêm mới sản phẩm vào kho
            </router-link>
          </div>
          <div
              v-else
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

/* ==================== PHIẾU GIẢM GIÁ (CÔNG KHAI & CÁ NHÂN) ==================== */
.voucher-group-wrapper {
  margin-bottom: 14px;
}

.voucher-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.voucher-stat-badges {
  display: flex;
  gap: 6px;
  align-items: center;
}

.badge-v-stat {
  font-size: 0.72rem;
  padding: 2px 7px;
  border-radius: 12px;
  font-weight: 600;
  white-space: nowrap;
  user-select: none;
}

.badge-v-stat.public-stat {
  background-color: #e8f4fd;
  color: #1976d2;
  border: 1px solid #bbdefb;
}

.badge-v-stat.personal-stat {
  background-color: #f5f3ff;
  color: #7c3aed;
  border: 1px solid #ddd6fe;
}

.badge-v-stat.personal-stat.has-personal {
  background-color: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(180, 83, 9, 0.15);
}

.selected-voucher-badge-box {
  margin-top: 6px;
}

.v-pill-personal,
.v-pill-public {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 7px;
  font-size: 0.8rem;
  line-height: 1.35;
  transition: all 0.2s ease;
}

.v-pill-personal {
  background-color: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.v-pill-public {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
}

.v-pill-content {
  flex: 1;
}

.v-pill-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  margin-bottom: 2px;
  color: #1e293b;
}

.pill-tag {
  font-size: 0.65rem;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.pill-tag-personal {
  background-color: #ea580c;
  color: #ffffff;
}

.pill-tag-public {
  background-color: #0284c7;
  color: #ffffff;
}

.v-pill-sub {
  font-size: 0.74rem;
  color: #64748b;
}

.text-accent-red {
  color: #c94a29;
  font-weight: 700;
}

.voucher-min-alert {
  color: #c94a29;
  font-size: 0.78rem;
  margin-top: 6px;
  display: block;
  font-weight: 500;
  background: #fff5f5;
  padding: 5px 8px;
  border-radius: 4px;
  border-left: 3px solid #c94a29;
}

.voucher-guest-hint {
  margin-top: 6px;
  font-size: 0.76rem;
  color: #64748b;
  background: #f8fafc;
  padding: 5px 9px;
  border-radius: 5px;
  border: 1px dashed #cbd5e1;
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

/* TOAST POPUP NOTIFICATION */
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
  z-index: 99999;
  border: 1px solid #4a6880;
}

.toast-popup.warning {
  background-color: #d97706;
  border-color: #b45309;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* NÚT ĐÓNG TAB HÓA ĐƠN */
.btn-close-tab {
  background: transparent;
  border: none;
  color: #a0aab0;
  font-size: 0.8rem;
  padding: 0 3px;
  margin-left: 2px;
  cursor: pointer;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.btn-close-tab:hover {
  background: rgba(201, 74, 41, 0.15);
  color: #c94a29;
}

.tab-loading-indicator {
  font-size: 0.82rem;
  color: #8a9292;
  font-style: italic;
  padding: 6px 10px;
}

/* MODAL XÁC NHẬN HỦY */
.confirm-modal-card {
  max-width: 420px;
  padding: 22px;
}

.confirm-modal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.confirm-modal-header h4 {
  margin: 0;
  font-size: 1.05rem;
  color: #1e3a5f;
  font-weight: 700;
}

.confirm-icon {
  font-size: 1.3rem;
}

.confirm-modal-desc {
  font-size: 0.88rem;
  color: #556066;
  line-height: 1.45;
  margin: 0 0 18px;
}

.confirm-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-modal-back {
  background: #f0ebe1;
  color: #5c686d;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-modal-danger {
  background: #c94a29;
  color: #ffffff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-danger:hover {
  background: #b03a1a;
}

/* CUSTOMER DROPDOWN EMPTY */
.customer-dropdown-empty {
  padding: 12px 14px;
  font-size: 0.83rem;
  color: #8a9292;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.btn-quick-create-cust {
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

/* MODAL PRODUCT EMPTY & LOADING */
.modal-loading-box, .empty-product-box {
  padding: 32px 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.btn-create-product-link {
  display: inline-block;
  background-color: #496883;
  color: #ffffff;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-top: 6px;
  transition: background-color 0.2s;
}

.btn-create-product-link:hover {
  background-color: #38536b;
}

.spinner-dot {
  width: 24px;
  height: 24px;
  border: 3px solid #e0e5ea;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1024px) {
  .pos-grid-container {
    grid-template-columns: 1fr;
  }
}
</style>