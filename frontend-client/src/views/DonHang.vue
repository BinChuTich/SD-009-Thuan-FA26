<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency, formatDateTime } from '@/utils/dinhDang.js'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const orders = ref([])
const selectedOrder = ref(null)
const selectedOrderItems = ref([])
const activeTab = ref('all') // all, pending, shipping, completed, cancelled

// Modal QR thanh toán
const showQrModal = ref(false)
// Modal đánh giá
const showReviewModal = ref(false)
const reviewingItem = ref(null)
const reviewRating = ref(5)
const reviewComment = ref('')

// Countdown 5 phút tự động hủy cho đơn chưa thanh toán
const countdownSeconds = ref(299)
let timerInterval = null

const formatTimer = (sec) => {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await api.get('/api/hoa-don')
    const list = Array.isArray(res.data) ? res.data : []
    // Sắp xếp đơn mới nhất lên đầu
    list.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))
    orders.value = list

    if (list.length > 0) {
      // Nếu có query orderCode hoặc lấy đơn đầu tiên
      const targetCode = route.query.code
      const found = list.find(o => o.maHoaDon === targetCode) || list[0]
      viewOrderDetail(found)
    }
  } catch (err) {
    console.error('Lỗi tải danh sách đơn hàng:', err)
  } finally {
    loading.value = false
  }
}

const viewOrderDetail = async (order) => {
  selectedOrder.value = order
  selectedOrderItems.value = []
  try {
    const res = await api.get(`/api/hoa-don/code/${order.maHoaDon}/chi-tiet`)
    selectedOrderItems.value = res.data || []
  } catch (e) {
    console.error('Lỗi tải chi tiết đơn:', e)
  }
}

const getStatusLabel = (status) => {
  switch (status) {
    case 1: return { text: 'CHỜ XÁC NHẬN', class: 'status-pending' }
    case 2: return { text: 'ĐÃ XÁC NHẬN', class: 'status-confirmed' }
    case 3: return { text: 'ĐANG CHUẨN BỊ HÀNG', class: 'status-shipping' }
    case 4: return { text: 'ĐANG GIAO HÀNG', class: 'status-shipping' }
    case 5: return { text: 'HOÀN THÀNH', class: 'status-done' }
    case 6: return { text: 'ĐÃ HỦY', class: 'status-cancelled' }
    default: return { text: 'CHỜ XỬ LÝ', class: 'status-pending' }
  }
}

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  if (activeTab.value === 'pending') return orders.value.filter(o => o.trangThai === 1)
  if (activeTab.value === 'shipping') return orders.value.filter(o => o.trangThai === 2 || o.trangThai === 3 || o.trangThai === 4)
  if (activeTab.value === 'completed') return orders.value.filter(o => o.trangThai === 5)
  if (activeTab.value === 'cancelled') return orders.value.filter(o => o.trangThai === 6)
  return orders.value
})

const handleCancelOrder = async () => {
  if (!selectedOrder.value) return
  if (confirm(`Bạn có chắc chắn muốn hủy đơn hàng #${selectedOrder.value.maHoaDon}?`)) {
    try {
      await api.put(`/api/hoa-don/${selectedOrder.value.id}/trang-thai`, {
        trangThai: 6,
        ghiChu: 'Khách hàng yêu cầu hủy đơn trên website'
      })
      selectedOrder.value.trangThai = 6
      alert('Đã hủy đơn hàng thành công!')
      loadOrders()
    } catch (e) {
      alert('Không thể hủy đơn hàng vào lúc này.')
    }
  }
}

const openPayModal = () => {
  showQrModal.value = true
}

const openReview = (item) => {
  reviewingItem.value = item
  reviewRating.value = 5
  reviewComment.value = ''
  showReviewModal.value = true
}

const submitReview = () => {
  alert('Cảm ơn bạn đã gửi đánh giá 5 sao cho sản phẩm của FF T-Shirt!')
  showReviewModal.value = false
}

