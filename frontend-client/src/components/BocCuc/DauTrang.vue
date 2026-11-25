<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { cartCount } from '@/utils/luuTruGioHang.js'
import {
  notifications,
  unreadCount,
  fetchNotifications,
  markAsRead,
  markAllAsRead,
  clearNotifications
} from '@/utils/thongBao.js'
import { currentClientUser, isClientLoggedIn, logoutClient } from '@/utils/taiKhoanKhachHang.js'
import ModalDangNhap from '@/components/BocCuc/ModalDangNhap.vue'
import { showAppToast } from '@/utils/thongBaoDialog.js'

const router = useRouter()
const route = useRoute()
const showUserMenu = ref(false)
const showNoti = ref(false)
const mobileMenuOpen = ref(false)
const searchKeyword = ref('')
const activeSection = ref(route.hash ? route.hash.replace('#', '') : '')

// Tab lọc thông báo: 'all' | 'order' | 'deal'
const notiFilterTab = ref('all')
const toastMessage = ref('')
let toastTimer = null

const showToast = (msg) => {
  toastMessage.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, 2500)
}

const copyVoucher = (code) => {
  if (!code) return
  try {
    navigator.clipboard.writeText(code)
    showToast(`Đã sao chép mã voucher ${code}!`)
  } catch (e) {
    showToast(`Mã voucher: ${code}`)
  }
}

const filteredNotifications = computed(() => {
  if (notiFilterTab.value === 'order') {
    return notifications.value.filter(n => n.category === 'order')
  }
  if (notiFilterTab.value === 'deal') {
    return notifications.value.filter(n => n.category === 'deal' || n.category === 'voucher')
  }
  return notifications.value
})

const orderUnreadCount = computed(() => {
  return notifications.value.filter(n => n.category === 'order' && n.unread).length
})

const dealUnreadCount = computed(() => {
  return notifications.value.filter(n => (n.category === 'deal' || n.category === 'voucher') && n.unread).length
})

const getCleanOrderTitle = (n) => {
  if (!n) return 'Đặt hàng thành công!'
  let t = n.title || ''
  t = t.replace(/^[\p{Emoji}\s]+/u, '').trim()
  if (t.includes('!')) {
    return t.split('!')[0] + '!'
  }
  if (t.includes('Đơn #')) {
    return t.split('Đơn #')[0].trim()
  }
  return t || 'Đặt hàng thành công!'
}

const getOrderCode = (n) => {
  if (!n) return ''
  if (n.orderCode) return n.orderCode
  const match = (n.title || '').match(/#([A-Za-z0-9_-]+)/)
  if (match) return match[1]
  const matchDesc = (n.desc || '').match(/#([A-Za-z0-9_-]+)/)
  if (matchDesc) return matchDesc[1]
  return ''
}

const showLoginModal = ref(false)
const openLoginModal = () => {
  showLoginModal.value = true
  showUserMenu.value = false
  showNoti.value = false
}

const handleLoginSuccess = (userData) => {
  fetchNotifications()
}

const handleLogout = () => {
  logoutClient()
  clearNotifications()
  showNoti.value = false
  showUserMenu.value = false
  showAppToast('Đã đăng xuất tài khoản!', 'success')
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
  showNoti.value = false
}

const toggleNoti = () => {
  showNoti.value = !showNoti.value
  showUserMenu.value = false
  if (showNoti.value) {
  fetchNotifications()
  }
}

const handleNotiClick = (n) => {
  markAsRead(n.id)
  showNoti.value = false

  if (n.voucherCode) {
    copyVoucher(n.voucherCode)
  }

  if (n.link) {
    if (n.link.startsWith('/#')) {
      const sectionId = n.link.replace('/#', '')
      scrollToHomeSection(sectionId)
    } else {
      router.push(n.link)
    }
  }
}

const handleMarkAllRead = () => {
  markAllAsRead()
  showToast('Đã đánh dấu tất cả thông báo là đã đọc!')
}

const handleDocumentClick = (e) => {
  const notiWrap = document.querySelector('.noti-dropdown-wrap')
  const userWrap = document.querySelector('.user-account-wrap')

  if (notiWrap && !notiWrap.contains(e.target)) {
    showNoti.value = false
  }
  if (userWrap && !userWrap.contains(e.target)) {
    showUserMenu.value = false
  }
}

const handleKeydown = (e) => {
  if (e.key === 'Escape') {
    showNoti.value = false
    showUserMenu.value = false
  }
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/cua-hang', query: { q: searchKeyword.value.trim() } })
    mobileMenuOpen.value = false
  } else {
    router.push('/cua-hang')
  }
}

const closeMobileMenu = () => {
  mobileMenuOpen.value = false
}

const scrollToTop = () => {
  closeMobileMenu()
  activeSection.value = ''
  if (route.path === '/') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    if (route.hash) {
      window.history.replaceState(null, '', window.location.pathname)
    }
  } else {
    router.push('/')
  }
}

