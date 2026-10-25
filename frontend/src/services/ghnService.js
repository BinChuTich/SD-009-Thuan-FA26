/**
 * GHN Service - Tích hợp API Giao Hàng Nhanh & Chính sách tính phí theo khu vực
 * Hỗ trợ đầy đủ cấu trúc 3 cấp hành chính: Tỉnh/Thành phố -> Quận/Huyện -> Phường/Xã
 */
import axios from 'axios'
import api from '@/api.js'

// Cấu hình GHN Dev / Sandbox
export const GHN_CONFIG = {
  API_URL: 'https://dev-online-gateway.ghn.vn/shiip/public-api',
  TOKEN: 'd007d135-e1fb-11ee-8bfa-8a2dda8ec551',
  SHOP_ID: 191437,
  SHOP_DISTRICT_ID: 1454, // Nam Từ Liêm, Hà Nội
  SHOP_WARD_CODE: '20109' // Mỹ Đình 1
}

// Cache bộ nhớ trong phiên làm việc
let provincesCache = null
const districtsCache = new Map()
const wardsCache = new Map()

// Danh sách 63 Tỉnh/Thành phố chuẩn kèm mã GHN ProvinceID
const DEFAULT_VN_PROVINCES = [
  { id: 201, code: 201, name: 'Hà Nội', region: 'Bắc' },
  { id: 202, code: 202, name: 'Hồ Chí Minh', region: 'Nam' },
  { id: 203, code: 203, name: 'Đà Nẵng', region: 'Trung' },
  { id: 204, code: 204, name: 'Hải Phòng', region: 'Bắc' },
  { id: 205, code: 205, name: 'Cần Thơ', region: 'Nam' },
  { id: 206, code: 206, name: 'An Giang', region: 'Nam' },
  { id: 207, code: 207, name: 'Bà Rịa - Vũng Tàu', region: 'Nam' },
  { id: 208, code: 208, name: 'Bắc Giang', region: 'Bắc' },
  { id: 209, code: 209, name: 'Bắc Kạn', region: 'Bắc' },
  { id: 210, code: 210, name: 'Bạc Liêu', region: 'Nam' },
  { id: 211, code: 211, name: 'Bắc Ninh', region: 'Bắc' },
  { id: 212, code: 212, name: 'Bến Tre', region: 'Nam' },
  { id: 213, code: 213, name: 'Bình Định', region: 'Trung' },
  { id: 214, code: 214, name: 'Bình Dương', region: 'Nam' },
  { id: 215, code: 215, name: 'Bình Phước', region: 'Nam' },
  { id: 216, code: 216, name: 'Bình Thuận', region: 'Trung' },
  { id: 217, code: 217, name: 'Cà Mau', region: 'Nam' },
  { id: 218, code: 218, name: 'Cao Bằng', region: 'Bắc' },
  { id: 219, code: 219, name: 'Đắk Lắk', region: 'Trung' },
  { id: 220, code: 220, name: 'Đắk Nông', region: 'Trung' },
  { id: 221, code: 221, name: 'Điện Biên', region: 'Bắc' },
  { id: 222, code: 222, name: 'Đồng Nai', region: 'Nam' },
  { id: 223, code: 223, name: 'Đồng Tháp', region: 'Nam' },
  { id: 224, code: 224, name: 'Gia Lai', region: 'Trung' },
  { id: 225, code: 225, name: 'Hà Giang', region: 'Bắc' },
  { id: 226, code: 226, name: 'Hà Nam', region: 'Bắc' },
  { id: 227, code: 227, name: 'Hà Tĩnh', region: 'Trung' },
  { id: 228, code: 228, name: 'Hải Dương', region: 'Bắc' },
  { id: 229, code: 229, name: 'Hậu Giang', region: 'Nam' },
  { id: 230, code: 230, name: 'Hòa Bình', region: 'Bắc' },
  { id: 231, code: 231, name: 'Hưng Yên', region: 'Bắc' },
  { id: 232, code: 232, name: 'Khánh Hòa', region: 'Trung' },
  { id: 233, code: 233, name: 'Kiên Giang', region: 'Nam' },
  { id: 234, code: 234, name: 'Kon Tum', region: 'Trung' },
  { id: 235, code: 235, name: 'Lai Châu', region: 'Bắc' },
  { id: 236, code: 236, name: 'Lâm Đồng', region: 'Trung' },
  { id: 237, code: 237, name: 'Lạng Sơn', region: 'Bắc' },
  { id: 238, code: 238, name: 'Lào Cai', region: 'Bắc' },
  { id: 239, code: 239, name: 'Long An', region: 'Nam' },
  { id: 240, code: 240, name: 'Nam Định', region: 'Bắc' },
  { id: 241, code: 241, name: 'Nghệ An', region: 'Trung' },
  { id: 242, code: 242, name: 'Ninh Bình', region: 'Bắc' },
  { id: 243, code: 243, name: 'Ninh Thuận', region: 'Trung' },
  { id: 244, code: 244, name: 'Phú Thọ', region: 'Bắc' },
  { id: 245, code: 245, name: 'Phú Yên', region: 'Trung' },
  { id: 246, code: 246, name: 'Quảng Bình', region: 'Trung' },
  { id: 247, code: 247, name: 'Quảng Nam', region: 'Trung' },
  { id: 248, code: 248, name: 'Quảng Ngãi', region: 'Trung' },
  { id: 249, code: 249, name: 'Quảng Ninh', region: 'Bắc' },
  { id: 250, code: 250, name: 'Quảng Trị', region: 'Trung' },
  { id: 251, code: 251, name: 'Sóc Trăng', region: 'Nam' },
  { id: 252, code: 252, name: 'Sơn La', region: 'Bắc' },
  { id: 253, code: 253, name: 'Tây Ninh', region: 'Nam' },
  { id: 254, code: 254, name: 'Thái Bình', region: 'Bắc' },
  { id: 255, code: 255, name: 'Thái Nguyên', region: 'Bắc' },
  { id: 256, code: 256, name: 'Thanh Hóa', region: 'Trung' },
  { id: 257, code: 257, name: 'Thừa Thiên Huế', region: 'Trung' },
  { id: 258, code: 258, name: 'Tiền Giang', region: 'Nam' },
  { id: 259, code: 259, name: 'Trà Vinh', region: 'Nam' },
  { id: 260, code: 260, name: 'Tuyên Quang', region: 'Bắc' },
  { id: 261, code: 261, name: 'Vĩnh Long', region: 'Nam' },
  { id: 262, code: 262, name: 'Vĩnh Phúc', region: 'Bắc' },
  { id: 263, code: 263, name: 'Yên Bái', region: 'Bắc' }
]

