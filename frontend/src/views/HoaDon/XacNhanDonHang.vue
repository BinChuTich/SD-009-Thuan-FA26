<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'
const route = useRoute()
const router = useRouter()
const maHoaDon = route.params.maHoaDon
const hoaDon = ref(null)
const loading = ref(false)
const saving = ref(false)
const paying = ref(false)
const errorMessage = ref('')

const loadHoaDon = async () => {
  try {
    loading.value = true
    errorMessage.value = ''
    const response = await api.get(`/api/hoa-don/code/${maHoaDon}`)
    hoaDon.value = response.data
  } catch (error) {
    console.error('Lỗi tải hóa đơn:', error)
    errorMessage.value = 'Không thể tải thông tin hóa đơn.'
  } finally {
    loading.value = false
  }
}

const formatMoney = (money) => {
  if (money == null) return '0đ'
  return Number(money).toLocaleString('vi-VN') + 'đ'
}

const getStatusText = (status) => {
  const map = {
    1: 'Chờ xác nhận',
    2: 'Đã xác nhận',
    3: 'Chờ vận chuyển',
    4: 'Vận chuyển',
    5: 'Đã hoàn thành',
    6: 'Hủy'
  }
  return map[Number(status)] || 'Chưa cập nhật'
}

const nextStatus = computed(() => {
  if (!hoaDon.value) return null
  const currentStatus = Number(hoaDon.value.trangThai)
  return currentStatus >= 1 && currentStatus <= 4 ? currentStatus + 1 : null
})

const actionText = computed(() => {
  if (!hoaDon.value) return ''
  switch (Number(hoaDon.value.trangThai)) {
    case 1:
      return '✓ Xác nhận đơn hàng'
    case 2:
      return '✓ Chuyển chờ vận chuyển'
    case 3:
      return '✓ Chuyển vận chuyển'
    case 4:
      return '✓ Hoàn thành đơn hàng'
    default:
      return ''
  }
})

const isPaid = computed(() => {
  return Number(hoaDon.value?.trangThaiThanhToan) === 1
})

const finalTotalPayment = computed(() => {
  if (!hoaDon.value) return 0
  if (hoaDon.value.tongTien != null) {
    return Number(hoaDon.value.tongTien)
  }
  const hang = Number(hoaDon.value.tongTienHang || 0)
  const ship = Number(hoaDon.value.phiShip || 0)
  const giam = Number(hoaDon.value.tongTienGiamGia || 0)
  return Math.max(0, hang + ship - giam)
})

const xacNhanDon = async () => {
  if (!hoaDon.value) return
  const status = nextStatus.value
  if (!status) return

  const confirmed = window.confirm(
      `Bạn có chắc muốn chuyển đơn hàng sang "${getStatusText(status)}"?`
  )
  if (!confirmed) return

  try {
    saving.value = true
    await api.put(`/api/hoa-don/${hoaDon.value.id}`, { trangThai: status })
    hoaDon.value.trangThai = status
    alert(`Đã chuyển đơn hàng sang "${getStatusText(status)}"!`)
  } catch (error) {
    console.error('Lỗi cập nhật trạng thái:', error)
    alert(error.response?.data?.message || 'Không thể cập nhật trạng thái đơn hàng!')
  } finally {
    saving.value = false
  }
}

const thanhToan = async () => {
  if (!hoaDon.value || isPaid.value) return

  const confirmed = window.confirm('Bạn có chắc hóa đơn này đã được thanh toán?')
  if (!confirmed) return

  try {
    paying.value = true
    await api.put(`/api/hoa-don/${hoaDon.value.id}`, { trangThaiThanhToan: 1 })
    hoaDon.value.trangThaiThanhToan = 1
    alert('Thanh toán thành công!')
  } catch (error) {
    console.error('Lỗi cập nhật thanh toán:', error)
    alert(error.response?.data?.message || 'Không thể cập nhật trạng thái thanh toán!')
  } finally {
    paying.value = false
  }
}

const quayLai = () => {
  router.push(`/hoa-don/${maHoaDon}`)
}

onMounted(() => {
  loadHoaDon()
})
</script>

