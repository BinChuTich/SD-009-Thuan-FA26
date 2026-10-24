<template>
  <div class="statistics-page-wrapper">
    <!-- 1. Tiêu đề chính -->
    <div class="page-breadcrumb">
      <router-link to="/thong-ke" class="crumb-parent">Quản lý thống kê</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Thống kê doanh thu</span>
    </div>
    <div class="page-top-bar" v-if="false">
      <div class="title-with-icon">
        <h1 class="page-main-title">Thống kê doanh thu</h1>
      </div>
    </div>

    <!-- 1. Card Bộ lọc thống kê -->
    <div class="content-card filter-card-box">
      <!-- Hàng trên: Tiêu đề bộ lọc & Cụm nút lọc nhanh + Xuất Excel -->
      <div class="filter-top-row">
        <div class="filter-heading">
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

        <!-- Kiểu thống kê (Select2) -->
        <div class="filter-field-item">
          <label class="field-label-tiny">Thống kê</label>
          <Select2Combobox
            v-model="filters.statType"
            :options="statTypeOptions"
            placeholder="Kiểu thống kê"
            search-placeholder="Tìm kiểu thống kê..."
            @change="applyFilters"
          />
        </div>

        <!-- Thương hiệu (Select2) -->
        <div class="filter-field-item">
          <label class="field-label-tiny">Thương hiệu</label>
          <Select2Combobox
            v-model="filters.brand"
            :options="brandComboboxOptions"
            placeholder="Tất cả thương hiệu"
            all-label="Tất cả thương hiệu"
            search-placeholder="Tìm thương hiệu..."
            @change="applyFilters"
          />
        </div>

        <!-- Tìm sản phẩm -->
        <div class="filter-field-item search-item">
          <label class="field-label-tiny">Tìm sản phẩm</label>
          <input
            type="text"
            v-model="filters.productKeyword"
            placeholder="Mã hoặc tên sản phẩm..."
            class="form-control-minimal"
            @input="debounceFilter"
          />
        </div>

        <!-- Nút Đặt lại -->
        <div class="filter-reset-action">
          <button type="button" class="btn-reset-round" @click="resetFilters" title="Đặt lại bộ lọc về mặc định">
            <span>Đặt lại</span>
          </button>
        </div>
      </div>

      <!-- Báo lỗi khoảng ngày nếu Từ ngày > Đến ngày -->
      <div v-if="dateFilterError" class="filter-error-msg">
        <span>{{ dateFilterError }}</span>
      </div>

      <!-- Hàng dưới cùng: Chấm xanh dương & Dòng trạng thái ngày -->
      <div class="filter-status-row">
        <span class="blue-status-dot"></span>
        <span class="filter-status-text">
          <template v-if="filters.fromDate && filters.toDate">
            Đang hiển thị dữ liệu từ ngày <strong>{{ formatDateDisplay(filters.fromDate) }}</strong> đến ngày <strong>{{ formatDateDisplay(filters.toDate) }}</strong>
          </template>
          <template v-else-if="filters.fromDate">
            Đang hiển thị dữ liệu từ ngày <strong>{{ formatDateDisplay(filters.fromDate) }}</strong>
          </template>
          <template v-else-if="filters.toDate">
            Đang hiển thị dữ liệu đến ngày <strong>{{ formatDateDisplay(filters.toDate) }}</strong>
          </template>
          <template v-else>
            Đang hiển thị tất cả dữ liệu
          </template>
        </span>
      </div>
    </div>

    <!-- 2. Hàng 6 Thẻ chỉ số tổng quan (Nhấp thẻ để xem bảng dữ liệu liên quan bên dưới) -->
    <div class="kpi-cards-grid">
      <!-- 1. Tổng doanh thu -->
      <div
        class="kpi-card interactive-kpi-card"
        :class="{ 'active-kpi': currentTableTab === 'payment' && !invoicePaymentFilter }"
        @click="switchTableTab('payment', 'all')"
        title="Nhấp để xem Bảng Cơ cấu Doanh thu & Phương thức thanh toán"
      >
        <div class="kpi-card-header">
          <span class="kpi-label">Tổng doanh thu</span>
          <span class="kpi-hint">Xem bảng</span>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.totalRevenue) }}</div>
        <div class="kpi-sub-calc">
          <span class="sub-highlight">{{ kpiData.completedOrders || 8 }} đơn hoàn thành</span> (loại đơn hủy)
        </div>
      </div>

      <!-- 2. Tiền mặt -->
      <div
        class="kpi-card interactive-kpi-card"
        :class="{ 'active-kpi': currentTableTab === 'payment' && invoicePaymentFilter === 'Tiền mặt' }"
        @click="switchTableTab('payment', 'cash')"
        title="Nhấp để xem Bảng chi tiết doanh thu Tiền mặt"
      >
        <div class="kpi-card-header">
          <span class="kpi-label">Tiền mặt</span>
          <span class="kpi-hint">Xem bảng</span>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.cashRevenue) }}</div>
        <div class="kpi-sub-calc">
          {{ paymentMethodSummary[0]?.orderCount || 6 }} đơn • {{ paymentMethodSummary[0]?.percent || '96.6' }}%
        </div>
      </div>

      <!-- 3. Chuyển khoản -->
      <div
        class="kpi-card interactive-kpi-card"
        :class="{ 'active-kpi': currentTableTab === 'payment' && invoicePaymentFilter === 'Chuyển khoản' }"
        @click="switchTableTab('payment', 'transfer')"
        title="Nhấp để xem Bảng chi tiết doanh thu Chuyển khoản"
      >
        <div class="kpi-card-header">
          <span class="kpi-label">Chuyển khoản</span>
          <span class="kpi-hint">Xem bảng</span>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.transferRevenue) }}</div>
        <div class="kpi-sub-calc">
          {{ paymentMethodSummary[1]?.orderCount || 2 }} đơn • {{ paymentMethodSummary[1]?.percent || '3.4' }}%
        </div>
      </div>

      <!-- 4. Tổng đơn hàng -->
      <div
        class="kpi-card interactive-kpi-card"
        :class="{ 'active-kpi': currentTableTab === 'channel' }"
        @click="switchTableTab('channel')"
        title="Nhấp để xem Bảng phân tích Đơn hàng & Kênh bán"
      >
        <div class="kpi-card-header">
          <span class="kpi-label">Tổng đơn hàng</span>
          <span class="kpi-hint">Xem bảng</span>
        </div>
        <div class="kpi-value text-dark">{{ kpiData.totalOrders.toLocaleString('vi-VN') }} đơn</div>
        <div class="kpi-sub-calc">
          <span class="text-success-bold">{{ kpiData.completedOrders || 8 }} hoàn thành</span>
          <span class="sub-sep">•</span>
          <span class="text-danger-bold">{{ kpiData.cancelledOrders || 2 }} đã hủy</span>
        </div>
      </div>

      <!-- 5. Giá trị TB đơn (AOV) -->
      <div
        class="kpi-card interactive-kpi-card"
        :class="{ 'active-kpi': currentTableTab === 'channel' }"
        @click="switchTableTab('channel')"
        title="Nhấp để xem Bảng phân tích Giá trị TB đơn (AOV)"
      >
        <div class="kpi-card-header">
          <span class="kpi-label">Giá trị TB đơn</span>
          <span class="kpi-hint">Xem bảng</span>
        </div>
        <div class="kpi-value text-dark">{{ formatCurrency(kpiData.aov) }}</div>
        <div class="kpi-sub-calc">
          Doanh thu / {{ kpiData.completedOrders || 8 }} đơn hoàn thành
        </div>
      </div>

      <!-- 6. Sản phẩm đã bán -->
      <div
        class="kpi-card interactive-kpi-card"
        :class="{ 'active-kpi': currentTableTab === 'products' }"
        @click="switchTableTab('products')"
        title="Nhấp để xem Bảng Thống kê chi tiết Sản phẩm đã bán"
      >
        <div class="kpi-card-header">
          <span class="kpi-label">Sản phẩm đã bán</span>
          <span class="kpi-hint">Xem bảng</span>
        </div>
        <div class="kpi-value text-dark">{{ kpiData.totalItemsSold.toLocaleString('vi-VN') }} cái</div>
        <div class="kpi-sub-calc">
          Tổng lượng xuất bán trong kỳ
        </div>
      </div>
    </div>

    <!-- 3. Biểu đồ cột: Thống kê Doanh thu & Sản phẩm bán theo Ngày / Tuần / Tháng / Năm -->
    <div class="content-card bar-chart-full-card">
      <div class="card-title-row bar-chart-header-row">
        <div class="card-title-group">
          <h3 class="card-heading">Biểu đồ {{ barChartMetricTitle }} theo {{ statTypeHeading }}</h3>
          <span class="badge-unit">Đơn vị: {{ barChartMetricUnit }}</span>
        </div>

        <div class="chart-controls-group">
          <!-- Nút chọn Mốc thời gian nhanh: Ngày / Tuần / Tháng / Năm -->
          <div class="segmented-pill-group">
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: filters.statType === 'day' }"
              @click="setStatTypeQuick('day')"
            >
              Ngày
            </button>
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: filters.statType === 'week' }"
              @click="setStatTypeQuick('week')"
            >
              Tuần
            </button>
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: filters.statType === 'month' }"
              @click="setStatTypeQuick('month')"
            >
              Tháng
            </button>
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: filters.statType === 'year' }"
              @click="setStatTypeQuick('year')"
            >
              Năm
            </button>
          </div>

          <!-- Nút chọn Chỉ số: Doanh thu / Sản phẩm / Cả hai -->
          <div class="segmented-pill-group metric-pill-group">
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: barChartMetric === 'revenue' }"
              @click="barChartMetric = 'revenue'"
            >
              Doanh thu
            </button>
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: barChartMetric === 'quantity' }"
              @click="barChartMetric = 'quantity'"
            >
              Sản phẩm
            </button>
            <button
              type="button"
              class="segment-pill-btn"
              :class="{ active: barChartMetric === 'both' }"
              @click="barChartMetric = 'both'"
            >
              Cả hai
            </button>
          </div>
        </div>
      </div>

      <!-- Chú thích chỉ số hiển thị khi chọn 'Cả hai' -->
      <div v-if="barChartMetric === 'both'" class="chart-legend-row">
        <div class="chart-legend-item">
          <span class="legend-rect rect-navy"></span>
          <span class="legend-text">Cột xanh navy: Doanh thu (VNĐ)</span>
        </div>
        <div class="chart-legend-item">
          <span class="legend-rect rect-green"></span>
          <span class="legend-text">Cột xanh lá: Sản phẩm bán ra (cái)</span>
        </div>
      </div>

      <!-- Biểu đồ cột Bar Chart với Trục Y, Gridlines, Tooltip nổi -->
      <div class="bar-chart-container" @mouseleave="hoveredBar = null">
        <div class="bar-chart-svg-wrap">
          <svg class="bar-chart-svg" viewBox="0 0 720 240" preserveAspectRatio="none">
            <!-- Vạch trục Y và Thang đo số liệu bên trái -->
            <g class="y-axis-group">
              <line x1="60" y1="30" x2="700" y2="30" stroke="#f1ede7" stroke-dasharray="3 3" />
              <text x="52" y="34" text-anchor="end" class="y-axis-label">{{ barChartYAxisLabels[4] }}</text>

              <line x1="60" y1="72" x2="700" y2="72" stroke="#f1ede7" stroke-dasharray="3 3" />
              <text x="52" y="76" text-anchor="end" class="y-axis-label">{{ barChartYAxisLabels[3] }}</text>

              <line x1="60" y1="115" x2="700" y2="115" stroke="#f1ede7" stroke-dasharray="3 3" />
              <text x="52" y="119" text-anchor="end" class="y-axis-label">{{ barChartYAxisLabels[2] }}</text>

              <line x1="60" y1="157" x2="700" y2="157" stroke="#f1ede7" stroke-dasharray="3 3" />
              <text x="52" y="161" text-anchor="end" class="y-axis-label">{{ barChartYAxisLabels[1] }}</text>

              <line x1="60" y1="200" x2="700" y2="200" stroke="#d5ccc1" stroke-width="1.2" />
              <text x="52" y="204" text-anchor="end" class="y-axis-label">0</text>
            </g>

            <!-- Các cột Bar Chart (Đơn hoặc Kép) -->
            <g
              v-for="(b, idx) in barChartComputedData"
              :key="idx"
              @mouseenter="onBarHover($event, b)"
              @mousemove="onBarHover($event, b)"
              style="cursor: pointer;"
            >
              <!-- TRƯỜNG HỢP 1: Chế độ Cả hai (Cột kép Doanh thu & Số lượng song song) -->
              <template v-if="barChartMetric === 'both'">
                <!-- Cột Doanh thu (Xanh navy) -->
                <rect
                  :x="b.x"
                  :y="b.revY"
                  :width="b.subWidth"
                  :height="b.revHeight"
                  rx="4"
                  :fill="b.revenue > 0 ? '#496883' : '#e5ded5'"
                  class="chart-bar-rect"
                />
                <!-- Cột Số lượng (Xanh lá) -->
                <rect
                  :x="b.x + b.subWidth + 2"
                  :y="b.qtyY"
                  :width="b.subWidth"
                  :height="b.qtyHeight"
                  rx="4"
                  :fill="b.value > 0 ? '#16a34a' : '#e5ded5'"
                  class="chart-bar-rect"
                />
              </template>

              <!-- TRƯỜNG HỢP 2: Chế độ Đơn lẻ (Doanh thu hoặc Sản phẩm) -->
              <template v-else>
                <rect
                  :x="b.x"
                  :y="b.y"
                  :width="b.width"
                  :height="b.height"
                  rx="5"
                  :fill="b.barColor"
                  class="chart-bar-rect"
                />
                <!-- Nhãn số trên đỉnh cột -->
                <text
                  :x="b.x + b.width / 2"
                  v-if="b.currentVal > 0 || barChartComputedData.length <= 10"
                  :y="b.currentVal > 0 ? b.y - 6 : 194"
                  text-anchor="middle"
                  :class="['bar-peak-number', { 'is-zero': b.currentVal === 0 }]"
                >
                  {{ b.topLabel }}
                </text>
              </template>

              <!-- Nhãn mốc thời gian ở trục X -->
              <text
                :x="b.x + b.width / 2"
                :y="barChartComputedData.length > 10 ? 218 : 220"
                :text-anchor="barChartComputedData.length > 10 ? 'end' : 'middle'"
                :transform="barChartComputedData.length > 10 ? `rotate(-45 ${b.x + b.width / 2} 218)` : ''"
                class="bar-bottom-label"
                v-if="barChartComputedData.length <= 15 || idx % (barChartComputedData.length > 25 ? 3 : 2) === 0 || idx === barChartComputedData.length - 1"
              >
                {{ b.label }}
              </text>
            </g>
          </svg>

          <!-- Tooltip nổi tương tác khi hover vào từng cột -->
          <div
            v-if="hoveredBar"
            class="bar-chart-tooltip"
            :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px' }"
          >
            <div class="tooltip-title">{{ hoveredBar.label }} ({{ hoveredBar.date }})</div>
            <div class="tooltip-body">
              <div class="tooltip-row">
                <span class="tooltip-dot dot-blue"></span>
                <span class="tooltip-text">Doanh thu:</span>
                <strong class="tooltip-val">{{ formatCurrency(hoveredBar.revenue) }}</strong>
              </div>
              <div class="tooltip-row">
                <span class="tooltip-dot dot-green"></span>
                <span class="tooltip-text">Sản phẩm bán:</span>
                <strong class="tooltip-val">{{ hoveredBar.value }} cái</strong>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. Lưới 2 biểu đồ hình tròn: Trạng thái đơn hàng & Cơ cấu thương hiệu (Tỷ lệ 1:1) -->
    <div class="charts-row-split-1-1">
      <!-- Bên trái (1/2): Biểu đồ Donut Trạng thái đơn hàng -->
      <div class="content-card order-status-card">
        <div class="card-title-row">
          <div class="card-title-group">
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
                <text x="152" y="20" fill="#15803d" font-size="12" font-weight="700">
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

      <!-- Bên phải (1/2): Biểu đồ Thương hiệu (Donut Chart) -->
      <div class="content-card chart-card-col-1">
        <div class="card-title-row">
          <div class="card-title-group">
            <h3 class="card-heading">Cơ cấu thương hiệu</h3>
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

    <!-- 5. Khu vực Các Bảng Thống Kê Liên Quan Theo Chỉ Số (Doanh thu, Tiền mặt, Chuyển khoản, Đơn hàng, Sản phẩm) -->
    <div class="kpi-related-tables-container" id="kpi-related-tables-section">
      <!-- Thanh điều hướng Tab các bảng thống kê liên quan -->
      <div class="tables-tab-switch-bar">
        <div class="tab-buttons-group">
          <button
            type="button"
            class="tab-switch-btn"
            :class="{ active: currentTableTab === 'payment' }"
            @click="switchTableTab('payment', 'all')"
          >
            Doanh thu & Thanh toán (2)
          </button>
          <button
            type="button"
            class="tab-switch-btn"
            :class="{ active: currentTableTab === 'channel' }"
            @click="switchTableTab('channel')"
          >
            Đơn hàng & Kênh bán ({{ kpiData.totalOrders }})
          </button>
          <button
            type="button"
            class="tab-switch-btn"
            :class="{ active: currentTableTab === 'staff' }"
            @click="switchTableTab('staff')"
          >
            Doanh số Nhân viên ({{ staffSummary.length }})
          </button>
          <button
            type="button"
            class="tab-switch-btn"
            :class="{ active: currentTableTab === 'products' }"
            @click="switchTableTab('products')"
          >
            Sản phẩm đã bán ({{ kpiData.totalItemsSold }})
          </button>
          <button
            type="button"
            class="tab-switch-btn"
            :class="{ active: currentTableTab === 'all' }"
            @click="currentTableTab = 'all'"
          >
            Xem tất cả các bảng
          </button>
        </div>
      </div>

      <!-- BẢNG 1: DOANH THU & PHƯƠNG THỨC THANH TOÁN (Giải trình Thẻ 1, 2, 3) -->
      <div
        v-show="currentTableTab === 'payment' || currentTableTab === 'all'"
        class="content-card related-table-card"
      >
        <div class="card-title-row">
          <div class="card-title-group">
            <h3 class="card-heading">Bảng Doanh thu theo Hình thức thanh toán</h3>
            <span class="badge-fixed-row">Giải trình Tiền mặt & Chuyển khoản</span>
          </div>
          <span class="badge-total-orders">Tổng doanh thu: {{ formatCurrency(kpiData.totalRevenue) }}</span>
        </div>

        <div class="table-responsive-clean">
          <table class="minimal-styled-table">
            <thead>
              <tr class="header-brown-beige">
                <th style="width: 220px">HÌNH THỨC THANH TOÁN</th>
                <th style="width: 140px; text-align: center">SỐ ĐƠN HÀNG</th>
                <th style="width: 180px; text-align: right">TỔNG DOANH THU</th>
                <th style="width: 130px; text-align: center">TỶ TRỌNG (%)</th>
                <th style="width: 170px; text-align: right">AOV (TB/ĐƠN)</th>
                <th style="width: 150px; text-align: center">THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in paymentMethodSummary" :key="item.methodKey" class="table-data-row">
                <td>
                  <div class="method-cell-title">
                    <strong class="text-dark">{{ item.name }}</strong>
                  </div>
                </td>
                <td style="text-align: center" class="font-bold text-black-bold">
                  {{ item.orderCount }} đơn
                </td>
                <td style="text-align: right" class="font-bold" :class="item.methodKey === 'cash' ? 'text-blue-amount' : 'text-green-amount'">
                  {{ formatCurrency(item.revenue) }}
                </td>
                <td style="text-align: center">
                  <span class="badge-growth-pill growth-neutral font-bold">{{ item.percent }}%</span>
                </td>
                <td style="text-align: right" class="text-gray-aov">
                  {{ formatCurrency(item.aov) }}
                </td>
                <td style="text-align: center">
                  <button
                    type="button"
                    class="btn-detail-link"
                    :class="{ 'active-filter-btn': invoicePaymentFilter === item.filterVal }"
                    @click="togglePaymentFilter(item.filterVal)"
                  >
                    {{ invoicePaymentFilter === item.filterVal ? 'Đang lọc đơn' : 'Xem đơn hàng' }}
                  </button>
                </td>
              </tr>
              <!-- Dòng Tổng cộng -->
              <tr class="table-data-row summary-total-tr">
                <td class="font-bold text-dark">TỔNG CỘNG DOANH THU</td>
                <td style="text-align: center" class="font-bold text-black-bold">{{ kpiData.totalOrders }} đơn</td>
                <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(kpiData.totalRevenue) }}</td>
                <td style="text-align: center" class="font-bold">100.0%</td>
                <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(kpiData.aov) }}</td>
                <td style="text-align: center">
                  <button
                    v-if="invoicePaymentFilter"
                    type="button"
                    class="btn-reset-round small"
                    @click="invoicePaymentFilter = ''; invoicePagination.page = 0"
                  >
                    Xem tất cả đơn
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Bảng hóa đơn chi tiết theo phương thức thanh toán -->
        <div class="sub-table-section">
          <div class="sub-table-header">
            <h4 class="sub-table-title">
              Danh sách đơn hàng {{ invoicePaymentFilter ? '[' + invoicePaymentFilter + ']' : 'toàn bộ' }} ({{ filteredInvoicesList.length }} đơn)
            </h4>
            <div class="search-invoice-box">
              <input
                type="text"
                v-model="invoiceSearchQuery"
                placeholder="Tìm mã HĐ, khách hàng, SĐT..."
                class="form-control-minimal"
                @input="invoicePagination.page = 0"
              />
            </div>
          </div>

          <div class="table-responsive-clean">
            <table class="minimal-styled-table">
              <thead>
                <tr class="header-brown-beige">
                  <th style="width: 50px; text-align: center">STT</th>
                  <th style="width: 130px">MÃ HÓA ĐƠN</th>
                  <th style="width: 150px">THỜI GIAN</th>
                  <th style="min-width: 180px">KHÁCH HÀNG</th>
                  <th style="width: 140px">NHÂN VIÊN</th>
                  <th style="width: 120px; text-align: center">THANH TOÁN</th>
                  <th style="width: 140px; text-align: right">TỔNG TIỀN</th>
                  <th style="width: 120px; text-align: center">TRẠNG THÁI</th>
                  <th style="width: 100px; text-align: center">CHI TIẾT</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="paginatedInvoices.length === 0">
                  <td colspan="9" style="text-align: center; padding: 2rem; color: #8c979d;">
                    Không có hóa đơn nào phù hợp với bộ lọc thanh toán này.
                  </td>
                </tr>
                <tr v-for="(inv, idx) in paginatedInvoices" :key="inv.id || idx" class="table-data-row invoice-row-hover">
                  <td style="text-align: center" class="text-muted-stt">
                    {{ (invoicePagination.page * invoicePagination.size) + idx + 1 }}
                  </td>
                  <td>
                    <span class="code-cell-clickable" @click="goToInvoiceDetail(inv.maHoaDon)" title="Xem chi tiết hóa đơn này">
                      {{ inv.maHoaDon }}
                    </span>
                  </td>
                  <td class="text-muted-sm">
                    {{ formatDateTimeDisplay(inv.ngayTao) }}
                  </td>
                  <td>
                    <div class="customer-info-cell">
                      <strong class="cust-name-text">{{ inv.tenKhachHang }}</strong>
                      <span v-if="inv.soDienThoaiKhachHang" class="cust-phone-text">{{ inv.soDienThoaiKhachHang }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="employee-name-tag">{{ inv.tenNhanVien || '---' }}</span>
                  </td>
                  <td style="text-align: center">
                    <span class="badge-pay-method" :class="getPayMethodClass(inv.hinhThucThanhToan)">
                      {{ inv.hinhThucThanhToan || 'Tiền mặt' }}
                    </span>
                  </td>
                  <td style="text-align: right" class="font-bold text-black-bold">
                    {{ formatCurrency(inv.tienSauGiamGia || inv.tongTien) }}
                  </td>
                  <td style="text-align: center">
                    <span class="badge-invoice-status" :class="getInvoiceStatusClass(inv.trangThai)">
                      {{ getInvoiceStatusText(inv.trangThai) }}
                    </span>
                  </td>
                  <td style="text-align: center">
                    <button
                      type="button"
                      class="btn-detail-link"
                      @click="goToInvoiceDetail(inv.maHoaDon)"
                      title="Nhấp để mở trang chi tiết hóa đơn"
                    >
                      Chi tiết
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Phân trang hóa đơn -->
          <div v-if="filteredInvoicesList.length > 0" class="pagination-section">
            <div class="pagination-info">
              Hiển thị {{ filteredInvoicesList.length ? (invoicePagination.page * invoicePagination.size + 1) : 0 }} - {{ Math.min((invoicePagination.page + 1) * invoicePagination.size, filteredInvoicesList.length) }} / {{ filteredInvoicesList.length }} hóa đơn
            </div>
            <div class="pagination-buttons">
              <button
                type="button"
                class="btn-pager"
                :disabled="invoicePagination.page === 0"
                @click="changeInvoicePage(invoicePagination.page)"
              >‹</button>
              <template v-for="(p, idx) in visibleInvoicePages" :key="idx">
                <span v-if="p === '...'" class="pg-dots">...</span>
                <button
                  v-else
                  type="button"
                  class="btn-pager"
                  :class="{ active: p === invoicePagination.page + 1 }"
                  @click="changeInvoicePage(p)"
                >{{ p }}</button>
              </template>
              <button
                type="button"
                class="btn-pager"
                :disabled="invoicePagination.page >= invoiceTotalPages - 1"
                @click="changeInvoicePage(invoicePagination.page + 2)"
              >›</button>
            </div>
          </div>
        </div>
      </div>

      <!-- BẢNG 2: ĐƠN HÀNG THEO KÊNH BÁN & TRẠNG THÁI (Giải trình Thẻ 4: Tổng đơn hàng, Thẻ 5: AOV) -->
      <div
        v-show="currentTableTab === 'channel' || currentTableTab === 'all'"
        class="content-card related-table-card"
      >
        <div class="card-title-row">
          <div class="card-title-group">
            <h3 class="card-heading">Bảng Phân tích Đơn hàng theo Kênh bán & Trạng thái</h3>
            <span class="badge-fixed-row">Giải trình Tổng đơn hàng & AOV</span>
          </div>
          <span class="badge-total-orders">Tổng đơn: {{ kpiData.totalOrders }} đơn • AOV: {{ formatCurrency(kpiData.aov) }}</span>
        </div>

        <div class="tables-split-2-cols">
          <!-- Bảng 2A: Theo Kênh bán (Tại quầy vs Online) -->
          <div class="sub-table-block">
            <h4 class="sub-block-title">Kênh Bán Hàng</h4>
            <div class="table-responsive-clean">
              <table class="minimal-styled-table">
                <thead>
                  <tr class="header-brown-beige">
                    <th>KÊNH BÁN</th>
                    <th style="text-align: center">SỐ ĐƠN</th>
                    <th style="text-align: right">DOANH THU</th>
                    <th style="text-align: center">TỶ TRỌNG</th>
                    <th style="text-align: right">AOV</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="ch in channelSummary" :key="ch.channelKey" class="table-data-row">
                    <td>
                      <strong class="text-dark">{{ ch.name }}</strong>
                    </td>
                    <td style="text-align: center" class="font-bold text-black-bold">{{ ch.orderCount }} đơn</td>
                    <td style="text-align: right" class="font-bold text-blue-amount">{{ formatCurrency(ch.revenue) }}</td>
                    <td style="text-align: center"><span class="badge-growth-pill growth-neutral font-bold">{{ ch.percent }}%</span></td>
                    <td style="text-align: right" class="text-gray-aov">{{ formatCurrency(ch.aov) }}</td>
                  </tr>
                  <tr class="table-data-row summary-total-tr">
                    <td class="font-bold text-dark">TỔNG CỘNG</td>
                    <td style="text-align: center" class="font-bold text-black-bold">{{ kpiData.totalOrders }} đơn</td>
                    <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(kpiData.totalRevenue) }}</td>
                    <td style="text-align: center" class="font-bold">100.0%</td>
                    <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(kpiData.aov) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Bảng 2B: Theo Trạng thái đơn -->
          <div class="sub-table-block">
            <h4 class="sub-block-title">Trạng Thái Đơn Hàng</h4>
            <div class="table-responsive-clean">
              <table class="minimal-styled-table">
                <thead>
                  <tr class="header-brown-beige">
                    <th>TRẠNG THÁI</th>
                    <th style="text-align: center">SỐ LƯỢNG</th>
                    <th style="text-align: center">TỶ LỆ (%)</th>
                    <th style="text-align: right">DOANH THU</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="st in orderStatusTableRows" :key="st.key" class="table-data-row">
                    <td>
                      <span class="legend-color-dot" :style="{ backgroundColor: st.color }"></span>
                      <strong class="text-dark">{{ st.label }}</strong>
                    </td>
                    <td style="text-align: center" class="font-bold text-black-bold">{{ st.count }} đơn</td>
                    <td style="text-align: center"><span class="badge-growth-pill growth-neutral font-bold">{{ st.percent }}%</span></td>
                    <td style="text-align: right" class="font-bold" :class="st.revenue > 0 ? 'text-green-amount' : 'text-gray-aov'">
                      {{ formatCurrency(st.revenue) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- BẢNG 3: DOANH SỐ THEO NHÂN VIÊN / THU NGÂN -->
      <div
        v-show="currentTableTab === 'staff' || currentTableTab === 'all'"
        class="content-card related-table-card"
      >
        <div class="card-title-row">
          <div class="card-title-group">
            <h3 class="card-heading">Bảng Thống kê Doanh số theo Nhân viên / Thu ngân</h3>
            <span class="badge-fixed-row">{{ staffSummary.length }} nhân viên ghi nhận doanh số</span>
          </div>
          <span class="badge-total-orders">Tổng: {{ formatCurrency(kpiData.totalRevenue) }}</span>
        </div>

        <div class="table-responsive-clean">
          <table class="minimal-styled-table">
            <thead>
              <tr class="header-brown-beige">
                <th style="width: 60px; text-align: center">STT</th>
                <th style="min-width: 200px">NHÂN VIÊN / THU NGÂN</th>
                <th style="width: 130px; text-align: center">SỐ ĐƠN CHỐT</th>
                <th style="width: 160px; text-align: right">TIỀN MẶT THU</th>
                <th style="width: 160px; text-align: right">CHUYỂN KHOẢN THU</th>
                <th style="width: 180px; text-align: right">TỔNG DOANH SỐ</th>
                <th style="width: 120px; text-align: center">TỶ TRỌNG (%)</th>
                <th style="width: 150px; text-align: right">AOV</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="staffSummary.length === 0">
                <td colspan="8" style="text-align: center; padding: 2rem; color: #8c979d;">
                  Chưa có dữ liệu nhân viên trong khoảng thời gian này.
                </td>
              </tr>
              <tr v-for="(staff, idx) in staffSummary" :key="staff.name || idx" class="table-data-row">
                <td style="text-align: center" class="text-muted-stt">{{ idx + 1 }}</td>
                <td>
                  <div class="staff-cell">
                    <span class="staff-avatar-circle">{{ staff.name.charAt(0).toUpperCase() }}</span>
                    <strong class="text-dark">{{ staff.name }}</strong>
                  </div>
                </td>
                <td style="text-align: center" class="font-bold text-black-bold">{{ staff.orderCount }} đơn</td>
                <td style="text-align: right" class="text-blue-amount">{{ formatCurrency(staff.cashRev) }}</td>
                <td style="text-align: right" class="text-green-amount">{{ formatCurrency(staff.transferRev) }}</td>
                <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(staff.totalRev) }}</td>
                <td style="text-align: center"><span class="badge-growth-pill growth-neutral font-bold">{{ staff.percent }}%</span></td>
                <td style="text-align: right" class="text-gray-aov">{{ formatCurrency(staff.aov) }}</td>
              </tr>
              <!-- Dòng Tổng cộng -->
              <tr v-if="staffSummary.length > 0" class="table-data-row summary-total-tr">
                <td colspan="2" class="font-bold text-dark">TỔNG CỘNG NHÂN VIÊN</td>
                <td style="text-align: center" class="font-bold text-black-bold">{{ kpiData.totalOrders }} đơn</td>
                <td style="text-align: right" class="font-bold text-blue-amount">{{ formatCurrency(kpiData.cashRevenue) }}</td>
                <td style="text-align: right" class="font-bold text-green-amount">{{ formatCurrency(kpiData.transferRevenue) }}</td>
                <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(kpiData.totalRevenue) }}</td>
                <td style="text-align: center" class="font-bold">100.0%</td>
                <td style="text-align: right" class="font-bold text-black-bold">{{ formatCurrency(kpiData.aov) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- BẢNG 4: THỐNG KÊ CHI TIẾT SẢN PHẨM ĐÃ BÁN (Giải trình Thẻ 6: Sản phẩm đã bán 123 cái) -->
      <div
        v-show="currentTableTab === 'products' || currentTableTab === 'all'"
        class="content-card product-stats-card"
      >
      <!-- Thanh công cụ đầu Card -->
      <div class="product-stats-toolbar">
        <div class="card-title-group">
          <h3 class="card-heading">Thống kê sản phẩm</h3>
        </div>

        <div class="product-stats-actions">
          <!-- Ô tìm kiếm Mã, tên, thương hiệu -->
          <div class="search-product-box">
            <input
              type="text"
              v-model="productTableQuery"
              placeholder="Mã, tên, thương hiệu..."
              class="form-control-minimal"
              @input="onProductSearchChange"
            />
          </div>

          <!-- Combobox lọc sắp xếp (Select2) -->
          <div class="product-sort-combobox">
            <Select2Combobox
              v-model="productSortBy"
              :options="productSortOptions"
              placeholder="Sắp xếp sản phẩm"
              search-placeholder="Tìm tiêu chí sắp xếp..."
              @change="sortProductTable"
            />
          </div>

          <!-- Nút Đặt lại -->
          <button type="button" class="btn-reset-round small" @click="resetProductTableFilters">
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
                    :src="formatProductImage(p.image, p)"
                    :alt="p.name"
                    class="product-thumb-img"
                    @error="onImgError($event, p)"
                  />
                  <div class="product-info-text">
                    <strong class="product-title-bold" :title="p.name">{{ p.name }}</strong>
                    <span class="product-size-hint">Size: {{ p.sizes || 'Tiêu chuẩn' }}</span>
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

      <!-- PHÂN TRANG SẢN PHẨM -->
      <div v-if="filteredProductsList.length > 0" class="pagination-section">
        <div class="pagination-info">
          Hiển thị {{ filteredProductsList.length ? (productPagination.page * productPagination.size + 1) : 0 }} - {{ Math.min((productPagination.page + 1) * productPagination.size, filteredProductsList.length) }} / {{ filteredProductsList.length }} sản phẩm
        </div>

        <div class="pagination-buttons">
          <button
            type="button"
            class="btn-pager"
            :disabled="productPagination.page === 0"
            @click="changeProductPage(productPagination.page)"
          >
            ‹
          </button>

          <template v-for="(p, idx) in visibleProductPages" :key="idx">
            <span v-if="p === '...'" class="pg-dots">...</span>
            <button
              v-else
              type="button"
              class="btn-pager"
              :class="{ active: p === productPagination.page + 1 }"
              @click="changeProductPage(p)"
            >
              {{ p }}
            </button>
          </template>

          <button
            type="button"
            class="btn-pager"
            :disabled="productPagination.page >= productTotalPages - 1"
            @click="changeProductPage(productPagination.page + 2)"
          >
            ›
          </button>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import * as XLSX from 'xlsx'
import { showToast } from '@/utils/dialog.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()
const defaultProductImage = '/images/products/default_tshirt.jpg'

// Đồng bộ ảnh sản phẩm chuẩn với hệ thống
function formatProductImage(url, product) {
  if (!url || typeof url !== 'string' || url.trim() === '' || url.includes('photo-1521572267360-ee0c2909d518')) {
    const sid = product?.id
    const scode = (product?.code || product?.maSanPham || '').toUpperCase()
    if (sid === 1 || scode.includes('SP001') || scode.includes('SP01')) return '/images/products/sp01_front.jpg'
    if (sid === 2 || scode.includes('SP002') || scode.includes('SP02')) return '/images/products/sp02_front.jpg'
    if (sid === 3 || scode.includes('SP003') || scode.includes('SP03')) return '/images/products/sp03_front.jpg'
    if (sid === 4 || scode.includes('SP004') || scode.includes('SP04')) return '/images/products/sp04_front.jpg'
    return defaultProductImage
  }

  let cleanUrl = url.trim()
  if (cleanUrl.includes('|')) {
    cleanUrl = cleanUrl.split('|')[1].trim()
  }

  if (cleanUrl.startsWith('data:') || cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {
    return cleanUrl
  }

  if (cleanUrl.startsWith('/images/')) {
    return cleanUrl
  }

  if (cleanUrl.startsWith('/uploads/') || cleanUrl.startsWith('uploads/')) {
    const path = cleanUrl.startsWith('/') ? cleanUrl : '/' + cleanUrl
    return `http://localhost:8080${path}`
  }

  return cleanUrl.startsWith('/') ? cleanUrl : `/${cleanUrl}`
}

function onImgError(e, product) {
  const sid = product?.id
  const scode = (product?.code || product?.maSanPham || '').toUpperCase()
  if (sid === 1 || scode.includes('SP001') || scode.includes('SP01')) {
    e.target.src = '/images/products/sp01_front.jpg'
  } else if (sid === 2 || scode.includes('SP002') || scode.includes('SP02')) {
    e.target.src = '/images/products/sp02_front.jpg'
  } else if (sid === 3 || scode.includes('SP003') || scode.includes('SP03')) {
    e.target.src = '/images/products/sp03_front.jpg'
  } else if (sid === 4 || scode.includes('SP004') || scode.includes('SP04')) {
    e.target.src = '/images/products/sp04_front.jpg'
  } else {
    e.target.src = defaultProductImage
  }
}
const activeBottomTab = ref('both')

// 1. Quản lý Bộ lọc thống kê
const quickFilter = ref('today')
const brandOptions = ref([])
const dateFilterError = ref('')
const loadingOverview = ref(false)

const filters = reactive({
  fromDate: getTodayDateString(),
  toDate: '',
  statType: 'day',
  brand: '',
  productKeyword: ''
})

// Cấu hình Options Select2 cho Thống kê
const statTypeOptions = [
  { value: 'day', label: 'Theo ngày' },
  { value: 'week', label: 'Theo tuần' },
  { value: 'month', label: 'Theo tháng' },
  { value: 'year', label: 'Theo năm' }
]

const brandComboboxOptions = computed(() => {
  const list = [{ value: '', label: 'Tất cả thương hiệu' }]
  if (Array.isArray(brandOptions.value)) {
    brandOptions.value.forEach(b => {
      list.push({
        value: b.id,
        label: b.ten || b.tenThuongHieu || `Thương hiệu #${b.id}`
      })
    })
  }
  return list
})

// Cấu hình Options Select2 cho Danh sách Hóa đơn
const invoiceTypeOptions = [
  { value: '', label: 'Tất cả loại đơn' },
  { value: '1', label: 'Tại quầy' },
  { value: '2', label: 'Online' }
]

const invoicePaymentOptions = [
  { value: '', label: 'Tất cả hình thức TT' },
  { value: 'Tiền mặt', label: 'Tiền mặt' },
  { value: 'Chuyển khoản', label: 'Chuyển khoản' },
  { value: 'Kết hợp', label: 'Kết hợp' }
]

const invoiceStatusOptions = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: '5', label: 'Hoàn thành' },
  { value: 'processing', label: 'Đang xử lý' },
  { value: '6', label: 'Đã hủy' }
]

const pageSizeInvoiceOptions = [
  { value: 5, label: 'Hiển thị 5 bản ghi / trang' },
  { value: 10, label: 'Hiển thị 10 bản ghi / trang' },
  { value: 20, label: 'Hiển thị 20 bản ghi / trang' }
]

// Cấu hình Options Select2 cho Thống kê sản phẩm
const productSortOptions = [
  { value: 'best_seller', label: 'Bán chạy nhất' },
  { value: 'slow_seller', label: 'Bán chậm nhất' },
  { value: 'highest_revenue', label: 'Doanh thu cao nhất' },
  { value: 'highest_stock', label: 'Tồn kho nhiều nhất' },
  { value: 'highest_return', label: 'Số lượng trả nhiều nhất' }
]

const pageSizeProductOptions = [
  { value: 5, label: 'Hiển thị 5 bản ghi / trang' },
  { value: 10, label: 'Hiển thị 10 bản ghi / trang' },
  { value: 20, label: 'Hiển thị 20 bản ghi / trang' },
  { value: 50, label: 'Hiển thị 50 bản ghi / trang' }
]

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
    filters.toDate = ''
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
  quickFilter.value = 'all'
  filters.fromDate = '2024-01-01'
  filters.toDate = getTodayDateString()
  filters.statType = 'day'
  filters.brand = ''
  filters.productKeyword = ''
  productTableQuery.value = ''
  productSortBy.value = 'best_seller'
  invoicePaymentFilter.value = ''
  invoiceSearchQuery.value = ''
  fetchOverviewData()
  showToast('Đã đặt lại toàn bộ bộ lọc Dashboard về mặc định!', 'info')
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
  completedOrders: 0,
  cancelledOrders: 0,
  processingOrders: 0,
  aov: 0,
  totalItemsSold: 0
})