const scrollToHomeSection = (sectionId) => {
  closeMobileMenu()
  activeSection.value = sectionId
  if (route.path === '/') {
    const el = document.getElementById(sectionId)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
      window.history.replaceState(null, '', `#${sectionId}`)
    } else {
      setTimeout(() => {
        const retryEl = document.getElementById(sectionId)
        if (retryEl) {
          retryEl.scrollIntoView({ behavior: 'smooth' })
          window.history.replaceState(null, '', `#${sectionId}`)
        }
      }, 100)
    }
  } else {
    router.push({ path: '/', hash: `#${sectionId}` })
  }
}

const handleScroll = () => {
  if (route.path !== '/') return
  const scrollPos = window.scrollY + 120
  const elNoiBat = document.getElementById('san-pham-noi-bat')
  const elUuDai = document.getElementById('san-pham-uu-dai')

  if (elUuDai && scrollPos >= elUuDai.offsetTop && scrollPos < elUuDai.offsetTop + elUuDai.offsetHeight) {
    activeSection.value = 'san-pham-uu-dai'
  } else if (elNoiBat && scrollPos >= elNoiBat.offsetTop && scrollPos < elNoiBat.offsetTop + elNoiBat.offsetHeight) {
    activeSection.value = 'san-pham-noi-bat'
  } else if (window.scrollY < 200) {
    activeSection.value = ''
  }
}

watch(() => route.path, (newPath) => {
  if (newPath !== '/') {
    activeSection.value = ''
  }
})

watch(() => route.hash, (newHash) => {
  if (newHash) {
    activeSection.value = newHash.replace('#', '')
  } else if (route.path === '/') {
    activeSection.value = ''
  }
})

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('click', handleDocumentClick)
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('open-client-login', openLoginModal)
  fetchNotifications()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('click', handleDocumentClick)
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('open-client-login', openLoginModal)
})
</script>

