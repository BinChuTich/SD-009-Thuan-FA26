// Service tích hợp API hành chính Việt Nam: https://provinces.open-api.vn/
// Cấu trúc rút gọn 2 cấp: Tỉnh / Thành phố và Phường / Xã
import axios from 'axios'

const PROVINCES_BASE_URL = 'https://provinces.open-api.vn/api'

// Cache để tránh gọi lại nhiều lần và tăng tốc độ tối đa
let provincesCache = null
const provinceWardsCache = new Map()

export async function getProvinces() {
  if (provincesCache) return provincesCache
  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/`)
    provincesCache = res.data || []
    return provincesCache
  } catch (error) {
    console.error('Lỗi khi tải danh sách tỉnh thành:', error)
    return []
  }
}

// Lấy danh sách toàn bộ Phường / Xã của một Tỉnh / Thành phố (rút gọn 2 cấp)
export async function getWardsByProvince(provinceCode) {
  if (!provinceCode) return []
  if (provinceWardsCache.has(provinceCode)) {
    return provinceWardsCache.get(provinceCode)
  }
  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/${provinceCode}?depth=3`)
    const districts = res.data?.districts || []
    const allWards = []
    districts.forEach(d => {
      (d.wards || []).forEach(w => {
        allWards.push({
          code: w.code,
          name: w.name,
          districtName: d.name
        })
      })
    })
    // Sắp xếp tiếng Việt theo tên phường xã
    allWards.sort((a, b) => a.name.localeCompare(b.name, 'vi'))
    provinceWardsCache.set(provinceCode, allWards)
    return allWards
  } catch (error) {
    console.error(`Lỗi khi tải danh sách phường xã cho tỉnh ${provinceCode}:`, error)
    return []
  }
}

// Khớp địa chỉ dạng chuỗi (từ CCCD) với cấu trúc 2 cấp Tỉnh / Thành phố - Phường / Xã
export async function matchAddressHierarchy(fullAddressString) {
  if (!fullAddressString || typeof fullAddressString !== 'string') {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: '' }
  }

  const provinces = await getProvinces()
  const cleanStr = (s) => (s || '').toLowerCase()
    .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')
    .trim()

  const parts = fullAddressString.split(',').map(p => p.trim()).filter(Boolean)
  if (parts.length === 0) {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: fullAddressString }
  }

  // 1. Tìm Tỉnh / Thành phố (thường ở cuối chuỗi)
  let foundProvince = null
  let provPartIndex = -1

  for (let i = parts.length - 1; i >= 0; i--) {
    const partClean = cleanStr(parts[i])
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

  // 2. Tìm Phường / Xã trực tiếp trong Tỉnh đó
  const wards = await getWardsByProvince(foundProvince.code)
  let foundWard = null
  let wardPartIndex = -1

  for (let i = provPartIndex - 1; i >= 0; i--) {
    const partClean = cleanStr(parts[i])
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

  // 3. Địa chỉ cụ thể là các phần còn lại trước phường/xã (hoặc trước tỉnh)
  const cutoffIndex = wardPartIndex !== -1 ? wardPartIndex : provPartIndex
  const specificParts = parts.slice(0, cutoffIndex)
  const specificAddress = specificParts.length > 0 ? specificParts.join(', ') : fullAddressString

  return {
    provinceCode: foundProvince.code,
    provinceName: foundProvince.name,
    wardCode: foundWard ? foundWard.code : null,
    wardName: foundWard ? foundWard.name : '',
    specificAddress
  }
}
