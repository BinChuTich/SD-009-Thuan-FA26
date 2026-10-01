<template>
  <div class="customer-page-wrapper">
    <!-- 1. Thanh Breadcrumb & Nút quay lại -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <button class="btn-back" @click="goBack" title="Quay lại">
          ←
        </button>
        <span class="breadcrumb-text">
          Khách hàng <span class="slash">/</span> <b>Thêm khách hàng</b>
        </span>
      </div>
    </div>

    <!-- 2. Bố cục chính 2 cột: Avatar bên trái & Form bên phải -->
    <div class="customer-layout-grid">
      <!-- Cột trái: Card Avatar khách hàng -->
      <div class="content-card avatar-card">
        <div class="avatar-circle-wrapper" @click="triggerUploadAvatar">
          <div class="avatar-circle">
            <span class="avatar-initials">{{ avatarInitials }}</span>
          </div>
          <input
              type="file"
              ref="fileInputRef"
              class="hidden-file-input"
              accept="image/*"
              @change="onAvatarChange"
          />
        </div>

        <h3 class="customer-preview-name">{{ form.hoTen || 'Chưa đặt tên' }}</h3>
        <span class="customer-preview-sub">{{ form.email ? form.email.split('@')[0] : 'username' }}</span>
        <p class="avatar-hint">(Bấm vào ảnh để chọn avatar)</p>
      </div>

      <!-- Cột phải: Khối form thông tin -->
      <div class="form-right-column">
        <!-- Khối 1: Thông tin cơ bản -->
        <div class="content-card form-section-card">
          <div class="section-header">
            <div class="section-icon-box blue-soft-bg">
              <span class="section-icon">👤</span>
            </div>
            <div class="section-title-wrap">
              <h4 class="section-title">Thông tin cơ bản</h4>
              <p class="section-subtitle">Họ tên, email, liên hệ và tài khoản.</p>
            </div>
          </div>

          <div class="form-grid-2">
            <!-- Họ và tên -->
            <div class="form-field">
              <label>Họ và tên <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="form.hoTen"
                  placeholder="Nhập họ và tên..."
              />
            </div>

            <!-- Email -->
            <div class="form-field">
              <label>Email <span class="required">*</span></label>
              <input
                  type="email"
                  v-model="form.email"
                  placeholder="Nhập địa chỉ email..."
              />
            </div>

            <!-- Số điện thoại -->
            <div class="form-field">
              <label>Số điện thoại</label>
              <input
                  type="text"
                  v-model="form.soDienThoai"
                  placeholder="VD: 0901234567"
              />
            </div>

            <!-- Giới tính -->
            <div class="form-field">
              <label>Giới tính</label>
              <select v-model="form.gioiTinh">
                <option value="">-- Chọn giới tính --</option>
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
                <option value="Khác">Khác</option>
              </select>
            </div>

            <!-- Ngày sinh -->
            <div class="form-field full-width">
              <label>Ngày sinh</label>
              <div class="date-input-wrap">
                <input
                    type="date"
                    v-model="form.ngaySinh"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Khối 2: Địa chỉ giao hàng -->
        <div class="content-card form-section-card">
          <div class="section-header">
            <div class="section-icon-box gold-soft-bg">
              <span class="section-icon">📍</span>
            </div>
            <div class="section-title-wrap">
              <h4 class="section-title">Địa chỉ giao hàng <span class="required">*</span></h4>
              <p class="section-subtitle">Địa chỉ mặc định của khách hàng mới.</p>
            </div>
          </div>

          <div class="form-grid-2">
            <!-- Họ tên người nhận -->
            <div class="form-field">
              <label>Họ tên người nhận <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="form.nguoiNhan"
                  placeholder="Họ tên người nhận hàng"
              />
            </div>

            <!-- Số điện thoại nhận -->
            <div class="form-field">
              <label>Số điện thoại <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="form.soDienThoaiNhan"
                  placeholder="VD: 0901234567"
              />
            </div>

            <!-- Tỉnh / Thành phố -->
            <div class="form-field">
              <label>Tỉnh/Thành phố <span class="required">*</span></label>
              <select v-model="form.tinhThanh">
                <option value="">-- Chọn tỉnh/thành --</option>
                <option value="Hà Nội">Hà Nội</option>
                <option value="TP. Hồ Chí Minh">TP. Hồ Chí Minh</option>
                <option value="Đà Nẵng">Đà Nẵng</option>
                <option value="Hải Phòng">Hải Phòng</option>
              </select>
            </div>

            <!-- Phường / Xã -->
            <div class="form-field">
              <label>Phường/Xã <span class="required">*</span></label>
              <select v-model="form.phuongXa">
                <option value="">-- Chọn phường/xã --</option>
                <option value="Phường Cầu Giấy">Phường Cầu Giấy</option>
                <option value="Phường Bến Nghé">Phường Bến Nghé</option>
                <option value="Phường Dịch Vọng">Phường Dịch Vọng</option>
              </select>
            </div>

            <!-- Địa chỉ cụ thể -->
            <div class="form-field full-width">
              <label>Địa chỉ cụ thể <span class="required">*</span></label>
              <input
                  type="text"
                  v-model="form.diaChiCuThe"
                  placeholder="Số nhà, tên đường..."
              />
            </div>
          </div>

          <!-- Nút Tạo khách hàng góc dưới bên trái -->
          <div class="form-submit-row">
            <button class="btn btn-primary" @click="submitForm">
              <span class="btn-icon">💾</span> Tạo khách hàng
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const fileInputRef = ref(null)

