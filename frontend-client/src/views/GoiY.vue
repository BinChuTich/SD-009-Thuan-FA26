<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import TheSanPham from '@/components/DungChung/TheSanPham.vue'
import { getMergedProducts } from '@/utils/danhMucSanPham.js'

const router = useRouter()

// Danh sách sản phẩm áo phông đang kinh doanh lấy từ API
const products = ref([])
const loading = ref(true)

// Bộ lọc tab danh mục áo phông
const activeTab = ref('all') // 'all' | 'sale' | 'polo' | 'basic' | 'unisex'

// Bộ tiêu chí gợi ý chọn áo phông
const filterNeed = ref('all') // 'all' | 'daily' | 'work' | 'style'
const filterBody = ref('all') // 'all' | 'slim' | 'fit' | 'plus'
const filterColor = ref('all') // 'all' | 'Đen' | 'Trắng' | 'Xám' | 'Xanh navy' | ...

const COLOR_HEX_MAP = {
  'đen': '#18181b',
  'trắng': '#ffffff',
  'xám': '#94a3b8',
  'ghi': '#94a3b8',
  'xanh navy': '#1e3a8a',
  'navy': '#1e3a8a',
  'xanh dương': '#2563eb',
  'xanh lá': '#16a34a',
  'xanh': '#0284c7',
  'đỏ': '#dc2626',
  'vàng': '#eab308',
  'be': '#d4b996',
  'nâu': '#78350f',
  'hồng': '#ec4899',
  'cam': '#ea580c'
}

const getColorHex = (colorName) => {
  const c = (colorName || '').trim().toLowerCase()
  for (const [key, hex] of Object.entries(COLOR_HEX_MAP)) {
    if (c.includes(key)) return hex
  }
  return '#cbd5e1'
}

const colorOptions = computed(() => {
  const set = new Set(['Đen', 'Trắng', 'Xám', 'Xanh navy'])
  products.value.forEach(p => {
    (p.colors || p.allColors || []).forEach(c => {
      if (c && typeof c === 'string') set.add(c.trim())
    })
  })
  return Array.from(set).map(name => {
    const hex = getColorHex(name)
    const isLight = hex.toLowerCase() === '#ffffff' || hex.toLowerCase() === '#fafafa' || hex.toLowerCase() === '#f8fafc'
    return {
      name,
      hex,
      isLight
    }
  })
})

// Tải danh sách áo phông đang kinh doanh (trangThai = 1) từ API
const loadActiveProducts = async () => {
  loading.value = true
  try {
    let list = []
    try {
      const res = await api.get('/api/san-pham', {
        params: { page: 0, size: 100, trangThai: 1 }
      })
      list = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    } catch (e) {
      console.warn('Lỗi gọi API san-pham:', e)
    }

    // Chỉ lấy sản phẩm đang kinh doanh (trangThai === 1)
    const activeList = getMergedProducts(list).filter(p => Number(p.trangThai ?? 1) === 1)

    // Tải thông tin màu sắc và ảnh biến thể cho từng sản phẩm
    await Promise.allSettled(
      activeList.map(async (p) => {
        try {
          const resVar = await api.get(`/api/chi-tiet-san-pham/by-san-pham/${p.id}`)
          const vars = resVar.data || []
          const colorImgMap = {}
          const activeColors = []
          const allColors = []

          vars.forEach(v => {
            const cName = v.tenMauSac || v.mauSac
            if (cName) {
              if (!allColors.includes(cName)) allColors.push(cName)
              if (Number(v.trangThai ?? 1) === 1 && (v.soLuongTon || v.soLuong || 0) > 0) {
                if (!activeColors.includes(cName)) activeColors.push(cName)
              }
              if (v.anhDaiDien && !colorImgMap[cName]) {
                colorImgMap[cName] = v.anhDaiDien
              }
            }
          })

          p.colorImageMap = colorImgMap
          p.colors = activeColors.length > 0 ? activeColors : allColors
          p.allColors = allColors
        } catch (e) {
          p.colors = []
          p.allColors = []
          p.colorImageMap = {}
        }
      })
    )

    // Đồng bộ các đợt giảm giá đang diễn ra cho từng áo phông
    try {
      const resActiveMap = await api.get('/api/dot-giam-gia/active-products-map')
      if (resActiveMap.data && typeof resActiveMap.data === 'object') {
        const dggMap = resActiveMap.data
        activeList.forEach(p => {
          const dot = dggMap[p.id]
          if (dot) {
            p.coGiamGia = true
            p.idDotGiamGia = dot.id
            p.tenDotGiamGia = dot.tenDotGiamGia || p.tenDotGiamGia
            p.phanTramGiam = dot.phanTramGiam || p.phanTramGiam
            if (p.minGia && (!p.minGiaSauGiam || p.minGiaSauGiam >= p.minGia) && p.phanTramGiam) {
              p.minGiaSauGiam = Math.round(p.minGia * (100 - p.phanTramGiam) / 100)
            }
          }
        })
      }
    } catch (e) {
      console.warn('Lỗi tải active-products-map:', e)
    }

    products.value = activeList
  } catch (err) {
    console.error('Lỗi tải sản phẩm áo phông:', err)
  } finally {
    loading.value = false
  }
}

