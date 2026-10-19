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
                  <span class="f-blue">F</span>
                  <span class="f-gold">F</span>
                </div>
                <div class="logo-sub">T-shirt</div>
              </div>
            </div>
          </div>
          <p class="brand-desc">
            Thương hiệu thời trang áo phông nam cao cấp. Mang đến phong cách trẻ trung, đĩnh đạc và trải nghiệm vải bông Cotton tự nhiên đỉnh cao.
          </p>
          <div class="contact-line">📍 Trụ sở: Tòa nhà FPT Polytechnic, Phố Trịnh Văn Bô, Hà Nội</div>
          <div class="contact-line">📧 Email: contact@fftshirt.vn</div>
          <div class="contact-line">📞 Hotline CSKH: <strong>1900 6868</strong></div>
        </div>

        <!-- Cột 2: Khám phá -->
        <div class="footer-col">
          <h4 class="col-title">BỘ SƯU TẬP</h4>
          <ul class="footer-links">
            <li><router-link to="/cua-hang">Tất cả sản phẩm</router-link></li>
            <li><router-link to="/cua-hang?danhmuc=basic">Áo Thun Basic</router-link></li>
            <li><router-link to="/cua-hang?danhmuc=polo">Áo Thun Polo</router-link></li>
            <li><router-link to="/goi-y">Gợi ý phối đồ</router-link></li>
          </ul>
        </div>

        <!-- Cột 3: Hỗ trợ khách hàng -->
        <div class="footer-col">
          <h4 class="col-title">HỖ TRỢ KHÁCH HÀNG</h4>
          <ul class="footer-links">
            <li><router-link to="/tra-cuu-don-hang">Tra cứu đơn hàng</router-link></li>
            <li><router-link to="/don-hang">Đơn hàng của tôi</router-link></li>
            <li><router-link to="/gioi-thieu">Về thương hiệu FF</router-link></li>
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
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.5rem;
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
.logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', sans-serif;
  user-select: none;
  margin-bottom: 12px;
}

.sparkles.mini {
  position: absolute;
  top: -5px;
  left: -6px;
  width: 20px;
  height: 20px;
}

.sparkles.mini .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 2px;
}

.sparkles.mini .ray-1 {
  width: 3px;
  height: 7px;
  top: 0;
  right: 4px;
  transform: rotate(10deg);
}

.sparkles.mini .ray-2 {
  width: 3px;
  height: 7px;
  top: 3px;
  left: 2px;
  transform: rotate(-45deg);
}

.sparkles.mini .ray-3 {
  width: 3px;
  height: 7px;
  bottom: 0px;
  left: -2px;
  transform: rotate(-80deg);
}

.logo-content {
  display: flex;
  align-items: flex-end;
}

.logo-name {
  display: flex;
  font-size: 32px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -1.5px;
}

.f-blue {
  color: #93c5fd;
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.logo-sub {
  color: #93c5fd;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  margin-left: 5px;
  margin-bottom: 1px;
}

.brand-desc {
  font-size: 0.84rem;
  line-height: 1.6;
  color: #cbd5e1;
  margin-bottom: 14px;
}

.contact-line {
  font-size: 0.8rem;
  margin-bottom: 6px;
  color: #94a3b8;
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
