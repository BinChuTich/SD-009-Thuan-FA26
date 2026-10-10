<template>
  <div class="attr-combobox" ref="comboboxRef">
    <div
      class="combobox-control"
      :class="{
        'is-open': isOpen,
        'has-value': !!selectedItem,
        'is-loading': loading
      }"
      @click="toggleDropdown"
    >
      <input
        ref="inputRef"
        type="text"
        class="combobox-input"
        :value="displayValue"
        :placeholder="placeholder"
        @focus="onFocus"
        @input="onInput"
        @keydown.enter.prevent.stop="handleEnter"
        @keydown.down.prevent="navigateOptions(1)"
        @keydown.up.prevent="navigateOptions(-1)"
        @keydown.esc.prevent="closeDropdown"
      />

      <div class="combobox-suffix">
        <!-- Nút xóa nhanh khi đã chọn -->
        <button
          v-if="modelValue && !loading"
          type="button"
          class="btn-clear-combobox"
          title="Xóa lựa chọn"
          @click.stop="clearSelection"
        >
          &times;
        </button>

        <!-- Loading spinner -->
        <span v-if="loading" class="combobox-spinner"></span>

        <!-- Mũi tên chỉ xuống -->
        <span class="arrow-icon" :class="{ 'rotate': isOpen }">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </span>
      </div>
    </div>

    <!-- Dropdown danh sách gợi ý & Thêm mới -->
    <transition name="dropdown-anim">
      <div v-if="isOpen" class="combobox-dropdown" @click.stop>
        <!-- Hàng thêm mới khi nhập từ khóa chưa có trong danh mục -->
        <div
          v-if="canAddNew"
          class="combobox-add-action"
          :class="{ 'is-highlighted': highlightedIndex === -1 }"
          @click="createNew(searchQuery)"
        >
          <div class="add-action-left">
            <span class="add-plus-badge">+</span>
            <span class="add-action-text">
              Thêm mới <strong>"{{ searchQuery.trim() }}"</strong>
            </span>
          </div>
          <span class="add-enter-hint">Nhấn Enter ↵</span>
        </div>

        <!-- Danh sách các mục đã lọc -->
        <div class="combobox-options-list" ref="listRef">
          <div
            v-for="(item, idx) in filteredItems"
            :key="item.id"
            class="combobox-option-item"
            :class="{
              'is-selected': item.id === modelValue,
              'is-highlighted': highlightedIndex === idx
            }"
            @click="selectItem(item)"
            @mouseenter="highlightedIndex = idx"
          >
            <span class="option-text">{{ item[nameKey] }}</span>
            <span v-if="item.id === modelValue" class="option-check">✓</span>
          </div>

          <div v-if="filteredItems.length === 0 && !canAddNew" class="combobox-empty-state">
            Không tìm thấy {{ (label || 'thuộc tính').toLowerCase() }} phù hợp
          </div>
        </div>
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
  items: {
    type: Array,
    default: () => []
  },
  nameKey: {
    type: String,
    required: true
  },
  type: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '-- Chọn hoặc nhập để tìm kiếm / thêm mới --'
  }
})

const emit = defineEmits(['update:modelValue', 'created', 'toast'])

const comboboxRef = ref(null)
const inputRef = ref(null)
const listRef = ref(null)

const isOpen = ref(false)
const isTyping = ref(false)
const searchQuery = ref('')
const loading = ref(false)
const highlightedIndex = ref(-1)

// Tìm item hiện tại đang được chọn
const selectedItem = computed(() => {
  if (!props.modelValue && props.modelValue !== 0) return null
  return (props.items || []).find(i => String(i.id) === String(props.modelValue)) || null
})

// Giá trị hiển thị trên ô input
const displayValue = computed(() => {
  if (isTyping.value) {
    return searchQuery.value
  }
  return selectedItem.value ? selectedItem.value[props.nameKey] : ''
})

