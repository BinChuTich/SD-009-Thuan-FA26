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
          <!-- 1. Nút Gửi mail tổng kết -->
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
            <span>Gửi mail tổng kết</span>
          </button>

          <!-- 2. Nút So sánh -->
          <button
            type="button"
            class="pill-btn btn-compare-highlight"
            @click="switchViewMode('comparison')"
            title="Mở màn hình So sánh & Đối chiếu tăng trưởng doanh thu"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="20" x2="18" y2="10" />
              <line x1="12" y1="20" x2="12" y2="4" />
              <line x1="6" y1="20" x2="6" y2="14" />
            </svg>
            <span>So sánh</span>
          </button>

          <!-- 3. Nút Xuất Excel -->
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
            <span>Xuất Excel</span>
          </button>
        </div>
      </div>

      <!-- 2. Card Bộ lọc thống kê -->
      <div class="content-card filter-card-box">
        <!-- Hàng trên: Tiêu đề bộ lọc & Cụm nút lọc nhanh mốc thời gian -->
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
                :max="maxFromDate"
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
                :max="todayStr"
                :min="filters.fromDate || undefined"
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

      <!-- 3. Hàng 5 Thẻ chỉ số tổng quan (5 KPI Cards - Đều có nút Chi tiết mở bảng riêng) -->
      <div class="kpi-cards-grid overview-kpi-grid">
        <!-- 1. Doanh thu thực tế -->
        <div
          class="kpi-card kpi-card-clickable"
          :class="{ 'kpi-card-active theme-green': selectedKpiType === 'actual_revenue' }"
          @click="toggleKpiTable('actual_revenue')"
          title="Nhấn để xem hoặc ẩn danh sách đơn hàng Doanh thu thực tế"
        >
          <div class="kpi-card-header-flex">
            <div class="kpi-icon-wrap pastel-green">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
                <polyline points="17 6 23 6 23 12" />
              </svg>
            </div>
            <div class="kpi-order-action-pill pill-green" :class="{ 'is-open': selectedKpiType === 'actual_revenue' }">
              <span>{{ selectedKpiType === 'actual_revenue' ? 'Đang mở' : 'Chi tiết' }}</span>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="pill-arrow" :class="{ 'arrow-up': selectedKpiType === 'actual_revenue' }">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
          <div class="kpi-value text-dark">{{ formatCurrency(kpiData.totalRevenue) }}</div>
          <div class="kpi-label">Doanh thu thực tế</div>
        </div>

        <!-- 2. Doanh thu dự kiến -->
        <div
          class="kpi-card kpi-card-clickable"
          :class="{ 'kpi-card-active theme-amber': selectedKpiType === 'expected_revenue' }"
          @click="toggleKpiTable('expected_revenue')"
          title="Nhấn để xem hoặc ẩn danh sách đơn hàng Doanh thu dự kiến"
        >
          <div class="kpi-card-header-flex">
            <div class="kpi-icon-wrap pastel-amber">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </div>
            <div class="kpi-order-action-pill pill-amber" :class="{ 'is-open': selectedKpiType === 'expected_revenue' }">
              <span>{{ selectedKpiType === 'expected_revenue' ? 'Đang mở' : 'Chi tiết' }}</span>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="pill-arrow" :class="{ 'arrow-up': selectedKpiType === 'expected_revenue' }">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
          <div class="kpi-value text-dark">{{ formatCurrency(kpiData.expectedRevenue) }}</div>
          <div class="kpi-label">Doanh thu dự kiến</div>
        </div>

        <!-- 3. Tiền mặt & Chuyển khoản (Gộp 2 thẻ thành 1) -->
        <div
          class="kpi-card kpi-card-clickable"
          :class="{ 'kpi-card-active theme-blue': selectedKpiType === 'payment' }"
          @click="toggleKpiTable('payment')"
          title="Nhấn để xem hoặc ẩn danh sách đơn hàng theo Tiền mặt & Chuyển khoản"
        >
          <div class="kpi-card-header-flex">
            <div class="kpi-icon-wrap pastel-blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="5" width="20" height="14" rx="2" />
                <line x1="2" y1="10" x2="22" y2="10" />
              </svg>
            </div>
            <div class="kpi-order-action-pill pill-blue" :class="{ 'is-open': selectedKpiType === 'payment' }">
              <span>{{ selectedKpiType === 'payment' ? 'Đang mở' : 'Chi tiết' }}</span>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="pill-arrow" :class="{ 'arrow-up': selectedKpiType === 'payment' }">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
          <div class="kpi-dual-values">
            <div class="dual-val-row">
              <span class="dual-label">Tiền mặt:</span>
              <span class="dual-num">{{ formatCurrency(kpiData.cashRevenue) }}</span>
            </div>
            <div class="dual-val-row">
              <span class="dual-label">Chuyển khoản:</span>
              <span class="dual-num">{{ formatCurrency(kpiData.transferRevenue) }}</span>
            </div>
          </div>
          <div class="kpi-label">Tiền mặt & Chuyển khoản</div>
        </div>

        <!-- 4. Tổng đơn hàng -->
        <div
          class="kpi-card kpi-card-clickable"
          :class="{ 'kpi-card-active theme-purple': selectedKpiType === 'total_orders' }"
          @click="toggleKpiTable('total_orders')"
          title="Nhấn để xem hoặc ẩn danh sách tất cả đơn hàng"
        >
          <div class="kpi-card-header-flex">
            <div class="kpi-icon-wrap pastel-purple">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
            </div>
            <div class="kpi-order-action-pill pill-purple" :class="{ 'is-open': selectedKpiType === 'total_orders' }">
              <span>{{ selectedKpiType === 'total_orders' ? 'Đang mở' : 'Chi tiết' }}</span>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="pill-arrow" :class="{ 'arrow-up': selectedKpiType === 'total_orders' }">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
          <div class="kpi-value text-dark">{{ kpiData.totalOrders.toLocaleString('vi-VN') }}</div>
          <div class="kpi-label">Tổng đơn hàng</div>
        </div>

        <!-- 5. Sản phẩm đã bán -->
        <div
          class="kpi-card kpi-card-clickable"
          :class="{ 'kpi-card-active theme-orange': selectedKpiType === 'products' }"
          @click="toggleKpiTable('products')"
          title="Nhấn để xem hoặc ẩn danh sách sản phẩm đã bán"
        >
          <div class="kpi-card-header-flex">
            <div class="kpi-icon-wrap pastel-orange">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                <line x1="12" y1="22.08" x2="12" y2="12" />
              </svg>
            </div>
            <div class="kpi-order-action-pill pill-orange" :class="{ 'is-open': selectedKpiType === 'products' }">
              <span>{{ selectedKpiType === 'products' ? 'Đang mở' : 'Chi tiết' }}</span>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="pill-arrow" :class="{ 'arrow-up': selectedKpiType === 'products' }">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </div>
          </div>
          <div class="kpi-value text-dark">{{ kpiData.totalItemsSold.toLocaleString('vi-VN') }}</div>
          <div class="kpi-label">Sản phẩm đã bán</div>
        </div>
      </div>

      <!-- 3.5. BẢNG SỔ RA CHI TIẾT (Mặc định ẨN - Tự động hiển thị tương ứng khi click vào bất kỳ thẻ KPI nào trong 5 thẻ) -->
      <transition name="order-table-slide">
        <div v-if="selectedKpiType !== null" class="card-box table-card order-stats-card" id="order-stats-section">
          <!-- TRƯỜNG HỢP 1: THẺ 1-4 LIÊN QUAN ĐẾN ĐƠN HÀNG & DOANH THU -->
          <template v-if="selectedKpiType !== 'products'">
            <!-- HEADER KHUNG BẢNG -->
            <div class="table-card-header">
              <div class="table-title-area">
                <svg class="icon-section" viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="9" cy="21" r="1" />
                  <circle cx="20" cy="21" r="1" />
                  <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
                </svg>
                <div class="table-heading-wrap">
                  <div class="table-heading-line">
                    <h3 class="card-heading">{{ kpiTableTitle }}</h3>
                    <span v-if="kpiTableBadge.text" class="kpi-table-badge" :class="kpiTableBadge.class">
                      {{ kpiTableBadge.text }}
                    </span>
                  </div>
                  <span class="table-total-count">({{ filteredOrdersList.length }} / {{ currentKpiOrdersTotal }} đơn)</span>
                </div>
              </div>

              <div class="table-card-header-actions">
                <!-- CỤM NÚT LỌC KÊNH BÁN: TẤT CẢ / TẠI QUẦY / ONLINE -->
                <div class="order-channel-tabs">
                  <button
                    type="button"
                    class="channel-tab-btn"
                    :class="{ active: orderChannelFilter === 'all' }"
                    @click="setOrderChannelFilter('all')"
                    title="Xem tất cả đơn trong mục này"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="3" y="3" width="7" height="7"></rect>
                      <rect x="14" y="3" width="7" height="7"></rect>
                      <rect x="14" y="14" width="7" height="7"></rect>
                      <rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                    <span>Tất cả</span>
                    <span class="tab-badge">{{ currentKpiOrdersTotal }}</span>
                  </button>

                  <button
                    type="button"
                    class="channel-tab-btn"
                    :class="{ active: orderChannelFilter === 'store' }"
                    @click="setOrderChannelFilter('store')"
                    title="Lọc đơn Tại quầy"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                      <polyline points="9 22 9 12 15 12 15 22"></polyline>
                    </svg>
                    <span>Tại quầy</span>
                    <span class="tab-badge badge-store-count">{{ currentKpiStoreCount }}</span>
                  </button>

                  <button
                    type="button"
                    class="channel-tab-btn"
                    :class="{ active: orderChannelFilter === 'online' }"
                    @click="setOrderChannelFilter('online')"
                    title="Lọc đơn Online"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="10"></circle>
                      <line x1="2" y1="12" x2="22" y2="12"></line>
                      <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
                    </svg>
                    <span>Online</span>
                    <span class="tab-badge badge-online-count">{{ currentKpiOnlineCount }}</span>
                  </button>
                </div>

                <!-- Nút Thu gọn bảng -->
                <button
                  type="button"
                  class="btn-collapse-order-table"
                  @click="closeKpiTable"
                  title="Thu gọn bảng chi tiết"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                  <span>Thu gọn</span>
                </button>
              </div>
            </div>

            <!-- THANH CÔNG CỤ TÌM KIẾM & LỌC -->
            <div class="order-filter-toolbar">
              <div class="input-with-icon search-box">
                <svg class="icon-prefix" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input
                  type="text"
                  v-model="orderTableQuery"
                  placeholder="Tìm mã đơn, khách hàng, SĐT..."
                  class="form-control input-has-prefix"
                  @input="orderPagination.page = 0"
                />
              </div>

              <div class="toolbar-select-group">
                <select v-model="orderStatusFilter" class="form-control select-combobox" @change="orderPagination.page = 0">
                  <option value="">Tất cả trạng thái</option>
                  <option value="5">Đã Hoàn Thành</option>
                  <option value="1">Chờ Xác Nhận</option>
                  <option value="2">Đã Xác Nhận</option>
                  <option value="3">Chờ Vận Chuyển</option>
                  <option value="4">Vận Chuyển</option>
                  <option value="6">Hủy</option>
                </select>

                <select v-model="orderPaymentFilter" class="form-control select-combobox" @change="orderPagination.page = 0">
                  <option value="">Tất cả thanh toán</option>
                  <option value="1">Đã thanh toán</option>
                  <option value="0">Chưa thanh toán</option>
                </select>

                <select
                  v-if="selectedKpiType === 'payment'"
                  v-model="orderMethodFilter"
                  class="form-control select-combobox"
                  @change="orderPagination.page = 0"
                >
                  <option value="">Tất cả phương thức</option>
                  <option value="cash">Tiền mặt</option>
                  <option value="cod">Thanh toán khi nhận hàng (COD)</option>
                  <option value="transfer">Chuyển khoản / VNPAY</option>
                  <option value="combined">Kết hợp (Tiền mặt + CK)</option>
                </select>

                <select v-model="orderSortBy" class="form-control select-combobox">
                  <option value="newest">Mới nhất</option>
                  <option value="oldest">Cũ nhất</option>
                  <option value="highest_val">Giá trị cao nhất</option>
                  <option value="lowest_val">Giá trị thấp nhất</option>
                </select>

                <button type="button" class="btn-refresh" @click="resetOrderTableFilters" title="Đặt lại bộ lọc">
                  ↻ Đặt lại
                </button>
              </div>
            </div>

            <!-- BẢNG DỮ LIỆU ĐỒNG BỘ 100% VỚI DANH SÁCH HÓA ĐƠN VÀ ẢNH 7 -->
            <div class="table-responsive">
              <table class="custom-table">
                <thead>
                  <tr>
                    <th class="th-stt">STT</th>
                    <th class="th-code">Mã Đơn Hàng</th>
                    <th class="th-time">Thời Gian Tạo</th>
                    <th class="th-customer">Khách Hàng</th>
                    <th class="th-employee">Nhân Viên</th>
                    <th class="th-type">Loại Đơn</th>
                    <th class="th-payment-method">Hình Thức TT</th>
                    <th class="th-status">Trạng Thái</th>
                    <th class="th-payment">Thanh Toán</th>
                    <th class="th-price">Thành Tiền</th>
                    <th class="th-action text-center">Thao Tác</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="filteredOrdersList.length === 0">
                    <td colspan="11" class="text-center text-empty">
                      Không có dữ liệu đơn hàng nào phù hợp với mục này.
                    </td>
                  </tr>
                  <tr
                    v-for="(ord, index) in paginatedOrders"
                    :key="ord.id || index"
                    class="table-row"
                  >
                    <td class="text-muted text-center">
                      {{ (orderPagination.page * orderPagination.size) + index + 1 }}
                    </td>
                    <td class="font-bold text-code">
                      <span class="code-link" @click="viewInvoiceDetail(ord.maHoaDon)">{{ ord.maHoaDon }}</span>
                    </td>
                    <td class="td-time">
                      <div class="date-main">{{ formatOrderDateTime(ord.ngayTao).date }}</div>
                      <div class="time-sub">{{ formatOrderDateTime(ord.ngayTao).time }}</div>
                    </td>
                    <td class="text-customer">
                      <div class="cust-name font-medium">{{ ord.tenKhachHang || 'Khách lẻ' }}</div>
                      <div v-if="ord.soDienThoaiKhachHang" class="time-sub">{{ ord.soDienThoaiKhachHang }}</div>
                    </td>
                    <td class="td-employee">
                      <span class="emp-chip">{{ ord.tenNhanVien || '—' }}</span>
                    </td>
                    <td class="td-type">
                      <span :class="Number(ord.loaiDon) === 2 ? 'badge-online' : 'badge-store'">
                        <span v-if="Number(ord.loaiDon) === 2">🌐 Online</span>
                        <span v-else>⌂ Tại quầy</span>
                      </span>
                    </td>
                    <td class="td-payment-method">
                      <span class="payment-method-text">{{ ord.hinhThucThanhToan || (Number(ord.loaiDon) === 1 ? 'Tiền mặt' : 'Thanh toán khi nhận hàng (COD)') }}</span>
                    </td>
                    <td class="td-status">
                      <div class="status-cell-wrap">
                        <span class="badge-status" :class="getStatusClass(ord.trangThai)">
                          <span class="status-dot"></span>
                          {{ getStatusText(ord.trangThai) }}
                        </span>
                      </div>
                    </td>
                    <td class="td-payment">
                      <span class="badge-payment" :class="getPaymentClass(ord.trangThaiThanhToan)">
                        <span class="payment-dot"></span>
                        {{ getPaymentText(ord.trangThaiThanhToan) }}
                      </span>
                    </td>
                    <td class="font-bold text-price">
                      {{ formatCurrency(ord.tienSauGiamGia != null ? ord.tienSauGiamGia : ord.tongTien) }}
                    </td>
                    <td class="text-center">
                      <button
                        class="btn-action-view"
                        title="Xem chi tiết đơn hàng"
                        @click="viewInvoiceDetail(ord.maHoaDon)"
                      >
                        <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- CHÂN BẢNG: PHÂN TRANG ĐỒNG BỘ CHUẨN 3 KHỐI NHƯ BẢNG SẢN PHẨM & ẢNH 7 -->
            <div v-if="filteredOrdersList.length > 0" class="order-table-footer">
              <div class="footer-left-count">
                Hiển thị <strong>{{ filteredOrdersList.length ? ((orderPagination.page * orderPagination.size) + 1) : 0 }} - {{ Math.min((orderPagination.page + 1) * orderPagination.size, filteredOrdersList.length) }}</strong> trong tổng số <strong>{{ filteredOrdersList.length }}</strong> đơn hàng
                <span v-if="orderChannelFilter !== 'all'" class="filter-indicator-text">
                  (Đang lọc <strong>{{ orderChannelFilter === 'store' ? 'Tại quầy' : 'Online' }}</strong>)
                </span>
              </div>

              <div class="footer-center-pagination">
                <button
                  type="button"
                  class="pg-btn"
                  :disabled="orderPagination.page === 0"
                  @click="goToOrderPage(orderPagination.page - 1)"
                >
                  ‹
                </button>
                <button
                  v-for="pNo in visibleOrderPageNumbers"
                  :key="pNo"
                  type="button"
                  class="pg-btn"
                  :class="{ active: pNo === orderPagination.page }"
                  @click="goToOrderPage(pNo)"
                >
                  {{ pNo + 1 }}
                </button>
                <button
                  type="button"
                  class="pg-btn"
                  :disabled="orderPagination.page >= orderTotalPages - 1"
                  @click="goToOrderPage(orderPagination.page + 1)"
                >
                  ›
                </button>
              </div>

              <div class="footer-right-size">
                <div class="select-wrapper-minimal size-select">
                  <select v-model.number="orderPagination.size" class="form-control-minimal" @change="orderPagination.page = 0">
                    <option :value="5">Hiển thị 5 bản ghi / trang</option>
                    <option :value="10">Hiển thị 10 bản ghi / trang</option>
                    <option :value="20">Hiển thị 20 bản ghi / trang</option>
                    <option :value="50">Hiển thị 50 bản ghi / trang</option>
                  </select>
                </div>
              </div>
            </div>
          </template>

          <!-- TRƯỜNG HỢP 2: THẺ 6 - SẢN PHẨM ĐÃ BÁN -->
          <template v-else>
            <!-- HEADER BẢNG SẢN PHẨM -->
            <div class="table-card-header">
              <div class="table-title-area">
                <svg class="icon-section" viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                  <line x1="12" y1="22.08" x2="12" y2="12" />
                </svg>
                <div class="table-heading-wrap">
                  <div class="table-heading-line">
                    <h3 class="card-heading">Danh sách sản phẩm đã bán trong kỳ</h3>
                    <span class="kpi-table-badge badge-products">
                      🏷 Sản phẩm đã bán
                    </span>
                  </div>
                  <span class="table-total-count">({{ filteredProductsList.length }} sản phẩm)</span>
                </div>
              </div>

              <div class="table-card-header-actions">
                <!-- Nút Thu gọn bảng -->
                <button
                  type="button"
                  class="btn-collapse-order-table"
                  @click="closeKpiTable"
                  title="Thu gọn bảng sản phẩm"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                  <span>Thu gọn</span>
                </button>
              </div>
            </div>

            <!-- TOOLBAR TÌM KIẾM SẢN PHẨM -->
            <div class="product-stats-toolbar inline-toolbar">
              <div class="product-stats-actions">
                <div class="input-with-prefix-icon search-product-box">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                  </svg>
                  <input
                    type="text"
                    v-model="productTableQuery"
                    placeholder="Tìm theo mã, tên sản phẩm..."
                    class="form-control-minimal with-icon"
                    @input="onProductSearchChange"
                  />
                </div>

                <div class="select-wrapper-minimal">
                  <select v-model="productSortBy" class="form-control-minimal" @change="sortProductTable">
                    <option value="best_seller">Lượt bán cao nhất</option>
                    <option value="slow_seller">Bán chậm nhất</option>
                    <option value="highest_revenue">Doanh thu cao nhất</option>
                    <option value="highest_stock">Tồn kho nhiều nhất</option>
                    <option value="highest_return">Số lượng trả nhiều nhất</option>
                  </select>
                </div>

                <button type="button" class="btn-reset-round small" @click="resetProductTableFilters" title="Đặt lại bộ lọc sản phẩm">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 4v6h-6" />
                    <path d="M1 20v-6h6" />
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                  </svg>
                  <span>Đặt lại</span>
                </button>
              </div>
            </div>

            <!-- BẢNG DỮ LIỆU SẢN PHẨM 8 CỘT -->
            <div class="table-responsive-clean">
              <table class="minimal-styled-table">
                <thead>
                  <tr class="header-brown-beige">
                    <th style="width: 60px; text-align: center">STT</th>
                    <th style="width: 130px">MÃ SẢN PHẨM</th>
                    <th style="min-width: 260px">TÊN SẢN PHẨM</th>
                    <th style="width: 140px">THƯƠNG HIỆU</th>
                    <th style="width: 100px; text-align: center">ĐÃ BÁN</th>
                    <th style="width: 120px; text-align: center">SỐ LƯỢNG TRẢ</th>
                    <th style="width: 170px; text-align: right; white-space: nowrap">DOANH THU</th>
                    <th style="width: 110px; text-align: center">TỒN KHO</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="paginatedProducts.length === 0">
                    <td colspan="8" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                      Không có sản phẩm nào phù hợp.
                    </td>
                  </tr>
                  <tr v-for="(p, index) in paginatedProducts" :key="p.id || index" class="table-data-row">
                    <td style="text-align: center" class="text-muted-stt">
                      {{ (productPagination.page * productPagination.size) + index + 1 }}
                    </td>
                    <td class="font-bold text-dark code-cell">{{ p.code }}</td>
                    <td>
                      <div class="product-name-with-thumb">
                        <img
                          :src="p.image || defaultProductImage"
                          class="product-thumb-img"
                          alt="product"
                          @error="(e) => e.target.src = defaultProductImage"
                        />
                        <div class="product-info-text">
                          <strong class="product-title-bold">{{ p.name }}</strong>
                          <span v-if="p.sizes" class="product-size-hint">Size: {{ p.sizes }}</span>
                        </div>
                      </div>
                    </td>
                    <td class="brand-name-cell">
                      <span class="brand-badge-tag">{{ p.brand || '—' }}</span>
                    </td>
                    <td style="text-align: center" class="font-bold text-dark">
                      {{ (p.soldCount || 0).toLocaleString('vi-VN') }}
                    </td>
                    <td style="text-align: center" class="text-red-return font-bold">
                      {{ (p.returnedCount || 0).toLocaleString('vi-VN') }}
                    </td>
                    <td style="text-align: right; white-space: nowrap" class="font-bold text-black-bold">
                      {{ formatCurrency(p.revenue) }}
                    </td>
                    <td style="text-align: center" class="text-green-stock font-bold">
                      {{ (p.stock || 0).toLocaleString('vi-VN') }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- PHÂN TRANG SẢN PHẨM -->
            <div v-if="filteredProductsList.length > 0" class="product-table-footer">
              <div class="footer-left-count">
                Hiển thị <strong>{{ filteredProductsList.length ? ((productPagination.page * productPagination.size) + 1) : 0 }} - {{ Math.min((productPagination.page + 1) * productPagination.size, filteredProductsList.length) }}</strong> trong tổng số <strong>{{ filteredProductsList.length }}</strong> sản phẩm
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
          </template>
        </div>
      </transition>

      <!-- 4. Biểu đồ Cột Sản phẩm bán được (Full Width - Tỷ lệ 1:1, font chuẩn sắc nét không bẹp) -->
      <div v-if="selectedKpiType === null" class="content-card full-chart-card">
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
      <div v-if="selectedKpiType === null" class="two-donuts-grid">
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
      <div v-if="selectedKpiType === null" class="content-card product-stats-card">
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
                <option value="best_seller">Lượt bán cao nhất</option>
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
                <th style="width: 170px; text-align: right; white-space: nowrap">DOANH THU</th>
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
                <td style="text-align: right; white-space: nowrap" class="font-bold text-black-bold">
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
            <span>Gửi mail tổng kết</span>
          </button>

          <!-- Xuất Excel so sánh -->
          <button
            type="button"
            class="pill-btn btn-export-excel"
            @click="handleExportComparisonExcel"
            title="Xuất dữ liệu so sánh ra file Excel"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span>Xuất Excel</span>
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
              Theo Tháng
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'quarter' }"
              @click="setCompareMode('quarter')"
            >
              Theo Quý
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'year' }"
              @click="setCompareMode('year')"
            >
              Theo Năm
            </button>
            <button
              type="button"
              class="pill-btn"
              :class="{ active: compareFilter.mode === 'custom' }"
              @click="setCompareMode('custom')"
            >
              Tùy chọn khoảng ngày
            </button>
          </div>
        </div>

        <!-- Hàng 2: Bộ lọc phụ (Thương hiệu & Sản phẩm & Đặt lại) -->
        <div class="filter-fields-row comparison-fields-row">
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

        <!-- Hàng 3: Danh sách các kỳ so sánh đa kỳ động (Dynamic Multi-period) -->
        <div class="dynamic-periods-section">
          <div class="dynamic-periods-header">
            <div class="periods-heading-left">
              <span class="periods-badge-count">{{ comparePeriods.length }} kỳ đối chiếu</span>
              <span class="periods-hint-txt">So sánh đồng thời nhiều mốc thời gian để theo dõi tốc độ tăng trưởng kinh doanh</span>
            </div>
            <!-- Nút Thêm kỳ so sánh -->
            <button
              type="button"
              class="btn-add-period-highlight"
              @click="addComparePeriod"
              title="Thêm một kỳ mới vào danh sách so sánh"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>Thêm kỳ so sánh</span>
            </button>
          </div>

          <!-- Danh sách các dòng chọn kỳ -->
          <div class="period-cards-grid">
            <div
              v-for="(p, pIdx) in comparePeriods"
              :key="p.id || pIdx"
              class="period-selector-row"
            >
              <div class="period-row-tag">
                <span class="tag-letter">Kỳ {{ pIdx + 1 }}</span>
              </div>

              <!-- Nội dung chọn theo chế độ -->
              <div class="period-row-inputs">
                <!-- Chế độ Theo Tháng -->
                <template v-if="compareFilter.mode === 'month'">
                  <div class="mini-field">
                    <span class="mini-label">Tháng:</span>
                    <select v-model.number="p.month" class="form-control-minimal small select-period" @change="onPeriodMonthChange(p)">
                      <option
                        v-for="m in 12"
                        :key="m"
                        :value="m"
                        :disabled="p.year === currentYear && m > currentMonth"
                      >
                        Tháng {{ m }}
                      </option>
                    </select>
                  </div>
                  <div class="mini-field">
                    <span class="mini-label">Năm:</span>
                    <select v-model.number="p.year" class="form-control-minimal small select-period" @change="onPeriodYearChange(p)">
                      <option v-for="y in availableYears" :key="y" :value="y">{{ y }}</option>
                    </select>
                  </div>
                </template>

                <!-- Chế độ Theo Quý -->
                <template v-else-if="compareFilter.mode === 'quarter'">
                  <div class="mini-field">
                    <span class="mini-label">Quý:</span>
                    <select v-model.number="p.quarter" class="form-control-minimal small select-period" @change="onPeriodQuarterChange(p)">
                      <option :value="1" :disabled="p.year === currentYear && 1 > currentQuarter">Quý 1 (T1 - T3)</option>
                      <option :value="2" :disabled="p.year === currentYear && 2 > currentQuarter">Quý 2 (T4 - T6)</option>
                      <option :value="3" :disabled="p.year === currentYear && 3 > currentQuarter">Quý 3 (T7 - T9)</option>
                      <option :value="4" :disabled="p.year === currentYear && 4 > currentQuarter">Quý 4 (T10 - T12)</option>
                    </select>
                  </div>
                  <div class="mini-field">
                    <span class="mini-label">Năm:</span>
                    <select v-model.number="p.year" class="form-control-minimal small select-period" @change="onPeriodYearChange(p)">
                      <option v-for="y in availableYears" :key="y" :value="y">{{ y }}</option>
                    </select>
                  </div>
                </template>

                <!-- Chế độ Theo Năm -->
                <template v-else-if="compareFilter.mode === 'year'">
                  <div class="mini-field">
                    <span class="mini-label">Năm:</span>
                    <select v-model.number="p.year" class="form-control-minimal small select-period" @change="onPeriodYearChange(p)">
                      <option v-for="y in availableYears" :key="y" :value="y">Năm {{ y }}</option>
                    </select>
                  </div>
                </template>

                <!-- Chế độ Tùy chọn khoảng ngày -->
                <template v-else-if="compareFilter.mode === 'custom'">
                  <div class="mini-field flex-grow-name">
                    <input
                      type="text"
                      v-model="p.name"
                      placeholder="Tên kỳ..."
                      class="form-control-minimal small input-custom-name"
                      @change="fetchComparisonData"
                    />
                  </div>
                  <div class="mini-field">
                    <span class="mini-label">Từ:</span>
                    <input
                      type="date"
                      v-model="p.fromDate"
                      :max="p.toDate ? (p.toDate < todayStr ? p.toDate : todayStr) : todayStr"
                      class="form-control-minimal small input-period-date"
                      @change="onCustomPeriodDateChange(p)"
                    />
                  </div>
                  <div class="mini-field">
                    <span class="mini-label">Đến:</span>
                    <input
                      type="date"
                      v-model="p.toDate"
                      :max="todayStr"
                      :min="p.fromDate || undefined"
                      class="form-control-minimal small input-period-date"
                      @change="onCustomPeriodDateChange(p)"
                    />
                  </div>
                </template>
              </div>

              <!-- Nút Xóa dòng kỳ -->
              <button
                type="button"
                class="btn-delete-period-row"
                :disabled="comparePeriods.length <= 2"
                :title="comparePeriods.length <= 2 ? 'Cần tối thiểu 2 kỳ để so sánh' : 'Xóa kỳ này khỏi danh sách'"
                @click="removeComparePeriod(pIdx)"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                <span>Xóa</span>
              </button>
            </div>
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
      <!-- Có trục tung bên trái (VNĐ) và trục tung bên phải (%) rõ ràng, chống đè chữ -->
      <div class="content-card combo-chart-card" @mouseleave="onComboBarMouseLeave">
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
            <div class="legend-item"><span class="legend-line-dot"></span> Tăng trưởng (%)</div>
          </div>
        </div>

        <!-- Khung vẽ SVG Combo Chart với tỷ lệ 1:1, cuộn ngang khi > 8 kỳ -->
        <div class="combo-chart-scroll-wrap">
          <div class="combo-chart-svg-wrap" :style="{ width: comboSvgWidthStyle, minWidth: '100%' }">
            <svg class="combo-chart-svg" :viewBox="`0 0 ${comboSvgWidth} 360`" preserveAspectRatio="xMidYMid meet">
              <!-- Nhãn đơn vị trục tung trái & phải -->
              <text :x="comboChartData.xLeftAxis - 6" y="38" text-anchor="end" class="y-axis-unit-label">(VNĐ)</text>
              <text :x="comboChartData.xRightAxis + 8" y="38" text-anchor="start" class="y-axis-unit-label right">(%)</text>

              <!-- Các đường lưới ngang & Nhãn trục tung trái (VNĐ) -->
              <g v-for="(tick, tIdx) in comboChartData.yTicksLeft" :key="'yt-left-' + tIdx">
                <!-- Đường lưới mờ -->
                <line
                  :x1="comboChartData.xLeftAxis"
                  :y1="tick.y"
                  :x2="comboChartData.xRightAxis"
                  :y2="tick.y"
                  :stroke="tick.y === comboChartData.yBase ? '#cbd5e1' : '#e5e7eb'"
                  :stroke-width="tick.y === comboChartData.yBase ? 1.5 : 1"
                  :stroke-dasharray="tick.y === comboChartData.yBase ? 'none' : '3 3'"
                />
                <!-- Nhãn trục trái (VNĐ) -->
                <text
                  :x="comboChartData.xLeftAxis - 8"
                  :y="tick.y + 4"
                  text-anchor="end"
                  class="y-axis-tick-label left"
                >
                  {{ tick.label }}
                </text>
              </g>

              <!-- Nhãn trục phải (%) & Đường gióng mờ riêng biệt ở tầng trên -->
              <g v-for="(rtick, rtIdx) in comboChartData.yTicksRight" :key="'yt-right-' + rtIdx">
                <line
                  :x1="comboChartData.xLeftAxis"
                  :y1="rtick.y"
                  :x2="comboChartData.xRightAxis"
                  :y2="rtick.y"
                  stroke="#ffe4e6"
                  stroke-width="0.8"
                  stroke-dasharray="2 3"
                />
                <text
                  :x="comboChartData.xRightAxis + 8"
                  :y="rtick.y + 4"
                  text-anchor="start"
                  class="y-axis-tick-label right"
                >
                  {{ rtick.label }}
                </text>
              </g>

              <!-- Trục dọc bên trái (Doanh thu) -->
              <line
                :x1="comboChartData.xLeftAxis"
                :y1="comboChartData.yLeftTop"
                :x2="comboChartData.xLeftAxis"
                :y2="comboChartData.yBase"
                stroke="#cbd5e1"
                stroke-width="1.2"
              />
              <!-- Trục dọc bên phải (Tăng trưởng %) -->
              <line
                :x1="comboChartData.xRightAxis"
                :y1="comboChartData.yRightTop"
                :x2="comboChartData.xRightAxis"
                :y2="comboChartData.yBase"
                stroke="#fca5a5"
                stroke-width="1.2"
              />

              <!-- Cột Doanh thu (Stacked Bar: Tiền mặt ở dưới, Chuyển khoản ở trên) -->
              <g
                v-for="(item, idx) in comboChartData.bars"
                :key="'bar-' + idx"
                class="combo-bar-group"
                @mouseenter="onComboBarHover(item, $event)"
                @mousemove="onComboBarHover(item, $event)"
              >
                <!-- Hit area trong suốt bao phủ toàn bộ cột -->
                <rect
                  :x="item.x - 8"
                  :y="comboChartData.yLeftTop - 10"
                  :width="item.width + 16"
                  :height="comboChartData.barChartHeight + 10"
                  fill="transparent"
                  class="combo-hit-area"
                />

                <!-- Vệt highlight khi hover -->
                <rect
                  v-if="hoveredComboBar && hoveredComboBar.name === item.name"
                  :x="item.x - 4"
                  :y="comboChartData.yLeftTop - 6"
                  :width="item.width + 8"
                  :height="comboChartData.barChartHeight + 6"
                  fill="#f1ede7"
                  opacity="0.5"
                  rx="6"
                />

                <!-- Cột Tiền mặt -->
                <rect
                  :x="item.x"
                  :y="item.yCash"
                  :width="item.width"
                  :height="item.heightCash"
                  fill="#3B82F6"
                  opacity="0.9"
                  :rx="item.rxCash"
                  class="combo-bar-segment"
                />

                <!-- Cột Chuyển khoản (Xếp trên tiền mặt) -->
                <rect
                  v-if="item.heightTransfer > 0"
                  :x="item.x"
                  :y="item.yTransfer"
                  :width="item.width"
                  :height="item.heightTransfer"
                  fill="#10B981"
                  opacity="0.92"
                  :rx="item.rxTransfer"
                  class="combo-bar-segment"
                />

                <!-- Con số Tổng doanh thu: Hiển thị chữ đậm màu tối ngay phía trên đỉnh cột 8px -->
                <text
                  v-if="item.totalRevenue > 0"
                  :x="item.centerX"
                  :y="item.yTransfer - 8"
                  text-anchor="middle"
                  class="combo-bar-total-label"
                >
                  {{ formatShortCurrency(item.totalRevenue) }}
                </text>
                <text
                  v-else
                  :x="item.centerX"
                  :y="item.yTransfer - 8"
                  text-anchor="middle"
                  class="combo-bar-total-label zero"
                >
                  0 đ
                </text>

                <!-- Nhãn trục hoành X (xoay nghiêng -35deg nếu nhiều kỳ để không bao giờ đè chữ) -->
                <g v-if="item.isRotated" :transform="`translate(${item.centerX}, 285) rotate(-35)`">
                  <text
                    x="0"
                    y="0"
                    text-anchor="end"
                    class="combo-bottom-label rotated"
                    :class="{ 'label-highlight': hoveredComboBar && hoveredComboBar.name === item.name }"
                  >
                    {{ item.displayName }}
                  </text>
                </g>
                <text
                  v-else
                  :x="item.centerX"
                  y="295"
                  text-anchor="middle"
                  class="combo-bottom-label"
                  :class="{ 'label-highlight': hoveredComboBar && hoveredComboBar.name === item.name }"
                >
                  {{ item.displayName }}
                </text>
              </g>

              <!-- Đường Line biểu diễn % Tăng trưởng ở thang đo trục phải -->
              <polyline
                v-if="comboChartData.linePoints"
                :points="comboChartData.linePoints"
                fill="none"
                stroke="#E11D48"
                stroke-width="2.6"
                stroke-linecap="round"
                stroke-linejoin="round"
              />

              <!-- Các điểm mốc tròn và Badge % Tăng trưởng bo tròn cách điểm tròn 20px -->
              <g v-for="(pt, idx) in comboChartData.points" :key="'pt-' + idx" class="growth-point-group">
                <!-- Khung Badge Bo Tròn riêng biệt phía trên điểm tròn 20px -->
                <g :transform="`translate(${pt.x}, ${Math.max(22, pt.y - 20)})`">
                  <rect
                    :x="-26"
                    :y="-10"
                    width="52"
                    height="20"
                    rx="10"
                    :fill="pt.growthRate > 0 ? '#dcfce7' : (pt.growthRate < 0 ? '#fee2e2' : '#f1f5f9')"
                    :stroke="pt.growthRate > 0 ? '#16a34a' : (pt.growthRate < 0 ? '#dc2626' : '#cbd5e1')"
                    stroke-width="1.2"
                  />
                  <text
                    x="0"
                    y="4"
                    text-anchor="middle"
                    font-size="10.5"
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

        <!-- Tooltip nổi tương tác cho Combo Chart -->
        <div
          v-if="hoveredComboBar"
          class="bar-chart-floating-tooltip"
          :style="{ left: comboTooltipPos.x + 'px', top: comboTooltipPos.y + 'px' }"
        >
          <div class="tooltip-header-clean">
            <span class="tooltip-cal-badge">📊</span>
            <strong class="tooltip-date-txt">{{ hoveredComboBar.displayName }}</strong>
          </div>
          <div v-if="hoveredComboBar.dateRangeText" class="tooltip-sub-range">
            {{ hoveredComboBar.dateRangeText }}
          </div>
          <div class="tooltip-divider-line"></div>
          <div class="tooltip-data-row">
            <span class="tooltip-row-lbl">💰 Tổng doanh thu:</span>
            <strong class="tooltip-row-val font-bold text-dark">{{ formatCurrency(hoveredComboBar.totalRevenue) }}</strong>
          </div>
          <div class="tooltip-data-row">
            <span class="tooltip-row-lbl">💵 Tiền mặt:</span>
            <span class="tooltip-row-val text-blue-amount font-semibold">{{ formatCurrency(hoveredComboBar.cash) }}</span>
          </div>
          <div class="tooltip-data-row">
            <span class="tooltip-row-lbl">💳 Chuyển khoản:</span>
            <span class="tooltip-row-val text-emerald font-semibold">{{ formatCurrency(hoveredComboBar.transfer) }}</span>
          </div>
          <div class="tooltip-data-row">
            <span class="tooltip-row-lbl">📈 Tăng trưởng:</span>
            <span
              class="tooltip-row-val font-bold"
              :class="hoveredComboBar.growthRate >= 0 ? 'text-emerald' : 'text-red-return'"
            >
              {{ hoveredComboBar.growthRate > 0 ? '+' : '' }}{{ hoveredComboBar.growthRate }}%
            </span>
          </div>
          <div class="tooltip-data-row">
            <span class="tooltip-row-lbl">🧾 Đơn hoàn thành:</span>
            <span class="tooltip-row-val font-bold">{{ hoveredComboBar.orderCount }} đơn</span>
          </div>
        </div>
      </div>

      <!-- Bảng đối chiếu doanh thu chi tiết các kỳ (Có Search & Filter & Sort) -->
      <div class="content-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="title-sub-icon">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
            </svg>
            <h3 class="card-heading">Bảng đối chiếu doanh thu chi tiết các kỳ</h3>
            <span class="badge-tag-peak" style="background:#e0f2fe; color:#0369a1;">
              {{ filteredComparisonPeriods.length }} kỳ hiển thị
            </span>
          </div>
          <span class="badge-unit">Đơn vị: VNĐ</span>
        </div>

        <!-- Toolbar tìm kiếm và lọc cho Bảng 1 -->
        <div class="table-sub-toolbar">
          <div class="input-with-prefix-icon search-sub-box">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              v-model="periodSearchQuery"
              placeholder="Tìm tên kỳ, khoảng ngày..."
              class="form-control-minimal with-icon small"
            />
          </div>

          <div class="select-wrapper-minimal">
            <select v-model="periodGrowthFilter" class="form-control-minimal small">
              <option value="all">Tất cả hiệu suất</option>
              <option value="positive">Tăng trưởng dương (↑)</option>
              <option value="negative">Tăng trưởng âm (↓)</option>
              <option value="extreme">Kỳ cao nhất / Thấp nhất</option>
            </select>
          </div>

          <div class="select-wrapper-minimal">
            <select v-model="periodSortBy" class="form-control-minimal small">
              <option value="default">Thứ tự các kỳ</option>
              <option value="revenue_desc">Doanh thu cao nhất</option>
              <option value="revenue_asc">Doanh thu thấp nhất</option>
              <option value="growth_desc">Tăng trưởng cao nhất</option>
              <option value="growth_asc">Tăng trưởng thấp nhất</option>
              <option value="orders_desc">Số đơn nhiều nhất</option>
            </select>
          </div>

          <button type="button" class="btn-reset-round small" @click="resetPeriodTableFilters" title="Đặt lại bộ lọc bảng">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Đặt lại</span>
          </button>
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
              <tr v-if="!filteredComparisonPeriods || filteredComparisonPeriods.length === 0">
                <td colspan="10" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                  Không có kỳ nào phù hợp với bộ lọc tìm kiếm.
                </td>
              </tr>
              <tr v-for="p in filteredComparisonPeriods" :key="p.periodKey" class="table-data-row">
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
            <tfoot v-if="filteredComparisonPeriods && filteredComparisonPeriods.length > 0">
              <tr class="table-total-row">
                <td colspan="2" class="font-bold text-dark">TỔNG CỘNG (KỲ ĐANG LỌC)</td>
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

      <!-- Bảng Top sản phẩm theo lượt bán đối chiếu sức mua qua các kỳ -->
      <div class="content-card">
        <div class="card-title-row">
          <div class="card-title-group">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="title-sub-icon">
              <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <path d="M16 10a4 4 0 0 1-8 0" />
            </svg>
            <h3 class="card-heading">Top sản phẩm theo lượt bán & Đối chiếu sức mua qua các kỳ</h3>
            <span class="badge-tag-peak" style="background:#e0f2fe; color:#0369a1;">
              {{ filteredComparisonProducts.length }} sản phẩm
            </span>
          </div>
          <span class="badge-unit">Top mặt hàng nổi bật</span>
        </div>

        <!-- Toolbar tìm kiếm và lọc cho Bảng 2 -->
        <div class="table-sub-toolbar">
          <div class="input-with-prefix-icon search-sub-box">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              v-model="compareProductSearch"
              placeholder="Tìm mã SP, tên sản phẩm, thương hiệu..."
              class="form-control-minimal with-icon small"
            />
          </div>

          <div class="select-wrapper-minimal">
            <select v-model="compareProductBrand" class="form-control-minimal small">
              <option value="">Tất cả thương hiệu</option>
              <option v-for="b in brandOptions" :key="b.id" :value="b.ten || b.tenThuongHieu">
                {{ b.ten || b.tenThuongHieu }}
              </option>
            </select>
          </div>

          <div class="select-wrapper-minimal">
            <select v-model="compareProductTrend" class="form-control-minimal small">
              <option value="all">Tất cả xu hướng</option>
              <option value="up">Xu hướng tăng (↑)</option>
              <option value="down">Xu hướng giảm (↓)</option>
              <option value="flat">Ổn định</option>
            </select>
          </div>

          <div class="select-wrapper-minimal">
            <select v-model="compareProductSort" class="form-control-minimal small">
              <option value="sold_desc">Tổng lượt bán cao → thấp</option>
              <option value="sold_asc">Bán chậm nhất</option>
              <option value="revenue_desc">Doanh thu cao nhất</option>
              <option value="trend_desc">Tăng trưởng mạnh nhất</option>
            </select>
          </div>

          <button type="button" class="btn-reset-round small" @click="resetCompareProductTableFilters" title="Đặt lại bộ lọc bảng sản phẩm">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Đặt lại</span>
          </button>
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
              <tr v-if="!filteredComparisonProducts || filteredComparisonProducts.length === 0">
                <td :colspan="7 + (comparisonData.periods?.length || 0)" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                  Không tìm thấy sản phẩm phù hợp với bộ lọc tìm kiếm.
                </td>
              </tr>
              <tr v-for="(sp, idx) in filteredComparisonProducts" :key="sp.productId" class="table-data-row">
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
                :class="{ active: emailForm.reportType === 'day' }"
                @click="emailForm.reportType = 'day'"
              >
                Báo cáo Hôm nay (Ngày)
              </button>
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
          <div v-if="emailForm.reportType !== 'day'" class="form-row-2col">
            <div v-if="emailForm.reportType === 'month'" class="form-group-custom">
              <label class="form-label-custom">Chọn Tháng</label>
              <div class="select-wrapper-minimal">
                <select v-model.number="emailForm.month" class="form-control-minimal" @change="onEmailMonthChange">
                  <option
                    v-for="m in 12"
                    :key="m"
                    :value="m"
                    :disabled="emailForm.year === currentYear && m > currentMonth"
                  >
                    Tháng {{ m }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-group-custom">
              <label class="form-label-custom">Chọn Năm</label>
              <div class="select-wrapper-minimal">
                <select v-model.number="emailForm.year" class="form-control-minimal" @change="onEmailYearChange">
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
import { useRouter } from 'vue-router'
import api from '@/api'
import * as XLSX from 'xlsx'
import { showToast } from '@/utils/dialog.js'

const router = useRouter()

const defaultProductImage = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=150&auto=format&fit=crop&q=80'

// ==========================================
// THỐNG KÊ ĐƠN HÀNG (DANH SÁCH ĐƠN HÀNG VỚI BỘ LỌC TẠI QUẦY & ONLINE)
// ==========================================
const selectedKpiType = ref(null) // null | 'actual_revenue' | 'expected_revenue' | 'payment' | 'total_orders' | 'products'
const showOrderTable = computed(() => selectedKpiType.value !== null)

const allInvoiceStats = ref([])
const orderChannelFilter = ref('all') // 'all' | 'store' | 'online'
const orderTableQuery = ref('')
const orderStatusFilter = ref('')
const orderPaymentFilter = ref('')
const orderMethodFilter = ref('') // '' | 'cash' | 'transfer'
const orderSortBy = ref('newest')
const orderPagination = reactive({ page: 0, size: 5 })

const kpiTableTitle = computed(() => {
  switch (selectedKpiType.value) {
    case 'actual_revenue':
      return 'Danh sách đơn hàng Doanh thu thực tế'
    case 'expected_revenue':
      return 'Danh sách đơn hàng Doanh thu dự kiến'
    case 'payment':
      return 'Danh sách đơn hàng theo phương thức thanh toán (Tiền mặt & Chuyển khoản)'
    case 'cash':
      return 'Danh sách đơn hàng thanh toán Tiền mặt (Thực thu)'
    case 'transfer':
      return 'Danh sách đơn hàng thanh toán Chuyển khoản (Thực thu)'
    case 'total_orders':
      return 'Danh sách tất cả đơn hàng'
    case 'products':
      return 'Danh sách sản phẩm đã bán trong kỳ'
    default:
      return 'Danh sách đơn hàng'
  }
})

const kpiTableBadge = computed(() => {
  switch (selectedKpiType.value) {
    case 'actual_revenue':
      return { text: 'Đã hoàn thành / Đã thanh toán', class: 'badge-actual' }
    case 'expected_revenue':
      return { text: 'Chờ xác nhận / Giao COD', class: 'badge-expected' }
    case 'payment':
      return { text: 'Tiền mặt & Chuyển khoản', class: 'badge-payment' }
    case 'cash':
      return { text: 'Tiền mặt', class: 'badge-cash' }
    case 'transfer':
      return { text: 'Chuyển khoản / VNPAY / QR', class: 'badge-transfer' }
    case 'total_orders':
      return { text: 'Toàn bộ đơn hàng', class: 'badge-all' }
    case 'products':
      return { text: 'Sản phẩm đã bán', class: 'badge-products' }
    default:
      return { text: '', class: '' }
  }
})

const currentKpiOrdersBaseList = computed(() => {
  let list = [...allInvoiceStats.value]
  if (selectedKpiType.value === 'actual_revenue') {
    list = list.filter(item => {
      const st = Number(item.trangThai)
      const pt = Number(item.trangThaiThanhToan)
      return (st === 5 || pt === 1) && st !== 6
    })
  } else if (selectedKpiType.value === 'expected_revenue') {
    list = list.filter(item => {
      const st = Number(item.trangThai)
      const pt = Number(item.trangThaiThanhToan)
      return st !== 5 && st !== 6 && pt !== 1
    })
  } else if (selectedKpiType.value === 'payment') {
    list = list.filter(item => {
      const st = Number(item.trangThai)
      const pt = Number(item.trangThaiThanhToan)
      return (st === 5 || pt === 1) && st !== 6
    })
  } else if (selectedKpiType.value === 'cash') {
    list = list.filter(item => {
      const st = Number(item.trangThai)
      const pt = Number(item.trangThaiThanhToan)
      const isValid = (st === 5 || pt === 1) && st !== 6
      const method = String(item.hinhThucThanhToan || '').toLowerCase()
      const isCash = method.includes('tiền mặt') || method.includes('kết hợp') || method.includes('cod') || method.includes('nhận hàng') || (!method && Number(item.loaiDon) === 1)
      return isValid && isCash
    })
  } else if (selectedKpiType.value === 'transfer') {
    list = list.filter(item => {
      const st = Number(item.trangThai)
      const pt = Number(item.trangThaiThanhToan)
      const isValid = (st === 5 || pt === 1) && st !== 6
      const method = String(item.hinhThucThanhToan || '').toLowerCase()
      const isTransfer = (method.includes('chuyển khoản') || method.includes('kết hợp') || method.includes('vnpay') || method.includes('qr')) && !method.includes('cod') && !method.includes('nhận hàng')
      return isValid && isTransfer
    })
  }
  return list
})

const currentKpiOrdersTotal = computed(() => currentKpiOrdersBaseList.value.length)
const currentKpiStoreCount = computed(() => currentKpiOrdersBaseList.value.filter(i => Number(i.loaiDon) === 1).length)
const currentKpiOnlineCount = computed(() => currentKpiOrdersBaseList.value.filter(i => Number(i.loaiDon) === 2).length)

const totalOrdersCount = computed(() => allInvoiceStats.value.length)
const storeOrdersCount = computed(() => allInvoiceStats.value.filter(i => Number(i.loaiDon) === 1).length)
const onlineOrdersCount = computed(() => allInvoiceStats.value.filter(i => Number(i.loaiDon) === 2).length)

const filteredOrdersList = computed(() => {
  let list = [...currentKpiOrdersBaseList.value]

  // 1. Lọc theo Kênh bán: Tại quầy vs Online
  if (orderChannelFilter.value === 'store') {
    list = list.filter(item => Number(item.loaiDon) === 1)
  } else if (orderChannelFilter.value === 'online') {
    list = list.filter(item => Number(item.loaiDon) === 2)
  }

  // 2. Lọc theo Trạng thái đơn hàng
  if (orderStatusFilter.value !== '') {
    list = list.filter(item => Number(item.trangThai) === Number(orderStatusFilter.value))
  }

  // 3. Lọc theo Trạng thái thanh toán
  if (orderPaymentFilter.value !== '') {
    list = list.filter(item => Number(item.trangThaiThanhToan) === Number(orderPaymentFilter.value))
  }

  // 3b. Lọc theo Phương thức thanh toán (Tiền mặt / Chuyển khoản / COD / Kết hợp) khi xem thẻ payment
  if (selectedKpiType.value === 'payment' && orderMethodFilter.value) {
    if (orderMethodFilter.value === 'cash') {
      list = list.filter(item => {
        const method = String(item.hinhThucThanhToan || '').toLowerCase()
        return (method.includes('tiền mặt') && !method.includes('cod') && !method.includes('nhận hàng')) || (!method && Number(item.loaiDon) === 1)
      })
    } else if (orderMethodFilter.value === 'cod') {
      list = list.filter(item => {
        const method = String(item.hinhThucThanhToan || '').toLowerCase()
        return method.includes('cod') || method.includes('nhận hàng') || (!method && Number(item.loaiDon) === 2)
      })
    } else if (orderMethodFilter.value === 'transfer') {
      list = list.filter(item => {
        const method = String(item.hinhThucThanhToan || '').toLowerCase()
        return (method.includes('chuyển khoản') || method.includes('vnpay') || method.includes('qr')) && !method.includes('cod') && !method.includes('nhận hàng')
      })
    } else if (orderMethodFilter.value === 'combined') {
      list = list.filter(item => {
        const method = String(item.hinhThucThanhToan || '').toLowerCase()
        return method.includes('kết hợp')
      })
    }
  }

  // 4. Tìm kiếm từ khóa (Mã đơn, Khách hàng, SĐT, Nhân viên)
  const q = orderTableQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(item => {
      const ma = String(item.maHoaDon || '').toLowerCase()
      const kh = String(item.tenKhachHang || '').toLowerCase()
      const sdt = String(item.soDienThoaiKhachHang || '').toLowerCase()
      const nv = String(item.tenNhanVien || '').toLowerCase()
      return ma.includes(q) || kh.includes(q) || sdt.includes(q) || nv.includes(q)
    })
  }

  // 5. Sắp xếp
  list.sort((a, b) => {
    if (orderSortBy.value === 'newest') {
      const timeA = a.ngayTao ? new Date(a.ngayTao).getTime() : 0
      const timeB = b.ngayTao ? new Date(b.ngayTao).getTime() : 0
      return timeB - timeA
    } else if (orderSortBy.value === 'oldest') {
      const timeA = a.ngayTao ? new Date(a.ngayTao).getTime() : 0
      const timeB = b.ngayTao ? new Date(b.ngayTao).getTime() : 0
      return timeA - timeB
    } else if (orderSortBy.value === 'highest_val') {
      const valA = Number(a.tienSauGiamGia != null ? a.tienSauGiamGia : a.tongTien || 0)
      const valB = Number(b.tienSauGiamGia != null ? b.tienSauGiamGia : b.tongTien || 0)
      return valB - valA
    } else if (orderSortBy.value === 'lowest_val') {
      const valA = Number(a.tienSauGiamGia != null ? a.tienSauGiamGia : a.tongTien || 0)
      const valB = Number(b.tienSauGiamGia != null ? b.tienSauGiamGia : b.tongTien || 0)
      return valA - valB
    }
    return 0
  })

  return list
})

