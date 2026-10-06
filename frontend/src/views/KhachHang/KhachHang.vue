<template>
  <div class="customer-page-wrapper">
    <!-- Header -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <h2 class="page-title">Quản lý khách hàng</h2>
      </div>
    </div>

    <!-- Khung Bộ lọc -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-icon-box">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"/>
          </svg>
        </div>
        <div class="filter-title-wrap">
          <h3 class="filter-title">Bộ lọc tìm kiếm</h3>
        </div>
      </div>

      <div class="filter-inputs-grid">
        <div class="form-field search-field">
          <div class="input-inner">
            <span class="prefix-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
              </svg>
            </span>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="Tìm theo mã, họ tên, email, SĐT..."
              @input="applyFilters"
            />
          </div>
        </div>

        <div class="form-field">
          <select v-model="filters.status" @change="applyFilters">
            <option value="">Tất cả trạng thái</option>
            <option value="1">Hoạt động</option>
            <option value="0">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          <span class="btn-icon">↺</span> Đặt lại bộ lọc
        </button>
        <button class="btn btn-export" :disabled="exporting" @click="exportExcel">
          <span class="btn-icon">📥</span> {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
        </button>
        <button class="btn btn-primary" @click="goToCreate">
          <span>+</span> Thêm khách hàng
        </button>
      </div>
    </div>

    <!-- Khung Danh sách khách hàng -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <div class="table-title-wrap">
          <span class="header-icon">👥</span>
          <h3 class="table-title">Danh sách khách hàng</h3>
        </div>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
            <tr>
              <th style="width: 50px; text-align: center;">STT</th>
              <th style="width: 65px; text-align: center;">Ảnh</th>
              <th style="width: 110px;">Mã KH</th>
              <th style="width: 160px;">Họ tên</th>
              <th style="width: 180px;">Email</th>
              <th style="width: 85px;">Giới tính</th>
              <th style="width: 120px;">SĐT</th>
              <th>Địa chỉ</th>
              <th style="width: 120px; text-align: center;">Trạng thái</th>
              <th style="width: 100px; text-align: center;">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="10" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Đang tải dữ liệu khách hàng...
              </td>
            </tr>
            <tr v-else-if="!items.length">
              <td colspan="10" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Không tìm thấy khách hàng nào phù hợp.
              </td>
            </tr>
            <tr v-for="(item, index) in items" v-else :key="item.id">
              <td style="text-align: center;" class="text-muted">
                {{ page * size + index + 1 }}
              </td>
              <td style="text-align: center;">
                <div class="avatar-cell">
                  <img
                    v-if="item.anhKhachHang"
                    :src="item.anhKhachHang.startsWith('http') ? item.anhKhachHang : `http://localhost:8080${item.anhKhachHang}`"
                    class="avatar-img"
                    alt="avatar"
                    @error="item.anhKhachHang = ''"
                  />
                  <div v-else class="avatar-placeholder">{{ initialsOf(item.tenKhachHang) }}</div>
                </div>
              </td>
              <td class="font-bold text-blue">{{ item.maKhachHang }}</td>
              <td class="font-medium text-dark">{{ item.tenKhachHang }}</td>
              <td class="text-email" :title="item.email">{{ item.email || '-' }}</td>
              <td>{{ genderText(item.gioiTinh) }}</td>
              <td class="text-dark">{{ item.soDienThoai || '-' }}</td>
              <td class="text-address">{{ formatAddress(item) || '-' }}</td>
              <td style="text-align: center;">
                <span
                  class="badge-status"
                  :class="item.trangThai === 1 ? 'status-active' : 'status-inactive'"
                >
                  {{ item.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
                </span>
              </td>
              <td style="text-align: center;">
                <div class="action-buttons">
                  <button
                    class="btn-circle-action btn-power"
                    :title="item.trangThai === 1 ? 'Khóa tài khoản' : 'Mở khóa tài khoản'"
                    @click="toggleStatus(item)"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                      <line x1="12" y1="2" x2="12" y2="12"></line>
                    </svg>
                  </button>
                  <button
                    class="btn-circle-action btn-edit"
                    title="Chỉnh sửa khách hàng"
                    @click="goToEdit(item.id)"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

      <div class="pagination-row" v-if="totalElements > 0">
        <span class="pagination-info">
          Hiển thị {{ items.length ? (page * size + 1) : 0 }} - {{ page * size + items.length }} / {{ totalElements }} khách hàng
        </span>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
          <button
            v-for="n in pageNumbers"
            :key="n"
            class="page-btn"
            :class="{ active: n === page }"
            @click="goPage(n)"
          >
            {{ n + 1 }}
          </button>
          <button class="page-btn" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">›</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'

const router = useRouter()
const filters = ref({ keyword: '', status: '' })
const items = ref([])
const page = ref(0)
const size = 5
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const exporting = ref(false)

const pageNumbers = computed(() => {
  const total = totalPages.value
  if (!total) return []
  const start = Math.max(0, page.value - 2)
  const end = Math.min(total, start + 5)
  return Array.from({ length: end - start }, (_, i) => start + i)
})

function initialsOf(name) {
  if (!name) return 'KH'
  const p = name.trim().split(/\s+/)
  return p.length === 1 ? p[0].slice(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase()
}

function genderText(g) {
  if (g === true) return 'Nam'
  if (g === false) return 'Nữ'
  return '-'
}

function formatAddress(item) {
  return [item.diaChiCuThe, item.phuong, item.huyen, item.thanhPho].filter(Boolean).join(', ')
}

async function loadCustomers() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size,
      keyword: filters.value.keyword || undefined,
      trangThai: filters.value.status === '' ? undefined : filters.value.status
    }
    const res = await api.get('/api/khach-hang', { params })
    items.value = res.data.content || []
    totalElements.value = res.data.totalElements || 0
    totalPages.value = res.data.totalPages || 0
  } catch (e) {
    showAlert({
      title: 'Lỗi tải dữ liệu',
      message: e?.response?.data?.message || 'Không thể kết nối đến máy chủ backend.',
      type: 'error'
    })
  } finally {
    loading.value = false
  }
}

let filterTimer
function applyFilters() {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(() => {
    page.value = 0
    loadCustomers()
  }, 250)
}

function resetFilters() {
  filters.value.keyword = ''
  filters.value.status = ''
  page.value = 0
  loadCustomers()
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadCustomers()
}

function goToCreate() {
  router.push('/khach-hang/them')
}

function goToEdit(id) {
  router.push(`/khach-hang/${id}`)
}

async function toggleStatus(item) {
  const isLocking = item.trangThai === 1
  const actionText = isLocking ? 'khóa tài khoản' : 'mở khóa tài khoản'

  const confirmed = await showConfirm({
    title: isLocking ? 'Xác nhận khóa tài khoản' : 'Xác nhận mở khóa tài khoản',
    message: `Bạn có chắc chắn muốn ${actionText} của khách hàng "${item.tenKhachHang}" (${item.maKhachHang}) không?`,
    type: isLocking ? 'warning' : 'question',
    confirmText: isLocking ? 'Khóa tài khoản' : 'Mở khóa'
  })

  if (!confirmed) return

  try {
    await api.patch(`/api/khach-hang/${item.id}/toggle-status`)
    showToast(`${isLocking ? 'Khóa' : 'Mở khóa'} khách hàng thành công!`, 'success')
    await loadCustomers()
  } catch (e) {
    showAlert({
      title: 'Thao tác thất bại',
      message: e?.response?.data?.message || 'Không thể thay đổi trạng thái khách hàng.',
      type: 'error'
    })
  }
}

async function exportExcel() {
  exporting.value = true
  try {
    const params = {
      keyword: filters.value.keyword || undefined,
      trangThai: filters.value.status === '' ? undefined : filters.value.status
    }
    const res = await api.get('/api/khach-hang/export-excel', { params, responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'danh-sach-khach-hang.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    showToast('Xuất danh sách khách hàng ra Excel thành công!', 'success')
  } catch (e) {
    showAlert({
      title: 'Lỗi xuất Excel',
      message: e?.response?.data?.message || 'Không thể tạo file Excel.',
      type: 'error'
    })
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadCustomers()
})
</script>

<style scoped>
.customer-page-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.breadcrumb-header {
  margin-bottom: 1.2rem;
}

.page-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--blue, #496883);
}

.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  margin-bottom: 1.25rem;
}

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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #b18b52;
  flex-shrink: 0;
}

