/**

 * Danh mục và danh sách sản phẩm trực tuyến (Tiếng Việt)

 * Đồng bộ với bộ nhận diện thương hiệu (#496883 & #d2a764)

 */



import api from '@/api'



export const CATEGORIES_CATALOG = [
  {
    id: 1,
    code: 'NAM_BASIC',
    name: 'Áo phông nam',
    count: '1 mẫu',
    desc: '100% Cotton tự nhiên thoáng mát hằng ngày',
    image: '/uploads/sp_1793539509324_05f339fd.png',
    tag: 'Yêu thích'
  },
  {
    id: 4,
    code: 'POLO',
    name: 'Áo polo',
    count: '1 mẫu',
    desc: 'Lịch lãm, chuẩn phom công sở & dạo phố',
    image: 'http://localhost:8080/uploads/sp_1793532233466_34869b33.png',
    tag: 'Được mua nhiều'
  },
  {
    id: 3,
    code: 'UNISEX_OVERSIZE',
    name: 'Áo phông unisex',
    count: '1 mẫu',
    desc: 'Form rộng streetwear trẻ trung, cá tính',
    image: '/uploads/sp_1793538349993_b1bed0a5.png',
    tag: 'Xu hướng mới'
  },
  {
    id: 2,
    code: 'NU_BASIC',
    name: 'Áo phông nữ',
    count: '1 mẫu',
    desc: 'Thiết kế tôn dáng, mềm mại và trẻ trung',
    image: '/uploads/sp_1793539019833_eee91625.png',
    tag: 'Năng động'
  }
]

// Toàn bộ dữ liệu sản phẩm đồng bộ từ Admin API

export const MOCK_PRODUCTS_CATALOG = []



/**

 * Lọc danh sách sản phẩm từ API / Admin

 * Chỉ hiển thị đúng các sản phẩm thực tế được thêm từ màn Admin (đang kinh doanh trangThai = 1)

 */

export function getMergedProducts(apiProducts = []) {

  if (!Array.isArray(apiProducts)) return []

  return apiProducts.filter(p => p && p.tenSanPham && Number(p.trangThai ?? 1) === 1)

}



/**

 * Lấy chi tiết 1 sản phẩm theo ID từ API

 */

export function getProductById(id) {

  return null

}



let cachedSalesMap = null

let cacheTimestamp = 0

const CACHE_TTL_MS = 30000 // 30s cache



/**

 * Lấy bản đồ số lượng đã bán (lượt mua) thực tế theo idSanPham

 */

export async function getProductSalesMap(forceRefresh = false) {

  const now = Date.now()

  if (!forceRefresh && cachedSalesMap && (now - cacheTimestamp < CACHE_TTL_MS)) {

    return cachedSalesMap

  }



  const map = {}



  try {

    // 1. Thử lấy từ thống kê overview

    const resThongKe = await api.get('/api/thong-ke/overview', {

      params: { fromDate: '2020-01-01', toDate: '2030-12-31' }

    })

    if (resThongKe.data?.products && Array.isArray(resThongKe.data.products)) {

      resThongKe.data.products.forEach(p => {

        if (p.id) {

          map[p.id] = Number(p.soldCount || 0)

        }

      })

    }

  } catch (e) {

    // Fallback qua hóa đơn

  }



  try {

    // 2. Lấy thêm tất cả các hóa đơn đã được xác nhận (trangThai >= 2) để tính số lượng bán thời gian thực
    // TUYỆT ĐỐI KHÔNG TÍNH: Đơn chờ xác nhận (trangThai === 1) và Đơn đã hủy (trangThai === 6)
    const resOrders = await api.get('/api/hoa-don')

    const orders = Array.isArray(resOrders.data) ? resOrders.data : (resOrders.data?.value || [])

    

    // Đơn hợp lệ đã bán: Chỉ tính khi nhân viên đã xác nhận đơn trở lên (trangThai >= 2)
    const validOrders = orders.filter(h => {
      if (!h || !h.id) return false
      const st = Number(h.trangThai)
      return st >= 2 && st !== 6
    })

    

    // Lấy chi tiết các đơn hợp lệ song song

    const detailPromises = validOrders.map(h => 

      api.get(`/api/hoa-don/${h.id}/chi-tiet`)

        .then(r => Array.isArray(r.data) ? r.data : (r.data?.value || []))

        .catch(() => [])

    )

    const allDetailsList = await Promise.all(detailPromises)



    // Tính tổng số lượng bán thực tế chính xác theo từng sản phẩm

    const liveMap = {}

    allDetailsList.flat().forEach(d => {

      const spId = d?.idChiTietSanPham?.idSanPham?.id || d?.idSanPham

      const qty = Number(d?.soLuong || 1)

      if (spId) {

        liveMap[spId] = (liveMap[spId] || 0) + qty

      }

    })



    // Hợp nhất (lấy số lớn nhất giữa liveMap và thống kê để đảm bảo chính xác nhất)

    Object.keys(liveMap).forEach(id => {

      map[id] = Math.max(map[id] || 0, liveMap[id])

    })

  } catch (e) {

    console.warn('Lỗi tính lượt bán thực tế:', e)

  }



  cachedSalesMap = map

  cacheTimestamp = Date.now()

  return map

}



/**

 * Gắn thêm trường `daBan` và `soLuongDaBan` vào danh sách sản phẩm

 */

export async function attachSalesCountToProducts(products) {

  if (!Array.isArray(products) || products.length === 0) return products

  try {

    const salesMap = await getProductSalesMap()

    products.forEach(p => {

      if (p && p.id) {

        p.daBan = salesMap[p.id] || 0

        p.soLuongDaBan = salesMap[p.id] || 0

      }

    })

  } catch (e) {

    products.forEach(p => {

      if (p && p.daBan == null) {

        p.daBan = 0

        p.soLuongDaBan = 0

      }

    })

  }

  return products

}

