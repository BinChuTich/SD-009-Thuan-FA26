<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'

const route = useRoute()
const router = useRouter()

const maHoaDon = route.params.maHoaDon || route.params.ma

const hoaDon = ref(null)

const loading = ref(false)
const statusLoading = ref(false)
const thanhToanLoading = ref(false)
const errorMessage = ref('')

// ===============================
// MODAL THÔNG BÁO DẠNG POPUP (GIỐNG POPUP FORM)
// ===============================
const orderAlert = ref({
  show: false,
  message: '',
  type: 'success' // 'success' | 'error'
})
let alertTimer = null

const triggerAlert = (message, type = 'success') => {
  if (alertTimer) clearTimeout(alertTimer)
  orderAlert.value = {
    show: true,
    message,
    type
  }
  // Tự đóng sau 3 giây hoặc người dùng bấm xác nhận
  alertTimer = setTimeout(() => {
    orderAlert.value.show = false
  }, 3000)
}

// ===============================
// STATE CHỈNH SỬA ĐƠN HÀNG
// ===============================
const showEditModal = ref(false)
const editLoading = ref(false)
const editForm = ref({
  trangThai: 1,
  trangThaiThanhToan: 0,
  tenKhachHang: '',
  soDienThoaiKhachHang: '',
  diaChiNhanHang: '',
  ghiChu: ''
})

const moModalChinhSua = () => {
  if (!hoaDon.value) return
  editForm.value = {
    trangThai: Number(hoaDon.value.trangThai) || 1,
    trangThaiThanhToan: Number(hoaDon.value.trangThaiThanhToan) || 0,
    tenKhachHang: hoaDon.value.tenKhachHang || hoaDon.value.khachHang?.hoTen || '',
    soDienThoaiKhachHang: hoaDon.value.soDienThoaiKhachHang || hoaDon.value.soDienThoai || '',
    diaChiNhanHang: hoaDon.value.diaChiNhanHang || hoaDon.value.diaChi || '',
    ghiChu: hoaDon.value.ghiChu || ''
  }
  showEditModal.value = true
}

const dongModalChinhSua = () => {
  showEditModal.value = false
}

const luuChinhSua = async () => {
  if (!hoaDon.value) return
  try {
    editLoading.value = true
    await api.put(`/api/hoa-don/${hoaDon.value.id}`, {
      ...hoaDon.value,
      trangThai: Number(editForm.value.trangThai),
      trangThaiThanhToan: Number(editForm.value.trangThaiThanhToan),
      tenKhachHang: editForm.value.tenKhachHang,
      soDienThoaiKhachHang: editForm.value.soDienThoaiKhachHang,
      diaChiNhanHang: editForm.value.diaChiNhanHang,
      ghiChu: editForm.value.ghiChu
    })

    hoaDon.value.trangThai = Number(editForm.value.trangThai)
    hoaDon.value.trangThaiThanhToan = Number(editForm.value.trangThaiThanhToan)
    hoaDon.value.tenKhachHang = editForm.value.tenKhachHang
    hoaDon.value.soDienThoaiKhachHang = editForm.value.soDienThoaiKhachHang
    hoaDon.value.diaChiNhanHang = editForm.value.diaChiNhanHang
    hoaDon.value.ghiChu = editForm.value.ghiChu

    showEditModal.value = false
    triggerAlert('Cập nhật thông tin đơn hàng thành công!', 'success')
  } catch (error) {
    console.error('Lỗi lưu thông tin chỉnh sửa:', error)
    triggerAlert('Không thể lưu thông tin đơn hàng!', 'error')
  } finally {
    editLoading.value = false
  }
}

