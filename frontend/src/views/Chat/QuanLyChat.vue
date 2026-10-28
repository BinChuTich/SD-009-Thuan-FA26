<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'
import { getCurrentUser } from '@/utils/auth.js'
import { showConfirm, showToast } from '@/utils/dialog.js'

const route = useRoute()
const router = useRouter()

const currentUser = ref(getCurrentUser())
const conversations = ref([])
const activeId = ref(null)
const activeConv = ref(null)
const messages = ref([])
const inputReply = ref('')
const loadingList = ref(false)
const loadingChat = ref(false)
const sending = ref(false)
const chatScrollRef = ref(null)

// 3 Tabs: 'ACTIVE' (Đang hoạt động: 1), 'WAITING' (Chờ nhận: 2), 'CLOSED' (Đã đóng: 0)
const activeTab = ref('ACTIVE')

let listTimer = null
let msgTimer = null

// Âm thanh chuông báo tin nhắn
const playChime = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(523.25, ctx.currentTime)
    osc.frequency.setValueAtTime(659.25, ctx.currentTime + 0.1)
    osc.frequency.setValueAtTime(783.99, ctx.currentTime + 0.2)
    gain.gain.setValueAtTime(0.08, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.45)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.45)
  } catch (e) {}
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatScrollRef.value) {
      chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
    }
  })
}

// 1. Nạp danh sách cuộc hội thoại từ backend
const loadConversations = async (silent = false) => {
  if (!silent) loadingList.value = true
  try {
    const res = await api.get('/api/chat/cuoc-hoi-thoai')
    if (Array.isArray(res.data)) {
      const prevUnread = conversations.value.reduce((acc, c) => acc + (c.soTinChuaDoc || 0), 0)
      const newUnread = res.data.reduce((acc, c) => acc + (c.soTinChuaDoc || 0), 0)
      if (newUnread > prevUnread && silent) {
        playChime()
      }

      conversations.value = res.data

      // Nếu có query param id hoặc chưa chọn, tự động chọn
      if (route.query.id && !activeId.value) {
        const found = res.data.find(c => String(c.id) === String(route.query.id))
        if (found) {
          if (found.trangThai === 2) activeTab.value = 'WAITING'
          else if (found.trangThai === 0) activeTab.value = 'CLOSED'
          else activeTab.value = 'ACTIVE'
          selectConversation(found)
        }
      } else if (activeId.value) {
        const current = res.data.find(c => c.id === activeId.value)
        if (current) activeConv.value = current
      } else if (filteredConversations.value.length > 0 && !activeId.value) {
        selectConversation(filteredConversations.value[0])
      }
    }
  } catch (e) {
    if (!silent) console.error('Lỗi nạp danh sách hội thoại:', e)
  } finally {
    if (!silent) loadingList.value = false
  }
}

// Lọc danh sách theo Tab: Đang hoạt động (1), Chờ nhận (2), Đã đóng (0)
const filteredConversations = computed(() => {
  return conversations.value.filter(c => {
    if (activeTab.value === 'ACTIVE') {
      return c.trangThai === 1
    }
    if (activeTab.value === 'WAITING') {
      return c.trangThai === 2
    }
    if (activeTab.value === 'CLOSED') {
      return c.trangThai === 0
    }
    return true
  })
})

const waitingCount = computed(() => {
  return conversations.value.filter(c => c.trangThai === 2).length
})

const activeCount = computed(() => {
  return conversations.value.filter(c => c.trangThai === 1).length
})

const closedCount = computed(() => {
  return conversations.value.filter(c => c.trangThai === 0).length
})

// 2. Chọn một cuộc hội thoại
const selectConversation = async (conv) => {
  if (!conv) return
  activeId.value = conv.id
  activeConv.value = conv
  loadingChat.value = true
  try {
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conv.id}/tin-nhan`)
    messages.value = Array.isArray(res.data) ? res.data : []
    scrollToBottom()

    // Đánh dấu đã đọc cho Admin
    if (conv.soTinChuaDoc > 0) {
      conv.soTinChuaDoc = 0
      api.put(`/api/chat/cuoc-hoi-thoai/${conv.id}/da-doc?role=ADMIN`).catch(() => {})
      window.dispatchEvent(new CustomEvent('admin-chat-read'))
    }
  } catch (e) {
    console.error('Lỗi nạp tin nhắn:', e)
  } finally {
    loadingChat.value = false
  }
}

// 3. Polling tin nhắn mới của cuộc hội thoại đang mở
const pollActiveMessages = async () => {
  if (!activeId.value) return
  try {
    const lastId = messages.value.length > 0 ? messages.value[messages.value.length - 1].id : 0
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${activeId.value}/tin-nhan?lastId=${lastId}`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      let hasCustomerMsg = false
      res.data.forEach(m => {
        if (!messages.value.some(existing => existing.id === m.id)) {
          if (m.nguoiGui === 'KHACH_HANG') hasCustomerMsg = true
          messages.value.push(m)
        }
      })
      if (hasCustomerMsg) {
        playChime()
        api.put(`/api/chat/cuoc-hoi-thoai/${activeId.value}/da-doc?role=ADMIN`).catch(() => {})
      }
      scrollToBottom()
    }
  } catch (e) {}
}