// Lời khuyên chọn áo phù hợp theo vóc dáng và nhu cầu
const recommendationAdvice = computed(() => {
  let title = 'Tất cả mẫu áo phông đang bán'
  let tip = 'Chọn áo phông chất liệu 100% Cotton thoáng mát, co giãn tự nhiên để thoải mái suốt cả ngày dài.'

  if (filterNeed.value === 'work') {
    title = 'Gợi ý Áo Phông Polo & Áo Cổ Bẻ Lịch Sự'
    tip = 'Áo thun Polo với phần cổ bẻ dệt cứng cáp, cài khuy chỉn chu là lựa chọn hoàn hảo cho môi trường công sở, đi dạy, đi học hoặc gặp gỡ đối tác.'
  } else if (filterNeed.value === 'daily') {
    title = 'Gợi ý Áo Phông Trơn Cổ Tròn Mát Mẻ'
    tip = 'Áo phông cổ tròn 100% Cotton thấm hút mồ hôi tối đa, dễ giặt, nhẹ nhàng, là món đồ không thể thiếu cho các hoạt động hằng ngày.'
  } else if (filterNeed.value === 'style') {
    title = 'Gợi ý Áo Phông Trẻ Trung & Cá Tính'
    tip = 'Thiết kế form suông hoặc phối màu phong cách, dễ phối cùng quần jeans, quần shorts cho buổi cafe, dạo phố năng động.'
  }

  if (filterBody.value === 'slim') {
    tip += ' Dáng thon gọn nên ưu tiên chọn áo phông form suông hoặc màu sáng (trắng, xám) để tạo cảm giác người đầy đặn hơn.'
  } else if (filterBody.value === 'plus') {
    tip += ' Dáng người đậm hoặc có bụng nên ưu tiên áo phông cổ bẻ Polo hoặc gam màu tối (đen, than chì) để cơ thể trông gọn gàng, nam tính.'
  }

  if (filterColor.value !== 'all') {
    tip += ` Đang lọc các mẫu áo có tông màu ${filterColor.value} sang trọng, trẻ trung và dễ phối đồ.`
  }

  return { title, tip }
})

// Danh sách áo phông sau khi lọc
const filteredProducts = computed(() => {
  let list = [...products.value]

  // 1. Lọc theo tab danh mục nhanh
  if (activeTab.value === 'sale') {
    list = list.filter(p => p.coGiamGia && p.phanTramGiam > 0)
  } else if (activeTab.value === 'polo') {
    list = list.filter(p => {
      const name = (p.tenSanPham || '').toLowerCase()
      return name.includes('polo') || name.includes('cổ bẻ')
    })
  } else if (activeTab.value === 'basic') {
    list = list.filter(p => {
      const name = (p.tenSanPham || '').toLowerCase()
      return name.includes('trơn') || name.includes('mát mẻ') || name.includes('cổ tròn') || name.includes('basic')
    })
  } else if (activeTab.value === 'unisex') {
    list = list.filter(p => {
      const name = (p.tenSanPham || '').toLowerCase()
      return name.includes('unisix') || name.includes('unisex') || name.includes('nữ') || name.includes('phong cách')
    })
  }

  // 2. Lọc theo Nhu cầu mặc
  if (filterNeed.value === 'work') {
    list = list.filter(p => {
      const name = (p.tenSanPham || '').toLowerCase()
      return name.includes('polo') || name.includes('cổ bẻ') || name.includes('nam')
    })
  } else if (filterNeed.value === 'daily') {
    list = list.filter(p => {
      const name = (p.tenSanPham || '').toLowerCase()
      return name.includes('trơn') || name.includes('mát mẻ') || name.includes('unisix') || name.includes('unisex')
    })
  } else if (filterNeed.value === 'style') {
    list = list.filter(p => {
      const name = (p.tenSanPham || '').toLowerCase()
      return name.includes('nữ') || name.includes('phong cách') || name.includes('unisix') || name.includes('unisex')
    })
  }

  // 3. Lọc theo Màu sắc
  if (filterColor.value !== 'all') {
    const target = filterColor.value.toLowerCase()
    list = list.filter(p => {
      const colors = (p.colors && p.colors.length > 0) ? p.colors : (p.allColors || [])
      return colors.some(c => c.toLowerCase().includes(target) || target.includes(c.toLowerCase()))
    })
  }

  return list
})

