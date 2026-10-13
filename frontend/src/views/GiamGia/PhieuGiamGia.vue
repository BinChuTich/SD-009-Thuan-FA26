<template>
  <div class="voucher-page-wrapper">
    <!-- 1. Thanh tiêu đề trên cùng -->
    <div class="page-breadcrumb">
      <router-link to="/phieu-giam-gia" class="crumb-parent">Quản lý giảm giá</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Phiếu giảm giá</span>
    </div>

    <!-- 2. Khung Bộ lọc (Tone màu ấm FF T-shirt) -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-title-wrap">
          <h3 class="filter-title">Bộ lọc</h3>
        </div>
      </div>

      <!-- Lưới 6 ô lọc ngang -->
      <div class="filter-inputs-grid">
        <!-- Tìm kiếm -->
        <div class="form-field">
          <label>Tìm kiếm</label>
          <div class="input-inner">
            <input
                type="text"
                v-model="filters.keyword"
                placeholder="Mã, tên phiếu..."
            />
          </div>
        </div>

        <!-- Hình thức -->
        <div class="form-field">
          <label>Hình thức</label>
          <Select2Combobox
            v-model="filters.hinhThuc"
            :options="hinhThucOptions"
            placeholder="Tất cả hình thức"
          />
        </div>

        <!-- Ngày bắt đầu -->
        <div class="form-field">
          <label>Ngày bắt đầu</label>
          <div class="input-inner">
            <input
                type="date"
                v-model="filters.startDate"
            />
          </div>
        </div>

        <!-- Ngày kết thúc -->
        <div class="form-field">
          <label>Ngày kết thúc</label>
          <div class="input-inner">
            <input
                type="date"
                v-model="filters.endDate"
            />
          </div>
        </div>

        <!-- Loại giảm -->
        <div class="form-field">
          <label>Loại giảm</label>
          <Select2Combobox
            v-model="filters.loaiGiam"
            :options="loaiGiamOptions"
            placeholder="Tất cả loại giảm"
          />
        </div>

        <!-- Trạng thái -->
        <div class="form-field">
          <label>Trạng thái</label>
          <Select2Combobox
            v-model="filters.trangThai"
            :options="trangThaiOptions"
            placeholder="Tất cả trạng thái"
          />
        </div>
      </div>

      <!-- Cụm nút thao tác bên dưới bên phải -->
      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          Đặt lại bộ lọc
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          Tạo phiếu mới
        </button>
      </div>
    </div>

    <!-- 3. Khung Danh sách phiếu giảm giá -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <h3 class="table-title">Danh sách phiếu giảm giá</h3>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 140px;">Mã</th>
            <th>Tên phiếu</th>
            <th style="width: 130px;">Hình thức</th>
            <th style="width: 130px;">Giá trị giảm</th>
            <th style="width: 120px;">Ngày bắt đầu</th>
            <th style="width: 120px;">Ngày kết thúc</th>
            <th style="width: 140px; text-align: center;">Trạng thái</th>
            <th style="width: 130px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <!-- Hiển thị khi đang nạp dữ liệu từ backend -->
          <tr v-if="loading">
            <td colspan="9" class="table-empty-cell">
              <span class="loading-spinner">⏳</span> Đang nạp dữ liệu phiếu giảm giá từ SQL Server...
            </td>
          </tr>

          <!-- Hiển thị khi không có dữ liệu nào khớp bộ lọc -->
          <tr v-else-if="filteredList.length === 0">
            <td colspan="9" class="table-empty-cell text-muted">
              Không có phiếu giảm giá nào phù hợp.
            </td>
          </tr>

          <!-- Danh sách phiếu giảm giá load từ Database (Đã phân trang) -->
          <tr v-else v-for="(item, index) in paginatedList" :key="item.id">
            <td style="text-align: center;" class="text-muted">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td class="font-bold text-blue">{{ item.code }}</td>
            <td class="font-medium text-title">{{ item.name }}</td>

            <!-- Badge Hình thức: Công khai (Vàng cát), Cá nhân (Xanh đá) -->
            <td>
                <span :class="['badge-form', item.form === 'Công khai' ? 'badge-public' : 'badge-personal']">
                  <span class="dot-icon">●</span> {{ item.form }}
                </span>
            </td>

            <!-- Giá trị giảm: format % hoặc VNĐ -->
            <td class="font-bold text-dark">{{ item.discountValue }}</td>

            <!-- Ngày tháng bắt đầu & kết thúc -->
            <td class="text-muted-dark">{{ item.startDate }}</td>
            <td class="text-muted-dark">{{ item.endDate }}</td>

            <!-- Badge Trạng thái: Đang hoạt động / Đã hết hạn / Ngừng hoạt động / Sắp diễn ra -->
            <td style="text-align: center;">
                <span :class="['badge-status', 'status-' + item.statusCode]">
                  {{ item.status }}
                </span>
            </td>

            <!-- Cột Hành động: 1. Đổi trạng thái (Bật/Tắt) | 2. Vừa xem vừa sửa -->
            <td style="text-align: center;">
              <div class="action-buttons">
                <!-- Switch Bật / Tắt trạng thái hoạt động chuẩn ảnh mẫu -->
                <label
                    class="switch-toggle"
                    :class="{ 'switch-disabled': item.isExpired }"
                    :title="item.isExpired ? 'Phiếu đã hết hạn - Không thể đổi trạng thái' : (item.rawTrangThai !== 0 ? 'Đang hoạt động (nhấn để tắt)' : 'Đang ngừng hoạt động (nhấn để bật)')"
                >
                  <input
                      type="checkbox"
                      :checked="item.rawTrangThai !== 0 && !item.isExpired"
                      :disabled="item.isExpired"
                      @click.prevent="openToggleStatusModal(item)"
                  />
                  <span class="slider-round"></span>
                </label>

                <!-- Nút 2: Vừa xem chi tiết vừa chỉnh sửa phiếu giảm giá -->
                <button
                    class="btn-action-edit"
                    title="Xem chi tiết & Chỉnh sửa phiếu giảm giá"
                    @click="viewDetail(item)"
                >
                  <!-- SVG biểu tượng Bút & Tài liệu chuẩn (Xem & Sửa) -->
                  <svg class="action-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. Thanh phân trang dưới cùng bảng -->
      <div class="pagination-footer" v-if="filteredList.length > 0">
        <div class="pagination-controls">
          <button
              class="pg-btn"
              :disabled="currentPage === 1"
              @click="goToPage(1)"
              title="Về trang đầu tiên"
          >
            ⇤
          </button>
          <button
              class="pg-btn"
              :disabled="currentPage === 1"
              @click="prevPage"
              title="Trang trước"
          >
            ‹
          </button>

          <template v-for="(p, idx) in visiblePages" :key="idx">
            <span v-if="p === '...'" class="pg-dots">...</span>
            <button
                v-else
                :class="['pg-btn', { active: currentPage === p }]"
                @click="goToPage(p)"
            >
              {{ p }}
            </button>
          </template>

          <button
              class="pg-btn"
              :disabled="currentPage === totalPages"
              @click="nextPage"
              title="Trang tiếp theo"
          >
            ›
          </button>
          <button
              class="pg-btn"
              :disabled="currentPage === totalPages"
              @click="goToPage(totalPages)"
              title="Đến trang cuối cùng"
          >
            ⇥
          </button>
        </div>
      </div>
    </div>

    <!-- 4. POPUP MODAL XÁC NHẬN ĐỔI TRẠNG THÁI HOẠT ĐỘNG -->
    <div v-if="showToggleConfirmModal" class="modal-overlay" @click.self="closeToggleConfirmModal">
      <div class="modal-box confirm-modal-box">
        <div class="confirm-modal-header">
          <div class="confirm-warning-icon">
            <svg v-if="voucherToToggle?.rawTrangThai !== 0" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"></path>
            </svg>
          </div>
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">
              {{ voucherToToggle?.rawTrangThai !== 0 ? 'Xác nhận ngừng hoạt động' : 'Xác nhận kích hoạt phiếu' }}
            </h3>
            <p class="confirm-subtitle">
              {{ voucherToToggle?.rawTrangThai !== 0 ? 'Thao tác sẽ tạm dừng áp dụng phiếu này' : 'Phiếu sẽ có hiệu lực áp dụng cho khách hàng' }}
            </p>
          </div>
          <button class="modal-close-btn" @click="closeToggleConfirmModal" :disabled="togglingStatus">✕</button>
        </div>

        <div class="confirm-modal-body">
          <p class="confirm-message-text" v-if="voucherToToggle?.rawTrangThai !== 0">
            Bạn có chắc chắn muốn chuyển phiếu giảm giá
            <b class="text-blue">[{{ voucherToToggle?.code }}]</b> sang trạng thái
            <b class="text-highlight">Ngừng hoạt động</b> không?
          </p>
          <p class="confirm-message-text" v-else>
            Bạn có chắc chắn muốn
            <b class="text-activate">Kích hoạt lại</b> phiếu giảm giá
            <b class="text-blue">[{{ voucherToToggle?.code }}]</b> để áp dụng cho khách hàng không?
          </p>
        </div>

        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="closeToggleConfirmModal" :disabled="togglingStatus">
            Hủy bỏ
          </button>
          <button
              class="btn btn-save-confirm"
              @click="confirmToggleStatus"
              :disabled="togglingStatus"
          >
            {{ togglingStatus ? 'Đang cập nhật...' : (voucherToToggle?.rawTrangThai !== 0 ? 'Ngừng hoạt động' : 'Kích hoạt phiếu') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 5. TOAST THÔNG BÁO ĐỒNG BỘ GIAO DIỆN PHIẾU GIẢM GIÁ -->
    <transition name="toast-slide">
      <div v-if="toast.show" :class="['toast-msg', `toast-${toast.type}`]">
        <div class="toast-icon">
          <svg v-if="toast.type === 'success'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          <svg v-else-if="toast.type === 'error'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          <svg v-else-if="toast.type === 'warning'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
        </div>
        <div class="toast-content">
          <span class="toast-title">
            {{ toast.type === 'success' ? 'Thành công!' : (toast.type === 'error' ? 'Thao tác thất bại' : (toast.type === 'warning' ? 'Cảnh báo' : 'Thông báo')) }}
          </span>
          <span class="toast-text">{{ toast.message }}</span>
        </div>
        <button class="toast-btn-close" @click="closeToast" title="Đóng">✕</button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api'
import Select2Combobox from '../../components/Select2Combobox.vue'

const router = useRouter()

const hinhThucOptions = [
  { value: '', label: 'Tất cả hình thức' },
  { value: 'Công khai', label: 'Công khai' },
  { value: 'Cá nhân', label: 'Cá nhân' }
]

const loaiGiamOptions = [
  { value: '', label: 'Tất cả loại giảm' },
  { value: 'percent', label: 'Giảm theo %' },
  { value: 'amount', label: 'Giảm tiền mặt' }
]

const trangThaiOptions = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang diễn ra' },
  { value: 'upcoming', label: 'Sắp diễn ra' },
  { value: 'expired', label: 'Đã kết thúc / Hết hạn' },
  { value: 'inactive', label: 'Ngừng hoạt động' }
]

const filters = ref({
  keyword: '',
  hinhThuc: '',
  startDate: '',
  endDate: '',
  loaiGiam: '',
  trangThai: ''
})

const vouchers = ref([])
const loading = ref(false)
const selectedVoucher = ref(null)

// State quản lý popup xác nhận đổi trạng thái & toast thông báo
const showToggleConfirmModal = ref(false)
const voucherToToggle = ref(null)
const togglingStatus = ref(false)
const toast = ref({ show: false, message: '', type: 'success' })
let toastTimer = null

const showToastNotification = (message, type = 'success', duration = 3500) => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, duration)
}

