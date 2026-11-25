<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { createBrandedQrDataUrl } from '@/utils/brandedQr.js'

const route = useRoute()
const router = useRouter()
const processing = ref(false)
const paid = ref(false)
const error = ref('')
const token = computed(() => String(route.query.t || ''))
const orderCode = computed(() => String(route.query.c || ''))
const amount = computed(() => Number(route.query.a || 0))
const money = computed(() => `${amount.value.toLocaleString('vi-VN')} đ`)
const isQrDisplay = computed(() => route.query.display === 'qr')
const qrImage = ref('')
const qrLoading = ref(false)
let pollTimer = null

const buildPhoneQr = async () => {
  if (!isQrDisplay.value || !token.value) return
  qrLoading.value = true
  try {
    let origin = window.location.origin
    if (['localhost', '127.0.0.1'].includes(window.location.hostname)) {
      try {
        const response = await api.get('/api/pos-sync/lan-ip', { timeout: 3000 })
        if (response.data?.primaryIp) {
          origin = `${window.location.protocol}//${response.data.primaryIp}:${window.location.port || '5174'}`
        }
      } catch (e) {}
    }
    const query = new URLSearchParams({ t: token.value, c: orderCode.value, a: String(amount.value) })
    qrImage.value = await createBrandedQrDataUrl(`${origin}/thanh-toan/vietqr-demo?${query}`, 'vietqr')
  } catch (e) {
    error.value = 'Không thể tạo mã QR. Vui lòng thử lại.'
  } finally {
    qrLoading.value = false
  }
}

const startPolling = () => {
  if (!isQrDisplay.value || !orderCode.value) return
  pollTimer = window.setInterval(async () => {
    try {
      const response = await api.get(`/api/payment/check-status/${orderCode.value}`)
      if (response.data?.daThanhToan) {
        paid.value = true
        window.clearInterval(pollTimer)
      }
    } catch (e) {}
  }, 1500)
}

const confirmPayment = async () => {
  if (!token.value || processing.value) return
  processing.value = true
  error.value = ''
  try {
    await api.post('/api/payment/demo/confirm', null, { params: { token: token.value } })
    paid.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Không thể xác nhận thanh toán demo.'
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  buildPhoneQr()
  startPolling()
})
onBeforeUnmount(() => pollTimer && window.clearInterval(pollTimer))
</script>

<template>
  <main class="page"><section class="card" :class="{ 'qr-card': isQrDisplay }">
    <div class="brand"><b>V</b><span>VIETQR DEMO</span></div>
    <template v-if="!paid && isQrDisplay">
      <h1>Thanh toán chuyển khoản VietQR</h1>
      <p class="subtitle">Dùng camera điện thoại quét mã để mở màn hình xác nhận thanh toán demo.</p>
      <div class="qr-layout">
        <div class="qr-panel">
          <div class="qr-frame">
            <span v-if="qrLoading">Đang tạo QR...</span>
            <img v-else-if="qrImage" :src="qrImage" alt="QR thanh toán VietQR demo" />
          </div>
          <strong>Quét mã bằng điện thoại</strong>
          <small>Điện thoại và máy tính cần dùng chung mạng Wi‑Fi</small>
        </div>
        <div class="qr-detail">
          <div class="waiting"><i></i> Đang chờ xác nhận trên điện thoại...</div>
          <div class="info"><div><span>Mã đơn hàng</span><strong>{{ orderCode }}</strong></div><div><span>Ngân hàng</span><strong>MB Bank (970422)</strong></div><div><span>Số tài khoản</span><strong>0988888888</strong></div><div><span>Chủ tài khoản</span><strong>FF T-SHIRT STORE</strong></div><div><span>Số tiền</span><strong class="amount">{{ money }}</strong></div></div>
          <p class="note">Môi trường demo cho đồ án, không phát sinh giao dịch ngân hàng thật.</p>
          <p v-if="error" class="error">{{ error }}</p>
          <button class="secondary" @click="router.back()">Đóng &amp; quay lại đơn hàng</button>
        </div>
      </div>
    </template>
    <template v-else-if="!paid">
      <h1>Xác nhận chuyển khoản</h1>
      <div class="info"><div><span>Mã đơn hàng</span><strong>{{ orderCode }}</strong></div><div><span>Ngân hàng</span><strong>MB Bank (970422)</strong></div><div><span>Số tài khoản</span><strong>0988888888</strong></div><div><span>Số tiền</span><strong class="amount">{{ money }}</strong></div></div>
      <p class="note">Môi trường demo cho đồ án, không phát sinh giao dịch ngân hàng thật.</p>
      <p v-if="error" class="error">{{ error }}</p>
      <button :disabled="processing || !token" @click="confirmPayment">{{ processing ? 'Đang xác nhận...' : 'Xác nhận thanh toán demo' }}</button>
    </template>
    <template v-else><div class="check">✓</div><h1>Thanh toán thành công</h1><p>Đơn hàng {{ orderCode }} đã được xác nhận.</p><button @click="router.push({path:'/tra-cuu',query:{code:orderCode}})">Xem đơn hàng</button></template>
  </section></main>
