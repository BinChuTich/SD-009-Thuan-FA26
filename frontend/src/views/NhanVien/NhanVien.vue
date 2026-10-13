<template>
  <div class="employee-page-wrapper">
    <!-- Breadcrumb -->
    <div class="page-breadcrumb">
      <router-link to="/nhan-vien" class="crumb-parent">Quản lý nhân viên</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Danh sách nhân viên</span>
    </div>

    <!-- Bộ lọc -->
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
          <Select2Combobox
            v-model="filters.role"
            :options="roles"
            value-key="id"
            label-key="tenVaiTro"
            placeholder="Tất cả chức vụ"
            all-label="Tất cả chức vụ"
            search-placeholder="Tìm chức vụ..."
            @change="applyFilters"
          />
        </div>

        <div class="form-field">
          <Select2Combobox
            v-model="filters.status"
            :options="trangThaiOptions"
            value-key="value"
            label-key="label"
            placeholder="Tất cả trạng thái"
            all-label="Tất cả trạng thái"
            search-placeholder="Tìm trạng thái..."
            @change="applyFilters"
          />
        </div>
      </div>

      <div class="filter-actions">
        <button class="btn btn-reset" @click="resetFilters">
          Đặt lại bộ lọc
        </button>
        <button class="btn btn-export" @click="exportExcel" :disabled="loading">
          Xuất Excel
        </button>
        <button
          class="btn btn-bulk-mail"
          :class="{ active: selectedEmployeeIds.length > 0 }"
          :disabled="selectedEmployeeIds.length === 0"
          @click="openBulkEmailModal"
          :title="selectedEmployeeIds.length === 0 ? 'Vui lòng tích chọn nhân viên đang hoạt động để gửi mail' : 'Gửi email đến ' + selectedEmployeeIds.length + ' nhân viên đã chọn'"
        >
          Gửi mail hàng loạt ({{ selectedEmployeeIds.length }})
        </button>
        <button class="btn btn-primary" @click="goToCreate">
          + Thêm nhân viên
        </button>
      </div>
    </div>

    <!-- Bảng danh sách -->
    <div class="content-card table-card">
      <div class="table-header-row">
        <div class="table-title-wrap">
          <h3 class="table-title">Danh sách nhân viên</h3>
        </div>
      </div>

      <!-- Thanh thông báo chọn hàng loạt -->
      <div v-if="selectedEmployeeIds.length > 0" class="selection-notice-bar">
        <div class="selection-notice-left">
          <span class="selection-notice-badge">{{ selectedEmployeeIds.length }}</span>
          <span>nhân viên đang hoạt động đã được chọn để gửi email</span>
        </div>
        <div class="selection-notice-actions">
          <button type="button" class="btn-notice-action" @click="openBulkEmailModal">
            Gửi email ngay
          </button>
          <button type="button" class="btn-notice-clear" @click="clearSelection">
            Bỏ chọn tất cả
          </button>
        </div>
      </div>

      <div class="table-responsive">
        <table class="custom-table">
          <thead>
            <tr>
              <th style="width: 44px; text-align: center">
                <input
                  type="checkbox"
                  class="row-checkbox"
                  :checked="isAllActiveSelected"
                  :indeterminate.prop="isIndeterminate"
                  @change="toggleSelectAll"
                  title="Chọn tất cả nhân viên đang hoạt động trên trang này"
                />
              </th>
              <th style="width: 50px; text-align: center">STT</th>
              <th style="width: 65px; text-align: center">ẢNH</th>
              <th style="width: 110px">MÃ NV</th>
              <th style="width: 160px">HỌ TÊN</th>
              <th style="width: 120px">SĐT</th>
              <th style="width: 180px">EMAIL</th>
              <th>ĐỊA CHỈ</th>
              <th style="width: 120px">CHỨC VỤ</th>
              <th style="width: 120px; text-align: center">TRẠNG THÁI</th>
              <th style="width: 100px; text-align: center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="11" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Đang tải dữ liệu nhân viên...
              </td>
            </tr>
            <tr v-else-if="!employees || employees.length === 0">
              <td colspan="11" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Không tìm thấy nhân viên nào phù hợp.
              </td>
            </tr>
            <template v-else>
              <tr v-for="(item, index) in employees" :key="item.id" :class="{ 'row-selected': isSelected(item.id) }">
                <td style="text-align: center">
                  <input
                    type="checkbox"
                    class="row-checkbox"
                    :value="item.id"
                    :checked="isSelected(item.id)"
                    :disabled="item.status !== 'active'"
                    @change="toggleSelect(item)"
                    :title="item.status !== 'active' ? 'Chỉ nhân viên đang hoạt động mới được chọn gửi mail' : 'Chọn ' + item.fullName"
                  />
                </td>
                <td style="text-align: center" class="text-muted">
                  {{ (pagination.page * pagination.size) + index + 1 }}
                </td>
                <td style="text-align: center">
                  <div class="avatar-cell">
                    <img v-if="item.avatar" :src="item.avatar" class="avatar-img" alt="avatar" @error="item.avatar = ''" />
                    <div v-else class="avatar-placeholder">{{ item.initials }}</div>
                  </div>
                </td>
                <td class="font-bold text-blue">{{ item.code }}</td>
                <td class="font-medium text-dark">{{ item.fullName }}</td>
                <td class="text-dark">{{ item.phone || '-' }}</td>
                <td class="text-email" :title="item.email">{{ item.email || '-' }}</td>
                <td class="text-address">{{ item.address || '-' }}</td>
                <td>
                  <span class="role-text" :class="{'role-admin': item.role === 'Quản trị viên'}">
                    {{ item.role || '-' }}
                  </span>
                </td>
                <td style="text-align: center">
                  <span class="badge-status" :class="item.status === 'active' ? 'status-active' : 'status-inactive'">
                    {{ item.statusText }}
                  </span>
                </td>
                <td style="text-align: center">
                  <div class="action-buttons">
                    <label
                      v-if="item.role !== 'Quản trị viên'"
                      class="switch-toggle"
                      :title="item.status === 'active' ? 'Đang hoạt động (nhấn để tắt)' : 'Đang ngừng hoạt động (nhấn để bật)'"
                    >
                      <input
                        type="checkbox"
                        :checked="item.status === 'active'"
                        @click.prevent="toggleStatus(item)"
                      />
                      <span class="slider-round"></span>
                    </label>
                    <button
                      class="btn-circle-action btn-edit"
                      title="Chỉnh sửa nhân viên"
                      @click="goToEdit(item.id)"
                    >
                      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="pagination-row" v-if="pagination.totalElements > 0">
        <div class="pagination-info">
          Hiển thị {{ employees.length ? (pagination.page * pagination.size + 1) : 0 }} - {{ pagination.page * pagination.size + employees.length }} / {{ pagination.totalElements }} nhân viên
        </div>
        <div class="pagination-buttons">
          <button class="page-btn" :disabled="pagination.page === 0" @click="goPage(pagination.page - 1)">‹</button>
          <button
            v-for="p in pageNumbers"
            :key="p"
            class="page-btn"
            :class="{ active: p === pagination.page }"
            @click="goPage(p)"
          >
            {{ p + 1 }}
          </button>
          <button class="page-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="goPage(pagination.page + 1)">›</button>
        </div>
      </div>
    </div>

    <!-- Modal Gửi mail hàng loạt -->
    <Teleport to="body">
      <div v-if="showBulkEmailModal" class="modal-backdrop" @click.self="closeBulkEmailModal">
        <div class="modal-card modal-bulk-email">
          <div class="modal-header">
            <h3 class="modal-title">Gửi email hàng loạt cho nhân viên</h3>
            <button type="button" class="btn-close-modal" @click="closeBulkEmailModal" :disabled="bulkEmailSending">✕</button>
          </div>

          <div class="modal-body">
            <div class="recipient-summary-box">
              <div class="summary-top">
                <span class="summary-label">Danh sách nhận email:</span>
                <span class="summary-count"><strong>{{ selectedEmployeesList.length }}</strong> nhân viên (Đang hoạt động)</span>
              </div>
              <div class="recipients-chip-list">
                <span v-for="emp in selectedEmployeesList" :key="emp.id" class="recipient-badge">
                  {{ emp.fullName }} <small>({{ emp.email || 'Chưa có email' }})</small>
                </span>
              </div>
            </div>

            <div class="form-section">
              <label class="form-label-title">Loại thư gửi:</label>
              <div class="email-type-cards">
                <label class="type-card" :class="{ active: bulkEmailType === 'account_info' }">
                  <input type="radio" v-model="bulkEmailType" value="account_info" :disabled="bulkEmailSending" />
                  <div class="type-card-content">
                    <span class="type-card-title">Thông tin tài khoản đăng nhập</span>
                    <span class="type-card-desc">Gửi thông tin đăng nhập gồm Mã NV, Tên đăng nhập và Mật khẩu đến từng nhân viên.</span>
                  </div>
                </label>

                <label class="type-card" :class="{ active: bulkEmailType === 'custom' }">
                  <input type="radio" v-model="bulkEmailType" value="custom" :disabled="bulkEmailSending" />
                  <div class="type-card-content">
                    <span class="type-card-title">Thông báo tùy chỉnh</span>
                    <span class="type-card-desc">Tự soạn tiêu đề và nội dung thông báo gửi đến toàn bộ nhân viên được chọn.</span>
                  </div>
                </label>
              </div>
            </div>

            <div v-if="bulkEmailType === 'custom'" class="custom-email-inputs">
              <div class="form-field-group">
                <label>Tiêu đề email <span class="required">*</span></label>
                <input
                  v-model="bulkEmailSubject"
                  type="text"
                  placeholder="Nhập tiêu đề thông báo..."
                  class="modal-input"
                  :disabled="bulkEmailSending"
                />
              </div>

              <div class="form-field-group">
                <label>Nội dung email <span class="required">*</span></label>
                <textarea
                  v-model="bulkEmailContent"
                  rows="5"
                  placeholder="Nhập nội dung thông báo gửi đến các nhân viên..."
                  class="modal-textarea"
                  :disabled="bulkEmailSending"
                ></textarea>
              </div>
            </div>

            <div v-if="bulkEmailSending" class="modal-loading-overlay">
              <div class="modal-spinner"></div>
              <span>Đang gửi email đến từng nhân viên, vui lòng chờ trong giây lát...</span>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-cancel" :disabled="bulkEmailSending" @click="closeBulkEmailModal">
              Hủy bỏ
            </button>
            <button type="button" class="btn-submit-action" :disabled="bulkEmailSending" @click="handleSendBulkEmail">
              {{ bulkEmailSending ? 'Đang gửi email...' : 'Xác nhận gửi email' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { showConfirm, showAlert, showToast } from '@/utils/dialog.js'
import Select2Combobox from '@/components/Select2Combobox.vue'

const router = useRouter()
const employees = ref([])
const roles = ref([])
const trangThaiOptions = [
  { value: '1', label: 'Hoạt động' },
  { value: '0', label: 'Ngừng hoạt động' }
]
const loading = ref(false)
const filters = reactive({ keyword: '', role: '', status: '' })
const pagination = reactive({ page: 0, size: 5, totalPages: 0, totalElements: 0 })

const pageNumbers = computed(() => {
  const total = pagination.totalPages
  if (!total) return []
  const start = Math.max(0, pagination.page - 2)
  const end = Math.min(total, start + 5)
  return Array.from({ length: end - start }, (_, i) => start + i)
})

function mapEmployee(x) {
  if (!x) return {}
  const gender = x.gioiTinh === true ? 'Nam' : x.gioiTinh === false ? 'Nữ' : '-'
  const address = [x.diaChiCuThe, x.phuong, x.queQuan].filter(Boolean).join(', ')
  const avatar = x.anhNhanVien
    ? (x.anhNhanVien.startsWith('http') ? x.anhNhanVien : `http://localhost:8080${x.anhNhanVien}`)
    : ''
  return {
    ...x,
    avatar,
    code: x.maNhanVien || '-',
    fullName: x.tenNhanVien || '-',
    phone: x.soDienThoai || '-',
    gender,
    address,
    role: x.tenVaiTro || '-',
    status: x.trangThai === 1 ? 'active' : 'inactive',
    statusText: x.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động',
    initials: getInitials(x.tenNhanVien)
  }
}

function getInitials(name) {
  if (!name || typeof name !== 'string') return 'NV'
  const p = name.trim().split(/\s+/).filter(Boolean)
  if (p.length === 0) return 'NV'
  if (p.length === 1) return (p[0].slice(0, 2) || 'NV').toUpperCase()
  const first = p[0][0] || ''
  const last = p[p.length - 1][0] || ''
  return (first + last).toUpperCase() || 'NV'
}

async function loadRoles() {
  try {
    const res = await api.get('/api/nhan-vien/vai-tro')
    const rawRoles = Array.isArray(res.data) ? res.data : (res.data?.content || res.data?.value || [])
    roles.value = rawRoles.filter(r =>
      r.maVaiTro === 'ADMIN' || r.maVaiTro === 'NV' ||
      r.tenVaiTro === 'Quản trị viên' || r.tenVaiTro === 'Nhân viên'
    )
  } catch (e) {
    console.error('Lỗi tải vai trò:', e)
  }
}

async function loadEmployees() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
      idVaiTro: filters.role || undefined,
      trangThai: filters.status === '' ? undefined : filters.status
    }
    const res = await api.get('/api/nhan-vien', { params })
    const content = res.data?.content || (Array.isArray(res.data) ? res.data : [])
    employees.value = content.map(mapEmployee)
    pagination.totalPages = res.data?.totalPages || (content.length > 0 ? 1 : 0)
    pagination.totalElements = res.data?.totalElements != null ? res.data.totalElements : content.length
  } catch (e) {
    console.error('Lỗi tải danh sách nhân viên:', e)
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
    pagination.page = 0
    loadEmployees()
  }, 250)
}

