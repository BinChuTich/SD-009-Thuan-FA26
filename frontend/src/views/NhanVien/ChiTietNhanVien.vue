<template>
  <div class="employee-page-wrapper">
    <!-- 1. Thanh tiêu đề trên cùng & Nút hành động -->
    <div class="page-top-header">
      <div class="page-breadcrumb">
        <router-link to="/nhan-vien" class="crumb-parent">Quản lý nhân viên</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Thông tin nhân viên: {{ form.tenNhanVien || form.maNhanVien }}</span>
      </div>
      <div class="header-right-actions">
        <button type="button" class="btn-scan-pill" @click="showScanModal = true">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 7V5a2 2 0 0 1 2-2h2"/>
            <path d="M17 3h2a2 2 0 0 1 2 2v2"/>
            <path d="M21 17v2a2 2 0 0 1-2 2h-2"/>
            <path d="M7 21H5a2 2 0 0 1-2-2v-2"/>
            <line x1="7" y1="12" x2="17" y2="12"/>
          </svg>
          Quét CCCD
        </button>
        <button class="btn-back-pill" @click="goBack" title="Quay lại danh sách nhân viên">
          ← Quay lại danh sách
        </button>
      </div>
    </div>

    <!-- Modal Quét CCCD -->
    <CccdScannerModal
      v-if="showScanModal"
      @close="showScanModal = false"
      @scan-success="onCccdScanned"
    />

    <!-- Trạng thái Đang tải dữ liệu -->
    <div v-if="loading" class="content-card status-msg-card">
      <div class="spinner">⏳</div>
      <span>Đang nạp dữ liệu chi tiết nhân viên từ hệ thống...</span>
    </div>

    <!-- 2. Form nội dung: Bố cục lưới phẳng hiện đại -->
    <form v-else @submit.prevent="handleUpdate" class="employee-layout-grid">
      <!-- Cột trái: Ảnh đại diện -->
      <div class="avatar-sidebar-col">
        <div class="content-card avatar-section-card">
          <div class="card-section-header">
            <h3 class="section-title">Ảnh đại diện</h3>
          </div>
          <div class="avatar-body">
            <div class="avatar-integrated-wrapper">
              <label class="avatar-uploader-box" title="Nhấp để chọn hoặc đổi ảnh đại diện">
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/jpg,image/webp"
                  @change="onImageSelected"
                  style="display: none"
                />
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview-img" alt="avatar" />
                <div v-else class="avatar-empty-placeholder">
                  <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#8c979d" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                  <span class="avatar-placeholder-text">Chọn ảnh</span>
                </div>
                <div class="avatar-hover-overlay">
                  <span>Đổi ảnh</span>
                </div>
              </label>
              <button v-if="avatarPreview" type="button" class="btn-remove-avatar-link" @click="removeAvatar">
                Gỡ ảnh
              </button>
              <span class="avatar-hint-note">Định dạng JPG, PNG, WEBP</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Cột phải: Các Card thông tin xếp dọc phẳng -->
      <div class="form-content-container">
        <!-- KHỐI 1: THÔNG TIN CƠ BẢN -->
        <div class="content-card form-section-card">
          <div class="card-section-header">
            <h3 class="section-title">Thông tin cơ bản</h3>
          </div>
          <div class="section-body">
            <!-- Hàng 1 (2 Cột): Mã nhân viên & Họ và tên -->
            <div class="form-grid-2">
              <div class="form-field">
                <label class="field-label">
                  Mã nhân viên
                  <span class="badge-lock">Cố định</span>
                </label>
                <input
                  v-model="form.maNhanVien"
                  type="text"
                  class="form-control input-disabled"
                  disabled
                />
              </div>

              <div class="form-field">
                <label class="field-label">Họ và tên <span class="required">*</span></label>
                <input
                  v-model="form.tenNhanVien"
                  type="text"
                  placeholder="Nhập họ và tên..."
                  class="form-control"
                  :class="{ 'input-error': errors.tenNhanVien }"
                  @input="onNameInput($event)"
                  @keyup="onNameInput($event)"
                  @compositionend="onNameInput($event)"
                />
                <span v-if="errors.tenNhanVien" class="field-error-msg">{{ errors.tenNhanVien }}</span>
              </div>
            </div>

            <!-- Hàng 2 (2 Cột): Số điện thoại & Email -->
            <div class="form-grid-2">
              <div class="form-field">
                <label class="field-label">Số điện thoại <span class="required">*</span></label>
                <input
                  v-model="form.soDienThoai"
                  type="text"
                  placeholder="VD: 0987654321 (10 - 11 số)"
                  class="form-control"
                  :class="{ 'input-error': errors.soDienThoai }"
                  @input="onPhoneInput($event)"
                  @keyup="onPhoneInput($event)"
                  @compositionend="onPhoneInput($event)"
                />
                <span v-if="errors.soDienThoai" class="field-error-msg">{{ errors.soDienThoai }}</span>
              </div>

              <div class="form-field">
                <label class="field-label">Email <span class="required">*</span></label>
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="example@gmail.com"
                  class="form-control"
                  :class="{ 'input-error': errors.email }"
                  @input="onEmailInput($event)"
                  @keyup="onEmailInput($event)"
                  @compositionend="onEmailInput($event)"
                />
                <span v-if="errors.email" class="field-error-msg">{{ errors.email }}</span>
              </div>
            </div>

            <!-- Hàng 3 (2 Cột): Ngày sinh & Chức vụ -->
            <div class="form-grid-2">
              <div class="form-field">
                <label class="field-label">
                  Ngày sinh <span class="required">*</span>
                  <span class="sub-hint">(Từ đủ 18 tuổi)</span>
                </label>
                <input
                  v-model="form.ngaySinh"
                  type="date"
                  :max="maxDate"
                  class="form-control"
                  :class="{ 'input-error': errors.ngaySinh }"
                  @input="onDobChange($event)"
                  @change="onDobChange($event)"
                />
                <span v-if="errors.ngaySinh" class="field-error-msg">{{ errors.ngaySinh }}</span>
              </div>

              <div class="form-field">
                <label class="field-label">Chức vụ <span class="required">*</span></label>
                <Select2Combobox
                  v-model="form.idVaiTro"
                  :options="roles"
                  value-key="id"
                  label-key="tenVaiTro"
                  placeholder="-- Chọn chức vụ --"
                  search-placeholder="Tìm chức vụ..."
                  :has-error="!!errors.idVaiTro"
                  @change="validateField('idVaiTro')"
                />
                <span v-if="errors.idVaiTro" class="field-error-msg">{{ errors.idVaiTro }}</span>
              </div>
            </div>

            <!-- Hàng 4 (2 Cột): Trạng thái & Giới tính -->
            <div class="form-grid-2">
              <div class="form-field">
                <div class="field-label-row flex items-center justify-between mb-1">
                  <label class="field-label mb-0">Trạng thái</label>
                  <span v-if="isAdminEmployee" class="badge-guard-tag" title="Tài khoản Quản trị viên luôn hoạt động">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                    Quản trị viên
                  </span>
                </div>
                <Select2Combobox
                  v-model="form.trangThai"
                  :options="isAdminEmployee ? [{ value: 1, label: 'Hoạt động (Quản trị viên)' }] : statusOptions"
                  value-key="value"
                  label-key="label"
                  :allow-clear="false"
                  :searchable="false"
                  :disabled="isAdminEmployee"
                  placeholder="Chọn trạng thái..."
                />
                <span v-if="isAdminEmployee" class="field-guard-note">
                  Tài khoản Quản trị viên luôn ở trạng thái Hoạt động
                </span>
              </div>

              <div class="form-field">
                <label class="field-label">Giới tính <span class="required">*</span></label>
                <div class="gender-radio-group">
                  <label class="radio-label">
                    <input type="radio" :value="true" v-model="form.gioiTinh" />
                    <span>Nam</span>
                  </label>
                  <label class="radio-label">
                    <input type="radio" :value="false" v-model="form.gioiTinh" />
                    <span>Nữ</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- KHỐI 2: THÔNG TIN ĐỊA CHỈ -->
        <div class="content-card form-section-card">
          <div class="card-section-header">
            <h3 class="section-title">Thông tin địa chỉ</h3>
          </div>
          <div class="section-body">
            <!-- Hàng 1 (2 Cột): Tỉnh / Thành phố & Phường / Xã -->
            <div class="form-grid-2">
              <div class="form-field">
                <label class="field-label">Tỉnh / Thành phố <span class="required">*</span></label>
                <Select2Combobox
                  v-model="selectedProvinceCode"
                  :options="provincesList"
                  value-key="code"
                  label-key="name"
                  placeholder="-- Chọn Tỉnh / Thành phố --"
                  search-placeholder="Tìm kiếm Tỉnh / Thành phố..."
                  :has-error="!!errors.queQuan"
                  @change="onProvinceChange"
                />
                <span v-if="errors.queQuan" class="field-error-msg">{{ errors.queQuan }}</span>
              </div>

              <div class="form-field">
                <label class="field-label">Phường / Xã <span class="required">*</span></label>
                <Select2Combobox
                  v-model="selectedWardCode"
                  :options="wardsList"
                  value-key="code"
                  label-key="name"
                  :disabled="!selectedProvinceCode"
                  placeholder="-- Chọn Phường / Xã --"
                  search-placeholder="Tìm kiếm Phường / Xã..."
                  :has-error="!!errors.phuong"
                  @change="onWardChange"
                />
                <span v-if="errors.phuong" class="field-error-msg">{{ errors.phuong }}</span>
              </div>
            </div>

            <!-- Hàng 2: Địa chỉ cụ thể -->
            <div class="form-field full-width">
              <label class="field-label">Địa chỉ cụ thể</label>
              <input
                v-model="form.diaChiCuThe"
                type="text"
                placeholder="Số nhà, ngõ, tên đường (không bắt buộc)..."
                class="form-control"
              />
            </div>
          </div>
        </div>

        <!-- KHỐI 3: NÚT HÀNH ĐỘNG -->
        <div class="form-actions-bar">
          <button type="button" class="btn btn-secondary" @click="goBack">
            Quay lại danh sách
          </button>
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}
          </button>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'
