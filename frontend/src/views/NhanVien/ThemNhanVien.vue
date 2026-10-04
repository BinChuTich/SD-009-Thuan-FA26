<template>
  <div class="employee-page-wrapper">
    <!-- Header -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <button class="btn-back" @click="goBack">
          <span>←</span> Quay lại danh sách
        </button>
        <h2 class="page-title">Thêm mới nhân viên</h2>
      </div>
    </div>

    <!-- Form Layout -->
    <div class="form-container-card">
      <div class="form-layout-grid">
        <!-- Cột trái: Ảnh đại diện -->
        <div class="avatar-upload-panel">
          <div class="avatar-card">
            <h4 class="card-subtitle">Ảnh đại diện</h4>
            <div class="avatar-preview-box">
              <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview-img" alt="avatar" />
              <div v-else class="avatar-placeholder-text">{{ previewInitials }}</div>
            </div>
            <div class="avatar-actions">
              <label class="btn btn-upload">
                <span>📁</span> Chọn ảnh
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/jpg,image/webp"
                  @change="onImageSelected"
                  style="display: none"
                />
              </label>
              <button v-if="avatarPreview" class="btn btn-remove-avatar" @click="removeAvatar">
                Xóa ảnh
              </button>
            </div>
            <p class="avatar-hint">Định dạng: JPG, PNG, WEBP. Tối đa 5MB.</p>
          </div>
        </div>

        <!-- Cột phải: Thông tin chi tiết -->
        <div class="form-fields-panel">
          <!-- Phần 1: Thông tin cơ bản -->
          <div class="section-card">
            <h4 class="section-title">
              <span class="section-icon">👤</span> Thông tin cơ bản
            </h4>
            <div class="form-grid">
              <!-- Mã nhân viên (TỰ SINH - KHÓA) -->
              <div class="form-field">
                <label>Mã nhân viên <span class="tag-auto">(Tự sinh)</span></label>
                <input
                  v-model="form.maNhanVien"
                  type="text"
                  class="input-disabled"
                  disabled
                  placeholder="Hệ thống tự sinh mã..."
                />
              </div>

              <!-- Họ và tên -->
              <div class="form-field">
                <label>Họ và tên <span class="required">*</span></label>
                <input
                  v-model="form.tenNhanVien"
                  type="text"
                  placeholder="Nhập họ và tên nhân viên..."
                />
              </div>

              <!-- Số điện thoại -->
              <div class="form-field">
                <label>Số điện thoại <span class="required">*</span></label>
                <input
                  v-model="form.soDienThoai"
                  type="text"
                  placeholder="VD: 0987654321 (10-11 số)"
                  maxlength="11"
                />
              </div>

              <!-- Email -->
              <div class="form-field">
                <label>Email <span class="required">*</span></label>
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="example@gmail.com"
                />
              </div>

              <!-- Giới tính -->
              <div class="form-field">
                <label>Giới tính</label>
                <select v-model="form.gioiTinh">
                  <option :value="true">Nam</option>
                  <option :value="false">Nữ</option>
                </select>
              </div>

              <!-- Ngày sinh -->
              <div class="form-field">
                <label>Ngày sinh</label>
                <input
                  v-model="form.ngaySinh"
                  type="date"
                  :max="maxDate"
                />
              </div>

              <!-- Vai trò (Chỉ có 2 vai trò) -->
              <div class="form-field">
                <label>Vai trò <span class="required">*</span></label>
                <select v-model="form.idVaiTro">
                  <option v-for="r in roles" :key="r.id" :value="r.id">
                    {{ r.tenVaiTro }}
                  </option>
                </select>
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

          <!-- Phần 2: Tài khoản đăng nhập (Tùy chọn) -->
          <div class="section-card">
            <h4 class="section-title">
              <span class="section-icon">🔐</span> Tài khoản hệ thống
            </h4>
            <div class="form-grid">
              <div class="form-field">
                <label>Tên tài khoản <span class="tag-hint">(Mặc định theo mã nhân viên nếu để trống)</span></label>
                <input
                  v-model="form.tenTaiKhoan"
                  type="text"
                  :placeholder="'Mặc định: ' + (form.maNhanVien ? form.maNhanVien.toLowerCase() : 'nv...')"
                />
              </div>

              <div class="form-field">
                <label>Mật khẩu <span class="tag-hint">(Mặc định: 123456 nếu để trống)</span></label>
                <input
                  v-model="form.matKhau"
                  type="password"
                  placeholder="Nhập mật khẩu hoặc để trống"
                />
              </div>
            </div>
          </div>

          <!-- Phần 3: Thông tin địa chỉ -->
          <div class="section-card">
            <h4 class="section-title">
              <span class="section-icon">📍</span> Thông tin địa chỉ
            </h4>
            <div class="form-grid">
              <div class="form-field">
                <label>Tỉnh / Thành phố (Quê quán)</label>
                <input
                  v-model="form.queQuan"
                  type="text"
                  placeholder="VD: Hà Nội, Hải Phòng..."
                />
              </div>

              <div class="form-field">
                <label>Phường / Xã</label>
                <input
                  v-model="form.phuong"
                  type="text"
                  placeholder="VD: Phường Dịch Vọng..."
                />
              </div>

              <div class="form-field full-width">
                <label>Địa chỉ cụ thể</label>
                <input
                  v-model="form.diaChiCuThe"
                  type="text"
                  placeholder="Số nhà, ngõ, tên đường..."
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
              {{ saving ? 'Đang lưu...' : '💾 Thêm nhân viên' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'

const router = useRouter()
const saving = ref(false)
const roles = ref([])
const selectedFile = ref(null)
const avatarPreview = ref('')
const maxDate = new Date().toISOString().split('T')[0]

const form = reactive({
  maNhanVien: '',
  tenNhanVien: '',
  tenTaiKhoan: '',
  matKhau: '',
  email: '',
  soDienThoai: '',
  gioiTinh: true,
  ngaySinh: '',
  queQuan: '',
  phuong: '',
  diaChiCuThe: '',
  idVaiTro: null,
  trangThai: 1
})

const previewInitials = computed(() => {
  const name = form.tenNhanVien?.trim() || 'NV'
  const parts = name.split(/\s+/)
  return parts.length === 1 ? parts[0].slice(0, 2).toUpperCase() : (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
})

async function fetchNextCode() {
  try {
    const res = await api.get('/api/nhan-vien/next-code')
    if (res.data?.code) {
      form.maNhanVien = res.data.code
    }
  } catch (e) {
    console.error('Không thể lấy mã tự sinh:', e)
  }
}

async function loadRoles() {
  try {
    const res = await api.get('/api/nhan-vien/vai-tro')
    roles.value = res.data || []
    if (roles.value.length > 0) {
      // Ưu tiên chọn vai trò Nhân viên (NV)
      const nv = roles.value.find(r => r.maVaiTro === 'NV')
      form.idVaiTro = nv ? nv.id : roles.value[0].id
    }
  } catch (e) {
    showAlert({
      title: 'Lỗi tải vai trò',
      message: 'Không thể kết nối danh sách vai trò từ máy chủ.',
      type: 'error'
    })
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
  // 1. Validate họ tên
  if (!form.tenNhanVien || !form.tenNhanVien.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng nhập họ và tên nhân viên!',
      type: 'warning'
    })
  }

  // 2. Validate số điện thoại
  const phone = form.soDienThoai ? form.soDienThoai.trim() : ''
  if (!phone) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng nhập số điện thoại của nhân viên!',
      type: 'warning'
    })
  }
  if (!/^0\d{9,10}$/.test(phone)) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Số điện thoại phải từ 10 - 11 chữ số và bắt đầu bằng số 0!',
      type: 'warning'
    })
  }

  // 3. Validate email (bắt buộc)
  const email = form.email ? form.email.trim() : ''
  if (!email) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng nhập địa chỉ email của nhân viên!',
      type: 'warning'
    })
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Định dạng email không hợp lệ (VD: example@gmail.com)!',
      type: 'warning'
    })
  }

  // 4. Validate ngày sinh
  if (form.ngaySinh) {
    const bDate = new Date(form.ngaySinh)
    const today = new Date()
    today.setHours(23, 59, 59, 999)
    if (bDate > today) {
      return showAlert({
        title: 'Thông tin chưa hợp lệ',
        message: 'Ngày sinh không được lớn hơn ngày hiện tại!',
        type: 'warning'
      })
    }
  }

  // 5. Validate vai trò
  if (!form.idVaiTro) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng chọn vai trò cho nhân viên!',
      type: 'warning'
    })
  }

  // Xác nhận lưu thông qua popup ở CHÍNH GIỮA TRANG
  const confirmed = await showConfirm({
    title: 'Xác nhận tạo mới nhân viên',
    message: `Bạn có chắc chắn muốn thêm nhân viên "${form.tenNhanVien.trim()}" vào hệ thống không?`,
    type: 'question',
    confirmText: 'Xác nhận thêm',
    cancelText: 'Xem lại'
  })

  if (!confirmed) return

  saving.value = true
  try {
    const payload = {
      maNhanVien: form.maNhanVien,
      tenNhanVien: form.tenNhanVien.trim(),
      tenTaiKhoan: form.tenTaiKhoan?.trim() || undefined,
      matKhau: form.matKhau?.trim() || undefined,
      email: form.email?.trim() || undefined,
      soDienThoai: phone,
      gioiTinh: form.gioiTinh,
      ngaySinh: form.ngaySinh || undefined,
      queQuan: form.queQuan?.trim() || undefined,
      phuong: form.phuong?.trim() || undefined,
      diaChiCuThe: form.diaChiCuThe?.trim() || undefined,
      idVaiTro: form.idVaiTro,
      trangThai: form.trangThai
    }

    if (selectedFile.value) {
      const fd = new FormData()
      fd.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
      fd.append('file', selectedFile.value)
      await api.post('/api/nhan-vien', fd)
    } else {
      await api.post('/api/nhan-vien', payload)
    }

    showToast('Thêm mới nhân viên thành công!', 'success')
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

onMounted(async () => {
  await fetchNextCode()
  await loadRoles()
})
</script>

<style scoped>
.employee-page-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.breadcrumb-header {
  margin-bottom: 1.2rem;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-back {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #496883;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #eaf1f4;
  border-color: #496883;
}

.page-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--blue, #496883);
}

.form-container-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  padding: 1.6rem 1.8rem;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.form-layout-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 1.8rem;
  align-items: start;
}

