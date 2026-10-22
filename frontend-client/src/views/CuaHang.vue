<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { CATEGORIES_CATALOG, getMergedProducts } from '@/utils/danhMucSanPham.js'
import BoLocSanPham from '@/components/CuaHang/BoLocSanPham.vue'
import ThanhSapXep from '@/components/CuaHang/ThanhSapXep.vue'
import TheSanPham from '@/components/DungChung/TheSanPham.vue'

const route = useRoute()
const router = useRouter()

// State dữ liệu
const loading = ref(false)
const products = ref([])
const categories = ref([])
const brands = ref([])
const sizes = ref([])
const colors = ref([])

// Toggle bộ lọc trên mobile
const showMobileFilter = ref(false)

// Bộ lọc
const filterKeyword = ref('')
const priceFrom = ref('')
const priceTo = ref('')
const selectedCategory = ref('')
const selectedBrand = ref('')
const selectedColor = ref('')
const selectedSize = ref('')
const sortBy = ref('newest')

// Phân trang
const currentPage = ref(1)
const pageSize = ref(12)

const loadAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      brands.value = res.data.thuongHieu || []
      sizes.value = res.data.kichCo || []
      colors.value = res.data.mauSac || []

      // Lấy danh mục trực tiếp từ Database (Admin), loại bỏ hoàn toàn trùng lặp
      const dbCats = res.data.danhMuc || []
      if (dbCats.length > 0) {
        const seen = new Set()
        const uniqueCats = []
        for (const c of dbCats) {
          const norm = (c.tenDanhMuc || '').trim().toLowerCase()
          if (norm && !seen.has(norm)) {
            seen.add(norm)
            uniqueCats.push(c)
          }
        }
        categories.value = uniqueCats
      } else {
        categories.value = CATEGORIES_CATALOG.map(c => ({ id: c.id, tenDanhMuc: c.name }))
      }
    }
  } catch (err) {
    console.error('Lỗi tải thuộc tính:', err)
    categories.value = CATEGORIES_CATALOG.map(c => ({ id: c.id, tenDanhMuc: c.name }))
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    let content = []
    try {
      const res = await api.get('/api/san-pham', {
        params: {
          page: 0,
          size: 200,
          trangThai: 1
        }
      })
      content = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    } catch (e) {
      console.warn('API error, using catalog fallback:', e)
    }
    products.value = getMergedProducts(content)
  } catch (err) {
    console.error('Lỗi tải sản phẩm:', err)
  } finally {
    loading.value = false
  }
}