import { getProvinces, getWardsByProvince, matchAddressHierarchy, lookupLegacyWard } from '@/services/provincesApi.js'
import { getCurrentUser, setCurrentUser, logout, isCurrentLoggedInUser } from '@/utils/auth.js'
import CccdScannerModal from '@/components/CccdScannerModal.vue'
import Select2Combobox from '@/components/Select2Combobox.vue'

const statusOptions = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Ngừng hoạt động' }
]

const route = useRoute()
const router = useRouter()
const id = route.params.id

const loading = ref(true)
const saving = ref(false)
const showScanModal = ref(false)
const roles = ref([])
const selectedFile = ref(null)
const avatarPreview = ref('')
const maxDate = new Date().toISOString().split('T')[0]

// Provinces (2 cấp: Tỉnh/Thành phố & Phường/Xã)
const provincesList = ref([])
const wardsList = ref([])
const selectedProvinceCode = ref('')
const selectedWardCode = ref('')

const form = reactive({
  maNhanVien: '',
  tenNhanVien: '',
  tenTaiKhoan: '',
  email: '',
  soDienThoai: '',
  anhNhanVien: '',
  gioiTinh: true,
  ngaySinh: '',
  idVaiTro: '',
  trangThai: 1,
  queQuan: '',
  phuong: '',
  diaChiCuThe: ''
})

