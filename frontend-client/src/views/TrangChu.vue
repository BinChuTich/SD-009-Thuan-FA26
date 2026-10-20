<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api'
import { CATEGORIES_CATALOG, getMergedProducts } from '@/utils/danhMucSanPham.js'

// Import các component con đã được chia nhỏ theo chuẩn Tiếng Việt
import BannerSlide from '@/components/TrangChu/BannerSlide.vue'
import ThanhCamKet from '@/components/TrangChu/ThanhCamKet.vue'
import DanhMucNoiBat from '@/components/TrangChu/DanhMucNoiBat.vue'
import SanPhamNoiBat from '@/components/TrangChu/SanPhamNoiBat.vue'
import SanPhamUuDai from '@/components/TrangChu/SanPhamUuDai.vue'
import TraCuuSize from '@/components/TrangChu/TraCuuSize.vue'
import CongNgheVai from '@/components/TrangChu/CongNgheVai.vue'
import CauChuyenThuongHieu from '@/components/TrangChu/CauChuyenThuongHieu.vue'
import DanhGiaKhachHang from '@/components/TrangChu/DanhGiaKhachHang.vue'
import DangKyNhanUuDai from '@/components/TrangChu/DangKyNhanUuDai.vue'

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
    featuredProducts.value = merged

    try {
      const resActive = await api.get('/api/dot-giam-gia/active-products')
      activeDiscounts.value = resActive.data || []
    } catch (e) {}

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

const dealProducts = computed(() => {
  const onSale = featuredProducts.value.filter(p => p.coGiamGia && p.phanTramGiam > 0)
  if (onSale.length > 0) {
    return onSale
  }
  return featuredProducts.value.slice(0, 4)
})

onMounted(() => {
  loadHomeData()
})
</script>

<template>
  <div class="client-home-page">
    <!-- 1. Hero Slide Banner -->
    <BannerSlide />

    <!-- 2. Thanh Cam Kết Chất Lượng -->
    <ThanhCamKet />

    <!-- 3. Danh Mục Sản Phẩm Nổi Bật -->
    <DanhMucNoiBat :categories="categories" />

    <!-- 4. Sản Phẩm Nổi Bật Tuyển Chọn -->
    <SanPhamNoiBat
      :products="displayedFeaturedProducts"
      :loading="loading"
      :category-tabs="categoryTabs"
      v-model:selected-category-tab="selectedCategoryTab"
    />

    <!-- 5. Sản Phẩm Ưu Đãi Flash Deal -->
    <SanPhamUuDai :deal-products="dealProducts" />

    <!-- 6. Bộ Tra Cứu Size Áo Thông Minh -->
    <TraCuuSize />

    <!-- 7. Công Nghệ Sợi Vải & Tiêu Chuẩn May Mặc -->
    <CongNgheVai />

    <!-- 8. Câu Chuyện Thương Hiệu -->
    <CauChuyenThuongHieu />

    <!-- 9. Đánh Giá Khách Hàng Thực Tế -->
    <DanhGiaKhachHang />

    <!-- 10. Hộp Đăng Ký VIP Nhận Ưu Đãi -->
    <DangKyNhanUuDai />
  </div>
</template>

<style scoped>
.client-home-page {
  background-color: #ffffff;
}
</style>
