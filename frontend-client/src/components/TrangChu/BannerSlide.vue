<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const heroSlides = [
  {
    id: 1,
    image: 'https://images.unsplash.com/photo-1523381294911-8d3cead13475?w=1200&auto=format&fit=crop&q=80',
    title: 'Áo Phông Nam Basic Cotton',
    subtitle: '100% Cotton tự nhiên thoáng mát',
    badgeText: 'Ưu Đãi Thu 2026',
    badgeDesc: 'Giảm 20% các mẫu tuyển chọn',
    link: '/cua-hang'
  },
  {
    id: 2,
    image: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=1200&auto=format&fit=crop&q=80',
    title: 'Áo Polo Nam Smart-Casual',
    subtitle: 'Dệt Pique mắt chim thoáng khí',
    badgeText: 'Polo Nam Đẳng Cấp',
    badgeDesc: 'Chống nhăn & chuẩn form',
    link: '/cua-hang?danhmuc=Áo Polo Nam'
  },
  {
    id: 3,
    image: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=1200&auto=format&fit=crop&q=80',
    title: 'Áo Thun Oversize Streetwear',
    subtitle: 'Phom rộng unisex năng động',
    badgeText: 'Xu Hướng Giới Trẻ',
    badgeDesc: 'Cotton dày dặn 250gsm',
    link: '/cua-hang?danhmuc=Áo Thun Oversize Unisex'
  },
  {
    id: 4,
    image: 'https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=1200&auto=format&fit=crop&q=80',
    title: 'Áo Thun Dry-Fit Thể Thao',
    subtitle: 'Thoát ẩm & co giãn 4 chiều',
    badgeText: 'Dry-Fit Performance',
    badgeDesc: 'Mát lạnh suốt ngày dài',
    link: '/cua-hang?danhmuc=Áo Thun Thể Thao Dry-Fit'
  }
]

const currentSlide = ref(0)
let slideTimer = null

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % heroSlides.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + heroSlides.length) % heroSlides.length
}

const goToSlide = (idx) => {
  currentSlide.value = idx
}

const startSlideTimer = () => {
  stopSlideTimer()
  slideTimer = setInterval(nextSlide, 4500)
}

const stopSlideTimer = () => {
  if (slideTimer) {
    clearInterval(slideTimer)
    slideTimer = null
  }
}

onMounted(() => {
  startSlideTimer()
})

onUnmounted(() => {
  stopSlideTimer()
})
</script>

<template>
  <section class="brand-hero-banner">
    <div class="hero-inner">
      <div class="hero-left-col">
        <span class="hero-vintage-tag">✨ BỘ SƯU TẬP 2026</span>
        <div class="hero-brand-heading-block">
          <h1 class="hero-subheading-lead">Thời Trang Áo Phông Nam</h1>
          <div class="hero-logo-box">
            <!-- LOGO THƯƠNG HIỆU: 3 TIA SÁNG + CHỮ FF + T-shirt -->
            <div class="banner-logo-wrapper">
              <div class="banner-sparkles">
                <span class="ray ray-1"></span>
                <span class="ray ray-2"></span>
                <span class="ray ray-3"></span>
              </div>
              <div class="banner-logo-content">
                <div class="banner-logo-name">
                  <span class="f-white">F</span>
                  <span class="f-gold">F</span>
                </div>
                <div class="banner-logo-sub">T-shirt</div>
              </div>
            </div>
          </div>
        </div>
        <p class="hero-quote">
          "Trải nghiệm phong cách thời trang năng động, thanh lịch với chất liệu 100% Cotton tự nhiên định lượng cao, thoáng mát suốt ngày dài."
        </p>
        <div class="hero-cta-btns">
          <router-link to="/cua-hang" class="btn-hero-primary">Khám Phá Cửa Hàng &rarr;</router-link>
          <a href="#danh-muc-hot" class="btn-hero-outline">Xem Danh Mục Áo</a>
        </div>
      </div>

      <div class="hero-right-col">
        <div
          class="editorial-image-frame"
          @mouseenter="stopSlideTimer"
          @mouseleave="startSlideTimer"
        >
          <div
            v-for="(slide, idx) in heroSlides"
            :key="slide.id"
            class="hero-slide-item"
            :class="{ active: currentSlide === idx }"
            @click="router.push(slide.link)"
          >
            <img
              :src="slide.image"
              :alt="slide.title"
              class="hero-img-cover"
              loading="eager"
            />
            <div class="slide-overlay-gradient"></div>

            <div class="slide-caption-info">
              <span class="slide-sub">{{ slide.subtitle }}</span>
              <h3 class="slide-title">{{ slide.title }}</h3>
            </div>
          </div>

          <button class="slide-arrow prev-arrow" @click.stop="prevSlide" title="Ảnh trước">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
          </button>
          <button class="slide-arrow next-arrow" @click.stop="nextSlide" title="Ảnh tiếp theo">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </button>

          <div class="floating-deal-badge">
            <span class="deal-icon">🔥</span>
            <div>
              <strong>{{ heroSlides[currentSlide].badgeText }}</strong>
              <span>{{ heroSlides[currentSlide].badgeDesc }}</span>
            </div>
          </div>

          <div class="slide-dots">
            <button
              v-for="(s, idx) in heroSlides"
              :key="s.id"
              class="slide-dot"
              :class="{ active: currentSlide === idx }"
              @click.stop="goToSlide(idx)"
              :title="`Chuyển đến ảnh ${idx + 1}`"
            ></button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.brand-hero-banner {
  background: linear-gradient(135deg, #1e293b 0%, #496883 100%);
  color: #ffffff;
  padding: 60px 0 70px;
  position: relative;
  overflow: hidden;
}

.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 40px;
  align-items: center;
}

