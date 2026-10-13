<template>
  <div class="customer-page-wrapper">
    <!-- 1. Thanh tiêu đề trên cùng & Nút quay lại -->
    <div class="page-top-header">
      <div class="page-breadcrumb">
        <router-link to="/khach-hang" class="crumb-parent">Quản lý khách hàng</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Thêm mới khách hàng</span>
      </div>
      <button class="btn-back-pill" @click="goBack" title="Quay lại danh sách khách hàng">
        ← Quay lại danh sách
      </button>
    </div>

    <!-- 2. Form nội dung: Bố cục dạng phẳng các Card xếp dọc đồng bộ hệ thống -->
    <form @submit.prevent="handleSubmit" class="form-content-container">
      <!-- KHỐI 1: THÔNG TIN KHÁCH HÀNG (CARD 1) -->
      <div class="content-card form-section-card">
        <div class="card-section-header">
          <h3 class="section-title">Thông tin khách hàng</h3>
        </div>

        <div class="section-body">
          <!-- Hàng 1 (2 Cột): Mã khách hàng & Họ và tên -->
          <div class="form-grid-2">
            <!-- Mã khách hàng (TỰ SINH - KHÓA) -->
            <div class="form-field">
              <label class="field-label">
                Mã khách hàng
                <span class="badge-lock">Tự sinh</span>
              </label>
              <input
                v-model="form.maKhachHang"
                type="text"
                class="form-control input-disabled"
                disabled
                placeholder="Hệ thống tự sinh mã..."
              />
            </div>

            <!-- Họ và tên khách hàng -->
            <div class="form-field">
              <label class="field-label">Họ và tên khách hàng <span class="required">*</span></label>
              <input
                v-model="form.tenKhachHang"
                type="text"
                placeholder="Nhập họ và tên khách hàng..."
                class="form-control"
                :class="{ 'input-error': errors.tenKhachHang }"
                @input="validateField('tenKhachHang')"
              />
              <span v-if="errors.tenKhachHang" class="field-error-msg">{{ errors.tenKhachHang }}</span>
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
                @input="validateField('soDienThoai')"
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
                @input="validateField('email')"
              />
              <span v-if="errors.email" class="field-error-msg">{{ errors.email }}</span>
            </div>
          </div>

          <!-- Hàng 3 (2 Cột): Ngày sinh & Giới tính -->
          <div class="form-grid-2">
            <div class="form-field">
              <label class="field-label">
                Ngày sinh
                <span class="sub-hint">(Từ 15 tuổi trở lên)</span>
              </label>
              <input
                v-model="form.ngaySinh"
                type="date"
                :max="maxDate"
                class="form-control"
                :class="{ 'input-error': errors.ngaySinh }"
                @change="validateField('ngaySinh')"
              />
              <span v-if="errors.ngaySinh" class="field-error-msg">{{ errors.ngaySinh }}</span>
            </div>

            <div class="form-field">
              <label class="field-label">Giới tính</label>
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

      <!-- KHỐI 2: ĐỊA CHỈ NHẬN HÀNG (CARD 2) -->
      <div class="content-card form-section-card">
        <div class="card-section-header">
          <h3 class="section-title">Địa chỉ nhận hàng</h3>
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
                :has-error="!!errors.thanhPho"
                @change="onProvinceChange"
              />
              <span v-if="errors.thanhPho" class="field-error-msg">{{ errors.thanhPho }}</span>
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
            <label class="field-label">Địa chỉ cụ thể <span class="required">*</span></label>
            <input
              v-model="form.diaChiCuThe"
              type="text"
              placeholder="Số nhà, ngõ, ngách, tên đường..."
              class="form-control"
              :class="{ 'input-error': errors.diaChiCuThe }"
              @input="validateField('diaChiCuThe')"
            />
            <span v-if="errors.diaChiCuThe" class="field-error-msg">{{ errors.diaChiCuThe }}</span>
          </div>
        </div>
      </div>

      <!-- KHỐI 3: NÚT HÀNH ĐỘNG -->
      <div class="form-actions-bar">
        <button type="button" class="btn btn-secondary" @click="goBack">
          Hủy bỏ
        </button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'Đang lưu...' : 'Thêm khách hàng' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'
import { getProvinces, getWardsByProvince } from '@/services/provincesApi.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()
const saving = ref(false)
const maxDate = new Date().toISOString().split('T')[0]

// Danh mục hành chính (2 cấp)
const provincesList = ref([])
const wardsList = ref([])

const selectedProvinceCode = ref('')
const selectedWardCode = ref('')

const form = reactive({
  maKhachHang: '',
  tenKhachHang: '',
  email: '',
  soDienThoai: '',
  gioiTinh: true,
  ngaySinh: '',
  trangThai: 1, // Mặc định hoạt động, không cần chọn trên form
  thanhPho: '',
  huyen: '',
  phuong: '',
  diaChiCuThe: ''
})

const errors = reactive({
  tenKhachHang: '',
  email: '',
  soDienThoai: '',
  ngaySinh: '',
  thanhPho: '',
  phuong: '',
  diaChiCuThe: ''
})

async function loadProvinces() {
  provincesList.value = await getProvinces()
}