// Active User Guard: Kiểm tra xem tài khoản này có phải chính là người dùng đang đăng nhập hay không
const isSelf = computed(() => isCurrentLoggedInUser({ id, ...form }))

// Kiểm tra xem tài khoản này có vai trò Quản trị viên hay không
const isAdminEmployee = computed(() => {
  const selectedRole = roles.value.find(r => String(r.id) === String(form.idVaiTro))
  const roleName = (selectedRole?.tenVaiTro || '').toLowerCase()
  const roleCode = (selectedRole?.maVaiTro || '').toUpperCase()
  return roleName.includes('quản trị') || roleCode === 'ADMIN' || isSelf.value
})

// Chặn ngay lập tức nếu có bất kỳ hành động đổi sang Ngừng hoạt động (trangThai = 0) cho Quản trị viên
watch(() => form.trangThai, (newVal) => {
  if (isAdminEmployee.value && Number(newVal) === 0) {
    showAlert({
      title: 'Hành động không được phép',
      message: 'Không thể chuyển trạng thái ngừng hoạt động đối với tài khoản Quản trị viên',
      type: 'warning'
    })
    form.trangThai = 1
  }
})

const errors = reactive({
  tenNhanVien: '',
  email: '',
  soDienThoai: '',
  ngaySinh: '',
  idVaiTro: '',
  queQuan: '',
  phuong: ''
})