const orderTotalPages = computed(() => {
  const total = filteredOrdersList.value.length
  return Math.ceil(total / orderPagination.size) || 1
})

const paginatedOrders = computed(() => {
  const start = orderPagination.page * orderPagination.size
  return filteredOrdersList.value.slice(start, start + orderPagination.size)
})

const visibleOrderPageNumbers = computed(() => {
  const pages = []
  const maxButtons = 5
  let start = Math.max(0, orderPagination.page - Math.floor(maxButtons / 2))
  let end = Math.min(orderTotalPages.value, start + maxButtons)
  if (end - start < maxButtons) {
    start = Math.max(0, end - maxButtons)
  }
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  return pages
})

function goToOrderPage(pageIndex) {
  if (pageIndex >= 0 && pageIndex < orderTotalPages.value) {
    orderPagination.page = pageIndex
  }
}

function setOrderChannelFilter(channel) {
  orderChannelFilter.value = channel
  orderPagination.page = 0
}

function resetOrderTableFilters() {
  orderChannelFilter.value = 'all'
  orderTableQuery.value = ''
  orderStatusFilter.value = ''
  orderPaymentFilter.value = ''
  orderMethodFilter.value = ''
  orderSortBy.value = 'newest'
  orderPagination.page = 0
}

function toggleKpiTable(type) {
  if (selectedKpiType.value === type) {
    selectedKpiType.value = null
  } else {
    selectedKpiType.value = type
    if (type !== 'payment') {
      orderMethodFilter.value = ''
    }
    orderPagination.page = 0
    productPagination.page = 0
    setTimeout(() => {
      const el = document.getElementById('order-stats-section')
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
      }
    }, 100)
  }
}

