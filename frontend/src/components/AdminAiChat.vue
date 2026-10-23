<script setup>
import { ref, nextTick } from 'vue'
import api from '@/api'

const showAiChat = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiMessagesRef = ref(null)

const aiMessages = ref([
  {
    role: 'ai',
    text: 'Xin chào Quản trị viên! Tôi là trợ lý ảo hỗ trợ quản lý SportShoe. Tôi có thể giúp bạn thống kê doanh thu, kiểm tra sản phẩm hết hàng hoặc tìm kiếm đơn nhanh chóng.'
  }
])

const aiSuggestions = [
  { icon: '📈', label: 'Doanh thu hôm nay', action: 'revenue' },
  { icon: '📦', label: 'Sản phẩm sắp hết hàng', action: 'low-stock' },
  { icon: '🔥', label: 'Sản phẩm bán chạy', action: 'best-sellers' },
  { icon: '⭐', label: 'Đánh giá tốt nhất', action: 'top-reviews' }
]

const bestSellingProducts = [
  { id: 1, name: 'Áo thun Basic Cotton 100%', price: 199000, originalPrice: 249000, sold: 1250, image: 'https://via.placeholder.com/80x80?text=Basic', badge: '#1' },
  { id: 2, name: 'Áo thun Oversize Premium', price: 279000, originalPrice: 349000, sold: 980, image: 'https://via.placeholder.com/80x80?text=Oversize', badge: '#2' },
  { id: 3, name: 'Áo polo thể thao Dry-Fit', price: 329000, originalPrice: null, sold: 845, image: 'https://via.placeholder.com/80x80?text=Polo', badge: '#3' },
  { id: 4, name: 'Áo thun Graphic Limited', price: 249000, originalPrice: 299000, sold: 720, image: 'https://via.placeholder.com/80x80?text=Graphic', badge: '#4' },
  { id: 5, name: 'Áo thun cổ tròn Soft Touch', price: 189000, originalPrice: 229000, sold: 690, image: 'https://via.placeholder.com/80x80?text=Soft', badge: '#5' }
]

function formatPrice(price) {
  if (price == null || price === '') return '—'
  return new Intl.NumberFormat('vi-VN').format(Number(price)) + '₫'
}

function toggleAiChat() {
  showAiChat.value = !showAiChat.value
}

function resetAiChat() {
  aiMessages.value = [
    {
      role: 'ai',
      text: 'Xin chào Quản trị viên! Tôi là trợ lý ảo hỗ trợ quản lý SportShoe. Tôi có thể giúp bạn thống kê doanh thu, kiểm tra sản phẩm hết hàng hoặc tìm kiếm đơn nhanh chóng.'
    }
  ]
  aiInput.value = ''
}

async function scrollAiMessages() {
  await nextTick()
  if (aiMessagesRef.value) {
    aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
  }
}

async function handleSuggestion(action) {
  if (action === 'best-sellers') return showBestSellers()
  if (action === 'revenue') return showRevenue()
  if (action === 'low-stock') return showLowStock()
  if (action === 'top-reviews') return showReviews()
}

async function showRevenue() {
  aiMessages.value.push({ role: 'user', text: 'Doanh thu hôm nay' })
  await scrollAiMessages()
  try {
    const { data } = await api.get('/api/chat/insights/revenue-today')
    const total = formatPrice(data.total || 0)
    aiMessages.value.push({
      role: 'ai',
      text: `📊 **${data.label || 'Doanh thu hôm nay'}**: ${total}\n\nXem chi tiết tại trang Thống kê: ${data.statsUrl || '/thong-ke'}`
    })
  } catch {
    aiMessages.value.push({
      role: 'ai',
      text: 'Không lấy được doanh thu (API chưa sẵn sàng). Mở trang **Thống kê** (/thong-ke) để xem chi tiết.'
    })
  }
  await scrollAiMessages()
}

async function showLowStock() {
  aiMessages.value.push({ role: 'user', text: 'Sản phẩm sắp hết hàng' })
  await scrollAiMessages()
  try {
    const { data } = await api.get('/api/chat/insights/low-stock')
    if (!data?.length) {
      aiMessages.value.push({ role: 'ai', text: 'Không có sản phẩm nào tồn kho dưới 10.' })
    } else {
      aiMessages.value.push({
        role: 'ai',
        text: `📦 Có **${data.length}** sản phẩm tồn kho dưới 10:`,
        products: data.map((p) => ({
          id: p.id,
          name: p.name,
          price: p.price,
          badge: `Còn ${p.stock}`,
          sold: p.stock,
          image: p.image || 'https://via.placeholder.com/80x80?text=SP'
        }))
      })
    }
  } catch {
    aiMessages.value.push({
      role: 'ai',
      text: 'Không tải được danh sách tồn kho thấp (API chưa sẵn sàng).'
    })
  }
  await scrollAiMessages()
}

