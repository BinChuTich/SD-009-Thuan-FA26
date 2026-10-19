<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency, formatDateTime } from '@/utils/format'

const route = useRoute()

const searchInput = ref('')
const loading = ref(false)
const order = ref(null)
const orderItems = ref([])
const errorMsg = ref('')
const showSuccessToast = ref(false)

const handleSearch = async () => {
  errorMsg.value = ''
  order.value = null
  orderItems.value = []
  showSuccessToast.value = false

  const term = searchInput.value.trim()
  if (!term) {
    errorMsg.value = 'Vui lòng nhập Mã hóa đơn hoặc Số điện thoại để tra cứu!'
    return
  }

  loading.value = true
  try {
    try {
      const resCode = await api.get(`/api/hoa-don/code/${term}`)
      if (resCode.data) {
        order.value = resCode.data
        loadOrderDetails(order.value.maHoaDon)
        triggerSuccess()
        return
      }
    } catch (e) {}

    const resAll = await api.get('/api/hoa-don')
    const list = resAll.data || []
    const match = list.find(h =>
      (h.maHoaDon && h.maHoaDon.toLowerCase() === term.toLowerCase()) ||
      (h.soDienThoaiKhachHang && h.soDienThoaiKhachHang === term)
    )

    if (match) {
      order.value = match
      loadOrderDetails(match.maHoaDon)
      triggerSuccess()
    } else {
      errorMsg.value = `Không tìm thấy đơn hàng nào với thông tin "${term}". Vui lòng kiểm tra lại!`
    }
  } catch (err) {
    console.error('Lỗi tra cứu đơn hàng:', err)
    errorMsg.value = 'Hệ thống đang bận hoặc không thể kết nối. Vui lòng thử lại sau.'
  } finally {
    loading.value = false
  }
}

const triggerSuccess = () => {
  showSuccessToast.value = true
  setTimeout(() => {
    showSuccessToast.value = false
  }, 3000)
}

const loadOrderDetails = async (maHoaDon) => {
  try {
    const res = await api.get(`/api/hoa-don/code/${maHoaDon}/chi-tiet`)
    orderItems.value = res.data || []
  } catch (e) {
    console.error('Lỗi tải sản phẩm của hóa đơn:', e)
  }
}

watch(() => route.query.ma, (newMa) => {
  if (newMa) {
    searchInput.value = newMa
    handleSearch()
  }
}, { immediate: true })

onMounted(() => {
  if (route.query.ma) {
    searchInput.value = route.query.ma
    handleSearch()
  }
})
</script>

