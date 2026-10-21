/**
 * Danh mục và danh sách sản phẩm trực tuyến (Tiếng Việt)
 * Đồng bộ với bộ nhận diện thương hiệu (#496883 & #d2a764)
 */

export const CATEGORIES_CATALOG = [
  {
    id: 1,
    code: 'NAM_BASIC',
    name: 'Áo phông nam',
    count: '0 mẫu',
    desc: '100% Cotton tự nhiên thoáng mát hằng ngày',
    image: '/images/products/sp01_front.jpg',
    tag: 'Yêu thích'
  },
  {
    id: 4,
    code: 'POLO',
    name: 'Áo polo',
    count: '0 mẫu',
    desc: 'Lịch lãm, chuẩn phom công sở & dạo phố',
    image: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=600&auto=format&fit=crop&q=80',
    tag: 'Bán chạy'
  },
  {
    id: 3,
    code: 'UNISEX_OVERSIZE',
    name: 'Áo phông unisex',
    count: '0 mẫu',
    desc: 'Form rộng streetwear trẻ trung, cá tính',
    image: 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=600&auto=format&fit=crop&q=80',
    tag: 'Xu hướng mới'
  },
  {
    id: 2,
    code: 'NU_BASIC',
    name: 'Áo phông nữ',
    count: '0 mẫu',
    desc: 'Thiết kế tôn dáng, mềm mại và trẻ trung',
    image: 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=600&auto=format&fit=crop&q=80',
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
  return apiProducts.filter(p => p && p.tenSanPham && p.trangThai === 1)
}

/**
 * Lấy chi tiết 1 sản phẩm theo ID từ API
 */
export function getProductById(id) {
  return null
}
