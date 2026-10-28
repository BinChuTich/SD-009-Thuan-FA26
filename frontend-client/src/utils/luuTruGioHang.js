/**
 * Quản lý lưu trữ giỏ hàng Online (Tiếng Việt)
 */
import { ref, computed, watch } from 'vue'

export const NEUTRAL_PRODUCT_PLACEHOLDER = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100' fill='none'%3E%3Crect width='100' height='100' fill='%23f1f5f9'/%3E%3Cpath d='M35 38L42 30H58L65 38L58 42V70H42V42L35 38Z' stroke='%23cbd5e1' stroke-width='2' stroke-linejoin='round' fill='%23f8fafc'/%3E%3C/svg%3E"

export const resolveProductImageUrl = (raw) => {
  if (!raw || String(raw).includes('unsplash.com') || String(raw).includes('default_tshirt')) {
    return NEUTRAL_PRODUCT_PLACEHOLDER
  }
  const str = String(raw).trim()
  if (str.startsWith('http://') || str.startsWith('https://') || str.startsWith('data:')) {
    return str
  }
  return `http://localhost:8080${str.startsWith('/') ? '' : '/'}${str}`
}

const CART_KEY = 'fftshirt_client_cart'

const loadCart = () => {
  try {
    const raw = localStorage.getItem(CART_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    // Khôi phục và đảm bảo mỗi sản phẩm luôn có số lượng hợp lệ >= 1
    return parsed.map(item => ({
      ...item,
      image: (item.image && !item.image.includes('unsplash.com') && !item.image.includes('default_tshirt')) ? item.image : '',
      quantity: Math.max(1, Number(item.quantity) || 1),
      maxStock: item.maxStock != null ? Number(item.maxStock) : 99
    }))
  } catch (e) {
    return []
  }
}

export const cartItems = ref(loadCart())

export const saveCart = () => {
  try {
    localStorage.setItem(CART_KEY, JSON.stringify(cartItems.value))
  } catch (e) {
    console.warn('Lỗi lưu giỏ hàng:', e)
  }
}

// Tự động đồng bộ giỏ hàng vào localStorage mỗi khi có bất kỳ thay đổi nào
watch(
  cartItems,
  () => {
    saveCart()
  },
  { deep: true }
)

export const addToCart = (product, variant, quantity = 1) => {
  if (!product || !variant) return false

  const rawStock = variant.soLuongTon != null ? variant.soLuongTon : (variant.soLuong != null ? variant.soLuong : 99)
  const maxStock = Number(rawStock)
  if (maxStock <= 0) return false

  const existing = cartItems.value.find(item => item.variantId === variant.id)
  const variantImg = variant.anhDaiDien || variant.hinhAnh || variant.duongDan || variant.image || product.anhDaiDien || (Array.isArray(product.hinhAnhs) ? product.hinhAnhs[0] : '') || ''

  if (existing) {
    existing.quantity += quantity
    existing.maxStock = maxStock
    if (variantImg) {
      existing.image = variantImg
    }
    if (existing.quantity > maxStock) {
      existing.quantity = maxStock
    }
  } else {
    cartItems.value.push({
      productId: product.id,
      productName: product.tenSanPham || product.ten || 'Áo thun nam',
      productCode: product.maSanPham || product.ma || 'SP',
      variantId: variant.id,
      variantCode: variant.maChiTietSanPham || variant.maBienThe || 'CTSP',
      color: variant.tenMauSac || variant.mauSac || 'Tiêu chuẩn',
      size: variant.tenKichCo || variant.kichCo || 'M',
      image: variantImg,
      price: Number(variant.giaBan || variant.gia || product.gia || 0),
      discountPrice: Number(variant.giaSauGiam || variant.giaBan || variant.gia || product.gia || 0),
      quantity: Math.min(quantity, maxStock),
      maxStock: maxStock
    })
  }
  saveCart()
  return true
}

export const updateCartQuantity = (variantId, quantity) => {
  const item = cartItems.value.find(i => i.variantId === variantId)
  if (item) {
    if (quantity <= 0) {
      removeFromCart(variantId)
      return
    }
    const limit = item.maxStock != null ? Number(item.maxStock) : 99
    if (limit > 0) {
      item.quantity = Math.min(quantity, limit)
    } else {
      item.quantity = 1
    }
    saveCart()
  }
}

export const removeFromCart = (variantId) => {
  cartItems.value = cartItems.value.filter(i => i.variantId !== variantId)
  saveCart()
}

export const removeMultipleFromCart = (variantIds) => {
  if (!variantIds || !variantIds.length) return
  const set = new Set(variantIds)
  cartItems.value = cartItems.value.filter(i => !set.has(i.variantId))
  saveCart()
}

export const getCheckoutItems = () => {
  try {
    const raw = sessionStorage.getItem('fftshirt_checkout_items')
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const setCheckoutItems = (items) => {
  try {
    if (!items || items.length === 0) {
      sessionStorage.removeItem('fftshirt_checkout_items')
    } else {
      sessionStorage.setItem('fftshirt_checkout_items', JSON.stringify(items))
    }
  } catch (e) {}
}

export const getCheckoutVoucher = () => {
  try {
    const raw = sessionStorage.getItem('fftshirt_checkout_voucher')
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const setCheckoutVoucher = (voucher) => {
  try {
    if (!voucher) {
      sessionStorage.removeItem('fftshirt_checkout_voucher')
    } else {
      sessionStorage.setItem('fftshirt_checkout_voucher', JSON.stringify(voucher))
    }
  } catch (e) {}
}

export const clearCart = () => {
  cartItems.value = []
  saveCart()
}

export const cartCount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.quantity || 1), 0)
})

export const cartSubtotal = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    const p = item.discountPrice != null && item.discountPrice > 0 ? item.discountPrice : item.price
    return sum + p * item.quantity
  }, 0)
})
