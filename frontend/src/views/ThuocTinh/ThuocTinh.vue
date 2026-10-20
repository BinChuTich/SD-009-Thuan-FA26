<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const route = useRoute()
const router = useRouter()

// 9 loại thuộc tính hỗ trợ
const attributeTypes = [
  { id: 'chat-lieu', name: 'Chất liệu', codePrefix: 'CL', icon: '🏷️' },
  { id: 'mau-sac', name: 'Màu sắc', codePrefix: 'MS', icon: '🎨', hasColor: true },
  { id: 'kich-co', name: 'Kích cỡ', codePrefix: 'KC', icon: '📏' },
  { id: 'co-ao', name: 'Cổ áo', codePrefix: 'CA', icon: '👔' },
  { id: 'tay-ao', name: 'Tay áo', codePrefix: 'TA', icon: '👕' },
  { id: 'danh-muc', name: 'Danh mục', codePrefix: 'DM', icon: '🗂️' },
  { id: 'thuong-hieu', name: 'Thương hiệu', codePrefix: 'TH', icon: '⭐' },
  { id: 'xuat-xu', name: 'Xuất xứ', codePrefix: 'XX', icon: '🌍' },
  { id: 'hoa-tiet', name: 'Họa tiết', codePrefix: 'HT', icon: '✨' }
]

// Tab đang chọn
const activeTab = ref('chat-lieu')

// Dữ liệu danh sách của tab hiện tại
const items = ref([])
const loading = ref(false)
const counts = ref({})

// Tìm kiếm & Bộ lọc
const searchKeyword = ref('')
const filterStatus = ref('')
const currentPage = ref(0)
const pageSize = ref(10)

const filterStatusOptions = [
  { id: '', name: 'Tất cả trạng thái' },
  { id: '1', name: 'Đang sử dụng' },
  { id: '0', name: 'Ngừng sử dụng' }
]

const formStatusOptions = [
  { id: 1, name: 'Đang sử dụng' },
  { id: 0, name: 'Ngừng sử dụng' }
]

// Modal Thêm / Sửa
const modal = ref({
  show: false,
  isEdit: false,
  id: null,
  data: {
    ma: '',
    ten: '',
    maHex: '#496883',
    trangThai: 1
  },
  errors: {}
})

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

// Dialog xác nhận
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

// Lấy thông tin cấu hình của Tab hiện tại
const currentTabInfo = computed(() => {
  return attributeTypes.find(t => t.id === activeTab.value) || attributeTypes[0]
})

// Lấy số lượng của tất cả thuộc tính
const fetchAllCounts = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      counts.value = {
        'chat-lieu': res.data.chatLieu?.length || 0,
        'thuong-hieu': res.data.thuongHieu?.length || 0,
        'xuat-xu': res.data.xuatXu?.length || 0,
        'danh-muc': res.data.danhMuc?.length || 0,
        'co-ao': res.data.coAo?.length || 0,
        'tay-ao': res.data.tayAo?.length || 0,
        'hoa-tiet': res.data.hoaTiet?.length || 0,
        'mau-sac': res.data.mauSac?.length || 0,
        'kich-co': res.data.kichCo?.length || 0
      }
    }
  } catch (err) {
    console.error('Lỗi lấy thống kê số lượng thuộc tính:', err)
  }
}

