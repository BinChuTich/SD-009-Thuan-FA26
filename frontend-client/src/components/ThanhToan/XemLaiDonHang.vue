<script setup>
import { formatCurrency } from '@/utils/dinhDang.js'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const props = defineProps({
  cartItems: {
    type: Array,
    required: true
  },
  cartSubtotal: {
    type: Number,
    required: true
  },
  shippingFee: {
    type: Number,
    default: 0
  },
  voucherDiscount: {
    type: Number,
    default: 0
  },
  finalTotal: {
    type: Number,
    required: true
  },
  orderNote: {
    type: String,
    default: ''
  },
  submitting: {
    type: Boolean,
    default: false
  },
  appliedVoucher: {
    type: Object,
    default: null
  },
  voucherSuggestion: {
    type: Object,
    default: null
  }
})

const emit = defineEmits([
  'update:orderNote',
  'placeOrder',
  'openVoucherModal',
  'removeVoucher'
])
</script>

<template>
  <aside class="right-summary-column">
    <div class="order-summary-box">
      <div class="summary-header">
        <h3 class="summary-title">ĐƠN HÀNG CỦA BẠN</h3>
        <span class="items-count">({{ cartItems.length }} sản phẩm)</span>
      </div>

      <!-- Danh sách món hàng tóm tắt -->
      <div class="checkout-items-preview">
        <div
          v-for="item in cartItems"
          :key="item.variantId"
          class="preview-item-row"
        >
          <div class="preview-img-box">
            <img :src="resolveProductImageUrl(item.image)" :alt="item.productName" @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER" />
            <span class="preview-badge">{{ item.quantity }}</span>
          </div>

          <div class="preview-info">
            <h4 class="preview-name">{{ item.productName }}</h4>
            <span class="preview-variant">{{ item.color }} / Size {{ item.size }}</span>
          </div>

          <div class="preview-price">
            {{ formatCurrency((item.discountPrice > 0 ? item.discountPrice : item.price) * item.quantity) }}
          </div>
        </div>
      </div>

      <!-- Ghi chú đơn hàng -->
      <div class="order-note-field">
        <label>Ghi chú cho đơn hàng (Tùy chọn)</label>
        <textarea
          :value="orderNote"
          @input="emit('update:orderNote', $event.target.value)"
          rows="2"
          placeholder="Ví dụ: Giao giờ hành chính, gọi trước khi giao..."
          class="note-textarea"
        ></textarea>
      </div>

      <!-- Khu vực chọn Voucher / Khuyến mãi -->
      <div class="checkout-voucher-section">
        <div class="voucher-section-header">
          <span class="v-section-title">Phiếu giảm giá (Voucher)</span>
          <button
            type="button"
            class="btn-pick-voucher"
            @click="emit('openVoucherModal')"
          >
            {{ appliedVoucher ? 'Đổi voucher' : 'Chọn voucher' }}
          </button>
        </div>

        <!-- Voucher đang áp dụng -->
        <div v-if="appliedVoucher" class="applied-voucher-chip">
          <div class="v-chip-left">
            <span class="v-chip-icon">🎟️</span>
            <div class="v-chip-info">
              <strong class="v-chip-code">{{ appliedVoucher.code || appliedVoucher.maPhieuGiamGia }}</strong>
              <span class="v-chip-desc">{{ appliedVoucher.name || appliedVoucher.tenPhieuGiamGia || 'Mã ưu đãi' }}</span>
            </div>
          </div>
          <button
            type="button"
            class="btn-clear-voucher"
            @click="emit('removeVoucher')"
            title="Bỏ voucher này"
          >
            ✕
          </button>
        </div>

        <!-- Gợi ý voucher tốt hơn -->
        <div v-if="voucherSuggestion" class="voucher-suggestion-hint">
          <span class="hint-icon">💡</span>
          <span class="hint-text">
            Mua thêm <strong>{{ formatCurrency(voucherSuggestion.neededAmount) }}</strong> để dùng mã <strong>{{ voucherSuggestion.code }}</strong> (giảm {{ voucherSuggestion.discountLabel || formatCurrency(voucherSuggestion.potentialDiscount) }}).
          </span>
        </div>
      </div>

      <!-- Bảng giá -->
      <div class="price-summary-breakdown">
        <div class="sum-row">
          <span>Tạm tính:</span>
          <strong>{{ formatCurrency(cartSubtotal) }}</strong>
        </div>

        <div class="sum-row">
          <span>Phí vận chuyển:</span>
          <span class="ship-val">
            {{ shippingFee === 0 ? 'MIỄN PHÍ' : formatCurrency(shippingFee) }}
          </span>
        </div>

        <div v-if="voucherDiscount > 0" class="sum-row">
          <span>Voucher giảm giá:</span>
          <span class="discount-val">-{{ formatCurrency(voucherDiscount) }}</span>
        </div>

        <div class="divider"></div>

        <div class="sum-row total-row">
          <span class="total-lbl">TỔNG THANH TOÁN:</span>
          <span class="total-amount">{{ formatCurrency(finalTotal) }}</span>
        </div>
      </div>

      <!-- Nút Đặt hàng -->
      <button
        class="btn-place-order-gold"
        :disabled="submitting || cartItems.length === 0"
        @click="emit('placeOrder')"
      >
        <span v-if="!submitting" class="btn-text">XÁC NHẬN ĐẶT HÀNG &rarr;</span>
        <span v-else class="btn-text">Đang xử lý đơn hàng...</span>
      </button>

      <!-- Cam kết an toàn -->
      <div class="security-guarantee">
        <span>🔒 Thanh toán bảo mật &amp; Kiểm tra hàng trước khi nhận</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.right-summary-column {
  width: 100%;
}

