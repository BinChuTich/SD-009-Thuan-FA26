<template>
  <div class="discount-page-container">
    <!-- 1. Thanh tiêu đề & Nút thao tác nhanh -->
    <div class="top-title-card">
      <div class="title-left">
        <div class="breadcrumb">Quản lý giảm giá / {{ activeMainTab === 'dot' ? 'Đợt giảm giá' : 'Phiếu giảm giá' }}</div>
        <h2 class="page-title">Quản Lý Khuyến Mãi & Giảm Giá</h2>
      </div>
      <div class="title-actions">
        <button class="btn btn-export">
          <span>📤</span> Xuất báo cáo
        </button>
        <button class="btn btn-primary" @click="openCreateModal">
          <span>＋</span> {{ activeMainTab === 'dot' ? 'Tạo Đợt Giảm Giá Mới' : 'Tạo Voucher Mới' }}
        </button>
      </div>
    </div>

    <!-- 2. Thẻ thống kê tổng quan -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrap green-bg">⚡</div>
        <div class="stat-info">
          <span>Đang diễn ra</span>
          <strong>04</strong>
          <small class="text-success">Đang kích hoạt tại quầy & online</small>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap gold-bg">⏳</div>
        <div class="stat-info">
          <span>Sắp diễn ra</span>
          <strong>02</strong>
          <small class="text-gold">Kế hoạch khuyến mãi tuần tới</small>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap gray-bg">⏹</div>
        <div class="stat-info">
          <span>Đã kết thúc / Hết hạn</span>
          <strong>18</strong>
          <small class="text-muted">Lưu trữ lịch sử giảm giá</small>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon-wrap blue-bg">🎟️</div>
        <div class="stat-info">
          <span>Tổng lượt đã áp dụng</span>
          <strong>1.248</strong>
          <small class="text-blue">↑ 15% so với tháng trước</small>
        </div>
      </div>
    </div>

    <!-- 3. Khung Bộ Lọc & Tìm Kiếm -->
    <div class="custom-card filter-card">
      <div class="card-head-title">
        <span class="icon-head">🌪️</span>
        <h3>Bộ Lọc Giảm Giá</h3>
      </div>

      <div class="filter-grid">
        <!-- Tìm kiếm từ khóa -->
        <div class="form-group search-col">
          <label>Tìm kiếm chương trình</label>
          <div class="input-search">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                v-model="filters.keyword"
                placeholder="Nhập tên đợt giảm giá, mã voucher..."
            />
          </div>
        </div>

        <!-- Hình thức giảm -->
        <div class="form-group">
          <label>Hình thức giảm</label>
          <select v-model="filters.discountType">
            <option value="">Tất cả hình thức</option>
            <option value="percent">Giảm theo phần trăm (%)</option>
            <option value="amount">Giảm tiền trực tiếp (VNĐ)</option>
          </select>
        </div>

        <!-- Trạng thái -->
        <div class="form-group">
          <label>Trạng thái</label>
          <select v-model="filters.status">
            <option value="">Tất cả trạng thái</option>
            <option value="active">Đang diễn ra</option>
            <option value="upcoming">Sắp diễn ra</option>
            <option value="expired">Đã kết thúc</option>
          </select>
        </div>

        <!-- Khoảng ngày áp dụng -->
        <div class="form-group">
          <label>Thời gian áp dụng</label>
          <div class="date-input-wrap">
            <input type="text" placeholder="Từ ngày - Đến ngày" value="01/10/2026 - 31/10/2026" />
            <span class="date-icon">📅</span>
          </div>
        </div>
      </div>

      <!-- Hàng nút lọc căn phải -->
      <div class="filter-actions-row">
        <button class="btn btn-reset" @click="resetFilters">Làm Mới Bộ Lọc</button>
        <button class="btn btn-primary">Tìm Kiếm</button>
      </div>
    </div>

    <!-- 4. Khung Bảng Danh Sách -->
    <div class="custom-card list-card">
      <!-- Tabs chuyển đổi giữa Đợt Giảm Giá & Phiếu Giảm Giá -->
      <div class="table-top-tabs">
        <div class="tab-button-group">
          <button
              class="tab-btn"
              :class="{ active: activeMainTab === 'dot' }"
              @click="activeMainTab = 'dot'"
          >
            🏷️ Đợt giảm giá ({{ discountCampaigns.length }})
          </button>
          <button
              class="tab-btn"
              :class="{ active: activeMainTab === 'phieu' }"
              @click="activeMainTab = 'phieu'"
          >
            🎟️️ Phiếu giảm giá / Voucher ({{ vouchers.length }})
          </button>
        </div>

        <span class="total-badge">
          Tổng cộng: <b>{{ currentDataList.length }}</b> bản ghi
        </span>
      </div>

      <!-- Bảng dữ liệu -->
      <div class="table-responsive">
        <table class="discount-table">
          <thead>
          <tr>
            <th style="width: 40px; text-align: center">STT</th>
            <th style="width: 120px">Mã giảm giá</th>
            <th>Tên chương trình</th>
            <th>Mức giảm</th>
            <th>Thời gian áp dụng</th>
            <th>Điều kiện tối thiểu</th>
            <th style="text-align: center">Đã dùng / Tổng</th>
            <th style="text-align: center">Trạng thái</th>
            <th style="text-align: center; width: 80px">Kích hoạt</th>
            <th style="text-align: center; width: 100px">Thao tác</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(item, idx) in currentDataList" :key="item.id">
            <td style="text-align: center">{{ idx + 1 }}</td>

            <!-- Mã -->
            <td>
              <b class="code-badge">{{ item.code }}</b>
            </td>

            <!-- Tên chương trình -->
            <td>
              <div class="program-title-wrap">
                <strong class="program-title">{{ item.name }}</strong>
                <small class="text-muted">{{ item.scope }}</small>
              </div>
            </td>

            <!-- Mức giảm -->
            <td>
                <span class="discount-value-tag" :class="item.type === 'percent' ? 'tag-percent' : 'tag-cash'">
                  {{ item.discountDisplay }}
                </span>
            </td>

            <!-- Thời gian áp dụng -->
            <td>
              <div class="date-time-box">
                <span>🟢 {{ item.startDate }}</span>
                <span class="text-muted">🔴 {{ item.endDate }}</span>
              </div>
            </td>

            <!-- Điều kiện -->
            <td>
              <div class="condition-box">
                <span>Đơn từ: <b>{{ item.minOrder }}</b></span>
                <small v-if="item.maxDiscount" class="text-muted">Giảm tối đa: {{ item.maxDiscount }}</small>
              </div>
            </td>

            <!-- Số lượng đã dùng -->
            <td style="text-align: center">
              <div class="usage-progress-wrap">
                <span class="usage-text">{{ item.used }} / {{ item.quantity }}</span>
                <div class="progress-bar-bg">
                  <div
                      class="progress-bar-fill"
                      :style="{ width: ((item.used / item.quantity) * 100) + '%' }"
                  ></div>
                </div>
              </div>
            </td>

            <!-- Badge Trạng thái -->
            <td style="text-align: center">
                <span class="status-pill" :class="item.statusClass">
                  {{ item.statusText }}
                </span>
            </td>

            <!-- Switch kích hoạt On / Off -->
            <td style="text-align: center">
              <label class="switch">
                <input type="checkbox" v-model="item.isEnabled" />
                <span class="slider"></span>
              </label>
            </td>

            <!-- Nút thao tác -->
            <td style="text-align: center">
              <div class="action-btn-group">
                <button class="act-btn" title="Chỉnh sửa" @click="editItem(item)">✏️</button>
                <button class="act-btn btn-del" title="Xóa" @click="deleteItem(item.id)">🗑️</button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Phân trang -->
      <div class="pagination-bar">
        <div class="page-info">
          Hiển thị 1 - {{ currentDataList.length }} của danh sách khuyến mãi
        </div>
        <div class="page-controls">
          <button class="pg-btn" disabled>‹</button>
          <button class="pg-btn active">1</button>
          <button class="pg-btn">2</button>
          <button class="pg-btn">›</button>
        </div>
      </div>
    </div>

    <!-- 5. Modal Tạo mới / Sửa chương trình giảm giá -->
    <div class="modal-overlay" v-if="showModal" @click.self="showModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ isEditing ? 'Chỉnh Sửa Giảm Giá' : (activeMainTab === 'dot' ? 'Tạo Đợt Giảm Giá Mới' : 'Tạo Mã Giảm Giá Mới') }}</h3>
          <button class="modal-close" @click="showModal = false">✕</button>
        </div>

        <div class="modal-body">
          <div class="form-row-2">
            <div class="form-group">
              <label>Mã chương trình / Voucher *</label>
              <input type="text" v-model="modalForm.code" placeholder="VD: SALEHE2026, FF10" />
            </div>
            <div class="form-group">
              <label>Tên chương trình khuyến mãi *</label>
              <input type="text" v-model="modalForm.name" placeholder="VD: Khuyến mãi chào hè FF T-shirt" />
            </div>
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Hình thức giảm *</label>
              <select v-model="modalForm.type">
                <option value="percent">Giảm theo % (Phần trăm)</option>
                <option value="amount">Giảm tiền trực tiếp (VNĐ)</option>
              </select>
            </div>
            <div class="form-group">
              <label>Mức giảm *</label>
              <input
                  type="number"
                  v-model="modalForm.discountVal"
                  :placeholder="modalForm.type === 'percent' ? 'VD: 15 (nghĩa là 15%)' : 'VD: 50000'"
              />
            </div>
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Giá trị đơn tối thiểu (đ)</label>
              <input type="number" v-model="modalForm.minOrderVal" placeholder="VD: 200000" />
            </div>
            <div class="form-group">
              <label>Giảm tối đa (đ) (nếu giảm %)</label>
              <input type="number" v-model="modalForm.maxDiscountVal" placeholder="VD: 100000" />
            </div>
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Thời gian bắt đầu *</label>
              <input type="datetime-local" v-model="modalForm.startDateTime" />
            </div>
            <div class="form-group">
              <label>Thời gian kết thúc *</label>
              <input type="datetime-local" v-model="modalForm.endDateTime" />
            </div>
          </div>

          <div class="form-row-2">
            <div class="form-group">
              <label>Số lượng phát hành / Giới hạn</label>
              <input type="number" v-model="modalForm.quantity" placeholder="VD: 100" />
            </div>
            <div class="form-group">
              <label>Phạm vi áp dụng</label>
              <select v-model="modalForm.scope">
                <option value="Toàn bộ sản phẩm">Toàn bộ sản phẩm</option>
                <option value="Chỉ áo Polo">Chỉ áo Polo</option>
                <option value="Chỉ áo Oversize">Chỉ áo Oversize</option>
              </select>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-reset" @click="showModal = false">Đóng</button>
          <button class="btn btn-primary" @click="saveDiscount">Xác Nhận Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const activeMainTab = ref('dot') // 'dot' (đợt giảm giá) hoặc 'phieu' (phiếu giảm giá)
