<template>
  <div class="product-page-container">
    <!-- 1. Thanh tiêu đề trang & Nút thêm mới -->
    <div class="top-title-card">
      <div class="title-left">
        <div class="breadcrumb">Quản lý sản phẩm / Danh sách sản phẩm</div>
        <h2 class="page-title">Quản Lý Sản Phẩm</h2>
      </div>
      <div class="title-actions">
        <button class="btn btn-export">
          <span>📤</span> Xuất Excel
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          <span>＋</span> Thêm Sản Phẩm Mới
        </button>
      </div>
    </div>

    <!-- 2. Thẻ thống kê tổng quan (Cards) -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrap blue-bg">👕</div>
        <div class="stat-info">
          <span>Tổng mẫu sản phẩm</span>
          <strong>128</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap gold-bg">📦</div>
        <div class="stat-info">
          <span>Tổng tồn kho (chiếc)</span>
          <strong>3.420</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap green-bg">✓</div>
        <div class="stat-info">
          <span>Đang kinh doanh</span>
          <strong>115</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap gray-bg">⏸</div>
        <div class="stat-info">
          <span>Tạm ngưng / Hết hàng</span>
          <strong>13</strong>
        </div>
      </div>
    </div>

    <!-- 3. Khung Bộ Lọc & Tìm Kiếm -->
    <div class="custom-card filter-card">
      <div class="card-head-title">
        <span class="icon-head">🌪️</span>
        <h3>Bộ Lọc Tìm Kiếm</h3>
      </div>

      <div class="filter-grid">
        <!-- Tìm kiếm từ khóa -->
        <div class="form-group search-col">
          <label>Từ khóa tìm kiếm</label>
          <div class="input-search">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                v-model="filters.keyword"
                placeholder="Tìm theo tên sản phẩm, mã SP..."
            />
          </div>
        </div>

        <!-- Danh mục -->
        <div class="form-group">
          <label>Danh mục</label>
          <select v-model="filters.category">
            <option value="">Tất cả danh mục</option>
            <option value="Áo thun basic">Áo thun Basic</option>
            <option value="Áo Polo">Áo Polo</option>
            <option value="Áo Oversize">Áo Oversize</option>
            <option value="Áo Tanktop">Áo Tanktop</option>
          </select>
        </div>

        <!-- Thương hiệu -->
        <div class="form-group">
          <label>Thương hiệu</label>
          <select v-model="filters.brand">
            <option value="">Tất cả thương hiệu</option>
            <option value="FF Original">FF Original</option>
            <option value="FF Premium">FF Premium</option>
            <option value="Cotton Plus">Cotton Plus</option>
          </select>
        </div>

        <!-- Trạng thái -->
        <div class="form-group">
          <label>Trạng thái</label>
          <select v-model="filters.status">
            <option value="">Tất cả trạng thái</option>
            <option value="active">Đang kinh doanh</option>
            <option value="inactive">Tạm ngưng</option>
          </select>
        </div>
      </div>

      <!-- Hàng nút lọc -->
      <div class="filter-actions-row">
        <button class="btn btn-reset" @click="resetFilters">Làm Mới Bộ Lọc</button>
        <button class="btn btn-primary">Tìm Kiếm</button>
      </div>
    </div>

    <!-- 4. Khung Bảng Danh Sách Sản Phẩm -->
    <div class="custom-card list-card">
      <div class="card-head-title between">
        <div class="title-with-icon">
          <div class="doc-icon-wrap">
            <span>👕</span>
          </div>
          <h3>Danh Sách Sản Phẩm ({{ filteredProducts.length }})</h3>
        </div>
        <div class="quick-view-toggle">
          <button class="view-btn active">Bảng chi tiết</button>
        </div>
      </div>

      <div class="table-responsive">
        <table class="product-table">
          <thead>
          <tr>
            <th style="width: 40px; text-align: center">STT</th>
            <th style="width: 70px; text-align: center">Hình ảnh</th>
            <th>Mã SP</th>
            <th>Tên sản phẩm</th>
            <th>Danh mục</th>
            <th>Chất liệu</th>
            <th style="text-align: center">Biến thể</th>
            <th style="text-align: center">Tổng kho</th>
            <th>Khoảng giá</th>
            <th style="text-align: center">Trạng thái</th>
            <th style="width: 110px; text-align: center">Hành động</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(prod, idx) in filteredProducts" :key="prod.id">
            <td style="text-align: center">{{ idx + 1 }}</td>

            <!-- Ảnh Thumbnail -->
            <td style="text-align: center">
              <div class="prod-img-box" :style="{ backgroundColor: prod.bgColor }">
                <span class="tshirt-emoji">{{ prod.icon }}</span>
              </div>
            </td>

            <!-- Mã SP -->
            <td><b class="text-blue">{{ prod.code }}</b></td>

            <!-- Tên SP -->
            <td>
              <div class="prod-name-wrap">
                <strong class="prod-title">{{ prod.name }}</strong>
                <small class="text-muted">Brand: {{ prod.brand }}</small>
              </div>
            </td>

            <!-- Danh mục -->
            <td>
              <span class="cat-tag">{{ prod.category }}</span>
            </td>

            <!-- Chất liệu -->
            <td>{{ prod.material }}</td>

            <!-- Số biến thể -->
            <td style="text-align: center">
              <span class="variant-pill">{{ prod.variantCount }} mẫu</span>
            </td>

            <!-- Tồn kho -->
            <td style="text-align: center">
              <b :class="prod.stock <= 10 ? 'text-danger' : 'text-stock'">{{ prod.stock }}</b>
            </td>

            <!-- Khoảng giá -->
            <td>
              <span class="price-text">{{ prod.priceRange }}</span>
            </td>

            <!-- Trạng thái kinh doanh -->
            <td style="text-align: center">
              <label class="switch">
                <input type="checkbox" v-model="prod.isActive" />
                <span class="slider"></span>
              </label>
            </td>

            <!-- Các nút hành động -->
            <td style="text-align: center">
              <div class="action-btn-group">
                <router-link to="/bien-the-san-pham" class="act-btn" title="Quản lý biến thể">
                  ⚙️
                </router-link>
                <button class="act-btn" title="Chỉnh sửa" @click="editProduct(prod)">
                  ✏️
                </button>
                <button class="act-btn btn-del" title="Xóa sản phẩm">
                  🗑️
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang -->
      <div class="pagination-bar">
        <div class="page-info">
          Hiển thị 1 - {{ filteredProducts.length }} trên tổng số 128 sản phẩm
        </div>
        <div class="page-controls">
          <button class="pg-btn" disabled>‹</button>
          <button class="pg-btn active">1</button>
          <button class="pg-btn">2</button>
          <button class="pg-btn">3</button>
          <button class="pg-btn">›</button>
        </div>
      </div>
    </div>

    <!-- 5. Modal Thêm / Chỉnh Sửa Sản Phẩm Nhanh -->
    <div class="modal-overlay" v-if="showModal" @click.self="showModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ isEditing ? 'Chỉnh Sửa Sản Phẩm' : 'Thêm Sản Phẩm Mới' }}</h3>
          <button class="modal-close" @click="showModal = false">✕</button>
        </div>

        <div class="modal-body">
          <div class="form-row-2">
            <div class="form-group">
              <label>Mã sản phẩm *</label>
              <input type="text" v-model="formData.code" placeholder="VD: FF-TS01" />
            </div>
            <div class="form-group">
              <label>Tên sản phẩm *</label>
              <input type="text" v-model="formData.name" placeholder="VD: Áo Thun Cổ Tròn Regular" />
            </div>
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Danh mục</label>
              <select v-model="formData.category">
                <option value="Áo thun basic">Áo thun Basic</option>
                <option value="Áo Polo">Áo Polo</option>
                <option value="Áo Oversize">Áo Oversize</option>
              </select>
            </div>
            <div class="form-group">
              <label>Thương hiệu</label>
              <select v-model="formData.brand">
                <option value="FF Original">FF Original</option>
                <option value="FF Premium">FF Premium</option>
              </select>
            </div>
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Chất liệu</label>
              <select v-model="formData.material">
                <option value="100% Cotton Compact">100% Cotton Compact</option>
                <option value="Cotton 2 chiều">Cotton 2 chiều</option>
                <option value="CVC 65/35">CVC 65/35</option>
              </select>
            </div>
            <div class="form-group">
              <label>Giá tham chiếu cơ bản (đ)</label>
              <input type="number" v-model="formData.basePrice" placeholder="VD: 180000" />
            </div>
          </div>

          <div class="form-group">
            <label>Mô tả chi tiết</label>
            <textarea rows="3" placeholder="Nhập tóm tắt mô tả sản phẩm, dáng áo, cách bảo quản..."></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-reset" @click="showModal = false">Hủy bỏ</button>
          <button class="btn btn-primary" @click="saveProduct">Lưu Sản Phẩm</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const showModal = ref(false)
