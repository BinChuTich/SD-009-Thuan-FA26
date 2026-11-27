<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'

const route = useRoute()
const router = useRouter()
const processing = ref(false)
const result = ref(null)
const error = ref('')
const copiedField = ref('')

// Ưu tiên bộ tham số ngắn để QR dễ quét; vẫn hỗ trợ link cũ đã phát hành.
const orderId = computed(() => String(route.query.o || route.query.orderId || ''))
const amount = computed(() => Number(route.query.a || route.query.amount || 0))
const expiresAt = computed(() => Number(route.query.e || route.query.expiresAt || 0))
const token = computed(() => String(route.query.t || route.query.token || ''))
const validRequest = computed(() => Boolean(orderId.value && amount.value > 0 && expiresAt.value && token.value))

const formatMoney = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`

const copyText = async (text, field) => {
  try {
    await navigator.clipboard.writeText(String(text))
    copiedField.value = field
    window.setTimeout(() => {
      if (copiedField.value === field) copiedField.value = ''
    }, 1800)
  } catch (e) {}
}

const confirmPayment = async () => {
  if (!validRequest.value || processing.value) return
  processing.value = true
  error.value = ''
  try {
    const response = await api.post('/api/momo/mock/result', {
      orderId: orderId.value,
      expiresAt: expiresAt.value,
      token: token.value,
      result: 'SUCCESS'
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
          <h1>Thanh toán MoMo</h1>
          <p>XÁC NHẬN THANH TOÁN AN TOÀN</p>
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
        <button class="confirm-button" @click="backToCart">Xác nhận</button>
      </template>

      <template v-else-if="!result">
        <div class="payment-info">
          <div><span>Đơn hàng</span><strong>{{ orderId }}</strong></div>
          <div><span>Số tài khoản</span><span class="mobile-copy-value"><strong class="account-number">0988888888</strong><button type="button" @click="copyText('0988888888', 'account')">{{ copiedField === 'account' ? '✓' : 'Sao chép' }}</button></span></div>
          <div><span>Tên chủ tài khoản</span><span class="mobile-copy-value"><strong>FF T-SHIRT STORE</strong><button type="button" @click="copyText('FF T-SHIRT STORE', 'owner')">{{ copiedField === 'owner' ? '✓' : 'Sao chép' }}</button></span></div>
          <div><span>Nội dung</span><strong>Thanh toán {{ orderId }}</strong></div>
          <div class="amount-row"><span>Số tiền</span><strong>{{ formatMoney(amount) }}</strong></div>
        </div>

        <div class="secure-note">
          <span>✓</span>
          <div><strong>Thông tin thanh toán đã sẵn sàng</strong><small>Vui lòng kiểm tra mã đơn hàng và số tiền trước khi xác nhận.</small></div>
        </div>

        <p class="instruction">Nhấn “Xác nhận” để hoàn tất thanh toán MoMo trong môi trường demo.</p>
        <p v-if="error" class="error-message">{{ error }}</p>

        <button class="confirm-button" :disabled="processing" @click="confirmPayment">
          {{ processing ? 'Đang xác nhận...' : 'Xác nhận' }}
        </button>
      </template>

      <template v-else>
        <div class="result-box" :class="result.success ? 'success' : 'failed'">
          <span class="result-icon">{{ result.success ? '✓' : '!' }}</span>
          <h2>{{ result.success ? 'Thanh toán thành công' : (result.result === 'CANCELLED' ? 'Đã hủy thanh toán' : 'Thanh toán thất bại') }}</h2>
          <p>{{ result.message }}</p>
          <strong v-if="result.success" class="paid-amount">{{ formatMoney(result.amount) }}</strong>
        </div>
        <button class="confirm-button" @click="result.success ? viewOrder() : backToCart()">Xác nhận</button>
      </template>
    </section>
  </main>
</template>

<style scoped>
.momo-test-page{min-height:100vh;background:radial-gradient(circle at top,#fff 0,#fff4fa 42%,#f4f6fb 100%);display:flex;align-items:center;justify-content:center;padding:28px 16px;font-family:Inter,system-ui,sans-serif;color:#172033}.momo-test-card{width:min(500px,100%);background:#fff;border:1px solid #f0dce8;border-radius:24px;box-shadow:0 24px 70px rgba(91,33,70,.16);overflow:hidden;padding:28px}.momo-header{display:flex;align-items:center;gap:14px;border-bottom:1px solid #f2e5ed;padding-bottom:20px}.momo-logo{width:58px;height:58px;border-radius:17px;background:linear-gradient(145deg,#a50064,#d10a7b);color:#fff;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:900;letter-spacing:-1px;box-shadow:0 10px 24px rgba(165,0,100,.25)}.momo-header h1{margin:0;font-size:22px}.momo-header p{margin:5px 0 0;color:#a50064;font-size:10px;font-weight:800;letter-spacing:.8px}.test-warning{margin:18px 0;background:#fff8e8;border:1px solid #f5d28c;border-radius:13px;padding:12px 14px;display:flex;flex-direction:column;color:#8a5700;font-size:13px;line-height:1.5}.payment-info{border:1px solid #eadde6;border-radius:16px;padding:4px 16px;background:#fff}.payment-info>div{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:13px 0;border-bottom:1px dashed #eadde6;font-size:13px}.payment-info>div:last-child{border-bottom:0}.payment-info span{color:#6b7280}.payment-info strong{text-align:right}.account-number{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;letter-spacing:.5px;color:#7f1d58}.mobile-copy-value{display:flex;align-items:center;justify-content:flex-end;gap:7px}.mobile-copy-value button{border:1px solid #e7bfd5;border-radius:7px;padding:5px 7px;color:#a50064;background:#fff5fa;font-size:10px;font-weight:800;cursor:pointer}.amount-row strong{color:#a50064;font-size:21px}.secure-note{display:flex;align-items:center;gap:12px;margin:18px 0 0;padding:14px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:13px}.secure-note>span{display:grid;place-items:center;flex:0 0 34px;height:34px;color:#fff;background:#16a34a;border-radius:50%;font-weight:900}.secure-note div{display:grid;gap:3px}.secure-note strong{font-size:13px;color:#166534}.secure-note small{color:#64748b;line-height:1.4}.instruction{text-align:center;color:#667085;font-size:13px;line-height:1.5;margin:17px 10px}.confirm-button{display:block;width:100%;min-height:50px;border:0;border-radius:13px;background:linear-gradient(135deg,#a50064,#c60073);color:#fff;font-size:15px;font-weight:800;cursor:pointer;box-shadow:0 10px 24px rgba(165,0,100,.2);transition:transform .18s ease,box-shadow .18s ease}.confirm-button:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 13px 28px rgba(165,0,100,.27)}.result-box{text-align:center;padding:30px 12px 20px}.result-icon{width:62px;height:62px;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;font-size:30px;font-weight:900}.result-box.success .result-icon{background:#dcfce7;color:#15803d}.result-box.failed .result-icon{background:#fee2e2;color:#b91c1c}.result-box h2{margin:14px 0 8px}.result-box p{color:#64748b}.paid-amount{display:block;color:#a50064;font-size:24px;margin-top:12px}.error-message{color:#b91c1c;background:#fff1f2;border:1px solid #fecdd3;border-radius:9px;padding:10px;font-size:13px}button:disabled{opacity:.6;cursor:not-allowed}@media(max-width:520px){.momo-test-page{align-items:flex-start;padding:12px;background:#fff4fa}.momo-test-card{padding:20px 17px;border-radius:18px}.momo-header{padding-bottom:16px}.momo-logo{width:50px;height:50px;border-radius:14px;font-size:16px}.momo-header h1{font-size:19px}.test-warning{margin:15px 0}.payment-info{padding:3px 13px}.payment-info>div{padding:12px 0;gap:12px}.payment-info span{font-size:12px}.payment-info strong{font-size:12px;max-width:none;overflow-wrap:anywhere}.mobile-copy-value{max-width:68%;flex-wrap:wrap}.payment-info .amount-row strong{font-size:19px}.secure-note{align-items:flex-start}.confirm-button{position:sticky;bottom:10px;min-height:52px;margin-top:4px}}
</style>
