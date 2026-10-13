<template>
  <div class="select2-combobox-wrap" ref="comboboxRef" :class="{ 'is-open': isOpen, 'is-disabled': disabled }">
    <!-- Trigger Field -->
    <div
      class="select2-trigger"
      :class="{ 'has-value': hasSelectedValue, 'is-active': isOpen }"
      @click="toggleDropdown"
      tabindex="0"
      @keydown.down.prevent="openDropdown"
      @keydown.esc.prevent="closeDropdown"
    >
      <span class="select2-selected-text" :class="{ 'placeholder-text': !selectedLabel && !hasSelectedValue }">
        {{ selectedLabel || placeholder }}
      </span>

      <div class="select2-trigger-actions">
        <!-- Nút Clear khi đã chọn -->
        <button
          v-if="allowClear && hasSelectedValue && !disabled"
          type="button"
          class="btn-select2-clear"
          @click.stop="clearSelection"
          title="Bỏ chọn"
        >
          ✕
        </button>

        <!-- Mũi tên Chevron -->
        <span class="select2-arrow" :class="{ 'arrow-up': isOpen }">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- Dropdown Menu dạng Floating Panel -->
    <transition name="select2-fade">
      <div v-if="isOpen" class="select2-dropdown-menu" @click.stop>
        <!-- Hộp tìm kiếm Select2 -->
        <div class="select2-search-box">
          <span class="select2-search-icon">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </span>
          <input
            ref="searchInputRef"
            v-model="searchKeyword"
            type="text"
            class="select2-search-input"
            :placeholder="searchPlaceholder || 'Gõ để tìm kiếm...'"
            @keydown.esc.prevent="closeDropdown"
          />
          <button
            v-if="searchKeyword"
            type="button"
            class="btn-search-clear"
            @click="searchKeyword = ''"
            title="Xóa từ khóa tìm"
          >
            ✕
          </button>
        </div>

        <!-- Danh sách các tùy chọn -->
        <ul class="select2-options-list" role="listbox">
          <!-- Tùy chọn Mặc định / Tất cả -->
          <li
            v-if="shouldShowAllOption"
            class="select2-option-item all-option"
            :class="{ 'is-selected': !hasSelectedValue }"
            @click="selectOption({ [valueKey]: '', [labelKey]: defaultOptionLabel })"
          >
            <span class="option-label">{{ defaultOptionLabel }}</span>
            <span v-if="!hasSelectedValue" class="option-check">✓</span>
          </li>

          <!-- Khi tìm kiếm không có kết quả -->
          <li v-if="filteredOptions.length === 0" class="select2-no-results">
            Không tìm thấy kết quả phù hợp
          </li>

          <!-- Các tùy chọn trong danh mục -->
          <li
            v-for="(opt, idx) in filteredOptions"
            :key="getOptionValue(opt) ?? idx"
            class="select2-option-item"
            :class="{ 'is-selected': isSelected(opt) }"
            @click="selectOption(opt)"
          >
            <span class="option-label">{{ getOptionLabel(opt) }}</span>
            <span v-if="isSelected(opt)" class="option-check">✓</span>
          </li>
        </ul>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, null],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  valueKey: {
    type: String,
    default: 'id'
  },
  labelKey: {
    type: String,
    default: 'name'
  },
  placeholder: {
    type: String,
    default: '-- Chọn --'
  },
  searchPlaceholder: {
    type: String,
    default: ''
  },
  allowClear: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  showAllOption: {
    type: Boolean,
    default: true
  },
  allLabel: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const comboboxRef = ref(null)
const searchInputRef = ref(null)
const isOpen = ref(false)
const searchKeyword = ref('')

