<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { currentClientUser, isClientLoggedIn } from '@/utils/taiKhoanKhachHang.js'
import ModalDangNhap from '@/components/BocCuc/ModalDangNhap.vue'

const router = useRouter()
const isOpen = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const chatBodyRef = ref(null)
const conversationId = ref(null)
const conversationStatus = ref(null) // 1: Đang hoạt động, 2: Chờ nhận (Đã chuyển yêu cầu), 0: Đã đóng, null: AI bot mode
const assignedStaffName = ref(null)
const unreadForClient = ref(0)
const showLoginModal = ref(false)
const featuredProducts = ref([])
let pollTimer = null

const CHAT_STORAGE_KEY = 'fftshirt_client_chat_conv_id'

// Tải danh sách sản phẩm mẫu để hiển thị dạng thẻ ngang (Card carousel) khi khách hỏi
const loadFeaturedProducts = async () => {
  try {
    const res = await api.get('/api/san-pham?page=0&size=6')
    const list = res.data?.content || res.data || []
    featuredProducts.value = list
  } catch (e) {
    console.warn('Lỗi tải sản phẩm chat:', e)
  }
}

// Gợi ý thao tác nhanh ở chân khung chat chuẩn video (chỉ hiện "Gặp nhân viên" khi đã đăng nhập)
const quickPromptChips = computed(() => {
  const chips = [
    { text: 'Sản phẩm bán chạy', type: 'bestseller' },
    { text: 'Sản phẩm đang giảm giá', type: 'promo' },
    { text: 'Tư vấn size', type: 'size' }
  ]
  if (isClientLoggedIn.value) {
    chips.push({ text: 'Gặp nhân viên', type: 'agent' })
  }
  return chips
})

// Danh sách các lựa chọn hành động nhanh ở tin nhắn chào mừng (chuẩn, không có đổi trả, không có chất liệu form dáng)
const welcomeActions = computed(() => {
  const acts = [
    { text: '📏 Tư vấn chọn size', type: 'size' },
    { text: '🚚 Tra cứu đơn hàng', type: 'track' },
    { text: '🔥 Xem ưu đãi hôm nay', type: 'promo' }
  ]
  if (isClientLoggedIn.value) {
    acts.push({ text: '💬 Gặp nhân viên tư vấn', type: 'agent' })
  }
  return acts
})

// Tiêu đề phụ (Subtitle) theo trạng thái chuẩn video & ảnh
const chatSubtitle = computed(() => {
  if (conversationStatus.value === 0) {
    return 'Phiên tư vấn đã kết thúc'
  }
  if (conversationStatus.value === 2) {
    return 'Đang chờ nhân viên tiếp nhận'
  }
  if (conversationStatus.value === 1) {
    return `Đang hỗ trợ bởi ${assignedStaffName.value || 'chuyên viên tư vấn'}`
  }
  return 'Tư vấn sản phẩm và mua sắm'
})

const createWelcomeMessage = () => {
  return {
    id: 'welcome-0',
    sender: 'bot',
    text: isClientLoggedIn.value
      ? `Xin chào ${currentClientUser.value?.name || 'quý khách'}! FF T-Shirt có thể hỗ trợ gì cho bạn hôm nay ạ?`
      : 'Xin chào bạn! FF T-Shirt có thể hỗ trợ tư vấn chọn size, sản phẩm hoặc ưu đãi nào cho bạn hôm nay ạ?',
    time: 'Vừa xong',
    actions: welcomeActions.value
  }
}

const messages = ref([createWelcomeMessage()])

// Phát âm thanh chuông nhẹ khi có tin nhắn từ nhân viên
const playNotificationSound = () => {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(587.33, ctx.currentTime)
    osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1)
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
    if (isClientLoggedIn.value) {
      initOrLoadConversation()
    }
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

// Nạp lại cuộc hội thoại kết nối với nhân viên (chỉ khi khách hàng đã có phiên chat dở trước đó)
const initOrLoadConversation = async () => {
  if (!isClientLoggedIn.value) return
  const savedId = localStorage.getItem(CHAT_STORAGE_KEY)
  if (!savedId) return

  try {
    const payload = {
      cuocHoiThoaiId: Number(savedId),
      khachHangId: currentClientUser.value?.id || null,
      tenKhachHang: currentClientUser.value?.name || null,
      soDienThoai: currentClientUser.value?.phone || null
    }

    const res = await api.post('/api/chat/khoi-tao', payload)
    if (res.data && res.data.id) {
      if (res.data.trangThai === 0) {
        conversationId.value = null
        conversationStatus.value = 0
        assignedStaffName.value = null
        localStorage.removeItem(CHAT_STORAGE_KEY)
        messages.value = [createWelcomeMessage()]
        return
      }
      conversationId.value = res.data.id
      conversationStatus.value = res.data.trangThai
      assignedStaffName.value = res.data.tenNhanVien || null
      localStorage.setItem(CHAT_STORAGE_KEY, String(res.data.id))
      await loadMessages()
    }
  } catch (e) {
    console.warn('Lỗi kết nối chat server:', e)
  }
}

