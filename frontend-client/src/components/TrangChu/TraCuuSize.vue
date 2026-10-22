<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const userHeight = ref(170)
const userWeight = ref(65)
const fitStyle = ref('regular')

const calculatedSize = computed(() => {
  const h = Number(userHeight.value)
  const w = Number(userWeight.value)
  const fit = fitStyle.value

  let size = 'L'
  if (w < 53) {
    size = h < 165 ? 'S' : 'M'
  } else if (w <= 62) {
    size = h < 170 ? 'M' : 'L'
  } else if (w <= 73) {
    size = h < 178 ? 'L' : 'XL'
  } else if (w <= 84) {
    size = 'XL'
  } else {
    size = '2XL'
  }

  if (fit === 'oversize') {
    if (size === 'S') return 'M'
    if (size === 'M') return 'L'
    if (size === 'L') return 'XL'
    if (size === 'XL') return '2XL'
  }
  if (fit === 'slim') {
    if (size === '2XL') return 'XL'
    if (size === 'XL') return 'L'
    if (size === 'L') return 'M'
    if (size === 'M') return 'S'
  }
  return size
})

const sizeFitDescription = computed(() => {
  switch (fitStyle.value) {
    case 'slim':
      return 'Vừa vặn ôm người nhẹ, tôn đường nét cơ bắp và dáng người'
    case 'oversize':
      return 'Rộng rãi cá tính phong cách Streetwear, thoáng mát tối đa'
    default:
      return 'Chuẩn Regular fit cân đối, thoải mái và lịch thiệp hằng ngày'
  }
})

const goToSizeShop = () => {
  router.push({ path: '/cua-hang', query: { size: calculatedSize.value } })
}
</script>

<template>
  <section class="size-finder-section">
    <div class="container-size">
      <div class="size-finder-box">
        <div class="finder-header text-center">
          <span class="finder-tag">📏 KHÔNG LO LỆCH SIZE</span>
          <h2 class="finder-title">Tìm Size Áo Chuẩn Xác Cho Riêng Bạn</h2>
          <p class="finder-subtitle">Kéo thanh trượt chiều cao và cân nặng để FF T-Shirt gợi ý size áo phông chuẩn theo vóc dáng người Việt</p>
        </div>

        <div class="finder-interactive-grid">
          <div class="finder-inputs-col">
            <div class="finder-slider-group">
              <div class="slider-label-row">
                <label>Chiều cao của bạn</label>
                <span class="slider-value-badge">{{ userHeight }} cm</span>
              </div>
              <input
                v-model="userHeight"
                type="range"
                min="150"
                max="190"
                step="1"
                class="custom-slider"
              />
              <div class="slider-ticks">
                <span>150 cm</span>
                <span>170 cm</span>
                <span>190 cm</span>
              </div>
            </div>

            <div class="finder-slider-group">
              <div class="slider-label-row">
                <label>Cân nặng của bạn</label>
                <span class="slider-value-badge">{{ userWeight }} kg</span>
              </div>
              <input
                v-model="userWeight"
                type="range"
                min="45"
                max="95"
                step="1"
                class="custom-slider"
              />
              <div class="slider-ticks">
                <span>45 kg</span>
                <span>70 kg</span>
                <span>95 kg</span>
              </div>
            </div>

            <div class="finder-fit-selector">
              <label class="fit-title">Form dáng mong muốn:</label>
              <div class="fit-btn-group">
                <button
                  :class="{ active: fitStyle === 'slim' }"
                  @click="fitStyle = 'slim'"
                >
                  Slim-Fit (Ôm nhẹ)
                </button>
                <button
                  :class="{ active: fitStyle === 'regular' }"
                  @click="fitStyle = 'regular'"
                >
                  Regular (Vừa vặn)
                </button>
                <button
                  :class="{ active: fitStyle === 'oversize' }"
                  @click="fitStyle = 'oversize'"
                >
                  Oversize (Rộng rãi)
                </button>
              </div>
            </div>
          </div>

          <div class="finder-result-col">
            <div class="result-card">
              <div class="result-badge-top">GỢI Ý TỐI ƯU</div>
              <div class="recommended-size-display">
                <span class="size-letter">{{ calculatedSize }}</span>
                <span class="size-label">SIZE CHUẨN</span>
              </div>
              <p class="size-fit-desc">{{ sizeFitDescription }}</p>
              
              <div class="size-guarantee-note">
                <span class="check-icon">✓</span>
                <span>Hỗ trợ đổi size tận nhà miễn phí 60 ngày</span>
              </div>

              <button class="btn-shop-this-size" @click="goToSizeShop">
                Xem Các Mẫu Size {{ calculatedSize }} &rarr;
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.size-finder-section {
  padding: 64px 0;
  background: #ffffff;
}

.container-size {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 20px;
}

.size-finder-box {
  background: linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%);
  border-radius: 18px;
  border: 1px solid #e2e8f0;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

.finder-header {
  margin-bottom: 36px;
}

.text-center {
  text-align: center;
}

.finder-tag {
  display: inline-block;
  background: #fef3c7;
  color: #92400e;
  font-size: 11px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 20px;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.finder-title {
  font-size: 26px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px;
}

.finder-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.finder-interactive-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 36px;
  align-items: center;
}

.finder-slider-group {
  margin-bottom: 24px;
}

.slider-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.slider-label-row label {
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.slider-value-badge {
  background: #496883;
  color: #ffffff;
  font-weight: 800;
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 20px;
}

.custom-slider {
  width: 100%;
  height: 8px;
  border-radius: 4px;
  background: #cbd5e1;
  outline: none;
  cursor: pointer;
  accent-color: #496883;
}

.slider-ticks {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.finder-fit-selector {
  margin-top: 20px;
}

.fit-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
}

.fit-btn-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.fit-btn-group button {
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.fit-btn-group button:hover {
  border-color: #496883;
  color: #1e293b;
}

.fit-btn-group button.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.25);
}

.result-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 28px 24px;
  text-align: center;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 25px rgba(73, 104, 131, 0.08);
  position: relative;
}

.result-badge-top {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: #d2a764;
  color: #1e293b;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 1px;
  padding: 4px 12px;
  border-radius: 12px;
}

.recommended-size-display {
  margin: 10px 0 14px;
}

.size-letter {
  display: block;
  font-size: 58px;
  font-weight: 900;
  color: #496883;
  line-height: 1;
}

.size-label {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #94a3b8;
}

.size-fit-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
  margin: 0 0 16px;
  min-height: 40px;
}

.size-guarantee-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: #16a34a;
  font-weight: 600;
  margin-bottom: 20px;
  background: #f0fdf4;
  padding: 8px 10px;
  border-radius: 6px;
}

.check-icon {
  font-weight: 900;
}

.btn-shop-this-size {
  width: 100%;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.btn-shop-this-size:hover {
  background: #385269;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.35);
}

@media (max-width: 992px) {
  .finder-interactive-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .size-finder-box {
    padding: 20px 16px;
  }
  .fit-btn-group {
    grid-template-columns: 1fr;
  }
}
</style>

