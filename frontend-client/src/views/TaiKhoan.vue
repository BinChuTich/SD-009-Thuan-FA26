<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { cartCount } from '@/utils/luuTruGioHang.js'
import { currentClientUser, isClientLoggedIn, loginClient } from '@/utils/taiKhoanKhachHang.js'
import { showAppConfirm, showAppToast } from '@/utils/thongBaoDialog.js'
import { getProvinces, getWardsByProvince } from '@/services/tinhThanhApi.js'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const addresses = ref([])
const loadingAddresses = ref(false)
const showAddressForm = ref(false)
const savingAddress = ref(false)
const editingAddressId = ref(null)
const provinces = ref([])
const wards = ref([])
const loadingWards = ref(false)
const form = reactive({
  tenKhachHang: '', email: '', soDienThoai: '', ngaySinh: '', gioiTinh: null,
  maKhachHang: '', trangThai: 1, thanhPho: '', huyen: '', phuong: '', diaChiCuThe: ''
})
const errors = reactive({ tenKhachHang: '', email: '', soDienThoai: '' })
const addressForm = reactive({ thanhPho: '', phuong: '', diaChiCuThe: '', macDinh: false })

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
  form.gioiTinh = data.gioiTinh ?? null
  form.maKhachHang = data.maKhachHang || ''
  form.trangThai = data.trangThai ?? 1
  form.thanhPho = data.thanhPho || ''
  form.huyen = data.huyen || ''
  form.phuong = data.phuong || ''
  form.diaChiCuThe = data.diaChiCuThe || ''
}

const loadProfile = async () => {
  if (!isClientLoggedIn.value) {
    fillForm({})
    loading.value = false
    return
  }
  loading.value = true
  fillForm(currentClientUser.value)
  if (!userId.value) {
    loading.value = false
    return
  }
  try {
    const res = await api.get(`/api/khach-hang/${userId.value}`)
    const profile = res.data || currentClientUser.value
    fillForm(profile)
    // Dữ liệu lúc đăng nhập chỉ là bản rút gọn. Cập nhật lại kho tài khoản dùng
    // chung để header, giỏ hàng và các màn khác nhận hồ sơ đầy đủ từ CSDL.
    if (res.data) {
      loginClient({
        ...currentClientUser.value,
        ...profile,
        id: profile.id || userId.value,
        name: profile.tenKhachHang || currentClientUser.value?.name || '',
        phone: profile.soDienThoai || '',
        email: profile.email || ''
      })
    }
  } catch (error) {
    console.warn('Không tải được hồ sơ mới nhất:', error)
  } finally {
    loading.value = false
  }
}

const displayValue = value => String(value ?? '').trim() || 'Chưa cập nhật'
const formatAddress = address => [address?.diaChiCuThe, address?.phuong, address?.huyen, address?.thanhPho].filter(Boolean).join(', ') || 'Chưa cập nhật'

const loadAddresses = async () => {
  if (!userId.value) {
    addresses.value = []
    return
  }
  loadingAddresses.value = true
  try {
    const res = await api.get(`/api/khach-hang/${userId.value}/dia-chi`)
    addresses.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.warn('Không tải được sổ địa chỉ:', error)
    addresses.value = []
  } finally {
    loadingAddresses.value = false
  }
}

const openAddAddress = () => {
  editingAddressId.value = null
  wards.value = []
  Object.assign(addressForm, { thanhPho: '', phuong: '', diaChiCuThe: '', macDinh: addresses.value.length === 0 })
  showAddressForm.value = true
}

const loadWardsForProvince = async (provinceName, keepWard = false) => {
  const province = provinces.value.find(item => item.name === provinceName)
  if (!keepWard) addressForm.phuong = ''
  wards.value = []
  if (!province) return
  loadingWards.value = true
  try {
    wards.value = await getWardsByProvince(province.code)
  } finally {
    loadingWards.value = false
  }
}

const onProvinceChanged = () => loadWardsForProvince(addressForm.thanhPho)

const openEditAddress = async address => {
  editingAddressId.value = address.id
  Object.assign(addressForm, {
    thanhPho: address.thanhPho || '',
    phuong: address.phuong || '',
    diaChiCuThe: address.diaChiCuThe || '',
    macDinh: Boolean(address.macDinh)
  })
  showAddressForm.value = true
  await loadWardsForProvince(addressForm.thanhPho, true)
}

