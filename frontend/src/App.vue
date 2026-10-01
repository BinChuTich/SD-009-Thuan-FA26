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
        <!-- Thống kê -->
        <router-link to="/thong-ke" class="menu-item" active-class="active">
          <span class="menu-icon">▦</span>
          <span class="menu-text">Thống kê</span>
        </router-link>

        <!-- Bán hàng tại quầy -->
        <router-link to="/ban-hang" class="menu-item" active-class="active">
          <span class="menu-icon">▣</span>
          <span class="menu-text">Bán hàng tại quầy</span>
        </router-link>

        <!-- Quản lý hóa đơn dạng link đơn trực tiếp -->
        <router-link to="/hoa-don" class="menu-item" active-class="active">
          <span class="menu-icon">▤</span>
          <span class="menu-text">Quản lý hóa đơn</span>
        </router-link>

        <!-- Quản lý sản phẩm (Đã gộp Thuộc tính sản phẩm vào bên trong) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('sanPham')">
            <span class="menu-icon">◈</span>
            <span class="menu-text">Quản lý sản phẩm</span>
            <span class="arrow-icon" :class="{ open: openMenus.sanPham }">⌄</span>
          </div>

          <!-- Cấp 2: Các mục con của Quản lý sản phẩm -->
          <div class="submenu-tree" v-show="openMenus.sanPham">
            <router-link to="/san-pham" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Sản phẩm</span>
            </router-link>

            <router-link to="/bien-the-san-pham" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Biến thể sản phẩm</span>
            </router-link>

            <!-- Nhánh con: Thuộc tính sản phẩm -->
            <div class="nested-tree-group">
              <div class="tree-item has-sub-nested" @click="toggleMenu('thuocTinh')">
                <span class="tree-branch">└─</span>
                <span class="nested-title">Thuộc tính sản phẩm</span>
                <span class="arrow-icon-sub" :class="{ open: openMenus.thuocTinh }">⌄</span>
              </div>

              <!-- Cấp 3: Danh sách các thuộc tính -->
              <div class="submenu-sub-tree" v-show="openMenus.thuocTinh">
                <router-link to="/danh-muc" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Danh mục</span>
                </router-link>
                <router-link to="/thuong-hieu" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Thương hiệu</span>
                </router-link>
                <router-link to="/chat-lieu" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Chất liệu</span>
                </router-link>
                <router-link to="/xuat-xu" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Xuất xứ</span>
                </router-link>
                <router-link to="/co-ao" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Cổ áo</span>
                </router-link>
                <router-link to="/tay-ao" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Tay áo</span>
                </router-link>
                <router-link to="/mau-sac" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">├─</span>
                  <span>Màu sắc</span>
                </router-link>
                <router-link to="/kich-co" class="tree-sub-item" active-class="active">
                  <span class="tree-branch">└─</span>
                  <span>Kích cỡ</span>
                </router-link>
              </div>
            </div>
          </div>
        </div>

        <!-- Quản lý giảm giá (Nhánh cây con) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('giamGia')">
            <span class="menu-icon">%</span>
            <span class="menu-text">Quản lý giảm giá</span>
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

        <!-- Quản lý khách hàng -->
        <router-link to="/khach-hang" class="menu-item" active-class="active">
          <span class="menu-icon">♟</span>
          <span class="menu-text">Quản lý khách hàng</span>
        </router-link>

        <!-- Quản lý nhân viên -->
        <router-link to="/nhan-vien" class="menu-item" active-class="active">
          <span class="menu-icon">♟</span>
          <span class="menu-text">Quản lý nhân viên</span>
        </router-link>
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

/* Khu vực Logo */
.logo-box {
  background-color: #ffffff;
  height: 76px;
  padding: 10px 8px;
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

.sparkles {
  position: absolute;
  top: -4px;
  left: -6px;
  width: 22px;
  height: 22px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 3px;
}

.sparkles .ray-1 {
  width: 3px;
  height: 8px;
  top: 0;
  right: 4px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 3px;
  height: 8px;
  top: 4px;
  left: 3px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 3px;
  height: 8px;
  bottom: 1px;
  left: -2px;
  transform: rotate(-80deg);
}

.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 3px 5px rgba(73, 104, 131, 0.12));
}

.logo-name {
  display: flex;
  font-size: 38px;
  font-weight: 700;
  line-height: 0.85;
  letter-spacing: -1.5px;
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
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  margin-left: 3px;
  margin-bottom: 2px;
  letter-spacing: -0.2px;
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