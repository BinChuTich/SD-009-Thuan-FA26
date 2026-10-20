<template>
  <div class="pos-page-wrapper support-page">
    
    <!-- Header -->
    <div class="support-header">
      <div>
        <h1 class="page-title">Hỗ trợ khách hàng trực tuyến</h1>
        <p class="page-desc">Quản lý và trả lời chat khách hàng theo thời gian thực.</p>
      </div>
      <div class="support-head-actions">
        <button class="btn-status-online">
          <span class="status-dot"></span>
          Ca làm việc
        </button>
        <button class="icon-btn" title="Thông báo">
          🔔
          <span class="badge">6</span>
        </button>
      </div>
    </div>

    <!-- Main 2-column layout -->
    <div class="support-grid">
      <!-- LEFT: Danh sách chat -->
      <aside class="pos-panel-card sidebar-panel">
        <div class="sidebar-search">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm khách hàng, SĐT..."
            class="pos-form-input"
          />
        </div>

        <div class="sidebar-tabs">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'active' }"
            @click="activeTab = 'active'"
          >
            Đang hoạt động
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'history' }"
            @click="activeTab = 'history'"
          >
            Lịch sử chat
          </button>
        </div>

        <div class="chat-list">
          <div
            v-for="chat in filteredChats"
            :key="chat.id"
            class="chat-item"
            :class="{ active: selectedChat?.id === chat.id, need: chat.status === 'need' }"
            @click="selectChat(chat)"
          >
            <div class="chat-avatar">👤</div>
            <div class="chat-meta">
              <div class="chat-top">
                <strong>{{ chat.name }}</strong>
                <small>{{ chat.time }}</small>
              </div>
              <div class="chat-phone">{{ chat.phone || 'Chưa cung cấp SĐT' }}</div>
              <div v-if="chat.status === 'need'" class="status-tag need">
                <span class="dot"></span> Cần hỗ trợ
              </div>
            </div>
          </div>

          <div v-if="filteredChats.length === 0" class="empty-list">
            Không có cuộc trò chuyện nào
          </div>
        </div>
      </aside>

      <!-- RIGHT: Khung chat -->
      <main class="pos-panel-card chat-panel" v-if="selectedChat">
        <!-- Chat header -->
        <div class="chat-header">
          <div class="chat-header-left">
            <div class="chat-avatar large">👤</div>
            <div>
              <strong class="chat-name">{{ selectedChat.name }}</strong>
              <small class="start-time">Bắt đầu lúc: {{ selectedChat.startTime }}</small>
            </div>
          </div>
          <button class="btn-close-chat" @click="closeChat">
            ✕ Đóng phiên chat
          </button>
        </div>

        <!-- Messages -->
        <div class="chat-messages" ref="messagesRef">
          <div
            v-for="(msg, idx) in selectedChat.messages"
            :key="idx"
            class="message-row"
            :class="msg.role"
          >
            <div class="message-meta">
              <span class="sender">
                {{ msg.role === 'customer' ? 'Khách hàng' : msg.role === 'ai' ? 'Trợ lý AI' : 'Bạn' }}
              </span>
              <span class="time">{{ msg.time }}</span>
            </div>
            <div class="message-bubble" :class="msg.role">
              {{ msg.text }}
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="chat-input-area">
          <input
            v-model="newMessage"
            type="text"
            placeholder="Nhập tin nhắn trả lời khách hàng..."
            class="pos-form-input chat-input"
            @keyup.enter="sendMessage"
          />
          <button
            class="btn-send"
            :disabled="!newMessage.trim()"
            @click="sendMessage"
          >
            Gửi
          </button>
        </div>
      </main>

      <!-- Empty state -->
      <main class="pos-panel-card chat-panel empty-state" v-else>
        <div class="empty-content">
          <div class="empty-icon">💬</div>
          <h3>Chọn một cuộc trò chuyện</h3>
          <p>Chọn khách hàng bên trái để bắt đầu hỗ trợ</p>
        </div>
      </main>
    </div>

    <!-- Chat AI -->
    <button
      class="ai-float-btn"
      :class="{ open: showAiChat }"
      @click="toggleAiChat"
      title="Chat với Trợ lý AI"
    >
      <span v-if="!showAiChat">🤖</span>
      <span v-else>✕</span>
    </button>

    <transition name="ai-slide">
      <div v-if="showAiChat" class="ai-chat-widget">
        <div class="ai-widget-header">
          <div class="ai-header-left">
            <span class="ai-avatar">🤖</span>
            <div>
              <strong>Trợ lý AI</strong>
              <small>Luôn sẵn sàng hỗ trợ bạn</small>
            </div>
          </div>
          <button class="ai-close-btn" @click="showAiChat = false">✕</button>
        </div>

        <div class="ai-messages" ref="aiMessagesRef">
          <div
            v-for="(msg, idx) in aiMessages"
            :key="idx"
            class="ai-msg"
            :class="msg.role"
          >
            <div class="ai-bubble">{{ msg.text }}</div>
          </div>
        </div>

        <div class="ai-input-area">
          <input
            v-model="aiInput"
            type="text"
            placeholder="Hỏi AI bất cứ điều gì..."
            class="pos-form-input"
            @keyup.enter="sendAiMessage"
          />
          <button
            class="btn-ai-send"
            :disabled="!aiInput.trim() || aiLoading"
            @click="sendAiMessage"
          >
            {{ aiLoading ? '...' : '➤' }}
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

