<template>
  <div class="variant-page-container">
    <!-- Tiêu đề và nút quay lại -->
    <div class="page-header-row">
      <div class="header-left">
        <button class="btn-back" @click="goBackToProducts" title="Quay lại danh sách sản phẩm">
          ← Quay lại
        </button>
        <h2 class="main-page-title">QUẢN LÝ BIẾN THỂ SẢN PHẨM (CHI TIẾT SẢN PHẨM)</h2>
      </div>

      <div v-if="toast.show" :class="['toast-notification', toast.type]">
        {{ toast.message }}
      </div>
    </div>

    <!-- Khối chọn & tóm tắt thông tin sản phẩm -->
    <div class="product-summary-card">
      <div class="summary-top">
        <div class="product-picker-group">
          <label class="picker-label">Chọn sản phẩm cần quản lý:</label>
          <select v-model="selectedProductId" @change="onProductChange" class="product-select">
            <option :value="null">-- Chọn sản phẩm --</option>
            <option v-for="sp in productList" :key="sp.id" :value="sp.id">
              [{{ sp.maSanPham }}] {{ sp.tenSanPham }}
            </option>
          </select>
        </div>

        <button class="btn btn-add" @click="openCreateModal" :disabled="!selectedProduct">
          + Thêm biến thể mới
        </button>
      </div>

      <div v-if="selectedProduct" class="product-info-details">
        <div class="info-badge"><b>Mã SP:</b> {{ selectedProduct.maSanPham }}</div>
        <div class="info-badge"><b>Tên SP:</b> {{ selectedProduct.tenSanPham }}</div>
        <div class="info-badge" v-if="selectedProduct.tenThuongHieu"><b>Thương hiệu:</b> {{ selectedProduct.tenThuongHieu }}</div>
        <div class="info-badge" v-if="selectedProduct.tenDanhMuc"><b>Danh mục:</b> {{ selectedProduct.tenDanhMuc }}</div>
        <div class="info-badge" v-if="selectedProduct.tenChatLieu"><b>Chất liệu:</b> {{ selectedProduct.tenChatLieu }}</div>
        <div class="info-badge stat-badge">
          <b>Tổng số biến thể:</b> {{ variants.length }}
        </div>
        <div class="info-badge stat-badge">
          <b>Tổng tồn kho:</b> {{ totalInventory }}
        </div>
      </div>
    </div>

    <!-- Khối bảng dữ liệu biến thể -->
    <div class="variant-card">
      <!-- Bộ lọc biến thể -->
      <div class="filter-bar">
        <div class="filter-inputs">
          <!-- Kích cỡ -->
          <select v-model="filters.idKichCo" @change="handleFilter" class="filter-select">
            <option value="">-- Tất cả Kích cỡ --</option>
            <option v-for="item in attributes.kichCo" :key="item.id" :value="item.id">
              {{ item.tenKichCo }}
            </option>
          </select>

          <!-- Màu sắc -->
          <select v-model="filters.idMauSac" @change="handleFilter" class="filter-select">
            <option value="">-- Tất cả Màu sắc --</option>
            <option v-for="item in attributes.mauSac" :key="item.id" :value="item.id">
              {{ item.tenMauSac }}
            </option>
          </select>

          <!-- Trạng thái -->
          <select v-model="filters.trangThai" @change="handleFilter" class="filter-select">
            <option value="">-- Tất cả trạng thái --</option>
            <option :value="1">Đang kinh doanh</option>
            <option :value="0">Ngừng kinh doanh</option>
          </select>

          <!-- Khoảng giá -->
          <input
              type="number"
              v-model="filters.minGia"
              placeholder="Giá từ..."
              class="filter-input-price"
              @keyup.enter="handleFilter"
          />
          <input
              type="number"
              v-model="filters.maxGia"
              placeholder="Giá đến..."
              class="filter-input-price"
              @keyup.enter="handleFilter"
          />

          <button class="btn btn-search" @click="handleFilter">
            🔍 Lọc
          </button>
          <button class="btn btn-refresh" @click="resetFilters">
            🔄 Làm mới
          </button>
        </div>
      </div>

      <!-- Bảng biến thể -->
      <div class="table-responsive">
        <table class="variant-table">
          <thead>
          <tr>
            <th style="width: 50px; text-align: center;">STT</th>
            <th style="width: 140px;">Mã biến thể</th>
            <th>Tên sản phẩm</th>
            <th style="width: 110px; text-align: center;">Kích cỡ</th>
            <th style="width: 140px; text-align: center;">Màu sắc</th>
            <th style="width: 130px; text-align: right;">Giá bán</th>
            <th style="width: 120px; text-align: center;">Số lượng tồn</th>
            <th style="width: 130px;">Ngày tạo</th>
            <th style="width: 140px; text-align: center;">Trạng thái</th>
            <th style="width: 100px; text-align: center;">Thao tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loading">
            <td colspan="10" class="empty-state">
              <div class="loading-spinner"></div> Đang tải danh sách biến thể...
            </td>
          </tr>
          <tr v-else-if="!selectedProductId">
            <td colspan="10" class="empty-state">
              Vui lòng chọn một sản phẩm ở trên để xem danh sách biến thể!
            </td>
          </tr>
          <tr v-else-if="variants.length === 0">
            <td colspan="10" class="empty-state">
              Sản phẩm này chưa có biến thể nào phù hợp với bộ lọc! Hãy nhấn <b>"+ Thêm biến thể mới"</b> để bổ sung.
            </td>
          </tr>
          <tr v-for="(v, index) in variants" :key="v.id">
            <!-- STT -->
            <td style="text-align: center;" class="stt-cell">{{ index + 1 }}</td>

            <!-- Mã biến thể -->
            <td class="code-cell">
              <b>{{ v.maChiTietSanPham }}</b>
            </td>

            <!-- Tên SP -->
            <td class="product-name-cell">{{ v.tenSanPham }}</td>

            <!-- Kích cỡ -->
            <td style="text-align: center;">
              <span class="size-badge">{{ v.tenKichCo }}</span>
            </td>

            <!-- Màu sắc kèm swatch -->
            <td style="text-align: center;">
              <div class="color-badge-wrap">
                <span
                    class="color-dot"
                    :style="{ backgroundColor: v.maHex || '#ccc' }"
                    :title="v.tenMauSac"
                ></span>
                <span class="color-text">{{ v.tenMauSac }}</span>
              </div>
            </td>

            <!-- Giá bán -->
            <td style="text-align: right;" class="price-cell">
              <b>{{ formatMoney(v.giaBan) }}</b>
            </td>

            <!-- Số lượng tồn -->
            <td style="text-align: center;">
              <span :class="['stock-pill', v.soLuong > 0 ? 'stock-ok' : 'stock-out']">
                {{ v.soLuong }}
              </span>
            </td>

            <!-- Ngày tạo -->
            <td class="date-cell">{{ formatDate(v.ngayTao) }}</td>

            <!-- Trạng thái -->
            <td style="text-align: center;">
              <button
                  :class="['status-toggle-btn', v.trangThai === 1 ? 'status-active' : 'status-inactive']"
                  @click="toggleStatus(v)"
                  title="Nhấn để thay đổi trạng thái"
              >
                {{ v.trangThai === 1 ? 'Kinh doanh' : 'Ngừng KD' }}
              </button>
            </td>

            <!-- Thao tác -->
            <td style="text-align: center;">
              <div class="action-buttons-group">
                <button
                    class="btn-action btn-edit"
                    @click="openEditModal(v)"
                    title="Chỉnh sửa biến thể"
                >
                  ✏️
                </button>
                <button
                    class="btn-action btn-delete"
                    @click="confirmDelete(v)"
                    title="Xóa biến thể"
                >
                  🗑️
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL THÊM / CẬP NHẬT BIẾN THỂ -->
    <div class="modal-backdrop" v-if="showModal" @click.self="closeModal">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 class="modal-title">{{ isEdit ? 'CẬP NHẬT BIẾN THỂ' : 'THÊM MỚI BIẾN THỂ' }}</h3>
          <button class="btn-close-modal" @click="closeModal">✕</button>
        </div>

        <form @submit.prevent="handleSubmitVariant" class="modal-form">
          <div class="form-grid">
            <!-- Sản phẩm -->
            <div class="form-group full-width">
              <label class="form-label">Sản phẩm <span class="required">*</span></label>
              <select v-model="variantForm.idSanPham" class="form-control" :disabled="isEdit">
                <option v-for="sp in productList" :key="sp.id" :value="sp.id">
                  [{{ sp.maSanPham }}] {{ sp.tenSanPham }}
                </option>
              </select>
            </div>

            <!-- Mã biến thể -->
            <div class="form-group">
              <label class="form-label">Mã biến thể</label>
              <input
                  type="text"
                  v-model="variantForm.maChiTietSanPham"
                  placeholder="Để trống để tự động sinh mã"
                  class="form-control"
              />
            </div>

            <!-- Trạng thái -->
            <div class="form-group">
              <label class="form-label">Trạng thái</label>
              <select v-model="variantForm.trangThai" class="form-control">
                <option :value="1">Đang kinh doanh</option>
                <option :value="0">Ngừng kinh doanh</option>
              </select>
            </div>

            <!-- Kích cỡ -->
            <div class="form-group">
              <label class="form-label">Kích cỡ <span class="required">*</span></label>
              <select v-model="variantForm.idKichCo" required class="form-control">
                <option :value="null">-- Chọn kích cỡ --</option>
                <option v-for="item in attributes.kichCo" :key="item.id" :value="item.id">
                  {{ item.tenKichCo }}
                </option>
              </select>
            </div>

            <!-- Màu sắc -->
            <div class="form-group">
              <label class="form-label">Màu sắc <span class="required">*</span></label>
              <select v-model="variantForm.idMauSac" required class="form-control">
                <option :value="null">-- Chọn màu sắc --</option>
                <option v-for="item in attributes.mauSac" :key="item.id" :value="item.id">
                  {{ item.tenMauSac }}
                </option>
              </select>
            </div>

            <!-- Số lượng tồn -->
            <div class="form-group">
              <label class="form-label">Số lượng tồn <span class="required">*</span></label>
              <input
                  type="number"
                  v-model.number="variantForm.soLuong"
                  min="0"
                  required
                  placeholder="0"
                  class="form-control"
              />
            </div>

            <!-- Giá bán -->
            <div class="form-group">
              <label class="form-label">Giá bán (VNĐ) <span class="required">*</span></label>
              <input
                  type="number"
                  v-model.number="variantForm.giaBan"
                  min="0"
                  step="1000"
                  required
                  placeholder="0"
                  class="form-control"
              />
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeModal">Hủy bỏ</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? 'Đang lưu...' : (isEdit ? 'Cập nhật' : 'Thêm mới') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'

