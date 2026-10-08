<template>
  <div class="voucher-create-wrapper">
    <!-- 1. Thanh tiêu đề trên cùng & Nút quay lại -->
    <div class="page-top-header">
      <div class="top-title-wrap">
        <h2 class="main-page-title">Thêm mới phiếu giảm giá</h2>
      </div>
      <button class="btn-back-pill" @click="goBack" title="Quay lại danh sách phiếu giảm giá">
        ← Quay lại danh sách
      </button>
    </div>

    <!-- 2. Form nội dung: Bố cục dạng phẳng với các khối thông tin chuyên nghiệp -->
    <form @submit.prevent="openConfirmModal" class="form-content-container">
      <!-- KHỐI 1: THÔNG TIN CHUNG (CARD 1 TRONG ẢNH) -->
      <div class="content-card form-section-card">
        <div class="card-section-header">
          <h3 class="section-title">Thông tin chung</h3>
        </div>

        <div class="section-body">
          <!-- Hàng 1 (Full width): Tên phiếu giảm giá -->
          <div class="form-field full-width">
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
                    type="number"
                    v-model.number="form.hoaDonToiThieu"
                    min="0"
                    placeholder="VD: 700 -> 700.000 đ"
                    @blur="handleMoneyBlur('hoaDonToiThieu')"
                    class="form-control"
                    :class="{ 'input-error': errors.hoaDonToiThieu }"
                />
                <span class="input-addon">đ</span>
              </div>
              <div v-if="errors.hoaDonToiThieu" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.hoaDonToiThieu }}
              </div>
              <div v-else-if="form.hoaDonToiThieu" class="field-hint-text">
                ≈ {{ formatMoney(form.hoaDonToiThieu) }}
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
                    type="number"
                    v-model.number="form.giaTriGiamGia"
                    :min="form.loaiPhieuGiamGia === 1 ? 1 : 1000"
                    :max="form.loaiPhieuGiamGia === 1 ? 100 : 999999999"
                    :placeholder="form.loaiPhieuGiamGia === 1 ? 'Nhập từ 1 - 100%' : 'VD: 15 -> 15.000 đ'"
                    @blur="handleMoneyBlur('giaTriGiamGia')"
                    class="form-control"
                    :class="{ 'input-error': errors.giaTriGiamGia }"
                />
                <span class="input-addon">{{ form.loaiPhieuGiamGia === 1 ? '%' : 'đ' }}</span>
              </div>
              <div v-if="errors.giaTriGiamGia" class="field-error-msg">
                <span class="err-icon">ⓘ</span> {{ errors.giaTriGiamGia }}
              </div>
              <div v-else-if="form.loaiPhieuGiamGia === 2 && form.giaTriGiamGia" class="field-hint-text">
                ≈ {{ formatMoney(form.giaTriGiamGia) }}
              </div>
            </div>
          </div>

          <!-- Hàng 4: Giảm tối đa (VNĐ) - Chỉ áp dụng khi giảm theo % -->
          <div class="form-field full-width" v-if="form.loaiPhieuGiamGia === 1">
            <label class="field-label">
              Giảm tối đa (VNĐ)
              <span class="field-hint-inline">(Không bắt buộc)</span>
            </label>
            <div class="input-with-addon">
              <input
                  type="number"
                  v-model.number="form.giamToiDa"
                  min="1000"
                  placeholder="VD: 50 -> 50.000 đ (để trống nếu không giới hạn tối đa)"
                  @blur="handleMoneyBlur('giamToiDa')"
                  class="form-control"
                  :class="{ 'input-error': errors.giamToiDa }"
              />
              <span class="input-addon">đ</span>
            </div>
            <div v-if="errors.giamToiDa" class="field-error-msg">
              <span class="err-icon">ⓘ</span> {{ errors.giamToiDa }}
            </div>
            <div v-else-if="form.giamToiDa" class="field-hint-text">
              ≈ {{ formatMoney(form.giamToiDa) }}
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
                  :min="getTodayString() + 'T00:00'"
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
              <div class="field-label-row">
                <label class="field-label">
                  Ngày kết thúc <span class="required">*</span>
                  <span class="field-hint-inline">(Để trống = Vô hạn)</span>
                </label>
                <button
                    v-if="form.ngayKetThucStr"
                    type="button"
                    class="btn-clear-date"
                    @click="form.ngayKetThucStr = ''"
                    title="Đặt thời gian áp dụng vô hạn"
                >
                  Đặt vô hạn
                </button>
              </div>
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
              <div v-else-if="!form.ngayKetThucStr" class="field-hint-text">
                Áp dụng không giới hạn ngày kết thúc (Vô hạn).
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

      <!-- KHỐI 2: ĐỐI TƯỢNG ÁP DỤNG (CARD 2 TRONG ẢNH) -->
      <div class="content-card form-section-card">
        <div class="card-section-header">
          <h3 class="section-title">Đối tượng áp dụng</h3>
        </div>

        <div class="section-body">
          <!-- Tùy chọn Radio: Công khai hoặc Cá nhân -->
          <div class="target-radio-group">
            <label class="radio-item-label" :class="{ active: form.hinhThuc === 'Công khai' }">
              <input
                  type="radio"
                  name="hinhThuc"
                  value="Công khai"
                  v-model="form.hinhThuc"
              />
              <span class="radio-custom"></span>
              <span class="radio-text">Tất cả (Công khai)</span>
            </label>

            <label class="radio-item-label" :class="{ active: form.hinhThuc === 'Cá nhân' }">
              <input
                  type="radio"
                  name="hinhThuc"
                  value="Cá nhân"
                  v-model="form.hinhThuc"
              />
              <span class="radio-custom"></span>
              <span class="radio-text">Nhóm chỉ định (Cá nhân)</span>
            </label>
          </div>

          <!-- Bảng Khách hàng áp dụng (Chỉ hiển thị khi chọn Nhóm chỉ định / Cá nhân) -->
          <div v-if="form.hinhThuc === 'Cá nhân'" class="customers-assign-panel">
            <div class="customers-panel-header">
              <h4 class="customers-panel-title">Danh sách khách hàng nhận phiếu</h4>
              <div class="customers-count-badge">
                Đã chọn {{ selectedCustomerIds.length }}
              </div>
            </div>

            <!-- Ô tìm kiếm & Chọn nhanh -->
            <div class="customers-search-row">
              <div class="cust-search-input-wrap">
                <input
                    type="text"
                    v-model="custSearchKeyword"
                    placeholder="Tìm kiếm theo mã, tên, SĐT..."
                    class="form-control cust-search-input"
                />
              </div>

              <div class="cust-quick-actions">
                <button type="button" class="btn-link-action" @click="selectAllCustomers">
                  Chọn tất cả
                </button>
                <span class="sep-slash">/</span>
                <button type="button" class="btn-link-action text-danger" @click="clearSelectedCustomers">
                  Bỏ chọn hết
                </button>
              </div>
            </div>

            <!-- Bảng dữ liệu khách hàng theo mẫu Ảnh 2 -->
            <div class="customers-table-wrapper">
              <table class="cust-table">
                <thead>
                <tr>
                  <th style="width: 45px; text-align: center;">
                    <input
                        type="checkbox"
                        :checked="isAllVisibleSelected"
                        @change="toggleSelectAllVisible"
                        title="Chọn tất cả danh sách hiện tại"
                    />
                  </th>
                  <th style="width: 110px;">Mã KH</th>
                  <th>Tên Khách Hàng</th>
                  <th style="width: 120px;">Ngày sinh</th>
                  <th style="width: 130px;">Số điện thoại</th>
                  <th>Email</th>
                  <th style="width: 100px; text-align: center;">Đã mua</th>
                  <th style="width: 140px;">Gần nhất</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="filteredCustomerList.length === 0">
                  <td colspan="8" class="table-empty-cell">
                    Không tìm thấy khách hàng nào khớp với từ khóa tìm kiếm.
                  </td>
                </tr>
                <tr
                    v-else
                    v-for="kh in filteredCustomerList"
                    :key="kh.id"
                    :class="{ 'row-selected': selectedCustomerIds.includes(kh.id) }"
                    @click="toggleCustomerSelect(kh.id)"
                    class="cust-row"
                >
                  <td style="text-align: center;" @click.stop>
                    <input
                        type="checkbox"
                        :checked="selectedCustomerIds.includes(kh.id)"
                        @change="toggleCustomerSelect(kh.id)"
                    />
                  </td>
                  <td class="font-bold text-blue">{{ kh.maKhachHang || ('KH' + kh.id) }}</td>
                  <td class="font-bold text-dark">{{ kh.tenKhachHang }}</td>
                  <td class="text-muted-dark">{{ kh.ngaySinh ? formatDateDisplay(kh.ngaySinh) : '---' }}</td>
                  <td class="font-medium text-dark">{{ kh.soDienThoai || '---' }}</td>
                  <td class="text-muted-dark">{{ kh.email || '---' }}</td>
                  <td style="text-align: center;" class="text-muted-dark">0 đơn</td>
                  <td class="text-muted-dark">---</td>
                </tr>
                </tbody>
              </table>
            </div>

            <div v-if="errors.customers" class="field-error-msg mt-3">
              <span class="err-icon">ⓘ</span> {{ errors.customers }}
            </div>
          </div>
        </div>
      </div>

      <!-- CỤM NÚT THAO TÁC SUBMIT CUỐI TRANG -->
      <div class="form-submit-footer">
        <button type="button" class="btn btn-secondary" @click="goBack">
          Hủy bỏ
        </button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          <span v-if="saving" class="spin">🔄</span>
          {{ saving ? 'Đang lưu vào hệ thống...' : 'Thêm mới phiếu giảm giá' }}
        </button>
      </div>
    </form>

    <!-- 3. POPUP MODAL XÁC NHẬN THÊM PHIẾU MỚI -->
    <div v-if="showConfirmModal" class="modal-overlay" @click.self="closeConfirmModal">
      <div class="modal-box confirm-modal-box">
        <div class="confirm-modal-header">
          <div class="confirm-title-wrap">
            <h3 class="confirm-title">Xác nhận tạo phiếu giảm giá</h3>
            <p class="confirm-subtitle">Kiểm tra thông tin trước khi thêm vào cơ sở dữ liệu</p>
          </div>
          <button class="modal-close-btn" @click="closeConfirmModal">✕</button>
        </div>

        <div class="confirm-modal-body">
          <p class="confirm-message-text">
            Bạn có chắc chắn muốn tạo phiếu giảm giá mới
            <b class="text-blue">[{{ form.tenPhieuGiamGia }}]</b> không?
          </p>

          <!-- Bảng tóm tắt thông số -->
          <div class="confirm-summary-panel">
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
              <span class="s-label">Số lượng phát hành:</span>
              <span class="s-val">{{ form.soLuongSuDung }} lượt</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Thời hạn:</span>
              <span class="s-val">{{ formatDateTimeDisplay(form.ngayBatDauStr) }} ➔ {{ formatDateTimeDisplay(form.ngayKetThucStr) }}</span>
            </div>
            <div class="summary-line">
              <span class="s-label">Trạng thái khởi tạo:</span>
              <span class="s-val font-medium" :class="calculatedStatusMeta.class">
                {{ calculatedStatusMeta.text }}
              </span>
            </div>
          </div>

          <div class="confirm-note-box note-info">
            <b>Lưu ý:</b> Trạng thái của phiếu được tự động xác định dựa trên ngày bắt đầu. Phiếu sẽ tự động có hiệu lực ngay khi đến thời điểm bắt đầu.
          </div>
        </div>

        <div class="confirm-modal-footer">
          <button class="btn btn-secondary" @click="closeConfirmModal" :disabled="saving">
            Hủy bỏ
          </button>
          <button class="btn btn-primary btn-save-confirm" @click="submitCreate" :disabled="saving">
            <span v-if="saving" class="spin">🔄</span>
            {{ saving ? 'Đang lưu vào DB...' : 'Xác nhận tạo' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 5. TOAST THÀNH CÔNG -->
    <div v-if="showSuccessToast" class="toast-success">
      <div class="toast-text">
        <b>Thành công!</b>
        <p>Đã tạo mới phiếu giảm giá vào cơ sở dữ liệu thành công.</p>
      </div>
      <button class="toast-close" @click="showSuccessToast = false">✕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
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

const getNowDateTimeString = () => {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// Khởi tạo Form: Bỏ hoàn toàn trường chọn trạng thái, mã phiếu tự sinh ngầm
const form = ref({
  maPhieuGiamGia: '',
  tenPhieuGiamGia: '',
  hinhThuc: 'Công khai',
  loaiPhieuGiamGia: 1, // 1: %, 2: Tiền mặt
  giaTriGiamGia: null,
  giamToiDa: null,
  hoaDonToiThieu: null,
  soLuongSuDung: null,
  ngayBatDauStr: getNowDateTimeString(),
  ngayKetThucStr: '',
  moTa: '',
  trangThai: 1 // Hệ thống tự động kích hoạt
})

const errors = ref({})

// Tính toán trạng thái tự động theo Ngày bắt đầu
const calculatedStatusMeta = computed(() => {
  if (!form.value.ngayBatDauStr) {
    return { text: 'Chưa xác định', class: 'text-muted' }
  }
  const startDate = new Date(form.value.ngayBatDauStr)
  const now = new Date()
  if (startDate > now) {
    return { text: 'Sắp diễn ra', class: 'text-warning font-bold' }
  }
  return { text: 'Đang hoạt động', class: 'text-success font-bold' }
})

// Tự động thêm 3 số 0 khi nhập tiền mặt nếu số nhập < 1000
const autoAppendThreeZeros = (field) => {
  const val = form.value[field]
  if (val != null && val !== '') {
    const num = Number(val)
    if (!isNaN(num) && num > 0 && num < 1000) {
      form.value[field] = Math.round(num * 1000)
    }
  }
}

const handleMoneyBlur = (field) => {
  if (field === 'giaTriGiamGia' && form.value.loaiPhieuGiamGia !== 2) {
    return
  }
  if (field === 'giamToiDa' && form.value.loaiPhieuGiamGia !== 1) {
    return
  }
  autoAppendThreeZeros(field)
}

const normalizeMoneyInputs = () => {
  if (form.value.loaiPhieuGiamGia === 2) {
    autoAppendThreeZeros('giaTriGiamGia')
  }
  autoAppendThreeZeros('hoaDonToiThieu')
  if (form.value.loaiPhieuGiamGia === 1) {
    autoAppendThreeZeros('giamToiDa')
  }
}

watch(() => form.value.loaiPhieuGiamGia, (newVal) => {
  if (newVal === 2) {
    form.value.giamToiDa = null
    if (errors.value.giamToiDa) delete errors.value.giamToiDa
    if (form.value.giaTriGiamGia != null && form.value.giaTriGiamGia !== '') {
      autoAppendThreeZeros('giaTriGiamGia')
    }
  } else if (newVal === 1) {
    if (form.value.giaTriGiamGia && form.value.giaTriGiamGia > 100) {
      form.value.giaTriGiamGia = null
    }
  }
  if (errors.value.giaTriGiamGia) delete errors.value.giaTriGiamGia
})

// Nạp danh sách khách hàng từ Backend
const fetchCustomers = async () => {
  try {
    const res = await api.get('/api/phieu-giam-gia/khach-hang')
    customerList.value = res.data || []
  } catch (err) {
    console.error('Không thể nạp danh sách khách hàng:', err)
  }
}

onMounted(() => {
  fetchCustomers()
})

const filteredCustomerList = computed(() => {
  if (!custSearchKeyword.value.trim()) return customerList.value
  const kw = custSearchKeyword.value.toLowerCase().trim()
  return customerList.value.filter(kh => {
    return (kh.tenKhachHang && kh.tenKhachHang.toLowerCase().includes(kw)) ||
        (kh.soDienThoai && kh.soDienThoai.toLowerCase().includes(kw)) ||
        (kh.maKhachHang && kh.maKhachHang.toLowerCase().includes(kw)) ||
        (kh.email && kh.email.toLowerCase().includes(kw))
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

const isAllVisibleSelected = computed(() => {
  if (filteredCustomerList.value.length === 0) return false
  return filteredCustomerList.value.every(kh => selectedCustomerIds.value.includes(kh.id))
})

const toggleSelectAllVisible = () => {
  if (isAllVisibleSelected.value) {
    const visibleIds = filteredCustomerList.value.map(kh => kh.id)
    selectedCustomerIds.value = selectedCustomerIds.value.filter(id => !visibleIds.includes(id))
  } else {
    filteredCustomerList.value.forEach(kh => {
      if (!selectedCustomerIds.value.includes(kh.id)) {
        selectedCustomerIds.value.push(kh.id)
      }
    })
  }
}

const selectAllCustomers = () => {
  selectedCustomerIds.value = customerList.value.map(kh => kh.id)
}

const clearSelectedCustomers = () => {
  selectedCustomerIds.value = []
}

// Formatters
const formatMoney = (val) => {
  if (val == null || val === '') return '0 đ'
  return `${Number(val).toLocaleString('vi-VN')} đ`
}

const formatDateDisplay = (dateStr) => {
  if (!dateStr) return '---'
  try {
    const parts = dateStr.split('-')
    if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`
    return dateStr
  } catch (e) {
    return dateStr
  }
}

const formatDateTimeDisplay = (dtStr) => {
  if (!dtStr) return 'Vô hạn'
  try {
    const date = new Date(dtStr)
    if (isNaN(date.getTime())) return dtStr
    const d = String(date.getDate()).padStart(2, '0')
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const y = date.getFullYear()
    const hh = String(date.getHours()).padStart(2, '0')
    const mm = String(date.getMinutes()).padStart(2, '0')
    return `${d}/${m}/${y} ${hh}:${mm}`
  } catch (e) {
    return dtStr
  }
}

// Logic Validate toàn diện tất cả các trường
const validateForm = () => {
  const errs = {}
  normalizeMoneyInputs()

  // 1. Tên phiếu
  if (!form.value.tenPhieuGiamGia || !form.value.tenPhieuGiamGia.trim()) {
    errs.tenPhieuGiamGia = 'Vui lòng nhập tên phiếu giảm giá.'
  } else if (form.value.tenPhieuGiamGia.trim().length < 3) {
    errs.tenPhieuGiamGia = 'Tên phiếu giảm giá phải có tối thiểu 3 ký tự.'
  }

  // 2. Số lượng sử dụng (Bắt buộc tối thiểu 1)
  if (form.value.soLuongSuDung == null || form.value.soLuongSuDung === '') {
    errs.soLuongSuDung = 'Vui lòng nhập số lượng phiếu phát hành.'
  } else {
    const soLuong = Number(form.value.soLuongSuDung)
    if (isNaN(soLuong) || !Number.isInteger(soLuong) || soLuong <= 0) {
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
      }
      if (form.value.hoaDonToiThieu && val > Number(form.value.hoaDonToiThieu)) {
        errs.giaTriGiamGia = 'Mức giảm tiền mặt không được lớn hơn giá trị đơn hàng tối thiểu.'
      }
    }
  }

  // 5. Giảm tối đa (Khi giảm theo %)
  if (form.value.loaiPhieuGiamGia === 1 && form.value.giamToiDa != null && form.value.giamToiDa !== '') {
    const maxVal = Number(form.value.giamToiDa)
    if (isNaN(maxVal) || maxVal < 1000) {
      errs.giamToiDa = 'Mức giảm tối đa phải từ 1.000 đ trở lên.'
    }
  }

  // 6. Thời gian bắt đầu
  if (!form.value.ngayBatDauStr) {
    errs.ngayBatDau = 'Vui lòng chọn thời gian bắt đầu.'
  } else {
    const startDate = new Date(form.value.ngayBatDauStr)
    const todayMidnight = new Date()
    todayMidnight.setHours(0, 0, 0, 0)
    if (startDate < todayMidnight) {
      errs.ngayBatDau = 'Ngày bắt đầu không được nhỏ hơn ngày hôm nay.'
    }
  }

  // 7. Thời gian kết thúc (Nếu có thì phải sau thời gian bắt đầu)
  if (form.value.ngayKetThucStr && form.value.ngayBatDauStr) {
    const startDate = new Date(form.value.ngayBatDauStr)
    const endDate = new Date(form.value.ngayKetThucStr)
    if (endDate <= startDate) {
      errs.ngayKetThuc = 'Thời gian kết thúc phải sau thời gian bắt đầu.'
    }
  }

  // 8. Nếu là hình thức Cá nhân thì phải chọn khách hàng
  if (form.value.hinhThuc === 'Cá nhân' && selectedCustomerIds.value.length === 0) {
    errs.customers = 'Vui lòng chọn ít nhất 1 khách hàng nhận phiếu giảm giá cá nhân.'
  }

  errors.value = errs
  return Object.keys(errs).length === 0
}

// Khi người dùng nhập lại trường nào thì tự động xóa cảnh báo lỗi đỏ của trường đó
watch(() => form.value.tenPhieuGiamGia, (val) => {
  if (val && errors.value.tenPhieuGiamGia) delete errors.value.tenPhieuGiamGia
})
watch(() => form.value.soLuongSuDung, (val) => {
  if (val && errors.value.soLuongSuDung) delete errors.value.soLuongSuDung
})
watch(() => form.value.hoaDonToiThieu, (val) => {
  if (val && errors.value.hoaDonToiThieu) delete errors.value.hoaDonToiThieu
})
watch(() => form.value.giaTriGiamGia, (val) => {
  if (val && errors.value.giaTriGiamGia) delete errors.value.giaTriGiamGia
})
watch(() => form.value.giamToiDa, (val) => {
  if (val && errors.value.giamToiDa) delete errors.value.giamToiDa
})
watch(() => form.value.ngayBatDauStr, (val) => {
  if (val && errors.value.ngayBatDau) delete errors.value.ngayBatDau
})
watch(() => form.value.ngayKetThucStr, () => {
  if (errors.value.ngayKetThuc) delete errors.value.ngayKetThuc
})
watch(selectedCustomerIds, (val) => {
  if (val.length > 0 && errors.value.customers) delete errors.value.customers
}, { deep: true })

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
  // Nếu hợp lệ toàn bộ thì hiển thị Popup modal xác nhận tạo phiếu giảm giá
  showConfirmModal.value = true
}

const closeConfirmModal = () => {
  showConfirmModal.value = false
}

const submitCreate = async () => {
  if (saving.value) return
  saving.value = true

  try {
    normalizeMoneyInputs()

    // Chuyển datetime-local sang ISO Instant
    let startInstant = null
    if (form.value.ngayBatDauStr) {
      startInstant = new Date(form.value.ngayBatDauStr).toISOString()
    }

    let endInstant = null
    if (form.value.ngayKetThucStr) {
      endInstant = new Date(form.value.ngayKetThucStr).toISOString()
    }

    const payload = {
      maPhieuGiamGia: form.value.maPhieuGiamGia ? form.value.maPhieuGiamGia.trim().toUpperCase() : null,
      tenPhieuGiamGia: form.value.tenPhieuGiamGia.trim(),
      hinhThuc: form.value.hinhThuc,
      loaiPhieuGiamGia: form.value.loaiPhieuGiamGia,
      giaTriGiamGia: Number(form.value.giaTriGiamGia),
      giamToiDa: (form.value.loaiPhieuGiamGia === 1 && form.value.giamToiDa) ? Number(form.value.giamToiDa) : null,
      hoaDonToiThieu: Number(form.value.hoaDonToiThieu || 0),
      soLuongSuDung: Number(form.value.soLuongSuDung),
      ngayBatDau: startInstant,
      ngayKetThuc: endInstant,
      trangThai: 1, // Luôn kích hoạt để hệ thống nhận diện
      idKhachHangList: form.value.hinhThuc === 'Cá nhân' ? selectedCustomerIds.value : []
    }

    await api.post('/api/phieu-giam-gia', payload)

    showConfirmModal.value = false
    showSuccessToast.value = true

    setTimeout(() => {
      router.push('/phieu-giam-gia')
    }, 1200)

  } catch (error) {
    console.error('Lỗi khi tạo mới phiếu giảm giá:', error)
    const msg = error.response?.data?.message || error.response?.data || 'Không thể tạo mới phiếu giảm giá. Vui lòng thử lại!'
    alert(typeof msg === 'string' ? msg : JSON.stringify(msg))
    showConfirmModal.value = false
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.push('/phieu-giam-gia')
}
</script>

<style scoped>
/* Khung tổng thể bao ngoài căn lề chuẩn hệ thống */
.voucher-create-wrapper {
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

/* Radio Button Group của Đối tượng áp dụng */
.target-radio-group {
  display: flex;
  gap: 2.5rem;
  padding: 0.3rem 0;
}

.radio-item-label {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #4b5563;
  cursor: pointer;
  user-select: none;
}

.radio-item-label input[type="radio"] {
  width: 17px;
  height: 17px;
  cursor: pointer;
  accent-color: #496883;
}

.radio-item-label.active {
  color: #111827;
}

/* Sub-panel Khách hàng áp dụng */
.customers-assign-panel {
  margin-top: 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1.25rem;
  background-color: #fdfdfd;
}

.customers-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
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

.customers-search-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;
}

.cust-search-input-wrap {
  flex: 1;
  max-width: 420px;
}

.cust-search-input {
  border-radius: 20px;
  padding-left: 1rem;
}

.cust-quick-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
}

.btn-link-action {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-link-action:hover {
  text-decoration: underline;
}

.sep-slash {
  color: #d1d5db;
}

/* Bảng khách hàng chuyên nghiệp */
.customers-table-wrapper {
  max-height: 380px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.cust-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.cust-table th {
  background-color: #f9fafb;
  color: #4b5563;
  font-weight: 700;
  padding: 0.75rem 0.85rem;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  white-space: nowrap;
}

.cust-table td {
  padding: 0.75rem 0.85rem;
  border-bottom: 1px solid #f3f4f6;
  color: #1f2937;
}

.cust-row {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.cust-row:hover {
  background-color: #f9fafb;
}

.row-selected {
  background-color: #f0f7fc !important;
}

.table-empty-cell {
  text-align: center;
  padding: 2rem !important;
  color: #9ca3af;
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

.confirm-modal-header,
.error-modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.confirm-title,
.error-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #111827;
}

.confirm-subtitle,
.error-subtitle {
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

.confirm-modal-body,
.error-modal-body {
  padding: 1.25rem 1.5rem;
}

.confirm-message-text {
  margin: 0 0 1rem;
  font-size: 0.95rem;
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

.confirm-modal-footer,
.error-modal-footer {
  padding: 1rem 1.5rem;
  background-color: #f9fafb;
  border-top: 1px solid #f3f4f6;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.error-list-panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.error-item-line {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #dc2626;
}

.error-bullet {
  font-weight: 700;
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
.mt-3 { margin-top: 0.75rem; }

.spin {
  display: inline-block;
  animation: spin 1s infinite linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 992px) {
  .voucher-create-wrapper {
    padding: 1rem 1.25rem 2rem;
  }
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
