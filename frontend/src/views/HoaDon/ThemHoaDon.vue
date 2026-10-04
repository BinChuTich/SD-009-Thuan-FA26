<template>
  <div class="add-invoice-page">

    <!-- Header -->
    <div class="page-header">
      <div>
        <div class="title-row">
          <span class="title-line"></span>
          <div>
            <h2>Thêm hóa đơn</h2>
            <p>Tạo hóa đơn mới cho khách hàng</p>
          </div>
        </div>
      </div>

      <button class="btn-back" @click="quayLai">
        ← Quay lại
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading">
      Đang lưu hóa đơn...
    </div>

    <!-- Error -->
    <div v-if="errorMessage" class="alert error">
      {{ errorMessage }}
    </div>

    <!-- Form -->
    <div class="form-container">

      <!-- Thông tin hóa đơn -->
      <div class="form-card">
        <div class="card-title">
          <span class="icon">🧾</span>
          <div>
            <h3>Thông tin hóa đơn</h3>
            <p>Nhập thông tin cơ bản của hóa đơn</p>
          </div>
        </div>

        <div class="form-grid">

          <!-- Mã hóa đơn -->
          <div class="form-group">
            <label>
              Mã hóa đơn <span>*</span>
            </label>

            <input
                v-model="form.maHoaDon"
                type="text"
                placeholder="VD: HD001"
            />

            <small>Nhập mã hóa đơn duy nhất</small>
          </div>

          <!-- Loại đơn -->
          <div class="form-group">
            <label>
              Loại đơn <span>*</span>
            </label>

            <select v-model="form.loaiDon">
              <option :value="1">Tại cửa hàng</option>
              <option :value="2">Online</option>
            </select>
          </div>

          <!-- Trạng thái -->
          <div class="form-group">
            <label>
              Trạng thái <span>*</span>
            </label>

            <select v-model="form.trangThai">
              <option :value="1">Chờ xác nhận</option>
              <option :value="2">Đã xác nhận</option>
              <option :value="3">Chờ vận chuyển</option>
              <option :value="4">Vận chuyển</option>
              <option :value="5">Đã hoàn thành</option>
              <option :value="6">Hủy</option>
            </select>
          </div>

          <!-- Người tạo -->
          <div class="form-group">
            <label>
              Người tạo
            </label>

            <input
                v-model="form.nguoiTao"
                type="text"
                placeholder="Nhập tên người tạo"
            />
          </div>

        </div>
      </div>


      <!-- Thông tin khách hàng -->
      <div class="form-card">

        <div class="card-title">
          <span class="icon">👤</span>
          <div>
            <h3>Thông tin khách hàng</h3>
            <p>Thông tin người mua hàng</p>
          </div>
        </div>

        <div class="form-grid">

          <!-- Tên khách hàng -->
          <div class="form-group">
            <label>
              Tên khách hàng <span>*</span>
            </label>

            <input
                v-model="form.tenKhachHang"
                type="text"
                placeholder="Nhập tên khách hàng"
            />
          </div>

          <!-- Số điện thoại -->
          <div class="form-group">
            <label>
              Số điện thoại
            </label>

            <input
                v-model="form.soDienThoaiKhachHang"
                type="text"
                placeholder="VD: 0987654321"
            />
          </div>

          <!-- Địa chỉ -->
          <div class="form-group full-width">
            <label>
              Địa chỉ nhận hàng
            </label>

            <textarea
                v-model="form.diaChiNhanHang"
                rows="3"
                placeholder="Nhập địa chỉ nhận hàng"
            ></textarea>
          </div>

        </div>
      </div>


      <!-- Thông tin thanh toán -->
      <div class="form-card">

        <div class="card-title">
          <span class="icon">💰</span>
          <div>
            <h3>Thông tin thanh toán</h3>
            <p>Nhập giá trị của hóa đơn</p>
          </div>
        </div>

        <div class="form-grid">

          <!-- Tổng tiền -->
          <div class="form-group">
            <label>
              Tổng tiền <span>*</span>
            </label>

            <div class="input-money">
              <input
                  v-model.number="form.tongTien"
                  type="number"
                  min="0"
                  placeholder="0"
              />
              <span>VNĐ</span>
            </div>
          </div>

          <!-- Phí vận chuyển -->
          <div class="form-group">
            <label>
              Phí vận chuyển
            </label>

            <div class="input-money">
              <input
                  v-model.number="form.phiVanChuyen"
                  type="number"
                  min="0"
                  placeholder="0"
              />
              <span>VNĐ</span>
            </div>
          </div>

          <!-- Tiền sau giảm -->
          <div class="form-group">
            <label>
              Tiền sau giảm giá
            </label>

            <div class="input-money">
              <input
                  v-model.number="form.tienSauGiamGia"
                  type="number"
                  min="0"
                  placeholder="0"
              />
              <span>VNĐ</span>
            </div>
          </div>

          <!-- Mã phiếu giảm giá -->
          <div class="form-group">
            <label>
              ID phiếu giảm giá
            </label>

            <input
                v-model.number="form.idPhieuGiamGia"
                type="number"
                min="1"
                placeholder="Có thể bỏ trống"
            />
          </div>

        </div>

        <!-- Tổng thanh toán -->
        <div class="total-box">
          <div>
            <span>Tổng thanh toán</span>
            <small>Đã bao gồm phí vận chuyển</small>
          </div>

          <strong>
            {{ formatMoney(tongThanhToan) }}
          </strong>
        </div>

      </div>


      <!-- Ghi chú -->
      <div class="form-card">

        <div class="card-title">
          <span class="icon">📝</span>
          <div>
            <h3>Ghi chú</h3>
            <p>Thông tin bổ sung cho hóa đơn</p>
          </div>
        </div>

        <div class="form-group">
          <textarea
              v-model="form.ghiChu"
              rows="4"
              placeholder="Nhập ghi chú nếu có..."
          ></textarea>
        </div>

      </div>


      <!-- Buttons -->
      <div class="action-buttons">

        <button
            type="button"
            class="btn-cancel"
            @click="quayLai"
            :disabled="loading"
        >
          Hủy
        </button>

        <button
            type="button"
            class="btn-save"
            @click="themHoaDon"
            :disabled="loading"
        >
          <span v-if="!loading">＋</span>
          {{ loading ? 'Đang lưu...' : 'Thêm hóa đơn' }}
        </button>

      </div>

    </div>
  </div>
