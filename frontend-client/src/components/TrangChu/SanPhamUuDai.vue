<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { formatCurrency } from '@/utils/dinhDang.js'
import { resolveProductImageUrl, NEUTRAL_PRODUCT_PLACEHOLDER } from '@/utils/luuTruGioHang.js'

const router = useRouter()

const props = defineProps({
  dealProducts: {
    type: Array,
    default: () => []
  },
  activeCampaigns: {
    type: Array,
    default: () => []
  }
})

// Tab đợt giảm giá đang chọn: 'ALL' hoặc id / tên đợt
const selectedCampaignId = ref('ALL')

// Đồng hồ đếm ngược thời gian
const nowTime = ref(Date.now())
let nowInterval = null

const getProductCountdown = (p) => {
  const endStr = p.ngayKetThucDotGiamGia || p.ngayKetThuc
  if (!endStr) return null
  const end = new Date(endStr).getTime()
  const diff = end - nowTime.value
  if (diff <= 0) return { expired: true, text: 'Đã hết hạn' }

  const d = Math.floor(diff / (1000 * 60 * 60 * 24))
  const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)

  return {
    expired: false,
    days: d,
    hours: String(h).padStart(2, '0'),
    mins: String(m).padStart(2, '0'),
    secs: String(s).padStart(2, '0')
  }
}

const dealCountdown = ref({
  days: 0,
  hours: '00',
  minutes: '00',
  seconds: '00'
})
let dealTimer = null

// Danh sách đợt giảm giá thực tế (lấy từ props hoặc tổng hợp từ dealProducts)
const campaignsList = computed(() => {
  if (props.activeCampaigns && props.activeCampaigns.length > 0) {
    return props.activeCampaigns
  }
  const map = new Map()
  for (const p of props.dealProducts) {
    const key = p.idDotGiamGia || p.tenDotGiamGia
    if (!key) continue
    if (!map.has(key)) {
      map.set(key, {
        id: p.idDotGiamGia || key,
        tenDotGiamGia: p.tenDotGiamGia,
        phanTramGiam: p.phanTramGiam,
        ngayBatDau: p.ngayBatDauDotGiamGia,
        ngayKetThuc: p.ngayKetThucDotGiamGia,
        count: 1
      })
    } else {
      map.get(key).count++
    }
  }
  return Array.from(map.values())
})

// Sản phẩm hiển thị theo tab đợt giảm giá được chọn
const displayedProducts = computed(() => {
  if (selectedCampaignId.value === 'ALL') {
    return props.dealProducts
  }
  return props.dealProducts.filter(p => {
    return p.idDotGiamGia === selectedCampaignId.value || p.tenDotGiamGia === selectedCampaignId.value
  })
})

// Xác định đợt giảm giá mục tiêu để hiển thị thời gian đếm ngược ở Header
const activeCountdownTarget = computed(() => {
  if (selectedCampaignId.value !== 'ALL') {
    const found = campaignsList.value.find(c => c.id === selectedCampaignId.value || c.tenDotGiamGia === selectedCampaignId.value)
    if (found) {
      return {
        tenDotGiamGia: found.tenDotGiamGia,
        dateStr: found.ngayKetThuc,
        label: `Đợt [${found.tenDotGiamGia}] kết thúc trong:`
      }
    }
  }

  // Khi chọn Tất cả: Tìm đợt có thời gian kết thúc GẦN NHẤT (sắp hết hạn trước)
  const validCampaigns = campaignsList.value
    .filter(c => c.ngayKetThuc)
    .sort((a, b) => new Date(a.ngayKetThuc).getTime() - new Date(b.ngayKetThuc).getTime())

  if (validCampaigns.length > 0) {
    const soonest = validCampaigns[0]
    return {
      tenDotGiamGia: soonest.tenDotGiamGia,
      dateStr: soonest.ngayKetThuc,
      label: `Đợt [${soonest.tenDotGiamGia}] kết thúc trong:`
    }
  }

  return {
    tenDotGiamGia: 'Ưu đãi hôm nay',
    dateStr: null,
    label: 'Kết thúc trong:'
  }
})

// Format ngày giờ theo chuẩn tiếng Việt DD/MM/YYYY HH:mm
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return ''
    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    const hours = String(d.getHours()).padStart(2, '0')
    const mins = String(d.getMinutes()).padStart(2, '0')
    return `${hours}:${mins} ${day}/${month}/${year}`
  } catch (e) {
    return ''
  }
}

// Cập nhật bộ đếm ngược thời gian thực
const updateTimer = () => {
  const targetInfo = activeCountdownTarget.value
  let targetTime
  if (targetInfo && targetInfo.dateStr) {
    targetTime = new Date(targetInfo.dateStr).getTime()
  } else {
    const def = new Date()
    def.setHours(23, 59, 59, 999)
    targetTime = def.getTime()
  }

  const now = Date.now()
  const diff = targetTime - now

  if (diff <= 0) {
    dealCountdown.value = { days: 0, hours: '00', minutes: '00', seconds: '00' }
    return
  }

  const d = Math.floor(diff / (1000 * 60 * 60 * 24))
  const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)

  dealCountdown.value = {
    days: d,
    hours: String(h).padStart(2, '0'),
    minutes: String(m).padStart(2, '0'),
    seconds: String(s).padStart(2, '0')
  }
}

