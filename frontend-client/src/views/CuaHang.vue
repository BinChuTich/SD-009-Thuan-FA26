<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/format'
import { CATEGORIES_CATALOG, getMergedProducts } from '@/utils/productsCatalog.js'

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

// Bộ lọc (khớp với frame_shop.jpg trong video)
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

const handleImgError = (e) => {
  e.target.src = '/images/products/default_tshirt.jpg'
}

const loadAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      brands.value = res.data.thuongHieu || []
      sizes.value = res.data.kichCo || []
      colors.value = res.data.mauSac || []

      const existingCatNames = new Set((res.data.danhMuc || []).map(d => d.tenDanhMuc))
      const enrichedCats = [...(res.data.danhMuc || [])]
      CATEGORIES_CATALOG.forEach(c => {
        if (!existingCatNames.has(c.name)) {
          enrichedCats.push({ id: c.id, tenDanhMuc: c.name })
        }
      })
      categories.value = enrichedCats
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
    list = list.filter(p => p.idDanhMuc == selectedCategory.value || p.tenDanhMuc === selectedCategory.value)
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

const goToDetail = (id) => {
  router.push(`/san-pham/${id}`)
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
    <!-- 1. TIÊU ĐỀ BỘ SƯU TẬP (MÀU #496883 & #d2a764) -->
    <div class="shop-header-intro">
      <div class="container-client text-center">
        <h1 class="shop-title-main">Bộ Sưu Tập Áo Phông Nam</h1>
        <p class="shop-quote-sub">
          "Mang trong mình vẻ đẹp hiện đại, tinh khôi và phong độ. Từng đường kim mũi chỉ là sự chắt chiu tinh hoa của thời trang Việt."
        </p>
      </div>
    </div>

    <div class="container-client shop-body-container">
      <!-- Nút mở bộ lọc nhanh trên Mobile -->
      <div class="mobile-filter-bar">
        <button class="btn-toggle-filter" @click="showMobileFilter = !showMobileFilter">
          <span>🔍 {{ showMobileFilter ? 'Đóng bộ lọc' : 'Mở bộ lọc sản phẩm' }}</span>
          <span class="filter-count" v-if="filteredProducts.length">({{ filteredProducts.length }})</span>
        </button>
      </div>

      <div class="shop-main-layout">
        <!-- 2. SIDEBAR BỘ LỌC (MATCH frame_shop.jpg + RESPONSIVE MOBILE) -->
        <aside class="filter-sidebar-card" :class="{ 'mobile-show': showMobileFilter }">
          <div class="filter-header-flex">
            <h3 class="filter-card-heading">Bộ lọc sản phẩm</h3>
            <button class="mobile-close-btn" @click="showMobileFilter = false">✕</button>
          </div>

          <!-- Từ khóa -->
          <div class="filter-field">
            <label class="field-label">Từ khóa</label>
            <div class="search-input-field">
              <span class="search-icon">🔍</span>
              <input
                v-model="filterKeyword"
                type="text"
                placeholder="Tên sản phẩm..."
                class="form-control"
              />
            </div>
          </div>

          <!-- Khoảng giá: Giá Từ - Giá Đến side-by-side -->
          <div class="filter-field">
            <label class="field-label">Khoảng giá</label>
            <div class="price-range-inputs">
              <input
                v-model="priceFrom"
                type="number"
                placeholder="Giá Từ"
                class="form-control price-input"
              />
              <span class="range-dash">-</span>
              <input
                v-model="priceTo"
                type="number"
                placeholder="Giá Đến"
                class="form-control price-input"
              />
            </div>
          </div>

          <!-- Loại sản phẩm (Danh mục) -->
          <div class="filter-field">
            <label class="field-label">Loại sản phẩm (Danh mục)</label>
            <select v-model="selectedCategory" class="form-control select-dropdown">
              <option value="">Tất cả danh mục</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">
                {{ c.tenDanhMuc }}
              </option>
            </select>
          </div>

          <!-- Thương hiệu -->
          <div class="filter-field">
            <label class="field-label">Thương hiệu</label>
            <select v-model="selectedBrand" class="form-control select-dropdown">
              <option value="">Tất cả thương hiệu</option>
              <option v-for="b in brands" :key="b.id" :value="b.id">
                {{ b.tenThuongHieu }}
              </option>
            </select>
          </div>

          <!-- Màu sắc -->
          <div class="filter-field">
            <label class="field-label">Màu sắc</label>
            <select v-model="selectedColor" class="form-control select-dropdown">
              <option value="">Tất cả màu sắc</option>
              <option v-for="cl in colors" :key="cl.id" :value="cl.id">
                {{ cl.tenMauSac }}
              </option>
            </select>
          </div>

          <!-- Kích cỡ -->
          <div v-if="sizes.length > 0" class="filter-field">
            <label class="field-label">Kích cỡ</label>
            <select v-model="selectedSize" class="form-control select-dropdown">
              <option value="">Tất cả kích cỡ</option>
              <option v-for="s in sizes" :key="s.id" :value="s.id">
                {{ s.tenKichCo }}
              </option>
            </select>
          </div>

          <!-- Nút Xóa toàn bộ bộ lọc -->
          <button @click="resetFilters" class="btn-clear-all-filter">
            ↺ Xóa toàn bộ bộ lọc
          </button>
        </aside>

        <!-- 3. DANH SÁCH SẢN PHẨM -->
        <main class="products-catalog-main">
          <!-- Dải chọn danh mục nhanh dạng viên thuốc -->
          <div class="store-category-pills">
            <button
              class="pill-btn"
              :class="{ active: !selectedCategory }"
              @click="selectedCategory = ''; currentPage = 1"
            >
              Tất cả ({{ products.length }})
            </button>
            <button
              v-for="cat in categories"
              :key="cat.id"
              class="pill-btn"
              :class="{ active: selectedCategory == cat.id || selectedCategory === cat.tenDanhMuc }"
              @click="selectedCategory = cat.tenDanhMuc; currentPage = 1"
            >
              {{ cat.tenDanhMuc }}
            </button>
          </div>

          <!-- Thanh hiển thị số lượng & Sắp xếp -->
          <div class="catalog-top-bar">
            <div class="count-text">
              Tìm thấy <strong>{{ filteredProducts.length }}</strong> sản phẩm
            </div>
            <div class="sort-wrapper">
              <label>Sắp xếp:</label>
              <select v-model="sortBy" class="select-sort">
                <option value="newest">Mới nhất</option>
                <option value="price-asc">Giá: Thấp đến cao</option>
                <option value="price-desc">Giá: Cao đến thấp</option>
                <option value="name-asc">Tên: A - Z</option>
              </select>
            </div>
          </div>

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

          <!-- Product Grid (3 cột desktop, 2 cột mobile) -->
          <div v-else class="video-style-grid">
            <div
              v-for="p in paginatedProducts"
              :key="p.id"
              class="video-product-card"
              @click="goToDetail(p.id)"
            >
              <div class="card-img-holder">
                <img
                  :src="p.anhDaiDien || (p.hinhAnhs && p.hinhAnhs[0]) || '/images/products/default_tshirt.jpg'"
                  :alt="p.tenSanPham"
                  @error="handleImgError"
                  loading="lazy"
                />
                <span v-if="p.coGiamGia && p.phanTramGiam > 0" class="discount-badge-red">
                  -{{ Math.round(p.phanTramGiam) }}%
                </span>
              </div>

              <div class="card-caption">
                <span class="card-cat-badge">{{ p.tenDanhMuc || 'Áo phông' }}</span>
                <h3 class="product-name" :title="p.tenSanPham">{{ p.tenSanPham }}</h3>
                
                <div class="price-container">
                  <template v-if="p.coGiamGia && p.minGiaSauGiam && p.minGiaSauGiam < p.minGia">
                    <span class="price-red">
                      {{ formatCurrency(p.minGiaSauGiam) }}
                    </span>
                    <span class="price-struck">
                      {{ formatCurrency(p.minGia) }}
                    </span>
                  </template>
                  <template v-else>
                    <span class="price-red">
                      {{ formatCurrency(p.minGia || 0) }}
                    </span>
                  </template>
                </div>

                <button class="btn-view-product">Xem chi tiết</button>
              </div>
            </div>
          </div>

          <!-- Phân trang -->
          <div v-if="totalPages > 1" class="shop-pagination">
            <button
              :disabled="currentPage === 1"
              @click="currentPage--"
              class="page-nav-btn"
            >
              &larr; Trước
            </button>
            <div class="page-numbers-row">
              <button
                v-for="pg in totalPages"
                :key="pg"
                class="num-btn"
                :class="{ active: currentPage === pg }"
                @click="currentPage = pg"
              >
                {{ pg }}
              </button>
            </div>
            <button
              :disabled="currentPage === totalPages"
              @click="currentPage++"
              class="page-nav-btn"
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
  min-height: 85vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.25rem;
}

/* 1. Header intro */
.shop-header-intro {
  background-color: #ffffff;
  padding: 36px 0 28px;
  border-bottom: 2px solid #ecd8af;
}

.shop-title-main {
  font-size: 30px;
  font-weight: 800;
  color: #496883;
  margin: 0 0 8px;
}

.shop-quote-sub {
  font-size: 13.5px;
  color: #64748b;
  max-width: 720px;
  margin: 0 auto;
  line-height: 1.6;
}

.shop-body-container {
  margin-top: 28px;
}

/* Mobile Filter Bar */
.mobile-filter-bar {
  display: none;
  margin-bottom: 16px;
}

.btn-toggle-filter {
  width: 100%;
  padding: 10px 16px;
  background: #ffffff;
  border: 1.5px solid #496883;
  border-radius: 8px;
  color: #496883;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.filter-count {
  background: #496883;
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.shop-main-layout {
  display: grid;
  grid-template-columns: 290px 1fr;
  gap: 28px;
  align-items: flex-start;
}

/* 2. Sidebar Filter */
.filter-sidebar-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 22px;
  box-shadow: 0 2px 10px rgba(73, 104, 131, 0.04);
  position: sticky;
  top: 90px;
}

.filter-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.filter-card-heading {
  font-size: 15px;
  font-weight: 800;
  color: #496883;
  margin: 0;
}

.mobile-close-btn {
  display: none;
  background: none;
  border: none;
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
}

.filter-field {
  margin-bottom: 16px;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  margin-bottom: 6px;
}

.search-input-field {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  font-size: 12px;
  color: #94a3b8;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  background: #f8fafc;
  box-sizing: border-box;
  transition: all 0.2s;
  color: #1e293b;
}

.search-input-field .form-control {
  padding-left: 30px;
}

.form-control:focus {
  border-color: #496883;
  background: #ffffff;
}

.select-dropdown {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23496883' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 28px;
}

.price-range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-input {
  flex: 1;
}

.range-dash {
  color: #94a3b8;
  font-weight: 700;
}

.btn-clear-all-filter {
  width: 100%;
  padding: 10px 0;
  background-color: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  transition: background 0.2s;
}

.btn-clear-all-filter:hover {
  background-color: #364f66;
}

/* 3. Products Catalog Main */
.products-catalog-main {
  min-width: 0;
}

.store-category-pills {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 18px;
  border-bottom: 1px solid #e2e8f0;
}

.pill-btn {
  background: #f8fafc;
  color: #496883;
  border: 1.5px solid #cbd5e1;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.pill-btn:hover {
  border-color: #496883;
  background: #ffffff;
}

.pill-btn.active {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.2);
}

