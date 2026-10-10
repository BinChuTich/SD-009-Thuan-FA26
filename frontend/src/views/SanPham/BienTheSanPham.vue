<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'

const route = useRoute()
const router = useRouter()

// SVG Placeholder khi chưa có ảnh
const defaultImage = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1.5'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E"

// Thông tin sản phẩm hiện tại
const currentProduct = ref(null)
const productsList = ref([])

// Dữ liệu bộ lọc
const filters = ref({
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
    anhDaiDien: item.anhDaiDien || currentProduct.value?.anhDaiDien,
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
    anhDaiDien: item.anhDaiDien || currentProduct.value?.anhDaiDien
  }
  detailModal.value.show = true
}

const closeDetailModal = () => {
  detailModal.value.show = false
}

const switchToEditFromDetail = () => {
  const item = { ...detailModal.value.data }
  closeDetailModal()
  openEditModal(item)
}

const blockInvalidIntegerKeys = (e) => {
  if (['-', '+', 'e', 'E', '.', ','].includes(e.key)) {
    e.preventDefault()
  }
}

// Chặn dán nội dung không phải số nguyên dương (cho phép dấu chấm/phẩy phân cách hàng nghìn khi copy paste)
const handleIntegerPaste = (e) => {
  const pasteData = e.clipboardData?.getData('text') || ''
  const clean = pasteData.replace(/[.,\s]/g, '')
  if (!/^\d+$/.test(clean.trim())) {
    e.preventDefault()
    showToast('Chỉ được dán số nguyên dương!', 'warning')
  }
}

