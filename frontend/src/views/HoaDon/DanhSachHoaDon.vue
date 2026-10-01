<template>
  <div class="invoice-container">
    <!-- 1. Thanh tiêu đề phía trên -->
    <div class="top-title-card">
      <span class="page-title">Quản Lý Hóa Đơn</span>
    </div>

    <!-- 2. Khung Bộ Lọc -->
    <div class="custom-card filter-card">
      <div class="card-title">
        <span class="filter-icon">🌪️</span>
        <h3>Bộ Lọc</h3>
      </div>

      <div class="filter-grid">
        <!-- Mã hóa đơn -->
        <div class="form-group">
          <label>Mã hóa đơn</label>
          <input
              type="text"
              v-model="filters.code"
              placeholder="Nhập mã hóa đơn"
          />
        </div>

        <!-- Ngày Bắt Đầu -->
        <div class="form-group">
          <label>Ngày Bắt Đầu</label>
          <div class="input-with-icon">
            <input type="text" v-model="filters.startDate" placeholder="28/05/2026" />
            <span class="field-icon">📅</span>
          </div>
        </div>

        <!-- Ngày Kết Thúc -->
        <div class="form-group">
          <label>Ngày Kết Thúc</label>
          <div class="input-with-icon">
            <input type="text" v-model="filters.endDate" placeholder="dd/mm/yy" />
            <span class="field-icon">📅</span>
          </div>
        </div>

        <!-- Loại Đơn -->
        <div class="form-group">
          <label>Loại Đơn</label>
          <select v-model="filters.type">
            <option value="">Loại Đơn</option>
            <option value="Tại cửa hàng">Tại cửa hàng</option>
            <option value="Online">Online</option>
          </select>
        </div>
      </div>

      <!-- 3 nút thao tác bên phải theo đúng tone màu của dự án -->
      <div class="filter-buttons">
        <button class="btn btn-search">Tìm Kiếm</button>
        <button class="btn btn-reset" @click="resetFilters">Làm Mới</button>
        <button class="btn btn-export">Xuất File</button>
      </div>
    </div>

    <!-- 3. Khung Danh Sách Hóa Đơn -->
    <div class="custom-card list-card">
      <div class="card-title list-header-title">
        <div class="doc-icon-wrap">
          <span class="doc-icon">📄</span>
        </div>
        <h3>Danh Sách Hóa Đơn</h3>
      </div>

      <!-- Các tabs trạng thái -->
      <div class="status-tabs">
        <button
            v-for="tab in statusTabs"
            :key="tab"
            class="tab-item"
            :class="{ active: currentTab === tab }"
            @click="currentTab = tab"
        >
          {{ tab }}
        </button>
      </div>

      <!-- Bảng dữ liệu hóa đơn -->
      <div class="table-responsive">
        <table class="invoice-table">
          <thead>
          <tr>
            <th style="width: 50px">STT</th>
            <th>Mã Hóa Đơn</th>
            <th>Tên Khách Hàng</th>
            <th>Tên Nhân Viên</th>
            <th>Tổng Tiền</th>
            <th>Ngày Tạo</th>
            <th>Loại Đơn</th>
            <th style="width: 90px; text-align: center">Hành Động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, index) in invoiceList" :key="item.code">
            <td>{{ index + 1 }}</td>
            <td><b class="text-blue">{{ item.code }}</b></td>
            <td>{{ item.customer }}</td>
            <td>{{ item.staff }}</td>
            <td class="total-text">{{ item.total }}</td>
            <td>
              <div class="datetime-cell">
                <span>{{ item.time }}</span>
                <span class="text-muted">{{ item.date }}</span>
              </div>
            </td>
            <td>
                <span class="pill-type" :class="item.type === 'Online' ? 'online' : 'instore'">
                  {{ item.type }}
                </span>
            </td>
            <td style="text-align: center">
              <button class="btn-action-view" title="Xem chi tiết">
                👁
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang góc dưới bên phải -->
      <div class="pagination-wrapper">
        <button class="pg-btn" disabled>‹</button>
        <button class="pg-btn active">1</button>
        <button class="pg-btn">›</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const currentTab = ref('Tất Cả')

const statusTabs = [
  'Tất Cả',
  'Chờ Xác Nhận',
  'Đã Xác Nhận',
  'Chờ Vận Chuyển',
  'Vận Chuyển',
  'Đã Hoàn Thành',
  'Hủy'
]

const filters = ref({
  code: '',
  startDate: '28/05/2026',
  endDate: '',
  type: ''
})

const resetFilters = () => {
  filters.value = {
    code: '',
    startDate: '',
    endDate: '',
    type: ''
  }
}

