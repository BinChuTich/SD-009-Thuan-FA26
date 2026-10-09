<template>
  <div class="customer-page-wrapper">
    <!-- Khung Bộ lọc -->
    <div class="content-card filter-card">
      <div class="card-header-filter">
        <h3 class="filter-title">Bộ lọc tìm kiếm</h3>
      </div>

      <div class="filter-inputs-grid">
        <div class="form-field search-field">
          <div class="input-inner">
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="Tìm theo mã, họ tên, email, SĐT..."
              @input="applyFilters"
            />
          </div>
        </div>

        <div class="form-field">
          <select v-model="filters.status" @change="applyFilters">
            <option value="">Tất cả trạng thái</option>
            <option value="1">Hoạt động</option>
            <option value="0">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          Đặt lại bộ lọc
        </button>
        <button class="btn btn-export" :disabled="exporting" @click="exportExcel">
          {{ exporting ? 'Đang xuất...' : 'Xuất Excel' }}
        </button>
        <button class="btn btn-primary" @click="goToCreate">
          + Thêm khách hàng mới
        </button>
      </div>
    </div>

    <!-- Khung Danh sách khách hàng -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <div class="table-title-wrap">
          <h3 class="table-title">Danh sách khách hàng</h3>
        </div>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
            <tr>
              <th style="width: 50px; text-align: center;">STT</th>
              <th style="width: 105px;">MÃ KH</th>
              <th style="width: 165px;">HỌ TÊN</th>
              <th style="width: 140px;">TÀI KHOẢN</th>
              <th style="width: 120px;">SĐT</th>
              <th>ĐỊA CHỈ</th>
              <th style="width: 95px; text-align: center;">GIỚI TÍNH</th>
              <th style="width: 135px; text-align: center;">TRẠNG THÁI</th>
              <th style="width: 120px; text-align: center;">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="9" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Đang tải dữ liệu khách hàng...
              </td>
            </tr>
            <tr v-else-if="!items.length">
              <td colspan="9" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Không tìm thấy khách hàng nào phù hợp.
              </td>
            </tr>
            <tr v-for="(item, index) in items" v-else :key="item.id">
              <td style="text-align: center;" class="text-muted">
                {{ page * size + index + 1 }}
              </td>
              <td class="font-bold text-code">{{ item.maKhachHang }}</td>
              <td class="font-medium text-dark">{{ item.tenKhachHang }}</td>
              <td class="text-account font-medium">{{ item.taiKhoan || item.maKhachHang?.toLowerCase() || '-' }}</td>
              <td class="text-dark">{{ item.soDienThoai || '-' }}</td>
              <td class="text-address">{{ formatAddress(item) || '-' }}</td>
              <td style="text-align: center;">{{ genderText(item.gioiTinh) }}</td>
              <td style="text-align: center;">
                <span
                  class="badge-status"
                  :class="item.trangThai === 1 ? 'status-active' : 'status-inactive'"
                >
                  {{ item.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
                </span>
              </td>
              <td style="text-align: center;">
                <div class="action-buttons">
                  <!-- Switch Bật / Tắt trạng thái -->
                  <label
                    class="switch-toggle"
                    :title="item.trangThai === 1 ? 'Đang hoạt động (nhấn để tắt)' : 'Đang ngừng hoạt động (nhấn để bật)'"
                  >
                    <input
                      type="checkbox"
                      :checked="item.trangThai === 1"
                      @click.prevent="toggleStatus(item)"
                    />
                    <span class="slider-round"></span>
                  </label>

                  <!-- Nút Chỉnh sửa thông tin -->
                  <button
                    class="btn-circle-action btn-edit"
                    title="Chỉnh sửa khách hàng"
                    @click="goToEdit(item.id)"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>

                  <!-- Nút Quản lý nhiều địa chỉ & Đặt địa chỉ mặc định -->
                  <button
                    class="btn-circle-action btn-address"
                    title="Quản lý địa chỉ giao hàng"
                    @click="openAddressModal(item)"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                      <circle cx="12" cy="10" r="3"></circle>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination-row" v-if="totalElements > 0">
        <span class="pagination-info">
          Hiển thị {{ items.length ? (page * size + 1) : 0 }} - {{ page * size + items.length }} / {{ totalElements }} khách hàng
        </span>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
          <button
            v-for="n in pageNumbers"
            :key="n"
            class="page-btn"
            :class="{ active: n === page }"
            @click="goPage(n)"
          >
            {{ n + 1 }}
          </button>
          <button class="page-btn" :disabled="page + 1 >= totalPages" @click="goPage(page + 1)">›</button>
        </div>
      </div>
    </div>

    <!-- MODAL QUẢN LÝ ĐỊA CHỈ KHÁCH HÀNG -->
    <Teleport to="body">
      <div v-if="addressModalVisible" class="modal-backdrop" @click.self="closeAddressModal">
        <div class="modal-card animate-pop">
          <div class="modal-header">
            <h3 class="modal-title">
              Quản lý địa chỉ: {{ currentCustomer?.tenKhachHang }} ({{ currentCustomer?.maKhachHang }})
            </h3>
            <button class="modal-close-btn" @click="closeAddressModal">✕</button>
          </div>

          <div class="modal-body">
            <!-- Thanh tác vụ trên: nút Thêm địa chỉ mới -->
            <div class="address-actions-bar">
              <span class="address-count-text">
                Tổng cộng: {{ customerAddresses.length }} địa chỉ
              </span>
              <button
                v-if="!showAddressForm"
                class="btn btn-sm btn-primary"
                @click="openAddAddressForm"
              >
                + Thêm địa chỉ mới
              </button>
            </div>

            <!-- FORM THÊM / SỬA ĐỊA CHỈ (sử dụng API hành chính mới) -->
            <div v-if="showAddressForm" class="address-form-box">
              <h4 class="form-box-title">
                {{ editingAddressId ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}
              </h4>
              <div class="address-form-grid">
                <div class="addr-field">
                  <label>Tỉnh / Thành phố <span class="required">*</span></label>
                  <select v-model="addrProvinceCode" @change="onAddrProvinceChange">
                    <option value="">-- Chọn Tỉnh / Thành phố --</option>
                    <option v-for="p in provincesList" :key="p.code" :value="p.code">
                      {{ p.name }}
                    </option>
                  </select>
                </div>

                <div class="addr-field">
                  <label>Phường / Xã <span class="required">*</span></label>
                  <select
                    v-model="addrWardCode"
                    :disabled="!addrProvinceCode"
                    @change="onAddrWardChange"
                  >
                    <option value="">-- Chọn Phường / Xã --</option>
                    <option v-for="w in addrWardsList" :key="w.code" :value="w.code">
                      {{ w.name }}
                    </option>
                  </select>
                </div>

                <div class="addr-field full-width">
                  <label>Địa chỉ cụ thể <span class="required">*</span></label>
                  <input
                    v-model="addrForm.diaChiCuThe"
                    type="text"
                    placeholder="Số nhà, ngõ, tên đường..."
                  />
                </div>

                <div class="addr-field full-width">
                  <label class="checkbox-label">
                    <input type="checkbox" v-model="addrForm.macDinh" />
                    <span>Đặt làm địa chỉ mặc định</span>
                  </label>
                </div>
              </div>

              <div class="addr-form-actions">
                <button class="btn btn-sm btn-secondary" @click="cancelAddressForm">
                  Hủy
                </button>
                <button class="btn btn-sm btn-primary" :disabled="savingAddress" @click="saveAddress">
                  {{ savingAddress ? 'Đang lưu...' : 'Lưu địa chỉ' }}
                </button>
              </div>
            </div>

            <!-- DANH SÁCH ĐỊA CHỈ HIỆN CÓ -->
            <div class="address-list-container">
              <div v-if="loadingAddresses" class="addr-loading">
                Đang tải danh sách địa chỉ...
              </div>
              <div v-else-if="!customerAddresses.length" class="addr-empty">
                Khách hàng chưa có địa chỉ nào. Nhấn "+ Thêm địa chỉ mới" để tạo.
              </div>
              <div
                v-for="addr in customerAddresses"
                :key="addr.id"
                class="address-item-card"
                :class="{ 'is-default': addr.macDinh }"
              >
                <div class="addr-card-main">
                  <div class="addr-card-header">
                    <span v-if="addr.macDinh" class="badge-default-addr">Mặc định</span>
                    <span class="addr-code">{{ addr.maDiaChi }}</span>
                  </div>
                  <div class="addr-card-detail">
                    <strong>{{ addr.diaChiCuThe }}</strong>, {{ [addr.phuong, addr.huyen, addr.thanhPho].filter(Boolean).join(', ') }}
                  </div>
                </div>

                <div class="addr-card-actions">
                  <button
                    v-if="!addr.macDinh"
                    class="btn-text-action btn-set-default"
                    @click="setAsDefault(addr.id)"
                  >
                    Đặt làm mặc định
                  </button>
                  <button
                    class="btn-text-action btn-edit-addr"
                    @click="editAddress(addr)"
                  >
                    Sửa
                  </button>
                  <button
                    v-if="!addr.macDinh"
                    class="btn-text-action btn-del-addr"
                    @click="deleteAddress(addr.id)"
                  >
                    Xóa
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'
import { getProvinces, getWardsByProvince } from '@/services/provincesApi.js'

const router = useRouter()
const filters = ref({ keyword: '', status: '' })
const items = ref([])
const page = ref(0)
const size = 5
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const exporting = ref(false)

// Address Modal State
const addressModalVisible = ref(false)
const currentCustomer = ref(null)
const customerAddresses = ref([])
const loadingAddresses = ref(false)
const showAddressForm = ref(false)
const editingAddressId = ref(null)
const savingAddress = ref(false)

// Provinces for Address Form (2 cấp)
const provincesList = ref([])
const addrWardsList = ref([])
const addrProvinceCode = ref('')
const addrWardCode = ref('')

const addrForm = reactive({
  thanhPho: '',
  huyen: '',
  phuong: '',
  diaChiCuThe: '',
  macDinh: false
})

const pageNumbers = computed(() => {
  const total = totalPages.value
  if (!total) return []
  const start = Math.max(0, page.value - 2)
  const end = Math.min(total, start + 5)
  return Array.from({ length: end - start }, (_, i) => start + i)
})

function initialsOf(name) {
  if (!name) return 'KH'
  const p = name.trim().split(/\s+/)
  return p.length === 1 ? p[0].slice(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase()
}

function genderText(g) {
  if (g === true) return 'Nam'
  if (g === false) return 'Nữ'
  return '-'
}

function formatAddress(item) {
  return [item.diaChiCuThe, item.phuong, item.huyen, item.thanhPho].filter(Boolean).join(', ')
}

async function loadCustomers() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size,
      keyword: filters.value.keyword || undefined,
      trangThai: filters.value.status === '' ? undefined : filters.value.status
    }
    const res = await api.get('/api/khach-hang', { params })
    items.value = res.data.content || []
    totalElements.value = res.data.totalElements || 0
    totalPages.value = res.data.totalPages || 0
  } catch (e) {
    showAlert({
      title: 'Lỗi tải dữ liệu',
      message: e?.response?.data?.message || 'Không thể kết nối đến máy chủ backend.',
      type: 'error'
    })
  } finally {
    loading.value = false
  }
}

let filterTimer
function applyFilters() {
  clearTimeout(filterTimer)
  filterTimer = setTimeout(() => {
    page.value = 0
    loadCustomers()
  }, 250)
}

function resetFilters() {
  filters.value.keyword = ''
  filters.value.status = ''
  page.value = 0
  loadCustomers()
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadCustomers()
}

function goToCreate() {
  router.push('/khach-hang/them')
}

function goToEdit(id) {
  router.push(`/khach-hang/${id}`)
}

async function toggleStatus(item) {
  const isLocking = item.trangThai === 1
  const actionText = isLocking ? 'khóa' : 'mở khóa'

  const confirmed = await showConfirm({
    title: isLocking ? 'Xác nhận khóa tài khoản' : 'Xác nhận mở khóa tài khoản',
    message: `Bạn có chắc chắn muốn ${actionText} khách hàng "${item.tenKhachHang}" (${item.maKhachHang}) không?`,
    type: isLocking ? 'warning' : 'question',
    confirmText: isLocking ? 'Khóa' : 'Mở khóa'
  })

  if (!confirmed) {
    // Re-render switch position
    item.trangThai = item.trangThai === 1 ? 1 : 0
    return
  }

  try {
    await api.patch(`/api/khach-hang/${item.id}/toggle-status`)
    item.trangThai = item.trangThai === 1 ? 0 : 1
    showToast(`${isLocking ? 'Khóa' : 'Mở khóa'} khách hàng thành công!`, 'success')
  } catch (e) {
    showAlert({
      title: 'Thao tác thất bại',
      message: e?.response?.data?.message || 'Không thể thay đổi trạng thái khách hàng.',
      type: 'error'
    })
  }
}

// ADDRESS MANAGEMENT MODAL METHODS
async function openAddressModal(item) {
  currentCustomer.value = item
  addressModalVisible.value = true
  showAddressForm.value = false
  editingAddressId.value = null
  provincesList.value = await getProvinces()
  await loadCustomerAddresses()
}

function closeAddressModal() {
  addressModalVisible.value = false
  currentCustomer.value = null
}

async function loadCustomerAddresses() {
  if (!currentCustomer.value) return
  loadingAddresses.value = true
  try {
    const res = await api.get(`/api/khach-hang/${currentCustomer.value.id}/dia-chi`)
    customerAddresses.value = res.data || []
  } catch (e) {
    console.error('Lỗi tải danh sách địa chỉ:', e)
  } finally {
    loadingAddresses.value = false
  }
}

function openAddAddressForm() {
  editingAddressId.value = null
  addrForm.thanhPho = ''
  addrForm.huyen = ''
  addrForm.phuong = ''
  addrForm.diaChiCuThe = ''
  addrForm.macDinh = customerAddresses.value.length === 0
  addrProvinceCode.value = ''
  addrWardCode.value = ''
  addrWardsList.value = []
  showAddressForm.value = true
}

async function editAddress(addr) {
  editingAddressId.value = addr.id
  addrForm.thanhPho = addr.thanhPho || ''
  addrForm.huyen = addr.huyen || ''
  addrForm.phuong = addr.phuong || ''
  addrForm.diaChiCuThe = addr.diaChiCuThe || ''
  addrForm.macDinh = addr.macDinh
  showAddressForm.value = true

  // Match province
  const matchP = provincesList.value.find(p => p.name === addr.thanhPho)
  if (matchP) {
    addrProvinceCode.value = matchP.code
    addrWardsList.value = await getWardsByProvince(matchP.code)

    const matchW = addrWardsList.value.find(w => w.name === addr.phuong)
    if (matchW) {
      addrWardCode.value = matchW.code
    }
  }
}

function cancelAddressForm() {
  showAddressForm.value = false
  editingAddressId.value = null
}

async function onAddrProvinceChange() {
  const p = provincesList.value.find(item => item.code === addrProvinceCode.value)
  addrForm.thanhPho = p ? p.name : ''
  addrWardCode.value = ''
  addrForm.huyen = ''
  addrForm.phuong = ''
  addrWardsList.value = []
  if (addrProvinceCode.value) {
    addrWardsList.value = await getWardsByProvince(addrProvinceCode.value)
  }
}

function onAddrWardChange() {
  const w = addrWardsList.value.find(item => item.code === addrWardCode.value)
  addrForm.phuong = w ? w.name : ''
}

async function saveAddress() {
  if (!addrForm.thanhPho || !addrForm.thanhPho.trim()) {
    return showAlert({ title: 'Thiếu thông tin', message: 'Vui lòng chọn Tỉnh / Thành phố!', type: 'warning' })
  }
  if (!addrForm.phuong || !addrForm.phuong.trim()) {
    return showAlert({ title: 'Thiếu thông tin', message: 'Vui lòng chọn Phường / Xã!', type: 'warning' })
  }
  if (!addrForm.diaChiCuThe || !addrForm.diaChiCuThe.trim()) {
    return showAlert({ title: 'Thiếu thông tin', message: 'Vui lòng nhập địa chỉ cụ thể!', type: 'warning' })
  }

  savingAddress.value = true
  try {
    const payload = {
      thanhPho: addrForm.thanhPho.trim(),
      huyen: addrForm.huyen ? addrForm.huyen.trim() : '',
      phuong: addrForm.phuong.trim(),
      diaChiCuThe: addrForm.diaChiCuThe.trim(),
      macDinh: addrForm.macDinh
    }

    if (editingAddressId.value) {
      await api.put(`/api/khach-hang/${currentCustomer.value.id}/dia-chi/${editingAddressId.value}`, payload)
      showToast('Cập nhật địa chỉ thành công!', 'success')
    } else {
      await api.post(`/api/khach-hang/${currentCustomer.value.id}/dia-chi`, payload)
      showToast('Thêm địa chỉ thành công!', 'success')
    }

    showAddressForm.value = false
    await loadCustomerAddresses()
    await loadCustomers() // cập nhật lại địa chỉ hiển thị trên bảng
  } catch (e) {
    showAlert({
      title: 'Lỗi lưu địa chỉ',
      message: e?.response?.data?.message || 'Không thể lưu địa chỉ khách hàng.',
      type: 'error'
    })
  } finally {
    savingAddress.value = false
  }
}

async function setAsDefault(addressId) {
  try {
    await api.put(`/api/khach-hang/${currentCustomer.value.id}/dia-chi/${addressId}/mac-dinh`)
    showToast('Đã đặt làm địa chỉ mặc định!', 'success')
    await loadCustomerAddresses()
    await loadCustomers()
  } catch (e) {
    showAlert({
      title: 'Lỗi',
      message: e?.response?.data?.message || 'Không thể đặt địa chỉ mặc định.',
      type: 'error'
    })
  }
}

async function deleteAddress(addressId) {
  const confirmed = await showConfirm({
    title: 'Xác nhận xóa địa chỉ',
    message: 'Bạn có chắc chắn muốn xóa địa chỉ này khỏi danh sách không?',
    type: 'danger',
    confirmText: 'Xóa địa chỉ'
  })
  if (!confirmed) return

  try {
    await api.delete(`/api/khach-hang/${currentCustomer.value.id}/dia-chi/${addressId}`)
    showToast('Đã xóa địa chỉ thành công!', 'success')
    await loadCustomerAddresses()
    await loadCustomers()
  } catch (e) {
    showAlert({
      title: 'Không thể xóa',
      message: e?.response?.data?.message || 'Có lỗi xảy ra khi xóa địa chỉ.',
      type: 'error'
    })
  }
}

async function exportExcel() {
  exporting.value = true
  try {
    const params = {
      keyword: filters.value.keyword || undefined,
      trangThai: filters.value.status === '' ? undefined : filters.value.status
    }
    const res = await api.get('/api/khach-hang/export-excel', { params, responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'danh-sach-khach-hang.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    showToast('Xuất danh sách khách hàng ra Excel thành công!', 'success')
  } catch (e) {
    showAlert({
      title: 'Lỗi xuất Excel',
      message: e?.response?.data?.message || 'Không thể tạo file Excel.',
      type: 'error'
    })
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadCustomers()
})
</script>

<style scoped>
.customer-page-wrapper {
  padding: 0.5rem 2.2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.content-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 4px rgba(65, 60, 50, 0.03);
  padding: 1.5rem 1.8rem;
  margin-bottom: 1.4rem;
}

.card-header-filter {
  margin-bottom: 1.2rem;
}

.filter-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: #43545c;
}

.filter-inputs-grid {
  display: grid;
  grid-template-columns: 2.5fr 1fr;
  gap: 1rem;
  margin-bottom: 1.2rem;
  align-items: center;
}

.form-field {
  display: flex;
  flex-direction: column;
}

.input-inner {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 2.7rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.95rem;
  font-size: 0.95rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-field input:focus,
.form-field select:focus {
  border-color: #496883;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
  align-items: center;
}

.btn {
  height: 2.6rem;
  padding: 0 1.25rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  transition: all 0.2s;
  border: none;
}

.btn-sm {
  height: 2.2rem;
  padding: 0 0.9rem;
  font-size: 0.85rem;
}

.btn-reset {
  background-color: #f4efe6;
  color: #6d6352;
  border: 1px solid #e2dacd;
}
.btn-reset:hover {
  background-color: #ebe3d6;
}

.btn-export {
  background-color: #eaf1f4;
  color: #3f5d75;
  border: 1px solid #d0dfe6;
}
.btn-export:hover {
  background-color: #dde7ec;
}

.btn-primary {
  background-color: #3b596a;
  color: #ffffff;
  border: 1px solid #324b5a;
  border-radius: 9999px;
  padding: 0.6rem 1.4rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
  transition: all 0.2s ease;
}
.btn-primary:hover {
  background-color: #2c4350;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
  transform: translateY(-1px);
}

.btn-secondary {
  background-color: #f0ece3;
  color: #555e62;
  border: 1px solid #dbd4c5;
}

.table-header-row {
  margin-bottom: 1.2rem;
}

.table-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #384952;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 0.92rem;
}

.custom-table th {
  background-color: #faf8f5;
  color: #4a5568;
  font-weight: 700;
  text-transform: uppercase;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  padding: 0.9rem 0.95rem;
  border-bottom: 2px solid #e9e5db;
  text-align: left;
  white-space: nowrap;
}

.custom-table td {
  padding: 0.85rem 0.9rem;
  border-bottom: 1px solid #f0ede6;
  color: #3d4a50;
  vertical-align: middle;
}

.avatar-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-img {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e0dad0;
}

.avatar-placeholder {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #f0e6dc;
  color: #8c6a46;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.8rem;
  border: 1px solid #e2d3c5;
}

.text-code { color: #8c6a46; }
.text-account { color: #52443a; }
.text-blue { color: #8c6a46; }
.text-dark { color: #222b30; }
.text-muted { color: #88959c; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 600; }

.badge-status {
  display: inline-block;
  padding: 0.32rem 0.85rem;
  border-radius: 9999px;
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.status-active {
  background-color: #eaf5ed;
  color: #247541;
  border: 1px solid #bce1c7;
}
.status-inactive {
  background-color: #f7edeb;
  color: #b34a3b;
  border: 1px solid #ecc9c3;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
}

/* SWITCH TOGGLE */
.switch-toggle {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
  cursor: pointer;
  margin: 0;
}

.switch-toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider-round {
  position: absolute;
  inset: 0;
  background-color: #d1d5db;
  border-radius: 22px;
  transition: all 0.25s ease;
}

.slider-round::before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: all 0.25s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch-toggle input:checked + .slider-round {
  background-color: #b38e6e;
}

.switch-toggle input:checked + .slider-round::before {
  transform: translateX(16px);
}

.btn-circle-action {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #f7f5ee;
}

.btn-edit {
  color: #8c7152;
  border-color: #dfd5c8;
}
.btn-edit:hover {
  background-color: #8c7152;
  border-color: #8c7152;
  color: #ffffff;
}

.btn-address {
  color: #a67c52;
  border-color: #dfd2c4;
  background-color: #faf5ef;
}
.btn-address:hover {
  background-color: #b38e6e;
  border-color: #b38e6e;
  color: #ffffff;
}

.pagination-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.4rem;
  padding-top: 1rem;
  border-top: 1px solid #f0ede6;
}

.pagination-info {
  font-size: 0.88rem;
  color: #7b888f;
}

.pagination-buttons {
  display: flex;
  gap: 0.35rem;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 0.5rem;
  border: 1px solid #e4dfd5;
  background-color: #ffffff;
  color: #496883;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}
.page-btn.active {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* MODAL STYLES */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(30, 41, 48, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 1.5rem;
}

.modal-card {
  background-color: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 680px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.2rem 1.6rem;
  border-bottom: 1px solid #eee9e0;
  background-color: #faf8f4;
}

.modal-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #496883;
}

.modal-close-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #8c979d;
  cursor: pointer;
  padding: 0.2rem 0.5rem;
}
.modal-close-btn:hover {
  color: #c94a4a;
}

.modal-body {
  padding: 1.5rem 1.6rem;
  overflow-y: auto;
}

.address-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.2rem;
}

.address-count-text {
  font-size: 0.9rem;
  color: #647b8c;
  font-weight: 600;
}

.address-form-box {
  background-color: #faf8f5;
  border: 1px solid #e8e2d5;
  border-radius: 10px;
  padding: 1.25rem 1.4rem;
  margin-bottom: 1.4rem;
}

.form-box-title {
  margin: 0 0 1rem;
  font-size: 1rem;
  font-weight: 700;
  color: #384952;
}

.address-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.addr-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.addr-field.full-width {
  grid-column: span 2;
}

.addr-field label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #414d55;
}

.addr-field input,
.addr-field select {
  height: 2.5rem;
  border: 1px solid #d9d4c7;
  border-radius: 6px;
  padding: 0 0.8rem;
  font-size: 0.9rem;
  background-color: #ffffff;
  outline: none;
}
.addr-field input:focus,
.addr-field select:focus {
  border-color: #496883;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.88rem;
  cursor: pointer;
  font-weight: 600;
  color: #384952;
}

.checkbox-label input[type='checkbox'] {
  width: 1rem;
  height: 1rem;
  accent-color: #496883;
}

.addr-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
  margin-top: 1rem;
}

.address-list-container {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.address-item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.2rem;
  border: 1px solid #ebe5d8;
  border-radius: 10px;
  background-color: #ffffff;
  transition: all 0.2s;
}

.address-item-card.is-default {
  border-color: #496883;
  background-color: #f7fafc;
}

.addr-card-main {
  flex: 1;
}

.addr-card-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.35rem;
}

.badge-default-addr {
  background-color: #eaf1f5;
  color: #496883;
  border: 1px solid #496883;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
}

.addr-code {
  font-size: 0.82rem;
  color: #7b8b93;
}

.addr-card-detail {
  font-size: 0.92rem;
  color: #333d42;
  line-height: 1.4;
}

.addr-card-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-text-action {
  background: none;
  border: none;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.3rem 0.5rem;
  border-radius: 4px;
  transition: background 0.15s;
}

.btn-set-default {
  color: #496883;
}
.btn-set-default:hover {
  background-color: #eaf1f4;
}

.btn-edit-addr {
  color: #8c713b;
}
.btn-edit-addr:hover {
  background-color: #fcf4e6;
}

.btn-del-addr {
  color: #c94a4a;
}
.btn-del-addr:hover {
  background-color: #fde8e8;
}

.addr-loading,
.addr-empty {
  text-align: center;
  padding: 2rem;
  color: #88969c;
  font-size: 0.92rem;
}
</style>
