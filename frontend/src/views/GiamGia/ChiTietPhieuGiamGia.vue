<template>
  <div class="voucher-detail-wrapper">
    <!-- 1. Thanh tiêu đề trên cùng & Nút quay lại -->
    <div class="page-top-header">
      <div class="top-title-wrap">
        <h2 class="main-page-title">
          Cập nhật phiếu giảm giá
          <span v-if="form" class="code-highlight">[{{ form.maPhieuGiamGia }}]</span>
        </h2>
      </div>
      <button class="btn-back-pill" @click="goBack" title="Quay lại danh sách phiếu giảm giá">
        ← Quay lại danh sách
      </button>
    </div>

    <!-- Trạng thái Đang nạp dữ liệu từ Backend -->
    <div v-if="loading" class="content-card status-msg-card">
      <div class="spinner">⏳</div>
      <span>Đang nạp dữ liệu chi tiết phiếu giảm giá từ SQL Server...</span>
    </div>

    <!-- Trạng thái Lỗi / Không tìm thấy -->
    <div v-else-if="errorMessage" class="content-card status-msg-card error-card">
      <div class="error-text">{{ errorMessage }}</div>
      <button class="btn btn-primary" @click="fetchVoucherDetail">Thử lại</button>
    </div>

    <!-- 2. Form nội dung: Bố cục dạng phẳng đồng bộ hoàn toàn với Tạo mới -->
    <form v-else-if="form" @submit.prevent="openConfirmModal" class="form-content-container">
      <!-- KHỐI 1: THÔNG TIN CHUNG (CARD 1) -->
      <div class="content-card form-section-card">
        <div class="card-section-header">
          <h3 class="section-title">Thông tin chung</h3>
        </div>

        <div class="section-body">
          <!-- Hàng 1 (2 Cột): Mã phiếu (Cố định) & Tên phiếu giảm giá -->
          <div class="form-grid-2">
            <!-- Mã phiếu giảm giá (Chỉ đọc) -->
            <div class="form-field">
              <label class="field-label">
                Mã phiếu giảm giá
                <span class="badge-lock">Cố định</span>
              </label>
              <input
                  type="text"
                  v-model="form.maPhieuGiamGia"
                  disabled
                  class="form-control input-disabled"
              />
            </div>

            <!-- Tên phiếu giảm giá -->
            <div class="form-field">
              <label class="field-label">Tên phiếu giảm giá <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="form.tenPhieuGiamGia"
                  placeholder="Nhập tên phiếu giảm giá..."
                  class="form-control"
                  :class="{ 'input-error': errors.tenPhieuGiamGia }"
              />
              <div v-if="errors.tenPhieuGiamGia" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.tenPhieuGiamGia }}
              </div>
            </div>
          </div>

          <!-- Hàng 2 (2 Cột): Số lượng & Đơn hàng tối thiểu -->
          <div class="form-grid-2">
            <!-- Cột trái: Số lượng -->
            <div class="form-field">
              <label class="field-label">Số lượng <span class="required">*</span></label>
              <input
                  type="number"
                  v-model.number="form.soLuongSuDung"
                  min="1"
                  placeholder="Nhập số lượng phiếu phát hành..."
                  class="form-control"
                  :class="{ 'input-error': errors.soLuongSuDung }"
              />
              <div v-if="errors.soLuongSuDung" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.soLuongSuDung }}
              </div>
            </div>

            <!-- Cột phải: Đơn hàng tối thiểu -->
            <div class="form-field">
              <label class="field-label">Đơn hàng tối thiểu <span class="required">*</span></label>
              <div class="input-with-addon">
                <input
                    type="text"
                    :value="displayMoney.hoaDonToiThieu"
                    placeholder="VD: 100.000"
                    @input="onMoneyInput('hoaDonToiThieu', $event)"
                    @blur="onMoneyBlur('hoaDonToiThieu')"
                    class="form-control"
                    :class="{ 'input-error': errors.hoaDonToiThieu }"
                />
                <span class="input-addon">đ</span>
              </div>
              <div v-if="errors.hoaDonToiThieu" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.hoaDonToiThieu }}
              </div>
            </div>
          </div>

          <!-- Hàng 3 (2 Cột): Loại giảm & Giá trị giảm -->
          <div class="form-grid-2">
            <!-- Cột trái: Loại giảm -->
            <div class="form-field">
              <label class="field-label">Loại giảm <span class="required">*</span></label>
              <select v-model.number="form.loaiPhieuGiamGia" class="form-control form-select">
                <option :value="1">Giảm theo %</option>
                <option :value="2">Giảm tiền mặt trực tiếp</option>
              </select>
            </div>

            <!-- Cột phải: Giá trị giảm -->
            <div class="form-field">
              <label class="field-label">
                Giá trị giảm <span class="required">*</span>
              </label>
              <div class="input-with-addon">
                <input
                    v-if="form.loaiPhieuGiamGia === 1"
                    type="number"
                    v-model.number="form.giaTriGiamGia"
                    min="1"
                    max="100"
                    placeholder="Nhập từ 1 - 100%"
                    class="form-control"
                    :class="{ 'input-error': errors.giaTriGiamGia }"
                />
                <input
                    v-else
                    type="text"
                    :value="displayMoney.giaTriGiamGia"
                    placeholder="VD: 20.000"
                    @input="onMoneyInput('giaTriGiamGia', $event)"
                    @blur="onMoneyBlur('giaTriGiamGia')"
                    class="form-control"
                    :class="{ 'input-error': errors.giaTriGiamGia }"
                />
                <span class="input-addon">{{ form.loaiPhieuGiamGia === 1 ? '%' : 'đ' }}</span>
              </div>
              <div v-if="errors.giaTriGiamGia" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.giaTriGiamGia }}
              </div>
            </div>
          </div>

          <!-- Hàng 4: Giảm tối đa (VNĐ) - Bắt buộc khi giảm theo % -->
          <div class="form-field full-width" v-if="form.loaiPhieuGiamGia === 1">
            <label class="field-label">
              Giảm tối đa (VNĐ) <span class="required">*</span>
            </label>
            <div class="input-with-addon">
              <input
                  type="text"
                  :value="displayMoney.giamToiDa"
                  placeholder="VD: 50.000"
                  @input="onMoneyInput('giamToiDa', $event)"
                  @blur="onMoneyBlur('giamToiDa')"
                  class="form-control"
                  :class="{ 'input-error': errors.giamToiDa }"
              />
              <span class="input-addon">đ</span>
            </div>
            <div v-if="errors.giamToiDa" class="field-error-msg">
              <span class="err-icon">ⓘ</span> {{ errors.giamToiDa }}
            </div>
          </div>

          <!-- Hàng 5 (2 Cột): Ngày bắt đầu & Ngày kết thúc -->
          <div class="form-grid-2">
            <!-- Ngày bắt đầu -->
            <div class="form-field">
              <label class="field-label">Ngày bắt đầu <span class="required">*</span></label>
              <input
                  type="datetime-local"
                  v-model="form.ngayBatDauStr"
                  class="form-control"
                  :class="{ 'input-error': errors.ngayBatDau }"
              />
              <div v-if="errors.ngayBatDau" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.ngayBatDau }}
              </div>
              <div v-else class="field-status-preview">
                Trạng thái dự kiến:
                <b :class="calculatedStatusMeta.class">{{ calculatedStatusMeta.text }}</b>
              </div>
            </div>

            <!-- Ngày kết thúc -->
            <div class="form-field">
              <label class="field-label">Ngày kết thúc <span class="required">*</span></label>
              <input
                  type="datetime-local"
                  v-model="form.ngayKetThucStr"
                  :min="form.ngayBatDauStr || ''"
                  class="form-control"
                  :class="{ 'input-error': errors.ngayKetThuc }"
              />
              <div v-if="errors.ngayKetThuc" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.ngayKetThuc }}
              </div>
            </div>
          </div>

          <!-- Hàng 6 (Full width): Mô tả phiếu giảm giá (Không bắt buộc) -->
          <div class="form-field full-width">
            <label class="field-label">
              Mô tả phiếu giảm giá <span class="field-hint-inline">(Không bắt buộc)</span>
            </label>
            <textarea
                v-model="form.moTa"
                rows="3"
                placeholder="Nhập ghi chú hoặc mô tả..."
                class="form-control form-textarea"
            ></textarea>
          </div>
        </div>
      </div>

      <!-- KHỐI 2: ĐỐI TƯỢNG ÁP DỤNG (CARD 2) -->
      <div class="content-card form-section-card">
        <div class="card-section-header">
          <h3 class="section-title">Đối tượng áp dụng</h3>
        </div>

        <div class="section-body">
          <div class="target-info-row">
            <span class="target-label">Hình thức phát hành:</span>
            <span :class="['badge-form', form.hinhThuc === 'Công khai' ? 'badge-public' : 'badge-personal']">
              ● {{ form.hinhThuc || 'Công khai' }}
            </span>
          </div>

          <!-- Danh sách khách hàng (Nếu là hình thức Cá nhân) -->
          <div v-if="form.hinhThuc === 'Cá nhân'" class="customers-assign-panel">
            <div class="customers-panel-header">
              <h4 class="customers-panel-title">Danh sách khách hàng nhận phiếu</h4>
              <div class="customers-count-badge">
                {{ (form.danhSachKhachHang || []).length }} khách hàng
              </div>
            </div>

            <div class="customers-tags-wrapper">
              <div
                  v-for="(kh, idx) in form.danhSachKhachHang"
                  :key="idx"
                  class="customer-tag-pill"
              >
                {{ kh }}
              </div>
              <div v-if="!form.danhSachKhachHang || form.danhSachKhachHang.length === 0" class="text-muted text-sm">
                Chưa có khách hàng liên kết cụ thể trong database.
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- CỤM NÚT THAO TÁC SUBMIT DƯỚI CÙNG -->
      <div class="form-submit-footer">
        <button type="button" class="btn btn-secondary" @click="goBack">
          Hủy bỏ
        </button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          <span v-if="saving" class="spin">🔄</span>
          {{ saving ? 'Đang cập nhật...' : 'Cập nhật phiếu giảm giá' }}
        </button>
      </div>
    </form>

    <!-- 3. POPUP MODAL XÁC NHẬN CẬP NHẬT DATABASE -->
    <div v-if="showConfirmModal" class="modal-overlay" @click.self="closeConfirmModal">
      <div class="modal-box confirm-modal-box">
        <div class="confirm-modal-header">
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">Xác nhận cập nhật phiếu giảm giá</h3>
          </div>
          <button class="modal-close-btn" @click="closeConfirmModal">✕</button>
        </div>

        <div class="confirm-modal-body">
          <p class="confirm-message-text">
            Bạn có chắc chắn muốn lưu các thay đổi cho phiếu giảm giá
            <b class="text-blue">[{{ form.maPhieuGiamGia }}]</b> không?
          </p>
        </div>

        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="closeConfirmModal" :disabled="saving">
            Hủy bỏ
          </button>
          <button class="btn btn-primary btn-save-confirm" @click="submitUpdate" :disabled="saving">
            <span v-if="saving" class="spin">🔄</span>
            {{ saving ? 'Đang cập nhật...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 4. THÔNG BÁO THÀNH CÔNG (TOAST NOTIFICATION) -->
    <div v-if="showSuccessToast" class="toast-success">
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
const showSuccessToast = ref(false)

const form = ref(null)
const errors = ref({})

// Giá trị hiển thị định dạng tiền tệ có dấu chấm phân cách hàng nghìn trực tiếp trong ô input
const displayMoney = ref({
  hoaDonToiThieu: '',
  giaTriGiamGia: '',
  giamToiDa: ''
})

const formatNumberString = (val) => {
  if (val == null || val === '') return ''
  const num = Number(val)
  if (isNaN(num)) return ''
  return num.toLocaleString('vi-VN')
}

// Formatters
const formatMoney = (val) => {
  if (val == null || val === '') return '0 đ'
  return `${Number(val).toLocaleString('vi-VN')} đ`
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

// Nạp chi tiết phiếu từ Backend
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
      moTa: '',
      trangThai: data.trangThai ?? 1,
      hinhThuc: data.hinhThuc || (data.soKhachHang > 0 ? 'Cá nhân' : 'Công khai'),
      soKhachHang: data.soKhachHang || 0,
      danhSachKhachHang: data.danhSachKhachHang || []
    }

    displayMoney.value = {
      hoaDonToiThieu: form.value.hoaDonToiThieu ? formatNumberString(form.value.hoaDonToiThieu) : '',
      giaTriGiamGia: (form.value.loaiPhieuGiamGia === 2 && form.value.giaTriGiamGia) ? formatNumberString(form.value.giaTriGiamGia) : '',
      giamToiDa: form.value.giamToiDa ? formatNumberString(form.value.giamToiDa) : ''
    }
  } catch (err) {
    console.error('Lỗi khi nạp chi tiết phiếu giảm giá:', err)
    errorMessage.value = 'Không thể kết nối đến Backend Spring Boot hoặc phiếu không tồn tại.'
  } finally {
    loading.value = false
  }
}

