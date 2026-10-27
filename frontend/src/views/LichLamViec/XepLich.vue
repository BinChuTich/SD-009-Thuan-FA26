<template>
  <div class="xep-lich-page">
    <!-- Tiêu đề trang -->
    <div class="page-breadcrumb">
      <router-link to="/xep-lich" class="crumb-parent">Lịch làm việc</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Lịch làm việc</span>
    </div>
    <div class="page-top-bar" v-if="false">
      <div class="title-with-icon">
        <div class="title-icon-box">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
          </svg>
        </div>
        <h1 class="page-main-title">Lịch làm việc</h1>
      </div>
    </div>

    <!-- Toolbar điều khiển chính -->
    <div class="content-card toolbar-card-box">
      <div class="toolbar-left-cluster">
        <!-- Cụm chọn ngày < dd/mm/yyyy > -->
        <div class="date-navigator-wrap">
          <button type="button" class="nav-arrow-btn" @click="changeWeek(-1)" title="Tuần trước">‹</button>
          <div class="input-date-display">
            <input
              type="date"
              v-model="selectedAnchorDate"
              class="date-picker-clean"
              @change="onAnchorDateChange"
            />
          </div>
          <button type="button" class="nav-arrow-btn" @click="changeWeek(1)" title="Tuần sau">›</button>
        </div>

        <button type="button" class="btn-today-pill" @click="goToCurrentWeek">
          Hôm nay
        </button>

        <span class="week-range-label">
          Tuần từ: <strong>{{ weekDays[0]?.dateStr }}</strong> đến <strong>{{ weekDays[6]?.dateStr }}</strong>
        </span>
      </div>

      <div class="toolbar-right-cluster">
        <!-- Chuyển đổi Dạng lịch / Dạng bảng -->
        <div class="view-mode-toggle">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: viewMode === 'calendar' }"
            @click="viewMode = 'calendar'"
          >
            Dạng lịch
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: viewMode === 'table' }"
            @click="viewMode = 'table'"
          >
            Dạng bảng
          </button>
        </div>

        <!-- Phân ca hàng loạt -->
        <button v-if="isAdmin()" type="button" class="btn-bulk-schedule" @click="openBulkModal" title="Phân ca hàng loạt cho nhân viên">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
            <line x1="16" y1="2" x2="16" y2="6" />
            <line x1="8" y1="2" x2="8" y2="6" />
            <line x1="3" y1="10" x2="21" y2="10" />
            <line x1="12" y1="14" x2="12" y2="18" />
            <line x1="10" y1="16" x2="14" y2="16" />
          </svg>
          <span>Phân ca hàng loạt</span>
        </button>

        <!-- Xuất Template -->
        <button v-if="isAdmin()" type="button" class="btn-export-template" @click="downloadTemplate" title="Tải file mẫu Excel xếp lịch">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          <span>Xuất Template</span>
        </button>

        <!-- Nhập Excel (Nền nâu đậm #6B4F3B) -->
        <button v-if="isAdmin()" type="button" class="btn-import-excel" @click="showImportModal = true" title="Nhập danh sách xếp lịch từ Excel">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <span>Nhập Excel</span>
        </button>
      </div>
    </div>

    <!-- DẠNG 1: Bảng lưới lịch tuần (Calendar View) -->
    <div v-if="viewMode === 'calendar'" class="content-card schedule-grid-card">
      <div class="schedule-table-wrap">
        <table class="schedule-grid-table">
          <thead>
            <tr class="header-brown-beige">
              <th class="col-shift-header">Ca / Ngày</th>
              <th
                v-for="day in weekDays"
                :key="day.dateStr"
                class="col-day-header"
                :class="{ 'today-header': isToday(day.dateStr) }"
              >
                <div class="day-header-title">{{ day.dayOfWeekName }}</div>
                <div class="day-header-sub">{{ day.displayDate }}</div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td :colspan="8" style="text-align: center; padding: 3rem; color: #8c979d;">
                Đang tải lịch phân ca tuần...
              </td>
            </tr>
            <tr v-else-if="shifts.length === 0">
              <td :colspan="8" style="text-align: center; padding: 3rem; color: #8c979d;">
                Chưa có ca làm việc nào đang hoạt động trong hệ thống.
              </td>
            </tr>
            <tr v-for="shift in shifts" :key="shift.id" class="grid-shift-row">
              <!-- Cột thông tin Ca -->
              <td class="shift-info-cell">
                <strong class="shift-name-text">{{ shift.tenCa }}</strong>
                <span class="shift-time-badge">{{ formatTime(shift.gioBatDau) }} - {{ formatTime(shift.gioKetThuc) }}</span>
              </td>

              <!-- 7 Cột từ Thứ 2 đến CN -->
              <td
                v-for="day in weekDays"
                :key="day.dateStr"
                class="schedule-day-cell"
                :class="{
                  'past-day-cell': isPastDateOrShift(day.dateStr, shift.gioKetThuc),
                  'today-cell': isToday(day.dateStr)
                }"
              >
                <!-- Danh sách thẻ nhân viên trong ca -->
                <div class="employee-tags-stack">
                  <div
                    v-for="item in getCellAssignments(shift.id, day.dateStr)"
                    :key="item.id"
                    class="employee-chip-card"
                  >
                    <div class="chip-avatar" :style="{ backgroundColor: getAvatarColor(item.nhanVien?.tenNhanVien) }">
                      {{ getInitial(item.nhanVien?.tenNhanVien) }}
                    </div>
                    <div class="chip-info">
                      <span class="chip-name" :title="item.nhanVien?.tenNhanVien">{{ item.nhanVien?.tenNhanVien }}</span>
                      <span class="chip-code">{{ item.nhanVien?.maNhanVien }}</span>
                    </div>

                    <!-- Nút xóa phân ca (chặn nếu ca đã qua trong quá khứ) -->
                    <button
                      v-if="isAdmin() && !isPastDateOrShift(day.dateStr, shift.gioKetThuc)"
                      type="button"
                      class="btn-remove-chip"
                      @click="removeAssignment(item)"
                      title="Xóa phân ca"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <!-- Nút dấu cộng (+) để thêm nhân viên vào ca -->
                <button
                  type="button"
                  v-if="isAdmin()"
                  class="btn-add-schedule-cell"
                  :disabled="isPastDateOrShift(day.dateStr, shift.gioKetThuc)"
                  @click="openAddModal(shift, day.dateStr)"
                  :title="isPastDateOrShift(day.dateStr, shift.gioKetThuc) ? 'Không thể xếp lịch cho ca trong quá khứ' : 'Thêm nhân viên vào ca'"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- DẠNG 2: Bảng chi tiết (Table View) -->
    <div v-else class="content-card schedule-table-card">
      <div class="table-responsive-clean">
        <table class="minimal-styled-table">
          <thead>
            <tr class="header-brown-beige">
              <th style="width: 60px; text-align: center">STT</th>
              <th style="width: 140px">NGÀY LÀM VIỆC</th>
              <th style="width: 140px">CA LÀM VIỆC</th>
              <th style="width: 150px; text-align: center">KHUNG GIỜ</th>
              <th style="width: 120px">MÃ NV</th>
              <th style="min-width: 180px">TÊN NHÂN VIÊN</th>
              <th>GHI CHÚ</th>
              <th v-if="isAdmin()" style="width: 100px; text-align: center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="allWeeklyAssignments.length === 0">
              <td :colspan="isAdmin() ? 8 : 7" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                Không có lịch phân ca nào trong tuần này.
              </td>
            </tr>
            <tr v-for="(item, idx) in allWeeklyAssignments" :key="item.id" class="table-data-row">
              <td style="text-align: center" class="text-muted-stt">{{ idx + 1 }}</td>
              <td class="font-bold text-dark">{{ formatDateVn(item.ngayLamViec) }}</td>
              <td>
                <span class="badge-shift-title">{{ item.caLamViec?.tenCa }}</span>
              </td>
              <td style="text-align: center" class="text-time-col">
                {{ formatTime(item.caLamViec?.gioBatDau) }} - {{ formatTime(item.caLamViec?.gioKetThuc) }}
              </td>
              <td class="font-bold">{{ item.nhanVien?.maNhanVien }}</td>
              <td>
                <div class="employee-row-info">
                  <div class="chip-avatar small" :style="{ backgroundColor: getAvatarColor(item.nhanVien?.tenNhanVien) }">
                    {{ getInitial(item.nhanVien?.tenNhanVien) }}
                  </div>
                  <strong>{{ item.nhanVien?.tenNhanVien }}</strong>
                </div>
              </td>
              <td class="text-muted">{{ item.ghiChu || '-' }}</td>
              <td v-if="isAdmin()" style="text-align: center">
                <button
                  type="button"
                  class="btn-delete-row"
                  :disabled="isPastDateOrShift(item.ngayLamViec, item.caLamViec?.gioKetThuc)"
                  @click="removeAssignment(item)"
                  title="Xóa phân ca"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                  </svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal Thêm Phân Ca Nhân Viên -->
    <div v-if="showAddModal" class="modal-backdrop-clean" @click.self="showAddModal = false">
      <div class="modal-dialog-clean">
        <div class="modal-header-clean">
          <h3 class="modal-title-clean">Phân ca nhân viên</h3>
          <button type="button" class="btn-close-modal" @click="showAddModal = false">✕</button>
        </div>

        <div class="modal-body-clean">
          <!-- Thông tin ca & ngày -->
          <div class="shift-context-box">
            <div class="context-item">
              <span class="context-lbl">Ca làm việc:</span>
              <strong class="context-val">{{ modalContext.shift?.tenCa }} ({{ formatTime(modalContext.shift?.gioBatDau) }} - {{ formatTime(modalContext.shift?.gioKetThuc) }})</strong>
            </div>
            <div class="context-item">
              <span class="context-lbl">Ngày làm việc:</span>
              <strong class="context-val">{{ formatDateVn(modalContext.dateStr) }}</strong>
            </div>
          </div>

          <!-- Dropdown chọn nhân viên -->
          <div class="form-group-item">
            <label class="modal-field-label">Nhân viên <span class="required-star">*</span></label>
            <div class="select-wrapper-minimal">
              <select
                v-model="modalSelectedEmployeeId"
                class="form-control-minimal modal-input"
                :class="{ 'input-error-border': addModalError }"
                @change="onEmployeeSelectChange"
              >
                <option value="">-- Chọn nhân viên --</option>
                <option
                  v-for="nv in availableEmployees"
                  :key="nv.id"
                  :value="nv.id"
                >
                  {{ nv.maNhanVien }} - {{ nv.tenNhanVien }} ({{ nv.soDienThoai || 'Chưa có SĐT' }})
                </option>
              </select>
            </div>
            <span v-if="addModalError" class="error-text-red">{{ addModalError }}</span>
          </div>

          <!-- Ghi chú phân ca -->
          <div class="form-group-item">
            <label class="modal-field-label">Ghi chú</label>
            <input
              type="text"
              v-model="modalNote"
              placeholder="Ghi chú phân công (ví dụ: Trực quầy thu ngân, quản lý kho...)"
              class="form-control-minimal modal-input"
            />
          </div>
        </div>

        <div class="modal-footer-clean">
          <button type="button" class="btn-cancel-modal" @click="showAddModal = false">Hủy</button>
          <button
            type="button"
            class="btn-save-modal"
            :disabled="savingSchedule"
            @click="submitAssignSchedule"
          >
            {{ savingSchedule ? 'Đang lưu...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Nhập Excel (Cơ chế Tất cả hoặc Không có gì) -->
    <div v-if="showImportModal" class="modal-backdrop-clean" @click.self="closeImportModal">
      <div class="modal-dialog-clean modal-wide">
        <div class="modal-header-clean">
          <h3 class="modal-title-clean">Nhập lịch làm việc từ Excel</h3>
          <button type="button" class="btn-close-modal" @click="closeImportModal">✕</button>
        </div>

        <div class="modal-body-clean">
          <!-- Hướng dẫn & tải template -->
          <div class="import-help-box">
            <div class="help-text">
              Vui lòng tải file mẫu và điền đầy đủ dữ liệu theo quy định. Hệ thống sẽ kiểm tra toàn bộ file trước khi lưu vào CSDL (All-or-Nothing).
            </div>
            <button type="button" class="btn-download-sample" @click="downloadTemplate">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              <span>Tải file mẫu Excel</span>
            </button>
          </div>

          <!-- Khu vực chọn file -->
          <div class="file-dropzone-box" @dragover.prevent @drop.prevent="onFileDrop">
            <input
              type="file"
              ref="fileInputRef"
              accept=".xlsx, .xls"
              class="file-input-hidden"
              @change="onFileSelected"
            />
            <div class="dropzone-content" @click="$refs.fileInputRef.click()">
              <div class="upload-icon-circle">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
              </div>
              <div v-if="!selectedFile" class="dropzone-prompt">
                <strong>Nhấn để chọn file Excel</strong> hoặc kéo thả file vào đây (.xlsx, .xls)
              </div>
              <div v-else class="file-selected-badge">
                <span class="file-name-bold">{{ selectedFile.name }}</span>
                <span class="file-size-tag">{{ (selectedFile.size / 1024).toFixed(1) }} KB</span>
              </div>
            </div>
          </div>

          <!-- Danh sách lỗi chi tiết nếu có -->
          <div v-if="importErrors.length > 0" class="import-error-list-container">
            <div class="error-list-header">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <strong>Phát hiện {{ importErrors.length }} lỗi trong file Excel:</strong>
            </div>
            <ul class="error-items-ul">
              <li v-for="(err, i) in importErrors" :key="i" class="error-item-li">
                {{ err }}
              </li>
            </ul>
          </div>
        </div>

        <div class="modal-footer-clean">
          <button type="button" class="btn-cancel-modal" @click="closeImportModal">Hủy</button>
          <button
            type="button"
            class="btn-save-modal"
            :disabled="!selectedFile || importing"
            @click="submitImportExcel"
          >
            {{ importing ? 'Đang kiểm tra & nhập...' : 'Thêm lịch nhân viên' }}
          </button>
        </div>
      </div>
    </div>
  </div>
    <!-- Modal Phân ca hàng loạt -->
    <div v-if="showBulkModal" class="modal-backdrop-clean" @click.self="closeBulkModal">
      <div class="modal-dialog-clean bulk-modal-dialog">
        <!-- Header màu tối theo thiết kế -->
        <div class="bulk-modal-header">
          <div class="bulk-header-text">
            <h3 class="bulk-modal-title">Phân ca hàng loạt</h3>
            <p class="bulk-modal-subtitle">Chọn nhân viên, ca làm việc rồi tick ngày để thêm nhanh.</p>
          </div>
          <button type="button" class="bulk-btn-close" @click="closeBulkModal" title="Đóng">✕</button>
        </div>

        <div class="bulk-modal-body">
          <!-- Cột trái: Cấu hình phân ca -->
          <div class="bulk-left-col">
            <!-- Chế độ -->
            <div class="bulk-form-group">
              <label class="bulk-field-label">Chế độ</label>
              <div class="bulk-mode-toggle-group">
                <button
                  type="button"
                  class="bulk-mode-toggle-btn"
                  :class="{ active: bulkMode === 'week' }"
                  @click="setBulkMode('week')"
                >
                  Theo tuần
                </button>
                <button
                  type="button"
                  class="bulk-mode-toggle-btn"
                  :class="{ active: bulkMode === 'month' }"
                  @click="setBulkMode('month')"
                >
                  Theo tháng
                </button>
              </div>
            </div>

            <!-- Chọn ngày/tháng mốc -->
            <div class="bulk-form-group">
              <label class="bulk-field-label">
                {{ bulkMode === 'week' ? 'Chọn ngày mốc (tự lấy T2–CN)' : 'Chọn tháng' }}
              </label>
              <div class="bulk-input-icon-wrap">
                <input
                  v-if="bulkMode === 'week'"
                  type="date"
                  v-model="bulkAnchorDate"
                  :min="todayYmd"
                  class="bulk-control-input"
                />
                <input
                  v-else
                  type="month"
                  v-model="bulkAnchorMonth"
                  :min="todayYmd.substring(0, 7)"
                  class="bulk-control-input"
                />
              </div>
              <div v-if="bulkMode === 'week'" class="bulk-range-hint-text">
                {{ bulkDateRangeLabel }}
              </div>
            </div>

            <!-- Nhân viên * -->
            <div class="bulk-form-group">
              <label class="bulk-field-label">Nhân viên <span class="required-star">*</span></label>
              <select v-model="bulkForm.nhanVienId" class="bulk-control-input">
                <option value="">-- Chọn nhân viên --</option>
                <option
                  v-for="nv in activeEmployees"
                  :key="nv.id"
                  :value="nv.id"
                >
                  {{ nv.maNhanVien }} - {{ nv.tenNhanVien }}
                </option>
              </select>
            </div>

            <!-- Ca làm việc * -->
            <div class="bulk-form-group">
              <label class="bulk-field-label">Ca làm việc <span class="required-star">*</span></label>
              <select v-model="bulkForm.caLamViecId" class="bulk-control-input">
                <option value="">-- Chọn ca mẫu --</option>
                <option
                  v-for="c in shifts"
                  :key="c.id"
                  :value="c.id"
                >
                  {{ c.tenCa }} ({{ formatTime(c.gioBatDau) }} - {{ formatTime(c.gioKetThuc) }})
                </option>
              </select>
            </div>

            <!-- Ghi chú -->
            <div class="bulk-form-group">
              <label class="bulk-field-label">Ghi chú</label>
              <input
                type="text"
                v-model="bulkForm.ghiChu"
                placeholder="VD: Tăng ca, trực thay..."
                class="bulk-control-input"
              />
            </div>

            <!-- Box chọn nhanh -->
            <div class="bulk-quick-select-card">
              <div class="bulk-quick-top-row">
                <span class="bulk-quick-title">Đang chọn</span>
                <span class="bulk-quick-badge">{{ bulkSelectedDates.length }} ngày</span>
              </div>
              <div class="bulk-quick-btn-group">
                <button type="button" class="bulk-quick-action-btn" @click="bulkSelectAll">Chọn tất cả</button>
                <button type="button" class="bulk-quick-action-btn" @click="bulkSelectWorkdays">Chỉ T2–T6</button>
                <button type="button" class="bulk-quick-action-btn" @click="bulkClearSelection">Bỏ chọn</button>
              </div>
            </div>
          </div>

          <!-- Cột phải: Danh sách ngày hiển thị -->
          <div class="bulk-right-col">
            <h4 class="bulk-right-title">
              {{ bulkMode === 'week' ? 'Chọn ngày theo tuần' : 'Chọn ngày theo tháng' }}
            </h4>

            <!-- Giao diện Theo Tuần: Lưới các thẻ ngày -->
            <template v-if="bulkMode === 'week'">
              <div v-if="bulkDaysList.length > 0" class="bulk-cards-container">
                <div
                  v-for="day in bulkDaysList"
                  :key="day.dateStr"
                  class="bulk-day-item-card"
                  :class="{ selected: isBulkDateChecked(day.dateStr) }"
                  @click="toggleBulkDate(day.dateStr)"
                >
                  <input
                    type="checkbox"
                    :value="day.dateStr"
                    :checked="isBulkDateChecked(day.dateStr)"
                    class="bulk-day-native-checkbox"
                    @click.stop="toggleBulkDate(day.dateStr)"
                  />
                  <div class="bulk-day-text-wrap">
                    <div class="bulk-day-weekday-name">{{ day.dayOfWeekName }}</div>
                    <div class="bulk-day-date-number">{{ day.displayDate }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="bulk-no-days-alert">
                Không có ngày nào từ ngày hiện tại trở đi trong khoảng thời gian này.
              </div>
            </template>

            <!-- Giao diện Theo Tháng: Lưới lịch 7 cột đẹp mắt -->
            <template v-else>
              <div class="month-calendar-wrap">
                <!-- Tiêu đề 7 thứ -->
                <div class="month-calendar-header">
                  <div class="month-header-cell">Thứ 2</div>
                  <div class="month-header-cell">Thứ 3</div>
                  <div class="month-header-cell">Thứ 4</div>
                  <div class="month-header-cell">Thứ 5</div>
                  <div class="month-header-cell">Thứ 6</div>
                  <div class="month-header-cell">Thứ 7</div>
                  <div class="month-header-cell">CN</div>
                </div>

                <!-- Lưới 42 ô ngày -->
                <div class="month-calendar-grid">
                  <div
                    v-for="cell in bulkMonthCalendarCells"
                    :key="cell.dateStr"
                    class="month-day-cell"
                    :class="{
                      'other-month': !cell.isCurrentMonth,
                      'past-day': cell.isPast,
                      'selectable': cell.isSelectable,
                      'selected': isBulkDateChecked(cell.dateStr)
                    }"
                    @click="cell.isSelectable && toggleBulkDate(cell.dateStr)"
                  >
                    <input
                      type="checkbox"
                      :checked="isBulkDateChecked(cell.dateStr)"
                      :disabled="!cell.isSelectable"
                      class="month-cell-checkbox"
                      @click.stop="cell.isSelectable && toggleBulkDate(cell.dateStr)"
                    />
                    <span class="month-cell-day-num">{{ cell.dayNumber }}</span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- Footer -->
        <div class="bulk-modal-footer">
          <button type="button" class="bulk-btn-cancel" @click="closeBulkModal">Hủy</button>
          <button
            type="button"
            class="bulk-btn-submit"
            :disabled="submittingBulk"
            @click="submitBulkSchedule"
          >
            {{ submittingBulk ? 'Đang thêm...' : `Thêm mới (${bulkSelectedDates.length})` }}
          </button>
        </div>
      </div>
    </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import api from '@/api'
import { showAlert, showConfirm, showToast } from '@/utils/dialog.js'
import { isAdmin } from '@/utils/auth.js'

const loading = ref(false)
const shifts = ref([])
const activeEmployees = ref([])
const schedules = ref([])

const viewMode = ref('calendar') // 'calendar' | 'table'

// Tuần hiện tại
const currentMonday = ref(getMondayOfDate(new Date()))
const selectedAnchorDate = ref(new Date().toISOString().split('T')[0])

function getMondayOfDate(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = date.getDate() - day + (day === 0 ? -6 : 1) // adjust when day is sunday
  const mon = new Date(date.setDate(diff))
  mon.setHours(0, 0, 0, 0)
  return mon
}

const weekDays = computed(() => {
  const days = []
  const dayNames = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật']
  const start = new Date(currentMonday.value)

  for (let i = 0; i < 7; i++) {
    const cur = new Date(start)
    cur.setDate(start.getDate() + i)
    const yyyy = cur.getFullYear()
    const mm = String(cur.getMonth() + 1).padStart(2, '0')
    const dd = String(cur.getDate()).padStart(2, '0')
    const dateStr = `${yyyy}-${mm}-${dd}`

    days.push({
      dateStr,
      displayDate: `${dd}/${mm}`,
      dayOfWeekName: dayNames[i]
    })
  }
  return days
})

function formatYmd(d) {
  if (!d) return ''
  const dateObj = typeof d === 'string' ? new Date(d) : d
  const y = dateObj.getFullYear()
  const m = String(dateObj.getMonth() + 1).padStart(2, '0')
  const day = String(dateObj.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function isToday(dateStr) {
  const todayStr = formatYmd(new Date())
  return dateStr === todayStr
}

function isPastDateOrShift(dateStr, endTimeStr) {
  const todayStr = formatYmd(new Date())
  if (dateStr < todayStr) return true
  if (dateStr === todayStr && endTimeStr) {
    const now = new Date()
    const nowTimeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:00`
    const normalizedEndTime = endTimeStr.length === 5 ? endTimeStr + ':00' : endTimeStr
    return normalizedEndTime < nowTimeStr
  }
  return false
}

function changeWeek(direction) {
  const d = new Date(currentMonday.value)
  d.setDate(d.getDate() + direction * 7)
  currentMonday.value = d
  selectedAnchorDate.value = formatYmd(d)
  loadWeekSchedule()
}

function onAnchorDateChange() {
  if (selectedAnchorDate.value) {
    currentMonday.value = getMondayOfDate(new Date(selectedAnchorDate.value))
    loadWeekSchedule()
  }
}

function goToCurrentWeek() {
  currentMonday.value = getMondayOfDate(new Date())
  selectedAnchorDate.value = formatYmd(new Date())
  loadWeekSchedule()
}

function formatTime(t) {
  if (!t) return '--:--'
  if (typeof t === 'string' && t.includes(':')) {
    const parts = t.split(':')
    return `${parts[0]}:${parts[1]}`
  }
  return t
}

function formatDateVn(dStr) {
  if (!dStr) return '--/--/----'
  const parts = dStr.split('-')
  if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`
  return dStr
}

function getInitial(name) {
  if (!name) return 'N'
  const parts = name.trim().split(' ')
  return parts[parts.length - 1].charAt(0).toUpperCase()
}

const avatarColors = ['#0284c7', '#059669', '#d97706', '#7c3aed', '#db2777', '#4b5563', '#0d9488']
function getAvatarColor(name) {
  if (!name) return '#0284c7'
  let hash = 0
  for (let i = 0; i < name.length; i++) hash += name.charCodeAt(i)
  return avatarColors[Math.abs(hash) % avatarColors.length]
}

function getCellAssignments(shiftId, dateStr) {
  return schedules.value.filter(s => {
    return s.caLamViec?.id === shiftId && s.ngayLamViec === dateStr
  })
}

const allWeeklyAssignments = computed(() => {
  return [...schedules.value].sort((a, b) => {
    if (a.ngayLamViec !== b.ngayLamViec) return a.ngayLamViec.localeCompare(b.ngayLamViec)
    return (a.caLamViec?.gioBatDau || '').localeCompare(b.caLamViec?.gioBatDau || '')
  })
})

async function loadShifts() {
  try {
    const res = await api.get('/api/ca-lam-viec', { params: { trangThai: 1 } })
    const dataList = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    shifts.value = dataList.map(s => {
      const start = s.gioBatDau || s.startTime || '00:00:00'
      const end = s.gioKetThuc || s.endTime || '00:00:00'
      return {
        ...s,
        gioBatDau: start,
        gioKetThuc: end,
        startTime: start,
        endTime: end
      }
    })
  } catch (e) {
    shifts.value = []
  }
}

async function loadActiveEmployees() {
  try {
    const res = await api.get('/api/nhan-vien', { params: { trangThai: 1, size: 200 } })
    activeEmployees.value = res.data?.content || []
  } catch (e) {}
}

async function loadWeekSchedule() {
  loading.value = true
  try {
    const fromDate = weekDays.value[0]?.dateStr
    const toDate = weekDays.value[6]?.dateStr
    const params = {}
    if (fromDate) params.fromDate = fromDate
    if (toDate) params.toDate = toDate
    const res = await api.get('/api/lich-lam-viec/tuan', { params })
    schedules.value = res.data || []
  } catch (e) {
    showToast(e?.response?.data?.message || 'Không thể tải lịch làm việc', 'error')
  } finally {
    loading.value = false
  }
}

// Modal Thêm Phân Ca
const showAddModal = ref(false)
const savingSchedule = ref(false)
const modalSelectedEmployeeId = ref('')
const modalNote = ref('')
const addModalError = ref('')

const modalContext = reactive({
  shift: null,
  dateStr: ''
})

const availableEmployees = computed(() => {
  if (!modalContext.shift || !modalContext.dateStr) return []
  // Lọc chỉ những nhân viên đang hoạt động
  const activeOnly = activeEmployees.value.filter(nv => nv.trangThai === 1)

  // Ẩn nhân viên đã có mặt trong ca này vào ngày này
  const currentAssignedEmpIds = new Set(
    getCellAssignments(modalContext.shift.id, modalContext.dateStr).map(s => s.nhanVien?.id)
  )

  return activeOnly.filter(nv => !currentAssignedEmpIds.has(nv.id))
})

function openAddModal(shift, dateStr) {
  if (isPastDateOrShift(dateStr, shift.gioKetThuc)) {
    showToast('Không thể xếp lịch cho những ca đã diễn ra trong quá khứ', 'warning')
    return
  }
  modalContext.shift = shift
  modalContext.dateStr = dateStr
  modalSelectedEmployeeId.value = ''
  modalNote.value = ''
  addModalError.value = ''
  showAddModal.value = true
}

function onEmployeeSelectChange() {
  if (modalSelectedEmployeeId.value) {
    addModalError.value = ''
  }
}

async function submitAssignSchedule() {
  if (!modalSelectedEmployeeId.value) {
    addModalError.value = 'Vui lòng chọn một nhân viên'
    return
  }

  // Check xung đột khung giờ với các ca khác trong cùng ngày của nhân viên đó
  const empId = Number(modalSelectedEmployeeId.value)
  const dateStr = modalContext.dateStr
  const newShift = modalContext.shift

  const existingInDay = schedules.value.filter(s => s.nhanVien?.id === empId && s.ngayLamViec === dateStr)
  for (const s of existingInDay) {
    const oShift = s.caLamViec
    if (newShift.gioBatDau < oShift.gioKetThuc && newShift.gioKetThuc > oShift.gioBatDau) {
      addModalError.value = `Xung đột khung giờ: Nhân viên đã được xếp vào ca '${oShift.tenCa}' (${formatTime(oShift.gioBatDau)} - ${formatTime(oShift.gioKetThuc)}) trong ngày!`
      return
    }
  }

  savingSchedule.value = true
  try {
    const payload = {
      nhanVienId: empId,
      caLamViecId: newShift.id,
      ngayLamViec: dateStr,
      ghiChu: modalNote.value ? modalNote.value.trim() : null
    }

    await api.post('/api/lich-lam-viec', payload)
    showToast('Phân ca nhân viên thành công!', 'success')
    showAddModal.value = false
    loadWeekSchedule()
  } catch (e) {
    addModalError.value = e?.response?.data?.message || 'Không thể xếp lịch nhân viên.'
  } finally {
    savingSchedule.value = false
  }
}

async function removeAssignment(item) {
  if (isPastDateOrShift(item.ngayLamViec, item.caLamViec?.gioKetThuc)) {
    showToast('Không thể xóa ca làm việc trong quá khứ', 'warning')
    return
  }

  const confirmed = await showConfirm({
    title: 'Xóa phân ca làm việc',
    message: `Bạn có chắc chắn muốn xóa phân ca của nhân viên "${item.nhanVien?.tenNhanVien}" khỏi ca "${item.caLamViec?.tenCa}" ngày ${formatDateVn(item.ngayLamViec)} không?`,
    type: 'warning',
    confirmText: 'Xác nhận xóa',
    cancelText: 'Hủy'
  })

  if (!confirmed) return

  try {
    await api.delete(`/api/lich-lam-viec/${item.id}`)
    showToast('Xóa phân ca thành công!', 'success')
    loadWeekSchedule()
  } catch (e) {
    showAlert({
      title: 'Lỗi',
      message: e?.response?.data?.message || 'Không thể xóa phân ca',
      type: 'error'
    })
  }
}

// Modal Import Excel
const showImportModal = ref(false)
const selectedFile = ref(null)
const importing = ref(false)
const importErrors = ref([])
const fileInputRef = ref(null)

function closeImportModal() {
  showImportModal.value = false
  selectedFile.value = null
  importErrors.value = []
}

function onFileSelected(e) {
  const file = e.target.files?.[0]
  if (file) {
    selectedFile.value = file
    importErrors.value = []
  }
}

function onFileDrop(e) {
  const file = e.dataTransfer.files?.[0]
  if (file) {
    selectedFile.value = file
    importErrors.value = []
  }
}

async function downloadTemplate() {
  try {
    const weekStart = weekDays.value[0]?.dateStr
    const weekEnd = weekDays.value[6]?.dateStr
    const params = {}
    if (weekStart && weekEnd) {
      params.fromDate = weekStart
      params.toDate = weekEnd
    }
    const res = await api.get('/api/lich-lam-viec/template', { params, responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    const fileName = weekStart && weekEnd ? `Lich_Lam_Viec_${weekStart}_den_${weekEnd}.xlsx` : 'Lich_Lam_Viec.xlsx'
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('Đã xuất file Excel theo dữ liệu thực của bảng thành công!', 'success')
  } catch (e) {
    showToast('Không thể tải file template', 'error')
  }
}

async function submitImportExcel() {
  if (!selectedFile.value) return
  importing.value = true
  importErrors.value = []

  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const res = await api.post('/api/lich-lam-viec/import-excel', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    showToast(res.data?.message || 'Nhập lịch nhân viên thành công!', 'success')
    closeImportModal()
    loadWeekSchedule()
  } catch (e) {
    const errData = e?.response?.data
    if (errData?.errors && Array.isArray(errData.errors)) {
      importErrors.value = errData.errors
    } else {
      importErrors.value = [errData?.message || 'Lỗi khi nhập file Excel']
    }
  } finally {
    importing.value = false
  }
}

// ==================== PHÂN CA HÀNG LOẠT ====================
const showBulkModal = ref(false)
const submittingBulk = ref(false)
const bulkMode = ref('week') // 'week' | 'month'
const bulkAnchorDate = ref(new Date().toISOString().split('T')[0])
const bulkAnchorMonth = ref(new Date().toISOString().split('T')[0].substring(0, 7))
const bulkSelectedDates = ref([])
const bulkForm = reactive({
  nhanVienId: '',
  caLamViecId: '',
  ghiChu: ''
})

function openBulkModal() {
  bulkMode.value = 'week'
  bulkAnchorDate.value = selectedAnchorDate.value || new Date().toISOString().split('T')[0]
  bulkAnchorMonth.value = (selectedAnchorDate.value || new Date().toISOString().split('T')[0]).substring(0, 7)
  bulkForm.nhanVienId = ''
  bulkForm.caLamViecId = shifts.value[0]?.id || ''
  bulkForm.ghiChu = ''
  bulkSelectedDates.value = []
  showBulkModal.value = true
}

function closeBulkModal() {
  showBulkModal.value = false
}

function setBulkMode(mode) {
  bulkMode.value = mode
  bulkSelectedDates.value = []
}

const todayYmd = computed(() => formatYmd(new Date()))

const bulkDaysList = computed(() => {
  const todayStr = formatYmd(new Date())
  if (bulkMode.value === 'week') {
    const list = []
    const anchor = bulkAnchorDate.value ? new Date(bulkAnchorDate.value) : new Date()
    const mon = getMondayOfDate(anchor)
    for (let i = 0; i < 7; i++) {
      const cur = new Date(mon)
      cur.setDate(mon.getDate() + i)
      const yyyy = cur.getFullYear()
      const mm = String(cur.getMonth() + 1).padStart(2, '0')
      const dd = String(cur.getDate()).padStart(2, '0')
      const dateStr = `${yyyy}-${mm}-${dd}`

      // Bỏ qua những ngày trước ngày hiện tại
      if (dateStr < todayStr) {
        continue
      }

      const dOfWeek = cur.getDay() // 0: CN, 1: T2, 2: T3, 3: T4, 4: T5, 5: T6, 6: T7
      list.push({
        dateStr,
        displayDate: `${dd}/${mm}/${yyyy}`,
        dayOfWeek: dOfWeek,
        dayOfWeekName: dOfWeek === 0 ? 'CN' : `Thứ ${dOfWeek + 1}`
      })
    }
    return list
  } else {
    const list = []
    const mStr = bulkAnchorMonth.value || formatYmd(new Date()).substring(0, 7)
    const [yearStr, monthStr] = mStr.split('-')
    const y = parseInt(yearStr, 10)
    const m = parseInt(monthStr, 10) - 1
    const lastDay = new Date(y, m + 1, 0).getDate()

    for (let d = 1; d <= lastDay; d++) {
      const cur = new Date(y, m, d)
      const yyyy = cur.getFullYear()
      const mm = String(cur.getMonth() + 1).padStart(2, '0')
      const dd = String(cur.getDate()).padStart(2, '0')
      const dateStr = `${yyyy}-${mm}-${dd}`

      // Bỏ qua những ngày trước ngày hiện tại
      if (dateStr < todayStr) {
        continue
      }

      const dOfWeek = cur.getDay()
      list.push({
        dateStr,
        displayDate: `${dd}/${mm}/${yyyy}`,
        dayOfWeek: dOfWeek,
        dayOfWeekName: dOfWeek === 0 ? 'CN' : `Thứ ${dOfWeek + 1}`
      })
    }
    return list
  }
})

const bulkDateRangeLabel = computed(() => {
  if (bulkMode.value === 'week') {
    const anchor = bulkAnchorDate.value ? new Date(bulkAnchorDate.value) : new Date()
    const mon = getMondayOfDate(anchor)
    const sun = new Date(mon)
    sun.setDate(mon.getDate() + 6)
    return `Tuần từ ${formatYmd(mon)} đến ${formatYmd(sun)}`
  } else {
    const mStr = bulkAnchorMonth.value || formatYmd(new Date()).substring(0, 7)
    const [yearStr, monthStr] = mStr.split('-')
    return `Tháng ${monthStr}/${yearStr}`
  }
})

const bulkMonthCalendarCells = computed(() => {
  const cells = []
  const todayStr = formatYmd(new Date())
  const mStr = bulkAnchorMonth.value || formatYmd(new Date()).substring(0, 7)
  const [yearStr, monthStr] = mStr.split('-')
  const y = parseInt(yearStr, 10)
  const m = parseInt(monthStr, 10) - 1

  const firstDay = new Date(y, m, 1)
  const dayOfWeek = firstDay.getDay() // 0 = CN, 1 = T2, ..., 6 = T7
  const diffToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1
  const startDate = new Date(y, m, 1 - diffToMonday)

  // 6 hàng x 7 cột = 42 ô ngày
  for (let i = 0; i < 42; i++) {
    const cur = new Date(startDate)
    cur.setDate(startDate.getDate() + i)
    const yyyy = cur.getFullYear()
    const mm = String(cur.getMonth() + 1).padStart(2, '0')
    const dd = String(cur.getDate()).padStart(2, '0')
    const dateStr = `${yyyy}-${mm}-${dd}`
    const isCurrentMonth = cur.getMonth() === m
    const isPast = dateStr < todayStr
    const isSelectable = isCurrentMonth && !isPast

    cells.push({
      dateStr,
      dayNumber: cur.getDate(),
      dayOfWeek: cur.getDay(),
      isCurrentMonth,
      isPast,
      isSelectable
    })
  }

  return cells
})

watch(bulkDaysList, (newList) => {
  if (bulkMode.value === 'week') {
    const validSet = new Set(newList.map(d => d.dateStr))
    bulkSelectedDates.value = bulkSelectedDates.value.filter(d => validSet.has(d))
  }
})

watch(bulkMonthCalendarCells, (newCells) => {
  if (bulkMode.value === 'month') {
    const validSet = new Set(newCells.filter(c => c.isSelectable).map(c => c.dateStr))
    bulkSelectedDates.value = bulkSelectedDates.value.filter(d => validSet.has(d))
  }
})

function isBulkDateChecked(dateStr) {
  return bulkSelectedDates.value.includes(dateStr)
}

function toggleBulkDate(dateStr) {
  const idx = bulkSelectedDates.value.indexOf(dateStr)
  if (idx > -1) {
    bulkSelectedDates.value.splice(idx, 1)
  } else {
    bulkSelectedDates.value.push(dateStr)
  }
}

function bulkSelectAll() {
  if (bulkMode.value === 'week') {
    bulkSelectedDates.value = bulkDaysList.value.map(d => d.dateStr)
  } else {
    bulkSelectedDates.value = bulkMonthCalendarCells.value
      .filter(c => c.isSelectable)
      .map(c => c.dateStr)
  }
}

function bulkSelectWorkdays() {
  if (bulkMode.value === 'week') {
    bulkSelectedDates.value = bulkDaysList.value
      .filter(d => d.dayOfWeek >= 1 && d.dayOfWeek <= 5)
      .map(d => d.dateStr)
  } else {
    bulkSelectedDates.value = bulkMonthCalendarCells.value
      .filter(c => c.isSelectable && c.dayOfWeek >= 1 && c.dayOfWeek <= 5)
      .map(c => c.dateStr)
  }
}

function bulkClearSelection() {
  bulkSelectedDates.value = []
}

async function submitBulkSchedule() {
  if (!bulkForm.nhanVienId) {
    showToast('Vui lòng chọn nhân viên!', 'warning')
    return
  }
  if (!bulkForm.caLamViecId) {
    showToast('Vui lòng chọn ca làm việc!', 'warning')
    return
  }
  if (bulkSelectedDates.value.length === 0) {
    showToast('Vui lòng chọn ít nhất 1 ngày làm việc!', 'warning')
    return
  }

  submittingBulk.value = true
  try {
    const payload = {
      nhanVienId: Number(bulkForm.nhanVienId),
      caLamViecId: Number(bulkForm.caLamViecId),
      danhSachNgay: bulkSelectedDates.value,
      ghiChu: bulkForm.ghiChu?.trim() || null
    }

    const res = await api.post('/api/lich-lam-viec/phan-ca-hang-loat', payload)
    const data = res.data || {}
    const successCount = data.successCount || 0
    const skippedCount = data.skippedCount || 0

    if (successCount > 0) {
      let msg = `Đã phân ca thành công cho ${successCount} ngày!`
      if (skippedCount > 0) {
        msg += ` (Bỏ qua ${skippedCount} ngày do trùng lịch hoặc trong quá khứ)`
      }
      showToast(msg, 'success')
      closeBulkModal()
      await loadWeekSchedule()
    } else {
      let errMsg = 'Không thể phân ca cho các ngày đã chọn.'
      if (data.skippedReasons && data.skippedReasons.length > 0) {
        errMsg = data.skippedReasons.join('; ')
      }
      showToast(errMsg, 'error')
    }
  } catch (err) {
    const msg = err.response?.data?.message || err.message || 'Lỗi khi phân ca hàng loạt!'
    showToast(msg, 'error')
  } finally {
    submittingBulk.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadShifts(), loadActiveEmployees(), loadWeekSchedule()])
})
</script>

<style scoped>
.xep-lich-page {
  padding: 1.25rem 2rem 3rem;
  background-color: #f7f5ef;
  min-height: calc(100vh - 48px);
  color: #3d4a50;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.content-card {
  background-color: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  box-sizing: border-box;
}

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

.title-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.12);
}

.page-main-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #2c3e50;
}

/* Toolbar Card */
.toolbar-card-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left-cluster {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.date-navigator-wrap {
  display: flex;
  align-items: center;
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  background-color: #fcfbf9;
  overflow: hidden;
}

.nav-arrow-btn {
  width: 32px;
  height: 36px;
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: var(--blue, #496883);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.nav-arrow-btn:hover {
  background-color: #eaf2f6;
}

.date-picker-clean {
  border: none;
  background: transparent;
  outline: none;
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
  padding: 0 6px;
  height: 36px;
}

.btn-today-pill {
  height: 36px;
  padding: 0 14px;
  border-radius: 20px;
  border: 1px solid #dfd8cc;
  background-color: #ffffff;
  color: #5d6c73;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-today-pill:hover {
  background-color: #f0f4f8;
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
}

.week-range-label {
  font-size: 0.88rem;
  color: #55656d;
}

.week-range-label strong {
  color: #1e293b;
}

.toolbar-right-cluster {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.view-mode-toggle {
  display: flex;
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  overflow: hidden;
  background: #fcfbf9;
}

.mode-btn {
  border: none;
  background: transparent;
  padding: 7px 14px;
  font-size: 0.82rem;
  font-weight: 600;
  color: #556268;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: var(--blue, #496883);
  color: #ffffff;
  font-weight: 700;
}

.btn-export-template {
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  color: #556268;
  font-size: 0.84rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-export-template:hover {
  background-color: #f0f4f8;
}

.btn-import-excel {
  height: 36px;
  padding: 0 15px;
  border-radius: 8px;
  border: 1px solid var(--blue, #496883);
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 0.85rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
}

.btn-import-excel:hover {
  background-color: #3b576e;
}

/* Lưới lịch tuần (Calendar Table) */
.schedule-grid-card {
  padding: 0;
  overflow: hidden;
}

.schedule-table-wrap {
  overflow-x: auto;
}

.schedule-grid-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.schedule-grid-table th,
.schedule-grid-table td {
  border: 1px solid #f1ece5;
}

.schedule-grid-table th {
  background-color: #f7f5ef !important;
  color: var(--blue, #496883) !important;
  padding: 10px 8px;
  text-align: center;
}

.col-shift-header {
  width: 140px;
  font-size: 0.85rem;
  font-weight: 700;
}

.col-day-header {
  min-width: 135px;
}

.day-header-title {
  font-size: 0.88rem;
  font-weight: 700;
}

.day-header-sub {
  font-size: 0.76rem;
  color: #7d8993;
  margin-top: 2px;
}

.today-header {
  background-color: #eaf2f6 !important;
  color: var(--blue, #496883) !important;
}

.grid-shift-row {
  height: 120px;
}

.shift-info-cell {
  background-color: #faf8f5;
  padding: 12px 10px;
  vertical-align: middle;
  text-align: center;
}

.shift-name-text {
  display: block;
  font-size: 0.95rem;
  color: #1e293b;
  margin-bottom: 4px;
}

.shift-time-badge {
  display: inline-block;
  font-size: 0.74rem;
  font-weight: 600;
  background-color: #ede7dd;
  color: #726252;
  padding: 2px 6px;
  border-radius: 4px;
}

.schedule-day-cell {
  padding: 8px;
  vertical-align: top;
  position: relative;
  background-color: #ffffff;
  transition: background-color 0.15s;
}

.schedule-day-cell:hover {
  background-color: #fdfcf9;
}

.today-cell {
  background-color: #fffdf9 !important;
}

.past-day-cell {
  background-color: #fcfbfa;
  opacity: 0.85;
}

.employee-tags-stack {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 30px;
}

.employee-chip-card {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 4px 6px;
  position: relative;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
}

.chip-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chip-avatar.small {
  width: 22px;
  height: 22px;
  font-size: 0.68rem;
}

.chip-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}

.chip-name {
  font-size: 0.78rem;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chip-code {
  font-size: 0.68rem;
  color: #64748b;
}

.btn-remove-chip {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 0.75rem;
  cursor: pointer;
  padding: 0 2px;
}

.btn-remove-chip:hover {
  color: #ef4444;
}

.btn-add-schedule-cell {
  position: absolute;
  bottom: 6px;
  right: 6px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px dashed var(--blue, #496883);
  background-color: #ffffff;
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-schedule-cell:hover:not(:disabled) {
  background-color: #eaf2f6;
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
}

.btn-add-schedule-cell:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  border-style: solid;
}

/* Table View styling */
.schedule-table-card {
  padding: 1.25rem;
}

.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.minimal-styled-table th {
  background-color: #f7f5ef !important;
  color: var(--blue, #496883) !important;
  font-weight: 700;
  font-size: 0.78rem;
  letter-spacing: 0.5px;
  padding: 10px 14px;
  text-transform: uppercase;
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
}

.table-data-row:hover {
  background-color: #f8fafc;
}

.minimal-styled-table td {
  padding: 12px 14px;
  vertical-align: middle;
}

.badge-shift-title {
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  font-size: 0.78rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
}

.employee-row-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-delete-row {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid #fee2e2;
  background-color: #fff1f2;
  color: #e11d48;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete-row:hover:not(:disabled) {
  background-color: #ffe4e6;
}

.btn-delete-row:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Modals */
.modal-backdrop-clean {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 1rem;
}

.modal-dialog-clean {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

.modal-dialog-clean.modal-wide {
  max-width: 620px;
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.modal-header-clean {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.1rem 1.4rem;
  border-bottom: 1px solid #f1ece5;
  background-color: #faf8f5;
}

.modal-title-clean {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #2c3e50;
}

.btn-close-modal {
  border: none;
  background: transparent;
  font-size: 1.2rem;
  color: #8c979d;
  cursor: pointer;
}

.modal-body-clean {
  padding: 1.25rem 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.shift-context-box {
  background-color: #fbf9f6;
  border: 1px solid #ebd9c8;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.context-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.86rem;
}

.context-lbl {
  color: #64748b;
}

.context-val {
  color: #1e293b;
}

.form-group-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-field-label {
  font-size: 0.84rem;
  font-weight: 700;
  color: #475569;
}

.required-star {
  color: #ef4444;
}

.form-control-minimal {
  width: 100%;
  height: 38px;
  background-color: #fcfbf9;
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 0.88rem;
  color: #334155;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
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

.error-text-red {
  color: #dc2626;
  font-size: 0.78rem;
  font-weight: 600;
}

.modal-footer-clean {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 1rem 1.4rem;
  border-top: 1px solid #f1ece5;
  background-color: #faf8f5;
}

.btn-cancel-modal {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid #d1c8bd;
  background: #ffffff;
  color: #556268;
  font-weight: 600;
  cursor: pointer;
}

.btn-save-modal {
  padding: 8px 22px;
  border-radius: 8px;
  border: 1px solid var(--blue, #496883);
  background: var(--blue, #496883);
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save-modal:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Import Excel Box */
.import-help-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 14px;
  gap: 10px;
}

.help-text {
  font-size: 0.82rem;
  color: #475569;
}

.btn-download-sample {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: #ffffff;
  border: 1px solid #cbd5e1;
  color: #1e293b;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.file-dropzone-box {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 1.8rem;
  text-align: center;
  cursor: pointer;
  background-color: #fcfbf9;
  transition: all 0.2s;
}

.file-dropzone-box:hover {
  border-color: var(--blue, #496883);
  background-color: #f8fafc;
}

.file-input-hidden {
  display: none;
}

.upload-icon-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.dropzone-prompt {
  font-size: 0.86rem;
  color: #64748b;
}

.dropzone-prompt strong {
  color: var(--blue, #496883);
}

.file-selected-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #e0f2fe;
  color: #0369a1;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.85rem;
}

.file-name-bold {
  font-weight: 700;
}

.file-size-tag {
  background-color: #bae6fd;
  padding: 1px 6px;
  border-radius: 10px;
  font-size: 0.74rem;
}

.import-error-list-container {
  max-height: 180px;
  overflow-y: auto;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 14px;
}

.error-list-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #dc2626;
  font-size: 0.86rem;
  margin-bottom: 6px;
}

.error-items-ul {
  margin: 0;
  padding-left: 20px;
  color: #b91c1c;
  font-size: 0.8rem;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
/* Nút Phân ca hàng loạt Toolbar */
.btn-bulk-schedule {
  height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid var(--blue, #496883);
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 0.85rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
}

.btn-bulk-schedule:hover {
  background-color: #3b576e;
  border-color: #3b576e;
}

/* Modal Phân ca hàng loạt */
.bulk-modal-dialog {
  max-width: 920px;
  width: 95%;
  padding: 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.bulk-modal-header {
  background-color: var(--blue, #496883);
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bulk-modal-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #ffffff;
}

.bulk-modal-subtitle {
  margin: 4px 0 0 0;
  font-size: 0.82rem;
  color: #eaf2f6;
}

.bulk-btn-close {
  width: 28px;
  height: 28px;
  background-color: #ffffff;
  color: var(--blue, #496883);
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.bulk-btn-close:hover {
  background-color: #f0f4f8;
}

.bulk-modal-body {
  padding: 24px;
  display: flex;
  gap: 24px;
  background-color: #ffffff;
}

.bulk-left-col {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bulk-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bulk-field-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #2c3e50;
}

.bulk-mode-toggle-group {
  display: flex;
  border: 1px solid var(--blue, #496883);
  border-radius: 6px;
  overflow: hidden;
}

.bulk-mode-toggle-btn {
  flex: 1;
  height: 34px;
  border: none;
  background: #ffffff;
  color: var(--blue, #496883);
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.bulk-mode-toggle-btn.active {
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-weight: 700;
}

.bulk-control-input {
  width: 100%;
  height: 36px;
  border: 1px solid #dfd8cc;
  background-color: #fcfbf9;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 0.86rem;
  color: #3d4a50;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s;
}

.bulk-control-input:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.15);
  background-color: #ffffff;
}

.bulk-range-hint-text {
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 2px;
}

.bulk-quick-select-card {
  border: 1px solid #e9e5db;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background-color: #fcfbf9;
}

.bulk-quick-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bulk-quick-title {
  font-size: 0.84rem;
  font-weight: 600;
  color: #3d4a50;
}

.bulk-quick-badge {
  font-size: 0.78rem;
  color: var(--blue, #496883);
  background-color: #eaf2f6;
  border: 1px solid #d5e4ec;
  border-radius: 4px;
  padding: 2px 8px;
  font-weight: 600;
}

.bulk-quick-btn-group {
  display: flex;
  gap: 6px;
}

.bulk-quick-action-btn {
  flex: 1;
  padding: 6px 0;
  border: 1px solid #dfd8cc;
  border-radius: 4px;
  background: #ffffff;
  color: #556268;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.bulk-quick-action-btn:hover {
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
  background-color: #f0f4f8;
}

.bulk-right-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.bulk-right-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 14px 0;
}

.bulk-cards-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.bulk-cards-container.cards-scrollable {
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}

.bulk-day-item-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e9e5db;
  border-radius: 8px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}

.bulk-day-item-card:hover {
  border-color: var(--blue2, #6e96ad);
  background-color: #fafcfe;
}

.bulk-day-item-card.selected {
  border-color: var(--blue, #496883);
  background-color: #eaf2f6;
}

.bulk-day-native-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--blue, #496883);
}

.bulk-day-text-wrap {
  display: flex;
  flex-direction: column;
}

.bulk-day-weekday-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #2c3e50;
}

.bulk-day-date-number {
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 2px;
}

.bulk-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 24px;
  border-top: 1px solid #f1ece5;
  background-color: #faf8f5;
}

.bulk-btn-cancel {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #d1c8bd;
  border-radius: 6px;
  background-color: #ffffff;
  color: #556268;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.bulk-btn-cancel:hover {
  background-color: #f1f5f9;
}

.bulk-btn-submit {
  height: 36px;
  padding: 0 20px;
  border: 1px solid var(--blue, #496883);
  border-radius: 6px;
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
}

.bulk-btn-submit:hover:not(:disabled) {
  background-color: #3b576e;
  border-color: #3b576e;
}

.bulk-btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bulk-no-days-alert {
  padding: 24px 16px;
  background-color: #fcfbf9;
  border: 1px dashed #dfd8cc;
  border-radius: 8px;
  color: #64748b;
  font-size: 0.88rem;
  text-align: center;
}

/* Giao diện Lịch Tháng 7 cột */
.month-calendar-wrap {
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  overflow: hidden;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.month-calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background-color: var(--blue, #496883);
  color: #ffffff;
  text-align: center;
  font-weight: 700;
  font-size: 0.82rem;
}

.month-header-cell {
  padding: 10px 4px;
  border-right: 1px solid rgba(255, 255, 255, 0.15);
}

.month-header-cell:last-child {
  border-right: none;
}

.month-calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background-color: #e9e5db;
  gap: 1px;
}

.month-day-cell {
  background-color: #ffffff;
  min-height: 52px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  transition: all 0.15s;
}

.month-day-cell.selectable {
  cursor: pointer;
}

.month-day-cell.selectable:hover {
  background-color: #f7fafc;
}

.month-day-cell.selected {
  background-color: #eaf2f6 !important;
}

.month-day-cell.other-month {
  background-color: #fcfbf9;
  cursor: default;
}

.month-day-cell.past-day {
  background-color: #fcfbf9;
  cursor: not-allowed;
}

.month-cell-checkbox {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: var(--blue, #496883);
}

.month-day-cell:not(.selectable) .month-cell-checkbox {
  opacity: 0.25;
  cursor: not-allowed;
  pointer-events: none;
}

.month-cell-day-num {
  font-size: 0.88rem;
  font-weight: 600;
  color: #2c3e50;
}

.month-day-cell.other-month .month-cell-day-num,
.month-day-cell.past-day .month-cell-day-num {
  color: #cbd5e1;
  font-weight: 500;
}
</style>

