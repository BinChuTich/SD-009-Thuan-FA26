<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api.js'

const route = useRoute()
const isSubmitting = ref(false)
const isPaid = ref(false)
const errorMessage = ref('')

const invoiceCode = computed(() => String(route.query.c || route.query.code || ''))
const amount = computed(() => Number(route.query.a || route.query.amount || 0))
const token = computed(() => String(route.query.t || route.query.token || ''))
const formattedAmount = computed(() => `${amount.value.toLocaleString('vi-VN')} đ`)

const confirmPayment = async () => {
  if (!token.value || isSubmitting.value) return
  try {
    isSubmitting.value = true
    errorMessage.value = ''
    await api.post('/api/payment/demo/confirm', null, { params: { token: token.value } })
    isPaid.value = true
  } catch (error) {
    errorMessage.value = error.response?.data?.message || 'Không thể xác nhận thanh toán demo.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="demo-payment-page">
    <section class="payment-card">
      <div class="brand">MB</div>
      <template v-if="!isPaid">
        <p class="eyebrow">THANH TOÁN MÔ PHỎNG · KHÔNG TRỪ TIỀN</p>
        <h1>Xác nhận chuyển khoản</h1>
        <div class="details">
          <div><span>Mã hóa đơn</span><strong>{{ invoiceCode }}</strong></div>
          <div><span>Số tiền</span><strong class="amount">{{ formattedAmount }}</strong></div>
          <div><span>Số tài khoản</span><strong>0988888888</strong></div>
          <div><span>Người nhận</span><strong>FF T-SHIRT STORE</strong></div>
        </div>
        <p class="notice">Đây là môi trường demo cho đồ án. Không có giao dịch ngân hàng thật được tạo.</p>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <button :disabled="isSubmitting || !token" @click="confirmPayment">
          {{ isSubmitting ? 'Đang xác nhận...' : 'Xác nhận thanh toán demo' }}
        </button>
      </template>
      <template v-else>
        <div class="success-icon">✓</div>
        <h1>Thanh toán thành công</h1>
        <p class="success-copy">Quầy bán hàng đã nhận được xác nhận cho hóa đơn <strong>{{ invoiceCode }}</strong>.</p>
        <p class="notice">Mày có thể đóng trang này.</p>
      </template>
    </section>
  </main>
</template>

<style scoped>
*{box-sizing:border-box}.demo-payment-page{min-height:100vh;padding:22px;display:flex;align-items:center;justify-content:center;background:linear-gradient(150deg,#eef5ff,#dbeafe);font-family:Inter,"Segoe UI",sans-serif;color:#132238}.payment-card{width:min(430px,100%);background:#fff;border-radius:24px;padding:28px;box-shadow:0 24px 70px rgba(20,55,100,.18)}.brand{width:62px;height:62px;border-radius:18px;display:flex;align-items:center;justify-content:center;background:#0756a5;color:#fff;font-weight:900;font-size:25px;margin-bottom:26px}.eyebrow{font-size:11px;font-weight:800;letter-spacing:.8px;color:#c2410c}h1{font-size:27px;margin:6px 0 24px}.details{border:1px solid #dbe5ef;border-radius:15px;padding:5px 16px}.details div{display:flex;justify-content:space-between;gap:18px;padding:14px 0;border-bottom:1px dashed #dbe5ef}.details div:last-child{border:0}.details span{color:#708095;font-size:13px}.details strong{text-align:right;font-size:14px}.details .amount{font-size:20px;color:#d61f2c}.notice{background:#fff7e6;color:#92400e;border-radius:11px;padding:12px;font-size:13px;line-height:1.5;margin:18px 0}.error{color:#b91c1c;font-size:13px}button{width:100%;border:0;border-radius:12px;padding:15px;background:#0756a5;color:#fff;font-size:16px;font-weight:800;cursor:pointer}button:disabled{opacity:.55}.success-icon{width:78px;height:78px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#dcfce7;color:#15803d;font-size:44px;font-weight:800;margin:8px auto 22px}.success-copy{text-align:center;line-height:1.6;color:#536276}.success-icon+h1{text-align:center}
</style>
