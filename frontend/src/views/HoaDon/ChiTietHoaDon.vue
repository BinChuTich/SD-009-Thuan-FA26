<template>
  <div class="chi-tiet-hoa-don-wrapper">
    <!-- Tiêu đề -->
    <div class="top-title-card">
      <span class="page-title">Quản Lý Hóa Đơn</span>
    </div>

    <!-- 1. Trạng Thái Đơn Hàng (Stepper) -->
    <div class="custom-card">
      <h3 class="card-title">Trạng Thái Đơn Hàng</h3>

      <div class="stepper-box">
        <div class="step-line"></div>

        <div class="step-node active">
          <div class="step-circle">🛒</div>
          <span class="step-text">Chờ Xác Nhận</span>
        </div>

        <div class="step-node">
          <div class="step-circle">💵</div>
          <span class="step-text">Đã Xác Nhận Thông Tin<br />Thanh Toán</span>
        </div>

        <div class="step-node">
          <div class="step-circle">📦</div>
          <span class="step-text">Chờ Lấy Vận Chuyển</span>
        </div>

        <div class="step-node">
          <div class="step-circle">🚚</div>
          <span class="step-text">Vận Chuyển</span>
        </div>

        <div class="step-node">
          <div class="step-circle">⭐</div>
          <span class="step-text">Đã Hoàn Thành</span>
        </div>
      </div>

      <div class="stepper-footer">
        <div class="btn-left-group">
          <button class="btn btn-blue">Tiếp tục</button>
          <button class="btn btn-red">Hủy đơn và hoàn tiền</button>
        </div>
        <button class="btn btn-blue">
          ℹ️ LỊCH SỬ HÓA ĐƠN
        </button>
      </div>
    </div>

    <!-- 2. Thông tin đơn hàng -->
    <div class="custom-card">
      <div class="order-code-header">
        <span>Thông tin đơn hàng có mã hóa đơn: <b class="code-highlight">HDMKT1</b></span>
        <button class="btn btn-blue">ℹ️ Lịch sử thanh toán</button>
      </div>

      <div class="table-wrap">
        <table class="custom-table">
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
              <div class="product-cell">
                <div class="prod-icon">👕</div>
                <div>
                  <div class="prod-name">{{ item.name }}</div>
                  <small class="prod-desc">{{ item.variant }}</small>
                </div>
              </div>
            </td>
            <td style="text-align: center">{{ item.qty }}</td>
            <td style="text-align: center">{{ item.stock }}</td>
            <td style="text-align: right">{{ item.price }}</td>
            <td style="text-align: right">{{ item.calcPrice }}</td>
            <td style="text-align: right"><b>{{ item.total }}</b></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 3. Hai cột: Thông tin khách hàng & Hóa đơn thanh toán -->
    <div class="bottom-grid">
      <!-- Cột trái -->
      <div class="custom-card">
        <h3 class="card-title">Thông tin khách hàng</h3>
        <div class="form-vertical">
          <div class="form-item">
            <label>Địa Chỉ</label>
            <input type="text" value="123 Đường ABC, Quận XYZ, HN" readonly />
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>Tên Người Nhận</label>
              <input type="text" value="Nguyễn Văn A" readonly />
            </div>
            <div class="form-item">
              <label>Số Điện Thoại</label>
              <input type="text" value="0375530923" readonly />
            </div>
          </div>

          <div class="form-item">
            <label>Ghi Chú</label>
            <textarea placeholder="Ghi chú" rows="4"></textarea>
          </div>
        </div>
      </div>

      <!-- Cột phải -->
      <div class="custom-card">
        <h3 class="card-title">Hóa đơn</h3>

        <div class="bill-summary">
          <div class="bill-row">
            <span>Tổng tiền:</span>
            <b>1.500.000 đ</b>
          </div>
          <div class="bill-row">
            <span>Giảm giá:</span>
            <span>- 0 đ</span>
          </div>
          <div class="bill-row">
            <span>Phí vận chuyển:</span>
            <span>+ 0 đ</span>
          </div>
          <div class="bill-row">
            <span>Phụ phí:</span>
            <span>+ 0 đ</span>
          </div>
          <div class="bill-row">
            <span>Hoàn Phí:</span>
            <span>- 0 đ</span>
          </div>
        </div>

        <div class="bill-divider"></div>

        <div class="total-row">
          <span>Cần Thanh Toán:</span>
          <strong class="total-price">2.400.000 đ</strong>
        </div>

        <button class="btn btn-update">CẬP NHẬT ĐƠN HÀNG</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const products = ref([
  {
    id: 1,
    name: 'Áo phông nam FF',
    variant: 'Size L / Xanh Navy',
    qty: 2,
    stock: 50,
    price: '750.000 đ',
    calcPrice: '750.000 đ',
    total: '1.500.000 đ'
  }
])
</script>