// Chuẩn hóa chuỗi tiếng Việt để tìm kiếm không dấu
const removeVietnameseTones = (str) => {
  if (!str) return ''
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
}

// Lọc danh sách thuộc tính theo từ khóa
const filteredItems = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return props.items || []

  const qNoTone = removeVietnameseTones(q)
  return (props.items || []).filter(item => {
    const name = String(item[props.nameKey] || '').toLowerCase()
    const nameNoTone = removeVietnameseTones(name)
    return name.includes(q) || nameNoTone.includes(qNoTone)
  })
})

// Kiểm tra xem từ khóa hiện tại có trùng chính xác với mục nào trong danh sách không
const exactMatchedItem = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return null
  return (props.items || []).find(item => {
    const name = String(item[props.nameKey] || '').trim().toLowerCase()
    return name === q
  })
})

// Điều kiện hiển thị dòng "+ Thêm mới"
const canAddNew = computed(() => {
  const q = searchQuery.value.trim()
  return q.length > 0 && !exactMatchedItem.value && !loading.value
})

const toggleDropdown = () => {
  if (isOpen.value) {
    closeDropdown()
  } else {
    openDropdown()
  }
}

const openDropdown = () => {
  isOpen.value = true
  isTyping.value = true
  searchQuery.value = ''
  highlightedIndex.value = -1
  nextTick(() => {
    inputRef.value?.focus()
    inputRef.value?.select()
  })
}

const closeDropdown = () => {
  isOpen.value = false
  isTyping.value = false
  searchQuery.value = ''
  highlightedIndex.value = -1
}

const onFocus = () => {
  if (!isOpen.value) {
    openDropdown()
  }
}

const onInput = (e) => {
  isTyping.value = true
  isOpen.value = true
  searchQuery.value = e.target.value
  highlightedIndex.value = -1
}

const selectItem = (item) => {
  emit('update:modelValue', item.id)
  closeDropdown()
}

