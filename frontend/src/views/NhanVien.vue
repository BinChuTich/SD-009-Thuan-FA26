<template>
  <div class="employee-page-wrapper">
    <!-- 1. Thanh tiêu đề -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <h2 class="page-title">Nhân viên</h2>
      </div>
    </div>

    <!-- 2. Khung Bộ lọc (Theme FF T-shirt) -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-icon-box">
          <span class="filter-icon">🍸</span>
        </div>
        <div class="filter-title-wrap">
          <h3 class="filter-title">Bộ lọc</h3>
        </div>
      </div>

      <!-- 3 trường lọc dữ liệu ngang hàng -->
      <div class="filter-inputs-grid">
        <!-- Tìm kiếm từ khóa -->
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

        <!-- Lọc theo Vai trò -->
        <div class="form-field">
          <select v-model="filters.role">
            <option value="">Tất cả vai trò</option>
            <option value="Quản trị viên">Quản trị viên</option>
            <option value="Nhân viên">Nhân viên</option>
          </select>
        </div>

        <!-- Lọc theo Trạng thái -->
        <div class="form-field">
          <select v-model="filters.status">
            <option value="">Tất cả trạng thái</option>
            <option value="active">Hoạt động</option>
            <option value="inactive">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <!-- Cụm 3 nút thao tác bên dưới bên phải -->
      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          <span class="btn-icon">↺</span> Đặt lại bộ lọc
        </button>
        <button class="btn btn-export">
          <span class="btn-icon">📥</span> Xuất Excel
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          <span>+</span> Thêm nhân viên
        </button>
      </div>
    </div>

    <!-- 3. Khung Danh sách nhân viên -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <div class="table-title-wrap">
          <span class="header-icon">👥</span>
          <h3 class="table-title">Danh sách nhân viên</h3>
        </div>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th style="width: 45px; text-align: center;">STT</th>
            <th style="width: 60px; text-align: center;">Ảnh</th>
            <th style="width: 105px;">Mã NV</th>
            <th style="width: 150px;">Họ tên</th>
            <th style="width: 170px;">Email</th>
            <th style="width: 80px;">Giới tính</th>
            <th style="width: 115px;">SĐT</th>
            <th>Địa chỉ</th>
            <th style="width: 110px;">Vai trò</th>
            <th style="width: 110px; text-align: center;">Trạng thái</th>
            <th style="width: 95px; text-align: center;">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, index) in filteredList" :key="item.id">
            <td style="text-align: center;" class="text-muted">{{ index + 1 }}</td>

            <!-- Ảnh đại diện / Avatar viết tắt -->
            <td style="text-align: center;">
              <div class="avatar-cell">
                <img v-if="item.avatar" :src="item.avatar" class="avatar-img" alt="avatar" />
                <div v-else class="avatar-placeholder">
                  {{ item.initials }}
                </div>
              </div>
            </td>

            <!-- Mã NV -->
            <td class="font-bold text-blue">{{ item.code }}</td>

            <!-- Họ tên -->
            <td class="font-medium text-dark">{{ item.fullName }}</td>

            <!-- Email -->
            <td class="text-email" :title="item.email">{{ item.email }}</td>

            <!-- Giới tính -->
            <td>{{ item.gender }}</td>

            <!-- SĐT -->
            <td class="text-dark">{{ item.phone }}</td>

            <!-- Địa chỉ -->
            <td class="text-address">{{ item.address }}</td>

            <!-- Vai trò -->
            <td>
                <span class="role-text" :class="{ 'role-admin': item.role === 'Quản trị viên' }">
                  {{ item.role }}
                </span>
            </td>

            <!-- Trạng thái -->
            <td style="text-align: center;">
                <span class="badge-status status-active">
                  {{ item.statusText }}
                </span>
            </td>

            <!-- Hành động: Nguồn & Con mắt -->
            <td style="text-align: center;">
              <div class="action-buttons">
                <button
                    v-if="item.role !== 'Quản trị viên'"
                    class="btn-circle-action"
                    title="Khóa/Mở tài khoản"
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
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const filters = ref({
  keyword: '',
  role: '',
  status: ''
})

const employees = ref([
  {
    id: 1,
    avatar: 'https://api.dicebear.com/7.x/bottts/svg?seed=NV92602',
    code: 'NV92602',
    fullName: 'Nguyễn Văn A',
    email: 'tunganhtranvu5@gmail.com',
    gender: 'Nam',
    phone: '0383854485',
    address: 'KĐT Dương Nội, Phường Dương Nội, Thành phố Hà Nội',
    role: 'Quản trị viên',
    status: 'active',
    statusText: 'Hoạt động'
  },
  {
    id: 2,
    avatar: 'https://api.dicebear.com/7.x/bottts/svg?seed=NV69085',
    code: 'NV69085',
    fullName: 'Trần Vũ ',
    email: 'tunganhabc@gmail.com',
    gender: 'Nam',
    phone: '0383854485',
    address: 'Chung cư Tỉnh Đội, đường Lam Sơn, Phường Vĩnh Yên, Tỉnh Phú Thọ',
    role: 'Quản trị viên',
    status: 'active',
    statusText: 'Hoạt động'
  }
])

const filteredList = computed(() => {
  return employees.value.filter(emp => {
    const kw = filters.value.keyword.toLowerCase()
    const matchKw = !filters.value.keyword ||
        emp.code.toLowerCase().includes(kw) ||
        emp.fullName.toLowerCase().includes(kw) ||
        emp.email.toLowerCase().includes(kw) ||
        emp.phone.includes(kw)
    const matchRole = !filters.value.role || emp.role === filters.value.role
    const matchStatus = !filters.value.status || emp.status === filters.value.status
    return matchKw && matchRole && matchStatus
  })
})

const resetFilters = () => {
  filters.value = {
    keyword: '',
    role: '',
    status: ''
  }
}

const openCreateModal = () => {
  alert('Mở form tạo nhân viên mới!')
}

const viewDetail = (item) => {
  alert(`Xem chi tiết nhân viên: ${item.code} - ${item.fullName}`)
}
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
</style>