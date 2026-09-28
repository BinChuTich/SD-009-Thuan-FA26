<template>
  <section class="dashboard">
    <div class="dashboard-head">
      <div>
        <div class="tiny-label">TRANG CHỦ / TỔNG QUAN</div>
        <h1>Tổng quan hoạt động</h1>
        <p>Theo dõi doanh thu, đơn hàng, tồn kho và khách hàng trong ngày.</p>
      </div>
      <button class="today-btn">◷ &nbsp; Lọc hôm nay</button>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">₫</div>
        <div class="stat-info">
          <span>Doanh thu hôm nay</span>
          <strong>12.580.000 ₫</strong>
          <small class="green">↑ 12,5% so với hôm qua</small>
        </div>
        <div class="corner"></div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">▣</div>
        <div class="stat-info">
          <span>Đơn hàng hôm nay</span>
          <strong>48</strong>
          <small class="green">↑ 8 đơn so với hôm qua</small>
        </div>
        <div class="corner"></div>
      </div>

      <div class="stat-card">
        <div class="stat-icon gold">◇</div>
        <div class="stat-info">
          <span>Sản phẩm tồn kho</span>
          <strong>186</strong>
          <small class="orange">12 sản phẩm sắp hết</small>
        </div>
        <div class="corner"></div>
      </div>

      <div class="stat-card">
        <div class="stat-icon beige">♙</div>
        <div class="stat-info">
          <span>Khách hàng</span>
          <strong>1.284</strong>
          <small class="green">↑ 24 khách mới trong tháng</small>
        </div>
        <div class="corner"></div>
      </div>
    </div>

    <div class="mini-grid">
      <div class="mini-card"><b>↗</b><span>Doanh thu tăng</span><strong>12,5%</strong></div>
      <div class="mini-card"><b class="ok">✓</b><span>Đơn hoàn tất</span><strong>36</strong></div>
      <div class="mini-card"><b class="warn">!</b><span>Sản phẩm sắp hết</span><strong>12</strong></div>
      <div class="mini-card"><b class="purple">♙</b><span>Khách hàng mới</span><strong>24</strong></div>
    </div>

    <div class="main-grid">
      <div class="card revenue-card">
        <div class="card-head">
          <div>
            <div class="section-label">HIỆU QUẢ KINH DOANH</div>
            <h2>Doanh thu 7 ngày</h2>
            <p>Doanh thu theo ngày trong tuần</p>
          </div>
          <div class="revenue-total">
            <div><strong>60,45M ₫</strong><small>Tổng 7 ngày</small></div>
            <div><strong>9,78M ₫</strong><small>Trung bình/ngày</small></div>
          </div>
        </div>

        <div class="chart">
          <div class="y-labels"><span>12M</span><span>9M</span><span>6M</span><span>3M</span><span>0</span></div>
          <div class="chart-area">
            <div class="grid-line" style="top:0"></div>
            <div class="grid-line" style="top:25%"></div>
            <div class="grid-line" style="top:50%"></div>
            <div class="grid-line" style="top:75%"></div>
            <div class="grid-line" style="top:100%"></div>
            <div class="bars">
              <div class="bar-col"><i style="height:48%"></i><small>8,2M</small><span>T2</span></div>
              <div class="bar-col"><i style="height:72%"></i><small>10,8M</small><span>T3</span></div>
              <div class="bar-col"><i style="height:62%"></i><small>9,4M</small><span>T4</span></div>
              <div class="bar-col"><i style="height:78%"></i><small>11,6M</small><span>T5</span></div>
              <div class="bar-col"><i style="height:88%"></i><small>13,0M</small><span>T6</span></div>
              <div class="bar-col"><i class="highlight" style="height:70%"></i><small>10,4M</small><span>T7</span></div>
              <div class="bar-col"><i style="height:54%"></i><small>8,1M</small><span>CN</span></div>
            </div>
          </div>
        </div>
        <div class="chart-note">Ngày cao nhất: <b>Thứ 6 - 13,0M ₫</b> &nbsp; • &nbsp; Ngày thấp nhất: <b>Chủ nhật - 8,1M ₫</b></div>
      </div>

      <div class="card status-card">
        <div class="card-head">
          <div><div class="section-label">ĐƠN HÀNG</div><h2>Trạng thái hóa đơn</h2></div>
          <a href="/hoa-don">Xem chi tiết →</a>
        </div>
        <div class="status-numbers">
          <div><b>8</b><span>Chờ xử lý</span></div>
          <div><b>12</b><span>Đang giao</span></div>
          <div><b>28</b><span>Hoàn tất</span></div>
        </div>
        <div class="progress-list">
          <div><span>Hoàn tất</span><b>58%</b><i><em style="width:58%"></em></i></div>
          <div><span>Đang giao</span><b>25%</b><i><em style="width:25%"></em></i></div>
          <div><span>Chờ xử lý</span><b>17%</b><i><em style="width:17%"></em></i></div>
        </div>
      </div>
    </div>

    <div class="lower-grid">
      <div class="card table-card">
        <div class="card-head">
          <div><div class="section-label">HOẠT ĐỘNG GẦN ĐÂY</div><h2>10 đơn hàng gần nhất</h2><p>Theo dõi các đơn hàng mới nhất</p></div>
          <a href="/hoa-don">Xem tất cả →</a>
        </div>
        <table>
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Tổng tiền</th><th>Trạng thái</th></tr></thead>
          <tbody>
          <tr v-for="order in orders" :key="order.code">
            <td><b class="blue-text">{{ order.code }}</b><small>{{ order.time }}</small></td>
            <td>{{ order.customer }}</td>
            <td><b>{{ order.total }}</b></td>
            <td><span class="pill" :class="order.type">{{ order.status }}</span></td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="card table-card">
        <div class="card-head">
          <div><div class="section-label">SẢN PHẨM BÁN CHẠY</div><h2>Top 10 sản phẩm bán chạy</h2><p>Theo số lượng bán</p></div>
          <a href="#">Xem tất cả →</a>
        </div>
        <table>
          <thead><tr><th>#</th><th>Sản phẩm</th><th>Đã bán</th><th>Doanh thu</th></tr></thead>
          <tbody>
          <tr v-for="(p, index) in products" :key="p.name">
            <td>{{ index + 1 }}</td>
            <td><b>{{ p.name }}</b></td>
            <td>{{ p.sold }}</td>
            <td><b>{{ p.revenue }}</b></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card alert-card">
      <div class="card-head">
        <div><div class="section-label">VẬN HÀNH</div><h2>Cảnh báo vận hành</h2><p>Các vấn đề cần được theo dõi</p></div>
      </div>
      <div class="alert-grid">
        <div><span class="alert-icon blue-bg">!</span><strong>12 sản phẩm</strong><small>Sắp hết hàng</small><b>→</b></div>
        <div><span class="alert-icon purple-bg">↻</span><strong>3 voucher</strong><small>Sắp hết hạn</small><b>→</b></div>
        <div><span class="alert-icon green-bg">▣</span><strong>4 đơn</strong><small>Chờ xác nhận</small><b>→</b></div>
        <div><span class="alert-icon gold-bg">♧</span><strong>2 phiếu trả</strong><small>Chờ xử lý</small><b>→</b></div>
      </div>
    </div>
  </section>
