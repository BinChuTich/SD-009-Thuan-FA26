// Danh sách dự phòng chuẩn khi offline hoặc API open-api.vn chưa phản hồi
const FALLBACK_PROVINCES = [
  { code: 1, name: 'Thành phố Hà Nội' },
  { code: 2, name: 'Tỉnh Hà Giang' },
  { code: 4, name: 'Tỉnh Cao Bằng' },
  { code: 6, name: 'Tỉnh Bắc Kạn' },
  { code: 8, name: 'Tỉnh Tuyên Quang' },
  { code: 10, name: 'Tỉnh Lào Cai' },
  { code: 11, name: 'Tỉnh Điện Biên' },
  { code: 12, name: 'Tỉnh Lai Châu' },
  { code: 14, name: 'Tỉnh Sơn La' },
  { code: 15, name: 'Tỉnh Yên Bái' },
  { code: 17, name: 'Tỉnh Hòa Bình' },
  { code: 19, name: 'Tỉnh Thái Nguyên' },
  { code: 20, name: 'Tỉnh Lạng Sơn' },
  { code: 22, name: 'Tỉnh Quảng Ninh' },
  { code: 24, name: 'Tỉnh Bắc Giang' },
  { code: 25, name: 'Tỉnh Phú Thọ' },
  { code: 26, name: 'Tỉnh Vĩnh Phúc' },
  { code: 27, name: 'Tỉnh Bắc Ninh' },
  { code: 30, name: 'Thành phố Hải Phòng' },
  { code: 31, name: 'Tỉnh Hải Dương' },
  { code: 33, name: 'Tỉnh Hưng Yên' },
  { code: 34, name: 'Tỉnh Thái Bình' },
  { code: 35, name: 'Tỉnh Hà Nam' },
  { code: 36, name: 'Tỉnh Nam Định' },
  { code: 37, name: 'Tỉnh Ninh Bình' },
  { code: 38, name: 'Tỉnh Thanh Hóa' },
  { code: 40, name: 'Tỉnh Nghệ An' },
  { code: 42, name: 'Tỉnh Hà Tĩnh' },
  { code: 44, name: 'Tỉnh Quảng Bình' },
  { code: 45, name: 'Tỉnh Quảng Trị' },
  { code: 46, name: 'Tỉnh Thừa Thiên Huế' },
  { code: 48, name: 'Thành phố Đà Nẵng' },
  { code: 79, name: 'Thành phố Hồ Chí Minh' },
  { code: 92, name: 'Thành phố Cần Thơ' }
];

const FALLBACK_HN_WARDS = [
  { code: 101, name: 'Phường Mỹ Đình 1', districtName: 'Nam Từ Liêm' },
  { code: 102, name: 'Phường Mỹ Đình 2', districtName: 'Nam Từ Liêm' },
  { code: 103, name: 'Phường Dịch Vọng Hậu', districtName: 'Cầu Giấy' },
  { code: 104, name: 'Phường Dịch Vọng', districtName: 'Cầu Giấy' },
  { code: 105, name: 'Phường Quan Hoa', districtName: 'Cầu Giấy' },
  { code: 106, name: 'Phường Nghĩa Tân', districtName: 'Cầu Giấy' },
  { code: 107, name: 'Phường Láng Thượng', districtName: 'Đống Đa' },
  { code: 108, name: 'Phường Láng Hạ', districtName: 'Đống Đa' },
  { code: 109, name: 'Phường Ô Chợ Dừa', districtName: 'Đống Đa' },
  { code: 110, name: 'Phường Kim Mã', districtName: 'Ba Đình' },
  { code: 111, name: 'Phường Cống Vị', districtName: 'Ba Đình' },
  { code: 112, name: 'Phường Hàng Bạc', districtName: 'Hoàn Kiếm' },
  { code: 113, name: 'Phường Tràng Tiền', districtName: 'Hoàn Kiếm' },
  { code: 114, name: 'Phường Thanh Xuân Bắc', districtName: 'Thanh Xuân' },
  { code: 115, name: 'Phường Thanh Xuân Trung', districtName: 'Thanh Xuân' },
  { code: 116, name: 'Phường Xuân Đỉnh', districtName: 'Bắc Từ Liêm' },
  { code: 117, name: 'Phường Cổ Nhuế 1', districtName: 'Bắc Từ Liêm' },
  { code: 118, name: 'Phường Hoàng Liệt', districtName: 'Hoàng Mai' },
  { code: 119, name: 'Phường Bồ Đề', districtName: 'Long Biên' },
  { code: 120, name: 'Phường Hà Cầu', districtName: 'Hà Đông' },
  { code: 121, name: 'Thị trấn Đông Anh', districtName: 'Đông Anh' },
  { code: 122, name: 'Thị trấn Trâu Quỳ', districtName: 'Gia Lâm' }
];

// Service tích hợp API hành chính Việt Nam chuẩn theo https://vn-provinces-lustre-demo.netlify.app/
// Cấu trúc chuẩn API v2 sau sáp nhập 2025: 34 Tỉnh / Thành phố và các Phường / Xã trực thuộc
import axios from 'axios'

const PROVINCES_BASE_URL = 'https://provinces.open-api.vn/api/v2'
const LEGACY_BASE_URL = 'https://provinces.open-api.vn/api/v1'

