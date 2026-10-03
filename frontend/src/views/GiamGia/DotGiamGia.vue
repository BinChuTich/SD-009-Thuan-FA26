<template>
  <div class="dot-giam-gia-wrapper">
    <!-- 1. Thanh Breadcrumb trên cùng -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <span class="breadcrumb-text">
          Quản lý giảm giá <span class="slash">/</span> <b>Đợt giảm giá</b>
        </span>
      </div>
    </div>

    <!-- 2. Khung Bộ lọc -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-icon-box">
          <span class="filter-icon">🍸</span>
        </div>
        <div class="filter-title-wrap">
          <h3 class="filter-title">Bộ lọc</h3>
          <p class="filter-subtitle">Tra cứu nhanh dữ liệu từ cơ sở dữ liệu.</p>
        </div>
      </div>

      <!-- Lưới 4 ô lọc -->
      <div class="filter-inputs-grid">
        <!-- Tìm kiếm -->
        <div class="form-field">
          <label>Tìm kiếm</label>
          <div class="input-inner">
            <span class="prefix-icon">🔍</span>
            <input
                type="text"
                v-model="filters.keyword"
                @input="handleSearchInput"
                placeholder="Mã, tên đợt giảm giá..."
            />
          </div>
        </div>

        <!-- Ngày bắt đầu -->
        <div class="form-field">
          <label>Ngày bắt đầu</label>
          <div class="input-inner">
            <input
                type="date"
                v-model="filters.startDate"
                @change="handleFilterChange"
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
                @change="handleFilterChange"
            />
          </div>
        </div>

        <!-- Trạng thái -->
        <div class="form-field">
          <label>Trạng thái</label>
          <select v-model="filters.trangThai" @change="handleFilterChange">
            <option value="">Tất cả trạng thái</option>
            <option value="1">Đang hoạt động</option>
            <option value="0">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <!-- Cụm nút thao tác -->
      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          <span class="btn-icon">↺</span> Đặt lại bộ lọc
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          <span>+</span> Tạo đợt giảm giá
        </button>
      </div>
    </div>

    <!-- 3. Bảng danh sách các đợt giảm giá -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <h3 class="table-title">Danh sách các đợt giảm giá</h3>
        <span class="record-count">
          Hiển thị <b>{{ campaigns.length }}</b> / <b>{{ totalElements }}</b> bản ghi
          (Trang {{ currentPage }}/{{ totalPages || 1 }})
        </span>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 150px;">Mã</th>
            <th>Tên đợt giảm giá</th>
            <th style="width: 120px;">Giá trị giảm</th>
            <th style="width: 160px;">Ngày bắt đầu</th>
            <th style="width: 160px;">Ngày kết thúc</th>
            <th style="width: 150px; text-align: center;">Trạng thái</th>
            <th style="width: 110px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <!-- Trạng thái Đang tải -->
          <tr v-if="loading">
            <td colspan="8" class="empty-cell">
              <span class="loading-spinner">⏳</span> Đang tải dữ liệu từ database SQL Server...
            </td>
          </tr>

          <!-- Trạng thái Lỗi -->
          <tr v-else-if="errorMessage">
            <td colspan="8" class="error-cell">
              {{ errorMessage }}
              <div style="margin-top: 8px;">
                <button class="btn btn-reset" style="height: 2rem; font-size: 0.85rem;" @click="fetchData">Thử lại</button>
              </div>
            </td>
          </tr>

          <!-- Trạng thái Không có dữ liệu -->
          <tr v-else-if="campaigns.length === 0">
            <td colspan="8" class="empty-cell">
              Không có đợt giảm giá nào phù hợp với bộ lọc hiện tại.
            </td>
          </tr>

          <!-- Dữ liệu thực từ Database -->
          <tr v-else v-for="(item, index) in campaigns" :key="item.id">
            <td style="text-align: center;" class="text-muted">
              {{ (currentPage - 1) * pageSize + index + 1 }}
            </td>
            <td class="font-bold text-blue">{{ item.maDotGiamGia || '—' }}</td>
            <td class="font-medium text-title">{{ item.tenDotGiamGia || '—' }}</td>
            <td class="font-bold text-dark">
              {{ item.phanTramGiam != null ? item.phanTramGiam + '%' : '0%' }}
            </td>
            <td class="text-date">{{ formatDate(item.ngayBatDau) }}</td>
            <td class="text-date">{{ formatDate(item.ngayKetThuc) }}</td>

            <!-- Trạng thái: Đang hoạt động / Ngừng hoạt động -->
            <td style="text-align: center;">
              <span
                  class="badge-status"
                  :class="item.trangThai === 1 ? 'status-active' : 'status-inactive'"
              >
                {{ item.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
              </span>
            </td>

            <!-- Cột Hành động: Bật/tắt trạng thái và Xem chi tiết -->
            <td style="text-align: center;">
              <div class="action-buttons">
                <button
                    class="btn-circle-action"
                    :title="item.trangThai === 1 ? 'Chuyển sang Ngừng hoạt động' : 'Kích hoạt lại'"
                    @click="toggleStatus(item)"
                >
                  <span class="icon-power">⏻</span>
                </button>
                <button class="btn-circle-action" title="Xem chi tiết" @click="viewDetail(item)">
                  <span class="icon-eye">👁</span>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang động -->
      <div class="pagination-footer">
        <div class="page-size-selector">
          <label class="page-size-label">Số dòng/trang:</label>
          <select v-model="pageSize" @change="onPageSizeChange">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </div>

        <div class="pagination-controls" v-if="totalPages > 0">
          <button
              class="pg-btn"
              :disabled="currentPage <= 1 || loading"
              @click="goToPage(1)"
              title="Trang đầu"
          >⇤</button>
          <button
              class="pg-btn"
              :disabled="currentPage <= 1 || loading"
              @click="goToPage(currentPage - 1)"
              title="Trang trước"
          >‹</button>

          <button
              v-for="p in visiblePages"
              :key="p"
              class="pg-btn"
              :class="{ active: p === currentPage }"
              :disabled="loading"
              @click="goToPage(p)"
          >
            {{ p }}
          </button>

          <button
              class="pg-btn"
              :disabled="currentPage >= totalPages || loading"
              @click="goToPage(currentPage + 1)"
              title="Trang sau"
          >›</button>
          <button
              class="pg-btn"
              :disabled="currentPage >= totalPages || loading"
              @click="goToPage(totalPages)"
              title="Trang cuối"
          >⇥</button>
        </div>
      </div>
    </div>

    <!-- Modal Xem Chi Tiết -->
    <div v-if="showDetailModal" class="modal-backdrop" @click.self="showDetailModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">Chi tiết đợt giảm giá</h4>
          <button class="modal-close-btn" @click="showDetailModal = false">✕</button>
        </div>
        <div class="modal-body" v-if="selectedItem">
          <div class="detail-row">
            <span class="detail-label">Mã đợt giảm giá:</span>
            <span class="detail-value font-bold text-blue">{{ selectedItem.maDotGiamGia }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Tên đợt:</span>
            <span class="detail-value font-medium">{{ selectedItem.tenDotGiamGia }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Phần trăm giảm:</span>
            <span class="detail-value font-bold">{{ selectedItem.phanTramGiam }}%</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Ngày bắt đầu:</span>
            <span class="detail-value">{{ formatDate(selectedItem.ngayBatDau) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Ngày kết thúc:</span>
            <span class="detail-value">{{ formatDate(selectedItem.ngayKetThuc) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Trạng thái:</span>
            <span
                class="badge-status"
                :class="selectedItem.trangThai === 1 ? 'status-active' : 'status-inactive'"
            >
              {{ selectedItem.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
            </span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-reset" @click="showDetailModal = false">Đóng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api.js'

// Dữ liệu danh sách lấy từ Database
const campaigns = ref([])
const loading = ref(false)
const errorMessage = ref('')

// Phân trang
const currentPage = ref(1) // 1-indexed cho UI
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)

// Modal chi tiết
const showDetailModal = ref(false)
const selectedItem = ref(null)

// Bộ lọc
const filters = ref({
  keyword: '',
  startDate: '',
  endDate: '',
  trangThai: ''
})

// Debounce timer cho tìm kiếm từ khóa
let searchTimeout = null

/* ==========================================================
   HÀM GỌI API LẤY DỮ LIỆU TỪ DATABASE QUA SPRING BOOT
========================================================== */
const fetchData = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    // Tham số gửi lên Backend
    const params = {
      page: currentPage.value - 1, // Spring Boot Pageable là 0-indexed
      size: pageSize.value,
      sortBy: 'id',
      sortDir: 'desc'
    }

    if (filters.value.keyword && filters.value.keyword.trim() !== '') {
      params.keyword = filters.value.keyword.trim()
    }
    if (filters.value.startDate) {
      params.startDate = filters.value.startDate
    }
    if (filters.value.endDate) {
      params.endDate = filters.value.endDate
    }
    if (filters.value.trangThai !== '' && filters.value.trangThai !== null) {
      params.trangThai = Number(filters.value.trangThai)
    }

    const response = await api.get('/api/dot-giam-gia', { params })

    // Gán dữ liệu trả về từ Spring Boot Page<DotGiamGia>
    const data = response.data
    campaigns.value = data.content || []
    totalPages.value = data.totalPages || 0
    totalElements.value = data.totalElements || 0

  } catch (error) {
    console.error('Lỗi khi tải dữ liệu từ database:', error)
    errorMessage.value = 'Không thể tải dữ liệu đợt giảm giá từ database. Vui lòng kiểm tra backend và kết nối SQL Server!'
    campaigns.value = []
    totalPages.value = 0
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

// Xử lý khi gõ tìm kiếm (debounce 350ms)
const handleSearchInput = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    fetchData()
  }, 350)
}

// Xử lý khi thay đổi ngày hoặc trạng thái
const handleFilterChange = () => {
  currentPage.value = 1
  fetchData()
}

// Đặt lại bộ lọc
const resetFilters = () => {
  filters.value = {
    keyword: '',
    startDate: '',
    endDate: '',
    trangThai: ''
  }
  currentPage.value = 1
  fetchData()
}

// Chuyển trang
const goToPage = (page) => {
  if (page < 1 || (totalPages.value > 0 && page > totalPages.value)) return
  currentPage.value = page
  fetchData()
}

// Thay đổi số dòng trên trang
const onPageSizeChange = () => {
  currentPage.value = 1
  fetchData()
}

// Tính danh sách các trang hiển thị (tối đa 5 nút trang quanh trang hiện tại)
const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  if (total <= 0) return []

  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  if (end - start < 4) {
    start = Math.max(1, end - 4)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Định dạng ngày giờ hiển thị kiểu Việt Nam
const formatDate = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  if (isNaN(date.getTime())) return val
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month}/${year} ${hours}:${minutes}`
}

// Bật/tắt trạng thái (gọi API cập nhật vào SQL Server)
const toggleStatus = async (item) => {
  const nextStatus = item.trangThai === 1 ? 'Ngừng hoạt động' : 'Đang hoạt động'
  const confirmMsg = `Bạn có chắc muốn đổi trạng thái đợt giảm giá "${item.tenDotGiamGia || item.maDotGiamGia}" sang [${nextStatus}]?`
  if (!confirm(confirmMsg)) return

  try {
    const res = await api.put(`/api/dot-giam-gia/${item.id}/toggle-status`)
    item.trangThai = res.data.trangThai
  } catch (error) {
    console.error('Lỗi khi đổi trạng thái:', error)
    alert('Không thể đổi trạng thái đợt giảm giá!')
  }
}

// Xem chi tiết
const viewDetail = (item) => {
  selectedItem.value = item
  showDetailModal.value = true
}

// Mở modal tạo mới
const openCreateModal = () => {
  alert('Chức năng tạo mới đợt giảm giá!')
}

// Khởi chạy khi component được mount
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* Khung bao ngoài chuẩn độ đệm padding, không bị tràn hay dính sát mép */
.dot-giam-gia-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header breadcrumb cách lề chuẩn */
.breadcrumb-header {
  margin-bottom: 1.2rem;
  display: flex;
  align-items: center;
}

.breadcrumb-text {
  font-size: 0.95rem;
  color: #8c9597;
}

.breadcrumb-text b {
  color: var(--blue, #496883);
  font-weight: 700;
}

.slash {
  margin: 0 6px;
  color: #d8d4c9;
}

/* 2. Thẻ Card trắng chuẩn bo góc và bóng đổ */
.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  margin-bottom: 1.25rem;
}

/* Tiêu đề Bộ lọc */
.card-header-filter {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.2rem;
}

.filter-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background-color: #f7eee1;
  display: grid;
  place-items: center;
}

.filter-icon {
  font-size: 1.2rem;
  color: #b18b52;
}

.filter-title-wrap {
  display: flex;
  flex-direction: column;
}

.filter-title {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

.filter-subtitle {
  font-size: 0.85rem;
  color: #8c9597;
  margin: 2px 0 0 0;
}

/* Lưới 4 ô lọc ngang đều đặn */
.filter-inputs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-field label {
  font-size: 0.88rem;
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
  left: 0.8rem;
  font-size: 0.95rem;
  color: #9aa0a0;
  pointer-events: none;
}

/* Ô input & dropdown cao 42px gõ thoáng */
.form-field input,
.form-field select {
  width: 100%;
  height: 2.6rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field .input-inner input[type="text"] {
  padding-left: 2.4rem;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.08);
}

/* Cụm nút thao tác bên dưới bên phải */
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

.btn-primary {
  background-color: var(--blue, #496883);
  border: none;
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #38536b;
}

/* 3. Bảng danh sách */
.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.1rem;
}

.table-title {
  font-size: 1.1rem;
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
  font-size: 0.95rem;
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
  padding: 1.1rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.custom-table tr:hover td {
  background-color: #fcfbf8;
}

.empty-cell {
  text-align: center;
  padding: 2.5rem !important;
  color: #8c9597;
  font-style: italic;
}

.error-cell {
  text-align: center;
  padding: 2.5rem !important;
  color: #e04f4f;
  font-weight: 500;
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

.text-date {
  color: #556268;
  font-size: 0.9rem;
}

.text-muted {
  color: #9aa0a0;
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
  background-color: #fdf0f0;
  color: #e04f4f;
}

/* Nút hành động tròn */
.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-circle-action {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #496883;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.95rem;
}

.btn-circle-action:hover {
  border-color: var(--blue, #496883);
  background-color: #eaf1f4;
  transform: scale(1.08);
}

/* 4. Phân trang */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 1.25rem;
  padding-top: 1.1rem;
  border-top: 1px dashed var(--line, #e9e5db);
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.page-size-label {
  font-size: 0.88rem;
  color: #6f7c82;
  font-weight: 600;
}

.page-size-selector select {
  height: 2.2rem;
  padding: 0 0.75rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  background-color: #fcfbf8;
  color: var(--text, #3d4a50);
  font-size: 0.9rem;
  font-weight: 600;
  outline: none;
  cursor: pointer;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.pg-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 0.5rem;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #4b585e;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.pg-btn:hover:not(:disabled) {
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
  background-color: #f4f7f9;
}

.pg-btn.active {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border-color: var(--blue, #496883);
}

.pg-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
}

/* Modal xem chi tiết */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #ffffff;
  border-radius: 12px;
  width: 90%;
  max-width: 520px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: fadeIn 0.2s ease-out;
}

.modal-header {
  padding: 1.1rem 1.4rem;
  border-bottom: 1px solid var(--line, #e9e5db);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  margin: 0;
  font-size: 1.15rem;
  color: #3c4d55;
  font-weight: 700;
}

.modal-close-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: #8c9597;
}

.modal-body {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed #f0ece3;
  padding-bottom: 0.6rem;
}

.detail-label {
  font-size: 0.92rem;
  color: #6f7c82;
}

.detail-value {
  font-size: 0.95rem;
  color: #2b383e;
}

.modal-footer {
  padding: 1rem 1.4rem;
  border-top: 1px solid var(--line, #e9e5db);
  display: flex;
  justify-content: flex-end;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1000px) {
  .filter-inputs-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 650px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }
  .pagination-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>