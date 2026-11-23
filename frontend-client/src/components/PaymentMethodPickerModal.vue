<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  open: Boolean,
  currentMethod: { type: String, default: '' },
  loading: Boolean
})
const emit = defineEmits(['close', 'continue'])
const selected = ref('')

watch(() => props.open, (open) => {
  if (open) selected.value = props.currentMethod || 'VIETQR'
})

const methods = [
  { value: 'VIETQR', badge: 'V', title: 'Chuyển khoản VietQR', desc: 'Quét QR ngân hàng qua Napas 247', cls: 'vietqr' },
  { value: 'MOMO', badge: 'momo', title: 'Ví MoMo', desc: 'Thanh toán mô phỏng bằng điện thoại', cls: 'momo' },
  { value: 'VNPAY', badge: 'VN', title: 'Cổng VNPAY', desc: 'Thanh toán qua cổng VNPAY Sandbox', cls: 'vnpay' },
  { value: 'COD', badge: 'COD', title: 'Thanh toán khi nhận hàng', desc: 'Thanh toán cho nhân viên giao hàng', cls: 'cod' }
]
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="picker-overlay" @click.self="emit('close')">
      <section class="picker-modal">
        <header><div><h2>Chọn phương thức thanh toán</h2><p>Bạn có thể giữ phương thức hiện tại hoặc chuyển sang phương thức khác.</p></div><button @click="emit('close')">×</button></header>
        <div class="method-list">
          <label v-for="method in methods" :key="method.value" :class="['method-item', { selected: selected === method.value }]">
            <input v-model="selected" type="radio" :value="method.value" />
            <span :class="['method-badge', method.cls]">{{ method.badge }}</span>
            <span class="method-copy"><strong>{{ method.title }}</strong><small>{{ method.desc }}</small></span>
            <span class="radio-dot"></span>
          </label>
        </div>
        <footer><button class="cancel" :disabled="loading" @click="emit('close')">Hủy</button><button class="continue" :disabled="!selected || loading" @click="emit('continue', selected)">{{ loading ? 'Đang xử lý...' : (selected === 'COD' ? 'Xác nhận chuyển COD' : 'Tiếp tục thanh toán') }}</button></footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.picker-overlay{position:fixed;inset:0;z-index:99998;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.58);backdrop-filter:blur(5px)}.picker-modal{width:min(560px,100%);overflow:hidden;background:#fff;border-radius:18px;box-shadow:0 26px 80px rgba(15,23,42,.3)}header{display:flex;justify-content:space-between;gap:15px;padding:22px 24px 17px;border-bottom:1px solid #e5eaf0}h2{margin:0;color:#17263a;font-size:21px}header p{margin:6px 0 0;color:#64748b;font-size:13px;line-height:1.45}header button{width:36px;height:36px;flex:none;color:#64748b;background:#f1f5f9;border:0;border-radius:9px;font-size:24px;cursor:pointer}.method-list{display:grid;gap:10px;padding:18px 24px}.method-item{display:grid;grid-template-columns:auto 48px 1fr auto;align-items:center;gap:12px;padding:13px 14px;border:1.5px solid #dce4ed;border-radius:12px;cursor:pointer;transition:.18s}.method-item:hover{border-color:#93b0c8;background:#f8fbfd}.method-item.selected{border-color:#2563eb;background:#eff6ff;box-shadow:0 0 0 3px rgba(37,99,235,.08)}.method-item input{position:absolute;opacity:0}.method-badge{display:grid;place-items:center;width:46px;height:38px;color:#fff;border-radius:9px;font-size:12px;font-weight:900}.method-badge.vietqr{background:#e51f32;font-size:23px}.method-badge.momo{background:#a50064}.method-badge.vnpay{background:#0756a5}.method-badge.cod{background:#0f766e}.method-copy{display:grid;gap:4px}.method-copy strong{color:#1e293b;font-size:14px}.method-copy small{color:#64748b}.radio-dot{width:20px;height:20px;border:2px solid #cbd5e1;border-radius:50%}.selected .radio-dot{border:6px solid #2563eb}footer{display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;background:#f8fafc;border-top:1px solid #e5eaf0}footer button{min-height:42px;padding:0 18px;border-radius:9px;font-weight:700;cursor:pointer}.cancel{color:#475569;background:#fff;border:1px solid #cbd5e1}.continue{color:#fff;background:#0756a5;border:1px solid #0756a5}.continue:disabled,footer button:disabled{opacity:.55;cursor:not-allowed}@media(max-width:520px){.picker-overlay{padding:10px}.method-list{padding:15px}.picker-modal header{padding:18px 16px}.method-item{grid-template-columns:42px 1fr auto}.method-item>input{display:none}.method-badge{width:42px}.picker-modal footer{padding:14px 16px}.picker-modal footer button{flex:1;padding:0 10px}}
.method-item{grid-template-columns:48px 1fr auto}
</style>
