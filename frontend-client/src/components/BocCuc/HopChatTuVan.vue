<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'
import { currentClientUser, isClientLoggedIn } from '@/utils/taiKhoanKhachHang.js'
import ModalDangNhap from '@/components/BocCuc/ModalDangNhap.vue'

const router = useRouter()
const isOpen = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const aiTypingLabel = ref('đang kiểm tra dữ liệu shop...')
const chatBodyRef = ref(null)
const conversationId = ref(null)
const conversationStatus = ref(null) // 1: Đang hoạt động, 2: Chờ nhận (Đã chuyển yêu cầu), 0: Đã đóng, null: AI bot mode
const assignedStaffName = ref(null)
const unreadForClient = ref(0)
const showLoginModal = ref(false)
const featuredProducts = ref([])
const discountProducts = ref([])
const activeDiscountCampaigns = ref([])
const orderSearchCode = ref('')
const isSearchingOrder = ref(false)
let pollTimer = null
let inactiveTimer = null
let countdownInterval = null
const lastUserActivityTime = ref(Date.now())
const warned5Min = ref(false)
const warned10Min = ref(false)
const warned2Min = ref(false)
const showCountdownBanner = ref(false)
const remainingCountdownSec = ref(120)
const staffIsTyping = ref(false)
let clientTypingTimeout = null
let lastClientTypingPing = 0
const INACTIVITY_TIMEOUT_SEC = 60
const FIRST_WARNING_SEC = 30
const COUNTDOWN_START_SEC = 50

const onClientInput = async () => {
  updateActivity()
  if (!conversationId.value && isClientLoggedIn.value) {
    await initOrLoadConversation()
  }
  if (!conversationId.value || conversationStatus.value === 0) return
  const now = Date.now()
  if (now - lastClientTypingPing > 1200) {
    lastClientTypingPing = now
    api.post(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/dang-nhap?nguoiGui=KHACH_HANG&isTyping=true`).catch(() => {})
  }
  if (clientTypingTimeout) clearTimeout(clientTypingTimeout)
  clientTypingTimeout = setTimeout(() => {
    if (conversationId.value) {
      api.post(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/dang-nhap?nguoiGui=KHACH_HANG&isTyping=false`).catch(() => {})
    }
  }, 3500)
}

const onClientBlur = () => {
  if (!conversationId.value || conversationStatus.value === 0) return
  if (clientTypingTimeout) clearTimeout(clientTypingTimeout)
  api.post(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/dang-nhap?nguoiGui=KHACH_HANG&isTyping=false`).catch(() => {})
}

const formattedCountdown = computed(() => {
  const m = Math.floor(remainingCountdownSec.value / 60).toString().padStart(2, '0')
  const s = (remainingCountdownSec.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const startCountdown = () => {
  if (countdownInterval) clearInterval(countdownInterval)
  countdownInterval = setInterval(() => {
    const elapsedSec = Math.floor((Date.now() - lastUserActivityTime.value) / 1000)
    const left = Math.max(0, INACTIVITY_TIMEOUT_SEC - elapsedSec)
    remainingCountdownSec.value = left

    if (left <= 0) {
      clearInterval(countdownInterval)
      countdownInterval = null
      handleAutoCloseDueToInactivity()
    }
  }, 1000)
}

const dismissCountdownBanner = () => {
  showCountdownBanner.value = false
  // Lưu ý: Nút "Đã hiểu" chỉ ẩn thanh đếm ngược,
  // dòng thông báo "Bạn vẫn còn ở đó chứ?..." trong tin nhắn chat vẫn giữ nguyên!
}

const updateActivity = () => {
  lastUserActivityTime.value = Date.now()
  warned5Min.value = false
  warned10Min.value = false
  warned2Min.value = false
  showCountdownBanner.value = false
  remainingCountdownSec.value = 10
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
  // Khi khách phản hồi: tự động xóa các thông báo nhắc nhở cũ (Bạn vẫn còn ở đó chứ...)
  messages.value = messages.value.filter(m => !m.isWarning && !(m.text && m.text.includes('Bạn vẫn còn ở đó chứ')))
}

const handleAutoCloseDueToInactivity = async () => {
  if (conversationStatus.value === 0) return
  conversationStatus.value = 0
  assignedStaffName.value = null
  showCountdownBanner.value = false
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
  localStorage.removeItem(CHAT_STORAGE_KEY)

  messages.value.push({
    id: 'sys-close-' + Date.now(),
    sender: 'system',
    text: 'Do không nhận được phản hồi từ bạn trong 1 phút, phiên tư vấn đã được tự động đóng. Khi cần hỗ trợ, bạn có thể bắt đầu một phiên chat mới bất cứ lúc nào. FF T-Shirt luôn sẵn sàng hỗ trợ bạn! 💙',
    isSystem: true,
    isAutoClosed: true
  })
  scrollToBottom()

  // Backend cũng kiểm tra mốc 1 phút và lưu thông báo đóng phiên vào lịch sử.
}

const ratingScore = ref(5)
const ratingComment = ref('')
const isSubmittingRating = ref(false)
const hasSubmittedRating = ref(false)
const currentRatingInfo = ref(null)
const hoverScore = ref(0)

const resetRatingState = () => {
  hasSubmittedRating.value = false
  currentRatingInfo.value = null
  ratingScore.value = 5
  ratingComment.value = ''
  hoverScore.value = 0
}

const getRatingLabel = (score) => {
  switch (score) {
    case 1: return '1★ Rất không hài lòng'
    case 2: return '2★ Chưa hài lòng'
    case 3: return '3★ Bình thường'
    case 4: return '4★ Hài lòng'
    case 5: return '5★ Rất hài lòng'
    default: return '5★ Rất hài lòng'
  }
}

const checkExistingRating = async (convId) => {
  if (!convId) return
  try {
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${convId}/danh-gia`)
    if (res.data && res.data.id) {
      hasSubmittedRating.value = true
      currentRatingInfo.value = res.data
    } else {
      hasSubmittedRating.value = false
      currentRatingInfo.value = null
    }
  } catch (e) {
    hasSubmittedRating.value = false
  }
}

