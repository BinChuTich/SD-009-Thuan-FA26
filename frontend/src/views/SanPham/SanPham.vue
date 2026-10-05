<template>
  <div class="product-page-container">
    <!-- Tiêu đề trang -->
    <div class="page-header-row">
      <h2 class="main-page-title">QUẢN LÝ SẢN PHẨM</h2>
      <div v-if="toast.show" :class="['toast-notification', toast.type]">
        {{ toast.message }}
      </div>
    </div>

    <!-- Khối khung trắng bao bọc toàn bộ chức năng -->
    <div class="product-card">
      <!-- 1. Hàng tìm kiếm và các nút chính sắp xếp full theo mẫu -->
      <div class="search-actions-bar">
        <!-- Cột trái: Tìm kiếm full width -->
        <div class="search-input-col">
          <label class="search-label">Tìm kiếm</label>
          <div class="input-inner-wrap">
            <span class="prefix-search-icon">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <input
                type="text"
                v-model="filters.keyword"
                @keyup.enter="handleSearch"
                @input="onKeywordChange"
                placeholder="Tìm theo mã SP / tên sản phẩm..."
                class="input-search-full"
            />
          </div>
        </div>

        <!-- Cột phải: 3 nút hành động (Đặt lại bộ lọc, Xuất Excel, + Thêm sản phẩm) -->
        <div class="action-buttons-col">
          <button class="btn btn-refresh" @click="resetFilters" title="Đặt lại bộ lọc">
            <span class="btn-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 4 23 10 17 10"></polyline>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
            </span>
            Đặt lại bộ lọc
          </button>
          <button class="btn btn-export" @click="handleExportExcel" :disabled="exporting" title="Xuất danh sách ra file Excel">
            <span class="btn-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
            </span>
            {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
          </button>
          <button class="btn btn-add" @click="goToAddPage" title="Thêm mới sản phẩm">
            <span class="btn-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
            </span>
            Thêm sản phẩm
          </button>
        </div>
      </div>

      <!-- 2. Khối 8 tiêu chí lọc thuộc tính -->
      <div class="filter-attributes-section">
        <div class="funnel-icon-wrap" title="Bộ lọc thuộc tính">
          <span class="funnel-icon">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
              <path d="M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"/>
            </svg>
          </span>
        </div>

        <div class="attribute-dropdowns-grid">
          <!-- 1. Thương hiệu -->
          <select v-model="filters.idThuongHieu" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Thương hiệu --</option>
            <option v-for="item in attributes.thuongHieu" :key="item.id" :value="item.id">
              {{ item.tenThuongHieu }}
            </option>
          </select>

          <!-- 2. Danh mục -->
          <select v-model="filters.idDanhMuc" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Danh mục --</option>
            <option v-for="item in attributes.danhMuc" :key="item.id" :value="item.id">
              {{ item.tenDanhMuc }}
            </option>
          </select>

          <!-- 3. Xuất xứ -->
          <select v-model="filters.idXuatXu" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Xuất xứ --</option>
            <option v-for="item in attributes.xuatXu" :key="item.id" :value="item.id">
              {{ item.tenXuatXu }}
            </option>
          </select>

          <!-- 4. Chất liệu -->
          <select v-model="filters.idChatLieu" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Chất liệu --</option>
            <option v-for="item in attributes.chatLieu" :key="item.id" :value="item.id">
              {{ item.tenChatLieu }}
            </option>
          </select>

          <!-- 5. Cổ áo -->
          <select v-model="filters.idCoAo" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Cổ áo --</option>
            <option v-for="item in attributes.coAo" :key="item.id" :value="item.id">
              {{ item.tenCoAo }}
            </option>
          </select>

          <!-- 6. Tay áo -->
          <select v-model="filters.idTayAo" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Tay áo --</option>
            <option v-for="item in attributes.tayAo" :key="item.id" :value="item.id">
              {{ item.tenTayAo }}
            </option>
          </select>

          <!-- 7. Họa tiết -->
          <select v-model="filters.idHoaTiet" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Họa tiết --</option>
            <option v-for="item in attributes.hoaTiet" :key="item.id" :value="item.id">
              {{ item.tenHoaTiet }}
            </option>
          </select>

          <!-- 8. Trạng thái -->
          <select v-model="filters.trangThai" @change="handleSearch" class="filter-pill-select">
            <option value="">-- Trạng thái --</option>
            <option :value="1">Đang kinh doanh</option>
            <option :value="0">Ngừng kinh doanh</option>
          </select>
        </div>
      </div>

      <!-- 3. Bảng dữ liệu sản phẩm -->
      <div class="table-responsive">
        <table class="product-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 100px; text-align: center;">Ảnh</th>
            <th style="width: 120px;">Mã SP</th>
            <th>Tên sản phẩm</th>
            <th style="width: 180px;">Phân loại</th>
            <th style="width: 150px;">Khoảng giá</th>
            <th style="width: 110px; text-align: center;">Tồn kho</th>
            <th style="width: 130px;">Ngày tạo</th>
            <th style="width: 150px; text-align: center;">Trạng thái</th>
            <th style="width: 140px; text-align: center;">Thao tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td colspan="10" class="empty-state">
              <div class="empty-box">
                <div class="loading-spinner"></div>
                <span class="empty-text">Đang tải dữ liệu sản phẩm...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="products.length === 0">
            <td colspan="10" class="empty-state">
              <div class="empty-box">
                <span class="empty-icon">
                  <svg viewBox="0 0 24 24" width="38" height="38" fill="currentColor" opacity="0.45">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v3c0 .55.45 1 1 1h1v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8h1c.55 0 1-.45 1-1V4c0-1.1-.9-2-2-2zm-1 6v12H5V8h14zM4 4h16v2H4V4zm5 6h6v2H9v-2z"/>
                  </svg>
                </span>
                <span class="empty-text">Không tìm thấy sản phẩm nào phù hợp với bộ lọc!</span>
                <button type="button" class="btn btn-refresh-sm" @click="handleReset">Làm mới bộ lọc</button>
              </div>
            </td>
          </tr>
          <tr v-for="(item, index) in products" :key="item.id">
            <!-- STT -->
            <td style="text-align: center;" class="stt-cell">
              {{ pageData.number * pageData.size + index + 1 }}
            </td>

            <!-- Ảnh sản phẩm -->
            <td style="text-align: center;">
              <div class="product-img-box">
                <img
                    :src="formatImageUrl(item.anhDaiDien) || defaultImage"
                    :alt="item.tenSanPham"
                    @error="onImgError"
                />
              </div>
            </td>

            <!-- Mã sản phẩm -->
            <td class="code-cell">
              <b>{{ item.maSanPham }}</b>
            </td>

            <!-- Tên sản phẩm -->
            <td class="product-name-cell">
              <div class="name-text">{{ item.tenSanPham }}</div>
              <div v-if="item.moTa" class="desc-text">{{ item.moTa }}</div>
            </td>

            <!-- Phân loại -->
            <td>
              <div class="attr-badges">
                <span class="attr-tag" v-if="item.tenThuongHieu">{{ item.tenThuongHieu }}</span>
                <span class="attr-tag" v-if="item.tenDanhMuc">{{ item.tenDanhMuc }}</span>
                <span class="attr-tag" v-if="item.tenChatLieu">{{ item.tenChatLieu }}</span>
              </div>
            </td>

            <!-- Giá tiền in đậm -->
            <td class="price-cell">
              <b>{{ formatPriceRange(item.minGia, item.maxGia) }}</b>
            </td>

            <!-- Số lượng tồn -->
            <td style="text-align: center;">
              <span :class="['stock-badge', item.tongSoLuong > 0 ? 'in-stock' : 'out-of-stock']">
                {{ item.tongSoLuong ?? 0 }}
              </span>
            </td>

            <!-- Ngày tạo -->
            <td class="date-cell">{{ formatDate(item.ngayTao) }}</td>

            <!-- Badge trạng thái kèm nút đổi -->
            <td style="text-align: center;">
              <button
                  :class="['status-toggle-btn', item.trangThai === 1 ? 'status-active' : 'status-inactive']"
                  @click="toggleStatus(item)"
                  title="Nhấn để thay đổi trạng thái"
              >
                {{ item.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
              </button>
            </td>

            <!-- Cột thao tác: Biến thể, Sửa, Xóa -->
            <!-- Cột thao tác: Sửa, Xóa (Chỉ icon, không cần text) -->
            <td style="text-align: center;">
              <div class="action-buttons-group">
                <button
                    class="btn-action btn-edit"
                    @click="goToEditPage(item)"
                    title="Chỉnh sửa thông tin sản phẩm"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
                  </svg>
                </button>
                <button
                    class="btn-action btn-delete"
                    @click="confirmDelete(item)"
                    title="Xóa sản phẩm"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. Thanh phân trang -->
      <div class="pagination-footer" v-if="pageData.totalPages > 0">
        <div class="pagination-info">
          Hiển thị <b>{{ products.length }}</b> / <b>{{ pageData.totalElements }}</b> sản phẩm (Trang {{ pageData.number + 1 }} / {{ pageData.totalPages }})
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

    <!-- MODAL XÁC NHẬN HÀNH ĐỘNG (XÓA / ĐỔI TRẠNG THÁI) -->
    <transition name="fade">
      <div v-if="confirmDialog.show" class="confirm-modal-overlay" @click="closeConfirmDialog">
        <div class="confirm-modal-box" @click.stop>
          <div class="confirm-icon-wrap" :class="confirmDialog.type">
            <span v-if="confirmDialog.type === 'danger'">⚠️</span>
            <span v-else-if="confirmDialog.type === 'info'">ℹ️</span>
            <span v-else>❓</span>
          </div>

          <h3 class="confirm-title">{{ confirmDialog.title }}</h3>
          <p class="confirm-message">{{ confirmDialog.message }}</p>

          <div class="confirm-actions">
            <button type="button" class="btn btn-confirm-cancel" @click="closeConfirmDialog">
              Hủy bỏ
            </button>
            <button
                type="button"
                :class="['btn', confirmDialog.type === 'danger' ? 'btn-confirm-danger' : 'btn-confirm-primary']"
                @click="executeConfirmedAction"
            >
              {{ confirmDialog.confirmText || 'Đồng ý' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'

const router = useRouter()

const defaultImage = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300&auto=format&fit=crop&q=80'

// Bộ lọc
const filters = ref({
  keyword: '',
  idThuongHieu: '',
  idDanhMuc: '',
  idXuatXu: '',
  idChatLieu: '',
  idCoAo: '',
  idTayAo: '',
  idHoaTiet: '',
  trangThai: '',
  page: 0,
  size: 10
})

// Dữ liệu thuộc tính
const attributes = ref({
  thuongHieu: [],
  danhMuc: [],
  xuatXu: [],
  chatLieu: [],
  coAo: [],
  tayAo: [],
  hoaTiet: [],
  mauSac: [],
  kichCo: []
})

// Danh sách sản phẩm & phân trang
const products = ref([])
const loading = ref(false)
const pageData = ref({
  number: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
})

// Hộp thoại xác nhận tương tác
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

const closeConfirmDialog = () => {
  confirmDialog.value.show = false
  confirmDialog.value.onConfirm = null
}

const executeConfirmedAction = async () => {
  const action = confirmDialog.value.onConfirm
  closeConfirmDialog()
  if (typeof action === 'function') {
    await action()
  }
}

// Chuyển trang Thêm sản phẩm
const goToAddPage = () => {
  router.push('/san-pham/them')
}

// Chuyển trang Sửa sản phẩm
const goToEditPage = (item) => {
  router.push(`/san-pham/chinh-sua/${item.id}`)
}

// Toast
const toast = ref({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// Lấy danh sách thuộc tính
const fetchAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      attributes.value = res.data
    }
  } catch (err) {
    console.error('Lỗi tải thuộc tính:', err)
  }
}

// Lấy danh sách sản phẩm
const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: filters.value.page,
      size: filters.value.size
    }
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idThuongHieu) params.idThuongHieu = filters.value.idThuongHieu
    if (filters.value.idDanhMuc) params.idDanhMuc = filters.value.idDanhMuc
    if (filters.value.idXuatXu) params.idXuatXu = filters.value.idXuatXu
    if (filters.value.idChatLieu) params.idChatLieu = filters.value.idChatLieu
    if (filters.value.idCoAo) params.idCoAo = filters.value.idCoAo
    if (filters.value.idTayAo) params.idTayAo = filters.value.idTayAo
    if (filters.value.idHoaTiet) params.idHoaTiet = filters.value.idHoaTiet
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/san-pham', { params })
    if (res.data) {
      products.value = res.data.content || []
      pageData.value = {
        number: res.data.pageNumber || 0,
        size: res.data.pageSize || 10,
        totalElements: res.data.totalElements || 0,
        totalPages: res.data.totalPages || 0
      }
    }
  } catch (err) {
    console.error('Lỗi tải sản phẩm:', err)
    showToast('Lỗi khi tải danh sách sản phẩm!', 'error')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  filters.value.page = 0
  fetchProducts()
}

let searchTimeout = null
const onKeywordChange = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleSearch()
  }, 350)
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    idThuongHieu: '',
    idDanhMuc: '',
    idXuatXu: '',
    idChatLieu: '',
    idCoAo: '',
    idTayAo: '',
    idHoaTiet: '',
    trangThai: '',
    page: 0,
    size: 10
  }
  fetchProducts()
}

