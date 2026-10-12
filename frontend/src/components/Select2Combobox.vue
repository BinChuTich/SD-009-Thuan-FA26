<template>
  <div class="select2-wrapper" ref="wrapperRef" :class="{ 'is-open': isOpen, 'is-disabled': disabled }">
    <!-- Trigger Button hiển thị giá trị đã chọn -->
    <div
      class="select2-trigger"
      :class="{ 'trigger-focus': isOpen }"
      @click="toggleDropdown"
      tabindex="0"
      @keydown.down.prevent="openAndFocusSearch"
      @keydown.enter.prevent="toggleDropdown"
      @keydown.esc.prevent="closeDropdown"
    >
      <span class="select2-display-text" :class="{ 'is-placeholder': !selectedOption }">
        {{ displayText }}
      </span>
      <span class="select2-arrow" :class="{ 'arrow-up': isOpen }">
        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </span>
    </div>

    <!-- Dropdown Panel chứa Ô tìm kiếm & Danh sách lựa chọn (chuẩn Select2) -->
    <transition name="select2-anim">
      <div v-if="isOpen" class="select2-dropdown" @click.stop>
        <!-- Ô Input tìm kiếm ở đầu dropdown -->
        <div class="select2-search-box">
          <input
            ref="searchInputRef"
            type="text"
            v-model="searchQuery"
            :placeholder="searchPlaceholder"
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
            :key="item.value"
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

          <div v-if="filteredOptions.length === 0" class="select2-empty-state">
            Không tìm thấy kết quả
          </div>
        </div>
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
    required: true
    // Mảng có dạng: [{ label: 'Tất cả...', value: '' }, ...] hoặc ['A', 'B']
  },
  placeholder: {
    type: String,
    default: 'Chọn giá trị...'
  },
  searchPlaceholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const wrapperRef = ref(null)
const searchInputRef = ref(null)
const listRef = ref(null)
const isOpen = ref(false)
const searchQuery = ref('')
const highlightedIndex = ref(-1)

// Chuẩn hóa mảng options sang dạng { label, value }
const normalizedOptions = computed(() => {
  if (!Array.isArray(props.options)) return []
  return props.options.map(opt => {
    if (typeof opt === 'object' && opt !== null) {
      return {
        label: opt.label !== undefined ? String(opt.label) : String(opt.value ?? ''),
        value: opt.value !== undefined ? opt.value : opt.label
      }
    }
    return {
      label: String(opt),
      value: opt
    }
  })
})

// Tìm option đang được chọn
const selectedOption = computed(() => {
  return normalizedOptions.value.find(opt => isSelected(opt.value))
})

const displayText = computed(() => {
  if (selectedOption.value) {
    return selectedOption.value.label
  }
  return props.placeholder
})

const isSelected = (val) => {
  return String(props.modelValue ?? '') === String(val ?? '')
}

// Hàm chuẩn hóa chuỗi không dấu để tìm kiếm tiếng Việt tốt nhất
const normalizeText = (text) => {
  if (!text) return ''
  return text
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
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
    if (searchInputRef.value) {
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

const selectHighlighted = () => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < filteredOptions.value.length) {
    selectOption(filteredOptions.value[highlightedIndex.value])
  } else if (filteredOptions.value.length === 1) {
    selectOption(filteredOptions.value[0])
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
  z-index: 100;
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

.select2-trigger:hover {
  border-color: #b8c3cc;
  background-color: #ffffff;
}

.select2-trigger.trigger-focus {
  border-color: var(--blue, #496883);
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
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

.select2-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 0.5rem;
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
  z-index: 1000;
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

/* Tùy chỉnh thanh cuộn đẹp mắt giống ảnh */
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
</style>