async function onProvinceChange() {
  const p = provincesList.value.find(item => String(item.code) === String(selectedProvinceCode.value))
  form.queQuan = p ? p.name : ''
  selectedWardCode.value = ''
  form.phuong = ''
  wardsList.value = []

  validateField('queQuan')

  if (selectedProvinceCode.value) {
    wardsList.value = await getWardsByProvince(selectedProvinceCode.value)
  }
}

function onWardChange() {
  const w = wardsList.value.find(item => String(item.code) === String(selectedWardCode.value))
  form.phuong = w ? w.name : ''
  validateField('phuong')
}

async function onCccdScanned(data) {
  showScanModal.value = false
  if (!data) return

  // 1. Điền Họ và tên
  if (data.fullName) {
    form.tenNhanVien = data.fullName
    errors.tenNhanVien = ''
  }

  // 2. Điền Ngày sinh
  if (data.dob) {
    form.ngaySinh = data.dob
    validateField('ngaySinh')
  }

  // 3. Điền Giới tính
  if (typeof data.gender === 'boolean') {
    form.gioiTinh = data.gender
  }

  // 4. Khớp và Điền Địa chỉ thông minh (2 cấp)
  if (data.fullAddress) {
    try {
      const matched = await matchAddressHierarchy(data.fullAddress)
      if (matched.provinceCode) {
        selectedProvinceCode.value = matched.provinceCode
        form.queQuan = matched.provinceName
        wardsList.value = await getWardsByProvince(matched.provinceCode)

        if (matched.wardCode) {
          selectedWardCode.value = matched.wardCode
          form.phuong = matched.wardName
        }
      }
      form.diaChiCuThe = matched.specificAddress || data.fullAddress
    } catch (e) {
      console.error('Lỗi khớp địa chỉ CCCD:', e)
      form.diaChiCuThe = data.fullAddress
    }
  }

  showToast(`Đã quét CCCD thành công! Đã điền thông tin: ${data.fullName}`, 'success')
}

function onNameInput(e) {
  if (e?.target) {
    form.tenNhanVien = e.target.value
  }
  validateField('tenNhanVien', form.tenNhanVien)
}

function onDobChange(e) {
  if (e?.target) {
    form.ngaySinh = e.target.value
  }
  validateField('ngaySinh', form.ngaySinh)
}