function resetFilters() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  pagination.page = 0
  loadEmployees()
}

function goPage(page) {
  if (page < 0 || page >= pagination.totalPages) return
  pagination.page = page
  loadEmployees()
}

function goToCreate() {
  router.push('/nhan-vien/them')
}

function goToEdit(id) {
  router.push(`/nhan-vien/${id}`)
}

async function toggleStatus(item) {
  const isLocking = item.status === 'active'
  const actionText = isLocking ? 'khóa tài khoản' : 'mở khóa tài khoản'
  
  const confirmed = await showConfirm({
    title: isLocking ? 'Xác nhận khóa tài khoản' : 'Xác nhận mở khóa tài khoản',
    message: `Bạn có chắc chắn muốn ${actionText} của nhân viên "${item.fullName}" (${item.code}) không?`,
    type: isLocking ? 'warning' : 'question',
    confirmText: isLocking ? 'Khóa tài khoản' : 'Mở khóa'
  })

  if (!confirmed) return

  try {
    await api.patch(`/api/nhan-vien/${item.id}/toggle-status`)
    item.status = isLocking ? 'inactive' : 'active'
    item.statusText = isLocking ? 'Ngừng hoạt động' : 'Hoạt động'
    item.trangThai = isLocking ? 0 : 1
    if (isLocking) {
      selectedEmployeesMap.value.delete(item.id)
    }
    showToast(`${isLocking ? 'Khóa' : 'Mở khóa'} nhân viên thành công!`, 'success')
  } catch (e) {
    showAlert({
      title: 'Thao tác thất bại',
      message: e?.response?.data?.message || 'Không thể thay đổi trạng thái nhân viên.',
      type: 'error'
    })
  }
}

