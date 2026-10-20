<template>
  <section class="tshirt-trust-bar">
    <div class="container-trust">
      <div class="trust-grid">
        <div class="trust-item">
          <div class="trust-icon-box">🧵</div>
          <div class="trust-text">
            <h4>100% Cotton Tự Nhiên</h4>
            <p>Định lượng cao 240-260 GSM mềm mịn</p>
          </div>
        </div>
        <div class="trust-item">
          <div class="trust-icon-box">🔄</div>
          <div class="trust-text">
            <h4>Đổi Size Tận Nhà 60 Ngày</h4>
            <p>Shipper đổi tận tay, hoàn toàn miễn phí</p>
          </div>
        </div>
        <div class="trust-item">
          <div class="trust-icon-box">🚚</div>
          <div class="trust-text">
            <h4>Freeship Đơn Từ 299K</h4>
            <p>Giao hàng nhanh toàn quốc 24h - 48h</p>
          </div>
        </div>
        <div class="trust-item">
          <div class="trust-icon-box">🛡️</div>
          <div class="trust-text">
            <h4>Kiểm Hàng Trước Thanh Toán</h4>
            <p>Đồng kiểm thực tế, ưng ý mới thanh toán</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.tshirt-trust-bar {
  background-color: #ffffff;
  border-bottom: 1px solid #eef2f6;
  padding: 24px 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.container-trust {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #eef2f6;
  transition: all 0.25s ease;
}

.trust-item:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: 0 6px 15px rgba(73, 104, 131, 0.08);
}

.trust-icon-box {
  font-size: 26px;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.trust-text h4 {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 2px;
}

.trust-text p {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

@media (max-width: 992px) {
  .trust-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .trust-grid {
    grid-template-columns: 1fr;
  }
}
</style>