// Chuẩn hóa tiếng Việt không dấu để tìm kiếm thông minh
const normalizeText = (str) => {
  if (str == null) return ''
  return String(str)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

const getOptionValue = (opt) => {
  if (opt == null) return ''
  if (typeof opt === 'object') {
    if (props.valueKey && opt[props.valueKey] !== undefined) return opt[props.valueKey]
    if (opt.value !== undefined) return opt.value
    if (opt.id !== undefined) return opt.id
    if (opt.ma !== undefined) return opt.ma
    if (opt.code !== undefined) return opt.code
    return ''
  }
  return opt
}

const getOptionLabel = (opt) => {
  if (opt == null) return ''
  if (typeof opt === 'object') {
    if (props.labelKey && opt[props.labelKey] !== undefined) return opt[props.labelKey]
    if (opt.label !== undefined) return opt.label
    if (opt.name !== undefined) return opt.name
    if (opt.ten !== undefined) return opt.ten
    if (opt.tenDotGiamGia !== undefined) return opt.tenDotGiamGia
    if (opt.title !== undefined) return opt.title
    if (opt.text !== undefined) return opt.text
    return String(getOptionValue(opt) ?? '')
  }
  return String(opt)
}

const defaultOptionLabel = computed(() => {
  if (props.allLabel) return props.allLabel
  return props.placeholder || '-- Tất cả --'
})

const shouldShowAllOption = computed(() => {
  if (!props.showAllOption) return false
  const hasEmpty = (props.options || []).some(opt => {
    const val = getOptionValue(opt)
    return val === '' || val === null || val === undefined
  })
  return !hasEmpty
})

const hasSelectedValue = computed(() => {
  return props.modelValue !== '' && props.modelValue !== null && props.modelValue !== undefined
})

const isValueEqual = (valA, valB) => {
  if (valA === valB) return true
  if ((valA === '' || valA == null) && (valB === '' || valB == null)) return true
  return String(valA ?? '').trim() === String(valB ?? '').trim()
}

const selectedLabel = computed(() => {
  const found = (props.options || []).find(opt => {
    return isValueEqual(getOptionValue(opt), props.modelValue)
  })
  if (found) {
    const lbl = getOptionLabel(found)
    if (lbl) return lbl
  }
  if (!hasSelectedValue.value) return ''
  return String(props.modelValue)
})

const isSelected = (opt) => {
  return isValueEqual(getOptionValue(opt), props.modelValue)
}

const filteredOptions = computed(() => {
  if (!searchKeyword.value.trim()) {
    return props.options || []
  }
  const kw = normalizeText(searchKeyword.value)
  return (props.options || []).filter(opt => {
    const text = normalizeText(getOptionLabel(opt))
    return text.includes(kw)
  })
})

const openDropdown = () => {
  if (props.disabled) return
  isOpen.value = true
  searchKeyword.value = ''
  nextTick(() => {
    if (searchInputRef.value) {
      searchInputRef.value.focus()
    }
  })
}

const closeDropdown = () => {
  isOpen.value = false
  searchKeyword.value = ''
}

const toggleDropdown = () => {
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

const selectOption = (opt) => {
  const val = getOptionValue(opt)
  emit('update:modelValue', val)
  emit('change', val)
  closeDropdown()
}

const clearSelection = () => {
  emit('update:modelValue', '')
  emit('change', '')
}

// Bắt sự kiện click ra ngoài để đóng popup
const handleClickOutside = (event) => {
  if (comboboxRef.value && !comboboxRef.value.contains(event.target)) {
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
.select2-combobox-wrap {
  position: relative;
  width: 100%;
  font-family: inherit;
  user-select: none;
}

.select2-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 2.45rem;
  padding: 0 0.85rem;
  background-color: #ffffff;
  border: 1.5px solid #dfd9ce;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.select2-trigger:hover:not(.is-disabled) {
  border-color: #bfa895;
  background-color: #fffefb;
}

.select2-trigger.is-active {
  border-color: #496883;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.select2-trigger.has-value {
  border-color: #c9c0b3;
}

.select2-selected-text {
  font-size: 0.86rem;
  color: #2b353b;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  padding-right: 6px;
}

.select2-selected-text.placeholder-text {
  color: #7d8890;
  font-weight: 500;
}

.select2-trigger-actions {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}

.btn-select2-clear {
  border: none;
  background: #ede8df;
  color: #736d65;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.15s;
  padding: 0;
  line-height: 1;
}

.btn-select2-clear:hover {
  background: #c94a29;
  color: #ffffff;
}

.select2-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #838e96;
  transition: transform 0.2s ease;
}

.select2-arrow.arrow-up {
  transform: rotate(180deg);
  color: #496883;
}

/* Dropdown Menu Floating */
.select2-dropdown-menu {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  right: 0;
  z-index: 1050;
  background-color: #ffffff;
  border: 1.5px solid #dcd4c7;
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(43, 66, 87, 0.16);
  padding: 8px;
  min-width: 190px;
  max-width: 100%;
  animation: select2Pop 0.18s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}

@keyframes select2Pop {
  from {
    opacity: 0;
    transform: translateY(-6px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Ô tìm kiếm bên trong dropdown */
.select2-search-box {
  display: flex;
  align-items: center;
  background-color: #f7f5ef;
  border: 1px solid #dfd9ce;
  border-radius: 6px;
  padding: 0 8px;
  margin-bottom: 6px;
  height: 32px;
}

.select2-search-icon {
  color: #8c979e;
  margin-right: 6px;
  display: flex;
  align-items: center;
}

.select2-search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.83rem;
  color: #2b353b;
  outline: none;
  width: 100%;
}

.btn-search-clear {
  border: none;
  background: transparent;
  color: #8c979e;
  cursor: pointer;
  font-size: 11px;
  padding: 2px 4px;
}

.btn-search-clear:hover {
  color: #c94a29;
}

/* Danh sách tùy chọn */
.select2-options-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: #cfc7ba #f7f5ef;
}

.select2-options-list::-webkit-scrollbar {
  width: 5px;
}

.select2-options-list::-webkit-scrollbar-track {
  background: #f7f5ef;
  border-radius: 4px;
}

.select2-options-list::-webkit-scrollbar-thumb {
  background: #cfc7ba;
  border-radius: 4px;
}

.select2-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 6px;
  font-size: 0.84rem;
  color: #3f4c53;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-bottom: 2px;
}

.select2-option-item:hover {
  background-color: #f7f5ef;
  color: #496883;
  font-weight: 600;
}

.select2-option-item.all-option {
  border-bottom: 1px dashed #e6dfd3;
  margin-bottom: 5px;
  font-weight: 500;
  color: #636f75;
}

.select2-option-item.all-option:hover {
  color: #496883;
}

.select2-option-item.is-selected {
  background-color: #eaf1f4;
  color: #496883;
  font-weight: 700;
}

.option-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.option-check {
  font-size: 0.85rem;
  color: #496883;
  font-weight: bold;
  margin-left: 6px;
}

.select2-no-results {
  padding: 12px 10px;
  text-align: center;
  font-size: 0.82rem;
  color: #929ea6;
  font-style: italic;
}

.is-disabled .select2-trigger {
  background-color: #f5f4f0;
  border-color: #e5e2db;
  cursor: not-allowed;
  opacity: 0.7;
}

/* Transition */
.select2-fade-enter-active,
.select2-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.select2-fade-enter-from,
.select2-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