const isEditing = ref(false)

const filters = ref({
  keyword: '',
  category: '',
  brand: '',
  status: ''
})

const formData = ref({
  code: '',
  name: '',
  category: 'Áo thun basic',
  brand: 'FF Original',
  material: '100% Cotton Compact',
  basePrice: 180000
})

const products = ref([
  {
    id: 1,
    code: 'FF-TS001',
    name: 'Áo Thun FF Basic Signature',
    brand: 'FF Original',
    category: 'Áo thun basic',
    material: '100% Cotton Compact',
    variantCount: 8,
    stock: 245,
    priceRange: '180.000 - 220.000 ₫',
    isActive: true,
    icon: '👕',
    bgColor: '#eaf1f5'
  },
  {
    id: 2,
    code: 'FF-POLO02',
    name: 'Áo Polo Nam Classic Khuy Gỗ',
    brand: 'FF Premium',
    category: 'Áo Polo',
    material: 'Pique Cotton',
    variantCount: 6,
    stock: 120,
    priceRange: '290.000 - 320.000 ₫',
    isActive: true,
    icon: '👔',
    bgColor: '#fcf3e6'
  },
  {
    id: 3,
    code: 'FF-OVER03',
    name: 'Áo Thun Form Rộng FF Streetwear',
    brand: 'FF Original',
    category: 'Áo Oversize',
    material: 'Cotton 2 chiều 250gsm',
    variantCount: 12,
    stock: 8,
    priceRange: '250.000 - 280.000 ₫',
    isActive: true,
    icon: '👕',
    bgColor: '#ebf5ef'
  },
  {
    id: 4,
    code: 'FF-TANK04',
    name: 'Áo Ba Lỗ Thể Thao Nam Breathable',
    brand: 'FF Original',
    category: 'Áo Tanktop',
    material: 'Thun Lạnh Spandex',
    variantCount: 4,
    stock: 0,
    priceRange: '140.000 - 160.000 ₫',
    isActive: false,
    icon: '🎽',
    bgColor: '#f7eee1'
  }
])

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    const matchKw = !filters.value.keyword ||
        p.name.toLowerCase().includes(filters.value.keyword.toLowerCase()) ||
        p.code.toLowerCase().includes(filters.value.keyword.toLowerCase())
    const matchCat = !filters.value.category || p.category === filters.value.category
    const matchBrand = !filters.value.brand || p.brand === filters.value.brand
    const matchStatus = !filters.value.status ||
        (filters.value.status === 'active' ? p.isActive : !p.isActive)
    return matchKw && matchCat && matchBrand && matchStatus
  })
})

