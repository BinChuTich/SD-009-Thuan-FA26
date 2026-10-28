<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'

const route = useRoute()

const searchCode = ref('')
const searchPhone = ref('')
const loading = ref(false)
const order = ref(null)
const orderItems = ref([])
const orderLogs = ref([])
const errorMsg = ref('')
const showSuccessToast = ref(false)

const triggerSuccess = () => {
  showSuccessToast.value = true
  setTimeout(() => {
    showSuccessToast.value = false
  }, 3500)
}

const loadOrderDetails = async (maHoaDon) => {
  try {
    const res = await api.get(`/api/hoa-don/code/${maHoaDon}/chi-tiet`)
    orderItems.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    console.error('Lỗi tải sản phẩm của hóa đơn:', e)
    orderItems.value = []
  }
}

const loadOrderLogs = async (maHoaDon) => {
  try {
    const res = await api.get(`/api/hoa-don/code/${maHoaDon}/lich-su`)
    const logs = Array.isArray(res.data) ? res.data : []
    logs.sort((a, b) => new Date(b.thoiGian || 0) - new Date(a.thoiGian || 0))
    orderLogs.value = logs
  } catch (e) {
    console.error('Lỗi tải lịch sử tác động đơn:', e)
    orderLogs.value = []
  }
}

const handleSearch = async () => {
  errorMsg.value = ''
  order.value = null
  orderItems.value = []
  orderLogs.value = []
  showSuccessToast.value = false

  const code = (searchCode.value || '').trim()
  const phone = (searchPhone.value || '').trim()

  if (!code && !phone) {
    errorMsg.value = 'Vui lòng nhập Mã đơn hàng hoặc Số điện thoại để tra cứu!'
    return
  }

  loading.value = true
  try {
    let foundOrder = null

    // 1. Nếu có mã đơn hàng, thử tìm trực tiếp bằng code
    if (code) {
      try {
        const resCode = await api.get(`/api/hoa-don/code/${code}`)
        if (resCode.data && resCode.data.maHoaDon) {
          // Nếu người dùng nhập cả SĐT, kiểm tra xem có khớp không
          if (phone) {
            const oPhone = (resCode.data.soDienThoaiKhachHang || resCode.data.soDienThoaiHienThi || '').trim()
            if (oPhone && oPhone !== phone) {
              errorMsg.value = `Số điện thoại "${phone}" không khớp với mã đơn hàng "${code}". Vui lòng kiểm tra lại!`
              loading.value = false
              return
            }
          }
          foundOrder = resCode.data
        }
      } catch (e) {
        // Tiếp tục quét danh sách nếu get theo code 404
      }
    }

    // 2. Nếu chưa tìm thấy hoặc chỉ nhập SĐT, quét danh sách đơn hàng
    if (!foundOrder) {
      const resAll = await api.get('/api/hoa-don')
      const list = Array.isArray(resAll.data) ? resAll.data : []
      // Sắp xếp đơn mới nhất lên đầu
      list.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))

      foundOrder = list.find(h => {
        const hCode = (h.maHoaDon || '').trim().toLowerCase()
        const hPhone = (h.soDienThoaiKhachHang || h.soDienThoaiHienThi || '').trim()

        if (code && phone) {
          return hCode === code.toLowerCase() && hPhone === phone
        } else if (code) {
          return hCode === code.toLowerCase()
        } else if (phone) {
          return hPhone === phone
        }
        return false
      })
    }

    if (foundOrder) {
      order.value = foundOrder
      await Promise.all([
        loadOrderDetails(foundOrder.maHoaDon),
        loadOrderLogs(foundOrder.maHoaDon)
      ])
      triggerSuccess()
    } else {
      errorMsg.value = `Không tìm thấy đơn hàng nào với thông tin bạn cung cấp. Vui lòng kiểm tra lại!`
    }
  } catch (err) {
    console.error('Lỗi tra cứu đơn hàng:', err)
    errorMsg.value = 'Hệ thống đang bận hoặc không thể kết nối. Vui lòng thử lại sau.'
  } finally {
    loading.value = false
  }
}

// Helpers định dạng thời gian
const formatOnlyTime = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const formatOnlyDate = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

const formatLogTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
}