onMounted(() => {
  loadOrders()
  timerInterval = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
    }
  }, 1000)
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<template>
  <div class="orders-page-shell">
    <div class="container-client">
      <div class="orders-layout">
        <!-- 1. CỘT TRÁI: PROFILE KHÁCH HÀNG (MATCH VIDEO frame_order_detail.jpg) -->
        <aside class="profile-sidebar">
          <div class="profile-header-card">
            <div class="avatar-large">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
            </div>
            <h3 class="user-display-name">chiendn</h3>
            <span class="user-full-name">Dương Ngọc Chiến</span>
          </div>

          <nav class="sidebar-menu">
            <button class="menu-tab-btn active">
              <span class="icon">📄</span>
              <span>Đơn mua</span>
            </button>
            <button class="menu-tab-btn" @click="router.push('/tra-cuu-don-hang')">
              <span class="icon">🔍</span>
              <span>Tra cứu nhanh</span>
            </button>
            <button class="menu-tab-btn" @click="router.push('/cua-hang')">
              <span class="icon">🛍️</span>
              <span>Tiếp tục mua hàng</span>
            </button>
          </nav>
        </aside>

        <!-- 2. CỘT PHẢI: CHI TIẾT ĐƠN HÀNG & DANH SÁCH (MATCH VIDEO) -->
        <main class="order-content-main">
          <!-- Filter Tabs -->
          <div class="order-filter-tabs">
            <button :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">Tất cả</button>
            <button :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'">Chờ xác nhận</button>
            <button :class="{ active: activeTab === 'shipping' }" @click="activeTab = 'shipping'">Đang giao</button>
            <button :class="{ active: activeTab === 'completed' }" @click="activeTab = 'completed'">Hoàn thành</button>
            <button :class="{ active: activeTab === 'cancelled' }" @click="activeTab = 'cancelled'">Đã hủy</button>
          </div>

          <!-- Empty list -->
          <div v-if="orders.length === 0 && !loading" class="empty-orders-card">
            <div class="empty-icon">📦</div>
            <h3>Bạn chưa có đơn hàng nào</h3>
            <p>Hãy khám phá các thiết kế áo phông cao cấp tại cửa hàng FF T-Shirt ngay nhé!</p>
            <router-link to="/cua-hang" class="btn-go-shop">Mua sắm ngay</router-link>
          </div>

          <!-- Danh sách đơn hàng vắn tắt nếu chưa chọn hoặc muốn chuyển -->
          <div v-if="orders.length > 1" class="quick-order-switch">
            <span class="switch-title">Đơn hàng của bạn:</span>
            <div class="switch-pills">
              <button
                v-for="o in filteredOrders"
                :key="o.id"
                class="switch-pill"
                :class="{ active: selectedOrder?.id === o.id }"
                @click="viewOrderDetail(o)"
              >
                #{{ o.maHoaDon }} ({{ formatCurrency(o.tongTien) }})
              </button>
            </div>
          </div>

          <!-- CHI TIẾT ĐƠN HÀNG ĐƯỢC CHỌN (MATCH CHÍNH XÁC frame_order_detail.jpg) -->
          <div v-if="selectedOrder" class="order-detail-card">
            <!-- Header bar -->
            <div class="detail-top-bar">
              <div class="back-link" @click="router.push('/cua-hang')">
                &lt; TRỞ LẠI
              </div>
              <div class="order-id-status">
                <span class="code">MÃ ĐƠN HÀNG: #{{ selectedOrder.maHoaDon }}</span>
                <span class="bar-sep">|</span>
                <span class="status-badge" :class="getStatusLabel(selectedOrder.trangThai).class">
                  {{ getStatusLabel(selectedOrder.trangThai).text }}
                </span>
              </div>
            </div>

            <div class="order-date-text">
              {{ formatDateTime(selectedOrder.ngayTao) }}
            </div>

            <!-- Action buttons: Thanh toán ngay & Hủy Đơn Hàng -->
            <div class="top-action-buttons">
              <button
                v-if="selectedOrder.trangThaiThanhToan !== 1 && selectedOrder.trangThai !== 6"
                class="btn-pay-now"
                @click="openPayModal"
              >
                💳 Thanh toán ngay
              </button>
              <button
                v-if="selectedOrder.trangThai === 1"
                class="btn-cancel-order"
                @click="handleCancelOrder"
              >
                🚫 Hủy Đơn Hàng
              </button>
            </div>

            <!-- Khối 1: Thông tin nhận hàng -->
            <div class="info-section-box">
              <div class="sec-title-row">
                <h4 class="sec-title">📍 Thông tin nhận hàng</h4>
                <button class="btn-edit-text" v-if="selectedOrder.trangThai === 1">✏️ Sửa</button>
              </div>
              <div class="sec-content">
                <div class="row-text">
                  <span class="label">Khách hàng:</span>
                  <strong>{{ selectedOrder.tenKhachHang || 'Dương Ngọc Chiến' }}</strong>
                </div>
                <div class="row-text">
                  <span class="label">SĐT:</span>
                  <span>{{ selectedOrder.soDienThoaiKhachHang || '0988776655' }}</span>
                </div>
                <div class="row-text">
                  <span class="label">Địa chỉ:</span>
                  <span>{{ selectedOrder.diaChiNhanHang || '123 ngõ abc, Phường Cửa Đông, Quận Hoàn Kiếm, Thành phố Hà Nội' }}</span>
                </div>
              </div>
            </div>

            <!-- Khối 2: Thông tin Thanh toán -->
            <div class="info-section-box">
              <h4 class="sec-title">💳 Thanh toán</h4>
              <div class="sec-content">
                <div class="row-text">
                  <span class="label">Hình thức:</span>
                  <span>{{ selectedOrder.phuongThucThanhToan || 'Thanh toán qua VietQR / Chuyển khoản' }}</span>
                </div>
                <div class="row-text">
                  <span class="label">Tình trạng:</span>
                  <strong :class="selectedOrder.trangThaiThanhToan === 1 ? 'text-green' : 'text-orange'">
                    {{ selectedOrder.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán' }}
                  </strong>
                </div>
              </div>
            </div>

            <!-- Khối 3: Cảnh báo đếm ngược tự động hủy nếu chưa thanh toán (EXACT LIKE VIDEO) -->
            <div
              v-if="selectedOrder.trangThaiThanhToan !== 1 && selectedOrder.trangThai === 1"
              class="timer-warning-box"
            >
              <div class="timer-icon">⏰</div>
              <div>
                <strong>Đơn hàng sẽ tự động hủy sau: <span class="countdown-val">{{ formatTimer(countdownSeconds) }}</span></strong>
                <p>Vui lòng thanh toán trực tuyến ngay để đơn hàng không bị hệ thống tự động hủy!</p>
              </div>
            </div>

            <!-- Khối 4: Danh sách sản phẩm -->
            <div class="info-section-box">
              <h4 class="sec-title">SẢN PHẨM</h4>
              
              <div v-if="selectedOrderItems.length === 0" class="items-loading-box">
                Đang tải thông tin sản phẩm...
              </div>

              <div v-else class="items-table-list">
                <div v-for="it in selectedOrderItems" :key="it.id" class="order-product-row">
                  <img
                    :src="it.anh || 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=200&q=80'"
                    alt="Sản phẩm"
                    class="product-thumb"
                  />
                  <div class="product-info-col">
                    <span class="prod-name">{{ it.tenSanPham || 'Áo thun nam cao cấp FF' }}</span>
                    <span class="prod-meta">Màu: {{ it.tenMauSac || 'Xanh' }} | Size: {{ it.tenKichCo || 'M' }}</span>
                    <span class="prod-qty">Số lượng: x{{ it.soLuong }}</span>
                  </div>
                  <div class="product-price-col">
                    <span class="price-val">{{ formatCurrency(it.giaDuocTinh || it.donGia || 0) }}</span>
                    <button
                      v-if="selectedOrder.trangThai === 5"
                      class="btn-review-item"
                      @click="openReview(it)"
                    >
                      ⭐ Đánh giá
                    </button>
                  </div>
                </div>
              </div>

              <!-- Tổng kết tài chính -->
              <div class="order-financial-box">
                <div class="f-line">
                  <span>Tổng tiền hàng:</span>
                  <span>{{ formatCurrency(selectedOrder.tongTien - (selectedOrder.phiShip || 0) + (selectedOrder.tongTienGiamGia || 0)) }}</span>
                </div>
                <div class="f-line">
                  <span>Phí vận chuyển:</span>
                  <span>{{ selectedOrder.phiShip > 0 ? formatCurrency(selectedOrder.phiShip) : 'Miễn phí' }}</span>
                </div>
                <div v-if="selectedOrder.tongTienGiamGia > 0" class="f-line text-discount">
                  <span>Giảm giá (Voucher):</span>
                  <span>-{{ formatCurrency(selectedOrder.tongTienGiamGia) }}</span>
                </div>
                <div class="f-line total-line">
                  <span>Thành tiền:</span>
                  <strong class="total-red">{{ formatCurrency(selectedOrder.tongTien) }}</strong>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>

    <!-- MODAL THANH TOÁN VIETQR -->
    <div v-if="showQrModal" class="modal-overlay" @click.self="showQrModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Quét mã QR để thanh toán đơn hàng</h3>
          <button class="btn-close" @click="showQrModal = false">×</button>
        </div>
        <div class="modal-body text-center">
          <p>Mở ứng dụng Ngân hàng bất kỳ (Vietcombank, MB, Techcombank...) để quét mã:</p>
          <div class="qr-container">
            <img
              :src="`https://api.vietqr.io/image/970422-0388888888-compact2.jpg?amount=${selectedOrder?.tongTien}&addInfo=${selectedOrder?.maHoaDon}`"
              alt="QR Payment"
              class="qr-modal-img"
            />
          </div>
          <div class="bank-info-box">
            <div>Số tiền: <strong class="text-red">{{ formatCurrency(selectedOrder?.tongTien) }}</strong></div>
            <div>Nội dung: <strong>{{ selectedOrder?.maHoaDon }}</strong></div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-done-pay" @click="showQrModal = false; alert('Cảm ơn bạn! Hệ thống sẽ cập nhật trạng thái đơn sau khi xác nhận nhận tiền.')">
            Tôi đã chuyển khoản xong
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL ĐÁNH GIÁ SẢN PHẨM (MATCH VIDEO: KHÁCH HÀNG ĐÁNH GIÁ 5 SAO) -->
    <div v-if="showReviewModal" class="modal-overlay" @click.self="showReviewModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Đánh giá sản phẩm</h3>
          <button class="btn-close" @click="showReviewModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="review-product-head">
            <strong>{{ reviewingItem?.tenSanPham || 'Áo thun FF' }}</strong>
            <p>Phân loại: {{ reviewingItem?.tenMauSac }} / {{ reviewingItem?.tenKichCo }}</p>
          </div>

          <div class="star-rating-box">
            <label>Chất lượng phục vụ & sản phẩm:</label>
            <div class="stars-row">
              <span
                v-for="s in 5"
                :key="s"
                class="star-icon"
                :class="{ filled: s <= reviewRating }"
                @click="reviewRating = s"
              >
                ★
              </span>
              <span class="rating-text">
                {{ reviewRating === 5 ? 'Tuyệt vời' : reviewRating === 4 ? 'Hài lòng' : 'Bình thường' }}
              </span>
            </div>
          </div>

          <div class="form-group">
            <label>Chia sẻ cảm nhận của bạn:</label>
            <textarea
              v-model="reviewComment"
              rows="3"
              placeholder="Chất vải rất mịn, thoáng mát, đường may tỉ mỉ, đóng gói đẹp..."
              class="form-control"
            ></textarea>
          </div>

          <div class="photo-upload-hint">
            📷 Bạn có thể đính kèm hình ảnh thực tế của chiếc áo.
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-submit-review" @click="submitReview">Gửi Đánh Giá</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page-shell {
  background-color: #f8fafc;
  min-height: 85vh;
  padding: 36px 0 70px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.container-client {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.orders-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 32px;
  align-items: flex-start;
}

/* Sidebar profile */
.profile-sidebar {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);
}

.profile-header-card {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 16px;
}

.avatar-large {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.user-display-name {
  font-family: 'Fredoka', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #496883;
  margin: 0 0 4px;
}

.user-full-name {
  font-size: 12px;
  color: #64748b;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-tab-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.menu-tab-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.menu-tab-btn.active {
  background: #496883;
  color: #ffffff;
}

/* Main content */
.order-content-main {
  min-width: 0;
}

.order-filter-tabs {
  display: flex;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 6px;
  margin-bottom: 20px;
  gap: 6px;
  overflow-x: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.order-filter-tabs button {
  flex: 1;
  padding: 10px 14px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.order-filter-tabs button.active {
  background: #496883;
  color: #ffffff;
}

.quick-order-switch {
  background: #ffffff;
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.switch-title {
  font-size: 13px;
  font-weight: 700;
  color: #496883;
}

.switch-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.switch-pill {
  padding: 5px 12px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  border-radius: 20px;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.switch-pill:hover {
  border-color: #496883;
}

.switch-pill.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
  font-weight: 600;
}

/* Order detail card matching frame_order_detail.jpg */
.order-detail-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 28px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.detail-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 6px;
}

.back-link {
  font-size: 12px;
  font-weight: 700;
  color: #496883;
  cursor: pointer;
}

.back-link:hover {
  color: #d2a764;
}

.order-id-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.order-id-status .code {
  font-weight: 700;
  color: #1e293b;
}

.bar-sep {
  color: #cbd5e1;
}

.status-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 4px;
}

.status-pending { background: #fff3e0; color: #e65100; }
.status-confirmed { background: #e0f2fe; color: #0284c7; }
.status-shipping { background: #ede7f6; color: #512da8; }
.status-done { background: #e8f5e9; color: #2e7d32; }
.status-cancelled { background: #fee2e2; color: #dc2626; }

.order-date-text {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 16px;
}

.top-action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 24px;
}

.btn-pay-now {
  background: #496883;
  color: #ffffff;
  border: none;
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-pay-now:hover {
  background: #364f66;
}

.btn-cancel-order {
  background: #ffffff;
  color: #dc2626;
  border: 1px solid #dc2626;
  padding: 9px 18px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-order:hover {
  background: #fee2e2;
}

/* Sections */
.info-section-box {
  background: #f8fafc;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
}

.sec-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sec-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: #496883;
}

.sec-title-row .sec-title {
  margin-bottom: 0;
}

.btn-edit-text {
  background: transparent;
  border: none;
  color: #496883;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.sec-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #334155;
}

.row-text {
  display: flex;
  gap: 8px;
}

.row-text .label {
  color: #64748b;
  min-width: 90px;
}

.text-green { color: #16a34a; }
.text-orange { color: #d97706; }

/* Warning timer */
.timer-warning-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #991b1b;
}

.timer-icon {
  font-size: 26px;
}

.timer-warning-box strong {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}

.countdown-val {
  color: #dc2626;
  font-weight: 800;
  font-size: 16px;
}

.timer-warning-box p {
  margin: 0;
  font-size: 12px;
  color: #b91c1c;
}

/* Items */
.items-table-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.order-product-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #ffffff;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.product-thumb {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #e2e8f0;
}

.product-info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.prod-name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.prod-meta, .prod-qty {
  font-size: 11px;
  color: #64748b;
}

.product-price-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.price-val {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.btn-review-item {
  background: #d2a764;
  color: #ffffff;
  border: none;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-review-item:hover {
  background: #c99a4e;
}

.order-financial-box {
  background: #ffffff;
  border-radius: 8px;
  padding: 14px 18px;
  border: 1px solid #e2e8f0;
}

.f-line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.text-discount {
  color: #dc2626;
}

.total-line {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e2e8f0;
  margin-bottom: 0;
  font-size: 15px;
  color: #1e293b;
}

.total-red {
  color: #dc2626;
  font-size: 18px;
  font-weight: 800;
}

/* Modals */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-card {
  background: #ffffff;
  width: 90%;
  max-width: 440px;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}

.btn-close {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
}

.qr-container {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}

.qr-modal-img {
  width: 240px;
  height: 240px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.bank-info-box {
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.text-red { color: #dc2626; }

.btn-done-pay, .btn-submit-review {
  width: 100%;
  padding: 12px 0;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-done-pay:hover, .btn-submit-review:hover {
  background: #364f66;
}

.stars-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 10px 0 14px;
}

.star-icon {
  font-size: 28px;
  color: #e2e8f0;
  cursor: pointer;
  transition: color 0.15s;
}

.star-icon.filled {
  color: #d2a764;
}

.rating-text {
  font-size: 13px;
  font-weight: 700;
  color: #496883;
  margin-left: 8px;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #496883;
  outline: none;
}

.photo-upload-hint {
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 900px) {
  .orders-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .order-detail-card {
    padding: 18px;
  }
  .detail-top-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .top-action-buttons {
    flex-direction: column;
  }
  .btn-pay-now, .btn-cancel-order {
    width: 100%;
    text-align: center;
  }
  .order-product-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .product-price-col {
    align-items: flex-start;
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
  }
}
</style>

