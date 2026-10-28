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
const searchQuery = ref('')

// Lọc bỏ bất kỳ tin mẫu cũ hoặc AI sender nếu còn sót
const displayMessages = computed(() => {
  return messages.value.filter(m => {
    if (m.nguoiGui === 'AI') return false
    if (m.noiDung && m.noiDung.includes('Xin chào quý khách! Cảm ơn bạn đã ghé thăm FF T-Shirt')) return false
    return true
  })
})

const inputReply = ref('')
const loadingList = ref(false)
const loadingChat = ref(false)
const sending = ref(false)
const chatScrollRef = ref(null)

// 2 Tabs theo chuẩn video tham khảo: 'ACTIVE' (Đang hoạt động) và 'CLOSED' (Lịch sử chat)
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
          if (found.trangThai === 0) activeTab.value = 'CLOSED'
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

// Lọc theo 2 Tab & Search Query (Đang hoạt động: 1 hoặc 2 | Lịch sử chat: 0)
const filteredConversations = computed(() => {
  let list = conversations.value
  if (activeTab.value === 'ACTIVE') {
    // Bao gồm cả Chờ tiếp nhận (2) và Đang chat (1)
    list = list.filter(c => c.trangThai === 1 || c.trangThai === 2)
  } else {
    // Lịch sử chat: Đã đóng (0)
    list = list.filter(c => c.trangThai === 0)
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(c => {
      const matchName = c.tenKhachHang && c.tenKhachHang.toLowerCase().includes(q)
      const matchPhone = c.soDienThoai && c.soDienThoai.includes(q)
      return matchName || matchPhone
    })
  }

  return list
})

const waitingCount = computed(() => {
  return conversations.value.filter(c => c.trangThai === 2).length
})

const activeCount = computed(() => {
  return conversations.value.filter(c => c.trangThai === 1 || c.trangThai === 2).length
})

const closedCount = computed(() => {
  return conversations.value.filter(c => c.trangThai === 0).length
})

// Chữ cái đầu viết tắt cho Avatar (Ví dụ: "Vũ Chí Tuấn Anh" -> "TA")
const getAvatarInitials = (name) => {
  if (!name || !name.trim()) return 'KH'
  const parts = name.trim().split(/\s+/)
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase()
  const first = parts[0].charAt(0).toUpperCase()
  const last = parts[parts.length - 1].charAt(0).toUpperCase()
  return first + last
}

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
const isColleague = (msg) => {
  if (!msg || msg.nguoiGui === 'KHACH_HANG' || msg.nguoiGui === 'HE_THONG') return false
  const myId = currentUser.value?.id
  return !!(msg.nhanVienId && myId && String(msg.nhanVienId) !== String(myId))
}

const getStaffSenderLabel = (msg) => {
  if (isColleague(msg)) {
    return msg.tenNhanVien || 'Đồng nghiệp'
  }
  return 'Bạn'
}

