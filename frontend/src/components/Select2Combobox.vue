<template>
  <div class="select2-wrapper" ref="wrapperRef" :class="{ 'is-open': isOpen, 'is-disabled': disabled }">
    <!-- Trigger Button hiển thị giá trị đã chọn -->
    <div
      class="select2-trigger"
      :class="{
        'trigger-focus': isOpen,
        'has-error': hasError,
        'disabled': disabled
      }"
      @click="toggleDropdown"
      tabindex="0"
      @keydown.down.prevent="openAndFocusSearch"
      @keydown.enter.prevent="toggleDropdown"
      @keydown.esc.prevent="closeDropdown"
    >
      <span class="select2-display-text" :class="{ 'is-placeholder': !selectedOption || isPlaceholderText }">
        {{ displayText }}
      </span>

      <div class="select2-suffix-actions">
        <!-- Nút xóa nhanh khi allowClear = true -->
        <button
          v-if="allowClear && hasSelectedValue && !disabled"
          type="button"
          class="select2-clear-btn"
          title="Xóa lựa chọn"
          @click.stop="clearSelection"
        >
          &times;
        </button>

        <span class="select2-arrow" :class="{ 'arrow-up': isOpen }">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- Dropdown Panel chứa Ô tìm kiếm & Danh sách lựa chọn (chuẩn Select2) -->
    <transition name="select2-anim">
      <div v-if="isOpen" class="select2-dropdown" @click.stop>
        <!-- Ô Input tìm kiếm ở đầu dropdown -->
        <div v-if="searchable" class="select2-search-box">
          <input
            ref="searchInputRef"
            type="text"
            v-model="searchQuery"
            :placeholder="searchPlaceholder || 'Gõ để tìm kiếm...'"
            class="select2-search-input"
            @click.stop
            @keydown.down.prevent="navigateOption(1)"
            @keydown.up.prevent="navigateOption(-1)"
            @keydown.enter.prevent="selectHighlighted"
            @keydown.esc.prevent="closeDropdown"
          />
        </div>

        <!-- Danh sách các Options -->
        <div class="select2-options-list" ref="listRef">
          <div
            v-for="(item, idx) in filteredOptions"
            :key="item.value + '_' + idx"
            class="select2-option-item"
            :class="{
              'is-selected': isSelected(item.value),
              'is-highlighted': highlightedIndex === idx
            }"
            @click="selectOption(item)"
            @mouseenter="highlightedIndex = idx"
          >
            <span class="option-label">{{ item.label }}</span>
            <span v-if="isSelected(item.value)" class="option-check">✓</span>
          </div>

          <!-- Nút thêm nhanh khi allowCreate = true và không trùng chính xác -->
          <div
            v-if="allowCreate && searchQuery.trim() && !hasExactMatch"
            class="select2-option-item select2-create-option"
            :class="{ 'is-highlighted': highlightedIndex === filteredOptions.length }"
            @click="handleCreateOption"
            @mouseenter="highlightedIndex = filteredOptions.length"
          >
            <span class="option-create-text">
              <span class="plus-icon-tag">+</span> Tạo mới "<strong>{{ searchQuery.trim() }}</strong>"
            </span>
          </div>

          <div v-if="filteredOptions.length === 0 && (!allowCreate || !searchQuery.trim())" class="select2-empty-state">
            Không tìm thấy kết quả phù hợp
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import api from '@/api'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  valueKey: {
    type: String,
    default: ''
  },
  labelKey: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: 'Chọn giá trị...'
  },
  searchPlaceholder: {
    type: String,
    default: ''
  },
  allLabel: {
    type: String,
    default: ''
  },
  showAllOption: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  allowClear: {
    type: Boolean,
    default: false
  },
  hasError: {
    type: Boolean,
    default: false
  },
  searchable: {
    type: Boolean,
    default: true
  },
  allowCreate: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'created', 'toast'])

const wrapperRef = ref(null)
const searchInputRef = ref(null)
const listRef = ref(null)
const isOpen = ref(false)
const searchQuery = ref('')
const highlightedIndex = ref(-1)

