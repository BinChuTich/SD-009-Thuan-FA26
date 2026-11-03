<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const heroSlides = [
  {
    id: 1,
    image: '/images/slides/slide_01.jpg',
    title: 'Thời Trang Áo Phông Đôi & Unisex Năng Động',
    link: '/cua-hang'
  },
  {
    id: 2,
    image: '/images/slides/slide_02.jpg',
    title: 'Áo Thun Nữ Phom Rộng Streetwear Cá Tính',
    link: '/cua-hang'
  },
  {
    id: 3,
    image: '/images/slides/slide_03.jpg',
    title: 'Áo Phông Nam Basic Cotton Tối Giản',
    link: '/cua-hang'
  },
  {
    id: 4,
    image: '/images/slides/slide_04.jpg',
    title: 'Thời Trang Áo Thun Nữ Hàn Quốc Thanh Lịch',
    link: '/cua-hang'
  },
  {
    id: 5,
    image: '/images/slides/slide_05.jpg',
    title: 'Áo Thun Nữ Streetwear Cá Tính Hiện Đại',
    link: '/cua-hang'
  },
  {
    id: 6,
    image: '/images/slides/slide_06.jpg',
    title: 'Bộ Sưu Tập Áo Phông Cao Cấp',
    link: '/cua-hang'
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
          <h1 class="hero-subheading-lead">Thời Trang Áo Phông</h1>
          <div class="hero-logo-box">
            <!-- LOGO THƯƠNG HIỆU: 3 TIA SÁNG + CHỮ FF + T-shirt -->
            <div class="banner-logo-wrapper">
              <div class="banner-sparkles">
                <span class="sparkle-aura"></span>
                <span class="ray ray-1"></span>
                <span class="ray ray-2"></span>
                <span class="ray ray-3"></span>
              </div>
              <div class="banner-logo-content">
                <div class="banner-logo-name">
                  <span class="f-blue">F</span>
                  <span class="f-gold">F</span>
                </div>
                <div class="banner-logo-sub">T-shirt</div>
              </div>
            </div>
          </div>
          <!-- DÒNG SLOGAN DƯỚI FF T-SHIRT (NGẮT DÒNG ĐỘC LẬP) -->
          <div class="hero-slogan-box">
            <p class="hero-slogan">Chất riêng trong từng sắc áo</p>
          </div>
        </div>

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
              :alt="slide.title || 'Banner'"
              class="hero-img-cover"
              loading="eager"
            />
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
  position: relative;
}

.brand-hero-banner {
  background: linear-gradient(135deg, #1e293b 0%, #334e68 50%, #496883 100%);
  color: #ffffff;
  padding: 56px 0 64px;
  position: relative;
  overflow: hidden;
}

.hero-inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 48px;
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
  margin-bottom: 28px;
}

.hero-subheading-lead {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 10px;
  color: #ffffff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6), 0 4px 14px rgba(0, 0, 0, 0.45);
  letter-spacing: -0.5px;
}

.hero-logo-box {
  display: flex;
  align-items: flex-end;
  margin-top: 6px;
  width: 100%;
}

.hero-slogan-box {
  display: block;
  width: 100%;
  margin-top: 14px;
  clear: both;
}

.hero-slogan {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.8px;
  line-height: 1.4;
  display: inline-block;
  background: linear-gradient(115deg, #94a3b8 0%, #ffffff 25%, #cbd5e1 45%, #ffffff 65%, #94a3b8 85%, #f1f5f9 100%);
  background-size: 250% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: silverGradualBrighten 3.6s ease-in-out infinite;
}

@keyframes silverGradualBrighten {
  0% {
    background-position: 0% 50%;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.6)) drop-shadow(0 0 2px rgba(255, 255, 255, 0.25));
    opacity: 0.82;
  }
  50% {
    background-position: 100% 50%;
    /* Action sang dan len voi hao quang anh bac kim loai lap lanh */
    filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.75)) drop-shadow(0 0 10px rgba(255, 255, 255, 0.95)) drop-shadow(0 0 22px rgba(226, 232, 240, 0.85));
    opacity: 1;
  }
  100% {
    background-position: 200% 50%;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.6)) drop-shadow(0 0 2px rgba(255, 255, 255, 0.25));
    opacity: 0.82;
  }
}

/* LOGO THUONG HIEU CHUAN: F XANH NAVY + F VANG GOLD + T-SHIRT XANH NAVY + 3 TIA SANG */
.banner-logo-wrapper {
  position: relative;
  display: inline-flex;
  align-items: flex-end;
  font-family: 'Fredoka', -apple-system, sans-serif;
  user-select: none;
}

.banner-sparkles {
  position: absolute;
  top: -14px;
  left: -8px;
  width: 36px;
  height: 36px;
  pointer-events: none;
}

.banner-sparkles .sparkle-aura {
  position: absolute;
  bottom: 2px;
  right: 6px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 252, 235, 0.95) 0%, rgba(251, 191, 36, 0.8) 45%, rgba(217, 119, 6, 0) 75%);
  animation: fireworkAuraBanner 2.4s ease-in-out infinite;
  pointer-events: none;
}

