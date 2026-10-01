<template>
  <div class="product-page-container">
    <!-- Tiêu đề trang -->
    <h2 class="main-page-title">QUẢN LÝ SẢN PHẨM</h2>

    <!-- Khối khung trắng bao bọc toàn bộ chức năng -->
    <div class="product-card">
      <!-- 1. Hàng tìm kiếm và các nút chính -->
      <div class="top-action-bar">
        <div class="search-group">
          <input
              type="text"
              v-model="filters.keyword"
              placeholder="Tìm kiếm"
              class="input-search"
          />
          <button class="btn btn-search" @click="handleSearch">Tìm kiếm</button>
          <button class="btn btn-refresh" @click="resetFilters">Làm mới</button>
        </div>

        <button class="btn btn-add" @click="openCreateProduct">
          + Thêm sản phẩm
        </button>
      </div>

      <!-- 2. Khối 8 tiêu chí lọc thuộc tính -->
      <div class="filter-attributes-section">
        <div class="funnel-icon-wrap">
          <span class="funnel-icon">🌪️</span>
        </div>

        <div class="attribute-dropdowns-grid">
          <select v-model="filters.brand" class="filter-pill-select">
            <option value="">Thương Hiệu</option>
            <option value="FF T-shirt">FF T-shirt</option>
            <option value="Atino">Atino</option>
            <option value="Teelab">Teelab</option>
          </select>

          <select v-model="filters.origin" class="filter-pill-select">
            <option value="">Xuất Xứ</option>
            <option value="Việt Nam">Việt Nam</option>
            <option value="Hàn Quốc">Hàn Quốc</option>
          </select>

          <select v-model="filters.size" class="filter-pill-select">
            <option value="">Size</option>
            <option value="S">S</option>
            <option value="M">M</option>
            <option value="L">L</option>
            <option value="XL">XL</option>
          </select>

          <select v-model="filters.color" class="filter-pill-select">
            <option value="">Màu sắc</option>
            <option value="Đen">Đen</option>
            <option value="Nâu">Nâu</option>
            <option value="Trắng">Trắng</option>
          </select>

          <select v-model="filters.collar" class="filter-pill-select">
            <option value="">Cổ áo</option>
            <option value="Cổ tròn">Cổ tròn</option>
            <option value="Cổ bẻ">Cổ bẻ (Polo)</option>
          </select>

          <select v-model="filters.sleeve" class="filter-pill-select">
            <option value="">Tay áo</option>
            <option value="Cộc tay">Cộc tay</option>
            <option value="Dài tay">Dài tay</option>
          </select>

          <select v-model="filters.material" class="filter-pill-select">
            <option value="">Chất liệu</option>
            <option value="Cotton Compact">Cotton Compact</option>
            <option value="CVC">CVC</option>
          </select>

          <select v-model="filters.pattern" class="filter-pill-select">
            <option value="">Họa tiết</option>
            <option value="Trơn">Trơn</option>
            <option value="In hình">In hình</option>
          </select>
        </div>
      </div>

      <!-- 3. Bảng dữ liệu sản phẩm -->
      <div class="table-responsive">
        <table class="product-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 140px; text-align: center;">Ảnh</th>
            <th>Tên sản phẩm</th>
            <th style="width: 130px;">Giá</th>
            <th style="width: 130px;">Ngày tạo</th>
            <th style="width: 140px; text-align: center;">Trạng thái</th>
            <th style="width: 80px; text-align: center;">Thao tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, index) in filteredProducts" :key="item.id">
            <!-- STT màu đỏ cam đặc trưng -->
            <td style="text-align: center;" class="stt-cell">{{ index + 1 }}</td>

            <!-- Ảnh sản phẩm -->
            <td style="text-align: center;">
              <div class="product-img-box">
                <img :src="item.image" :alt="item.name" />
              </div>
            </td>

            <!-- Tên sản phẩm -->
            <td class="product-name-cell">{{ item.name }}</td>

            <!-- Giá tiền in đậm -->
            <td class="price-cell"><b>{{ item.price }}</b></td>

            <!-- Ngày tạo -->
            <td class="date-cell">{{ item.createDate }}</td>

            <!-- Badge trạng thái: Kinh doanh -->
            <td style="text-align: center;">
                <span class="status-badge-business">
                  {{ item.status }}
                </span>
            </td>

            <!-- Cột thao tác: Xem chi tiết 👁 -->
            <td style="text-align: center;">
              <button class="btn-action-view" @click="viewDetail(item)" title="Xem chi tiết">
                👁
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. Thanh phân trang bo góc dưới bảng -->
      <div class="pagination-center">
        <button class="pg-box active">1</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const filters = ref({
  keyword: '',
  brand: '',
  origin: '',
  size: '',
  color: '',
  collar: '',
  sleeve: '',
  material: '',
  pattern: ''
})

const products = ref([
  {
    id: 1,
    name: 'Áo phông cộc tay',
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=300&auto=format&fit=crop&q=80',
    price: '350.000đ',
    createDate: '02/10/2026',
    status: 'Kinh doanh'
  },
  {
    id: 2,
    name: 'Atino form rộng',
    image: 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=300&auto=format&fit=crop&q=80',
    price: '450.000đ',
    createDate: '02/10/2026',
    status: 'Kinh doanh'
  },
  {
    id: 3,
    name: 'Áo phông form fit cơ thể',
    image: 'https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=300&auto=format&fit=crop&q=80',
    price: '500.000đ',
    createDate: '02/10/2026',
    status: 'Kinh doanh'
  },
  {
    id: 4,
    name: 'Áo phông form boxy',
    image: 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=300&auto=format&fit=crop&q=80',
    price: '400.000đ',
    createDate: '02/10/2026',
    status: 'Kinh doanh'
  }
])

