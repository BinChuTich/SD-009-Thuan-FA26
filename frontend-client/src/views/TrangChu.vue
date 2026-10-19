<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/format'
import { CATEGORIES_CATALOG, getMergedProducts } from '@/utils/productsCatalog.js'

const router = useRouter()

const categories = ref(CATEGORIES_CATALOG)
const selectedCategoryTab = ref('ALL')
const categoryTabs = ref([
  { id: 'ALL', name: 'Tất cả' },
  { id: 'Áo phông nam', name: 'Áo Phông Nam' },
  { id: 'Áo polo', name: 'Áo Polo Nam' },
  { id: 'Áo phông unisex', name: 'Áo Phông Unisex' },
  { id: 'Áo phông nữ', name: 'Áo Phông Nữ' }
])
const featuredProducts = ref([])
const discountedProducts = ref([])
const activeDiscounts = ref([])
const loading = ref(true)

const handleImgError = (e) => {
  e.target.src = '/images/products/default_tshirt.jpg'
}



const loadHomeData = async () => {
  loading.value = true
  try {
    let productList = []
    try {
      const resProducts = await api.get('/api/san-pham', {
        params: { page: 0, size: 50, trangThai: 1 }
      })
      productList = resProducts.data?.content || (Array.isArray(resProducts.data) ? resProducts.data : [])
    } catch (e) {
      console.warn('API /api/san-pham error, using catalog fallback:', e)
    }

    const merged = getMergedProducts(productList)
    featuredProducts.value = merged

    try {
      const resActive = await api.get('/api/dot-giam-gia/active-products')
      activeDiscounts.value = resActive.data || []
    } catch (e) {
      // Bỏ qua nếu chưa có đợt giảm giá active
    }

    try {
      const resCat = await api.get('/api/thuoc-tinh/danh-muc')
      const catList = Array.isArray(resCat.data) ? resCat.data : (resCat.data?.value || [])
      if (catList.length > 0) {
        categoryTabs.value = [
          { id: 'ALL', name: 'Tất cả' },
          ...catList.map(c => ({
            id: c.tenDanhMuc,
            name: c.tenDanhMuc
          }))
        ]
      }
    } catch (e) {
      // Giữ danh mục mặc định nếu API lỗi
    }

    discountedProducts.value = merged.filter(p => p.coGiamGia || (p.minGiaSauGiam && p.minGiaSauGiam < p.minGia))
    if (discountedProducts.value.length === 0) {
      discountedProducts.value = merged.slice(0, 4)
    }
  } catch (err) {
    console.error('Lỗi tải dữ liệu trang chủ:', err)
  } finally {
    loading.value = false
  }
}

const displayedFeaturedProducts = computed(() => {
  if (selectedCategoryTab.value === 'ALL') {
    return featuredProducts.value.slice(0, 8)
  }
  return featuredProducts.value.filter(p => {
    if (!p.tenDanhMuc) return false
    return p.tenDanhMuc.toLowerCase().trim() === selectedCategoryTab.value.toLowerCase().trim()
  }).slice(0, 8)
})

const goToProduct = (id) => {
  router.push(`/san-pham/${id}`)
}

const goToCategory = (categoryName) => {
  router.push({ path: '/cua-hang', query: { danhmuc: categoryName } })
}

// Hero Banner Slideshow
const currentSlide = ref(0)
let slideTimer = null
const heroSlides = ref([
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
])

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % heroSlides.value.length
}

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + heroSlides.value.length) % heroSlides.value.length
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

// --- 1. T-SHIRT SMART SIZE FINDER ---
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

// --- 2. FABRIC TECHNOLOGIES ---
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

// --- 3. SẢN PHẨM ƯU ĐÃI (SPECIAL DEALS & PROMOTIONS) ---
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

const dealProducts = computed(() => {
  // Lấy các sản phẩm đang có đợt giảm giá (coGiamGia = true hoặc minGiaSauGiam < minGia)
  const onSale = featuredProducts.value.filter(p => p.coGiamGia && p.phanTramGiam > 0)
  if (onSale.length > 0) {
    return onSale
  }
  // Nếu chưa có sản phẩm giảm giá trong DB, lấy các sản phẩm nổi bật
  return featuredProducts.value.slice(0, 4)
})

