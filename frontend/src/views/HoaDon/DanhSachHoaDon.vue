<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'

const router = useRouter()

/* =========================
   ĐIỀU HƯỚNG
========================= */

const xemChiTiet = (ma) => {
  router.push(`/hoa-don/${ma}`)
}

const themHoaDon = () => {
  router.push('/hoa-don/them')
}

/* =========================
   TAB TRẠNG THÁI
========================= */

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

/* =========================
   BỘ LỌC
========================= */

const filters = ref({
  code: '',
  startDate: '',
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

  currentTab.value = 'Tất Cả'
}

/* =========================
   DỮ LIỆU
========================= */

const invoiceList = ref([])

const loading = ref(false)
const errorMessage = ref('')

/* =========================
   FORMAT TIỀN
========================= */

const formatMoney = (money) => {
  if (money == null) return '0đ'

  return Number(money).toLocaleString('vi-VN') + 'đ'
}

/* =========================
   FORMAT NGÀY
========================= */

const formatDate = (date) => {
  if (!date) return ''

  return new Date(date).toLocaleDateString('vi-VN')
}

/* =========================
   FORMAT GIỜ
========================= */

const formatTime = (date) => {
  if (!date) return ''

  return new Date(date).toLocaleTimeString('vi-VN')
}

/* =========================
   TRẠNG THÁI
========================= */

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

/* =========================
   CLASS TRẠNG THÁI
========================= */

const getStatusClass = (status) => {
  switch (status) {
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

/* =========================
   LẤY DỮ LIỆU API
========================= */

const loadHoaDon = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const response = await api.get('/api/hoa-don')

    console.log('Dữ liệu hóa đơn từ API:', response.data)

    /*
     * API thực tế của bạn trả:
     *
     * id
     * maHoaDon
     * loaiDon
     * phiShip
     * tongTien
     * tongTienGiamGia
     * tenKhachHang
     * soDienThoaiKhachHang
     * diaChiNhanHang
     * ngayTao
     * nguoiTao
     * trangThai
     * ghiChu
     */

    invoiceList.value = response.data.map(item => ({
      id: item.id,

      // Mã hóa đơn
      code: item.maHoaDon,

      // Khách hàng
      customerName: item.tenKhachHang || 'Khách lẻ',

      // Địa chỉ
      address: item.diaChiNhanHang || '',

      // Người tạo hóa đơn
      employeeName: item.nguoiTao || 'Không xác định',


      // Tổng tiền
      totalPrice: Number(item.tongTien || 0),

      // Ngày giờ tạo
      createTime: formatTime(item.ngayTao),

      // Ngày tạo
      createDate: formatDate(item.ngayTao),

      // Giữ lại ngày gốc để lọc
      rawDate: item.ngayTao,

      // Loại đơn
      type: item.loaiDon === 1
          ? 'Tại cửa hàng'
          : 'Online',

      // Trạng thái dạng số
      status: item.trangThai,

      // Ghi chú
      note: item.ghiChu || ''
    }))

  } catch (error) {
    console.error('Lỗi lấy danh sách hóa đơn:', error)

    errorMessage.value = 'Không thể tải dữ liệu hóa đơn.'
  } finally {
    loading.value = false
  }
}

/* =========================
   DANH SÁCH SAU KHI LỌC
========================= */

const filteredInvoiceList = computed(() => {
  return invoiceList.value.filter(item => {

    /* -------------------------
       Lọc mã hóa đơn
    ------------------------- */

    if (filters.value.code) {
      const keyword = filters.value.code
          .trim()
          .toLowerCase()

      if (!item.code?.toLowerCase().includes(keyword)) {
        return false
      }
    }

    /* -------------------------
       Lọc loại đơn
    ------------------------- */

    if (filters.value.type) {
      if (item.type !== filters.value.type) {
        return false
      }
    }

    /* -------------------------
       Lọc từ ngày
    ------------------------- */

    if (filters.value.startDate) {
      const itemDate = new Date(item.rawDate)
      const startDate = new Date(filters.value.startDate)

      itemDate.setHours(0, 0, 0, 0)
      startDate.setHours(0, 0, 0, 0)

      if (itemDate < startDate) {
        return false
      }
    }

    /* -------------------------
       Lọc đến ngày
    ------------------------- */

    if (filters.value.endDate) {
      const itemDate = new Date(item.rawDate)
      const endDate = new Date(filters.value.endDate)

      itemDate.setHours(0, 0, 0, 0)
      endDate.setHours(0, 0, 0, 0)

      if (itemDate > endDate) {
        return false
      }
    }

    /* -------------------------
       Lọc trạng thái theo Tab
    ------------------------- */

    if (currentTab.value !== 'Tất Cả') {
      const statusText = getStatusText(item.status)

      if (statusText !== currentTab.value) {
        return false
      }
    }

    return true
  })
})

/* =========================
   MOUNT
========================= */

onMounted(() => {
  loadHoaDon()
})
</script>

<template>
  <div class="hoa-don-page">

    <!-- =========================
         TIÊU ĐỀ
    ========================== -->

    <div class="page-title-box">
      <div class="title-row">

        <h2 class="page-title">
          Quản Lý Hóa Đơn
        </h2>

        <button
            class="btn-add"
            @click="themHoaDon"
        >
          + Thêm hóa đơn
        </button>

      </div>
    </div>


    <!-- =========================
         BỘ LỌC
    ========================== -->

    <div class="card-box filter-card">

      <div class="card-title">
        Bộ lọc tìm kiếm
      </div>

      <div class="filter-grid">

        <!-- Mã hóa đơn -->

        <div class="form-group">

          <label>
            Mã hóa đơn
          </label>

          <input
              v-model="filters.code"
              type="text"
              placeholder="Tìm theo mã hóa đơn..."
              class="form-control"
          />

        </div>


        <!-- Từ ngày -->

        <div class="form-group">

          <label>
            Từ ngày
          </label>

          <input
              v-model="filters.startDate"
              type="date"
              class="form-control"
          />

        </div>


        <!-- Đến ngày -->

        <div class="form-group">

          <label>
            Đến ngày
          </label>

          <input
              v-model="filters.endDate"
              type="date"
              class="form-control"
          />

        </div>


        <!-- Loại đơn -->

        <div class="form-group">

          <label>
            Loại đơn hàng
          </label>

          <select
              v-model="filters.type"
              class="form-control"
          >

            <option value="">
              Tất cả
            </option>

            <option value="Tại cửa hàng">
              Tại cửa hàng
            </option>

            <option value="Online">
              Online
            </option>

          </select>

        </div>

      </div>


      <!-- Nút lọc -->

      <div class="filter-actions">

        <button
            class="btn btn-secondary"
            @click="resetFilters"
        >
          Đặt lại
        </button>

        <button
            class="btn btn-primary"
            @click="loadHoaDon"
        >
          Tải lại
        </button>

      </div>

    </div>


    <!-- =========================
         DANH SÁCH
    ========================== -->

    <div class="card-box table-card">

      <!-- Tabs -->

      <div class="status-tabs">

        <button
            v-for="tab in statusTabs"
            :key="tab"
            class="tab-item"
            :class="{
            active: currentTab === tab
          }"
            @click="currentTab = tab"
        >
          {{ tab }}
        </button>

      </div>


      <!-- Loading -->

      <div
          v-if="loading"
          class="state-message"
      >
        Đang tải dữ liệu...
      </div>


      <!-- Error -->

      <div
          v-else-if="errorMessage"
          class="state-message error"
      >
        {{ errorMessage }}
      </div>


      <!-- Bảng -->

      <div
          v-else
          class="table-responsive"
      >

        <table class="custom-table">

          <thead>

          <tr>

            <th>
              #
            </th>

            <th>
              Mã Hóa Đơn
            </th>

            <th>
              Khách Hàng
            </th>

            <th>
              Nhân Viên
            </th>

            <th>
              Tổng Tiền
            </th>

            <th>
              Loại Đơn
            </th>

            <th>
              Thời Gian Tạo
            </th>

            <th>
              Trạng Thái
            </th>

            <th class="text-center">
              Thao Tác
            </th>

          </tr>

          </thead>


          <tbody>

          <!-- Không có dữ liệu -->

          <tr
              v-if="filteredInvoiceList.length === 0"
          >

            <td
                colspan="9"
                class="text-center text-muted"
            >
              Không có dữ liệu hóa đơn nào.
            </td>

          </tr>


          <!-- Danh sách -->

          <tr
              v-for="(item, index) in filteredInvoiceList"
              :key="item.id"
          >

            <!-- STT -->

            <td>
              {{ index + 1 }}
            </td>


            <!-- Mã -->

            <td class="font-bold text-code">
              {{ item.code }}
            </td>


            <!-- Khách hàng -->

            <td>
              {{ item.customerName }}
            </td>


            <!-- Nhân viên -->

            <td>
              {{ item.employeeName }}
            </td>


            <!-- Tổng tiền -->

            <td class="font-bold text-price">

              {{ formatMoney(item.totalPrice) }}

            </td>


            <!-- Loại đơn -->

            <td>

              <span
                  :class="
                  item.type === 'Tại cửa hàng'
                    ? 'badge-store'
                    : 'badge-online'
                "
              >

                {{ item.type }}

              </span>

            </td>


            <!-- Thời gian -->

            <td>

              <div>
                {{ item.createDate }}
              </div>

              <small class="text-muted">
                {{ item.createTime }}
              </small>

            </td>


            <!-- Trạng thái -->

            <td>

              <span
                  class="badge-status"
                  :class="getStatusClass(item.status)"
              >

                {{ getStatusText(item.status) }}

              </span>

            </td>


            <!-- Thao tác -->

            <td class="text-center">

              <button
                  class="btn-action"
                  @click="xemChiTiet(item.code)"
              >
                Chi tiết
              </button>

            </td>

          </tr>

          </tbody>

        </table>

      </div>

    </div>

  </div>