const showModal = ref(false)
const isEditing = ref(false)

const filters = ref({
  keyword: '',
  discountType: '',
  status: ''
})

// Dữ liệu mẫu: Đợt giảm giá
const discountCampaigns = ref([
  {
    id: 1,
    code: 'CAMPAIGN-SUMMER',
    name: 'Đợt giảm giá mùa hè cực nhiệt',
    scope: 'Áp dụng cho dòng áo thun Basic & Polo',
    type: 'percent',
    discountDisplay: 'Giảm 20%',
    startDate: '01/10/2026 00:00',
    endDate: '15/10/2026 23:59',
    minOrder: '250.000 ₫',
    maxDiscount: '100.000 ₫',
    used: 142,
    quantity: 300,
    statusText: 'Đang diễn ra',
    statusClass: 'status-active',
    isEnabled: true
  },
  {
    id: 2,
    code: 'POLO-FLASH50K',
    name: 'Flash Sale Áo Polo Premium cuối tuần',
    scope: 'Áp dụng danh mục Áo Polo',
    type: 'amount',
    discountDisplay: 'Giảm 50.000 ₫',
    startDate: '10/10/2026 09:00',
    endDate: '12/10/2026 22:00',
    minOrder: '300.000 ₫',
    maxDiscount: null,
    used: 0,
    quantity: 100,
    statusText: 'Sắp diễn ra',
    statusClass: 'status-upcoming',
    isEnabled: true
  },
  {
    id: 3,
    code: 'MID-YEAR-30',
    name: 'Xả kho giữa năm FF T-shirt',
    scope: 'Áp dụng toàn bộ sản phẩm tồn kho',
    type: 'percent',
    discountDisplay: 'Giảm 30%',
    startDate: '01/09/2026 00:00',
    endDate: '20/09/2026 23:59',
    minOrder: '0 ₫',
    maxDiscount: '150.000 ₫',
    used: 500,
    quantity: 500,
    statusText: 'Đã kết thúc',
    statusClass: 'status-expired',
    isEnabled: false
  }
])

