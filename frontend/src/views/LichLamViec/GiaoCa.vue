<template>
  <div class="giao-ca-page">

    <div class="page-top-header">
      <div class="page-breadcrumb">
        <router-link to="/giao-ca" class="crumb-parent">Lịch làm việc</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Vào ca & Giao ca</span>
      </div>
      <div class="title-with-icon" v-if="false">
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

                    <div class="form-item" style="margin-top: 6px;">
            <label class="field-label">Ghi chú bàn giao</label>
            <input
              type="text"
              v-model="dongCaForm.ghiChu"
              placeholder="Ghi chú bàn giao ca (nếu có)..."
              class="form-control-minimal"
            />
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
      <div class="shift-inactive-empty-card">
        <div class="empty-shift-content">
          <div class="empty-shift-icon-wrap">
            <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--blue, #496883)" stroke-width="1.8">
              <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
              <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
            </svg>
          </div>
          <h3 class="empty-shift-title">Bạn hiện chưa vào ca làm việc</h3>
          <p class="empty-shift-desc">
            Vui lòng mở ca làm việc để thực hiện ghi nhận doanh số, quản lý két tiền và bàn giao ca.
          </p>
          <button type="button" class="btn-open-shift-action" @click="triggerOpenMoCaPopup">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 11 12 14 22 4" />
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
            </svg>
            <span>Mở ca làm việc ngay</span>
          </button>
        </div>
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { getCurrentUser, getRoleCode, setActiveShift, clearActiveShift } from '@/utils/auth.js'
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
const assignedShiftsToday = ref([])
const hasScheduleToday = ref(false)
const loadingSchedule = ref(false)

const currentTimeStr = ref('')
const currentDateStr = ref('')
let clockTimer = null

function updateClock() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  currentTimeStr.value = `${hh}:${mm}:${ss}`

  const dd = String(now.getDate()).padStart(2, '0')
  const MM = String(now.getMonth() + 1).padStart(2, '0')
  const yyyy = now.getFullYear()
  currentDateStr.value = `${dd}/${MM}/${yyyy}`
}

const displayEmployeeName = computed(() => {
  if (isUserAdmin.value && moCaForm.nhanVienId) {
    const nv = activeEmployees.value.find(e => e.id === Number(moCaForm.nhanVienId))
    if (nv) return nv.tenNhanVien
  }
  return currentUser?.tenNhanVien || 'Nhân viên'
})

const displayEmployeeCode = computed(() => {
  if (isUserAdmin.value && moCaForm.nhanVienId) {
    const nv = activeEmployees.value.find(e => e.id === Number(moCaForm.nhanVienId))
    if (nv) return nv.maNhanVien || nv.tenTaiKhoan
  }
  return currentUser?.maNhanVien || currentUser?.tenTaiKhoan || 'NV'
})

