<template>
  <div
    class="select2-wrapper"
    ref="wrapperRef"
    :class="{ 'is-open': isOpen, 'is-disabled': disabled }"
  >
    <!-- 1. Hộp lựa chọn Select2 đóng -->
    <div
      class="select2-selection"
      :class="{
        'has-value': hasSelectedValue,
        'has-focus': isOpen
      }"
      tabindex="0"
      @click="toggleDropdown"
      @keydown.space.prevent="openDropdown"
      @keydown.enter.prevent="openDropdown"
      @keydown.down.prevent="openDropdown"
      @keydown.esc.prevent="closeDropdown"
    >
      <div class="select2-rendered">
        <span v-if="hasSelectedValue" class="select2-selected-text">
          <span
            v-if="selectedColorHex"
            class="select2-color-dot"
            :style="{ backgroundColor: selectedColorHex }"
          ></span>
          {{ selectedLabel }}
        </span>
        <span v-else class="select2-placeholder">{{ placeholder }}</span>
      </div>

      <div class="select2-actions">
        <!-- Nút xóa nhanh (clear) -->
        <button
          v-if="allowClear && hasSelectedValue && !disabled"
          type="button"
          class="select2-clear"
          title="Xóa lựa chọn"
          @click.stop="clearSelection"
        >
          &times;
        </button>

        <!-- Mũi tên chevron Select2 -->
        <span class="select2-arrow" :class="{ 'is-rotated': isOpen }">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- 2. Menu danh sách Select2 mở rộng -->
    <transition name="select2-anim">
      <div v-if="isOpen" class="select2-dropdown" @click.stop>
        <!-- Thanh tìm kiếm Select2 với icon kính lúp -->
        <div v-if="searchable" class="select2-search">
          <span class="select2-search-icon">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </span>
          <input
            ref="searchInputRef"
            type="text"
            v-model="searchQuery"
            class="select2-search-input"
            :placeholder="searchPlaceholder"
            @keydown.down.prevent="navigateOptions(1)"
            @keydown.up.prevent="navigateOptions(-1)"
            @keydown.enter.prevent.stop="handleEnter"
            @keydown.esc.prevent="closeDropdown"
          />
        </div>

        <!-- Danh sách kết quả Select2 -->
        <ul class="select2-results" ref="resultsListRef">
          <!-- Tùy chọn Tất cả / Mặc định khi bật showAllOption -->
          <li
            v-if="showAllOption && !searchQuery.trim()"
            class="select2-option select2-all-option"
            :class="{
              'is-selected': !hasSelectedValue
            }"
            @click="clearSelection"
          >
            <div class="option-content">
              <span class="option-label">{{ allOptionLabel || placeholder || 'Tất cả' }}</span>
            </div>
            <span v-if="!hasSelectedValue" class="option-check">✓</span>
          </li>

          <li
            v-for="(item, idx) in filteredOptions"
            :key="getItemValue(item)"
            class="select2-option"
            :class="{
              'is-selected': isSelected(item),
              'is-highlighted': highlightedIndex === idx
            }"
            @click="selectOption(item)"
            @mouseenter="highlightedIndex = idx"
          >
            <div class="option-content">
              <span
                v-if="item.maHex"
                class="select2-color-dot"
                :style="{ backgroundColor: item.maHex }"
              ></span>
              <span class="option-label">{{ getItemLabel(item) }}</span>
            </div>
            <span v-if="isSelected(item)" class="option-check">✓</span>
          </li>

          <!-- Khi không có kết quả phù hợp -->
          <li v-if="filteredOptions.length === 0 && !canCreateNew" class="select2-no-results">
            Không tìm thấy kết quả phù hợp
          </li>

          <!-- Tùy chọn thêm mới nhanh cho các thuộc tính sản phẩm -->
          <li
            v-if="canCreateNew"
            class="select2-create-option"
            :class="{ 'is-loading': creating }"
            @click="handleCreateNew"
          >
            <span class="create-icon">➕</span>
            <span>Thêm mới: <strong>"{{ searchQuery.trim() }}"</strong></span>
            <span v-if="creating" class="select2-spinner"></span>
          </li>
        </ul>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import api from '../api'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  items: {
    type: Array,
    default: null
  },
  valueKey: {
    type: String,
    default: 'id'
  },
  labelKey: {
    type: String,
    default: ''
  },
  nameKey: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '-- Chọn --'
  },
  searchPlaceholder: {
    type: String,
    default: 'Tìm kiếm...'
  },
  allowClear: {
    type: Boolean,
    default: true
  },
  searchable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  allowCreate: {
    type: Boolean,
    default: false
  },
  showAllOption: {
    type: Boolean,
    default: false
  },
  allOptionLabel: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'created', 'toast'])

const wrapperRef = ref(null)
const searchInputRef = ref(null)
const resultsListRef = ref(null)

const isOpen = ref(false)
const searchQuery = ref('')
const highlightedIndex = ref(-1)
const creating = ref(false)