// Dữ liệu mẫu: Phiếu giảm giá (Vouchers)
const vouchers = ref([
  {
    id: 101,
    code: 'FFWELCOME10',
    name: 'Voucher chào mừng khách hàng mới',
    scope: 'Áp dụng cho đơn hàng đầu tiên',
    type: 'percent',
    discountDisplay: 'Giảm 10%',
    startDate: '01/01/2026 00:00',
    endDate: '31/12/2026 23:59',
    minOrder: '150.000 ₫',
    maxDiscount: '50.000 ₫',
    used: 489,
    quantity: 1000,
    statusText: 'Đang diễn ra',
    statusClass: 'status-active',
    isEnabled: true
  },
  {
    id: 102,
    code: 'FREESHIPMAX',
    name: 'Hỗ trợ phí vận chuyển nội thành',
    scope: 'Đơn hàng giao tận nơi',
    type: 'amount',
    discountDisplay: 'Giảm 30.000 ₫',
    startDate: '01/10/2026 00:00',
    endDate: '31/10/2026 23:59',
    minOrder: '400.000 ₫',
    maxDiscount: null,
    used: 117,
    quantity: 200,
    statusText: 'Đang diễn ra',
    statusClass: 'status-active',
    isEnabled: true
  }
])

const currentDataList = computed(() => {
  const source = activeMainTab.value === 'dot' ? discountCampaigns.value : vouchers.value
  return source.filter(item => {
    const matchKw = !filters.value.keyword ||
        item.name.toLowerCase().includes(filters.value.keyword.toLowerCase()) ||
        item.code.toLowerCase().includes(filters.value.keyword.toLowerCase())
    const matchType = !filters.value.discountType || item.type === filters.value.discountType
    return matchKw && matchType
  })
})