// Fallback danh sách Quận/Huyện phổ biến của Hà Nội & TP.HCM
const COMMON_DISTRICTS = {
  201: [ // Hà Nội
    { id: 1454, code: 1454, name: 'Quận Nam Từ Liêm' },
    { id: 1442, code: 1442, name: 'Quận Ba Đình' },
    { id: 1443, code: 1443, name: 'Quận Hoàn Kiếm' },
    { id: 1444, code: 1444, name: 'Quận Cầu Giấy' },
    { id: 1445, code: 1445, name: 'Quận Đống Đa' },
    { id: 1446, code: 1446, name: 'Quận Hai Bà Trưng' },
    { id: 1447, code: 1447, name: 'Quận Tây Hồ' },
    { id: 1448, code: 1448, name: 'Quận Thanh Xuân' },
    { id: 1449, code: 1449, name: 'Quận Hoàng Mai' },
    { id: 1450, code: 1450, name: 'Quận Long Biên' },
    { id: 1451, code: 1451, name: 'Quận Bắc Từ Liêm' },
    { id: 1452, code: 1452, name: 'Quận Hà Đông' },
    { id: 1453, code: 1453, name: 'Huyện Gia Lâm' },
    { id: 1455, code: 1455, name: 'Huyện Đông Anh' },
    { id: 1456, code: 1456, name: 'Huyện Sóc Sơn' },
    { id: 1457, code: 1457, name: 'Huyện Thanh Trì' },
    { id: 1458, code: 1458, name: 'Huyện Hoài Đức' },
    { id: 1459, code: 1459, name: 'Huyện Đan Phượng' },
    { id: 1460, code: 1460, name: 'Huyện Mê Linh' },
    { id: 1461, code: 1461, name: 'Huyện Thường Tín' }
  ],
  202: [ // TP. Hồ Chí Minh
    { id: 1441, code: 1441, name: 'Quận 1' },
    { id: 1450, code: 1450, name: 'Quận 3' },
    { id: 1451, code: 1451, name: 'Quận 5' },
    { id: 1452, code: 1452, name: 'Quận 7' },
    { id: 1453, code: 1453, name: 'Quận 10' },
    { id: 1460, code: 1460, name: 'Quận Bình Thạnh' },
    { id: 1461, code: 1461, name: 'Thành phố Thủ Đức' },
    { id: 1462, code: 1462, name: 'Quận Gò Vấp' },
    { id: 1463, code: 1463, name: 'Quận Tân Bình' },
    { id: 1464, code: 1464, name: 'Quận Phú Nhuận' },
    { id: 1465, code: 1465, name: 'Huyện Bình Chánh' },
    { id: 1466, code: 1466, name: 'Huyện Hóc Môn' }
  ]
}

