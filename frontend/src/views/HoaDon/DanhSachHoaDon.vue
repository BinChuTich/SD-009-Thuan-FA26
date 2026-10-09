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

// Chức năng Chỉnh sửa trạng thái hóa đơn trực tiếp
const showStatusModal = ref(false)
const selectedInvoice = ref(null)
const targetStatus = ref(1)
const statusNote = ref('')
const updatingStatus = ref(false)

const toast = ref({
  show: false,
  message: '',
  type: 'success'
})
let toastTimer = null

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
    toastTimer = null
  }, 2800)
}

const statusOptions = [
  { value: 1, label: 'Chờ Xác Nhận', desc: 'Đơn hàng mới tạo, đang chờ xác nhận từ cửa hàng', color: '#d97706', bg: '#fff4e5' },
  { value: 2, label: 'Đã Xác Nhận', desc: 'Đơn hàng đã được xác nhận, chuẩn bị đóng gói hàng', color: '#0369a1', bg: '#e0f2fe' },
  { value: 3, label: 'Chờ Vận Chuyển', desc: 'Hàng đã đóng gói xong, đang chờ bàn giao vận chuyển', color: '#8a3ee6', bg: '#f5edff' },
  { value: 4, label: 'Vận Chuyển', desc: 'Shipper / bên vận chuyển đang giao hàng tới khách', color: '#00838f', bg: '#e0f7fa' },
  { value: 5, label: 'Đã Hoàn Thành', desc: 'Đơn hàng đã giao thành công và hoàn tất toàn bộ', color: '#1b7a37', bg: '#e6f6ec' },
  { value: 6, label: 'Hủy', desc: 'Hủy đơn hàng và ngừng tiến trình xử lý', color: '#be2626', bg: '#fee6e6' }
]

const openStatusModal = (item) => {
  selectedInvoice.value = item
  targetStatus.value = Number(item.status) || 1
  statusNote.value = ''
  showStatusModal.value = true
}

const saveStatusChange = async () => {
  if (!selectedInvoice.value || updatingStatus.value) return
  const id = selectedInvoice.value.id
  const code = selectedInvoice.value.code
  const newStatus = Number(targetStatus.value)

  if (!newStatus || newStatus < 1 || newStatus > 6) {
    showToast('Vui lòng chọn trạng thái hợp lệ!', 'warning')
    return
  }

  updatingStatus.value = true
  try {
    let success = false
    try {
      if (id) {
        await api.put(`/api/hoa-don/${id}/trang-thai`, {
          trangThai: newStatus,
          ghiChu: statusNote.value.trim() || undefined
        })
        success = true
      }
    } catch {
      success = false
    }

    if (!success && code) {
      try {
        await api.put(`/api/hoa-don/code/${encodeURIComponent(code)}/trang-thai`, {
          trangThai: newStatus,
          ghiChu: statusNote.value.trim() || undefined
        })
        success = true
      } catch {
        success = false
      }
    }

    if (success) {
      showToast(`Đã cập nhật trạng thái đơn ${code} thành "${getStatusText(newStatus)}"!`)
      showStatusModal.value = false
      // Cập nhật ngay trong list hiện tại
      const found = invoiceList.value.find(i => i.id === id || i.code === code)
      if (found) {
        found.status = newStatus
      }
      await loadHoaDon()
    } else {
      showToast('Cập nhật trạng thái thất bại. Vui lòng thử lại!', 'warning')
    }
  } catch (error) {
    showToast(error.response?.data?.message || 'Cập nhật trạng thái thất bại!', 'warning')
  } finally {
    updatingStatus.value = false
  }
}

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', refreshHoaDonWhenVisible)
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
})
</script>

