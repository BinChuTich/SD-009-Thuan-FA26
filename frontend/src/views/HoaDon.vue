<template>
  <div class="invoice-page">
    <!-- Tiêu đề trang & các nút thao tác -->
    <div class="page-head">
      <div>
        <div class="tiny-label">BÁN HÀNG / HÓA ĐƠN</div>
        <h1>Danh sách hóa đơn</h1>
        <p>Tra cứu, lọc trạng thái và in hóa đơn thanh toán của cửa hàng.</p>
      </div>
      <div class="head-actions">
        <button class="btn btn-secondary">↓ Xuất Excel</button>
        <button class="btn btn-primary">+ Tạo hóa đơn</button>
      </div>
    </div>

    <!-- Thanh thống kê trạng thái hóa đơn dạng Tab/Cards -->
    <div class="invoice-summary-grid">
      <div class="summary-card active">
        <span>Tất cả</span>
        <strong>142</strong>
        <small>Toàn bộ hóa đơn</small>
      </div>
      <div class="summary-card">
        <span>Chờ thanh toán</span>
        <strong>14</strong>
        <small class="pill-wait">Cần xử lý</small>
      </div>
      <div class="summary-card">
        <span>Đã thanh toán</span>
        <strong>118</strong>
        <small class="pill-done">Thành công</small>
      </div>
      <div class="summary-card">
        <span>Đã hủy / Hoàn tiền</span>
        <strong>10</strong>
        <small class="pill-cancel">Đã hoàn tất</small>
      </div>
    </div>

    <!-- Khối tìm kiếm & bộ lọc -->
    <div class="card filter-box">
      <div class="search-input-wrap">
        <span class="search-icon">🔍</span>
        <input type="text" placeholder="Tìm theo mã HĐ, tên khách, số điện thoại..." />
      </div>

      <div class="filter-group">
        <select>
          <option>Tất cả trạng thái</option>
          <option>Đã thanh toán</option>
          <option>Chờ xác nhận</option>
          <option>Đang giao</option>
          <option>Đã hủy</option>
        </select>

        <select>
          <option>Hình thức: Tất cả</option>
          <option>Tiền mặt</option>
          <option>Chuyển khoản</option>
          <option>Quẹt thẻ</option>
        </select>

        <button class="btn btn-filter">Lọc</button>
      </div>
    </div>

    <!-- Bảng danh sách hóa đơn -->
    <div class="card table-card invoice-table-card">
      <table>
        <thead>
        <tr>
          <th>MÃ HÓA ĐƠN</th>
          <th>NGÀY TẠO</th>
          <th>KHÁCH HÀNG</th>
          <th>THU NGÂN</th>
          <th>PHƯƠNG THỨC</th>
          <th>TỔNG TIỀN</th>
          <th>TRẠNG THÁI</th>
          <th style="text-align: right">THAO TÁC</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in invoices" :key="item.id">
          <td>
            <b class="blue-text">{{ item.id }}</b>
          </td>
          <td>
            <span>{{ item.date }}</span>
            <small class="muted-text">{{ item.time }}</small>
          </td>
          <td>
            <b>{{ item.customer }}</b>
            <small class="muted-text">{{ item.phone }}</small>
          </td>
          <td>{{ item.cashier }}</td>
          <td>
            <span class="pay-tag">{{ item.method }}</span>
          </td>
          <td>
            <b class="price-val">{{ item.total }}</b>
          </td>
          <td>
            <span class="pill" :class="item.statusClass">{{ item.statusText }}</span>
          </td>
          <td style="text-align: right">
            <button class="action-btn" title="In hóa đơn">🖶</button>
            <button class="action-btn" title="Xem chi tiết">👁</button>
          </td>
        </tr>
        </tbody>
      </table>

      <!-- Phân trang -->
      <div class="table-pagination">
        <span>Hiển thị 1 - 10 trên 142 hóa đơn</span>
        <div class="page-btns">
          <button class="pg-btn" disabled>‹</button>
          <button class="pg-btn active">1</button>
          <button class="pg-btn">2</button>
          <button class="pg-btn">3</button>
          <button class="pg-btn">›</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const invoices = [
  { id: 'HD0010', date: '28/09/2026', time: '14:20', customer: 'Nguyễn Minh Anh', phone: '0981 234 567', cashier: 'Thu Ngân 01', method: 'Chuyển khoản', total: '1.875.000 ₫', statusClass: 'done', statusText: 'Đã thanh toán' },
  { id: 'HD0009', date: '28/09/2026', time: '13:45', customer: 'Trần Hữu Đức', phone: '0912 345 678', cashier: 'Thu Ngân 02', method: 'Tiền mặt', total: '825.000 ₫', statusClass: 'ship', statusText: 'Đang giao' },
  { id: 'HD0008', date: '28/09/2026', time: '12:10', customer: 'Lê Thị Bích', phone: '0903 888 999', cashier: 'Thu Ngân 01', method: 'Chuyển khoản', total: '1.240.000 ₫', statusClass: 'wait', statusText: 'Chờ xác nhận' },
  { id: 'HD0007', date: '28/09/2026', time: '11:05', customer: 'Phạm Gia Hân', phone: '0977 112 233', cashier: 'Thu Ngân 01', method: 'Quẹt thẻ', total: '580.000 ₫', statusClass: 'done', statusText: 'Đã thanh toán' },
  { id: 'HD0006', date: '28/09/2026', time: '10:30', customer: 'Đỗ Trí Khang', phone: '0966 445 566', cashier: 'Thu Ngân 02', method: 'Tiền mặt', total: '2.480.000 ₫', statusClass: 'done', statusText: 'Đã thanh toán' },
  { id: 'HD0005', date: '27/09/2026', time: '19:15', customer: 'Khách lẻ tại quầy', phone: '-', cashier: 'Thu Ngân 01', method: 'Tiền mặt', total: '275.000 ₫', statusClass: 'done', statusText: 'Đã thanh toán' },
  { id: 'HD0004', date: '27/09/2026', time: '18:50', customer: 'Nguyễn Quỳnh Anh', phone: '0934 556 778', cashier: 'Thu Ngân 02', method: 'Chuyển khoản', total: '990.000 ₫', statusClass: 'cancel', statusText: 'Đã hủy' }
]
</script>