/** Chỉ khai báo 1 lần */
async function showBestSellers() {
  aiMessages.value.push({ role: 'user', text: 'Sản phẩm bán chạy' })
  await scrollAiMessages()
  try {
    const { data } = await api.get('/api/chat/insights/best-sellers')
    const list = (data?.length ? data : bestSellingProducts).map((p, i) => ({
      id: p.id || i + 1,
      name: p.name,
      price: p.price,
      originalPrice: p.originalPrice,
      badge: p.badge || `#${i + 1}`,
      sold: p.sold,
      image: p.image || 'https://via.placeholder.com/80x80?text=SP'
    }))
    aiMessages.value.push({
      role: 'ai',
      text: '🔥 **5 sản phẩm bán chạy nhất**:',
      products: list
    })
  } catch {
    aiMessages.value.push({
      role: 'ai',
      text: '🔥 **5 sản phẩm bán chạy nhất** (dữ liệu mẫu):',
      products: bestSellingProducts
    })
  }
  await scrollAiMessages()
}

async function showReviews() {
  aiMessages.value.push({ role: 'user', text: 'Đánh giá tốt nhất' })
  await scrollAiMessages()
  aiMessages.value.push({
    role: 'ai',
    text: '⭐ Hiện tại hệ thống **chưa có đánh giá** nào từ khách hàng. Khi khách đánh giá sản phẩm, thống kê sẽ hiển thị tại đây.'
  })
  await scrollAiMessages()
}

async function sendAiMessage() {
  const question = aiInput.value.trim()
  if (!question || aiLoading.value) return

  const lower = question.toLowerCase()
  if (lower.includes('bán chạy') || lower.includes('best seller')) {
    aiInput.value = ''
    await showBestSellers()
    return
  }
  if (lower.includes('doanh thu') || lower.includes('revenue')) {
    aiInput.value = ''
    await showRevenue()
    return
  }
  if (lower.includes('hết hàng') || lower.includes('tồn kho') || lower.includes('low stock')) {
    aiInput.value = ''
    await showLowStock()
    return
  }
  if (lower.includes('đánh giá') || lower.includes('review')) {
    aiInput.value = ''
    await showReviews()
    return
  }

  aiMessages.value.push({ role: 'user', text: question })
  aiInput.value = ''
  aiLoading.value = true
  await scrollAiMessages()

  try {
    const { data } = await api.post('/api/chat/knowledge/ask', { question })
    aiMessages.value.push({
      role: 'ai',
      text: data.answer || 'Chưa tìm thấy thông tin phù hợp.'
    })
  } catch (error) {
    aiMessages.value.push({
      role: 'ai',
      text: error.response?.data?.message || 'Không thể tra cứu kiến thức. Vui lòng thử lại.'
    })
  } finally {
    aiLoading.value = false
    await scrollAiMessages()
  }
}
</script>