</template>


<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')

/*
 * Form thêm hóa đơn
 */
const form = ref({
  maHoaDon: '',
  loaiDon: 1,
  trangThai: 1,

  nguoiTao: 'Admin',

  tenKhachHang: '',
  soDienThoaiKhachHang: '',
  diaChiNhanHang: '',

  tongTien: 0,
  phiVanChuyen: 0,
  tienSauGiamGia: 0,

  idPhieuGiamGia: null,

  ghiChu: ''
})


/*
 * Tổng thanh toán
 */
const tongThanhToan = computed(() => {
  const tien = Number(form.value.tienSauGiamGia || 0)
  const phiShip = Number(form.value.phiVanChuyen || 0)

  return tien + phiShip
})


/*
 * Format tiền
 */
const formatMoney = (money) => {
  return Number(money || 0).toLocaleString('vi-VN') + 'đ'
}


/*
 * Thêm hóa đơn
 */
const themHoaDon = async () => {

  errorMessage.value = ''

  // Kiểm tra mã hóa đơn
  if (!form.value.maHoaDon.trim()) {
    errorMessage.value = 'Vui lòng nhập mã hóa đơn.'
    return
  }

  // Kiểm tra khách hàng
  if (!form.value.tenKhachHang.trim()) {
    errorMessage.value = 'Vui lòng nhập tên khách hàng.'
    return
  }

  // Kiểm tra tổng tiền
  if (Number(form.value.tongTien) < 0) {
    errorMessage.value = 'Tổng tiền không hợp lệ.'
    return
  }

  try {

    loading.value = true

    const data = {
      maHoaDon: form.value.maHoaDon.trim(),

      loaiDon: Number(form.value.loaiDon),

      trangThai: Number(form.value.trangThai),

      nguoiTao: form.value.nguoiTao?.trim() || 'Admin',

      tenKhachHang: form.value.tenKhachHang.trim(),

      soDienThoaiKhachHang:
          form.value.soDienThoaiKhachHang?.trim() || null,

      diaChiNhanHang:
          form.value.diaChiNhanHang?.trim() || null,

      tongTien: Number(form.value.tongTien || 0),

      phiVanChuyen: Number(form.value.phiVanChuyen || 0),

      tienSauGiamGia: Number(
          form.value.tienSauGiamGia || form.value.tongTien || 0
      ),

      idPhieuGiamGia:
          form.value.idPhieuGiamGia
              ? Number(form.value.idPhieuGiamGia)
              : null,

      ghiChu:
          form.value.ghiChu?.trim() || null
    }

    console.log('Dữ liệu gửi lên:', data)

    await api.post('/api/hoa-don', data)

    alert('Thêm hóa đơn thành công!')

    router.push('/hoa-don')

  } catch (error) {

    console.error('Lỗi thêm hóa đơn:', error)

    if (error.response) {
      console.error('Response:', error.response.data)

      errorMessage.value =
          error.response.data?.message ||
          'Không thể thêm hóa đơn. Vui lòng kiểm tra dữ liệu.'
    } else {
      errorMessage.value =
          'Không thể kết nối đến server.'
    }

  } finally {
    loading.value = false
  }
}