</template>

<style scoped>
*{box-sizing:border-box}.page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:22px;background:linear-gradient(145deg,#eef6ff,#e2eaf7);font-family:Inter,Arial;color:#17263a}.card{width:min(440px,100%);background:#fff;border-radius:22px;padding:27px;box-shadow:0 24px 65px rgba(20,50,90,.18)}.card.qr-card{width:min(900px,100%)}.brand{display:flex;align-items:center;gap:10px;color:#0756a5;font-weight:900}.brand b{width:48px;height:48px;display:flex;align-items:center;justify-content:center;border-radius:13px;background:#fff;color:#e51f32;font-size:32px;border:1px solid #e1e7ef}.brand span{font-size:13px}h1{margin:24px 0 8px}.subtitle{margin:0 0 20px;color:#64748b}.qr-layout{display:grid;grid-template-columns:minmax(280px,360px) 1fr;gap:24px}.qr-panel{padding:18px;border:1px solid #dce4ed;border-radius:16px;text-align:center}.qr-frame{aspect-ratio:1;display:grid;place-items:center;margin-bottom:12px;background:#f8fafc;border-radius:12px;overflow:hidden}.qr-frame img{display:block;width:100%;height:100%;object-fit:contain}.qr-panel strong,.qr-panel small{display:block}.qr-panel small{margin-top:5px;color:#64748b}.qr-detail{display:flex;flex-direction:column;gap:12px}.waiting{padding:11px 13px;color:#9a5b00;background:#fff7df;border:1px solid #f7d17a;border-radius:10px;font-weight:700}.waiting i{display:inline-block;width:9px;height:9px;margin-right:7px;background:#f59e0b;border-radius:50%;box-shadow:0 0 0 4px rgba(245,158,11,.14)}.info{border:1px solid #dce4ed;border-radius:14px;padding:4px 15px}.info div{display:flex;justify-content:space-between;gap:18px;padding:13px 0;border-bottom:1px dashed #dce4ed}.info div:last-child{border:0}.info span{color:#718096}.info strong{text-align:right}.amount{color:#d51e2c;font-size:20px}.note{padding:12px;background:#fff6da;color:#8b590a;border-radius:10px;line-height:1.5}.error{color:#b91c1c}button{width:100%;padding:14px;border:0;border-radius:11px;background:#0756a5;color:#fff;font-weight:800;font-size:15px;cursor:pointer}.secondary{background:#496883}.check{width:72px;height:72px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#dcfce7;color:#15803d;font-size:40px;font-weight:900;margin:15px auto;text-align:center}@media(max-width:720px){.card.qr-card{padding:20px}.qr-layout{grid-template-columns:1fr}.qr-panel{max-width:380px;width:100%;margin:auto}}
</style>