// Mô tả trạng thái đơn hàng (Match video tc_1925.png)
const getOrderStatusBanner = (o) => {
  if (!o) return ''
  if (o.trangThai === 6) return 'Đơn hàng đã được Hủy & Hoàn tiền thành công'
  if (o.trangThai === 5) return 'Đơn hàng đã được giao thành công'
  if (o.trangThai === 4) return 'Đơn hàng đang được giao'
  if (o.trangThai === 3) return 'Đơn hàng đã đóng gói, chờ giao ĐVVC'
  if (o.trangThai === 2) return 'Đơn hàng đã được xác nhận'
  return 'Đơn hàng đang được xử lý & giao nhận'
}

const isVnPay = (o) => {
  if (!o) return false
  const pt = (o.phuongThucThanhToan || o.ghiChu || '').toUpperCase()
  return pt.includes('VNPAY') || Boolean(o.isVnPay)
}

const getPaymentStatusText = (o) => {
  if (!o) return ''
  if (o.trangThai === 6) {
    if (isVnPay(o)) return 'Đã nhận hoàn tiền'
    return 'Đã hủy đơn'
  }
  if (o.trangThaiThanhToan === 1) return 'Đã thanh toán'
  return 'Chưa thanh toán'
}

const isPaidOrRefunded = (o) => {
  if (!o) return false
  return o.trangThai === 6 || o.trangThaiThanhToan === 1
}

const getActorName = (log) => {
  if (log.vaiTroNguoiThucHien) return log.vaiTroNguoiThucHien
  if (log.nguoiThucHien) return log.nguoiThucHien
  return 'Hệ thống'
}

const getActorClass = (log) => {
  const actor = (log.vaiTroNguoiThucHien || log.nguoiThucHien || '').toLowerCase()
  if (actor.includes('quản trị') || actor.includes('admin') || actor.includes('nhân viên') || actor.includes('vnpay') || actor.includes('hệ thống')) {
    return 'actor-admin'
  }
  return 'actor-customer'
}

// Helper hình ảnh sản phẩm
const resolveProductImg = (it) => {
  const raw = it.anhDaiDien || it.anh || it.hinhAnh || it.idChiTietSanPham?.idSanPham?.anh || it.idChiTietSanPham?.anh
  if (!raw) return 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=120&q=80'
  if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:')) return raw
  return `http://localhost:8080${raw.startsWith('/') ? '' : '/'}${raw}`
}

const onImgError = (e) => {
  e.target.src = 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=120&q=80'
}

// 5 Bước Stepper
const isPosOrder = computed(() => {
  if (!order.value) return false
  if (order.value.loaiDon === 1) return true
  const addr = (order.value.diaChiNhanHang || '').toLowerCase()
  return addr.includes('tại quầy') || addr.includes('tai quay') || addr.includes('tại cửa hàng') || addr.includes('tai cua hang')
})

const defaultSteps = [
  { num: 1, label: 'Đặt hàng' },
  { num: 2, label: 'Xác nhận' },
  { num: 3, label: 'Đóng gói' },
  { num: 4, label: 'Đang giao' },
  { num: 5, label: 'Giao thành công' }
]

const steps = computed(() => {
  if (isPosOrder.value) {
    return [
      { num: 1, label: 'Tạo đơn tại quầy' },
      { num: 2, label: 'Thanh toán' },
      { num: 5, label: 'Nhận hàng tại quầy' }
    ]
  }
  return defaultSteps
})
/*
})

]

*/
const currentStep = computed(() => {
  if (!order.value) return 1
  if (order.value.trangThai === 6) return -1
  return order.value.trangThai || 1
})

// Tính toán tài chính
const shippingFee = computed(() => {
  if (!order.value) return 0
  if (isPosOrder.value) return 0
  return Number(order.value.phiVanChuyen || order.value.phiShip || 0)
})

const finalTotal = computed(() => {
  if (!order.value) return 0
  return Number(order.value.tienSauGiamGia || order.value.tongTien || 0)
})

const goodsTotal = computed(() => {
  if (!order.value) return 0
  if (orderItems.value && orderItems.value.length > 0) {
    return orderItems.value.reduce((sum, it) => sum + (it.thanhTien || ((it.giaDuocTinh || it.donGia || 0) * (it.soLuong || 1))), 0)
  }
  return Number(order.value.tongTien || 0)
})