<template>
  <header class="client-header-wrap">
    <!-- Toast thông báo nhanh -->
    <transition name="toast-fade">
      <div v-if="toastMessage" class="header-floating-toast">
        <span class="toast-icon">✨</span>
        <span>{{ toastMessage }}</span>
      </div>
    </transition>

    <!-- 2. Header chính -->
    <div class="main-nav-bar">
      <div class="container-client nav-inner">
        <!-- Nút Hamburger cho Mobile -->
        <button class="mobile-hamburger-btn" @click="mobileMenuOpen = true" title="Mở menu">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </svg>
        </button>

        <!-- LOGO THƯƠNG HIỆU: 3 TIA SÁNG + CHỮ FF + T-shirt (MATCH ADMIN 100%) -->
        <router-link to="/" class="brand-logo-link" title="FF T-shirt" @click="scrollToTop">
          <div class="logo-wrapper">
            <!-- 3 tia sáng phát ra phía trên bên trái -->
            <div class="sparkles">
                <span class="sparkle-aura"></span>
              <span class="ray ray-1"></span>
              <span class="ray ray-2"></span>
              <span class="ray ray-3"></span>
            </div>

            <div class="logo-content">
              <div class="logo-name">
                <span class="f-blue">F</span>
                <span class="f-gold">F</span>
              </div>
              <div class="logo-sub">T-shirt</div>
            </div>
          </div>
        </router-link>

        <!-- Menu điều hướng chính ở giữa (Desktop): TRANG CHỦ, SẢN PHẨM, ƯU ĐÃI, ĐƠN HÀNG, TRA CỨU, LIÊN HỆ -->
        <nav class="nav-links">
          <router-link
            to="/"
            class="nav-item"
            :class="{ active: route.path === '/' && !activeSection && (!route.hash || route.hash === '#') }"
            @click.prevent="scrollToTop"
          >
            TRANG CHỦ
          </router-link>
          <router-link to="/cua-hang" class="nav-item" active-class="active">SẢN PHẨM</router-link>
          <router-link to="/goi-y" class="nav-item" active-class="active">GỢI Ý</router-link>
          <a
            href="#san-pham-uu-dai"
            class="nav-item"
            :class="{ active: route.path === '/' && (activeSection === 'san-pham-uu-dai' || route.hash === '#san-pham-uu-dai') }"
            @click.prevent="scrollToHomeSection('san-pham-uu-dai')"
          >
            ƯU ĐÃI
          </a>
          <router-link to="/don-hang" class="nav-item" active-class="active">ĐƠN HÀNG</router-link>
          <router-link to="/tra-cuu" class="nav-item" active-class="active">TRA CỨU</router-link>
          <router-link to="/lien-he" class="nav-item" active-class="active">LIÊN HỆ</router-link>
        </nav>

        <!-- Cụm tiện ích bên phải: Tìm kiếm nhanh, Chuông, Giỏ hàng, Tài khoản -->
        <div class="header-right-actions">
          <!-- Ô tìm kiếm ẩn hiện trên desktop -->
          <div class="desktop-search-box">
            <input
              type="text"
              v-model="searchKeyword"
              placeholder="Tìm kiếm áo thun..."
              @keyup.enter="handleSearch"
            />
            <button class="search-btn-icon" @click="handleSearch" title="Tìm kiếm">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </button>
          </div>

          <!-- Chuông thông báo (Chỉ hiển thị khi tài khoản đã đăng nhập) -->
          <div v-if="isClientLoggedIn" class="action-dropdown-wrap noti-dropdown-wrap">
            <button
              class="icon-btn noti-bell-btn"
              :class="{ 'has-unread': unreadCount > 0 }"
              @click.stop="toggleNoti"
              title="Thông báo"
            >
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
              </svg>
              <!-- Huy hiệu số thông báo chưa đọc -->
              <span class="noti-badge-count" v-if="unreadCount > 0">
                {{ unreadCount > 9 ? '9+' : unreadCount }}
              </span>
            </button>

            <!-- Popup thông báo hoàn chỉnh -->
            <div v-if="showNoti" class="dropdown-popup noti-popup" @click.stop>
              <!-- 1. Header Popup -->
              <div class="noti-popup-header">
                <div class="noti-header-left">
                  <div class="noti-header-icon">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                      <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>
                  </div>
                  <span class="noti-header-title">Thông báo đơn hàng</span>
                  <span class="noti-pill-unread" v-if="unreadCount > 0">
                    <span class="noti-pulse-dot"></span>
                    {{ unreadCount }} mới
                  </span>
                </div>
                <button
                  class="btn-mark-all-read"
                  @click="handleMarkAllRead"
                  :disabled="unreadCount === 0"
                  title="Đánh dấu tất cả đã đọc"
                >
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  Đã đọc tất cả
                </button>
              </div>

              <!-- 2. Danh sách thông báo dạng cuộn -->
              <div class="noti-list-container custom-noti-scroll">
                <div
                  v-for="n in notifications"
                  :key="n.id"
                  class="noti-card-item"
                  :class="{ unread: n.unread }"
                  @click="handleNotiClick(n)"
                >
                  <!-- Avatar Icon Box -->
                  <div class="noti-item-avatar">
                    <div class="avatar-icon-box">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"/>
                        <path d="M3 6h18"/>
                        <path d="M16 10a4 4 0 0 1-8 0"/>
                      </svg>
                    </div>
                  </div>

                  <!-- Content Body -->
                  <div class="noti-card-body">
                    <div class="noti-card-top">
                      <div class="noti-title-group">
                        <h4 class="noti-card-title">{{ getCleanOrderTitle(n) }}</h4>
                        <span v-if="getOrderCode(n)" class="noti-order-chip">#{{ getOrderCode(n) }}</span>
                      </div>
                      <span class="noti-unread-pulse" v-if="n.unread" title="Chưa đọc"></span>
                    </div>

                    <p class="noti-card-desc">{{ n.desc }}</p>

                    <div class="noti-card-foot">
                      <span class="noti-time-text">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="noti-clock-icon">
                          <circle cx="12" cy="12" r="10"></circle>
                          <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                        {{ n.time }}
                      </span>
                      <span class="noti-link-cta">
                        {{ n.actionText || 'Theo dõi đơn hàng' }}
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="cta-arrow-icon">
                          <path d="M5 12h14"></path>
                          <path d="m12 5 7 7-7 7"></path>
                        </svg>
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Trạng thái trống khi chưa có thông báo đơn hàng nào -->
                <div v-if="notifications.length === 0" class="noti-empty-wrap">
                  <div class="empty-icon-circle">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                      <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                    </svg>
                  </div>
                  <strong class="empty-title">Chưa có thông báo đơn hàng</strong>
                  <p class="empty-sub">Khi bạn đặt hàng thành công, lịch sử và trạng thái cập nhật đơn hàng sẽ hiển thị tại đây.</p>
                </div>
              </div>

              <!-- 3. Footer Popup -->
              <div class="noti-popup-footer">
                <router-link to="/don-hang" class="noti-footer-link" @click="showNoti = false">
                  <span>Xem tất cả đơn hàng của bạn</span>
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14"></path>
                    <path d="m12 5 7 7-7 7"></path>
                  </svg>
                </router-link>
              </div>
            </div>
          </div>

          <!-- Nút Giỏ hàng (Icon xe đẩy tối giản nền tròn mềm mại) -->
          <router-link to="/gio-hang" class="icon-btn cart-btn" title="Giỏ hàng">
            <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="header-cart-svg">
              <circle cx="9" cy="20" r="1.5"></circle>
              <circle cx="18" cy="20" r="1.5"></circle>
              <path d="M1 1h4l2.68 12.8a2 2 0 0 0 2 1.6h9.72a2 2 0 0 0 2-1.6L23 5H6"></path>
            </svg>
            <span class="cart-badge-count" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</span>
          </router-link>

          <!-- Vạch phân cách -->
          <div class="account-divider desktop-only"></div>

          <!-- Nút ĐĂNG NHẬP (khi chưa đăng nhập) hoặc Avatar / Tên khách hàng (khi đã đăng nhập) -->
          <div class="action-dropdown-wrap desktop-only user-account-wrap">
            <!-- 1. Khi chưa đăng nhập: Hiển thị nút ĐĂNG NHẬP -->
            <button
              v-if="!isClientLoggedIn"
              class="user-profile-btn btn-header-login"
              @click="openLoginModal"
              title="Bấm để đăng nhập tài khoản"
            >
              <span class="user-avatar-circle">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </span>
              <span class="user-name-text">ĐĂNG NHẬP</span>
            </button>

            <!-- 2. Khi đã đăng nhập: Hiển thị tên tài khoản + dropdown -->
            <template v-else>
              <button class="user-profile-btn" @click="toggleUserMenu">
                <span
                  class="user-avatar-circle"
                  :style="currentClientUser?.avatarBg ? { backgroundColor: currentClientUser.avatarBg, color: '#ffffff', fontWeight: '800' } : {}"
                >
                  <template v-if="currentClientUser?.avatarText">
                    {{ currentClientUser.avatarText }}
                  </template>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </span>
                <span class="user-name-text">{{ currentClientUser?.name || 'Tài khoản' }}</span>
                <svg class="chevron-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>

              <!-- Popup tài khoản -->
              <div v-if="showUserMenu" class="dropdown-popup user-popup">
                <div class="user-summary">
                  <div class="user-summary-row">
                    <strong>{{ currentClientUser?.name }}</strong>
                    <span v-if="currentClientUser?.provider === 'google'" class="google-badge-tag">Gmail</span>
                  </div>
                  <span>{{ currentClientUser?.email || currentClientUser?.phone }}</span>
                </div>
                <div class="popup-menu-divider"></div>
                <router-link to="/don-hang" class="menu-link" @click="showUserMenu = false">
                  📦 Đơn hàng của tôi
                </router-link>
                <router-link to="/tra-cuu" class="menu-link" @click="showUserMenu = false">
                  🚚 Tra cứu đơn hàng
                </router-link>
                <router-link to="/gio-hang" class="menu-link" @click="showUserMenu = false">
                  🛒 Giỏ hàng ({{ cartCount }})
                </router-link>
                <div class="popup-menu-divider"></div>
                <button type="button" class="menu-link menu-logout-btn" @click="handleLogout">
                  🚪 Đăng xuất
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. MOBILE SLIDE-OUT DRAWER MENU (CHO ĐIỆN THOẠI & TABLET) -->
    <div v-if="mobileMenuOpen" class="mobile-drawer-overlay" @click.self="closeMobileMenu">
      <div class="mobile-drawer-content">
        <div class="drawer-header">
          <div class="drawer-logo">
            <div class="logo-wrapper mini">
              <div class="sparkles mini">
                <span class="ray ray-1"></span>
                <span class="ray ray-2"></span>
                <span class="ray ray-3"></span>
              </div>
              <div class="logo-content">
                <div class="logo-name">
                  <span class="f-blue">F</span>
                  <span class="f-gold">F</span>
                </div>
                <div class="logo-sub">T-shirt</div>
              </div>
            </div>
          </div>
          <button class="btn-close-drawer" @click="closeMobileMenu">✕</button>
        </div>

        <!-- Tìm kiếm trong mobile menu -->
        <div class="mobile-search-box">
          <input
            type="text"
            v-model="searchKeyword"
            placeholder="Tìm kiếm sản phẩm..."
            @keyup.enter="handleSearch"
          />
          <button @click="handleSearch">🔍</button>
        </div>

        <!-- Danh sách link trong mobile menu: TRANG CHỦ, SẢN PHẨM, ƯU ĐÃI, ĐƠN HÀNG, TRA CỨU, LIÊN HỆ -->
        <nav class="mobile-nav-list">
          <router-link
            to="/"
            class="mobile-nav-link"
            :class="{ active: route.path === '/' && !activeSection && (!route.hash || route.hash === '#') }"
            @click.prevent="scrollToTop"
          >
            🏠 TRANG CHỦ
          </router-link>
          <router-link to="/cua-hang" class="mobile-nav-link" @click="closeMobileMenu">
            👕 SẢN PHẨM
          </router-link>
          <router-link to="/goi-y" class="mobile-nav-link" @click="closeMobileMenu">
            💡 GỢI Ý
          </router-link>
          <a
            href="#san-pham-uu-dai"
            class="mobile-nav-link"
            :class="{ active: route.path === '/' && (activeSection === 'san-pham-uu-dai' || route.hash === '#san-pham-uu-dai') }"
            @click.prevent="scrollToHomeSection('san-pham-uu-dai')"
          >
            🔥 ƯU ĐÃI
          </a>
          <router-link to="/don-hang" class="mobile-nav-link" @click="closeMobileMenu">
            📦 ĐƠN HÀNG
          </router-link>
          <router-link to="/tra-cuu" class="mobile-nav-link" @click="closeMobileMenu">
            🔍 TRA CỨU
          </router-link>
          <router-link to="/lien-he" class="mobile-nav-link" @click="closeMobileMenu">
            📞 LIÊN HỆ
          </router-link>
          <button
            v-if="isClientLoggedIn"
            type="button"
            class="mobile-nav-link mobile-noti-btn"
            @click="closeMobileMenu(); showNoti = true"
          >
            <span>🔔 THÔNG BÁO</span>
            <span class="mob-noti-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
          </button>
        </nav>

        <div class="mobile-drawer-footer">
          <div class="mob-user-info" v-if="isClientLoggedIn">
            <strong>{{ currentClientUser?.name }}</strong>
            <p>{{ currentClientUser?.phone || currentClientUser?.email }}</p>
            <button type="button" class="mob-btn-action-logout" @click="closeMobileMenu(); handleLogout()">
              🚪 Đăng xuất
            </button>
          </div>
          <div class="mob-user-info" v-else>
            <button type="button" class="mob-btn-action-login" @click="closeMobileMenu(); openLoginModal()">
              👤 ĐĂNG NHẬP / ĐĂNG KÝ
            </button>
          </div>
          <div class="mob-hotline">
            Hotline CSKH: <strong>1900 6677</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Đăng nhập / Đăng ký tài khoản -->
    <ModalDangNhap
      v-model="showLoginModal"
      @login-success="handleLoginSuccess"
    />
  </header>