const closeToast = () => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value.show = false
}

const currentPage = ref(1)
const pageSize = ref(5)

const formatDate = (dateStr, isEndDate = false) => {
  if (!dateStr) return isEndDate ? 'Vô hạn' : '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes} ${day}/${month}/${year}`
  } catch (e) {
    return dateStr
  }
}

const fetchVouchers = async () => {
  loading.value = true
  try {
    const response = await api.get('/api/phieu-giam-gia')
    const data = response.data || []

    const now = new Date()

    vouchers.value = data.map(item => {
      const isPercent = item.loaiPhieuGiamGia === 1
      const discountDisplay = isPercent
          ? `${item.giaTriGiamGia}%`
          : `${Number(item.giaTriGiamGia || 0).toLocaleString('vi-VN')} đ`
      const endDateObj = item.ngayKetThuc ? new Date(item.ngayKetThuc) : null
      const startDateObj = item.ngayBatDau ? new Date(item.ngayBatDau) : null
      const isExpired = endDateObj ? endDateObj < now : false
      const isUpcoming = startDateObj ? startDateObj > now : false

      let statusText = 'Đang diễn ra'
      let statusCode = 'active'

      if (isExpired) {
        statusText = 'Đã kết thúc'
        statusCode = 'expired'
      } else if (item.trangThai === 0) {
        statusText = 'Ngừng hoạt động'
        statusCode = 'inactive'
      } else if (isUpcoming || item.trangThai === 2) {
        statusText = 'Sắp diễn ra'
        statusCode = 'upcoming'
      } else {
        statusText = 'Đang diễn ra'
        statusCode = 'active'
      }

      return {
        id: item.id,
        code: item.maPhieuGiamGia || ('PGG' + item.id),
        name: item.tenPhieuGiamGia || '',
        form: item.hinhThuc || (item.soKhachHang > 0 ? 'Cá nhân' : 'Công khai'),
        soKhachHang: item.soKhachHang || 0,
        danhSachKhachHang: item.danhSachKhachHang || [],
        danhSachKhachHangText: (item.danhSachKhachHang && item.danhSachKhachHang.length > 0)
            ? item.danhSachKhachHang.join(', ')
            : 'Chưa có khách hàng cụ thể',
        loaiPhieuGiamGia: item.loaiPhieuGiamGia,
        loaiGiamText: isPercent ? 'Giảm theo phần trăm (%)' : 'Giảm tiền mặt trực tiếp',
        discountValue: discountDisplay,
        giamToiDaFormatted: item.giamToiDa ? `${Number(item.giamToiDa).toLocaleString('vi-VN')} đ` : 'Không giới hạn',
        hoaDonToiThieuFormatted: item.hoaDonToiThieu ? `${Number(item.hoaDonToiThieu).toLocaleString('vi-VN')} đ` : '0 đ',
        soLuongSuDung: item.soLuongSuDung ?? 0,
        startDate: formatDate(item.ngayBatDau, false),
        endDate: formatDate(item.ngayKetThuc, true),
        rawStartDate: item.ngayBatDau,
        rawEndDate: item.ngayKetThuc,
        rawTrangThai: item.trangThai,
        isExpired: isExpired,
        statusCode: statusCode,
        status: statusText
      }
    })
  } catch (error) {
    console.error('Lỗi khi nạp dữ liệu phiếu giảm giá từ SQL Server:', error)
    showToastNotification('Không thể kết nối tới máy chủ dữ liệu!', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchVouchers()
})

const filteredList = computed(() => {
  return vouchers.value.filter(v => {
    const matchKw = !filters.value.keyword ||
        v.code.toLowerCase().includes(filters.value.keyword.toLowerCase()) ||
        v.name.toLowerCase().includes(filters.value.keyword.toLowerCase())
    const matchForm = !filters.value.hinhThuc || v.form === filters.value.hinhThuc
    const matchType = !filters.value.loaiGiam ||
        (filters.value.loaiGiam === 'percent' && v.loaiPhieuGiamGia === 1) ||
        (filters.value.loaiGiam === 'amount' && v.loaiPhieuGiamGia === 2)
    const matchStatus = !filters.value.trangThai || v.statusCode === filters.value.trangThai
    const matchStart = !filters.value.startDate ||
        (v.rawStartDate && new Date(v.rawStartDate) >= new Date(filters.value.startDate))
    const matchEnd = !filters.value.endDate ||
        (v.rawEndDate && new Date(v.rawEndDate) <= new Date(filters.value.endDate + 'T23:59:59'))

    return matchKw && matchForm && matchType && matchStatus && matchStart && matchEnd
  })
})

const totalPages = computed(() => {
  return Math.ceil(filteredList.value.length / pageSize.value) || 1
})

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredList.value.slice(start, end)
})