<template>
  <div class="confirm-page">
    <!-- HEADER -->
    <div class="page-header">
      <div class="title-row">
        <span class="title-line"></span>
        <div>
          <h2>Xử lý hóa đơn</h2>
          <p>Hóa đơn: {{ maHoaDon }}</p>
        </div>
      </div>

      <button class="btn-back" @click="quayLai">
        ← Quay lại
      </button>
    </div>

    <!-- LOADING & ERROR -->
    <div v-if="loading" class="loading">
      Đang tải thông tin hóa đơn...
    </div>

    <div v-else-if="errorMessage" class="error-box">
      {{ errorMessage }}
    </div>

    <!-- MAIN CONTENT -->
    <div v-else-if="hoaDon" class="content">
      <!-- KHUNG THÔNG TIN HÓA ĐƠN -->
      <div class="card">
        <div class="card-header">
          <div>
            <h3>Thông tin hóa đơn</h3>
            <p>Kiểm tra thông tin trước khi xử lý</p>
          </div>

          <div class="status-group">
            <span class="status">
              {{ getStatusText(hoaDon.trangThai) }}
            </span>
            <span v-if="isPaid" class="payment-paid">
              ✓ Đã thanh toán
            </span>
            <span v-else class="payment-unpaid">
              Chưa thanh toán
            </span>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="label">Mã hóa đơn</span>
            <strong>{{ hoaDon.maHoaDon }}</strong>
          </div>

          <div class="info-item">
            <span class="label">Khách hàng</span>
            <strong>{{ hoaDon.tenKhachHang || 'Khách lẻ' }}</strong>
          </div>

          <div class="info-item">
            <span class="label">Số điện thoại</span>
            <strong>{{ hoaDon.soDienThoaiKhachHang || hoaDon.soDienThoai || '---' }}</strong>
          </div>

          <div class="info-item">
            <span class="label">Tổng thanh toán</span>
            <strong class="price">{{ formatMoney(finalTotalPayment) }}</strong>
          </div>
        </div>
      </div>

      <!-- KHUNG THANH TOÁN -->
      <div class="card">
        <div class="card-header">
          <div>
            <h3>Thanh toán</h3>
            <p>Trạng thái thanh toán của hóa đơn</p>
          </div>
        </div>

        <div v-if="isPaid" class="paid-box">
          <span class="paid-icon">✓</span>
          <div>
            <strong>Đã thanh toán</strong>
            <p>Hóa đơn này đã được xác nhận thanh toán.</p>
          </div>
        </div>

        <div v-else class="unpaid-box">
          <div>
            <strong>Chưa thanh toán</strong>
            <p>Xác nhận ngay khi khách hàng đã chuyển khoản hoặc thanh toán tiền mặt.</p>
          </div>

          <button class="btn-payment" @click="thanhToan" :disabled="paying">
            {{ paying ? 'Đang xử lý...' : '✓ Xác nhận đã thanh toán' }}
          </button>
        </div>
      </div>

      <!-- TIẾN TRÌNH TRẠNG THÁI -->
      <div class="card">
        <div class="card-header">
          <div>
            <h3>Trạng thái đơn hàng</h3>
            <p>Tiến trình xử lý đơn hàng</p>
          </div>
        </div>

        <div class="status-flow">
          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 1 }">
            <div class="flow-number">1</div>
            <span>Chờ xác nhận</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 2 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 2 }">
            <div class="flow-number">2</div>
            <span>Đã xác nhận</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 3 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 3 }">
            <div class="flow-number">3</div>
            <span>Chờ vận chuyển</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 4 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 4 }">
            <div class="flow-number">4</div>
            <span>Vận chuyển</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 5 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 5 }">
            <div class="flow-number">5</div>
            <span>Đã hoàn thành</span>
          </div>
        </div>
      </div>

      <!-- NÚT THAO TÁC CUỐI TRANG -->
      <div class="action-buttons">
        <button class="btn-cancel" @click="quayLai" :disabled="saving || paying">
          Hủy
        </button>

        <button
            v-if="nextStatus"
            class="btn-confirm"
            @click="xacNhanDon"
            :disabled="saving || paying"
        >
          {{ saving ? 'Đang xử lý...' : actionText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.confirm-page {
  min-height: 100vh;
  background: #f7f5ef;
  padding: 30px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title-line {
  width: 5px;
  height: 48px;
  background: #496883;
  border-radius: 4px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.page-header p {
  margin: 5px 0 0;
  color: #777;
}

.btn-back {
  padding: 10px 18px;
  border: 1px solid #ddd8cc;
  border-radius: 8px;
  background: white;
  color: #496883;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #f0ebe1;
}

.content {
  max-width: 1000px;
  margin: 0 auto;
}

.card {
  background: white;
  border: 1px solid #e9e5db;
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  color: #333;
  font-size: 17px;
}

.card-header p {
  margin: 5px 0 0;
  color: #888;
  font-size: 13px;
}

.status-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status,
.payment-paid,
.payment-unpaid {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status {
  background: #dbeafe;
  color: #1d4ed8;
}

.payment-paid {
  background: #dcfce7;
  color: #15803d;
}

.payment-unpaid {
  background: #fff3cd;
  color: #856404;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px 30px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  color: #888;
  font-size: 13px;
}

.info-item strong {
  color: #333;
}

.price {
  color: #c94a29 !important;
  font-size: 1.15rem;
}

/* KHỐI THANH TOÁN */
.paid-box,
.unpaid-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border-radius: 10px;
}

.paid-box {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  justify-content: flex-start;
}

.unpaid-box {
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.paid-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #dcfce7;
  color: #15803d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}

.paid-box strong,
.unpaid-box strong {
  color: #333;
}

.paid-box p,
.unpaid-box p {
  margin: 5px 0 0;
  color: #777;
  font-size: 13px;
}

.btn-payment {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: #496883;
  color: white;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.btn-payment:hover {
  background: #3d596f;
}

/* FLOW TRẠNG THÁI */
.status-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.flow-item {
  min-width: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  color: #aaa;
  font-size: 12px;
}

.flow-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #eee;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.flow-item.active {
  color: #496883;
  font-weight: 600;
}

.flow-item.active .flow-number {
  background: #496883;
  color: white;
}

.flow-line {
  flex: 1;
  height: 2px;
  background: #e5e5e5;
  margin-bottom: 24px;
}

.flow-line.active {
  background: #496883;
}

/* THAO TÁC */
.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-bottom: 30px;
}

.btn-cancel {
  padding: 11px 22px;
  border-radius: 8px;
  border: 1px solid #ddd8cc;
  background: white;
  color: #555;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f0ebe1;
}

.btn-confirm {
  padding: 11px 22px;
  border-radius: 8px;
  border: 1px solid #496883;
  background: #496883;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm:hover {
  background: #3d596f;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: 50px;
  color: #777;
}

.error-box {
  padding: 15px;
  border-radius: 8px;
  background: #fee2e2;
  color: #b91c1c;
}

@media (max-width: 768px) {
  .confirm-page {
    padding: 15px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .status-flow {
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 10px;
  }

  .flow-item {
    min-width: 100px;
  }

  .paid-box,
  .unpaid-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
}
</style>