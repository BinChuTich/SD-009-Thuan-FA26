<template>
  <div class="select2-container" ref="containerRef" :class="{ 'is-open': isOpen, 'is-disabled': disabled }">
    <!-- Nút hiển thị giá trị hiện tại -->
    <div class="select2-selection" @click="toggleDropdown" tabindex="0" @keydown.down.prevent="openAndFocus">
      <div class="select2-rendered-text" :class="{ 'is-placeholder': !selectedOption }">
        {{ selectedOption ? selectedOption.label : placeholder }}
      </div>

      <div class="select2-actions">
        <!-- Nút xóa nhanh nếu có giá trị và clearable -->
        <button
          v-if="clearable && modelValue !== '' && modelValue !== null && modelValue !== undefined && !disabled"
          type="button"
          class="select2-btn-clear"
          title="Xóa lựa chọn"
          @click.stop="clearSelection"
        >
          &times;
        </button>

        <!-- Mũi tên chỉ xuống -->
        <span class="select2-arrow" :class="{ 'is-rotated': isOpen }">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- Dropdown chứa ô tìm kiếm và danh sách options -->
    <transition name="select2-dropdown-anim">
      <div v-if="isOpen" class="select2-dropdown" @click.stop>
        <!-- Ô tìm kiếm bên trong dropdown (Select2 style) -->
        <div class="select2-search-wrap" v-if="filterable">
          <span class="select2-search-icon">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </span>
          <input
            ref="searchInputRef"
            type="text"
            v-model="searchTerm"
            class="select2-search-input"
            :placeholder="searchPlaceholder"
            @keydown.down.prevent="navigateOptions(1)"
            @keydown.up.prevent="navigateOptions(-1)"
            @keydown.enter.prevent.stop="selectHighlighted"
            @keydown.esc.prevent="closeDropdown"
          />
        </div>

        <!-- Danh sách các options -->
        <div class="select2-options-list" ref="listRef">
          <div
            v-for="(opt, idx) in filteredOptions"
            :key="opt.value"
            class="select2-option-item"
            :class="{
              'is-selected': isSelected(opt.value),
              'is-highlighted': highlightedIndex === idx
            }"
            @click="selectOption(opt)"
            @mouseenter="highlightedIndex = idx"
          >
            <span class="select2-option-text">{{ opt.label }}</span>
            <span v-if="isSelected(opt.value)" class="select2-option-check">✓</span>
          </div>

          <div v-if="filteredOptions.length === 0" class="select2-empty-state">
            Không tìm thấy kết quả phù hợp
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
    type: [String, Number, Boolean, Object],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '-- Chọn --'
  },
  searchPlaceholder: {
    type: String,
    default: 'Tìm kiếm nhanh...'
  },
  clearable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  filterable: {
    type: Boolean,
    default: true
  },
  valueKey: {
    type: String,
    default: 'value'
  },
  labelKey: {
    type: String,
    default: 'label'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const containerRef = ref(null)
const searchInputRef = ref(null)
const listRef = ref(null)

const isOpen = ref(false)
const searchTerm = ref('')
const highlightedIndex = ref(0)

// Chuẩn hóa options
const normalizedOptions = computed(() => {
  return (props.options || []).map(opt => {
    if (typeof opt === 'object' && opt !== null) {
      const val = opt[props.valueKey] !== undefined ? opt[props.valueKey] : (opt.value !== undefined ? opt.value : opt.id)
      const lbl = opt[props.labelKey] !== undefined ? opt[props.labelKey] : (opt.label !== undefined ? opt.label : opt.name || opt.ten || String(val))
      return {
        value: val,
        label: String(lbl ?? ''),
        raw: opt
      }
    }
    return {
      value: opt,
      label: String(opt),
      raw: opt
    }
  })
})

// Lọc options theo từ khóa (hỗ trợ tiếng Việt không dấu)
const removeAccents = (str) => {
  return String(str || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

const filteredOptions = computed(() => {
  const term = removeAccents(searchTerm.value.trim())
  if (!term) return normalizedOptions.value

  return normalizedOptions.value.filter(opt => {
    return removeAccents(opt.label).includes(term)
  })
})

// Tìm option đang được chọn
const selectedOption = computed(() => {
  if (props.modelValue === '' || props.modelValue === null || props.modelValue === undefined) {
    return null
  }
  return normalizedOptions.value.find(opt => String(opt.value) === String(props.modelValue)) || null
})

const isSelected = (val) => {
  if (props.modelValue === '' || props.modelValue === null || props.modelValue === undefined) {
    return false
  }
  return String(val) === String(props.modelValue)
}

const toggleDropdown = () => {
  if (props.disabled) return
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

const openAndFocus = () => {
  if (!isOpen.value) {
    openDropdown()
  }
}

const openDropdown = () => {
  if (props.disabled) return
  isOpen.value = true
  searchTerm.value = ''
  
  // Highlight option đang chọn
  const curIdx = filteredOptions.value.findIndex(opt => isSelected(opt.value))
  highlightedIndex.value = curIdx >= 0 ? curIdx : 0

  nextTick(() => {
    if (searchInputRef.value) {
      searchInputRef.value.focus()
    }
    scrollToHighlighted()
  })
}

const closeDropdown = () => {
  isOpen.value = false
  searchTerm.value = ''
}

const clearSelection = () => {
  if (props.disabled) return
  emit('update:modelValue', '')
  emit('change', '')
  closeDropdown()
}

const selectOption = (opt) => {
  emit('update:modelValue', opt.value)
  emit('change', opt.value, opt.raw)
  closeDropdown()
}

const selectHighlighted = () => {
  if (filteredOptions.value.length > 0 && highlightedIndex.value >= 0 && highlightedIndex.value < filteredOptions.value.length) {
    selectOption(filteredOptions.value[highlightedIndex.value])
  }
}

const navigateOptions = (dir) => {
  const len = filteredOptions.value.length
  if (len === 0) return
  let next = highlightedIndex.value + dir
  if (next < 0) next = len - 1
  if (next >= len) next = 0
  highlightedIndex.value = next
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

const handleClickOutside = (e) => {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
    closeDropdown()
  }
}

watch(isOpen, (val) => {
  if (val) {
    document.addEventListener('click', handleClickOutside)
  } else {
    document.removeEventListener('click', handleClickOutside)
  }
})

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
  box-sizing: border-box;
}

.select2-container.is-disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.select2-selection {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  padding: 0 10px;
  background-color: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
  box-sizing: border-box;
}

.select2-selection:hover {
  border-color: #94a3b8;
}

.select2-container.is-open .select2-selection {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.select2-rendered-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #1e293b;
  font-weight: 500;
  padding-right: 6px;
}

.select2-rendered-text.is-placeholder {
  color: #94a3b8;
  font-weight: 400;
}

.select2-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.select2-btn-clear {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.select2-btn-clear:hover {
  color: #ef4444;
}

.select2-arrow {
  color: #64748b;
  display: inline-flex;
  align-items: center;
  transition: transform 0.2s ease;
}

.select2-arrow.is-rotated {
  transform: rotate(180deg);
}

/* Dropdown */
.select2-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 9999;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  animation: select2-dropdown-show 0.15s ease-out;
}

@keyframes select2-dropdown-show {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.select2-search-wrap {
  position: relative;
  padding: 8px;
  border-bottom: 1px solid #f1f5f9;
  background-color: #f8fafc;
}

.select2-search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  display: flex;
  align-items: center;
}

.select2-search-input {
  width: 100%;
  box-sizing: border-box;
  padding: 6px 10px 6px 30px;
  font-size: 13px;
  border: 1px solid #cbd5e1;
  border-radius: 5px;
  outline: none;
  background: #ffffff;
  color: #1e293b;
  transition: border-color 0.15s ease;
}

.select2-search-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.select2-options-list {
  max-height: 220px;
  overflow-y: auto;
  padding: 4px 0;
}

.select2-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: background-color 0.1s ease;
}

.select2-option-item:hover,
.select2-option-item.is-highlighted {
  background-color: #f1f5f9;
  color: #0f172a;
}

.select2-option-item.is-selected {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.select2-option-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.select2-option-check {
  font-size: 12px;
  color: #2563eb;
  font-weight: 700;
  margin-left: 8px;
}

.select2-empty-state {
  padding: 16px 12px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}
</style>