// Xóa tiền tố "Nhân viên" nếu có ở thông báo tiếp nhận
const cleanMsgContent = (txt) => {
  if (!txt) return ''
  return txt.replace(/^Nhân viên\s+/i, '')
}

// Nạp lịch sử tin nhắn từ Backend
const loadMessages = async () => {
  if (!conversationId.value) return
  try {
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/tin-nhan`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      const isClosed = res.data.some(m => m.noiDung && (m.noiDung.includes('Phiên hỗ trợ đã kết thúc') || m.noiDung.includes('kết thúc')))

      const dbMsgs = res.data.map(m => {
        const isStaff = m.nguoiGui === 'NHAN_VIEN' || m.nguoiGui === 'ADMIN'
        const isSystem = m.nguoiGui === 'HE_THONG' || (m.noiDung && (m.noiDung.includes('Phiên hỗ trợ đã kết thúc') || m.noiDung.includes('kết thúc') || m.noiDung.includes('tiếp nhận') || m.noiDung.includes('Yêu cầu hỗ trợ trực tiếp')))

        if (isStaff && m.tenNhanVien && !isClosed) {
          assignedStaffName.value = m.tenNhanVien
          conversationStatus.value = 1
        }
        return {
          id: m.id,
          sender: isSystem ? 'system' : (m.nguoiGui === 'KHACH_HANG' ? 'user' : 'staff'),
          text: cleanMsgContent(m.noiDung),
          staffName: m.tenNhanVien || (isStaff ? 'Chuyên viên tư vấn' : null),
          time: formatMsgTime(m.ngayTao),
          isSystem
        }
      })

      const welcome = messages.value.find(m => m.id === 'welcome-0') || createWelcomeMessage()
      const waitingSysMsg = conversationStatus.value === 2
        ? [{
            id: 'sys-transfer',
            sender: 'system',
            text: 'Yêu cầu hỗ trợ trực tiếp đã được gửi. Vui lòng chờ nhân viên tiếp nhận cuộc trò chuyện.',
            isSystem: true
          }]
        : []
      messages.value = [welcome, ...waitingSysMsg, ...dbMsgs]
      scrollToBottom()

      if (isClosed) {
        conversationStatus.value = 0
        assignedStaffName.value = null
        localStorage.removeItem(CHAT_STORAGE_KEY)
      } else if (conversationId.value) {
        api.put(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/da-doc?role=KHACH_HANG`).catch(() => {})
      }
    } else {
      const welcome = messages.value.find(m => m.id === 'welcome-0') || createWelcomeMessage()
      const waitingSysMsg = conversationStatus.value === 2
        ? [{
            id: 'sys-transfer',
            sender: 'system',
            text: 'Yêu cầu hỗ trợ trực tiếp đã được gửi. Vui lòng chờ nhân viên tiếp nhận cuộc trò chuyện.',
            isSystem: true
          }]
        : []
      messages.value = [welcome, ...waitingSysMsg]
      scrollToBottom()
    }
  } catch (e) {
    console.warn('Lỗi nạp tin nhắn chat:', e)
  }
}

