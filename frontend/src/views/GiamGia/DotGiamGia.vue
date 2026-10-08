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
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#b88628" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
            </svg>
          </div>
          <div class="filter-title-wrap">
            <h3 class="filter-title">Bộ lọc</h3>
            <p class="filter-subtitle">Tìm kiếm và lọc đợt giảm giá</p>
          </div>
        </div>

        <!-- Lưới 5 ô lọc -->
        <div class="filter-inputs-grid">
          <!-- Tìm kiếm -->
          <div class="form-field">
            <label>Tìm kiếm</label>
            <div class="input-inner">
              <span class="prefix-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
              </span>
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
                  :min="filters.startDate || undefined"
                  @change="handleFilterChange"
              />
            </div>
          </div>

          <!-- Loại giảm giá -->
          <div class="form-field">
            <label>Loại giảm</label>
            <select v-model="filters.loaiGiam" @change="handleFilterChange">
              <option value="">Tất cả loại giảm</option>
              <option value="1">Giảm theo %</option>
              <option value="2">Giảm tiền mặt (₫)</option>
            </select>
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
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="1 4 1 10 7 10"></polyline>
              <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
            </svg>
            <span>Đặt lại bộ lọc</span>
          </button>
          <button class="btn btn-excel" @click="exportToExcel" :disabled="exporting">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            <span>{{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}</span>
          </button>
          <button class="btn btn-theme" @click="openCreateView">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span>Tạo đợt giảm giá</span>
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
              <th style="width: 55px; text-align: center;">STT</th>
              <th style="width: 130px;">Mã</th>
              <th style="width: 220px;">Tên đợt giảm giá</th>
              <th style="width: 170px;">Giá trị giảm</th>
              <th style="width: 155px;">Ngày bắt đầu</th>
              <th style="width: 155px;">Ngày kết thúc</th>
              <th style="width: 150px; text-align: center; white-space: nowrap;">Trạng thái</th>
              <th style="width: 110px; text-align: center; white-space: nowrap;">Hành động</th>
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
              <td class="text-dark">
                <div v-if="isCashDiscount(item)" class="discount-col-info">
                  <span class="font-bold text-dark">{{ formatMoney(item.giaTriGiam || item.phanTramGiam) }}</span>
                  <div class="discount-sub-info">
                    <span class="badge-mini badge-money">Tiền mặt</span>
                  </div>
                </div>
                <div v-else class="discount-col-info">
                  <span class="font-bold text-dark">{{ item.phanTramGiam != null ? item.phanTramGiam + '%' : '0%' }}</span>
                  <div class="discount-sub-info">
                    <span class="badge-mini badge-percent">Phần trăm</span>
                    <span v-if="item.giamToiDa" class="tag-max-discount">Tối đa: {{ formatMoney(item.giamToiDa) }}</span>
                  </div>
                </div>
              </td>
              <td class="text-date">{{ formatDate(item.ngayBatDau) }}</td>
              <td class="text-date">
                <span v-if="item.ngayKetThuc">{{ formatDate(item.ngayKetThuc) }}</span>
                <span v-else class="badge-mini badge-infinity">Vô thời hạn</span>
              </td>

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
                  <!-- Nút 1: Đổi trạng thái (Bật/Tắt) -->
                  <button
                      class="btn-action-square btn-action-power"
                      :class="item.trangThai === 1 ? 'power-active' : 'power-inactive'"
                      :title="item.trangThai === 1 ? 'Chuyển sang Ngưng Hoạt Động' : 'Chuyển sang Hoạt Động'"
                      @click="openToggleStatusModal(item)"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                      <line x1="12" y1="2" x2="12" y2="12"></line>
                    </svg>
                  </button>

                  <!-- Nút 2: Xem chi tiết & Sửa (Eye Icon thanh lịch) -->
                  <button
                      class="btn-action-square btn-action-edit"
                      title="Xem chi tiết & Chỉnh sửa"
                      @click="openEditView(item)"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#b88628" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
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
                {{ formData.trangThai === 1 ? 'Đang hoạt động' : 'Ngưng hoạt động' }}
              </span>
            </div>

            <div class="preview-info-row">
              <span class="preview-label">Mức giảm:</span>
              <span class="preview-discount-val">
                {{ formData.loaiGiamGia === 2 ? formatMoney(formData.giaTriGiam || 0) : (formData.phanTramGiam ? formData.phanTramGiam + '%' : '0%') }}
              </span>
            </div>

            <div class="preview-info-row">
              <span class="preview-label">Giảm tối đa:</span>
              <span :class="formData.loaiGiamGia === 2 ? 'text-hint' : (formData.giamToiDa ? 'font-bold text-danger' : 'text-hint')">
                {{ formData.loaiGiamGia === 2 ? 'Không áp dụng' : (formData.giamToiDa ? formatMoney(formData.giamToiDa) : 'Không giới hạn') }}
              </span>
            </div>

            <div class="preview-info-row">
              <span class="preview-label">Sản phẩm áp dụng:</span>
              <b class="text-dark">{{ selectedProductCount }} sản phẩm</b>
            </div>

            <div class="preview-info-col">
              <span class="preview-label">Thời gian áp dụng:</span>
              <span class="preview-date-range">
                {{ formatInputDateDisplay(formData.ngayBatDau) }} ➔ {{ formData.ngayKetThuc ? formatInputDateDisplay(formData.ngayKetThuc) : '---' }}
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
                  <span v-if="!isEditing" class="text-hint">(tự sinh nếu để trống)</span>
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

              <!-- Loại giảm giá -->
              <div class="form-field">
                <label>Loại giảm giá <span class="text-danger">*</span></label>
                <select v-model.number="formData.loaiGiamGia" @change="onLoaiGiamGiaChange">
                  <option :value="1">Giảm theo phần trăm (%)</option>
                  <option :value="2">Giảm theo số tiền (VNĐ)</option>
                </select>
              </div>

              <!-- Mức giảm -->
              <div class="form-field">
                <label>
                  {{ formData.loaiGiamGia === 2 ? 'Số tiền giảm (VNĐ)' : 'Mức giảm (%)' }}
                  <span class="text-danger">*</span>
                </label>
                <div class="input-inner">
                  <input
                      v-if="formData.loaiGiamGia === 1"
                      type="number"
                      min="1"
                      max="100"
                      step="0.5"
                      v-model.number="formData.phanTramGiam"
                      @input="handlePhanTramInput"
                      placeholder="VD: 20"
                  />
                  <input
                      v-else
                      type="number"
                      min="1000"
                      step="1000"
                      v-model.number="formData.giaTriGiam"
                      @input="handleGiaTriGiamInput"
                      placeholder="VD: 50000"
                  />
                  <span class="suffix-text font-bold">{{ formData.loaiGiamGia === 2 ? '₫' : '%' }}</span>
                </div>
              </div>

              <!-- Giảm tối đa: Chỉ áp dụng khi giảm % -->
              <div class="form-field">
                <label>
                  Giảm tối đa (VNĐ)
                  <span v-if="formData.loaiGiamGia === 2" class="text-hint">(không áp dụng khi giảm tiền)</span>
                </label>
                <div class="input-inner">
                  <input
                      type="number"
                      min="1000"
                      step="1000"
                      v-model.number="formData.giamToiDa"
                      @input="handleGiamToiDaInput"
                      :disabled="formData.loaiGiamGia === 2"
                      :placeholder="formData.loaiGiamGia === 2 ? 'Không áp dụng khi giảm bằng tiền' : 'Mức giảm tối đa (VD: 50.000)'"
                      :class="{ 'input-disabled': formData.loaiGiamGia === 2 }"
                  />
                  <span v-if="formData.loaiGiamGia === 1" class="suffix-text font-bold">₫</span>
                </div>
              </div>



              <!-- Trạng thái: Khi tạo mới không được chọn, mặc định luôn là Hoạt Động -->
              <div class="form-field">
                <label>Trạng thái</label>
                <select v-if="isEditing" v-model.number="formData.trangThai">
                  <option :value="1">Hoạt Động</option>
                  <option :value="0">Ngưng Hoạt Động</option>
                </select>
                <input
                    v-else
                    type="text"
                    value="Hoạt Động"
                    disabled
                    class="input-disabled"
                />
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

            <!-- Thanh tìm kiếm sản phẩm giao diện cao cấp & đẹp mắt -->
            <div class="product-search-wrapper">
              <div class="product-search-box">
                <div class="search-input-group">
                  <svg class="search-svg-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                  </svg>
                  <input
                      type="text"
                      v-model="productSearchKeyword"
                      class="product-search-input"
                      placeholder="Tìm kiếm sản phẩm theo tên áo, mã sản phẩm..."
                      @keyup.enter="handleSearchProductUI"
                  />
                  <button
                      v-if="productSearchKeyword"
                      type="button"
                      class="btn-clear-search"
                      @click="productSearchKeyword = ''; handleSearchProductUI()"
                      title="Xóa nhanh từ khóa"
                  >✕</button>
                </div>
                <button class="btn btn-search-product" @click="handleSearchProductUI">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
                  Tìm kiếm
                </button>
              </div>

              <div class="product-search-actions">
                <button
                    class="btn btn-toggle-all"
                    :class="{ 'btn-all-active': isAllProductsSelected }"
                    @click="toggleSelectAllProducts"
                >
                  <svg v-if="isAllProductsSelected" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                  <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/></svg>
                  {{ isAllProductsSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả' }}
                </button>
                <span class="product-badge-found">
                  Tìm thấy <b>{{ filteredProducts.length }}</b> SP
                </span>
              </div>
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
                <input
                  type="datetime-local"
                  v-model="formData.ngayBatDau"
                  @change="handleNgayBatDauChange"
                />
              </div>
              <div class="form-field">
                <label>
                  Ngày kết thúc
                  <span class="text-hint">(để trống = vô thời hạn)</span>
                </label>
                <div class="input-inner">
                  <input
                    type="datetime-local"
                    v-model="formData.ngayKetThuc"
                    :min="formData.ngayBatDau || undefined"
                    @change="handleNgayKetThucChange"
                  />
                  <button
                    v-if="formData.ngayKetThuc"
                    type="button"
                    class="btn-clear-date"
                    @click="formData.ngayKetThuc = ''"
                    title="Đặt lại thành Vô thời hạn"
                  >
                    ✕
                  </button>
                </div>
                <small v-if="!formData.ngayKetThuc" class="text-infinity-hint">
                  Đang áp dụng <b>Vô thời hạn</b>
                </small>
                <span
                  v-else-if="formData.ngayBatDau && new Date(formData.ngayKetThuc) <= new Date(formData.ngayBatDau)"
                  class="field-error-text"
                >
                  Ngày kết thúc phải sau ngày bắt đầu!
                </span>
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

          <!-- Bảng tóm tắt thông tin -->
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
              <span class="toggle-info-label">Loại giảm:</span>
              <span class="toggle-info-val font-medium">{{ isCashDiscount(selectedToggleItem) ? 'Giảm bằng tiền mặt (VNĐ)' : 'Giảm theo phần trăm (%)' }}</span>
            </div>
            <div class="toggle-info-row">
              <span class="toggle-info-label">Mức giảm:</span>
              <span class="toggle-info-val font-bold text-dark">
                {{ isCashDiscount(selectedToggleItem) ? formatMoney(selectedToggleItem.giaTriGiam || selectedToggleItem.phanTramGiam) : (selectedToggleItem.phanTramGiam != null ? selectedToggleItem.phanTramGiam + '%' : '0%') }}
              </span>
            </div>
            <div v-if="selectedToggleItem.giamToiDa" class="toggle-info-row">
              <span class="toggle-info-label">Giảm tối đa:</span>
              <span class="toggle-info-val font-bold text-danger">{{ formatMoney(selectedToggleItem.giamToiDa) }}</span>
            </div>
            <div class="toggle-info-row">
              <span class="toggle-info-label">Thời hạn:</span>
              <span class="toggle-info-val">{{ formatInputDateDisplay(selectedToggleItem.ngayBatDau) }} ➔ {{ selectedToggleItem.ngayKetThuc ? formatInputDateDisplay(selectedToggleItem.ngayKetThuc) : 'Vô thời hạn' }}</span>
            </div>
            <div class="toggle-info-row">
              <span class="toggle-info-label">Trạng thái hiện tại:</span>
              <span class="toggle-info-val font-bold" :class="selectedToggleItem.trangThai === 1 ? 'text-green' : 'text-danger'">
                {{ selectedToggleItem.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
              </span>
            </div>
            <div class="toggle-info-row">
              <span class="toggle-info-label">Trạng thái mới:</span>
              <span class="toggle-info-val font-bold" :class="selectedToggleItem.trangThai === 1 ? 'text-danger' : 'text-green'">
                {{ selectedToggleItem.trangThai === 1 ? 'Ngừng hoạt động' : 'Đang hoạt động' }}
              </span>
            </div>
          </div>

          <!-- Khung Lưu ý -->
          <div class="toggle-warning-alert">
            <div class="alert-content">
              <span class="alert-tag">Lưu ý:</span>
              <span v-if="selectedToggleItem.trangThai === 1">
                Khi ngừng hoạt động, khách hàng sẽ tạm thời không thể áp dụng đợt giảm giá này khi thanh toán.
              </span>
              <span v-else>
                Khi kích hoạt, đợt giảm giá này sẽ được áp dụng cho các sản phẩm theo đúng thời gian quy định.
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
  phanTramGiam: 15,
  giaTriGiam: 50000,
  giamToiDa: null,
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

// Helper nhận diện loại giảm giá tiền mặt
const isCashDiscount = (item) => {
  if (!item) return false
  if (item.loaiGiamGia === 2) return true
  if (item.giaTriGiam != null && item.giaTriGiam > 0 && item.loaiGiamGia !== 1) return true
  if (item.phanTramGiam != null && item.phanTramGiam > 100) return true
  return false
}

// Xử lý khi thay đổi Loại giảm giá trong form: Giảm % thì chọn được giảm tối đa, giảm tiền thì không
const onLoaiGiamGiaChange = () => {
  if (formData.value.loaiGiamGia === 2) {
    formData.value.giamToiDa = null
    if (!formData.value.giaTriGiam) {
      formData.value.giaTriGiam = 50000
    }
    showToast('Đã chuyển sang giảm bằng tiền (Ô Giảm tối đa đã bị khóa)!', 'info')
  } else {
    showToast('Đã chuyển sang giảm theo % (Bạn có thể nhập Giảm tối đa)!', 'info')
  }
}

// Chặn mức giảm phần trăm tối đa không vượt quá 100%
const handlePhanTramInput = () => {
  if (formData.value.phanTramGiam > 100) {
    formData.value.phanTramGiam = 100
    showToast('Mức giảm theo phần trăm tối đa là 100%!', 'info')
  } else if (formData.value.phanTramGiam < 0) {
    formData.value.phanTramGiam = 0
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

// Kiểm tra ngày kết thúc phải sau ngày bắt đầu khi thay đổi ngày
const handleNgayBatDauChange = () => {
  if (formData.value.ngayBatDau && formData.value.ngayKetThuc) {
    if (new Date(formData.value.ngayKetThuc) <= new Date(formData.value.ngayBatDau)) {
      showToast('Ngày kết thúc phải sau ngày bắt đầu!', 'error')
      formData.value.ngayKetThuc = ''
    }
  }
}

const handleNgayKetThucChange = () => {
  if (formData.value.ngayBatDau && formData.value.ngayKetThuc) {
    if (new Date(formData.value.ngayKetThuc) <= new Date(formData.value.ngayBatDau)) {
      showToast('Ngày kết thúc phải sau ngày bắt đầu!', 'error')
      formData.value.ngayKetThuc = ''
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

    campaigns.value = list
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
const openCreateView = () => {
  isEditing.value = false
  formData.value = {
    id: null,
    maDotGiamGia: '',
    tenDotGiamGia: '',
    loaiGiamGia: 1, // 1: Giảm theo %, 2: Giảm theo tiền
    phanTramGiam: 15,
    giaTriGiam: 50000,
    giamToiDa: null, // Bị khóa khi loaiGiamGia === 1
    ngayBatDau: toInputDateTime(new Date()),
    ngayKetThuc: '', // Để trống = vô thời hạn
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
  const isCash = isCashDiscount(item)
  const meta = getSavedCampaignMeta(item.id, item.maDotGiamGia)
  const effectiveGiamToiDa = (item.giamToiDa != null) ? item.giamToiDa : (meta?.giamToiDa != null ? meta.giamToiDa : null)

  formData.value = {
    id: item.id,
    maDotGiamGia: item.maDotGiamGia || '',
    tenDotGiamGia: item.tenDotGiamGia || '',
    loaiGiamGia: isCash ? 2 : 1,
    phanTramGiam: !isCash ? (item.phanTramGiam || 15) : 15,
    giaTriGiam: isCash ? (item.giaTriGiam || item.phanTramGiam || 50000) : 50000,
    giamToiDa: !isCash ? effectiveGiamToiDa : null,
    ngayBatDau: toInputDateTime(item.ngayBatDau),
    ngayKetThuc: item.ngayKetThuc ? toInputDateTime(item.ngayKetThuc) : '',
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

  // Validate theo từng loại giảm: Giảm % thì chọn được giảm tối đa, giảm tiền thì không
  if (formData.value.loaiGiamGia === 1) {
    if (!formData.value.phanTramGiam || formData.value.phanTramGiam <= 0) {
      showToast('Vui lòng nhập mức giảm lớn hơn 0%!', 'error')
      return
    }
    if (formData.value.phanTramGiam > 100) {
      formData.value.phanTramGiam = 100
      showToast('Mức giảm theo phần trăm không được vượt quá 100%!', 'error')
      return
    }
    if (formData.value.giamToiDa !== null && formData.value.giamToiDa !== '' && formData.value.giamToiDa !== undefined) {
      if (Number(formData.value.giamToiDa) <= 0) {
        showToast('Số tiền giảm tối đa phải lớn hơn 0đ (hoặc để trống nếu không giới hạn)!', 'error')
        return
      }
    }
  } else {
    formData.value.giamToiDa = null
    if (!formData.value.giaTriGiam || formData.value.giaTriGiam <= 0) {
      showToast('Vui lòng nhập số tiền giảm lớn hơn 0đ!', 'error')
      return
    }
  }

  // Kiểm tra yêu cầu nhập ngày bắt đầu
  if (!formData.value.ngayBatDau || formData.value.ngayBatDau.trim() === '') {
    showToast('Vui lòng chọn ngày bắt đầu!', 'error')
    return
  }
  // Ngày kết thúc: để trống là vô thời hạn. Nếu có chọn thì phải sau ngày bắt đầu
  if (formData.value.ngayKetThuc && formData.value.ngayKetThuc.trim() !== '') {
    if (new Date(formData.value.ngayKetThuc) <= new Date(formData.value.ngayBatDau)) {
      showToast('Ngày kết thúc phải sau ngày bắt đầu!', 'error')
      return
    }
  }

  showConfirmModal.value = true
}

// Xác nhận gọi API tạo mới hoặc cập nhật
const doSubmitAPI = async () => {
  try {
    submitting.value = true
    const isPercent = formData.value.loaiGiamGia === 1
    const finalVal = isPercent ? formData.value.phanTramGiam : formData.value.giaTriGiam
    const payload = {
      maDotGiamGia: formData.value.maDotGiamGia?.trim() || null,
      tenDotGiamGia: formData.value.tenDotGiamGia.trim(),
      phanTramGiam: finalVal,
      loaiGiamGia: formData.value.loaiGiamGia,
      giaTriGiam: isPercent ? null : formData.value.giaTriGiam,
      giamToiDa: isPercent ? (formData.value.giamToiDa || null) : null,
      ngayBatDau: new Date(formData.value.ngayBatDau).toISOString(),
      ngayKetThuc: formData.value.ngayKetThuc ? new Date(formData.value.ngayKetThuc).toISOString() : null,
      trangThai: formData.value.trangThai
    }

    let res = null
    if (isEditing.value) {
      res = await api.put(`/api/dot-giam-gia/${formData.value.id}`, payload)
      showToast('Cập nhật đợt giảm giá thành công!', 'success')
    } else {
      res = await api.post('/api/dot-giam-gia', payload)
      showToast('Tạo mới đợt giảm giá thành công!', 'success')
    }

    // Lưu metadata mở rộng (giảm tối đa) vào local cache
    const savedId = isEditing.value ? formData.value.id : res?.data?.id
    saveCampaignMeta(savedId, formData.value.maDotGiamGia, {
      giamToiDa: formData.value.giamToiDa
    })

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
  padding: 2rem 2.75rem 4rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Toast Notification góc phải trên màn hình */
.toast-msg {
  position: fixed;
  top: 24px;
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
  transform: translateX(45px);
}
.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(45px);
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

/* Content Card: Tăng padding trong và margin ngoài để tạo khoảng trắng đẹp mắt */
.content-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
  padding: 1.75rem 2.25rem;
  margin-bottom: 1.75rem;
}

.card-header-filter {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.6rem;
}
.filter-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 9px;
  background-color: #fef3c7;
  display: grid;
  place-items: center;
}
.filter-icon { font-size: 1.25rem; color: #b45309; }
.filter-title-wrap { display: flex; flex-direction: column; }
.filter-title { font-size: 1.18rem; font-weight: 800; margin: 0; color: #111827; letter-spacing: -0.015em; }
.filter-subtitle { font-size: 0.86rem; color: #6b7280; margin: 4px 0 0 0; }

.filter-inputs-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 1.35rem;
  margin-bottom: 1.6rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}
.form-field label {
  font-size: 0.9rem;
  font-weight: 750;
  color: #1f2937;
  letter-spacing: 0.01em;
}
.input-inner {
  position: relative;
  display: flex;
  align-items: center;
}
.prefix-icon {
  position: absolute;
  left: 1rem;
  color: #9ca3af;
  pointer-events: none;
  display: flex;
  align-items: center;
}

.form-field input, .form-field select {
  width: 100%;
  height: 2.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 1.1rem;
  font-size: 0.92rem;
  color: #1f2937;
  background-color: #ffffff;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}
.form-field .input-inner input[type="text"] {
  padding-left: 2.85rem;
}
.form-field input:focus, .form-field select:focus {
  border-color: #3b82f6;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.input-disabled { background: #f3f4f6 !important; cursor: not-allowed; color: #9ca3af; }

.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.9rem;
  border-top: 1px dashed #e5e7eb;
  padding-top: 1.4rem;
  margin-top: 0.5rem;
}

.btn {
  height: 2.65rem;
  padding: 0 1.5rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.btn-sm { height: 2.2rem; padding: 0 0.9rem; font-size: 0.84rem; }
.btn-reset { border: 1px solid #d1d5db; background-color: #ffffff; color: #4b5563; }
.btn-reset:hover { background-color: #f9fafb; border-color: #9ca3af; }
.btn-excel { background-color: #15803d; color: #ffffff; }
.btn-excel:hover:not(:disabled) { background-color: #166534; }
.btn-excel:disabled { opacity: 0.65; cursor: not-allowed; }
.btn-theme { background-color: #304b60; color: #ffffff; }
.btn-theme:hover { background-color: #1e3342; }
.btn-outline { background: #ffffff; border-color: #d1d5db; color: #4b5563; }
.btn-outline:hover { background: #f9fafb; border-color: #9ca3af; }

/* Table chuẩn chỉnh khoảng cách lề và chữ cách xa viền */
.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.4rem;
}
.table-title {
  font-size: 1.18rem;
  font-weight: 800;
  margin: 0;
  color: #111827;
  letter-spacing: -0.015em;
}
.record-count {
  font-size: 0.9rem;
  color: #6b7280;
}
.record-count b {
  color: #111827;
}
.table-responsive {
  overflow-x: auto;
}
.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}
.custom-table th {
  background-color: #f9fafb;
  color: #374151;
  font-weight: 750;
  padding: 0.95rem 1.05rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
  letter-spacing: 0.01em;
}
.custom-table td {
  padding: 1rem 1.05rem;
  border-bottom: 1px solid #f3f4f6;
  color: #374151;
  vertical-align: middle;
  line-height: 1.5;
}
.custom-table th:first-child,
.custom-table td:first-child {
  padding-left: 1.25rem;
}
.custom-table th:last-child,
.custom-table td:last-child {
  padding-right: 1.25rem;
}
.custom-table tr:hover td {
  background-color: #f9fafb;
}

.empty-cell { text-align: center; padding: 2.75rem !important; color: #6b7280; font-style: italic; }
.error-cell { text-align: center; padding: 2.75rem !important; color: #dc2626; font-weight: 500; }
.inline-spinner {
  display: inline-block;
  width: 15px;
  height: 15px;
  border: 2px solid #d1d5db;
  border-top-color: #2563eb;
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
.text-blue { color: #1d4ed8; font-family: monospace, sans-serif; letter-spacing: 0.5px; }
.text-title { color: #111827; }
.text-dark { color: #111827; }
.text-date { color: #4b5563; font-size: 0.88rem; }
.text-muted { color: #9ca3af; }
.text-danger { color: #dc2626; }
.text-green { color: #16a34a; }
.text-hint { font-size: 0.8rem; font-weight: normal; color: #6b7280; }
.tag-lock { font-size: 0.75rem; background: #f3f4f6; padding: 0.15rem 0.4rem; border-radius: 4px; color: #4b5563; margin-left: 0.4rem; }

/* Badge Status */
.badge-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  white-space: nowrap;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.35rem 0.85rem;
  border-radius: 9999px;
  box-sizing: border-box;
}
.status-active { background-color: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
.status-inactive { background-color: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }

/* ========================================================
   CỘT HÀNH ĐỘNG: 2 NÚT VUÔNG BO GÓC MỀM ĐƠN GIẢN
======================================================== */
.action-buttons {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
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
  transition: all 0.18s ease;
  box-sizing: border-box;
  flex-shrink: 0;
}

/* Nút 1: Power button xanh lá khi đang hoạt động */
.btn-action-power.power-active {
  background-color: #f0fdf4;
  border-color: #bbf7d0;
  color: #16a34a;
}
.btn-action-power.power-active:hover {
  background-color: #dcfce7;
  color: #15803d;
  transform: translateY(-1px);
}

/* Nút 1: Power button đỏ khi đang ngưng hoạt động */
.btn-action-power.power-inactive {
  background-color: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}
.btn-action-power.power-inactive:hover {
  background-color: #fee2e2;
  color: #b91c1c;
  transform: translateY(-1px);
}

/* Nút 2: Edit button thanh lịch */
.btn-action-edit {
  background-color: #f0f4f8;
  border-color: #dbe4ee;
  color: #304b60;
}
.btn-action-edit:hover {
  background-color: #e2ecf5;
  color: #1e3342;
  transform: translateY(-1px);
}

/* Pagination */
.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1.2rem;
  margin-top: 1.5rem;
  padding-top: 1.35rem;
  border-top: 1px dashed var(--line, #e9e5db);
}
.page-size-selector { display: flex; align-items: center; gap: 0.75rem; }
.page-size-label { font-size: 0.9rem; color: #6f7c82; font-weight: 600; }
.page-size-selector select {
  height: 2.35rem;
  padding: 0 0.85rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  background-color: #fcfbf8;
  color: var(--text, #3d4a50);
  font-size: 0.9rem;
  font-weight: 600;
  outline: none;
  cursor: pointer;
}
.pagination-controls { display: flex; align-items: center; gap: 0.5rem; }
.pg-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 0.6rem;
  border-radius: 6px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #4b585e;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.pg-btn:hover:not(:disabled) { border-color: var(--blue, #496883); color: var(--blue, #496883); background-color: #f4f7f9; }
.pg-btn.active { background-color: var(--blue, #496883); color: #ffffff; border-color: var(--blue, #496883); }
.pg-btn:disabled { opacity: 0.35; cursor: not-allowed; }

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
  display: grid;
  grid-template-columns: 310px 1fr;
  gap: 1.75rem;
  align-items: start;
}
.preview-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem 1.6rem;
}
.preview-icon-box { width: 62px; height: 62px; border-radius: 14px; background: #fbf5e8; display: grid; place-items: center; margin-bottom: 1rem; }
.preview-badge-code { font-family: monospace; font-size: 0.84rem; font-weight: 700; color: #304b60; background: #eef3f7; padding: 0.25rem 0.7rem; border-radius: 5px; margin-bottom: 0.75rem; }
.preview-title { margin: 0; font-size: 1.15rem; font-weight: 800; color: #111827; line-height: 1.35; letter-spacing: -0.01em; }
.preview-divider { width: 100%; height: 1px; background: #efeae0; margin: 1.35rem 0; }
.preview-info-row { width: 100%; display: flex; justify-content: space-between; align-items: center; font-size: 0.9rem; margin-bottom: 0.9rem; }
.preview-info-col { width: 100%; display: flex; flex-direction: column; align-items: flex-start; gap: 0.4rem; font-size: 0.88rem; margin-top: 0.5rem; padding-top: 0.9rem; border-top: 1px dashed #efeae0; }
.preview-label { color: #8c9597; font-size: 0.86rem; }
.preview-discount-val { font-size: 1.25rem; font-weight: 800; color: #c62828; }
.preview-date-range { color: #496883; font-weight: 600; font-size: 0.85rem; text-align: left; }

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
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #374151;
  font-weight: 600;
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-cancel-confirm:hover { background: #f9fafb; border-color: #9ca3af; }
.btn-accept-confirm {
  background: #16a34a;
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
.btn-accept-confirm:hover { background: #15803d; }

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
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #374151;
  font-weight: 600;
  height: 2.5rem;
  padding: 0 1.4rem;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-toggle-cancel:hover {
  background: #f9fafb;
  border-color: #9ca3af;
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