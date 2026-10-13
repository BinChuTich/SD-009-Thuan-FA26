<template>
  <div class="employee-page-wrapper">
    <!-- Header -->
    <div class="page-top-header">
      <div class="page-breadcrumb">
        <router-link to="/nhan-vien" class="crumb-parent">Quản lý nhân viên</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Thêm mới nhân viên</span>
      </div>
      <div class="header-right">
        <button class="btn-back" @click="goBack">
          <span>←</span> Quay lại danh sách
        </button>
        <button type="button" class="btn-scan-cccd" @click="showScanModal = true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="7" height="7"></rect>
            <rect x="14" y="3" width="7" height="7"></rect>
            <rect x="14" y="14" width="7" height="7"></rect>
            <rect x="3" y="14" width="7" height="7"></rect>
          </svg>
          Quét CCCD gắn chip
        </button>
      </div>
    </div>

    <!-- Modal Quét CCCD -->
    <CccdScannerModal
      v-if="showScanModal"
      @close="showScanModal = false"
      @scan-success="onCccdScanned"
    />

    <!-- Form Container -->
    <div class="form-container-card">
      <div class="form-layout-grid">
        <!-- Cột trái: Ảnh đại diện -->
        <div class="avatar-upload-panel">
          <div class="avatar-card">
            <h4 class="card-subtitle">Ảnh đại diện</h4>
            <div class="avatar-integrated-wrapper">
              <label class="avatar-uploader-box" title="Nhấp để chọn ảnh đại diện">
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/jpg,image/webp"
                  @change="onImageSelected"
                  style="display: none"
                />
                <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview-img" alt="avatar" />
                <div v-else class="avatar-empty-placeholder">
                  <span class="avatar-placeholder-text">Chọn ảnh</span>
                </div>
                <div class="avatar-hover-overlay">
                  <span>Đổi ảnh</span>
                </div>
              </label>
              <button v-if="avatarPreview" type="button" class="btn-remove-avatar-link" @click="removeAvatar">
                Gỡ ảnh
              </button>
            </div>
          </div>
        </div>

        <!-- Cột phải: Thông tin chi tiết -->
        <div class="form-fields-panel">
          <!-- Phần 1: Thông tin nhân viên -->
          <div class="section-card">
            <h4 class="section-title">Thông tin nhân viên</h4>
            <div class="form-grid">
              <!-- Mã nhân viên (TỰ SINH - KHÓA) -->
              <div class="form-field">
                <label>Mã nhân viên <span class="tag-auto">(Tự sinh theo họ tên)</span></label>
                <input
                  v-model="form.maNhanVien"
                  type="text"
                  class="input-disabled"
                  disabled
                  placeholder="Tự sinh theo tên (VD: anhntv1)..."
                />
              </div>

              <!-- Họ và tên -->
              <div class="form-field">
                <label>Họ và tên <span class="required">*</span></label>
                <input
                  v-model="form.tenNhanVien"
                  type="text"
                  placeholder="Nhập họ và tên (VD: Nguyễn Thị Vân Anh)..."
                  :class="{ 'has-error': errors.tenNhanVien }"
                  @input="onNameInput"
                />
                <span v-if="errors.tenNhanVien" class="error-inline-msg">{{ errors.tenNhanVien }}</span>
              </div>

              <!-- Số điện thoại -->
              <div class="form-field">
                <label>Số điện thoại <span class="required">*</span></label>
                <input
                  v-model="form.soDienThoai"
                  type="text"
                  placeholder="VD: 0987654321 (10 - 11 số)"
                  :class="{ 'has-error': errors.soDienThoai }"
                  @input="validateField('soDienThoai')"
                />
                <span v-if="errors.soDienThoai" class="error-inline-msg">{{ errors.soDienThoai }}</span>
              </div>

              <!-- Email -->
              <div class="form-field">
                <label>Email <span class="required">*</span></label>
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="example@gmail.com"
                  :class="{ 'has-error': errors.email }"
                  @input="validateField('email')"
                />
                <span v-if="errors.email" class="error-inline-msg">{{ errors.email }}</span>
              </div>

              <!-- Giới tính (Radio) -->
              <div class="form-field">
                <label>Giới tính <span class="required">*</span></label>
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

              <!-- Ngày sinh -->
              <div class="form-field">
                <label>Ngày sinh <span class="required">*</span> <span class="sub-hint">(Từ đủ 18 tuổi)</span></label>
                <input
                  v-model="form.ngaySinh"
                  type="date"
                  :max="maxDate"
                  :class="{ 'has-error': errors.ngaySinh }"
                  @change="validateField('ngaySinh')"
                />
                <span v-if="errors.ngaySinh" class="error-inline-msg">{{ errors.ngaySinh }}</span>
              </div>

              <!-- Chức vụ -->
              <div class="form-field">
                <label>Chức vụ <span class="required">*</span></label>
                <Select2Combobox
                  v-model="form.idVaiTro"
                  :options="roles"
                  value-key="id"
                  label-key="tenVaiTro"
                  placeholder="-- Chọn chức vụ --"
                  all-label="-- Chọn chức vụ --"
                  search-placeholder="Tìm chức vụ..."
                  @change="validateField('idVaiTro')"
                />
                <span v-if="errors.idVaiTro" class="error-inline-msg">{{ errors.idVaiTro }}</span>
              </div>

              <!-- Trạng thái -->
              <div class="form-field">
                <label>Trạng thái</label>
                <Select2Combobox
                  v-model="form.trangThai"
                  :options="trangThaiOptions"
                  value-key="id"
                  label-key="name"
                  :show-all-option="false"
                  :allow-clear="false"
                  placeholder="-- Chọn trạng thái --"
                  search-placeholder="Tìm trạng thái..."
                />
              </div>
            </div>
          </div>

          <!-- Phần 2: Thông tin địa chỉ -->
          <div class="section-card">
            <h4 class="section-title">Thông tin địa chỉ</h4>
            <div class="form-grid">
              <!-- Tỉnh / Thành phố -->
              <div class="form-field">
                <label>Tỉnh / Thành phố <span class="required">*</span></label>
                <Select2Combobox
                  v-model="selectedProvinceCode"
                  :options="provincesList"
                  value-key="code"
                  label-key="name"
                  placeholder="-- Chọn Tỉnh / Thành phố --"
                  all-label="-- Chọn Tỉnh / Thành phố --"
                  search-placeholder="Tìm kiếm Tỉnh / Thành phố..."
                  @change="onProvinceChange"
                />
                <span v-if="errors.queQuan" class="error-inline-msg">{{ errors.queQuan }}</span>
              </div>

              <!-- Phường / Xã -->
              <div class="form-field">
                <label>Phường / Xã <span class="required">*</span></label>
                <Select2Combobox
                  v-model="selectedWardCode"
                  :options="wardsList"
                  :disabled="!selectedProvinceCode"
                  value-key="code"
                  label-key="name"
                  placeholder="-- Chọn Phường / Xã --"
                  all-label="-- Chọn Phường / Xã --"
                  search-placeholder="Tìm kiếm Phường / Xã..."
                  @change="onWardChange"
                />
                <span v-if="errors.phuong" class="error-inline-msg">{{ errors.phuong }}</span>
              </div>

              <!-- Địa chỉ cụ thể -->
              <div class="form-field full-width">
                <label>Địa chỉ cụ thể</label>
                <input
                  v-model="form.diaChiCuThe"
                  type="text"
                  placeholder="Số nhà, ngõ, tên đường (không bắt buộc)..."
                />
              </div>
            </div>
          </div>

          <!-- Nút hành động -->
          <div class="form-actions-bar">
            <button class="btn btn-secondary" @click="goBack">
              Hủy bỏ
            </button>
            <button class="btn btn-primary" :disabled="saving" @click="handleSubmit">
              {{ saving ? 'Đang lưu...' : 'Thêm nhân viên' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'
import { getProvinces, getWardsByProvince, matchAddressHierarchy } from '@/services/provincesApi.js'
import CccdScannerModal from '@/components/cccdScannerModal.vue'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()
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

const trangThaiOptions = [
  { id: 1, name: 'Hoạt động' },
  { id: 0, name: 'Ngừng hoạt động' }
]

const form = reactive({
  maNhanVien: '',
  tenNhanVien: '',
  email: '',
  soDienThoai: '',
  gioiTinh: true,
  ngaySinh: '',
  idVaiTro: '',
  trangThai: 1,
  queQuan: '',
  phuong: '',
  diaChiCuThe: ''
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

function removeVietnameseTones(str) {
  if (!str) return ''
  str = str.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, 'a')
  str = str.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, 'e')
  str = str.replace(/ì|í|ị|ỉ|ĩ/g, 'i')
  str = str.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, 'o')
  str = str.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, 'u')
  str = str.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, 'y')
  str = str.replace(/đ/g, 'd')
  str = str.replace(/À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ/g, 'a')
  str = str.replace(/È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ/g, 'e')
  str = str.replace(/Ì|Í|Ị|Ỉ|Ĩ/g, 'i')
  str = str.replace(/Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ/g, 'o')
  str = str.replace(/Ù|Ú|Ụ|Ủ|Ũ|Ư|Ừ|Ứ|Ự|Ử|Ữ/g, 'u')
  str = str.replace(/Ỳ|Ý|Ỵ|Ỷ|Ỹ/g, 'y')
  str = str.replace(/Đ/g, 'd')
  str = str.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  return str.toLowerCase().replace(/[^a-z\s]/g, ' ').trim()
}