// Chuẩn hóa mảng options sang dạng { label, value }
const normalizedOptions = computed(() => {
  if (!Array.isArray(props.options)) return []

  const list = []

  // Nếu có allLabel hoặc showAllOption được yêu cầu, kiểm tra xem đã có mục giá trị rỗng chưa
  const hasEmptyVal = props.options.some(opt => {
    if (typeof opt === 'object' && opt !== null) {
      const v = props.valueKey && opt[props.valueKey] !== undefined ? opt[props.valueKey] : (opt.value !== undefined ? opt.value : opt.id)
      return v === '' || v === null || v === undefined
    }
    return opt === '' || opt === null || opt === undefined
  })

  if (props.allLabel && !hasEmptyVal) {
    list.push({ label: props.allLabel, value: '' })
  }

  props.options.forEach(opt => {
    if (typeof opt === 'object' && opt !== null) {
      // 1. Tìm nhãn
      let label = ''
      if (props.labelKey && opt[props.labelKey] !== undefined) {
        label = String(opt[props.labelKey])
      } else if (opt.label !== undefined) {
        label = String(opt.label)
      } else if (opt.name !== undefined) {
        label = String(opt.name)
      } else if (opt.ten !== undefined) {
        label = String(opt.ten)
      } else if (opt.tenDanhMuc !== undefined) {
        label = String(opt.tenDanhMuc)
      } else if (opt.tenThuongHieu !== undefined) {
        label = String(opt.tenThuongHieu)
      } else if (opt.tenChatLieu !== undefined) {
        label = String(opt.tenChatLieu)
      } else if (opt.tenCoAo !== undefined) {
        label = String(opt.tenCoAo)
      } else if (opt.tenTayAo !== undefined) {
        label = String(opt.tenTayAo)
      } else if (opt.tenXuatXu !== undefined) {
        label = String(opt.tenXuatXu)
      } else if (opt.tenHoaTiet !== undefined) {
        label = String(opt.tenHoaTiet)
      } else if (opt.tenMauSac !== undefined) {
        label = String(opt.tenMauSac)
      } else if (opt.tenKichCo !== undefined) {
        label = String(opt.tenKichCo)
      } else if (opt.tenVaiTro !== undefined) {
        label = String(opt.tenVaiTro)
      } else if (opt.title !== undefined) {
        label = String(opt.title)
      } else if (opt.text !== undefined) {
        label = String(opt.text)
      } else if (opt.value !== undefined) {
        label = String(opt.value)
      }

      // 2. Tìm giá trị
      let value = ''
      if (props.valueKey && opt[props.valueKey] !== undefined) {
        value = opt[props.valueKey]
      } else if (opt.value !== undefined) {
        value = opt.value
      } else if (opt.id !== undefined) {
        value = opt.id
      } else if (opt.code !== undefined) {
        value = opt.code
      } else if (opt.ma !== undefined) {
        value = opt.ma
      } else {
        value = label
      }

      list.push({ label: label || String(value), value })
    } else {
      list.push({
        label: String(opt),
        value: opt
      })
    }
  })

  return list
})

// Kiểm tra giá trị đã chọn
const hasSelectedValue = computed(() => {
  return props.modelValue !== null && props.modelValue !== undefined && props.modelValue !== ''
})

// Tìm option đang được chọn
const selectedOption = computed(() => {
  if (!hasSelectedValue.value) {
    // Nếu rỗng, ưu tiên tìm option có giá trị rỗng (như 'Tất cả')
    return normalizedOptions.value.find(opt => opt.value === '' || opt.value === null || opt.value === undefined) || null
  }
  return normalizedOptions.value.find(opt => isSelected(opt.value)) || null
})

const isPlaceholderText = computed(() => {
  return !selectedOption.value || (!hasSelectedValue.value && !selectedOption.value.label)
})

const displayText = computed(() => {
  if (selectedOption.value) {
    return selectedOption.value.label
  }
  return props.placeholder || 'Chọn giá trị...'
})

const isSelected = (val) => {
  if (props.modelValue === null || props.modelValue === undefined || props.modelValue === '') {
    return val === '' || val === null || val === undefined
  }
  return String(props.modelValue) === String(val)
}

// Hàm chuẩn hóa chuỗi không dấu để tìm kiếm tiếng Việt tốt nhất
const normalizeText = (text) => {
  if (!text) return ''
  return text
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .trim()
}

// Lọc options theo từ khóa tìm kiếm
const filteredOptions = computed(() => {
  const query = normalizeText(searchQuery.value)
  if (!query) return normalizedOptions.value
  return normalizedOptions.value.filter(opt => {
    return normalizeText(opt.label).includes(query)
  })
})

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
  highlightedIndex.value = -1

  nextTick(() => {
    if (props.searchable && searchInputRef.value) {
      searchInputRef.value.focus()
    }
    // Cuộn tới mục đang chọn nếu có
    const selectedIdx = filteredOptions.value.findIndex(o => isSelected(o.value))
    if (selectedIdx >= 0) {
      highlightedIndex.value = selectedIdx
      scrollToHighlighted()
    }
  })
}

const openAndFocusSearch = () => {
  if (!isOpen.value) {
    openDropdown()
  }
}

const closeDropdown = () => {
  isOpen.value = false
  searchQuery.value = ''
  highlightedIndex.value = -1
}

const selectOption = (item) => {
  emit('update:modelValue', item.value)
  emit('change', item.value)
  closeDropdown()
}

const clearSelection = (e) => {
  e?.stopPropagation()
  emit('update:modelValue', '')
  emit('change', '')
  closeDropdown()
}

const hasExactMatch = computed(() => {
  const q = normalizeText(searchQuery.value)
  if (!q) return false
  return normalizedOptions.value.some(o => normalizeText(o.label) === q)
})

