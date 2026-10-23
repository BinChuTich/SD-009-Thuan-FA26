<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import api from '@/api'

const props = defineProps({
  customerId: { type: [Number, String], required: true }
})

const opened = ref(false)
const loading = ref(false)
const error = ref('')
const conversation = ref(null)
const input = ref('')
const messagesEl = ref(null)
const showSuggestions = ref(true)

const messages = computed(() => conversation.value?.messages || [])
const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
const wsUrl = `${wsProtocol}//${location.hostname}:8080/ws/chat`
let socket, reconnectTimer, disposed = false

const SIZE_GUIDE = `📏 Hướng dẫn chọn size (áo thun FF T-shirt):

• Size S  — Nặng 45–55 kg | Cao 1m50–1m60 | Ngực 84–88 cm
• Size M  — Nặng 55–65 kg | Cao 1m60–1m68 | Ngực 88–92 cm
• Size L  — Nặng 65–75 kg | Cao 1m68–1m75 | Ngực 92–96 cm
• Size XL — Nặng 75–85 kg | Cao 1m75–1m82 | Ngực 96–100 cm
• Size XXL— Nặng 85–95 kg | Cao > 1m82    | Ngực 100–106 cm

Tip: thích form rộng chọn +1 size. Cần tư vấn cụ thể hơn hãy gặp nhân viên nhé!`