const startDealCountdown = () => {
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

watch(selectedCampaignId, () => {
  updateTimer()
})

const handleImgError = (e) => {
  e.target.src = NEUTRAL_PRODUCT_PLACEHOLDER
}

const goToProduct = (id) => {
  router.push(`/san-pham/${id}`)
}

onMounted(() => {
  startDealCountdown()
  nowInterval = setInterval(() => { nowTime.value = Date.now() }, 1000)
})

onUnmounted(() => {
  stopDealCountdown()
  if (nowInterval) clearInterval(nowInterval)
})
</script>

<template>
  <section class="deals-section" id="san-pham-uu-dai">
    <div class="container-deals">
      <!-- 1. Header Khối Giảm Giá -->
      <div class="deal-header-wrapper">
        <div class="deal-title-left">
          <h2 class="script-title">
            <span class="deal-fire-emoji">🔥</span> Sản Phẩm Ưu Đãi
          </h2>
        </div>
      </div>

      <!-- 2. Thanh Tabs Phân Loại Đợt Giảm Giá (Tối giản, trang nhã) -->
      <div class="campaign-tabs-nav" v-if="campaignsList.length > 1">
        <button
          type="button"
          class="campaign-tab-btn"
          :class="{ active: selectedCampaignId === 'ALL' }"
          @click="selectedCampaignId = 'ALL'"
        >
          <span>Tất cả ưu đãi</span>
          <span class="tab-count-badge">{{ dealProducts.length }}</span>
        </button>

        <button
          v-for="camp in campaignsList"
          :key="camp.id"
          type="button"
          class="campaign-tab-btn"
          :class="{ active: selectedCampaignId === camp.id }"
          @click="selectedCampaignId = camp.id"
        >
          <span>{{ camp.tenDotGiamGia }}</span>
          <span class="tab-discount-tag" v-if="camp.phanTramGiam">-{{ Math.round(camp.phanTramGiam) }}%</span>
        </button>
      </div>

      <!-- 3. Lưới Sản Phẩm Đang Giảm Giá (Gọn gàng, loại bỏ khối trạng thái rối mắt) -->
      <div v-if="displayedProducts.length > 0" class="deals-grid" :class="{ 'compact-center': displayedProducts.length <= 2 }">
        <div
          v-for="p in displayedProducts"
          :key="p.id"
          class="deal-card"
          @click="goToProduct(p.id)"
        >
          <div class="deal-media">
            <img
              :src="resolveProductImageUrl(p.anhDaiDien || (p.hinhAnhs && p.hinhAnhs[0]))"
              :alt="p.tenSanPham"
              @error="handleImgError"
              loading="lazy"
            />

            <!-- Badge % Giảm Giá Góc Trái -->
            <div class="deal-discount-badge" v-if="p.coGiamGia && p.phanTramGiam > 0">
              <span class="badge-pct">-{{ Math.round(p.phanTramGiam) }}%</span>
            </div>
            <div class="deal-discount-badge best-deal" v-else>
              <span class="badge-pct">DEAL HOT</span>
            </div>

            <!-- Tên Đợt Giảm Giá Góc Dưới -->
            <span v-if="p.tenDotGiamGia" class="deal-campaign-tag">
              🏷️ {{ p.tenDotGiamGia }}
            </span>
          </div>

          <div class="deal-content">
            <div class="deal-meta-row">
              <span class="deal-category">{{ p.tenDanhMuc || 'Áo phông' }}</span>
            </div>

            <!-- Đếm ngược thời gian kết thúc của riêng từng sản phẩm -->
            <div class="product-deal-timer-pill" v-if="getProductCountdown(p) && !getProductCountdown(p).expired">
              <span class="pdt-icon">⏰</span>
              <span class="pdt-label">Kết thúc sau:</span>
              <span class="pdt-time">
                <template v-if="getProductCountdown(p).days > 0">
                  <strong class="pdt-num">{{ getProductCountdown(p).days }}</strong><small>N </small>
                </template>
                <strong class="pdt-num">{{ getProductCountdown(p).hours }}</strong>:<strong class="pdt-num">{{ getProductCountdown(p).mins }}</strong>:<strong class="pdt-num">{{ getProductCountdown(p).secs }}</strong>
              </span>
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

            <button class="btn-deal-action" type="button">
              <span>Săn Deal Ngay</span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 4. Giao Diện Khi Chưa Có Đợt Giảm Giá Hoạt Động -->
      <div v-else class="empty-deals-box text-center">
        <p>Hiện các đợt giảm giá mới đang được cập nhật. Mời bạn ghé xem toàn bộ sản phẩm tại cửa hàng!</p>
        <router-link to="/cua-hang" class="btn-view-all-shop">Xem Cửa Hàng &rarr;</router-link>
      </div>

      <!-- 5. Nút Xem Tất Cả Sản Phẩm Tại Cửa Hàng -->
      <div class="view-all-center-btn">
        <router-link to="/cua-hang" class="btn-view-all-shop">
          Xem Tất Cả Sản Phẩm Tại Cửa Hàng &rarr;
        </router-link>
      </div>
    </div>
  </section>
</template>

<style scoped>
.deals-section {
  padding: 56px 0 50px;
  background: #fbfcfe;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
  scroll-margin-top: 80px;
}

.container-deals {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 20px;
}

.deal-header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  gap: 20px;
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
  letter-spacing: 1.2px;
  padding: 4px 10px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.script-title {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.script-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.deal-fire-emoji {
  display: inline-block;
}

/* Khối Đếm Ngược Header */
.deal-countdown-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
}

.countdown-info-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.countdown-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 6px;
}