const exporting = ref(false)

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const params = {}
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idThuongHieu) params.idThuongHieu = filters.value.idThuongHieu
    if (filters.value.idDanhMuc) params.idDanhMuc = filters.value.idDanhMuc
    if (filters.value.idXuatXu) params.idXuatXu = filters.value.idXuatXu
    if (filters.value.idChatLieu) params.idChatLieu = filters.value.idChatLieu
    if (filters.value.idCoAo) params.idCoAo = filters.value.idCoAo
    if (filters.value.idTayAo) params.idTayAo = filters.value.idTayAo
    if (filters.value.idHoaTiet) params.idHoaTiet = filters.value.idHoaTiet
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/san-pham/export-excel', {
      params,
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `danh_sach_san_pham_${Date.now()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    showToast('Xuất file Excel thành công!')
  } catch (err) {
    console.error(err)
    showToast('Lỗi khi xuất file Excel!', 'error')
  } finally {
    exporting.value = false
  }
}

const changePage = (p) => {
  if (p < 0 || p >= pageData.value.totalPages) return
  filters.value.page = p
  fetchProducts()
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

// Định dạng giá tiền
const formatPriceRange = (min, max) => {
  if (min == null && max == null) return 'Chưa có biến thể'
  if (min != null && max != null && min === max) {
    return Number(min).toLocaleString('vi-VN') + ' đ'
  }
  const minStr = min != null ? Number(min).toLocaleString('vi-VN') : '0'
  const maxStr = max != null ? Number(max).toLocaleString('vi-VN') : '0'
  return `${minStr} - ${maxStr} đ`
}

// Định dạng ngày
const formatDate = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleDateString('vi-VN')
}

const formatImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('data:') || url.startsWith('http://') || url.startsWith('https://')) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const onImgError = (e) => {
  e.target.src = defaultImage
}

// Thay đổi trạng thái sản phẩm có xác nhận
const toggleStatus = (item) => {
  const newStatus = item.trangThai === 1 ? 0 : 1
  const actionText = newStatus === 1 ? 'mở kinh doanh lại' : 'ngừng kinh doanh'
  openConfirm({
    title: 'Xác nhận thay đổi trạng thái',
    message: `Bạn có chắc chắn muốn ${actionText} sản phẩm "${item.tenSanPham}" không?`,
    type: 'info',
    confirmText: 'Xác nhận',
    onConfirm: async () => {
      try {
        await api.patch(`/api/san-pham/${item.id}/status`, null, {
          params: { trangThai: newStatus }
        })
        item.trangThai = newStatus
        showToast(`Đã chuyển trạng thái sang "${newStatus === 1 ? 'Kinh doanh' : 'Ngừng KD'}"`)
      } catch (err) {
        console.error('Lỗi cập nhật trạng thái:', err)
        showToast('Lỗi cập nhật trạng thái!', 'error')
      }
    }
  })
}

// Xóa sản phẩm có xác nhận
const confirmDelete = (item) => {
  openConfirm({
    title: 'Xác nhận xóa sản phẩm',
    message: `Bạn có chắc chắn muốn xóa sản phẩm "${item.tenSanPham}" không? Hành động này sẽ không thể hoàn tác!`,
    type: 'danger',
    confirmText: 'Xóa ngay',
    onConfirm: async () => {
      try {
        await api.delete(`/api/san-pham/${item.id}`)
        showToast('Xóa sản phẩm thành công!')
        fetchProducts()
      } catch (err) {
        console.error('Lỗi xóa sản phẩm:', err)
        const msg = err.response?.data?.message || 'Lỗi khi xóa sản phẩm!'
        showToast(msg, 'error')
      }
    }
  })
}

onMounted(() => {
  fetchAttributes()
  fetchProducts()
})
</script>

<style scoped>
/* Toàn bộ vùng hiển thị trang */
.product-page-container {
  padding: 1.25rem 1.5rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

/* Tiêu đề trang in hoa to rõ */
.main-page-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: 0.3px;
  margin: 0;
}

/* Toast notification */
.toast-notification {
  padding: 0.6rem 1.2rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  animation: slideIn 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.toast-notification.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}
.toast-notification.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Khối Card trắng */
.product-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.75rem;
}

/* 1. Hàng tìm kiếm và các nút chính sắp xếp full */
.search-actions-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.25rem;
  width: 100%;
}

.search-input-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.search-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  margin: 0;
}

.input-inner-wrap {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.prefix-search-icon {
  position: absolute;
  left: 0.95rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  pointer-events: none;
}

.input-search-full {
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

.input-search-full:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.action-buttons-col {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-shrink: 0;
}

/* Các loại nút bấm */
.btn {
  height: 2.5rem;
  padding: 0 1rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: none;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-icon {
  font-size: 0.9rem;
}

/* Nút Tìm kiếm */
.btn-search {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-search:hover {
  background-color: #385269;
}

/* Nút Làm mới */
.btn-refresh {
  background-color: #d2a764;
  color: #ffffff;
}
.btn-refresh:hover {
  background-color: #be9453;
}

.btn-export {
  background-color: #2e7d32;
  color: #ffffff;
}
.btn-export:hover:not(:disabled) {
  background-color: #256628;
}
.btn-export:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Nút + Thêm sản phẩm */
.btn-add {
  background-color: #edd9b8;
  color: #4a3e2e;
  flex-shrink: 0;
  margin-left: auto;
}
.btn-add:hover {
  background-color: #e4cda7;
}

/* 2. Cụm 8 tiêu chí lọc thuộc tính */
.filter-attributes-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 0.75rem 1rem;
  background: #fbfaf7;
  border: 1px solid #f0ede6;
  border-radius: 10px;
}

.funnel-icon-wrap {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.attribute-dropdowns-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem 1rem;
}

.filter-pill-select {
  height: 2.35rem;
  background-color: #ffffff;
  border: 1px solid #e2ded5;
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.88rem;
  color: #4f5a60;
  outline: none;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-pill-select:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.1);
}

/* 3. Bảng danh sách sản phẩm */
.table-responsive {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  min-width: 960px;
  border-collapse: collapse;
  font-size: 0.92rem;
}

.product-table th {
  background-color: #f8fafc;
  color: #6c787f;
  font-weight: 700;
  padding: 0.9rem 1rem;
  text-align: left;
  border-bottom: 2px solid #edf1f5;
  font-size: 0.9rem;
  white-space: nowrap;
}

.product-table td {
  padding: 0.9rem 1rem;
  border-bottom: 1px solid #f1f4f7;
  vertical-align: middle;
  color: #4a565c;
}

.product-table tr:hover td {
  background-color: #faf9f5;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem !important;
  color: #8c9ba5;
  font-size: 0.95rem;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  padding: 1rem 0;
  width: 100%;
}

.empty-icon {
  font-size: 2.2rem;
  line-height: 1;
}

.empty-text {
  font-size: 0.95rem;
  color: #71828d;
  font-weight: 600;
}

.btn-refresh-sm {
  margin-top: 0.25rem;
  padding: 0.4rem 0.9rem;
  background-color: #eef3f6;
  color: #496883;
  font-size: 0.82rem;
  font-weight: 600;
  border-radius: 6px;
  border: 1px solid #d5e1e8;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh-sm:hover {
  background-color: #496883;
  color: #ffffff;
}

.loading-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid #ccc;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  vertical-align: middle;
  margin-right: 6px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* STT số đỏ đậm */
.stt-cell {
  font-weight: 800;
  color: #e65228;
  font-size: 1rem;
}

/* Ô chứa ảnh sản phẩm */
.product-img-box {
  width: 60px;
  height: 60px;
  margin: 0 auto;
  border-radius: 8px;
  overflow: hidden;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eae5db;
}

.product-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.code-cell {
  font-size: 0.88rem;
  color: #496883;
}

/* Tên sản phẩm */
.product-name-cell {
  min-width: 160px;
}

.name-text {
  font-weight: 700;
  color: #2e3b40;
  font-size: 0.95rem;
}

.desc-text {
  font-size: 0.8rem;
  color: #8c9ba5;
  margin-top: 3px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.attr-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.attr-tag {
  display: inline-block;
  background: #f1f4f8;
  color: #556877;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 0.78rem;
  border: 1px solid #e1e7ed;
}

/* Giá tiền */
.price-cell {
  color: #1f272b;
  font-size: 0.95rem;
}

/* Tồn kho badge */
.stock-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.82rem;
}
.stock-badge.in-stock {
  background-color: #e8f5e9;
  color: #2e7d32;
}
.stock-badge.out-of-stock {
  background-color: #ffebee;
  color: #c62828;
}

/* Ngày tạo */
.date-cell {
  color: #6f7c82;
  font-size: 0.88rem;
}

/* Trạng thái */
.status-toggle-btn {
  display: inline-block;
  padding: 0.35rem 0.8rem;
  font-weight: 700;
  border-radius: 6px;
  font-size: 0.82rem;
  white-space: nowrap;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-toggle-btn.status-active {
  background-color: #edd9b8;
  color: #453a29;
  border-color: #d2a764;
}

.status-toggle-btn.status-active:hover {
  background-color: #e2cba6;
}

.status-toggle-btn.status-inactive {
  background-color: #f1f1f1;
  color: #888888;
  border-color: #cccccc;
}

.status-toggle-btn.status-inactive:hover {
  background-color: #e2e2e2;
}

/* Các nút hành động */
.action-buttons-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-action {
  width: 32px;
  height: 32px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  padding: 0;
}

.btn-action svg {
  display: block;
}

.btn-edit {
  color: var(--blue, #496883);
}
.btn-edit:hover {
  background-color: #eaf1f4;
  border-color: var(--blue, #496883);
  transform: scale(1.08);
}

.btn-delete {
  color: #dc2626;
}
.btn-delete:hover {
  background-color: #fef2f2;
  border-color: #dc2626;
  transform: scale(1.08);
}

/* 4. Thanh phân trang */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f4f8;
}

.pagination-info {
  font-size: 0.88rem;
  color: #6f7c82;
}

.pagination-center {
  display: flex;
  gap: 0.35rem;
}

.pg-box {
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  color: #3d4a50;
  transition: all 0.2s;
}

.pg-box:hover:not(:disabled) {
  border-color: #496883;
  color: #496883;
}

.pg-box:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pg-box.active {
  border-color: #d2a764;
  color: #ffffff;
  background-color: #d2a764;
}

/* MODAL */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
  animation: fadeIn 0.2s ease;
}

.modal-dialog {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 720px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f0ede6;
}

.modal-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #888;
  cursor: pointer;
}
.btn-close-modal:hover {
  color: #000;
}

.modal-form {
  padding: 1.5rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #3d4a50;
}

.form-label .required {
  color: #d9534f;
}

.form-control {
  height: 2.45rem;
  border: 1px solid #d5d9df;
  border-radius: 6px;
  padding: 0 0.8rem;
  font-size: 0.9rem;
  outline: none;
  transition: all 0.2s;
  background-color: #ffffff;
}

.form-control:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.1);
}

textarea.form-control {
  height: auto;
  padding: 0.6rem 0.8rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #f0ede6;
}

.btn-secondary {
  background-color: #f1f4f8;
  color: #556877;
}
.btn-secondary:hover {
  background-color: #e2e8f0;
}

.btn-primary {
  background-color: #496883;
  color: #ffffff;
}
.btn-primary:hover:not(:disabled) {
  background-color: #385269;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 992px) {
  .search-actions-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 0.85rem;
  }
  .action-buttons-col {
    justify-content: flex-end;
    flex-wrap: wrap;
    width: 100%;
  }
}

@media (max-width: 600px) {
  .product-page-container {
    padding: 1rem 0.75rem 2rem;
  }
  .product-card {
    padding: 1rem;
  }
  .action-buttons-col {
    flex-direction: column;
    align-items: stretch;
  }
  .action-buttons-col .btn {
    width: 100%;
  }
  .filter-attributes-section {
    flex-direction: column;
    align-items: stretch;
    padding: 0.75rem;
  }
  .funnel-icon-wrap {
    display: none;
  }
  .attribute-dropdowns-grid {
    grid-template-columns: 1fr;
  }
  .pagination-footer {
    flex-direction: column;
    gap: 0.75rem;
    align-items: center;
    text-align: center;
  }
  .pagination-center {
    flex-wrap: wrap;
    justify-content: center;
  }
  .modal-dialog {
    max-width: 95%;
    margin: 10px;
  }
}

/* Modal xác nhận chuyên nghiệp */
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 440px;
  padding: 1.75rem 1.5rem 1.5rem;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: modalScale 0.2s ease-out;
}

@keyframes modalScale {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.confirm-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  margin: 0 auto 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.confirm-icon-wrap.primary {
  background: #eff6ff;
  color: #2563eb;
}

.confirm-icon-wrap.danger {
  background: #fef2f2;
  color: #dc2626;
}

.confirm-icon-wrap.info {
  background: #f0fdf4;
  color: #16a34a;
}

.confirm-title {
  margin: 0 0 0.5rem;
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
}

.confirm-message {
  margin: 0 0 1.5rem;
  font-size: 0.92rem;
  color: #64748b;
  line-height: 1.5;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
}

.btn-confirm-cancel {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1.4rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-cancel:hover {
  background: #e2e8f0;
}

.btn-confirm-primary {
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  padding: 0.6rem 1.4rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-primary:hover {
  background: #385269;
}

.btn-confirm-danger {
  background: #dc2626;
  color: #ffffff;
  border: none;
  padding: 0.6rem 1.4rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-danger:hover {
  background: #b91c1c;
}
</style>