<template>
  <div class="dot-giam-gia-wrapper">
    <!-- Thông báo nổi góc phải (Toast Notification) -->
    <transition name="toast-slide">
      <div v-if="toast.show" :class="['toast-msg', `toast-${toast.type}`]">
        <div class="toast-icon">
          <svg v-if="toast.type === 'success'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          <svg v-else-if="toast.type === 'error'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
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
      <div class="page-breadcrumb">
        <router-link to="/dot-giam-gia" class="crumb-parent">Quản lý giảm giá</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Đợt giảm giá</span>
      </div>

      <!-- 2. Khung Bộ lọc -->
      <div class="content-card filter-card">
        <div class="card-header-filter">
          <h3 class="filter-title">Bộ lọc tìm kiếm</h3>
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

        <!-- Hàng nút thao tác -->
        <div class="filter-actions">
          <button class="btn btn-reset" @click="resetFilters">
            Đặt lại bộ lọc
          </button>
          <button class="btn btn-export" @click="exportToExcel" :disabled="exporting">
            {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
          </button>
          <button class="btn btn-primary" @click="openCreateView">
            + Thêm đợt giảm giá
          </button>
        </div>
      </div>

      <!-- 3. Khung Bảng danh sách -->
      <div class="content-card table-card">
        <div class="table-header-row">
          <div class="table-title-wrap">
            <h3 class="table-title">Danh sách đợt giảm giá</h3>
          </div>
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
                  <button class="btn btn-export" style="height: 2rem; font-size: 0.85rem;" @click="fetchData">Thử lại</button>
                </div>
              </td>
            </tr>

            <!-- Trạng thái Không có dữ liệu -->
            <tr v-else-if="displayCampaigns.length === 0">
              <td colspan="8" class="empty-cell">
                Không có đợt giảm giá nào phù hợp với bộ lọc hiện tại.
              </td>
            </tr>

            <!-- Dữ liệu thực từ Database -->
            <tr v-else v-for="(item, index) in displayCampaigns" :key="item.id">
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
                    :class="item._status.class"
                >
                  {{ item._status.text }}
                </span>
              </td>

              <!-- Cột Hành động: 2 nút tròn bo góc nhẹ (Power đỏ nhạt + Eye xám xanh) -->
              <td style="text-align: center;">
                <div class="action-buttons-flex">
                  <!-- Công tắc gạt đổi trạng thái (Toggle Switch tương tự như PGG) -->
                  <label
                    class="switch-toggle"
                    :class="{ 'switch-disabled': item._status.code === 'ENDED' }"
                    :title="item._status.code === 'ENDED'
                      ? 'Đợt giảm giá đã kết thúc - Không thể đổi trạng thái'
                      : (item.trangThai !== 0 ? 'Đang hoạt động (nhấn để tắt)' : 'Đang ngừng hoạt động (nhấn để bật)')"
                  >
                    <input
                      type="checkbox"
                      :checked="item.trangThai !== 0 && item._status.code !== 'ENDED'"
                      :disabled="item._status.code === 'ENDED'"
                      @click.prevent="onToggleSwitchClick(item)"
                    />
                    <span class="slider-round"></span>
                  </label>

                  <!-- Nút xem chi tiết & chỉnh sửa đợt giảm giá -->
                  <button
                    class="btn-action-edit"
                    title="Xem chi tiết & Chỉnh sửa đợt giảm giá"
                    @click="openEditView(item)"
                  >
                    <svg class="action-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
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
          <span class="pagination-info">
            Hiển thị {{ campaigns.length ? ((currentPage - 1) * pageSize + 1) : 0 }} - {{ (currentPage - 1) * pageSize + campaigns.length }} / {{ totalElements }} đợt giảm giá
          </span>
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
      <div class="page-breadcrumb">
        <button class="btn-back-pill" type="button" @click="backToList" title="Quay lại">
          ← Quay lại
        </button>
        <span class="crumb-parent" @click="backToList" style="cursor: pointer;">Quản lý giảm giá</span>
        <span class="crumb-separator">/</span>
        <span class="crumb-parent" @click="backToList" style="cursor: pointer;">Đợt giảm giá</span>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">{{ isEditing ? 'Chi tiết đợt giảm giá' : 'Thêm đợt giảm giá' }}</span>
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

            <!-- Từ ngày & Đến ngày ngang nhau (Có cả giờ đợt giảm giá) -->
            <div class="video-date-row">
              <div class="video-form-group">
                <label class="video-form-label">Từ ngày <span class="req">*</span></label>
                <input
                  type="datetime-local"
                  v-model="formData.ngayBatDau"
                  :min="minStartDateTime"
                  :class="['video-input', { 'input-has-error': errors.ngayBatDau }]"
                  @focus="refreshMinDateTime"
                  @input="handleNgayBatDauInput"
                  @change="handleNgayBatDauChange"
                />
                <span v-if="errors.ngayBatDau" class="field-error-msg">{{ errors.ngayBatDau }}</span>
              </div>

              <div class="video-form-group">
                <label class="video-form-label">Đến ngày <span class="req">*</span></label>
                <input
                  type="datetime-local"
                  v-model="formData.ngayKetThuc"
                  :min="minEndDateTime"
                  :class="['video-input', { 'input-has-error': errors.ngayKetThuc }]"
                  @focus="refreshMinDateTime"
                  @input="handleNgayKetThucInput"
                  @change="handleNgayKetThucChange"
                />
                <span v-if="errors.ngayKetThuc" class="field-error-msg">{{ errors.ngayKetThuc }}</span>
              </div>
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

            <!-- Bảng danh sách sản phẩm để chọn (hỗ trợ mở rộng chọn từng biến thể kèm ảnh) -->
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
                    <th>Tên sản phẩm</th>
                    <th style="width: 125px;">Thương hiệu</th>
                    <th style="width: 120px;">Chất liệu</th>
                    <th style="width: 55px; text-align: center;"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(p, idx) in paginatedSourceProducts"
                    :key="p.id"
                    :class="{ 
                      'row-checked': isProductFullySelected(p),
                      'row-partially-checked': isProductPartiallySelected(p)
                    }"
                  >
                    <!-- Cột 1: Checkbox chọn toàn bộ biến thể của SP này -->
                    <td style="text-align: center;" @click.stop>
                      <input
                        type="checkbox"
                        :checked="isProductFullySelected(p)"
                        :indeterminate.prop="isProductPartiallySelected(p)"
                        @change="toggleProductSelection(p)"
                        class="custom-check"
                        title="Chọn tất cả biến thể của sản phẩm này"
                      />
                    </td>
                    <td style="text-align: center;" class="text-sub">
                      {{ (productCurrentPage - 1) * productPageSize + idx + 1 }}
                    </td>
                    <td class="font-bold text-code">{{ p.ma }}</td>
                    <td class="font-medium text-name">
                      <div class="product-cell-flex">
                        <div class="product-thumb-box">
                          <img
                            :src="getProductImage(p)"
                            :alt="p.ten"
                            class="product-thumb-img"
                            @error="(e) => { e.target.src = defaultProductImages[0] }"
                          />
                        </div>
                        <div class="product-cell-info">
                          <div class="product-name-row">
                            <span class="product-title-text">{{ p.ten }}</span>
                          </div>
                          <div class="product-sub-info">
                            <span v-if="getSelectedVariantCountForProduct(p) > 0" class="badge-selected-count">
                              Đã chọn {{ getSelectedVariantCountForProduct(p) }}/{{ p.variants ? p.variants.length : 0 }} biến thể
                            </span>
                          </div>
                        </div>
                      </div>
                    </td>
                    <td class="text-sub font-medium">{{ p.thuongHieu || '—' }}</td>
                    <td class="text-sub font-medium">{{ p.chatLieu || '—' }}</td>
                    <td style="text-align: center;" @click.stop>
                      <button
                        type="button"
                        class="btn-add-circle"
                        @click="addProductVariants(p)"
                        :title="isProductFullySelected(p) ? 'Bỏ chọn sản phẩm này' : 'Chọn sản phẩm này'"
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
            <span class="pagination-info">
              Hiển thị {{ paginatedSourceProducts.length ? ((productCurrentPage - 1) * productPageSize + 1) : 0 }} - {{ (productCurrentPage - 1) * productPageSize + paginatedSourceProducts.length }} / {{ filteredAvailableProducts.length }} sản phẩm
            </span>
            <div class="page-nav-group">
              <button
                class="btn-nav-arrow"
                :disabled="productCurrentPage <= 1"
                @click="productCurrentPage--"
                title="Trang trước"
              >‹</button>
              <span class="active-page-num" title="Trang hiện tại / Tổng số trang">{{ productCurrentPage }} / {{ sourceTotalPages }}</span>
              <button
                class="btn-nav-arrow"
                :disabled="productCurrentPage >= sourceTotalPages"
                @click="productCurrentPage++"
                title="Trang sau"
              >›</button>
            </div>
          </div>
        </div>
      </div> <!-- đóng video-right-column -->
    </div> <!-- đóng video-top-grid -->

    <!-- HÀNG DƯỚI NẰM NGANG: Thẻ sản phẩm & biến thể đã chọn (Full-width ngang bên dưới) -->
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

        <!-- Bảng chi tiết biến thể (Kèm ảnh biến thể) -->
        <div class="video-table-wrap">
          <table class="video-table">
            <thead>
              <tr>
                <th style="width: 55px; text-align: center;">STT</th>
                <th style="width: 75px; text-align: center;">Ảnh biến thể</th>
                <th style="width: 250px;">Sản phẩm</th>
                <th style="width: 230px;">Biến thể</th>
                <th style="width: 140px;">Giá bán</th>
                <th style="width: 150px;">Giá sau giảm</th>
                <th style="width: 110px; text-align: center;">Số lượng</th>
                <th style="width: 50px; text-align: center;"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(v, idx) in paginatedSelectedVariants" :key="v.id">
                <td style="text-align: center;" class="text-sub">
                  {{ (variantCurrentPage - 1) * variantPageSize + idx + 1 }}
                </td>
                <td style="text-align: center;">
                  <div class="variant-img-cell">
                    <img
                      :src="formatVariantImageUrl(v.hinhAnh)"
                      :alt="v.maBienThe"
                      class="applied-variant-thumb"
                      @error="(e) => { e.target.src = defaultVariantSvg }"
                    />
                    <!-- Badge phần trăm giảm giá hiển thị trực tiếp trên ảnh biến thể (ví dụ -30%, -25%) theo ảnh mẫu -->
                    <span
                      v-if="getVariantThumbBadge(v)"
                      class="variant-thumb-discount-badge"
                      :title="'Đang áp dụng giảm: ' + getVariantThumbBadge(v) + (getVariantCampaignInfo(v) ? ' (Đợt ' + getVariantCampaignInfo(v).maDotGiamGia + ')' : '')"
                    >
                      {{ getVariantThumbBadge(v) }}
                    </span>
                  </div>
                </td>
                <td>
                  <div class="product-cell-info">
                    <div class="font-bold text-dark">{{ v.spTen }}</div>
                    <div class="text-sub-code">{{ v.spMa }}</div>
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
                  <div>{{ formatMoney(calculateVariantDiscountedPrice(v)) }}</div>
                  <div v-if="calculateVariantDiscountPercent(v) > 0" class="text-discount-badge">
                    (-{{ calculateVariantDiscountPercent(v) }}%)
                  </div>
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
                <td colspan="8" class="empty-cell">Chưa có biến thể nào được chọn áp dụng!</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Phân trang bảng biến thể -->
        <div class="video-pagination-bar">
          <span class="pagination-info">
            Hiển thị {{ paginatedSelectedVariants.length ? ((variantCurrentPage - 1) * variantPageSize + 1) : 0 }} - {{ (variantCurrentPage - 1) * variantPageSize + paginatedSelectedVariants.length }} / {{ displaySelectedVariants.length }} biến thể
          </span>
          <div class="page-nav-group">
            <button
              class="btn-nav-arrow"
              :disabled="variantCurrentPage <= 1"
              @click="variantCurrentPage--"
              title="Trang trước"
            >‹</button>
            <span class="active-page-num" title="Trang hiện tại / Tổng số trang">{{ variantCurrentPage }} / {{ variantTotalPages }}</span>
            <button
              class="btn-nav-arrow"
              :disabled="variantCurrentPage >= variantTotalPages"
              @click="variantCurrentPage++"
              title="Trang sau"
            >›</button>
          </div>
        </div>
      </div> <!-- đóng card-applied-variants -->
    </div> <!-- đóng view-form -->

    <!-- ========================================================
         MODAL: XÁC NHẬN CẬP NHẬT / TẠO MỚI (Y NHƯ ẢNH MẪU 1)
    ======================================================== -->
    <!-- ========================================================
         MODAL 1: XÁC NHẬN LƯU / CẬP NHẬT ĐỢT GIẢM GIÁ
    ======================================================== -->
    <div v-if="showConfirmModal" class="modal-mask confirm-modal-overlay" @click.self="showConfirmModal = false">
      <div class="confirm-modal-box">
        <div class="confirm-modal-header">
          <div class="confirm-warning-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
          </div>
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">{{ isEditing ? 'Xác nhận cập nhật' : 'Xác nhận tạo mới' }}</h3>
            <p class="confirm-subtitle">Vui lòng kiểm tra lại thông tin đợt giảm giá trước khi lưu</p>
          </div>
          <button class="modal-close-btn" @click="showConfirmModal = false">✕</button>
        </div>

        <div class="confirm-modal-body">
          <p class="confirm-message-text">
            Bạn có chắc chắn muốn lưu các thay đổi cho đợt giảm giá
            <b class="text-blue">[{{ formData.maDotGiamGia || 'MỚI' }}]</b> không?
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

        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="showConfirmModal = false">
            Hủy bỏ
          </button>
          <button class="btn btn-save-confirm" :disabled="submitting" @click="doSubmitAPI">
            {{ submitting ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========================================================
         MODAL 2: XÁC NHẬN ĐỔI TRẠNG THÁI (CHUẨN HÌNH ẢNH MẪU)
    ======================================================== -->
    <div v-if="showToggleStatusModal && selectedToggleItem" class="modal-mask confirm-modal-overlay" @click.self="showToggleStatusModal = false">
      <div class="confirm-modal-box">
        <!-- Header -->
        <div class="confirm-modal-header">
          <div class="confirm-warning-icon">
            <svg v-if="selectedToggleItem.trangThai === 1" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"></path>
            </svg>
          </div>
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">
              {{ selectedToggleItem.trangThai === 1 ? 'Xác nhận ngừng hoạt động' : 'Xác nhận kích hoạt hoạt động' }}
            </h3>
            <p class="confirm-subtitle">
              {{ selectedToggleItem.trangThai === 1 ? 'Tạm ngưng hiệu lực của đợt giảm giá' : 'Kích hoạt hiệu lực của đợt giảm giá' }}
            </p>
          </div>
          <button class="modal-close-btn" @click="showToggleStatusModal = false" :disabled="togglingStatus">✕</button>
        </div>

        <!-- Body -->
        <div class="confirm-modal-body">
          <p class="confirm-message-text" v-if="selectedToggleItem.trangThai === 1">
            Bạn có chắc chắn muốn chuyển đợt giảm giá
            <b class="text-blue">[{{ selectedToggleItem.maDotGiamGia || '—' }}]</b> sang trạng thái
            <b class="text-highlight">Ngừng hoạt động</b> không?
          </p>
          <p class="confirm-message-text" v-else>
            Bạn có chắc chắn muốn
            <b class="text-activate">Kích hoạt lại</b> đợt giảm giá
            <b class="text-blue">[{{ selectedToggleItem.maDotGiamGia || '—' }}]</b> để áp dụng cho sản phẩm không?
          </p>
        </div>

        <!-- Footer -->
        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="showToggleStatusModal = false" :disabled="togglingStatus">
            Hủy bỏ
          </button>
          <button
              class="btn btn-save-confirm"
              :disabled="togglingStatus"
              @click="confirmToggleStatus"
          >
            {{ togglingStatus ? 'Đang cập nhật...' : (selectedToggleItem.trangThai === 1 ? 'Ngừng hoạt động' : 'Kích hoạt đợt') }}
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
const originalNgayBatDau = ref('')
const originalNgayKetThuc = ref('')
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

// Lấy chuỗi ngày giờ hiện tại chuẩn YYYY-MM-DDTHH:mm
const getCurrentDateTimeString = () => {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

// Lấy mốc timestamp đầu phút hiện tại (bỏ qua giây để so khớp chuẩn từng phút)
const getCurrentMinuteFloorMs = () => {
  const d = new Date()
  d.setSeconds(0, 0)
  return d.getTime()
}

// Mốc thời gian tối thiểu cho form Từ ngày & Đến ngày (khóa không cho chọn trước giờ hiện tại)
const formMinDateTime = ref('')

const refreshMinDateTime = () => {
  formMinDateTime.value = getCurrentDateTimeString()
}

const minStartDateTime = computed(() => {
  return formMinDateTime.value || getCurrentDateTimeString()
})
const minStartDate = minStartDateTime

// Mốc thời gian tối thiểu cho Đến ngày (phải từ ngày bắt đầu hoặc mốc thời gian hiện tại)
const minEndDateTime = computed(() => {
  if (formData.value.ngayBatDau) {
    const startMs = new Date(formData.value.ngayBatDau).getTime()
    const nowMs = getCurrentMinuteFloorMs()
    if (!isNaN(startMs) && startMs > nowMs) {
      return formData.value.ngayBatDau
    }
  }
  return formMinDateTime.value || getCurrentDateTimeString()
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

// Ảnh SVG placeholder khi biến thể chưa có ảnh riêng (không lấy ảnh biến thể khác gắn vào)
const defaultVariantSvg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1.5'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E"

const formatVariantImageUrl = (url) => {
  if (!url || typeof url !== 'string' || url.trim() === '') return defaultVariantSvg
  let cleanUrl = url.trim()
  if (cleanUrl.includes('|')) {
    cleanUrl = cleanUrl.split('|')[1].trim()
  }
  if (!cleanUrl) return defaultVariantSvg
  if (cleanUrl.startsWith('data:') || cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) return cleanUrl
  const origin = (typeof window !== 'undefined' && window.location.hostname) ? `http://${window.location.hostname}:8080` : 'http://localhost:8080'
  return `${origin}${cleanUrl.startsWith('/') ? '' : '/'}${cleanUrl}`
}

// Ảnh sản phẩm mặc định chuẩn áo phông
const defaultProductImages = [
  'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=300&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=300&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=300&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=300&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1622445262464-84b14e3295b6?w=300&auto=format&fit=crop&q=80'
]

const getFallbackProductImage = (id, code) => {
  let num = typeof id === 'number' ? id : 0
  if (!num && code) {
    const digits = String(code).replace(/\D/g, '')
    num = digits ? parseInt(digits.slice(-4)) : 0
  }
  const idx = Math.abs(num) % defaultProductImages.length
  return defaultProductImages[idx]
}

const resolveImageUrl = (url, fallbackId, fallbackCode) => {
  if (!url || typeof url !== 'string' || url.trim() === '') {
    return getFallbackProductImage(fallbackId, fallbackCode)
  }
  const trimmed = url.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('data:image')) {
    return trimmed
  }
  if (trimmed.startsWith('/')) {
    return `http://localhost:8080${trimmed}`
  }
  return `http://localhost:8080/${trimmed}`
}

const getProductImage = (item) => {
  if (!item) return defaultProductImages[0]
  const rawImg = item.spHinhAnh || item.hinhAnh || item.anhDaiDien || item.image
  if (rawImg && typeof rawImg === 'string' && rawImg.trim() !== '') {
    return resolveImageUrl(rawImg, item.spId || item.id, item.spMa || item.ma)
  }
  const found = productSourceList.value.find(p => p.id === (item.spId || item.id) || (p.ma && p.ma === (item.spMa || item.ma)))
  if (found && (found.hinhAnh || found.anhDaiDien || found.image)) {
    return resolveImageUrl(found.hinhAnh || found.anhDaiDien || found.image, item.spId || item.id, item.spMa || item.ma)
  }
  return getFallbackProductImage(item.spId || item.id, item.spMa || item.ma)
}

// Danh sách sản phẩm nguồn chuẩn theo CSDL FFTShirt (Áo thun)
const productSourceList = ref([
  {
    id: 1,
    ma: 'SP001',
    ten: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    hinhAnh: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300&auto=format&fit=crop&q=80',
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
    hinhAnh: 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=300&auto=format&fit=crop&q=80',
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
    hinhAnh: 'https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=300&auto=format&fit=crop&q=80',
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
    hinhAnh: 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=300&auto=format&fit=crop&q=80',
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
    hinhAnh: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=300&auto=format&fit=crop&q=80',
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

// Kiểm tra xem sản phẩm cha có đang được chọn áp dụng trong đợt này hay không
const isProductSelected = (p) => {
  if (!p) return false
  return !!p.selected || selectedVariants.value.some(v => v.spId === p.id)
}

// Rút gọn số tiền hiển thị (ví dụ: 50.000 -> 50k, 1.500.000 -> 1.5tr)
const formatShortMoney = (val) => {
  if (!val) return '0'
  const num = Number(val)
  if (isNaN(num)) return String(val)
  if (num >= 1000000) {
    const tr = num / 1000000
    return (tr % 1 === 0 ? tr : tr.toFixed(1)) + 'tr'
  }
  if (num >= 1000) {
    const k = num / 1000
    return (k % 1 === 0 ? k : k.toFixed(0)) + 'k'
  }
  return num.toLocaleString('vi-VN') + 'đ'
}

// Lấy giá trị giảm của đợt giảm giá đang áp dụng (hiển thị ở góc trên phải ảnh sản phẩm)
const getActiveCampaignDiscountBadge = (pOrV) => {
  if (!pOrV) return ''
  const pId = pOrV.spId || pOrV.id
  if (!pId) return ''
  let camp = getProductActiveCampaign({ id: pId })
  if (!camp) return ''
  if (camp.phanTramGiam == null && campaigns.value && campaigns.value.length > 0) {
    const fullCamp = campaigns.value.find(c => c.id === camp.id || (c.maDotGiamGia && c.maDotGiamGia === camp.maDotGiamGia))
    if (fullCamp) camp = fullCamp
  }
  if (camp.loaiGiamGia === 2 && camp.giaTriGiam) {
    return `-${formatShortMoney(camp.giaTriGiam)}`
  }
  if (camp.phanTramGiam != null && Number(camp.phanTramGiam) > 0) {
    const num = Number(camp.phanTramGiam)
    return `-${num % 1 === 0 ? num : num.toFixed(0)}%`
  }
  if (camp.giaTriGiam != null && Number(camp.giaTriGiam) > 0) {
    return `-${formatShortMoney(camp.giaTriGiam)}`
  }
  return ''
}

// Mức trừ phần trăm theo giá trị được nhập trong form hiện tại (hiển thị ở góc dưới phải ảnh sản phẩm)
const currentFormDiscountBadge = computed(() => {
  const val = Number(formData.value.phanTramGiam)
  if (val && val > 0) {
    return `-${val % 1 === 0 ? val : val.toFixed(0)}%`
  }
  if (formData.value.loaiGiamGia === 2) {
    const cashVal = Number(formData.value.giaTriGiam)
    if (cashVal && cashVal > 0) {
      return `-${formatShortMoney(cashVal)}`
    }
  }
  return ''
})

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

// Lọc và sắp xếp sản phẩm ở bảng chọn trên
const filteredAvailableProducts = computed(() => {
  const kw = productSearchText.value.trim().toLowerCase()
  return productSourceList.value.filter(p => {
    return !kw || (p.ma && p.ma.toLowerCase().includes(kw)) || (p.ten && p.ten.toLowerCase().includes(kw))
  })
})

// Tự động về trang 1 khi người dùng gõ tìm kiếm sản phẩm
watch(productSearchText, () => {
  productCurrentPage.value = 1
})


// Kiểm tra 1 biến thể có đang được chọn hay không
const isVariantSelected = (vId) => {
  return selectedVariants.value.some(sv => sv.id === vId)
}

// Số biến thể đã chọn của 1 sản phẩm
const getSelectedVariantCountForProduct = (p) => {
  if (!p || !p.variants) return 0
  return p.variants.filter(v => isVariantSelected(v.id)).length
}

// Kiểm tra sản phẩm đã được chọn toàn bộ biến thể hay chưa
const isProductFullySelected = (p) => {
  if (!p || !p.variants || p.variants.length === 0) return false
  return p.variants.every(v => isVariantSelected(v.id))
}

// Kiểm tra sản phẩm đã chọn một phần biến thể (Indeterminate)
const isProductPartiallySelected = (p) => {
  const c = getSelectedVariantCountForProduct(p)
  return c > 0 && c < (p.variants?.length || 0)
}

// Đếm tổng số sản phẩm cha có ít nhất 1 biến thể được chọn
const selectedProductCount = computed(() => {
  const spIds = new Set(selectedVariants.value.map(sv => sv.spId))
  return spIds.size
})

// Kiểm tra chọn tất cả sản phẩm
const isAllProductsChecked = computed(() => {
  if (!filteredAvailableProducts.value.length) return false
  return filteredAvailableProducts.value.every(p => isProductFullySelected(p))
})

// Chọn / bỏ chọn 1 biến thể riêng lẻ
const toggleVariantSelection = (p, v) => {
  const idx = selectedVariants.value.findIndex(sv => sv.id === v.id)
  if (idx > -1) {
    selectedVariants.value.splice(idx, 1)
  } else {
    if (errors.value.selectedVariants) {
      delete errors.value.selectedVariants
    }
    selectedVariants.value.push({
      id: v.id,
      spId: p.id,
      spMa: p.ma,
      spTen: p.ten,
      spHinhAnh: p.hinhAnh || '',
      hinhAnh: v.hinhAnh || '',
      maBienThe: v.maBienThe,
      mauSac: v.mauSac,
      kichCo: v.kichCo,
      gia: v.gia,
      soLuong: v.soLuong
    })
  }
  p.selected = isProductFullySelected(p)
}

// Chọn / bỏ chọn toàn bộ biến thể của 1 sản phẩm
const toggleProductSelection = (p) => {
  const isFully = isProductFullySelected(p)
  if (isFully) {
    selectedVariants.value = selectedVariants.value.filter(sv => sv.spId !== p.id)
    p.selected = false
  } else {
    if (errors.value.selectedVariants) {
      delete errors.value.selectedVariants
    }
    p.variants.forEach(v => {
      if (!isVariantSelected(v.id)) {
        selectedVariants.value.push({
          id: v.id,
          spId: p.id,
          spMa: p.ma,
          spTen: p.ten,
          spHinhAnh: p.hinhAnh || '',
          hinhAnh: v.hinhAnh || '',
          maBienThe: v.maBienThe,
          mauSac: v.mauSac,
          kichCo: v.kichCo,
          gia: v.gia,
          soLuong: v.soLuong
        })
      }
    })
    p.selected = true
  }
}

// Nút (+) chọn tất cả biến thể của sản phẩm
const addProductVariants = (p) => {
  toggleProductSelection(p)
}

// Xóa 1 biến thể khỏi bảng đã chọn bên dưới
const removeSelectedVariant = (vId) => {
  const item = selectedVariants.value.find(v => v.id === vId)
  selectedVariants.value = selectedVariants.value.filter(v => v.id !== vId)
  if (item) {
    const prod = productSourceList.value.find(p => p.id === item.spId)
    if (prod) {
      prod.selected = isProductFullySelected(prod)
    }
  }
}

// Chọn tất cả sản phẩm
const toggleSelectAllSourceProducts = () => {
  const willSelectAll = !isAllProductsChecked.value
  if (willSelectAll && errors.value.selectedVariants) {
    delete errors.value.selectedVariants
  }
  filteredAvailableProducts.value.forEach(p => {
    p.selected = willSelectAll
    if (willSelectAll) {
      p.variants.forEach(v => {
        if (!isVariantSelected(v.id)) {
          selectedVariants.value.push({
            id: v.id,
            spId: p.id,
            spMa: p.ma,
            spTen: p.ten,
            spHinhAnh: p.hinhAnh || '',
            hinhAnh: v.hinhAnh || '',
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

// Map các biến thể đang trong đợt giảm giá từ backend
const activeVariantsMap = ref({})
const loadActiveVariantsMap = async (excludeDotId = null) => {
  try {
    const params = {}
    if (excludeDotId) params.excludeDotId = excludeDotId
    const res = await api.get('/api/dot-giam-gia/active-variants-map', { params })
    if (res.data) {
      activeVariantsMap.value = res.data
    }
  } catch (e) {
  }
}

// Lấy thông tin đợt giảm giá đang áp dụng cho biến thể (chỉ xét chính biến thể đó)
const getVariantCampaignInfo = (v) => {
  if (!v) return null
  // Kiểm tra trực tiếp theo id biến thể trong activeVariantsMap
  if (v.id && activeVariantsMap.value && activeVariantsMap.value[v.id]) {
    return activeVariantsMap.value[v.id]
  }
  return null
}

const getVariantActiveCampaign = (variantId) => {
  if (!variantId) return null
  return activeVariantsMap.value[variantId] || null
}

const getVariantCampaignDiscountText = (v) => {
  const info = getVariantCampaignInfo(v)
  if (!info) return ''
  if (info.phanTramGiam != null && Number(info.phanTramGiam) > 0) {
    return `-${info.phanTramGiam}%`
  }
  if (info.giaTriGiam != null && Number(info.giaTriGiam) > 0) {
    return `-${formatShortMoney(info.giaTriGiam)}`
  }
  return ''
}

// Tính giá sau giảm cho từng biến thể cụ thể (xét cả đợt đang tạo/sửa và đợt đang chạy)
const calculateVariantDiscountedPrice = (v) => {
  if (!v) return 0
  const originalPrice = Number(v.gia) || 0
  if (originalPrice <= 0) return 0

  // 1. Mức giảm tính từ form hiện tại
  let formDiscountAmount = 0
  const formPercent = Number(formData.value.phanTramGiam) || 0
  const formCash = Number(formData.value.giaTriGiam) || 0

  if (formData.value.loaiGiamGia === 2 && formCash > 0) {
    formDiscountAmount = formCash
  } else if (formPercent > 0) {
    formDiscountAmount = (originalPrice * formPercent) / 100
    if (formData.value.giamToiDa && Number(formData.value.giamToiDa) > 0) {
      formDiscountAmount = Math.min(formDiscountAmount, Number(formData.value.giamToiDa))
    }
  }

  // 2. Mức giảm từ đợt khác đang áp dụng cho biến thể này
  let activeDiscountAmount = 0
  const activeCamp = getVariantCampaignInfo(v)
  if (activeCamp) {
    const activePercent = Number(activeCamp.phanTramGiam) || 0
    const activeCash = Number(activeCamp.giaTriGiam) || 0
    if (activeCamp.loaiGiamGia === 2 && activeCash > 0) {
      activeDiscountAmount = activeCash
    } else if (activePercent > 0) {
      activeDiscountAmount = (originalPrice * activePercent) / 100
      if (activeCamp.giamToiDa && Number(activeCamp.giamToiDa) > 0) {
        activeDiscountAmount = Math.min(activeDiscountAmount, Number(activeCamp.giamToiDa))
      }
    }
  }

  // 3. Lấy mức giảm cao nhất (Max Discount) giữa form mới và đợt đang áp dụng
  const finalDiscount = Math.max(formDiscountAmount, activeDiscountAmount)
  return Math.max(0, Math.round(originalPrice - finalDiscount))
}

// Tính phần trăm giảm thực tế đang được hưởng
const calculateVariantDiscountPercent = (v) => {
  if (!v || !v.gia) return 0
  const originalPrice = Number(v.gia) || 0
  const discounted = calculateVariantDiscountedPrice(v)
  if (originalPrice <= 0 || discounted >= originalPrice) return 0
  const pct = ((originalPrice - discounted) / originalPrice) * 100
  return pct % 1 === 0 ? pct : Number(pct.toFixed(1))
}

// Lấy nhãn phần trăm giảm giá hiển thị trên ảnh biến thể, luôn lấy mức giảm sâu nhất (MAX discount)
const getVariantThumbBadge = (v) => {
  if (!v) return ''
  // 1. Luôn ưu tiên mức giảm sâu nhất thực tế đang được hưởng (giữa đợt trong form và đợt khác)
  const pct = calculateVariantDiscountPercent(v)
  if (pct > 0) return `-${pct}%`
  // 2. Dự phòng nếu không tính được % từ giá thì lấy từ đợt đang áp dụng
  const activeText = getVariantCampaignDiscountText(v)
  if (activeText) return activeText
  return ''
}

// Tính giá sau giảm (fallback)
const calculateDiscountedPrice = (originalPrice) => {
  const percent = Number(formData.value.phanTramGiam) || 0
  const discount = (originalPrice * percent) / 100
  return Math.max(0, Math.round(originalPrice - discount))
}

// Lọc hiển thị ở bảng biến thể dưới
const displaySelectedVariants = computed(() => {
  return selectedVariants.value.filter(v => {
    const kw = selectedFilterKeyword.value.trim().toLowerCase()
    const matchKw = !kw ||
      (v.spTen && v.spTen.toLowerCase().includes(kw)) ||
      (v.spMa && v.spMa.toLowerCase().includes(kw)) ||
      (v.maBienThe && v.maBienThe.toLowerCase().includes(kw))
    
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

// Kiểm tra Từ ngày ngay khi người dùng chọn/nhập
const validateNgayBatDauValue = (rawVal, isFromInput = false) => {
  clearError('ngayBatDau')
  if (!rawVal || rawVal.trim() === '') {
    if (!isFromInput) {
      errors.value.ngayBatDau = 'Từ ngày không được để trống!'
    }
    return
  }
  const startMs = new Date(rawVal).getTime()
  if (isNaN(startMs)) return

  const isOriginalStart = isEditing.value && originalNgayBatDau.value && rawVal === toInputDateTime(originalNgayBatDau.value)
  if (!isOriginalStart) {
    const minMs = getCurrentMinuteFloorMs()
    if (startMs < minMs) {
      // Lập tức khóa cứng không cho chọn ngược lại thời gian hiện tại
      const currentStr = getCurrentDateTimeString()
      formData.value.ngayBatDau = currentStr
      errors.value.ngayBatDau = 'Thời gian bắt đầu không được trước thời gian hiện tại!'
      showToast('Thời gian bắt đầu không được trước thời gian hiện tại!', 'error')
      return
    }
  }

  if (formData.value.ngayKetThuc) {
    const endMs = new Date(formData.value.ngayKetThuc).getTime()
    if (!isNaN(endMs) && endMs <= startMs) {
      errors.value.ngayKetThuc = 'Đến ngày phải sau thời gian bắt đầu!'
    } else {
      clearError('ngayKetThuc')
    }
  }
}

const handleNgayBatDauInput = (e) => {
  const val = e?.target?.value || formData.value.ngayBatDau
  validateNgayBatDauValue(val, true)
}

const handleNgayBatDauChange = (e) => {
  const val = e?.target?.value || formData.value.ngayBatDau
  validateNgayBatDauValue(val, false)
}

// Kiểm tra Đến ngày ngay khi người dùng chọn/nhập
const validateNgayKetThucValue = (rawVal, isFromInput = false) => {
  clearError('ngayKetThuc')
  if (!rawVal || rawVal.trim() === '') {
    if (!isFromInput) {
      errors.value.ngayKetThuc = 'Đến ngày không được để trống!'
    }
    return
  }
  const endMs = new Date(rawVal).getTime()
  if (isNaN(endMs)) return

  const minMs = getCurrentMinuteFloorMs()
  if (endMs < minMs) {
    errors.value.ngayKetThuc = 'Thời gian kết thúc không được trước thời gian hiện tại!'
    showToast('Thời gian kết thúc không được trước thời gian hiện tại!', 'error')
    formData.value.ngayKetThuc = ''
    return
  }

  if (formData.value.ngayBatDau) {
    const startMs = new Date(formData.value.ngayBatDau).getTime()
    if (!isNaN(startMs) && endMs <= startMs) {
      errors.value.ngayKetThuc = 'Đến ngày phải sau thời gian bắt đầu!'
      showToast('Đến ngày phải sau thời gian bắt đầu!', 'error')
      formData.value.ngayKetThuc = ''
      return
    } else {
      clearError('ngayKetThuc')
    }
  }
}

const handleNgayKetThucInput = (e) => {
  const val = e?.target?.value || formData.value.ngayKetThuc
  validateNgayKetThucValue(val, true)
}

const handleNgayKetThucChange = (e) => {
  const val = e?.target?.value || formData.value.ngayKetThuc
  validateNgayKetThucValue(val, false)
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

// Helper ngày giờ an toàn hỗ trợ cả chuỗi ISO, timestamp và mảng Java LocalDateTime [yyyy, mm, dd, hh, mm]
const parseDateHelper = (val) => {
  if (!val) return null
  if (Array.isArray(val)) {
    const [y, m, d, h = 0, min = 0, s = 0] = val
    return new Date(y, m - 1, d, h, min, s)
  }
  const d = new Date(val)
  return isNaN(d.getTime()) ? null : d
}

const toInputDateTime = (val) => {
  const d = parseDateHelper(val)
  if (!d) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
const toInputDate = (val) => {
  const d = parseDateHelper(val)
  if (!d) return ''
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const formatDate = (val) => {
  if (!val) return '—'
  const date = parseDateHelper(val)
  if (!date) return val
  const day = date.getDate()
  const month = date.getMonth() + 1
  const year = date.getFullYear()
  return `${day}/${month}/${year}`
}

const formatDateTime = (val) => {
  if (!val) return '—'
  const date = parseDateHelper(val)
  if (!date) return val
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
  const now = currentTime.value
  const start = item.ngayBatDau ? new Date(item.ngayBatDau).getTime() : null
  const end = item.ngayKetThuc ? new Date(item.ngayKetThuc).getTime() : null

  // 1. Nếu đã qua ngày kết thúc -> Đã kết thúc
  if (end && now >= end) {
    return { code: 'ENDED', text: 'Đã kết thúc', class: 'status-pill-ended' }
  }
  // 2. Nếu trạng thái bằng 0 (người dùng tắt) -> Ngừng hoạt động
  if (item.trangThai === 0) {
    return { code: 'INACTIVE', text: 'Ngừng hoạt động', class: 'status-pill-inactive' }
  }
  // 3. Nếu chưa tới ngày bắt đầu -> Sắp diễn ra
  if (start && now < start) {
    return { code: 'UPCOMING', text: 'Sắp diễn ra', class: 'status-pill-upcoming' }
  }
  // 4. Đang trong khoảng thời gian hiệu lực -> Đang diễn ra
  return { code: 'ACTIVE', text: 'Đang diễn ra', class: 'status-pill-active' }
}

// Danh sách đợt giảm giá hiển thị tự động cập nhật theo currentTime mỗi giây
const displayCampaigns = computed(() => {
  const _now = currentTime.value
  return (campaigns.value || []).map(item => ({
    ...item,
    _status: getCampaignStatus(item)
  }))
})

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

// Load dữ liệu (hỗ trợ isSilent = true để tự động cập nhật ngầm mà không nháy màn hình)
const fetchData = async (isSilent = false) => {
  try {
    if (!isSilent) loading.value = true
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
        list = list.filter(item => getCampaignStatus(item).code === 'INACTIVE')
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
    if (!isSilent) {
      errorMessage.value = 'Không thể tải dữ liệu đợt giảm giá từ database. Vui lòng kiểm tra backend và kết nối SQL Server!'
      campaigns.value = []
      totalPages.value = 0
      totalElements.value = 0
    }
  } finally {
    if (!isSilent) loading.value = false
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

// Xử lý bấm công tắc đổi trạng thái
const onToggleSwitchClick = (item) => {
  if (getCampaignStatus(item).code === 'ENDED') {
    showToast('Đợt giảm giá này đã kết thúc, không thể thay đổi trạng thái!', 'warning')
    return
  }
  openToggleStatusModal(item)
}

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
  originalNgayBatDau.value = ''
  originalNgayKetThuc.value = ''
  currentView.value = 'form'

  const currentStr = getCurrentDateTimeString()
  formMinDateTime.value = currentStr

  formData.value = {
    id: null,
    maDotGiamGia: '', // Để trống để làm mờ theo yêu cầu, hệ thống sẽ tự sinh nếu không nhập
    tenDotGiamGia: '',
    loaiGiamGia: 1, // 1: Giảm theo %, 2: Giảm theo tiền
    phanTramGiam: null,
    giaTriGiam: null,
    giamToiDa: null,
    ngayBatDau: currentStr,
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
  showToast('Chuyển sang màn hình tạo đợt giảm giá!', 'info')

  try {
    await Promise.all([
      loadActiveCampaignProducts(null),
      loadActiveVariantsMap(null)
    ])
  } catch (err) {
    console.warn('Lỗi tải sản phẩm đang áp dụng:', err)
  }
}

// Chuyển sang màn hình chỉnh sửa
const openEditView = async (item) => {
  errors.value = {}
  isEditing.value = true
  originalNgayBatDau.value = item.ngayBatDau || ''
  originalNgayKetThuc.value = item.ngayKetThuc || ''
  currentView.value = 'form'

  formMinDateTime.value = getCurrentDateTimeString()

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

  try {
    await Promise.all([
      loadActiveCampaignProducts(item.id),
      loadActiveVariantsMap(item.id)
    ])
  } catch (err) {
    console.warn('Lỗi tải sản phẩm đang áp dụng:', err)
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
      if (res.data.ngayBatDau) {
        formData.value.ngayBatDau = toInputDateTime(res.data.ngayBatDau)
        originalNgayBatDau.value = res.data.ngayBatDau
      }
      if (res.data.ngayKetThuc) {
        formData.value.ngayKetThuc = toInputDateTime(res.data.ngayKetThuc)
        originalNgayKetThuc.value = res.data.ngayKetThuc
      }
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
        const matchedP = productSourceList.value.find(p => p.id === (sp.id || 1) || (p.ma && p.ma === sp.maSanPham))
        const matchedV = matchedP?.variants?.find(v => v.id === (ctsp.id || ct.id))
        return {
          id: ctsp.id || ct.id,
          spId: sp.id || (matchedP ? matchedP.id : 1),
          spMa: sp.maSanPham || matchedP?.ma || ('SP' + (sp.id || '')),
          spTen: sp.tenSanPham || matchedP?.ten || 'Sản phẩm',
          spHinhAnh: matchedP?.hinhAnh || sp.anhDaiDien || '',
          hinhAnh: matchedV?.hinhAnh || ctsp.anhDaiDien || '',
          maBienThe: ctsp.maChiTietSanPham || matchedV?.maBienThe || ('BT-' + ct.id),
          mauSac: ctsp.idMauSac?.tenMauSac || matchedV?.mauSac || 'Xám',
          kichCo: ctsp.idKichCo?.tenKichCo || matchedV?.kichCo || '37',
          gia: ctsp.giaBan || matchedV?.gia || 0,
          soLuong: ctsp.soLuong || matchedV?.soLuong || 0
        }
      })
      const selectedSpIds = new Set(selectedVariants.value.map(v => v.spId))
      saveCampaignProductIds(item.id, item.maDotGiamGia, Array.from(selectedSpIds))
      productSourceList.value.forEach(p => {
        p.selected = isProductFullySelected(p)
      })
    } else {
      selectedVariants.value = []
      productSourceList.value.forEach(p => {
        p.selected = false
      })
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
      errors.value.ngayKetThuc = 'Đến ngày phải sau thời gian bắt đầu!'
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

  // 3. Kiểm tra Từ ngày
  if (!formData.value.ngayBatDau || formData.value.ngayBatDau.trim() === '') {
    errors.value.ngayBatDau = 'Từ ngày không được để trống!'
    hasError = true
  } else {
    const startMs = new Date(formData.value.ngayBatDau).getTime()
    const isOriginalStart = isEditing.value && originalNgayBatDau.value && formData.value.ngayBatDau === toInputDateTime(originalNgayBatDau.value)
    if (!isOriginalStart && startMs < getCurrentMinuteFloorMs()) {
      errors.value.ngayBatDau = 'Thời gian bắt đầu không được trước thời gian hiện tại!'
      hasError = true
    }
  }

  // 4. Kiểm tra Đến ngày (Bắt buộc, không cho để trống)
  if (!formData.value.ngayKetThuc || formData.value.ngayKetThuc.trim() === '') {
    errors.value.ngayKetThuc = 'Đến ngày không được để trống!'
    hasError = true
  } else {
    const endMs = new Date(formData.value.ngayKetThuc).getTime()
    if (endMs < getCurrentMinuteFloorMs()) {
      errors.value.ngayKetThuc = 'Thời gian kết thúc không được trước thời gian hiện tại!'
      hasError = true
    } else if (formData.value.ngayBatDau) {
      if (endMs <= new Date(formData.value.ngayBatDau).getTime()) {
        errors.value.ngayKetThuc = 'Đến ngày phải sau thời gian bắt đầu!'
        hasError = true
      }
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

  // Thông báo nếu có biến thể đang trùng đợt giảm giá khác chạy song song
  const hasOverlapping = selectedVariants.value.some(v => getVariantActiveCampaign(v.id))
  if (hasOverlapping) {
    showToast('Một số biến thể đang thuộc đợt giảm giá khác. Hệ thống sẽ tự động áp dụng mức giảm tối đa (Max)!', 'info')
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
      ngayBatDau: formData.value.ngayBatDau.length === 10
        ? new Date(formData.value.ngayBatDau + 'T00:00:00').toISOString()
        : new Date(formData.value.ngayBatDau).toISOString(),
      ngayKetThuc: formData.value.ngayKetThuc
        ? (formData.value.ngayKetThuc.length === 10
            ? new Date(formData.value.ngayKetThuc + 'T23:59:59').toISOString()
            : new Date(formData.value.ngayKetThuc).toISOString())
        : null,
      moTa: formData.value.moTa?.trim() || null,
      trangThai: formData.value.trangThai,
      chiTietSanPhamIds: chiTietSanPhamIds,
      sanPhamIds: [] // Chỉ lưu chính xác các biến thể đã chọn, không lưu cả sản phẩm cha
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
        const spImg = sp.anhDaiDien || (sp.hinhAnhs && sp.hinhAnhs[0]) || sp.image || sp.hinhAnh || ''
        try {
          const varRes = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${sp.id}`)
          if (varRes.data && varRes.data.length > 0) {
            variants = varRes.data.map(v => ({
              id: v.id,
              maBienThe: v.maChiTietSanPham || ('CTSP' + v.id),
              mauSac: v.tenMauSac || v.idMauSac?.tenMauSac || 'Đen',
              kichCo: v.tenKichCo || v.idKichCo?.tenKichCo || 'M',
              gia: v.giaBan || 199000,
              soLuong: v.soLuong || 50,
              hinhAnh: v.anhDaiDien || ''
            }))
          }
        } catch (e) {
        }
        return {
          id: sp.id,
          ma: sp.maSanPham || ('SP' + sp.id),
          ten: sp.tenSanPham || 'Áo phông FFTShirt',
          hinhAnh: spImg,
          thuongHieu: sp.tenThuongHieu || sp.idThuongHieu?.tenThuongHieu || 'FF Shirt',
          chatLieu: sp.tenChatLieu || sp.idChatLieu?.tenChatLieu || 'Cotton 100%',
          gia: sp.giaBan || (variants[0]?.gia || 199000),
          selected: false,
          variants: variants.length > 0 ? variants : [
            { id: sp.id * 10 + 1, maBienThe: (sp.maSanPham || 'SP') + '-M', mauSac: 'Đen', kichCo: 'M', gia: 199000, soLuong: 50, hinhAnh: spImg }
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
  loadActiveVariantsMap()

  // Real-time ticker mỗi giây và đồng bộ dữ liệu ngầm tự động (tự động cập nhật không cần load lại trang)
  let pollCounter = 0
  realTimeTimer = setInterval(() => {
    currentTime.value = Date.now()
    pollCounter++
    // Mỗi 3 giây tự động đồng bộ ngầm dữ liệu danh sách và sản phẩm đang áp dụng
    if (pollCounter >= 3) {
      pollCounter = 0
      if (currentView.value === 'list' && !loading.value && !submitting.value && !togglingStatus.value) {
        fetchData(true)
        loadActiveCampaignProducts()
        loadActiveVariantsMap()
      }
    }
  }, 1000)
})

onUnmounted(() => {
  if (realTimeTimer) clearInterval(realTimeTimer)
})
</script>

<style scoped>
.dot-giam-gia-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Toast Notification trên góc phải màn hình */
.toast-msg {
  position: fixed;
  top: 18px;
  right: 22px;
  min-width: 250px;
  max-width: 360px;
  padding: 0.65rem 0.95rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 8px 18px -4px rgba(0, 0, 0, 0.1), 0 3px 6px -3px rgba(0, 0, 0, 0.04);
  z-index: 999999;
}
.toast-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.toast-content {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
}
.toast-title {
  font-size: 0.82rem;
  font-weight: 750;
  color: #111827;
  line-height: 1.25;
}
.toast-text {
  font-size: 0.78rem;
  font-weight: 500;
  color: #4b5563;
  line-height: 1.35;
}
.toast-btn-close {
  background: transparent;
  border: none;
  font-size: 0.85rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 2px 4px;
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
  border-left: 3.5px solid #16a34a;
}
.toast-success .toast-icon {
  background-color: #f0fdf4;
  color: #16a34a;
}
.toast-error {
  border-left: 3.5px solid #dc2626;
}
.toast-error .toast-icon {
  background-color: #fef2f2;
  color: #dc2626;
}
.toast-info {
  border-left: 3.5px solid #2563eb;
}
.toast-info .toast-icon {
  background-color: #eff6ff;
  color: #2563eb;
}

/* Hiệu ứng trượt từ góc trên phải */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}
.toast-slide-enter-from {
  opacity: 0;
  transform: translateY(-25px) translateX(35px);
}
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(-25px) translateX(35px);
}

/* 1. Thanh tiêu đề trên cùng (Breadcrumb) */
.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  margin-bottom: 20px;
}
.crumb-parent {
  color: #647074;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s ease;
}
.crumb-parent:hover {
  color: var(--blue, #496883);
  text-decoration: underline;
}
.crumb-separator {
  color: #b0b7bd;
}
.crumb-current {
  color: var(--blue, #496883);
  font-weight: 700;
}
.btn-back-pill {
  padding: 0.35rem 0.85rem;
  border-radius: 20px;
  border: 1px solid #d2d6dc;
  background-color: #ffffff;
  color: #4b5563;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-right: 6px;
}
.btn-back-pill:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
  color: #111827;
}

/* Content Card chuẩn hệ thống */
.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.85rem;
  margin-bottom: 1.5rem;
}

/* 2. KHỐI BỘ LỌC */
.card-header-filter {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2eae1;
}
.filter-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: var(--blue, #496883);
}

/* Lưới 4 ô lọc */
.filter-inputs-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
  margin-bottom: 1.25rem;
}
.filter-label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #4f5d63;
  margin-bottom: 0.45rem;
  display: block;
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
.filter-text-input,
.filter-date-input {
  width: 100%;
  height: 2.6rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.filter-text-input {
  padding-left: 2.3rem;
}
.filter-text-input:focus,
.filter-date-input:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

/* Hàng nút thao tác bộ lọc */
.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  border-top: 1px dashed #efeae0;
  padding-top: 1.1rem;
}

.btn {
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
  border: none;
}

.btn-reset {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-reset:hover {
  background-color: #faeed7;
}

.btn-export {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #d2e5d6;
}
.btn-export:hover {
  background-color: #deede1;
}

.btn-primary {
  background-color: #3b596a;
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #2c4350;
}

/* 3. KHỐI BẢNG DANH SÁCH */
.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.1rem;
}
.table-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #3c4d55;
  margin: 0;
}
.table-responsive-custom {
  overflow-x: auto;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
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
  color: var(--blue, #496883);
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
/* Công tắc gạt đổi trạng thái (Toggle Switch chuẩn hình ảnh mẫu) */
.status-switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 40px;
  height: 22px;
  cursor: pointer;
  user-select: none;
  vertical-align: middle;
  flex-shrink: 0;
}
.status-switch.is-disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.switch-native-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}
.switch-track {
  position: absolute;
  inset: 0;
  background-color: #d1d5db;
  border-radius: 9999px;
  transition: background-color 0.25s ease, box-shadow 0.2s ease;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.12);
}
.status-switch:hover:not(.is-disabled) .switch-track {
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.15), 0 0 0 3px rgba(73, 104, 131, 0.15);
}
.status-switch.is-active .switch-track {
  background-color: var(--blue, #496883);
}
.switch-handle {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background-color: #ffffff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.status-switch.is-active .switch-handle {
  transform: translateX(18px);
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
.page-size-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.pagination-info {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  color: #555e65;
  line-height: 1.5;
}
.pagination-total-info {
  font-size: 0.84rem;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.pagination-total-info b {
  color: #1e293b;
  font-weight: 700;
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
  background-color: #3b596a;
  border-color: #3b596a;
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
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
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
  background: #3b596a;
  color: #ffffff;
}

.btn-search-product {
  height: 2.75rem;
  padding: 0 1.35rem;
  background: #3b596a;
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
  background: #2c4350;
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
  color: #496883;
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

/* Modal Xác nhận chuẩn thiết kế */
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(18, 25, 33, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99990;
  backdrop-filter: blur(2px);
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: fadeIn 0.2s ease-out;
}

.confirm-modal-header {
  padding: 1.25rem 1.4rem;
  background-color: #fff9f0;
  border-bottom: 1px solid #faedd9;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  position: relative;
}

.confirm-modal-header.header-danger {
  background-color: #fff5f5;
  border-bottom: 1px solid #fed7d7;
}

.confirm-warning-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #eaf1f5;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-danger .confirm-warning-icon {
  background-color: #fdeded;
  color: #c0392b;
}

.confirm-title-wrap {
  flex: 1;
}

.confirm-title {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 700;
  color: #8c6328;
}

.header-danger .confirm-title {
  color: #c0392b;
}

.confirm-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.83rem;
  color: #aa8651;
}

.modal-close-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #9aa0a0;
  cursor: pointer;
  transition: color 0.15s;
}

.modal-close-btn:hover {
  color: #8c6328;
}

.confirm-modal-body {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.confirm-message-text {
  margin: 0;
  font-size: 0.95rem;
  color: #435158;
  line-height: 1.5;
}

.text-blue {
  color: #496883;
  font-weight: 700;
}

.text-highlight {
  color: #8c6328;
  font-weight: 700;
}

.text-activate {
  color: #496883;
  font-weight: 700;
}

.confirm-info-card {
  background: #faf9f6;
  border: 1px solid #efeae0;
  border-radius: 8px;
  padding: 0.9rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.88rem;
}

.confirm-label {
  color: #7b888e;
  font-weight: 600;
}

.confirm-val {
  color: #2b383e;
}

.confirm-modal-footer {
  padding: 1rem 1.4rem;
  border-top: 1px solid #eeebe3;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  background-color: #faf9f6;
}

.btn-secondary {
  background-color: #ffffff;
  border: 1px solid #d8d2c6;
  color: #616e75;
  font-weight: 600;
  padding: 0.55rem 1.15rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background-color: #f0ece1;
}

.btn-save-confirm {
  background-color: #b38e6e;
  color: #ffffff;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(179, 142, 110, 0.25);
  transition: all 0.2s;
}

.btn-save-confirm:hover:not(:disabled) {
  background-color: #9c7857;
}

.btn-save-confirm:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-confirm-danger {
  background-color: #c0392b;
  color: #ffffff;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(192, 57, 43, 0.25);
  transition: all 0.2s;
}

.btn-confirm-danger:hover:not(:disabled) {
  background-color: #a93226;
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
  font-size: 1.15rem;
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
  color: var(--blue, #496883);
}
.crumb-sep {
  color: #9ca3af;
}
.crumb-current {
  color: var(--blue, #496883);
  font-weight: 700;
  font-size: 0.88rem;
}

/* Hàng trên: Grid 2 cột (Cột trái rộng hơn, Cột phải hẹp hơn một chút và chứa cả 2 thẻ sản phẩm) */
.video-top-grid {
  display: grid;
  grid-template-columns: 460px 1fr;
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
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.video-card-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #111827;
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
  font-size: 0.72rem;
  font-weight: 500;
  line-height: 1.3;
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
.video-date-row .video-input {
  padding: 0.62rem 0.6rem;
  font-size: 0.85rem;
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
  background: #3b596a;
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
  box-shadow: 0 4px 12px rgba(59, 89, 106, 0.22);
}
.btn-video-submit:hover:not(:disabled) {
  background: #2c4350;
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
  gap: 1rem;
  flex-wrap: wrap;
}
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.page-summary-text {
  font-size: 0.82rem;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.page-summary-text b {
  color: #1e293b;
  font-weight: 700;
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
  font-weight: 700;
  color: #1e293b;
  padding: 0 6px;
  white-space: nowrap;
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
.product-name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.product-title-text {
  font-weight: 500;
  color: #1f2937;
}
.product-thumb-box {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
}
.product-thumb-relative {
  position: relative !important;
}
.product-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.thumb-corner-badge {
  position: absolute;
  right: 1.5px;
  font-size: 8.5px;
  font-weight: 800;
  line-height: 1.1;
  padding: 1.5px 3.5px;
  border-radius: 3px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  pointer-events: none;
  z-index: 2;
  white-space: nowrap;
  letter-spacing: -0.2px;
}
.thumb-badge-top-right {
  top: 1.5px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #ffffff;
  border: 0.5px solid rgba(255, 255, 255, 0.6);
}
.thumb-badge-bottom-right {
  bottom: 1.5px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: 0.5px solid rgba(255, 255, 255, 0.6);
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

/* ==================== SWITCH TOGGLE & ACTION BUTTONS (TƯƠNG TỰ PGG) ==================== */
.action-buttons-flex {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
}

.switch-toggle {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
  cursor: pointer;
  margin: 0;
  vertical-align: middle;
}

.switch-toggle.switch-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.switch-toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider-round {
  position: absolute;
  inset: 0;
  background-color: #d1d5db;
  border-radius: 22px;
  transition: all 0.25s ease;
}

.slider-round::before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: all 0.25s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch-toggle input:checked + .slider-round {
  background-color: var(--blue, #304b60);
}

.switch-toggle input:checked + .slider-round::before {
  transform: translateX(16px);
}

.btn-action-edit {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #d0dfe8;
  background-color: #eef5f9;
  color: #304b60;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.btn-action-edit:hover {
  background-color: #dce9f3;
  border-color: #304b60;
  color: #1e3a5f;
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(48, 75, 96, 0.22);
}

.action-svg {
  width: 17px;
  height: 17px;
  display: block;
}

/* ==================== QUY TẮC TRÙNG ĐỢT GIẢM GIÁ (OVERLAP RULE CARD) ==================== */
.overlap-rule-card {
  margin: 0.85rem 0 1.25rem;
  padding: 0.85rem 1rem;
  background: #f0f7ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
}
/* ==================== BADGE BIẾN THỂ TRONG BẢNG SẢN PHẨM ==================== */
.variant-pill-badge {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0.35rem 0.65rem;
  min-width: 68px;
  border-radius: 20px;
  border: 1px solid #d0dfe8;
  background-color: #f4f8fb;
  color: #304b60;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.15;
}
.badge-selected-count {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #059669;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 600;
}
.badge-variant-active-dgg {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 0.72rem;
  font-weight: 700;
  color: #b45309;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  white-space: nowrap;
}
.text-discount-badge {
  font-size: 0.75rem;
  color: #dc2626;
  font-weight: 600;
  margin-top: 2px;
}

/* ==================== ẢNH BIẾN THỂ (PRODUCT TABLE & APPLIED TABLE) ==================== */
.variant-img-box {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}
.variant-img-item {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.variant-img-cell {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  position: relative;
  overflow: visible;
}
.applied-variant-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 7px;
}
.variant-thumb-discount-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 1.5px 5.5px;
  border-radius: 5px;
  border: 1.5px solid #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.22);
  line-height: 1.2;
  white-space: nowrap;
  z-index: 5;
  letter-spacing: -0.2px;
}
</style>