function validateField(fieldName, overrideVal) {
  if (fieldName === 'tenNhanVien') {
    const raw = overrideVal !== undefined ? overrideVal : form.tenNhanVien
    const val = raw ? String(raw).trim() : ''
    if (!val) {
      errors.tenNhanVien = 'Họ và tên nhân viên không được để trống!'
    } else {
      errors.tenNhanVien = ''
    }
  }

  if (fieldName === 'email') {
    const raw = overrideVal !== undefined ? overrideVal : form.email
    const val = raw ? String(raw).trim() : ''
    if (!val) {
      errors.email = 'Email nhân viên không được để trống!'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
      errors.email = 'Định dạng email không hợp lệ (VD: example@gmail.com)!'
    } else {
      errors.email = ''
    }
  }

  if (fieldName === 'soDienThoai') {
    const raw = overrideVal !== undefined ? overrideVal : form.soDienThoai
    const val = raw ? String(raw).trim() : ''
    if (!val) {
      errors.soDienThoai = 'Số điện thoại không được để trống!'
    } else if (!/^\d+$/.test(val)) {
      errors.soDienThoai = 'Số điện thoại chỉ được chứa các chữ số!'
    } else if (!val.startsWith('0')) {
      errors.soDienThoai = 'Số điện thoại phải bắt đầu bằng số 0!'
    } else if (val.length < 10) {
      errors.soDienThoai = `Số điện thoại không được dưới 10 số (hiện tại có ${val.length} số)!`
    } else if (val.length > 11) {
      errors.soDienThoai = `Số điện thoại không được trên 11 số (hiện tại có ${val.length} số)!`
    } else {
      errors.soDienThoai = ''
    }
  }

  if (fieldName === 'ngaySinh') {
    const raw = overrideVal !== undefined ? overrideVal : form.ngaySinh
    if (!raw) {
      errors.ngaySinh = 'Ngày sinh nhân viên không được để trống!'
    } else {
      const bDate = new Date(raw)
      const today = new Date()
      if (bDate > today) {
        errors.ngaySinh = 'Ngày sinh không được lớn hơn ngày hiện tại!'
      } else {
        let age = today.getFullYear() - bDate.getFullYear()
        const m = today.getMonth() - bDate.getMonth()
        if (m < 0 || (m === 0 && today.getDate() < bDate.getDate())) {
          age--
        }
        if (age < 18) {
          errors.ngaySinh = 'Nhân viên phải từ đủ 18 tuổi trở lên!'
        } else {
          errors.ngaySinh = ''
        }
      }
    }
  }

  if (fieldName === 'idVaiTro') {
    if (!form.idVaiTro) {
      errors.idVaiTro = 'Vui lòng chọn vai trò cho nhân viên!'
    } else {
      errors.idVaiTro = ''
    }
  }

  if (fieldName === 'queQuan') {
    const raw = overrideVal !== undefined ? overrideVal : form.queQuan
    if (!raw || !String(raw).trim()) {
      errors.queQuan = 'Vui lòng chọn Tỉnh / Thành phố!'
    } else {
      errors.queQuan = ''
    }
  }

  if (fieldName === 'phuong') {
    const raw = overrideVal !== undefined ? overrideVal : form.phuong
    if (!raw || !String(raw).trim()) {
      errors.phuong = 'Vui lòng chọn Phường / Xã!'
    } else {
      errors.phuong = ''
    }
  }
}

let emailTimer = null
let phoneTimer = null

