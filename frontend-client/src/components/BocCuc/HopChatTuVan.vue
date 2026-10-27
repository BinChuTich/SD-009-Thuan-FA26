<script setup>
import { ref, reactive, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { currentClientUser } from '@/utils/taiKhoanKhachHang.js'

const router = useRouter()
const isOpen = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const chatBodyRef = ref(null)
const conversationId = ref(null)
const unreadForClient = ref(0)
let pollTimer = null

const CHAT_STORAGE_KEY = 'fftshirt_client_chat_conv_id'

const messages = ref([
  {
    id: 'welcome-0',
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

// Phát âm thanh nhẹ khi có tin nhắn mới
const playNotificationSound = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(587.33, ctx.currentTime) // D5
    osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1) // A5
    gain.gain.setValueAtTime(0.08, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.35)
  } catch (e) {}
}

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    unreadForClient.value = 0
    initOrLoadConversation()
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

// Khởi tạo cuộc trò chuyện kết nối Spring Boot Backend
const initOrLoadConversation = async () => {
  try {
    const savedId = localStorage.getItem(CHAT_STORAGE_KEY)
    const payload = {
      khachHangId: currentClientUser.value?.id || null,
      tenKhachHang: currentClientUser.value?.name || null,
      soDienThoai: currentClientUser.value?.phone || null
    }

    const res = await api.post('/api/chat/khoi-tao', payload)
    if (res.data && res.data.id) {
      conversationId.value = res.data.id
      localStorage.setItem(CHAT_STORAGE_KEY, String(res.data.id))
      await loadMessages()
    }
  } catch (e) {
    console.warn('Lỗi kết nối chat server:', e)
  }
}

// Nạp lịch sử tin nhắn từ cơ sở dữ liệu
const loadMessages = async () => {
  if (!conversationId.value) return
  try {
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/tin-nhan`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      const dbMsgs = res.data.map(m => ({
        id: m.id,
        sender: m.nguoiGui === 'KHACH_HANG' ? 'user' : 'bot',
        text: m.noiDung,
        staffName: m.tenNhanVien || (m.nguoiGui === 'NHAN_VIEN' ? 'Nhân viên FF' : null),
        time: formatMsgTime(m.ngayTao)
      }))

      // Giữ tin nhắn chào mừng và các action buttons ở đầu
      messages.value = [
        {
          id: 'welcome-0',
          sender: 'bot',
          text: 'Xin chào! FF T-Shirt có thể hỗ trợ gì cho bạn hôm nay?',
          time: 'Vừa xong',
          actions: [
            { text: '📏 Tư vấn chọn size', type: 'size' },
            { text: '🚚 Tra cứu đơn hàng', type: 'track' },
            { text: '🔥 Xem ưu đãi hôm nay', type: 'promo' },
            { text: '💬 Gặp nhân viên tư vấn', type: 'agent' }
          ]
        },
        ...dbMsgs
      ]
      scrollToBottom()

      // Đánh dấu đã đọc
      api.put(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/da-doc?role=KHACH_HANG`).catch(() => {})
    }
  } catch (e) {
    console.warn('Lỗi nạp tin nhắn chat:', e)
  }
}

// Polling kiểm tra tin nhắn mới từ Admin
const pollNewMessages = async () => {
  if (!conversationId.value) return
  try {
    const numericIds = messages.value
      .map(m => typeof m.id === 'number' ? m.id : 0)
      .filter(n => n > 0)
    const lastId = numericIds.length > 0 ? Math.max(...numericIds) : 0

    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/tin-nhan?lastId=${lastId}`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      let hasNewStaffMsg = false

      res.data.forEach(m => {
        // Tránh trùng tin nhắn
        if (!messages.value.some(existing => existing.id === m.id)) {
          const isStaff = m.nguoiGui === 'NHAN_VIEN'
          if (isStaff) hasNewStaffMsg = true
          messages.value.push({
            id: m.id,
            sender: m.nguoiGui === 'KHACH_HANG' ? 'user' : 'bot',
            text: m.noiDung,
            staffName: m.tenNhanVien || (isStaff ? 'Chuyên viên FF' : null),
            time: formatMsgTime(m.ngayTao)
          })
        }
      })

      if (hasNewStaffMsg) {
        playNotificationSound()
        if (!isOpen.value) {
          unreadForClient.value += 1
        } else {
          api.put(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/da-doc?role=KHACH_HANG`).catch(() => {})
        }
      }
      scrollToBottom()
    }
  } catch (e) {}
}

