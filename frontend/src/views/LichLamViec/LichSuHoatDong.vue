<template>
  <div class="lich-su-ca-page">
    <!-- Tiêu đề trang -->
    <div class="page-top-bar">
      <div class="title-with-icon">
        <div class="title-icon-box">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 14 14" />
            <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
          </svg>
        </div>
        <h1 class="page-main-title">Lịch sử hoạt động ca & Giao ca</h1>
      </div>

      <div class="top-actions-cluster">
        <!-- Nút Mở ca mới -->
        <button
          type="button"
          class="btn-open-shift"
          @click="openModalMoCa"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
          <span>Mở ca làm việc</span>
        </button>

        <!-- Nút Đóng ca hiện tại nếu đang có ca mở -->
        <button
          v-if="currentOpenShift"
          type="button"
          class="btn-close-shift"
          @click="openModalDongCa(currentOpenShift)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
            <line x1="9" y1="9" x2="15" y2="15" />
            <line x1="15" y1="9" x2="9" y2="15" />
          </svg>
          <span>Kết ca & Bàn giao</span>
        </button>
      </div>
    </div>

    <!-- Card Bộ lọc Lịch sử hoạt động -->
    <div class="content-card filter-card-box">
      <div class="filter-grid-layout">
        <!-- Ô Tìm kiếm: 'Tìm theo tài khoản / mã ca / mã hóa đơn...' -->
        <div class="filter-col search-col">
          <label class="field-label-tiny">Tìm kiếm</label>
          <div class="input-with-prefix-icon">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              v-model="filters.keyword"
              placeholder="Tìm theo tài khoản / mã ca / mã hóa đơn..."
              class="form-control-minimal with-icon"
              @input="onSearchInput"
            />
          </div>
        </div>

        <!-- Từ ngày -->
        <div class="filter-col">
          <label class="field-label-tiny">Từ ngày:</label>
          <input
            type="date"
            v-model="filters.fromDate"
            class="form-control-minimal"
            :class="{ 'input-error-border': dateFilterError }"
            @change="onDateChange"
          />
        </div>

        <!-- Đến ngày -->
        <div class="filter-col">
          <label class="field-label-tiny">Đến ngày:</label>
          <input
            type="date"
            v-model="filters.toDate"
            class="form-control-minimal"
            :class="{ 'input-error-border': dateFilterError }"
            @change="onDateChange"
          />
        </div>

        <!-- Trạng thái ca -->
        <div class="filter-col">
          <label class="field-label-tiny">Trạng thái</label>
          <div class="select-wrapper-minimal">
            <select v-model="filters.trangThai" class="form-control-minimal" @change="loadHistory">
              <option value="">Tất cả</option>
              <option value="1">Đang mở</option>
              <option value="2">Đã đóng</option>
            </select>
          </div>
        </div>

        <!-- Nút Tải lại -->
        <div class="filter-col-action">
          <button type="button" class="btn-refresh-clean" @click="resetFilters" title="Tải lại danh sách">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Tải lại</span>
          </button>
        </div>
      </div>

      <!-- Thông báo lỗi khi Từ ngày > Đến ngày -->
      <div v-if="dateFilterError" class="filter-error-msg">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>{{ dateFilterError }}</span>
      </div>
    </div>

    <!-- Bảng Lịch sử hoạt động (8 Cột chuẩn) -->
    <div class="content-card history-table-card">
      <div class="table-responsive-clean">
        <table class="minimal-styled-table">
          <thead>
            <tr class="header-brown-beige">
              <th style="width: 50px; text-align: center">#</th>
              <th style="min-width: 170px">TÀI KHOẢN</th>
              <th style="width: 140px">CA</th>
              <th style="width: 160px; text-align: center">MỞ</th>
              <th style="width: 160px; text-align: center">ĐÓNG</th>
              <th style="width: 170px; text-align: right">TIỀN MẶT</th>
              <th style="width: 140px; text-align: right">CHÊNH LỆCH</th>
              <th style="width: 130px; text-align: center">TRẠNG THÁI</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                Đang tải dữ liệu lịch sử hoạt động ca...
              </td>
            </tr>
            <tr v-else-if="historyList.length === 0">
              <td colspan="8" style="text-align: center; padding: 2.5rem; color: #8c979d;">
                Không có dữ liệu lịch sử ca nào phù hợp với bộ lọc.
              </td>
            </tr>
            <tr v-for="(item, index) in historyList" :key="item.id" class="table-data-row">
              <!-- Cột 1: # -->
              <td style="text-align: center" class="text-muted-stt">
                {{ (pagination.page * pagination.size) + index + 1 }}
              </td>

              <!-- Cột 2: TÀI KHOẢN -->
              <td>
                <div class="account-cell-info">
                  <div class="account-avatar-dot" :style="{ backgroundColor: getAvatarColor(item.nhanVien?.tenNhanVien) }">
                    {{ getInitial(item.nhanVien?.tenNhanVien) }}
                  </div>
                  <div class="account-text-wrap">
                    <strong class="account-username">{{ item.nhanVien?.tenTaiKhoan || item.nhanVien?.maNhanVien }}</strong>
                    <span class="employee-realname">{{ item.nhanVien?.tenNhanVien }}</span>
                  </div>
                </div>
              </td>

              <!-- Cột 3: CA -->
              <td>
                <div class="shift-name-tag">
                  {{ item.caLamViec?.tenCa }}
                </div>
              </td>

              <!-- Cột 4: MỞ -->
              <td style="text-align: center" class="text-time-cell">
                {{ formatDateTime(item.thoiGianMoCa) }}
              </td>

              <!-- Cột 5: ĐÓNG -->
              <td style="text-align: center" class="text-time-cell">
                <span v-if="item.thoiGianDongCa">{{ formatDateTime(item.thoiGianDongCa) }}</span>
                <span v-else class="badge-running-shift">Đang trực</span>
              </td>

              <!-- Cột 6: TIỀN MẶT -->
              <td style="text-align: right">
                <div class="cash-flow-wrap">
                  <span class="cash-start" title="Tiền mặt đầu ca">Đầu: {{ formatCurrency(item.tienMatDauCa) }}</span>
                  <span class="cash-end" title="Tiền mặt cuối ca">Cuối: {{ item.tienMatCuoiCa !== null ? formatCurrency(item.tienMatCuoiCa) : 'Chưa chốt' }}</span>
                </div>
              </td>

              <!-- Cột 7: CHÊNH LỆCH -->
              <td style="text-align: right">
                <div v-if="item.trangThai === 2" class="diff-amount-wrap">
                  <span
                    class="diff-badge"
                    :class="Number(item.chenhLech || 0) === 0 ? 'diff-zero' : 'diff-warn'"
                    :title="item.lyDoChenhLech ? 'Lý do: ' + item.lyDoChenhLech : ''"
                  >
                    {{ Number(item.chenhLech || 0) > 0 ? '+' : '' }}{{ formatCurrency(item.chenhLech || 0) }}
                  </span>
                  <span v-if="item.lyDoChenhLech" class="diff-reason-tooltip" :title="item.lyDoChenhLech">
                    ℹ
                  </span>
                </div>
                <span v-else class="text-muted">-</span>
              </td>

              <!-- Cột 8: TRẠNG THÁI -->
              <td style="text-align: center">
                <span
                  class="badge-shift-state"
                  :class="item.trangThai === 1 ? 'state-open' : 'state-closed'"
                >
                  {{ item.trangThai === 1 ? 'Đang mở' : 'Đã đóng' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang -->
      <div class="table-footer-pagination">
        <div class="footer-left-info">
          Hiển thị <strong>{{ historyList.length ? (pagination.page * pagination.size + 1) : 0 }} - {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }}</strong> / <strong>{{ pagination.totalElements }}</strong> lượt giao ca
        </div>

        <div class="footer-center-buttons">
          <button
            type="button"
            class="pg-btn"
            :disabled="pagination.page === 0"
            @click="goToPage(pagination.page - 1)"
          >
            ‹
          </button>
          <button
            v-for="p in visiblePages"
            :key="p"
            type="button"
            class="pg-btn"
            :class="{ active: p === pagination.page }"
            @click="goToPage(p)"
          >
            {{ p + 1 }}
          </button>
          <button
            type="button"
            class="pg-btn"
            :disabled="pagination.page >= pagination.totalPages - 1"
            @click="goToPage(pagination.page + 1)"
          >
            ›
          </button>
        </div>

        <div class="footer-right-size">
          <select v-model.number="pagination.size" class="form-control-minimal" @change="pagination.page = 0; loadHistory()">
            <option :value="5">5 bản ghi / trang</option>
            <option :value="10">10 bản ghi / trang</option>
            <option :value="20">20 bản ghi / trang</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Modal Mở Ca Làm Việc -->
    <div v-if="showMoCaModal" class="modal-backdrop-clean" @click.self="showMoCaModal = false">
      <div class="modal-dialog-clean">
        <div class="modal-header-clean">
          <h3 class="modal-title-clean">Mở ca làm việc</h3>
          <button type="button" class="btn-close-modal" @click="showMoCaModal = false">✕</button>
        </div>

        <div class="modal-body-clean">
          <!-- Chọn nhân viên -->
          <div class="form-group-item">
            <label class="modal-field-label">Nhân viên trực quầy <span class="required-star">*</span></label>
            <select v-model="moCaForm.nhanVienId" class="form-control-minimal modal-input">
              <option value="">-- Chọn nhân viên mở ca --</option>
              <option v-for="nv in activeEmployees" :key="nv.id" :value="nv.id">
                {{ nv.maNhanVien }} - {{ nv.tenNhanVien }}
              </option>
            </select>
          </div>

          <!-- Chọn ca làm việc -->
          <div class="form-group-item">
            <label class="modal-field-label">Ca làm việc <span class="required-star">*</span></label>
            <select v-model="moCaForm.caLamViecId" class="form-control-minimal modal-input">
              <option value="">-- Chọn ca làm việc --</option>
              <option v-for="ca in activeShifts" :key="ca.id" :value="ca.id">
                {{ ca.tenCa }} ({{ formatTime(ca.gioBatDau) }} - {{ formatTime(ca.gioKetThuc) }})
              </option>
            </select>
          </div>

          <!-- Tiền mặt đầu ca (Bắt buộc >= 0) -->
          <div class="form-group-item">
            <label class="modal-field-label">Tiền mặt đầu ca (VNĐ) <span class="required-star">*</span></label>
            <input
              type="number"
              min="0"
              step="1000"
              v-model.number="moCaForm.tienMatDauCa"
              placeholder="Nhập số tiền mặt đầu ca (ví dụ: 1000000)"
              class="form-control-minimal modal-input"
              :class="{ 'input-error-border': moCaErrors.tienMatDauCa }"
              @input="validateMoCaTien"
            />
            <span v-if="moCaErrors.tienMatDauCa" class="error-text-red">{{ moCaErrors.tienMatDauCa }}</span>
            <small class="hint-currency">{{ formatCurrency(moCaForm.tienMatDauCa || 0) }}</small>
          </div>

          <!-- Ghi chú mở ca -->
          <div class="form-group-item">
            <label class="modal-field-label">Ghi chú</label>
            <input
              type="text"
              v-model="moCaForm.ghiChu"
              placeholder="Ghi chú mở ca..."
              class="form-control-minimal modal-input"
            />
          </div>
        </div>

        <div class="modal-footer-clean">
          <button type="button" class="btn-cancel-modal" @click="showMoCaModal = false">Hủy</button>
          <button
            type="button"
            class="btn-save-modal"
            :disabled="submittingMoCa || !!moCaErrors.tienMatDauCa || !moCaForm.nhanVienId || !moCaForm.caLamViecId"
            @click="submitMoCa"
          >
            {{ submittingMoCa ? 'Đang mở...' : 'Xác nhận mở ca' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal Đóng Ca Làm Việc & Bàn Giao -->
    <div v-if="showDongCaModal" class="modal-backdrop-clean" @click.self="showDongCaModal = false">
      <div class="modal-dialog-clean modal-wide">
        <div class="modal-header-clean">
          <h3 class="modal-title-clean">Kết ca & Bàn giao tiền mặt</h3>
          <button type="button" class="btn-close-modal" @click="showDongCaModal = false">✕</button>
        </div>

        <div class="modal-body-clean">
          <!-- Thông tin ca đang đóng -->
          <div class="shift-info-summary">
            <div class="summary-line">
              <span>Nhân viên trực ca:</span>
              <strong>{{ closingTarget?.nhanVien?.tenNhanVien }} ({{ closingTarget?.nhanVien?.maNhanVien }})</strong>
            </div>
            <div class="summary-line">
              <span>Ca làm việc:</span>
              <strong>{{ closingTarget?.caLamViec?.tenCa }} (Mở lúc {{ formatDateTime(closingTarget?.thoiGianMoCa) }})</strong>
            </div>
            <div class="summary-line">
              <span>Tiền mặt đầu ca:</span>
              <strong class="text-blue">{{ formatCurrency(closingTarget?.tienMatDauCa || 0) }}</strong>
            </div>
            <div class="summary-line">
              <span>Tiền mặt bán trong ca:</span>
              <strong class="text-green">{{ formatCurrency(shiftSummary?.tienMatHeThong || 0) }} ({{ shiftSummary?.soHoaDon || 0 }} đơn)</strong>
            </div>
            <div class="summary-line">
              <span>Chuyển khoản trong ca:</span>
              <strong class="text-green">{{ formatCurrency(shiftSummary?.tienChuyenKhoanHeThong || 0) }}</strong>
            </div>
            <div class="summary-line highlight-expected">
              <span>Tiền mặt dự kiến két:</span>
              <strong class="text-black-bold">{{ formatCurrency(shiftSummary?.tienMatDuKien || 0) }}</strong>
            </div>
          </div>

          <!-- Nhập Tiền mặt cuối ca -->
          <div class="form-group-item">
            <label class="modal-field-label">Tiền mặt kiểm kê thực tế cuối ca (VNĐ) <span class="required-star">*</span></label>
            <input
              type="number"
              min="0"
              step="1000"
              v-model.number="dongCaForm.tienMatCuoiCa"
              placeholder="Nhập tổng số tiền mặt đếm được trong két..."
              class="form-control-minimal modal-input"
              :class="{ 'input-error-border': dongCaErrors.tienMatCuoiCa }"
              @input="computeClosingDiff"
            />
            <span v-if="dongCaErrors.tienMatCuoiCa" class="error-text-red">{{ dongCaErrors.tienMatCuoiCa }}</span>
            <small class="hint-currency">{{ formatCurrency(dongCaForm.tienMatCuoiCa || 0) }}</small>
          </div>

          <!-- Hiển thị Chênh lệch tính toán tự động -->
          <div class="diff-calculation-box" :class="calculatedDiff === 0 ? 'box-balanced' : 'box-unbalanced'">
            <div class="diff-calc-row">
              <span>Chênh lệch tiền mặt:</span>
              <strong :class="calculatedDiff === 0 ? 'text-green' : 'text-red'">
                {{ calculatedDiff > 0 ? '+' : '' }}{{ formatCurrency(calculatedDiff) }}
              </strong>
            </div>
            <div class="diff-desc-text">
              <span v-if="calculatedDiff === 0">✓ Khớp số tiền mặt dự kiến ({{ formatCurrency(shiftSummary?.tienMatDuKien || 0) }}), không phát sinh chênh lệch.</span>
              <span v-else-if="calculatedDiff > 0">⚠ Tiền mặt thực tế thừa {{ formatCurrency(calculatedDiff) }} so với dự kiến két ({{ formatCurrency(shiftSummary?.tienMatDuKien || 0) }}).</span>
              <span v-else>⚠ Tiền mặt thực tế thiếu {{ formatCurrency(Math.abs(calculatedDiff)) }} so với dự kiến két ({{ formatCurrency(shiftSummary?.tienMatDuKien || 0) }}).</span>
            </div>
          </div>

          <!-- Bắt buộc nhập lý do giải trình nếu chênh lệch != 0 -->
          <div v-if="calculatedDiff !== 0" class="form-group-item">
            <label class="modal-field-label">Lý do giải trình chênh lệch <span class="required-star">*</span></label>
            <textarea
              v-model="dongCaForm.lyDoChenhLech"
              rows="3"
              placeholder="Bắt buộc nhập rõ nguyên nhân phát sinh thừa/thiếu tiền mặt trong ca..."
              class="form-control-minimal modal-textarea"
              :class="{ 'input-error-border': dongCaErrors.lyDoChenhLech }"
              @input="validateDongCaReason"
            ></textarea>
            <span v-if="dongCaErrors.lyDoChenhLech" class="error-text-red">{{ dongCaErrors.lyDoChenhLech }}</span>
          </div>

          <!-- Ghi chú chung -->
          <div class="form-group-item">
            <label class="modal-field-label">Ghi chú bàn giao</label>
            <input
              type="text"
              v-model="dongCaForm.ghiChu"
              placeholder="Ghi chú bổ sung khi bàn giao..."
              class="form-control-minimal modal-input"
            />
          </div>
        </div>

        <div class="modal-footer-clean">
          <button type="button" class="btn-cancel-modal" @click="showDongCaModal = false">Hủy</button>
          <button
            type="button"
            class="btn-save-modal"
            :disabled="submittingDongCa || !canSubmitDongCa"
            @click="submitDongCa"
          >
            {{ submittingDongCa ? 'Đang đóng...' : 'Xác nhận đóng ca & Bàn giao' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/api'
import { showAlert, showConfirm, showToast } from '@/utils/dialog.js'
import { getCurrentUser } from '@/utils/auth.js'

const loading = ref(false)
const historyList = ref([])
const activeShifts = ref([])
const activeEmployees = ref([])
const currentOpenShift = ref(null)

const currentUser = getCurrentUser()

const filters = reactive({
  keyword: '',
  fromDate: '',
  toDate: '',
  trangThai: ''
})

const dateFilterError = ref('')

const pagination = reactive({
  page: 0,
  size: 10,
  totalPages: 1,
  totalElements: 0
})

const visiblePages = computed(() => {
  const pages = []
  for (let i = 0; i < pagination.totalPages; i++) pages.push(i)
  return pages
})

function formatCurrency(v) {
  if (v === null || v === undefined) return '0 đ'
  return Number(v).toLocaleString('vi-VN') + ' đ'
}

function formatTime(t) {
  if (!t) return '--:--'
  if (typeof t === 'string' && t.includes(':')) {
    const parts = t.split(':')
    return `${parts[0]}:${parts[1]}`
  }
  return t
}

function formatDateTime(dtStr) {
  if (!dtStr) return '--:--'
  try {
    const d = new Date(dtStr)
    const hh = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    return `${hh}:${min} ${dd}/${mm}`
  } catch (e) {
    return dtStr
  }
}

function getInitial(name) {
  if (!name) return 'A'
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

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pagination.page = 0
    loadHistory()
  }, 250)
}

function onDateChange() {
  if (filters.fromDate && filters.toDate) {
    if (filters.fromDate > filters.toDate) {
      dateFilterError.value = 'Từ ngày không được lớn hơn Đến ngày!'
      return
    }
  }
  dateFilterError.value = ''
  pagination.page = 0
  loadHistory()
}

function resetFilters() {
  filters.keyword = ''
  filters.fromDate = ''
  filters.toDate = ''
  filters.trangThai = ''
  dateFilterError.value = ''
  pagination.page = 0
  loadHistory()
}

function goToPage(p) {
  if (p < 0 || p >= pagination.totalPages) return
  pagination.page = p
  loadHistory()
}

async function loadHistory() {
  if (dateFilterError.value) return
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.fromDate && filters.fromDate.trim()) params.fromDate = filters.fromDate.trim()
    if (filters.toDate && filters.toDate.trim()) params.toDate = filters.toDate.trim()
    if (filters.trangThai !== '' && filters.trangThai !== null && filters.trangThai !== undefined) {
      params.trangThai = Number(filters.trangThai)
    }

    const res = await api.get('/api/lich-su-ca', { params })
    historyList.value = res.data?.content || []
    pagination.totalPages = res.data?.totalPages || 1
    pagination.totalElements = res.data?.totalElements || 0
  } catch (e) {
    showToast(e?.response?.data?.message || 'Không thể tải lịch sử ca', 'error')
  } finally {
    loading.value = false
  }
}

async function loadActiveData() {
  try {
    const [shiftRes, empRes] = await Promise.all([
      api.get('/api/ca-lam-viec', { params: { trangThai: 1 } }),
      api.get('/api/nhan-vien', { params: { trangThai: 1, size: 200 } })
    ])
    activeShifts.value = shiftRes.data || []
    activeEmployees.value = empRes.data?.content || []
  } catch (e) {}
}

async function checkCurrentOpenShift() {
  if (!currentUser?.id) return
  try {
    const res = await api.get(`/api/lich-su-ca/current/${currentUser.id}`)
    currentOpenShift.value = res.data || null
  } catch (e) {
    currentOpenShift.value = null
  }
}

// Modal Mở Ca
const showMoCaModal = ref(false)
const submittingMoCa = ref(false)
const moCaForm = reactive({
  nhanVienId: '',
  caLamViecId: '',
  tienMatDauCa: 0,
  ghiChu: ''
})

const moCaErrors = reactive({
  tienMatDauCa: ''
})

function openModalMoCa() {
  moCaForm.nhanVienId = currentUser?.id || ''
  moCaForm.caLamViecId = activeShifts.value[0]?.id || ''
  moCaForm.tienMatDauCa = 0
  moCaForm.ghiChu = ''
  moCaErrors.tienMatDauCa = ''
  showMoCaModal.value = true
}

function validateMoCaTien() {
  if (moCaForm.tienMatDauCa === null || moCaForm.tienMatDauCa === undefined || moCaForm.tienMatDauCa < 0) {
    moCaErrors.tienMatDauCa = 'Số tiền mặt đầu ca phải lớn hơn hoặc bằng 0'
  } else {
    moCaErrors.tienMatDauCa = ''
  }
}

async function submitMoCa() {
  validateMoCaTien()
  if (moCaErrors.tienMatDauCa) return

  submittingMoCa.value = true
  try {
    await api.post('/api/lich-su-ca/mo-ca', {
      nhanVienId: Number(moCaForm.nhanVienId),
      caLamViecId: Number(moCaForm.caLamViecId),
      tienMatDauCa: Number(moCaForm.tienMatDauCa),
      ghiChu: moCaForm.ghiChu ? moCaForm.ghiChu.trim() : null
    })

    showToast('Mở ca làm việc thành công!', 'success')
    showMoCaModal.value = false
    await checkCurrentOpenShift()
    loadHistory()
  } catch (e) {
    showAlert({
      title: 'Không thể mở ca',
      message: e?.response?.data?.message || 'Lỗi khi mở ca',
      type: 'error'
    })
  } finally {
    submittingMoCa.value = false
  }
}

// Modal Đóng Ca
const showDongCaModal = ref(false)
const submittingDongCa = ref(false)
const closingTarget = ref(null)

const dongCaForm = reactive({
  tienMatCuoiCa: 0,
  lyDoChenhLech: '',
  ghiChu: ''
})

const dongCaErrors = reactive({
  tienMatCuoiCa: '',
  lyDoChenhLech: ''
})

const calculatedDiff = ref(0)

const shiftSummary = ref({
  tienMatDauCa: 0,
  tienMatHeThong: 0,
  tienChuyenKhoanHeThong: 0,
  tongTienHeThong: 0,
  tienMatDuKien: 0,
  soHoaDon: 0
})

async function openModalDongCa(shift) {
  closingTarget.value = shift
  dongCaForm.lyDoChenhLech = ''
  dongCaForm.ghiChu = ''
  dongCaErrors.tienMatCuoiCa = ''
  dongCaErrors.lyDoChenhLech = ''

  try {
    const res = await api.get(`/api/lich-su-ca/${shift.id}/summary`)
    shiftSummary.value = res.data || {
      tienMatDauCa: shift.tienMatDauCa || 0,
      tienMatHeThong: 0,
      tienChuyenKhoanHeThong: 0,
      tongTienHeThong: 0,
      tienMatDuKien: shift.tienMatDauCa || 0,
      soHoaDon: 0
    }
  } catch (e) {
    shiftSummary.value = {
      tienMatDauCa: shift.tienMatDauCa || 0,
      tienMatHeThong: 0,
      tienChuyenKhoanHeThong: 0,
      tongTienHeThong: 0,
      tienMatDuKien: shift.tienMatDauCa || 0,
      soHoaDon: 0
    }
  }

  dongCaForm.tienMatCuoiCa = Number(shiftSummary.value.tienMatDuKien || shift.tienMatDauCa || 0)
  computeClosingDiff()
  showDongCaModal.value = true
}

function computeClosingDiff() {
  if (dongCaForm.tienMatCuoiCa === null || dongCaForm.tienMatCuoiCa === undefined || dongCaForm.tienMatCuoiCa < 0) {
    dongCaErrors.tienMatCuoiCa = 'Số tiền mặt cuối ca phải lớn hơn hoặc bằng 0'
    calculatedDiff.value = 0
    return
  }
  dongCaErrors.tienMatCuoiCa = ''
  const expected = Number(shiftSummary.value.tienMatDuKien || closingTarget.value?.tienMatDauCa || 0)
  calculatedDiff.value = Number(dongCaForm.tienMatCuoiCa) - expected
  validateDongCaReason()
}

function validateDongCaReason() {
  if (calculatedDiff.value !== 0) {
    if (!dongCaForm.lyDoChenhLech || !dongCaForm.lyDoChenhLech.trim()) {
      dongCaErrors.lyDoChenhLech = 'Phát sinh chênh lệch tiền mặt. Bắt buộc phải nhập lý do giải trình!'
    } else {
      dongCaErrors.lyDoChenhLech = ''
    }
  } else {
    dongCaErrors.lyDoChenhLech = ''
  }
}

const canSubmitDongCa = computed(() => {
  if (dongCaErrors.tienMatCuoiCa) return false
  if (dongCaForm.tienMatCuoiCa === null || dongCaForm.tienMatCuoiCa === undefined || dongCaForm.tienMatCuoiCa < 0) return false
  if (calculatedDiff.value !== 0 && (!dongCaForm.lyDoChenhLech || !dongCaForm.lyDoChenhLech.trim())) return false
  return true
})

async function submitDongCa() {
  computeClosingDiff()
  if (!canSubmitDongCa.value) return

  submittingDongCa.value = true
  try {
    await api.post('/api/lich-su-ca/dong-ca', {
      id: closingTarget.value?.id,
      nhanVienId: closingTarget.value?.nhanVien?.id,
      caLamViecId: closingTarget.value?.caLamViec?.id,
      tienMatCuoiCa: Number(dongCaForm.tienMatCuoiCa),
      chenhLech: calculatedDiff.value,
      lyDoChenhLech: calculatedDiff.value !== 0 ? dongCaForm.lyDoChenhLech.trim() : null,
      ghiChu: dongCaForm.ghiChu ? dongCaForm.ghiChu.trim() : null
    })

    showToast('Kết ca & Bàn giao tiền mặt thành công!', 'success')
    showDongCaModal.value = false
    await checkCurrentOpenShift()
    loadHistory()
  } catch (e) {
    showAlert({
      title: 'Không thể đóng ca',
      message: e?.response?.data?.message || 'Lỗi khi đóng ca',
      type: 'error'
    })
  } finally {
    submittingDongCa.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadActiveData(), checkCurrentOpenShift(), loadHistory()])
})
</script>