const submitRating = async () => {
  if (!conversationId.value || isSubmittingRating.value) return
  isSubmittingRating.value = true
  try {
    const payload = {
      cuocHoiThoaiId: conversationId.value,
      khachHangId: currentClientUser.value?.id || null,
      tenKhachHang: currentClientUser.value?.name || null,
      diemDanhGia: ratingScore.value,
      noiDungDanhGia: ratingComment.value.trim()
    }
    const res = await api.post(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/danh-gia`, payload)
    hasSubmittedRating.value = true
    currentRatingInfo.value = res.data
    await loadMessages()
  } catch (e) {
    console.warn('Lỗi gửi đánh giá:', e)
  } finally {
    isSubmittingRating.value = false
  }
}

const CHAT_STORAGE_KEY = 'fftshirt_client_chat_conv_id'
const STAFF_REQUESTED_STORAGE_KEY = 'fftshirt_client_staff_requested'

// Tải danh sách sản phẩm và các đợt giảm giá đang hoạt động
const loadFeaturedProducts = async () => {
  try {
    // 1. Tải bản đồ sản phẩm -> đợt giảm giá trực tiếp từ backend
    let productsMap = {}
    try {
      const mapRes = await api.get('/api/dot-giam-gia/active-products-map')
      productsMap = mapRes.data || {}
    } catch (me) {}

    // 2. Tải danh sách các đợt giảm giá đang hoạt động
    let activeCampaigns = []
    try {
      const campRes = await api.get('/api/dot-giam-gia/active')
      activeCampaigns = campRes.data || []
      activeDiscountCampaigns.value = activeCampaigns
    } catch (ce) {
      console.warn('Lỗi tải đợt giảm giá active:', ce)
    }

    // 3. Tải danh sách sản phẩm từ backend
    const res = await api.get('/api/san-pham?page=0&size=50')
    const list = res.data?.content || res.data || []
    featuredProducts.value = list

    // Lọc các sản phẩm đang có đợt giảm giá thực tế đang hoạt động
    discountProducts.value = list.filter(p => {
      const hasMap = !!productsMap[String(p.id)]
      const hasDiscountFlag = p.coGiamGia === true || (p.phanTramGiam && Number(p.phanTramGiam) > 0)
      const hasDiscountPrice = p.minGiaSauGiam && p.minGia && Number(p.minGiaSauGiam) < Number(p.minGia)
      return hasMap || hasDiscountFlag || hasDiscountPrice
    }).map(p => {
      const dgg = productsMap[String(p.id)]
      const campaignName = p.tenDotGiamGia || dgg?.tenDotGiamGia || (activeCampaigns.length > 0 ? activeCampaigns[0].tenDotGiamGia : 'Chương trình Giảm giá Đặc biệt')
      const pct = p.phanTramGiam || dgg?.phanTramGiam || (p.minGia && p.minGiaSauGiam ? Math.round(((p.minGia - p.minGiaSauGiam) / p.minGia) * 100) : 20)
      let minSauGiam = p.minGiaSauGiam
      if (!minSauGiam && p.minGia && pct) {
        minSauGiam = Math.round(p.minGia * (100 - pct) / 100)
      }
      return {
        ...p,
        coGiamGia: true,
        tenDotGiamGia: campaignName,
        phanTramGiam: pct,
        minGiaSauGiam: minSauGiam || p.minGia
      }
    })
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
  return [
    { text: '📏 Tư vấn chọn size', type: 'size' },
    { text: '🚚 Tra cứu đơn hàng', type: 'track' },
    { text: '🔥 Xem ưu đãi hôm nay', type: 'promo' }
  ]
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

// Nạp lại cuộc hội thoại kết nối với nhân viên
const initOrLoadConversation = async () => {
  if (!isClientLoggedIn.value) return
  const savedId = localStorage.getItem(CHAT_STORAGE_KEY)
  const hasRequestedStaff = localStorage.getItem(STAFF_REQUESTED_STORAGE_KEY) === 'true'

  // Chỉ khôi phục phiên mà khách đã chủ động yêu cầu hỗ trợ trước đó.
  // Không gọi API khởi tạo ở đây vì backend sẽ tạo một phiên "Chờ nhận",
  // khiến yêu cầu gặp nhân viên bị gửi ngay khi khách vừa mở trang.
  if (!savedId || !hasRequestedStaff) {
    localStorage.removeItem(CHAT_STORAGE_KEY)
    localStorage.removeItem(STAFF_REQUESTED_STORAGE_KEY)
    return
  }

  try {
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${Number(savedId)}`)
    if (res.data && res.data.id) {
      if (res.data.trangThai === 0) {
        conversationId.value = res.data.id
        conversationStatus.value = 0
        assignedStaffName.value = null
        localStorage.removeItem(CHAT_STORAGE_KEY)
        await loadMessages()
        await checkExistingRating(res.data.id)
        return
      }
      conversationId.value = res.data.id
      conversationStatus.value = res.data.trangThai
      assignedStaffName.value = res.data.tenNhanVien || null
      localStorage.setItem(CHAT_STORAGE_KEY, String(res.data.id))
      await loadMessages()
      await checkExistingRating(res.data.id)
    }
  } catch (e) {
    // ID có thể đã hết hiệu lực khi backend khởi động lại. Xóa ID cũ và
    // giữ widget ở chế độ bot; phiên nhân viên chỉ được tạo khi khách bấm nút.
    conversationId.value = null
    conversationStatus.value = null
    assignedStaffName.value = null
    localStorage.removeItem(CHAT_STORAGE_KEY)
    localStorage.removeItem(STAFF_REQUESTED_STORAGE_KEY)
    console.warn('Không thể khôi phục phiên chat cũ:', e)
  }
}

// Xóa tiền tố "Nhân viên" nếu có ở thông báo tiếp nhận
const cleanMsgContent = (txt) => {
  if (!txt) return ''
  return txt.replace(/^Nhân viên\s+/i, '')
}

// Chỉ nhận diện câu xác nhận đã đóng; không được nhầm "sắp kết thúc" là đã đóng.
const isConversationCloseContent = (text) => {
  if (!text) return false
  const normalized = String(text).toLowerCase()
  return normalized.includes('đã kết thúc') || normalized.includes('đã được tự động đóng')
}

