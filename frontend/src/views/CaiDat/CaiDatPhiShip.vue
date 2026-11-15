<template>
  <div class="page">
    <transition name="toast"><div v-if="notice.show" class="notice" :class="notice.type">{{ notice.message }}</div></transition>
    <div class="page-top-header">
      <div class="page-breadcrumb">
        <router-link to="/cai-dat" class="crumb-parent">Cài đặt hệ thống</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Cấu hình miễn phí vận chuyển</span>
      </div>
    </div>

    <main class="card content-card">
      <div class="card-title">
        <div><small>CẤU HÌNH VẬN CHUYỂN</small><h2>Miễn phí vận chuyển theo giá trị hóa đơn</h2>
          <p>Quản lý ngưỡng freeship áp dụng đồng bộ cho bán hàng tại quầy và đặt hàng online.</p></div>
        <b :class="{ off: rules.length === 0 }">{{ rules.length ? 'Đang áp dụng' : 'Đã tắt' }}</b>
      </div>

      <section class="create-box">
        <label for="threshold">Giá trị hóa đơn tối thiểu</label>
        <div class="create-row">
          <div class="money-input" :class="{ invalid: errorMessage }">
            <input id="threshold" v-model="newThreshold" type="text" inputmode="numeric" maxlength="10"
                   placeholder="Ví dụ: 2.000.000" :disabled="busy || !isAdmin"
                   @input="handleThresholdInput" @blur="validateThreshold(false)" @keyup.enter="addRule" />
            <span>VNĐ</span>
          </div>
          <button class="primary" :disabled="busy || !isAdmin" @click="addRule">+ Thêm cấu hình</button>
        </div>
        <em v-if="errorMessage" class="error">{{ errorMessage }}</em>
      </section>

      <section class="list-section">
        <div class="list-heading"><h3>Các mức freeship đã tạo</h3><span>{{ rules.length }} cấu hình</span></div>
        <div v-if="loading" class="empty">Đang tải cấu hình...</div>
        <div v-else-if="rules.length === 0" class="empty">
          <strong>Chưa có cấu hình freeship</strong>
          <p>Hóa đơn sẽ tự tính phí vận chuyển bình thường cho đến khi thêm cấu hình mới.</p>
        </div>
        <div v-else class="table-wrap">
          <table><thead><tr><th>STT</th><th>Giá trị hóa đơn được freeship</th><th>Áp dụng</th><th>Thao tác</th></tr></thead>
            <tbody><tr v-for="(rule, index) in sortedRules" :key="rule.id">
              <td>{{ index + 1 }}</td>
              <td><strong>Từ {{ money(rule.giaTriTu) }} trở lên</strong></td>
              <td><span class="channel">POS &amp; Client</span></td>
              <td><button class="delete" :disabled="busy || !isAdmin" @click="removeRule(rule)">Xóa</button></td>
            </tr></tbody>
          </table>
        </div>
      </section>

      <section class="status" :class="{ disabled: rules.length === 0 }">
        <i>{{ rules.length ? '✓' : '!' }}</i><div>
          <strong v-if="rules.length">Mức freeship đang có hiệu lực: từ {{ money(effectiveThreshold) }}</strong>
          <strong v-else>Freeship đang tắt</strong>
          <p v-if="rules.length">Áp dụng cho POS và client, không phụ thuộc khoảng cách giao hàng.</p>
          <p v-else>POS và client sẽ tự tính phí ship theo chính sách vận chuyển bình thường.</p>
        </div>
      </section>
      <p v-if="!isAdmin" class="permission">Chỉ Quản trị viên được thay đổi cấu hình freeship.</p>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/api.js'
import { getCurrentUser } from '@/utils/auth.js'
import { DEFAULT_SHIPPING_CONFIG, getShippingConfig, invalidateShippingConfigCache } from '@/services/ghnService.js'

const user = computed(() => getCurrentUser())
const isAdmin = computed(() => {
  if (!user.value) return true
  const code = String(user.value.maVaiTro || '').toUpperCase()
  const role = String(user.value.role || user.value.tenVaiTro || '').toLowerCase()
  return code === 'ADMIN' || code === 'QL' || role.includes('quản trị') || role.includes('quan tri')
})
const config = ref({ ...DEFAULT_SHIPPING_CONFIG })
const rules = ref([])
const newThreshold = ref('')
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const notice = reactive({ show: false, message: '', type: 'success' })
let noticeTimer
const busy = computed(() => loading.value || saving.value)
const sortedRules = computed(() => [...rules.value].sort((a, b) => Number(a.giaTriTu) - Number(b.giaTriTu)))
const effectiveThreshold = computed(() => sortedRules.value.length ? Number(sortedRules.value[0].giaTriTu) : 0)
const money = value => `${Number(value || 0).toLocaleString('vi-VN')} đ`

