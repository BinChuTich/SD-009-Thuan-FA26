<template>
  <footer class="client-footer">
    <!-- Thông tin liên hệ và chính sách (Đã bỏ dải 4 cam kết bị trùng bên trên) -->
    <div class="footer-main">
      <div class="container-client main-grid">
        <!-- Cột 1: Logo & Giới thiệu -->
        <div class="footer-col brand-col">
          <div class="footer-logo">
            <div class="logo-wrapper">
              <div class="sparkles mini">
                <span class="ray ray-1"></span>
                <span class="ray ray-2"></span>
                <span class="ray ray-3"></span>
              </div>
              <div class="logo-content">
                <div class="logo-name">
                  <span class="f-white">F</span>
                  <span class="f-gold">F</span>
                </div>
                <div class="logo-sub">T-shirt</div>
              </div>
            </div>
          </div>

          <div class="contact-line">
            <span class="contact-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                <circle cx="12" cy="10" r="3"></circle>
              </svg>
            </span>
            <span>Trụ sở: Tòa nhà FPT Polytechnic, Phố Trịnh Văn Bô, Hà Nội</span>
          </div>
          <div class="contact-line">
            <span class="contact-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                <polyline points="22,6 12,13 2,6"></polyline>
              </svg>
            </span>
            <span>Email: <a href="mailto:fftshirt.vn@gmail.com" class="footer-email-link">fftshirt.vn@gmail.com</a></span>
          </div>
          <div class="contact-line">
            <span class="contact-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
              </svg>
            </span>
            <span>Hotline CSKH: <strong>1900 6677</strong></span>
          </div>
        </div>

        <!-- Cột 2: Khám phá -->
        <div class="footer-col">
          <h4 class="col-title">BỘ SƯU TẬP</h4>
          <ul class="footer-links">
            <li><router-link to="/cua-hang">Tất cả sản phẩm</router-link></li>
            <li><router-link to="/cua-hang?danhmuc=basic">Áo Thun Basic</router-link></li>
            <li><router-link to="/cua-hang?danhmuc=polo">Áo Thun Polo</router-link></li>
            <li><router-link to="/cua-hang?danhmuc=oversize">Áo Thun Oversize</router-link></li>
          </ul>
        </div>

        <!-- Cột 3: Hỗ trợ khách hàng -->
        <div class="footer-col">
          <h4 class="col-title">HỖ TRỢ KHÁCH HÀNG</h4>
          <ul class="footer-links">
            <li><router-link to="/tra-cuu">Tra cứu đơn hàng</router-link></li>
                        <li><router-link to="/uu-dai">Chương trình ưu đãi</router-link></li>
            <li><router-link to="/lien-he">Liên hệ hỗ trợ</router-link></li>
          </ul>
        </div>

      </div>
    </div>

    <!-- Dòng bản quyền -->
    <div class="footer-bottom">
      <div class="container-client bottom-inner">
        <span>© 2026 FF T-Shirt. Dự Án Tốt Nghiệp SD-009. Bản quyền thuộc về Nhóm SD-009.</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.client-footer {
  background-color: #1e293b;
  color: #94a3b8;
  font-size: 0.88rem;
  margin-top: auto;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  border-top: 3px solid #d2a764;
}

.container-client {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

@media (min-width: 1600px) {
  .container-client {
    max-width: 1520px;
    padding: 0 2rem;
  }
}

@media (max-width: 640px) {
  .container-client {
    padding: 0 1rem;
  }
}

/* Main */
.footer-main {
  padding: 44px 0 32px;
}

.main-grid {
  display: grid;
  grid-template-columns: 1.8fr 1.1fr 1.1fr;
  gap: 36px;
}

/* Logo chuẩn thương hiệu trong footer */
.footer-logo {
  margin-bottom: 14px;
}

.footer-logo .logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', sans-serif;
  user-select: none;
}

.footer-logo .sparkles.mini {
  position: absolute;
  top: -5px;
  left: -6px;
  width: 20px;
  height: 20px;
}

.footer-logo .sparkles.mini .ray {
  position: absolute;
  background-color: #d2a764;
  border-radius: 2px;
}

.footer-logo .sparkles.mini .ray-1 {
  width: 3px;
  height: 8px;
  top: 0;
  right: 4px;
  transform: rotate(10deg);
}

.footer-logo .sparkles.mini .ray-2 {
  width: 3px;
  height: 8px;
  top: 3px;
  left: 2px;
  transform: rotate(-45deg);
}

.footer-logo .sparkles.mini .ray-3 {
  width: 3px;
  height: 8px;
  bottom: 0px;
  left: -2px;
  transform: rotate(-80deg);
}

.footer-logo .logo-content {
  display: flex;
  align-items: flex-end;
}

.footer-logo .logo-name {
  display: flex;
  font-size: 34px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -1.5px;
}

.footer-logo .f-white {
  color: #ffffff;
}

.footer-logo .f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.footer-logo .logo-sub {
  color: #d2a764;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  margin-left: 6px;
  margin-bottom: 1px;
}

.brand-desc {
  font-size: 0.84rem;
  line-height: 1.6;
  color: #cbd5e1;
  margin-bottom: 14px;
}

.contact-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  margin-bottom: 7px;
  color: #94a3b8;
}

.contact-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #d2a764;
  flex-shrink: 0;
}

.footer-email-link {
  color: #d2a764;
  text-decoration: none;
  font-weight: 600;
  transition: opacity 0.2s;
}

.footer-email-link:hover {
  text-decoration: underline;
  opacity: 0.9;
}

.contact-line strong {
  color: #f8fafc;
}

.col-title {
  font-size: 0.84rem;
  font-weight: 700;
  color: #f8fafc;
  margin: 0 0 16px;
  letter-spacing: 0.6px;
}

.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-links a {
  color: #94a3b8;
  text-decoration: none;
  font-size: 0.84rem;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: #d2a764;
}



/* Bottom */
.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 0;
  text-align: center;
  font-size: 0.76rem;
  color: #64748b;
}

@media (max-width: 900px) {
  .main-grid {
    grid-template-columns: 1fr;
    gap: 28px;
  }
}
</style>