/*
 * Quay lại danh sách
 */
const quayLai = () => {
  router.push('/hoa-don')
}
</script>


<style scoped>

* {
  box-sizing: border-box;
}

.add-invoice-page {
  min-height: 100vh;
  background: #f7f5ef;
  padding: 28px 35px 50px;
}


/* =========================
   HEADER
========================= */

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
  height: 50px;
  background: #496883;
  border-radius: 4px;
}

.title-row h2 {
  margin: 0;
  color: #2f3e4d;
  font-size: 26px;
  font-weight: 700;
}

.title-row p {
  margin: 6px 0 0;
  color: #777;
  font-size: 14px;
}

.btn-back {
  border: 1px solid #ddd8cc;
  background: white;
  color: #496883;
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: .2s;
}

.btn-back:hover {
  background: #496883;
  color: white;
}


/* =========================
   ALERT
========================= */

.alert {
  padding: 13px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.alert.error {
  background: #fff0ed;
  color: #c0392b;
  border: 1px solid #f3c6bf;
}


/* =========================
   FORM CARD
========================= */

.form-container {
  max-width: 1200px;
  margin: auto;
}

.form-card {
  background: white;
  border: 1px solid #e9e5db;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .03);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 25px;
  padding-bottom: 17px;
  border-bottom: 1px solid #eee;
}

.card-title .icon {
  width: 40px;
  height: 40px;
  background: #f1f4f7;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.card-title h3 {
  margin: 0;
  color: #334454;
  font-size: 17px;
}

.card-title p {
  margin: 4px 0 0;
  color: #999;
  font-size: 13px;
}


/* =========================
   GRID
========================= */

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 25px;
}

.full-width {
  grid-column: 1 / -1;
}


/* =========================
   FORM
========================= */

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  color: #3e4d5b;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.form-group label span {
  color: #d9534f;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  border: 1px solid #ddd9d0;
  border-radius: 7px;
  padding: 11px 13px;
  outline: none;
  font-size: 14px;
  color: #333;
  background: white;
  transition: .2s;
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #496883;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, .08);
}

.form-group textarea {
  resize: vertical;
}

.form-group small {
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}


/* =========================
   MONEY
========================= */

.input-money {
  position: relative;
}

.input-money input {
  padding-right: 60px;
}

.input-money span {
  position: absolute;
  right: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: #888;
  font-size: 13px;
}


/* =========================
   TOTAL
========================= */

.total-box {
  margin-top: 25px;
  padding: 18px 20px;
  background: #f7f8fa;
  border-radius: 9px;
  border: 1px solid #e7e9eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-box div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.total-box span {
  font-size: 15px;
  font-weight: 600;
  color: #3d4c5a;
}

.total-box small {
  color: #999;
}

.total-box strong {
  font-size: 22px;
  color: #c94a29;
}


/* =========================
   BUTTONS
========================= */

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 25px;
}

.btn-cancel,
.btn-save {
  min-width: 130px;
  padding: 12px 22px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: .2s;
}

.btn-cancel {
  background: white;
  border: 1px solid #d8d4ca;
  color: #555;
}

.btn-cancel:hover {
  background: #f3f1ed;
}

.btn-save {
  background: #496883;
  color: white;
  border: 1px solid #496883;
}

.btn-save:hover {
  background: #3d596f;
}

.btn-save:disabled,
.btn-cancel:disabled {
  opacity: .6;
  cursor: not-allowed;
}


/* =========================
   LOADING
========================= */

.loading {
  text-align: center;
  padding: 15px;
  margin-bottom: 15px;
  color: #496883;
  background: white;
  border-radius: 8px;
}


/* =========================
   RESPONSIVE
========================= */

@media (max-width: 768px) {

  .add-invoice-page {
    padding: 20px 15px 40px;
  }

  .page-header {
    align-items: flex-start;
    gap: 15px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: auto;
  }

  .form-card {
    padding: 18px;
  }

  .total-box {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .action-buttons {
    flex-direction: column-reverse;
  }

  .btn-cancel,
  .btn-save {
    width: 100%;
  }
}

</style>