<script setup>
const props = defineProps({
  paymentMethod: {
    type: String,
    default: 'COD'
  }
})

const emit = defineEmits(['update:paymentMethod'])
</script>

<template>
  <div class="checkout-card">
    <h3 class="card-title">Phương thức thanh toán <span class="star">*</span></h3>

    <div class="payment-methods-stack">
      <!-- COD -->
      <label
        class="pay-method-card"
        :class="{ selected: paymentMethod === 'COD' }"
        @click="emit('update:paymentMethod', 'COD')"
      >
        <input
          type="radio"
          name="payMethod"
          value="COD"
          :checked="paymentMethod === 'COD'"
          class="radio-dot"
        />
        <div class="pay-text">
          <strong>Thanh toán khi nhận hàng (COD)</strong>
          <p>Khách hàng thanh toán bằng tiền mặt khi shipper giao hàng tới.</p>
        </div>
      </label>

      <!-- VNPAY -->
      <label
        class="pay-method-card"
        :class="{ selected: paymentMethod === 'VNPAY' }"
        @click="emit('update:paymentMethod', 'VNPAY')"
      >
        <input
          type="radio"
          name="payMethod"
          value="VNPAY"
          :checked="paymentMethod === 'VNPAY'"
          class="radio-dot"
        />
        <div class="pay-text">
          <strong>Thanh toán qua VNPAY</strong>
          <p>Hệ thống sẽ mở một cửa sổ nhỏ để bạn thanh toán an toàn.</p>
        </div>
      </label>
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

.card-title {
  font-size: 16px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #ecd8af;
}

.star {
  color: #e11d48;
}

.payment-methods-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pay-method-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 18px;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.pay-method-card:hover {
  border-color: #d2a764;
}

.pay-method-card.selected {
  border-color: #496883;
  background: #f8fafc;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.08);
}

.radio-dot {
  margin-top: 4px;
  accent-color: #496883;
}

.pay-text strong {
  display: block;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 2px;
}

.pay-text p {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}
</style>

