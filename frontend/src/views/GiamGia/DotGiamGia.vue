<template>
  <div class="dot-giam-gia-wrapper">
    <!-- Thông báo nổi góc phải (Toast Notification) -->
    <transition name="toast-slide">
      <div v-if="toast.show" :class="['toast-msg', `toast-${toast.type}`]">
        <div class="toast-icon">
          <svg v-if="toast.type === 'success'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          <svg v-else-if="toast.type === 'error'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
        </div>
        <div class="toast-content">
          <span class="toast-title">{{ toast.type === 'success' ? 'Thành công' : (toast.type === 'error' ? 'Lỗi' : 'Thông báo') }}</span>
          <span class="toast-text">{{ toast.message }}</span>
        </div>
        <button class="toast-btn-close" @click="closeToast" title="Đóng">✕</button>
      </div>
    </transition>

    <!-- ========================================================
         VIEW 1: MÀN HÌNH DANH SÁCH ĐỢT GIẢM GIÁ
    ======================================================== -->
    <div v-if="currentView === 'list'" class="view-list">
      <!-- 1. Thanh tiêu đề trên cùng góc trái -->
      <div class="breadcrumb-header">
        <div class="breadcrumb-left">
          <span class="breadcrumb-text">Quản lý giảm giá <span class="slash">/</span> <b>Đợt giảm giá</b></span>
        </div>
      </div>

      <!-- 2. Khung Bộ lọc (Chuẩn ảnh mẫu) -->
      <div class="content-card filter-card-custom">
        <div class="card-header-filter">
          <div class="filter-icon-box">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#6b7280" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
            </svg>
          </div>
          <div class="filter-title-wrap">
            <h3 class="filter-title">Bộ lọc</h3>
            <p class="filter-subtitle">Tra cứu nhanh dữ liệu.</p>
          </div>
        </div>

        <!-- Lưới 4 ô lọc trên 1 hàng -->
        <div class="filter-inputs-grid-4">
          <!-- Tìm kiếm -->
          <div class="form-field">
            <label class="filter-label">Tìm kiếm</label>
            <div class="input-inner-custom">
              <span class="prefix-icon-svg">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </span>
              <input
                  type="text"
                  v-model="filters.keyword"
                  @input="handleSearchInput"
                  placeholder="Mã, tên, giá trị..."
                  class="filter-text-input"
              />
            </div>
          </div>

          <!-- Ngày bắt đầu -->
          <div class="form-field">
            <label class="filter-label">Ngày bắt đầu</label>
            <div class="input-inner-custom">
              <input
                  type="date"
                  v-model="filters.startDate"
                  @change="handleFilterChange"
                  class="filter-date-input"
              />
            </div>
          </div>

          <!-- Ngày kết thúc -->
          <div class="form-field">
            <label class="filter-label">Ngày kết thúc</label>
            <div class="input-inner-custom">
              <input
                  type="date"
                  v-model="filters.endDate"
                  :min="filters.startDate || undefined"
                  @change="handleFilterChange"
                  class="filter-date-input"
              />
            </div>
          </div>

          <!-- Trạng thái (Select2Combobox vừa chọn vừa tìm kiếm gõ phím được) -->
          <div class="form-field">
            <label class="filter-label">Trạng thái</label>
            <Select2Combobox
              v-model="filters.trangThai"
              :options="trangThaiOptions"
              value-key="value"
              label-key="label"
              placeholder="Tất cả trạng thái"
              search-placeholder="Tìm kiếm trạng thái..."
              @change="handleFilterChange"
            />
          </div>
        </div>

        <!-- Hàng nút góc phải (Xanh dương & Be) -->
        <div class="filter-actions-right">
          <button class="btn btn-action-beige" @click="resetFilters">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="1 4 1 10 7 10"></polyline>
              <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
            </svg>
            <span>Đặt lại bộ lọc</span>
          </button>
          <button class="btn btn-action-outline-blue" @click="exportToExcel" :disabled="exporting">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
            </svg>
            <span>{{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}</span>
          </button>
          <button class="btn btn-action-solid-blue" @click="openCreateView">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span>Tạo đợt giảm giá</span>
          </button>
        </div>
      </div>

      <!-- 2. Khung Bảng danh sách (Chuẩn ảnh mẫu) -->
      <div class="content-card table-card-custom">
        <div class="table-header-row-custom">
          <h3 class="table-title-custom">Danh sách các đợt giảm giá</h3>
          <span class="record-count-simple">
            {{ totalElements }} bản ghi hiển thị.
          </span>
        </div>

        <div class="table-responsive-custom">
          <table class="custom-table-styled">
            <thead>
            <tr>
              <th style="width: 5%; text-align: center;">STT</th>
              <th style="width: 13%;">Mã</th>
              <th style="width: 20%;">Tên</th>
              <th style="width: 11%;">Giá trị</th>
              <th style="width: 15%;">Thời gian bắt đầu</th>
              <th style="width: 15%;">Thời gian kết thúc</th>
              <th style="width: 13%; text-align: center;">Trạng thái</th>
              <th style="width: 8%; text-align: center;">Hành động</th>
            </tr>
            </thead>
            <tbody>
            <!-- Trạng thái Đang tải -->
            <tr v-if="loading">
              <td colspan="8" class="empty-cell">
                <span class="inline-spinner"></span> Đang tải dữ liệu...
              </td>
            </tr>

            <!-- Trạng thái Lỗi -->
            <tr v-else-if="errorMessage">
              <td colspan="8" class="error-cell">
                {{ errorMessage }}
                <div style="margin-top: 8px;">
                  <button class="btn btn-action-outline-blue" style="height: 2rem; font-size: 0.85rem;" @click="fetchData">Thử lại</button>
                </div>
              </td>
            </tr>

            <!-- Trạng thái Không có dữ liệu -->
            <tr v-else-if="campaigns.length === 0">
              <td colspan="8" class="empty-cell">
                Không có đợt giảm giá nào phù hợp với bộ lọc hiện tại.
              </td>
            </tr>

            <!-- Dữ liệu thực từ Database -->
            <tr v-else v-for="(item, index) in campaigns" :key="item.id">
              <td style="text-align: center;" class="text-sub-num">
                {{ (currentPage - 1) * pageSize + index + 1 }}
              </td>
              <td class="font-bold text-main-code">{{ item.maDotGiamGia || '—' }}</td>
              <td class="font-medium text-main-name">{{ item.tenDotGiamGia || '—' }}</td>
              <td class="font-bold text-dark">
                {{ formatDiscountVal(item) }}
              </td>
              <td class="text-date-simple">{{ formatDateTime(item.ngayBatDau) }}</td>
              <td class="text-date-simple">
                <span v-if="item.ngayKetThuc">{{ formatDateTime(item.ngayKetThuc) }}</span>
                <span v-else class="text-muted">—</span>
              </td>

              <!-- Trạng thái: Badge pill tròn mượt mà theo trạng thái thực tế -->
              <td style="text-align: center;">
                <span
                    class="badge-pill-status"
                    :class="getCampaignStatus(item).class"
                >
                  {{ getCampaignStatus(item).text }}
                </span>
              </td>

              <!-- Cột Hành động: 2 nút tròn bo góc nhẹ (Power đỏ nhạt + Eye xám xanh) -->
              <td style="text-align: center;">
                <div class="action-buttons-flex">
                  <button
                      v-if="getCampaignStatus(item).code !== 'ENDED'"
                      class="btn-icon-circle btn-icon-power"
                      :title="item.trangThai === 1 ? 'Chuyển sang Ngừng hoạt động' : 'Chuyển sang Đang hoạt động'"
                      @click="openToggleStatusModal(item)"
                  >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                      <line x1="12" y1="2" x2="12" y2="12"></line>
                    </svg>
                  </button>

                  <button
                      class="btn-icon-circle btn-icon-eye"
                      title="Xem chi tiết & Chỉnh sửa"
                      @click="openEditView(item)"
                  >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>

        <!-- Phân trang đáy bảng chuẩn ảnh mẫu -->
        <div class="pagination-footer-flex" v-if="totalPages > 0">
          <div class="page-size-left">
            <select v-model.number="pageSize" @change="onPageSizeChange" class="select-page-size-custom">
              <option :value="5">5</option>
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </div>

          <div class="pagination-controls-right">
            <button
                class="pg-btn-box"
                :disabled="currentPage <= 1 || loading"
                @click="goToPage(1)"
                title="Trang đầu"
            >|‹</button>
            <button
                class="pg-btn-box"
                :disabled="currentPage <= 1 || loading"
                @click="goToPage(currentPage - 1)"
                title="Trang trước"
            >‹</button>

            <button
                v-for="p in visiblePages"
                :key="p"
                class="pg-btn-box"
                :class="{ 'pg-active': p === currentPage }"
                :disabled="loading"
                @click="goToPage(p)"
            >
              {{ p }}
            </button>

            <button
                class="pg-btn-box"
                :disabled="currentPage >= totalPages || loading"
                @click="goToPage(currentPage + 1)"
                title="Trang sau"
            >›</button>
            <button
                class="pg-btn-box"
                :disabled="currentPage >= totalPages || loading"
                @click="goToPage(totalPages)"
                title="Trang cuối"
            >›|</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================================
         VIEW 2: MÀN HÌNH TẠO MỚI / CHỈNH SỬA TOÀN TRANG (FULL-PAGE)
    ======================================================== -->
    <div v-else class="view-form-video">
      <!-- Breadcrumb thanh điều hướng trên cùng -->
      <div class="video-breadcrumb-bar">
        <button class="btn-video-back" @click="backToList" title="Quay lại">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
        </button>
        <div class="video-breadcrumb-text">
          <span class="crumb-link" @click="backToList">Quản lý giảm giá</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-link" @click="backToList">Đợt giảm giá</span>
          <span class="crumb-sep">/</span>
          <span class="crumb-current">{{ isEditing ? 'Chi tiết đợt giảm giá' : 'Thêm đợt giảm giá' }}</span>
        </div>
      </div>

      <!-- HÀNG TRÊN: GRID 2 CỘT (Cột trái: Thông tin đợt giảm, Cột phải: Chọn sản phẩm) -->
      <div class="video-top-grid">
        <!-- CỘT TRÁI: THẺ THÔNG TIN ĐỢT GIẢM -->
        <div class="video-card card-campaign-info">
          <div class="video-card-header">
            <h3 class="video-card-title">Thông tin đợt giảm</h3>
          </div>

          <div class="video-form-body">
            <!-- Mã đợt (Để trống tự tạo, làm mờ khi để trống) -->
            <div class="video-form-group">
              <label class="video-form-label">Mã đợt</label>
              <div class="input-with-action-btn">
                <input
                  type="text"
                  v-model="formData.maDotGiamGia"
                  :disabled="isEditing"
                  class="video-input"
                  :class="{ 'input-auto-gen-empty': !formData.maDotGiamGia && !isEditing }"
                  placeholder="Hệ thống tự tạo nếu để trống..."
                />
                <button
                  type="button"
                  class="btn-code-refresh"
                  @click="generateRandomMa"
                  :disabled="isEditing"
                  title="Sinh mã ngẫu nhiên"
                >
                  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="23 4 23 10 17 10"></polyline>
                    <polyline points="1 20 1 14 7 14"></polyline>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Tên đợt * -->
            <div class="video-form-group">
              <label class="video-form-label">Tên đợt <span class="req">*</span></label>
              <input
                type="text"
                v-model="formData.tenDotGiamGia"
                :class="['video-input', { 'input-has-error': errors.tenDotGiamGia }]"
                placeholder="Nhập tên đợt giảm giá..."
                @input="clearError('tenDotGiamGia')"
              />
              <span v-if="errors.tenDotGiamGia" class="field-error-msg">{{ errors.tenDotGiamGia }}</span>
            </div>

            <!-- Giá trị giảm (%) * -->
            <div class="video-form-group">
              <label class="video-form-label">Giá trị giảm (%) <span class="req">*</span></label>
              <div class="input-with-suffix-group" :class="{ 'input-has-error': errors.phanTramGiam }">
                <input
                  type="number"
                  min="1"
                  v-model.number="formData.phanTramGiam"
                  @input="handlePhanTramInput"
                  class="video-input input-no-spin"
                  placeholder="Nhập mức giảm..."
                />
                <span class="input-suffix-addon">%</span>
              </div>
              <span v-if="errors.phanTramGiam" class="field-error-msg">{{ errors.phanTramGiam }}</span>
            </div>

            <!-- Thời gian bắt đầu * -->
            <div class="video-form-group">
              <label class="video-form-label">Thời gian bắt đầu <span class="req">*</span></label>
              <input
                type="datetime-local"
                v-model="formData.ngayBatDau"
                :min="minStartDateTime"
                :class="['video-input', { 'input-has-error': errors.ngayBatDau }]"
                @change="handleNgayBatDauChange"
                @input="handleNgayBatDauChange"
              />
              <span v-if="errors.ngayBatDau" class="field-error-msg">{{ errors.ngayBatDau }}</span>
            </div>

            <!-- Thời gian kết thúc * -->
            <div class="video-form-group">
              <label class="video-form-label">Thời gian kết thúc <span class="req">*</span></label>
              <input
                type="datetime-local"
                v-model="formData.ngayKetThuc"
                :min="formData.ngayBatDau || minStartDateTime || undefined"
                :class="['video-input', { 'input-has-error': errors.ngayKetThuc }]"
                @change="handleNgayKetThucChange"
                @input="handleNgayKetThucChange"
              />
              <span v-if="errors.ngayKetThuc" class="field-error-msg">{{ errors.ngayKetThuc }}</span>
            </div>


            <!-- Mô tả -->
            <div class="video-form-group">
              <label class="video-form-label">Mô tả</label>
              <textarea
                v-model="formData.moTa"
                class="video-textarea"
                rows="3"
                placeholder="Nhập mô tả..."
              ></textarea>
            </div>

            <!-- Nút Tạo đợt giảm giá -->
            <div class="video-form-actions">
              <button 
                class="btn-video-submit" 
                @click="onSaveClick" 
                :disabled="submitting"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                  <polyline points="17 21 17 13 7 13 7 21"></polyline>
                  <polyline points="7 3 7 8 15 8"></polyline>
                </svg>
                <span>{{ isEditing ? 'Cập nhật đợt giảm giá' : 'Tạo đợt giảm giá' }}</span>
              </button>
              <button class="btn-video-cancel" @click="backToList">
                Hủy
              </button>
            </div>
          </div>
        </div>

        <!-- CỘT PHẢI: GỒM 2 THẺ (Chọn sản phẩm áp dụng + Sản phẩm & biến thể đã chọn) -->
        <div class="video-right-column">
          <!-- 1. Thẻ chọn sản phẩm áp dụng -->
          <div class="video-card card-select-products" :class="{ 'card-has-error': errors.selectedVariants }">
            <div class="video-card-header header-between">
              <div class="header-left-group">
                <div>
                  <h3 class="video-card-title">Chọn sản phẩm áp dụng</h3>
                  <span class="video-card-subtitle" :class="{ 'text-error-red': errors.selectedVariants }">
                    Đã chọn {{ selectedProductCount }} sản phẩm ({{ selectedVariants.length }} biến thể)
                  </span>
                </div>
              </div>

              <!-- Thanh tìm kiếm sản phẩm: ô input, nút xanh -->
              <div class="product-top-filters">
                <div class="search-box-inner">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  </svg>
                  <input
                    type="text"
                    v-model="productSearchText"
                    placeholder="Tìm theo tên hoặc mã..."
                    class="filter-search-input filter-search-input-wide"
                    @keyup.enter="handleFilterProducts"
                  />
                </div>

                <button class="btn-search-blue" @click="handleFilterProducts" title="Tìm kiếm">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Cảnh báo lỗi khi chưa chọn sản phẩm nào -->
            <div v-if="errors.selectedVariants" class="select-products-error-alert">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              <span>{{ errors.selectedVariants }}</span>
            </div>

            <!-- Bảng danh sách sản phẩm để chọn -->
            <div class="video-table-wrap">
              <table class="video-table">
                <thead>
                  <tr>
                    <th style="width: 44px; text-align: center;">
                      <input
                        type="checkbox"
                        :checked="isAllProductsChecked"
                        @change="toggleSelectAllSourceProducts"
                        class="custom-check"
                      />
                    </th>
                    <th style="width: 50px; text-align: center;">STT</th>
                    <th style="width: 105px;">Mã SP</th>
                    <th>Tên SP</th>
                    <th style="width: 125px;">Thương hiệu</th>
                    <th style="width: 125px;">Chất liệu</th>
                    <th style="width: 55px; text-align: center;"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(p, idx) in paginatedSourceProducts"
                    :key="p.id"
                    :class="{ 
                      'row-checked': p.selected,
                      'row-disabled-campaign': isProductInActiveCampaign(p)
                    }"
                    :title="isProductInActiveCampaign(p) ? ('Sản phẩm này đang được áp dụng trong đợt giảm giá [' + (getProductActiveCampaign(p)?.maDotGiamGia || getProductActiveCampaign(p)?.tenDotGiamGia || 'khác') + ']') : ''"
                    @click="onProductRowClick(p)"
                  >
                    <td style="text-align: center;" @click.stop :title="isProductInActiveCampaign(p) ? 'Sản phẩm này đang được áp dụng trong đợt giảm giá khác' : ''">
                      <input
                        type="checkbox"
                        :checked="p.selected"
                        :disabled="isProductInActiveCampaign(p)"
                        @change="toggleProductSelection(p)"
                        class="custom-check"
                        :title="isProductInActiveCampaign(p) ? 'Sản phẩm này đang được áp dụng trong đợt giảm giá khác' : ''"
                      />
                    </td>
                    <td style="text-align: center;" class="text-sub">
                      {{ (productCurrentPage - 1) * productPageSize + idx + 1 }}
                    </td>
                    <td class="font-bold text-code">{{ p.ma }}</td>
                    <td class="font-medium text-name">
                      <span>{{ p.ten }}</span>
                      <span 
                        v-if="isProductInActiveCampaign(p)" 
                        class="badge-active-campaign"
                        :title="'Đang áp dụng trong đợt [' + (getProductActiveCampaign(p)?.maDotGiamGia || '') + '] - Bấm để sao chép mã!'"
                        @click.stop
                      >
                        <span>Đang áp dụng: </span>
                        <strong class="badge-code-text" @click.stop="copyCampaignCode(getProductActiveCampaign(p)?.maDotGiamGia)">
                          {{ getProductActiveCampaign(p)?.maDotGiamGia || 'Đợt khác' }}
                        </strong>
                        <button 
                          v-if="getProductActiveCampaign(p)?.maDotGiamGia"
                          type="button" 
                          class="btn-badge-copy" 
                          @click.stop="copyCampaignCode(getProductActiveCampaign(p)?.maDotGiamGia)"
                          title="Sao chép mã đợt giảm giá này"
                        >
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                          </svg>
                        </button>
                      </span>
                    </td>
                  <td class="text-sub font-medium">{{ p.thuongHieu || '—' }}</td>
                  <td class="text-sub font-medium">{{ p.chatLieu || '—' }}</td>
                  <td style="text-align: center;">
                    <button 
                      class="btn-add-circle" 
                      :disabled="isProductInActiveCampaign(p)"
                      :class="{ 'btn-disabled': isProductInActiveCampaign(p) }"
                      @click.stop="addProductVariants(p)" 
                      :title="isProductInActiveCampaign(p) ? 'Sản phẩm này đang được áp dụng trong đợt giảm giá khác' : 'Thêm tất cả biến thể'"
                    >
                      +
                    </button>
                  </td>
                </tr>
                <tr v-if="filteredAvailableProducts.length === 0">
                  <td colspan="7" class="empty-cell">Không có sản phẩm nào phù hợp!</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Phân trang bảng sản phẩm -->
          <div class="video-pagination-bar">
            <div class="page-size-selector">
              <select v-model.number="productPageSize" class="select-page-size">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
              </select>
            </div>
            <div class="page-nav-group">
              <button
                class="btn-nav-arrow"
                :disabled="productCurrentPage <= 1"
                @click="productCurrentPage--"
              >‹</button>
              <span class="active-page-num">{{ productCurrentPage }}</span>
              <button
                class="btn-nav-arrow"
                :disabled="productCurrentPage >= sourceTotalPages"
                @click="productCurrentPage++"
              >›</button>
            </div>
          </div>
        </div>

        <!-- 2. Thẻ sản phẩm & biến thể đã chọn (được đẩy lên trên vào cột phải) -->
        <div class="video-card card-applied-variants">
        <div class="video-card-header header-between">
          <div class="header-left-group">
            <h3 class="video-card-title">Sản phẩm & biến thể đã chọn áp dụng</h3>
          </div>

          <!-- Bộ lọc bên phải: Màu sắc, Kích cỡ, Tìm kiếm biến thể -->
          <div class="product-top-filters">
            <div style="min-width: 145px;">
              <Select2Combobox
                v-model="selectedFilterColor"
                :options="colorOptions"
                value-key="value"
                label-key="label"
                placeholder="Tất cả màu sắc"
                search-placeholder="Tìm màu..."
              />
            </div>

            <div style="min-width: 135px;">
              <Select2Combobox
                v-model="selectedFilterSize"
                :options="sizeOptions"
                value-key="value"
                label-key="label"
                placeholder="Tất cả kích cỡ"
                search-placeholder="Tìm cỡ..."
              />
            </div>

            <div class="search-box-inner" style="min-width: 220px;">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                type="text"
                v-model="selectedFilterKeyword"
                placeholder="Tìm kiếm biến thể..."
                class="filter-search-input"
              />
            </div>
          </div>
        </div>

        <!-- Bảng chi tiết biến thể -->
        <div class="video-table-wrap">
          <table class="video-table">
            <thead>
              <tr>
                <th style="width: 55px; text-align: center;">STT</th>
                <th style="width: 280px;">Sản phẩm</th>
                <th style="width: 260px;">Biến thể</th>
                <th style="width: 140px;">Giá bán</th>
                <th style="width: 150px;">Giá sau giảm</th>
                <th style="width: 110px; text-align: center;">Số lượng</th>
                <th style="width: 60px; text-align: center;"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(v, idx) in paginatedSelectedVariants" :key="v.id">
                <td style="text-align: center;" class="text-sub">
                  {{ (variantCurrentPage - 1) * variantPageSize + idx + 1 }}
                </td>
                <td>
                  <div class="product-cell-flex">
                    <div class="product-thumb-box">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.8">
                        <path d="M4 16l4.586-4.586a2 2 0 0 1 2.828 0L16 16m-2-2l1.586-1.586a2 2 0 0 1 2.828 0L20 14m-6-6h.01M6 20h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2z"></path>
                      </svg>
                    </div>
                    <div class="product-cell-info">
                      <div class="font-bold text-dark">{{ v.spTen }}</div>
                      <div class="text-sub-code">{{ v.spMa }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="variant-cell-flex">
                    <span class="variant-code-text">{{ v.maBienThe }}</span>
                    <div class="variant-tags-row">
                      <span class="badge-pill badge-color">{{ v.mauSac }}</span>
                      <span class="badge-pill badge-size">{{ v.kichCo }}</span>
                    </div>
                  </div>
                </td>
                <td class="font-medium text-dark">
                  {{ formatMoney(v.gia) }}
                </td>
                <td class="font-bold text-discounted-price">
                  {{ formatMoney(calculateDiscountedPrice(v.gia)) }}
                </td>
                <td style="text-align: center;">
                  <span class="badge-pill badge-qty">{{ v.soLuong }}</span>
                </td>
                <td style="text-align: center;">
                  <button class="btn-delete-cross" @click="removeSelectedVariant(v.id)" title="Gỡ bỏ biến thể này">
                    ✕
                  </button>
                </td>
              </tr>
              <tr v-if="displaySelectedVariants.length === 0">
                <td colspan="7" class="empty-cell">Chưa có biến thể nào được chọn áp dụng!</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Phân trang bảng biến thể -->
        <div class="video-pagination-bar">
          <div class="page-size-selector">
            <select v-model.number="variantPageSize" class="select-page-size">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </div>
          <div class="page-nav-group">
            <button
              class="btn-nav-arrow"
              :disabled="variantCurrentPage <= 1"
              @click="variantCurrentPage--"
            >‹</button>
            <span class="active-page-num">{{ variantCurrentPage }}</span>
            <button
              class="btn-nav-arrow"
              :disabled="variantCurrentPage >= variantTotalPages"
              @click="variantCurrentPage++"
            >›</button>
          </div>
        </div>
      </div> <!-- đóng card-applied-variants -->
        </div> <!-- đóng video-right-column -->
      </div> <!-- đóng video-top-grid -->
    </div> <!-- đóng view-form -->

    <!-- ========================================================
         MODAL: XÁC NHẬN CẬP NHẬT / TẠO MỚI (Y NHƯ ẢNH MẪU 1)
    ======================================================== -->
    <div v-if="showConfirmModal" class="modal-mask" @click.self="showConfirmModal = false">
      <div class="confirm-modal-box">
        <div class="confirm-header">
          <div class="confirm-title-left">
            <h4>{{ isEditing ? 'Xác nhận cập nhật' : 'Xác nhận tạo mới' }}</h4>
          </div>
          <button class="btn-close" @click="showConfirmModal = false">✕</button>
        </div>

        <div class="confirm-body">
          <p class="confirm-lead-text">
            Bạn có chắc chắn muốn lưu các thay đổi cho đợt giảm giá
            <b>[{{ formData.maDotGiamGia || 'MỚI' }}]</b> không?
          </p>

          <div class="confirm-info-card">
            <div class="confirm-row">
              <span class="confirm-label">Tên đợt:</span>
              <span class="confirm-val font-medium">{{ formData.tenDotGiamGia }}</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Loại giảm:</span>
              <span class="confirm-val font-medium">{{ formData.loaiGiamGia === 2 ? 'Giảm bằng tiền mặt (VNĐ)' : 'Giảm theo phần trăm (%)' }}</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Mức giảm:</span>
              <span class="confirm-val font-bold text-dark">
                {{ formData.loaiGiamGia === 2 ? formatMoney(formData.giaTriGiam) : formData.phanTramGiam + '%' }}
              </span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Giảm tối đa:</span>
              <span class="confirm-val">
                {{ formData.loaiGiamGia === 2 ? 'Không áp dụng (Khóa khi giảm bằng tiền)' : (formData.giamToiDa ? formatMoney(formData.giamToiDa) : 'Không giới hạn') }}
              </span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Thời hạn:</span>
              <span class="confirm-val">{{ formatInputDateDisplay(formData.ngayBatDau) }} ➔ {{ formData.ngayKetThuc ? formatInputDateDisplay(formData.ngayKetThuc) : 'Vô thời hạn' }}</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Sản phẩm áp dụng:</span>
              <span class="confirm-val">{{ selectedProductCount }} sản phẩm</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Trạng thái:</span>
              <span class="confirm-val font-bold" :class="formData.trangThai === 1 ? 'text-green' : 'text-danger'">
                {{ formData.trangThai === 1 ? 'Hoạt Động' : 'Ngưng Hoạt Động' }}
              </span>
            </div>
          </div>
        </div>

        <div class="confirm-footer">
          <button class="btn btn-cancel-confirm" @click="showConfirmModal = false">
            Hủy bỏ
          </button>
          <button class="btn btn-accept-confirm" :disabled="submitting" @click="doSubmitAPI">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8"><polyline points="20 6 9 17 4 12"/></svg>
            {{ submitting ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========================================================
         MODAL: XÁC NHẬN ĐỔI TRẠNG THÁI (CHUẨN HÌNH ẢNH MẪU)
    ======================================================== -->
    <div v-if="showToggleStatusModal && selectedToggleItem" class="modal-mask" @click.self="showToggleStatusModal = false">
      <div class="toggle-status-modal-box">
        <!-- Header -->
        <div class="toggle-modal-header" :class="selectedToggleItem.trangThai === 1 ? 'header-deactivate' : 'header-activate'">
          <div class="toggle-header-left">
            <div class="toggle-title-wrap">
              <h4 class="toggle-title-text" :class="selectedToggleItem.trangThai === 1 ? 'title-deactivate' : 'title-activate'">
                {{ selectedToggleItem.trangThai === 1 ? 'Xác nhận ngừng hoạt động' : 'Xác nhận kích hoạt hoạt động' }}
              </h4>
              <p class="toggle-subtitle-text">
                {{ selectedToggleItem.trangThai === 1 ? 'Tạm ngưng hiệu lực của đợt giảm giá' : 'Kích hoạt hiệu lực của đợt giảm giá' }}
              </p>
            </div>
          </div>
          <button class="btn-toggle-close" @click="showToggleStatusModal = false">✕</button>
        </div>

        <!-- Body -->
        <div class="toggle-modal-body">
          <p class="toggle-lead-msg">
            Bạn có chắc chắn muốn chuyển đợt giảm giá <b>[{{ selectedToggleItem.maDotGiamGia || '—' }}]</b> sang trạng thái
            <b :class="selectedToggleItem.trangThai === 1 ? 'text-danger' : 'text-green'">
              {{ selectedToggleItem.trangThai === 1 ? 'Ngừng hoạt động' : 'Hoạt động' }}
            </b> không?
          </p>

          <!-- Bảng tóm tắt thông tin tinh gọn -->
          <div class="toggle-info-card">
            <div class="toggle-info-row">
              <span class="toggle-info-label">Mã đợt:</span>
              <span class="toggle-info-val font-bold text-dark">{{ selectedToggleItem.maDotGiamGia || '—' }}</span>
            </div>
            <div class="toggle-info-row">
              <span class="toggle-info-label">Tên đợt:</span>
              <span class="toggle-info-val font-medium text-dark">{{ selectedToggleItem.tenDotGiamGia || '—' }}</span>
            </div>
            <div class="toggle-info-row">
              <span class="toggle-info-label">Trạng thái mới:</span>
              <span class="toggle-info-val font-bold" :class="selectedToggleItem.trangThai === 1 ? 'text-danger' : 'text-green'">
                {{ selectedToggleItem.trangThai === 1 ? 'Ngừng hoạt động' : 'Đang hoạt động' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="toggle-modal-footer">
          <button class="btn btn-toggle-cancel" @click="showToggleStatusModal = false" :disabled="togglingStatus">
            Hủy bỏ
          </button>
          <button
              class="btn btn-toggle-action"
              :class="selectedToggleItem.trangThai === 1 ? 'btn-red-action' : 'btn-green-action'"
              :disabled="togglingStatus"
              @click="confirmToggleStatus"
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span>{{ togglingStatus ? 'Đang cập nhật...' : (selectedToggleItem.trangThai === 1 ? 'Ngừng hoạt động' : 'Hoạt động') }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

// Đồng hồ thời gian thực tự động cập nhật mỗi 1 giây để đổi trạng thái tự động không cần reload
const currentTime = ref(Date.now())

// Tùy chọn cho các combobox Select2 (vừa chọn vừa tìm kiếm gõ phím được)
const trangThaiOptions = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang diễn ra' },
  { value: 'UPCOMING', label: 'Sắp diễn ra' },
  { value: 'ENDED', label: 'Đã kết thúc' },
  { value: '0', label: 'Ngừng hoạt động' }
]

const colorOptions = [
  { value: '', label: 'Tất cả màu sắc' },
  { value: 'Đen', label: 'Đen' },
  { value: 'Trắng', label: 'Trắng' },
  { value: 'Xám', label: 'Xám' },
  { value: 'Đỏ', label: 'Đỏ' },
  { value: 'Xanh navy', label: 'Xanh navy' }
]

const sizeOptions = [
  { value: '', label: 'Tất cả kích cỡ' },
  { value: 'S', label: 'S' },
  { value: 'M', label: 'M' },
  { value: 'L', label: 'L' },
  { value: 'XL', label: 'XL' },
  { value: 'XXL', label: 'XXL' }
]

// Điều hướng view: 'list' (danh sách) hoặc 'form' (màn hình tạo/sửa)
const currentView = ref('list')

// Thông báo Toast
const toast = ref({ show: false, message: '', type: 'success' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => { toast.value.show = false }, 3200)
}
const closeToast = () => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value.show = false
}

// Dữ liệu danh sách đợt giảm giá
const campaigns = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)

// Modal xác nhận lưu/tạo
const showConfirmModal = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const exporting = ref(false)

// Modal xác nhận đổi trạng thái (theo hình ảnh thiết kế)
const showToggleStatusModal = ref(false)
const selectedToggleItem = ref(null)
const togglingStatus = ref(false)

const formData = ref({
  id: null,
  maDotGiamGia: '',
  tenDotGiamGia: '',
  loaiGiamGia: 1, // 1: Giảm theo %, 2: Giảm theo số tiền (VNĐ)
  phanTramGiam: null,
  giaTriGiam: null,
  giamToiDa: null,
  ngayBatDau: '',
  ngayKetThuc: '',
  moTa: '',
  trangThai: 1
})

// Quản lý lỗi validate inline trong form
const errors = ref({})
const clearError = (field) => {
  if (errors.value[field]) {
    delete errors.value[field]
  }
}


// Mốc thời gian tối thiểu cho thời gian bắt đầu (không trước hiện tại khi tạo mới)
const minStartDateTime = computed(() => {
  if (isEditing.value) return undefined
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
})

// Hàm tự sinh mã đợt ngẫu nhiên (nút xoay tròn)
const generateRandomMa = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let result = 'DGG'
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  formData.value.maDotGiamGia = result
  showToast(`Đã tạo mã ngẫu nhiên: ${result}`, 'info')
}

// ==========================================
// DỮ LIỆU SẢN PHẨM & BIẾN THỂ (CHUẨN VIDEO)
// ==========================================
const productSearchText = ref('')
const productSearchColor = ref('')
const productSearchSize = ref('')

const selectedFilterColor = ref('')
const selectedFilterSize = ref('')
const selectedFilterKeyword = ref('')

// Danh sách sản phẩm nguồn chuẩn theo CSDL FFTShirt (Áo thun)
const productSourceList = ref([
  {
    id: 1,
    ma: 'SP001',
    ten: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    thuongHieu: 'FF Shirt',
    chatLieu: 'Cotton 100%',
    gia: 199000,
    selected: false,
    variants: [
      { id: 1, maBienThe: 'CTSP01', mauSac: 'Đen', kichCo: 'S', gia: 199000, soLuong: 50 },
      { id: 2, maBienThe: 'CTSP02', mauSac: 'Đen', kichCo: 'M', gia: 199000, soLuong: 100 },
      { id: 3, maBienThe: 'CTSP03', mauSac: 'Trắng', kichCo: 'M', gia: 199000, soLuong: 80 }
    ]
  },
  {
    id: 2,
    ma: 'SP002',
    ten: 'Áo Polo Nam Cổ Bẻ Cao Cấp',
    thuongHieu: 'FF Shirt',
    chatLieu: 'CVC Cá Sấu',
    gia: 299000,
    selected: false,
    variants: [
      { id: 4, maBienThe: 'CTSP04', mauSac: 'Đen', kichCo: 'M', gia: 299000, soLuong: 60 },
      { id: 5, maBienThe: 'CTSP05', mauSac: 'Xanh navy', kichCo: 'L', gia: 299000, soLuong: 40 }
    ]
  },
  {
    id: 3,
    ma: 'SP003',
    ten: 'Áo Phông Unisex In Hình Graphic',
    thuongHieu: 'Local Brand',
    chatLieu: 'Cotton 100%',
    gia: 250000,
    selected: false,
    variants: [
      { id: 6, maBienThe: 'CTSP06', mauSac: 'Trắng', kichCo: 'L', gia: 250000, soLuong: 70 }
    ]
  },
  {
    id: 4,
    ma: 'SP004',
    ten: 'Áo Phông Cổ Tim Cotton Compact',
    thuongHieu: 'FF Shirt',
    chatLieu: 'Cotton Compact',
    gia: 220000,
    selected: false,
    variants: [
      { id: 7, maBienThe: 'CTSP07', mauSac: 'Xám', kichCo: 'XL', gia: 220000, soLuong: 80 },
      { id: 8, maBienThe: 'CTSP08', mauSac: 'Đen', kichCo: 'XXL', gia: 220000, soLuong: 50 }
    ]
  },
  {
    id: 5,
    ma: 'SP005',
    ten: 'Áo Polo Thể Thao Phối Kẻ Sọc',
    thuongHieu: 'Polo Pro',
    chatLieu: 'Coolmax Co Giãn',
    gia: 320000,
    selected: false,
    variants: [
      { id: 9, maBienThe: 'CTSP09', mauSac: 'Đỏ', kichCo: 'L', gia: 320000, soLuong: 60 },
      { id: 10, maBienThe: 'CTSP10', mauSac: 'Trắng', kichCo: 'XL', gia: 320000, soLuong: 45 }
    ]
  }
])

// Danh sách biến thể đã chọn áp dụng
const selectedVariants = ref([])

// Bản đồ ID sản phẩm cha -> Đối tượng đợt giảm giá đang áp dụng
const activeProductCampaignMap = ref(new Map())

// Danh sách ID sản phẩm đang áp dụng (phản ứng theo thời gian thực)
const activeCampaignProductIds = computed(() => {
  const ids = new Set()
  if (!activeProductCampaignMap.value) return ids
  activeProductCampaignMap.value.forEach((c, spId) => {
    if (c && c.trangThai !== 0) {
      const end = c.ngayKetThuc ? new Date(c.ngayKetThuc).getTime() : null
      if (!end || currentTime.value < end) {
        ids.add(Number(spId))
      }
    }
  })
  return ids
})

// Lấy thông tin đợt giảm giá đang chứa sản phẩm này (tự đổi khi hết hạn theo currentTime)
const getProductActiveCampaign = (p) => {
  if (!p || !p.id) return null
  const c = activeProductCampaignMap.value.get(Number(p.id))
  if (!c) return null
  if (c.trangThai === 0) return null
  const end = c.ngayKetThuc ? new Date(c.ngayKetThuc).getTime() : null
  if (end && currentTime.value >= end) return null
  return c
}

// Kiểm tra xem sản phẩm cha có đang thuộc đợt giảm giá chưa kết thúc hay không (tự động cập nhật không cần load lại trang)
const isProductInActiveCampaign = (p) => {
  return getProductActiveCampaign(p) !== null
}

// Tải danh sách sản phẩm cha đang trong thời gian đợt giảm giá (kèm đối tượng đợt giảm giá)
const loadActiveCampaignProducts = async (excludeDotId = null) => {
  const newMap = new Map()
  const now = Date.now()

  // 1. Quét từ danh sách campaigns đang có trong state / cache frontend
  if (campaigns.value && campaigns.value.length > 0) {
    for (const c of campaigns.value) {
      if (excludeDotId && Number(c.id) === Number(excludeDotId)) continue
      if (c.trangThai === 0) continue // Ngừng hoạt động thì bỏ qua
      const end = c.ngayKetThuc ? new Date(c.ngayKetThuc).getTime() : null
      if (end && now >= end) continue // Đã kết thúc thì bỏ qua

      const cachedProds = getSavedCampaignProductIds(c.id, c.maDotGiamGia)
      if (cachedProds && Array.isArray(cachedProds)) {
        cachedProds.forEach(spId => {
          if (!newMap.has(Number(spId))) {
            newMap.set(Number(spId), c)
          }
        })
      }
    }
  }

  // 2. Gọi API chuyên dụng lấy map sản phẩm -> đợt giảm giá từ backend
  try {
    const params = {}
    if (excludeDotId) params.excludeDotId = excludeDotId
    const res = await api.get('/api/dot-giam-gia/active-products-map', { params })
    if (res.data && typeof res.data === 'object') {
      Object.entries(res.data).forEach(([spId, dgg]) => {
        if (dgg && dgg.trangThai !== 0) {
          const end = dgg.ngayKetThuc ? new Date(dgg.ngayKetThuc).getTime() : null
          if (!end || now < end) {
            newMap.set(Number(spId), dgg)
          }
        }
      })
    }
  } catch (e) {
  }

  // 3. Dự phòng API active-product-ids nếu map trống
  try {
    const params = {}
    if (excludeDotId) params.excludeDotId = excludeDotId
    const res = await api.get('/api/dot-giam-gia/active-product-ids', { params })
    if (res.data && Array.isArray(res.data)) {
      res.data.forEach(id => {
        if (!newMap.has(Number(id))) {
          // Tìm trong campaigns list
          const foundCampaign = campaigns.value.find(c => c.trangThai === 1 && (!c.ngayKetThuc || new Date(c.ngayKetThuc).getTime() > now))
          if (foundCampaign) {
            newMap.set(Number(id), foundCampaign)
          }
        }
      })
    }
  } catch (e) {
  }

  activeProductCampaignMap.value = newMap
}

// Sao chép mã đợt giảm giá
const copyCampaignCode = async (code) => {
  if (!code) return
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(code)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = code
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    showToast(`Đã sao chép mã đợt giảm giá: ${code}!`, 'success')
  } catch (err) {
    showToast(`Mã đợt: ${code}`, 'info')
  }
}

// Lọc và sắp xếp sản phẩm ở bảng chọn trên:
// Sản phẩm chưa áp dụng đẩy lên trên, sản phẩm đã áp dụng đợt giảm giá khác đẩy xuống dưới
const filteredAvailableProducts = computed(() => {
  const kw = productSearchText.value.trim().toLowerCase()
  const list = productSourceList.value.filter(p => {
    return !kw || (p.ma && p.ma.toLowerCase().includes(kw)) || (p.ten && p.ten.toLowerCase().includes(kw))
  })

  // Sắp xếp: chưa áp dụng (0) lên TRÊN, đã áp dụng (1) xuống DƯỚI
  return [...list].sort((a, b) => {
    const aInCampaign = isProductInActiveCampaign(a) ? 1 : 0
    const bInCampaign = isProductInActiveCampaign(b) ? 1 : 0
    return aInCampaign - bInCampaign
  })
})

// Tự động về trang 1 khi người dùng gõ tìm kiếm sản phẩm
watch(productSearchText, () => {
  productCurrentPage.value = 1
})

const isAllProductsChecked = computed(() => {
  const selectable = filteredAvailableProducts.value.filter(p => !isProductInActiveCampaign(p))
  return selectable.length > 0 && selectable.every(p => p.selected)
})

const selectedProductCount = computed(() => {
  return productSourceList.value.filter(p => p.selected).length
})

// Xử lý khi click vào dòng sản phẩm: nếu đang trong đợt giảm giá khác thì không cho chọn
const onProductRowClick = (p) => {
  if (isProductInActiveCampaign(p)) {
    showToast('Sản phẩm này đang được áp dụng trong đợt giảm giá khác!', 'warning')
    return
  }
  toggleProductSelection(p)
}

// Toggle chọn 1 sản phẩm
const toggleProductSelection = (p) => {
  if (isProductInActiveCampaign(p)) {
    showToast('Sản phẩm này đang được áp dụng trong đợt giảm giá khác!', 'warning')
    return
  }
  p.selected = !p.selected
  if (p.selected) {
    if (errors.value.selectedVariants) {
      delete errors.value.selectedVariants
    }
    p.variants.forEach(v => {
      if (!selectedVariants.value.some(sv => sv.id === v.id)) {
        selectedVariants.value.push({
          id: v.id,
          spId: p.id,
          spMa: p.ma,
          spTen: p.ten,
          maBienThe: v.maBienThe,
          mauSac: v.mauSac,
          kichCo: v.kichCo,
          gia: v.gia,
          soLuong: v.soLuong
        })
      }
    })
    showToast(`Đã thêm các biến thể của "${p.ten}"!`, 'info')
  } else {
    selectedVariants.value = selectedVariants.value.filter(sv => sv.spId !== p.id)
    showToast(`Đã bỏ các biến thể của "${p.ten}"!`, 'info')
  }
}

// Thêm biến thể bằng nút (+)
const addProductVariants = (p) => {
  if (isProductInActiveCampaign(p)) {
    showToast('Sản phẩm này đang được áp dụng trong đợt giảm giá khác!', 'warning')
    return
  }
  p.selected = true
  if (errors.value.selectedVariants) {
    delete errors.value.selectedVariants
  }
  p.variants.forEach(v => {
    if (!selectedVariants.value.some(sv => sv.id === v.id)) {
      selectedVariants.value.push({
        id: v.id,
        spId: p.id,
        spMa: p.ma,
        spTen: p.ten,
        maBienThe: v.maBienThe,
        mauSac: v.mauSac,
        kichCo: v.kichCo,
        gia: v.gia,
        soLuong: v.soLuong
      })
    }
  })
  showToast(`Đã thêm tất cả biến thể của "${p.ten}"!`, 'success')
}

// Xóa 1 biến thể khỏi bảng dưới
const removeSelectedVariant = (vId) => {
  const item = selectedVariants.value.find(v => v.id === vId)
  selectedVariants.value = selectedVariants.value.filter(v => v.id !== vId)
  if (item) {
    const hasMore = selectedVariants.value.some(v => v.spId === item.spId)
    if (!hasMore) {
      const prod = productSourceList.value.find(p => p.id === item.spId)
      if (prod) prod.selected = false
    }
  }
  showToast('Đã xóa biến thể khỏi danh sách áp dụng!', 'info')
}

// Chọn tất cả sản phẩm (chỉ chọn sản phẩm chưa có đợt giảm giá)
const toggleSelectAllSourceProducts = () => {
  const next = !isAllProductsChecked.value
  if (next && errors.value.selectedVariants) {
    delete errors.value.selectedVariants
  }
  filteredAvailableProducts.value.forEach(p => {
    if (isProductInActiveCampaign(p)) {
      return // Bỏ qua sản phẩm đang trong đợt giảm giá chưa kết thúc
    }
    p.selected = next
    if (next) {
      p.variants.forEach(v => {
        if (!selectedVariants.value.some(sv => sv.id === v.id)) {
          selectedVariants.value.push({
            id: v.id,
            spId: p.id,
            spMa: p.ma,
            spTen: p.ten,
            maBienThe: v.maBienThe,
            mauSac: v.mauSac,
            kichCo: v.kichCo,
            gia: v.gia,
            soLuong: v.soLuong
          })
        }
      })
    } else {
      selectedVariants.value = selectedVariants.value.filter(sv => sv.spId !== p.id)
    }
  })
}

// Lọc hiển thị ở bảng biến thể dưới
const displaySelectedVariants = computed(() => {
  return selectedVariants.value.filter(v => {
    const kw = selectedFilterKeyword.value.trim().toLowerCase()
    const matchKw = !kw ||
      v.spTen.toLowerCase().includes(kw) ||
      v.spMa.toLowerCase().includes(kw) ||
      v.maBienThe.toLowerCase().includes(kw)
    
    const matchColor = !selectedFilterColor.value || selectedFilterColor.value === 'Tất cả màu sắc' ||
      v.mauSac === selectedFilterColor.value

    const matchSize = !selectedFilterSize.value || selectedFilterSize.value === 'Tất cả kích cỡ' ||
      String(v.kichCo) === String(selectedFilterSize.value)

    return matchKw && matchColor && matchSize
  })
})

// Phân trang bảng sản phẩm nguồn
const productCurrentPage = ref(1)
const productPageSize = ref(5)
const sourceTotalPages = computed(() => {
  return Math.ceil(filteredAvailableProducts.value.length / productPageSize.value) || 1
})
const paginatedSourceProducts = computed(() => {
  const start = (productCurrentPage.value - 1) * productPageSize.value
  return filteredAvailableProducts.value.slice(start, start + productPageSize.value)
})

// Phân trang bảng biến thể đã chọn
const variantCurrentPage = ref(1)
const variantPageSize = ref(10)
const variantTotalPages = computed(() => {
  return Math.ceil(displaySelectedVariants.value.length / variantPageSize.value) || 1
})
const paginatedSelectedVariants = computed(() => {
  const start = (variantCurrentPage.value - 1) * variantPageSize.value
  return displaySelectedVariants.value.slice(start, start + variantPageSize.value)
})

// Tính giá sau giảm
const calculateDiscountedPrice = (originalPrice) => {
  const percent = Number(formData.value.phanTramGiam) || 0
  const discount = (originalPrice * percent) / 100
  return Math.max(0, Math.round(originalPrice - discount))
}

const handleFilterProducts = () => {
  showToast(`Tìm thấy ${filteredAvailableProducts.value.length} sản phẩm phù hợp!`, 'info')
}

// Helper nhận diện loại giảm giá tiền mặt
const isCashDiscount = (item) => {
  if (!item) return false
  if (item.loaiGiamGia === 2) return true
  if (item.giaTriGiam != null && item.giaTriGiam > 0 && item.loaiGiamGia !== 1) return true
  if (item.phanTramGiam != null && item.phanTramGiam > 100) return true
  return false
}

// Kiểm tra mức giảm phần trăm khi người dùng nhập
const handlePhanTramInput = () => {
  clearError('phanTramGiam')
  const val = formData.value.phanTramGiam
  if (val === null || val === '' || val === undefined) {
    errors.value.phanTramGiam = 'Mức giảm không được để trống!'
  } else if (Number(val) > 100) {
    errors.value.phanTramGiam = 'Mức giảm không được vượt quá 100%!'
  } else if (Number(val) <= 0) {
    errors.value.phanTramGiam = 'Mức giảm phải lớn hơn 0%!'
  }
}

// Kiểm tra số tiền giảm tối đa (áp dụng khi giảm theo phần trăm)
const handleGiamToiDaInput = () => {
  if (formData.value.loaiGiamGia === 2) {
    formData.value.giamToiDa = null
  } else if (formData.value.giamToiDa != null && formData.value.giamToiDa < 0) {
    formData.value.giamToiDa = 0
  }
}

// Khi chỉnh sửa số tiền giảm
const handleGiaTriGiamInput = () => {
  if (formData.value.loaiGiamGia === 2) {
    formData.value.giamToiDa = null
  }
}

// Kiểm tra thời gian bắt đầu
const handleNgayBatDauChange = () => {
  clearError('ngayBatDau')
  if (!formData.value.ngayBatDau || formData.value.ngayBatDau.trim() === '') {
    errors.value.ngayBatDau = 'Thời gian bắt đầu không được để trống!'
    return
  }
  if (!isEditing.value) {
    if (new Date(formData.value.ngayBatDau).getTime() < Date.now() - 60000) {
      errors.value.ngayBatDau = 'Thời gian bắt đầu không được trước thời gian hiện tại!'
      return
    }
  }
  if (formData.value.ngayKetThuc) {
    if (new Date(formData.value.ngayKetThuc).getTime() <= new Date(formData.value.ngayBatDau).getTime()) {
      errors.value.ngayKetThuc = 'Thời gian kết thúc phải sau thời gian bắt đầu!'
    } else {
      clearError('ngayKetThuc')
    }
  }
}

// Kiểm tra thời gian kết thúc (bắt buộc, không cho để trống)
const handleNgayKetThucChange = () => {
  clearError('ngayKetThuc')
  if (!formData.value.ngayKetThuc || formData.value.ngayKetThuc.trim() === '') {
    errors.value.ngayKetThuc = 'Thời gian kết thúc không được để trống!'
    return
  }
  if (formData.value.ngayBatDau) {
    if (new Date(formData.value.ngayKetThuc).getTime() <= new Date(formData.value.ngayBatDau).getTime()) {
      errors.value.ngayKetThuc = 'Thời gian kết thúc phải sau thời gian bắt đầu!'
    }
  }
}

// Lưu trữ và khôi phục metadata đợt giảm giá (lượt sử dụng, hình thức, giảm tối đa)
const getSavedCampaignMeta = (id, ma) => {
  try {
    if (id) {
      const rawId = localStorage.getItem('dgg_meta_id_' + id)
      if (rawId) return JSON.parse(rawId)
    }
    if (ma) {
      const rawMa = localStorage.getItem('dgg_meta_ma_' + ma)
      if (rawMa) return JSON.parse(rawMa)
    }
  } catch (e) {
    console.error('Lỗi đọc cache đợt giảm giá:', e)
  }
  return null
}

const saveCampaignMeta = (id, ma, meta) => {
  try {
    const json = JSON.stringify(meta)
    if (id) localStorage.setItem('dgg_meta_id_' + id, json)
    if (ma) localStorage.setItem('dgg_meta_ma_' + ma, json)
  } catch (e) {
    console.error('Lỗi lưu cache đợt giảm giá:', e)
  }
}

// Lưu trữ và lấy danh sách sản phẩm cha của từng đợt giảm giá
const getSavedCampaignProductIds = (id, ma) => {
  try {
    if (id) {
      const raw = localStorage.getItem('dgg_prods_id_' + id)
      if (raw) return JSON.parse(raw)
    }
    if (ma) {
      const raw = localStorage.getItem('dgg_prods_ma_' + ma)
      if (raw) return JSON.parse(raw)
    }
  } catch (e) {
  }
  return []
}

const saveCampaignProductIds = (id, ma, productIds) => {
  try {
    const json = JSON.stringify(productIds || [])
    if (id) localStorage.setItem('dgg_prods_id_' + id, json)
    if (ma) localStorage.setItem('dgg_prods_ma_' + ma, json)
  } catch (e) {
  }
}

// Bộ lọc
const filters = ref({
  keyword: '',
  startDate: '',
  endDate: '',
  loaiGiam: '',
  trangThai: ''
})

let searchTimeout = null

// Helper ngày giờ
const toInputDateTime = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const formatDate = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  if (isNaN(date.getTime())) return val
  const day = date.getDate()
  const month = date.getMonth() + 1
  const year = date.getFullYear()
  return `${day}/${month}/${year}`
}

