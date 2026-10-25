<template>
  <div class="statistics-page-wrapper">
    <!-- ================================================================= -->
    <!-- VIEW 1: MÀN HÌNH THỐNG KÊ DOANH THU CHÍNH (OVERVIEW)              -->
    <!-- ================================================================= -->
    <template v-if="viewMode === 'overview'">
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
          <div>
            <h1 class="page-main-title">Thống kê doanh thu</h1>
            <p class="page-sub-title">Tổng hợp kinh doanh, phân tích sản phẩm và theo dõi tăng trưởng bán hàng</p>
          </div>
        </div>

        <div class="top-bar-actions">
          <!-- Nút Gửi mail tổng kết nổi bật ở header -->
          <button
            type="button"
            class="pill-btn btn-send-mail-highlight"
            @click="openEmailModal"
            title="Mở bảng cấu hình và gửi email tổng kết doanh thu cho Admin"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
              <polyline points="22,6 12,13 2,6" />
            </svg>
            <span>📧 Gửi mail tổng kết</span>
          </button>

          <!-- Nút Chuyển sang màn hình So sánh -->
          <button
            type="button"
            class="pill-btn btn-compare-highlight"
            @click="switchViewMode('comparison')"
            title="Mở màn hình So sánh & Đối chiếu tăng trưởng doanh thu"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 3h5v5" />
              <path d="M4 20L21 3" />
              <path d="M21 16v5h-5" />
              <path d="M15 15l6 6" />
              <path d="M4 4l5 5" />
            </svg>
            <span>So sánh doanh thu</span>
          </button>
        </div>
      </div>

      <!-- 2. Card Bộ lọc thống kê -->
      <div class="content-card filter-card-box">
        <!-- Hàng trên: Tiêu đề bộ lọc & Cụm nút lọc nhanh + Gửi mail + So sánh + Xuất Excel -->
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

            <!-- Nút Gửi mail tổng kết cạnh So sánh & Xuất Excel -->
            <button
              type="button"
              class="pill-btn btn-send-mail-action"
              @click="openEmailModal"
              title="Gửi email tổng kết doanh thu cho Admin"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
              <span>📧 Gửi mail tổng kết</span>
            </button>

            <!-- Nút So sánh -->
            <button
              type="button"
              class="pill-btn btn-compare-pill"
              @click="switchViewMode('comparison')"
              title="So sánh đối chiếu doanh thu theo Tháng / Quý / Năm"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
              <span>So sánh</span>
            </button>

            <!-- Nút Xuất Excel -->
            <button
              type="button"
              class="pill-btn btn-export-excel"
              @click="handleExportExcel"
              title="Xuất dữ liệu thống kê ra file Excel"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              <span>Xuất Excel</span>
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

      <!-- 3. Hàng 6 Thẻ chỉ số tổng quan (6 KPI Cards) -->
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

      <!-- 4. Biểu đồ Cột Sản phẩm bán được (Full Width - Tỷ lệ 1:1, font chuẩn sắc nét không bẹp) -->
      <div class="content-card full-chart-card">
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

        <!-- Biểu đồ cột Bar Chart với tỷ lệ chuẩn, cuộn ngang khi > 12 cột, xoay nghiêng -45deg chống đè chữ -->
        <div class="bar-chart-container" @mouseleave="onBarMouseLeave">
          <!-- Gợi ý cuộn ngang khi có nhiều cột -->
          <div v-if="barChartCount > 12" class="chart-scroll-hint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="13 17 18 12 13 7" />
              <polyline points="6 17 11 12 6 7" />
            </svg>
            <span>Cuộn ngang để xem chi tiết toàn bộ {{ barChartCount }} mốc thời gian</span>
          </div>

          <!-- Khung cuộn ngang mượt mà -->
          <div class="bar-chart-scroll-wrap">
            <div class="bar-chart-svg-wrap" :style="{ width: barChartSvgWidthStyle, minWidth: '100%' }">
              <svg
                class="bar-chart-svg"
                :viewBox="`0 0 ${barChartSvgWidth} 320`"
                preserveAspectRatio="xMidYMid meet"
              >
                <!-- Đường lưới ngang mờ -->
                <line x1="20" y1="50" :x2="barChartSvgWidth - 20" y2="50" stroke="#f1ede7" stroke-dasharray="3 3" />
                <line x1="20" y1="100" :x2="barChartSvgWidth - 20" y2="100" stroke="#f1ede7" stroke-dasharray="3 3" />
                <line x1="20" y1="150" :x2="barChartSvgWidth - 20" y2="150" stroke="#f1ede7" stroke-dasharray="3 3" />
                <line x1="20" y1="200" :x2="barChartSvgWidth - 20" y2="200" stroke="#f1ede7" stroke-dasharray="3 3" />
                <line x1="20" y1="240" :x2="barChartSvgWidth - 20" y2="240" stroke="#e2dbcf" stroke-width="1.2" />

                <!-- Các cột Bar Chart -->
                <g
                  v-for="(b, idx) in barChartComputedData"
                  :key="idx"
                  class="chart-bar-group"
                  @mouseenter="onBarHover(b, $event)"
                  @mousemove="onBarHover(b, $event)"
                >
                  <!-- Vùng nhận hover rộng bao phủ toàn bộ cột -->
                  <rect
                    :x="b.x - (b.stepGap || 6)"
                    y="30"
                    :width="b.width + (b.stepGap || 6) * 2"
                    height="225"
                    fill="transparent"
                    class="bar-hit-area"
                  />

                  <!-- Vệt highlight khi hover -->
                  <rect
                    v-if="hoveredBar && hoveredBar.label === b.label"
                    :x="b.x - 4"
                    y="45"
                    :width="b.width + 8"
                    height="195"
                    fill="#f1ede7"
                    opacity="0.65"
                    rx="6"
                    class="bar-hover-guide"
                  />

                  <!-- Cột dữ liệu thực tế -->
                  <rect
                    :x="b.x"
                    :y="b.y"
                    :width="b.width"
                    :height="b.height"
                    rx="6"
                    :fill="hoveredBar && hoveredBar.label === b.label ? '#8C7355' : (b.value > 0 ? '#C8B6A6' : '#E2DBCF')"
                    class="chart-bar-rect"
                    :class="{ 'is-hovered': hoveredBar && hoveredBar.label === b.label }"
                  />

                  <!-- Con số trên đỉnh cột:
                       - Nếu value > 0: màu đỏ đậm nổi bật
                       - Nếu value = 0: chỉ hiện khi số cột <= 12 (màu xám mờ #9ca3af), ẩn đi khi > 12 cột để tránh rối mắt -->
                  <text
                    v-if="b.value > 0"
                    :x="b.centerX"
                    :y="b.y - 8"
                    text-anchor="middle"
                    class="bar-peak-number positive"
                  >
                    {{ b.value }}
                  </text>
                  <text
                    v-else-if="barChartCount <= 12"
                    :x="b.centerX"
                    :y="b.y - 6"
                    text-anchor="middle"
                    class="bar-peak-number zero"
                  >
                    0
                  </text>

                  <!-- Nhãn ngày tháng ở đáy cột:
                       - Khi isRotated = true: xoay nghiêng -45deg, text-anchor="end", tuyệt đối không bao giờ đè chữ
                       - Khi isRotated = false: text-anchor="middle" thẳng đứng chuẩn mực -->
                  <g v-if="b.showLabel && b.isRotated" :transform="`translate(${b.centerX}, 258) rotate(-45)`">
                    <text
                      x="0"
                      y="0"
                      text-anchor="end"
                      class="bar-bottom-label rotated"
                      :class="{ 'label-highlight': hoveredBar && hoveredBar.label === b.label }"
                    >
                      {{ b.label }}
                    </text>
                  </g>
                  <text
                    v-else-if="b.showLabel"
                    :x="b.centerX"
                    y="262"
                    text-anchor="middle"
                    class="bar-bottom-label"
                    :class="{ 'label-highlight': hoveredBar && hoveredBar.label === b.label }"
                  >
                    {{ b.label }}
                  </text>
                </g>
              </svg>
            </div>
          </div>

          <!-- Tooltip nổi tương tác cao cấp -->
          <div
            v-if="hoveredBar"
            class="bar-chart-floating-tooltip"
            :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px' }"
          >
            <div class="tooltip-header-clean">
              <span class="tooltip-cal-badge">📅</span>
              <strong class="tooltip-date-txt">{{ hoveredBar.dateTitle }}</strong>
            </div>
            <div class="tooltip-divider-line"></div>
            <div class="tooltip-data-row">
              <span class="tooltip-row-lbl">📦 Số lượng bán:</span>
              <span class="tooltip-row-val font-bold text-amber">
                {{ hoveredBar.value.toLocaleString('vi-VN') }} sản phẩm
              </span>
            </div>
            <div class="tooltip-data-row">
              <span class="tooltip-row-lbl">💰 Doanh thu:</span>
              <span class="tooltip-row-val font-bold text-emerald">
                {{ formatCurrency(hoveredBar.revenue) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 5. Hàng 2 Biểu đồ Donut: Thương hiệu (50%) & Trạng thái đơn hàng (50%) -->
      <div class="two-donuts-grid">
        <!-- Donut 1: Thương hiệu -->
        <div class="content-card donut-card-item">
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

        <!-- Donut 2: Trạng thái đơn hàng -->
        <div class="content-card donut-card-item">
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

      <!-- 6. Khu vực Bảng 'Thống kê sản phẩm' (Đồng bộ 100% với Bộ lọc trên) -->
      <div class="content-card product-stats-card">
        <div class="product-stats-toolbar">
          <div class="card-title-group">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
              <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
              <line x1="12" y1="22.08" x2="12" y2="12" />
            </svg>
            <h3 class="card-heading">Thống kê sản phẩm</h3>
            <span class="badge-filter-status" v-if="filters.brand || filters.productKeyword">
              Đang lọc theo bộ lọc phía trên
            </span>
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
                placeholder="Tìm trong bảng..."
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

            <!-- Nút Đặt lại riêng của bảng -->
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
    </template>

    <!-- ================================================================= -->
    <!-- VIEW 2: MÀN HÌNH SO SÁNH & TĂNG TRƯỞNG DOANH THU CHUYÊN SÂU        -->
    <!-- ================================================================= -->
    <template v-else-if="viewMode === 'comparison'">
      <!-- Thanh tiêu đề trang So sánh -->
      <div class="page-top-bar comparison-top-bar">
        <div class="title-with-icon">
          <button type="button" class="btn-back-square" @click="switchViewMode('overview')" title="Quay lại Thống kê chung">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
          </button>
          <div>
            <h1 class="page-main-title">So sánh & Tăng trưởng doanh thu</h1>
            <p class="page-sub-title">Đối chiếu doanh thu, đo lường tốc độ tăng trưởng và theo dõi biến động sức mua sản phẩm</p>
          </div>
        </div>

        <div class="top-bar-actions">
          <!-- Gửi mail tổng kết -->
          <button type="button" class="pill-btn btn-send-mail-highlight" @click="openEmailModal">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
              <polyline points="22,6 12,13 2,6" />
            </svg>
            <span>📧 Gửi mail tổng kết</span>
          </button>

          <!-- Xuất Excel so sánh -->
          <button type="button" class="pill-btn btn-export-excel" @click="handleExportComparisonExcel">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span>Xuất Excel So sánh</span>
          </button>
        </div>
      </div>

      <!-- Card Bộ lọc riêng của trang So sánh -->
      <div class="content-card filter-card-box">
        <!-- Hàng 1: Chế độ so sánh -->
        <div class="filter-top-row">
          <div class="filter-heading">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="filter-icon">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
            </svg>
            <span class="filter-title-text">Chế độ đối chiếu:</span>
          </div>

          <div class="filter-pills-cluster">
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'month' }"
              @click="setCompareMode('month')"
            >
              Theo Tháng (12 tháng)
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'quarter' }"
              @click="setCompareMode('quarter')"
            >
              Theo Quý (4 quý)
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'year' }"
              @click="setCompareMode('year')"
            >
              Theo Năm (Đối chiếu nhiều năm)
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'custom' }"
              @click="setCompareMode('custom')"
            >
              Tùy chọn 2 kỳ
            </button>
          </div>
        </div>

        <!-- Hàng 2: Các trường lọc phụ thuộc theo chế độ -->
        <div class="filter-fields-row comparison-fields-row">
          <!-- Nếu xem theo Tháng hoặc Quý: Chọn Năm -->
          <div v-if="compareFilter.mode === 'month' || compareFilter.mode === 'quarter'" class="filter-field-item">
            <label class="field-label-tiny">Năm so sánh</label>
            <div class="select-wrapper-minimal">
              <select v-model.number="compareFilter.year" class="form-control-minimal" @change="fetchComparisonData">
                <option v-for="y in availableYears" :key="y" :value="y">Năm {{ y }}</option>
              </select>
            </div>
          </div>

          <!-- Nếu xem theo Năm: Chọn Từ năm - Đến năm -->
          <template v-if="compareFilter.mode === 'year'">
            <div class="filter-field-item">
              <label class="field-label-tiny">Từ năm</label>
              <div class="select-wrapper-minimal">
                <select v-model.number="compareFilter.startYear" class="form-control-minimal" @change="fetchComparisonData">
                  <option v-for="y in availableYears" :key="y" :value="y">{{ y }}</option>
                </select>
              </div>
            </div>
            <div class="filter-field-item">
              <label class="field-label-tiny">Đến năm</label>
              <div class="select-wrapper-minimal">
                <select v-model.number="compareFilter.endYear" class="form-control-minimal" @change="fetchComparisonData">
                  <option v-for="y in availableYears" :key="y" :value="y">{{ y }}</option>
                </select>
              </div>
            </div>
          </template>

          <!-- Nếu Tùy chọn 2 kỳ: Kỳ A và Kỳ B -->
          <template v-if="compareFilter.mode === 'custom'">
            <div class="filter-field-item">
              <label class="field-label-tiny">Kỳ A (Từ - Đến)</label>
              <div class="dual-date-inline">
                <input type="date" v-model="compareFilter.fromDateA" class="form-control-minimal small" @change="fetchComparisonData" />
                <span class="date-sep">-</span>
                <input type="date" v-model="compareFilter.toDateA" class="form-control-minimal small" @change="fetchComparisonData" />
              </div>
            </div>
            <div class="filter-field-item">
              <label class="field-label-tiny">Kỳ B (Từ - Đến)</label>
              <div class="dual-date-inline">
                <input type="date" v-model="compareFilter.fromDateB" class="form-control-minimal small" @change="fetchComparisonData" />
                <span class="date-sep">-</span>
                <input type="date" v-model="compareFilter.toDateB" class="form-control-minimal small" @change="fetchComparisonData" />
              </div>
            </div>
          </template>

          <!-- Lọc theo Thương hiệu -->
          <div class="filter-field-item">
            <label class="field-label-tiny">Thương hiệu</label>
            <div class="input-with-prefix-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
              </svg>
              <select v-model="compareFilter.brandId" class="form-control-minimal with-icon" @change="fetchComparisonData">
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
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                v-model="compareFilter.productKeyword"
                placeholder="Mã hoặc tên sản phẩm..."
                class="form-control-minimal with-icon"
                @input="debounceCompareFilter"
              />
            </div>
          </div>

          <!-- Nút Đặt lại -->
          <div class="filter-reset-action">
            <button type="button" class="btn-reset-round" @click="resetCompareFilters" title="Đặt lại bộ lọc so sánh">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M23 4v6h-6" />
                <path d="M1 20v-6h6" />
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
              </svg>
              <span>Đặt lại</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Thẻ Tổng quan kỳ so sánh (KPI Summary Bar) -->
      <div class="kpi-cards-grid">
        <div class="kpi-card">
          <div class="kpi-icon-wrap pastel-green">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
              <polyline points="17 6 23 6 23 12" />
            </svg>
          </div>
          <div class="kpi-value text-dark">{{ formatCurrency(comparisonData.summary?.totalRevenue) }}</div>
          <div class="kpi-label">Tổng doanh thu kỳ</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon-wrap pastel-blue">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <rect x="2" y="6" width="20" height="12" rx="2" />
              <circle cx="12" cy="12" r="2" />
            </svg>
          </div>
          <div class="kpi-value text-dark">{{ formatCurrency(comparisonData.summary?.cashRevenue) }}</div>
          <div class="kpi-label">Tiền mặt</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon-wrap pastel-emerald">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <rect x="2" y="4" width="20" height="16" rx="2" />
              <line x1="2" y1="10" x2="22" y2="10" />
            </svg>
          </div>
          <div class="kpi-value text-dark">{{ formatCurrency(comparisonData.summary?.transferRevenue) }}</div>
          <div class="kpi-label">Chuyển khoản</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon-wrap pastel-purple">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <circle cx="9" cy="21" r="1" />
              <circle cx="20" cy="21" r="1" />
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
            </svg>
          </div>
          <div class="kpi-value text-dark">{{ (comparisonData.summary?.totalOrders || 0).toLocaleString('vi-VN') }}</div>
          <div class="kpi-label">Tổng đơn hoàn thành</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon-wrap pastel-pink">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <rect x="4" y="2" width="16" height="20" rx="2" />
              <line x1="8" y1="6" x2="16" y2="6" />
            </svg>
          </div>
          <div class="kpi-value text-dark">{{ formatCurrency(comparisonData.summary?.aov) }}</div>
          <div class="kpi-label">AOV trung bình</div>
        </div>

        <div class="kpi-card">
          <div class="kpi-icon-wrap" :class="Number(comparisonData.summary?.avgGrowthRate || 0) >= 0 ? 'pastel-green' : 'pastel-orange'">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
              <polyline points="17 6 23 6 23 12" />
            </svg>
          </div>
          <div class="kpi-value" :class="Number(comparisonData.summary?.avgGrowthRate || 0) >= 0 ? 'text-green-amount' : 'text-red-return'">
            {{ Number(comparisonData.summary?.avgGrowthRate || 0) >= 0 ? '+' : '' }}{{ comparisonData.summary?.avgGrowthRate || 0 }}%
          </div>
          <div class="kpi-label">Tăng trưởng bình quân</div>
        </div>
      </div>

      <!-- Biểu đồ Combo Chart trực quan: Cột Doanh thu ghép + Đường Line Tăng trưởng % -->
      <!-- Tách biệt hoàn toàn nhãn Tăng trưởng (Badge bo tròn trên) và Doanh thu (dưới) - Không đè chữ -->
      <div class="content-card combo-chart-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="title-sub-icon">
              <path d="M3 3v18h18" />
              <path d="M18.7 8l-5.1 5.2-2.8-2.7L7 14.3" />
            </svg>
            <h3 class="card-heading">Biểu đồ Doanh thu & Tốc độ tăng trưởng qua các kỳ</h3>
          </div>
          <!-- Legend chú thích -->
          <div class="chart-combo-legend">
            <div class="legend-item"><span class="legend-box color-cash"></span> Tiền mặt</div>
            <div class="legend-item"><span class="legend-box color-transfer"></span> Chuyển khoản</div>
            <div class="legend-item"><span class="legend-line-dot"></span> Tốc độ tăng trưởng (%)</div>
          </div>
        </div>

        <!-- Khung vẽ SVG Combo Chart với tỷ lệ 1:1, không dùng preserveAspectRatio="none" -->
        <div class="combo-chart-svg-wrap">
          <svg class="combo-chart-svg" viewBox="0 0 1000 340">
            <!-- Grid Lines (Padding-top >= 50px) -->
            <line x1="50" y1="50" x2="950" y2="50" stroke="#f1ede7" stroke-dasharray="3 3" />
            <line x1="50" y1="105" x2="950" y2="105" stroke="#f1ede7" stroke-dasharray="3 3" />
            <line x1="50" y1="160" x2="950" y2="160" stroke="#f1ede7" stroke-dasharray="3 3" />
            <line x1="50" y1="215" x2="950" y2="215" stroke="#f1ede7" stroke-dasharray="3 3" />
            <line x1="50" y1="270" x2="950" y2="270" stroke="#cbd5e1" stroke-width="1.5" />

            <!-- Cột Doanh thu (Stacked Bar: Tiền mặt ở dưới, Chuyển khoản ở trên) -->
            <g v-for="(item, idx) in comboChartData.bars" :key="'bar-' + idx">
              <!-- Cột Tiền mặt -->
              <rect
                :x="item.x"
                :y="item.yCash"
                :width="item.width"
                :height="item.heightCash"
                fill="#3B82F6"
                opacity="0.85"
                rx="3"
                class="combo-bar-segment"
              >
                <title>{{ item.name }} - Tiền mặt: {{ formatCurrency(item.cash) }}</title>
              </rect>

              <!-- Cột Chuyển khoản (Xếp trên tiền mặt) -->
              <rect
                :x="item.x"
                :y="item.yTransfer"
                :width="item.width"
                :height="item.heightTransfer"
                fill="#10B981"
                opacity="0.9"
                rx="4"
                class="combo-bar-segment"
              >
                <title>{{ item.name }} - Chuyển khoản: {{ formatCurrency(item.transfer) }}</title>
              </rect>

              <!-- Con số Tổng doanh thu: Hiển thị ngay trên đỉnh cột, cách đỉnh 8px -->
              <text
                v-if="item.totalRevenue > 0"
                :x="item.x + item.width / 2"
                :y="item.yTransfer - 8"
                text-anchor="middle"
                class="combo-bar-total-label"
              >
                {{ formatShortCurrency(item.totalRevenue) }}
              </text>
              <text
                v-else
                :x="item.x + item.width / 2"
                :y="item.yTransfer - 6"
                text-anchor="middle"
                class="combo-bar-total-label zero"
              >
                0 đ
              </text>

              <!-- Nhãn kỳ tiếng Việt có dấu ở đáy trục X -->
              <text
                :x="item.x + item.width / 2"
                y="296"
                text-anchor="middle"
                class="combo-bottom-label"
              >
                {{ item.displayName }}
              </text>
            </g>

            <!-- Đường Line biểu diễn % Tăng trưởng ở vùng riêng biệt (Phía trên) -->
            <polyline
              v-if="comboChartData.linePoints"
              :points="comboChartData.linePoints"
              fill="none"
              stroke="#E11D48"
              stroke-width="2.6"
              stroke-linecap="round"
              stroke-linejoin="round"
            />

            <!-- Các điểm mốc tròn và Badge % Tăng trưởng bo tròn cách điểm tròn ít nhất 28px -->
            <g v-for="(pt, idx) in comboChartData.points" :key="'pt-' + idx" class="growth-point-group">
              <!-- Khung Badge Bo Tròn riêng biệt phía trên điểm tròn 28px -->
              <g :transform="`translate(${pt.x}, ${pt.y - 28})`">
                <rect
                  :x="-28"
                  :y="-11"
                  width="56"
                  height="22"
                  rx="11"
                  :fill="pt.growthRate > 0 ? '#dcfce7' : (pt.growthRate < 0 ? '#fee2e2' : '#f1f5f9')"
                  :stroke="pt.growthRate > 0 ? '#16a34a' : (pt.growthRate < 0 ? '#dc2626' : '#cbd5e1')"
                  stroke-width="1.2"
                />
                <text
                  x="0"
                  y="4.5"
                  text-anchor="middle"
                  font-size="11"
                  font-weight="700"
                  :fill="pt.growthRate > 0 ? '#15803d' : (pt.growthRate < 0 ? '#b91c1c' : '#475569')"
                  class="font-inter-badge"
                >
                  {{ pt.growthRate > 0 ? '↑ +' : (pt.growthRate < 0 ? '↓ ' : '') }}{{ pt.growthRate }}%
                </text>
              </g>

              <!-- Điểm tròn đỏ -->
              <circle
                :cx="pt.x"
                :cy="pt.y"
                r="5"
                fill="#ffffff"
                stroke="#E11D48"
                stroke-width="2.5"
              />
            </g>
          </svg>
        </div>
      </div>

      <!-- Bảng đối chiếu doanh thu chi tiết các kỳ -->
      <div class="content-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="title-sub-icon">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
            </svg>
            <h3 class="card-heading">Bảng đối chiếu doanh thu chi tiết các kỳ</h3>
          </div>
          <span class="badge-unit">Đơn vị: VNĐ</span>
        </div>

        <div class="table-responsive-clean">
          <table class="minimal-styled-table">
            <thead>
              <tr class="header-brown-beige">
                <th style="width: 140px">KỲ SO SÁNH</th>
                <th style="min-width: 160px">KHOẢNG THỜI GIAN</th>
                <th style="width: 160px; text-align: right">TỔNG DOANH THU</th>
                <th style="width: 140px; text-align: right">TIỀN MẶT</th>
                <th style="width: 140px; text-align: right">CHUYỂN KHOẢN</th>
                <th style="width: 90px; text-align: center">SỐ ĐƠN</th>
                <th style="width: 130px; text-align: right">AOV</th>
                <th style="width: 110px; text-align: center">ĐÃ BÁN</th>
                <th style="width: 150px; text-align: right">CHÊNH LỆCH</th>
                <th style="width: 130px; text-align: center">TĂNG TRƯỞNG</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!comparisonData.periods || comparisonData.periods.length === 0">
                <td colspan="10" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                  Đang tải hoặc chưa có dữ liệu đối chiếu trong kỳ này.
                </td>
              </tr>
              <tr v-for="p in comparisonData.periods" :key="p.periodKey" class="table-data-row">
                <td class="font-bold text-dark">
                  <span>{{ p.periodName }}</span>
                  <span v-if="p.highest" class="badge-tag-peak highest">Cao nhất</span>
                  <span v-else-if="p.lowest" class="badge-tag-peak lowest">Thấp nhất</span>
                </td>
                <td class="text-muted-stt">{{ p.dateRangeText }}</td>
                <td style="text-align: right" class="font-bold text-black-bold">
                  {{ formatCurrency(p.totalRevenue) }}
                </td>
                <td style="text-align: right" class="text-blue-amount font-semibold">
                  {{ formatCurrency(p.cashRevenue) }}
                </td>
                <td style="text-align: right" class="text-green-amount font-semibold">
                  {{ formatCurrency(p.transferRevenue) }}
                </td>
                <td style="text-align: center" class="font-bold text-dark">
                  {{ p.orderCount }}
                </td>
                <td style="text-align: right" class="text-gray-aov">
                  {{ formatCurrency(p.aov) }}
                </td>
                <td style="text-align: center" class="font-bold text-dark">
                  {{ p.itemsSold }}
                </td>
                <td style="text-align: right" :class="Number(p.diffAmount) >= 0 ? 'text-green-amount' : 'text-red-return'">
                  {{ Number(p.diffAmount) >= 0 ? '+' : '' }}{{ formatCurrency(p.diffAmount) }}
                </td>
                <td style="text-align: center">
                  <span
                    class="badge-growth-pill"
                    :class="Number(p.growthRate) > 0 ? 'growth-positive' : Number(p.growthRate) < 0 ? 'growth-negative' : 'growth-neutral'"
                  >
                    {{ Number(p.growthRate) > 0 ? '↑ +' : (Number(p.growthRate) < 0 ? '↓ ' : '') }}{{ p.growthRate }}%
                  </span>
                </td>
              </tr>
            </tbody>
            <!-- Chân bảng: Tổng cộng -->
            <tfoot v-if="comparisonData.periods && comparisonData.periods.length > 0">
              <tr class="table-total-row">
                <td colspan="2" class="font-bold text-dark">TỔNG CỘNG</td>
                <td style="text-align: right" class="font-bold text-black-bold">
                  {{ formatCurrency(comparisonData.summary?.totalRevenue) }}
                </td>
                <td style="text-align: right" class="font-bold text-blue-amount">
                  {{ formatCurrency(comparisonData.summary?.cashRevenue) }}
                </td>
                <td style="text-align: right" class="font-bold text-green-amount">
                  {{ formatCurrency(comparisonData.summary?.transferRevenue) }}
                </td>
                <td style="text-align: center" class="font-bold">
                  {{ comparisonData.summary?.totalOrders }}
                </td>
                <td style="text-align: right" class="font-bold">
                  {{ formatCurrency(comparisonData.summary?.aov) }}
                </td>
                <td style="text-align: center" class="font-bold">
                  {{ comparisonData.summary?.totalItemsSold }}
                </td>
                <td colspan="2" style="text-align: center" class="font-bold text-green-amount">
                  Tăng trưởng TB: {{ comparisonData.summary?.avgGrowthRate }}%
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <!-- Bảng Top sản phẩm bán chạy đối chiếu sức mua qua các kỳ -->
      <div class="content-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="title-sub-icon">
              <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <path d="M16 10a4 4 0 0 1-8 0" />
            </svg>
            <h3 class="card-heading">Top sản phẩm bán chạy & Đối chiếu sức mua qua các kỳ</h3>
          </div>
          <span class="badge-unit">Top 10 mặt hàng</span>
        </div>

        <div class="table-responsive-clean">
          <table class="minimal-styled-table">
            <thead>
              <tr class="header-brown-beige">
                <th style="width: 50px; text-align: center">STT</th>
                <th style="width: 120px">MÃ SP</th>
                <th style="min-width: 220px">TÊN SẢN PHẨM</th>
                <th style="width: 130px">THƯƠNG HIỆU</th>
                <th style="width: 100px; text-align: center">TỔNG BÁN</th>
                <th style="width: 140px; text-align: right">TỔNG DOANH THU</th>
                <!-- Cột động theo từng kỳ -->
                <th
                  v-for="p in comparisonData.periods"
                  :key="'th-' + p.periodKey"
                  style="min-width: 90px; text-align: center"
                >
                  {{ p.periodName }}
                </th>
                <th style="width: 140px; text-align: center">XU HƯỚNG</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!comparisonData.topProducts || comparisonData.topProducts.length === 0">
                <td :colspan="7 + (comparisonData.periods?.length || 0)" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                  Chưa có dữ liệu sản phẩm đối chiếu trong giai đoạn này.
                </td>
              </tr>
              <tr v-for="(sp, idx) in comparisonData.topProducts" :key="sp.productId" class="table-data-row">
                <td style="text-align: center" class="text-muted-stt">{{ idx + 1 }}</td>
                <td class="font-bold text-dark code-cell">{{ sp.productCode }}</td>
                <td>
                  <div class="product-name-with-thumb">
                    <img :src="sp.imageUrl || defaultProductImage" alt="thumb" class="product-thumb-img" />
                    <span class="product-title-bold">{{ sp.productName }}</span>
                  </div>
                </td>
                <td><span class="brand-badge-tag">{{ sp.brandName }}</span></td>
                <td style="text-align: center" class="font-bold text-dark">{{ sp.totalSold }}</td>
                <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(sp.totalRevenue) }}</td>

                <!-- Số lượng bán từng kỳ -->
                <td
                  v-for="p in comparisonData.periods"
                  :key="'td-' + sp.productId + '-' + p.periodKey"
                  style="text-align: center"
                  class="period-qty-cell"
                >
                  <strong :class="(sp.periodSoldMap?.[p.periodKey] || 0) > 0 ? 'text-dark' : 'text-muted-stt'">
                    {{ sp.periodSoldMap?.[p.periodKey] || 0 }}
                  </strong>
                </td>

                <!-- Xu hướng tăng / giảm -->
                <td style="text-align: center">
                  <span
                    class="badge-trend-indicator"
                    :class="sp.trend === 'up' ? 'trend-up' : sp.trend === 'down' ? 'trend-down' : 'trend-flat'"
                  >
                    <span v-if="sp.trend === 'up'">↑ Tăng (+{{ sp.growthTrend }}%)</span>
                    <span v-else-if="sp.trend === 'down'">↓ Giảm ({{ sp.growthTrend }}%)</span>
                    <span v-else>Ổn định</span>
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ================================================================= -->
    <!-- MODAL (HỘP THOẠI) CẤU HÌNH & GỬI MAIL BÁO CÁO TỔNG KẾT             -->
    <!-- ================================================================= -->
    <div v-if="showEmailModal" class="modal-backdrop-custom" @click.self="showEmailModal = false">
      <div class="modal-card-custom">
        <!-- Modal Header -->
        <div class="modal-header-custom">
          <div class="modal-title-with-icon">
            <div class="modal-icon-badge">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
            </div>
            <div>
              <h3 class="modal-main-title">Gửi Mail Tổng Kết Doanh Thu</h3>
              <p class="modal-sub-title">Tổng hợp số liệu kinh doanh & so sánh trực tiếp gửi về hộp thư Quản trị viên</p>
            </div>
          </div>
          <button type="button" class="btn-close-modal" @click="showEmailModal = false" title="Đóng">✕</button>
        </div>

        <!-- Modal Body -->
        <div class="modal-body-custom">
          <!-- 1. Loại báo cáo tổng kết -->
          <div class="form-group-custom">
            <label class="form-label-custom">1. Loại báo cáo tổng kết</label>
            <div class="radio-pill-group">
              <button
                type="button"
                class="radio-pill-btn"
                :class="{ active: emailForm.reportType === 'month' }"
                @click="emailForm.reportType = 'month'"
              >
                Báo cáo tổng kết Tháng
              </button>
              <button
                type="button"
                class="radio-pill-btn"
                :class="{ active: emailForm.reportType === 'year' }"
                @click="emailForm.reportType = 'year'"
              >
                Báo cáo tổng kết Năm
              </button>
            </div>
          </div>

          <!-- Chọn Thời gian (Tháng / Năm) -->
          <div class="form-row-2col">
            <div v-if="emailForm.reportType === 'month'" class="form-group-custom">
              <label class="form-label-custom">Chọn Tháng</label>
              <div class="select-wrapper-minimal">
                <select v-model.number="emailForm.month" class="form-control-minimal">
                  <option v-for="m in 12" :key="m" :value="m">Tháng {{ m }}</option>
                </select>
              </div>
            </div>

            <div class="form-group-custom">
              <label class="form-label-custom">Chọn Năm</label>
              <div class="select-wrapper-minimal">
                <select v-model.number="emailForm.year" class="form-control-minimal">
                  <option v-for="y in availableYears" :key="y" :value="y">Năm {{ y }}</option>
                </select>
              </div>
            </div>
          </div>

          <!-- 2. Email Admin nhận báo cáo -->
          <div class="form-group-custom">
            <label class="form-label-custom">2. Email Admin nhận báo cáo</label>
            <input
              type="email"
              v-model="emailForm.recipientEmail"
              placeholder="Nhập email Admin nhận báo cáo (vd: admin@gmail.com)"
              class="form-control-minimal"
            />
            <span class="field-hint-text">
              Mặc định tự động điền email Admin hệ thống. Bạn có thể thay đổi để kiểm thử gửi vào email bất kỳ.
            </span>
          </div>

          <!-- 3. Trạng thái tự động -->
          <div class="email-schedule-status-box">
            <div class="schedule-status-header">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="schedule-icon">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              <strong>Trạng thái tự động của hệ thống (@Scheduled):</strong>
            </div>
            <p class="schedule-status-text">
              Hệ thống tự động gửi báo cáo vào <strong>23:59 ngày cuối mỗi tháng</strong> và <strong>31/12 cuối năm</strong> về hộp thư Quản trị viên.
            </p>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="modal-footer-custom">
          <button type="button" class="btn-cancel-custom" @click="showEmailModal = false">
            Hủy bỏ
          </button>
          <button
            type="button"
            class="btn-send-custom"
            :disabled="sendingEmail"
            @click="handleSendEmail"
          >
            <span v-if="sendingEmail" class="spinner-small"></span>
            <span v-else>Gửi báo cáo ngay</span>
          </button>
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