const resetFilters = () => {
  filters.value = {
    keyword: '',
    discountType: '',
    status: ''
  }
}

const modalForm = ref({
  code: '',
  name: '',
  type: 'percent',
  discountVal: 15,
  minOrderVal: 200000,
  maxDiscountVal: 50000,
  startDateTime: '',
  endDateTime: '',
  quantity: 100,
  scope: 'Toàn bộ sản phẩm'
})

const openCreateModal = () => {
  isEditing.value = false
  modalForm.value = {
    code: activeMainTab.value === 'dot' ? 'SALE-OCT26' : 'VOUCHER-OCT',
    name: '',
    type: 'percent',
    discountVal: 15,
    minOrderVal: 200000,
    maxDiscountVal: 50000,
    startDateTime: '2026-10-01T00:00',
    endDateTime: '2026-10-31T23:59',
    quantity: 100,
    scope: 'Toàn bộ sản phẩm'
  }
  showModal.value = true
}

const editItem = (item) => {
  isEditing.value = true
  modalForm.value = {
    code: item.code,
    name: item.name,
    type: item.type,
    discountVal: item.type === 'percent' ? 20 : 50000,
    minOrderVal: 250000,
    maxDiscountVal: 100000,
    startDateTime: '2026-10-01T00:00',
    endDateTime: '2026-10-15T23:59',
    quantity: item.quantity,
    scope: item.scope
  }
  showModal.value = true
}

const deleteItem = (id) => {
  if (confirm('Bạn có chắc chắn muốn xóa khuyến mãi này?')) {
    if (activeMainTab.value === 'dot') {
      discountCampaigns.value = discountCampaigns.value.filter(d => d.id !== id)
    } else {
      vouchers.value = vouchers.value.filter(v => v.id !== id)
    }
  }
}

