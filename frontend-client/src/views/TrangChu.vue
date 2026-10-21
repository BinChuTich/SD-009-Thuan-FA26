<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import { CATEGORIES_CATALOG, getMergedProducts } from '@/utils/danhMucSanPham.js'

// Import các component con đã được chia nhỏ theo chuẩn Tiếng Việt
import BannerSlide from '@/components/TrangChu/BannerSlide.vue'
import DanhMucNoiBat from '@/components/TrangChu/DanhMucNoiBat.vue'
import SanPhamNoiBat from '@/components/TrangChu/SanPhamNoiBat.vue'
import SanPhamUuDai from '@/components/TrangChu/SanPhamUuDai.vue'
import TraCuuSize from '@/components/TrangChu/TraCuuSize.vue'

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
const activeDiscounts = ref([])
const loading = ref(true)

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
      console.warn('API /api/san-pham error:', e)
    }

    const merged = getMergedProducts(productList)

    // Đồng bộ trực tiếp bản đồ đợt giảm giá đang hoạt động từ Admin
    try {
      const resActiveMap = await api.get('/api/dot-giam-gia/active-products-map')
      if (resActiveMap.data && typeof resActiveMap.data === 'object') {
        const dggMap = resActiveMap.data
        merged.forEach(p => {
          const dot = dggMap[p.id]
          if (dot) {
            p.coGiamGia = true
            p.idDotGiamGia = dot.id
            p.tenDotGiamGia = dot.tenDotGiamGia || p.tenDotGiamGia
            p.phanTramGiam = dot.phanTramGiam || p.phanTramGiam
            p.ngayBatDauDotGiamGia = dot.ngayBatDau || p.ngayBatDauDotGiamGia
            p.ngayKetThucDotGiamGia = dot.ngayKetThuc || p.ngayKetThucDotGiamGia
            if (p.minGia && (!p.minGiaSauGiam || p.minGiaSauGiam >= p.minGia) && p.phanTramGiam) {
              p.minGiaSauGiam = Math.round(p.minGia * (100 - p.phanTramGiam) / 100)
            }
          }
        })
      }
    } catch (e) {
      console.warn('Lỗi lấy active-products-map:', e)
    }

    featuredProducts.value = merged

    try {
      const resActive = await api.get('/api/dot-giam-gia/active-products')
      activeDiscounts.value = resActive.data || []
    } catch (e) {}

    try {
      const resCat = await api.get('/api/thuoc-tinh/danh-muc')
      const catList = Array.isArray(resCat.data) ? resCat.data : (resCat.data?.value || [])
      if (catList.length > 0) {
        const seen = new Set()
        const unique = []
        for (const c of catList) {
          const norm = (c.tenDanhMuc || '').trim().toLowerCase()
          if (norm && !seen.has(norm)) {
            seen.add(norm)
            unique.push(c)
          }
        }
        categoryTabs.value = [
          { id: 'ALL', name: 'Tất cả' },
          ...unique.map(c => ({
            id: c.tenDanhMuc,
            name: c.tenDanhMuc
          }))
        ]
      }
    } catch (e) {}

    // Cập nhật số lượng sản phẩm thực tế theo danh mục từ Admin
    categories.value = CATEGORIES_CATALOG.map(c => {
      const count = merged.filter(p => {
        if (!p.tenDanhMuc) return false
        const pCat = p.tenDanhMuc.toLowerCase().trim()
        const cName = c.name.toLowerCase().trim()
        return pCat.includes(cName) || cName.includes(pCat)
      }).length
      return {
        ...c,
        count: `${count} mẫu`
      }
    })
  } catch (err) {
    console.error('Lỗi tải dữ liệu trang chủ:', err)
  } finally {
    loading.value = false
    if (route.hash) {
      setTimeout(scrollToHash, 100)
    }
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

// Lọc chuẩn xác các sản phẩm đang nằm trong đợt giảm giá HOẠT ĐỘNG từ Admin
const dealProducts = computed(() => {
  const now = Date.now()
  return featuredProducts.value.filter(p => {
    if (!p.coGiamGia || !p.phanTramGiam || p.phanTramGiam <= 0) return false
    // Kiểm tra thời gian kết thúc: chỉ lấy các đợt chưa hết hạn
    if (p.ngayKetThucDotGiamGia) {
      const end = new Date(p.ngayKetThucDotGiamGia).getTime()
      if (end <= now) return false
    }
    return true
  })
})

// Danh sách các đợt giảm giá đang hoạt động
const activeCampaigns = computed(() => {
  const map = new Map()
  for (const p of dealProducts.value) {
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

const route = useRoute()

const scrollToHash = () => {
  if (route.hash) {
    const targetId = route.hash.replace('#', '')
    const doScroll = () => {
      const el = document.getElementById(targetId)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' })
      }
    }
    setTimeout(doScroll, 100)
    setTimeout(doScroll, 350)
  }
}

onMounted(() => {
  loadHomeData()
  scrollToHash()
})

watch(() => route.hash, () => {
  scrollToHash()
})
</script>

<template>
  <div class="client-home-page">
    <!-- 1. Hero Slide Banner -->
    <BannerSlide />

    <!-- 2. Danh Mục Sản Phẩm Nổi Bật -->
    <DanhMucNoiBat :categories="categories" />

    <!-- 4. Sản Phẩm Nổi Bật Tuyển Chọn -->
    <SanPhamNoiBat
      :products="displayedFeaturedProducts"
      :loading="loading"
      :category-tabs="categoryTabs"
      v-model:selected-category-tab="selectedCategoryTab"
    />

    <!-- 5. Sản Phẩm Ưu Đãi Flash Deal (Đồng bộ chuẩn Admin & thời gian kết thúc từng đợt) -->
    <SanPhamUuDai
      :deal-products="dealProducts"
      :active-campaigns="activeCampaigns"
    />

    <!-- 6. Bộ Tra Cứu Size Áo Thông Minh -->
    <TraCuuSize />
  </div>
</template>

<style scoped>
.client-home-page {
  background-color: #ffffff;
}
</style>