const COMMON_WARDS = {
  1454: [ // Nam Từ Liêm
    { id: '20109', code: '20109', name: 'Phường Mỹ Đình 1' },
    { id: '20110', code: '20110', name: 'Phường Mỹ Đình 2' },
    { id: '20111', code: '20111', name: 'Phường Mễ Trì' },
    { id: '20112', code: '20112', name: 'Phường Phú Đô' },
    { id: '20113', code: '20113', name: 'Phường Tây Mỗ' },
    { id: '20114', code: '20114', name: 'Phường Đại Mỗ' },
    { id: '20115', code: '20115', name: 'Phường Trung Văn' },
    { id: '20116', code: '20116', name: 'Phường Cầu Diễn' }
  ],
  1444: [ // Cầu Giấy
    { id: '20101', code: '20101', name: 'Phường Dịch Vọng' },
    { id: '20102', code: '20102', name: 'Phường Dịch Vọng Hậu' },
    { id: '20103', code: '20103', name: 'Phường Quan Hoa' },
    { id: '20104', code: '20104', name: 'Phường Nghĩa Đô' },
    { id: '20105', code: '20105', name: 'Phường Nghĩa Tân' },
    { id: '20106', code: '20106', name: 'Phường Mai Dịch' },
    { id: '20107', code: '20107', name: 'Phường Yên Hòa' },
    { id: '20108', code: '20108', name: 'Phường Trung Hòa' }
  ],
  1445: [ // Đống Đa
    { id: '20201', code: '20201', name: 'Phường Láng Thượng' },
    { id: '20202', code: '20202', name: 'Phường Láng Hạ' },
    { id: '20203', code: '20203', name: 'Phường Ô Chợ Dừa' },
    { id: '20204', code: '20204', name: 'Phường Văn Miếu' },
    { id: '20205', code: '20205', name: 'Phường Quang Trung' }
  ]
}

/**
 * 1. Lấy danh sách Tỉnh / Thành phố
 */
export async function getGhnProvinces() {
  if (provincesCache && provincesCache.length > 0) return provincesCache

  try {
    const res = await axios.get(`${GHN_CONFIG.API_URL}/master-data/province`, {
      headers: { Token: GHN_CONFIG.TOKEN },
      timeout: 3500
    })
    if (res.data?.data && Array.isArray(res.data.data)) {
      provincesCache = res.data.data.map(p => ({
        id: p.ProvinceID,
        code: p.ProvinceID,
        name: p.ProvinceName,
        region: p.RegionID
      })).sort((a, b) => a.name.localeCompare(b.name, 'vi'))
      return provincesCache
    }
  } catch (e) {
    // Fallback sang danh sách chuẩn 63 tỉnh thành
  }

  provincesCache = [...DEFAULT_VN_PROVINCES].sort((a, b) => a.name.localeCompare(b.name, 'vi'))
  return provincesCache
}

/**
 * 2. Lấy danh sách Quận / Huyện theo Tỉnh / Thành phố
 */
export async function getGhnDistricts(provinceId) {
  if (!provinceId) return []
  const numId = Number(provinceId)
  if (districtsCache.has(numId)) return districtsCache.get(numId)

  try {
    const res = await axios.get(`${GHN_CONFIG.API_URL}/master-data/district?province_id=${numId}`, {
      headers: { Token: GHN_CONFIG.TOKEN },
      timeout: 3500
    })
    if (res.data?.data && Array.isArray(res.data.data)) {
      const list = res.data.data.map(d => ({
        id: d.DistrictID,
        code: d.DistrictID,
        name: d.DistrictName,
        provinceId: numId
      })).sort((a, b) => a.name.localeCompare(b.name, 'vi'))
      districtsCache.set(numId, list)
      return list
    }
  } catch (e) {
    // Fallback
  }

  if (COMMON_DISTRICTS[numId]) {
    const list = COMMON_DISTRICTS[numId]
    districtsCache.set(numId, list)
    return list
  }

  // Danh sách mẫu cho các tỉnh khác nếu offline
  const fallbackList = [
    { id: numId * 10 + 1, code: numId * 10 + 1, name: 'Thành phố / Thị xã trung tâm' },
    { id: numId * 10 + 2, code: numId * 10 + 2, name: 'Huyện khu vực 1' },
    { id: numId * 10 + 3, code: numId * 10 + 3, name: 'Huyện khu vực 2' }
  ]
  districtsCache.set(numId, fallbackList)
  return fallbackList
}

