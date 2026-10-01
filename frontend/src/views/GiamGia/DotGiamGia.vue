<template>
  <div class="dot-giam-gia-wrapper">
    <!-- 1. Thanh Breadcrumb trên cùng -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <span class="menu-toggle-icon">☰</span>
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
          <p class="filter-subtitle">Tra cứu nhanh dữ liệu.</p>
        </div>
      </div>

      <!-- Lưới 4 ô lọc đúng theo mẫu ảnh -->
      <div class="filter-inputs-grid">
        <!-- Tìm kiếm -->
        <div class="form-field">
          <label>Tìm kiếm</label>
          <div class="input-inner">
            <span class="prefix-icon">🔍</span>
            <input
                type="text"
                v-model="filters.keyword"
                placeholder="Mã, tên, giá trị..."
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

        <!-- Trạng thái -->
        <div class="form-field">
          <label>Trạng thái</label>
          <select v-model="filters.status">
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
          <span class="btn-icon">📄</span> Xuất Excel
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
        <span class="record-count">{{ filteredList.length }} bản ghi hiển thị.</span>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 140px;">Mã</th>
            <th>Tên</th>
            <th style="width: 100px;">Giá trị</th>
            <th style="width: 130px;">Ngày bắt đầu</th>
            <th style="width: 130px;">Ngày kết thúc</th>
            <th style="width: 150px; text-align: center;">Trạng thái</th>
            <th style="width: 110px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, index) in filteredList" :key="item.id">
            <td style="text-align: center;" class="text-muted">{{ index + 1 }}</td>
            <td class="font-bold text-blue">{{ item.code }}</td>
            <td class="font-medium text-title">{{ item.name }}</td>
            <td class="font-bold text-dark">{{ item.discountValue }}</td>
            <td class="text-date">{{ item.startDate }}</td>
            <td class="text-date">{{ item.endDate }}</td>

            <!-- Trạng thái: Đang hoạt động / Ngừng hoạt động -->
            <td style="text-align: center;">
                <span
                    class="badge-status"
                    :class="item.status === 'active' ? 'status-active' : 'status-inactive'"
                >
                  {{ item.statusText }}
                </span>
            </td>

            <!-- Cột Hành động: Icon nút nguồn & Icon con mắt -->
            <td style="text-align: center;">
              <div class="action-buttons">
                <button class="btn-circle-action" title="Bật/Tắt hoạt động" @click="toggleStatus(item)">
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

      <!-- Phân trang dưới cùng -->
      <div class="pagination-footer">
        <div class="page-size-selector">
          <select v-model="pageSize">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
          </select>
        </div>

        <div class="pagination-controls">
          <button class="pg-btn" disabled>⇤</button>
          <button class="pg-btn" disabled>‹</button>
          <button class="pg-btn active">1</button>
          <button class="pg-btn">›</button>
          <button class="pg-btn">⇥</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const pageSize = ref(5)

const filters = ref({
  keyword: '',
  startDate: '',
  endDate: '',
  status: ''
})

const campaigns = ref([
  {
    id: 1,
    code: 'DGGRYCXHG',
    name: 'New Balance Back To School',
    discountValue: '20%',
    startDate: '16/8/2026',
    endDate: '31/8/2026',
    status: 'active',
    statusText: 'Đang hoạt động'
  },
  {
    id: 2,
    code: 'DGGFQ1OAF',
    name: 'Puma Running giảm 20%',
    discountValue: '20%',
    startDate: '13/8/2026',
    endDate: '31/8/2026',
    status: 'active',
    statusText: 'Đang hoạt động'
  },
  {
    id: 3,
    code: 'DGGAG86WH',
    name: 'Air Jordan 1 Low G - Summer Sale',
    discountValue: '30%',
    startDate: '12/8/2026',
    endDate: '31/8/2026',
    status: 'inactive',
    statusText: 'Ngừng hoạt động'
  },
  {
    id: 4,
    code: 'DGG_WINTER',
    name: 'Sale mùa đông',
    discountValue: '20%',
    startDate: '1/11/2025',
    endDate: '31/12/2026',
    status: 'active',
    statusText: 'Đang hoạt động'
  },
  {
    id: 5,
    code: 'DGG_SUMMER',
    name: 'Siêu sale mùa hè',
    discountValue: '15%',
    startDate: '1/6/2025',
    endDate: '31/12/2026',
    status: 'active',
    statusText: 'Đang hoạt động'
  }
])

