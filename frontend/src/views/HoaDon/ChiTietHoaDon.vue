<script setup>
import {ref, onMounted, computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import api from '@/api.js'

const route = useRoute()
const router = useRouter()

// Lấy mã từ params URL (ví dụ HDMKT1 hoặc HD001)
const maHoaDon = route.params.ma

const hoaDon = ref(null)
const loading = ref(false)
const errorMessage = ref('')

// Trạng thái bước hiện tại của Timeline Stepper (1 đến 5)
const currentStep = ref(1)

// Danh sách 5 bước trạng thái đúng theo giao diện
const steps = [
  {id: 1, name: 'Chờ Xác Nhận', icon: '🛒'},
  {id: 2, name: 'Đã Xác Nhận Thông Tin\nThanh Toán', icon: '💳'},
  {id: 3, name: 'Chờ Lấy Vận Chuyển', icon: '📦'},
  {id: 4, name: 'Vận Chuyển', icon: '🚚'},
  {id: 5, name: 'Đã Hoàn Thành', icon: '⭐'}
]

const quayLai = () => {
  router.push('/hoa-don')
}

const formatMoney = (money) => {
  if (money == null) return '0 đ'
  return Number(money).toLocaleString('vi-VN') + ' đ'
}

// Map trạng thái từ chuỗi backend sang bước tương ứng
const mapStatusToStep = (status) => {
  if (!status) return 1
  const s = status.toString().toLowerCase()
  if (s.includes('xác nhận thanh toán') || s.includes('đã xác nhận')) return 2
  if (s.includes('chờ lấy') || s.includes('chờ vận chuyển')) return 3
  if (s.includes('vận chuyển') || s.includes('đang giao')) return 4
  if (s.includes('hoàn thành') || s.includes('thành công')) return 5
  return 1
}

const loadChiTiet = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const res = await api.get(`/api/hoa-don`)
    const found = res.data.find(item => item.maHoaDon === maHoaDon)

    if (found) {
      hoaDon.value = found
      currentStep.value = mapStatusToStep(found.trangThai)
    } else {
      errorMessage.value = 'Không tìm thấy thông tin hóa đơn!'
    }
  } catch (err) {
    console.error('Lỗi lấy chi tiết:', err)
    errorMessage.value = 'Không thể tải chi tiết hóa đơn.'
  } finally {
    loading.value = false
  }
}

// Chuyển bước kế tiếp
const nextStep = () => {
  if (currentStep.value < 5) {
    currentStep.value++
  } else {
    alert('Đơn hàng đã ở trạng thái hoàn thành!')
  }
}

// Hủy đơn
const cancelOrder = () => {
  if (confirm(`Bạn có chắc muốn hủy đơn hàng ${maHoaDon} và hoàn tiền không?`)) {
    alert('Đã ghi nhận yêu cầu hủy đơn và hoàn tiền!')
  }
}

const openInvoiceHistory = () => {
  alert(`Xem lịch sử thao tác của hóa đơn: ${maHoaDon}`)
}

const openPaymentHistory = () => {
  alert(`Xem lịch sử thanh toán của hóa đơn: ${maHoaDon}`)
}

const updateOrder = () => {
  alert(`Cập nhật thông tin đơn hàng ${maHoaDon} thành công!`)
}

onMounted(() => {
  loadChiTiet()
})
</script>

