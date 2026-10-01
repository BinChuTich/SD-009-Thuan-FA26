<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Hàm nhận mã hóa đơn và chuyển trang
const xemChiTiet = (ma) => {
  router.push(`/hoa-don/${ma}`)
}

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

// Dữ liệu danh sách hóa đơn chuẩn
const invoiceList = ref([
  {
    id: 1,
    code: 'HDMKT1',
    customerName: 'No name',
    employeeName: 'Admin 1',
    totalPrice: '3.500.000đ',
    createTime: '00:00:00',
    createDate: '28/05/2026',
    type: 'Tại cửa hàng'
  },
  {
    id: 2,
    code: 'HDSLA1',
    customerName: 'No name',
    employeeName: 'Admin 2',
    totalPrice: '2.500.000đ',
    createTime: '00:00:00',
    createDate: '28/05/2026',
    type: 'Online'
  },
  {
    id: 3,
    code: 'HDSBOG1',
    customerName: 'No name',
    employeeName: 'Admin 3',
    totalPrice: '2.700.000đ',
    createTime: '00:00:00',
    createDate: '28/05/2026',
    type: 'Online'
  }
])
</script>

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

      <!-- 3 nút thao tác bên phải -->
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
          <tr v-for="(item, index) in invoiceList" :key="item.id">
            <td style="text-align: center">{{ index + 1 }}</td>

            <!-- Click vào mã hóa đơn để chuyển sang trang chi tiết -->
            <td>
                <span class="code-link" @click="xemChiTiet(item.code)">
                  {{ item.code }}
                </span>
            </td>

            <td>{{ item.customerName }}</td>
            <td>{{ item.employeeName }}</td>
            <td><b>{{ item.totalPrice }}</b></td>
            <td>
              <div>{{ item.createTime }}</div>
              <small style="color: #9aa0a0">{{ item.createDate }}</small>
            </td>
            <td>
                <span class="badge" :class="item.type === 'Tại cửa hàng' ? 'instore' : 'online'">
                  {{ item.type }}
                </span>
            </td>

            <!-- Click vào nút con mắt để xem chi tiết -->
            <td style="text-align: center">
              <button class="btn-action-view" @click="xemChiTiet(item.code)" title="Xem chi tiết">
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

<style scoped>
/* Toàn bộ vùng hiển thị trang Hóa đơn */
.invoice-container {
  padding: 1.25rem 1.5rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header trên cùng */
.top-title-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0.9rem 1.2rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.page-title {
  color: var(--blue, #496883);
  font-weight: 700;
  font-size: 1.15rem; /* ~18px, to rõ */
  letter-spacing: 0.3px;
}

/* 2. Thẻ Card dùng chung */
.custom-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.2rem;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1.2rem;
}

.card-title h3 {
  font-size: 1.05rem; /* ~16.5px */
  font-weight: 700;
  margin: 0;
  color: #4b5b62;
}

.filter-icon {
  font-size: 1.2rem;
  color: var(--blue, #496883);
}

/* Bộ lọc Grid 4 cột */
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-group label {
  font-size: 0.88rem; /* Cũ: 8.5px -> Tăng lên ~14px */
  font-weight: 700;
  color: #556268;
}

.form-group input,
.form-group select {
  height: 2.5rem; /* Cao 40px thoải mái */
  border: 1px solid var(--line, #e9e5db);
  border-radius: 7px;
  padding: 0 0.85rem;
  font-size: 0.92rem; /* Cũ: 9px -> Tăng lên ~14.7px */
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
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
  padding-right: 2.2rem;
}

.field-icon {
  position: absolute;
  right: 0.75rem;
  font-size: 1rem;
  color: var(--muted, #8a9292);
  pointer-events: none;
}

/* 3 Nút lọc */
.filter-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
}

.btn {
  height: 2.4rem;
  padding: 0 1.25rem;
  border: none;
  border-radius: 7px;
  font-size: 0.88rem; /* Chữ nút bấm to rõ */
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.btn-search {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-search:hover {
  background-color: #38536b;
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

/* 3. Danh sách hóa đơn */
.list-header-title {
  margin-bottom: 1rem;
}

.doc-icon-wrap {
  width: 34px;
  height: 34px;
  background-color: #f7eee1;
  border-radius: 8px;
  display: grid;
  place-items: center;
}

.doc-icon {
  font-size: 1.1rem;
  color: #b18b52;
}

/* Tabs trạng thái */
.status-tabs {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #efede7;
  overflow-x: auto;
  margin-bottom: 0.85rem;
}

.tab-item {
  border: none;
  background: transparent;
  padding: 0.65rem 1.15rem;
  font-size: 0.9rem; /* Cũ: 8.5px -> Tăng lên 14.4px */
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
  border-top-left-radius: 7px;
  border-top-right-radius: 7px;
}

/* =====================================================
   BẢNG DỮ LIỆU ĐÃ PHÓNG TO FONT CHỮ VÀ DÃN DÒNG
===================================================== */
.table-responsive {
  overflow-x: auto;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem; /* Cũ: 8px -> Nâng lên ~14.7px chuẩn đọc */
}

.invoice-table th {
  background-color: #faf9f6;
  color: #727b7d;
  font-weight: 700;
  padding: 0.85rem 1rem; /* Dãn khoảng đệm bảng */
  text-align: left;
  border-bottom: 1px solid #efede7;
  font-size: 0.92rem;
  white-space: nowrap;
}

.invoice-table td {
  padding: 0.95rem 1rem; /* Dãn dòng cách đều, không bị bí */
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.invoice-table tr:hover td {
  background-color: #fcfbf8;
}

/* Link mã hóa đơn */
.code-link {
  color: var(--blue, #496883);
  font-weight: 700;
  font-size: 0.95rem;
  font-family: monospace, sans-serif;
  cursor: pointer;
  text-decoration: none;
}
.code-link:hover {
  text-decoration: underline;
}

/* Cột ngày giờ */
.invoice-table td small {
  display: block;
  font-size: 0.78rem;
  color: #9aa0a0;
  margin-top: 3px;
}

/* Badge Tại cửa hàng / Online */
.badge {
  font-size: 0.82rem; /* Cũ: 7.5px -> Tăng lên ~13px */
  padding: 0.35rem 0.75rem;
  border-radius: 14px;
  font-weight: 600;
  display: inline-block;
}
.badge.instore {
  background: #f5eddf;
  color: #a17e45;
}
.badge.online {
  background: #eaf2f6;
  color: #5587a3;
}

/* Nút con mắt xem chi tiết */
.btn-action-view {
  width: 32px;
  height: 32px;
  background-color: #ffffff;
  border: 1px solid #e9e5db;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem; /* Biểu tượng con mắt to rõ */
  color: #496883;
  text-decoration: none;
  transition: all 0.2s;
}
.btn-action-view:hover {
  background-color: #eaf1f4;
  border-color: #496883;
  transform: scale(1.08);
}

/* Phân trang dưới cùng */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  padding-top: 1.2rem;
}

.pg-btn {
  width: 30px;
  height: 30px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 5px;
  font-size: 0.85rem;
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