</template>

<style scoped>
.client-header-wrap {
  width: 100%;
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: #ffffff;
  border-bottom: 2px solid #ecd8af;
  box-shadow: 0 2px 10px rgba(73, 104, 131, 0.08);
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

.container-client {
  max-width: 100%;
  width: 100%;
  margin: 0 auto;
  padding: 0 2.5rem;
}


/* 2. Header chính */
.main-nav-bar {
  padding: 0.85rem 0;
  background: #ffffff;
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

/* Nút Hamburger chỉ hiện trên Mobile/Tablet */
.mobile-hamburger-btn {
  display: none;
  background: transparent;
  border: none;
  color: #496883;
  cursor: pointer;
  padding: 4px;
}

/* LOGO THƯƠNG HIỆU: MATCH ADMIN 100% */
.brand-logo-link {
  text-decoration: none;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', sans-serif;
  user-select: none;
}

.sparkles {
  position: absolute;
  top: -6px;
  left: -8px;
  width: 24px;
  height: 24px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 99px;
  transform-origin: center center;
  animation: fireworkSparkleHeader 2.4s ease-in-out infinite;
}

.sparkles .ray-1 {
  --rot: 10deg;
  width: 3.5px;
  height: 9px;
  top: 0;
  right: 5px;
  transform: rotate(10deg);
  animation-delay: 0s;
}

.sparkles .ray-2 {
  --rot: -45deg;
  width: 3.5px;
  height: 9px;
  top: 4px;
  left: 3px;
  transform: rotate(-45deg);
  animation-delay: 0.38s;
}

.sparkles .ray-3 {
  --rot: -80deg;
  width: 3.5px;
  height: 9px;
  bottom: 0px;
  left: -2px;
  transform: rotate(-80deg);
  animation-delay: 0.76s;
}

@keyframes fireworkSparkleHeader {
  0%, 100% {
    opacity: 0.4;
    transform: rotate(var(--rot, 0deg)) scale(0.85);
    background-color: #d1a868;
    box-shadow: 0 0 2px rgba(209, 168, 104, 0.3);
  }
  25% {
    opacity: 0.75;
    transform: rotate(var(--rot, 0deg)) scale(1.1);
    background-color: #fce198;
    box-shadow: 0 0 4px #ffd700, 0 0 8px rgba(210, 167, 100, 0.5);
  }
  45% {
    opacity: 1;
    transform: rotate(var(--rot, 0deg)) scale(1.35);
    background-color: #ffffff;
    box-shadow: 0 0 6px #ffffff, 0 0 12px #ffd700, 0 0 16px rgba(210, 167, 100, 0.85);
  }
  65% {
    opacity: 0.8;
    transform: rotate(var(--rot, 0deg)) scale(1.15);
    background-color: #f3c774;
    box-shadow: 0 0 5px #ffd700;
  }
  85% {
    opacity: 0.5;
    transform: rotate(var(--rot, 0deg)) scale(0.95);
    background-color: #d1a868;
  }
}

.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 2px 4px rgba(73, 104, 131, 0.16));
}