function thresholdValue() {
  const digits = String(newThreshold.value || '').replace(/\D/g, '')
  return digits ? Number(digits) : 0
}

function handleThresholdInput(event) {
  const digits = String(event?.target?.value || '').replace(/\D/g, '').slice(0, 8)
  newThreshold.value = digits ? Number(digits).toLocaleString('vi-VN') : ''
  errorMessage.value = ''
}

function validateThreshold(showRequired = true) {
  const raw = String(newThreshold.value || '').trim()
  const value = thresholdValue()
  if (!raw) {
    errorMessage.value = showRequired ? 'Vui lòng nhập giá trị hóa đơn tối thiểu.' : ''
    return false
  }
  if (!Number.isSafeInteger(value) || value < 1000) {
    errorMessage.value = 'Giá trị freeship phải từ 1.000 VNĐ trở lên.'
    return false
  }
  if (value > 50000000) {
    errorMessage.value = 'Giá trị freeship không được vượt quá 50.000.000 VNĐ.'
    return false
  }
  if (value % 1000 !== 0) {
    errorMessage.value = 'Giá trị freeship phải là bội số của 1.000 VNĐ.'
    return false
  }
  if (rules.value.some(rule => Number(rule.giaTriTu) === value)) {
    errorMessage.value = 'Mức freeship này đã có trong danh sách.'
    return false
  }
  errorMessage.value = ''
  return true
}

function notify(message, type = 'success') {
  Object.assign(notice, { show: true, message, type }); clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => { notice.show = false }, 2800)
}
function authData() {
  return { idNhanVien: user.value?.id || null, maVaiTro: user.value?.maVaiTro || 'ADMIN',
    nguoiCapNhat: user.value?.tenNhanVien || user.value?.hoTen || user.value?.tenTaiKhoan || 'Quản trị viên' }
}
function normalizeRules(cfg) {
  const freeRules = (Array.isArray(cfg.danhSachCauHinh) ? cfg.danhSachCauHinh : [])
    .filter(rule => String(rule.hinhThucTinhPhi || '').toUpperCase() === 'MIEN_PHI' && Number(rule.trangThai ?? 1) !== 0)
  if (freeRules.length) return freeRules
  // Chuyển cấu hình một ngưỡng của phiên bản cũ thành một dòng trong danh sách.
  if (cfg.apDungMienPhiShip !== false && Number(cfg.nguongMienPhiShip) > 0) {
    return [{ id: 1, giaTriTu: Number(cfg.nguongMienPhiShip), giaTriDen: 50000000,
      khoangCachTu: 0, khoangCachDen: 99999, hinhThucTinhPhi: 'MIEN_PHI', phiVanChuyen: 0, trangThai: 1 }]
  }
  return []
}
async function loadConfig() {
  loading.value = true
  try {
    invalidateShippingConfigCache(); const cfg = await getShippingConfig({ forceRefresh: true })
    config.value = { ...DEFAULT_SHIPPING_CONFIG, ...(cfg || {}) }; rules.value = normalizeRules(config.value)
  } catch { notify('Không thể tải danh sách freeship.', 'error') }
  finally { loading.value = false }
}
async function persist(nextRules, successMessage) {
  saving.value = true
  try {
    const auth = authData()
    const minimum = nextRules.length ? Math.min(...nextRules.map(rule => Number(rule.giaTriTu))) : 0
    const payload = { ...config.value, apDungMienPhiShip: nextRules.length > 0,
      nguongMienPhiShip: minimum, apDungFreeshipPos: nextRules.length > 0,
      apDungFreeshipOnline: nextRules.length > 0, danhSachCauHinh: nextRules, ...auth }
    const { data } = await api.put('/api/cau-hinh-phi-ship', payload, { params: auth })
    config.value = { ...DEFAULT_SHIPPING_CONFIG, ...(data || payload) }; rules.value = normalizeRules(config.value)
    invalidateShippingConfigCache(); await getShippingConfig({ forceRefresh: true })
    window.dispatchEvent(new CustomEvent('shipping-config-updated', { detail: config.value })); notify(successMessage)
  } catch (e) { notify(e?.response?.data?.message || 'Không thể cập nhật cấu hình freeship.', 'error') }
  finally { saving.value = false }
}
async function addRule() {
  if (!isAdmin.value || busy.value) return
  if (!validateThreshold(true)) return
  const value = thresholdValue()
  const nextId = rules.value.reduce((max, rule) => Math.max(max, Number(rule.id) || 0), 0) + 1
  const rule = { id: nextId, giaTriTu: value, giaTriDen: 50000000, khoangCachTu: 0,
    khoangCachDen: 99999, hinhThucTinhPhi: 'MIEN_PHI', phiVanChuyen: 0, trangThai: 1 }
  await persist([...rules.value, rule], `Đã thêm mức freeship từ ${money(value)}.`)
  if (!saving.value) newThreshold.value = ''
}
async function removeRule(rule) {
  if (!isAdmin.value || busy.value) return
  const next = rules.value.filter(item => Number(item.id) !== Number(rule.id))
  await persist(next, next.length ? 'Đã xóa cấu hình freeship.' : 'Đã xóa hết cấu hình. Hóa đơn sẽ tự tính phí ship.')
}
onMounted(loadConfig)
</script>