// Danh sách tùy chọn chuẩn hóa (hỗ trợ cả options hoặc items)
const normalizedOptions = computed(() => {
  if (Array.isArray(props.options) && props.options.length > 0) return props.options
  if (Array.isArray(props.items)) return props.items
  return props.options || []
})

const getItemValue = (item) => {
  if (item === null || item === undefined) return ''
  if (typeof item === 'object') {
    return item[props.valueKey] !== undefined ? item[props.valueKey] : item.id !== undefined ? item.id : item.value
  }
  return item
}

const getItemLabel = (item) => {
  if (item === null || item === undefined) return ''
  if (typeof item === 'object') {
    if (props.labelKey && item[props.labelKey] !== undefined) return String(item[props.labelKey])
    if (props.nameKey && item[props.nameKey] !== undefined) return String(item[props.nameKey])
    if (item.label !== undefined) return String(item.label)
    if (item.name !== undefined) return String(item.name)
    if (item.ten !== undefined) return String(item.ten)
    if (item.tenDanhMuc !== undefined) return String(item.tenDanhMuc)
    if (item.tenThuongHieu !== undefined) return String(item.tenThuongHieu)
    if (item.tenChatLieu !== undefined) return String(item.tenChatLieu)
    if (item.tenCoAo !== undefined) return String(item.tenCoAo)
    if (item.tenTayAo !== undefined) return String(item.tenTayAo)
    if (item.tenXuatXu !== undefined) return String(item.tenXuatXu)
    if (item.tenHoaTiet !== undefined) return String(item.tenHoaTiet)
    if (item.tenMauSac !== undefined) return String(item.tenMauSac)
    if (item.tenKichCo !== undefined) return String(item.tenKichCo)
    return String(item[props.valueKey] || '')
  }
  return String(item)
}

const isSelected = (item) => {
  const val = getItemValue(item)
  return String(val) === String(props.modelValue) && props.modelValue !== '' && props.modelValue !== null && props.modelValue !== undefined
}

const hasSelectedValue = computed(() => {
  return props.modelValue !== '' && props.modelValue !== null && props.modelValue !== undefined
})

const selectedOption = computed(() => {
  if (!hasSelectedValue.value) return null
  return normalizedOptions.value.find(item => String(getItemValue(item)) === String(props.modelValue)) || null
})

const selectedLabel = computed(() => {
  if (selectedOption.value) {
    return getItemLabel(selectedOption.value)
  }
  return ''
})

const selectedColorHex = computed(() => {
  return selectedOption.value?.maHex || null
})

// Chuẩn hóa tiếng Việt để tìm kiếm không dấu
const removeVietnameseTones = (str) => {
  if (!str) return ''
  return String(str)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
}

// Lọc kết quả tìm kiếm theo Select2
const filteredOptions = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return normalizedOptions.value

  const qNoTone = removeVietnameseTones(q)
  return normalizedOptions.value.filter(item => {
    const label = getItemLabel(item).toLowerCase()
    const labelNoTone = removeVietnameseTones(label)
    return label.includes(q) || labelNoTone.includes(qNoTone)
  })
})

const canCreateNew = computed(() => {
  if (!props.allowCreate || !props.type) return false
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return false
  const exactMatch = normalizedOptions.value.some(item => getItemLabel(item).trim().toLowerCase() === q)
  return !exactMatch
})

const openDropdown = () => {
  if (props.disabled) return
  isOpen.value = true
  searchQuery.value = ''
  highlightedIndex.value = -1

  nextTick(() => {
    if (props.searchable && searchInputRef.value) {
      searchInputRef.value.focus()
    }
  })
}

const closeDropdown = () => {
  isOpen.value = false
  searchQuery.value = ''
  highlightedIndex.value = -1
}

const toggleDropdown = () => {
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

const selectOption = (item) => {
  const val = getItemValue(item)
  emit('update:modelValue', val)
  emit('change', item)
  closeDropdown()
}

const clearSelection = () => {
  emit('update:modelValue', '')
  emit('change', null)
  closeDropdown()
}

// Xử lý tạo mới thuộc tính
const handleCreateNew = async () => {
  const ten = searchQuery.value.trim()
  if (!ten || creating.value || !props.type) return

  creating.value = true
  try {
    const res = await api.post(`/api/thuoc-tinh/type/${props.type}`, {
      ten,
      trangThai: 1
    })
    const createdItem = res.data

    emit('created', createdItem)
    emit('update:modelValue', createdItem.id)
    emit('change', createdItem)
    emit('toast', {
      message: `Đã thêm mới và chọn ${props.label ? props.label.toLowerCase() : 'thuộc tính'} "${ten}"!`,
      type: 'success'
    })
    closeDropdown()
  } catch (err) {
    const msg = err.response?.data?.message || `Không thể thêm mới ${props.label ? props.label.toLowerCase() : 'thuộc tính'}!`
    emit('toast', { message: msg, type: 'error' })
  } finally {
    creating.value = false
  }
}

const handleEnter = () => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < filteredOptions.value.length) {
    selectOption(filteredOptions.value[highlightedIndex.value])
  } else if (canCreateNew.value) {
    handleCreateNew()
  }
}

