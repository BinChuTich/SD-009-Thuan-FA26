<template>
  <div class="product-form-container">
    <!-- 1. Thanh tiêu đề & Điều hướng -->
    <div class="form-header-bar">
      <div class="breadcrumb-group">
        <span class="breadcrumb-icon">☰</span>
        <span class="breadcrumb-root" @click="handleBack">Sản phẩm</span>
        <span class="breadcrumb-sep">/</span>
        <span class="breadcrumb-current">{{ isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm' }}</span>
      </div>

      <button type="button" class="btn btn-back-top" @click="handleBack">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" style="display: inline-block; vertical-align: middle; margin-right: 4px;">
          <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
        Quay lại danh sách
      </button>
    </div>

    <!-- Thông báo Toast -->
    <transition name="fade">
      <div v-if="toast.show" :class="['toast-notification', toast.type]">
        {{ toast.message }}
      </div>
    </transition>

    <form @submit.prevent="openSaveConfirm" class="product-form-body">
      <!-- 2. Khối 1: Thông tin sản phẩm -->
      <div class="form-card">
        <div class="grid-2-cols">
          <!-- Mã sản phẩm -->
          <div class="form-item">
            <label class="item-label">Mã sản phẩm</label>
            <input
                type="text"
                v-model="form.maSanPham"
                class="form-input"
                placeholder="Nhập mã hoặc để trống hệ thống tự sinh..."
            />
          </div>

          <!-- Tên sản phẩm -->
          <div class="form-item">
            <label class="item-label">Sản phẩm <span class="text-danger">*</span></label>
            <input
                type="text"
                v-model="form.tenSanPham"
                class="form-input"
                placeholder="Nhập tên sản phẩm..."
                required
            />
          </div>
        </div>

        <div class="grid-2-cols mt-3">
          <!-- Thương hiệu -->
          <div class="form-item">
            <label class="item-label">Thương hiệu <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idThuongHieu" class="form-select" required>
                <option value="">-- Chọn thương hiệu --</option>
                <option v-for="item in attributes.thuongHieu" :key="item.id" :value="item.id">
                  {{ item.tenThuongHieu }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('thuong-hieu')" title="Thêm thương hiệu mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>

          <!-- Danh mục -->
          <div class="form-item">
            <label class="item-label">Danh mục <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idDanhMuc" class="form-select" required>
                <option value="">-- Chọn danh mục --</option>
                <option v-for="item in attributes.danhMuc" :key="item.id" :value="item.id">
                  {{ item.tenDanhMuc }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('danh-muc')" title="Thêm danh mục mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>
        </div>

        <div class="grid-2-cols mt-3">
          <!-- Xuất xứ -->
          <div class="form-item">
            <label class="item-label">Xuất xứ <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idXuatXu" class="form-select" required>
                <option value="">-- Chọn xuất xứ --</option>
                <option v-for="item in attributes.xuatXu" :key="item.id" :value="item.id">
                  {{ item.tenXuatXu }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('xuat-xu')" title="Thêm xuất xứ mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>

          <!-- Chất liệu -->
          <div class="form-item">
            <label class="item-label">Chất liệu <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idChatLieu" class="form-select" required>
                <option value="">-- Chọn chất liệu --</option>
                <option v-for="item in attributes.chatLieu" :key="item.id" :value="item.id">
                  {{ item.tenChatLieu }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('chat-lieu')" title="Thêm chất liệu mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>
        </div>

        <div class="grid-2-cols mt-3">
          <!-- Cổ áo -->
          <div class="form-item">
            <label class="item-label">Cổ áo <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idCoAo" class="form-select" required>
                <option value="">-- Chọn cổ áo --</option>
                <option v-for="item in attributes.coAo" :key="item.id" :value="item.id">
                  {{ item.tenCoAo }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('co-ao')" title="Thêm cổ áo mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>

          <!-- Tay áo -->
          <div class="form-item">
            <label class="item-label">Tay áo <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idTayAo" class="form-select" required>
                <option value="">-- Chọn tay áo --</option>
                <option v-for="item in attributes.tayAo" :key="item.id" :value="item.id">
                  {{ item.tenTayAo }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('tay-ao')" title="Thêm tay áo mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>
        </div>

        <div class="grid-2-cols mt-3">
          <!-- Họa tiết -->
          <div class="form-item">
            <label class="item-label">Họa tiết <span class="text-danger">*</span></label>
            <div class="select-with-btn">
              <select v-model="form.idHoaTiet" class="form-select" required>
                <option value="">-- Chọn họa tiết --</option>
                <option v-for="item in attributes.hoaTiet" :key="item.id" :value="item.id">
                  {{ item.tenHoaTiet }}
                </option>
              </select>
              <button type="button" class="btn btn-add-attr-inline" @click="openQuickAddModal('hoa-tiet')" title="Thêm họa tiết mới">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
                <span>Thêm mới</span>
              </button>
            </div>
          </div>

          <!-- Trạng thái -->
          <div class="form-item">
            <label class="item-label">Trạng thái <span class="text-danger">*</span></label>
            <select v-model="form.trangThai" class="form-select" required>
              <option :value="1">Đang kinh doanh</option>
              <option :value="0">Ngừng kinh doanh</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 3. Khối 2: Chọn Màu sắc & Kích cỡ để sinh biến thể tự động -->
      <div class="form-card mt-3">
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
            <div class="multiselect-dropdown-wrap">
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

      <!-- 4. Khối 3: Bảng danh sách biến thể theo màu sắc -->
      <div v-if="groupedVariants.length > 0" class="form-card mt-3">
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
                  type="number"
                  min="0"
                  step="1000"
                  v-model.number="bulkPrice"
                  class="form-input bulk-input"
                  placeholder="0"
              />
            </div>

            <button type="button" class="btn btn-bulk-apply" @click="applyBulkValues">
              Áp dụng
            </button>
          </div>
        </div>

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
              <span class="size-summary">{{ group.sizesSummary }}</span>
              <button
                  type="button"
                  class="btn-delete-group"
                  @click="openDeleteColorGroupConfirm(group)"
                  title="Xóa toàn bộ biến thể màu này"
              >
                ✕ Xóa màu {{ group.color.tenMauSac }}
              </button>
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
                <th style="width: 130px;">Kích cỡ</th>
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
                      type="number"
                      min="0"
                      step="1000"
                      v-model.number="vItem.giaBan"
                      class="form-input cell-input"
                      placeholder="0"
                      required
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

      <!-- 5. Khối 4: Ảnh sản phẩm chi tiết theo màu sắc -->
      <div v-if="activeColorsForImages.length > 0" class="form-card mt-3 color-images-container">
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

      <!-- 6. Khối 5: Mô tả sản phẩm -->
      <div class="form-card mt-3">
        <label class="item-label">Mô tả</label>
        <textarea
            v-model="form.moTa"
            rows="4"
            class="form-textarea"
            placeholder="Mô tả sản phẩm..."
        ></textarea>
      </div>

      <!-- 7. Chân trang Action buttons -->
      <div class="form-footer-actions mt-4">
        <button type="button" class="btn btn-cancel-lg" @click="handleBack">
          Hủy
        </button>
        <button
            type="submit"
            class="btn btn-save-lg"
            :disabled="submitting"
        >
          {{ submitting ? 'Đang lưu...' : (isEdit ? 'Cập nhật sản phẩm' : 'Lưu sản phẩm') }}
        </button>
      </div>
    </form>

    <!-- 8. Modal Xác nhận hành động (Xác nhận Lưu / Cập nhật / Xóa) -->
    <transition name="fade">
      <div v-if="confirmDialog.show" class="confirm-modal-overlay" @click="closeConfirmDialog">
        <div class="confirm-modal-box" @click.stop>
          <div class="confirm-icon-wrap" :class="confirmDialog.type">
            <span v-if="confirmDialog.type === 'danger'">⚠️</span>
            <span v-else-if="confirmDialog.type === 'info'">ℹ️</span>
            <span v-else>❓</span>
          </div>

          <h3 class="confirm-title">{{ confirmDialog.title }}</h3>
          <p class="confirm-message">{{ confirmDialog.message }}</p>

          <div class="confirm-actions">
            <button type="button" class="btn btn-confirm-cancel" @click="closeConfirmDialog">
              Hủy bỏ
            </button>
            <button
                type="button"
                :class="['btn', confirmDialog.type === 'danger' ? 'btn-confirm-danger' : 'btn-confirm-primary']"
                @click="executeConfirmedAction"
            >
              {{ confirmDialog.confirmText || 'Đồng ý' }}
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
            <div class="quick-attr-title-box">
              <span class="quick-attr-badge">✨ Thuộc tính</span>
              <h3 class="quick-attr-title">Thêm mới {{ quickAttrModal.title }}</h3>
            </div>
            <button type="button" class="btn-close-modal" @click="closeQuickAttrModal">&times;</button>
          </div>

          <div class="quick-attr-modal-body">
            <div class="quick-attr-field">
              <label class="item-label">Tên {{ quickAttrModal.title.toLowerCase() }} <span class="text-danger">*</span></label>
              <input
                  type="text"
                  v-model="quickAttrModal.ten"
                  class="form-input"
                  :placeholder="`Nhập tên ${quickAttrModal.title.toLowerCase()}...`"
                  @keyup.enter="submitQuickAttr"
                  autofocus
                  maxlength="255"
              />
            </div>

            <div class="quick-attr-field mt-3">
              <label class="item-label">Mã {{ quickAttrModal.title.toLowerCase() }} <span class="text-muted">(Tùy chọn)</span></label>
              <input
                  type="text"
                  v-model="quickAttrModal.ma"
                  class="form-input"
                  :placeholder="`Mã ${quickAttrModal.title.toLowerCase()} (VD: ${quickAttrModal.codePrefix}01)...`"
                  maxlength="50"
              />
            </div>

            <div v-if="quickAttrModal.error" class="quick-attr-error-banner mt-3">
              {{ quickAttrModal.error }}
            </div>
          </div>

          <div class="quick-attr-modal-footer">
            <button type="button" class="btn btn-confirm-cancel" @click="closeQuickAttrModal" :disabled="quickAttrModal.loading">
              Hủy bỏ
            </button>
            <button
                type="button"
                class="btn btn-confirm-primary"
                @click="submitQuickAttr"
                :disabled="quickAttrModal.loading"
            >
              {{ quickAttrModal.loading ? 'Đang lưu...' : 'Lưu và Chọn' }}
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

// Biến thể đã sinh
const variantsList = ref([])
const selectAllVariants = ref(true)
const bulkQty = ref(0)
const bulkPrice = ref(0)
const submitting = ref(false)

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

// Modal Thêm nhanh 7 loại thuộc tính
const attrTypeConfigs = {
  'thuong-hieu': { title: 'Thương hiệu', field: 'idThuongHieu', listKey: 'thuongHieu', codePrefix: 'TH' },
  'danh-muc': { title: 'Danh mục', field: 'idDanhMuc', listKey: 'danhMuc', codePrefix: 'DM' },
  'xuat-xu': { title: 'Xuất xứ', field: 'idXuatXu', listKey: 'xuatXu', codePrefix: 'XX' },
  'chat-lieu': { title: 'Chất liệu', field: 'idChatLieu', listKey: 'chatLieu', codePrefix: 'CL' },
  'co-ao': { title: 'Cổ áo', field: 'idCoAo', listKey: 'coAo', codePrefix: 'CA' },
  'tay-ao': { title: 'Tay áo', field: 'idTayAo', listKey: 'tayAo', codePrefix: 'TA' },
  'hoa-tiet': { title: 'Họa tiết', field: 'idHoaTiet', listKey: 'hoaTiet', codePrefix: 'HT' }
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

    // Tự động gán thuộc tính mới tạo vào form
    form.value[quickAttrModal.value.field] = createdItem.id

    showToast(`Đã thêm mới và chọn ${quickAttrModal.value.title.toLowerCase()} "${ten}"!`, 'success')
    closeQuickAttrModal()
  } catch (err) {
    const msg = err.response?.data?.message || 'Có lỗi xảy ra khi thêm thuộc tính!'
    quickAttrModal.value.error = msg
  } finally {
    quickAttrModal.value.loading = false
  }
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
        newVariants.push(existingMap.get(key))
      } else {
        newVariants.push({
          key,
          id: null,
          idMauSac: color.id,
          tenMauSac: color.tenMauSac,
          maHex: color.maHex,
          idKichCo: size.id,
          tenKichCo: size.tenKichCo,
          soLuong: bulkQty.value ?? 0,
          giaBan: bulkPrice.value ?? 0,
          trangThai: 1,
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
    if (isNaN(gb) || gb < 0 || gb > 1000000000) {
      showToast('Giá bán áp dụng phải từ 0 đến 1,000,000,000 VNĐ!', 'error')
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

// Xóa toàn bộ biến thể của 1 nhóm màu
const openDeleteColorGroupConfirm = (group) => {
  openConfirm({
    title: 'Xác nhận xóa nhóm màu',
    message: `Bạn có muốn xóa tất cả biến thể và ảnh của màu "${group.color.tenMauSac}" không?`,
    type: 'danger',
    confirmText: 'Xóa nhóm màu',
    onConfirm: () => {
      const colorId = group.color.id
      variantsList.value = variantsList.value.filter(v => v.idMauSac !== colorId)
      delete colorImages.value[colorId]
      selectedColors.value = selectedColors.value.filter(c => c.id !== colorId)
      showToast(`Đã xóa toàn bộ biến thể và ảnh của màu "${group.color.tenMauSac}"!`)
    }
  })
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
    colorImages.value[colorId] = event.target.result
    showToast('Tải ảnh thành công!')
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
  if (url.startsWith('data:') || url.startsWith('http://') || url.startsWith('https://')) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
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
const openSaveConfirm = () => {
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

  if (form.value.moTa && form.value.moTa.length > 2000) {
    showToast('Mô tả sản phẩm không được vượt quá 2000 ký tự!', 'error')
    return
  }

  // Validate các biến thể được chọn
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
    if (isNaN(gb) || gb < 0 || gb > 1000000000) {
      showToast(`Giá bán biến thể (${label}) phải từ 0 đến 1,000,000,000 VNĐ!`, 'error')
      return
    }
  }

  const actionText = isEdit.value ? 'chỉnh sửa' : 'lưu'
  const countVar = activeVariants.length

  openConfirm({
    title: isEdit.value ? 'Xác nhận chỉnh sửa sản phẩm' : 'Xác nhận lưu sản phẩm',
    message: `Bạn có muốn ${actionText} sản phẩm "${ten}"${countVar > 0 ? ` cùng ${countVar} biến thể` : ''} không?`,
    type: 'primary',
    confirmText: isEdit.value ? 'Xác nhận cập nhật' : 'Xác nhận lưu',
    onConfirm: submitForm
  })
}

// Thực hiện gọi API lưu sản phẩm
const submitForm = async () => {
  submitting.value = true
  try {
    // Thu thập danh sách ảnh từ các màu thực sự còn tồn tại
    const activeColorIds = new Set(activeColorsForImages.value.map(c => String(c.id)))
    const imageList = Object.entries(colorImages.value)
      .filter(([cId, img]) => activeColorIds.has(String(cId)) && !!img)
      .map(([_, img]) => img)

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
      trangThai: form.value.trangThai ?? 1,
      hinhAnhs: imageList
    }

    let savedProduct = null
    if (isEdit.value) {
      const res = await api.put(`/api/san-pham/${productId.value}`, payload)
      savedProduct = res.data
      showToast('Cập nhật sản phẩm thành công!')
    } else {
      const res = await api.post('/api/san-pham', payload)
      savedProduct = res.data
      showToast('Thêm mới sản phẩm thành công!')
    }

    // Nếu có biến thể, lưu danh sách biến thể được chọn
    const activeVariants = variantsList.value.filter(v => v.selected !== false)
    if (savedProduct && savedProduct.id && activeVariants.length > 0) {
      const variantPayloads = activeVariants.map(v => ({
        id: v.id || undefined,
        idSanPham: savedProduct.id,
        idMauSac: v.idMauSac,
        idKichCo: v.idKichCo,
        soLuong: v.soLuong || 0,
        giaBan: v.giaBan || 0,
        trangThai: v.trangThai ?? 1
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
      form.value = {
        maSanPham: p.maSanPham || '',
        tenSanPham: p.tenSanPham || '',
        idThuongHieu: p.idThuongHieu || '',
        idDanhMuc: p.idDanhMuc || '',
        idXuatXu: p.idXuatXu || '',
        idChatLieu: p.idChatLieu || '',
        idCoAo: p.idCoAo || '',
        idTayAo: p.idTayAo || '',
        idHoaTiet: p.idHoaTiet || '',
        trangThai: p.trangThai ?? 1,
        moTa: p.moTa || ''
      }

      // Tải biến thể của sản phẩm
      const varRes = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${id}`)
      if (varRes.data && varRes.data.length > 0) {
        variantsList.value = varRes.data.map(item => ({
          key: `${item.idMauSac}_${item.idKichCo}`,
          id: item.id,
          idMauSac: item.idMauSac,
          tenMauSac: item.tenMauSac,
          maHex: item.maHex,
          idKichCo: item.idKichCo,
          tenKichCo: item.tenKichCo,
          soLuong: item.soLuong,
          giaBan: item.giaBan,
          trangThai: item.trangThai ?? 1,
          selected: true
        }))

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
          selectedColors.value.forEach((color, idx) => {
            if (p.hinhAnhs[idx]) {
              colorImages.value[color.id] = p.hinhAnhs[idx]
            }
          })
        }
      }
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

onMounted(async () => {
  document.addEventListener('click', closeAllDropdowns)
  await fetchAttributes()
  if (isEdit.value && productId.value) {
    await loadProductDetails(productId.value)
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
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.2rem;
}

.breadcrumb-group {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
}

.breadcrumb-icon {
  font-size: 1.1rem;
  color: #71828d;
}

.breadcrumb-root {
  color: #64748b;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.breadcrumb-root:hover {
  color: var(--blue, #496883);
}

.breadcrumb-sep {
  color: #94a3b8;
}

.breadcrumb-current {
  color: #1e293b;
  font-weight: 700;
}

.btn-back-top {
  background-color: #ffffff;
  color: var(--blue, #496883);
  border: 1px solid #cbd5e1;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back-top:hover {
  background-color: #eaf2f6;
  border-color: #6e96ad;
}

/* Card container */
.form-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.mt-3 {
  margin-top: 1rem;
}

.mt-4 {
  margin-top: 1.5rem;
}

/* Form items & Grid */
.grid-2-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
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
  border: 1px solid #e2e8f0;
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
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

/* Select with Add Button */
.select-with-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.select-with-btn .form-select {
  flex: 1;
  min-width: 0;
}

.btn-add-attr-inline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  height: 38px;
  padding: 0 0.75rem;
  font-size: 0.82rem;
  font-weight: 600;
  color: #496883;
  background-color: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  flex-shrink: 0;
}

.btn-add-attr-inline:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}

/* Quick Attr Modal Card */
.quick-attr-modal-card {
  background: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15), 0 10px 10px -5px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  animation: modalScaleUp 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.quick-attr-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.25rem 1.25rem 0.75rem;
  border-bottom: 1px solid #f1f5f9;
}

.quick-attr-title-box {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.quick-attr-badge {
  font-size: 0.72rem;
  font-weight: 600;
  color: #496883;
  background-color: #eaf2f6;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  width: fit-content;
}

.quick-attr-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1e293b;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
  padding: 0 0.25rem;
  transition: color 0.2s;
}

.btn-close-modal:hover {
  color: #ef4444;
}

.quick-attr-modal-body {
  padding: 1.25rem;
}

.quick-attr-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  padding: 0.85rem 1.25rem;
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
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
  background: linear-gradient(135deg, #496883, #385269);
  color: #ffffff;
  border: none;
  padding: 0.7rem 1.6rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
  transition: all 0.2s;
}

.btn-generate:hover {
  background: linear-gradient(135deg, #3d5870, #2c4154);
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
  background-color: #ffffff;
  color: var(--blue, #496883);
  border: 1px solid var(--blue, #496883);
  padding: 0.55rem 1.1rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-bulk-apply:hover {
  background-color: var(--blue, #496883);
  color: #ffffff;
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

.group-header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-delete-group {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background-color: #ffffff;
  border: 1px solid #fecaca;
  color: #ef4444;
  padding: 0.25rem 0.65rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete-group:hover {
  background-color: #fef2f2;
  border-color: #ef4444;
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
  background: #ffffff;
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
  color: #64748b;
  border-bottom: 1px solid #edf1f4;
}

.variant-data-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.cell-stt {
  color: #94a3b8;
  font-weight: 600;
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
  border: none;
  background: var(--blue, #496883);
  color: #ffffff;
  border-radius: 6px;
  padding: 0.4rem 0.8rem;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-add-img:hover {
  background: #385269;
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
  background: #ffffff;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.75rem 2rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-lg:hover {
  background: #f1f5f9;
}

.btn-save-lg {
  background: linear-gradient(135deg, #496883, #385269);
  color: #ffffff;
  border: none;
  padding: 0.75rem 2.2rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
  transition: all 0.2s;
}

.btn-save-lg:hover:not(:disabled) {
  background: linear-gradient(135deg, #3d5870, #2c4154);
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

/* Modal xác nhận chuyên nghiệp */
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.55);
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
  max-width: 440px;
  padding: 1.75rem 1.5rem 1.5rem;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
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

.confirm-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  margin: 0 auto 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.confirm-icon-wrap.primary {
  background: #eff6ff;
  color: #2563eb;
}

.confirm-icon-wrap.danger {
  background: #fef2f2;
  color: #dc2626;
}

.confirm-icon-wrap.info {
  background: #f0fdf4;
  color: #16a34a;
}

.confirm-title {
  margin: 0 0 0.5rem;
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
}

.confirm-message {
  margin: 0 0 1.5rem;
  font-size: 0.92rem;
  color: #64748b;
  line-height: 1.5;
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
}

.btn-confirm-cancel {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1.4rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-cancel:hover {
  background: #e2e8f0;
}

.btn-confirm-primary {
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  padding: 0.6rem 1.4rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-primary:hover {
  background: #385269;
}

.btn-confirm-danger {
  background: #dc2626;
  color: #ffffff;
  border: none;
  padding: 0.6rem 1.4rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm-danger:hover {
  background: #b91c1c;
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