const startIndex = computed(() => {
  if (filteredList.value.length === 0) return 0
  return (currentPage.value - 1) * pageSize.value + 1
})

const endIndex = computed(() => {
  return Math.min(currentPage.value * pageSize.value, filteredList.value.length)
})

watch(filters, () => {
  currentPage.value = 1
}, { deep: true })

watch(pageSize, () => {
  currentPage.value = 1
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const pages = []
  const left = Math.max(2, current - 1)
  const right = Math.min(total - 1, current + 1)

  pages.push(1)
  if (left > 2) pages.push('...')
  for (let i = left; i <= right; i++) {
    pages.push(i)
  }
  if (right < total - 1) pages.push('...')
  pages.push(total)

  return pages
})

const goToPage = (page) => {
  if (typeof page === 'number' && page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    hinhThuc: '',
    startDate: '',
    endDate: '',
    loaiGiam: '',
    trangThai: ''
  }
  currentPage.value = 1
}

// Mở modal xác nhận đổi trạng thái
const openToggleStatusModal = (item) => {
  if (item.isExpired) {
    showToastNotification(`Phiếu [${item.code}] đã hết hạn vào ngày ${item.endDate}, không thể kích hoạt lại!`, 'warning')
    return
  }
  voucherToToggle.value = item
  showToggleConfirmModal.value = true
}