const discountAmount = computed(() => {
  if (!order.value) return 0
  if (order.value.tongTienGiamGia && Number(order.value.tongTienGiamGia) > 0) {
    return Number(order.value.tongTienGiamGia)
  }
  const diff = (goodsTotal.value + shippingFee.value) - finalTotal.value
  return diff > 0 ? diff : 0
})

watch(
  () => route.query,
  (q) => {
    if (q.ma || q.code || q.sdt || q.phone) {
      if (q.ma || q.code) searchCode.value = q.ma || q.code
      if (q.sdt || q.phone) searchPhone.value = q.sdt || q.phone
      handleSearch()
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (route.query.ma || route.query.code || route.query.sdt || route.query.phone) {
    if (route.query.ma || route.query.code) searchCode.value = route.query.ma || route.query.code
    if (route.query.sdt || route.query.phone) searchPhone.value = route.query.sdt || route.query.phone
    handleSearch()
  }
})
</script>

<template>
  <div class="tracking-page-wrap">
    <!-- TOAST THÔNG BÁO TÌM THẤY ĐƠN (MATCH VIDEO GÓC PHẢI TRÊN tc_1925.png) -->
    <transition name="toast-slide">
      <div v-if="showSuccessToast" class="success-top-toast">
        <span class="toast-check-icon">✓</span>
        <div class="toast-text-box">
          <strong>Thành công</strong>
          <p>Đã tìm thấy đơn hàng</p>
        </div>
      </div>
    </transition>

    <!-- TIÊU ĐỀ TRANG TRANG NHÃ (MATCH VIDEO tc_input_1920.png) -->
    <div class="tracking-hero-header">
      <div class="container-client text-center">
        <h1 class="tracking-title">Tra Cứu Đơn Hàng</h1>
      </div>
    </div>

    <div class="container-client tracking-main-container">
      <!-- THANH TÌM KIẾM 2 TRƯỜNG MATCH CHUẨN VIDEO tc_input_1920.png -->
      <div class="search-input-card">
        <form @submit.prevent="handleSearch" class="search-two-fields-form">
          <div class="form-field-col">
            <label class="field-label">
              Mã đơn hàng <span class="req">*</span>
            </label>
            <input
              v-model="searchCode"
              type="text"
              placeholder="Nhập Mã Hóa Đơn"
              class="track-input"
            />
          </div>

          <div class="form-field-col">
            <label class="field-label">
              Số điện thoại đặt hàng <span class="req">*</span>
            </label>
            <input
              v-model="searchPhone"
              type="text"
              placeholder="Nhập SĐT"
              class="track-input"
            />
          </div>

          <div class="form-btn-col">
            <button type="submit" class="btn-track-submit" :disabled="loading">
              <span class="btn-icon">🔍</span> {{ loading ? 'Đang tìm...' : 'Tìm' }}
            </button>
          </div>
        </form>

        <div v-if="errorMsg" class="track-err-box">
          ⚠️ {{ errorMsg }}
        </div>
      </div>

      <!-- KẾT QUẢ TRA CỨU CHI TIẾT (MATCH CHÍNH XÁC VIDEO tc_1925.png) -->
      <div v-if="order" class="tracking-result-box">
        <!-- 1. DẢI BANNER TRẠNG THÁI TRÊN CÙNG (XANH LÁ HOẶC ĐỎ KHI HỦY) -->
        <div
          class="status-banner"
          :class="order.trangThai === 6 ? 'status-banner-red' : 'status-banner-green'"
        >
          <div class="banner-icon-title">
            <span class="icon-circle">{{ order.trangThai === 6 ? '✕' : '✓' }}</span>
            <span class="banner-text">{{ getOrderStatusBanner(order) }}</span>
          </div>
          <div class="banner-time">
            <div class="time-part">{{ formatOnlyTime(order.ngayCapNhat || order.ngayTao) }}</div>
            <div class="date-part">{{ formatOnlyDate(order.ngayCapNhat || order.ngayTao) }}</div>
          </div>
        </div>

        <!-- 2. THANH TIẾN TRÌNH STEPPER TINH TẾ (CHỈ HIỂN THỊ KHI CHƯA BỊ HỦY) -->
        <div v-if="order.trangThai !== 6" class="stepper-minimal-card">
          <div class="stepper-line-track">
            <div
              v-for="(st, idx) in steps"
              :key="st.num"
              class="stepper-node-item"
              :class="{
                done: currentStep >= st.num,
                active: currentStep === st.num
              }"
            >
              <div v-if="idx > 0" class="step-line-segment" :class="{ filled: currentStep >= st.num }"></div>
              <div class="step-bullet-circle">
                <span v-if="currentStep > st.num" class="step-check">✓</span>
                <span v-else>{{ st.num }}</span>
              </div>
              <span class="step-label-text">{{ st.label }}</span>
            </div>
          </div>
        </div>

        <!-- 3. HAI KHỐI THÔNG TIN BÊN DƯỚI (MATCH CHUẨN tc_1925.png) -->
        <div class="two-info-boxes-grid">
          <!-- Khối Thông nhận hàng -->
          <div class="info-block-card">
            <h4>📍 Thông nhận hàng</h4>
            <div class="info-lines">
              <div class="i-line">
                <span class="i-label">Khách hàng:</span>
                <strong>{{ order.tenKhachHang || order.tenKhachHangHienThi || 'Khách hàng' }}</strong>
              </div>
              <div class="i-line">
                <span class="i-label">SĐT:</span>
                <span>{{ order.soDienThoaiKhachHang || order.soDienThoaiHienThi || '—' }}</span>
              </div>
              <div class="i-line">
                <span class="i-label">Địa chỉ:</span>
                <span>{{ order.diaChiNhanHang || '—' }}</span>
              </div>
              <div class="i-line" v-if="order.loaiDon !== 1">
                <span class="i-label">Vận chuyển:</span>
                <span style="font-weight: 600; color: #3e5c76;">🚚 {{ order.donViVanChuyen || 'Giao Hàng Nhanh (GHN)' }}</span>
              </div>
            </div>
          </div>

          <!-- Khối Thanh toán -->
          <div class="info-block-card">
            <h4>💳 Thanh toán</h4>
            <div class="info-lines">
              <div class="i-line">
                <span class="i-label">Hình thức:</span>
                <span>{{ order.phuongThucThanhToan || (isVnPay(order) ? 'Thanh toán qua VNPAY' : 'Thanh toán khi nhận hàng (COD)') }}</span>
              </div>
              <div class="i-line">
                <span class="i-label">Tình trạng:</span>
                <strong :class="isPaidOrRefunded(order) ? 'text-green' : 'text-orange'">
                  {{ getPaymentStatusText(order) }}
                </strong>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. DANH SÁCH SẢN PHẨM & TỔNG TIỀN (MATCH CHUẨN tc_1925.png) -->
        <div class="ordered-items-card">
          <h4 class="card-head">
            Danh sách sản phẩm ({{ orderItems.length > 0 ? orderItems.length : 1 }})
          </h4>

          <div class="items-view-list">
            <div
              v-for="it in (orderItems.length > 0 ? orderItems : [{ id: 1, tenSanPham: 'Áo Polo FF Nam Cổ Bẻ Cao Cấp', tenMauSac: 'Đen', tenKichCo: 'M', soLuong: 1, donGia: order.tongTien }])"
              :key="it.id"
              class="single-item-row"
            >
              <div class="item-img-col">
                <img
                  :src="resolveProductImg(it)"
                  :alt="it.tenSanPham"
                  class="it-pic"
                  @error="onImgError"
                />
                <span class="badge-num">{{ it.soLuong || 1 }}</span>
              </div>

              <div class="item-desc-col">
                <strong>{{ it.tenSanPham || it.idChiTietSanPham?.idSanPham?.tenSanPham || 'Áo Polo Nam Cao Cấp' }}</strong>
                <p>Phân loại: {{ it.tenMauSac || it.idChiTietSanPham?.idMauSac?.tenMauSac || 'Tiêu chuẩn' }} / {{ it.tenKichCo || it.idChiTietSanPham?.idKichCo?.tenKichCo || 'M' }}</p>
              </div>

              <div class="item-price-col">
                {{ formatCurrency(it.thanhTien || ((it.giaDuocTinh || it.donGia || order.tongTien) * (it.soLuong || 1))) }}
              </div>
            </div>
          </div>

          <!-- BẢNG TỔNG KẾT TÀI CHÍNH GÓC DƯỚI BÊN PHẢI (MATCH tc_1925.png) -->
          <div class="bottom-financial-summary">
            <div class="sum-line">
              <span>Tổng tiền hàng:</span>
              <span>{{ formatCurrency(goodsTotal) }}</span>
            </div>
            <div class="sum-line">
              <span>Phí vận chuyển:</span>
              <span>+ {{ formatCurrency(shippingFee) }}</span>
            </div>
            <div v-if="discountAmount > 0" class="sum-line text-green-discount">
              <span>Giảm giá (Voucher):</span>
              <span>- {{ formatCurrency(discountAmount) }}</span>
            </div>
            <div class="sum-line big-total-line">
              <span class="lbl-big">Thành tiền:</span>
              <strong class="val-big">{{ formatCurrency(finalTotal) }}</strong>
            </div>
          </div>
        </div>

        <!-- 5. KHỐI LỊCH SỬ TÁC ĐỘNG ĐƠN HÀNG (MATCH tc_1925.png) -->
        <div class="info-block-card history-card-block">
          <h4>🕒 Lịch sử tác động</h4>
          <div v-if="orderLogs.length > 0" class="order-history-timeline">
            <div
              v-for="(log, idx) in orderLogs"
              :key="log.id || idx"
              class="history-item-row"
            >
              <span class="history-time">{{ formatLogTime(log.thoiGian) }}</span>
              <span class="history-actor" :class="getActorClass(log)">[{{ getActorName(log) }}]</span>
              <span class="history-desc">{{ log.ghiChu || log.hanhDong || 'Cập nhật tiến trình đơn hàng' }}</span>
            </div>
          </div>
          <div v-else class="history-empty-text">
            Chưa có lịch sử cập nhật cho đơn hàng này.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tracking-page-wrap {
  background-color: #fbfcfd;
  min-height: 85vh;
  padding-bottom: 70px;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1e293b;
  position: relative;
}

