<script setup>
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
  'update:streetAddress'
])
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
        <div class="input-field">
          <label>Họ và tên người nhận <span class="star">*</span></label>
          <input
            :value="customerName"
            @input="emit('update:customerName', $event.target.value)"
            type="text"
            placeholder="Nhập họ và tên..."
            class="form-control"
          />
        </div>

        <div class="input-field">
          <label>Số điện thoại <span class="star">*</span></label>
          <input
            :value="customerPhone"
            @input="emit('update:customerPhone', $event.target.value)"
            type="tel"
            placeholder="Nhập số điện thoại..."
            class="form-control"
          />
        </div>
      </div>

      <div class="input-field full">
        <label>Email <span class="star">*</span> (Dùng nhận hóa đơn &amp; tra cứu đơn)</label>
        <input
          :value="customerEmail"
          @input="emit('update:customerEmail', $event.target.value)"
          type="email"
          placeholder="Nhập địa chỉ email của bạn"
          class="form-control"
        />
      </div>

      <!-- 3 Selects địa giới -->
      <div class="form-three-col">
        <div class="input-field">
          <label>Tỉnh/Thành phố <span class="star">*</span></label>
          <select
            :value="selectedProvince"
            @change="emit('update:selectedProvince', $event.target.value)"
            class="form-control select-box"
          >
            <option value="Thành phố Hà Nội">Thành phố Hà Nội</option>
            <option value="Thành phố Hồ Chí Minh">Thành phố Hồ Chí Minh</option>
            <option value="Thành phố Đà Nẵng">Thành phố Đà Nẵng</option>
            <option value="Tỉnh Hải Dương">Tỉnh Hải Dương</option>
            <option value="Tỉnh Bắc Ninh">Tỉnh Bắc Ninh</option>
          </select>
        </div>

        <div class="input-field">
          <label>Quận/Huyện <span class="star">*</span></label>
          <select
            :value="selectedDistrict"
            @change="emit('update:selectedDistrict', $event.target.value)"
            class="form-control select-box"
          >
            <option value="Quận Hoàn Kiếm">Quận Hoàn Kiếm</option>
            <option value="Quận Cầu Giấy">Quận Cầu Giấy</option>
            <option value="Quận Nam Từ Liêm">Quận Nam Từ Liêm</option>
            <option value="Quận 1">Quận 1</option>
            <option value="Quận 3">Quận 3</option>
          </select>
        </div>

        <div class="input-field">
          <label>Phường/Xã <span class="star">*</span></label>
          <select
            :value="selectedWard"
            @change="emit('update:selectedWard', $event.target.value)"
            class="form-control select-box"
          >
            <option value="Phường Cửa Đông">Phường Cửa Đông</option>
            <option value="Phường Dịch Vọng">Phường Dịch Vọng</option>
            <option value="Phường Mễ Trì">Phường Mễ Trì</option>
            <option value="Phường Bến Nghé">Phường Bến Nghé</option>
          </select>
        </div>
      </div>

      <!-- Địa chỉ cụ thể -->
      <div class="input-field full">
        <label>Địa chỉ cụ thể <span class="star">*</span></label>
        <input
          :value="streetAddress"
          @input="emit('update:streetAddress', $event.target.value)"
          type="text"
          placeholder="Ví dụ: 123 ngõ abc"
          class="form-control"
        />
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

.card-header-line h3,
.card-title {
  font-size: 16px;
  font-weight: 800;
  color: #496883;
  margin: 0;
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