.banner-sparkles .ray {
  position: absolute;
  background-color: #d2a764;
  border-radius: 99px;
  transform-origin: center center;
  animation: fireworkSparkleBanner 2.4s ease-in-out infinite;
}

.banner-sparkles .ray-1 {
  --rot: 12deg;
  width: 4.8px;
  height: 14px;
  top: 1px;
  right: 8px;
  transform: rotate(12deg);
  animation-delay: 0s;
}

.banner-sparkles .ray-2 {
  --rot: -42deg;
  width: 4.8px;
  height: 14px;
  top: 6px;
  left: 6px;
  transform: rotate(-42deg);
  animation-delay: 0.35s;
}

.banner-sparkles .ray-3 {
  --rot: -78deg;
  width: 4.8px;
  height: 14px;
  bottom: 1px;
  left: 0px;
  transform: rotate(-78deg);
  animation-delay: 0.7s;
}

.banner-logo-wrapper:hover .banner-sparkles .ray {
  animation-duration: 1.1s;
}

@keyframes fireworkAuraBanner {
  0%, 100% {
    opacity: 0.15;
    transform: scale(0.75);
  }
  35% {
    opacity: 0.95;
    transform: scale(1.35);
    filter: drop-shadow(0 0 6px #f59e0b) drop-shadow(0 0 14px #fbbf24);
  }
  65% {
    opacity: 0.35;
    transform: scale(0.95);
  }
}

@keyframes fireworkSparkleBanner {
  0%, 100% {
    opacity: 0.35;
    transform: rotate(var(--rot, 0deg)) scale(0.88);
    background-color: #d1a868;
    box-shadow: 0 0 3px rgba(209, 168, 104, 0.4);
    filter: drop-shadow(0 0 2px rgba(209, 168, 104, 0.3));
  }
  25% {
    opacity: 0.8;
    transform: rotate(var(--rot, 0deg)) scale(1.15);
    background-color: #fde68a;
    box-shadow: 0 0 8px #f59e0b, 0 0 16px rgba(245, 158, 11, 0.75);
    filter: drop-shadow(0 0 4px #fbbf24);
  }
  45% {
    opacity: 1;
    transform: rotate(var(--rot, 0deg)) scale(1.4);
    background-color: #ffffff;
    box-shadow: 0 0 8px #ffffff, 0 0 18px #fbbf24, 0 0 32px #f59e0b, 0 0 44px rgba(245, 158, 11, 0.95);
    filter: drop-shadow(0 0 6px #ffffff) drop-shadow(0 0 12px #fbbf24);
  }
  65% {
    opacity: 0.85;
    transform: rotate(var(--rot, 0deg)) scale(1.18);
    background-color: #fcd34d;
    box-shadow: 0 0 8px #f59e0b, 0 0 16px rgba(210, 167, 100, 0.6);
    filter: drop-shadow(0 0 4px #f59e0b);
  }
  85% {
    opacity: 0.45;
    transform: rotate(var(--rot, 0deg)) scale(0.95);
    background-color: #d1a868;
    box-shadow: 0 0 3px rgba(209, 168, 104, 0.3);
  }
}

.banner-logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 6px 18px rgba(0, 0, 0, 0.75)) drop-shadow(0 2px 4px rgba(0, 0, 0, 0.6));
}

.banner-logo-name {
  display: flex;
  font-family: 'Fredoka', -apple-system, sans-serif;
  font-size: 64px;
  font-weight: 800;
  line-height: 0.82;
  letter-spacing: -2px;
}

/* Chu F Xanh Navy noi bat da tang do bong vien sang phan quang */
.f-blue {
  color: #496883;
  font-weight: 800;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.9),
    0 3px 6px rgba(0, 0, 0, 0.75),
    0 6px 16px rgba(0, 0, 0, 0.55),
    0 0 2px rgba(255, 255, 255, 0.45);
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.7)) drop-shadow(0 0 1px rgba(255, 255, 255, 0.35));
}

/* Chu F Vang Gold sang trong noi khoi 3D */
.f-gold {
  color: #d2a764;
  margin-left: 2px;
  font-weight: 800;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.85),
    0 3px 6px rgba(0, 0, 0, 0.7),
    0 6px 14px rgba(0, 0, 0, 0.5),
    0 0 2px rgba(210, 167, 100, 0.8);
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.65));
}

/* Chu T-shirt Xanh Navy chuan Fredoka noi bat ro net */
.banner-logo-sub {
  font-family: 'Fredoka', -apple-system, sans-serif;
  color: #496883;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  margin-left: 10px;
  margin-bottom: 4px;
  letter-spacing: -0.3px;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.9),
    0 2px 5px rgba(0, 0, 0, 0.7),
    0 0 2px rgba(255, 255, 255, 0.35);
  filter: drop-shadow(0 2px 5px rgba(0, 0, 0, 0.65));
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
  height: 450px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.35);
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
  object-position: center 20%;
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
  left: 50%;
  transform: translateX(-50%);
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
    padding-left: 20px;
    padding-right: 20px;
    margin: 0 auto;
    gap: 32px;
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