const handleCreateOption = async () => {
  const text = searchQuery.value.trim()
  if (!text) return
  if (props.type) {
    try {
      const res = await api.post(`/api/thuoc-tinh/type/${props.type}`, {
        ten: text,
        trangThai: 1
      })
      const createdItem = res.data
      emit('created', createdItem)
      emit('update:modelValue', createdItem.id)
      emit('change', createdItem.id)
      emit('toast', { message: `Đã thêm mới và chọn "${text}"!`, type: 'success' })
      closeDropdown()
      return
    } catch (err) {
      console.error('Lỗi thêm nhanh thuộc tính:', err)
      emit('toast', { message: err.response?.data?.message || 'Không thể tạo mới thuộc tính!', type: 'error' })
    }
  }
  emit('created', { id: text, name: text, label: text })
  emit('update:modelValue', text)
  emit('change', text)
  closeDropdown()
}

const selectHighlighted = () => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < filteredOptions.value.length) {
    selectOption(filteredOptions.value[highlightedIndex.value])
  } else if (props.allowCreate && highlightedIndex.value === filteredOptions.value.length) {
    handleCreateOption()
  } else if (filteredOptions.value.length === 1) {
    selectOption(filteredOptions.value[0])
  } else if (props.allowCreate && searchQuery.value.trim() && !hasExactMatch.value) {
    handleCreateOption()
  }
}

const navigateOption = (step) => {
  const len = filteredOptions.value.length
  if (len === 0) return
  highlightedIndex.value = (highlightedIndex.value + step + len) % len
  scrollToHighlighted()
}

const scrollToHighlighted = () => {
  nextTick(() => {
    if (!listRef.value) return
    const el = listRef.value.children[highlightedIndex.value]
    if (el) {
      el.scrollIntoView({ block: 'nearest' })
    }
  })
}

// Bắt sự kiện click ra ngoài để đóng dropdown
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
.select2-wrapper {
  position: relative;
  width: 100%;
  font-family: inherit;
}

.select2-wrapper.is-open {
  z-index: 1050;
}

.select2-trigger {
  width: 100%;
  height: 2.6rem;
  border: 1px solid var(--line, #e9e5db);
  border-radius: 8px;
  padding: 0 0.85rem;
  font-size: 0.92rem;
  color: var(--text, #3d4a50);
  background-color: #fcfbf8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  user-select: none;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.select2-trigger:hover:not(.disabled) {
  border-color: #b8c3cc;
  background-color: #ffffff;
}

.select2-trigger.trigger-focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.select2-trigger.has-error {
  border-color: #ef4444 !important;
  background-color: #fffbfb !important;
}

.select2-trigger.disabled {
  background-color: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
  opacity: 0.75;
}

.select2-display-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.select2-display-text.is-placeholder {
  color: #94a3b8;
}

.select2-suffix-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.select2-clear-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.15s;
}

.select2-clear-btn:hover {
  color: #ef4444;
  background-color: #fee2e2;
}

.select2-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  transition: transform 0.2s ease;
}

.select2-arrow.arrow-up {
  transform: rotate(180deg);
  color: var(--blue, #496883);
}

/* Dropdown Container */
.select2-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  min-width: 100%;
  background: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  padding: 8px;
  z-index: 1050;
  box-sizing: border-box;
}

/* Ô input tìm kiếm bên trong dropdown */
.select2-search-box {
  margin-bottom: 6px;
}

.select2-search-input {
  width: 100%;
  height: 34px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0 10px;
  font-size: 13.5px;
  color: #1e293b;
  background: #ffffff;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.select2-search-input:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.15);
}

/* Danh sách Options */
.select2-options-list {
  max-height: 210px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

/* Tùy chỉnh thanh cuộn đẹp mắt */
.select2-options-list::-webkit-scrollbar {
  width: 6px;
}

.select2-options-list::-webkit-scrollbar-track {
  background: transparent;
}

.select2-options-list::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 10px;
}

.select2-options-list::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}

/* Từng Option */
.select2-option-item {
  padding: 7px 10px;
  font-size: 13.5px;
  color: #334155;
  border-radius: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: background-color 0.12s;
  user-select: none;
}

.select2-option-item:hover,
.select2-option-item.is-highlighted {
  background-color: #f1f5f9;
  color: #0f172a;
}

.select2-option-item.is-selected {
  background-color: #f1f5f9;
  font-weight: 600;
  color: var(--blue, #496883);
}

.option-check {
  font-size: 12px;
  font-weight: 700;
  color: var(--blue, #496883);
}

.select2-empty-state {
  padding: 12px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
  font-style: italic;
}

/* Hiệu ứng dropdown */
.select2-anim-enter-active,
.select2-anim-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.select2-anim-enter-from,
.select2-anim-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Nút thêm mới thuộc tính */
.select2-create-option {
  color: var(--blue, #496883) !important;
  font-weight: 600;
  border-top: 1px dashed #e2e8f0;
  margin-top: 4px;
  background-color: #f8fafc;
}

.select2-create-option:hover,
.select2-create-option.is-highlighted {
  background-color: #eaf1f5 !important;
  color: #2c4350 !important;
}

.plus-icon-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 13px;
  line-height: 1;
  margin-right: 6px;
}
</style>

