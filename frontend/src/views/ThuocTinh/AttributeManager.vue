<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/api.js'
import * as XLSX from 'xlsx'

const props = defineProps({
  attrType: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  codePrefix: {
    type: String,
    default: 'TT'
  },
  hasColor: {
    type: Boolean,
    default: false
  }
})

// Dữ liệu danh sách
const items = ref([])
const loading = ref(false)

// Bộ lọc
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(0)
const pageSize = ref(10)

// Modal Thêm mới
const showAddModal = ref(false)
// Modal Cập nhật
const showEditModal = ref(false)

// Form data dùng chung cho Thêm / Cập nhật
const formData = ref({
  id: null,
  ma: '',
  ten: '',
  maHex: '#1e293b',
  trangThai: 1
})
const formErrors = ref({})

// Toast notification
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
  }, 3500)
}

// Dialog xác nhận thực hiện (Thêm, Cập nhật, Xóa)
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  type: 'primary',
  confirmText: 'Đồng ý',
  onConfirm: null
})

const openConfirm = ({ title, message, type = 'primary', confirmText = 'Đồng ý', onConfirm }) => {
  confirmDialog.value = { show: true, title, message, type, confirmText, onConfirm }
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

// Lấy danh sách từ API
const fetchItems = async () => {
  loading.value = true
  try {
    const res = await api.get(`/api/thuoc-tinh/type/${props.attrType}`)
    if (Array.isArray(res.data)) {
      items.value = res.data.map(item => {
        const ma = item.maThuongHieu || item.maDanhMuc || item.maChatLieu || item.maXuatXu ||
            item.maCoAo || item.maTayAo || item.maHoaTiet || item.maMauSac || item.maKichCo || ''
        const ten = item.tenThuongHieu || item.tenDanhMuc || item.tenChatLieu || item.tenXuatXu ||
            item.tenCoAo || item.tenTayAo || item.tenHoaTiet || item.tenMauSac || item.tenKichCo || ''
        return {
          id: item.id,
          ma,
          ten,
          maHex: item.maHex || '#1e293b',
          trangThai: item.trangThai != null ? item.trangThai : 1,
          raw: item
        }
      })
    }
  } catch (err) {
    console.error(`Lỗi tải danh sách ${props.title}:`, err)
    showToast('Lỗi tải dữ liệu', `Không thể tải danh sách ${props.title}`, 'error')
  } finally {
    loading.value = false
  }
}

// Lọc dữ liệu hiển thị
const filteredItems = computed(() => {
  let list = items.value
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(item =>
        (item.ten && item.ten.toLowerCase().includes(kw)) ||
        (item.ma && item.ma.toLowerCase().includes(kw))
    )
  }
  if (filterStatus.value !== '') {
    list = list.filter(item => String(item.trangThai) === String(filterStatus.value))
  }
  return list
})

// Phân trang
const totalPages = computed(() => {
  return Math.ceil(filteredItems.value.length / pageSize.value) || 1
})