<style scoped>
.chi-tiet-hoa-don-wrapper {
  padding: 16px 24px 30px;
  background-color: #f7f5ef;
  min-height: calc(100vh - 48px);
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #3d4a50;
}

/* Header */
.top-title-card {
  background: #ffffff;
  border: 1px solid #e9e5db;
  border-radius: 8px;
  padding: 11px 18px;
  margin-bottom: 12px;
}

.page-title {
  color: #496883;
  font-weight: 700;
  font-size: 13px;
}

/* Card dùng chung */
.custom-card {
  background: #ffffff;
  border: 1px solid #e9e5db;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 14px;
}

.card-title {
  font-size: 12px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #3e4d54;
}

/* Stepper */
.stepper-box {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin: 16px 30px 22px;
}

.step-line {
  position: absolute;
  top: 22px;
  left: 20px;
  right: 20px;
  height: 2px;
  background-color: #e5ded2;
  z-index: 1;
}

.step-node {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 130px;
}

.step-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #ded6c7;
  display: grid;
  place-items: center;
  font-size: 16px;
  margin-bottom: 8px;
}

.step-node.active .step-circle {
  border-color: #558764;
  background: #edf5ef;
}

.step-text {
  font-size: 9px;
  color: #647074;
  line-height: 1.3;
}

.step-node.active .step-text {
  font-weight: 700;
  color: #3b5c45;
}

.stepper-footer {
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

/* Buttons */
.btn {
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 9px;
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

/* Bảng */
.order-code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 10px;
}

.code-highlight {
  color: #496883;
}

.table-wrap {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 9px;
}

.custom-table th {
  background-color: #faf9f6;
  color: #8f9695;
  font-weight: 700;
  padding: 9px 12px;
  border-bottom: 1px solid #efede7;
  text-align: left;
}

.custom-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f0eb;
  color: #556268;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prod-icon {
  width: 28px;
  height: 28px;
  background: #eaf1f4;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 14px;
}

.prod-name {
  font-weight: 600;
}

.prod-desc {
  color: #9aa0a0;
  font-size: 7.5px;
}

/* Hai cột dưới */
.bottom-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 14px;
  align-items: start;
}

.form-vertical {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-item label {
  font-size: 8.5px;
  font-weight: 700;
  color: #556268;
}

.form-item input,
.form-item textarea {
  border: 1px solid #e9e5db;
  background-color: #fcfbf8;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 9px;
  color: #3d4a50;
  outline: none;
  font-family: inherit;
}

.form-item textarea {
  resize: none;
}

/* Cột hóa đơn */
.bill-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.bill-row {
  display: flex;
  justify-content: space-between;
  font-size: 9px;
  color: #647074;
}

.bill-divider {
  border-top: 1px dashed #e9e5db;
  margin-bottom: 12px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.total-row span {
  font-size: 10px;
  font-weight: 700;
}

.total-price {
  font-size: 14px;
  font-weight: 800;
  color: #ea5434;
}

.btn-update {
  width: 100%;
  height: 36px;
  background-color: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 9px;
  font-weight: 700;
  cursor: pointer;
}

.btn-update:hover {
  background-color: #38536b;
}

@media (max-width: 950px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>