<template>
  <div class="customer-page-wrapper">
    <!-- 1. Thanh tiêu đề -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <h2 class="page-title">Khách hàng</h2>
      </div>
    </div>

    <div v-if="message" class="alert alert-success">
      <span>{{ message }}</span>
      <button class="alert-close" @click="message = ''">✕</button>
    </div>
    <div v-if="error" class="alert alert-error">
      <span>{{ error }}</span>
      <button class="alert-close" @click="error = ''">✕</button>
    </div>

    <!-- 2. Khung Bộ lọc -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-icon-box">
          <span class="filter-icon">🍸</span>
        </div>
        <div class="filter-title-wrap">
          <h3 class="filter-title">Bộ lọc</h3>
        </div>
      </div>

      <div class="filter-inputs-grid">
        <div class="form-field search-field">
          <div class="input-inner">
            <span class="prefix-icon">🔍</span>
            <input
                type="text"
                v-model="filters.keyword"
                placeholder="Tìm theo mã, họ tên, tài khoản, SĐT..."
            />
          </div>
        </div>


        <div class="form-field">
          <select v-model="filters.status">
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
        <button class="btn btn-primary" @click="openCreate">
          <span>+</span> Thêm khách hàng
        </button>
      </div>
    </div>

    <!-- 3. Khung Danh sách khách hàng -->
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
            <th style="width: 45px; text-align: center;">STT</th>
            <th style="width: 60px; text-align: center;">Ảnh</th>
            <th style="width: 105px;">Mã KH</th>
            <th style="width: 150px;">Họ tên</th>
            <th style="width: 170px;">Email</th>
            <th style="width: 80px;">Giới tính</th>
            <th style="width: 115px;">SĐT</th>
            <th>Địa chỉ</th>
            <th style="width: 110px; text-align: center;">Trạng thái</th>
            <th style="width: 95px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading" class="empty-row"><td colspan="10">Đang tải dữ liệu...</td></tr>
          <tr v-else-if="!items.length" class="empty-row"><td colspan="10">Không có khách hàng nào</td></tr>
          <tr v-else v-for="(item, index) in items" :key="item.id">
            <td style="text-align: center;" class="text-muted">{{ page * size + index + 1 }}</td>

            <td style="text-align: center;">
              <div class="avatar-cell">
                <div class="avatar-placeholder">{{ initialsOf(item.tenKhachHang) }}</div>
              </div>
            </td>

            <td class="font-bold text-blue">{{ item.maKhachHang }}</td>
            <td class="font-medium text-dark">{{ item.tenKhachHang }}</td>
            <td class="text-email" :title="item.email">{{ item.email || '-' }}</td>
            <td>{{ genderText(item.gioiTinh) }}</td>
            <td class="text-dark">{{ item.soDienThoai || '-' }}</td>
            <td class="text-address">{{ joinAddress(item.diaChiCuThe, item.phuong, item.huyen, item.thanhPho) || '-' }}</td>


            <td style="text-align: center;">
              <span class="badge-status" :class="item.trangThai === 1 ? 'status-active' : 'status-inactive'">
                {{ item.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
              </span>
            </td>

            <td style="text-align: center;">
              <div class="action-buttons">
                <button
                    class="btn-circle-action"
                    title="Khóa/Mở tài khoản"
                    @click="toggleStatus(item)"
                >
                  <span class="icon-power">⏻</span>
                </button>
                <button class="btn-circle-action" title="Xem chi tiết" @click="openView(item)">
                  <span class="icon-eye">👁</span>
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination" v-if="totalElements > 0">
        <span class="pagination-info">Hiển thị {{ from }} - {{ to }} / {{ totalElements }} khách hàng</span>
        <div class="pagination-buttons">
          <button class="page-btn page-arrow" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
          <template v-for="(n, i) in pageNumbers" :key="i">
            <span v-if="n === '...'" class="page-dots">…</span>
            <button v-else class="page-btn" :class="{ active: n - 1 === page }" @click="goPage(n - 1)">{{ n }}</button>
          </template>
          <button class="page-btn page-arrow" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">›</button>
        </div>
      </div>
    </div>


    <!-- Modal thêm / xem / sửa khách hàng -->
    <div v-if="modal.open" class="modal-backdrop" @click.self="closeModal">
      <div class="customer-modal">
        <div class="modal-header">
          <h3 class="modal-title">{{ modalTitle }}</h3>
          <button class="modal-close" @click="closeModal">✕</button>
        </div>

        <div class="modal-body">
          <div class="modal-avatar-card">
            <div class="modal-avatar-placeholder">{{ modalInitials }}</div>
            <div class="modal-code">{{ modal.form.maKhachHang || 'Mã sẽ tự sinh' }}</div>
            <span class="badge-status" :class="modal.form.trangThai === 1 ? 'status-active' : 'status-inactive'">
              {{ modal.form.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
            </span>
          </div>

          <div class="modal-section">
            <h4 class="modal-section-title">👤 Thông tin khách hàng</h4>
            <div class="modal-grid">
              <div class="form-field">
                <label>Mã khách hàng</label>
                <input v-model="modal.form.maKhachHang" :disabled="isReadOnly" placeholder="Tự sinh nếu bỏ trống" />
              </div>
              <div class="form-field">
                <label>Họ và tên <span class="required">*</span></label>
                <input v-model="modal.form.tenKhachHang" :disabled="isReadOnly" placeholder="Nhập họ và tên" />
              </div>
              <div class="form-field">
                <label>Tài khoản</label>
                <input v-model="modal.form.taiKhoan" :disabled="isReadOnly" placeholder="Tên đăng nhập" />
              </div>
              <div class="form-field">
                <label>Mật khẩu</label>
                <input v-model="modal.form.matKhau" :disabled="isReadOnly" type="password" placeholder="Không bắt buộc" />
              </div>
              <div class="form-field">
                <label>Email</label>
                <input v-model="modal.form.email" :disabled="isReadOnly" type="email" placeholder="email@example.com" />
              </div>
              <div class="form-field">
                <label>Số điện thoại <span class="required">*</span></label>
                <input v-model="modal.form.soDienThoai" :disabled="isReadOnly" inputmode="numeric" maxlength="11" placeholder="VD: 0901234567" />
              </div>
              <div class="form-field">
                <label>Giới tính</label>
                <select v-model="modal.form.gioiTinh" :disabled="isReadOnly">
                  <option :value="true">Nam</option>
                  <option :value="false">Nữ</option>
                </select>
              </div>
              <div class="form-field">
                <label>Ngày sinh</label>
                <input v-model="modal.form.ngaySinh" :disabled="isReadOnly" type="date" />
              </div>
              <div class="form-field">
                <label>Trạng thái</label>
                <select v-model="modal.form.trangThai" :disabled="isReadOnly">
                  <option :value="1">Hoạt động</option>
                  <option :value="0">Ngừng hoạt động</option>
                </select>
              </div>
            </div>
          </div>

          <div class="modal-section">
            <h4 class="modal-section-title">📍 Địa chỉ</h4>
            <div class="modal-grid">
              <div class="form-field"><label>Tỉnh/Thành phố</label><input v-model="modal.form.thanhPho" :disabled="isReadOnly" /></div>
              <div class="form-field"><label>Huyện</label><input v-model="modal.form.huyen" :disabled="isReadOnly" /></div>
              <div class="form-field"><label>Phường/Xã</label><input v-model="modal.form.phuong" :disabled="isReadOnly" /></div>
              <div class="form-field"><label>Địa chỉ cụ thể</label><input v-model="modal.form.diaChiCuThe" :disabled="isReadOnly" /></div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button v-if="modal.mode === 'view'" class="btn-secondary" @click="enableEdit">✏ Sửa</button>
          <button class="btn-secondary" @click="closeModal">Đóng</button>
          <button v-if="modal.mode !== 'view'" class="btn btn-primary" @click="saveCustomer" :disabled="saving">
            {{ saving ? 'Đang lưu...' : (modal.mode === 'edit' ? '💾 Lưu thay đổi' : '💾 Thêm khách hàng') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import api from '@/api'
import { initialsOf, genderText, joinAddress, errorMessage, timestamp, downloadBlob } from '@/utils/format'

const filters = ref({ keyword: '', status: '' })
const items = ref([])
const page = ref(0)
const size = 10
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')

const modal = ref({
  open: false,
  mode: 'create',
  id: null,
  form: emptyForm()
})

function emptyForm() {
  return {
    maKhachHang: '',
    taiKhoan: '',
    tenKhachHang: '',
    email: '',
    matKhau: '',
    soDienThoai: '',
    ngaySinh: '',
    gioiTinh: true,
    trangThai: 1,
    thanhPho: '',
    huyen: '',
    phuong: '',
    diaChiCuThe: ''
  }
}

const isReadOnly = computed(() => modal.value.mode === 'view')
const modalTitle = computed(() => {
  if (modal.value.mode === 'create') return 'Thêm khách hàng'
  if (modal.value.mode === 'edit') return 'Cập nhật khách hàng'
  return 'Chi tiết khách hàng'
})
const modalInitials = computed(() => initialsOf(modal.value.form.tenKhachHang || 'FF'))

const from = computed(() => (totalElements.value === 0 ? 0 : page.value * size + 1))
const to = computed(() => Math.min((page.value + 1) * size, totalElements.value))
const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const set = new Set([1, total, cur - 1, cur, cur + 1])
  const list = [...set].filter(n => n >= 1 && n <= total).sort((a, b) => a - b)
  const out = []
  list.forEach((n, i) => {
    if (i > 0 && n - list[i - 1] > 1) out.push('...')
    out.push(n)
  })
  return out
})

const buildParams = () => {
  const p = {}
  if (filters.value.keyword.trim()) p.keyword = filters.value.keyword.trim()
  if (filters.value.status !== '') p.trangThai = filters.value.status
  return p
}

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/api/khach-hang', {
      params: { ...buildParams(), page: page.value, size, sortBy: 'id', direction: 'desc' }
    })
    items.value = data.content || []
    totalElements.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
  } catch (e) {
    error.value = errorMessage(e, 'Không tải được danh sách khách hàng')
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  fetchList()
}

let timer = null
watch(filters, () => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    page.value = 0
    fetchList()
  }, 300)
}, { deep: true })
onBeforeUnmount(() => clearTimeout(timer))

