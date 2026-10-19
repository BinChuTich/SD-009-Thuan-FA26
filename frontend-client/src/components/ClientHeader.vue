<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { cartCount } from '@/utils/cartStorage.js'

const router = useRouter()
const showUserMenu = ref(false)
const showNoti = ref(false)
const mobileMenuOpen = ref(false)
const searchKeyword = ref('')

const currentUser = ref({
  name: 'DƯƠNG NGỌC CHIẾN',
  phone: '0988776655',
  email: 'chien.duong@example.com'
})

const notifications = ref([
  { id: 1, text: 'Đơn hàng #HD123 của bạn đang được chuẩn bị giao!', time: '10 phút trước', unread: true },
  { id: 2, text: 'Ưu đãi Flash Sale giảm 30% cho áo thun Polo vừa bắt đầu!', time: '1 giờ trước', unread: false },
  { id: 3, text: 'Voucher VC-DCB392 đã được thêm vào ví của bạn.', time: 'Hôm qua', unread: false }
])

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
  showNoti.value = false
}

const toggleNoti = () => {
  showNoti.value = !showNoti.value
  showUserMenu.value = false
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
</script>

<template>
  <header class="client-header-wrap">

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

        <!-- LOGO CỦA BẠN CHÍNH XÁC THEO ADMIN (3 TIA SÁNG + CHỮ FF + T-shirt) -->
        <router-link to="/" class="brand-logo-link">
          <div class="logo-wrapper">
            <!-- 3 tia sáng phát ra phía trên bên trái -->
            <div class="sparkles">
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

        <!-- Menu điều hướng chính ở giữa (Desktop) -->
        <nav class="nav-links">
          <router-link to="/" class="nav-item" exact-active-class="active">TRANG CHỦ</router-link>
          <router-link to="/gioi-thieu" class="nav-item" active-class="active">GIỚI THIỆU</router-link>
          <router-link to="/cua-hang" class="nav-item" active-class="active">SẢN PHẨM</router-link>
          <router-link to="/tra-cuu-don-hang" class="nav-item" active-class="active">TRA CỨU</router-link>
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

          <!-- Chuông thông báo -->
          <div class="action-dropdown-wrap">
            <button class="icon-btn" @click="toggleNoti" title="Thông báo">
              <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
              </svg>
              <span class="dot-unread"></span>
            </button>

            <!-- Popup thông báo -->
            <div v-if="showNoti" class="dropdown-popup noti-popup">
              <div class="popup-title">Thông báo mới</div>
              <div class="noti-list">
                <div v-for="n in notifications" :key="n.id" class="noti-item" :class="{ unread: n.unread }">
                  <p class="noti-text">{{ n.text }}</p>
                  <span class="noti-time">{{ n.time }}</span>
                </div>
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

          <!-- Avatar / Tên khách hàng (Desktop) dịch sang bên phải -->
          <div class="action-dropdown-wrap desktop-only user-account-wrap">
            <button class="user-profile-btn" @click="toggleUserMenu">
              <span class="user-avatar-circle">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
              </span>
              <span class="user-name-text">{{ currentUser.name }}</span>
              <svg class="chevron-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </button>

            <!-- Popup tài khoản -->
            <div v-if="showUserMenu" class="dropdown-popup user-popup">
              <div class="user-summary">
                <strong>{{ currentUser.name }}</strong>
                <span>{{ currentUser.phone }}</span>
              </div>
              <div class="popup-menu-divider"></div>
              <router-link to="/don-hang" class="menu-link" @click="showUserMenu = false">
                📄 Đơn mua của tôi
              </router-link>
              <router-link to="/tra-cuu-don-hang" class="menu-link" @click="showUserMenu = false">
                🚚 Tra cứu đơn hàng
              </router-link>
              <router-link to="/gio-hang" class="menu-link" @click="showUserMenu = false">
                🛒 Giỏ hàng ({{ cartCount }})
              </router-link>
            </div>
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

        <!-- Danh sách link trong mobile menu -->
        <nav class="mobile-nav-list">
          <router-link to="/" class="mobile-nav-link" @click="closeMobileMenu">
            🏠 TRANG CHỦ
          </router-link>
          <router-link to="/cua-hang" class="mobile-nav-link" @click="closeMobileMenu">
            👕 SẢN PHẨM (CỬA HÀNG)
          </router-link>
          <router-link to="/tra-cuu-don-hang" class="mobile-nav-link" @click="closeMobileMenu">
            🚚 TRA CỨU TIẾN ĐỘ ĐƠN
          </router-link>
          <router-link to="/gioi-thieu" class="mobile-nav-link" @click="closeMobileMenu">
            📖 GIỚI THIỆU THƯƠNG HIỆU
          </router-link>
          <router-link to="/lien-he" class="mobile-nav-link" @click="closeMobileMenu">
            📞 LIÊN HỆ HỖ TRỢ
          </router-link>
        </nav>

        <div class="mobile-drawer-footer">
          <div class="mob-user-info">
            <strong>{{ currentUser.name }}</strong>
            <p>{{ currentUser.phone }}</p>
          </div>
          <div class="mob-hotline">
            Hotline CSKH: <strong>1900 6868</strong>
          </div>
        </div>
      </div>
    </div>
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

/* LOGO CHUẨN CỦA BẠN (MATCH ADMIN 100%) */
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
  border-radius: 3px;
}

.sparkles .ray-1 {
  width: 3.5px;
  height: 9px;
  top: 0;
  right: 5px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 3.5px;
  height: 9px;
  top: 4px;
  left: 3px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 3.5px;
  height: 9px;
  bottom: 0px;
  left: -2px;
  transform: rotate(-80deg);
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
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.logo-sub {
  color: #496883;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  margin-left: 5px;
  margin-bottom: 2px;
  letter-spacing: -0.3px;
}

/* Menu links desktop */
.nav-links {
  display: flex;
  align-items: center;
  gap: 2.2rem;
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

/* Active underline màu vàng gold #d2a764 */
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
  width: 310px;
  padding: 14px;
}

.popup-title {
  font-size: 13px;
  font-weight: 700;
  color: #496883;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 8px;
}

.noti-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.noti-item {
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8fafc;
  font-size: 12px;
}

.noti-item.unread {
  background: #fefce8;
  border-left: 3px solid #d2a764;
}

.noti-text {
  margin: 0 0 4px;
  color: #1e293b;
}

.noti-time {
  font-size: 10px;
  color: #94a3b8;
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

.user-summary strong {
  font-size: 13px;
  color: #496883;
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
}

.mobile-nav-link:hover, .mobile-nav-link.router-link-exact-active {
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
@media (max-width: 1024px) {
  .nav-links {
    gap: 1.2rem;
  }
  .desktop-search-box input {
    width: 130px;
  }
}

@media (max-width: 900px) {
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
  .bar-left span:nth-child(3) {
    display: none;
  }
}

@media (max-width: 600px) {
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
