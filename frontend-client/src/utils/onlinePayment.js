export const getOnlinePaymentProvider = (order) => {
  if (!order) return ''
  const text = `${order.phuongThucThanhToan || ''} ${order.ghiChu || ''}`.toUpperCase()
  if (text.includes('MOMO')) return 'MOMO'
  if (text.includes('VIETQR') || text.includes('CHUYỂN KHOẢN VIETQR')) return 'VIETQR'
  if (text.includes('VNPAY') || order.isVnPay === true) return 'VNPAY'
  return ''
}

export const isPendingOnlinePayment = (order) => Boolean(
  getOnlinePaymentProvider(order) &&
  Number(order?.trangThaiThanhToan || 0) !== 1 &&
  Number(order?.trangThai) === 1
)

export const changeOnlinePaymentMethod = async (order, method, api) => {
  if (!order?.maHoaDon) throw new Error('Không tìm thấy mã đơn hàng.')
  const normalized = String(method || '').toUpperCase()
  if (!['COD', 'VIETQR', 'MOMO', 'VNPAY'].includes(normalized)) {
    throw new Error('Phương thức thanh toán không hợp lệ.')
  }
  const response = await api.put(`/api/hoa-don/code/${order.maHoaDon}/doi-phuong-thuc-thanh-toan`, {
    phuongThucThanhToan: normalized
  })
  return response.data
}

export const continueOnlinePayment = async (order, api, router) => {
  const provider = getOnlinePaymentProvider(order)
  if (!provider) throw new Error('Phương thức thanh toán không hỗ trợ tiếp tục thanh toán.')
  const code = order.maHoaDon
  const amount = Math.max(1, Math.round(Number(order.tienSauGiamGia || order.tongTienSauGiam || order.tongTien || 0)))

  if (provider === 'MOMO') {
    const response = await api.post('/api/momo/create-payment', { orderId: code })
    if (!response.data?.paymentUrl) throw new Error('Không tạo được liên kết thanh toán MoMo.')
    const target = new URL(response.data.paymentUrl, window.location.origin)
    if (['localhost', '127.0.0.1'].includes(window.location.hostname)) {
      try {
        const lan = await api.get('/api/pos-sync/lan-ip', { timeout: 3000 })
        target.hostname = lan.data?.primaryIp || window.location.hostname
        target.port = window.location.port || target.port || '5174'
        target.protocol = window.location.protocol
      } catch (e) {}
    } else {
      target.protocol = window.location.protocol
      target.host = window.location.host
    }
    return { provider, code, amount, phoneUrl: target.toString(), qrImage: await createBrandedQrDataUrl(target.toString(), 'momo') }
  }

  if (provider === 'VIETQR') {
    let token = ''
    try {
      const response = await api.post('/api/payment/demo/create', null, {
        params: { maHoaDon: code, soTien: amount },
        timeout: 8000
      })
      token = String(response.data?.token || '')
    } catch (e) {
      console.warn('Backend chưa hỗ trợ phiên VietQR demo, dùng chế độ tương thích:', e)
    }
    let origin = window.location.origin
    if (['localhost', '127.0.0.1'].includes(window.location.hostname)) {
      try {
        const lan = await api.get('/api/pos-sync/lan-ip', { timeout: 3000 })
        if (lan.data?.primaryIp) origin = `${window.location.protocol}//${lan.data.primaryIp}:${window.location.port || '5174'}`
      } catch (e) {}
    }
    const query = new URLSearchParams({ c: code, a: String(amount) })
    if (token) query.set('t', token)
    const phoneUrl = `${origin}/thanh-toan/vietqr-demo?${query}`
    return { provider, code, amount, phoneUrl, qrImage: await createBrandedQrDataUrl(phoneUrl, 'vietqr') }
  }

  const response = await api.post('/api/vnpay/create-payment', {
    orderCode: code,
    amount,
    orderInfo: `Thanh toan don hang ${code}`
  })
  if (!response.data?.paymentUrl) throw new Error('Không tạo được liên kết thanh toán VNPAY.')
  const width = 850
  const height = 750
  window.open(
    response.data.paymentUrl,
    'VNPayPaymentPopup',
    `width=${width},height=${height},top=${(window.screen.height-height)/2},left=${(window.screen.width-width)/2},scrollbars=yes,status=no,resizable=yes`
  )
  return { provider, code, amount }
}
import { createBrandedQrDataUrl } from '@/utils/brandedQr.js'