// 0. Quản lý Chế độ xem: 'overview' (Thống kê chính) hoặc 'comparison' (So sánh & Tăng trưởng)
const viewMode = ref('overview')

function switchViewMode(mode) {
  viewMode.value = mode
  if (mode === 'comparison' && (!comparisonData.value.periods || comparisonData.value.periods.length === 0)) {
    fetchComparisonData()
  }
}

// 1. Quản lý Bộ lọc thống kê (Overview)
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
// ViewBox chuẩn, tự động tính toán width và cuộn ngang khi > 12 cột, xoay nghiêng -45deg chống đè chữ
const rawDailySales = ref([])
const hoveredBar = ref(null)
const tooltipPos = reactive({ x: 0, y: 0 })

function formatTooltipDate(item) {
  if (!item) return ''
  const lbl = item.label || item.date || ''
  if (filters.statType === 'day') {
    const yr = filters.fromDate ? filters.fromDate.substring(0, 4) : new Date().getFullYear()
    return `Ngày ${lbl}/${yr}`
  }
  if (filters.statType === 'month') {
    return lbl.startsWith('T') ? `Tháng ${lbl.substring(1)}` : `Tháng ${lbl}`
  }
  if (filters.statType === 'year') {
    return `Năm ${lbl}`
  }
  return lbl
}