// --- 4. REAL CUSTOMER REVIEWS ---
const customerReviews = [
  {
    id: 1,
    name: 'Đặng Tuấn Anh',
    tag: 'Khách hàng thân thiết',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&fit=crop&q=80',
    product: 'Áo Phông Nam Basic Cotton Cổ Tròn',
    rating: 5,
    comment: 'Chất vải 100% Cotton sờ vào cực kỳ mịn tay và dày dặn. Mình giặt máy 4-5 lần rồi mà cổ áo không hề bị bai dão như mấy áo trước đây mua ở shop khác. Rất đáng tiền!'
  },
  {
    id: 2,
    name: 'Nguyễn Văn Hoàng',
    tag: 'Khách hàng văn phòng',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&fit=crop&q=80',
    product: 'Áo Polo Nam Cổ Bẻ Cao Cấp',
    rating: 5,
    comment: 'Áo Polo mặc đi làm công sở cực kỳ lịch sự và chuẩn form. Phần vải mắt chim thoáng khí tốt nên ngồi văn phòng hay ra ngoài cà phê đều rất dễ chịu.'
  },
  {
    id: 3,
    name: 'Trần Minh Quân',
    tag: 'Người yêu thời trang',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&fit=crop&q=80',
    product: 'Áo Phông Unisex In Hình Graphic',
    rating: 5,
    comment: 'Hình in sắc nét và bền, form rộng unisex mặc phối với quần short hay quần túi hộp bao đẹp. Giao hàng nhanh, shipper còn cho kiểm tra áo kỹ mới thu tiền.'
  }
]

// --- 5. NEWSLETTER VIP CLUB ---
const newsletterEmail = ref('')
const newsletterSuccess = ref(false)
const subscribeNewsletter = () => {
  if (newsletterEmail.value && newsletterEmail.value.includes('@')) {
    newsletterSuccess.value = true
  }
}

onMounted(() => {
  loadHomeData()
  startSlideTimer()
  startDealCountdown()
})

onUnmounted(() => {
  stopSlideTimer()
  stopDealCountdown()
})
</script>

