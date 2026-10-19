<script setup>
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GlobalDialog from '@/components/GlobalDialog.vue'
import { getCurrentUser, logout } from '@/utils/auth.js'

const route = useRoute()
const router = useRouter()

const openMenus = ref({
  sanPham: true,
  thuocTinh: false,
  giamGia: false
})

const toggleMenu = (menu) => {
  openMenus.value[menu] = !openMenus.value[menu]
}

const currentUser = ref(getCurrentUser())
const showUserDropdown = ref(false)

const isLoginPage = computed(() => route.path === '/login')

const isUserAdmin = computed(() => {
  if (!currentUser.value) return false
  const r = (currentUser.value.maVaiTro || currentUser.value.role || '').toUpperCase()
  return r === 'ADMIN' || r.includes('QUẢN TRỊ')
})

function onAuthChanged(e) {
  currentUser.value = e.detail
}

function handleClickOutside(e) {
  const profileMenu = document.querySelector('.user-profile-menu')
  if (profileMenu && !profileMenu.contains(e.target)) {
    showUserDropdown.value = false
  }
}

onMounted(() => {
  currentUser.value = getCurrentUser()
  window.addEventListener('auth-changed', onAuthChanged)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', onAuthChanged)
  document.removeEventListener('click', handleClickOutside)
})

function handleLogout() {
  logout()
  showUserDropdown.value = false
  router.push('/login')
}
</script>

