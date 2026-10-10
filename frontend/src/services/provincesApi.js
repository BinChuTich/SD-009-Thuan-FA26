// Service tích hợp API hành chính Việt Nam: https://provinces.open-api.vn/
// Cung cấp đầy đủ 63 Tỉnh / Thành phố và cấu trúc 2 cấp (Tỉnh / Thành phố & Phường / Xã)
import axios from 'axios'

const PROVINCES_BASE_URL = 'https://provinces.open-api.vn/api'

// Cache bộ nhớ trong phiên làm việc để phản hồi tức thì (0ms)
let provincesCache = null
const provinceWardsCache = new Map()

/**
 * Lấy danh sách đầy đủ 63 Tỉnh / Thành phố Việt Nam
 */
export async function getProvinces() {
  if (provincesCache && provincesCache.length > 0) return provincesCache

  // Thử đọc từ sessionStorage
  try {
    const local = sessionStorage.getItem('cached_vn_provinces_v1')
    if (local) {
      provincesCache = JSON.parse(local)
      if (Array.isArray(provincesCache) && provincesCache.length > 0) {
        return provincesCache
      }
    }
  } catch (e) {}

  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/`)
    provincesCache = res.data || []
    try {
      sessionStorage.setItem('cached_vn_provinces_v1', JSON.stringify(provincesCache))
    } catch (e) {}
    return provincesCache
  } catch (error) {
    console.error('Lỗi khi tải danh sách 63 tỉnh thành Việt Nam:', error)
    return []
  }
}

/**
 * Lấy danh sách toàn bộ Phường / Xã của một Tỉnh / Thành phố
 * Rút gọn trực tiếp 2 cấp: Tỉnh / Thành phố -> Phường / Xã (bỏ qua cấp Quận/Huyện)
 */
export async function getWardsByProvince(provinceCode) {
  if (!provinceCode) return []
  const numCode = Number(provinceCode)
  if (isNaN(numCode)) return []

  if (provinceWardsCache.has(numCode)) {
    return provinceWardsCache.get(numCode)
  }

  // Thử đọc từ sessionStorage
  try {
    const local = sessionStorage.getItem(`cached_vn_wards_${numCode}`)
    if (local) {
      const parsed = JSON.parse(local)
      if (Array.isArray(parsed) && parsed.length > 0) {
        provinceWardsCache.set(numCode, parsed)
        return parsed
      }
    }
  } catch (e) {}

  try {
    // Lấy thông tin tỉnh cùng toàn bộ các quận/huyện và phường/xã (depth=3)
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/${numCode}?depth=3`)
    const districts = res.data?.districts || []
    
    // Gộp phẳng toàn bộ phường/xã từ tất cả các quận/huyện trong tỉnh
    const wards = districts.flatMap(d => (d.wards || []).map(w => ({
      code: w.code,
      name: w.name,
      divisionType: w.division_type,
      codename: w.codename,
      districtCode: d.code,
      districtName: d.name,
      displayName: `${w.name} (${d.name})`
    })))

    // Sắp xếp tiếng Việt theo tên phường/xã
    wards.sort((a, b) => a.name.localeCompare(b.name, 'vi'))

    provinceWardsCache.set(numCode, wards)
    try {
      sessionStorage.setItem(`cached_vn_wards_${numCode}`, JSON.stringify(wards))
    } catch (e) {}
    return wards
  } catch (error) {
    console.error(`Lỗi khi tải danh sách phường xã cho tỉnh mã ${provinceCode}:`, error)
    return []
  }
}

/**
 * Khớp địa chỉ chuỗi (quét từ CCCD hoặc dữ liệu cũ) với 2 cấp Tỉnh/Thành phố & Phường/Xã
 */
export async function matchAddressHierarchy(fullAddressString) {
  if (!fullAddressString || typeof fullAddressString !== 'string') {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: '' }
  }

  const provinces = await getProvinces()
  const cleanStr = (s) => (s || '').toLowerCase()
    .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')
    .trim()

  // Phân tách các phần tử theo dấu phẩy hoặc chấm phẩy
  const parts = fullAddressString.split(/[,;\n]+/).map(p => p.trim()).filter(Boolean)
  if (parts.length === 0) {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: fullAddressString }
  }

  // 1. Tìm Tỉnh / Thành phố (duyệt từ cuối chuỗi lên)
  let foundProvince = null
  let provPartIndex = -1

  for (let i = parts.length - 1; i >= 0; i--) {
    const partClean = cleanStr(parts[i])
    if (!partClean) continue
    const match = provinces.find(p => {
      const pClean = cleanStr(p.name)
      return pClean === partClean || p.name.toLowerCase().includes(partClean) || parts[i].toLowerCase().includes(pClean)
    })
    if (match) {
      foundProvince = match
      provPartIndex = i
      break
    }
  }

  if (!foundProvince) {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: fullAddressString }
  }

  // 2. Tìm Phường / Xã trực tiếp trong danh sách các phường/xã của Tỉnh đó
  const wards = await getWardsByProvince(foundProvince.code)
  let foundWard = null
  let wardPartIndex = -1

  // Ưu tiên 1: Khớp chính xác tên đầy đủ của phường/xã
  for (let i = provPartIndex - 1; i >= 0; i--) {
    const rawPart = parts[i].toLowerCase()
    const matchExact = wards.find(w => w.name.toLowerCase() === rawPart)
    if (matchExact) {
      foundWard = matchExact
      wardPartIndex = i
      break
    }
  }

  // Ưu tiên 2: Khớp theo tên đã chuẩn hóa (bỏ tiền tố Phường / Xã / Thị trấn)
  if (!foundWard) {
    for (let i = provPartIndex - 1; i >= 0; i--) {
      const partClean = cleanStr(parts[i])
      if (!partClean) continue
      const match = wards.find(w => {
        const wClean = cleanStr(w.name)
        return wClean === partClean || w.name.toLowerCase().includes(partClean) || parts[i].toLowerCase().includes(wClean)
      })
      if (match) {
        foundWard = match
        wardPartIndex = i
        break
      }
    }
  }

  // 3. Địa chỉ cụ thể là các phần còn lại đứng trước Phường/Xã (hoặc trước Tỉnh)
  const cutoffIndex = wardPartIndex !== -1 ? wardPartIndex : provPartIndex
  const specificParts = parts.slice(0, cutoffIndex)
  const specificAddress = specificParts.length > 0 ? specificParts.join(', ') : fullAddressString

  return {
    provinceCode: foundProvince.code,
    provinceName: foundProvince.name,
    wardCode: foundWard ? foundWard.code : null,
    wardName: foundWard ? foundWard.name : '',
    wardDisplayName: foundWard ? foundWard.displayName : '',
    districtName: foundWard ? foundWard.districtName : '',
    specificAddress
  }
}
