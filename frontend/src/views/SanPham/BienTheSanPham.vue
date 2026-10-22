<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'
import { isAdmin } from '@/utils/auth.js'
import Select2 from '@/components/Select2.vue'
import Select2Combobox from '@/components/Select2Combobox.vue'

const route = useRoute()
const router = useRouter()

// SVG Placeholder khi chưa có ảnh
const defaultImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1.5'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E"

// Tùy chọn trạng thái cho Select2
const trangThaiOptions = [
  { id: 1, label: 'Đang bán' },
  { id: 0, label: 'Ngừng bán' }
]

// Thông tin sản phẩm hiện tại
const currentProduct = ref(null)
const productsList = ref([])

// Danh sách sản phẩm định dạng cho Select2
const formattedProducts = computed(() => {
  return productsList.value.map(p => ({
    id: p.id,
    label: `${p.tenSanPham} (${p.maSanPham || 'SP'})`,
    tenSanPham: p.tenSanPham,
    maSanPham: p.maSanPham
  }))
})

// Danh sách mã sản phẩm định dạng cho Select2Combobox lọc
const productCodeOptions = computed(() => {
  return productsList.value.map(p => ({
    id: p.id,
    maSanPham: p.maSanPham,
    tenSanPham: p.tenSanPham,
    label: p.maSanPham ? `${p.maSanPham} (${p.tenSanPham})` : p.tenSanPham
  }))
})

// Dữ liệu bộ lọc
const filters = ref({
  idSanPham: '',
  keyword: '',
  idMauSac: '',
  idKichCo: '',
  trangThai: '',
  page: 0,
  size: 10
})

// Dữ liệu thuộc tính
const colors = ref([])
const sizes = ref([])

// Danh sách kích cỡ định dạng cho Select2
const formattedSizes = computed(() => {
  return sizes.value.map(s => ({
    id: s.id,
    label: `Size ${s.tenKichCo}`,
    tenKichCo: s.tenKichCo
  }))
})

// Dữ liệu bảng biến thể
const variants = ref([])
const loading = ref(false)
const selectedVariantIds = ref([])

const pageData = ref({
  number: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
})

// Toast notification theo mẫu ảnh
const toast = ref({
  show: false,
  title: '',
  message: '',
  type: 'success'
})

let toastTimer = null
const showToast = (title, message = '', type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, title, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 4000)
}

// Hộp thoại xác nhận (Confirm modal)
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  type: 'primary',
  confirmText: 'Đồng ý',
  onConfirm: null
})

const openConfirm = ({ title, message, type = 'primary', confirmText = 'Đồng ý', onConfirm }) => {
  confirmDialog.value = {
    show: true,
    title,
    message,
    type,
    confirmText,
    onConfirm
  }
}

const closeConfirm = () => {
  confirmDialog.value.show = false
  confirmDialog.value.onConfirm = null
}

const executeConfirm = async () => {
  const fn = confirmDialog.value.onConfirm
  closeConfirm()
  if (typeof fn === 'function') {
    await fn()
  }
}

// Modal Chỉnh sửa biến thể
const editModal = ref({
  show: false,
  data: {
    id: null,
    maChiTietSanPham: '',
    idKichCo: '',
    idMauSac: '',
    soLuong: 0,
    giaBan: 0,
    trangThai: 1
  },
  errors: {}
})

const openEditModal = (item) => {
  if (!isAdmin()) {
    showToast('Từ chối quyền truy cập', 'Nhân viên chỉ có quyền xem chi tiết, không được phép chỉnh sửa biến thể hoặc giá bán!', 'error')
    return
  }
  editModal.value.data = {
    id: item.id,
    idSanPham: item.idSanPham || currentProduct.value?.id,
    tenSanPham: item.tenSanPham || currentProduct.value?.tenSanPham,
    maSanPham: item.maSanPham || currentProduct.value?.maSanPham,
    maChiTietSanPham: item.maChiTietSanPham,
    idKichCo: item.idKichCo,
    tenKichCo: item.tenKichCo,
    idMauSac: item.idMauSac,
    tenMauSac: item.tenMauSac,
    maHex: item.maHex,
    anhDaiDien: item.anhDaiDien || null,
    soLuong: item.soLuong,
    giaBan: item.giaBan,
    trangThai: item.trangThai
  }
  editModal.value.errors = {}
  editModal.value.show = true
}

const closeEditModal = () => {
  editModal.value.show = false
}

// Xử lý tải / thay đổi / gỡ ảnh cho biến thể trong editModal
const variantImageFileInput = ref(null)

const triggerSelectVariantImage = () => {
  if (variantImageFileInput.value) {
    variantImageFileInput.value.click()
  }
}

const onVariantImageFileSelected = (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    showToast('Ảnh quá lớn', 'Kích thước ảnh không được vượt quá 5MB!', 'error')
    return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    editModal.value.data.anhDaiDien = event.target.result
    showToast('Đã chọn ảnh', 'Ảnh biến thể đã được tải, hãy bấm "Lưu biến thể" để hoàn tất.', 'success')
  }
  reader.readAsDataURL(file)
}

const removeVariantImage = () => {
  editModal.value.data.anhDaiDien = ''
  if (variantImageFileInput.value) {
    variantImageFileInput.value.value = ''
  }
  showToast('Đã gỡ ảnh', 'Ảnh biến thể đã được gỡ. Hãy bấm "Lưu biến thể" để lưu thay đổi.', 'info')
}

// Modal Thêm mới biến thể
const addModal = ref({
  show: false,
  saving: false,
  data: {
    idSanPham: '',
    maChiTietSanPham: '',
    idKichCo: '',
    idMauSac: '',
    soLuong: 1,
    giaBan: 0,
    trangThai: 1
  },
  errors: {}
})

const openAddModal = async () => {
  if (!isAdmin()) {
    showToast('Từ chối quyền truy cập', 'Nhân viên không có quyền thêm biến thể sản phẩm!', 'error')
    return
  }
  if (colors.value.length === 0 || sizes.value.length === 0) {
    await fetchAttributes()
  }
  if (!currentProduct.value && productsList.value.length === 0) {
    try {
      const res = await api.get('/api/san-pham')
      productsList.value = res.data?.content || []
    } catch (_) {}
  }

  addModal.value = {
    show: true,
    saving: false,
    data: {
      idSanPham: currentProduct.value?.id || (productsList.value[0]?.id || ''),
      maChiTietSanPham: '',
      idKichCo: sizes.value[0]?.id || '',
      idMauSac: colors.value[0]?.id || '',
      soLuong: 1,
      giaBan: 0,
      trangThai: 1,
      anhDaiDien: ''
    },
    errors: {}
  }
}

const closeAddModal = () => {
  addModal.value.show = false
  addModal.value.saving = false
}

const addVariantFileInput = ref(null)

const triggerAddVariantImage = () => {
  if (addVariantFileInput.value) {
    addVariantFileInput.value.click()
  }
}

const onAddVariantImageFileSelected = (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    showToast('Ảnh quá lớn', 'Kích thước ảnh không được vượt quá 5MB!', 'warning')
    return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    addModal.value.data.anhDaiDien = event.target.result
    showToast('Đã chọn ảnh', 'Ảnh biến thể đã sẵn sàng.', 'success')
  }
  reader.readAsDataURL(file)
}

const removeAddVariantImage = () => {
  addModal.value.data.anhDaiDien = ''
  if (addVariantFileInput.value) {
    addVariantFileInput.value.value = ''
  }
}

const onAddModalPriceInput = (e) => {
  handlePriceInput(e, (val) => {
    addModal.value.data.giaBan = val === '' ? '' : val
  })
}

// Modal Xem chi tiết biến thể
const detailModal = ref({
  show: false,
  data: {}
})

const openDetailModal = (item) => {
  detailModal.value.data = {
    ...item,
    tenSanPham: item.tenSanPham || currentProduct.value?.tenSanPham,
    maSanPham: item.maSanPham || currentProduct.value?.maSanPham,
    anhDaiDien: item.anhDaiDien || null
  }
  detailModal.value.show = true
}

const closeDetailModal = () => {
  detailModal.value.show = false
}

const switchToEditFromDetail = () => {
  if (!isAdmin()) {
    showToast('Từ chối quyền truy cập', 'Nhân viên không có quyền chỉnh sửa biến thể hoặc giá bán!', 'error')
    return
  }
  const item = { ...detailModal.value.data }
  closeDetailModal()
  openEditModal(item)
}

const blockInvalidIntegerKeys = (e) => {
  if (['-', '+', 'e', 'E', '.', ','].includes(e.key)) {
    e.preventDefault()
  }
}

const handleIntegerPaste = (e) => {
  const pasteData = e.clipboardData?.getData('text') || ''
  const clean = pasteData.replace(/[.,\s]/g, '')
  if (!/^\d+$/.test(clean.trim())) {
    e.preventDefault()
    showToast('Chỉ được dán số nguyên dương!', 'warning')
  }
}