const formatDateTime = (val) => {
  if (!val) return '—'
  const date = new Date(val)
  if (isNaN(date.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const day = pad(date.getDate())
  const month = pad(date.getMonth() + 1)
  const year = date.getFullYear()
  return `${hours}:${minutes} ${day}/${month}/${year}`
}

// Tính trạng thái thực tế dựa trên mốc thời gian hiện tại (phản ứng tự động theo currentTime)
const getCampaignStatus = (item) => {
  if (!item) return { code: 'INACTIVE', text: 'Ngừng hoạt động', class: 'status-pill-inactive' }
  if (item.trangThai === 0) {
    return { code: 'INACTIVE', text: 'Ngừng hoạt động', class: 'status-pill-inactive' }
  }
  const now = currentTime.value
  const start = item.ngayBatDau ? new Date(item.ngayBatDau).getTime() : null
  const end = item.ngayKetThuc ? new Date(item.ngayKetThuc).getTime() : null

  if (start && now < start) {
    return { code: 'UPCOMING', text: 'Sắp diễn ra', class: 'status-pill-upcoming' }
  }
  if (end && now >= end) {
    return { code: 'ENDED', text: 'Đã kết thúc', class: 'status-pill-ended' }
  }
  return { code: 'ACTIVE', text: 'Đang diễn ra', class: 'status-pill-active' }
}

const formatDiscountVal = (item) => {
  if (!item) return '0%'
  if (isCashDiscount(item)) {
    return formatMoney(item.giaTriGiam || item.phanTramGiam)
  }
  return `${item.phanTramGiam != null ? item.phanTramGiam : 0}%`
}

const formatInputDateDisplay = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return val
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

const formatMoney = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0)

// Load dữ liệu
const fetchData = async () => {
  try {
    loading.value = true
    errorMessage.value = ''

    const hasStatusFilter = Boolean(filters.value.trangThai !== '' && filters.value.trangThai !== null && filters.value.trangThai !== undefined)
    const hasLoaiFilter = Boolean(filters.value.loaiGiam !== '' && filters.value.loaiGiam !== null && filters.value.loaiGiam !== undefined)
    const isClientFilter = hasStatusFilter || hasLoaiFilter

    const params = {
      page: isClientFilter ? 0 : (currentPage.value - 1),
      size: isClientFilter ? 1000 : pageSize.value,
      sortBy: 'id',
      sortDir: 'desc'
    }

    if (filters.value.keyword && filters.value.keyword.trim() !== '') {
      params.keyword = filters.value.keyword.trim()
    }
    if (filters.value.startDate) {
      params.startDate = filters.value.startDate
    }
    if (filters.value.endDate) {
      params.endDate = filters.value.endDate
    }

    const response = await api.get('/api/dot-giam-gia', { params })
    const data = response.data
    let list = data.content || []
    
    // Khôi phục metadata mở rộng (giảm tối đa)
    list.forEach(item => {
      const meta = getSavedCampaignMeta(item.id, item.maDotGiamGia)
      if (meta) {
        if (meta.giamToiDa !== undefined && item.giamToiDa == null) item.giamToiDa = meta.giamToiDa
      }
    })

    // Lọc theo loại giảm giá nếu người dùng chọn
    if (filters.value.loaiGiam === '1') {
      list = list.filter(item => !isCashDiscount(item))
    } else if (filters.value.loaiGiam === '2') {
      list = list.filter(item => isCashDiscount(item))
    }

    // Lọc theo trạng thái (Đang diễn ra, Sắp diễn ra, Đã kết thúc, Ngừng hoạt động)
    if (hasStatusFilter) {
      if (filters.value.trangThai === '0' || filters.value.trangThai === 0 || filters.value.trangThai === 'INACTIVE') {
        list = list.filter(item => item.trangThai === 0 || getCampaignStatus(item).code === 'INACTIVE')
      } else {
        list = list.filter(item => getCampaignStatus(item).code === filters.value.trangThai)
      }
    }

    if (isClientFilter) {
      totalElements.value = list.length
      totalPages.value = Math.ceil(list.length / pageSize.value) || 1
      const start = (currentPage.value - 1) * pageSize.value
      campaigns.value = list.slice(start, start + pageSize.value)
    } else {
      campaigns.value = list
      totalPages.value = data.totalPages || 0
      totalElements.value = data.totalElements || 0
    }
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu từ database:', error)
    errorMessage.value = 'Không thể tải dữ liệu đợt giảm giá từ database. Vui lòng kiểm tra backend và kết nối SQL Server!'
    campaigns.value = []
    totalPages.value = 0
    totalElements.value = 0
  } finally {
    loading.value = false
  }
}

// Watch filters.trangThai để tự động lọc ngay lập tức khi người dùng chọn trong combobox
watch(() => filters.value.trangThai, () => {
  currentPage.value = 1
  fetchData()
})

const handleSearchInput = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    fetchData()
  }, 350)
}

