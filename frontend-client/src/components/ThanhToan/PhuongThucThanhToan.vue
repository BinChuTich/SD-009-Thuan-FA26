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
          <strong>Thanh toán qua cổng VNPAY (Chuyển khoản / Quét QR)</strong>
          <p>Thanh toán bảo mật và tiện lợi qua ứng dụng ngân hàng hoặc cổng VNPAY.</p>
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
}

.card-title {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.star {
  color: #ef4444;
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
  padding: 16px 18px;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #ffffff;
}

.pay-method-card:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.pay-method-card.selected {
  border-color: #d2a764;
  background: #fdfbf7;
  box-shadow: 0 4px 12px rgba(210, 167, 100, 0.15);
}

.radio-dot {
  margin-top: 3px;
  cursor: pointer;
  accent-color: #d2a764;
  width: 17px;
  height: 17px;
}

.pay-text strong {
  display: block;
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.pay-text p {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.4;
}
</style>