const form = ref({
  hoTen: 'Nguyễn Miu',
  email: 'miu@gmail.com',
  soDienThoai: '',
  gioiTinh: '',
  ngaySinh: '',
  nguoiNhan: 'tùng',
  soDienThoaiNhan: '',
  tinhThanh: '',
  phuongXa: '',
  diaChiCuThe: ''
})

// Tự động tạo chữ viết tắt Avatar (VD: "Trần Vũ Tùng Anh" -> "TA")
const avatarInitials = computed(() => {
  if (!form.value.hoTen) return 'FF'
  const words = form.value.hoTen.trim().split(' ')
  if (words.length === 1) return words[0].substring(0, 2).toUpperCase()
  return (words[0][0] + words[words.length - 1][0]).toUpperCase()
})

const goBack = () => {
  router.back()
}

const triggerUploadAvatar = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const onAvatarChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    alert(`Đã chọn ảnh: ${file.name}`)
  }
}

const submitForm = () => {
  if (!form.value.hoTen || !form.value.email) {
    alert('Vui lòng điền các thông tin bắt buộc (*)!')
    return
  }
  alert(`Tạo khách hàng [${form.value.hoTen}] thành công!`)
}
</script>

<style scoped>
/* Toàn trang nền kem ấm theo hệ thống màu FF T-shirt */
.customer-page-wrapper {
  padding: 1.25rem 1.75rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* 1. Header Breadcrumb */
.breadcrumb-header {
  margin-bottom: 1.2rem;
}

.breadcrumb-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.95rem; /* ~15.2px */
}

.btn-back {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  background-color: #ffffff;
  color: var(--blue, #496883);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 1.1rem;
  font-weight: 700;
  transition: all 0.2s;
}

.btn-back:hover {
  background-color: #eaf1f4;
  border-color: var(--blue, #496883);
}

.breadcrumb-text {
  color: #8c9597;
}

.breadcrumb-text b {
  color: var(--blue, #496883);
  font-weight: 700;
}

.slash {
  margin: 0 5px;
  color: #d8d4c9;
}

/* 2. Lưới bố cục 2 cột */
.customer-layout-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 1.25rem;
  align-items: start;
}

/* Thẻ Card chung */
.content-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem;
}

/* Cột trái: Card Avatar */
.avatar-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2.5rem 1.5rem;
}

.avatar-circle-wrapper {
  position: relative;
  cursor: pointer;
  margin-bottom: 1.2rem;
}

.avatar-circle {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background-color: #eaf1f5;
  border: 2px dashed #b9cddc;
  display: grid;
  place-items: center;
  transition: all 0.2s;
}

.avatar-circle:hover {
  border-color: var(--blue, #496883);
  transform: scale(1.03);
}

.avatar-initials {
  font-size: 2.2rem; /* Cũ: 26px -> To rõ nét */
  font-weight: 800;
  color: var(--blue, #496883);
  letter-spacing: 1px;
}

.hidden-file-input {
  display: none;
}

.customer-preview-name {
  font-size: 1.15rem; /* ~18.4px */
  font-weight: 700;
  color: #3b4b53;
  margin: 0 0 4px 0;
}

.customer-preview-sub {
  font-size: 0.9rem;
  color: #8c9597;
  margin-bottom: 0.75rem;
}

.avatar-hint {
  font-size: 0.85rem;
  color: #9aa0a0;
  margin: 0;
}

/* Cột phải: Form chia các khối */
.form-right-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-section-card {
  padding: 1.5rem 1.75rem;
}

/* Tiêu đề từng khối */
.section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.4rem;
}

.section-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: grid;
  place-items: center;
}

.blue-soft-bg {
  background-color: #eaf1f5;
  color: var(--blue, #496883);
}

.gold-soft-bg {
  background-color: #f7eee1;
  color: #b18b52;
}

.section-icon {
  font-size: 1.2rem;
}

.section-title-wrap {
  display: flex;
  flex-direction: column;
}

.section-title {
  font-size: 1.05rem; /* ~16.8px */
  font-weight: 700;
  margin: 0;
  color: #3e4e56;
}

.section-subtitle {
  font-size: 0.85rem;
  color: #8c9597;
  margin: 2px 0 0 0;
}

/* Lưới ô nhập form 2 cột */
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.1rem 1.35rem;
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
  font-size: 0.92rem; /* Cũ: 11.5px -> Nâng lên ~14.7px */
  font-weight: 700;
  color: #4f5d63;
}

.required {
  color: #e04f4f;
  margin-left: 2px;
}

/* Các ô input và select to rộng, dễ bấm */
.form-field input,
.form-field select {
  height: 2.6rem; /* ~41.6px, gõ chữ thoải mái */
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem; /* Cũ: 12.5px -> Nâng lên ~15.2px */
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.08);
}

.date-input-wrap {
  width: 100%;
}

.date-input-wrap input {
  width: 100%;
}

/* Nút Submit */
.form-submit-row {
  margin-top: 1.5rem;
  padding-top: 1.2rem;
  border-top: 1px dashed #efeae0;
}

.btn {
  height: 2.6rem;
  padding: 0 1.5rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: none;
  transition: all 0.2s;
}

/* Nút Tạo khách hàng màu xanh đá thương hiệu FF */
.btn-primary {
  background-color: var(--blue, #496883);
  color: #ffffff;
}

.btn-primary:hover {
  background-color: #38536b;
  transform: translateY(-1px);
}

/* Responsive */
@media (max-width: 950px) {
  .customer-layout-grid {
    grid-template-columns: 1fr;
  }
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
  .form-field.full-width {
    grid-column: span 1;
  }
}
</style>