<style scoped>
.lich-su-ca-page {
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
  padding: 1.25rem 1.4rem;
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
  background-color: #f1ede7;
  color: #8c7355;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(140, 115, 85, 0.12);
}

.page-main-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #2c3e50;
}

.top-actions-cluster {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-open-shift {
  height: 38px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid #c8b7a0;
  background-color: #d8cbb8;
  color: #4a3824;
  font-size: 0.86rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(140, 115, 85, 0.15);
}

.btn-open-shift:hover {
  background-color: #cbb9a3;
}

.btn-close-shift {
  height: 38px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid #573f2f;
  background-color: #6b4f3b;
  color: #ffffff;
  font-size: 0.86rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close-shift:hover {
  background-color: #553e2d;
}

/* Filter card */
.filter-card-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-grid-layout {
  display: grid;
  grid-template-columns: 2.2fr 1.2fr 1.2fr 1.2fr auto;
  gap: 12px;
  align-items: flex-end;
}

.filter-col {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label-tiny {
  font-size: 0.78rem;
  font-weight: 700;
  color: #6c787e;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.input-with-prefix-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 10px;
  color: #8c7355;
  pointer-events: none;
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
  border-color: #8c7355;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(140, 115, 85, 0.12);
}

.form-control-minimal.with-icon {
  padding-left: 32px;
}

.input-error-border {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}

.btn-refresh-clean {
  height: 38px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  color: #5d6c73;
  font-size: 0.84rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-refresh-clean:hover {
  background-color: #f5efe7;
  border-color: #b5a494;
}

.filter-error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #dc2626;
  font-size: 0.82rem;
  font-weight: 600;
  margin-top: -4px;
}

/* Table styling */
.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.minimal-styled-table th {
  background-color: #ddd3c5 !important;
  color: #3b2f23;
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
  background-color: #faf7f2;
}

.minimal-styled-table td {
  padding: 12px 14px;
  vertical-align: middle;
}

.account-cell-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.account-avatar-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #ffffff;
  font-size: 0.74rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.account-text-wrap {
  display: flex;
  flex-direction: column;
}

.account-username {
  font-size: 0.86rem;
  color: #1e293b;
}

.employee-realname {
  font-size: 0.76rem;
  color: #64748b;
}

.shift-name-tag {
  display: inline-block;
  background-color: #f1ede7;
  color: #8c7355;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
}

.badge-running-shift {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 0.74rem;
  font-weight: 700;
  background-color: #fef3c7;
  color: #b45309;
}

.cash-flow-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 0.8rem;
}

.cash-start {
  color: #64748b;
}

.cash-end {
  color: #0f172a;
  font-weight: 600;
}

.diff-amount-wrap {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.diff-badge {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
}

.diff-zero {
  background-color: #dcfce7;
  color: #15803d;
}

.diff-warn {
  background-color: #fee2e2;
  color: #b91c1c;
}

.diff-reason-tooltip {
  cursor: help;
  color: #3b82f6;
  font-size: 0.78rem;
}

.badge-shift-state {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.state-open {
  background-color: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.state-closed {
  background-color: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

/* Footer Pagination */
.table-footer-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 1.1rem;
  margin-top: 0.5rem;
  border-top: 1px solid #f1ece5;
  flex-wrap: wrap;
  gap: 12px;
}

.footer-left-info {
  font-size: 0.85rem;
  color: #64748b;
}

.footer-left-info strong {
  color: #1e293b;
}

.footer-center-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pg-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e2dbcf;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.84rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.pg-btn:hover:not(:disabled) {
  background-color: #f5efe7;
}

.pg-btn.active {
  background-color: #8c7355 !important;
  border-color: #8c7355 !important;
  color: #ffffff !important;
}

.pg-btn:disabled {
  opacity: 0.45;
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
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

.modal-dialog-clean.modal-wide {
  max-width: 560px;
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

.shift-info-summary {
  background-color: #fbf9f6;
  border: 1px solid #ebd9c8;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  font-size: 0.86rem;
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

.hint-currency {
  font-size: 0.78rem;
  color: #0284c7;
  font-weight: 600;
}

.error-text-red {
  color: #dc2626;
  font-size: 0.78rem;
  font-weight: 600;
}

.diff-calculation-box {
  padding: 10px 14px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.box-balanced {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.box-unbalanced {
  background-color: #fef2f2;
  border: 1px solid #fecaca;
}

.diff-calc-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.88rem;
}

.diff-desc-text {
  font-size: 0.8rem;
  color: #475569;
}

.modal-textarea {
  height: 75px;
  padding: 8px 10px;
  resize: vertical;
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
  border: 1px solid #8c7355;
  background: #8c7355;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save-modal:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.text-blue { color: #0284c7; }
.text-green { color: #16a34a; }
.text-red { color: #dc2626; }

@media (max-width: 900px) {
  .filter-grid-layout {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 600px) {
  .filter-grid-layout {
    grid-template-columns: 1fr;
  }
}
</style>

