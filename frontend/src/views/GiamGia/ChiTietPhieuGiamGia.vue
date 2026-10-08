<template>
  <div class="voucher-detail-wrapper">
    <!-- 1. Thanh tiêu đề Breadcrumb & Nút quay lại -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <button class="btn-back" @click="goBack" title="Quay lại danh sách">←</button>
        <span class="breadcrumb-text">
          Quản lý giảm giá <span class="slash">/</span>
          <router-link to="/phieu-giam-gia" class="breadcrumb-link">Phiếu giảm giá</router-link>
          <span class="slash">/</span>
          <b>Chi tiết phiếu</b>
        </span>
      </div>

      <!-- Cụm nút hành động phía trên bên phải -->
      <div class="header-actions">
        <button class="btn btn-secondary" @click="goBack">
          <span>✕</span> Hủy bỏ
        </button>
        <button class="btn btn-primary" @click="openConfirmModal" :disabled="loading || saving">
          <span v-if="saving" class="spin">🔄</span>
          <span v-else>💾</span>
          Lưu thay đổi
        </button>
      </div>
    </div>

    <!-- Trạng thái Đang nạp dữ liệu từ Backend -->
    <div v-if="loading" class="content-card status-msg-card">
      <div class="spinner">⏳</div>
      <span>Đang nạp dữ liệu chi tiết phiếu giảm giá từ SQL Server...</span>
    </div>

    <!-- Trạng thái Lỗi / Không tìm thấy -->
    <div v-else-if="errorMessage" class="content-card status-msg-card error-card">
      <div class="error-icon">⚠️</div>
      <div class="error-text">{{ errorMessage }}</div>
      <button class="btn btn-primary" @click="fetchVoucherDetail">Thử lại</button>
    </div>

    <!-- 2. Form chi tiết phiếu giảm giá -->
    <div v-else-if="form" class="detail-content-layout">
      <!-- Cột trái: Card tóm tắt tổng quan & Hình thức khách hàng -->
      <div class="left-summary-column">
        <!-- Card tổng quan nhanh -->
        <div class="content-card summary-card">
          <div class="card-icon-header">
            <div class="icon-avatar">🎟️</div>
            <div class="card-title-group">
              <span class="voucher-code-badge">{{ form.maPhieuGiamGia }}</span>
              <h3 class="voucher-name-preview">{{ form.tenPhieuGiamGia || 'Chưa đặt tên phiếu' }}</h3>
            </div>
          </div>

          <div class="divider"></div>

          <div class="summary-info-list">
            <div class="info-row">
              <span class="info-label">Hình thức:</span>
              <span :class="['badge-form', form.hinhThuc === 'Công khai' ? 'badge-public' : 'badge-personal']">
                ● {{ form.hinhThuc || 'Công khai' }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Trạng thái:</span>
              <span :class="['badge-status', currentStatusMeta.badgeClass]">
                {{ currentStatusMeta.text }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Mức giảm:</span>
              <span class="info-value font-bold text-highlight">
                {{ form.loaiPhieuGiamGia === 1 ? form.giaTriGiamGia + '%' : formatMoney(form.giaTriGiamGia) }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Số lượt sử dụng:</span>
              <span class="info-value font-medium">{{ (form.soLuongSuDung && form.soLuongSuDung > 0) ? form.soLuongSuDung + ' lượt' : 'Không giới hạn' }}</span>
            </div>

            <div class="info-row">
              <span class="info-label">Thời gian:</span>
              <span class="info-value text-xs text-muted-dark">
                {{ form.ngayBatDauStr ? formatDateTimeDisplay(form.ngayBatDauStr) : '---' }} ➔ {{ form.ngayKetThucStr ? formatDateTimeDisplay(form.ngayKetThucStr) : 'Vô hạn' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Card Thông tin khách hàng áp dụng (nếu là hình thức Cá nhân) -->
        <div v-if="form.hinhThuc === 'Cá nhân'" class="content-card customers-card">
          <div class="section-card-header">
            <div class="section-icon-box gold-soft-bg">
              <span>👥</span>
            </div>
            <div>
              <h4 class="section-title">Khách hàng áp dụng</h4>
              <p class="section-subtitle">{{ (form.danhSachKhachHang || []).length }} khách hàng được nhận riêng</p>
            </div>
          </div>

          <div class="customers-list-box">
            <div
                v-for="(kh, idx) in form.danhSachKhachHang"
                :key="idx"
                class="customer-tag-item"
            >
              <span class="cust-icon">👤</span>
              <span class="cust-name">{{ kh }}</span>
            </div>
            <div v-if="!form.danhSachKhachHang || form.danhSachKhachHang.length === 0" class="text-muted text-sm">
              Chưa có khách hàng liên kết cụ thể trong database.
            </div>
          </div>
        </div>
      </div>

      <!-- Cột phải: Khối form chỉnh sửa các trường dữ liệu -->
      <div class="right-form-column">
        <!-- Khối 1: Thông tin cơ bản phiếu -->
        <div class="content-card form-section-card">
          <div class="section-card-header">
            <div class="section-icon-box blue-soft-bg">
              <span>📝</span>
            </div>
            <div>
              <h4 class="section-title">Thông tin cơ bản</h4>
              <p class="section-subtitle">Mã phiếu, tên gọi và trạng thái hoạt động</p>
            </div>
          </div>

          <div class="form-grid-2">
            <!-- Mã phiếu (Chỉ đọc - không cho phép sửa mã định danh) -->
            <div class="form-field">
              <label>Mã phiếu giảm giá <span class="badge-lock">🔒 Cố định</span></label>
              <input
                  type="text"
                  v-model="form.maPhieuGiamGia"
                  disabled
                  class="input-disabled"
              />
            </div>

            <!-- Tên phiếu giảm giá -->
            <div class="form-field">
              <label>Tên phiếu giảm giá <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="form.tenPhieuGiamGia"
                  placeholder="Nhập tên phiếu giảm giá..."
                  :class="{ 'input-error': errors.tenPhieuGiamGia }"
              />
              <span v-if="errors.tenPhieuGiamGia" class="error-msg">{{ errors.tenPhieuGiamGia }}</span>
            </div>

            <!-- Trạng thái hoạt động -->
            <div class="form-field">
              <label>Trạng thái hoạt động <span class="required">*</span></label>
              <select v-model.number="form.trangThai">
                <option :value="1">Đang hoạt động (Bật)</option>
                <option :value="0">Ngừng hoạt động (Tắt)</option>
              </select>
            </div>

            <!-- Hình thức (Hiển thị) -->
            <div class="form-field">
              <label>Hình thức phiếu</label>
              <input
                  type="text"
                  :value="form.hinhThuc || 'Công khai'"
                  disabled
                  class="input-disabled"
              />
            </div>
          </div>
        </div>

        <!-- Khối 2: Cấu hình giá trị và điều kiện giảm -->
        <div class="content-card form-section-card">
          <div class="section-card-header">
            <div class="section-icon-box gold-soft-bg">
              <span>💰</span>
            </div>
            <div>
              <h4 class="section-title">Giá trị & Điều kiện áp dụng</h4>
              <p class="section-subtitle">Loại giảm giá, mức giảm, giá trị đơn tối thiểu</p>
            </div>
          </div>

          <div class="form-grid-2">
            <!-- Loại giảm giá -->
            <div class="form-field">
              <label>Loại giảm giá <span class="required">*</span></label>
              <select v-model.number="form.loaiPhieuGiamGia">
                <option :value="1">Giảm theo phần trăm (%)</option>
                <option :value="2">Giảm tiền mặt trực tiếp (VNĐ)</option>
              </select>
            </div>

            <!-- Giá trị giảm -->
            <div class="form-field">
              <label>
                Mức giảm
                <span class="required">*</span>
                <span class="unit-tag">({{ form.loaiPhieuGiamGia === 1 ? '%' : 'VNĐ' }})</span>
              </label>
              <div class="input-with-addon">
                <input
                    type="number"
                    v-model.number="form.giaTriGiamGia"
                    :min="form.loaiPhieuGiamGia === 1 ? 1 : 1000"
                    :max="form.loaiPhieuGiamGia === 1 ? 100 : 999999999"
                    :placeholder="form.loaiPhieuGiamGia === 1 ? 'Nhập từ 1 - 100%' : 'VD: 12 -> 12.000 đ'"
                    @blur="handleMoneyBlur('giaTriGiamGia')"
                    :class="{ 'input-error': errors.giaTriGiamGia }"
                />
                <span class="input-addon">{{ form.loaiPhieuGiamGia === 1 ? '%' : 'đ' }}</span>
              </div>
              <span v-if="form.loaiPhieuGiamGia === 2 && form.giaTriGiamGia" class="field-hint text-blue font-medium">
                ≈ {{ formatMoney(form.giaTriGiamGia) }}
              </span>
              <span v-if="errors.giaTriGiamGia" class="error-msg">{{ errors.giaTriGiamGia }}</span>
            </div>

            <!-- Giảm tối đa (chỉ có ý nghĩa khi giảm theo %) -->
            <div class="form-field">
              <label>
                Giảm tối đa (VNĐ)
                <span v-if="form.loaiPhieuGiamGia === 2" class="field-optional">(Không bắt buộc với tiền mặt)</span>
              </label>
              <div class="input-with-addon">
                <input
                    type="number"
                    v-model.number="form.giamToiDa"
                    min="1000"
                    :disabled="form.loaiPhieuGiamGia === 2"
                    :placeholder="form.loaiPhieuGiamGia === 2 ? 'Không áp dụng' : 'VD: 50 -> 50.000 đ'"
                    @blur="handleMoneyBlur('giamToiDa')"
                    :class="{ 'input-error': errors.giamToiDa }"
                />
                <span class="input-addon">đ</span>
              </div>
              <span v-if="form.loaiPhieuGiamGia === 1 && form.giamToiDa" class="field-hint text-blue font-medium">
                ≈ {{ formatMoney(form.giamToiDa) }}
              </span>
              <span v-if="errors.giamToiDa" class="error-msg">{{ errors.giamToiDa }}</span>
            </div>

            <!-- Đơn hàng tối thiểu -->
            <div class="form-field">
              <label>Hóa đơn tối thiểu (VNĐ) <span class="required">*</span></label>
              <div class="input-with-addon">
                <input
                    type="number"
                    v-model.number="form.hoaDonToiThieu"
                    min="0"
                    placeholder="VD: 700 -> 700.000 đ"
                    @blur="handleMoneyBlur('hoaDonToiThieu')"
                    :class="{ 'input-error': errors.hoaDonToiThieu }"
                />
                <span class="input-addon">đ</span>
              </div>
              <span v-if="form.hoaDonToiThieu" class="field-hint text-blue font-medium">
                ≈ {{ formatMoney(form.hoaDonToiThieu) }}
              </span>
              <span v-if="errors.hoaDonToiThieu" class="error-msg">{{ errors.hoaDonToiThieu }}</span>
            </div>

            <!-- Số lượng sử dụng -->
            <div class="form-field full-width">
              <label>
                Số lượt sử dụng
                <span class="field-hint-inline">(Để trống = Không giới hạn)</span>
              </label>
              <input
                  type="number"
                  v-model.number="form.soLuongSuDung"
                  min="1"
                  placeholder="Để trống = Không giới hạn số lượt dùng..."
                  :class="{ 'input-error': errors.soLuongSuDung }"
              />
              <span class="field-hint" v-if="!form.soLuongSuDung">
                💡 Không nhập số lượng = Áp dụng <b>Không giới hạn</b> số lượt sử dụng.
              </span>
              <span v-if="errors.soLuongSuDung" class="error-msg">{{ errors.soLuongSuDung }}</span>
            </div>
          </div>
        </div>

        <!-- Khối 3: Thời gian áp dụng -->
        <div class="content-card form-section-card">
          <div class="section-card-header">
            <div class="section-icon-box green-soft-bg">
              <span>📅</span>
            </div>
            <div>
              <h4 class="section-title">Thời gian hiệu lực</h4>
              <p class="section-subtitle">Ngày bắt đầu và ngày kết thúc chương trình</p>
            </div>
          </div>

          <div class="form-grid-2">
            <!-- Thời gian bắt đầu -->
            <div class="form-field">
              <label>Thời gian bắt đầu <span class="required">*</span></label>
              <input
                  type="datetime-local"
                  v-model="form.ngayBatDauStr"
                  :class="{ 'input-error': errors.ngayBatDau }"
              />
              <span v-if="errors.ngayBatDau" class="error-msg">{{ errors.ngayBatDau }}</span>
            </div>

            <!-- Thời gian kết thúc -->
            <div class="form-field">
              <div class="field-label-row">
                <label>
                  Thời gian kết thúc
                  <span class="field-hint-inline">(Để trống = Vô hạn)</span>
                </label>
                <button
                    v-if="form.ngayKetThucStr"
                    type="button"
                    class="btn-clear-date"
                    @click="form.ngayKetThucStr = ''"
                    title="Bấm để không giới hạn thời gian kết thúc"
                >
                  ✕ Đặt vô hạn
                </button>
              </div>
              <input
                  type="datetime-local"
                  v-model="form.ngayKetThucStr"
                  :min="form.ngayBatDauStr || ''"
                  :class="{ 'input-error': errors.ngayKetThuc }"
              />
              <span class="field-hint" v-if="!form.ngayKetThucStr">
                💡 Không chọn thời gian kết thúc = Áp dụng <b>Vô hạn</b>.
              </span>
              <span v-if="errors.ngayKetThuc" class="error-msg">{{ errors.ngayKetThuc }}</span>
            </div>
          </div>
        </div>

        <!-- Cụm nút thao tác dưới chân trang -->
        <div class="form-submit-bar">
          <button class="btn btn-secondary" @click="goBack">
            <span>←</span> Quay lại danh sách
          </button>
          <button class="btn btn-primary" @click="openConfirmModal" :disabled="saving">
            <span v-if="saving" class="spin">🔄</span>
            <span v-else>💾</span>
            Cập nhật phiếu giảm giá
          </button>
        </div>
      </div>
    </div>

    <!-- 3. POPUP MODAL XÁC NHẬN CẬP NHẬT DATABASE ĐẸP MẮT -->
    <div v-if="showConfirmModal" class="modal-overlay" @click.self="closeConfirmModal">
      <div class="modal-box confirm-modal-box">
        <div class="confirm-modal-header">
          <div class="confirm-warning-icon">⚠️</div>
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">Xác nhận cập nhật</h3>
          </div>
          <button class="modal-close-btn" @click="closeConfirmModal">✕</button>
        </div>

        <div class="confirm-modal-body">
          <p class="confirm-message-text">
            Bạn có chắc chắn muốn lưu các thay đổi cho phiếu giảm giá
            <b class="text-blue">[{{ form.maPhieuGiamGia }}]</b> không?
          </p>

          <!-- Bảng tóm tắt các thông số sắp cập nhật -->
          <div class="confirm-summary-panel">
            <div class="summary-line">
              <span class="s-label">Tên phiếu:</span>
              <span class="s-val font-medium">{{ form.tenPhieuGiamGia }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Mức giảm:</span>
              <span class="s-val font-bold text-highlight">
                {{ form.loaiPhieuGiamGia === 1 ? form.giaTriGiamGia + '%' : formatMoney(form.giaTriGiamGia) }}
              </span>
            </div>
            <div class="summary-line">
              <span class="s-label">Thời hạn:</span>
              <span class="s-val">{{ formatDateTimeDisplay(form.ngayBatDauStr) }} ➔ {{ formatDateTimeDisplay(form.ngayKetThucStr) }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Lượt sử dụng:</span>
              <span class="s-val">{{ (form.soLuongSuDung && form.soLuongSuDung > 0) ? form.soLuongSuDung + ' lượt' : 'Không giới hạn' }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Trạng thái:</span>
              <span class="s-val font-medium">{{ form.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}</span>
            </div>
          </div>
        </div>

        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="closeConfirmModal" :disabled="saving">
            Hủy bỏ
          </button>
          <button class="btn btn-primary btn-save-confirm" @click="submitUpdate" :disabled="saving">
            <span v-if="saving" class="spin">🔄</span>
            <span v-else>✔</span>
            {{ saving ? 'Đang cập nhật...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 4. POPUP MODAL CẢNH BÁO LỖI NHẬP LIỆU (THAY THẾ ALERT XẤU CỦA TRÌNH DUYỆT) -->
    <div v-if="showErrorModal" class="modal-overlay" @click.self="showErrorModal = false">
      <div class="modal-box error-modal-box">
        <div class="error-modal-header">
          <div class="error-warning-icon">⚠️</div>
          <div class="error-title-wrap">
            <h3 class="error-title">Thông tin chưa hợp lệ</h3>
            <p class="error-subtitle">Vui lòng kiểm tra lại các trường dữ liệu dưới đây</p>
          </div>
          <button class="modal-close-btn" @click="showErrorModal = false">✕</button>
        </div>

        <div class="error-modal-body">
          <div class="error-list-panel">
            <div v-for="(msg, field) in errors" :key="field" class="error-item-line">
              <span class="error-bullet">❌</span>
              <span class="error-text-content">{{ msg }}</span>
            </div>
          </div>
          <div class="error-hint-panel">
            💡 <span>Gợi ý: Nếu chọn <b>Giảm theo phần trăm (%)</b>, mức giảm phải nằm trong khoảng từ <b>1% đến 100%</b>.</span>
          </div>
        </div>

        <div class="error-modal-footer">
          <button class="btn btn-primary btn-error-close" @click="showErrorModal = false">
            Xác nhận
          </button>
        </div>
      </div>
    </div>

    <!-- 5. THÔNG BÁO THÀNH CÔNG (TOAST NOTIFICATION) -->
    <div v-if="showSuccessToast" class="toast-success">
      <div class="toast-icon">✅</div>
      <div class="toast-text">
        <b>Thành công!</b>
        <p>Đã cập nhật thông tin phiếu giảm giá vào cơ sở dữ liệu.</p>
      </div>
      <button class="toast-close" @click="showSuccessToast = false">✕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../../api'

const router = useRouter()
const route = useRoute()

const voucherId = route.params.id


const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const showConfirmModal = ref(false)
const showErrorModal = ref(false)
const showSuccessToast = ref(false)


const form = ref(null)


const errors = ref({})


const formatMoney = (val) => {
  if (val == null || val === '') return '0 đ'
  return Number(val).toLocaleString('vi-VN') + ' đ'
}

const formatDateDisplay = (dateStr) => {
  if (!dateStr) return '-'
  const parts = dateStr.split('-')
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`
  }
  return dateStr
}

const formatDateTimeDisplay = (dtStr) => {
  if (!dtStr) return 'Vô hạn'
  try {
    const d = new Date(dtStr)
    if (isNaN(d.getTime())) return dtStr
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes} ${day}/${month}/${year}`
  } catch (e) {
    return dtStr
  }
}

const isoToDateTimeInput = (isoStr) => {
  if (!isoStr) return ''
  try {
    const d = new Date(isoStr)
    if (isNaN(d.getTime())) return ''
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day}T${hours}:${minutes}`
  } catch (e) {
    return ''
  }
}

const dateTimeInputToIso = (dtStr) => {
  if (!dtStr) return null
  try {
    const d = new Date(dtStr)
    if (isNaN(d.getTime())) return null
    return d.toISOString()
  } catch (e) {
    return null
  }
}


const fetchVoucherDetail = async () => {
  if (!voucherId) {
    errorMessage.value = 'Không tìm thấy ID phiếu giảm giá!'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await api.get(`/api/phieu-giam-gia/${voucherId}`)
    const data = res.data

    if (!data) {
      errorMessage.value = `Không tìm thấy phiếu giảm giá có ID = ${voucherId} trong database!`
      return
    }

    form.value = {
      id: data.id,
      maPhieuGiamGia: data.maPhieuGiamGia,
      tenPhieuGiamGia: data.tenPhieuGiamGia || '',
      loaiPhieuGiamGia: data.loaiPhieuGiamGia ?? 1,
      giaTriGiamGia: data.giaTriGiamGia ?? 0,
      giamToiDa: data.giamToiDa ?? null,
      hoaDonToiThieu: data.hoaDonToiThieu ?? 0,
      soLuongSuDung: data.soLuongSuDung ?? 0,
      ngayBatDauStr: isoToDateTimeInput(data.ngayBatDau),
      ngayKetThucStr: isoToDateTimeInput(data.ngayKetThuc),
      trangThai: data.trangThai ?? 1,
      hinhThuc: data.hinhThuc || (data.soKhachHang > 0 ? 'Cá nhân' : 'Công khai'),
      soKhachHang: data.soKhachHang || 0,
      danhSachKhachHang: data.danhSachKhachHang || []
    }
  } catch (err) {
    console.error('Lỗi khi nạp chi tiết phiếu giảm giá:', err)
    errorMessage.value = 'Không thể kết nối đến Backend Spring Boot hoặc phiếu không tồn tại.'
  } finally {
    loading.value = false
  }
}

const currentStatusMeta = computed(() => {
  if (!form.value) return { text: 'Không xác định', badgeClass: 'status-inactive' }

  const now = new Date()
  const endObj = form.value.ngayKetThucStr ? new Date(form.value.ngayKetThucStr) : null
  const startObj = form.value.ngayBatDauStr ? new Date(form.value.ngayBatDauStr) : null

  if (endObj && endObj < now) {
    return { text: 'Đã hết hạn', badgeClass: 'status-expired' }
  }
  if (form.value.trangThai === 0) {
    return { text: 'Ngừng hoạt động', badgeClass: 'status-inactive' }
  }
  if (startObj && startObj > now) {
    return { text: 'Sắp diễn ra', badgeClass: 'status-upcoming' }
  }
  return { text: 'Đang hoạt động', badgeClass: 'status-active' }
})

const autoAppendThreeZeros = (field) => {
  if (!form.value) return
  const val = form.value[field]
  if (val != null && val !== '') {
    const num = Number(val)
    if (!isNaN(num) && num > 0 && num < 1000) {
      form.value[field] = Math.round(num * 1000)
    }
  }
}

const handleMoneyBlur = (field) => {
  if (!form.value) return
  if (field === 'giaTriGiamGia' && form.value.loaiPhieuGiamGia !== 2) {
    return
  }
  if (field === 'giamToiDa' && form.value.loaiPhieuGiamGia !== 1) {
    return
  }
  autoAppendThreeZeros(field)
}

const normalizeMoneyInputs = () => {
  if (!form.value) return
  if (form.value.loaiPhieuGiamGia === 2) {
    autoAppendThreeZeros('giaTriGiamGia')
  }
  autoAppendThreeZeros('hoaDonToiThieu')
  if (form.value.loaiPhieuGiamGia === 1) {
    autoAppendThreeZeros('giamToiDa')
  }
}

watch(() => form.value?.loaiPhieuGiamGia, (newVal) => {
  if (form.value && newVal === 2) {
    form.value.giamToiDa = null
    if (errors.value.giamToiDa) delete errors.value.giamToiDa
    if (form.value.giaTriGiamGia != null && form.value.giaTriGiamGia !== '') {
      autoAppendThreeZeros('giaTriGiamGia')
    }
  } else if (form.value && newVal === 1) {
    if (form.value.giaTriGiamGia && form.value.giaTriGiamGia > 100) {
      form.value.giaTriGiamGia = null
    }
  }
  if (errors.value.giaTriGiamGia) delete errors.value.giaTriGiamGia
})

const validateForm = () => {
  normalizeMoneyInputs()
  errors.value = {}

  // 1. Tên phiếu giảm giá
  if (!form.value.tenPhieuGiamGia || !form.value.tenPhieuGiamGia.trim()) {
    errors.value.tenPhieuGiamGia = 'Vui lòng nhập tên phiếu giảm giá!'
  } else if (form.value.tenPhieuGiamGia.trim().length > 255) {
    errors.value.tenPhieuGiamGia = 'Tên phiếu giảm giá không được vượt quá 255 ký tự!'
  }

  // 2. Mức giảm giá
  if (form.value.giaTriGiamGia === null || form.value.giaTriGiamGia === undefined || form.value.giaTriGiamGia === '') {
    errors.value.giaTriGiamGia = 'Vui lòng nhập mức giảm giá!'
  } else {
    const val = Number(form.value.giaTriGiamGia)
    if (isNaN(val) || val <= 0) {
      errors.value.giaTriGiamGia = 'Mức giảm giá phải lớn hơn 0!'
    } else if (form.value.loaiPhieuGiamGia === 1) {
      if (val < 1 || val > 100) {
        errors.value.giaTriGiamGia = 'Mức giảm theo phần trăm phải từ 1% đến 100%!'
      }
    } else if (form.value.loaiPhieuGiamGia === 2) {
      if (val < 1000) {
        errors.value.giaTriGiamGia = 'Mức giảm tiền mặt phải tối thiểu từ 1.000 đ!'
      } else if (form.value.hoaDonToiThieu !== null && form.value.hoaDonToiThieu !== '' && val > Number(form.value.hoaDonToiThieu)) {
        errors.value.giaTriGiamGia = 'Mức giảm tiền mặt không được lớn hơn hóa đơn tối thiểu!'
      }
    }
  }

  // 3. Giảm tối đa (nếu là loại %)
  if (form.value.loaiPhieuGiamGia === 1 && form.value.giamToiDa !== null && form.value.giamToiDa !== undefined && form.value.giamToiDa !== '') {
    const maxVal = Number(form.value.giamToiDa)
    if (isNaN(maxVal) || maxVal < 1000) {
      errors.value.giamToiDa = 'Mức giảm tối đa phải tối thiểu từ 1.000 đ!'
    }
  }

  // 4. Hóa đơn tối thiểu
  if (form.value.hoaDonToiThieu === null || form.value.hoaDonToiThieu === undefined || form.value.hoaDonToiThieu === '') {
    errors.value.hoaDonToiThieu = 'Vui lòng nhập giá trị hóa đơn tối thiểu!'
  } else {
    const minBill = Number(form.value.hoaDonToiThieu)
    if (isNaN(minBill) || minBill < 0) {
      errors.value.hoaDonToiThieu = 'Hóa đơn tối thiểu không được nhỏ hơn 0!'
    } else if (minBill > 0 && minBill < 1000) {
      errors.value.hoaDonToiThieu = 'Hóa đơn tối thiểu phải từ 1.000 đ trở lên (hoặc bằng 0)!'
    }
  }

  // 5. Số lượng sử dụng (Tùy chọn: để trống là không giới hạn)
  if (form.value.soLuongSuDung !== null && form.value.soLuongSuDung !== undefined && form.value.soLuongSuDung !== '') {
    const qty = Number(form.value.soLuongSuDung)
    if (isNaN(qty) || !Number.isInteger(qty) || qty <= 0) {
      errors.value.soLuongSuDung = 'Số lượt sử dụng phải là số nguyên lớn hơn 0!'
    }
  }

  // 6. Thời gian bắt đầu
  if (!form.value.ngayBatDauStr) {
    errors.value.ngayBatDau = 'Vui lòng chọn thời gian bắt đầu!'
  }

  // 7. Thời gian kết thúc (Nếu để trống thì là vô hạn, không validate)
  if (form.value.ngayKetThucStr) {
    if (form.value.ngayBatDauStr && new Date(form.value.ngayKetThucStr) < new Date(form.value.ngayBatDauStr)) {
      errors.value.ngayKetThuc = 'Thời gian kết thúc phải diễn ra sau hoặc cùng thời điểm với thời gian bắt đầu!'
    }
  }

  return Object.keys(errors.value).length === 0
}

const openConfirmModal = () => {
  if (!validateForm()) {
    showErrorModal.value = true
    return
  }
  showConfirmModal.value = true
}

const closeConfirmModal = () => {
  showConfirmModal.value = false
}

const submitUpdate = async () => {
  saving.value = true

  const payload = {
    id: form.value.id,
    maPhieuGiamGia: form.value.maPhieuGiamGia,
    tenPhieuGiamGia: form.value.tenPhieuGiamGia.trim(),
    loaiPhieuGiamGia: form.value.loaiPhieuGiamGia,
    giaTriGiamGia: form.value.giaTriGiamGia,
    giamToiDa: form.value.loaiPhieuGiamGia === 1 && form.value.giamToiDa ? Number(form.value.giamToiDa) : null,
    hoaDonToiThieu: form.value.hoaDonToiThieu != null ? Number(form.value.hoaDonToiThieu) : 0,
    soLuongSuDung: (form.value.soLuongSuDung && Number(form.value.soLuongSuDung) > 0) ? Number(form.value.soLuongSuDung) : 0,
    ngayBatDau: dateTimeInputToIso(form.value.ngayBatDauStr),
    ngayKetThuc: dateTimeInputToIso(form.value.ngayKetThucStr),
    trangThai: form.value.trangThai
  }

  try {
    await api.put(`/api/phieu-giam-gia/${form.value.id}`, payload)
    showConfirmModal.value = false
    showSuccessToast.value = true
    setTimeout(() => {
      showSuccessToast.value = false
      router.push('/phieu-giam-gia')
    }, 1200)
  } catch (err) {
    console.error('Lỗi khi cập nhật vào SQL Server:', err)
    let msg = 'Cập nhật phiếu giảm giá thất bại!'
    if (err.response?.data) {
      if (typeof err.response.data === 'string') {
        msg = err.response.data
      } else if (err.response.data.message) {
        msg = err.response.data.message
      } else if (err.response.data.error) {
        msg = err.response.data.error
      }
    } else if (err.message) {
      msg = err.message
    }
    alert(msg)
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.push('/phieu-giam-gia')
}

onMounted(() => {
  fetchVoucherDetail()
})
</script>

<style scoped>
.voucher-detail-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header Breadcrumb & Actions */
.breadcrumb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.95rem;
}

.btn-back {
  background-color: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 1.1rem;
  cursor: pointer;
  color: #496883;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #eaf1f4;
  border-color: #496883;
  transform: translateX(-2px);
}

.breadcrumb-text {
  color: #6c777d;
}

.breadcrumb-link {
  color: #496883;
  text-decoration: none;
  font-weight: 600;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.slash {
  margin: 0 0.35rem;
  color: #c4beaf;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* Nút bấm chung */
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
}

.btn-secondary {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #faeed7;
}

.btn-primary {
  background-color: var(--blue, #496883);
  border: none;
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background-color: #38536b;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* Loading & Error msg */
.status-msg-card {
  padding: 3rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  font-size: 1rem;
  color: #60727a;
}

.error-card {
  color: #c0392b;
}

.spinner {
  font-size: 2rem;
  animation: spin 1.5s infinite linear;
}

/* Bố cục 2 cột chính */
.detail-content-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 1.4rem;
  align-items: start;
}

/* Content card chuẩn FF T-shirt */
.content-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #eeebe3);
  box-shadow: 0 3px 12px rgba(60, 50, 30, 0.04);
  padding: 1.4rem;
  box-sizing: border-box;
}

/* Cột trái */
.left-summary-column {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.card-icon-header {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.icon-avatar {
  width: 48px;
  height: 48px;
  background-color: #f7eee1;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 1.6rem;
}

.voucher-code-badge {
  font-family: monospace, sans-serif;
  font-weight: 700;
  font-size: 0.88rem;
  color: #496883;
  background-color: #eaf1f4;
  padding: 0.2rem 0.55rem;
  border-radius: 6px;
  display: inline-block;
  margin-bottom: 0.25rem;
}

.voucher-name-preview {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c383e;
  word-break: break-word;
}

.divider {
  height: 1px;
  background-color: #f1eee7;
  margin: 1.1rem 0;
}

.summary-info-list {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.92rem;
}

.info-label {
  color: #7b888e;
  font-weight: 600;
}

.info-value {
  color: #2b383e;
}

.text-highlight {
  color: #c0392b;
  font-size: 1.05rem;
}

/* Khách hàng cá nhân list */
.customers-list-box {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 220px;
  overflow-y: auto;
  margin-top: 0.85rem;
}

.customer-tag-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: #fbf9f5;
  border: 1px solid #eeebe3;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  font-size: 0.88rem;
  color: #496883;
  font-weight: 600;
}

/* Cột phải: Form */
.right-form-column {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.section-card-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  margin-bottom: 1.2rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px dashed #efeae0;
}

.section-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 1.15rem;
}

.blue-soft-bg {
  background-color: #eaf1f4;
  color: #496883;
}

.gold-soft-bg {
  background-color: #fdf3e5;
  color: #b38536;
}

.green-soft-bg {
  background-color: #edf6ef;
  color: #4c8a5a;
}

.section-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #33444d;
}

.section-subtitle {
  margin: 0.15rem 0 0;
  font-size: 0.85rem;
  color: #8c9597;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.1rem 1.4rem;
}

.full-width {
  grid-column: 1 / -1;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #4c5d65;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.field-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-clear-date {
  background: none;
  border: none;
  color: #c0392b;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-clear-date:hover {
  background-color: #fde8e8;
  text-decoration: underline;
}

.field-hint-inline {
  font-size: 0.78rem;
  color: #8c9597;
  font-weight: normal;
}

.required {
  color: #c0392b;
}

.unit-tag {
  color: #8c9597;
  font-weight: 500;
}

.field-optional {
  font-size: 0.78rem;
  color: #9aa0a0;
  font-weight: 400;
}

.badge-lock {
  font-size: 0.75rem;
  background-color: #f1eee7;
  color: #7b888e;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  font-weight: 600;
}

.form-field input,
.form-field select {
  height: 2.5rem;
  padding: 0 0.85rem;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #faf9f6;
  font-size: 0.92rem;
  color: #2b383e;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  border-color: #496883;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.08);
}

.input-disabled {
  background-color: #f4f2ec !important;
  color: #7d898f !important;
  cursor: not-allowed;
}

.input-error {
  border-color: #c0392b !important;
  background-color: #fff9f9 !important;
}

.error-msg {
  font-size: 0.8rem;
  color: #c0392b;
  font-weight: 600;
}

.field-hint {
  font-size: 0.78rem;
  color: #9aa0a0;
}

.input-with-addon {
  display: flex;
  align-items: center;
}

.input-with-addon input {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  flex: 1;
}

.input-addon {
  height: 2.5rem;
  padding: 0 0.85rem;
  background-color: #eeebe3;
  border: 1px solid #e9e5db;
  border-left: none;
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.88rem;
  color: #60727a;
  font-weight: 700;
}

.form-submit-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.85rem;
  padding: 1.1rem 1.4rem;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #eeebe3;
}

/* Badge Hình thức & Trạng thái */
.badge-form {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  padding: 0.35rem 0.75rem;
  border-radius: 14px;
  font-weight: 600;
}

.badge-public {
  background-color: #fcf3e6;
  color: #b38536;
}

.badge-personal {
  background-color: #eaf1f5;
  color: #496883;
}

.badge-status {
  display: inline-block;
  font-size: 0.82rem;
  font-weight: 700;
  padding: 0.35rem 0.85rem;
  border-radius: 14px;
}

.status-active {
  background-color: #edf6ef;
  color: #4c8a5a;
}

.status-inactive {
  background-color: #f6f6f6;
  color: #7f8c8d;
}

.status-expired {
  background-color: #fdeeee;
  color: #c0392b;
  border: 1px solid #fadad7;
}

.status-upcoming {
  background-color: #eaf1f8;
  color: #2980b9;
  border: 1px solid #d4e6f1;
}

.font-bold {
  font-weight: 700;
}

.font-medium {
  font-weight: 600;
}

/* POPUP MODAL XÁC NHẬN (CONFIRMATION DIALOG) */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(30, 40, 45, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 520px;
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

.confirm-warning-icon {
  font-size: 1.8rem;
}

.confirm-title-wrap {
  flex: 1;
}

.confirm-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #8c6328;
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
}

.modal-close-btn:hover {
  color: #c0392b;
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
}

.confirm-summary-panel {
  background-color: #faf9f6;
  border: 1px solid #efeae0;
  border-radius: 8px;
  padding: 0.9rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
}

.s-label {
  color: #7b888e;
  font-weight: 600;
}

.s-val {
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

.btn-save-confirm {
  background-color: #4c8a5a;
}

.btn-save-confirm:hover:not(:disabled) {
  background-color: #3b7047;
}

/* Toast thông báo */
.toast-success {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  background-color: #ffffff;
  border-left: 5px solid #4c8a5a;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  z-index: 2000;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.toast-icon {
  font-size: 1.5rem;
}

.toast-text b {
  font-size: 0.95rem;
  color: #2b383e;
}

.toast-text p {
  margin: 0.2rem 0 0;
  font-size: 0.85rem;
  color: #65757d;
}

.toast-close {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  color: #9aa0a0;
  cursor: pointer;
  margin-left: 0.5rem;
}

.spin {
  display: inline-block;
  animation: spin 1s infinite linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* POPUP MODAL CẢNH BÁO LỖI NHẬP LIỆU ĐẸP MẮT */
.error-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.22);
  overflow: hidden;
  animation: popIn 0.2s ease-out;
  border: 1px solid #f9d6d5;
}

.error-modal-header {
  padding: 1.15rem 1.35rem;
  background-color: #fff4f4;
  border-bottom: 1px solid #fcdcdc;
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.error-warning-icon {
  font-size: 1.8rem;
}

.error-title-wrap {
  flex: 1;
}

.error-title {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 700;
  color: #c0392b;
}

.error-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.82rem;
  color: #9c4b44;
}

.error-modal-body {
  padding: 1.25rem 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.error-list-panel {
  background-color: #fdf7f7;
  border: 1px solid #f6dede;
  border-radius: 8px;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 220px;
  overflow-y: auto;
}

.error-item-line {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  font-size: 0.88rem;
  color: #b03a2e;
  line-height: 1.4;
}

.error-bullet {
  font-size: 0.85rem;
  margin-top: 1px;
}

.error-text-content {
  font-weight: 600;
}

.error-hint-panel {
  background-color: #fdfbee;
  border: 1px solid #f5edcf;
  border-radius: 8px;
  padding: 0.65rem 0.9rem;
  font-size: 0.82rem;
  color: #8c7634;
  line-height: 1.4;
}

.error-modal-footer {
  padding: 0.9rem 1.4rem;
  border-top: 1px solid #f5efea;
  display: flex;
  justify-content: flex-end;
  background-color: #faf9f6;
}

.btn-error-close {
  background-color: #c0392b;
  color: #ffffff;
  padding: 0.55rem 1.25rem;
}

.btn-error-close:hover {
  background-color: #a93226;
}

@media (max-width: 992px) {
  .detail-content-layout {
    grid-template-columns: 1fr;
  }

  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
