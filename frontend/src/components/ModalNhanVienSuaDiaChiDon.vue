<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '@/api.js'
import { getProvinces, getWardsByProvince } from '@/services/provincesApi.js'
import { calculateGhnShippingFee } from '@/services/ghnService.js'
import { getCurrentUser } from '@/utils/auth.js'
import { showAlert, showConfirm, showToast } from '@/utils/dialog.js'

const props = defineProps({
  modelValue: Boolean,
  order: { type: Object, required: true }
})
const emit = defineEmits(['update:modelValue', 'updated'])

const provinces = ref([])
const wards = ref([])
const savedAddresses = ref([])
const selectedSavedId = ref('current')
const calculating = ref(false)
const submitting = ref(false)
const shippingMessage = ref('')
const newShippingFee = ref(0)
const form = ref({ receiver: '', phone: '', provinceCode: '', province: '', wardCode: '', ward: '', detail: '', note: '' })

const originalShippingFee = computed(() => Number(props.order?.summary?.shippingFee || 0))
const isPaid = computed(() => props.order?.paymentHistory?.status === 'Đã thanh toán')
const productSubtotal = computed(() => Math.max(0,
  Number(props.order?.summary?.totalPayment || 0) - originalShippingFee.value))

const parseAddress = (raw = '') => {
  const parts = String(raw).split(',').map(x => x.trim()).filter(Boolean)
  return {
    detail: parts.length > 2 ? parts.slice(0, -2).join(', ') : (parts[0] || ''),
    ward: parts.length > 1 ? parts[parts.length - 2] : '',
    province: parts.length > 1 ? parts[parts.length - 1] : ''
  }
}

const hydrateCurrentOrder = async () => {
  const parsed = parseAddress(props.order?.delivery?.address)
  const province = provinces.value.find(p =>
    String(p.name || '').toLowerCase() === parsed.province.toLowerCase())
  form.value = {
    receiver: props.order?.customer?.name || '',
    phone: props.order?.customer?.phone || '',
    provinceCode: province?.code || '',
    province: parsed.province,
    wardCode: '',
    ward: parsed.ward,
    detail: parsed.detail,
    note: ''
  }
  newShippingFee.value = originalShippingFee.value
  if (province?.code) wards.value = await getWardsByProvince(province.code)
}

const loadAddresses = async () => {
  savedAddresses.value = []
  const customerId = props.order?.customer?.id
  if (!customerId) return
  try {
    const res = await api.get(`/api/khach-hang/${customerId}/dia-chi`)
    savedAddresses.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.warn('Không tải được địa chỉ đã lưu của khách:', error)
  }
}

const chooseSavedAddress = async () => {
  if (selectedSavedId.value === 'current') return hydrateCurrentOrder()
  const addr = savedAddresses.value.find(x => String(x.id) === String(selectedSavedId.value))
  if (!addr) return
  const province = provinces.value.find(p => String(p.name || '').toLowerCase() === String(addr.thanhPho || '').toLowerCase())
  form.value.receiver = addr.tenNguoiNhan || props.order?.customer?.name || ''
  form.value.phone = addr.soDienThoaiNhan || props.order?.customer?.phone || ''
  form.value.provinceCode = province?.code || ''
  form.value.province = addr.thanhPho || ''
  form.value.ward = addr.phuong || ''
  form.value.detail = addr.diaChiCuThe || ''
  wards.value = province?.code ? await getWardsByProvince(province.code) : []
  const ward = wards.value.find(w => String(w.name || '').toLowerCase() === form.value.ward.toLowerCase())
  form.value.wardCode = ward?.code || ''
  await recalculateShipping()
}

const changeProvince = async () => {
  const province = provinces.value.find(p => String(p.code) === String(form.value.provinceCode))
  form.value.province = province?.name || ''
  form.value.wardCode = ''
  form.value.ward = ''
  wards.value = province?.code ? await getWardsByProvince(province.code) : []
}

const changeWard = async () => {
  const ward = wards.value.find(w => String(w.code) === String(form.value.wardCode))
  form.value.ward = ward?.name || ''
  await recalculateShipping()
}