<template>
  <div class="client-home-page">
    <!-- 1. HERO BANNER MANG MÀU THƯƠNG HIỆU #496883 & #d2a764 -->
    <section class="brand-hero-banner">
      <div class="hero-inner">
        <div class="hero-left-col">
          <span class="hero-vintage-tag">✨ BỘ SƯU TẬP 2026</span>
          <h1 class="hero-heading">Thời Trang Áo Phông Nam <span class="highlight-gold">FF T-SHIRT</span></h1>
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
            <!-- Danh sách các Slide -->
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

              <!-- Tiêu đề & tag phong cách trên ảnh -->
              <div class="slide-caption-info">
                <span class="slide-sub">{{ slide.subtitle }}</span>
                <h3 class="slide-title">{{ slide.title }}</h3>
              </div>
            </div>

            <!-- Nút mũi tên chuyển ảnh Trước / Sau -->
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

            <!-- Badge ưu đãi đổi theo slide -->
            <div class="floating-deal-badge">
              <span class="deal-icon">🔥</span>
              <div>
                <strong>{{ heroSlides[currentSlide].badgeText }}</strong>
                <span>{{ heroSlides[currentSlide].badgeDesc }}</span>
              </div>
            </div>

            <!-- Các chấm tròn phân trang Slide (Dots) -->
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

    <!-- 1.1 THANH CAM KẾT CHẤT LƯỢNG CHUYÊN BIỆT ÁO PHÔNG -->
    <section class="tshirt-trust-bar">
      <div class="container-client">
        <div class="trust-grid">
          <div class="trust-item">
            <div class="trust-icon-box">🧵</div>
            <div class="trust-text">
              <h4>100% Cotton Tự Nhiên</h4>
              <p>Định lượng cao 240-260 GSM mềm mịn</p>
            </div>
          </div>
          <div class="trust-item">
            <div class="trust-icon-box">🔄</div>
            <div class="trust-text">
              <h4>Đổi Size Tận Nhà 60 Ngày</h4>
              <p>Shipper đổi tận tay, hoàn toàn miễn phí</p>
            </div>
          </div>
          <div class="trust-item">
            <div class="trust-icon-box">🚚</div>
            <div class="trust-text">
              <h4>Freeship Đơn Từ 299K</h4>
              <p>Giao hàng nhanh toàn quốc 24h - 48h</p>
            </div>
          </div>
          <div class="trust-item">
            <div class="trust-icon-box">🛡️</div>
            <div class="trust-text">
              <h4>Kiểm Hàng Trước Thanh Toán</h4>
              <p>Đồng kiểm thực tế, ưng ý mới thanh toán</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 1.5. KHỐI CÁC DANH MỤC SẢN PHẨM NỔI BẬT (THEO MÀU WEB #496883 & #d2a764) -->
    <section id="danh-muc-hot" class="categories-section">
      <div class="container-client">
        <div class="section-title-wrapper text-center">
          <span class="badge-sub-title">BỘ SƯU TẬP THEO PHONG CÁCH</span>
          <h2 class="script-title">Danh Mục Sản Phẩm</h2>
          <p class="script-subtitle">Đa dạng kiểu dáng áo phông nam cao cấp, chuẩn tỉ lệ phom dáng Việt</p>
        </div>

        <div class="categories-grid">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-card"
            @click="goToCategory(cat.name)"
          >
            <div class="cat-img-box">
              <img :src="cat.image" :alt="cat.name" loading="lazy" />
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

    <!-- 2. SẢN PHẨM NỔI BẬT (8 SẢN PHẨM PHONG PHÚ THEO DANH MỤC) -->
    <section class="featured-section">
      <div class="container-client">
        <div class="section-title-wrapper text-center">
          <span class="badge-sub-title">THIẾT KẾ ĐƯỢC YÊU THÍCH</span>
          <h2 class="script-title">Sản Phẩm Nổi Bật</h2>
          <p class="script-subtitle">Bộ sưu tập áo phông bán chạy nhất mùa này với chất liệu Cotton cao cấp</p>
        </div>

        <!-- Tab lọc danh mục nhanh ngay trên trang chủ -->
        <div class="home-cat-tabs">
          <button
            v-for="tab in categoryTabs"
            :key="tab.id"
            :class="{ active: selectedCategoryTab === tab.id }"
            @click="selectedCategoryTab = tab.id"
          >
            {{ tab.name }}
          </button>
        </div>

        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải bộ sưu tập...</p>
        </div>

        <div v-else-if="displayedFeaturedProducts.length > 0" class="boutique-product-grid four-cols">
          <div
            v-for="p in displayedFeaturedProducts"
            :key="p.id"
            class="boutique-card"
            @click="goToProduct(p.id)"
          >
            <div class="card-media">
              <img
                :src="p.anhDaiDien || (p.hinhAnhs && p.hinhAnhs[0]) || '/images/products/default_tshirt.jpg'"
                :alt="p.tenSanPham"
                @error="handleImgError"
                loading="lazy"
              />
              <span v-if="p.coGiamGia && p.phanTramGiam > 0" class="tag-percent">
                -{{ Math.round(p.phanTramGiam) }}%
              </span>
            </div>

            <div class="card-details">
              <span class="card-category-badge">{{ p.tenDanhMuc || 'Áo phông' }}</span>
              <h3 class="product-title-text">{{ p.tenSanPham }}</h3>
              <div class="product-pricing">
                <template v-if="p.coGiamGia && p.minGiaSauGiam && p.minGiaSauGiam < p.minGia">
                  <span class="price-current">{{ formatCurrency(p.minGiaSauGiam) }}</span>
                  <span class="price-struck">{{ formatCurrency(p.minGia) }}</span>
                </template>
                <template v-else>
                  <span class="price-current">{{ formatCurrency(p.minGia || 0) }}</span>
                </template>
              </div>
              <button class="btn-detail-link">Xem chi tiết</button>
            </div>
          </div>
        </div>

        <div v-else class="empty-category-notice">
          <p>Chưa có sản phẩm nào thuộc danh mục này.</p>
        </div>

        <div class="view-all-center-btn">
          <router-link to="/cua-hang" class="btn-view-all-shop">
            Khám Phá Toàn Bộ Bộ Sưu Tập Áo Phông Tại Cửa Hàng &rarr;
          </router-link>
        </div>
      </div>
    </section>

    <!-- 3. SẢN PHẨM ƯU ĐÃI (SPECIAL DEALS & PROMOTIONS) -->
    <section class="deals-section">
      <div class="container-client">
        <div class="deal-header-wrapper">
          <div class="deal-title-left">
            <span class="badge-sub-title">GIÁ TỐT HÔM NAY • SỐ LƯỢNG CÓ HẠN</span>
            <h2 class="script-title">
              <span class="deal-fire-emoji">🔥</span> Sản Phẩm Ưu Đãi
            </h2>
            <p class="script-subtitle">Các mẫu áo phông Cotton tuyển chọn đang áp dụng chương trình trợ giá tốt nhất</p>
          </div>

          <!-- Đồng hồ đếm ngược Flash Deal -->
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
              
              <!-- Tag % giảm giá -->
              <div class="deal-discount-badge" v-if="p.coGiamGia && p.phanTramGiam > 0">
                <span class="badge-pct">-{{ Math.round(p.phanTramGiam) }}%</span>
              </div>
              <div class="deal-discount-badge best-deal" v-else>
                <span class="badge-pct">DEAL HOT</span>
              </div>

              <!-- Tên đợt giảm giá (nếu có từ DB) -->
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

              <!-- Thanh tiến độ số lượng đã bán -->
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

    <!-- 4. CÔNG CỤ TÌM SIZE ÁO THÔNG MINH (SMART SIZE FINDER) -->
    <section class="size-finder-section">
      <div class="container-client">
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
                  type="range"
                  min="150"
                  max="190"
                  step="1"
                  v-model="userHeight"
                  class="custom-slider"
                />
                <div class="slider-ticks">
                  <span>150cm</span>
                  <span>160cm</span>
                  <span>170cm</span>
                  <span>180cm</span>
                  <span>190cm</span>
                </div>
              </div>

              <div class="finder-slider-group">
                <div class="slider-label-row">
                  <label>Cân nặng của bạn</label>
                  <span class="slider-value-badge">{{ userWeight }} kg</span>
                </div>
                <input
                  type="range"
                  min="45"
                  max="95"
                  step="1"
                  v-model="userWeight"
                  class="custom-slider"
                />
                <div class="slider-ticks">
                  <span>45kg</span>
                  <span>55kg</span>
                  <span>65kg</span>
                  <span>75kg</span>
                  <span>95kg</span>
                </div>
              </div>

              <div class="finder-fit-selector">
                <label class="fit-title">Sở thích dáng áo mong muốn:</label>
                <div class="fit-btn-group">
                  <button
                    type="button"
                    :class="{ active: fitStyle === 'slim' }"
                    @click="fitStyle = 'slim'"
                  >
                    Ôm Vừa (Slim)
                  </button>
                  <button
                    type="button"
                    :class="{ active: fitStyle === 'regular' }"
                    @click="fitStyle = 'regular'"
                  >
                    Chuẩn Phom (Regular)
                  </button>
                  <button
                    type="button"
                    :class="{ active: fitStyle === 'oversize' }"
                    @click="fitStyle = 'oversize'"
                  >
                    Rộng Rãi (Oversize)
                  </button>
                </div>
              </div>
            </div>

            <div class="finder-result-col">
              <div class="result-card">
                <span class="rec-label">Gợi Ý Hoàn Hảo Cho Bạn:</span>
                <div class="size-display-circle">
                  <span class="size-big-text">{{ calculatedSize }}</span>
                </div>
                <h4 class="fit-verdict-title">Áo Phông Size {{ calculatedSize }}</h4>
                <p class="fit-verdict-desc">{{ sizeFitDescription }}</p>
                
                <div class="size-specs-mini">
                  <div class="spec-col">
                    <span class="s-label">Chiều cao:</span>
                    <strong>{{ userHeight }} cm</strong>
                  </div>
                  <div class="spec-divider"></div>
                  <div class="spec-col">
                    <span class="s-label">Cân nặng:</span>
                    <strong>{{ userWeight }} kg</strong>
                  </div>
                </div>

                <button class="btn-shop-this-size" @click="goToSizeShop">
                  Xem Áo Size {{ calculatedSize }} Tại Cửa Hàng &rarr;
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 5. CÔNG NGHỆ CHẤT LIỆU VẢI CAO CẤP -->
    <section class="fabric-tech-section">
      <div class="container-client">
        <div class="section-title-wrapper text-center">
          <span class="badge-sub-title">ĐỈNH CAO CHẤT LIỆU</span>
          <h2 class="script-title">Chất Liệu Định Hình Đẳng Cấp FF T-Shirt</h2>
          <p class="script-subtitle">Mỗi sợi vải đều được dệt và xử lý kỹ lưỡng để mang lại cảm giác mềm mại, thoáng mát và độ bền vượt trội</p>
        </div>

        <div class="fabric-cards-grid">
          <div
            v-for="(tech, idx) in fabricTechnologies"
            :key="idx"
            class="fabric-card"
          >
            <div class="fabric-icon">{{ tech.icon }}</div>
            <h3 class="fabric-name">{{ tech.title }}</h3>
            <p class="fabric-desc">{{ tech.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 6. KHỐI CÂU CHUYỆN THỜI TRANG -->
    <section class="story-editorial-section">
      <div class="container-client">
        <div class="editorial-banner-box">
          <div class="grid-story">
            <div class="img-composition">
              <img src="/images/products/sp01_front.jpg" alt="Chất liệu Cotton" class="story-img-main" />
              <img src="/images/products/sp02_front.jpg" alt="Chi tiết Áo Polo" class="story-img-sub" />
            </div>

            <div class="story-content-box">
              <span class="story-tag">VỀ FF T-SHIRT</span>
              <h2 class="story-title-main">Phong Cách Hiện Đại &amp; Tinh Tế</h2>
              <p class="story-body-text">
                FF T-Shirt ra đời với sứ mệnh định hình lại chiếc áo phông nam hằng ngày: vừa vặn với thể trạng người Việt, chất lượng bền bỉ và giữ giá trị thẩm mỹ qua nhiều năm tháng. Từng đường may 2 kim, từng nẹp viền cổ áo đều được kiểm định nghiêm ngặt trước khi đến tay bạn.
              </p>
              <div class="story-stats-row">
                <div class="stat-item">
                  <strong>100%</strong>
                  <span>Cotton Tự Nhiên</span>
                </div>
                <div class="stat-item">
                  <strong>50+</strong>
                  <span>Lần Giặt Chống Nhão</span>
                </div>
                <div class="stat-item">
                  <strong>60 Ngày</strong>
                  <span>Đổi Size Tận Nhà</span>
                </div>
              </div>
              <router-link to="/cua-hang" class="link-discover-more">
                Xem toàn bộ bộ sưu tập &rarr;
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 7. ĐÁNH GIÁ KHÁCH HÀNG THỰC TẾ -->
    <section class="reviews-section">
      <div class="container-client">
        <div class="section-title-wrapper text-center">
          <span class="badge-sub-title">TRẢI NGHIỆM KHÁCH HÀNG</span>
          <h2 class="script-title">Khách Hàng Nói Gì Về FF T-Shirt?</h2>
          <p class="script-subtitle">Hơn 15.000+ khách hàng trên cả nước tin dùng và hài lòng với chất lượng áo phông của chúng tôi</p>
        </div>

        <div class="reviews-grid">
          <div
            v-for="rev in customerReviews"
            :key="rev.id"
            class="review-card"
          >
            <div class="rev-stars">★★★★★</div>
            <p class="rev-text">"{{ rev.comment }}"</p>
            <div class="rev-product-tag">
              <span>Đã mua:</span> <strong>{{ rev.product }}</strong>
            </div>
            <div class="rev-user-profile">
              <img :src="rev.avatar" :alt="rev.name" class="rev-avatar" />
              <div>
                <h4 class="rev-user-name">{{ rev.name }}</h4>
                <span class="rev-user-role">{{ rev.tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 8. ĐĂNG KÝ NHẬN VOUCHER ƯU ĐÃI VIP 10% -->
    <section class="newsletter-cta-section">
      <div class="container-client">
        <div class="newsletter-card">
          <div class="newsletter-content">
            <span class="vip-badge">🎁 ĐẶC QUYỀN THÀNH VIÊN MỚI</span>
            <h2>Nhận Ngay Voucher Giảm 10% Cho Đơn Hàng Đầu Tiên</h2>
            <p>Đăng ký nhận thông tin bộ sưu tập áo phông mới nhất cùng các đợt ưu đãi đặc quyền chỉ dành riêng cho bạn.</p>

            <div v-if="newsletterSuccess" class="newsletter-success-box">
              <span class="check-icon">✓</span>
              <div>
                <strong>Đăng ký thành công!</strong>
                <p>Mã ưu đãi của bạn là: <code class="voucher-code">FFCHAOHE10</code> (Giảm 10% khi đặt hàng).</p>
              </div>
            </div>

            <form v-else class="newsletter-form" @submit.prevent="subscribeNewsletter">
              <input
                type="email"
                v-model="newsletterEmail"
                placeholder="Nhập địa chỉ email của bạn..."
                required
                class="newsletter-input"
              />
              <button type="submit" class="btn-newsletter-submit">
                Nhận Mã Giảm Giá &rarr;
              </button>
            </form>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<style scoped>
.client-home-page {
  background-color: #ffffff;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.25rem;
}

/* 1. Hero banner mang màu #496883 và #d2a764 */
.brand-hero-banner {
  background: linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 100%);
  padding: 44px 0 54px;
  border-bottom: 2px solid #ecd8af;
}

.hero-inner {
  max-width: 100%;
  width: 100%;
  padding-left: max(1.5rem, calc((100% - 1240px) / 2 + 1.25rem));
  padding-right: 1.25rem;
  display: grid;
  grid-template-columns: minmax(360px, 440px) 1fr;
  align-items: center;
  gap: 36px;
  box-sizing: border-box;
}

.hero-left-col {
  max-width: 440px;
}

.hero-vintage-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #d2a764;
  margin-bottom: 12px;
}

.hero-heading {
  font-size: 38px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 16px;
  line-height: 1.2;
}

.highlight-gold {
  color: #d2a764;
}

.hero-quote {
  font-size: 15px;
  line-height: 1.7;
  color: #475569;
  margin: 0 0 28px;
  max-width: 500px;
}

.hero-cta-btns {
  display: flex;
  gap: 14px;
}

.btn-hero-primary {
  background-color: #496883;
  color: #ffffff;
  padding: 12px 26px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.25);
}

.btn-hero-primary:hover {
  background-color: #364f66;
  transform: translateY(-2px);
}

.btn-hero-outline {
  background-color: transparent;
  color: #496883;
  border: 1.5px solid #496883;
  padding: 12px 24px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}

.btn-hero-outline:hover {
  background-color: #f1f5f9;
}

/* KHUNG ẢNH SLIDESHOW RỘNG RÃI & HIỆU ỨNG CHUYỂN CẢNH */
.editorial-image-frame {
  position: relative;
  width: 100%;
  height: 470px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 18px 45px rgba(73, 104, 131, 0.18);
  cursor: pointer;
  background-color: #1e293b;
  user-select: none;
}

/* Từng slide ảnh */
.hero-slide-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1), transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  transform: scale(1.03);
}

