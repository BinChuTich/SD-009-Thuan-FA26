<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AdminAiChat from './components/AdminAiChat.vue'
import { useRouter, useRoute } from 'vue-router'
import GlobalDialog from '@/components/GlobalDialog.vue'
import { getCurrentUser, setCurrentUser, logout, isAdmin } from '@/utils/auth.js'

const openMenus = ref({
  sanPham: true,
  thuocTinh: true,
  giamGia: false,
  lichLamViec: true
})

const toggleMenu = (menu) => {
  openMenus.value[menu] = !openMenus.value[menu]
}

const router = useRouter()
const route = useRoute()
const currentUser = ref(getCurrentUser())
const showUserDropdown = ref(false)
const userMenuRef = ref(null)

const isUserAdmin = computed(() => {
  if (!currentUser.value) return false
  return isAdmin()
})

function getAvatarInitials(name) {
  if (!name) return 'U'
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) {
    const first = parts[parts.length - 2][0]
    const second = parts[parts.length - 1][0]
    return (first + second).toUpperCase()
  }
  return parts[0].slice(0, 2).toUpperCase()
}

function handleClickOutside(e) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserDropdown.value = false
  }
}

function onAuthChanged(e) {
  currentUser.value = e.detail
}

onMounted(() => {
  currentUser.value = getCurrentUser()
  window.addEventListener('auth-changed', onAuthChanged)
  window.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', onAuthChanged)
  window.removeEventListener('click', handleClickOutside)
})

function handleLogout() {
  logout()
  router.push('/login')
  showUserDropdown.value = false
}

function goToLoginPage() {
  router.push('/login')
}

function handleLoginAdmin() {
  const defaultAdmin = {
    id: 1,
    maNhanVien: 'NV001',
    tenNhanVien: 'Nguyễn Văn Admin',
    tenTaiKhoan: 'admin',
    email: 'admin@ffshirt.com',
    soDienThoai: '0900000001',
    role: 'Quản trị viên',
    trangThai: 1
  }
  setCurrentUser(defaultAdmin)
  showUserDropdown.value = false
}
</script>

