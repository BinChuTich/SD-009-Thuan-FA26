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