.card-cat-badge {
  font-size: 10px;
  font-weight: 700;
  color: #d2a764;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.catalog-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  padding: 12px 18px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-bottom: 22px;
}

.count-text {
  font-size: 13px;
  color: #64748b;
}

.count-text strong {
  color: #496883;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.select-sort {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  font-size: 12px;
  outline: none;
  background: #ffffff;
  cursor: pointer;
}

/* 3 columns grid on desktop */
.video-style-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.video-product-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
}

.video-product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(73, 104, 131, 0.12);
  border-color: #d2a764;
}

.card-img-holder {
  position: relative;
  width: 100%;
  padding-top: 125%;
  background: #f8fafc;
  overflow: hidden;
}

.card-img-holder img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.video-product-card:hover .card-img-holder img {
  transform: scale(1.05);
}

.discount-badge-red {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 7px;
  border-radius: 4px;
  z-index: 2;
}

.card-caption {
  padding: 16px 12px 14px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.product-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.price-container {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.price-red {
  color: #ef4444;
  font-size: 15px;
  font-weight: 800;
}

.price-struck {
  color: #94a3b8;
  font-size: 12px;
  text-decoration: line-through;
}

.btn-view-product {
  background: #f1f5f9;
  color: #496883;
  border: 1px solid #e2e8f0;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  width: fit-content;
}

.video-product-card:hover .btn-view-product {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.shop-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 36px;
}

.page-nav-btn {
  padding: 8px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #496883;
  cursor: pointer;
}

.page-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers-row {
  display: flex;
  gap: 6px;
}

.num-btn {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #496883;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.num-btn.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.loading-state, .empty-state-box {
  text-align: center;
  padding: 50px 0;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.btn-reset-brand {
  margin-top: 14px;
  padding: 8px 18px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

/* RESPONSIVE CHO ĐIỆN THOẠI & TABLET */
@media (max-width: 900px) {
  .mobile-filter-bar {
    display: block;
  }
  .shop-main-layout {
    grid-template-columns: 1fr;
  }
  /* Ẩn sidebar trên mobile nếu chưa mở */
  .filter-sidebar-card {
    display: none;
    position: static;
    margin-bottom: 20px;
  }
  .filter-sidebar-card.mobile-show {
    display: block;
    animation: fadeIn 0.2s ease-out;
  }
  .mobile-close-btn {
    display: block;
  }
  .video-style-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 600px) {
  .shop-title-main {
    font-size: 24px;
  }
  .card-caption {
    padding: 12px 8px 10px;
  }
  .product-name {
    font-size: 12px;
  }
  .price-red {
    font-size: 13px;
  }
  .price-struck {
    font-size: 11px;
  }
  .btn-view-product {
    font-size: 11px;
    padding: 4px 10px;
  }
}
</style>
