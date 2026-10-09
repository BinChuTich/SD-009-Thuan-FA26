<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'
const router = useRouter()

const xemChiTiet = (maHoaDon) => {
  if (!maHoaDon) {
    console.error('Không tìm thấy mã hóa đơn')
    return
  }

  router.push(`/hoa-don/${encodeURIComponent(maHoaDon)}`)
}
const exportExcel = () => {
  alert('Đang xuất danh sách hóa đơn ra file Excel...')
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
  startDate: '',
  endDate: '',
  type: ''
})

const invoiceList = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(5)

const resetPage = () => {
  currentPage.value = 1
}
const formatMoney = (money) => {
  if (money == null) return '0đ'
  return Number(money).toLocaleString('vi-VN') + 'đ'
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('vi-VN')
}

const formatTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleTimeString('vi-VN')
}

const getStatusText = (status) => {
  const statusMap = {
    1: 'Chờ Xác Nhận',
    2: 'Đã Xác Nhận',
    3: 'Chờ Vận Chuyển',
    4: 'Vận Chuyển',
    5: 'Đã Hoàn Thành',
    6: 'Hủy'
  }
  return statusMap[status] || 'Chưa cập nhật'
}

const getStatusClass = (status) => {
  switch (Number(status)) {
    case 1:
      return 'status-pending'
    case 2:
      return 'status-confirmed'
    case 3:
      return 'status-waiting'
    case 4:
      return 'status-shipping'
    case 5:
      return 'status-completed'
    case 6:
      return 'status-cancelled'
    default:
      return 'status-default'
  }
}

const getPaymentText = (status) => {
  return Number(status) === 1 ? 'Đã thanh toán' : 'Chưa thanh toán'
}

const getPaymentClass = (status) => {
  return Number(status) === 1 ? 'payment-paid' : 'payment-unpaid'
}

const loadHoaDon = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const response = await api.get('/api/hoa-don')
    const result = response.data

    // Hỗ trợ API trả về mảng trực tiếp hoặc bọc trong content/data.
    const dataList = Array.isArray(result)
        ? result
        : Array.isArray(result?.content)
            ? result.content
            : Array.isArray(result?.data)
                ? result.data
                : null

    if (!dataList) {
      throw new Error('API không trả về danh sách hóa đơn hợp lệ.')
    }

    invoiceList.value = dataList.map((item) => ({
      id: item.id,
      code: item.maHoaDon,
      customerName: item.tenKhachHang || 'Khách lẻ',
      address: item.diaChiNhanHang || '',
      employeeName: item.nguoiTao || 'Không xác định',
      totalPrice: Number(item.tongTien || 0),
      createTime: formatTime(item.ngayTao),
      createDate: formatDate(item.ngayTao),
      rawDate: item.ngayTao,
      type: Number(item.loaiDon) === 1 ? 'Tại cửa hàng' : 'Online',
      status: Number(item.trangThai),
      paymentStatus: Number(item.trangThaiThanhToan ?? 0),
      note: item.ghiChu || ''
    }))

    currentPage.value = 1
  } catch (error) {
    console.error('Lỗi lấy danh sách hóa đơn:', error)
    errorMessage.value = 'Không thể tải dữ liệu hóa đơn. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const filteredInvoiceList = computed(() => {
  return invoiceList.value.filter((item) => {
    if (filters.value.code) {
      const keyword = filters.value.code.trim().toLowerCase()
      if (!item.code?.toLowerCase().includes(keyword)) {
        return false
      }
    }

    if (filters.value.type && item.type !== filters.value.type) {
      return false
    }

    if (filters.value.startDate) {
      const itemDate = new Date(item.rawDate)
      const startDate = new Date(filters.value.startDate)
      itemDate.setHours(0, 0, 0, 0)
      startDate.setHours(0, 0, 0, 0)
      if (itemDate < startDate) return false
    }

    if (filters.value.endDate) {
      const itemDate = new Date(item.rawDate)
      const endDate = new Date(filters.value.endDate)
      itemDate.setHours(0, 0, 0, 0)
      endDate.setHours(0, 0, 0, 0)
      if (itemDate > endDate) return false
    }

    if (currentTab.value !== 'Tất Cả') {
      const statusText = getStatusText(item.status)
      if (statusText !== currentTab.value) return false
    }

    return true
  })
})

