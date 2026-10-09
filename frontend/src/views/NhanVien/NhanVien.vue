<template>
  <div class="employee-page-wrapper">
    <!-- Bộ lọc -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <h3 class="filter-title">Bộ lọc tìm kiếm</h3>
      </div>

      <div class="filter-inputs-grid">
        <div class="form-field search-field">
          <div class="input-inner">
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="Tìm theo mã, họ tên, email, SĐT..."
              @input="applyFilters"
            />
          </div>
        </div>

        <div class="form-field">
          <select v-model="filters.role" @change="applyFilters">
            <option value="">Tất cả chức vụ</option>
            <option v-for="role in roles" :key="role.id" :value="String(role.id)">
              {{ role.tenVaiTro }}
            </option>
          </select>
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
          Đặt lại bộ lọc
        </button>
        <button class="btn btn-export" @click="exportExcel" :disabled="loading">
          Xuất Excel
        </button>
        <button class="btn btn-primary" @click="goToCreate">
          + Thêm nhân viên
        </button>
      </div>
    </div>

    <!-- Bảng danh sách -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <div class="table-title-wrap">
          <h3 class="table-title">Danh sách nhân viên</h3>
        </div>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
            <tr>
              <th style="width: 50px; text-align: center">STT</th>
              <th style="width: 65px; text-align: center">ẢNH</th>
              <th style="width: 110px">MÃ NV</th>
              <th style="width: 160px">HỌ TÊN</th>
              <th style="width: 120px">SĐT</th>
              <th style="width: 180px">EMAIL</th>
              <th>ĐỊA CHỈ</th>
              <th style="width: 120px">CHỨC VỤ</th>
              <th style="width: 120px; text-align: center">TRẠNG THÁI</th>
              <th style="width: 100px; text-align: center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="10" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Đang tải dữ liệu nhân viên...
              </td>
            </tr>
            <tr v-else-if="employees.length === 0">
              <td colspan="10" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Không tìm thấy nhân viên nào phù hợp.
              </td>
            </tr>
            <tr v-for="(item, index) in employees" v-else :key="item.id">
              <td style="text-align: center" class="text-muted">
                {{ (pagination.page * pagination.size) + index + 1 }}
              </td>
              <td style="text-align: center">
                <div class="avatar-cell">
                  <img v-if="item.avatar" :src="item.avatar" class="avatar-img" alt="avatar" @error="item.avatar = ''" />
                  <div v-else class="avatar-placeholder">{{ item.initials }}</div>
                </div>
              </td>
              <td class="font-bold text-blue">{{ item.code }}</td>
              <td class="font-medium text-dark">{{ item.fullName }}</td>
              <td class="text-dark">{{ item.phone || '-' }}</td>
              <td class="text-email" :title="item.email">{{ item.email || '-' }}</td>
              <td class="text-address">{{ item.address || '-' }}</td>
              <td>
                <span class="role-text" :class="{'role-admin': item.role === 'Quản trị viên'}">
                  {{ item.role || '-' }}
                </span>
              </td>
              <td style="text-align: center">
                <span class="badge-status" :class="item.status === 'active' ? 'status-active' : 'status-inactive'">
                  {{ item.statusText }}
                </span>
              </td>
              <td style="text-align: center">
                <div class="action-buttons">
                  <label
                    v-if="item.role !== 'Quản trị viên'"
                    class="switch-toggle"
                    :title="item.status === 'active' ? 'Đang hoạt động (nhấn để tắt)' : 'Đang ngừng hoạt động (nhấn để bật)'"
                  >
                    <input
                      type="checkbox"
                      :checked="item.status === 'active'"
                      @click.prevent="toggleStatus(item)"
                    />
                    <span class="slider-round"></span>
                  </label>
                  <button
                    class="btn-circle-action btn-edit"
                    title="Chỉnh sửa nhân viên"
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

      <div class="pagination-row" v-if="pagination.totalElements > 0">
        <div class="pagination-info">
          Hiển thị {{ employees.length ? (pagination.page * pagination.size + 1) : 0 }} - {{ pagination.page * pagination.size + employees.length }} / {{ pagination.totalElements }} nhân viên
        </div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="pagination.page === 0" @click="goPage(pagination.page - 1)">‹</button>
          <button
            v-for="p in pageNumbers"
            :key="p"
            class="page-btn"
            :class="{ active: p === pagination.page }"
            @click="goPage(p)"
          >
            {{ p + 1 }}
          </button>
          <button class="page-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="goPage(pagination.page + 1)">›</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'

const router = useRouter()
const employees = ref([])
const roles = ref([])
const loading = ref(false)
const filters = reactive({ keyword: '', role: '', status: '' })
const pagination = reactive({ page: 0, size: 5, totalPages: 0, totalElements: 0 })

const pageNumbers = computed(() => {
  const total = pagination.totalPages
  if (!total) return []
  const start = Math.max(0, pagination.page - 2)
  const end = Math.min(total, start + 5)
  return Array.from({ length: end - start }, (_, i) => start + i)
})

function mapEmployee(x) {
  const gender = x.gioiTinh === true ? 'Nam' : x.gioiTinh === false ? 'Nữ' : '-'
  const address = [x.queQuan, x.phuong, x.diaChiCuThe].filter(Boolean).join(', ')
  const avatar = x.anhNhanVien
    ? (x.anhNhanVien.startsWith('http') ? x.anhNhanVien : `http://localhost:8080${x.anhNhanVien}`)
    : ''
  return {
    ...x,
    avatar,
    code: x.maNhanVien,
    fullName: x.tenNhanVien,
    phone: x.soDienThoai,
    gender,
    address,
    role: x.tenVaiTro,
    status: x.trangThai === 1 ? 'active' : 'inactive',
    statusText: x.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động',
    initials: getInitials(x.tenNhanVien)
  }
}