.logo-name {
  display: flex;
  font-size: 38px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -2px;
}

.f-blue {
  color: #496883;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(73, 104, 131, 0.25);
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(210, 167, 100, 0.35);
}

.logo-sub {
  color: #496883;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  margin-left: 5px;
  margin-bottom: 2px;
  letter-spacing: -0.3px;
  text-shadow: 0 1px 2px rgba(73, 104, 131, 0.2);
}

/* Menu links desktop */
.nav-links {
  display: flex;
  align-items: center;
  gap: 1.6rem;
}

.nav-item {
  color: #496883;
  text-decoration: none;
  font-weight: 700;
  font-size: 0.88rem;
  letter-spacing: 0.6px;
  padding: 8px 4px;
  white-space: nowrap;
  position: relative;
  transition: all 0.2s ease;
}

.nav-item:hover {
  color: #d2a764;
}

/* Active underline màu vàng be #d2a764 chuẩn logo thương hiệu */
.nav-item.active {
  color: #496883;
}

.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  right: 0;
  height: 2.5px;
  background-color: #d2a764;
  border-radius: 2px;
}

/* Right actions */
.header-right-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-shrink: 0;
}

.desktop-search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.desktop-search-box input {
  padding: 8px 34px 8px 14px;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 20px;
  font-size: 0.83rem;
  width: 180px;
  outline: none;
  transition: all 0.25s;
  color: #1e293b;
}