const handleFilterChange = () => {
  if (filters.value.startDate && filters.value.endDate) {
    if (filters.value.endDate < filters.value.startDate) {
      showToast('Ngày kết thúc phải sau ngày bắt đầu!', 'error')
      filters.value.endDate = ''
      return
    }
  }
  currentPage.value = 1
  fetchData()
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    startDate: '',
    endDate: '',
    loaiGiam: '',
    trangThai: ''
  }
  currentPage.value = 1
  fetchData()
  showToast('Đã đặt lại bộ lọc!', 'info')
}

const goToPage = (page) => {
  if (page < 1 || (totalPages.value > 0 && page > totalPages.value)) return
  currentPage.value = page
  fetchData()
}

const onPageSizeChange = () => {
  currentPage.value = 1
  fetchData()
}

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  if (total <= 0) return []

  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  if (end - start < 4) {
    start = Math.max(1, end - 4)
  }
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Mở modal xác nhận đổi trạng thái (theo đúng giao diện mẫu)
const openToggleStatusModal = (item) => {
  selectedToggleItem.value = { ...item }
  showToggleStatusModal.value = true
}

const toggleStatus = (item) => {
  openToggleStatusModal(item)
}

// Xác nhận gọi API đổi trạng thái từ modal
const confirmToggleStatus = async () => {
  if (!selectedToggleItem.value) return
  const itemInModal = selectedToggleItem.value
  const isActivating = itemInModal.trangThai !== 1
  const targetStatus = isActivating ? 'Hoạt Động' : 'Ngưng Hoạt Động'

  try {
    togglingStatus.value = true
    const res = await api.put(`/api/dot-giam-gia/${itemInModal.id}/toggle-status`)
    
    // Cập nhật trạng thái trực tiếp trên mảng danh sách
    const targetInList = campaigns.value.find(c => c.id === itemInModal.id)
    if (targetInList) {
      targetInList.trangThai = res.data.trangThai
    }
    loadActiveCampaignProducts()
    
    showToast(`Đã chuyển đợt giảm giá sang trạng thái "${targetStatus}" thành công!`, 'success')
    showToggleStatusModal.value = false
    selectedToggleItem.value = null
  } catch (error) {
    console.error('Lỗi khi đổi trạng thái:', error)
    showToast(`Không thể cập nhật trạng thái đợt giảm giá!`, 'error')
  } finally {
    togglingStatus.value = false
  }
}

