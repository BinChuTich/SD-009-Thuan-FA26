<template>
  <div
    class="select2-container"
    :class="[customClass, { 'is-open': isOpen, 'is-disabled': disabled }]"
    ref="containerRef"
  >
    <!-- Thanh hiển thị lựa chọn hiện tại -->
    <div
      class="select2-selection"
      :class="{ 'has-value': isNotEmpty, 'has-focus': isOpen }"
      @click="toggleDropdown"
      tabindex="0"
      @keydown.down.prevent="onKeyDown"
      @keydown.up.prevent="onKeyUp"
      @keydown.enter.prevent="onKeyEnter"
      @keydown.esc="closeDropdown"
    >
      <div class="select2-selection-rendered" :title="displayLabel">
        <span v-if="isNotEmpty" class="selected-text">{{ displayLabel }}</span>
        <span v-else class="placeholder-text">{{ placeholder || 'Chọn...' }}</span>
      </div>

      <div class="select2-actions">
        <!-- Nút xóa nhanh nếu allowClear và có giá trị -->
        <button
          v-if="allowClear && isNotEmpty && !disabled && canClear"
          type="button"
          class="select2-clear-btn"
          @click.stop="clearSelection"
          title="Xóa lựa chọn"
        >
          &times;
        </button>

        <!-- Mũi tên dropdown -->
        <span class="select2-arrow" :class="{ 'arrow-up': isOpen }">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- Dropdown chứa ô tìm kiếm (nhập được) và danh sách kết quả (chọn được) -->
    <transition name="select2-dropdown-fade">
      <div v-if="isOpen" class="select2-dropdown" @click.stop>
        <!-- Ô nhập tìm kiếm chuẩn phong cách Select2 -->
        <div class="select2-search-box">
          <svg class="search-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            ref="searchInputRef"
            type="text"
            v-model="searchQuery"
            class="select2-search-input"
            :placeholder="searchPlaceholder || 'Nhập để tìm kiếm...'"
            @keydown.down.prevent="onKeyDown"
            @keydown.up.prevent="onKeyUp"
            @keydown.enter.prevent="onKeyEnter"
            @keydown.esc="closeDropdown"
          />
          <button
            v-if="searchQuery"
            type="button"
            class="search-clear-btn"
            @click.stop="searchQuery = ''"
          >
            &times;
          </button>
        </div>

        <!-- Danh sách options có thể scroll -->
        <ul class="select2-results" ref="resultsListRef">
          <li
            v-for="(item, idx) in normalizedFilteredOptions"
            :key="String(item.value)"
            class="select2-result-item"
            :class="{
              'is-selected': isSelected(item.value),
              'is-highlighted': highlightedIndex === idx
            }"
            @mouseenter="highlightedIndex = idx"
            @click.stop="selectItem(item)"
          >
            <span class="item-label">{{ item.label }}</span>
            <svg v-if="isSelected(item.value)" class="check-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </li>
          <li v-if="normalizedFilteredOptions.length === 0" class="select2-no-results">
            Không tìm thấy lựa chọn phù hợp!
          </li>
        </ul>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: 'Chọn...'
  },
  searchPlaceholder: {
    type: String,
    default: 'Nhập để tìm kiếm...'
  },
  allowClear: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  customClass: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const searchQuery = ref('')
const highlightedIndex = ref(-1)

const containerRef = ref(null)
const searchInputRef = ref(null)
const resultsListRef = ref(null)

// Chuẩn hóa danh sách options sang format { value, label }
const normalizedOptions = computed(() => {
  return props.options.map(opt => {
    if (typeof opt === 'object' && opt !== null) {
      return {
        value: opt.value !== undefined ? opt.value : '',
        label: opt.label !== undefined ? String(opt.label) : String(opt.value || '')
      }
    }
    return {
      value: opt,
      label: String(opt)
    }
  })
})

// Lọc options dựa trên từ khóa tìm kiếm mà người dùng nhập
const normalizedFilteredOptions = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return normalizedOptions.value
  return normalizedOptions.value.filter(item =>
    item.label.toLowerCase().includes(q)
  )
})

// Kiểm tra xem hiện tại có giá trị được chọn hay không
const isNotEmpty = computed(() => {
  return props.modelValue !== null && props.modelValue !== undefined && props.modelValue !== ''
})

// Kiểm tra xem có thể xóa lựa chọn không (khi giá trị khác rỗng)
const canClear = computed(() => {
  return props.modelValue !== '' && props.modelValue !== null && props.modelValue !== undefined
})

// Nhãn hiển thị cho giá trị đang được chọn
const displayLabel = computed(() => {
  const found = normalizedOptions.value.find(o => String(o.value) === String(props.modelValue))
  if (found) return found.label
  if (props.modelValue !== null && props.modelValue !== undefined && props.modelValue !== '') {
    return String(props.modelValue)
  }
  const defaultOption = normalizedOptions.value.find(o => o.value === '')
  return defaultOption ? defaultOption.label : props.placeholder
})

const isSelected = (val) => {
  return String(props.modelValue) === String(val)
}