// Đếm số sản phẩm đang có giảm giá
const discountCount = computed(() => {
  return products.value.filter(p => p.coGiamGia && p.phanTramGiam > 0).length
})

const resetAllFilters = () => {
  activeTab.value = 'all'
  filterNeed.value = 'all'
  filterBody.value = 'all'
  filterColor.value = 'all'
}

onMounted(() => {
  loadActiveProducts()
})
</script>

<template>
  <div class="goi-y-ao-phong-page">
    <!-- 1. HEADER BANNER: TẬP TRUNG BÁN ÁO PHÔNG -->
    <section class="page-hero">
      <div class="container-client">
        <div class="hero-content">
          <h1 class="hero-heading">Gợi Ý Mẫu Áo Phù Hợp Dành Cho Bạn</h1>
        </div>
      </div>
    </section>

    <div class="container-client page-body">
      <!-- 2. KHỐI TRỢ LÝ GỢI Ý ÁO PHÔNG THEO NHU CẦU & VÓC DÁNG -->
      <section class="smart-finder-card">
        <div class="finder-header">
          <div class="finder-header-left">
            <div class="finder-icon-wrap">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="3"></circle>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
              </svg>
            </div>
            <div>
              <h2 class="finder-title">Trợ Lý Gợi Ý</h2>
              <p class="finder-subtitle">Chọn nhu cầu của bạn để xem mẫu áo vừa vặn và tôn dáng nhất</p>
            </div>
          </div>
          <button
            v-if="filterNeed !== 'all' || filterBody !== 'all' || filterColor !== 'all'"
            type="button"
            class="btn-reset-top"
            @click="resetAllFilters"
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path>
              <path d="M3 3v5h5"></path>
            </svg>
            Đặt lại bộ lọc
          </button>
        </div>

        <!-- CÁC HÀNG TIÊU CHÍ GỢI Ý ĐƯỢC BỐ TRÍ NGANG TINH TẾ -->
        <div class="finder-filter-list">
          <!-- 1. Nhu cầu mặc áo -->
          <div class="finder-filter-row">
            <div class="filter-label-group">
              <span class="step-badge">1</span>
              <span class="filter-label">Nhu cầu mặc:</span>
            </div>
            <div class="filter-pill-wrap">
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterNeed === 'all' }]"
                @click="filterNeed = 'all'"
              >
                Tất cả nhu cầu
              </button>
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterNeed === 'daily' }]"
                @click="filterNeed = 'daily'"
              >
                Mặc hằng ngày & Mát mẻ
              </button>
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterNeed === 'work' }]"
                @click="filterNeed = 'work'"
              >
                Đi làm / Lịch sự (Polo)
              </button>
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterNeed === 'style' }]"
                @click="filterNeed = 'style'"
              >
                Trẻ trung & Phong cách
              </button>
            </div>
          </div>

          <!-- 2. Vóc dáng -->
          <div class="finder-filter-row">
            <div class="filter-label-group">
              <span class="step-badge">2</span>
              <span class="filter-label">Vóc dáng của bạn:</span>
            </div>
            <div class="filter-pill-wrap">
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterBody === 'all' }]"
                @click="filterBody = 'all'"
              >
                Mọi vóc dáng
              </button>
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterBody === 'fit' }]"
                @click="filterBody = 'fit'"
              >
                Cân đối / Chuẩn người
              </button>
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterBody === 'slim' }]"
                @click="filterBody = 'slim'"
              >
                Thon gọn / Dáng gầy
              </button>
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterBody === 'plus' }]"
                @click="filterBody = 'plus'"
              >
                Đậm người / Có bụng
              </button>
            </div>
          </div>

          <!-- 3. Màu sắc -->
          <div class="finder-filter-row">
            <div class="filter-label-group">
              <span class="step-badge">3</span>
              <span class="filter-label">Màu sắc:</span>
            </div>
            <div class="filter-pill-wrap">
              <button
                type="button"
                :class="['filter-pill-btn', { active: filterColor === 'all' }]"
                @click="filterColor = 'all'"
              >
                Tất cả màu sắc
              </button>
              <button
                v-for="c in colorOptions"
                :key="c.name"
                type="button"
                :class="['filter-pill-btn', 'color-pill-btn', { active: filterColor === c.name }]"
                @click="filterColor = c.name"
              >
                <span
                  class="color-dot"
                  :style="{
                    backgroundColor: c.hex,
                    border: c.isLight ? '1px solid #cbd5e1' : '1px solid rgba(0,0,0,0.1)'
                  }"
                ></span>
                <span>{{ c.name }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Hộp tư vấn nhanh -->
        <div class="recommend-tip-box">
          <div class="tip-icon-bulb">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#d2a764" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 18h6"></path>
              <path d="M10 22h4"></path>
              <path d="M15.09 14c.18-.98.65-1.74 1.41-2.5A4.65 4.65 0 0 0 18 8 6 6 0 0 0 6 8c0 1 .23 2.23 1.5 3.5A4.61 4.61 0 0 1 8.91 14"></path>
            </svg>
          </div>
          <div class="tip-text-body">
            <strong>Lời khuyên chọn áo:</strong> {{ recommendationAdvice.tip }}
          </div>
        </div>
      </section>

      <!-- 3. TABS PHÂN LOẠI NHANH CÁC DÒNG ÁO PHÔNG ĐANG BÁN -->
      <section class="tshirt-catalog-section">
        <div class="catalog-header-bar">
          <div>
            <h2 class="catalog-heading">Sản Phẩm Gợi Ý</h2>
            <span class="catalog-count-badge">Hiển thị {{ filteredProducts.length }} mẫu áo phông gợi ý sẵn hàng</span>
          </div>

          <!-- Các tab danh mục -->
          <div class="catalog-tabs">
            <button
              type="button"
              :class="['tab-pill', { active: activeTab === 'all' }]"
              @click="activeTab = 'all'"
            >
              Tất cả ({{ products.length }})
            </button>
            <button
              v-if="discountCount > 0"
              type="button"
              :class="['tab-pill', 'pill-sale', { active: activeTab === 'sale' }]"
              @click="activeTab = 'sale'"
            >
              🔥 Đang giảm giá ({{ discountCount }})
            </button>
            <button
              type="button"
              :class="['tab-pill', { active: activeTab === 'basic' }]"
              @click="activeTab = 'basic'"
            >
              Áo phông Basic cổ tròn
            </button>
            <button
              type="button"
              :class="['tab-pill', { active: activeTab === 'polo' }]"
              @click="activeTab = 'polo'"
            >
              Áo Polo cổ bẻ
            </button>
            <button
              type="button"
              :class="['tab-pill', { active: activeTab === 'unisex' }]"
              @click="activeTab = 'unisex'"
            >
              Áo Unisex & Nữ
            </button>
          </div>
        </div>

        <!-- Trạng thái Đang tải dữ liệu -->
        <div v-if="loading" class="products-loading-state">
          <span class="loading-spinner"></span>
          <p>Đang tải danh sách áo phông từ cửa hàng...</p>
        </div>

        <!-- Lưới sản phẩm thật từ cửa hàng -->
        <div v-else-if="filteredProducts.length > 0" class="products-showcase-grid">
          <TheSanPham
            v-for="product in filteredProducts"
            :key="product.id"
            :product="product"
            :target-color="filterColor !== 'all' ? filterColor : ''"
          />
        </div>

        <!-- Không có sản phẩm phù hợp -->
        <div v-else class="products-empty-state">
          <span class="empty-icon">👕</span>
          <p>Hiện không tìm thấy mẫu áo phông nào phù hợp với bộ lọc đã chọn.</p>
          <button type="button" class="btn-view-all" @click="resetAllFilters">
            Xem toàn bộ áo phông đang bán
          </button>
        </div>
      </section>


    </div>
  </div>
</template>

<style scoped>
/* TÔNG MÀU NHẬN DIỆN THƯƠNG HIỆU FF T-SHIRT (#496883 & #d2a764) */
.goi-y-ao-phong-page {
  background-color: #faf8f6;
  min-height: 100vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #2b2320;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

/* 1. HERO BANNER */
.page-hero {
  background: linear-gradient(135deg, #2b394a 0%, #496883 100%);
  color: #ffffff;
  padding: 28px 0 45px;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.breadcrumb-nav a {
  color: #cbd5e1;
  text-decoration: none;
  transition: color 0.2s;
}

.breadcrumb-nav a:hover {
  color: #d2a764;
}

.breadcrumb-nav .sep {
  color: #64748b;
}

.breadcrumb-nav .curr {
  color: #d2a764;
  font-weight: 600;
}

.hero-content {
  max-width: 820px;
}

.hero-badge {
  display: inline-block;
  background: rgba(210, 167, 100, 0.2);
  border: 1px solid rgba(210, 167, 100, 0.4);
  color: #fce7c8;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.8px;
  padding: 5px 12px;
  border-radius: 99px;
  margin-bottom: 12px;
}

.hero-heading {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.3;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.hero-subtext {
  font-size: 15px;
  line-height: 1.6;
  color: #e2e8f0;
  margin: 0;
}

.page-body {
  margin-top: -24px;
}

/* 2. SMART FINDER CARD */
.smart-finder-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 26px 30px;
  border: 1px solid #ede8e3;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.05);
  margin-bottom: 40px;
}

.finder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1ece7;
  flex-wrap: wrap;
}

.finder-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.finder-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fbf5ec;
  border: 1px solid #ecd8ba;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #996e27;
  flex-shrink: 0;
}

.finder-title {
  font-size: 20px;
  font-weight: 800;
  color: #2b394a;
  margin: 0 0 3px;
}

.finder-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin: 0;
}

.btn-reset-top {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #64748b;
  font-size: 12.5px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-reset-top:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fff5f5;
}

/* FINDER FILTER ROWS: BỐ TRÍ NGANG TINH TẾ & GỌN GÀNG */
.finder-filter-list {
  display: flex;
  flex-direction: column;
  margin-bottom: 18px;
}

.finder-filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 0;
  border-bottom: 1px solid #f6f2ee;
}