const route = useRoute()
const router = useRouter()

// Sản phẩm được chọn
const productList = ref([])
const selectedProductId = ref(null)
const selectedProduct = computed(() => {
  return productList.value.find(p => p.id === selectedProductId.value) || null
})

// Dữ liệu thuộc tính
const attributes = ref({
  mauSac: [],
  kichCo: []
})

// Danh sách biến thể
const variants = ref([])
const loading = ref(false)

// Bộ lọc
const filters = ref({
  idKichCo: '',
  idMauSac: '',
  trangThai: '',
  minGia: '',
  maxGia: ''
})

// Modal & Form
const showModal = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)

const variantForm = ref({
  idSanPham: null,
  idKichCo: null,
  idMauSac: null,
  maChiTietSanPham: '',
  soLuong: 10,
  giaBan: 200000,
  trangThai: 1
})

// Toast
const toast = ref({
  show: false,
  message: '',
  type: 'success'
})

const showToast = (message, type = 'success') => {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// Tổng tồn kho
const totalInventory = computed(() => {
  return variants.value.reduce((acc, curr) => acc + (curr.soLuong || 0), 0)
})

// Định dạng tiền
const formatMoney = (val) => {
  if (val == null) return '0 đ'
  return Number(val).toLocaleString('vi-VN') + ' đ'
}

// Định dạng ngày
const formatDate = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleDateString('vi-VN')
}