// 3. Biểu đồ cột Bar Chart: Đa dạng chỉ số Doanh thu / Sản phẩm / Cả hai & Tooltip
const rawDailySales = ref([])
const barChartMetric = ref('revenue') // 'revenue' | 'quantity' | 'both'
const hoveredBar = ref(null)
const tooltipPos = reactive({ x: 0, y: 0 })

const barChartMetricTitle = computed(() => {
  if (barChartMetric.value === 'revenue') return 'Doanh thu bán hàng'
  if (barChartMetric.value === 'quantity') return 'Sản phẩm bán được'
  return 'Doanh thu & Sản phẩm bán được'
})

const barChartMetricUnit = computed(() => {
  if (barChartMetric.value === 'revenue') return 'VNĐ'
  if (barChartMetric.value === 'quantity') return 'Sản phẩm (cái)'
  return 'VNĐ / Cái'
})

function setStatTypeQuick(type) {
  filters.statType = type
  applyFilters()
}

function onBarHover(e, b) {
  hoveredBar.value = b
  const rect = e.currentTarget.closest('.bar-chart-svg-wrap')?.getBoundingClientRect()
  if (rect) {
    tooltipPos.x = Math.max(10, Math.min(rect.width - 200, e.clientX - rect.left - 70))
    tooltipPos.y = Math.max(5, e.clientY - rect.top - 75)
  }
}

