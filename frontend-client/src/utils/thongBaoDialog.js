import { ref } from 'vue'

export const dialogState = ref({
  isOpen: false,
  type: 'info', // 'confirm' | 'info' | 'warning' | 'error' | 'success' | 'danger'
  isDanger: false,
  icon: '',
  title: 'Thông báo',
  message: '',
  confirmText: 'Đồng ý',
  cancelText: 'Hủy bỏ',
  resolve: null
})

export const toastsList = ref([])
let toastIdCounter = 1

/**
 * Hiển thị Hộp thoại xác nhận In-App (Thay thế hoàn toàn window.confirm)
 */
export function showAppConfirm(message, title = 'Xác nhận yêu cầu', options = {}) {
  const isDelete = options.type === 'delete' || /xóa|delete|remove/i.test(title || '')
  const isCancel = /hủy|cancel/i.test(title || '')
  const isDanger = Boolean(
    options.isDanger ||
    options.type === 'danger' ||
    options.type === 'delete' ||
    isDelete ||
    isCancel
  )

  let defaultConfirmText = 'Xác nhận'
  let defaultCancelText = 'Hủy bỏ'

  if (isDelete) {
    defaultConfirmText = 'Xóa'
    defaultCancelText = 'Hủy'
  } else if (isCancel) {
    defaultConfirmText = 'Xác nhận hủy'
    defaultCancelText = 'Quay lại'
  }

  return new Promise((resolve) => {
    dialogState.value = {
      isOpen: true,
      type: options.type || (isDanger ? 'danger' : 'confirm'),
      isDanger,
      icon: options.icon || (isDelete ? 'trash' : (isCancel ? 'warning' : '')),
      title: title || 'Xác nhận',
      message: message || '',
      confirmText: options.confirmText || defaultConfirmText,
      cancelText: options.cancelText || defaultCancelText,
      resolve
    }
  })
}

/**
 * Hiển thị Thông báo In-App (Thay thế hoàn toàn window.alert)
 */
export function showAppAlert(message, title = 'Thông báo', type = 'info', options = {}) {
  return new Promise((resolve) => {
    dialogState.value = {
      isOpen: true,
      type: type || 'info',
      isDanger: type === 'error' || type === 'danger',
      icon: options.icon || '',
      title: title || 'Thông báo',
      message: message || '',
      confirmText: options.confirmText || 'Đã hiểu',
      cancelText: '',
      resolve
    }
  })
}

export function closeAppDialog(result = false) {
  if (dialogState.value.resolve) {
    dialogState.value.resolve(result)
  }
  dialogState.value.isOpen = false
  dialogState.value.resolve = null
}

/**
 * Hiển thị Toast thông báo nổi ở góc màn hình
 */
export function showAppToast(message, type = 'success', duration = 3500, title = null) {
  const id = toastIdCounter++
  if (typeof message === 'object' && message !== null) {
    toastsList.value.push({
      id,
      title: message.title || null,
      message: message.message || message.desc || '',
      type: message.type || type
    })
  } else {
    toastsList.value.push({ id, title, message, type })
  }
  setTimeout(() => {
    toastsList.value = toastsList.value.filter(t => t.id !== id)
  }, duration)
}

if (typeof window !== 'undefined') {
  window.showAppToast = showAppToast
  window.showAppAlert = showAppAlert
  window.showAppConfirm = showAppConfirm
}