const totalPages = computed(() => {
  return Math.ceil(filteredInvoiceList.value.length / pageSize.value) || 1
})

const paginatedInvoiceList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredInvoiceList.value.slice(start, end)
})

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

// Tải dữ liệu khi mở trang và làm mới khi quay lại tab trình duyệt.
const refreshHoaDonWhenVisible = () => {
  if (document.visibilityState === 'visible') {
    loadHoaDon()
  }
}

onMounted(() => {
  loadHoaDon()
  document.addEventListener('visibilitychange', refreshHoaDonWhenVisible)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', refreshHoaDonWhenVisible)
})
</script>

<template>
  <div class="hoa-don-container">
    <!-- TIÊU ĐỀ CHÍNH -->
    <div class="page-breadcrumb">
      <span class="crumb-parent">Hóa đơn</span>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Danh sách hóa đơn</span>
    </div>

    <!-- KHUNG 1: BỘ LỌC TÌM KIẾM -->
    <div class="card-box filter-card">
      <div class="filter-grid">
        <div class="form-group">
          <label>Mã hóa đơn</label>
          <input
              v-model="filters.code"
              @input="resetPage"
              type="text"
              placeholder="Tìm theo mã hóa đơn..."
              class="form-control"
          />
        </div>

        <div class="form-group">
          <label>Từ ngày</label>
          <input
              v-model="filters.startDate"
              @change="resetPage"
              type="date"
              class="form-control"
          />
        </div>

        <div class="form-group">
          <label>Đến ngày</label>
          <input
              v-model="filters.endDate"
              @change="resetPage"
              type="date"
              class="form-control"
          />
        </div>

        <div class="form-group">
          <label>Loại đơn hàng</label>
          <select
              v-model="filters.type"
              @change="resetPage"
              class="form-control"
          >
            <option value="">Tất cả</option>
            <option value="Tại cửa hàng">Tại cửa hàng</option>
            <option value="Online">Online</option>
          </select>
        </div>
      </div>
    </div>

    <!-- KHUNG 2: DANH SÁCH BẢNG HÓA ĐƠN -->
    <div class="card-box table-card">
      <!-- HEADER KHUNG BẢNG -->
      <div class="table-card-header">
        <h3 class="card-heading">Danh sách hóa đơn</h3>
        <div class="header-actions">
          <button
              class="btn-refresh"
              type="button"
              :disabled="loading"
              @click="loadHoaDon"
          >
            {{ loading ? 'Đang tải...' : '↻ Làm mới' }}
          </button>

          <button class="btn-export" type="button" @click="exportExcel">
            <svg
                class="icon-export"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            Xuất Excel
          </button>
        </div>
      </div>

      <!-- TABS TRẠNG THÁI -->
      <div class="status-tabs">
        <button
            v-for="tab in statusTabs"
            :key="tab"
            class="tab-item"
            :class="{ active: currentTab === tab }"
            @click="currentTab = tab; resetPage()"
        >
          {{ tab }}
        </button>
      </div>

      <!-- LOADING & ERROR -->
      <div v-if="loading" class="state-message">
        Đang tải dữ liệu hóa đơn...
      </div>
      <div v-else-if="errorMessage" class="state-message error">
        {{ errorMessage }}
      </div>

      <!-- BẢNG DỮ LIỆU -->
      <div v-else class="table-responsive">
        <table class="custom-table">
          <thead>
          <tr>
            <th class="th-stt">#</th>
            <th>Mã Hóa Đơn</th>
            <th>Khách Hàng</th>
            <th>Nhân Viên</th>
            <th>Tổng Tiền</th>
            <th>Loại Đơn</th>
            <th>Thời Gian Tạo</th>
            <th>Trạng Thái</th>
            <th>Thanh Toán</th>
            <th class="text-center">Thao Tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="filteredInvoiceList.length === 0">
            <td colspan="10" class="text-center text-empty">
              Không có dữ liệu hóa đơn nào.
            </td>
          </tr>

          <tr
              v-for="(item, index) in paginatedInvoiceList"
              :key="item.id"
              class="table-row"
          >
            <td class="text-muted">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td class="font-bold text-code">{{ item.code }}</td>
            <td class="text-customer">{{ item.customerName }}</td>
            <td class="text-employee">{{ item.employeeName }}</td>
            <td class="font-bold text-price">{{ formatMoney(item.totalPrice) }}</td>
            <td>
                <span :class="item.type === 'Tại cửa hàng' ? 'badge-store' : 'badge-online'">
                  {{ item.type }}
                </span>
            </td>
            <td>
              <div class="date-main">{{ item.createDate }}</div>
              <div class="time-sub">{{ item.createTime }}</div>
            </td>
            <td>
                <span class="badge-status" :class="getStatusClass(item.status)">
                  {{ getStatusText(item.status) }}
                </span>
            </td>
            <td>
                <span class="badge-payment" :class="getPaymentClass(item.paymentStatus)">
                  {{ getPaymentText(item.paymentStatus) }}
                </span>
            </td>
            <td class="text-center">
              <button
                  class="btn-action-view"
                  title="Xem chi tiết"
                  @click="xemChiTiet(item.code)"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              </button>
            </td>
          </tr>
          </tbody>
        </table>

        <!-- PHÂN TRANG -->
        <div v-if="filteredInvoiceList.length > 0" class="pagination-section">
          <div class="pagination-info">
            Hiển thị <strong>{{ (currentPage - 1) * pageSize + 1 }}</strong> -
            <strong>{{ Math.min(currentPage * pageSize, filteredInvoiceList.length) }}</strong> trên tổng
            <strong>{{ filteredInvoiceList.length }}</strong> hóa đơn
          </div>

          <div class="pagination-buttons">
            <button
                class="btn-pager"
                :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)"
            >
              ‹
            </button>

            <button
                v-for="page in totalPages"
                :key="page"
                class="btn-pager"
                :class="{ active: currentPage === page }"
                @click="changePage(page)"
            >
              {{ page }}
            </button>

            <button
                class="btn-pager"
                :disabled="currentPage === totalPages"
                @click="changePage(currentPage + 1)"
            >
              ›
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hoa-don-container {
  padding: 24px 32px;
  background-color: #faf7f0;
  min-height: 100vh;
  box-sizing: border-box;
  margin-left: 20px;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  margin-bottom: 20px;
}

