<script setup>
import { useRouter } from 'vue-router'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const router = useRouter()

defineProps({
  categories: {
    type: Array,
    default: () => []
  }
})

const goToCategory = (categoryName) => {
  router.push({ path: '/cua-hang', query: { danhmuc: categoryName } })
}
</script>

<template>
  <section id="danh-muc-hot" class="categories-section">
    <div class="container-cat">
      <div class="section-title-wrapper text-center">
        <span class="badge-sub-title">BỘ SƯU TẬP THEO PHONG CÁCH</span>
        <h2 class="script-title">Danh Mục Sản Phẩm</h2>
      </div>

      <div class="categories-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-card"
          @click="goToCategory(cat.name)"
        >
          <div class="cat-img-box">
            <img
              :src="resolveProductImageUrl(cat.image)"
              :alt="cat.name"
              loading="lazy"
              @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER"
            />
            <div class="cat-overlay"></div>
            <span class="cat-tag">{{ cat.tag }}</span>
          </div>
          <div class="cat-content">
            <h3 class="cat-name">{{ cat.name }}</h3>
            <p class="cat-desc">{{ cat.desc }}</p>
            <div class="cat-action">
              <span class="cat-count">{{ cat.count }}</span>
              <span class="cat-arrow">&rarr;</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.categories-section {
  padding: 60px 0 50px;
  background-color: #ffffff;
}

.container-cat {
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

.categories-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.category-card {
  background: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eef2f6;
  display: flex;
  flex-direction: column;
}

.category-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(73, 104, 131, 0.12);
  border-color: #cbd5e1;
}

.cat-img-box {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cat-img-box img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 14px;
  transition: transform 0.4s ease;
}

.category-card:hover .cat-img-box img {
  transform: scale(1.06);
}

.cat-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 65%, rgba(30, 41, 59, 0.18) 100%);
  pointer-events: none;
}

.cat-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(255, 255, 255, 0.95);
  color: #496883;
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  letter-spacing: 0.5px;
}

.cat-content {
  padding: 18px 20px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.cat-name {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 6px;
}

.cat-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 14px;
  line-height: 1.45;
  flex: 1;
}

.cat-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f1f5f9;
  padding-top: 12px;
}

.cat-count {
  font-size: 12.5px;
  font-weight: 700;
  color: #496883;
}

.cat-arrow {
  color: #d2a764;
  font-size: 16px;
  transition: transform 0.2s ease;
}

.category-card:hover .cat-arrow {
  transform: translateX(4px);
}

@media (max-width: 1024px) {
  .categories-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .categories-section { padding-top: 36px; padding-bottom: 40px; }
  .container-categories { padding-left: 12px; padding-right: 12px; }
  .categories-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
  .category-card { border-radius: 10px; }
  .cat-img-box { padding-top: 86%; }
  .cat-img-box img { padding: 8px; }
  .cat-tag { top: 7px; left: 7px; padding: 3px 6px; font-size: 9px; }
  .cat-content { padding: 10px; }
  .cat-name { font-size: 13px; margin-bottom: 4px; }
  .cat-desc { display: none; }
  .cat-action { padding-top: 7px; }
  .cat-count { font-size: 10.5px; }
}
</style>