/* ==========================================================
   TOAST
========================================================== */
const toast = ref({ show: false, message: '', type: 'success' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { show: true, message, type }
  toastTimer = setTimeout(() => {
    toast.value.show = false
    toastTimer = null
  }, 2800)
}

/* ==========================================================
   CHAT HỖ TRỢ KHÁCH HÀNG
========================================================== */
const activeTab = ref('active')
const searchQuery = ref('')
const newMessage = ref('')
const messagesRef = ref(null)

const chats = ref([
  {
    id: 1,
    name: 'Khách hàng',
    phone: null,
    time: '17:29 - 16/8/2026',
    startTime: '17:29 - 16/8/2026',
    status: 'need',
    messages: [
      {
        role: 'customer',
        time: '17:29 - 16/8/2026',
        text: 'Tôi muốn gặp nhân viên trực tiếp hỗ trợ'
      },
      {
        role: 'ai',
        time: '17:29 - 16/8/2026',
        text: 'Đã gửi yêu cầu kết nối với nhân viên tư vấn. Nhân viên trực sẽ phản hồi bạn trong giây lát!'
      }
    ]
  },
  {
    id: 2,
    name: 'Nguyễn Minh Anh',
    phone: '0901 234 567',
    time: '16:45 - 16/8/2026',
    startTime: '16:40 - 16/8/2026',
    status: 'active',
    messages: [
      {
        role: 'customer',
        time: '16:42 - 16/8/2026',
        text: 'Đơn hàng của tôi đến chưa ạ?'
      },
      {
        role: 'agent',
        time: '16:44 - 16/8/2026',
        text: 'Chào bạn, đơn hàng HD0008 đang trên đường giao, dự kiến nhận trong hôm nay ạ.'
      }
    ]
  },
  {
    id: 3,
    name: 'Trần Hữu Đức',
    phone: '0912 345 678',
    time: '15:20 - 16/8/2026',
    startTime: '15:10 - 16/8/2026',
    status: 'active',
    messages: [
      {
        role: 'customer',
        time: '15:12 - 16/8/2026',
        text: 'Tôi muốn đổi size áo'
      }
    ]
  }
])

const selectedChat = ref(chats.value[0])

const filteredChats = computed(() => {
  let list = chats.value
  if (activeTab.value === 'history') {
    list = list.filter(c => c.status === 'closed')
  } else {
    list = list.filter(c => c.status === 'need' || c.status === 'active')
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(
      c =>
        c.name.toLowerCase().includes(q) ||
        (c.phone && c.phone.includes(q))
    )
  }
  return list
})

function selectChat(chat) {
  selectedChat.value = chat
}

function closeChat() {
  if (selectedChat.value) {
    selectedChat.value.status = 'closed'
    showToast(`Đã đóng phiên chat với ${selectedChat.value.name}`)
    selectedChat.value = null
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || !selectedChat.value) return

  const now = new Date()
  const timeStr =
    now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) +
    ' - ' +
    now.toLocaleDateString('vi-VN')

  selectedChat.value.messages.push({
    role: 'agent',
    time: timeStr,
    text: newMessage.value.trim()
  })

  newMessage.value = ''
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

/* ==========================================================
   FLOATING AI CHAT WIDGET
========================================================== */
const showAiChat = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiMessagesRef = ref(null)

const aiMessages = ref([
  {
    role: 'ai',
    text: 'Xin chào! Tôi là Trợ lý AI của FF T-Shirt Store. Bạn cần hỗ trợ gì về đơn hàng, sản phẩm hay chính sách không?'
  }
])

function toggleAiChat() {
  showAiChat.value = !showAiChat.value
}

async function sendAiMessage() {
  if (!aiInput.value.trim() || aiLoading.value) return

  const userText = aiInput.value.trim()
  aiMessages.value.push({ role: 'user', text: userText })
  aiInput.value = ''
  aiLoading.value = true

  await nextTick()
  if (aiMessagesRef.value) {
    aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
  }

  // Giả lập phản hồi AI
  setTimeout(() => {
    const replies = [
      'Tôi đã ghi nhận yêu cầu của bạn. Bạn có thể cung cấp thêm mã đơn hàng để tôi kiểm tra chi tiết không?',
      'Hiện tại hệ thống đang hoạt động bình thường. Bạn cần hỗ trợ về đổi trả, giao hàng hay sản phẩm nào ạ?',
      'Cảm ơn bạn đã liên hệ. Nhân viên sẽ sớm hỗ trợ nếu cần. Bạn còn câu hỏi nào khác không?',
      'Tôi có thể giúp bạn tra cứu tồn kho, trạng thái đơn hàng hoặc hướng dẫn đổi size. Bạn muốn hỏi gì?'
    ]
    const reply = replies[Math.floor(Math.random() * replies.length)]
    aiMessages.value.push({ role: 'ai', text: reply })
    aiLoading.value = false

    nextTick(() => {
      if (aiMessagesRef.value) {
        aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
      }
    })
  }, 900)
}
</script>

<style scoped>
.support-page {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  color: #3d4a50;
  box-sizing: border-box;
  position: relative;
}

/* Breadcrumb */
.page-breadcrumb {
  margin-bottom: 12px;
  font-size: 0.88rem;
}
.crumb-parent {
  color: #8a9292;
  text-decoration: none;
}
.crumb-parent:hover { color: #496883; }
.crumb-separator { margin: 0 8px; color: #c4c4c4; }
.crumb-current { color: #496883; font-weight: 600; }

/* Header */
.support-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: #496883;
  margin: 0 0 4px;
}
.page-desc {
  font-size: 0.88rem;
  color: #8a9292;
  margin: 0;
}

.support-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-status-online {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #e6f4ea;
  color: #137333;
  border: 1px solid #c8e6c9;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #43a047;
  box-shadow: 0 0 0 3px rgba(67, 160, 71, 0.25);
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  border: 1px solid #d6d0c3;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  font-size: 15px;
}
.icon-btn .badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #c94a29;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  border: 1px solid #d6d0c3;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #496883;
}
.user-chip .avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #496883;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}
.user-chip .chevron { color: #8a9292; font-size: 12px; }

.support-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 18px;
  align-items: stretch;
  min-height: calc(100vh - 200px);
}

.pos-panel-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.03);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== SIDEBAR ===== */
.sidebar-panel {
  padding: 0;
}
.sidebar-search {
  padding: 14px 14px 8px;
}
.pos-form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d6d0c3;
  border-radius: 6px;
  font-size: 0.88rem;
  outline: none;
  box-sizing: border-box;
  background: #fff;
  color: #333;
  transition: border-color 0.2s;
}
.pos-form-input:focus { border-color: #496883; }

.sidebar-tabs {
  display: flex;
  gap: 6px;
  padding: 0 14px 12px;
}
.tab-btn {
  flex: 1;
  padding: 8px 0;
  border-radius: 6px;
  border: 1px solid transparent;
  background: #f7f5ef;
  font-size: 0.83rem;
  font-weight: 600;
  color: #647074;
  cursor: pointer;
  transition: all 0.2s;
}
.tab-btn.active {
  background: #eaf1f4;
  color: #496883;
  border-color: #496883;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 12px;
}
.chat-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}
.chat-item:hover { background: #f7f5ef; }
.chat-item.active {
  background: #fef2f2;
  border-color: #fecaca;
}
.chat-item.need { background: #fff8f6; }

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f7f5ef;
  border: 1px solid #e9e5db;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.chat-avatar.large {
  width: 42px;
  height: 42px;
  font-size: 20px;
}

.chat-meta { flex: 1; min-width: 0; }
.chat-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.chat-top strong {
  font-size: 0.9rem;
  color: #333;
}
.chat-top small {
  font-size: 0.75rem;
  color: #8a9292;
  white-space: nowrap;
}
.chat-phone {
  font-size: 0.8rem;
  color: #647074;
  margin-top: 2px;
}
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}
.status-tag.need {
  background: #fee2e2;
  color: #c94a29;
}
.status-tag .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c94a29;
}
.empty-list {
  text-align: center;
  padding: 40px 16px;
  color: #8a9292;
  font-size: 0.88rem;
}

