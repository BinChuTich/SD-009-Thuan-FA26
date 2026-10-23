/**
 * Quản lý dữ liệu địa giới hành chính 2 cấp mới nhất của Việt Nam (34 Tỉnh / Thành phố và Phường / Xã)
 * Đồng bộ với chuẩn sáp nhập địa giới hành chính 2025 - 2026 và backend AdministrativeRegionService
 */

// Danh sách 34 Tỉnh / Thành phố sau sáp nhập
export const FALLBACK_PROVINCES = [
  'Thành phố Hà Nội', 'Thành phố Hồ Chí Minh', 'Thành phố Hải Phòng', 'Thành phố Đà Nẵng', 'Thành phố Cần Thơ',
  'Thành phố Bắc Ninh', 'Thành phố Quảng Ninh', 'Thành phố Huế', 'Thành phố Đồng Nai',
  'Tỉnh Cao Bằng', 'Tỉnh Tuyên Quang', 'Tỉnh Điện Biên', 'Tỉnh Lai Châu', 'Tỉnh Sơn La', 'Tỉnh Lào Cai',
  'Tỉnh Thái Nguyên', 'Tỉnh Lạng Sơn', 'Tỉnh Phú Thọ', 'Tỉnh Hưng Yên', 'Tỉnh Ninh Bình',
  'Tỉnh Thanh Hóa', 'Tỉnh Nghệ An', 'Tỉnh Hà Tĩnh', 'Tỉnh Quảng Trị', 'Tỉnh Quảng Ngãi',
  'Tỉnh Gia Lai', 'Tỉnh Khánh Hòa', 'Tỉnh Đắk Lắk', 'Tỉnh Lâm Đồng', 'Tỉnh Tây Ninh',
  'Tỉnh Đồng Tháp', 'Tỉnh Vĩnh Long', 'Tỉnh An Giang', 'Tỉnh Cà Mau'
]

let cachedRegions = null

/**
 * Tải động dữ liệu chi tiết 34 tỉnh thành và các phường xã trực thuộc (Code-split tối ưu bundle)
 */
export async function loadAdministrativeData() {
  if (cachedRegions) return cachedRegions
  try {
    const mod = await import('@/data/administrative_regions_v2.json')
    cachedRegions = mod.default || mod
    return cachedRegions
  } catch (err) {
    console.error('Lỗi tải dữ liệu địa giới hành chính:', err)
    return []
  }
}

/**
 * Lấy danh sách phường xã theo tên Tỉnh / Thành phố
 */
export async function getWardsForProvince(provinceName) {
  if (!provinceName) return []
  const data = await loadAdministrativeData()
  const norm = provinceName.trim().toLowerCase()
  const found = data.find(p => {
    if (!p.name) return false
    const pNorm = p.name.trim().toLowerCase()
    return pNorm === norm || norm.includes(pNorm) || pNorm.includes(norm)
  })
  return found && Array.isArray(found.wards) ? found.wards : []
}