function onBarHover(bar, event) {
  hoveredBar.value = bar
  const container = document.querySelector('.bar-chart-container')
  if (!container) return
  const rect = container.getBoundingClientRect()
  const clientX = event.clientX - rect.left
  const clientY = event.clientY - rect.top
  // Giới hạn tooltip không bị tràn lề trái/phải của container
  tooltipPos.x = Math.max(105, Math.min(rect.width - 105, clientX))
  tooltipPos.y = Math.max(75, clientY)
}

function onBarMouseLeave() {
  hoveredBar.value = null
}

const barChartCount = computed(() => rawDailySales.value?.length || 0)

const barChartSvgWidth = computed(() => {
  const count = barChartCount.value
  if (count <= 12) return 1000
  const colSlot = 56 // min-width 56px mỗi cột
  const paddingX = 60
  return Math.max(1000, paddingX * 2 + count * colSlot)
})

const barChartSvgWidthStyle = computed(() => {
  const count = barChartCount.value
  if (count <= 12) return '100%'
  return `${barChartSvgWidth.value}px`
})

const barChartComputedData = computed(() => {
  const data = rawDailySales.value
  if (!data || data.length === 0) return []

  const maxVal = Math.max(...data.map(d => Number(d.value || 0)), 10)
  const chartHeight = 150
  const baseY = 240
  const count = data.length
  const isScroll = count > 12
  const isRotated = count > 8 // Tự động xoay nghiêng nhãn khi trên 8 mốc thời gian

  const paddingX = 60

  if (!isScroll) {
    const totalWidth = 880
    const barWidth = Math.min(46, Math.max(26, Math.floor((totalWidth / count) * 0.52)))
    const step = count > 1 ? (totalWidth - barWidth) / (count - 1) : 0
    return data.map((item, idx) => {
      const val = Number(item.value || 0)
      const h = Math.max((val / maxVal) * chartHeight, val > 0 ? 8 : 2)
      const x = count === 1 ? (paddingX + totalWidth / 2 - barWidth / 2) : (paddingX + idx * step)
      const centerX = x + barWidth / 2
      return {
        ...item,
        x,
        y: baseY - h,
        width: barWidth,
        height: h,
        centerX,
        stepGap: 10,
        isRotated,
        showLabel: true,
        dateTitle: formatTooltipDate(item)
      }
    })
  }

  // Chế độ nhiều mốc thời gian (> 12 mốc):
  // Mỗi cột có không gian tối thiểu 56px (barWidth 32px + gap 24px)
  const colSlot = 56
  const barWidth = 32
  return data.map((item, idx) => {
    const val = Number(item.value || 0)
    const h = Math.max((val / maxVal) * chartHeight, val > 0 ? 8 : 2)
    const x = paddingX + idx * colSlot + (colSlot - barWidth) / 2
    const centerX = x + barWidth / 2

    // Tự động giãn cách nhãn nếu danh sách quá dài (> 35 mốc)
    const showLabel = count <= 35 || (idx % 2 === 0) || (idx === count - 1)

    return {
      ...item,
      x,
      y: baseY - h,
      width: barWidth,
      height: h,
      centerX,
      stepGap: (colSlot - barWidth) / 2,
      isRotated: true,
      showLabel,
      dateTitle: formatTooltipDate(item)
    }
  })
})

