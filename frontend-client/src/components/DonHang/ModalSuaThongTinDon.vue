<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { loadAdministrativeData, getWardsForProvince } from '@/utils/diaGioiHanhChinh.js'
import { calculateGhnShippingFee } from '@/services/ghnService.js'
import { showAppAlert, showAppConfirm } from '@/utils/thongBaoDialog.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'updated'])

// Form fields
const tenNguoiNhan = ref('')
const soDienThoaiNhan = ref('')
const selectedProvince = ref('')
const selectedWard = ref('')
const streetAddress = ref('')
const ghiChu = ref('')

// Dropdowns data
const provincesList = ref([])
const wardsList = ref([])

// Shipping calculation state
const loadingShipping = ref(false)
const calculatedFee = ref(null)
const shippingMessage = ref('')
const submitting = ref(false)
const formErrors = ref({})

// Initial original values from order
const originalShippingFee = computed(() => {
  return Number(props.order?.phiVanChuyen || props.order?.phiShip || 0)
})

// Kiểm tra đơn đã thanh toán hay chưa (Chuyển khoản / VNPAY)
const isPaidOrder = computed(() => {
  if (!props.order) return false
  return props.order.trangThaiThanhToan === 1
})

// Tiền hàng sau voucher (không bao gồm tiền ship)
const merchandiseSubtotal = computed(() => {
  if (!props.order) return 0
  const total = Number(props.order.tienSauGiamGia || props.order.tongTienSauGiam || props.order.tongTien || 0)
  const ship = originalShippingFee.value
  return Math.max(0, total - ship)
})

// Tổng tiền mới thu COD nếu là đơn COD
const newCodTotal = computed(() => {
  const ship = calculatedFee.value != null ? calculatedFee.value : originalShippingFee.value
  return merchandiseSubtotal.value + ship
})

// Kiểm tra tính hợp lệ cước phí theo hình thức thanh toán
const isFeeAllowed = computed(() => {
  if (!isPaidOrder.value) {
    // Đơn COD: Địa chỉ nào cũng hợp lệ
    return true
  }
  // Đơn đã thanh toán: Cước mới phải <= Cước ban đầu
  if (calculatedFee.value === null) return true
  return calculatedFee.value <= originalShippingFee.value
})

// Kiểm tra xem đã thay đổi thông tin so với ban đầu chưa
const isAddressChanged = computed(() => {
  const currentFull = [streetAddress.value, selectedWard.value, selectedProvince.value].filter(Boolean).join(', ').trim().toLowerCase()
  const oldFull = (props.order?.diaChiNhanHang || '').trim().toLowerCase()
  return currentFull !== oldFull
})

// Tải danh sách tỉnh thành
onMounted(async () => {
  try {
    const data = await loadAdministrativeData()
    provincesList.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Lỗi tải danh mục tỉnh thành:', err)
  }
})

// Phân tích và điền sẵn thông tin ban đầu của đơn hàng
const initFormFromOrder = async () => {
  if (!props.order) return

  tenNguoiNhan.value = props.order.tenKhachHang || props.order.tenKhachHangHienThi || ''
  soDienThoaiNhan.value = props.order.soDienThoaiKhachHang || props.order.soDienThoaiHienThi || ''
  ghiChu.value = ''
  formErrors.value = {}

  // Reset tính phí
  calculatedFee.value = originalShippingFee.value
  shippingMessage.value = ''

  const rawAddress = (props.order.diaChiNhanHang || '').trim()
  if (!rawAddress) {
    selectedProvince.value = ''
    selectedWard.value = ''
    streetAddress.value = ''
    wardsList.value = []
    return
  }

  // Tách địa chỉ để thử match với Tỉnh/Thành và Phường/Xã
  const parts = rawAddress.split(',').map(s => s.trim()).filter(Boolean)
  if (parts.length >= 3) {
    const provPart = parts[parts.length - 1]
    const wardPart = parts[parts.length - 2]
    const streetPart = parts.slice(0, parts.length - 2).join(', ')

    // Tìm tỉnh trong danh sách
    const matchedProv = provincesList.value.find(p => {
      const pName = (p.name || '').toLowerCase()
      const partLower = provPart.toLowerCase()
      return pName.includes(partLower) || partLower.includes(pName)
    })

    if (matchedProv) {
      selectedProvince.value = matchedProv.name
      wardsList.value = await getWardsForProvince(matchedProv.name)
      const matchedWard = wardsList.value.find(w => {
        const wName = (w.name || w || '').toLowerCase()
        const wardLower = wardPart.toLowerCase()
        return wName.includes(wardLower) || wardLower.includes(wName)
      })
      if (matchedWard) {
        selectedWard.value = matchedWard.name || matchedWard
      } else {
        selectedWard.value = ''
      }
      streetAddress.value = streetPart
    } else {
      selectedProvince.value = ''
      selectedWard.value = ''
      streetAddress.value = rawAddress
    }
  } else {
    streetAddress.value = rawAddress
  }
}

