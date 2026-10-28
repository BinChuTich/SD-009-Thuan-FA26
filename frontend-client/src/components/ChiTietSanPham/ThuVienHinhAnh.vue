<script setup>
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
  }
})

const emit = defineEmits(['update:activeIndex'])

const prevImage = () => {
  const newIndex = (props.activeIndex - 1 + props.images.length) % props.images.length
  emit('update:activeIndex', newIndex)
}

const nextImage = () => {
  const newIndex = (props.activeIndex + 1) % props.images.length
  emit('update:activeIndex', newIndex)
}
</script>

<template>
  <div class="image-showcase-col">
    <!-- Khung ảnh chính với nút chuyển slide -->
    <div class="main-slider-frame">
      <img
        :src="resolveProductImageUrl(images[activeIndex])"
        @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER"
        :alt="productName"
        class="current-slide-img"
      />

      <button
        v-if="images.length > 1"
        class="slider-arrow arrow-left"
        @click="prevImage"
        title="Ảnh trước"
      >
        ‹
      </button>
      <button
        v-if="images.length > 1"
        class="slider-arrow arrow-right"
        @click="nextImage"
        title="Ảnh kế"
      >
        ›
      </button>
    </div>

    <!-- Dải thumbnail nhỏ phía dưới -->
    <div v-if="images.length > 1" class="thumb-strip">
      <div
        v-for="(img, idx) in images"
        :key="idx"
        class="thumb-tile"
        :class="{ active: activeIndex === idx }"
        @click="emit('update:activeIndex', idx)"
      >
        <img :src="resolveProductImageUrl(img)" :alt="'Thumbnail ' + idx" @error="(e) => e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.image-showcase-col {
  width: 100%;
}

.main-slider-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1.15;
  background-color: #f8fafc;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.current-slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.slider-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #1e293b;
  border: 1px solid #cbd5e1;
  font-size: 24px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.slider-arrow:hover {
  background: #d2a764;
  color: #1e293b;
  border-color: #d2a764;
}

.arrow-left {
  left: 12px;
}

.arrow-right {
  right: 12px;
}

.thumb-strip {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 6px;
}

.thumb-tile {
  width: 72px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.thumb-tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-tile.active {
  border-color: #d2a764;
  box-shadow: 0 0 0 2px rgba(210, 167, 100, 0.3);
}
</style>