// Format số nguyên có dấu chấm phân cách hàng nghìn (VD: 100000 -> "100.000", 10000 -> "10.000")
const formatNumberWithDots = (val) => {
  if (val === null || val === undefined || val === '') return ''
  const clean = String(val).replace(/\D/g, '')
  if (!clean) return ''
  return clean.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
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

// Lấy danh sách biến thể
const fetchVariants = async () => {
  loading.value = true
  try {
    const params = {
      page: filters.value.page,
      size: filters.value.size
    }
    const productId = route.query.sanPhamId || route.params.id
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
  filters.value = {
    keyword: '',
    idMauSac: '',
    idKichCo: '',
    trangThai: '',
    page: 0,
    size: 10
  }
  fetchVariants()
}

// Xuất file Excel
const exporting = ref(false)
const handleExportExcel = async () => {
  exporting.value = true
  try {
    const params = {}
    const productId = route.query.sanPhamId || route.params.id
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
  if (price == null) return '0 đ'
  return Number(price).toLocaleString('vi-VN') + ' đ'
}

const formatImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('data:') || url.startsWith('http://') || url.startsWith('https://')) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const onImgError = (e) => {
  e.target.src = defaultImage
}

const goToProductList = () => {
  router.push('/san-pham')
}

// Khởi tạo
onMounted(async () => {
  await fetchAttributes()
  const productId = route.query.sanPhamId || route.params.id
  if (productId) {
    await fetchProductInfo(productId)
  }
  await fetchVariants()
})

watch(() => route.query.sanPhamId, async (newId) => {
  if (newId) {
    await fetchProductInfo(newId)
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
    <div class="breadcrumb-nav-bar">
      <div class="nav-breadcrumbs">
        <span class="crumb-link" @click="goToProductList">Quản lý sản phẩm</span>
        <span class="crumb-slash">/</span>
        <span class="crumb-current">Biến thể sản phẩm</span>
      </div>
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

        <button class="btn-back-link" @click="goToProductList" title="Quay lại danh sách sản phẩm">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          Danh sách sản phẩm
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
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 4 23 10 17 10"></polyline>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
              Đặt lại bộ lọc
            </button>

            <button class="btn-outline-qr" @click="openQrModal" title="Tải / Xem mã QR biến thể">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              Tải QR
            </button>

            <button class="btn-outline-excel" @click="handleExportExcel" :disabled="exporting" title="Xuất danh sách ra file Excel">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
            </button>
          </div>
        </div>

        <!-- Hàng 2: Ba bộ chọn Dropdown (Màu sắc, Kích cỡ, Trạng thái) -->
        <div class="filter-bottom-row">
          <div class="dropdown-filter-item">
            <label class="dropdown-label">Màu sắc</label>
            <select v-model="filters.idMauSac" @change="handleFilterChange" class="dropdown-select">
              <option value="">Tất cả màu sắc</option>
              <option v-for="c in colors" :key="c.id" :value="c.id">{{ c.tenMauSac }}</option>
            </select>
          </div>

          <div class="dropdown-filter-item">
            <label class="dropdown-label">Kích cỡ</label>
            <select v-model="filters.idKichCo" @change="handleFilterChange" class="dropdown-select">
              <option value="">Tất cả kích cỡ</option>
              <option v-for="s in sizes" :key="s.id" :value="s.id">{{ s.tenKichCo }}</option>
            </select>
          </div>

          <div class="dropdown-filter-item">
            <label class="dropdown-label">Trạng thái</label>
            <select v-model="filters.trangThai" @change="handleFilterChange" class="dropdown-select">
              <option value="">Tất cả trạng thái</option>
              <option :value="1">Đang bán</option>
              <option :value="0">Ngừng bán</option>
            </select>
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
            <th style="width: 90px;">Mã SP</th>
            <th style="width: 140px;">Mã CTSP</th>
            <th style="width: 65px; text-align: center;">Ảnh</th>
            <th style="width: 120px;">Màu sắc</th>
            <th style="width: 80px; text-align: center;">Kích cỡ</th>
            <th style="width: 90px; text-align: center;">Số lượng</th>
            <th style="width: 120px; text-align: right;">Giá bán</th>
            <th style="width: 80px; text-align: center;">Giảm</th>
            <th style="width: 110px; text-align: center;">Trạng thái</th>
            <th style="width: 130px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td colspan="12" class="empty-cell">
              <div class="loading-state">
                <span class="loading-spinner"></span> Đang tải danh sách biến thể...
              </div>
            </td>
          </tr>
          <tr v-else-if="variants.length === 0">
            <td colspan="12" class="empty-cell">
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

            <!-- Mã SP -->
            <td class="cell-bold-code">
              {{ item.maSanPham || currentProduct?.maSanPham || '—' }}
            </td>

            <!-- Mã CTSP -->
            <td class="cell-code">
              {{ item.maChiTietSanPham }}
            </td>

            <!-- Ảnh -->
            <td style="text-align: center;">
              <div class="thumb-img-box">
                <img
                    :src="formatImageUrl(item.anhDaiDien || currentProduct?.anhDaiDien)"
                    @error="onImgError"
                    alt="Variant thumbnail"
                />
              </div>
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

            <!-- Giá bán -->
            <td style="text-align: right;" class="cell-price">
              {{ formatPrice(item.giaBan) }}
            </td>

            <!-- Giảm -->
            <td style="text-align: center;" class="cell-discount">
              —
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
                <!-- 1. Icon Kinh doanh (Power icon) -->
                <button
                    class="btn-icon-round btn-status-toggle"
                    :class="{ 'is-active': item.trangThai === 1 }"
                    @click="toggleVariantStatus(item)"
                    :title="item.trangThai === 1 ? 'Ngừng kinh doanh' : 'Bật kinh doanh'"
                >
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                    <line x1="12" y1="2" x2="12" y2="12"></line>
                  </svg>
                </button>

                <!-- 2. Icon Sửa (Pencil icon) -->
                <button
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
          <div class="page-size-selector">
            <span class="page-size-label">Hiển thị:</span>
            <select v-model.number="filters.size" @change="handlePageSizeChange" class="page-size-select">
              <option :value="5">5</option>
              <option :value="10">10</option>
              <option :value="15">15</option>
              <option :value="20">20</option>
              <option :value="25">25</option>
            </select>
            <span class="page-size-unit">/ trang</span>
          </div>
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
          <!-- Cột trái: Ảnh to + 2 thẻ thông tin Màu sắc, Kích cỡ -->
          <div class="left-variant-preview">
            <div class="large-img-card">
              <img
                  :src="formatImageUrl(editModal.data.anhDaiDien || currentProduct?.anhDaiDien)"
                  @error="onImgError"
                  alt="Variant big preview"
                  class="preview-img-tag"
              />
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
                    type="number"
                    min="0"
                    v-model.number="editModal.data.giaBan"
                    class="field-input-control field-price-focus"
                    placeholder="0"
                    @keydown="blockInvalidIntegerKeys"
                />
                <div v-if="editModal.data.giaBan && editModal.data.giaBan > 0" class="modal-price-preview">
                  👉 {{ formatNumberWithDots(editModal.data.giaBan) }} đ
                </div>
                <span class="err-text" v-if="editModal.errors.giaBan">{{ editModal.errors.giaBan }}</span>
              </div>

              <div class="variant-field-group">
                <label class="field-label-text">TRẠNG THÁI</label>
                <select v-model.number="editModal.data.trangThai" class="field-input-control field-select-control">
                  <option :value="1">Đang bán</option>
                  <option :value="0">Ngừng bán</option>
                </select>
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
                  :src="formatImageUrl(detailModal.data.anhDaiDien || currentProduct?.anhDaiDien)"
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
              <button class="btn-detail-edit" @click="switchToEditFromDetail">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
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
    <div class="modal-backdrop" v-if="confirmDialog.show" @click.self="closeConfirm">
      <div class="modal-dialog-box confirm-box">
        <div class="modal-header-row">
          <h3 class="modal-dialog-title">{{ confirmDialog.title }}</h3>
          <button class="modal-close-icon" @click="closeConfirm">&times;</button>
        </div>
        <div class="modal-body-content">
          <p class="confirm-message-text">{{ confirmDialog.message }}</p>
        </div>
        <div class="modal-footer-row">
          <button class="btn btn-cancel-modal" @click="closeConfirm">Hủy bỏ</button>
          <button
              :class="['btn', confirmDialog.type === 'danger' ? 'btn-danger-confirm' : 'btn-save-modal']"
              @click="executeConfirm"
          >
            {{ confirmDialog.confirmText }}
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
  padding: 1.25rem 1.75rem 2.5rem;
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
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  background-color: #ffffff;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-outline-gold {
  border: 1px solid #d2a764;
  color: #b0823b;
}

.btn-outline-gold:hover {
  background-color: #fdf9f3;
  transform: translateY(-1px);
}

.btn-outline-qr {
  border: 1px solid #e2e8f0;
  color: #475569;
}

.btn-outline-qr:hover {
  background-color: #f8fafc;
  color: #0f172a;
  transform: translateY(-1px);
}

.btn-outline-excel {
  border: 1px solid #2e7d32;
  color: #2e7d32;
}

.btn-outline-excel:hover {
  background-color: #f0fdf4;
  transform: translateY(-1px);
}

/* Hàng bộ chọn dropdown 2 */
.filter-bottom-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
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
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 1.25rem;
}

.variant-data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.variant-data-table th {
  background-color: #f8fafc;
  color: #475569;
  font-weight: 700;
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

.variant-data-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  vertical-align: middle;
}

.variant-table-row:hover {
  background-color: #fbfcfe;
}

.table-chk {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--blue, #496883);
}

.cell-stt {
  font-weight: 700;
  color: #64748b;
}

.cell-bold-code {
  font-weight: 700;
  color: #0f172a;
}

.cell-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.85rem;
  color: #475569;
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
  color: #0f172a;
}

.cell-discount {
  color: #94a3b8;
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
  background-color: #ecfdf5;
  color: #059669;
}

.status-pill.status-inactive {
  background-color: #fef2f2;
  color: #dc2626;
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
  color: #e11d48;
  background-color: #fff1f2;
  border-color: #ffe4e6;
}

.btn-status-toggle.is-active {
  color: #059669;
  background-color: #ecfdf5;
  border-color: #a7f3d0;
}

.btn-status-toggle:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.08);
}

