<template>
  <div class="customer-page-wrapper">
    <!-- Header -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
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
                  <div class="avatar-placeholder-icon">📷</div>
                  <span class="avatar-placeholder-text">Chọn ảnh</span>
                </div>
                <div class="avatar-hover-overlay">
                  <span>📷 Đổi ảnh</span>
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
            <h4 class="section-title">
              <span class="section-icon">👤</span> Thông tin khách hàng
            </h4>
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
            <h4 class="section-title">
              <span class="section-icon">📍</span> Địa chỉ nhận hàng
            </h4>
            <div class="form-grid">
              <!-- Họ tên người nhận -->
              <div class="form-field">
                <label>Họ tên người nhận <span class="required">*</span></label>
                <input
                  v-model="form.nguoiNhan"
                  type="text"
                  placeholder="Nhập họ tên người nhận..."
                />
              </div>

              <!-- Số điện thoại người nhận -->
              <div class="form-field">
                <label>Số điện thoại người nhận <span class="required">*</span></label>
                <input
                  v-model="form.soDienThoaiNhan"
                  type="text"
                  placeholder="VD: 0987654321 (10 - 11 số)"
                />
              </div>

              <!-- Tỉnh / Thành phố -->
              <div class="form-field">
                <label>Tỉnh / Thành phố <span class="required">*</span></label>
                <input
                  v-model="form.thanhPho"
                  type="text"
                  placeholder="VD: Hà Nội, TP.HCM..."
                />
              </div>

              <!-- Quận / Huyện -->
              <div class="form-field">
                <label>Quận / Huyện <span class="required">*</span></label>
                <input
                  v-model="form.huyen"
                  type="text"
                  placeholder="VD: Cầu Giấy, Đống Đa..."
                />
              </div>

              <!-- Phường / Xã -->
              <div class="form-field">
                <label>Phường / Xã <span class="required">*</span></label>
                <input
                  v-model="form.phuong"
                  type="text"
                  placeholder="VD: Dịch Vọng Hậu..."
                />
              </div>

              <!-- Địa chỉ cụ thể -->
              <div class="form-field full-width">
                <label>Địa chỉ cụ thể <span class="required">*</span></label>
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
              Quay lại danh sách
            </button>
            <button class="btn btn-primary" :disabled="saving" @click="handleUpdate">
              {{ saving ? 'Đang lưu...' : '💾 Lưu thay đổi' }}
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

const route = useRoute()
const router = useRouter()
const id = route.params.id

const loading = ref(true)
const saving = ref(false)
const selectedFile = ref(null)
const avatarPreview = ref('')
const maxDate = new Date().toISOString().split('T')[0]

const form = reactive({
  maKhachHang: '',
  tenKhachHang: '',
  email: '',
  gioiTinh: true,
  ngaySinh: '',
  trangThai: 1,
  anhKhachHang: '',
  // Địa chỉ nhận hàng
  nguoiNhan: '',
  soDienThoaiNhan: '',
  thanhPho: '',
  huyen: '',
  phuong: '',
  diaChiCuThe: ''
})