const formatMsgTime = (dtStr) => {
  if (!dtStr) return 'Vừa xong'
  try {
    const d = new Date(dtStr)
    const hours = String(d.getHours()).padStart(2, '0')
    const mins = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${mins}`
  } catch (e) {
    return 'Vừa xong'
  }
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
    sendMessageText('Tôi muốn gặp nhân viên tư vấn trực tiếp')
    requestStaffSupport()
  } else if (action.type === 'custom-size') {
    sendMessageText(`Gợi ý size: ${action.text}`)
    showBotReply(`Form dáng áo phông FF T-Shirt được may chuẩn tỉ lệ người Việt. Với thông số trên, Size ${action.val} sẽ ôm vừa vặn thanh lịch; nếu bạn thích mặc form rộng thoải mái (Oversize) có thể tăng thêm 1 size nhé! ❤️`, [
      { text: '🛍️ Đi đến Cửa hàng chọn áo', type: 'link-shop' }
    ])
  } else if (action.type === 'link-shop') {
    isOpen.value = false
    router.push('/cua-hang')
  } else if (action.type === 'track-direct' && action.code) {
    isOpen.value = false
    router.push(`/tra-cuu?ma=${encodeURIComponent(action.code)}`)
  }
}

// Gửi tin nhắn đồng bộ lên Database
const sendMessageText = async (txt) => {
  const tempId = Date.now()
  messages.value.push({
    id: tempId,
    sender: 'user',
    text: txt,
    time: 'Vừa xong'
  })
  scrollToBottom()

  try {
    if (!conversationId.value) {
      await initOrLoadConversation()
    }

    const payload = {
      cuocHoiThoaiId: conversationId.value,
      khachHangId: currentClientUser.value?.id || null,
      tenKhachHang: currentClientUser.value?.name || null,
      soDienThoai: currentClientUser.value?.phone || null,
      nguoiGui: 'KHACH_HANG',
      noiDung: txt
    }

    const res = await api.post('/api/chat/gui-tin-nhan', payload)
    if (res.data && res.data.id) {
      // Cập nhật lại ID từ DB cho tin nhắn tạm
      const match = messages.value.find(m => m.id === tempId)
      if (match) match.id = res.data.id
    }
  } catch (e) {
    console.warn('Lỗi lưu tin nhắn:', e)
  }
}

const showBotReply = (replyText, actions = null) => {
  isTyping.value = true
  setTimeout(() => {
    isTyping.value = false
    messages.value.push({
      id: 'bot-' + Date.now(),
      sender: 'bot',
      text: replyText,
      time: 'Vừa xong',
      actions: actions
    })
    scrollToBottom()
  }, 400)
}

const handleSend = () => {
  const txt = inputMessage.value.trim()
  if (!txt) return
  sendMessageText(txt)
  inputMessage.value = ''

  // Phân tích câu hỏi để bot gợi ý thông minh ngay lập tức
  const lower = txt.toLowerCase()
  if (lower.includes('size') || lower.includes('kg') || lower.includes('cm') || lower.includes('nặng') || lower.includes('cao')) {
    showBotReply('Bảng size chuẩn FF T-Shirt:\n- Size S: 48 - 55kg (dưới 1m65)\n- Size M: 56 - 65kg (1m65 - 1m70)\n- Size L: 66 - 74kg (1m71 - 1m76)\n- Size XL: 75 - 84kg (1m77 - 1m83)\n- Size XXL: Trên 85kg\nBạn đang quan tâm mẫu nào ạ?', [
      { text: '🛍️ Xem Cửa hàng ngay', type: 'link-shop' }
    ])
  } else if (lower.startsWith('hd') || (lower.length >= 10 && /^\d+$/.test(lower))) {
    showBotReply(`Đang kiểm tra thông tin đơn "${txt}"... Bạn có thể bấm nút bên dưới để mở trang tra cứu chi tiết nhé!`, [
      { text: `🔍 Mở tra cứu đơn ${txt}`, type: 'track-direct', code: txt }
    ])
  } else if (lower.includes('người bán') || lower.includes('nhân viên') || lower.includes('tư vấn') || lower.includes('gặp')) {
    requestStaffSupport()
  } else {
    showBotReply('Dạ, câu hỏi này nằm ngoài dữ liệu tự động của bot. Em đã chuyển yêu cầu của anh/chị đến nhân viên tư vấn của FF T-Shirt rồi ạ. Quý khách vui lòng chờ trong giây lát nhé! ❤️')
    // Tự động đẩy phiên chat sang màn hình Admin (trạng thái Chờ nhận)
    requestStaffSupport()
  }
}

const requestStaffSupport = async () => {
  try {
    if (!conversationId.value) {
      await initOrLoadConversation()
    }
    if (conversationId.value) {
      await api.put(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/yeu-cau-ho-tro`)
      await loadMessages()
    }
  } catch (e) {
    showBotReply('Hệ thống đang kết nối bạn với nhân viên. Vui lòng chờ trong giây lát...')
  }
}