// Format số nguyên có dấu chấm phân cách hàng nghìn (VD: 10000 -> "10.000", 100000 -> "100.000")
const formatNumberWithDots = (val) => {
  if (val === null || val === undefined || val === '') return ''
  const clean = String(val).replace(/\D/g, '')
  if (!clean) return ''
  return clean.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

// Xử lý nhập giá tiền hiển thị liền có dấu chấm mà không bị lỗi bộ gõ tiếng Việt Telex
const handlePriceInput = (e, callback) => {
  const input = e.target
  const val = input.value
  const raw = val.replace(/\D/g, '')

  if (!raw) {
    callback('')
    if (input.value !== '') input.value = ''
    return
  }

  let num = parseInt(raw, 10)
  if (num > 1000000000) num = 1000000000
  callback(num)

  const formatted = formatNumberWithDots(num)

  // Chỉ can thiệp DOM và con trỏ chuột khi chuỗi hiển thị có thay đổi (chèn/bớt dấu chấm)
  if (input.value !== formatted) {
    const cursorFromRight = val.length - (input.selectionEnd || val.length)
    input.value = formatted
    const newPos = Math.max(0, formatted.length - cursorFromRight)
    input.setSelectionRange(newPos, newPos)
  }
}

const onModalPriceInput = (e) => {
  handlePriceInput(e, (val) => {
    editModal.value.data.giaBan = val === '' ? '' : val
  })
}

const handleSaveVariant = async () => {
  editModal.value.errors = {}
  const data = editModal.value.data

  if (!data.idKichCo) {
    editModal.value.errors.idKichCo = 'Vui lòng chọn kích cỡ'
    return
  }
  if (!data.idMauSac) {
    editModal.value.errors.idMauSac = 'Vui lòng chọn màu sắc'
    return
  }

  if (data.soLuong === '' || data.soLuong === null || data.soLuong === undefined) {
    editModal.value.errors.soLuong = 'Vui lòng nhập số lượng'
    return
  }
  const sl = Number(data.soLuong)
  if (isNaN(sl) || !Number.isInteger(sl) || sl < 0 || sl > 999999) {
    editModal.value.errors.soLuong = 'Số lượng phải là số nguyên từ 0 đến 999,999'
    return
  }

  if (data.giaBan === '' || data.giaBan === null || data.giaBan === undefined) {
    editModal.value.errors.giaBan = 'Vui lòng nhập giá bán'
    return
  }
  const gb = Number(data.giaBan)
  if (isNaN(gb) || !Number.isInteger(gb) || gb < 0 || gb > 1000000000) {
    editModal.value.errors.giaBan = 'Giá bán phải là số nguyên từ 0 đến 1,000,000,000 VNĐ'
    return
  }

  if (data.maChiTietSanPham?.trim()) {
    const ma = data.maChiTietSanPham.trim()
    if (ma.length > 50) {
      editModal.value.errors.maChiTietSanPham = 'Mã chi tiết sản phẩm không quá 50 ký tự'
      return
    }
    const codeRegex = /^[A-Za-z0-9_-]+$/
    if (!codeRegex.test(ma)) {
      editModal.value.errors.maChiTietSanPham = 'Mã chỉ gồm chữ cái, số, gạch dưới và gạch ngang'
      return
    }
  }

  try {
    const payload = {
      id: data.id,
      idSanPham: data.idSanPham,
      idKichCo: data.idKichCo,
      idMauSac: data.idMauSac,
      maChiTietSanPham: data.maChiTietSanPham ? data.maChiTietSanPham.trim() : undefined,
      soLuong: Number(data.soLuong),
      giaBan: Number(data.giaBan),
      trangThai: Number(data.trangThai),
      anhDaiDien: data.anhDaiDien !== undefined ? data.anhDaiDien : null,
      nguoiThaoTac: 'Admin'
    }

    await api.put(`/api/chi-tiet-san-pham/${data.id}`, payload)
    closeEditModal()
    showToast('Cập nhật thành công', `Đã cập nhật biến thể ${data.maChiTietSanPham || ''}`)
    fetchVariants()
  } catch (err) {
    console.error('Lỗi cập nhật biến thể:', err)
    const msg = err.response?.data?.message || 'Lỗi khi cập nhật biến thể!'
    showToast('Lỗi cập nhật', msg, 'error')
  }
}

const handleSaveNewVariant = async () => {
  addModal.value.errors = {}
  const data = addModal.value.data

  const targetProductId = data.idSanPham || currentProduct.value?.id
  if (!targetProductId) {
    addModal.value.errors.idSanPham = 'Vui lòng chọn sản phẩm'
    return
  }
  if (!data.idMauSac) {
    addModal.value.errors.idMauSac = 'Vui lòng chọn màu sắc'
    return
  }
  if (!data.idKichCo) {
    addModal.value.errors.idKichCo = 'Vui lòng chọn kích cỡ'
    return
  }
  if (data.soLuong === '' || data.soLuong === null || data.soLuong === undefined) {
    addModal.value.errors.soLuong = 'Vui lòng nhập số lượng'
    return
  }
  const sl = Number(data.soLuong)
  if (isNaN(sl) || !Number.isInteger(sl) || sl < 0 || sl > 999999) {
    addModal.value.errors.soLuong = 'Số lượng phải là số nguyên từ 0 đến 999,999'
    return
  }

  if (data.giaBan === '' || data.giaBan === null || data.giaBan === undefined) {
    addModal.value.errors.giaBan = 'Vui lòng nhập giá bán'
    return
  }
  const gb = Number(data.giaBan)
  if (isNaN(gb) || !Number.isInteger(gb) || gb < 0 || gb > 1000000000) {
    addModal.value.errors.giaBan = 'Giá bán phải là số nguyên từ 0 đến 1,000,000,000 VNĐ'
    return
  }

  if (data.maChiTietSanPham?.trim()) {
    const ma = data.maChiTietSanPham.trim()
    if (ma.length > 50) {
      addModal.value.errors.maChiTietSanPham = 'Mã chi tiết sản phẩm không quá 50 ký tự'
      return
    }
    const codeRegex = /^[A-Za-z0-9_-]+$/
    if (!codeRegex.test(ma)) {
      addModal.value.errors.maChiTietSanPham = 'Mã chỉ gồm chữ cái, số, gạch dưới và gạch ngang'
      return
    }
  }

  addModal.value.saving = true
  try {
    const payload = {
      idSanPham: targetProductId,
      idKichCo: data.idKichCo,
      idMauSac: data.idMauSac,
      maChiTietSanPham: data.maChiTietSanPham?.trim() || undefined,
      soLuong: Number(data.soLuong),
      giaBan: Number(data.giaBan),
      trangThai: Number(data.trangThai ?? 1),
      anhDaiDien: data.anhDaiDien || undefined,
      nguoiThaoTac: 'Admin'
    }

    await api.post('/api/chi-tiet-san-pham', payload)
    closeAddModal()
    showToast('Thêm thành công', 'Đã thêm biến thể mới cho sản phẩm')
    fetchVariants()
  } catch (err) {
    console.error('Lỗi thêm biến thể:', err)
    const msg = err.response?.data?.message || 'Lỗi khi thêm biến thể!'
    showToast('Lỗi thêm biến thể', msg, 'error')
  } finally {
    addModal.value.saving = false
  }
}

// Modal Tải QR
const qrModal = ref({
  show: false,
  items: []
})

const openQrModal = () => {
  let targetItems = []
  if (selectedVariantIds.value.length > 0) {
    targetItems = variants.value.filter(v => selectedVariantIds.value.includes(v.id))
  } else {
    targetItems = variants.value
  }
  qrModal.value.items = targetItems
  qrModal.value.show = true
}

const closeQrModal = () => {
  qrModal.value.show = false
}

// Lấy danh mục thuộc tính màu sắc & kích cỡ
const fetchAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      colors.value = res.data.mauSac || []
      sizes.value = res.data.kichCo || []
    }
  } catch (err) {
    console.error('Lỗi tải thuộc tính:', err)
  }
}

// Lấy danh sách sản phẩm để đưa vào bộ lọc Select2Combobox
const fetchProductsList = async () => {
  try {
    const res = await api.get('/api/san-pham', { params: { size: 1000 } })
    productsList.value = res.data?.content || []
  } catch (err) {
    console.error('Lỗi tải danh sách sản phẩm:', err)
  }
}

// Lấy thông tin sản phẩm chính
const fetchProductInfo = async (id) => {
  try {
    const res = await api.get(`/api/san-pham/${id}`)
    if (res.data) {
      currentProduct.value = res.data
      showToast('Xem CTSP thành công', `Đang xem CTSP của ${res.data.tenSanPham} (${res.data.maSanPham})`)
    }
  } catch (err) {
    console.error('Lỗi tải sản phẩm:', err)
  }
}

// Lắng nghe thay đổi chọn mã sản phẩm trong Select2Combobox
const onProductFilterChange = (selectedId) => {
  if (selectedId) {
    const p = productsList.value.find(item => item.id === selectedId)
    if (p) {
      currentProduct.value = p
    } else {
      fetchProductInfo(selectedId)
    }
  } else {
    currentProduct.value = null
  }
  handleFilterChange()
}