// Tính toán các nhãn trục Y (0, 25%, 50%, 75%, 100%)
const barChartYAxisLabels = computed(() => {
  const data = rawDailySales.value || []
  if (barChartMetric.value === 'revenue' || barChartMetric.value === 'both') {
    const maxRev = Math.max(...data.map(d => Number(d.revenue || 0)), 1000000)
    return [
      '0',
      formatShortCurrency(maxRev * 0.25),
      formatShortCurrency(maxRev * 0.5),
      formatShortCurrency(maxRev * 0.75),
      formatShortCurrency(maxRev)
    ]
  } else {
    const maxQty = Math.max(...data.map(d => Number(d.value || 0)), 10)
    return [
      '0',
      String(Math.round(maxQty * 0.25)),
      String(Math.round(maxQty * 0.5)),
      String(Math.round(maxQty * 0.75)),
      String(Math.round(maxQty))
    ]
  }
})

function formatShortCurrency(val) {
  if (val >= 1000000000) return (val / 1000000000).toFixed(1) + 'B'
  if (val >= 1000000) return (val / 1000000).toFixed(1) + 'M'
  if (val >= 1000) return (val / 1000).toFixed(0) + 'k'
  return String(Math.round(val))
}

const barChartComputedData = computed(() => {
  const data = rawDailySales.value
  if (!data || data.length === 0) return []

  const maxVal = Math.max(...data.map(d => Number(d.value || 0)), 10)
  const maxRev = Math.max(...data.map(d => Number(d.revenue || 0)), 1000000)

  const chartHeight = 160
  const baseY = 200
  const totalWidth = 630
  const startX = 65
  const count = data.length
  const barWidth = Math.min(42, Math.max(14, Math.floor((totalWidth / count) * 0.6)))
  const step = count > 1 ? (totalWidth - barWidth) / (count - 1) : 0

  return data.map((item, idx) => {
    const val = Number(item.value || 0)
    const rev = Number(item.revenue || 0)
    const x = count === 1 ? (startX + totalWidth / 2 - barWidth / 2) : (startX + idx * step)

    // Chiều cao theo từng metric
    const revH = Math.max((rev / maxRev) * chartHeight, rev > 0 ? 8 : 2)
    const qtyH = Math.max((val / maxVal) * chartHeight, val > 0 ? 8 : 2)

    let currentVal = val
    let currentH = qtyH
    let barColor = val > 0 ? '#16a34a' : '#e5ded5'
    let topLabel = String(val)

    if (barChartMetric.value === 'revenue') {
      currentVal = rev
      currentH = revH
      barColor = rev > 0 ? '#496883' : '#e5ded5'
      topLabel = rev > 0 ? formatShortCurrency(rev) : '0'
    }

    const subWidth = Math.max(6, Math.floor((barWidth - 2) / 2))

    return {
      ...item,
      x,
      y: baseY - currentH,
      width: barWidth,
      height: currentH,
      currentVal,
      topLabel,
      barColor,
      // Hỗ trợ chế độ 'both' cột kép
      subWidth,
      revHeight: revH,
      revY: baseY - revH,
      qtyHeight: qtyH,
      qtyY: baseY - qtyH
    }
  })
})