// 4. Gửi tin nhắn trả lời khách hàng (Staff)
const sendReply = async () => {
  const text = (inputReply.value || '').trim()
  if (!text || !activeId.value || sending.value) return

  sending.value = true
  try {
    const payload = {
      cuocHoiThoaiId: activeId.value,
      nhanVienId: currentUser.value?.id || null,
      nguoiGui: 'NHAN_VIEN',
      noiDung: text
    }

    const res = await api.post('/api/chat/gui-tin-nhan', payload)
    if (res.data && res.data.id) {
      messages.value.push(res.data)
      inputReply.value = ''
      scrollToBottom()

      // Tự động chuyển trạng thái active nếu đang ở chờ nhận
      if (activeConv.value) {
        activeConv.value.trangThai = 1
        activeConv.value.tinNhanCuoi = text
        activeConv.value.thoiGianTinNhanCuoi = new Date().toISOString()
      }
      loadConversations(true)
    }
  } catch (e) {
    showToast('Lỗi gửi tin nhắn!', 'error')
  } finally {
    sending.value = false
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendReply()
  }
}

// 5. Tiếp nhận cuộc trò chuyện
const acceptConversation = async () => {
  if (!activeId.value) return
  try {
    const staffId = currentUser.value?.id || ''
    const res = await api.put(`/api/chat/cuoc-hoi-thoai/${activeId.value}/tiep-nhan?nhanVienId=${staffId}`)
    if (res.data) {
      activeConv.value = res.data
      activeTab.value = 'ACTIVE'
      showToast('Đã tiếp nhận phiên tư vấn', 'success')
      await selectConversation(res.data)
      loadConversations(true)
    }
  } catch (e) {
    showToast('Lỗi khi tiếp nhận cuộc trò chuyện', 'error')
  }
}

// 6. Đóng phiên trò chuyện
const closeConversation = async () => {
  if (!activeId.value) return

  const custName = activeConv.value?.tenKhachHang || 'khách hàng'
  const confirmed = await showConfirm({
    title: 'Xác nhận đóng phiên hỗ trợ',
    message: `Bạn có chắc chắn muốn kết thúc và đóng phiên tư vấn với ${custName}?`,
    type: 'warning',
    confirmText: 'Đóng phiên',
    cancelText: 'Hủy bỏ'
  })
  if (!confirmed) return

  try {
    await api.put(`/api/chat/cuoc-hoi-thoai/${activeId.value}/dong`)
    showToast('Đã đóng phiên hỗ trợ thành công', 'success')
    if (activeConv.value) {
      activeConv.value.trangThai = 0
    }
    // Reload tin nhắn để thấy thông báo kết thúc
    if (activeId.value) {
      const res = await api.get(`/api/chat/cuoc-hoi-thoai/${activeId.value}/tin-nhan`)
      messages.value = Array.isArray(res.data) ? res.data : []
      scrollToBottom()
    }
    loadConversations(true)
  } catch (e) {
    showToast('Lỗi khi đóng phiên', 'error')
  }
}

