import { generateQrDataUrl } from '../../../frontend/src/utils/qrcode.js'

export async function createBrandedQrDataUrl(content, brand = 'vietqr') {
  if (!String(content || '').trim()) throw new Error('Nội dung QR không hợp lệ')
  return generateQrDataUrl(content, {
    margin: 4,
    errorLevel: 'H',
    centerLogo: brand === 'momo' ? 'momo' : 'vietqr'
  })
}