// Quản lý chọn nhân viên (chỉ chọn nhân viên đang hoạt động)
const selectedEmployeesMap = ref(new Map())
const selectedEmployeeIds = computed(() => Array.from(selectedEmployeesMap.value.keys()))
const selectedEmployeesList = computed(() => Array.from(selectedEmployeesMap.value.values()))

const activeEmployeesOnPage = computed(() => employees.value.filter(e => e.status === 'active'))

const isAllActiveSelected = computed(() => {
  if (activeEmployeesOnPage.value.length === 0) return false
  return activeEmployeesOnPage.value.every(e => selectedEmployeesMap.value.has(e.id))
})

const isIndeterminate = computed(() => {
  if (isAllActiveSelected.value) return false
  return activeEmployeesOnPage.value.some(e => selectedEmployeesMap.value.has(e.id))
})

function isSelected(id) {
  return selectedEmployeesMap.value.has(id)
}

function toggleSelect(item) {
  if (item.status !== 'active') return
  if (selectedEmployeesMap.value.has(item.id)) {
    selectedEmployeesMap.value.delete(item.id)
  } else {
    selectedEmployeesMap.value.set(item.id, {
      id: item.id,
      fullName: item.fullName,
      code: item.code,
      email: item.email
    })
  }
}

function toggleSelectAll(e) {
  const checked = e.target.checked
  if (checked) {
    activeEmployeesOnPage.value.forEach(item => {
      selectedEmployeesMap.value.set(item.id, {
        id: item.id,
        fullName: item.fullName,
        code: item.code,
        email: item.email
      })
    })
  } else {
    activeEmployeesOnPage.value.forEach(item => {
      selectedEmployeesMap.value.delete(item.id)
    })
  }
}