// Nạp lịch sử tin nhắn từ Backend
const loadMessages = async () => {
  if (!conversationId.value) return
  try {
    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/tin-nhan`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      const isClosed = res.data.some(m => isConversationCloseContent(m.noiDung))

      const dbMsgs = res.data.map(m => {
        const isRating = m.nguoiGui === 'DANH_GIA' || !!m.danhGia
        const isStaff = m.nguoiGui === 'NHAN_VIEN' || m.nguoiGui === 'ADMIN'
        const isBot = m.nguoiGui === 'BOT' || m.nguoiGui === 'AI'
        const isClose = isConversationCloseContent(m.noiDung)
        const isAutoClosed = isClose && m.noiDung.includes('không nhận được phản hồi')
        const isWarning = m.noiDung && (m.noiDung.includes('Bạn vẫn còn ở đó chứ') || m.noiDung.includes('10 giây nữa để tiếp tục trò chuyện'))
        const isSystem = m.nguoiGui === 'HE_THONG' || isClose || isWarning || (m.noiDung && (m.noiDung.includes('tiếp nhận') || m.noiDung.includes('Yêu cầu hỗ trợ trực tiếp')))

        if (isRating) {
          hasSubmittedRating.value = true
          if (m.danhGia) currentRatingInfo.value = m.danhGia
        }

        if (isStaff && m.tenNhanVien && !isClosed) {
          assignedStaffName.value = m.tenNhanVien
          conversationStatus.value = 1
        }
        return {
          id: m.id,
          serverPersisted: true,
          sender: isRating ? 'rating' : (isSystem ? 'system' : (isBot ? 'bot' : (m.nguoiGui === 'KHACH_HANG' ? 'user' : 'staff'))),
          text: cleanMsgContent(m.noiDung),
          staffName: m.tenNhanVien || (isStaff ? 'Chuyên viên tư vấn' : null),
          time: formatMsgTime(m.ngayTao),
          isSystem,
          isWarning,
          isAutoClosed,
          ratingData: m.danhGia || null
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
    // 1. Kiểm tra trực tiếp trạng thái cuộc hội thoại
    if (conversationId.value) {
      try {
        const convRes = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}`)
        if (convRes.data) {
          if (convRes.data.trangThai !== undefined && convRes.data.trangThai !== null) {
            const nextStatus = convRes.data.trangThai
            if (nextStatus === 1 && conversationStatus.value !== 1) updateActivity()
            conversationStatus.value = nextStatus
            if (convRes.data.tenNhanVien) {
              assignedStaffName.value = convRes.data.tenNhanVien
            }
          }
          if (convRes.data.trangThai === 0 && conversationStatus.value !== 0) {
            conversationStatus.value = 0
            assignedStaffName.value = null
            localStorage.removeItem(CHAT_STORAGE_KEY)
          }
          if (convRes.data.danhGia) {
            hasSubmittedRating.value = true
            currentRatingInfo.value = convRes.data.danhGia
          }
          if (convRes.data.nhanVienDangNhap !== undefined) {
            staffIsTyping.value = !!convRes.data.nhanVienDangNhap
          }
        }
      } catch (err) {}
    }

    const numericIds = messages.value
        // ID tạm trên client dùng Date.now() và lớn hơn ID backend rất nhiều.
        // Chỉ dùng ID đã được server xác nhận, nếu không polling sẽ bỏ qua mọi tin mới.
        .map(m => m.serverPersisted && typeof m.id === 'number' ? m.id : 0)
        .filter(n => n > 0)
    const lastId = numericIds.length > 0 ? Math.max(...numericIds) : 0

    const res = await api.get(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/tin-nhan?lastId=${lastId}`)
    if (Array.isArray(res.data) && res.data.length > 0) {
      let hasNewStaffMsg = false

      res.data.forEach(m => {
        if (!messages.value.some(existing => existing.id === m.id)) {
          const isRating = m.nguoiGui === 'DANH_GIA' || !!m.danhGia
          const isStaff = m.nguoiGui === 'NHAN_VIEN' || m.nguoiGui === 'ADMIN'
          const isBot = m.nguoiGui === 'BOT' || m.nguoiGui === 'AI'
          const isClose = isConversationCloseContent(m.noiDung)
          const isAutoClosed = isClose && m.noiDung.includes('không nhận được phản hồi')
          const isWarning = m.noiDung && (m.noiDung.includes('Bạn vẫn còn ở đó chứ') || m.noiDung.includes('10 giây nữa để tiếp tục trò chuyện'))
          const isSystem = m.nguoiGui === 'HE_THONG' || isClose || isWarning || (m.noiDung && (m.noiDung.includes('tiếp nhận') || m.noiDung.includes('Yêu cầu hỗ trợ trực tiếp')))

          if (isRating) {
            hasSubmittedRating.value = true
            if (m.danhGia) currentRatingInfo.value = m.danhGia
          }

          if (isStaff) {
            hasNewStaffMsg = true
            conversationStatus.value = 1
            if (m.tenNhanVien) assignedStaffName.value = m.tenNhanVien
          }
          if (m.noiDung && m.noiDung.includes('đã tiếp nhận')) {
            conversationStatus.value = 1
            if (m.tenNhanVien) assignedStaffName.value = m.tenNhanVien
          }
          if (isWarning) {
            // Khi khách đã phản hồi gần đây (< 5 phút), không thêm các thông báo nhắc nhở cũ
            const elapsedSec = Math.floor((Date.now() - lastUserActivityTime.value) / 1000)
            if (elapsedSec < FIRST_WARNING_SEC) {
              return
            }
            if (!warned2Min.value && conversationStatus.value !== 0 && m.noiDung && m.noiDung.includes('10 giây nữa')) {
              warned2Min.value = true
              showCountdownBanner.value = true
              remainingCountdownSec.value = 10
              startCountdown()
            }
          }

          if (isClose) {
            messages.value = messages.value.filter(existing => !existing.isAutoClosed)
            conversationStatus.value = 0
            assignedStaffName.value = null
            showCountdownBanner.value = false
            if (countdownInterval) {
              clearInterval(countdownInterval)
              countdownInterval = null
            }
            localStorage.removeItem(CHAT_STORAGE_KEY)
          }

          messages.value.push({
            id: m.id,
            serverPersisted: true,
            sender: isRating ? 'rating' : (isSystem ? 'system' : (isBot ? 'bot' : (m.nguoiGui === 'KHACH_HANG' ? 'user' : 'staff'))),
            text: cleanMsgContent(m.noiDung),
            staffName: m.tenNhanVien || (isStaff ? 'Chuyên viên tư vấn' : null),
            time: formatMsgTime(m.ngayTao),
            isSystem,
            isWarning,
            isAutoClosed,
            ratingData: m.danhGia || null
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
  resetRatingState()
  localStorage.removeItem(CHAT_STORAGE_KEY)
  localStorage.removeItem(STAFF_REQUESTED_STORAGE_KEY)
  messages.value = [createWelcomeMessage()]
  inputMessage.value = ''
  scrollToBottom()
}

// Xử lý các lựa chọn hành động nhanh dạng dọc (chuẩn Ảnh 2, không có đổi trả, không có chất liệu)

// Mở ô nhập tra cứu đơn hàng ngay trong chat
const triggerOrderLookupPrompt = () => {
  updateActivity()
  messages.value.push({
    id: 'track-prompt-' + Date.now(),
    sender: 'bot',
    text: 'Dạ, bạn nhập Mã đơn hàng (Ví dụ: HD001 hoặc HD002) vào ô bên dưới để shop kiểm tra hành trình vận chuyển chi tiết ngay nhé! 🚚',
    isOrderLookup: true,
    time: 'Vừa xong'
  })
  scrollToBottom()
}

// Xử lý tra cứu đơn hàng và tạo thẻ hành trình đầy đủ
const doOrderLookup = async (rawCode) => {
  updateActivity()
  const code = (rawCode || orderSearchCode.value || '').trim().toUpperCase()
  if (!code || isSearchingOrder.value) return
  isSearchingOrder.value = true

  // Thêm tin nhắn của khách hàng
  messages.value.push({
    id: Date.now(),
    sender: 'user',
    text: `Tra cứu đơn hàng ${code}`,
    time: 'Vừa xong'
  })
  scrollToBottom()

  try {
    const [resOrder, resLogs] = await Promise.all([
      api.get(`/api/hoa-don/code/${code}`).catch(() => null),
      api.get(`/api/hoa-don/code/${code}/lich-su`).catch(() => null)
    ])

    if (resOrder && resOrder.data && resOrder.data.maHoaDon) {
      const o = resOrder.data
      const logs = Array.isArray(resLogs?.data) ? resLogs.data : []
      logs.sort((a, b) => new Date(b.thoiGian || 0) - new Date(a.thoiGian || 0))

      const curStatus = Number(o.trangThai || 1)
      const statusLabels = {
        1: 'Chờ xác nhận',
        2: 'Đã xác nhận',
        3: 'Chờ vận chuyển',
        4: 'Đang vận chuyển',
        5: 'Giao thành công',
        6: 'Đã hủy đơn',
        7: 'Giao hàng thất bại'
      }

      const formatStepTime = (stepNum) => {
        if (stepNum === 1) return formatMsgTime(o.ngayTao)
        const matched = logs.find(l => Number(l.trangThai) === stepNum)
        if (matched && matched.thoiGian) return formatMsgTime(matched.thoiGian)
        if (curStatus === stepNum && o.ngayCapNhat) return formatMsgTime(o.ngayCapNhat)
        return ''
      }

      let steps = []
      if (curStatus === 6) {
        steps = [
          { id: 1, name: 'Chờ xác nhận', time: formatStepTime(1), completed: true, current: false },
          { id: 6, name: 'Đã hủy đơn', time: formatMsgTime(o.ngayCapNhat || o.ngayTao), completed: false, current: true, isCancelled: true }
        ]
      } else if (curStatus === 7) {
        steps = [
          { id: 1, name: 'Chờ xác nhận', time: formatStepTime(1), completed: true, current: false },
          { id: 2, name: 'Đã xác nhận', time: formatStepTime(2), completed: true, current: false },
          { id: 3, name: 'Chờ vận chuyển', time: formatStepTime(3), completed: true, current: false },
          { id: 4, name: 'Đang vận chuyển', time: formatStepTime(4), completed: true, current: false },
          { id: 7, name: 'Giao thất bại', time: formatMsgTime(o.ngayCapNhat || o.ngayTao), completed: false, current: true, isFailed: true }
        ]
      } else {
        const defSteps = [
          { id: 1, name: 'Chờ xác nhận' },
          { id: 2, name: 'Đã xác nhận' },
          { id: 3, name: 'Chờ vận chuyển' },
          { id: 4, name: 'Đang giao hàng' },
          { id: 5, name: 'Giao thành công' }
        ]
        const maxShow = Math.min(curStatus + 1, 5)
        steps = defSteps.filter(s => s.id <= maxShow).map(s => ({
          id: s.id,
          name: s.name,
          time: formatStepTime(s.id),
          completed: s.id <= curStatus,
          current: s.id === curStatus
        }))
      }

      messages.value.push({
        id: 'track-result-' + Date.now(),
        sender: 'bot',
        text: `Dạ, shop đã tìm thấy thông tin đơn hàng **#${o.maHoaDon}** của bạn. Dưới đây là hành trình vận chuyển chi tiết:`,
        trackingData: {
          maHoaDon: o.maHoaDon,
          trangThai: o.trangThai,
          tenTrangThai: statusLabels[o.trangThai] || 'Đang xử lý',
          tenKhachHang: o.tenKhachHang,
          soDienThoaiKhachHang: o.soDienThoaiKhachHang,
          diaChiNhanHang: o.diaChiNhanHang,
          tongTien: o.tongTien,
          tienSauGiamGia: o.tienSauGiamGia,
          steps
        },
        time: 'Vừa xong'
      })
    } else {
      messages.value.push({
        id: 'track-err-' + Date.now(),
        sender: 'bot',
        text: `Dạ, shop không tìm thấy đơn hàng nào với mã **"${code}"** trên hệ thống. 🔍

Bạn vui lòng kiểm tra lại mã trên hóa đơn/email, hoặc nhập lại bên dưới nhé:`,
        isOrderLookup: true,
        time: 'Vừa xong'
      })
    }
  } catch (err) {
    messages.value.push({
      id: 'track-err-' + Date.now(),
      sender: 'bot',
      text: 'Dạ hệ thống tra cứu đơn đang cập nhật, bạn vui lòng bấm **Gặp nhân viên** để được chuyên viên hỗ trợ kiểm tra đơn ngay nhé! ❤️',
      time: 'Vừa xong'
    })
  } finally {
    isSearchingOrder.value = false
    orderSearchCode.value = ''
    scrollToBottom()
  }
}

const goToFullTracking = (code) => {
  isOpen.value = false
  router.push({ path: '/tra-cuu-don-hang', query: { code } })
}