.desktop-search-box input:focus {
  width: 230px;
  background: #ffffff;
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.search-btn-icon {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.search-btn-icon:hover {
  color: #496883;
}

.action-dropdown-wrap {
  position: relative;
}

.icon-btn {
  background: transparent;
  border: none;
  color: #496883;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  text-decoration: none;
}

.icon-btn:hover {
  background-color: #f1f5f9;
  color: #d2a764;
}

.dot-unread {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 7px;
  height: 7px;
  background-color: #ef4444;
  border-radius: 50%;
}

.cart-btn {
  position: relative;
  background: transparent !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  color: #496883;
}

.cart-btn:hover {
  background-color: #f1f5f9 !important;
  color: #364f66;
  transform: translateY(-1px);
}

.header-cart-svg {
  display: block;
  transition: transform 0.2s ease;
}

.cart-btn:hover .header-cart-svg {
  transform: scale(1.08);
}

.cart-badge-count {
  position: absolute;
  top: -2px;
  right: -3px;
  background-color: #d2a764;
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 800;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #ffffff;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
}

/* Vạch phân cách & Cụm tài khoản dịch sang góc ngoài cùng bên phải */
.account-divider {
  width: 1px;
  height: 26px;
  background-color: #e2e8f0;
  margin: 0 12px 0 24px;
}

.user-account-wrap {
  margin-left: 20px; /* Đẩy sang góc ngoài cùng bên phải */
}

/* User profile button */
.user-profile-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  padding: 4px 12px 4px 5px;
  border-radius: 24px;
  transition: all 0.2s ease;
}

.user-profile-btn:hover {
  background-color: #f1f5f9;
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.08);
}