.hero-slide-item.active {
  opacity: 1;
  visibility: visible;
  transform: scale(1);
  z-index: 1;
}

.hero-img-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 25%;
}

.slide-overlay-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.25) 0%, rgba(15, 23, 42, 0) 45%, rgba(15, 23, 42, 0.5) 100%);
  pointer-events: none;
}

/* Caption thông tin trên slide */
.slide-caption-info {
  position: absolute;
  top: 24px;
  left: 24px;
  z-index: 2;
  pointer-events: none;
  max-width: 80%;
}

.slide-sub {
  display: inline-block;
  background: rgba(210, 167, 100, 0.95);
  color: #1e293b;
  font-size: 11px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 20px;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  text-transform: uppercase;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.slide-title {
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
  margin: 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.6);
  letter-spacing: -0.3px;
}

/* Nút chuyển mũi tên slide */
.slide-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 5;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.16);
  opacity: 0;
  transition: all 0.25s ease;
}

.editorial-image-frame:hover .slide-arrow {
  opacity: 1;
}

.slide-arrow:hover {
  background: #ffffff;
  color: #d2a764;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 8px 24px rgba(73, 104, 131, 0.3);
}

.prev-arrow {
  left: 16px;
}

.next-arrow {
  right: 16px;
}

/* Badge deal nổi đổi theo slide */
.floating-deal-badge {
  position: absolute;
  bottom: 22px;
  left: 22px;
  z-index: 5;
  background: #ffffff;
  padding: 10px 18px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
  border: 1.5px solid #ecd8af;
  pointer-events: none;
}