async function checkEmailRealtime() {
  const val = form.email ? form.email.trim() : ''
  if (!val || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return
  try {
    const res = await api.get('/api/nhan-vien/check-email', { params: { email: val, id: id } })
    if (form.email && form.email.trim() === val) {
      if (res.data?.exists) {
        errors.email = 'Email này đã tồn tại trong hệ thống!'
      } else if (errors.email === 'Email này đã tồn tại trong hệ thống!') {
        errors.email = ''
      }
    }
  } catch (e) {
    console.error('Lỗi check email:', e)
  }
}

async function checkPhoneRealtime() {
  const val = form.soDienThoai ? form.soDienThoai.trim() : ''
  if (!val || !/^\d+$/.test(val) || !val.startsWith('0') || val.length < 10 || val.length > 11) return
  try {
    const res = await api.get('/api/nhan-vien/check-phone', { params: { phone: val, id: id } })
    if (form.soDienThoai && form.soDienThoai.trim() === val) {
      if (res.data?.exists) {
        errors.soDienThoai = 'Số điện thoại này đã được sử dụng!'
      } else if (errors.soDienThoai === 'Số điện thoại này đã được sử dụng!') {
        errors.soDienThoai = ''
      }
    }
  } catch (e) {
    console.error('Lỗi check phone:', e)
  }
}

function onEmailInput(e) {
  if (e?.target) {
    form.email = e.target.value
  }
  validateField('email', form.email)
  clearTimeout(emailTimer)
  if (!errors.email) {
    emailTimer = setTimeout(checkEmailRealtime, 200)
  }
}

function onPhoneInput(e) {
  if (e?.target) {
    form.soDienThoai = e.target.value
  }
  validateField('soDienThoai', form.soDienThoai)
  clearTimeout(phoneTimer)
  if (!errors.soDienThoai) {
    phoneTimer = setTimeout(checkPhoneRealtime, 200)
  }
}

watch(() => form.soDienThoai, (newVal) => {
  validateField('soDienThoai', newVal)
  clearTimeout(phoneTimer)
  if (!errors.soDienThoai) {
    phoneTimer = setTimeout(checkPhoneRealtime, 200)
  }
})

watch(() => form.email, (newVal) => {
  validateField('email', newVal)
  clearTimeout(emailTimer)
  if (!errors.email) {
    emailTimer = setTimeout(checkEmailRealtime, 200)
  }
})

async function validateAll() {
  validateField('tenNhanVien')
  validateField('email')
  validateField('soDienThoai')
  validateField('ngaySinh')
  validateField('idVaiTro')
  validateField('queQuan')
  validateField('phuong')

  if (!errors.email) {
    await checkEmailRealtime()
  }
  if (!errors.soDienThoai) {
    await checkPhoneRealtime()
  }

  return !Object.values(errors).some(msg => msg !== '')
}

async function loadRoles() {
  try {
    const res = await api.get('/api/nhan-vien/vai-tro')
    roles.value = (res.data || []).filter(r =>
      r.maVaiTro === 'ADMIN' || r.maVaiTro === 'NV' ||
      r.tenVaiTro === 'Quản trị viên' || r.tenVaiTro === 'Nhân viên'
    )
  } catch (e) {
    console.error('Lỗi vai trò:', e)
  }
}

async function loadEmployee() {
  loading.value = true
  try {
    provincesList.value = await getProvinces()
    await loadRoles()

    const res = await api.get(`/api/nhan-vien/${id}`)
    const data = res.data
    form.maNhanVien = data.maNhanVien || ''
    form.tenNhanVien = data.tenNhanVien || ''
    form.tenTaiKhoan = data.tenTaiKhoan || ''
    form.email = data.email || ''
    form.soDienThoai = data.soDienThoai || ''
    form.anhNhanVien = data.anhNhanVien || ''
    form.gioiTinh = data.gioiTinh ?? true
    form.ngaySinh = data.ngaySinh || ''
    form.idVaiTro = data.idVaiTro || (roles.value[0]?.id ?? '')
    form.trangThai = data.trangThai ?? 1
    form.queQuan = data.queQuan || ''
    form.phuong = data.phuong || ''
    form.diaChiCuThe = data.diaChiCuThe || ''

    if (data.anhNhanVien) {
      avatarPreview.value = data.anhNhanVien.startsWith('http')
        ? data.anhNhanVien
        : `http://localhost:8080${data.anhNhanVien}`
    }

    // Khớp danh mục hành chính từ địa chỉ quê quán đã lưu
    if (form.queQuan) {
      const cleanStr = (s) => (s || '').toLowerCase()
        .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')
        .trim()

      const matchP = provincesList.value.find(p =>
        p.name.toLowerCase() === form.queQuan.toLowerCase() ||
        cleanStr(p.name) === cleanStr(form.queQuan) ||
        p.name.toLowerCase().includes(cleanStr(form.queQuan)) ||
        form.queQuan.toLowerCase().includes(cleanStr(p.name))
      )
      if (matchP) {
        selectedProvinceCode.value = matchP.code
        wardsList.value = await getWardsByProvince(matchP.code)

        if (form.phuong) {
          let matchW = wardsList.value.find(w =>
            w.name.toLowerCase() === form.phuong.toLowerCase() ||
            cleanStr(w.name) === cleanStr(form.phuong) ||
            w.name.toLowerCase().includes(cleanStr(form.phuong)) ||
            form.phuong.toLowerCase().includes(cleanStr(w.name))
          )
          if (!matchW && form.phuong) {
            const legWard = await lookupLegacyWard(form.phuong, matchP.code)
            if (legWard) {
              matchW = wardsList.value.find(w => w.code === legWard.code)
            }
          }
          if (matchW) {
            selectedWardCode.value = matchW.code
            form.phuong = matchW.name
          } else {
            wardsList.value.unshift({ code: -1, name: form.phuong })
            selectedWardCode.value = -1
          }
        }
      } else {
        provincesList.value.unshift({ code: -1, name: form.queQuan })
        selectedProvinceCode.value = -1
        if (form.phuong) {
          wardsList.value = [{ code: -1, name: form.phuong }]
          selectedWardCode.value = -1
        }
      }
    }
  } catch (e) {
    showAlert({
      title: 'Không tìm thấy nhân viên',
      message: e?.response?.data?.message || 'Không thể tải thông tin nhân viên.',
      type: 'error'
    })
    router.push('/nhan-vien')
  } finally {
    loading.value = false
  }
}

function onImageSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showAlert({
      title: 'Định dạng không hợp lệ',
      message: 'Vui lòng chọn file hình ảnh (JPG, PNG, WEBP).',
      type: 'warning'
    })
    e.target.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    showAlert({
      title: 'Dung lượng quá lớn',
      message: 'Kích thước ảnh không được vượt quá 5MB.',
      type: 'warning'
    })
    e.target.value = ''
    return
  }

  selectedFile.value = file
  if (avatarPreview.value) {
    URL.revokeObjectURL(avatarPreview.value)
  }
  avatarPreview.value = URL.createObjectURL(file)
}

