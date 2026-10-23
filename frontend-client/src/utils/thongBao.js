/**
 * Quản lý hệ thống thông báo khách hàng (Client Notifications)
 * Đồng bộ đơn hàng thực tế từ DB, chương trình giảm giá và voucher ưu đãi
 */
import { ref, computed } from 'vue'
import api from '@/api'

const READ_STORAGE_KEY = 'fftshirt_read_notifications'

const loadReadIds = () => {
  try {
    const raw = localStorage.getItem(READ_STORAGE_KEY)
    return raw ? new Set(JSON.parse(raw)) : new Set()
  } catch (e) {
    return new Set()
  }
}

const saveReadIds = (set) => {
  try {
    localStorage.setItem(READ_STORAGE_KEY, JSON.stringify(Array.from(set)))
  } catch (e) {
    console.warn('Lỗi lưu trạng thái đã đọc thông báo:', e)
  }
}

export const readIds = ref(loadReadIds())
export const notifications = ref([])
export const loadingNotifications = ref(false)

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

const getOrderStatusInfo = (trangThai) => {
  switch (Number(trangThai)) {
    case 1:
      return {
        title: 'Đơn hàng mới đã tạo thành công',
        desc: 'Đơn hàng đang chờ nhân viên kiểm tra và xác nhận đóng gói.',
        badge: 'Chờ xác nhận'
      }
    case 2:
      return {
        title: 'Đơn hàng đã được xác nhận',
        desc: 'Shop đang tiến hành phân loại và đóng gói áo phông cẩn thận.',
        badge: 'Đã xác nhận'
      }
    case 3:
      return {
        title: 'Đơn hàng đã giao cho bưu tá',
        desc: 'Kiện hàng đã rời kho và đang trên lộ trình trung chuyển.',
        badge: 'Chờ giao hàng'
      }
    case 4:
      return {
        title: 'Đơn hàng đang được giao tới bạn',
        desc: 'Shipper đang trên đường giao hàng, bạn vui lòng để ý điện thoại nhé.',
        badge: 'Đang giao'
      }
    case 5:
      return {
        title: 'Đơn hàng giao thành công',
        desc: 'Cảm ơn bạn đã lựa chọn FF T-Shirt. Chúc bạn có trải nghiệm tuyệt vời cùng sản phẩm!',
        badge: 'Hoàn thành'
      }
    case 6:
      return {
        title: 'Đơn hàng đã hủy',
        desc: 'Đơn hàng đã được ghi nhận hủy trên hệ thống.',
        badge: 'Đã hủy'
      }
    default:
      return {
        title: 'Cập nhật trạng thái đơn hàng',
        desc: 'Trạng thái đơn hàng của bạn vừa được cập nhật trên hệ thống.',
        badge: 'Thông báo'
      }
  }
}

export const fetchNotifications = async () => {
  loadingNotifications.value = true
  const list = []

  // 1. Lấy đơn hàng thực tế của khách hàng từ DB
  try {
    const resOrders = await api.get('/api/hoa-don')
    const allOrders = Array.isArray(resOrders.data) ? resOrders.data : []
    // Lấy tối đa 5 đơn gần nhất của khách
    const userPhone = '0988776655'
    const myOrders = allOrders
      .filter(o => !o.soDienThoaiKhachHang || o.soDienThoaiKhachHang === userPhone || o.tenKhachHang?.includes('Chiến'))
      .sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))
      .slice(0, 5)

    myOrders.forEach(o => {
      const statusInfo = getOrderStatusInfo(o.trangThai)
      const orderCode = o.maHoaDon || `HD${o.id}`
      const notiId = `order-${o.id}-${o.trangThai || 1}`
      const totalFmt = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(o.tienSauGiamGia || o.tongTien || 0)

      list.push({
        id: notiId,
        category: 'order', // order | deal | voucher | system
        title: `📦 #${orderCode}: ${statusInfo.title}`,
        desc: `${statusInfo.desc} (Tổng tiền: ${totalFmt})`,
        statusBadge: statusInfo.badge,
        time: formatTimeAgo(o.ngayCapNhat || o.ngayTao),
        timestamp: new Date(o.ngayCapNhat || o.ngayTao || Date.now()).getTime(),
        unread: !readIds.value.has(notiId),
        link: `/don-hang?code=${orderCode}`,
        actionText: 'Xem chi tiết đơn'
      })
    })
  } catch (err) {
    console.warn('Lỗi lấy thông báo đơn hàng:', err)
  }

  // 2. Lấy đợt giảm giá đang chạy thực tế từ DB
  try {
    const resDeals = await api.get('/api/dot-giam-gia/active-products')
    const deals = Array.isArray(resDeals.data) ? resDeals.data : []
    deals.slice(0, 2).forEach(d => {
      const notiId = `deal-${d.id}`
      const name = d.tenDotGiamGia || 'Flash Sale Đặc Biệt'
      const pct = d.phanTramGiam ? `${d.phanTramGiam}%` : 'cực sốc'
      list.push({
        id: notiId,
        category: 'deal',
        title: `🔥 Ưu đãi lớn: ${name}`,
        desc: `Chương trình giảm giá ${pct} cho toàn bộ dòng áo phông cao cấp đang diễn ra. Đừng bỏ lỡ!`,
        time: formatTimeAgo(d.ngayBatDau),
        timestamp: new Date(d.ngayBatDau || Date.now()).getTime(),
        unread: !readIds.value.has(notiId),
        link: '/#san-pham-uu-dai',
        actionText: 'Săn ưu đãi ngay'
      })
    })
  } catch (err) {
    console.warn('Lỗi lấy thông báo giảm giá:', err)
  }

  // 3. Thông báo Vouchers quà tặng độc quyền
  const voucherNoti1 = {
    id: 'voucher-freeship',
    category: 'voucher',
    title: '🎟️ Tặng bạn Mã FREESHIP toàn quốc',
    desc: 'Nhận ngay miễn phí vận chuyển tối đa 30.000đ cho đơn từ 250.000đ. Nhấn để lưu mã!',
    voucherCode: 'FREESHIP',
    time: 'Hôm nay',
    timestamp: Date.now() - 3600000,
    unread: !readIds.value.has('voucher-freeship'),
    link: '/cua-hang',
    actionText: 'Dùng mã mua sắm'
  }

  const voucherNoti2 = {
    id: 'voucher-vip10',
    category: 'voucher',
    title: '🎁 Mã ưu đãi FFVIP10 - Giảm thêm 10%',
    desc: 'Đặc quyền thành viên: Giảm trực tiếp 10% khi mua các mẫu áo polo và áo phông unisex.',
    voucherCode: 'FFVIP10',
    time: 'Hôm qua',
    timestamp: Date.now() - 86400000,
    unread: !readIds.value.has('voucher-vip10'),
    link: '/cua-hang',
    actionText: 'Dùng mã mua sắm'
  }

  list.push(voucherNoti1)
  list.push(voucherNoti2)

  // 4. Thông báo Hệ thống / Lời chào
  const welcomeNoti = {
    id: 'system-welcome',
    category: 'system',
    title: '👋 Chào mừng bạn đến với FF T-Shirt',
    desc: 'Trải nghiệm chất vải 100% Cotton tự nhiên thoáng mát, bo cổ 2 kim chống nhão chuẩn form.',
    time: '2 ngày trước',
    timestamp: Date.now() - 172800000,
    unread: !readIds.value.has('system-welcome'),
    link: '/cua-hang',
    actionText: 'Xem sản phẩm mới'
  }
  list.push(welcomeNoti)

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