<template>
  <!-- Nếu đang ở trang đăng nhập thì chỉ hiển thị giao diện login -->
  <div v-if="isLoginPage" class="login-wrapper-root">
    <router-view />
    <GlobalDialog />
  </div>

  <!-- Giao diện hệ thống chính sau khi đã đăng nhập -->
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="logo-box">
        <div class="logo-wrapper">
          <!-- 3 vạch tia sáng phát ra phía trên bên trái -->
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
      </div>

      <nav class="menu">
        <!-- 1. Thống kê (Chỉ Admin) -->
        <router-link v-if="isUserAdmin" to="/thong-ke" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z"/>
            </svg>
          </span>
          <span class="menu-text">Thống kê</span>
        </router-link>

        <!-- 2. Bán Hàng Tại Quầy (Cả Admin và Nhân viên) -->
        <router-link to="/ban-hang" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z"/>
            </svg>
          </span>
          <span class="menu-text">Bán Hàng Tại Quầy</span>
        </router-link>

        <!-- 3. Quản Lý Hóa Đơn (Cả Admin và Nhân viên) -->
        <router-link to="/hoa-don" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm7 17H5V8h2v2c0 .55.45 1 1 1s1-.45 1-1V8h6v2c0 .55.45 1 1 1s1-.45 1-1V8h2v12z"/>
            </svg>
          </span>
          <span class="menu-text">Quản Lý Hóa Đơn</span>
        </router-link>

        <!-- 4. Quản Lý Sản Phẩm (Cả Admin và Nhân viên) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('sanPham')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 2H4c-1.1 0-2 .9-2 2v3c0 .55.45 1 1 1h1v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8h1c.55 0 1-.45 1-1V4c0-1.1-.9-2-2-2zm-1 6v12H5V8h14zM4 4h16v2H4V4zm5 6h6v2H9v-2z"/>
              </svg>
            </span>
            <span class="menu-text">Quản Lý Sản phẩm</span>
            <span class="arrow-icon" :class="{ open: openMenus.sanPham }">⌄</span>
          </div>

          <!-- Menu con của Sản phẩm -->
          <div class="submenu-tree" v-show="openMenus.sanPham">
            <router-link to="/san-pham" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Sản phẩm</span>
            </router-link>
            <router-link to="/bien-the-san-pham" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Biến thể sản phẩm</span>
            </router-link>
          </div>
        </div>

        <!-- 5. Quản lý thuộc tính (Chỉ Admin) -->
        <div v-if="isUserAdmin" class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('thuocTinh')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l-5.5 9h11z M3 13.5h8v8H3z M17.5 13.5a4 4 0 1 1 0 8 4 4 0 0 1 0-8z"/>
              </svg>
            </span>
            <span class="menu-text">Quản lý thuộc tính</span>
            <span class="arrow-icon" :class="{ open: openMenus.thuocTinh }">⌄</span>
          </div>

          <!-- Menu con của Thuộc tính -->
          <div class="submenu-tree" v-show="openMenus.thuocTinh">
            <router-link to="/thuong-hieu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Thương hiệu</span>
            </router-link>
            <router-link to="/danh-muc" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Danh mục</span>
            </router-link>
            <router-link to="/chat-lieu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Chất liệu</span>
            </router-link>
            <router-link to="/xuat-xu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Xuất xứ</span>
            </router-link>
            <router-link to="/co-ao" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Cổ áo</span>
            </router-link>
            <router-link to="/tay-ao" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Tay áo</span>
            </router-link>
            <router-link to="/hoa-tiet" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Họa tiết</span>
            </router-link>
            <router-link to="/mau-sac" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Màu sắc</span>
            </router-link>
            <router-link to="/kich-co" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Kích cỡ</span>
            </router-link>
          </div>
        </div>

        <!-- 6. Quản lý khách hàng (Cả Admin và Nhân viên) -->
        <router-link to="/khach-hang" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </span>
          <span class="menu-text">Quản lý khách hàng</span>
        </router-link>

        <!-- 7. Quản lý nhân viên (Chỉ Admin) -->
        <router-link v-if="isUserAdmin" to="/nhan-vien" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </span>
          <span class="menu-text">Quản lý nhân viên</span>
        </router-link>

        <!-- 8. Quản Lý Giảm Giá (Chỉ Admin) -->
        <div v-if="isUserAdmin" class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('giamGia')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"/>
              </svg>
            </span>
            <span class="menu-text">Quản Lý Giảm Giá</span>
            <span class="arrow-icon" :class="{ open: openMenus.giamGia }">⌄</span>
          </div>
          <div class="submenu-tree" v-show="openMenus.giamGia">
            <router-link to="/dot-giam-gia" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Đợt giảm giá</span>
            </router-link>
            <router-link to="/phieu-giam-gia" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Phiếu giảm giá</span>
            </router-link>
          </div>
        </div>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-right">
          <!-- Người dùng đang đăng nhập -->
          <div v-if="currentUser" class="user-profile-menu">
            <div
              class="admin-badge"
              :class="{ 'badge-active': showUserDropdown }"
              @click.stop="showUserDropdown = !showUserDropdown"
              title="Thông tin tài khoản"
            >
              <div class="admin-avatar">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
              </div>
              <div class="admin-info-text">
                <span class="admin-name">{{ currentUser.tenNhanVien || currentUser.tenTaiKhoan || 'Người dùng' }}</span>
                <span class="admin-role-badge" :class="isUserAdmin ? 'role-admin' : 'role-staff'">
                  <span class="role-dot"></span>
                  {{ isUserAdmin ? 'Quản trị viên' : 'Nhân viên' }}
                </span>
              </div>
              <span class="dropdown-caret" :class="{ open: showUserDropdown }">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </span>
            </div>

            <!-- Menu dropdown cá nhân sang trọng -->
            <transition name="popover-anim">
              <div v-if="showUserDropdown" class="user-dropdown-popover" @click.stop>
                <!-- Header thông tin user -->
                <div class="popover-user-card">
                  <div class="popover-avatar-large">
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                    </svg>
                  </div>
                  <div class="popover-user-meta">
                    <div class="popover-name" :title="currentUser.tenNhanVien || currentUser.tenTaiKhoan">
                      {{ currentUser.tenNhanVien || currentUser.tenTaiKhoan }}
                    </div>
                    <div class="popover-email" :title="currentUser.email">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                        <polyline points="22,6 12,13 2,6"/>
                      </svg>
                      <span>{{ currentUser.email || 'Chưa cập nhật email' }}</span>
                    </div>
                  </div>
                </div>

                <!-- Cụm thông tin vai trò & Mã nhân viên -->
                <div class="popover-details-box">
                  <div class="popover-detail-row">
                    <span class="detail-label">Mã tài khoản:</span>
                    <span class="detail-value code-pill">{{ currentUser.maNhanVien || currentUser.tenTaiKhoan || '—' }}</span>
                  </div>
                  <div class="popover-detail-row">
                    <span class="detail-label">Vai trò:</span>
                    <span class="detail-value role-pill" :class="isUserAdmin ? 'role-pill-admin' : 'role-pill-staff'">
                      {{ isUserAdmin ? '👑 Quản trị viên' : '💼 Nhân viên bán hàng' }}
                    </span>
                  </div>
                </div>

                <div class="popover-divider"></div>

                <!-- Nút đăng xuất cao cấp -->
                <button type="button" class="btn-popover-logout" @click="handleLogout">
                  <div class="logout-icon-wrap">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                      <polyline points="16 17 21 12 16 7" />
                      <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                  </div>
                  <span>Đăng xuất hệ thống</span>
                </button>
              </div>
            </transition>
          </div>
        </div>
      </header>

      <div class="page-body">
        <router-view />
      </div>
    </main>

    <GlobalDialog />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Fredoka:wght@600;700&display=swap');