// Watch khi modal mở ra
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    initFormFromOrder()
  }
})

// Khi thay đổi Tỉnh / Thành phố
const handleProvinceChange = async () => {
  selectedWard.value = ''
  wardsList.value = []
  calculatedFee.value = null
  shippingMessage.value = ''
  if (selectedProvince.value) {
    wardsList.value = await getWardsForProvince(selectedProvince.value)
  }
  updateShippingFee()
}

// Khi thay đổi Phường / Xã
const handleWardChange = () => {
  updateShippingFee()
}

// Tính phí vận chuyển thực tế qua GHN
const updateShippingFee = async () => {
  if (!selectedProvince.value) {
    calculatedFee.value = null
    shippingMessage.value = ''
    return
  }

  loadingShipping.value = true
  try {
    const fullAddr = [streetAddress.value, selectedWard.value, selectedProvince.value].filter(Boolean).join(', ')
    const res = await calculateGhnShippingFee({
      provinceName: selectedProvince.value,
      wardName: selectedWard.value || '',
      fullAddress: fullAddr,
      weightGram: 300,
      insuranceValue: merchandiseSubtotal.value
    })

    if (res && res.fee != null) {
      calculatedFee.value = Number(res.fee)
      shippingMessage.value = res.regionDescription || 'Tính cước GHN thành công'
    } else {
      calculatedFee.value = originalShippingFee.value
    }
  } catch (err) {
    console.error('Lỗi tính cước GHN:', err)
    calculatedFee.value = originalShippingFee.value
  } finally {
    loadingShipping.value = false
  }
}

// Validate form
const validateForm = () => {
  const errs = {}
  if (!tenNguoiNhan.value.trim()) {
    errs.tenNguoiNhan = 'Vui lòng nhập tên người nhận hàng'
  }
  const phone = soDienThoaiNhan.value.trim()
  if (!phone) {
    errs.soDienThoaiNhan = 'Vui lòng nhập số điện thoại'
  } else if (!/^(0|\+84)[3|5|7|8|9][0-9]{8}$/.test(phone.replace(/\s+/g, ''))) {
    errs.soDienThoaiNhan = 'Số điện thoại không đúng định dạng (10 số)'
  }

  if (!selectedProvince.value) {
    errs.selectedProvince = 'Vui lòng chọn Tỉnh / Thành phố'
  }
  if (!streetAddress.value.trim()) {
    errs.streetAddress = 'Vui lòng nhập số nhà, tên đường'
  }

  // Kiểm tra cước phí cho đơn đã thanh toán
  if (isPaidOrder.value && calculatedFee.value !== null && calculatedFee.value > originalShippingFee.value) {
    errs.fee = `Phí ship địa chỉ mới (${formatCurrency(calculatedFee.value)}) vượt quá phí đã trả (${formatCurrency(originalShippingFee.value)}).`
  }

  formErrors.value = errs
  return Object.keys(errs).length === 0
}

// Đóng modal
const handleClose = () => {
  emit('update:modelValue', false)
}

