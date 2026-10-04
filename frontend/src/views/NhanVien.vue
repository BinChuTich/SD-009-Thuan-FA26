<template>
  <div class="employee-page-wrapper">
    <div class="breadcrumb-header">
      <div class="breadcrumb-left"><h2 class="page-title">Nhân viên</h2></div>
    </div>

    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-icon-box"><span class="filter-icon">🍸</span></div>
        <div class="filter-title-wrap"><h3 class="filter-title">Bộ lọc</h3></div>
      </div>
      <div class="filter-inputs-grid">
        <div class="form-field search-field">
          <div class="input-inner">
            <span class="prefix-icon">🔍</span>
            <input v-model="filters.keyword" type="text" placeholder="Tìm theo mã, họ tên, tài khoản, SĐT..." @input="applyFilters" />
          </div>
        </div>
        <div class="form-field">
          <select v-model="filters.role" @change="applyFilters">
            <option value="">Tất cả vai trò</option>
            <option v-for="role in roles" :key="role.id" :value="String(role.id)">{{ role.tenVaiTro }}</option>
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
        <button class="btn btn-reset" @click="resetFilters"><span class="btn-icon">↺</span> Đặt lại bộ lọc</button>
        <button class="btn btn-export" @click="exportExcel" :disabled="loading"><span class="btn-icon">📥</span> Xuất Excel</button>
        <button class="btn btn-primary" @click="openCreateModal"><span>+</span> Thêm nhân viên</button>
      </div>
    </div>

    <div class="content-card table-card">
      <div class="table-header-row">
        <div class="table-title-wrap"><span class="header-icon">👥</span><h3 class="table-title">Danh sách nhân viên</h3></div>
      </div>
      <div class="table-responsive">
        <table class="custom-table">
          <thead><tr>
            <th style="width:45px;text-align:center">STT</th><th style="width:60px;text-align:center">Ảnh</th><th style="width:105px">Mã NV</th><th style="width:150px">Họ tên</th><th style="width:170px">Email</th><th style="width:80px">Giới tính</th><th style="width:115px">SĐT</th><th>Địa chỉ</th><th style="width:110px">Vai trò</th><th style="width:110px;text-align:center">Trạng thái</th><th style="width:95px;text-align:center">Hành động</th>
          </tr></thead>
          <tbody>
          <tr v-if="loading"><td colspan="11" style="text-align:center;padding:2rem">Đang tải dữ liệu...</td></tr>
          <tr v-else-if="employees.length === 0"><td colspan="11" style="text-align:center;padding:2rem">Không có dữ liệu phù hợp.</td></tr>
          <tr v-for="(item,index) in employees" v-else :key="item.id">
            <td style="text-align:center" class="text-muted">{{ (pagination.page * pagination.size) + index + 1 }}</td>
            <td style="text-align:center"><div class="avatar-cell">
              <img v-if="item.avatar" :src="item.avatar" class="avatar-img" alt="avatar" @error="item.avatar = ''" />
              <div v-else class="avatar-placeholder">{{ item.initials }}</div>
            </div></td>
            <td class="font-bold text-blue">{{ item.code }}</td>
            <td class="font-medium text-dark">{{ item.fullName }}</td>
            <td class="text-email" :title="item.email">{{ item.email || '-' }}</td>
            <td>{{ item.gender }}</td><td class="text-dark">{{ item.phone || '-' }}</td><td class="text-address">{{ item.address || '-' }}</td>
            <td><span class="role-text" :class="{'role-admin': item.role === 'Quản trị viên'}">{{ item.role || '-' }}</span></td>
            <td style="text-align:center"><span class="badge-status" :class="item.status === 'active' ? 'status-active' : 'status-inactive'">{{ item.statusText }}</span></td>
            <td style="text-align:center"><div class="action-buttons">
              <button v-if="item.role !== 'Quản trị viên'" class="btn-circle-action" title="Khóa/Mở tài khoản" @click="toggleStatus(item)"><span class="icon-power">⏻</span></button>
              <button class="btn-circle-action" title="Xem / sửa chi tiết" @click="viewDetail(item)"><span class="icon-eye">👁</span></button>
            </div></td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination-row">
        <div class="pagination-info">Hiển thị {{ employees.length ? (pagination.page * pagination.size + 1) : 0 }} - {{ pagination.page * pagination.size + employees.length }} / {{ pagination.totalElements }} nhân viên</div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="pagination.page === 0" @click="goPage(pagination.page - 1)">‹</button>
          <button v-for="p in pageNumbers" :key="p" class="page-btn" :class="{active:p===pagination.page}" @click="goPage(p)">{{ p + 1 }}</button>
          <button class="page-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="goPage(pagination.page + 1)">›</button>
        </div>
      </div>
    </div>

    <div v-if="modal.open" class="modal-backdrop" @click.self="closeModal">
      <div class="employee-modal">
        <div class="modal-header"><h3 class="modal-title">{{ modal.editing ? 'Thông tin nhân viên' : 'Thêm nhân viên' }}</h3><button class="modal-close" @click="closeModal">✕</button></div>
        <div class="modal-body">
          <div class="modal-layout">
            <div class="content-card modal-avatar-card">
              <div v-if="modal.avatarPreview" class="modal-avatar-preview-wrap"><img :src="modal.avatarPreview" class="modal-avatar-preview" alt="avatar" /></div>
              <div v-else class="modal-avatar-placeholder">{{ modalInitials }}</div>
              <input ref="imageInput" type="file" accept="image/png,image/jpeg,image/jpg,image/gif,image/webp" @change="onImageChange" />
              <p class="avatar-hint">Chọn ảnh JPG/PNG, tối đa 5MB.</p>
            </div>
            <div>
              <div class="modal-section">
                <h4 class="modal-section-title">👤 Thông tin cơ bản</h4>
                <div class="modal-grid">
                  <div class="form-field"><label>Mã nhân viên</label><input v-model="modal.form.maNhanVien" placeholder="Tự sinh nếu bỏ trống" /></div>
                  <div class="form-field"><label>Họ và tên <span class="required">*</span></label><input v-model="modal.form.tenNhanVien" placeholder="Nhập họ và tên" /></div>
                  <div class="form-field"><label>Tài khoản <span class="required">*</span></label><input v-model="modal.form.tenTaiKhoan" placeholder="Tên đăng nhập" /></div>
                  <div class="form-field"><label>Email <span class="required">*</span></label><input v-model="modal.form.email" type="email" placeholder="email@example.com" /></div>
                  <div class="form-field"><label>Mật khẩu <span class="required">*</span></label><input v-model="modal.form.matKhau" type="password" placeholder="Nhập mật khẩu" /></div>
                  <div class="form-field"><label>Số điện thoại <span class="required">*</span></label><input v-model="modal.form.soDienThoai" placeholder="VD: 0901234567" /></div>
                  <div class="form-field"><label>Giới tính</label><select v-model="modal.form.gioiTinh"><option :value="null">-- Chọn giới tính --</option><option :value="true">Nam</option><option :value="false">Nữ</option></select></div>
                  <div class="form-field"><label>Ngày sinh</label><input v-model="modal.form.ngaySinh" type="date" /></div>
                  <div class="form-field"><label>Vai trò <span class="required">*</span></label><select v-model="modal.form.idVaiTro"><option :value="null">-- Chọn vai trò --</option><option v-for="role in roles" :key="role.id" :value="role.id">{{ role.tenVaiTro }}</option></select></div>
                  <div class="form-field"><label>Trạng thái</label><select v-model="modal.form.trangThai"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></select></div>
                </div>
              </div>
              <div class="modal-section">
                <h4 class="modal-section-title">📍 Thông tin địa chỉ</h4>
                <div class="modal-grid">
                  <div class="form-field"><label>Quê quán</label><input v-model="modal.form.queQuan" placeholder="Tỉnh/Thành phố" /></div>
                  <div class="form-field"><label>Phường/Xã</label><input v-model="modal.form.phuong" placeholder="Phường/Xã" /></div>
                  <div class="form-field full-width"><label>Địa chỉ cụ thể</label><input v-model="modal.form.diaChiCuThe" placeholder="Số nhà, tên đường..." /></div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button v-if="modal.editing" class="btn-danger" @click="deleteEmployee">Xóa</button>
          <button class="btn-secondary" @click="closeModal">Hủy</button>
          <button class="btn btn-primary" @click="saveEmployee" :disabled="saving">{{ saving ? 'Đang lưu...' : (modal.editing ? 'Lưu thay đổi' : '💾 Tạo nhân viên') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '../api'

const employees = ref([])
const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const imageInput = ref(null)
const filters = reactive({ keyword: '', role: '', status: '' })
const pagination = reactive({ page: 0, size: 10, totalPages: 0, totalElements: 0 })
const modal = reactive({ open: false, editing: false, id: null, avatarPreview: '', file: null, form: emptyForm() })

function emptyForm() {
  return { maNhanVien: '', tenTaiKhoan: '', tenNhanVien: '', matKhau: '', email: '', soDienThoai: '', anhNhanVien: '', gioiTinh: null, ngaySinh: '', queQuan: '', phuong: '', diaChiCuThe: '', idVaiTro: null, trangThai: 1 }
}

const modalInitials = computed(() => {
  const name = modal.form.tenNhanVien?.trim() || 'FF'
  const parts = name.split(/\s+/)
  return parts.length === 1 ? parts[0].slice(0,2).toUpperCase() : (parts[0][0] + parts[parts.length-1][0]).toUpperCase()
})

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
  const avatar = x.anhNhanVien ? (x.anhNhanVien.startsWith('http') ? x.anhNhanVien : `http://localhost:8080${x.anhNhanVien}`) : ''
  return { ...x, avatar, code: x.maNhanVien, fullName: x.tenNhanVien, phone: x.soDienThoai, gender, address, role: x.tenVaiTro, status: x.trangThai === 1 ? 'active' : 'inactive', statusText: x.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động', initials: getInitials(x.tenNhanVien) }
}

function getInitials(name) {
  if (!name) return 'FF'
  const p = name.trim().split(/\s+/)
  return p.length === 1 ? p[0].slice(0,2).toUpperCase() : (p[0][0] + p[p.length-1][0]).toUpperCase()
}

async function loadRoles() {
  try { roles.value = (await api.get('/api/nhan-vien/vai-tro')).data }
  catch (e) { notifyError(e) }
}

async function loadEmployees() {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size, keyword: filters.keyword || undefined, idVaiTro: filters.role || undefined, trangThai: filters.status === '' ? undefined : filters.status }
    const res = await api.get('/api/nhan-vien', { params })
    employees.value = (res.data.content || []).map(mapEmployee)
    pagination.totalPages = res.data.totalPages || 0
    pagination.totalElements = res.data.totalElements || 0
  } catch (e) { notifyError(e) }
  finally { loading.value = false }
}

