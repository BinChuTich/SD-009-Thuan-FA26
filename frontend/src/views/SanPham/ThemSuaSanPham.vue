<template>
  <div class="product-form-container">
    <!-- 1. Thanh tiêu đề & Điều hướng -->
    <div class="page-top-header">
      <div class="header-left-col">
        <div class="page-breadcrumb">
          <router-link to="/san-pham" class="crumb-parent">Quản lý sản phẩm</router-link>
          <span class="crumb-separator">/</span>
          <span class="crumb-current">{{ isEdit ? 'Sửa sản phẩm' : 'Thêm sản phẩm' }}</span>
        </div>
      </div>

      <div class="header-right-col">
        <button type="button" class="btn btn-back-top" @click="handleBack" title="Quay lại danh sách sản phẩm">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          <span>Quay lại</span>
        </button>
      </div>
    </div>

    <!-- Thông báo Toast -->
    <transition name="fade">
      <div v-if="toast.show" :class="['toast-notification', toast.type]">
        {{ toast.message }}
      </div>
    </transition>

    <form @submit.prevent="openSaveConfirm" class="product-form-body">
      <!-- 2. Khối: Thông tin sản phẩm -->
      <div class="form-card">
        <div class="card-box-header">
          <div class="card-title-group">
            <span class="card-icon-box">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
                <line x1="12" y1="22.08" x2="12" y2="12"></line>
              </svg>
            </span>
            <h3 class="card-title-text">Thông tin sản phẩm</h3>
          </div>
        </div>

        <div class="form-grid-layout">
          <!-- Hàng 1: Mã SP (chỉ hiện khi Sửa) | Tên SP -->
          <div class="form-item" v-if="isEdit">
            <label class="item-label">
              Mã sản phẩm <span class="text-danger">*</span>
            </label>
            <input
                type="text"
                v-model="form.maSanPham"
                :disabled="isEdit"
                :class="['form-input', { 'input-disabled': isEdit }]"
                placeholder="Mã sản phẩm"
            />
            <span class="help-subtext">Mã được giữ nguyên khi sửa</span>
          </div>

          <div class="form-item" :class="{ 'full-width-col': !isEdit }">
            <label class="item-label">Tên sản phẩm <span class="text-danger">*</span></label>
            <input
                type="text"
                v-model="form.tenSanPham"
                class="form-input"
                placeholder="Nhập tên sản phẩm..."
                required
            />
          </div>

          <!-- Hàng 2: Danh mục | Thương hiệu -->
          <div class="form-item">
            <label class="item-label">Danh mục <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idDanhMuc"
                :options="attributes.danhMuc"
                label-key="tenDanhMuc"
                placeholder="-- Chọn danh mục --"
                search-placeholder="Tìm kiếm danh mục..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('danh-muc')"
                title="Thêm nhanh Danh mục"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="form-item">
            <label class="item-label">Thương hiệu <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idThuongHieu"
                :options="attributes.thuongHieu"
                label-key="tenThuongHieu"
                placeholder="-- Chọn thương hiệu --"
                search-placeholder="Tìm kiếm thương hiệu..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('thuong-hieu')"
                title="Thêm nhanh Thương hiệu"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- Hàng 3: Chất liệu | Cổ áo -->
          <div class="form-item">
            <label class="item-label">Chất liệu <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idChatLieu"
                :options="attributes.chatLieu"
                label-key="tenChatLieu"
                placeholder="-- Chọn chất liệu --"
                search-placeholder="Tìm kiếm chất liệu..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('chat-lieu')"
                title="Thêm nhanh Chất liệu"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="form-item">
            <label class="item-label">Cổ áo <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idCoAo"
                :options="attributes.coAo"
                label-key="tenCoAo"
                placeholder="-- Chọn cổ áo --"
                search-placeholder="Tìm kiếm cổ áo..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('co-ao')"
                title="Thêm nhanh Cổ áo"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- Hàng 4: Tay áo | Xuất xứ -->
          <div class="form-item">
            <label class="item-label">Tay áo <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idTayAo"
                :options="attributes.tayAo"
                label-key="tenTayAo"
                placeholder="-- Chọn tay áo --"
                search-placeholder="Tìm kiếm tay áo..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('tay-ao')"
                title="Thêm nhanh Tay áo"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="form-item">
            <label class="item-label">Xuất xứ <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idXuatXu"
                :options="attributes.xuatXu"
                label-key="tenXuatXu"
                placeholder="-- Chọn xuất xứ --"
                search-placeholder="Tìm kiếm xuất xứ..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('xuat-xu')"
                title="Thêm nhanh Xuất xứ"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- Hàng 5: Họa tiết | Trạng thái -->
          <div class="form-item">
            <label class="item-label">Họa tiết <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <Select2
                v-model="form.idHoaTiet"
                :options="attributes.hoaTiet"
                label-key="tenHoaTiet"
                placeholder="-- Chọn họa tiết --"
                search-placeholder="Tìm kiếm họa tiết..."
              />
              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('hoa-tiet')"
                title="Thêm nhanh Họa tiết"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>

          <div class="form-item" v-if="isEdit">
            <label class="item-label">Trạng thái <span class="text-danger">*</span></label>
            <Select2
              v-model="form.trangThai"
              :options="statusOptions"
              placeholder="-- Chọn trạng thái --"
              :allow-clear="false"
              :searchable="false"
            />
          </div>

          <!-- Hàng 6: Mô tả chi tiết (chiếm 2 cột) -->
          <div class="form-item full-width-col">
            <label class="item-label">Mô tả chi tiết</label>
            <textarea
                v-model="form.moTa"
                rows="4"
                class="form-textarea"
                placeholder="Nhập mô tả sản phẩm..."
            ></textarea>
          </div>
        </div>

        <!-- Các nút bấm Lưu / Hủy trong chế độ Sửa sản phẩm (isEdit) -->
        <div v-if="isEdit" class="card-bottom-actions">
          <button
              type="submit"
              class="btn btn-save-action"
              :disabled="submitting"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <span>{{ submitting ? 'Đang lưu...' : 'Lưu thay đổi' }}</span>
          </button>
          <button type="button" class="btn btn-cancel-action" @click="handleBack">
            Hủy
          </button>
        </div>
      </div>



      <!-- 3. Khối: Chọn Màu sắc & Kích cỡ để sinh biến thể tự động (Chỉ hiện khi Thêm mới) -->
      <div v-if="!isEdit" class="form-card mt-3">
        <!-- Hàng 1: Màu sắc -->
        <div class="attribute-picker-row">
          <div class="picker-label-col">
            <label class="item-label">Màu sắc <span class="text-danger">*</span></label>
          </div>
          <div class="picker-input-col">
            <div class="color-picker-input-group">
              <div class="multiselect-dropdown-wrap">
                <div class="dropdown-trigger" @click.stop="toggleColorDropdown">
                  <span class="trigger-text" v-if="selectedColors.length === 0">Chọn màu...</span>
                  <span class="trigger-text" v-else>{{ selectedColorNames }}</span>
                  <span class="trigger-arrow">⌄</span>
                </div>

                <!-- Menu chọn màu sắc -->
                <div v-if="showColorDropdown" class="multiselect-popover" @click.stop>
                  <div class="popover-search">
                    <input
                        type="text"
                        v-model="colorSearch"
                        placeholder="Nhập màu sắc..."
                        class="popover-input"
                    />
                    <button
                        type="button"
                        class="btn-clear-all"
                        v-if="selectedColors.length > 0"
                        @click="clearAllColors"
                    >
                      Bỏ chọn tất cả ✕
                    </button>
                  </div>
                  <div class="popover-list">
                    <div
                        v-for="color in filteredColors"
                        :key="color.id"
                        class="popover-item"
                        :class="{ selected: isColorSelected(color.id) }"
                        @click="toggleColor(color)"
                    >
                      <span
                          class="color-dot"
                          :style="{ backgroundColor: color.maHex || '#888' }"
                      ></span>
                      <span class="item-name">{{ color.tenMauSac }}</span>
                      <span v-if="isColorSelected(color.id)" class="check-icon">✓</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Nút Bảng màu trực quan & Native Color Picker ngay bên cạnh dropdown -->
              <div class="color-palette-action-wrapper" @click.stop>
                <div class="palette-btn-group">
                  <!-- Ô chọn màu nhanh (Native color picker quang phổ) -->
                  <label class="native-color-picker-box" title="Click để chọn màu từ bảng màu quang phổ">
                    <input
                        type="color"
                        v-model="quickPickHex"
                        @change="handleQuickPickColor"
                        class="native-color-input"
                    />
                    <span class="color-preview-disc" :style="{ backgroundColor: quickPickHex }"></span>
                  </label>

                  <!-- Nút mở Popover Bảng màu -->
                  <button
                      type="button"
                      class="btn btn-palette-open"
                      @click.stop="toggleColorPalette"
                      :class="{ active: showColorPalette }"
                      title="Mở bảng màu sắc trực quan"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="13.5" cy="6.5" r=".5" fill="currentColor"/>
                      <circle cx="17.5" cy="10.5" r=".5" fill="currentColor"/>
                      <circle cx="8.5" cy="7.5" r=".5" fill="currentColor"/>
                      <circle cx="6.5" cy="12.5" r=".5" fill="currentColor"/>
                      <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2z"/>
                    </svg>
                    <span>Bảng màu</span>
                  </button>
                </div>

                <!-- Popover Bảng màu -->
                <div v-if="showColorPalette" class="color-palette-popover" @click.stop>
                  <div class="palette-popover-header">
                    <div class="palette-header-title">
                      <span class="palette-title-icon">🎨</span>
                      <strong>Bảng màu sắc</strong>
                    </div>
                    <span class="palette-selected-count">Đã chọn: {{ selectedColors.length }}</span>
                  </div>

                  <!-- Lưới các ô màu có sẵn trong hệ thống -->
                  <div class="palette-swatches-section">
                    <div class="palette-section-label">Màu sắc có sẵn (Click để chọn):</div>
                    <div class="palette-swatches-grid" v-if="attributes.mauSac && attributes.mauSac.length > 0">
                      <button
                          type="button"
                          v-for="color in attributes.mauSac"
                          :key="color.id"
                          class="palette-swatch-btn"
                          :class="{ selected: isColorSelected(color.id) }"
                          :style="{ backgroundColor: color.maHex || '#888' }"
                          @click="toggleColor(color)"
                          :title="`${color.tenMauSac} (${color.maHex || ''})`"
                      >
                        <span v-if="isColorSelected(color.id)" class="swatch-check">✓</span>
                      </button>
                    </div>
                    <div v-else class="text-muted small py-1">Chưa có màu sắc trong hệ thống</div>
                  </div>

                  <!-- Thêm màu tùy chỉnh từ bảng màu quang phổ -->
                  <div class="palette-custom-section">
                    <div class="palette-section-label">Chọn màu mới từ Bảng màu:</div>
                    <div class="custom-color-row">
                      <input
                          type="color"
                          v-model="newColorForm.maHex"
                          class="custom-color-input"
                          title="Chọn mã màu"
                      />
                      <input
                          type="text"
                          v-model="newColorForm.maHex"
                          class="custom-hex-text"
                          placeholder="#000000"
                          maxlength="7"
                      />
                      <input
                          type="text"
                          v-model="newColorForm.ten"
                          class="custom-name-text"
                          placeholder="Tên màu..."
                          maxlength="50"
                          @keyup.enter="handleAddCustomColor"
                      />
                      <button
                          type="button"
                          class="btn btn-add-custom-color"
                          @click="handleAddCustomColor"
                          :disabled="newColorForm.loading"
                      >
                        {{ newColorForm.loading ? '...' : '+ Thêm' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Tags màu sắc đã chọn -->
            <div class="selected-tags-box" v-if="selectedColors.length > 0">
              <span
                  v-for="color in selectedColors"
                  :key="color.id"
                  class="attr-chip"
              >
                <span class="color-dot-sm" :style="{ backgroundColor: color.maHex || '#888' }"></span>
                {{ color.tenMauSac }}
                <span class="chip-remove" @click.stop="removeColor(color.id)">×</span>
              </span>
            </div>
          </div>
        </div>

        <!-- Hàng 2: Kích cỡ -->
        <div class="attribute-picker-row mt-3">
          <div class="picker-label-col">
            <label class="item-label">Kích cỡ <span class="text-danger">*</span></label>
          </div>
          <div class="picker-input-col">
            <div class="select-with-btn">
              <div class="multiselect-dropdown-wrap" style="flex: 1; min-width: 0;">
                <div class="dropdown-trigger" @click.stop="toggleSizeDropdown">
                  <span class="trigger-text" v-if="selectedSizes.length === 0">Chọn size...</span>
                  <span class="trigger-text" v-else>{{ selectedSizeNames }}</span>
                  <span class="trigger-arrow">⌄</span>
                </div>

                <!-- Menu chọn kích cỡ -->
                <div v-if="showSizeDropdown" class="multiselect-popover" @click.stop>
                  <div class="popover-search">
                    <input
                        type="text"
                        v-model="sizeSearch"
                        placeholder="Nhập kích cỡ..."
                        class="popover-input"
                    />
                    <button
                        type="button"
                        class="btn-clear-all"
                        v-if="selectedSizes.length > 0"
                        @click="clearAllSizes"
                    >
                      Bỏ chọn tất cả ✕
                    </button>
                  </div>
                  <div class="popover-list">
                    <div
                        v-for="size in filteredSizes"
                        :key="size.id"
                        class="popover-item"
                        :class="{ selected: isSizeSelected(size.id) }"
                        @click="toggleSize(size)"
                    >
                      <span class="item-name">{{ size.tenKichCo }}</span>
                      <span v-if="isSizeSelected(size.id)" class="check-icon">✓</span>
                    </div>
                  </div>
                </div>
              </div>

              <button
                type="button"
                class="btn-add-attr-inline"
                @click="openQuickAddModal('kich-co')"
                title="Thêm nhanh Kích cỡ"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>

            <!-- Tags kích cỡ đã chọn -->
            <div class="selected-tags-box" v-if="selectedSizes.length > 0">
              <span
                  v-for="size in selectedSizes"
                  :key="size.id"
                  class="attr-chip"
              >
                {{ size.tenKichCo }}
                <span class="chip-remove" @click.stop="removeSize(size.id)">×</span>
              </span>
            </div>
          </div>
        </div>

        <!-- Nút Tạo biến thể tự động (Màu đỏ mận gradient) -->
        <div class="generate-btn-row">
          <button
              type="button"
              class="btn btn-generate"
              @click="generateVariants"
          >
            Tạo biến thể tự động
          </button>
        </div>
      </div>

      <!-- 4. Khối 3: Bảng danh sách biến thể theo màu sắc (Chỉ hiện khi Thêm mới) -->
      <div v-if="!isEdit && groupedVariants.length > 0" class="form-card mt-3">
        <!-- Thanh áp dụng nhanh hàng loạt -->
        <div class="bulk-apply-bar">
          <label class="bulk-select-all">
            <input
                type="checkbox"
                v-model="selectAllVariants"
                @change="toggleSelectAllVariants"
            />
            <span>Chọn tất cả biến thể</span>
          </label>

          <div class="bulk-inputs-group">
            <div class="bulk-field">
              <label>Số lượng mặc định</label>
              <input
                  type="number"
                  min="0"
                  v-model.number="bulkQty"
                  class="form-input bulk-input"
                  placeholder="0"
              />
            </div>

            <div class="bulk-field">
              <label>Giá bán mặc định <span class="text-danger">*</span></label>
              <input
                  type="text"
                  inputmode="numeric"
                  :value="formatNumberWithDots(bulkPrice)"
                  class="form-input bulk-input"
                  placeholder="0"
                  @focus="$event.target.select()"
                  @input="onBulkPriceInput"
                  @keydown="blockInvalidIntegerKeys"
                  @paste="handleIntegerPaste"
                  @blur="onBlurPrice('bulk')"
              />
            </div>

            <button type="button" class="btn btn-bulk-apply" @click="applyBulkValues">
              Áp dụng
            </button>
          </div>
        </div>

        <!-- File input ẩn dùng chung cho upload ảnh từng biến thể -->
        <input
          type="file"
          ref="variantFileInput"
          accept="image/png, image/jpeg, image/webp"
          style="display: none;"
          @change="onVariantImageSelected"
        />

        <!-- Nhóm các biến thể theo từng màu sắc -->
        <div
            v-for="group in groupedVariants"
            :key="group.color.id"
            class="color-variant-group-card"
        >
          <div class="group-header">
            <div class="group-header-left">
              <span class="color-dot-md" :style="{ backgroundColor: group.color.maHex || '#888' }"></span>
              <strong class="color-group-title">{{ group.color.tenMauSac }}</strong>
            </div>
            <div class="group-header-right">
              <button
                type="button"
                class="btn-apply-color-img"
                @click="triggerUpload(group.color.id)"
                :title="`Tải ảnh áp dụng cho toàn bộ kích cỡ màu ${group.color.tenMauSac}`"
              >
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span>Ảnh màu {{ group.color.tenMauSac }}</span>
              </button>
              <span class="size-summary">{{ group.sizesSummary }}</span>
            </div>
          </div>

          <div class="table-responsive">
            <table class="variant-data-table">
              <thead>
              <tr>
                <th style="width: 40px; text-align: center;">
                  <input
                      type="checkbox"
                      :checked="isGroupAllSelected(group)"
                      @change="toggleGroupSelection(group)"
                  />
                </th>
                <th style="width: 50px; text-align: center;">STT</th>
                <th style="width: 75px; text-align: center;">Ảnh</th>
                <th style="width: 120px;">Kích cỡ</th>
                <th style="width: 180px;">Số lượng</th>
                <th>Giá bán (VNĐ)</th>
                <th style="width: 70px; text-align: center;">Xóa</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(vItem, vIndex) in group.items" :key="vItem.key">
                <td style="text-align: center;">
                  <input type="checkbox" v-model="vItem.selected" />
                </td>
                <td style="text-align: center;" class="cell-stt">{{ vIndex + 1 }}</td>
                <td style="text-align: center;" class="cell-variant-img">
                  <div
                    class="variant-img-uploader"
                    @click="triggerVariantImageUpload(vItem)"
                    :title="vItem.anhDaiDien ? 'Nhấn để đổi ảnh riêng của biến thể' : 'Nhấn để chọn ảnh riêng cho biến thể này'"
                  >
                    <img
                      v-if="vItem.anhDaiDien || colorImages[vItem.idMauSac]"
                      :src="formatImageUrl(vItem.anhDaiDien || colorImages[vItem.idMauSac])"
                      class="variant-table-img"
                      alt="Ảnh biến thể"
                    />
                    <div v-else class="variant-img-empty">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                      </svg>
                      <span>+ Ảnh</span>
                    </div>
                    <button
                      v-if="vItem.anhDaiDien"
                      type="button"
                      class="btn-remove-var-img"
                      @click.stop="removeVariantImage(vItem)"
                      title="Gỡ ảnh riêng của biến thể"
                    >
                      &times;
                    </button>
                  </div>
                </td>
                <td class="cell-size">
                  <strong>{{ vItem.tenKichCo }}</strong>
                </td>
                <td>
                  <input
                      type="number"
                      min="0"
                      v-model.number="vItem.soLuong"
                      class="form-input cell-input"
                      placeholder="0"
                      required
                  />
                </td>
                <td>
                  <input
                      type="text"
                      inputmode="numeric"
                      :value="formatNumberWithDots(vItem.giaBan)"
                      class="form-input cell-input"
                      placeholder="0"
                      required
                      @focus="$event.target.select()"
                      @input="onVariantPriceInput($event, vItem)"
                      @keydown="blockInvalidIntegerKeys"
                      @paste="handleIntegerPaste"
                      @blur="onBlurPrice(vItem)"
                  />
                </td>
                <td style="text-align: center;">
                  <button
                      type="button"
                      class="btn-delete-row"
                      @click="openDeleteVariantConfirm(vItem)"
                      title="Xóa biến thể này"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="currentColor">
                      <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                    </svg>
                  </button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 5. Khối 4: Ảnh sản phẩm chi tiết theo màu sắc (Chỉ hiện khi Thêm mới) -->
      <div v-if="!isEdit && activeColorsForImages.length > 0" class="form-card mt-3 color-images-container">
        <div class="images-header-wrap">
          <h4 class="card-section-title">Ảnh sản phẩm chi tiết</h4>
          <p class="card-section-subtitle">
            Thêm ảnh cho từng màu sắc (biến thể đại diện) để tự động đồng bộ cho toàn bộ kích cỡ.
          </p>
        </div>

        <div class="color-images-grid">
          <div
              v-for="color in activeColorsForImages"
              :key="color.id"
              class="color-image-card"
          >
            <div class="image-card-header">
              <span class="color-img-title">Ảnh sản phẩm màu {{ color.tenMauSac.toLowerCase() }}</span>
              <div class="image-card-actions">
                <button
                    type="button"
                    class="btn-reset-img"
                    @click="resetColorImage(color.id)"
                    title="Đặt lại ảnh"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="23 4 23 10 17 10"></polyline>
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                  </svg>
                </button>
                <button
                    type="button"
                    class="btn-add-img"
                    @click="triggerUpload(color.id)"
                >
                  + Thêm ảnh
                </button>
              </div>
            </div>

            <!-- Vùng hiển thị ảnh -->
            <div class="image-dropzone" @click="triggerUpload(color.id)">
              <input
                  type="file"
                  :ref="el => fileInputs[color.id] = el"
                  accept="image/*"
                  style="display: none;"
                  @change="onFileChange($event, color.id)"
              />

              <div v-if="colorImages[color.id]" class="image-preview-wrap">
                <img :src="formatImageUrl(colorImages[color.id])" :alt="color.tenMauSac" class="preview-img" />
                <button
                    type="button"
                    class="btn-remove-preview"
                    @click.stop="removeColorImage(color.id)"
                >
                  ✕
                </button>
              </div>

              <div v-else class="image-placeholder">
                <div class="placeholder-icon">
                  <svg viewBox="0 0 24 24" width="36" height="36" fill="currentColor" opacity="0.45">
                    <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/>
                  </svg>
                </div>
                <div class="placeholder-text">Nhóm màu này chưa có ảnh</div>
                <div class="placeholder-sub">Nhấn để tải ảnh áp dụng cho toàn bộ kích cỡ màu này.</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 6. Chân trang Action buttons (Chỉ hiển thị khi Thêm mới sản phẩm) -->
      <div v-if="!isEdit" class="form-footer-actions mt-4">
        <button type="button" class="btn btn-cancel-action" @click="handleBack">
          Hủy
        </button>
        <button
            type="submit"
            class="btn btn-save-action"
            :disabled="submitting"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>{{ submitting ? 'Đang lưu...' : 'Thêm sản phẩm' }}</span>
        </button>
      </div>
    </form>

    <!-- 8. Modal Xác nhận hành động (Chuẩn theo mẫu thiết kế) -->
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
                class="btn btn-save-confirm"
                @click="executeConfirmedAction"
            >
              {{ confirmDialog.confirmText || 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 9. Modal Thêm nhanh thuộc tính sản phẩm -->
    <transition name="fade">
      <div v-if="quickAttrModal.show" class="confirm-modal-overlay" @click.self="closeQuickAttrModal">
        <div class="quick-attr-modal-card" @click.stop>
          <div class="quick-attr-modal-header">
            <h3 class="quick-attr-title">Thêm Mới {{ quickAttrModal.title }}</h3>
            <button type="button" class="btn-close-modal" @click="closeQuickAttrModal">✕</button>
          </div>

          <div class="quick-attr-modal-body">
            <div class="quick-attr-field">
              <label class="form-label">Tên {{ quickAttrModal.title }} <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="quickAttrModal.ten"
                  class="form-control"
                  :placeholder="`Nhập tên ${quickAttrModal.title.toLowerCase()}...`"
                  @keyup.enter="submitQuickAttr"
                  autofocus
                  maxlength="255"
              />
            </div>

            <div v-if="quickAttrModal.error" class="quick-attr-error-banner mt-3">
              {{ quickAttrModal.error }}
            </div>
          </div>

          <div class="quick-attr-modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeQuickAttrModal" :disabled="quickAttrModal.loading">
              Hủy bỏ
            </button>
            <button
                type="button"
                class="btn btn-primary"
                @click="submitQuickAttr"
                :disabled="quickAttrModal.loading"
            >
              {{ quickAttrModal.loading ? 'Đang lưu...' : 'Xác nhận lưu' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 10. Modal Chỉnh sửa / Thêm mới biến thể sản phẩm -->
    <transition name="fade">
      <div v-if="variantModal.show" class="confirm-modal-overlay" @click.self="closeVariantModal">
        <div class="variant-modal-card" @click.stop>
          <div class="variant-modal-header">
            <div class="variant-modal-title-box">
              <span class="variant-modal-badge">{{ variantModal.isEdit ? '✏️ Chỉnh sửa' : '✨ Thêm mới' }}</span>
              <h3 class="variant-modal-title">
                {{ variantModal.isEdit ? 'Chỉnh sửa biến thể sản phẩm' : 'Thêm mới biến thể sản phẩm' }}
              </h3>
            </div>
            <button type="button" class="btn-close-modal" @click="closeVariantModal">&times;</button>
          </div>

          <div class="variant-modal-body">
            <div class="variant-modal-grid">
              <!-- Mã CTSP -->
              <div class="variant-modal-field">
                <label class="item-label">Mã chi tiết sản phẩm</label>
                <input
                    type="text"
                    v-model="variantModal.data.maChiTietSanPham"
                    class="form-input"
                    placeholder="Mã tự sinh nếu để trống..."
                    maxlength="50"
                />
              </div>

              <!-- Màu sắc -->
              <div class="variant-modal-field">
                <label class="item-label">Màu sắc <span class="text-danger">*</span></label>
                <Select2
                  v-model="variantModal.data.idMauSac"
                  :options="attributes.mauSac"
                  label-key="tenMauSac"
                  placeholder="-- Chọn màu sắc --"
                  search-placeholder="Tìm kiếm màu sắc..."
                  :allow-clear="false"
                />
              </div>

              <!-- Kích cỡ -->
              <div class="variant-modal-field">
                <label class="item-label">Kích cỡ <span class="text-danger">*</span></label>
                <Select2
                  v-model="variantModal.data.idKichCo"
                  :options="attributes.kichCo"
                  label-key="tenKichCo"
                  placeholder="-- Chọn kích cỡ --"
                  search-placeholder="Tìm kiếm kích cỡ..."
                  :allow-clear="false"
                />
              </div>

              <!-- Số lượng -->
              <div class="variant-modal-field">
                <label class="item-label">Số lượng tồn kho <span class="text-danger">*</span></label>
                <input
                    type="number"
                    min="0"
                    v-model.number="variantModal.data.soLuong"
                    class="form-input"
                    placeholder="Nhập số lượng..."
                    @keydown="blockInvalidIntegerKeys"
                />
              </div>

              <!-- Giá bán -->
              <div class="variant-modal-field">
                <label class="item-label">Giá bán (VNĐ) <span class="text-danger">*</span></label>
                <input
                    type="text"
                    inputmode="numeric"
                    :value="formatNumberWithDots(variantModal.data.giaBan)"
                    class="form-input"
                    placeholder="Nhập giá bán..."
                    @focus="$event.target.select()"
                    @input="onModalVariantPriceInput"
                    @keydown="blockInvalidIntegerKeys"
                    @paste="handleIntegerPaste"
                    @blur="onModalBlurPrice"
                />
              </div>

              <!-- Trạng thái -->
              <div class="variant-modal-field">
                <label class="item-label">Trạng thái kinh doanh</label>
                <div class="status-radio-group">
                  <label class="status-radio-option">
                    <input
                        type="radio"
                        :value="1"
                        v-model.number="variantModal.data.trangThai"
                    />
                    <span class="status-radio-label is-active">Đang kinh doanh</span>
                  </label>
                  <label class="status-radio-option">
                    <input
                        type="radio"
                        :value="0"
                        v-model.number="variantModal.data.trangThai"
                    />
                    <span class="status-radio-label is-inactive">Ngừng kinh doanh</span>
                  </label>
                </div>
              </div>

              <!-- Hình ảnh biến thể -->
              <div class="variant-modal-field full-width-col">
                <label class="item-label">Hình ảnh biến thể</label>
                <div class="variant-img-upload-box">
                  <div class="variant-img-thumb-preview" @click="triggerModalVariantImageUpload">
                    <img
                      v-if="variantModal.data.anhDaiDien"
                      :src="formatImageUrl(variantModal.data.anhDaiDien)"
                      alt="Ảnh biến thể"
                      class="variant-thumb-img"
                    />
                    <div v-else class="variant-thumb-empty">
                      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                      </svg>
                      <span>Chọn ảnh</span>
                    </div>
                  </div>

                  <div class="variant-img-meta-col">
                    <input
                      type="file"
                      ref="modalVariantFileInput"
                      accept="image/png, image/jpeg, image/webp"
                      style="display: none;"
                      @change="onModalVariantImageSelected"
                    />
                    <div class="variant-img-btn-row">
                      <button
                        type="button"
                        class="btn btn-choose-variant-img"
                        @click="triggerModalVariantImageUpload"
                      >
                        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                          <polyline points="17 8 12 3 7 8"></polyline>
                          <line x1="12" y1="3" x2="12" y2="15"></line>
                        </svg>
                        <span>{{ variantModal.data.anhDaiDien ? 'Đổi ảnh' : 'Tải ảnh lên' }}</span>
                      </button>
                      <button
                        v-if="variantModal.data.anhDaiDien"
                        type="button"
                        class="btn btn-remove-variant-img"
                        @click="removeModalVariantImage"
                      >
                        Gỡ ảnh
                      </button>
                    </div>
                    <span class="variant-img-hint-text">JPG, PNG, WEBP (Tối đa 5MB). Ảnh hiển thị riêng cho biến thể này.</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="variant-modal-footer">
            <button type="button" class="btn btn-modal-cancel" @click="closeVariantModal">
              Hủy
            </button>
            <button
                type="button"
                class="btn btn-modal-submit"
                :disabled="variantModal.saving"
                @click="saveVariantModal"
            >
              {{ variantModal.saving ? 'Đang lưu...' : (variantModal.isEdit ? 'Cập nhật biến thể' : 'Thêm biến thể') }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 11. Modal Xem Chi Tiết Biến Thể -->
    <transition name="fade">
      <div v-if="viewVariantModal.show" class="confirm-modal-overlay" @click.self="closeViewVariantModal">
        <div class="variant-modal-card" @click.stop>
          <div class="variant-modal-header">
            <div class="variant-modal-title-box">
              <span class="variant-modal-badge">👁 Chi tiết</span>
              <h3 class="variant-modal-title">Chi tiết biến thể sản phẩm</h3>
            </div>
            <button type="button" class="btn-close-modal" @click="closeViewVariantModal">&times;</button>
          </div>

          <div class="variant-modal-body">
            <div class="variant-modal-grid">
              <div class="variant-modal-field">
                <span class="detail-label-text">Mã chi tiết sản phẩm:</span>
                <span class="ctsp-code-badge">{{ viewVariantModal.data.maChiTietSanPham || '—' }}</span>
              </div>
              <div class="variant-modal-field">
                <span class="detail-label-text">Tên sản phẩm:</span>
                <span class="detail-val-text font-bold">{{ form.tenSanPham || '—' }}</span>
              </div>
              <div class="variant-modal-field">
                <span class="detail-label-text">Màu sắc:</span>
                <div class="color-badge-cell">
                  <span class="color-circle-dot" :style="{ backgroundColor: getVariantColorHex(viewVariantModal.data) }"></span>
                  <span class="color-name-text">{{ getVariantColorName(viewVariantModal.data) }}</span>
                </div>
              </div>
              <div class="variant-modal-field">
                <span class="detail-label-text">Kích cỡ:</span>
                <span class="size-pill-tag">{{ getVariantSizeName(viewVariantModal.data) }}</span>
              </div>
              <div class="variant-modal-field">
                <span class="detail-label-text">Số lượng tồn kho:</span>
                <span class="detail-val-text font-bold text-success">{{ viewVariantModal.data.soLuong ?? 0 }}</span>
              </div>
              <div class="variant-modal-field">
                <span class="detail-label-text">Giá bán:</span>
                <span class="detail-val-text font-bold text-danger">{{ formatNumberWithDots(viewVariantModal.data.giaBan) }} VNĐ</span>
              </div>
              <div class="variant-modal-field" style="grid-column: span 2;">
                <span class="detail-label-text">Trạng thái kinh doanh:</span>
                <div class="mt-1">
                  <span :class="['status-badge', viewVariantModal.data.trangThai === 1 ? 'status-active' : 'status-inactive']">
                    {{ viewVariantModal.data.trangThai === 1 ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
                  </span>
                </div>
              </div>
              <div class="variant-modal-field" style="grid-column: span 2;" v-if="viewVariantModal.data.anhDaiDien">
                <span class="detail-label-text">Hình ảnh biến thể:</span>
                <div class="view-variant-img-preview mt-2">
                  <img
                    :src="formatImageUrl(viewVariantModal.data.anhDaiDien)"
                    alt="Ảnh biến thể"
                    class="view-variant-img"
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="variant-modal-footer">
            <button type="button" class="btn btn-modal-cancel" @click="closeViewVariantModal">
              Đóng
            </button>
            <button
                type="button"
                class="btn btn-modal-submit"
                @click="switchToEditFromView"
            >
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block; vertical-align: middle; margin-right: 4px;">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
              <span>Chỉnh sửa biến thể này</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../api'
import Select2 from '../../components/Select2Combobox.vue'

// Tùy chọn trạng thái kinh doanh dùng cho Select2
const statusOptions = [
  { id: 1, label: 'Kinh doanh' },
  { id: 0, label: 'Ngừng kinh doanh' }
]

const route = useRoute()
const router = useRouter()

// Trạng thái Form: Thêm mới hoặc Chỉnh sửa
const isEdit = computed(() => !!route.params.id)
const productId = computed(() => route.params.id)

// Toast notification
const toast = ref({ show: false, message: '', type: 'success' })
const showToast = (message, type = 'success') => {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3500)
}

// Modal xác nhận
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
  if (confirmDialog.value.onConfirm) {
    const fn = confirmDialog.value.onConfirm
    closeConfirmDialog()
    await fn()
  } else {
    closeConfirmDialog()
  }
}

// Danh mục thuộc tính
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

// Dữ liệu Form sản phẩm
const form = ref({
  maSanPham: '',
  tenSanPham: '',
  idThuongHieu: '',
  idDanhMuc: '',
  idXuatXu: '',
  idChatLieu: '',
  idCoAo: '',
  idTayAo: '',
  idHoaTiet: '',
  trangThai: 1,
  moTa: ''
})

// Chọn màu sắc & kích cỡ
const selectedColors = ref([])
const selectedSizes = ref([])
const showColorDropdown = ref(false)
const showSizeDropdown = ref(false)
const colorSearch = ref('')
const sizeSearch = ref('')

// Ảnh theo màu sắc
const colorImages = ref({})
const fileInputs = ref({})

// Quản lý ảnh chung của sản phẩm (dùng chung cho các biến thể)
const productImages = ref([])
const pendingFiles = ref([])
const productFileInput = ref(null)

const triggerSelectProductImages = () => {
  if (productFileInput.value) {
    productFileInput.value.click()
  }
}

const onProductFilesSelected = (e) => {
  const files = Array.from(e.target.files || [])
  if (files.length === 0) return

  const validFiles = []
  for (const file of files) {
    if (file.size > 5 * 1024 * 1024) {
      showToast(`Ảnh "${file.name}" vượt quá dung lượng tối đa 5MB!`, 'error')
    } else {
      validFiles.push(file)
    }
  }

  pendingFiles.value = validFiles
}

const addPendingImages = () => {
  if (pendingFiles.value.length === 0) return

  const filesToProcess = [...pendingFiles.value]
  pendingFiles.value = []
  if (productFileInput.value) {
    productFileInput.value.value = ''
  }

  filesToProcess.forEach(file => {
    const reader = new FileReader()
    reader.onload = (event) => {
      const isFirst = productImages.value.length === 0
      productImages.value.push({
        url: event.target.result,
        file: file,
        name: file.name,
        isMain: isFirst
      })
      showToast(`Đã thêm ảnh "${file.name}"!`, 'success')
    }
    reader.readAsDataURL(file)
  })
}

const setMainImage = (index) => {
  productImages.value.forEach((img, idx) => {
    img.isMain = (idx === index)
  })
  const [mainItem] = productImages.value.splice(index, 1)
  productImages.value.unshift(mainItem)
  showToast('Đã đặt làm ảnh chính!', 'success')
}

const removeProductImage = (index) => {
  const removed = productImages.value.splice(index, 1)[0]
  if (productImages.value.length > 0 && (!productImages.value.some(img => img.isMain) || (removed && removed.isMain))) {
    productImages.value[0].isMain = true
  }
  showToast('Đã gỡ ảnh!')
}

// Biến thể đã sinh
const variantsList = ref([])
const selectAllVariants = ref(true)
const bulkQty = ref(0)
const bulkPrice = ref(0)
const submitting = ref(false)

// Quản lý ảnh riêng biệt cho từng biến thể
const variantFileInput = ref(null)
const editingVariantItem = ref(null)

const triggerVariantImageUpload = (vItem) => {
  editingVariantItem.value = vItem
  if (variantFileInput.value) {
    variantFileInput.value.value = ''
    variantFileInput.value.click()
  }
}

const onVariantImageSelected = (e) => {
  const file = e.target.files?.[0]
  if (!file || !editingVariantItem.value) return

  if (file.size > 5 * 1024 * 1024) {
    showToast('Ảnh quá lớn (tối đa 5MB)!', 'error')
    return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    editingVariantItem.value.anhDaiDien = event.target.result
    showToast(`Đã chọn ảnh riêng cho biến thể (${editingVariantItem.value.tenMauSac} - ${editingVariantItem.value.tenKichCo})!`, 'success')
  }
  reader.readAsDataURL(file)
}

const removeVariantImage = (vItem) => {
  vItem.anhDaiDien = ''
  showToast(`Đã gỡ ảnh riêng của biến thể (${vItem.tenMauSac} - ${vItem.tenKichCo})!`)
}

// Lọc màu sắc & kích cỡ theo search
const filteredColors = computed(() => {
  if (!colorSearch.value.trim()) return attributes.value.mauSac
  const q = colorSearch.value.toLowerCase().trim()
  return attributes.value.mauSac.filter(c => c.tenMauSac?.toLowerCase().includes(q))
})

const filteredSizes = computed(() => {
  if (!sizeSearch.value.trim()) return attributes.value.kichCo
  const q = sizeSearch.value.toLowerCase().trim()
  return attributes.value.kichCo.filter(s => s.tenKichCo?.toLowerCase().includes(q))
})

const selectedColorNames = computed(() => {
  return selectedColors.value.map(c => c.tenMauSac).join(', ')
})

const selectedSizeNames = computed(() => {
  return selectedSizes.value.map(s => s.tenKichCo).join(', ')
})

// Kiểm tra màu / size đã chọn chưa
const isColorSelected = (id) => selectedColors.value.some(c => c.id === id)
const isSizeSelected = (id) => selectedSizes.value.some(s => s.id === id)

// Bảng màu & Color Picker
const showColorPalette = ref(false)
const quickPickHex = ref('#496883')
const newColorForm = ref({
  ten: '',
  maHex: '#496883',
  loading: false
})

const toggleColorPalette = () => {
  showColorPalette.value = !showColorPalette.value
  showColorDropdown.value = false
  showSizeDropdown.value = false
}

// Khi người dùng chọn màu từ ô Native Color Picker quang phổ
const handleQuickPickColor = () => {
  const hex = (quickPickHex.value || '').toUpperCase()
  newColorForm.value.maHex = hex

  // 1. Tìm xem trong danh mục màu sắc đã có màu có mã hex này chưa
  const exactMatch = attributes.value.mauSac?.find(
    c => c.maHex && c.maHex.toUpperCase() === hex
  )

  if (exactMatch) {
    if (!isColorSelected(exactMatch.id)) {
      toggleColor(exactMatch)
      showToast(`Đã chọn màu "${exactMatch.tenMauSac}" từ bảng màu!`, 'success')
    } else {
      showToast(`Màu "${exactMatch.tenMauSac}" đã được chọn trước đó.`, 'info')
    }
    return
  }

  // 2. Nếu chưa có màu khớp mã Hex, tự động mở bảng màu để người dùng đặt tên và lưu nhanh
  showColorPalette.value = true
  showToast(`Mã màu ${hex} chưa có trong danh mục. Bạn có thể đặt tên và bấm "+ Thêm"!`, 'info')
}

// Thêm màu mới trực tiếp từ Bảng màu
const handleAddCustomColor = async () => {
  const ten = newColorForm.value.ten?.trim()
  if (!ten) {
    showToast('Vui lòng nhập tên cho màu mới!', 'error')
    return
  }
  const hex = newColorForm.value.maHex || quickPickHex.value

  newColorForm.value.loading = true
  try {
    const res = await api.post('/api/thuoc-tinh/type/mau-sac', {
      ten: ten,
      maHex: hex,
      trangThai: 1
    })
    const createdColor = res.data
    // Đưa vào danh sách màu sắc của hệ thống
    if (!attributes.value.mauSac) attributes.value.mauSac = []
    attributes.value.mauSac.push(createdColor)
    // Tự động chọn luôn màu vừa thêm
    if (!isColorSelected(createdColor.id)) {
      toggleColor(createdColor)
    }
    newColorForm.value.ten = ''
    showToast(`Đã thêm và chọn màu "${createdColor.tenMauSac}"!`, 'success')
  } catch (err) {
    const msg = err.response?.data?.message || 'Không thể thêm màu mới!'
    showToast(msg, 'error')
  } finally {
    newColorForm.value.loading = false
  }
}

// Modal Thêm nhanh các loại thuộc tính
const attrTypeConfigs = {
  'thuong-hieu': { title: 'Thương hiệu', field: 'idThuongHieu', listKey: 'thuongHieu', codePrefix: 'TH' },
  'danh-muc': { title: 'Danh mục', field: 'idDanhMuc', listKey: 'danhMuc', codePrefix: 'DM' },
  'xuat-xu': { title: 'Xuất xứ', field: 'idXuatXu', listKey: 'xuatXu', codePrefix: 'XX' },
  'chat-lieu': { title: 'Chất liệu', field: 'idChatLieu', listKey: 'chatLieu', codePrefix: 'CL' },
  'co-ao': { title: 'Cổ áo', field: 'idCoAo', listKey: 'coAo', codePrefix: 'CA' },
  'tay-ao': { title: 'Tay áo', field: 'idTayAo', listKey: 'tayAo', codePrefix: 'TA' },
  'hoa-tiet': { title: 'Họa tiết', field: 'idHoaTiet', listKey: 'hoaTiet', codePrefix: 'HT' },
  'kich-co': { title: 'Kích cỡ', field: '', listKey: 'kichCo', codePrefix: 'KC' }
}

const quickAttrModal = ref({
  show: false,
  type: '',
  title: '',
  field: '',
  listKey: '',
  codePrefix: '',
  ten: '',
  ma: '',
  loading: false,
  error: ''
})

const openQuickAddModal = (type) => {
  const cfg = attrTypeConfigs[type]
  if (!cfg) return
  const suffix = Math.floor(1000 + Math.random() * 9000)
  quickAttrModal.value = {
    show: true,
    type,
    title: cfg.title,
    field: cfg.field,
    listKey: cfg.listKey,
    codePrefix: cfg.codePrefix,
    ten: '',
    ma: `${cfg.codePrefix}${suffix}`,
    loading: false,
    error: ''
  }
}

const closeQuickAttrModal = () => {
  quickAttrModal.value.show = false
  quickAttrModal.value.error = ''
}

const submitQuickAttr = async () => {
  const ten = quickAttrModal.value.ten?.trim()
  if (!ten) {
    quickAttrModal.value.error = `Vui lòng nhập tên ${quickAttrModal.value.title.toLowerCase()}!`
    return
  }
  if (ten.length > 255) {
    quickAttrModal.value.error = 'Tên không được vượt quá 255 ký tự!'
    return
  }

  quickAttrModal.value.loading = true
  quickAttrModal.value.error = ''
  try {
    const payload = {
      ten: ten,
      ma: quickAttrModal.value.ma?.trim() || undefined,
      trangThai: 1
    }
    const res = await api.post(`/api/thuoc-tinh/type/${quickAttrModal.value.type}`, payload)
    const createdItem = res.data

    // Thêm vào danh sách thuộc tính tương ứng
    const listKey = quickAttrModal.value.listKey
    if (!attributes.value[listKey]) {
      attributes.value[listKey] = []
    }
    attributes.value[listKey].push(createdItem)

    // Tự động gán thuộc tính mới tạo vào form hoặc danh sách chọn
    if (quickAttrModal.value.field) {
      form.value[quickAttrModal.value.field] = createdItem.id
    } else if (quickAttrModal.value.type === 'kich-co') {
      if (!isSizeSelected(createdItem.id)) {
        selectedSizes.value.push(createdItem)
      }
    }

    showToast(`Đã thêm mới và chọn ${quickAttrModal.value.title.toLowerCase()} "${ten}"!`, 'success')
    closeQuickAttrModal()
  } catch (err) {
    const msg = err.response?.data?.message || 'Có lỗi xảy ra khi thêm thuộc tính!'
    quickAttrModal.value.error = msg
  } finally {
    quickAttrModal.value.loading = false
  }
}

// Xử lý khi tạo mới thuộc tính từ AttributeCombobox
const onAttributeCreated = (listKey, createdItem) => {
  if (!attributes.value[listKey]) {
    attributes.value[listKey] = []
  }
  attributes.value[listKey].push(createdItem)
}

const toggleColorDropdown = () => {
  showColorDropdown.value = !showColorDropdown.value
  showSizeDropdown.value = false
  showColorPalette.value = false
}

const toggleSizeDropdown = () => {
  showSizeDropdown.value = !showSizeDropdown.value
  showColorDropdown.value = false
  showColorPalette.value = false
}

const toggleColor = (color) => {
  const idx = selectedColors.value.findIndex(c => c.id === color.id)
  if (idx >= 0) {
    selectedColors.value.splice(idx, 1)
  } else {
    selectedColors.value.push(color)
  }
}

const toggleSize = (size) => {
  const idx = selectedSizes.value.findIndex(s => s.id === size.id)
  if (idx >= 0) {
    selectedSizes.value.splice(idx, 1)
  } else {
    selectedSizes.value.push(size)
  }
}

const removeColor = (id) => {
  selectedColors.value = selectedColors.value.filter(c => c.id !== id)
  // Xóa toàn bộ biến thể của màu này trong variantsList
  variantsList.value = variantsList.value.filter(v => v.idMauSac !== id)
  // Xóa ảnh tương ứng của màu này
  delete colorImages.value[id]
}

const removeSize = (id) => {
  selectedSizes.value = selectedSizes.value.filter(s => s.id !== id)
  // Xóa toàn bộ biến thể của kích cỡ này trong variantsList
  variantsList.value = variantsList.value.filter(v => v.idKichCo !== id)
}

const clearAllColors = () => {
  selectedColors.value = []
  variantsList.value = []
  colorImages.value = {}
}

const clearAllSizes = () => {
  selectedSizes.value = []
  variantsList.value = []
}

// Đóng dropdown khi click bên ngoài
const closeAllDropdowns = () => {
  showColorDropdown.value = false
  showSizeDropdown.value = false
  showColorPalette.value = false
}

// Sinh danh sách biến thể tự động từ các Màu x Size đã chọn
const generateVariants = () => {
  if (selectedColors.value.length === 0) {
    showToast('Vui lòng chọn ít nhất 1 màu sắc!', 'error')
    return
  }
  if (selectedSizes.value.length === 0) {
    showToast('Vui lòng chọn ít nhất 1 kích cỡ!', 'error')
    return
  }

  const existingMap = new Map()
  variantsList.value.forEach(v => {
    existingMap.set(`${v.idMauSac}_${v.idKichCo}`, v)
  })

  const newVariants = []
  selectedColors.value.forEach(color => {
    selectedSizes.value.forEach(size => {
      const key = `${color.id}_${size.id}`
      if (existingMap.has(key)) {
        const item = existingMap.get(key)
        if (!item.anhDaiDien && colorImages.value[color.id]) {
          item.anhDaiDien = colorImages.value[color.id]
        }
        newVariants.push(item)
      } else {
        newVariants.push({
          key,
          id: null,
          idMauSac: color.id,
          tenMauSac: color.tenMauSac,
          maHex: color.maHex,
          idKichCo: size.id,
          tenKichCo: size.tenKichCo,
          soLuong: (bulkQty.value !== '' && bulkQty.value !== null && bulkQty.value !== undefined)
            ? Math.floor(Number(bulkQty.value))
            : 0,
          giaBan: (bulkPrice.value !== '' && bulkPrice.value !== null && bulkPrice.value !== undefined)
            ? Math.floor(Number(bulkPrice.value))
            : 0,
          trangThai: 1,
          anhDaiDien: colorImages.value[color.id] || '',
          selected: true
        })
      }
    })
  })

  variantsList.value = newVariants
  showToast(`Đã tạo tự động ${newVariants.length} biến thể!`, 'success')
}

// Gom nhóm biến thể theo Màu sắc
const groupedVariants = computed(() => {
  const groups = []
  selectedColors.value.forEach(color => {
    const items = variantsList.value.filter(v => v.idMauSac === color.id)
    if (items.length > 0) {
      const sizeNames = items.map(i => i.tenKichCo).join(' • ')
      groups.push({
        color,
        sizesSummary: sizeNames,
        items
      })
    }
  })
  return groups
})

// Danh sách màu sắc thực tế hiển thị cho khối ảnh sản phẩm chi tiết
const activeColorsForImages = computed(() => {
  if (variantsList.value.length > 0) {
    const colorMap = new Map()
    variantsList.value.forEach(v => {
      if (v.idMauSac && !colorMap.has(v.idMauSac)) {
        colorMap.set(v.idMauSac, {
          id: v.idMauSac,
          tenMauSac: v.tenMauSac,
          maHex: v.maHex
        })
      }
    })
    return Array.from(colorMap.values())
  }
  return selectedColors.value
})

// Tự động xóa ảnh và dọn dẹp khi một màu không còn biến thể nào
watch(activeColorsForImages, (newColors) => {
  const validIds = new Set(newColors.map(c => String(c.id)))
  Object.keys(colorImages.value).forEach(colorId => {
    if (!validIds.has(String(colorId))) {
      delete colorImages.value[colorId]
    }
  })
}, { deep: true })

// Kiểm tra group đã chọn hết chưa
const isGroupAllSelected = (group) => {
  return group.items.length > 0 && group.items.every(i => i.selected)
}

const toggleGroupSelection = (group) => {
  const allSel = isGroupAllSelected(group)
  group.items.forEach(i => i.selected = !allSel)
}

const toggleSelectAllVariants = () => {
  variantsList.value.forEach(v => v.selected = selectAllVariants.value)
}

// Chặn các phím số âm, số thập phân và ký tự lạ (chỉ cho phép nhập số nguyên không âm)
const blockInvalidIntegerKeys = (e) => {
  if (['-', '+', 'e', 'E', '.', ','].includes(e.key)) {
    e.preventDefault()
  }
}

// Chặn dán nội dung không phải số nguyên dương (cho phép dấu chấm/phẩy phân cách khi copy paste)
const handleIntegerPaste = (e) => {
  const pasteData = e.clipboardData?.getData('text') || ''
  const clean = pasteData.replace(/[.,\s]/g, '')
  if (!/^\d+$/.test(clean.trim())) {
    e.preventDefault()
    showToast('Chỉ được dán số nguyên dương!', 'warning')
  }
}

// Format số nguyên có dấu chấm phân cách hàng nghìn (VD: 10000 -> "10.000", 100000 -> "100.000")
const formatNumberWithDots = (val) => {
  if (val === null || val === undefined || val === '') return ''
  const clean = String(val).replace(/\D/g, '')
  if (!clean) return ''
  return clean.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

// Xử lý nhập giá tiền hiển thị liền có dấu chấm mà không bị lỗi bộ gõ tiếng Việt Telex
const handlePriceInput = (e, callback) => {
  const input = e.target
  const val = input.value
  const raw = val.replace(/\D/g, '')

  if (!raw) {
    callback('')
    if (input.value !== '') input.value = ''
    return
  }

  let num = parseInt(raw, 10)
  if (num > 1000000000) num = 1000000000
  callback(num)

  const formatted = formatNumberWithDots(num)

  // Chỉ can thiệp DOM và con trỏ chuột khi chuỗi hiển thị có thay đổi (chèn/bớt dấu chấm)
  if (input.value !== formatted) {
    const cursorFromRight = val.length - (input.selectionEnd || val.length)
    input.value = formatted
    const newPos = Math.max(0, formatted.length - cursorFromRight)
    input.setSelectionRange(newPos, newPos)
  }
}

const onBulkPriceInput = (e) => {
  handlePriceInput(e, (val) => {
    bulkPrice.value = val === '' ? '' : val
  })
}

const onVariantPriceInput = (e, item) => {
  handlePriceInput(e, (val) => {
    item.giaBan = val === '' ? '' : val
  })
}

// Chuẩn hóa về số nguyên khi rời ô nhập giá bán (blur)
const onBlurPrice = (target) => {
  if (target === 'bulk') {
    if (bulkPrice.value !== '' && bulkPrice.value !== null && bulkPrice.value !== undefined) {
      const num = Math.floor(Number(bulkPrice.value))
      bulkPrice.value = isNaN(num) || num < 0 ? 0 : Math.min(num, 1000000000)
    }
  } else if (target && typeof target === 'object') {
    if (target.giaBan !== '' && target.giaBan !== null && target.giaBan !== undefined) {
      const num = Math.floor(Number(target.giaBan))
      target.giaBan = isNaN(num) || num < 0 ? 0 : Math.min(num, 1000000000)
    }
  }
}

// Áp dụng số lượng & giá bán mặc định hàng loạt
const applyBulkValues = () => {
  if (bulkQty.value !== null && bulkQty.value !== undefined && bulkQty.value !== '') {
    const sl = Number(bulkQty.value)
    if (isNaN(sl) || !Number.isInteger(sl) || sl < 0 || sl > 999999) {
      showToast('Số lượng áp dụng phải là số nguyên từ 0 đến 999,999!', 'error')
      return
    }
  }
  if (bulkPrice.value !== null && bulkPrice.value !== undefined && bulkPrice.value !== '') {
    const gb = Number(bulkPrice.value)
    if (isNaN(gb) || !Number.isInteger(gb) || gb < 0 || gb > 1000000000) {
      showToast('Giá bán áp dụng phải là số nguyên từ 0 đến 1,000,000,000 VNĐ!', 'error')
      return
    }
  }

  let count = 0
  variantsList.value.forEach(v => {
    if (v.selected) {
      if (bulkQty.value !== null && bulkQty.value !== undefined && bulkQty.value !== '') v.soLuong = Number(bulkQty.value)
      if (bulkPrice.value !== null && bulkPrice.value !== undefined && bulkPrice.value !== '') v.giaBan = Number(bulkPrice.value)
      count++
    }
  })

  if (count === 0) {
    showToast('Vui lòng chọn ít nhất một biến thể để áp dụng hàng loạt!', 'warning')
    return
  }
  showToast(`Đã áp dụng cho ${count} biến thể được chọn!`, 'success')
}

// Xóa 1 biến thể với xác nhận (tự động xóa ảnh và nhóm màu nếu không còn biến thể nào của màu đó)
const openDeleteVariantConfirm = (item) => {
  openConfirm({
    title: 'Xác nhận xóa biến thể',
    message: `Bạn có muốn xóa biến thể "${item.tenMauSac} - ${item.tenKichCo}" không?`,
    type: 'danger',
    confirmText: 'Xóa biến thể',
    onConfirm: () => {
      const colorId = item.idMauSac
      variantsList.value = variantsList.value.filter(v => v.key !== item.key)

      // Kiểm tra xem màu này còn biến thể nào trong variantsList không
      const remainingOfColor = variantsList.value.filter(v => v.idMauSac === colorId)
      if (remainingOfColor.length === 0) {
        // Tự động xóa ảnh của màu này nếu màu không còn biến thể nào
        delete colorImages.value[colorId]
        // Bỏ chọn màu này khỏi selectedColors để đồng bộ cả ở dropdown & tags
        selectedColors.value = selectedColors.value.filter(c => c.id !== colorId)
      }
      showToast('Đã xóa biến thể khỏi danh sách!')
    }
  })
}

// -------------------------------------------------------------
// QUẢN LÝ BIẾN THỂ TRONG CHẾ ĐỘ SỬA SẢN PHẨM (isEdit)
// -------------------------------------------------------------
const getVariantColorHex = (v) => {
  if (v.maHex) return v.maHex
  const found = attributes.value.mauSac?.find(c => c.id === v.idMauSac)
  return found?.maHex || '#888888'
}

const getVariantColorName = (v) => {
  if (v.tenMauSac) return v.tenMauSac
  const found = attributes.value.mauSac?.find(c => c.id === v.idMauSac)
  return found?.tenMauSac || 'Mặc định'
}

const getVariantSizeName = (v) => {
  if (v.tenKichCo) return v.tenKichCo
  const found = attributes.value.kichCo?.find(s => s.id === v.idKichCo)
  return found?.tenKichCo || '—'
}

const variantModal = ref({
  show: false,
  isEdit: false,
  saving: false,
  data: {
    id: null,
    maChiTietSanPham: '',
    idMauSac: '',
    idKichCo: '',
    soLuong: 0,
    giaBan: 0,
    trangThai: 1
  }
})

const openAddVariantModal = () => {
  const nextIdx = variantsList.value.length + 1
  variantModal.value = {
    show: true,
    isEdit: false,
    saving: false,
    data: {
      id: null,
      maChiTietSanPham: `CTSP${String(nextIdx).padStart(3, '0')}`,
      idMauSac: attributes.value.mauSac?.[0]?.id || '',
      idKichCo: attributes.value.kichCo?.[0]?.id || '',
      soLuong: 0,
      giaBan: 0,
      trangThai: 1,
      anhDaiDien: ''
    }
  }
}

const openEditVariantModal = (v) => {
  variantModal.value = {
    show: true,
    isEdit: true,
    saving: false,
    data: {
      id: v.id,
      maChiTietSanPham: v.maChiTietSanPham || '',
      idMauSac: v.idMauSac || '',
      idKichCo: v.idKichCo || '',
      soLuong: v.soLuong ?? 0,
      giaBan: v.giaBan ?? 0,
      trangThai: v.trangThai ?? 1,
      anhDaiDien: v.anhDaiDien || ''
    }
  }
}

const closeVariantModal = () => {
  variantModal.value.show = false
  variantModal.value.saving = false
}

// Xử lý upload / gỡ ảnh cho biến thể trong modal của ThemSuaSanPham
const modalVariantFileInput = ref(null)

const triggerModalVariantImageUpload = () => {
  if (modalVariantFileInput.value) {
    modalVariantFileInput.value.click()
  }
}

const onModalVariantImageSelected = (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    showToast('Ảnh quá lớn (tối đa 5MB)!', 'error')
    return
  }

  const reader = new FileReader()
  reader.onload = (event) => {
    variantModal.value.data.anhDaiDien = event.target.result
    showToast('Đã chọn ảnh cho biến thể!', 'success')
  }
  reader.readAsDataURL(file)
}

const removeModalVariantImage = () => {
  variantModal.value.data.anhDaiDien = ''
  if (modalVariantFileInput.value) {
    modalVariantFileInput.value.value = ''
  }
}

// Modal Xem chi tiết biến thể
const viewVariantModal = ref({
  show: false,
  data: {}
})

const openViewVariantModal = (v) => {
  viewVariantModal.value = {
    show: true,
    data: { ...v }
  }
}

const closeViewVariantModal = () => {
  viewVariantModal.value.show = false
  viewVariantModal.value.data = {}
}

const switchToEditFromView = () => {
  const item = { ...viewVariantModal.value.data }
  closeViewVariantModal()
  openEditVariantModal(item)
}

const onModalVariantPriceInput = (e) => {
  handlePriceInput(e, (val) => {
    variantModal.value.data.giaBan = val === '' ? '' : val
  })
}

const onModalBlurPrice = () => {
  const d = variantModal.value.data
  if (d.giaBan !== '' && d.giaBan !== null && d.giaBan !== undefined) {
    const num = Math.floor(Number(d.giaBan))
    d.giaBan = isNaN(num) || num < 0 ? 0 : Math.min(num, 1000000000)
  }
}

// 1. Thao tác bật/tắt trạng thái kinh doanh của biến thể (Power button)
const toggleVariantActiveStatus = (v) => {
  const newStatus = v.trangThai === 1 ? 0 : 1
  const actionText = newStatus === 1 ? 'mở kinh doanh' : 'ngừng kinh doanh'
  const label = v.maChiTietSanPham || `${getVariantColorName(v)} - ${getVariantSizeName(v)}`

  openConfirm({
    title: 'Xác nhận đổi trạng thái biến thể',
    message: `Bạn có chắc chắn muốn ${actionText} biến thể "${label}" không?`,
    type: 'info',
    confirmText: 'Xác nhận',
    onConfirm: async () => {
      try {
        if (v.id) {
          try {
            await api.patch(`/api/chi-tiet-san-pham/${v.id}/status`, null, {
              params: { trangThai: newStatus }
            })
          } catch (_) {
            await api.put(`/api/chi-tiet-san-pham/${v.id}`, {
              ...v,
              trangThai: newStatus
            })
          }
        }
        v.trangThai = newStatus
        showToast(newStatus === 1 ? 'Đã mở kinh doanh biến thể!' : 'Đã ngừng kinh doanh biến thể!', 'success')
      } catch (err) {
        console.error('Lỗi đổi trạng thái biến thể:', err)
        showToast('Không thể thay đổi trạng thái biến thể!', 'error')
      }
    }
  })
}

// 2. Lưu / Cập nhật biến thể từ modal
const saveVariantModal = async () => {
  const d = variantModal.value.data
  if (!d.idMauSac) {
    showToast('Vui lòng chọn màu sắc!', 'error')
    return
  }
  if (!d.idKichCo) {
    showToast('Vui lòng chọn kích cỡ!', 'error')
    return
  }
  if (d.soLuong === '' || d.soLuong === null || d.soLuong === undefined) {
    showToast('Vui lòng nhập số lượng!', 'error')
    return
  }
  const sl = Number(d.soLuong)
  if (isNaN(sl) || !Number.isInteger(sl) || sl < 0 || sl > 999999) {
    showToast('Số lượng phải là số nguyên từ 0 đến 999,999!', 'error')
    return
  }
  if (d.giaBan === '' || d.giaBan === null || d.giaBan === undefined) {
    showToast('Vui lòng nhập giá bán!', 'error')
    return
  }
  const gb = Number(d.giaBan)
  if (isNaN(gb) || !Number.isInteger(gb) || gb < 0 || gb > 1000000000) {
    showToast('Giá bán phải là số nguyên từ 0 đến 1,000,000,000 VNĐ!', 'error')
    return
  }

  // Check trùng màu và size nếu là thêm mới hoặc đổi màu/size sang biến thể khác đã có
  const existingDup = variantsList.value.find(item =>
    item.id !== d.id && item.idMauSac === d.idMauSac && item.idKichCo === d.idKichCo
  )
  if (existingDup) {
    showToast(`Biến thể với màu và kích cỡ này đã tồn tại (${existingDup.maChiTietSanPham || 'CTSP'})!`, 'error')
    return
  }

  variantModal.value.saving = true
  try {
    const colorObj = attributes.value.mauSac?.find(c => c.id === d.idMauSac)
    const sizeObj = attributes.value.kichCo?.find(s => s.id === d.idKichCo)

    const payload = {
      id: d.id || undefined,
      idSanPham: productId.value,
      idMauSac: d.idMauSac,
      idKichCo: d.idKichCo,
      maChiTietSanPham: d.maChiTietSanPham?.trim() || undefined,
      soLuong: sl,
      giaBan: gb,
      trangThai: Number(d.trangThai ?? 1),
      anhDaiDien: d.anhDaiDien !== undefined ? d.anhDaiDien : null,
      nguoiThaoTac: 'Admin'
    }

    if (variantModal.value.isEdit && d.id) {
      await api.put(`/api/chi-tiet-san-pham/${d.id}`, payload)
      const idx = variantsList.value.findIndex(item => item.id === d.id)
      if (idx !== -1) {
        variantsList.value[idx] = {
          ...variantsList.value[idx],
          ...payload,
          anhDaiDien: d.anhDaiDien || '',
          tenMauSac: colorObj?.tenMauSac || variantsList.value[idx].tenMauSac,
          maHex: colorObj?.maHex || variantsList.value[idx].maHex,
          tenKichCo: sizeObj?.tenKichCo || variantsList.value[idx].tenKichCo
        }
      }
      showToast('Cập nhật biến thể thành công!', 'success')
    } else {
      const res = await api.post('/api/chi-tiet-san-pham', payload)
      const created = res.data || payload
      variantsList.value.push({
        ...created,
        id: created.id || Date.now(),
        maChiTietSanPham: created.maChiTietSanPham || d.maChiTietSanPham || '',
        idMauSac: d.idMauSac,
        tenMauSac: colorObj?.tenMauSac || '',
        maHex: colorObj?.maHex || '',
        idKichCo: d.idKichCo,
        tenKichCo: sizeObj?.tenKichCo || '',
        soLuong: sl,
        giaBan: gb,
        trangThai: Number(d.trangThai ?? 1),
        anhDaiDien: created.anhDaiDien || d.anhDaiDien || '',
        selected: true
      })
      showToast('Thêm mới biến thể thành công!', 'success')
    }
    closeVariantModal()
  } catch (err) {
    console.error('Lỗi lưu biến thể:', err)
    const msg = err.response?.data?.message || 'Có lỗi xảy ra khi lưu biến thể!'
    showToast(msg, 'error')
  } finally {
    variantModal.value.saving = false
  }
}

// Upload ảnh theo màu
const triggerUpload = (colorId) => {
  if (fileInputs.value[colorId]) {
    fileInputs.value[colorId].click()
  }
}

const onFileChange = (e, colorId) => {
  const file = e.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    const dataUrl = event.target.result
    colorImages.value[colorId] = dataUrl
    // Tự động đồng bộ sang các biến thể cùng màu nếu biến thể đó chưa có ảnh riêng
    variantsList.value.forEach(v => {
      if (v.idMauSac === colorId && !v.anhDaiDien) {
        v.anhDaiDien = dataUrl
      }
    })
    showToast('Tải ảnh theo màu thành công!')
  }
  reader.readAsDataURL(file)
}

const resetColorImage = (colorId) => {
  delete colorImages.value[colorId]
  if (fileInputs.value[colorId]) {
    fileInputs.value[colorId].value = ''
  }
}

const removeColorImage = (colorId) => {
  resetColorImage(colorId)
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

// Thêm nhanh thuộc tính
const handleQuickAdd = (type) => {
  if (type === 'mauSac') {
    router.push('/mau-sac')
  } else if (type === 'kichCo') {
    router.push('/kich-co')
  }
}

// Xác nhận Lưu / Cập nhật sản phẩm
const openSaveConfirm = async () => {
  const ten = form.value.tenSanPham?.trim()
  if (!ten) {
    showToast('Vui lòng nhập tên sản phẩm!', 'error')
    return
  }
  if (ten.length > 255) {
    showToast('Tên sản phẩm không được vượt quá 255 ký tự!', 'error')
    return
  }

  if (form.value.maSanPham?.trim()) {
    const ma = form.value.maSanPham.trim()
    if (ma.length > 50) {
      showToast('Mã sản phẩm không được vượt quá 50 ký tự!', 'error')
      return
    }
    const codeRegex = /^[A-Za-z0-9_-]+$/
    if (!codeRegex.test(ma)) {
      showToast('Mã sản phẩm chỉ được chứa chữ cái, số, gạch dưới và gạch ngang (không dấu cách)!', 'error')
      return
    }
  }

  if (!form.value.idThuongHieu) {
    showToast('Vui lòng chọn hoặc thêm thương hiệu!', 'error')
    return
  }
  if (!form.value.idDanhMuc) {
    showToast('Vui lòng chọn hoặc thêm danh mục!', 'error')
    return
  }
  if (!form.value.idXuatXu) {
    showToast('Vui lòng chọn hoặc thêm xuất xứ!', 'error')
    return
  }
  if (!form.value.idChatLieu) {
    showToast('Vui lòng chọn hoặc thêm chất liệu!', 'error')
    return
  }
  if (!form.value.idCoAo) {
    showToast('Vui lòng chọn hoặc thêm cổ áo!', 'error')
    return
  }
  if (!form.value.idTayAo) {
    showToast('Vui lòng chọn hoặc thêm tay áo!', 'error')
    return
  }
  if (!form.value.idHoaTiet) {
    showToast('Vui lòng chọn hoặc thêm họa tiết!', 'error')
    return
  }

  // Validate các biến thể được chọn nếu là Thêm mới
  if (!isEdit.value) {
    const activeVariants = variantsList.value.filter(v => v.selected !== false)
    for (const v of activeVariants) {
      const label = `${v.tenMauSac || 'Màu'} - ${v.tenKichCo || 'Size'}`
      if (v.soLuong === '' || v.soLuong === null || v.soLuong === undefined) {
        showToast(`Vui lòng nhập số lượng cho biến thể (${label})!`, 'error')
        return
      }
      const sl = Number(v.soLuong)
      if (isNaN(sl) || !Number.isInteger(sl) || sl < 0 || sl > 999999) {
        showToast(`Số lượng biến thể (${label}) phải là số nguyên từ 0 đến 999,999!`, 'error')
        return
      }

      if (v.giaBan === '' || v.giaBan === null || v.giaBan === undefined) {
        showToast(`Vui lòng nhập giá bán cho biến thể (${label})!`, 'error')
        return
      }
      const gb = Number(v.giaBan)
      if (isNaN(gb) || !Number.isInteger(gb) || gb < 0 || gb > 1000000000) {
        showToast(`Giá bán biến thể (${label}) phải là số nguyên từ 0 đến 1,000,000,000 VNĐ!`, 'error')
        return
      }
    }
  }

  // Kiểm tra trùng sản phẩm khi Thêm mới (chỉ coi là trùng khi trùng cả tên và tất cả các thuộc tính)
  if (!isEdit.value) {
    try {
      const checkRes = await api.get('/api/san-pham/check-duplicate', {
        params: {
          maSanPham: form.value.maSanPham?.trim() || undefined,
          tenSanPham: form.value.tenSanPham?.trim(),
          idDanhMuc: form.value.idDanhMuc || undefined,
          idThuongHieu: form.value.idThuongHieu || undefined,
          idChatLieu: form.value.idChatLieu || undefined,
          idCoAo: form.value.idCoAo || undefined,
          idTayAo: form.value.idTayAo || undefined,
          idXuatXu: form.value.idXuatXu || undefined,
          idHoaTiet: form.value.idHoaTiet || undefined
        }
      })

      if (checkRes.data?.conflict) {
        showToast(checkRes.data.message, 'error')
        return
      }

      if (checkRes.data?.exists) {
        openConfirm({
          title: 'Phát hiện sản phẩm đã tồn tại!',
          type: 'warning',
          confirmText: 'Xác nhận gộp',
          message: `Sản phẩm "${checkRes.data.tenSanPham}" (Mã: "${checkRes.data.maSanPham}") đã tồn tại trong hệ thống với cùng tất cả các thuộc tính.\n\nNếu bạn tiếp tục, hệ thống sẽ thực hiện:\n• Tăng số lượng: Cộng dồn số lượng biến thể nhập vào số lượng hiện có.\n• Cập nhật đơn giá: Cập nhật giá bán theo giá mới nhất vừa nhập.\n• Thêm mới các biến thể chưa có.\n\nBạn có chắc chắn muốn gộp vào sản phẩm này không?`,
          onConfirm: () => submitForm(true)
        })
        return
      }
    } catch (checkErr) {
      console.warn('Không thể kiểm tra trùng sản phẩm trước:', checkErr)
    }
  }

  openConfirm({
    title: isEdit.value ? 'Xác nhận cập nhật sản phẩm' : 'Xác nhận lưu sản phẩm',
    message: isEdit.value
      ? `Bạn có chắc chắn muốn lưu các thay đổi cho sản phẩm "${ten}" không?`
      : `Bạn có muốn lưu sản phẩm "${ten}" cùng ${variantsList.value.filter(v => v.selected !== false).length} biến thể không?`,
    type: 'primary',
    confirmText: isEdit.value ? 'Lưu thay đổi' : 'Xác nhận lưu',
    onConfirm: () => submitForm(false)
  })
}

// Thực hiện gọi API lưu sản phẩm
const submitForm = async (xacNhanGop = false) => {
  submitting.value = true
  try {
    const payload = {
      maSanPham: form.value.maSanPham?.trim() || undefined,
      tenSanPham: form.value.tenSanPham.trim(),
      moTa: form.value.moTa || '',
      idXuatXu: form.value.idXuatXu || null,
      idChatLieu: form.value.idChatLieu || null,
      idThuongHieu: form.value.idThuongHieu || null,
      idDanhMuc: form.value.idDanhMuc || null,
      idCoAo: form.value.idCoAo || null,
      idTayAo: form.value.idTayAo || null,
      idHoaTiet: form.value.idHoaTiet || null,
      trangThai: isEdit.value ? (form.value.trangThai ?? 1) : 1,
      xacNhanGop: xacNhanGop === true
    }

    if (isEdit.value) {
      // Đính kèm danh sách ảnh sản phẩm khi cập nhật (gồm cả ảnh chung và ảnh theo màu)
      const allImages = []
      if (productImages.value.length > 0) {
        allImages.push(...productImages.value.map(img => img.url))
      }
      const activeColorIds = new Set(activeColorsForImages.value.map(c => String(c.id)))
      Object.entries(colorImages.value).forEach(([cId, img]) => {
        if (activeColorIds.has(String(cId)) && !!img) {
          allImages.push(`${cId}|${img}`)
        }
      })
      payload.hinhAnhs = allImages
      if (productImages.value.length > 0) {
        const mainImg = productImages.value.find(img => img.isMain) || productImages.value[0]
        payload.anhDaiDien = mainImg.url
      } else if (allImages.length > 0) {
        const firstImg = allImages[0].includes('|') ? allImages[0].split('|')[1] : allImages[0]
        payload.anhDaiDien = firstImg
      }

      await api.put(`/api/san-pham/${productId.value}`, payload)

      // Cập nhật đồng bộ các biến thể nếu có thay đổi từ bảng
      if (variantsList.value && variantsList.value.length > 0) {
        for (const v of variantsList.value) {
          if (v.id) {
            try {
              await api.put(`/api/chi-tiet-san-pham/${v.id}`, {
                id: v.id,
                idSanPham: productId.value,
                idMauSac: v.idMauSac,
                idKichCo: v.idKichCo,
                maChiTietSanPham: v.maChiTietSanPham || undefined,
                soLuong: Number(v.soLuong || 0),
                giaBan: Number(v.giaBan || 0),
                trangThai: Number(v.trangThai ?? 1),
                anhDaiDien: v.anhDaiDien !== undefined ? v.anhDaiDien : undefined,
                nguoiThaoTac: 'Admin'
              })
            } catch (vErr) {
              console.warn('Lỗi lưu biến thể ID ' + v.id, vErr)
            }
          }
        }
      }

      showToast('Cập nhật sản phẩm thành công!')
      setTimeout(() => {
        router.push('/san-pham')
      }, 700)
      return
    }

    // Thu thập danh sách ảnh khi Thêm mới: gồm cả ảnh chung và ảnh theo màu
    const allImages = []
    if (productImages.value.length > 0) {
      allImages.push(...productImages.value.map(img => img.url))
    }
    const activeColorIds = new Set(activeColorsForImages.value.map(c => String(c.id)))
    Object.entries(colorImages.value).forEach(([cId, img]) => {
      if (activeColorIds.has(String(cId)) && !!img) {
        allImages.push(`${cId}|${img}`)
      }
    })
    payload.hinhAnhs = allImages

    if (productImages.value.length > 0) {
      const mainImg = productImages.value.find(img => img.isMain) || productImages.value[0]
      payload.anhDaiDien = mainImg.url
    } else if (allImages.length > 0) {
      const firstImg = allImages[0].includes('|') ? allImages[0].split('|')[1] : allImages[0]
      payload.anhDaiDien = firstImg
    }

    const res = await api.post('/api/san-pham', payload)
    const savedProduct = res.data
    if (xacNhanGop) {
      showToast('Gộp sản phẩm thành công! Đã cộng dồn số lượng và cập nhật giá mới nhất.', 'success')
    } else {
      showToast('Thêm mới sản phẩm thành công!', 'success')
    }

    // Lưu danh sách biến thể với ảnh riêng biệt cho từng biến thể
    const activeVariants = variantsList.value.filter(v => v.selected !== false)
    if (savedProduct && savedProduct.id && activeVariants.length > 0) {
      const variantPayloads = activeVariants.map(v => ({
        id: v.id || undefined,
        idSanPham: savedProduct.id,
        idMauSac: v.idMauSac,
        idKichCo: v.idKichCo,
        soLuong: v.soLuong || 0,
        giaBan: v.giaBan || 0,
        trangThai: 1, // Auto kinh doanh
        anhDaiDien: v.anhDaiDien || colorImages.value[v.idMauSac] || undefined
      }))

      try {
        await api.post('/api/chi-tiet-san-pham/batch', variantPayloads)
      } catch (vErr) {
        console.warn('Lỗi lưu danh sách biến thể qua batch, thử lưu lẻ:', vErr)
        for (const vReq of variantPayloads) {
          try {
            await api.post('/api/chi-tiet-san-pham', vReq)
          } catch (_) {}
        }
      }
    }

    setTimeout(() => {
      router.push('/san-pham')
    }, 1000)
  } catch (err) {
    console.error('Lỗi lưu sản phẩm:', err)
    const msg = err.response?.data?.message || 'Có lỗi xảy ra khi lưu sản phẩm!'
    showToast(msg, 'error')
  } finally {
    submitting.value = false
  }
}

// Quay lại danh sách có xác nhận nếu đã nhập liệu
const handleBack = () => {
  if (form.value.tenSanPham || variantsList.value.length > 0) {
    openConfirm({
      title: 'Xác nhận quay lại',
      message: 'Các thông tin bạn vừa nhập chưa được lưu. Bạn có chắc chắn muốn quay lại danh sách sản phẩm không?',
      type: 'info',
      confirmText: 'Quay lại danh sách',
      onConfirm: () => {
        router.push('/san-pham')
      }
    })
  } else {
    router.push('/san-pham')
  }
}

// Tải thông tin sản phẩm và biến thể nếu là trang Edit
const loadProductDetails = async (id) => {
  try {
    const res = await api.get(`/api/san-pham/${id}`)
    if (res.data) {
      const p = res.data
      const extractId = (val) => {
        if (!val) return ''
        if (typeof val === 'object' && val.id !== undefined && val.id !== null) return val.id
        return val
      }

      form.value = {
        maSanPham: p.maSanPham || '',
        tenSanPham: p.tenSanPham || '',
        idThuongHieu: extractId(p.idThuongHieu),
        idDanhMuc: extractId(p.idDanhMuc),
        idXuatXu: extractId(p.idXuatXu),
        idChatLieu: extractId(p.idChatLieu),
        idCoAo: extractId(p.idCoAo),
        idTayAo: extractId(p.idTayAo),
        idHoaTiet: extractId(p.idHoaTiet),
        trangThai: p.trangThai ?? 1,
        moTa: p.moTa || ''
      }

      // Tải biến thể của sản phẩm
      const varRes = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${id}`)
      if (varRes.data && varRes.data.length > 0) {
        variantsList.value = varRes.data.map(item => {
          const colorObj = attributes.value.mauSac?.find(c => c.id === item.idMauSac)
          const sizeObj = attributes.value.kichCo?.find(s => s.id === item.idKichCo)
          return {
            key: `${item.idMauSac}_${item.idKichCo}`,
            id: item.id,
            maChiTietSanPham: item.maChiTietSanPham || '',
            idMauSac: item.idMauSac,
            tenMauSac: item.tenMauSac || colorObj?.tenMauSac || '',
            maHex: item.maHex || colorObj?.maHex || '',
            idKichCo: item.idKichCo,
            tenKichCo: item.tenKichCo || sizeObj?.tenKichCo || '',
            soLuong: item.soLuong ?? 0,
            giaBan: item.giaBan ?? 0,
            trangThai: item.trangThai ?? 1,
            anhDaiDien: item.anhDaiDien || '',
            selected: true
          }
        })

        // Đổ vào selectedColors và selectedSizes từ các biến thể đã có
        const colorMap = new Map()
        const sizeMap = new Map()
        varRes.data.forEach(v => {
          if (v.idMauSac && !colorMap.has(v.idMauSac)) {
            colorMap.set(v.idMauSac, { id: v.idMauSac, tenMauSac: v.tenMauSac, maHex: v.maHex })
          }
          if (v.idKichCo && !sizeMap.has(v.idKichCo)) {
            sizeMap.set(v.idKichCo, { id: v.idKichCo, tenKichCo: v.tenKichCo })
          }
        })
        selectedColors.value = Array.from(colorMap.values())
        selectedSizes.value = Array.from(sizeMap.values())

        if (p.hinhAnhs && p.hinhAnhs.length > 0) {
          colorImages.value = {}
          p.hinhAnhs.forEach(item => {
            if (item && item.includes('|')) {
              const [cId, url] = item.split('|', 2)
              colorImages.value[cId] = url
            } else if (item && selectedColors.value.length === 1) {
              colorImages.value[selectedColors.value[0].id] = item
            }
          })
        }
      }

      // Nạp danh sách ảnh sản phẩm hiện có
      const loadedImgs = []
      if (p.anhDaiDien) {
        loadedImgs.push({
          url: formatImageUrl(p.anhDaiDien),
          isMain: true
        })
      }
      if (Array.isArray(p.hinhAnhs) && p.hinhAnhs.length > 0) {
        p.hinhAnhs.forEach((item) => {
          let cleanUrl = item
          if (cleanUrl && cleanUrl.includes('|')) {
            cleanUrl = cleanUrl.split('|')[1]
          }
          cleanUrl = formatImageUrl(cleanUrl)
          if (cleanUrl && !loadedImgs.some(img => img.url === cleanUrl)) {
            loadedImgs.push({
              url: cleanUrl,
              isMain: loadedImgs.length === 0
            })
          }
        })
      }
      productImages.value = loadedImgs
    }
  } catch (err) {
    console.error('Lỗi tải thông tin sản phẩm:', err)
    showToast('Lỗi khi tải thông tin sản phẩm!', 'error')
  }
}

// Tải thuộc tính
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

// Lấy mã sản phẩm tự sinh 3 số theo thứ tự (SP001, SP002, ...)
const fetchNextCode = async () => {
  try {
    const res = await api.get('/api/san-pham/next-code')
    if (res.data?.code) {
      form.value.maSanPham = res.data.code
    }
  } catch (e) {
    console.error('Không thể lấy mã sản phẩm tự sinh:', e)
  }
}

// Đặt lại form về mặc định khi Thêm sản phẩm
const resetFormToAdd = async () => {
  form.value = {
    maSanPham: '',
    tenSanPham: '',
    idThuongHieu: '',
    idDanhMuc: '',
    idXuatXu: '',
    idChatLieu: '',
    idCoAo: '',
    idTayAo: '',
    idHoaTiet: '',
    trangThai: 1, // Mặc định Đang kinh doanh
    moTa: ''
  }
  selectedColors.value = []
  selectedSizes.value = []
  variantsList.value = []
  colorImages.value = {}
  productImages.value = []
  pendingFiles.value = []
  bulkQty.value = 0
  bulkPrice.value = 0
  // Ô mã sản phẩm để trống theo yêu cầu người dùng (nếu để trống backend tự sinh khi lưu)
}

onMounted(async () => {
  document.addEventListener('click', closeAllDropdowns)
  await fetchAttributes()
  if (isEdit.value && productId.value) {
    await loadProductDetails(productId.value)
  } else {
    await resetFormToAdd()
  }
})

// Tự động chuyển đổi hoặc đặt lại form khi điều hướng giữa Thêm và Sửa
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await loadProductDetails(newId)
  } else {
    await resetFormToAdd()
  }
})
</script>

<style scoped>
.product-form-container {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header bar */
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
}

.btn-back-top {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #ffffff;
  color: #475569;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  height: 38px;
  padding: 0 16px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.btn-back-top:hover {
  background-color: #f7f5ee;
  border-color: #496883;
  color: #496883;
  transform: translateY(-1px);
}

/* Card container */
.form-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem 1.75rem;
  border: 1px solid var(--line, #ede8dc);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-box-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.85rem;
  margin-bottom: 1.25rem;
  border-bottom: 1px solid #ebd9c8;
}

.card-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon-box {
  display: flex;
  align-items: center;
  color: var(--blue, #496883);
}

.card-title-text {
  font-size: 1.05rem;
  font-weight: 700;
  color: #2b353b;
  margin: 0;
}

/* Form items & Grid */
.form-grid-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem 1.5rem;
}

@media (max-width: 768px) {
  .form-grid-layout {
    grid-template-columns: 1fr;
  }
}

.full-width-col {
  grid-column: 1 / -1;
}

.input-disabled {
  background-color: #f5f4ef !important;
  color: #71828d !important;
  cursor: not-allowed !important;
  border-color: #e5e0d3 !important;
}

.help-subtext {
  font-size: 0.8rem;
  color: #8c8275;
  margin-top: 4px;
}

.tag-auto {
  font-size: 0.78rem;
  color: #647b8c;
  font-weight: normal;
  margin-left: 0.35rem;
}

/* Styled select */
.select-styled-wrap {
  width: 100%;
}

.styled-select {
  height: 38px;
  padding: 0 0.85rem;
  background-color: #ffffff;
  cursor: pointer;
}

/* Card bottom actions (Lưu thay đổi / Hủy) */
.card-bottom-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1rem;
}

.btn-save-action {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  height: 38px;
  padding: 0 1.5rem;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
  transition: all 0.2s ease;
}

.btn-save-action:hover:not(:disabled) {
  background-color: #385269;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.35);
  transform: translateY(-1px);
}

.btn-save-action:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-cancel-action {
  background-color: #ffffff;
  color: #475569;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  height: 38px;
  padding: 0 1.4rem;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
}

.btn-cancel-action:hover {
  background-color: #f7f5ee;
  border-color: #496883;
  color: #496883;
}

/* =========================================
   HÌNH ẢNH SẢN PHẨM (CARD STYLE THEO MẪU)
   ========================================= */
.title-with-sub {
  display: flex;
  flex-direction: column;
}

.card-subtitle-text {
  font-size: 0.83rem;
  color: #64748b;
  margin: 3px 0 0 0;
  font-weight: 400;
}

.product-images-manager {
  display: flex;
  flex-direction: column;
}

.upload-trigger-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn-choose-files {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background-color: #ffffff;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  height: 38px;
  padding: 0 16px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.btn-choose-files:hover {
  background-color: #f7f5ee;
  border-color: #496883;
  color: #496883;
}

.upload-limits-hint {
  font-size: 0.82rem;
  color: #94a3b8;
}

.images-section-divider {
  border-top: 1px dashed #ede8dc;
  margin: 1.15rem 0;
}

.staged-action-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}

.btn-add-staged {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 7px;
  height: 36px;
  padding: 0 16px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(73, 104, 131, 0.2);
}

.btn-add-staged:hover:not(:disabled) {
  background-color: #385269;
}

.btn-add-staged:disabled {
  background-color: #94a3b8;
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

.staged-hint-text {
  font-size: 0.82rem;
  color: #7f8c8d;
}

.product-images-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 140px));
  gap: 1rem;
}

.product-image-card-item {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s;
}

.product-image-card-item:hover {
  border-color: #cbd5e1;
}

.image-thumb-box {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  border: 1px solid #f1f5f9;
}

.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-card-meta {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.badge-main-image {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
  border-radius: 9999px;
  padding: 2px 10px;
  font-size: 0.72rem;
  font-weight: 600;
}

.btn-set-main {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #475569;
  border-radius: 9999px;
  padding: 2px 8px;
  font-size: 0.72rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-set-main:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-remove-image {
  background: transparent;
  border: none;
  color: #e11d48;
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  padding: 2px 6px;
  transition: color 0.15s;
}

.btn-remove-image:hover {
  color: #be123c;
  text-decoration: underline;
}

.empty-product-images {
  padding: 2.5rem 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #faf9f6;
  border: 1.5px dashed #e2ded5;
  border-radius: 8px;
  color: #71828d;
  text-align: center;
}

.empty-icon-photo {
  color: #a0aec0;
  margin-bottom: 4px;
}

.empty-images-text {
  font-size: 0.9rem;
  font-weight: 600;
  color: #475569;
}

.empty-images-sub {
  font-size: 0.8rem;
  color: #94a3b8;
}

.mt-3 {
  margin-top: 1rem;
}

.mt-4 {
  margin-top: 1.5rem;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-label {
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
}

.text-danger {
  color: #e11d48;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  font-size: 0.9rem;
  color: #1e293b;
  background-color: #ffffff;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

/* Select with Add Button */
.select-with-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.select-with-btn :deep(.select2-container) {
  flex: 1;
  min-width: 0;
  width: auto;
}

.select-with-btn .form-select {
  flex: 1;
  min-width: 0;
}

.btn-add-attr-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  min-width: 36px;
  padding: 0;
  color: #ffffff;
  background-color: var(--blue, #496883);
  border: 1px solid var(--blue, #496883);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(73, 104, 131, 0.18);
}

.btn-add-attr-inline:hover {
  background-color: #385166;
  border-color: #385166;
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(73, 104, 131, 0.28);
}

.btn-add-attr-inline:active {
  transform: translateY(0);
  background-color: #2c4153;
}

/* Quick Attr Modal Card - Đồng bộ thiết kế theo AttributeManager.vue */
.quick-attr-modal-card {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  animation: modalScaleUp 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.quick-attr-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #efeae0;
}

.quick-attr-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: #3c4d55;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #888;
  cursor: pointer;
  padding: 0 0.25rem;
  transition: color 0.2s;
}

.btn-close-modal:hover {
  color: #000;
}

.quick-attr-modal-body {
  padding: 1.5rem;
}

.quick-attr-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.quick-attr-field .form-label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #3d4a50;
}

.quick-attr-field .form-label .required {
  color: #d9534f;
}

.quick-attr-field .form-control {
  height: 2.45rem;
  border: 1px solid #d5d9df;
  border-radius: 6px;
  padding: 0 0.8rem;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s;
  background-color: #ffffff;
  color: #1e293b;
}

.quick-attr-field .form-control:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.1);
}

.quick-attr-error-banner {
  background-color: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
  padding: 0.6rem 0.85rem;
  border-radius: 8px;
  font-size: 0.85rem;
}

.quick-attr-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #efeae0;
  background-color: #faf8f5;
}

.quick-attr-modal-footer .btn-secondary {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
  height: 38px;
  padding: 0 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-attr-modal-footer .btn-secondary:hover {
  background-color: #faeed7;
}

.quick-attr-modal-footer .btn-primary {
  background-color: #3b596a;
  border: 1px solid #324b5a;
  color: #ffffff;
  height: 38px;
  padding: 0 18px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-attr-modal-footer .btn-primary:hover:not(:disabled) {
  background-color: #2c4350;
  border-color: #243742;
}

.form-textarea {
  resize: vertical;
}

/* Card 2: Attribute pickers */
.attribute-picker-row {
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
}

.picker-label-col {
  width: 100px;
  padding-top: 0.6rem;
}

.picker-input-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.picker-action-col {
  width: 120px;
  padding-top: 0.2rem;
}

/* Color picker input group & Palette action */
.color-picker-input-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.color-palette-action-wrapper {
  position: relative;
  flex-shrink: 0;
}

.palette-btn-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.native-color-picker-box {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
}

.native-color-picker-box:hover {
  border-color: #496883;
  transform: translateY(-1px);
}

.native-color-input {
  position: absolute;
  top: -10px;
  left: -10px;
  width: 60px;
  height: 60px;
  opacity: 0;
  cursor: pointer;
}

.color-preview-disc {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.15);
  pointer-events: none;
}

.btn-palette-open {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  height: 38px;
  padding: 0 0.85rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #496883;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-palette-open:hover,
.btn-palette-open.active {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}

/* Palette Popover */
.color-palette-popover {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 320px;
  background-color: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  z-index: 120;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.palette-popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f1f5f9;
}

.palette-header-title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: #1e293b;
}

.palette-selected-count {
  font-size: 0.75rem;
  color: #64748b;
  background: #f1f5f9;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-weight: 500;
}

.palette-section-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin-bottom: 0.4rem;
}

.palette-swatches-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.45rem;
  max-height: 160px;
  overflow-y: auto;
  padding: 2px;
}

.palette-swatch-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  padding: 0;
}

.palette-swatch-btn:hover {
  transform: scale(1.15);
  box-shadow: 0 0 0 2px #496883;
}

.palette-swatch-btn.selected {
  box-shadow: 0 0 0 2px #496883, 0 2px 5px rgba(0, 0, 0, 0.2);
  transform: scale(1.1);
}

.swatch-check {
  color: #ffffff;
  font-weight: 800;
  font-size: 0.85rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.palette-custom-section {
  padding-top: 0.6rem;
  border-top: 1px solid #f1f5f9;
}

.custom-color-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.custom-color-input {
  width: 34px;
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  cursor: pointer;
  padding: 0;
  background: none;
}

.custom-hex-text {
  width: 75px;
  height: 34px;
  font-size: 0.78rem;
  font-family: monospace;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0 0.35rem;
  text-transform: uppercase;
}

.custom-name-text {
  flex: 1;
  min-width: 0;
  height: 34px;
  font-size: 0.8rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0 0.5rem;
}

.btn-add-custom-color {
  height: 34px;
  padding: 0 0.65rem;
  font-size: 0.78rem;
  font-weight: 600;
  background-color: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.btn-add-custom-color:hover:not(:disabled) {
  background-color: #385166;
}

.btn-add-custom-color:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.multiselect-dropdown-wrap {
  position: relative;
  width: 100%;
}

.dropdown-trigger {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 0.85rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background-color: #ffffff;
  cursor: pointer;
  user-select: none;
  font-size: 0.9rem;
}

.trigger-text {
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trigger-arrow {
  color: #94a3b8;
  font-size: 0.9rem;
}

.multiselect-popover {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: 280px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.popover-search {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #f1f5f9;
  background: #fafafa;
}

.popover-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.88rem;
  width: 60%;
}

.btn-clear-all {
  border: none;
  background: none;
  color: #94a3b8;
  font-size: 0.8rem;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-clear-all:hover {
  color: #dc2626;
}

.popover-list {
  overflow-y: auto;
  padding: 0.25rem 0;
}

.popover-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.85rem;
  cursor: pointer;
  font-size: 0.88rem;
  transition: background-color 0.15s;
}

.popover-item:hover {
  background-color: #f8fafc;
}

.popover-item.selected {
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  font-weight: 600;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.15);
}

.check-icon {
  margin-left: auto;
  color: var(--blue, #496883);
  font-weight: 700;
}

.selected-tags-box {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.attr-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0.3rem 0.65rem;
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 0.82rem;
  color: #334155;
}

.color-dot-sm {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.chip-remove {
  font-size: 1.1rem;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
}

.chip-remove:hover {
  color: #dc2626;
}

.btn-quick-add {
  background: #ffffff;
  color: var(--blue, #496883);
  border: 1px solid #cbd5e1;
  padding: 0.6rem 0.8rem;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.btn-quick-add:hover {
  background: #eaf2f6;
  border-color: #6e96ad;
}

.generate-btn-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 1.25rem;
}

.btn-generate {
  background-color: #3b596a;
  border: 1px solid #324b5a;
  color: #ffffff;
  padding: 0.7rem 1.6rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
  transition: all 0.2s;
}

.btn-generate:hover {
  background-color: #2c4350;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
  transform: translateY(-1px);
}

/* Card 3: Bulk apply & Variants table */
.bulk-apply-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  padding-bottom: 1.2rem;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 1.2rem;
}

.bulk-select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
}

.bulk-inputs-group {
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  flex-wrap: wrap;
}

.bulk-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bulk-field label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
}

.bulk-input {
  width: 160px;
  padding: 0.5rem 0.75rem;
}

.btn-bulk-apply {
  background-color: #3b596a;
  color: #ffffff;
  border: 1px solid #324b5a;
  padding: 0.55rem 1.1rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-bulk-apply:hover {
  background-color: #2c4350;
  border-color: #243742;
}

.color-variant-group-card {
  border: 1px solid #edf1f4;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 1.25rem;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.8rem 1rem;
  background-color: #f8fafc;
  border-bottom: 1px solid #edf1f4;
}

.group-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-dot-md {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.15);
}

.color-group-title {
  font-size: 0.95rem;
  color: #1e293b;
}


.size-summary {
  font-size: 0.82rem;
  color: #64748b;
}

.variant-data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.variant-data-table th {
  background: #faf8f5;
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 700;
  text-transform: uppercase;
  color: #4a5568;
  border-bottom: 2px solid #e9e5db;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
}

.variant-data-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  vertical-align: middle;
  color: #4b585e;
}

.cell-stt {
  color: #9aa0a0;
  font-weight: 500;
}

.cell-size {
  color: #334155;
}

.cell-input {
  max-width: 220px;
  padding: 0.45rem 0.75rem;
}

.btn-delete-row {
  border: none;
  background: #fee2e2;
  color: #dc2626;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-delete-row:hover {
  background: #dc2626;
  color: #ffffff;
}

.btn-apply-color-img {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 0.25rem 0.65rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  margin-right: 0.75rem;
  transition: all 0.15s ease;
}

.btn-apply-color-img:hover {
  background-color: #e2e8f0;
  border-color: #94a3b8;
  color: #0f172a;
}

.cell-variant-img {
  padding: 0.45rem 0.5rem !important;
}

.variant-img-uploader {
  position: relative;
  width: 44px;
  height: 44px;
  margin: 0 auto;
  border-radius: 8px;
  border: 1.5px dashed #cbd5e1;
  background-color: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s ease;
}

.variant-img-uploader:hover {
  border-color: var(--blue, #496883);
  background-color: #f1f5f9;
  transform: translateY(-1px);
  box-shadow: 0 2px 5px rgba(0,0,0,0.06);
}

.variant-table-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.variant-img-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #94a3b8;
  font-size: 0.65rem;
  font-weight: 600;
  user-select: none;
}

.btn-remove-var-img {
  position: absolute;
  top: 1px;
  right: 1px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(220, 38, 38, 0.9);
  color: #ffffff;
  border: none;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: transform 0.15s ease;
}

.btn-remove-var-img:hover {
  transform: scale(1.15);
  background: #dc2626;
}

/* Card 4: Color Images */
.images-header-wrap {
  margin-bottom: 1rem;
}

.card-section-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
}

.card-section-subtitle {
  margin: 4px 0 0;
  font-size: 0.82rem;
  color: #64748b;
}

.color-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.25rem;
}

.color-image-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 1rem;
  background: #fafbfc;
}

.image-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.85rem;
}

.color-img-title {
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
}

.image-card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-reset-img {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  padding: 0.35rem 0.5rem;
  cursor: pointer;
  font-size: 0.8rem;
}

.btn-add-img {
  border: 1px solid #324b5a;
  background: #3b596a;
  color: #ffffff;
  border-radius: 6px;
  padding: 0.4rem 0.8rem;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-img:hover {
  background: #2c4350;
  border-color: #243742;
}

.image-dropzone {
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
  overflow: hidden;
  position: relative;
}

.image-dropzone:hover {
  border-color: var(--blue, #496883);
}

.image-placeholder {
  text-align: center;
  padding: 1rem;
}

.placeholder-icon {
  font-size: 2rem;
  margin-bottom: 0.3rem;
}

.placeholder-text {
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
}

.placeholder-sub {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 2px;
}

.image-preview-wrap {
  width: 100%;
  height: 160px;
  position: relative;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.btn-remove-preview {
  position: absolute;
  top: 8px;
  right: 8px;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 0.8rem;
}

/* Footer buttons */
.form-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn-cancel-lg {
  border: 1px solid #dfd5c2;
  background: #fff8eb;
  color: #957b48;
  padding: 0.75rem 2rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-lg:hover {
  background: #faeed7;
}

.btn-save-lg {
  background-color: #3b596a;
  border: 1px solid #324b5a;
  color: #ffffff;
  padding: 0.75rem 2.2rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
  transition: all 0.2s;
}

.btn-save-lg:hover:not(:disabled) {
  background-color: #2c4350;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
  transform: translateY(-1px);
}

.btn-save-lg:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Toast */
.toast-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  z-index: 1000;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
}

.toast-notification.success {
  background-color: #10b981;
  color: #ffffff;
}

.toast-notification.error {
  background-color: #ef4444;
  color: #ffffff;
}

/* Modal xác nhận chuẩn theo ảnh PhieuGiamGia */
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(18, 25, 33, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: popIn 0.2s ease-out;
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
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
  white-space: pre-line;
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
  transition: all 0.15s;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #f7f5ef;
  color: #2b383e;
  border-color: #c5bfae;
}

.btn-save-confirm {
  background-color: #b38e6e;
  color: #ffffff;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 6px rgba(179, 142, 110, 0.25);
}

.btn-save-confirm:hover:not(:disabled) {
  background-color: #9c7857;
}

/* =========================================================
   CỤM NÚT THAO TÁC BIẾN THỂ (Khớp 1:1 theo ảnh mẫu: Power + Eye)
   ========================================================= */
.action-buttons-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 3px;
  background-color: #f7f5ee;
  border: 1px solid #e7e2d6;
  border-radius: 8px;
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

.btn-action.btn-status {
  color: #dc2626;
  background-color: #fff1f2;
  border-color: #ffe4e6;
}

.btn-action.btn-status.is-active {
  color: #059669;
  background-color: #ecfdf5;
  border-color: #a7f3d0;
}

.btn-action.btn-status:hover {
  transform: translateY(-1px);
  filter: brightness(0.95);
}

.btn-action.btn-view {
  color: #496883;
  background-color: #ffffff;
  border: 1px solid #e7e2d6;
}

.btn-action.btn-view:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: translateY(-1px);
}

.btn-action.btn-edit {
  color: #496883;
  background-color: #ffffff;
  border: 1px solid #e7e2d6;
}

.btn-action.btn-edit:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  transform: translateY(-1px);
}

.detail-label-text {
  font-size: 0.82rem;
  color: #64748b;
  font-weight: 500;
}

.detail-val-text {
  font-size: 0.92rem;
  color: #1e293b;
}

/* =========================================================
   GIAO DIỆN KHỐI BIẾN THỂ SẢN PHẨM (Card 3: Chế độ Sửa)
   ========================================================= */
.card-title-badge-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.variant-count-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  font-size: 0.78rem;
  font-weight: 700;
  color: #496883;
  background-color: #eaf2f8;
  border-radius: 20px;
  border: 1px solid #d4e2ee;
}

.btn-add-variant-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #496883;
  color: #ffffff;
  border: 1px solid #496883;
  border-radius: 8px;
  padding: 7px 15px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.btn-add-variant-btn:hover {
  background-color: #385269;
  border-color: #385269;
  transform: translateY(-1px);
}

.edit-variants-table-wrap {
  border: 1px solid #ede8dc;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  margin-top: 0.5rem;
}

.edit-variants-table {
  width: 100%;
  border-collapse: collapse;
}

.edit-variants-table th {
  background-color: #f7f5ef;
  color: #496883;
  font-weight: 700;
  font-size: 0.84rem;
  padding: 10px 12px;
  border-bottom: 2px solid #ede8dc;
  text-transform: none;
}

.edit-variant-row td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1ece4;
  vertical-align: middle;
  font-size: 0.88rem;
}

.edit-variant-row:last-child td {
  border-bottom: none;
}

.edit-variant-row:hover td {
  background-color: #fdfcf9;
}

.ctsp-code-badge {
  font-family: monospace;
  font-size: 0.85rem;
  font-weight: 700;
  color: #8c6a46;
  background-color: #fbf8f3;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid #ebd9c8;
  display: inline-block;
}

.color-badge-cell {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.color-circle-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  display: inline-block;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.color-name-text {
  font-weight: 600;
  color: #2c3e50;
  font-size: 0.88rem;
}

.size-pill-tag {
  display: inline-block;
  padding: 2px 10px;
  background-color: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.82rem;
}

.variant-inline-input {
  max-width: 140px;
  padding: 6px 8px;
  font-size: 0.88rem;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  transition: all 0.2s;
}

.variant-inline-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.15);
  outline: none;
}

.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.8rem;
  white-space: nowrap;
}

.status-badge.status-active {
  background-color: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.status-badge.status-inactive {
  background-color: #fff1f2;
  color: #dc2626;
  border: 1px solid #fecdd3;
}

.empty-edit-variants {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2.5rem 1rem;
  background-color: #faf8f3;
  border: 1px dashed #ded8cb;
  border-radius: 8px;
  margin-top: 0.5rem;
}

.empty-icon-box {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.empty-text {
  font-size: 0.92rem;
  color: #64748b;
  font-weight: 500;
}

/* =========================================================
   MODAL CHỈNH SỬA / THÊM MỚI BIẾN THỂ (variantModal)
   ========================================================= */
.variant-modal-card {
  background: #ffffff;
  border-radius: 12px;
  width: 540px;
  max-width: 95vw;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: modalScaleUp 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.variant-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.1rem 1.4rem;
  background-color: #fdfcf9;
  border-bottom: 1px solid #ede8dc;
}

.variant-modal-title-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.variant-modal-badge {
  font-size: 0.76rem;
  font-weight: 700;
  padding: 2px 8px;
  background-color: #eaf2f8;
  color: #496883;
  border-radius: 6px;
}

.variant-modal-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.variant-modal-body {
  padding: 1.25rem 1.4rem;
}

.variant-modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.variant-modal-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.status-radio-group {
  display: flex;
  gap: 14px;
  margin-top: 4px;
}

.status-radio-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 0.86rem;
}

.status-radio-label.is-active {
  color: #059669;
  font-weight: 600;
}

.status-radio-label.is-inactive {
  color: #dc2626;
  font-weight: 600;
}

.variant-modal-field.full-width-col {
  grid-column: span 2;
}

.variant-img-upload-box {
  display: flex;
  align-items: center;
  gap: 16px;
  background-color: #faf8f5;
  border: 1px dashed #d5c8b8;
  border-radius: 8px;
  padding: 12px 14px;
}

.variant-img-thumb-preview {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  background-color: #fff;
  border: 1px solid #e2dcd0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.variant-img-thumb-preview:hover {
  border-color: #496883;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.15);
}

.variant-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.variant-thumb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 0.72rem;
}

.variant-img-meta-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.variant-img-btn-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-choose-variant-img {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background-color: #496883;
  color: #ffffff;
  border: 1px solid #496883;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-choose-variant-img:hover {
  background-color: #385269;
}

.btn-remove-variant-img {
  padding: 6px 12px;
  background-color: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-remove-variant-img:hover {
  background-color: #fee2e2;
}

.variant-img-hint-text {
  font-size: 0.74rem;
  color: #8c827a;
}

.view-variant-img-preview {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #d5c8b8;
  overflow: hidden;
  background-color: #fff;
}

.view-variant-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.variant-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 1rem 1.4rem;
  background-color: #fdfcf9;
  border-top: 1px solid #ede8dc;
}

.btn-modal-cancel {
  background-color: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.55rem 1.2rem;
  border-radius: 7px;
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-cancel:hover {
  background-color: #e2e8f0;
}

.btn-modal-submit {
  background-color: #496883;
  color: #ffffff;
  border: 1px solid #496883;
  padding: 0.55rem 1.4rem;
  border-radius: 7px;
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(73, 104, 131, 0.2);
}

.btn-modal-submit:hover {
  background-color: #385269;
  border-color: #385269;
}

.btn-modal-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .product-form-container {
    padding: 1rem 0.75rem 2rem;
  }
  .grid-2-cols {
    grid-template-columns: 1fr;
    gap: 0.85rem;
  }
  .attribute-picker-row {
    flex-direction: column;
    gap: 0.75rem;
  }
  .picker-label-col,
  .picker-action-col {
    width: 100%;
  }
  .bulk-inputs-group {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }
  .bulk-input {
    width: 100%;
  }
  .color-images-grid {
    grid-template-columns: 1fr;
  }
  .form-footer-actions {
    flex-direction: column;
  }
  .btn-cancel-lg,
  .btn-save-lg {
    width: 100%;
  }
}
</style>
