<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: { type: Number, required: true },
  total: { type: Number, required: true },
  oneBased: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['select'])

const items = computed(() => {
  const total = Math.max(0, Number(props.total) || 0)
  if (!total) return []
  const offset = props.oneBased ? 1 : 0
  const first = offset
  const last = total - 1 + offset
  const current = Math.min(last, Math.max(first, Number(props.current) || offset))
  if (total <= 4) return Array.from({ length: total }, (_, index) => ({ type: 'page', page: index + offset }))
  const pages = new Set([first, last, current])
  if (current <= first + 2) {
    [first + 1, first + 2].forEach(page => pages.add(page))
  } else if (current >= last - 2) {
    [last - 2, last - 1].forEach(page => pages.add(page))
  } else {
    pages.add(current - 1)
    pages.add(current + 1)
  }
  const sorted = [...pages].filter(page => page >= first && page <= last).sort((a, b) => a - b)
  const result = []
  sorted.forEach((page, index) => {
    const previous = sorted[index - 1]
    if (previous !== undefined && page - previous > 1) {
      result.push({ type: 'ellipsis', key: `${previous}-${page}`, page: Math.floor((previous + page) / 2) })
    }
    result.push({ type: 'page', page })
  })
  return result
})
</script>

<template>
  <template v-for="item in items" :key="item.type === 'page' ? `p-${item.page}` : `e-${item.key}`">
    <button
      type="button"
      class="admin-page-number"
      :class="{ active: item.type === 'page' && item.page === current, ellipsis: item.type === 'ellipsis' }"
      :disabled="disabled"
      :title="item.type === 'ellipsis' ? `Chuyển đến trang ${item.page + (oneBased ? 0 : 1)}` : `Trang ${item.page + (oneBased ? 0 : 1)}`"
      @click="emit('select', item.page)"
    >{{ item.type === 'ellipsis' ? '…' : item.page + (oneBased ? 0 : 1) }}</button>
  </template>
</template>

<style scoped>
.admin-page-number {
  min-width: 32px;
  height: 32px;
  padding: 0 9px;
  border: 1px solid #d6dee8;
  border-radius: 6px;
  background: #fff;
  color: #46627d;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  transition: 0.18s ease;
}

.admin-page-number:hover:not(:disabled) {
  border-color: #496883;
  background: #f1f6fa;
  transform: translateY(-1px);
}

.admin-page-number.active {
  border-color: #496883;
  background: #496883;
  color: #fff;
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.2);
}

.admin-page-number.ellipsis {
  font-size: 17px;
  letter-spacing: 1px;
}

.admin-page-number:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