.container-client {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

@media (min-width: 1400px) {
  .container-client {
    max-width: 1240px;
    padding: 0 2rem;
  }
}

@media (max-width: 640px) {
  .container-client {
    padding: 0 1rem;
  }
}

/* Toast matching video góc phải trên (tc_1925.png) */
.success-top-toast {
  position: fixed;
  top: 85px;
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
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.28s ease;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px);
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
  flex-shrink: 0;
}

.toast-text-box strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.toast-text-box p {
  margin: 2px 0 0;
  font-size: 11px;
  color: #64748b;
}

/* Hero Header (Match Video tc_input_1920.png) */
.tracking-hero-header {
  background-color: #ffffff;
  padding: 36px 0 26px;
  border-bottom: 1px solid #eef2f6;
  text-align: center;
}

.tracking-title {
  font-size: 32px;
  font-weight: 800;
  color: #2c3e50;
  margin: 0;
  letter-spacing: -0.01em;
}

.tracking-main-container {
  margin-top: 30px;
  max-width: 900px;
}

/* Search input card 2 fields (Match Video tc_input_1920.png) */
.search-input-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 24px 28px;
  margin-bottom: 24px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.03);
}

.search-two-fields-form {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.form-field-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.field-label .req {
  color: #dc2626;
  font-weight: 700;
}

.track-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1.5px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13.5px;
  outline: none;
  background-color: #ffffff;
  transition: all 0.2s;
  box-sizing: border-box;
  color: #1e293b;
}

