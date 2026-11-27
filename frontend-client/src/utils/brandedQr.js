import { generateQrDataUrl } from '../../../frontend/src/utils/qrcode.js'

export async function createBrandedQrDataUrl(content, brand = 'vietqr') {
  if (!String(content || '').trim()) throw new Error('Nội dung QR không hợp lệ')
  return generateQrDataUrl(content, {
    // Vùng trắng rộng hơn và không chèn logo vào ma trận dữ liệu để camera
    // điện thoại phổ thông có thể nhận diện ổn định ở mọi độ sáng/kích thước.
    margin: 6,
    // M cân bằng giữa dung lượng URL dài và khả năng phục hồi phần tâm có logo.
    // Bộ tạo QR hiện tự tăng tới version 10 nên không cần hạ xuống mức L.
    errorLevel: 'M'
  })
}