</template>


<style scoped>

.hoa-don-page {
  padding: 24px;
}


/* =========================
   TITLE
========================= */

.page-title-box {
  margin-bottom: 20px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #496883;
  margin: 0;
}


/* Nút thêm */

.btn-add {
  background: #496883;
  color: #ffffff;
  border: none;
  padding: 9px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.85rem;
  transition: all 0.2s ease;
}

.btn-add:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}


/* =========================
   CARD
========================= */

.card-box {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  padding: 20px;
  margin-bottom: 24px;
}

.card-title {
  font-weight: 600;
  color: #496883;
  margin-bottom: 16px;
  font-size: 1rem;
}


/* =========================
   FILTER
========================= */

.filter-grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fit,
    minmax(200px, 1fr)
  );
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 0.85rem;
  font-weight: 500;
  color: #647074;
}

.form-control {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #d6d0c3;
  border-radius: 6px;
  outline: none;
  font-size: 0.9rem;
  color: #333;
  background-color: #fff;
}

.form-control:focus {
  border-color: #496883;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: opacity 0.2s ease;
}

.btn:hover {
  opacity: 0.9;
}

.btn-primary {
  background-color: #496883;
  color: #ffffff;
}

.btn-secondary {
  background-color: #e9e5db;
  color: #647074;
}