// Đóng modal xác nhận
const closeToggleConfirmModal = () => {
  if (togglingStatus.value) return
  showToggleConfirmModal.value = false
  voucherToToggle.value = null
}

// Xác nhận đổi trạng thái qua API Backend
const confirmToggleStatus = async () => {
  if (!voucherToToggle.value) return
  togglingStatus.value = true
  const item = voucherToToggle.value
  const newStatusText = item.rawTrangThai !== 0 ? 'Ngừng hoạt động' : 'Đang hoạt động'

  try {
    await api.put(`/api/phieu-giam-gia/${item.id}/toggle-status`)
    // Tải lại danh sách mới nhất từ Database
    await fetchVouchers()
    showToggleConfirmModal.value = false
    showToastNotification(`Đã chuyển phiếu [${item.code}] sang trạng thái "${newStatusText}".`, 'success')
  } catch (error) {
    console.error('Lỗi khi cập nhật trạng thái:', error)
    showToastNotification('Cập nhật trạng thái thất bại! Vui lòng kiểm tra kết nối cơ sở dữ liệu.', 'error')
  } finally {
    togglingStatus.value = false
    voucherToToggle.value = null
  }
}

// Điều hướng vừa xem vừa sửa phiếu giảm giá
const viewDetail = (item) => {
  router.push(`/phieu-giam-gia/chi-tiet/${item.id}`)
}