function generateEmployeePrefixClient(fullName) {
  if (!fullName || !fullName.trim()) return ''
  const clean = removeVietnameseTones(fullName)
  const parts = clean.split(/\s+/).filter(Boolean)
  if (parts.length === 0) return ''
  if (parts.length === 1) return parts[0]
  const firstName = parts[parts.length - 1]
  let initials = ''
  for (let i = 0; i < parts.length - 1; i++) {
    if (parts[i].length > 0) {
      initials += parts[i][0]
    }
  }
  return `${firstName}${initials}`
}

let nameDebounceTimer = null
watch(() => form.tenNhanVien, (newVal) => {
  if (!newVal || !newVal.trim()) {
    form.maNhanVien = ''
    return
  }
  // 1. Tự sinh mã tức thì trên client (VD: Nguyễn Thị Vân Anh -> anhntv1)
  const prefix = generateEmployeePrefixClient(newVal)
  if (prefix) {
    if (!form.maNhanVien || !form.maNhanVien.toLowerCase().startsWith(prefix)) {
      form.maNhanVien = prefix + '1'
    }
  }

  // 2. Tra cứu database để lấy số thứ tự tiếp theo chính xác (+1 so với đã lưu)
  clearTimeout(nameDebounceTimer)
  nameDebounceTimer = setTimeout(async () => {
    try {
      const res = await api.get('/api/nhan-vien/preview-code', {
        params: { name: newVal.trim() }
      })
      if (res.data?.code) {
        form.maNhanVien = res.data.code
      }
    } catch (e) {
      console.warn('Lỗi kiểm tra mã nhân viên từ server:', e)
    }
  }, 150)
})