/* 2. Icon Sửa (Pencil) */
.btn-edit-variant {
  color: var(--blue, #496883);
  background-color: #f0f7fa;
  border-color: #dbeafe;
}

.btn-edit-variant:hover {
  background-color: #e0f2fe;
  color: #0284c7;
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.08);
}

/* 3. Icon Xem chi tiết (Eye) */
.btn-view-variant {
  color: #0284c7;
  background-color: #f0f9ff;
  border-color: #bae6fd;
}

.btn-view-variant:hover {
  background-color: #e0f2fe;
  color: #0369a1;
  transform: scale(1.1);
  box-shadow: 0 2px 5px rgba(2, 132, 199, 0.15);
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
  background-color: var(--blue, #496883);
  border: 1px solid var(--blue, #496883);
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
  background-color: #3d576e;
  border-color: #3d576e;
  transform: translateY(-1px);
}

.btn-detail-close {
  height: 2.5rem;
  padding: 0 1.25rem;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-detail-close:hover {
  background-color: #f1f5f9;
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
  font-size: 0.88rem;
  color: #64748b;
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
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.pg-box:hover:not(:disabled) {
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
  background-color: #f8fafc;
}

.pg-box.active {
  background-color: var(--blue, #496883);
  border-color: var(--blue, #496883);
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

@keyframes modalIn {
  from { opacity: 0; transform: scale(0.96) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.qr-dialog-box {
  max-width: 680px;
}

.confirm-box {
  max-width: 440px;
}

.modal-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
}

.modal-dialog-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #0f172a;
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
  border-top: 1px solid #f1f5f9;
  background-color: #f8fafc;
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
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
}

.btn-cancel-modal:hover {
  background-color: #f1f5f9;
}

.btn-save-modal {
  background-color: var(--blue, #496883);
  border: 1px solid var(--blue, #496883);
  color: #ffffff;
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
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  overflow: hidden;
}

.preview-img-tag {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
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

.modal-price-preview {
  font-size: 0.78rem;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
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
    grid-template-columns: 1fr;
    gap: 0.75rem;
  }
}

@media (max-width: 600px) {
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