const filteredProducts = computed(() => {
  return products.value.filter(p => {
    return !filters.value.keyword || p.name.toLowerCase().includes(filters.value.keyword.toLowerCase())
  })
})

const handleSearch = () => {
  // Lọc theo keyword
}

const resetFilters = () => {
  filters.value = {
    keyword: '',
    brand: '',
    origin: '',
    size: '',
    color: '',
    collar: '',
    sleeve: '',
    material: '',
    pattern: ''
  }
}

const openCreateProduct = () => {
  alert('Mở form tạo sản phẩm mới!')
}

const viewDetail = (item) => {
  alert(`Xem chi tiết sản phẩm: ${item.name}`)
}
</script>

<style scoped>
/* Toàn bộ vùng hiển thị trang */
.product-page-container {
  padding: 1.25rem 1.5rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

/* Tiêu đề trang in hoa to rõ */
.main-page-title {
  font-size: 1.2rem; /* ~19px */
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: 0.3px;
  margin: 0 0 1rem 0.2rem;
}

/* Khối Card trắng */
.product-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.75rem;
}

/* 1. Hàng tìm kiếm và nút thao tác trên cùng */
.top-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.search-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.input-search {
  width: 260px;
  height: 2.5rem; /* ~40px */
  background-color: #f1f4f8;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 0 1rem;
  font-size: 0.95rem; /* Chữ gõ tìm kiếm to rõ */
  color: #3d4a50;
  outline: none;
  transition: all 0.2s ease;
}

.input-search:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

/* Các loại nút bấm */
.btn {
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 8px;
  font-size: 0.92rem; /* Chữ nút bấm ~14.7px */
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  transition: all 0.2s ease;
}

/* Nút Tìm kiếm */
.btn-search {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-search:hover {
  background-color: #385269;
}

/* Nút Làm mới */
.btn-refresh {
  background-color: #d2a764;
  color: #ffffff;
}
.btn-refresh:hover {
  background-color: #be9453;
}

/* Nút + Thêm sản phẩm */
.btn-add {
  background-color: #edd9b8;
  color: #4a3e2e;
}
.btn-add:hover {
  background-color: #e4cda7;
}

/* 2. Cụm 8 tiêu chí lọc thuộc tính */
.filter-attributes-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.funnel-icon-wrap {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.attribute-dropdowns-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem 1rem;
}

.filter-pill-select {
  height: 2.35rem; /* ~38px */
  background-color: #f1f4f8;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.9rem; /* Chữ trong dropdown ~14.4px */
  color: #4f5a60;
  outline: none;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-pill-select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

/* 3. Bảng danh sách sản phẩm */
.table-responsive {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem; /* Tăng cỡ chữ bảng lên ~15.2px */
}

.product-table th {
  background-color: #f8fafc;
  color: #6c787f;
  font-weight: 700;
  padding: 1rem 1.1rem;
  text-align: left;
  border-bottom: 1px solid #edf1f5;
  font-size: 0.95rem;
  white-space: nowrap;
}

.product-table td {
  padding: 1.1rem; /* Tăng đệm dòng cách đều */
  border-bottom: 1px solid #f1f4f7;
  vertical-align: middle;
  color: #4a565c;
}

.product-table tr:hover td {
  background-color: #faf9f5;
}

/* STT số đỏ đậm */
.stt-cell {
  font-weight: 800;
  color: #e65228;
  font-size: 1.1rem;
}

/* Ô chứa ảnh sản phẩm */
.product-img-box {
  width: 90px;
  height: 90px;
  margin: 0 auto;
  border-radius: 6px;
  overflow: hidden;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.product-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Tên sản phẩm */
.product-name-cell {
  font-weight: 600;
  color: #2e3b40;
  font-size: 0.98rem;
}

/* Giá tiền */
.price-cell {
  color: #1f272b;
  font-size: 1rem;
}

/* Ngày tạo */
.date-cell {
  color: #6f7c82;
  font-size: 0.92rem;
}

/* Badge trạng thái "Kinh doanh" */
.status-badge-business {
  display: inline-block;
  padding: 0.45rem 1rem;
  background-color: #edd9b8;
  color: #453a29;
  font-weight: 700;
  border-radius: 8px;
  font-size: 0.88rem;
  white-space: nowrap;
}

/* Nút hành động con mắt */
.btn-action-view {
  width: 34px;
  height: 34px;
  border: 1px solid #e9e5db;
  background-color: #ffffff;
  border-radius: 6px;
  color: #496883;
  font-size: 1.15rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-action-view:hover {
  background-color: #eaf1f4;
  border-color: #496883;
  transform: scale(1.1);
}

/* 4. Thanh phân trang */
.pagination-center {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}

.pg-box {
  width: 36px;
  height: 36px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  color: #3d4a50;
  transition: all 0.2s;
}

.pg-box.active {
  border-color: #d2a764;
  color: #d2a764;
  background-color: #fdfaf3;
}

@media (max-width: 1000px) {
  .top-action-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .attribute-dropdowns-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 650px) {
  .attribute-dropdowns-grid {
    grid-template-columns: 1fr;
  }
}
</style>