.filter-icon-box svg {
  display: block;
}

.filter-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: #43545c;
  line-height: 1;
}

.filter-inputs-grid {
  display: grid;
  grid-template-columns: 2.5fr 1fr;
  gap: 1rem;
  margin-bottom: 1.2rem;
  align-items: center;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.input-inner {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.prefix-icon {
  position: absolute;
  left: 0.85rem;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #9aa0a0;
  pointer-events: none;
  z-index: 1;
}

.prefix-icon svg {
  display: block;
  width: 18px;
  height: 18px;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 2.6rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.search-field input {
  padding-left: 2.5rem;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

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

.table-header-row {
  margin-bottom: 1.1rem;
}

.table-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.header-icon {
  font-size: 1.15rem;
}

.table-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: #3c4d55;
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
  padding: 1.05rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.custom-table tr:hover td {
  background-color: #fcfbf8;
}

.avatar-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #eaf1f5;
  color: var(--blue, #496883);
  font-size: 0.85rem;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #d8e2e6;
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
  letter-spacing: 0.4px;
}

.text-dark {
  color: #2b383e;
}

.text-email {
  color: #556268;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-address {
  color: #556268;
  line-height: 1.45;
  max-width: 320px;
}

.text-muted {
  color: #9aa0a0;
}

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
  background-color: #f7eeee;
  color: #a65d5d;
}

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
  color: #556268;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-circle-action svg {
  display: block;
}

.btn-power:hover {
  border-color: #e09f3e;
  background-color: #fff8eb;
  color: #b57a1b;
  transform: scale(1.08);
}

.btn-edit:hover {
  border-color: #496883;
  background-color: #eef4f8;
  color: #38536b;
  transform: scale(1.08);
}

.btn-view:hover {
  border-color: #627d98;
  background-color: #f1f5f9;
  color: #243b53;
  transform: scale(1.08);
}

.pagination-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding-top: 1.1rem;
}

.pagination-info {
  color: #7c878b;
  font-size: 0.88rem;
}

.pagination-buttons {
  display: flex;
  gap: 0.35rem;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  border: 1px solid var(--line, #e9e5db);
  background: #fff;
  color: #496883;
  border-radius: 7px;
  cursor: pointer;
  font-weight: 600;
}

.page-btn.active {
  background: #496883;
  color: #fff;
  border-color: #496883;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
