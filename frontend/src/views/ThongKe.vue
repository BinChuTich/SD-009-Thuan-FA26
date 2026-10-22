<template>
  <div class="statistics-page-wrapper">
    <!-- 1. Tiêu đề chính -->
    <div class="page-top-bar">
      <div class="title-with-icon">
        <div class="title-icon-box">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="20" x2="18" y2="10" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="6" y1="20" x2="6" y2="14" />
            <line x1="2" y1="20" x2="22" y2="20" />
          </svg>
        </div>
        <h1 class="page-main-title">Thống kê doanh thu</h1>
      </div>
    </div>

    <!-- 1. Card Bộ lọc thống kê -->
    <div class="content-card filter-card-box">
      <!-- Hàng trên: Tiêu đề bộ lọc & Cụm nút lọc nhanh + Xuất Excel -->
      <div class="filter-top-row">
        <div class="filter-heading">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="filter-icon">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
          </svg>
          <span class="filter-title-text">Bộ lọc thống kê</span>
        </div>

        <div class="filter-pills-cluster">
          <button
            type="button"
            class="pill-btn"
            :class="{ active: quickFilter === 'all' }"
            @click="setQuickFilter('all')"
          >
            Tất cả
          </button>
          <button
            type="button"
            class="pill-btn"
            :class="{ active: quickFilter === 'today' }"
            @click="setQuickFilter('today')"
          >
            Hôm nay
          </button>
          <button
            type="button"
            class="pill-btn"
            :class="{ active: quickFilter === '7days' }"
            @click="setQuickFilter('7days')"
          >
            7 ngày qua
          </button>
          <button
            type="button"
            class="pill-btn"
            :class="{ active: quickFilter === 'thisMonth' }"
            @click="setQuickFilter('thisMonth')"
          >
            Tháng này
          </button>
          <button
            type="button"
            class="pill-btn"
            :class="{ active: quickFilter === 'lastMonth' }"
            @click="setQuickFilter('lastMonth')"
          >
            Tháng trước
          </button>
          <button
            type="button"
            class="pill-btn"
            :class="{ active: quickFilter === 'thisYear' }"
            @click="setQuickFilter('thisYear')"
          >
            Năm nay
          </button>

          <!-- Nút Xuất Excel ngoài cùng -->
          <button
            type="button"
            class="pill-btn btn-export-excel"
            @click="handleExportExcel"
            title="Xuất dữ liệu thống kê ra file Excel"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            Xuất Excel
          </button>
        </div>
      </div>

      <!-- Hàng giữa: 5 trường lọc + Nút Đặt lại -->
      <div class="filter-fields-row">
        <!-- Từ ngày -->
        <div class="filter-field-item">
          <label class="field-label-tiny">Từ ngày</label>
          <div class="input-with-icon">
            <input
              type="date"
              v-model="filters.fromDate"
              class="form-control-minimal"
              :class="{ 'input-error-border': dateFilterError }"
              @change="onCustomDateChange"
            />
          </div>
        </div>

        <!-- Đến ngày -->
        <div class="filter-field-item">
          <label class="field-label-tiny">Đến ngày</label>
          <div class="input-with-icon">
            <input
              type="date"
              v-model="filters.toDate"
              class="form-control-minimal"
              :class="{ 'input-error-border': dateFilterError }"
              @change="onCustomDateChange"
            />
          </div>
        </div>

        <!-- Kiểu thống kê -->
        <div class="filter-field-item">
          <label class="field-label-tiny">Thống kê</label>
          <div class="select-wrapper-minimal">
            <select v-model="filters.statType" class="form-control-minimal" @change="applyFilters">
              <option value="day">Theo ngày</option>
              <option value="week">Theo tuần</option>
              <option value="month">Theo tháng</option>
              <option value="year">Theo năm</option>
            </select>
          </div>
        </div>

        <!-- Thương hiệu -->
        <div class="filter-field-item">
          <label class="field-label-tiny">Thương hiệu</label>
          <div class="input-with-prefix-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="prefix-icon">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
            <select v-model="filters.brand" class="form-control-minimal with-icon" @change="applyFilters">
              <option value="">Tất cả thương hiệu</option>
              <option v-for="b in brandOptions" :key="b.id" :value="b.id">
                {{ b.ten || b.tenThuongHieu }}
              </option>
            </select>
          </div>
        </div>

        <!-- Tìm sản phẩm -->
        <div class="filter-field-item search-item">
          <label class="field-label-tiny">Tìm sản phẩm</label>
          <div class="input-with-prefix-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="prefix-icon">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              v-model="filters.productKeyword"
              placeholder="Mã hoặc tên sản phẩm..."
              class="form-control-minimal with-icon"
              @input="debounceFilter"
            />
          </div>
        </div>

        <!-- Nút Đặt lại -->
        <div class="filter-reset-action">
          <button type="button" class="btn-reset-round" @click="resetFilters" title="Đặt lại bộ lọc về mặc định">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Đặt lại</span>
          </button>
        </div>
      </div>

      <!-- Báo lỗi khoảng ngày nếu Từ ngày > Đến ngày -->
      <div v-if="dateFilterError" class="filter-error-msg">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>{{ dateFilterError }}</span>
      </div>

      <!-- Hàng dưới cùng: Chấm xanh dương & Dòng trạng thái ngày -->
      <div class="filter-status-row">
        <span class="blue-status-dot"></span>
        <span class="filter-status-text">
          Đang hiển thị dữ liệu từ ngày <strong>{{ formatDateDisplay(filters.fromDate) }}</strong> đến ngày <strong>{{ formatDateDisplay(filters.toDate) }}</strong>
        </span>
      </div>
    </div>

    <!-- 2. Hàng 6 Thẻ chỉ số tổng quan (6 KPI Cards) -->
    <div class="kpi-cards-grid">
      <!-- 1. Tổng doanh thu -->
      <div class="kpi-card">
        <div class="kpi-icon-wrap pastel-green">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
            <polyline points="17 6 23 6 23 12" />
          </svg>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.totalRevenue) }}</div>
        <div class="kpi-label">Tổng doanh thu</div>
      </div>

      <!-- 2. Tiền mặt -->
      <div class="kpi-card">
        <div class="kpi-icon-wrap pastel-blue">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="6" width="20" height="12" rx="2" />
            <circle cx="12" cy="12" r="2" />
            <path d="M6 12h.01M18 12h.01" />
          </svg>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.cashRevenue) }}</div>
        <div class="kpi-label">Tiền mặt</div>
      </div>

      <!-- 3. Chuyển khoản -->
      <div class="kpi-card">
        <div class="kpi-icon-wrap pastel-emerald">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2" />
            <line x1="2" y1="10" x2="22" y2="10" />
          </svg>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.transferRevenue) }}</div>
        <div class="kpi-label">Chuyển khoản</div>
      </div>

      <!-- 4. Tổng đơn hàng -->
      <div class="kpi-card">
        <div class="kpi-icon-wrap pastel-purple">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
        </div>
        <div class="kpi-value text-dark">{{ kpiData.totalOrders.toLocaleString('vi-VN') }}</div>
        <div class="kpi-label">Tổng đơn hàng</div>
      </div>

      <!-- 5. Giá trị TB đơn (AOV) -->
      <div class="kpi-card">
        <div class="kpi-icon-wrap pastel-pink">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="4" y="2" width="16" height="20" rx="2" />
            <line x1="8" y1="6" x2="16" y2="6" />
            <line x1="16" y1="14" x2="16" y2="18" />
            <path d="M16 10h.01" />
            <path d="M12 10h.01" />
            <path d="M8 10h.01" />
            <path d="M12 14h.01" />
            <path d="M8 14h.01" />
            <path d="M12 18h.01" />
            <path d="M8 18h.01" />
          </svg>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.aov) }}</div>
        <div class="kpi-label">Giá trị TB đơn (AOV)</div>
      </div>

      <!-- 6. Sản phẩm đã bán -->
      <div class="kpi-card">
        <div class="kpi-icon-wrap pastel-orange">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
            <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
            <line x1="12" y1="22.08" x2="12" y2="12" />
          </svg>
        </div>
        <div class="kpi-value text-dark">{{ kpiData.totalItemsSold.toLocaleString('vi-VN') }}</div>
        <div class="kpi-label">Sản phẩm đã bán</div>
      </div>
    </div>

    <!-- 3. Khu vực Biểu đồ hàng 1 (Tỷ lệ 2:1) -->
    <div class="charts-row-split-2-1">
      <!-- Bên trái (2/3): Sản phẩm bán được theo Ngày (Bar Chart) -->
      <div class="content-card chart-card-col-2">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
              <line x1="18" y1="20" x2="18" y2="10" />
              <line x1="12" y1="20" x2="12" y2="4" />
              <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
            <h3 class="card-heading">Sản phẩm bán được theo {{ statTypeHeading }}</h3>
          </div>
          <span class="badge-unit">Đơn vị: Sản phẩm</span>
        </div>

        <!-- Biểu đồ cột Bar Chart với số đỏ trên đỉnh -->
        <div class="bar-chart-container">
          <div class="bar-chart-svg-wrap">
            <svg class="bar-chart-svg" viewBox="0 0 700 240" preserveAspectRatio="none">
              <!-- Đường lưới ngang mờ -->
              <line x1="40" y1="30" x2="680" y2="30" stroke="#f1ede7" stroke-dasharray="3 3" />
              <line x1="40" y1="80" x2="680" y2="80" stroke="#f1ede7" stroke-dasharray="3 3" />
              <line x1="40" y1="130" x2="680" y2="130" stroke="#f1ede7" stroke-dasharray="3 3" />
              <line x1="40" y1="180" x2="680" y2="180" stroke="#f1ede7" stroke-dasharray="3 3" />
              <line x1="40" y1="200" x2="680" y2="200" stroke="#e6e0d6" />

              <!-- Các cột Bar Chart -->
              <g v-for="(b, idx) in barChartComputedData" :key="idx">
                <!-- Cột hình chữ nhật màu nâu be (#C8B6A6) -->
                <rect
                  :x="b.x"
                  :y="b.y"
                  :width="b.width"
                  :height="b.height"
                  rx="5"
                  fill="#C8B6A6"
                  class="chart-bar-rect"
                >
                  <title>{{ b.date }}: {{ b.value }} sản phẩm ({{ formatCurrency(b.revenue) }})</title>
                </rect>

                <!-- Con số tổng MÀU ĐỎ trên đỉnh cột -->
                <text
                  :x="b.x + b.width / 2"
                  :y="b.y - 7"
                  text-anchor="middle"
                  class="bar-peak-number"
                >
                  {{ b.value }}
                </text>

                <!-- Nhãn ngày tháng ở đáy cột -->
                <text
                  :x="b.x + b.width / 2"
                  y="222"
                  text-anchor="middle"
                  class="bar-bottom-label"
                >
                  {{ b.label }}
                </text>
              </g>
            </svg>
          </div>
        </div>
      </div>

      <!-- Bên phải (1/3): Biểu đồ Thương hiệu (Donut Chart) -->
      <div class="content-card chart-card-col-1">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 2a10 10 0 0 1 10 10" />
            </svg>
            <h3 class="card-heading">Thương hiệu</h3>
          </div>
          <button type="button" class="btn-toggle-metric" @click="toggleBrandMetric">
            {{ brandMetric === 'revenue' ? 'Theo doanh thu' : 'Theo số lượng' }}
          </button>
        </div>

        <!-- Biểu đồ Donut Thương hiệu -->
        <div class="donut-chart-wrapper">
          <div class="donut-svg-box">
            <svg class="donut-svg" viewBox="0 0 180 180">
              <circle cx="90" cy="90" r="70" fill="none" stroke="#f5f2eb" stroke-width="26" />
              <circle
                v-for="(slice, i) in brandDonutSlices"
                :key="i"
                cx="90"
                cy="90"
                r="70"
                fill="none"
                :stroke="slice.color"
                stroke-width="26"
                :stroke-dasharray="slice.dashArray"
                :stroke-dashoffset="slice.dashOffset"
                class="donut-segment"
              >
                <title>{{ slice.name }}: {{ slice.percentage }}% ({{ brandMetric === 'revenue' ? formatCurrency(slice.value) : slice.value + ' chiếc' }})</title>
              </circle>
            </svg>
            <div class="donut-center-info">
              <span class="donut-center-label">Tổng</span>
              <strong class="donut-center-val">{{ brandDonutTotalDisplay }}</strong>
            </div>
          </div>

          <!-- Chú thích thương hiệu -->
          <div class="donut-legend-list">
            <div v-for="(slice, i) in brandDonutSlices" :key="i" class="legend-row-item">
              <div class="legend-left-col">
                <span class="legend-color-dot" :style="{ backgroundColor: slice.color }"></span>
                <span class="legend-name-text">{{ slice.name }}</span>
              </div>
              <span class="legend-percent-tag">{{ slice.percentage }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. Khu vực Bảng tổng hợp theo thời gian & Biểu đồ trạng thái đơn (Tỷ lệ 2:1) -->
    <div class="charts-row-split-2-1">
      <!-- Bên trái (2/3): Bảng doanh thu theo mốc thời gian -->
      <div class="content-card time-table-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            <h3 class="card-heading">Doanh thu theo mốc thời gian</h3>
          </div>
          <span class="badge-fixed-row">4 mốc so sánh</span>
        </div>

        <div class="table-responsive-clean">
          <table class="minimal-styled-table">
            <thead>
              <tr class="header-brown-beige">
                <th style="width: 130px">THỜI GIAN</th>
                <th style="width: 160px; text-align: right">TỔNG DOANH THU</th>
                <th style="width: 140px; text-align: right">TIỀN MẶT</th>
                <th style="width: 140px; text-align: right">CHUYỂN KHOẢN</th>
                <th style="width: 100px; text-align: center">SỐ ĐƠN</th>
                <th style="width: 130px; text-align: right">AOV</th>
                <th style="width: 130px; text-align: center">TĂNG TRƯỞNG</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in timeSummaryRows" :key="row.periodKey" class="table-data-row">
                <td class="font-bold text-dark">{{ row.periodName }}</td>
                <td style="text-align: right" class="font-bold text-black-bold">
                  {{ formatCurrency(row.totalRevenue) }}
                </td>
                <td style="text-align: right" class="text-blue-amount">
                  {{ formatCurrency(row.cash) }}
                </td>
                <td style="text-align: right" class="text-green-amount">
                  {{ formatCurrency(row.transfer) }}
                </td>
                <td style="text-align: center" class="font-bold text-black-bold">
                  {{ row.orderCount }}
                </td>
                <td style="text-align: right" class="text-gray-aov">
                  {{ formatCurrency(row.aov) }}
                </td>
                <td style="text-align: center">
                  <span
                    class="badge-growth-pill"
                    :class="Number(row.growthRate) > 0 ? 'growth-positive' : Number(row.growthRate) < 0 ? 'growth-negative' : 'growth-neutral'"
                  >
                    <span v-if="Number(row.growthRate) > 0">↑ +{{ row.growthRate }}%</span>
                    <span v-else-if="Number(row.growthRate) < 0">↓ {{ row.growthRate }}%</span>
                    <span v-else>0.0%</span>
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Bên phải (1/3): Biểu đồ Donut Trạng thái đơn hàng -->
      <div class="content-card order-status-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
              <polyline points="10 9 9 9 8 9" />
            </svg>
            <h3 class="card-heading">Trạng thái đơn hàng</h3>
          </div>
          <span class="badge-total-orders">Tổng {{ orderStatusTotal }} đơn</span>
        </div>

        <div class="donut-chart-wrapper">
          <div class="donut-svg-box order-status-donut-box">
            <svg class="donut-svg order-donut-svg" viewBox="0 0 260 180">
              <!-- Vòng tròn nền & các phân khúc trạng thái xoay -90 độ quanh tâm (85, 90) -->
              <g transform="rotate(-90 85 90)">
                <circle cx="85" cy="90" r="62" fill="none" stroke="#f5f2eb" stroke-width="24" />
                <circle
                  v-for="(st, i) in orderStatusDonutSlices"
                  :key="i"
                  cx="85"
                  cy="90"
                  r="62"
                  fill="none"
                  :stroke="st.color"
                  stroke-width="24"
                  :stroke-dasharray="st.dashArray"
                  :stroke-dashoffset="st.dashOffset"
                  class="donut-segment"
                >
                  <title>{{ st.label }}: {{ st.count }} đơn ({{ st.percentage }}%)</title>
                </circle>
              </g>

              <!-- Đường chỉ dẫn ghi chú trực tiếp (Leader pointer line + label) -->
              <g v-if="orderStatusTotal > 0" class="callout-leader-group">
                <circle cx="129" cy="46" r="3" fill="#16A34A" />
                <polyline points="129,46 150,26 248,26" fill="none" stroke="#16A34A" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                <text x="152" y="20" fill="#15803d" font-size="10.5" font-weight="700">
                  Hoàn thành {{ orderStatusCounts.completed }} ({{ orderStatusPercentages.completed }}%)
                </text>
              </g>
            </svg>
            <div class="donut-center-info order-donut-center">
              <span class="donut-center-label">Đơn hàng</span>
              <strong class="donut-center-val">{{ orderStatusTotal }}</strong>
            </div>
          </div>

          <!-- 3 chấm tròn chú thích (Legend): Đã hủy (đỏ), Hoàn thành (xanh lá), Đang xử lý (vàng cam) -->
          <div class="status-legend-footer">
            <div class="legend-dot-item">
              <span class="legend-circle-dot dot-red"></span>
              <span class="legend-text">Đã hủy ({{ orderStatusCounts.cancelled }})</span>
            </div>
            <div class="legend-dot-item">
              <span class="legend-circle-dot dot-green"></span>
              <span class="legend-text">Hoàn thành ({{ orderStatusCounts.completed }})</span>
            </div>
            <div class="legend-dot-item">
              <span class="legend-circle-dot dot-orange"></span>
              <span class="legend-text">Đang xử lý ({{ orderStatusCounts.processing }})</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. Khu vực Bảng 'Thống kê sản phẩm' (Full width ở cuối trang) -->
    <div class="content-card product-stats-card">
      <!-- Thanh công cụ đầu Card -->
      <div class="product-stats-toolbar">
        <div class="card-title-group">
          <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
            <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
            <line x1="12" y1="22.08" x2="12" y2="12" />
          </svg>
          <h3 class="card-heading">Thống kê sản phẩm</h3>
        </div>

        <div class="product-stats-actions">
          <!-- Ô tìm kiếm Mã, tên, thương hiệu -->
          <div class="input-with-prefix-icon search-product-box">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              v-model="productTableQuery"
              placeholder="Mã, tên, thương hiệu..."
              class="form-control-minimal with-icon"
              @input="onProductSearchChange"
            />
          </div>

          <!-- Combobox lọc sắp xếp -->
          <div class="select-wrapper-minimal">
            <select v-model="productSortBy" class="form-control-minimal" @change="sortProductTable">
              <option value="best_seller">Bán chạy nhất</option>
              <option value="slow_seller">Bán chậm nhất</option>
              <option value="highest_revenue">Doanh thu cao nhất</option>
              <option value="highest_stock">Tồn kho nhiều nhất</option>
              <option value="highest_return">Số lượng trả nhiều nhất</option>
            </select>
          </div>

          <!-- Nút Đặt lại -->
          <button type="button" class="btn-reset-round small" @click="resetProductTableFilters">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Đặt lại</span>
          </button>
        </div>
      </div>

      <!-- Cấu trúc Bảng sản phẩm 8 cột -->
      <div class="table-responsive-clean">
        <table class="minimal-styled-table">
          <thead>
            <tr class="header-brown-beige">
              <th style="width: 60px; text-align: center">STT</th>
              <th style="width: 140px">MÃ SẢN PHẨM</th>
              <th style="min-width: 240px">TÊN SẢN PHẨM</th>
              <th style="width: 150px">THƯƠNG HIỆU</th>
              <th style="width: 110px; text-align: center">ĐÃ BÁN</th>
              <th style="width: 120px; text-align: center">SỐ LƯỢNG TRẢ</th>
              <th style="width: 150px; text-align: right">DOANH THU</th>
              <th style="width: 120px; text-align: center">TỒN KHO</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paginatedProducts.length === 0">
              <td colspan="8" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                Không có dữ liệu sản phẩm phù hợp với bộ lọc hiện tại.
              </td>
            </tr>
            <tr v-for="(p, index) in paginatedProducts" :key="p.id || index" class="table-data-row">
              <td style="text-align: center" class="text-muted-stt">
                {{ (productPagination.page * productPagination.size) + index + 1 }}
              </td>
              <td class="font-bold text-dark code-cell">
                {{ p.code }}
              </td>
              <td>
                <div class="product-name-with-thumb">
                  <img
                    :src="p.image || defaultProductImage"
                    alt="thumbnail"
                    class="product-thumb-img"
                    @error="p.image = defaultProductImage"
                  />
                  <div class="product-info-text">
                    <strong class="product-title-bold">{{ p.name }}</strong>
                    <span class="product-size-hint">Size: {{ p.sizes || 'Free Size' }}</span>
                  </div>
                </div>
              </td>
              <td class="brand-name-cell">
                <span class="brand-badge-tag">{{ p.brand || 'FF-Tshirt' }}</span>
              </td>
              <td style="text-align: center" class="font-bold text-dark">
                {{ p.soldCount.toLocaleString('vi-VN') }}
              </td>
              <td style="text-align: center" class="text-red-return font-bold">
                {{ p.returnedCount }}
              </td>
              <td style="text-align: right" class="font-bold text-black-bold">
                {{ formatCurrency(p.revenue) }}
              </td>
              <td style="text-align: center" class="text-green-stock font-bold">
                {{ p.stock.toLocaleString('vi-VN') }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Chân bảng: Phân trang -->
      <div class="product-table-footer">
        <div class="footer-left-count">
          Hiển thị <strong>{{ filteredProductsList.length ? (productPagination.page * productPagination.size + 1) : 0 }} - {{ Math.min((productPagination.page + 1) * productPagination.size, filteredProductsList.length) }}</strong> / <strong>{{ filteredProductsList.length }}</strong> bản ghi
        </div>

        <div class="footer-center-pagination">
          <button
            type="button"
            class="pg-btn"
            :disabled="productPagination.page === 0"
            @click="goToProductPage(productPagination.page - 1)"
          >
            ‹
          </button>
          <button
            v-for="pNo in visibleProductPageNumbers"
            :key="pNo"
            type="button"
            class="pg-btn"
            :class="{ active: pNo === productPagination.page }"
            @click="goToProductPage(pNo)"
          >
            {{ pNo + 1 }}
          </button>
          <button
            type="button"
            class="pg-btn"
            :disabled="productPagination.page >= productTotalPages - 1"
            @click="goToProductPage(productPagination.page + 1)"
          >
            ›
          </button>
        </div>

        <div class="footer-right-size">
          <div class="select-wrapper-minimal size-select">
            <select v-model.number="productPagination.size" class="form-control-minimal" @change="productPagination.page = 0">
              <option :value="5">Hiển thị 5 bản ghi / trang</option>
              <option :value="10">Hiển thị 10 bản ghi / trang</option>
              <option :value="20">Hiển thị 20 bản ghi / trang</option>
              <option :value="50">Hiển thị 50 bản ghi / trang</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/api'
import * as XLSX from 'xlsx'
import { showToast } from '@/utils/dialog.js'

const defaultProductImage = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=150&auto=format&fit=crop&q=80'

// 1. Quản lý Bộ lọc thống kê
const quickFilter = ref('today')
const brandOptions = ref([])
const dateFilterError = ref('')
const loadingOverview = ref(false)

const filters = reactive({
  fromDate: getTodayDateString(),
  toDate: getTodayDateString(),
  statType: 'day',
  brand: '',
  productKeyword: ''
})

const statTypeHeading = computed(() => {
  if (filters.statType === 'week') return 'Tuần'
  if (filters.statType === 'month') return 'Tháng'
  if (filters.statType === 'year') return 'Năm'
  return 'Ngày'
})

function getTodayDateString() {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateDisplay(dStr) {
  if (!dStr) return '--/--/----'
  const parts = dStr.split('-')
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`
  }
  return dStr
}

function setQuickFilter(type) {
  quickFilter.value = type
  dateFilterError.value = ''
  const now = new Date()
  const todayStr = getTodayDateString()

  if (type === 'all') {
    filters.fromDate = '2024-01-01'
    filters.toDate = todayStr
  } else if (type === 'today') {
    filters.fromDate = todayStr
    filters.toDate = todayStr
  } else if (type === '7days') {
    const past7 = new Date()
    past7.setDate(now.getDate() - 6)
    const y = past7.getFullYear()
    const m = String(past7.getMonth() + 1).padStart(2, '0')
    const d = String(past7.getDate()).padStart(2, '0')
    filters.fromDate = `${y}-${m}-${d}`
    filters.toDate = todayStr
  } else if (type === 'thisMonth') {
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1)
    const y = firstDay.getFullYear()
    const m = String(firstDay.getMonth() + 1).padStart(2, '0')
    const d = String(firstDay.getDate()).padStart(2, '0')
    filters.fromDate = `${y}-${m}-${d}`
    filters.toDate = todayStr
  } else if (type === 'lastMonth') {
    const firstDayLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1)
    const lastDayLastMonth = new Date(now.getFullYear(), now.getMonth(), 0)
    const y1 = firstDayLastMonth.getFullYear()
    const m1 = String(firstDayLastMonth.getMonth() + 1).padStart(2, '0')
    const d1 = String(firstDayLastMonth.getDate()).padStart(2, '0')
    const y2 = lastDayLastMonth.getFullYear()
    const m2 = String(lastDayLastMonth.getMonth() + 1).padStart(2, '0')
    const d2 = String(lastDayLastMonth.getDate()).padStart(2, '0')
    filters.fromDate = `${y1}-${m1}-${d1}`
    filters.toDate = `${y2}-${m2}-${d2}`
  } else if (type === 'thisYear') {
    const y = now.getFullYear()
    filters.fromDate = `${y}-01-01`
    filters.toDate = todayStr
  }

  fetchOverviewData()
}

function onCustomDateChange() {
  if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
    dateFilterError.value = 'Từ ngày không được lớn hơn Đến ngày!'
    return
  }
  dateFilterError.value = ''
  quickFilter.value = 'custom'
  fetchOverviewData()
}

let filterTimer = null
function debounceFilter() {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(() => {
    fetchOverviewData()
  }, 250)
}

function resetFilters() {
  filters.statType = 'day'
  filters.brand = ''
  filters.productKeyword = ''
  productTableQuery.value = ''
  productSortBy.value = 'best_seller'
  setQuickFilter('today')
  showToast('Đã đặt lại bộ lọc thống kê về mặc định!', 'info')
}

function applyFilters() {
  if (dateFilterError.value) return
  fetchOverviewData()
}

// 2. Dữ liệu 6 Thẻ chỉ số tổng quan (KPIs)
const kpiData = reactive({
  totalRevenue: 0,
  cashRevenue: 0,
  transferRevenue: 0,
  totalOrders: 0,
  aov: 0,
  totalItemsSold: 0
})

// 3. Biểu đồ cột Bar Chart: Sản phẩm bán được theo Ngày / Tuần / Tháng / Năm
const rawDailySales = ref([])

const barChartComputedData = computed(() => {
  const data = rawDailySales.value
  if (!data || data.length === 0) return []

  const maxVal = Math.max(...data.map(d => Number(d.value || 0)), 10)
  const chartHeight = 150
  const baseY = 200
  const totalWidth = 620
  const startX = 45
  const count = data.length
  const barWidth = Math.min(38, Math.max(12, Math.floor((totalWidth / count) * 0.6)))
  const step = count > 1 ? (totalWidth - barWidth) / (count - 1) : 0

  return data.map((item, idx) => {
    const val = Number(item.value || 0)
    const h = Math.max((val / maxVal) * chartHeight, val > 0 ? 8 : 2)
    return {
      ...item,
      x: count === 1 ? (startX + totalWidth / 2 - barWidth / 2) : (startX + idx * step),
      y: baseY - h,
      width: barWidth,
      height: h
    }
  })
})

// 4. Biểu đồ Donut Thương hiệu
const brandMetric = ref('revenue') // 'revenue' hoặc 'quantity'
function toggleBrandMetric() {
  brandMetric.value = brandMetric.value === 'revenue' ? 'quantity' : 'revenue'
}

const rawBrandData = ref([])

const brandDonutTotalDisplay = computed(() => {
  if (brandMetric.value === 'revenue') {
    const total = rawBrandData.value.reduce((sum, b) => sum + Number(b.revenue || 0), 0)
    if (total >= 1000000000) return (total / 1000000000).toFixed(1) + 'B'
    if (total >= 1000000) return (total / 1000000).toFixed(1) + 'M'
    return total.toLocaleString('vi-VN') + ' đ'
  } else {
    const total = rawBrandData.value.reduce((sum, b) => sum + Number(b.quantity || 0), 0)
    return total + ' cái'
  }
})

const brandDonutSlices = computed(() => {
  const data = rawBrandData.value
  if (!data || data.length === 0) return []
  const metricKey = brandMetric.value
  const total = data.reduce((s, item) => s + Number(item[metricKey] || 0), 0) || 1
  const circumference = 2 * Math.PI * 70 // ~ 439.82

  let accumulatedPercent = 0
  return data.map(item => {
    const val = Number(item[metricKey] || 0)
    const percent = Math.round((val / total) * 100)
    const dashLength = (val / total) * circumference
    const offset = -accumulatedPercent * circumference
    accumulatedPercent += val / total

    return {
      name: item.name,
      value: val,
      percentage: percent,
      color: item.color || '#8C7355',
      dashArray: `${dashLength} ${circumference}`,
      dashOffset: offset
    }
  })
})

// 5. Bảng tổng hợp theo mốc thời gian (4 dòng cố định: Hôm nay, Tuần này, Tháng này, Năm nay)
const timeSummaryRows = ref([
  { periodKey: 'today', periodName: 'Hôm nay', totalRevenue: 0, cash: 0, transfer: 0, orderCount: 0, aov: 0, growthRate: 0.0 },
  { periodKey: 'thisWeek', periodName: 'Tuần này', totalRevenue: 0, cash: 0, transfer: 0, orderCount: 0, aov: 0, growthRate: 0.0 },
  { periodKey: 'thisMonth', periodName: 'Tháng này', totalRevenue: 0, cash: 0, transfer: 0, orderCount: 0, aov: 0, growthRate: 0.0 },
  { periodKey: 'thisYear', periodName: 'Năm nay', totalRevenue: 0, cash: 0, transfer: 0, orderCount: 0, aov: 0, growthRate: 0.0 }
])

// 6. Biểu đồ Donut Trạng thái đơn hàng (Đã hủy, Hoàn thành, Đang xử lý)
const orderStatusCounts = reactive({
  completed: 0,
  processing: 0,
  cancelled: 0
})

const orderStatusTotal = computed(() => {
  return orderStatusCounts.completed + orderStatusCounts.processing + orderStatusCounts.cancelled
})

const orderStatusPercentages = computed(() => {
  const tot = orderStatusTotal.value || 1
  return {
    completed: ((orderStatusCounts.completed / tot) * 100).toFixed(1),
    processing: ((orderStatusCounts.processing / tot) * 100).toFixed(1),
    cancelled: ((orderStatusCounts.cancelled / tot) * 100).toFixed(1)
  }
})

const orderStatusDonutSlices = computed(() => {
  const tot = orderStatusTotal.value || 1
  const circumference = 2 * Math.PI * 62
  const items = [
    { label: 'Hoàn thành', count: orderStatusCounts.completed, color: '#16A34A', percentage: orderStatusPercentages.value.completed },
    { label: 'Đang xử lý', count: orderStatusCounts.processing, color: '#F59E0B', percentage: orderStatusPercentages.value.processing },
    { label: 'Đã hủy', count: orderStatusCounts.cancelled, color: '#EF4444', percentage: orderStatusPercentages.value.cancelled }
  ]

  let acc = 0
  return items.map(it => {
    const fraction = it.count / tot
    const dashLength = fraction * circumference
    const offset = -acc * circumference
    acc += fraction

    return {
      ...it,
      dashArray: `${dashLength} ${circumference}`,
      dashOffset: offset
    }
  })
})

// 7. Bảng Thống kê sản phẩm (8 cột, tìm kiếm, sắp xếp, phân trang)
const productTableQuery = ref('')
const productSortBy = ref('best_seller')
const productPagination = reactive({
  page: 0,
  size: 10
})

const allProductStats = ref([])

function onProductSearchChange() {
  productPagination.page = 0
}

function sortProductTable() {
  productPagination.page = 0
}

function resetProductTableFilters() {
  productTableQuery.value = ''
  productSortBy.value = 'best_seller'
  productPagination.page = 0
  showToast('Đã đặt lại danh sách thống kê sản phẩm!', 'info')
}

const filteredProductsList = computed(() => {
  let list = [...allProductStats.value]
  const q = productTableQuery.value.trim().toLowerCase()

  if (q) {
    list = list.filter(p =>
      (p.code && p.code.toLowerCase().includes(q)) ||
      (p.name && p.name.toLowerCase().includes(q)) ||
      (p.brand && p.brand.toLowerCase().includes(q))
    )
  }

  if (productSortBy.value === 'best_seller') {
    list.sort((a, b) => Number(b.soldCount || 0) - Number(a.soldCount || 0))
  } else if (productSortBy.value === 'slow_seller') {
    list.sort((a, b) => Number(a.soldCount || 0) - Number(b.soldCount || 0))
  } else if (productSortBy.value === 'highest_revenue') {
    list.sort((a, b) => Number(b.revenue || 0) - Number(a.revenue || 0))
  } else if (productSortBy.value === 'highest_stock') {
    list.sort((a, b) => Number(b.stock || 0) - Number(a.stock || 0))
  } else if (productSortBy.value === 'highest_return') {
    list.sort((a, b) => Number(b.returnedCount || 0) - Number(a.returnedCount || 0))
  }

  return list
})

const productTotalPages = computed(() => {
  return Math.ceil(filteredProductsList.value.length / productPagination.size) || 1
})

const visibleProductPageNumbers = computed(() => {
  const total = productTotalPages.value
  const curr = productPagination.page
  const start = Math.max(0, curr - 2)
  const end = Math.min(total, start + 5)
  return Array.from({ length: end - start }, (_, i) => start + i)
})

const paginatedProducts = computed(() => {
  const start = productPagination.page * productPagination.size
  return filteredProductsList.value.slice(start, start + productPagination.size)
})

function goToProductPage(page) {
  if (page < 0 || page >= productTotalPages.value) return
  productPagination.page = page
}

// 8. Định dạng số tiền
function formatCurrency(val) {
  if (val == null || isNaN(val)) return '0 đ'
  return Math.round(Number(val)).toLocaleString('vi-VN') + ' đ'
}

// 9. Lấy dữ liệu thực từ Backend API
async function fetchOverviewData() {
  if (dateFilterError.value) return
  loadingOverview.value = true
  try {
    const params = {
      fromDate: filters.fromDate,
      toDate: filters.toDate,
      statType: filters.statType || 'day'
    }
    if (filters.brand) params.brandId = filters.brand
    if (filters.productKeyword && filters.productKeyword.trim()) {
      params.productKeyword = filters.productKeyword.trim()
    }

    const res = await api.get('/api/thong-ke/overview', { params })
    const data = res.data
    if (data) {
      if (data.kpi) {
        kpiData.totalRevenue = Number(data.kpi.totalRevenue || 0)
        kpiData.cashRevenue = Number(data.kpi.cashRevenue || 0)
        kpiData.transferRevenue = Number(data.kpi.transferRevenue || 0)
        kpiData.totalOrders = Number(data.kpi.totalOrders || 0)
        kpiData.aov = Number(data.kpi.aov || 0)
        kpiData.totalItemsSold = Number(data.kpi.totalItemsSold || 0)
      }

      if (Array.isArray(data.barChart)) {
        rawDailySales.value = data.barChart
      }

      if (Array.isArray(data.brandDonut)) {
        rawBrandData.value = data.brandDonut
      }

      if (data.orderStatusDonut) {
        orderStatusCounts.completed = Number(data.orderStatusDonut.completed || 0)
        orderStatusCounts.processing = Number(data.orderStatusDonut.processing || 0)
        orderStatusCounts.cancelled = Number(data.orderStatusDonut.cancelled || 0)
      }

      if (Array.isArray(data.timeSummary)) {
        timeSummaryRows.value = data.timeSummary
      }

      if (Array.isArray(data.products)) {
        allProductStats.value = data.products
      }
    }
  } catch (err) {
    console.error('Lỗi tải dữ liệu thống kê:', err)
    showToast(err?.response?.data?.message || 'Không thể tải dữ liệu thống kê từ hệ thống', 'error')
  } finally {
    loadingOverview.value = false
  }
}

// 10. Xuất file Excel thực tế (Ưu tiên gọi API Backend, fallback Client)
async function handleExportExcel() {
  try {
    const params = {
      fromDate: filters.fromDate,
      toDate: filters.toDate,
      statType: filters.statType || 'day'
    }
    if (filters.brand) params.brandId = filters.brand
    if (filters.productKeyword && filters.productKeyword.trim()) {
      params.productKeyword = filters.productKeyword.trim()
    }

    const response = await api.get('/api/thong-ke/export-excel', {
      params,
      responseType: 'blob'
    })

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const fileName = `BaoCao_ThongKe_DoanhThu_${filters.fromDate}_den_${filters.toDate}.xlsx`
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    showToast('Xuất file Excel thống kê thành công!', 'success')
  } catch (err) {
    console.error('Lỗi xuất Excel từ server, dùng fallback client:', err)
    exportClientExcelFallback()
  }
}

function exportClientExcelFallback() {
  try {
    const wb = XLSX.utils.book_new()
    const kpiSummaryRows = [
      ['BÁO CÁO THỐNG KÊ DOANH THU & KINH DOANH'],
      ['Thời gian lọc:', `Từ ${formatDateDisplay(filters.fromDate)} đến ${formatDateDisplay(filters.toDate)}`],
      ['Kiểu thống kê:', filters.statType === 'day' ? 'Theo ngày' : filters.statType === 'week' ? 'Theo tuần' : filters.statType === 'month' ? 'Theo tháng' : 'Theo năm'],
      [],
      ['CHỈ SỐ TỔNG QUAN (KPIS)', 'GIÁ TRỊ'],
      ['Tổng doanh thu', kpiData.totalRevenue],
      ['Tiền mặt', kpiData.cashRevenue],
      ['Chuyển khoản', kpiData.transferRevenue],
      ['Tổng đơn hàng', kpiData.totalOrders],
      ['Giá trị TB đơn (AOV)', kpiData.aov],
      ['Sản phẩm đã bán', kpiData.totalItemsSold],
      [],
      ['DOANH THU THEO MỐC THỜI GIAN'],
      ['Thời gian', 'Tổng doanh thu', 'Tiền mặt', 'Chuyển khoản', 'Số đơn', 'AOV', 'Tăng trưởng (%)']
    ]

    timeSummaryRows.value.forEach(r => {
      kpiSummaryRows.push([
        r.periodName,
        r.totalRevenue,
        r.cash,
        r.transfer,
        r.orderCount,
        r.aov,
        `${r.growthRate}%`
      ])
    })

    const wsSummary = XLSX.utils.aoa_to_sheet(kpiSummaryRows)
    XLSX.utils.book_append_sheet(wb, wsSummary, 'TongQuan')

    const productHeader = ['STT', 'Mã sản phẩm', 'Tên sản phẩm', 'Thương hiệu', 'Kích cỡ', 'Đã bán', 'Số lượng trả', 'Doanh thu (đ)', 'Tồn kho']
    const productRows = [productHeader]

    filteredProductsList.value.forEach((p, idx) => {
      productRows.push([
        idx + 1,
        p.code,
        p.name,
        p.brand,
        p.sizes,
        p.soldCount,
        p.returnedCount,
        p.revenue,
        p.stock
      ])
    })

    const wsProducts = XLSX.utils.aoa_to_sheet(productRows)
    XLSX.utils.book_append_sheet(wb, wsProducts, 'ThongKeSanPham')

    const fileName = `Thong-ke-doanh-thu-${filters.fromDate}-den-${filters.toDate}.xlsx`
    XLSX.writeFile(wb, fileName)
    showToast('Xuất file Excel thống kê thành công!', 'success')
  } catch (e) {
    showToast('Có lỗi xảy ra khi xuất file Excel.', 'error')
  }
}

// 11. Tải danh sách thương hiệu và kết nối dữ liệu ban đầu
async function loadBrandOptions() {
  try {
    const res = await api.get('/api/thuoc-tinh/thuong-hieu')
    brandOptions.value = res.data || []
  } catch (e) {
    brandOptions.value = []
  }
}

onMounted(async () => {
  await loadBrandOptions()
  await fetchOverviewData()
})
</script>

<style scoped>
/* Khung tổng thể trang Thống kê */
.statistics-page-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: #f7f5ef;
  min-height: calc(100vh - 48px);
  color: #3d4a50;
  box-sizing: border-box;
  font-family: inherit;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Card chuẩn thiết kế: Nền trắng, bo góc rounded-2xl, viền mảnh */
.content-card {
  background-color: #ffffff;
  border-radius: 16px;
  border: 1px solid #ece6dc;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.03);
  padding: 1.25rem 1.4rem;
  box-sizing: border-box;
}

/* 1. Tiêu đề trang */
.page-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background-color: #f1ede7;
  color: #8c7355;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(140, 115, 85, 0.12);
}

.page-main-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: -0.2px;
}

/* Card Bộ lọc thống kê */
.filter-card-box {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

/* Hàng trên: Tiêu đề lọc & Cụm nút pill */
.filter-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.85rem;
}

.filter-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-icon {
  color: #8c7355;
}

.filter-title-text {
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
}

.filter-pills-cluster {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.pill-btn {
  background-color: #fbf9f6;
  border: 1px solid #e2dbcf;
  color: #556268;
  font-size: 0.84rem;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.pill-btn:hover {
  background-color: #f0ebe1;
  border-color: #d1c6b5;
  color: #3b4d55;
}

/* Nút đang chọn: tone màu nâu be nhạt (#D8CBB8 / #8C7355) */
.pill-btn.active {
  background-color: #d8cbb8;
  border-color: #c2b19b;
  color: #4a3824;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(140, 115, 85, 0.18);
}

.btn-export-excel {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #d8cbb8;
  border-color: #c4b5a2;
  color: #43321f;
  font-weight: 700;
  margin-left: 6px;
}

.btn-export-excel:hover {
  background-color: #c8b7a0;
  border-color: #b5a28b;
  color: #2e2213;
}

/* Hàng giữa: 5 trường lọc + Nút Đặt lại */
.filter-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1.1fr 1.6fr auto;
  gap: 12px;
  align-items: flex-end;
  padding-top: 1rem;
  border-top: 1px solid #f2eee8;
}

.filter-field-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label-tiny {
  font-size: 0.78rem;
  font-weight: 700;
  color: #6c787e;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.form-control-minimal {
  width: 100%;
  height: 38px;
  background-color: #fcfbf9;
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 0.88rem;
  color: #334155;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-control-minimal:focus {
  border-color: #8c7355;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(140, 115, 85, 0.12);
}

.input-error-border {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}

.filter-error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #dc2626;
  font-size: 0.82rem;
  font-weight: 600;
  margin-top: 4px;
}

.input-with-prefix-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 10px;
  color: #8c7355;
  pointer-events: none;
}

.form-control-minimal.with-icon {
  padding-left: 32px;
}

.select-wrapper-minimal {
  position: relative;
}

.btn-reset-round {
  height: 38px;
  padding: 0 14px;
  border-radius: 20px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  color: #5d6c73;
  font-size: 0.84rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-reset-round:hover {
  background-color: #f5efe7;
  border-color: #b5a494;
  color: #39464c;
}

.btn-reset-round.small {
  height: 34px;
  padding: 0 11px;
  font-size: 0.8rem;
}

/* Hàng dưới cùng: Trạng thái ngày */
.filter-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.86rem;
  color: #55656d;
}

.blue-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  display: inline-block;
  flex-shrink: 0;
}

.filter-status-text strong {
  color: #1e293b;
  font-weight: 700;
}

/* 2. Hàng 6 Thẻ chỉ số tổng quan (6 KPI Cards) */
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  background-color: #ffffff;
  border-radius: 16px;
  border: 1px solid #ece6dc;
  box-shadow: 0 3px 14px rgba(0, 0, 0, 0.025);
  padding: 1.15rem 1rem;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.05);
}

.kpi-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.85rem;
}

.pastel-green { background-color: #e8f5e9; color: #2e7d32; }
.pastel-blue { background-color: #e3f2fd; color: #1565c0; }
.pastel-emerald { background-color: #e0f2f1; color: #00796b; }
.pastel-purple { background-color: #f3e5f5; color: #7b1fa2; }
.pastel-pink { background-color: #fce4ec; color: #c2185b; }
.pastel-orange { background-color: #fff3e0; color: #e65100; }

.kpi-value {
  font-size: 1.25rem;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 0.35rem;
  letter-spacing: -0.3px;
  word-break: break-word;
}

.kpi-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: #7e8b93;
}

/* 3 & 4. Lưới biểu đồ 2:1 */
.charts-row-split-2-1 {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.1rem;
}

.card-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-sub-icon {
  color: #8c7355;
}

.card-heading {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
}

.badge-unit,
.badge-fixed-row,
.badge-total-orders {
  font-size: 0.74rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #f2ede7;
  color: #76695b;
}

/* Bar Chart styling */
.bar-chart-container {
  width: 100%;
  padding-top: 0.5rem;
}

.bar-chart-svg-wrap {
  width: 100%;
  height: 230px;
}

.bar-chart-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.chart-bar-rect {
  transition: fill 0.2s, opacity 0.2s;
  cursor: pointer;
}

.chart-bar-rect:hover {
  fill: #8c7355;
  opacity: 0.95;
}

.bar-peak-number {
  font-size: 12px;
  font-weight: 700;
  fill: #dc2626; /* Số đỏ trên đỉnh cột */
}

.bar-bottom-label {
  font-size: 11px;
  font-weight: 600;
  fill: #64748b;
}

/* Donut Chart styling */
.btn-toggle-metric {
  background: #fbf9f6;
  border: 1px solid #d5cbc0;
  padding: 3px 9px;
  font-size: 0.76rem;
  font-weight: 600;
  color: #635345;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-toggle-metric:hover {
  background: #f1ebd8;
}

.donut-chart-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 0.4rem;
}

.donut-svg-box {
  position: relative;
  width: 170px;
  height: 170px;
}

.order-status-donut-box {
  width: 250px;
  height: 170px;
}

.donut-svg {
  width: 100%;
  height: 100%;
}

.donut-svg:not(.order-donut-svg) {
  transform: rotate(-90deg);
}

.order-donut-svg {
  transform: none;
  overflow: visible;
}

.order-donut-center {
  width: 170px;
  left: 0;
  right: auto;
}

.donut-segment {
  transition: stroke-width 0.2s, opacity 0.2s;
  cursor: pointer;
}

.donut-segment:hover {
  stroke-width: 30;
  opacity: 0.9;
}

.donut-center-info {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.donut-center-label {
  font-size: 0.74rem;
  color: #8c979d;
  font-weight: 500;
}

.donut-center-val {
  font-size: 1.15rem;
  color: #2c3e50;
  font-weight: 700;
}

.donut-legend-list {
  width: 100%;
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.legend-row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.82rem;
  padding: 2px 4px;
}

.legend-left-col {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-name-text {
  color: #475569;
  font-weight: 500;
}

.legend-percent-tag {
  font-weight: 700;
  color: #1e293b;
}

/* Callout trạng thái đơn hàng & footer legend */
.order-callout-status {
  margin-top: 0.65rem;
  margin-bottom: 0.5rem;
}

.callout-pill.green-callout {
  display: inline-block;
  background-color: #dcfce7;
  color: #15803d;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 9999px;
  border: 1px solid #bbf7d0;
}

.status-legend-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 0.75rem;
  flex-wrap: wrap;
}

.legend-dot-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  color: #556268;
}

.legend-circle-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.dot-red { background-color: #ef4444; }
.dot-green { background-color: #22c55e; }
.dot-orange { background-color: #f59e0b; }

/* 4 & 5. Bảng dữ liệu tối giản: Header nền nâu be nhạt (#DDD3C5) */
.table-responsive-clean {
  overflow-x: auto;
}

.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.minimal-styled-table th {
  background-color: #ddd3c5 !important; /* Header màu nâu be nhạt */
  color: #3b2f23;
  font-weight: 700;
  font-size: 0.78rem;
  letter-spacing: 0.5px;
  padding: 10px 14px;
  text-transform: uppercase;
  border: none;
}

.minimal-styled-table th:first-child {
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

.minimal-styled-table th:last-child {
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}

.table-data-row {
  border-bottom: 1px solid #f3eee7;
  transition: background-color 0.15s;
}

.table-data-row:hover {
  background-color: #faf7f2;
}

.minimal-styled-table td {
  padding: 12px 14px;
  vertical-align: middle;
}

/* Định dạng màu sắc cột theo yêu cầu */
.text-black-bold {
  color: #0f172a;
  font-weight: 700;
}

.text-blue-amount {
  color: #2563eb;
  font-weight: 600;
}

.text-green-amount {
  color: #16a34a;
  font-weight: 600;
}

.text-gray-aov {
  color: #64748b;
  font-weight: 500;
}

.badge-growth-pill {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 0.76rem;
  font-weight: 700;
}

.growth-positive {
  background-color: #dcfce7;
  color: #15803d;
}

.growth-negative {
  background-color: #fee2e2;
  color: #b91c1c;
}

/* 5. Khu vực Bảng Thống kê sản phẩm */
.product-stats-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.15rem;
  flex-wrap: wrap;
  gap: 12px;
}

.product-stats-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-product-box {
  width: 240px;
}

.product-name-with-thumb {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb-img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #ece6dc;
  background-color: #f7f5ef;
  flex-shrink: 0;
}

.product-info-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.product-title-bold {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.35;
}

.product-size-hint {
  font-size: 0.76rem;
  color: #8c979d;
  font-weight: 500;
}

.brand-badge-tag {
  background-color: #f5f2eb;
  color: #605141;
  font-size: 0.76rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.text-red-return {
  color: #dc2626;
}

.text-green-stock {
  color: #16a34a;
}

.text-muted-stt {
  color: #94a3b8;
}

.code-cell {
  letter-spacing: 0.2px;
}

/* Chân bảng: Phân trang */
.product-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 1.15rem;
  margin-top: 0.5rem;
  border-top: 1px solid #f1ece5;
  flex-wrap: wrap;
  gap: 12px;
}

.footer-left-count {
  font-size: 0.85rem;
  color: #64748b;
}

.footer-left-count strong {
  color: #1e293b;
}

.footer-center-pagination {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pg-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e2dbcf;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.84rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.pg-btn:hover:not(:disabled) {
  background-color: #f5efe7;
  border-color: #bfae9d;
  color: #2c3e50;
}

.pg-btn.active {
  background-color: #8c7355 !important; /* Nút phân trang active màu nâu đậm */
  border-color: #8c7355 !important;
  color: #ffffff !important;
}

.pg-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.size-select {
  width: 210px;
}

/* Responsive adjustments */
@media (max-width: 1280px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .charts-row-split-2-1 {
    grid-template-columns: 1fr;
  }
  .filter-fields-row {
    grid-template-columns: 1fr 1fr;
  }
  .kpi-cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .statistics-page-wrapper {
    padding: 1rem;
  }
  .filter-fields-row {
    grid-template-columns: 1fr;
  }
  .kpi-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>