function getInitials(name) {
  if (!name) return 'FF'
  const p = name.trim().split(/\s+/)
  return p.length === 1 ? p[0].slice(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase()
}

async function loadRoles() {
  try {
    const res = await api.get('/api/nhan-vien/vai-tro')
    roles.value = (res.data || []).filter(r =>
      r.maVaiTro === 'ADMIN' || r.maVaiTro === 'NV' ||
      r.tenVaiTro === 'Quản trị viên' || r.tenVaiTro === 'Nhân viên'
    )
  } catch (e) {
    showAlert({
      title: 'Lỗi tải danh mục vai trò',
      message: e?.response?.data?.message || 'Không thể tải danh sách vai trò từ máy chủ.',
      type: 'error'
    })
  }
}

async function loadEmployees() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
      idVaiTro: filters.role || undefined,
      trangThai: filters.status === '' ? undefined : filters.status
    }
    const res = await api.get('/api/nhan-vien', { params })
    employees.value = (res.data.content || []).map(mapEmployee)
    pagination.totalPages = res.data.totalPages || 0
    pagination.totalElements = res.data.totalElements || 0
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
    pagination.page = 0
    loadEmployees()
  }, 250)
}

function resetFilters() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  pagination.page = 0
  loadEmployees()
}

function goPage(page) {
  if (page < 0 || page >= pagination.totalPages) return
  pagination.page = page
  loadEmployees()
}

function goToCreate() {
  router.push('/nhan-vien/them')
}

function goToEdit(id) {
  router.push(`/nhan-vien/${id}`)
}

async function toggleStatus(item) {
  const isLocking = item.status === 'active'
  const actionText = isLocking ? 'khóa tài khoản' : 'mở khóa tài khoản'
  
  const confirmed = await showConfirm({
    title: isLocking ? 'Xác nhận khóa tài khoản' : 'Xác nhận mở khóa tài khoản',
    message: `Bạn có chắc chắn muốn ${actionText} của nhân viên "${item.fullName}" (${item.code}) không?`,
    type: isLocking ? 'warning' : 'question',
    confirmText: isLocking ? 'Khóa tài khoản' : 'Mở khóa'
  })

  if (!confirmed) return

  try {
    await api.patch(`/api/nhan-vien/${item.id}/toggle-status`)
    item.status = isLocking ? 'inactive' : 'active'
    item.statusText = isLocking ? 'Ngừng hoạt động' : 'Hoạt động'
    item.trangThai = isLocking ? 0 : 1
    showToast(`${isLocking ? 'Khóa' : 'Mở khóa'} nhân viên thành công!`, 'success')
  } catch (e) {
    showAlert({
      title: 'Thao tác thất bại',
      message: e?.response?.data?.message || 'Không thể thay đổi trạng thái nhân viên.',
      type: 'error'
    })
  }
}

async function exportExcel() {
  try {
    const params = {
      keyword: filters.keyword || undefined,
      idVaiTro: filters.role || undefined,
      trangThai: filters.status === '' ? undefined : filters.status
    }
    const res = await api.get('/api/nhan-vien/export-excel', { params, responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'danh-sach-nhan-vien.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    showToast('Xuất danh sách nhân viên ra Excel thành công!', 'success')
  } catch (e) {
    showAlert({
      title: 'Lỗi xuất Excel',
      message: e?.response?.data?.message || 'Không thể tạo file Excel.',
      type: 'error'
    })
  }
}

onMounted(async () => {
  await loadRoles()
  await loadEmployees()
})
</script>

<style scoped>
.employee-page-wrapper {
  padding: 0.5rem 2.2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
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
  grid-template-columns: 2.2fr 1fr 1fr;
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
  color: #8c979a;
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
  background-color: #3b596a;
  border: 1px solid #324b5a;
  border-radius: 9999px;
  color: #ffffff;
  padding: 0 1.4rem;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
  transition: all 0.2s ease;
}
.btn-primary:hover {
  background-color: #2c4350;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
  transform: translateY(-1px);
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
  background-color: #faf8f5;
  color: #4a5568;
  font-weight: 700;
  text-transform: uppercase;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  padding: 0.95rem 1rem;
  text-align: left;
  border-bottom: 2px solid #e9e5db;
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

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #f0f0f0;
}

.avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f0e6dc;
  color: #8c6a46;
  font-size: 0.85rem;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.font-bold {
  font-weight: 700;
}

.font-medium {
  font-weight: 600;
}

.text-blue {
  color: #8c6a46;
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
  max-width: 280px;
}

.text-muted {
  color: #9aa0a0;
}

.role-text {
  font-size: 0.92rem;
  color: #4f5d63;
}

.role-admin {
  font-weight: 700;
  color: #8c6a46;
}

.badge-status {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.32rem 0.85rem;
  border-radius: 9999px;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.status-active {
  background-color: #eaf5ed;
  color: #247541;
  border: 1px solid #bce1c7;
}

.status-inactive {
  background-color: #f7edeb;
  color: #b34a3b;
  border: 1px solid #ecc9c3;
}

.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

/* Switch Toggle */
.switch-toggle {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
  cursor: pointer;
  margin: 0;
  flex-shrink: 0;
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
  background-color: #b38e6e;
}

.switch-toggle input:checked + .slider-round::before {
  transform: translateX(16px);
}

.btn-circle-action {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid #d8e2e8;
  background-color: #f7f5ee;
  color: #496883;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-circle-action svg {
  display: block;
}

.btn-edit:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
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

@media (max-width: 900px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
