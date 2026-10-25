<template>
  <div class="quan-ly-ca-page">
    <!-- Tiêu đề trang -->
    <div class="page-breadcrumb">
      <router-link to="/ca-lam-viec" class="crumb-parent">Lịch làm việc</router-link>
      <span class="crumb-separator">/</span>
      <span class="crumb-current">Quản lý ca làm việc</span>
    </div>
    <div class="page-top-bar" v-if="false">
      <div class="title-with-icon">
        <div class="title-icon-box">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
        </div>
        <h1 class="page-main-title">Quản lý ca làm việc</h1>
      </div>
    </div>

    <!-- 1. Card Bộ lọc tìm kiếm -->
    <div class="content-card filter-card-box">
      <div class="filter-heading">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="filter-icon">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
        </svg>
        <span class="filter-title-text">Bộ lọc tìm kiếm ca làm việc</span>
      </div>

      <div class="filter-grid-fields">
        <!-- Tìm kiếm tên ca -->
        <div class="filter-col">
          <label class="field-label-tiny">Tìm kiếm ca làm việc</label>
          <div class="input-with-prefix-icon">
            <svg v-if="false" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="prefix-icon">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              v-model="filters.keyword"
              placeholder="Tìm theo tên ca..."
              class="form-control-minimal"
              @input="onFilterChange"
            />
          </div>
        </div>

        <!-- Trạng thái -->
        <div class="filter-col">
          <label class="field-label-tiny">Trạng thái</label>
          <div class="select-wrapper-minimal">
            <select v-model="filters.trangThai" class="form-control-minimal" @change="onFilterChange">
              <option value="">Tất cả trạng thái</option>
              <option value="1">Đang hoạt động</option>
              <option value="0">Ngừng hoạt động</option>
            </select>
          </div>
        </div>

        <!-- Giờ bắt đầu (Tìm chính xác) -->
        <div class="filter-col">
          <label class="field-label-tiny">Thời gian bắt đầu (Tìm chính xác)</label>
          <input
            type="time"
            v-model="filters.gioBatDau"
            class="form-control-minimal"
            :class="{ 'input-error-border': timeFilterError }"
            @change="onTimeFilterChange"
          />
        </div>

        <!-- Giờ kết thúc (Tìm chính xác) -->
        <div class="filter-col">
          <label class="field-label-tiny">Thời gian kết thúc (Tìm chính xác)</label>
          <input
            type="time"
            v-model="filters.gioKetThuc"
            class="form-control-minimal"
            :class="{ 'input-error-border': timeFilterError }"
            @change="onTimeFilterChange"
          />
        </div>

        <!-- Cụm 2 nút thao tác -->
        <div class="filter-actions-col">
          <button type="button" class="btn-reset-filter" @click="resetFilters" title="Đặt lại bộ lọc về mặc định">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 4v6h-6" />
              <path d="M1 20v-6h6" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>Đặt lại bộ lọc</span>
          </button>

          <button type="button" class="btn-primary-beige" @click="openModalCreate">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>+ Thêm ca làm việc</span>
          </button>
        </div>
      </div>

      <!-- Báo lỗi lọc thời gian nếu bắt đầu >= kết thúc -->
      <div v-if="timeFilterError" class="filter-error-msg">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <span>{{ timeFilterError }}</span>
      </div>
    </div>

    <!-- 2. Bảng danh sách Ca làm việc -->
    <div class="content-card table-card-box">
      <div class="table-responsive-clean">
        <table class="minimal-styled-table">
          <thead>
            <tr class="header-brown-beige">
              <th style="width: 70px; text-align: center">STT</th>
              <th style="min-width: 180px">TÊN CA</th>
              <th style="width: 170px; text-align: center">GIỜ BẮT ĐẦU</th>
              <th style="width: 170px; text-align: center">GIỜ KẾT THÚC</th>
              <th style="width: 170px; text-align: center">TRẠNG THÁI</th>
              <th style="width: 150px; text-align: center">HÀNH ĐỘNG</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="6" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Đang tải danh sách ca làm việc...
              </td>
            </tr>
            <tr v-else-if="shiftList.length === 0">
              <td colspan="6" style="text-align: center; padding: 2.5rem; color: #8a969b;">
                Không tìm thấy ca làm việc nào phù hợp.
              </td>
            </tr>
            <tr v-for="(shift, index) in shiftList" :key="shift.id" class="table-data-row">
              <td style="text-align: center" class="text-muted-stt">{{ index + 1 }}</td>
              <td>
                <div class="shift-name-group">
                  <strong class="shift-title">{{ shift.tenCa }}</strong>
                  <span class="shift-code-tag">{{ shift.maCa }}</span>
                </div>
                <div v-if="shift.moTa" class="shift-desc-hint">{{ shift.moTa }}</div>
              </td>
              <td style="text-align: center" class="font-bold text-dark">
                {{ formatTimeDisplay(shift.gioBatDau) }}
              </td>
              <td style="text-align: center" class="font-bold text-dark">
                {{ formatTimeDisplay(shift.gioKetThuc) }}
              </td>
              <td style="text-align: center">
                <span class="badge-status" :class="shift.trangThai === 1 ? 'status-active' : 'status-inactive'">
                  {{ shift.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
                </span>
              </td>
              <td style="text-align: center">
                <div class="action-buttons-group">
                  <!-- Switch bật/tắt trạng thái -->
                  <label class="switch-toggle" :title="shift.trangThai === 1 ? 'Nhấn để ngừng hoạt động' : 'Nhấn để kích hoạt lại'">
                    <input
                      type="checkbox"
                      :checked="shift.trangThai === 1"
                      @click.prevent="handleToggleStatus(shift)"
                    />
                    <span class="slider-round"></span>
                  </label>

                  <!-- Nút icon bút chỉnh sửa -->
                  <button
                    type="button"
                    class="btn-circle-edit"
                    title="Chỉnh sửa ca làm việc"
                    @click="openModalEdit(shift)"
                  >
                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal Thêm Mới / Chỉnh Sửa Ca Làm Việc -->
    <div v-if="showModal" class="modal-backdrop-clean" @click.self="closeModal">
      <div class="modal-dialog-clean">
        <div class="modal-header-clean">
          <h3 class="modal-title-clean">{{ isEditing ? 'Chỉnh sửa ca làm việc' : 'Thêm mới ca làm việc' }}</h3>
          <button type="button" class="btn-close-modal" @click="closeModal">✕</button>
        </div>

        <div class="modal-body-clean">
          <!-- Tên ca * -->
          <div class="form-group-item">
            <label class="modal-field-label">Tên ca <span class="required-star">*</span></label>
            <input
              type="text"
              v-model="form.tenCa"
              placeholder="Ví dụ: Ca Sáng, Ca Chiều, Ca Tối..."
              class="form-control-minimal modal-input"
              :class="{ 'input-error-border': errors.tenCa }"
              @input="onTenCaInput"
            />
            <span v-if="errors.tenCa" class="error-text-red">{{ errors.tenCa }}</span>
          </div>

          <!-- Giờ bắt đầu & Giờ kết thúc -->
          <div class="form-row-2cols">
            <div class="form-group-item">
              <label class="modal-field-label">Giờ bắt đầu <span class="required-star">*</span></label>
              <input
                type="time"
                v-model="form.gioBatDau"
                class="form-control-minimal modal-input"
                :class="{ 'input-error-border': errors.gioBatDau || errors.timeRange }"
                @change="onModalTimeChange"
              />
              <span v-if="errors.gioBatDau" class="error-text-red">{{ errors.gioBatDau }}</span>
            </div>

            <div class="form-group-item">
              <label class="modal-field-label">Giờ kết thúc <span class="required-star">*</span></label>
              <input
                type="time"
                v-model="form.gioKetThuc"
                class="form-control-minimal modal-input"
                :class="{ 'input-error-border': errors.gioKetThuc || errors.timeRange }"
                @change="onModalTimeChange"
              />
              <span v-if="errors.gioKetThuc" class="error-text-red">{{ errors.gioKetThuc }}</span>
            </div>
          </div>

          <!-- Báo lỗi khoảng thời gian hoặc trùng lấn giờ -->
          <span v-if="errors.timeRange" class="error-text-red block-error">{{ errors.timeRange }}</span>
          <span v-if="errors.overlap" class="error-text-red block-error">{{ errors.overlap }}</span>

          <!-- Mô tả chi tiết -->
          <div class="form-group-item">
            <div class="label-with-counter">
              <label class="modal-field-label">Mô tả chi tiết</label>
              <span class="char-counter" :class="{ 'char-exceeded': (form.moTa || '').length > 255 }">
                {{ (form.moTa || '').length }} / 255
              </span>
            </div>
            <textarea
              v-model="form.moTa"
              rows="3"
              placeholder="Nhập ghi chú hoặc mô tả phân công của ca..."
              class="form-control-minimal modal-textarea"
              :class="{ 'input-error-border': errors.moTa }"
              @input="onMoTaInput"
            ></textarea>
            <span v-if="errors.moTa" class="error-text-red">{{ errors.moTa }}</span>
          </div>

          <!-- Trạng thái -->
          <div class="form-group-item" v-if="isEditing">
            <label class="modal-field-label">Trạng thái ca làm việc</label>
            <div class="select-wrapper-minimal">
              <select v-model.number="form.trangThai" class="form-control-minimal modal-input">
                <option :value="1">Đang hoạt động</option>
                <option :value="0">Ngừng hoạt động</option>
              </select>
            </div>
          </div>
        </div>

        <div class="modal-footer-clean">
          <button type="button" class="btn-cancel-modal" @click="closeModal">Hủy</button>
          <button
            type="button"
            class="btn-save-modal"
            :disabled="hasValidationErrors || saving"
            @click="handleSubmitForm"
          >
            {{ saving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/api'
import { showAlert, showConfirm, showToast } from '@/utils/dialog.js'

const loading = ref(false)
const saving = ref(false)
const shiftList = ref([])

const filters = reactive({
  keyword: '',
  trangThai: '',
  gioBatDau: '',
  gioKetThuc: ''
})

const timeFilterError = ref('')

const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)

const form = reactive({
  tenCa: '',
  gioBatDau: '',
  gioKetThuc: '',
  moTa: '',
  trangThai: 1
})

const errors = reactive({
  tenCa: '',
  gioBatDau: '',
  gioKetThuc: '',
  timeRange: '',
  overlap: '',
  moTa: ''
})

const hasValidationErrors = computed(() => {
  return !!(
    errors.tenCa ||
    errors.gioBatDau ||
    errors.gioKetThuc ||
    errors.timeRange ||
    errors.overlap ||
    errors.moTa ||
    !form.tenCa.trim() ||
    !form.gioBatDau ||
    !form.gioKetThuc
  )
})

function formatTimeDisplay(t) {
  if (!t) return '--:--'
  if (typeof t === 'string' && t.includes(':')) {
    const parts = t.split(':')
    const hh = (parts[0] || '00').padStart(2, '0')
    const mm = (parts[1] || '00').padStart(2, '0')
    return `${hh}:${mm}`
  }
  return String(t)
}

let filterDebounceTimer = null
function onFilterChange() {
  clearTimeout(filterDebounceTimer)
  filterDebounceTimer = setTimeout(() => {
    loadShifts()
  }, 250)
}

function onTimeFilterChange() {
  if (filters.gioBatDau && filters.gioKetThuc) {
    if (filters.gioBatDau >= filters.gioKetThuc) {
      timeFilterError.value = 'Thời gian bắt đầu tìm kiếm phải nhỏ hơn Thời gian kết thúc!'
      return
    }
  }
  timeFilterError.value = ''
  onFilterChange()
}

function resetFilters() {
  filters.keyword = ''
  filters.trangThai = ''
  filters.gioBatDau = ''
  filters.gioKetThuc = ''
  timeFilterError.value = ''
  loadShifts()
}

async function loadShifts() {
  if (timeFilterError.value) return
  loading.value = true
  try {
    const params = {}
    if (filters.keyword && filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.trangThai !== '' && filters.trangThai !== null && filters.trangThai !== undefined) {
      params.trangThai = Number(filters.trangThai)
    }
    if (filters.gioBatDau && filters.gioBatDau.trim()) {
      const s = filters.gioBatDau.trim()
      params.gioBatDau = s.length === 5 ? s + ':00' : s
    }
    if (filters.gioKetThuc && filters.gioKetThuc.trim()) {
      const e = filters.gioKetThuc.trim()
      params.gioKetThuc = e.length === 5 ? e + ':00' : e
    }

    const res = await api.get('/api/ca-lam-viec', { params })
    const dataList = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    shiftList.value = dataList.map(s => {
      const start = s.gioBatDau || s.startTime || '00:00:00'
      const end = s.gioKetThuc || s.endTime || '00:00:00'
      return {
        ...s,
        id: s.id,
        maCa: s.maCa || '',
        tenCa: s.tenCa || '',
        gioBatDau: start,
        gioKetThuc: end,
        startTime: start,
        endTime: end,
        moTa: s.moTa || '',
        trangThai: s.trangThai !== undefined && s.trangThai !== null ? s.trangThai : 1
      }
    })
  } catch (e) {
    showToast(e?.response?.data?.message || 'Không thể tải danh sách ca làm việc', 'error')
  } finally {
    loading.value = false
  }
}

// Modal Form handling
function openModalCreate() {
  isEditing.value = false
  editingId.value = null
  form.tenCa = ''
  form.gioBatDau = ''
  form.gioKetThuc = ''
  form.moTa = ''
  form.trangThai = 1
  clearErrors()
  showModal.value = true
}

function openModalEdit(shift) {
  if (!shift) return
  isEditing.value = true
  editingId.value = shift.id
  form.tenCa = shift.tenCa || ''
  const start = shift.gioBatDau || shift.startTime || '00:00'
  const end = shift.gioKetThuc || shift.endTime || '00:00'
  form.gioBatDau = start.substring(0, 5)
  form.gioKetThuc = end.substring(0, 5)
  form.moTa = shift.moTa || ''
  form.trangThai = shift.trangThai !== undefined ? shift.trangThai : 1
  clearErrors()
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  clearErrors()
}

function clearErrors() {
  errors.tenCa = ''
  errors.gioBatDau = ''
  errors.gioKetThuc = ''
  errors.timeRange = ''
  errors.overlap = ''
  errors.moTa = ''
}

// Real-time Validations
let debounceTenCaTimer = null
function onTenCaInput() {
  clearTimeout(debounceTenCaTimer)
  const val = form.tenCa.trim()

  if (!val) {
    errors.tenCa = 'Tên ca làm việc không được để trống'
    return
  }
  if (val.length < 2 || val.length > 50) {
    errors.tenCa = 'Tên ca làm việc phải từ 2 đến 50 ký tự'
    return
  }

  // Check trùng lặp ngay trong danh sách local trước
  const isDuplicateLocal = shiftList.value.some(s => {
    if (isEditing.value && s.id === editingId.value) return false
    return (s.tenCa || '').toLowerCase() === val.toLowerCase()
  })
  if (isDuplicateLocal) {
    errors.tenCa = 'Tên ca làm việc này đã tồn tại'
    return
  }

  errors.tenCa = ''

  // Debounce kiểm tra với backend API
  debounceTenCaTimer = setTimeout(async () => {
    try {
      const res = await api.get('/api/ca-lam-viec/check-ten', {
        params: {
          tenCa: val,
          excludeId: isEditing.value ? editingId.value : undefined
        }
      })
      if (res.data?.exists) {
        errors.tenCa = 'Tên ca làm việc này đã tồn tại'
      } else {
        errors.tenCa = ''
      }
    } catch (e) {}
  }, 250)
}

function onModalTimeChange() {
  errors.gioBatDau = form.gioBatDau ? '' : 'Vui lòng chọn Giờ bắt đầu'
  errors.gioKetThuc = form.gioKetThuc ? '' : 'Vui lòng chọn Giờ kết thúc'
  errors.timeRange = ''
  errors.overlap = ''

  if (!form.gioBatDau || !form.gioKetThuc) return

  // Check End > Start
  if (form.gioKetThuc <= form.gioBatDau) {
    errors.timeRange = 'Giờ kết thúc phải lớn hơn Giờ bắt đầu!'
    return
  }

  // Tính số phút chênh lệch
  const [sH, sM] = form.gioBatDau.split(':').map(Number)
  const [eH, eM] = form.gioKetThuc.split(':').map(Number)
  const diffMinutes = (eH * 60 + eM) - (sH * 60 + sM)
  if (diffMinutes < 30) {
    errors.timeRange = 'Thời lượng ca tối thiểu phải từ 30 phút trở lên!'
    return
  }

  // Check trùng lấn (Overlap) với các ca đang hoạt động
  const startSec = form.gioBatDau + ':00'
  const endSec = form.gioKetThuc + ':00'

  for (const s of shiftList.value) {
    if (s.trangThai !== 1) continue
    if (isEditing.value && s.id === editingId.value) continue

    const existingStart = s.gioBatDau || s.startTime || '00:00:00'
    const existingEnd = s.gioKetThuc || s.endTime || '00:00:00'

    // Điều kiện giao nhau: startA < endB && endA > startB
    if (startSec < existingEnd && endSec > existingStart) {
      errors.overlap = `Khung giờ (${form.gioBatDau} - ${form.gioKetThuc}) bị chồng lấn với ca '${s.tenCa}' (${formatTimeDisplay(existingStart)} - ${formatTimeDisplay(existingEnd)})!`
      return
    }
  }
}

function onMoTaInput() {
  if ((form.moTa || '').length > 255) {
    errors.moTa = 'Mô tả chi tiết tối đa 255 ký tự'
  } else {
    errors.moTa = ''
  }
}

async function handleSubmitForm() {
  onTenCaInput()
  onModalTimeChange()
  onMoTaInput()

  if (hasValidationErrors.value) {
    showToast('Vui lòng kiểm tra lại các trường thông tin bị lỗi', 'warning')
    return
  }

  saving.value = true
  try {
    const payload = {
      tenCa: form.tenCa.trim(),
      gioBatDau: form.gioBatDau + ':00',
      gioKetThuc: form.gioKetThuc + ':00',
      moTa: form.moTa ? form.moTa.trim() : null,
      trangThai: form.trangThai
    }

    if (isEditing.value) {
      await api.put(`/api/ca-lam-viec/${editingId.value}`, payload)
      showToast('Cập nhật ca làm việc thành công!', 'success')
    } else {
      await api.post('/api/ca-lam-viec', payload)
      showToast('Thêm mới ca làm việc thành công!', 'success')
    }

    closeModal()
    loadShifts()
  } catch (e) {
    showAlert({
      title: 'Lỗi lưu ca làm việc',
      message: e?.response?.data?.message || 'Không thể lưu ca làm việc.',
      type: 'error'
    })
  } finally {
    saving.value = false
  }
}

async function handleToggleStatus(shift) {
  const isTurningOff = shift.trangThai === 1
  const actionText = isTurningOff ? 'ngừng hoạt động' : 'kích hoạt lại'

  const confirmed = await showConfirm({
    title: isTurningOff ? 'Ngừng hoạt động ca làm việc?' : 'Kích hoạt lại ca làm việc?',
    message: `Bạn có chắc chắn muốn ${actionText} ca "${shift.tenCa}" (${formatTimeDisplay(shift.gioBatDau)} - ${formatTimeDisplay(shift.gioKetThuc)}) không?`,
    type: isTurningOff ? 'warning' : 'question',
    confirmText: isTurningOff ? 'Ngừng hoạt động' : 'Kích hoạt',
    cancelText: 'Hủy'
  })

  if (!confirmed) return

  try {
    await api.patch(`/api/ca-lam-viec/${shift.id}/toggle-status`)
    shift.trangThai = isTurningOff ? 0 : 1
    showToast(`${isTurningOff ? 'Ngừng hoạt động' : 'Kích hoạt'} ca làm việc thành công!`, 'success')
  } catch (e) {
    showAlert({
      title: 'Không thể thay đổi trạng thái ca',
      message: e?.response?.data?.message || 'Ca làm việc đang có nhân viên phân lịch làm việc.',
      type: 'error'
    })
  }
}

onMounted(() => {
  loadShifts()
})
</script>

<style scoped>
.quan-ly-ca-page {
  padding: 1.25rem 2rem 3rem;
  background-color: #f7f5ef;
  min-height: calc(100vh - 48px);
  color: #3d4a50;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.content-card {
  background-color: #ffffff;
  border-radius: 10px;
  border: 1px solid var(--line, #e9e5db);
  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.025);
  padding: 1.4rem 1.6rem;
  box-sizing: border-box;
}

.page-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.12);
}

.page-main-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #2c3e50;
}

