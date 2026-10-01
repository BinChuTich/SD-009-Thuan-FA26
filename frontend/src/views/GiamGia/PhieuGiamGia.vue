<template>
  <div class="voucher-page-wrapper">
    <!-- 1. Thanh tiêu đề trên cùng -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <span class="menu-toggle-icon">☰</span>
        <span class="breadcrumb-text">Quản lý giảm giá <span class="slash">/</span> <b>Phiếu giảm giá</b></span>
      </div>
    </div>

    <!-- 2. Khung Bộ lọc (Tone màu ấm FF T-shirt) -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-icon-box">
          <span class="filter-icon">🌪️</span>
        </div>
        <div class="filter-title-wrap">
          <h3 class="filter-title">Bộ lọc</h3>
          <p class="filter-subtitle">Tra cứu nhanh dữ liệu.</p>
        </div>
      </div>

      <!-- Lưới 6 ô lọc ngang -->
      <div class="filter-inputs-grid">
        <!-- Tìm kiếm -->
        <div class="form-field">
          <label>Tìm kiếm</label>
          <div class="input-inner">
            <span class="prefix-icon">🔍</span>
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
          <select v-model="filters.hinhThuc">
            <option value="">Tất cả hình thức</option>
            <option value="Công khai">Công khai</option>
            <option value="Cá nhân">Cá nhân</option>
          </select>
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
          <select v-model="filters.loaiGiam">
            <option value="">Tất cả loại giảm</option>
            <option value="percent">Giảm theo %</option>
            <option value="amount">Giảm tiền mặt</option>
          </select>
        </div>

        <!-- Trạng thái -->
        <div class="form-field">
          <label>Trạng thái</label>
          <select v-model="filters.trangThai">
            <option value="">Tất cả trạng thái</option>
            <option value="active">Đang hoạt động</option>
            <option value="inactive">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <!-- Cụm nút thao tác bên dưới bên phải -->
      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          <span class="btn-icon">↺</span> Đặt lại bộ lọc
        </button>
        <button class="btn btn-export">
          <span class="btn-icon">📥</span> Xuất Excel
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          <span>+</span> Tạo phiếu mới
        </button>
      </div>
    </div>

    <!-- 3. Khung Danh sách phiếu giảm giá -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <h3 class="table-title">Danh sách phiếu giảm giá</h3>
        <span class="record-count">{{ filteredList.length }} bản ghi hiển thị.</span>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 140px;">Mã</th>
            <th>Tên phiếu</th>
            <th style="width: 130px;">Hình thức</th>
            <th style="width: 110px;">Giá trị giảm</th>
            <th style="width: 120px;">Ngày bắt đầu</th>
            <th style="width: 120px;">Ngày kết thúc</th>
            <th style="width: 140px; text-align: center;">Trạng thái</th>
            <th style="width: 110px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, index) in filteredList" :key="item.id">
            <td style="text-align: center;" class="text-muted">{{ index + 1 }}</td>
            <td class="font-bold text-blue">{{ item.code }}</td>
            <td class="font-medium text-title">{{ item.name }}</td>

            <!-- Badge Hình thức: Công khai (Vàng cát), Cá nhân (Xanh đá) -->
            <td>
                <span :class="['badge-form', item.form === 'Công khai' ? 'badge-public' : 'badge-personal']">
                  <span class="dot-icon">●</span> {{ item.form }}
                </span>
            </td>

            <!-- Giá trị giảm -->
            <td class="font-bold text-dark">{{ item.discountValue }}</td>

            <!-- Ngày tháng -->
            <td class="text-muted-dark">{{ item.startDate }}</td>
            <td class="text-muted-dark">{{ item.endDate }}</td>

            <!-- Badge Trạng thái: Đang hoạt động -->
            <td style="text-align: center;">
                <span class="badge-status status-active">
                  {{ item.status }}
                </span>
            </td>

            <!-- Cột Hành động: Icon nút nguồn & Icon con mắt -->
            <td style="text-align: center;">
              <div class="action-buttons">
                <button class="btn-circle-action" title="Bật/Tắt hoạt động">
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
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const filters = ref({
  keyword: '',
  hinhThuc: '',
  startDate: '',
  endDate: '',
  loaiGiam: '',
  trangThai: ''
})

const vouchers = ref([
  {
    id: 1,
    code: 'VCH65FFTO',
    name: 'Giảm sốc cho bộ sưu tập Cotton Compact',
    form: 'Công khai',
    discountValue: '60%',
    startDate: '16/8/2026',
    endDate: '1/9/2026',
    status: 'Đang hoạt động'
  },
  {
    id: 2,
    code: 'VCH9OSEQJ',
    name: 'FF T-shirt Polo giảm 25%',
    form: 'Công khai',
    discountValue: '25%',
    startDate: '16/8/2026',
    endDate: '1/9/2026',
    status: 'Đang hoạt động'
  },
  {
    id: 3,
    code: 'VCH4FNGLO',
    name: 'Oversize Streetwear giảm 20%',
    form: 'Công khai',
    discountValue: '50%',
    startDate: '13/8/2026',
    endDate: '3/9/2026',
    status: 'Đang hoạt động'
  },
  {
    id: 4,
    code: 'VCHWS9DFI',
    name: 'Tri ân khách hàng thân thiết hè 2026',
    form: 'Công khai',
    discountValue: '50%',
    startDate: '12/8/2026',
    endDate: '1/9/2026',
    status: 'Đang hoạt động'
  },
  {
    id: 5,
    code: 'VOUCHER5',
    name: 'Voucher tặng sinh nhật VIP',
    form: 'Cá nhân',
    discountValue: '500.000đ',
    startDate: '1/1/2026',
    endDate: '31/12/2026',
    status: 'Đang hoạt động'
  }
])

const filteredList = computed(() => {
  return vouchers.value.filter(v => {
    const matchKw = !filters.value.keyword ||
        v.code.toLowerCase().includes(filters.value.keyword.toLowerCase()) ||
        v.name.toLowerCase().includes(filters.value.keyword.toLowerCase())
    const matchForm = !filters.value.hinhThuc || v.form === filters.value.hinhThuc
    return matchKw && matchForm
  })
})

const resetFilters = () => {
  filters.value = {
    keyword: '',
    hinhThuc: '',
    startDate: '',
    endDate: '',
    loaiGiam: '',
    trangThai: ''
  }
}

const openCreateModal = () => {
  alert('Mở form tạo phiếu giảm giá mới!')
}

const viewDetail = (item) => {
  alert(`Chi tiết phiếu: ${item.code} - ${item.name}`)
}
</script>

<style scoped>
.voucher-page-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Header breadcrumb */
.breadcrumb-header {
  margin-bottom: 1.2rem;
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

@media (max-width: 1200px) {
  .filter-inputs-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }
}
</style>