function resetFilters() {
  filters.value = { keyword: '', status: '' }
}

async function exportExcel() {
  exporting.value = true
  error.value = ''
  try {
    const res = await api.get('/api/khach-hang/export-excel', {
      params: buildParams(),
      responseType: 'blob'
    })
    downloadBlob(res.data, `DanhSachKhachHang_${timestamp()}.xlsx`)
  } catch (e) {
    let msg = errorMessage(e, 'Xuất Excel thất bại')
    if (e?.response?.data instanceof Blob) {
      try { msg = JSON.parse(await e.response.data.text()).message || msg } catch {}
    }
    error.value = msg
  } finally {
    exporting.value = false
  }
}

function openCreate() {
  modal.value = { open: true, mode: 'create', id: null, form: emptyForm() }
}

function openView(item) {
  modal.value = {
    open: true,
    mode: 'view',
    id: item.id,
    form: { ...emptyForm(), ...item }
  }
}

function enableEdit() {
  modal.value.mode = 'edit'
}

function closeModal() {
  if (!saving.value) modal.value.open = false
}

function validateCustomer() {
  const f = modal.value.form
  if (!f.tenKhachHang?.trim()) return 'Họ tên khách hàng là bắt buộc.'
  if (!f.soDienThoai?.trim()) return 'Số điện thoại là bắt buộc.'
  if (!/^0\d{9,10}$/.test(f.soDienThoai.trim())) return 'Số điện thoại phải gồm 10-11 số và bắt đầu bằng 0.'
  if (f.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) return 'Email không hợp lệ.'
  if (f.ngaySinh && new Date(f.ngaySinh) > new Date()) return 'Ngày sinh không được lớn hơn ngày hiện tại.'
  return ''
}

