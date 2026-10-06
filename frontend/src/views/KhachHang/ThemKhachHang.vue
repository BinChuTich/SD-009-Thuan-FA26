<template>
  <div class="customer-page-wrapper">
    <!-- Header -->
    <div class="breadcrumb-header">
      <div class="breadcrumb-left">
        <button class="btn-back" @click="goBack">
          <span>←</span> Quay lại danh sách
        </button>
        <h2 class="page-title">Thêm mới khách hàng</h2>
      </div>
    </div>

    <!-- Form Container -->
    <div class="form-container-card">
      <div class="form-layout-single">
        <!-- Thông tin khách hàng -->
        <div class="section-card">
          <h4 class="section-title">
            <span class="section-icon">👤</span> Thông tin khách hàng
          </h4>
          <div class="form-grid">
            <!-- Mã khách hàng (TỰ SINH - KHÓA) -->
            <div class="form-field">
              <label>Mã khách hàng <span class="tag-auto">(Tự sinh)</span></label>
              <input
                v-model="form.maKhachHang"
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
                v-model="form.tenKhachHang"
                type="text"
                placeholder="Nhập họ và tên khách hàng..."
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
              <label>Email</label>
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

        <!-- Thông tin địa chỉ -->
        <div class="section-card">
          <h4 class="section-title">
            <span class="section-icon">📍</span> Địa chỉ nhận hàng
          </h4>
          <div class="form-grid">
            <div class="form-field">
              <label>Tỉnh / Thành phố</label>
              <input
                v-model="form.thanhPho"
                type="text"
                placeholder="VD: Hà Nội, TP.HCM..."
              />
            </div>

            <div class="form-field">
              <label>Quận / Huyện</label>
              <input
                v-model="form.huyen"
                type="text"
                placeholder="VD: Cầu Giấy, Đống Đa..."
              />
            </div>

            <div class="form-field">
              <label>Phường / Xã</label>
              <input
                v-model="form.phuong"
                type="text"
                placeholder="VD: Dịch Vọng Hậu..."
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
            {{ saving ? 'Đang lưu...' : '💾 Thêm khách hàng' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'

const router = useRouter()
const saving = ref(false)
const maxDate = new Date().toISOString().split('T')[0]

const form = reactive({
  maKhachHang: '',
  tenKhachHang: '',
  email: '',
  soDienThoai: '',
  gioiTinh: true,
  ngaySinh: '',
  thanhPho: '',
  huyen: '',
  phuong: '',
  diaChiCuThe: '',
  trangThai: 1
})

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
  // 1. Validate họ tên
  if (!form.tenKhachHang || !form.tenKhachHang.trim()) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng nhập họ và tên khách hàng!',
      type: 'warning'
    })
  }

  // 2. Validate số điện thoại
  const phone = form.soDienThoai ? form.soDienThoai.trim() : ''
  if (!phone) {
    return showAlert({
      title: 'Thông tin chưa hợp lệ',
      message: 'Vui lòng nhập số điện thoại khách hàng!',
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

  // 3. Validate email (nếu có nhập)
  if (form.email && form.email.trim()) {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      return showAlert({
        title: 'Thông tin chưa hợp lệ',
        message: 'Định dạng email không hợp lệ (VD: example@gmail.com)!',
        type: 'warning'
      })
    }
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

  // Xác nhận lưu thông qua popup ở CHÍNH GIỮA TRANG
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
      email: form.email?.trim() || undefined,
      soDienThoai: phone,
      gioiTinh: form.gioiTinh,
      ngaySinh: form.ngaySinh || undefined,
      thanhPho: form.thanhPho?.trim() || undefined,
      huyen: form.huyen?.trim() || undefined,
      phuong: form.phuong?.trim() || undefined,
      diaChiCuThe: form.diaChiCuThe?.trim() || undefined,
      trangThai: form.trangThai
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
})
</script>

<style scoped>
.customer-page-wrapper {
  padding: 1.5rem 1.75rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.breadcrumb-header {
  width: 100%;
  max-width: 920px;
  margin-bottom: 1.25rem;
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
  width: 100%;
  max-width: 920px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid var(--line, #e9e5db);
  padding: 2rem 2.2rem;
  box-shadow: 0 4px 18px rgba(65, 60, 50, 0.04);
  margin: 0 auto;
}

.form-layout-single {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-card {
  border: 1px solid #ece8de;
  border-radius: 10px;
  padding: 1.3rem 1.5rem;
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

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-field.full-width {
    grid-column: span 1;
  }
}
</style>