// 4. Biểu đồ Donut Thương hiệu
const brandMetric = ref('revenue')
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
  const circumference = 2 * Math.PI * 70

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

// 5. Biểu đồ Donut Trạng thái đơn hàng
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

// 6. Bảng Thống kê sản phẩm (8 cột, tìm kiếm, sắp xếp, phân trang)
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

// 7. Định dạng tiền tệ
function formatCurrency(val) {
  if (val == null || isNaN(val)) return '0 đ'
  return Math.round(Number(val)).toLocaleString('vi-VN') + ' đ'
}

function formatShortCurrency(val) {
  const num = Number(val || 0)
  if (num >= 1000000000) return (num / 1000000000).toFixed(1).replace('.0', '') + ' tỷ'
  if (num >= 1000000) return (num / 1000000).toFixed(1).replace('.0', '') + ' tr'
  if (num >= 1000) return (num / 1000).toFixed(0) + ' k'
  return num.toLocaleString('vi-VN') + ' đ'
}

// 8. Tải dữ liệu Overview từ Backend API
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

// 9. Xuất Excel Overview
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
      ['Sản phẩm đã bán', kpiData.totalItemsSold]
    ]

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

// =========================================================================
// PHẦN 10: LOGIC CHO MÀN HÌNH SO SÁNH & TĂNG TRƯỞNG DOANH THU (COMPARISON)
// =========================================================================
const currentYear = new Date().getFullYear()
const availableYears = [currentYear - 2, currentYear - 1, currentYear, currentYear + 1]