<template>
  <div class="order-detail-wrapper">
    <!-- 1. Header trên cùng -->
    <div class="top-header-bar">
      <div class="header-left">
        <button class="btn-back" @click="quayLai" title="Quay lại">←</button>
        <h2 class="page-title">Quản Lý Hóa Đơn</h2>
      </div>
    </div>

    <!-- Thông báo Đang tải / Lỗi -->
    <div v-if="loading" class="content-card status-msg-card">
      <div class="spinner"></div>
      <span>Đang tải thông tin chi tiết hóa đơn...</span>
    </div>

    <div v-else-if="errorMessage" class="content-card status-msg-card error">
      <span>⚠️ {{ errorMessage }}</span>
      <button class="btn btn-primary" style="margin-top: 10px;" @click="loadChiTiet">Thử lại</button>
    </div>

    <!-- Giao diện chính hiển thị khi đã nạp dữ liệu -->
    <div v-else-if="hoaDon" class="order-detail-body">
      <!-- 2. Khối Trạng Thái Đơn Hàng (Timeline Stepper) -->
      <div class="content-card status-timeline-card">
        <div class="card-head-title">
          <h3>Trạng Thái Đơn Hàng</h3>
        </div>

        <div class="timeline-container">
          <div class="timeline-track">
            <!-- Đường nối xám -->
            <div class="timeline-line"></div>
            <!-- Đường nối tiến độ xanh chạy theo bước active -->
            <div
                class="timeline-line-progress"
                :style="{ width: ((currentStep - 1) / (steps.length - 1)) * 100 + '%' }"
            ></div>

            <!-- Các nút tròn bước trạng thái -->
            <div
                v-for="step in steps"
                :key="step.id"
                class="timeline-step"
                :class="{
                active: currentStep === step.id,
                completed: currentStep > step.id
              }"
            >
              <div class="step-circle">
                <span class="step-icon">{{ step.icon }}</span>
              </div>
              <span class="step-label">{{ step.name }}</span>
            </div>
          </div>
        </div>

        <!-- Hàng nút hành động dưới Stepper -->
        <div class="timeline-action-bar">
          <div class="left-actions">
            <button class="btn btn-primary" @click="nextStep">Tiếp tục</button>
            <button class="btn btn-danger" @click="cancelOrder">Hủy đơn và hoàn tiền</button>
          </div>

          <div class="right-actions">
            <button class="btn btn-primary" @click="openInvoiceHistory">
              📋 LỊCH SỬ HÓA ĐƠN
            </button>
          </div>
        </div>
      </div>

      <!-- 3. Khối Thông tin đơn hàng & Bảng sản phẩm -->
      <div class="content-card product-table-card">
        <div class="order-info-header">
          <span class="order-code-title">
            Thông tin đơn hàng có mã hóa đơn: <b>{{ hoaDon.maHoaDon }}</b>
          </span>
          <button class="btn btn-primary btn-history-sub" @click="openPaymentHistory">
            📋 Lịch sử thanh toán
          </button>
        </div>

        <div class="table-responsive">
          <table class="custom-table">
            <thead>
            <tr>
              <th>Sản phẩm</th>
              <th style="width: 100px; text-align: center;">Số Lượng</th>
              <th style="width: 100px; text-align: center;">Kho</th>
              <th style="width: 150px; text-align: right;">Giá hiện tại</th>
              <th style="width: 150px; text-align: right;">Giá được tính</th>
              <th style="width: 160px; text-align: right;">Tổng</th>
            </tr>
            </thead>
            <tbody>
            <!-- Nếu backend có mảng danhSachSanPham hoặc hoaDonChiTiet -->
            <tr
                v-for="(item, idx) in (hoaDon.chiTietList || hoaDon.danhSachSanPham || [
                  {
                    id: 1,
                    tenSanPham: 'Áo phông nam FF',
                    phanLoai: 'Size L / Xanh Navy',
                    soLuong: 2,
                    kho: 50,
                    giaHienTai: 750000,
                    giaDuocTinh: 750000,
                    anh: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
                  }
                ])"
                :key="item.id || idx"
            >
              <td>
                <div class="product-info-cell">
                  <div class="product-thumb">
                    <img
                        :src="item.anh || item.hinhAnh || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'"
                        :alt="item.tenSanPham"
                    />
                  </div>
                  <div class="product-desc">
                    <span class="prod-name">{{ item.tenSanPham || 'Áo phông cộc tay' }}</span>
                    <span class="prod-variant">{{ item.phanLoai || item.bienThe || 'Màu tiêu chuẩn' }}</span>
                  </div>
                </div>
              </td>
              <td style="text-align: center;">{{ item.soLuong || 1 }}</td>
              <td style="text-align: center;">{{ item.kho || 50 }}</td>
              <td style="text-align: right;">{{ formatMoney(item.giaHienTai || hoaDon.tongTien) }}</td>
              <td style="text-align: right;">{{ formatMoney(item.giaDuocTinh || hoaDon.tongTien) }}</td>
              <td style="text-align: right;" class="font-bold text-dark">
                {{ formatMoney((item.giaDuocTinh || hoaDon.tongTien) * (item.soLuong || 1)) }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 4. Khối dưới 2 cột: Thông tin khách hàng & Hóa đơn -->
      <div class="bottom-split-grid">
        <!-- Cột trái: Thông tin khách hàng -->
        <div class="content-card customer-section">
          <h3 class="section-title">Thông tin khách hàng</h3>

          <div class="form-vertical">
            <div class="form-group">
              <label>Địa Chỉ</label>
              <input
                  type="text"
                  :value="hoaDon.diaChi || hoaDon.khachHang?.diaChi || '123 Đường ABC, Quận XYZ, HN'"
              />
            </div>

            <div class="form-row-2">
              <div class="form-group">
                <label>Tên Người Nhận</label>
                <input
                    type="text"
                    :value="hoaDon.khachHang?.hoTen || hoaDon.tenNguoiNhan || 'Nguyễn Văn A'"
                />
              </div>
              <div class="form-group">
                <label>Số Điện Thoại</label>
                <input
                    type="text"
                    :value="hoaDon.soDienThoai || hoaDon.khachHang?.sdt || '0375530923'"
                />
              </div>
            </div>

            <div class="form-group">
              <label>Ghi Chú</label>
              <textarea
                  rows="3"
                  :value="hoaDon.ghiChu || ''"
                  placeholder="Ghi chú đơn hàng..."
              ></textarea>
            </div>
          </div>
        </div>

        <!-- Cột phải: Hóa đơn thanh toán -->
        <div class="content-card summary-section">
          <h3 class="section-title">Hóa đơn</h3>

          <div class="summary-lines">
            <div class="summary-line">
              <span>Tổng tiền:</span>
              <b>{{ formatMoney(hoaDon.tongTien || 1500000) }}</b>
            </div>
            <div class="summary-line">
              <span>Giảm giá:</span>
              <span class="text-danger">- {{ formatMoney(hoaDon.tienGiam || 0) }}</span>
            </div>
            <div class="summary-line">
              <span>Phí vận chuyển:</span>
              <span>+ {{ formatMoney(hoaDon.phiVanChuyen || 0) }}</span>
            </div>
            <div class="summary-line">
              <span>Phụ phí:</span>
              <span>+ {{ formatMoney(hoaDon.phuPhi || 0) }}</span>
            </div>
            <div class="summary-line">
              <span>Hoàn Phí:</span>
              <span>- {{ formatMoney(hoaDon.hoanPhi || 0) }}</span>
            </div>

            <!-- Cần Thanh Toán to cam nổi bật -->
            <div class="total-line">
              <span class="total-label">Cần Thanh Toán:</span>
              <span class="total-value">{{ formatMoney(hoaDon.tongTien || 2400000) }}</span>
            </div>
          </div>

          <button class="btn btn-primary btn-block-update" @click="updateOrder">
            CẬP NHẬT ĐƠN HÀNG
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Toàn bộ vùng hiển thị trang */
.order-detail-wrapper {
  padding: 1.25rem 1.75rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header */
.top-header-bar {
  margin-bottom: 1.2rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-back {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: var(--blue, #496883);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 1.1rem;
  font-weight: 700;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #eaf1f4;
  border-color: var(--blue, #496883);
}

.page-title {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--blue, #496883);
  margin: 0;
}

/* Card dùng chung */
.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  margin-bottom: 1.25rem;
}

.card-head-title h3,
.section-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0 0 1.2rem;
  color: #3b4c54;
}

/* Trạng thái Loading / Lỗi */
.status-msg-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  gap: 0.75rem;
  font-size: 1rem;
  color: #647074;
}

.status-msg-card.error {
  color: #d9534f;
  font-weight: 600;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5dfd2;
  border-top-color: var(--blue, #496883);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 2. Timeline Stepper */
.timeline-container {
  padding: 1.5rem 2rem 2.2rem;
}

.timeline-track {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.timeline-line {
  position: absolute;
  top: 26px;
  left: 30px;
  right: 30px;
  height: 2px;
  background-color: #e5dfd2;
  z-index: 1;
}

.timeline-line-progress {
  position: absolute;
  top: 26px;
  left: 30px;
  height: 2px;
  background-color: #76a486;
  z-index: 1;
  transition: width 0.3s ease;
}

.timeline-step {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 150px;
}

.step-circle {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #dfd8cc;
  display: grid;
  place-items: center;
  font-size: 1.25rem;
  margin-bottom: 0.65rem;
  transition: all 0.3s;
}

.timeline-step.completed .step-circle {
  border-color: #76a486;
  background-color: #edf5ef;
}

.timeline-step.active .step-circle {
  border-color: #76a486;
  background-color: #f1f8f3;
  box-shadow: 0 0 0 4px rgba(118, 164, 134, 0.18);
  transform: scale(1.05);
}

.step-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #647074;
  line-height: 1.35;
  white-space: pre-line;
}

.timeline-step.active .step-label {
  color: #3b4c54;
  font-weight: 700;
}

/* Thanh nút thao tác Timeline */
.timeline-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #efeae0;
  padding-top: 1.2rem;
}

.left-actions {
  display: flex;
  gap: 0.75rem;
}

/* Nút bấm chuẩn */
.btn {
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 7px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary {
  background-color: var(--blue, #496883);
  color: #ffffff;
}

.btn-primary:hover {
  background-color: #38536b;
}

.btn-danger {
  background-color: #d9534f;
  color: #ffffff;
}

.btn-danger:hover {
  background-color: #c9302c;
}

/* 3. Bảng sản phẩm */
.order-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.1rem;
}

.order-code-title {
  font-size: 0.95rem;
  color: #556268;
}

.order-code-title b {
  color: var(--blue, #496883);
  font-family: monospace, sans-serif;
  font-size: 1.05rem;
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.custom-table th {
  background-color: #faf9f6;
  color: #717d84;
  font-weight: 700;
  padding: 0.95rem 1rem;
  border-bottom: 1px solid #efede7;
}

.custom-table td {
  padding: 1.05rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.product-info-cell {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-desc {
  display: flex;
  flex-direction: column;
}

.prod-name {
  font-weight: 700;
  color: #3b4c54;
}

.prod-variant {
  font-size: 0.8rem;
  color: #8c9597;
  margin-top: 2px;
}

.font-bold {
  font-weight: 700;
}

.text-dark {
  color: #2b353a;
}

/* 4. Khối dưới 2 cột */
.bottom-split-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 1.25rem;
  align-items: start;
}

/* Form khách hàng */
.form-vertical {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-group label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #4f5d63;
}

.form-group input,
.form-group textarea {
  width: 100%;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.92rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-group input {
  height: 2.6rem;
}

.form-group textarea {
  padding: 0.75rem 0.95rem;
  resize: vertical;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.08);
}

/* Khối Hóa đơn bên phải */
.summary-lines {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.92rem;
  color: #556268;
}

.text-danger {
  color: #d9534f;
}

.total-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #e4dfd4;
  padding-top: 1rem;
  margin-top: 0.5rem;
}

.total-label {
  font-size: 1.05rem;
  font-weight: 700;
  color: #394850;
}

.total-value {
  font-size: 1.45rem;
  font-weight: 800;
  color: #d97706;
}

.btn-block-update {
  width: 100%;
  height: 2.8rem;
  font-size: 0.95rem;
}

@media (max-width: 992px) {
  .bottom-split-grid {
    grid-template-columns: 1fr;
  }
}
</style>