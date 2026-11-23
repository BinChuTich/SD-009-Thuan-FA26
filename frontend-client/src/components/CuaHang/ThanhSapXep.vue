<script setup>
const props = defineProps({
  totalCount: {
    type: Number,
    default: 0
  },
  sortBy: {
    type: String,
    default: 'newest'
  },
  selectedCategory: {
    type: String,
    default: ''
  },
  categories: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits([
  'update:sortBy',
  'update:selectedCategory'
])

const isCategoryActive = (cat) => {
  if (!props.selectedCategory) return false
  const sel = String(props.selectedCategory).trim().toLowerCase()
  return (
    (cat.id && String(cat.id).toLowerCase() === sel) ||
    (cat.tenDanhMuc && String(cat.tenDanhMuc).trim().toLowerCase() === sel)
  )
}
</script>

<template>
  <div class="thanh-sap-xep-wrap">
    <!-- Category pills scrollable -->
    <div class="category-pills-bar">
      <button
        class="pill-btn"
        :class="{ active: !selectedCategory }"
        @click="emit('update:selectedCategory', '')"
      >
        Tất cả
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="pill-btn"
        :class="{ active: isCategoryActive(cat) }"
        @click="emit('update:selectedCategory', cat.tenDanhMuc)"
      >
        {{ cat.tenDanhMuc }}
      </button>
    </div>

    <!-- Count & Sắp xếp -->
    <div class="catalog-top-bar">
      <div class="count-text">
        Tìm thấy <strong>{{ totalCount }}</strong> sản phẩm
      </div>
      <div class="sort-wrapper">
        <label>Sắp xếp:</label>
        <select
          :value="sortBy"
          @change="emit('update:sortBy', $event.target.value)"
          class="select-sort"
        >
          <option value="newest">Mới nhất</option>
          <option value="best-seller">Lượt mua cao nhất</option>
          <option value="price-asc">Giá: Thấp đến cao</option>
          <option value="price-desc">Giá: Cao đến thấp</option>
          <option value="name-asc">Tên: A - Z</option>
        </select>
      </div>
    </div>
  </div>
</template>

<style scoped>
.thanh-sap-xep-wrap {
  margin-bottom: 20px;
}

.category-pills-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 16px;
  scrollbar-width: thin;
  scroll-snap-type: x proximity;
}

.pill-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  scroll-snap-align: start;
}

.pill-btn:hover {
  border-color: #d2a764;
  color: #1e293b;
}

.pill-btn.active {
  background: #1e293b;
  color: #ffffff;
  border-color: #1e293b;
}

.catalog-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.count-text {
  font-size: 13px;
  color: #64748b;
}

.count-text strong {
  color: #1e293b;
  font-weight: 800;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
}

.select-sort {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background-color: #f8fafc;
  font-size: 13px;
  cursor: pointer;
  outline: none;
}

.select-sort:focus {
  border-color: #496883;
}

@media (max-width: 720px) {
  .thanh-sap-xep-wrap {
    margin-bottom: 14px;
  }

  .category-pills-bar {
    gap: 6px;
    margin-inline: -1rem;
    padding: 0 1rem 10px;
    margin-bottom: 10px;
    scrollbar-width: none;
  }

  .category-pills-bar::-webkit-scrollbar {
    display: none;
  }

  .pill-btn {
    padding: 7px 12px;
    font-size: 11px;
  }

  .catalog-top-bar {
    padding: 9px 10px;
    border-radius: 9px;
    gap: 8px;
  }

  .count-text {
    font-size: 11px;
    white-space: nowrap;
  }

  .sort-wrapper {
    flex: 1;
    justify-content: flex-end;
    min-width: 0;
  }

  .sort-wrapper label {
    display: none;
  }

  .select-sort {
    width: min(100%, 160px);
    min-width: 0;
    padding: 6px 7px;
    font-size: 11px;
  }
}

@media (max-width: 390px) {
  .category-pills-bar {
    margin-inline: -0.7rem;
    padding-inline: 0.7rem;
  }

  .count-text {
    max-width: 88px;
    white-space: normal;
    line-height: 1.25;
  }
}
</style>