// Polling tin nhắn mới định kỳ khi mở chat
const pollNewMessages = async () => {
  if (!conversationId.value || !isClientLoggedIn.value) return
  try {
    // 1. Kiểm tra trực tiếp trạng thái cuộc hội thoại nếu đang chờ nhân viên tiếp nhận
    if (conversationStatus.value === 2 && conversationId.value) {
      try {
        const convRes = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}`)
        if (convRes.data && convRes.data.trangThai === 1) {
          conversationStatus.value = 1
          if (convRes.data.tenNhanVien) {
            assignedStaffName.value = convRes.data.tenNhanVien
          }
        }
      } catch (err) {}
    }

    const numericIds = messages.value
      .map(m => typeof m.id === 'number' ? m.id : 0)
      .filter(n => n > 0)
    const lastId = numericIds.length > 0 ? Math.max(...numericIds) : 0

    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/tin-nhan?lastId=${lastId}`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      let hasNewStaffMsg = false

      res.data.forEach(m => {
        if (!messages.value.some(existing => existing.id === m.id)) {
          const isStaff = m.nguoiGui === 'NHAN_VIEN' || m.nguoiGui === 'ADMIN'
          const isSystem = m.nguoiGui === 'HE_THONG' || (m.noiDung && (m.noiDung.includes('Phiên hỗ trợ đã kết thúc') || m.noiDung.includes('kết thúc') || m.noiDung.includes('tiếp nhận') || m.noiDung.includes('Yêu cầu hỗ trợ trực tiếp')))

          if (isStaff) {
            hasNewStaffMsg = true
            conversationStatus.value = 1
            if (m.tenNhanVien) assignedStaffName.value = m.tenNhanVien
          }
          if (m.noiDung && m.noiDung.includes('đã tiếp nhận')) {
            conversationStatus.value = 1
            if (m.tenNhanVien) assignedStaffName.value = m.tenNhanVien
          }
          const isClosedMsg = m.noiDung && (m.noiDung.includes('Phiên hỗ trợ đã kết thúc') || m.noiDung.includes('kết thúc'))
          if (isClosedMsg) {
            conversationStatus.value = 0
            assignedStaffName.value = null
            localStorage.removeItem(CHAT_STORAGE_KEY)
          }

          messages.value.push({
            id: m.id,
            sender: isSystem ? 'system' : (m.nguoiGui === 'KHACH_HANG' ? 'user' : 'staff'),
            text: cleanMsgContent(m.noiDung),
            staffName: m.tenNhanVien || (isStaff ? 'Chuyên viên tư vấn' : null),
            time: formatMsgTime(m.ngayTao),
            isSystem
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

// Bắt đầu phiên trò chuyện mới sau khi phiên cũ đã kết thúc (chuẩn video)
const resetToNewSession = () => {
  conversationId.value = null
  conversationStatus.value = null
  assignedStaffName.value = null
  localStorage.removeItem(CHAT_STORAGE_KEY)
  messages.value = [createWelcomeMessage()]
  inputMessage.value = ''
  scrollToBottom()
}

// Xử lý các lựa chọn hành động nhanh dạng dọc (chuẩn Ảnh 2, không có đổi trả, không có chất liệu)
const handleActionClick = (action) => {
  if (!action) return

  if (action.type === 'size') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Tư vấn chọn size',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showBotReply(
      'Dạ, bạn gửi giúp shop Chiều cao và Cân nặng (Ví dụ: 1m70 - 65kg) để shop chọn size chuẩn và vừa vặn nhất cho bạn nhé! 📏'
    )
  } else if (action.type === 'track') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Tra cứu đơn hàng',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showBotReply(
      'Dạ, bạn gửi giúp shop Mã đơn hàng (Ví dụ: HD00001) hoặc Số điện thoại đặt hàng để shop kiểm tra thông tin đơn cho bạn ngay nhé! 🚚'
    )
  } else if (action.type === 'promo') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Xem ưu đãi hôm nay',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showBotReply(
      'Dạ, hôm nay FF T-Shirt đang có nhiều chương trình ưu đãi hấp dẫn:\n• Giảm giá tới 30% cho nhiều mẫu áo polo & áo thun hot\n• Tặng voucher giảm giá khi đặt hàng online\n\nBạn tham khảo một số mẫu đang có giá tốt dưới đây nhé! 👇',
      featuredProducts.value.slice(0, 4)
    )
  } else if (action.type === 'agent') {
    if (!isClientLoggedIn.value) {
      showBotReply('Dạ, để kết nối và trò chuyện trực tiếp với nhân viên tư vấn của FF T-Shirt, bạn vui lòng đăng nhập tài khoản trước nhé! ❤️')
      showLoginModal.value = true
      return
    }
    handleStaffRequest()
  }
}

// Xử lý khi khách bấm các chip gợi ý nhanh (Sản phẩm bán chạy, Giảm giá, Tư vấn size, Gặp nhân viên)
const handleChipClick = (chip) => {
  if (!chip) return

  if (chip.type === 'bestseller') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Sản phẩm bán chạy',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showBotReply(
      'Dựa trên các đơn hàng đã hoàn thành, đây là những mẫu áo thun & polo có lượng bán tốt nhất tại FF T-Shirt. Bạn có thể chọn trực tiếp một sản phẩm để xem chi tiết:',
      featuredProducts.value.slice(0, 4)
    )
  } else if (chip.type === 'promo') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Sản phẩm đang giảm giá',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showBotReply(
      'Đây là các sản phẩm đang có giá ưu đãi nổi bật tại FF T-Shirt. Giá gốc, giá hiện tại và mức giảm đều được lấy từ chương trình đang có hiệu lực:',
      featuredProducts.value.slice(0, 4)
    )
  } else if (chip.type === 'size') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Tư vấn size',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showBotReply(
      'Dạ, bạn gửi giúp shop Chiều cao và Cân nặng (Ví dụ: 1m70 - 65kg) để shop chọn size chuẩn và vừa vặn nhất cho bạn nhé! 📏'
    )
  } else if (chip.type === 'agent') {
    if (!isClientLoggedIn.value) {
      showBotReply('Dạ, để kết nối và trò chuyện trực tiếp với nhân viên tư vấn của FF T-Shirt, bạn vui lòng đăng nhập tài khoản trước nhé! ❤️')
      showLoginModal.value = true
      return
    }
    handleStaffRequest()
  }
}

// Điều hướng tới trang chi tiết sản phẩm hoặc cửa hàng khi click vào thẻ sản phẩm
const viewProductDetail = (p) => {
  isOpen.value = false
  if (p && p.id) {
    router.push(`/san-pham/${p.id}`)
  } else {
    router.push('/cua-hang')
  }
}

// Gửi tin nhắn thực tế tới nhân viên qua API Backend
const sendMessageText = async (txt) => {
  const tempId = Date.now()
  messages.value.push({
    id: tempId,
    sender: 'user',
    text: txt,
    time: 'Vừa xong'
  })
  scrollToBottom()

  if (!isClientLoggedIn.value) return

  try {
    if (conversationStatus.value === 0 || !conversationId.value) {
      conversationId.value = null
      conversationStatus.value = null
      assignedStaffName.value = null
      localStorage.removeItem(CHAT_STORAGE_KEY)
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
      const match = messages.value.find(m => m.id === tempId)
      if (match) match.id = res.data.id
    }
  } catch (e) {
    console.warn('Lỗi gửi tin nhắn tới server:', e)
  }
}