// Tự động tính toán trạng thái theo Ngày bắt đầu và Ngày kết thúc
const calculatedStatusMeta = computed(() => {
  if (!form.value || !form.value.ngayBatDauStr) {
    return { text: 'Không xác định', class: 'text-muted' }
  }

  const now = new Date()
  const endObj = form.value.ngayKetThucStr ? new Date(form.value.ngayKetThucStr) : null
  const startObj = form.value.ngayBatDauStr ? new Date(form.value.ngayBatDauStr) : null

  if (endObj && endObj < now) {
    return { text: 'Đã hết hạn', class: 'text-danger font-bold' }
  }
  if (startObj && startObj > now) {
    return { text: 'Sắp diễn ra', class: 'text-warning font-bold' }
  }
  return { text: 'Đang hoạt động', class: 'text-success font-bold' }
})

// Xử lý khi người dùng nhập vào các ô tiền tệ: chỉ cho phép số, format trực tiếp trong ô
const onMoneyInput = (field, event) => {
  if (!form.value) return
  const raw = event.target.value.replace(/\D/g, '')
  if (!raw) {
    form.value[field] = null
    displayMoney.value[field] = ''
    event.target.value = ''
  } else {
    const trimmed = raw.slice(0, 12)
    const num = parseInt(trimmed, 10)
    form.value[field] = num
    displayMoney.value[field] = num.toLocaleString('vi-VN')
    event.target.value = displayMoney.value[field]
  }
  if (errors.value[field]) {
    delete errors.value[field]
  }
}

