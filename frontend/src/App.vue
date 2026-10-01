<script setup>
import { ref } from 'vue'
import { reactive } from 'vue'
const openMenus = ref({
  sanPham: true,
  thuocTinh: false,
  giamGia: false,
  hoaDon: true
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

        <!-- Quản lý hóa đơn (Có nhánh con: Danh sách & Chi tiết hóa đơn) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('hoaDon')">
            <span class="menu-icon">▤</span>
            <span class="menu-text">Quản lý hóa đơn</span>
            <span class="arrow-icon" :class="{ open: openMenus.hoaDon }">⌄</span>
          </div>
          <div class="submenu-tree" v-show="openMenus.hoaDon">
            <router-link to="/hoa-don" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Danh sách hóa đơn</span>
            </router-link>
            <router-link to="/hoa-don/chi-tiet" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Chi tiết hóa đơn</span>
            </router-link>
          </div>
        </div>

        <!-- Quản lý sản phẩm (Nhánh cây con) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('sanPham')">
            <span class="menu-icon">◈</span>
            <span class="menu-text">Quản lý sản phẩm</span>
            <span class="arrow-icon" :class="{ open: openMenus.sanPham }">⌄</span>
          </div>
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

        <!-- Thuộc tính sản phẩm (Nhánh cây con) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('thuocTinh')">
            <span class="menu-icon">◇</span>
            <span class="menu-text">Thuộc tính sản phẩm</span>
            <span class="arrow-icon" :class="{ open: openMenus.thuocTinh }">⌄</span>
          </div>
          <div class="submenu-tree" v-show="openMenus.thuocTinh">
            <router-link to="/danh-muc" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Danh mục</span>
            </router-link>
            <router-link to="/thuong-hieu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Thương hiệu</span>
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

<script setup lang="ts">
</script>

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

/* Sidebar toàn bộ là nền trắng cố định 155px */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 200px; /* Chiều rộng thanh menu */
  background-color: #ffffff;
  border-right: 1px solid #e9e5db;
  z-index: 100;
  display: flex;
  flex-direction: column;
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

/* 3 tia sáng */
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

/* Cụm FF và T-shirt vừa vặn cột 155px */
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

/* Menu điều hướng nền trắng */
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
  gap: 9px;
  border-radius: 7px;
  color: #647074;
  font-size: 9px;
  margin-bottom: 3px;
  cursor: pointer;
  text-decoration: none;
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease;
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

/* Nội dung chính thụt vào 155px tránh bị đè */
.main-content {
  margin-left: 200px; /* Phải bằng width của .sidebar */
  width: calc(100% - 200px);
  min-height: 100vh;
  background-color: #f7f5ef;
  display: flex;
  flex-direction: column;
}

.top-header {
  height: 48px;
  background-color: #ffffff;
  padding: 0 24px;
  display: flex;
  justify-content: flex-end; /* Đẩy cụm Quản trị viên sang góc phải */
  align-items: center;
  border-bottom: 1px solid #e9e5db;
}

.header-title {
  font-size: 11px;
  font-weight: 700;
  color: #4b5b62;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 8px;
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