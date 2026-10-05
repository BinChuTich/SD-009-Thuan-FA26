<template>
  <div class="voucher-create-wrapper">
    <!-- 1. Thanh tiêu đề Breadcrumb & Nút hành động -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <button class="btn-back" @click="goBack" title="Quay lại danh sách">←</button>
        <span class="breadcrumb-text">
          Quản lý giảm giá <span class="slash">/</span>
          <router-link to="/phieu-giam-gia" class="breadcrumb-link">Phiếu giảm giá</router-link>
          <span class="slash">/</span>
          <b>Tạo phiếu mới</b>
        </span>
      </div>

      <!-- Cụm nút hành động phía trên bên phải -->
      <div class="header-actions">
        <button class="btn btn-secondary" @click="goBack">
          <span>✕</span> Hủy bỏ
        </button>
        <button class="btn btn-primary" @click="openConfirmModal" :disabled="saving">
          <span v-if="saving" class="spin">🔄</span>
          <span v-else>+</span>
          Tạo phiếu mới
        </button>
      </div>
    </div>

    <!-- 2. Form tạo phiếu giảm giá (Layout 2 cột đồng bộ với Chi tiết) -->
    <div class="create-content-layout">
      <!-- Cột trái: Card xem trước trực tiếp & Khách hàng áp dụng -->
      <div class="left-summary-column">
        <!-- Card Xem trước phiếu giảm giá (Live Preview) -->
        <div class="content-card summary-card">
          <div class="card-icon-header">
            <div class="icon-avatar">🎟️</div>
            <div class="card-title-group">
              <span class="voucher-code-badge">{{ form.maPhieuGiamGia || 'MÃ TỰ ĐỘNG' }}</span>
              <h3 class="voucher-name-preview">{{ form.tenPhieuGiamGia || 'Chưa đặt tên phiếu' }}</h3>
            </div>
          </div>

          <div class="divider"></div>

          <div class="summary-info-list">
            <div class="info-row">
              <span class="info-label">Hình thức:</span>
              <span :class="['badge-form', form.hinhThuc === 'Công khai' ? 'badge-public' : 'badge-personal']">
                ● {{ form.hinhThuc }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Trạng thái:</span>
              <span :class="['badge-status', form.trangThai === 1 ? 'status-active' : 'status-inactive']">
                {{ form.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Mức giảm:</span>
              <span class="info-value font-bold text-highlight">
                {{ form.loaiPhieuGiamGia === 1 ? (form.giaTriGiamGia || 0) + '%' : formatMoney(form.giaTriGiamGia) }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Đơn tối thiểu:</span>
              <span class="info-value font-medium">{{ formatMoney(form.hoaDonToiThieu) }}</span>
            </div>

            <div class="info-row">
              <span class="info-label">Giảm tối đa:</span>
              <span class="info-value font-medium">
                {{ form.loaiPhieuGiamGia === 1 ? (form.giamToiDa ? formatMoney(form.giamToiDa) : 'Không giới hạn') : 'Không áp dụng' }}
              </span>
            </div>

            <div class="info-row">
              <span class="info-label">Số lượt sử dụng:</span>
              <span class="info-value font-medium">{{ form.soLuongSuDung || 0 }} lượt</span>
            </div>

            <div class="info-row">
              <span class="info-label">Thời gian:</span>
              <span class="info-value text-xs text-muted-dark">
                {{ form.ngayBatDauStr ? formatDateDisplay(form.ngayBatDauStr) : '---' }} ➔ {{ form.ngayKetThucStr ? formatDateDisplay(form.ngayKetThucStr) : '---' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Card Chọn khách hàng áp dụng (Khi chọn hình thức Cá nhân) -->
        <div v-if="form.hinhThuc === 'Cá nhân'" class="content-card customers-card">
          <div class="section-card-header">
            <div class="section-icon-box gold-soft-bg">
              <span>👥</span>
            </div>
            <div>
              <h4 class="section-title">Khách hàng áp dụng</h4>
              <p class="section-subtitle">Đã chọn {{ selectedCustomerIds.length }} khách hàng</p>
            </div>
          </div>

          <!-- Tìm kiếm khách hàng -->
          <div class="cust-search-box">
            <input
                type="text"
                v-model="custSearchKeyword"
                placeholder="Tìm theo tên, SĐT, mã KH..."
                class="cust-search-input"
            />
          </div>

          <div class="cust-actions-bar">
            <button type="button" class="btn-text-action" @click="selectAllCustomers">Chọn tất cả</button>
            <span class="sep">|</span>
            <button type="button" class="btn-text-action" @click="clearSelectedCustomers">Bỏ chọn hết</button>
          </div>

          <!-- Danh sách khách hàng chọn checkbox -->
          <div class="customers-select-box">
            <div
                v-for="kh in filteredCustomerList"
                :key="kh.id"
                class="customer-select-item"
                :class="{ selected: selectedCustomerIds.includes(kh.id) }"
                @click="toggleCustomerSelect(kh.id)"
            >
              <input
                  type="checkbox"
                  :checked="selectedCustomerIds.includes(kh.id)"
                  @click.stop="toggleCustomerSelect(kh.id)"
              />
              <div class="cust-info">
                <span class="cust-name-text">{{ kh.tenKhachHang }}</span>
                <span class="cust-sub-text">{{ kh.soDienThoai || kh.email || ('Mã: ' + kh.maKhachHang) }}</span>
              </div>
            </div>

            <div v-if="filteredCustomerList.length === 0" class="text-muted text-sm text-center py-2">
              Không tìm thấy khách hàng nào.
            </div>
          </div>

          <span v-if="errors.customers" class="error-msg mt-2">{{ errors.customers }}</span>
        </div>
      </div>

      <!-- Cột phải: Các khối form nhập liệu tất cả trường trong database -->
      <div class="right-form-column">
        <!-- Khối 1: Thông tin cơ bản -->
        <div class="content-card form-section-card">
          <div class="section-card-header">
            <div class="section-icon-box blue-soft-bg">
              <span>📝</span>
            </div>
            <div>
              <h4 class="section-title">Thông tin cơ bản</h4>
              <p class="section-subtitle">Mã phiếu, tên chương trình và hình thức phát hành</p>
            </div>
          </div>

          <div class="form-grid-2">
            <!-- Mã phiếu giảm giá -->
            <div class="form-field">
              <label>
                Mã phiếu giảm giá
                <span class="field-hint-inline">(Tự nhập hoặc sinh tự động)</span>
              </label>
              <div class="input-with-button">
                <input
                    type="text"
                    v-model="form.maPhieuGiamGia"
                    placeholder="VD: PGG_SUMMER2026..."
                    @input="form.maPhieuGiamGia = form.maPhieuGiamGia.toUpperCase()"
                    :class="{ 'input-error': errors.maPhieuGiamGia }"
                />
                <button type="button" class="btn-gen-code" @click="generateRandomCode" title="Tạo mã ngẫu nhiên">
                  🎲 Tạo mã
                </button>
              </div>
              <span v-if="errors.maPhieuGiamGia" class="error-msg">{{ errors.maPhieuGiamGia }}</span>
              <span class="field-hint">Mã định danh duy nhất trong cơ sở dữ liệu.</span>
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

            <!-- Hình thức phát hành (Công khai / Cá nhân) -->
            <div class="form-field">
              <label>Hình thức phiếu <span class="required">*</span></label>
              <select v-model="form.hinhThuc">
                <option value="Công khai">Công khai (Tất cả khách hàng)</option>
                <option value="Cá nhân">Cá nhân (Gán khách hàng cụ thể)</option>
              </select>
            </div>

            <!-- Trạng thái hoạt động ban đầu -->
            <div class="form-field">
              <label>Trạng thái khởi tạo <span class="required">*</span></label>
              <select v-model.number="form.trangThai">
                <option :value="1">Đang hoạt động (Kích hoạt ngay)</option>
                <option :value="0">Ngừng hoạt động (Tạm tắt)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Khối 2: Cấu hình giá trị và điều kiện giảm giá -->
        <div class="content-card form-section-card">
          <div class="section-card-header">
            <div class="section-icon-box gold-soft-bg">
              <span>💰</span>
            </div>
            <div>
              <h4 class="section-title">Giá trị & Điều kiện áp dụng</h4>
              <p class="section-subtitle">Loại giảm giá, mức giảm, giá trị đơn tối thiểu và số lượt dùng</p>
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

            <!-- Mức giảm -->
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
                    min="0"
                    :max="form.loaiPhieuGiamGia === 1 ? 100 : 999999999"
                    placeholder="Nhập mức giảm..."
                    :class="{ 'input-error': errors.giaTriGiamGia }"
                />
                <span class="input-addon">{{ form.loaiPhieuGiamGia === 1 ? '%' : 'đ' }}</span>
              </div>
              <span v-if="errors.giaTriGiamGia" class="error-msg">{{ errors.giaTriGiamGia }}</span>
            </div>

            <!-- Giảm tối đa (chỉ áp dụng khi giảm theo %) -->
            <div class="form-field">
              <label>
                Giảm tối đa (VNĐ)
                <span v-if="form.loaiPhieuGiamGia === 2" class="field-optional">(Không cần với giảm tiền mặt)</span>
              </label>
              <div class="input-with-addon">
                <input
                    type="number"
                    v-model.number="form.giamToiDa"
                    min="0"
                    :disabled="form.loaiPhieuGiamGia === 2"
                    :placeholder="form.loaiPhieuGiamGia === 2 ? 'Không áp dụng' : 'VD: 50000 (để trống nếu không giới hạn)'"
                />
                <span class="input-addon">đ</span>
              </div>
            </div>

            <!-- Đơn hàng tối thiểu -->
            <div class="form-field">
              <label>Hóa đơn tối thiểu (VNĐ)</label>
              <div class="input-with-addon">
                <input
                    type="number"
                    v-model.number="form.hoaDonToiThieu"
                    min="0"
                    placeholder="VD: 200000 (đơn hàng phải đạt mức này)"
                    :class="{ 'input-error': errors.hoaDonToiThieu }"
                />
                <span class="input-addon">đ</span>
              </div>
              <span v-if="errors.hoaDonToiThieu" class="error-msg">{{ errors.hoaDonToiThieu }}</span>
            </div>

            <!-- Số lượng sử dụng -->
            <div class="form-field full-width">
              <label>Số lượt sử dụng <span class="required">*</span></label>
              <input
                  type="number"
                  v-model.number="form.soLuongSuDung"
                  min="1"
                  placeholder="Nhập số lượt có thể áp dụng..."
                  :class="{ 'input-error': errors.soLuongSuDung }"
              />
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
            <!-- Ngày bắt đầu -->
            <div class="form-field">
              <label>Ngày bắt đầu <span class="required">*</span></label>
              <input
                  type="date"
                  v-model="form.ngayBatDauStr"
                  :class="{ 'input-error': errors.ngayBatDau }"
              />
              <span v-if="errors.ngayBatDau" class="error-msg">{{ errors.ngayBatDau }}</span>
            </div>

            <!-- Ngày kết thúc -->
            <div class="form-field">
              <label>Ngày kết thúc <span class="required">*</span></label>
              <input
                  type="date"
                  v-model="form.ngayKetThucStr"
                  :class="{ 'input-error': errors.ngayKetThuc }"
              />
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
            <span v-else>+</span>
            Tạo phiếu giảm giá
          </button>
        </div>
      </div>
    </div>

    <!-- 3. POPUP MODAL XÁC NHẬN THÊM PHIẾU MỚI (CHUẨN ĐẸP NHƯ BÊN CẬP NHẬT) -->
    <div v-if="showConfirmModal" class="modal-overlay" @click.self="closeConfirmModal">
      <div class="modal-box confirm-modal-box">
        <div class="confirm-modal-header">
          <div class="confirm-warning-icon">🎟️</div>
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">Xác nhận tạo phiếu giảm giá</h3>
            <p class="confirm-subtitle">Kiểm tra thông tin trước khi thêm vào cơ sở dữ liệu</p>
          </div>
          <button class="modal-close-btn" @click="closeConfirmModal">✕</button>
        </div>

        <div class="confirm-modal-body">
          <p class="confirm-message-text">
            Bạn có chắc chắn muốn tạo phiếu giảm giá mới
            <b class="text-blue">[{{ form.maPhieuGiamGia || 'MÃ TỰ SINH' }}]</b> không?
          </p>

          <!-- Bảng tóm tắt các thông số sắp tạo -->
          <div class="confirm-summary-panel">
            <div class="summary-line">
              <span class="s-label">Mã phiếu:</span>
              <span class="s-val font-bold text-blue">{{ form.maPhieuGiamGia || '(Hệ thống tự động sinh)' }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Tên phiếu:</span>
              <span class="s-val font-medium">{{ form.tenPhieuGiamGia }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Hình thức:</span>
              <span class="s-val font-medium">
                {{ form.hinhThuc }}
                <span v-if="form.hinhThuc === 'Cá nhân'" class="text-xs text-muted">
                  ({{ selectedCustomerIds.length }} khách hàng)
                </span>
              </span>
            </div>
            <div class="summary-line">
              <span class="s-label">Mức giảm:</span>
              <span class="s-val font-bold text-highlight">
                {{ form.loaiPhieuGiamGia === 1 ? form.giaTriGiamGia + '%' : formatMoney(form.giaTriGiamGia) }}
              </span>
            </div>
            <div class="summary-line">
              <span class="s-label">Đơn tối thiểu:</span>
              <span class="s-val">{{ formatMoney(form.hoaDonToiThieu) }}</span>
            </div>
            <div class="summary-line" v-if="form.loaiPhieuGiamGia === 1 && form.giamToiDa">
              <span class="s-label">Giảm tối đa:</span>
              <span class="s-val">{{ formatMoney(form.giamToiDa) }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Lượt sử dụng:</span>
              <span class="s-val">{{ form.soLuongSuDung }} lượt</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Thời hạn:</span>
              <span class="s-val">{{ formatDateDisplay(form.ngayBatDauStr) }} ➔ {{ formatDateDisplay(form.ngayKetThucStr) }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Trạng thái:</span>
              <span class="s-val font-medium">{{ form.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}</span>
            </div>
          </div>

          <div class="confirm-note-box note-info">
            💡 <b>Lưu ý:</b> Phiếu giảm giá sau khi tạo sẽ được lưu vào cơ sở dữ liệu và sẵn sàng áp dụng ngay nếu ở trạng thái hoạt động.
          </div>
        </div>

        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="closeConfirmModal" :disabled="saving">
            Hủy bỏ
          </button>
          <button class="btn btn-primary btn-save-confirm" @click="submitCreate" :disabled="saving">
            <span v-if="saving" class="spin">🔄</span>
            <span v-else>✔</span>
            {{ saving ? 'Đang lưu vào DB...' : 'Xác nhận tạo' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 4. THÔNG BÁO THÀNH CÔNG (TOAST NOTIFICATION) -->
    <div v-if="showSuccessToast" class="toast-success">
      <div class="toast-icon">✅</div>
      <div class="toast-text">
        <b>Thành công!</b>
        <p>Đã tạo phiếu giảm giá mới thành công vào cơ sở dữ liệu.</p>
      </div>
      <button class="toast-close" @click="showSuccessToast = false">✕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../api'

const router = useRouter()

const saving = ref(false)
const showConfirmModal = ref(false)
const showSuccessToast = ref(false)

const customerList = ref([])
const custSearchKeyword = ref('')
const selectedCustomerIds = ref([])

const getTodayString = () => {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getDefaultEndString = () => {
  const d = new Date()
  d.setDate(d.getDate() + 30) // Mặc định thời hạn 30 ngày
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const form = ref({
  maPhieuGiamGia: '',
  tenPhieuGiamGia: '',
  hinhThuc: 'Công khai',
  loaiPhieuGiamGia: 1, // 1: %, 2: Tiền mặt
  giaTriGiamGia: 10,
  giamToiDa: null,
  hoaDonToiThieu: 0,
  soLuongSuDung: 100,
  ngayBatDauStr: getTodayString(),
  ngayKetThucStr: getDefaultEndString(),
  trangThai: 1
})

const errors = ref({})

const generateRandomCode = () => {
  const randomSuffix = Math.floor(100000 + Math.random() * 900000)
  form.value.maPhieuGiamGia = `PGG${randomSuffix}`
  if (errors.value.maPhieuGiamGia) delete errors.value.maPhieuGiamGia
}

const fetchCustomers = async () => {
  try {
    const res = await api.get('/api/phieu-giam-gia/khach-hang')
    customerList.value = res.data || []
  } catch (err) {
    console.error('Không thể nạp danh sách khách hàng:', err)
  }
}

onMounted(() => {
  generateRandomCode()
  fetchCustomers()
})

const filteredCustomerList = computed(() => {
  if (!custSearchKeyword.value.trim()) return customerList.value
  const kw = custSearchKeyword.value.toLowerCase().trim()
  return customerList.value.filter(kh => {
    return (kh.tenKhachHang && kh.tenKhachHang.toLowerCase().includes(kw)) ||
        (kh.soDienThoai && kh.soDienThoai.toLowerCase().includes(kw)) ||
        (kh.maKhachHang && kh.maKhachHang.toLowerCase().includes(kw))
  })
})

const toggleCustomerSelect = (id) => {
  const idx = selectedCustomerIds.value.indexOf(id)
  if (idx > -1) {
    selectedCustomerIds.value.splice(idx, 1)
  } else {
    selectedCustomerIds.value.push(id)
  }
}

const selectAllCustomers = () => {
  selectedCustomerIds.value = filteredCustomerList.value.map(k => k.id)
}

const clearSelectedCustomers = () => {
  selectedCustomerIds.value = []
}

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

const dateInputToIso = (dateStr, isEndOfDay = false) => {
  if (!dateStr) return null
  try {
    const time = isEndOfDay ? 'T23:59:59Z' : 'T00:00:00Z'
    return new Date(dateStr + time).toISOString()
  } catch (e) {
    return null
  }
}

const validateForm = () => {
  errors.value = {}

  if (!form.value.tenPhieuGiamGia || !form.value.tenPhieuGiamGia.trim()) {
    errors.value.tenPhieuGiamGia = 'Vui lòng nhập tên phiếu giảm giá!'
  }

  if (form.value.giaTriGiamGia == null || form.value.giaTriGiamGia === '') {
    errors.value.giaTriGiamGia = 'Vui lòng nhập giá trị giảm giá!'
  } else if (Number(form.value.giaTriGiamGia) <= 0) {
    errors.value.giaTriGiamGia = 'Giá trị giảm giá phải lớn hơn 0!'
  } else if (form.value.loaiPhieuGiamGia === 1 && Number(form.value.giaTriGiamGia) > 100) {
    errors.value.giaTriGiamGia = 'Giảm theo % không được vượt quá 100%!'
  }

  if (form.value.hoaDonToiThieu != null && Number(form.value.hoaDonToiThieu) < 0) {
    errors.value.hoaDonToiThieu = 'Hóa đơn tối thiểu không được âm!'
  }

  if (form.value.soLuongSuDung == null || Number(form.value.soLuongSuDung) <= 0) {
    errors.value.soLuongSuDung = 'Số lượt sử dụng phải lớn hơn 0!'
  }

  if (!form.value.ngayBatDauStr) {
    errors.value.ngayBatDau = 'Vui lòng chọn ngày bắt đầu!'
  }

  if (!form.value.ngayKetThucStr) {
    errors.value.ngayKetThuc = 'Vui lòng chọn ngày kết thúc!'
  } else if (form.value.ngayBatDauStr && form.value.ngayKetThucStr < form.value.ngayBatDauStr) {
    errors.value.ngayKetThuc = 'Ngày kết thúc phải diễn ra sau ngày bắt đầu!'
  }

  if (form.value.hinhThuc === 'Cá nhân' && selectedCustomerIds.value.length === 0) {
    errors.value.customers = 'Hình thức Cá nhân yêu cầu chọn ít nhất 1 khách hàng áp dụng!'
  }

  return Object.keys(errors.value).length === 0
}

const openConfirmModal = () => {
  if (validateForm()) {
    showConfirmModal.value = true
  }
}

const closeConfirmModal = () => {
  if (saving.value) return
  showConfirmModal.value = false
}

const submitCreate = async () => {
  saving.value = true
  try {
    const payload = {
      maPhieuGiamGia: form.value.maPhieuGiamGia ? form.value.maPhieuGiamGia.trim() : null,
      tenPhieuGiamGia: form.value.tenPhieuGiamGia.trim(),
      hinhThuc: form.value.hinhThuc,
      loaiPhieuGiamGia: Number(form.value.loaiPhieuGiamGia),
      giaTriGiamGia: Number(form.value.giaTriGiamGia),
      giamToiDa: form.value.loaiPhieuGiamGia === 1 && form.value.giamToiDa ? Number(form.value.giamToiDa) : null,
      hoaDonToiThieu: form.value.hoaDonToiThieu ? Number(form.value.hoaDonToiThieu) : 0,
      soLuongSuDung: Number(form.value.soLuongSuDung),
      ngayBatDau: dateInputToIso(form.value.ngayBatDauStr, false),
      ngayKetThuc: dateInputToIso(form.value.ngayKetThucStr, true),
      trangThai: Number(form.value.trangThai),
      idKhachHangList: form.value.hinhThuc === 'Cá nhân' ? selectedCustomerIds.value : []
    }

    await api.post('/api/phieu-giam-gia', payload)

    showConfirmModal.value = false
    showSuccessToast.value = true

    setTimeout(() => {
      showSuccessToast.value = false
      router.push('/phieu-giam-gia')
    }, 1500)
  } catch (err) {
    console.error('Lỗi khi tạo mới phiếu giảm giá:', err)
    let msg = 'Có lỗi xảy ra khi tạo phiếu giảm giá!'
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
    alert('Thất bại: ' + msg)
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.push('/phieu-giam-gia')
}
</script>

<style scoped>
.voucher-create-wrapper {
  padding: 1.25rem 1.75rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: var(--system-font, sans-serif);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header Breadcrumb & Nút quay lại */
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
  gap: 0.65rem;
  font-size: 0.95rem;
}

.btn-back {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: #496883;
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 1.1rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #eaf1f4;
  border-color: #496883;
  transform: translateX(-2px);
}

.breadcrumb-text {
  color: #8c9597;
}

.breadcrumb-link {
  color: #8c9597;
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: #496883;
}

.breadcrumb-text b {
  color: var(--blue, #496883);
  font-weight: 700;
}

.slash {
  margin: 0 4px;
  color: #d8d4c9;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* Các loại nút bấm */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 1.15rem;
  border-radius: 7px;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  background-color: #ffffff;
  border: 1px solid #dfd9cb;
  color: #55646b;
}

.btn-secondary:hover {
  background-color: #f7f5ef;
  border-color: #c9c0ae;
}

.btn-primary {
  background-color: var(--blue, #496883);
  color: #ffffff;
}

.btn-primary:hover:not(:disabled) {
  background-color: #38536b;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 2. Layout Form 2 cột */
.create-content-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 1.4rem;
  align-items: start;
}

.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem;
  margin-bottom: 1.25rem;
}

/* Cột trái: Card Tổng quan & Khách hàng */
.left-summary-column {
  display: flex;
  flex-direction: column;
}

.summary-card {
  border-top: 4px solid var(--blue, #496883);
}

.card-icon-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.icon-avatar {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background-color: #eaf1f5;
  display: grid;
  place-items: center;
  font-size: 1.35rem;
}

.card-title-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  overflow: hidden;
}

.voucher-code-badge {
  font-family: monospace, sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  color: #496883;
  letter-spacing: 0.5px;
}

.voucher-name-preview {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2b383e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.divider {
  height: 1px;
  background-color: #f2eee6;
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
  font-size: 0.9rem;
}

.info-label {
  color: #7b888e;
  font-weight: 500;
}

.info-value {
  color: #2b383e;
}

.text-highlight {
  color: #c0392b;
  font-size: 1.05rem;
}

.text-xs {
  font-size: 0.78rem;
}

/* Card chọn khách hàng */
.customers-card {
  border-top: 4px solid #b38536;
}

.section-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.1rem;
}

.section-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 1.15rem;
}

.gold-soft-bg {
  background-color: #fcf3e6;
}

.blue-soft-bg {
  background-color: #eaf1f5;
}

.green-soft-bg {
  background-color: #edf6ef;
}

.section-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #33444d;
}

.section-subtitle {
  margin: 0.15rem 0 0;
  font-size: 0.8rem;
  color: #8c9597;
}

.cust-search-box {
  margin-bottom: 0.65rem;
}

.cust-search-input {
  width: 100%;
  padding: 0.45rem 0.65rem;
  border-radius: 6px;
  border: 1px solid #e0dbce;
  font-size: 0.85rem;
  box-sizing: border-box;
}

.cust-actions-bar {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.8rem;
  margin-bottom: 0.65rem;
}

.btn-text-action {
  background: transparent;
  border: none;
  color: #496883;
  cursor: pointer;
  font-weight: 600;
  padding: 0;
}

.btn-text-action:hover {
  text-decoration: underline;
}

.sep {
  color: #d0c8b8;
}

.customers-select-box {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  max-height: 250px;
  overflow-y: auto;
  border: 1px solid #efebe1;
  border-radius: 7px;
  padding: 0.5rem;
  background-color: #fdfdfc;
}

.customer-select-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.4rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.customer-select-item:hover {
  background-color: #f4eee2;
}

.customer-select-item.selected {
  background-color: #eaf1f5;
}

.cust-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.cust-name-text {
  font-size: 0.85rem;
  font-weight: 600;
  color: #33444d;
}

.cust-sub-text {
  font-size: 0.75rem;
  color: #8b969b;
}

/* Cột phải: Form nhập liệu */
.right-form-column {
  display: flex;
  flex-direction: column;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.1rem 1.4rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.form-field label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #43545c;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.required {
  color: #c0392b;
}

.unit-tag {
  color: #8c9597;
  font-weight: 500;
}

.field-hint-inline {
  font-size: 0.78rem;
  color: #8c9597;
  font-weight: normal;
}

.field-optional {
  font-size: 0.78rem;
  color: #8c9597;
  font-weight: normal;
}

.field-hint {
  font-size: 0.78rem;
  color: #8c9597;
  margin-top: 0.15rem;
}

.form-field input,
.form-field select {
  height: 38px;
  border: 1px solid #dfd8cc;
  border-radius: 7px;
  padding: 0 0.85rem;
  font-size: 0.9rem;
  color: #33444d;
  background-color: #ffffff;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.input-with-button {
  display: flex;
  gap: 0.45rem;
}

.input-with-button input {
  flex: 1;
}

.btn-gen-code {
  height: 38px;
  padding: 0 0.75rem;
  border-radius: 7px;
  border: 1px solid #c9d7e1;
  background-color: #eaf1f5;
  color: #496883;
  font-size: 0.83rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.btn-gen-code:hover {
  background-color: #dce7ee;
  border-color: #496883;
}

.input-with-addon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-addon input {
  width: 100%;
  padding-right: 2.5rem;
}

.input-addon {
  position: absolute;
  right: 0.85rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: #8c9597;
  pointer-events: none;
}

.input-error {
  border-color: #c0392b !important;
  background-color: #fdf5f5;
}

.error-msg {
  font-size: 0.78rem;
  color: #c0392b;
  font-weight: 600;
}

.form-submit-bar {
  display: flex;
  justify-content: flex-end;
  gap: 0.85rem;
  margin-top: 0.5rem;
}

/* Badges */
.badge-form {
  display: inline-flex;
  align-items: center;
  font-size: 0.82rem;
  padding: 0.3rem 0.75rem;
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

.confirm-note-box {
  border-radius: 8px;
  padding: 0.75rem 0.95rem;
  font-size: 0.85rem;
  line-height: 1.45;
}

.note-info {
  background-color: #edf5fa;
  border: 1px solid #d0e4f2;
  color: #376384;
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
  color: #a0a8ab;
  cursor: pointer;
}

.toast-close:hover {
  color: #c0392b;
}

.spin {
  display: inline-block;
  animation: spin 1s infinite linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .create-content-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
  .form-field.full-width {
    grid-column: span 1;
  }
}
</style>