const filteredList = computed(() => {
  return campaigns.value.filter(item => {
    const matchKw = !filters.value.keyword ||
        item.code.toLowerCase().includes(filters.value.keyword.toLowerCase()) ||
        item.name.toLowerCase().includes(filters.value.keyword.toLowerCase()) ||
        item.discountValue.toLowerCase().includes(filters.value.keyword.toLowerCase())
    const matchStatus = !filters.value.status || item.status === filters.value.status
    return matchKw && matchStatus
  })
})

const resetFilters = () => {
  filters.value = {
    keyword: '',
    startDate: '',
    endDate: '',
    status: ''
  }
}

const toggleStatus = (item) => {
  if (item.status === 'active') {
    item.status = 'inactive'
    item.statusText = 'Ngừng hoạt động'
  } else {
    item.status = 'active'
    item.statusText = 'Đang hoạt động'
  }
}

const openCreateModal = () => {
  alert('Mở form tạo đợt giảm giá mới!')
}

const viewDetail = (item) => {
  alert(`Chi tiết đợt giảm giá: ${item.code} - ${item.name}`)
}
</script>

<style scoped>
.dot-giam-gia-wrapper {
  padding: 16px 20px 32px;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header breadcrumb */
.breadcrumb-header {
  margin-bottom: 12px;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.menu-toggle-icon {
  font-size: 14px;
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
  margin: 0 4px;
  color: #d8d4c9;
}

/* 2. Thẻ Khung nội dung */
.content-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 16px 20px;
  margin-bottom: 14px;
}

.card-header-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.filter-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background-color: #f7eee1;
  display: grid;
  place-items: center;
}

.filter-icon {
  font-size: 14px;
  color: #b18b52;
}

.filter-title-wrap {
  display: flex;
  flex-direction: column;
}

.filter-title {
  font-size: 13.5px;
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

.filter-subtitle {
  font-size: 11.5px;
  color: #8c9597;
  margin: 2px 0 0 0;
}

/* Grid 4 ô lọc ngang */
.filter-inputs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-field label {
  font-size: 11.5px;
  font-weight: 700;
  color: #556268;
}

.input-inner {
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 9px;
  font-size: 11px;
  color: #9aa0a0;
  pointer-events: none;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 35px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  padding: 0 10px;
  font-size: 12.5px;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field .input-inner input[type="text"] {
  padding-left: 28px;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

/* Cụm 3 nút thao tác */
.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  border-top: 1px dashed #efeae0;
  padding-top: 12px;
}

.btn {
  height: 33px;
  padding: 0 15px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
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

/* 3. Bảng dữ liệu */
.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.table-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
  color: #3c4d55;
}

.record-count {
  font-size: 12px;
  color: #8c9597;
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.custom-table th {
  background-color: #faf9f6;
  color: #8f9695;
  font-weight: 700;
  padding: 11px 12px;
  text-align: left;
  border-bottom: 1px solid #efede7;
}

.custom-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f0eb;
  color: #556268;
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
  color: #394a52;
}

.text-dark {
  color: #3d4a50;
}

.text-muted {
  color: #9aa0a0;
}

.text-date {
  color: #647074;
}

/* Badge Trạng thái */
.badge-status {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 12px;
}

.status-active {
  background-color: #edf6ef;
  color: #4c8a5a;
}

.status-inactive {
  background-color: #fdf0f0;
  color: #e04f4f;
}

/* Cột nút hành động */
.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-circle-action {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #647074;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}

.btn-circle-action:hover {
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
  background-color: #eaf1f4;
}

.icon-power {
  font-size: 11px;
}

.icon-eye {
  font-size: 12px;
}

/* Phân trang */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
}

.page-size-selector select {
  height: 28px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 5px;
  background-color: #ffffff;
  padding: 0 8px;
  font-size: 12px;
  color: #556268;
}

.pagination-controls {
  display: flex;
  gap: 4px;
}

.pg-btn {
  width: 26px;
  height: 26px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  display: grid;
  place-items: center;
  color: #647074;
}

.pg-btn.active {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border-color: var(--blue, #496883);
  font-weight: 700;
}

.pg-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .filter-inputs-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }
}
</style>