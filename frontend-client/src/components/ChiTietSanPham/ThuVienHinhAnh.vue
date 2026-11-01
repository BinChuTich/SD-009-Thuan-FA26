<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const props = defineProps({
  images: {
    type: Array,
    default: () => []
  },
  activeIndex: {
    type: Number,
    default: 0
  },
  productName: {
    type: String,
    default: ''
  },
  discountPercent: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:activeIndex'])

const showLightbox = ref(false)

const openLightbox = () => {
  if (props.images && props.images.length > 0) {
    showLightbox.value = true
  }
}

const closeLightbox = () => {
  showLightbox.value = false
}

const prevImage = (e) => {
  if (e) e.stopPropagation()
  if (!props.images.length) return
  const newIndex = (props.activeIndex - 1 + props.images.length) % props.images.length
  emit('update:activeIndex', newIndex)
}

const nextImage = (e) => {
  if (e) e.stopPropagation()
  if (!props.images.length) return
  const newIndex = (props.activeIndex + 1) % props.images.length
  emit('update:activeIndex', newIndex)
}

const handleKeydown = (e) => {
  if (!showLightbox.value) return
  if (e.key === 'Escape') {
    closeLightbox()
  } else if (e.key === 'ArrowLeft') {
    prevImage()
  } else if (e.key === 'ArrowRight') {
    nextImage()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="image-showcase-col">
    <!-- Khung ảnh chính với nút chuyển slide và click xem ảnh vừa vặn -->
    <div class="main-slider-frame" @click="openLightbox">
      <img
        :src="resolveProductImageUrl(images[activeIndex])"
        @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER"
        :alt="productName"
        class="current-slide-img"
      />

      <!-- Tag đợt giảm giá ở đầu ảnh -->
      <span v-if="discountPercent > 0" class="gallery-discount-badge">
        -{{ Math.round(discountPercent) }}%
      </span>

      <span class="zoom-badge-btn" title="Nhấp để xem ảnh">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          <line x1="11" y1="8" x2="11" y2="14"></line>
          <line x1="8" y1="11" x2="14" y2="11"></line>
        </svg>
        Phóng to
      </span>

      <button
        v-if="images.length > 1"
        class="slider-arrow arrow-left"
        @click.stop="prevImage"
        title="Ảnh màu trước"
      >
        ‹
      </button>
      <button
        v-if="images.length > 1"
        class="slider-arrow arrow-right"
        @click.stop="nextImage"
        title="Ảnh màu kế"
      >
        ›
      </button>
    </div>

    <!-- Dải thumbnail ảnh màu phía dưới (Mỗi màu 1 ảnh) -->
    <div v-if="images.length > 1" class="thumb-strip">
      <div
        v-for="(img, idx) in images"
        :key="idx"
        class="thumb-tile"
        :class="{ active: activeIndex === idx }"
        @click="emit('update:activeIndex', idx)"
      >
        <img
          :src="resolveProductImageUrl(img)"
          :alt="'Màu ' + idx"
          @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER"
        />
      </div>
    </div>

    <!-- Modal xem ảnh lớn vừa vặn màn hình (Không bị quá to) -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showLightbox" class="lightbox-overlay" @click.self="closeLightbox">
          <div class="lightbox-dialog">
            <button class="lightbox-close-btn" @click="closeLightbox" title="Đóng (Esc)">✕</button>
            <div class="lightbox-img-wrapper">
              <img
                :src="resolveProductImageUrl(images[activeIndex])"
                :alt="productName"
                class="lightbox-img"
                @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER"
              />
            </div>
            <div v-if="images.length > 1" class="lightbox-nav">
              <button class="lightbox-arrow" @click.stop="prevImage" title="Ảnh màu trước">‹</button>
              <span class="lightbox-counter">{{ activeIndex + 1 }} / {{ images.length }}</span>
              <button class="lightbox-arrow" @click.stop="nextImage" title="Ảnh màu kế">›</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.image-showcase-col {
  width: 100%;
  max-width: 440px;
  margin: 0 auto;
}

.main-slider-frame {
  position: relative;
  width: 100%;
  max-width: 440px;
  aspect-ratio: 1 / 1;
  background-color: #f8fafc;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-in;
  transition: border-color 0.2s;
}

.main-slider-frame:hover {
  border-color: #cbd5e1;
}

.current-slide-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 12px;
  transition: transform 0.25s ease;
}

.current-slide-img:hover {
  transform: scale(1.02);
}

.zoom-badge-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  background: rgba(15, 23, 42, 0.7);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
  pointer-events: none;
  opacity: 0.85;
  transition: opacity 0.2s;
}

.main-slider-frame:hover .zoom-badge-btn {
  opacity: 1;
}

.slider-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #1e293b;
  border: 1px solid #cbd5e1;
  font-size: 22px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 5;
}

.slider-arrow:hover {
  background: #111827;
  color: #ffffff;
  border-color: #111827;
}

.arrow-left {
  left: 10px;
}

.arrow-right {
  right: 10px;
}

.thumb-strip {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 4px;
  justify-content: flex-start;
}

.thumb-tile {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e2e8f0;
  background-color: #f8fafc;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb-tile img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 4px;
}

.thumb-tile.active {
  border-color: #111827;
  box-shadow: 0 0 0 2px rgba(17, 24, 39, 0.15);
}

.thumb-tile:hover:not(.active) {
  border-color: #94a3b8;
}

/* Lightbox Modal vừa vặn màn hình */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(4px);
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.lightbox-dialog {
  position: relative;
  max-width: 520px;
  width: 100%;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
}

.lightbox-close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.65);
  color: #ffffff;
  border: none;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  transition: all 0.2s;
}

.lightbox-close-btn:hover {
  background: rgba(15, 23, 42, 0.95);
  transform: scale(1.08);
}

.lightbox-img-wrapper {
  width: 100%;
  aspect-ratio: 1 / 1;
  max-height: 68vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8fafc;
  padding: 16px;
}

.lightbox-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.lightbox-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 10px 16px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
}

.lightbox-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.lightbox-arrow:hover {
  background: #111827;
  color: #ffffff;
  border-color: #111827;
}

.lightbox-counter {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

/* Transition fade */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.gallery-discount-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.45);
  z-index: 10;
  letter-spacing: -0.2px;
}
</style>