const recalculateShipping = async () => {
  if (!form.value.province || !form.value.ward || !form.value.detail.trim()) return
  calculating.value = true
  try {
    const fullAddress = [form.value.detail, form.value.ward, form.value.province].filter(Boolean).join(', ')
    const result = await calculateGhnShippingFee({
      provinceName: form.value.province,
      wardName: form.value.ward,
      fullAddress,
      weightGram: 300,
      insuranceValue: productSubtotal.value,
      orderSubtotal: productSubtotal.value,
      channel: 'ONLINE'
    })
    if (result?.fee != null) {
      newShippingFee.value = Number(result.fee)
      shippingMessage.value = result.regionDescription || 'Đã tính lại phí vận chuyển'
    }
  } catch (error) {
    showToast('Không tính được phí vận chuyển cho địa chỉ này.', 'warning')
  } finally {
    calculating.value = false
  }
}

const submit = async () => {
  if (!form.value.receiver.trim() || !form.value.phone.trim() || !form.value.province || !form.value.ward || !form.value.detail.trim()) {
    return showAlert({ title: 'Thiếu thông tin', message: 'Vui lòng nhập đủ người nhận, số điện thoại và địa chỉ giao hàng.' })
  }
  if (!/^(0|\+84)[35789][0-9]{8}$/.test(form.value.phone.replace(/\s+/g, ''))) {
    return showAlert({ title: 'Số điện thoại không hợp lệ', message: 'Số điện thoại người nhận phải đúng định dạng Việt Nam.' })
  }
  await recalculateShipping()
  if (isPaid.value && newShippingFee.value > originalShippingFee.value) {
    return showAlert({
      title: 'Không thể đổi địa chỉ', type: 'error',
      message: 'Đơn đã thanh toán trước nên phí vận chuyển mới không được cao hơn phí đã thanh toán.'
    })
  }
  const accepted = await showConfirm({
    title: 'Xác nhận sửa địa chỉ', type: 'warning', confirmText: 'Cập nhật địa chỉ',
    message: 'Nhân viên chỉ được sửa thông tin nhận hàng của đơn này 1 lần. Bạn có chắc chắn tiếp tục?'
  })
  if (!accepted) return
  submitting.value = true
  try {
    const actor = getCurrentUser()?.tenNhanVien || getCurrentUser()?.tenTaiKhoan || 'Nhân viên bán hàng'
    const payload = {
      tenNguoiNhan: form.value.receiver.trim(),
      soDienThoaiNhan: form.value.phone.trim(),
      diaChiNhanHang: [form.value.detail.trim(), form.value.ward, form.value.province].join(', '),
      ghiChu: form.value.note.trim(),
      phiVanChuyenMoi: newShippingFee.value
    }
    const res = await api.put(
      `/api/hoa-don/code/${encodeURIComponent(props.order.code)}/nhan-vien-sua-thong-tin`,
      payload,
      { params: { tenNhanVien: actor } }
    )
    showToast('Đã cập nhật địa chỉ giao hàng của đơn.', 'success')
    emit('updated', res.data)
    emit('update:modelValue', false)
  } catch (error) {
    await showAlert({
      title: 'Không thể cập nhật', type: 'error',
      message: error?.response?.data?.message || 'Cập nhật địa chỉ thất bại.'
    })
  } finally {
    submitting.value = false
  }
}

watch(() => props.modelValue, async open => {
  if (!open) return
  provinces.value = await getProvinces()
  selectedSavedId.value = 'current'
  shippingMessage.value = ''
  await hydrateCurrentOrder()
  await loadAddresses()
})
onMounted(async () => { provinces.value = await getProvinces() })
</script>