let filterTimer
function applyFilters() {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(() => { pagination.page = 0; loadEmployees() }, 250)
}

function resetFilters() { filters.keyword = ''; filters.role = ''; filters.status = ''; pagination.page = 0; loadEmployees() }
function goPage(page) { if (page < 0 || page >= pagination.totalPages) return; pagination.page = page; loadEmployees() }

function openCreateModal() {
  modal.open = true; modal.editing = false; modal.id = null; modal.file = null; modal.avatarPreview = ''; modal.form = emptyForm()
  if (roles.value.length) modal.form.idVaiTro = roles.value.find(r => r.maVaiTro === 'NV')?.id ?? roles.value[0].id
}

function viewDetail(item) {
  modal.open = true; modal.editing = true; modal.id = item.id; modal.file = null; modal.avatarPreview = item.avatar || ''
  modal.form = { maNhanVien: item.maNhanVien || '', tenTaiKhoan: item.tenTaiKhoan || '', tenNhanVien: item.tenNhanVien || '', matKhau: '', email: item.email || '', soDienThoai: item.soDienThoai || '', anhNhanVien: item.anhNhanVien || '', gioiTinh: item.gioiTinh ?? null, ngaySinh: item.ngaySinh || '', queQuan: item.queQuan || '', phuong: item.phuong || '', diaChiCuThe: item.diaChiCuThe || '', idVaiTro: item.idVaiTro ?? null, trangThai: item.trangThai ?? 1 }
}

