<script setup>
import { ref } from 'vue'
const openMenus = ref({
  sanPham: true,
  thuocTinh: true,
  giamGia: false
})

const toggleMenu = (menu) => {
  openMenus.value[menu] = !openMenus.value[menu]
}
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

        <!-- 5. Thuộc tính (Dropdown menu con) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('thuocTinh')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"/>
              </svg>
            </span>
            <span class="menu-text">Thuộc tính</span>
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

        <!-- 6. Quản lý khách hàng (2 người) -->
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
      <header class="top-header">
        <div class="header-right">
          <span>Quản trị viên</span>
          <span class="divider">|</span>
          <a href="#">Xem website</a>
        </div>
      </header>

      <div class="page-body">
        <router-view />
      </div>
    </main>
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
  height: 3.2rem !important;
  background-color: #ffffff;
  padding: 0 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: 1px solid #e9e5db;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 0.85rem !important;
  color: #6c7477;
}

.header-right a {
  color: #496883;
  font-weight: 700;
  text-decoration: none;
}

.divider {
  color: #d6d0c3;
}

.page-body {
  flex: 1;
  width: 100%;
}
</style>