function formatPrice(v) {
  if (v == null) return '—'
  return new Intl.NumberFormat('vi-VN').format(Number(v)) + '₫'
}
function nowTime() {
  return new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function pushLocal(role, text, extra = {}) {
  if (!conversation.value) conversation.value = { messages: [], mode: 'AI' }
  if (!conversation.value.messages) conversation.value.messages = []
  conversation.value.messages.push({
    id: 'local-' + Date.now() + Math.random(),
    role,
    text,
    time: nowTime(),
    ...extra
  })
}

async function startConversation() {
  if (conversation.value?.id) return
  if (!props.customerId) {
    error.value = 'Không xác định được tài khoản khách hàng. Vui lòng đăng nhập lại.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const customerId = Number(props.customerId)
    if (!Number.isInteger(customerId) || customerId <= 0) throw new Error('ID khách hàng không hợp lệ.')
    const { data: existing } = await api.get(`/api/chat/customer/${customerId}/conversations`)
    conversation.value = existing.find(c => c.status !== 'closed') || null
    if (!conversation.value) {
      const { data } = await api.post('/api/chat/conversations', { customerId, mode: 'AI' })
      conversation.value = data
    }
    await scrollBottom()
  } catch (e) {
    error.value = e.response?.data?.message || e.message || 'Không thể khởi tạo cuộc trò chuyện.'
  } finally {
    loading.value = false
  }
}

async function refreshConversation() {
  if (!conversation.value?.id) return
  try {
    const { data } = await api.get(`/api/chat/conversations/${conversation.value.id}`)
    conversation.value = data
    await scrollBottom()
  } catch {}
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  // Bắt intent local trước khi gửi knowledge
  if (await handleIntent(text)) {
    input.value = ''
    return
  }
  await startConversation()
  if (!conversation.value?.id) return
  input.value = ''
  loading.value = true
  error.value = ''
  try {
    const url = conversation.value.mode === 'NHAN_VIEN'
      ? `/api/chat/conversations/${conversation.value.id}/customer-messages`
      : `/api/chat/conversations/${conversation.value.id}/knowledge-messages`
    const { data } = await api.post(url, { text })
    conversation.value = data
    showSuggestions.value = false
    await scrollBottom()
  } catch (e) {
    error.value = e.response?.data?.message || 'Gửi tin nhắn thất bại.'
    input.value = text
  } finally {
    loading.value = false
  }
}

async function handleIntent(text) {
  const q = text.toLowerCase()
  if (q.includes('giảm giá') || q.includes('khuyến mãi') || q.includes('sale')) {
    await showPromotions()
    return true
  }
  if (q.includes('size') || q.includes('cỡ') || q.includes('tư vấn chọn')) {
    await showSizeGuide()
    return true
  }
  if (q.includes('mới nhất') || q.includes('mẫu mới') || q.includes('sản phẩm mới')) {
    await showNewestProducts()
    return true
  }
  if (q.includes('nhân viên') || q.includes('liên hệ trực tiếp') || q.includes('gặp nhân viên')) {
    await requestHuman()
    return true
  }
  return false
}

async function askSuggestion(text) {
  showSuggestions.value = false
  if (await handleIntent(text)) return
  input.value = text
  await send()
}

async function showPromotions() {
  await startConversation()
  pushLocal('customer', 'Cửa hàng đang có đợt giảm giá nào không?')
  loading.value = true
  try {
    const { data } = await api.get('/api/chat/insights/promotions')
    if (!data || data.length === 0) {
      pushLocal('ai', 'Hiện tại cửa hàng chưa có đợt giảm giá nào đang diễn ra. Bạn theo dõi fanpage để cập nhật sớm nhất nhé!')
    } else {
      pushLocal('ai', 'Các đợt giảm giá đang áp dụng:', { promotions: data })
    }
  } catch {
    pushLocal('ai', 'Không tải được danh sách giảm giá. Vui lòng thử lại hoặc gặp nhân viên.')
  } finally {
    loading.value = false
    await scrollBottom()
  }
}

async function showSizeGuide() {
  await startConversation()
  pushLocal('customer', 'Tôi muốn được tư vấn chọn size')
  pushLocal('ai', SIZE_GUIDE)
  await scrollBottom()
}

async function showNewestProducts() {
  await startConversation()
  pushLocal('customer', 'Xem các mẫu sản phẩm mới nhất')
  loading.value = true
  try {
    const { data } = await api.get('/api/chat/insights/newest-products')
    if (!data || data.length === 0) {
      pushLocal('ai', 'Chưa có sản phẩm mới trong hệ thống.')
    } else {
      pushLocal('ai', '5 sản phẩm mới nhất:', {
        products: data.map((p, i) => ({
          id: p.id,
          name: p.name,
          price: p.price,
          badge: 'Mới',
          sold: null
        }))
      })
    }
  } catch {
    pushLocal('ai', 'Không tải được sản phẩm mới. Vui lòng thử lại sau.')
  } finally {
    loading.value = false
    await scrollBottom()
  }
}

async function requestHuman() {
  await startConversation()
  if (!conversation.value?.id) return
  loading.value = true
  try {
    const { data } = await api.post(`/api/chat/conversations/${conversation.value.id}/request-human`)
    conversation.value = data
    showSuggestions.value = false
    await scrollBottom()
  } catch (e) {
    error.value = e.response?.data?.message || 'Không thể chuyển nhân viên.'
  } finally {
    loading.value = false
  }
}

/** Nút load lại – hiện lại gợi ý để khách chọn chức năng */
function resetChatView() {
  showSuggestions.value = true
  error.value = ''
  scrollBottom()
}

async function scrollBottom() {
  await nextTick()
  if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
}

function connectSocket() {
  if (disposed) return
  socket = new WebSocket(wsUrl)
  socket.onmessage = async (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'conversation-updated' && Number(data.conversationId) === Number(conversation.value?.id)) {
        await refreshConversation()
      }
    } catch (e) {
      console.error(e)
    }
  }
  socket.onclose = () => {
    if (!disposed) reconnectTimer = setTimeout(connectSocket, 2500)
  }
}

watch(opened, async (v) => {
  if (v) {
    await startConversation()
    if (!socket || socket.readyState > 1) connectSocket()
  }
})

onBeforeUnmount(() => {
  disposed = true
  clearTimeout(reconnectTimer)
  socket?.close()
})
</script>