<template>
  <div v-if="modelValue" class="staff-address-overlay" @click.self="$emit('update:modelValue', false)">
    <section class="staff-address-modal">
      <header>
        <div><h3>Sửa thông tin nhận hàng</h3><p>Đơn {{ order.code }} · Nhân viên được sửa 1 lần</p></div>
        <button type="button" @click="$emit('update:modelValue', false)">×</button>
      </header>
      <div class="policy">Thao tác chỉ áp dụng khi đơn Online đang ở trạng thái <b>Chờ xác nhận</b> và được ghi vào lịch sử đơn hàng.</div>
      <div class="field full">
        <label>Chọn địa chỉ</label>
        <select v-model="selectedSavedId" @change="chooseSavedAddress">
          <option value="current">Địa chỉ hiện tại của đơn</option>
          <option v-for="addr in savedAddresses" :key="addr.id" :value="addr.id">
            {{ [addr.diaChiCuThe, addr.phuong, addr.thanhPho].filter(Boolean).join(', ') }}
          </option>
        </select>
      </div>
      <div class="form-grid">
        <div class="field"><label>Người nhận</label><input v-model="form.receiver" /></div>
        <div class="field"><label>Số điện thoại</label><input v-model="form.phone" /></div>
        <div class="field"><label>Tỉnh / Thành phố</label><select v-model="form.provinceCode" @change="changeProvince"><option value="">Chọn tỉnh/thành</option><option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option></select></div>
        <div class="field"><label>Phường / Xã</label><select v-model="form.wardCode" @change="changeWard"><option value="">Chọn phường/xã</option><option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option></select></div>
        <div class="field full"><label>Địa chỉ cụ thể</label><input v-model="form.detail" @blur="recalculateShipping" placeholder="Số nhà, ngõ, tên đường..." /></div>
        <div class="field full"><label>Ghi chú bổ sung</label><textarea v-model="form.note" rows="2" /></div>
      </div>
      <div class="shipping-box">
        <span>Phí vận chuyển mới</span><b>{{ newShippingFee.toLocaleString('vi-VN') }}đ</b>
        <small>{{ calculating ? 'Đang tính phí...' : shippingMessage }}</small>
      </div>
      <footer>
        <button class="cancel" type="button" @click="$emit('update:modelValue', false)">Hủy</button>
        <button class="save" type="button" :disabled="submitting || calculating" @click="submit">{{ submitting ? 'Đang cập nhật...' : 'Cập nhật địa chỉ' }}</button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.staff-address-overlay{position:fixed;inset:0;background:#0f172a80;display:grid;place-items:center;z-index:10050;padding:20px}.staff-address-modal{width:min(720px,96vw);max-height:92vh;overflow:auto;background:#fff;border-radius:16px;box-shadow:0 24px 70px #0f172a55;padding:22px}.staff-address-modal header{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:1px solid #e2e8f0;padding-bottom:14px}.staff-address-modal h3{margin:0;color:#0f172a;font-size:20px}.staff-address-modal header p{margin:5px 0 0;color:#64748b}.staff-address-modal header button{border:0;background:#f1f5f9;border-radius:9px;width:34px;height:34px;font-size:24px;cursor:pointer}.policy{margin:16px 0;padding:12px 14px;border-radius:10px;background:#eff6ff;color:#1e40af;font-size:13px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.field{display:flex;flex-direction:column;gap:6px}.field.full{grid-column:1/-1;margin-bottom:14px}.field label{font-size:13px;font-weight:700;color:#334155}.field input,.field select,.field textarea{border:1px solid #cbd5e1;border-radius:9px;padding:10px 11px;font:inherit;outline:none}.field input:focus,.field select:focus,.field textarea:focus{border-color:#2563eb;box-shadow:0 0 0 3px #2563eb1a}.shipping-box{display:grid;grid-template-columns:1fr auto;gap:4px;margin-top:16px;padding:13px 15px;border-radius:10px;background:#f8fafc}.shipping-box b{color:#dc2626}.shipping-box small{grid-column:1/-1;color:#64748b}.staff-address-modal footer{display:flex;justify-content:flex-end;gap:10px;margin-top:20px}.staff-address-modal footer button{border:0;border-radius:9px;padding:10px 18px;font-weight:700;cursor:pointer}.cancel{background:#e2e8f0;color:#334155}.save{background:#2563eb;color:#fff}.save:disabled{opacity:.55;cursor:not-allowed}@media(max-width:640px){.form-grid{grid-template-columns:1fr}.field.full{grid-column:auto}}
</style>