const paginatedItems = computed(() => {
  const start = currentPage.value * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

const changePage = (p) => {
  if (p >= 0 && p < totalPages.value) {
    currentPage.value = p
  }
}

// Đặt lại bộ lọc
const resetFilters = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  currentPage.value = 0
}

// Sinh mã ngẫu nhiên cho form thêm mới
const generateRandomCode = () => {
  const randNum = Math.floor(1000 + Math.random() * 9000)
  return `${props.codePrefix}_${randNum}`
}

// Mở modal Thêm mới
const openAddModal = () => {
  formErrors.value = {}
  formData.value = {
    id: null,
    ma: generateRandomCode(),
    ten: '',
    maHex: '#1e293b',
    trangThai: 1
  }
  showAddModal.value = true
}

const closeAddModal = () => {
  showAddModal.value = false
}

// Mở modal Cập nhật khi nhấn icon Con Mắt
const openEditModal = (item) => {
  formErrors.value = {}
  formData.value = {
    id: item.id,
    ma: item.ma,
    ten: item.ten,
    maHex: item.maHex || '#1e293b',
    trangThai: item.trangThai != null ? item.trangThai : 1
  }
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
}

// Validate form
const validateForm = () => {
  const errors = {}
  if (!formData.value.ten || !formData.value.ten.trim()) {
    errors.ten = `Tên ${props.title.toLowerCase()} không được để trống!`
  } else if (formData.value.ten.trim().length > 255) {
    errors.ten = `Tên ${props.title.toLowerCase()} không quá 255 ký tự!`
  }

  if (props.hasColor) {
    if (!formData.value.maHex || !/^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(formData.value.maHex.trim())) {
      errors.maHex = 'Mã màu HEX không hợp lệ (Ví dụ: #FF0000)!'
    }
  }

  formErrors.value = errors
  return Object.keys(errors).length === 0
}

// Xác nhận Thêm mới
const promptSaveNew = () => {
  if (!validateForm()) return

  openConfirm({
    title: `Xác nhận thêm ${props.title.toLowerCase()}`,
    message: `Bạn có chắc chắn muốn thêm mới ${props.title.toLowerCase()} "${formData.value.ten.trim()}" không?`,
    type: 'primary',
    confirmText: 'Xác nhận lưu',
    onConfirm: async () => {
      try {
        const payload = {
          ma: formData.value.ma?.trim() || undefined,
          ten: formData.value.ten.trim(),
          maHex: props.hasColor ? formData.value.maHex : undefined,
          trangThai: Number(formData.value.trangThai)
        }
        await api.post(`/api/thuoc-tinh/type/${props.attrType}`, payload)
        showToast('Thành công', `Đã thêm mới ${props.title.toLowerCase()} thành công!`)
        closeAddModal()
        await fetchItems()
      } catch (err) {
        console.error('Lỗi thêm thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể thêm mới!'
        showToast('Lỗi', msg, 'error')
      }
    }
  })
}

// Xác nhận Cập nhật
const promptSaveUpdate = () => {
  if (!validateForm()) return

  openConfirm({
    title: `Xác nhận cập nhật ${props.title.toLowerCase()}`,
    message: `Bạn có chắc chắn muốn cập nhật thông tin ${props.title.toLowerCase()} "${formData.value.ten.trim()}" không?`,
    type: 'primary',
    confirmText: 'Xác nhận cập nhật',
    onConfirm: async () => {
      try {
        const payload = {
          ma: formData.value.ma?.trim() || undefined,
          ten: formData.value.ten.trim(),
          maHex: props.hasColor ? formData.value.maHex : undefined,
          trangThai: Number(formData.value.trangThai)
        }
        await api.put(`/api/thuoc-tinh/type/${props.attrType}/${formData.value.id}`, payload)
        showToast('Thành công', `Cập nhật ${props.title.toLowerCase()} thành công!`)
        closeEditModal()
        await fetchItems()
      } catch (err) {
        console.error('Lỗi cập nhật thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể cập nhật!'
        showToast('Lỗi', msg, 'error')
      }
    }
  })
}

// Xác nhận Xóa
const confirmDeleteItem = (item) => {
  openConfirm({
    title: `Xác nhận xóa ${props.title.toLowerCase()}`,
    message: `Bạn có chắc chắn muốn xóa ${props.title.toLowerCase()} "${item.ten}"? Nếu đã được sử dụng trong sản phẩm, hệ thống sẽ chặn xóa.`,
    type: 'danger',
    confirmText: 'Xác nhận xóa',
    onConfirm: async () => {
      try {
        await api.delete(`/api/thuoc-tinh/type/${props.attrType}/${item.id}`)
        showToast('Thành công', `Đã xóa ${props.title.toLowerCase()} "${item.ten}"!`)
        await fetchItems()
      } catch (err) {
        console.error('Lỗi xóa thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể xóa vì đang có sản phẩm sử dụng!'
        showToast('Không thể xóa', msg, 'error')
      }
    }
  })
}

// Xuất file Excel
const exportExcel = () => {
  try {
    const dataToExport = filteredItems.value.map((item, index) => {
      const row = {
        'STT': index + 1,
        [`MÃ ${props.title.toUpperCase()}`]: item.ma,
        [`TÊN ${props.title.toUpperCase()}`]: item.ten
      }
      if (props.hasColor) {
        row['MÃ MÀU HEX'] = item.maHex
      }
      row['TRẠNG THÁI'] = item.trangThai === 1 ? 'Kinh doanh' : 'Ngừng kinh doanh'
      return row
    })

    if (dataToExport.length === 0) {
      showToast('Thông báo', 'Không có dữ liệu để xuất Excel!', 'warning')
      return
    }

    const worksheet = XLSX.utils.json_to_sheet(dataToExport)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, props.title)
    const fileName = `Danh_Sach_${props.title.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0, 10)}.xlsx`
    XLSX.writeFile(workbook, fileName)
    showToast('Thành công', `Đã xuất ${dataToExport.length} dòng ra file Excel!`)
  } catch (err) {
    console.error('Lỗi xuất Excel:', err)
    showToast('Lỗi', 'Không thể xuất file Excel!', 'error')
  }
}

// Theo dõi khi thay đổi attrType
watch(() => props.attrType, () => {
  resetFilters()
  fetchItems()
})

onMounted(() => {
  fetchItems()
})
</script>

<template>
  <div class="attribute-management-page">
    <!-- 1. Header & Breadcrumb -->
    <div class="page-header-row">
      <div class="header-left">
        <h2 class="page-title">Quản Lý {{ title }}</h2>
        <div class="breadcrumb-trail">
          <router-link to="/" class="bc-link">Trang chủ</router-link>
          <span class="bc-sep">|</span>
          <router-link to="/san-pham" class="bc-link">Sản phẩm</router-link>
          <span class="bc-sep">|</span>
          <span class="bc-current">{{ title }}</span>
        </div>
      </div>
    </div>

    <!-- 2. Khung bộ lọc tìm kiếm -->
    <div class="filter-card">
      <div class="filter-card-header">
        <span class="filter-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
            <path d="M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"/>
          </svg>
        </span>
        <h3 class="filter-card-title">Bộ lọc tìm kiếm {{ title }}</h3>
      </div>

      <div class="filter-card-body">
        <div class="filter-inputs-row">
          <!-- Từ khóa tìm kiếm -->
          <div class="filter-item search-item">
            <label class="filter-label">Từ khóa tìm kiếm</label>
            <div class="search-input-wrap">
              <span class="search-icon-slot">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                  <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
                </svg>
              </span>
              <input
                  type="text"
                  v-model="searchKeyword"
                  :placeholder="`Tìm theo mã hoặc tên ${title.toLowerCase()}...`"
                  class="filter-input"
              />
              <button v-if="searchKeyword" class="btn-clear-search" @click="searchKeyword = ''">✕</button>
            </div>
          </div>

          <!-- Trạng thái -->
          <div class="filter-item status-item">
            <label class="filter-label">Trạng thái</label>
            <div class="select-wrap">
              <select v-model="filterStatus" class="filter-select">
                <option value="">Tất cả trạng thái</option>
                <option value="1">Kinh doanh</option>
                <option value="0">Ngừng kinh doanh</option>
              </select>
            </div>
          </div>

          <!-- Nhóm các nút hành động bên phải -->
          <div class="filter-actions-group">
            <button type="button" class="btn-filter-action btn-reset" @click="resetFilters" title="Đặt lại bộ lọc">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M23 4v6h-6"></path>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
              <span>Đặt lại</span>
            </button>

            <button type="button" class="btn-filter-action btn-add-new" @click="openAddModal">
              <span>+ Thêm mới</span>
            </button>

            <button type="button" class="btn-filter-action btn-export-excel" @click="exportExcel">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor">
                <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
              </svg>
              <span>Xuất Excel</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. Bảng dữ liệu danh sách thuộc tính -->
    <div class="data-table-card">
      <div class="table-responsive">
        <table class="styled-attr-table">
          <thead>
          <tr>
            <th style="width: 70px; text-align: center;">STT</th>
            <th style="width: 200px;">MÃ {{ title.toUpperCase() }}</th>
            <th>TÊN {{ title.toUpperCase() }}</th>
            <th v-if="hasColor" style="width: 140px; text-align: center;">MÃ MÀU</th>
            <th style="width: 160px; text-align: center;">TRẠNG THÁI</th>
            <th style="width: 130px; text-align: center;">HÀNH ĐỘNG</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td :colspan="hasColor ? 6 : 5" class="empty-state-cell">
              <div class="loading-wrap">
                <span class="loading-spinner"></span> Đang tải dữ liệu {{ title.toLowerCase() }}...
              </div>
            </td>
          </tr>
          <tr v-else-if="filteredItems.length === 0">
            <td :colspan="hasColor ? 6 : 5" class="empty-state-cell">
              Không tìm thấy {{ title.toLowerCase() }} nào phù hợp!
            </td>
          </tr>
          <tr v-else v-for="(item, index) in paginatedItems" :key="item.id" class="data-row">
            <!-- STT -->
            <td style="text-align: center;" class="cell-stt">
              {{ currentPage * pageSize + index + 1 }}
            </td>

            <!-- Mã thuộc tính -->
            <td class="cell-code">
              <strong>{{ item.ma || '—' }}</strong>
            </td>

            <!-- Tên thuộc tính -->
            <td class="cell-name">
              {{ item.ten || '—' }}
            </td>

            <!-- Màu sắc (nếu có) -->
            <td v-if="hasColor" style="text-align: center;" class="cell-color">
              <div class="color-indicator-wrap">
                <span class="color-preview-circle" :style="{ backgroundColor: item.maHex || '#888' }"></span>
                <span class="color-hex-text">{{ item.maHex || '—' }}</span>
              </div>
            </td>

            <!-- Trạng thái -->
            <td style="text-align: center;">
              <span :class="['status-badge', item.trangThai === 1 ? 'badge-active' : 'badge-inactive']">
                {{ item.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
              </span>
            </td>

            <!-- Hành động: Con Mắt (Cập nhật) & Thùng Rác (Xóa) -->
            <td style="text-align: center;">
              <div class="action-btn-group">
                <!-- Icon Mắt (Cập nhật) -->
                <button
                    type="button"
                    class="btn-row-action btn-view-edit"
                    @click="openEditModal(item)"
                    :title="`Cập nhật ${title.toLowerCase()}`"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                    <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                  </svg>
                </button>

                <!-- Icon Xóa (Thùng rác) -->
                <button
                    type="button"
                    class="btn-row-action btn-delete-item"
                    @click="confirmDeleteItem(item)"
                    :title="`Xóa ${title.toLowerCase()}`"
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

      <!-- Phân trang -->
      <div v-if="filteredItems.length > 0" class="table-pagination-footer">
        <div class="pagination-info">
          Hiển thị <strong>{{ paginatedItems.length }}</strong> / <strong>{{ filteredItems.length }}</strong> bản ghi
        </div>
        <div class="pagination-controls">
          <button
              class="page-btn"
              :disabled="currentPage === 0"
              @click="changePage(currentPage - 1)"
          >
            ‹ Trước
          </button>
          <span class="page-indicator">Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
              class="page-btn"
              :disabled="currentPage >= totalPages - 1"
              @click="changePage(currentPage + 1)"
          >
            Sau ›
          </button>
        </div>
      </div>
    </div>

    <!-- 4. MODAL THÊM MỚI -->
    <div v-if="showAddModal" class="modal-backdrop-overlay" @click.self="closeAddModal">
      <div class="modal-card-dialog">
        <div class="modal-card-header">
          <h4 class="modal-card-title">+ THÊM {{ title.toUpperCase() }} MỚI</h4>
          <button class="modal-close-cross" @click="closeAddModal">✕</button>
        </div>

        <div class="modal-card-body">
          <!-- Mã thuộc tính -->
          <div class="form-field-group">
            <label class="form-field-label">Mã {{ title.toLowerCase() }} <span class="required-star">*</span></label>
            <input
                type="text"
                v-model="formData.ma"
                class="form-field-input"
                placeholder="Ví dụ: TH_9852"
            />
            <span class="field-error-msg" v-if="formErrors.ma">{{ formErrors.ma }}</span>
          </div>

          <!-- Tên thuộc tính -->
          <div class="form-field-group">
            <label class="form-field-label">Tên {{ title.toLowerCase() }} <span class="required-star">*</span></label>
            <input
                type="text"
                v-model="formData.ten"
                class="form-field-input"
                :placeholder="`Nhập tên ${title.toLowerCase()}`"
            />
            <span class="field-error-msg" v-if="formErrors.ten">{{ formErrors.ten }}</span>
          </div>

          <!-- Mã màu (nếu là Màu sắc) -->
          <div v-if="hasColor" class="form-field-group">
            <label class="form-field-label">Mã màu (Hex) <span class="required-star">*</span></label>
            <div class="color-picker-input-group">
              <input type="color" v-model="formData.maHex" class="color-picker-box" />
              <input
                  type="text"
                  v-model="formData.maHex"
                  class="form-field-input"
                  placeholder="#1e293b"
              />
            </div>
            <span class="field-error-msg" v-if="formErrors.maHex">{{ formErrors.maHex }}</span>
          </div>

          <!-- Trạng thái hoạt động -->
          <div class="form-field-group">
            <label class="form-field-label">Trạng thái hoạt động</label>
            <select v-model.number="formData.trangThai" class="form-field-input form-field-select">
              <option :value="1">Kinh doanh</option>
              <option :value="0">Ngừng kinh doanh</option>
            </select>
          </div>
        </div>

        <div class="modal-card-footer">
          <button type="button" class="btn-modal-cancel" @click="closeAddModal">Hủy bỏ</button>
          <button type="button" class="btn-modal-confirm" @click="promptSaveNew">Xác nhận lưu</button>
        </div>
      </div>
    </div>

    <!-- 5. MODAL CẬP NHẬT -->
    <div v-if="showEditModal" class="modal-backdrop-overlay" @click.self="closeEditModal">
      <div class="modal-card-dialog">
        <div class="modal-card-header">
          <h4 class="modal-card-title">✏️ CẬP NHẬT {{ title.toUpperCase() }}</h4>
          <button class="modal-close-cross" @click="closeEditModal">✕</button>
        </div>

        <div class="modal-card-body">
          <!-- Mã thuộc tính (readonly) -->
          <div class="form-field-group">
            <label class="form-field-label">Mã {{ title.toLowerCase() }} <span class="required-star">*</span></label>
            <input
                type="text"
                v-model="formData.ma"
                class="form-field-input input-readonly"
                readonly
                disabled
            />
          </div>

          <!-- Tên thuộc tính -->
          <div class="form-field-group">
            <label class="form-field-label">Tên {{ title.toLowerCase() }} <span class="required-star">*</span></label>
            <input
                type="text"
                v-model="formData.ten"
                class="form-field-input"
                :placeholder="`Nhập tên ${title.toLowerCase()}`"
            />
            <span class="field-error-msg" v-if="formErrors.ten">{{ formErrors.ten }}</span>
          </div>

          <!-- Mã màu (nếu là Màu sắc) -->
          <div v-if="hasColor" class="form-field-group">
            <label class="form-field-label">Mã màu (Hex) <span class="required-star">*</span></label>
            <div class="color-picker-input-group">
              <input type="color" v-model="formData.maHex" class="color-picker-box" />
              <input
                  type="text"
                  v-model="formData.maHex"
                  class="form-field-input"
                  placeholder="#1e293b"
              />
            </div>
            <span class="field-error-msg" v-if="formErrors.maHex">{{ formErrors.maHex }}</span>
          </div>

          <!-- Trạng thái hoạt động -->
          <div class="form-field-group">
            <label class="form-field-label">Trạng thái hoạt động</label>
            <select v-model.number="formData.trangThai" class="form-field-input form-field-select">
              <option :value="1">Kinh doanh</option>
              <option :value="0">Ngừng kinh doanh</option>
            </select>
          </div>
        </div>

        <div class="modal-card-footer">
          <button type="button" class="btn-modal-cancel" @click="closeEditModal">Hủy bỏ</button>
          <button type="button" class="btn-modal-confirm" @click="promptSaveUpdate">Xác nhận lưu</button>
        </div>
      </div>
    </div>

    <!-- 6. DIALOG XÁC NHẬN CHUNG (THÊM, CẬP NHẬT, XÓA) -->
    <div v-if="confirmDialog.show" class="confirm-backdrop-overlay" @click.self="closeConfirm">
      <div class="confirm-dialog-box">
        <div class="confirm-header">
          <span :class="['confirm-icon-badge', `icon-${confirmDialog.type}`]">
            <svg v-if="confirmDialog.type === 'danger'" viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
          </span>
          <h4 class="confirm-title-text">{{ confirmDialog.title }}</h4>
        </div>

        <div class="confirm-message-text">
          {{ confirmDialog.message }}
        </div>

        <div class="confirm-footer-btns">
          <button type="button" class="btn-confirm-cancel" @click="closeConfirm">Hủy</button>
          <button
              type="button"
              :class="['btn-confirm-submit', `btn-type-${confirmDialog.type}`]"
              @click="executeConfirm"
          >
            {{ confirmDialog.confirmText }}
          </button>
        </div>
      </div>
    </div>

    <!-- 7. TOAST THÔNG BÁO -->
    <div v-if="toast.show" :class="['global-toast-notification', `toast-${toast.type}`]">
      <div class="toast-title-bold">{{ toast.title }}</div>
      <div class="toast-desc-text" v-if="toast.message">{{ toast.message }}</div>
    </div>
  </div>
</template>

<style scoped>
.attribute-management-page {
  padding: 1.25rem 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
}

/* Header & Breadcrumb */
.page-header-row {
  margin-bottom: 1.25rem;
}

.page-title {
  font-size: 1.45rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.35rem 0;
}

.breadcrumb-trail {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.85rem;
  color: #64748b;
}

.bc-link {
  color: #64748b;
  text-decoration: none;
  transition: color 0.15s;
}

.bc-link:hover {
  color: #2563eb;
}

.bc-sep {
  color: #cbd5e1;
}

.bc-current {
  color: #334155;
  font-weight: 600;
}

/* Filter Card */
.filter-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
  margin-bottom: 1.5rem;
  padding: 1.25rem 1.5rem;
}

.filter-card-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  color: #334155;
}

.filter-icon {
  color: #d97706;
  display: flex;
  align-items: center;
}

.filter-card-title {
  font-size: 0.98rem;
  font-weight: 700;
  margin: 0;
  color: #1e293b;
}

.filter-inputs-row {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.filter-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #64748b;
}

.search-item {
  flex: 1;
  min-width: 260px;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon-slot {
  position: absolute;
  left: 0.75rem;
  color: #94a3b8;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.filter-input {
  width: 100%;
  height: 38px;
  padding: 0 2rem 0 2.25rem;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  font-size: 0.88rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s;
  background-color: #ffffff;
}

.filter-input:focus {
  border-color: #64748b;
  box-shadow: 0 0 0 3px rgba(100, 116, 139, 0.1);
}

.btn-clear-search {
  position: absolute;
  right: 0.75rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 0.8rem;
  padding: 2px;
}

.btn-clear-search:hover {
  color: #ef4444;
}

.status-item {
  width: 180px;
}

.filter-select {
  height: 38px;
  width: 100%;
  padding: 0 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  font-size: 0.88rem;
  color: #1e293b;
  outline: none;
  background-color: #ffffff;
  cursor: pointer;
}

.filter-select:focus {
  border-color: #64748b;
}

.filter-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-left: auto;
}

.btn-filter-action {
  height: 38px;
  padding: 0 1.15rem;
  border-radius: 20px;
  font-size: 0.86rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-reset {
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
}

.btn-reset:hover {
  background-color: #f8fafc;
  border-color: #94a3b8;
}

.btn-add-new {
  background-color: #7c4a3e;
  color: #ffffff;
}

.btn-add-new:hover {
  background-color: #663b31;
}

.btn-export-excel {
  background-color: #558b68;
  color: #ffffff;
}

.btn-export-excel:hover {
  background-color: #457255;
}

/* Data Table Card */
.data-table-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
  overflow: hidden;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.styled-attr-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
  text-align: left;
}

.styled-attr-table thead tr {
  background-color: #e5dcd6;
}

.styled-attr-table th {
  padding: 0.85rem 1rem;
  font-weight: 700;
  color: #4a3b32;
  font-size: 0.82rem;
  letter-spacing: 0.02em;
  border-bottom: 1px solid #d5c8be;
}

.styled-attr-table td {
  padding: 0.95rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.data-row:hover {
  background-color: #fdfaf8;
}

.cell-stt {
  color: #64748b;
  font-weight: 600;
}

.cell-code {
  color: #1e293b;
  font-weight: 600;
  font-size: 0.9rem;
}

.cell-name {
  font-weight: 500;
  color: #1e293b;
}

.color-indicator-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.color-preview-circle {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  display: inline-block;
}

.color-hex-text {
  font-size: 0.82rem;
  font-family: monospace;
  color: #64748b;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.85rem;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 600;
}

.badge-active {
  background-color: #15803d;
  color: #ffffff;
}

.badge-inactive {
  background-color: #dc2626;
  color: #ffffff;
}

.action-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
}

.btn-row-action {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-view-edit {
  color: #3b82f6;
}

.btn-view-edit:hover {
  background-color: #eff6ff;
  color: #1d4ed8;
}

.btn-delete-item {
  color: #ef4444;
}

.btn-delete-item:hover {
  background-color: #fef2f2;
  color: #b91c1c;
}

.empty-state-cell {
  text-align: center;
  padding: 2.5rem 1rem !important;
  color: #64748b;
}

.loading-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: #475569;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #cbd5e1;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Pagination Footer */
.table-pagination-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1.25rem;
  background-color: #ffffff;
  border-top: 1px solid #f1f5f9;
  font-size: 0.85rem;
  color: #64748b;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.page-btn {
  padding: 0.35rem 0.75rem;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  border-radius: 6px;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  background-color: #f8fafc;
  border-color: #94a3b8;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  font-weight: 600;
  color: #334155;
}

/* Modal Dialogs */
.modal-backdrop-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  backdrop-filter: blur(2px);
  padding: 1rem;
}

.modal-card-dialog {
  background: #ffffff;
  width: 100%;
  max-width: 480px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: modalScale 0.2s ease-out;
}

@keyframes modalScale {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.modal-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  background-color: #fbf7f4;
  border-bottom: 1px solid #f1e9e3;
}

.modal-card-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: #4a3b32;
}

.modal-close-cross {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  color: #8c7c72;
  cursor: pointer;
  line-height: 1;
}

.modal-close-cross:hover {
  color: #ef4444;
}

.modal-card-body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-field-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #334155;
}