// Chuyển sang trang tạo phiếu mới
const openCreateModal = () => {
  router.push('/phieu-giam-gia/tao-moi')
}
</script>

<style scoped>
.voucher-page-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.breadcrumb-header {
  margin-bottom: 1.25rem;
  padding: 0.2rem 0;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.95rem;
}

.menu-toggle-icon {
  font-size: 1.1rem;
  color: #8c9597;
  cursor: pointer;
}

.breadcrumb-text {
  color: #8c9597;
}

.breadcrumb-text b {
  color: var(--blue, #496883);
  font-weight: 700;
}

.slash {
  margin: 0 5px;
  color: #d8d4c9;
}

/* Khung Card */
.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  margin-bottom: 1.25rem;
}

/* Header Bộ lọc */
.card-header-filter {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.2rem;
}

.filter-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #f7eee1;
  display: grid;
  place-items: center;
}

.filter-icon {
  font-size: 1.15rem;
  color: #b18b52;
}

.filter-title-wrap {
  display: flex;
  flex-direction: column;
}

.filter-title {
  font-size: 1.05rem; /* ~16.8px */
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

.filter-subtitle {
  font-size: 0.85rem;
  color: #8c9597;
  margin: 2px 0 0 0;
}

/* Lưới 6 ô lọc */
.filter-inputs-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-field label {
  font-size: 0.88rem; /* ~14px */
  font-weight: 700;
  color: #4f5d63;
}

.input-inner {
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 0.75rem;
  font-size: 0.9rem;
  color: #9aa0a0;
  pointer-events: none;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 2.6rem; /* ~41.6px */
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem; /* Cũ: 12.5px -> ~14.7px */
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field .input-inner input[type="text"] {
  padding-left: 2.2rem;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

/* Cụm nút thao tác */
.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  border-top: 1px dashed #efeae0;
  padding-top: 1.1rem;
}

.btn {
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
}

.btn-reset {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-reset:hover {
  background-color: #faeed7;
}

.btn-export {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #d2e5d6;
}
.btn-export:hover {
  background-color: #deede1;
}

.btn-primary {
  background-color: var(--blue, #496883);
  border: none;
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #38536b;
}

/* Bảng danh sách */
.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.1rem;
}

.table-title {
  font-size: 1.05rem; /* ~16.8px */
  font-weight: 700;
  margin: 0;
  color: #3c4d55;
}

.record-count {
  font-size: 0.88rem;
  color: #8c9597;
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem; /* To rõ ~15.2px */
}

.custom-table th {
  background-color: #faf9f6;
  color: #6f7c82;
  font-weight: 700;
  padding: 0.95rem 1rem;
  text-align: left;
  border-bottom: 1px solid #efede7;
  font-size: 0.95rem;
  white-space: nowrap;
}

.custom-table td {
  padding: 1.05rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.custom-table tr:hover td {
  background-color: #fcfbf8;
}

.font-bold {
  font-weight: 700;
}

.font-medium {
  font-weight: 600;
}

.text-blue {
  color: var(--blue, #496883);
  font-family: monospace, sans-serif;
  letter-spacing: 0.5px;
}

.text-title {
  color: #2b383e;
}

.text-dark {
  color: #1f272b;
}

.text-muted {
  color: #9aa0a0;
}

.text-muted-dark {
  color: #556268;
}

/* Badge Hình thức */
.badge-form {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  padding: 0.35rem 0.75rem;
  border-radius: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-public {
  background-color: #fcf3e6;
  color: #b38536;
}

.badge-personal {
  background-color: #eaf1f5;
  color: #496883;
}

.dot-icon {
  font-size: 0.6rem;
}

/* Badge Trạng thái */
.badge-status {
  display: inline-block;
  font-size: 0.82rem;
  font-weight: 700;
  padding: 0.35rem 0.85rem;
  border-radius: 14px;
  white-space: nowrap;
}

.status-active {
  background-color: #edf6ef;
  color: #4c8a5a;
}

.status-inactive {
  background-color: #f6f6f6;
  color: #7f8c8d;
}

.status-expired {
  background-color: #fdeeee;
  color: #c0392b;
  border: 1px solid #fadad7;
}

.status-upcoming {
  background-color: #eaf1f8;
  color: #2980b9;
  border: 1px solid #d4e6f1;
}

/* Nút làm mới bảng */
.header-right-tools {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #f7eee1;
  border: 1px solid #dfd5c2;
  color: #957b48;
  padding: 0.35rem 0.75rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh:hover {
  background-color: #faeed7;
}

.refresh-icon.spin {
  display: inline-block;
  animation: spin 1s infinite linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.table-empty-cell {
  text-align: center;
  padding: 2.5rem 1rem !important;
  font-size: 0.95rem;
}

/* Cột hành động: Các nút thao tác */
.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
}

/* SWITCH TOGGLE (Công tắc bật/tắt chuẩn theo ảnh) */
.switch-toggle {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
  cursor: pointer;
  margin: 0;
  vertical-align: middle;
}

.switch-toggle.switch-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.switch-toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider-round {
  position: absolute;
  inset: 0;
  background-color: #d1d5db;
  border-radius: 22px;
  transition: all 0.25s ease;
}

.slider-round::before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: all 0.25s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch-toggle input:checked + .slider-round {
  background-color: var(--blue, #496883);
}

.switch-toggle input:checked + .slider-round::before {
  transform: translateX(16px);
}

/* Nút Vừa xem chi tiết vừa chỉnh sửa phiếu */
.btn-action-edit {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #d0dfe8;
  background-color: #eef5f9;
  color: #496883;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.btn-action-edit:hover {
  background-color: #dce9f3;
  border-color: #496883;
  color: #2c465d;
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(73, 104, 131, 0.22);
}

/* Icon SVG bên trong 2 nút hành động */
.action-svg {
  width: 17px;
  height: 17px;
  display: block;
}

/* POPUP MODAL XÁC NHẬN ĐỔI TRẠNG THÁI (GIAO DIỆN CHUẨN ĐẸP) */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(30, 40, 45, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: popIn 0.2s ease-out;
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
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
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.confirm-title-wrap {
  flex: 1;
}

.confirm-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #8c6328;
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

.text-danger {
  color: #c0392b;
  font-weight: 700;
}

.text-success {
  color: #2e7d32;
  font-weight: 700;
}

.text-activate {
  color: var(--blue, #496883);
  font-weight: 700;
}

.text-blue {
  color: var(--blue, #496883);
  font-weight: 700;
}

.text-highlight {
  color: #8c6328;
  font-weight: 700;
}

/* Bảng tóm tắt thông số phiếu trong Modal */
.confirm-summary-panel {
  background-color: #faf9f6;
  border: 1px solid #efeae0;
  border-radius: 8px;
  padding: 0.9rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
}

.s-label {
  color: #7b888e;
  font-weight: 600;
}

.s-val {
  color: #2b383e;
}

.badge-status-sm {
  display: inline-block;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 0.2rem 0.65rem;
  border-radius: 12px;
}

/* Khung ghi chú nhắc nhở */
.confirm-note-box {
  border-radius: 8px;
  padding: 0.75rem 0.95rem;
  font-size: 0.85rem;
  line-height: 1.45;
}

.note-warning {
  background-color: #fef8ee;
  border: 1px solid #f6e2be;
  color: #8a6528;
}

.note-info {
  background-color: #edf5fa;
  border: 1px solid #d0e4f2;
  color: #376384;
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
}

.btn-secondary:hover:not(:disabled) {
  background-color: #f7f5ef;
  color: #2b383e;
  border-color: #c5bfae;
}

.btn-save-confirm {
  background-color: #b38e6e;
  color: #ffffff;
  font-weight: 700;
}

.btn-save-confirm:hover:not(:disabled) {
  background-color: #9c7857;
}

.btn-confirm-deactivate {
  background-color: #b38e6e;
  color: #ffffff;
  font-weight: 700;
}

.btn-confirm-deactivate:hover:not(:disabled) {
  background-color: #9c7857;
}

/* Toast thông báo đồng bộ giao diện Phiếu giảm giá */
.toast-msg {
  position: fixed;
  bottom: 24px;
  right: 28px;
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(65, 60, 50, 0.14), 0 8px 10px -6px rgba(65, 60, 50, 0.08);
  border: 1px solid #efeae0;
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 99999;
  max-width: 440px;
}

.toast-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.toast-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.toast-title {
  font-size: 0.92rem;
  font-weight: 700;
  color: #2b383e;
  line-height: 1.25;
}

.toast-text {
  font-size: 0.86rem;
  font-weight: 500;
  color: #616e75;
  line-height: 1.4;
}

.toast-btn-close {
  background: transparent;
  border: none;
  font-size: 1rem;
  color: #a0a8ab;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.toast-btn-close:hover {
  color: #2b383e;
  background-color: #f7f5ef;
}

/* Biến thể màu cho Toast theo phong cách ấm của Phiếu giảm giá */
.toast-success {
  border-left: 4px solid #4c8a5a;
}
.toast-success .toast-icon {
  background-color: #edf5ef;
  color: #4c8a5a;
}

.toast-warning {
  border-left: 4px solid #b38e6e;
}
.toast-warning .toast-icon {
  background-color: #fff9f0;
  color: #b38e6e;
}
.toast-warning .toast-title {
  color: #8c6328;
}

.toast-error {
  border-left: 4px solid #c0392b;
}
.toast-error .toast-icon {
  background-color: #fdeded;
  color: #c0392b;
}
.toast-error .toast-title {
  color: #c0392b;
}

.toast-info {
  border-left: 4px solid #496883;
}
.toast-info .toast-icon {
  background-color: #edf5fa;
  color: #496883;
}
.toast-info .toast-title {
  color: #496883;
}

/* Hiệu ứng trượt */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}
.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) translateX(25px);
}

/* Phân trang dưới cùng bảng */
.pagination-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 1.1rem 1.25rem 0.5rem;
  margin-top: 0.5rem;
  border-top: 1px dashed #eeebe3;
  flex-wrap: wrap;
  gap: 1rem;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #616e75;
}

.page-size-select {
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #33444d;
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
}

.page-size-select:focus {
  border-color: var(--blue, #496883);
}

.pagination-separator {
  color: #d1cbbe;
  margin: 0 0.25rem;
}

.pagination-info {
  color: #616e75;
}

.pagination-info b {
  color: #2b383e;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.pg-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 0.55rem;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #496883;
  font-size: 0.88rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.pg-btn:hover:not(:disabled) {
  border-color: var(--blue, #496883);
  background-color: #eaf1f4;
  color: var(--blue, #496883);
}

.pg-btn.active {
  background-color: var(--blue, #496883);
  border-color: var(--blue, #496883);
  color: #ffffff;
  font-weight: 700;
}

.pg-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background-color: #f7f6f2;
  border-color: #eeebe3;
  color: #a8b0b4;
}

.pg-dots {
  padding: 0 0.35rem;
  color: #8c9597;
  font-weight: bold;
}

@media (max-width: 1200px) {
  .filter-inputs-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }

  .pagination-footer {
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
  }
}
</style>