// Hiển thị danh sách sản phẩm đợt giảm giá
const showPromoProducts = async () => {
  updateActivity()
  await loadFeaturedProducts()

  const activePromoList = discountProducts.value.filter(p =>
      p.coGiamGia === true || (p.minGiaSauGiam && p.minGia && Number(p.minGiaSauGiam) < Number(p.minGia))
  )

  if (activePromoList.length > 0) {
    const list = activePromoList.slice(0, 6)
    const maxPct = Math.max(...activePromoList.map(p => Number(p.phanTramGiam || 0)))
    const campaignNames = Array.from(new Set(activePromoList.map(p => p.tenDotGiamGia).filter(Boolean)))
    const campaign = campaignNames.length > 0 ? campaignNames.join(', ') : 'Đợt Giảm Giá Đang Hoạt Động'

    showBotReply(
        `Dạ, FF T-Shirt đang diễn ra chương trình **${campaign}** với mức ưu đãi lên đến **${maxPct > 0 ? maxPct + '%' : 'hấp dẫn'}**! 🔥

Dưới đây là các sản phẩm đang áp dụng giảm giá trực tiếp trong đợt này dành cho bạn: 👇`,
        list
    )
  } else {
    showBotReply(
        `Dạ hiện tại các chương trình đợt giảm giá vừa kết thúc hoặc đang chuẩn bị cập nhật đợt mới.\n\nFF T-Shirt xin gợi ý các sản phẩm đang có giá tốt nhất tại cửa hàng dưới đây nhé! 👇`,
        featuredProducts.value.slice(0, 4)
    )
  }
}

const handleActionClick = (action) => {
  if (!action) return
  updateActivity()

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
    triggerOrderLookupPrompt()
  } else if (action.type === 'promo') {
    messages.value.push({
      id: Date.now(),
      sender: 'user',
      text: 'Xem ưu đãi hôm nay',
      time: 'Vừa xong'
    })
    scrollToBottom()
    showPromoProducts()
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
  updateActivity()

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
    showPromoProducts()
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
  updateActivity()
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
      if (match) {
        match.id = res.data.id
        match.serverPersisted = true
      }
    }
    if (clientTypingTimeout) clearTimeout(clientTypingTimeout)
    if (conversationId.value) {
      api.post(`/api/chat/cuoc-hoi-thoai/${conversationId.value}/dang-nhap?nguoiGui=KHACH_HANG&isTyping=false`).catch(() => {})
    }
  } catch (e) {
    console.warn('Lỗi gửi tin nhắn tới server:', e)
  }
}

const showBotReply = (replyText, products = null) => {
  isTyping.value = false
  messages.value.push({
    id: 'bot-' + Date.now(),
    sender: 'bot',
    text: replyText,
    products: products || null,
    time: 'Vừa xong'
  })
  scrollToBottom()
}

// AI chạy ở Spring Boot để API key không bao giờ xuất hiện trên trình duyệt.
// Khi AI bên ngoài tạm lỗi, backend vẫn phân tích tiếng Việt cục bộ và truy vấn dữ liệu thật từ DB.
const requestAiShoppingAssistant = async (text) => {
  const history = messages.value
    .slice(0, -1)
    .filter(message => message?.text && (message.sender === 'user' || message.sender === 'bot'))
    .slice(-8)
    .map(message => ({
      role: message.sender === 'user' ? 'user' : 'assistant',
      content: String(message.text).slice(0, 1000)
    }))
  const response = await api.post('/api/ai-chat/message', { message: text, history })
  return response.data
}

// BỘ NÃO TƯ VẤN THÔNG MINH CỦA FF T-SHIRT
const generateSmartAiReply = (rawTxt) => {
  const txt = rawTxt.trim()
  const lower = txt.toLowerCase()

  // 1. Chào hỏi
  if (/^(xin\s*chào|chào|chào\s*(bạn|shop|ad|admin|em|anh|chị)|hello|hi|hey|alo|ê|ad\s*ơi|shop\s*ơi|bạn\s*ơi|cho\s*mình\s*hỏi|mình\s*cần\s*hỏi|có\s*ai|hé\s*lô)/i.test(lower) ||
      ['chào', 'xin chào', 'hi', 'hello', 'alo', 'shop ơi', 'ad ơi', 'có ai không', 'bạn ơi', 'hé lô', 'ad'].includes(lower)) {
    return {
      text: 'Dạ, FF T-Shirt xin chào bạn! Rất vui được hỗ trợ bạn hôm nay. Bạn đang cần tư vấn về chọn size áo, kiểm tra hành trình đơn hàng hay xem các đợt giảm giá hot ạ? 😊'
    }
  }

  // 2. Nhận diện trực tiếp mã đơn hàng (Ví dụ: HD001, HD002, HD00001,...)
  const orderCodeMatch = txt.match(/\b(HD\d{2,8})\b/i)
  if (orderCodeMatch) {
    return {
      isDirectLookup: true,
      orderCode: orderCodeMatch[1].toUpperCase()
    }
  }

  // 3. Tra cứu đơn hàng
  if (lower.includes('tra cứu') || lower.includes('đơn hàng') || lower.includes('kiểm tra đơn') || lower.includes('vận đơn') || lower.includes('hành trình')) {
    return {
      isOrderLookupPrompt: true
    }
  }

  // 4. Tư vấn chọn size áo thông minh (Xử lý khi chỉ có chiều cao, chỉ có cân nặng hoặc có cả hai)
  const weightMatch = lower.match(/(\d{2,3})\s*(?:kg|kí|cân|kilo)/i) || lower.match(/(?:nặng|cân\s*nặng)\s*(\d{2,3})/i)
  const heightMatch = lower.match(/(?:1m|1\.|m)(\d{2})/i) || lower.match(/(\d{3})\s*(?:cm|phân)/i) || lower.match(/(?:cao)\s*(\d{2,3})/i)

  if (weightMatch || heightMatch || lower.includes('size') || lower.includes('bảng size') || lower.includes('vừa không') || lower.includes('chật') || lower.includes('rộng') || lower.includes('mặc size gì')) {
    let weight = weightMatch ? parseInt(weightMatch[1]) : null
    let height = null
    if (heightMatch) {
      const hVal = parseInt(heightMatch[1])
      height = hVal < 100 ? 100 + hVal : hVal
    }

    // Trường hợp 4.1: Khách CHỈ nhắn chiều cao (THIẾU cân nặng)
    if (height && !weight) {
      return {
        text: `Dạ, với chiều cao **${height}cm**, bạn cho shop xin thêm **Cân nặng** (Ví dụ: 60kg hay 65kg) nữa nhé!\n\nVì vóc dáng mỗi người khác nhau (đậm người hay mảnh người), có đủ cả Chiều cao & Cân nặng thì shop sẽ chọn size vừa vặn và chuẩn form nhất cho bạn nha! 📏✨`
      }
    }

    // Trường hợp 4.2: Khách CHỈ nhắn cân nặng (THIẾU chiều cao)
    if (weight && !height) {
      return {
        text: `Dạ, với cân nặng **${weight}kg**, bạn gửi thêm cho shop **Chiều cao** (Ví dụ: 1m68 hay 1m72) nhé!\n\nShop cần thêm chiều cao để cân đối tỷ lệ dài áo và vai áo, giúp bạn chọn được chiếc áo vừa vặn nhất ạ! 👕✨`
      }
    }

    // Trường hợp 4.3: Có cả chiều cao VÀ cân nặng -> Tính size thông minh chính xác
    if (weight && height) {
      let suggestedSize = 'L'
      let desc = ''

      if (weight < 54 && height < 165) {
        suggestedSize = 'S'
        desc = 'ôm vừa vặn tôn dáng'
      } else if (weight <= 64 && height <= 172) {
        suggestedSize = 'M'
        desc = 'chuẩn form Regular Fit thanh lịch'
      } else if (weight <= 73 && height <= 177) {
        suggestedSize = 'L'
        desc = 'rất vừa vặn, cử động thoải mái'
      } else if (weight <= 83 && height <= 183) {
        suggestedSize = 'XL'
        desc = 'rộng rãi, đứng form'
      } else {
        suggestedSize = 'XXL'
        desc = 'form rộng thoải mái tối đa'
      }

      let bodyAdvice = ''
      if (height >= 175 && weight < 60) {
        bodyAdvice = '\n💡 *Lưu ý:* Bạn có dáng người cao gầy, nếu thích áo phủ dài qua mông thì có thể tăng 1 size nhé!'
      } else if (height <= 165 && weight >= 70) {
        bodyAdvice = '\n💡 *Lưu ý:* Dáng người bạn đậm, chọn size này sẽ giúp thoải mái phần bụng và ngực ạ!'
      }

      const sizeRank = ['S', 'M', 'L', 'XL', 'XXL']
      const curIdx = sizeRank.indexOf(suggestedSize)
      const oversize = curIdx < sizeRank.length - 1 ? sizeRank[curIdx + 1] : 'XXL'

      return {
        text: `Dạ với thông số (**${height}cm - ${weight}kg**), bạn mặc **Size ${suggestedSize}** (${desc}) là đẹp nhất ạ! ✨${bodyAdvice}\n\n💡 **Mẹo chọn size theo sở thích:**\n• Mặc vừa vặn, chỉn chu: Bạn chọn **Size ${suggestedSize}**\n• Mặc rộng rãi, thoải mái (Oversize): Bạn có thể chọn **Size ${oversize}** nha! ❤️`
      }
    }

    return {
      text: 'Dạ, bảng size áo chuẩn tại FF T-Shirt:\n\n• **Size S**: Dưới 54kg (chiều cao 1m55 - 1m64)\n• **Size M**: 55 - 64kg (chiều cao 1m65 - 1m70)\n• **Size L**: 65 - 73kg (chiều cao 1m71 - 1m76)\n• **Size XL**: 74 - 83kg (chiều cao 1m77 - 1m82)\n• **Size XXL**: Trên 84kg (chiều cao trên 1m80)\n\n👉 Bạn gửi giúp shop **Chiều cao và Cân nặng** (Ví dụ: 1m70 - 65kg) để shop chọn size chuẩn và vừa vặn nhất cho bạn nhé! 📏'
    }
  }

  // 5. Giá cả, khuyến mãi, đợt giảm giá
  if (lower.includes('giảm giá') || lower.includes('khuyến mãi') || lower.includes('ưu đãi') || lower.includes('sale') || lower.includes('đợt giảm')) {
    return { isPromoRequest: true }
  }

  // 6. Giao hàng / thời gian giao / phí ship (Xử lý khi khách thiếu địa chỉ)
  if (lower.includes('giao hàng') || lower.includes('ship') || lower.includes('vận chuyển') || lower.includes('bao lâu nhận') || lower.includes('phí ship') || lower.includes('bao giờ có')) {
    const provinceKeywords = ['hà nội', 'hồ chí minh', 'sài gòn', 'đà nẵng', 'hải phòng', 'cần thơ', 'bắc ninh', 'hải dương', 'nam định', 'thái bình', 'quảng ninh', 'bình dương', 'đồng nai', 'huế', 'nha trang', 'vinh']
    const foundProvince = provinceKeywords.some(p => lower.includes(p))

    if (!foundProvince) {
      return {
        text: 'Dạ FF T-Shirt hỗ trợ giao hàng tận nơi trên toàn quốc ạ! 🚚\n\n• **Nội thành Hà Nội**: Nhận hàng nhanh trong 1 - 2 ngày.\n• **Các tỉnh thành khác**: Nhận hàng từ 2 - 4 ngày.\n• **Ưu đãi freeship**: Miễn phí vận chuyển cho đơn hàng từ 500.000đ!\n\n👉 Bạn cho shop xin **Tỉnh / Thành phố** bạn muốn nhận hàng để shop báo chính xác thời gian và phí ship nhé! 📦'
      }
    } else {
      return {
        text: 'Dạ với địa chỉ của bạn, FF T-Shirt giao hàng qua đơn vị vận chuyển uy tín (Giao Hàng Nhanh / Viettel Post) chỉ mất khoảng 1 - 3 ngày làm việc là nhận được hàng ạ! Bạn được kiểm tra hàng trước khi thanh toán nhé! 🚚'
      }
    }
  }

  // 7. Yêu cầu gặp trực tiếp nhân viên
  if (lower.includes('người bán') || lower.includes('nhân viên') || lower.includes('gặp nhân viên') || lower.includes('chuyên viên') || lower.includes('người thật') || lower.includes('tư vấn viên')) {
    return { isStaffRequest: true }
  }

  // 8. Câu hỏi tổng quát khác
  return {
    text: 'Dạ FF T-Shirt đã ghi nhận câu hỏi của bạn! Bạn có thể chọn các tính năng gợi ý nhanh bên dưới (Tư vấn size, Xem ưu đãi hôm nay, Tra cứu đơn hàng) hoặc bấm **Gặp nhân viên** để được hỗ trợ chi tiết nhất nhé! ❤️'
  }
}