.required-star {
  color: #ef4444;
}

.form-field-input {
  width: 100%;
  height: 38px;
  padding: 0 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.88rem;
  color: #1e293b;
  outline: none;
  transition: border-color 0.15s;
}

.form-field-input:focus {
  border-color: #7c4a3e;
}

.input-readonly {
  background-color: #f1f5f9;
  color: #64748b;
  cursor: not-allowed;
}

.color-picker-input-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.color-picker-box {
  width: 42px;
  height: 38px;
  padding: 2px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  cursor: pointer;
}

.field-error-msg {
  font-size: 0.78rem;
  color: #ef4444;
}

.modal-card-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.85rem;
  padding: 1rem 1.25rem 1.25rem;
  background-color: #ffffff;
}

.btn-modal-cancel {
  padding: 0.5rem 1.25rem;
  border-radius: 20px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-modal-cancel:hover {
  background-color: #f8fafc;
}

.btn-modal-confirm {
  padding: 0.5rem 1.35rem;
  border-radius: 20px;
  border: none;
  background-color: #8c5e4e;
  color: #ffffff;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-modal-confirm:hover {
  background-color: #734839;
}

/* Confirm Dialog */
.confirm-backdrop-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  backdrop-filter: blur(2px);
  padding: 1rem;
}

.confirm-dialog-box {
  background: #ffffff;
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.18);
  padding: 1.5rem;
  text-align: center;
  animation: modalScale 0.2s ease-out;
}