function closeKpiTable() {
  selectedKpiType.value = null
}

function toggleOrderTable(channel = null) {
  if (channel && typeof channel === 'string' && channel !== 'toggle') {
    orderChannelFilter.value = channel
    selectedKpiType.value = 'total_orders'
  } else {
    selectedKpiType.value = (selectedKpiType.value === 'total_orders') ? null : 'total_orders'
  }
  orderPagination.page = 0
  if (selectedKpiType.value) {
    setTimeout(() => {
      const el = document.getElementById('order-stats-section')
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
      }
    }, 100)
  }
}

function scrollToOrderTable(channel = 'all') {
  orderChannelFilter.value = channel
  orderPagination.page = 0
  selectedKpiType.value = 'total_orders'
  setTimeout(() => {
    const el = document.getElementById('order-stats-section')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, 100)
}

function viewInvoiceDetail(maHoaDon) {
  if (!maHoaDon) return
  router.push(`/hoa-don/${encodeURIComponent(maHoaDon)}`)
}

function formatOrderDateTime(dt) {
  if (!dt) return { date: '-', time: '-' }
  const d = new Date(dt)
  return {
    date: d.toLocaleDateString('vi-VN'),
    time: d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }
}

function getStatusText(status) {
  const statusMap = {
    1: 'Chờ Xác Nhận',
    2: 'Đã Xác Nhận',
    3: 'Chờ Vận Chuyển',
    4: 'Vận Chuyển',
    5: 'Đã Hoàn Thành',
    6: 'Hủy',
    7: 'Giao Hàng Thất Bại'
  }
  return statusMap[Number(status)] || 'Chưa cập nhật'
}

function getStatusClass(status) {
  switch (Number(status)) {
    case 1: return 'status-pending'
    case 2: return 'status-confirmed'
    case 3: return 'status-waiting'
    case 4: return 'status-shipping'
    case 5: return 'status-completed'
    case 6: return 'status-cancelled'
    case 7: return 'status-failed'
    default: return 'status-default'
  }
}

function getPaymentText(status) {
  return Number(status) === 1 ? 'Đã thanh toán' : 'Chưa thanh toán'
}

function getPaymentClass(status) {
  return Number(status) === 1 ? 'payment-paid' : 'payment-unpaid'
}

// 0. Quản lý Chế độ xem: 'overview' (Thống kê chính) hoặc 'comparison' (So sánh & Tăng trưởng)
const viewMode = ref('overview')

function switchViewMode(mode) {
  viewMode.value = mode
  if (mode === 'comparison') {
    if (comparePeriods.value.length === 0) {
      initPeriodsForMode(compareFilter.mode)
    }
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

const todayStr = computed(() => getTodayDateString())
const maxFromDate = computed(() => {
  if (filters.toDate && filters.toDate < todayStr.value) {
    return filters.toDate
  }
  return todayStr.value
})

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
  const today = todayStr.value

  if (type === 'all') {
    filters.fromDate = '2024-01-01'
    filters.toDate = today
  } else if (type === 'today') {
    filters.fromDate = today
    filters.toDate = today
  } else if (type === '7days') {
    const past7 = new Date()
    past7.setDate(now.getDate() - 6)
    const y = past7.getFullYear()
    const m = String(past7.getMonth() + 1).padStart(2, '0')
    const d = String(past7.getDate()).padStart(2, '0')
    filters.fromDate = `${y}-${m}-${d}`
    filters.toDate = today
  } else if (type === 'thisMonth') {
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1)
    const y = firstDay.getFullYear()
    const m = String(firstDay.getMonth() + 1).padStart(2, '0')
    const d = String(firstDay.getDate()).padStart(2, '0')
    filters.fromDate = `${y}-${m}-${d}`
    filters.toDate = today
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
    filters.toDate = today
  }

  fetchOverviewData()
}

function onCustomDateChange() {
  const today = todayStr.value
  if (filters.fromDate && filters.fromDate > today) {
    filters.fromDate = today
  }
  if (filters.toDate && filters.toDate > today) {
    filters.toDate = today
  }
  if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
    filters.fromDate = filters.toDate
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
  expectedRevenue: 0,
  expectedOrders: 0,
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
        kpiData.totalRevenue = Number(data.kpi.totalRevenue || data.kpi.actualRevenue || 0)
        kpiData.expectedRevenue = Number(data.kpi.expectedRevenue || 0)
        kpiData.expectedOrders = Number(data.kpi.expectedOrders || 0)
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

      if (Array.isArray(data.invoices)) {
        allInvoiceStats.value = data.invoices
      } else {
        allInvoiceStats.value = []
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
      ['Doanh thu thực tế', kpiData.totalRevenue],
      ['Doanh thu dự kiến', kpiData.expectedRevenue],
      ['Tiền mặt (Thực thu)', kpiData.cashRevenue],
      ['Chuyển khoản (Thực thu)', kpiData.transferRevenue],
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

    // Sheet Đơn hàng
    const orderHeader = ['STT', 'Mã đơn hàng', 'Thời gian tạo', 'Khách hàng', 'Số điện thoại', 'Nhân viên', 'Kênh bán', 'Hình thức TT', 'Trạng thái đơn hàng', 'Trạng thái thanh toán', 'Tổng tiền (đ)', 'Thành tiền (đ)']
    const orderRows = [orderHeader]

    filteredOrdersList.value.forEach((ord, idx) => {
      orderRows.push([
        idx + 1,
        ord.maHoaDon || '',
        ord.ngayTao || '',
        ord.tenKhachHang || 'Khách lẻ',
        ord.soDienThoaiKhachHang || '',
        ord.tenNhanVien || '',
        Number(ord.loaiDon) === 2 ? 'Online' : 'Tại quầy',
        ord.hinhThucThanhToan || '',
        getOrderStatusText(ord.trangThai),
        Number(ord.trangThaiThanhToan) === 1 ? 'Đã thanh toán' : 'Chưa thanh toán',
        ord.tongTien || 0,
        ord.tienSauGiamGia != null ? ord.tienSauGiamGia : ord.tongTien || 0
      ])
    })
    const wsOrders = XLSX.utils.aoa_to_sheet(orderRows)
    XLSX.utils.book_append_sheet(wb, wsOrders, 'DanhSachDonHang')

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
const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1
const currentQuarter = Math.ceil(currentMonth / 3)
const availableYears = [currentYear - 4, currentYear - 3, currentYear - 2, currentYear - 1, currentYear]

const compareFilter = reactive({
  mode: 'month', // 'month', 'quarter', 'year', 'custom'
  year: currentYear,
  startYear: currentYear - 3,
  endYear: currentYear,
  brandId: '',
  productKeyword: ''
})

// Danh sách các kỳ so sánh đa kỳ động
const comparePeriods = ref([])

function onPeriodYearChange(p) {
  if (p.year >= currentYear) {
    p.year = currentYear
    if (p.month && p.month > currentMonth) {
      p.month = currentMonth
    }
    if (p.quarter && p.quarter > currentQuarter) {
      p.quarter = currentQuarter
    }
  }
  fetchComparisonData()
}

function onPeriodMonthChange(p) {
  if (p.year === currentYear && p.month > currentMonth) {
    p.month = currentMonth
  }
  fetchComparisonData()
}

function onPeriodQuarterChange(p) {
  if (p.year === currentYear && p.quarter > currentQuarter) {
    p.quarter = currentQuarter
  }
  fetchComparisonData()
}

function onCustomPeriodDateChange(p) {
  const today = todayStr.value
  if (p.fromDate && p.fromDate > today) {
    p.fromDate = today
  }
  if (p.toDate && p.toDate > today) {
    p.toDate = today
  }
  if (p.fromDate && p.toDate && p.fromDate > p.toDate) {
    p.fromDate = p.toDate
  }
  fetchComparisonData()
}

function initPeriodsForMode(mode) {
  const y = currentYear
  const m = currentMonth

  if (mode === 'month') {
    const prevM = m === 1 ? 12 : m - 1
    const prevY = m === 1 ? y - 1 : y
    comparePeriods.value = [
      { id: 1, month: prevM, year: prevY },
      { id: 2, month: m, year: y }
    ]
  } else if (mode === 'quarter') {
    const prevQ = currentQuarter === 1 ? 4 : currentQuarter - 1
    const prevQYear = currentQuarter === 1 ? y - 1 : y
    comparePeriods.value = [
      { id: 1, quarter: prevQ, year: prevQYear },
      { id: 2, quarter: currentQuarter, year: y }
    ]
  } else if (mode === 'year') {
    comparePeriods.value = [
      { id: 1, year: y - 1 },
      { id: 2, year: y }
    ]
  } else if (mode === 'custom') {
    const past30 = new Date()
    past30.setDate(now.getDate() - 30)
    const past60 = new Date()
    past60.setDate(now.getDate() - 60)
    const fmt = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    comparePeriods.value = [
      { id: 1, name: 'Kỳ A (Kỳ trước)', fromDate: fmt(past60), toDate: fmt(past30) },
      { id: 2, name: 'Kỳ B (Kỳ này)', fromDate: fmt(past30), toDate: todayStr.value }
    ]
  }
}

function addComparePeriod() {
  const mode = compareFilter.mode
  const nextId = Date.now()

  if (mode === 'month') {
    // Tìm kỳ sớm nhất hiện có để lùi tiếp về quá khứ (không bao giờ vượt tới tương lai)
    let earliest = comparePeriods.value[0] || { month: currentMonth, year: currentYear }
    for (const p of comparePeriods.value) {
      if (p.year < earliest.year || (p.year === earliest.year && p.month < earliest.month)) {
        earliest = p
      }
    }
    let newM = earliest.month - 1
    let newY = earliest.year
    if (newM < 1) {
      newM = 12
      newY -= 1
    }
    comparePeriods.value.unshift({ id: nextId, month: newM, year: newY })
  } else if (mode === 'quarter') {
    let earliest = comparePeriods.value[0] || { quarter: currentQuarter, year: currentYear }
    for (const p of comparePeriods.value) {
      if (p.year < earliest.year || (p.year === earliest.year && p.quarter < earliest.quarter)) {
        earliest = p
      }
    }
    let newQ = earliest.quarter - 1
    let newY = earliest.year
    if (newQ < 1) {
      newQ = 4
      newY -= 1
    }
    comparePeriods.value.unshift({ id: nextId, quarter: newQ, year: newY })
  } else if (mode === 'year') {
    let earliest = comparePeriods.value[0] || { year: currentYear }
    for (const p of comparePeriods.value) {
      if (p.year < earliest.year) {
        earliest = p
      }
    }
    const newY = earliest.year - 1
    comparePeriods.value.unshift({ id: nextId, year: newY })
  } else if (mode === 'custom') {
    let earliestFrom = comparePeriods.value[0]?.fromDate || todayStr.value
    for (const p of comparePeriods.value) {
      if (p.fromDate && p.fromDate < earliestFrom) {
        earliestFrom = p.fromDate
      }
    }
    const dEnd = new Date(earliestFrom)
    dEnd.setDate(dEnd.getDate() - 1)
    const dStart = new Date(dEnd)
    dStart.setDate(dStart.getDate() - 30)
    const fmt = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    comparePeriods.value.unshift({
      id: nextId,
      name: `Kỳ ${comparePeriods.value.length + 1}`,
      fromDate: fmt(dStart),
      toDate: fmt(dEnd)
    })
  }
  fetchComparisonData()
}

function removeComparePeriod(index) {
  if (comparePeriods.value.length <= 2) {
    showToast('Cần tối thiểu 2 kỳ để thực hiện so sánh đối chiếu!', 'warning')
    return
  }
  comparePeriods.value.splice(index, 1)
  fetchComparisonData()
}

function buildPeriodsPayload() {
  const mode = compareFilter.mode
  return comparePeriods.value.map((p, idx) => {
    if (mode === 'month') {
      const monthStr = String(p.month).padStart(2, '0')
      const start = `${p.year}-${monthStr}-01`
      const lastDay = new Date(p.year, p.month, 0).getDate()
      const end = `${p.year}-${monthStr}-${String(lastDay).padStart(2, '0')}`
      return {
        key: `T${p.month}_${p.year}`,
        name: `Tháng ${p.month}/${p.year}`,
        startDate: start,
        endDate: end
      }
    } else if (mode === 'quarter') {
      const startMonth = (p.quarter - 1) * 3 + 1
      const endMonth = p.quarter * 3
      const start = `${p.year}-${String(startMonth).padStart(2, '0')}-01`
      const lastDay = new Date(p.year, endMonth, 0).getDate()
      const end = `${p.year}-${String(endMonth).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
      return {
        key: `Q${p.quarter}_${p.year}`,
        name: `Quý ${p.quarter}/${p.year}`,
        startDate: start,
        endDate: end
      }
    } else if (mode === 'year') {
      return {
        key: `${p.year}`,
        name: `Năm ${p.year}`,
        startDate: `${p.year}-01-01`,
        endDate: `${p.year}-12-31`
      }
    } else {
      return {
        key: `Custom_${idx + 1}`,
        name: p.name && p.name.trim() ? p.name.trim() : `Kỳ ${idx + 1}`,
        startDate: p.fromDate || getTodayDateString(),
        endDate: p.toDate || getTodayDateString()
      }
    }
  })
}

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
  initPeriodsForMode(mode)
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
  initPeriodsForMode('month')
  fetchComparisonData()
  showToast('Đã đặt lại bộ lọc so sánh!', 'info')
}

async function fetchComparisonData() {
  loadingComparison.value = true
  try {
    const periodsPayload = buildPeriodsPayload()
    const params = {
      compareMode: compareFilter.mode,
      periodsJson: JSON.stringify(periodsPayload),
      year: compareFilter.year,
      startYear: compareFilter.startYear,
      endYear: compareFilter.endYear
    }
    if (compareFilter.brandId) params.brandId = compareFilter.brandId
    if (compareFilter.productKeyword && compareFilter.productKeyword.trim()) {
      params.productKeyword = compareFilter.productKeyword.trim()
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

// =========================================================================
// BIỂU ĐỒ COMBO CHART: CỘT DOANH THU & ĐƯỜNG TĂNG TRƯỞNG % (2 TRỤC TUNG)
// =========================================================================
const hoveredComboBar = ref(null)
const comboTooltipPos = reactive({ x: 0, y: 0 })

function onComboBarHover(bar, event) {
  hoveredComboBar.value = bar
  const container = document.querySelector('.combo-chart-card')
  if (!container) return
  const rect = container.getBoundingClientRect()
  const clientX = event.clientX - rect.left
  const clientY = event.clientY - rect.top
  comboTooltipPos.x = Math.max(120, Math.min(rect.width - 160, clientX))
  comboTooltipPos.y = Math.max(60, clientY - 80)
}

function onComboBarMouseLeave() {
  hoveredComboBar.value = null
}

const comboSvgWidth = computed(() => {
  const count = comparisonData.value.periods?.length || 0
  if (count <= 6) return 1000
  return Math.max(1000, 160 + count * 90)
})

const comboSvgWidthStyle = computed(() => {
  const count = comparisonData.value.periods?.length || 0
  if (count <= 6) return '100%'
  return `${comboSvgWidth.value}px`
})

const comboChartData = computed(() => {
  const periods = comparisonData.value.periods || []
  const count = periods.length
  const xLeftAxis = 75
  const xRightAxis = comboSvgWidth.value - 65
  const yBase = 270

  // 1. Phân vùng Bar Chart (Doanh thu - Trục trái VNĐ):
  // Chiều cao cột tối đa 110px. Đỉnh cột cao nhất = 270 - 110 = 160px.
  // Nhãn doanh thu trên đỉnh cột: y = item.yTransfer - 8. Đỉnh nhãn = 152px.
  const barChartHeight = 110
  const yLeftTop = yBase - barChartHeight // 160px

  // 2. Phân vùng Line Chart (Tăng trưởng % - Trục phải %):
  // Nằm ở tầng trên (yLineTop: 50px đến yLineBottom: 115px, lineHeight: 65px).
  // Điểm tròn đỏ thấp nhất là 115px, cách đỉnh nhãn doanh thu cao nhất (152px) tới 37px!
  // Đảm bảo đường kẻ và điểm tròn đỏ tuyệt đối không bao giờ chạm hay che khuất chữ số doanh thu.
  const yLineTop = 50
  const yLineBottom = 115
  const lineHeight = yLineBottom - yLineTop // 65px

  if (count === 0) {
    return {
      bars: [],
      points: [],
      linePoints: '',
      xLeftAxis,
      xRightAxis,
      yTop: yLineTop,
      yLeftTop,
      yRightTop: yLineTop,
      yBase,
      barChartHeight,
      chartHeight: barChartHeight,
      yTicksLeft: [],
      yTicksRight: []
    }
  }

  // 1. Trục tung bên trái (VNĐ): Doanh thu
  const rawMaxRev = Math.max(...periods.map(p => Number(p.totalRevenue || 0)), 1000000)
  const magnitude = Math.pow(10, Math.floor(Math.log10(rawMaxRev)))
  const maxRev = Math.ceil(rawMaxRev / (magnitude / 2)) * (magnitude / 2)

  const yTicksLeft = [0, 0.25, 0.5, 0.75, 1.0].map(frac => ({
    y: yBase - frac * barChartHeight,
    label: formatShortCurrency(frac * maxRev)
  }))

  // 2. Trục tung bên phải (%): Tốc độ tăng trưởng
  const gRates = periods.map(p => Number(p.growthRate || 0))
  let minRate = Math.min(0, ...gRates)
  let maxRate = Math.max(50, ...gRates)
  minRate = Math.floor(minRate / 25) * 25
  maxRate = Math.ceil(maxRate / 25) * 25
  if (maxRate <= minRate) maxRate = minRate + 50
  const rateRange = maxRate - minRate

  const yTicksRight = [0, 0.25, 0.5, 0.75, 1.0].map(frac => {
    const v = Math.round(minRate + frac * rateRange)
    return {
      y: yLineBottom - frac * lineHeight,
      label: `${v > 0 ? '+' : ''}${v}%`
    }
  })

  // 3. Phân bổ vị trí các cột cân đối ở khu vực trung tâm khung vẽ (không bị dạt sát ra 2 mép trục tung)
  const plotW = xRightAxis - xLeftAxis
  let barWidth = 44
  const centers = []

  if (count === 1) {
    centers.push(xLeftAxis + plotW / 2)
  } else if (count === 2) {
    barWidth = 46
    const spacing = 220
    centers.push(xLeftAxis + plotW / 2 - spacing / 2)
    centers.push(xLeftAxis + plotW / 2 + spacing / 2)
  } else if (count === 3) {
    barWidth = 46
    const spacing = 200
    centers.push(xLeftAxis + plotW / 2 - spacing)
    centers.push(xLeftAxis + plotW / 2)
    centers.push(xLeftAxis + plotW / 2 + spacing)
  } else if (count === 4) {
    barWidth = 44
    const spacing = 170
    const startX = xLeftAxis + plotW / 2 - 1.5 * spacing
    for (let i = 0; i < count; i++) {
      centers.push(startX + i * spacing)
    }
  } else {
    // count >= 5: chia đều theo các slot tương ứng
    const slotW = plotW / count
    barWidth = Math.min(44, Math.max(24, Math.floor(slotW * 0.48)))
    for (let i = 0; i < count; i++) {
      centers.push(xLeftAxis + (i + 0.5) * slotW)
    }
  }

  const isRotated = count > 6
  const bars = []
  const points = []

  periods.forEach((p, idx) => {
    const total = Number(p.totalRevenue || 0)
    const cash = Number(p.cashRevenue || 0)
    const transfer = Number(p.transferRevenue || 0)

    const totalH = Math.max((total / maxRev) * barChartHeight, total > 0 ? 8 : 2)
    const cashH = total > 0 ? (cash / total) * totalH : 0
    const transferH = total > 0 ? totalH - cashH : 0

    const centerX = centers[idx]
    const x = centerX - barWidth / 2
    const yTransfer = yBase - totalH
    const yCash = yBase - cashH

    bars.push({
      name: p.periodKey,
      displayName: p.periodName || p.periodKey,
      dateRangeText: p.dateRangeText || '',
      totalRevenue: total,
      totalH,
      cash,
      transfer,
      orderCount: Number(p.orderCount || 0),
      growthRate: Number(p.growthRate || 0),
      x,
      centerX,
      width: barWidth,
      yTransfer,
      heightTransfer: transferH,
      yCash,
      heightCash: cashH,
      rxTransfer: 6,
      rxCash: transferH > 0 ? 0 : 6,
      isRotated
    })

    // Điểm biểu diễn tốc độ tăng trưởng % (thang đo trục phải ở tầng trên)
    const g = Number(p.growthRate || 0)
    const clampedG = Math.max(minRate, Math.min(maxRate, g))
    const yPoint = yLineBottom - ((clampedG - minRate) / rateRange) * lineHeight

    points.push({
      x: centerX,
      y: yPoint,
      growthRate: g
    })
  })

  const linePoints = points.map(pt => `${pt.x},${pt.y}`).join(' ')

  return {
    bars,
    points,
    linePoints,
    xLeftAxis,
    xRightAxis,
    yTop: yLineTop,
    yLeftTop,
    yRightTop: yLineTop,
    yBase,
    barChartHeight,
    chartHeight: barChartHeight,
    yTicksLeft,
    yTicksRight
  }
})

// =========================================================================
// BẢNG 1: ĐỐI CHIẾU DOANH THU CHI TIẾT CÁC KỲ (SEARCH & FILTER & SORT)
// =========================================================================
const periodSearchQuery = ref('')
const periodGrowthFilter = ref('all') // 'all', 'positive', 'negative', 'extreme'
const periodSortBy = ref('default') // 'default', 'revenue_desc', 'revenue_asc', 'growth_desc', 'growth_asc', 'orders_desc'

function resetPeriodTableFilters() {
  periodSearchQuery.value = ''
  periodGrowthFilter.value = 'all'
  periodSortBy.value = 'default'
  showToast('Đã đặt lại bộ lọc bảng kỳ so sánh!', 'info')
}

const filteredComparisonPeriods = computed(() => {
  let list = [...(comparisonData.value.periods || [])]
  const q = periodSearchQuery.value.trim().toLowerCase()
  if (q) {
    list = list.filter(p =>
      (p.periodName && p.periodName.toLowerCase().includes(q)) ||
      (p.periodKey && p.periodKey.toLowerCase().includes(q)) ||
      (p.dateRangeText && p.dateRangeText.toLowerCase().includes(q))
    )
  }
  if (periodGrowthFilter.value === 'positive') {
    list = list.filter(p => Number(p.growthRate || 0) > 0)
  } else if (periodGrowthFilter.value === 'negative') {
    list = list.filter(p => Number(p.growthRate || 0) < 0)
  } else if (periodGrowthFilter.value === 'extreme') {
    if (list.length > 2) {
      const sortedByRev = [...list].sort((a, b) => Number(b.totalRevenue || 0) - Number(a.totalRevenue || 0))
      const maxP = sortedByRev[0]
      const minP = sortedByRev[sortedByRev.length - 1]
      list = list.filter(p => p.periodKey === maxP.periodKey || p.periodKey === minP.periodKey)
    }
  }

  if (periodSortBy.value === 'revenue_desc') {
    list.sort((a, b) => Number(b.totalRevenue || 0) - Number(a.totalRevenue || 0))
  } else if (periodSortBy.value === 'revenue_asc') {
    list.sort((a, b) => Number(a.totalRevenue || 0) - Number(b.totalRevenue || 0))
  } else if (periodSortBy.value === 'growth_desc') {
    list.sort((a, b) => Number(b.growthRate || 0) - Number(a.growthRate || 0))
  } else if (periodSortBy.value === 'growth_asc') {
    list.sort((a, b) => Number(a.growthRate || 0) - Number(b.growthRate || 0))
  } else if (periodSortBy.value === 'orders_desc') {
    list.sort((a, b) => Number(b.orderCount || 0) - Number(a.orderCount || 0))
  }

  return list
})

// =========================================================================
// BẢNG 2: TOP SẢN PHẨM & ĐỐI CHIẾU SỨC MUA (SEARCH & FILTER & SORT)
// =========================================================================
const compareProductSearch = ref('')
const compareProductBrand = ref('')
const compareProductTrend = ref('all') // 'all', 'up', 'down', 'flat'
const compareProductSort = ref('sold_desc') // 'sold_desc', 'sold_asc', 'revenue_desc', 'trend_desc'

function resetCompareProductTableFilters() {
  compareProductSearch.value = ''
  compareProductBrand.value = ''
  compareProductTrend.value = 'all'
  compareProductSort.value = 'sold_desc'
  showToast('Đã đặt lại bộ lọc bảng sản phẩm so sánh!', 'info')
}

const filteredComparisonProducts = computed(() => {
  let list = [...(comparisonData.value.topProducts || [])]
  const q = compareProductSearch.value.trim().toLowerCase()
  if (q) {
    list = list.filter(p =>
      (p.productCode && p.productCode.toLowerCase().includes(q)) ||
      (p.productName && p.productName.toLowerCase().includes(q)) ||
      (p.brandName && p.brandName.toLowerCase().includes(q))
    )
  }
  if (compareProductBrand.value) {
    list = list.filter(p => p.brandName === compareProductBrand.value)
  }
  if (compareProductTrend.value !== 'all') {
    list = list.filter(p => p.trend === compareProductTrend.value)
  }

  if (compareProductSort.value === 'sold_desc') {
    list.sort((a, b) => Number(b.totalSold || 0) - Number(a.totalSold || 0))
  } else if (compareProductSort.value === 'sold_asc') {
    list.sort((a, b) => Number(a.totalSold || 0) - Number(b.totalSold || 0))
  } else if (compareProductSort.value === 'revenue_desc') {
    list.sort((a, b) => Number(b.totalRevenue || 0) - Number(a.totalRevenue || 0))
  } else if (compareProductSort.value === 'trend_desc') {
    list.sort((a, b) => Number(b.growthTrend || 0) - Number(a.growthTrend || 0))
  }

  return list
})

// Xuất file Excel So sánh
async function handleExportComparisonExcel() {
  try {
    const periodsPayload = buildPeriodsPayload()
    const params = {
      compareMode: compareFilter.mode,
      periodsJson: JSON.stringify(periodsPayload),
      year: compareFilter.year,
      startYear: compareFilter.startYear,
      endYear: compareFilter.endYear
    }
    if (compareFilter.brandId) params.brandId = compareFilter.brandId
    if (compareFilter.productKeyword && compareFilter.productKeyword.trim()) {
      params.productKeyword = compareFilter.productKeyword.trim()
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
  reportType: 'day', // 'day', 'month', 'year'
  year: currentYear,
  month: currentMonth,
  recipientEmail: 'thuanminh2908200334.1@gmail.com'
})

function onEmailYearChange() {
  if (emailForm.year >= currentYear) {
    emailForm.year = currentYear
    if (emailForm.month > currentMonth) {
      emailForm.month = currentMonth
    }
  }
}

function onEmailMonthChange() {
  if (emailForm.year === currentYear && emailForm.month > currentMonth) {
    emailForm.month = currentMonth
  }
}

function openEmailModal() {
  emailForm.reportType = 'day'
  emailForm.year = currentYear
  emailForm.month = currentMonth
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
  initPeriodsForMode('month')
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
  width: 100%;
}

.prefix-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  pointer-events: none;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-control-minimal.with-icon,
select.form-control-minimal.with-icon,
input.form-control-minimal.with-icon {
  padding-left: 38px !important;
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

/* 5 KPI Cards */
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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
.pastel-amber { background-color: #fef3c7; color: #b45309; }
.pastel-blue { background-color: #e3f2fd; color: #1565c0; }
.pastel-emerald { background-color: #e0f2f1; color: #00796b; }
.pastel-purple { background-color: #f3e5f5; color: #7b1fa2; }
.pastel-pink { background-color: #fce4ec; color: #c2185b; }
.pastel-orange { background-color: #fff3e0; color: #e65100; }

.kpi-sub-hint {
  font-size: 0.73rem;
  font-weight: 500;
  color: #94a3b8;
  margin-top: 5px;
  line-height: 1.35;
}

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

.product-stats-toolbar.inline-toolbar {
  justify-content: flex-end;
  margin-top: 0.25rem;
}

.product-name-with-thumb,
.product-cell-flex {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb-img,
.product-thumb-small {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #ece6dc;
  background-color: #f7f5ef;
  flex-shrink: 0;
}

.product-info-text,
.product-info-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.product-title-bold,
.product-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.35;
}

.product-size-hint,
.product-sizes-text {
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
/* Dynamic Multi-period section */
.dynamic-periods-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #faf8f5;
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px dashed #d5cbc0;
}

.dynamic-periods-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.periods-heading-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.periods-badge-count {
  background-color: #e0f2fe;
  color: #0369a1;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 9999px;
}

.periods-hint-txt {
  font-size: 0.8rem;
  color: #64748b;
}

.btn-add-period-highlight {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid #16a34a;
  background-color: #ecfdf5;
  color: #15803d;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-add-period-highlight:hover {
  background-color: #16a34a;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(22, 163, 74, 0.25);
}

.period-cards-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.period-selector-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #ffffff;
  border: 1px solid #e2dbcf;
  border-radius: 10px;
  padding: 6px 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.period-row-tag {
  display: flex;
  align-items: center;
}

.tag-letter {
  background-color: #8c7355;
  color: #ffffff;
  font-size: 0.74rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  white-space: nowrap;
}

.period-row-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-field {
  display: flex;
  align-items: center;
  gap: 4px;
}

.mini-label {
  font-size: 0.76rem;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.form-control-minimal.small {
  padding: 4px 8px;
  font-size: 0.82rem;
  height: 32px;
  border-radius: 6px;
}

.select-period {
  min-width: 90px;
}

.input-period-date {
  width: 130px;
}

.input-custom-name {
  width: 120px;
}

.flex-grow-name {
  flex: 1;
}

.btn-delete-period-row {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #fecaca;
  background-color: #fef2f2;
  color: #dc2626;
  font-size: 0.74rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-delete-period-row:hover:not(:disabled) {
  background-color: #dc2626;
  color: #ffffff;
}

.btn-delete-period-row:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  border-color: #e2e8f0;
  background-color: #f1f5f9;
  color: #94a3b8;
}

/* Toolbars lọc & tìm kiếm trong bảng chi tiết */
.table-sub-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.search-sub-box {
  width: 240px;
}

.btn-reset-round.small {
  padding: 5px 12px;
  font-size: 0.8rem;
  height: 32px;
}

/* Biểu đồ Combo Chart */
.combo-chart-card {
  position: relative;
}

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

.combo-chart-scroll-wrap {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  position: relative;
  border-radius: 8px;
}

.combo-chart-scroll-wrap::-webkit-scrollbar {
  height: 6px;
}

.combo-chart-scroll-wrap::-webkit-scrollbar-track {
  background: #f1ede7;
  border-radius: 4px;
}

.combo-chart-scroll-wrap::-webkit-scrollbar-thumb {
  background: #d5cbc0;
  border-radius: 4px;
}

.combo-chart-svg-wrap {
  width: 100%;
  height: 360px;
  margin-top: 0.5rem;
}

.combo-chart-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.combo-hit-area {
  cursor: pointer;
}

.combo-bar-segment {
  transition: opacity 0.2s;
  cursor: pointer;
}

.combo-bar-segment:hover {
  opacity: 1;
}

.combo-bar-total-label {
  font-size: 12px;
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

.combo-bottom-label.rotated {
  font-size: 10.5px;
  white-space: nowrap;
}

.combo-bottom-label.label-highlight {
  fill: #1e293b;
  font-weight: 700;
}

.y-axis-unit-label {
  font-size: 11px;
  font-weight: 600;
  fill: #64748b;
  font-family: 'Inter', Roboto, sans-serif;
}

.y-axis-unit-label.right {
  fill: #e11d48;
}

.y-axis-tick-label {
  font-size: 10.5px;
  font-weight: 600;
  font-family: 'Inter', Roboto, sans-serif;
}

.y-axis-tick-label.left {
  fill: #64748b;
}

.y-axis-tick-label.right {
  fill: #e11d48;
}

.font-inter-badge {
  font-family: 'Inter', Roboto, sans-serif !important;
}

.tooltip-sub-range {
  font-size: 0.74rem;
  color: #94a3b8;
  margin: 2px 0 6px 0;
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
@media (max-width: 1440px) {
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

/* ========================================================================= */
/* CSS CHO BẢNG DANH SÁCH HÓA ĐƠN (ĐỒNG BỘ 100% VỚI MÀN DANH SÁCH HÓA ĐƠN)  */
/* ========================================================================= */
.order-stats-card.card-box {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  padding: 22px 26px;
  margin-top: 1.5rem;
  margin-bottom: 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.table-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-section {
  color: var(--blue, #496883);
}

.card-heading {
  font-size: 1rem;
  font-weight: 700;
  color: var(--blue, #496883);
  margin: 0;
}

.table-total-count {
  font-size: 0.85rem;
  color: #8c969e;
  font-weight: 500;
}

/* Cụm nút lọc kênh bán */
.order-channel-tabs {
  display: inline-flex;
  align-items: center;
  background: #f7f5ef;
  padding: 4px;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  gap: 4px;
}

.channel-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 0.84rem;
  font-weight: 600;
  color: #636b72;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.channel-tab-btn:hover:not(.active) {
  background: rgba(255, 255, 255, 0.7);
  color: #2b353b;
}

.channel-tab-btn.active {
  background: #ffffff;
  color: var(--blue, #496883);
  font-weight: 700;
  border-color: #d5d0c8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 9999px;
  background: #e9e5db;
  color: #496883;
}

.channel-tab-btn.active .tab-badge {
  background: var(--blue, #496883);
  color: #ffffff;
}

/* Thanh công cụ filter toolbar */
.order-filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.order-filter-toolbar .search-box {
  flex: 1 1 280px;
  min-width: 240px;
  max-width: 400px;
}

.toolbar-select-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.order-filter-toolbar .form-control {
  height: 36px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  background-color: #fcfbf8;
  color: var(--text, #3d4a50);
  font-size: 0.88rem;
  padding: 0 12px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.order-filter-toolbar .form-control:hover {
  border-color: #b8c3cc;
  background-color: #ffffff;
}

.order-filter-toolbar .form-control:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.order-filter-toolbar .input-has-prefix {
  padding-left: 36px !important;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.icon-prefix {
  position: absolute;
  left: 11px;
  color: #8c969e;
  pointer-events: none;
  z-index: 1;
}

.select-combobox {
  cursor: pointer;
  min-width: 155px;
}

.order-filter-toolbar .btn-refresh {
  height: 36px;
  padding: 0 14px;
  border: 1.5px solid #c8d6df;
  border-radius: 8px;
  background: #edf4f7;
  color: var(--blue, #496883);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.order-filter-toolbar .btn-refresh:hover {
  background: #dfeaf0;
  border-color: #9eb6c5;
}

/* Custom Table (chuẩn màn Danh sách hóa đơn) */
.table-responsive {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border-radius: 8px;
  background: #ffffff;
}

.custom-table {
  width: 100%;
  min-width: 1040px;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.88rem;
}

.custom-table th {
  background-color: #f7f5ef;
  text-transform: uppercase;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  color: var(--blue, #496883);
  font-weight: 700;
  padding: 13px 14px;
  white-space: nowrap;
  border-bottom: 2px solid #e9e5db;
}

.custom-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1e7dc;
  color: #40484f;
  vertical-align: middle;
}

.th-stt { width: 48px; text-align: center; }
.th-code { min-width: 130px; white-space: nowrap; }
.th-customer { min-width: 140px; }
.th-employee { min-width: 120px; }
.th-price { min-width: 115px; white-space: nowrap; }
.th-type { min-width: 120px; white-space: nowrap; }
.th-time { min-width: 130px; white-space: nowrap; }
.th-status { min-width: 140px; white-space: nowrap; }
.th-payment { min-width: 135px; white-space: nowrap; }
.th-action { width: 75px; text-align: center; white-space: nowrap; }

.td-type,
.td-time,
.td-status,
.td-payment {
  white-space: nowrap;
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
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.date-main {
  color: #3e474f;
  font-weight: 600;
  white-space: nowrap;
}

.time-sub {
  font-size: 0.75rem;
  color: #8c969e;
  margin-top: 2px;
  white-space: nowrap;
}

.text-center {
  text-align: center;
}

.text-empty {
  padding: 30px;
  color: #8c969e;
}

/* Badges đồng bộ 100% với Danh sách hóa đơn */
.badge-store,
.badge-online,
.badge-status,
.badge-payment {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 4px 12px !important;
  border-radius: 9999px !important;
  font-size: 0.76rem !important;
  font-weight: 600 !important;
  letter-spacing: 0.2px !important;
  white-space: nowrap !important;
  line-height: 1.25 !important;
  text-align: center !important;
  box-sizing: border-box !important;
  transition: all 0.2s ease;
}

.badge-store {
  background: #eaf2f6 !important;
  color: #38627d !important;
  border: 1px solid #c5d9e5 !important;
}

.badge-online {
  background: #eff6ff !important;
  color: #1d4ed8 !important;
  border: 1px solid #bfdbfe !important;
}

.status-cell-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.status-waiting {
  background: #f5edff !important;
  color: #8a3ee6 !important;
  border: 1px solid #e6d4fc !important;
}

.status-pending {
  background: #fff4e5 !important;
  color: #d97706 !important;
  border: 1px solid #fde0be !important;
}

.status-confirmed {
  background: #e0f2fe !important;
  color: #0369a1 !important;
  border: 1px solid #bfe5fd !important;
}

.status-shipping {
  background: #e0f7fa !important;
  color: #00838f !important;
  border: 1px solid #b5eff5 !important;
}

.status-completed {
  background: #e6f6ec !important;
  color: #1b7a37 !important;
  border: 1px solid #bce1c7 !important;
}

.status-cancelled {
  background: #fee6e6 !important;
  color: #be2626 !important;
  border: 1px solid #facdcd !important;
}

.status-failed {
  background: #fef2f2 !important;
  color: #dc2626 !important;
  border: 1px solid #fecaca !important;
}

.status-default {
  background: #f0e9df !important;
  color: #636b72 !important;
  border: 1px solid #e2dad0 !important;
}

.payment-paid {
  background: #eaf8ed !important;
  color: #227838 !important;
  border: 1px solid #bce1c5 !important;
}

.payment-unpaid {
  background: #fff6eb !important;
  color: #b75e11 !important;
  border: 1px solid #fad3ae !important;
}

.status-dot,
.payment-dot {
  display: none !important;
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

/* Pagination đồng bộ 100% với Danh sách hóa đơn */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 18px;
  margin-top: 8px;
  flex-wrap: wrap;
  gap: 12px;
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
  gap: 6px;
  align-items: center;
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

/* KPI Card Clickable & Toggle Bảng đơn hàng */
.kpi-card-clickable {
  cursor: pointer;
  transition: all 0.22s ease-in-out;
  position: relative;
  user-select: none;
}

.kpi-card-clickable:hover {
  transform: translateY(-2px);
  border-color: #8c7355 !important;
  box-shadow: 0 6px 18px rgba(140, 115, 85, 0.14);
}

.kpi-card-clickable.kpi-card-active {
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.kpi-card-clickable.kpi-card-active.theme-green { border-color: #16a34a !important; background: #f0fdf4; }
.kpi-card-clickable.kpi-card-active.theme-amber { border-color: #d97706 !important; background: #fffbeb; }
.kpi-card-clickable.kpi-card-active.theme-blue { border-color: #2563eb !important; background: #eff6ff; }
.kpi-card-clickable.kpi-card-active.theme-emerald { border-color: #059669 !important; background: #ecfdf5; }
.kpi-card-clickable.kpi-card-active.theme-purple { border-color: #7c3aed !important; background: #f5f3ff; }
.kpi-card-clickable.kpi-card-active.theme-orange { border-color: #ea580c !important; background: #fff7ed; }

.kpi-card-header-flex {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
}

.kpi-order-action-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.72rem;
  font-weight: 600;
  color: #7c3aed;
  background: #f5f3ff;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid #ddd6fe;
  transition: all 0.2s ease;
  line-height: 1;
  cursor: pointer;
}

.kpi-order-action-pill.pill-green {
  color: #16a34a;
  background: #ecfdf5;
  border-color: #a7f3d0;
}
.kpi-card-clickable:hover .pill-green,
.pill-green.is-open {
  background: #16a34a !important;
  color: #ffffff !important;
  border-color: #16a34a !important;
}

.kpi-order-action-pill.pill-amber {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}
.kpi-card-clickable:hover .pill-amber,
.pill-amber.is-open {
  background: #d97706 !important;
  color: #ffffff !important;
  border-color: #d97706 !important;
}

.kpi-order-action-pill.pill-blue {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}
.kpi-card-clickable:hover .pill-blue,
.pill-blue.is-open {
  background: #2563eb !important;
  color: #ffffff !important;
  border-color: #2563eb !important;
}

.kpi-order-action-pill.pill-emerald {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}
.kpi-card-clickable:hover .pill-emerald,
.pill-emerald.is-open {
  background: #059669 !important;
  color: #ffffff !important;
  border-color: #059669 !important;
}

.kpi-order-action-pill.pill-purple {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ddd6fe;
}
.kpi-card-clickable:hover .pill-purple,
.pill-purple.is-open {
  background: #7c3aed !important;
  color: #ffffff !important;
  border-color: #7c3aed !important;
}

.kpi-order-action-pill.pill-orange {
  color: #c2410c;
  background: #fff7ed;
  border-color: #fed7aa;
}
.kpi-card-clickable:hover .pill-orange,
.pill-orange.is-open {
  background: #ea580c !important;
  color: #ffffff !important;
  border-color: #ea580c !important;
}

.table-heading-wrap {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.table-heading-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.kpi-table-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}

.badge-actual { background: #ecfdf5; color: #16a34a; border: 1px solid #a7f3d0; }
.badge-expected { background: #fffbeb; color: #b45309; border: 1px solid #fde68a; }
.badge-payment { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.badge-cash { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.badge-transfer { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
.badge-all { background: #f5f3ff; color: #7c3aed; border: 1px solid #ddd6fe; }
.badge-products { background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; }

/* KPI Dual Values cho thẻ gộp Tiền mặt & Chuyển khoản */
.kpi-dual-values {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  min-height: 32px;
  margin-bottom: 0.35rem;
}

.dual-val-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
  line-height: 1.25;
}

.dual-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
}

.dual-num {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
}

.product-stats-toolbar.inline-toolbar {
  margin-bottom: 1rem;
  padding: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pill-arrow {
  transition: transform 0.2s ease;
}

.pill-arrow.arrow-up {
  transform: rotate(180deg);
}

/* Hiệu ứng trượt mượt cho Bảng danh sách đơn hàng */
.order-table-slide-enter-active,
.order-table-slide-leave-active {
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 1600px;
  opacity: 1;
  overflow: hidden;
}

.order-table-slide-enter-from,
.order-table-slide-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  border-width: 0 !important;
}

.table-card-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.btn-collapse-order-table {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #5f6b73;
  background: #f1ede7;
  border: 1px solid #e0d9cd;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-collapse-order-table:hover {
  background: #e2dbcf;
  color: #2b353b;
  border-color: #c9bfaf;
}

.emp-chip {
  display: inline-block;
  background: #f0f7ff;
  color: #0369a1;
  border-radius: 6px;
  padding: 3px 8px;
  font-size: 0.8rem;
  font-weight: 500;
  border: 1px solid #e0f2fe;
  white-space: nowrap;
}

.code-link {
  color: var(--blue, #496883);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.code-link:hover {
  color: #1f3b54;
}

.th-payment-method {
  min-width: 120px;
  white-space: nowrap;
}

.td-payment-method {
  white-space: nowrap;
  color: #4b5563;
  font-size: 0.85rem;
}

.status-dot,
.payment-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 4px;
}

.badge-status.status-completed .status-dot,
.badge-payment.payment-paid .payment-dot {
  background-color: #16a34a;
}

.badge-status.status-pending .status-dot,
.badge-payment.payment-unpaid .payment-dot {
  background-color: #ea580c;
}

.order-table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  margin-top: 10px;
  border-top: 1px solid #e9e5db;
  flex-wrap: wrap;
  gap: 12px;
}

.order-table-footer .footer-left-count {
  font-size: 0.85rem;
  color: #5d676e;
}

.order-table-footer .filter-indicator-text {
  margin-left: 4px;
  color: #7c3aed;
}

.order-table-footer .footer-center-pagination {
  display: flex;
  align-items: center;
  gap: 4px;
}

.order-table-footer .footer-right-size {
  display: flex;
  align-items: center;
}

/* Badges đồng bộ 100% với ảnh 7 và màn Danh sách hóa đơn */
.badge-store {
  background: #f3e8ff !important;
  color: #7e22ce !important;
  border: 1px solid #e9d5ff !important;
  white-space: nowrap !important;
  display: inline-flex !important;
  align-items: center !important;
  gap: 4px;
}

.badge-online {
  background: #eff6ff !important;
  color: #1d4ed8 !important;
  border: 1px solid #bfdbfe !important;
  white-space: nowrap !important;
  display: inline-flex !important;
  align-items: center !important;
  gap: 4px;
}
</style>
