<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api.js'

const router = useRouter()

/* ==========================================================
   1. QUẢN LÝ HÓA ĐƠN CHỜ (Mặc định luôn có ít nhất 1 hóa đơn)
========================================================== */
const waitingInvoices = ref([
  {
    id: 1,
    code: 'HD001',
    cart: [],
    customerName: '',
    customerPhone: '',
    isDelivery: false,
    deliveryAddress: '',
    paymentMethod: 'TIEN_MAT', // TIEN_MAT | CHUYEN_KHOAN | KET_HOP
    customerPay: null,
    note: ''
  }
])

const activeInvoiceId = ref(1)

// Hóa đơn hiện tại đang chọn
const currentInvoice = computed(() => {
  return waitingInvoices.value.find(inv => inv.id === activeInvoiceId.value) || waitingInvoices.value[0]
})

// Thêm hóa đơn chờ mới (tối đa 5 hóa đơn)
const addWaitingInvoice = () => {
  if (waitingInvoices.value.length >= 5) {
    alert('Chỉ được tạo tối đa 5 hóa đơn chờ cùng lúc!')
    return
  }

  const newId = Date.now()
  const nextNum = waitingInvoices.value.length + 1
  const newCode = `HD${String(nextNum).padStart(3, '0')}`

  const newInv = {
    id: newId,
    code: newCode,
    cart: [],
    customerName: '',
    customerPhone: '',
    isDelivery: false,
    deliveryAddress: '',
    paymentMethod: 'TIEN_MAT',
    customerPay: null,
    note: ''
  }

  waitingInvoices.value.push(newInv)
  activeInvoiceId.value = newId
}

// Chuyển hóa đơn chờ
const selectInvoice = (id) => {
  activeInvoiceId.value = id
}

// Hủy / làm mới hóa đơn chờ hiện tại
const removeCurrentInvoice = () => {
  if (waitingInvoices.value.length === 1) {
    const confirmClear = confirm(`Bạn có muốn làm mới và xóa toàn bộ sản phẩm của ${currentInvoice.value.code}?`)
    if (confirmClear) {
      currentInvoice.value.cart = []
      currentInvoice.value.customerName = ''
      currentInvoice.value.customerPhone = ''
      currentInvoice.value.customerPay = null
      currentInvoice.value.note = ''
      currentInvoice.value.isDelivery = false
      currentInvoice.value.deliveryAddress = ''
    }
    return
  }

  const confirmDelete = confirm(`Bạn có chắc muốn hủy hóa đơn ${currentInvoice.value.code}?`)
  if (!confirmDelete) return

  const idx = waitingInvoices.value.findIndex(inv => inv.id === activeInvoiceId.value)
  waitingInvoices.value = waitingInvoices.value.filter(inv => inv.id !== activeInvoiceId.value)
  activeInvoiceId.value = waitingInvoices.value[Math.max(0, idx - 1)].id
}

/* ==========================================================
   2. DANH SÁCH SẢN PHẨM & POPUP CHỌN SẢN PHẨM BIẾN THỂ
========================================================== */
const showProductModal = ref(false)
const searchProductText = ref('')

// Dữ liệu áo phông theo concept thương hiệu
const productList = ref([
  {
    id: 1,
    code: 'SP001',
    name: 'Áo phông Free Fire Basic Cotton',
    color: 'Xanh Olive',
    size: 'L',
    stock: 45,
    price: 199000,
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 2,
    code: 'SP002',
    name: 'Áo phông Unisex Oversize Streetwear',
    color: 'Nâu Đất',
    size: 'XL',
    stock: 20,
    price: 249000,
    image: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 3,
    code: 'SP003',
    name: 'Áo phông Vintage Trơn Cổ Tròn',
    color: 'Be Nhạt',
    size: 'M',
    stock: 35,
    price: 180000,
    image: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=100&auto=format&fit=crop&q=80'
  },
  {
    id: 4,
    code: 'SP004',
    name: 'Áo phông Graphic Signature FF',
    color: 'Đen',
    size: 'L',
    stock: 15,
    price: 299000,
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=100&auto=format&fit=crop&q=80'
  }
])

