<script setup>
import { ref, reactive, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'

const router = useRouter()
const isOpen = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const chatBodyRef = ref(null)

const messages = reactive([
  {
    id: 1,
    sender: 'bot',
    text: 'Xin chào! FF T-Shirt có thể hỗ trợ gì cho bạn hôm nay?',
    time: 'Vừa xong',
    actions: [
      { text: '📏 Tư vấn chọn size', type: 'size' },
      { text: '🚚 Tra cứu đơn hàng', type: 'track' },
      { text: '🔥 Xem ưu đãi hôm nay', type: 'promo' },
      { text: '💬 Gặp nhân viên tư vấn', type: 'agent' }
    ]
  }
])

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

const handleActionClick = (action) => {
  if (action.type === 'size') {
    sendMessageText('Tôi muốn tư vấn chọn size áo phông')
    showBotReply('Bạn hãy cho shop biết Chiều cao (cm) và Cân nặng (kg) nhé!\n\nVí dụ: 1m70 65kg', [
      { text: '1m65, 55kg (Size M)', type: 'custom-size', val: 'M' },
      { text: '1m72, 68kg (Size L)', type: 'custom-size', val: 'L' },
      { text: '1m78, 78kg (Size XL)', type: 'custom-size', val: 'XL' }
    ])
  } else if (action.type === 'track') {
    sendMessageText('Tôi muốn tra cứu tình trạng đơn hàng')
    showBotReply('Bạn vui lòng nhập Mã đơn hàng (Ví dụ: HD00001) hoặc Số điện thoại để hệ thống kiểm tra ngay nhé!')
  } else if (action.type === 'promo') {
    sendMessageText('Hôm nay có chương trình khuyến mãi gì?')
    showBotReply('🎉 Hiện tại FF T-Shirt đang có các ưu đãi cực sốc:\n- Giảm tới 30% bộ sưu tập Áo Polo & Basic\n- Miễn phí vận chuyển toàn quốc cho đơn từ 500.000đ\n- Áp dụng Voucher VC-GIAM50K giảm 50k!', [
      { text: '👉 Xem ngay tại Cửa hàng', type: 'link-shop' }
    ])
  } else if (action.type === 'agent') {
    sendMessageText('Tôi muốn gặp nhân viên tư vấn')
    showBotReply('Dạ vâng! Nhân viên CSKH FF T-Shirt đã sẵn sàng kết nối. Bạn hãy để lại câu hỏi hoặc số điện thoại, chuyên viên sẽ phản hồi bạn trong 1 - 2 phút ạ!')
  } else if (action.type === 'custom-size') {
    sendMessageText(`Gợi ý size: ${action.text}`)
    showBotReply(`Form dáng áo phông FF T-Shirt được may chuẩn tỉ lệ người Việt. Với thông số trên, Size ${action.val} sẽ ôm vừa vặn thanh lịch; nếu bạn thích mặc form rộng thoải mái (Oversize) có thể tăng thêm 1 size nhé! ❤️`, [
      { text: '🛍️ Đi đến Cửa hàng chọn áo', type: 'link-shop' }
    ])
  } else if (action.type === 'link-shop') {
    isOpen.value = false
    router.push('/cua-hang')
  }
}

const sendMessageText = (txt) => {
  messages.push({
    id: Date.now(),
    sender: 'user',
    text: txt,
    time: 'Vừa xong'
  })
  scrollToBottom()
}

const showBotReply = (replyText, actions = null) => {
  isTyping.value = true
  setTimeout(() => {
    isTyping.value = false
    messages.push({
      id: Date.now(),
      sender: 'bot',
      text: replyText,
      time: 'Vừa xong',
      actions: actions
    })
    scrollToBottom()
  }, 600)
}

const handleSend = () => {
  const txt = inputMessage.value.trim()
  if (!txt) return
  sendMessageText(txt)
  inputMessage.value = ''

  // Phân tích câu hỏi
  const lower = txt.toLowerCase()
  if (lower.includes('size') || lower.includes('kg') || lower.includes('cm') || lower.includes('nặng') || lower.includes('cao')) {
    showBotReply('Bảng size chuẩn FF T-Shirt:\n- Size S: 48 - 55kg (dưới 1m65)\n- Size M: 56 - 65kg (1m65 - 1m70)\n- Size L: 66 - 74kg (1m71 - 1m76)\n- Size XL: 75 - 84kg (1m77 - 1m83)\n- Size XXL: Trên 85kg\nBạn đang quan tâm mẫu nào ạ?', [
      { text: 'Xem Cửa hàng ngay', type: 'link-shop' }
    ])
  } else if (lower.startsWith('hd') || lower.length >= 10 && /^\d+$/.test(lower)) {
    showBotReply(`Đang kiểm tra thông tin đơn "${txt}"... Bạn có thể bấm nút bên dưới để mở trang tra cứu chi tiết nhé!`, [
      { text: `🔍 Mở tra cứu đơn ${txt}`, type: 'track-direct', code: txt }
    ])
  } else {
    showBotReply('Cảm ơn bạn đã nhắn tin cho FF T-Shirt! Đội ngũ tư vấn sẽ hỗ trợ bạn ngay tức thì. Bạn cũng có thể liên hệ nhanh qua hotline 1900 6677.', [
      { text: '📏 Tư vấn size', type: 'size' },
      { text: '🔥 Xem ưu đãi', type: 'promo' }
    ])
  }
}
</script>

<template>
  <div class="chat-widget-root">
    <!-- NÚT BONG BÓNG CHAT NỔI (MATCH VIDEO) -->
    <button class="chat-floating-btn" @click="toggleChat" :class="{ open: isOpen }" title="Chat với hỗ trợ">
      <svg v-if="!isOpen" width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
      </svg>
      <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <line x1="18" y1="6" x2="6" y2="18"></line>
        <line x1="6" y1="6" x2="18" y2="18"></line>
      </svg>
      <span v-if="!isOpen" class="online-indicator"></span>
    </button>

    <!-- CỬA SỔ HỘP THOẠI CHAT -->
    <div v-if="isOpen" class="chat-window-box">
      <!-- Chat Header mang màu thương hiệu và logo FF -->
      <div class="chat-header">
        <div class="header-info">
          <div class="brand-avatar-box">
            <span class="chat-ray ray-1"></span>
            <span class="chat-ray ray-2"></span>
            <span class="chat-ray ray-3"></span>
            <span class="f-blue-char">F</span>
            <span class="f-gold-char">F</span>
            <span class="status-dot"></span>
          </div>
          <div>
            <h4>FF T-Shirt Support</h4>
            <span class="status-text">Đang trực tuyến • Sẵn sàng hỗ trợ</span>
          </div>
        </div>
        <button class="btn-close-chat" @click="isOpen = false">×</button>
      </div>

      <!-- Chat Body -->
      <div class="chat-body" ref="chatBodyRef">
        <div class="chat-notice-box">
          Chào mừng bạn đến với <strong>FF T-Shirt</strong>! Chúng tôi luôn ở đây để giúp bạn chọn được chiếc áo ưng ý nhất.
        </div>

        <div v-for="msg in messages" :key="msg.id" class="message-row" :class="msg.sender">
          <div v-if="msg.sender === 'bot'" class="bot-avatar">
            <span class="f-w">F</span><span class="f-g">F</span>
          </div>
          <div class="bubble-wrap">
            <div class="message-bubble" :class="msg.sender">
              <p style="white-space: pre-line; margin: 0;">{{ msg.text }}</p>
            </div>
            
            <!-- Quick actions / Suggestions -->
            <div v-if="msg.actions && msg.actions.length > 0" class="actions-list">
              <button
                v-for="(act, idx) in msg.actions"
                :key="idx"
                class="action-pill-btn"
                @click="handleActionClick(act)"
              >
                {{ act.text }}
              </button>
            </div>
          </div>
        </div>

        <!-- Typing Indicator -->
        <div v-if="isTyping" class="message-row bot">
          <div class="bot-avatar">
            <span class="f-w">F</span><span class="f-g">F</span>
          </div>
          <div class="typing-bubble">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- Chat Input Footer -->
      <div class="chat-footer">
        <form @submit.prevent="handleSend" class="input-form">
          <input
            v-model="inputMessage"
            type="text"
            placeholder="Nhập tin nhắn tư vấn hoặc mã đơn..."
            class="chat-input"
          />
          <button type="submit" class="btn-send" :disabled="!inputMessage.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-widget-root {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

/* Nút tròn nổi mang màu chủ đạo #496883 */
.chat-floating-btn {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background-color: #496883;
  color: #ffffff;
  border: none;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(73, 104, 131, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  position: relative;
}

.chat-floating-btn:hover {
  background-color: #364f66;
  transform: scale(1.08);
}

.chat-floating-btn.open {
  background-color: #364f66;
}

.online-indicator {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 13px;
  height: 13px;
  background-color: #22c55e;
  border: 2.5px solid #ffffff;
  border-radius: 50%;
}

/* Hộp thoại chat */
.chat-window-box {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 360px;
  height: 520px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 35px rgba(73, 104, 131, 0.2);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: popIn 0.25s ease-out;
}

@keyframes popIn {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.chat-header {
  background: linear-gradient(135deg, #364f66 0%, #496883 100%);
  color: #ffffff;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-avatar-box {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Fredoka', sans-serif;
  font-weight: 800;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.chat-ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 2px;
}

.chat-ray.ray-1 {
  width: 2.5px;
  height: 6px;
  top: 3px;
  left: 6px;
  transform: rotate(-30deg);
}

.chat-ray.ray-2 {
  width: 2.5px;
  height: 6px;
  top: 1px;
  left: 12px;
  transform: rotate(0deg);
}

.chat-ray.ray-3 {
  width: 2.5px;
  height: 6px;
  top: 4px;
  left: 18px;
  transform: rotate(30deg);
}

.f-blue-char {
  color: #496883;
}

.f-gold-char {
  color: #d2a764;
  margin-left: 1px;
}

.brand-avatar-box .status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #22c55e;
  border: 2px solid #496883;
  border-radius: 50%;
}

.header-info h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
}

.status-text {
  font-size: 11px;
  color: #e2e8f0;
}

.btn-close-chat {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  opacity: 0.8;
}
.btn-close-chat:hover {
  opacity: 1;
}

.chat-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-notice-box {
  background: #f0f7ff;
  color: #364f66;
  border: 1px solid #d9eafd;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
}

.message-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.message-row.user {
  justify-content: flex-end;
}

.bot-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #496883;
  font-size: 12px;
  font-family: 'Fredoka', sans-serif;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(73, 104, 131, 0.2);
}

.bot-avatar .f-w {
  color: #ffffff;
}

.bot-avatar .f-g {
  color: #d2a764;
  margin-left: 1px;
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 82%;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.45;
}

.message-bubble.bot {
  background: #ffffff;
  color: #1e293b;
  border-bottom-left-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
}

.message-bubble.user {
  background: #496883;
  color: #ffffff;
  border-bottom-right-radius: 2px;
}

.actions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.action-pill-btn {
  background: #ffffff;
  color: #496883;
  border: 1px solid #cbd5e1;
  border-radius: 16px;
  padding: 5px 11px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-pill-btn:hover {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.typing-bubble {
  background: #ffffff;
  padding: 8px 14px;
  border-radius: 14px;
  display: inline-flex;
  gap: 4px;
  align-items: center;
  border: 1px solid #e2e8f0;
}

.typing-bubble span {
  width: 6px;
  height: 6px;
  background: #496883;
  border-radius: 50%;
  animation: blink 1.2s infinite both;
}

.typing-bubble span:nth-child(2) { animation-delay: 0.2s; }
.typing-bubble span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.1); }
}

.chat-footer {
  padding: 10px 14px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
}

.input-form {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 9px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.btn-send {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #496883;
  color: #ffffff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-send:hover:not(:disabled) {
  background: #364f66;
}

.btn-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .chat-window-box {
    width: calc(100vw - 32px);
    right: -8px;
    height: 480px;
  }
}
</style>