function removeAvatar() {
  selectedFile.value = null
  avatarPreview.value = ''
  form.anhNhanVien = ''
}

function goBack() {
  router.push('/nhan-vien')
}

async function handleUpdate() {
  const isValid = await validateAll()
  if (!isValid) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng kiểm tra và sửa các thông tin báo lỗi trên form!',
      type: 'warning'
    })
  }

  const isSelfUser = isCurrentLoggedInUser({ id, ...form })
  if (isSelfUser && Number(form.trangThai) === 0) {
    showAlert({
      title: 'Hành động không được phép',
      message: 'Bạn không thể chuyển trạng thái ngừng hoạt động đối với tài khoản đang đăng nhập hệ thống',
      type: 'warning'
    })
    form.trangThai = 1
    return
  }

  const confirmed = await showConfirm({
    title: 'Xác nhận cập nhật nhân viên',
    message: `Bạn có chắc chắn muốn lưu các thay đổi cho nhân viên "${form.tenNhanVien.trim()}" không?`,
    type: 'question',
    confirmText: 'Lưu thay đổi',
    cancelText: 'Hủy bỏ'
  })
  if (!confirmed) return

  saving.value = true
  try {
    const payload = {
      tenNhanVien: form.tenNhanVien.trim(),
      email: form.email.trim(),
      soDienThoai: form.soDienThoai.trim(),
      gioiTinh: form.gioiTinh,
      ngaySinh: form.ngaySinh,
      idVaiTro: form.idVaiTro,
      trangThai: form.trangThai,
      queQuan: form.queQuan.trim(),
      phuong: form.phuong.trim(),
      diaChiCuThe: form.diaChiCuThe ? form.diaChiCuThe.trim() : '',
      anhNhanVien: form.anhNhanVien || undefined
    }

    if (selectedFile.value) {
      const formData = new FormData()
      formData.append(
        'data',
        new Blob([JSON.stringify(payload)], { type: 'application/json' })
      )
      formData.append('file', selectedFile.value)
      await api.put(`/api/nhan-vien/${id}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    } else {
      await api.put(`/api/nhan-vien/${id}`, payload)
    }

    if (isSelfUser) {
      const current = getCurrentUser() || {}
      setCurrentUser({
        ...current,
        tenNhanVien: payload.tenNhanVien,
        email: payload.email,
        soDienThoai: payload.soDienThoai
      })
    }

    showToast('Cập nhật thông tin nhân viên thành công!', 'success')
    router.push('/nhan-vien')
  } catch (e) {
    showAlert({
      title: 'Không thể cập nhật nhân viên',
      message: e?.response?.data?.message || 'Có lỗi xảy ra trong quá trình cập nhật dữ liệu.',
      type: 'error'
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadEmployee()
})
</script>

<style scoped>
/* Khung tổng thể bao ngoài căn lề chuẩn hệ thống */
.employee-page-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header trên cùng & Breadcrumb */
.page-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-breadcrumb {
  font-size: 0.95rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  line-height: 1.4;
}

.crumb-parent {
  color: #6b7280;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s ease;
}

.crumb-parent:hover {
  color: #1f2937;
  text-decoration: underline;
}

.crumb-separator {
  color: #9ca3af;
  font-weight: 400;
}

.crumb-current {
  color: var(--blue, #496883);
  font-weight: 700;
}

.header-right-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-scan-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.4rem 1.1rem;
  background-color: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(73, 104, 131, 0.25);
  transition: all 0.2s ease;
}

.btn-scan-pill:hover {
  background-color: #3b576e;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.35);
}

.btn-back-pill {
  padding: 0.4rem 1rem;
  border-radius: 20px;
  border: 1px solid #d2d6dc;
  background-color: #ffffff;
  color: #4b5563;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.btn-back-pill:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
  color: #111827;
}

/* Trạng thái thông báo đang nạp dữ liệu */
.status-msg-card {
  text-align: center;
  padding: 3.5rem !important;
  color: #647074;
}

.spinner {
  font-size: 1.8rem;
  margin-bottom: 0.5rem;
}

/* 2. Bố cục trang nhân viên */
.employee-layout-grid {
  display: grid;
  grid-template-columns: 270px 1fr;
  gap: 1.5rem;
  align-items: start;
}

/* Thẻ Card trắng chuẩn hệ thống */
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

/* Avatar Card */
.avatar-section-card {
  text-align: center;
}

.avatar-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0.5rem 0;
}

.avatar-integrated-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.85rem;
  width: 100%;
}

.avatar-uploader-box {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  margin: 0 auto;
  background: #eaf1f5;
  border: 3px solid #ffffff;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.25s ease;
}

.avatar-uploader-box:hover {
  border-color: #496883;
  transform: scale(1.02);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.2);
}

.avatar-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-empty-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  color: #647b8c;
}

.avatar-placeholder-text {
  font-size: 0.88rem;
  font-weight: 600;
  color: #496883;
}

.avatar-hover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(43, 62, 79, 0.65);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-uploader-box:hover .avatar-hover-overlay {
  opacity: 1;
}

.btn-remove-avatar-link {
  background: none;
  border: none;
  color: #dc2626;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  transition: background-color 0.15s ease;
}

.btn-remove-avatar-link:hover {
  background-color: #fee2e2;
}

.avatar-hint-note {
  font-size: 0.78rem;
  color: #8c979d;
}

/* Cột phải: Form Content Container */
.form-content-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
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
  gap: 0.45rem;
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

.required {
  color: #dc2626;
  font-weight: bold;
}

.sub-hint {
  font-size: 0.78rem;
  color: #8c979d;
  font-weight: normal;
}

.form-control {
  width: 100%;
  height: 2.7rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #496883;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.input-error {
  border-color: #dc2626 !important;
  background-color: #fffaf9 !important;
}

.input-error:focus {
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15) !important;
}

.field-error-msg {
  font-size: 0.82rem;
  color: #dc2626;
  font-weight: 500;
  line-height: 1.25;
}

.input-disabled {
  background-color: #f3f4f6 !important;
  color: #6b7280 !important;
  cursor: not-allowed;
  font-weight: 600;
  border-color: #e5e7eb;
}

/* Radio Group */
.gender-radio-group {
  display: flex;
  align-items: center;
  gap: 2rem;
  height: 2.7rem;
}

.radio-label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #374151;
  cursor: pointer;
  font-weight: 500;
}

.radio-label input[type='radio'] {
  width: 1.15rem;
  height: 1.15rem;
  accent-color: #496883;
  cursor: pointer;
  margin: 0;
}

/* Form Actions */
.form-actions-bar {
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
  padding-top: 0.5rem;
}

.btn {
  height: 2.6rem;
  padding: 0 1.5rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  background-color: #f4efe6;
  color: #6d6352;
  border: 1px solid #e2dacd;
}
.btn-secondary:hover {
  background-color: #ebe3d6;
}

.btn-primary {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #2c4350;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .employee-layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}

.badge-guard-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  color: #854d0e;
  background-color: #fef9c3;
  border: 1px solid #fef08a;
  padding: 2px 8px;
  border-radius: 9999px;
  letter-spacing: 0.2px;
}

.field-guard-note {
  display: block;
  font-size: 0.78rem;
  color: #b45309;
  margin-top: 5px;
  font-weight: 500;
}
</style>