const handleSend = async () => {
  // Khi đang chờ nhân viên tiếp nhận (trangThai === 2), khóa không cho gửi tin nhắn
  if (conversationStatus.value === 2) return

  const txt = inputMessage.value.trim()
  if (!txt) return
  inputMessage.value = ''
  updateActivity()

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

  updateActivity()
  const aiResult = generateSmartAiReply(txt)

  if (aiResult.isStaffRequest) {
    await handleStaffRequest()
    return
  }

  if (aiResult.isDirectLookup) {
    await doOrderLookup(aiResult.orderCode)
    return
  }

  if (aiResult.isOrderLookupPrompt) {
    triggerOrderLookupPrompt()
    return
  }

  if (aiResult.isPromoRequest) {
    showPromoProducts()
    return
  }

  try {
    aiTypingLabel.value = 'đang đối chiếu màu, size và tồn kho...'
    isTyping.value = true
    const assistantResult = await requestAiShoppingAssistant(txt)
    isTyping.value = false
    aiTypingLabel.value = 'đang kiểm tra dữ liệu shop...'
    showBotReply(
        assistantResult?.reply || aiResult.text,
        Array.isArray(assistantResult?.products) ? assistantResult.products : null
    )
  } catch (error) {
    console.warn('AI shopping assistant unavailable, using local chat rules:', error)
    isTyping.value = false
    aiTypingLabel.value = 'đang kiểm tra dữ liệu shop...'
    showBotReply(aiResult.text)
  }
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

  // Chụp lịch sử trước khi thêm thông báo chuyển nhân viên để đồng bộ sang backend.
  const historyToSync = messages.value
    .filter(m => m?.text && (m.sender === 'user' || m.sender === 'bot') && m.id !== 'welcome-0')
    .map(m => ({ sender: m.sender, text: String(m.text).slice(0, 2000) }))

  // Tạo phiên mới và gửi yêu cầu hỗ trợ (trangThai = 2)
  resetRatingState()
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

  await requestStaffSupport(historyToSync)
}