:root {
  --blue: #496883;
  --gold: #d2a764;
  --line: #e9e5db;
}

/* Layout tổng thể */
.app-shell {
  display: flex;
  min-height: 100vh;
  background-color: #f7f5ef;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Sidebar nền trắng cố định 200px */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 14rem !important;
  background-color: #ffffff;
  border-right: 1px solid #e9e5db;
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* Khu vực khung chứa Logo */
.logo-box {
  background-color: #ffffff;
  height: 96px; /* Tăng chiều cao từ 76px lên 96px để logo to không bị kích */
  padding: 14px 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-bottom: 2px solid #ecd8af;
  user-select: none;
  flex-shrink: 0;
}

.logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', sans-serif;
}

/* 3 tia sáng phát ra góc trên bên trái - Phóng to và chỉnh vị trí theo chữ lớn */
.sparkles {
  position: absolute;
  top: -8px;
  left: -10px;
  width: 28px;
  height: 28px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 3px;
}

.sparkles .ray-1 {
  width: 4px;
  height: 11px;
  top: 0;
  right: 5px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 4px;
  height: 11px;
  top: 5px;
  left: 4px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 4px;
  height: 11px;
  bottom: 0px;
  left: -3px;
  transform: rotate(-80deg);
}

/* Cụm chữ FF và chữ T-shirt */
.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 3px 6px rgba(73, 104, 131, 0.16));
}