const saveAddress = async () => {
  if (!addressForm.diaChiCuThe.trim() || !addressForm.phuong.trim() || !addressForm.thanhPho.trim()) {
    showAppToast('Vui lòng nhập địa chỉ cụ thể, phường/xã và tỉnh/thành phố.', 'warning')
    return
  }
  savingAddress.value = true
  try {
    const payload = {
      diaChiCuThe: addressForm.diaChiCuThe.trim(),
      phuong: addressForm.phuong.trim(),
      huyen: '',
      thanhPho: addressForm.thanhPho.trim(),
      macDinh: Boolean(addressForm.macDinh),
      tenNguoiNhan: form.tenKhachHang.trim() || null,
      soDienThoaiNhan: form.soDienThoai.trim() || null
    }
    if (editingAddressId.value) {
      await api.put(`/api/khach-hang/${userId.value}/dia-chi/${editingAddressId.value}`, payload)
    } else {
      await api.post(`/api/khach-hang/${userId.value}/dia-chi`, payload)
    }
    showAddressForm.value = false
    await loadAddresses()
    showAppToast(editingAddressId.value ? 'Đã cập nhật địa chỉ!' : 'Đã thêm địa chỉ mới!', 'success')
  } catch (error) {
    showAppToast(error?.response?.data?.message || 'Không thể thêm địa chỉ.', 'error')
  } finally {
    savingAddress.value = false
  }
}

const setDefaultAddress = async address => {
  if (address.macDinh) return
  try {
    await api.put(`/api/khach-hang/${userId.value}/dia-chi/${address.id}/mac-dinh`)
    await loadAddresses()
    showAppToast('Đã thay đổi địa chỉ mặc định!', 'success')
  } catch (error) {
    showAppToast(error?.response?.data?.message || 'Không thể đặt địa chỉ mặc định.', 'error')
  }
}

