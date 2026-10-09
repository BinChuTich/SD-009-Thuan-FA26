<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { dialogState, handleConfirmChoice, handleAlertClose, removeToast } from './utils/dialog.js'

const route = useRoute()

const openMenus = ref({
  sanPham: true,
  thuocTinh: true,
  giamGia: false
})

const toggleMenu = (menu) => {
  openMenus.value[menu] = !openMenus.value[menu]
}

const pageTitle = computed(() => {
  if (route.path === '/khach-hang') return 'Quản lý khách hàng'
  if (route.path === '/nhan-vien') return 'Quản lý nhân viên'
  if (route.path === '/thong-ke' || route.path === '/') return 'Tổng quan'
  if (route.path === '/ban-hang') return 'Bán hàng tại quầy'
  if (route.path === '/hoa-don') return 'Quản lý hóa đơn'
  if (route.path === '/san-pham') return 'Quản lý sản phẩm'
  if (route.path === '/bien-the-san-pham') return 'Biến thể sản phẩm'
  if (route.path === '/dot-giam-gia') return 'Đợt giảm giá'
  if (route.path === '/phieu-giam-gia') return 'Phiếu giảm giá'
  return ''
})
</script>

<template>
  <div class="app-shell">
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
        <!-- 1. Tổng quan -->
        <router-link to="/thong-ke" class="menu-item" active-class="active">
    <span class="menu-icon">
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z"/>
      </svg>
    </span>
          <span class="menu-text">Tổng quan</span>
        </router-link>

        <!-- 2. Bán Hàng Tại Quầy -->
        <router-link to="/ban-hang" class="menu-item" active-class="active">
    <span class="menu-icon">
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z"/>
      </svg>
    </span>
          <span class="menu-text">Bán Hàng Tại Quầy</span>
        </router-link>

        <!-- 3. Quản Lý Hóa Đơn (Link trực tiếp dạng túi xách) -->
        <router-link to="/hoa-don" class="menu-item" active-class="active">
    <span class="menu-icon">
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm7 17H5V8h2v2c0 .55.45 1 1 1s1-.45 1-1V8h6v2c0 .55.45 1 1 1s1-.45 1-1V8h2v12z"/>
      </svg>
    </span>
          <span class="menu-text">Quản Lý Hóa Đơn</span>
        </router-link>

        <!-- 4. Quản Lý Sản Phẩm (Hộp đóng hàng có nắp) -->
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

        <!-- 5. Quản lý khách hàng (2 người) -->
        <router-link to="/khach-hang" class="menu-item" active-class="active">
    <span class="menu-icon">
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
      </svg>
    </span>
          <span class="menu-text">Quản lý khách hàng</span>
        </router-link>

        <!-- 6. Quản lý nhân viên (2 người) -->
        <router-link to="/nhan-vien" class="menu-item" active-class="active">
    <span class="menu-icon">
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
      </svg>
    </span>
          <span class="menu-text">Quản lý nhân viên</span>
        </router-link>

        <!-- 7. Quản Lý Giảm Giá (Tag giảm giá) -->
        <div class="menu-group">
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
      <header v-if="pageTitle" class="top-header">
        <div class="header-left">
          <h2 class="header-page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <div class="user-profile-badge">
            <div class="user-avatar-circle" title="Tài khoản: Quản trị viên">
              <span class="user-initials">AD</span>
            </div>
            <div class="user-info-text">
              <span class="user-name">Quản trị viên</span>
              <span class="user-role-label">Hệ thống</span>
            </div>
          </div>
        </div>
      </header>

      <div class="page-body">
        <router-view />
      </div>
    </main>

    <!-- Hộp thoại xác nhận & Cảnh báo Validate ở CHÍNH GIỮA TRANG -->
    <Teleport to="body">
      <!-- Modal Xác nhận (Confirm) -->
      <transition name="dialog-fade">
        <div v-if="dialogState.confirm.show" class="center-dialog-backdrop" @click.self="handleConfirmChoice(false)">
          <div class="center-dialog-card animate-pop">
            <div class="dialog-icon-wrap" :class="'icon-' + (dialogState.confirm.type || 'question')">
              <svg v-if="dialogState.confirm.type === 'danger'" viewBox="0 0 24 24" width="34" height="34" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
              </svg>
              <svg v-else-if="dialogState.confirm.type === 'warning'" viewBox="0 0 24 24" width="34" height="34" fill="currentColor">
                <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="34" height="34" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 16h-2v-2h2v2zm1.07-7.75l-.9.92C12.45 11.9 12 12.5 12 14h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H7c0-2.76 2.24-5 5-5s5 2.24 5 5c0 1.04-.42 1.99-1.07 2.75z"/>
              </svg>
            </div>
            <h3 class="dialog-title">{{ dialogState.confirm.title }}</h3>
            <p class="dialog-message">{{ dialogState.confirm.message }}</p>
            <div class="dialog-actions">
              <button class="dialog-btn dialog-btn-cancel" @click="handleConfirmChoice(false)">
                {{ dialogState.confirm.cancelText || 'Hủy bỏ' }}
              </button>
              <button class="dialog-btn dialog-btn-confirm" :class="'btn-' + (dialogState.confirm.type || 'primary')" @click="handleConfirmChoice(true)">
                {{ dialogState.confirm.confirmText || 'Xác nhận' }}
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- Modal Cảnh báo Validate (Alert) ở CHÍNH GIỮA TRANG -->
      <transition name="dialog-fade">
        <div v-if="dialogState.alert.show" class="center-dialog-backdrop" @click.self="handleAlertClose">
          <div class="center-dialog-card animate-pop">
            <div class="dialog-icon-wrap" :class="'icon-' + (dialogState.alert.type || 'warning')">
              <svg v-if="dialogState.alert.type === 'error'" viewBox="0 0 24 24" width="34" height="34" fill="currentColor">
                <path d="M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/>
              </svg>
              <svg v-else-if="dialogState.alert.type === 'success'" viewBox="0 0 24 24" width="34" height="34" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="34" height="34" fill="currentColor">
                <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
              </svg>
            </div>
            <h3 class="dialog-title">{{ dialogState.alert.title }}</h3>
            <p class="dialog-message">{{ dialogState.alert.message }}</p>
            <div class="dialog-actions single-action">
              <button class="dialog-btn dialog-btn-primary" @click="handleAlertClose">
                {{ dialogState.alert.btnText || 'Đã hiểu' }}
              </button>
            </div>
          </div>
        </div>
      </transition>

      <!-- Toast Container -->
      <div class="toast-container">
        <transition-group name="toast-slide">
          <div v-for="toast in dialogState.toasts" :key="toast.id" class="toast-item" :class="'toast-' + toast.type" @click="removeToast(toast.id)">
            <span class="toast-icon">
              <svg v-if="toast.type === 'success'" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z"/></svg>
              <svg v-else-if="toast.type === 'error'" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M11 17h2v-6h-2v6zm1-15C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zM11 9h2V7h-2v2z"/></svg>
            </span>
            <span class="toast-text">{{ toast.message }}</span>
            <button class="toast-close" @click.stop="removeToast(toast.id)">✕</button>
          </div>
        </transition-group>
      </div>
    </Teleport>
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

