<template>
  <div class="product-page-container">
    <!-- 1. Header Trang: Breadcrumb bên trái, 3 nút hành động bên phải -->
    <div class="page-top-header">
      <div class="header-left-col">
        <div class="page-breadcrumb">
          <router-link to="/san-pham" class="crumb-parent">Quản lý sản phẩm</router-link>
          <span class="crumb-separator">/</span>
          <span class="crumb-current">Danh sách sản phẩm</span>
        </div>
      </div>

      <div class="header-right-col">
        <button type="button" class="btn btn-header-outline" @click="goToVariantsPage" title="Quản lý biến thể sản phẩm">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
          </svg>
          <span>Biến thể</span>
        </button>

        <button type="button" class="btn btn-header-outline" @click="goToAttributesPage" title="Quản lý thuộc tính">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="4" y1="21" x2="4" y2="14"></line>
            <line x1="4" y1="10" x2="4" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12" y2="3"></line>
            <line x1="20" y1="21" x2="20" y2="16"></line>
            <line x1="20" y1="12" x2="20" y2="3"></line>
            <line x1="1" y1="14" x2="7" y2="14"></line>
            <line x1="9" y1="8" x2="15" y2="8"></line>
            <line x1="17" y1="16" x2="23" y2="16"></line>
          </svg>
          <span>Thuộc tính</span>
        </button>

        <button type="button" class="btn btn-header-primary" @click="goToAddPage" title="Thêm mới sản phẩm">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Thêm sản phẩm</span>
        </button>
      </div>
    </div>

    <!-- Toast thông báo -->
    <div v-if="toast.show" :class="['toast-notification', toast.type]">
      {{ toast.message }}
    </div>

    <!-- 2. KHUNG 1: BỘ LỌC SẢN PHẨM -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <div class="filter-header-title">
          <svg class="filter-funnel-svg" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
          </svg>
          <span>Bộ lọc sản phẩm</span>
        </div>
      </div>

      <div class="filter-form-grid">
        <!-- 1. Mã / tên sản phẩm -->
        <div class="filter-field-item">
          <label class="filter-field-label">Mã / tên sản phẩm</label>
          <div class="filter-input-wrap">
            <input
                type="text"
                v-model="filters.keyword"
                @keyup.enter="handleSearch"
                @input="onKeywordChange"
                placeholder="Tìm kiếm sản phẩm..."
                class="filter-text-input"
            />
          </div>
        </div>

        <!-- 2. Danh mục -->
        <div class="filter-field-item">
          <label class="filter-field-label">Danh mục</label>
          <Select2Combobox
            v-model="filters.idDanhMuc"
            :options="attributes.danhMuc"
            value-key="id"
            label-key="tenDanhMuc"
            placeholder="Tất cả"
            search-placeholder="Tìm danh mục..."
            @change="handleSearch"
          />
        </div>

        <!-- 3. Thương hiệu -->
        <div class="filter-field-item">
          <label class="filter-field-label">Thương hiệu</label>
          <Select2Combobox
            v-model="filters.idThuongHieu"
            :options="attributes.thuongHieu"
            value-key="id"
            label-key="tenThuongHieu"
            placeholder="Tất cả"
            search-placeholder="Tìm thương hiệu..."
            @change="handleSearch"
          />
        </div>

        <!-- 4. Chất liệu -->
        <div class="filter-field-item">
          <label class="filter-field-label">Chất liệu</label>
          <Select2Combobox
            v-model="filters.idChatLieu"
            :options="attributes.chatLieu"
            value-key="id"
            label-key="tenChatLieu"
            placeholder="Tất cả"
            search-placeholder="Tìm chất liệu..."
            @change="handleSearch"
          />
        </div>

        <!-- 5. Cổ áo -->
        <div class="filter-field-item">
          <label class="filter-field-label">Cổ áo</label>
          <Select2Combobox
            v-model="filters.idCoAo"
            :options="attributes.coAo"
            value-key="id"
            label-key="tenCoAo"
            placeholder="Tất cả"
            search-placeholder="Tìm cổ áo..."
            @change="handleSearch"
          />
        </div>

        <!-- 6. Tay áo -->
        <div class="filter-field-item">
          <label class="filter-field-label">Tay áo</label>
          <Select2Combobox
            v-model="filters.idTayAo"
            :options="attributes.tayAo"
            value-key="id"
            label-key="tenTayAo"
            placeholder="Tất cả"
            search-placeholder="Tìm tay áo..."
            @change="handleSearch"
          />
        </div>

        <!-- 7. Xuất xứ -->
        <div class="filter-field-item">
          <label class="filter-field-label">Xuất xứ</label>
          <Select2Combobox
            v-model="filters.idXuatXu"
            :options="attributes.xuatXu"
            value-key="id"
            label-key="tenXuatXu"
            placeholder="Tất cả"
            search-placeholder="Tìm xuất xứ..."
            @change="handleSearch"
          />
        </div>

        <!-- 8. Họa tiết -->
        <div class="filter-field-item">
          <label class="filter-field-label">Họa tiết</label>
          <Select2Combobox
            v-model="filters.idHoaTiet"
            :options="attributes.hoaTiet"
            value-key="id"
            label-key="tenHoaTiet"
            placeholder="Tất cả"
            search-placeholder="Tìm họa tiết..."
            @change="handleSearch"
          />
        </div>
      </div>

      <!-- Hàng chân bộ lọc: Trạng thái bên trái, Các nút Đặt lại, Xuất Excel, Tìm kiếm bên phải -->
      <div class="filter-footer-row">
        <div class="filter-field-item filter-status-col">
          <label class="filter-field-label">Trạng thái</label>
          <Select2Combobox
            v-model="filters.trangThai"
            :options="trangThaiOptions"
            value-key="id"
            label-key="name"
            placeholder="Tất cả trạng thái"
            search-placeholder="Tìm trạng thái..."
            @change="handleSearch"
          />
        </div>

        <div class="filter-buttons-group">
          <button type="button" class="btn btn-filter-reset" @click="resetFilters" title="Đặt lại bộ lọc">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
            </svg>
            <span>Đặt lại</span>
          </button>

          <button type="button" class="btn btn-filter-export" @click="handleExportExcel" :disabled="exporting" title="Xuất danh sách ra file Excel">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            <span>{{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. KHUNG 2: BẢNG DANH SÁCH SẢN PHẨM -->
    <div class="content-card table-card">
      <div class="table-card-header">
        <div class="table-heading-title">
          <span class="heading-box-icon">
            <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
              <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
              <line x1="12" y1="22.08" x2="12" y2="12"></line>
            </svg>
          </span>
          <span>Danh sách sản phẩm</span>
        </div>
        <div class="table-heading-meta">
          <span class="count-pill">{{ pageData.totalElements || products.length }} sản phẩm</span>
        </div>
      </div>

      <!-- 3. Bảng dữ liệu sản phẩm -->
      <div class="table-responsive">
        <table class="product-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 90px; text-align: center;">Ảnh</th>
            <th style="width: 120px;">Mã SP</th>
            <th>Tên sản phẩm</th>
            <th style="width: 170px;">Phân loại</th>
            <th style="width: 80px; text-align: center;">Số màu</th>
            <th style="width: 95px; text-align: center;">Số kích thước</th>
            <th style="width: 165px;">Giá bán</th>
            <th style="width: 90px; text-align: center;">Tồn kho</th>
            <th style="width: 120px;">Ngày tạo</th>
            <th style="width: 130px; text-align: center;">Trạng thái</th>
            <th style="width: 120px; text-align: center;">Thao tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td colspan="12" class="empty-state">
              <div class="empty-box">
                <div class="loading-spinner"></div>
                <span class="empty-text">Đang tải dữ liệu sản phẩm...</span>
              </div>
            </td>
          </tr>
          <tr v-else-if="products.length === 0">
            <td colspan="12" class="empty-state">
              <div class="empty-box">
                <span class="empty-icon">
                  <svg viewBox="0 0 24 24" width="38" height="38" fill="currentColor" opacity="0.45">
                    <path d="M20 2H4c-1.1 0-2 .9-2 2v3c0 .55.45 1 1 1h1v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8h1c.55 0 1-.45 1-1V4c0-1.1-.9-2-2-2zm-1 6v12H5V8h14zM4 4h16v2H4V4zm5 6h6v2H9v-2z"/>
                  </svg>
                </span>
                <span class="empty-text">Không tìm thấy sản phẩm nào phù hợp với bộ lọc!</span>
                <button type="button" class="btn btn-refresh-sm" @click="handleReset">Làm mới bộ lọc</button>
              </div>
            </td>
          </tr>
          <tr v-for="(item, index) in products" :key="item.id">
            <!-- STT -->
            <td style="text-align: center;" class="stt-cell">
              {{ pageData.number * pageData.size + index + 1 }}
            </td>

            <!-- Ảnh sản phẩm -->
            <td style="text-align: center;">
              <div class="product-img-box">
                <img
                    :src="formatImageUrl(item.anhDaiDien) || defaultImage"
                    :alt="item.tenSanPham"
                    @error="onImgError"
                />
                <span
                    v-if="item.coGiamGia && item.phanTramGiam"
                    class="discount-badge-percent product-img-discount"
                    style="position: absolute; top: -6px; right: -12px; left: auto; margin: 0;"
                    :title="item.tenDotGiamGia ? `Đợt giảm: ${item.tenDotGiamGia}` : 'Đang áp dụng giảm giá'"
                >
                  -{{ item.phanTramGiam }}%
                </span>
              </div>
            </td>

            <!-- Mã sản phẩm -->
            <td class="code-cell">
              <b>{{ item.maSanPham }}</b>
            </td>

            <!-- Tên sản phẩm -->
            <td class="product-name-cell">
              <div class="name-text">{{ item.tenSanPham }}</div>
              <div v-if="item.moTa" class="desc-text">{{ item.moTa }}</div>
            </td>

            <!-- Phân loại -->
            <td>
              <div class="attr-badges">
                <span class="attr-tag" v-if="item.tenThuongHieu">{{ item.tenThuongHieu }}</span>
                <span class="attr-tag" v-if="item.tenDanhMuc">{{ item.tenDanhMuc }}</span>
                <span class="attr-tag" v-if="item.tenChatLieu">{{ item.tenChatLieu }}</span>
              </div>
            </td>

            <!-- Số màu (từ ảnh 1) -->
            <td style="text-align: center;">
              <span class="badge-mini-stat color-stat" title="Số màu sắc">
                {{ item.soMau ?? (item.soLuongBienThe ? 1 : 0) }}
              </span>
            </td>

            <!-- Số kích thước (từ ảnh 1) -->
            <td style="text-align: center;">
              <span class="badge-mini-stat size-stat" title="Số kích cỡ">
                {{ item.soKichCo ?? (item.soLuongBienThe ? 1 : 0) }}
              </span>
            </td>

            <!-- Giá bán (liên kết đợt giảm giá) -->
            <td class="price-cell">
              <div v-if="item.coGiamGia && item.minGiaSauGiam != null" class="discounted-price-wrapper">
                <div class="original-price-strikethrough">
                  {{ formatPriceRange(item.minGia, item.maxGia) }}
                </div>
                <div class="active-discount-price">
                  <span class="sale-price-text">{{ formatPriceRange(item.minGiaSauGiam, item.maxGiaSauGiam) }}</span>
                </div>
              </div>
              <div v-else class="normal-price-display">
                <b>{{ formatPriceRange(item.minGia, item.maxGia) }}</b>
              </div>
            </td>

            <!-- Số lượng tồn -->
            <td style="text-align: center;">
              <span :class="['stock-badge', item.tongSoLuong > 0 ? 'in-stock' : 'out-of-stock']">
                {{ item.tongSoLuong ?? 0 }}
              </span>
            </td>

            <!-- Ngày tạo -->
            <td class="date-cell">{{ formatDate(item.ngayTao) }}</td>

            <!-- Trạng thái sản phẩm -->
            <td style="text-align: center;">
              <span :class="['status-badge', item.trangThai === 1 ? 'status-active' : 'status-inactive']">
                {{ item.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
              </span>
            </td>

            <!-- Cột thao tác: Chuyển trạng thái kinh doanh, Xem chi tiết biến thể -->
            <td style="text-align: center;">
              <div class="action-buttons-group">
                <!-- 1. Switch chuyển trạng thái kinh doanh -->
                <label
                  class="switch-toggle"
                  :title="item.trangThai === 1 ? 'Đang kinh doanh (nhấn để ngừng)' : 'Đang ngừng kinh doanh (nhấn để bật)'"
                >
                  <input
                    type="checkbox"
                    :checked="item.trangThai === 1"
                    @click.prevent="toggleStatus(item)"
                  />
                  <span class="slider-round"></span>
                </label>

                <!-- 2. Icon chỉnh sửa biến thể sản phẩm -->
                <button
                    class="btn-action btn-edit"
                    @click="goToEditVariant(item)"
                    title="Chỉnh sửa biến thể"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>

                <!-- 3. Icon xem chi tiết sản phẩm -->
                <button
                    class="btn-action btn-view"
                    @click="openProductDetail(item)"
                    title="Xem chi tiết sản phẩm"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
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

      <!-- 4. Thanh phân trang -->
      <div class="pagination-footer" v-if="pageData.totalPages > 0">
        <div class="pagination-left">
          <span class="pagination-info">
            Hiển thị {{ products.length ? (pageData.number * pageData.size + 1) : 0 }} - {{ pageData.number * pageData.size + products.length }} / {{ pageData.totalElements }} sản phẩm
          </span>
        </div>
        <div class="pagination-center">
          <button
              class="pg-box"
              :disabled="pageData.number === 0"
              @click="changePage(0)"
              title="Trang đầu"
          >«</button>
          <button
              class="pg-box"
              :disabled="pageData.number === 0"
              @click="changePage(pageData.number - 1)"
              title="Trang trước"
          >‹</button>

          <button
              v-for="p in visiblePages"
              :key="p"
              :class="['pg-box', { active: pageData.number === p }]"
              @click="changePage(p)"
          >
            {{ p + 1 }}
          </button>

          <button
              class="pg-box"
              :disabled="pageData.number >= pageData.totalPages - 1"
              @click="changePage(pageData.number + 1)"
              title="Trang sau"
          >›</button>
          <button
              class="pg-box"
              :disabled="pageData.number >= pageData.totalPages - 1"
              @click="changePage(pageData.totalPages - 1)"
              title="Trang cuối"
          >»</button>
        </div>
      </div>
    </div>

    <!-- MODAL XÁC NHẬN HÀNH ĐỘNG (XÓA / ĐỔI TRẠNG THÁI) -->
    <transition name="fade">
      <div v-if="confirmDialog.show" class="confirm-modal-overlay" @click="closeConfirmDialog">
        <div class="confirm-modal-box" @click.stop>
          <div class="confirm-modal-header" :class="{ 'header-danger': confirmDialog.type === 'danger' }">
            <div class="confirm-warning-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </div>
            <div class="confirm-title-wrap">
              <h3 class="confirm-title">{{ confirmDialog.title || 'Xác nhận thao tác' }}</h3>
              <p class="confirm-subtitle">Vui lòng xác nhận để tiếp tục thực hiện</p>
            </div>
            <button type="button" class="modal-close-btn" @click="closeConfirmDialog">✕</button>
          </div>

          <div class="confirm-modal-body">
            <p class="confirm-message-text">{{ confirmDialog.message }}</p>
          </div>

          <div class="confirm-modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeConfirmDialog">
              Hủy bỏ
            </button>
            <button
                type="button"
                :class="['btn', confirmDialog.type === 'danger' ? 'btn-confirm-danger' : 'btn-save-confirm']"
                @click="executeConfirmedAction"
            >
              {{ confirmDialog.confirmText || 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL XEM CHI TIẾT SẢN PHẨM -->
    <transition name="fade">
      <div v-if="detailModal.show" class="confirm-modal-overlay" @click.self="closeProductDetail">
        <div class="product-detail-modal-box" @click.stop>
          <!-- Header Modal -->
          <div class="pdm-header">
            <div class="pdm-title-wrap">
              <div class="pdm-icon-badge">👕</div>
              <div>
                <h3 class="pdm-title">{{ detailModal.product?.tenSanPham || 'Chi tiết sản phẩm' }}</h3>
                <div class="pdm-subtitle">
                  <span class="pdm-code">Mã: {{ detailModal.product?.maSanPham || '—' }}</span>
                  <span class="pdm-dot">•</span>
                  <span :class="['status-badge', detailModal.product?.trangThai === 1 ? 'status-active' : 'status-inactive']">
                    {{ detailModal.product?.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
                  </span>
                </div>
              </div>
            </div>
            <button type="button" class="btn-pdm-close" @click="closeProductDetail" title="Đóng">&times;</button>
          </div>

          <!-- Body Modal -->
          <div class="pdm-body">
            <div v-if="detailModal.loading" class="pdm-loading">
              <div class="loading-spinner"></div>
              <span>Đang tải thông tin chi tiết sản phẩm...</span>
            </div>

            <div v-else-if="detailModal.product" class="pdm-content-scroll">
              <!-- Hàng thông tin tổng quan -->
              <div class="pdm-overview-row">
                <!-- Cột trái: Ảnh sản phẩm & Giá -->
                <div class="pdm-image-col">
                  <div class="pdm-image-card">
                    <img
                      :src="formatImageUrl(detailModal.product.anhDaiDien)"
                      :alt="detailModal.product.tenSanPham"
                      class="pdm-image"
                      @error="onImgError"
                    />
                  </div>
                  <div class="pdm-price-summary">
                    <div class="pdm-price-title">Giá bán sản phẩm</div>
                    <div class="pdm-price-amount">
                      {{ formatPriceRange(detailModal.product.minGia, detailModal.product.maxGia) }}
                    </div>
                    <div v-if="detailModal.product.coGiamGia" class="pdm-discount-tag">
                      🔥 {{ detailModal.product.tenDotGiamGia || 'Đang có đợt giảm giá' }} (-{{ detailModal.product.phanTramGiam }}%)
                    </div>
                  </div>
                </div>

                <!-- Cột phải: Bảng thuộc tính chi tiết sản phẩm -->
                <div class="pdm-details-col">
                  <h4 class="pdm-section-heading">Thuộc tính sản phẩm</h4>
                  <div class="pdm-grid-props">
                    <div class="pdm-prop-item">
                      <span class="prop-label">Danh mục:</span>
                      <span class="prop-val">{{ detailModal.product.tenDanhMuc || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Thương hiệu:</span>
                      <span class="prop-val">{{ detailModal.product.tenThuongHieu || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Chất liệu:</span>
                      <span class="prop-val">{{ detailModal.product.tenChatLieu || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Xuất xứ:</span>
                      <span class="prop-val">{{ detailModal.product.tenXuatXu || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Cổ áo:</span>
                      <span class="prop-val">{{ detailModal.product.tenCoAo || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Tay áo:</span>
                      <span class="prop-val">{{ detailModal.product.tenTayAo || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Họa tiết:</span>
                      <span class="prop-val">{{ detailModal.product.tenHoaTiet || '—' }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Tổng tồn kho:</span>
                      <span class="prop-val font-bold text-success">{{ detailModal.product.tongSoLuong ?? 0 }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Số màu sắc:</span>
                      <span class="prop-val">{{ detailModal.product.soMau ?? 0 }} màu</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Số kích thước:</span>
                      <span class="prop-val">{{ detailModal.product.soKichCo ?? 0 }} size</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Ngày tạo:</span>
                      <span class="prop-val">{{ formatDate(detailModal.product.ngayTao) }}</span>
                    </div>
                    <div class="pdm-prop-item">
                      <span class="prop-label">Người tạo:</span>
                      <span class="prop-val">{{ detailModal.product.nguoiTao || '—' }}</span>
                    </div>
                  </div>

                  <div class="pdm-description-box" v-if="detailModal.product.moTa">
                    <span class="prop-label">Mô tả sản phẩm:</span>
                    <p class="prop-desc-text">{{ detailModal.product.moTa }}</p>
                  </div>
                </div>
              </div>

              <!-- Danh sách các biến thể của riêng sản phẩm này -->
              <div class="pdm-variants-block">
                <div class="pdm-variants-header">
                  <h4 class="pdm-section-heading">Danh sách biến thể của sản phẩm ({{ detailModal.variants.length }})</h4>
                </div>

                <div class="pdm-table-wrap" v-if="detailModal.variants.length > 0">
                  <table class="pdm-variants-table">
                    <thead>
                      <tr>
                        <th style="width: 45px; text-align: center;">STT</th>
                        <th style="width: 140px;">Mã biến thể</th>
                        <th style="width: 140px;">Màu sắc</th>
                        <th style="width: 85px; text-align: center;">Kích cỡ</th>
                        <th style="width: 130px; text-align: right;">Giá bán</th>
                        <th style="width: 85px; text-align: center;">Tồn kho</th>
                        <th style="width: 110px; text-align: center;">Trạng thái</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(v, vIdx) in detailModal.variants" :key="v.id || vIdx">
                        <td style="text-align: center;" class="text-muted">{{ vIdx + 1 }}</td>
                        <td class="font-bold text-code">{{ v.maChiTietSanPham || '—' }}</td>
                        <td>
                          <div class="pdm-color-cell">
                            <span class="color-disc" :style="{ backgroundColor: v.maHex || '#888' }"></span>
                            <span>{{ v.tenMauSac || '—' }}</span>
                          </div>
                        </td>
                        <td style="text-align: center;">
                          <span class="pdm-size-badge">{{ v.tenKichCo || '—' }}</span>
                        </td>
                        <td style="text-align: right;" class="font-bold text-price">
                          <div v-if="v.coGiamGia">
                            <span class="old-price">{{ Number(v.giaBan).toLocaleString('vi-VN') }} đ</span>
                            <span class="discounted-price">{{ Number(v.giaSauGiam).toLocaleString('vi-VN') }} đ</span>
                          </div>
                          <span v-else>{{ Number(v.giaBan).toLocaleString('vi-VN') }} đ</span>
                        </td>
                        <td style="text-align: center;" class="font-bold">
                          {{ v.soLuong ?? 0 }}
                        </td>
                        <td style="text-align: center;">
                          <span :class="['status-badge', v.trangThai === 1 ? 'status-active' : 'status-inactive']">
                            {{ v.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
                          </span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="pdm-empty-variants">
                  Sản phẩm này chưa có biến thể nào được tạo.
                </div>
              </div>
            </div>
          </div>

          <!-- Footer Modal -->
          <div class="pdm-footer">
            <button
              type="button"
              class="btn btn-edit-product-action"
              @click="goToEditPage(detailModal.product)"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              Chỉnh sửa sản phẩm
            </button>
            <button type="button" class="btn btn-confirm-cancel" @click="closeProductDetail">
              Đóng
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()

const defaultImage = '/images/products/default_tshirt.jpg'

// Tùy chọn trạng thái cho Select2 Combobox
const trangThaiOptions = [
  { id: 1, name: 'Đang kinh doanh' },
  { id: 0, name: 'Ngừng kinh doanh' }
]

// Bộ lọc
const filters = ref({
  keyword: '',
  idThuongHieu: '',
  idDanhMuc: '',
  idXuatXu: '',
  idChatLieu: '',
  idCoAo: '',
  idTayAo: '',
  idHoaTiet: '',
  trangThai: '',
  page: 0,
  size: 10
})

// Dữ liệu thuộc tính
const attributes = ref({
  thuongHieu: [],
  danhMuc: [],
  xuatXu: [],
  chatLieu: [],
  coAo: [],
  tayAo: [],
  hoaTiet: [],
  mauSac: [],
  kichCo: []
})

// Danh sách sản phẩm & phân trang
const products = ref([])
const loading = ref(false)
const pageData = ref({
  number: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0
})

// Hộp thoại xác nhận tương tác
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  type: 'primary',
  confirmText: 'Đồng ý',
  onConfirm: null
})

const openConfirm = ({ title, message, type = 'primary', confirmText = 'Đồng ý', onConfirm }) => {
  confirmDialog.value = {
    show: true,
    title,
    message,
    type,
    confirmText,
    onConfirm
  }
}

const closeConfirmDialog = () => {
  confirmDialog.value.show = false
  confirmDialog.value.onConfirm = null
}

const executeConfirmedAction = async () => {
  const action = confirmDialog.value.onConfirm
  closeConfirmDialog()
  if (typeof action === 'function') {
    await action()
  }
}

// Chuyển trang Thêm sản phẩm
const goToAddPage = () => {
  router.push('/san-pham/them')
}

// Chuyển trang Biến thể sản phẩm
const goToVariantsPage = () => {
  router.push('/bien-the-san-pham')
}

// Chuyển trang Quản lý thuộc tính
const goToAttributesPage = () => {
  router.push('/thuoc-tinh')
}

// Chuyển trang Sửa sản phẩm
const goToEditPage = (item) => {
  if (!item || !item.id) return
  router.push(`/san-pham/chinh-sua/${item.id}`)
}

// Chuyển trang Sửa biến thể sản phẩm
const goToEditVariant = (item) => {
  if (!item || !item.id) return
  if (detailModal.value.show) {
    closeProductDetail()
  }
  router.push({ path: '/bien-the-san-pham', query: { sanPhamId: item.id } })
}

// Modal Xem chi tiết sản phẩm
const detailModal = ref({
  show: false,
  loading: false,
  product: null,
  variants: []
})

const openProductDetail = async (item) => {
  if (!item) return
  detailModal.value.show = true
  detailModal.value.loading = true
  detailModal.value.product = { ...item }
  detailModal.value.variants = []

  try {
    const [prodRes, varRes] = await Promise.all([
      api.get(`/api/san-pham/${item.id}`).catch(() => ({ data: null })),
      api.get(`/api/chi-tiet-san-pham/by-san-pham/${item.id}`).catch(() => ({ data: [] }))
    ])
    if (prodRes && prodRes.data) {
      detailModal.value.product = prodRes.data
    }
    if (varRes && varRes.data) {
      detailModal.value.variants = varRes.data || []
    }
  } catch (err) {
    console.error('Lỗi tải chi tiết sản phẩm:', err)
  } finally {
    detailModal.value.loading = false
  }
}

const closeProductDetail = () => {
  detailModal.value.show = false
  detailModal.value.product = null
  detailModal.value.variants = []
}

// Toast
const toast = ref({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// Lấy danh sách thuộc tính
const fetchAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      attributes.value = res.data
    }
  } catch (err) {
    console.error('Lỗi tải thuộc tính:', err)
  }
}

// Lấy danh sách sản phẩm
const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: filters.value.page,
      size: filters.value.size,
      sortBy: 'id',
      sortDir: 'desc'
    }
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idThuongHieu) params.idThuongHieu = filters.value.idThuongHieu
    if (filters.value.idDanhMuc) params.idDanhMuc = filters.value.idDanhMuc
    if (filters.value.idXuatXu) params.idXuatXu = filters.value.idXuatXu
    if (filters.value.idChatLieu) params.idChatLieu = filters.value.idChatLieu
    if (filters.value.idCoAo) params.idCoAo = filters.value.idCoAo
    if (filters.value.idTayAo) params.idTayAo = filters.value.idTayAo
    if (filters.value.idHoaTiet) params.idHoaTiet = filters.value.idHoaTiet
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/san-pham', { params })
    if (res.data) {
      products.value = res.data.content || []
      pageData.value = {
        number: res.data.pageNumber || 0,
        size: res.data.pageSize || 10,
        totalElements: res.data.totalElements || 0,
        totalPages: res.data.totalPages || 0
      }
    }
  } catch (err) {
    console.error('Lỗi tải sản phẩm:', err.response?.data || err)
    showToast(err.response?.data?.message || 'Lỗi khi tải danh sách sản phẩm!', 'error')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  filters.value.page = 0
  fetchProducts()
}

let searchTimeout = null
const onKeywordChange = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleSearch()
  }, 350)
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    idThuongHieu: '',
    idDanhMuc: '',
    idXuatXu: '',
    idChatLieu: '',
    idCoAo: '',
    idTayAo: '',
    idHoaTiet: '',
    trangThai: '',
    page: 0,
    size: 10
  }
  fetchProducts()
}

const exporting = ref(false)

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const params = {}
    if (filters.value.keyword?.trim()) params.keyword = filters.value.keyword.trim()
    if (filters.value.idThuongHieu) params.idThuongHieu = filters.value.idThuongHieu
    if (filters.value.idDanhMuc) params.idDanhMuc = filters.value.idDanhMuc
    if (filters.value.idXuatXu) params.idXuatXu = filters.value.idXuatXu
    if (filters.value.idChatLieu) params.idChatLieu = filters.value.idChatLieu
    if (filters.value.idCoAo) params.idCoAo = filters.value.idCoAo
    if (filters.value.idTayAo) params.idTayAo = filters.value.idTayAo
    if (filters.value.idHoaTiet) params.idHoaTiet = filters.value.idHoaTiet
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai

    const res = await api.get('/api/san-pham/export-excel', {
      params,
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `danh_sach_san_pham_${Date.now()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    showToast('Xuất file Excel thành công!')
  } catch (err) {
    console.error(err)
    showToast('Lỗi khi xuất file Excel!', 'error')
  } finally {
    exporting.value = false
  }
}

const handlePageSizeChange = () => {
  filters.value.page = 0
  fetchProducts()
}

const changePage = (p) => {
  if (p < 0 || p >= pageData.value.totalPages) return
  filters.value.page = p
  fetchProducts()
}

const visiblePages = computed(() => {
  const current = pageData.value.number
  const total = pageData.value.totalPages
  const pages = []
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, current + 2)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Định dạng giá tiền
const formatPriceRange = (min, max) => {
  if (min == null && max == null) return 'Chưa có biến thể'
  if (min != null && max != null && min === max) {
    return Number(min).toLocaleString('vi-VN') + ' đ'
  }
  const minStr = min != null ? Number(min).toLocaleString('vi-VN') : '0'
  const maxStr = max != null ? Number(max).toLocaleString('vi-VN') : '0'
  return `${minStr} - ${maxStr} đ`
}

// Định dạng ngày
const formatDate = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleDateString('vi-VN')
}

const formatImageUrl = (url) => {
  if (!url) return ''
  let cleanUrl = url
  if (cleanUrl.includes('|')) {
    cleanUrl = cleanUrl.split('|')[1]
  }
  if (cleanUrl.startsWith('data:') || cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) return cleanUrl
  return `http://localhost:8080${cleanUrl.startsWith('/') ? '' : '/'}${cleanUrl}`
}

const onImgError = (e) => {
  e.target.src = defaultImage
}

// Thay đổi trạng thái sản phẩm có xác nhận
const toggleStatus = (item) => {
  const newStatus = item.trangThai === 1 ? 0 : 1
  const actionText = newStatus === 1 ? 'mở kinh doanh lại' : 'ngừng kinh doanh'
  openConfirm({
    title: 'Xác nhận thay đổi trạng thái',
    message: `Bạn có chắc chắn muốn ${actionText} sản phẩm "${item.tenSanPham}" không?`,
    type: 'info',
    confirmText: 'Xác nhận',
    onConfirm: async () => {
      try {
        await api.patch(`/api/san-pham/${item.id}/status`, null, {
          params: { trangThai: newStatus }
        })
        item.trangThai = newStatus
        showToast(`Đã chuyển trạng thái sang "${newStatus === 1 ? 'Kinh doanh' : 'Ngừng KD'}"`)
      } catch (err) {
        console.error('Lỗi cập nhật trạng thái:', err)
        showToast('Lỗi cập nhật trạng thái!', 'error')
      }
    }
  })
}

// Xóa sản phẩm có xác nhận
const confirmDelete = (item) => {
  openConfirm({
    title: 'Xác nhận xóa sản phẩm',
    message: `Bạn có chắc chắn muốn xóa sản phẩm "${item.tenSanPham}" không? Hành động này sẽ không thể hoàn tác!`,
    type: 'danger',
    confirmText: 'Xóa ngay',
    onConfirm: async () => {
      try {
        await api.delete(`/api/san-pham/${item.id}`)
        showToast('Xóa sản phẩm thành công!')
        fetchProducts()
      } catch (err) {
        console.error('Lỗi xóa sản phẩm:', err)
        const msg = err.response?.data?.message || 'Lỗi khi xóa sản phẩm!'
        showToast(msg, 'error')
      }
    }
  })
}

onMounted(() => {
  fetchAttributes()
  fetchProducts()
})
</script>

<style scoped>
/* Toàn bộ vùng hiển thị trang */
.product-page-container {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header Trang */
.page-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-left-col {
  display: flex;
  flex-direction: column;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
  color: #7f8c8d;
  margin-bottom: 0;
}

.crumb-parent {
  color: #7f8c8d;
  text-decoration: none;
  transition: color 0.15s;
}
.crumb-parent:hover {
  color: var(--blue, #496883);
}

.crumb-separator {
  color: #bdc3c7;
}

.crumb-current {
  color: #496883;
  font-weight: 600;
}

.page-main-title {
  font-size: 1.65rem;
  font-weight: 800;
  color: #2c3e50;
  letter-spacing: -0.3px;
  margin: 0 0 0.25rem 0;
}

.page-sub-title {
  font-size: 0.88rem;
  color: #7f8c8d;
  margin: 0;
}

.header-right-col {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.btn-header-outline {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #3d4a50;
  height: 38px;
  padding: 0 14px;
  font-weight: 600;
  font-size: 0.88rem;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.btn-header-outline:hover {
  background: #f7f5ee;
  border-color: #496883;
  color: #496883;
  transform: translateY(-1px);
}

.btn-header-primary {
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  height: 38px;
  padding: 0 16px;
  font-weight: 600;
  font-size: 0.88rem;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}
.btn-header-primary:hover {
  background: #385269;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.35);
}

/* Toast notification */
.toast-notification {
  padding: 0.6rem 1.2rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  animation: slideIn 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 1rem;
}
.toast-notification.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}
.toast-notification.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Khối Card trắng */
.content-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e8e2d5);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  margin-bottom: 1.25rem;
}

/* 2. KHUNG BỘ LỌC SẢN PHẨM */
.filter-card {
  padding: 1.25rem 1.5rem 1.15rem;
}

.card-header-filter {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.filter-header-title {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 1rem;
  font-weight: 700;
  color: #2c3e50;
}

.filter-funnel-svg {
  color: var(--blue, #496883);
}

.filter-form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.85rem 1.15rem;
  margin-bottom: 1rem;
}

@media (max-width: 1200px) {
  .filter-form-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .filter-form-grid {
    grid-template-columns: 1fr;
  }
}

.filter-field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.filter-field-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #475569;
  margin: 0;
}

.filter-input-wrap {
  position: relative;
  width: 100%;
}

.filter-text-input {
  width: 100%;
  height: 38px;
  background-color: #ffffff;
  border: 1px solid #dcd6c8;
  border-radius: 7px;
  padding: 0 0.85rem;
  font-size: 0.88rem;
  color: #1e293b;
  outline: none;
  transition: all 0.18s ease;
  box-sizing: border-box;
}

.filter-text-input:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.filter-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
  padding-top: 0.75rem;
  border-top: 1px dashed #ede8dc;
}

.filter-status-col {
  width: calc(25% - 0.85rem);
  min-width: 200px;
}

@media (max-width: 1200px) {
  .filter-status-col {
    width: 100%;
  }
}

.filter-buttons-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-left: auto;
  flex-wrap: wrap;
}

.btn-filter-reset {
  background: #ffffff;
  border: 1px solid #d8d2c4;
  color: #64748b;
  border-radius: 7px;
  height: 38px;
  padding: 0 14px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.btn-filter-reset:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #334155;
}

.btn-filter-export {
  background: #edf5ef;
  border: 1px solid #c8dfce;
  color: #3f6e4d;
  border-radius: 7px;
  height: 38px;
  padding: 0 14px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.btn-filter-export:hover:not(:disabled) {
  background: #deede1;
}
.btn-filter-export:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}


/* 3. KHUNG BẢNG SẢN PHẨM */
.table-card {
  padding: 1.25rem 1.5rem;
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #ebd9c8;
}

.table-heading-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2b353b;
}

.heading-box-icon {
  display: flex;
  align-items: center;
  color: #496883;
}

.table-heading-meta {
  display: flex;
  align-items: center;
}

.count-pill {
  font-size: 0.8rem;
  font-weight: 600;
  background-color: #f7f5ef;
  color: #496883;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #d6d0c3;
}

/* Badge thống kê Số màu và Số kích thước */
.badge-mini-stat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 24px;
  padding: 0 6px;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 700;
  color: #3b4b53;
  background-color: #f7f5ee;
  border: 1px solid #e2ddd4;
}

.badge-mini-stat.color-stat {
  background-color: #f4f6f8;
  color: #354e63;
  border-color: #dbe4eb;
}

.badge-mini-stat.size-stat {
  background-color: #fdf8f4;
  color: #7a5035;
  border-color: #eddcd1;
}

/* 3. Bảng danh sách sản phẩm */
.table-responsive {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  min-width: 960px;
  border-collapse: collapse;
  font-size: 0.92rem;
}

.product-table th {
  background-color: #faf8f5;
  color: #4a5568;
  font-weight: 700;
  text-transform: uppercase;
  padding: 0.95rem 1rem;
  text-align: left;
  border-bottom: 2px solid #e9e5db;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.product-table td {
  padding: 0.95rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  vertical-align: middle;
  color: #4b585e;
}

.product-table tr:hover td {
  background-color: #fcfbf8;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem !important;
  color: #8c9ba5;
  font-size: 0.95rem;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  padding: 1rem 0;
  width: 100%;
}

.empty-icon {
  font-size: 2.2rem;
  line-height: 1;
}

.empty-text {
  font-size: 0.95rem;
  color: #71828d;
  font-weight: 600;
}

.btn-refresh-sm {
  margin-top: 0.25rem;
  padding: 0.4rem 0.9rem;
  background-color: #fff8eb;
  color: #957b48;
  font-size: 0.82rem;
  font-weight: 600;
  border-radius: 6px;
  border: 1px solid #dfd5c2;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh-sm:hover {
  background-color: #faeed7;
}

.loading-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid #ccc;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  vertical-align: middle;
  margin-right: 6px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* STT */
.stt-cell {
  font-weight: 500;
  color: #9aa0a0;
  font-size: 0.95rem;
}

/* Ô chứa ảnh sản phẩm */
.product-img-box {
  position: relative;
  width: 60px;
  height: 60px;
  margin: 0 auto;
  border-radius: 8px;
  overflow: visible;
  background-color: #f5f5f5;
  border: 1px solid #eae5db;
}

.product-img-box img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 7px;
}

.product-img-discount {
  position: absolute !important;
  top: -6px !important;
  right: -12px !important;
  left: auto !important;
  bottom: auto !important;
  margin: 0 !important;
  font-size: 0.65rem !important;
  font-weight: 800 !important;
  padding: 1px 4px !important;
  line-height: 1.15 !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 5px rgba(220, 38, 38, 0.25), 0 1px 3px rgba(0, 0, 0, 0.12);
  z-index: 5;
  white-space: nowrap;
}

.code-cell {
  font-size: 0.88rem;
  color: #8c6a46;
  font-family: monospace, sans-serif;
  letter-spacing: 0.4px;
}

/* Tên sản phẩm */
.product-name-cell {
  min-width: 160px;
}

.name-text {
  font-weight: 700;
  color: #2e3b40;
  font-size: 0.95rem;
}

.desc-text {
  font-size: 0.8rem;
  color: #8c9ba5;
  margin-top: 3px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.attr-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.attr-tag {
  display: inline-block;
  background: #eaf2f6;
  color: #496883;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 0.78rem;
  border: 1px solid #d4e2ed;
}

/* Giá tiền & Đợt giảm giá */
.price-cell {
  color: #1f272b;
  font-size: 0.95rem;
  line-height: 1.35;
}

.discounted-price-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.original-price-strikethrough {
  font-size: 0.8rem;
  color: #8c9ba5;
  text-decoration: line-through;
  font-weight: 500;
}

.active-discount-price {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.sale-price-text {
  font-weight: 700;
  color: #c94a29;
  font-size: 0.93rem;
}

.discount-badge-percent {
  display: inline-flex;
  align-items: center;
  padding: 1px 5px;
  font-size: 0.72rem;
  font-weight: 700;
  color: #dc2626;
  background-color: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 4px;
  line-height: 1.2;
}

.normal-price-display {
  color: #1f272b;
  font-size: 0.93rem;
}


/* Tồn kho badge */
.stock-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.82rem;
}
.stock-badge.in-stock {
  background-color: #eaf5ed;
  color: #247541;
  border: 1px solid #bce1c7;
}
.stock-badge.out-of-stock {
  background-color: #f7edeb;
  color: #b34a3b;
  border: 1px solid #ecc9c3;
}

/* Ngày tạo */
.date-cell {
  color: #6f7c82;
  font-size: 0.88rem;
}

.status-badge {
  display: inline-block;
  padding: 0.35rem 0.8rem;
  font-weight: 700;
  border-radius: 6px;
  font-size: 0.82rem;
  white-space: nowrap;
}

.status-badge.status-active {
  background-color: #eaf5ed;
  color: #247541;
  border: 1px solid #bce1c7;
}

.status-badge.status-inactive {
  background-color: #f7edeb;
  color: #b34a3b;
  border: 1px solid #ecc9c3;
}

.status-toggle-btn {
  display: inline-block;
  padding: 0.35rem 0.8rem;
  font-weight: 700;
  border-radius: 6px;
  font-size: 0.82rem;
  white-space: nowrap;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-toggle-btn.status-active {
  background-color: #eaf5ed;
  color: #247541;
  border-color: #bce1c7;
}

.status-toggle-btn.status-active:hover {
  background-color: #deede1;
}

.status-toggle-btn.status-inactive {
  background-color: #f7edeb;
  color: #b34a3b;
  border-color: #ecc9c3;
}

.status-toggle-btn.status-inactive:hover {
  background-color: #f0dedb;
}

/* Các nút hành động */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-action {
  width: 28px;
  height: 28px;
  border: 1px solid transparent;
  background-color: #ffffff;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s ease;
  padding: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-action svg {
  display: block;
}

.btn-status {
  color: #dc2626;
  background-color: #fff1f2;
  border-color: #ffe4e6;
}
.btn-status.is-active {
  color: #059669;
  background-color: #ecfdf5;
  border-color: #a7f3d0;
}
.btn-status:hover {
  transform: translateY(-1px);
  filter: brightness(0.95);
}

.btn-edit {
  color: #496883;
  background-color: #ffffff;
  border: 1px solid #e7e2d6;
}
.btn-edit:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: translateY(-1px);
}

.btn-view {
  color: #496883;
  background-color: #ffffff;
  border: 1px solid #e7e2d6;
}
.btn-view:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: translateY(-1px);
}

.btn-delete {
  color: #b34a3b;
  background-color: #f7edeb;
  border: 1px solid #ecc9c3;
}
.btn-delete:hover {
  background-color: #b34a3b;
  color: #ffffff;
  border-color: #b34a3b;
  transform: scale(1.08);
}

/* 4. Thanh phân trang */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f4f8;
}

.pagination-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-size-selector {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.88rem;
  color: #64748b;
}

.page-size-label {
  font-weight: 500;
  color: #64748b;
}

.page-size-select {
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background-color: #ffffff;
  color: #1e293b;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.page-size-select:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.page-size-unit {
  font-size: 0.85rem;
  color: #64748b;
}

.pagination-info {
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  color: #555e65;
  line-height: 1.5;
}

.pagination-center {
  display: flex;
  gap: 0.35rem;
}

.pg-box {
  min-width: 34px;
  height: 34px;
  padding: 0 6px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  color: #3d4a50;
  transition: all 0.2s;
}

.pg-box:hover:not(:disabled) {
  border-color: #496883;
  color: #496883;
}

.pg-box:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pg-box.active {
  border-color: #496883;
  color: #ffffff;
  background-color: #496883;
}

/* MODAL */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
  animation: fadeIn 0.2s ease;
}

.modal-dialog {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 720px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f0ede6;
}

.modal-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #888;
  cursor: pointer;
}
.btn-close-modal:hover {
  color: #000;
}

.modal-form {
  padding: 1.5rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #3d4a50;
}

.form-label .required {
  color: #d9534f;
}

.form-control {
  height: 2.45rem;
  border: 1px solid #d5d9df;
  border-radius: 6px;
  padding: 0 0.8rem;
  font-size: 0.9rem;
  outline: none;
  transition: all 0.2s;
  background-color: #ffffff;
}

.form-control:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.1);
}

textarea.form-control {
  height: auto;
  padding: 0.6rem 0.8rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #f0ede6;
}

.btn-secondary {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-secondary:hover {
  background-color: #faeed7;
}

.btn-primary {
  background-color: #3b596a;
  border: 1px solid #324b5a;
  color: #ffffff;
}
.btn-primary:hover:not(:disabled) {
  background-color: #2c4350;
  border-color: #243742;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 992px) {
  .search-actions-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 0.85rem;
  }
  .action-buttons-col {
    justify-content: flex-end;
    flex-wrap: wrap;
    width: 100%;
  }
}

@media (max-width: 600px) {
  .product-page-container {
    padding: 1rem 0.75rem 2rem;
  }
  .product-card {
    padding: 1rem;
  }
  .action-buttons-col {
    flex-direction: column;
    align-items: stretch;
  }
  .action-buttons-col .btn {
    width: 100%;
  }
  .filter-attributes-section {
    flex-direction: column;
    align-items: stretch;
    padding: 0.75rem;
  }
  .funnel-icon-wrap {
    display: none;
  }
  .attribute-dropdowns-grid {
    grid-template-columns: 1fr;
  }
  .pagination-footer {
    flex-direction: column;
    gap: 0.75rem;
    align-items: center;
    text-align: center;
  }
  .pagination-center {
    flex-wrap: wrap;
    justify-content: center;
  }
  .modal-dialog {
    max-width: 95%;
    margin: 10px;
  }
}

/* Modal xác nhận chuẩn thiết kế */
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
  max-width: 460px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: modalScale 0.2s ease-out;
}

@keyframes modalScale {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
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

/* ========================================= */
/* CSS CHO MODAL XEM CHI TIẾT SẢN PHẨM      */
/* ========================================= */
.product-detail-modal-box {
  background: #ffffff;
  border-radius: 12px;
  width: 920px;
  max-width: 95vw;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.22);
  border: 1px solid #e2ddd3;
  overflow: hidden;
  animation: modalScaleIn 0.22s ease-out;
}

@keyframes modalScaleIn {
  from {
    transform: scale(0.96);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.pdm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.1rem 1.5rem;
  background: #faf7f0;
  border-bottom: 1px solid #e8e2d5;
}

.pdm-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.pdm-icon-badge {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #ede8dc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  border: 1px solid #ded7c8;
}

.pdm-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #2c3e50;
}

.pdm-subtitle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.pdm-code {
  font-size: 0.82rem;
  font-weight: 700;
  color: #496883;
  background: #edf2f7;
  padding: 2px 7px;
  border-radius: 4px;
}

.pdm-dot {
  color: #cbd5e1;
}

.btn-pdm-close {
  background: none;
  border: none;
  font-size: 1.6rem;
  line-height: 1;
  cursor: pointer;
  color: #8898aa;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  transition: all 0.15s;
}

.btn-pdm-close:hover {
  background: #e2ddd3;
  color: #1e293b;
}

.pdm-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.4rem 1.5rem;
  background: #ffffff;
}

.pdm-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  gap: 1rem;
  color: #64748b;
  font-size: 0.95rem;
}

.pdm-content-scroll {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.pdm-overview-row {
  display: flex;
  gap: 1.5rem;
}

.pdm-image-col {
  width: 250px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.pdm-image-card {
  width: 100%;
  height: 230px;
  border-radius: 10px;
  border: 1px solid #e7e2d6;
  overflow: hidden;
  background: #fdfcf9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pdm-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pdm-price-summary {
  background: #f7f5ee;
  border: 1px solid #e8e2d5;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  text-align: center;
}

.pdm-price-title {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #7f8c8d;
  margin-bottom: 0.25rem;
}

.pdm-price-amount {
  font-size: 1.05rem;
  font-weight: 700;
  color: #c0392b;
}

.pdm-discount-tag {
  margin-top: 0.4rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: #dc2626;
  background: #fee2e2;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
}

.pdm-details-col {
  flex: 1;
  min-width: 0;
}

.pdm-section-heading {
  font-size: 0.92rem;
  font-weight: 700;
  color: #2c3e50;
  text-transform: uppercase;
  margin: 0 0 0.75rem 0;
  padding-bottom: 0.35rem;
  border-bottom: 2px solid #ede8dc;
  letter-spacing: 0.3px;
}

.pdm-grid-props {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.6rem 1.25rem;
  background: #faf8f3;
  padding: 0.9rem 1.1rem;
  border-radius: 8px;
  border: 1px solid #ebe5d8;
}

.pdm-prop-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
}

.prop-label {
  color: #64748b;
  font-weight: 500;
}

.prop-val {
  color: #1e293b;
  font-weight: 600;
}

.pdm-description-box {
  margin-top: 0.85rem;
  padding: 0.75rem 1rem;
  background: #faf8f3;
  border-radius: 8px;
  border: 1px solid #ebe5d8;
  font-size: 0.85rem;
}

.prop-desc-text {
  margin: 0.35rem 0 0 0;
  color: #475569;
  line-height: 1.45;
}

.pdm-variants-block {
  border-top: 1px solid #e7e2d6;
  padding-top: 1.25rem;
}

.pdm-variants-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.btn-header-edit-variants {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background-color: #ffffff;
  color: #496883;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-header-edit-variants:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
}

.pdm-table-wrap {
  border: 1px solid #e2ddd3;
  border-radius: 8px;
  overflow: hidden;
}

.pdm-variants-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

.pdm-variants-table th {
  background: #f4f0e6;
  padding: 0.65rem 0.85rem;
  font-weight: 600;
  color: #496883;
  border-bottom: 1px solid #ded8cb;
  text-align: left;
}

.pdm-variants-table td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid #f1ece1;
  vertical-align: middle;
}

.pdm-variants-table tr:last-child td {
  border-bottom: none;
}

.pdm-variants-table tr:hover td {
  background: #faf8f3;
}

.pdm-color-cell {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 500;
}

.color-disc {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.18);
  display: inline-block;
  flex-shrink: 0;
}

.pdm-size-badge {
  display: inline-block;
  padding: 2px 7px;
  background: #edf2f7;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-weight: 600;
  font-size: 0.8rem;
  color: #334155;
}

.old-price {
  text-decoration: line-through;
  font-size: 0.75rem;
  color: #94a3b8;
  display: block;
}

.discounted-price {
  color: #dc2626;
  font-size: 0.85rem;
  font-weight: 700;
  display: block;
}

.pdm-empty-variants {
  padding: 2rem;
  text-align: center;
  color: #94a3b8;
  background: #faf8f3;
  border-radius: 8px;
  font-size: 0.88rem;
}

.pdm-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.9rem 1.5rem;
  background: #faf7f0;
  border-top: 1px solid #e8e2d5;
}

.btn-edit-product-action {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  padding: 0.6rem 1.3rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit-product-action:hover {
  background: #385269;
}
</style>