.countdown-clock-icon {
  font-size: 14px;
}

.countdown-sub-deadline {
  font-size: 11px;
  color: #64748b;
}

.countdown-sub-deadline strong {
  color: #0f172a;
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
  padding: 4px 7px;
  text-align: center;
  min-width: 42px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timer-unit strong {
  font-size: 14px;
  font-weight: 800;
  color: #ffffff;
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
  font-size: 14px;
  font-weight: 800;
  color: #64748b;
  margin: 0 1px;
}

/* Thanh Tabs Đợt Giảm Giá */
.campaign-tabs-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 22px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.campaign-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.campaign-tab-btn:hover {
  border-color: #cbd5e1;
  color: #0f172a;
}

.campaign-tab-btn.active {
  background: #1e293b;
  border-color: #1e293b;
  color: #ffffff;
}

.tab-count-badge {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
}

.campaign-tab-btn.active .tab-count-badge {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.tab-discount-tag {
  background: #fef2f2;
  color: #dc2626;
  font-size: 11px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 4px;
}

.campaign-tab-btn.active .tab-discount-tag {
  background: #ef4444;
  color: #ffffff;
}

/* Lưới Sản Phẩm */
.deals-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.deals-grid.compact-center {
  max-width: 780px;
  margin: 0 auto;
  grid-template-columns: repeat(2, 1fr);
}

.deal-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}

.deal-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

.deal-media {
  position: relative;
  width: 100%;
  padding-top: 105%;
  overflow: hidden;
  background: #f8fafc;
}

.deal-media img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.deal-card:hover .deal-media img {
  transform: scale(1.05);
}

.deal-discount-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #ef4444;
  color: #ffffff;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 800;
  z-index: 2;
}

.deal-discount-badge.best-deal {
  background: #d97706;
}

.deal-campaign-tag {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(15, 23, 42, 0.85);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
  max-width: 80%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  z-index: 2;
}

.deal-content {
  padding: 14px 16px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.deal-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.deal-category {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.deal-rating {
  font-size: 11px;
  color: #f59e0b;
  font-weight: 600;
}

.deal-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
  line-height: 1.4;
  min-height: 40px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.deal-price-row {
  margin-bottom: 14px;
  margin-top: auto;
}

.deal-price-group {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.deal-sale-price {
  font-size: 17px;
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
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 4px;
}

.btn-deal-action {
  width: 100%;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.btn-deal-action:hover {
  background: #364e63;
}

.empty-deals-box {
  padding: 36px 20px;
  background: #ffffff;
  border-radius: 10px;
  border: 1px dashed #cbd5e1;
  color: #64748b;
  font-size: 14px;
}

.view-all-center-btn {
  text-align: center;
  margin-top: 32px;
}

.btn-view-all-shop {
  display: inline-block;
  background: #ffffff;
  color: #496883;
  border: 1.5px solid #cbd5e1;
  padding: 10px 24px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}

.btn-view-all-shop:hover {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

@media (max-width: 992px) {
  .deals-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
  .deal-header-wrapper {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
}

@media (max-width: 600px) {
  .deals-grid {
    grid-template-columns: 1fr !important;
  }
  .deal-countdown-box {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

/* ĐẾM NGƯỢC THỜI GIAN RIÊNG CỦA TỪNG SẢN PHẨM ƯU ĐÃI */
.product-deal-timer-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 11.5px;
  color: #334155;
  margin: 6px 0 8px;
  width: fit-content;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
}

.pdt-icon {
  font-size: 12px;
}

.pdt-label {
  font-weight: 600;
  color: #64748b;
  font-size: 11px;
}

.pdt-time {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #ea580c;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.pdt-num {
  font-weight: 800;
  color: #c2410c;
}

.pdt-time small {
  font-size: 9.5px;
  margin-right: 2px;
  color: #64748b;
}

</style>