// Lấy danh sách biến thể
const fetchVariants = async () => {
  loading.value = true
  try {
    const params = {
      page: filters.value.page,
      size: filters.value.size
    }
    const productId = filters.value.idSanPham || route.query.sanPhamId || route.params.id
    if (productId) {
      params.idSanPham = productId
    }
    if (filters.value.keyword?.trim()) {
      params.keyword = filters.value.keyword.trim()
    }
    if (filters.value.idMauSac) {
      params.idMauSac = filters.value.idMauSac
    }
    if (filters.value.idKichCo) {
      params.idKichCo = filters.value.idKichCo
    }
    if (filters.value.trangThai !== '') {
      params.trangThai = filters.value.trangThai
    }

    const res = await api.get('/api/chi-tiet-san-pham/filter', { params })
    if (res.data) {
      variants.value = res.data.content || []
      pageData.value = {
        number: res.data.pageNumber || 0,
        size: res.data.pageSize || 10,
        totalElements: res.data.totalElements || 0,
        totalPages: res.data.totalPages || 0
      }
    }
  } catch (err) {
    console.error('Lỗi tải biến thể:', err)
  } finally {
    loading.value = false
  }
}

// Tìm kiếm debounce
let searchTimer = null
const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filters.value.page = 0
    fetchVariants()
  }, 350)
}

const handleFilterChange = () => {
  filters.value.page = 0
  fetchVariants()
}

const resetFilters = () => {
  const defaultPid = route.query.sanPhamId ? Number(route.query.sanPhamId) : ''
  filters.value = {
    idSanPham: defaultPid,
    keyword: '',
    idMauSac: '',
    idKichCo: '',
    trangThai: '',
    page: 0,
    size: 10
  }
  if (!defaultPid) {
    currentProduct.value = null
  } else {
    const found = productsList.value.find(p => p.id === defaultPid)
    if (found) currentProduct.value = found
  }
  fetchVariants()
}

// Xuất file Excel
const exporting = ref(false)
const handleExportExcel = async () => {
  exporting.value = true
  try {
    const params = {}
    const productId = filters.value.idSanPham || route.query.sanPhamId || route.params.id
    if (productId) params.idSanPham = productId
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idMauSac) params.idMauSac = filters.value.idMauSac
    if (filters.value.idKichCo) params.idKichCo = filters.value.idKichCo
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/chi-tiet-san-pham/export-excel', {
      params,
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `bien_the_san_pham_${Date.now()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    showToast('Xuất Excel thành công', 'File Excel danh sách biến thể đã được tải xuống.')
  } catch (err) {
    console.error('Lỗi xuất Excel:', err)
    showToast('Lỗi xuất file', 'Không thể xuất file Excel biến thể', 'error')
  } finally {
    exporting.value = false
  }
}

// 1. Thao tác Kinh doanh (Power icon: bật / tắt)
const toggleVariantStatus = (item) => {
  if (!isAdmin()) {
    showToast('Từ chối quyền truy cập', 'Nhân viên không có quyền thay đổi trạng thái kinh doanh của biến thể!', 'error')
    return
  }
  const newStatus = item.trangThai === 1 ? 0 : 1
  const actionText = newStatus === 1 ? 'mở bán' : 'ngừng bán'
  openConfirm({
    title: 'Đổi trạng thái kinh doanh',
    message: `Bạn có chắc chắn muốn ${actionText} biến thể "${item.maChiTietSanPham}" không?`,
    type: 'info',
    confirmText: 'Xác nhận',
    onConfirm: async () => {
      try {
        await api.patch(`/api/chi-tiet-san-pham/${item.id}/status`, null, {
          params: { trangThai: newStatus }
        })
        item.trangThai = newStatus
        showToast('Cập nhật trạng thái', `Đã chuyển biến thể sang "${newStatus === 1 ? 'Đang bán' : 'Ngừng bán'}"`)
      } catch (err) {
        console.error('Lỗi đổi trạng thái:', err)
        showToast('Lỗi cập nhật', 'Không thể đổi trạng thái biến thể', 'error')
      }
    }
  })
}

// 3. Thao tác Xóa biến thể (Trash icon)
const confirmDeleteVariant = (item) => {
  openConfirm({
    title: 'Xác nhận xóa biến thể',
    message: `Bạn có chắc chắn muốn xóa biến thể "${item.maChiTietSanPham}" không? Hành động này sẽ không thể hoàn tác!`,
    type: 'danger',
    confirmText: 'Xóa ngay',
    onConfirm: async () => {
      try {
        await api.delete(`/api/chi-tiet-san-pham/${item.id}`)
        showToast('Xóa thành công', `Đã xóa biến thể ${item.maChiTietSanPham}`)
        fetchVariants()
      } catch (err) {
        console.error('Lỗi xóa biến thể:', err)
        const msg = err.response?.data?.message || 'Lỗi khi xóa biến thể!'
        showToast('Lỗi xóa', msg, 'error')
      }
    }
  })
}

// Checkbox chọn tất cả
const isAllSelected = computed(() => {
  return variants.value.length > 0 && selectedVariantIds.value.length === variants.value.length
})

const toggleSelectAll = (e) => {
  if (e.target.checked) {
    selectedVariantIds.value = variants.value.map(v => v.id)
  } else {
    selectedVariantIds.value = []
  }
}

// Phân trang
const handlePageSizeChange = () => {
  filters.value.page = 0
  fetchVariants()
}

const changePage = (p) => {
  if (p < 0 || p >= pageData.value.totalPages) return
  filters.value.page = p
  fetchVariants()
}

const visiblePages = computed(() => {
  const current = pageData.value.number
  const total = pageData.value.totalPages
  const pages = []
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, current + 2)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Format tiền tệ & hình ảnh
const formatPrice = (price) => {
  if (price == null) return '0\u00A0đ'
  return Number(price).toLocaleString('vi-VN') + '\u00A0đ'
}

const formatImageUrl = (url) => {
  if (!url) return defaultImage
  let cleanUrl = url
  if (cleanUrl.includes('|')) {
    cleanUrl = cleanUrl.split('|')[1]
  }
  if (!cleanUrl) return defaultImage
  if (cleanUrl.startsWith('data:') || cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) return cleanUrl
  return `http://localhost:8080${cleanUrl.startsWith('/') ? '' : '/'}${cleanUrl}`
}

const onImgError = (e) => {
  e.target.src = defaultImage
}

const goToProductList = () => {
  router.push('/san-pham')
}

// Khởi tạo
onMounted(async () => {
  await Promise.all([fetchAttributes(), fetchProductsList()])
  const productId = route.query.sanPhamId || route.params.id
  if (productId) {
    filters.value.idSanPham = Number(productId) || productId
    await fetchProductInfo(productId)
  }
  await fetchVariants()
})

watch(() => route.query.sanPhamId, async (newId) => {
  if (newId) {
    filters.value.idSanPham = Number(newId) || newId
    await fetchProductInfo(newId)
    filters.value.page = 0
    fetchVariants()
  } else {
    filters.value.idSanPham = ''
    currentProduct.value = null
    filters.value.page = 0
    fetchVariants()
  }
})
</script>