// Lọc sản phẩm
const filteredProducts = computed(() => {
  let list = [...products.value]

  if (filterKeyword.value && filterKeyword.value.trim()) {
    const q = filterKeyword.value.trim().toLowerCase()
    list = list.filter(p =>
      (p.tenSanPham && p.tenSanPham.toLowerCase().includes(q)) ||
      (p.maSanPham && p.maSanPham.toLowerCase().includes(q)) ||
      (p.tenThuongHieu && p.tenThuongHieu.toLowerCase().includes(q)) ||
      (p.tenDanhMuc && p.tenDanhMuc.toLowerCase().includes(q))
    )
  }

  if (selectedCategory.value) {
    const selNorm = String(selectedCategory.value).trim().toLowerCase()
    list = list.filter(p => {
      if (p.idDanhMuc && String(p.idDanhMuc).trim().toLowerCase() === selNorm) return true
      if (p.tenDanhMuc && p.tenDanhMuc.trim().toLowerCase() === selNorm) return true
      return false
    })
  }

  if (selectedBrand.value) {
    list = list.filter(p => p.idThuongHieu == selectedBrand.value || p.tenThuongHieu === selectedBrand.value)
  }

  if (selectedColor.value) {
    list = list.filter(p => p.idMauSac == selectedColor.value || p.tenMauSac === selectedColor.value)
  }

  if (selectedSize.value) {
    list = list.filter(p => p.idKichCo == selectedSize.value || p.tenKichCo === selectedSize.value)
  }

  if (priceFrom.value !== '') {
    const fromVal = Number(priceFrom.value)
    if (!isNaN(fromVal)) {
      list = list.filter(p => {
        const pr = p.minGiaSauGiam != null && p.coGiamGia ? Number(p.minGiaSauGiam) : Number(p.minGia || 0)
        return pr >= fromVal
      })
    }
  }

  if (priceTo.value !== '') {
    const toVal = Number(priceTo.value)
    if (!isNaN(toVal)) {
      list = list.filter(p => {
        const pr = p.minGiaSauGiam != null && p.coGiamGia ? Number(p.minGiaSauGiam) : Number(p.minGia || 0)
        return pr <= toVal
      })
    }
  }

  if (sortBy.value === 'price-asc') {
    list.sort((a, b) => {
      const pA = a.minGiaSauGiam != null && a.coGiamGia ? Number(a.minGiaSauGiam) : Number(a.minGia || 0)
      const pB = b.minGiaSauGiam != null && b.coGiamGia ? Number(b.minGiaSauGiam) : Number(b.minGia || 0)
      return pA - pB
    })
  } else if (sortBy.value === 'price-desc') {
    list.sort((a, b) => {
      const pA = a.minGiaSauGiam != null && a.coGiamGia ? Number(a.minGiaSauGiam) : Number(a.minGia || 0)
      const pB = b.minGiaSauGiam != null && b.coGiamGia ? Number(b.minGiaSauGiam) : Number(b.minGia || 0)
      return pB - pA
    })
  } else if (sortBy.value === 'name-asc') {
    list.sort((a, b) => (a.tenSanPham || '').localeCompare(b.tenSanPham || '', 'vi'))
  } else {
    list.sort((a, b) => (b.id || 0) - (a.id || 0))
  }

  return list
})

const totalPages = computed(() => Math.ceil(filteredProducts.value.length / pageSize.value) || 1)
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredProducts.value.slice(start, start + pageSize.value)
})

const resetFilters = () => {
  filterKeyword.value = ''
  priceFrom.value = ''
  priceTo.value = ''
  selectedCategory.value = ''
  selectedBrand.value = ''
  selectedColor.value = ''
  selectedSize.value = ''
  sortBy.value = 'newest'
  currentPage.value = 1
  router.replace({ query: {} })
}

watch(() => route.query, (q) => {
  if (q.q) filterKeyword.value = q.q
  if (q.danhmuc) selectedCategory.value = q.danhmuc
  currentPage.value = 1
}, { immediate: true })

onMounted(() => {
  loadAttributes()
  loadProducts()
})
</script>

