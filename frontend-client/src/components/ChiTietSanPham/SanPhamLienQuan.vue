<script setup>
import TheSanPham from '@/components/DungChung/TheSanPham.vue'

const props = defineProps({
  relatedProducts: {
    type: Array,
    default: () => []
  }
})
</script>

<template>
  <div v-if="relatedProducts && relatedProducts.length > 0" class="related-products-section">
    <div class="related-header">
      <span class="related-tag">GỢI Ý CHO BẠN</span>
      <h2 class="related-title">Sản Phẩm Cùng Bộ Sưu Tập</h2>
    </div>

    <div class="related-grid">
      <TheSanPham
        v-for="p in relatedProducts.slice(0, 4)"
        :key="p.id"
        :product="p"
      />
    </div>
  </div>
</template>

<style scoped>
.related-products-section {
  margin-top: 60px;
}

.related-header {
  text-align: center;
  margin-bottom: 30px;
}

.related-tag {
  display: inline-block;
  background: #eaf1f4;
  color: #496883;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1.2px;
  padding: 4px 12px;
  border-radius: 20px;
  margin-bottom: 8px;
}

.related-title {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

@media (max-width: 992px) {
  .related-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 580px) {
  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