/* Nhánh lồng cấp 3 (Thuộc tính sản phẩm) */
.has-sub-nested {
  cursor: pointer;
  user-select: none;
}

.nested-title {
  flex: 1;
  white-space: nowrap;
}

.arrow-icon-sub {
  font-size: 8.5px;
  color: #8a9292;
  margin-left: auto;
  transition: transform 0.2s ease;
}

.arrow-icon-sub.open {
  transform: rotate(180deg);
}

.submenu-sub-tree {
  display: flex !important;
  flex-direction: column !important;
  padding-left: 14px;
  margin: 2px 0;
  width: 100%;
  box-sizing: border-box;
}

.tree-sub-item {
  display: flex !important;
  align-items: center;
  height: 26px;
  padding: 0 6px;
  font-size: 8.5px;
  color: #647074;
  text-decoration: none;
  border-radius: 4px;
  white-space: nowrap !important;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.15s ease;
}

.tree-sub-item:hover {
  background-color: #f7f5ef;
  color: #496883;
}

.tree-sub-item.active {
  background-color: #eaf1f4;
  color: #496883;
  font-weight: 700;
}

.tree-sub-item.active .tree-branch {
  color: #496883;
}

/* Nội dung chính */
.main-content {
  margin-left: 200px;
  width: calc(100% - 200px);
  min-height: 100vh;
  background-color: #f7f5ef;
  display: flex;
  flex-direction: column;
}

