<script setup>
const props = defineProps({
  showMobile: {
    type: Boolean,
    default: false
  },
  filterKeyword: {
    type: String,
    default: ''
  },
  priceFrom: {
    type: [String, Number],
    default: ''
  },
  priceTo: {
    type: [String, Number],
    default: ''
  },
  selectedCategory: {
    type: [String, Number],
    default: ''
  },
  selectedBrand: {
    type: [String, Number],
    default: ''
  },
  selectedColor: {
    type: [String, Number],
    default: ''
  },
  selectedSize: {
    type: [String, Number],
    default: ''
  },
  categories: {
    type: Array,
    default: () => []
  },
  brands: {
    type: Array,
    default: () => []
  },
  colors: {
    type: Array,
    default: () => []
  },
  sizes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits([
  'update:filterKeyword',
  'update:priceFrom',
  'update:priceTo',
  'update:selectedCategory',
  'update:selectedBrand',
  'update:selectedColor',
  'update:selectedSize',
  'closeMobile',
  'reset'
])
</script>

<template>
  <aside class="filter-sidebar-card" :class="{ 'mobile-show': showMobile }">
    <div class="filter-header-flex">
      <h3 class="filter-card-heading">Bộ Lọc Sản Phẩm</h3>
      <button class="mobile-close-btn" @click="emit('closeMobile')">✕</button>
    </div>

    <!-- 1. Từ khóa -->
    <div class="filter-field">
      <label class="field-label">Tìm Kiếm</label>
      <div class="search-input-field">
        <span class="search-icon">🔍</span>
        <input
          :value="filterKeyword"
          @input="emit('update:filterKeyword', $event.target.value)"
          type="text"
          placeholder="Tên áo phông, mã..."
          class="form-control"
        />
      </div>
    </div>

    <!-- 2. Khoảng giá -->
    <div class="filter-field">
      <label class="field-label">Khoảng Giá (VNĐ)</label>
      <div class="price-range-inputs">
        <input
          :value="priceFrom"
          @input="emit('update:priceFrom', $event.target.value)"
          type="number"
          placeholder="Từ"
          class="form-control price-input"
        />
        <span class="range-dash">-</span>
        <input
          :value="priceTo"
          @input="emit('update:priceTo', $event.target.value)"
          type="number"
          placeholder="Đến"
          class="form-control price-input"
        />
      </div>
    </div>

    <!-- 3. Danh mục -->
    <div class="filter-field">
      <label class="field-label">Danh Mục</label>
      <select
        :value="selectedCategory"
        @change="emit('update:selectedCategory', $event.target.value)"
        class="form-control"
      >
        <option value="">-- Tất cả danh mục --</option>
        <option
          v-for="cat in categories"
          :key="cat.id"
          :value="cat.tenDanhMuc"
        >
          {{ cat.tenDanhMuc }}
        </option>
      </select>
    </div>

    <!-- 4. Thương hiệu -->
    <div class="filter-field">
      <label class="field-label">Thương Hiệu</label>
      <select
        :value="selectedBrand"
        @change="emit('update:selectedBrand', $event.target.value)"
        class="form-control"
      >
        <option value="">-- Tất cả thương hiệu --</option>
        <option
          v-for="b in brands"
          :key="b.id"
          :value="b.id"
        >
          {{ b.tenThuongHieu }}
        </option>
      </select>
    </div>

    <!-- 5. Kích cỡ -->
    <div class="filter-field">
      <label class="field-label">Kích Cỡ (Size)</label>
      <div class="filter-chips-grid">
        <button
          class="chip-btn"
          :class="{ active: !selectedSize }"
          @click="emit('update:selectedSize', '')"
        >
          Tất cả
        </button>
        <button
          v-for="sz in sizes"
          :key="sz.id"
          class="chip-btn"
          :class="{ active: selectedSize == sz.id }"
          @click="emit('update:selectedSize', sz.id)"
        >
          {{ sz.tenKichCo }}
        </button>
      </div>
    </div>

    <!-- 6. Màu sắc -->
    <div class="filter-field">
      <label class="field-label">Màu Sắc</label>
      <div class="filter-chips-grid">
        <button
          class="chip-btn"
          :class="{ active: !selectedColor }"
          @click="emit('update:selectedColor', '')"
        >
          Tất cả
        </button>
        <button
          v-for="cl in colors"
          :key="cl.id"
          class="chip-btn"
          :class="{ active: selectedColor == cl.id }"
          @click="emit('update:selectedColor', cl.id)"
        >
          {{ cl.tenMauSac }}
        </button>
      </div>
    </div>

    <!-- Nút hành động -->
    <div class="filter-actions-row">
      <button @click="emit('reset')" class="btn-reset-filters">
        Đặt lại bộ lọc
      </button>
    </div>
  </aside>
</template>

<style scoped>
.filter-sidebar-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(73, 104, 131, 0.05);
}

.filter-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #ecd8af;
}

.filter-card-heading {
  font-size: 17px;
  font-weight: 800;
  color: #496883;
  margin: 0;
}

.mobile-close-btn {
  display: none;
  background: transparent;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #64748b;
}

.filter-field {
  margin-bottom: 18px;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.search-input-field {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  color: #94a3b8;
}

.search-input-field .form-control {
  padding-left: 32px;
}

.form-control {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  box-sizing: border-box;
  background-color: #f8fafc;
  transition: all 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #496883;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
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

.filter-chips-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.chip-btn:hover {
  border-color: #d2a764;
  color: #1e293b;
}

.chip-btn.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.filter-actions-row {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

.btn-reset-filters {
  width: 100%;
  padding: 10px 0;
  background: transparent;
  border: 1.5px solid #cbd5e1;
  color: #64748b;
  border-radius: 8px;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-reset-filters:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #dc2626;
}

@media (max-width: 992px) {
  .filter-sidebar-card {
    display: none;
  }
  .filter-sidebar-card.mobile-show {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    border-radius: 0;
    overflow-y: auto;
    padding: 20px;
  }
  .mobile-close-btn {
    display: block;
  }
}
</style>