.order-summary-box {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.05);
}

.summary-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 2px solid #ecd8af;
  margin-bottom: 16px;
}

.summary-title {
  font-size: 16px;
  font-weight: 800;
  color: #496883;
  margin: 0;
  letter-spacing: 0.5px;
}

.items-count {
  font-size: 12px;
  color: #64748b;
}

.checkout-items-preview {
  max-height: 280px;
  overflow-y: auto;
  margin-bottom: 16px;
  padding-right: 4px;
}

.preview-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #f1f5f9;
}

.preview-img-box {
  position: relative;
  width: 48px;
  height: 54px;
  border-radius: 6px;
  overflow: visible;
  background: #f1f5f9;
  flex-shrink: 0;
}

.preview-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
}

.preview-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #496883;
  color: #ffffff;
  font-size: 10px;
  font-weight: 800;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-variant {
  font-size: 11px;
  color: #64748b;
}

.preview-price {
  font-size: 13px;
  font-weight: 800;
  color: #1e293b;
}

.order-note-field {
  margin-bottom: 16px;
}

.order-note-field label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 6px;
}

.note-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  box-sizing: border-box;
  resize: none;
  background: #f8fafc;
}

.price-summary-breakdown {
  margin-bottom: 20px;
}

.sum-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #475569;
  margin-bottom: 8px;
}

.ship-val {
  color: #0284c7;
  font-weight: 700;
}

.discount-val {
  color: #16a34a;
  font-weight: 700;
}

.divider {
  height: 1px;
  background: #e2e8f0;
  margin: 12px 0;
}

.total-row {
  font-size: 15px;
  margin-bottom: 0;
}

.total-lbl {
  font-weight: 800;
  color: #1e293b;
}

.total-amount {
  font-size: 19px;
  font-weight: 900;
  color: #e11d48;
}

.btn-place-order-gold {
  width: 100%;
  padding: 15px 0;
  background: #d2a764;
  color: #1e293b;
  border: none;
  border-radius: 10px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  font-weight: 800;
  font-size: 15.5px;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-place-order-gold .btn-text {
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.btn-place-order-gold:hover:not(:disabled) {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.5);
}

.btn-place-order-gold:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.security-guarantee {
  text-align: center;
  font-size: 11px;
  color: #64748b;
  margin-top: 14px;
}

/* ==========================================================
   CHECKOUT VOUCHER STYLES
========================================================== */
.checkout-voucher-section {
  margin-bottom: 20px;
  background: #fdfbf7;
  border: 1px dashed #e2d9cc;
  border-radius: 10px;
  padding: 12px 14px;
}

.voucher-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.v-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.btn-pick-voucher {
  background: #1e293b;
  color: #ecd8af;
  border: none;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-pick-voucher:hover {
  background: #334155;
}

.applied-voucher-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.v-chip-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.v-chip-icon {
  font-size: 18px;
}

.v-chip-info {
  display: flex;
  flex-direction: column;
}

.v-chip-code {
  font-size: 13px;
  font-weight: 800;
  color: #166534;
}

.v-chip-desc {
  font-size: 11px;
  color: #15803d;
}

.btn-clear-voucher {
  background: transparent;
  border: none;
  color: #dc2626;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.btn-clear-voucher:hover {
  background: #fee2e2;
}

.voucher-suggestion-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: #92400e;
  line-height: 1.4;
}

.hint-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.hint-text strong {
  color: #b45309;
}
</style>