const requestStaffSupport = async (historyToSync = []) => {
  if (!isClientLoggedIn.value) return
  try {
    const payload = {
      khachHangId: currentClientUser.value?.id || null,
      tenKhachHang: currentClientUser.value?.name || null,
      soDienThoai: currentClientUser.value?.phone || null
    }
    const res = await api.post('/api/chat/khoi-tao', payload)
    if (res.data && res.data.id) {
      resetRatingState()
      conversationId.value = res.data.id
      conversationStatus.value = res.data.trangThai || 2
      localStorage.setItem(CHAT_STORAGE_KEY, String(res.data.id))
      localStorage.setItem(STAFF_REQUESTED_STORAGE_KEY, 'true')
      // Lưu cả phần khách đã trao đổi với bot để reload không mất lịch sử.
      // BOT được giữ riêng, không bị hiểu nhầm là tin nhắn nhân viên.
      for (const item of historyToSync) {
        await api.post('/api/chat/gui-tin-nhan', {
          cuocHoiThoaiId: res.data.id,
          khachHangId: currentClientUser.value?.id || null,
          tenKhachHang: currentClientUser.value?.name || null,
          soDienThoai: currentClientUser.value?.phone || null,
          nguoiGui: item.sender === 'user' ? 'KHACH_HANG' : 'BOT',
          noiDung: item.text
        })
      }
      await api.put(`/api/chat/cuoc-hoi-thoai/${res.data.id}/yeu-cau-ho-tro`).catch(() => {})
      await loadMessages()
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
  localStorage.removeItem(STAFF_REQUESTED_STORAGE_KEY)
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
  const hasRequestedStaff = localStorage.getItem(STAFF_REQUESTED_STORAGE_KEY) === 'true'
  if (savedId) {
    conversationId.value = Number(savedId)
  }
  if (isClientLoggedIn.value && savedId && hasRequestedStaff) {
    initOrLoadConversation()
  } else if (savedId) {
    conversationId.value = null
    localStorage.removeItem(CHAT_STORAGE_KEY)
    localStorage.removeItem(STAFF_REQUESTED_STORAGE_KEY)
  }
  pollTimer = setInterval(pollNewMessages, 1200)

  // Kiểm tra thời gian không phản hồi để cảnh báo 2 phút và tự động đóng phiên
  inactiveTimer = setInterval(() => {
    // Theo yêu cầu: Chỉ tính khi ĐANG CHAT VỚI NHÂN VIÊN (conversationStatus === 1). Chưa tiếp nhận thì CHƯA TÍNH!
    if (!isOpen.value || conversationStatus.value !== 1) return
    const elapsedSec = Math.floor((Date.now() - lastUserActivityTime.value) / 1000)

    // 1. Sau 30 giây: nhắc khách phản hồi
    if (elapsedSec >= FIRST_WARNING_SEC && !warned5Min.value) {
      warned5Min.value = true
      const exists = messages.value.some(m => m.text && m.text.includes('30 giây nữa'))
      if (!exists) {
        messages.value.push({
          id: 'sys-warn-5-' + Date.now(),
          sender: 'system',
          text: 'Bạn vẫn còn ở đó chứ? Phiên tư vấn sẽ tự động đóng sau 30 giây nữa nếu bạn chưa phản hồi.',
          isSystem: true,
          isWarning: true
        })
        scrollToBottom()
      }
    }

    // 2. Sau 50 giây: hiện đếm ngược 10 giây
    if (elapsedSec >= COUNTDOWN_START_SEC && !warned2Min.value) {
      warned2Min.value = true
      showCountdownBanner.value = true
      remainingCountdownSec.value = Math.max(0, INACTIVITY_TIMEOUT_SEC - elapsedSec)
      startCountdown()

      const exists = messages.value.some(m => m.text && m.text.includes('10 giây nữa'))
      if (!exists) {
        messages.value.push({
          id: 'sys-warn-2-' + Date.now(),
          sender: 'system',
          text: 'Phiên tư vấn sắp kết thúc. Bạn vui lòng gửi tin nhắn trong 10 giây nữa để tiếp tục trò chuyện nhé.',
          isSystem: true,
          isWarning: true
        })
        scrollToBottom()
      }
    }

    // 3. Tự động đóng phiên sau 1 phút không phản hồi
    if (elapsedSec >= INACTIVITY_TIMEOUT_SEC) {
      handleAutoCloseDueToInactivity()
    }
  }, 2000)

  window.addEventListener('client-auth-change', refreshChatOnLogin)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (inactiveTimer) clearInterval(inactiveTimer)
  if (countdownInterval) clearInterval(countdownInterval)
  window.removeEventListener('client-auth-change', refreshChatOnLogin)
})

watch(() => currentClientUser.value, async (newVal, oldVal) => {
  if (newVal) {
    // Khi trạng thái tài khoản được hydrate sau lúc mount, phải khôi phục phiên
    // hiện tại thay vì xóa ID khiến polling realtime mất mục tiêu.
    if (!oldVal) await initOrLoadConversation()
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
        <div class="chat-header-actions">
          <button
              type="button"
              class="btn-meet-agent"
              @click="handleActionClick({ type: 'agent' })"
          >
            <span aria-hidden="true">💬</span>
            Gặp nhân viên tư vấn
          </button>
          <button
              type="button"
              class="btn-collapse"
              title="Đóng khung tư vấn"
              aria-label="Đóng khung tư vấn"
              @click="isOpen = false"
          >
            ×
          </button>
        </div>
      </div>

      <!-- Thanh đếm ngược 10 giây sắp tự động đóng phiên -->
      <transition name="slide-down">
        <div
            v-if="showCountdownBanner && conversationStatus !== 0"
            class="countdown-warning-banner"
        >
          <div class="cwb-left">
            <span class="cwb-icon">⏳</span>
            <div class="cwb-text">
              <span class="cwb-title">Sắp hết thời gian phiên chat</span>
              <span class="cwb-time">Tự động đóng sau: <strong>{{ formattedCountdown }}</strong></span>
            </div>
          </div>
          <button
              type="button"
              class="btn-cwb-dismiss"
              @click="dismissCountdownBanner"
              title="Ẩn thanh đếm ngược"
          >
            Đã hiểu
          </button>
        </div>
      </transition>

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
          <div
              v-if="msg.isSystem"
              class="system-event-bubble"
              :class="{
                'warning-timeout-bubble': msg.isWarning || (msg.text && msg.text.includes('Bạn vẫn còn ở đó chứ')),
                'auto-closed-bubble': msg.isAutoClosed
              }"
          >
            {{ msg.text }}
          </div>

          <!-- Card Đánh giá chất lượng trong dòng lịch sử chat -->
          <div v-else-if="msg.sender === 'rating'" class="rating-bubble-card">
            <div class="rb-header">
              <span class="rb-star-badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="#d2a764">
                <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
              </svg>
            </span>
              <span class="rb-title">Đánh giá chất lượng tư vấn</span>
              <span class="rb-time">{{ msg.time }}</span>
            </div>
            <div class="rb-stars-row">
              <span
                  v-for="s in 5"
                  :key="s"
                  class="rb-star-icon"
                  :class="{ filled: s <= (msg.ratingData?.diemDanhGia || 5) }"
              >★</span>
              <span class="rb-score-num">{{ msg.ratingData?.diemDanhGia || 5 }}/5 Sao</span>
            </div>
            <div v-if="msg.ratingData?.noiDungDanhGia" class="rb-comment-box">
              "{{ msg.ratingData.noiDungDanhGia }}"
            </div>
            <div class="rb-footer-note">
              ✓ Đã ghi nhận đánh giá của bạn. Cảm ơn bạn đã đồng hành cùng FF T-Shirt!
            </div>
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
              <p class="bubble-text" style="white-space: pre-line;">{{ msg.text }}</p>

              <!-- Ô NHẬP TRA CỨU ĐƠN HÀNG TRỰC TIẾP TRONG CHAT -->
              <div v-if="msg.isOrderLookup" class="order-lookup-inline-box">
                <div class="lookup-input-group">
                  <input
                      v-model="orderSearchCode"
                      type="text"
                      placeholder="Nhập mã đơn (Ví dụ: HD001)..."
                      class="lookup-text-input"
                      @keydown.enter.prevent="doOrderLookup(orderSearchCode)"
                  />
                  <button
                      type="button"
                      class="btn-lookup-submit"
                      :disabled="isSearchingOrder || !orderSearchCode.trim()"
                      @click="doOrderLookup(orderSearchCode)"
                  >
                    <span v-if="isSearchingOrder">Đang tìm...</span>
                    <span v-else>Tra cứu</span>
                  </button>
                </div>
              </div>

              <!-- THẺ HÀNH TRÌNH ĐƠN HÀNG CHI TIẾT (CHUẨN TRANG TRA CỨU) -->
              <div v-if="msg.trackingData" class="order-tracking-card">
                <div class="otc-header">
                  <div class="otc-code-row">
                    <span class="otc-icon">📦</span>
                    <strong class="otc-code">Đơn hàng #{{ msg.trackingData.maHoaDon }}</strong>
                  </div>
                  <span class="otc-status-badge" :class="'status-' + msg.trackingData.trangThai">
                    {{ msg.trackingData.tenTrangThai }}
                  </span>
                </div>

                <!-- Thanh tiến trình hành trình (Timeline Delivery Steps) -->
                <div class="otc-timeline-wrapper">
                  <div
                      v-for="(step, sIdx) in msg.trackingData.steps"
                      :key="sIdx"
                      class="otc-step-item"
                      :class="{
                      'is-completed': step.completed,
                      'is-current': step.current,
                      'is-cancelled': step.isCancelled,
                      'is-failed': step.isFailed
                    }"
                  >
                    <div class="otc-step-node">
                      <div class="otc-step-dot">
                        <span v-if="step.completed && !step.isCancelled && !step.isFailed">✓</span>
                        <span v-else-if="step.isCancelled">✕</span>
                        <span v-else-if="step.isFailed">!</span>
                        <span v-else>{{ sIdx + 1 }}</span>
                      </div>
                      <div v-if="sIdx < msg.trackingData.steps.length - 1" class="otc-step-line"></div>
                    </div>
                    <div class="otc-step-info">
                      <div class="otc-step-name">{{ step.name }}</div>
                      <div v-if="step.time" class="otc-step-time">{{ step.time }}</div>
                    </div>
                  </div>
                </div>

                <!-- Thông tin tóm tắt đơn -->
                <div class="otc-details-box">
                  <div class="otc-info-row">
                    <span class="otc-info-label">Người nhận:</span>
                    <span class="otc-info-val">{{ msg.trackingData.tenKhachHang || 'Khách hàng' }} <template v-if="msg.trackingData.soDienThoaiKhachHang">• {{ msg.trackingData.soDienThoaiKhachHang }}</template></span>
                  </div>
                  <div class="otc-info-row" v-if="msg.trackingData.diaChiNhanHang">
                    <span class="otc-info-label">Địa chỉ:</span>
                    <span class="otc-info-val">{{ msg.trackingData.diaChiNhanHang }}</span>
                  </div>
                  <div class="otc-info-row" v-if="msg.trackingData.tongTien">
                    <span class="otc-info-label">Tổng thanh toán:</span>
                    <strong class="otc-info-val price-highlight">{{ formatCurrency(msg.trackingData.tienSauGiamGia || msg.trackingData.tongTien) }}</strong>
                  </div>
                </div>

                <!-- Nút liên kết xem chi tiết trên trang Tra cứu -->
                <button
                    type="button"
                    class="otc-btn-view-full"
                    @click="goToFullTracking(msg.trackingData.maHoaDon)"
                >
                  Xem chi tiết đơn hàng trên Web &rarr;
                </button>
              </div>

              <!-- Thẻ hiển thị sản phẩm dạng băng chuyền ngang (Card Carousel) -->
              <div v-if="msg.products && msg.products.length > 0" class="products-carousel-wrap">
                <div v-for="p in msg.products" :key="p.id" class="product-mini-card">
                  <div class="card-img-wrap">
                    <img
                        :src="resolveProductImageUrl(p.anhDaiDien)"
                        :alt="p.tenSanPham"
                        class="product-thumb"
                        @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER"
                    />
                    <!-- Tag % giảm giá -->
                    <span
                        v-if="p.coGiamGia || (p.phanTramGiam && Number(p.phanTramGiam) > 0) || (p.minGiaSauGiam && p.minGia && Number(p.minGiaSauGiam) < Number(p.minGia))"
                        class="card-discount-tag"
                    >
                      -{{ Math.round(p.phanTramGiam || ((p.minGia - p.minGiaSauGiam) / p.minGia * 100) || 20) }}%
                    </span>
                  </div>
                  <div class="card-info">
                    <div class="card-brand">{{ p.tenThuongHieu || 'FF T-SHIRT' }}</div>

                    <!-- Tên đợt giảm giá đang hoạt động -->
                    <div v-if="p.tenDotGiamGia" class="card-campaign-badge" :title="p.tenDotGiamGia">
                      🏷️ {{ p.tenDotGiamGia }}
                    </div>

                    <div class="card-title" :title="p.tenSanPham">{{ p.tenSanPham }}</div>

                    <!-- Hàng giá: Giá được giảm (sau giảm) & Giá gốc gạch ngang -->
                    <div class="card-price-row">
                      <!-- Giá được giảm (sau giảm) -->
                      <span class="card-cur-price">
                        {{ formatCurrency(p.minGiaSauGiam || p.minGia || 189000) }}
                      </span>
                      <!-- Giá gốc (gạch ngang) -->
                      <span
                          v-if="p.coGiamGia || (p.minGiaSauGiam && p.minGia && Number(p.minGiaSauGiam) < Number(p.minGia))"
                          class="card-old-price"
                      >
                        {{ formatCurrency(p.minGia || 250000) }}
                      </span>
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

        <!-- Trạng thái đang gõ (AI Bot hoặc Chuyên viên tư vấn kiểu Zalo) -->
        <div v-if="isTyping || staffIsTyping" class="message-row bot zalo-typing-row">
          <div class="sender-header-tag">
            <span>{{ staffIsTyping ? `${assignedStaffName || 'Chuyên viên tư vấn'} đang soạn tin...` : `Tư vấn FF T-Shirt ${aiTypingLabel}` }}</span>
          </div>
          <div class="zalo-typing-bubble">
            <span class="z-dot"></span>
            <span class="z-dot"></span>
            <span class="z-dot"></span>
          </div>
        </div>

        <!-- Form đánh giá khi kết thúc phiên tư vấn (chỉ hiện khi đã đóng phiên và chưa đánh giá) -->
        <div v-if="conversationStatus === 0 && !hasSubmittedRating" class="rating-form-card">
          <div class="rf-header">
            <div class="rf-title-row">
              <span class="rf-icon">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="#d2a764">
                  <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
                </svg>
              </span>
              <span class="rf-title">Đánh giá chất lượng hỗ trợ</span>
            </div>
            <p class="rf-subtitle">Hãy cho FF T-Shirt biết mức độ hài lòng của bạn về chuyên viên tư vấn nhé!</p>
          </div>

          <!-- Picker 5 sao -->
          <div class="rf-stars-picker">
            <div class="rf-stars-buttons">
              <button
                  v-for="s in 5"
                  :key="s"
                  type="button"
                  class="rf-star-btn"
                  :class="{ active: s <= (hoverScore || ratingScore) }"
                  @mouseenter="hoverScore = s"
                  @mouseleave="hoverScore = 0"
                  @click="ratingScore = s"
                  :title="s + ' sao'"
              >★</button>
            </div>
            <span class="rf-star-label">{{ getRatingLabel(hoverScore || ratingScore) }}</span>
          </div>

          <!-- Nhập nhận xét -->
          <div class="rf-input-box">
            <textarea
                v-model="ratingComment"
                rows="2"
                class="rf-textarea"
                placeholder="Nhập cảm nhận của bạn (tùy chọn)..."
            ></textarea>
          </div>

          <!-- Nút gửi đánh giá -->
          <button
              type="button"
              class="rf-submit-btn"
              :disabled="isSubmittingRating"
              @click="submitRating"
          >
            <span v-if="isSubmittingRating">Đang gửi đánh giá...</span>
            <span v-else>Gửi đánh giá</span>
          </button>
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
              @input="onClientInput"
              @focus="onClientInput"
              @blur="onClientBlur"
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

@media (max-width: 640px) {
  .chat-widget-root {
    right: max(8px, env(safe-area-inset-right));
    bottom: max(8px, env(safe-area-inset-bottom));
  }

  .chat-window-box {
    position: fixed;
    left: 8px;
    right: 8px;
    bottom: max(8px, env(safe-area-inset-bottom));
    width: auto;
    max-width: none;
    height: min(680px, calc(100dvh - 16px));
    max-height: calc(100dvh - 16px);
    border-radius: 16px;
  }
}

@media (max-width: 390px) {
  .chat-window-box {
    left: 4px;
    right: 4px;
    bottom: max(4px, env(safe-area-inset-bottom));
    height: calc(100dvh - 8px);
    max-height: calc(100dvh - 8px);
    border-radius: 14px;
  }
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
  width: 30px;
  height: 30px;
  font-size: 22px;
  line-height: 1;
  font-weight: 400;
  color: #475569;
  cursor: pointer;
  padding: 0;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.btn-collapse:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.btn-meet-agent {
  height: 30px;
  padding: 0 11px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-meet-agent:hover {
  color: #1d4ed8;
  border-color: #93c5fd;
  background: #eff6ff;
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
  font-size: 13px;
  font-weight: 700;
  color: #dc2626;
}

.card-old-price {
  font-size: 11px;
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
/* Typing indicator kiểu Zalo */
.zalo-typing-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 6px 0;
  animation: fadeInTyping 0.25s ease-out;
}

@keyframes fadeInTyping {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.zalo-typing-content {
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
}

.zalo-typing-label {
  font-size: 11px;
  color: #64748b;
  font-style: italic;
  padding-left: 2px;
}

.zalo-typing-bubble {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 8px 14px;
  border-radius: 14px;
  border-bottom-left-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  width: fit-content;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.zalo-typing-bubble .z-dot {
  width: 6px;
  height: 6px;
  background: #496883;
  border-radius: 50%;
  animation: zaloDotBounce 1.4s infinite ease-in-out both;
}

.zalo-typing-bubble .z-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.zalo-typing-bubble .z-dot:nth-child(2) {
  animation-delay: -0.16s;
}

.zalo-typing-bubble .z-dot:nth-child(3) {
  animation-delay: 0s;
}

@keyframes zaloDotBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1.15);
    opacity: 1;
  }
}

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

/* Thanh đếm ngược 10 giây sắp tự động đóng */
.countdown-warning-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: linear-gradient(135deg, #fffcf5 0%, #fcf6e8 100%);
  border-bottom: 1.5px solid #d2a764;
  padding: 8px 14px;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.08);
  z-index: 10;
  animation: slideDownBanner 0.25s ease-out;
}

@keyframes slideDownBanner {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.cwb-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.cwb-icon {
  font-size: 18px;
  animation: pulseIcon 1.5s infinite ease-in-out;
}

@keyframes pulseIcon {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.cwb-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.cwb-title {
  font-size: 11px;
  font-weight: 700;
  color: #496883;
  line-height: 1.2;
}

.cwb-time {
  font-size: 11px;
  color: #64748b;
  line-height: 1.2;
}

.cwb-time strong {
  color: #b45309;
  font-weight: 700;
  font-size: 12px;
}

.btn-cwb-dismiss {
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 5px 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
  box-shadow: 0 2px 4px rgba(73, 104, 131, 0.2);
}

.btn-cwb-dismiss:hover {
  background: #385269;
  transform: translateY(-1px);
}

.btn-cwb-dismiss:active {
  transform: translateY(0);
}

/* Badge tên đợt giảm giá trong thẻ sản phẩm */
.card-campaign-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #fdf5e6;
  border: 1px solid #d2a764;
  color: #8c6827;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Cảnh báo khách chưa phản hồi */
.warning-timeout-bubble {
  background: #fbf7ee !important;
  color: #496883 !important;
  border: 1.5px solid #d2a764 !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.12);
  animation: pulseWarning 2s infinite ease-in-out;
}

.auto-closed-bubble {
  position: relative;
  background: linear-gradient(135deg, #eef6ff 0%, #f5f9ff 100%) !important;
  color: #274c6b !important;
  border: 1px solid #b9d3ea !important;
  border-left: 4px solid #496883 !important;
  border-radius: 12px !important;
  padding: 12px 14px 12px 38px !important;
  text-align: left !important;
  font-weight: 600 !important;
  line-height: 1.55 !important;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.13) !important;
}

.auto-closed-bubble::before {
  content: '✓';
  position: absolute;
  left: 13px;
  top: 12px;
  width: 17px;
  height: 17px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #496883;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

@keyframes pulseWarning {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}

/* Thẻ hiển thị đánh giá trong luồng chat - Chuẩn xanh dương #496883 & be #d2a764 */
.rating-bubble-card {
  width: 100%;
  max-width: 320px;
  margin: 8px auto;
  background: linear-gradient(135deg, #ffffff 0%, #fdfbf7 50%, #fbf6ec 100%);
  border: 1.5px solid #d2a764;
  border-radius: 14px;
  padding: 12px 14px;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.08), 0 2px 6px rgba(210, 167, 100, 0.12);
  text-align: left;
}

.rb-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.rb-star-badge {
  color: #d2a764;
  display: inline-flex;
  align-items: center;
}

.rb-title {
  font-size: 12px;
  font-weight: 800;
  color: #496883;
  flex: 1;
}

.rb-time {
  font-size: 11px;
  color: #7a8e9e;
}

.rb-stars-row {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-bottom: 6px;
}

.rb-star-icon {
  font-size: 18px;
  color: #ded5c7;
  line-height: 1;
}

.rb-star-icon.filled {
  color: #d2a764;
}

.rb-score-num {
  font-size: 12px;
  font-weight: 800;
  color: #496883;
  margin-left: 6px;
}

.rb-comment-box {
  font-size: 12px;
  color: #2b3a4a;
  background: #ffffff;
  border: 1px solid #edd9c0;
  border-left: 3px solid #496883;
  border-radius: 8px;
  padding: 6px 10px;
  margin-bottom: 6px;
  font-style: italic;
  word-break: break-word;
}

/* Form đánh giá khi kết thúc phiên - Chuẩn xanh dương #496883 & be #d2a764 */
.rating-form-card {
  width: 100%;
  background: linear-gradient(135deg, #ffffff 0%, #fdfbf7 100%);
  border: 1.5px solid #d2a764;
  border-radius: 14px;
  padding: 14px;
  margin: 10px 0 6px;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.08);
  box-sizing: border-box;
}

.rf-header {
  margin-bottom: 10px;
}

.rf-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 800;
  color: #496883;
}

.rf-icon {
  color: #d2a764;
  display: inline-flex;
  align-items: center;
}

.rf-subtitle {
  font-size: 11.5px;
  color: #64748b;
  margin: 4px 0 0;
  line-height: 1.4;
}

.rf-stars-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  background: #fbf8f2;
  border: 1px solid #f0e6d6;
  padding: 8px 12px;
  border-radius: 8px;
}

.rf-stars-buttons {
  display: flex;
  gap: 4px;
}

.rf-star-btn {
  background: none;
  border: none;
  font-size: 24px;
  line-height: 1;
  color: #ded5c7;
  cursor: pointer;
  padding: 0;
  transition: transform 0.15s, color 0.15s;
}

.rf-star-btn:hover {
  transform: scale(1.2);
}

.rf-star-btn.active {
  color: #d2a764;
}

.rf-star-label {
  font-size: 11.5px;
  font-weight: 700;
  color: #496883;
}

.rf-input-box {
  margin-bottom: 10px;
}

.rf-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  border: 1px solid #d8cfc4;
  border-radius: 8px;
  font-size: 12px;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.rf-textarea:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.15);
}

.rf-submit-btn {
  width: 100%;
  padding: 9.5px;
  border: 1px solid #d2a764;
  border-radius: 8px;
  background: linear-gradient(135deg, #496883 0%, #364f66 100%);
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
}

.rf-submit-btn:hover {
  opacity: 0.95;
  transform: translateY(-1px);
  background: linear-gradient(135deg, #3d586f 0%, #2b3f52 100%);
}

.rf-submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Ô NHẬP TRA CỨU ĐƠN HÀNG TRỰC TIẾP TRONG CHAT */
.order-lookup-inline-box {
  margin-top: 10px;
  background: #fbf8f2;
  border: 1px solid #ebd9c0;
  border-radius: 10px;
  padding: 8px 10px;
}

.lookup-input-group {
  display: flex;
  gap: 6px;
}

.lookup-text-input {
  flex: 1;
  padding: 7px 10px;
  border: 1.5px solid #d8cfc4;
  border-radius: 7px;
  font-size: 12px;
  font-family: inherit;
  outline: none;
  background: #ffffff;
}

.lookup-text-input:focus {
  border-color: #496883;
}

.btn-lookup-submit {
  padding: 7px 14px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.btn-lookup-submit:hover {
  opacity: 0.9;
}

.btn-lookup-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* THẺ HÀNH TRÌNH ĐƠN HÀNG (ORDER TRACKING CARD) */
.order-tracking-card {
  margin-top: 10px;
  background: linear-gradient(135deg, #ffffff 0%, #fdfbf7 50%, #fbf6ec 100%);
  border: 1.5px solid #d2a764;
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.08), 0 2px 6px rgba(210, 167, 100, 0.12);
  text-align: left;
}

.otc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebd9c0;
}

.otc-code-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.otc-icon {
  font-size: 16px;
}

.otc-code {
  font-size: 13px;
  font-weight: 800;
  color: #496883;
}

.otc-status-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fbf7ee;
  color: #496883;
  border: 1px solid #d2a764;
}

.otc-status-badge.status-5 {
  background: #ecfdf5;
  color: #047857;
  border-color: #a7f3d0;
}

.otc-status-badge.status-6, .otc-status-badge.status-7 {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}

/* Timeline Stepper */
.otc-timeline-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin-bottom: 12px;
  padding-left: 4px;
}

.otc-step-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  position: relative;
}

