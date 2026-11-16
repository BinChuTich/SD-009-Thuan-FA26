<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const processing = ref(false)
const result = ref(null)
const error = ref('')

const orderId = computed(() => String(route.query.orderId || ''))
const amount = computed(() => Number(route.query.amount || 0))
const expiresAt = computed(() => Number(route.query.expiresAt || 0))
const token = computed(() => String(route.query.token || ''))
const validRequest = computed(() => Boolean(orderId.value && amount.value > 0 && expiresAt.value && token.value))

const formatMoney = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`

const submitResult = async (type) => {
  if (!validRequest.value || processing.value) return
  processing.value = true
  error.value = ''
  try {
    const response = await api.post('/api/momo/mock/result', {
      orderId: orderId.value,
      expiresAt: expiresAt.value,
      token: token.value,
      result: type
    })
    result.value = response.data
  } catch (e) {
    error.value = e.response?.data?.message || 'Không thể xử lý giao dịch MoMo thử nghiệm.'
  } finally {
    processing.value = false
  }
}

const viewOrder = () => router.push({ path: '/tra-cuu', query: { code: orderId.value } })
const backToCart = () => router.push('/gio-hang')
</script>

<template>
  <main class="momo-test-page">
    <section class="momo-test-card">
      <header class="momo-header">
        <div class="momo-logo">momo</div>
        <div>
          <h1>MoMo Payment</h1>
          <p>CỔNG THANH TOÁN GIẢ LẬP DÀNH CHO KIỂM THỬ</p>
        </div>
      </header>

      <div class="test-warning">
        <strong>Chế độ thử nghiệm</strong>
        <span>Không phát sinh giao dịch hoặc tiền thật.</span>
      </div>

      <template v-if="!validRequest">
        <div class="result-box failed">
          <span class="result-icon">!</span>
          <h2>Liên kết thanh toán không hợp lệ</h2>
          <p>Thiếu thông tin giao dịch hoặc chữ ký thử nghiệm.</p>
        </div>
        <button class="secondary-button" @click="backToCart">Quay lại giỏ hàng</button>
      </template>

      <template v-else-if="!result">
        <div class="payment-info">
          <div><span>Đơn hàng</span><strong>{{ orderId }}</strong></div>
          <div><span>Nội dung</span><strong>Thanh toán đơn hàng FF T-Shirt</strong></div>
          <div class="amount-row"><span>Số tiền</span><strong>{{ formatMoney(amount) }}</strong></div>
        </div>

        <div class="fake-qr" aria-label="Mã QR mô phỏng">
          <div class="qr-grid">
            <i v-for="n in 100" :key="n" :class="{ dark: (n * 7 + n % 9) % 5 < 2 }"></i>
          </div>
          <span>TEST</span>
        </div>

        <p class="instruction">Chọn một kết quả bên dưới để mô phỏng phản hồi của MoMo Sandbox.</p>
        <p v-if="error" class="error-message">{{ error }}</p>

        <div class="test-actions">
          <button class="success-button" :disabled="processing" @click="submitResult('SUCCESS')">
            {{ processing ? 'Đang xử lý...' : '✓ Thanh toán thành công' }}
          </button>
          <button class="failed-button" :disabled="processing" @click="submitResult('FAILED')">Giao dịch thất bại</button>
          <button class="cancel-button" :disabled="processing" @click="submitResult('CANCELLED')">Hủy thanh toán</button>
        </div>
      </template>

      <template v-else>
        <div class="result-box" :class="result.success ? 'success' : 'failed'">
          <span class="result-icon">{{ result.success ? '✓' : '!' }}</span>
          <h2>{{ result.success ? 'Thanh toán thành công' : (result.result === 'CANCELLED' ? 'Đã hủy thanh toán' : 'Thanh toán thất bại') }}</h2>
          <p>{{ result.message }}</p>
          <strong v-if="result.success" class="paid-amount">{{ formatMoney(result.amount) }}</strong>
        </div>
        <div class="result-actions">
          <button class="primary-button" @click="viewOrder">Xem đơn hàng</button>
          <button v-if="!result.success" class="secondary-button" @click="backToCart">Quay lại cửa hàng</button>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.momo-test-page{min-height:100vh;background:linear-gradient(145deg,#fff4fa,#f7f8fc);display:flex;align-items:center;justify-content:center;padding:32px 16px;font-family:Inter,system-ui,sans-serif;color:#172033}.momo-test-card{width:min(520px,100%);background:#fff;border:1px solid #eadde6;border-radius:22px;box-shadow:0 24px 60px rgba(91,33,70,.14);overflow:hidden;padding:28px}.momo-header{display:flex;align-items:center;gap:14px;border-bottom:1px solid #f0e7ed;padding-bottom:20px}.momo-logo{width:58px;height:58px;border-radius:16px;background:#a50064;color:#fff;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:900;letter-spacing:-1px;box-shadow:0 8px 20px rgba(165,0,100,.24)}.momo-header h1{margin:0;font-size:22px}.momo-header p{margin:4px 0 0;color:#a50064;font-size:10px;font-weight:800;letter-spacing:.8px}.test-warning{margin:18px 0;background:#fff7e6;border:1px solid #f9d78c;border-radius:12px;padding:12px 14px;display:flex;flex-direction:column;color:#8a5700;font-size:13px}.payment-info{border:1px solid #e8eaf0;border-radius:14px;padding:4px 16px}.payment-info>div{display:flex;justify-content:space-between;gap:20px;padding:13px 0;border-bottom:1px dashed #e1e4eb;font-size:13px}.payment-info>div:last-child{border-bottom:0}.payment-info span{color:#6b7280}.payment-info strong{text-align:right}.amount-row strong{color:#a50064;font-size:20px}.fake-qr{width:174px;height:174px;margin:22px auto 12px;padding:13px;border:1px solid #d8dbe4;border-radius:14px;position:relative;background:#fff}.qr-grid{display:grid;grid-template-columns:repeat(10,1fr);gap:2px;width:100%;height:100%}.qr-grid i{background:#fff}.qr-grid i.dark{background:#1d1d24}.fake-qr>span{position:absolute;inset:50% auto auto 50%;transform:translate(-50%,-50%);background:#a50064;color:white;border-radius:6px;padding:4px 7px;font-size:10px;font-weight:900}.instruction{text-align:center;color:#667085;font-size:13px}.test-actions,.result-actions{display:grid;gap:10px;margin-top:18px}.test-actions{grid-template-columns:1fr 1fr}.success-button{grid-column:1/-1;background:#a50064}.success-button,.failed-button,.cancel-button,.primary-button,.secondary-button{min-height:44px;border-radius:10px;border:0;font-weight:750;cursor:pointer;padding:0 16px}.success-button,.primary-button{background:#a50064;color:#fff}.failed-button{background:#fff1f2;color:#be123c;border:1px solid #fecdd3}.cancel-button,.secondary-button{background:#f1f5f9;color:#475569;border:1px solid #d9e0e8}.result-box{text-align:center;padding:30px 12px 16px}.result-icon{width:62px;height:62px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;font-size:30px;font-weight:900}.result-box.success .result-icon{background:#dcfce7;color:#15803d}.result-box.failed .result-icon{background:#fee2e2;color:#b91c1c}.result-box h2{margin:14px 0 8px}.result-box p{color:#64748b}.paid-amount{display:block;color:#a50064;font-size:24px;margin-top:12px}.error-message{color:#b91c1c;background:#fff1f2;border-radius:8px;padding:10px;font-size:13px}.result-actions{grid-template-columns:1fr 1fr}.result-actions>.primary-button:only-child{grid-column:1/-1}button:disabled{opacity:.6;cursor:not-allowed}@media(max-width:520px){.momo-test-card{padding:20px}.test-actions,.result-actions{grid-template-columns:1fr}.success-button{grid-column:auto}}
</style>
