<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { clearCart } from '@/utils/luuTruGioHang.js'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const paymentResult = ref(null)
const isPopup = ref(false)
const closeCountdown = ref(3)

const formatPrice = (price) => {
  if (!price) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
}

const formatPayDate = (dateStr) => {
  if (!dateStr || dateStr.length < 14) return dateStr || ''
  // yyyyMMddHHmmss -> dd/MM/yyyy HH:mm:ss
  const y = dateStr.slice(0, 4)
  const m = dateStr.slice(4, 6)
  const d = dateStr.slice(6, 8)
  const hh = dateStr.slice(8, 10)
  const mm = dateStr.slice(10, 12)
  const ss = dateStr.slice(12, 14)
  return `${hh}:${mm}:${ss} - ${d}/${m}/${y}`
}

onMounted(async () => {
  isPopup.value = !!window.opener
  try {
    const params = { ...route.query }
    if (Object.keys(params).length === 0) {
      paymentResult.value = {
        success: false,
        message: 'Không tìm thấy thông tin giao dịch trả về từ VNPAY.'
      }
      loading.value = false
      return
    }

    const res = await api.get('/api/vnpay/payment-return', { params })
    paymentResult.value = res.data

    if (res.data && res.data.success) {
      // Xóa sạch giỏ hàng khi thanh toán thành công
      clearCart()

      // Báo tín hiệu cho trang chính (parent window / window.opener) biết đã thanh toán thành công
      if (window.opener && !window.opener.closed) {
        try {
          window.opener.postMessage({
            type: 'VNPAY_PAID_SUCCESS',
            orderCode: res.data.orderCode
          }, '*')
        } catch (e) {}
      }

      // Nếu đang mở dưới dạng popup window: đếm ngược 3 giây tự động đóng
      if (isPopup.value) {
        const timer = setInterval(() => {
          closeCountdown.value--
          if (closeCountdown.value <= 0) {
            clearInterval(timer)
            window.close()
          }
        }, 1000)
      }
    }
  } catch (err) {
    console.error('Lỗi kiểm tra trạng thái thanh toán VNPAY:', err)
    paymentResult.value = {
      success: false,
      message: err.response?.data?.message || 'Không thể xác thực giao dịch với máy chủ.'
    }
  } finally {
    loading.value = false
  }
})

const closeWindow = () => {
  if (isPopup.value) {
    window.close()
  } else {
    router.push(`/don-hang?code=${paymentResult.value?.orderCode}`)
  }
}
</script>

<template>
  <div class="vnpay-return-page">
    <!-- HERO HEADER -->
    <div class="return-hero">
      <div class="container-client">
        <h1 class="page-title">Kết Quả Thanh Toán VNPAY</h1>
        <p class="page-sub">Hệ thống xử lý giao dịch trực tuyến qua cổng Cổng Thanh Toán Quốc Gia VNPAY</p>
      </div>
    </div>

    <div class="container-client content-wrapper">
      <!-- 1. ĐANG XÁC THỰC -->
      <div v-if="loading" class="card-status loading-card">
        <div class="spinner-large"></div>
        <h3>Đang Xác Thực Giao Dịch VNPAY...</h3>
        <p>Vui lòng đợi giây lát trong khi hệ thống xác thực chữ ký bảo mật và cập nhật đơn hàng.</p>
      </div>

      <!-- 2. THANH TOÁN THÀNH CÔNG -->
      <div v-else-if="paymentResult?.success" class="card-status success-card">
        <div class="badge-icon success-badge">
          <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
        </div>

        <h2 class="status-title success-title">Thanh Toán VNPAY Thành Công!</h2>
        <p class="status-desc">
          Cảm ơn quý khách! Đơn hàng của bạn đã được ghi nhận thanh toán thành công và nhân viên FF T-Shirt sẽ chuẩn bị đơn hàng để giao sớm nhất.
        </p>

        <!-- Bảng hóa đơn giao dịch -->
        <div class="receipt-box">
          <div class="receipt-header">
            <span class="vnpay-tag">CỔNG VNPAY QR</span>
            <span class="receipt-status-pill">ĐÃ THANH TOÁN</span>
          </div>

          <div class="receipt-body">
            <div class="receipt-row">
              <span class="label">Mã đơn hàng:</span>
              <strong class="value text-highlight">#{{ paymentResult?.orderCode }}</strong>
            </div>

            <div class="receipt-row">
              <span class="label">Số tiền đã thanh toán:</span>
              <strong class="value text-price">{{ formatPrice(paymentResult?.amount) }}</strong>
            </div>

            <div class="receipt-row">
              <span class="label">Ngân hàng thanh toán:</span>
              <span class="value font-semibold">{{ paymentResult?.bankCode || 'NCB (Test)' }}</span>
            </div>

            <div class="receipt-row">
              <span class="label">Mã giao dịch VNPAY:</span>
              <span class="value font-mono">{{ paymentResult?.transactionNo || '---' }}</span>
            </div>

            <div class="receipt-row" v-if="paymentResult?.payDate">
              <span class="label">Thời gian giao dịch:</span>
              <span class="value">{{ formatPayDate(paymentResult?.payDate) }}</span>
            </div>
          </div>
        </div>

        <div v-if="isPopup" class="popup-countdown-notice">
          ⏱️ Cửa sổ này sẽ tự động đóng sau <strong>{{ closeCountdown }}</strong> giây...
        </div>

        <div class="action-buttons">
          <button v-if="isPopup" @click="closeWindow" class="btn-primary">
            ✓ Đóng Cửa Sổ Này
          </button>
          <template v-else>
            <router-link :to="`/don-hang?code=${paymentResult?.orderCode}`" class="btn-primary">
              🔍 Xem Chi Tiết Đơn Hàng
            </router-link>
            <router-link to="/cua-hang" class="btn-secondary">
              Tiếp Tục Mua Sắm &rarr;
            </router-link>
          </template>
        </div>
      </div>

      <!-- 3. GIAO DỊCH THẤT BẠI HOẶC BỊ HỦY -->
      <div v-else class="card-status error-card">
        <div class="badge-icon error-badge">
          <span class="cross-icon">✕</span>
        </div>

        <h2 class="status-title error-title">Thanh Toán Chưa Hoàn Tất</h2>
        <p class="status-desc">
          {{ paymentResult?.message || 'Giao dịch thanh toán qua cổng VNPAY đã bị hủy hoặc không thành công.' }}
        </p>

        <div class="receipt-box error-receipt" v-if="paymentResult?.orderCode">
          <div class="receipt-row">
            <span class="label">Mã đơn hàng:</span>
            <strong class="value">#{{ paymentResult?.orderCode }}</strong>
          </div>
          <div class="receipt-row">
            <span class="label">Trạng thái:</span>
            <span class="value text-danger font-semibold">Chưa thanh toán</span>
          </div>
        </div>

        <div class="action-buttons">
          <button v-if="isPopup" @click="closeWindow" class="btn-primary">
            Đóng Cửa Sổ
          </button>
          <template v-else>
            <router-link :to="`/don-hang?code=${paymentResult?.orderCode}`" class="btn-primary">
              Về Trang Đơn Hàng
            </router-link>
            <router-link to="/cua-hang" class="btn-secondary">
              Tiếp Tục Mua Sắm
            </router-link>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vnpay-return-page {
  background-color: #f8fafc;
  min-height: 85vh;
  padding-bottom: 80px;
  font-family: 'Be Vietnam Pro', -apple-system, sans-serif;
  color: #1e293b;
}