<template>
  <div class="hoa-don-container">
    <!-- Toast Popup Thông Báo -->
    <transition name="toast-fade">
      <div v-if="toast.show" class="toast-popup" :class="toast.type">
        <span class="toast-icon">✓</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>

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
              <div class="action-btn-group">
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
                <button
                    class="btn-action-edit-status"
                    title="Chỉnh sửa trạng thái"
                    @click="openStatusModal(item)"
                >
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>
              </div>
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

    <!-- MODAL CHỈNH SỬA TRẠNG THÁI HÓA ĐƠN -->
    <div v-if="showStatusModal" class="modal-backdrop" @click.self="showStatusModal = false">
      <div class="modal-container modal-edit-status">
        <div class="modal-header">
          <div>
            <h3 class="modal-title">Chỉnh Sửa Trạng Thái Hóa Đơn</h3>
            <p v-if="selectedInvoice" class="modal-subtitle">
              Mã: <strong>{{ selectedInvoice.code }}</strong> — Khách hàng: <strong>{{ selectedInvoice.customerName }}</strong>
            </p>
          </div>
          <button class="btn-close-modal" type="button" @click="showStatusModal = false">✕</button>
        </div>

        <div class="modal-body">
          <div class="form-group-modal">
            <label class="modal-label">Trạng thái hiện tại:</label>
            <div>
              <span v-if="selectedInvoice" class="badge-status" :class="getStatusClass(selectedInvoice.status)">
                {{ getStatusText(selectedInvoice.status) }}
              </span>
            </div>
          </div>

          <div class="form-group-modal mt-14">
            <label class="modal-label">Chọn trạng thái mới <span class="text-danger">*</span></label>
            <div class="status-options-grid">
              <label
                  v-for="st in statusOptions"
                  :key="st.value"
                  class="status-radio-card"
                  :class="{ active: targetStatus === st.value }"
                  :style="{ borderColor: targetStatus === st.value ? st.color : '#e2d8cd' }"
              >
                <input
                    type="radio"
                    name="listStatusRadio"
                    :value="st.value"
                    v-model="targetStatus"
                    class="radio-hidden"
                />
                <div
                    class="radio-circle"
                    :style="{
                      borderColor: st.color,
                      backgroundColor: targetStatus === st.value ? st.color : 'transparent'
                    }"
                >
                  <span v-if="targetStatus === st.value" class="radio-dot"></span>
                </div>
                <div class="status-card-content">
                  <div class="status-card-header">
                    <span
                        class="status-card-badge"
                        :style="{ color: st.color, backgroundColor: st.bg }"
                    >
                      {{ st.label }}
                    </span>
                    <span v-if="selectedInvoice && selectedInvoice.status === st.value" class="current-label">
                      (Hiện tại)
                    </span>
                  </div>
                  <p class="status-card-desc">{{ st.desc }}</p>
                </div>
              </label>
            </div>
          </div>

          <div class="form-group-modal mt-14">
            <label class="modal-label">Ghi chú cập nhật (Tùy chọn)</label>
            <textarea
                v-model="statusNote"
                rows="2"
                class="modal-textarea"
                placeholder="Nhập ghi chú thay đổi trạng thái nếu có..."
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-modal-cancel" type="button" @click="showStatusModal = false">
            Hủy bỏ
          </button>
          <button
              class="btn-modal-save"
              type="button"
              :disabled="updatingStatus"
              @click="saveStatusChange"
          >
            {{ updatingStatus ? 'Đang lưu...' : '✓ Lưu Thay Đổi' }}
          </button>
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

/* TOAST POPUP */
.toast-popup {
  position: fixed;
  top: 24px;
  right: 28px;
  background-color: #36536b;
  color: #ffffff;
  padding: 10px 18px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.88rem;
  font-weight: 600;
  z-index: 9999;
  border: 1px solid #4a6880;
}

.toast-popup.warning {
  background-color: #d97706;
  border-color: #b45309;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ACTION BUTTONS */
.action-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
}

.btn-action-edit-status {
  width: 32px;
  height: 32px;
  border: 1px solid #d5c8b8;
  background: #ffffff;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #d97706;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-action-edit-status:hover {
  background-color: #fff9f2;
  border-color: #d97706;
  color: #b45309;
}

/* MODAL CHỈNH SỬA TRẠNG THÁI */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(30, 41, 59, 0.55);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 16px;
}

.modal-container {
  background-color: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 580px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: modalIn 0.2s ease-out;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid #ebd9c8;
  background-color: #faf7f0;
}

.modal-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #274053;
  margin: 0;
}

.modal-subtitle {
  font-size: 0.82rem;
  color: #636b72;
  margin: 4px 0 0 0;
}

.btn-close-modal {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #8c969e;
  cursor: pointer;
  line-height: 1;
  padding: 4px 6px;
  border-radius: 4px;
}

.btn-close-modal:hover {
  color: #222222;
  background-color: #eee4d8;
}

.modal-body {
  padding: 20px;
  max-height: 75vh;
  overflow-y: auto;
}

.form-group-modal {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.modal-label {
  font-size: 0.86rem;
  font-weight: 700;
  color: #2e3a40;
}

.mt-14 {
  margin-top: 14px;
}

.text-danger {
  color: #dc2626;
}

.status-options-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-radio-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 14px;
  border: 1.5px solid #ded5c7;
  border-radius: 8px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-radio-card:hover {
  background-color: #fdfaf7;
}

.status-radio-card.active {
  background-color: #fbf7f2;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.radio-hidden {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.radio-circle {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #ccc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 3px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.radio-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #ffffff;
}

.status-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.status-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-card-badge {
  padding: 2px 9px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 700;
}

.current-label {
  font-size: 0.75rem;
  color: #8c969e;
  font-weight: 500;
}

.status-card-desc {
  margin: 0;
  font-size: 0.78rem;
  color: #636b72;
  line-height: 1.35;
}

.modal-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1.5px solid #d4c5b3;
  border-radius: 7px;
  font-size: 0.86rem;
  font-family: inherit;
  color: #333333;
  box-sizing: border-box;
  resize: vertical;
}

.modal-textarea:focus {
  outline: none;
  border-color: #3e5c76;
  box-shadow: 0 0 0 3px rgba(62, 92, 118, 0.12);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #ebd9c8;
  background-color: #faf7f0;
}

.btn-modal-cancel {
  padding: 8px 16px;
  border: 1.5px solid #d4c5b3;
  background-color: #ffffff;
  color: #555e65;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-cancel:hover {
  background-color: #f5ece2;
}

.btn-modal-save {
  padding: 8px 20px;
  border: none;
  background-color: #3e5c76;
  color: #ffffff;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-save:hover:not(:disabled) {
  background-color: #2b4357;
}

.btn-modal-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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