/* ===== CHAT PANEL ===== */
.chat-panel { padding: 0; }
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #e9e5db;
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.chat-name {
  display: block;
  font-size: 0.95rem;
  color: #496883;
  font-weight: 700;
}
.start-time {
  font-size: 0.78rem;
  color: #8a9292;
}
.btn-close-chat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 6px;
  border: 1px solid #fecaca;
  background: #fff;
  color: #c94a29;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-close-chat:hover { background: #fef2f2; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: #fbfaf7;
  min-height: 320px;
}
.message-row {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}
.message-row.customer { align-self: flex-start; }
.message-row.ai,
.message-row.agent { align-self: flex-start; }

.message-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 0.75rem;
}
.message-meta .sender {
  font-weight: 600;
  color: #647074;
}
.message-meta .time { color: #8a9292; }

.message-bubble {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 0.88rem;
  line-height: 1.5;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.message-bubble.customer {
  background: #fff;
  border: 1px solid #e9e5db;
  color: #333;
  border-bottom-left-radius: 3px;
}
.message-bubble.ai {
  background: #f3e8ff;
  color: #5b21b6;
  border-bottom-left-radius: 3px;
}
.message-bubble.agent {
  background: #eaf1f4;
  color: #1e3a5f;
  border-bottom-left-radius: 3px;
}

.chat-input-area {
  display: flex;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid #e9e5db;
  background: #fff;
}
.chat-input { flex: 1; }
.btn-send {
  padding: 0 18px;
  height: 38px;
  border-radius: 6px;
  border: none;
  background: #496883;
  color: #fff;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-send:hover:not(:disabled) { opacity: 0.9; }
.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Empty state */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-content {
  text-align: center;
  color: #8a9292;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-content h3 {
  font-size: 1rem;
  color: #496883;
  margin: 0 0 4px;
}
.empty-content p { font-size: 0.88rem; margin: 0; }

/* ==========================================================
   FLOATING AI CHAT WIDGET – GÓC DƯỚI BÊN PHẢI
========================================================== */
.ai-float-btn {
  position: fixed;
  bottom: 28px;
  right: 28px;          
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  background: #496883;
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.35);
  z-index: 9990;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
}
.ai-float-btn:hover {
  transform: scale(1.08);
  background: #3a5670;
}
.ai-float-btn.open {
  background: #c94a29;
}

.ai-chat-widget {
  position: fixed;
  bottom: 92px;
  right: 28px;          /* ← chuyển sang bên phải */
  width: 340px;
  max-height: 420px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e9e5db;
  box-shadow: 0 8px 28px rgba(65, 60, 50, 0.15);
  z-index: 9991;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: #496883;
  color: #fff;
}
.ai-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}
.ai-header-left strong {
  display: block;
  font-size: 0.9rem;
}
.ai-header-left small {
  font-size: 0.72rem;
  opacity: 0.85;
}
.ai-close-btn {
  background: none;
  border: none;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  opacity: 0.85;
}
.ai-close-btn:hover { opacity: 1; }

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #fbfaf7;
  min-height: 200px;
  max-height: 280px;
}
.ai-msg {
  display: flex;
  max-width: 85%;
}
.ai-msg.user { align-self: flex-end; }
.ai-msg.ai { align-self: flex-start; }