async function saveCustomer() {
  const validation = validateCustomer()
  if (validation) return alert(validation)
  if (!confirm(modal.value.mode === 'edit' ? 'Bạn có chắc muốn lưu thay đổi khách hàng này?' : 'Bạn có chắc muốn thêm khách hàng này?')) return

  saving.value = true
  error.value = ''
  try {
    const data = { ...modal.value.form }
    if (!data.matKhau) delete data.matKhau
    if (!data.taiKhoan) delete data.taiKhoan
    if (!data.email) delete data.email

    if (modal.value.mode === 'edit') {
      await api.put(`/api/khach-hang/${modal.value.id}`, data)
      message.value = 'Cập nhật khách hàng thành công.'
    } else {
      await api.post('/api/khach-hang', data)
      message.value = 'Thêm khách hàng thành công.'
    }
    modal.value.open = false
    await fetchList()
  } catch (e) {
    error.value = errorMessage(e, 'Không thể lưu khách hàng')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(item) {
  const action = item.trangThai === 1 ? 'khóa' : 'mở khóa'
  if (!confirm(`Bạn có chắc muốn ${action} khách hàng ${item.maKhachHang} - ${item.tenKhachHang}?`)) return
  try {
    await api.patch(`/api/khach-hang/${item.id}/toggle-status`)
    await fetchList()
  } catch (e) {
    error.value = errorMessage(e, 'Không đổi được trạng thái')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.customer-page-wrapper {
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
  grid-template-columns: 2.2fr 1fr;
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

/* Trạng thái ngừng hoạt động */
.status-inactive {
  background-color: #f6eeee;
  color: #b25a5a;
}

/* Thông báo thành công / lỗi */
.alert {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px;
  padding: 0.7rem 1rem;
  font-size: 0.92rem;
  margin-bottom: 1.1rem;
}
.alert-success { background: #edf6ef; border: 1px solid #d2e5d6; color: #4c8a5a; }
.alert-error { background: #fdeeee; border: 1px solid #f3c9c9; color: #b43c3c; }
.alert-close { background: none; border: none; cursor: pointer; font-size: 1rem; color: inherit; }

.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.empty-row td {
  text-align: center;
  color: #9aa0a0;
  padding: 2rem 1rem;
}

/* Phân trang */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.1rem;
  font-size: 0.9rem;
  color: #6f7c82;
}
.pagination-buttons { display: flex; gap: 0.4rem; align-items: center; }
.page-btn {
  min-width: 2.1rem;
  height: 2.1rem;
  padding: 0 0.6rem;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background: #fff;
  color: #6f7c82;
  font-weight: 700;
  cursor: pointer;
}
.page-btn:hover:not(:disabled):not(.active) { background: #eaf1f4; border-color: var(--blue, #496883); }
.page-btn.active { background: var(--blue, #496883); border-color: var(--blue, #496883); color: #fff; }
.page-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.page-dots { color: #9aa0a0; padding: 0 0.2rem; }


.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(35, 43, 48, .48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 1000;
}
.customer-modal {
  width: min(900px, 96vw);
  max-height: 92vh;
  overflow-y: auto;
  background: #f7f5ef;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0,0,0,.2);
}
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:1rem 1.35rem; background:#fff; border-bottom:1px solid var(--line,#e9e5db); }
.modal-title { margin:0; color:var(--blue,#496883); font-size:1.15rem; }
.modal-close { width:34px; height:34px; border-radius:8px; border:1px solid #e9e5db; background:#fff; color:#6f7c82; cursor:pointer; }
.modal-body { padding:1.1rem 1.35rem; }
.modal-avatar-card { text-align:center; margin-bottom:1rem; }
.modal-avatar-placeholder { width:92px; height:92px; border-radius:50%; margin:0 auto 8px; display:grid; place-items:center; background:#eaf1f5; color:var(--blue,#496883); border:2px dashed #b9cddc; font-size:1.8rem; font-weight:800; }
.modal-code { color:#496883; font-weight:700; margin-bottom:7px; }
.modal-section { background:#fff; border:1px solid var(--line,#e9e5db); border-radius:12px; padding:1.25rem 1.4rem; }
.modal-section + .modal-section { margin-top:1rem; }
.modal-section-title { display:flex; align-items:center; gap:.65rem; margin:0 0 1rem; color:#3e4e56; font-size:1rem; }
.modal-grid { display:grid; grid-template-columns:1fr 1fr; gap:1rem 1.15rem; }
.form-field { display:flex; flex-direction:column; }
.form-field label { margin-bottom:.4rem; font-weight:700; font-size:.88rem; color:#526168; }
.form-field input, .form-field select { width:100%; height:2.55rem; box-sizing:border-box; border:1px solid #e4dfd4; border-radius:8px; padding:0 .8rem; background:#fcfbf8; color:#3d4a50; outline:none; }
.form-field input:focus, .form-field select:focus { border-color:#496883; background:#fff; }
.form-field input:disabled, .form-field select:disabled { background:#f4f2ed; color:#68757a; cursor:not-allowed; }
.required { color:#c43e3e; }
.modal-footer { display:flex; justify-content:flex-end; gap:.7rem; padding:0 1.35rem 1.25rem; }
.btn-secondary { height:2.6rem; padding:0 1.4rem; border-radius:8px; border:1px solid #dfd5c2; background:#fff8eb; color:#957b48; font-weight:700; cursor:pointer; }
@media (max-width: 750px) { .modal-grid { grid-template-columns:1fr; } }

</style>