const showBotReply = (replyText, products = null) => {
  isTyping.value = true
  setTimeout(() => {
    isTyping.value = false
    messages.value.push({
      id: 'bot-' + Date.now(),
      sender: 'bot',
      text: replyText,
      products: products || null,
      time: 'Vừa xong'
    })
    scrollToBottom()
  }, 350)
}

// BỘ NÃO TƯ VẤN THÔNG MINH CỦA FF T-SHIRT
const generateSmartAiReply = (rawTxt) => {
  const txt = rawTxt.trim()
  const lower = txt.toLowerCase()

  // 1. Chào hỏi
  if (/^(xin\s*chào|chào|chào\s*(bạn|shop|ad|admin|em|anh|chị)|hello|hi|hey|alo|ê|ad\s*ơi|shop\s*ơi|bạn\s*ơi|cho\s*mình\s*hỏi|mình\s*cần\s*hỏi|có\s*ai|hé\s*lô)/i.test(lower) ||
      ['chào', 'xin chào', 'hi', 'hello', 'alo', 'shop ơi', 'ad ơi', 'có ai không', 'bạn ơi', 'hé lô', 'ad'].includes(lower)) {
    return {
      text: 'Dạ, FF T-Shirt xin chào bạn! Rất vui được hỗ trợ bạn hôm nay. Bạn đang cần tư vấn về chọn size áo, mẫu áo hot hay chương trình ưu đãi nào ạ? 😊'
    }
  }

  // 2. Tính size áo thông minh dựa trên số đo
  const weightMatch = lower.match(/(\d{2,3})\s*(?:kg|kí|cân|kilo)/i) || lower.match(/(?:nặng|cân\s*nặng)\s*(\d{2,3})/i)
  const heightMatch = lower.match(/(?:1m|1\.|m)(\d{2})/i) || lower.match(/(\d{3})\s*(?:cm|phân)/i) || lower.match(/(?:cao)\s*(\d{2,3})/i)
  
  if (weightMatch || heightMatch || lower.includes('size') || lower.includes('bảng size') || lower.includes('vừa không') || lower.includes('chật') || lower.includes('rộng') || lower.includes('mặc size gì')) {
    let weight = weightMatch ? parseInt(weightMatch[1]) : null
    let height = null
    if (heightMatch) {
      const hVal = parseInt(heightMatch[1])
      height = hVal < 100 ? 100 + hVal : hVal
    }

    if (weight || height) {
      let suggestedSize = 'L'
      let desc = ''
      const w = weight || (height ? (height - 105) : 65)

      if (w < 55) {
        suggestedSize = 'S'
        desc = 'ôm vừa vặn tôn dáng'
      } else if (w <= 65) {
        suggestedSize = 'M'
        desc = 'chuẩn form Regular Fit thanh lịch'
      } else if (w <= 74) {
        suggestedSize = 'L'
        desc = 'rất vừa vặn, cử động thoải mái'
      } else if (w <= 84) {
        suggestedSize = 'XL'
        desc = 'rộng rãi, đứng form'
      } else {
        suggestedSize = 'XXL'
        desc = 'form rộng thoải mái tối đa'
      }

      const sizeRank = ['S', 'M', 'L', 'XL', 'XXL']
      const curIdx = sizeRank.indexOf(suggestedSize)
      const oversize = curIdx < sizeRank.length - 1 ? sizeRank[curIdx + 1] : 'XXL'
      const spec = [height ? `${height}cm` : null, weight ? `${weight}kg` : null].filter(Boolean).join(' - ')

      return {
        text: `Dạ với thông số (${spec}), bạn mặc **Size ${suggestedSize}** (${desc}) là vừa đẹp nhất ạ! ✨\n\n💡 **Mẹo chọn size:**\n• Mặc vừa vặn, chỉn chu: Bạn chọn **Size ${suggestedSize}** nhé.\n• Mặc rộng rãi, thoải mái (Oversize): Bạn có thể tăng 1 size lên **Size ${oversize}** nha! ❤️`
      }
    }

    return {
      text: 'Dạ, bảng size áo chuẩn tại FF T-Shirt:\n\n• **Size S**: Dưới 55kg (chiều cao dưới 1m65)\n• **Size M**: 56 - 65kg (chiều cao 1m65 - 1m70)\n• **Size L**: 66 - 74kg (chiều cao 1m71 - 1m76)\n• **Size XL**: 75 - 84kg (chiều cao 1m77 - 1m83)\n• **Size XXL**: Trên 85kg\n\n👉 Bạn gửi giúp shop Chiều cao và Cân nặng (Ví dụ: 1m70 - 65kg) để shop chọn size chuẩn và vừa vặn nhất cho bạn nhé! 📏'
    }
  }

  // 3. Giá cả, khuyến mãi
  if (lower.includes('giá') || lower.includes('nhiêu') || lower.includes('bao tiền') || lower.includes('tiền') || lower.includes('đắt') || lower.includes('rẻ') || lower.includes('khuyến mãi') || lower.includes('ưu đãi')) {
    return {
      text: 'Dạ, các sản phẩm áo tại FF T-Shirt có mức giá cực kỳ cạnh tranh đi cùng chất lượng vải cao cấp ạ:\n\n👕 **Áo Thun T-Shirt Basic Cotton**: Chỉ từ 150.000đ - 220.000đ.\n👔 **Áo Polo Cổ Bẻ Pique Mắt Chim**: Chỉ từ 250.000đ - 350.000đ.\n\n🔥 **Ưu đãi hôm nay:** Đang giảm tới 30% cho nhiều mẫu và tặng voucher giảm giá khi đặt hàng online!'
    }
  }

  // 4. Sản phẩm, mẫu mã
  if (lower.includes('sản phẩm') || lower.includes('mẫu') || lower.includes('bán gì') || lower.includes('polo') || lower.includes('thun') || lower.includes('phông')) {
    return {
      text: 'Dạ, FF T-Shirt có các dòng áo nam chất lượng cao:\n\n✨ **Áo Polo Pique Mắt Chim**: Thoáng khí, cổ bẻ lịch sự, đứng form.\n✨ **Áo Thun T-Shirt Cotton**: Cổ tròn trẻ trung, mềm mại và thoáng mát.\n✨ **Áo Phông Graphic In Hình**: Trẻ trung, năng động.\n\nBạn có thể gửi câu hỏi hoặc chọn sản phẩm cần tư vấn chi tiết nhé!'
    }
  }

  // 5. Yêu cầu gặp trực tiếp nhân viên
  if (lower.includes('người bán') || lower.includes('nhân viên') || lower.includes('gặp nhân viên') || lower.includes('chuyên viên') || lower.includes('người thật')) {
    return { isStaffRequest: true }
  }

  // 6. Câu hỏi tổng quát khác
  return {
    text: 'Dạ, FF T-Shirt có thể hỗ trợ bạn về chọn size áo, giá cả, mẫu mã hoặc ưu đãi hôm nay. Bạn có thể gửi câu hỏi hoặc chọn "Gặp nhân viên tư vấn" để được hỗ trợ trực tiếp nhé! ❤️'
  }
}