.ai-bubble {
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 0.84rem;
  line-height: 1.45;
}
.ai-msg.user .ai-bubble {
  background: #496883;
  color: #fff;
  border-bottom-right-radius: 3px;
}
.ai-msg.ai .ai-bubble {
  background: #fff;
  border: 1px solid #e9e5db;
  color: #333;
  border-bottom-left-radius: 3px;
}

.ai-input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e9e5db;
  background: #fff;
}
.ai-input-area .pos-form-input { flex: 1; }
.btn-ai-send {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  border: none;
  background: #496883;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-ai-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Animation */
.ai-slide-enter-active,
.ai-slide-leave-active {
  transition: all 0.25s ease;
}
.ai-slide-enter-from,
.ai-slide-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}

/* Toast */
.toast-popup {
  position: fixed;
  top: 24px;
  right: 28px;
  background-color: #36536b;
  color: #fff;
  padding: 10px 18px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0,0,0,0.15);
  font-size: 0.88rem;
  font-weight: 600;
  z-index: 99999;
  border: 1px solid #4a6880;
}
.toast-popup.warning {
  background-color: #d97706;
  border-color: #b45309;
}
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* Responsive */
@media (max-width: 900px) {
  .support-grid {
    grid-template-columns: 1fr;
  }
  .sidebar-panel {
    max-height: 280px;
  }
  .ai-chat-widget {
    width: calc(100vw - 40px);
    right: 20px;
    left: auto;
  }
  .ai-float-btn {
    right: 20px;
    left: auto;
    bottom: 20px;
  }
}
</style>