function closeModal() { if (!saving.value) modal.open = false }

function onImageChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { alert('Vui lòng chọn file ảnh.'); e.target.value = ''; return }
  if (file.size > 5 * 1024 * 1024) { alert('Ảnh không được vượt quá 5MB.'); e.target.value = ''; return }
  modal.file = file
  if (modal.avatarPreview) URL.revokeObjectURL(modal.avatarPreview)
  modal.avatarPreview = URL.createObjectURL(file)
}

async function saveEmployee() {
  const f = modal.form
  if (!f.tenNhanVien?.trim()) return alert('Vui lòng nhập họ tên nhân viên.')
  if (!f.tenTaiKhoan?.trim()) return alert('Vui lòng nhập tên tài khoản.')
  if (!modal.editing && !f.matKhau?.trim()) return alert('Vui lòng nhập mật khẩu khi tạo nhân viên.')
  if (!f.idVaiTro) return alert('Vui lòng chọn vai trò.')
  if (!f.email?.trim()) return alert('Vui lòng nhập email.')
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) return alert('Email không hợp lệ.')
  if (!f.soDienThoai?.trim()) return alert('Vui lòng nhập số điện thoại.')
  if (!/^0\d{9,10}$/.test(f.soDienThoai.trim())) return alert('Số điện thoại phải gồm 10-11 số và bắt đầu bằng 0.')
  if (f.ngaySinh && new Date(f.ngaySinh) > new Date()) return alert('Ngày sinh không được lớn hơn ngày hiện tại.')
  if (!confirm(modal.editing ? 'Bạn có chắc muốn lưu thay đổi nhân viên này?' : 'Bạn có chắc muốn tạo nhân viên mới?')) return
  saving.value = true
  try {
    const fd = new FormData()
    const data = { ...modal.form }
    if (!data.matKhau) delete data.matKhau
    fd.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
    if (modal.file) fd.append('file', modal.file)
    if (modal.editing) await api.put(`/api/nhan-vien/${modal.id}`, fd)
    else await api.post('/api/nhan-vien', fd)
    alert(modal.editing ? 'Cập nhật nhân viên thành công!' : 'Tạo nhân viên thành công!')
    modal.open = false
    await loadEmployees()
  } catch (e) { notifyError(e) }
  finally { saving.value = false }
}