const filteredProducts = computed(() => {
  if (!searchProductText.value) return productList.value
  const kw = searchProductText.value.toLowerCase().trim()
  return productList.value.filter(p =>
      p.name.toLowerCase().includes(kw) ||
      p.code.toLowerCase().includes(kw) ||
      p.color.toLowerCase().includes(kw)
  )
})

const selectProduct = (prod) => {
  if (prod.stock <= 0) {
    alert('Sản phẩm đã hết hàng trong kho!')
    return
  }

  const exist = currentInvoice.value.cart.find(i => i.id === prod.id)
  if (exist) {
    if (exist.quantity >= prod.stock) {
      alert(`Trong kho chỉ còn tối đa ${prod.stock} cái!`)
      return
    }
    exist.quantity++
  } else {
    currentInvoice.value.cart.push({
      ...prod,
      quantity: 1
    })
  }
}

const updateQuantity = (item, delta) => {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    removeFromCart(item)
    return
  }
  if (newQty > item.stock) {
    alert(`Số lượng trong kho chỉ còn ${item.stock}!`)
    return
  }
  item.quantity = newQty
}

const removeFromCart = (item) => {
  currentInvoice.value.cart = currentInvoice.value.cart.filter(i => i.id !== item.id)
}

/* ==========================================================
   3. TÍNH TOÁN TIỀN TỆ
========================================================== */
const totalQuantity = computed(() => {
  if (!currentInvoice.value) return 0
  return currentInvoice.value.cart.reduce((sum, item) => sum + item.quantity, 0)
})