// Khi blur ra ngoài: nếu số > 0 và < 1000 thì tự động nhân 1000 (VD: 15 -> 15.000, 700 -> 700.000)
const onMoneyBlur = (field) => {
  if (!form.value) return
  const val = form.value[field]
  if (val != null && val !== '') {
    let num = Number(val)
    if (!isNaN(num) && num > 0 && num < 1000) {
      num = Math.round(num * 1000)
      form.value[field] = num
      displayMoney.value[field] = num.toLocaleString('vi-VN')
    }
  }
}

const normalizeMoneyInputs = () => {
  if (!form.value) return
  onMoneyBlur('hoaDonToiThieu')
  if (form.value.loaiPhieuGiamGia === 2) {
    onMoneyBlur('giaTriGiamGia')
  }
  if (form.value.loaiPhieuGiamGia === 1) {
    onMoneyBlur('giamToiDa')
  }
}

watch(() => form.value?.loaiPhieuGiamGia, (newVal) => {
  if (form.value && newVal === 2) {
    form.value.giamToiDa = null
    displayMoney.value.giamToiDa = ''
    if (errors.value.giamToiDa) delete errors.value.giamToiDa
    if (form.value.giaTriGiamGia != null && form.value.giaTriGiamGia !== '') {
      onMoneyBlur('giaTriGiamGia')
    }
  } else if (form.value && newVal === 1) {
    if (form.value.giaTriGiamGia && form.value.giaTriGiamGia > 100) {
      form.value.giaTriGiamGia = null
    }
    displayMoney.value.giaTriGiamGia = ''
    if (form.value.giamToiDa != null && form.value.giamToiDa !== '') {
      onMoneyBlur('giamToiDa')
    }
  }
  if (errors.value.giaTriGiamGia) delete errors.value.giaTriGiamGia
})