.user-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #ecd8af;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-name-text {
  font-size: 0.8rem;
  font-weight: 700;
  color: #496883;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.chevron-icon {
  color: #64748b;
  margin-left: 2px;
  transition: transform 0.2s;
}

/* Dropdown popups */
.dropdown-popup {
  position: absolute;
  top: 48px;
  right: 0;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(73, 104, 131, 0.15);
  border: 1px solid #e2e8f0;
  z-index: 1001;
  animation: dropIn 0.2s ease-out;
}

@keyframes dropIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.noti-popup {
  width: 395px;
  max-width: 92vw;
  padding: 0;
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 18px 45px -8px rgba(15, 23, 42, 0.16), 0 0 0 1px rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;
  background: #ffffff;
  animation: notiPopIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes notiPopIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.noti-badge-count {
  position: absolute;
  top: -2px;
  right: -2px;
  background-color: #ef4444;
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 800;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
  animation: badgePulse 2s infinite;
}

@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.noti-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 16px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

.noti-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.noti-header-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.noti-header-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.2px;
}

.noti-pill-unread {
  background: #fef2f2;
  color: #ef4444;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #fee2e2;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.noti-pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef4444;
  animation: pulseDot 1.5s infinite;
}

@keyframes pulseDot {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 5px rgba(239, 68, 68, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}

.btn-mark-all-read {
  background: transparent;
  border: 1px solid transparent;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.btn-mark-all-read:hover:not(:disabled) {
  background: #f8fafc;
  color: #0f172a;
  border-color: #e2e8f0;
}

.btn-mark-all-read:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.noti-list-container {
  max-height: 380px;
  overflow-y: auto;
}

.custom-noti-scroll {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f8fafc;
}

.custom-noti-scroll::-webkit-scrollbar {
  width: 5px;
}

.custom-noti-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.noti-card-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all 0.18s ease;
  background: #ffffff;
  position: relative;
}

.noti-card-item:hover {
  background: #f8fafc;
}

.noti-card-item.unread {
  background: #fcfdfe;
  border-left: 3.5px solid #d2a764;
}

.noti-card-item.unread:hover {
  background: #f8fafc;
}

/* Avatar Box */
.noti-item-avatar {
  flex-shrink: 0;
  margin-top: 1px;
}

.avatar-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #ecfdf5;
  color: #059669;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d1fae5;
  box-shadow: 0 1px 2px rgba(5, 150, 105, 0.08);
  transition: transform 0.2s ease;
}

.noti-card-item:hover .avatar-icon-box {
  transform: scale(1.05);
}

.noti-card-body {
  flex: 1;
  min-width: 0;
}

.noti-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 5px;
}

.noti-title-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  min-width: 0;
}

.noti-card-title {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.35;
}

.noti-order-chip {
  font-size: 11px;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #496883;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  letter-spacing: 0.2px;
}

.noti-unread-pulse {
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
}

.noti-card-desc {
  font-size: 12.5px;
  color: #475569;
  line-height: 1.45;
  margin: 0 0 10px;
  word-break: break-word;
}

.noti-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: #94a3b8;
}

.noti-time-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
}

.noti-clock-icon {
  flex-shrink: 0;
}

.noti-link-cta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 700;
  color: #d2a764;
  transition: all 0.2s ease;
}

.cta-arrow-icon {
  transition: transform 0.2s ease;
}

.noti-card-item:hover .noti-link-cta {
  color: #b48538;
}

.noti-card-item:hover .cta-arrow-icon {
  transform: translateX(3px);
}