const saveDiscount = () => {
  if (!modalForm.value.code || !modalForm.value.name) {
    alert('Vui lòng điền đầy đủ mã và tên khuyến mãi!')
    return
  }

  const newItem = {
    id: Date.now(),
    code: modalForm.value.code.toUpperCase(),
    name: modalForm.value.name,
    scope: modalForm.value.scope,
    type: modalForm.value.type,
    discountDisplay: modalForm.value.type === 'percent' ? `Giảm ${modalForm.value.discountVal}%` : `Giảm ${Number(modalForm.value.discountVal).toLocaleString('vi-VN')} ₫`,
    startDate: modalForm.value.startDateTime.replace('T', ' '),
    endDate: modalForm.value.endDateTime.replace('T', ' '),
    minOrder: `${Number(modalForm.value.minOrderVal).toLocaleString('vi-VN')} ₫`,
    maxDiscount: modalForm.value.type === 'percent' ? `${Number(modalForm.value.maxDiscountVal).toLocaleString('vi-VN')} ₫` : null,
    used: 0,
    quantity: modalForm.value.quantity,
    statusText: 'Sắp diễn ra',
    statusClass: 'status-upcoming',
    isEnabled: true
  }

  if (activeMainTab.value === 'dot') {
    discountCampaigns.value.unshift(newItem)
  } else {
    vouchers.value.unshift(newItem)
  }

  showModal.value = false
}
</script>

<style scoped>
/* Vùng chứa tổng thể chuẩn tone màu dự án */
.discount-page-container {
  padding: 16px 20px 30px;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
  color: var(--text, #3d4a50);
}

/* 1. Header trên cùng */
.top-title-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 12px 18px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.breadcrumb {
  font-size: 8px;
  color: #8f9798;
  margin-bottom: 2px;
}

.page-title {
  font-size: 13px;
  font-weight: 700;
  margin: 0;
  color: var(--blue, #496883);
}

.title-actions {
  display: flex;
  gap: 8px;
}

/* 2. Thẻ thống kê */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.02);
}

.stat-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 16px;
}