/* 1. Bộ lọc */
.filter-card-box {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.filter-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: #2c3e50;
}

.filter-icon {
  color: var(--blue, #496883);
}

.filter-grid-fields {
  display: grid;
  grid-template-columns: 1.8fr 1.2fr 1.2fr 1.2fr auto;
  gap: 12px;
  align-items: flex-end;
}

.filter-col {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-label-tiny {
  font-size: 0.82rem;
  font-weight: 700;
  color: #6c787e;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.input-with-prefix-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.prefix-icon {
  position: absolute;
  left: 10px;
  color: var(--blue, #496883);
  pointer-events: none;
}

.form-control-minimal {
  width: 100%;
  height: 38px;
  background-color: #fcfbf9;
  border: 1px solid #dfd8cc;
  border-radius: 8px;
  padding: 0 10px;
  font-size: 0.88rem;
  color: #334155;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-control-minimal:focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.form-control-minimal.with-icon {
  padding-left: 32px;
}

.input-error-border {
  border-color: #ef4444 !important;
  background-color: #fef2f2 !important;
}

.filter-actions-col {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-reset-filter {
  height: 38px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid #d5cbc0;
  background-color: #ffffff;
  color: #5d6c73;
  font-size: 0.84rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-reset-filter:hover {
  background-color: #f4f7f9;
  border-color: #b8c8d2;
}

.btn-primary-beige {
  height: 38px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid var(--blue, #496883);
  background-color: var(--blue, #496883) !important;
  color: #ffffff !important;
  font-size: 0.86rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.2);
}

.btn-primary-beige:hover {
  background-color: #3b576e;
  color: #ffffff;
}

.filter-error-msg {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #dc2626;
  font-size: 0.82rem;
  font-weight: 600;
  margin-top: -4px;
}

/* 2. Bảng */
.minimal-styled-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.minimal-styled-table th {
  background-color: #f7f5ef !important;
  border-bottom: 2px solid #e9e5db !important;
  color: var(--blue, #496883) !important;
  font-weight: 700;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  padding: 10px 14px;
  text-transform: uppercase;
}

.minimal-styled-table th:first-child {
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

.minimal-styled-table th:last-child {
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
}

.table-data-row {
  border-bottom: 1px solid #f3eee7;
  transition: background-color 0.15s;
}

.table-data-row:hover {
  background-color: #faf7f2;
}

.minimal-styled-table td {
  padding: 12px 14px;
  vertical-align: middle;
}

.shift-name-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.shift-title {
  color: #1e293b;
  font-size: 0.95rem;
}

.shift-code-tag {
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  font-size: 0.74rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.shift-desc-hint {
  font-size: 0.82rem;
  color: #8a969b;
  margin-top: 3px;
}

.badge-status {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 0.82rem;
  font-weight: 700;
}

.status-active {
  background-color: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.status-inactive {
  background-color: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.action-buttons-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

/* Switch toggle */
.switch-toggle {
  position: relative;
  display: inline-block;
  width: 36px;
  height: 20px;
  cursor: pointer;
}

.switch-toggle input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider-round {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e1;
  transition: .3s;
  border-radius: 20px;
}

.slider-round:before {
  position: absolute;
  content: "";
  height: 14px;
  width: 14px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .3s;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.switch-toggle input:checked + .slider-round {
  background-color: #10b981;
}

.switch-toggle input:checked + .slider-round:before {
  transform: translateX(16px);
}

.btn-circle-edit {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid #dfd8cc;
  background-color: #ffffff;
  color: var(--blue, #496883);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-circle-edit:hover {
  background-color: #f2eee7;
  border-color: var(--blue, #496883);
}

/* Modal styling */
.modal-backdrop-clean {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 1rem;
}

.modal-dialog-clean {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.modal-header-clean {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.1rem 1.4rem;
  border-bottom: 1px solid #f1ece5;
  background-color: #faf8f5;
}

.modal-title-clean {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #2c3e50;
}

.btn-close-modal {
  border: none;
  background: transparent;
  font-size: 1.2rem;
  color: #8c979d;
  cursor: pointer;
}

.modal-body-clean {
  padding: 1.25rem 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-field-label {
  font-size: 0.84rem;
  font-weight: 700;
  color: #475569;
}

.required-star {
  color: #ef4444;
}

.label-with-counter {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.char-counter {
  font-size: 0.75rem;
  color: #94a3b8;
}

.char-exceeded {
  color: #ef4444 !important;
  font-weight: 700;
}

.form-row-2cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.modal-textarea {
  height: 75px;
  padding: 8px 10px;
  resize: vertical;
}

.error-text-red {
  color: #dc2626;
  font-size: 0.82rem;
  font-weight: 600;
}

.block-error {
  display: block;
  margin-top: -4px;
}

.modal-footer-clean {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 1rem 1.4rem;
  border-top: 1px solid #f1ece5;
  background-color: #faf8f5;
}

.btn-cancel-modal {
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid #d1c8bd;
  background: #ffffff;
  color: #556268;
  font-weight: 600;
  cursor: pointer;
}

.btn-save-modal {
  padding: 8px 22px;
  border-radius: 8px;
  border: 1px solid var(--blue, #496883);
  background: var(--blue, #496883);
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save-modal:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-save-modal:hover:not(:disabled) {
  background: #3b576e;
}

@media (max-width: 900px) {
  .filter-grid-fields {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .filter-grid-fields {
    grid-template-columns: 1fr;
  }
  .form-row-2cols {
    grid-template-columns: 1fr;
  }
}
</style>

