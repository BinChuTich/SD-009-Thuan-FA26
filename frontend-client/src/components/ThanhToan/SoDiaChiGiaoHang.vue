<script setup>
import { ref, watch, onMounted } from 'vue'
import { FALLBACK_PROVINCES, loadAdministrativeData, getWardsForProvince } from '@/utils/diaGioiHanhChinh.js'

const props = defineProps({
  savedAddresses: {
    type: Array,
    default: () => []
  },
  selectedAddressId: {
    type: [Number, String],
    default: 1
  },
  isCustomAddress: {
    type: Boolean,
    default: false
  },
  customerName: {
    type: String,
    default: ''
  },
  customerPhone: {
    type: String,
    default: ''
  },
  customerEmail: {
    type: String,
    default: ''
  },
  selectedProvince: {
    type: String,
    default: ''
  },
  selectedDistrict: {
    type: String,
    default: ''
  },
  selectedWard: {
    type: String,
    default: ''
  },
  streetAddress: {
    type: String,
    default: ''
  },
  fullDeliveryAddress: {
    type: String,
    default: ''
  },
  errors: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits([
  'selectSavedAddress',
  'selectCustomAddress',
  'update:customerName',
  'update:customerPhone',
  'update:customerEmail',
  'update:selectedProvince',
  'update:selectedDistrict',
  'update:selectedWard',
  'update:streetAddress',
  'clearError'
])

const provincesList = ref(FALLBACK_PROVINCES.map(name => ({ name })))
const currentWards = ref([])

const reloadWards = async (provName) => {
  if (!provName) {
    currentWards.value = []
    return
  }
  const wards = await getWardsForProvince(provName)
  currentWards.value = wards
}

onMounted(async () => {
  const allData = await loadAdministrativeData()
  if (allData && allData.length > 0) {
    provincesList.value = allData
  }
  await reloadWards(props.selectedProvince)
})

watch(() => props.selectedProvince, async (newProv) => {
  await reloadWards(newProv)
})

const onProvinceChange = async (newProvince) => {
  emit('update:selectedProvince', newProvince)
  emit('update:selectedDistrict', '')
  const wards = await getWardsForProvince(newProvince)
  currentWards.value = wards
  if (wards.length > 0) {
    emit('update:selectedWard', wards[0].name)
  } else {
    emit('update:selectedWard', '')
  }
}
</script>

<template>
  <div class="shipping-section-box">
    <!-- KHỐI 1: CHỌN ĐỊA CHỈ ĐÃ LƯU -->
    <div class="checkout-card">
      <div class="card-header-line">
        <h3>Chọn địa chỉ đã lưu</h3>
        <button class="btn-refresh-text" @click="emit('selectSavedAddress', savedAddresses[0])">
          🔄 Mặc định
        </button>
      </div>

      <div class="saved-address-list">
        <label
          v-for="addr in savedAddresses"
          :key="addr.id"
          class="saved-addr-box"
          :class="{ selected: selectedAddressId === addr.id }"
          @click="emit('selectSavedAddress', addr)"
        >
          <input
            type="radio"
            name="savedAddr"
            :checked="selectedAddressId === addr.id"
            class="radio-dot"
          />
          <div class="addr-info-text">
            <div class="name-phone">
              <strong>{{ addr.name }}</strong> • <span>{{ addr.phone }}</span>
            </div>
            <div class="addr-str">{{ addr.address }}</div>
          </div>
        </label>

        <!-- Link Giao đến địa chỉ khác -->
        <button
          class="btn-other-address"
          :class="{ active: isCustomAddress }"
          @click="emit('selectCustomAddress')"
        >
          + Giao đến địa chỉ khác...
        </button>
      </div>
    </div>

    <!-- KHỐI 2: THÔNG TIN NGƯỜI NHẬN -->
    <div class="checkout-card">
      <h3 class="card-title">Thông tin người nhận</h3>

      <div class="form-two-col">
        <div class="input-field" :class="{ 'has-error': errors?.customerName }">
          <label>Họ và tên người nhận <span class="star">*</span></label>
          <input
            :value="customerName"
            @input="emit('update:customerName', $event.target.value); emit('clearError', 'customerName')"
            type="text"
            placeholder="Nhập họ và tên..."
            class="form-control"
            :class="{ 'input-error': errors?.customerName }"
          />
          <span v-if="errors?.customerName" class="field-error-text">{{ errors.customerName }}</span>
        </div>

        <div class="input-field" :class="{ 'has-error': errors?.customerPhone }">
          <label>Số điện thoại <span class="star">*</span></label>
          <input
            :value="customerPhone"
            @input="emit('update:customerPhone', $event.target.value); emit('clearError', 'customerPhone')"
            type="tel"
            placeholder="Nhập số điện thoại (ví dụ: 0988776655)..."
            class="form-control"
            :class="{ 'input-error': errors?.customerPhone }"
          />
          <span v-if="errors?.customerPhone" class="field-error-text">{{ errors.customerPhone }}</span>
        </div>
      </div>

      <div class="input-field full" :class="{ 'has-error': errors?.customerEmail }">
        <label>Email <span class="star">*</span> (Dùng nhận hóa đơn &amp; tra cứu đơn)</label>
        <input
          :value="customerEmail"
          @input="emit('update:customerEmail', $event.target.value); emit('clearError', 'customerEmail')"
          type="email"
          placeholder="Nhập địa chỉ email của bạn..."
          class="form-control"
          :class="{ 'input-error': errors?.customerEmail }"
        />
        <span v-if="errors?.customerEmail" class="field-error-text">{{ errors.customerEmail }}</span>
      </div>

      <!-- 2 Selects địa giới theo chuẩn 2 cấp hành chính mới (Tỉnh/Thành phố & Phường/Xã) -->
      <div class="form-two-col">
        <div class="input-field" :class="{ 'has-error': errors?.selectedProvince }">
          <label>Tỉnh/Thành phố <span class="star">*</span></label>
          <select
            :value="selectedProvince"
            @change="onProvinceChange($event.target.value); emit('clearError', 'selectedProvince')"
            class="form-control select-box"
            :class="{ 'input-error': errors?.selectedProvince }"
          >
            <option value="">-- Chọn Tỉnh/Thành phố --</option>
            <option
              v-for="prov in provincesList"
              :key="prov.code || prov.name"
              :value="prov.name"
            >
              {{ prov.name }}
            </option>
          </select>
          <span v-if="errors?.selectedProvince" class="field-error-text">{{ errors.selectedProvince }}</span>
        </div>

        <div class="input-field" :class="{ 'has-error': errors?.selectedWard }">
          <label>Phường/Xã <span class="star">*</span></label>
          <select
            :value="selectedWard"
            @change="emit('update:selectedWard', $event.target.value); emit('clearError', 'selectedWard')"
            class="form-control select-box"
            :class="{ 'input-error': errors?.selectedWard }"
            :disabled="!selectedProvince"
          >
            <option value="">-- Chọn Phường/Xã --</option>
            <option
              v-for="w in currentWards"
              :key="w.code || w.name"
              :value="w.name"
            >
              {{ w.name }}
            </option>
          </select>
          <span v-if="errors?.selectedWard" class="field-error-text">{{ errors.selectedWard }}</span>
        </div>
      </div>

      <!-- Địa chỉ cụ thể -->
      <div class="input-field full" :class="{ 'has-error': errors?.streetAddress }">
        <label>Địa chỉ cụ thể <span class="star">*</span></label>
        <input
          :value="streetAddress"
          @input="emit('update:streetAddress', $event.target.value); emit('clearError', 'streetAddress')"
          type="text"
          placeholder="Ví dụ: Số 123 ngõ abc, đường xyz"
          class="form-control"
          :class="{ 'input-error': errors?.streetAddress }"
        />
        <span v-if="errors?.streetAddress" class="field-error-text">{{ errors.streetAddress }}</span>
      </div>

      <div class="delivery-helper-box">
        <span>Giao đến: {{ fullDeliveryAddress }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkout-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.05);
  margin-bottom: 24px;
}

.card-header-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 2px solid #ecd8af;
  margin-bottom: 18px;
}