// Gửi tin nhắn từ ô input
const handleSend = async () => {
  // Khi đang chờ nhân viên tiếp nhận (trangThai === 2), khóa không cho gửi tin nhắn
  if (conversationStatus.value === 2) return

  const txt = inputMessage.value.trim()
  if (!txt) return
  inputMessage.value = ''

  // 1. Đang chat với nhân viên (trangThai === 1)
  if (isClientLoggedIn.value && conversationStatus.value === 1) {
    sendMessageText(txt)
    return
  }

  // 2. Chat với AI Bot
  messages.value.push({
    id: Date.now(),
    sender: 'user',
    text: txt,
    time: 'Vừa xong'
  })
  scrollToBottom()

  const aiResult = generateSmartAiReply(txt)
  if (aiResult.isStaffRequest) {
    await handleStaffRequest()
    return
  }

  showBotReply(aiResult.text)
}

// Xử lý yêu cầu gặp nhân viên tư vấn: Đẩy vào Chờ nhận (trangThai = 2)
const handleStaffRequest = async () => {
  if (!isClientLoggedIn.value) {
    showBotReply('Dạ, để kết nối và trò chuyện trực tiếp với nhân viên tư vấn của FF T-Shirt, bạn vui lòng đăng nhập tài khoản trước nhé! ❤️')
    showLoginModal.value = true
    return
  }

  // Nếu đã được nhân viên tiếp nhận rồi
  if (conversationStatus.value === 1) {
    const staff = assignedStaffName.value || 'chuyên viên tư vấn'
    showBotReply(`Dạ, ${staff} đã tiếp nhận cuộc trò chuyện và đang hỗ trợ bạn. Bạn cứ nhắn nội dung bên dưới nhé! ❤️`)
    return
  }

  // Nếu đang ở hàng chờ sẵn
  if (conversationStatus.value === 2) {
    showBotReply('Dạ, yêu cầu của bạn đang trong danh sách chờ tiếp nhận. Bạn vui lòng chờ trong giây lát nhé! ❤️')
    return
  }

  // Tạo phiên mới và gửi yêu cầu hỗ trợ (trangThai = 2)
  conversationId.value = null
  conversationStatus.value = 2
  assignedStaffName.value = null
  localStorage.removeItem(CHAT_STORAGE_KEY)

  // Đẩy tin thông báo hệ thống chuẩn câu thoại mới
  messages.value.push({
    id: 'sys-' + Date.now(),
    sender: 'system',
    text: 'Yêu cầu hỗ trợ trực tiếp đã được gửi. Vui lòng chờ nhân viên tiếp nhận cuộc trò chuyện.',
    isSystem: true
  })
  scrollToBottom()

  await requestStaffSupport()
}