// ===============================
// LOAD HÓA ĐƠN
// ===============================
const loadHoaDon = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const response = await api.get(`/api/hoa-don/code/${maHoaDon}`)
    hoaDon.value = response.data
  } catch (error) {
    console.error('Lỗi lấy hóa đơn:', error)
    if (error.response?.status === 404) {
      errorMessage.value = 'Không tìm thấy hóa đơn: ' + maHoaDon
    } else {
      errorMessage.value = 'Không thể tải thông tin hóa đơn.'
    }
  } finally {
    loading.value = false
  }
}

// ===============================
// FORMAT TIỀN
// ===============================
const formatMoney = (money) => {
  if (money == null) return '0 ₫'
  return Number(money).toLocaleString('vi-VN') + ' ₫'
}

// ===============================
// FORMAT NGÀY + GIỜ
// ===============================
const formatDateTime = (date) => {
  if (!date) return '---'
  return new Date(date).toLocaleString('vi-VN')
}

// ===============================
// TRẠNG THÁI
// ===============================
const statusList = [
  { id: 1, name: 'Chờ xác nhận', shortName: 'Hóa đơn chờ' },
  { id: 2, name: 'Đã xác nhận', shortName: 'Đã xác nhận' },
  { id: 3, name: 'Chờ vận chuyển', shortName: 'Chờ vận chuyển' },
  { id: 4, name: 'Vận chuyển', shortName: 'Đang vận chuyển' },
  { id: 5, name: 'Đã hoàn thành', shortName: 'Hoàn thành' }
]