/* 2 chữ FF to nổi bật */
.logo-name {
  display: flex;
  font-size: 52px; /* Tăng từ 38px lên 52px */
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

/* Chữ T-shirt nhỏ bên cạnh */
.logo-sub {
  color: #496883;
  font-size: 14px; /* Tăng từ 11px lên 14px */
  font-weight: 700;
  line-height: 1;
  margin-left: 5px;
  margin-bottom: 3px;
  letter-spacing: -0.3px;
}

/* Menu điều hướng */
.menu {
  flex: 1;
  padding: 14px 8px;
  background: #ffffff;
}

.menu-item {
  height: 34px;
  padding: 0 11px;
  display: flex;
  align-items: center;
  gap: 0.65rem !important;
  border-radius: 7px;
  color: #647074;
  font-size: 0.9rem !important;
  margin-bottom: 3px;
  cursor: pointer;
  text-decoration: none;
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease;
  user-select: none;
  min-height: 2.4rem !important;
}

.menu-item:hover {
  background: #f7f5ef;
  color: #496883;
}

.menu-item.active {
  background: #eaf1f4;
  color: #496883;
  font-weight: 700;
}

.menu-icon {
  width: 15px;
  text-align: center;
  color: #496883;
  font-size: 11px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}

.menu-text {
  flex: 1;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 9px;
  color: #8a9292;
  margin-left: auto;
  transition: transform 0.2s ease;
}

.arrow-icon.open {
  transform: rotate(180deg);
}

/* Menu con cấp 2 */
.submenu-tree {
  display: flex;
  flex-direction: column;
  padding-left: 14px;
  margin: 2px 0 4px;
}

.tree-item {
  height: 28px;
  padding: 0 8px;
  display: flex;
  align-items: center;
  border-radius: 6px;
  color: #647074;
  font-size: 0.85rem !important;
  text-decoration: none;
  transition: all 0.15s ease;
  min-height: 2rem !important;
}

.tree-branch {
  font-family: monospace, sans-serif;
  color: #9aa0a0;
  margin-right: 6px;
  font-size: 9px;
  user-select: none;
  flex-shrink: 0;
}

.tree-item:hover {
  background-color: #f7f5ef;
  color: #496883;
}

.tree-item.active {
  background-color: #eaf1f4;
  color: #496883;
  font-weight: 700;
}

.tree-item.active .tree-branch {
  color: #496883;
}

/* Nội dung chính */
.main-content {
  margin-left: 14rem;
  width: calc(100% - 14rem);
  min-height: 100vh;
  background-color: #f7f5ef;
  display: flex;
  flex-direction: column;
}

.top-header {
  height: 3.5rem !important;
  background-color: transparent;
  padding: 0 2rem;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-profile-menu {
  position: relative;
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 14px 4px 6px;
  border-radius: 999px;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
}

.admin-badge:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.admin-badge.badge-active {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #496883 0%, #2b4356 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}

.admin-avatar svg {
  width: 20px;
  height: 20px;
}

.admin-info-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.admin-name {
  font-size: 0.88rem !important;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.2px;
}

.admin-role-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.72rem;
  font-weight: 600;
  margin-top: 1px;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.role-admin {
  color: #b45309;
}
.role-admin .role-dot {
  background: #f59e0b;
}

.role-staff {
  color: #0284c7;
}
.role-staff .role-dot {
  background: #0ea5e9;
}

.dropdown-caret {
  display: flex;
  align-items: center;
  color: #94a3b8;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  margin-left: 2px;
}

.dropdown-caret.open {
  transform: rotate(180deg);
  color: #496883;
}

/* Menu dropdown cá nhân */
.user-dropdown-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 280px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e8e4dc;
  box-shadow: 0 14px 35px -5px rgba(0, 0, 0, 0.12), 0 6px 12px -4px rgba(0, 0, 0, 0.05);
  padding: 16px;
  z-index: 1000;
}

/* Header popup */
.popover-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.popover-avatar-large {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #496883 0%, #2b4356 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 3px 8px rgba(73, 104, 131, 0.25);
}

.popover-avatar-large svg {
  width: 24px;
  height: 24px;
}

.popover-user-meta {
  flex: 1;
  min-width: 0;
}

.popover-name {
  font-size: 0.94rem;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: -0.2px;
}

.popover-email {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.76rem;
  color: #64748b;
  margin-top: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.popover-email svg {
  flex-shrink: 0;
  color: #94a3b8;
}

.popover-email span {
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Details Box */
.popover-details-box {
  margin-top: 14px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.popover-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.78rem;
}

.detail-label {
  color: #64748b;
  font-weight: 500;
}

.detail-value {
  font-weight: 600;
}

.code-pill {
  font-weight: 700;
  color: #334155;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.75rem;
}

.role-pill {
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 6px;
  font-size: 0.72rem;
}

.role-pill-admin {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.role-pill-staff {
  background: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.popover-divider {
  height: 1px;
  background: #edf2f7;
  margin: 14px 0 10px;
}

/* Nút đăng xuất */
.btn-popover-logout {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 9px;
  background: transparent;
  border: 1px solid transparent;
  color: #ef4444;
  font-weight: 600;
  font-size: 0.84rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-popover-logout:hover {
  background: #fef2f2;
  border-color: #fee2e2;
  color: #dc2626;
}

.logout-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #dc2626;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.btn-popover-logout:hover .logout-icon-wrap {
  transform: translateX(2px);
  background: #fecaca;
}

/* Transitions */
.popover-anim-enter-active,
.popover-anim-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.popover-anim-enter-from,
.popover-anim-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}

.logged-out-header-box {
  display: flex;
  align-items: center;
}

.btn-re-login {
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-re-login:hover {
  background: #3a546b;
}

/* Modal overlay khi bị đăng xuất */
.logged-out-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
}

.logged-out-modal-card {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 16px;
  padding: 2.2rem 2rem;
  text-align: center;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.25);
  animation: cardZoomIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cardZoomIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.logged-out-icon-wrap {
  width: 72px;
  height: 72px;
  margin: 0 auto 1.25rem;
  border-radius: 50%;
  background: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logged-out-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.65rem;
}

.logged-out-desc {
  font-size: 0.9rem;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 1.6rem;
}

.btn-relogin-action {
  background: #dc2626;
  color: #ffffff;
  border: none;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 0.75rem 1.6rem;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
  transition: all 0.2s;
}

.btn-relogin-action:hover {
  background: #b91c1c;
  box-shadow: 0 6px 16px rgba(185, 28, 28, 0.35);
}

.page-body {
  flex: 1;
  width: 100%;
}

@media print {
  .sidebar,
  .top-header,
  header {
    display: none !important;
    visibility: hidden !important;
    width: 0 !important;
    height: 0 !important;
    overflow: hidden !important;
  }

  .app-shell {
    display: block !important;
    min-height: auto !important;
    background: transparent !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  .main-content {
    margin: 0 !important;
    margin-left: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    background: transparent !important;
  }

  .page-body {
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
  }
}

.login-wrapper-root {
  width: 100vw;
  min-height: 100vh;
  margin: 0;
  padding: 0;
}

.badge-role-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 700;
  margin-left: 6px;
}

.badge-role-admin {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}

.badge-role-staff {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}
</style>