// Lấy danh sách thuộc tính theo tab
const fetchTabItems = async () => {
  loading.value = true
  try {
    const res = await api.get(`/api/thuoc-tinh/type/${activeTab.value}`)
    if (Array.isArray(res.data)) {
      items.value = res.data.map(item => {
        // Chuẩn hóa trường mã, tên cho mọi entity
        const ma = item.maChatLieu || item.maMauSac || item.maKichCo || item.maCoAo ||
            item.maTayAo || item.maDanhMuc || item.maThuongHieu || item.maXuatXu || item.maHoaTiet || ''
        const ten = item.tenChatLieu || item.tenMauSac || item.tenKichCo || item.tenCoAo ||
            item.tenTayAo || item.tenDanhMuc || item.tenThuongHieu || item.tenXuatXu || item.tenHoaTiet || ''
        return {
          id: item.id,
          ma,
          ten,
          maHex: item.maHex || '#496883',
          trangThai: item.trangThai != null ? item.trangThai : 1,
          raw: item
        }
      })
      counts.value[activeTab.value] = items.value.length
    }
  } catch (err) {
    console.error(`Lỗi tải danh sách ${activeTab.value}:`, err)
    showToast('Lỗi tải dữ liệu', `Không thể tải danh sách ${currentTabInfo.value.name}`, 'error')
  } finally {
    loading.value = false
  }
}

// Chuyển tab
const switchTab = (tabId) => {
  activeTab.value = tabId
  searchKeyword.value = ''
  filterStatus.value = ''
  currentPage.value = 0
  router.replace({ path: '/thuoc-tinh', query: { tab: tabId } })
  fetchTabItems()
}

// Lọc dữ liệu hiển thị phía client
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

// Dữ liệu phân trang
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

// Thêm mới thuộc tính
const openAddModal = () => {
  const suffix = Math.floor(1000 + Math.random() * 9000)
  modal.value = {
    show: true,
    isEdit: false,
    id: null,
    data: {
      ma: `${currentTabInfo.value.codePrefix}${suffix}`,
      ten: '',
      maHex: '#496883',
      trangThai: 1
    },
    errors: {}
  }
}

// Chỉnh sửa thuộc tính
const openEditModal = (item) => {
  modal.value = {
    show: true,
    isEdit: true,
    id: item.id,
    data: {
      ma: item.ma,
      ten: item.ten,
      maHex: item.maHex || '#496883',
      trangThai: item.trangThai
    },
    errors: {}
  }
}

const closeModal = () => {
  modal.value.show = false
  modal.value.errors = {}
}

const saving = ref(false)

// Lưu thuộc tính (Thêm hoặc Cập nhật) với validate chặt chẽ
const handleSave = async () => {
  modal.value.errors = {}
  const rawTen = modal.value.data.ten ? modal.value.data.ten.trim() : ''
  const rawMa = modal.value.data.ma ? modal.value.data.ma.trim() : ''
  const typeName = currentTabInfo.value.name.toLowerCase()

  // 1. Validate Tên
  if (!rawTen) {
    modal.value.errors.ten = `Vui lòng nhập tên ${typeName}!`
  } else if (rawTen.length < 1) {
    modal.value.errors.ten = `Tên ${typeName} không được để trống!`
  } else if (rawTen.length > 255) {
    modal.value.errors.ten = `Tên ${typeName} không được vượt quá 255 ký tự!`
  } else {
    // Kiểm tra trùng tên phía client
    const isDupTen = items.value.some(it =>
        it.ten && it.ten.trim().toLowerCase() === rawTen.toLowerCase() &&
        (!modal.value.isEdit || it.id !== modal.value.id)
    )
    if (isDupTen) {
      modal.value.errors.ten = `Tên ${typeName} "${rawTen}" đã tồn tại trong danh sách!`
    }
  }

  // 2. Validate Mã (nếu người dùng nhập)
  if (rawMa) {
    if (rawMa.length > 50) {
      modal.value.errors.ma = 'Mã thuộc tính không được vượt quá 50 ký tự!'
    } else if (!/^[a-zA-Z0-9_.-]+$/.test(rawMa)) {
      modal.value.errors.ma = 'Mã thuộc tính chỉ được chứa chữ cái, chữ số và các ký tự _ - .'
    } else {
      // Kiểm tra trùng mã phía client
      const isDupMa = items.value.some(it =>
          it.ma && it.ma.trim().toLowerCase() === rawMa.toLowerCase() &&
          (!modal.value.isEdit || it.id !== modal.value.id)
      )
      if (isDupMa) {
        modal.value.errors.ma = `Mã "${rawMa}" đã tồn tại trong danh sách!`
      }
    }
  }

  // 3. Validate Mã HEX nếu là Màu sắc
  let finalHex = modal.value.data.maHex ? modal.value.data.maHex.trim() : '#496883'
  if (currentTabInfo.value.hasColor) {
    if (!/^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$/.test(finalHex)) {
      modal.value.errors.maHex = 'Mã màu HEX không hợp lệ (Ví dụ: #FF0000 hoặc #000)!'
    }
  }

  // Dừng lại nếu có lỗi
  if (Object.keys(modal.value.errors).length > 0) {
    return
  }

  const payload = {
    ma: rawMa,
    ten: rawTen,
    maHex: finalHex,
    trangThai: Number(modal.value.data.trangThai)
  }

  saving.value = true
  try {
    if (modal.value.isEdit) {
      await api.put(`/api/thuoc-tinh/type/${activeTab.value}/${modal.value.id}`, payload)
      showToast('Cập nhật thành công', `Đã cập nhật ${typeName} "${payload.ten}"`)
    } else {
      await api.post(`/api/thuoc-tinh/type/${activeTab.value}`, payload)
      showToast('Thêm mới thành công', `Đã thêm ${typeName} "${payload.ten}"`)
    }
    closeModal()
    await fetchTabItems()
    await fetchAllCounts()
  } catch (err) {
    console.error('Lỗi lưu thuộc tính:', err)
    const msg = err.response?.data?.message || err.message || 'Lỗi khi lưu thuộc tính!'
    if (msg.includes('Tên') || msg.includes('tên')) {
      modal.value.errors.ten = msg
    } else if (msg.includes('Mã') || msg.includes('mã')) {
      modal.value.errors.ma = msg
    } else if (msg.includes('màu') || msg.includes('HEX')) {
      modal.value.errors.maHex = msg
    }
    showToast('Lỗi thao tác', msg, 'error')
  } finally {
    saving.value = false
  }
}