const requestStaffSupport = async () => {
  if (!isClientLoggedIn.value) return
  try {
    const payload = {
      khachHangId: currentClientUser.value?.id || null,
      tenKhachHang: currentClientUser.value?.name || null,
      soDienThoai: currentClientUser.value?.phone || null
    }
    const res = await api.post('/api/chat/khoi-tao', payload)
    if (res.data && res.data.id) {
      conversationId.value = res.data.id
      conversationStatus.value = res.data.trangThai || 2
      localStorage.setItem(CHAT_STORAGE_KEY, String(res.data.id))
      await api.put(`/api/chat/cuoc-hoi-thoai/${res.data.id}/yeu-cau-ho-tro`).catch(() => {})
    }
  } catch (e) {
    console.warn('Lỗi gửi yêu cầu hỗ trợ nhân viên:', e)
  }
}

// Làm mới phiên chat màn online khi đăng nhập
const refreshChatOnLogin = async () => {
  conversationId.value = null
  conversationStatus.value = null
  assignedStaffName.value = null
  localStorage.removeItem(CHAT_STORAGE_KEY)
  messages.value = [createWelcomeMessage()]
}

// Đăng nhập thành công từ Modal
const onLoginSuccess = async () => {
  showLoginModal.value = false
  await refreshChatOnLogin()
}

onMounted(() => {
  loadFeaturedProducts()
  const savedId = localStorage.getItem(CHAT_STORAGE_KEY)
  if (savedId) {
    conversationId.value = Number(savedId)
  }
  pollTimer = setInterval(pollNewMessages, 3000)
  window.addEventListener('client-auth-change', refreshChatOnLogin)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  window.removeEventListener('client-auth-change', refreshChatOnLogin)
})

watch(() => currentClientUser.value, async (newVal) => {
  if (newVal) {
    await refreshChatOnLogin()
  } else {
    resetToNewSession()
  }
})
</script>