<template>
  <div class="customer-chat-root">
    <transition name="customer-chat-pop">
      <section v-if="opened" class="customer-chat-panel">
        <header class="customer-chat-header">
          <div class="customer-chat-logo">♧</div>
          <div class="customer-chat-heading">
            <strong>Trợ lý ảo FF T-shirt</strong>
            <small><i></i> AI sẵn sàng phản hồi</small>
          </div>
          <!-- Nút load lại đoạn chat / hiện lại gợi ý -->
          <button class="customer-chat-reload" title="Làm mới gợi ý" @click="resetChatView">↻</button>
          <button class="customer-chat-min" @click="opened = false">−</button>
        </header>

        <div ref="messagesEl" class="customer-chat-messages">
          <div v-if="!messages.length" class="customer-chat-welcome">
            <div class="customer-chat-bot">♙</div>
            <strong>Xin chào! 👋</strong>
            <p>Tôi là chuyên viên ảo của cửa hàng. Tôi có thể giúp bạn tìm sản phẩm, tư vấn size và tra cứu chính sách.</p>
          </div>

          <div v-for="m in messages" :key="m.id" class="customer-chat-row" :class="m.role">
            <div class="customer-chat-bubble">
              <span style="white-space:pre-wrap">{{ m.text }}</span>

              <!-- Card giảm giá -->
              <div v-if="m.promotions?.length" class="promo-list">
                <div v-for="p in m.promotions" :key="p.id" class="promo-card">
                  <strong>{{ p.name }}</strong>
                  <span v-if="p.value">Giảm {{ p.type === 'PHAN_TRAM' ? p.value + '%' : formatPrice(p.value) }}</span>
                  <small v-if="p.endDate">Đến {{ p.endDate }}</small>
                </div>
              </div>

              <!-- Card sản phẩm -->
              <div v-if="m.products?.length" class="product-list">
                <div v-for="p in m.products" :key="p.id" class="product-card">
                  <span class="product-badge" v-if="p.badge">{{ p.badge }}</span>
                  <div class="product-name">{{ p.name }}</div>
                  <div class="product-price">{{ formatPrice(p.price) }}</div>
                </div>
              </div>
            </div>
            <small>
              {{ m.role === 'customer' ? 'Bạn' : m.role === 'agent' ? 'Nhân viên' : 'Trợ lý ảo' }}
              · {{ m.time }}
            </small>
          </div>

          <p v-if="loading" class="customer-chat-loading">Đang xử lý…</p>
          <p v-if="error" class="customer-chat-error">{{ error }}</p>

          <div v-if="showSuggestions && conversation?.mode !== 'NHAN_VIEN'" class="customer-chat-suggestions">
            <small>💡 Gợi ý câu hỏi tiếp theo:</small>
            <button @click="askSuggestion('Cửa hàng đang có đợt giảm giá nào không?')">Cửa hàng đang có đợt giảm giá nào không?</button>
            <button @click="askSuggestion('Tôi muốn được tư vấn chọn size')">Tôi muốn được tư vấn chọn size</button>
            <button @click="askSuggestion('Xem các mẫu sản phẩm mới nhất')">Xem các mẫu sản phẩm mới nhất</button>
            <button @click="askSuggestion('Liên hệ trực tiếp với nhân viên')">Liên hệ trực tiếp với nhân viên</button>
          </div>
        </div>

        <div class="customer-chat-human">
          <span>Bạn chưa tìm thấy thông tin phù hợp?</span>
          <button @click="requestHuman">♧ Gặp nhân viên</button>
        </div>

        <form class="customer-chat-input" @submit.prevent="send">
          <input v-model="input" placeholder="Nhập câu hỏi của bạn tại đây..." :disabled="loading" />
          <button type="submit" :disabled="!input.trim() || loading">➤</button>
        </form>
      </section>
    </transition>

    <button class="customer-chat-launcher" :class="{ active: opened }" @click="opened = !opened">
      <span v-if="!opened">☏</span><span v-else>×</span>
      <i v-if="!opened"></i>
    </button>
  </div>
</template>