<template>
  <div class="variant-page-container">
    <!-- Toast Notification góc trên bên phải theo mẫu ảnh -->
    <transition name="toast-fade">
      <div v-if="toast.show" :class="['custom-toast-card', toast.type]">
        <div class="toast-icon-wrap">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
        </div>
        <div class="toast-content-wrap">
          <div class="toast-main-title">{{ toast.title }}</div>
          <div class="toast-sub-desc">{{ toast.message }}</div>
        </div>
        <button class="toast-close-btn" @click="toast.show = false" title="Đóng">&times;</button>
      </div>
    </transition>

    <!-- 1. Thanh Breadcrumb đầu trang -->
    <div class="page-breadcrumb">
      <router-link to="/san-pham" class="crumb-parent">Quản lý sản phẩm</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Biến thể sản phẩm</span>
    </div>

    <!-- Khối thẻ chính bao bọc toàn bộ nội dung -->
    <div class="variant-main-card">
      <!-- 2. Header thông tin biến thể của sản phẩm -->
      <div class="product-banner-header">
        <div class="product-names-block">
          <h2 class="variant-title">
            Biến thể của:
            <span class="highlight-product-title">
              {{ currentProduct?.tenSanPham || 'Tất cả sản phẩm' }}
            </span>
          </h2>
          <div class="variant-code-sub">
            Mã sản phẩm: <strong>{{ currentProduct?.maSanPham || '—' }}</strong>
          </div>
        </div>

        <button v-if="isAdmin()" class="btn-add-variant-primary" @click="openAddModal" title="Thêm biến thể sản phẩm">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Thêm biến thể sản phẩm</span>
        </button>
      </div>

      <!-- 3. Khu vực bộ lọc và công cụ (Theo bố cục mẫu) -->
      <div class="variant-filter-box">
        <!-- Hàng 1: Ô tìm kiếm full bên trái + Cụm 3 nút Đặt lại, Tải QR, Xuất Excel -->
        <div class="filter-top-row">
          <div class="search-input-wrapper">
            <label class="search-title-label">Tìm kiếm theo tên hoặc mã phân loại</label>
            <div class="search-field-inner">
              <span class="search-icon-prefix">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </span>
              <input
                  type="text"
                  v-model="filters.keyword"
                  @input="onSearchInput"
                  @keyup.enter="handleFilterChange"
                  placeholder="Nhập mã sản phẩm, phân loại, màu sắc..."
                  class="search-input-control"
              />
            </div>
          </div>

          <div class="filter-buttons-right">
            <button class="btn-outline-gold" @click="resetFilters" title="Đặt lại bộ lọc">
              Đặt lại bộ lọc
            </button>

            <button class="btn-outline-qr" @click="openQrModal" title="Tải / Xem mã QR biến thể">
              Tải QR
            </button>

            <button class="btn-outline-excel" @click="handleExportExcel" :disabled="exporting" title="Xuất danh sách ra file Excel">
              {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
            </button>
          </div>
        </div>

        <!-- Hàng 2: Bốn bộ chọn Dropdown Select2 (Mã sản phẩm, Màu sắc, Kích cỡ, Trạng thái) -->
        <div class="filter-bottom-row">
          <div class="dropdown-filter-item">
            <label class="dropdown-label">Mã sản phẩm</label>
            <Select2Combobox
              v-model="filters.idSanPham"
              :options="productCodeOptions"
              value-key="id"
              label-key="label"
              placeholder="Tất cả mã sản phẩm"
              all-label="Tất cả mã sản phẩm"
              search-placeholder="Tìm kiếm mã sản phẩm..."
              @change="onProductFilterChange"
            />
          </div>

          <div class="dropdown-filter-item">
            <label class="dropdown-label">Màu sắc</label>
            <Select2Combobox
              v-model="filters.idMauSac"
              :options="colors"
              value-key="id"
              label-key="tenMauSac"
              placeholder="Tất cả màu sắc"
              all-label="Tất cả màu sắc"
              search-placeholder="Tìm kiếm màu sắc..."
              @change="handleFilterChange"
            />
          </div>

          <div class="dropdown-filter-item">
            <label class="dropdown-label">Kích cỡ</label>
            <Select2Combobox
              v-model="filters.idKichCo"
              :options="sizes"
              value-key="id"
              label-key="tenKichCo"
              placeholder="Tất cả kích cỡ"
              all-label="Tất cả kích cỡ"
              search-placeholder="Tìm kiếm kích cỡ..."
              @change="handleFilterChange"
            />
          </div>

          <div class="dropdown-filter-item">
            <label class="dropdown-label">Trạng thái</label>
            <Select2Combobox
              v-model="filters.trangThai"
              :options="trangThaiOptions"
              value-key="id"
              label-key="label"
              placeholder="Tất cả trạng thái"
              all-label="Tất cả trạng thái"
              search-placeholder="Tìm trạng thái..."
              @change="handleFilterChange"
            />
          </div>
        </div>
      </div>

      <!-- 4. Bảng danh sách biến thể sản phẩm -->
      <div class="variant-table-container">
        <table class="variant-data-table">
          <thead>
          <tr>
            <th style="width: 40px; text-align: center;">
              <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" class="table-chk" />
            </th>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 85px; text-align: center;">Ảnh</th>
            <th style="width: 90px;">Mã SP</th>
            <th style="width: 140px;">Mã CTSP</th>
            <th style="width: 120px;">Màu sắc</th>
            <th style="width: 80px; text-align: center;">Kích cỡ</th>
            <th style="width: 90px; text-align: center;">Số lượng</th>
            <th style="width: 130px; text-align: right; white-space: nowrap;">Giá bán</th>
            <th style="width: 110px; text-align: center;">Trạng thái</th>
            <th style="width: 130px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td colspan="11" class="empty-cell">
              <div class="loading-state">
                <span class="loading-spinner"></span> Đang tải danh sách biến thể...
              </div>
            </td>
          </tr>
          <tr v-else-if="variants.length === 0">
            <td colspan="11" class="empty-cell">
              Không tìm thấy biến thể nào phù hợp!
            </td>
          </tr>
          <tr v-else v-for="(item, index) in variants" :key="item.id" class="variant-table-row">
            <!-- Checkbox -->
            <td style="text-align: center;">
              <input type="checkbox" :value="item.id" v-model="selectedVariantIds" class="table-chk" />
            </td>

            <!-- STT -->
            <td style="text-align: center;" class="cell-stt">
              {{ pageData.number * pageData.size + index + 1 }}
            </td>

            <!-- Ảnh (nằm sau STT, kèm badge % giảm ở góc phải) -->
            <td style="text-align: center;">
              <div class="variant-img-wrapper">
                <div class="thumb-img-box">
                  <img
                      :src="formatImageUrl(item.anhDaiDien)"
                      @error="onImgError"
                      alt="Variant thumbnail"
                  />
                </div>
                <span
                  v-if="item.coGiamGia && (item.phanTramGiam || (item.giaBan && item.giaSauGiam && item.giaBan > item.giaSauGiam))"
                  class="discount-badge-percent variant-img-discount"
                  :title="item.tenDotGiamGia ? `Đợt giảm: ${item.tenDotGiamGia}` : 'Đang áp dụng giảm giá'"
                >
                  -{{ item.phanTramGiam || Math.round((1 - item.giaSauGiam / item.giaBan) * 100) }}%
                </span>
              </div>
            </td>

            <!-- Mã SP -->
            <td class="cell-bold-code">
              {{ item.maSanPham || currentProduct?.maSanPham || '—' }}
            </td>

            <!-- Mã CTSP -->
            <td class="cell-code">
              {{ item.maChiTietSanPham }}
            </td>

            <!-- Màu sắc (Dấu chấm màu + Tên) -->
            <td>
              <div class="color-badge-wrap">
                <span class="color-circle-dot" :style="{ backgroundColor: item.maHex || '#64748b' }"></span>
                <span class="color-name-text">{{ item.tenMauSac || 'Mặc định' }}</span>
              </div>
            </td>

            <!-- Kích cỡ -->
            <td style="text-align: center;" class="cell-size">
              {{ item.tenKichCo || '—' }}
            </td>

            <!-- Số lượng -->
            <td style="text-align: center;" class="cell-quantity">
              {{ item.soLuong ?? 0 }}
            </td>

            <!-- Giá bán (không ngắt dòng) -->
            <td style="text-align: right;" class="cell-price">
              <div v-if="item.coGiamGia && item.giaSauGiam != null" class="variant-price-stack">
                <span class="original-price-strike">{{ formatPrice(item.giaBan) }}</span>
                <span class="sale-price-val">{{ formatPrice(item.giaSauGiam) }}</span>
              </div>
              <div v-else>
                {{ formatPrice(item.giaBan) }}
              </div>
            </td>

            <!-- Trạng thái -->
            <td style="text-align: center;">
              <span :class="['status-pill', item.trangThai === 1 ? 'status-active' : 'status-inactive']">
                {{ item.trangThai === 1 ? 'Đang bán' : 'Ngừng bán' }}
              </span>
            </td>

            <!-- Cột hành động gồm 3 icon: (kinh doanh, sửa, xóa) -->
            <td style="text-align: center;">
              <div class="variant-action-group">
                <!-- 1. Switch Bật / Tắt kinh doanh (Chỉ Admin) -->
                <label
                  v-if="isAdmin()"
                  class="switch-toggle"
                  :title="item.trangThai === 1 ? 'Đang kinh doanh (nhấn để ngừng)' : 'Đang ngừng kinh doanh (nhấn để bật)'"
                >
                  <input
                    type="checkbox"
                    :checked="item.trangThai === 1"
                    @click.prevent="toggleVariantStatus(item)"
                  />
                  <span class="slider-round"></span>
                </label>

                <!-- 2. Icon Sửa (Pencil icon): Chỉ Admin -->
                <button
                    v-if="isAdmin()"
                    class="btn-icon-round btn-edit-variant"
                    @click="openEditModal(item)"
                    title="Chỉnh sửa chi tiết biến thể"
                >
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>

                <!-- 3. Icon Xem chi tiết (Eye icon) -->
                <button
                    class="btn-icon-round btn-view-variant"
                    @click="openDetailModal(item)"
                    title="Xem chi tiết biến thể"
                >
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 5. Phân trang -->
      <div class="pagination-footer" v-if="pageData.totalPages > 0">
        <div class="pagination-left">
          <span class="pagination-info">
            Hiển thị {{ variants.length ? (pageData.number * pageData.size + 1) : 0 }} - {{ pageData.number * pageData.size + variants.length }} / {{ pageData.totalElements }} biến thể
          </span>
        </div>
        <div class="pagination-center">
          <button
              class="pg-box"
              :disabled="pageData.number === 0"
              @click="changePage(0)"
              title="Trang đầu"
          >«</button>
          <button
              class="pg-box"
              :disabled="pageData.number === 0"
              @click="changePage(pageData.number - 1)"
              title="Trang trước"
          >‹</button>

          <button
              v-for="p in visiblePages"
              :key="p"
              :class="['pg-box', { active: pageData.number === p }]"
              @click="changePage(p)"
          >
            {{ p + 1 }}
          </button>

          <button
              class="pg-box"
              :disabled="pageData.number >= pageData.totalPages - 1"
              @click="changePage(pageData.number + 1)"
              title="Trang sau"
          >›</button>
          <button
              class="pg-box"
              :disabled="pageData.number >= pageData.totalPages - 1"
              @click="changePage(pageData.totalPages - 1)"
              title="Trang cuối"
          >»</button>
        </div>
      </div>
    </div>

    <!-- Modal Cập Nhật Biến Thể theo đúng giao diện mẫu -->
    <div class="modal-backdrop" v-if="editModal.show" @click.self="closeEditModal">
      <div class="modal-dialog-box update-variant-dialog">
        <!-- Header -->
        <div class="update-variant-header">
          <div class="header-left-info">
            <div class="variant-tag-badge">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 20h9"></path>
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
              </svg>
              <span>Cập nhật biến thể</span>
            </div>
            <h3 class="update-product-title">{{ editModal.data.tenSanPham || currentProduct?.tenSanPham || 'Sản phẩm' }}</h3>
            <div class="update-product-code">Mã SP: {{ editModal.data.maSanPham || currentProduct?.maSanPham || '—' }}</div>
          </div>
          <button class="modal-close-icon" @click="closeEditModal" title="Đóng">&times;</button>
        </div>

        <!-- Body: 2 cột theo ảnh mẫu -->
        <div class="update-variant-body">
          <!-- Cột trái: Ảnh to + Thao tác ảnh + 2 thẻ thông tin Màu sắc, Kích cỡ -->
          <div class="left-variant-preview">
            <div class="large-img-card" @click="triggerSelectVariantImage">
              <img
                  v-if="editModal.data.anhDaiDien"
                  :src="formatImageUrl(editModal.data.anhDaiDien)"
                  @error="onImgError"
                  alt="Variant big preview"
                  class="preview-img-tag"
              />
              <div v-else class="variant-img-placeholder">
                <svg viewBox="0 0 24 24" width="38" height="38" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span class="placeholder-main">Chưa có ảnh riêng</span>
                <span class="placeholder-sub">Bấm để tải ảnh lên</span>
              </div>

              <!-- Overlay khi hover ảnh -->
              <div class="img-hover-overlay">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                  <circle cx="12" cy="13" r="4"></circle>
                </svg>
                <span>{{ editModal.data.anhDaiDien ? 'Thay đổi ảnh' : 'Tải ảnh mới' }}</span>
              </div>
            </div>

            <!-- Các nút thao tác ảnh bên dưới -->
            <div class="variant-img-action-bar">
              <input
                type="file"
                ref="variantImageFileInput"
                accept="image/png, image/jpeg, image/webp"
                style="display: none;"
                @change="onVariantImageFileSelected"
              />
              <button
                type="button"
                class="btn-variant-upload-img"
                @click="triggerSelectVariantImage"
              >
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
                <span>{{ editModal.data.anhDaiDien ? 'Đổi ảnh' : 'Thêm ảnh' }}</span>
              </button>
              <button
                v-if="editModal.data.anhDaiDien"
                type="button"
                class="btn-variant-remove-img"
                @click="removeVariantImage"
                title="Gỡ ảnh biến thể"
              >
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
                <span>Gỡ ảnh</span>
              </button>
            </div>

            <div class="variant-attr-summary-row">
              <div class="attr-summary-box">
                <span class="attr-box-label">MÀU SẮC</span>
                <div class="attr-box-val">
                  <span class="attr-color-dot" :style="{ backgroundColor: editModal.data.maHex || 'var(--blue, #496883)' }"></span>
                  <span class="attr-text-val">{{ editModal.data.tenMauSac || 'Mặc định' }}</span>
                </div>
              </div>

              <div class="attr-summary-box">
                <span class="attr-box-label">KÍCH CỠ</span>
                <div class="attr-box-val">
                  <span class="attr-text-val">Size {{ editModal.data.tenKichCo || '—' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Cột phải: Cập nhật thông tin -->
          <div class="right-variant-form-card">
            <div class="form-card-heading">CẬP NHẬT THÔNG TIN</div>

            <div class="form-fields-stack">
              <div class="variant-field-group">
                <label class="field-label-text">SỐ LƯỢNG</label>
                <input
                    type="number"
                    min="0"
                    v-model.number="editModal.data.soLuong"
                    class="field-input-control"
                    placeholder="0"
                />
                <span class="err-text" v-if="editModal.errors.soLuong">{{ editModal.errors.soLuong }}</span>
              </div>

              <div class="variant-field-group">
                <label class="field-label-text">GIÁ BÁN <span class="req-star">*</span></label>
                <input
                    type="text"
                    inputmode="numeric"
                    :value="formatNumberWithDots(editModal.data.giaBan)"
                    class="field-input-control field-price-focus"
                    placeholder="0"
                    @focus="$event.target.select()"
                    @input="onModalPriceInput"
                    @keydown="blockInvalidIntegerKeys"
                    @paste="handleIntegerPaste"
                />
                <span class="err-text" v-if="editModal.errors.giaBan">{{ editModal.errors.giaBan }}</span>
              </div>

              <div class="variant-field-group">
                <label class="field-label-text">TRẠNG THÁI</label>
                <Select2
                  v-model="editModal.data.trangThai"
                  :options="trangThaiOptions"
                  label-key="label"
                  placeholder="-- Chọn trạng thái --"
                  search-placeholder="Tìm trạng thái..."
                  :allow-clear="false"
                  :searchable="false"
                />
              </div>

              <button class="btn-save-variant-action" @click="handleSaveVariant">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                  <polyline points="17 21 17 13 7 13 7 21"></polyline>
                  <polyline points="7 3 7 8 15 8"></polyline>
                </svg>
                Lưu biến thể
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Xem Chi Tiết Biến Thể -->
    <div class="modal-backdrop" v-if="detailModal.show" @click.self="closeDetailModal">
      <div class="modal-dialog-box update-variant-dialog">
        <!-- Header -->
        <div class="update-variant-header">
          <div class="header-left-info">
            <div class="variant-tag-badge view-badge">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <span>Chi tiết biến thể</span>
            </div>
            <h3 class="update-product-title">{{ detailModal.data.tenSanPham || currentProduct?.tenSanPham || 'Sản phẩm' }}</h3>
            <div class="update-product-code">Mã SP: {{ detailModal.data.maSanPham || currentProduct?.maSanPham || '—' }} | Mã CTSP: {{ detailModal.data.maChiTietSanPham || '—' }}</div>
          </div>
          <button class="modal-close-icon" @click="closeDetailModal" title="Đóng">&times;</button>
        </div>

        <!-- Body: 2 cột đồng bộ với giao diện hệ thống -->
        <div class="update-variant-body">
          <!-- Cột trái: Ảnh to + 2 thẻ Màu sắc, Kích cỡ -->
          <div class="left-variant-preview">
            <div class="large-img-card">
              <img
                  :src="formatImageUrl(detailModal.data.anhDaiDien)"
                  @error="onImgError"
                  alt="Variant preview"
                  class="preview-img-tag"
              />
            </div>

            <div class="variant-attr-summary-row">
              <div class="attr-summary-box">
                <span class="attr-box-label">MÀU SẮC</span>
                <div class="attr-box-val">
                  <span class="attr-color-dot" :style="{ backgroundColor: detailModal.data.maHex || 'var(--blue, #496883)' }"></span>
                  <span class="attr-text-val">{{ detailModal.data.tenMauSac || 'Mặc định' }}</span>
                </div>
              </div>

              <div class="attr-summary-box">
                <span class="attr-box-label">KÍCH CỠ</span>
                <div class="attr-box-val">
                  <span class="attr-text-val">Size {{ detailModal.data.tenKichCo || '—' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Cột phải: Thông tin chi tiết biến thể -->
          <div class="right-variant-form-card">
            <div class="form-card-heading">THÔNG TIN BIẾN THỂ</div>

            <div class="detail-info-grid">
              <div class="detail-item-box">
                <span class="detail-item-label">Mã biến thể</span>
                <span class="detail-item-value highlight-code">{{ detailModal.data.maChiTietSanPham || '—' }}</span>
              </div>

              <div class="detail-item-box">
                <span class="detail-item-label">Số lượng tồn kho</span>
                <span class="detail-item-value bold-number">{{ detailModal.data.soLuong ?? 0 }}</span>
              </div>

              <div class="detail-item-box">
                <span class="detail-item-label">Giá bán</span>
                <span class="detail-item-value price-number">{{ formatPrice(detailModal.data.giaBan) }}</span>
              </div>

              <div class="detail-item-box">
                <span class="detail-item-label">Trạng thái</span>
                <span :class="['detail-status-pill', detailModal.data.trangThai === 1 ? 'status-pill-active' : 'status-pill-inactive']">
                  {{ detailModal.data.trangThai === 1 ? 'Đang bán' : 'Ngừng bán' }}
                </span>
              </div>
            </div>

            <div class="detail-actions-footer">
              <button v-if="isAdmin()" class="btn-detail-edit" @click="switchToEditFromDetail">
                Chỉnh sửa biến thể
              </button>
              <button class="btn-detail-close" @click="closeDetailModal">Đóng</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Xem / Tải QR -->
    <div class="modal-backdrop" v-if="qrModal.show" @click.self="closeQrModal">
      <div class="modal-dialog-box qr-dialog-box">
        <div class="modal-header-row">
          <h3 class="modal-dialog-title">Mã QR các biến thể</h3>
          <button class="modal-close-icon" @click="closeQrModal">&times;</button>
        </div>
        <div class="modal-body-content qr-list-grid">
          <div v-for="item in qrModal.items" :key="item.id" class="qr-card-item">
            <div class="qr-mockup-box">
              <img
                  :src="`https://api.qrserver.com/v1/create-qr-code/?size=130x130&data=${encodeURIComponent(item.maChiTietSanPham)}`"
                  alt="QR code"
                  class="qr-code-img"
              />
            </div>
            <div class="qr-variant-code">{{ item.maChiTietSanPham }}</div>
            <div class="qr-variant-detail">{{ item.tenMauSac }} - {{ item.tenKichCo }}</div>
            <div class="qr-variant-price">{{ formatPrice(item.giaBan) }}</div>
          </div>
        </div>
        <div class="modal-footer-row">
          <button class="btn btn-cancel-modal" @click="closeQrModal">Đóng</button>
        </div>
      </div>
    </div>

    <!-- Hộp thoại xác nhận tương tác -->
    <div class="modal-backdrop confirm-modal-overlay" v-if="confirmDialog.show" @click.self="closeConfirm">
      <div class="confirm-modal-box">
        <div class="confirm-modal-header" :class="{ 'header-danger': confirmDialog.type === 'danger' }">
          <div class="confirm-warning-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
          </div>
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">{{ confirmDialog.title || 'Xác nhận thao tác' }}</h3>
            <p class="confirm-subtitle">Vui lòng xác nhận để tiếp tục thực hiện</p>
          </div>
          <button type="button" class="modal-close-btn" @click="closeConfirm">✕</button>
        </div>
        <div class="confirm-modal-body">
          <p class="confirm-message-text">{{ confirmDialog.message }}</p>
        </div>
        <div class="confirm-modal-footer">
          <button type="button" class="btn btn-secondary" @click="closeConfirm">Hủy bỏ</button>
          <button
              type="button"
              :class="['btn', confirmDialog.type === 'danger' ? 'btn-confirm-danger' : 'btn-save-confirm']"
              @click="executeConfirm"
          >
            {{ confirmDialog.confirmText || 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Thêm Mới Biến Thể -->
    <div class="modal-backdrop" v-if="addModal.show" @click.self="closeAddModal">
      <div class="modal-dialog-box add-variant-dialog">
        <!-- Header -->
        <div class="modal-header-row">
          <div>
            <div class="variant-tag-badge add-badge">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              <span>Thêm biến thể sản phẩm</span>
            </div>
            <h3 class="modal-dialog-title" style="margin-top: 6px;">{{ currentProduct?.tenSanPham || 'Thêm mới biến thể' }}</h3>
            <div class="update-product-code" v-if="currentProduct?.maSanPham">Mã SP: {{ currentProduct.maSanPham }}</div>
          </div>
          <button class="modal-close-icon" @click="closeAddModal" title="Đóng">&times;</button>
        </div>

        <!-- Body -->
        <div class="modal-body-content">
          <div class="modal-form-grid">
            <!-- Sản phẩm nếu chưa chọn -->
            <div class="modal-field-item full-width" v-if="!currentProduct && productsList.length > 0">
              <label class="form-item-label">Sản phẩm <span class="req-star">*</span></label>
              <Select2
                v-model="addModal.data.idSanPham"
                :options="formattedProducts"
                label-key="label"
                placeholder="-- Chọn sản phẩm --"
                search-placeholder="Tìm kiếm sản phẩm..."
                :allow-clear="false"
              />
              <span class="err-text" v-if="addModal.errors.idSanPham">{{ addModal.errors.idSanPham }}</span>
            </div>

            <!-- Mã CTSP -->
            <div class="modal-field-item full-width">
              <label class="form-item-label">Mã chi tiết sản phẩm <span class="sub-hint" style="color: #64748b; font-weight: normal; font-size: 0.8rem;">(Để trống hệ thống tự sinh)</span></label>
              <input
                  type="text"
                  v-model="addModal.data.maChiTietSanPham"
                  class="form-control-input"
                  placeholder="Nhập mã hoặc để trống tự sinh..."
                  maxlength="50"
              />
              <span class="err-text" v-if="addModal.errors.maChiTietSanPham">{{ addModal.errors.maChiTietSanPham }}</span>
            </div>

            <!-- Màu sắc -->
            <div class="modal-field-item">
              <label class="form-item-label">Màu sắc <span class="req-star">*</span></label>
              <Select2
                v-model="addModal.data.idMauSac"
                :options="colors"
                label-key="tenMauSac"
                placeholder="-- Chọn màu sắc --"
                search-placeholder="Tìm kiếm màu sắc..."
                :allow-clear="false"
              />
              <span class="err-text" v-if="addModal.errors.idMauSac">{{ addModal.errors.idMauSac }}</span>
            </div>

            <!-- Kích cỡ -->
            <div class="modal-field-item">
              <label class="form-item-label">Kích cỡ <span class="req-star">*</span></label>
              <Select2
                v-model="addModal.data.idKichCo"
                :options="formattedSizes"
                label-key="label"
                placeholder="-- Chọn kích cỡ --"
                search-placeholder="Tìm kiếm kích cỡ..."
                :allow-clear="false"
              />
              <span class="err-text" v-if="addModal.errors.idKichCo">{{ addModal.errors.idKichCo }}</span>
            </div>

            <!-- Số lượng -->
            <div class="modal-field-item">
              <label class="form-item-label">Số lượng <span class="req-star">*</span></label>
              <input
                  type="number"
                  min="0"
                  v-model.number="addModal.data.soLuong"
                  class="form-control-input"
                  placeholder="0"
                  @keydown="blockInvalidIntegerKeys"
              />
              <span class="err-text" v-if="addModal.errors.soLuong">{{ addModal.errors.soLuong }}</span>
            </div>

            <!-- Giá bán -->
            <div class="modal-field-item">
              <label class="form-item-label">Giá bán (VNĐ) <span class="req-star">*</span></label>
              <input
                  type="text"
                  inputmode="numeric"
                  :value="formatNumberWithDots(addModal.data.giaBan)"
                  class="form-control-input"
                  placeholder="0"
                  @focus="$event.target.select()"
                  @input="onAddModalPriceInput"
                  @keydown="blockInvalidIntegerKeys"
                  @paste="handleIntegerPaste"
              />
              <span class="err-text" v-if="addModal.errors.giaBan">{{ addModal.errors.giaBan }}</span>
            </div>

            <!-- Trạng thái -->
            <div class="modal-field-item full-width">
              <label class="form-item-label">Trạng thái kinh doanh</label>
              <Select2
                v-model="addModal.data.trangThai"
                :options="trangThaiOptions"
                label-key="label"
                placeholder="-- Chọn trạng thái --"
                search-placeholder="Tìm trạng thái..."
                :allow-clear="false"
                :searchable="false"
              />
            </div>

            <!-- Hình ảnh biến thể -->
            <div class="modal-field-item full-width">
              <label class="form-item-label">Hình ảnh biến thể <span class="sub-hint" style="color: #64748b; font-weight: normal; font-size: 0.8rem;">(Tùy chọn - Tối đa 5MB)</span></label>
              <div class="add-variant-img-row">
                <div class="add-variant-img-preview" @click="triggerAddVariantImage">
                  <img v-if="addModal.data.anhDaiDien" :src="formatImageUrl(addModal.data.anhDaiDien)" alt="Preview" class="add-variant-img" />
                  <div v-else class="add-variant-img-placeholder">
                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                      <circle cx="8.5" cy="8.5" r="1.5"/>
                      <polyline points="21 15 16 10 5 21"/>
                    </svg>
                    <span>Chọn ảnh</span>
                  </div>
                </div>
                <div class="add-variant-img-actions">
                  <input
                    type="file"
                    ref="addVariantFileInput"
                    accept="image/png, image/jpeg, image/webp"
                    style="display: none;"
                    @change="onAddVariantImageFileSelected"
                  />
                  <button type="button" class="btn-img-action-sm btn-pick" @click="triggerAddVariantImage">
                    {{ addModal.data.anhDaiDien ? 'Đổi ảnh' : 'Tải ảnh lên' }}
                  </button>
                  <button v-if="addModal.data.anhDaiDien" type="button" class="btn-img-action-sm btn-remove" @click="removeAddVariantImage">
                    Gỡ ảnh
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="modal-footer-row">
          <button type="button" class="btn btn-cancel-modal" @click="closeAddModal">Hủy bỏ</button>
          <button
              type="button"
              class="btn btn-save-modal"
              :disabled="addModal.saving"
              @click="handleSaveNewVariant"
          >
            {{ addModal.saving ? 'Đang lưu...' : 'Thêm biến thể' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Fredoka:wght@600;700&display=swap');

:root {
  --blue: #496883;
  --gold: #d2a764;
  --line: #e9e5db;
  --bg: #f7f5ef;
}

.variant-page-container {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1e293b;
  box-sizing: border-box;
}

/* Toast Floating Card theo chuẩn ảnh mẫu */
.custom-toast-card {
  position: fixed;
  top: 1.25rem;
  right: 1.5rem;
  z-index: 9999;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  border: 1px solid #f1f5f9;
  padding: 12px 18px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 320px;
  max-width: 440px;
}

.toast-icon-wrap {
  color: #10b981;
  flex-shrink: 0;
  margin-top: 1px;
}

.custom-toast-card.error .toast-icon-wrap {
  color: #ef4444;
}

.toast-content-wrap {
  flex: 1;
}

.toast-main-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 2px;
}

.toast-sub-desc {
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.35;
}

.toast-close-btn {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}

.toast-close-btn:hover {
  color: #334155;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

/* 1. Breadcrumb navigation */
.breadcrumb-nav-bar {
  margin-bottom: 1rem;
}

.nav-breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #64748b;
}

.crumb-link {
  cursor: pointer;
  color: #64748b;
  transition: color 0.2s;
}

.crumb-link:hover {
  color: var(--blue, #496883);
}

.crumb-slash {
  color: #cbd5e1;
}

.crumb-current {
  color: #0f172a;
  font-weight: 700;
}

/* Thẻ bao bọc chính */
.variant-main-card {
  background-color: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  padding: 1.5rem 1.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 2. Banner header hiển thị tên & mã sản phẩm */
.product-banner-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

.variant-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.highlight-product-title {
  color: var(--blue, #496883);
  font-weight: 800;
}

.variant-code-sub {
  font-size: 0.92rem;
  color: #64748b;
}

.variant-code-sub strong {
  color: #0f172a;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back-link:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.btn-add-variant-primary {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background-color: var(--blue, #496883);
  color: #ffffff;
  border: 1px solid var(--blue, #496883);
  border-radius: 8px;
  height: 38px;
  padding: 0 16px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(73, 104, 131, 0.2);
}

.btn-add-variant-primary:hover {
  background-color: #3d5870;
  border-color: #3d5870;
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(73, 104, 131, 0.25);
}

.variant-tag-badge.add-badge {
  color: #0d9488;
  background-color: #ccfbf1;
}

.add-variant-dialog {
  max-width: 540px;
}

/* 3. Khu vực bộ lọc theo ảnh mẫu */
.variant-filter-box {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.filter-top-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.25rem;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.search-title-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
}

.search-field-inner {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.search-icon-prefix {
  position: absolute;
  left: 0.95rem;
  color: #94a3b8;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.search-input-control {
  width: 100%;
  height: 2.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 1rem 0 2.5rem;
  font-size: 0.92rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}

.search-input-control:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.filter-buttons-right {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-shrink: 0;
}

/* Các nút bấm viền có icon theo mẫu */
.btn-outline-gold,
.btn-outline-qr,
.btn-outline-excel {
  height: 2.5rem;
  padding: 0 1rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-outline-gold {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}

.btn-outline-gold:hover {
  background-color: #faeed7;
  transform: translateY(-1px);
}

.btn-outline-qr {
  background-color: #f7f5ee;
  border: 1px solid #d8e2e8;
  color: #496883;
}

.btn-outline-qr:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: translateY(-1px);
}

.btn-outline-excel {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #d2e5d6;
}

.btn-outline-excel:hover {
  background-color: #deede1;
  transform: translateY(-1px);
}

/* Hàng bộ chọn dropdown 2 */
.filter-bottom-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.dropdown-filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.dropdown-label {
  font-size: 0.83rem;
  font-weight: 600;
  color: #64748b;
}

.dropdown-select {
  height: 2.45rem;
  background-color: #fcfbf8;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.88rem;
  color: #334155;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.dropdown-select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

/* 4. Bảng danh sách dữ liệu */
.variant-table-container {
  overflow-x: auto;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  margin-bottom: 1.25rem;
}

.variant-data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.variant-data-table th {
  background-color: #faf8f5;
  color: #4a5568;
  font-weight: 700;
  text-transform: uppercase;
  padding: 12px 14px;
  text-align: left;
  border-bottom: 2px solid #e9e5db;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.variant-data-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.variant-table-row:hover {
  background-color: #fcfbf8;
}

.table-chk {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--blue, #496883);
}

.cell-stt {
  font-weight: 500;
  color: #9aa0a0;
  font-size: 0.95rem;
}

.cell-bold-code {
  font-weight: 700;
  color: var(--blue, #496883);
  font-family: monospace, sans-serif;
  letter-spacing: 0.4px;
}

.cell-code {
  font-family: monospace, sans-serif;
  font-size: 0.85rem;
  color: var(--blue, #496883);
  font-weight: 700;
  letter-spacing: 0.4px;
}

.variant-img-wrapper {
  position: relative;
  display: inline-flex;
  vertical-align: middle;
}

.variant-img-discount {
  position: absolute;
  top: -6px;
  right: -16px;
  font-size: 0.65rem;
  padding: 1px 4px;
  font-weight: 700;
  border-radius: 4px;
  line-height: 1.1;
  box-shadow: 0 1px 3px rgba(220, 38, 38, 0.25);
  z-index: 2;
  white-space: nowrap;
}

.thumb-img-box {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  background-color: #f8fafc;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.thumb-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.color-badge-wrap {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background-color: #f1f5f9;
  padding: 4px 10px;
  border-radius: 14px;
}

.color-circle-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
}

.color-name-text {
  font-size: 0.84rem;
  font-weight: 600;
  color: #334155;
}

.cell-size {
  font-weight: 700;
  color: #1e293b;
}

.cell-quantity {
  font-weight: 600;
  color: #334155;
}

.cell-price {
  font-weight: 700;
  color: #2b353b;
  white-space: nowrap;
}

.variant-price-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  white-space: nowrap;
}

.original-price-strike {
  font-size: 0.8rem;
  color: #94a3b8;
  text-decoration: line-through;
  font-weight: 500;
  white-space: nowrap;
}

.sale-price-val {
  color: #c94a29;
  font-weight: 700;
  white-space: nowrap;
}

.cell-discount {
  color: #94a3b8;
}

.discount-badge-percent {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  font-size: 0.75rem;
  font-weight: 700;
  color: #dc2626;
  background-color: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 4px;
}

.status-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}

.status-pill.status-active {
  background-color: #eaf5ed;
  color: #247541;
  border: 1px solid #bce1c7;
}

.status-pill.status-inactive {
  background-color: #f7edeb;
  color: #b34a3b;
  border: 1px solid #ecc9c3;
}

/* Cột hành động: 3 icon tròn (kinh doanh, sửa, xóa) */
.variant-action-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-icon-round {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 0;
}

.btn-icon-round svg {
  display: block;
}

/* 1. Icon Kinh doanh (Power) */
.btn-status-toggle {
  color: #b34a3b;
  background-color: #f7edeb;
  border-color: #ecc9c3;
}

.btn-status-toggle.is-active {
  color: #247541;
  background-color: #eaf5ed;
  border-color: #bce1c7;
}

.btn-status-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.08);
}

/* 2. Icon Sửa (Pencil) */
.btn-edit-variant {
  color: #496883;
  background-color: #f7f5ee;
  border-color: #d8e2e8;
}

.btn-edit-variant:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(73, 104, 131, 0.2);
}

/* 3. Icon Xem chi tiết (Eye) */
.btn-view-variant {
  color: #496883;
  background-color: #f7f5ee;
  border-color: #d8e2e8;
}

.btn-view-variant:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(73, 104, 131, 0.2);
}

.variant-tag-badge.view-badge {
  color: #0284c7;
  background-color: #e0f2fe;
}

.detail-info-grid {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  flex: 1;
}

.detail-item-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 0.85rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.detail-item-label {
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 600;
}

.detail-item-value {
  font-size: 0.92rem;
  color: #1e293b;
  font-weight: 700;
}

.detail-item-value.highlight-code {
  font-family: monospace;
  color: var(--blue, #496883);
}

.detail-item-value.bold-number {
  color: #0f172a;
}

.detail-item-value.price-number {
  color: #e65228;
  font-size: 1.05rem;
}

.detail-status-pill {
  padding: 0.2rem 0.65rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-pill-active {
  background-color: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.status-pill-inactive {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.detail-actions-footer {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}

.btn-detail-edit {
  flex: 1;
  height: 2.5rem;
  background-color: #3b596a;
  border: 1px solid #324b5a;
  border-radius: 8px;
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-detail-edit:hover {
  background-color: #2c4350;
  border-color: #243742;
  transform: translateY(-1px);
}

.btn-detail-close {
  height: 2.5rem;
  padding: 0 1.25rem;
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  border-radius: 8px;
  color: #957b48;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-detail-close:hover {
  background-color: #faeed7;
}

.empty-cell {
  text-align: center;
  padding: 2.5rem 1rem !important;
  color: #94a3b8;
  font-size: 0.95rem;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.loading-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid #cbd5e1;
  border-top-color: var(--blue, #496883);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 5. Phân trang */
.pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding-top: 0.5rem;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-size-selector {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
  color: #64748b;
}

.page-size-label {
  font-weight: 500;
  color: #64748b;
}

.page-size-select {
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background-color: #ffffff;
  color: #1e293b;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.page-size-select:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.page-size-unit {
  font-size: 0.85rem;
  color: #64748b;
}

.pagination-info {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  color: #555e65;
  line-height: 1.5;
}

.pagination-center {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pg-box {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  background-color: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  color: #496883;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.pg-box:hover:not(:disabled) {
  border-color: #496883;
  color: #496883;
  background-color: #f8fafc;
}

.pg-box.active {
  background-color: #496883;
  border-color: #496883;
  color: #ffffff;
}

.pg-box:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* Modal Backdrop & Dialog */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-dialog-box {
  background-color: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: modalIn 0.25s ease;
}

.modal-dialog-box.add-variant-dialog,
.modal-dialog-box.update-variant-dialog {
  overflow: visible;
}

@keyframes modalIn {
  from { opacity: 0; transform: scale(0.96) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.qr-dialog-box {
  max-width: 680px;
}

/* Modal xác nhận chuẩn thiết kế */
.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: modalIn 0.2s ease-out;
}

.confirm-modal-header {
  padding: 1.25rem 1.4rem;
  background-color: #fff9f0;
  border-bottom: 1px solid #faedd9;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  position: relative;
}

.confirm-modal-header.header-danger {
  background-color: #fff5f5;
  border-bottom: 1px solid #fed7d7;
}

.confirm-warning-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #eaf1f5;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-danger .confirm-warning-icon {
  background-color: #fdeded;
  color: #c0392b;
}

.confirm-title-wrap {
  flex: 1;
}

.confirm-title {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 700;
  color: #8c6328;
}

.header-danger .confirm-title {
  color: #c0392b;
}

.confirm-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.83rem;
  color: #aa8651;
}

.modal-close-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #9aa0a0;
  cursor: pointer;
  transition: color 0.15s;
}

.modal-close-btn:hover {
  color: #8c6328;
}

.confirm-modal-body {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.confirm-modal-footer {
  padding: 1rem 1.4rem;
  border-top: 1px solid #eeebe3;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  background-color: #faf9f6;
}

.btn-secondary {
  background-color: #ffffff;
  border: 1px solid #d8d2c6;
  color: #616e75;
  font-weight: 600;
  padding: 0.55rem 1.15rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background-color: #f0ece1;
}

.btn-save-confirm {
  background-color: #b38e6e;
  color: #ffffff;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(179, 142, 110, 0.25);
  transition: all 0.2s;
}

.btn-save-confirm:hover:not(:disabled) {
  background-color: #9c7857;
}

.btn-confirm-danger {
  background-color: #c0392b;
  color: #ffffff;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(192, 57, 43, 0.25);
  transition: all 0.2s;
}

.btn-confirm-danger:hover:not(:disabled) {
  background-color: #a93226;
}

.modal-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #efeae0;
}

.modal-dialog-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #3c4d55;
}

.modal-close-icon {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}

.modal-close-icon:hover {
  color: #334155;
}

.modal-body-content {
  padding: 1.25rem 1.5rem;
}

.modal-form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.modal-field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.modal-field-item.full-width {
  grid-column: 1 / -1;
}

.form-item-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
}

.req-star {
  color: #ef4444;
}

.form-control-input,
.form-control-select {
  height: 2.4rem;
  background-color: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  padding: 0 0.85rem;
  font-size: 0.9rem;
  color: #1e293b;
  outline: none;
  transition: all 0.15s;
}

.form-control-input:focus,
.form-control-select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.err-text {
  font-size: 0.78rem;
  color: #ef4444;
}

.confirm-message-text {
  font-size: 0.95rem;
  color: #475569;
  line-height: 1.5;
  margin: 0;
}

.modal-footer-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #efeae0;
  background-color: #faf8f5;
}

.btn {
  height: 2.35rem;
  padding: 0 1.2rem;
  border-radius: 7px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel-modal {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}

.btn-cancel-modal:hover {
  background-color: #faeed7;
}

.btn-save-modal {
  background-color: #3b596a;
  border: 1px solid #324b5a;
  color: #ffffff;
}

.btn-save-modal:hover {
  background-color: #2c4350;
  border-color: #243742;
}

.btn-save-modal:hover {
  background-color: #3d576e;
}

.btn-danger-confirm {
  background-color: #dc2626;
  border: 1px solid #dc2626;
  color: #ffffff;
}

.btn-danger-confirm:hover {
  background-color: #b91c1c;
}

/* QR Code Grid in modal */
.qr-list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 1rem;
  max-height: 60vh;
  overflow-y: auto;
}

.qr-card-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  background-color: #ffffff;
}

.qr-mockup-box {
  width: 130px;
  height: 130px;
  margin: 0 auto 8px;
}

.qr-code-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qr-variant-code {
  font-weight: 700;
  font-size: 0.88rem;
  color: #0f172a;
}

.qr-variant-detail {
  font-size: 0.8rem;
  color: #64748b;
  margin: 2px 0 4px;
}

.qr-variant-price {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--blue, #496883);
}

/* Update Variant Dialog per Screenshot */
.update-variant-dialog {
  max-width: 760px;
  border-radius: 16px;
  background-color: #ffffff;
  padding: 1.5rem 1.75rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
}

.update-variant-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.header-left-info {
  display: flex;
  flex-direction: column;
}

.variant-tag-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--blue, #496883);
  font-size: 0.8rem;
  font-weight: 700;
  background-color: #eaf1f4;
  padding: 3px 8px;
  border-radius: 6px;
  width: fit-content;
  margin-bottom: 6px;
}

.update-product-title {
  margin: 0 0 4px 0;
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
}

.update-product-code {
  font-size: 0.88rem;
  color: #64748b;
  font-weight: 600;
}

.update-variant-body {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 1.5rem;
}

.left-variant-preview {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.large-img-card {
  height: 235px;
  background-color: #f8fafc;
  border: 1.5px dashed #cbd5e1;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.2s ease;
}

.large-img-card:hover {
  border-color: var(--blue, #496883);
  background-color: #f1f5f9;
}

.preview-img-tag {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.variant-img-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  text-align: center;
}

.variant-img-placeholder .placeholder-main {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
}

.variant-img-placeholder .placeholder-sub {
  font-size: 0.75rem;
  color: #94a3b8;
}

.img-hover-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.6);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s ease;
  backdrop-filter: blur(2px);
}

.large-img-card:hover .img-hover-overlay {
  opacity: 1;
}

.variant-img-action-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-variant-upload-img {
  flex: 1;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background-color: #ffffff;
  color: var(--blue, #496883);
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-variant-upload-img:hover {
  background-color: #f1f5f9;
  border-color: var(--blue, #496883);
}

.btn-variant-remove-img {
  height: 32px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background-color: #fff1f2;
  color: #e11d48;
  border: 1px solid #fecdd3;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-variant-remove-img:hover {
  background-color: #ffe4e6;
  border-color: #fb7185;
}

.variant-attr-summary-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.attr-summary-box {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 12px;
  background-color: #ffffff;
}

.attr-box-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
  display: block;
  margin-bottom: 4px;
}

.attr-box-val {
  display: flex;
  align-items: center;
  gap: 6px;
}

.attr-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.attr-text-val {
  font-size: 0.9rem;
  font-weight: 700;
  color: #1e293b;
}

.right-variant-form-card {
  background-color: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
}

.form-card-heading {
  font-size: 0.82rem;
  font-weight: 800;
  color: #334155;
  letter-spacing: 0.6px;
  margin-bottom: 1.1rem;
}

.form-fields-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  flex: 1;
}

.variant-field-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-label-text {
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  letter-spacing: 0.3px;
}

.field-input-control {
  height: 2.45rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
  color: #0f172a;
  outline: none;
  background-color: #ffffff;
  transition: all 0.2s;
}

.field-input-control:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.field-select-control {
  cursor: pointer;
}

.btn-save-variant-action {
  height: 2.75rem;
  background-color: var(--blue, #496883);
  border: 1px solid var(--blue, #496883);
  border-radius: 8px;
  color: #ffffff;
  font-size: 0.92rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: auto;
}

.btn-save-variant-action:hover {
  background-color: #3d576e;
  border-color: #3d576e;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.add-variant-img-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background-color: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 10px 14px;
}

.add-variant-img-preview {
  width: 68px;
  height: 68px;
  border-radius: 6px;
  background-color: #fff;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.2s;
}

.add-variant-img-preview:hover {
  border-color: #496883;
}

.add-variant-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.add-variant-img-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  color: #94a3b8;
  font-size: 0.72rem;
}

.add-variant-img-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.btn-img-action-sm {
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-img-action-sm.btn-pick {
  background-color: #496883;
  color: #ffffff;
  border: 1px solid #496883;
}

.btn-img-action-sm.btn-pick:hover {
  background-color: #385269;
}

.btn-img-action-sm.btn-remove {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.btn-img-action-sm.btn-remove:hover {
  background-color: #fee2e2;
}

/* Responsive */
@media (max-width: 992px) {
  .filter-top-row {
    flex-direction: column;
    align-items: stretch;
    gap: 0.85rem;
  }
  .filter-buttons-right {
    justify-content: flex-end;
    width: 100%;
    flex-wrap: wrap;
  }
  .filter-bottom-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.75rem;
  }
}

@media (max-width: 600px) {
  .filter-bottom-row {
    grid-template-columns: 1fr;
  }
  .variant-page-container {
    padding: 1rem;
  }
  .variant-main-card {
    padding: 1rem;
  }
  .product-banner-header {
    flex-direction: column;
    gap: 0.75rem;
  }
  .filter-buttons-right {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-buttons-right button {
    width: 100%;
    justify-content: center;
  }
  .modal-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>