// Quay lại danh sách sản phẩm
const goBackToProducts = () => {
  router.push('/san-pham')
}

// Lấy danh mục thuộc tính
const fetchAttributes = async () => {
  try {
    const res = await api.get('/api/thuoc-tinh')
    if (res.data) {
      attributes.value = res.data
    }
  } catch (err) {
    console.error('Lỗi tải thuộc tính:', err)
  }
}

// Lấy danh sách sản phẩm để chọn
const fetchProductList = async () => {
  try {
    const res = await api.get('/api/san-pham', { params: { size: 1000 } })
    if (res.data) {
      productList.value = res.data.content || []

      // Kiểm tra xem query param có sanPhamId không
      const queryId = route.query.sanPhamId
      if (queryId) {
        selectedProductId.value = Number(queryId)
        fetchVariants()
      } else if (productList.value.length > 0) {
        selectedProductId.value = productList.value[0].id
        fetchVariants()
      }
    }
  } catch (err) {
    console.error('Lỗi tải danh sách sản phẩm:', err)
  }
}

// Khi đổi sản phẩm
const onProductChange = () => {
  filters.value = {
    idKichCo: '',
    idMauSac: '',
    trangThai: '',
    minGia: '',
    maxGia: ''
  }
  fetchVariants()
}

// Tải danh sách biến thể
const fetchVariants = async () => {
  if (!selectedProductId.value) {
    variants.value = []
    return
  }

  loading.value = true
  try {
    const params = {
      idSanPham: selectedProductId.value,
      size: 500
    }
    if (filters.value.idKichCo) params.idKichCo = filters.value.idKichCo
    if (filters.value.idMauSac) params.idMauSac = filters.value.idMauSac
    if (filters.value.trangThai !== '') params.trangThai = filters.value.trangThai
    if (filters.value.minGia !== '') params.minGia = filters.value.minGia
    if (filters.value.maxGia !== '') params.maxGia = filters.value.maxGia

    const res = await api.get('/api/chi-tiet-san-pham/filter', { params })
    if (res.data) {
      variants.value = res.data.content || []
    }
  } catch (err) {
    console.error('Lỗi tải biến thể:', err)
    showToast('Lỗi khi tải danh sách biến thể!', 'error')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  fetchVariants()
}

const resetFilters = () => {
  filters.value = {
    idKichCo: '',
    idMauSac: '',
    trangThai: '',
    minGia: '',
    maxGia: ''
  }
  fetchVariants()
}

// Đổi trạng thái biến thể
const toggleStatus = async (item) => {
  const newStatus = item.trangThai === 1 ? 0 : 1
  try {
    await api.patch(`/api/chi-tiet-san-pham/${item.id}/status`, null, {
      params: { trangThai: newStatus }
    })
    item.trangThai = newStatus
    showToast(`Đã chuyển trạng thái biến thể sang "${newStatus === 1 ? 'Kinh doanh' : 'Ngừng KD'}"`)
  } catch (err) {
    console.error('Lỗi cập nhật trạng thái:', err)
    showToast('Lỗi cập nhật trạng thái!', 'error')
  }
}

// Mở modal thêm
const openCreateModal = () => {
  isEdit.value = false
  editId.value = null
  variantForm.value = {
    idSanPham: selectedProductId.value,
    idKichCo: null,
    idMauSac: null,
    maChiTietSanPham: '',
    soLuong: 10,
    giaBan: 200000,
    trangThai: 1
  }
  showModal.value = true
}

// Mở modal sửa
const openEditModal = (item) => {
  isEdit.value = true
  editId.value = item.id
  variantForm.value = {
    idSanPham: item.idSanPham || selectedProductId.value,
    idKichCo: item.idKichCo || null,
    idMauSac: item.idMauSac || null,
    maChiTietSanPham: item.maChiTietSanPham || '',
    soLuong: item.soLuong != null ? item.soLuong : 0,
    giaBan: item.giaBan != null ? item.giaBan : 0,
    trangThai: item.trangThai != null ? item.trangThai : 1
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

// Lưu biến thể
const handleSubmitVariant = async () => {
  if (!variantForm.value.idKichCo) {
    showToast('Vui lòng chọn kích cỡ!', 'error')
    return
  }
  if (!variantForm.value.idMauSac) {
    showToast('Vui lòng chọn màu sắc!', 'error')
    return
  }
  if (variantForm.value.soLuong < 0) {
    showToast('Số lượng phải >= 0!', 'error')
    return
  }
  if (variantForm.value.giaBan < 0) {
    showToast('Giá bán phải >= 0!', 'error')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await api.put(`/api/chi-tiet-san-pham/${editId.value}`, variantForm.value)
      showToast('Cập nhật biến thể thành công!')
    } else {
      await api.post('/api/chi-tiet-san-pham', variantForm.value)
      showToast('Thêm mới biến thể thành công!')
    }
    closeModal()
    fetchVariants()
  } catch (err) {
    console.error('Lỗi lưu biến thể:', err)
    const msg = err.response?.data?.message || 'Có lỗi xảy ra khi lưu biến thể!'
    showToast(msg, 'error')
  } finally {
    submitting.value = false
  }
}

// Xóa biến thể
const confirmDelete = async (item) => {
  if (confirm(`Bạn có chắc chắn muốn xóa biến thể "${item.maChiTietSanPham}" (${item.tenKichCo} - ${item.tenMauSac})?`)) {
    try {
      await api.delete(`/api/chi-tiet-san-pham/${item.id}`)
      showToast('Xóa biến thể thành công!')
      fetchVariants()
    } catch (err) {
      console.error('Lỗi xóa biến thể:', err)
      const msg = err.response?.data?.message || 'Lỗi khi xóa biến thể!'
      showToast(msg, 'error')
    }
  }
}

onMounted(() => {
  fetchAttributes()
  fetchProductList()
})
</script>

<style scoped>
.variant-page-container {
  padding: 1.25rem 1.5rem 2.5rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.page-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-back {
  background: #ffffff;
  border: 1px solid #d5d9df;
  border-radius: 8px;
  padding: 0.5rem 0.9rem;
  font-weight: 700;
  font-size: 0.88rem;
  color: #496883;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-back:hover {
  background-color: #f0f5fa;
  border-color: #496883;
}

.main-page-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: 0.3px;
  margin: 0;
}

/* Toast */
.toast-notification {
  padding: 0.6rem 1.2rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.9rem;
  animation: slideIn 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.toast-notification.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}
.toast-notification.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Product Summary Card */
.product-summary-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.25rem;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.product-picker-group {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  flex: 1;
}

.picker-label {
  font-weight: 700;
  font-size: 0.92rem;
  color: #2e3b40;
  white-space: nowrap;
}

.product-select {
  height: 2.5rem;
  border: 1px solid #d5d9df;
  border-radius: 8px;
  padding: 0 0.9rem;
  font-size: 0.92rem;
  color: #2e3b40;
  background-color: #ffffff;
  min-width: 350px;
  outline: none;
}
.product-select:focus {
  border-color: #496883;
}

.product-info-details {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 1rem;
  padding-top: 0.9rem;
  border-top: 1px dashed #e9e5db;
}

.info-badge {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  color: #4a565c;
}

.info-badge.stat-badge {
  background: #fff8e8;
  border-color: #fae0a5;
  color: #8f6515;
}

/* Variant Card */
.variant-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.5rem 1.75rem;
}

/* Bộ lọc */
.filter-bar {
  margin-bottom: 1.25rem;
}

.filter-inputs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.filter-select {
  height: 2.35rem;
  border: 1px solid #d5d9df;
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.88rem;
  color: #4f5a60;
  background-color: #ffffff;
  outline: none;
}

.filter-input-price {
  height: 2.35rem;
  width: 130px;
  border: 1px solid #d5d9df;
  border-radius: 8px;
  padding: 0 0.8rem;
  font-size: 0.88rem;
  outline: none;
}
.filter-input-price:focus, .filter-select:focus {
  border-color: #496883;
}

/* Nút */
.btn {
  height: 2.35rem;
  padding: 0 1.15rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border: none;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-search {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-search:hover {
  background-color: #385269;
}

.btn-refresh {
  background-color: #d2a764;
  color: #ffffff;
}
.btn-refresh:hover {
  background-color: #be9453;
}

.btn-add {
  background-color: #edd9b8;
  color: #4a3e2e;
}
.btn-add:hover:not(:disabled) {
  background-color: #e4cda7;
}
.btn-add:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Bảng */
.table-responsive {
  overflow-x: auto;
}

.variant-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem;
}

.variant-table th {
  background-color: #f8fafc;
  color: #6c787f;
  font-weight: 700;
  padding: 0.9rem 1rem;
  text-align: left;
  border-bottom: 2px solid #edf1f5;
  font-size: 0.9rem;
  white-space: nowrap;
}

.variant-table td {
  padding: 0.9rem 1rem;
  border-bottom: 1px solid #f1f4f7;
  vertical-align: middle;
  color: #4a565c;
}

.variant-table tr:hover td {
  background-color: #faf9f5;
}

.empty-state {
  text-align: center;
  padding: 2.5rem !important;
  color: #8c9ba5;
  font-size: 0.95rem;
}

.loading-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid #ccc;
  border-top-color: #496883;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  vertical-align: middle;
  margin-right: 6px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.stt-cell {
  font-weight: 800;
  color: #e65228;
  font-size: 1rem;
}

.code-cell {
  font-size: 0.88rem;
  color: #496883;
}

.product-name-cell {
  font-weight: 600;
  color: #2e3b40;
}

.size-badge {
  display: inline-block;
  padding: 4px 10px;
  background: #f1f4f8;
  border: 1px solid #d5d9df;
  border-radius: 6px;
  font-weight: 800;
  color: #334155;
  font-size: 0.88rem;
}

.color-badge-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.color-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.2);
  display: inline-block;
}