<style scoped>
/* Giữ CSS cũ + bổ sung */
.customer-chat-root{position:fixed;right:24px;bottom:24px;z-index:99999;font-family:Inter,Arial,sans-serif;color:#252b36}
.customer-chat-launcher{position:relative;width:58px;height:58px;border:0;border-radius:50%;background:#496883;color:white;font-size:30px;box-shadow:0 8px 24px #9d001c42;cursor:pointer}
.customer-chat-launcher i{position:absolute;right:2px;top:1px;width:12px;height:12px;border:2px solid white;border-radius:50%;background:#18c37e}
.customer-chat-launcher.active{font-size:34px}
.customer-chat-panel{position:absolute;right:0;bottom:76px;width:min(390px,calc(100vw - 28px));height:min(590px,calc(100dvh - 120px));min-height:390px;background:#fff;border-radius:22px;overflow:hidden;box-shadow:0 18px 60px #18203330;display:flex;flex-direction:column;border:1px solid #eceef2}
.customer-chat-header{height:68px;flex-shrink:0;background:#496883;color:#fff;display:flex;align-items:center;padding:0 12px 0 17px;gap:8px}
.customer-chat-logo{width:36px;height:36px;border-radius:50%;background:#ffffff26;display:grid;place-items:center;font-size:22px}
.customer-chat-heading{display:flex;flex-direction:column;gap:4px;flex:1}
.customer-chat-heading strong{font-size:14px}
.customer-chat-heading small{font-size:11px;color:#ffe2e5}
.customer-chat-heading i{display:inline-block;width:7px;height:7px;background:#21d18b;border-radius:50%;margin-right:4px}
.customer-chat-reload,.customer-chat-min{border:0;background:transparent;color:white;font-size:20px;cursor:pointer;width:32px;height:32px;border-radius:6px}
.customer-chat-reload:hover,.customer-chat-min:hover{background:rgba(255,255,255,.18)}
.customer-chat-messages{flex:1;overflow:auto;padding:16px 14px 10px;background:#fff;scroll-behavior:smooth}
.customer-chat-welcome{text-align:center;padding:8px 14px 18px}
.customer-chat-bot{width:52px;height:52px;border-radius:50%;background:#fff0f1;color:#d5091b;display:grid;place-items:center;font-size:27px;margin:0 auto 12px}
.customer-chat-welcome strong{font-size:15px}
.customer-chat-welcome p{font-size:12px;line-height:1.55;color:#747b88;margin:8px 0}
.customer-chat-row{display:flex;flex-direction:column;margin:10px 0;max-width:92%}
.customer-chat-row.customer{align-self:flex-end;margin-left:auto;align-items:flex-end}
.customer-chat-row.ai,.customer-chat-row.agent{align-items:flex-start}
.customer-chat-bubble{font-size:13px;line-height:1.5;padding:10px 12px;border-radius:13px;background:#f3f4f7;color:#303643;overflow-wrap:anywhere}
.customer-chat-row.customer .customer-chat-bubble{background:#496883;color:white;border-bottom-right-radius:4px}
.customer-chat-row small{font-size:10px;color:#959ba5;margin:4px 2px}
.customer-chat-suggestions{display:grid;gap:7px;margin-top:16px}
.customer-chat-suggestions small{color:#8b919b;font-size:11px}
.customer-chat-suggestions button{background:#fff;border:1px solid #eceef1;border-radius:11px;text-align:left;padding:10px;font-size:12px;color:#444b57;cursor:pointer}
.customer-chat-suggestions button:hover{border-color:#e8a4aa;background:#fff8f8}
.customer-chat-human{background:#fff3f3;border-top:1px solid #ffe0e2;padding:9px 12px;display:flex;align-items:center;justify-content:space-between;gap:6px;font-size:10px;color:#a72b38}
.customer-chat-human button{background:white;border:1px solid #f2c3c7;border-radius:8px;padding:8px;color:#b20c1e;font-size:11px;font-weight:600;cursor:pointer}
.customer-chat-input{padding:10px 12px;display:flex;gap:8px;border-top:1px solid #f1f2f4;background:#fff}
.customer-chat-input input{min-width:0;flex:1;border:0;outline:0;background:#f5f6f8;border-radius:24px;padding:12px 14px;font-size:12px}
.customer-chat-input button{width:40px;height:40px;border:0;border-radius:50%;background:#eef1f5;color:#a6afbd;font-size:18px;cursor:pointer}
.customer-chat-input button:enabled{background:#d5091b;color:white}
.customer-chat-error{color:#c30d21;font-size:12px}
.customer-chat-loading{font-size:12px;color:#888}
.promo-list,.product-list{display:flex;flex-direction:column;gap:8px;margin-top:10px}
.promo-card,.product-card{background:#fff;border:1px solid #eceef1;border-radius:10px;padding:8px 10px;display:flex;flex-direction:column;gap:2px}
.promo-card strong,.product-name{font-size:12px;font-weight:600}
.product-price{font-size:13px;font-weight:700;color:#d5091b}
.product-badge{align-self:flex-start;background:#d5091b;color:#fff;font-size:9px;font-weight:700;padding:1px 6px;border-radius:4px}
.customer-chat-pop-enter-active,.customer-chat-pop-leave-active{transition:all .18s ease}
.customer-chat-pop-enter-from,.customer-chat-pop-leave-to{opacity:0;transform:translateY(12px) scale(.98)}
@media(max-width:480px){.customer-chat-root{right:12px;bottom:12px}.customer-chat-panel{bottom:70px;width:calc(100vw - 24px)}}
</style>