const deleteAddress = async address => {
  const accepted = await showAppConfirm(`Bạn có chắc muốn xóa địa chỉ "${address.maDiaChi || formatAddress(address)}"?`, 'Xóa địa chỉ')
  if (!accepted) return
  try {
    await api.delete(`/api/khach-hang/${userId.value}/dia-chi/${address.id}`)
    await loadAddresses()
    showAppToast('Đã xóa địa chỉ!', 'success')
  } catch (error) {
    showAppToast(error?.response?.data?.message || 'Không thể xóa địa chỉ.', 'error')
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

onMounted(async () => {
  provinces.value = await getProvinces()
  await loadProfile()
  await loadAddresses()
})

// Trang tài khoản có thể đã được mở trước khi modal đăng nhập hoàn tất. Theo dõi
// đúng customer ID để tự nạp hồ sơ và địa chỉ ngay sau khi đăng nhập/đăng xuất.
watch(userId, async (newId, oldId) => {
  if (String(newId || '') === String(oldId || '')) return
  await loadProfile()
  await loadAddresses()
})
</script>

<template>
  <div class="account-page">
    <div class="account-hero">
      <div class="container-client account-hero-inner">
        <div class="profile-avatar">{{ initials }}</div>
        <div class="hero-copy">
          <span class="eyebrow">TÀI KHOẢN KHÁCH HÀNG</span>
          <h1>{{ form.tenKhachHang || 'Khách hàng FF T-Shirt' }}</h1>
          <p>{{ form.email || form.soDienThoai || 'Chưa cập nhật' }}</p>
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
            <label class="form-field"><span>Họ và tên</span><input v-if="editing" v-model="form.tenKhachHang" /><div v-else class="field-value" :class="{ missing: !form.tenKhachHang }">{{ displayValue(form.tenKhachHang) }}</div><small v-if="errors.tenKhachHang">{{ errors.tenKhachHang }}</small></label>
            <label class="form-field"><span>Email</span><input v-if="editing" v-model="form.email" type="email" /><div v-else class="field-value" :class="{ missing: !form.email }">{{ displayValue(form.email) }}</div><small v-if="errors.email">{{ errors.email }}</small></label>
            <label class="form-field"><span>Số điện thoại</span><input v-if="editing" v-model="form.soDienThoai" /><div v-else class="field-value" :class="{ missing: !form.soDienThoai }">{{ displayValue(form.soDienThoai) }}</div><small v-if="errors.soDienThoai">{{ errors.soDienThoai }}</small></label>
            <label class="form-field"><span>Ngày sinh</span><input v-if="editing" v-model="form.ngaySinh" type="date" /><div v-else class="field-value" :class="{ missing: !form.ngaySinh }">{{ displayValue(form.ngaySinh) }}</div></label>
            <div class="form-field gender-field"><span>Giới tính</span><div v-if="editing" class="gender-options"><label><input v-model="form.gioiTinh" type="radio" :value="true" /> Nam</label><label><input v-model="form.gioiTinh" type="radio" :value="false" /> Nữ</label></div><div v-else class="field-value" :class="{ missing: form.gioiTinh == null }">{{ form.gioiTinh == null ? 'Chưa cập nhật' : (form.gioiTinh ? 'Nam' : 'Nữ') }}</div></div>
            <label class="form-field"><span>Mã khách hàng</span><div class="field-value" :class="{ missing: !form.maKhachHang }">{{ displayValue(form.maKhachHang) }}</div></label>
            <div v-if="editing" class="profile-actions"><button type="button" class="btn-cancel" @click="editing = false; loadProfile()">Hủy</button><button class="btn-save" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button></div>
          </form>
        </section>

        <section class="profile-card address-card-section">
          <header class="profile-card-head address-head">
            <div><span class="section-kicker">SỔ ĐỊA CHỈ</span><h2>Địa chỉ của tôi</h2><p>Địa chỉ đã lưu sẽ được sử dụng nhanh khi đặt hàng.</p></div>
            <button class="btn-edit-profile" @click="openAddAddress">＋ Thêm địa chỉ</button>
          </header>
          <div v-if="loadingAddresses" class="profile-loading">Đang tải địa chỉ...</div>
          <div v-else-if="addresses.length" class="address-grid">
            <article v-for="address in addresses" :key="address.id" class="address-item" :class="{ default: address.macDinh }">
              <div class="address-item-top"><strong>{{ address.maDiaChi || 'Địa chỉ nhận hàng' }}</strong><span v-if="address.macDinh">Mặc định</span></div>
              <p>📍 {{ formatAddress(address) }}</p>
              <div class="address-actions">
                <button v-if="!address.macDinh" class="btn-address-default" @click="setDefaultAddress(address)">Đặt làm mặc định</button>
                <button class="btn-address-edit" @click="openEditAddress(address)">Sửa</button>
                <button class="btn-address-delete" @click="deleteAddress(address)">Xóa</button>
              </div>
            </article>
          </div>
          <div v-else class="address-empty"><span>📍</span><div><strong>Chưa cập nhật</strong><p>Bạn chưa có địa chỉ nhận hàng nào.</p></div><button @click="openAddAddress">Thêm địa chỉ đầu tiên</button></div>
        </section>
      </template>
    </main>

    <div v-if="showAddressForm" class="address-modal-backdrop" @click.self="showAddressForm = false">
      <form class="address-modal" @submit.prevent="saveAddress">
        <header><div><span class="section-kicker">SỔ ĐỊA CHỈ</span><h3>{{ editingAddressId ? 'Sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h3></div><button type="button" @click="showAddressForm = false">×</button></header>
        <label class="form-field full"><span>Địa chỉ cụ thể *</span><input v-model="addressForm.diaChiCuThe" placeholder="Số nhà, ngõ/ngách, tên đường..." /></label>
        <div class="address-form-grid">
          <label class="form-field"><span>Tỉnh / Thành phố *</span><select v-model="addressForm.thanhPho" class="address-combobox" @change="onProvinceChanged"><option value="">Chọn Tỉnh / Thành phố</option><option v-for="province in provinces" :key="province.code" :value="province.name">{{ province.name }}</option></select></label>
          <label class="form-field"><span>Phường / Xã *</span><select v-model="addressForm.phuong" class="address-combobox" :disabled="!addressForm.thanhPho || loadingWards"><option value="">{{ loadingWards ? 'Đang tải...' : 'Chọn Phường / Xã' }}</option><option v-for="ward in wards" :key="ward.code" :value="ward.name">{{ ward.name }}</option></select></label>
        </div>
        <label class="default-check"><input v-model="addressForm.macDinh" type="checkbox" /> Đặt làm địa chỉ mặc định</label>
        <footer><button type="button" class="btn-cancel" @click="showAddressForm = false">Hủy</button><button class="btn-save" :disabled="savingAddress">{{ savingAddress ? 'Đang lưu...' : (editingAddressId ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ') }}</button></footer>
      </form>
    </div>
  </div>
</template>

<style scoped>
.account-page{min-height:100vh;background:#f5f7fa;padding-bottom:70px}.account-hero{background:linear-gradient(135deg,#172235,#304b62);color:#fff;padding:34px 0;border-bottom:3px solid #d2a764}.account-hero-inner{display:flex;align-items:center;gap:20px}.profile-avatar{width:76px;height:76px;border-radius:50%;display:grid;place-items:center;background:linear-gradient(135deg,#f2d59d,#d2a764);color:#263d52;font-size:24px;font-weight:900;border:4px solid rgba(255,255,255,.25)}.eyebrow,.section-kicker{font-size:11px;font-weight:800;letter-spacing:1px;color:#d2a764}.hero-copy h1{margin:5px 0 3px;font-size:25px}.hero-copy p{margin:0;color:#dbe4ec;font-size:13px}.account-content{margin-top:22px}.account-shortcuts{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px;margin-bottom:18px}.shortcut-card{display:flex;align-items:center;gap:12px;padding:16px;background:#fff;border:1px solid #e2e8f0;border-radius:14px;text-decoration:none;box-shadow:0 3px 12px rgba(15,23,42,.04);transition:.2s}.shortcut-card:hover{transform:translateY(-2px);border-color:#d2a764}.shortcut-icon{width:42px;height:42px;border-radius:12px;display:grid;place-items:center}.shortcut-icon.blue{background:#eff6ff}.shortcut-icon.orange{background:#fff7ed}.shortcut-icon.green{background:#ecfdf5}.shortcut-card>span:nth-child(2){display:flex;flex:1;flex-direction:column}.shortcut-card strong{font-size:14px}.shortcut-card small{font-size:11px;color:#64748b;margin-top:2px}.shortcut-card b{font-size:25px;color:#94a3b8}.profile-card,.login-required-card{background:#fff;border:1px solid #e2e8f0;border-radius:16px;box-shadow:0 5px 20px rgba(15,23,42,.05)}.profile-card{padding:24px}.profile-card-head{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;padding-bottom:18px;border-bottom:1px solid #edf2f7}.profile-card-head h2{margin:4px 0;font-size:21px}.profile-card-head p{margin:0;color:#64748b;font-size:13px}.btn-edit-profile,.btn-save{border:0;border-radius:9px;background:#496883;color:#fff;padding:10px 16px;font-weight:700;cursor:pointer;white-space:nowrap}.profile-form{display:grid;grid-template-columns:1fr 1fr;gap:18px 20px;padding-top:22px}.form-field{display:flex;flex-direction:column;gap:7px}.form-field>span{font-size:12px;font-weight:700;color:#475569}.form-field input{width:100%;height:44px;border:1px solid #cbd5e1;border-radius:9px;padding:0 12px;font-size:14px;background:#fff;color:#172033}.form-field input:disabled{background:#f8fafc;color:#475569}.form-field small{color:#dc2626;font-size:11px}.gender-options{height:44px;display:flex;align-items:center;gap:24px;padding:0 12px;border:1px solid #cbd5e1;border-radius:9px}.gender-options label{font-size:13px}.gender-options input{width:auto;height:auto;margin-right:5px}.profile-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:10px;padding-top:4px}.btn-cancel{border:1px solid #cbd5e1;background:#fff;border-radius:9px;padding:10px 18px;font-weight:700;color:#64748b}.profile-loading{text-align:center;padding:50px;color:#64748b}.login-required-card{text-align:center;padding:60px 20px}.empty-profile-icon{font-size:50px}.login-required-card button{border:0;background:#496883;color:#fff;padding:11px 22px;border-radius:9px;font-weight:700}
.field-value{width:100%;min-height:44px;display:flex;align-items:center;border:1px solid #cbd5e1;border-radius:9px;padding:10px 12px;font-size:14px;background:#f8fafc;color:#334155}.field-value.missing{color:#94a3b8;font-style:italic}.address-card-section{margin-top:18px}.address-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;padding-top:20px}.address-item{padding:16px;border:1px solid #dbe3ec;border-radius:12px;background:#fff}.address-item.default{border-color:#d2a764;background:#fffbf3}.address-item-top{display:flex;align-items:center;justify-content:space-between;gap:12px}.address-item-top strong{font-size:14px}.address-item-top span{padding:4px 8px;border-radius:999px;background:#dcfce7;color:#15803d;font-size:10px;font-weight:800}.address-item p{margin:10px 0 0;color:#526273;font-size:13px;line-height:1.55}.address-actions{display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-top:14px;padding-top:12px;border-top:1px solid #edf2f7}.address-actions button{border:0;background:transparent;padding:6px 8px;border-radius:6px;font-size:12px;font-weight:700;cursor:pointer}.btn-address-default{margin-right:auto!important;color:#2563eb;background:#eff6ff!important}.btn-address-edit{color:#475569}.btn-address-edit:hover{background:#f1f5f9}.btn-address-delete{color:#dc2626}.btn-address-delete:hover{background:#fef2f2}.address-empty{display:flex;align-items:center;gap:14px;margin-top:20px;padding:18px;border:1px dashed #cbd5e1;border-radius:12px;background:#f8fafc}.address-empty>span{font-size:28px}.address-empty div{flex:1}.address-empty p{margin:3px 0 0;color:#64748b;font-size:12px}.address-empty button{border:1px solid #496883;border-radius:8px;background:#fff;color:#36546d;padding:9px 13px;font-weight:700;cursor:pointer}.address-modal-backdrop{position:fixed;inset:0;z-index:12000;display:grid;place-items:center;padding:16px;background:rgba(15,23,42,.56);backdrop-filter:blur(3px)}.address-modal{width:min(580px,100%);max-height:calc(100vh - 32px);overflow:auto;background:#fff;border-radius:16px;padding:22px;box-shadow:0 24px 70px rgba(15,23,42,.3)}.address-modal header{display:flex;align-items:flex-start;justify-content:space-between;padding-bottom:16px;margin-bottom:18px;border-bottom:1px solid #e2e8f0}.address-modal h3{margin:4px 0 0;font-size:20px}.address-modal header>button{width:34px;height:34px;border:0;border-radius:50%;background:#f1f5f9;color:#64748b;font-size:24px;cursor:pointer}.address-form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:14px}.address-form-grid .full{grid-column:1/-1}.address-combobox{width:100%;height:44px;border:1px solid #cbd5e1;border-radius:9px;padding:0 38px 0 12px;background:#fff;color:#172033;font-size:14px;cursor:pointer}.address-combobox:focus{outline:none;border-color:#496883;box-shadow:0 0 0 3px rgba(73,104,131,.12)}.address-combobox:disabled{background:#f1f5f9;color:#94a3b8;cursor:not-allowed}.default-check{display:flex;align-items:center;gap:8px;margin-top:16px;font-size:13px;color:#475569}.default-check input{width:16px;height:16px}.address-modal footer{display:flex;justify-content:flex-end;gap:10px;padding-top:20px;margin-top:18px;border-top:1px solid #e2e8f0}
@media(max-width:720px){.account-page{padding-bottom:40px}.account-hero{padding:22px 0}.profile-avatar{width:58px;height:58px;font-size:18px}.hero-copy h1{font-size:19px}.account-content{margin-top:12px;padding-inline:10px}.account-shortcuts{grid-template-columns:1fr;gap:8px}.shortcut-card{padding:12px;border-radius:11px}.profile-card{padding:16px 14px;border-radius:12px}.profile-card-head{flex-direction:column;gap:12px}.btn-edit-profile{width:100%}.profile-form,.address-grid,.address-form-grid{grid-template-columns:1fr;gap:14px}.address-form-grid .full{grid-column:auto}.address-empty{align-items:flex-start;flex-wrap:wrap}.address-empty button{width:100%}.address-modal{padding:18px 14px;border-radius:14px}.address-modal footer button{flex:1}.profile-actions{position:sticky;bottom:0;background:#fff;margin:0 -14px -16px;padding:12px 14px;border-top:1px solid #e2e8f0}.profile-actions button{flex:1}.login-required-card{padding:42px 16px}}
.btn-address-default{margin-right:0!important}
</style>