/**
 * 3. Lấy danh sách Phường / Xã theo Quận / Huyện
 */
export async function getGhnWards(districtId) {
  if (!districtId) return []
  const numId = Number(districtId)
  if (wardsCache.has(numId)) return wardsCache.get(numId)

  try {
    const res = await axios.get(`${GHN_CONFIG.API_URL}/master-data/ward?district_id=${numId}`, {
      headers: { Token: GHN_CONFIG.TOKEN },
      timeout: 3500
    })
    if (res.data?.data && Array.isArray(res.data.data)) {
      const list = res.data.data.map(w => ({
        id: String(w.WardCode),
        code: String(w.WardCode),
        name: w.WardName,
        districtId: numId
      })).sort((a, b) => a.name.localeCompare(b.name, 'vi'))
      wardsCache.set(numId, list)
      return list
    }
  } catch (e) {
    // Fallback
  }

  if (COMMON_WARDS[numId]) {
    const list = COMMON_WARDS[numId]
    wardsCache.set(numId, list)
    return list
  }

  const fallbackWards = [
    { id: `${numId}01`, code: `${numId}01`, name: 'Phường 1 / Thị trấn' },
    { id: `${numId}02`, code: `${numId}02`, name: 'Phường 2 / Xã trung tâm' },
    { id: `${numId}03`, code: `${numId}03`, name: 'Xã lân cận' }
  ]
  wardsCache.set(numId, fallbackWards)
  return fallbackWards
}

/**
 * 4. TÍNH PHÍ VẬN CHUYỂN: Tích hợp API GHN & Chính sách khu vực
 */
export async function calculateGhnShippingFee({
  toDistrictId,
  toWardCode,
  provinceName,
  districtName,
  wardName = '',
  fullAddress = '',
  weightGram = 500,
  insuranceValue = 0
}) {
  const cleanP = cleanVietnameseText(provinceName)
  const cleanD = cleanVietnameseText(districtName)
  const cleanW = cleanVietnameseText(wardName)
  const cleanFull = cleanVietnameseText(fullAddress)
  const combined = `${cleanP} ${cleanD} ${cleanW} ${cleanFull}`

  // 1. NỘI THÀNH HÀ NỘI: Áp dụng giá ship ưu đãi rẻ hơn các khu vực khác (20.000 VNĐ)
  if (cleanP.includes('ha noi') || cleanP.includes('hanoi') || combined.includes('ha noi') || combined.includes('hanoi')) {
    const outer = [
      'dong anh', 'gia lam', 'soc son', 'ba vi', 'chuong my', 'dan phuong', 
      'hoai duc', 'me linh', 'my duc', 'phu xuyen', 'phuc tho', 'quoc oai', 
      'thach that', 'thanh oai', 'thuong tin', 'ung hoa', 'son tay'
    ]
    const isOuter = outer.some(dist => combined.includes(dist))
    return {
      fee: isOuter ? 30000 : 20000,
      source: 'REGIONAL_POLICY',
      regionDescription: isOuter ? 'Ngoại thành Hà Nội (Tiêu chuẩn 1 ngày)' : 'Nội thành Hà Nội (Giao nhanh giá rẻ)'
    }
  }

  // 2. Thử gọi qua Backend proxy /api/ghn/calculate-fee
  try {
    const res = await api.post('/api/ghn/calculate-fee', {
      toDistrictId: toDistrictId ? Number(toDistrictId) : null,
      toWardCode: toWardCode ? String(toWardCode) : '',
      provinceName: provinceName || '',
      districtName: districtName || '',
      weight: weightGram,
      insuranceValue: insuranceValue || 0
    })
    if (res.data && res.data.success && res.data.fee != null) {
      return {
        fee: Number(res.data.fee),
        source: res.data.source || 'GHN_API',
        regionDescription: res.data.regionDescription || 'Tính phí GHN',
        message: res.data.message
      }
    }
  } catch (err) {
    // Fallback
  }

  // 3. Thử gọi trực tiếp API GHN từ Client nếu có toDistrictId & toWardCode
  if (toDistrictId && toWardCode) {
    try {
      const ghnRes = await axios.post(`${GHN_CONFIG.API_URL}/v2/shipping-order/fee`, {
        service_type_id: 2,
        from_district_id: GHN_CONFIG.SHOP_DISTRICT_ID,
        to_district_id: Number(toDistrictId),
        to_ward_code: String(toWardCode).trim(),
        height: 10,
        length: 20,
        width: 15,
        weight: weightGram || 500,
        insurance_value: Math.min(Number(insuranceValue || 0), 5000000)
      }, {
        headers: {
          Token: GHN_CONFIG.TOKEN,
          ShopId: GHN_CONFIG.SHOP_ID,
          'Content-Type': 'application/json'
        },
        timeout: 4000
      })

      if (ghnRes.data?.code === 200 && ghnRes.data?.data?.total != null) {
        return {
          fee: Number(ghnRes.data.data.total),
          source: 'GHN_API',
          regionDescription: 'Giao Hàng Nhanh (API Trực tiếp)',
          message: 'Tính phí vận chuyển thành công qua API GHN'
        }
      }
    } catch (e) {}
  }

  // 4. Chính sách tính phí theo khu vực địa lý cho các tỉnh thành khác
  return calculateLocalRegionalFee(provinceName, districtName, wardName, fullAddress)
}