const formatCardTime = (dtStr) => {
  if (!dtStr) return ''
  try {
    const d = new Date(dtStr)
    const hours = String(d.getHours()).padStart(2, '0')
    const mins = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${mins}`
  } catch (e) {
    return ''
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (filteredConversations.value.length > 0) {
    // Nếu activeId hiện tại không nằm trong tab mới, chọn phần tử đầu
    const exists = filteredConversations.value.some(c => c.id === activeId.value)
    if (!exists) {
      selectConversation(filteredConversations.value[0])
    }
  } else {
    activeId.value = null
    activeConv.value = null
    messages.value = []
  }
}

onMounted(() => {
  loadConversations()
  listTimer = setInterval(() => loadConversations(true), 3500)
  msgTimer = setInterval(pollActiveMessages, 2000)
})

onUnmounted(() => {
  if (listTimer) clearInterval(listTimer)
  if (msgTimer) clearInterval(msgTimer)
})

watch(() => route.query.id, (newId) => {
  if (newId && conversations.value.length > 0) {
    const found = conversations.value.find(c => String(c.id) === String(newId))
    if (found) selectConversation(found)
  }
})
</script>

<template>
  <div class="chat-page-container">
    <!-- Breadcrumb đồng bộ với Admin chung -->
    <div class="page-breadcrumb">
      <router-link to="/thong-ke" class="crumb-parent">Trang Chủ</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Quản Lý Chat</span>
    </div>

    <!-- Main Workspace Card theo chuẩn bố cục trong ảnh -->
    <div class="chat-workspace-card">
      <!-- CỘT TRÁI: DANH SÁCH CUỘC HỘI THOẠI -->
      <aside class="chat-left-pane">
        <!-- Header: 💬 Quản lý Chat -->
        <div class="left-header-title">
          <span class="chat-icon-badge">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="#496883">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z M8 11a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z m4 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z m4 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"/>
            </svg>
          </span>
          <span class="title-text">Quản lý Chat</span>
        </div>

        <!-- 3 Tabs: Đang hoạt động | Chờ nhận | Đã đóng -->
        <div class="tabs-header-bar">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'ACTIVE' }"
            @click="switchTab('ACTIVE')"
          >
            Đang hoạt động
            <span v-if="activeCount > 0" class="tab-count-pill">({{ activeCount }})</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'WAITING' }"
            @click="switchTab('WAITING')"
          >
            Chờ nhận
            <span v-if="waitingCount > 0" class="tab-count-pill alert">({{ waitingCount }})</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'CLOSED' }"
            @click="switchTab('CLOSED')"
          >
            Đã đóng
          </button>
        </div>

        <!-- Conversation Item List -->
        <div class="conv-list-container">
          <div v-if="loadingList && conversations.length === 0" class="loading-state">
            <div class="spinner"></div>
            <span>Đang tải danh sách...</span>
          </div>

          <div v-else-if="filteredConversations.length === 0" class="empty-list-state">
            <p>Không có cuộc trò chuyện nào</p>
          </div>

          <div
            v-for="c in filteredConversations"
            :key="c.id"
            class="conv-item-card"
            :class="{ selected: activeId === c.id }"
            @click="selectConversation(c)"
          >
            <!-- Top line: Name and Time -->
            <div class="card-top-row">
              <span class="customer-name" :title="c.tenKhachHang">
                {{ c.tenKhachHang || 'Khách trực tuyến' }}
              </span>
              <span class="msg-time">{{ formatCardTime(c.thoiGianTinNhanCuoi || c.ngayCapNhat) }}</span>
            </div>

            <!-- Bottom line: Status Badge -->
            <div class="card-bottom-row">
              <span v-if="c.trangThai === 1" class="status-pill status-processing">
                <span class="status-dot green">●</span> Đang xử lý
              </span>
              <span v-else-if="c.trangThai === 2" class="status-pill status-waiting">
                <span class="status-dot orange">●</span> Chờ nhận
              </span>
              <span v-else class="status-pill status-closed">
                <span class="status-dot gray">●</span> Đã đóng
              </span>

              <!-- Unread counter -->
              <span v-if="(c.soTinChuaDoc || 0) > 0" class="unread-pill">
                {{ c.soTinChuaDoc }}
              </span>
            </div>
          </div>
        </div>
      </aside>

      <!-- CỘT PHẢI: KHUNG TRÒ CHUYỆN CHI TIẾT -->
      <section class="chat-right-pane">
        <template v-if="activeConv">
          <!-- Top Header Right Pane: Customer name + ● Đang kết nối + Đóng phiên -->
          <div class="chat-top-header">
            <div class="customer-header-info">
              <h3 class="header-name">{{ activeConv.tenKhachHang || 'Khách trực tuyến' }}</h3>
              <div class="connection-status-indicator">
                <template v-if="activeConv.trangThai === 1">
                  <span class="status-dot green">●</span>
                  <span class="status-label">Đang kết nối</span>
                </template>
                <template v-else-if="activeConv.trangThai === 2">
                  <span class="status-dot orange">●</span>
                  <span class="status-label">Đang chờ tiếp nhận</span>
                </template>
                <template v-else>
                  <span class="status-dot gray">●</span>
                  <span class="status-label">Phiên đã kết thúc</span>
                </template>
              </div>
            </div>

            <div class="header-actions">
              <!-- Nút Tiếp nhận (khi trạng thái là Chờ nhận) -->
              <button
                v-if="activeConv.trangThai === 2"
                type="button"
                class="btn-accept-pill"
                @click="acceptConversation"
              >
                Tiếp nhận
              </button>

              <!-- Nút Đóng phiên theo mẫu ảnh -->
              <button
                v-if="activeConv.trangThai !== 0"
                type="button"
                class="btn-close-session"
                @click="closeConversation"
              >
                Đóng phiên
              </button>
            </div>
          </div>

          <!-- Message Body Stream -->
          <div class="chat-messages-stream" ref="chatScrollRef">
            <div v-if="loadingChat" class="loading-state-chat">
              <div class="spinner"></div>
              <span>Đang tải tin nhắn...</span>
            </div>

            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-group-row"
              :class="{
                'from-customer': msg.nguoiGui === 'KHACH_HANG',
                'from-ai': msg.nguoiGui === 'AI' || msg.nguoiGui === 'HE_THONG',
                'from-staff': msg.nguoiGui === 'NHAN_VIEN' || msg.nguoiGui === 'ADMIN'
              }"
            >
              <!-- Label tên người gửi trên bóng tin nhắn -->
              <div class="sender-tag-label">
                <template v-if="msg.nguoiGui === 'KHACH_HANG'">
                  {{ activeConv.tenKhachHang || 'Khách hàng' }}
                </template>
                <template v-else-if="msg.nguoiGui === 'AI' || msg.nguoiGui === 'HE_THONG'">
                  Giai Đài AI
                </template>
                <template v-else>
                  Bạn
                </template>
              </div>

              <!-- Nội dung tin nhắn -->
              <div class="message-bubble-box">
                <div class="bubble-text-content">{{ msg.noiDung }}</div>
              </div>
            </div>
          </div>

          <!-- Bottom Chat Input Footer -->
          <div class="chat-input-footer">
            <div class="rounded-input-pill-wrapper">
              <input
                v-model="inputReply"
                type="text"
                class="chat-text-input"
                placeholder="Nhập tin nhắn trả lời khách..."
                :disabled="activeConv.trangThai === 0"
                @keydown="handleKeydown"
              />
              <button
                type="button"
                class="btn-send-airplane"
                :disabled="!inputReply.trim() || sending || activeConv.trangThai === 0"
                @click="sendReply"
                title="Gửi tin nhắn"
              >
                <!-- SVG Paper Plane icon -->
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                </svg>
              </button>
            </div>
          </div>
        </template>

        <!-- Empty right pane -->
        <div v-else class="empty-right-pane">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="1.8">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <p>Chọn một cuộc trò chuyện từ danh sách bên trái để xem nội dung</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Trang Quản Lý Chat */
.chat-page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  height: calc(100vh - 90px);
  box-sizing: border-box;
}

/* Breadcrumb */
.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 2px;
}

.crumb-parent {
  color: #496883;
  text-decoration: none;
  transition: color 0.2s;
}

.crumb-parent:hover {
  color: #d2a764;
}

.crumb-separator {
  color: #cbd5e1;
}

.crumb-current {
  color: #1e293b;
  font-weight: 600;
}

/* Thẻ khung chat tổng (2 cột) */
.chat-workspace-card {
  display: flex;
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  min-height: 540px;
}

/* CỘT TRÁI */
.chat-left-pane {
  width: 310px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: #fafaf9;
}

.left-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px 12px;
  background: #ffffff;
}

.chat-icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
}

.title-text {
  font-size: 16px;
  font-weight: 700;
  color: #2c1810;
  font-family: inherit;
}

/* 3 Tabs: Đang hoạt động, Chờ nhận, Đã đóng */
.tabs-header-bar {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 0 12px;
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  border-bottom: 2.5px solid transparent;
  padding: 10px 4px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #2c1810;
}

.tab-btn.active {
  color: #2c1810;
  font-weight: 700;
  border-bottom-color: #d2a764; color: #496883;
}

.tab-count-pill {
  font-size: 11px;
}

.tab-count-pill.alert {
  color: #ea580c;
  font-weight: 700;
}

/* Danh sách cuộc trò chuyện */
.conv-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.loading-state,
.empty-list-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: #9ca3af;
  font-size: 13px;
  text-align: center;
}

.spinner {
  width: 22px;
  height: 22px;
  border: 2px solid #e5e7eb;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Conversation Item Card theo mẫu ảnh */
.conv-item-card {
  background: #ffffff;
  border: 1px solid #e7e5e4;
  border-radius: 6px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.18s ease;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.conv-item-card:hover {
  background: #fdfbf7;
  border-color: #d6ceb8;
}

/* Active Card theo mẫu ảnh có border vàng nâu */
.conv-item-card.selected {
  border: 2px solid #d2a764;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(210, 167, 100, 0.18);
}

.card-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.customer-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 190px;
}

.msg-time {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 400;
}

.card-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-pill {
  font-size: 12px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-pill.status-processing {
  color: #059669;
}

.status-pill.status-waiting {
  color: #d97706;
}

.status-pill.status-closed {
  color: #6b7280;
}

.status-dot {
  font-size: 10px;
  line-height: 1;
}

.status-dot.green {
  color: #10b981;
}

.status-dot.orange {
  color: #f59e0b;
}

.status-dot.gray {
  color: #9ca3af;
}

.unread-pill {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 9999px;
}

/* CỘT PHẢI */
.chat-right-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  position: relative;
}

/* Header bên phải */
.chat-top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid #f0ede6;
  background: #ffffff;
}

.customer-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-name {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.connection-status-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-accept-pill {
  background: #496883;
  color: #ffffff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 18px;
  border-radius: 9999px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-accept-pill:hover {
  background: #39536a;
}

/* Nút Đóng phiên dạng pill viền xám theo ảnh */
.btn-close-session {
  background: #ffffff;
  border: 1px solid #d1d5db;
  color: #374151;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 18px;
  border-radius: 9999px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close-session:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  color: #111827;
}

/* Luồng tin nhắn chi tiết */
.chat-messages-stream {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #ffffff;
}

.loading-state-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 13px;
  padding: 20px;
}

/* Nhóm từng tin nhắn */
.message-group-row {
  display: flex;
  flex-direction: column;
  max-width: 78%;
}

.sender-tag-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

/* Tin nhắn Khách hàng & AI ở bên trái */
.message-group-row.from-customer,
.message-group-row.from-ai {
  align-self: flex-start;
}

.message-group-row.from-customer .sender-tag-label,
.message-group-row.from-ai .sender-tag-label {
  text-align: left;
}

.message-group-row.from-customer .message-bubble-box,
.message-group-row.from-ai .message-bubble-box {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.message-group-row.from-customer .bubble-text-content {
  color: #1f2937;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
}

.message-group-row.from-ai .bubble-text-content {
  color: #374151;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-line;
  word-break: break-word;
}

/* Tin nhắn Nhân viên (Bạn) ở bên phải, màu nâu logo theo ảnh */
.message-group-row.from-staff {
  align-self: flex-end;
}

.message-group-row.from-staff .sender-tag-label {
  text-align: right;
  color: #6b7280;
}

.message-group-row.from-staff .message-bubble-box {
  background: #496883; /* Màu xanh Navy theo chữ F Logo */
  border-radius: 12px;
  padding: 10px 16px;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
}

.message-group-row.from-staff .bubble-text-content {
  color: #ffffff;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
}

/* Khung nhập tin nhắn bên dưới */
.chat-input-footer {
  padding: 16px 24px;
  background: #ffffff;
  border-top: 1px solid #f0ede6;
}

.rounded-input-pill-wrapper {
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1.8px solid #c7d2fe; /* Đường viền nhẹ có sắc xanh tím/xám như ảnh */
  border-radius: 28px;
  padding: 4px 16px;
  transition: all 0.2s;
}

.rounded-input-pill-wrapper:focus-within {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.chat-text-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 10px 4px;
  color: #1e293b;
  background: transparent;
}

.chat-text-input::placeholder {
  color: #9ca3af;
  font-size: 14px;
}

.btn-send-airplane {
  background: none;
  border: none;
  padding: 6px;
  cursor: pointer;
  color: #4b5563;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.btn-send-airplane:hover:not(:disabled) {
  color: #d2a764;
  transform: translateX(2px);
}

.btn-send-airplane:disabled {
  color: #d1d5db;
  cursor: not-allowed;
}

/* Empty State */
.empty-right-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #9ca3af;
  font-size: 14px;
}
</style>