function clearSelection() {
  selectedEmployeesMap.value.clear()
}

// Gửi email hàng loạt
const showBulkEmailModal = ref(false)
const bulkEmailType = ref('account_info')
const bulkEmailSubject = ref('')
const bulkEmailContent = ref('')
const bulkEmailSending = ref(false)

function openBulkEmailModal() {
  if (selectedEmployeeIds.value.length === 0) {
    return showAlert({
      title: 'Chưa chọn nhân viên',
      message: 'Vui lòng chọn ít nhất một nhân viên đang hoạt động để gửi email.',
      type: 'warning'
    })
  }
  bulkEmailSubject.value = ''
  bulkEmailContent.value = ''
  showBulkEmailModal.value = true
}

function closeBulkEmailModal() {
  if (bulkEmailSending.value) return
  showBulkEmailModal.value = false
}

async function handleSendBulkEmail() {
  if (bulkEmailType.value === 'custom') {
    if (!bulkEmailSubject.value.trim()) {
      return showAlert({
        title: 'Thiếu thông tin',
        message: 'Vui lòng nhập tiêu đề email thông báo.',
        type: 'warning'
      })
    }
    if (!bulkEmailContent.value.trim()) {
      return showAlert({
        title: 'Thiếu thông tin',
        message: 'Vui lòng nhập nội dung email thông báo.',
        type: 'warning'
      })
    }
  }

  const confirmed = await showConfirm({
    title: 'Xác nhận gửi email hàng loạt',
    message: `Bạn có chắc muốn gửi email đến ${selectedEmployeeIds.value.length} nhân viên đang hoạt động đã chọn?`,
    type: 'question',
    confirmText: 'Gửi ngay',
    cancelText: 'Xem lại'
  })
  if (!confirmed) return

  bulkEmailSending.value = true
  try {
    const payload = {
      employeeIds: selectedEmployeeIds.value,
      emailType: bulkEmailType.value,
      subject: bulkEmailType.value === 'custom' ? bulkEmailSubject.value.trim() : null,
      content: bulkEmailType.value === 'custom' ? bulkEmailContent.value.trim() : null
    }
    const res = await api.post('/api/nhan-vien/bulk-email', payload)
    const data = res.data
    closeBulkEmailModal()

    if (data.totalFailed === 0) {
      showAlert({
        title: 'Gửi email thành công',
        message: `Đã gửi thành công ${data.totalSent} email đến các nhân viên đang hoạt động.`,
        type: 'success'
      })
    } else {
      showAlert({
        title: 'Kết quả gửi email',
        message: `Đã gửi thành công ${data.totalSent} email. Thất bại ${data.totalFailed} email.\n${data.failedEmails && data.failedEmails.length ? 'Các email thất bại: ' + data.failedEmails.join(', ') : ''}`,
        type: 'warning'
      })
    }
    clearSelection()
  } catch (e) {
    showAlert({
      title: 'Gửi email thất bại',
      message: e?.response?.data?.message || 'Có lỗi xảy ra trong quá trình gửi email hàng loạt.',
      type: 'error'
    })
  } finally {
    bulkEmailSending.value = false
  }
}