const invoiceList = ref([
  {
    code: 'HDMKT1',
    customer: 'No name',
    staff: 'Admin 1',
    total: '3.500.000đ',
    time: '00:00:00',
    date: '28/05/2026',
    type: 'Tại cửa hàng'
  },
  {
    code: 'HDSLA1',
    customer: 'No name',
    staff: 'Admin 2',
    total: '2.500.000đ',
    time: '00:00:00',
    date: '28/05/2026',
    type: 'Online'
  },
  {
    code: 'HDSBOG1',
    customer: 'No name',
    staff: 'Admin 3',
    total: '2.700.000đ',
    time: '00:00:00',
    date: '28/05/2026',
    type: 'Online'
  }
])
</script>

<style scoped>
/* Toàn bộ vùng hiển thị trang - Nền màu kem ấm của dự án */
.invoice-container {
  padding: 16px 20px 24px;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  color: var(--text, #3d4a50);
}

/* 1. Header trên cùng có viền be/vàng */
.top-title-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 11px 18px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.page-title {
  color: var(--blue, #496883);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.3px;
}

/* 2. Thẻ Card dùng chung chuẩn bảng màu */
.custom-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 14px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.card-title h3 {
  font-size: 12px;
  font-weight: 700;
  margin: 0;
  color: #4b5b62;
}

.filter-icon {
  font-size: 14px;
  color: var(--blue, #496883);
}

/* Bộ lọc Grid 4 cột */
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 8.5px;
  font-weight: 700;
  color: #556268;
}

.form-group input,
.form-group select {
  height: 34px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  padding: 0 11px;
  font-size: 9px;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s ease;
}

.form-group input:focus,
.form-group select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon input {
  width: 100%;
  padding-right: 30px;
}

.field-icon {
  position: absolute;
  right: 9px;
  font-size: 11px;
  color: var(--muted, #8a9292);
  pointer-events: none;
}

/* 3 Nút bấm chuẩn hệ màu giao diện */
.filter-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn {
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

/* Tìm Kiếm: Nền xanh đá theo tone logo FF */
.btn-search {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-search:hover {
  background-color: #38536b;
}

/* Làm Mới: Nền kem viền vàng be */
.btn-reset {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-reset:hover {
  background-color: #faeed7;
}

/* Xuất File: Nền xanh dịu đồng bộ */
.btn-export {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #d2e5d6;
}
.btn-export:hover {
  background-color: #deede1;
}

/* 3. Danh sách hóa đơn */
.list-header-title {
  margin-bottom: 12px;
}

.doc-icon-wrap {
  width: 28px;
  height: 28px;
  background-color: #f7eee1;
  border-radius: 6px;
  display: grid;
  place-items: center;
}

.doc-icon {
  font-size: 13px;
  color: #b18b52;
}

/* Tabs trạng thái */
.status-tabs {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #efede7;
  margin-bottom: 0px;
  overflow-x: auto;
}

.tab-item {
  border: none;
  background: transparent;
  padding: 9px 15px;
  font-size: 8.5px;
  font-weight: 600;
  color: #647074;
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item:hover {
  color: var(--blue, #496883);
}

.tab-item.active {
  background-color: #eaf1f4;
  color: var(--blue, #496883);
  font-weight: 700;
  border-bottom: 2px solid var(--blue, #496883);
  border-top-left-radius: 6px;
  border-top-right-radius: 6px;
}

/* Bảng dữ liệu */
.table-responsive {
  overflow-x: auto;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 8px;
}

.invoice-table th {
  background-color: #faf9f6;
  color: #8f9695;
  font-weight: 700;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #efede7;
}

.invoice-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f0eb;
  color: #556268;
}

.invoice-table tr:hover td {
  background-color: #fcfbf8;
}

.text-blue {
  color: var(--blue, #496883);
}

.text-muted {
  color: #9aa0a0;
  font-size: 7px;
}

.total-text {
  font-weight: 700;
  color: #42555e;
}

.datetime-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.pill-type {
  font-size: 7px;
  padding: 2px 7px;
  border-radius: 10px;
}
.pill-type.instore {
  background: #f5eddf;
  color: #a17e45;
}
.pill-type.online {
  background: #eaf2f6;
  color: #6a95ad;
}

.btn-action-view {
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  width: 24px;
  height: 22px;
  cursor: pointer;
  font-size: 11px;
  color: var(--blue, #496883);
  display: inline-grid;
  place-items: center;
  transition: all 0.2s;
}

.btn-action-view:hover {
  background-color: #eaf1f4;
}

/* Phân trang góc dưới bên phải */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 4px;
  padding-top: 14px;
}

.pg-btn {
  width: 22px;
  height: 22px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  font-size: 8px;
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

@media (max-width: 1000px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 650px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
  .filter-buttons {
    flex-wrap: wrap;
  }
}
</style>