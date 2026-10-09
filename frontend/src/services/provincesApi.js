// Service tích hợp API hành chính Việt Nam sau sáp nhập: https://provinces.open-api.vn/
import axios from 'axios'

const PROVINCES_BASE_URL = 'https://provinces.open-api.vn/api'

// Cache để tránh gọi lại nhiều lần và tăng tốc độ tối đa
let provincesCache = null
const districtsCache = new Map()
const wardsCache = new Map()

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

export async function getDistricts(provinceCode) {
  if (!provinceCode) return []
  if (districtsCache.has(provinceCode)) {
    return districtsCache.get(provinceCode)
  }
  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/${provinceCode}?depth=2`)
    const districts = res.data?.districts || []
    districtsCache.set(provinceCode, districts)
    return districts
  } catch (error) {
    console.error(`Lỗi khi tải danh sách quận huyện cho tỉnh ${provinceCode}:`, error)
    return []
  }
}

export async function getWards(districtCode) {
  if (!districtCode) return []
  if (wardsCache.has(districtCode)) {
    return wardsCache.get(districtCode)
  }
  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/d/${districtCode}?depth=2`)
    const wards = res.data?.wards || []
    wardsCache.set(districtCode, wards)
    return wards
  } catch (error) {
    console.error(`Lỗi khi tải danh sách phường xã cho huyện ${districtCode}:`, error)
    return []
  }
}

// Khớp địa chỉ dạng chuỗi (từ CCCD) với cấu trúc Tỉnh - Huyện - Xã
export async function matchAddressHierarchy(fullAddressString) {
  if (!fullAddressString || typeof fullAddressString !== 'string') {
    return { provinceCode: null, districtCode: null, wardCode: null, specificAddress: '' }
  }

  const provinces = await getProvinces()
  const cleanStr = (s) => (s || '').toLowerCase()
    .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')
    .trim()

  const parts = fullAddressString.split(',').map(p => p.trim()).filter(Boolean)
  if (parts.length === 0) {
    return { provinceCode: null, districtCode: null, wardCode: null, specificAddress: fullAddressString }
  }

  // 1. Tìm tỉnh / thành phố (thường ở cuối)
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
    return { provinceCode: null, districtCode: null, wardCode: null, specificAddress: fullAddressString }
  }

  // 2. Tìm quận / huyện trong tỉnh đó
  const districts = await getDistricts(foundProvince.code)
  let foundDistrict = null
  let distPartIndex = -1

  for (let i = provPartIndex - 1; i >= 0; i--) {
    const partClean = cleanStr(parts[i])
    const match = districts.find(d => {
      const dClean = cleanStr(d.name)
      return dClean === partClean || d.name.toLowerCase().includes(partClean) || parts[i].toLowerCase().includes(dClean)
    })
    if (match) {
      foundDistrict = match
      distPartIndex = i
      break
    }
  }

  // 3. Tìm phường / xã trong huyện đó
  let foundWard = null
  let wardPartIndex = -1

  if (foundDistrict) {
    const wards = await getWards(foundDistrict.code)
    for (let i = distPartIndex - 1; i >= 0; i--) {
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
  }

  // 4. Địa chỉ cụ thể là các phần còn lại trước phường/xã (hoặc trước huyện/tỉnh)
  const cutoffIndex = wardPartIndex !== -1 ? wardPartIndex : (distPartIndex !== -1 ? distPartIndex : provPartIndex)
  const specificParts = parts.slice(0, cutoffIndex)
  const specificAddress = specificParts.length > 0 ? specificParts.join(', ') : fullAddressString

  return {
    provinceCode: foundProvince ? foundProvince.code : null,
    provinceName: foundProvince ? foundProvince.name : '',
    districtCode: foundDistrict ? foundDistrict.code : null,
    districtName: foundDistrict ? foundDistrict.name : '',
    wardCode: foundWard ? foundWard.code : null,
    wardName: foundWard ? foundWard.name : '',
    specificAddress
  }
}


