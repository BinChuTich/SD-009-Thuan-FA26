<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import api from '@/api'

const props = defineProps({ payment: { type: Object, default: null } })
const emit = defineEmits(['close', 'paid'])
let timer = null
const copiedField = ref('')

const copyText = async (text, field) => {
  try {
    await navigator.clipboard.writeText(String(text))
    copiedField.value = field
    window.setTimeout(() => {
      if (copiedField.value === field) copiedField.value = ''
    }, 1800)
  } catch (e) {}
}

const stop = () => {
  if (timer) window.clearInterval(timer)
  timer = null
}

watch(() => props.payment, (payment) => {
  stop()
  if (!payment?.code) return
  timer = window.setInterval(async () => {
    try {
      const response = await api.get(`/api/payment/check-status/${payment.code}`)
      if (response.data?.daThanhToan) {
        stop()
        emit('paid', payment)
      }
    } catch (e) {}
  }, 1500)
}, { immediate: true })

onBeforeUnmount(stop)
</script>

<template>
  <Teleport to="body">
    <div v-if="payment" class="pay-overlay" @click.self="emit('close')">
      <section class="pay-modal">
        <header :class="['pay-header', payment.provider === 'MOMO' ? 'momo' : 'vietqr']">
          <div class="pay-brand"><b>{{ payment.provider === 'MOMO' ? 'momo' : 'V' }}</b><div><strong>{{ payment.provider === 'MOMO' ? 'Thanh toán Ví MoMo' : 'Thanh toán chuyển khoản VietQR' }}</strong><small>Quét mã bằng điện thoại để xác nhận</small></div></div>
          <button v-if="payment.provider !== 'MOMO'" type="button" @click="emit('close')">×</button>
        </header>
        <div class="pay-body">
          <div class="qr-column">
            <div class="qr-box"><img :src="payment.qrImage" :alt="`QR ${payment.provider}`" /></div>
            <strong>Quét bằng camera điện thoại</strong>
            <span>Điện thoại và máy tính cần dùng chung mạng Wi‑Fi</span>
          </div>
          <div class="detail-column">
            <div class="waiting"><i></i> Hệ thống đang chờ xác nhận trên điện thoại...</div>
            <div class="payment-info">
              <div><span>Mã đơn hàng</span><strong>{{ payment.code }}</strong></div>
              <div><span>Phương thức</span><strong>{{ payment.provider === 'MOMO' ? 'Ví MoMo · Demo' : 'VietQR (Napas 247)' }}</strong></div>
              <template v-if="payment.provider === 'MOMO'">
                <div><span>Số tài khoản</span><span class="copy-value"><strong>0988888888</strong><button type="button" @click="copyText('0988888888', 'account')">{{ copiedField === 'account' ? '✓' : 'Sao chép' }}</button></span></div>
                <div><span>Tên chủ tài khoản</span><span class="copy-value"><strong>FF T-SHIRT STORE</strong><button type="button" @click="copyText('FF T-SHIRT STORE', 'owner')">{{ copiedField === 'owner' ? '✓' : 'Sao chép' }}</button></span></div>
              </template>
              <template v-if="payment.provider === 'VIETQR'">
                <div><span>Ngân hàng</span><strong>MB Bank (970422)</strong></div>
                <div><span>Số tài khoản</span><strong>0988888888</strong></div>
                <div><span>Người nhận</span><strong>FF T-SHIRT STORE</strong></div>
              </template>
              <div><span>Số tiền</span><strong class="money">{{ Number(payment.amount || 0).toLocaleString('vi-VN') }} đ</strong></div>
            </div>
            <p class="demo-note">Đây là môi trường demo cho đồ án, không phát sinh giao dịch ngân hàng hay ví điện tử thật.</p>
            <button v-if="payment.provider !== 'MOMO'" class="close-btn" type="button" @click="emit('close')">Đóng &amp; xem đơn hàng</button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.pay-overlay{position:fixed;inset:0;z-index:99999;display:grid;place-items:center;padding:24px;background:rgba(15,23,42,.62);backdrop-filter:blur(5px)}.pay-modal{width:min(960px,100%);overflow:hidden;background:#fff;border-radius:18px;box-shadow:0 28px 80px rgba(15,23,42,.32)}.pay-header{display:flex;align-items:center;justify-content:space-between;padding:17px 20px;color:#fff}.pay-header.vietqr{background:#17263a}.pay-header.momo{background:linear-gradient(135deg,#a50064,#d20a7c)}.pay-header>button{width:36px;height:36px;color:#fff;background:rgba(255,255,255,.13);border:0;border-radius:9px;font-size:25px;cursor:pointer}.pay-brand{display:flex;align-items:center;gap:12px}.pay-brand>b{display:grid;place-items:center;width:48px;height:42px;color:#e51f32;background:#fff;border-radius:10px;font-size:25px}.pay-header.momo .pay-brand>b{width:58px;color:#a50064;font-size:13px}.pay-brand div{display:grid;gap:3px}.pay-brand strong{font-size:17px}.pay-brand small{opacity:.78}.pay-body{display:grid;grid-template-columns:minmax(360px,420px) 1fr;gap:25px;padding:24px}.qr-column{padding:16px;text-align:center;border:1px solid #dbe4ee;border-radius:14px}.qr-box{aspect-ratio:1;margin-bottom:12px;padding:5px;background:#fff;border-radius:10px}.qr-box img{display:block;width:100%;height:100%;object-fit:contain}.qr-column strong,.qr-column span{display:block}.qr-column span{margin-top:5px;color:#64748b;font-size:12px}.detail-column{display:flex;flex-direction:column;gap:12px}.waiting{padding:11px 13px;color:#9a5b00;background:#fff7df;border:1px solid #f7d17a;border-radius:9px;font-weight:700}.waiting i{display:inline-block;width:9px;height:9px;margin-right:7px;background:#f59e0b;border-radius:50%;box-shadow:0 0 0 4px rgba(245,158,11,.15)}.payment-info{padding:3px 14px;border:1px solid #dbe4ee;border-radius:12px}.payment-info div{display:flex;align-items:center;justify-content:space-between;gap:15px;padding:11px 0;border-bottom:1px dashed #dbe4ee}.payment-info div:last-child{border:0}.payment-info span{color:#64748b}.payment-info strong{text-align:right}.copy-value{display:flex;align-items:center;justify-content:flex-end;gap:8px}.copy-value button{border:1px solid #e7bfd5;border-radius:7px;padding:5px 8px;color:#a50064;background:#fff5fa;font-size:11px;font-weight:800;cursor:pointer}.money{color:#dc2626;font-size:18px}.demo-note{margin:0;padding:11px;color:#92400e;background:#fffbeb;border-radius:9px;line-height:1.5}.close-btn{margin-top:auto;padding:12px;color:#fff;background:#496883;border:0;border-radius:9px;font-weight:700;cursor:pointer}@media(max-width:700px){.pay-overlay{padding:10px}.pay-body{grid-template-columns:1fr;max-height:calc(100vh - 90px);overflow:auto}.qr-column{max-width:390px;width:100%;margin:auto}.pay-brand small{display:none}}
</style>
