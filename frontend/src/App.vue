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
        <router-link to="/" class="menu-item active">
          <span class="menu-icon">▦</span> Tổng quan
        </router-link>
        <div class="menu-item"><span class="menu-icon">▣</span> Bán hàng tại quầy</div>
        <div class="menu-item"><span class="menu-icon">▤</span> Quản lý đơn hàng</div>
        <router-link to="/hoa-don" class="menu-item">
          <span class="menu-icon">▧</span> Hóa đơn
        </router-link>
        <div class="menu-item"><span class="menu-icon">↩</span> Trả hàng</div>
        <div class="menu-item"><span class="menu-icon">▧</span> Quản lý sản phẩm</div>
        <div class="menu-item"><span class="menu-icon">♙</span> Quản lý tài khoản</div>
        <div class="menu-item"><span class="menu-icon">%</span> Giảm giá</div>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-title">Tổng quan</div>
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
@import url('https://fonts.googleapis.com/css2?family=Fredoka:wght@600;700&family=Nunito:wght@700;800;900&display=swap');

/* Layout tổng thể */
.app-shell {
  display: flex;
  min-height: 100vh;
  background-color: #f7f9fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

/* Sidebar */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 155px;
  background-color: #ffffff; /* Nền trắng toàn bộ cột */
  border-right: 1px solid var(--line);
  z-index: 5;
  display: flex;
  flex-direction: column;
}
/* Khu vực Logo */
.logo-box {
  background-color: #ffffff; /* Nền trắng phía sau logo */
  padding: 24px 16px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-bottom: 2px solid #ecd8af; /* Đường kẻ viền vàng be bên dưới như trong ảnh */
  user-select: none;
}

.logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', 'Nunito', sans-serif;
}

/* 3 tia sáng */
.sparkles {
  position: absolute;
  top: -6px;
  left: -8px;
  width: 28px;
  height: 28px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 4px;
}

.sparkles .ray-1 {
  width: 4px;
  height: 10px;
  top: 0px;
  right: 5px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 4px;
  height: 10px;
  top: 5px;
  left: 5px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 4px;
  height: 10px;
  bottom: 0px;
  left: -3px;
  transform: rotate(-80deg);
}

/* Cụm FF và T-shirt */
.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.07));
}

.logo-name {
  display: flex;
  font-size: 56px;
  font-weight: 700;
  line-height: 0.82;
  letter-spacing: -2px;
}

.f-blue {
  color: #496883; /* Màu xanh đá đặc trưng */
}

.f-gold {
  color: #d2a764; /* Màu vàng cát đặc trưng */
  margin-left: 2px;
}

.logo-sub {
  color: #496883;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  margin-left: 3px;
  margin-bottom: 2px;
  letter-spacing: -0.2px;
}

/* Menu điều hướng */
/* Phần menu trên nền trắng */
.menu {
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
  transition: background 0.2s ease, color 0.2s ease;
}

.menu-item:hover {
  background: #f7f5ef;
  color: var(--blue);
}

.menu-item.active {
  background: #eaf1f4;
  color: var(--blue);
  font-weight: 700;
}

.menu-icon {
  width: 15px;
  text-align: center;
  color: var(--blue);
  font-size: 11px;
}

/* Nội dung chính */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.top-header {
  background-color: #ffffff;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e9ecef;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #6c757d;
}

.header-right a {
  color: #0d6efd;
  text-decoration: none;
}

.header-right a:hover {
  text-decoration: underline;
}

.page-body {
  padding: 24px;
}

</style>