// Tự động xóa chữ đỏ lỗi khi người dùng gõ nhập lại
watch(() => form.value?.tenPhieuGiamGia, (v) => { if (v && errors.value.tenPhieuGiamGia) delete errors.value.tenPhieuGiamGia })
watch(() => form.value?.soLuongSuDung, (v) => { if (v && errors.value.soLuongSuDung) delete errors.value.soLuongSuDung })
watch(() => form.value?.hoaDonToiThieu, (v) => { if (v && errors.value.hoaDonToiThieu) delete errors.value.hoaDonToiThieu })
watch(() => form.value?.giaTriGiamGia, (v) => { if (v && errors.value.giaTriGiamGia) delete errors.value.giaTriGiamGia })
watch(() => form.value?.giamToiDa, (v) => { if (v && errors.value.giamToiDa) delete errors.value.giamToiDa })
watch(() => form.value?.ngayBatDauStr, (v) => { if (v && errors.value.ngayBatDau) delete errors.value.ngayBatDau })
watch(() => form.value?.ngayKetThucStr, () => { if (errors.value.ngayKetThuc) delete errors.value.ngayKetThuc })

const validateForm = () => {
  if (!form.value) return false
  normalizeMoneyInputs()
  const errs = {}

  // 1. Tên phiếu giảm giá
  if (!form.value.tenPhieuGiamGia || !form.value.tenPhieuGiamGia.trim()) {
    errs.tenPhieuGiamGia = 'Vui lòng nhập tên phiếu giảm giá.'
  } else if (form.value.tenPhieuGiamGia.trim().length < 3) {
    errs.tenPhieuGiamGia = 'Tên phiếu giảm giá phải có tối thiểu 3 ký tự.'
  }

  // 2. Số lượng sử dụng (Bắt buộc tối thiểu 1)
  if (form.value.soLuongSuDung == null || form.value.soLuongSuDung === '') {
    errs.soLuongSuDung = 'Vui lòng nhập số lượng phiếu phát hành.'
  } else {
    const qty = Number(form.value.soLuongSuDung)
    if (isNaN(qty) || !Number.isInteger(qty) || qty <= 0) {
      errs.soLuongSuDung = 'Số lượng sử dụng phải là số nguyên dương lớn hơn 0.'
    }
  }

  // 3. Đơn hàng tối thiểu (>= 1.000 đ)
  if (form.value.hoaDonToiThieu == null || form.value.hoaDonToiThieu === '') {
    errs.hoaDonToiThieu = 'Vui lòng nhập giá trị đơn hàng tối thiểu.'
  } else {
    const minBill = Number(form.value.hoaDonToiThieu)
    if (isNaN(minBill) || minBill < 1000) {
      errs.hoaDonToiThieu = 'Đơn hàng tối thiểu phải từ 1.000 đ trở lên.'
    }
  }

  // 4. Mức giảm giá
  if (form.value.giaTriGiamGia == null || form.value.giaTriGiamGia === '') {
    errs.giaTriGiamGia = 'Vui lòng nhập mức giảm giá.'
  } else {
    const val = Number(form.value.giaTriGiamGia)
    if (isNaN(val) || val <= 0) {
      errs.giaTriGiamGia = 'Mức giảm giá phải lớn hơn 0.'
    } else if (form.value.loaiPhieuGiamGia === 1) {
      if (val < 1 || val > 100) {
        errs.giaTriGiamGia = 'Mức giảm (%) phải nằm trong khoảng từ 1% đến 100%.'
      }
    } else if (form.value.loaiPhieuGiamGia === 2) {
      if (val < 1000) {
        errs.giaTriGiamGia = 'Mức giảm tiền mặt trực tiếp phải từ 1.000 đ trở lên.'
      } else if (form.value.hoaDonToiThieu && val > Number(form.value.hoaDonToiThieu)) {
        errs.giaTriGiamGia = 'Mức giảm tiền mặt không được lớn hơn đơn hàng tối thiểu.'
      }
    }
  }

  // 5. Giảm tối đa (Bắt buộc khi giảm theo %)
  if (form.value.loaiPhieuGiamGia === 1) {
    if (form.value.giamToiDa == null || form.value.giamToiDa === '') {
      errs.giamToiDa = 'Vui lòng nhập mức giảm tối đa.'
    } else {
      const maxVal = Number(form.value.giamToiDa)
      if (isNaN(maxVal) || maxVal < 1000) {
        errs.giamToiDa = 'Mức giảm tối đa phải từ 1.000 đ trở lên.'
      }
    }
  }

  // 6. Thời gian bắt đầu
  if (!form.value.ngayBatDauStr) {
    errs.ngayBatDau = 'Vui lòng chọn thời gian bắt đầu.'
  }

  // 7. Thời gian kết thúc (Bắt buộc và phải sau thời gian bắt đầu)
  if (!form.value.ngayKetThucStr) {
    errs.ngayKetThuc = 'Vui lòng chọn thời gian kết thúc.'
  } else if (form.value.ngayBatDauStr) {
    const startDate = new Date(form.value.ngayBatDauStr)
    const endDate = new Date(form.value.ngayKetThucStr)
    if (endDate <= startDate) {
      errs.ngayKetThuc = 'Thời gian kết thúc phải sau thời gian bắt đầu.'
    }
  }

  errors.value = errs
  return Object.keys(errs).length === 0
}

