<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'

const router = useRouter()
const activeDiscounts = ref([])
const suggestedProducts = ref([])
const loading = ref(true)

const loadData = async () => {
  loading.value = true
  try {
    try {
      const resD = await api.get('/api/dot-giam-gia/active-products')
      activeDiscounts.value = resD.data || []
    } catch (e) {}

    const resP = await api.get('/api/san-pham', { params: { page: 0, size: 12, trangThai: 1 } })
    const list = resP.data?.content || (Array.isArray(resP.data) ? resP.data : [])
    suggestedProducts.value = list.filter(p => p && p.tenSanPham && p.trangThai === 1)
  } catch (err) {
    console.error('Lỗi tải gợi ý:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="goi-y-page">
    <div class="page-header">
      <div class="container-client">
        <h1 class="header-title">Gợi Ý Mua Sắm & Xu Hướng Mới</h1>
        <p class="header-sub">Những bản phối thời trang nam thời thượng và các chương trình ưu đãi đặc quyền cho bạn</p>
      </div>
    </div>

    <div class="container-client content-body">
      <!-- 1. Gợi ý combo phối đồ -->
      <section class="outfit-section">
        <div class="sec-head">
          <span class="sub-head">OUTFIT OF THE DAY</span>
          <h2>Gợi Ý Phối Đồ Cho Từng Dịp</h2>
        </div>

        <div class="outfit-grid">
          <div class="outfit-card" @click="router.push('/cua-hang?danhmuc=polo')">
            <img src="https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=600&q=80" alt="Casual Office" />
            <div class="outfit-overlay">
              <span class="tag">Thanh Lịch Công Sở</span>
              <h3>Áo Polo Cotton + Quần Tây / Kaki</h3>
              <p>Chững chạc, chỉn chu nhưng vẫn cực kỳ thoải mái.</p>
            </div>
          </div>

          <div class="outfit-card" @click="router.push('/cua-hang?danhmuc=basic')">
            <img src="https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=600&q=80" alt="Streetwear" />
            <div class="outfit-overlay">
              <span class="tag">Dạo Phố Cuối Tuần</span>
              <h3>Áo Phông Cổ Tròn Basic + Quần Jeans Short</h3>
              <p>Trẻ trung, năng động, đậm chất thể thao.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 2. Danh sách sản phẩm được đề xuất -->
      <section class="suggest-products">
        <div class="sec-head">
          <span class="sub-head">SẢN PHẨM PHÙ HỢP VỚI BẠN</span>
          <h2>Dành Riêng Cho Bạn Hôm Nay</h2>
        </div>

        <div class="product-grid">
          <div
            v-for="p in suggestedProducts"
            :key="p.id"
            class="prod-item-card"
            @click="router.push(`/san-pham/${p.id}`)"
          >
            <div class="img-box">
              <img :src="p.anhDaiDien || '/images/products/default_tshirt.jpg'" :alt="p.tenSanPham" />
              <span v-if="p.coGiamGia" class="badge-sale">-{{ Math.round(p.phanTramGiam || 20) }}%</span>
            </div>
            <div class="info-box">
              <h4>{{ p.tenSanPham }}</h4>
              <div class="price-row">
                <span class="red-price">{{ formatCurrency(p.minGiaSauGiam || p.minGia || 0) }}</span>
                <span v-if="p.coGiamGia" class="old-price">{{ formatCurrency(p.minGia) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.goi-y-page {
  background-color: #faf8f6;
  min-height: 80vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
}

.page-header {
  background: #3b2c25;
  color: #ffffff;
  padding: 50px 0;
  text-align: center;
}

.header-title {
  font-family: 'Playfair Display', serif;
  font-size: 32px;
  margin: 0 0 10px;
}

.header-sub {
  color: #d7ccc8;
  font-size: 15px;
  margin: 0;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.content-body {
  margin-top: 40px;
}

.sec-head {
  text-align: center;
  margin-bottom: 30px;
}

.sub-head {
  color: #8d6e63;
  font-weight: 700;
  font-size: 12px;
  letter-spacing: 1.5px;
}

.sec-head h2 {
  font-family: 'Playfair Display', serif;
  font-size: 26px;
  color: #2b2320;
  margin: 6px 0 0;
}

.outfit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  margin-bottom: 60px;
}

.outfit-card {
  position: relative;
  height: 380px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 10px 25px rgba(0,0,0,0.08);
}

.outfit-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.outfit-card:hover img {
  transform: scale(1.05);
}

.outfit-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.2) 60%, transparent 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 24px;
  color: #ffffff;
}

.outfit-overlay .tag {
  background: #c99a4e;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 8px;
  width: fit-content;
}

.outfit-overlay h3 {
  margin: 0 0 6px;
  font-size: 20px;
}

.outfit-overlay p {
  margin: 0;
  font-size: 13px;
  color: #e2e8f0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.prod-item-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #ede8e3;
  cursor: pointer;
  transition: all 0.2s;
}

.prod-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.06);
}

.img-box {
  position: relative;
  padding-top: 120%;
}

.img-box img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-sale {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.info-box {
  padding: 14px;
  text-align: center;
}

.info-box h4 {
  font-size: 13px;
  color: #2b2320;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price-row {
  display: flex;
  justify-content: center;
  align-items: baseline;
  gap: 8px;
}

.red-price {
  color: #c62828;
  font-weight: 700;
  font-size: 14px;
}

.old-price {
  color: #8c7a72;
  text-decoration: line-through;
  font-size: 11px;
}

@media (max-width: 860px) {
  .outfit-grid {
    grid-template-columns: 1fr;
  }
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

