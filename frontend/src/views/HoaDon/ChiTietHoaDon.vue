<template>
  <div class="chi-tiet-container">
    <!-- 1. Thanh tiêu đề trên cùng có nút quay lại -->
    <div class="top-title-card">
      <div class="header-left">
        <button class="btn-back" @click="quayLaiDanhSach">
          ← Quay lại danh sách
        </button>
        <span class="page-title">
          Chi Tiết Hóa Đơn: <b class="highlight-code">{{ maHoaDonHienTai }}</b>
        </span>
      </div>
    </div>

    <!-- 2. Khối Trạng Thái Đơn Hàng (Stepper tiến trình) -->
    <div class="custom-card">
      <h3 class="card-section-title">Trạng Thái Đơn Hàng</h3>

      <div class="stepper-wrapper">
        <div class="stepper-line"></div>

        <div class="step-item active">
          <div class="step-circle">🛒</div>
          <span class="step-label">Chờ Xác Nhận</span>
        </div>

        <div class="step-item">
          <div class="step-circle">💵</div>
          <span class="step-label">Đã Xác Nhận Thông Tin<br />Thanh Toán</span>
        </div>

        <div class="step-item">
          <div class="step-circle">📦</div>
          <span class="step-label">Chờ Lấy Vận Chuyển</span>
        </div>

        <div class="step-item">
          <div class="step-circle">🚚</div>
          <span class="step-label">Vận Chuyển</span>
        </div>

        <div class="step-item">
          <div class="step-circle">⭐</div>
          <span class="step-label">Đã Hoàn Thành</span>
        </div>
      </div>

      <div class="stepper-actions">
        <div class="btn-left-group">
          <button class="btn btn-blue">Tiếp tục</button>
          <button class="btn btn-red">Hủy đơn và hoàn tiền</button>
        </div>
        <button class="btn btn-info">
          ℹ️ LỊCH SỬ HÓA ĐƠN
        </button>
      </div>
    </div>

    <!-- 3. Khối Thông tin đơn hàng & Bảng sản phẩm -->
    <div class="custom-card">
      <div class="card-head-between">
        <div class="head-code">
          Thông tin đơn hàng có mã hóa đơn: <b class="highlight-code">{{ maHoaDonHienTai }}</b>
        </div>
        <button class="btn btn-info">
          ℹ️ Lịch sử thanh toán
        </button>
      </div>

      <div class="table-responsive">
        <table class="detail-order-table">
          <thead>
          <tr>
            <th>Sản phẩm</th>
            <th style="text-align: center">Số Lượng</th>
            <th style="text-align: center">Kho</th>
            <th style="text-align: right">Giá hiện tại</th>
            <th style="text-align: right">Giá được tính</th>
            <th style="text-align: right">Tổng</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in products" :key="item.id">
            <td>
              <div class="product-info-cell">
                <span class="prod-badge">👕</span>
                <div>
                  <b>{{ item.name }}</b>
                  <small class="text-muted">{{ item.variant }}</small>
                </div>
              </div>
            </td>
            <td style="text-align: center">{{ item.quantity }}</td>
            <td style="text-align: center">{{ item.stock }}</td>
            <td style="text-align: right">{{ item.currentPrice }}</td>
            <td style="text-align: right">{{ item.calculatedPrice }}</td>
            <td style="text-align: right"><b>{{ item.total }}</b></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 4. Lưới 2 cột: Thông tin khách hàng & Chi tiết tính tiền -->
    <div class="bottom-split-grid">
      <!-- Cột trái: Form khách hàng -->
      <div class="custom-card">
        <h3 class="card-section-title">Thông tin khách hàng</h3>
        <div class="customer-form">
          <div class="form-group">
            <label>Địa Chỉ</label>
            <input type="text" value="123 Đường ABC, Quận XYZ, HN" readonly />
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Tên Người Nhận</label>
              <input type="text" value="Nguyễn Văn A" readonly />
            </div>
            <div class="form-group">
              <label>Số Điện Thoại</label>
              <input type="text" value="0375530923" readonly />
            </div>
          </div>

          <div class="form-group">
            <label>Ghi Chú</label>
            <textarea placeholder="Ghi chú đơn hàng..." rows="3"></textarea>
          </div>
        </div>
      </div>

      <!-- Cột phải: Tính tiền -->
      <div class="custom-card">
        <h3 class="card-section-title">Hóa đơn</h3>

        <div class="calc-list">
          <div class="calc-item">
            <span>Tổng tiền:</span>
            <b>1.500.000 đ</b>
          </div>
          <div class="calc-item">
            <span>Giảm giá:</span>
            <span>- 0 đ</span>
          </div>
          <div class="calc-item">
            <span>Phí vận chuyển:</span>
            <span>+ 0 đ</span>
          </div>
          <div class="calc-item">
            <span>Phụ phí:</span>
            <span>+ 0 đ</span>
          </div>
          <div class="calc-item">
            <span>Hoàn Phí:</span>
            <span>- 0 đ</span>
          </div>
        </div>

        <div class="calc-divider"></div>

        <div class="need-to-pay-row">
          <span>Cần Thanh Toán:</span>
          <strong class="pay-amount">2.400.000 đ</strong>
        </div>

        <button class="btn btn-update-order">CẬP NHẬT ĐƠN HÀNG</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const maHoaDonHienTai = computed(() => {
  return route.params.ma || route.query.ma || 'HDMKT1'
})

