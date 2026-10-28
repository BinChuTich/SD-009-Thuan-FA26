import { reactive } from 'vue'

export const dialogState = reactive({
  confirm: {
    show: false,
    title: '',
    subtitle: '',
    message: '',
    type: 'question', // 'question' | 'warning' | 'success' | 'danger'
    confirmText: 'Xác nhận',
    cancelText: 'Hủy bỏ',
    hideIcon: false,
    resolve: null
  },
  alert: {
    show: false,
    title: '',
    message: '',
    type: 'warning', // 'warning' | 'error' | 'info' | 'success'
    btnText: 'Đã hiểu',
    resolve: null
  },
  toasts: []
})

/**
 * Hiển thị popup xác nhận đẹp mắt ở CHÍNH GIỮA TRANG với 2 nút: Xác nhận & Hủy bỏ
 * @returns Promise<boolean>
 */
export function showConfirm({
  title = 'Xác nhận thao tác',
  subtitle = 'Vui lòng xác nhận để tiếp tục thực hiện thao tác',
  message = 'Bạn có chắc chắn muốn thực hiện hành động này?',
  type = 'question',
  confirmText = 'Xác nhận',
  cancelText = 'Hủy bỏ',
  hideIcon = false,
  showIcon = true
} = {}) {
  return new Promise((resolve) => {
    dialogState.confirm = {
      show: true,
      title,
      subtitle,
      message,
      type,
      confirmText,
      cancelText,
      hideIcon: hideIcon || showIcon === false,
      resolve
    }
  })
}

export function handleConfirmChoice(choice) {
  if (dialogState.confirm.resolve) {
    dialogState.confirm.resolve(choice)
  }
  dialogState.confirm.show = false
}

/**
 * Hiển thị popup thông báo validate / cảnh báo lỗi ở CHÍNH GIỮA TRANG với nút "Đã hiểu"
 * @returns Promise<void>
 */
export function showAlert({
  title = 'Thông tin chưa hợp lệ',
  message = '',
  type = 'warning',
  btnText = 'Đã hiểu'
} = {}) {
  return new Promise((resolve) => {
    dialogState.alert = {
      show: true,
      title,
      message,
      type,
      btnText,
      resolve
    }
  })
}

export function handleAlertClose() {
  if (dialogState.alert.resolve) {
    dialogState.alert.resolve()
  }
  dialogState.alert.show = false
}

let toastCounter = 0
/**
 * Hiển thị thông báo Toast nhanh gọn
 * @param {string|object} message Nội dung thông báo hoặc object { title, message, type }
 * @param {'success'|'error'|'warning'|'info'} type Loại thông báo
 * @param {number} duration Thời gian hiển thị (ms)
 * @param {string|null} title Tiêu đề thông báo
 */
export function showToast(message, type = 'success', duration = 3500, title = null) {
  const id = ++toastCounter
  if (typeof message === 'object' && message !== null) {
    dialogState.toasts.push({
      id,
      title: message.title || null,
      message: message.message || message.desc || '',
      type: message.type || type
    })
  } else {
    dialogState.toasts.push({ id, title, message, type })
  }
  setTimeout(() => {
    removeToast(id)
  }, duration)
}

export function removeToast(id) {
  const index = dialogState.toasts.findIndex(t => t.id === id)
  if (index !== -1) {
    dialogState.toasts.splice(index, 1)
  }
}

if (typeof window !== 'undefined') {
  window.showToast = showToast
  window.showAlert = showAlert
}