</template>

<script setup>
const orders = [
  { code:'HD0008', time:'10 phút trước', customer:'Nguyễn Minh Anh', total:'1.875.000 ₫', status:'Đã thanh toán', type:'done' },
  { code:'HD0007', time:'18 phút trước', customer:'Trần Hữu Đức', total:'825.000 ₫', status:'Đang giao', type:'ship' },
  { code:'HD0006', time:'35 phút trước', customer:'Lê Thị Bích', total:'1.240.000 ₫', status:'Chờ xác nhận', type:'wait' },
  { code:'HD0005', time:'1 giờ trước', customer:'Phạm Gia Hân', total:'580.000 ₫', status:'Đã thanh toán', type:'done' },
  { code:'HD0004', time:'1 giờ trước', customer:'Đỗ Trí Khang', total:'2.480.000 ₫', status:'Đã thanh toán', type:'done' },
  { code:'HD0003', time:'2 giờ trước', customer:'Vũ Ngọc Linh', total:'275.000 ₫', status:'Đã thanh toán', type:'done' },
  { code:'HD0002', time:'2 giờ trước', customer:'Nguyễn Quỳnh Anh', total:'990.000 ₫', status:'Đang giao', type:'ship' },
  { code:'HD0001', time:'3 giờ trước', customer:'Lê Thị Bích', total:'480.000 ₫', status:'Hoàn tất', type:'done' }
]

const products = [
  { name:'Áo thun FF Basic', sold:342, revenue:'12.580.000 ₫' },
  { name:'Áo thun Oversize', sold:280, revenue:'9.820.000 ₫' },
  { name:'Áo Polo Premium', sold:251, revenue:'8.745.000 ₫' },
  { name:'Áo Regular Summer', sold:198, revenue:'7.425.000 ₫' },
  { name:'Áo Daily Shirt', sold:174, revenue:'6.940.000 ₫' },
  { name:'Áo thun Unisex', sold:152, revenue:'5.780.000 ₫' },
  { name:'Áo Raglan Basic', sold:133, revenue:'5.120.000 ₫' },
  { name:'Áo Minimal Cotton', sold:121, revenue:'4.830.000 ₫' },
  { name:'Áo Easy Fit', sold:107, revenue:'4.510.000 ₫' },
  { name:'Áo Local Print', sold:97, revenue:'3.960.000 ₫' }
]
</script>