<template>
  <div class="admin-ai-root">
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
          <div class="ai-header-text">
            <strong>Trợ lý Admin AI</strong>
            <small><i class="online-dot"></i> Trực tuyến</small>
          </div>
          <div class="ai-header-actions">
            <button class="ai-icon-btn" title="Làm mới" @click="resetAiChat">↻</button>
            <button class="ai-icon-btn" title="Tắt" @click="showAiChat = false">⏻</button>
            <button class="ai-icon-btn" title="Đóng" @click="showAiChat = false">✕</button>
          </div>
        </div>

        <div class="ai-messages" ref="aiMessagesRef">
          <div
            v-for="(msg, idx) in aiMessages"
            :key="idx"
            class="ai-msg"
            :class="msg.role"
          >
            <div class="ai-bubble">
              <template v-if="!msg.products">
                <span v-html="msg.text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')"></span>
              </template>
              <div v-else class="best-sellers-block">
                <p
                  class="best-sellers-title"
                  v-html="msg.text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')"
                ></p>
                <div class="product-list">
                  <div v-for="p in msg.products" :key="p.id" class="product-card">
                    <div class="product-img-wrap">
                      <img :src="p.image" :alt="p.name" />
                      <span class="product-badge">{{ p.badge }}</span>
                    </div>
                    <div class="product-info">
                      <div class="product-name">{{ p.name }}</div>
                      <div class="product-price">
                        <span class="current">{{ formatPrice(p.price) }}</span>
                        <span v-if="p.originalPrice" class="original">{{ formatPrice(p.originalPrice) }}</span>
                      </div>
                      <div class="product-sold">Đã bán {{ p.sold }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="aiMessages.length <= 1" class="ai-suggestions-box">
            <button
              v-for="s in aiSuggestions"
              :key="s.action"
              class="ai-suggestion-chip"
              @click="handleSuggestion(s.action)"
            >
              <span class="chip-icon">{{ s.icon }}</span>
              {{ s.label }}
            </button>
          </div>
        </div>

        <div class="ai-input-area">
          <input
            v-model="aiInput"
            type="text"
            placeholder="Nhập câu hỏi hỗ trợ quản trị..."
            class="ai-input"
            @keyup.enter="sendAiMessage"
          />
          <button
            class="btn-ai-send"
            :disabled="!aiInput.trim() || aiLoading"
            @click="sendAiMessage"
          >
            {{ aiLoading ? '…' : '➤' }}
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.admin-ai-root {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 99999;
  font-family: Inter, Arial, sans-serif;
}

.ai-float-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: #496883;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(21, 75, 106, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
}
.ai-float-btn:hover {
  transform: scale(1.06);
  background: #2e3e4c;
}
.ai-float-btn.open {
  background: #333;
}

.ai-chat-widget {
  position: absolute;
  right: 0;
  bottom: 68px;
  width: 360px;
  max-height: 520px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #eceef2;
  box-shadow: 0 12px 40px rgba(24, 32, 51, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: #496883;
  color: #fff;
  flex-shrink: 0;
}
.ai-header-text strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
}
.ai-header-text small {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  opacity: 0.95;
  margin-top: 2px;
}
.online-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  background: #21d18b;
  border-radius: 50%;
}
.ai-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.ai-icon-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
}
.ai-icon-btn:hover {
  background: rgba(255, 255, 255, 0.18);
  opacity: 1;
}

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fff;
  min-height: 220px;
  max-height: 340px;
}
.ai-msg {
  display: flex;
  max-width: 92%;
}
.ai-msg.user {
  align-self: flex-end;
}
.ai-msg.ai {
  align-self: flex-start;
}
.ai-bubble {
  padding: 10px 13px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}
.ai-msg.user .ai-bubble {
  background: #496883;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-msg.ai .ai-bubble {
  background: #f4f5f7;
  color: #303643;
  border-bottom-left-radius: 4px;
}

.ai-suggestions-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
  padding: 12px;
  background: #fafbfc;
  border: 1px solid #eceef2;
  border-radius: 14px;
}
.ai-suggestion-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  color: #444b57;
  cursor: pointer;
  text-align: left;
}
.ai-suggestion-chip:hover {
  border-color: #e8a4aa;
  background: #fff8f8;
}
.chip-icon {
  font-size: 16px;
}

.ai-input-area {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-top: 1px solid #f1f2f4;
  background: #fff;
}
.ai-input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: #f5f6f8;
  border-radius: 24px;
  padding: 11px 16px;
  font-size: 13px;
}
.btn-ai-send {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: #eef1f5;
  color: #a6afbd;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-ai-send:enabled {
  background: #d5091b;
  color: #fff;
}
.btn-ai-send:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.best-sellers-block {
  width: 100%;
}
.best-sellers-title {
  margin: 0 0 10px;
  font-size: 13px;
}
.product-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.product-card {
  display: flex;
  gap: 10px;
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 10px;
  padding: 8px;
}
.product-img-wrap {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f0f0;
  flex-shrink: 0;
}
.product-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-badge {
  position: absolute;
  top: 3px;
  left: 3px;
  background: #d5091b;
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
}
.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}
.product-name {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.product-price {
  display: flex;
  gap: 6px;
  align-items: baseline;
}
.product-price .current {
  font-size: 13px;
  font-weight: 700;
  color: #d5091b;
}
.product-price .original {
  font-size: 11px;
  color: #959ba5;
  text-decoration: line-through;
}
.product-sold {
  font-size: 10px;
  color: #8b919b;
}

.ai-slide-enter-active,
.ai-slide-leave-active {
  transition: all 0.22s ease;
}
.ai-slide-enter-from,
.ai-slide-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.96);
}

@media (max-width: 480px) {
  .admin-ai-root {
    right: 12px;
    bottom: 12px;
  }
  .ai-chat-widget {
    width: calc(100vw - 24px);
  }
}
</style>