const compareFilter = reactive({
  mode: 'month', // 'month', 'quarter', 'year', 'custom'
  year: currentYear,
  startYear: currentYear - 3,
  endYear: currentYear,
  brandId: '',
  productKeyword: '',
  fromDateA: `${currentYear}-01-01`,
  toDateA: `${currentYear}-06-30`,
  fromDateB: `${currentYear}-07-01`,
  toDateB: `${currentYear}-12-31`
})

const loadingComparison = ref(false)
const comparisonData = ref({
  summary: {
    totalRevenue: 0,
    cashRevenue: 0,
    transferRevenue: 0,
    totalOrders: 0,
    aov: 0,
    totalItemsSold: 0,
    avgGrowthRate: 0
  },
  periods: [],
  topProducts: []
})

function setCompareMode(mode) {
  compareFilter.mode = mode
  fetchComparisonData()
}

let compareDebounceTimer = null
function debounceCompareFilter() {
  clearTimeout(compareDebounceTimer)
  compareDebounceTimer = setTimeout(() => {
    fetchComparisonData()
  }, 300)
}

function resetCompareFilters() {
  compareFilter.mode = 'month'
  compareFilter.year = currentYear
  compareFilter.startYear = currentYear - 3
  compareFilter.endYear = currentYear
  compareFilter.brandId = ''
  compareFilter.productKeyword = ''
  fetchComparisonData()
  showToast('Đã đặt lại bộ lọc so sánh!', 'info')
}