/* =========================
   STATUS TABS
========================= */

.status-tabs {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #e9e5db;
  padding-bottom: 12px;
  margin-bottom: 16px;
  overflow-x: auto;
}

.tab-item {
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: #647074;
  font-size: 0.9rem;
  font-weight: 600;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.tab-item:hover {
  background: #f7f5ef;
  color: #496883;
}

.tab-item.active {
  background: #eaf1f4;
  color: #496883;
}


/* =========================
   TABLE
========================= */

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.9rem;
}

.custom-table th {
  background-color: #f7f5ef;
  color: #496883;
  padding: 12px 14px;
  font-weight: 600;
  border-bottom: 1px solid #e9e5db;
  white-space: nowrap;
}

.custom-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #e9e5db;
  color: #555;
}

.font-bold {
  font-weight: 600;
}

.text-code {
  color: #496883;
}

.text-price {
  color: #c94a29;
}

.text-muted {
  color: #8a9292;
}

.text-center {
  text-align: center;
}

.customer-name {
  font-weight: 500;
}


/* =========================
   BADGES
========================= */

.badge-store,
.badge-online,
.badge-status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  display: inline-block;
}


/* Loại đơn */

.badge-store {
  background: #e6f4ea;
  color: #1e7e34;
}

.badge-online {
  background: #e8f0fe;
  color: #1a73e8;
}


/* =========================
   TRẠNG THÁI
========================= */

.badge-status {
  border: 1px solid transparent;
}


/* Chờ xác nhận */

.status-pending {
  background: #fff4e5;
  color: #e67e22;
  border-color: #f5d6a6;
}


/* Đã xác nhận */

.status-confirmed {
  background: #e8f0fe;
  color: #1a73e8;
  border-color: #c8d9f5;
}


/* Chờ vận chuyển */

.status-waiting {
  background: #f3e8ff;
  color: #7b3fb5;
  border-color: #dfc8f4;
}


/* Vận chuyển */

.status-shipping {
  background: #e0f7fa;
  color: #00838f;
  border-color: #b2ebf2;
}


/* Hoàn thành */

.status-completed {
  background: #e6f4ea;
  color: #1e7e34;
  border-color: #b7dfc1;
}


/* Hủy */

.status-cancelled {
  background: #fdecea;
  color: #c62828;
  border-color: #f5c2c0;
}


/* Không xác định */

.status-default {
  background: #f5f5f5;
  color: #777;
  border-color: #ddd;
}


/* =========================
   BUTTON CHI TIẾT
========================= */

.btn-action {
  background-color: #f7f5ef;
  color: #496883;
  border: 1px solid #d6d0c3;
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-action:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
}


/* =========================
   LOADING / ERROR
========================= */

.state-message {
  padding: 30px;
  text-align: center;
  color: #647074;
}

.state-message.error {
  color: #dc3545;
}


/* =========================
   MOBILE
========================= */

@media (max-width: 768px) {

  .hoa-don-page {
    padding: 16px;
  }

  .title-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .btn-add {
    width: 100%;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

}

</style>