.card-header-line h3 {
  font-family: 'Be Vietnam Pro', sans-serif;
  font-size: 16px;
  font-weight: 800;
  color: #496883;
  margin: 0;
}

.card-title {
  font-family: 'Be Vietnam Pro', sans-serif;
  font-size: 16.5px;
  font-weight: 800;
  color: #496883;
  margin-top: -8px;
  margin-bottom: 18px;
  display: block;
}

.btn-refresh-text {
  background: transparent;
  border: none;
  color: #d2a764;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}

.saved-address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.saved-addr-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.saved-addr-box:hover {
  border-color: #d2a764;
}

.saved-addr-box.selected {
  border-color: #496883;
  background: #f8fafc;
}

.radio-dot {
  margin-top: 3px;
  accent-color: #496883;
}

.addr-info-text {
  font-size: 13px;
}

.name-phone {
  margin-bottom: 2px;
}

.addr-str {
  color: #64748b;
}

.btn-other-address {
  background: transparent;
  border: 1px dashed #cbd5e1;
  padding: 10px;
  border-radius: 8px;
  color: #496883;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  text-align: left;
}

.btn-other-address:hover,
.btn-other-address.active {
  border-color: #d2a764;
  background: #fffdfa;
}

.form-two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 14px;
}

.form-three-col {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}

.input-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-field.full {
  margin-bottom: 14px;
}

.input-field label {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}

.star {
  color: #e11d48;
}

.form-control {
  padding: 10px 12px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  background: #f8fafc;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #496883;
  background: #ffffff;
}

/* Error styles (Báo đỏ bên dưới trường nhập) */
.input-field.has-error label {
  color: #dc2626;
  font-weight: 700;
}

.form-control.input-error {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
}

.field-error-text {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #dc2626;
  font-weight: 600;
  margin-top: 5px;
  animation: shakeInput 0.3s ease-in-out;
}

.field-error-text::before {
  content: '⚠️';
  font-size: 11px;
}

@keyframes shakeInput {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.delivery-helper-box {
  background: #f1f5f9;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  color: #475569;
  border-left: 3px solid #d2a764;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .form-two-col,
  .form-three-col {
    grid-template-columns: 1fr;
  }
}
</style>

