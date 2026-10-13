<template>
  <div
    ref="containerRef"
    class="select2-container"
    :class="{
      'is-open': isOpen,
      'is-disabled': disabled,
      'has-error': hasError
    }"
  >
    <!-- Selection Box -->
    <div
      class="select2-selection"
      tabindex="0"
      role="combobox"
      :aria-expanded="isOpen"
      :aria-disabled="disabled"
      @click="toggleDropdown"
      @keydown="onTriggerKeydown"
    >
      <span
        class="select2-selection__rendered"
        :class="{ 'is-placeholder': !selectedOption }"
      >
        {{ selectedOption ? selectedOption[labelKey] : placeholder }}
      </span>

      <span class="select2-selection__arrow">
        <svg
          viewBox="0 0 20 20"
          width="16"
          height="16"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="6 8 10 12 14 8" />
        </svg>
      </span>
    </div>

    <!-- Dropdown Menu -->
    <div v-if="isOpen" class="select2-dropdown">
      <div class="select2-search">
        <input
          ref="searchInputRef"
          v-model="searchQuery"
          type="text"
          class="select2-search__field"
          :placeholder="searchPlaceholder"
          autocomplete="off"
          @keydown="onSearchKeydown"
        />
      </div>

      <ul ref="listRef" class="select2-results__options" role="listbox">
        <li
          v-if="allowEmpty && !searchQuery.trim()"
          class="select2-results__option select2-results__option--placeholder"
          :class="{ 'is-selected': modelValue === '' || modelValue === null || modelValue === undefined }"
          @click="selectValue('')"
        >
          {{ placeholder }}
        </li>
        <li
          v-for="(item, idx) in filteredOptions"
          :key="item[valueKey]"
          class="select2-results__option"
          :class="{
            'is-selected': String(item[valueKey]) === String(modelValue),
            'is-highlighted': idx === highlightedIndex
          }"
          @mouseenter="highlightedIndex = idx"
          @click="selectValue(item[valueKey])"
        >
          {{ item[labelKey] }}
        </li>
        <li v-if="!filteredOptions.length" class="select2-results__empty">
          Không tìm thấy kết quả phù hợp
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  valueKey: {
    type: String,
    default: 'code'
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
    default: 'Gõ để tìm kiếm...'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  hasError: {
    type: Boolean,
    default: false
  },
  allowEmpty: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const containerRef = ref(null)
const searchInputRef = ref(null)
const listRef = ref(null)

const isOpen = ref(false)
const searchQuery = ref('')
const highlightedIndex = ref(0)

function removeAccents(str) {
  return String(str || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
}

const selectedOption = computed(() => {
  if (props.modelValue === '' || props.modelValue === null || props.modelValue === undefined) {
    return null
  }
  return props.options.find(opt => String(opt[props.valueKey]) === String(props.modelValue)) || null
})

const filteredOptions = computed(() => {
  const q = removeAccents(searchQuery.value)
  if (!q) return props.options
  return props.options.filter(opt => {
    const labelNorm = removeAccents(opt[props.labelKey])
    return labelNorm.includes(q)
  })
})

watch(searchQuery, () => {
  highlightedIndex.value = 0
})

watch(() => props.disabled, (val) => {
  if (val && isOpen.value) {
    closeDropdown()
  }
})

function toggleDropdown() {
  if (props.disabled) return
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

function openDropdown() {
  if (props.disabled) return
  isOpen.value = true
  searchQuery.value = ''

  const currentIdx = props.options.findIndex(
    opt => String(opt[props.valueKey]) === String(props.modelValue)
  )
  highlightedIndex.value = currentIdx >= 0 ? currentIdx : 0

  nextTick(() => {
    searchInputRef.value?.focus()
    scrollToHighlighted()
  })
}

function closeDropdown() {
  isOpen.value = false
  searchQuery.value = ''
}

function selectValue(val) {
  emit('update:modelValue', val)
  closeDropdown()
  nextTick(() => {
    emit('change', val)
  })
}

function scrollToHighlighted() {
  if (!listRef.value) return
  const activeEl =
    listRef.value.querySelector('.is-highlighted') ||
    listRef.value.querySelector('.is-selected')
  if (activeEl) {
    activeEl.scrollIntoView({ block: 'nearest' })
  }
}

function onTriggerKeydown(e) {
  if (props.disabled) return
  if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
    e.preventDefault()
    openDropdown()
  }
}

function onSearchKeydown(e) {
  const len = filteredOptions.value.length
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (len > 0) {
      highlightedIndex.value = (highlightedIndex.value + 1) % len
      nextTick(scrollToHighlighted)
    }
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (len > 0) {
      highlightedIndex.value = (highlightedIndex.value - 1 + len) % len
      nextTick(scrollToHighlighted)
    }
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (len > 0 && filteredOptions.value[highlightedIndex.value]) {
      selectValue(filteredOptions.value[highlightedIndex.value][props.valueKey])
    }
  } else if (e.key === 'Escape') {
    e.preventDefault()
    closeDropdown()
  }
}

function handleClickOutside(e) {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})
</script>

