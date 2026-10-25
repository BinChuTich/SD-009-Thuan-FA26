/**
 * Quản lý hệ thống thông báo khách hàng (Client Notifications)
 * CHỈ HIỂN THỊ THÔNG BÁO RIÊNG BIỆT CHO TỪNG TÀI KHOẢN ĐÃ ĐĂNG NHẬP
 * CHỈ THÔNG BÁO ĐẶT HÀNG THÀNH CÔNG VỚI ĐƯỜNG LINK THEO DÕI ĐƠN HÀNG
 */
import { ref, computed } from 'vue'
import api from '@/api'

const getReadStorageKey = () => {
  try {
    const u = JSON.parse(localStorage.getItem('fftshirt_client_user') || 'null')
    if (u && (u.id || u.email)) {
      return `fftshirt_read_notifications_${u.id || u.email}`
    }
  } catch (e) {}
  return 'fftshirt_read_notifications_guest'
}

const loadReadIds = () => {
  try {
    const key = getReadStorageKey()
    const raw = localStorage.getItem(key)
    return raw ? new Set(JSON.parse(raw)) : new Set()
  } catch (e) {
    return new Set()
  }
}

const saveReadIds = (set) => {
  try {
    const key = getReadStorageKey()
    localStorage.setItem(key, JSON.stringify(Array.from(set)))
  } catch (e) {
    console.warn('Lỗi lưu trạng thái đã đọc thông báo:', e)
  }
}

export const readIds = ref(loadReadIds())
export const notifications = ref([])
export const loadingNotifications = ref(false)

export const clearNotifications = () => {
  notifications.value = []
  readIds.value = new Set()
}

export const formatTimeAgo = (dateInput) => {
  if (!dateInput) return 'Vừa xong'
  try {
    const d = new Date(dateInput)
    if (isNaN(d.getTime())) return 'Gần đây'
    const now = Date.now()
    const diffSec = Math.floor((now - d.getTime()) / 1000)

    if (diffSec < 60) return 'Vừa xong'
    const diffMin = Math.floor(diffSec / 60)
    if (diffMin < 60) return `${diffMin} phút trước`
    const diffHours = Math.floor(diffMin / 60)
    if (diffHours < 24) return `${diffHours} giờ trước`
    const diffDays = Math.floor(diffHours / 24)
    if (diffDays === 1) return 'Hôm qua'
    if (diffDays < 7) return `${diffDays} ngày trước`

    const day = String(d.getDate()).padStart(2, '0')
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const year = d.getFullYear()
    return `${day}/${month}/${year}`
  } catch (e) {
    return 'Gần đây'
  }
}

/**
 * Lấy danh sách thông báo riêng của tài khoản đang đăng nhập
 * CHỈ LẤY CÁC THÔNG BÁO ĐẶT HÀNG THÀNH CÔNG
 */
export const fetchNotifications = async () => {
  let stored = null
  try {
    stored = JSON.parse(localStorage.getItem('fftshirt_client_user') || 'null')
  } catch (e) {}

  // KHÔNG ĐĂNG NHẬP THÌ PHẦN THÔNG BÁO KHÔNG CÓ
  if (!stored || (!stored.id && !stored.email && !stored.phone)) {
    notifications.value = []
    loadingNotifications.value = false
    return
  }

  // Tải lại danh sách ID đã đọc riêng cho tài khoản này
  readIds.value = loadReadIds()

  loadingNotifications.value = true
  const list = []

  try {
    const resOrders = await api.get('/api/hoa-don')
    const allOrders = Array.isArray(resOrders.data) ? resOrders.data : []
    const userPhone = stored.phone ? String(stored.phone).trim() : ''
    const userEmail = stored.email ? String(stored.email).trim().toLowerCase() : ''
    const userId = stored.id

    // Lọc đơn hàng thuộc về chính tài khoản này
    const myOrders = allOrders
      .filter(o => {
        if (userId && o.khachHang && o.khachHang.id === userId) return true
        if (userEmail && o.emailKhachHang && String(o.emailKhachHang).trim().toLowerCase() === userEmail) return true
        if (userPhone && o.soDienThoaiKhachHang && String(o.soDienThoaiKhachHang).trim() === userPhone) return true
        return false
      })
      .sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))

    // Chỉ tạo thông báo đặt hàng thành công
    myOrders.forEach(o => {
      const orderCode = o.maHoaDon || `HD${o.id}`
      const notiId = `order-placed-${o.id}`
      const totalFmt = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(o.tienSauGiamGia || o.tongTien || 0)

      list.push({
        id: notiId,
        category: 'order',
        title: `🎉 Đặt hàng thành công! Đơn #${orderCode}`,
        desc: `Đơn hàng #${orderCode} đã được tạo thành công trên hệ thống. Tổng thanh toán: ${totalFmt}.`,
        statusBadge: 'Đặt thành công',
        time: formatTimeAgo(o.ngayTao),
        timestamp: new Date(o.ngayTao || Date.now()).getTime(),
        unread: !readIds.value.has(notiId),
        link: `/don-hang?code=${orderCode}`,
        actionText: 'Theo dõi đơn hàng'
      })
    })
  } catch (err) {
    console.warn('Lỗi lấy thông báo đơn hàng riêng tài khoản:', err)
  }

  // Sắp xếp thông báo mới nhất lên đầu
  list.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))
  notifications.value = list
  loadingNotifications.value = false
}

export const unreadCount = computed(() => {
  return notifications.value.filter(n => n.unread).length
})

export const markAsRead = (id) => {
  if (!id) return
  readIds.value.add(id)
  saveReadIds(readIds.value)
  const item = notifications.value.find(n => n.id === id)
  if (item) {
    item.unread = false
  }
}

export const markAllAsRead = () => {
  notifications.value.forEach(n => {
    n.unread = false
    readIds.value.add(n.id)
  })
  saveReadIds(readIds.value)
}

/**
 * Thêm ngay thông báo đặt hàng thành công khi vừa hoàn tất đặt hàng
 */
export const notifyOrderSuccess = (order) => {
  if (!order) return
  const orderCode = order.maHoaDon || `HD${order.id || ''}`
  const notiId = `order-placed-${order.id || Date.now()}`
  const totalFmt = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.tienSauGiamGia || order.tongTien || 0)

  const newNoti = {
    id: notiId,
    category: 'order',
    title: `🎉 Đặt hàng thành công! Đơn #${orderCode}`,
    desc: `Đơn hàng #${orderCode} đã được tạo thành công trên hệ thống. Tổng thanh toán: ${totalFmt}.`,
    statusBadge: 'Đặt thành công',
    time: 'Vừa xong',
    timestamp: Date.now(),
    unread: true,
    link: `/don-hang?code=${orderCode}`,
    actionText: 'Theo dõi đơn hàng'
  }

  notifications.value.unshift(newNoti)
}