.deal-icon {
  font-size: 24px;
}

.floating-deal-badge strong {
  display: block;
  font-size: 13px;
  font-weight: 800;
  color: #496883;
}

.floating-deal-badge span {
  font-size: 11px;
  color: #64748b;
}

/* Dots chuyển slide */
.slide-dots {
  position: absolute;
  bottom: 22px;
  right: 22px;
  z-index: 5;
  display: flex;
  gap: 8px;
  align-items: center;
}

.slide-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  border: none;
  cursor: pointer;
  padding: 0;
  transition: all 0.3s ease;
}

.slide-dot:hover {
  background: rgba(255, 255, 255, 0.95);
}

.slide-dot.active {
  width: 28px;
  border-radius: 6px;
  background: #d2a764;
  box-shadow: 0 2px 10px rgba(210, 167, 100, 0.7);
}

/* 1.5. Danh mục sản phẩm */
.categories-section {
  padding: 50px 0 30px;
  background-color: #ffffff;
}

.badge-sub-title {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #d2a764;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.category-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
}

.category-card:hover {
  transform: translateY(-5px);
  border-color: #d2a764;
  box-shadow: 0 10px 24px rgba(73, 104, 131, 0.14);
}

.cat-img-box {
  position: relative;
  width: 100%;
  padding-top: 100%;
  overflow: hidden;
  background: #f8fafc;
}