async function toggleStatus(item) {
  const action = item.status === 'active' ? 'khóa' : 'mở khóa'
  if (!confirm(`Bạn có chắc muốn ${action} nhân viên ${item.fullName}?`)) return
  try {
    await api.patch(`/api/nhan-vien/${item.id}/toggle-status`)
    alert(`${action.charAt(0).toUpperCase() + action.slice(1)} nhân viên thành công!`)
    await loadEmployees()
  } catch (e) { notifyError(e) }
}

async function deleteEmployee() {
  if (!modal.id) return
  if (!confirm('Bạn có chắc muốn xóa nhân viên này? Tài khoản sẽ chuyển sang trạng thái ngừng hoạt động.')) return
  try {
    await api.delete(`/api/nhan-vien/${modal.id}`)
    alert('Xóa nhân viên thành công!')
    modal.open = false
    await loadEmployees()
  } catch (e) { notifyError(e) }
}

async function exportExcel() {
  try {
    const params = { keyword: filters.keyword || undefined, idVaiTro: filters.role || undefined, trangThai: filters.status === '' ? undefined : filters.status }
    const res = await api.get('/api/nhan-vien/export-excel', { params, responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a'); a.href = url; a.download = 'danh-sach-nhan-vien.xlsx'; a.click(); URL.revokeObjectURL(url)
    alert('Xuất Excel thành công!')
  } catch (e) { notifyError(e) }
}

function notifyError(error) {
  const message = error?.response?.data?.message || 'Không thể kết nối máy chủ hoặc thao tác thất bại.'
  alert(message)
}

onMounted(async () => { await loadRoles(); await loadEmployees() })
</script>

<style scoped>
.employee-page-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header */
.breadcrumb-header {
  margin-bottom: 1.2rem;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.menu-toggle-icon {
  font-size: 1.1rem;
  color: #8c9597;
  cursor: pointer;
}

.page-title {
  margin: 0;
  font-size: 1.25rem; /* ~20px */
  font-weight: 700;
  color: var(--blue, #496883);
}

/* 2. Thẻ Khung Card */
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
  display: grid;
  place-items: center;
}

.filter-icon {
  font-size: 1.15rem;
  color: #b18b52;
}

.filter-title {
  font-size: 1.05rem; /* ~16.8px */
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

/* Lưới lọc 3 trường ngang */
.filter-inputs-grid {
  display: grid;
  grid-template-columns: 2.2fr 1fr 1fr;
  gap: 1rem;
  margin-bottom: 1.2rem;
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
  font-size: 0.95rem;
  color: #9aa0a0;
  pointer-events: none;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 2.6rem; /* ~41.6px */
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem; /* Cũ: 12.5px -> ~15.2px */
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.search-field input {
  padding-left: 2.4rem;
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
  font-size: 0.92rem; /* Chữ nút bấm to rõ */
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

/* 3. Bảng danh sách */
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
  font-size: 1.05rem; /* ~16.8px */
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
  font-size: 0.95rem; /* Tăng cỡ chữ bảng ~15.2px */
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
  padding: 1.05rem 1rem; /* Dãn cách đều hàng */
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.custom-table tr:hover td {
  background-color: #fcfbf8;
}

/* Avatar */
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
  background-color: #eaf1f5;
  color: var(--blue, #496883);
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
  max-width: 300px;
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
  color: var(--blue, #496883);
}

/* Badge trạng thái */
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

/* Cột hành động */
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


/* Modal thêm/sửa nhân viên - dùng cùng ngôn ngữ giao diện FF T-shirt */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(35, 43, 48, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 1000;
}
.employee-modal {
  width: min(1050px, 96vw);
  max-height: 92vh;
  overflow-y: auto;
  background: #f7f5ef;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0,0,0,.2);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.35rem;
  background: #fff;
  border-bottom: 1px solid var(--line, #e9e5db);
}
.modal-title { margin: 0; color: var(--blue, #496883); font-size: 1.15rem; }
.modal-close {
  width: 34px; height: 34px; border-radius: 8px; border: 1px solid #e9e5db;
  background: #fff; color: #6f7c82; cursor: pointer; font-size: 1.1rem;
}
.modal-body { padding: 1.1rem 1.35rem; }
.modal-layout { display: grid; grid-template-columns: 220px 1fr; gap: 1.1rem; align-items: start; }
.modal-avatar-card { text-align: center; }
.modal-avatar-preview {
  width: 120px; height: 120px; border-radius: 50%; margin: 0 auto 12px;
  object-fit: cover; background: #eaf1f5; border: 2px dashed #b9cddc;
}
.modal-avatar-placeholder {
  width: 120px; height: 120px; border-radius: 50%; margin: 0 auto 12px;
  display: grid; place-items: center; background: #eaf1f5; color: var(--blue, #496883);
  border: 2px dashed #b9cddc; font-size: 2rem; font-weight: 800;
}
.modal-avatar-card input[type=file] { width: 100%; font-size: .78rem; }
.modal-section { background: #fff; border: 1px solid var(--line, #e9e5db); border-radius: 12px; padding: 1.25rem 1.4rem; }
.modal-section + .modal-section { margin-top: 1rem; }
.modal-section-title { display:flex; align-items:center; gap:.65rem; margin:0 0 1rem; color:#3e4e56; font-size:1rem; }
.modal-grid { display:grid; grid-template-columns:1fr 1fr; gap:1rem 1.15rem; }
.modal-grid .form-field.full-width { grid-column: span 2; }
.modal-footer { display:flex; justify-content:flex-end; gap:.7rem; padding: 0 1.35rem 1.25rem; }
.btn-secondary { height:2.6rem; padding:0 1.4rem; border-radius:8px; border:1px solid #dfd5c2; background:#fff8eb; color:#957b48; font-weight:700; cursor:pointer; }
.btn-danger { height:2.6rem; padding:0 1.4rem; border-radius:8px; border:1px solid #efcaca; background:#fff1f1; color:#b64d4d; font-weight:700; cursor:pointer; }
.pagination-row { display:flex; justify-content:space-between; align-items:center; gap:1rem; padding-top:1rem; }
.pagination-info { color:#7c878b; font-size:.88rem; }
.pagination-buttons { display:flex; gap:.35rem; }
.page-btn { min-width:34px; height:34px; border:1px solid var(--line,#e9e5db); background:#fff; color:#496883; border-radius:7px; cursor:pointer; }
.page-btn.active { background:#496883; color:#fff; border-color:#496883; }
.page-btn:disabled { opacity:.45; cursor:not-allowed; }
.status-inactive { background:#f7eeee; color:#a65d5d; }
.required { color:#c43e3e; }

@media (max-width: 850px) {
  .modal-layout { grid-template-columns: 1fr; }
  .modal-grid { grid-template-columns: 1fr; }
  .modal-grid .form-field.full-width { grid-column: span 1; }
}

</style>
