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
  message: '',
  type: 'success'
})
let toastTimer = null
const showToast = (message = '', type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
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
    showToast(`Không thể tải danh sách ${props.title}`, 'error')
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

const visiblePages = computed(() => {
  const current = currentPage.value
  const total = totalPages.value
  const pages = []
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, current + 2)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
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
        showToast(`Đã thêm mới ${props.title.toLowerCase()} thành công!`, 'success')
        closeAddModal()
        await fetchItems()
      } catch (err) {
        console.error('Lỗi thêm thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể thêm mới!'
        showToast(msg, 'error')
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
        showToast(`Cập nhật ${props.title.toLowerCase()} thành công!`, 'success')
        closeEditModal()
        await fetchItems()
      } catch (err) {
        console.error('Lỗi cập nhật thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể cập nhật!'
        showToast(msg, 'error')
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
        showToast(`Đã xóa ${props.title.toLowerCase()} "${item.ten}"!`, 'success')
        await fetchItems()
      } catch (err) {
        console.error('Lỗi xóa thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể xóa vì đang có sản phẩm sử dụng!'
        showToast(msg, 'error')
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
      showToast('Không có dữ liệu để xuất Excel!', 'error')
      return
    }

    const worksheet = XLSX.utils.json_to_sheet(dataToExport)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, props.title)
    const fileName = `Danh_Sach_${props.title.replace(/\s+/g, '_')}_${new Date().toISOString().slice(0, 10)}.xlsx`
    XLSX.writeFile(workbook, fileName)
    showToast(`Đã xuất ${dataToExport.length} dòng ra file Excel!`, 'success')
  } catch (err) {
    console.error('Lỗi xuất Excel:', err)
    showToast('Không thể xuất file Excel!', 'error')
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
  <div class="product-page-container">
    <!-- Tiêu đề trang -->
    <div class="page-header-row">
      <h2 class="main-page-title">QUẢN LÝ {{ title.toUpperCase() }}</h2>
      <div v-if="toast.show" :class="['toast-notification', toast.type]">
        {{ toast.message }}
      </div>
    </div>

    <!-- Khối khung trắng bao bọc toàn bộ chức năng (style đồng bộ trang sản phẩm) -->
    <div class="product-card">
      <!-- 1. Hàng tìm kiếm, bộ lọc trạng thái và các nút chính -->
      <div class="search-actions-bar">
        <!-- Cột trái: Tìm kiếm -->
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
                v-model="searchKeyword"
                :placeholder="`Tìm theo mã / tên ${title.toLowerCase()}...`"
                class="input-search-full"
            />
          </div>
        </div>

        <!-- Cột giữa: Bộ lọc trạng thái -->
        <div class="filter-select-col">
          <label class="search-label">Trạng thái</label>
          <select v-model="filterStatus" class="filter-select-control">
            <option value="">Tất cả trạng thái</option>
            <option value="1">Kinh doanh</option>
            <option value="0">Ngừng kinh doanh</option>
          </select>
        </div>

        <!-- Cột phải: 3 nút hành động (Đặt lại, Xuất Excel, + Thêm mới) -->
        <div class="action-buttons-col">
          <button class="btn btn-refresh" @click="resetFilters" title="Đặt lại bộ lọc">
            <span class="btn-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 4 23 10 17 10"></polyline>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
            </span>
            Đặt lại
          </button>

          <button class="btn btn-export" @click="exportExcel" title="Xuất danh sách ra file Excel">
            <span class="btn-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
            </span>
            Xuất Excel
          </button>

          <button class="btn btn-add" @click="openAddModal" :title="`Thêm mới ${title.toLowerCase()}`">
            <span class="btn-icon">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
            </span>
            Thêm mới
          </button>
        </div>
      </div>

      <!-- 3. Bảng dữ liệu thuộc tính -->
      <div class="table-responsive">
        <table class="product-table">
          <thead>
          <tr>
            <th style="width: 60px; text-align: center;">STT</th>
            <th style="width: 180px;">Mã {{ title }}</th>
            <th>Tên {{ title }}</th>
            <th v-if="hasColor" style="width: 140px; text-align: center;">Mã màu</th>
            <th style="width: 160px; text-align: center;">Trạng thái</th>
            <th style="width: 140px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td :colspan="hasColor ? 6 : 5" class="empty-state">
              <div class="empty-box">
                <div class="loading-spinner"></div>
                <span class="empty-text">Đang tải dữ liệu {{ title.toLowerCase() }}...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="filteredItems.length === 0">
            <td :colspan="hasColor ? 6 : 5" class="empty-state">
              <div class="empty-box">
                <span class="empty-icon">
                  <svg viewBox="0 0 24 24" width="38" height="38" fill="currentColor" opacity="0.45">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v3c0 .55.45 1 1 1h1v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8h1c.55 0 1-.45 1-1V4c0-1.1-.9-2-2-2zm-1 6v12H5V8h14zM4 4h16v2H4V4zm5 6h6v2H9v-2z"/>
                  </svg>
                </span>
                <span class="empty-text">Không tìm thấy {{ title.toLowerCase() }} nào phù hợp!</span>
                <button type="button" class="btn btn-refresh-sm" @click="resetFilters">Làm mới bộ lọc</button>
              </div>
            </td>
          </tr>
          <tr v-else v-for="(item, index) in paginatedItems" :key="item.id">
            <!-- STT -->
            <td style="text-align: center;" class="stt-cell">
              {{ currentPage * pageSize + index + 1 }}
            </td>

            <!-- Mã thuộc tính -->
            <td class="code-cell">
              <b>{{ item.ma || '—' }}</b>
            </td>

            <!-- Tên thuộc tính -->
            <td class="product-name-cell">
              <div class="name-text">{{ item.ten || '—' }}</div>
            </td>

            <!-- Mã màu (nếu có) -->
            <td v-if="hasColor" style="text-align: center;">
              <div class="color-badge-wrap">
                <span class="color-dot" :style="{ backgroundColor: item.maHex || '#888' }"></span>
                <span class="color-code">{{ item.maHex || '—' }}</span>
              </div>
            </td>

            <!-- Trạng thái -->
            <td style="text-align: center;">
              <span :class="['status-badge', item.trangThai === 1 ? 'status-active' : 'status-inactive']">
                {{ item.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
              </span>
            </td>

            <!-- Cột hành động: Icon mắt (Cập nhật) & Icon thùng rác (Xóa) -->
            <td style="text-align: center;">
              <div class="action-buttons-group">
                <!-- Icon mắt (Xem / Cập nhật) -->
                <button
                    class="btn-action btn-view"
                    @click="openEditModal(item)"
                    :title="`Cập nhật ${title.toLowerCase()}`"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                </button>

                <!-- Icon xóa (Thùng rác) -->
                <button
                    class="btn-action btn-delete"
                    @click="confirmDeleteItem(item)"
                    :title="`Xóa ${title.toLowerCase()}`"
                >
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    <line x1="10" y1="11" x2="10" y2="17"></line>
                    <line x1="14" y1="11" x2="14" y2="17"></line>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. Thanh phân trang tương tự trang sản phẩm -->
      <div class="pagination-footer" v-if="totalPages > 0">
        <div class="pagination-left">
          <div class="page-size-selector">
            <span class="page-size-label">Hiển thị:</span>
            <select v-model.number="pageSize" @change="currentPage = 0" class="page-size-select">
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
              :disabled="currentPage === 0"
              @click="changePage(0)"
              title="Trang đầu"
          >«</button>
          <button
              class="pg-box"
              :disabled="currentPage === 0"
              @click="changePage(currentPage - 1)"
              title="Trang trước"
          >‹</button>

          <button
              v-for="p in visiblePages"
              :key="p"
              :class="['pg-box', { active: currentPage === p }]"
              @click="changePage(p)"
          >
            {{ p + 1 }}
          </button>

          <button
              class="pg-box"
              :disabled="currentPage >= totalPages - 1"
              @click="changePage(currentPage + 1)"
              title="Trang sau"
          >›</button>
          <button
              class="pg-box"
              :disabled="currentPage >= totalPages - 1"
              @click="changePage(totalPages - 1)"
              title="Trang cuối"
          >»</button>
        </div>
      </div>
    </div>

    <!-- MODAL THÊM MỚI -->
    <div v-if="showAddModal" class="modal-backdrop" @click.self="closeAddModal">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 class="modal-title">Thêm Mới {{ title }}</h3>
          <button type="button" class="btn-close-modal" @click="closeAddModal">✕</button>
        </div>

        <div class="modal-form">
          <div class="form-grid">
            <div class="form-group full-width">
              <label class="form-label">Mã {{ title }} <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="formData.ma"
                  class="form-control"
                  placeholder="Hệ thống tự sinh mã..."
              />
            </div>

            <div class="form-group full-width">
              <label class="form-label">Tên {{ title }} <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="formData.ten"
                  class="form-control"
                  :placeholder="`Nhập tên ${title.toLowerCase()}...`"
              />
              <span class="field-error-msg" v-if="formErrors.ten">{{ formErrors.ten }}</span>
            </div>

            <div v-if="hasColor" class="form-group full-width">
              <label class="form-label">Mã màu (Hex) <span class="required">*</span></label>
              <div class="color-picker-input-wrap">
                <input type="color" v-model="formData.maHex" class="color-picker-box" />
                <input
                    type="text"
                    v-model="formData.maHex"
                    class="form-control"
                    placeholder="#1e293b"
                />
              </div>
              <span class="field-error-msg" v-if="formErrors.maHex">{{ formErrors.maHex }}</span>
            </div>

            <div class="form-group full-width">
              <label class="form-label">Trạng thái hoạt động</label>
              <select v-model.number="formData.trangThai" class="form-control">
                <option :value="1">Kinh doanh</option>
                <option :value="0">Ngừng kinh doanh</option>
              </select>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeAddModal">Hủy bỏ</button>
            <button type="button" class="btn btn-primary" @click="promptSaveNew">Xác nhận lưu</button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL CẬP NHẬT (KHI BẤM ICON MẮT) -->
    <div v-if="showEditModal" class="modal-backdrop" @click.self="closeEditModal">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 class="modal-title">Cập Nhật {{ title }}</h3>
          <button type="button" class="btn-close-modal" @click="closeEditModal">✕</button>
        </div>

        <div class="modal-form">
          <div class="form-grid">
            <div class="form-group full-width">
              <label class="form-label">Mã {{ title }}</label>
              <input
                  type="text"
                  v-model="formData.ma"
                  class="form-control"
                  disabled
              />
            </div>

            <div class="form-group full-width">
              <label class="form-label">Tên {{ title }} <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="formData.ten"
                  class="form-control"
                  :placeholder="`Nhập tên ${title.toLowerCase()}...`"
              />
              <span class="field-error-msg" v-if="formErrors.ten">{{ formErrors.ten }}</span>
            </div>

            <div v-if="hasColor" class="form-group full-width">
              <label class="form-label">Mã màu (Hex) <span class="required">*</span></label>
              <div class="color-picker-input-wrap">
                <input type="color" v-model="formData.maHex" class="color-picker-box" />
                <input
                    type="text"
                    v-model="formData.maHex"
                    class="form-control"
                    placeholder="#1e293b"
                />
              </div>
              <span class="field-error-msg" v-if="formErrors.maHex">{{ formErrors.maHex }}</span>
            </div>

            <div class="form-group full-width">
              <label class="form-label">Trạng thái hoạt động</label>
              <select v-model.number="formData.trangThai" class="form-control">
                <option :value="1">Kinh doanh</option>
                <option :value="0">Ngừng kinh doanh</option>
              </select>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeEditModal">Hủy bỏ</button>
            <button type="button" class="btn btn-primary" @click="promptSaveUpdate">Xác nhận cập nhật</button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL XÁC NHẬN HÀNH ĐỘNG (XÓA / LƯU) -->
    <transition name="fade">
      <div v-if="confirmDialog.show" class="confirm-modal-overlay" @click="closeConfirm">
        <div class="confirm-modal-box" @click.stop>
          <div class="confirm-icon-wrap" :class="confirmDialog.type">
            <span v-if="confirmDialog.type === 'danger'">⚠️</span>
            <span v-else-if="confirmDialog.type === 'info'">ℹ️</span>
            <span v-else>❓</span>
          </div>

          <h3 class="confirm-title">{{ confirmDialog.title }}</h3>
          <p class="confirm-message">{{ confirmDialog.message }}</p>

          <div class="confirm-actions">
            <button type="button" class="btn btn-confirm-cancel" @click="closeConfirm">
              Hủy bỏ
            </button>
            <button
                type="button"
                :class="['btn', confirmDialog.type === 'danger' ? 'btn-confirm-danger' : 'btn-confirm-primary']"
                @click="executeConfirm"
            >
              {{ confirmDialog.confirmText || 'Đồng ý' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* Vùng hiển thị trang đồng bộ trang sản phẩm */
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

/* Khối Card trắng */
.product-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.75rem;
}

/* 1. Hàng tìm kiếm và các nút chính */
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
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-icon {
  font-size: 0.9rem;
}

/* Nút Làm mới */
.btn-refresh {
  background-color: #d2a764;
  color: #ffffff;
}
.btn-refresh:hover {
  background-color: #be9453;
}

/* Nút Xuất Excel */
.btn-export {
  background-color: #2e7d32;
  color: #ffffff;
}
.btn-export:hover {
  background-color: #256628;
}

/* Nút Thêm mới */
.btn-add {
  background-color: #edd9b8;
  color: #4a3e2e;
}
.btn-add:hover {
  background-color: #e4cda7;
}

/* 2. Cột lọc trạng thái cùng hàng */
.filter-select-col {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  width: 200px;
  flex-shrink: 0;
}

.filter-select-control {
  width: 100%;
  height: 2.5rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
  color: #4f5a60;
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-select-control:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

/* 3. Bảng dữ liệu */
.table-responsive {
  overflow-x: auto;
}

.product-table {
  width: 100%;
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

.stt-cell {
  color: #64748b;
  font-weight: 600;
}

.code-cell {
  color: #1e293b;
  font-size: 0.92rem;
}

.name-text {
  font-weight: 600;
  color: #1e293b;
}

.color-badge-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.color-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  display: inline-block;
}

.color-code {
  font-size: 0.85rem;
  font-family: monospace;
  color: #64748b;
}

.status-badge {
  display: inline-block;
  padding: 0.35rem 0.8rem;
  font-weight: 700;
  border-radius: 6px;
  font-size: 0.82rem;
  white-space: nowrap;
}

.status-badge.status-active {
  background-color: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.status-badge.status-inactive {
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
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
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
  padding: 0;
}

.btn-action svg {
  display: block;
}

.btn-view {
  color: #0284c7;
}
.btn-view:hover {
  background-color: #e0f2fe;
  border-color: #0284c7;
}

.btn-delete {
  color: #dc2626;
}
.btn-delete:hover {
  background-color: #fef2f2;
  border-color: #dc2626;
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

.loading-spinner {
  width: 22px;
  height: 22px;
  border: 3px solid #e2e8f0;
  border-top-color: var(--blue, #496883);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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
  transition: background-color 0.2s;
}

/* 4. Thanh phân trang tương tự trang sản phẩm */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f4f8;
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
  transition: border-color 0.2s;
}

.page-size-select:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.page-size-unit {
  font-size: 0.85rem;
  color: #64748b;
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
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

/* Modal Form Thêm / Sửa */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.modal-dialog {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 540px;
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
  grid-template-columns: 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
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
  transition: border-color 0.2s;
  background-color: #ffffff;
  color: #1e293b;
}

.form-control:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.1);
}

.form-control:disabled {
  background-color: #f8fafc;
  color: #64748b;
  cursor: not-allowed;
}

.color-picker-input-wrap {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.color-picker-box {
  width: 44px;
  height: 2.45rem;
  border: 1px solid #d5d9df;
  border-radius: 6px;
  cursor: pointer;
  padding: 2px;
  background-color: #ffffff;
}

.field-error-msg {
  font-size: 0.8rem;
  color: #dc2626;
  font-weight: 500;
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
  transition: background-color 0.2s;
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
  transition: background-color 0.2s;
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
  transition: background-color 0.2s;
}

.btn-confirm-danger:hover {
  background: #b91c1c;
}

@media (max-width: 992px) {
  .search-actions-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 0.85rem;
  }
  .filter-select-col {
    width: 100%;
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
}
</style>