<style scoped>
.page{min-height:calc(100vh - 72px);padding:34px;background:#f7f6f2;color:#172033}.header,.card{max-width:1060px;margin:auto;background:#fff;border:1px solid #dce3ed;border-radius:16px;box-shadow:0 4px 14px #0f172a0d}.header{display:flex;gap:18px;align-items:center;padding:26px 30px}.icon{display:grid;place-items:center;width:50px;height:50px;border-radius:13px;background:#2450b8;font-size:25px}.title-row{display:flex;align-items:center;gap:12px;flex-wrap:wrap}h1,h2,h3{margin:0}h1{font-size:23px}.title-row span,.card-title b{padding:5px 10px;border-radius:999px;font-size:12px}.title-row span{color:#2450b8;background:#eff6ff;border:1px solid #bfdbfe}.header p,.card-title p,.status p{margin:7px 0 0;color:#64748b;line-height:1.5}.card{margin-top:22px;padding:32px}.card-title{display:flex;justify-content:space-between;gap:20px}.card-title small{color:#2450b8;font-weight:800;letter-spacing:.08em}.card-title h2{margin-top:7px;font-size:22px}.card-title b{height:max-content;color:#087f5b;background:#ecfdf5;border:1px solid #a7f3d0}.card-title b.off{color:#64748b;background:#f8fafc;border-color:#cbd5e1}.create-box{margin-top:28px;padding:22px;border:1px solid #dbe4f0;border-radius:13px;background:#f8fafc}.create-box label{display:block;margin-bottom:10px;font-weight:700}.create-row{display:flex;gap:12px;max-width:780px}.money-input{display:flex;flex:1;border:1px solid #cbd5e1;border-radius:10px;overflow:hidden;background:#fff}.money-input:focus-within{border-color:#2450b8;box-shadow:0 0 0 3px #2450b81a}.money-input.invalid{border-color:#ef4444}.money-input input{flex:1;min-width:0;border:0;outline:0;padding:14px 16px;font-size:16px;font-weight:700}.money-input span{padding:14px 17px;color:#64748b;font-weight:700;border-left:1px solid #e2e8f0}.create-box em{display:block;margin-top:9px;color:#64748b;font-style:normal}.create-box .error{color:#dc2626}.list-section{margin-top:27px}.list-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}.list-heading span{color:#64748b}.table-wrap{overflow:auto;border:1px solid #dce3ed;border-radius:12px}table{width:100%;border-collapse:collapse}th,td{padding:15px 17px;text-align:left;border-bottom:1px solid #e8edf3}th{background:#f8fafc;color:#475569;font-size:13px}tr:last-child td{border-bottom:0}.channel{padding:5px 9px;border-radius:999px;color:#2450b8;background:#eff6ff;font-size:12px;font-weight:700}.empty{text-align:center;padding:42px;border:1px dashed #cbd5e1;border-radius:12px;color:#64748b}.empty p{margin:7px 0 0}.status{display:flex;gap:13px;margin-top:24px;padding:18px;border-radius:12px;background:#f0fdf4;border:1px solid #bbf7d0}.status.disabled{background:#f8fafc;border-color:#cbd5e1}.status i{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;color:#fff;background:#16a34a;font-style:normal;font-weight:800}.status.disabled i{background:#64748b}.status p{margin-top:4px}.primary,.delete{border-radius:9px;font-weight:700;cursor:pointer}.primary{padding:0 20px;color:#fff;background:#2450b8;border:1px solid #2450b8}.delete{padding:7px 12px;color:#dc2626;background:#fff;border:1px solid #fecaca}.primary:disabled,.delete:disabled{opacity:.55;cursor:not-allowed}.permission{text-align:right;color:#b45309}.notice{position:fixed;z-index:3000;top:24px;left:50%;transform:translateX(-50%);padding:13px 20px;border-radius:10px;color:#fff;background:#16a34a;box-shadow:0 10px 30px #0f172a33;font-weight:700}.notice.error{background:#dc2626}.toast-enter-active,.toast-leave-active{transition:.2s}.toast-enter-from,.toast-leave-to{opacity:0;transform:translate(-50%,-8px)}@media(max-width:720px){.page{padding:16px}.header,.card{padding:22px}.card-title,.create-row{flex-direction:column}.primary{min-height:44px}th:nth-child(1),td:nth-child(1),th:nth-child(3),td:nth-child(3){display:none}}
</style>

<style scoped>
/* Đồng bộ khung trang với các màn quản trị sản phẩm, nhân viên và phiếu giảm giá. */
.page {
  min-height: calc(100vh - 48px);
  box-sizing: border-box;
  padding: 1.25rem 2rem 3rem !important;
  background: var(--bg, #f7f5ef) !important;
  color: var(--text, #3d4a50);
}

.page-top-header {
  display: flex;
  align-items: center;
  min-height: 38px;
  margin: 0 0 1.25rem;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: .4rem;
  font-size: .95rem;
  color: #7f8c8d;
}

.crumb-parent { color: #7f8c8d; text-decoration: none; transition: color .15s; }
.crumb-parent:hover { color: var(--blue, #496883); }
.crumb-separator { color: #bdc3c7; }
.crumb-current { color: var(--blue, #496883); font-weight: 600; }

.content-card.card {
  max-width: none !important;
  width: 100%;
  margin: 0 !important;
  padding: 1.5rem !important;
  border: 1px solid var(--line, #e8e2d5) !important;
  border-radius: 12px !important;
  background: #fff !important;
  box-shadow: 0 1px 3px rgba(65, 60, 50, .025) !important;
}

.card-title { padding: 0 0 1.25rem !important; }
.card-title small { color: #8b6b3f !important; font-size: .75rem; }
.card-title h2 { color: #2c3e50; font-size: 1.25rem !important; }
.card-title p { font-size: .88rem; color: #7f8c8d; }
.card-title b { color: #276749; background: #edf7f0; border-color: #b8dcc4; }

.create-box {
  margin-top: 1.25rem !important;
  padding: 1.25rem !important;
  border: 1px solid #e9e5db !important;
  border-radius: 10px !important;
  background: #fbfaf7 !important;
}

.money-input:focus-within { border-color: var(--blue, #496883) !important; box-shadow: 0 0 0 3px rgba(73,104,131,.12) !important; }
.primary { background: var(--blue, #496883) !important; border-color: var(--blue, #496883) !important; box-shadow: 0 2px 6px rgba(73,104,131,.22) !important; }
.primary:hover:not(:disabled) { background: #385269 !important; transform: translateY(-1px); }
.list-section { margin-top: 1.5rem !important; }
.list-heading h3 { color: #43545c; font-size: 1.05rem; }
.list-heading span { padding: 4px 9px; border-radius: 999px; background: #f3f1eb; font-size: .8rem; }
.table-wrap { border-color: #e9e5db !important; border-radius: 10px !important; }
.table-wrap th { background: #f8f7f3 !important; color: #59676d !important; }
.channel { color: #496883; background: #eef3f7; border-color: #cdd9e2; }
.status { border-radius: 10px !important; }
.notice { top: 72px; border-radius: 8px; font-size: .9rem; }

@media (max-width: 760px) {
  .page { padding: 1rem !important; }
  .content-card.card { padding: 1rem !important; }
}
</style>

<style scoped>
.page {
  padding: 32px 40px 56px;
  background: linear-gradient(180deg, #f8fafc 0%, #f5f3ef 100%);
}

.header,
.card {
  max-width: 1180px;
  border: 1px solid #e1e7ef;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.header {
  position: relative;
  overflow: hidden;
  padding: 28px 32px;
}

.header::after {
  content: '';
  position: absolute;
  width: 180px;
  height: 180px;
  right: -70px;
  top: -90px;
  border-radius: 50%;
  background: rgba(37, 80, 184, 0.06);
}

.icon {
  flex: 0 0 54px;
  width: 54px;
  height: 54px;
  border-radius: 15px;
  background: linear-gradient(145deg, #2f61d5, #183c9a);
  box-shadow: 0 9px 20px rgba(36, 80, 184, 0.24);
}

.card {
  padding: 34px;
  border-radius: 18px;
}

.card-title {
  padding-bottom: 26px;
  border-bottom: 1px solid #edf1f6;
}

.card-title h2 {
  margin-top: 8px;
  font-size: 24px;
  letter-spacing: -0.02em;
}

.card-title b {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 13px;
  font-size: 13px;
}

.card-title b::before {
  content: '';
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.13);
}

.card-title b.off::before {
  background: #94a3b8;
  box-shadow: none;
}

.create-box {
  margin-top: 26px !important;
  padding: 24px !important;
  border: 1px solid #d9e4f5 !important;
  border-radius: 15px !important;
  background: linear-gradient(135deg, #f8fbff, #f4f7fd) !important;
}

.create-box label {
  margin-bottom: 11px;
  color: #26344d;
  font-size: 14px;
}

.create-row {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  max-width: none;
}

.money-input {
  min-height: 50px;
  border-color: #cdd8e7;
  border-radius: 11px;
  box-shadow: 0 2px 5px rgba(15, 23, 42, 0.03);
}

.money-input input {
  padding: 13px 17px;
  color: #15213a;
  font-size: 17px;
}

.money-input input::placeholder {
  color: #9aa8bc;
  font-weight: 500;
}

.money-input span {
  display: grid;
  place-items: center;
  min-width: 72px;
  background: #f8fafc;
}

.primary {
  min-height: 50px;
  padding: 0 23px !important;
  border-radius: 11px;
  background: linear-gradient(135deg, #2d5dcc, #1d45ad);
  box-shadow: 0 8px 18px rgba(36, 80, 184, 0.21);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 11px 24px rgba(36, 80, 184, 0.27);
}

.create-box em {
  margin-top: 11px;
  font-size: 13px;
}

.list-section {
  margin-top: 30px !important;
}

.list-heading {
  margin-bottom: 14px;
}

.list-heading h3 {
  font-size: 17px;
}

.list-heading span {
  padding: 5px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.table-wrap {
  display: block !important;
  overflow: hidden;
  border: 1px solid #e1e7ef !important;
  border-radius: 14px !important;
  background: #fff;
}

.table-wrap table {
  display: table !important;
  width: 100% !important;
  border-collapse: collapse !important;
}

.table-wrap th {
  padding: 13px 18px !important;
  border-bottom: 1px solid #e5eaf1 !important;
  background: #f7f9fc !important;
  color: #64748b !important;
  font-size: 12px !important;
  font-weight: 800 !important;
  letter-spacing: 0.035em;
  text-transform: uppercase;
}

.table-wrap td {
  padding: 17px 18px !important;
  border-bottom: 1px solid #edf1f5 !important;
  color: #334155;
  vertical-align: middle;
}

.table-wrap tbody tr {
  transition: background 0.15s ease;
}

.table-wrap tbody tr:hover {
  background: #fafcff;
}

.table-wrap td:first-child,
.table-wrap th:first-child {
  width: 70px;
  text-align: center;
}

.table-wrap td:last-child,
.table-wrap th:last-child {
  width: 110px;
  text-align: center;
}

.table-wrap td strong {
  color: #14213a;
  font-size: 15px;
}

.channel {
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border: 1px solid #cfe0ff;
}

.delete {
  padding: 8px 14px;
  border-color: #fecaca;
  border-radius: 9px;
  color: #dc2626;
  transition: 0.15s ease;
}

.delete:hover:not(:disabled) {
  border-color: #ef4444;
  background: #fff1f2;
}

.status {
  align-items: center;
  margin-top: 26px !important;
  padding: 19px 21px !important;
  border-radius: 14px !important;
}

.status i {
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
}

.empty {
  padding: 52px 24px;
  border-radius: 14px;
  background: #fafbfc;
}

@media (max-width: 760px) {
  .page { padding: 18px; }
  .header, .card { padding: 22px; }
  .create-row { grid-template-columns: 1fr; }
  .primary { width: 100%; }
  .table-wrap { overflow-x: auto; }
  .table-wrap table { min-width: 650px; }
}
</style>
