<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { cartCount } from '@/utils/luuTruGioHang.js'
import { currentClientUser, isClientLoggedIn, loginClient } from '@/utils/taiKhoanKhachHang.js'
import { showAppToast } from '@/utils/thongBaoDialog.js'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const form = reactive({
  tenKhachHang: '', email: '', soDienThoai: '', ngaySinh: '', gioiTinh: true,
  maKhachHang: '', trangThai: 1, thanhPho: '', huyen: '', phuong: '', diaChiCuThe: ''
})
const errors = reactive({ tenKhachHang: '', email: '', soDienThoai: '' })

const userId = computed(() => currentClientUser.value?.id || currentClientUser.value?.customerId)
const initials = computed(() => {
  const name = form.tenKhachHang || currentClientUser.value?.name || 'Khách hàng'
  return name.trim().split(/\s+/).slice(-2).map(part => part[0]).join('').toUpperCase()
})

const fillForm = (data = {}) => {
  form.tenKhachHang = data.tenKhachHang || data.name || ''
  form.email = data.email || ''
  form.soDienThoai = data.soDienThoai || data.phone || ''
  form.ngaySinh = data.ngaySinh || ''
  form.gioiTinh = data.gioiTinh ?? true
  form.maKhachHang = data.maKhachHang || ''
  form.trangThai = data.trangThai ?? 1
  form.thanhPho = data.thanhPho || ''
  form.huyen = data.huyen || ''
  form.phuong = data.phuong || ''
  form.diaChiCuThe = data.diaChiCuThe || ''
}

const loadProfile = async () => {
  if (!isClientLoggedIn.value) {
    loading.value = false
    return
  }
  fillForm(currentClientUser.value)
  if (!userId.value) {
    loading.value = false
    return
  }
  try {
    const res = await api.get(`/api/khach-hang/${userId.value}`)
    fillForm(res.data || currentClientUser.value)
  } catch (error) {
    console.warn('Không tải được hồ sơ mới nhất:', error)
  } finally {
    loading.value = false
  }
}

const validate = () => {
  errors.tenKhachHang = form.tenKhachHang.trim() ? '' : 'Vui lòng nhập họ và tên.'
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim()) ? '' : 'Email chưa đúng định dạng.'
  errors.soDienThoai = /^(0|\+84)[0-9]{9}$/.test(form.soDienThoai.trim()) ? '' : 'Số điện thoại phải có 10 chữ số.'
  return !errors.tenKhachHang && !errors.email && !errors.soDienThoai
}