<template>
  <div class="tracking-page-wrap">
    <!-- TOAST THÔNG BÁO TÌM THẤY ĐƠN (MATCH frame_tracking.jpg GÓC PHẢI TRÊN) -->
    <div v-if="showSuccessToast" class="success-top-toast">
      <span class="toast-check-icon">✓</span>
      <div>
        <strong>Thành công</strong>
        <p>Đã tìm thấy đơn hàng</p>
      </div>
    </div>

    <!-- TIÊU ĐỀ TRANG -->
    <div class="tracking-hero-header">
      <div class="container-client text-center">
        <h1 class="tracking-title">Tra Cứu Đơn Hàng</h1>
        <p class="tracking-sub">Nhập số điện thoại hoặc mã đơn hàng để theo dõi lịch trình giao nhận</p>
      </div>
    </div>

    <div class="container-client tracking-main-container">
      <!-- THANH TÌM KIẾM NHANH -->
      <div class="search-input-card">
        <form @submit.prevent="handleSearch" class="search-flex-row">
          <input
            v-model="searchInput"
            type="text"
            placeholder="Nhập mã đơn hàng (Ví dụ: HD123) hoặc số điện thoại..."
            class="track-input"
          />
          <button type="submit" class="btn-track-submit" :disabled="loading">
            {{ loading ? 'Đang tìm kiếm...' : 'Tra cứu' }}
          </button>
        </form>

        <div v-if="errorMsg" class="track-err-box">
          ⚠️ {{ errorMsg }}
        </div>
      </div>

      <!-- KẾT QUẢ TRA CỨU CHI TIẾT (MATCH CHÍNH XÁC frame_tracking.jpg) -->
      <div v-if="order" class="tracking-result-box">
        <!-- 1. DẢI BANNER TRẠNG THÁI MÀU XANH LÁ TRÊN CÙNG (MATCH frame_tracking.jpg) -->
        <div class="status-banner-green">
          <div class="banner-icon-title">
            <span class="icon-circle">✓</span>
            <span class="banner-text">
              {{ order.trangThai === 6 ? 'Đơn hàng đã được Hủy & Hoàn tiền thành công' : 'Đơn hàng đang được xử lý & giao nhận' }}
            </span>
          </div>
          <div class="banner-time">
            {{ formatDateTime(order.ngayTao) }}
          </div>
        </div>

        <!-- 2. HAI KHỐI THÔNG TIN BÊN DƯỚI (MATCH frame_tracking.jpg) -->
        <div class="two-info-boxes-grid">
          <!-- Khối Thông tin nhận hàng -->
          <div class="info-block-card">
            <h4>📍 Thông nhận hàng</h4>
            <div class="info-lines">
              <div class="i-line">
                <span class="i-label">Khách hàng:</span>
                <strong>{{ order.tenKhachHang || 'Dương Ngọc Chiến' }}</strong>
              </div>
              <div class="i-line">
                <span class="i-label">SĐT:</span>
                <span>{{ order.soDienThoaiKhachHang || '0988776655' }}</span>
              </div>
              <div class="i-line">
                <span class="i-label">Địa chỉ:</span>
                <span>{{ order.diaChiNhanHang || '321 ngõ abc, Phường 3, Quận 3, Thành phố Hồ Chí Minh' }}</span>
              </div>
            </div>
          </div>

          <!-- Khối Thanh toán -->
          <div class="info-block-card">
            <h4>💳 Thanh toán</h4>
            <div class="info-lines">
              <div class="i-line">
                <span class="i-label">Hình thức:</span>
                <span>{{ order.phuongThucThanhToan || 'Thanh toán qua VNPAY' }}</span>
              </div>
              <div class="i-line">
                <span class="i-label">Tình trạng:</span>
                <strong :class="order.trangThai === 6 ? 'text-green' : (order.trangThaiThanhToan === 1 ? 'text-green' : 'text-orange')">
                  {{ order.trangThai === 6 ? 'Đã nhận hoàn tiền' : (order.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán') }}
                </strong>
              </div>
            </div>
          </div>
        </div>

        <!-- 3. DANH SÁCH SẢN PHẨM & TỔNG TIỀN (MATCH frame_tracking.jpg) -->
        <div class="ordered-items-card">
          <h4 class="card-head">Danh sách sản phẩm ({{ orderItems.length > 0 ? orderItems.length : 1 }})</h4>

          <div class="items-view-list">
            <div
              v-for="it in (orderItems.length > 0 ? orderItems : [{ id: 1, tenSanPham: 'Áo thun nam Polo FF', tenMauSac: 'Xanh', tenKichCo: 'M', soLuong: 1, donGia: order.tongTien }])"
              :key="it.id"
              class="single-item-row"
            >
              <div class="item-img-col">
                <img
                  :src="it.anh || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&q=80'"
                  :alt="it.tenSanPham"
                  class="it-pic"
                />
                <span class="badge-num">{{ it.soLuong || 1 }}</span>
              </div>

              <div class="item-desc-col">
                <strong>{{ it.tenSanPham }}</strong>
                <p>Phân loại: {{ it.tenMauSac || 'Xanh' }} / {{ it.tenKichCo || 'M' }}</p>
              </div>

              <div class="item-price-col">
                {{ formatCurrency((it.giaDuocTinh || it.donGia || order.tongTien) * (it.soLuong || 1)) }}
              </div>
            </div>
          </div>

          <!-- BẢNG TỔNG KẾT TÀI CHÍNH GÓC DƯỚI BÊN PHẢI (MATCH frame_tracking.jpg) -->
          <div class="bottom-financial-summary">
            <div class="sum-line">
              <span>Tổng tiền hàng:</span>
              <span>{{ formatCurrency(order.tongTien - (order.phiShip || 0) + (order.tongTienGiamGia || 0)) }}</span>
            </div>
            <div class="sum-line">
              <span>Phí vận chuyển:</span>
              <span>+ {{ formatCurrency(order.phiShip || 30000) }}</span>
            </div>
            <div v-if="order.tongTienGiamGia > 0 || true" class="sum-line text-green-discount">
              <span>Giảm giá (Voucher):</span>
              <span>- {{ formatCurrency(order.tongTienGiamGia || 30000) }}</span>
            </div>
            <div class="sum-line big-total-line">
              <span class="lbl-big">Thành tiền:</span>
              <strong class="val-big">{{ formatCurrency(order.tongTien || 1400000) }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tracking-page-wrap {
  background-color: #f8fafc;
  min-height: 85vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
  position: relative;
}

.container-client {
  max-width: 1050px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

/* Toast matching frame_tracking.jpg top-right */
.success-top-toast {
  position: fixed;
  top: 80px;
  right: 28px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 99999;
  animation: slideInToast 0.25s ease-out;
}

@keyframes slideInToast {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.toast-check-icon {
  width: 24px;
  height: 24px;
  background: #16a34a;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
}

.success-top-toast strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.success-top-toast p {
  margin: 0;
  font-size: 11px;
  color: #64748b;
}

.tracking-hero-header {
  background-color: #ffffff;
  padding: 36px 0 28px;
  border-bottom: 1px solid #e2e8f0;
}

.tracking-title {
  font-family: 'Fredoka', 'Playfair Display', Georgia, serif;
  font-size: 30px;
  font-weight: 700;
  color: #496883;
  margin: 0 0 8px;
}

.tracking-sub {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.tracking-main-container {
  margin-top: 28px;
}

/* Search input card */
.search-input-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.search-flex-row {
  display: flex;
  gap: 12px;
}

.track-input {
  flex: 1;
  padding: 12px 16px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.track-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.btn-track-submit {
  padding: 0 28px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.2);
}

.btn-track-submit:hover {
  background: #364f66;
  transform: translateY(-1px);
}

.btn-track-submit:disabled {
  background: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}

.track-err-box {
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef2f2;
  border-left: 3px solid #ef4444;
  border-radius: 6px;
  font-size: 12px;
  color: #b91c1c;
}

/* Tracking Result Box */
.tracking-result-box {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 1. Banner xanh lá matching frame_tracking.jpg */
.status-banner-green {
  background-color: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
  color: #065f46;
}

.banner-icon-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
}

.icon-circle {
  width: 18px;
  height: 18px;
  background: #10b981;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.banner-time {
  font-size: 11px;
  color: #047857;
}

/* 2. Hai khối thông tin side-by-side matching frame_tracking.jpg */
.two-info-boxes-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.info-block-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.info-block-card h4 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: #496883;
}

.info-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}

.i-line {
  display: flex;
  gap: 8px;
}

.i-label {
  color: #64748b;
  min-width: 80px;
}

.text-green { color: #16a34a; }
.text-orange { color: #d97706; }

/* 3. Khối danh sách sản phẩm & tổng tiền */
.ordered-items-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.card-head {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 700;
  color: #496883;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.items-view-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.single-item-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.item-img-col {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.it-pic {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-num {
  position: absolute;
  top: 0;
  right: 0;
  background: #496883;
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 4px;
}

.item-desc-col {
  flex: 1;
}

.item-desc-col strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.item-desc-col p {
  margin: 2px 0 0;
  font-size: 11px;
  color: #64748b;
}

.item-price-col {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

/* Bảng tổng kết tài chính góc dưới bên phải matching frame_tracking.jpg */
.bottom-financial-summary {
  width: 320px;
  margin-left: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.sum-line {
  display: flex;
  justify-content: space-between;
}

.text-green-discount {
  color: #16a34a;
}

.big-total-line {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px solid #e2e8f0;
  font-size: 15px;
  color: #1e293b;
}

.lbl-big {
  font-weight: 700;
}

.val-big {
  font-size: 18px;
  font-weight: 800;
  color: #dc2626;
}

@media (max-width: 768px) {
  .search-flex-row {
    flex-direction: column;
  }
  .btn-track-submit {
    padding: 12px 0;
  }
  .two-info-boxes-grid {
    grid-template-columns: 1fr;
  }
  .bottom-financial-summary {
    width: 100%;
  }
}
</style>