<template>
  <div class="shop-page-wrap">
    <!-- 1. TIÊU ĐỀ BỘ SƯU TẬP -->
    <div class="shop-header-intro">
      <div class="container-client text-center">
        <h1 class="shop-title-main">Bộ Sưu Tập Áo Phông Nam</h1>
        <p class="shop-quote-sub">
          "Mang trong mình vẻ đẹp hiện đại, tinh khôi và phong độ. Từng đường kim mũi chỉ là sự chắt chiu tinh hoa của thời trang Việt."
        </p>
      </div>
    </div>

    <div class="container-client shop-body-container">
      <!-- Nút mở bộ lọc trên Mobile -->
      <div class="mobile-filter-bar">
        <button class="btn-toggle-filter" @click="showMobileFilter = !showMobileFilter">
          <span>🔍 {{ showMobileFilter ? 'Đóng bộ lọc' : 'Mở bộ lọc sản phẩm' }}</span>
          <span class="filter-count" v-if="filteredProducts.length">({{ filteredProducts.length }})</span>
        </button>
      </div>

      <div class="shop-main-layout">
        <!-- 2. SIDEBAR BỘ LỌC (TÁCH THÀNH COMPONENT) -->
        <BoLocSanPham
          :show-mobile="showMobileFilter"
          v-model:filterKeyword="filterKeyword"
          v-model:priceFrom="priceFrom"
          v-model:priceTo="priceTo"
          v-model:selectedCategory="selectedCategory"
          v-model:selectedBrand="selectedBrand"
          v-model:selectedColor="selectedColor"
          v-model:selectedSize="selectedSize"
          :categories="categories"
          :brands="brands"
          :colors="colors"
          :sizes="sizes"
          @closeMobile="showMobileFilter = false"
          @reset="resetFilters"
        />

        <!-- 3. KHU VỰC HIỂN THỊ SẢN PHẨM -->
        <main class="products-display-area">
          <!-- Thanh sắp xếp & Tab danh mục -->
          <ThanhSapXep
            :total-count="filteredProducts.length"
            v-model:sortBy="sortBy"
            v-model:selectedCategory="selectedCategory"
            :categories="categories"
          />

          <!-- Loading state -->
          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>Đang tải danh sách sản phẩm...</p>
          </div>

          <!-- Empty state -->
          <div v-else-if="filteredProducts.length === 0" class="empty-state-box">
            <div class="empty-icon">🔍</div>
            <h3>Không tìm thấy sản phẩm nào phù hợp</h3>
            <p>Vui lòng thử tìm từ khóa khác hoặc điều chỉnh lại khoảng giá bộ lọc.</p>
            <button @click="resetFilters" class="btn-reset-brand">Xóa bộ lọc</button>
          </div>

          <!-- Product Grid (Dùng chung TheSanPham) -->
          <div v-else class="products-grid">
            <TheSanPham
              v-for="p in paginatedProducts"
              :key="p.id"
              :product="p"
            />
          </div>

          <!-- Phân trang -->
          <div v-if="totalPages > 1" class="pagination-wrapper">
            <button
              :disabled="currentPage === 1"
              @click="currentPage--; window.scrollTo({ top: 250, behavior: 'smooth' })"
              class="page-btn nav-btn"
            >
              &larr; Trước
            </button>
            <button
              v-for="p in totalPages"
              :key="p"
              class="page-btn num-btn"
              :class="{ active: currentPage === p }"
              @click="currentPage = p; window.scrollTo({ top: 250, behavior: 'smooth' })"
            >
              {{ p }}
            </button>
            <button
              :disabled="currentPage === totalPages"
              @click="currentPage++; window.scrollTo({ top: 250, behavior: 'smooth' })"
              class="page-btn nav-btn"
            >
              Sau &rarr;
            </button>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shop-page-wrap {
  background-color: #f8fafc;
  min-height: 100vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

.shop-header-intro {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #2b394a 100%);
  color: #ffffff;
  padding: 45px 0 40px;
  border-bottom: 2px solid #ecd8af;
}

.container-client {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

@media (min-width: 1600px) {
  .container-client {
    max-width: 1540px;
    padding: 0 2rem;
  }
}

@media (max-width: 640px) {
  .container-client {
    padding: 0 1rem;
  }
}

.text-center {
  text-align: center;
}

.shop-title-main {
  font-size: 32px;
  font-weight: 800;
  margin: 0 0 10px;
  letter-spacing: -0.5px;
}

.shop-quote-sub {
  color: #cbd5e1;
  font-size: 15px;
  max-width: 700px;
  margin: 0 auto;
  line-height: 1.6;
}

.shop-body-container {
  margin-top: 30px;
}

.mobile-filter-bar {
  display: none;
  margin-bottom: 16px;
}

.btn-toggle-filter {
  width: 100%;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-weight: 700;
  color: #496883;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.shop-main-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 30px;
  align-items: start;
}

.products-display-area {
  min-width: 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 22px;
}

/* Loading & Empty */
.loading-state,
.empty-state-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
  color: #64748b;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e2e8f0;
  border-top-color: #d2a764;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.btn-reset-brand {
  margin-top: 16px;
  background: #d2a764;
  color: #1e293b;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  font-weight: 700;
  cursor: pointer;
}

/* Phân trang */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 40px;
}

.page-btn {
  padding: 8px 14px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #496883;
  color: #496883;
}

.page-btn.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 992px) {
  .shop-main-layout {
    grid-template-columns: 1fr;
  }
  .mobile-filter-bar {
    display: block;
  }
  .products-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 580px) {
  .products-grid {
    grid-template-columns: 1fr;
  }
  .shop-title-main {
    font-size: 24px;
  }
}
</style>