// Submit cập nhật thông tin
const handleSubmit = async () => {
  if (!validateForm()) return

  // Cảnh báo chỉ được sửa 1 lần duy nhất
  const confirmMsg = `Lưu ý quan trọng: Quý khách chỉ có thể thay đổi thông tin nhận hàng DUY NHẤT 1 LẦN cho đơn hàng này.\n\nBạn có chắc chắn thông tin mới đã hoàn toàn chính xác?`
  const ok = await showAppConfirm(confirmMsg, 'Xác nhận đổi thông tin nhận hàng')
  if (!ok) return

  submitting.value = true
  try {
    const fullAddress = [
      streetAddress.value.trim(),
      selectedWard.value ? selectedWard.value.trim() : '',
      selectedProvince.value ? selectedProvince.value.trim() : ''
    ].filter(Boolean).join(', ')

    const payload = {
      tenNguoiNhan: tenNguoiNhan.value.trim(),
      soDienThoaiNhan: soDienThoaiNhan.value.trim(),
      diaChiNhanHang: fullAddress,
      ghiChu: ghiChu.value.trim(),
      phiVanChuyenMoi: calculatedFee.value != null ? calculatedFee.value : originalShippingFee.value
    }

    const res = await api.put(`/api/hoa-don/code/${props.order.maHoaDon}/khach-sua-thong-tin`, payload)

    await showAppAlert('Cập nhật thông tin nhận đơn thành công!', 'Thành công', 'success')
    emit('updated', res.data)
    handleClose()
  } catch (err) {
    const msg = err.response?.data?.message || err.message || 'Không thể cập nhật thông tin nhận hàng vào lúc này!'
    await showAppAlert(msg, 'Không thể cập nhật', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="modelValue" class="modal-edit-addr-backdrop" @click.self="handleClose">
    <div class="modal-edit-addr-dialog">
      <!-- HEADER -->
      <div class="modal-header">
        <div class="header-left">
          <div class="header-icon-wrap">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </div>
          <div>
            <h3 class="modal-title">Thay đổi thông tin nhận hàng</h3>
            <span class="modal-subtitle">Đơn hàng #{{ order?.maHoaDon }} • Chỉ được sửa 1 lần duy nhất</span>
          </div>
        </div>
        <button type="button" class="btn-close-modal" @click="handleClose" title="Đóng">✕</button>
      </div>

      <!-- BODY -->
      <div class="modal-body">
        <!-- BANNER CẢNH BÁO 1 LẦN DUY NHẤT -->
        <div class="alert-notice-box">
          <span class="alert-icon">ℹ️</span>
          <div class="alert-text">
            <strong>Chính sách đổi địa chỉ:</strong>
            Quý khách chỉ được phép chỉnh sửa thông tin nhận hàng <strong>1 lần duy nhất</strong> khi đơn hàng đang ở trạng thái <em>Chờ xác nhận</em>.
          </div>
        </div>

        <!-- THÔNG TIN THANH TOÁN & RÀNG BUỘC PHÍ SHIP -->
        <div class="payment-policy-card" :class="isPaidOrder ? 'paid-card' : 'cod-card'">
          <div class="policy-header">
            <span class="policy-badge" :class="isPaidOrder ? 'badge-paid' : 'badge-cod'">
              {{ isPaidOrder ? 'ĐÃ THANH TOÁN (VNPAY / CK)' : 'THANH TOÁN KHI NHẬN HÀNG (COD)' }}
            </span>
            <span class="orig-fee">
              Phí ship ban đầu: <strong>{{ formatCurrency(originalShippingFee) }}</strong>
            </span>
          </div>

          <div class="policy-desc" v-if="isPaidOrder">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>
              Vì quý khách đã thanh toán đơn hàng trước đó, <strong>phí ship của địa chỉ mới không được vượt quá {{ formatCurrency(originalShippingFee) }}</strong>.
            </span>
          </div>

          <div class="policy-desc" v-else>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 6L9 17l-5-5"></path>
            </svg>
            <span>
              Đơn hàng thanh toán khi nhận hàng: Quý khách có thể đổi đến bất kỳ địa chỉ nào. Cước vận chuyển và tổng tiền thu COD sẽ tự động cập nhật lại.
            </span>
          </div>
        </div>

        <!-- FORM NHẬP LIỆU -->
        <form @submit.prevent="handleSubmit" class="edit-form-grid">
          <!-- HỌ TÊN & SỐ ĐIỆN THOẠI -->
          <div class="form-row-2">
            <div class="form-group">
              <label class="form-label">Tên người nhận hàng <span class="required">*</span></label>
              <input
                type="text"
                v-model="tenNguoiNhan"
                class="form-control"
                :class="{ 'has-error': formErrors.tenNguoiNhan }"
                placeholder="Ví dụ: Nguyễn Văn A"
              />
              <span v-if="formErrors.tenNguoiNhan" class="error-msg">{{ formErrors.tenNguoiNhan }}</span>
            </div>

            <div class="form-group">
              <label class="form-label">Số điện thoại nhận hàng <span class="required">*</span></label>
              <input
                type="tel"
                v-model="soDienThoaiNhan"
                class="form-control"
                :class="{ 'has-error': formErrors.soDienThoaiNhan }"
                placeholder="Ví dụ: 0912345678"
              />
              <span v-if="formErrors.soDienThoaiNhan" class="error-msg">{{ formErrors.soDienThoaiNhan }}</span>
            </div>
          </div>

          <!-- TỈNH THÀNH & PHƯỜNG XÃ -->
          <div class="form-row-2">
            <div class="form-group">
              <label class="form-label">Tỉnh / Thành phố <span class="required">*</span></label>
              <select
                v-model="selectedProvince"
                @change="handleProvinceChange"
                class="form-select"
                :class="{ 'has-error': formErrors.selectedProvince }"
              >
                <option value="" disabled>-- Chọn Tỉnh / Thành phố --</option>
                <option v-for="p in provincesList" :key="p.name || p" :value="p.name || p">
                  {{ p.name || p }}
                </option>
              </select>
              <span v-if="formErrors.selectedProvince" class="error-msg">{{ formErrors.selectedProvince }}</span>
            </div>

            <div class="form-group">
              <label class="form-label">Phường / Xã</label>
              <select
                v-model="selectedWard"
                @change="handleWardChange"
                class="form-select"
                :disabled="!selectedProvince || wardsList.length === 0"
              >
                <option value="">-- Chọn Phường / Xã --</option>
                <option v-for="w in wardsList" :key="w.name || w" :value="w.name || w">
                  {{ w.name || w }}
                </option>
              </select>
            </div>
          </div>

          <!-- ĐỊA CHỈ CHI TIẾT -->
          <div class="form-group">
            <label class="form-label">Số nhà, tên đường, thôn xóm <span class="required">*</span></label>
            <input
              type="text"
              v-model="streetAddress"
              @blur="updateShippingFee"
              class="form-control"
              :class="{ 'has-error': formErrors.streetAddress }"
              placeholder="Ví dụ: Số 25 ngõ 180 đường Cầu Giấy"
            />
            <span v-if="formErrors.streetAddress" class="error-msg">{{ formErrors.streetAddress }}</span>
          </div>

          <!-- GHI CHÚ GIAO HÀNG -->
          <div class="form-group">
            <label class="form-label">Ghi chú giao hàng mới (Tùy chọn)</label>
            <input
              type="text"
              v-model="ghiChu"
              class="form-control"
              placeholder="Ví dụ: Giao giờ hành chính, gọi trước khi đến..."
            />
          </div>

          <!-- KHỐI TÍNH CƯỚC SHIP THỰC TẾ & KẾT QUẢ -->
          <div class="shipping-fee-result-card" :class="isFeeAllowed ? 'fee-valid' : 'fee-invalid'">
            <div class="fee-row">
              <div class="fee-title-group">
                <span class="carrier-icon">🚚</span>
                <div>
                  <strong>Cước phí vận chuyển GHN địa chỉ mới:</strong>
                  <div class="fee-sub">{{ shippingMessage || 'Ước tính theo dịch vụ chuyển phát Giao Hàng Nhanh' }}</div>
                </div>
              </div>
              <div class="fee-number-group">
                <span v-if="loadingShipping" class="loading-spin">Đang tính...</span>
                <span v-else class="fee-highlight" :class="{ 'fee-danger': !isFeeAllowed }">
                  {{ calculatedFee != null ? formatCurrency(calculatedFee) : '—' }}
                </span>
              </div>
            </div>

            <!-- Cảnh báo nếu phí mới vượt mức đối với đơn đã thanh toán -->
            <div v-if="isPaidOrder && !isFeeAllowed" class="fee-error-banner">
              ⚠️ <strong>Không được phép:</strong> Phí ship mới ({{ formatCurrency(calculatedFee) }}) cao hơn cước ban đầu ({{ formatCurrency(originalShippingFee) }}). Vui lòng chọn địa chỉ trong phạm vi cước phí đã thanh toán!
            </div>

            <!-- Thông báo đối với đơn COD -->
            <div v-else-if="!isPaidOrder && calculatedFee != null" class="fee-cod-info">
              💵 <strong>Tổng tiền cần thanh toán khi nhận hàng (COD):</strong>
              <span class="cod-final-highlight">{{ formatCurrency(newCodTotal) }}</span>
            </div>

            <!-- Thông báo thành công cho đơn đã thanh toán -->
            <div v-else-if="isPaidOrder && isFeeAllowed && calculatedFee != null" class="fee-success-banner">
              ✓ Cước phí hợp lệ (Không phát sinh thêm chi phí thanh toán).
            </div>
          </div>
        </form>
      </div>

      <!-- FOOTER ACTIONS -->
      <div class="modal-footer">
        <button type="button" class="btn-cancel" @click="handleClose" :disabled="submitting">
          Hủy bỏ
        </button>
        <button
          type="button"
          class="btn-confirm-save"
          :disabled="submitting || loadingShipping || !isFeeAllowed"
          @click="handleSubmit"
        >
          <span v-if="submitting">Đang lưu...</span>
          <span v-else>Xác nhận cập nhật</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-edit-addr-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-edit-addr-dialog {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 620px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  max-height: 92vh;
  overflow: hidden;
  animation: modalPopIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPopIn {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* HEADER */
.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafbfc;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.header-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
}

.modal-subtitle {
  font-size: 0.825rem;
  color: #64748b;
}

.btn-close-modal {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  transition: all 0.15s;
}

.btn-close-modal:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* BODY */
.modal-body {
  padding: 1.25rem 1.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

/* NOTICE BOX */
.alert-notice-box {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  font-size: 0.85rem;
  color: #92400e;
  line-height: 1.45;
}

.alert-notice-box .alert-icon {
  font-size: 1.1rem;
}

/* PAYMENT POLICY CARD */
.payment-policy-card {
  border-radius: 12px;
  padding: 0.85rem 1.1rem;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.payment-policy-card.paid-card {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.payment-policy-card.cod-card {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.policy-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.policy-badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.badge-paid {
  background: #16a34a;
  color: #ffffff;
}

.badge-cod {
  background: #2563eb;
  color: #ffffff;
}

.orig-fee {
  font-size: 0.825rem;
  color: #475569;
}

.orig-fee strong {
  color: #0f172a;
}

.policy-desc {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-size: 0.825rem;
  color: #334155;
  line-height: 1.4;
}

.policy-desc svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: #16a34a;
}

/* FORM GRID */
.edit-form-grid {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

@media (max-width: 540px) {
  .form-row-2 {
    grid-template-columns: 1fr;
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: #334155;
}

.required {
  color: #ef4444;
}

.form-control,
.form-select {
  height: 42px;
  padding: 0 0.85rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 9px;
  font-size: 0.875rem;
  color: #0f172a;
  background: #ffffff;
  transition: all 0.15s;
  outline: none;
}

.form-control:focus,
.form-select:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.form-control.has-error,
.form-select.has-error {
  border-color: #ef4444;
  background: #fef2f2;
}

.error-msg {
  font-size: 0.75rem;
  color: #ef4444;
  margin-top: 0.15rem;
}

/* SHIPPING FEE RESULT CARD */
.shipping-fee-result-card {
  border-radius: 12px;
  padding: 0.95rem 1.15rem;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  transition: all 0.2s;
}

.shipping-fee-result-card.fee-valid {
  border-color: #cbd5e1;
}

.shipping-fee-result-card.fee-invalid {
  border-color: #fca5a5;
  background: #fff5f5;
}

.fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.fee-title-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.carrier-icon {
  font-size: 1.35rem;
}

.fee-title-group strong {
  font-size: 0.875rem;
  color: #1e293b;
}

.fee-sub {
  font-size: 0.75rem;
  color: #64748b;
}

.fee-highlight {
  font-size: 1.1rem;
  font-weight: 700;
  color: #2563eb;
}

.fee-highlight.fee-danger {
  color: #dc2626;
}

.fee-error-banner {
  background: #fee2e2;
  color: #b91c1c;
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  font-size: 0.825rem;
  line-height: 1.4;
}

.fee-cod-info {
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  color: #1e40af;
  border-radius: 8px;
  padding: 0.65rem 0.85rem;
  font-size: 0.825rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.cod-final-highlight {
  font-size: 1rem;
  font-weight: 800;
  color: #2563eb;
}

.fee-success-banner {
  color: #15803d;
  font-size: 0.8rem;
  font-weight: 600;
}

/* FOOTER */
.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #f1f5f9;
  background: #fafbfc;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-cancel {
  padding: 0.65rem 1.25rem;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  border-radius: 9px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel:hover:not(:disabled) {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-confirm-save {
  padding: 0.65rem 1.45rem;
  border: none;
  background: #2563eb;
  color: #ffffff;
  border-radius: 9px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.25);
}

.btn-confirm-save:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.btn-confirm-save:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}
</style>