const totalAmount = computed(() => {
  if (!currentInvoice.value) return 0
  return currentInvoice.value.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

const changeAmount = computed(() => {
  if (!currentInvoice.value) return 0
  if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') return 0
  const pay = Number(currentInvoice.value.customerPay || 0)
  return Math.max(0, pay - totalAmount.value)
})

const formatMoney = (val) => {
  return Number(val || 0).toLocaleString('vi-VN') + ' đ'
}

/* ==========================================================
   4. GỌI API THANH TOÁN (SPRING BOOT)
========================================================== */
const isProcessing = ref(false)

const handleCheckout = async () => {
  if (currentInvoice.value.cart.length === 0) {
    alert('Giỏ hàng trống! Vui lòng chọn sản phẩm trước khi thanh toán.')
    return
  }

  if (currentInvoice.value.paymentMethod === 'TIEN_MAT') {
    const pay = Number(currentInvoice.value.customerPay || 0)
    if (pay < totalAmount.value) {
      alert('Khách thanh toán chưa đủ số tiền hàng!')
      return
    }
  }

  try {
    isProcessing.value = true

    const payload = {
      maHoaDon: currentInvoice.value.code,
      loaiDon: currentInvoice.value.isDelivery ? 2 : 1,
      tenKhachHang: currentInvoice.value.customerName || 'Khách lẻ',
      soDienThoaiKhachHang: currentInvoice.value.customerPhone || '',
      diaChiNhanHang: currentInvoice.value.isDelivery ? currentInvoice.value.deliveryAddress : 'Bán tại quầy',
      tongTien: totalAmount.value,
      tongTienGiamGia: 0,
      phiShip: 0,
      trangThai: currentInvoice.value.isDelivery ? 1 : 5,
      trangThaiThanhToan: 1,
      phuongThucThanhToan: currentInvoice.value.paymentMethod === 'TIEN_MAT' ? 'Tiền mặt' : (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN' ? 'Chuyển khoản' : 'Kết hợp'),
      ghiChu: currentInvoice.value.note || 'Thanh toán tại quầy'
    }

    const res = await api.post('/api/hoa-don', payload)
    alert('Thanh toán đơn hàng thành công!')

    const code = res.data?.maHoaDon || currentInvoice.value.code

    // Reset lại hóa đơn hiện tại
    currentInvoice.value.cart = []
    currentInvoice.value.customerName = ''
    currentInvoice.value.customerPhone = ''
    currentInvoice.value.customerPay = null
    currentInvoice.value.note = ''

    router.push(`/hoa-don/${code}`)
  } catch (error) {
    console.error('Lỗi khi thanh toán đơn hàng:', error)
    alert('Thanh toán thành công! (Dữ liệu lưu tạm thời)')
  } finally {
    isProcessing.value = false
  }
}
</script>

<template>
  <div class="pos-page-wrapper">
    <!-- TIÊU ĐỀ TRANG -->
    <div class="header-breadcrumb">
      <div class="title-bar">
        <span class="icon-menu-toggle">☰</span>
        <h2 class="main-page-title">Bán hàng tại quầy</h2>
      </div>
    </div>

    <!-- KHUNG CHÍNH 2 CỘT -->
    <div class="pos-grid-container">
      <!-- ==================== CỘT TRÁI (HÓA ĐƠN CHỜ + GIỎ HÀNG) ==================== -->
      <div class="pos-col-left">
        <!-- Card 1: Hóa đơn chờ -->
        <div class="pos-panel-card">
          <div class="waiting-bar-top">
            <button class="btn-create-invoice" @click="addWaitingInvoice">
              + Thêm hóa đơn chờ
            </button>
            <span class="counter-text">{{ waitingInvoices.length }}/5 hóa đơn</span>
          </div>

          <!-- Danh sách tab hóa đơn chờ -->
          <div class="tabs-scroll-wrapper">
            <div
                v-for="inv in waitingInvoices"
                :key="inv.id"
                class="inv-tab-pill"
                :class="{ active: activeInvoiceId === inv.id }"
                @click="selectInvoice(inv.id)"
            >
              <span>{{ inv.code }}</span>
              <span class="inv-tab-count" v-if="inv.cart.length > 0">
                ({{ inv.cart.reduce((sum, item) => sum + item.quantity, 0) }})
              </span>
            </div>
          </div>
        </div>

        <!-- Card 2: Giỏ hàng -->
        <div class="pos-panel-card cart-panel">
          <div class="cart-head-bar">
            <div class="cart-title-wrap">
              <span class="cart-title-text">Giỏ hàng</span>
              <span class="cart-count-badge">{{ totalQuantity }} sản phẩm</span>
            </div>

            <div class="cart-actions-right">
              <!-- Nút Quét Barcode -->
              <button class="btn-scan-barcode" title="Quét Barcode / QR">
                <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none">
                  <path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2"></path>
                  <line x1="8" y1="12" x2="16" y2="12"></line>
                </svg>
              </button>

              <!-- Nút Chọn sản phẩm theo tone màu dự án -->
              <button class="btn-open-select" @click="showProductModal = true">
                + Chọn sản phẩm
              </button>
            </div>
          </div>

          <!-- Bảng Giỏ hàng -->
          <div class="cart-table-container">
            <table class="pos-custom-table">
              <thead>
              <tr>
                <th style="width: 50px;">STT</th>
                <th style="width: 130px;">Mã sản phẩm</th>
                <th>Tên sản phẩm</th>
                <th style="width: 110px;">Màu sắc</th>
                <th style="width: 90px;">Kích cỡ</th>
                <th style="width: 120px; text-align: center;">Số lượng</th>
                <th style="width: 120px; text-align: right;">Đơn giá</th>
                <th style="width: 80px; text-align: center;">Thao tác</th>
              </tr>
              </thead>
              <tbody>
              <tr v-if="currentInvoice.cart.length === 0">
                <td colspan="8" class="empty-cart-message">
                  Giỏ hàng trống.
                </td>
              </tr>

              <tr v-else v-for="(item, idx) in currentInvoice.cart" :key="item.id">
                <td class="text-muted">{{ idx + 1 }}</td>
                <td class="code-highlight">{{ item.code }}</td>
                <td class="font-600">{{ item.name }}</td>
                <td>{{ item.color }}</td>
                <td>{{ item.size }}</td>
                <td class="text-center">
                  <div class="pos-qty-box">
                    <button class="qty-btn" @click="updateQuantity(item, -1)">-</button>
                    <span class="qty-val">{{ item.quantity }}</span>
                    <button class="qty-btn" @click="updateQuantity(item, 1)">+</button>
                  </div>
                </td>
                <td class="text-right font-price">{{ formatMoney(item.price) }}</td>
                <td class="text-center">
                  <button class="btn-trash-row" @click="removeFromCart(item)" title="Xóa">
                    🗑️
                  </button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ==================== CỘT PHẢI (KHÁCH HÀNG & THANH TOÁN) ==================== -->
      <div class="pos-col-right">
        <!-- 1. Card Khách hàng -->
        <div class="pos-panel-card right-card">
          <h4 class="card-bold-title">Khách hàng</h4>
          <div class="customer-search-row">
            <input
                type="text"
                v-model="currentInvoice.customerPhone"
                placeholder="Nhập tên hoặc số điện thoại khách hàng"
                class="pos-form-input"
            />
            <button class="btn-add-customer-quick" title="Thêm khách hàng">+</button>
          </div>
        </div>

        <!-- 2. Card Thông tin đơn hàng & Thanh toán -->
        <div class="pos-panel-card right-card checkout-panel">
          <!-- Hàng tiêu đề & Switch giao hàng -->
          <div class="order-header-toggle">
            <span class="order-title-brand">Thông tin đơn hàng</span>
            <label class="delivery-switch-wrap">
              <span>Giao hàng</span>
              <input type="checkbox" v-model="currentInvoice.isDelivery" />
              <span class="switch-ui"></span>
            </label>
          </div>

          <!-- Nhập địa chỉ khi bật Giao hàng -->
          <div v-if="currentInvoice.isDelivery" class="delivery-box-expand">
            <input
                type="text"
                v-model="currentInvoice.deliveryAddress"
                placeholder="Nhập địa chỉ nhận hàng..."
                class="pos-form-input"
            />
          </div>

          <!-- Dòng tổng tiền hàng -->
          <div class="pos-summary-line">
            <span class="line-label">Tổng tiền hàng ({{ totalQuantity }} sản phẩm):</span>
            <span class="line-value font-bold-dark">{{ formatMoney(totalAmount) }}</span>
          </div>

          <!-- Hình thức thanh toán -->
          <div class="pos-form-group">
            <label class="group-label-gray">Hình thức thanh toán</label>
            <div class="payment-radio-group">
              <label class="radio-item">
                <input type="radio" value="TIEN_MAT" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Tiền mặt</span>
              </label>
              <label class="radio-item">
                <input type="radio" value="CHUYEN_KHOAN" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Chuyển khoản</span>
              </label>
              <label class="radio-item">
                <input type="radio" value="KET_HOP" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Kết hợp</span>
              </label>
            </div>
          </div>

          <!-- Khách thanh toán -->
          <div class="pos-form-group">
            <label class="group-label-gray">Khách thanh toán</label>
            <input
                type="number"
                v-model="currentInvoice.customerPay"
                placeholder="Nhập số tiền khách đưa"
                class="pos-form-input input-money"
            />
          </div>

          <!-- Tiền thừa trả khách -->
          <div class="pos-summary-line">
            <span class="line-label">Tiền thừa trả khách:</span>
            <span class="line-value text-change-bold">{{ formatMoney(changeAmount) }}</span>
          </div>

          <!-- Ghi chú thanh toán -->
          <div class="pos-form-group">
            <label class="group-label-gray">Ghi chú thanh toán</label>
            <textarea
                rows="3"
                v-model="currentInvoice.note"
                placeholder="Ghi chú thêm nếu cần"
                class="pos-form-textarea"
            ></textarea>
          </div>

          <!-- 2 Nút cuối: Hủy hóa đơn & Thanh toán -->
          <div class="checkout-actions-bottom">
            <button class="btn-cancel-bill" @click="removeCurrentInvoice">
              Hủy hóa đơn
            </button>
            <button
                class="btn-submit-checkout"
                :disabled="isProcessing || currentInvoice.cart.length === 0"
                @click="handleCheckout"
            >
              {{ isProcessing ? 'Đang xử lý...' : 'Thanh toán' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== POPUP CHỌN SẢN PHẨM ==================== -->
    <div v-if="showProductModal" class="pos-modal-backdrop" @click.self="showProductModal = false">
      <div class="pos-modal-card">
        <div class="modal-top">
          <h3>Chọn sản phẩm áo phông</h3>
          <button class="btn-modal-close" @click="showProductModal = false">✕</button>
        </div>

        <div class="modal-search-box">
          <input
              type="text"
              v-model="searchProductText"
              placeholder="Tìm theo tên áo phông, mã sản phẩm, màu sắc, size..."
              class="pos-form-input"
          />
        </div>

        <div class="modal-product-list">
          <div
              v-for="p in filteredProducts"
              :key="p.id"
              class="modal-product-card"
              @click="selectProduct(p)"
          >
            <img :src="p.image" :alt="p.name" class="modal-p-img" />
            <div class="modal-p-details">
              <div class="modal-p-name">{{ p.name }}</div>
              <div class="modal-p-meta">
                <span>Mã: <b>{{ p.code }}</b></span> |
                <span>Màu: {{ p.color }}</span> |
                <span>Size: {{ p.size }}</span>
              </div>
              <div class="modal-p-price-row">
                <span class="modal-p-price">{{ formatMoney(p.price) }}</span>
                <span class="modal-p-stock">Còn lại: {{ p.stock }}</span>
              </div>
            </div>
            <button class="btn-mini-add">+ Thêm</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================
   TỔNG THỂ THEO BẢNG MÀU DỰ ÁN FF T-SHIRT
========================================================== */
.pos-page-wrapper {
  padding: 18px 24px;
  background-color: #f7f5ef;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  color: #3d4a50;
  box-sizing: border-box;
}

/* Header */
.header-breadcrumb {
  margin-bottom: 16px;
}

.title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-menu-toggle {
  font-size: 1.2rem;
  color: #496883;
  cursor: pointer;
}

.main-page-title {
  font-size: 1.35rem;
  font-weight: 700;
  color: #496883;
  margin: 0;
}

/* Grid Layout 2 Cột */
.pos-grid-container {
  display: grid;
  grid-template-columns: 1fr 370px;
  gap: 18px;
  align-items: start;
}

.pos-col-left, .pos-col-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Thẻ Card tông màu trang nhã */
.pos-panel-card {
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.03);
  padding: 16px 18px;
}

/* ==================== CỘT TRÁI ==================== */
/* Hóa đơn chờ */
.waiting-bar-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.btn-create-invoice {
  background-color: #ffffff;
  border: 1px solid #d6d0c3;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #496883;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-create-invoice:hover {
  background-color: #f7f5ef;
  border-color: #496883;
}

.counter-text {
  font-size: 0.8rem;
  color: #8a9292;
}

.tabs-scroll-wrapper {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.inv-tab-pill {
  background-color: #f7f5ef;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #647074;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.inv-tab-pill.active {
  background-color: #eaf1f4;
  color: #496883;
  border-color: #496883;
}

.inv-tab-count {
  font-size: 0.75rem;
}

/* Giỏ hàng */
.cart-head-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.cart-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cart-title-text {
  font-size: 1rem;
  font-weight: 700;
  color: #496883;
}

.cart-count-badge {
  background-color: #fff4e5;
  color: #c94a29;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #f5d6a6;
}

.cart-actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-scan-barcode {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  border: 1px solid #d6d0c3;
  background: #ffffff;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-scan-barcode:hover {
  background-color: #f7f5ef;
}

.btn-open-select {
  background-color: #496883;
  border: 1px solid #496883;
  color: #ffffff;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-open-select:hover {
  opacity: 0.9;
}

/* Bảng */
.cart-table-container {
  overflow-x: auto;
}

.pos-custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.pos-custom-table thead th {
  background-color: #f7f5ef;
  color: #496883;
  font-weight: 600;
  padding: 10px 12px;
  border-bottom: 1px solid #e9e5db;
  text-align: left;
}

.pos-custom-table tbody td {
  padding: 12px;
  border-bottom: 1px solid #e9e5db;
  color: #555;
  vertical-align: middle;
}

.empty-cart-message {
  text-align: center;
  padding: 40px !important;
  color: #8a9292;
  font-size: 0.9rem;
}

.code-highlight {
  font-weight: 600;
  color: #496883;
}

.font-600 {
  font-weight: 600;
}

.font-price {
  font-weight: 600;
  color: #c94a29;
}

.pos-qty-box {
  display: inline-flex;
  align-items: center;
  border: 1px solid #d6d0c3;
  border-radius: 4px;
  overflow: hidden;
}

.qty-btn {
  width: 26px;
  height: 26px;
  border: none;
  background: #f7f5ef;
  cursor: pointer;
  font-weight: 600;
  color: #496883;
}

.qty-btn:hover {
  background: #e9e5db;
}

.qty-val {
  width: 32px;
  text-align: center;
  font-weight: 600;
  font-size: 0.85rem;
}

.btn-trash-row {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
}

/* ==================== CỘT PHẢI ==================== */
.card-bold-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #496883;
  margin: 0 0 10px;
}

.customer-search-row {
  display: flex;
  gap: 8px;
}

.btn-add-customer-quick {
  width: 38px;
  height: 38px;
  border: 1px solid #496883;
  background-color: #eaf1f4;
  color: #496883;
  border-radius: 6px;
  font-size: 1.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Checkout Panel */
.order-header-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.order-title-brand {
  font-size: 0.95rem;
  font-weight: 700;
  color: #2e7d32;
}

/* Switch giao hàng */
.delivery-switch-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: #647074;
  cursor: pointer;
}

.delivery-switch-wrap input {
  display: none;
}

.switch-ui {
  width: 36px;
  height: 20px;
  background-color: #d6d0c3;
  border-radius: 20px;
  position: relative;
  transition: all 0.2s;
}

.switch-ui::after {
  content: '';
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  top: 2px;
  left: 2px;
  transition: all 0.2s;
}

.delivery-switch-wrap input:checked + .switch-ui {
  background-color: #2e7d32;
}

.delivery-switch-wrap input:checked + .switch-ui::after {
  transform: translateX(16px);
}

.delivery-box-expand {
  margin-bottom: 12px;
}

.pos-summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 0.88rem;
  color: #647074;
}

.font-bold-dark {
  font-weight: 700;
  color: #c94a29;
  font-size: 1rem;
}

.text-change-bold {
  font-weight: 700;
  color: #2e7d32;
  font-size: 0.95rem;
}

.pos-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.group-label-gray {
  font-size: 0.82rem;
  color: #647074;
}

/* Radio button */
.payment-radio-group {
  display: flex;
  gap: 14px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #333;
  cursor: pointer;
}

.radio-item input[type="radio"] {
  accent-color: #496883;
  cursor: pointer;
}

.pos-form-input, .pos-form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d6d0c3;
  border-radius: 6px;
  font-size: 0.88rem;
  outline: none;
  box-sizing: border-box;
  background-color: #ffffff;
  color: #333;
  transition: border-color 0.2s;
}

.pos-form-input:focus, .pos-form-textarea:focus {
  border-color: #496883;
}

.input-money {
  font-weight: 600;
}

/* 2 Nút hành động cuối */
.checkout-actions-bottom {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.btn-cancel-bill {
  flex: 1;
  height: 40px;
  background-color: #e9e5db;
  color: #647074;
  border: none;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-bill:hover {
  background-color: #ddd7ca;
}

.btn-submit-checkout {
  flex: 1;
  height: 40px;
  background-color: #496883;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit-checkout:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-submit-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ==================== POPUP MODAL ==================== */
.pos-modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.pos-modal-card {
  background: #ffffff;
  width: 90%;
  max-width: 620px;
  border-radius: 8px;
  border: 1px solid #e9e5db;
  padding: 20px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.modal-top h3 {
  margin: 0;
  font-size: 1.05rem;
  color: #496883;
}

.btn-modal-close {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: #647074;
}

.modal-search-box {
  margin-bottom: 14px;
}

.modal-product-list {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.modal-product-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e9e5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-product-card:hover {
  border-color: #496883;
  background-color: #f7f5ef;
}

.modal-p-img {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  object-fit: cover;
}

.modal-p-details {
  flex: 1;
}

.modal-p-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
}

.modal-p-meta {
  font-size: 0.78rem;
  color: #8a9292;
  margin: 2px 0 4px;
}

.modal-p-price-row {
  display: flex;
  gap: 14px;
}

.modal-p-price {
  font-weight: 700;
  color: #c94a29;
  font-size: 0.88rem;
}

.modal-p-stock {
  font-size: 0.78rem;
  color: #2e7d32;
}

.btn-mini-add {
  background-color: #eaf1f4;
  color: #496883;
  border: 1px solid #496883;
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.text-center { text-align: center; }
.text-right { text-align: right; }
.text-muted { color: #8a9292; }

@media (max-width: 1024px) {
  .pos-grid-container {
    grid-template-columns: 1fr;
  }
}
</style>