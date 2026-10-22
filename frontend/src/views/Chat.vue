<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import api from '../api'
import { getCurrentUser } from '../utils/auth'

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

const activeTab = ref('active')
const searchQuery = ref('')
const newMessage = ref('')
const messagesRef = ref(null)
const chats = ref([])
const selectedChat = ref(null)
const loadingChats = ref(false)
const currentUser = getCurrentUser()

const mapStatusToApi = () => (activeTab.value === 'history' ? 'history' : 'active')

const loadChats = async () => {
  loadingChats.value = true
  try {
    const { data } = await api.get('/api/chat/admin/conversations', {
      params: { status: mapStatusToApi() }
    })
    chats.value = Array.isArray(data) ? data : []
    if (selectedChat.value) {
      const refreshed = chats.value.find((c) => c.id === selectedChat.value.id)
      selectedChat.value = refreshed || null
    }
  } catch (error) {
    showToast(
      error.response?.data?.message || 'Không tải được danh sách chat.',
      'error'
    )
  } finally {
    loadingChats.value = false
  }
}

const filteredChats = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return chats.value
  return chats.value.filter(
    (c) =>
      (c.name || '').toLowerCase().includes(q) || (c.phone || '').includes(q)
  )
})

async function selectChat(chat) {
  try {
    const { data } = await api.get(`/api/chat/conversations/${chat.id}`)
    selectedChat.value = data
    await scrollMessages()
  } catch (error) {
    showToast(
      error.response?.data?.message || 'Không tải được lịch sử hội thoại.',
      'error'
    )
  }
}

async function closeChat() {
  if (!selectedChat.value) return
  try {
    await api.post(`/api/chat/conversations/${selectedChat.value.id}/close`)
    showToast(`Đã đóng phiên chat với ${selectedChat.value.name}`)
    selectedChat.value = null
    await loadChats()
  } catch (error) {
    showToast(
      error.response?.data?.message || 'Không đóng được phiên chat.',
      'error'
    )
  }
}

async function sendMessage() {
  const text = newMessage.value.trim()
  if (!text || !selectedChat.value) return
  try {
    const params = currentUser?.id ? { employeeId: currentUser.id } : {}
    const { data } = await api.post(
      `/api/chat/conversations/${selectedChat.value.id}/messages`,
      { text },
      { params }
    )
    selectedChat.value = data
    newMessage.value = ''
    await scrollMessages()
    await loadChats()
  } catch (error) {
    showToast(
      error.response?.data?.message || 'Không gửi được tin nhắn.',
      'error'
    )
  }
}

async function scrollMessages() {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

watch(activeTab, async () => {
  selectedChat.value = null
  await loadChats()
})

let chatSocket = null
let chatSocketRetry = null
let chatSocketStopped = false

function connectChatSocket() {
  if (chatSocketStopped) return
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  chatSocket = new WebSocket(
    `${protocol}//${window.location.hostname}:8080/ws/chat`
  )
  chatSocket.onmessage = async (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (payload.type === 'conversation-updated') {
        await loadChats()
        if (selectedChat.value?.id === payload.conversationId) {
          await selectChat({ id: payload.conversationId })
        }
      }
    } catch (e) {}
  }
  chatSocket.onclose = () => {
    if (!chatSocketStopped) {
      chatSocketRetry = setTimeout(connectChatSocket, 2500)
    }
  }
}

onMounted(() => {
  loadChats()
  connectChatSocket()
})

onBeforeUnmount(() => {
  chatSocketStopped = true
  clearTimeout(chatSocketRetry)
  chatSocket?.close()
})
</script>

<template>
  <div class="pos-page-wrapper support-page">
    <div class="support-header">
      <div>
        <h1 class="page-title">Hỗ trợ khách hàng trực tuyến</h1>
        <p class="page-desc">
          Quản lý và trả lời chat khách hàng theo thời gian thực.
        </p>
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

    <div class="support-grid">
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
            :class="{
              active: selectedChat?.id === chat.id,
              need: chat.status === 'need'
            }"
            @click="selectChat(chat)"
          >
            <div class="chat-avatar">👤</div>
            <div class="chat-meta">
              <div class="chat-top">
                <strong>{{ chat.name }}</strong>
                <small>{{ chat.time }}</small>
              </div>
              <div class="chat-phone">
                {{ chat.phone || 'Chưa cung cấp SĐT' }}
              </div>
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

      <main class="pos-panel-card chat-panel" v-if="selectedChat">
        <div class="chat-header">
          <div class="chat-header-left">
            <div class="chat-avatar large">👤</div>
            <div>
              <strong class="chat-name">{{ selectedChat.name }}</strong>
              <small class="start-time"
                >Bắt đầu lúc: {{ selectedChat.startTime }}</small
              >
            </div>
          </div>
          <button class="btn-close-chat" @click="closeChat">
            ✕ Đóng phiên chat
          </button>
        </div>
        <div class="chat-messages" ref="messagesRef">
          <div
            v-for="(msg, idx) in selectedChat.messages"
            :key="idx"
            class="message-row"
            :class="msg.role"
          >
            <div class="message-meta">
              <span class="sender">
                {{
                  msg.role === 'customer'
                    ? 'Khách hàng'
                    : msg.role === 'ai'
                      ? 'Trợ lý AI'
                      : 'Bạn'
                }}
              </span>
              <span class="time">{{ msg.time }}</span>
            </div>
            <div class="message-bubble" :class="msg.role">
              {{ msg.text }}
            </div>
          </div>
        </div>
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

      <main class="pos-panel-card chat-panel empty-state" v-else>
        <div class="empty-content">
          <div class="empty-icon">💬</div>
          <h3>Chọn một cuộc trò chuyện</h3>
          <p>Chọn khách hàng bên trái để bắt đầu hỗ trợ</p>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.support-page {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #3d4a50;
  box-sizing: border-box;
  position: relative;
}

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
}
.pos-form-input:focus {
  border-color: #496883;
}
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
  margin-bottom: 4px;
  border: 1px solid transparent;
}
.chat-item:hover {
  background: #f7f5ef;
}
.chat-item.active {
  background: #fef2f2;
  border-color: #fecaca;
}
.chat-item.need {
  background: #fff8f6;
}
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
.chat-meta {
  flex: 1;
  min-width: 0;
}
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

.chat-panel {
  padding: 0;
}
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
  padding: 7px 12px;
  border-radius: 6px;
  border: 1px solid #fecaca;
  background: #fff;
  color: #c94a29;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}
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
.message-row.customer {
  align-self: flex-start;
}
.message-row.ai,
.message-row.agent {
  align-self: flex-start;
}
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
.message-meta .time {
  color: #8a9292;
}
.message-bubble {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 0.88rem;
  line-height: 1.5;
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
.chat-input {
  flex: 1;
}
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
}
.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-content {
  text-align: center;
  color: #8a9292;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-content h3 {
  font-size: 1rem;
  color: #496883;
  margin: 0 0 4px;
}
.empty-content p {
  font-size: 0.88rem;
  margin: 0;
}

@media (max-width: 900px) {
  .support-grid {
    grid-template-columns: 1fr;
  }
  .sidebar-panel {
    max-height: 280px;
  }
}
</style>