const clearSelection = () => {
  emit('update:modelValue', '')
  searchQuery.value = ''
  isTyping.value = false
  closeDropdown()
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// Xử lý tạo mới thuộc tính
const createNew = async (name) => {
  const ten = (name || '').trim()
  if (!ten) return

  // 1. Kiểm tra trùng lần nữa
  const dup = (props.items || []).find(
    i => String(i[props.nameKey] || '').trim().toLowerCase() === ten.toLowerCase()
  )
  if (dup) {
    selectItem(dup)
    emit('toast', { message: `Đã chọn ${props.label.toLowerCase()} "${dup[props.nameKey]}" có sẵn!`, type: 'info' })
    return
  }

  loading.value = true
  try {
    const payload = {
      ten: ten,
      trangThai: 1
    }
    const res = await api.post(`/api/thuoc-tinh/type/${props.type}`, payload)
    const createdItem = res.data

    // Bắn sự kiện lên component cha để cập nhật mảng attributes
    emit('created', createdItem)
    emit('update:modelValue', createdItem.id)
    emit('toast', { message: `Đã thêm mới và chọn ${props.label.toLowerCase()} "${ten}"!`, type: 'success' })
    closeDropdown()
  } catch (err) {
    const msg = err.response?.data?.message || `Không thể thêm mới ${props.label.toLowerCase()}!`
    emit('toast', { message: msg, type: 'error' })
  } finally {
    loading.value = false
  }
}

// Xử lý phím Enter
const handleEnter = () => {
  if (!isOpen.value) {
    openDropdown()
    return
  }

  // 1. Nếu đang chọn mục qua phím mũi tên
  if (highlightedIndex.value >= 0 && highlightedIndex.value < filteredItems.value.length) {
    selectItem(filteredItems.value[highlightedIndex.value])
    return
  }

  // 2. Nếu highlightedIndex là -1 (dòng thêm mới) hoặc có từ khóa
  const q = searchQuery.value.trim()
  if (!q) {
    closeDropdown()
    return
  }

  // 3. Kiểm tra trùng: nếu trùng thì chọn mục đã có
  if (exactMatchedItem.value) {
    selectItem(exactMatchedItem.value)
    emit('toast', { message: `Đã chọn ${props.label.toLowerCase()} "${exactMatchedItem.value[props.nameKey]}" có sẵn!`, type: 'info' })
    return
  }

  // 4. Chưa trùng -> thêm mới luôn
  createNew(q)
}

// Điều hướng bằng phím mũi tên Up/Down
const navigateOptions = (direction) => {
  if (!isOpen.value) {
    openDropdown()
    return
  }

  const maxIndex = filteredItems.value.length - 1
  const minIndex = canAddNew.value ? -1 : 0

  if (maxIndex < minIndex) return

  let next = highlightedIndex.value + direction
  if (next < minIndex) next = maxIndex
  if (next > maxIndex) next = minIndex

  highlightedIndex.value = next
}

// Bắt sự kiện click bên ngoài để đóng dropdown
const handleClickOutside = (e) => {
  if (comboboxRef.value && !comboboxRef.value.contains(e.target)) {
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
.attr-combobox {
  position: relative;
  width: 100%;
}

.combobox-control {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0 0.75rem;
  box-sizing: border-box;
  cursor: pointer;
  transition: all 0.2s ease;
}

.combobox-control:hover {
  border-color: #cbd5e1;
}

.combobox-control.is-open {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.15);
}

.combobox-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.9rem;
  color: #1e293b;
  padding: 0;
  cursor: pointer;
}

.combobox-control.is-open .combobox-input {
  cursor: text;
}

.combobox-input::placeholder {
  color: #94a3b8;
  font-size: 0.88rem;
}

.combobox-suffix {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: 6px;
  flex-shrink: 0;
}

.btn-clear-combobox {
  border: none;
  background: #f1f5f9;
  color: #64748b;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0;
  transition: all 0.15s;
}

.btn-clear-combobox:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.combobox-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #e2e8f0;
  border-top-color: var(--blue, #496883);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.arrow-icon {
  display: flex;
  align-items: center;
  color: #64748b;
  transition: transform 0.2s ease;
}

.arrow-icon.rotate {
  transform: rotate(180deg);
}

/* Dropdown */
.combobox-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
  z-index: 1000;
  overflow: hidden;
  max-height: 280px;
  display: flex;
  flex-direction: column;
}

/* Hàng Thêm mới */
.combobox-add-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  background-color: #f0fdf4;
  border-bottom: 1px solid #bbf7d0;
  cursor: pointer;
  transition: all 0.15s;
}

.combobox-add-action:hover,
.combobox-add-action.is-highlighted {
  background-color: #dcfce7;
}

.add-action-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  color: #15803d;
  min-width: 0;
}

.add-plus-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: #22c55e;
  color: #ffffff;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.add-action-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.add-action-text strong {
  color: #14532d;
}

.add-enter-hint {
  font-size: 0.75rem;
  font-weight: 600;
  color: #16a34a;
  background: #ffffff;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #86efac;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Options list */
.combobox-options-list {
  overflow-y: auto;
  max-height: 220px;
  padding: 4px 0;
}

.combobox-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  font-size: 0.88rem;
  color: #334155;
  cursor: pointer;
  transition: background-color 0.12s;
}

.combobox-option-item:hover,
.combobox-option-item.is-highlighted {
  background-color: #f1f5f9;
  color: #0f172a;
}

.combobox-option-item.is-selected {
  background-color: #eaf2f6;
  color: var(--blue, #496883);
  font-weight: 600;
}

.option-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-check {
  font-weight: 700;
  color: var(--blue, #496883);
  margin-left: 8px;
}

.combobox-empty-state {
  padding: 1rem 0.85rem;
  text-align: center;
  font-size: 0.85rem;
  color: #94a3b8;
}

/* Animation */
.dropdown-anim-enter-active,
.dropdown-anim-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-anim-enter-from,
.dropdown-anim-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