.confirm-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.75rem;
}

.confirm-icon-badge {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-primary {
  background-color: #eff6ff;
  color: #2563eb;
}

.icon-danger {
  background-color: #fef2f2;
  color: #ef4444;
}

.confirm-title-text {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #1e293b;
}

.confirm-message-text {
  font-size: 0.88rem;
  color: #475569;
  line-height: 1.5;
  margin-bottom: 1.25rem;
}

.confirm-footer-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.btn-confirm-cancel {
  padding: 0.45rem 1.15rem;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm-cancel:hover {
  background-color: #f8fafc;
}

.btn-confirm-submit {
  padding: 0.45rem 1.25rem;
  border-radius: 6px;
  border: none;
  font-weight: 600;
  font-size: 0.85rem;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-type-primary {
  background-color: #2563eb;
}

.btn-type-primary:hover {
  background-color: #1d4ed8;
}

.btn-type-danger {
  background-color: #ef4444;
}

.btn-type-danger:hover {
  background-color: #dc2626;
}

/* Toast */
.global-toast-notification {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  padding: 0.85rem 1.25rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1200;
  color: #ffffff;
  max-width: 350px;
  animation: slideInRight 0.3s ease-out;
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

.toast-success {
  background-color: #10b981;
}

.toast-error {
  background-color: #ef4444;
}

.toast-warning {
  background-color: #f59e0b;
}

.toast-title-bold {
  font-weight: 700;
  font-size: 0.9rem;
}

.toast-desc-text {
  font-size: 0.82rem;
  margin-top: 0.2rem;
  opacity: 0.95;
}
</style>