async function onProvinceChange() {
  const p = provincesList.value.find(item => item.code === selectedProvinceCode.value)
  form.thanhPho = p ? p.name : ''
  selectedWardCode.value = ''
  form.huyen = ''
  form.phuong = ''
  wardsList.value = []

  validateField('thanhPho')

  if (selectedProvinceCode.value) {
    wardsList.value = await getWardsByProvince(selectedProvinceCode.value)
  }
}

function onWardChange() {
  const w = wardsList.value.find(item => item.code === selectedWardCode.value)
  form.phuong = w ? w.name : ''
  if (w && w.districtName) {
    form.huyen = w.districtName
  }
  validateField('phuong')
}

function validateField(fieldName) {
  if (fieldName === 'tenKhachHang') {
    if (!form.tenKhachHang || !form.tenKhachHang.trim()) {
      errors.tenKhachHang = 'Họ và tên khách hàng không được để trống!'
    } else {
      errors.tenKhachHang = ''
    }
  }

  if (fieldName === 'email') {
    const val = form.email ? form.email.trim() : ''
    if (!val) {
      errors.email = 'Email khách hàng không được để trống!'
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
    if (form.ngaySinh) {
      const bDate = new Date(form.ngaySinh)
      const today = new Date()
      if (bDate > today) {
        errors.ngaySinh = 'Ngày sinh không được lớn hơn ngày hiện tại!'
      } else {
        // Kiểm tra đủ 15 tuổi
        let age = today.getFullYear() - bDate.getFullYear()
        const m = today.getMonth() - bDate.getMonth()
        if (m < 0 || (m === 0 && today.getDate() < bDate.getDate())) {
          age--
        }
        if (age < 15) {
          errors.ngaySinh = 'Khách hàng phải từ 15 tuổi trở lên!'
        } else {
          errors.ngaySinh = ''
        }
      }
    } else {
      errors.ngaySinh = ''
    }
  }

  if (fieldName === 'thanhPho') {
    if (!form.thanhPho || !form.thanhPho.trim()) {
      errors.thanhPho = 'Vui lòng chọn Tỉnh / Thành phố!'
    } else {
      errors.thanhPho = ''
    }
  }

  if (fieldName === 'phuong') {
    if (!form.phuong || !form.phuong.trim()) {
      errors.phuong = 'Vui lòng chọn Phường / Xã!'
    } else {
      errors.phuong = ''
    }
  }

  if (fieldName === 'diaChiCuThe') {
    if (!form.diaChiCuThe || !form.diaChiCuThe.trim()) {
      errors.diaChiCuThe = 'Địa chỉ cụ thể không được để trống!'
    } else {
      errors.diaChiCuThe = ''
    }
  }
}

function validateAll() {
  validateField('tenKhachHang')
  validateField('email')
  validateField('soDienThoai')
  validateField('ngaySinh')
  validateField('thanhPho')
  validateField('phuong')
  validateField('diaChiCuThe')

  return !Object.values(errors).some(msg => msg !== '')
}

async function fetchNextCode() {
  try {
    const res = await api.get('/api/khach-hang/next-code')
    if (res.data?.code) {
      form.maKhachHang = res.data.code
    }
  } catch (e) {
    console.error('Không thể lấy mã khách hàng tự sinh:', e)
  }
}

function goBack() {
  router.push('/khach-hang')
}

async function handleSubmit() {
  if (!validateAll()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng kiểm tra và hoàn thành các thông tin báo lỗi trên form!',
      type: 'warning'
    })
  }

  const confirmed = await showConfirm({
    title: 'Xác nhận tạo mới khách hàng',
    message: `Bạn có chắc chắn muốn thêm khách hàng "${form.tenKhachHang.trim()}" vào hệ thống không?`,
    type: 'question',
    confirmText: 'Xác nhận thêm',
    cancelText: 'Xem lại'
  })

  if (!confirmed) return

  saving.value = true
  try {
    const payload = {
      maKhachHang: form.maKhachHang,
      tenKhachHang: form.tenKhachHang.trim(),
      email: form.email.trim(),
      soDienThoai: form.soDienThoai.trim(),
      gioiTinh: form.gioiTinh,
      ngaySinh: form.ngaySinh || undefined,
      trangThai: 1,
      thanhPho: form.thanhPho.trim(),
      huyen: form.huyen.trim(),
      phuong: form.phuong.trim(),
      diaChiCuThe: form.diaChiCuThe.trim()
    }

    await api.post('/api/khach-hang', payload)

    showToast('Thêm mới khách hàng thành công!', 'success')
    router.push('/khach-hang')
  } catch (e) {
    showAlert({
      title: 'Không thể thêm khách hàng',
      message: e?.response?.data?.message || 'Có lỗi xảy ra trong quá trình lưu dữ liệu khách hàng.',
      type: 'error'
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchNextCode()
  loadProvinces()
})
</script>

<style scoped>
/* Khung tổng thể bao ngoài căn lề chuẩn hệ thống */
.customer-page-wrapper {
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

/* 2. Form nội dung: Bố cục dạng phẳng các Card xếp dọc */
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
  background-color: #3b596a;
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #2c4350;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
