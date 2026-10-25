<script setup>
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'

const props = defineProps({
  selectedCount: {
    type: Number,
    default: 0
  },
  selectedSubtotal: {
    type: Number,
    default: 0
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
    default: 0
  },
  voucherInput: {
    type: String,
    default: ''
  },
  appliedVoucher: {
    type: Object,
    default: null
  },
  availableVouchers: {
    type: Array,
    default: () => []
  }
})

const router = useRouter()
const goToShop = () => {
  router.push('/cua-hang')
}

const emit = defineEmits([
  'update:voucherInput',
  'applyVoucher',
  'applyQuickVoucher',
  'removeVoucher',
  'checkout'
])
</script>

<template>
  <aside class="cart-summary-panel">
    <div class="summary-card-modern">
      <div class="summary-header">
        <h3 class="summary-card-title">TÓM TẮT ĐƠN HÀNG</h3>
      </div>

      <!-- Khối Voucher khuyến mãi -->
      <div class="voucher-box-section">
        <label class="voucher-lbl">MÃ GIẢM GIÁ / VOUCHER</label>
        
        <div v-if="!appliedVoucher" class="voucher-input-group">
          <input
            type="text"
            :value="voucherInput"
            @input="emit('update:voucherInput', $event.target.value)"
            placeholder="Nhập mã voucher..."
            @keyup.enter="emit('applyVoucher')"
            class="voucher-input"
          />
          <button class="btn-apply-voucher" @click="emit('applyVoucher')">
            Áp Dụng
          </button>
        </div>

        <!-- Các mã giảm giá gợi ý bấm nhanh từ Admin -->
        <div class="quick-vouchers-list" v-if="!appliedVoucher && availableVouchers && availableVouchers.length > 0">
          <span class="quick-v-title">Mã ưu đãi:</span>
          <button
            v-for="v in availableVouchers.slice(0, 4)"
            :key="v.id"
            type="button"
            class="quick-v-chip"
            @click="emit('applyQuickVoucher', v.maPhieuGiamGia)"
            :title="`${v.tenPhieuGiamGia || v.maPhieuGiamGia} - ${Number(v.loaiPhieuGiamGia) === 1 ? 'Giảm ' + v.giaTriGiamGia + '%' : 'Giảm ' + formatCurrency(v.giaTriGiamGia)}`"
          >
            {{ v.maPhieuGiamGia }}
          </button>
        </div>

        <div v-else class="applied-voucher-badge">
          <div class="av-info">
            <span class="av-icon">🎟️</span>
            <div>
              <strong>{{ appliedVoucher.code }}</strong>
              <span>-{{ formatCurrency(appliedVoucher.discount) }}</span>
            </div>
          </div>
          <button class="btn-remove-voucher" @click="emit('removeVoucher')" title="Bỏ voucher">✕</button>
        </div>
      </div>

      <!-- Bảng tính tiền chi tiết -->
      <div class="pricing-breakdown">
        <div class="p-row">
          <span>Tạm tính ({{ selectedCount }} món):</span>
          <strong>{{ formatCurrency(selectedSubtotal) }}</strong>
        </div>

        <div class="p-row">
          <span>Giảm giá voucher:</span>
          <span class="discount-val">-{{ formatCurrency(voucherDiscount) }}</span>
        </div>

        <div class="p-row">
          <span>Phí vận chuyển:</span>
          <span class="ship-val">
            {{ shippingFee === 0 ? 'MIỄN PHÍ' : formatCurrency(shippingFee) }}
          </span>
        </div>

        <div class="p-divider"></div>

        <div class="p-row total-row">
          <span class="total-lbl">TỔNG THANH TOÁN:</span>
          <span class="total-val">{{ formatCurrency(finalTotal) }}</span>
        </div>
      </div>

      <!-- Nút Thanh toán -->
      <button
        class="btn-checkout-gold"
        :disabled="selectedCount === 0"
        @click="emit('checkout')"
      >
        <span>TIẾN HÀNH THANH TOÁN &rarr;</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.cart-summary-panel {
  width: 100%;
}

.summary-card-modern {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.05);
}

.summary-header {
  padding-bottom: 14px;
  border-bottom: 2px solid #ecd8af;
  margin-bottom: 18px;
}

.clickable-freeship {
  cursor: pointer;
  transition: all 0.25s ease;
}
.clickable-freeship:hover {
  border-color: #3e5c76 !important;
  background-color: #f1f6fa !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(62, 92, 118, 0.12);
}
.fs-link-tag {
  font-size: 11px;
  font-weight: 700;
  color: #3e5c76;
  margin-left: auto;
  white-space: nowrap;
}
.freeship-box {
  background: #fdfbf7;
  border: 1px solid #ecd8af;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 18px;
}

.freeship-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #496883;
  margin-bottom: 8px;
}

.fs-icon {
  font-size: 16px;
}

.fs-text strong {
  color: #1e293b;
}

.fs-progress-track {
  width: 100%;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.fs-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #d2a764, #496883);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.summary-card-title {
  font-size: 16px;
  font-weight: 800;
  color: #496883;
  margin: 0;
  letter-spacing: 0.5px;
}

.voucher-box-section {
  margin-bottom: 20px;
}

.voucher-lbl {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.voucher-input-group {
  display: flex;
  gap: 8px;
}

.voucher-input {
  flex: 1;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
}

.voucher-input:focus {
  border-color: #496883;
}

.btn-apply-voucher {
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 0 16px;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-apply-voucher:hover {
  background: #364f66;
}

.quick-vouchers-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.quick-v-title {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.quick-v-chip {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  color: #496883;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-v-chip:hover {
  background: #fffbeb;
  border-color: #d2a764;
  color: #b45309;
}

.applied-voucher-badge {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f0fdf4;
  border: 1px dashed #22c55e;
  padding: 10px 14px;
  border-radius: 6px;
}

.av-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.av-info strong {
  display: block;
  color: #15803d;
}

.btn-remove-voucher {
  background: transparent;
  border: none;
  color: #dc2626;
  font-weight: 800;
  cursor: pointer;
}

.pricing-breakdown {
  margin-bottom: 24px;
}

.p-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #475569;
  margin-bottom: 10px;
}

.discount-val {
  color: #16a34a;
  font-weight: 700;
}

.ship-val {
  color: #0284c7;
  font-weight: 700;
}

.p-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 14px 0;
}

.total-row {
  font-size: 16px;
  margin-bottom: 0;
}

.total-lbl {
  font-weight: 800;
  color: #1e293b;
}

.total-val {
  font-size: 20px;
  font-weight: 900;
  color: #e11d48;
}

.btn-checkout-gold {
  width: 100%;
  padding: 14px 0;
  background: #d2a764;
  color: #1e293b;
  border: none;
  border-radius: 10px;
  font-weight: 800;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.4);
}

.btn-checkout-gold:hover:not(:disabled) {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.5);
}

.btn-checkout-gold:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>