.otc-step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.otc-step-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.otc-step-item.is-completed .otc-step-dot {
  background: #496883;
  color: #ffffff;
}

.otc-step-item.is-current .otc-step-dot {
  background: #d2a764;
  color: #ffffff;
  box-shadow: 0 0 0 3px rgba(210, 167, 100, 0.25);
}

.otc-step-item.is-cancelled .otc-step-dot,
.otc-step-item.is-failed .otc-step-dot {
  background: #ef4444;
  color: #ffffff;
}

.otc-step-line {
  width: 2px;
  height: 22px;
  background: #e2e8f0;
  margin: 2px 0;
}

.otc-step-item.is-completed .otc-step-line {
  background: #496883;
}

.otc-step-info {
  display: flex;
  flex-direction: column;
  padding-top: 1px;
  margin-bottom: 8px;
}

.otc-step-name {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.otc-step-item.is-completed .otc-step-name {
  color: #1e293b;
  font-weight: 700;
}

.otc-step-item.is-current .otc-step-name {
  color: #496883;
  font-weight: 800;
}

.otc-step-item.is-cancelled .otc-step-name {
  color: #ef4444;
  font-weight: 700;
}

.otc-step-time {
  font-size: 10.5px;
  color: #7a8e9e;
}

/* Details Box */
.otc-details-box {
  background: #ffffff;
  border: 1px solid #edd9c0;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 10px;
  font-size: 11.5px;
  line-height: 1.5;
}

.otc-info-row {
  display: flex;
  margin-bottom: 3px;
}

.otc-info-row:last-child {
  margin-bottom: 0;
}

.otc-info-label {
  color: #64748b;
  width: 95px;
  flex-shrink: 0;
}

.otc-info-val {
  color: #1e293b;
  font-weight: 500;
  word-break: break-word;
}

.otc-info-val.price-highlight {
  color: #d2a764;
  font-weight: 800;
}

.otc-btn-view-full {
  width: 100%;
  padding: 7px;
  background: #fbf7ee;
  border: 1px solid #d2a764;
  border-radius: 7px;
  color: #496883;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.otc-btn-view-full:hover {
  background: #496883;
  color: #ffffff;
}
</style>
