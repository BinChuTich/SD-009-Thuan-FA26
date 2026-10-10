// Service tích hợp API hành chính Việt Nam sau sát nhập 2025: https://provinces.open-api.vn/
// Cấu trúc 2 cấp: 34 Tỉnh / Thành phố và các Phường / Xã trực thuộc (chuẩn API v2)
import axios from 'axios'

const PROVINCES_BASE_URL = 'https://provinces.open-api.vn/api/v2'

// Cache bộ nhớ trong phiên làm việc để phản hồi tức thì
let provincesCache = null
const provinceWardsCache = new Map()

/**
 * Lấy danh sách đúng 34 Tỉnh / Thành phố sau sát nhập 2025 (chuẩn API v2)
 */
export async function getProvinces() {
  if (provincesCache && provincesCache.length === 34) return provincesCache

  try {
    const local = sessionStorage.getItem('cached_vn_provinces_v2_34')
    if (local) {
      const parsed = JSON.parse(local)
      if (Array.isArray(parsed) && parsed.length === 34) {
        provincesCache = parsed
        return provincesCache
      }
    }
  } catch (e) {}

  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/p/`)
    provincesCache = res.data || []
    try {
      sessionStorage.setItem('cached_vn_provinces_v2_34', JSON.stringify(provincesCache))
    } catch (e) {}
    return provincesCache
  } catch (error) {
    console.error('Lỗi khi tải danh sách 34 tỉnh thành sau sát nhập 2025:', error)
    return []
  }
}

/**
 * Lấy danh sách toàn bộ Phường / Xã của một Tỉnh / Thành phố (chuẩn API v2)
 * Cấu trúc 2 cấp trực tiếp: Tỉnh / Thành phố -> Phường / Xã
 */
export async function getWardsByProvince(provinceCode) {
  if (!provinceCode) return []
  const numCode = Number(provinceCode)
  if (isNaN(numCode)) return []

  if (provinceWardsCache.has(numCode)) {
    return provinceWardsCache.get(numCode)
  }

  try {
    const local = sessionStorage.getItem(`cached_vn_wards_v2_${numCode}`)
    if (local) {
      const parsed = JSON.parse(local)
      if (Array.isArray(parsed) && parsed.length > 0) {
        provinceWardsCache.set(numCode, parsed)
        return parsed
      }
    }
  } catch (e) {}

  try {
    let wards = []
    try {
      const res = await axios.get(`${PROVINCES_BASE_URL}/w/?province=${numCode}`)
      if (Array.isArray(res.data) && res.data.length > 0) {
        wards = res.data.map(w => ({
          code: w.code,
          name: w.name,
          divisionType: w.division_type,
          codename: w.codename,
          provinceCode: w.province_code
        }))
      }
    } catch (err) {
      const resP = await axios.get(`${PROVINCES_BASE_URL}/p/${numCode}?depth=2`)
      const rawWards = resP.data?.wards || []
      wards = rawWards.map(w => ({
        code: w.code,
        name: w.name,
        divisionType: w.division_type,
        codename: w.codename,
        provinceCode: w.province_code
      }))
    }

    // Sắp xếp tiếng Việt theo tên phường xã
    wards.sort((a, b) => a.name.localeCompare(b.name, 'vi'))

    provinceWardsCache.set(numCode, wards)
    try {
      sessionStorage.setItem(`cached_vn_wards_v2_${numCode}`, JSON.stringify(wards))
    } catch (e) {}
    return wards
  } catch (error) {
    console.error(`Lỗi khi tải danh sách phường xã cho tỉnh ${provinceCode}:`, error)
    return []
  }
}

/**
 * Tra cứu chuyển đổi xã/phường cũ sang xã/phường mới sau sáp nhập
 */
export async function lookupLegacyWard(name, provinceCode = null) {
  if (!name || typeof name !== 'string') return null
  const clean = name.replace(/^(phường|xã|thị trấn|tt\.|tt)\s+/gi, '').trim()
  if (!clean) return null

  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/w/from-legacy/`, {
      params: { legacy_name: clean }
    })
    const items = res.data || []
    if (items.length === 0) return null

    if (provinceCode) {
      const numProv = Number(provinceCode)
      const matchedItem = items.find(item => item.ward && item.ward.province_code === numProv)
      if (matchedItem && matchedItem.ward) return matchedItem.ward
    }
    return items[0]?.ward || null
  } catch (e) {
    return null
  }
}

/**
 * Khớp địa chỉ dạng chuỗi (từ CCCD) với cấu trúc 2 cấp sau sáp nhập 2025
 */
export async function matchAddressHierarchy(fullAddressString) {
  if (!fullAddressString || typeof fullAddressString !== 'string') {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: '' }
  }

  const provinces = await getProvinces()
  const cleanStr = (s) => (s || '').toLowerCase()
    .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')
    .trim()

  const parts = fullAddressString.split(/[,;\n]+/).map(p => p.trim()).filter(Boolean)
  if (parts.length === 0) {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: fullAddressString }
  }

  // 1. Tìm Tỉnh / Thành phố trong 34 tỉnh thành sau sáp nhập (duyệt từ cuối chuỗi)
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

  // Nếu không tìm thấy tỉnh trực tiếp, tra cứu xã/phường cũ để suy ra tỉnh mới
  if (!foundProvince) {
    for (let i = parts.length - 1; i >= 0; i--) {
      const legWard = await lookupLegacyWard(parts[i])
      if (legWard && legWard.province_code) {
        const prov = provinces.find(p => p.code === legWard.province_code)
        if (prov) {
          foundProvince = prov
          provPartIndex = i
          break
        }
      }
    }
  }

  if (!foundProvince) {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', specificAddress: fullAddressString }
  }

  // 2. Tìm Phường / Xã trực tiếp trong Tỉnh đó
  const wards = await getWardsByProvince(foundProvince.code)
  let foundWard = null
  let wardPartIndex = -1

  // Ưu tiên khớp chính xác tên
  for (let i = provPartIndex - 1; i >= 0; i--) {
    const rawPart = parts[i].toLowerCase()
    const matchExact = wards.find(w => w.name.toLowerCase() === rawPart)
    if (matchExact) {
      foundWard = matchExact
      wardPartIndex = i
      break
    }
  }

  // Khớp theo tên chuẩn hóa
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

  // Tra cứu theo tên cũ qua /w/from-legacy/
  if (!foundWard) {
    for (let i = provPartIndex - 1; i >= 0; i--) {
      const legWard = await lookupLegacyWard(parts[i], foundProvince.code)
      if (legWard) {
        const matchInWards = wards.find(w => w.code === legWard.code)
        foundWard = matchInWards || legWard
        wardPartIndex = i
        break
      }
    }
  }

  // 3. Địa chỉ cụ thể là các phần còn lại trước Phường/Xã (hoặc trước Tỉnh)
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