async function exportExcel() {
  try {
    const params = {
      keyword: filters.keyword || undefined,
      idVaiTro: filters.role || undefined,
      trangThai: filters.status === '' ? undefined : filters.status
    }
    const res = await api.get('/api/nhan-vien/export-excel', { params, responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'danh-sach-nhan-vien.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    showToast('Xuất danh sách nhân viên ra Excel thành công!', 'success')
  } catch (e) {
    showAlert({
      title: 'Lỗi xuất Excel',
      message: e?.response?.data?.message || 'Không thể tạo file Excel.',
      type: 'error'
    })
  }
}

onMounted(async () => {
  await loadRoles()
  await loadEmployees()
})
</script>

<style scoped>
.employee-page-wrapper {
  padding: 1.25rem 2rem 3rem;
  background-color: var(--bg, #f7f5ef);
  min-height: calc(100vh - 48px);
  color: var(--text, #3d4a50);
  box-sizing: border-box;
}

.page-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  margin-bottom: 20px;
}

.crumb-parent {
  color: #8c969e;
  font-weight: 400;
  cursor: pointer;
  transition: color 0.2s;
}

.crumb-parent:hover {
  color: #3e5c76;
}

.crumb-separator {
  color: #b0b7bd;
  font-weight: 300;
  user-select: none;
}

.crumb-current {
  color: #3e5c76;
  font-weight: 600;
}

.content-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  margin-bottom: 1.25rem;
}

.card-header-filter {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.2rem;
}

.filter-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #f7eee1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #b18b52;
  flex-shrink: 0;
}

.filter-icon-box svg {
  display: block;
}

.filter-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: #43545c;
  line-height: 1;
}