const sendReply = async () => {
  const text = (inputReply.value || '').trim()
  if (!text || !activeId.value || sending.value) return

  sending.value = true
  try {
    const staffName = currentUser.value?.hoVaTen || currentUser.value?.tenNhanVien || currentUser.value?.name || null
    const payload = {
      cuocHoiThoaiId: activeId.value,
      nhanVienId: currentUser.value?.id || null,
      tenNhanVien: staffName,
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
    const staffName = encodeURIComponent(currentUser.value?.hoVaTen || currentUser.value?.tenNhanVien || currentUser.value?.name || '')
    const res = await api.put(`/api/chat/cuoc-hoi-thoai/${activeId.value}/tiep-nhan?nhanVienId=${staffId}&tenNhanVien=${staffName}`)
    if (res.data) {
      activeConv.value = res.data
      activeTab.value = 'ACTIVE'
      showToast({
        title: 'Thành công!',
        message: 'Đã tiếp nhận cuộc trò chuyện.',
        type: 'success'
      })
      await selectConversation(res.data)
      loadConversations(true)
    }
  } catch (e) {
    showToast('Lỗi khi tiếp nhận cuộc trò chuyện', 'error')
  }
}

// Nhận bàn giao phụ trách phiên tư vấn từ đồng nghiệp
const takeoverConversation = async () => {
  if (!activeId.value) return
  const currentStaff = activeConv.value?.tenNhanVien || 'đồng nghiệp'
  const confirmed = await showConfirm({
    title: 'Xác nhận nhận phụ trách',
    message: `Phiên này đang do ${currentStaff} phụ trách. Bạn có chắc muốn nhận phụ trách tư vấn cho khách hàng này?`,
    confirmText: 'Nhận phụ trách',
    cancelText: 'Hủy'
  })
  if (!confirmed) return

  try {
    const staffId = currentUser.value?.id || ''
    const staffName = encodeURIComponent(currentUser.value?.hoVaTen || currentUser.value?.tenNhanVien || currentUser.value?.name || '')
    const res = await api.put(`/api/chat/cuoc-hoi-thoai/${activeId.value}/tiep-nhan?nhanVienId=${staffId}&tenNhanVien=${staffName}`)
    if (res.data) {
      activeConv.value = res.data
      showToast({
        title: 'Thành công!',
        message: 'Bạn đã nhận phụ trách phiên tư vấn này.',
        type: 'success'
      })
      await selectConversation(res.data)
      loadConversations(true)
    }
  } catch (e) {
    showToast('Lỗi nhận phụ trách cuộc trò chuyện', 'error')
  }
}

// 6. Đóng phiên trò chuyện
const closeConversation = async () => {
  if (!activeId.value) return

  const custName = activeConv.value?.tenKhachHang || 'khách hàng'
  const confirmed = await showConfirm({
    title: 'Xác nhận đóng phiên hỗ trợ',
    message: `Bạn có chắc chắn muốn đóng phiên tư vấn với ${custName}?`,
    hideIcon: true,
    confirmText: 'Đóng phiên',
    cancelText: 'Hủy bỏ'
  })
  if (!confirmed) return

  try {
    await api.put(`/api/chat/cuoc-hoi-thoai/${activeId.value}/dong`)
    showToast({
      title: 'Thành công!',
      message: 'Phiên hỗ trợ đã được đóng.',
      type: 'success'
    })
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
    <!-- Tiêu đề / Breadcrumb chuẩn như cũ của Admin -->
    <div class="page-breadcrumb">
      <router-link to="/thong-ke" class="crumb-parent">Trang Chủ</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Quản Lý Chat</span>
    </div>

    <!-- Main Workspace Card -->
    <div class="chat-workspace-card">
      <!-- CỘT TRÁI: DANH SÁCH CUỘC HỘI THOẠI (2 TABS: Đang hoạt động | Lịch sử chat) -->
      <aside class="chat-left-pane">
        <!-- Tiêu đề Quản lý Chat góc trái chuẩn màu như cũ -->
        <div class="left-header-title">
          <span class="chat-icon-badge">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="#496883">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z M8 11a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z m4 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z m4 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"/>
            </svg>
          </span>
          <span class="title-text">Quản lý Chat</span>
        </div>

        <!-- Ô Tìm kiếm khách hàng, SĐT -->
        <div class="search-box-wrap">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm khách hàng, SĐT..."
            class="search-input"
          />
          <button v-if="searchQuery" @click="searchQuery = ''" class="btn-clear-search">✕</button>
        </div>

        <!-- 2 Tabs chuẩn Video: Đang hoạt động | Lịch sử chat -->
        <div class="tabs-header-bar">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'ACTIVE' }"
            @click="switchTab('ACTIVE')"
          >
            Đang hoạt động
            <span v-if="waitingCount > 0" class="badge-waiting-count" title="Số khách đang chờ nhân viên">{{ waitingCount }}</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'CLOSED' }"
            @click="switchTab('CLOSED')"
          >
            Lịch sử chat
            <span v-if="closedCount > 0" class="badge-count">{{ closedCount }}</span>
          </button>
        </div>

        <!-- Danh sách cuộc hội thoại -->
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
            <div class="avatar-col">
              <div class="avatar-circle" :class="{ 'avatar-waiting': c.trangThai === 2 }">
                {{ getAvatarInitials(c.tenKhachHang) }}
              </div>
            </div>

            <div class="content-col">
              <div class="row-top">
                <span class="customer-name" :title="c.tenKhachHang">
                  {{ c.tenKhachHang || 'Khách trực tuyến' }}
                </span>
                <span class="msg-time">{{ formatCardTime(c.thoiGianTinNhanCuoi || c.ngayCapNhat) }}</span>
              </div>

              <div class="row-phone" v-if="c.soDienThoai">
                {{ c.soDienThoai }}
              </div>

              <div class="row-snippet" v-if="c.tinNhanCuoi">
                {{ (c.tinNhanCuoi || '').replace(/^Nhân viên\s+/i, '') }}
              </div>

              <div class="row-bottom">
                <span v-if="c.trangThai === 2" class="status-pill status-waiting">
                  Chờ hỗ trợ
                </span>
                <span v-else-if="c.trangThai === 1" class="status-pill status-active">
                  Đang chat
                </span>
                <span v-else class="status-pill status-closed">
                  Đã đóng
                </span>

                <!-- Phân biệt ai đang phụ trách cuộc trò chuyện -->
                <span
                  v-if="c.trangThai === 1 && c.tenNhanVien"
                  class="staff-handling-pill"
                  :class="{ 'is-me': c.nhanVienId && String(c.nhanVienId) === String(currentUser?.id) }"
                  :title="`Người phụ trách: ${c.tenNhanVien}`"
                >
                  👤 {{ (c.nhanVienId && String(c.nhanVienId) === String(currentUser?.id)) ? 'Bạn' : c.tenNhanVien }}
                </span>

                <span v-if="(c.soTinChuaDoc || 0) > 0" class="unread-pill">
                  {{ c.soTinChuaDoc }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- CỘT PHẢI: KHUNG TRÒ CHUYỆN CHI TIẾT -->
      <section class="chat-right-pane">
        <template v-if="activeConv">
          <!-- Top Header Right Pane: Avatar + Tên khách + SĐT + Nút thao tác (Tiếp nhận / Đóng phiên chat) -->
          <div class="chat-top-header">
            <div class="customer-header-info">
              <div class="header-avatar-circle">
                {{ getAvatarInitials(activeConv.tenKhachHang) }}
              </div>
              <div>
                <h3 class="header-name">{{ activeConv.tenKhachHang || 'Khách trực tuyến' }}</h3>
                <div class="header-sub-info">
                  <span class="header-phone">{{ activeConv.soDienThoai || 'Khách hàng trực tuyến' }}</span>
                  <span
                    v-if="activeConv.trangThai === 1 && activeConv.tenNhanVien"
                    class="assigned-staff-badge"
                    :class="{ 'is-me': activeConv.nhanVienId && String(activeConv.nhanVienId) === String(currentUser?.id) }"
                  >
                    <span class="dot-online"></span>
                    {{ (activeConv.nhanVienId && String(activeConv.nhanVienId) === String(currentUser?.id)) ? 'Bạn đang phụ trách' : `Phụ trách: ${activeConv.tenNhanVien}` }}
                  </span>
                </div>
              </div>
            </div>

            <div class="header-actions">
              <!-- Nút Tiếp nhận (khi trạng thái là Chờ hỗ trợ: 2) -->
              <button
                v-if="activeConv.trangThai === 2"
                type="button"
                class="btn-accept-action"
                @click="acceptConversation"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                <span>Tiếp nhận</span>
              </button>

              <!-- Nút Đóng phiên chat (khi trạng thái đang hoạt động: 1) -->
              <button
                v-else-if="activeConv.trangThai === 1"
                type="button"
                class="btn-close-session"
                @click="closeConversation"
                title="Đóng phiên tư vấn"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
                <span>Đóng phiên chat</span>
              </button>

              <!-- Huy hiệu Đã đóng (khi ở Lịch sử chat: 0) -->
              <span v-else class="pill-closed-header">
                Đã đóng
              </span>
            </div>
          </div>

          <!-- Message Body Stream -->
          <div class="chat-messages-stream" ref="chatScrollRef">
            <div v-if="loadingChat" class="loading-state-chat">
              <div class="spinner"></div>
              <span>Đang tải tin nhắn...</span>
            </div>

            <div
              v-for="msg in displayMessages"
              :key="msg.id"
              class="message-group-row"
              :class="{
                'from-customer': msg.nguoiGui === 'KHACH_HANG',
                'from-system': msg.nguoiGui === 'HE_THONG',
                'from-staff': msg.nguoiGui === 'NHAN_VIEN' || msg.nguoiGui === 'ADMIN',
                'from-colleague': isColleague(msg)
              }"
            >
              <!-- Thông báo hệ thống (tiếp nhận / đóng phiên / chuyển yêu cầu) -->
              <div v-if="msg.nguoiGui === 'HE_THONG'" class="system-event-notice">
                <span class="system-event-badge">{{ (msg.noiDung || '').replace(/^Nhân viên\s+/i, '') }}</span>
              </div>

              <!-- Tin nhắn trao đổi giữa Khách và Nhân viên -->
              <template v-else>
                <div class="sender-tag-label">
                  <template v-if="msg.nguoiGui === 'KHACH_HANG'">
                    {{ activeConv.tenKhachHang || 'Khách hàng' }} • {{ formatCardTime(msg.ngayTao) }}
                  </template>
                  <template v-else>
                    <span :class="{ 'colleague-sender-name': isColleague(msg) }">
                      {{ getStaffSenderLabel(msg) }} • {{ formatCardTime(msg.ngayTao) }}
                    </span>
                  </template>
                </div>

                <div class="message-bubble-box">
                  <div class="bubble-text-content">{{ msg.noiDung }}</div>
                </div>
              </template>
            </div>
          </div>

          <!-- Bottom Chat Input / Status Bar -->
          <div class="chat-input-footer">
            <!-- Thanh cảnh báo khi khách đang chờ tiếp nhận (trangThai === 2) -->
            <div v-if="activeConv.trangThai === 2" class="waiting-prompt-bar">
              <span>⚠️ Bấm <strong>Tiếp nhận</strong> để bắt đầu trả lời.</span>
            </div>

            <!-- Thanh thông báo khi đồng nghiệp đang phụ trách phiên này -->
            <div
              v-else-if="activeConv.trangThai === 1 && activeConv.nhanVienId && String(activeConv.nhanVienId) !== String(currentUser?.id)"
              class="colleague-handling-bar"
            >
              <div class="chb-left">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
                <span><strong>{{ activeConv.tenNhanVien }}</strong> đang phụ trách phiên tư vấn này.</span>
              </div>
              <button
                type="button"
                class="btn-takeover"
                @click="takeoverConversation"
                title="Bấm để nhận quyền phụ trách khách hàng này"
              >
                Nhận phụ trách
              </button>
            </div>

            <!-- Nếu phiên chat ĐÃ ĐÓNG (trangThai === 0) -->
            <div v-if="activeConv.trangThai === 0" class="closed-notice-bar">
              <span>Phiên chat đã đóng</span>
            </div>

            <!-- Khung nhập tin nhắn phản hồi khách -->
            <div v-else class="rounded-input-pill-wrapper">
              <input
                v-model="inputReply"
                type="text"
                class="chat-text-input"
                :placeholder="activeConv.trangThai === 2 ? 'Hãy tiếp nhận phiên chat trước...' : 'Nhập nội dung phản hồi khách hàng...'"
                :disabled="activeConv.trangThai === 2"
                @keydown="handleKeydown"
              />
              <button
                type="button"
                class="btn-send-airplane"
                :disabled="!inputReply.trim() || sending || activeConv.trangThai === 2"
                @click="sendReply"
                title="Gửi phản hồi"
              >
                Gửi
              </button>
            </div>
          </div>
        </template>

        <!-- Trạng thái trống khi chưa chọn cuộc trò chuyện (Match Video) -->
        <div v-else class="empty-right-pane">
          <svg class="empty-chat-icon" width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="#cbd5e1" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
          <h4 class="empty-title">Chọn một cuộc trò chuyện</h4>
          <p class="empty-desc">Các yêu cầu cần nhân viên hỗ trợ sẽ xuất hiện ở cột bên trái.</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.chat-page-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  height: calc(100vh - 84px);
  box-sizing: border-box;
}

