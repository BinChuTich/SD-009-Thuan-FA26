/**
 * Quản lý lưu trữ giỏ hàng Online (Tiếng Việt)
 */
import { ref, computed } from 'vue'

const CART_KEY = 'fftshirt_client_cart'

const loadCart = () => {
  try {
    const raw = localStorage.getItem(CART_KEY)
    return raw ? JSON.parse(raw) : []
  } catch (e) {
    return []
  }
}

export const cartItems = ref(loadCart())

export const saveCart = () => {
  localStorage.setItem(CART_KEY, JSON.stringify(cartItems.value))
}

export const addToCart = (product, variant, quantity = 1) => {
  if (!product || !variant) return false

  const rawStock = variant.soLuongTon != null ? variant.soLuongTon : (variant.soLuong != null ? variant.soLuong : 99)
  const maxStock = Number(rawStock)
  if (maxStock <= 0) return false

  const existing = cartItems.value.find(item => item.variantId === variant.id)

  if (existing) {
    existing.quantity += quantity
    existing.maxStock = maxStock
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
      image: variant.hinhAnh || product.hinhAnh || product.spHinhAnh || '',
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
    const limit = item.maxStock != null ? Number(item.maxStock) : 99
    if (limit <= 0) {
      item.quantity = 0
      saveCart()
      return
    }
    if (quantity <= 0) {
      removeFromCart(variantId)
    } else {
      item.quantity = Math.min(quantity, limit)
      saveCart()
    }
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