const openConfirmModal = () => {
  // Chỉ kiểm tra validate: nếu có lỗi thì hiện chữ đỏ ở dưới ô input và cuộn tới ô đó, không hiện popup modal lỗi
  if (!validateForm()) {
    setTimeout(() => {
      const firstErrEl = document.querySelector('.input-error')
      if (firstErrEl) {
        firstErrEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
        firstErrEl.focus()
      }
    }, 50)
    return
  }
  // Nếu hợp lệ toàn bộ thì hiển thị Popup modal xác nhận cập nhật
  showConfirmModal.value = true
}

const closeConfirmModal = () => {
  showConfirmModal.value = false
}

const submitUpdate = async () => {
  if (saving.value || !form.value) return
  saving.value = true

  const payload = {
    id: form.value.id,
    maPhieuGiamGia: form.value.maPhieuGiamGia,
    tenPhieuGiamGia: form.value.tenPhieuGiamGia.trim(),
    loaiPhieuGiamGia: form.value.loaiPhieuGiamGia,
    giaTriGiamGia: Number(form.value.giaTriGiamGia),
    giamToiDa: form.value.loaiPhieuGiamGia === 1 && form.value.giamToiDa ? Number(form.value.giamToiDa) : null,
    hoaDonToiThieu: form.value.hoaDonToiThieu != null ? Number(form.value.hoaDonToiThieu) : 0,
    soLuongSuDung: Number(form.value.soLuongSuDung),
    ngayBatDau: dateTimeInputToIso(form.value.ngayBatDauStr),
    ngayKetThuc: dateTimeInputToIso(form.value.ngayKetThucStr),
    trangThai: 1 // Luôn duy trì kích hoạt, backend và frontend tự tính hiển thị
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
      }
    }
    alert(msg)
    showConfirmModal.value = false
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
/* Khung tổng thể bao ngoài căn lề chuẩn hệ thống */
.voucher-detail-wrapper {
  padding: 1.5rem 2.5rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header trên cùng */
.page-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.main-page-title {
  font-size: 1.45rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.code-highlight {
  color: #2563eb;
  font-size: 1.25rem;
}

.btn-back-pill {
  padding: 0.55rem 1.25rem;
  border-radius: 20px;
  border: 1px solid #d2d6dc;
  background-color: #ffffff;
  color: #4b5563;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-back-pill:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
  color: #111827;
}

/* Status message */
.status-msg-card {
  text-align: center;
  padding: 3rem !important;
  color: #647074;
}

.spinner {
  font-size: 1.8rem;
  margin-bottom: 0.5rem;
}

.error-card {
  border-color: #fca5a5 !important;
  background-color: #fffafb !important;
}

.error-text {
  color: #dc2626;
  font-weight: 600;
  margin-bottom: 1rem;
}

/* 2. Form bố cục phẳng 1 cột các Card xếp dọc */
.form-content-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Thẻ Card trắng chuẩn */
.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.85rem;
}

.card-section-header {
  border-bottom: 1px solid #f0eee6;
  padding-bottom: 0.85rem;
  margin-bottom: 1.25rem;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

/* Grid 2 cột */
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.full-width {
  width: 100%;
}

.field-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.badge-lock {
  font-size: 0.72rem;
  font-weight: 700;
  color: #6b7280;
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.field-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.required {
  color: #dc2626;
  margin-left: 2px;
}

.field-hint-inline {
  font-size: 0.8rem;
  font-weight: 400;
  color: #6b7280;
  margin-left: 0.35rem;
}

/* Controls input */
.form-control {
  width: 100%;
  padding: 0.65rem 0.95rem;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  font-size: 0.92rem;
  color: #1f2937;
  background-color: #ffffff;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.form-control:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.input-disabled {
  background-color: #f9fafb !important;
  color: #6b7280 !important;
  cursor: not-allowed;
  border-color: #e5e7eb !important;
}

.form-select {
  cursor: pointer;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.input-with-addon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-addon .form-control {
  padding-right: 2.5rem;
}

.input-addon {
  position: absolute;
  right: 0.9rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: #6b7280;
  pointer-events: none;
}

.input-error {
  border-color: #ef4444 !important;
  background-color: #fffafb;
}

/* Báo lỗi chuẩn dưới ô input dạng ⓘ text đỏ */
.field-error-msg {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #dc2626;
  font-size: 0.82rem;
  font-weight: 500;
  margin-top: 0.2rem;
}

.err-icon {
  font-size: 0.88rem;
  font-style: normal;
}

.field-hint-text {
  font-size: 0.82rem;
  color: #496883;
  font-weight: 500;
  margin-top: 0.15rem;
}

.field-status-preview {
  font-size: 0.82rem;
  color: #6b7280;
  margin-top: 0.15rem;
}

.btn-clear-date {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.btn-clear-date:hover {
  text-decoration: underline;
}

/* Khối đối tượng áp dụng */
.target-info-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.95rem;
}

.target-label {
  font-weight: 600;
  color: #374151;
}

.badge-form {
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0.3rem 0.85rem;
  border-radius: 12px;
}

.badge-public {
  background-color: #fef3c7;
  color: #b45309;
}

.badge-personal {
  background-color: #e0f2fe;
  color: #0369a1;
}

.customers-assign-panel {
  margin-top: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.25rem;
  background-color: #fdfdfd;
}

.customers-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.85rem;
}

.customers-panel-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.customers-count-badge {
  background-color: #4b5563;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
}

.customers-tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.customer-tag-pill {
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 0.35rem 0.9rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: #374151;
}

/* Submit bar dưới cùng */
.form-submit-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 0.5rem 0 1.5rem;
}

.btn {
  padding: 0.65rem 1.6rem;
  border-radius: 7px;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.btn-secondary {
  background-color: #ffffff;
  border: 1px solid #d1d5db;
  color: #4b5563;
}

.btn-secondary:hover {
  background-color: #f3f4f6;
  color: #111827;
}

.btn-primary {
  background-color: #496883;
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background-color: #3b546b;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Modals */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(30, 41, 59, 0.55);
  backdrop-filter: blur(2px);
  z-index: 999;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.25rem;
}

.modal-box {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  animation: modalScaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalScaleIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.confirm-modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.confirm-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #111827;
}

.confirm-subtitle {
  margin: 4px 0 0;
  font-size: 0.85rem;
  color: #6b7280;
}

.modal-close-btn {
  background: none;
  border: none;
  font-size: 1.1rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
}

.modal-close-btn:hover {
  color: #374151;
}

.confirm-modal-body {
  padding: 1.25rem 1.5rem;
}

.confirm-modal-box {
  max-width: 460px;
}

.confirm-message-text {
  margin: 0.25rem 0;
  font-size: 0.95rem;
  line-height: 1.5;
  color: #374151;
}

.confirm-summary-panel {
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0.85rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.88rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.s-label {
  color: #6b7280;
}

.s-val {
  color: #111827;
}

.confirm-note-box {
  margin-top: 1rem;
  padding: 0.75rem 0.95rem;
  border-radius: 6px;
  font-size: 0.82rem;
  line-height: 1.45;
}

.note-info {
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e40af;
}

.confirm-modal-footer {
  padding: 1rem 1.5rem;
  background-color: #f9fafb;
  border-top: 1px solid #f3f4f6;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

/* Toast */
.toast-success {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  background-color: #ffffff;
  border-left: 4px solid #10b981;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  z-index: 1000;
}

.toast-text b {
  color: #065f46;
  display: block;
  font-size: 0.95rem;
}

.toast-text p {
  margin: 2px 0 0;
  color: #4b5563;
  font-size: 0.85rem;
}

.toast-close {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  font-size: 1rem;
}

/* Typography Helpers */
.text-blue { color: #2563eb; }
.text-danger { color: #dc2626; }
.text-success { color: #16a34a; }
.text-warning { color: #d97706; }
.text-highlight { color: #b45309; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 600; }

.spin {
  display: inline-block;
  animation: spin 1s infinite linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .voucher-detail-wrapper {
    padding: 1rem 1.25rem 2rem;
  }
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