const resetFilters = () => {
  filters.value = {
    keyword: '',
    category: '',
    brand: '',
    status: ''
  }
}

const openCreateModal = () => {
  isEditing.value = false
  formData.value = {
    code: `FF-TS0${products.value.length + 1}`,
    name: '',
    category: 'Áo thun basic',
    brand: 'FF Original',
    material: '100% Cotton Compact',
    basePrice: 180000
  }
  showModal.value = true
}

const editProduct = (prod) => {
  isEditing.value = true
  formData.value = { ...prod, basePrice: 200000 }
  showModal.value = true
}

const saveProduct = () => {
  if (!formData.value.name || !formData.value.code) {
    alert('Vui lòng nhập tên và mã sản phẩm!')
    return
  }
  if (!isEditing.value) {
    products.value.unshift({
      id: Date.now(),
      code: formData.value.code,
      name: formData.value.name,
      brand: formData.value.brand,
      category: formData.value.category,
      material: formData.value.material,
      variantCount: 1,
      stock: 50,
      priceRange: `${Number(formData.value.basePrice).toLocaleString('vi-VN')} ₫`,
      isActive: true,
      icon: '👕',
      bgColor: '#eaf1f5'
    })
  }
  showModal.value = false
}
</script>

<style scoped>
/* Toàn bộ vùng hiển thị chuẩn tone màu dự án */
.product-page-container {
  padding: 16px 20px 30px;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  color: var(--text, #3d4a50);
}

/* 1. Header trên cùng */
.top-title-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 12px 18px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.breadcrumb {
  font-size: 8px;
  color: #8f9798;
  margin-bottom: 2px;
}

.page-title {
  font-size: 13px;
  font-weight: 700;
  margin: 0;
  color: var(--blue, #496883);
}

.title-actions {
  display: flex;
  gap: 8px;
}

/* 2. Cards thống kê */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.02);
}

.stat-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 16px;
}