/* Empty State */
.noti-empty-wrap {
  padding: 40px 24px;
  text-align: center;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon-circle {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.empty-sub {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  max-width: 260px;
  line-height: 1.45;
}

/* Footer */
.noti-popup-footer {
  padding: 11px 16px;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}

.noti-footer-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 700;
  color: #496883;
  text-decoration: none;
  transition: all 0.15s ease;
}

.noti-footer-link:hover {
  color: #d2a764;
}

.noti-footer-link svg {
  transition: transform 0.15s ease;
}

.noti-footer-link:hover svg {
  transform: translateX(3px);
}

.header-floating-toast {
  position: fixed;
  top: 75px;
  right: 24px;
  background: #1e293b;
  color: #ffffff;
  padding: 10px 18px;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  z-index: 10005;
  border: 1px solid #496883;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.mobile-noti-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}

.mob-noti-badge {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 1px 7px;
  border-radius: 10px;
}

.user-popup {
  width: 220px;
  padding: 14px;
}

.user-summary {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  color: #333333;
}

.user-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 2px;
}

.user-summary strong {
  font-size: 13px;
  color: #496883;
}

.google-badge-tag {
  background: #e8f0fe;
  color: #1a73e8;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
}

.user-summary span {
  color: #64748b;
  font-size: 11px;
}

.popup-menu-divider {
  height: 1px;
  background-color: #f1f5f9;
  margin: 10px 0;
}

.menu-link {
  display: block;
  padding: 8px 10px;
  color: #475569;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  border-radius: 6px;
  transition: all 0.15s;
}

.menu-link:hover {
  background-color: #f8fafc;
  color: #496883;
}

.menu-logout-btn {
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  color: #ef4444;
}

.menu-logout-btn:hover {
  background-color: #fef2f2;
  color: #dc2626;
}

.btn-header-login {
  padding: 4px 14px 4px 5px;
}

.btn-header-login:hover {
  border-color: #d2a764;
  background-color: #fdfbf7;
}

.btn-header-login:hover .user-name-text {
  color: #b45309;
}

.mob-btn-action-login {
  width: 100%;
  padding: 10px 14px;
  background: #d2a764;
  color: #1e293b;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}

.mob-btn-action-logout {
  margin-top: 6px;
  padding: 5px 10px;
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

/* 3. MOBILE SLIDE-OUT DRAWER */
.mobile-drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 10000;
  display: flex;
}

.mobile-drawer-content {
  width: 280px;
  max-width: 85vw;
  background: #ffffff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
  animation: slideDrawer 0.25s ease-out;
}

@keyframes slideDrawer {
  from { transform: translateX(-100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 2px solid #ecd8af;
}

.logo-wrapper.mini .logo-name {
  font-size: 28px;
}
.logo-wrapper.mini .logo-sub {
  font-size: 11px;
}

.btn-close-drawer {
  background: none;
  border: none;
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
}

.mobile-search-box {
  padding: 12px 16px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.mobile-search-box input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
}

.mobile-search-box button {
  background: #496883;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0 12px;
  cursor: pointer;
}

.mobile-nav-list {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-nav-link {
  display: block;
  padding: 12px 14px;
  color: #496883;
  text-decoration: none;
  font-size: 13px;
  font-weight: 700;
  border-radius: 6px;
  transition: all 0.15s;
  cursor: pointer;
}

.mobile-nav-link:hover,
.mobile-nav-link.router-link-exact-active,
.mobile-nav-link.active {
  background: #f8fafc;
  color: #d2a764;
}

.mobile-drawer-footer {
  padding: 16px;
  border-top: 1px solid #f1f5f9;
  background: #fafafa;
  font-size: 12px;
}

.mob-user-info strong {
  display: block;
  color: #496883;
}
.mob-user-info p {
  margin: 2px 0 8px;
  color: #64748b;
}
.mob-hotline {
  color: #64748b;
}

/* RESPONSIVE MEDIA QUERIES */
@media (max-width: 1200px) {
  .nav-links {
    gap: 1rem;
  }
  .nav-item {
    font-size: 0.84rem;
  }
}

@media (max-width: 1024px) {
  .nav-links {
    gap: 0.75rem;
  }
  .nav-item {
    font-size: 0.8rem;
    padding: 6px 2px;
  }
  .desktop-search-box input {
    width: 120px;
  }
}

@media (max-width: 960px) {
  .mobile-hamburger-btn {
    display: block;
  }
  .nav-links {
    display: none;
  }
  .desktop-search-box {
    display: none;
  }
  .desktop-only {
    display: none;
  }
  .container-client {
    padding: 0 1.25rem;
  }
  .bar-left span:nth-child(3) {
    display: none;
  }
}

@media (max-width: 600px) {
  .container-client {
    padding: 0 0.85rem;
  }
  .bar-left {
    font-size: 0.72rem;
  }
  .bar-right {
    display: none;
  }
  .logo-name {
    font-size: 30px;
  }
  .logo-sub {
    font-size: 11px;
  }
}
</style>