// 4. Biểu đồ Donut Thương hiệu (Tỷ trọng chính xác & Tổng luôn đúng 100.0%)
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
  const total = data.reduce((s, item) => s + Number(item[metricKey] || 0), 0)
  if (total <= 0) return []

  const circumference = 2 * Math.PI * 70 // ~ 439.82
  let accumulatedRatio = 0
  let accumulatedPercent = 0

  return data.map((item, idx) => {
    const val = Number(item[metricKey] || 0)
    let percent = Math.round((val / total) * 1000) / 10
    if (idx === data.length - 1) {
      // Cân bằng phần dư ở lát cắt cuối cùng để đảm bảo tổng luôn chính xác 100.0%
      percent = Math.max(0, Math.round((100.0 - accumulatedPercent) * 10) / 10)
    } else {
      accumulatedPercent += percent
    }

    const ratio = val / total
    const dashLength = ratio * circumference
    const offset = -accumulatedRatio * circumference
    accumulatedRatio += ratio

    return {
      name: item.name,
      value: val,
      percentage: percent.toFixed(1),
      color: item.color || '#496883',
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

const visibleProductPages = computed(() => {
  const total = productTotalPages.value
  const curr = productPagination.page + 1
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const pages = []
  if (curr <= 4) {
    for (let i = 1; i <= 5; i++) pages.push(i)
    pages.push('...')
    pages.push(total)
  } else if (curr >= total - 3) {
    pages.push(1)
    pages.push('...')
    for (let i = total - 4; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    pages.push('...')
    for (let i = curr - 1; i <= curr + 1; i++) pages.push(i)
    pages.push('...')
    pages.push(total)
  }
  return pages
})

function changeProductPage(p) {
  if (p === '...' || p < 1 || p > productTotalPages.value) return
  productPagination.page = p - 1
}

const paginatedProducts = computed(() => {
  const start = productPagination.page * productPagination.size
  return filteredProductsList.value.slice(start, start + productPagination.size)
})

function goToProductPage(page) {
  if (page < 0 || page >= productTotalPages.value) return
  productPagination.page = page
}

// Quản lý các Bảng thống kê liên quan theo chỉ số KPI
const currentTableTab = ref('payment') // 'payment' | 'channel' | 'staff' | 'products' | 'all'

function switchTableTab(tabKey, subFilter = null) {
  currentTableTab.value = tabKey
  if (tabKey === 'payment') {
    if (subFilter === 'cash') {
      invoicePaymentFilter.value = 'Tiền mặt'
    } else if (subFilter === 'transfer') {
      invoicePaymentFilter.value = 'Chuyển khoản'
    } else {
      invoicePaymentFilter.value = ''
    }
    invoiceTypeFilter.value = ''
    invoicePagination.page = 0
  } else if (tabKey === 'channel') {
    invoicePaymentFilter.value = ''
    invoicePagination.page = 0
  }

  setTimeout(() => {
    const el = document.getElementById('kpi-related-tables-section')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, 50)
}

function togglePaymentFilter(val) {
  if (invoicePaymentFilter.value === val) {
    invoicePaymentFilter.value = ''
  } else {
    invoicePaymentFilter.value = val
  }
  invoicePagination.page = 0
}

// 1. Tổng hợp Doanh thu & Phương thức thanh toán (Tiền mặt, Chuyển khoản)
const paymentMethodSummary = computed(() => {
  const totalRev = Number(kpiData.totalRevenue) || 0
  const cashRev = Number(kpiData.cashRevenue) || 0
  const transferRev = Number(kpiData.transferRevenue) || 0

  let cashOrders = 0
  let transferOrders = 0

  const validInvs = allInvoicesList.value.filter(inv =>
    (Number(inv.trangThai) === 5 || Number(inv.trangThaiThanhToan) === 1) && Number(inv.trangThai) !== 6
  )

  validInvs.forEach(inv => {
    const method = (inv.hinhThucThanhToan || '').toLowerCase()
    if (method.includes('chuyển khoản') || method.includes('chuyen khoan') || method === 'transfer') {
      transferOrders++
    } else {
      cashOrders++
    }
  })

  if (cashOrders === 0 && transferOrders === 0 && validInvs.length > 0) {
    cashOrders = validInvs.length
  }

  return [
    {
      methodKey: 'cash',
      name: 'Tiền mặt',
      filterVal: 'Tiền mặt',
      revenue: cashRev,
      orderCount: cashOrders,
      percent: totalRev > 0 ? ((cashRev / totalRev) * 100).toFixed(1) : '0.0',
      aov: cashOrders > 0 ? Math.round(cashRev / cashOrders) : 0
    },
    {
      methodKey: 'transfer',
      name: 'Chuyển khoản (Ngân hàng)',
      filterVal: 'Chuyển khoản',
      revenue: transferRev,
      orderCount: transferOrders,
      percent: totalRev > 0 ? ((transferRev / totalRev) * 100).toFixed(1) : '0.0',
      aov: transferOrders > 0 ? Math.round(transferRev / transferOrders) : 0
    }
  ]
})

// 2. Tổng hợp Đơn hàng theo Kênh bán (Tại quầy vs Online)
const channelSummary = computed(() => {
  const validInvs = allInvoicesList.value.filter(inv =>
    (Number(inv.trangThai) === 5 || Number(inv.trangThaiThanhToan) === 1) && Number(inv.trangThai) !== 6
  )
  const totalRev = Number(kpiData.totalRevenue) || 1

  let posOrders = 0
  let posRev = 0
  let onlineOrders = 0
  let onlineRev = 0

  validInvs.forEach(inv => {
    const val = Number(inv.tienSauGiamGia || inv.tongTien) || 0
    if (Number(inv.loaiDon) === 1) {
      posOrders++
      posRev += val
    } else {
      onlineOrders++
      onlineRev += val
    }
  })

  return [
    {
      channelKey: 'pos',
      name: 'Bán tại quầy (POS)',
      orderCount: posOrders,
      revenue: posRev,
      percent: ((posRev / totalRev) * 100).toFixed(1),
      aov: posOrders > 0 ? Math.round(posRev / posOrders) : 0
    },
    {
      channelKey: 'online',
      name: 'Giao hàng (Online)',
      orderCount: onlineOrders,
      revenue: onlineRev,
      percent: ((onlineRev / totalRev) * 100).toFixed(1),
      aov: onlineOrders > 0 ? Math.round(onlineRev / onlineOrders) : 0
    }
  ]
})

// 3. Tổng hợp Bảng trạng thái đơn hàng
const orderStatusTableRows = computed(() => {
  const tot = orderStatusTotal.value || 1
  let completedRev = 0
  let processingRev = 0
  let cancelledRev = 0

  allInvoicesList.value.forEach(inv => {
    const val = Number(inv.tienSauGiamGia || inv.tongTien) || 0
    const st = Number(inv.trangThai)
    if (st === 5) completedRev += val
    else if (st === 6) cancelledRev += val
    else if (st >= 1 && st <= 4) processingRev += val
  })

  return [
    {
      key: 'completed',
      label: 'Hoàn thành',
      color: '#16A34A',
      count: orderStatusCounts.completed,
      percent: ((orderStatusCounts.completed / tot) * 100).toFixed(1),
      revenue: completedRev
    },
    {
      key: 'processing',
      label: 'Đang xử lý / Giao hàng',
      color: '#F59E0B',
      count: orderStatusCounts.processing,
      percent: ((orderStatusCounts.processing / tot) * 100).toFixed(1),
      revenue: processingRev
    },
    {
      key: 'cancelled',
      label: 'Đã hủy',
      color: '#EF4444',
      count: orderStatusCounts.cancelled,
      percent: ((orderStatusCounts.cancelled / tot) * 100).toFixed(1),
      revenue: cancelledRev
    }
  ]
})

// 4. Tổng hợp Doanh số theo Nhân viên / Thu ngân
const staffSummary = computed(() => {
  const validInvs = allInvoicesList.value.filter(inv =>
    (Number(inv.trangThai) === 5 || Number(inv.trangThaiThanhToan) === 1) && Number(inv.trangThai) !== 6
  )
  const totalRev = Number(kpiData.totalRevenue) || 1

  const map = {}
  validInvs.forEach(inv => {
    const name = inv.tenNhanVien || 'Thu ngân'
    if (!map[name]) {
      map[name] = { name, orderCount: 0, cashRev: 0, transferRev: 0, totalRev: 0 }
    }
    const val = Number(inv.tienSauGiamGia || inv.tongTien) || 0
    map[name].orderCount++
    map[name].totalRev += val
    const method = (inv.hinhThucThanhToan || '').toLowerCase()
    if (method.includes('chuyển khoản') || method.includes('chuyen khoan') || method === 'transfer') {
      map[name].transferRev += val
    } else {
      map[name].cashRev += val
    }
  })

  const list = Object.values(map)
  list.sort((a, b) => b.totalRev - a.totalRev)
  return list.map(st => ({
    ...st,
    percent: ((st.totalRev / totalRev) * 100).toFixed(1),
    aov: st.orderCount > 0 ? Math.round(st.totalRev / st.orderCount) : 0
  }))
})

// Quản lý Danh sách hóa đơn trong kỳ thống kê
const allInvoicesList = ref([])
const invoiceSearchQuery = ref('')
const invoicePaymentFilter = ref('')
const invoiceStatusFilter = ref('')
const invoiceTypeFilter = ref('')
const invoicePagination = reactive({
  page: 0,
  size: 10
})

function resetInvoiceFilters() {
  invoiceSearchQuery.value = ''
  invoicePaymentFilter.value = ''
  invoiceStatusFilter.value = ''
  invoiceTypeFilter.value = ''
  invoicePagination.page = 0
}

// Kiểm tra hóa đơn chờ tại quầy (tuyệt đối không hiển thị ở màn thống kê)
const isWaitingInvoice = (inv) => {
  if (!inv || typeof inv !== 'object') return false
  const isStoreWaiting = Number(inv.loaiDon) === 1 &&
      Number(inv.trangThai) === 1 &&
      (inv.trangThaiThanhToan == null || Number(inv.trangThaiThanhToan) === 0)
  const note = String(inv.ghiChu || '').toLowerCase()
  const isWaitingNote = note.includes('hóa đơn chờ') || note.includes('hoa don cho')
  const isZeroStoreDraft = Number(inv.loaiDon) === 1 && Number(inv.trangThai) === 1 && Number(inv.tienSauGiamGia || inv.tongTien || 0) === 0
  return isStoreWaiting || isWaitingNote || isZeroStoreDraft
}

const filteredInvoicesList = computed(() => {
  let list = allInvoicesList.value.filter(inv => !isWaitingInvoice(inv))
  const q = invoiceSearchQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(inv =>
      (inv.maHoaDon && inv.maHoaDon.toLowerCase().includes(q)) ||
      (inv.tenKhachHang && inv.tenKhachHang.toLowerCase().includes(q)) ||
      (inv.soDienThoaiKhachHang && inv.soDienThoaiKhachHang.includes(q)) ||
      (inv.tenNhanVien && inv.tenNhanVien.toLowerCase().includes(q))
    )
  }

  if (invoicePaymentFilter.value) {
    list = list.filter(inv => {
      const p = (inv.hinhThucThanhToan || '').toLowerCase()
      const target = invoicePaymentFilter.value.toLowerCase()
      return p.includes(target)
    })
  }

  if (invoiceTypeFilter.value) {
    list = list.filter(inv => Number(inv.loaiDon) === Number(invoiceTypeFilter.value))
  }

  if (invoiceStatusFilter.value) {
    if (invoiceStatusFilter.value === 'processing') {
      list = list.filter(inv => Number(inv.trangThai) >= 1 && Number(inv.trangThai) <= 4)
    } else {
      list = list.filter(inv => Number(inv.trangThai) === Number(invoiceStatusFilter.value))
    }
  }

  return list
})

const invoiceTotalPages = computed(() => {
  return Math.ceil(filteredInvoicesList.value.length / invoicePagination.size) || 1
})

const visibleInvoicePages = computed(() => {
  const total = invoiceTotalPages.value
  const curr = invoicePagination.page + 1
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const pages = []
  if (curr <= 4) {
    for (let i = 1; i <= 5; i++) pages.push(i)
    pages.push('...')
    pages.push(total)
  } else if (curr >= total - 3) {
    pages.push(1)
    pages.push('...')
    for (let i = total - 4; i <= total; i++) pages.push(i)
  } else {
    pages.push(1)
    pages.push('...')
    for (let i = curr - 1; i <= curr + 1; i++) pages.push(i)
    pages.push('...')
    pages.push(total)
  }
  return pages
})

function changeInvoicePage(p) {
  if (p === '...' || p < 1 || p > invoiceTotalPages.value) return
  invoicePagination.page = p - 1
}

const paginatedInvoices = computed(() => {
  const start = invoicePagination.page * invoicePagination.size
  return filteredInvoicesList.value.slice(start, start + invoicePagination.size)
})

function goToInvoiceDetail(maHoaDon) {
  if (!maHoaDon) return
  router.push(`/hoa-don/${maHoaDon}`)
}

function scrollToInvoices() {
  if (activeBottomTab.value === 'products') {
    activeBottomTab.value = 'invoices'
  }
  setTimeout(() => {
    const el = document.getElementById('invoices-section')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }, 50)
}

function handleKpiFilter(type) {
  resetInvoiceFilters()
  if (type === 'cash') {
    invoicePaymentFilter.value = 'Tiền mặt'
  } else if (type === 'transfer') {
    invoicePaymentFilter.value = 'Chuyển khoản'
  }
  scrollToInvoices()
}

function handleStatusFilter(status) {
  resetInvoiceFilters()
  invoiceStatusFilter.value = status
  scrollToInvoices()
}

function handlePeriodRowClick(periodKey) {
  if (periodKey === 'today') setQuickFilter('today')
  else if (periodKey === 'thisWeek' || periodKey === 'week') setQuickFilter('7days')
  else if (periodKey === 'thisMonth') setQuickFilter('thisMonth')
  else if (periodKey === 'thisYear') setQuickFilter('thisYear')
  scrollToInvoices()
}

function formatDateTimeDisplay(dtStr) {
  if (!dtStr) return '--:--'
  try {
    const d = new Date(dtStr)
    if (isNaN(d.getTime())) return dtStr
    const hh = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const yyyy = d.getFullYear()
    return `${hh}:${min} ${dd}/${mm}/${yyyy}`
  } catch (e) {
    return dtStr
  }
}

function getInvoiceStatusText(st) {
  const n = Number(st)
  if (n === 5) return 'Hoàn thành'
  if (n === 6) return 'Đã hủy'
  if (n === 2) return 'Đã xác nhận'
  if (n === 3) return 'Chờ vận chuyển'
  if (n === 4) return 'Đang vận chuyển'
  if (n === 7) return 'Giao hàng thất bại'
  return 'Chờ xác nhận'
}

function getInvoiceStatusClass(st) {
  const n = Number(st)
  if (n === 5) return 'status-completed'
  if (n === 6) return 'status-cancelled'
  if (n >= 2 && n <= 4) return 'status-processing'
  return 'status-pending'
}

function getPayMethodClass(method) {
  const m = (method || '').toLowerCase()
  if (m.includes('kết hợp')) return 'pay-combined'
  if (m.includes('chuyển khoản') || m.includes('ngân hàng') || m.includes('vnpay')) return 'pay-transfer'
  return 'pay-cash'
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
        kpiData.completedOrders = Number(data.kpi.completedOrders ?? (data.orderStatusDonut?.completed ?? 8))
        kpiData.cancelledOrders = Number(data.kpi.cancelledOrders ?? (data.orderStatusDonut?.cancelled ?? 2))
        kpiData.processingOrders = Number(data.kpi.processingOrders ?? (data.orderStatusDonut?.processing ?? 0))
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

      if (Array.isArray(data.invoices)) {
        allInvoicesList.value = data.invoices.filter(inv => !isWaitingInvoice(inv))
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
    const toDatePart = filters.toDate || getTodayDateString()
    const fileName = `BaoCao_ThongKe_DoanhThu_${filters.fromDate}_den_${toDatePart}.xlsx`
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
      ['Thời gian lọc:', filters.toDate ? `Từ ${formatDateDisplay(filters.fromDate)} đến ${formatDateDisplay(filters.toDate)}` : `Từ ${formatDateDisplay(filters.fromDate)}`],
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

    const toDateFallback = filters.toDate || getTodayDateString()
    const fileName = `Thong-ke-doanh-thu-${filters.fromDate}-den-${toDateFallback}.xlsx`
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
  font-size: 0.95rem;
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
  background-color: var(--blue, #496883);
  border-color: var(--blue, #496883);
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}

.btn-export-excel {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #edf5ef;
  border-color: #cce5d3;
  color: #247541;
  font-weight: 700;
  margin-left: 6px;
}

.btn-export-excel:hover {
  background-color: #deede1;
  border-color: #b3dac0;
  color: #1e6336;
}

/* Hàng giữa: 5 trường lọc + Nút Đặt lại */
.filter-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1.15fr 1.5fr auto;
  gap: 12px;
  align-items: flex-end;
  padding-top: 1rem;
  border-top: 1px solid #f2eee8;
}

@media (max-width: 1200px) {
  .filter-fields-row {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .filter-fields-row {
    grid-template-columns: 1fr;
  }
}

.filter-field-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label-tiny {
  font-size: 0.82rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.form-control-minimal {
  width: 100%;
  height: 40px;
  background-color: #fcfbf9;
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 0.95rem;
  color: #334155;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.form-control-minimal:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
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
  font-size: 0.88rem;
  font-weight: 600;
  margin-top: 4px;
}

.btn-reset-round {
  height: 40px;
  padding: 0 16px;
  border-radius: 20px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  color: #5d6c73;
  font-size: 0.95rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-reset-round:hover {
  background-color: #f0f4f8;
  border-color: #b5a494;
  color: #39464c;
}

.btn-reset-round.small {
  height: 34px;
  padding: 0 14px;
  font-size: 0.88rem;
}

/* Hàng dưới cùng: Trạng thái ngày */
.filter-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
  color: #55656d;
}

.blue-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background-color: #3b576e;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  display: inline-block;
  flex-shrink: 0;
}

.filter-status-text strong {
  color: #1e293b;
  font-weight: 700;
}

/* 2. Hàng 6 Thẻ chỉ số tổng quan (1 hàng ngang thu nhỏ gọn, khung viền tối giản) */
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1024px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }
}

@media (max-width: 580px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
}

.kpi-card {
  background-color: #ffffff;
  border-radius: 10px;
  border: 1px solid #ece6dc;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.02);
  padding: 0.72rem 0.85rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 70px;
  box-sizing: border-box;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.kpi-card.clickable-kpi-card {
  cursor: pointer;
}

.kpi-card.clickable-kpi-card:hover {
  border-color: var(--blue, #496883);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-bottom: 3px;
}

.kpi-label {
  font-size: 0.72rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi-hint {
  font-size: 0.68rem;
  color: #94a3b8;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.kpi-value {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.25;
  margin-bottom: 0;
  letter-spacing: -0.2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Hiệu ứng tương tác cho các thẻ KPI liên kết đến bảng */
.kpi-card.interactive-kpi-card {
  cursor: pointer;
  transition: all 0.22s ease;
  position: relative;
}

.kpi-card.interactive-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.12);
  border-color: #9cb5c9;
}

.kpi-card.interactive-kpi-card.active-kpi {
  border-color: var(--blue, #496883) !important;
  background-color: #f7fafc;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.15);
}

.kpi-card.interactive-kpi-card .kpi-hint {
  color: var(--blue, #496883);
  font-weight: 600;
}

/* Khu vực Bảng Thống kê liên quan theo chỉ số KPI */
.kpi-related-tables-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-top: 0.5rem;
}

.related-table-card {
  padding: 1.35rem 1.5rem;
}

.method-cell-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
}

.method-ico {
  font-size: 1.15rem;
  line-height: 1;
}

.summary-total-tr {
  background-color: #faf7f2 !important;
  border-top: 2px solid #e6dec3;
}

.summary-total-tr td {
  padding-top: 12px;
  padding-bottom: 12px;
  font-size: 0.95rem;
}

.active-filter-btn {
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  border-color: var(--blue, #496883) !important;
}

.sub-table-section {
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px dashed #e2d9cd;
}

.sub-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 10px;
}

.sub-table-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #2c3e50;
}

.tables-split-2-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

@media (max-width: 1024px) {
  .tables-split-2-cols {
    grid-template-columns: 1fr;
  }
}

.sub-table-block {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.sub-block-title {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 700;
  color: #2c3e50;
}

.staff-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.staff-avatar-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #eaf2f6;
  color: var(--blue, #496883);
  font-weight: 700;
  font-size: 0.85rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d3e1ec;
}

/* 3. Biểu đồ cột sản phẩm bán được full width */
.bar-chart-full-card {
  width: 100%;
}

/* 4. Lưới 2 biểu đồ tròn trạng thái & thương hiệu (Tỷ lệ 1 : 1 cân đối) */
.charts-row-split-1-1 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 1024px) {
  .charts-row-split-1-1 {
    grid-template-columns: 1fr;
  }
}

/* 3 & 4. Lưới biểu đồ (Tỷ lệ 1.6 : 1) */
.charts-row-split-2-1 {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 14px;
}

@media (max-width: 1024px) {
  .charts-row-split-2-1 {
    grid-template-columns: 1fr;
  }
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

.card-heading {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
}

.badge-unit,
.badge-fixed-row,
.badge-total-orders {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #eaf2f6;
  border: 1px solid #d3e1ec;
  color: var(--blue, #496883);
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
  fill: #3b576e !important;
  opacity: 0.95;
}

.bar-peak-number {
  font-size: 10px;
  font-weight: 500;
  fill: var(--blue, #496883);
}

.bar-peak-number.is-zero {
  font-size: 9px;
  font-weight: 400;
  fill: #94a3b8;
}

.bar-bottom-label {
  font-size: 9.5px;
  font-weight: 400;
  fill: #64748b;
}

/* Donut Chart styling */
.btn-toggle-metric {
  background: #fbf9f6;
  border: 1px solid #d5cbc0;
  padding: 3px 9px;
  font-size: 0.84rem;
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
  font-size: 0.82rem;
  color: #8c979d;
  font-weight: 500;
}

.donut-center-val {
  font-size: 1.25rem;
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
  font-size: 0.88rem;
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
  font-size: 0.88rem;
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

/* 4 & 5. Bảng dữ liệu tối giản */
.table-responsive-clean {
  overflow-x: auto;
}

.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.minimal-styled-table th {
  background-color: #f7f5ef !important;
  border-bottom: 2px solid #e9e5db !important;
  color: var(--blue, #496883) !important;
  font-weight: 700;
  font-size: 0.88rem;
  letter-spacing: 0.5px;
  padding: 13px 14px;
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
  padding: 13px 14px;
  vertical-align: middle;
}

/* Định dạng màu sắc cột */
.text-black-bold {
  color: #1e293b;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.text-blue-amount {
  color: var(--blue, #496883);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

.text-green-amount {
  color: #3b576e;
  font-variant-numeric: tabular-nums;
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
  font-size: 0.84rem;
  font-weight: 700;
}

.growth-positive {
  background-color: #eaf5ed;
  border: 1px solid #bce1c7;
  color: #247541;
}

.growth-negative {
  background-color: #f7edeb;
  border: 1px solid #ecc9c3;
  color: #b34a3b;
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

.product-sort-combobox {
  min-width: 195px;
}

.invoice-filter-combobox {
  min-width: 165px;
}

.size-select-wrap {
  width: 220px;
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
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.35;
}

.product-size-hint {
  font-size: 0.84rem;
  color: #8c979d;
  font-weight: 500;
}

.brand-badge-tag {
  background-color: #f5f2eb;
  color: #496883;
  font-size: 0.84rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.text-red-return {
  color: #dc2626;
}

.text-green-stock {
  color: #3b576e;
  font-variant-numeric: tabular-nums;
}

.text-muted-stt {
  color: #94a3b8;
}

.code-cell {
  letter-spacing: 0.2px;
}

/* PHÂN TRANG ĐỒNG BỘ CHUẨN CÁC MÀN KHÁC (Danh sách hóa đơn, Đợt giảm giá, ...) */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  margin-top: 8px;
}

.pagination-info {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  color: #555e65;
  line-height: 1.5;
}

.pagination-buttons {
  display: flex;
  align-items: center;
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

.pg-dots {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  color: #8c979d;
  font-size: 0.85rem;
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

/* Liên kết hóa đơn với thống kê */
.clickable-kpi-text {
  cursor: pointer;
  transition: color 0.15s;
}
.clickable-kpi-text:hover {
  color: var(--blue, #496883);
  text-decoration: underline;
}
.cell-link-click {
  cursor: pointer;
  color: var(--blue, #496883);
  font-variant-numeric: tabular-nums;
  padding: 2px 8px;
  border-radius: 6px;
  background-color: #eaf2f6;
  border: 1px solid #d0deea;
  transition: all 0.15s;
}
.cell-link-click:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  text-decoration: underline;
}
.clickable-legend {
  cursor: pointer;
  transition: all 0.15s;
  padding: 2px 6px;
  border-radius: 4px;
}
.clickable-legend:hover {
  background-color: #f1ede7;
  text-decoration: underline;
}
.invoice-stats-card {
  margin-top: 1.25rem;
}
.badge-total-count {
  font-size: 0.88rem;
  background-color: #f1ede7;
  color: var(--blue, #496883);
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 600;
}
.invoice-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.search-invoice-box {
  width: 220px;
}
.code-cell-clickable {
  color: var(--blue, #496883);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  cursor: pointer;
  padding: 3px 8px;
  border-radius: 6px;
  background-color: #eaf2f6;
  border: 1px solid #d0deea;
  display: inline-block;
  transition: all 0.15s;
}
.code-cell-clickable:hover {
  background-color: #3b576e;
  color: #ffffff;
}
.customer-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cust-name-text {
  font-size: 0.95rem;
  color: #2b353b;
}
.cust-phone-text {
  font-size: 0.82rem;
  color: #8c979d;
}
.employee-name-tag {
  font-size: 0.88rem;
  color: #556268;
}
.badge-order-type {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  display: inline-block;
}
.type-pos {
  background-color: #fef3c7;
  color: #b45309;
}
.type-online {
  background-color: #e0e7ff;
  color: #4338ca;
}
.badge-pay-method {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  display: inline-block;
}
.pay-cash {
  background-color: #f0fdf4;
  color: #247541;
  border: 1px solid #bbf7d0;
}
.pay-transfer {
  background-color: #eaf2f6;
  border: 1px solid #d0deea;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
.pay-combined {
  background-color: #faf5ff;
  color: #7e22ce;
  border: 1px solid #e9d5ff;
}
.badge-invoice-status {
  font-size: 0.82rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  display: inline-block;
}
.status-completed {
  background-color: #eaf5ed;
  border: 1px solid #bce1c7;
  color: #166534;
}
.status-cancelled {
  background-color: #f7edeb;
  border: 1px solid #ecc9c3;
  color: #991b1b;
}
.status-processing {
  background-color: #fef9c3;
  color: #854d0e;
}
.status-pending {
  background-color: #f1f5f9;
  color: #475569;
}
.btn-detail-link {
  border: 1px solid #dcd3c8;
  background: #ffffff;
  color: var(--blue, #496883);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-detail-link:hover {
  background-color: #3b576e;
  color: #ffffff;
  border-color: var(--blue, #496883);
}
.invoice-row-hover:hover {
  background-color: #faf8f5;
}

/* 5. Khu vực chuyển đổi Tab danh sách bên dưới */
.bottom-tables-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-top: 0.5rem;
}

.tables-tab-switch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 2px solid #ece6dc;
  padding-bottom: 8px;
}

.tab-buttons-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tab-switch-btn {
  background-color: #ffffff;
  border: 1px solid #dfd8cc;
  font-size: 0.95rem;
  font-weight: 600;
  color: #556268;
  padding: 8px 18px;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.tab-switch-btn:hover {
  color: #1e293b;
  background-color: #f5efe6;
  border-color: #cbbeab;
}

.tab-switch-btn.active {
  color: #ffffff;
  background-color: var(--blue, #496883);
  border-color: var(--blue, #496883);
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.25);
}

/* 3. Phong cách Bộ điều khiển Biểu đồ Bar Chart & Tooltip */
.bar-chart-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.chart-controls-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.segmented-pill-group {
  display: inline-flex;
  background-color: #f1ede7;
  border-radius: 20px;
  padding: 3px;
  border: 1px solid #e5ded5;
}

.segment-pill-btn {
  background: transparent;
  border: none;
  border-radius: 16px;
  padding: 4px 12px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.segment-pill-btn:hover {
  color: #1e293b;
}

.segment-pill-btn.active {
  background-color: #ffffff;
  color: var(--blue, #496883);
  font-weight: 700;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.metric-pill-group .segment-pill-btn.active {
  background-color: var(--blue, #496883);
  color: #ffffff;
}

.chart-legend-row {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  margin-bottom: 4px;
}

.chart-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  color: #475569;
}

.legend-rect {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.rect-navy {
  background-color: #496883;
}

.rect-green {
  background-color: #16a34a;
}

.y-axis-label {
  font-size: 10px;
  fill: #8c9ba5;
  font-weight: 600;
}

.bar-chart-tooltip {
  position: absolute;
  background: rgba(30, 41, 59, 0.94);
  color: #ffffff;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 0.8rem;
  pointer-events: none;
  z-index: 20;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
  white-space: nowrap;
}

.tooltip-title {
  font-weight: 700;
  font-size: 0.82rem;
  margin-bottom: 4px;
  color: #f1f5f9;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  padding-bottom: 3px;
}

.tooltip-body {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.tooltip-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tooltip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.tooltip-dot.dot-blue {
  background-color: #60a5fa;
}

.tooltip-dot.dot-green {
  background-color: #4ade80;
}

.tooltip-text {
  color: #cbd5e1;
}

.tooltip-val {
  color: #ffffff;
  margin-left: auto;
}

.kpi-sub-calc {
  margin-top: 5px;
  font-size: 0.76rem;
  color: #64748b;
  line-height: 1.25;
}

.sub-highlight {
  color: #15803d;
  font-weight: 700;
}

.text-success-bold {
  color: #16a34a;
  font-weight: 700;
}

.text-danger-bold {
  color: #dc2626;
  font-weight: 700;
}

.sub-sep {
  margin: 0 4px;
  color: #94a3b8;
}

</style>