.top-header {
  background-color: #ffffff;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  padding: 22px 32px;
  min-height: 82px;
  margin: 20px 2.2rem 20px 2.2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(65, 60, 50, 0.04);
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-page-title {
  margin: 0;
  font-size: 1.65rem;
  font-weight: 800;
  color: #2e3e48;
  letter-spacing: -0.3px;
  line-height: 1.2;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-profile-badge {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 16px 6px 8px;
  border-radius: 36px;
  background-color: #faf8f5;
  border: 1px solid #e9e5db;
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-profile-badge:hover {
  background-color: #f1ede4;
  border-color: #496883;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.user-avatar-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #496883, #2f4557);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.3);
}

.user-info-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-weight: 700;
  color: #2e3e48;
  font-size: 0.98rem;
}

.user-role-label {
  font-size: 0.8rem;
  color: #7b888f;
  font-weight: 500;
}

.page-body {
  flex: 1;
  width: 100%;
}

/* =========================================================
   HỘP THOẠI XÁC NHẬN & CẢNH BÁO VALIDATE CHÍNH GIỮA TRANG
   ========================================================= */
.center-dialog-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(26, 36, 44, 0.55);
  backdrop-filter: blur(4px);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.center-dialog-card {
  background: #ffffff;
  width: 100%;
  max-width: 440px;
  border-radius: 18px;
  padding: 2rem 1.8rem 1.6rem;
  box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.3), 0 0 1px rgba(0, 0, 0, 0.2);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #efeae0;
}

.dialog-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.2rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.dialog-icon-wrap.icon-question {
  background-color: #eaf1f5;
  color: #496883;
}

.dialog-icon-wrap.icon-warning {
  background-color: #fef5e7;
  color: #d2a764;
}

.dialog-icon-wrap.icon-danger,
.dialog-icon-wrap.icon-error {
  background-color: #fdeeee;
  color: #d33c3c;
}

.dialog-icon-wrap.icon-success {
  background-color: #edf8ef;
  color: #438f55;
}

.dialog-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #2e3e48;
  margin: 0 0 0.65rem 0;
}

.dialog-message {
  font-size: 0.96rem;
  color: #617078;
  line-height: 1.55;
  margin: 0 0 1.6rem 0;
  white-space: pre-line;
}

.dialog-actions {
  display: flex;
  gap: 0.85rem;
  width: 100%;
}

.dialog-actions.single-action {
  justify-content: center;
}

.dialog-btn {
  flex: 1;
  height: 2.75rem;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.dialog-btn-cancel {
  background-color: #f3f0e8;
  color: #69767c;
  border: 1px solid #e2ddd0;
}

.dialog-btn-cancel:hover {
  background-color: #e9e3d8;
  color: #3b4950;
}

.dialog-btn-confirm.btn-primary,
.dialog-btn-primary {
  background-color: #496883;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(73, 104, 131, 0.3);
}

.dialog-btn-confirm.btn-primary:hover,
.dialog-btn-primary:hover {
  background-color: #38536b;
  box-shadow: 0 6px 18px rgba(73, 104, 131, 0.4);
}

.dialog-btn-confirm.btn-danger {
  background-color: #d33c3c;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(211, 60, 60, 0.3);
}

.dialog-btn-confirm.btn-danger:hover {
  background-color: #b82d2d;
}

.dialog-btn-confirm.btn-warning {
  background-color: #d2a764;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.3);
}

.dialog-btn-confirm.btn-warning:hover {
  background-color: #be924d;
}

/* Animations */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.25s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

.animate-pop {
  animation: dialogPop 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes dialogPop {
  0% {
    transform: scale(0.85);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* =========================================================
   TOAST NOTIFICATIONS
   ========================================================= */
.toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 100000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 290px;
  max-width: 440px;
  padding: 12px 18px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  border-left: 5px solid transparent;
  transition: transform 0.2s ease;
}

.toast-item:hover {
  transform: translateY(-2px);
}

.toast-item.toast-success {
  border-left-color: #438f55;
  color: #295734;
}
.toast-item.toast-success .toast-icon {
  color: #438f55;
}

.toast-item.toast-error {
  border-left-color: #d33c3c;
  color: #8c2525;
}
.toast-item.toast-error .toast-icon {
  color: #d33c3c;
}

.toast-item.toast-warning {
  border-left-color: #d2a764;
  color: #85612c;
}
.toast-item.toast-warning .toast-icon {
  color: #d2a764;
}

.toast-item.toast-info {
  border-left-color: #496883;
  color: #2d4559;
}
.toast-item.toast-info .toast-icon {
  color: #496883;
}

.toast-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.toast-text {
  flex: 1;
}

.toast-close {
  background: transparent;
  border: none;
  color: #98a2a6;
  font-size: 14px;
  cursor: pointer;
  padding: 0 4px;
}
.toast-close:hover {
  color: #3b4950;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.3s ease;
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>