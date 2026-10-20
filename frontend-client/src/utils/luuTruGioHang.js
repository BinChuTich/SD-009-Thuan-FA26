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

  const existing = cartItems.value.find(item => item.variantId === variant.id)
  const maxStock = variant.soLuongTon != null ? variant.soLuongTon : (variant.soLuong != null ? variant.soLuong : 99)

  if (existing) {
    existing.quantity += quantity
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
    if (quantity <= 0) {
      removeFromCart(variantId)
    } else {
      item.quantity = Math.min(quantity, item.maxStock || 99)
      saveCart()
    }
  }
}

export const removeFromCart = (variantId) => {
  cartItems.value = cartItems.value.filter(i => i.variantId !== variantId)
  saveCart()
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
