<template>
  <div class="customer-page-wrapper">
    <!-- Header -->
    <div class="page-top-header">
      <div class="header-left">
        <button class="btn-back" @click="goBack">
          <span>←</span> Quay lại danh sách
        </button>
        <h2 class="page-title">
          Thông tin khách hàng: {{ form.tenKhachHang || form.maKhachHang }}
        </h2>
      </div>
    </div>

    <div v-if="loading" class="content-card loading-card">
      <p>Đang tải thông tin khách hàng...</p>
    </div>

    <!-- Form Container -->
    <div v-else class="form-container-card">
      <div class="form-layout-grid">
        <!-- Cột trái: Ảnh đại diện -->
        <div class="avatar-upload-panel">
          <div class="avatar-card">
            <h4 class="card-subtitle">Ảnh đại diện</h4>
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
          <!-- Phần 1: Thông tin khách hàng -->
          <div class="section-card">
            <h4 class="section-title">Thông tin khách hàng</h4>
            <div class="form-grid">
              <!-- Mã khách hàng (KHÓA) -->
              <div class="form-field">
                <label>Mã khách hàng</label>
                <input
                  v-model="form.maKhachHang"
                  type="text"
                  class="input-disabled"
                  disabled
                />
              </div>

              <!-- Họ và tên khách hàng -->
              <div class="form-field">
                <label>Họ và tên khách hàng <span class="required">*</span></label>
                <input
                  v-model="form.tenKhachHang"
                  type="text"
                  placeholder="Nhập họ và tên khách hàng..."
                  :class="{ 'has-error': errors.tenKhachHang }"
                  @input="validateField('tenKhachHang')"
                />
                <span v-if="errors.tenKhachHang" class="error-inline-msg">{{ errors.tenKhachHang }}</span>
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

              <!-- Giới tính (Radio) -->
              <div class="form-field">
                <label>Giới tính</label>
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
                <label>Ngày sinh <span class="sub-hint">(Từ 15 tuổi trở lên)</span></label>
                <input
                  v-model="form.ngaySinh"
                  type="date"
                  :max="maxDate"
                  :class="{ 'has-error': errors.ngaySinh }"
                  @change="validateField('ngaySinh')"
                />
                <span v-if="errors.ngaySinh" class="error-inline-msg">{{ errors.ngaySinh }}</span>
              </div>

              <!-- Trạng thái -->
              <div class="form-field">
                <label>Trạng thái</label>
                <select v-model="form.trangThai">
                  <option :value="1">Hoạt động</option>
                  <option :value="0">Ngừng hoạt động</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Phần 2: Địa chỉ nhận hàng -->
          <div class="section-card">
            <h4 class="section-title">Địa chỉ nhận hàng (Mặc định)</h4>
            <div class="form-grid">
              <!-- Tỉnh / Thành phố -->
              <div class="form-field">
                <label>Tỉnh / Thành phố <span class="required">*</span></label>
                <select
                  v-model="selectedProvinceCode"
                  :class="{ 'has-error': errors.thanhPho }"
                  @change="onProvinceChange"
                >
                  <option value="">-- Chọn Tỉnh / Thành phố --</option>
                  <option v-for="p in provincesList" :key="p.code" :value="p.code">
                    {{ p.name }}
                  </option>
                </select>
                <span v-if="errors.thanhPho" class="error-inline-msg">{{ errors.thanhPho }}</span>
              </div>

              <!-- Phường / Xã -->
              <div class="form-field">
                <label>Phường / Xã <span class="required">*</span></label>
                <select
                  v-model="selectedWardCode"
                  :disabled="!selectedProvinceCode"
                  :class="{ 'has-error': errors.phuong }"
                  @change="onWardChange"
                >
                  <option value="">-- Chọn Phường / Xã --</option>
                  <option v-for="w in wardsList" :key="w.code" :value="w.code">
                    {{ w.name }}
                  </option>
                </select>
                <span v-if="errors.phuong" class="error-inline-msg">{{ errors.phuong }}</span>
              </div>

              <!-- Địa chỉ cụ thể -->
              <div class="form-field full-width">
                <label>Địa chỉ cụ thể <span class="required">*</span></label>
                <input
                  v-model="form.diaChiCuThe"
                  type="text"
                  placeholder="Số nhà, ngõ, ngách, tên đường..."
                  :class="{ 'has-error': errors.diaChiCuThe }"
                  @input="validateField('diaChiCuThe')"
                />
                <span v-if="errors.diaChiCuThe" class="error-inline-msg">{{ errors.diaChiCuThe }}</span>
              </div>
            </div>
          </div>

          <!-- Nút hành động -->
          <div class="form-actions-bar">
            <button class="btn btn-secondary" @click="goBack">
              Quay lại danh sách
            </button>
            <button class="btn btn-primary" :disabled="saving" @click="handleUpdate">
              {{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'
import { getProvinces, getWardsByProvince } from '@/services/provincesApi.js'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const loading = ref(true)
const saving = ref(false)
const selectedFile = ref(null)
const avatarPreview = ref('')
const maxDate = new Date().toISOString().split('T')[0]

// Danh mục hành chính (2 cấp: Tỉnh/Thành phố & Phường/Xã)
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
  trangThai: 1,
  anhKhachHang: '',
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

async function loadCustomer() {
  loading.value = true
  try {
    provincesList.value = await getProvinces()

    const res = await api.get(`/api/khach-hang/${id}`)
    const data = res.data
    form.maKhachHang = data.maKhachHang || ''
    form.tenKhachHang = data.tenKhachHang || ''
    form.email = data.email || ''
    form.soDienThoai = data.soDienThoai || data.soDienThoaiNhan || ''
    form.gioiTinh = data.gioiTinh ?? true
    form.ngaySinh = data.ngaySinh || ''
    form.trangThai = data.trangThai ?? 1
    form.anhKhachHang = data.anhKhachHang || ''
    form.thanhPho = data.thanhPho || ''
    form.huyen = data.huyen || ''
    form.phuong = data.phuong || ''
    form.diaChiCuThe = data.diaChiCuThe || ''

    if (data.anhKhachHang) {
      avatarPreview.value = data.anhKhachHang.startsWith('http')
        ? data.anhKhachHang
        : `http://localhost:8080${data.anhKhachHang}`
    }

    // Khớp danh mục hành chính 2 cấp từ địa chỉ đã lưu
    if (form.thanhPho) {
      const matchP = provincesList.value.find(p =>
        p.name.toLowerCase() === form.thanhPho.toLowerCase() ||
        p.name.toLowerCase().includes(form.thanhPho.toLowerCase()) ||
        form.thanhPho.toLowerCase().includes(p.name.toLowerCase())
      )
      if (matchP) {
        selectedProvinceCode.value = matchP.code
        wardsList.value = await getWardsByProvince(matchP.code)

        if (form.phuong) {
          const matchW = wardsList.value.find(w =>
            w.name.toLowerCase() === form.phuong.toLowerCase() ||
            w.name.toLowerCase().includes(form.phuong.toLowerCase()) ||
            form.phuong.toLowerCase().includes(w.name.toLowerCase())
          )
          if (matchW) {
            selectedWardCode.value = matchW.code
          }
        }
      }
    }
  } catch (e) {
    showAlert({
      title: 'Không tìm thấy khách hàng',
      message: e?.response?.data?.message || 'Không thể tải thông tin khách hàng từ hệ thống.',
      type: 'error'
    })
    router.push('/khach-hang')
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
  form.anhKhachHang = ''
}

function goBack() {
  router.push('/khach-hang')
}

async function handleUpdate() {
  if (!validateAll()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng kiểm tra và sửa các thông tin báo lỗi trên form!',
      type: 'warning'
    })
  }

  const confirmed = await showConfirm({
    title: 'Xác nhận cập nhật khách hàng',
    message: `Bạn có chắc chắn muốn lưu các thay đổi cho khách hàng "${form.tenKhachHang.trim()}" không?`,
    type: 'question',
    confirmText: 'Lưu thay đổi',
    cancelText: 'Hủy bỏ'
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
      trangThai: form.trangThai,
      anhKhachHang: form.anhKhachHang || undefined,
      thanhPho: form.thanhPho.trim(),
      huyen: form.huyen.trim(),
      phuong: form.phuong.trim(),
      diaChiCuThe: form.diaChiCuThe.trim()
    }

    if (selectedFile.value) {
      const formData = new FormData()
      formData.append(
        'data',
        new Blob([JSON.stringify(payload)], { type: 'application/json' })
      )
      formData.append('file', selectedFile.value)
      await api.put(`/api/khach-hang/${id}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    } else {
      await api.put(`/api/khach-hang/${id}`, payload)
    }

    showToast('Cập nhật thông tin khách hàng thành công!', 'success')
    router.push('/khach-hang')
  } catch (e) {
    showAlert({
      title: 'Không thể cập nhật khách hàng',
      message: e?.response?.data?.message || 'Có lỗi xảy ra trong quá trình cập nhật dữ liệu.',
      type: 'error'
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCustomer()
})
</script>

<style scoped>
.customer-page-wrapper {
  padding: 1.8rem 2.2rem 3.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.page-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  max-width: 1250px;
  margin-left: auto;
  margin-right: auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1.2rem;
}

.btn-back {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  padding: 0.55rem 1.1rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 600;
  color: #496883;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #eaf1f4;
  border-color: #496883;
}

.page-title {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 700;
  color: var(--blue, #496883);
}

.loading-card {
  padding: 3.5rem;
  text-align: center;
  color: #7d8b91;
  font-size: 1.05rem;
  max-width: 1250px;
  margin: 0 auto;
}

.form-container-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid var(--line, #e9e5db);
  padding: 2.2rem 2.5rem;
  box-shadow: 0 2px 8px rgba(65, 60, 50, 0.03);
  max-width: 1250px;
  margin: 0 auto;
}

.form-layout-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 2.5rem;
  align-items: start;
}

/* Avatar Panel */
.avatar-card {
  background: #faf8f4;
  border: 1px dashed #d6d0c4;
  border-radius: 12px;
  padding: 1.6rem 1rem;
  text-align: center;
}

.card-subtitle {
  margin: 0 0 1.2rem;
  font-size: 1rem;
  font-weight: 700;
  color: #496883;
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
  gap: 1.8rem;
}

.section-card {
  border: 1px solid #ece8de;
  border-radius: 12px;
  padding: 1.8rem 2rem;
  background-color: #ffffff;
}

.section-title {
  margin: 0 0 1.4rem;
  font-size: 1.15rem;
  font-weight: 700;
  color: #384952;
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

.form-actions-bar {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 0.8rem;
}

.btn {
  height: 2.85rem;
  padding: 0 1.8rem;
  border-radius: 8px;
  font-size: 0.98rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
  border: none;
}

.btn-secondary {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-secondary:hover {
  background-color: #faeed7;
}

.btn-primary {
  background-color: #b38e6e;
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #9c7756;
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