const toggleDropdown = () => {
  if (props.disabled) return
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

const openDropdown = () => {
  if (props.disabled) return
  isOpen.value = true
  searchQuery.value = ''
  
  // Đặt vị trí highlight ở phần tử đang được chọn
  const curIdx = normalizedFilteredOptions.value.findIndex(o => isSelected(o.value))
  highlightedIndex.value = curIdx >= 0 ? curIdx : 0

  nextTick(() => {
    if (searchInputRef.value) {
      searchInputRef.value.focus()
    }
  })
}

const closeDropdown = () => {
  isOpen.value = false
  searchQuery.value = ''
}

const selectItem = (item) => {
  emit('update:modelValue', item.value)
  emit('change', item.value)
  closeDropdown()
}

const clearSelection = () => {
  // Tìm option mặc định rỗng '' nếu có (ví dụ 'Tất cả trạng thái' có value = '')
  const emptyOption = normalizedOptions.value.find(o => o.value === '')
  const defaultVal = emptyOption ? emptyOption.value : ''
  emit('update:modelValue', defaultVal)
  emit('change', defaultVal)
  closeDropdown()
}

// Điều hướng bàn phím (Arrow Up, Down, Enter)
const onKeyDown = () => {
  if (!isOpen.value) {
    openDropdown()
    return
  }
  const max = normalizedFilteredOptions.value.length - 1
  if (highlightedIndex.value < max) {
    highlightedIndex.value++
    scrollToHighlighted()
  }
}

const onKeyUp = () => {
  if (!isOpen.value) {
    openDropdown()
    return
  }
  if (highlightedIndex.value > 0) {
    highlightedIndex.value--
    scrollToHighlighted()
  }
}

const onKeyEnter = () => {
  if (!isOpen.value) {
    openDropdown()
    return
  }
  if (
    highlightedIndex.value >= 0 &&
    highlightedIndex.value < normalizedFilteredOptions.value.length
  ) {
    selectItem(normalizedFilteredOptions.value[highlightedIndex.value])
  }
}

const scrollToHighlighted = () => {
  nextTick(() => {
    if (!resultsListRef.value) return
    const el = resultsListRef.value.children[highlightedIndex.value]
    if (el) {
      el.scrollIntoView({ block: 'nearest' })
    }
  })
}

// Xử lý click ra ngoài để đóng dropdown
const handleClickOutside = (e) => {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
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
.select2-container {
  position: relative;
  display: inline-block;
  width: 100%;
  font-family: inherit;
  font-size: 13.5px;
  user-select: none;
}

.select2-container.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
}

/* Khung hiển thị */
.select2-selection {
  min-height: 38px;
  height: 38px;
  background-color: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0 10px 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  box-sizing: border-box;
}

.select2-selection:hover {
  border-color: #9ca3af;
}

.select2-selection.has-focus,
.select2-selection:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.select2-selection-rendered {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #1f2937;
  font-weight: 500;
  text-align: left;
}

.placeholder-text {
  color: #9ca3af;
  font-weight: normal;
}

.selected-text {
  color: #1f2937;
}

/* Cụm nút hành động bên phải */
.select2-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 6px;
  flex-shrink: 0;
}

.select2-clear-btn {
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 16px;
  line-height: 1;
  padding: 0 2px;
  cursor: pointer;
  transition: color 0.15s;
}

.select2-clear-btn:hover {
  color: #ef4444;
}

.select2-arrow {
  display: flex;
  align-items: center;
  color: #6b7280;
  transition: transform 0.2s ease;
}

.select2-arrow.arrow-up {
  transform: rotate(180deg);
}

/* Dropdown menu */
.select2-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  width: 100%;
  min-width: 160px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08);
  z-index: 9999;
  overflow: hidden;
  box-sizing: border-box;
}

/* Ô tìm kiếm bên trong dropdown */
.select2-search-box {
  padding: 7px 9px;
  background-color: #f9fafb;
  border-bottom: 1px solid #f3f4f6;
  position: relative;
  display: flex;
  align-items: center;
}

.select2-search-box .search-icon {
  position: absolute;
  left: 17px;
  pointer-events: none;
}

.select2-search-input {
  width: 100%;
  height: 32px;
  padding: 0 26px 0 28px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: #ffffff;
  font-size: 13px;
  color: #1f2937;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.select2-search-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.search-clear-btn {
  position: absolute;
  right: 15px;
  background: none;
  border: none;
  color: #9ca3af;
  font-size: 15px;
  cursor: pointer;
}

.search-clear-btn:hover {
  color: #4b5563;
}

/* Danh sách kết quả */
.select2-results {
  list-style: none;
  margin: 0;
  padding: 4px;
  max-height: 210px;
  overflow-y: auto;
}

.select2-result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 5px;
  font-size: 13.5px;
  color: #374151;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
}

.select2-result-item.is-highlighted {
  background-color: #f3f4f6;
  color: #111827;
}

.select2-result-item.is-selected {
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.select2-result-item.is-selected.is-highlighted {
  background-color: #dbeafe;
}

.check-icon {
  flex-shrink: 0;
  margin-left: 6px;
}

.select2-no-results {
  padding: 12px 10px;
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
  font-style: italic;
}

/* Hiệu ứng mờ dần khi mở/đóng dropdown */
.select2-dropdown-fade-enter-active,
.select2-dropdown-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.select2-dropdown-fade-enter-from,
.select2-dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