/**
 * Chính sách phí theo khu vực địa lý chuẩn
 */
export function calculateLocalRegionalFee(provinceName = '', districtName = '', wardName = '', fullAddress = '') {
  const p = cleanVietnameseText(provinceName)
  const d = cleanVietnameseText(districtName)
  const w = cleanVietnameseText(wardName)
  const f = cleanVietnameseText(fullAddress)
  const combined = `${p} ${d} ${w} ${f}`

  // 1. Hà Nội (Nội thành rẻ hơn các khu khác)
  if (p.includes('ha noi') || p.includes('hanoi') || combined.includes('ha noi') || combined.includes('hanoi')) {
    const outer = [
      'dong anh', 'gia lam', 'soc son', 'ba vi', 'chuong my', 'dan phuong', 
      'hoai duc', 'me linh', 'my duc', 'phu xuyen', 'phuc tho', 'quoc oai', 
      'thach that', 'thanh oai', 'thuong tin', 'ung hoa', 'son tay'
    ]
    const isOuter = outer.some(dist => combined.includes(dist))
    return {
      fee: isOuter ? 30000 : 20000,
      source: 'REGIONAL_POLICY',
      regionDescription: isOuter ? 'Ngoại thành Hà Nội (Tiêu chuẩn 1 ngày)' : 'Nội thành Hà Nội (Giao nhanh giá rẻ)'
    }
  }

  // 2. Miền Bắc lân cận
  const north = ['bac ninh', 'hung yen', 'hai phong', 'hai duong', 'quang ninh', 'ha nam', 'nam dinh', 'ninh binh', 'thai binh', 'vinh phuc', 'phu tho', 'thai nguyen', 'bac giang', 'lang son', 'hoa binh', 'tuyen quang', 'yen bai']
  if (north.some(prov => combined.includes(prov))) {
    return {
      fee: 35000,
      source: 'REGIONAL_POLICY',
      regionDescription: 'Miền Bắc lân cận (1-2 ngày)'
    }
  }

  // 3. Miền Trung
  const central = ['thanh hoa', 'nghe an', 'ha tinh', 'quang binh', 'quang tri', 'thua thien hue', 'hue', 'da nang', 'quang nam', 'quang ngai', 'binh dinh', 'khanh hoa', 'dak lak']
  if (central.some(prov => combined.includes(prov))) {
    return {
      fee: 40000,
      source: 'REGIONAL_POLICY',
      regionDescription: 'Miền Trung (2-3 ngày)'
    }
  }

  // 4. Miền Nam & Toàn quốc
  return {
    fee: 45000,
    source: 'REGIONAL_POLICY',
    regionDescription: 'Miền Nam & Toàn quốc (3-4 ngày)'
  }
}

function cleanVietnameseText(str) {
  if (!str) return ''
  return str.toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/[^a-z0-9 ]/g, '')
    .trim()
}