.color-text {
  font-weight: 600;
  font-size: 0.88rem;
}

.price-cell {
  color: #1f272b;
  font-size: 0.95rem;
}

.stock-pill {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.85rem;
}
.stock-pill.stock-ok {
  background-color: #e8f5e9;
  color: #2e7d32;
}
.stock-pill.stock-out {
  background-color: #ffebee;
  color: #c62828;
}

.date-cell {
  color: #6f7c82;
  font-size: 0.88rem;
}

.status-toggle-btn {
  display: inline-block;
  padding: 0.35rem 0.8rem;
  font-weight: 700;
  border-radius: 6px;
  font-size: 0.82rem;
  white-space: nowrap;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-toggle-btn.status-active {
  background-color: #edd9b8;
  color: #453a29;
  border-color: #d2a764;
}
.status-toggle-btn.status-active:hover {
  background-color: #e2cba6;
}

.status-toggle-btn.status-inactive {
  background-color: #f1f1f1;
  color: #888888;
  border-color: #cccccc;
}
.status-toggle-btn.status-inactive:hover {
  background-color: #e2e2e2;
}

/* Nút hành động */
.action-buttons-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
}

.btn-action {
  width: 32px;
  height: 32px;
  border: 1px solid #e9e5db;
  background-color: #ffffff;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-size: 0.88rem;
}