.blue-bg { background: #eaf1f5; color: #496883; }
.gold-bg { background: #fdf5e6; color: #d2a764; }
.green-bg { background: #edf6ef; color: #4c8a5a; }
.gray-bg { background: #f1f2f3; color: #7f898d; }

.stat-info span {
  display: block;
  font-size: 8px;
  color: #8c9597;
}

.stat-info strong {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #3b4b53;
  margin-top: 1px;
}

/* 3. Khung Card dùng chung */
.custom-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-head-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.card-head-title.between {
  justify-content: space-between;
}

.card-head-title h3 {
  font-size: 11px;
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

.icon-head {
  font-size: 13px;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-icon-wrap {
  width: 26px;
  height: 26px;
  background-color: #f7eee1;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 13px;
}

/* Bộ lọc */
.filter-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-size: 8px;
  font-weight: 700;
  color: #556268;
}

.form-group input,
.form-group select,
.form-group textarea {
  height: 32px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  padding: 0 10px;
  font-size: 8.5px;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-group textarea {
  height: auto;
  padding: 8px 10px;
  resize: vertical;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

.input-search {
  position: relative;
  display: flex;
  align-items: center;
}

.input-search input {
  width: 100%;
  padding-left: 28px;
}

.search-icon {
  position: absolute;
  left: 9px;
  font-size: 10px;
  color: #9aa0a0;
  pointer-events: none;
}

.filter-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px dashed #efeae0;
  padding-top: 12px;
}

/* Các loại nút bấm */
.btn {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-primary {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #38536b;
}

.btn-reset {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-reset:hover {
  background-color: #faeed7;
}

.btn-export {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #d2e5d6;
}
.btn-export:hover {
  background-color: #deede1;
}

/* 4. Bảng sản phẩm */
.table-responsive {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 8px;
}

.product-table th {
  background-color: #faf9f6;
  color: #8f9695;
  font-weight: 700;
  padding: 9px 12px;
  text-align: left;
  border-bottom: 1px solid #efede7;
}

.product-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f0eb;
  color: #556268;
}

.product-table tr:hover td {
  background-color: #fcfbf8;
}

.prod-img-box {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  margin: 0 auto;
  display: grid;
  place-items: center;
  border: 1px solid #ece7dd;
}

.tshirt-emoji {
  font-size: 16px;
}

.prod-name-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.prod-title {
  color: #364851;
  font-size: 8.5px;
}

.text-blue {
  color: var(--blue, #496883);
}

.text-muted {
  font-size: 7px;
  color: #9aa0a0;
}

.cat-tag {
  background: #f4f0e6;
  color: #8a7346;
  font-size: 7px;
  padding: 2px 7px;
  border-radius: 4px;
  font-weight: 600;
}

.variant-pill {
  background: #eaf1f5;
  color: #496883;
  font-size: 7px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 700;
}

.price-text {
  font-weight: 700;
  color: #3d4f58;
}

.text-stock {
  color: #467b57;
}

.text-danger {
  color: #d14949;
}

/* Switch on/off */
.switch {
  position: relative;
  display: inline-block;
  width: 28px;
  height: 16px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd3d6;
  border-radius: 16px;
  transition: 0.3s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 12px;
  width: 12px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  border-radius: 50%;
  transition: 0.3s;
}

.switch input:checked + .slider {
  background-color: var(--blue, #496883);
}

.switch input:checked + .slider:before {
  transform: translateX(12px);
}

/* Cột hành động */
.action-btn-group {
  display: flex;
  justify-content: center;
  gap: 5px;
}

.act-btn {
  width: 24px;
  height: 24px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 10px;
  display: inline-grid;
  place-items: center;
  text-decoration: none;
  transition: all 0.15s;
}

.act-btn:hover {
  background-color: #eaf1f4;
}

.btn-del:hover {
  background-color: #fdeeee;
}

/* Phân trang */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  font-size: 7.5px;
  color: #8e9596;
}

.page-controls {
  display: flex;
  gap: 4px;
}

.pg-btn {
  width: 22px;
  height: 22px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  font-size: 8px;
  cursor: pointer;
  display: grid;
  place-items: center;
  color: #647074;
}

.pg-btn.active {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border-color: var(--blue, #496883);
  font-weight: 700;
}

.pg-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Modal Popup */
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(30, 40, 48, 0.4);
  backdrop-filter: blur(2px);
  z-index: 999;
  display: grid;
  place-items: center;
  padding: 20px;
}

.modal-card {
  width: 100%;
  max-width: 520px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.modal-header {
  padding: 12px 18px;
  border-bottom: 1px solid #f0eae0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  font-size: 11.5px;
  font-weight: 700;
  margin: 0;
  color: var(--blue, #496883);
}

.modal-close {
  border: none;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
  color: #8c9597;
}

.modal-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.modal-footer {
  padding: 12px 18px;
  background-color: #faf9f6;
  border-top: 1px solid #f0eae0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>