/* Avatar Panel */
.avatar-card {
  background: #faf8f4;
  border: 1px dashed #d6d0c4;
  border-radius: 12px;
  padding: 1.5rem 1rem;
  text-align: center;
}

.card-subtitle {
  margin: 0 0 1rem;
  font-size: 1rem;
  color: #496883;
}

.avatar-preview-box {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  margin: 0 auto 1.2rem;
  background: #eaf1f5;
  border: 3px solid #ffffff;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  display: grid;
  place-items: center;
  overflow: hidden;
}

.avatar-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder-text {
  font-size: 2.2rem;
  font-weight: 800;
  color: #496883;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: center;
}

.btn-upload {
  background-color: #ffffff;
  border: 1px solid #d2a764;
  color: #926f34;
  padding: 0.45rem 1rem;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.btn-upload:hover {
  background-color: #fef8ee;
}

.btn-remove-avatar {
  background: transparent;
  border: none;
  color: #bf4545;
  font-size: 0.82rem;
  cursor: pointer;
  text-decoration: underline;
}

.avatar-hint {
  margin: 0.8rem 0 0;
  font-size: 0.78rem;
  color: #8c979c;
}

/* Form Fields Panel */
.form-fields-panel {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.section-card {
  border: 1px solid #ece8de;
  border-radius: 10px;
  padding: 1.25rem 1.4rem;
  background-color: #ffffff;
}

.section-title {
  margin: 0 0 1.2rem;
  font-size: 1.05rem;
  font-weight: 700;
  color: #384952;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.section-icon {
  font-size: 1.1rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.1rem 1.3rem;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.form-field.full-width {
  grid-column: span 2;
}

.form-field label {
  font-size: 0.88rem;
  font-weight: 600;
  color: #4f5f67;
  margin-bottom: 0.45rem;
}

.tag-auto {
  color: #496883;
  font-size: 0.8rem;
  font-weight: 500;
}

.tag-hint {
  color: #889398;
  font-size: 0.78rem;
  font-weight: 400;
}

.required {
  color: #cc3a3a;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 2.6rem;
  border: 1px solid #e2ddd3;
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem;
  color: #334249;
  background-color: #fdfcf9;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  border-color: #496883;
  background-color: #ffffff;
}

.input-disabled {
  background-color: #f1ede4 !important;
  color: #7b868b !important;
  cursor: not-allowed;
  font-weight: 600;
}

.form-actions-bar {
  display: flex;
  justify-content: flex-end;
  gap: 0.9rem;
  padding-top: 0.8rem;
}

.btn {
  height: 2.7rem;
  padding: 0 1.6rem;
  border-radius: 8px;
  font-size: 0.95rem;
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
  background-color: #496883;
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #38536b;
}

@media (max-width: 880px) {
  .form-layout-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-field.full-width {
    grid-column: span 1;
  }
}
</style>