async function loadCustomer() {
  loading.value = true
  try {
    const res = await api.get(`/api/khach-hang/${id}`)
    const data = res.data
    form.maKhachHang = data.maKhachHang || ''
    form.tenKhachHang = data.tenKhachHang || ''
    form.email = data.email || ''
    form.gioiTinh = data.gioiTinh ?? true
    form.ngaySinh = data.ngaySinh || ''
    form.trangThai = data.trangThai ?? 1
    form.anhKhachHang = data.anhKhachHang || ''
    // Địa chỉ nhận hàng & Người nhận
    form.nguoiNhan = data.nguoiNhan || data.tenKhachHang || ''
    form.soDienThoaiNhan = data.soDienThoaiNhan || data.soDienThoai || ''
    form.thanhPho = data.thanhPho || ''
    form.huyen = data.huyen || ''
    form.phuong = data.phuong || ''
    form.diaChiCuThe = data.diaChiCuThe || ''

    if (data.anhKhachHang) {
      avatarPreview.value = data.anhKhachHang.startsWith('http')
        ? data.anhKhachHang
        : `http://localhost:8080${data.anhKhachHang}`
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
  // 1. Validate thông tin khách hàng
  if (!form.tenKhachHang || !form.tenKhachHang.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Họ và tên khách hàng không được để trống!',
      type: 'warning'
    })
  }

  const email = form.email ? form.email.trim() : ''
  if (!email) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Email khách hàng không được để trống!',
      type: 'warning'
    })
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Định dạng email khách hàng không hợp lệ (VD: example@gmail.com)!',
      type: 'warning'
    })
  }

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

  // 2. Validate toàn bộ địa chỉ nhận hàng
  if (!form.nguoiNhan || !form.nguoiNhan.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Họ tên người nhận không được để trống!',
      type: 'warning'
    })
  }

  const phone = form.soDienThoaiNhan ? form.soDienThoaiNhan.trim() : ''
  if (!phone) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Số điện thoại người nhận không được để trống!',
      type: 'warning'
    })
  }
  if (!/^\d+$/.test(phone)) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Số điện thoại chỉ được chứa các chữ số!',
      type: 'warning'
    })
  }
  if (!phone.startsWith('0')) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Số điện thoại phải bắt đầu bằng số 0!',
      type: 'warning'
    })
  }
  if (phone.length < 10) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Số điện thoại không được dưới 10 số (hiện tại có ' + phone.length + ' số)!',
      type: 'warning'
    })
  }
  if (phone.length > 11) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Số điện thoại không được trên 11 số (hiện tại có ' + phone.length + ' số)!',
      type: 'warning'
    })
  }

  if (!form.thanhPho || !form.thanhPho.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Tỉnh / Thành phố nhận hàng không được để trống!',
      type: 'warning'
    })
  }

  if (!form.huyen || !form.huyen.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Quận / Huyện nhận hàng không được để trống!',
      type: 'warning'
    })
  }

  if (!form.phuong || !form.phuong.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Phường / Xã nhận hàng không được để trống!',
      type: 'warning'
    })
  }

  if (!form.diaChiCuThe || !form.diaChiCuThe.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Địa chỉ cụ thể nhận hàng không được để trống!',
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
      email: email,
      gioiTinh: form.gioiTinh,
      ngaySinh: form.ngaySinh || undefined,
      trangThai: form.trangThai,
      anhKhachHang: form.anhKhachHang || undefined,
      nguoiNhan: form.nguoiNhan.trim(),
      soDienThoaiNhan: phone,
      soDienThoai: phone,
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
  padding: 1.5rem 1.75rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.breadcrumb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
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

.loading-card {
  padding: 3rem;
  text-align: center;
  color: #7d8b91;
  font-size: 1.05rem;
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
  margin: 0 0 1.1rem;
  font-size: 1rem;
  font-weight: 700;
  color: #496883;
}

.avatar-integrated-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.7rem;
}

.avatar-uploader-box {
  width: 136px;
  height: 136px;
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
  gap: 0.35rem;
  color: #647b8c;
}

.avatar-placeholder-icon {
  font-size: 2rem;
  line-height: 1;
}

.avatar-placeholder-text {
  font-size: 0.85rem;
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
  color: #c94a4a;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  transition: background 0.15s;
}

.btn-remove-avatar-link:hover {
  background-color: #fde8e8;
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
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem 1.4rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field.full-width {
  grid-column: span 2;
}

.form-field label {
  font-size: 0.88rem;
  font-weight: 600;
  color: #414d55;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.required {
  color: #d9534f;
  font-weight: bold;
}

.form-field input,
.form-field select {
  height: 2.6rem;
  border: 1px solid var(--line, #dfdcd3);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
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
  padding-top: 0.5rem;
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