async function fetchComparisonData() {
  loadingComparison.value = true
  try {
    const params = {
      compareMode: compareFilter.mode,
      year: compareFilter.year,
      startYear: compareFilter.startYear,
      endYear: compareFilter.endYear
    }
    if (compareFilter.brandId) params.brandId = compareFilter.brandId
    if (compareFilter.productKeyword && compareFilter.productKeyword.trim()) {
      params.productKeyword = compareFilter.productKeyword.trim()
    }
    if (compareFilter.mode === 'custom') {
      params.fromDateA = compareFilter.fromDateA
      params.toDateA = compareFilter.toDateA
      params.fromDateB = compareFilter.fromDateB
      params.toDateB = compareFilter.toDateB
    }

    const res = await api.get('/api/thong-ke/so-sanh', { params })
    if (res.data) {
      comparisonData.value = res.data
    }
  } catch (err) {
    console.error('Lỗi tải dữ liệu so sánh doanh thu:', err)
    showToast('Không thể tải dữ liệu so sánh doanh thu.', 'error')
  } finally {
    loadingComparison.value = false
  }
}

// Tính toán dữ liệu cho Combo Chart SVG:
// - Viewbox 1000x340 chuẩn, KHÔNG dùng preserveAspectRatio="none"
// - Cột Doanh thu ở vùng dưới (Y: 150..270), nhãn Doanh thu tại đỉnh cột
// - Đường Line & Badge Tăng trưởng % ở vùng trên (Y: 45..95), badge cách điểm tròn 28px
// - Nhãn trục hoành hiển thị tiếng Việt có dấu rõ ràng
const comboChartData = computed(() => {
  const periods = comparisonData.value.periods || []
  if (periods.length === 0) {
    return { bars: [], points: [], linePoints: '' }
  }

  const maxRev = Math.max(...periods.map(p => Number(p.totalRevenue || 0)), 1000000)
  const chartHeight = 110 // Giới hạn chiều cao cột để không bao giờ chạm vùng Line phía trên
  const baseY = 270
  const totalWidth = 840
  const startX = 75
  const count = periods.length
  const barWidth = Math.min(50, Math.max(18, Math.floor((totalWidth / count) * 0.52)))
  const step = count > 1 ? (totalWidth - barWidth) / (count - 1) : 0

  const bars = []
  const points = []

  periods.forEach((p, idx) => {
    const total = Number(p.totalRevenue || 0)
    const cash = Number(p.cashRevenue || 0)
    const transfer = Number(p.transferRevenue || 0)

    const totalH = Math.max((total / maxRev) * chartHeight, total > 0 ? 8 : 2)
    const cashH = total > 0 ? (cash / total) * totalH : 0
    const transferH = total > 0 ? totalH - cashH : 0

    const x = count === 1 ? (startX + totalWidth / 2 - barWidth / 2) : (startX + idx * step)
    const yTransfer = baseY - totalH
    const yCash = baseY - cashH

    // Định dạng tên tiếng Việt có dấu cho trục X
    let displayName = p.periodName || p.periodKey
    if (compareFilter.mode === 'month') {
      const mNum = parseInt(p.periodKey.replace(/\D/g, ''), 10)
      if (!isNaN(mNum)) displayName = `Tháng ${mNum}`
    } else if (compareFilter.mode === 'quarter') {
      const qNum = parseInt(p.periodKey.replace(/\D/g, ''), 10)
      if (!isNaN(qNum)) displayName = `Quý ${qNum}`
    } else if (compareFilter.mode === 'year') {
      displayName = `Năm ${p.periodKey}`
    } else if (compareFilter.mode === 'custom') {
      if (p.periodKey === 'KyA' || idx === 0) displayName = 'Kỳ A (Kỳ trước)'
      else if (p.periodKey === 'KyB' || idx === 1) displayName = 'Kỳ B (Kỳ này)'
    }

    bars.push({
      name: p.periodKey,
      displayName,
      totalRevenue: total,
      cash,
      transfer,
      x,
      width: barWidth,
      yTransfer,
      heightTransfer: transferH,
      yCash,
      heightCash: cashH
    })

    // Điểm nút biểu diễn tốc độ tăng trưởng %:
    // Đặt trong vùng Y: 45px đến 95px (cách hoàn toàn cột doanh thu)
    const gRate = Number(p.growthRate || 0)
    const clampedG = Math.max(-50, Math.min(100, gRate))
    // Map -50%..+100% sang Y: 95px..45px
    const yLine = 75 - (clampedG / 100) * 30

    points.push({
      x: x + barWidth / 2,
      y: yLine,
      growthRate: gRate
    })
  })

  const linePoints = points.map(pt => `${pt.x},${pt.y}`).join(' ')

  return { bars, points, linePoints }
})