const saveProfile = async () => {
  if (!validate() || !userId.value) return
  saving.value = true
  try {
    const payload = {
      maKhachHang: form.maKhachHang,
      tenKhachHang: form.tenKhachHang.trim(),
      email: form.email.trim(),
      soDienThoai: form.soDienThoai.trim(),
      ngaySinh: form.ngaySinh || null,
      gioiTinh: form.gioiTinh,
      trangThai: form.trangThai,
      thanhPho: form.thanhPho,
      huyen: form.huyen,
      phuong: form.phuong,
      diaChiCuThe: form.diaChiCuThe
    }
    const res = await api.put(`/api/khach-hang/${userId.value}`, payload)
    const saved = res.data || payload
    loginClient({
      ...currentClientUser.value,
      ...saved,
      id: userId.value,
      name: saved.tenKhachHang || payload.tenKhachHang,
      phone: saved.soDienThoai || payload.soDienThoai,
      email: saved.email || payload.email,
      avatarText: initials.value
    })
    editing.value = false
    showAppToast('Đã cập nhật thông tin tài khoản!', 'success')
  } catch (error) {
    showAppToast(error?.response?.data?.message || 'Không thể cập nhật tài khoản.', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="account-page">
    <div class="account-hero">
      <div class="container-client account-hero-inner">
        <div class="profile-avatar">{{ initials }}</div>
        <div class="hero-copy">
          <span class="eyebrow">TÀI KHOẢN KHÁCH HÀNG</span>
          <h1>{{ form.tenKhachHang || 'Khách hàng FF T-Shirt' }}</h1>
          <p>{{ form.email || form.soDienThoai }}</p>
        </div>
      </div>
    </div>

    <main class="container-client account-content">
      <div v-if="!isClientLoggedIn" class="login-required-card">
        <div class="empty-profile-icon">👤</div>
        <h2>Vui lòng đăng nhập</h2>
        <p>Đăng nhập để quản lý hồ sơ và theo dõi đơn hàng của bạn.</p>
        <button @click="router.push('/')">Về trang chủ</button>
      </div>

      <template v-else>
        <section class="account-shortcuts">
          <router-link to="/don-hang" class="shortcut-card">
            <span class="shortcut-icon blue">📦</span><span><strong>Đơn hàng</strong><small>Theo dõi đơn mua</small></span><b>›</b>
          </router-link>
          <router-link to="/gio-hang" class="shortcut-card">
            <span class="shortcut-icon orange">🛒</span><span><strong>Giỏ hàng</strong><small>{{ cartCount }} sản phẩm</small></span><b>›</b>
          </router-link>
          <router-link to="/tra-cuu" class="shortcut-card">
            <span class="shortcut-icon green">🚚</span><span><strong>Tra cứu</strong><small>Kiểm tra vận chuyển</small></span><b>›</b>
          </router-link>
        </section>

        <section class="profile-card">
          <header class="profile-card-head">
            <div><span class="section-kicker">HỒ SƠ CỦA TÔI</span><h2>Thông tin cá nhân</h2><p>Quản lý thông tin để mua hàng và nhận hỗ trợ thuận tiện hơn.</p></div>
            <button v-if="!editing" class="btn-edit-profile" @click="editing = true">✎ Chỉnh sửa</button>
          </header>

          <div v-if="loading" class="profile-loading">Đang tải hồ sơ...</div>
          <form v-else class="profile-form" @submit.prevent="saveProfile">
            <label class="form-field"><span>Họ và tên</span><input v-model="form.tenKhachHang" :disabled="!editing" /><small v-if="errors.tenKhachHang">{{ errors.tenKhachHang }}</small></label>
            <label class="form-field"><span>Email</span><input v-model="form.email" type="email" :disabled="!editing" /><small v-if="errors.email">{{ errors.email }}</small></label>
            <label class="form-field"><span>Số điện thoại</span><input v-model="form.soDienThoai" :disabled="!editing" /><small v-if="errors.soDienThoai">{{ errors.soDienThoai }}</small></label>
            <label class="form-field"><span>Ngày sinh</span><input v-model="form.ngaySinh" type="date" :disabled="!editing" /></label>
            <div class="form-field gender-field"><span>Giới tính</span><div class="gender-options"><label><input v-model="form.gioiTinh" type="radio" :value="true" :disabled="!editing" /> Nam</label><label><input v-model="form.gioiTinh" type="radio" :value="false" :disabled="!editing" /> Nữ</label></div></div>
            <label class="form-field"><span>Mã khách hàng</span><input :value="form.maKhachHang || 'Chưa cấp mã'" disabled /></label>
            <div v-if="editing" class="profile-actions"><button type="button" class="btn-cancel" @click="editing = false; loadProfile()">Hủy</button><button class="btn-save" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button></div>
          </form>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.account-page{min-height:100vh;background:#f5f7fa;padding-bottom:70px}.account-hero{background:linear-gradient(135deg,#172235,#304b62);color:#fff;padding:34px 0;border-bottom:3px solid #d2a764}.account-hero-inner{display:flex;align-items:center;gap:20px}.profile-avatar{width:76px;height:76px;border-radius:50%;display:grid;place-items:center;background:linear-gradient(135deg,#f2d59d,#d2a764);color:#263d52;font-size:24px;font-weight:900;border:4px solid rgba(255,255,255,.25)}.eyebrow,.section-kicker{font-size:11px;font-weight:800;letter-spacing:1px;color:#d2a764}.hero-copy h1{margin:5px 0 3px;font-size:25px}.hero-copy p{margin:0;color:#dbe4ec;font-size:13px}.account-content{margin-top:22px}.account-shortcuts{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px;margin-bottom:18px}.shortcut-card{display:flex;align-items:center;gap:12px;padding:16px;background:#fff;border:1px solid #e2e8f0;border-radius:14px;text-decoration:none;box-shadow:0 3px 12px rgba(15,23,42,.04);transition:.2s}.shortcut-card:hover{transform:translateY(-2px);border-color:#d2a764}.shortcut-icon{width:42px;height:42px;border-radius:12px;display:grid;place-items:center}.shortcut-icon.blue{background:#eff6ff}.shortcut-icon.orange{background:#fff7ed}.shortcut-icon.green{background:#ecfdf5}.shortcut-card>span:nth-child(2){display:flex;flex:1;flex-direction:column}.shortcut-card strong{font-size:14px}.shortcut-card small{font-size:11px;color:#64748b;margin-top:2px}.shortcut-card b{font-size:25px;color:#94a3b8}.profile-card,.login-required-card{background:#fff;border:1px solid #e2e8f0;border-radius:16px;box-shadow:0 5px 20px rgba(15,23,42,.05)}.profile-card{padding:24px}.profile-card-head{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;padding-bottom:18px;border-bottom:1px solid #edf2f7}.profile-card-head h2{margin:4px 0;font-size:21px}.profile-card-head p{margin:0;color:#64748b;font-size:13px}.btn-edit-profile,.btn-save{border:0;border-radius:9px;background:#496883;color:#fff;padding:10px 16px;font-weight:700;cursor:pointer;white-space:nowrap}.profile-form{display:grid;grid-template-columns:1fr 1fr;gap:18px 20px;padding-top:22px}.form-field{display:flex;flex-direction:column;gap:7px}.form-field>span{font-size:12px;font-weight:700;color:#475569}.form-field input{width:100%;height:44px;border:1px solid #cbd5e1;border-radius:9px;padding:0 12px;font-size:14px;background:#fff;color:#172033}.form-field input:disabled{background:#f8fafc;color:#475569}.form-field small{color:#dc2626;font-size:11px}.gender-options{height:44px;display:flex;align-items:center;gap:24px;padding:0 12px;border:1px solid #cbd5e1;border-radius:9px}.gender-options label{font-size:13px}.gender-options input{width:auto;height:auto;margin-right:5px}.profile-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:10px;padding-top:4px}.btn-cancel{border:1px solid #cbd5e1;background:#fff;border-radius:9px;padding:10px 18px;font-weight:700;color:#64748b}.profile-loading{text-align:center;padding:50px;color:#64748b}.login-required-card{text-align:center;padding:60px 20px}.empty-profile-icon{font-size:50px}.login-required-card button{border:0;background:#496883;color:#fff;padding:11px 22px;border-radius:9px;font-weight:700}
@media(max-width:720px){.account-page{padding-bottom:40px}.account-hero{padding:22px 0}.profile-avatar{width:58px;height:58px;font-size:18px}.hero-copy h1{font-size:19px}.account-content{margin-top:12px;padding-inline:10px}.account-shortcuts{grid-template-columns:1fr;gap:8px}.shortcut-card{padding:12px;border-radius:11px}.profile-card{padding:16px 14px;border-radius:12px}.profile-card-head{flex-direction:column;gap:12px}.btn-edit-profile{width:100%}.profile-form{grid-template-columns:1fr;gap:14px}.profile-actions{position:sticky;bottom:0;background:#fff;margin:0 -14px -16px;padding:12px 14px;border-top:1px solid #e2e8f0}.profile-actions button{flex:1}.login-required-card{padding:42px 16px}}
</style>
