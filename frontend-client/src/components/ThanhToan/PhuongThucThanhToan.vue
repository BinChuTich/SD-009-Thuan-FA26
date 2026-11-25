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
      <!-- 1. COD -->
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
        <div class="pay-icon-box cod-icon-box">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="1" y="3" width="15" height="13"></rect>
            <polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon>
            <circle cx="5.5" cy="18.5" r="2.5"></circle>
            <circle cx="18.5" cy="18.5" r="2.5"></circle>
          </svg>
        </div>
        <div class="pay-text">
          <div class="title-with-badge">
            <strong>Thanh toán khi nhận hàng (COD)</strong>
            <span class="pay-pill-tag">Phổ biến</span>
          </div>
          <p>Khách hàng thanh toán tiền mặt trực tiếp cho shipper khi nhận hàng.</p>
        </div>
      </label>

      <!-- 2. VIETQR (Napas 247) -->
      <label
        class="pay-method-card"
        :class="{ selected: paymentMethod === 'VIETQR' }"
        @click="emit('update:paymentMethod', 'VIETQR')"
      >
        <input
          type="radio"
          name="payMethod"
          value="VIETQR"
          :checked="paymentMethod === 'VIETQR'"
          class="radio-dot"
        />
        <div class="pay-icon-box vietqr-icon-box">
          <span class="vietqr-mini-badge">VietQR</span>
        </div>
        <div class="pay-text">
          <div class="title-with-badge">
            <strong>Chuyển khoản VietQR (Napas 247)</strong>
            <span class="pay-pill-tag qr-tag">Khuyên dùng</span>
          </div>
          <p>Quét mã QR bằng bất kỳ ứng dụng ngân hàng hoặc ví điện tử (MB, Techcombank, VCB, Momo,...).</p>
        </div>
      </label>

      <!-- 3. VNPAY -->
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
        <div class="pay-icon-box vnpay-icon-box">
          <span class="vnpay-mini-badge">VNPAY</span>
        </div>
        <div class="pay-text">
          <div class="title-with-badge">
            <strong>Cổng thanh toán trực tuyến VNPAY</strong>
          </div>
          <p>Thanh toán bảo mật qua ứng dụng ngân hàng hoặc cổng VNPAY (Thẻ ATM, Internet Banking, QR).</p>
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
  margin-top: 12px;
  cursor: pointer;
  accent-color: #d2a764;
  width: 17px;
  height: 17px;
  flex-shrink: 0;
}

.pay-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cod-icon-box {
  background: #f1f5f9;
  color: #475569;
}

.vietqr-icon-box {
  background: #005baa;
}

.vietqr-mini-badge {
  color: #ffffff;
  font-weight: 900;
  font-size: 10px;
  letter-spacing: 0.5px;
}

.vnpay-icon-box {
  background: #e0f2fe;
}

.vnpay-mini-badge {
  color: #0284c7;
  font-weight: 900;
  font-size: 10px;
  letter-spacing: 0.5px;
}

.pay-text {
  flex: 1;
}

.title-with-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.pay-text strong {
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.pay-pill-tag {
  font-size: 10.5px;
  font-weight: 700;
  background: #f1f5f9;
  color: #64748b;
  padding: 2px 8px;
  border-radius: 20px;
}

.pay-pill-tag.qr-tag {
  background: #dbeafe;
  color: #1d4ed8;
}

.pay-text p {
  margin: 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.4;
}
</style>