const quayLaiDanhSach = () => {
  router.push('/hoa-don')
}

const products = ref([
  {
    id: 1,
    name: 'Áo thun FF Basic Signature',
    variant: 'Size L - Xanh Navy',
    quantity: 2,
    stock: 45,
    currentPrice: '750.000 đ',
    calculatedPrice: '750.000 đ',
    total: '1.500.000 đ'
  }
])
</script>

<style scoped>
/* Khung bao ngoài, chừa lề cách đều 2 bên */
.chi-tiet-container {
  padding: 18px 24px 36px;
  background-color: #f7f5ef;
  min-height: calc(100vh - 48px);
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #3d4a50;
}

/* 1. Header */
.top-title-card {
  background: #ffffff;
  border: 1px solid #e9e5db;
  border-radius: 8px;
  padding: 12px 18px;
  margin-bottom: 14px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.btn-back {
  background: #eaf1f4;
  border: 1px solid #c9d8e2;
  color: #496883;
  font-weight: 700;
  font-size: 8.5px;
  padding: 5px 12px;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #496883;
  color: #ffffff;
}

.page-title {
  font-size: 13px;
  font-weight: 700;
  color: #496883;
}

.highlight-code {
  color: #496883;
}

/* 2. Card dùng chung */
.custom-card {
  background: #ffffff;
  border: 1px solid #e9e5db;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 14px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-section-title {
  font-size: 11.5px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #43545c;
}

/* Stepper / Tiến trình */
.stepper-wrapper {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin: 16px 40px 24px;
}

.stepper-line {
  position: absolute;
  top: 22px;
  left: 30px;
  right: 30px;
  height: 2px;
  background-color: #e5ded2;
  z-index: 1;
}

.step-item {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 120px;
}

.step-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #ded6c7;
  display: grid;
  place-items: center;
  font-size: 17px;
  margin-bottom: 8px;
}

.step-item.active .step-circle {
  border-color: #558764;
  background: #edf5ef;
}

.step-label {
  font-size: 8.5px;
  color: #647074;
  line-height: 1.3;
}

.step-item.active .step-label {
  font-weight: 700;
  color: #3b5c45;
}

.stepper-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #e9e5db;
  padding-top: 14px;
}

.btn-left-group {
  display: flex;
  gap: 8px;
}

/* Nút bấm */
.btn {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-blue {
  background-color: #496883;
  color: #ffffff;
}
.btn-blue:hover {
  background-color: #395267;
}

.btn-red {
  background-color: #e04f4f;
  color: #ffffff;
}
.btn-red:hover {
  background-color: #c93b3b;
}

.btn-info {
  background-color: #3b82f6;
  color: #ffffff;
}
.btn-info:hover {
  background-color: #2563eb;
}

/* 3. Bảng sản phẩm */
.card-head-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.head-code {
  font-size: 9.5px;
  color: #556268;
}

.table-responsive {
  overflow-x: auto;
}

.detail-order-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 8.5px;
}

.detail-order-table th {
  background-color: #faf9f6;
  color: #8f9695;
  font-weight: 700;
  padding: 9px 12px;
  text-align: left;
  border-bottom: 1px solid #efede7;
}

.detail-order-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f0eb;
  color: #556268;
}

.product-info-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prod-badge {
  width: 28px;
  height: 28px;
  background-color: #eaf1f4;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 14px;
}

.text-muted {
  display: block;
  font-size: 7.5px;
  color: #9aa0a0;
  margin-top: 1px;
}

/* 4. Lưới 2 cột */
.bottom-split-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 14px;
  align-items: start;
}

.customer-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-size: 8.5px;
  font-weight: 700;
  color: #556268;
}

.form-group input,
.form-group textarea {
  border: 1px solid #e9e5db;
  background-color: #fcfbf8;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 9px;
  color: #3d4a50;
  outline: none;
  font-family: inherit;
}

.form-group textarea {
  resize: vertical;
}

/* Tính tiền */
.calc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.calc-item {
  display: flex;
  justify-content: space-between;
  font-size: 8.5px;
  color: #647074;
}

.calc-divider {
  border-top: 1px dashed #e9e5db;
  margin-bottom: 12px;
}

.need-to-pay-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.need-to-pay-row span {
  font-size: 9.5px;
  font-weight: 700;
}

.pay-amount {
  font-size: 13.5px;
  font-weight: 800;
  color: #ea5434;
}

.btn-update-order {
  width: 100%;
  height: 34px;
  background-color: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-update-order:hover {
  background-color: #2563eb;
}

@media (max-width: 950px) {
  .bottom-split-grid {
    grid-template-columns: 1fr;
  }
}
</style>