.finder-filter-row:last-child {
  border-bottom: none;
  padding-bottom: 6px;
}

.filter-label-group {
  width: 175px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fbf5ec;
  border: 1px solid #ecd8ba;
  color: #996e27;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.filter-label {
  font-size: 13.5px;
  font-weight: 700;
  color: #2b394a;
  white-space: nowrap;
}

.filter-pill-wrap {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 15px;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.3;
}

.filter-pill-btn:hover {
  border-color: #d2a764;
  color: #996e27;
  background: #fdfaf5;
}

.filter-pill-btn.active {
  background: #496883;
  border-color: #496883;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.25);
}

.filter-pill-btn.active .color-dot {
  box-shadow: 0 0 0 2px #ffffff;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.recommend-tip-box {
  background: #f8fafc;
  border: 1.5px dashed #cbd5e1;
  border-radius: 12px;
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.tip-icon-bulb {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #fbf5ec;
  border: 1px solid #ecd8ba;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tip-text-body {
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
  flex: 1;
}

/* 3. TSHIRT CATALOG SECTION */
.tshirt-catalog-section {
  margin-bottom: 45px;
}

.catalog-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.catalog-heading {
  font-size: 24px;
  font-weight: 800;
  color: #2b394a;
  margin: 0 0 4px;
}

.catalog-count-badge {
  font-size: 13px;
  color: #64748b;
}

.catalog-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tab-pill {
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 99px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-pill:hover {
  border-color: #d2a764;
  color: #d2a764;
}

.tab-pill.active {
  background: #496883;
  border-color: #496883;
  color: #ffffff;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.2);
}

.tab-pill.pill-sale.active {
  background: #ef4444;
  border-color: #ef4444;
  color: #ffffff;
}

.products-loading-state {
  text-align: center;
  padding: 60px 0;
  color: #64748b;
}

.loading-spinner {
  display: inline-block;
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.products-showcase-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}

.products-empty-state {
  text-align: center;
  padding: 50px 20px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px dashed #cbd5e1;
}

.empty-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
}

.products-empty-state p {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px;
}

.btn-view-all {
  background: #496883;
  color: #ffffff;
  border: none;
  font-size: 13px;
  font-weight: 700;
  padding: 9px 20px;
  border-radius: 8px;
  cursor: pointer;
}

/* RESPONSIVE */
@media (max-width: 1024px) {
  .products-showcase-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 860px) {
  .hero-heading {
    font-size: 26px;
  }
  .finder-filter-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .filter-label-group {
    width: 100%;
  }
  .products-showcase-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .catalog-header-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 580px) {
  .products-showcase-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