.crumb-parent {
  color: #8c969e;
  font-weight: 400;
  cursor: pointer;
  transition: color 0.2s;
}

.crumb-parent:hover {
  color: #3e5c76;
}

.crumb-separator {
  color: #b0b7bd;
  font-weight: 300;
  user-select: none;
}

.crumb-current {
  color: #3e5c76;
  font-weight: 600;
}

.card-box {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #ebd9c8;
  padding: 22px 26px;
  margin-bottom: 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.card-heading {
  font-size: 1rem;
  font-weight: 700;
  color: #3e5c76;
  margin: 0;
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-refresh {
  height: 35px;
  padding: 0 14px;
  border: 1.5px solid #c8d6df;
  border-radius: 8px;
  background: #edf4f7;
  color: #3e5c76;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover:not(:disabled) {
  background: #dfeaf0;
  border-color: #9eb6c5;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-export {
  background-color: #f7ebe1;
  color: #3e5c76;
  border: 1.5px solid #e5c3a3;
  height: 35px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  transition: all 0.2s ease;
}

.btn-export:hover {
  background-color: #f2decb;
  border-color: #cca47f;
  transform: translateY(-1px);
}

.icon-export {
  width: 15px;
  height: 15px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-group label {
  font-size: 0.83rem;
  font-weight: 500;
  color: #555e65;
}

.form-control {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #d5c8b8;
  border-radius: 7px;
  background-color: #ffffff;
  color: #333333;
  font-size: 0.88rem;
  outline: none;
  transition: border-color 0.2s ease;
}

.form-control:focus {
  border-color: #3e5c76;
}

.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  overflow-x: auto;
}

.tab-item {
  padding: 7px 18px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #4f5d68;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.tab-item:hover {
  background: #f4ede4;
}

.tab-item.active {
  background: #e8f1f5;
  color: #3e5c76;
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.88rem;
}

.custom-table th {
  background-color: #f5efeb;
  color: #3e5c76;
  font-weight: 700;
  padding: 13px 14px;
  white-space: nowrap;
  border-bottom: 1px solid #ebd9c8;
}

.custom-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #f1e7dc;
  color: #40484f;
  vertical-align: middle;
}

.table-row:hover {
  background-color: #fcfaf7;
}

.font-bold {
  font-weight: 700;
}

.text-code {
  color: #2b353b;
}

.text-customer {
  color: #3e474f;
}

.text-employee {
  color: #5d676e;
}

.text-price {
  color: #2b353b;
}

.date-main {
  color: #3e474f;
}

.time-sub {
  font-size: 0.75rem;
  color: #8c969e;
}

.text-center {
  text-align: center;
}

.text-empty {
  padding: 30px;
  color: #8c969e;
}

.badge-store {
  background: #e8f6ed;
  color: #23783a;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
}

.badge-online {
  background: #e7f0fd;
  color: #1a73e8;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
}

.badge-status {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 0.76rem;
  font-weight: 600;
  display: inline-block;
}

.status-waiting {
  background: #f5edff;
  color: #8a3ee6;
}

.status-pending {
  background: #fff4e5;
  color: #d97706;
}

.status-confirmed {
  background: #e0f2fe;
  color: #0369a1;
}

.status-shipping {
  background: #e0f7fa;
  color: #00838f;
}

.status-completed {
  background: #e6f6ec;
  color: #1b7a37;
}

.status-cancelled {
  background: #fee6e6;
  color: #be2626;
}

.status-default {
  background: #f0e9df;
  color: #636b72;
}

.badge-payment {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.76rem;
  font-weight: 600;
}

.payment-paid {
  background: #eaf8ed;
  color: #227838;
  border: 1px solid #bce1c5;
}

.payment-unpaid {
  background: #fff6eb;
  color: #b75e11;
  border: 1px solid #fad3ae;
}

.btn-action-view {
  width: 32px;
  height: 32px;
  border: 1px solid #d5c8b8;
  background: #ffffff;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #277da1;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-action-view:hover {
  background-color: #faf7f0;
  border-color: #a49787;
}

.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  margin-top: 8px;
}

.pagination-info {
  font-size: 0.85rem;
  color: #555e65;
}

.pagination-buttons {
  display: flex;
  gap: 5px;
}

.btn-pager {
  width: 32px;
  height: 32px;
  border: 1px solid #ded5c7;
  background: #ffffff;
  color: #555e65;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.84rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-pager.active {
  background: #36536b;
  border-color: #36536b;
  color: #ffffff;
}

.btn-pager:hover:not(:disabled):not(.active) {
  background: #f4ede4;
}

.btn-pager:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.state-message {
  padding: 36px;
  text-align: center;
  color: #555e65;
}

.state-message.error {
  color: #c53030;
}

@media (max-width: 1024px) {
  .filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hoa-don-container {
    padding: 16px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .table-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .pagination-section {
    flex-direction: column;
    gap: 14px;
    align-items: flex-start;
  }
}
</style>