.cat-img-box img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.category-card:hover .cat-img-box img {
  transform: scale(1.08);
}

.cat-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(30, 41, 59, 0.4) 0%, transparent 60%);
}

.cat-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: #d2a764;
  color: #ffffff;
  font-size: 9px;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 12px;
  letter-spacing: 0.3px;
}

.cat-content {
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.cat-name {
  font-size: 13px;
  font-weight: 700;
  color: #496883;
  margin: 0 0 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-desc {
  font-size: 11px;
  color: #64748b;
  margin: 0 0 8px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.cat-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
  color: #d2a764;
  padding-top: 6px;
  border-top: 1px dashed #f1f5f9;
}

.cat-arrow {
  transition: transform 0.2s ease;
}

.category-card:hover .cat-arrow {
  transform: translateX(4px);
}

/* 2. Sản phẩm nổi bật */
.featured-section {
  padding: 40px 0 60px;
}

.home-cat-tabs {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.home-cat-tabs button {
  background: #f8fafc;
  color: #496883;
  border: 1.5px solid #e2e8f0;
  border-radius: 20px;
  padding: 7px 18px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.home-cat-tabs button:hover {
  border-color: #496883;
  background: #ffffff;
}

.home-cat-tabs button.active {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.2);
}

.card-category-badge {
  font-size: 10px;
  font-weight: 700;
  color: #d2a764;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.view-all-center-btn {
  text-align: center;
  margin-top: 36px;
}

.btn-view-all-shop {
  display: inline-block;
  background-color: #f8fafc;
  color: #496883;
  border: 1.5px solid #496883;
  padding: 12px 30px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  transition: all 0.25s ease;
}

.btn-view-all-shop:hover {
  background-color: #496883;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.25);
}

.script-title {
  font-size: 28px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 6px;
}

.script-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.boutique-product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
}

.boutique-product-grid.four-cols {
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.boutique-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.boutique-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 24px rgba(73, 104, 131, 0.12);
  border-color: #d2a764;
}

.card-media {
  position: relative;
  width: 100%;
  padding-top: 125%;
  background-color: #f8fafc;
  overflow: hidden;
}

.card-media img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.boutique-card:hover .card-media img {
  transform: scale(1.05);
}

.tag-percent {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 7px;
  border-radius: 4px;
}

.card-details {
  text-align: center;
  padding: 16px 12px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.product-title-text {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.product-pricing {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.price-current {
  color: #ef4444;
  font-size: 15px;
  font-weight: 800;
}

.price-struck {
  color: #94a3b8;
  font-size: 12px;
  text-decoration: line-through;
}

.btn-detail-link {
  background: #f1f5f9;
  color: #496883;
  border: 1px solid #e2e8f0;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.boutique-card:hover .btn-detail-link {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.empty-category-notice {
  text-align: center;
  padding: 48px 20px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  color: #64748b;
  font-size: 14.5px;
  font-weight: 600;
  margin-bottom: 24px;
}

/* 3. Flash Sale */
.flash-sale-section {
  background-color: #f8fafc;
  padding: 48px 0 56px;
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.flash-sale-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  border: 1px solid #ecd8af;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 24px;
}

.flash-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.flash-badge {
  background: #ef4444;
  color: #ffffff;
  font-weight: 800;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 4px;
}

.flash-text h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.flash-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748b;
}

.countdown-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cd-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-right: 4px;
}

.cd-box {
  background: #496883;
  color: #ffffff;
  padding: 5px 8px;
  border-radius: 4px;
  text-align: center;
  min-width: 34px;
}

.cd-num {
  font-size: 13px;
  font-weight: 800;
  display: block;
  line-height: 1;
}

.cd-unit {
  font-size: 8px;
  color: #d2a764;
  text-transform: uppercase;
}

.cd-sep {
  font-weight: 800;
  color: #496883;
}

/* 4. Story editorial */
.story-editorial-section {
  padding: 56px 0;
}

.editorial-banner-box {
  background-color: #f1f5f9;
  border-radius: 14px;
  padding: 40px;
  border: 1px solid #e2e8f0;
}

.grid-story {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 36px;
  align-items: center;
}

.img-composition {
  position: relative;
  height: 320px;
}

.story-img-main {
  width: 78%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
}

.story-img-sub {
  position: absolute;
  right: 0;
  bottom: -16px;
  width: 50%;
  height: 180px;
  object-fit: cover;
  border-radius: 8px;
  border: 4px solid #ffffff;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.story-tag {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.5px;
  color: #d2a764;
}

.story-title-main {
  font-size: 26px;
  font-weight: 800;
  color: #496883;
  margin: 6px 0 14px;
}

.story-body-text {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  margin-bottom: 16px;
}

.link-discover-more {
  display: inline-block;
  color: #496883;
  font-weight: 700;
  font-size: 13px;
  text-decoration: none;
  border-bottom: 2px solid #d2a764;
  padding-bottom: 2px;
}


.loading-state {
  text-align: center;
  padding: 40px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* RESPONSIVE CHO ĐIỆN THOẠI & TABLET */
@media (max-width: 900px) {
  .hero-inner {
    grid-template-columns: 1fr;
    padding: 0 1.25rem;
    gap: 28px;
  }
  .hero-left-col {
    max-width: 100%;
  }
  .grid-story {
    grid-template-columns: 1fr;
  }
  .editorial-image-frame {
    height: 380px;
  }
  .slide-arrow {
    opacity: 0.9;
    width: 38px;
    height: 38px;
  }
  .categories-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .flash-sale-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .boutique-product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  .boutique-product-grid.four-cols {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (max-width: 600px) {
  .hero-heading {
    font-size: 28px;
  }
  .hero-quote {
    font-size: 13px;
  }
  .editorial-image-frame {
    display: block;
    height: 280px;
    margin-top: 8px;
  }
  .slide-caption-info {
    top: 14px;
    left: 14px;
  }
  .slide-title {
    font-size: 16px;
  }
  .floating-deal-badge {
    bottom: 12px;
    left: 12px;
    padding: 6px 12px;
  }
  .slide-dots {
    bottom: 12px;
    right: 12px;
  }
  .categories-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  .img-composition {
    height: 240px;
  }
}

/* ==================== 1.1 T-SHIRT TRUST BAR ==================== */
.tshirt-trust-bar {
  background-color: #ffffff;
  border-bottom: 1px solid #eef2f6;
  padding: 24px 0;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.02);
}

.trust-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.trust-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #eef2f6;
  transition: all 0.25s ease;
}

.trust-item:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: 0 6px 15px rgba(73, 104, 131, 0.08);
}

.trust-icon-box {
  font-size: 26px;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.trust-text h4 {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 2px;
}

.trust-text p {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

/* ==================== 3. DEALS & PROMOTIONS SECTION ==================== */
.deals-section {
  padding: 64px 0 54px;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  border-top: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
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

/* Deals Grid */
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

/* ==================== 4. SMART SIZE FINDER ==================== */
.size-finder-section {
  padding: 64px 0;
  background: #ffffff;
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

.fit-btn-group button.active {
  background: #496883;
  border-color: #496883;
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.2);
}

.result-card {
  background: #ffffff;
  border-radius: 14px;
  padding: 28px 24px;
  text-align: center;
  border: 2px solid #ecd8af;
  box-shadow: 0 10px 24px rgba(210, 167, 100, 0.12);
}

.rec-label {
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.size-display-circle {
  width: 84px;
  height: 84px;
  margin: 14px auto;
  border-radius: 50%;
  background: linear-gradient(135deg, #496883 0%, #354d63 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(73, 104, 131, 0.3);
}

.size-big-text {
  font-size: 34px;
  font-weight: 900;
  color: #ffffff;
}

.fit-verdict-title {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 6px;
}

.fit-verdict-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 18px;
  line-height: 1.4;
}

.size-specs-mini {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  background: #f8fafc;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.spec-col {
  display: flex;
  flex-direction: column;
  font-size: 12px;
}

.s-label {
  color: #94a3b8;
}

.spec-divider {
  width: 1px;
  height: 24px;
  background: #e2e8f0;
}

.btn-shop-this-size {
  width: 100%;
  background: #d2a764;
  color: #ffffff;
  border: none;
  padding: 12px 18px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 4px 12px rgba(210, 167, 100, 0.3);
}

.btn-shop-this-size:hover {
  background: #be924d;
  transform: translateY(-2px);
}

/* ==================== 5. FABRIC TECH SECTION ==================== */
.fabric-tech-section {
  padding: 64px 0;
  background: #f8fafc;
}

.fabric-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.fabric-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 28px 22px;
  border: 1px solid #eef2f6;
  text-align: center;
  transition: all 0.25s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.fabric-card:hover {
  transform: translateY(-4px);
  border-color: #cbd5e1;
  box-shadow: 0 10px 20px rgba(73, 104, 131, 0.08);
}

.fabric-icon {
  font-size: 38px;
  margin-bottom: 14px;
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

/* Story stats row */
.story-stats-row {
  display: flex;
  gap: 28px;
  margin: 20px 0 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-item strong {
  font-size: 22px;
  font-weight: 900;
  color: #496883;
}

.stat-item span {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
}

/* ==================== 7. REVIEWS SECTION ==================== */
.reviews-section {
  padding: 64px 0;
  background: #ffffff;
}

.reviews-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.review-card {
  background: #f8fafc;
  border-radius: 14px;
  padding: 26px 24px;
  border: 1px solid #eef2f6;
  display: flex;
  flex-direction: column;
  transition: all 0.25s;
}

.review-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.rev-stars {
  color: #f59e0b;
  font-size: 16px;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.rev-text {
  font-size: 13.5px;
  color: #334155;
  line-height: 1.6;
  font-style: italic;
  margin: 0 0 16px;
  flex: 1;
}

.rev-product-tag {
  font-size: 12px;
  color: #64748b;
  background: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

.rev-product-tag strong {
  color: #496883;
}

.rev-user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rev-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ecd8af;
}

.rev-user-name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 2px;
}

.rev-user-role {
  font-size: 12px;
  color: #94a3b8;
}

/* ==================== 8. NEWSLETTER SECTION ==================== */
.newsletter-cta-section {
  padding: 20px 0 60px;
  background: #ffffff;
}

.newsletter-card {
  background: linear-gradient(135deg, #496883 0%, #2f4557 100%);
  border-radius: 18px;
  padding: 48px 36px;
  color: #ffffff;
  text-align: center;
  box-shadow: 0 14px 30px rgba(73, 104, 131, 0.25);
}

.vip-badge {
  display: inline-block;
  background: rgba(210, 167, 100, 0.25);
  color: #ffd88a;
  border: 1px solid #d2a764;
  font-size: 11.5px;
  font-weight: 800;
  padding: 4px 14px;
  border-radius: 20px;
  margin-bottom: 14px;
  letter-spacing: 1px;
}

.newsletter-content h2 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 10px;
  color: #ffffff;
}

.newsletter-content p {
  font-size: 15px;
  color: #cbd5e1;
  max-width: 620px;
  margin: 0 auto 28px;
  line-height: 1.5;
}

.newsletter-form {
  display: flex;
  max-width: 520px;
  margin: 0 auto;
  gap: 10px;
}

.newsletter-input {
  flex: 1;
  padding: 14px 20px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.95);
  color: #1e293b;
  font-size: 14px;
  outline: none;
}

.btn-newsletter-submit {
  background: #d2a764;
  color: #ffffff;
  border: none;
  padding: 14px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-newsletter-submit:hover {
  background: #be924d;
}

.newsletter-success-box {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid #d2a764;
  padding: 16px 24px;
  border-radius: 10px;
  text-align: left;
}

.check-icon {
  font-size: 26px;
  color: #4ade80;
  font-weight: bold;
}

.voucher-code {
  background: #d2a764;
  color: #ffffff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 800;
}

/* Responsive updates */
@media (max-width: 992px) {
  .trust-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .deals-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
  .deal-header-wrapper {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .finder-interactive-grid {
    grid-template-columns: 1fr;
  }
  .fabric-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .reviews-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .trust-grid {
    grid-template-columns: 1fr;
  }
  .deals-grid {
    grid-template-columns: 1fr !important;
  }
  .deal-countdown-box {
    width: 100%;
    justify-content: space-between;
  }
  .fabric-cards-grid {
    grid-template-columns: 1fr;
  }
  .newsletter-form {
    flex-direction: column;
  }
  .size-finder-box {
    padding: 20px 16px;
  }
}
</style>