/* Breadcrumb như cũ chuẩn Admin */
.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  padding: 2px 4px;
}

.crumb-parent {
  color: #496883;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;
}

.crumb-parent:hover {
  color: #364f66;
}

.crumb-separator {
  color: #cbd5e1;
}

.crumb-current {
  color: #1e293b;
  font-weight: 600;
}

/* Khối Workspace 2 cột */
.chat-workspace-card {
  display: flex;
  flex: 1;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  min-height: 0;
}

/* CỘT TRÁI */
.chat-left-pane {
  width: 320px;
  min-width: 290px;
  max-width: 340px;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

.left-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.chat-icon-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eff6ff;
}

.title-text {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

/* Ô tìm kiếm */
.search-box-wrap {
  position: relative;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.search-input {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 30px 7px 32px;
  font-size: 13px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #1e293b;
  outline: none;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: #496883;
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.btn-clear-search {
  position: absolute;
  right: 22px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 12px;
}

/* 2 Tabs theo phong cách trong video */
.tabs-header-bar {
  display: flex;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  padding: 4px 8px;
  gap: 4px;
}

.tab-btn {
  flex: 1;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tab-btn:hover {
  color: #1e293b;
  background: #f1f5f9;
}

.tab-btn.active {
  color: #496883;
  background: #eff6ff;
}

.badge-waiting-count {
  background: #f59e0b;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
}

.badge-count {
  background: #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 10px;
}

/* Danh sách Item Card */
.conv-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.conv-item-card {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s ease;
}

.conv-item-card:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.conv-item-card.selected {
  background: #eff6ff;
  border-color: #496883;
  box-shadow: 0 1px 3px rgba(73, 104, 131, 0.12);
}

.avatar-col {
  flex-shrink: 0;
}

.avatar-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.avatar-circle.avatar-waiting {
  background: #fef3c7;
  color: #b45309;
}

.content-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.customer-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.msg-time {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

.row-phone {
  font-size: 11px;
  color: #64748b;
}

.row-snippet {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.row-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.status-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.status-pill.status-waiting {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}

.status-pill.status-active {
  background: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.status-pill.status-closed {
  background: #f1f5f9;
  color: #64748b;
}

.unread-pill {
  background: #ef4444;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
}

/* CỘT PHẢI */
.chat-right-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  min-width: 0;
}

/* Header bên phải */
.chat-top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.customer-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.header-name {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.header-phone {
  font-size: 12px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Nút Tiếp nhận thiết kế hiện đại, chuyên nghiệp */
.btn-accept-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #10b981;
  color: #ffffff;
  border: 1px solid #059669;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(16, 185, 129, 0.25);
}

.btn-accept-action:hover {
  background: #059669;
  border-color: #047857;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}

/* Nút Đóng phiên */
.btn-close-session {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(153, 27, 27, 0.08);
}

.btn-close-session:hover {
  background: #fca5a5;
  color: #7f1d1d;
  border-color: #f87171;
  box-shadow: 0 2px 6px rgba(153, 27, 27, 0.18);
  transform: translateY(-1px);
}

.pill-closed-header {
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 12px;
}

/* Vùng hiển thị tin nhắn */
.chat-messages-stream {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f8fafc;
}

.message-group-row {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-group-row.from-customer {
  align-self: flex-start;
}

.message-group-row.from-staff {
  align-self: flex-end;
}

.message-group-row.from-system {
  align-self: center;
  max-width: 90%;
}

/* Thông báo hệ thống nhẹ nhàng ở giữa */
.system-event-notice {
  display: flex;
  justify-content: center;
  margin: 6px 0;
}

.system-event-badge {
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  padding: 4px 14px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  text-align: center;
}

.sender-tag-label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.from-staff .sender-tag-label {
  text-align: right;
}

/* Bong bóng tin nhắn */
.message-bubble-box {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.from-customer .message-bubble-box {
  background: #ffffff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 2px;
}

.from-staff .message-bubble-box {
  background: #496883;
  color: #ffffff;
  border-bottom-right-radius: 2px;
}

/* Footer / Input */
.chat-input-footer {
  padding: 14px 18px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.waiting-prompt-bar {
  background: #fefce8;
  border: 1px solid #fef08a;
  color: #854d0e;
  font-size: 13px;
  padding: 8px 14px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.waiting-prompt-bar strong {
  color: #854d0e;
  font-weight: 700;
}

.closed-notice-bar {
  text-align: center;
  padding: 10px;
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
  background: #f8fafc;
  border-radius: 8px;
}

.rounded-input-pill-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-text-input {
  flex: 1;
  padding: 10px 14px;
  font-size: 13px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
  background: #ffffff;
}

.chat-text-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);
}

.chat-text-input:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
  color: #94a3b8;
}

.btn-send-airplane {
  background: #496883;
  color: #ffffff;
  border: none;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 18px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-send-airplane:hover:not(:disabled) {
  background: #364f66;
}

.btn-send-airplane:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

/* Empty States */
.empty-right-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  padding: 40px;
  text-align: center;
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: #475569;
  margin: 12px 0 4px 0;
}

.empty-desc {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.empty-list-state,
.loading-state,
.loading-state-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 10px;
  color: #94a3b8;
  font-size: 13px;
  gap: 8px;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e2e8f0;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Phân biệt người phụ trách trong danh sách và header */
.staff-handling-pill {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
}

.staff-handling-pill.is-me {
  background: #e0f2fe;
  color: #0369a1;
  font-weight: 600;
}

.header-sub-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.assigned-staff-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 12px;
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.assigned-staff-badge.is-me {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.assigned-staff-badge .dot-online {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* Thanh thông báo khi đồng nghiệp đang phụ trách */
.colleague-handling-bar {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  color: #0369a1;
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.colleague-handling-bar .chb-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-takeover {
  background: #0284c7;
  color: #ffffff;
  border: none;
  font-size: 12px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.btn-takeover:hover {
  background: #0369a1;
}

/* Tin nhắn của đồng nghiệp hiển thị khác biệt rõ nét so với chính mình */
.colleague-sender-name {
  color: #0284c7;
  font-weight: 700;
}

.message-group-row.from-staff.from-colleague .message-bubble-box .bubble-text-content {
  background: #0284c7 !important;
  color: #ffffff !important;
}

</style>
