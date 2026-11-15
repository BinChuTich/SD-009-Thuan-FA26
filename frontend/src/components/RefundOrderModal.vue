<template>
  <transition name="refund-modal-fade">
    <div v-if="show" class="refund-modal-backdrop" @click.self="handleClose">
      <div class="refund-modal-container" role="dialog" aria-modal="true" aria-labelledby="refund-modal-title">
        <div class="refund-modal-header">
          <div class="refund-title-group">
            <span class="refund-icon-badge" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </span>
            <div>
              <h3 id="refund-modal-title" class="refund-modal-title">{{ modalTitle }}</h3>
              <p class="refund-modal-subtitle">{{ modalSubtitle }}</p>
            </div>
          </div>
          <button class="refund-btn-close" type="button" :disabled="loading" @click="handleClose" aria-label="Đóng">✕</button>
        </div>

        <div class="refund-modal-body">
          <div class="refund-alert-banner">
            <span class="refund-alert-emoji">💸</span>
            <div class="refund-alert-copy">
              <template v-if="refundOnly">
                Đơn hàng online <strong>{{ orderCode }}</strong> đã bị hủy nhưng khoản thanh toán chuyển khoản đã được ghi nhận. Vui lòng kiểm tra giao dịch và xác nhận hoàn tiền cho khách hàng.
              </template>
              <template v-else>
                Đơn hàng online <strong>{{ orderCode }}</strong> đã <strong>thanh toán thành công</strong>, nhưng hiện tại sản phẩm trong kho <strong>không còn / không đủ số lượng</strong> để xuất giao. Không thể xác nhận đơn hàng — vui lòng hủy đơn và thực hiện hoàn tiền cho khách hàng.
              </template>
            </div>
          </div>

          <div class="refund-summary-card">
            <div class="refund-summary-row">
              <span class="summary-label">Khách hàng</span>
              <span class="summary-value">
                <strong>{{ customerName || 'Khách lẻ' }}</strong>
                <span v-if="customerPhone && customerPhone !== '---' && customerPhone !== '—'" class="summary-muted">
                  ({{ customerPhone }})
                </span>
              </span>
            </div>
            <div class="refund-summary-row">
              <span class="summary-label">Phương thức đã thanh toán</span>
              <span class="summary-value">
                {{ paymentMethod || 'Thanh toán trực tuyến' }}
                <span v-if="transactionCode" class="summary-trans-code">#{{ transactionCode }}</span>
              </span>
            </div>
            <div class="refund-summary-row refund-amount-row">
              <span class="summary-label">Số tiền cần hoàn cho khách</span>
              <span class="refund-amount-highlight">{{ formatMoney(refundAmount) }}</span>
            </div>
          </div>

          <div v-if="!refundOnly && outOfStockItems && outOfStockItems.length > 0" class="refund-stock-section">
            <div class="section-heading">Sản phẩm hết hàng / không đủ tồn kho ({{ outOfStockItems.length }})</div>
            <div class="stock-item-list">
              <div v-for="(item, idx) in outOfStockItems" :key="item.variantId || item.chiTietId || item.id || idx" class="stock-item-row">
                <div class="stock-item-info">
                  <div class="stock-item-name">{{ item.name }}</div>
                  <div v-if="item.variantText" class="stock-item-variant">{{ item.variantText }}</div>
                </div>
                <div class="stock-item-qty">
                  <span class="qty-needed">Đặt: <strong>{{ item.needed }}</strong></span>
                  <span class="qty-stock-pill" :class="Number(item.available) <= 0 ? 'is-zero' : 'is-low'">
                    {{ Number(item.available) <= 0 ? 'Kho còn: 0 (Hết hàng)' : `Kho còn: ${item.available}` }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="refund-form-group">
            <label class="refund-field-label" for="refund-reason-input">
              {{ refundOnly ? 'Ghi chú hoàn tiền' : 'Lý do hủy đơn & ghi chú hoàn tiền' }} <span class="text-required">*</span>
            </label>
            <textarea
              id="refund-reason-input"
              v-model="reason"
              rows="2"
              maxlength="300"
              class="refund-textarea"
              :placeholder="refundOnly ? 'Nhập nội dung xác nhận hoàn tiền...' : 'Nhập lý do hủy đơn và thông tin hoàn tiền...'"
              :disabled="loading"
            ></textarea>
          </div>

          <div class="refund-email-box">
            <label class="refund-checkbox-label">
              <input type="checkbox" v-model="sendEmail" :disabled="loading" />
              <span>✉ {{ refundOnly ? 'Gửi email thông báo hoàn tiền cho khách hàng' : 'Gửi email thông báo hủy đơn & hoàn tiền cho khách hàng' }}</span>
            </label>
            <div v-if="sendEmail" class="refund-email-input-wrap">
              <label class="refund-field-label" for="refund-email-input">
                Địa chỉ email nhận thông báo <span class="text-required">*</span>
              </label>
              <input
                id="refund-email-input"
                v-model="email"
                type="email"
                maxlength="120"
                class="refund-input"
                placeholder="Nhập email khách hàng (VD: khachhang@gmail.com)..."
                :disabled="loading"
              />
              <p class="refund-email-hint">
                {{ refundOnly ? 'Hệ thống sẽ gửi thông báo xác nhận hoàn tiền tới email này.' : 'Hệ thống sẽ gửi thông báo hủy đơn do hết hàng kèm số tiền hoàn trả tới email này.' }}
              </p>
            </div>
          </div>

          <p v-if="errorText" class="refund-error-text">{{ errorText }}</p>
        </div>

        <div class="refund-modal-footer">
          <button class="refund-btn-cancel" type="button" :disabled="loading" @click="handleClose">
            Đóng
          </button>
          <button class="refund-btn-confirm" type="button" :disabled="loading" @click="handleSubmit">
            {{ loading ? 'Đang xử lý...' : (refundOnly ? 'Xác nhận hoàn tiền' : 'Xác nhận hủy đơn & Hoàn tiền') }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const DEFAULT_REFUND_REASON = 'Sản phẩm trong kho đã hết hàng - Hủy đơn và hoàn tiền lại cho khách hàng'
const CANCELLED_ORDER_REFUND_REASON = 'Hoàn tiền cho đơn hàng online đã hủy'

const props = defineProps({
  show: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  orderCode: { type: String, default: '' },
  customerName: { type: String, default: 'Khách hàng' },
  customerPhone: { type: String, default: '' },
  paymentMethod: { type: String, default: 'Thanh toán trực tuyến' },
  transactionCode: { type: String, default: '' },
  refundAmount: { type: Number, default: 0 },
  customerEmail: { type: String, default: '' },
  outOfStockItems: { type: Array, default: () => [] },
  refundOnly: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'confirm'])

const reason = ref(DEFAULT_REFUND_REASON)
const sendEmail = ref(true)
const email = ref('')
const errorText = ref('')
const modalTitle = computed(() => props.refundOnly ? 'Xác Nhận Hoàn Tiền Cho Khách Hàng' : 'Thông Báo Hoàn Tiền Cho Khách Hàng')
const modalSubtitle = computed(() => props.refundOnly ? 'Đơn hàng online đã hủy và đã thanh toán chuyển khoản' : 'Đơn hàng online đã thanh toán nhưng kho không đủ sản phẩm')

const normalizeEmail = (val) => {
  const s = String(val || '').trim()
  if (!s || s === '---' || s === '—') return ''
  return s
}

watch(
  () => props.show,
  (isOpen) => {
    if (isOpen) {
      reason.value = props.refundOnly ? CANCELLED_ORDER_REFUND_REASON : DEFAULT_REFUND_REASON
      const initialEmail = normalizeEmail(props.customerEmail)
      email.value = initialEmail
      sendEmail.value = Boolean(initialEmail && initialEmail.includes('@'))
      errorText.value = ''
    }
  }
)

const formatMoney = (value) => {
  return `${Number(value || 0).toLocaleString('vi-VN')} đ`
}

const handleClose = () => {
  if (props.loading) return
  emit('close')
}

const handleSubmit = () => {
  if (props.loading) return
  const trimmedReason = String(reason.value || '').trim()
  if (!trimmedReason) {
    errorText.value = props.refundOnly ? 'Vui lòng nhập ghi chú hoàn tiền!' : 'Vui lòng nhập lý do hủy đơn & hoàn tiền!'
    return
  }

  const trimmedEmail = normalizeEmail(email.value)
  if (sendEmail.value && (!trimmedEmail || !trimmedEmail.includes('@'))) {
    errorText.value = 'Vui lòng nhập địa chỉ email khách hàng hợp lệ hoặc bỏ chọn gửi email!'
    return
  }

  errorText.value = ''
  emit('confirm', {
    reason: trimmedReason,
    sendEmail: sendEmail.value,
    email: sendEmail.value ? trimmedEmail : ''
  })
}
</script>

<style scoped>
.refund-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(15, 23, 42, 0.52);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.refund-modal-container {
  width: 100%;
  max-width: 560px;
  max-height: 92vh;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #ebd9c8;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: refundModalPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes refundModalPop {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.refund-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: #fff7ed;
  border-bottom: 1px solid #fed7aa;
}

.refund-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refund-icon-badge {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #fee2e2;
  color: #dc2626;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.refund-modal-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #7c2d12;
}

.refund-modal-subtitle {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: #9a3412;
}

.refund-btn-close {
  background: transparent;
  border: none;
  font-size: 18px;
  line-height: 1;
  color: #9a3412;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 6px;
}

.refund-btn-close:hover:not(:disabled) {
  background: rgba(154, 52, 18, 0.1);
}

.refund-modal-body {
  padding: 18px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.refund-alert-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: #fff1f2;
  border: 1px solid #fda4af;
  border-radius: 10px;
  color: #881337;
  font-size: 13.5px;
  line-height: 1.5;
}

.refund-alert-emoji {
  font-size: 20px;
  line-height: 1.2;
  flex-shrink: 0;
}

.refund-summary-card {
  background: #faf7f2;
  border: 1px solid #e8dfd1;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.refund-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 13.5px;
}

.summary-label {
  color: #647074;
}

.summary-value {
  color: #2b353b;
  text-align: right;
}

.summary-muted {
  color: #647074;
  font-size: 12.5px;
}

.summary-trans-code {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  background: #eef2ff;
  color: #3730a3;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
}

.refund-amount-row {
  padding-top: 8px;
  border-top: 1px dashed #d6ccbc;
}

.refund-amount-highlight {
  font-size: 17px;
  font-weight: 800;
  color: #dc2626;
}

.refund-stock-section {
  border: 1px solid #f1e5d6;
  border-radius: 10px;
  overflow: hidden;
}

.section-heading {
  background: #f7f2ea;
  padding: 8px 12px;
  font-size: 12.5px;
  font-weight: 700;
  color: #4a3427;
  border-bottom: 1px solid #f1e5d6;
}

.stock-item-list {
  max-height: 150px;
  overflow-y: auto;
}

.stock-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-bottom: 1px solid #f5efe7;
  font-size: 13px;
}

.stock-item-row:last-child {
  border-bottom: none;
}

.stock-item-info {
  min-width: 0;
}

.stock-item-name {
  font-weight: 600;
  color: #2b353b;
}

.stock-item-variant {
  font-size: 12px;
  color: #7b5943;
  margin-top: 2px;
}

.stock-item-qty {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.qty-needed {
  color: #475569;
  font-size: 12.5px;
}

.qty-stock-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;
}

.qty-stock-pill.is-zero {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fca5a5;
}

.qty-stock-pill.is-low {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fcd34d;
}

.refund-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.refund-field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.text-required {
  color: #dc2626;
}

.refund-textarea,
.refund-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 13.5px;
  font-family: inherit;
  color: #1e293b;
  outline: none;
  transition: border-color 0.2s;
}

.refund-textarea:focus,
.refund-input:focus {
  border-color: #7b5943;
}

.refund-email-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
}

.refund-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
}

.refund-email-input-wrap {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.refund-email-hint {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.refund-error-text {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #dc2626;
}

.refund-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  background: #fcfaf7;
  border-top: 1px solid #ebd9c8;
}

.refund-btn-cancel,
.refund-btn-confirm {
  padding: 9px 16px;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.refund-btn-cancel {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
}

.refund-btn-cancel:hover:not(:disabled) {
  background: #f1f5f9;
}

.refund-btn-confirm {
  background: #dc2626;
  border: 1px solid #dc2626;
  color: #ffffff;
}

.refund-btn-confirm:hover:not(:disabled) {
  background: #b91c1c;
}

.refund-btn-cancel:disabled,
.refund-btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refund-modal-fade-enter-active,
.refund-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.refund-modal-fade-enter-from,
.refund-modal-fade-leave-to {
  opacity: 0;
}
</style>