<template>
  <div class="chat-widget-root">
    <!-- NÚT BONG BÓNG CHAT NỔI (Khi đóng: 'Tư vấn' | Khi mở: 'Đóng') - Chuẩn màu cũ #496883 -->
    <button
      class="chat-floating-btn"
      :class="{ 'is-open': isOpen }"
      @click="toggleChat"
      title="Hỗ trợ trực tuyến"
    >
      <template v-if="!isOpen">
        <!-- Icon Chat -->
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
        </svg>
        <span class="btn-text">Tư vấn</span>
        <span v-if="unreadForClient > 0" class="client-unread-badge">{{ unreadForClient }}</span>
      </template>

      <template v-else>
        <!-- Icon Đóng -->
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
        <span class="btn-text">Đóng</span>
      </template>
    </button>

    <!-- CỬA SỔ HỘP THOẠI CHAT (MÀU SẮC CHUẨN THƯƠNG HIỆU CŨ #496883) -->
    <div v-if="isOpen" class="chat-window-box">
      <!-- Chat Header -->
      <div class="chat-header">
        <div class="header-info">
          <div class="brand-title-row">
            <h4 class="brand-title">Tư vấn FF T-Shirt</h4>
            <span class="status-live-dot"></span>
          </div>
          <span class="status-subtext">{{ chatSubtitle }}</span>
        </div>
        <button class="btn-collapse" @click="isOpen = false">Thu gọn</button>
      </div>

      <!-- Chat Body -->
      <div class="chat-body" ref="chatBodyRef">
        <!-- Vòng lặp tin nhắn -->
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="[msg.sender, { 'is-system-row': msg.isSystem }]"
        >
          <!-- Thông báo hệ thống nhẹ nhàng ở giữa màn hình (chuẩn video & ảnh 3) -->
          <div v-if="msg.isSystem" class="system-event-bubble">
            {{ msg.text }}
          </div>

          <!-- Tin nhắn thông thường -->
          <template v-else>
            <!-- Tên người gửi phía trên bóng tin nhắn -->
            <div class="sender-header-tag" v-if="msg.sender !== 'user'">
              <span v-if="msg.sender === 'bot'">Tư vấn FF T-Shirt</span>
              <span v-else>{{ msg.staffName || 'Chuyên viên tư vấn' }}</span>
              <span class="dot-sep">•</span>
              <span class="tag-time">{{ msg.time }}</span>
            </div>
            <div class="sender-header-tag text-right" v-else>
              <span>Bạn</span>
              <span class="dot-sep">•</span>
              <span class="tag-time">{{ msg.time }}</span>
            </div>

            <!-- Khối nội dung tin nhắn (Chuẩn màu cũ #496883) -->
            <div class="message-bubble" :class="msg.sender">
              <p class="bubble-text">{{ msg.text }}</p>

              <!-- Thẻ hiển thị sản phẩm dạng băng chuyền ngang (Card Carousel) -->
              <div v-if="msg.products && msg.products.length > 0" class="products-carousel-wrap">
                <div v-for="p in msg.products" :key="p.id" class="product-mini-card">
                  <div class="card-img-wrap">
                    <img
                      :src="p.anhDaiDien || '/placeholder-shirt.png'"
                      :alt="p.tenSanPham"
                      class="product-thumb"
                      @error="(e) => e.target.src = 'https://placehold.co/200x200?text=FF+T-Shirt'"
                    />
                    <span v-if="p.coGiamGia || p.phanTramGiam" class="card-discount-tag">
                      -{{ p.phanTramGiam || 20 }}%
                    </span>
                  </div>
                  <div class="card-info">
                    <div class="card-brand">{{ p.tenThuongHieu || 'FF T-SHIRT' }}</div>
                    <div class="card-title" :title="p.tenSanPham">{{ p.tenSanPham }}</div>
                    <div class="card-price-row">
                      <span class="card-cur-price">{{ formatCurrency(p.minGiaSauGiam || p.minGia || 189000) }}</span>
                      <span v-if="p.coGiamGia" class="card-old-price">{{ formatCurrency(p.minGia || 250000) }}</span>
                    </div>
                    <button class="card-btn-view" @click="viewProductDetail(p)">
                      Xem thông tin sản phẩm
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Các nút hành động nhanh dạng dọc dưới tin nhắn (chuẩn Ảnh 2, không có đổi trả) -->
            <div
              v-if="msg.actions && msg.actions.length > 0 && conversationStatus !== 2 && conversationStatus !== 0"
              class="actions-vertical-list"
            >
              <button
                v-for="(act, idx) in msg.actions"
                :key="idx"
                type="button"
                class="action-item-pill"
                @click="handleActionClick(act)"
              >
                {{ act.text }}
              </button>
            </div>
          </template>
        </div>

        <!-- Trạng thái đang gõ -->
        <div v-if="isTyping" class="message-row bot">
          <div class="sender-header-tag">
            <span>Tư vấn FF T-Shirt đang trả lời...</span>
          </div>
          <div class="typing-bubble">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- Khung thông báo ĐÃ CHUYỂN YÊU CẦU chuẩn câu thoại mới -->
      <div v-if="conversationStatus === 2" class="transfer-request-bar">
        <div class="transfer-request-title">
          <span class="transfer-dot"></span>
          Đã chuyển yêu cầu hỗ trợ
        </div>
        <div class="transfer-request-desc">Hệ thống đã gửi thông báo đến nhân viên. Vui lòng chờ nhân viên tiếp nhận để bắt đầu trò chuyện.</div>
      </div>

      <!-- Khung chân chat (Input Footer hoặc Thanh Bắt đầu phiên mới) -->
      <div class="chat-footer">
        <!-- Khi phiên chat ĐÃ ĐÓNG (trangThai === 0): Hiện thông báo và nút Bắt đầu phiên mới -->
        <div v-if="conversationStatus === 0" class="closed-session-footer">
          <div class="closed-footer-text">
            <strong>Phiên tư vấn đã kết thúc</strong>
            <span>Bạn có thể bắt đầu một cuộc trò chuyện mới khi cần.</span>
          </div>
          <button class="btn-restart-session" @click="resetToNewSession">
            Bắt đầu phiên mới
          </button>
        </div>

        <!-- Khi phiên đang hoạt động hoặc chờ nhận: Form nhập tin nhắn -->
        <form v-else @submit.prevent="handleSend" class="input-form">
          <input
            v-model="inputMessage"
            type="text"
            :placeholder="conversationStatus === 2 ? 'Đang chờ nhân viên tiếp nhận cuộc trò chuyện...' : 'Nhập câu hỏi về sản phẩm, size hoặc khuyến mãi...'"
            class="chat-input"
            :disabled="conversationStatus === 2"
          />
          <button
            type="submit"
            class="btn-send"
            :disabled="!inputMessage.trim() || conversationStatus === 2"
          >
            Gửi
          </button>
        </form>
      </div>
    </div>

    <!-- Modal Đăng nhập khi khách chưa đăng nhập bấm Gặp nhân viên -->
    <ModalDangNhap
      v-if="showLoginModal"
      @close="showLoginModal = false"
      @login-success="onLoginSuccess"
    />
  </div>
</template>

<style scoped>
.chat-widget-root {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: inherit;
}

/* Nút bấm nổi Tư vấn / Đóng góc phải - Phong cách hiện đại cao cấp */
.chat-floating-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 11px 20px;
  border-radius: 28px;
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  color: #ffffff;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 24px -4px rgba(37, 99, 235, 0.4);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.chat-floating-btn:hover {
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
  transform: translateY(-2px);
  box-shadow: 0 12px 28px -4px rgba(37, 99, 235, 0.5);
}

.chat-floating-btn.is-open {
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
  box-shadow: 0 8px 20px -4px rgba(15, 23, 42, 0.3);
}

.chat-floating-btn.is-open:hover {
  background: #0f172a;
}

.client-unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid #ffffff;
}