const getStatusText = (status) => {
  const map = {
    1: 'Chờ xác nhận',
    2: 'Đã xác nhận',
    3: 'Chờ vận chuyển',
    4: 'Vận chuyển',
    5: 'Đã hoàn thành',
    6: 'Hủy'
  }
  return map[Number(status)] || 'Chưa cập nhật'
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

// ===============================
// TRẠNG THÁI THANH TOÁN
// ===============================
const getPaymentText = (status) => {
  return Number(status) === 1 ? 'Đã thanh toán' : 'Chưa thanh toán'
}

const getPaymentClass = (status) => {
  return Number(status) === 1 ? 'payment-paid' : 'payment-unpaid'
}

// ===============================
// TỔNG THANH TOÁN
// ===============================
const tongThanhToan = computed(() => {
  if (!hoaDon.value) return 0
  const tongTien = Number(hoaDon.value.tongTien || 0)
  const phiShip = Number(hoaDon.value.phiShip || 0)
  const giamGia = Number(hoaDon.value.tongTienGiamGia || 0)
  return Math.max(0, tongTien + phiShip - giamGia)
})

// ===============================
// TRẠNG THÁI HIỆN TẠI
// ===============================
const currentStatus = computed(() => {
  if (!hoaDon.value) return 0
  return Number(hoaDon.value.trangThai)
})

const canAction = computed(() => {
  return [1, 2, 3, 4].includes(currentStatus.value)
})

const actionText = computed(() => {
  switch (currentStatus.value) {
    case 1:
      return '✓ Xác nhận đơn hàng'
    case 2:
      return '✓ Chuyển chờ vận chuyển'
    case 3:
      return '✓ Chuyển vận chuyển'
    case 4:
      return '✓ Hoàn thành đơn hàng'
    default:
      return ''
  }
})

// ===============================
// CHUYỂN TRẠNG THÁI
// ===============================
const xuLyDonHang = async () => {
  if (!hoaDon.value) return
  const hienTai = Number(hoaDon.value.trangThai)
  if (![1, 2, 3, 4].includes(hienTai)) return

  const trangThaiMoi = hienTai + 1
  try {
    statusLoading.value = true
    await api.put(`/api/hoa-don/${hoaDon.value.id}`, {
      trangThai: trangThaiMoi
    })
    hoaDon.value.trangThai = trangThaiMoi
    triggerAlert(`Đã chuyển trạng thái sang: ${getStatusText(trangThaiMoi)}`, 'success')
  } catch (error) {
    console.error('Lỗi cập nhật trạng thái:', error)
    triggerAlert('Không thể cập nhật trạng thái đơn hàng!', 'error')
  } finally {
    statusLoading.value = false
  }
}

// ===============================
// THANH TOÁN
// ===============================
const thanhToan = async () => {
  if (!hoaDon.value) return
  if (Number(hoaDon.value.trangThaiThanhToan) === 1) return

  try {
    thanhToanLoading.value = true
    await api.put(`/api/hoa-don/${hoaDon.value.id}`, {
      trangThaiThanhToan: 1
    })
    hoaDon.value.trangThaiThanhToan = 1
    triggerAlert('Đã xác nhận thanh toán thành công!', 'success')
  } catch (error) {
    console.error('Lỗi cập nhật thanh toán:', error)
    triggerAlert('Không thể cập nhật thanh toán!', 'error')
  } finally {
    thanhToanLoading.value = false
  }
}

const quayLai = () => {
  router.push('/hoa-don')
}

const inHoaDon = () => {
  window.print()
}

onMounted(() => {
  loadHoaDon()
})
</script>

<template>
  <div class="invoice-page">
    <!-- ================= HEADER ================= -->
    <div class="page-header">
      <div>
        <div class="breadcrumb">
          <span>Hóa đơn</span>
          <b>/</b>
          <strong>Chi tiết hóa đơn</strong>
        </div>

        <div v-if="hoaDon" class="invoice-meta">
          <span>Mã đơn hàng: <strong>{{ hoaDon.maHoaDon }}</strong></span>
          <span>|</span>
          <span>Ngày tạo: <strong>{{ formatDateTime(hoaDon.ngayTao) }}</strong></span>
          <br />
          <span>Tạo bởi: <strong>{{ hoaDon.nguoiTao || 'Admin' }}</strong></span>
          <span>|</span>
          <span>Cập nhật gần nhất: <strong>{{ formatDateTime(hoaDon.ngayCapNhat || hoaDon.ngayTao) }}</strong></span>
        </div>
      </div>

      <div class="header-actions">
        <!-- NÚT CHỈNH SỬA ĐƠN HÀNG -->
        <button class="btn-edit" @click="moModalChinhSua">
          ✏ Chỉnh sửa đơn hàng
        </button>

        <button class="btn-pos" @click="quayLai">
          ⇆ Quay lại Bán hàng tại quầy
        </button>

        <button class="btn-list" @click="quayLai">
          ← Quay lại danh sách
        </button>
      </div>
    </div>

    <!-- ================= LOADING ================= -->
    <div v-if="loading" class="loading">
      Đang tải thông tin hóa đơn...
    </div>

    <!-- ================= ERROR ================= -->
    <div v-if="errorMessage" class="error-box">
      {{ errorMessage }}
    </div>

    <!-- ================= CONTENT ================= -->
    <div v-if="hoaDon && !loading" class="detail-layout">
      <!-- ================= LEFT ================= -->
      <div class="left-column">
        <!-- TRẠNG THÁI -->
        <div class="card status-card">
          <div class="card-title">
            <div>
              <span class="title-icon">▣</span>
              Trạng Thái Đơn Hàng
            </div>
            <span class="current-status" :class="getStatusClass(hoaDon.trangThai)">
              {{ getStatusText(hoaDon.trangThai) }}
            </span>
          </div>

          <div class="status-timeline">
            <div
                v-for="item in statusList"
                :key="item.id"
                class="timeline-item"
                :class="{
                active: currentStatus >= item.id,
                current: currentStatus === item.id
              }"
            >
              <div class="timeline-icon">
                <span v-if="currentStatus > item.id">✓</span>
                <span v-else-if="currentStatus === item.id">{{ item.id === 5 ? '⚑' : '⌛' }}</span>
                <span v-else>{{ item.id }}</span>
              </div>
              <div class="timeline-name">
                {{ item.shortName }}
              </div>
              <div v-if="currentStatus >= item.id" class="timeline-date">
                {{ item.id === 1 ? formatDateTime(hoaDon.ngayTao) : '' }}
              </div>
            </div>
          </div>

          <div class="status-action">
            <button
                v-if="canAction"
                class="btn-status"
                :disabled="statusLoading"
                @click="xuLyDonHang"
            >
              {{ statusLoading ? 'Đang cập nhật...' : actionText }}
            </button>
            <span v-else-if="currentStatus === 5" class="completed-box">
              ✓ Đơn hàng đã hoàn thành
            </span>
            <span v-else-if="currentStatus === 6" class="cancelled-box">
              ✕ Đơn hàng đã hủy
            </span>
          </div>
        </div>

        <!-- KHÁCH HÀNG & GIAO HÀNG -->
        <div class="two-column">
          <div class="card">
            <div class="card-title">
              <span>♙ Thông Tin Khách Hàng</span>
            </div>
            <div class="info-list">
              <div class="info-row">
                <span>Tên Khách Hàng</span>
                <strong>{{ hoaDon.tenKhachHang || hoaDon.khachHang?.hoTen || 'Khách vãng lai' }}</strong>
              </div>
              <div class="info-row">
                <span>Số Điện Thoại</span>
                <strong>{{ hoaDon.soDienThoaiKhachHang || hoaDon.soDienThoai || '---' }}</strong>
              </div>
              <div class="info-row">
                <span>Email</span>
                <strong>{{ hoaDon.email || 'Không có' }}</strong>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-title">
              <span>♧ Thông Tin Giao Hàng</span>
            </div>
            <div class="info-list">
              <div class="info-row">
                <span>Địa Chỉ</span>
                <strong>{{ hoaDon.diaChiNhanHang || hoaDon.diaChi || '---' }}</strong>
              </div>
              <div class="info-row">
                <span>Loại Đơn</span>
                <strong>{{ Number(hoaDon.loaiDon) === 1 ? 'Cửa hàng' : 'Online' }}</strong>
              </div>
              <div class="info-row">
                <span>Ghi Chú</span>
                <strong>{{ hoaDon.ghiChu || '---' }}</strong>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= RIGHT ================= -->
      <div class="right-column">
        <!-- TỔNG KẾT -->
        <div class="card payment-summary">
          <div class="card-title">
            <span>▣ Tổng Kết Thanh Toán</span>
          </div>

          <div class="summary-row">
            <span>Tổng Tiền Hàng</span>
            <strong>{{ formatMoney(hoaDon.tongTien) }}</strong>
          </div>

          <div class="summary-row">
            <span>Phí Vận Chuyển</span>
            <strong>{{ formatMoney(hoaDon.phiShip) }}</strong>
          </div>

          <div class="summary-row discount">
            <span>Phiếu Giảm Giá</span>
            <strong>{{ formatMoney(hoaDon.tongTienGiamGia) }}</strong>
          </div>

          <div class="summary-total">
            <span>Tổng Tiền</span>
            <strong>{{ formatMoney(tongThanhToan) }}</strong>
          </div>
        </div>

        <!-- LỊCH SỬ THANH TOÁN -->
        <div class="card">
          <div class="card-title">
            <span>◷ Lịch Sử Thanh Toán</span>
          </div>

          <div class="payment-history">
            <div class="payment-item">
              <div>
                <strong>Tiền mặt</strong>
                <p>Thanh toán đơn cửa hàng</p>
                <small>{{ formatDateTime(hoaDon.ngayTao) }}</small>
              </div>

              <div class="payment-right">
                <span class="payment-badge" :class="getPaymentClass(hoaDon.trangThaiThanhToan)">
                  {{ getPaymentText(hoaDon.trangThaiThanhToan) }}
                </span>
                <strong>{{ formatMoney(tongThanhToan) }}</strong>
              </div>
            </div>
          </div>

          <div class="payment-action">
            <button
                v-if="Number(hoaDon.trangThaiThanhToan) === 0"
                class="btn-payment"
                :disabled="thanhToanLoading"
                @click="thanhToan"
            >
              {{ thanhToanLoading ? 'Đang cập nhật...' : '✓ Xác nhận đã thanh toán' }}
            </button>
            <div v-else class="paid-success">
              ✓ Đã thanh toán
            </div>
          </div>
        </div>

        <button class="btn-print" @click="inHoaDon">
          🖨 In Hóa Đơn
        </button>
      </div>
    </div>

    <!-- ================= MODAL CHỈNH SỬA ĐƠN HÀNG ================= -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="dongModalChinhSua">
      <div class="modal-box">
        <div class="modal-header">
          <div class="modal-title-with-icon">
            <span class="modal-title-icon-edit">✏</span>
            <h3>Chỉnh Sửa Thông Tin Đơn Hàng</h3>
          </div>
          <button class="btn-close" @click="dongModalChinhSua">✕</button>
        </div>

        <div class="modal-body">
          <!-- 1. Cụm đổi Trạng thái đơn hàng & Thanh toán -->
          <div class="form-row-2">
            <div class="form-group">
              <label>Trạng Thái Đơn Hàng</label>
              <select v-model="editForm.trangThai">
                <option :value="1">Chờ xác nhận</option>
                <option :value="2">Đã xác nhận</option>
                <option :value="3">Chờ vận chuyển</option>
                <option :value="4">Đang vận chuyển</option>
                <option :value="5">Đã hoàn thành</option>
                <option :value="6">Đã hủy</option>
              </select>
            </div>

            <div class="form-group">
              <label>Trạng Thái Thanh Toán</label>
              <select v-model="editForm.trangThaiThanhToan">
                <option :value="0">Chưa thanh toán</option>
                <option :value="1">Đã thanh toán</option>
              </select>
            </div>
          </div>

          <!-- 2. Thông tin khách hàng & Giao hàng -->
          <div class="form-row-2">
            <div class="form-group">
              <label>Tên Khách Hàng</label>
              <input
                  type="text"
                  v-model="editForm.tenKhachHang"
                  placeholder="Nhập tên khách hàng..."
              />
            </div>

            <div class="form-group">
              <label>Số Điện Thoại</label>
              <input
                  type="text"
                  v-model="editForm.soDienThoaiKhachHang"
                  placeholder="Nhập số điện thoại..."
              />
            </div>
          </div>

          <div class="form-group">
            <label>Địa Chỉ Giao Hàng</label>
            <textarea
                rows="2"
                v-model="editForm.diaChiNhanHang"
                placeholder="Nhập địa chỉ giao hàng..."
            ></textarea>
          </div>

          <div class="form-group">
            <label>Ghi Chú</label>
            <textarea
                rows="2"
                v-model="editForm.ghiChu"
                placeholder="Nhập ghi chú cho đơn hàng..."
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-modal-cancel" @click="dongModalChinhSua">Hủy</button>
          <button
              class="btn-modal-save"
              :disabled="editLoading"
              @click="luuChinhSua"
          >
            {{ editLoading ? 'Đang lưu...' : 'Lưu Thay Đổi' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ================= MODAL THÔNG BÁO (POPUP ĐỒNG BỘ NGUYÊN BẢN VỚI FORM) ================= -->
    <div v-if="orderAlert.show" class="modal-overlay" @click.self="orderAlert.show = false">
      <div class="modal-box alert-modal-box">
        <div class="modal-header">
          <div class="modal-title-with-icon">
            <span class="modal-title-icon-alert" :class="orderAlert.type">
              {{ orderAlert.type === 'success' ? '✓' : '!' }}
            </span>
            <h3>{{ orderAlert.type === 'success' ? 'Thông Báo Thành Công' : 'Thông Báo Hệ Thống' }}</h3>
          </div>
          <button class="btn-close" @click="orderAlert.show = false">✕</button>
        </div>

        <div class="modal-body alert-modal-body">
          <p class="alert-main-text">{{ orderAlert.message }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-modal-save" @click="orderAlert.show = false">
            Xác Nhận
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
*, *::before, *::after {
  box-sizing: border-box;
}

.invoice-page {
  min-height: 100vh;
  background-color: var(--bg, #f7f5ef);
  padding: 1.25rem 1.75rem 2.5rem;
  color: var(--text, #3d4a50);
  font-family: var(--system-font, sans-serif);
}

/* ================= MODAL TITLE & ICON GIỐNG ẢNH ================= */
.modal-title-with-icon {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.modal-title-icon-edit {
  color: var(--blue, #496883);
  font-size: 1.15rem;
  font-weight: 700;
}

.modal-title-icon-alert {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 0.85rem;
  font-weight: 800;
}

.modal-title-icon-alert.success {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #cce3d1;
}

.modal-title-icon-alert.error {
  background-color: #fbeeed;
  color: #c94343;
  border: 1px solid #f6d4d4;
}

.alert-modal-box {
  width: 440px !important;
  max-width: 90%;
}

.alert-modal-body {
  padding: 1.5rem 1.4rem !important;
  text-align: center;
}

.alert-main-text {
  font-size: 0.98rem;
  font-weight: 600;
  color: #3d4a50;
  line-height: 1.5;
  margin: 0;
}

/* ================= MODAL BODY SELECT & ROW ================= */
.modal-body .form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.modal-body select {
  width: 100%;
  height: 2.5rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  box-sizing: border-box;
  cursor: pointer;
}

.modal-body select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

/* ================= HEADER ================= */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.3rem;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  margin-bottom: 0.6rem;
}

.breadcrumb span {
  color: #8c9597;
}

.breadcrumb b {
  color: #d6d0c3;
  font-weight: normal;
}

.breadcrumb strong {
  color: var(--blue, #496883);
  font-weight: 700;
}

.invoice-meta {
  color: #7b8587;
  font-size: 0.85rem;
  line-height: 1.8;
}

.invoice-meta strong {
  color: #3d4a50;
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 0.65rem;
  align-items: center;
}

/* Nút Header */
.btn-pos,
.btn-list,
.btn-edit {
  padding: 0.55rem 1.15rem;
  border-radius: 7px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-edit {
  background-color: var(--gold, #d2a764) !important;
  color: #ffffff !important;
  border: 1px solid var(--gold, #d2a764) !important;
}

.btn-edit:hover {
  background-color: #be9453 !important;
  border-color: #be9453 !important;
}

.btn-pos {
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  border: 1px solid var(--blue, #496883) !important;
}

.btn-pos:hover {
  background-color: #38536b !important;
}

.btn-list {
  background-color: #ffffff !important;
  color: var(--blue, #496883) !important;
  border: 1px solid var(--line, #e9e5db) !important;
}

.btn-list:hover {
  background-color: #eaf1f4 !important;
}

/* ================= LAYOUT ================= */
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 1.25rem;
  align-items: start;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 10px;
  padding: 1.35rem 1.5rem;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.9rem;
  border-bottom: 1px solid #f0ece3;
  color: #3e4e56;
  font-size: 1.05rem;
  font-weight: 700;
}

.card-title > div,
.card-title > span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* ================= STATUS BADGES ================= */
.current-status {
  padding: 0.35rem 0.85rem;
  border-radius: 14px;
  font-size: 0.82rem;
  font-weight: 700;
}

.status-pending {
  background-color: #f7eee1;
  color: #b18b52;
}

.status-confirmed {
  background-color: #eaf1f5;
  color: var(--blue, #496883);
}

.status-waiting {
  background-color: #fdf5ea;
  color: #c48c3b;
}

.status-shipping {
  background-color: #e3edf3;
  color: #38627e;
}

.status-completed {
  background-color: #edf5ef;
  color: #558764;
}

.status-cancelled {
  background-color: #fbeeed;
  color: #c94343;
}

.status-default {
  background-color: #f3f1ec;
  color: #7b8587;
}

/* ================= TIMELINE ================= */
.status-card {
  min-height: 270px;
}

.status-timeline {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 2.2rem 1.8rem 1rem;
}

.status-timeline::before {
  content: '';
  position: absolute;
  top: 54px;
  left: 60px;
  right: 60px;
  height: 2px;
  background-color: #e6e0d5;
}

.timeline-item {
  position: relative;
  z-index: 1;
  width: 110px;
  text-align: center;
}

.timeline-icon {
  width: 44px;
  height: 44px;
  margin: 0 auto 0.6rem;
  border-radius: 50%;
  background-color: #f7f5ef;
  color: #9aa0a0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.05rem;
  font-weight: 700;
  border: 3px solid #ffffff;
  box-shadow: 0 0 0 1px var(--line, #e9e5db);
  transition: all 0.2s ease;
}

.timeline-item.active .timeline-icon {
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  box-shadow: 0 0 0 2px var(--blue, #496883) !important;
}

.timeline-item.current .timeline-icon {
  box-shadow: 0 0 0 4px rgba(73, 104, 131, 0.2) !important;
}

.timeline-name {
  font-size: 0.85rem;
  color: #7b8587;
  line-height: 1.35;
}

.timeline-item.active .timeline-name {
  color: var(--blue, #496883) !important;
  font-weight: 700;
}

.timeline-date {
  font-size: 0.75rem;
  color: #9aa0a0;
  margin-top: 4px;
}

/* ================= STATUS BUTTON ================= */
.status-action {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.1rem;
  padding-top: 1rem;
  border-top: 1px dashed #efeae0;
}

.btn-status {
  border: 0;
  border-radius: 7px;
  padding: 0.6rem 1.35rem;
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-status:hover {
  background-color: #38536b !important;
}

.btn-status:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.completed-box {
  padding: 0.6rem 1.1rem;
  border-radius: 7px;
  background-color: #edf5ef;
  color: #558764;
  font-size: 0.88rem;
  font-weight: 700;
}

.cancelled-box {
  padding: 0.6rem 1.1rem;
  border-radius: 7px;
  background-color: #fbeeed;
  color: #c94343;
  font-size: 0.88rem;
  font-weight: 700;
}

/* ================= TWO COLUMN ================= */
.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}

.info-list {
  padding-top: 0.4rem;
}

.info-row {
  min-height: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  border-bottom: 1px solid #f3efe8;
  font-size: 0.9rem;
}

.info-row:last-child {
  border-bottom: 0;
}

.info-row span {
  color: #7b8587;
}

.info-row strong {
  color: #3d4a50;
  font-weight: 600;
  text-align: right;
}

/* ================= PAYMENT SUMMARY ================= */
.payment-summary {
  padding-bottom: 0.6rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.85rem 0;
  border-bottom: 1px solid #f3efe8;
  font-size: 0.92rem;
}

.summary-row span {
  color: #7b8587;
}

.summary-row strong {
  color: #3d4a50;
}

.summary-row.discount strong {
  color: #558764;
}

.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.1rem 0 0.5rem;
  font-weight: 800;
  border-top: 1px dashed #e8e2d5;
  margin-top: 0.4rem;
}

.summary-total span {
  color: #3d4a50;
  font-size: 1.05rem;
}

.summary-total strong {
  color: #d97706 !important;
  font-size: 1.45rem;
}

/* ================= PAYMENT HISTORY ================= */
.payment-history {
  padding-top: 0.6rem;
}

.payment-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  background-color: #faf9f6;
  border: 1px solid #eeebe3;
  border-radius: 8px;
  padding: 0.85rem;
  margin-bottom: 0.65rem;
}

.payment-item strong {
  font-size: 0.92rem;
  color: #3d4a50;
}

.payment-item p {
  margin: 4px 0;
  color: #7b8587;
  font-size: 0.85rem;
}

.payment-item small {
  color: #9aa0a0;
  font-size: 0.78rem;
}

.payment-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
}

.payment-right > strong {
  color: #d97706 !important;
  font-size: 0.95rem;
}

.payment-badge {
  padding: 0.25rem 0.65rem;
  border-radius: 12px;
  font-size: 0.78rem;
  font-weight: 700;
}

.payment-paid {
  background-color: #edf5ef;
  color: #558764;
}

.payment-unpaid {
  background-color: #f7eee1;
  color: #b18b52;
}

/* Nút thanh toán */
.payment-action {
  margin-top: 1rem;
}

.btn-payment {
  width: 100%;
  padding: 0.75rem;
  border: 0;
  border-radius: 8px;
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-payment:hover {
  background-color: #38536b !important;
}

.btn-payment:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.paid-success {
  width: 100%;
  padding: 0.75rem;
  border-radius: 8px;
  background-color: #edf5ef;
  color: #558764;
  text-align: center;
  font-size: 0.92rem;
  font-weight: 700;
}

/* Nút In Hóa Đơn */
.btn-print {
  width: 100%;
  padding: 0.85rem;
  border: 0;
  border-radius: 8px;
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-print:hover {
  background-color: #38536b !important;
}

/* ================= MODAL CHỈNH SỬA ================= */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-box {
  background: #ffffff;
  width: 520px;
  max-width: 92%;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  animation: modalFadeIn 0.2s ease;
}

.modal-header {
  padding: 1.1rem 1.4rem;
  background-color: #faf9f6;
  border-bottom: 1px solid #efeae0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--blue, #496883);
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #8c9597;
  cursor: pointer;
}

.btn-close:hover {
  color: #333;
}

.modal-body {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.modal-body .form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.modal-body label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #4f5d63;
}

.modal-body input,
.modal-body textarea {
  width: 100%;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  font-size: 0.92rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  box-sizing: border-box;
}

.modal-body input:focus,
.modal-body textarea:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

.modal-footer {
  padding: 1rem 1.4rem;
  background-color: #faf9f6;
  border-top: 1px dashed #efeae0;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-modal-cancel {
  background: #ffffff;
  color: #647074;
  border: 1px solid #ded8cb;
  padding: 0.55rem 1.2rem;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

.btn-modal-save {
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  padding: 0.55rem 1.4rem;
  border-radius: 6px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-modal-save:hover {
  background: #38536b;
}

.btn-modal-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Loading & Error */
.loading {
  background: #ffffff;
  padding: 3rem;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  text-align: center;
  color: #7b8587;
  font-size: 0.95rem;
}

.error-box {
  background-color: #fbeeed;
  color: #c94343;
  padding: 1rem 1.25rem;
  border-radius: 8px;
  border: 1px solid #f6d4d4;
  font-size: 0.92rem;
}

/* Responsive */
@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .right-column {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .btn-print {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .invoice-page {
    padding: 1rem;
  }
  .page-header {
    flex-direction: column;
    gap: 1rem;
  }
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  .header-actions button {
    flex: 1;
  }
  .two-column {
    grid-template-columns: 1fr;
  }
  .right-column {
    display: flex;
  }
  .status-timeline {
    padding-left: 0;
    padding-right: 0;
  }
  .status-timeline::before {
    left: 20px;
    right: 20px;
  }
  .timeline-name {
    font-size: 0.75rem;
  }
}

@media print {
  .invoice-page {
    background: #ffffff;
    padding: 0;
  }
  .header-actions,
  .status-action,
  .payment-action,
  .btn-print,
  .modal-overlay {
    display: none !important;
  }
  .card {
    box-shadow: none;
    border: 1px solid #ddd;
    break-inside: avoid;
  }
}
</style>