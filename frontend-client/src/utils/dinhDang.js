/**
 * Tiện ích định dạng tiền tệ và thời gian (Tiếng Việt)
 */

// Định dạng tiền tệ VND
export const formatCurrency = (val) => {
  if (val == null || isNaN(val)) return '0₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(val))
}

// Định dạng ngày giờ hiển thị
export const formatDateTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  return d.toLocaleString('vi-VN')
}