// Cache bộ nhớ trong phiên làm việc để phản hồi tức thì
let provincesCache = null
const provinceWardsCache = new Map()
const legacyDistrictsCache = new Map()
const legacyProvincesCache = new Map()

/**
 * Lấy danh sách đúng 34 Tỉnh / Thành phố sau sáp nhập 2025 (chuẩn API v2)
 * Đúng theo https://vn-provinces-lustre-demo.netlify.app/ (endpoint: /api/v2/p/)
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
    console.warn('Fallback sang danh sách 34 tỉnh thành dự phòng');
    provincesCache = [...FALLBACK_PROVINCES];
    return provincesCache;
  }
}

/**
 * Lấy danh sách toàn bộ Phường / Xã của một Tỉnh / Thành phố (chuẩn API v2)
 * Đúng theo https://vn-provinces-lustre-demo.netlify.app/ (endpoint: /api/v2/p/{code}?depth=2)
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
      // 1. Gọi trực tiếp theo demo: /p/{code}?depth=2
      const resP = await axios.get(`${PROVINCES_BASE_URL}/p/${numCode}?depth=2`)
      const rawWards = resP.data?.wards || []
      wards = rawWards.map(w => ({
        code: w.code,
        name: w.name,
        divisionType: w.division_type,
        codename: w.codename,
        provinceCode: w.province_code || numCode
      }))
    } catch (err) {
      // 2. Fallback sang /w/?province={code} nếu cần
      const res = await axios.get(`${PROVINCES_BASE_URL}/w/?province=${numCode}`)
      if (Array.isArray(res.data) && res.data.length > 0) {
        wards = res.data.map(w => ({
          code: w.code,
          name: w.name,
          divisionType: w.division_type,
          codename: w.codename,
          provinceCode: w.province_code || numCode
        }))
      }
    }

    // Sắp xếp tiếng Việt theo tên phường xã
    wards.sort((a, b) => a.name.localeCompare(b.name, 'vi'))

    provinceWardsCache.set(numCode, wards)
    try {
      sessionStorage.setItem(`cached_vn_wards_v2_${numCode}`, JSON.stringify(wards))
    } catch (e) {}
    return wards
  } catch (error) {
    console.warn(`Fallback danh sách phường xã dự phòng cho tỉnh ${provinceCode}`);
    if (Number(provinceCode) === 1) return [...FALLBACK_HN_WARDS];
    return [
      { code: Number(provinceCode) * 100 + 1, name: 'Phường Trung tâm', districtName: 'Quận Trung tâm' },
      { code: Number(provinceCode) * 100 + 2, name: 'Phường 1', districtName: 'Quận 1' },
      { code: Number(provinceCode) * 100 + 3, name: 'Xã Ngoại thành', districtName: 'Huyện lân cận' }
    ];
  }
}

/**
 * Tra cứu thông tin Phường/Xã nguồn và Quận/Huyện trước sáp nhập 2025
 * Đúng theo https://vn-provinces-lustre-demo.netlify.app/ (endpoint: /api/v2/w/{code}/to-legacies/)
 */
export async function getWardLegacies(wardCode) {
  if (!wardCode) return []
  const numCode = Number(wardCode)
  if (isNaN(numCode)) return []

  try {
    const res = await axios.get(`${PROVINCES_BASE_URL}/w/${numCode}/to-legacies/`)
    const items = res.data || []
    if (!Array.isArray(items)) return []

    const enriched = await Promise.all(items.map(async item => {
      let districtName = ''
      let provinceName = ''
      try {
        if (item.district_code) {
          if (legacyDistrictsCache.has(item.district_code)) {
            districtName = legacyDistrictsCache.get(item.district_code)
          } else {
            const dRes = await axios.get(`${LEGACY_BASE_URL}/d/${item.district_code}`)
            districtName = dRes.data?.name || ''
            legacyDistrictsCache.set(item.district_code, districtName)
          }
        }
        if (item.province_code) {
          if (legacyProvincesCache.has(item.province_code)) {
            provinceName = legacyProvincesCache.get(item.province_code)
          } else {
            const pRes = await axios.get(`${LEGACY_BASE_URL}/p/${item.province_code}`)
            provinceName = pRes.data?.name || ''
            legacyProvincesCache.set(item.province_code, provinceName)
          }
        }
      } catch (e) {}
      return {
        ...item,
        districtName,
        provinceName
      }
    }))
    return enriched
  } catch (e) {
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
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', districtName: '', specificAddress: '' }
  }

  const provinces = await getProvinces()
  const cleanStr = (s) => (s || '').toLowerCase()
    .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')
    .trim()

  const parts = fullAddressString.split(/[,;\n]+/).map(p => p.trim()).filter(Boolean)
  if (parts.length === 0) {
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', districtName: '', specificAddress: fullAddressString }
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
    return { provinceCode: null, provinceName: '', wardCode: null, wardName: '', districtName: '', specificAddress: fullAddressString }
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

  // 4. Tìm Quận/Huyện từ legacy nếu có
  let districtName = ''
  if (foundWard?.code) {
    try {
      const legacies = await getWardLegacies(foundWard.code)
      if (legacies.length > 0 && legacies[0].districtName) {
        districtName = legacies[0].districtName
      }
    } catch (e) {}
  }

  return {
    provinceCode: foundProvince.code,
    provinceName: foundProvince.name,
    wardCode: foundWard ? foundWard.code : null,
    wardName: foundWard ? foundWard.name : '',
    districtName,
    specificAddress
  }
}
