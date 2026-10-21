<template>
  <div class="giao-ca-page">
    <!-- Thanh điều hướng và tiêu đề trên cùng -->
    <div class="page-top-bar">
      <div class="title-with-icon">
        <div class="title-icon-box">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
          </svg>
        </div>
        <div>
          <h1 class="page-main-title">Quản lý Vào ca & Giao ca</h1>
        </div>
      </div>

      <div class="top-nav-links">
        <button type="button" class="btn-history-link" @click="$router.push('/lich-su-ca')">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 14 14" />
          </svg>
          <span>Xem sổ lịch sử ca →</span>
        </button>
        <button type="button" class="btn-pos-link" @click="$router.push('/ban-hang')">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
          <span>Màn hình Bán hàng (POS)</span>
        </button>
      </div>
    </div>

    <!-- KHU VỰC 1: TRƯỜNG HỢP NHÂN VIÊN ĐANG TRONG CA LÀM VIỆC -->
    <div v-if="currentOpenShift" class="shift-active-container">
      <!-- Thẻ trạng thái ca đang mở -->
      <div class="content-card current-shift-card">
        <div class="shift-card-header">
          <div class="shift-badge-active">
            <span class="pulse-dot"></span>
            <span>ĐANG TRONG CA TRỰC</span>
          </div>
          <div class="shift-code-tag">
            Mã ca: <strong>{{ currentOpenShift.maGiaoCa || ('GC-' + currentOpenShift.id) }}</strong>
          </div>
        </div>

        <div class="shift-info-grid">
          <div class="info-block">
            <span class="info-label">Ca làm việc</span>
            <strong class="info-value text-brown">{{ currentOpenShift.caLamViec?.tenCa || 'Chưa xác định' }}</strong>
            <small class="info-sub">{{ formatTime(currentOpenShift.caLamViec?.gioBatDau) }} - {{ formatTime(currentOpenShift.caLamViec?.gioKetThuc) }}</small>
          </div>

          <div class="info-block">
            <span class="info-label">Nhân viên trực</span>
            <strong class="info-value">{{ currentOpenShift.nhanVien?.tenNhanVien }}</strong>
            <small class="info-sub">{{ currentOpenShift.nhanVien?.maNhanVien }} ({{ currentOpenShift.nhanVien?.tenTaiKhoan }})</small>
          </div>

          <div class="info-block">
            <span class="info-label">Thời gian mở ca</span>
            <strong class="info-value">{{ formatDateTime(currentOpenShift.thoiGianMoCa) }}</strong>
            <small class="info-sub">Hôm nay</small>
          </div>

          <div class="info-block">
            <span class="info-label">Ghi chú</span>
            <span class="info-value text-note">{{ currentOpenShift.ghiChu || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- Khối tóm tắt tiền két thời gian thực -->
      <div class="content-card cash-metrics-card">
        <div class="metrics-header">
          <div class="metrics-title-wrap">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="4" width="20" height="16" rx="2" />
              <line x1="2" y1="10" x2="22" y2="10" />
            </svg>
            <h2 class="metrics-title">Két tiền ca trực (Thời gian thực)</h2>
          </div>
          <button type="button" class="btn-refresh-clean" @click="loadShiftSummary(currentOpenShift.id)" title="Cập nhật số liệu mới nhất">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Làm mới số liệu két</span>
          </button>
        </div>

        <div class="metrics-grid">
          <!-- 1. Tiền mặt đầu ca -->
          <div class="metric-box bg-warm">
            <span class="metric-name">Tiền mặt đầu ca</span>
            <strong class="metric-val text-brown">{{ formatCurrency(currentOpenShift.tienMatDauCa || 0) }}</strong>
          </div>

          <!-- 2. Tiền mặt bán trong ca -->
          <div class="metric-box bg-green">
            <span class="metric-name">Tiền mặt bán tại quầy</span>
            <strong class="metric-val text-green">{{ formatCurrency(shiftSummary?.tienMatHeThong || 0) }}</strong>
          </div>

          <!-- 3. Chuyển khoản trong ca -->
          <div class="metric-box bg-blue">
            <span class="metric-name">Chuyển khoản / QR quầy</span>
            <strong class="metric-val text-blue">{{ formatCurrency(shiftSummary?.tienChuyenKhoanHeThong || 0) }}</strong>
          </div>

          <!-- 4. Tiền mặt dự kiến có trong két -->
          <div class="metric-box bg-highlight">
            <span class="metric-name">Tiền mặt dự kiến trong két</span>
            <strong class="metric-val text-primary-highlight">{{ formatCurrency(calculatedExpectedCash) }}</strong>
          </div>
        </div>
      </div>

      <!-- Khối thao tác Kết thúc ca & Bàn giao -->
      <div class="content-card close-shift-card">
        <div class="card-section-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
          <h2>Kết thúc ca & Bàn giao tiền mặt</h2>
        </div>

        <form @submit.prevent="submitDongCa" class="close-shift-form">
          <div class="form-cols-2">
            <!-- 1. Tiền chi trong ca -->
            <div class="form-item">
              <label class="field-label">Tiền chi trong ca</label>
              <div class="input-currency-box">
                <input
                  type="text"
                  inputmode="numeric"
                  v-model="displayTienChiTrongCa"
                  placeholder="0"
                  class="form-control-minimal"
                />
                <span class="currency-tag">VNĐ</span>
              </div>
            </div>

            <!-- 2. Tiền mặt thực tế đếm được cuối ca (BẮT BUỘC) -->
            <div class="form-item">
              <label class="field-label">Tiền mặt thực tế kiểm đếm cuối ca <span class="required-star">*</span></label>
              <div class="input-currency-box">
                <input
                  type="text"
                  inputmode="numeric"
                  v-model="displayTienMatCuoiCa"
                  placeholder="0"
                  class="form-control-minimal"
                  :class="{ 'input-error-border': dongCaForm.tienMatCuoiCa === null }"
                  required
                />
                <span class="currency-tag">VNĐ</span>
              </div>
            </div>
          </div>

          <!-- Hộp trạng thái đối soát Chênh lệch -->
          <div
            v-if="dongCaForm.tienMatCuoiCa !== null"
            class="diff-result-banner"
            :class="{
              'diff-match': calculatedDiff === 0,
              'diff-surplus': calculatedDiff > 0,
              'diff-deficit': calculatedDiff < 0
            }"
          >
            <div class="diff-banner-left">
              <span class="diff-icon">{{ calculatedDiff === 0 ? '✓' : '⚠️' }}</span>
              <div>
                <strong class="diff-title">
                  {{
                    calculatedDiff === 0
                      ? 'Tiền mặt kiểm đếm KHỚP 100% với hệ thống'
                      : (calculatedDiff > 0 ? 'Phát hiện THỪA TIỀN so với dự kiến' : 'Phát hiện THIẾU TIỀN so với dự kiến')
                  }}
                </strong>
                <p class="diff-desc">
                  Thực tế: {{ formatCurrency(dongCaForm.tienMatCuoiCa) }} | Dự kiến: {{ formatCurrency(calculatedExpectedCash) }}
                </p>
              </div>
            </div>
            <div class="diff-banner-right">
              <span class="diff-amount-tag">
                {{ calculatedDiff > 0 ? '+' : '' }}{{ formatCurrency(calculatedDiff) }}
              </span>
            </div>
          </div>

          <!-- Ô nhập lý do giải trình nếu chênh lệch khác 0 -->
          <div v-if="calculatedDiff !== 0" class="form-item diff-reason-item">
            <label class="field-label text-warn">
              Lý do chênh lệch tiền mặt <span class="required-star">*</span>
            </label>
            <textarea
              v-model="dongCaForm.lyDoChenhLech"
              rows="2"
              placeholder="Nhập lý do giải trình chênh lệch..."
              class="form-control-minimal textarea-minimal"
              :class="{ 'input-error-border': !dongCaForm.lyDoChenhLech || !dongCaForm.lyDoChenhLech.trim() }"
            ></textarea>
          </div>

          <div class="form-cols-2">
            <!-- 3. Chọn nhân viên nhận bàn giao ca -->
            <div class="form-item">
              <label class="field-label">Nhân viên nhận bàn giao</label>
              <div class="select-wrapper-minimal">
                <select v-model="dongCaForm.nhanVienNhanId" class="form-control-minimal">
                  <option value="">-- Quản trị viên / Không bàn giao ca tiếp theo --</option>
                  <option
                    v-for="nv in handoverEmployees"
                    :key="nv.id"
                    :value="nv.id"
                  >
                    {{ nv.maNhanVien }} - {{ nv.tenNhanVien }} ({{ nv.tenTaiKhoan }})
                  </option>
                </select>
              </div>
            </div>

            <!-- 4. Ghi chú chung khi bàn giao -->
            <div class="form-item">
              <label class="field-label">Ghi chú bàn giao</label>
              <input
                type="text"
                v-model="dongCaForm.ghiChu"
                placeholder="Ghi chú bàn giao (nếu có)..."
                class="form-control-minimal"
              />
            </div>
          </div>

          <div class="form-actions-bar">
            <button
              type="submit"
              class="btn-submit-close-shift"
              :disabled="submittingDongCa || !canSubmitDongCa"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>{{ submittingDongCa ? 'Đang đóng ca...' : 'Xác nhận Đóng ca & Bàn giao tiền' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- KHU VỰC 2: TRƯỜNG HỢP NHÂN VIÊN CHƯA MỞ CA LÀM VIỆC -->
    <div v-else class="shift-inactive-container">
      <div class="content-card open-shift-card">
        <div class="open-shift-header">
          <div class="inactive-badge">
            <span class="idle-dot"></span>
            <span>CHƯA VÀO CA LÀM VIỆC</span>
          </div>
          <h2 class="open-card-title">Mở ca làm việc & Kiểm đếm tiền ban đầu</h2>
        </div>

        <!-- Banner thông tin tiền mặt từ ca trước vừa đóng (nếu có) -->
        <div v-if="lastClosedShift" class="last-shift-suggestion">
          <div class="suggestion-left">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
            <div>
              <strong>Ca trước ({{ lastClosedShift.caLamViec?.tenCa || lastClosedShift.maGiaoCa }})</strong> vừa bàn giao kết ca lúc
              {{ formatDateTime(lastClosedShift.thoiGianDongCa) }}.
              <div class="suggestion-cash">
                Tiền mặt bàn giao lại trong két: <strong>{{ formatCurrency(lastClosedShift.tienMatCuoiCa || 0) }}</strong>
                (bởi nhân viên {{ lastClosedShift.nhanVien?.tenNhanVien }}).
              </div>
            </div>
          </div>
          <button type="button" class="btn-use-last-cash" @click="applyLastClosedCash">
            Dùng số tiền này ({{ formatCurrency(lastClosedShift.tienMatCuoiCa || 0) }})
          </button>
        </div>

        <!-- Form Mở ca -->
        <form @submit.prevent="submitMoCa" class="open-shift-form">
          <div class="form-cols-2">
            <!-- 1. Chọn ca làm việc -->
            <div class="form-item">
              <label class="field-label">Ca làm việc <span class="required-star">*</span></label>
              <div class="select-wrapper-minimal">
                <select v-model="moCaForm.caLamViecId" class="form-control-minimal" required>
                  <option value="">-- Chọn ca làm việc hôm nay --</option>
                  <option
                    v-for="ca in activeShifts"
                    :key="ca.id"
                    :value="ca.id"
                    :disabled="!isUserAdmin && !getShiftStatus(ca).canOpen"
                  >
                    {{ ca.tenCa }} ({{ formatTime(ca.gioBatDau) }} - {{ formatTime(ca.gioKetThuc) }})
                    {{ !getShiftStatus(ca).canOpen ? (' - [' + getShiftStatus(ca).label + ']') : ' - [Đang trong ca]' }}
                  </option>
                </select>
              </div>
              <div v-if="selectedShiftStatus && !selectedShiftStatus.canOpen" class="shift-time-warning">
                ⚠️ {{ selectedShiftStatus.label }}. Bạn chỉ có thể mở ca trước tối đa 30 phút!
              </div>
            </div>

            <!-- 2. Chọn nhân viên trực -->
            <div class="form-item">
              <label class="field-label">Nhân viên trực quầy <span class="required-star">*</span></label>
              <!-- Nếu là Nhân viên: Khóa cứng theo tài khoản đang đăng nhập, không được mở cho nhân viên khác -->
              <div v-if="!isUserAdmin" class="locked-staff-box">
                <div class="staff-avatar-mini">{{ getInitial(currentUser?.tenNhanVien) }}</div>
                <div class="staff-text-locked">
                  <strong class="staff-name-locked">{{ currentUser?.tenNhanVien }}</strong>
                  <span class="staff-code-locked">Mã: {{ currentUser?.maNhanVien || currentUser?.tenTaiKhoan }} (Tài khoản đang đăng nhập)</span>
                </div>
              </div>
              <!-- Nếu là Quản trị viên: Có quyền chọn nhân viên mở ca thay -->
              <div v-else class="select-wrapper-minimal">
                <select v-model="moCaForm.nhanVienId" class="form-control-minimal" required>
                  <option value="">-- Chọn nhân viên --</option>
                  <option v-for="nv in activeEmployees" :key="nv.id" :value="nv.id">
                    {{ nv.maNhanVien }} - {{ nv.tenNhanVien }} ({{ nv.tenTaiKhoan }})
                  </option>
                </select>
              </div>
            </div>
          </div>

          <div class="form-cols-2">
            <!-- 3. Tiền mặt đầu ca -->
            <div class="form-item">
              <label class="field-label">Số tiền mặt đầu ca trong két <span class="required-star">*</span></label>
              <div class="input-currency-box">
                <input
                  type="text"
                  inputmode="numeric"
                  v-model="displayTienMatDauCa"
                  placeholder="0"
                  class="form-control-minimal"
                  required
                />
                <span class="currency-tag">VNĐ</span>
              </div>
            </div>

            <!-- 4. Ghi chú mở ca -->
            <div class="form-item">
              <label class="field-label">Ghi chú khi mở ca</label>
              <input
                type="text"
                v-model="moCaForm.ghiChu"
                placeholder="Ghi chú mở ca (nếu có)..."
                class="form-control-minimal"
              />
            </div>
          </div>

          <div class="form-actions-bar">
            <button
              type="submit"
              class="btn-submit-open-shift"
              :disabled="!canSubmitMoCa"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="16" />
                <line x1="8" y1="12" x2="16" y2="12" />
              </svg>
              <span>{{ submittingMoCa ? 'Đang mở ca...' : 'Bắt đầu ca làm việc ngay' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- KHU VỰC 3: CÁC CA ĐÃ GIAO GẦN NHẤT (MINI AUDIT LOG) -->
    <div class="content-card recent-shifts-card">
      <div class="recent-header">
        <h3 class="recent-title">Các lượt giao ca gần nhất</h3>
        <button type="button" class="btn-text-more" @click="$router.push('/lich-su-ca')">
          Xem tất cả lịch sử →
        </button>
      </div>

      <div class="table-responsive-clean">
        <table class="minimal-styled-table">
          <thead>
            <tr class="header-brown-beige">
              <th style="width: 130px">MÃ CA</th>
              <th style="min-width: 150px">NGƯỜI GIAO</th>
              <th style="min-width: 150px">NGƯỜI NHẬN</th>
              <th style="width: 120px">CA</th>
              <th style="width: 150px; text-align: center">THỜI GIAN MỞ</th>
              <th style="width: 150px; text-align: center">THỜI GIAN ĐÓNG</th>
              <th style="width: 140px; text-align: right">TIỀN CUỐI CA</th>
              <th style="width: 120px; text-align: right">CHÊNH LỆCH</th>
              <th style="width: 110px; text-align: center">BIÊN BẢN</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="recentShifts.length === 0">
              <td colspan="9" style="text-align: center; padding: 2rem; color: #8c979d;">
                Chưa có dữ liệu ca giao nào gần đây.
              </td>
            </tr>
            <tr v-for="item in recentShifts" :key="item.id" class="table-data-row">
              <td><strong>{{ item.maGiaoCa || ('GC-' + item.id) }}</strong></td>
              <td>{{ item.nhanVien?.tenNhanVien }}</td>
              <td>{{ item.nhanVienNhan?.tenNhanVien || 'Quản lý / Cuối ngày' }}</td>
              <td>{{ item.caLamViec?.tenCa }}</td>
              <td style="text-align: center">{{ formatDateTime(item.thoiGianMoCa) }}</td>
              <td style="text-align: center">
                <span v-if="item.thoiGianDongCa">{{ formatDateTime(item.thoiGianDongCa) }}</span>
                <span v-else class="badge-running">Đang trực</span>
              </td>
              <td style="text-align: right">
                {{ (item.trangThai === 0 || item.trangThai === 2) ? formatCurrency(item.tienMatCuoiCa) : 'Chưa chốt' }}
              </td>
              <td style="text-align: right">
                <span
                  v-if="item.trangThai === 0 || item.trangThai === 2"
                  class="diff-pill"
                  :class="Number(item.chenhLech || 0) === 0 ? 'diff-pill-zero' : 'diff-pill-warn'"
                >
                  {{ Number(item.chenhLech || 0) > 0 ? '+' : '' }}{{ formatCurrency(item.chenhLech || 0) }}
                </span>
                <span v-else class="text-muted">-</span>
              </td>
              <td style="text-align: center">
                <button
                  type="button"
                  class="btn-action-view"
                  title="In / Xem biên bản giao ca"
                  @click="openReceiptModal(item)"
                >
                  📄 In
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL IN BIÊN BẢN BÀN GIAO CA (RECEIPT MODAL) -->
    <div v-if="showReceiptModal" class="modal-backdrop-clean" @click.self="showReceiptModal = false">
      <div class="modal-dialog-clean modal-receipt">
        <div class="modal-header-clean no-print">
          <h3 class="modal-title-clean">Biên bản bàn giao ca làm việc</h3>
          <button type="button" class="btn-close-modal" @click="showReceiptModal = false">✕</button>
        </div>

        <div class="modal-body-clean printable-receipt" id="print-receipt-area">
          <div class="receipt-header">
            <h2 class="receipt-brand">FF T-SHIRT</h2>
            <p class="receipt-subtitle">BIÊN BẢN BÀN GIAO CA LÀM VIỆC</p>
            <div class="receipt-code">Mã giao ca: <strong>{{ receiptShift?.maGiaoCa || ('GC-' + receiptShift?.id) }}</strong></div>
          </div>

          <div class="receipt-divider"></div>

          <div class="receipt-meta-grid">
            <div class="receipt-meta-row">
              <span class="meta-label">Ca làm việc:</span>
              <strong class="meta-val">{{ receiptShift?.caLamViec?.tenCa }}</strong>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Người giao ca:</span>
              <strong class="meta-val">{{ receiptShift?.nhanVien?.tenNhanVien }} ({{ receiptShift?.nhanVien?.maNhanVien }})</strong>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Người nhận ca:</span>
              <strong class="meta-val">{{ receiptShift?.nhanVienNhan?.tenNhanVien ? (receiptShift?.nhanVienNhan?.tenNhanVien + ' (' + receiptShift?.nhanVienNhan?.maNhanVien + ')') : 'Quản lý / Cuối ngày' }}</strong>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Thời gian mở ca:</span>
              <span class="meta-val">{{ formatDateTime(receiptShift?.thoiGianMoCa) }}</span>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Thời gian kết ca:</span>
              <span class="meta-val">{{ receiptShift?.thoiGianDongCa ? formatDateTime(receiptShift?.thoiGianDongCa) : 'Đang trực' }}</span>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Ghi chú giao ca:</span>
              <span class="meta-val">{{ receiptShift?.ghiChu || '-' }}</span>
            </div>
          </div>

          <div class="receipt-divider"></div>

          <div class="receipt-financial-table">
            <div class="fin-row">
              <span>1. Tiền mặt đầu ca:</span>
              <strong>{{ formatCurrency(receiptShift?.tienMatDauCa || 0) }}</strong>
            </div>
            <div class="fin-row">
              <span>2. Tiền mặt thu tại quầy:</span>
              <strong>{{ formatCurrency(receiptShift?.tongTienTrongCa || 0) }}</strong>
            </div>
            <div class="fin-row">
              <span>3. Doanh thu chuyển khoản:</span>
              <strong>{{ formatCurrency(receiptShift?.tienChuyenKhoan || 0) }}</strong>
            </div>
            <div v-if="Number(receiptShift?.tienChiTrongCa || 0) > 0" class="fin-row text-red">
              <span>4. Tiền chi trong ca:</span>
              <strong>-{{ formatCurrency(receiptShift?.tienChiTrongCa || 0) }}</strong>
            </div>
            <div class="fin-row fin-expected">
              <span>5. Tiền mặt dự kiến có trong két:</span>
              <strong>{{ formatCurrency(calculateReceiptExpected(receiptShift)) }}</strong>
            </div>
            <div class="fin-row fin-actual">
              <span>6. Tiền mặt thực tế kiểm đếm:</span>
              <strong>{{ formatCurrency(receiptShift?.tienMatCuoiCa || 0) }}</strong>
            </div>
            <div class="fin-row fin-diff" :class="Number(receiptShift?.chenhLech || 0) === 0 ? 'text-green' : 'text-warn'">
              <span>7. Chênh lệch tiền mặt:</span>
              <strong>{{ Number(receiptShift?.chenhLech || 0) > 0 ? '+' : '' }}{{ formatCurrency(receiptShift?.chenhLech || 0) }}</strong>
            </div>
            <div v-if="receiptShift?.lyDoChenhLech" class="fin-reason">
              <span>* Lý do giải trình chênh lệch:</span>
              <em>{{ receiptShift?.lyDoChenhLech }}</em>
            </div>
          </div>

          <div class="receipt-signatures">
            <div class="sig-col">
              <p class="sig-title">NGƯỜI GIAO CA</p>
              <p class="sig-sub">(Ký và ghi rõ họ tên)</p>
              <div class="sig-space"></div>
              <p class="sig-name">{{ receiptShift?.nhanVien?.tenNhanVien }}</p>
            </div>
            <div class="sig-col">
              <p class="sig-title">NGƯỜI NHẬN CA</p>
              <p class="sig-sub">(Ký và ghi rõ họ tên)</p>
              <div class="sig-space"></div>
              <p class="sig-name">{{ receiptShift?.nhanVienNhan?.tenNhanVien || 'Quản lý' }}</p>
            </div>
          </div>
        </div>

        <div class="modal-footer-clean no-print">
          <button type="button" class="btn-cancel-modal" @click="showReceiptModal = false">Đóng</button>
          <button type="button" class="btn-print-action" @click="printReceipt">
            🖨 In biên bản bàn giao
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { getCurrentUser, getRoleCode } from '@/utils/auth.js'
import { showAlert, showConfirm, showToast } from '@/utils/dialog.js'

const router = useRouter()
const currentUser = getCurrentUser()
const roleCode = getRoleCode()
const isUserAdmin = computed(() => roleCode === 'ADMIN')

// Dữ liệu danh mục
const activeEmployees = ref([])
const activeShifts = ref([])
const currentOpenShift = ref(null)
const shiftSummary = ref(null)
const lastClosedShift = ref(null)
const recentShifts = ref([])

// Form Mở ca
const submittingMoCa = ref(false)
const moCaForm = reactive({
  nhanVienId: '',
  caLamViecId: '',
  tienMatDauCa: 0,
  ghiChu: ''
})

// Form Đóng ca
const submittingDongCa = ref(false)
const dongCaForm = reactive({
  nhanVienNhanId: '',
  tienChiTrongCa: 0,
  tienMatCuoiCa: null,
  lyDoChenhLech: '',
  ghiChu: ''
})

// Computed 2 chiều để format và validate trực tiếp số tiền ngay trong ô input
const displayTienMatDauCa = computed({
  get() {
    if (moCaForm.tienMatDauCa === null || moCaForm.tienMatDauCa === undefined || moCaForm.tienMatDauCa === '') return ''
    return Number(moCaForm.tienMatDauCa).toLocaleString('vi-VN')
  },
  set(val) {
    if (!val) {
      moCaForm.tienMatDauCa = 0
      return
    }
    const clean = String(val).replace(/\D/g, '')
    moCaForm.tienMatDauCa = clean !== '' ? parseInt(clean, 10) : 0
  }
})

const displayTienChiTrongCa = computed({
  get() {
    if (dongCaForm.tienChiTrongCa === null || dongCaForm.tienChiTrongCa === undefined || dongCaForm.tienChiTrongCa === '') return ''
    if (dongCaForm.tienChiTrongCa === 0) return '0'
    return Number(dongCaForm.tienChiTrongCa).toLocaleString('vi-VN')
  },
  set(val) {
    if (!val) {
      dongCaForm.tienChiTrongCa = 0
      return
    }
    const clean = String(val).replace(/\D/g, '')
    dongCaForm.tienChiTrongCa = clean !== '' ? parseInt(clean, 10) : 0
  }
})

const displayTienMatCuoiCa = computed({
  get() {
    if (dongCaForm.tienMatCuoiCa === null || dongCaForm.tienMatCuoiCa === undefined || dongCaForm.tienMatCuoiCa === '') return ''
    return Number(dongCaForm.tienMatCuoiCa).toLocaleString('vi-VN')
  },
  set(val) {
    if (!val) {
      dongCaForm.tienMatCuoiCa = null
      return
    }
    const clean = String(val).replace(/\D/g, '')
    dongCaForm.tienMatCuoiCa = clean !== '' ? parseInt(clean, 10) : null
  }
})

// Modal biên bản
const showReceiptModal = ref(false)
const receiptShift = ref(null)

// Danh sách nhân viên có thể nhận bàn giao (bỏ người giao ca hiện tại)
const handoverEmployees = computed(() => {
  if (!currentOpenShift.value) return activeEmployees.value
  return activeEmployees.value.filter(nv => nv.id !== currentOpenShift.value.nhanVien?.id)
})

// Tính tiền mặt dự kiến trong ca hiện tại
const calculatedExpectedCash = computed(() => {
  const start = Number(currentOpenShift.value?.tienMatDauCa || 0)
  const posCash = Number(shiftSummary.value?.tienMatHeThong || 0)
  const chi = Number(dongCaForm.tienChiTrongCa || 0)
  return Math.max(0, start + posCash - chi)
})

// Tính chênh lệch tiền mặt cuối ca
const calculatedDiff = computed(() => {
  if (dongCaForm.tienMatCuoiCa === null || dongCaForm.tienMatCuoiCa === undefined || dongCaForm.tienMatCuoiCa < 0) {
    return 0
  }
  const counted = Number(dongCaForm.tienMatCuoiCa || 0)
  return counted - calculatedExpectedCash.value
})

const canSubmitDongCa = computed(() => {
  if (dongCaForm.tienMatCuoiCa === null || dongCaForm.tienMatCuoiCa === undefined || dongCaForm.tienMatCuoiCa < 0) return false
  if (calculatedDiff.value !== 0 && (!dongCaForm.lyDoChenhLech || !dongCaForm.lyDoChenhLech.trim())) return false
  return true
})

// Kiểm tra khung giờ của ca làm việc
function getShiftStatus(ca) {
  if (!ca || !ca.gioBatDau || !ca.gioKetThuc) return { status: 'open', label: '', canOpen: true }
  const now = new Date()
  const nowH = now.getHours()
  const nowM = now.getMinutes()
  const nowMinutes = nowH * 60 + nowM

  const [startH, startM] = ca.gioBatDau.split(':').map(Number)
  const [endH, endM] = ca.gioKetThuc.split(':').map(Number)
  const startMinutes = startH * 60 + startM
  const endMinutes = endH * 60 + endM

  const earlyMinutes = startMinutes - 30

  if (startMinutes < endMinutes) {
    if (nowMinutes < earlyMinutes) {
      return {
        status: 'future',
        label: `Chưa đến giờ (bắt đầu lúc ${ca.gioBatDau.substring(0, 5)})`,
        canOpen: false
      }
    } else if (nowMinutes > endMinutes + 30) {
      return {
        status: 'past',
        label: `Đã kết thúc lúc ${ca.gioKetThuc.substring(0, 5)}`,
        canOpen: false
      }
    } else {
      return {
        status: 'current',
        label: `Đang trong khung giờ ca`,
        canOpen: true
      }
    }
  } else {
    // Ca qua đêm
    if (nowMinutes >= earlyMinutes || nowMinutes <= endMinutes + 30) {
      return { status: 'current', label: 'Đang trong khung giờ ca', canOpen: true }
    } else {
      return { status: 'future', label: `Chưa đến giờ (bắt đầu lúc ${ca.gioBatDau.substring(0, 5)})`, canOpen: false }
    }
  }
}

const selectedShiftStatus = computed(() => {
  if (!moCaForm.caLamViecId) return null
  const ca = activeShifts.value.find(c => c.id === Number(moCaForm.caLamViecId))
  return ca ? getShiftStatus(ca) : null
})

const canSubmitMoCa = computed(() => {
  if (submittingMoCa.value) return false
  if (!moCaForm.nhanVienId || !moCaForm.caLamViecId) return false
  if (moCaForm.tienMatDauCa === null || moCaForm.tienMatDauCa === undefined || moCaForm.tienMatDauCa < 0) return false
  if (!isUserAdmin.value && selectedShiftStatus.value && !selectedShiftStatus.value.canOpen) return false
  return true
})

function getInitial(name) {
  if (!name) return 'NV'
  const parts = name.trim().split(' ')
  return parts[parts.length - 1].charAt(0).toUpperCase()
}

// Formatters
const formatCurrency = (val) => {
  if (val === null || val === undefined || isNaN(val)) return '0 đ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

const formatDateTime = (val) => {
  if (!val) return '-'
  try {
    const s = String(val).replace('T', ' ')
    return s.substring(0, 19)
  } catch (e) {
    return val
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return '--:--'
  if (typeof timeStr === 'string' && timeStr.length >= 5) {
    return timeStr.substring(0, 5)
  }
  return timeStr
}

// 1. Tải dữ liệu ban đầu
async function loadBaseData() {
  try {
    const [empRes, caRes] = await Promise.all([
      api.get('/api/nhan-vien'),
      api.get('/api/ca-lam-viec')
    ])

    const rawEmp = empRes.data?.content || empRes.data || []
    activeEmployees.value = rawEmp.filter(e => e.trangThai === 1 || e.trangThai === true)

    const rawCa = caRes.data?.content || caRes.data || []
    activeShifts.value = rawCa.filter(c => c.trangThai === 1 || c.trangThai === true)
  } catch (e) {
    console.error('Lỗi nạp dữ liệu cơ bản:', e)
  }
}

// 2. Kiểm tra ca làm việc đang mở
async function checkCurrentOpenShift() {
  if (!currentUser?.id) return
  try {
    const res = await api.get(`/api/lich-su-ca/current/${currentUser.id}`)
    currentOpenShift.value = res.data || null

    if (currentOpenShift.value) {
      await loadShiftSummary(currentOpenShift.value.id)
    } else {
      await loadLastClosedShift()
      autoSelectShiftAndEmployee()
    }
  } catch (e) {
    currentOpenShift.value = null
  }
}

// 3. Tải tổng hợp két tiền ca hiện tại
async function loadShiftSummary(shiftId) {
  if (!shiftId) return
  try {
    const res = await api.get(`/api/lich-su-ca/${shiftId}/summary`)
    shiftSummary.value = res.data || null
  } catch (e) {
    console.error('Lỗi nạp tổng hợp tiền ca:', e)
  }
}

// 4. Lấy ca đóng gần nhất
async function loadLastClosedShift() {
  try {
    const res = await api.get('/api/lich-su-ca/last-closed')
    lastClosedShift.value = res.data || null
    if (lastClosedShift.value && lastClosedShift.value.tienMatCuoiCa !== undefined) {
      moCaForm.tienMatDauCa = Number(lastClosedShift.value.tienMatCuoiCa || 0)
    }
  } catch (e) {
    lastClosedShift.value = null
  }
}

// 5. Tải 5 lượt giao ca gần nhất
async function loadRecentShifts() {
  try {
    const res = await api.get('/api/lich-su-ca?page=0&size=5')
    recentShifts.value = res.data?.content || []
  } catch (e) {
    recentShifts.value = []
  }
}

function autoSelectShiftAndEmployee() {
  if (currentUser?.id) {
    moCaForm.nhanVienId = currentUser.id
  }
  if (activeShifts.value.length > 0) {
    const validShift = activeShifts.value.find(ca => getShiftStatus(ca).canOpen)
    if (validShift) {
      moCaForm.caLamViecId = validShift.id
    } else {
      moCaForm.caLamViecId = ''
    }
  }
}

function applyLastClosedCash() {
  if (lastClosedShift.value) {
    moCaForm.tienMatDauCa = Number(lastClosedShift.value.tienMatCuoiCa || 0)
  }
}

// Xử lý Mở ca
async function submitMoCa() {
  if (moCaForm.tienMatDauCa === null || moCaForm.tienMatDauCa === undefined || moCaForm.tienMatDauCa < 0) {
    showToast('Vui lòng nhập số tiền mặt đầu ca hợp lệ', 'warning')
    return
  }
  if (!moCaForm.nhanVienId) {
    showToast('Vui lòng chọn nhân viên trực quầy', 'warning')
    return
  }
  if (!moCaForm.caLamViecId) {
    showToast('Vui lòng chọn ca làm việc', 'warning')
    return
  }
  if (!isUserAdmin.value && selectedShiftStatus.value && !selectedShiftStatus.value.canOpen) {
    showToast(selectedShiftStatus.value.label + '. Không thể mở ca!', 'warning')
    return
  }

  submittingMoCa.value = true
  try {
    await api.post('/api/lich-su-ca/mo-ca', {
      nhanVienId: Number(moCaForm.nhanVienId),
      caLamViecId: Number(moCaForm.caLamViecId),
      tienMatDauCa: Number(moCaForm.tienMatDauCa || 0),
      ghiChu: moCaForm.ghiChu ? moCaForm.ghiChu.trim() : null
    })

    showToast('Mở ca làm việc thành công!', 'success')
    await checkCurrentOpenShift()
    await loadRecentShifts()

    showAlert({
      title: 'Mở ca thành công!',
      message: 'Ca làm việc đã được kích hoạt. Bạn có muốn chuyển ngay tới màn hình Bán hàng (POS) không?',
      type: 'success',
      showCancelButton: true,
      confirmButtonText: 'Đến màn Bán hàng ngay',
      cancelButtonText: 'Ở lại trang Giao ca'
    }).then(result => {
      if (result?.isConfirmed) {
        router.push('/ban-hang')
      }
    })
  } catch (e) {
    showAlert({
      title: 'Không thể mở ca',
      message: e?.response?.data?.message || 'Lỗi khi mở ca làm việc',
      type: 'error'
    })
  } finally {
    submittingMoCa.value = false
  }
}

// Xử lý Đóng ca & Bàn giao
async function submitDongCa() {
  if (dongCaForm.tienMatCuoiCa === null || dongCaForm.tienMatCuoiCa === undefined || dongCaForm.tienMatCuoiCa < 0) {
    showToast('Vui lòng nhập số tiền mặt thực tế kiểm đếm', 'warning')
    return
  }
  if (calculatedDiff.value !== 0 && (!dongCaForm.lyDoChenhLech || !dongCaForm.lyDoChenhLech.trim())) {
    showToast('Phát hiện chênh lệch tiền mặt. Vui lòng nhập lý do giải trình!', 'warning')
    return
  }

  submittingDongCa.value = true
  try {
    const res = await api.post('/api/lich-su-ca/dong-ca', {
      id: currentOpenShift.value?.id,
      nhanVienId: currentOpenShift.value?.nhanVien?.id,
      caLamViecId: currentOpenShift.value?.caLamViec?.id,
      nhanVienNhanId: dongCaForm.nhanVienNhanId ? Number(dongCaForm.nhanVienNhanId) : null,
      tienChiTrongCa: Number(dongCaForm.tienChiTrongCa || 0),
      tienMatCuoiCa: Number(dongCaForm.tienMatCuoiCa),
      chenhLech: calculatedDiff.value,
      lyDoChenhLech: calculatedDiff.value !== 0 ? dongCaForm.lyDoChenhLech.trim() : null,
      ghiChu: dongCaForm.ghiChu ? dongCaForm.ghiChu.trim() : null
    })

    showToast('Kết ca & Bàn giao tiền mặt thành công!', 'success')
    const closedShiftData = res.data

    dongCaForm.tienChiTrongCa = 0
    dongCaForm.tienMatCuoiCa = null
    dongCaForm.lyDoChenhLech = ''
    dongCaForm.ghiChu = ''

    await checkCurrentOpenShift()
    await loadRecentShifts()

    if (closedShiftData) {
      openReceiptModal(closedShiftData)
    }
  } catch (e) {
    showAlert({
      title: 'Không thể đóng ca',
      message: e?.response?.data?.message || 'Lỗi khi đóng ca làm việc',
      type: 'error'
    })
  } finally {
    submittingDongCa.value = false
  }
}

// Modal biên bản
async function openReceiptModal(shift) {
  if (!shift) return
  try {
    if (shift.id) {
      const res = await api.get(`/api/lich-su-ca/${shift.id}`)
      receiptShift.value = res.data || shift
    } else {
      receiptShift.value = shift
    }
    showReceiptModal.value = true
  } catch (e) {
    receiptShift.value = shift
    showReceiptModal.value = true
  }
}

function calculateReceiptExpected(s) {
  if (!s) return 0
  const start = Number(s.tienMatDauCa || 0)
  const thu = Number(s.tongTienTrongCa || s.tienMatThuTrongCa || 0)
  const chi = Number(s.tienChiTrongCa || 0)
  return Math.max(0, start + thu - chi)
}

function printReceipt() {
  window.print()
}

onMounted(async () => {
  await loadBaseData()
  await checkCurrentOpenShift()
  await loadRecentShifts()
})
</script>

<style scoped>
.giao-ca-page {
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
  border-radius: 16px;
  border: 1px solid #ece6dc;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.03);
  padding: 1.35rem 1.6rem;
  box-sizing: border-box;
}

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
}

.page-main-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.top-nav-links {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-history-link,
.btn-pos-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e2dad0;
  background-color: #ffffff;
  color: #634b35;
}

.btn-history-link:hover {
  background-color: #faf7f2;
  border-color: #8c7355;
}

.btn-pos-link {
  background-color: #634b35;
  color: #ffffff;
  border-color: #634b35;
}

.btn-pos-link:hover {
  background-color: #4e3a29;
}

/* KHỐI CA ĐANG MỞ */
.shift-active-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.current-shift-card {
  background: linear-gradient(135deg, #ffffff 0%, #faf8f5 100%);
  border-left: 5px solid #2e7d32;
}

.shift-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.shift-badge-active {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  background-color: #e8f5e9;
  color: #2e7d32;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #2e7d32;
  box-shadow: 0 0 0 0 rgba(46, 125, 50, 0.7);
  animation: pulse-green 1.8s infinite;
}

@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(46, 125, 50, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(46, 125, 50, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(46, 125, 50, 0); }
}

.shift-code-tag {
  font-size: 0.88rem;
  color: #634b35;
}

.shift-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-block {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-label {
  font-size: 0.76rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #8c979d;
  letter-spacing: 0.4px;
}

.info-value {
  font-size: 1rem;
  font-weight: 700;
  color: #2c3e50;
}

.info-sub {
  font-size: 0.78rem;
  color: #8c979d;
}

.text-brown { color: #8c7355; }
.text-green { color: #2e7d32; }
.text-blue { color: #1976d2; }
.text-note { font-weight: normal; font-size: 0.88rem; color: #5a666e; }

/* KÉT TIỀN REALTIME METRICS */
.metrics-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.metrics-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #634b35;
}

.metrics-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
}

.btn-refresh-clean {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #e2dad0;
  background-color: #faf8f5;
  color: #634b35;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh-clean:hover {
  background-color: #f1ede7;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.metric-box {
  padding: 1rem 1.2rem;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 1px solid transparent;
}

.bg-warm {
  background-color: #faf5ee;
  border-color: #f0e6d6;
}

.bg-green {
  background-color: #f1f8e9;
  border-color: #dcedc8;
}

.bg-blue {
  background-color: #e3f2fd;
  border-color: #bbdefb;
}

.bg-highlight {
  background-color: #fff8e1;
  border-color: #ffe082;
}

.metric-name {
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #616161;
}

.metric-val {
  font-size: 1.35rem;
  font-weight: 800;
}

.text-primary-highlight {
  color: #e65100;
}

/* ĐÓNG CA FORM */
.card-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #634b35;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #f1ede7;
}

.card-section-title h2 {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
}

.close-shift-form,
.open-shift-form {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.form-cols-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 768px) {
  .form-cols-2 {
    grid-template-columns: 1fr;
  }
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  display: block;
  font-size: 0.82rem;
  font-weight: 700;
  color: #4a3a2a;
  margin-bottom: 6px;
}

.locked-staff-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 8px;
  background-color: #fbf9f6;
  border: 1px solid #dfd8cc;
  height: 38px;
  box-sizing: border-box;
}

.staff-avatar-mini {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8c7355, #634b35);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.75rem;
  flex-shrink: 0;
}

.staff-text-locked {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.staff-name-locked {
  font-size: 0.88rem;
  font-weight: 700;
  color: #2c3e50;
}

.staff-code-locked {
  font-size: 0.76rem;
  color: #8c7355;
  font-weight: 500;
}

.shift-time-warning {
  margin-top: 5px;
  padding: 5px 10px;
  border-radius: 6px;
  background-color: #fff8eb;
  border: 1px solid #fed7aa;
  color: #c2410c;
  font-size: 0.8rem;
  font-weight: 600;
}

.required-star {
  color: #e53935;
}

.form-control-minimal {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #dfd8cc;
  background-color: #ffffff;
  color: #2c3e50;
  font-size: 0.88rem;
  box-sizing: border-box;
  outline: none;
  transition: all 0.2s;
  font-family: inherit;
}

.form-control-minimal:focus {
  border-color: #8c7355;
  box-shadow: 0 0 0 3px rgba(140, 115, 85, 0.12);
}

.select-wrapper-minimal {
  position: relative;
  width: 100%;
}

.select-wrapper-minimal::after {
  content: "";
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 4.5px solid transparent;
  border-right: 4.5px solid transparent;
  border-top: 5px solid #8c7355;
  pointer-events: none;
}

.select-wrapper-minimal select.form-control-minimal {
  appearance: none;
  -webkit-appearance: none;
  padding-right: 30px;
  cursor: pointer;
}

.input-currency-box {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.input-currency-box input {
  padding-right: 48px;
}

.currency-tag {
  position: absolute;
  right: 12px;
  font-size: 0.82rem;
  font-weight: 700;
  color: #8c7355;
  pointer-events: none;
}

.textarea-minimal {
  resize: vertical;
  min-height: 60px;
  height: auto;
  padding: 8px 12px;
  font-family: inherit;
}

.input-error-border {
  border-color: #e53935 !important;
  background-color: #fff9f9;
}

/* BANNER CHÊNH LỆCH */
.diff-result-banner {
  padding: 0.9rem 1.2rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.diff-match {
  background-color: #e8f5e9;
  border: 1px solid #c8e6c9;
  color: #2e7d32;
}

.diff-surplus {
  background-color: #fff3e0;
  border: 1px solid #ffe0b2;
  color: #e65100;
}

.diff-deficit {
  background-color: #ffebee;
  border: 1px solid #ffcdd2;
  color: #c62828;
}

.diff-banner-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.diff-icon {
  font-size: 1.3rem;
}

.diff-title {
  display: block;
  font-size: 0.92rem;
  font-weight: 700;
}

.diff-desc {
  font-size: 0.8rem;
  margin: 2px 0 0 0;
  opacity: 0.88;
}

.diff-amount-tag {
  font-size: 1.2rem;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 8px;
  background-color: rgba(255, 255, 255, 0.65);
}

.form-actions-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.btn-submit-close-shift {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 24px;
  border-radius: 8px;
  background-color: #634b35;
  color: #ffffff;
  font-size: 0.92rem;
  font-weight: 700;
  border: 1px solid #543e2a;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(99, 75, 53, 0.25);
  font-family: inherit;
}

.btn-submit-close-shift:hover:not(:disabled) {
  background-color: #4e3a29;
  border-color: #402f20;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(99, 75, 53, 0.32);
}

.btn-submit-close-shift:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* KHỐI CHƯA MỞ CA */
.shift-inactive-container {
  display: flex;
  flex-direction: column;
}

.open-shift-card {
  border-top: 5px solid #8c7355;
}

.open-shift-header {
  margin-bottom: 1.25rem;
}

.inactive-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 20px;
  background-color: #eceff1;
  color: #546e7a;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 0.75rem;
}

.idle-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #78909c;
}

.open-card-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.last-shift-suggestion {
  background-color: #faf5ee;
  border: 1px solid #f0e6d6;
  border-radius: 12px;
  padding: 1rem 1.25rem;
  margin-bottom: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.suggestion-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #634b35;
  font-size: 0.88rem;
}

.suggestion-cash {
  margin-top: 3px;
  font-size: 0.84rem;
}

.btn-use-last-cash {
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid #8c7355;
  background-color: #ffffff;
  color: #634b35;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-use-last-cash:hover {
  background-color: #634b35;
  color: #ffffff;
}

.btn-submit-open-shift {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 26px;
  border-radius: 8px;
  background-color: #8c7355;
  color: #ffffff;
  font-size: 0.92rem;
  font-weight: 700;
  border: 1px solid #7d6549;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(140, 115, 85, 0.25);
  font-family: inherit;
}

.btn-submit-open-shift:hover:not(:disabled) {
  background-color: #755f45;
  border-color: #68533b;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(140, 115, 85, 0.32);
}

.btn-submit-open-shift:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* KHỐI CÁC CA ĐÃ GIAO GẦN NHẤT */
.recent-shifts-card {
  margin-top: 0.25rem;
}

.recent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.9rem;
}

.recent-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.btn-text-more {
  background: none;
  border: none;
  color: #8c7355;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.btn-text-more:hover {
  text-decoration: underline;
}

.table-responsive-clean {
  overflow-x: auto;
}

.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.86rem;
}

.minimal-styled-table th {
  padding: 10px 12px;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.header-brown-beige {
  background-color: #f7f4ee;
  color: #634b35;
  border-bottom: 2px solid #e8e2d8;
}

.minimal-styled-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0ece5;
  color: #374151;
}

.table-data-row:hover {
  background-color: #fcfbf9;
}

.badge-running {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  background-color: #e8f5e9;
  color: #2e7d32;
  font-size: 0.76rem;
  font-weight: 600;
}

.diff-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 0.8rem;
  font-weight: 700;
}

.diff-pill-zero {
  background-color: #f1f8e9;
  color: #33691e;
}

.diff-pill-warn {
  background-color: #fff3e0;
  color: #e65100;
}

.btn-action-view {
  background-color: #faf5ee;
  border: 1px solid #dcd5ca;
  color: #634b35;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action-view:hover {
  background-color: #634b35;
  color: #ffffff;
  border-color: #634b35;
}

/* BIÊN BẢN IN */
.modal-backdrop-clean {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.modal-dialog-clean {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  box-sizing: border-box;
  animation: modal-pop 0.25s ease-out;
}

@keyframes modal-pop {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.modal-receipt {
  width: 580px;
  max-width: 95vw;
}

.modal-header-clean {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f0ece5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title-clean {
  font-size: 1.15rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #8c979d;
  cursor: pointer;
}

.modal-body-clean {
  padding: 1.5rem;
  overflow-y: auto;
}

.modal-footer-clean {
  padding: 1rem 1.5rem;
  border-top: 1px solid #f0ece5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel-modal {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid #dcd5ca;
  background-color: #ffffff;
  color: #5a666e;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-print-action {
  padding: 8px 18px;
  border-radius: 8px;
  border: none;
  background-color: #634b35;
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}

.printable-receipt {
  font-family: Arial, sans-serif;
  color: #222;
  font-size: 13px;
  line-height: 1.45;
}

.receipt-header {
  text-align: center;
  margin-bottom: 10px;
}

.receipt-brand {
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 1.5px;
  margin: 0 0 2px 0;
  color: #333;
}

.receipt-subtitle {
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: #666;
}

.receipt-code {
  font-size: 12px;
  color: #555;
}

.receipt-divider {
  border-bottom: 1px dashed #bbb;
  margin: 10px 0 12px 0;
}

.receipt-meta-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.receipt-meta-row {
  display: flex;
  justify-content: space-between;
}

.meta-label {
  color: #666;
}

.meta-val {
  color: #111;
  text-align: right;
}

.receipt-financial-table {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 6px 0;
}

.fin-row {
  display: flex;
  justify-content: space-between;
}

.fin-expected {
  background-color: #f7f7f7;
  padding: 4px 6px;
  border-radius: 4px;
}

.fin-actual {
  font-size: 14px;
  padding: 4px 6px;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
}

.fin-diff {
  padding: 4px 6px;
  font-size: 14px;
}

.fin-reason {
  font-size: 12px;
  color: #c62828;
  padding: 4px 6px;
  background-color: #fff8e1;
  border-radius: 4px;
}

.receipt-signatures {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  text-align: center;
}

.sig-col {
  width: 45%;
}

.sig-title {
  font-size: 12px;
  font-weight: 700;
  margin: 0 0 2px 0;
}

.sig-sub {
  font-size: 11px;
  color: #777;
  margin: 0 0 45px 0;
}

.sig-name {
  font-weight: 700;
  margin: 0;
}

@media print {
  body * {
    visibility: hidden;
  }
  #print-receipt-area, #print-receipt-area * {
    visibility: visible;
  }
  #print-receipt-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    padding: 0;
    margin: 0;
  }
  .no-print {
    display: none !important;
  }
}
</style>
