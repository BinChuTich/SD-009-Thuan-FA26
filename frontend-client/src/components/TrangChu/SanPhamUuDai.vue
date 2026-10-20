<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'

const router = useRouter()

defineProps({
  dealProducts: {
    type: Array,
    default: () => []
  }
})

const dealCountdown = ref({
  hours: '08',
  minutes: '45',
  seconds: '30'
})
let dealTimer = null

const startDealCountdown = () => {
  const updateTimer = () => {
    const now = new Date()
    const target = new Date()
    target.setHours(23, 59, 59, 999)
    const diff = target.getTime() - now.getTime()
    if (diff <= 0) {
      dealCountdown.value = { hours: '00', minutes: '00', seconds: '00' }
      return
    }
    const h = Math.floor(diff / (1000 * 60 * 60))
    const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const s = Math.floor((diff % (1000 * 60)) / 1000)
    dealCountdown.value = {
      hours: String(h).padStart(2, '0'),
      minutes: String(m).padStart(2, '0'),
      seconds: String(s).padStart(2, '0')
    }
  }
  updateTimer()
  if (dealTimer) clearInterval(dealTimer)
  dealTimer = setInterval(updateTimer, 1000)
}

const stopDealCountdown = () => {
  if (dealTimer) {
    clearInterval(dealTimer)
    dealTimer = null
  }
}

const handleImgError = (e) => {
  e.target.src = '/images/products/default_tshirt.jpg'
}

const goToProduct = (id) => {
  router.push(`/san-pham/${id}`)
}

onMounted(() => {
  startDealCountdown()
})

onUnmounted(() => {
  stopDealCountdown()
})
</script>

<template>
  <section class="deals-section">
    <div class="container-deals">
      <div class="deal-header-wrapper">
        <div class="deal-title-left">
          <span class="badge-sub-title">GIÁ TỐT HÔM NAY • SỐ LƯỢNG CÓ HẠN</span>
          <h2 class="script-title">
            <span class="deal-fire-emoji">🔥</span> Sản Phẩm Ưu Đãi
          </h2>
          <p class="script-subtitle">Các mẫu áo phông Cotton tuyển chọn đang áp dụng chương trình trợ giá tốt nhất</p>
        </div>

        <div class="deal-countdown-box">
          <span class="countdown-label">
            <span class="pulsing-dot"></span> Kết thúc trong:
          </span>
          <div class="countdown-timer">
            <span class="timer-unit"><strong>{{ dealCountdown.hours }}</strong><small>Giờ</small></span>
            <span class="timer-colon">:</span>
            <span class="timer-unit"><strong>{{ dealCountdown.minutes }}</strong><small>Phút</small></span>
            <span class="timer-colon">:</span>
            <span class="timer-unit"><strong>{{ dealCountdown.seconds }}</strong><small>Giây</small></span>
          </div>
        </div>
      </div>

      <div v-if="dealProducts.length > 0" class="deals-grid" :class="{ 'compact-center': dealProducts.length <= 2 }">
        <div
          v-for="p in dealProducts"
          :key="p.id"
          class="deal-card"
          @click="goToProduct(p.id)"
        >
          <div class="deal-media">
            <img
              :src="p.anhDaiDien || (p.hinhAnhs && p.hinhAnhs[0]) || '/images/products/default_tshirt.jpg'"
              :alt="p.tenSanPham"
              @error="handleImgError"
              loading="lazy"
            />
            
            <div class="deal-discount-badge" v-if="p.coGiamGia && p.phanTramGiam > 0">
              <span class="badge-pct">-{{ Math.round(p.phanTramGiam) }}%</span>
            </div>
            <div class="deal-discount-badge best-deal" v-else>
              <span class="badge-pct">DEAL HOT</span>
            </div>

            <span v-if="p.tenDotGiamGia" class="deal-campaign-tag">
              🏷️ {{ p.tenDotGiamGia }}
            </span>
          </div>

          <div class="deal-content">
            <div class="deal-meta-row">
              <span class="deal-category">{{ p.tenDanhMuc || 'Áo phông' }}</span>
              <span class="deal-rating">⭐ 5.0 (Đã kiểm định)</span>
            </div>

            <h3 class="deal-title">{{ p.tenSanPham }}</h3>

            <div class="deal-price-row">
              <div class="deal-price-group">
                <template v-if="p.coGiamGia && p.minGiaSauGiam && p.minGiaSauGiam < p.minGia">
                  <span class="deal-sale-price">{{ formatCurrency(p.minGiaSauGiam) }}</span>
                  <span class="deal-regular-price">{{ formatCurrency(p.minGia) }}</span>
                </template>
                <template v-else>
                  <span class="deal-sale-price">{{ formatCurrency(p.minGia || 0) }}</span>
                </template>
              </div>

              <div v-if="p.coGiamGia && p.minGiaSauGiam && p.minGiaSauGiam < p.minGia" class="deal-save-chip">
                Tiết kiệm {{ formatCurrency(p.minGia - p.minGiaSauGiam) }}
              </div>
            </div>

            <div class="deal-status-box">
              <div class="deal-status-label">
                <span>Trạng thái: <strong>Đang bán chạy 🔥</strong></span>
                <span class="stock-text">Sẵn hàng</span>
              </div>
              <div class="deal-progress-bar">
                <div class="deal-progress-fill" :style="{ width: ((p.id * 27) % 35 + 60) + '%' }"></div>
              </div>
            </div>

            <button class="btn-deal-action">
              <span>Săn Deal Ngay</span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-else class="empty-deals-box text-center">
        <p>Hiện các chương trình ưu đãi đang được cập nhật. Mời bạn ghé xem toàn bộ sản phẩm tại cửa hàng!</p>
        <router-link to="/cua-hang" class="btn-view-all-shop">Xem Cửa Hàng &rarr;</router-link>
      </div>

      <div class="view-all-center-btn" style="margin-top: 36px;">
        <router-link to="/cua-hang" class="btn-view-all-shop">
          Xem Tất Cả Sản Phẩm Tại Cửa Hàng &rarr;
        </router-link>
      </div>
    </div>
  </section>