.btn-edit {
  color: #d2a764;
}
.btn-edit:hover {
  background-color: #fff9ee;
  border-color: #d2a764;
}

.btn-delete {
  color: #d9534f;
}
.btn-delete:hover {
  background-color: #fdf2f2;
  border-color: #d9534f;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
  animation: fadeIn 0.2s ease;
}

.modal-dialog {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #f0ede6;
}

.modal-title {
  font-size: 1.15rem;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #888;
  cursor: pointer;
}

.modal-form {
  padding: 1.5rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #3d4a50;
}

.form-label .required {
  color: #d9534f;
}

.form-control {
  height: 2.45rem;
  border: 1px solid #d5d9df;
  border-radius: 6px;
  padding: 0 0.8rem;
  font-size: 0.9rem;
  outline: none;
  transition: all 0.2s;
  background-color: #ffffff;
}

.form-control:focus {
  border-color: #496883;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #f0ede6;
}

.btn-secondary {
  background-color: #f1f4f8;
  color: #556877;
}

.btn-primary {
  background-color: #496883;
  color: #ffffff;
}
.btn-primary:hover:not(:disabled) {
  background-color: #385269;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 800px) {
  .summary-top {
    flex-direction: column;
    align-items: stretch;
  }
  .product-picker-group {
    flex-direction: column;
    align-items: stretch;
  }
  .product-select {
    min-width: 100%;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>