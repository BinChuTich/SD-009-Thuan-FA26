// Service tích hợp API hành chính Việt Nam sau sát nhập 2025: https://provinces.open-api.vn/
// Cấu trúc 2 cấp: Tỉnh / Thành phố và Phường / Xã (phiên bản v2 sau sáp nhập 07/2025)
import axios from 'axios'

const PROVINCES_BASE_URL = 'https://provinces.open-api.vn/api/v2'

// Cache để tránh gọi lại nhiều lần và tăng tốc độ tối đa
let provincesCache = null
const provinceWardsCache = new Map()

export async function getProvinces() {
  if (provincesCache && provincesCache.length > 0) return provincesCache
  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/`)
    provincesCache = res.data || []
    return provincesCache
  } catch (error) {
    console.error('Lỗi khi tải danh sách tỉnh thành sau sát nhập 2025:', error)
    return []
  }
}

// Lấy danh sách toàn bộ Phường / Xã của một Tỉnh / Thành phố (2 cấp trực tiếp sau sát nhập 2025)
export async function getWardsByProvince(provinceCode) {
  if (!provinceCode) return []
  const numCode = Number(provinceCode)
  if (provinceWardsCache.has(numCode)) {
    return provinceWardsCache.get(numCode)
  }
  try {
    // Trong API v2: Gọi trực tiếp /w/?province={code} hoặc fallback /p/{code}?depth=2
    let wards = []
    try {
      const res = await axios.get(`${PROVINCES_BASE_URL}/w/?province=${numCode}`)
      if (Array.isArray(res.data) && res.data.length > 0) {
        wards = res.data.map(w => ({
          code: w.code,
          name: w.name,
          divisionType: w.division_type,
          codename: w.codename
        }))
      }
    } catch (e) {
      // Fallback endpoint
      const resP = await axios.get(`${PROVINCES_BASE_URL}/p/${numCode}?depth=2`)
      const rawWards = resP.data?.wards || []
      wards = rawWards.map(w => ({
        code: w.code,
        name: w.name,
        divisionType: w.division_type,
        codename: w.codename
      }))
    }

    // Sắp xếp tiếng Việt theo tên phường xã
    wards.sort((a, b) => a.name.localeCompare(b.name, 'vi'))
    provinceWardsCache.set(numCode, wards)
    return wards
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

  // Nếu không tìm thấy tỉnh trực tiếp (do địa chỉ cũ trên CCCD thuộc tỉnh cũ đã sáp nhập),
  // tra cứu qua endpoint v2: /w/from-legacy/?legacy_name=...
  if (!foundProvince && parts.length > 0) {
    for (let i = parts.length - 1; i >= 0; i--) {
      try {
        const legacyRes = await axios.get(`${PROVINCES_BASE_URL}/w/from-legacy/`, {
          params: { legacy_name: parts[i] }
        })
        const items = legacyRes.data || []
        if (items.length > 0 && items[0].ward) {
          const newWard = items[0].ward
          const prov = provinces.find(p => p.code === newWard.province_code)
          if (prov) {
            foundProvince = prov
            provPartIndex = i
            const specificParts = parts.slice(0, i)
            return {
              provinceCode: prov.code,
              provinceName: prov.name,
              wardCode: newWard.code,
              wardName: newWard.name,
              specificAddress: specificParts.length > 0 ? specificParts.join(', ') : fullAddressString
            }
          }
        }
      } catch (ignored) {}
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

  // Nếu chưa tìm thấy phường/xã trong danh sách mới, tra cứu theo tên cũ qua /w/from-legacy/
  if (!foundWard) {
    for (let i = provPartIndex - 1; i >= 0; i--) {
      try {
        const legacyRes = await axios.get(`${PROVINCES_BASE_URL}/w/from-legacy/`, {
          params: { legacy_name: parts[i] }
        })
        const items = legacyRes.data || []
        const matchedItem = items.find(item => item.ward && item.ward.province_code === foundProvince.code)
        if (matchedItem && matchedItem.ward) {
          foundWard = matchedItem.ward
          wardPartIndex = i
          break
        }
      } catch (ignored) {}
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