// Đổi trạng thái (Bật / Tắt)
const toggleStatus = (item) => {
  const newStatus = item.trangThai === 1 ? 0 : 1
  const actionText = newStatus === 1 ? 'kích hoạt' : 'ngừng sử dụng'
  openConfirm({
    title: 'Đổi trạng thái thuộc tính',
    message: `Bạn có chắc chắn muốn ${actionText} ${currentTabInfo.value.name.toLowerCase()} "${item.ten}" không?`,
    type: 'info',
    confirmText: 'Xác nhận',
    onConfirm: async () => {
      try {
        await api.patch(`/api/thuoc-tinh/type/${activeTab.value}/${item.id}/status`, null, {
          params: { trangThai: newStatus }
        })
        item.trangThai = newStatus
        showToast('Đổi trạng thái', `Đã chuyển sang "${newStatus === 1 ? 'Đang sử dụng' : 'Ngừng sử dụng'}"`)
      } catch (err) {
        console.error('Lỗi đổi trạng thái:', err)
        showToast('Lỗi cập nhật', 'Không thể đổi trạng thái thuộc tính', 'error')
      }
    }
  })
}

// Xóa thuộc tính
const confirmDelete = (item) => {
  openConfirm({
    title: 'Xác nhận xóa',
    message: `Bạn có chắc chắn muốn xóa ${currentTabInfo.value.name.toLowerCase()} "${item.ten}"? Nếu đang được gán vào sản phẩm, hệ thống sẽ cảnh báo ngăn chặn.`,
    type: 'danger',
    confirmText: 'Xóa ngay',
    onConfirm: async () => {
      try {
        await api.delete(`/api/thuoc-tinh/type/${activeTab.value}/${item.id}`)
        showToast('Xóa thành công', `Đã xóa ${currentTabInfo.value.name.toLowerCase()} "${item.ten}"`)
        await fetchTabItems()
        await fetchAllCounts()
      } catch (err) {
        console.error('Lỗi xóa thuộc tính:', err)
        const msg = err.response?.data?.message || err.message || 'Không thể xóa thuộc tính này vì đang có sản phẩm sử dụng!'
        showToast('Không thể xóa', msg, 'error')
      }
    }
  })
}

