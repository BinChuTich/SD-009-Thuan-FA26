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
                <button class="btn-circle-action" title="Xem chi tiết" @click="viewDetail(item)">
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
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { initialsOf, genderText, joinAddress, errorMessage, timestamp, downloadBlob } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const filters = ref({ keyword: '', status: '' })
const items = ref([])
const page = ref(0)
const size = 10
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const exporting = ref(false)
const message = ref('')
const error = ref('')

const from = computed(() => (totalElements.value === 0 ? 0 : page.value * size + 1))
// Dãy số trang, có dấu ... khi nhiều trang: 1 ... 4 5 6 ... 20
const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const set = new Set([1, total, cur - 1, cur, cur + 1])
  const list = [...set].filter((n) => n >= 1 && n <= total).sort((a, b) => a - b)
  const out = []
  list.forEach((n, i) => {
    if (i > 0 && n - list[i - 1] > 1) out.push('...')
    out.push(n)
  })
  return out
})
const to = computed(() => Math.min((page.value + 1) * size, totalElements.value))

// Bộ lọc dùng chung cho cả danh sách và xuất Excel
const buildParams = () => {
  const p = {}
  if (filters.value.keyword.trim()) p.keyword = filters.value.keyword.trim()
  if (filters.value.status !== '') p.trangThai = filters.value.status
  return p
}

const fetchList = async () => {
  loading.value = true
  try {
    const { data } = await api.get('/api/khach-hang', {
      params: { ...buildParams(), page: page.value, size, sortBy: 'id', direction: 'desc' }
    })
    items.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch (e) {
    error.value = errorMessage(e, 'Không tải được danh sách khách hàng')
  } finally {
    loading.value = false
  }
}

const goPage = (p) => {
  page.value = p
  fetchList()
}

// Đổi bộ lọc -> về trang 1 (tìm kiếm chờ 300ms sau khi ngừng gõ)
let timer = null
watch(filters, () => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    page.value = 0
    fetchList()
  }, 300)
}, { deep: true })
onBeforeUnmount(() => clearTimeout(timer))

const resetFilters = () => {
  filters.value = { keyword: '', status: '' }
}

const exportExcel = async () => {
  exporting.value = true
  error.value = ''
  try {
    const res = await api.get('/api/khach-hang/export', { params: buildParams(), responseType: 'blob' })
    downloadBlob(res.data, `DanhSachKhachHang_${timestamp()}.xlsx`)
  } catch (e) {
    // responseType blob nên lỗi trả về cũng là blob -> đọc ra JSON để lấy message
    let msg = errorMessage(e, 'Xuất Excel thất bại')
    if (e?.response?.data instanceof Blob) {
      try { msg = JSON.parse(await e.response.data.text()).message || msg } catch { /* giữ msg mặc định */ }
    }
    error.value = msg
  } finally {
    exporting.value = false
  }
}

const toggleStatus = async (item) => {
  const action = item.trangThai === 1 ? 'khóa' : 'mở khóa'
  if (!confirm(`Bạn có chắc muốn ${action} khách hàng ${item.maKhachHang} - ${item.tenKhachHang}?`)) return
  try {
    await api.patch(`/api/khach-hang/${item.id}/toggle-status`)
    await fetchList()
  } catch (e) {
    error.value = errorMessage(e, 'Không đổi được trạng thái')
  }
}

const openCreate = () => router.push('/khach-hang/them')

const viewDetail = (item) => {
  alert(`Xem chi tiết khách hàng: ${item.maKhachHang} - ${item.tenKhachHang}`)
}

onMounted(async () => {
  if (route.query.created) {
    message.value = 'Đã thêm khách hàng mới thành công'
    router.replace({ path: route.path })
  }
  fetchList()
})
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
</style>