<template>
  <div v-if="route.path === '/login'">
    <router-view />
    <GlobalDialog />
  </div>

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
        <!-- 1. Tổng quan (Chỉ Quản trị viên) -->
        <router-link v-if="isUserAdmin" to="/thong-ke" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z"/>
            </svg>
          </span>
          <span class="menu-text">Thống kê</span>
        </router-link>

        <!-- 2. Bán Hàng Tại Quầy (Cả Quản trị viên và Nhân viên) -->
        <router-link to="/ban-hang" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z"/>
            </svg>
          </span>
          <span class="menu-text">Bán Hàng Tại Quầy</span>
        </router-link>

        <!-- 3. Quản Lý Hóa Đơn (Cả Quản trị viên và Nhân viên) -->
        <router-link to="/hoa-don" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm7 17H5V8h2v2c0 .55.45 1 1 1s1-.45 1-1V8h6v2c0 .55.45 1 1 1s1-.45 1-1V8h2v12z"/>
            </svg>
          </span>
          <span class="menu-text">Quản Lý Hóa Đơn</span>
        </router-link>

        <!-- 4. Quản Lý Sản Phẩm (Cả Quản trị viên và Nhân viên xem) -->
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

        <!-- 5. Quản lý thuộc tính (Chỉ Quản trị viên) -->
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

        <!-- 6. Quản lý khách hàng (Cả Quản trị viên và Nhân viên) -->
        <router-link to="/khach-hang" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </span>
          <span class="menu-text">Quản lý khách hàng</span>
        </router-link>

        <router-link to="/chat" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M20 2H4C2.9 2 2 2.9 2 4v14c0 1.1.9 2 2 2h3v3l5-3h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 14H5v-2h14v2zm0-4H5v-2h14v2zm0-4H5V6h14v2z"/>
            </svg>
          </span>
          <span class="menu-text">Hỗ trợ trực tuyến</span>
        </router-link>

        <!-- 7. Quản lý nhân viên (Chỉ Quản trị viên) -->
        <router-link v-if="isUserAdmin" to="/nhan-vien" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </span>
          <span class="menu-text">Quản lý nhân viên</span>
        </router-link>

        <!-- 8. Quản Lý Giảm Giá (Chỉ Quản trị viên) -->
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

        <!-- 9. Lịch Làm Việc (Cả Quản trị viên và Nhân viên) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('lichLamViec')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z"/>
              </svg>
            </span>
            <span class="menu-text">Lịch Làm Việc</span>
            <span class="arrow-icon" :class="{ open: openMenus.lichLamViec }">⌄</span>
          </div>
          <div class="submenu-tree" v-show="openMenus.lichLamViec">
            <!-- Quản lý ca làm việc (Chỉ Quản trị viên) -->
            <router-link v-if="isUserAdmin" to="/ca-lam-viec" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Quản lý ca làm việc</span>
            </router-link>
            <!-- Xếp lịch nhân viên (Cả Quản trị viên và Nhân viên xem) -->
            <router-link to="/xep-lich" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Xếp lịch nhân viên</span>
            </router-link>
            <!-- Vào ca & Giao ca (Cả Quản trị viên và Nhân viên) -->
            <router-link to="/giao-ca" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Vào ca & Giao ca</span>
            </router-link>
            <!-- Lịch sử hoạt động (Cả Quản trị viên và Nhân viên) -->
            <router-link to="/lich-su-ca" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Lịch sử hoạt động</span>
            </router-link>
          </div>
        </div>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-right">
          <!-- Người dùng đang đăng nhập -->
          <div v-if="currentUser" ref="userMenuRef" class="user-profile-menu">
            <div class="admin-badge" @click="showUserDropdown = !showUserDropdown" title="Tài khoản đang đăng nhập">
              <div class="admin-avatar">
                <span>{{ getAvatarInitials(currentUser.tenNhanVien || currentUser.tenTaiKhoan) }}</span>
              </div>
              <div class="admin-info-text">
                <span class="admin-name">{{ currentUser.tenNhanVien || currentUser.tenTaiKhoan || (isUserAdmin ? 'Quản trị viên' : 'Nhân viên') }}</span>
                <span class="admin-role">{{ currentUser.role || (isUserAdmin ? 'Quản trị viên' : 'Nhân viên') }}</span>
              </div>
              <span class="dropdown-caret" :class="{ open: showUserDropdown }">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </div>

            <!-- Menu dropdown cá nhân -->
            <div v-if="showUserDropdown" class="user-dropdown-popover">
              <div class="popover-header">
                <div class="popover-avatar">
                  <span>{{ getAvatarInitials(currentUser.tenNhanVien || currentUser.tenTaiKhoan) }}</span>
                </div>
                <div class="popover-header-info">
                  <div class="popover-name" :title="currentUser.tenNhanVien || currentUser.tenTaiKhoan">
                    {{ currentUser.tenNhanVien || currentUser.tenTaiKhoan }}
                  </div>
                  <span class="popover-role-tag" :class="{ 'role-admin': isUserAdmin }">
                    {{ currentUser.role || (isUserAdmin ? 'Quản trị viên' : 'Nhân viên') }}
                  </span>
                </div>
              </div>

              <div class="popover-details">
                <div class="popover-detail-row">
                  <span class="popover-detail-label">Mã NV</span>
                  <span class="popover-detail-val code-val">{{ currentUser.maNhanVien || '-' }}</span>
                </div>
                <div class="popover-detail-row" v-if="currentUser.email">
                  <span class="popover-detail-label">Email</span>
                  <span class="popover-detail-val text-truncate" :title="currentUser.email">{{ currentUser.email }}</span>
                </div>
                <div class="popover-detail-row" v-if="currentUser.soDienThoai">
                  <span class="popover-detail-label">SĐT</span>
                  <span class="popover-detail-val">{{ currentUser.soDienThoai }}</span>
                </div>
              </div>

              <div class="popover-footer">
                <button type="button" class="btn-popover-logout" @click="handleLogout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                    <polyline points="16 17 21 12 16 7" />
                    <line x1="21" y1="12" x2="9" y2="12" />
                  </svg>
                  Đăng xuất tài khoản
                </button>
              </div>
            </div>
          </div>

          <!-- Trạng thái chưa đăng nhập / đã đăng xuất -->
          <div v-else class="logged-out-header-box">
            <button class="btn-re-login" @click="handleLoginAdmin">Đăng nhập Quản trị viên</button>
          </div>
        </div>
      </header>

      <div class="page-body">
        <router-view />
      </div>
    </main>

    <!-- Overlay khi bị đăng xuất hoặc tài khoản bị khóa -->
    <div v-if="!currentUser" class="logged-out-modal-overlay">
      <div class="logged-out-modal-card">
        <div class="logged-out-icon-wrap">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
        <h3 class="logged-out-title">Bạn đã đăng xuất khỏi hệ thống</h3>
        <p class="logged-out-desc">
          Tài khoản của bạn đã được chuyển sang trạng thái ngừng hoạt động hoặc phiên làm việc đã kết thúc.
          Vui lòng đăng nhập lại để tiếp tục sử dụng hệ thống!
        </p>
        <div class="logged-out-actions">
          <button class="btn-relogin-action" @click="handleLoginAdmin">
            Đăng nhập lại ngay (Quản trị viên)
          </button>
          <button class="btn-goto-login-action" @click="goToLoginPage">
            Đến màn hình đăng nhập
          </button>
        </div>
      </div>
    </div>

    <GlobalDialog />
    <AdminAiChat />
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
  height: 96px;
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