onMounted(() => {
  // Lấy ID cuộc trò chuyện đã lưu nếu có
  const savedId = localStorage.getItem(CHAT_STORAGE_KEY)
  if (savedId) {
    conversationId.value = Number(savedId)
  }

  // Khởi động chu kỳ polling định kỳ mỗi 3s
  pollTimer = setInterval(pollNewMessages, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

// Khi người dùng đăng nhập hoặc đổi tài khoản, kết nối lại cuộc trò chuyện
watch(() => currentClientUser.value, () => {
  if (isOpen.value) {
    initOrLoadConversation()
  }
})
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
      <span v-if="!isOpen && unreadForClient === 0" class="online-indicator"></span>
      <span v-if="!isOpen && unreadForClient > 0" class="client-unread-badge">{{ unreadForClient }}</span>
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
        <button class="btn-close-chat" @click="isOpen = false">✕</button>
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
            <div v-if="msg.staffName" class="staff-sender-name">
              👨‍💼 {{ msg.staffName }}
            </div>
            <div class="message-bubble" :class="msg.sender">
              <p style="white-space: pre-line; margin: 0;">{{ msg.text }}</p>
            </div>
            <span class="msg-timestamp">{{ msg.time }}</span>
            
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
  bottom: 28px;
  right: 28px;
  z-index: 99999;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

/* 1. Nút bong bóng chat nổi */
.chat-floating-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #496883 0%, #2f4557 100%);
  color: #d2a764;
  border: 2px solid #d2a764;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.chat-floating-btn:hover {
  transform: scale(1.08) translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.45);
  background: #0f172a;
}

.chat-floating-btn.open {
  background: #334155;
  border-color: #94a3b8;
  color: #ffffff;
  transform: rotate(90deg);
}

.online-indicator {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 13px;
  height: 13px;
  background-color: #22c55e;
  border: 2px solid #ffffff;
  border-radius: 50%;
  animation: pulse-ring 2s infinite;
}

.client-unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  animation: pulse-ring 1.5s infinite;
}

@keyframes pulse-ring {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(34, 197, 94, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

/* 2. Cửa sổ chat nổi */
.chat-window-box {
  position: absolute;
  bottom: 74px;
  right: 0;
  width: 380px;
  height: 540px;
  max-height: calc(100vh - 120px);
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Header */
.chat-header {
  background: linear-gradient(135deg, #496883 0%, #2f4557 100%);
  color: #ffffff;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 2px solid #d2a764;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-avatar-box {
  width: 38px;
  height: 38px;
  background: #ffffff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-weight: 900;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.f-blue-char { color: #496883; }
.f-gold-char { color: #d2a764; }

.status-dot {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  background: #22c55e;
  border: 2px solid #0f172a;
  border-radius: 50%;
}

.header-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
}

.status-text {
  font-size: 11px;
  color: #94a3b8;
  display: block;
  margin-top: 2px;
}

.btn-close-chat {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.btn-close-chat:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
}

/* Body */
.chat-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-notice-box {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e40af;
  font-size: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  line-height: 1.5;
  text-align: center;
}

.message-row {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.message-row.bot {
  align-self: flex-start;
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.bot-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1e293b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 900;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.bot-avatar .f-w { color: #ffffff; }
.bot-avatar .f-g { color: #ecd8af; }

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.staff-sender-name {
  font-size: 11px;
  font-weight: 700;
  color: #b45309;
  margin-bottom: 2px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 13.5px;
  line-height: 1.5;
  word-break: break-word;
}

.message-bubble.bot {
  background: #ffffff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-top-left-radius: 4px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.03);
}

.message-bubble.user {
  background: #1e293b;
  color: #ffffff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(30, 41, 59, 0.15);
}

.msg-timestamp {
  font-size: 10px;
  color: #94a3b8;
  align-self: flex-end;
  margin-top: 2px;
}

.message-row.bot .msg-timestamp {
  align-self: flex-start;
}

/* Actions list */
.actions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.action-pill-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #1e293b;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.action-pill-btn:hover {
  background: #1e293b;
  color: #ecd8af;
  border-color: #1e293b;
  transform: translateY(-1px);
}

/* Typing bubble */
.typing-bubble {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 10px 16px;
  border-radius: 16px;
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-bubble span {
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-bubble span:nth-child(2) { animation-delay: 0.2s; }
.typing-bubble span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-6px); }
}

/* Footer */
.chat-footer {
  padding: 12px 14px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.input-form {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 24px;
  font-size: 13.5px;
  outline: none;
  background: #f8fafc;
  transition: all 0.2s;
}

.chat-input:focus {
  background: #ffffff;
  border-color: #1e293b;
  box-shadow: 0 0 0 3px rgba(30, 41, 59, 0.1);
}

.btn-send {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #1e293b;
  color: #ecd8af;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.btn-send:hover:not(:disabled) {
  background: #0f172a;
  transform: scale(1.05);
}

.btn-send:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .chat-window-box {
    width: calc(100vw - 32px);
    right: 0;
    bottom: 74px;
  }
}
</style>