.hero-vintage-tag {
  display: inline-block;
  background: rgba(210, 167, 100, 0.2);
  color: #d2a764;
  border: 1px solid rgba(210, 167, 100, 0.4);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 18px;
}

.hero-brand-heading-block {
  margin-bottom: 16px;
}

.hero-subheading-lead {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 10px;
  color: #ffffff;
}

.hero-logo-box {
  display: inline-block;
  margin-top: 4px;
}

.banner-logo-wrapper {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  font-family: 'Fredoka', sans-serif;
  user-select: none;
}

.banner-sparkles {
  position: absolute;
  top: -12px;
  left: -14px;
  width: 32px;
  height: 32px;
  pointer-events: none;
}

.banner-sparkles .ray {
  position: absolute;
  background-color: #d2a764;
  border-radius: 3px;
  box-shadow: 0 0 10px rgba(210, 167, 100, 0.7);
}

.banner-sparkles .ray-1 {
  width: 4px;
  height: 13px;
  top: 0;
  right: 6px;
  transform: rotate(10deg);
}

.banner-sparkles .ray-2 {
  width: 4px;
  height: 13px;
  top: 5px;
  left: 5px;
  transform: rotate(-45deg);
}

.banner-sparkles .ray-3 {
  width: 4px;
  height: 13px;
  bottom: 0px;
  left: -3px;
  transform: rotate(-80deg);
}

.banner-logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 4px 14px rgba(0, 0, 0, 0.35));
}

.banner-logo-name {
  display: flex;
  font-size: 64px;
  font-weight: 800;
  line-height: 0.82;
  letter-spacing: -2.5px;
}

.f-white {
  color: #ffffff;
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.banner-logo-sub {
  color: #d2a764;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  margin-left: 10px;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.hero-quote {
  font-size: 16px;
  color: #cbd5e1;
  line-height: 1.6;
  margin: 0 0 28px;
  max-width: 520px;
}

.hero-cta-btns {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-hero-primary {
  background: #d2a764;
  color: #1e293b;
  font-weight: 700;
  font-size: 15px;
  padding: 13px 28px;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(210, 167, 100, 0.35);
}

.btn-hero-primary:hover {
  background: #e0b875;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(210, 167, 100, 0.45);
}

.btn-hero-outline {
  border: 2px solid #cbd5e1;
  color: #ffffff;
  font-weight: 700;
  font-size: 15px;
  padding: 11px 26px;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.25s ease;
}

.btn-hero-outline:hover {
  border-color: #d2a764;
  color: #d2a764;
  background: rgba(210, 167, 100, 0.1);
}

.editorial-image-frame {
  position: relative;
  width: 100%;
  height: 420px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  cursor: pointer;
}

.hero-slide-item {
  position: absolute;
  inset: 0;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.6s ease, visibility 0.6s ease;
}

.hero-slide-item.active {
  opacity: 1;
  visibility: visible;
}

.hero-img-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.editorial-image-frame:hover .hero-img-cover {
  transform: scale(1.04);
}

.slide-overlay-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(15, 23, 42, 0.85) 100%);
}

.slide-caption-info {
  position: absolute;
  bottom: 24px;
  left: 24px;
  right: 24px;
  z-index: 2;
}

.slide-sub {
  font-size: 13px;
  color: #d2a764;
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.slide-title {
  font-size: 22px;
  font-weight: 800;
  color: #ffffff;
  margin: 0;
}

.slide-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(4px);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 3;
  transition: all 0.25s ease;
}

.slide-arrow:hover {
  background: #d2a764;
  color: #1e293b;
  border-color: #d2a764;
}

.prev-arrow { left: 14px; }
.next-arrow { right: 14px; }

.floating-deal-badge {
  position: absolute;
  top: 18px;
  right: 18px;
  background: rgba(30, 41, 59, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(210, 167, 100, 0.5);
  padding: 8px 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 3;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.floating-deal-badge .deal-icon { font-size: 20px; }
.floating-deal-badge strong { font-size: 13px; color: #d2a764; display: block; }
.floating-deal-badge span { font-size: 11px; color: #cbd5e1; }

.slide-dots {
  position: absolute;
  bottom: 16px;
  right: 24px;
  display: flex;
  gap: 8px;
  z-index: 3;
}

.slide-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
  padding: 0;
}

.slide-dot.active {
  background: #d2a764;
  width: 24px;
  border-radius: 5px;
}

@media (max-width: 992px) {
  .hero-inner {
    grid-template-columns: 1fr;
    text-align: center;
  }
  .hero-heading { font-size: 32px; }
  .hero-quote { margin: 0 auto 24px; }
  .hero-cta-btns { justify-content: center; }
  .editorial-image-frame { height: 340px; }
}

@media (max-width: 600px) {
  .brand-hero-banner { padding: 40px 0; }
  .hero-heading { font-size: 26px; }
  .editorial-image-frame { height: 280px; }
  .floating-deal-badge { top: 12px; right: 12px; padding: 6px 10px; }
}
</style>