.return-hero {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #2b394a 100%);
  color: #ffffff;
  padding: 45px 0 40px;
  text-align: center;
  border-bottom: 2px solid #ecd8af;
}

.container-client {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 8px;
  color: #ffffff;
}

.page-sub {
  color: #cbd5e1;
  font-size: 14px;
  margin: 0;
}

.content-wrapper {
  margin-top: 40px;
}

.card-status {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  padding: 50px 36px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(73, 104, 131, 0.08);
}

/* Spinner */
.spinner-large {
  width: 52px;
  height: 52px;
  border: 4px solid #e2e8f0;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Badges */
.badge-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 22px;
}

.success-badge {
  background: #ecfdf5;
  border: 3px solid #10b981;
  color: #10b981;
  box-shadow: 0 0 0 10px rgba(16, 185, 129, 0.12);
}

.error-badge {
  background: #fef2f2;
  border: 3px solid #ef4444;
  color: #ef4444;
  box-shadow: 0 0 0 10px rgba(239, 68, 68, 0.12);
}

.icon-svg {
  width: 38px;
  height: 38px;
}

.cross-icon {
  font-size: 36px;
  font-weight: 800;
}

.status-title {
  font-size: 24px;
  font-weight: 800;
  margin: 0 0 10px;
}

.success-title {
  color: #0f172a;
}

.error-title {
  color: #dc2626;
}

.status-desc {
  font-size: 14.5px;
  color: #64748b;
  max-width: 560px;
  margin: 0 auto 30px;
  line-height: 1.6;
}

/* Receipt box */
.receipt-box {
  background: #f8fafc;
  border: 1.5px dashed #cbd5e1;
  border-radius: 14px;
  max-width: 500px;
  margin: 0 auto 24px;
  padding: 20px 24px;
  text-align: left;
}

.error-receipt {
  background: #fff5f5;
  border-color: #fca5a5;
}

.receipt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
  margin-bottom: 14px;
}

.vnpay-tag {
  background: #0284c7;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.receipt-status-pill {
  background: #dcfce7;
  color: #15803d;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
}

.receipt-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13.5px;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.receipt-row .label {
  color: #64748b;
}

.receipt-row .value {
  color: #1e293b;
}

.text-highlight {
  color: #0284c7 !important;
}

.text-price {
  color: #b58c4c !important;
  font-size: 16px;
}

.text-danger {
  color: #dc2626 !important;
}

.font-semibold {
  font-weight: 600;
}

.font-mono {
  font-family: monospace;
  font-size: 13px;
}

.popup-countdown-notice {
  font-size: 13.5px;
  color: #0284c7;
  margin-bottom: 20px;
  font-weight: 500;
}

/* Actions */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}

.btn-primary {
  background: #496883;
  color: #ffffff;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.2);
}

.btn-primary:hover {
  background: #395267;
  transform: translateY(-1px);
}

.btn-secondary {
  background: #d2a764;
  color: #1e293b;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #e0b875;
  transform: translateY(-1px);
}
</style>