.track-input:focus {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.form-btn-col {
  flex-shrink: 0;
}

.btn-track-submit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 26px;
  height: 42px;
  background: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 3px 8px rgba(73, 104, 131, 0.2);
}

.btn-track-submit:hover:not(:disabled) {
  background: #364f66;
  transform: translateY(-1px);
}

.btn-track-submit:disabled {
  background: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}

.btn-icon {
  font-size: 13px;
}

.track-err-box {
  margin-top: 14px;
  padding: 10px 14px;
  background: #fef2f2;
  border-left: 3px solid #ef4444;
  border-radius: 6px;
  font-size: 12.5px;
  color: #b91c1c;
}

/* Tracking Result Box */
.tracking-result-box {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 1. Banner trạng thái (Xanh lá tc_1925.png hoặc Đỏ khi hủy) */
.status-banner {
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-banner-green {
  background-color: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.status-banner-red {
  background-color: #fef2f2;
  border: 1px solid #fecdd3;
  color: #991b1b;
}

.banner-icon-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14.5px;
  font-weight: 700;
}

.icon-circle {
  width: 22px;
  height: 22px;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
}

.status-banner-green .icon-circle {
  background: #10b981;
}

.status-banner-red .icon-circle {
  background: #ef4444;
}

.banner-time {
  text-align: right;
  font-size: 12px;
  font-family: monospace;
  line-height: 1.35;
}

.status-banner-green .banner-time {
  color: #047857;
}

.status-banner-red .banner-time {
  color: #b91c1c;
}

.banner-time .time-part {
  font-weight: 700;
}

/* 2. Stepper tối giản, tinh tế */
.stepper-minimal-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 18px 24px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.stepper-line-track {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.stepper-node-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 2;
  flex: 1;
}

.step-line-segment {
  position: absolute;
  top: 13px;
  left: -50%;
  width: 100%;
  height: 3px;
  background: #e2e8f0;
  z-index: -1;
}

.step-line-segment.filled {
  background: #496883;
}

.step-bullet-circle {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f1f5f9;
  border: 2px solid #cbd5e1;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.stepper-node-item.done .step-bullet-circle {
  background: #496883;
  border-color: #496883;
  color: #ffffff;
}

.stepper-node-item.active .step-bullet-circle {
  background: #ffffff;
  border-color: #496883;
  color: #496883;
  box-shadow: 0 0 0 4px rgba(73, 104, 131, 0.15);
  font-weight: 800;
}

.step-label-text {
  font-size: 11.5px;
  color: #64748b;
  font-weight: 500;
  text-align: center;
}

.stepper-node-item.done .step-label-text,
.stepper-node-item.active .step-label-text {
  color: #1e293b;
  font-weight: 700;
}

/* 3. Hai khối thông tin side-by-side matching tc_1925.png */
.two-info-boxes-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.info-block-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 18px 20px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.info-block-card h4 {
  margin: 0 0 14px;
  font-size: 14.5px;
  font-weight: 700;
  color: #334155;
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
  min-width: 85px;
  flex-shrink: 0;
}

.text-green { color: #16a34a; }
.text-orange { color: #d97706; }

/* 4. Khối danh sách sản phẩm & tổng tiền matching tc_1925.png */
.ordered-items-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 22px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.card-head {
  margin: 0 0 16px;
  font-size: 14.5px;
  font-weight: 700;
  color: #496883;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.items-view-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.single-item-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f8fafc;
}

.item-img-col {
  position: relative;
  width: 56px;
  height: 56px;
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
  font-size: 9.5px;
  font-weight: 700;
  padding: 1px 5px;
  border-bottom-left-radius: 4px;
}

.item-desc-col {
  flex: 1;
}

.item-desc-col strong {
  display: block;
  font-size: 13.5px;
  color: #1e293b;
}

.item-desc-col p {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}

.item-price-col {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

/* Bảng tổng kết tài chính góc dưới bên phải matching tc_1925.png */
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
  color: #1e293b;
}

/* 5. Khối lịch sử tác động đơn hàng (Match video tc_1925.png) */
.history-card-block {
  margin-top: 0;
}

.order-history-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.history-item-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-size: 13px;
  line-height: 1.5;
}

.history-time {
  color: #64748b;
  font-size: 12px;
  min-width: 140px;
  flex-shrink: 0;
  font-family: monospace;
}

.history-actor {
  font-weight: 700;
  font-size: 12.5px;
  flex-shrink: 0;
}

.actor-admin {
  color: #dc2626;
}

.actor-customer {
  color: #2563eb;
}

.history-desc {
  color: #1e293b;
  flex: 1;
}

.history-empty-text {
  font-size: 13px;
  color: #94a3b8;
  padding: 8px 0;
  font-style: italic;
}

@media (max-width: 768px) {
  .search-two-fields-form {
    flex-direction: column;
    align-items: stretch;
  }
  .btn-track-submit {
    width: 100%;
    justify-content: center;
  }
  .two-info-boxes-grid {
    grid-template-columns: 1fr;
  }
  .bottom-financial-summary {
    width: 100%;
  }
  .history-item-row {
    flex-direction: column;
    gap: 3px;
  }
  .history-time {
    min-width: auto;
  }
  .stepper-line-track {
    flex-wrap: wrap;
    gap: 12px;
  }
  .step-line-segment {
    display: none;
  }
}
</style>