// Khởi tạo
onMounted(async () => {
  const qTab = route.query.tab
  if (qTab && attributeTypes.some(t => t.id === qTab)) {
    activeTab.value = qTab
  }
  await fetchAllCounts()
  await fetchTabItems()
})

// Theo dõi query param khi thay đổi
watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== activeTab.value && attributeTypes.some(t => t.id === newTab)) {
    activeTab.value = newTab
    fetchTabItems()
  }
})
</script>

<template>
  <div class="thuoc-tinh-page">
    <!-- Header trang -->
    <div class="page-top-bar">
      <div class="title-cluster">
        <div class="page-breadcrumb">
          <router-link to="/thuong-hieu" class="crumb-parent">Quản lý thuộc tính</router-link>
          <span class="crumb-separator">/</span>
          <span class="crumb-current">Danh sách thuộc tính</span>
        </div>
      </div>

      <button class="btn-create-attr" @click="openAddModal">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span>Thêm {{ currentTabInfo.name.toLowerCase() }} mới</span>
      </button>
    </div>

    <!-- Main Card -->
    <div class="attr-card-container">
      <!-- 1. Thanh Tabs chuyển đổi 9 loại thuộc tính -->
      <div class="tabs-scroll-wrapper">
        <div class="attr-tabs-nav">
          <button
              v-for="t in attributeTypes"
              :key="t.id"
              :class="['attr-tab-btn', { active: activeTab === t.id }]"
              @click="switchTab(t.id)"
          >
            <span class="tab-emoji">{{ t.icon }}</span>
            <span class="tab-label">{{ t.name }}</span>
            <span class="tab-count-badge">{{ counts[t.id] ?? 0 }}</span>
          </button>
        </div>
      </div>

      <!-- 2. Thanh tìm kiếm và bộ lọc của Tab hiện tại -->
      <div class="table-toolbar">
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" class="search-icon">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
              type="text"
              v-model="searchKeyword"
              :placeholder="`Tìm kiếm ${currentTabInfo.name.toLowerCase()} theo mã, tên...`"
              class="search-input"
          />
          <button v-if="searchKeyword" class="clear-search-btn" @click="searchKeyword = ''">&times;</button>
        </div>

        <div class="toolbar-actions">
          <div style="min-width: 170px;">
            <Select2Combobox
              v-model="filterStatus"
              :options="filterStatusOptions"
              value-key="id"
              label-key="name"
              placeholder="Tất cả trạng thái"
              search-placeholder="Tìm trạng thái..."
              @change="currentPage = 0"
            />
          </div>

          <button class="btn-refresh" @click="fetchTabItems" title="Làm mới danh sách">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"></polyline>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            Làm mới
          </button>
        </div>
      </div>

      <!-- 3. Bảng dữ liệu thuộc tính -->
      <div class="table-responsive">
        <table class="data-table">
          <thead>
          <tr>
            <th style="width: 60px; text-align: center;">STT</th>
            <th style="width: 140px;">Mã {{ currentTabInfo.name.toLowerCase() }}</th>
            <th>Tên {{ currentTabInfo.name.toLowerCase() }}</th>
            <th v-if="currentTabInfo.hasColor" style="width: 170px;">Mã màu (HEX)</th>
            <th style="width: 160px; text-align: center;">Trạng thái</th>
            <th style="width: 140px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td :colspan="currentTabInfo.hasColor ? 6 : 5" class="table-empty-cell">
              <span class="spinner-dot"></span> Đang tải dữ liệu...
            </td>
          </tr>
          <tr v-else-if="paginatedItems.length === 0">
            <td :colspan="currentTabInfo.hasColor ? 6 : 5" class="table-empty-cell">
              Không tìm thấy {{ currentTabInfo.name.toLowerCase() }} nào phù hợp.
            </td>
          </tr>
          <tr v-else v-for="(item, index) in paginatedItems" :key="item.id">
            <td style="text-align: center; font-weight: 700; color: #496883;">
              {{ currentPage * pageSize + index + 1 }}
            </td>
            <td>
              <span class="attr-code-pill">{{ item.ma || '—' }}</span>
            </td>
            <td>
              <span class="attr-name-text">{{ item.ten }}</span>
            </td>
            <!-- Cột màu sắc đặc biệt nếu là Màu sắc -->
            <td v-if="currentTabInfo.hasColor">
              <div class="color-badge-preview">
                <span class="color-swatch-circle" :style="{ backgroundColor: item.maHex || '#ccc' }"></span>
                <span class="color-hex-val">{{ item.maHex || '—' }}</span>
              </div>
            </td>
            <!-- Trạng thái -->
            <td style="text-align: center;">
              <span :class="['status-chip', item.trangThai === 1 ? 'chip-active' : 'chip-inactive']">
                {{ item.trangThai === 1 ? 'Đang sử dụng' : 'Ngừng sử dụng' }}
              </span>
            </td>
            <!-- Hành động: Power, Edit, Delete -->
            <td style="text-align: center;">
              <div class="row-actions-group">
                <!-- 1. Bật/Tắt trạng thái -->
                <button
                    :class="['btn-circle-action', 'btn-power', { 'is-active': item.trangThai === 1 }]"
                    @click="toggleStatus(item)"
                    :title="item.trangThai === 1 ? 'Chuyển sang Ngừng sử dụng' : 'Kích hoạt sử dụng'"
                >
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                    <line x1="12" y1="2" x2="12" y2="12"></line>
                  </svg>
                </button>

                <!-- 2. Sửa -->
                <button
                    class="btn-circle-action btn-edit"
                    @click="openEditModal(item)"
                    :title="`Chỉnh sửa ${currentTabInfo.name.toLowerCase()}`"
                >
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>

                <!-- 3. Xóa -->
                <button
                    class="btn-circle-action btn-delete"
                    @click="confirmDelete(item)"
                    :title="`Xóa ${currentTabInfo.name.toLowerCase()}`"
                >
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

      <!-- 4. Footer phân trang -->
      <div class="pagination-bar" v-if="filteredItems.length > 0">
        <div class="pagination-info">
          Hiển thị {{ paginatedItems.length ? (currentPage * pageSize + 1) : 0 }} - {{ currentPage * pageSize + paginatedItems.length }} / {{ filteredItems.length }} {{ currentTabInfo.name.toLowerCase() }}
        </div>

        <div class="pagination-pages" v-if="totalPages > 1">
          <button
              class="page-btn"
              :disabled="currentPage === 0"
              @click="changePage(currentPage - 1)"
          >‹</button>

          <button
              v-for="p in totalPages"
              :key="p"
              :class="['page-btn', { active: currentPage === p - 1 }]"
              @click="changePage(p - 1)"
          >
            {{ p }}
          </button>

          <button
              class="page-btn"
              :disabled="currentPage >= totalPages - 1"
              @click="changePage(currentPage + 1)"
          >›</button>
        </div>
      </div>
    </div>

    <!-- Modal Thêm / Chỉnh Sửa Thuộc Tính -->
    <div class="modal-backdrop" v-if="modal.show" @click.self="closeModal">
      <div class="modal-box">
        <div class="modal-header">
          <div class="modal-header-title">
            <span class="modal-tag">{{ currentTabInfo.icon }} {{ currentTabInfo.name }}</span>
            <h3>{{ modal.isEdit ? `Cập nhật ${currentTabInfo.name.toLowerCase()}` : `Thêm mới ${currentTabInfo.name.toLowerCase()}` }}</h3>
          </div>
          <button class="modal-close" @click="closeModal">&times;</button>
        </div>

        <div class="modal-body">
          <div class="form-group" v-if="modal.isEdit">
            <label class="form-label">MÃ {{ currentTabInfo.name.toUpperCase() }} <span class="note">(Tùy chọn)</span></label>
            <input
                type="text"
                v-model="modal.data.ma"
                @input="delete modal.errors.ma"
                class="form-input"
                :placeholder="`Mã ${currentTabInfo.name.toLowerCase()} (VD: ${currentTabInfo.codePrefix}01)...`"
                maxlength="50"
            />
            <span class="error-msg" v-if="modal.errors.ma">{{ modal.errors.ma }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">TÊN {{ currentTabInfo.name.toUpperCase() }} <span class="req">*</span></label>
            <input
                type="text"
                v-model="modal.data.ten"
                @input="delete modal.errors.ten"
                class="form-input"
                :placeholder="`Nhập tên ${currentTabInfo.name.toLowerCase()}...`"
                @keyup.enter="handleSave"
                maxlength="255"
                autofocus
            />
            <span class="error-msg" v-if="modal.errors.ten">{{ modal.errors.ten }}</span>
          </div>

          <!-- Nhập mã màu nếu là Màu Sắc -->
          <div class="form-group" v-if="currentTabInfo.hasColor">
            <label class="form-label">CHỌN MÃ MÀU HEX <span class="req">*</span></label>
            <div class="color-picker-row">
              <input type="color" v-model="modal.data.maHex" @input="delete modal.errors.maHex" class="color-picker-input" />
              <input type="text" v-model="modal.data.maHex" @input="delete modal.errors.maHex" class="form-input color-hex-input" placeholder="#000000" maxlength="7" />
              <span class="color-preview-block" :style="{ backgroundColor: modal.data.maHex }"></span>
            </div>
            <span class="error-msg" v-if="modal.errors.maHex">{{ modal.errors.maHex }}</span>
          </div>

          <div class="form-group" v-if="modal.isEdit">
            <label class="form-label">TRẠNG THÁI</label>
            <Select2Combobox
              v-model="modal.data.trangThai"
              :options="formStatusOptions"
              value-key="id"
              label-key="name"
              :show-all-option="false"
              :allow-clear="false"
              placeholder="-- Chọn trạng thái --"
              search-placeholder="Tìm trạng thái..."
            />
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModal" :disabled="saving">Hủy bỏ</button>
          <button class="btn-submit" @click="handleSave" :disabled="saving">
            <svg v-if="!saving" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
              <polyline points="17 21 17 13 7 13 7 21"></polyline>
              <polyline points="7 3 7 8 15 8"></polyline>
            </svg>
            <span v-else class="spinner-dot"></span>
            {{ saving ? 'Đang lưu...' : (modal.isEdit ? 'Lưu thay đổi' : 'Tạo thuộc tính') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Xác Nhận -->
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
          <button type="button" class="btn btn-secondary" @click="closeConfirm">Đóng</button>
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

    <!-- Toast Notification -->
    <div :class="['custom-toast', toast.type, { show: toast.show }]">
      <div class="toast-indicator"></div>
      <div class="toast-body">
        <h5 class="toast-title">{{ toast.title }}</h5>
        <p class="toast-desc">{{ toast.message }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.thuoc-tinh-page {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  box-sizing: border-box;
}

/* Header */
.page-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.title-cluster {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.breadcrumb-trail {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 4px;
}

.bc-link {
  color: var(--blue, #496883);
  text-decoration: none;
  font-weight: 600;
}

.bc-link:hover {
  text-decoration: underline;
}

.bc-separator {
  color: #cbd5e1;
}

.bc-current {
  color: #0f172a;
  font-weight: 700;
}

.main-title {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.3px;
}

.sub-title {
  margin: 0;
  font-size: 0.9rem;
  color: #64748b;
}

.btn-create-attr {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #3b596a;
  color: #ffffff;
  border: 1px solid #324b5a;
  padding: 0.6rem 1.4rem;
  border-radius: 9999px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
}

.btn-create-attr:hover {
  background-color: #2c4350;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
  transform: translateY(-1px);
}

/* Card chính */
.attr-card-container {
  background-color: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  overflow: hidden;
}

/* Thanh Tabs */
.tabs-scroll-wrapper {
  overflow-x: auto;
  border-bottom: 1px solid var(--line, #e9e5db);
  background-color: #faf8f5;
}

.attr-tabs-nav {
  display: flex;
  gap: 4px;
  padding: 8px 12px 0;
  min-width: max-content;
}

.attr-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0.65rem 1.1rem;
  border: 1px solid transparent;
  border-bottom: none;
  background: transparent;
  color: #5d6a70;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px 8px 0 0;
  transition: all 0.15s ease;
}

.attr-tab-btn:hover {
  color: #496883;
  background-color: #f4f6f5;
}

.attr-tab-btn.active {
  background-color: #ffffff;
  color: #355d74;
  font-weight: 700;
  border-color: var(--line, #e9e5db);
  border-bottom: 2px solid #ffffff;
  margin-bottom: -1px;
}

.tab-emoji {
  font-size: 1rem;
}

.tab-count-badge {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 10px;
  font-size: 0.72rem;
  font-weight: 700;
  background-color: #e9e5db;
  color: #5d6a70;
}

.attr-tab-btn.active .tab-count-badge {
  background-color: #e7f0f4;
  color: #355d74;
}

/* Toolbar */
.table-toolbar {
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  border-bottom: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
}

.search-box {
  position: relative;
  flex: 1;
  max-width: 380px;
}

.search-icon {
  position: absolute;
  left: 11px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

.search-input {
  width: 100%;
  height: 38px;
  padding: 0 32px 0 34px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  font-size: 0.88rem;
  color: var(--text, #3d4a50);
  outline: none;
  background-color: #fcfbf8;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.clear-search-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #94a3b8;
  cursor: pointer;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.select-filter {
  height: 38px;
  padding: 0 10px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  font-size: 0.88rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  cursor: pointer;
}

.select-filter:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

.btn-refresh {
  height: 38px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  border-radius: 8px;
  color: #957b48;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-refresh:hover {
  background-color: #faeed7;
}

/* Table */
.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  background-color: #faf8f5;
  padding: 0.95rem 1rem;
  font-size: 0.82rem;
  font-weight: 700;
  color: #4a5568;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #e9e5db;
}

.data-table td {
  padding: 0.95rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  font-size: 0.9rem;
  color: #4b585e;
  vertical-align: middle;
}

.data-table tr:hover td {
  background-color: #fcfbf8;
}

.table-empty-cell {
  text-align: center;
  padding: 3rem 1rem !important;
  color: #94a3b8;
  font-size: 0.95rem;
}

.attr-code-pill {
  display: inline-block;
  padding: 3px 8px;
  background-color: #f7f5ef;
  border: 1px solid #e9e5db;
  border-radius: 5px;
  font-family: monospace, sans-serif;
  font-weight: 700;
  color: #8c6a46;
  font-size: 0.85rem;
  letter-spacing: 0.4px;
}

.attr-name-text {
  font-weight: 700;
  color: #2b383e;
}

.color-badge-preview {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.color-swatch-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.color-hex-val {
  font-family: monospace;
  font-weight: 600;
  color: #334155;
  font-size: 0.85rem;
}

.status-chip {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 700;
  white-space: nowrap;
}

.chip-active {
  background-color: #eaf5ed;
  color: #247541;
  border: 1px solid #bce1c7;
}

.chip-inactive {
  background-color: #f7edeb;
  color: #b34a3b;
  border: 1px solid #ecc9c3;
}

/* Nút hành động */
.row-actions-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-circle-action {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #e2e8f0;
  background-color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
}

.btn-power {
  color: #b34a3b;
  background-color: #f7edeb;
  border-color: #ecc9c3;
}

.btn-power.is-active {
  color: #247541;
  background-color: #eaf5ed;
  border-color: #bce1c7;
}

.btn-power:hover {
  transform: scale(1.1);
}

.btn-edit {
  color: #496883;
  background-color: #f7f5ee;
  border-color: #d8e2e8;
}

.btn-edit:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: scale(1.1);
}

.btn-delete {
  color: #b34a3b;
  background-color: #f7edeb;
  border-color: #ecc9c3;
}

.btn-delete:hover {
  background-color: #b34a3b;
  color: #ffffff;
  border-color: #b34a3b;
  transform: scale(1.1);
}

/* Phân trang */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-top: 1px solid #f1f5f9;
  flex-wrap: wrap;
  gap: 1rem;
}

.pagination-info {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  color: #555e65;
  line-height: 1.5;
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  border-radius: 6px;
  color: #496883;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
}

.page-btn.active {
  background-color: var(--blue, #496883);
  border-color: var(--blue, #496883);
  color: #ffffff;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-box {
  background-color: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: modalEnter 0.2s ease;
}

@keyframes modalEnter {
  from { opacity: 0; transform: scale(0.97) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
}

.modal-header-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.modal-tag {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--blue, #496883);
  background-color: #eaf1f4;
  padding: 2px 7px;
  border-radius: 5px;
  width: fit-content;
}

.modal-header-title h3 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 800;
  color: #0f172a;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}

.modal-close:hover {
  color: #334155;
}

.modal-body {
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: #475569;
  letter-spacing: 0.4px;
}

.form-label .note {
  font-weight: 500;
  color: #94a3b8;
  font-size: 0.75rem;
}

.form-label .req {
  color: #ef4444;
}

.form-input {
  height: 2.45rem;
  padding: 0 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  color: #1e293b;
  outline: none;
  transition: all 0.15s;
}

.form-input:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.error-msg {
  font-size: 0.78rem;
  color: #ef4444;
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-picker-input {
  width: 42px;
  height: 2.45rem;
  padding: 0;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
}

.color-hex-input {
  flex: 1;
  font-family: monospace;
  font-weight: 700;
}

.color-preview-block {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #efeae0;
  background-color: #faf8f5;
}

.btn-cancel {
  height: 2.4rem;
  padding: 0 1.2rem;
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  border-radius: 8px;
  color: #957b48;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background-color: #faeed7;
}

.btn-submit {
  height: 2.4rem;
  padding: 0 1.25rem;
  background-color: #3b596a;
  border: 1px solid #324b5a;
  border-radius: 8px;
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-submit:hover {
  background-color: #2c4350;
  border-color: #243742;
}

.btn-submit.btn-danger {
  background-color: #dc2626;
  border-color: #dc2626;
}

.btn-submit.btn-danger:hover {
  background-color: #b91c1c;
}

/* Modal xác nhận chuẩn thiết kế */
.confirm-modal-overlay {
  z-index: 99990;
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: modalEnter 0.2s ease;
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

.confirm-message-text {
  margin: 0;
  font-size: 0.95rem;
  color: #435158;
  line-height: 1.5;
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

/* Toast */
.custom-toast {
  position: fixed;
  top: 24px;
  right: 24px;
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);
  display: flex;
  overflow: hidden;
  min-width: 320px;
  max-width: 420px;
  z-index: 2000;
  opacity: 0;
  transform: translateY(-16px);
  pointer-events: none;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.custom-toast.show {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.toast-indicator {
  width: 5px;
}

.custom-toast.success .toast-indicator {
  background-color: #10b981;
}

.custom-toast.error .toast-indicator {
  background-color: #ef4444;
}

.toast-body {
  padding: 12px 16px;
  flex: 1;
}

.toast-title {
  margin: 0 0 2px 0;
  font-size: 0.92rem;
  font-weight: 700;
  color: #0f172a;
}

.toast-desc {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
}

/* Responsive */
@media (max-width: 768px) {
  .thuoc-tinh-page {
    padding: 1rem;
  }
  .page-top-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .btn-create-attr {
    width: 100%;
    justify-content: center;
  }
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .search-box {
    max-width: 100%;
  }
  .toolbar-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