const submittingMoCa = ref(false)
const moCaForm = reactive({
  nhanVienId: '',
  caLamViecId: '',
  tienMatDauCa: 0,
  tienChuyenKhoan: 0,
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

const displayTienTaiKhoanDauCa = computed({
  get() {
    if (moCaForm.tienChuyenKhoan === null || moCaForm.tienChuyenKhoan === undefined || moCaForm.tienChuyenKhoan === '') return ''
    return Number(moCaForm.tienChuyenKhoan).toLocaleString('vi-VN')
  },
  set(val) {
    if (!val) {
      moCaForm.tienChuyenKhoan = 0
      return
    }
    const clean = String(val).replace(/\D/g, '')
    moCaForm.tienChuyenKhoan = clean !== '' ? parseInt(clean, 10) : 0
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
  const sourceList = isUserAdmin.value ? activeShifts.value : assignedShiftsToday.value
  const ca = sourceList.find(c => c.id === Number(moCaForm.caLamViecId))
  return ca ? getShiftStatus(ca) : null
})

const canSubmitMoCa = computed(() => {
  if (submittingMoCa.value) return false
  if (!moCaForm.nhanVienId || !moCaForm.caLamViecId) return false
  if (!isUserAdmin.value && !hasScheduleToday.value) return false
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
      setActiveShift(currentOpenShift.value)
      await loadShiftSummary(currentOpenShift.value.id)
    } else {
      clearActiveShift()
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

// Tải lịch làm việc hôm nay của nhân viên
async function loadTodayShiftsForEmployee(targetEmpId = null) {
  const empId = targetEmpId || currentUser?.id
  if (!empId) return
  loadingSchedule.value = true
  try {
    const res = await api.get(`/api/lich-lam-viec/hom-nay/${empId}`)
    const schedules = res.data || []
    assignedShiftsToday.value = schedules.map(s => s.caLamViec).filter(Boolean)
    hasScheduleToday.value = assignedShiftsToday.value.length > 0
  } catch (e) {
    assignedShiftsToday.value = []
    hasScheduleToday.value = false
  } finally {
    loadingSchedule.value = false
  }
}

async function onAdminEmployeeChange() {
  if (isUserAdmin.value && moCaForm.nhanVienId) {
    await loadTodayShiftsForEmployee(moCaForm.nhanVienId)
    if (assignedShiftsToday.value.length > 0) {
      moCaForm.caLamViecId = assignedShiftsToday.value[0].id
    }
  }
}

function goToViewSchedule() {
  showToast('Chuyển sang chế độ xem lịch làm việc...', 'info')
  router.push('/xep-lich')
}

async function autoSelectShiftAndEmployee() {
  if (currentUser?.id) {
    moCaForm.nhanVienId = currentUser.id
  }
  if (!isUserAdmin.value) {
    await loadTodayShiftsForEmployee(currentUser?.id)
    if (assignedShiftsToday.value.length > 0) {
      const validShift = assignedShiftsToday.value.find(ca => getShiftStatus(ca).canOpen)
      if (validShift) {
        moCaForm.caLamViecId = validShift.id
      } else {
        moCaForm.caLamViecId = assignedShiftsToday.value[0].id
      }
    } else {
      moCaForm.caLamViecId = ''
    }
  } else if (activeShifts.value.length > 0) {
    const validShift = activeShifts.value.find(ca => getShiftStatus(ca).canOpen)
    if (validShift) {
      moCaForm.caLamViecId = validShift.id
    } else {
      moCaForm.caLamViecId = activeShifts.value[0]?.id || ''
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
    const res = await api.post('/api/lich-su-ca/mo-ca', {
      nhanVienId: Number(moCaForm.nhanVienId),
      caLamViecId: Number(moCaForm.caLamViecId),
      tienMatDauCa: Number(moCaForm.tienMatDauCa || 0),
      tienChuyenKhoan: Number(moCaForm.tienChuyenKhoan || 0),
      ghiChu: moCaForm.ghiChu ? moCaForm.ghiChu.trim() : null
    })

    showToast('Mở ca làm việc thành công!', 'success')
    const openedShift = res?.data || null
    if (openedShift) {
      setActiveShift(openedShift)
      currentOpenShift.value = openedShift
    } else {
      await checkCurrentOpenShift()
    }
    await loadRecentShifts()
    window.dispatchEvent(new CustomEvent('shift-opened', { detail: openedShift || currentOpenShift.value }))

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
      nhanVienNhanId: shiftSummary.value?.nhanVienNhanDuKienId || (dongCaForm.nhanVienNhanId ? Number(dongCaForm.nhanVienNhanId) : null),
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

    clearActiveShift()
    await checkCurrentOpenShift()
    await loadRecentShifts()
    window.dispatchEvent(new CustomEvent('shift-closed', { detail: closedShiftData }))

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

function triggerOpenMoCaPopup() {
  window.dispatchEvent(new CustomEvent('open-mo-ca-popup'))
}

async function onGlobalShiftOpened() {
  await checkCurrentOpenShift()
  await loadRecentShifts()
}

function printReceipt() {
  window.print()
}

onMounted(async () => {
  window.addEventListener('shift-opened', onGlobalShiftOpened)
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  await loadBaseData()
  await checkCurrentOpenShift()
  await loadRecentShifts()
})
onUnmounted(() => {
  window.removeEventListener('shift-opened', onGlobalShiftOpened)
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.shift-inactive-empty-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  padding: 3rem 2rem;
  text-align: center;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.empty-shift-content {
  max-width: 440px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty-shift-icon-wrap {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: #f0f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}

.empty-shift-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.empty-shift-desc {
  font-size: 0.88rem;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
}

.btn-open-shift-action {
  margin-top: 10px;
  padding: 10px 24px;
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 3px 10px rgba(73, 104, 131, 0.25);
  transition: all 0.2s ease;
}

.btn-open-shift-action:hover {
  background: #3b576e;
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(73, 104, 131, 0.35);
}

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
  flex-wrap: wrap;
  gap: 12px;
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
  color: var(--blue, #496883);
}

.btn-history-link:hover {
  background-color: #faf7f2;
  border-color: var(--blue, #496883);
}

.btn-pos-link {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border-color: var(--blue, #496883);
}

.btn-pos-link:hover {
  background-color: #3b576e;
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
  color: var(--blue, #496883);
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

.text-brown { color: var(--blue, #496883); }
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
  color: var(--blue, #496883);
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
  color: var(--blue, #496883);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-refresh-clean:hover {
  background-color: #eaf2f6;
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
  color: var(--blue, #496883);
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--line, #e9e5db);
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
  background: linear-gradient(135deg, var(--blue, #496883), #3b576e);
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
  color: var(--blue, #496883);
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
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
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
  border-top: 5px solid var(--blue, #496883);
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
  color: var(--blue, #496883);
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
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 0.92rem;
  font-weight: 700;
  border: 1px solid var(--blue, #496883);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(99, 75, 53, 0.25);
  font-family: inherit;
}

.btn-submit-close-shift:hover:not(:disabled) {
  background-color: #3b576e;
  border-color: #402f20;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(99, 75, 53, 0.32);
}

.btn-submit-close-shift:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* KHỐI CHƯA MỞ CA - CARD CHUẨN THEO ẢNH */
.shift-inactive-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1.5rem 0 2.5rem;
}

.open-shift-modal-card {
  width: 100%;
  max-width: 480px;
  background-color: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #dfd8cc;
}

.open-shift-modal-header {
  background-color: var(--blue, #496883);
  padding: 18px 24px 16px;
  text-align: center;
}

.open-modal-title {
  margin: 0;
  font-size: 1.18rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.open-modal-subtitle {
  margin-top: 4px;
  font-size: 0.8rem;
  color: #eaf2f6;
  opacity: 0.95;
}

.open-shift-modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #ffffff;
}

.open-modal-staff-info {
  text-align: center;
  margin-bottom: 4px;
}

.open-modal-staff-name {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
}

.open-modal-staff-sub {
  margin-top: 4px;
  font-size: 0.82rem;
  color: #64748b;
}

.form-item-admin-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background-color: #f8fafc;
  padding: 10px;
  border-radius: 8px;
  border: 1px dashed #cbd5e1;
}

.shift-status-container-box {
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  padding: 10px 14px;
  background-color: #fcfbf9;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.shift-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  color: #64748b;
  font-weight: 600;
}

.empty-shift-notice {
  font-size: 0.88rem;
  color: #94a3b8;
  font-weight: 500;
  padding: 4px 0;
}

.shift-select-custom {
  background: transparent;
  border: none;
  font-weight: 600;
  padding: 0;
  height: auto;
  color: #1e293b;
}

.shift-time-alert {
  padding: 8px 12px;
  border-radius: 6px;
  background-color: #fff8eb;
  border: 1px solid #fed7aa;
  color: #c2410c;
  font-size: 0.8rem;
  font-weight: 600;
}

.modal-form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-item-label {
  font-size: 0.84rem;
  font-weight: 700;
  color: #334155;
}

.modal-input-field {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #dfd8cc;
  background-color: #ffffff;
  color: #1e293b;
  font-size: 0.88rem;
  outline: none;
  box-sizing: border-box;
  transition: all 0.2s;
}

.modal-input-field:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.15);
}

.last-shift-summary-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.82rem;
  color: #64748b;
  margin-top: -4px;
}

.btn-quick-apply-cash {
  background: none;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 0.75rem;
  color: var(--blue, #496883);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-quick-apply-cash:hover {
  background-color: #f1f5f9;
  border-color: var(--blue, #496883);
}

.open-modal-actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.btn-view-mode {
  flex: 1;
  height: 42px;
  border: 1px solid #d1c8bd;
  border-radius: 8px;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-view-mode:hover {
  background-color: #f8fafc;
  border-color: var(--blue, #496883);
  color: var(--blue, #496883);
}

.btn-confirm-enter-shift {
  flex: 1;
  height: 42px;
  border: 1px solid var(--blue, #496883);
  border-radius: 8px;
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-confirm-enter-shift:hover:not(:disabled) {
  background-color: #3b576e;
  border-color: #3b576e;
}

.btn-confirm-enter-shift:disabled {
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
  color: var(--blue, #496883);
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
  color: var(--blue, #496883);
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
  color: var(--blue, #496883);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action-view:hover {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border-color: var(--blue, #496883);
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
  border-radius: 12px;
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
  background-color: var(--blue, #496883);
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

/* Hộp nhận bàn giao tự động */
.auto-badge-pill {
  font-size: 0.72rem;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: 6px;
}

.auto-handover-box {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 14px;
  min-height: 44px;
}

.auto-handover-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #e2e8f0;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.auto-handover-text {
  display: flex;
  flex-direction: column;
}

.auto-handover-name {
  font-size: 0.9rem;
  color: #1e293b;
}

.auto-handover-sub {
  font-size: 0.78rem;
  color: #64748b;
}
</style>
