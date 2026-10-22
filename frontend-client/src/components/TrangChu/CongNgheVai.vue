<script setup>
const fabricTechnologies = [
  {
    icon: '🧵',
    title: '100% Cotton Tự Nhiên 260GSM',
    desc: 'Sợi bông dài chải kỹ, định lượng dày dặn chuẩn form, êm ái trên da và bền bỉ gấp đôi cotton thường.'
  },
  {
    icon: '🌬️',
    title: 'Công Nghệ Thoát Ẩm Air-Cool',
    desc: 'Cấu trúc dệt siêu vi giải phóng nhiệt độ cơ thể tức thì, giúp bạn luôn sảng khoái và khô thoáng suốt ngày dài.'
  },
  {
    icon: '🛡️',
    title: 'Bo Cổ 2 Kim Chống Nhão',
    desc: 'Đường may đè nẹp gia cố độc quyền, chống biến dạng và bai dão cổ áo sau hơn 50+ chu kỳ giặt máy.'
  },
  {
    icon: '🌱',
    title: 'Nhuộm Hoạt Tính Thân Thiện',
    desc: 'Giữ độ bền màu xuất sắc qua thời gian, an toàn 100% cho làn da nhạy cảm và bảo vệ môi trường.'
  }
]
</script>

<template>
  <section class="fabric-tech-section">
    <div class="container-fabric">
      <div class="section-title-wrapper text-center">
        <span class="badge-sub-title">CHẤT LIỆU & GIA CÔNG ĐỈNH CAO</span>
        <h2 class="script-title">Công Nghệ Sợi Vải & Tiêu Chuẩn May Mặc</h2>
        <p class="script-subtitle">Từng chiếc áo phông FF T-Shirt là kết tinh của quy chuẩn khắt khe từ sợi bông đến đường chỉ</p>
      </div>

      <div class="fabric-cards-grid">
        <div
          v-for="(tech, idx) in fabricTechnologies"
          :key="idx"
          class="fabric-card"
        >
          <div class="fabric-icon-holder">{{ tech.icon }}</div>
          <h3 class="fabric-name">{{ tech.title }}</h3>
          <p class="fabric-desc">{{ tech.desc }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.fabric-tech-section {
  padding: 64px 0 54px;
  background: #f8fafc;
}

.container-fabric {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 20px;
}

.section-title-wrapper {
  margin-bottom: 36px;
}

.text-center {
  text-align: center;
}

.badge-sub-title {
  display: inline-block;
  background: #f1f5f9;
  color: #496883;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 8px;
}

.script-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 6px;
}

.script-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.fabric-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}

.fabric-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 28px 22px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.fabric-card:hover {
  transform: translateY(-5px);
  border-color: #cbd5e1;
  box-shadow: 0 12px 24px rgba(73, 104, 131, 0.1);
}

.fabric-icon-holder {
  font-size: 32px;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.fabric-name {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px;
}

.fabric-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0;
}

@media (max-width: 992px) {
  .fabric-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .fabric-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>