/* 3 tia sáng phát ra góc trên bên trái */
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

.logo-name {
  display: flex;
  font-size: 52px;
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
  font-size: 14px;
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
  border-radius: 40px;
  background: #ffffff;
  border: 1px solid #dfd8cb;
  box-shadow: 0 2px 8px rgba(90, 70, 50, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.admin-badge:hover {
  border-color: #8c7355;
  box-shadow: 0 4px 14px rgba(140, 115, 85, 0.16);
  transform: translateY(-1px);
}

.admin-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8c7355, #634b35);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 6px rgba(99, 75, 53, 0.25);
  flex-shrink: 0;
}

.admin-info-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.admin-name {
  font-size: 0.88rem !important;
  font-weight: 700;
  color: #2c3e50;
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-role {
  font-size: 0.72rem;
  color: #8c7355;
  font-weight: 600;
}

.dropdown-caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #8c7355;
  transition: transform 0.2s ease;
  margin-left: 2px;
}

.dropdown-caret.open {
  transform: rotate(180deg);
}

.user-dropdown-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 260px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #ebd9c8;
  box-shadow: 0 12px 32px rgba(90, 70, 50, 0.14);
  padding: 16px;
  z-index: 1000;
  animation: dropdownIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dropdownIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.popover-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2ece3;
}

.popover-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8c7355, #634b35);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
  box-shadow: 0 3px 8px rgba(99, 75, 53, 0.25);
  flex-shrink: 0;
}

.popover-header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
  flex: 1;
}

.popover-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.popover-role-tag {
  display: inline-block;
  align-self: flex-start;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 9999px;
  background: #f7f3ee;
  color: #8c7355;
  border: 1px solid #ebd9c8;
}

.popover-role-tag.role-admin {
  background: #fef7ee;
  color: #b45309;
  border-color: #fde68a;
}

.popover-details {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.popover-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.82rem;
  gap: 8px;
}

.popover-detail-label {
  color: #8a7b6e;
  font-weight: 500;
  flex-shrink: 0;
}

.popover-detail-val {
  color: #2c3e50;
  font-weight: 600;
}

.popover-detail-val.code-val {
  background: #f5efe6;
  color: #634b35;
  padding: 1px 7px;
  border-radius: 5px;
  font-family: monospace;
  font-size: 0.8rem;
  border: 1px solid #e5dacd;
}

.popover-detail-val.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
}

.popover-footer {
  padding-top: 10px;
  border-top: 1px solid #f2ece3;
}

.btn-popover-logout {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #fff5f5;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-weight: 600;
  font-size: 0.86rem;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-popover-logout:hover {
  background: #dc2626;
  border-color: #dc2626;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.logged-out-header-box {
  display: flex;
  align-items: center;
}

.btn-re-login {
  background: #8c7355;
  color: #ffffff;
  border: none;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  box-shadow: 0 2px 8px rgba(140, 115, 85, 0.25);
}

.btn-re-login:hover {
  background: #755f45;
}

/* ================= ĐỒNG BỘ FORM CONTROLS & COMBOBOX TOÀN DỰ ÁN ================= */
button, input, select, textarea {
  font-family: var(--system-font, 'Inter', 'Roboto', 'Be Vietnam Pro', system-ui, sans-serif) !important;
}

.select-wrapper-minimal {
  position: relative;
  width: 100%;
  display: inline-block;
}

.select-wrapper-minimal::after {
  content: "";
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 4.5px solid transparent;
  border-right: 4.5px solid transparent;
  border-top: 5px solid #8c7355;
  pointer-events: none;
}

.select-wrapper-minimal select.form-control-minimal,
.select-wrapper-minimal select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 32px !important;
  cursor: pointer;
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

.logged-out-actions {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.btn-goto-login-action {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
  font-size: 0.92rem;
  font-weight: 600;
  padding: 0.7rem 1.4rem;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-goto-login-action:hover {
  background: #e2e8f0;
  color: #0f172a;
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
</style>