const navigateOptions = (step) => {
  const total = filteredOptions.value.length
  if (total === 0) return

  highlightedIndex.value = (highlightedIndex.value + step + total) % total
  nextTick(() => {
    const list = resultsListRef.value
    if (list) {
      const activeEl = list.children[highlightedIndex.value]
      if (activeEl) {
        activeEl.scrollIntoView({ block: 'nearest' })
      }
    }
  })
}

// Lắng nghe click bên ngoài để đóng dropdown
const handleClickOutside = (e) => {
  if (wrapperRef.value && !wrapperRef.value.contains(e.target)) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* Khung chứa Select2 */
.select2-wrapper {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  font-family: inherit;
  user-select: none;
}

.select2-wrapper.is-open {
  z-index: 100;
}

.select2-wrapper.is-disabled {
  opacity: 0.65;
  pointer-events: none;
}

/* Hộp lựa chọn chính khi đóng */
.select2-selection {
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background-color: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  cursor: pointer;
  box-sizing: border-box;
  transition: all 0.15s ease;
  outline: none;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.select2-selection:hover {
  border-color: #3e5c76;
  background-color: #fbf9f6;
}

.select2-selection.has-focus,
.select2-wrapper.is-open .select2-selection {
  border-color: #3e5c76;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(62, 92, 118, 0.15);
}

.select2-rendered {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.85rem;
  line-height: 1.4;
  text-align: left;
}

.select2-selected-text {
  color: #2b353b;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.select2-placeholder {
  color: #8c969e;
  font-weight: 400;
}

.select2-color-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
  border: 1px solid rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}

.select2-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
  flex-shrink: 0;
}

.select2-clear {
  border: none;
  background: none;
  font-size: 16px;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
  padding: 0 2px;
  transition: color 0.15s ease;
}

.select2-clear:hover {
  color: #ef4444;
}

.select2-arrow {
  color: #647074;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;
}

.select2-arrow.is-rotated {
  transform: rotate(180deg);
}

/* Dropdown đổ xuống */
.select2-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background-color: #ffffff;
  border: 1.5px solid #d5c8b8;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 1050;
  overflow: hidden;
  box-sizing: border-box;
}

/* Ô tìm kiếm Select2 */
.select2-search {
  position: relative;
  padding: 8px 10px;
  background-color: #faf8f5;
  border-bottom: 1px solid #ebd9c8;
  display: flex;
  align-items: center;
}

.select2-search-icon {
  position: absolute;
  left: 18px;
  color: #8c969e;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.select2-search-input {
  width: 100%;
  height: 32px;
  padding: 0 10px 0 32px;
  border: 1px solid #d5c8b8;
  border-radius: 6px;
  font-size: 0.82rem;
  color: #2b353b;
  background-color: #ffffff;
  outline: none;
  box-sizing: border-box;
  transition: all 0.15s ease;
}

.select2-search-input:focus {
  border-color: #3e5c76;
  box-shadow: 0 0 0 2px rgba(62, 92, 118, 0.15);
}

/* Danh sách kết quả */
.select2-results {
  list-style: none;
  margin: 0;
  padding: 4px 0;
  max-height: 220px;
  overflow-y: auto;
}

.select2-results::-webkit-scrollbar {
  width: 6px;
}

.select2-results::-webkit-scrollbar-thumb {
  background-color: #d5c8b8;
  border-radius: 3px;
}

.select2-option {
  padding: 8px 12px;
  font-size: 0.84rem;
  color: #2b353b;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: background-color 0.12s ease;
  line-height: 1.4;
}

.select2-option:hover,
.select2-option.is-highlighted {
  background-color: #f7f5ef;
  color: #3e5c76;
  font-weight: 600;
}

.select2-option.is-selected {
  background-color: #edf4f7;
  color: #3e5c76;
  font-weight: 700;
}

.select2-all-option {
  font-weight: 600;
  border-bottom: 1px dashed #ebd9c8;
}

.option-content {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-check {
  color: #3e5c76;
  font-weight: 700;
  font-size: 0.85rem;
}

.select2-no-results {
  padding: 12px;
  text-align: center;
  color: #8c969e;
  font-size: 0.82rem;
  font-style: italic;
}

.select2-create-option {
  padding: 9px 12px;
  font-size: 0.82rem;
  color: #0284c7;
  background-color: #f0f9ff;
  border-top: 1px dashed #bae6fd;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: background 0.15s ease;
}

.select2-create-option:hover {
  background-color: #e0f2fe;
  color: #0369a1;
}

.create-icon {
  font-size: 0.85rem;
}

.select2-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid #0284c7;
  border-top-color: transparent;
  border-radius: 50%;
  animation: select2Spin 0.6s linear infinite;
  margin-left: auto;
}

@keyframes select2Spin {
  to {
    transform: rotate(360deg);
  }
}

/* Animation mở đóng */
.select2-anim-enter-active,
.select2-anim-leave-active {
  transition: all 0.15s ease-out;
}

.select2-anim-enter-from,
.select2-anim-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