// Xuất danh sách đợt giảm giá ra file Excel (.xls UTF-8 mở được bằng Excel & Sheets)
const exportToExcel = async () => {
  try {
    exporting.value = true
    showToast('Đang chuẩn bị dữ liệu xuất Excel...', 'info')

    // Lấy toàn bộ bản ghi theo bộ lọc hiện tại (tối đa 5000 dòng)
    const params = {
      page: 0,
      size: 5000,
      sortBy: 'id',
      sortDir: 'desc'
    }

    if (filters.value.keyword && filters.value.keyword.trim() !== '') {
      params.keyword = filters.value.keyword.trim()
    }
    if (filters.value.startDate) {
      params.startDate = filters.value.startDate
    }
    if (filters.value.endDate) {
      params.endDate = filters.value.endDate
    }
    const isDynamicStatus = (filters.value.trangThai === 'ACTIVE' || filters.value.trangThai === 'UPCOMING' || filters.value.trangThai === 'ENDED')
    if (filters.value.trangThai === '0' || filters.value.trangThai === 0) {
      params.trangThai = 0
    }

    const res = await api.get('/api/dot-giam-gia', { params })
    let dataList = res.data?.content || campaigns.value || []
    if (isDynamicStatus) {
      dataList = dataList.filter(item => getCampaignStatus(item).code === filters.value.trangThai)
    }

    if (!dataList || dataList.length === 0) {
      showToast('Không có dữ liệu đợt giảm giá để xuất Excel!', 'error')
      return
    }

    const now = new Date()
    const nowStr = `${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`

    let rowsHtml = ''
    dataList.forEach((item, idx) => {
      const isCash = isCashDiscount(item)
      let discountDisplay = ''
      if (isCash) {
        discountDisplay = formatMoney(item.giaTriGiam || item.phanTramGiam)
        if (item.giamToiDa) {
          discountDisplay += ` (Tối đa: ${formatMoney(item.giamToiDa)})`
        }
      } else {
        discountDisplay = (item.phanTramGiam != null ? item.phanTramGiam + '%' : '0%')
      }
      const typeText = isCash ? 'Tiền mặt' : 'Phần trăm'
      const statusText = item.trangThai === 1 ? 'Hoạt Động' : 'Ngưng Hoạt Động'
      const statusColor = item.trangThai === 1 ? '#2e7d32' : '#c62828'
      rowsHtml += `
        <tr>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${idx + 1}</td>
          <td style="text-align: center; font-weight: bold; color: #304b60; border: 1px solid #bfbfbf; padding: 6px;">${item.maDotGiamGia || ''}</td>
          <td style="border: 1px solid #bfbfbf; padding: 6px;">${item.tenDotGiamGia || ''}</td>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${typeText}</td>
          <td style="text-align: center; font-weight: bold; border: 1px solid #bfbfbf; padding: 6px;">${discountDisplay}</td>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${formatDate(item.ngayBatDau)}</td>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${item.ngayKetThuc ? formatDate(item.ngayKetThuc) : 'Vô thời hạn'}</td>
          <td style="text-align: center; font-weight: bold; color: ${statusColor}; border: 1px solid #bfbfbf; padding: 6px;">${statusText}</td>
        </tr>
      `
    })

    const excelHtml = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office" 
            xmlns:x="urn:schemas-microsoft-com:office:excel" 
            xmlns="http://www.w3.org/TR/REC-html40">
        <head>
          <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
          <!--[if gte mso 9]>
          <xml>
            <x:ExcelWorkbook>
              <x:ExcelWorksheets>
                <x:ExcelWorksheet>
                  <x:Name>DanhSachDotGiamGia</x:Name>
                  <x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions>
                </x:ExcelWorksheet>
              </x:ExcelWorksheets>
            </x:ExcelWorkbook>
          </xml>
          <![endif]-->
          <style>
            table { border-collapse: collapse; font-family: Arial, sans-serif; font-size: 13px; }
            th { background-color: #304b60; color: #ffffff; font-weight: bold; border: 1px solid #bfbfbf; padding: 10px 8px; text-align: center; }
            td { border: 1px solid #bfbfbf; padding: 6px 8px; }
            .title { font-size: 16px; font-weight: bold; color: #304b60; text-align: center; }
          </style>
        </head>
        <body>
          <table>
            <tr>
              <td colspan="8" class="title" style="height: 38px; vertical-align: middle; border: none;">
                DANH SÁCH ĐỢT GIẢM GIÁ - CỬA HÀNG FF T-SHIRT
              </td>
            </tr>
            <tr>
              <td colspan="8" style="color: #666; font-style: italic; border: none; padding-bottom: 10px;">
                Thời gian xuất: ${nowStr} | Tổng số bản ghi: ${dataList.length} đợt giảm giá
              </td>
            </tr>
            <thead>
              <tr>
                <th style="width: 50px;">STT</th>
                <th style="width: 130px;">Mã đợt</th>
                <th style="width: 260px;">Tên đợt giảm giá</th>
                <th style="width: 120px;">Hình thức</th>
                <th style="width: 160px;">Mức giảm & Tối đa</th>
                <th style="width: 160px;">Ngày bắt đầu</th>
                <th style="width: 160px;">Ngày kết thúc</th>
                <th style="width: 140px;">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              ${rowsHtml}
            </tbody>
          </table>
        </body>
      </html>
    `

    const blob = new Blob(['\uFEFF' + excelHtml], { type: 'application/vnd.ms-excel;charset=utf-8;' })
    const link = document.createElement('a')
    const fileDateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`
    link.href = URL.createObjectURL(blob)
    link.download = `Danh_Sach_Dot_Giam_Gia_${fileDateStr}.xls`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)

    showToast(`Xuất file Excel thành công (${dataList.length} đợt)!`, 'success')
  } catch (error) {
    console.error('Lỗi khi xuất Excel:', error)
    showToast('Lỗi khi xuất file Excel!', 'error')
  } finally {
    exporting.value = false
  }
}

// Chuyển sang màn hình tạo mới
const openCreateView = async () => {
  errors.value = {}
  isEditing.value = false
  try {
    await loadActiveCampaignProducts(null)
  } catch (err) {
    console.warn('Lỗi tải sản phẩm đang áp dụng:', err)
  }

  formData.value = {
    id: null,
    maDotGiamGia: '', // Để trống để làm mờ theo yêu cầu, hệ thống sẽ tự sinh nếu không nhập
    tenDotGiamGia: '',
    loaiGiamGia: 1, // 1: Giảm theo %, 2: Giảm theo tiền
    phanTramGiam: null,
    giaTriGiam: null,
    giamToiDa: null,
    ngayBatDau: '',
    ngayKetThuc: '',
    moTa: '',
    trangThai: 1
  }

  // Làm mới hoàn toàn danh sách sản phẩm & biến thể đã chọn
  productSourceList.value.forEach(p => {
    p.selected = false
  })
  selectedVariants.value = []

  productCurrentPage.value = 1
  variantCurrentPage.value = 1
  currentView.value = 'form'
  showToast('Chuyển sang màn hình tạo đợt giảm giá!', 'info')
}

// Chuyển sang màn hình chỉnh sửa
const openEditView = async (item) => {
  errors.value = {}
  isEditing.value = true
  try {
    await loadActiveCampaignProducts(item.id)
  } catch (err) {
    console.warn('Lỗi tải sản phẩm đang áp dụng:', err)
  }

  formData.value = {
    id: item.id,
    maDotGiamGia: item.maDotGiamGia || '',
    tenDotGiamGia: item.tenDotGiamGia || '',
    loaiGiamGia: item.loaiGiamGia || 1,
    phanTramGiam: item.phanTramGiam || 20,
    giaTriGiam: item.giaTriGiam || 50000,
    giamToiDa: item.giamToiDa || null,
    ngayBatDau: item.ngayBatDau ? toInputDateTime(item.ngayBatDau) : '',
    ngayKetThuc: item.ngayKetThuc ? toInputDateTime(item.ngayKetThuc) : '',
    moTa: item.moTa || '',
    trangThai: item.trangThai != null ? item.trangThai : 1
  }

  // Lấy chi tiết mới nhất từ API để đảm bảo nhận đầy đủ mô tả từ database
  try {
    const res = await api.get(`/api/dot-giam-gia/${item.id}`)
    if (res.data) {
      if (res.data.moTa !== undefined && res.data.moTa !== null) {
        formData.value.moTa = res.data.moTa
      }
      if (res.data.tenDotGiamGia) formData.value.tenDotGiamGia = res.data.tenDotGiamGia
      if (res.data.maDotGiamGia) formData.value.maDotGiamGia = res.data.maDotGiamGia
      if (res.data.phanTramGiam != null) formData.value.phanTramGiam = res.data.phanTramGiam
      if (res.data.loaiGiamGia != null) formData.value.loaiGiamGia = res.data.loaiGiamGia
      if (res.data.giaTriGiam != null) formData.value.giaTriGiam = res.data.giaTriGiam
      if (res.data.giamToiDa != null) formData.value.giamToiDa = res.data.giamToiDa
      if (res.data.ngayBatDau) formData.value.ngayBatDau = toInputDateTime(res.data.ngayBatDau)
      if (res.data.ngayKetThuc) formData.value.ngayKetThuc = toInputDateTime(res.data.ngayKetThuc)
      if (res.data.trangThai != null) formData.value.trangThai = res.data.trangThai
    }
  } catch (e) {
    console.warn('Lỗi khi tải chi tiết đợt giảm giá:', e)
  }

  // Tải danh sách chi tiết biến thể đã áp dụng từ backend
  try {
    const ctRes = await api.get(`/api/dot-giam-gia/${item.id}/chi-tiet`)
    if (ctRes.data && ctRes.data.length > 0) {
      selectedVariants.value = ctRes.data.map(ct => {
        const ctsp = ct.idChiTietSanPham || {}
        const sp = ctsp.idSanPham || {}
        return {
          id: ctsp.id || ct.id,
          spId: sp.id || 1,
          spMa: sp.maSanPham || ('SP' + (sp.id || '')),
          spTen: sp.tenSanPham || 'Sản phẩm',
          maBienThe: ctsp.maChiTietSanPham || ('BT-' + ct.id),
          mauSac: ctsp.idMauSac?.tenMauSac || 'Xám',
          kichCo: ctsp.idKichCo?.tenKichCo || '37',
          gia: ctsp.giaBan || 0,
          soLuong: ctsp.soLuong || 0
        }
      })
      const selectedSpIds = new Set(selectedVariants.value.map(v => v.spId))
      saveCampaignProductIds(item.id, item.maDotGiamGia, Array.from(selectedSpIds))
      productSourceList.value.forEach(p => {
        p.selected = selectedSpIds.has(p.id)
      })
    } else {
      productSourceList.value.forEach(p => {
        p.selected = (p.id === 1)
      })
      const p1 = productSourceList.value[0]
      if (p1 && p1.variants) {
        selectedVariants.value = p1.variants.map(v => ({
          id: v.id,
          spId: p1.id,
          spMa: p1.ma,
          spTen: p1.ten,
          maBienThe: v.maBienThe,
          mauSac: v.mauSac,
          kichCo: v.kichCo,
          gia: v.gia,
          soLuong: v.soLuong
        }))
      }
    }
  } catch (e) {
    console.warn('Lỗi khi tải chi tiết biến thể:', e)
  }

  productCurrentPage.value = 1
  variantCurrentPage.value = 1
  currentView.value = 'form'

  // Kiểm tra nếu ngày kết thúc <= ngày bắt đầu thì báo đỏ ngay dưới ô
  if (formData.value.ngayBatDau && formData.value.ngayKetThuc) {
    if (new Date(formData.value.ngayKetThuc).getTime() <= new Date(formData.value.ngayBatDau).getTime()) {
      errors.value.ngayKetThuc = 'Thời gian kết thúc phải sau thời gian bắt đầu!'
    }
  }

  showToast(`Mở chi tiết đợt "${formData.value.tenDotGiamGia || item.tenDotGiamGia}"!`, 'info')
}

const backToList = () => {
  currentView.value = 'list'
}

// Bấm nút Lưu / Tạo -> kiểm tra và mở popup xác nhận
const onSaveClick = () => {
  errors.value = {}

  let hasError = false

  // 1. Kiểm tra Tên đợt
  if (!formData.value.tenDotGiamGia || formData.value.tenDotGiamGia.trim() === '') {
    errors.value.tenDotGiamGia = 'Tên đợt giảm giá không được để trống!'
    hasError = true
  }

  // 2. Kiểm tra Mức giảm
  if (formData.value.loaiGiamGia === 1) {
    if (formData.value.phanTramGiam === null || formData.value.phanTramGiam === '' || formData.value.phanTramGiam === undefined) {
      errors.value.phanTramGiam = 'Mức giảm không được để trống!'
      hasError = true
    } else if (Number(formData.value.phanTramGiam) > 100) {
      errors.value.phanTramGiam = 'Mức giảm không được vượt quá 100%!'
      hasError = true
    } else if (Number(formData.value.phanTramGiam) <= 0) {
      errors.value.phanTramGiam = 'Mức giảm phải lớn hơn 0%!'
      hasError = true
    }
  } else {
    formData.value.giamToiDa = null
    if (!formData.value.giaTriGiam || Number(formData.value.giaTriGiam) <= 0) {
      errors.value.giaTriGiam = 'Vui lòng nhập số tiền giảm lớn hơn 0đ!'
      hasError = true
    }
  }

  // 3. Kiểm tra Thời gian bắt đầu
  if (!formData.value.ngayBatDau || formData.value.ngayBatDau.trim() === '') {
    errors.value.ngayBatDau = 'Thời gian bắt đầu không được để trống!'
    hasError = true
  } else if (!isEditing.value) {
    const startMs = new Date(formData.value.ngayBatDau).getTime()
    // Dung sai 10 phút để người dùng thoải mái thao tác mà không bị chặn
    if (startMs < Date.now() - 10 * 60 * 1000) {
      errors.value.ngayBatDau = 'Thời gian bắt đầu không được trước thời gian hiện tại!'
      hasError = true
    }
  }

  // 4. Kiểm tra Thời gian kết thúc (Bắt buộc, không cho để trống)
  if (!formData.value.ngayKetThuc || formData.value.ngayKetThuc.trim() === '') {
    errors.value.ngayKetThuc = 'Thời gian kết thúc không được để trống!'
    hasError = true
  } else if (formData.value.ngayBatDau) {
    if (new Date(formData.value.ngayKetThuc).getTime() <= new Date(formData.value.ngayBatDau).getTime()) {
      errors.value.ngayKetThuc = 'Thời gian kết thúc phải sau thời gian bắt đầu!'
      hasError = true
    }
  }

  // 5. Kiểm tra sản phẩm & biến thể áp dụng (Báo đỏ nếu chưa chọn)
  if (!selectedVariants.value || selectedVariants.value.length === 0) {
    errors.value.selectedVariants = 'Vui lòng chọn ít nhất một sản phẩm hoặc biến thể áp dụng!'
    hasError = true
  }

  if (hasError) {
    showToast('Vui lòng kiểm tra lại các trường báo đỏ trong form!', 'error')
    if (errors.value.selectedVariants) {
      setTimeout(() => {
        const cardEl = document.querySelector('.card-select-products')
        if (cardEl) {
          cardEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
        }
      }, 80)
    }
    return
  }

  // Kiểm tra quy tắc: Không được chứa sản phẩm đang trong đợt giảm giá chưa kết thúc
  const conflicting = selectedVariants.value.find(v => {
    if (!activeProductCampaignMap.value) return false
    const c = activeProductCampaignMap.value.get(Number(v.spId))
    if (!c) return false
    if (c.trangThai === 0) return false
    const end = c.ngayKetThuc ? new Date(c.ngayKetThuc).getTime() : null
    if (end && currentTime.value >= end) return false
    return true
  })
  if (conflicting) {
    showToast(`Sản phẩm "${conflicting.spTen || 'đã chọn'}" đang trong một đợt giảm giá khác chưa kết thúc!`, 'error')
    return
  }

  showConfirmModal.value = true
}

// Xác nhận gọi API tạo mới hoặc cập nhật
const doSubmitAPI = async () => {
  try {
    submitting.value = true
    const isPercent = formData.value.loaiGiamGia === 1
    const finalVal = isPercent ? formData.value.phanTramGiam : formData.value.giaTriGiam

    // Tự sinh mã nếu người dùng để trống
    let finalMa = formData.value.maDotGiamGia?.trim() || ''
    if (!finalMa) {
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
      let randomSuffix = ''
      for (let i = 0; i < 6; i++) {
        randomSuffix += chars.charAt(Math.floor(Math.random() * chars.length))
      }
      finalMa = 'DGG' + randomSuffix
      formData.value.maDotGiamGia = finalMa
    }

    // Lấy danh sách ID các biến thể áp dụng (chuyển sang số hợp lệ)
    const chiTietSanPhamIds = selectedVariants.value.map(v => {
      if (typeof v.id === 'number') return v.id
      const num = parseInt(String(v.id).replace(/\D/g, ''))
      return isNaN(num) ? 1 : num
    })

    const appliedParentSpIds = Array.from(new Set(selectedVariants.value.map(v => Number(v.spId))))

    const payload = {
      maDotGiamGia: finalMa,
      tenDotGiamGia: formData.value.tenDotGiamGia.trim(),
      phanTramGiam: finalVal,
      loaiGiamGia: formData.value.loaiGiamGia,
      giaTriGiam: isPercent ? null : formData.value.giaTriGiam,
      giamToiDa: isPercent ? (formData.value.giamToiDa || null) : null,
      ngayBatDau: new Date(formData.value.ngayBatDau).toISOString(),
      ngayKetThuc: formData.value.ngayKetThuc ? new Date(formData.value.ngayKetThuc).toISOString() : null,
      moTa: formData.value.moTa?.trim() || null,
      trangThai: formData.value.trangThai,
      chiTietSanPhamIds: chiTietSanPhamIds,
      sanPhamIds: appliedParentSpIds
    }

    let res = null
    if (isEditing.value) {
      res = await api.put(`/api/dot-giam-gia/${formData.value.id}`, payload)
      showToast('Cập nhật đợt giảm giá thành công!', 'success')
    } else {
      res = await api.post('/api/dot-giam-gia', payload)
      showToast('Tạo mới đợt giảm giá thành công!', 'success')
    }

    // Lưu metadata mở rộng và danh sách sản phẩm áp dụng
    const savedId = isEditing.value ? formData.value.id : res?.data?.id
    saveCampaignMeta(savedId, formData.value.maDotGiamGia, {
      giamToiDa: formData.value.giamToiDa
    })
    saveCampaignProductIds(savedId, formData.value.maDotGiamGia, appliedParentSpIds)

    showConfirmModal.value = false
    currentView.value = 'list'
    fetchData()
    loadActiveCampaignProducts()
  } catch (e) {
    console.error('Lỗi khi lưu đợt giảm giá:', e)
    const errorMsg = e.response?.data?.message || 'Lỗi khi lưu dữ liệu vào cơ sở dữ liệu!'
    showToast(errorMsg, 'error')
  } finally {
    submitting.value = false
  }
}

// Tải danh sách sản phẩm và biến thể thực tế từ CSDL backend (nếu có API)
const fetchRealProductsFromDB = async () => {
  try {
    const res = await api.get('/api/san-pham', { params: { page: 0, size: 50 } })
    const items = res.data?.content || res.data?.data || (Array.isArray(res.data) ? res.data : [])
    if (items && items.length > 0) {
      const prods = await Promise.all(items.map(async (sp) => {
        let variants = []
        try {
          const varRes = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${sp.id}`)
          if (varRes.data && varRes.data.length > 0) {
            variants = varRes.data.map(v => ({
              id: v.id,
              maBienThe: v.maChiTietSanPham || ('CTSP' + v.id),
              mauSac: v.tenMauSac || v.idMauSac?.tenMauSac || 'Đen',
              kichCo: v.tenKichCo || v.idKichCo?.tenKichCo || 'M',
              gia: v.giaBan || 199000,
              soLuong: v.soLuong || 50
            }))
          }
        } catch (e) {
        }
        return {
          id: sp.id,
          ma: sp.maSanPham || ('SP' + sp.id),
          ten: sp.tenSanPham || 'Áo phông FFTShirt',
          thuongHieu: sp.tenThuongHieu || sp.idThuongHieu?.tenThuongHieu || 'FF Shirt',
          chatLieu: sp.tenChatLieu || sp.idChatLieu?.tenChatLieu || 'Cotton 100%',
          gia: sp.giaBan || (variants[0]?.gia || 199000),
          selected: false,
          variants: variants.length > 0 ? variants : [
            { id: sp.id * 10 + 1, maBienThe: (sp.maSanPham || 'SP') + '-M', mauSac: 'Đen', kichCo: 'M', gia: 199000, soLuong: 50 }
          ]
        }
      }))
      if (prods.length > 0) {
        productSourceList.value = prods
      }
    }
  } catch (e) {
    // Nếu API chưa sẵn sàng thì giữ nguyên dữ liệu mẫu chuẩn theo du_lieu_FF_Tshirt.sql
  }
}

let realTimeTimer = null

onMounted(() => {
  fetchData()
  fetchRealProductsFromDB()
  loadActiveCampaignProducts()

  // Real-time ticker mỗi giây: tự động đổi trạng thái mượt mà (Sắp diễn ra -> Đang diễn ra -> Đã kết thúc) không cần tải lại trang
  realTimeTimer = setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (realTimeTimer) clearInterval(realTimeTimer)
})
</script>

<style scoped>
.dot-giam-gia-wrapper {
  padding: 2.2rem 3.5rem 5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Toast Notification dưới góc phải màn hình */
.toast-msg {
  position: fixed;
  bottom: 24px;
  right: 28px;
  min-width: 320px;
  max-width: 460px;
  padding: 1rem 1.2rem;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 0.9rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  z-index: 99999;
}
.toast-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.toast-content {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
}
.toast-title {
  font-size: 0.9rem;
  font-weight: 750;
  color: #111827;
  line-height: 1.25;
}
.toast-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: #4b5563;
  line-height: 1.4;
}
.toast-btn-close {
  background: transparent;
  border: none;
  font-size: 1rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  line-height: 1;
  flex-shrink: 0;
}
.toast-btn-close:hover {
  color: #111827;
  background-color: #f3f4f6;
}

/* Biến thể màu cho Toast tinh tế (không nền chói lọi) */
.toast-success {
  border-left: 4px solid #16a34a;
}
.toast-success .toast-icon {
  background-color: #f0fdf4;
  color: #16a34a;
}
.toast-error {
  border-left: 4px solid #dc2626;
}
.toast-error .toast-icon {
  background-color: #fef2f2;
  color: #dc2626;
}
.toast-info {
  border-left: 4px solid #2563eb;
}
.toast-info .toast-icon {
  background-color: #eff6ff;
  color: #2563eb;
}

/* Hiệu ứng trượt từ góc phải */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}
.toast-slide-enter-from {
  opacity: 0;
  transform: translateY(25px) translateX(35px);
}
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(25px) translateX(35px);
}

/* Header breadcrumb: Tăng khoảng cách lề thoáng đãng */
.breadcrumb-header {
  margin-bottom: 1.6rem;
  display: flex;
  align-items: center;
  padding: 0 0.5rem;
}
.breadcrumb-text {
  font-size: 1rem;
  color: #6b7280;
  letter-spacing: 0.01em;
}
.breadcrumb-text b {
  color: #111827;
  font-weight: 800;
}
.slash {
  margin: 0 10px;
  color: #d1d5db;
}

/* Content Card chung màu trắng be tinh tế */
.content-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #ede9de;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
  padding: 1.5rem 1.85rem;
  margin-bottom: 1.6rem;
}

/* 1. KHỐI BỘ LỌC (Chuẩn ảnh mẫu) */
.filter-card-custom {
  background: #ffffff;
}

.card-header-filter {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1.25rem;
}
.filter-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}
.filter-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: #1f2937;
}
.filter-subtitle {
  font-size: 0.8rem;
  color: #9ca3af;
  margin: 2px 0 0 0;
}

/* Lưới 4 ô lọc */
.filter-inputs-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.25rem;
}
.filter-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.4rem;
}
.input-inner-custom {
  position: relative;
  display: flex;
  align-items: center;
}
.prefix-icon-svg {
  position: absolute;
  left: 0.85rem;
  pointer-events: none;
  display: flex;
  align-items: center;
}
.filter-text-input {
  width: 100%;
  height: 2.6rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 0.85rem 0 2.5rem;
  font-size: 0.88rem;
  color: #1f2937;
  background-color: #ffffff;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.filter-text-input:focus,
.filter-date-input:focus,
.filter-select-input:focus {
  border-color: #304b60;
  box-shadow: 0 0 0 3px rgba(48, 75, 96, 0.12);
}
.filter-date-input,
.filter-select-input {
  width: 100%;
  height: 2.6rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.88rem;
  color: #1f2937;
  background-color: #ffffff;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

/* Hàng nút góc phải bộ lọc */
.filter-actions-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
}
/* Nút Đặt lại bộ lọc (Màu be thanh lịch) */
.btn-action-beige {
  height: 2.45rem;
  padding: 0 1.2rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all 0.2s;
}
.btn-action-beige:hover:not(:disabled) {
  background-color: #faeed7;
  border-color: #c8b99c;
}
.btn-action-beige:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Nút Xuất Excel (Viền xanh dương nhạt) */
.btn-action-outline-blue {
  height: 2.45rem;
  padding: 0 1.2rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  border: 1px solid #c2daf0;
  background-color: #f0f7ff;
  color: #304b60;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all 0.2s;
}
.btn-action-outline-blue:hover:not(:disabled) {
  background-color: #e2effb;
  border-color: #93c5fd;
  color: #1e3a5f;
}
.btn-action-outline-blue:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Nút Tạo đợt giảm giá (Màu xanh dương đậm nổi bật) */
.btn-action-solid-blue {
  height: 2.45rem;
  padding: 0 1.35rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  background-color: #304b60;
  color: #ffffff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(48, 75, 96, 0.25);
}
.btn-action-solid-blue:hover {
  background-color: #1e3342;
}

/* 2. KHỐI BẢNG DANH SÁCH (Chuẩn ảnh mẫu & cột gần nhau) */
.table-card-custom {
  background: #ffffff;
}
.table-header-row-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.2rem;
}
.table-title-custom {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}
.record-count-simple {
  font-size: 0.85rem;
  color: #9ca3af;
}
.table-responsive-custom {
  overflow-x: auto;
  border: 1px solid #ede9de;
  border-radius: 10px;
  background: #ffffff;
}
.custom-table-styled {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
  table-layout: fixed;
}
.custom-table-styled th {
  background-color: #faf9f6;
  color: #556268;
  font-weight: 700;
  padding: 0.72rem 0.45rem;
  text-align: left;
  border-bottom: 1px solid #ede9de;
  font-size: 0.86rem;
}
.custom-table-styled td {
  padding: 0.75rem 0.45rem;
  border-bottom: 1px solid #f3f0e8;
  color: #374151;
  vertical-align: middle;
}
.custom-table-styled tr:last-child td {
  border-bottom: none;
}
.custom-table-styled tr:hover td {
  background-color: #fdfcf9;
}

.text-sub-num {
  color: #9ca3af;
  font-size: 0.85rem;
}
.text-main-code {
  color: #304b60;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.text-main-name {
  color: #1f2937;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}
.text-date-simple {
  color: #4b5563;
  font-size: 0.88rem;
}
.text-muted-infinity {
  color: #9ca3af;
  font-size: 0.82rem;
  font-style: italic;
}

/* Badge pill trạng thái tròn mượt y ảnh (không bao giờ rớt dòng) */
.badge-pill-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.28rem 0.85rem;
  border-radius: 9999px;
  font-size: 0.82rem;
  font-weight: 500;
  white-space: nowrap;
}
.status-pill-active {
  background-color: #ecfdf5;
  color: #10b981;
  border: 1px solid #d1fae5;
}
.status-pill-upcoming {
  background-color: #eff6ff;
  color: #2563eb;
  border: 1px solid #dbeafe;
}
.status-pill-ended {
  background-color: #f3f4f6;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}
.status-pill-inactive {
  background-color: #fef2f2;
  color: #ef4444;
  border: 1px solid #fee2e2;
}

.input-auto-gen-empty {
  font-style: italic;
  color: #6b7280;
  background-color: #f9fafb !important;
}
.input-auto-gen-empty::placeholder {
  font-style: italic;
  color: #9ca3af;
}
.auto-code-hint {
  font-size: 0.76rem;
  color: #6b7280;
  font-style: italic;
  margin-top: 5px;
  display: block;
}

/* Cụm 2 nút hành động (Màu Be & Xanh dương) */
.action-buttons-flex {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
}
.btn-icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #ede9de;
  background-color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-icon-power {
  color: #b45309;
  background-color: #fff8eb;
  border-color: #dfd5c2;
}
.btn-icon-power:hover {
  background-color: #faeed7;
  border-color: #c8b99c;
  color: #92400e;
  transform: scale(1.05);
}
.btn-icon-eye {
  color: #304b60;
  background-color: #f0f7ff;
  border-color: #c2daf0;
}
.btn-icon-eye:hover {
  background-color: #e2effb;
  border-color: #93c5fd;
  color: #1e3a5f;
  transform: scale(1.05);
}

/* Phân trang đáy bảng (Màu Xanh dương & Be) */
.pagination-footer-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.25rem;
  padding-top: 0.85rem;
}
.select-page-size-custom {
  border: 1px solid #dfd5c2;
  border-radius: 6px;
  padding: 0.35rem 0.65rem;
  font-size: 0.82rem;
  color: #4b5563;
  background-color: #fffbf5;
  outline: none;
  cursor: pointer;
}
.pagination-controls-right {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.pg-btn-box {
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  color: #556268;
  font-size: 0.85rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}
.pg-btn-box:hover:not(:disabled):not(.pg-active) {
  border-color: #c2daf0;
  color: #304b60;
  background-color: #f0f7ff;
}
.pg-btn-box.pg-active {
  background-color: #304b60;
  border-color: #304b60;
  color: #ffffff;
  font-weight: 700;
}
.pg-btn-box:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.empty-cell { text-align: center; padding: 2.75rem !important; color: #6b7280; font-style: italic; }
.error-cell { text-align: center; padding: 2.75rem !important; color: #dc2626; font-weight: 500; }
.inline-spinner {
  display: inline-block;
  width: 15px;
  height: 15px;
  border: 2px solid #d1d5db;
  border-top-color: #304b60;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  vertical-align: middle;
  margin-right: 8px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.font-bold { font-weight: 700; }
.font-medium { font-weight: 600; }
.text-dark { color: #111827; }

/* ========================================================
   STYLES CHO VIEW 2: TẠO MỚI / CHỈNH SỬA FULL-PAGE
======================================================== */
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.6rem;
  flex-wrap: wrap;
  gap: 1rem;
  padding: 0 0.5rem;
}
.form-breadcrumb { display: flex; align-items: center; gap: 0.8rem; }
.btn-back-link {
  width: 36px;
  height: 36px;
  border-radius: 7px;
  background: #ffffff;
  border: 1px solid #e0dbce;
  cursor: pointer;
  display: grid;
  place-items: center;
  color: #304b60;
  transition: all 0.15s;
}
.btn-back-link:hover { background: #fbf9f4; border-color: #304b60; }
.form-header-actions { display: flex; gap: 0.8rem; }

.form-main-layout {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
}

.form-section-card {
  padding: 1.75rem 2.25rem;
  margin-bottom: 1.5rem;
}
.section-card-head {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}
.sec-icon-box { width: 40px; height: 40px; border-radius: 9px; background: #fbf5e8; display: grid; place-items: center; }
.sec-title { margin: 0; font-size: 1.1rem; color: #111827; font-weight: 800; letter-spacing: -0.01em; }
.sec-desc { margin: 3px 0 0; font-size: 0.84rem; color: #8c9597; }
.form-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.4rem;
}

/* Thanh tìm kiếm sản phẩm cao cấp & hiện đại */
.product-search-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  background: #fdfbf7;
  padding: 0.9rem 1.2rem;
  border-radius: 9px;
  border: 1px solid #efeae0;
}

.product-search-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 280px;
}

.search-input-group {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
}

.search-svg-icon {
  position: absolute;
  left: 1rem;
  color: #9aa1a4;
  pointer-events: none;
  transition: color 0.2s;
}

.product-search-input {
  width: 100%;
  height: 2.75rem;
  border: 1px solid #ded7c9;
  border-radius: 8px;
  padding: 0 2.4rem 0 2.85rem;
  font-size: 0.9rem;
  background: #ffffff;
  color: #37444a;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.product-search-input:focus {
  border-color: #304b60;
  box-shadow: 0 0 0 3px rgba(48, 75, 96, 0.12);
  background: #ffffff;
}

.btn-clear-search {
  position: absolute;
  right: 0.75rem;
  background: #ebe6dc;
  border: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.7rem;
  color: #636d72;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-clear-search:hover {
  background: #304b60;
  color: #ffffff;
}

.btn-search-product {
  height: 2.75rem;
  padding: 0 1.35rem;
  background: #304b60;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.88rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-search-product:hover {
  background: #213544;
  transform: translateY(-1px);
}

.product-search-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.btn-toggle-all {
  height: 2.75rem;
  padding: 0 1.25rem;
  border-radius: 8px;
  border: 1px solid #dcd4c3;
  background: #ffffff;
  color: #4b585e;
  font-size: 0.88rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
}

.btn-toggle-all:hover {
  background: #fbf5e8;
  border-color: #b88628;
  color: #7f6027;
}

.btn-all-active {
  background: #eef7f0;
  border-color: #b3dfbc;
  color: #2e7d32;
}

.product-badge-found {
  font-size: 0.85rem;
  color: #637076;
  background: #f2ede1;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  white-space: nowrap;
  font-weight: 600;
}
.product-badge-found b {
  color: #304b60;
}
.product-table-box { max-height: 260px; overflow-y: auto; border: 1px solid #e9e5db; border-radius: 8px; background: #ffffff; }
.product-table th { padding: 0.95rem 1.25rem; }
.product-table td { padding: 0.95rem 1.25rem; }
.row-active td { background: #f0f7f3 !important; }
.tag-select { font-size: 0.75rem; padding: 0.15rem 0.5rem; border-radius: 4px; font-weight: 600; }
.tag-selected { background: #e8f5e9; color: #2e7d32; }
.tag-unselected { background: #eee; color: #888; }
.product-count-summary { font-size: 0.88rem; color: #8c9597; margin-top: 0.8rem; text-align: right; }
.form-footer-bar { display: flex; justify-content: flex-end; gap: 0.85rem; padding: 1.25rem 0.5rem; }

/* Modal Xác nhận lưu / cập nhật */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 10000; animation: fadeIn 0.15s ease-out; }
.confirm-modal-box {
  background: #ffffff;
  border-radius: 12px;
  width: 92%;
  max-width: 520px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  border: 1px solid #e5e7eb;
}
.confirm-header {
  padding: 1.1rem 1.4rem;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.confirm-title-left { display: flex; align-items: center; gap: 0.6rem; }
.confirm-header h4 { margin: 0; font-size: 1.15rem; color: #111827; font-weight: 800; letter-spacing: -0.015em; }
.btn-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: #9ca3af; padding: 4px; border-radius: 4px; transition: color 0.15s; }
.btn-close:hover { color: #111827; background-color: #f3f4f6; }

.confirm-body { padding: 1.35rem 1.4rem 1rem; }
.confirm-lead-text { font-size: 0.93rem; color: #374151; margin: 0 0 1rem 0; line-height: 1.5; }
.confirm-lead-text b { color: #111827; font-weight: 700; }

.confirm-info-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}
.confirm-row { display: flex; justify-content: space-between; font-size: 0.88rem; }
.confirm-label { color: #6b7280; font-weight: 500; }
.confirm-val { color: #111827; font-weight: 600; }

.confirm-footer {
  padding: 1rem 1.4rem 1.25rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  border-top: 1px solid #f3f4f6;
}
.btn-cancel-confirm {
  background: #fff8eb;
  border: 1px solid #dfd5c2;
  color: #957b48;
  font-weight: 600;
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel-confirm:hover { background: #faeed7; color: #7a6336; border-color: #c8b99c; }
.btn-accept-confirm {
  background: #304b60;
  color: #ffffff;
  font-weight: 700;
  height: 2.5rem;
  padding: 0 1.45rem;
  border-radius: 7px;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all 0.15s;
}
.btn-accept-confirm:hover { background: #1e3342; }

/* ========================================================
   MODAL XÁC NHẬN ĐỔI TRẠNG THÁI (ĐƠN GIẢN, KHÔNG MÀU MÈ)
======================================================== */
.toggle-status-modal-box {
  background: #ffffff;
  border-radius: 12px;
  width: 92%;
  max-width: 520px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: fadeIn 0.15s ease-out;
  border: 1px solid #e5e7eb;
}

.toggle-modal-header {
  padding: 1.15rem 1.4rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.toggle-header-left {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
}
.toggle-title-wrap {
  display: flex;
  flex-direction: column;
}
.toggle-title-text {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: -0.015em;
  color: #111827;
}
.title-deactivate { color: #b91c1c; }
.title-activate { color: #15803d; }
.toggle-subtitle-text {
  margin: 3px 0 0 0;
  font-size: 0.84rem;
  color: #6b7280;
}

.btn-toggle-close {
  background: transparent;
  border: none;
  font-size: 1.15rem;
  cursor: pointer;
  color: #9ca3af;
  padding: 4px;
  border-radius: 4px;
  line-height: 1;
  transition: all 0.15s;
}
.btn-toggle-close:hover { color: #111827; background-color: #f3f4f6; }

.toggle-modal-body {
  padding: 1.35rem 1.4rem 1.1rem;
}
.toggle-lead-msg {
  font-size: 0.93rem;
  color: #374151;
  margin: 0 0 1.15rem 0;
  line-height: 1.5;
}

.toggle-info-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.toggle-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
}
.toggle-info-label {
  color: #6b7280;
  font-weight: 500;
}
.toggle-info-val {
  color: #111827;
  font-weight: 600;
  text-align: right;
}

.toggle-warning-alert {
  margin-top: 1.1rem;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0.8rem 1rem;
}
.alert-content {
  font-size: 0.85rem;
  color: #4b5563;
  line-height: 1.45;
}
.alert-tag {
  font-weight: 750;
  color: #1f2937;
  margin-right: 6px;
}

.toggle-modal-footer {
  padding: 1.1rem 1.4rem 1.3rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  border-top: 1px solid #f3f4f6;
}
.btn-toggle-cancel {
  background: #fff8eb;
  border: 1px solid #dfd5c2;
  color: #957b48;
  font-weight: 600;
  height: 2.5rem;
  padding: 0 1.4rem;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-toggle-cancel:hover {
  background: #faeed7;
  color: #7a6336;
  border-color: #c8b99c;
}

.btn-toggle-action {
  height: 2.5rem;
  padding: 0 1.45rem;
  border-radius: 7px;
  border: none;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all 0.15s;
}
.btn-red-action {
  background: #dc2626;
  color: #ffffff;
}
.btn-red-action:hover:not(:disabled) {
  background: #b91c1c;
}
.btn-green-action {
  background: #16a34a;
  color: #ffffff;
}
.btn-green-action:hover:not(:disabled) {
  background: #15803d;
}
.btn-toggle-action:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* Styling cho Loại giảm giá & Giảm tối đa */
.suffix-text {
  position: absolute;
  right: 0.85rem;
  font-size: 0.85rem;
  color: #8c9597;
  pointer-events: none;
}

.tag-lock-hint {
  font-size: 0.72rem;
  color: #b71c1c;
  background: #fdeeed;
  border: 1px solid #fad2d0;
  padding: 0.12rem 0.4rem;
  border-radius: 4px;
  margin-left: 0.4rem;
  font-weight: 600;
}

.badge-mini {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
}
.badge-percent {
  background: #eef4f8;
  color: #304b60;
  border: 1px solid #d8e5ee;
}
.badge-money {
  background: #fbf5e8;
  color: #b88628;
  border: 1px solid #faeec7;
}

.discount-col-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.discount-sub-info {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}
.tag-max-discount {
  font-size: 0.73rem;
  color: #c62828;
  font-weight: 600;
  background: #fdf0f0;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  border: 1px dashed #f5c6cb;
}
.text-muted-tag {
  color: #8c9597;
  font-style: italic;
  font-size: 0.85rem;
}

.flex-1 { flex: 1; }

.field-error-text {
  color: #d32f2f;
  font-size: 0.78rem;
  margin-top: 0.35rem;
  font-weight: 600;
  display: block;
}

.badge-infinity {
  background: #eef7f0;
  color: #2e7d32;
  border: 1px solid #b3dfbc;
  font-weight: 700;
}



/* ========================================================
   GIAO DIỆN TẠO / SỬA ĐỢT GIẢM GIÁ (CHUẨN VIDEO)
======================================================== */
.view-form-video {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  animation: fadeIn 0.25s ease-out;
}

/* Breadcrumb trên cùng */
.video-breadcrumb-bar {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 0.25rem;
}
.btn-video-back {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #4b5563;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-video-back:hover {
  background: #f3f4f6;
  color: #111827;
  border-color: #d1d5db;
}
.video-breadcrumb-text {
  font-size: 0.95rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.crumb-link {
  color: #6b7280;
  cursor: pointer;
  transition: color 0.15s;
}
.crumb-link:hover {
  color: #304b60;
}
.crumb-sep {
  color: #9ca3af;
}
.crumb-current {
  color: #111827;
  font-weight: 600;
}

/* Hàng trên: Grid 2 cột (Cột trái rộng hơn, Cột phải hẹp hơn một chút và chứa cả 2 thẻ sản phẩm) */
.video-top-grid {
  display: grid;
  grid-template-columns: 430px 1fr;
  gap: 1.25rem;
  align-items: start;
}

.card-campaign-info {
  position: sticky;
  top: 1rem;
}

.video-right-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  min-width: 0;
}

/* Thẻ chung */
.video-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #edf0f2;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 1.25rem 1.4rem;
  min-width: 0;
  box-sizing: border-box;
}

/* Header của thẻ */
.video-card-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1.25rem;
}
.video-card-header.header-between {
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.85rem;
}
.header-left-group {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}
.header-icon-box.blue-icon-box,
.blue-icon-box,
.pink-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #eaf1f5;
  color: #304b60;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.video-card-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}
.video-card-subtitle {
  display: block;
  font-size: 0.8rem;
  color: #6b7280;
  font-weight: 500;
  margin-top: 2px;
}

/* Form body bên trái */
.video-form-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.video-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 1rem;
  min-width: 0;
}
.video-form-label {
  font-size: 0.86rem;
  font-weight: 600;
  color: #374151;
}
.video-form-label .req {
  color: #dc2626;
  font-weight: bold;
  margin-left: 2px;
}
.video-input,
.video-textarea {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  font-size: 0.9rem;
  color: #1f2937;
  background: #ffffff;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}
.video-input:focus,
.video-textarea:focus {
  border-color: #304b60;
  box-shadow: 0 0 0 3px rgba(48, 75, 96, 0.12);
}
.video-input:disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.input-has-error {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}
.input-has-error:focus,
.input-with-suffix-group.input-has-error:focus-within {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
}
.input-with-suffix-group.input-has-error .video-input {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}
.input-with-suffix-group.input-has-error .input-suffix-addon {
  border-color: #ef4444 !important;
  background-color: #fee2e2 !important;
  color: #b91c1c !important;
}
.field-error-msg {
  color: #ef4444;
  font-size: 0.8rem;
  font-weight: 500;
  margin-top: 4px;
  display: block;
}
.campaign-ended-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  color: #b91c1c;
  padding: 0.65rem 0.85rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.input-with-action-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.btn-code-refresh {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.btn-code-refresh:hover:not(:disabled) {
  background: #f0f7ff;
  color: #304b60;
  border-color: #c2daf0;
}
.btn-code-refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.input-with-suffix-group {
  display: flex;
  align-items: stretch;
  width: 100%;
}
.input-with-suffix-group .video-input {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: none;
  flex: 1;
}
.input-suffix-addon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 1.15rem;
  background-color: #faf9f6;
  border: 1px solid #e5e7eb;
  border-left: 1px solid #ede9de;
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
  color: #304b60;
  font-weight: 700;
  font-size: 0.95rem;
  user-select: none;
  transition: all 0.2s;
}
.input-with-suffix-group:focus-within .video-input {
  border-color: #304b60;
  box-shadow: 0 0 0 3px rgba(48, 75, 96, 0.12);
}
.input-with-suffix-group:focus-within .input-suffix-addon {
  border-color: #304b60;
  background-color: #f0f7ff;
}

/* Ẩn hoàn toàn mũi tên tăng giảm mặc định của input type=number */
.input-no-spin::-webkit-inner-spin-button,
.input-no-spin::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.input-no-spin {
  -moz-appearance: textfield;
}

.input-with-suffix {
  position: relative;
  display: flex;
  align-items: center;
}
.input-suffix {
  position: absolute;
  right: 0.85rem;
  color: #6b7280;
  font-weight: 600;
  font-size: 0.9rem;
  pointer-events: none;
}

.video-date-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.video-textarea {
  resize: vertical;
  min-height: 75px;
}

.video-form-actions {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-top: 0.5rem;
}
.btn-video-submit {
  width: 100%;
  padding: 0.75rem;
  background: #304b60;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(48, 75, 96, 0.22);
}
.btn-video-submit:hover:not(:disabled) {
  background: #1e3342;
}
.btn-video-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-video-cancel {
  width: 100%;
  padding: 0.65rem;
  background: #fff8eb;
  color: #957b48;
  border: 1px solid #dfd5c2;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: all 0.15s;
}
.btn-video-cancel:hover {
  background: #faeed7;
  color: #7a6336;
}

/* Bộ lọc tìm kiếm sản phẩm phía trên bảng */
.product-top-filters {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.search-box-inner {
  position: relative;
  display: flex;
  align-items: center;
}
.search-box-inner svg {
  position: absolute;
  left: 0.65rem;
  pointer-events: none;
}
.filter-search-input {
  padding: 0.5rem 0.75rem 0.5rem 2rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.85rem;
  outline: none;
  width: 175px;
  transition: border-color 0.2s;
  background: #ffffff;
}
.filter-search-input:focus {
  border-color: #304b60;
}
.filter-search-input-wide {
  width: 280px;
}
@media (max-width: 650px) {
  .filter-search-input-wide {
    width: 200px;
  }
}
.filter-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.85rem;
  outline: none;
  background: #ffffff;
  color: #374151;
  cursor: pointer;
  transition: border-color 0.2s;
}
.filter-select:focus {
  border-color: #304b60;
}
.btn-search-blue,
.btn-search-pink {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #304b60;
  color: #ffffff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}
.btn-search-blue:hover,
.btn-search-pink:hover {
  background: #1e3342;
}

/* Bảng dữ liệu video */
.video-table-wrap {
  overflow-x: auto;
  margin-top: 0.5rem;
}
.video-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}
.video-table th {
  padding: 0.75rem 0.85rem;
  font-size: 0.82rem;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 1px solid #edf0f2;
  background: #fafafa;
  text-align: left;
}
.video-table td {
  padding: 0.75rem 0.85rem;
  border-bottom: 1px solid #edf0f2;
  vertical-align: middle;
}
.video-table tr:hover td {
  background: #fdfafb;
}
.video-table tr.row-checked td {
  background: #f0f7ff;
}
.custom-check {
  width: 16px;
  height: 16px;
  accent-color: #304b60;
  cursor: pointer;
}
.text-code {
  color: #304b60;
}
.text-name {
  color: #1f2937;
}
.text-sub {
  color: #9ca3af;
  font-size: 0.85rem;
}
.btn-add-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid #c2daf0;
  background: #f0f7ff;
  color: #304b60;
  font-size: 1.15rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1;
}
.btn-add-circle:hover {
  background: #304b60;
  color: #ffffff;
  border-color: #304b60;
}

/* Phân trang bảng */
.video-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.85rem;
  margin-top: 0.5rem;
}
.select-page-size {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 0.35rem 0.6rem;
  font-size: 0.82rem;
  color: #4b5563;
  background: #ffffff;
  cursor: pointer;
  outline: none;
}
.page-nav-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.btn-nav-arrow {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #4b5563;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.15s;
}
.btn-nav-arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.btn-nav-arrow:not(:disabled):hover {
  border-color: #304b60;
  color: #304b60;
}
.active-page-num {
  font-size: 0.85rem;
  font-weight: 600;
  color: #111827;
  padding: 0 4px;
}

/* Thẻ biến thể đã chọn ở dưới */
.card-applied-variants {
  margin-top: 0;
}
.product-cell-flex {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.product-thumb-box {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.product-cell-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.text-sub-code {
  font-size: 0.78rem;
  color: #9ca3af;
}
.variant-cell-flex {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.variant-code-text {
  font-size: 0.84rem;
  font-weight: 600;
  color: #374151;
}
.variant-tags-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.badge-pill {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
}
.badge-color,
.badge-size {
  background: #f3f4f6;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}
.badge-qty {
  background: #f3f4f6;
  color: #374151;
  font-weight: 600;
  padding: 0.2rem 0.65rem;
}
.text-discounted-price {
  color: #b45309;
  font-size: 0.92rem;
  font-weight: 700;
}
.btn-delete-cross {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #9ca3af;
  font-size: 0.95rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.btn-delete-cross:hover {
  background: #fee2e2;
  color: #dc2626;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1050px) {
  .video-top-grid {
    grid-template-columns: 1fr;
  }
  .card-campaign-info {
    position: static;
  }
  .filter-inputs-grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 650px) {
  .dot-giam-gia-wrapper {
    padding: 1.25rem 1rem 3rem;
  }
  .filter-inputs-grid-4 {
    grid-template-columns: 1fr;
  }
  .filter-actions-right {
    flex-direction: column;
    width: 100%;
  }
  .filter-actions-right .btn {
    width: 100%;
    justify-content: center;
  }
  .pagination-footer-flex {
    flex-direction: column;
    gap: 0.75rem;
    align-items: flex-start;
  }
}

/* Badge & Row cho sản phẩm đang trong đợt giảm giá chưa kết thúc */
.row-disabled-campaign {
  background-color: #f3f4f6 !important;
  opacity: 0.72 !important;
  filter: grayscale(20%);
  user-select: text;
  cursor: not-allowed !important;
}
.row-disabled-campaign td {
  color: #64748b !important;
}
.row-disabled-campaign:hover {
  background-color: #f3f4f6 !important;
}
.badge-active-campaign {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: 8px;
  padding: 2.5px 8px;
  font-size: 11.5px;
  font-weight: 600;
  color: #dc2626;
  background-color: #fee2e2;
  border: 1px solid #fca5a5;
  border-radius: 12px;
  vertical-align: middle;
  white-space: nowrap;
  user-select: text !important;
  cursor: default;
}
.badge-code-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11.5px;
  font-weight: 700;
  color: #991b1b;
  user-select: all !important;
  cursor: pointer;
  text-decoration: underline;
  text-decoration-style: dotted;
  transition: color 0.15s;
}
.badge-code-text:hover {
  color: #7f1d1d;
}
.btn-badge-copy {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  margin-left: 2px;
  background-color: #ffffff;
  border: 1px solid #fca5a5;
  border-radius: 4px;
  color: #dc2626;
  cursor: pointer;
  transition: all 0.15s ease;
  flex-shrink: 0;
}
.btn-badge-copy:hover {
  background-color: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
  transform: scale(1.1);
}
.btn-add-circle.btn-disabled,
.btn-add-circle:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  background-color: #e2e8f0;
  color: #94a3b8;
  border-color: #cbd5e1;
}
.custom-check:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

/* Báo đỏ khi chưa chọn sản phẩm áp dụng */
.card-select-products.card-has-error {
  border: 2px solid #ef4444 !important;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12) !important;
  transition: all 0.25s ease;
}
.card-select-products.card-has-error .video-card-title {
  color: #dc2626 !important;
}
.text-error-red {
  color: #ef4444 !important;
  font-weight: 600 !important;
}
.select-products-error-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  margin: 10px 16px 4px;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 600;
  animation: fadeInError 0.25s ease-in-out;
}
@keyframes fadeInError {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Custom tinh chỉnh chiều cao Select2 khớp giao diện */
:deep(.filter-inputs-grid-4 .select2-selection) {
  height: 2.6rem !important;
  min-height: 2.6rem !important;
  border-radius: 8px !important;
  border-color: #e5e7eb !important;
}
:deep(.product-top-filters .select2-selection) {
  height: 35px !important;
  min-height: 35px !important;
  border-radius: 8px !important;
  border-color: #e5e7eb !important;
  font-size: 0.85rem !important;
}
:deep(.select2-dropdown) {
  z-index: 99999 !important;
}
</style>