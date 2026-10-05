<template>
  <div class="dot-giam-gia-wrapper">
    <!-- Thông báo nổi (Toast Notification) -->
    <transition name="fade">
      <div v-if="toast.show" :class="['toast-msg', `toast-${toast.type}`]">
        <svg v-if="toast.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
        <svg v-else-if="toast.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        <span>{{ toast.message }}</span>
      </div>
    </transition>

    <!-- ========================================================
         VIEW 1: MÀN HÌNH DANH SÁCH ĐỢT GIẢM GIÁ
    ======================================================== -->
    <div v-if="currentView === 'list'" class="view-list">
      <!-- 1. Thanh Breadcrumb trên cùng -->
      <div class="breadcrumb-header">
        <div class="breadcrumb-left">
          <span class="breadcrumb-text">
            Quản lý giảm giá <span class="slash">/</span> <b>Đợt giảm giá</b>
          </span>
        </div>
      </div>

      <!-- 2. Khung Bộ lọc -->
      <div class="content-card filter-card">
        <div class="card-header-filter">
          <div class="filter-icon-box">
            <span class="filter-icon">🍸</span>
          </div>
          <div class="filter-title-wrap">
            <h3 class="filter-title">Bộ lọc</h3>
            <p class="filter-subtitle">Tra cứu nhanh dữ liệu từ cơ sở dữ liệu.</p>
          </div>
        </div>

        <!-- Lưới 4 ô lọc -->
        <div class="filter-inputs-grid">
          <!-- Tìm kiếm -->
          <div class="form-field">
            <label>Tìm kiếm</label>
            <div class="input-inner">
              <span class="prefix-icon">🔍</span>
              <input
                  type="text"
                  v-model="filters.keyword"
                  @input="handleSearchInput"
                  placeholder="Mã, tên đợt giảm giá..."
              />
            </div>
          </div>

          <!-- Ngày bắt đầu -->
          <div class="form-field">
            <label>Ngày bắt đầu</label>
            <div class="input-inner">
              <input
                  type="date"
                  v-model="filters.startDate"
                  @change="handleFilterChange"
              />
            </div>
          </div>

          <!-- Ngày kết thúc -->
          <div class="form-field">
            <label>Ngày kết thúc</label>
            <div class="input-inner">
              <input
                  type="date"
                  v-model="filters.endDate"
                  @change="handleFilterChange"
              />
            </div>
          </div>

          <!-- Trạng thái -->
          <div class="form-field">
            <label>Trạng thái</label>
            <select v-model="filters.trangThai" @change="handleFilterChange">
              <option value="">Tất cả trạng thái</option>
              <option value="1">Hoạt Động</option>
              <option value="0">Ngưng Hoạt Động</option>
            </select>
          </div>
        </div>

        <!-- Cụm nút thao tác -->
        <div class="filter-actions">
          <button class="btn btn-reset" @click="resetFilters">
            <span class="btn-icon">↺</span> Đặt lại bộ lọc
          </button>
          <button class="btn btn-excel" @click="exportToExcel" :disabled="exporting">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="8" y1="13" x2="16" y2="13"></line>
              <line x1="8" y1="17" x2="16" y2="17"></line>
            </svg>
            <span>{{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}</span>
          </button>
          <button class="btn btn-theme" @click="openCreateView">
            <span>+</span> Tạo đợt giảm giá
          </button>
        </div>
      </div>

      <!-- 3. Bảng danh sách các đợt giảm giá -->
      <div class="content-card table-card">
        <div class="table-header-row">
          <h3 class="table-title">Danh sách các đợt giảm giá</h3>
          <span class="record-count">
            Hiển thị <b>{{ campaigns.length }}</b> / <b>{{ totalElements }}</b> bản ghi
            (Trang {{ currentPage }}/{{ totalPages || 1 }})
          </span>
        </div>

        <div class="table-responsive">
          <table class="custom-table">
            <thead>
            <tr>
              <th style="width: 50px; text-align: center;">STT</th>
              <th style="width: 130px;">Mã</th>
              <th>Tên đợt giảm giá</th>
              <th style="width: 110px;">Giá trị giảm</th>
              <th style="width: 150px;">Ngày bắt đầu</th>
              <th style="width: 150px;">Ngày kết thúc</th>
              <th style="width: 155px; text-align: center; white-space: nowrap;">Trạng thái</th>
              <th style="width: 130px; text-align: center; white-space: nowrap;">Hành động</th>
            </tr>
            </thead>
            <tbody>
            <!-- Trạng thái Đang tải -->
            <tr v-if="loading">
              <td colspan="8" class="empty-cell">
                <span class="loading-spinner">⏳</span> Đang tải dữ liệu từ database SQL Server...
              </td>
            </tr>

            <!-- Trạng thái Lỗi -->
            <tr v-else-if="errorMessage">
              <td colspan="8" class="error-cell">
                {{ errorMessage }}
                <div style="margin-top: 8px;">
                  <button class="btn btn-reset" style="height: 2rem; font-size: 0.85rem;" @click="fetchData">Thử lại</button>
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
              <td style="text-align: center;" class="text-muted">
                {{ (currentPage - 1) * pageSize + index + 1 }}
              </td>
              <td class="font-bold text-blue">{{ item.maDotGiamGia || '—' }}</td>
              <td class="font-medium text-title">{{ item.tenDotGiamGia || '—' }}</td>
              <td class="font-bold text-dark">
                {{ item.phanTramGiam != null ? item.phanTramGiam + '%' : '0%' }}
              </td>
              <td class="text-date">{{ formatDate(item.ngayBatDau) }}</td>
              <td class="text-date">{{ formatDate(item.ngayKetThuc) }}</td>

              <!-- Trạng thái: Hoạt Động / Ngưng Hoạt Động -->
              <td style="text-align: center;">
                <span
                    class="badge-status"
                    :class="item.trangThai === 1 ? 'status-active' : 'status-inactive'"
                >
                  {{ item.trangThai === 1 ? 'Hoạt Động' : 'Ngưng Hoạt Động' }}
                </span>
              </td>

              <!-- Cột Hành động: 2 nút vuông bo góc mềm (Nguồn + Sửa) y hệt ảnh thiết kế -->
              <td style="text-align: center;">
                <div class="action-buttons">
                  <!-- Nút 1: Đổi trạng thái (Power Icon dạng thẻ vuông) -->
                  <button
                      class="btn-action-square btn-action-power"
                      :class="item.trangThai === 1 ? 'power-active' : 'power-inactive'"
                      :title="item.trangThai === 1 ? 'Chuyển sang Ngưng Hoạt Động' : 'Chuyển sang Hoạt Động'"
                      @click="toggleStatus(item)"
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                      <line x1="12" y1="2" x2="12" y2="12"></line>
                    </svg>
                  </button>

                  <!-- Nút 2: Sửa đợt giảm giá (Edit Icon dạng thẻ vuông) -->
                  <button
                      class="btn-action-square btn-action-edit"
                      title="Chỉnh sửa đợt giảm giá"
                      @click="openEditView(item)"
                  >
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

        <!-- Phân trang động -->
        <div class="pagination-footer">
          <div class="page-size-selector">
            <label class="page-size-label">Số dòng/trang:</label>
            <select v-model="pageSize" @change="onPageSizeChange">
              <option :value="5">5</option>
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </div>

          <div class="pagination-controls" v-if="totalPages > 0">
            <button
                class="pg-btn"
                :disabled="currentPage <= 1 || loading"
                @click="goToPage(1)"
                title="Trang đầu"
            >⇤</button>
            <button
                class="pg-btn"
                :disabled="currentPage <= 1 || loading"
                @click="goToPage(currentPage - 1)"
                title="Trang trước"
            >‹</button>

            <button
                v-for="p in visiblePages"
                :key="p"
                class="pg-btn"
                :class="{ active: p === currentPage }"
                :disabled="loading"
                @click="goToPage(p)"
            >
              {{ p }}
            </button>

            <button
                class="pg-btn"
                :disabled="currentPage >= totalPages || loading"
                @click="goToPage(currentPage + 1)"
                title="Trang sau"
            >›</button>
            <button
                class="pg-btn"
                :disabled="currentPage >= totalPages || loading"
                @click="goToPage(totalPages)"
                title="Trang cuối"
            >⇥</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================================
         VIEW 2: MÀN HÌNH TẠO MỚI / CHỈNH SỬA TOÀN TRANG (FULL-PAGE)
    ======================================================== -->
    <div v-else class="view-form">
      <!-- Header điều hướng phía trên -->
      <div class="form-header-bar">
        <div class="form-breadcrumb">
          <button class="btn-back-link" @click="backToList">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
          </button>
          <span class="breadcrumb-text">
            Quản lý giảm giá / <b>Đợt giảm giá</b> / <b>{{ isEditing ? 'Chi tiết đợt giảm giá' : 'Tạo mới đợt giảm giá' }}</b>
          </span>
        </div>
        <div class="form-header-actions">
          <button class="btn btn-outline" @click="backToList">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            Hủy bỏ
          </button>
          <button class="btn btn-theme" @click="onSaveClick">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            {{ isEditing ? 'Lưu thay đổi' : 'Tạo đợt giảm giá' }}
          </button>
        </div>
      </div>

      <!-- Layout 2 cột: Cột trái Preview Card & Cột phải Form Nhập liệu -->
      <div class="form-main-layout">
        <!-- Cột trái: Card Preview tóm tắt đợt giảm giá -->
        <div class="preview-col">
          <div class="content-card preview-card">
            <div class="preview-icon-box">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#b88628" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            </div>
            <div class="preview-badge-code">
              {{ formData.maDotGiamGia || 'DGG_NEW' }}
            </div>
            <h3 class="preview-title">
              {{ formData.tenDotGiamGia || 'Tên đợt giảm giá...' }}
            </h3>

            <div class="preview-divider"></div>

            <div class="preview-info-row">
              <span class="preview-label">Trạng thái:</span>
              <span :class="['badge-status', formData.trangThai === 1 ? 'status-active' : 'status-inactive']">
                {{ formData.trangThai === 1 ? 'Hoạt Động' : 'Ngưng Hoạt Động' }}
              </span>
            </div>

            <div class="preview-info-row">
              <span class="preview-label">Mức giảm:</span>
              <span class="preview-discount-val">
                {{ formData.phanTramGiam ? formData.phanTramGiam + '%' : '0%' }}
              </span>
            </div>

            <div class="preview-info-row">
              <span class="preview-label">Sản phẩm áp dụng:</span>
              <b class="text-dark">{{ selectedProductCount }} sản phẩm</b>
            </div>

            <div class="preview-info-col">
              <span class="preview-label">Thời gian áp dụng:</span>
              <span class="preview-date-range">
                {{ formatInputDateDisplay(formData.ngayBatDau) }} ➔ {{ formatInputDateDisplay(formData.ngayKetThuc) }}
              </span>
            </div>
          </div>
        </div>

        <!-- Cột phải: Các khối Card nhập liệu -->
        <div class="form-col">
          <!-- KHỐI 1: Thông tin cơ bản -->
          <div class="content-card form-section-card">
            <div class="section-card-head">
              <div class="sec-icon-box">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#b88628" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
              </div>
              <div>
                <h4 class="sec-title">Thông tin cơ bản</h4>
                <p class="sec-desc">Mã đợt, tên đợt và trạng thái hoạt động</p>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-field">
                <label>
                  Mã đợt giảm giá
                  <span v-if="isEditing" class="tag-lock">🔒 Cố định</span>
                  <span v-else class="text-hint">(tự sinh nếu để trống)</span>
                </label>
                <input
                    type="text"
                    v-model="formData.maDotGiamGia"
                    :disabled="isEditing"
                    placeholder="VD: DGG01"
                    :class="{ 'input-disabled': isEditing }"
                />
              </div>

              <div class="form-field">
                <label>Tên đợt giảm giá <span class="text-danger">*</span></label>
                <input
                    type="text"
                    v-model="formData.tenDotGiamGia"
                    placeholder="Nhập tên đợt giảm giá..."
                />
              </div>

              <div class="form-field">
                <label>Trạng thái hoạt động <span class="text-danger">*</span></label>
                <select v-model.number="formData.trangThai">
                  <option :value="1">Hoạt Động</option>
                  <option :value="0">Ngưng Hoạt Động</option>
                </select>
              </div>

              <div class="form-field">
                <label>Mức giảm (%) <span class="text-danger">*</span></label>
                <div class="input-inner">
                  <input
                      type="number"
                      min="1"
                      max="100"
                      step="0.5"
                      v-model.number="formData.phanTramGiam"
                      placeholder="VD: 20"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- KHỐI 2: Sản phẩm áp dụng (Giao diện + Tìm kiếm) -->
          <div class="content-card form-section-card">
            <div class="section-card-head">
              <div class="sec-icon-box">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#b88628" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              </div>
              <div>
                <h4 class="sec-title">Sản phẩm áp dụng</h4>
                <p class="sec-desc">Chọn các sản phẩm áp dụng chương trình đợt giảm giá này</p>
              </div>
            </div>

            <!-- Thanh tìm kiếm sản phẩm giao diện -->
            <div class="product-search-bar">
              <div class="input-inner flex-1">
                <span class="prefix-icon">🔍</span>
                <input
                    type="text"
                    v-model="productSearchKeyword"
                    placeholder="Tìm kiếm sản phẩm theo mã, tên..."
                    @keyup.enter="handleSearchProductUI"
                />
              </div>
              <button class="btn btn-theme btn-sm" @click="handleSearchProductUI">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                Tìm kiếm
              </button>
              <button class="btn btn-outline btn-sm" @click="toggleSelectAllProducts">
                {{ isAllProductsSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả' }}
              </button>
            </div>

            <!-- Bảng danh sách sản phẩm mẫu -->
            <div class="product-table-box">
              <table class="custom-table product-table">
                <thead>
                <tr>
                  <th style="width: 40px; text-align: center;">
                    <input type="checkbox" :checked="isAllProductsSelected" @change="toggleSelectAllProducts" />
                  </th>
                  <th style="width: 90px;">Mã SP</th>
                  <th>Tên sản phẩm</th>
                  <th style="width: 120px;">Đơn giá</th>
                  <th style="width: 100px; text-align: center;">Trạng thái</th>
                </tr>
                </thead>
                <tbody>
                <tr
                    v-for="p in filteredProducts"
                    :key="p.id"
                    :class="{ 'row-active': p.selected }"
                    @click="p.selected = !p.selected"
                >
                  <td style="text-align: center;" @click.stop>
                    <input type="checkbox" v-model="p.selected" />
                  </td>
                  <td class="font-bold text-blue">{{ p.ma }}</td>
                  <td class="font-medium text-title">{{ p.ten }}</td>
                  <td class="font-bold text-dark">{{ formatMoney(p.gia) }}</td>
                  <td style="text-align: center;">
                    <span :class="['tag-select', p.selected ? 'tag-selected' : 'tag-unselected']">
                      {{ p.selected ? 'Đã chọn' : 'Chưa' }}
                    </span>
                  </td>
                </tr>
                <tr v-if="filteredProducts.length === 0">
                  <td colspan="5" class="empty-cell">Không tìm thấy sản phẩm nào!</td>
                </tr>
                </tbody>
              </table>
            </div>

            <div class="product-count-summary">
              Đã chọn: <b>{{ selectedProductCount }}</b> sản phẩm áp dụng đợt giảm giá.
            </div>
          </div>

          <!-- KHỐI 3: Thời gian hiệu lực -->
          <div class="content-card form-section-card">
            <div class="section-card-head">
              <div class="sec-icon-box">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#d32f2f" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              </div>
              <div>
                <h4 class="sec-title">Thời gian hiệu lực</h4>
                <p class="sec-desc">Ngày bắt đầu và ngày kết thúc chương trình đợt giảm giá</p>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-field">
                <label>Ngày bắt đầu <span class="text-danger">*</span></label>
                <input type="datetime-local" v-model="formData.ngayBatDau" />
              </div>
              <div class="form-field">
                <label>Ngày kết thúc <span class="text-danger">*</span></label>
                <input type="datetime-local" v-model="formData.ngayKetThuc" />
              </div>
            </div>
          </div>

          <!-- Footer thanh nút cuối trang -->
          <div class="content-card form-footer-bar">
            <button class="btn btn-outline" @click="backToList">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
              Quay lại danh sách
            </button>
            <button class="btn btn-theme" @click="onSaveClick">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
              {{ isEditing ? 'Cập nhật đợt giảm giá' : 'Tạo đợt giảm giá' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================================
         MODAL: XÁC NHẬN CẬP NHẬT / TẠO MỚI (Y NHƯ ẢNH MẪU 1)
    ======================================================== -->
    <div v-if="showConfirmModal" class="modal-mask" @click.self="showConfirmModal = false">
      <div class="confirm-modal-box">
        <div class="confirm-header">
          <div class="confirm-title-left">
            <span class="warning-triangle">⚠️</span>
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
              <span class="confirm-label">Mức giảm:</span>
              <span class="confirm-val font-bold text-dark">{{ formData.phanTramGiam }}%</span>
            </div>
            <div class="confirm-row">
              <span class="confirm-label">Thời hạn:</span>
              <span class="confirm-val">{{ formatInputDateDisplay(formData.ngayBatDau) }} ➔ {{ formatInputDateDisplay(formData.ngayKetThuc) }}</span>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api.js'

// Điều hướng view: 'list' (danh sách) hoặc 'form' (màn hình tạo/sửa)
const currentView = ref('list')

// Thông báo Toast
const toast = ref({ show: false, message: '', type: 'success' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => { toast.value.show = false }, 3000)
}

// Dữ liệu danh sách đợt giảm giá
const campaigns = ref([])
const loading = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(5)
const totalPages = ref(0)
const totalElements = ref(0)

// Modal xác nhận
const showConfirmModal = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const exporting = ref(false)

// Form dữ liệu đợt giảm giá
const formData = ref({
  id: null,
  maDotGiamGia: '',
  tenDotGiamGia: '',
  phanTramGiam: 15,
  ngayBatDau: '',
  ngayKetThuc: '',
  trangThai: 1
})

// Dữ liệu sản phẩm mẫu (Giao diện)
const productSearchKeyword = ref('')
const products = ref([
  { id: 1, ma: 'SP001', ten: 'Áo thun Cotton Classic Cổ Tròn', gia: 199000, selected: false },
  { id: 2, ma: 'SP002', ten: 'Áo polo Phối Bo Cổ Thể Thao', gia: 249000, selected: false },
  { id: 3, ma: 'SP003', ten: 'Áo thun Oversize Unisex Trẻ Trung', gia: 219000, selected: false },
  { id: 4, ma: 'SP004', ten: 'Áo thun Basic Regular Fit Thoáng Mát', gia: 189000, selected: false },
  { id: 5, ma: 'SP005', ten: 'Áo phông Graphic Streetwear Retro', gia: 289000, selected: false },
  { id: 6, ma: 'SP006', ten: 'Áo polo Premium Co Giãn 4 Chiều', gia: 319000, selected: false }
])

const filteredProducts = computed(() => {
  const kw = productSearchKeyword.value.trim().toLowerCase()
  if (!kw) return products.value
  return products.value.filter(p => p.ma.toLowerCase().includes(kw) || p.ten.toLowerCase().includes(kw))
})

const isAllProductsSelected = computed(() => filteredProducts.value.length > 0 && filteredProducts.value.every(p => p.selected))
const selectedProductCount = computed(() => products.value.filter(p => p.selected).length)

const toggleSelectAllProducts = () => {
  const next = !isAllProductsSelected.value
  filteredProducts.value.forEach(p => (p.selected = next))
  showToast(next ? `Đã chọn tất cả sản phẩm!` : 'Đã bỏ chọn tất cả!', 'info')
}

const handleSearchProductUI = () => {
  showToast(`Tìm thấy ${filteredProducts.value.length} sản phẩm phù hợp!`, 'info')
}

// Bộ lọc
const filters = ref({
  keyword: '',
  startDate: '',
  endDate: '',
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
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month}/${year} ${hours}:${minutes}`
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

    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
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
    if (filters.value.trangThai !== '' && filters.value.trangThai !== null) {
      params.trangThai = Number(filters.value.trangThai)
    }

    const response = await api.get('/api/dot-giam-gia', { params })
    const data = response.data
    campaigns.value = data.content || []
    totalPages.value = data.totalPages || 0
    totalElements.value = data.totalElements || 0
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

const handleSearchInput = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    fetchData()
  }, 350)
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchData()
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    startDate: '',
    endDate: '',
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

// Đổi trạng thái Hoạt Động / Ngưng Hoạt Động
const toggleStatus = async (item) => {
  const isActivating = item.trangThai !== 1
  const targetStatus = isActivating ? 'Hoạt Động' : 'Ngưng Hoạt Động'
  const confirmMsg = `Bạn có chắc chắn muốn chuyển trạng thái đợt giảm giá "${item.tenDotGiamGia || item.maDotGiamGia}" sang [${targetStatus}]?`
  if (!confirm(confirmMsg)) return

  try {
    const res = await api.put(`/api/dot-giam-gia/${item.id}/toggle-status`)
    item.trangThai = res.data.trangThai
    showToast(`Đã chuyển sang ${targetStatus} thành công!`, 'success')
  } catch (error) {
    console.error('Lỗi khi đổi trạng thái:', error)
    showToast(`Không thể cập nhật trạng thái đợt giảm giá!`, 'error')
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
    if (filters.value.trangThai !== '' && filters.value.trangThai !== null) {
      params.trangThai = Number(filters.value.trangThai)
    }

    const res = await api.get('/api/dot-giam-gia', { params })
    const dataList = res.data?.content || campaigns.value || []

    if (!dataList || dataList.length === 0) {
      showToast('Không có dữ liệu đợt giảm giá để xuất Excel!', 'error')
      return
    }

    const now = new Date()
    const nowStr = `${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`

    let rowsHtml = ''
    dataList.forEach((item, idx) => {
      const statusText = item.trangThai === 1 ? 'Hoạt Động' : 'Ngưng Hoạt Động'
      const statusColor = item.trangThai === 1 ? '#2e7d32' : '#c62828'
      rowsHtml += `
        <tr>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${idx + 1}</td>
          <td style="text-align: center; font-weight: bold; color: #304b60; border: 1px solid #bfbfbf; padding: 6px;">${item.maDotGiamGia || ''}</td>
          <td style="border: 1px solid #bfbfbf; padding: 6px;">${item.tenDotGiamGia || ''}</td>
          <td style="text-align: center; font-weight: bold; border: 1px solid #bfbfbf; padding: 6px;">${item.phanTramGiam != null ? item.phanTramGiam + '%' : '0%'}</td>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${formatDate(item.ngayBatDau)}</td>
          <td style="text-align: center; border: 1px solid #bfbfbf; padding: 6px;">${formatDate(item.ngayKetThuc)}</td>
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
              <td colspan="7" class="title" style="height: 38px; vertical-align: middle; border: none;">
                DANH SÁCH ĐỢT GIẢM GIÁ - CỬA HÀNG FF T-SHIRT
              </td>
            </tr>
            <tr>
              <td colspan="7" style="color: #666; font-style: italic; border: none; padding-bottom: 10px;">
                Thời gian xuất: ${nowStr} | Tổng số bản ghi: ${dataList.length} đợt giảm giá
              </td>
            </tr>
            <thead>
              <tr>
                <th style="width: 50px;">STT</th>
                <th style="width: 140px;">Mã đợt</th>
                <th style="width: 280px;">Tên đợt giảm giá</th>
                <th style="width: 120px;">Mức giảm</th>
                <th style="width: 170px;">Ngày bắt đầu</th>
                <th style="width: 170px;">Ngày kết thúc</th>
                <th style="width: 150px;">Trạng thái</th>
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
const openCreateView = () => {
  isEditing.value = false
  formData.value = {
    id: null,
    maDotGiamGia: '',
    tenDotGiamGia: '',
    phanTramGiam: 15,
    ngayBatDau: toInputDateTime(new Date()),
    ngayKetThuc: toInputDateTime(new Date(Date.now() + 14 * 86400000)),
    trangThai: 1
  }
  products.value.forEach(p => (p.selected = false))
  productSearchKeyword.value = ''
  currentView.value = 'form'
  showToast('Chuyển sang màn hình tạo đợt giảm giá!', 'info')
}

// Chuyển sang màn hình chỉnh sửa
const openEditView = (item) => {
  isEditing.value = true
  formData.value = {
    id: item.id,
    maDotGiamGia: item.maDotGiamGia || '',
    tenDotGiamGia: item.tenDotGiamGia || '',
    phanTramGiam: item.phanTramGiam,
    ngayBatDau: toInputDateTime(item.ngayBatDau),
    ngayKetThuc: toInputDateTime(item.ngayKetThuc),
    trangThai: item.trangThai != null ? item.trangThai : 1
  }
  products.value.forEach((p, i) => (p.selected = i < 3))
  productSearchKeyword.value = ''
  currentView.value = 'form'
  showToast(`Mở chỉnh sửa đợt "${item.tenDotGiamGia}"!`, 'info')
}

const backToList = () => {
  currentView.value = 'list'
}

// Bấm nút Lưu / Tạo -> kiểm tra và mở popup xác nhận
const onSaveClick = () => {
  if (!formData.value.tenDotGiamGia || formData.value.tenDotGiamGia.trim() === '') {
    showToast('Vui lòng nhập tên đợt giảm giá!', 'error')
    return
  }
  if (!formData.value.phanTramGiam || formData.value.phanTramGiam <= 0 || formData.value.phanTramGiam > 100) {
    showToast('Mức giảm giá phải từ 1% đến 100%!', 'error')
    return
  }
  if (!formData.value.ngayBatDau || !formData.value.ngayKetThuc) {
    showToast('Vui lòng chọn ngày bắt đầu và ngày kết thúc!', 'error')
    return
  }
  if (new Date(formData.value.ngayKetThuc) <= new Date(formData.value.ngayBatDau)) {
    showToast('Ngày kết thúc phải lớn hơn ngày bắt đầu!', 'error')
    return
  }

  showConfirmModal.value = true
}

// Xác nhận gọi API tạo mới hoặc cập nhật
const doSubmitAPI = async () => {
  try {
    submitting.value = true
    const payload = {
      maDotGiamGia: formData.value.maDotGiamGia?.trim() || null,
      tenDotGiamGia: formData.value.tenDotGiamGia.trim(),
      phanTramGiam: formData.value.phanTramGiam,
      ngayBatDau: new Date(formData.value.ngayBatDau).toISOString(),
      ngayKetThuc: new Date(formData.value.ngayKetThuc).toISOString(),
      trangThai: formData.value.trangThai
    }

    if (isEditing.value) {
      await api.put(`/api/dot-giam-gia/${formData.value.id}`, payload)
      showToast('Cập nhật đợt giảm giá thành công!', 'success')
    } else {
      await api.post('/api/dot-giam-gia', payload)
      showToast('Tạo mới đợt giảm giá thành công!', 'success')
    }

    showConfirmModal.value = false
    currentView.value = 'list'
    fetchData()
  } catch (e) {
    console.error('Lỗi khi lưu đợt giảm giá:', e)
    showToast('Lỗi khi lưu dữ liệu vào cơ sở dữ liệu!', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.dot-giam-gia-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Toast */
.toast-msg {
  position: fixed;
  top: 22px;
  right: 28px;
  padding: 0.8rem 1.3rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.9rem;
  font-weight: 600;
  box-shadow: 0 6px 20px rgba(0,0,0,0.15);
  z-index: 9999;
}
.toast-success { background: #2e7d32; color: #fff; }
.toast-error { background: #d32f2f; color: #fff; }
.toast-info { background: #304b60; color: #fff; }
.fade-enter-active, .fade-leave-active { transition: all 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-10px); }

/* Header breadcrumb */
.breadcrumb-header { margin-bottom: 1.2rem; display: flex; align-items: center; }
.breadcrumb-text { font-size: 0.95rem; color: #8c9597; }
.breadcrumb-text b { color: var(--blue, #496883); font-weight: 700; }
.slash { margin: 0 6px; color: #d8d4c9; }

/* Content Card */
.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  margin-bottom: 1.25rem;
}

.card-header-filter { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.2rem; }
.filter-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background-color: #f7eee1;
  display: grid;
  place-items: center;
}
.filter-icon { font-size: 1.2rem; color: #b18b52; }
.filter-title-wrap { display: flex; flex-direction: column; }
.filter-title { font-size: 1.1rem; font-weight: 700; margin: 0; color: #43545c; }
.filter-subtitle { font-size: 0.85rem; color: #8c9597; margin: 2px 0 0 0; }

.filter-inputs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.form-field { display: flex; flex-direction: column; gap: 0.45rem; }
.form-field label { font-size: 0.88rem; font-weight: 700; color: #4f5d63; }
.input-inner { position: relative; display: flex; align-items: center; }
.prefix-icon { position: absolute; left: 0.8rem; font-size: 0.95rem; color: #9aa0a0; pointer-events: none; }

.form-field input, .form-field select {
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
.form-field .input-inner input[type="text"] { padding-left: 2.4rem; }
.form-field input:focus, .form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.08);
}
.input-disabled { background: #f5f3ed !important; cursor: not-allowed; color: #8c9597; }

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
  border: 1px solid transparent;
}
.btn-sm { height: 2.2rem; padding: 0 0.9rem; font-size: 0.84rem; }
.btn-reset { border: 1px solid #dfd5c2; background-color: #fff8eb; color: #957b48; }
.btn-reset:hover { background-color: #faeed7; }
.btn-excel { background-color: #1d6f42; color: #ffffff; }
.btn-excel:hover:not(:disabled) { background-color: #145531; }
.btn-excel:disabled { opacity: 0.65; cursor: not-allowed; }
.btn-theme { background-color: #304b60; color: #ffffff; }
.btn-theme:hover { background-color: #223747; }
.btn-outline { background: #ffffff; border-color: #dfd5c2; color: #556268; }
.btn-outline:hover { background: #fbf9f4; border-color: #c99738; }

/* Table */
.table-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.1rem; }
.table-title { font-size: 1.1rem; font-weight: 700; margin: 0; color: #3c4d55; }
.record-count { font-size: 0.88rem; color: #8c9597; }
.table-responsive { overflow-x: auto; }
.custom-table { width: 100%; border-collapse: collapse; font-size: 0.95rem; }
.custom-table th { background-color: #faf9f6; color: #6f7c82; font-weight: 700; padding: 0.95rem 1rem; text-align: left; border-bottom: 1px solid #efede7; white-space: nowrap; }
.custom-table td { padding: 0.95rem 1rem; border-bottom: 1px solid #f2f0eb; color: #4b585e; vertical-align: middle; }
.custom-table tr:hover td { background-color: #fcfbf8; }

.empty-cell { text-align: center; padding: 2.5rem !important; color: #8c9597; font-style: italic; }
.error-cell { text-align: center; padding: 2.5rem !important; color: #e04f4f; font-weight: 500; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 600; }
.text-blue { color: var(--blue, #496883); font-family: monospace, sans-serif; letter-spacing: 0.5px; }
.text-title { color: #2b383e; }
.text-dark { color: #1f272b; }
.text-date { color: #556268; font-size: 0.9rem; }
.text-muted { color: #9aa0a0; }
.text-danger { color: #e04f4f; }
.text-green { color: #2e7d32; }
.text-hint { font-size: 0.8rem; font-weight: normal; color: #8c9597; }
.tag-lock { font-size: 0.75rem; background: #efeae0; padding: 0.15rem 0.4rem; border-radius: 4px; color: #556268; margin-left: 0.4rem; }

/* Badge Status */
.badge-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 122px;
  white-space: nowrap;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.35rem 0.75rem;
  border-radius: 12px;
  box-sizing: border-box;
}
.status-active { background-color: #edf6ef; color: #2e7d32; }
.status-inactive { background-color: #fdf0f0; color: #d32f2f; }

/* ========================================================
   CỘT HÀNH ĐỘNG: 2 NÚT VUÔNG BO GÓC MỀM (Y HỆT HÌNH ẢNH)
======================================================== */
.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  white-space: nowrap;
}

.btn-action-square {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
  flex-shrink: 0;
}

/* Nút 1: Power button xanh lá khi đang hoạt động */
.btn-action-power.power-active {
  background-color: #eaf5ea;
  border-color: #d1ebd1;
  color: #2e7d32;
}
.btn-action-power.power-active:hover {
  background-color: #d7edd7;
  color: #1e5a22;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(46, 125, 50, 0.18);
}

/* Nút 1: Power button đỏ khi đang ngưng hoạt động */
.btn-action-power.power-inactive {
  background-color: #fdeeed;
  border-color: #fad2d0;
  color: #c62828;
}
.btn-action-power.power-inactive:hover {
  background-color: #fbd6d4;
  color: #a71d1d;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(198, 40, 40, 0.18);
}

/* Nút 2: Edit button xanh pastel / navy dịu mắt chuẩn ảnh */
.btn-action-edit {
  background-color: #eef4f8;
  border-color: #d8e5ee;
  color: #304b60;
}
.btn-action-edit:hover {
  background-color: #dceaf2;
  color: #1e3342;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(48, 75, 96, 0.18);
}

/* Pagination */
.pagination-footer { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; margin-top: 1.25rem; padding-top: 1.1rem; border-top: 1px dashed var(--line, #e9e5db); }
.page-size-selector { display: flex; align-items: center; gap: 0.6rem; }
.page-size-label { font-size: 0.88rem; color: #6f7c82; font-weight: 600; }
.page-size-selector select { height: 2.2rem; padding: 0 0.75rem; border: 1px solid var(--line, #e9e5db); border-radius: 6px; background-color: #fcfbf8; color: var(--text, #3d4a50); font-size: 0.9rem; font-weight: 600; outline: none; cursor: pointer; }
.pagination-controls { display: flex; align-items: center; gap: 0.4rem; }
.pg-btn { min-width: 34px; height: 34px; padding: 0 0.5rem; border-radius: 6px; border: 1px solid var(--line, #e9e5db); background-color: #ffffff; color: #4b585e; font-size: 0.9rem; font-weight: 600; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; transition: all 0.2s; }
.pg-btn:hover:not(:disabled) { border-color: var(--blue, #496883); color: var(--blue, #496883); background-color: #f4f7f9; }
.pg-btn.active { background-color: var(--blue, #496883); color: #ffffff; border-color: var(--blue, #496883); }
.pg-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ========================================================
   STYLES CHO VIEW 2: TẠO MỚI / CHỈNH SỬA FULL-PAGE
======================================================== */
.form-header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 0.8rem; }
.form-breadcrumb { display: flex; align-items: center; gap: 0.6rem; }
.btn-back-link {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #e0dbce;
  cursor: pointer;
  display: grid;
  place-items: center;
  color: #304b60;
  transition: all 0.15s;
}
.btn-back-link:hover { background: #fbf9f4; border-color: #304b60; }
.form-header-actions { display: flex; gap: 0.6rem; }

.form-main-layout { display: grid; grid-template-columns: 290px 1fr; gap: 1.25rem; align-items: start; }
.preview-card { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 1.6rem 1.25rem; }
.preview-icon-box { width: 58px; height: 58px; border-radius: 12px; background: #fbf5e8; display: grid; place-items: center; margin-bottom: 0.8rem; }
.preview-badge-code { font-family: monospace; font-size: 0.82rem; font-weight: 700; color: #304b60; background: #eef3f7; padding: 0.2rem 0.6rem; border-radius: 4px; margin-bottom: 0.6rem; }
.preview-title { margin: 0; font-size: 1.05rem; font-weight: 700; color: #2b383e; line-height: 1.35; }
.preview-divider { width: 100%; height: 1px; background: #efeae0; margin: 1.1rem 0; }
.preview-info-row { width: 100%; display: flex; justify-content: space-between; align-items: center; font-size: 0.88rem; margin-bottom: 0.75rem; }
.preview-info-col { width: 100%; display: flex; flex-direction: column; align-items: flex-start; gap: 0.3rem; font-size: 0.85rem; margin-top: 0.4rem; padding-top: 0.75rem; border-top: 1px dashed #efeae0; }
.preview-label { color: #8c9597; font-size: 0.85rem; }
.preview-discount-val { font-size: 1.2rem; font-weight: 800; color: #c62828; }
.preview-date-range { color: #496883; font-weight: 600; font-size: 0.82rem; text-align: left; }

.form-section-card { padding: 1.4rem 1.6rem; margin-bottom: 1.1rem; }
.section-card-head { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.1rem; }
.sec-icon-box { width: 36px; height: 36px; border-radius: 8px; background: #fbf5e8; display: grid; place-items: center; }
.sec-title { margin: 0; font-size: 1.02rem; color: #304b60; font-weight: 700; }
.sec-desc { margin: 2px 0 0; font-size: 0.8rem; color: #8c9597; }
.form-grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }

.product-search-bar { display: flex; gap: 0.5rem; margin-bottom: 0.75rem; }
.product-table-box { max-height: 220px; overflow-y: auto; border: 1px solid #e9e5db; border-radius: 6px; background: #ffffff; }
.row-active td { background: #f0f7f3 !important; }
.tag-select { font-size: 0.75rem; padding: 0.15rem 0.5rem; border-radius: 4px; font-weight: 600; }
.tag-selected { background: #e8f5e9; color: #2e7d32; }
.tag-unselected { background: #eee; color: #888; }
.product-count-summary { font-size: 0.85rem; color: #8c9597; margin-top: 0.6rem; text-align: right; }
.form-footer-bar { display: flex; justify-content: flex-end; gap: 0.75rem; padding: 1rem 1.4rem; }

/* Modal Xác nhận */
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 10000; animation: fadeIn 0.15s ease-out; }
.confirm-modal-box { background: #ffffff; border-radius: 12px; width: 92%; max-width: 520px; box-shadow: 0 12px 36px rgba(0,0,0,0.18); overflow: hidden; }
.confirm-header { padding: 1rem 1.4rem; background: #fffcf5; border-bottom: 1px solid #f2edd9; display: flex; justify-content: space-between; align-items: center; }
.confirm-title-left { display: flex; align-items: center; gap: 0.6rem; }
.warning-triangle { font-size: 1.3rem; }
.confirm-header h4 { margin: 0; font-size: 1.1rem; color: #7f6027; font-weight: 700; }
.btn-close { background: none; border: none; font-size: 1.2rem; cursor: pointer; color: #8c9597; }

.confirm-body { padding: 1.4rem 1.4rem 1rem; }
.confirm-lead-text { font-size: 0.95rem; color: #3d4a50; margin: 0 0 1rem 0; line-height: 1.45; }
.confirm-lead-text b { color: #304b60; }

.confirm-info-card { background: #fbf9f4; border: 1px solid #efeae0; border-radius: 8px; padding: 1rem 1.25rem; display: flex; flex-direction: column; gap: 0.65rem; }
.confirm-row { display: flex; justify-content: space-between; font-size: 0.9rem; }
.confirm-label { color: #8c9597; }
.confirm-val { color: #2b383e; }

.confirm-footer { padding: 1rem 1.4rem 1.25rem; display: flex; justify-content: flex-end; gap: 0.75rem; }
.btn-cancel-confirm { background: #fffdf7; border: 1px solid #dfd5c2; color: #7f6027; font-weight: 700; height: 2.5rem; padding: 0 1.3rem; border-radius: 6px; }
.btn-cancel-confirm:hover { background: #fbf5e8; }
.btn-accept-confirm { background: #3a7d44; color: #ffffff; font-weight: 700; height: 2.5rem; padding: 0 1.4rem; border-radius: 6px; border: none; cursor: pointer; display: inline-flex; align-items: center; gap: 0.4rem; box-shadow: 0 2px 6px rgba(58, 125, 68, 0.25); }
.btn-accept-confirm:hover { background: #2f6937; }

.flex-1 { flex: 1; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1000px) {
  .form-main-layout { grid-template-columns: 1fr; }
  .filter-inputs-grid { grid-template-columns: repeat(2, 1fr); }
  .form-grid-2 { grid-template-columns: 1fr; }
}

@media (max-width: 650px) {
  .filter-inputs-grid { grid-template-columns: 1fr; }
  .pagination-footer { flex-direction: column; align-items: flex-start; }
}
</style>