.filter-inputs-grid {
  display: grid;
  grid-template-columns: 2.2fr 1fr 1fr;
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

.prefix-icon {
  position: absolute;
  left: 0.85rem;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #8c979a;
  pointer-events: none;
  z-index: 1;
}

.prefix-icon svg {
  display: block;
  width: 18px;
  height: 18px;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 2.6rem;
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

.search-field input {
  padding-left: 2.5rem;
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  border-top: 1px dashed #efeae0;
  padding-top: 1.1rem;
}

.btn {
  height: 2.5rem;
  padding: 0 1.35rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.2s;
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

.btn-bulk-mail {
  background-color: #f7eee1;
  color: #a67c52;
  border: 1px solid #e5d3bc;
}
.btn-bulk-mail:hover:not(:disabled) {
  background-color: #ecdcc6;
  color: #8c6239;
}
.btn-bulk-mail.active {
  background-color: #b38e6e;
  color: #ffffff;
  border-color: #a67c52;
  box-shadow: 0 2px 6px rgba(179, 142, 110, 0.3);
}
.btn-bulk-mail:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-primary {
  background-color: #3b596a;
  border: 1px solid #324b5a;
  border-radius: 9999px;
  color: #ffffff;
  padding: 0 1.4rem;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
  transition: all 0.2s ease;
}
.btn-primary:hover {
  background-color: #2c4350;
  border-color: #243742;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
  transform: translateY(-1px);
}

.table-header-row {
  margin-bottom: 1.1rem;
}

.table-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.header-icon {
  font-size: 1.15rem;
}

.table-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
  color: #3c4d55;
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.custom-table th {
  background-color: #faf8f5;
  color: #4a5568;
  font-weight: 700;
  text-transform: uppercase;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  padding: 0.95rem 1rem;
  text-align: left;
  border-bottom: 2px solid #e9e5db;
  white-space: nowrap;
}

.custom-table td {
  padding: 1.05rem 1rem;
  border-bottom: 1px solid #f2f0eb;
  color: #4b585e;
  vertical-align: middle;
}

.custom-table tr:hover td {
  background-color: #fcfbf8;
}

.avatar-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #f0f0f0;
}

.avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f0e6dc;
  color: #8c6a46;
  font-size: 0.85rem;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.font-bold {
  font-weight: 700;
}

.font-medium {
  font-weight: 600;
}

.text-blue {
  color: #8c6a46;
  font-family: monospace, sans-serif;
  letter-spacing: 0.4px;
}

.text-dark {
  color: #2b383e;
}

.text-email {
  color: #556268;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-address {
  color: #556268;
  line-height: 1.45;
  max-width: 280px;
}

.text-muted {
  color: #9aa0a0;
}

.role-text {
  font-size: 0.92rem;
  color: #4f5d63;
}

.role-admin {
  font-weight: 700;
  color: #8c6a46;
}

.badge-status {
  display: inline-block;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.32rem 0.85rem;
  border-radius: 9999px;
  white-space: nowrap;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

/* Switch Toggle */
.switch-toggle {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 22px;
  cursor: pointer;
  margin: 0;
  flex-shrink: 0;
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
  background-color: var(--blue, #496883);
}

.switch-toggle input:checked + .slider-round::before {
  transform: translateX(16px);
}

.btn-circle-action {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid #d8e2e8;
  background-color: #f7f5ee;
  color: #496883;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-circle-action svg {
  display: block;
}

.btn-edit:hover {
  background-color: #496883;
  color: #ffffff;
  border-color: #496883;
}

.pagination-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding-top: 1.1rem;
}

.pagination-info {
  color: #7c878b;
  font-size: 0.88rem;
}

.pagination-buttons {
  display: flex;
  gap: 0.35rem;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  border: 1px solid var(--line, #e9e5db);
  background: #fff;
  color: #496883;
  border-radius: 7px;
  cursor: pointer;
  font-weight: 600;
}

.page-btn.active {
  background: #496883;
  color: #fff;
  border-color: #496883;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .filter-inputs-grid {
    grid-template-columns: 1fr;
  }
}

/* Checkbox & Selection Bar */
.row-checkbox {
  width: 17px;
  height: 17px;
  cursor: pointer;
  accent-color: var(--blue, #496883);
}

.row-checkbox:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.custom-table tr.row-selected td {
  background-color: #fcf8f2 !important;
}

.selection-notice-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fdfbf7;
  border: 1px solid #ebdccb;
  border-radius: 8px;
  padding: 0.65rem 1.1rem;
  margin-bottom: 0.95rem;
  font-size: 0.92rem;
  color: #5a4b3d;
}

.selection-notice-left {
  display: flex;
  align-items: center;
}

.selection-notice-badge {
  background-color: #b38e6e;
  color: #ffffff;
  font-size: 0.78rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
  margin-right: 0.5rem;
}

.selection-notice-actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.btn-notice-action {
  background-color: #b38e6e;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 700;
  padding: 0.35rem 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-notice-action:hover {
  background-color: #9c7756;
}

.btn-notice-clear {
  background: none;
  border: none;
  color: #8c979a;
  font-size: 0.84rem;
  cursor: pointer;
  text-decoration: underline;
}

.btn-notice-clear:hover {
  color: #556268;
}

/* Modal System */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(22, 30, 36, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
  padding: 1rem;
  box-sizing: border-box;
}

.modal-card {
  background: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 680px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  animation: modalPop 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  padding: 1.15rem 1.6rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee9e0;
  background-color: #faf8f5;
}

.modal-title {
  margin: 0;
  font-size: 1.18rem;
  font-weight: 700;
  color: #3b4d55;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.25rem;
  line-height: 1;
  color: #8c979a;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
}

.btn-close-modal:hover {
  color: #2b383e;
}

.modal-body {
  padding: 1.4rem 1.6rem;
  overflow-y: auto;
  flex: 1;
  position: relative;
}

.modal-footer {
  padding: 1rem 1.6rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  border-top: 1px solid #eee9e0;
  background-color: #faf8f5;
}

.btn-cancel {
  background: #ffffff;
  border: 1px solid #dcd4c6;
  color: #556268;
  padding: 0.58rem 1.3rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover:not(:disabled) {
  background-color: #f7f4ee;
  border-color: #c7beaf;
}

.btn-submit-action {
  background-color: #3b596a;
  color: #ffffff;
  border: none;
  padding: 0.58rem 1.45rem;
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(59, 89, 106, 0.25);
  transition: all 0.2s;
}

.btn-submit-action:hover:not(:disabled) {
  background-color: #2c4350;
  box-shadow: 0 4px 10px rgba(59, 89, 106, 0.35);
}

.btn-submit-action:disabled,
.btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Modal Gửi email */
.recipient-summary-box {
  background-color: #fdfbf7;
  border: 1px solid #ebdccb;
  border-radius: 8px;
  padding: 0.95rem 1.1rem;
  margin-bottom: 1.25rem;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.92rem;
  color: #4b585e;
  margin-bottom: 0.65rem;
}

.summary-count strong {
  color: #b38e6e;
}

.recipients-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  max-height: 96px;
  overflow-y: auto;
  padding-right: 4px;
}

.recipient-badge {
  background-color: #f4ecdf;
  color: #7a5a3a;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0.22rem 0.65rem;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.recipient-badge small {
  color: #9c7b5b;
  font-weight: 400;
}

.form-section {
  margin-bottom: 1.25rem;
}

.form-label-title {
  display: block;
  font-size: 0.92rem;
  font-weight: 700;
  color: #3b4d55;
  margin-bottom: 0.55rem;
}

.email-type-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

.type-card {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  border: 1.5px solid #e7dfd4;
  background-color: #fdfbf8;
  border-radius: 9px;
  padding: 0.85rem 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.type-card:hover {
  border-color: #c9b9a6;
  background-color: #faf6f0;
}

.type-card.active {
  border-color: #b38e6e;
  background-color: #faf3ea;
  box-shadow: 0 2px 8px rgba(179, 142, 110, 0.15);
}

.type-card input[type="radio"] {
  accent-color: #b38e6e;
  margin-top: 0.2rem;
}

.type-card-title {
  display: block;
  font-size: 0.92rem;
  font-weight: 700;
  color: #3b4d55;
  margin-bottom: 0.2rem;
}

.type-card-desc {
  display: block;
  font-size: 0.8rem;
  color: #7b878b;
  line-height: 1.35;
}

.custom-email-inputs {
  display: flex;
  flex-direction: column;
  gap: 0.95rem;
  margin-top: 0.5rem;
}

.form-field-group label {
  display: block;
  font-size: 0.88rem;
  font-weight: 700;
  color: #4b585e;
  margin-bottom: 0.35rem;
}

.form-field-group .required {
  color: #dc2626;
}

.modal-input,
.modal-textarea {
  width: 100%;
  border: 1px solid #d9d0c2;
  border-radius: 8px;
  padding: 0.65rem 0.9rem;
  font-size: 0.92rem;
  color: #3b4d55;
  background-color: #fdfbf8;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.modal-input:focus,
.modal-textarea:focus {
  border-color: #b38e6e;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(179, 142, 110, 0.12);
}

.modal-textarea {
  resize: vertical;
  line-height: 1.45;
}



/* Spinner Overlay */
.modal-loading-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(255, 255, 255, 0.88);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.85rem;
  z-index: 20;
  font-size: 0.92rem;
  font-weight: 600;
  color: #4b585e;
}

.modal-spinner {
  width: 34px;
  height: 34px;
  border: 3px solid #e7dfd4;
  border-top-color: #b38e6e;
  border-radius: 50%;
  animation: modalSpin 0.75s linear infinite;
}

@keyframes modalSpin {
  to {
    transform: rotate(360deg);
  }
}
</style>