/* Cửa sổ chat */
.chat-window-box {
  position: absolute;
  bottom: 60px;
  right: 0;
  width: 420px;
  max-width: calc(100vw - 32px);
  height: 585px;
  max-height: calc(100vh - 110px);
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 20px 48px -10px rgba(15, 23, 42, 0.18), 0 0 0 1px rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.22s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Chat Header */
.chat-header {
  padding: 14px 18px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.brand-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.status-live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.status-subtext {
  font-size: 12px;
  color: #64748b;
}

.btn-collapse {
  background: #f1f5f9;
  border: none;
  font-size: 12px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.15s;
}

.btn-collapse:hover {
  background: #e2e8f0;
  color: #0f172a;
}

/* Chat Body */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-row {
  display: flex;
  flex-direction: column;
  max-width: 88%;
}

.message-row.bot,
.message-row.staff {
  align-self: flex-start;
}

.message-row.user {
  align-self: flex-end;
}

.message-row.is-system-row {
  align-self: center;
  max-width: 95%;
}

/* Thông báo hệ thống nhẹ nhàng ở giữa màn hình */
.system-event-bubble {
  background: #eff6ff;
  color: #1e40af;
  font-size: 12px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 14px;
  border: 1px solid #dbeafe;
  text-align: center;
  line-height: 1.45;
  margin: 4px 0;
}

.sender-header-tag {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sender-header-tag.text-right {
  justify-content: flex-end;
}

.dot-sep {
  opacity: 0.5;
}

.tag-time {
  color: #94a3b8;
}

/* Bong bóng chat */
.message-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.55;
}

.message-bubble.bot {
  background: #ffffff;
  color: #0f172a;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 3px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.message-bubble.staff {
  background: #ffffff;
  color: #0f172a;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 3px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.message-bubble.user {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  border-bottom-right-radius: 3px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.22);
}

.bubble-text {
  margin: 0;
  white-space: pre-line;
}

/* Thẻ Carousel sản phẩm chuẩn video */
.products-carousel-wrap {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 10px 2px 4px 2px;
  margin-top: 8px;
}

.products-carousel-wrap::-webkit-scrollbar {
  height: 4px;
}

.products-carousel-wrap::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.product-mini-card {
  width: 175px;
  min-width: 175px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

.card-img-wrap {
  position: relative;
  width: 100%;
  height: 110px;
  background: #f1f5f9;
}

.product-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-discount-tag {
  position: absolute;
  top: 6px;
  left: 6px;
  background: #ef4444;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.card-info {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.card-brand {
  font-size: 9px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.card-title {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 2px 0 6px 0;
}

.card-cur-price {
  font-size: 12px;
  font-weight: 700;
  color: #2563eb;
}

.card-old-price {
  font-size: 10px;
  color: #94a3b8;
  text-decoration: line-through;
}

.card-btn-view {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  font-size: 11px;
  font-weight: 600;
  padding: 5px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
  margin-top: auto;
}

.card-btn-view:hover {
  background: #2563eb;
  color: #ffffff;
}

/* Khung thông báo ĐÃ CHUYỂN YÊU CẦU */
.transfer-request-bar {
  padding: 12px 18px;
  background: #f8fafc;
  border-top: 1px solid #eef2f6;
  border-left: 3px solid #2563eb;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.transfer-request-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.transfer-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.7; }
  50% { transform: scale(1.25); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.7; }
}

.transfer-request-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}

/* Danh sách các nút hành động dạng dọc */
.actions-vertical-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
  width: fit-content;
  max-width: 100%;
}

.action-item-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  text-align: left;
}

.action-item-pill:hover {
  border-color: #93c5fd;
  color: #1d4ed8;
  background: #eff6ff;
  transform: translateX(4px);
  box-shadow: 0 3px 8px rgba(37, 99, 235, 0.12);
}

/* Chat Footer */
.chat-footer {
  padding: 12px 16px;
  background: #ffffff;
  border-top: 1px solid #edf2f7;
}

.input-form {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 10px 16px;
  font-size: 13.5px;
  border: 1.5px solid #e2e8f0;
  border-radius: 22px;
  outline: none;
  background: #f8fafc;
  color: #0f172a;
  transition: all 0.2s;
}

.chat-input::placeholder {
  color: #94a3b8;
}

.chat-input:focus {
  background: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.chat-input:disabled {
  background: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
  border-color: #e2e8f0;
}

.btn-send {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  font-size: 13.5px;
  font-weight: 600;
  padding: 9px 18px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
}

.btn-send:hover:not(:disabled) {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.35);
  transform: translateY(-1px);
}

.btn-send:disabled {
  background: #a5c5f8;
  color: #ffffff;
  box-shadow: none;
  cursor: not-allowed;
  transform: none;
}

/* Khung phiên chat đã kết thúc chuẩn video */
.closed-session-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 2px;
}

.closed-footer-text {
  display: flex;
  flex-direction: column;
}

.closed-footer-text strong {
  font-size: 13px;
  color: #0f172a;
}

.closed-footer-text span {
  font-size: 11px;
  color: #64748b;
}

.btn-restart-session {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-restart-session:hover {
  background: #f8fafc;
  border-color: #94a3b8;
}

/* Typing indicator */
.typing-bubble {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  width: fit-content;
}

.typing-bubble span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2563eb;
  animation: bounce 1.2s infinite ease-in-out;
}

.typing-bubble span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-bubble span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-5px); }
}
</style>