// Xuất file Excel So sánh
async function handleExportComparisonExcel() {
  try {
    const params = {
      compareMode: compareFilter.mode,
      year: compareFilter.year,
      startYear: compareFilter.startYear,
      endYear: compareFilter.endYear
    }
    if (compareFilter.brandId) params.brandId = compareFilter.brandId
    if (compareFilter.productKeyword && compareFilter.productKeyword.trim()) {
      params.productKeyword = compareFilter.productKeyword.trim()
    }
    if (compareFilter.mode === 'custom') {
      params.fromDateA = compareFilter.fromDateA
      params.toDateA = compareFilter.toDateA
      params.fromDateB = compareFilter.fromDateB
      params.toDateB = compareFilter.toDateB
    }

    const response = await api.get('/api/thong-ke/export-so-sanh', {
      params,
      responseType: 'blob'
    })

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const fileName = `BaoCao_SoSanh_DoanhThu_${compareFilter.mode}_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    showToast('Xuất báo cáo So sánh doanh thu thành công!', 'success')
  } catch (err) {
    console.error('Lỗi xuất Excel so sánh:', err)
    showToast('Lỗi khi xuất file Excel so sánh doanh thu.', 'error')
  }
}

// =========================================================================
// PHẦN 11: MODAL GỬI MAIL BÁO CÁO CHO ADMIN
// =========================================================================
const showEmailModal = ref(false)
const sendingEmail = ref(false)

const emailForm = reactive({
  reportType: 'month',
  year: currentYear,
  month: new Date().getMonth() + 1,
  recipientEmail: 'thuanminh2908200334.1@gmail.com'
})

function openEmailModal() {
  emailForm.reportType = 'month'
  emailForm.year = currentYear
  emailForm.month = new Date().getMonth() + 1
  if (!emailForm.recipientEmail) {
    emailForm.recipientEmail = 'thuanminh2908200334.1@gmail.com'
  }
  showEmailModal.value = true
}

async function handleSendEmail() {
  sendingEmail.value = true
  try {
    const params = {
      reportType: emailForm.reportType.toUpperCase(),
      year: emailForm.year,
      month: emailForm.reportType === 'month' ? emailForm.month : undefined
    }
    if (emailForm.recipientEmail && emailForm.recipientEmail.trim()) {
      params.recipientEmail = emailForm.recipientEmail.trim()
    }

    const res = await api.post('/api/thong-ke/send-report-email', null, { params })
    if (res.data && res.data.success) {
      showToast(res.data.message || 'Đã gửi email báo cáo tổng kết thành công về hộp thư Admin!', 'success')
      showEmailModal.value = false
    } else {
      showToast(res.data?.message || 'Không thể gửi email báo cáo.', 'warning')
    }
  } catch (err) {
    console.error('Lỗi gửi email báo cáo:', err)
    showToast(err?.response?.data?.message || 'Lỗi khi kết nối gửi email báo cáo.', 'error')
  } finally {
    sendingEmail.value = false
  }
}

// Tải thương hiệu
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
  font-family: 'Inter', Roboto, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Card chuẩn thiết kế */
.content-card {
  background-color: #ffffff;
  border-radius: 16px;
  border: 1px solid #ece6dc;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.03);
  padding: 1.25rem 1.4rem;
  box-sizing: border-box;
}

/* Tiêu đề trang */
.page-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon-box {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background-color: #f1ede7;
  color: #8c7355;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(140, 115, 85, 0.15);
}

.btn-back-square {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background-color: #ffffff;
  border: 1px solid #d5cbc0;
  color: #4a3427;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back-square:hover {
  background-color: #f5efe7;
  border-color: #8c7355;
}

.page-main-title {
  margin: 0;
  font-size: 1.55rem;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: -0.2px;
}

.page-sub-title {
  margin: 2px 0 0;
  font-size: 0.84rem;
  color: #7b8a92;
}

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* Cụm nút pill */
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pill-btn:hover {
  background-color: #f0ebe1;
  border-color: #d1c6b5;
  color: #3b4d55;
}

.pill-btn.active {
  background-color: #d8cbb8;
  border-color: #bfae9d;
  color: #3b2e21;
}

/* Nút Gửi mail nổi bật (Header) */
.btn-send-mail-highlight {
  background-color: #ecfdf5;
  border-color: #86efac;
  color: #15803d;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(22, 163, 74, 0.15);
}

.btn-send-mail-highlight:hover {
  background-color: #dcfce7;
  border-color: #4ade80;
  color: #166534;
}

/* Nút Gửi mail trong cụm pill bộ lọc */
.btn-send-mail-action {
  background-color: #f0fdf4;
  border-color: #bbf7d0;
  color: #15803d;
  font-weight: 700;
}

.btn-send-mail-action:hover {
  background-color: #dcfce7;
  border-color: #86efac;
}

/* Nút So sánh nổi bật */
.btn-compare-highlight {
  background-color: #8c7355;
  border-color: #8c7355;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(140, 115, 85, 0.25);
}

.btn-compare-highlight:hover {
  background-color: #786044;
  border-color: #786044;
  color: #ffffff;
}

/* Nút So sánh trong cụm pill */
.btn-compare-pill {
  background-color: #f5eee6;
  border-color: #cfbeab;
  color: #715539;
  font-weight: 700;
}

.btn-compare-pill:hover {
  background-color: #ebdccb;
  color: #4a3427;
}

/* Nút Xuất Excel */
.btn-export-excel {
  background-color: #ffffff;
  border-color: #16a34a;
  color: #16a34a;
}

.btn-export-excel:hover {
  background-color: #16a34a;
  color: #ffffff;
}

/* Card Bộ lọc */
.filter-card-box {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

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

/* Hàng trường lọc */
.filter-fields-row {
  display: grid;
  grid-template-columns: 140px 140px 150px 200px 1fr auto;
  gap: 12px;
  align-items: flex-end;
}

.comparison-fields-row {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)) auto;
}

.filter-field-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label-tiny {
  font-size: 0.76rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.form-control-minimal {
  width: 100%;
  height: 38px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  font-size: 0.86rem;
  color: #2c3e50;
  outline: none;
  box-sizing: border-box;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-control-minimal:focus {
  border-color: #8c7355;
  box-shadow: 0 0 0 3px rgba(140, 115, 85, 0.12);
}

.form-control-minimal.small {
  height: 34px;
  font-size: 0.82rem;
}

.input-with-prefix-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 10px;
  color: #94a3b8;
  pointer-events: none;
}

.form-control-minimal.with-icon {
  padding-left: 32px;
}

.select-wrapper-minimal {
  position: relative;
}

.dual-date-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}

.date-sep {
  color: #94a3b8;
  font-weight: bold;
}

/* Nút đặt lại */
.filter-reset-action {
  display: flex;
  align-items: center;
}

.btn-reset-round {
  height: 38px;
  padding: 0 14px;
  background: #fbf9f6;
  border: 1px solid #d5cbc0;
  border-radius: 8px;
  color: #556268;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-reset-round:hover {
  background: #f1ebd8;
  color: #3b2f23;
  border-color: #bfae9d;
}

.filter-error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #dc2626;
  font-size: 0.82rem;
  font-weight: 600;
}

.filter-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.84rem;
  color: #556268;
  padding-top: 4px;
}

.blue-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3b82f6;
}

/* 6 KPI Cards */
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

/* Full Width Bar Chart - Không bẹp chữ, chống đè chữ, cuộn ngang mượt mà & Tooltip */
.full-chart-card {
  width: 100%;
}

.bar-chart-container {
  position: relative;
  width: 100%;
  padding-top: 0.5rem;
}

.chart-scroll-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.76rem;
  font-weight: 600;
  color: #8c7355;
  background: #fbf9f6;
  border: 1px dashed #d5cbc0;
  padding: 4px 10px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.bar-chart-scroll-wrap {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #c8b6a6 #f1ede7;
  border-radius: 8px;
  padding-bottom: 6px;
}

.bar-chart-scroll-wrap::-webkit-scrollbar {
  height: 6px;
}

.bar-chart-scroll-wrap::-webkit-scrollbar-track {
  background: #f1ede7;
  border-radius: 4px;
}

.bar-chart-scroll-wrap::-webkit-scrollbar-thumb {
  background: #c8b6a6;
  border-radius: 4px;
}

.bar-chart-scroll-wrap::-webkit-scrollbar-thumb:hover {
  background: #8c7355;
}

.bar-chart-svg-wrap {
  height: 320px;
}

.bar-chart-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.chart-bar-group {
  cursor: pointer;
}

.bar-hit-area {
  cursor: pointer;
}

.bar-hover-guide {
  pointer-events: none;
  transition: opacity 0.15s ease;
}

.chart-bar-rect {
  transition: fill 0.2s ease, opacity 0.2s ease, filter 0.2s ease;
  cursor: pointer;
}

.chart-bar-rect.is-hovered {
  fill: #8c7355 !important;
  filter: drop-shadow(0 2px 6px rgba(140, 115, 85, 0.45));
}

.bar-peak-number {
  font-family: 'Inter', Roboto, sans-serif;
  user-select: none;
  pointer-events: none;
}

.bar-peak-number.positive {
  font-size: 12px;
  font-weight: 700;
  fill: #dc2626;
}

.bar-peak-number.zero {
  font-size: 10px;
  font-weight: 500;
  fill: #9ca3af;
  opacity: 0.65;
}

.bar-bottom-label {
  font-size: 12px;
  font-weight: 500;
  fill: #475569;
  font-family: 'Inter', Roboto, sans-serif;
  white-space: nowrap;
  user-select: none;
  pointer-events: none;
  transition: fill 0.15s, font-weight 0.15s;
}

.bar-bottom-label.rotated {
  font-size: 11.5px;
  fill: #64748b;
}

.bar-bottom-label.label-highlight {
  fill: #0f172a !important;
  font-weight: 700 !important;
}

/* Tooltip nổi tương tác */
.bar-chart-floating-tooltip {
  position: absolute;
  pointer-events: none;
  z-index: 100;
  background: rgba(15, 23, 42, 0.94);
  backdrop-filter: blur(8px);
  color: #ffffff;
  border-radius: 10px;
  padding: 10px 14px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.28), 0 4px 10px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.15);
  font-family: 'Inter', Roboto, sans-serif;
  min-width: 185px;
  transform: translate(-50%, -115%);
  transition: transform 0.05s ease-out, opacity 0.15s ease;
}

.tooltip-header-clean {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 0.84rem;
  color: #f8fafc;
}

.tooltip-cal-badge {
  font-size: 0.95rem;
}

.tooltip-date-txt {
  font-weight: 700;
  color: #ffffff;
}

.tooltip-divider-line {
  height: 1px;
  background: rgba(255, 255, 255, 0.12);
  margin: 6px 0 8px;
}

.tooltip-data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 0.78rem;
  margin-top: 3px;
}

.tooltip-row-lbl {
  color: #94a3b8;
}

.tooltip-row-val {
  font-weight: 700;
}

.text-amber {
  color: #f59e0b;
}

.text-emerald {
  color: #34d399;
}

/* 2 Donut Cards */
.two-donuts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.donut-card-item {
  display: flex;
  flex-direction: column;
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
.badge-total-orders {
  font-size: 0.74rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #f2ede7;
  color: #76695b;
}

.badge-filter-status {
  font-size: 0.74rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #e0f2fe;
  color: #0369a1;
}

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

/* Bảng dữ liệu tối giản */
.table-responsive-clean {
  overflow-x: auto;
}

.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.minimal-styled-table th {
  background-color: #ddd3c5 !important;
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

.table-total-row td {
  background-color: #faf5ed;
  font-weight: 700;
  border-top: 2px solid #ddd3c5;
  padding: 12px 14px;
}

.text-black-bold {
  color: #0f172a;
  font-weight: 700;
}

.text-blue-amount {
  color: #2563eb;
}

.text-green-amount {
  color: #16a34a;
}

.text-red-return {
  color: #dc2626;
}

.text-green-stock {
  color: #16a34a;
}

.text-gray-aov {
  color: #64748b;
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

.growth-neutral {
  background-color: #f1f5f9;
  color: #64748b;
}

.badge-tag-peak {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  margin-left: 6px;
}

.badge-tag-peak.highest {
  background-color: #dcfce7;
  color: #15803d;
}

.badge-tag-peak.lowest {
  background-color: #ffedd5;
  color: #c2410c;
}

/* Bảng Thống kê sản phẩm */
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

.text-muted-stt {
  color: #94a3b8;
}

.code-cell {
  letter-spacing: 0.2px;
}

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
  background-color: #8c7355 !important;
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

/* =========================================================================
   STYLE CHO MÀN HÌNH SO SÁNH (COMPARISON)
   ========================================================================= */
.chart-combo-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 0.82rem;
  color: #556268;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-box {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.legend-box.color-cash { background-color: #3b82f6; }
.legend-box.color-transfer { background-color: #10b981; }

.legend-line-dot {
  width: 16px;
  height: 3px;
  background-color: #e11d48;
  position: relative;
  display: inline-block;
}

.legend-line-dot::after {
  content: '';
  position: absolute;
  top: -2.5px;
  left: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ffffff;
  border: 2px solid #e11d48;
}

.combo-chart-svg-wrap {
  width: 100%;
  height: 340px;
  margin-top: 0.5rem;
}

.combo-chart-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.combo-bar-segment {
  transition: opacity 0.2s;
  cursor: pointer;
}

.combo-bar-segment:hover {
  opacity: 1;
}

.combo-bar-total-label {
  font-size: 11.5px;
  font-weight: 700;
  fill: #1e293b;
  font-family: 'Inter', Roboto, sans-serif;
}

.combo-bar-total-label.zero {
  fill: #9ca3af;
  font-size: 10px;
  font-weight: 500;
  opacity: 0.65;
}

.combo-bottom-label {
  font-size: 11.5px;
  font-weight: 600;
  fill: #475569;
  font-family: 'Inter', Roboto, sans-serif;
}

.font-inter-badge {
  font-family: 'Inter', Roboto, sans-serif !important;
}

.badge-trend-indicator {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 9999px;
  font-size: 0.74rem;
  font-weight: 700;
}

.trend-up {
  background-color: #dcfce7;
  color: #15803d;
}

.trend-down {
  background-color: #fee2e2;
  color: #b91c1c;
}

.trend-flat {
  background-color: #f1f5f9;
  color: #64748b;
}

.period-qty-cell {
  background-color: #faf8f5;
}

/* =========================================================================
   STYLE CHO MODAL GỬI MAIL BÁO CÁO
   ========================================================================= */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1rem;
}

.modal-card-custom {
  background-color: #ffffff;
  border-radius: 18px;
  width: 100%;
  max-width: 530px;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: modalPopIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPopIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.modal-header-custom {
  padding: 1.25rem 1.4rem;
  border-bottom: 1px solid #f0eae1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title-with-icon {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-icon-badge {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background-color: #ecfdf5;
  color: #16a34a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-main-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
}

.modal-sub-title {
  margin: 2px 0 0;
  font-size: 0.78rem;
  color: #64748b;
}

.btn-close-modal {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close-modal:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.modal-body-custom {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.form-group-custom {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label-custom {
  font-size: 0.84rem;
  font-weight: 700;
  color: #334155;
}

.radio-pill-group {
  display: flex;
  gap: 8px;
}

.radio-pill-btn {
  flex: 1;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid #d5cbc0;
  background-color: #fbf9f6;
  color: #475569;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.radio-pill-btn.active {
  background-color: #8c7355;
  border-color: #8c7355;
  color: #ffffff;
}

.form-row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.field-hint-text {
  font-size: 0.76rem;
  color: #94a3b8;
  line-height: 1.4;
}

.email-schedule-status-box {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
  padding: 12px 14px;
}

.schedule-status-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.82rem;
  color: #166534;
  margin-bottom: 4px;
}

.schedule-icon {
  color: #16a34a;
  flex-shrink: 0;
}

.schedule-status-text {
  margin: 0;
  font-size: 0.78rem;
  color: #15803d;
  line-height: 1.45;
}

.modal-footer-custom {
  padding: 1rem 1.4rem;
  background-color: #faf8f5;
  border-top: 1px solid #f0eae1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel-custom {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  color: #64748b;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

.btn-cancel-custom:hover {
  background-color: #f1f5f9;
}

.btn-send-custom {
  padding: 9px 22px;
  border-radius: 8px;
  border: none;
  background-color: #16a34a;
  color: #ffffff;
  font-size: 0.86rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 140px;
  font-family: inherit;
  transition: background-color 0.2s;
}

.btn-send-custom:hover:not(:disabled) {
  background-color: #15803d;
}

.btn-send-custom:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Responsive adjustments */
@media (max-width: 1280px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .two-donuts-grid {
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