.green-bg { background: #edf6ef; color: #4c8a5a; }
.gold-bg  { background: #fdf5e6; color: #d2a764; }
.gray-bg  { background: #f1f2f3; color: #7f898d; }
.blue-bg  { background: #eaf1f5; color: #496883; }

.stat-info span {
  display: block;
  font-size: 8px;
  color: #8c9597;
}

.stat-info strong {
  display: block;
  font-size: 15px;
  font-weight: 800;
  color: #3b4b53;
  margin: 1px 0;
}

.stat-info small {
  display: block;
  font-size: 7px;
}

.text-success { color: #4c8a5a; }
.text-gold    { color: #b98e47; }
.text-muted   { color: #9aa0a0; }
.text-blue    { color: #496883; }

/* 3. Card dùng chung */
.custom-card {
  background: #ffffff;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
}

.card-head-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.card-head-title h3 {
  font-size: 11px;
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

.icon-head {
  font-size: 13px;
}

/* Bộ lọc */
.filter-grid {
  display: grid;
  grid-template-columns: 1.8fr 1.1fr 1.1fr 1.4fr;
  gap: 12px;
  margin-bottom: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-size: 8px;
  font-weight: 700;
  color: #556268;
}

.form-group input,
.form-group select {
  height: 32px;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 6px;
  padding: 0 10px;
  font-size: 8.5px;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

.input-search {
  position: relative;
  display: flex;
  align-items: center;
}

.input-search input {
  width: 100%;
  padding-left: 28px;
}

.search-icon {
  position: absolute;
  left: 9px;
  font-size: 10px;
  color: #9aa0a0;
  pointer-events: none;
}

.date-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.date-input-wrap input {
  width: 100%;
  padding-right: 28px;
}

.date-icon {
  position: absolute;
  right: 8px;
  font-size: 10px;
  color: #9aa0a0;
  pointer-events: none;
}

.filter-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px dashed #efeae0;
  padding-top: 12px;
}

/* Các loại nút bấm */
.btn {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-primary {
  background-color: var(--blue, #496883);
  color: #ffffff;
}
.btn-primary:hover {
  background-color: #38536b;
}

.btn-reset {
  border: 1px solid #dfd5c2;
  background-color: #fff8eb;
  color: #957b48;
}
.btn-reset:hover {
  background-color: #faeed7;
}

.btn-export {
  background-color: #edf5ef;
  color: #558764;
  border: 1px solid #d2e5d6;
}
.btn-export:hover {
  background-color: #deede1;
}

/* 4. Tabs chuyển đổi bảng */
.table-top-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0eae0;
  padding-bottom: 10px;
  margin-bottom: 12px;
}

.tab-button-group {
  display: flex;
  gap: 6px;
}

.tab-btn {
  border: 1px solid var(--line, #e9e5db);
  background: #fbf9f5;
  color: #647074;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 8.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #eaf1f4;
  color: var(--blue, #496883);
  border-color: #c8d9e3;
  font-weight: 700;
}

.total-badge {
  font-size: 8px;
  color: #8c9597;
}

/* Bảng dữ liệu */
.table-responsive {
  overflow-x: auto;
}

.discount-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 8px;
}

.discount-table th {
  background-color: #faf9f6;
  color: #8f9695;
  font-weight: 700;
  padding: 9px 12px;
  text-align: left;
  border-bottom: 1px solid #efede7;
}

.discount-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f0eb;
  color: #556268;
}

.discount-table tr:hover td {
  background-color: #fcfbf8;
}

.code-badge {
  color: var(--blue, #496883);
  font-family: monospace, sans-serif;
  font-size: 9px;
  letter-spacing: 0.5px;
}

.program-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.program-title {
  color: #3b4c54;
  font-size: 8.5px;
}

.discount-value-tag {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: 700;
  font-size: 8px;
}

.tag-percent {
  background: #fdf5e6;
  color: #b38536;
}

.tag-cash {
  background: #eaf1f5;
  color: #496883;
}

.date-time-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 7.5px;
}

.condition-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* Tiến độ dùng */
.usage-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}

.usage-text {
  font-size: 7.5px;
  font-weight: 600;
  color: #5d6d74;
}

.progress-bar-bg {
  width: 60px;
  height: 5px;
  background-color: #ebe5d8;
  border-radius: 10px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background-color: var(--blue, #496883);
  border-radius: 10px;
}

/* Badge trạng thái */
.status-pill {
  display: inline-block;
  font-size: 7px;
  padding: 2px 7px;
  border-radius: 10px;
  font-weight: 700;
}

.status-active {
  background: #edf6ef;
  color: #4c8a5a;
}

.status-upcoming {
  background: #fef8eb;
  color: #baa04e;
}

.status-expired {
  background: #f0f1f2;
  color: #8c9597;
}

/* Switch on/off */
.switch {
  position: relative;
  display: inline-block;
  width: 28px;
  height: 16px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd3d6;
  border-radius: 16px;
  transition: 0.3s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 12px;
  width: 12px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  border-radius: 50%;
  transition: 0.3s;
}

.switch input:checked + .slider {
  background-color: var(--blue, #496883);
}

.switch input:checked + .slider:before {
  transform: translateX(12px);
}

/* Action buttons */
.action-btn-group {
  display: flex;
  justify-content: center;
  gap: 5px;
}

.act-btn {
  width: 24px;
  height: 24px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 10px;
  display: inline-grid;
  place-items: center;
  transition: all 0.15s;
}

.act-btn:hover {
  background-color: #eaf1f4;
}

.btn-del:hover {
  background-color: #fdeeee;
}

/* Phân trang */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  font-size: 7.5px;
  color: #8e9596;
}

.page-controls {
  display: flex;
  gap: 4px;
}

.pg-btn {
  width: 22px;
  height: 22px;
  border: 1px solid var(--line, #e9e5db);
  background: #ffffff;
  border-radius: 4px;
  font-size: 8px;
  cursor: pointer;
  display: grid;
  place-items: center;
  color: #647074;
}

.pg-btn.active {
  background-color: var(--blue, #496883);
  color: #ffffff;
  border-color: var(--blue, #496883);
  font-weight: 700;
}

.pg-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Modal Popup */
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(30, 40, 48, 0.4);
  backdrop-filter: blur(2px);
  z-index: 999;
  display: grid;
  place-items: center;
  padding: 20px;
}

.modal-card {
  width: 100%;
  max-width: 520px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.modal-header {
  padding: 12px 18px;
  border-bottom: 1px solid #f0eae0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  font-size: 11.5px;
  font-weight: 700;
  margin: 0;
  color: var(--blue, #496883);
}

.modal-close {
  border: none;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
  color: #8c9597;
}

.modal-body {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.modal-footer {
  padding: 12px 18px;
  background-color: #faf9f6;
  border-top: 1px solid #f0eae0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>