</template>

<style scoped>
.deals-section {
  padding: 64px 0 54px;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  border-top: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.container-deals {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.deal-header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 36px;
  gap: 24px;
  flex-wrap: wrap;
}

.deal-title-left {
  flex: 1;
  min-width: 280px;
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

.deal-fire-emoji {
  display: inline-block;
  animation: pulseFlame 1.5s infinite;
}

@keyframes pulseFlame {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.18); }
}

.deal-countdown-box {
  background: #ffffff;
  border: 1px solid #fde68a;
  border-radius: 12px;
  padding: 10px 18px;
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.08);
  display: flex;
  align-items: center;
  gap: 14px;
}

.countdown-label {
  font-size: 13px;
  font-weight: 700;
  color: #92400e;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pulsing-dot {
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  display: inline-block;
  animation: blinkDot 1s infinite;
}

@keyframes blinkDot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.2; }
}

.countdown-timer {
  display: flex;
  align-items: center;
  gap: 4px;
}

.timer-unit {
  background: #1e293b;
  color: #ffffff;
  border-radius: 6px;
  padding: 4px 8px;
  text-align: center;
  min-width: 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timer-unit strong {
  font-size: 15px;
  font-weight: 800;
  color: #fde68a;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.timer-unit small {
  font-size: 9px;
  text-transform: uppercase;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.timer-colon {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 2px;
}

.deals-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}

.deals-grid.compact-center {
  max-width: 780px;
  margin: 0 auto;
  grid-template-columns: repeat(2, 1fr);
}

.deal-card {
  background: #ffffff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.165, 0.84, 0.44, 1);
  border: 1px solid #eef2f6;
  display: flex;
  flex-direction: column;
}

.deal-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(73, 104, 131, 0.16);
  border-color: #d2a764;
}

.deal-media {
  position: relative;
  width: 100%;
  padding-top: 105%;
  overflow: hidden;
  background: #f1f5f9;
}

.deal-media img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}

.deal-card:hover .deal-media img {
  transform: scale(1.07);
}

.deal-discount-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #ffffff;
  padding: 4px 10px;
  border-radius: 6px;
  box-shadow: 0 4px 10px rgba(220, 38, 38, 0.35);
  z-index: 2;
}

.deal-discount-badge.best-deal {
  background: linear-gradient(135deg, #d2a764 0%, #b88a44 100%);
  box-shadow: 0 4px 10px rgba(210, 167, 100, 0.35);
}

.deal-discount-badge .badge-pct {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.deal-campaign-tag {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(30, 41, 59, 0.88);
  backdrop-filter: blur(4px);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
  max-width: 85%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  z-index: 2;
}

.deal-content {
  padding: 18px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.deal-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.deal-category {
  font-size: 12px;
  font-weight: 700;
  color: #496883;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.deal-rating {
  font-size: 11px;
  color: #64748b;
  font-weight: 600;
}

.deal-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 10px;
  line-height: 1.4;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.deal-price-row {
  margin-bottom: 12px;
}

.deal-price-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.deal-sale-price {
  font-size: 18px;
  font-weight: 800;
  color: #dc2626;
}

.deal-regular-price {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
}

.deal-save-chip {
  display: inline-block;
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fee2e2;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  margin-top: 4px;
}

.deal-status-box {
  margin-top: auto;
  margin-bottom: 14px;
}

.deal-status-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 6px;
}

.deal-status-label strong {
  color: #d97706;
}

.stock-text {
  font-size: 11px;
  color: #16a34a;
  font-weight: 700;
}

.deal-progress-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.deal-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ef4444 0%, #f59e0b 100%);
  border-radius: 3px;
}

.btn-deal-action {
  width: 100%;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.25s ease;
}

.btn-deal-action:hover {
  background: #385269;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.empty-deals-box {
  padding: 40px 20px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  color: #64748b;
}

.view-all-center-btn {
  text-align: center;
}

.btn-view-all-shop {
  display: inline-block;
  background: #f8fafc;
  color: #496883;
  border: 1.5px solid #cbd5e1;
  padding: 12px 28px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.25s;
}

.btn-view-all-shop:hover {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.2);
}

@media (max-width: 992px) {
  .deals-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
  .deal-header-wrapper {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}

@media (max-width: 600px) {
  .deals-grid {
    grid-template-columns: 1fr !important;
  }
  .deal-countdown-box {
    width: 100%;
    justify-content: space-between;
  }
}
</style>