function onNameInput() {
  validateField('tenNhanVien')
}

async function loadProvinces() {
  provincesList.value = await getProvinces()
}

async function onProvinceChange(newVal) {
  if (newVal !== undefined) {
    selectedProvinceCode.value = newVal
  }
  const p = provincesList.value.find(item => item.code === selectedProvinceCode.value)
  form.queQuan = p ? p.name : ''
  selectedWardCode.value = ''
  form.phuong = ''
  wardsList.value = []

  validateField('queQuan')

  if (selectedProvinceCode.value) {
    wardsList.value = await getWardsByProvince(selectedProvinceCode.value)
  }
}

function onWardChange(newVal) {
  if (newVal !== undefined) {
    selectedWardCode.value = newVal
  }
  const w = wardsList.value.find(item => item.code === selectedWardCode.value)
  form.phuong = w ? w.name : ''
  validateField('phuong')
}

async function onCccdScanned(data) {
  showScanModal.value = false
  if (!data) return

  // 1. Điền Họ và tên & Tự sinh mã
  if (data.fullName) {
    form.tenNhanVien = data.fullName
    errors.tenNhanVien = ''
    try {
      const res = await api.get('/api/nhan-vien/preview-code', {
        params: { name: form.tenNhanVien.trim() }
      })
      if (res.data?.code) {
        form.maNhanVien = res.data.code
      }
    } catch (e) {
      console.error('Lỗi sinh mã nhân viên:', e)
    }
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

function validateField(fieldName) {
  if (fieldName === 'tenNhanVien') {
    if (!form.tenNhanVien || !form.tenNhanVien.trim()) {
      errors.tenNhanVien = 'Họ và tên nhân viên không được để trống!'
    } else {
      errors.tenNhanVien = ''
    }
  }

  if (fieldName === 'email') {
    const val = form.email ? form.email.trim() : ''
    if (!val) {
      errors.email = 'Email nhân viên không được để trống!'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
      errors.email = 'Định dạng email không hợp lệ (VD: example@gmail.com)!'
    } else {
      errors.email = ''
    }
  }

  if (fieldName === 'soDienThoai') {
    const val = form.soDienThoai ? form.soDienThoai.trim() : ''
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
    if (!form.ngaySinh) {
      errors.ngaySinh = 'Ngày sinh nhân viên không được để trống!'
    } else {
      const bDate = new Date(form.ngaySinh)
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
    if (!form.queQuan || !form.queQuan.trim()) {
      errors.queQuan = 'Vui lòng chọn Tỉnh / Thành phố!'
    } else {
      errors.queQuan = ''
    }
  }

  if (fieldName === 'phuong') {
    if (!form.phuong || !form.phuong.trim()) {
      errors.phuong = 'Vui lòng chọn Phường / Xã!'
    } else {
      errors.phuong = ''
    }
  }
}

function validateAll() {
  validateField('tenNhanVien')
  validateField('email')
  validateField('soDienThoai')
  validateField('ngaySinh')
  validateField('idVaiTro')
  validateField('queQuan')
  validateField('phuong')

  return !Object.values(errors).some(msg => msg !== '')
}

async function loadRoles() {
  try {
    const res = await api.get('/api/nhan-vien/vai-tro')
    roles.value = (res.data || []).filter(r =>
      r.maVaiTro === 'ADMIN' || r.maVaiTro === 'NV' ||
      r.tenVaiTro === 'Quản trị viên' || r.tenVaiTro === 'Nhân viên'
    )
    if (roles.value.length > 0) {
      const defaultRole = roles.value.find(r => r.maVaiTro === 'NV') || roles.value[0]
      form.idVaiTro = defaultRole.id
    }
  } catch (e) {
    console.error('Không thể tải danh sách vai trò:', e)
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
  if (avatarPreview.value) {
    URL.revokeObjectURL(avatarPreview.value)
    avatarPreview.value = ''
  }
}

function goBack() {
  router.push('/nhan-vien')
}

async function handleSubmit() {
  if (!validateAll()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng kiểm tra và sửa các trường thông tin báo lỗi trên form!',
      type: 'warning'
    })
  }

  const confirmed = await showConfirm({
    title: 'Xác nhận thêm mới nhân viên',
    message: `Bạn có chắc chắn muốn thêm nhân viên "${form.tenNhanVien.trim()}"? Hệ thống sẽ tự động gửi email thông tin đăng nhập tới "${form.email.trim()}".`,
    type: 'question',
    confirmText: 'Xác nhận thêm',
    cancelText: 'Xem lại'
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
      diaChiCuThe: form.diaChiCuThe ? form.diaChiCuThe.trim() : ''
    }

    if (selectedFile.value) {
      const formData = new FormData()
      formData.append(
        'data',
        new Blob([JSON.stringify(payload)], { type: 'application/json' })
      )
      formData.append('file', selectedFile.value)
      await api.post('/api/nhan-vien', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    } else {
      await api.post('/api/nhan-vien', payload)
    }

    showToast('Thêm mới nhân viên thành công! Thông tin tài khoản đã gửi tới email.', 'success')
    router.push('/nhan-vien')
  } catch (e) {
    showAlert({
      title: 'Không thể thêm nhân viên',
      message: e?.response?.data?.message || 'Có lỗi xảy ra trong quá trình lưu dữ liệu nhân viên.',
      type: 'error'
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadRoles()
  loadProvinces()
})
</script>

<style scoped>
.employee-page-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.page-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  width: 100%;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #ffffff;
  border: 1px solid #d2d6dc;
  padding: 0.45rem 1rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  background-color: #f3f4f6;
  border-color: #9ca3af;
  color: #111827;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-scan-cccd {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.48rem 1.15rem;
  background-color: #2e9f65;
  color: #ffffff;
  border: none;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(46, 159, 101, 0.25);
  transition: all 0.2s ease;
}

.btn-scan-cccd:hover {
  background-color: #248352;
  box-shadow: 0 3px 8px rgba(46, 159, 101, 0.35);
  transform: translateY(-1px);
}

.btn-scan-cccd svg {
  display: block;
}

.form-container-card {
  background: transparent;
  border: none;
  padding: 0;
  box-shadow: none;
  width: 100%;
  max-width: 100%;
  margin: 0;
}

.form-layout-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 1.5rem;
  align-items: start;
  width: 100%;
}

/* Avatar Panel */
.avatar-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 12px;
  padding: 1.6rem 1.25rem;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  text-align: center;
}

.card-subtitle {
  margin: 0 0 1.25rem;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
  border-bottom: 1px solid #f0eee6;
  padding-bottom: 0.75rem;
}

.avatar-integrated-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
}

.avatar-uploader-box {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  margin: 0 auto;
  background: #eaf1f5;
  border: 3px solid #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
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
  box-shadow: 0 6px 18px rgba(73, 104, 131, 0.2);
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
  color: #647b8c;
}

.avatar-placeholder-text {
  font-size: 0.9rem;
  font-weight: 700;
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
  font-size: 0.88rem;
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
  color: #c94a4a;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
}

.btn-remove-avatar-link:hover {
  background-color: #fde8e8;
}

/* Form Fields Panel */
.form-fields-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-card {
  border: 1px solid var(--line, #e9e5db);
  border-radius: 12px;
  padding: 1.5rem 1.85rem;
  background-color: #ffffff;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.section-title {
  margin: 0 0 1.25rem;
  font-size: 1.1rem;
  font-weight: 700;
  color: #2c3e50;
  border-bottom: 1px solid #f0eee6;
  padding-bottom: 0.75rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.4rem 1.8rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.form-field label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #414d55;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.required {
  color: #d9534f;
  font-weight: bold;
}

.tag-auto {
  font-size: 0.78rem;
  color: #647b8c;
  font-weight: normal;
}

.sub-hint {
  font-size: 0.78rem;
  color: #8c979d;
  font-weight: normal;
}

.form-field input,
.form-field select {
  height: 2.85rem;
  border: 1px solid var(--line, #dfdcd3);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem;
  color: #2b383e;
  background-color: #ffffff;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.form-field input.has-error,
.form-field select.has-error {
  border-color: #e53e3e !important;
  background-color: #fffaf9;
}

.form-field input.has-error:focus,
.form-field select.has-error:focus {
  box-shadow: 0 0 0 3px rgba(229, 62, 62, 0.15) !important;
}

.error-inline-msg {
  font-size: 0.82rem;
  color: #e53e3e;
  font-weight: 500;
  line-height: 1.25;
}

.input-disabled {
  background-color: #f1ede4 !important;
  color: #7b868b !important;
  cursor: not-allowed;
  font-weight: 600;
}

/* Radio Group */
.gender-radio-group {
  display: flex;
  align-items: center;
  gap: 2rem;
  height: 2.85rem;
}

.radio-label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #384952;
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

/* Info Notice Banner */
.info-notice-banner {
  background-color: #f0f5f9;
  border: 1px solid #d4e2ed;
  border-radius: 10px;
  padding: 1.1rem 1.4rem;
}

.notice-title {
  font-weight: 700;
  font-size: 0.92rem;
  color: #365067;
  margin-bottom: 0.35rem;
}

.notice-desc {
  font-size: 0.88rem;
  color: #4d667c;
  line-height: 1.45;
}

.form-actions-bar {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 12px;
  padding: 1rem 1.5rem;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  display: flex;
  justify-content: flex-end;
  gap: 0.85rem;
}

.btn {
  height: 2.65rem;
  padding: 0 1.6rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  border: 1px solid #d1d5db;
  background-color: #ffffff;
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
.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .form-layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-field.full-width {
    grid-column: span 1;
  }
}
</style>
