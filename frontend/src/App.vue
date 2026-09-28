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
        <router-link to="/" class="menu-item" active-class="active">
          <span class="menu-icon">▦</span> Thống kê
        </router-link>

        <router-link to="/ban-hang" class="menu-item" active-class="active">
          <span class="menu-icon">▣</span> Bán hàng tại quầy
        </router-link>

        <!-- Nhấn vào Quản lý đơn hàng sẽ nhảy sang trang Hóa đơn -->
        <router-link to="/hoa-don" class="menu-item" active-class="active">
          <span class="menu-icon">▤</span> Quản lý đơn hàng
        </router-link>

        <div class="menu-item">
          <span class="menu-icon">↩</span> Trả hàng
        </div>

        <router-link to="/san-pham" class="menu-item" active-class="active">
          <span class="menu-icon">▧</span> Quản lý sản phẩm
        </router-link>

        <router-link to="/khach-hang" class="menu-item" active-class="active">
          <span class="menu-icon">♙</span> Quản lý tài khoản
        </router-link>

        <router-link to="/khuyen-mai" class="menu-item" active-class="active">
          <span class="menu-icon">%</span> Giảm giá
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-title">Hệ thống quản lý FF T-shirt</div>
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
  width: 155px;
  background-color: #ffffff;
  border-right: 1px solid #e9e5db;
  z-index: 5;
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
  margin-left: 155px;
  width: calc(100% - 155px);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-header {
  height: 48px;
  background-color: #ffffff;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
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
}
</style>