<style scoped>
.select2-container {
  position: relative;
  width: 100%;
  font-size: 0.95rem;
  user-select: none;
  box-sizing: border-box;
}

.select2-selection {
  height: 2.85rem;
  border: 1px solid var(--line, #dfdcd3);
  border-radius: 8px;
  padding: 0 0.85rem 0 0.95rem;
  background-color: #ffffff;
  color: #2b383e;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.select2-selection:focus,
.select2-container.is-open .select2-selection {
  border-color: #496883;
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.select2-container.is-open .select2-selection {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.select2-container.has-error .select2-selection {
  border-color: #e53e3e !important;
  background-color: #fffaf9;
}

.select2-container.has-error.is-open .select2-selection {
  box-shadow: 0 0 0 3px rgba(229, 62, 62, 0.15) !important;
}

.select2-container.is-disabled .select2-selection {
  background-color: #f3f0e8;
  color: #8c969c;
  cursor: not-allowed;
}

.select2-selection__rendered {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.select2-selection__rendered.is-placeholder {
  color: #7b878d;
}

.select2-selection__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #6b7880;
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.select2-container.is-open .select2-selection__arrow {
  transform: rotate(180deg);
  color: #496883;
}

.select2-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background-color: #ffffff;
  border: 1px solid #496883;
  border-top: none;
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
  box-shadow: 0 8px 20px rgba(35, 48, 58, 0.14);
  z-index: 1050;
  overflow: hidden;
}

.select2-search {
  padding: 0.45rem 0.5rem;
  background-color: #faf8f4;
  border-bottom: 1px solid #ece8de;
}

.select2-search__field {
  width: 100%;
  height: 2.2rem !important;
  padding: 0 0.7rem !important;
  border: 1px solid #d5cfc2 !important;
  border-radius: 6px !important;
  font-size: 0.9rem !important;
  color: #2b383e !important;
  background-color: #ffffff !important;
  outline: none !important;
  box-sizing: border-box !important;
}

.select2-search__field:focus {
  border-color: #496883 !important;
  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12) !important;
}

.select2-results__options {
  list-style: none;
  margin: 0;
  padding: 0.25rem 0;
  max-height: 225px;
  overflow-y: auto;
}

.select2-results__option {
  padding: 0.52rem 0.9rem;
  font-size: 0.92rem;
  color: #2b383e;
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.select2-results__option--placeholder {
  color: #7b878d;
  font-style: italic;
}

.select2-results__option.is-highlighted {
  background-color: #496883;
  color: #ffffff;
}

.select2-results__option.is-selected:not(.is-highlighted) {
  background-color: #eaf1f5;
  color: #2c4350;
  font-weight: 600;
}

.select2-results__empty {
  padding: 0.75rem 0.9rem;
  text-align: center;
  color: #88959c;
  font-size: 0.88rem;
}
</style>
