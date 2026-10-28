<script setup>

import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

import { useRouter, useRoute } from 'vue-router'

import api from '@/api.js'

import Select2Combobox from '@/components/Select2Combobox.vue'

import { getProvinces, getWardsByProvince } from '@/services/provincesApi.js'

import { calculateGhnShippingFee } from '@/services/ghnService.js'

import { getCurrentUser } from '@/utils/auth.js'



const router = useRouter()

const route = useRoute()



const toast = ref({

  show: false,

  message: '',

  type: 'success'

})

let toastTimer = null

const showToast = (message, type = 'success') => {

  if (toastTimer) clearTimeout(toastTimer)

  toast.value = { show: true, message, type }

  toastTimer = setTimeout(() => {

    toast.value.show = false

    toastTimer = null

  }, 2800)

}



const showConfirmModal = ref(false)

const confirmModalData = ref({

  title: '',

  message: '',

  onConfirm: () => {}

})

const openConfirm = (title, message, onConfirm) => {

  confirmModalData.value = {

    title,

    message,

    onConfirm: () => {

      showConfirmModal.value = false

      onConfirm()

    }

  }

  showConfirmModal.value = true

}



/* ==========================================================

   1. QUẢN LÝ HÓA ĐƠN CHỜ TRÊN CSDL (Tối đa 5 hóa đơn)

========================================================== */

const createEmptyInvoice = (id, code) => ({

  id,

  code,

  cart: [],

  customerId: null,

  customerName: '',

  customerPhone: '',

  isDelivery: false,

  deliveryProvinceCode: '',

  deliveryProvince: '',

  deliveryDistrictCode: '',

  deliveryDistrict: '',

  deliveryWardCode: '',

  deliveryWard: '',

  deliveryDetail: '',

  deliveryAddress: '',

  shippingFee: null,

  shippingDesc: '',

  shippingSource: '',

  voucher: null,

  paymentMethod: 'TIEN_MAT', 

  customerPay: null,

  note: ''

})



const waitingInvoices = ref([])

const activeInvoiceId = ref(null)

const isLoadingInvoices = ref(false)



// Hóa đơn hiện tại đang chọn

const currentInvoice = computed(() => {

  if (waitingInvoices.value.length === 0) {

    return createEmptyInvoice(null, '---')

  }

  return waitingInvoices.value.find(inv => inv.id === activeInvoiceId.value) || waitingInvoices.value[0]

})



// Tải danh sách hóa đơn chờ từ CSDL

const loadWaitingInvoices = async () => {

  try {

    await api.post('/api/hoa-don/cancel-expired-waiting').catch(() => {})

  } catch (e) {}

  try {

    isLoadingInvoices.value = true

    let res = null

    try {

      res = await api.get('/api/hoa-don/waiting')

    } catch (err) {

      console.warn('API /api/hoa-don/waiting chưa hỗ trợ, fallback /api/hoa-don:', err)

      const allRes = await api.get('/api/hoa-don')

      const all = Array.isArray(allRes.data) ? allRes.data : []

      res = {

        data: all.filter(h => Number(h.loaiDon) === 1 && Number(h.trangThai) === 1)

      }

    }



    let list = Array.isArray(res?.data) ? res.data : []



    // Nếu chưa có bất kỳ hóa đơn chờ nào trong CSDL, tự động tạo mới 1 hóa đơn chờ

    if (list.length === 0) {

      try {

        const createRes = await api.post('/api/hoa-don/waiting')

        if (createRes?.data) {

          list = [createRes.data]

        }

      } catch (err) {

        // Fallback tạo qua /api/hoa-don

        const createRes = await api.post('/api/hoa-don', {

          loaiDon: 1,

          trangThai: 1,

          trangThaiThanhToan: 0,

          tenKhachHang: 'Khách lẻ',

          diaChiNhanHang: 'Bán tại quầy',

          tongTien: 0,

          phiVanChuyen: 0,

          tienSauGiamGia: 0,

          ghiChu: 'Hóa đơn chờ tại quầy'

        })

        if (createRes?.data) {

          list = [createRes.data]

        }

      }

    }



    // Giới hạn tối đa 5 hóa đơn chờ

    list = list.slice(0, 5)



    // Tải chi tiết sản phẩm cho từng hóa đơn chờ

    const loadedTabs = await Promise.all(list.map(async (inv) => {

      let cartItems = []

      try {

        const detailRes = await api.get(`/api/hoa-don/${inv.id}/chi-tiet`)

        const items = Array.isArray(detailRes.data) ? detailRes.data : []

        const aggregatedMap = new Map()
        for (const ct of items) {
          const ctsp = ct.idChiTietSanPham || {}
          const sp = ctsp.idSanPham || {}
          const ms = ctsp.idMauSac || {}
          const kc = ctsp.idKichCo || {}
          const vid = ctsp.id || ct.id
          if (!vid) continue
          const origPrice = ctsp.giaBan != null ? Number(ctsp.giaBan) : (ct.donGia != null ? Number(ct.donGia) : 0)
          const curPrice = ct.donGia != null ? Number(ct.donGia) : origPrice
          const hasDiscount = origPrice > curPrice
          const qty = ct.soLuong || 1

          if (aggregatedMap.has(vid)) {
            const ex = aggregatedMap.get(vid)
            ex.quantity += qty
            ex.syncedQuantity += qty
          } else {
            aggregatedMap.set(vid, {
              id: vid,
              chiTietHoaDonId: ct.id,
              code: sp.maSanPham || ctsp.maChiTietSanPham || '',
              variantCode: ctsp.maChiTietSanPham || '',
              name: sp.tenSanPham || ctsp.tenSanPham || ct.tenSanPham || 'Áo Phông Cao Cấp',
              color: ms.tenMau || ms.tenMauSac || ct.tenMauSac || 'Mặc định',
              colorHex: ms.maHex || '',
              size: kc.tenKichCo || ct.tenKichCo || 'Freesize',
              stock: ctsp.soLuong != null ? Number(ctsp.soLuong) : 50,
              originalPrice: origPrice,
              price: curPrice,
              hasDiscount: hasDiscount,
              discountText: hasDiscount ? ('-' + formatShortMoney(origPrice - curPrice)) : '',
              quantity: qty,
              syncedQuantity: qty,
              image: ct.anhDaiDien || ctsp.anhDaiDien || ''
            })
          }
        }
        cartItems = Array.from(aggregatedMap.values())

      } catch (e) {

        console.warn(`Không tải được chi tiết cho HĐ ${inv.maHoaDon}:`, e)

      }



      const rawCust = inv.tenKhachHang

      const isKhachLe = !rawCust || rawCust.trim().toLowerCase() === 'khách lẻ'



      return {

        id: inv.id,

        code: inv.maHoaDon,

        cart: cartItems,

        customerId: inv.idKhachHang?.id || null,

        customerName: isKhachLe ? '' : rawCust,

        customerPhone: inv.soDienThoaiKhachHang || '',

        isDelivery: Boolean(inv.diaChiNhanHang && inv.diaChiNhanHang !== 'Bán tại quầy' && inv.diaChiNhanHang !== '—' && inv.diaChiNhanHang.trim() !== ''),
        deliveryAddress: (inv.diaChiNhanHang && inv.diaChiNhanHang !== 'Bán tại quầy') ? inv.diaChiNhanHang : '',
        shippingFee: (Boolean(inv.diaChiNhanHang && inv.diaChiNhanHang !== 'Bán tại quầy' && inv.diaChiNhanHang !== '—')) ? Number(inv.phiVanChuyen || 0) : 0,
        isManualShippingFee: Number(inv.phiVanChuyen || 0) > 0,

        voucher: inv.idPhieuGiamGia || null,

        paymentMethod: 'TIEN_MAT',

        customerPay: null,

        cashAmount: null,

        transferAmount: null,

        note: (inv.ghiChu && inv.ghiChu !== 'Hóa đơn chờ tại quầy') ? inv.ghiChu : '',

        isTransferPaid: Number(inv.trangThaiThanhToan) === 1,

        transferTransactionCode: ''

      }

    }))



    waitingInvoices.value = loadedTabs

    if (loadedTabs.length > 0) {

      activeInvoiceId.value = loadedTabs[0].id

    }

  } catch (err) {

    console.error('Lỗi khi tải hóa đơn chờ từ CSDL:', err)

    showToast('Lỗi khi tải danh sách hóa đơn chờ từ hệ thống!', 'warning')

  } finally {

    isLoadingInvoices.value = false

  }

}



// Thêm hóa đơn chờ mới (lưu thẳng vào CSDL, tối đa 5 hóa đơn)

const addWaitingInvoice = async () => {

  if (waitingInvoices.value.length >= 5) {

    showToast('Chỉ được tạo tối đa 5 hóa đơn chờ cùng lúc!', 'warning')

    return

  }



  try {

    const currentUser = getCurrentUser()

    const staffName = currentUser?.tenNhanVien || currentUser?.tenTaiKhoan || 'Nhân viên bán hàng'

    const staffId = currentUser?.id || null



    let newInv = null

    try {

      const res = await api.post('/api/hoa-don/waiting', { idNhanVien: staffId, nguoiTao: staffName })

      newInv = res.data

    } catch {

      const res = await api.post('/api/hoa-don', {

        loaiDon: 1,

        trangThai: 1,

        trangThaiThanhToan: 0,

        tenKhachHang: 'Khách lẻ',

        diaChiNhanHang: 'Bán tại quầy',

        tongTien: 0,

        phiVanChuyen: 0,

        tienSauGiamGia: 0,

        ghiChu: 'Hóa đơn chờ tại quầy'

      })

      newInv = res.data

    }



    if (newInv) {

      const newTab = {

        id: newInv.id,

        code: newInv.maHoaDon,

        cart: [],

        customerId: null,

        customerName: '',

        customerPhone: '',

        isDelivery: false,

        deliveryAddress: '',

        shippingFee: 0,

        voucher: null,

        paymentMethod: 'TIEN_MAT',

        customerPay: null,

        cashAmount: null,

        transferAmount: null,

        note: '',

        isTransferPaid: false,

        transferTransactionCode: ''

      }

      waitingInvoices.value.push(newTab)

      activeInvoiceId.value = newInv.id

      showToast(`Đã tạo hóa đơn chờ ${newInv.maHoaDon}!`)

    }

  } catch (err) {

    console.error('Lỗi khi tạo hóa đơn chờ:', err)

    showToast('Không thể tạo hóa đơn chờ mới trong CSDL!', 'warning')

  }

}



// Chuyển tab hóa đơn

const selectInvoice = (id) => {

  activeInvoiceId.value = id

}



// Hủy hóa đơn chờ cụ thể

const confirmRemoveInvoice = (inv) => {

  if (!inv) return

  openConfirm(

      'Xác nhận hủy hóa đơn',

      `Bạn có chắc chắn muốn hủy hóa đơn chờ "${inv.code}" không? Hóa đơn này sẽ được xóa hoàn toàn khỏi cơ sở dữ liệu.`,

      async () => {

        try {

          if (inv.cart && inv.cart.length > 0) {

            inv.cart.forEach(item => {

              const p = productList.value.find(x => x.id === item.id)

              if (p) {

                p.stock += item.quantity

              }

            })

          }

          if (inv.id) {

            await api.delete(`/api/hoa-don/${inv.id}`)

          }

          waitingInvoices.value = waitingInvoices.value.filter(i => i.id !== inv.id)

          showToast(`Đã hủy thành công hóa đơn ${inv.code}!`)



          // Nếu đã xóa hết, tự động tạo mới 1 hóa đơn chờ trên DB

          if (waitingInvoices.value.length === 0) {

            await addWaitingInvoice()

          } else {

            if (activeInvoiceId.value === inv.id) {

              activeInvoiceId.value = waitingInvoices.value[0].id

            }

          }

        } catch (err) {

          console.error('Lỗi khi hủy hóa đơn:', err)

          showToast('Không thể hủy hóa đơn khỏi cơ sở dữ liệu!', 'warning')

        }

      }

  )

}



// Nút Hủy hóa đơn ở góc dưới bên phải

const removeCurrentInvoice = () => {

  if (currentInvoice.value) {

    confirmRemoveInvoice(currentInvoice.value)

  }

}



/* ==========================================================

   2. DANH SÁCH SẢN PHẨM & TỒN KHO & QUÉT BARCODE

========================================================== */

const showProductModal = ref(false)

const searchProductText = ref('')

const barcodeInput = ref('')

const isLoadingProducts = ref(false)

const productList = ref([])



// Rút gọn số tiền (VD: 50.000 -> 50k, 1.500.000 -> 1.5tr)

const formatShortMoney = (val) => {

  if (!val) return '0đ'

  const num = Number(val)

  if (isNaN(num)) return String(val)

  if (num >= 1000000) {

    const tr = num / 1000000

    return (tr % 1 === 0 ? tr : tr.toFixed(1)) + 'tr'

  }

  if (num >= 1000) {

    const k = num / 1000

    return (k % 1 === 0 ? k : k.toFixed(0)) + 'k'

  }

  return num.toLocaleString('vi-VN') + 'đ'

}



const getCartItemName = (item) => {

  if (item?.name && typeof item.name === 'string' && item.name.trim() !== '' && item.name !== 'Áo phông') {

    return item.name

  }

  const found = productList.value.find(p => p.id === item?.id || (item?.variantCode && p.variantCode === item.variantCode))

  if (found && found.name) return found.name

  return item?.name || 'Áo Phông Cao Cấp'

}



// Tự động tải lại biến thể sản phẩm & chiến dịch giảm giá khi mở modal chọn sản phẩm

watch(showProductModal, (isOpen) => {

  if (isOpen) {

    fetchProductVariants()

  }

})



// Tính toán số lượng của biến thể đang trong giỏ nhưng chưa đồng bộ vào CSDL (unsynced delta)

const getUnsyncedQuantityForVariant = (variantId, variantCode) => {

  let delta = 0

  if (currentInvoice.value && Array.isArray(currentInvoice.value.cart)) {

    for (const c of currentInvoice.value.cart) {

      const match = (variantId != null && Number(c.id) === Number(variantId)) ||

                    (variantCode && c.variantCode && String(c.variantCode).trim().toLowerCase() === String(variantCode).trim().toLowerCase())

      if (match) {

        const qty = Number(c.quantity || 0)

        const synced = c.syncedQuantity != null ? Number(c.syncedQuantity) : qty

        delta += (qty - synced)

      }

    }

  }

  return delta

}



// Đồng bộ tồn kho khả dụng (stock) theo dbStock trừ chênh lệch giỏ hàng chưa đồng bộ

const refreshAvailableStock = () => {

  if (!Array.isArray(productList.value)) return

  for (const p of productList.value) {

    if (p.dbStock == null) {

      p.dbStock = p.stock != null ? Number(p.stock) : 0

    }

    const unsynced = getUnsyncedQuantityForVariant(p.id, p.variantCode)

    p.stock = Math.max(0, p.dbStock - unsynced)

  }

}



// Bổ sung đầy đủ thông tin (tên, màu, size, ảnh) cho các sản phẩm trong giỏ từ productList

const enrichCartItemsWithProductList = () => {

  if (!Array.isArray(productList.value) || productList.value.length === 0) return

  if (!Array.isArray(waitingInvoices.value)) return

  for (const inv of waitingInvoices.value) {

    if (!Array.isArray(inv.cart)) continue

    for (const item of inv.cart) {

      const p = findProduct(item)

      if (p) {

        if (!item.name || item.name === 'Áo phông' || item.name === 'Áo Phông Cao Cấp') {

          item.name = p.name

        }

        if (!item.color || item.color === 'Mặc định') item.color = p.color

        if (!item.colorHex && p.colorHex) item.colorHex = p.colorHex

        if (!item.size || item.size === 'Freesize') item.size = p.size

        if (!item.image || item.image.includes('default_tshirt')) item.image = p.image

        item.originalPrice = p.originalPrice || p.price
        item.price = p.price
        item.hasDiscount = Boolean(p.hasDiscount)
        item.discountText = p.discountText || ''
        item.discountCampaignName = p.discountCampaignName || ''

      }

    }

  }

}



// Gọi API lấy toàn bộ biến thể sản phẩm đang bán kèm đợt giảm giá (HOÀN TOÀN TỪ CSDL)

const fetchProductVariants = async () => {

  try {

    isLoadingProducts.value = true

    const [resVariants, resActiveVariantsMap] = await Promise.allSettled([

      api.get('/api/chi-tiet-san-pham/filter', {

        params: { trangThai: 1, size: 500 }

      }),

      api.get('/api/dot-giam-gia/active-variants-map')

    ])



    const content = (resVariants.status === 'fulfilled' && (resVariants.value.data?.content || (Array.isArray(resVariants.value.data) ? resVariants.value.data : []))) || []

    const activeMap = (resActiveVariantsMap.status === 'fulfilled' && resActiveVariantsMap.value.data && typeof resActiveVariantsMap.value.data === 'object')

      ? resActiveVariantsMap.value.data

      : {}



    productList.value = content.map((item, idx) => {

      const origPrice = item.giaBan != null ? Number(item.giaBan) : 0

      let salePrice = origPrice

      let hasDiscount = false

      let discountText = ''

      let discountCampaignName = ''



      // 1. Kiểm tra thông tin giảm giá từ DTO backend (ChiTietSanPhamResponse)

      if (item.coGiamGia && item.giaSauGiam != null && Number(item.giaSauGiam) < origPrice) {

        hasDiscount = true

        salePrice = Number(item.giaSauGiam)

        discountCampaignName = item.tenDotGiamGia || ''

        if (item.phanTramGiam != null && Number(item.phanTramGiam) > 0) {

          const pt = Number(item.phanTramGiam)

          discountText = `-${pt % 1 === 0 ? pt : pt.toFixed(0)}%`

        } else {

          discountText = `-${formatShortMoney(origPrice - salePrice)}`

        }

      }



      // 2. Bổ sung/đồng bộ tức thì từ activeVariantsMap (nếu có đợt giảm giá mới nhất)

      const dggInfo = activeMap[item.id]

      if (dggInfo) {

        let computedSalePrice = origPrice

        let computedText = ''

        const loai = Number(dggInfo.loaiGiamGia)

        if (loai === 2) {

          // Giảm theo số tiền (VNĐ)

          const cashVal = Number(dggInfo.giaTriGiam || 0)

          computedSalePrice = Math.max(0, origPrice - cashVal)

          computedText = `-${formatShortMoney(cashVal)}`

        } else {

          // Giảm theo %

          const pt = Number(dggInfo.phanTramGiam || 0)

          let discountAmt = (origPrice * pt) / 100

          if (dggInfo.giamToiDa && Number(dggInfo.giamToiDa) > 0 && discountAmt > Number(dggInfo.giamToiDa)) {

            discountAmt = Number(dggInfo.giamToiDa)

          }

          computedSalePrice = Math.max(0, origPrice - discountAmt)

          computedText = `-${pt % 1 === 0 ? pt : pt.toFixed(0)}%`

        }



        if (computedSalePrice < salePrice || (!hasDiscount && computedSalePrice < origPrice)) {

          salePrice = computedSalePrice

          hasDiscount = true

          discountText = computedText

          discountCampaignName = dggInfo.tenDotGiamGia || discountCampaignName

        }

      }



      return {

        id: item.id,

        sanPhamId: item.idSanPham?.id || item.idSanPham || item.id,

        code: item.idSanPham?.maSanPham || item.maSanPham || item.maChiTietSanPham || `SP00${idx + 1}`,

        variantCode: item.maChiTietSanPham || `CTSP0${idx + 1}`,

        name: item.idSanPham?.tenSanPham || item.tenSanPham || 'Áo Phông Cao Cấp',

        color: item.idMauSac?.tenMau || item.idMauSac?.tenMauSac || item.tenMauSac || 'Mặc định',

        colorHex: item.idMauSac?.maHex || item.maHex || '',

        size: item.idKichCo?.tenKichCo || item.tenKichCo || 'Freesize',

        dbStock: item.soLuong != null ? Number(item.soLuong) : 0,

        stock: Math.max(0, (item.soLuong != null ? Number(item.soLuong) : 0) - getUnsyncedQuantityForVariant(item.id, item.maChiTietSanPham)),

        originalPrice: origPrice,

        price: hasDiscount ? salePrice : origPrice,

        hasDiscount: hasDiscount,

        discountText: discountText,

        discountCampaignName: discountCampaignName,

        image: item.anhDaiDien || ''

      }

    })

    enrichCartItemsWithProductList()

  } catch (e) {

    console.error('Không tải được danh sách CTSP từ CSDL:', e)

    productList.value = []

  } finally {

    isLoadingProducts.value = false

  }

}



// Tự động đồng bộ giỏ hàng hóa đơn chờ với CSDL (debounced)

let syncCartTimer = null

const syncCurrentCartToDb = () => {

  clearTimeout(syncCartTimer)

  if (!currentInvoice.value || !currentInvoice.value.id) return

  syncCartTimer = setTimeout(async () => {

    try {

      const aggregatedSyncMap = new Map()
      for (const item of currentInvoice.value.cart) {
        const vid = Number(item.id)
        if (!vid) continue
        const q = Number(item.quantity || 1)
        if (aggregatedSyncMap.has(vid)) {
          aggregatedSyncMap.get(vid).soLuong += q
        } else {
          aggregatedSyncMap.set(vid, {
            idChiTietSanPham: vid,
            sanPhamId: vid,
            soLuong: q,
            donGia: item.price,
            giaDuocTinh: item.price
          })
        }
      }
      const itemsPayload = Array.from(aggregatedSyncMap.values())

      await api.post(`/api/hoa-don/${currentInvoice.value.id}/sync-cart`, itemsPayload)

      if (currentInvoice.value && Array.isArray(currentInvoice.value.cart)) {

        for (const it of currentInvoice.value.cart) {

          const p = findProduct(it)

          if (p) {

            p.dbStock = p.stock

          }

          it.syncedQuantity = it.quantity

        }

      }

    } catch (err) {

      console.warn('Đồng bộ giỏ hàng với CSDL:', err)

    }

  }, 400)

}



// Các bộ lọc bổ sung cho Modal Chọn sản phẩm

const filterColor = ref('')

const filterSize = ref('')

const filterDiscount = ref('')

const filterSort = ref('')

const productModalPage = ref(1)

const productModalPageSize = ref(8)



const availableColors = computed(() => {

  const set = new Set()

  productList.value.forEach(p => {

    if (p.color) set.add(p.color)

  })

  return Array.from(set).sort()

})



const availableSizes = computed(() => {

  const set = new Set()

  productList.value.forEach(p => {

    if (p.size) set.add(p.size)

  })

  return Array.from(set).sort()

})



const colorFilterOptions = computed(() => [

  { id: '', name: 'Tất cả màu sắc' },

  ...availableColors.value.map(c => ({ id: c, name: c }))

])



const sizeFilterOptions = computed(() => [

  { id: '', name: 'Tất cả kích cỡ' },

  ...availableSizes.value.map(s => ({ id: s, name: s }))

])



const discountFilterOptions = [

  { id: '', name: 'Tất cả ưu đãi' },

  { id: 'discount', name: 'Đang giảm giá' },

  { id: 'normal', name: 'Giá gốc (không giảm)' }

]



const sortFilterOptions = [

  { id: '', name: 'Mặc định (Mã CTSP)' },

  { id: 'price_asc', name: 'Giá: Thấp đến cao' },

  { id: 'price_desc', name: 'Giá: Cao đến thấp' },

  { id: 'stock_desc', name: 'Tồn kho: Nhiều nhất' },

  { id: 'stock_asc', name: 'Tồn kho: Ít nhất' },

  { id: 'name_asc', name: 'Tên: A đến Z' }

]



const resetProductModalFilters = () => {

  searchProductText.value = ''

  filterColor.value = ''

  filterSize.value = ''

  filterDiscount.value = ''

  filterSort.value = ''

  productModalPage.value = 1

}



watch([searchProductText, filterColor, filterSize, filterDiscount, filterSort, productModalPageSize], () => {

  productModalPage.value = 1

})



const filteredProducts = computed(() => {

  // Chỉ hiển thị sản phẩm còn hàng trong kho (sản phẩm hết hàng sẽ không hiện ở bảng)

  let list = productList.value.filter(p => p.stock > 0)



  const kw = (searchProductText.value || '').toLowerCase().trim()

  if (kw) {

    list = list.filter(p =>

      (p.name && p.name.toLowerCase().includes(kw)) ||

      (p.code && p.code.toLowerCase().includes(kw)) ||

      (p.variantCode && p.variantCode.toLowerCase().includes(kw)) ||

      (p.color && p.color.toLowerCase().includes(kw)) ||

      (p.size && p.size.toLowerCase().includes(kw))

    )

  }



  if (filterColor.value) {

    list = list.filter(p => p.color === filterColor.value)

  }



  if (filterSize.value) {

    list = list.filter(p => p.size === filterSize.value)

  }



  if (filterDiscount.value === 'discount') {

    list = list.filter(p => p.hasDiscount)

  } else if (filterDiscount.value === 'normal') {

    list = list.filter(p => !p.hasDiscount)

  }



  // Sắp xếp danh sách biến thể

  if (filterSort.value === 'price_asc') {

    list.sort((a, b) => a.price - b.price)

  } else if (filterSort.value === 'price_desc') {

    list.sort((a, b) => b.price - a.price)

  } else if (filterSort.value === 'stock_desc') {

    list.sort((a, b) => b.stock - a.stock)

  } else if (filterSort.value === 'stock_asc') {

    list.sort((a, b) => a.stock - b.stock)

  } else if (filterSort.value === 'name_asc') {

    list.sort((a, b) => (a.name || '').localeCompare(b.name || '', 'vi'))

  }



  return list

})



const productModalTotalPages = computed(() => {

  return Math.ceil(filteredProducts.value.length / productModalPageSize.value) || 1

})



const paginatedProductsModal = computed(() => {

  const start = (productModalPage.value - 1) * productModalPageSize.value

  return filteredProducts.value.slice(start, start + productModalPageSize.value)

})



const changeProductModalPage = (p) => {

  if (p < 1 || p > productModalTotalPages.value) return

  productModalPage.value = p

}



const visibleProductModalPages = computed(() => {

  const total = productModalTotalPages.value

  const curr = productModalPage.value

  if (total <= 7) {

    return Array.from({ length: total }, (_, i) => i + 1)

  }

  const pages = []

  if (curr <= 4) {

    for (let i = 1; i <= 5; i++) pages.push(i)

    pages.push('...')

    pages.push(total)

  } else if (curr >= total - 3) {

    pages.push(1)

    pages.push('...')

    for (let i = total - 4; i <= total; i++) pages.push(i)

  } else {

    pages.push(1)

    pages.push('...')

    for (let i = curr - 1; i <= curr + 1; i++) pages.push(i)

    pages.push('...')

    pages.push(total)

  }

  return pages

})



const defaultProductPlaceholder = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1.5'%3E%3Crect x='3' y='3' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'%3E%3C/circle%3E%3Cpolyline points='21 15 16 10 5 21'%3E%3C/polyline%3E%3C/svg%3E"

const formatProductImage = (url, prod) => {

  if (!url || typeof url !== 'string' || url.trim() === '' || url.includes('photo-1521572267360-ee0c2909d518')) {

    const sid = prod?.sanPhamId || prod?.id

    const scode = (prod?.code || prod?.variantCode || '').toUpperCase()

    // không tự fill ảnh mẫu sp01

    // không tự fill ảnh mẫu sp02

    // không tự fill ảnh mẫu sp03

    // không tự fill ảnh mẫu sp04

    return defaultProductPlaceholder

  }

  let cleanUrl = url.trim()

  if (cleanUrl.includes('|')) {

    cleanUrl = cleanUrl.split('|')[1].trim()

  }

  if (cleanUrl.startsWith('data:') || cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {

    return cleanUrl

  }

  if (cleanUrl.startsWith('/images/')) {

    return cleanUrl

  }

  if (cleanUrl.startsWith('/uploads/') || cleanUrl.startsWith('uploads/')) {

    const p = cleanUrl.startsWith('/') ? cleanUrl : '/' + cleanUrl

    return `http://localhost:8080${p}`

  }

  return cleanUrl.startsWith('/') ? cleanUrl : `/${cleanUrl}`

}







const findProduct = (item) => {

  if (!item) return null

  // 1. Ưu tiên tìm chính xác theo ID biến thể chi tiết (ChiTietSanPham.id)

  if (item.id != null) {

    const foundById = productList.value.find(x => Number(x.id) === Number(item.id))

    if (foundById) return foundById

  }

  // 2. Tìm theo Mã biến thể CTSP duy nhất (variantCode / maChiTietSanPham ví dụ CTSP04)

  const vCode = item.variantCode || (typeof item.code === 'string' && item.code.toUpperCase().startsWith('CTSP') ? item.code : null)

  if (vCode) {

    const targetCode = String(vCode).trim().toLowerCase()

    const foundByVCode = productList.value.find(x => x.variantCode && String(x.variantCode).trim().toLowerCase() === targetCode)

    if (foundByVCode) return foundByVCode

  }

  // 3. Fallback: Nếu item truyền vào đã là biến thể trong productList có sẵn thông tin

  if (item.stock != null && item.variantCode) {

    return item

  }

  return null

}



const getMaxStockForItem = (item) => {

  const p = findProduct(item)

  const currentSynced = Number(item?.syncedQuantity || 0)

  const dbStock = p ? Number(p.dbStock ?? p.stock ?? 0) : 0

  return Math.max(1, dbStock + currentSynced)

}



const getRemainingStock = (item) => {

  const p = findProduct(item)

  return p ? Math.max(0, p.stock) : 0

}



const selectProduct = (prod) => {
  const p = findProduct(prod) || prod
  const available = Number(p.stock ?? prod.stock ?? 0)
  if (available <= 0) {
    showToast('Sản phẩm đã hết hàng trong kho!', 'warning')
    return
  }

  const targetId = p.id ?? prod.id
  const targetVCode = p.variantCode || prod.variantCode
  const origPrice = Number(p.originalPrice || p.giaBan || prod.originalPrice || prod.giaBan || p.price || prod.price || 0)
  const salePrice = Number(p.price ?? prod.price ?? origPrice)
  const hasDiscount = Boolean(p.hasDiscount || (origPrice > salePrice && salePrice > 0))
  const discountText = p.discountText || prod.discountText || (hasDiscount ? ('-' + formatShortMoney(origPrice - salePrice)) : '')
  const discountCampaignName = p.discountCampaignName || prod.discountCampaignName || ''

  const exist = currentInvoice.value.cart.find(i => 
    (targetId != null && Number(i.id) === Number(targetId)) ||
    (targetVCode && i.variantCode && i.variantCode.toLowerCase() === targetVCode.toLowerCase())
  )

  if (exist) {
    if (available < 1) {
      showToast('Trong kho không còn đủ sản phẩm để thêm tiếp!', 'warning')
      return
    }
    exist.quantity++
    exist.price = salePrice
    exist.originalPrice = origPrice
    exist.hasDiscount = hasDiscount
    exist.discountText = discountText
    exist.discountCampaignName = discountCampaignName
  } else {
    currentInvoice.value.cart.push({
      ...p,
      id: targetId,
      variantCode: targetVCode,
      quantity: 1,
      syncedQuantity: 0,
      price: salePrice,
      originalPrice: origPrice,
      hasDiscount: hasDiscount,
      discountText: discountText,
      discountCampaignName: discountCampaignName
    })
  }



  refreshAvailableStock()

  syncCurrentCartToDb()

  showToast(`Đã thêm "${p.name || prod.name} (${p.color || prod.color} - ${p.size || prod.size})" vào giỏ hàng!`)

}



// Quét barcode / nhập mã nhanh

const handleBarcodeScan = () => {

  const code = barcodeInput.value?.trim().toLowerCase()

  if (!code) return

  const found = productList.value.find(p =>

      (p.variantCode && p.variantCode.toLowerCase() === code) ||

      String(p.id) === code ||

      (p.code && p.code.toLowerCase() === code)

  )

  if (found) {

    selectProduct(found)

    barcodeInput.value = ''

  } else {

    showToast(`Không tìm thấy sản phẩm có mã "${barcodeInput.value}" trong kho!`, 'warning')

  }

}



const updateQuantity = (item, delta) => {

  const p = findProduct(item)

  if (delta > 0) {

    if (p && p.stock < delta) {

      showToast(`Trong kho chỉ còn thêm tối đa ${p.stock} sản phẩm!`, 'warning')

      return

    }

    item.quantity += delta

    refreshAvailableStock()

    syncCurrentCartToDb()

  } else if (delta < 0) {

    const absDelta = Math.abs(delta)

    if (item.quantity <= absDelta) {

      removeFromCart(item)

      return

    }

    item.quantity -= absDelta

    refreshAvailableStock()

    syncCurrentCartToDb()

  }

}



const handleQuantityInput = (item, event) => {

  const p = findProduct(item)

  const currentSynced = Number(item?.syncedQuantity || 0)

  const dbStock = p ? Number(p.dbStock ?? p.stock ?? 0) : 0

  const maxAvailable = Math.max(1, dbStock + currentSynced)



  let raw = event.target.value

  if (raw === '') {

    if (event.type === 'change' || event.type === 'blur') {

      event.target.value = item.quantity

    }

    return

  }



  let val = parseInt(raw, 10)

  if (isNaN(val) || val <= 0) {

    val = 1

    event.target.value = 1

  }



  if (val > maxAvailable) {

    showToast(`Trong kho chỉ còn tối đa ${maxAvailable} sản phẩm!`, 'warning')

    val = maxAvailable

    event.target.value = maxAvailable

  }



  if (val !== item.quantity) {

    item.quantity = val

    refreshAvailableStock()

    syncCurrentCartToDb()

  }

  event.target.value = item.quantity

}



const removeFromCart = (item) => {

  currentInvoice.value.cart = currentInvoice.value.cart.filter(i => 

    !((item?.id != null && Number(i.id) === Number(item.id)) || (item?.variantCode && i.variantCode && item.variantCode === i.variantCode))

  )

  refreshAvailableStock()

  syncCurrentCartToDb()

  showToast('Đã xóa sản phẩm khỏi giỏ hàng!')

}



const _oldRemoveFromCart = (item) => { if (true) return;

  currentInvoice.value.cart = currentInvoice.value.cart.filter(i => i.id !== item.id)

  syncCurrentCartToDb()

  showToast('Đã xóa sản phẩm khỏi giỏ hàng!')

}



/* ==========================================================

   3. TÌM KIẾM & THÊM NHANH KHÁCH HÀNG (HOÀN TOÀN CSDL)

========================================================== */

const customerSearchResults = ref([])

const showCustomerDropdown = ref(false)

const showQuickAddCustomerModal = ref(false)



// Danh mục hành chính chuẩn 2 cấp (Tỉnh/Thành & Phường/Xã)

const provincesList = ref([])

const quickCustomerWardsList = ref([])

const quickCustomerProvinceCode = ref('')

const quickCustomerWardCode = ref('')



const loadProvincesData = async () => {

  if (provincesList.value.length === 0) {

    provincesList.value = await getProvinces()

  }

}



const onQuickCustomerProvinceChange = async () => {

  const p = provincesList.value.find(item => String(item.code) === String(quickCustomerProvinceCode.value))

  newCustomerForm.value.thanhPho = p ? p.name : ''

  quickCustomerWardCode.value = ''

  newCustomerForm.value.huyen = ''

  newCustomerForm.value.phuong = ''

  quickCustomerWardsList.value = []

  if (quickCustomerProvinceCode.value) {

    quickCustomerWardsList.value = await getWardsByProvince(quickCustomerProvinceCode.value)

  }

}



const onQuickCustomerWardChange = () => {

  const w = quickCustomerWardsList.value.find(item => String(item.code) === String(quickCustomerWardCode.value))

  newCustomerForm.value.phuong = w ? w.name : ''

  if (w && w.districtName) {

    newCustomerForm.value.huyen = w.districtName

  }

}



const newCustomerForm = ref({

  tenKhachHang: '',

  soDienThoai: '',

  email: '',

  thanhPho: '',

  huyen: '',

  phuong: '',

  diaChiCuThe: ''

})

const newCustomerErrors = ref({})



// Quản lý địa chỉ khách hàng trong Bán hàng

const customerAddressesList = ref([])

const showAddAddressModal = ref(false)

const showSelectAddressModal = ref(false)

const savingCustomerAddress = ref(false)



const newAddressForm = ref({

  thanhPho: '',

  huyen: '',

  phuong: '',

  diaChiCuThe: '',

  macDinh: false

})

const addrModalProvinceCode = ref('')

const addrModalWardCode = ref('')

const addrModalWardsList = ref([])



const formatAddrObj = (a) => {

  if (!a) return ''

  return [a.diaChiCuThe, a.phuong, a.huyen, a.thanhPho].filter(Boolean).join(', ')

}



const loadCustomerAddressesForCurrent = async (customerId) => {

  if (!customerId) {

    customerAddressesList.value = []

    return

  }

  try {

    const res = await api.get(`/api/khach-hang/${customerId}/dia-chi`)

    customerAddressesList.value = res.data || []

    if (customerAddressesList.value.length > 0) {

      const def = customerAddressesList.value.find(a => a.macDinh) || customerAddressesList.value[0]

      await syncInvoiceDeliveryFromAddress(currentInvoice.value, def)

    }

  } catch (e) {

    customerAddressesList.value = []

  }

}



// Quản lý địa chỉ giao hàng trực tiếp trên hóa đơn (2 cấp: Tỉnh/Thành & Phường/Xã)

const deliveryWardsList = ref([])

const isCalculatingShipping = ref(false)



const updateCombinedDeliveryAddress = () => {

  if (!currentInvoice.value) return

  const parts = [

    currentInvoice.value.deliveryDetail?.trim(),

    currentInvoice.value.deliveryWard?.trim(),

    currentInvoice.value.deliveryProvince?.trim()

  ].filter(Boolean)

  currentInvoice.value.deliveryAddress = parts.join(', ')

}



const onDeliveryProvinceChange = async () => {

  if (!currentInvoice.value) return

  const p = provincesList.value.find(item => String(item.code) === String(currentInvoice.value.deliveryProvinceCode))

  currentInvoice.value.deliveryProvince = p ? p.name : ''

  currentInvoice.value.deliveryWardCode = ''

  currentInvoice.value.deliveryDistrict = ''

  currentInvoice.value.deliveryWard = ''

  deliveryWardsList.value = []



  if (currentInvoice.value.deliveryProvinceCode) {

    deliveryWardsList.value = await getWardsByProvince(currentInvoice.value.deliveryProvinceCode)

  }

  updateCombinedDeliveryAddress()

  await autoCalculateShippingFee()

}



const onDeliveryWardChange = async () => {

  if (!currentInvoice.value) return

  const w = deliveryWardsList.value.find(item => String(item.code) === String(currentInvoice.value.deliveryWardCode))

  currentInvoice.value.deliveryWard = w ? w.name : ''

  if (w && w.districtName) {

    currentInvoice.value.deliveryDistrict = w.districtName

  }

  updateCombinedDeliveryAddress()

  await autoCalculateShippingFee()

}



const onDeliveryDetailInput = () => {

  updateCombinedDeliveryAddress()

}



// Tự động tính phí ship theo API GHN / Chính sách khu vực địa lý

const autoCalculateShippingFee = async () => {
  if (!currentInvoice.value || !currentInvoice.value.isDelivery) {
    if (currentInvoice.value) {
      currentInvoice.value.shippingFee = 0
    }
    return
  }

  // Nếu chưa có Tỉnh/Thành phố hoặc địa chỉ cụ thể, không tự ý gán cứng mức phí
  if (!currentInvoice.value.deliveryProvince && !currentInvoice.value.deliveryAddress && !currentInvoice.value.deliveryProvinceCode) {
    if (!currentInvoice.value.isManualShippingFee) {
      currentInvoice.value.shippingFee = 0
    }
    return
  }

  // Nếu thu ngân đã chủ động sửa phí vận chuyển, tôn trọng phí đã nhập và không tự động ghi đè
  if (currentInvoice.value.isManualShippingFee) {
    return
  }

  isCalculatingShipping.value = true

  try {

    const res = await calculateGhnShippingFee({

      provinceName: currentInvoice.value.deliveryProvince,

      districtName: currentInvoice.value.deliveryDistrict,

      wardName: currentInvoice.value.deliveryWard,

      fullAddress: currentInvoice.value.deliveryAddress,

      insuranceValue: totalAmount.value

    })

    if (res && res.fee != null) {

      currentInvoice.value.shippingFee = res.fee

      currentInvoice.value.shippingDesc = res.regionDescription || ''

      currentInvoice.value.shippingSource = res.source || ''
    }
  } catch (e) {
    console.warn('Lỗi tính phí ship GHN:', e)
  } finally {
    isCalculatingShipping.value = false
  }
}

// Gọi API GHN tính lại cước vận chuyển và thông báo cho thu ngân
const triggerRecalculateShippingFee = async () => {
  if (!currentInvoice.value || !currentInvoice.value.isDelivery) return
  if (!currentInvoice.value.deliveryProvinceCode) {
    showToast('Vui lòng chọn Tỉnh/Thành phố nhận hàng để gọi API GHN tính cước!', 'warning')
    return
  }
  await autoCalculateShippingFee()
  showToast(`Đã đồng bộ cước từ API Giao Hàng Nhanh (GHN): ${formatMoney(currentInvoice.value.shippingFee || 0)}!`, 'success')
}



const syncInvoiceDeliveryFromAddress = async (inv, addrObj) => {

  if (!inv || !addrObj) return

  await loadProvincesData()



  let rawThanhPho = ''

  let rawHuyen = ''

  let rawPhuong = ''

  let rawDiaChiCuThe = ''

  let rawNguoiNhan = ''

  let rawSdtNhan = ''



  if (typeof addrObj === 'object') {

    rawThanhPho = addrObj.thanhPho || addrObj.province || addrObj.city || ''

    rawHuyen = addrObj.huyen || addrObj.district || ''

    rawPhuong = addrObj.phuong || addrObj.ward || ''

    rawDiaChiCuThe = addrObj.diaChiCuThe || addrObj.diaChi || addrObj.addressDetail || ''

    rawNguoiNhan = addrObj.tenNguoiNhan || addrObj.nguoiNhan || ''

    rawSdtNhan = addrObj.soDienThoaiNhan || addrObj.sdtNhan || ''

  } else if (typeof addrObj === 'string') {

    const rawStr = addrObj.trim()

    const parts = rawStr.split(',').map(s => s.trim()).filter(Boolean)

    if (parts.length >= 3) {

      rawThanhPho = parts[parts.length - 1]

      rawHuyen = parts.length >= 4 ? parts[parts.length - 2] : ''

      rawPhuong = parts.length >= 4 ? parts[parts.length - 3] : parts[parts.length - 2]

      rawDiaChiCuThe = parts.slice(0, parts.length >= 4 ? parts.length - 3 : parts.length - 2).join(', ')

    } else {

      rawDiaChiCuThe = rawStr

    }

  }



  inv.deliveryDetail = rawDiaChiCuThe

  inv.deliveryProvince = rawThanhPho

  inv.deliveryDistrict = rawHuyen

  inv.deliveryWard = rawPhuong

  inv.deliveryAddress = formatAddrObj({

    diaChiCuThe: rawDiaChiCuThe,

    phuong: rawPhuong,

    huyen: rawHuyen,

    thanhPho: rawThanhPho

  }) || rawDiaChiCuThe



  if (rawNguoiNhan && (!inv.customerName || inv.customerName === 'Khách lẻ')) {

    inv.customerName = rawNguoiNhan

  }

  if (rawSdtNhan && !inv.customerPhone) {

    inv.customerPhone = rawSdtNhan

  }



  const cleanStr = (s) => (s || '').toLowerCase()

      .replace(/^(tỉnh|thành phố|tp\.|tp|quận|huyện|thị xã|tx\.|tx|phường|xã|thị trấn|tt\.|tt)\s+/gi, '')

      .trim()



  if (rawThanhPho) {

    let matchP = provincesList.value.find(p =>

        p.name.toLowerCase() === rawThanhPho.toLowerCase() ||

        cleanStr(p.name) === cleanStr(rawThanhPho) ||

        p.name.toLowerCase().includes(cleanStr(rawThanhPho)) ||

        rawThanhPho.toLowerCase().includes(cleanStr(p.name))

    )



    if (matchP) {

      inv.deliveryProvinceCode = matchP.code

      inv.deliveryProvince = matchP.name

      const wards = await getWardsByProvince(matchP.code)

      if (inv.id === currentInvoice.value?.id) {

        deliveryWardsList.value = wards

      }



      let matchW = wards.find(w =>

          w.name.toLowerCase() === rawPhuong.toLowerCase() ||

          cleanStr(w.name) === cleanStr(rawPhuong) ||

          w.name.toLowerCase().includes(cleanStr(rawPhuong)) ||

          rawPhuong.toLowerCase().includes(cleanStr(w.name))

      )



      if (matchW) {

        inv.deliveryWardCode = matchW.code

        inv.deliveryWard = matchW.name

        if (matchW.districtName && !inv.deliveryDistrict) {

          inv.deliveryDistrict = matchW.districtName

        }

      } else if (rawPhuong) {

        const customW = { code: 'CUSTOM_W_' + matchP.code, name: rawPhuong }

        wards.unshift(customW)

        inv.deliveryWardCode = customW.code

        inv.deliveryWard = customW.name

      }

    } else {

      const customP = { code: 'CUSTOM_P_' + rawThanhPho, name: rawThanhPho }

      provincesList.value.unshift(customP)

      inv.deliveryProvinceCode = customP.code

      if (rawPhuong) {

        const customW = { code: 'CUSTOM_W_' + rawPhuong, name: rawPhuong }

        if (inv.id === currentInvoice.value?.id) {

          deliveryWardsList.value = [customW]

        }

        inv.deliveryWardCode = customW.code

      }

    }

  }



  updateCombinedDeliveryAddress()

  await autoCalculateShippingFee()

}



watch(activeInvoiceId, async () => {
  showVietQrModal.value = false

  if (currentInvoice.value?.deliveryProvinceCode) {

    deliveryWardsList.value = await getWardsByProvince(currentInvoice.value.deliveryProvinceCode)

  } else {

    deliveryWardsList.value = []

  }

  if (currentInvoice.value?.customerId) {

    loadCustomerAddressesForCurrent(currentInvoice.value.customerId)

  } else {

    customerAddressesList.value = []

  }

})



watch(() => currentInvoice.value?.isDelivery, async (newVal) => {
  if (newVal) {
    if (currentInvoice.value?.customerId) {
      if (!currentInvoice.value.deliveryProvinceCode || !currentInvoice.value.deliveryAddress) {
        const def = customerAddressesList.value.find(a => a.macDinh) || customerAddressesList.value[0]
        if (def) {
          await syncInvoiceDeliveryFromAddress(currentInvoice.value, def)
        }
      }
    }
    if (currentInvoice.value?.deliveryProvince && !currentInvoice.value?.isManualShippingFee) {
      await autoCalculateShippingFee()
    }
  } else {
    if (currentInvoice.value) {
      currentInvoice.value.shippingFee = 0
      currentInvoice.value.isManualShippingFee = false
    }
  }
})




const openCustomerAddAddressModal = async () => {

  if (!currentInvoice.value.customerId) {

    showToast('Vui lòng chọn hoặc thêm khách hàng trước khi thêm địa chỉ!', 'warning')

    return

  }

  await loadProvincesData()

  newAddressForm.value = {

    thanhPho: '',

    huyen: '',

    phuong: '',

    diaChiCuThe: '',

    macDinh: customerAddressesList.value.length === 0

  }

  addrModalProvinceCode.value = ''

  addrModalWardCode.value = ''

  addrModalWardsList.value = []

  showAddAddressModal.value = true

}



const onAddrModalProvinceChange = async () => {

  const p = provincesList.value.find(item => String(item.code) === String(addrModalProvinceCode.value))

  newAddressForm.value.thanhPho = p ? p.name : ''

  addrModalWardCode.value = ''

  newAddressForm.value.huyen = ''

  newAddressForm.value.phuong = ''

  addrModalWardsList.value = []

  if (addrModalProvinceCode.value) {

    addrModalWardsList.value = await getWardsByProvince(addrModalProvinceCode.value)

  }

}



const onAddrModalWardChange = () => {

  const w = addrModalWardsList.value.find(item => String(item.code) === String(addrModalWardCode.value))

  newAddressForm.value.phuong = w ? w.name : ''

  if (w && w.districtName) {

    newAddressForm.value.huyen = w.districtName

  }

}



const submitAddCustomerAddress = async () => {

  if (!newAddressForm.value.thanhPho?.trim()) {

    showToast('Vui lòng chọn Tỉnh / Thành phố!', 'warning')

    return

  }

  if (!newAddressForm.value.phuong?.trim()) {

    showToast('Vui lòng chọn Phường / Xã!', 'warning')

    return

  }

  if (!newAddressForm.value.diaChiCuThe?.trim()) {

    showToast('Vui lòng nhập địa chỉ cụ thể!', 'warning')

    return

  }



  savingCustomerAddress.value = true

  try {

    const currentUser = getCurrentUser()

    const staffName = currentUser?.tenNhanVien || currentUser?.tenTaiKhoan || 'Nhân viên bán hàng'

    const staffId = currentUser?.id || null



    const payload = {

      idNhanVien: staffId,

      nguoiTao: staffName,

      thanhPho: newAddressForm.value.thanhPho.trim(),

      huyen: newAddressForm.value.huyen?.trim() || '',

      phuong: newAddressForm.value.phuong.trim(),

      diaChiCuThe: newAddressForm.value.diaChiCuThe.trim(),

      macDinh: newAddressForm.value.macDinh

    }

    const custId = currentInvoice.value.customerId

    const res = await api.post(`/api/khach-hang/${custId}/dia-chi`, payload)

    showToast('Thêm địa chỉ cho khách hàng thành công!')

    showAddAddressModal.value = false

    await loadCustomerAddressesForCurrent(custId)

    await syncInvoiceDeliveryFromAddress(currentInvoice.value, res.data || payload)

  } catch (err) {

    console.error('Lỗi khi thêm địa chỉ cho khách:', err)

    showToast(err.response?.data?.message || 'Không thể lưu địa chỉ vào CSDL!', 'warning')

  } finally {

    savingCustomerAddress.value = false

  }

}



const openSelectAddressModal = () => {

  if (customerAddressesList.value.length === 0) {

    showToast('Khách hàng này chưa có địa chỉ nào được lưu!', 'warning')

    return

  }

  showSelectAddressModal.value = true

}



const chooseSavedAddress = async (addr) => {

  await syncInvoiceDeliveryFromAddress(currentInvoice.value, addr)

  showSelectAddressModal.value = false

  showToast('Đã áp dụng địa chỉ nhận hàng!')

}



let searchCustomerTimer = null

const onCustomerInput = () => {

  clearTimeout(searchCustomerTimer)

  const query = currentInvoice.value.customerPhone?.trim()

  if (!query) {

    customerSearchResults.value = []

    showCustomerDropdown.value = false

    return

  }



  searchCustomerTimer = setTimeout(async () => {

    try {

      const res = await api.get('/api/khach-hang', {

        params: { keyword: query, size: 8 }

      })

      const list = res.data?.content || (Array.isArray(res.data) ? res.data : [])

      customerSearchResults.value = list

      showCustomerDropdown.value = list.length > 0

    } catch (e) {

      customerSearchResults.value = []

      showCustomerDropdown.value = false

    }

  }, 250)

}



const selectCustomer = async (cust) => {

  currentInvoice.value.customerId = cust.id

  currentInvoice.value.customerName = cust.tenKhachHang

  currentInvoice.value.customerPhone = cust.soDienThoai

  showCustomerDropdown.value = false



  // Tải danh sách địa chỉ đã lưu của khách hàng và tự động fill địa chỉ

  await loadCustomerAddressesForCurrent(cust.id)



  // Nếu khách hàng chưa có trong bảng dia-chi nhưng có địa chỉ trong thông tin khách hàng cust

  if (customerAddressesList.value.length === 0 && (cust.thanhPho || cust.diaChiCuThe || cust.diaChi || cust.phuong)) {

    await syncInvoiceDeliveryFromAddress(currentInvoice.value, cust)

  }



  // Tự động kiểm tra và áp dụng phiếu cá nhân tốt nhất cho khách hàng

  const bestVoucher = findBestPersonalVoucherForCustomer(cust.id, cust.tenKhachHang, totalAmount.value)

  if (bestVoucher) {

    currentInvoice.value.voucher = bestVoucher

    const discount = calculateDiscountAmount(bestVoucher, totalAmount.value)

    showToast(`Đã chọn "${cust.tenKhachHang}" và tự động áp dụng phiếu cá nhân "${bestVoucher.maPhieuGiamGia}" (-${formatMoney(discount)})!`)

  } else {

    // Kiểm tra số voucher cá nhân dành riêng cho khách hàng này

    const personalCount = availableVouchers.value.filter(v => isVoucherPersonal(v) && isVoucherForCustomer(v, cust.id, cust.tenKhachHang)).length

    if (personalCount > 0) {

      showToast(`Đã chọn "${cust.tenKhachHang}" (Có ${personalCount} phiếu cá nhân khả dụng)!`)

    } else {

      showToast(`Đã chọn khách hàng "${cust.tenKhachHang}"!`)

    }

  }

}



const clearSelectedCustomer = () => {

  currentInvoice.value.customerId = null

  currentInvoice.value.customerName = ''

  currentInvoice.value.customerPhone = ''

  currentInvoice.value.deliveryAddress = ''

  currentInvoice.value.deliveryProvinceCode = ''

  currentInvoice.value.deliveryWardCode = ''

  currentInvoice.value.deliveryProvince = ''

  currentInvoice.value.deliveryDistrict = ''

  currentInvoice.value.deliveryWard = ''

  currentInvoice.value.deliveryDetail = ''

  deliveryWardsList.value = []

  customerAddressesList.value = []



  // Nếu hóa đơn đang áp dụng phiếu cá nhân thì hủy áp dụng

  if (currentInvoice.value.voucher && isVoucherPersonal(currentInvoice.value.voucher)) {

    const vCode = currentInvoice.value.voucher.maPhieuGiamGia

    currentInvoice.value.voucher = null

    showToast(`Đã chuyển sang Khách lẻ và hủy phiếu cá nhân "${vCode}"!`, 'warning')

  } else {

    showToast('Đã chuyển sang Khách lẻ!')

  }

}



const openQuickAddModal = async () => {

  await loadProvincesData()

  newCustomerForm.value = {

    tenKhachHang: '',

    soDienThoai: currentInvoice.value.customerPhone || '',

    email: '',

    thanhPho: '',

    huyen: '',

    phuong: '',

    diaChiCuThe: ''

  }

  quickCustomerProvinceCode.value = ''

  quickCustomerWardCode.value = ''

  quickCustomerWardsList.value = []

  newCustomerErrors.value = {}

  showQuickAddCustomerModal.value = true

}



const submitQuickAddCustomer = async () => {

  newCustomerErrors.value = {}

  if (!newCustomerForm.value.tenKhachHang?.trim()) {

    newCustomerErrors.value.tenKhachHang = 'Vui lòng nhập họ và tên khách hàng'

  }

  const phone = newCustomerForm.value.soDienThoai?.trim() || ''

  if (!phone) {

    newCustomerErrors.value.soDienThoai = 'Vui lòng nhập số điện thoại'

  } else if (!/^(0[3|5|7|8|9])[0-9]{8}$/.test(phone)) {

    newCustomerErrors.value.soDienThoai = 'Số điện thoại phải gồm 10 chữ số hợp lệ'

  }



  if (Object.keys(newCustomerErrors.value).length > 0) return



  try {

    const fullDiaChi = [

      newCustomerForm.value.diaChiCuThe?.trim(),

      newCustomerForm.value.phuong?.trim(),

      newCustomerForm.value.thanhPho?.trim()

    ].filter(Boolean).join(', ')



    const res = await api.post('/api/khach-hang', {

      tenKhachHang: newCustomerForm.value.tenKhachHang.trim(),

      soDienThoai: phone,

      email: newCustomerForm.value.email?.trim() || null,

      thanhPho: newCustomerForm.value.thanhPho?.trim() || null,

      huyen: newCustomerForm.value.huyen?.trim() || null,

      phuong: newCustomerForm.value.phuong?.trim() || null,

      diaChiCuThe: newCustomerForm.value.diaChiCuThe?.trim() || null,

      trangThai: 1

    })

    const created = res.data || {}

    currentInvoice.value.customerId = created.id

    currentInvoice.value.customerName = created.tenKhachHang || newCustomerForm.value.tenKhachHang.trim()

    currentInvoice.value.customerPhone = created.soDienThoai || phone

    await syncInvoiceDeliveryFromAddress(currentInvoice.value, {

      thanhPho: newCustomerForm.value.thanhPho,

      huyen: newCustomerForm.value.huyen,

      phuong: newCustomerForm.value.phuong,

      diaChiCuThe: newCustomerForm.value.diaChiCuThe

    })

    showQuickAddCustomerModal.value = false

    await loadCustomerAddressesForCurrent(created.id)

    autoApplyBestPersonalVoucher(false)

    showToast('Đã thêm mới khách hàng vào cơ sở dữ liệu!')

  } catch (err) {

    console.error('Lỗi khi thêm khách hàng:', err)

    showToast(err.response?.data?.message || 'Không thể lưu khách hàng vào CSDL!', 'warning')

  }

}



/* ==========================================================

   4. QUẢN LÝ PHIẾU GIẢM GIÁ (CÔNG KHAI & CÁ NHÂN TỪ CSDL)

========================================================== */

const availableVouchers = ref([])



// Helper: Phân biệt phiếu Cá nhân vs phiếu Công khai

function isVoucherPersonal(v) {

  if (!v) return false

  if (v.hinhThuc) {

    const h = String(v.hinhThuc).trim().toLowerCase()

    if (h.includes('nhân') || h.includes('nhan')) return true

    if (h.includes('khai')) return false

  }

  if (Array.isArray(v.idKhachHangList) && v.idKhachHangList.length > 0) return true

  if (Array.isArray(v.danhSachKhachHang) && v.danhSachKhachHang.length > 0) return true

  if (Number(v.soKhachHang || 0) > 0) return true

  return false

}



// Helper: Kiểm tra voucher có được phép áp dụng cho khách hàng hiện tại hay không

function isVoucherForCustomer(v, customerId, customerName) {

  if (!v) return false

  if (!isVoucherPersonal(v)) return true // Phiếu công khai: áp dụng cho tất cả

  if (!customerId) return false // Phiếu cá nhân: yêu cầu phải có khách hàng thành viên

  if (Array.isArray(v.idKhachHangList) && v.idKhachHangList.length > 0) {

    if (v.idKhachHangList.some(id => String(id) === String(customerId))) return true

  }

  if (customerName && Array.isArray(v.danhSachKhachHang) && v.danhSachKhachHang.length > 0) {

    const cName = String(customerName).trim().toLowerCase()

    if (v.danhSachKhachHang.some(name => String(name).trim().toLowerCase() === cName)) return true

  }

  return false

}



// Helper: Tính mức tiền giảm giá thực tế của một voucher dựa vào tổng giá trị đơn hàng

function calculateDiscountAmount(v, subtotal) {

  if (!v || subtotal <= 0) return 0

  if (v.hoaDonToiThieu && subtotal < Number(v.hoaDonToiThieu)) return 0



  if (isPercentVoucher(v)) {

    const percent = Number(v.giaTriGiamGia || 0)

    let amount = (subtotal * percent) / 100

    if (v.giamToiDa && amount > Number(v.giamToiDa)) {

      amount = Number(v.giamToiDa)

    }

    return Math.min(amount, subtotal)

  } else {

    return Math.min(Number(v.giaTriGiamGia || 0), subtotal)

  }

}



// Helper: Tìm phiếu giảm giá cá nhân hợp lệ mang lại mức giảm cao nhất cho khách hàng

// Helper: Tìm phiếu giảm giá TỐT NHẤT (mức giảm cao nhất) trong tất cả phiếu hợp lệ (Công khai & Cá nhân)

function findBestVoucher(customerId, customerName, subtotal) {

  if (subtotal <= 0) return null

  const now = new Date()



  const eligible = availableVouchers.value.filter(v => {

    if (v.trangThai != null && Number(v.trangThai) !== 1) return false

    if (v.soLuongSuDung != null && Number(v.soLuongSuDung) <= 0) return false

    if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false

    if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false

    if (isVoucherPersonal(v)) {

      if (!customerId || !isVoucherForCustomer(v, customerId, customerName)) return false

    }

    if (v.hoaDonToiThieu && Number(subtotal) < Number(v.hoaDonToiThieu)) return false



    const discount = calculateDiscountAmount(v, subtotal)

    if (discount <= 0) return false



    return true

  })



  if (eligible.length === 0) return null



  eligible.sort((a, b) => {

    const discountA = calculateDiscountAmount(a, subtotal)

    const discountB = calculateDiscountAmount(b, subtotal)

    if (discountB !== discountA) {

      return discountB - discountA

    }

    const personalA = isVoucherPersonal(a) ? 1 : 0

    const personalB = isVoucherPersonal(b) ? 1 : 0

    if (personalB !== personalA) {

      return personalB - personalA

    }

    const minA = Number(a.hoaDonToiThieu || 0)

    const minB = Number(b.hoaDonToiThieu || 0)

    if (minA !== minB) {

      return minA - minB

    }

    return (Number(b.id) || 0) - (Number(a.id) || 0)

  })



  return eligible[0]

}



function autoApplyBestVoucher() {

  if (!currentInvoice.value) return

  const subtotal = totalAmount.value

  const custId = currentInvoice.value.customerId

  const custName = currentInvoice.value.customerName



  if (subtotal <= 0) {

    currentInvoice.value.voucher = null

    return

  }



  const best = findBestVoucher(custId, custName, subtotal)

  if (best) {

    if (!currentInvoice.value.voucher || currentInvoice.value.voucher.id !== best.id) {

      currentInvoice.value.voucher = best

    }

  } else {

    currentInvoice.value.voucher = null

  }

}



function findBestPersonalVoucherForCustomer(customerId, customerName, subtotal) {

  if (!customerId) return null

  const now = new Date()



  const eligible = availableVouchers.value.filter(v => {

    // Phải là phiếu cá nhân

    if (!isVoucherPersonal(v)) return false

    // Phải thuộc quyền sở hữu của khách hàng này

    if (!isVoucherForCustomer(v, customerId, customerName)) return false

    // Trạng thái đang hoạt động (1 hoặc null)

    if (v.trangThai != null && Number(v.trangThai) !== 1) return false

    // Còn số lượt sử dụng

    if (v.soLuongSuDung != null && Number(v.soLuongSuDung) <= 0) return false

    // Còn hạn sử dụng

    if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false

    if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false

    // Thỏa mãn giá trị đơn hàng tối thiểu

    if (v.hoaDonToiThieu && Number(subtotal) < Number(v.hoaDonToiThieu)) return false



    return true

  })



  if (eligible.length === 0) return null



  // Sắp xếp ưu tiên: mức giảm cao nhất

  eligible.sort((a, b) => {

    const discountA = calculateDiscountAmount(a, subtotal)

    const discountB = calculateDiscountAmount(b, subtotal)

    if (discountB !== discountA) {

      return discountB - discountA

    }

    return (Number(b.id) || 0) - (Number(a.id) || 0)

  })



  return eligible[0]

}



function autoApplyBestPersonalVoucher(silent = false) {

  if (!currentInvoice.value) return

  const custId = currentInvoice.value.customerId

  if (!custId) return



  const subtotal = totalAmount.value

  const bestVoucher = findBestPersonalVoucherForCustomer(custId, currentInvoice.value.customerName, subtotal)



  if (bestVoucher) {

    if (!currentInvoice.value.voucher || currentInvoice.value.voucher.id !== bestVoucher.id) {

      currentInvoice.value.voucher = bestVoucher

      const discount = calculateDiscountAmount(bestVoucher, subtotal)

      if (!silent) {

        showToast(`Tự động áp dụng phiếu cá nhân "${bestVoucher.maPhieuGiamGia}" (-${formatMoney(discount)})!`)

      }

    }

  }

}



const fetchVouchers = async () => {

  try {

    const res = await api.get('/api/phieu-giam-gia')

    const list = Array.isArray(res.data) ? res.data : (res.data?.content || [])

    availableVouchers.value = list.filter(v => Number(v.trangThai) === 1 || v.trangThai == null)



    // Đồng bộ lại object voucher đầy đủ cho các tab hóa đơn nếu đã có voucher

    waitingInvoices.value.forEach(inv => {

      if (inv.voucher && inv.voucher.id) {

        const fullV = availableVouchers.value.find(v => v.id === inv.voucher.id)

        if (fullV) {

          if (isVoucherPersonal(fullV) && !isVoucherForCustomer(fullV, inv.customerId, inv.customerName)) {

            inv.voucher = null

          } else {

            inv.voucher = fullV

          }

        }

      }

    })

  } catch (e) {

    console.warn('Không tải được phiếu giảm giá từ API:', e)

    availableVouchers.value = []

  }

}



// 1 = Giảm theo % | 2 = Giảm theo tiền mặt (VNĐ)

function isPercentVoucher(v) {

  if (!v) return false

  const loai = Number(v.loaiPhieuGiamGia)

  if (loai === 1) return true

  if (loai === 2) return false

  const val = Number(v.giaTriGiamGia || 0)

  return val > 0 && val <= 100

}



function formatVoucherDiscountLabel(v) {

  if (isPercentVoucher(v)) {

    return `-${v.giaTriGiamGia}%`

  }

  return `-${formatMoney(v.giaTriGiamGia)}`

}



function formatVoucherDisplayName(v) {

  if (!v) return ''

  const name = (v.tenPhieuGiamGia || '').trim()

  if (!name) return ''

  // Loại bỏ các chữ vô nghĩa/dummy như "uuu", "uu", test

  if (/^u+$/i.test(name) || name.toLowerCase() === 'uuu' || name.toLowerCase().includes('uuu')) {

    return ''

  }

  return name

}



function formatVoucherCondition(v) {

  if (!v) return ''

  const parts = []

  if (v.hoaDonToiThieu && Number(v.hoaDonToiThieu) > 0) {

    parts.push(`Đơn từ ${formatMoney(v.hoaDonToiThieu)}`)

  }

  if (isPercentVoucher(v) && v.giamToiDa && Number(v.giamToiDa) > 0) {

    parts.push(`Tối đa ${formatMoney(v.giamToiDa)}`)

  }

  return parts.length > 0 ? parts.join(' • ') : 'Áp dụng cho mọi đơn hàng'

}



// Thống kê số lượng phiếu công khai & cá nhân

const publicVouchers = computed(() => {

  return availableVouchers.value.filter(v => !isVoucherPersonal(v))

})

const publicVouchersCount = computed(() => publicVouchers.value.length)



const personalVouchers = computed(() => {

  return availableVouchers.value.filter(v => isVoucherPersonal(v))

})

const personalVouchersCount = computed(() => personalVouchers.value.length)



const customerPersonalVouchers = computed(() => {

  const custId = currentInvoice.value?.customerId

  if (!custId) return []

  return availableVouchers.value.filter(v => isVoucherPersonal(v) && isVoucherForCustomer(v, custId, currentInvoice.value?.customerName))

})

const customerPersonalCount = computed(() => customerPersonalVouchers.value.length)



// Danh sách voucher hiển thị trong combobox:

// - Khi KHÔNG chọn khách hàng: TUYỆT ĐỐI CHỈ hiển thị các phiếu giảm giá CÔNG KHAI.

// - Khi ĐÃ chọn khách hàng: Chỉ hiển thị phiếu cá nhân thuộc khách hàng đó + phiếu công khai.

const formattedVouchers = computed(() => {

  const currentCustId = currentInvoice.value?.customerId

  const currentCustName = currentInvoice.value?.customerName



  if (currentCustId) {

    const personalForCust = []

    const publicList = []



    availableVouchers.value.forEach(v => {

      if (!isVoucherPersonal(v)) {

        publicList.push({

          id: v.id,

          label: `[Công khai] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`

        })

      } else if (isVoucherForCustomer(v, currentCustId, currentCustName)) {

        personalForCust.push({

          id: v.id,

          label: `[Cá nhân] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`

        })

      }

      // Tuyệt đối KHÔNG hiển thị phiếu cá nhân của khách hàng khác

    })



    return [...personalForCust, ...publicList]

  } else {

    // Khi chưa chọn khách hàng (Khách lẻ / vãng lai): CHỈ hiển thị phiếu CÔNG KHAI

    const publicList = []



    availableVouchers.value.forEach(v => {

      if (!isVoucherPersonal(v)) {

        publicList.push({

          id: v.id,

          label: `[Công khai] ${v.maPhieuGiamGia} - ${v.tenPhieuGiamGia} (${formatVoucherDiscountLabel(v)})`

        })

      }

    })



    return publicList

  }

})



const selectedVoucherId = computed({

  get: () => currentInvoice.value?.voucher?.id || '',

  set: (val) => {

    if (!currentInvoice.value) return

    if (!val) {

      currentInvoice.value.voucher = null

      return

    }



    const found = availableVouchers.value.find(v => v.id === val || String(v.id) === String(val))

    if (!found) {

      currentInvoice.value.voucher = null

      return

    }



    // Kiểm tra tính hợp lệ nếu chọn phiếu cá nhân

    if (isVoucherPersonal(found)) {

      if (!currentInvoice.value.customerId) {

        showToast(`Phiếu "${found.maPhieuGiamGia}" là phiếu CÁ NHÂN. Vui lòng chọn khách hàng thành viên trước khi áp dụng!`, 'warning')

        const prev = currentInvoice.value.voucher

        currentInvoice.value.voucher = null

        nextTick(() => {

          currentInvoice.value.voucher = prev

        })

        return

      }



      if (!isVoucherForCustomer(found, currentInvoice.value.customerId, currentInvoice.value.customerName)) {

        const targetNames = (found.danhSachKhachHang && found.danhSachKhachHang.length > 0)

            ? found.danhSachKhachHang.join(', ')

            : 'khách hàng khác'

        showToast(`Phiếu "${found.maPhieuGiamGia}" là phiếu CÁ NHÂN chỉ dành riêng cho: ${targetNames}!`, 'warning')

        const prev = currentInvoice.value.voucher

        currentInvoice.value.voucher = null

        nextTick(() => {

          currentInvoice.value.voucher = prev

        })

        return

      }



      currentInvoice.value.voucher = found

      showToast(`Đã áp dụng phiếu cá nhân "${found.maPhieuGiamGia}" cho khách hàng ${currentInvoice.value.customerName}!`)

    } else {

      currentInvoice.value.voucher = found

      showToast(`Đã áp dụng phiếu giảm giá công khai "${found.maPhieuGiamGia}"!`)

    }

  }

})



const voucherDiscount = computed(() => {

  if (!currentInvoice.value || !currentInvoice.value.voucher) return 0

  const v = currentInvoice.value.voucher

  const subtotal = totalAmount.value



  // Nếu là phiếu cá nhân mà khách hàng không hợp lệ -> không tính giảm giá

  if (isVoucherPersonal(v) && !isVoucherForCustomer(v, currentInvoice.value.customerId, currentInvoice.value.customerName)) {

    return 0

  }



  return calculateDiscountAmount(v, subtotal)

})



// Gợi ý số tiền cần mua thêm để đạt voucher ưu đãi tốt hơn

const betterVoucherSuggestion = computed(() => {

  const subtotal = totalAmount.value

  if (!availableVouchers.value || availableVouchers.value.length === 0 || subtotal <= 0) return null

  const now = new Date()

  const custId = currentInvoice.value?.customerId

  const custName = currentInvoice.value?.customerName

  const currentDiscount = voucherDiscount.value



  const candidates = availableVouchers.value.filter(v => {

    if (v.trangThai != null && Number(v.trangThai) !== 1) return false

    if (v.soLuongSuDung != null && Number(v.soLuongSuDung) <= 0) return false

    if (v.ngayBatDau && new Date(v.ngayBatDau) > now) return false

    if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false

    if (isVoucherPersonal(v)) {

      if (!custId || !isVoucherForCustomer(v, custId, custName)) return false

    }

    const minOrder = Number(v.hoaDonToiThieu || 0)

    if (minOrder <= subtotal) return false



    const potentialDiscount = calculateDiscountAmount(v, minOrder)

    return potentialDiscount > currentDiscount

  })



  if (candidates.length === 0) return null



  candidates.sort((a, b) => {

    const diffA = Number(a.hoaDonToiThieu || 0) - subtotal

    const diffB = Number(b.hoaDonToiThieu || 0) - subtotal

    if (diffA !== diffB) return diffA - diffB

    const discA = calculateDiscountAmount(a, Number(a.hoaDonToiThieu || 0))

    const discB = calculateDiscountAmount(b, Number(b.hoaDonToiThieu || 0))

    return discB - discA

  })



  const bestCandidate = candidates[0]

  const neededAmount = Math.max(0, Number(bestCandidate.hoaDonToiThieu || 0) - subtotal)

  const potentialDiscount = calculateDiscountAmount(bestCandidate, Number(bestCandidate.hoaDonToiThieu || 0))



  return {

    voucher: bestCandidate,

    code: bestCandidate.maPhieuGiamGia,

    neededAmount,

    potentialDiscount,

    discountLabel: formatVoucherDiscountLabel(bestCandidate)

  }

})



// Modal Xác nhận đơn hàng trước khi thanh toán

const showOrderConfirmModal = ref(false)



// State quản lý 4 CASE KHÓ NGHIỆP VỤ POS

const showPriceChangeModal = ref(false)

const priceChangeData = ref({ tongTienCu: 0, tongTienMoi: 0, items: [] })



const showHandoverModal = ref(false)

const handoverReason = ref('')

const handoverStaffList = ref([])

const selectedHandoverStaffId = ref(null)

const handoverNote = ref('Bàn giao hóa đơn do tài khoản nhân viên thay đổi quyền hoặc bị khóa')

const isHandingOver = ref(false)



const openHandoverModal = async (reason) => {

  handoverReason.value = reason || 'Tài khoản nhân viên không có quyền thao tác bán hàng.'

  try {

    const res = await api.get('/api/hoa-don/pos/danh-sach-nhan-vien-ban-giao')

    handoverStaffList.value = res.data || []

    if (handoverStaffList.value.length > 0) {

      selectedHandoverStaffId.value = handoverStaffList.value[0].id

    }

  } catch (e) {

    console.error('Lỗi lấy danh sách nhân viên bàn giao:', e)

  }

  showHandoverModal.value = true

}



const confirmHandoverInvoice = async () => {

  if (!selectedHandoverStaffId.value) {

    showToast('Vui lòng chọn nhân viên tiếp nhận bàn giao!', 'warning')

    return

  }

  try {

    isHandingOver.value = true

    await api.post('/api/hoa-don/pos/ban-giao', {

      maHoaDon: currentInvoice.value.code,

      idNhanVienMoi: selectedHandoverStaffId.value,

      ghiChu: handoverNote.value

    })

    const staff = handoverStaffList.value.find(s => s.id === selectedHandoverStaffId.value)

    showToast(`Đã bàn giao hóa đơn ${currentInvoice.value.code} cho nhân viên ${staff?.tenNhanVien || ''} thành công!`, 'success')

    showHandoverModal.value = false

    await fetchWaitingInvoices()

  } catch (e) {

    showToast('Lỗi bàn giao hóa đơn: ' + (e.response?.data?.message || e.message), 'warning')

  } finally {

    isHandingOver.value = false

  }

}



const acceptNewPriceAndContinue = () => {
  if (priceChangeData.value?.items) {
    priceChangeData.value.items.forEach(cItem => {
      const targetId = cItem.idChiTietSanPham
      const cartItem = currentInvoice.value.cart.find(ci => ci.id === targetId || Number(ci.id) === Number(targetId))
      const newPrice = Number(cItem.giaMoi != null ? cItem.giaMoi : (cItem.giaHienTai != null ? cItem.giaHienTai : cartItem?.price))
      if (cartItem && !isNaN(newPrice)) {
        cartItem.price = newPrice
      }
    })
  }
  currentInvoice.value.chapNhanGiaMoi = true
  showPriceChangeModal.value = false
  showToast('Đã cập nhật đơn giá mới vào giỏ hàng!', 'success')
  showOrderConfirmModal.value = true
}



const openOrderConfirmModal = async () => {

  if (!currentInvoice.value || currentInvoice.value.cart.length === 0) {

    showToast('Vui lòng thêm sản phẩm vào giỏ hàng trước khi thanh toán!', 'warning')

    return

  }

  if (currentInvoice.value.isDelivery) {

    if (!currentInvoice.value.customerName?.trim()) {

      showToast('Vui lòng nhập tên người nhận hàng!', 'warning')

      return

    }

    const phone = currentInvoice.value.customerPhone?.trim() || ''

    if (!phone) {

      showToast('Vui lòng nhập số điện thoại người nhận hàng!', 'warning')

      return

    }

    if (!/^(0[3|5|7|8|9])[0-9]{8}$/.test(phone)) {

      showToast('Số điện thoại không đúng định dạng 10 số (VD: 0912345678)!', 'warning')

      return

    }

    if (!currentInvoice.value.deliveryAddress?.trim() || currentInvoice.value.deliveryAddress === '—') {

      showToast('Vui lòng nhập địa chỉ nhận hàng cụ thể!', 'warning')

      return

    }

  }



  // Gọi API backend kiểm tra toàn diện 4 case khó: đổi giá, sửa voucher, chuyển quyền NV, đợt giảm giá thay đổi

  const currentUser = getCurrentUser()

  const staffId = currentUser?.id || null

  try {

    const checkPayload = {

      idHoaDon: currentInvoice.value.id,

      maHoaDon: currentInvoice.value.code,

      idNhanVien: staffId,

      idKhachHang: currentInvoice.value.customerId || null,

      idPhieuGiamGia: currentInvoice.value.voucher?.id || null,

      phiShip: shippingFee.value,

      tongTien: totalAmount.value,

      tongTienGiamGia: voucherDiscount.value,

      chiTietList: currentInvoice.value.cart.map(item => ({

        sanPhamId: item.id,

        idChiTietSanPham: item.id,

        soLuong: item.quantity,

        donGia: item.price,

        giaDuocTinh: item.price

      })),

      items: currentInvoice.value.cart.map(item => ({

        sanPhamId: item.id,

        idChiTietSanPham: item.id,

        soLuong: item.quantity,

        donGia: item.price,

        giaDuocTinh: item.price

      }))

    }

    const checkRes = await api.post('/api/hoa-don/pos/kiem-tra-hop-le', checkPayload)

    const checkData = checkRes.data || {}



    // Case 3: Chuyển quyền nhân viên / Khóa tài khoản

    if (checkData.nhanVienHopLe === false) {

      await openHandoverModal(checkData.nhanVienLyDo)

      return

    }



    // Case 2: Sửa voucher / Hết hạn / Đổi điều kiện

    if (currentInvoice.value.voucher) {

      if (checkData.voucherHopLe === false) {
        showToast(checkData.voucherLyDo || checkData.lyDoVoucher || 'Voucher không còn hợp lệ và đã bị gỡ khỏi đơn hàng!', 'warning')
        currentInvoice.value.voucher = null
        currentInvoice.value.voucherDiscount = 0
      } else if (checkData.thayDoiVoucher === true) {

        showToast(`Mức giảm voucher đã được cập nhật lại thành: ${formatMoney(checkData.soTienGiamMoi)}đ!`, 'info')

        currentInvoice.value.voucherDiscount = checkData.soTienGiamMoi

      }

    }



    // Case 1 & 4: Đổi giá sản phẩm hoặc Đợt giảm giá thay đổi

    if (checkData.thayDoiGia === true) {

      priceChangeData.value = {

        tongTienCu: checkData.tongTienCu,

        tongTienMoi: checkData.tongTienMoi,

        items: checkData.danhSachThayDoiGia || []

      }

      showPriceChangeModal.value = true

      return

    }

  } catch (err) {

    console.warn('Lỗi kiểm tra tính hợp lệ đơn hàng POS:', err)

  }



  if (currentInvoice.value.paymentMethod === 'TIEN_MAT') {

    if (currentInvoice.value.customerPay == null || currentInvoice.value.customerPay === '') {
      currentInvoice.value.customerPay = finalTotal.value
    }
    const pay = Number(currentInvoice.value.customerPay)

    if (pay < finalTotal.value) {

      showToast(`Khách thanh toán chưa đủ! Cần: ${formatMoney(finalTotal.value)} (Còn thiếu: ${formatMoney(finalTotal.value - pay)})`, 'warning')

      return

    }

  } else if (currentInvoice.value.paymentMethod === 'KET_HOP') {

    const cash = Number(currentInvoice.value.cashAmount || 0)

    const transfer = Number(currentInvoice.value.transferAmount || 0)

    if (cash + transfer < finalTotal.value) {

      showToast(`Khách thanh toán kết hợp chưa đủ! Cần: ${formatMoney(finalTotal.value)} (Còn thiếu: ${formatMoney(finalTotal.value - (cash + transfer))})`, 'warning')

      return

    }

  }



  showOrderConfirmModal.value = true

}



const confirmOrderAndCheckout = async () => {

  showOrderConfirmModal.value = false

  await handleCheckout()

}



/* ==========================================================

   5. TÍNH TOÁN TIỀN TỆ & TIỀN THỪA

========================================================== */

const totalQuantity = computed(() => {

  if (!currentInvoice.value) return 0

  return currentInvoice.value.cart.reduce((sum, item) => sum + item.quantity, 0)

})



const totalAmount = computed(() => {

  if (!currentInvoice.value) return 0

  return currentInvoice.value.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)

})



const shippingFee = computed(() => {

  if (!currentInvoice.value || !currentInvoice.value.isDelivery) return 0

  const fee = currentInvoice.value.shippingFee

  if (fee === '' || fee == null || isNaN(fee)) return 0

  return Math.max(0, Number(fee))

})



const finalTotal = computed(() => {

  return Math.max(0, totalAmount.value + shippingFee.value - voucherDiscount.value)

})



const combinedTotalPaid = computed(() => {

  if (!currentInvoice.value) return 0

  if (currentInvoice.value.paymentMethod === 'KET_HOP') {

    return (Number(currentInvoice.value.cashAmount) || 0) + (Number(currentInvoice.value.transferAmount) || 0)

  }

  if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') {

    return finalTotal.value

  }

  if (currentInvoice.value.customerPay == null || currentInvoice.value.customerPay === '') {
    return finalTotal.value
  }
  return Number(currentInvoice.value.customerPay || 0)

})



const missingAmount = computed(() => {

  if (!currentInvoice.value) return 0

  if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') return 0

  const totalPaid = combinedTotalPaid.value

  return Math.max(0, finalTotal.value - totalPaid)

})



const changeAmount = computed(() => {

  if (!currentInvoice.value) return 0

  if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') return 0

  if (currentInvoice.value.paymentMethod === 'KET_HOP') {

    const cash = Number(currentInvoice.value.cashAmount || 0)

    const transfer = Number(currentInvoice.value.transferAmount || 0)

    return Math.max(0, (cash + transfer) - finalTotal.value)

  }

  const pay = Number(currentInvoice.value.customerPay || 0)

  return Math.max(0, pay - finalTotal.value)

})



const setExactPayment = () => {

  currentInvoice.value.customerPay = finalTotal.value

}



const addQuickCash = (delta) => {

  const current = Number(currentInvoice.value.customerPay || 0)

  currentInvoice.value.customerPay = current + delta

}



// =========================================================================

// ĐỊNH DẠNG SỐ TIỀN VỚI DẤU CHẤM PHÂN CÁCH HÀNG NGHÌN (VD: 1250000 -> 1.250.000)

// =========================================================================

const formatNumberWithDots = (val) => {

  if (val === null || val === undefined || val === '') return ''

  const num = typeof val === 'number' ? val : Number(String(val).replace(/\D/g, ''))

  if (Number.isNaN(num)) return ''

  return String(Math.round(num)).replace(/\B(?=(\d{3})+(?!\d))/g, '.')

}



const parseNumberFromDots = (str) => {

  if (str === null || str === undefined || str === '') return null

  const cleaned = String(str).replace(/\D/g, '')

  return cleaned ? Number(cleaned) : null

}



const displayCustomerPay = computed(() => {

  return formatNumberWithDots(currentInvoice.value?.customerPay)

})



const onCustomerPayFormattedInput = (e) => {

  const val = parseNumberFromDots(e.target.value)

  if (currentInvoice.value) {

    currentInvoice.value.customerPay = val

  }

  e.target.value = formatNumberWithDots(val)

}



const displayCashAmount = computed(() => {

  return formatNumberWithDots(currentInvoice.value?.cashAmount)

})



const onCashAmountFormattedInput = (e) => {

  const val = parseNumberFromDots(e.target.value)

  if (currentInvoice.value) {

    currentInvoice.value.cashAmount = val

  }

  onCashAmountInput()

  e.target.value = formatNumberWithDots(val)

}



const displayTransferAmount = computed(() => {

  return formatNumberWithDots(currentInvoice.value?.transferAmount)

})



const onTransferAmountFormattedInput = (e) => {

  const val = parseNumberFromDots(e.target.value)

  if (currentInvoice.value) {

    currentInvoice.value.transferAmount = val

  }

  onTransferAmountInput()

  e.target.value = formatNumberWithDots(val)

}



const displayShippingFee = computed(() => {

  return formatNumberWithDots(currentInvoice.value?.shippingFee)

})



const onShippingFeeFormattedInput = (e) => {

  const val = parseNumberFromDots(e.target.value)

  if (currentInvoice.value) {

    currentInvoice.value.shippingFee = val
    currentInvoice.value.isManualShippingFee = true

  }

  e.target.value = formatNumberWithDots(val)

}



// Khởi tạo các giá trị tiền khi chọn phương thức thanh toán kết hợp

const initKetHopAmounts = () => {

  if (!currentInvoice.value) return

  const total = finalTotal.value

  const curCash = currentInvoice.value.cashAmount

  const curTransfer = currentInvoice.value.transferAmount



  if ((curCash == null || curCash === '') && (curTransfer == null || curTransfer === '')) {

    currentInvoice.value.transferAmount = total > 0 ? total : 0

    currentInvoice.value.cashAmount = null

  } else if (curCash != null && (curTransfer == null || curTransfer === '')) {

    currentInvoice.value.transferAmount = Math.max(0, total - Number(curCash))

  } else if (curTransfer != null && (curCash == null || curCash === '')) {

    currentInvoice.value.cashAmount = Math.max(0, total - Number(curTransfer))

  }

}



// Tự động tính phần còn lại khi thu ngân gõ số tiền mặt

const onCashAmountInput = () => {

  if (!currentInvoice.value) return

  const total = finalTotal.value

  const cash = currentInvoice.value.cashAmount

  if (cash === '' || cash == null) {

    currentInvoice.value.transferAmount = total

  } else {

    const cashNum = Math.max(0, Number(cash) || 0)

    currentInvoice.value.transferAmount = Math.max(0, total - cashNum)

  }

}



// Tự động tính phần còn lại khi thu ngân gõ số tiền chuyển khoản

const onTransferAmountInput = () => {

  if (!currentInvoice.value) return

  const total = finalTotal.value

  const transfer = currentInvoice.value.transferAmount

  if (transfer === '' || transfer == null) {

    currentInvoice.value.cashAmount = total

  } else {

    const transNum = Math.max(0, Number(transfer) || 0)

    if (transNum <= total) {

      currentInvoice.value.cashAmount = Math.max(0, total - transNum)

    }

  }

}



const fillCashRemaining = () => {

  if (!currentInvoice.value) return

  const trans = Number(currentInvoice.value.transferAmount || 0)

  currentInvoice.value.cashAmount = Math.max(0, finalTotal.value - trans)

}



const fillTransferRemaining = () => {

  if (!currentInvoice.value) return

  const cash = Number(currentInvoice.value.cashAmount || 0)

  currentInvoice.value.transferAmount = Math.max(0, finalTotal.value - cash)

}



/* ==========================================================

   TÍCH HỢP API THANH TOÁN CHUYỂN KHOẢN (VIETQR / BANK API)

========================================================== */

const isCheckingPayment = ref(false)

const isConfirmingPayment = ref(false)

const showVietQrModal = ref(false)

const selectPaymentMethod = (method) => {
  if (!currentInvoice.value) return
  currentInvoice.value.paymentMethod = method
  if (method === 'CHUYEN_KHOAN') {
    currentInvoice.value.customerPay = finalTotal.value
    fetchTransferInfoFromApi(finalTotal.value)
    if (!currentInvoice.value.isTransferPaid) startPaymentPolling()
    openVietQrModal()
  } else if (method === 'TIEN_MAT') {
    stopPaymentPolling()
    const curPay = currentInvoice.value.customerPay
    if (curPay === null || curPay === undefined || curPay === '' || curPay === 0) {
      currentInvoice.value.customerPay = finalTotal.value > 0 ? finalTotal.value : null
    }
  } else if (method === 'KET_HOP') {
    initKetHopAmounts()
    if (Number(currentInvoice.value.transferAmount) > 0) {
      fetchTransferInfoFromApi(currentInvoice.value.transferAmount)
      if (!currentInvoice.value.isTransferPaid) startPaymentPolling()
    } else {
      stopPaymentPolling()
    }
  }
}

const handleSelectChuyenKhoan = () => {
  if (!currentInvoice.value) return
  currentInvoice.value.paymentMethod = 'CHUYEN_KHOAN'
  currentInvoice.value.customerPay = finalTotal.value
  openVietQrModal()
}

const openVietQrModal = () => {
  if (!currentInvoice.value) return
  if (currentInvoice.value.cart.length === 0) {
    showToast('Giỏ hàng trống! Vui lòng chọn sản phẩm vào giỏ trước khi tạo mã chuyển khoản.', 'warning')
    return
  }
  const amt = transferTargetAmount.value
  fetchTransferInfoFromApi(amt)
  if (!currentInvoice.value?.isTransferPaid) {
    startPaymentPolling()
  }
  showVietQrModal.value = true
}

const closeVietQrModal = () => {
  showVietQrModal.value = false
}

const handleConfirmVietQrInModal = async () => {
  try {
    currentInvoice.value.isTransferVerifiedByStaff = true
    await confirmTransferPaymentDirect()
    showVietQrModal.value = false
    showToast('Đang hoàn tất đơn hàng và chuyển sang in hóa đơn...', 'info')
    await handleCheckout()
  } catch (err) {
    console.error('Lỗi khi hoàn tất đơn hàng từ modal VietQR:', err)
  }
}


let paymentPollingTimer = null



// Chế độ xem mã QR: 'VNPAY' | 'VIETQR'

const qrViewMode = ref('VIETQR')



const transferApiInfo = ref({

  bankId: 'MB',

  bankName: 'MB Bank (970422)',

  accountNo: '0988888888',

  accountName: 'FF T-SHIRT STORE',

  qrCodeUrl: '',

  vnpayQrUrl: '',

  vietQrUrl: '',

  vnpayUrl: '',

  content: '',

  amount: 0,

  transactionCode: ''

})



// Số tiền mục tiêu chuyển khoản của đơn hàng hiện tại

const transferTargetAmount = computed(() => {

  if (!currentInvoice.value) return 0

  if (currentInvoice.value.paymentMethod === 'KET_HOP') {

    return Math.max(0, Number(currentInvoice.value.transferAmount) || 0)

  }

  return finalTotal.value

})



// Nội dung thanh toán VNPAY chuẩn hóa

const transferContent = computed(() => {
  const code = currentInvoice.value?.code || 'POS'
  const clean = code.replace(/[^a-zA-Z0-9_-]/g, '')
  return `FFTSHIRT ${clean}`
})



// Tạo URL mã VietQR động Napas 247 theo số tiền và mã hóa đơn

const getVietQrUrl = (amount) => {

  const code = currentInvoice.value?.code || 'POS'

  const amt = Math.max(0, Math.round(Number(amount) || 0))

  const cleanCode = code.replace(/[^a-zA-Z0-9_-]/g, '')

  const noiDung = `FFTSHIRT ${cleanCode}`

  return `https://img.vietqr.io/image/MB-0988888888-compact2.png?amount=${amt}&addInfo=${encodeURIComponent(noiDung)}&accountName=FF%20TSHIRT%20STORE`

}



// Tạo URL mã MoMo QR động theo số tiền và mã hóa đơn

const getMoMoQrUrl = (amount) => {

  const code = currentInvoice.value?.code || 'POS'

  const amt = Math.max(0, Math.round(Number(amount) || 0))

  const cleanCode = code.replace(/[^a-zA-Z0-9_-]/g, '')

  const noiDung = `MOMO ${cleanCode}`

  return `https://api.qrserver.com/v1/create-qr-code/?size=300x300&margin=8&data=2|99|0988888888|FF%20TSHIRT%20STORE||0|0|${amt}|${encodeURIComponent(noiDung)}|transfer_myqr`

}



const transferQrUrl = computed(() => {
  const amt = transferTargetAmount.value
  if (transferApiInfo.value.vietQrUrl) return transferApiInfo.value.vietQrUrl
  if (transferApiInfo.value.qrCodeUrl) return transferApiInfo.value.qrCodeUrl
  return getVietQrUrl(amt)
})



// Mở trang cổng thanh toán VNPAY (hỗ trợ SDK vnpay.js từ vnpay_pay.jsp mẫu hoặc popup)

const openVNPayPortal = () => {

  const targetUrl = transferApiInfo.value.vnpayUrl

  if (targetUrl) {

    if (window.vnpay && typeof window.vnpay.open === 'function') {

      window.vnpay.open({ width: 768, height: 600, url: targetUrl })

    } else {

      window.open(targetUrl, '_blank')

    }

  } else {

    const code = currentInvoice.value?.code || 'POS'

    const amt = transferTargetAmount.value

    api.get(`/api/payment/vnpay/create-url?maHoaDon=${code}&soTien=${amt}&bankCode=`)

        .then(res => {

          if (res.data?.url) {

            transferApiInfo.value.vnpayUrl = res.data.url

            if (window.vnpay && typeof window.vnpay.open === 'function') {

              window.vnpay.open({ width: 768, height: 600, url: res.data.url })

            } else {

              window.open(res.data.url, '_blank')

            }

          }

        })

        .catch(() => showToast('Không thể mở cổng VNPAY lúc này', 'warning'))

  }

}



// Gọi API lấy thông tin chuyển khoản từ backend

const fetchTransferInfoFromApi = async (amount) => {

  if (!currentInvoice.value) return

  const code = currentInvoice.value.code || 'POS'

  const amt = Math.max(0, Math.round(Number(amount) || 0))

  try {

    const res = await api.post('/api/payment/transfer-info', {

      maHoaDon: code,

      soTien: amt

    })

    if (res?.data) {

      transferApiInfo.value = {

        bankId: res.data.maNganHang || 'VNPAY',

        bankName: res.data.nganHang || 'VNPAY-QR',

        accountNo: res.data.soTaiKhoan || '0988888888',

        accountName: res.data.tenChuTaiKhoan || 'FF T-SHIRT STORE',

        qrCodeUrl: res.data.qrCodeUrl || '',

        vnpayQrUrl: res.data.vnpayQrUrl || res.data.qrCodeUrl || '',

        vietQrUrl: res.data.vietQrUrl || getVietQrUrl(amt),

        vnpayUrl: res.data.vnpayUrl || '',

        content: res.data.noiDungChuyenKhoan || `VNPAY ${code}`,

        amount: amt,

        transactionCode: res.data.maGiaoDich || ''

      }

      if (res.data.daThanhToan) {

        currentInvoice.value.isTransferPaid = true

      }

    }

  } catch (err) {

    console.warn('API /api/payment/transfer-info chưa sẵn sàng, dùng fallback cấu hình:', err)

  }

}



// Sao chép thông tin vào clipboard

const copyToClipboard = async (text, label) => {

  if (!text && text !== 0) return

  try {

    if (navigator?.clipboard?.writeText) {

      await navigator.clipboard.writeText(String(text))

    } else {

      const textarea = document.createElement('textarea')

      textarea.value = String(text)

      textarea.style.position = 'fixed'

      textarea.style.opacity = '0'

      document.body.appendChild(textarea)

      textarea.select()

      document.execCommand('copy')

      document.body.removeChild(textarea)

    }

    showToast(`Đã sao chép ${label}: ${text}`)

  } catch (e) {

    showToast(`Không thể sao chép ${label}`, 'warning')

  }

}



// Kiểm tra trạng thái thanh toán từ API

const checkPaymentStatus = async (isManual = true) => {
  if (!currentInvoice.value) return
  const code = currentInvoice.value.code
  if (!code) return

  try {
    if (isManual) isCheckingPayment.value = true
    const res = await api.get(`/api/payment/check-status/${code}`)
    if (res?.data?.daThanhToan) {
      currentInvoice.value.isTransferPaid = true
      currentInvoice.value.transferTransactionCode = res.data.maGiaoDich || `TRANS-${code}`
      stopPaymentPolling()

      const isKetHopCheck = currentInvoice.value.paymentMethod === 'KET_HOP'
      if (!isKetHopCheck) {
        currentInvoice.value.paymentMethod = 'CHUYEN_KHOAN'
        currentInvoice.value.customerPay = finalTotal.value
      }

      showToast(`Đã nhận thanh toán chuyển khoản cho hóa đơn ${code}! Đang chuyển sang in hóa đơn...`, 'success')
      stopPaymentPolling()
      showVietQrModal.value = false
      setTimeout(() => {
        handleCheckout()
      }, 500)
    } else {
      if (isManual) {
        showToast(`Đang chờ giao dịch chuyển khoản cho đơn hàng ${code}...`, 'info')
      }
    }
  } catch (err) {
    if (isManual) {
      showToast('Không thể kết nối đến máy chủ kiểm tra thanh toán!', 'warning')
    }
  } finally {
    if (isManual) isCheckingPayment.value = false
  }
}

// Xác nhận đã nhận tiền chuyển khoản trực tiếp (Khi khách chuyển thành công)

const confirmTransferPaymentDirect = async () => {
  if (currentInvoice.value && currentInvoice.value.cart.length === 0) {
    showToast('Giỏ hàng trống! Vui lòng chọn sản phẩm trước khi thanh toán.', 'warning')
    return
  }
  if (!currentInvoice.value) return

  const code = currentInvoice.value.code
  const isKetHop = currentInvoice.value.paymentMethod === 'KET_HOP'
  const amt = isKetHop ? Number(currentInvoice.value.transferAmount || 0) : finalTotal.value

  try {
    isConfirmingPayment.value = true
    currentInvoice.value.isTransferPaid = true
    currentInvoice.value.transferTransactionCode = `TRANS-${code}`
    stopPaymentPolling()

    if (!isKetHop) {
      currentInvoice.value.paymentMethod = 'CHUYEN_KHOAN'
      currentInvoice.value.customerPay = finalTotal.value
    }

    const res = await api.post('/api/payment/confirm-transfer', {
      maHoaDon: code,
      soTien: amt,
      ghiChu: 'Thu ngân xác nhận chuyển khoản thành công',
      phuongThuc: 'Chuyển khoản'
    })

    if (res?.data?.maGiaoDich) {
      currentInvoice.value.transferTransactionCode = res.data.maGiaoDich
    }

    showToast(`Đã xác nhận thanh toán chuyển khoản ${formatMoney(amt)} thành công!`, 'success')
  } catch (err) {
    console.error('Lỗi khi xác nhận chuyển khoản:', err)
    showToast('Lỗi khi ghi nhận thanh toán: ' + (err.response?.data?.message || err.message), 'warning')
  } finally {
    isConfirmingPayment.value = false
    stopPaymentPolling()
  }
}

// Tự động Polling kiểm tra thanh toán mỗi 4 giây

const startPaymentPolling = () => {

  stopPaymentPolling()

  // Tắt tự động polling thanh toán

  return

}

const _unusedPolling = () => {

  stopPaymentPolling()

  paymentPollingTimer = setInterval(() => {

    if (

        currentInvoice.value &&

        !currentInvoice.value.isTransferPaid &&

        (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN' ||

            (currentInvoice.value.paymentMethod === 'KET_HOP' && Number(currentInvoice.value.transferAmount) > 0))

    ) {

      checkPaymentStatus(false)

    } else {

      stopPaymentPolling()

    }

  }, 4000)

}



const stopPaymentPolling = () => {

  if (paymentPollingTimer) {

    clearInterval(paymentPollingTimer)

    paymentPollingTimer = null

  }

}



onUnmounted(() => {

  if (typeof dayCheckTimer !== 'undefined' && dayCheckTimer) clearInterval(dayCheckTimer)

  if (typeof onTabVisibilityChange === 'function') {

    document.removeEventListener('visibilitychange', onTabVisibilityChange)

  }

  stopPaymentPolling()

})



// Tự động cập nhật số tiền khách đưa khi tổng tiền hóa đơn thay đổi

watch(

    finalTotal,

    (newTotal, oldTotal) => {

      if (!currentInvoice.value) return

      const curPay = currentInvoice.value.customerPay

      // Nếu chưa nhập tiền, hoặc bằng 0, hoặc trước đó đang bằng đúng oldTotal thì tự động cập nhật theo newTotal

      if (curPay === null || curPay === undefined || curPay === '' || curPay === 0 || (oldTotal != null && Number(curPay) === Number(oldTotal))) {

        currentInvoice.value.customerPay = newTotal > 0 ? newTotal : null

      }



      if (currentInvoice.value.paymentMethod === 'KET_HOP') {

        const curCash = currentInvoice.value.cashAmount

        if (curCash != null && curCash !== '' && Number(curCash) > 0) {

          currentInvoice.value.transferAmount = Math.max(0, newTotal - Number(curCash))

        } else {

          currentInvoice.value.transferAmount = newTotal

        }

      } else if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') {

        currentInvoice.value.customerPay = newTotal

        fetchTransferInfoFromApi(newTotal)

      }

    },

    { immediate: true }

)



// Khi chuyển tab hóa đơn

watch(

    () => currentInvoice.value?.id,

    () => {

      stopPaymentPolling()

      if (!currentInvoice.value) return

      const curPay = currentInvoice.value.customerPay

      if (curPay === null || curPay === undefined || curPay === '' || curPay === 0) {

        if (finalTotal.value > 0) {

          currentInvoice.value.customerPay = finalTotal.value

        }

      }

      if (currentInvoice.value.paymentMethod === 'KET_HOP') {

        initKetHopAmounts()

        if (Number(currentInvoice.value.transferAmount) > 0) {

          fetchTransferInfoFromApi(currentInvoice.value.transferAmount)

          if (!currentInvoice.value.isTransferPaid) startPaymentPolling()

        }

      } else if (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN') {

        fetchTransferInfoFromApi(finalTotal.value)

        if (!currentInvoice.value.isTransferPaid) startPaymentPolling()

      }

    },

    { immediate: true }

)



watch(() => currentInvoice.value?.paymentMethod, (newVal) => {

  if (newVal === 'CHUYEN_KHOAN') {
    currentInvoice.value.customerPay = finalTotal.value
    if (currentInvoice.value.cart.length > 0) {
      fetchTransferInfoFromApi(finalTotal.value)
      if (!currentInvoice.value.isTransferPaid) startPaymentPolling()
      openVietQrModal()
    } else {
      showToast('Giỏ hàng trống! Vui lòng chọn sản phẩm vào giỏ trước khi tạo mã chuyển khoản.', 'warning')
    }
  } else if (newVal === 'TIEN_MAT') {

    stopPaymentPolling()

    const curPay = currentInvoice.value.customerPay

    if (curPay === null || curPay === undefined || curPay === '' || curPay === 0) {

      currentInvoice.value.customerPay = finalTotal.value > 0 ? finalTotal.value : null

    }

  } else if (newVal === 'KET_HOP') {

    initKetHopAmounts()

    if (Number(currentInvoice.value.transferAmount) > 0) {

      fetchTransferInfoFromApi(currentInvoice.value.transferAmount)

      if (!currentInvoice.value.isTransferPaid) startPaymentPolling()

    } else {

      stopPaymentPolling()

    }

  }

})



// Theo dõi khi số tiền chuyển khoản ở hình thức Kết Hợp thay đổi

watch(() => currentInvoice.value?.transferAmount, (newVal) => {

  if (currentInvoice.value?.paymentMethod === 'KET_HOP') {

    const amt = Number(newVal) || 0

    if (amt > 0) {

      fetchTransferInfoFromApi(amt)

      if (!currentInvoice.value.isTransferPaid) startPaymentPolling()

    } else {

      stopPaymentPolling()

    }

  }

})







// Tự động kiểm tra và hủy phiếu cá nhân nếu đổi hoặc xóa khách hàng trên hóa đơn

watch(

    () => currentInvoice.value?.customerId,

    (newCustId, oldCustId) => {

      if (newCustId === oldCustId) return

      const curVoucher = currentInvoice.value?.voucher

      if (curVoucher && isVoucherPersonal(curVoucher)) {

        if (!newCustId || !isVoucherForCustomer(curVoucher, newCustId, currentInvoice.value?.customerName)) {

          currentInvoice.value.voucher = null

          if (!newCustId) {

            showToast(`Đã hủy phiếu giảm giá cá nhân "${curVoucher.maPhieuGiamGia}" vì chuyển sang Khách lẻ!`, 'warning')

          } else {

            showToast(`Đã hủy phiếu giảm giá cá nhân "${curVoucher.maPhieuGiamGia}" vì không thuộc khách hàng mới!`, 'warning')

          }

        }

      }

      // Nếu chọn khách hàng mới và chưa có voucher, tự động áp dụng phiếu cá nhân tốt nhất

      if (newCustId && !currentInvoice.value?.voucher) {

        autoApplyBestPersonalVoucher(false)

      }

    }

)



// Khi thêm hoặc bớt sản phẩm làm thay đổi tổng tiền:

// Nếu đơn hàng có khách hàng và chưa có voucher, tự động áp dụng phiếu cá nhân thỏa mãn điều kiện tối thiểu

// Tự động kiểm tra và áp dụng phiếu giảm giá tốt nhất khi giỏ hàng, khách hàng hoặc tab hóa đơn thay đổi

watch(

  [

    totalAmount,

    () => currentInvoice.value?.customerId,

    () => currentInvoice.value?.customerName,

    () => currentInvoice.value?.id,

    availableVouchers

  ],

  () => {

    autoApplyBestVoucher()

  },

  { immediate: true }

)



function formatMoney(val) {

  return Number(val || 0).toLocaleString('vi-VN') + ' đ'

}



/* ==========================================================

   6. THANH TOÁN ĐƠN HÀNG (POS CHECKOUT)

========================================================== */

const isProcessing = ref(false)



const handleCheckout = async () => {

  if (currentInvoice.value.cart.length === 0) {

    showToast('Giỏ hàng trống! Vui lòng chọn sản phẩm trước khi thanh toán.', 'warning')

    return

  }



  // Kiểm tra tính hợp lệ của phiếu giảm giá trước khi thanh toán

  if (currentInvoice.value.voucher) {

    const v = currentInvoice.value.voucher

    if (isVoucherPersonal(v) && !isVoucherForCustomer(v, currentInvoice.value.customerId, currentInvoice.value.customerName)) {

      showToast(`Phiếu giảm giá "${v.maPhieuGiamGia}" là phiếu CÁ NHÂN không thuộc khách hàng này!`, 'warning')

      return

    }

    if (v.hoaDonToiThieu && totalAmount.value < Number(v.hoaDonToiThieu)) {
      showToast(`Đơn hàng (${formatMoney(totalAmount.value)}) chưa đạt mức tối thiểu ${formatMoney(v.hoaDonToiThieu)} của voucher "${v.maPhieuGiamGia}", đã tự động gỡ voucher để tiếp tục thanh toán.`, 'info')
      currentInvoice.value.voucher = null
    }

  }



  if (currentInvoice.value.isDelivery) {

    if (!currentInvoice.value.customerName?.trim() || currentInvoice.value.customerName.trim().toLowerCase() === 'khách lẻ') {

      showToast('Đơn giao hàng bắt buộc phải có tên người nhận cụ thể (không được để "Khách lẻ")!', 'warning')

      return

    }

    if (!currentInvoice.value.customerPhone?.trim()) {

      showToast('Vui lòng nhập số điện thoại người nhận khi chọn giao hàng!', 'warning')

      return

    }

    if (!currentInvoice.value.deliveryAddress?.trim()) {

      showToast('Vui lòng nhập địa chỉ nhận hàng khi chọn giao hàng!', 'warning')

      return

    }

  }



  if (currentInvoice.value.customerPhone?.trim()) {

    const phone = currentInvoice.value.customerPhone.trim()

    if (!/^(0[3|5|7|8|9])[0-9]{8}$/.test(phone)) {

      showToast('Số điện thoại không đúng định dạng 10 số (VD: 0912345678)!', 'warning')

      return

    }

  }



  if (currentInvoice.value.paymentMethod === 'TIEN_MAT') {

    if (currentInvoice.value.customerPay == null || currentInvoice.value.customerPay === '') {
      currentInvoice.value.customerPay = finalTotal.value
    }
    const pay = Number(currentInvoice.value.customerPay)

    if (pay < finalTotal.value) {

      showToast(`Khách thanh toán chưa đủ! Cần thanh toán: ${formatMoney(finalTotal.value)} (Còn thiếu: ${formatMoney(finalTotal.value - pay)})`, 'warning')

      return

    }

  } else if (currentInvoice.value.paymentMethod === 'KET_HOP') {

    const cash = Number(currentInvoice.value.cashAmount || 0)

    const transfer = Number(currentInvoice.value.transferAmount || 0)

    if (cash + transfer < finalTotal.value) {

      showToast(`Khách thanh toán kết hợp chưa đủ! Cần: ${formatMoney(finalTotal.value)} (Còn thiếu: ${formatMoney(finalTotal.value - (cash + transfer))})`, 'warning')

      return

    }

  }



  try {

    isProcessing.value = true



    const isKetHop = currentInvoice.value.paymentMethod === 'KET_HOP'

    const cashVal = isKetHop

        ? Number(currentInvoice.value.cashAmount || 0)

        : (currentInvoice.value.paymentMethod === 'TIEN_MAT' ? Number(currentInvoice.value.customerPay || finalTotal.value) : 0)

    const transferVal = isKetHop

        ? Number(currentInvoice.value.transferAmount || 0)

        : (currentInvoice.value.paymentMethod === 'CHUYEN_KHOAN' ? finalTotal.value : 0)



    let resolvedPaymentMethod = 'Tiền mặt'

    const method = currentInvoice.value.paymentMethod

    if (method === 'VIETQR' || method === 'CHUYEN_KHOAN') {

      resolvedPaymentMethod = 'VietQR'

    } else if (method === 'MOMO') {

      resolvedPaymentMethod = 'MoMo'

    } else if (method === 'VNPAY') {

      resolvedPaymentMethod = 'VNPAY'

    } else if (isKetHop) {

      if (cashVal > 0 && transferVal > 0) {

        resolvedPaymentMethod = 'Kết hợp'

      } else if (transferVal > 0) {

        resolvedPaymentMethod = 'VietQR'

      } else {

        resolvedPaymentMethod = 'Tiền mặt'

      }

    }



    let noteExtra = ''

    if (resolvedPaymentMethod === 'Kết hợp') {

      noteExtra = ` [Thanh toán kết hợp: Tiền mặt ${formatMoney(cashVal)}, Chuyển khoản ${formatMoney(transferVal)}]`

    }



    const currentUser = getCurrentUser()

    const staffName = currentUser?.tenNhanVien || currentUser?.tenTaiKhoan || 'Nhân viên bán hàng'

    const staffId = currentUser?.id || null



    const payload = {

      idNhanVien: staffId,

      nguoiTao: staffName,

      maHoaDon: currentInvoice.value.code,

      loaiDon: 1, // Bất kể đơn giao hàng nhưng phát sinh tại quầy thì loại đơn vẫn là Tại quầy (1)

      idKhachHang: currentInvoice.value.customerId || null,

      idPhieuGiamGia: currentInvoice.value.voucher?.id || null,

      tenKhachHang: currentInvoice.value.customerName?.trim() || 'Khách lẻ',

      soDienThoaiKhachHang: currentInvoice.value.customerPhone?.trim() || '',

      diaChiNhanHang: currentInvoice.value.isDelivery ? currentInvoice.value.deliveryAddress?.trim() : 'Bán tại quầy',

      tongTien: totalAmount.value,

      tongTienGiamGia: voucherDiscount.value,

      phiShip: shippingFee.value,

      chapNhanGiaMoi: Boolean(currentInvoice.value.chapNhanGiaMoi),

      trangThai: currentInvoice.value.isDelivery ? 2 : 5,

      trangThaiThanhToan: 1,

      phuongThucThanhToan: resolvedPaymentMethod,

      tienMat: resolvedPaymentMethod.includes('Chuyển khoản') ? 0 : cashVal,

      tienChuyenKhoan: resolvedPaymentMethod === 'Tiền mặt' ? 0 : transferVal,

      ghiChu: (currentInvoice.value.note?.trim() || (currentInvoice.value.isDelivery ? 'Đơn giao hàng từ POS' : 'Thanh toán tại quầy')) + noteExtra,

      chiTietList: (() => {
        const aggregatedCartMap = new Map()
        for (const item of currentInvoice.value.cart) {
          const vid = Number(item.id)
          if (!vid) continue
          const q = Number(item.quantity || 1)
          if (aggregatedCartMap.has(vid)) {
            aggregatedCartMap.get(vid).soLuong += q
          } else {
            aggregatedCartMap.set(vid, {
              sanPhamId: vid,
              idChiTietSanPham: vid,
              soLuong: q,
              giaDuocTinh: item.price,
              donGia: item.price
            })
          }
        }
        return Array.from(aggregatedCartMap.values())
      })()

    }



    let res = null

    try {

      res = await api.post('/api/hoa-don/pos', payload)

    } catch (posErr) {

      console.error('Lỗi khi gọi API /api/hoa-don/pos:', posErr)
      const errMsg = posErr.response?.data?.message || posErr.response?.data?.error || posErr.message || 'Lỗi khi thanh toán đơn hàng POS'
      showToast(`Thanh toán thất bại: ${errMsg}`, 'error')
      isProcessing.value = false
      return

    }



    const savedCode = res?.data?.maHoaDon || currentInvoice.value.code

    stopPaymentPolling()

    if (currentInvoice.value.isDelivery) {

      showToast(`Đã xác nhận đơn giao hàng ${savedCode} thành công và đang in hóa đơn...`)

    } else {

      showToast(`Thanh toán hóa đơn ${savedCode} thành công!`)

    }



    // Chuyển tab hóa đơn hoặc tạo mới nếu là hóa đơn duy nhất

    const completedId = currentInvoice.value.id

    waitingInvoices.value = waitingInvoices.value.filter(inv => inv.id !== completedId)



    if (waitingInvoices.value.length === 0) {

      await addWaitingInvoice()

    } else {

      activeInvoiceId.value = waitingInvoices.value[0].id

    }



    // Cập nhật lại tồn kho sản phẩm hiển thị trên client

    fetchProductVariants()



    // Chuyển hướng tới chi tiết hóa đơn vừa tạo và tự động in hóa đơn với mẫu chuẩn FF T-SHIRT STORE

    setTimeout(() => {

      router.push({ path: `/hoa-don/${savedCode}`, query: { autoPrint: 'true' } })

    }, 400)

  } catch (error) {

    console.error('Lỗi khi thanh toán đơn hàng:', error)

    showToast('Lỗi khi thanh toán: ' + (error.response?.data?.message || error.message), 'warning')

  } finally {

    isProcessing.value = false

  }

}



// Kiểm tra tham số trả về từ Cổng VNPAY (vnpay_return theo chuẩn vnpay_jsp)

const checkVNPayReturnParams = () => {

  const query = route.query

  if (query && query.vnp_ResponseCode) {

    const responseCode = String(query.vnp_ResponseCode)

    const txnRef = String(query.vnp_TxnRef || '')

    let invCode = txnRef

    if (txnRef.includes('_')) {

      invCode = txnRef.split('_')[0]

    }

    const amt = Number(query.vnp_Amount || 0) / 100

    const transNo = query.vnp_TransactionNo || 'VNPAY'



    if (responseCode === '00') {

      showToast(`Thanh toán VNPAY thành công cho đơn ${invCode}! (+${formatMoney(amt)})`)

      api.post('/api/payment/confirm-transfer', {

        maHoaDon: invCode,

        soTien: amt,

        maGiaoDich: `VNPAY-${transNo}`,

        ghiChu: 'Thanh toán thành công qua Cổng VNPAY Sandbox',

        phuongThuc: 'Chuyển khoản'

      }).then(() => {

        const found = waitingInvoices.value.find(inv => inv.code === invCode || inv.code?.replace(/[^a-zA-Z0-9_-]/g, '') === invCode)

        if (found) {

          activeInvoiceId.value = found.id

          found.isTransferPaid = true

          handleCheckout()

        } else if (currentInvoice.value && (currentInvoice.value.code === invCode || currentInvoice.value.code?.replace(/[^a-zA-Z0-9_-]/g, '') === invCode)) {

          currentInvoice.value.isTransferPaid = true

          handleCheckout()

        }

      }).catch(err => {

        console.error('Lỗi lưu kết quả VNPAY return:', err)

      })

    } else {

      showToast(`Giao dịch VNPAY không thành công (Mã lỗi: ${responseCode})`, 'warning')

    }

    router.replace({ path: route.path, query: {} })

  }

}



// Phát hiện khi sang ngày mới để tự động hủy các hóa đơn chờ ngày trước

let recordedDateStr = new Date().toDateString()

let dayCheckTimer = null



const checkNewDayTransition = async () => {

  const todayStr = new Date().toDateString()

  if (todayStr !== recordedDateStr) {

    recordedDateStr = todayStr

    try {

      await api.post('/api/hoa-don/cancel-expired-waiting')

    } catch (e) {

      console.warn('Lỗi gọi API hủy hóa đơn chờ cũ:', e)

    }

    await loadWaitingInvoices()

    showToast('Đã sang ngày mới, hệ thống tự động hủy các hóa đơn chờ của ngày trước!', 'info')

  }

}



const onTabVisibilityChange = () => {

  if (document.visibilityState === 'visible') {

    checkNewDayTransition()

  }

}



onMounted(async () => {

  dayCheckTimer = setInterval(checkNewDayTransition, 30000)

  document.addEventListener('visibilitychange', onTabVisibilityChange)

  checkVNPayReturnParams()

  await loadWaitingInvoices()

  fetchProductVariants()

  fetchVouchers()

  loadProvincesData()

})

</script>



<template>

  <div class="pos-page-wrapper">

    <!-- TOAST THÔNG BÁO TỔNG THỂ -->

    <transition name="toast-fade">

      <div v-if="toast.show" class="toast-popup" :class="toast.type">

        <span class="toast-icon">{{ toast.type === 'warning' ? '⚠️' : '✓' }}</span>

        <span class="toast-text">{{ toast.message }}</span>

      </div>

    </transition>



    <!-- MODAL XÁC NHẬN HỦY / THAO TÁC HÓA ĐƠN -->

    <div v-if="showConfirmModal" class="pos-modal-backdrop" @click.self="showConfirmModal = false">

      <div class="pos-modal-card confirm-modal-card">

        <div class="confirm-modal-header">

          <span class="confirm-icon">⚠️</span>

          <h4>{{ confirmModalData.title }}</h4>

        </div>

        <p class="confirm-modal-desc">{{ confirmModalData.message }}</p>

        <div class="confirm-modal-actions">

          <button class="btn-modal-back" @click="showConfirmModal = false">Hủy bỏ</button>

          <button class="btn-modal-danger" @click="confirmModalData.onConfirm">Xác nhận</button>

        </div>

      </div>

    </div>



    <!-- TIÊU ĐỀ TRANG -->

    <div class="page-breadcrumb">

      <router-link to="/ban-hang" class="crumb-parent">Quản lý bán hàng</router-link>

      <span class="crumb-separator">/</span>

      <span class="crumb-current">Bán hàng tại quầy</span>

    </div>



    <!-- KHUNG CHÍNH 2 CỘT -->

    <div class="pos-grid-container">

      <!-- ==================== CỘT TRÁI (HÓA ĐƠN CHỜ + GIỎ HÀNG) ==================== -->

      <div class="pos-col-left">

        <!-- Card 1: Hóa đơn chờ -->

        <div class="pos-panel-card waiting-invoices-card">

          <div class="waiting-top-row">

            <button class="btn-create-invoice" @click="addWaitingInvoice" :disabled="isLoadingInvoices">

              + Thêm hóa đơn chờ

            </button>

            <span class="counter-text">{{ waitingInvoices.length }}/5 hóa đơn</span>

          </div>



          <!-- Danh sách tab hóa đơn chờ xếp ở dòng dưới -->

          <div class="tabs-scroll-wrapper">

            <div v-if="isLoadingInvoices" class="tab-loading-indicator">

              <span>Đang tải hóa đơn chờ từ CSDL...</span>

            </div>

            <div

                v-else

                v-for="inv in waitingInvoices"

                :key="inv.id"

                class="inv-tab-pill"

                :class="{ active: activeInvoiceId === inv.id }"

                @click="selectInvoice(inv.id)"

            >

              <span>{{ inv.code }}</span>

              <span class="inv-tab-count" v-if="inv.cart.length > 0">

                ({{ inv.cart.reduce((sum, item) => sum + item.quantity, 0) }})

              </span>

              <button

                  class="btn-close-tab"

                  @click.stop="confirmRemoveInvoice(inv)"

                  title="Hủy hóa đơn này"

              >✕</button>

            </div>

          </div>

        </div>



        <!-- Card 2: Giỏ hàng -->

        <div class="pos-panel-card cart-panel">

          <div class="cart-head-bar">

            <div class="cart-title-wrap">

              <span class="cart-title-text">Giỏ hàng</span>

              <span class="cart-count-badge">{{ totalQuantity }} sản phẩm</span>

            </div>



            <div class="cart-actions-right">

              <!-- Ô nhập/quét Barcode nhanh -->

              <div class="barcode-input-wrapper">

                <input

                    type="text"

                    v-model="barcodeInput"

                    @keyup.enter="handleBarcodeScan"

                    placeholder="Quét Barcode / Mã..."

                    class="barcode-quick-input"

                />

                <button class="btn-scan-barcode" @click="handleBarcodeScan" title="Thêm theo mã">

                  <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.2" fill="none">

                    <path d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2"></path>

                    <line x1="8" y1="12" x2="16" y2="12"></line>

                  </svg>

                </button>

              </div>



              <!-- Nút Chọn sản phẩm từ danh mục -->

              <button class="btn-open-select" @click="showProductModal = true">

                + Chọn sản phẩm

              </button>

            </div>

          </div>



          <!-- Bảng Giỏ hàng -->

          <div class="cart-table-container">

            <table class="pos-custom-table">

              <thead>

              <tr>

                <th class="text-center" style="width: 5%;">STT</th>

                <th class="text-center" style="width: 13%;">Mã SP</th>

                <th class="text-left" style="width: 25%;">Tên sản phẩm</th>

                <th class="text-center" style="width: 13%;">Phân loại</th>

                <th class="text-center" style="width: 16%;">Số lượng</th>

                <th class="text-center" style="width: 14%;">Đơn giá</th>

                <th class="text-center" style="width: 14%;">Thao tác</th>

              </tr>

              </thead>

              <tbody>

              <tr v-if="currentInvoice.cart.length === 0">

                <td colspan="7" class="empty-cart-message">

                  Giỏ hàng trống. Vui lòng bấm <b>"+ Chọn sản phẩm"</b> hoặc nhập mã để thêm hàng.

                </td>

              </tr>



              <tr v-else v-for="(item, idx) in currentInvoice.cart" :key="item.id">

                <td class="text-center font-bold text-muted">{{ idx + 1 }}</td>

                <td class="text-center">

                  <span class="cart-code-badge">{{ item.variantCode || item.code }}</span>

                </td>

                <td class="text-left font-600">

                  <div class="cart-product-name-wrap">

                    <span class="cart-product-title" :title="item.name || getCartItemName(item)">{{ item.name || getCartItemName(item) }}</span>

                    <span v-if="item.hasDiscount && (item.discountText || item.discountCampaignName)" class="cart-discount-badge" :title="item.discountCampaignName">

                      {{ item.discountText || item.discountCampaignName }}

                    </span>

                  </div>

                </td>

                <td class="text-center">

                  <span class="cart-variant-pill">

                    <span v-if="item.colorHex" class="cart-color-dot" :style="{ backgroundColor: item.colorHex }"></span>

                    <span class="variant-color-text">{{ item.color }}</span>

                    <span class="variant-slash">/</span>

                    <span class="variant-size-text">{{ item.size }}</span>

                  </span>

                </td>

                <td class="text-center">

                  <div class="pos-qty-box">

                    <button

                        type="button"

                        class="qty-btn"

                        @click="updateQuantity(item, -1)"

                        title="Giảm số lượng"

                    >-</button>

                    <input

                        type="number"

                        :value="item.quantity"

                        @input="handleQuantityInput(item, $event)"

                        @change="handleQuantityInput(item, $event)"

                        @blur="handleQuantityInput(item, $event)"

                        class="qty-input"

                        min="1"

                        :max="getMaxStockForItem(item)"

                    />

                    <button

                        type="button"

                        class="qty-btn"

                        :disabled="getRemainingStock(item) <= 0"

                        :title="getRemainingStock(item) <= 0 ? 'Đã đạt số lượng tồn kho tối đa' : 'Tăng số lượng'"

                        @click="updateQuantity(item, 1)"

                    >+</button>

                  </div>

                </td>

                <td class="text-center font-price">

                  <div v-if="item.hasDiscount && item.originalPrice > item.price" class="cart-price-col">

                    <span class="cart-price-sale">{{ formatMoney(item.price) }}</span>

                    <span class="cart-price-old">{{ formatMoney(item.originalPrice) }}</span>

                  </div>

                  <span v-else class="cart-price-normal">{{ formatMoney(item.price) }}</span>

                </td>

                <td class="text-center">

                  <button class="btn-trash-row" @click="removeFromCart(item)" title="Xóa khỏi giỏ hàng">

                    <svg viewBox="0 0 24 24" width="15" height="15" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" class="trash-icon-svg">

                      <polyline points="3 6 5 6 21 6"></polyline>

                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>

                      <line x1="10" y1="11" x2="10" y2="17"></line>

                      <line x1="14" y1="11" x2="14" y2="17"></line>

                    </svg>

                  </button>

                </td>

              </tr>

              </tbody>

            </table>

          </div>

        </div>



        <!-- Card 3: Thông tin đơn hàng & Giao hàng (Bố trí dưới phần Giỏ hàng / Chọn sản phẩm) -->

        <div class="pos-panel-card delivery-info-card">

          <!-- Hàng tiêu đề, Sổ địa chỉ & Switch giao hàng -->

          <div class="delivery-card-header">

            <div class="delivery-title-badge-wrap">

              <span class="delivery-card-title">Thông tin giao hàng</span>

              <span v-if="currentInvoice.isDelivery" class="delivery-badge-on">Đang bật giao hàng</span>

              <span v-else class="delivery-badge-off">Bán tại quầy</span>

            </div>

            <div class="delivery-header-actions">

              <button

                  v-if="currentInvoice.customerId && currentInvoice.isDelivery"

                  type="button"

                  class="btn-address-book"

                  @click="openSelectAddressModal"

                  title="Xem và chọn địa chỉ khác từ sổ địa chỉ của khách hàng"

              >

                Sổ địa chỉ

              </button>

              <label class="delivery-switch-wrap">

                <span class="delivery-switch-label">Giao hàng</span>

                <input type="checkbox" v-model="currentInvoice.isDelivery" />

                <span class="switch-ui"></span>

              </label>

            </div>

          </div>



          <!-- Nhập địa chỉ & phí ship khi bật Giao hàng -->

          <div v-if="currentInvoice.isDelivery" class="delivery-box-expand">

            <!-- Thông tin người nhận khách lẻ: sắp xếp dạng lưới 2 cột gọn gàng -->

            <div v-if="!currentInvoice.customerId" class="pos-form-grid-2">

              <div class="pos-form-group">

                <label class="group-label-gray">Tên người nhận <span class="text-danger">*</span></label>

                <input

                    type="text"

                    v-model="currentInvoice.customerName"

                    placeholder="Họ tên người nhận..."

                    class="pos-form-input"

                />

              </div>

              <div class="pos-form-group">

                <label class="group-label-gray">Số điện thoại <span class="text-danger">*</span></label>

                <input

                    type="text"

                    v-model="currentInvoice.customerPhone"

                    placeholder="VD: 0987654321..."

                    class="pos-form-input"

                />

              </div>

            </div>



            <!-- Cấu trúc địa chỉ 2 cấp: Tỉnh/Thành phố & Phường/Xã -->

            <div class="pos-form-grid-2">

              <div class="pos-form-group">

                <label class="group-label-gray" style="font-size: 0.78rem;">Tỉnh / Thành phố <span class="text-danger">*</span></label>

                <Select2Combobox

                    v-model="currentInvoice.deliveryProvinceCode"

                    :options="provincesList"

                    value-key="code"

                    label-key="name"

                    placeholder="-- Tỉnh / Thành phố --"

                    search-placeholder="Tìm Tỉnh / TP..."

                    @change="onDeliveryProvinceChange"

                />

              </div>



              <div class="pos-form-group">

                <label class="group-label-gray" style="font-size: 0.78rem;">Phường / Xã <span class="text-danger">*</span></label>

                <Select2Combobox

                    v-model="currentInvoice.deliveryWardCode"

                    :options="deliveryWardsList"

                    value-key="code"

                    label-key="name"

                    :disabled="!currentInvoice.deliveryProvinceCode"

                    placeholder="-- Phường / Xã --"

                    search-placeholder="Tìm Phường / Xã..."

                    @change="onDeliveryWardChange"

                />

              </div>

            </div>



            <div class="pos-form-grid-2" style="margin-top: 6px;">

              <div class="pos-form-group">

                <label class="group-label-gray" style="font-size: 0.78rem;">Số nhà, tên đường (Địa chỉ cụ thể) <span class="text-danger">*</span></label>

                <input

                    type="text"

                    v-model="currentInvoice.deliveryDetail"

                    @input="onDeliveryDetailInput"

                    placeholder="Số nhà, ngõ, tên đường..."

                    class="pos-form-input"

                />

              </div>



              <!-- Phí vận chuyển: Tích hợp tự động API GHN Express (Gọn gàng, tinh tế) -->
              <div class="pos-form-group">
                <div class="shipping-fee-header">
                  <label class="group-label-gray">Phí vận chuyển (VNĐ)</label>
                  <span class="ghn-api-badge cursor-pointer" title="Bấm để tính lại cước qua API Giao Hàng Nhanh" @click="triggerRecalculateShippingFee">
                    API GHN Express
                  </span>
                </div>

                <input
                    type="text"
                    min="0"
                    step="1000"
                    :value="displayShippingFee"
                    @input="onShippingFeeFormattedInput"
                    @focus="$event.target.select()"
                    placeholder="Phí giao hàng (GHN)..."
                    class="pos-form-input input-money"
                />
              </div>

            </div>

            <!-- Xem trước địa chỉ giao hàng đầy đủ -->

            <div class="pos-form-group" v-if="currentInvoice.deliveryAddress" style="margin-top: 8px;">

              <div class="cust-address-preview" style="margin-top: 0; background: #ffffff; border-style: solid;">

                

                <span class="addr-preview-text"><strong>Địa chỉ giao:</strong> {{ currentInvoice.deliveryAddress }}</span>

              </div>

            </div>

          </div>

        </div>

      </div>



      <!-- ==================== CỘT PHẢI (KHÁCH HÀNG & THANH TOÁN) ==================== -->

      <div class="pos-col-right">

        <!-- 1. Card Khách hàng -->

        <div class="pos-panel-card right-card">

          <div class="cust-card-header">

            <h4 class="card-bold-title" style="margin: 0;">Khách hàng</h4>

            <span v-if="!currentInvoice.customerId && !currentInvoice.customerName" class="badge-guest-pill">Mặc định: Khách lẻ</span>

          </div>



          <!-- Đã chọn khách hàng cụ thể -->

          <div v-if="currentInvoice.customerId || currentInvoice.customerName" class="selected-customer-pill-wrap" style="margin-top: 10px;">

            <div class="selected-customer-pill">

              <div class="cust-info">

                <span class="cust-name">{{ currentInvoice.customerName }}</span>

                <span class="cust-phone" v-if="currentInvoice.customerPhone">({{ currentInvoice.customerPhone }})</span>

              </div>

              <div class="cust-actions-group">

                <button

                    v-if="currentInvoice.customerId"

                    type="button"

                    class="btn-address-book"

                    @click="openSelectAddressModal"

                    title="Xem và chọn địa chỉ khác của khách hàng"

                >

                  Sổ địa chỉ

                </button>

                <button class="btn-clear-cust" @click="clearSelectedCustomer" title="Đổi sang Khách lẻ">Bỏ chọn</button>

              </div>

            </div>

            <div v-if="currentInvoice.deliveryAddress" class="cust-address-preview">

              <span class="addr-preview-text">{{ currentInvoice.deliveryAddress }}</span>

            </div>

          </div>



          <!-- Tìm kiếm khách hàng -->

          <div v-else class="customer-search-relative" style="margin-top: 10px;">

            <div class="customer-search-row">

              <input

                  type="text"

                  v-model="currentInvoice.customerPhone"

                  @input="onCustomerInput"

                  @focus="onCustomerInput"

                  placeholder="Nhập tên hoặc SĐT (để trống: Khách lẻ)..."

                  class="pos-form-input"

              />

              <button class="btn-add-customer-quick" @click="openQuickAddModal" title="Thêm mới khách hàng">Thêm mới</button>

            </div>



            <!-- Autocomplete danh sách khách hàng tìm thấy -->

            <div v-if="showCustomerDropdown" class="customer-dropdown-menu">

              <template v-if="customerSearchResults.length > 0">

                <div

                    v-for="c in customerSearchResults"

                    :key="c.id"

                    class="customer-dropdown-item"

                    @click="selectCustomer(c)"

                >

                  <div class="c-name">{{ c.tenKhachHang }}</div>

                  <div class="c-meta">{{ c.soDienThoai }} <span v-if="c.maKhachHang">({{ c.maKhachHang }})</span></div>

                </div>

              </template>

              <div v-else class="customer-dropdown-empty">

                <span>Không tìm thấy khách hàng trong CSDL.</span>

                <button type="button" class="btn-quick-create-cust" @click="openQuickAddModal">Thêm mới</button>

              </div>

            </div>

          </div>

        </div>



        <!-- 2. Card Thanh toán -->

        <div class="pos-panel-card right-card checkout-panel">

          <div class="checkout-head-bar">

            <span class="order-title-brand">Thanh toán</span>

          </div>



          <!-- Phiếu giảm giá: Tự động áp dụng mã tốt nhất -->

          <div class="pos-form-group voucher-auto-wrapper">

            <div class="voucher-label-row">

              <label class="group-label-gray">Phiếu giảm giá (Voucher)</label>

              

            </div>



            <!-- Khi có phiếu giảm giá được tự động áp dụng -->

            <div v-if="currentInvoice.voucher" class="auto-voucher-card">

              <div class="auto-v-left">

                

                <div class="auto-v-details">

                  <div class="auto-v-title-row">

                    <span class="auto-v-badge" :class="isVoucherPersonal(currentInvoice.voucher) ? 'badge-personal' : 'badge-public'">

                      {{ isVoucherPersonal(currentInvoice.voucher) ? 'Cá nhân' : 'Công khai' }}

                    </span>

                    <strong class="auto-v-code">{{ currentInvoice.voucher.maPhieuGiamGia }}</strong>

                    <span v-if="formatVoucherDisplayName(currentInvoice.voucher)" class="auto-v-name" :title="formatVoucherDisplayName(currentInvoice.voucher)">

                      - {{ formatVoucherDisplayName(currentInvoice.voucher) }}

                    </span>

                  </div>

                  <div class="auto-v-condition">

                    {{ formatVoucherCondition(currentInvoice.voucher) }}

                  </div>

                </div>

              </div>

              <div class="auto-v-right">

                <div class="auto-v-discount-text">-{{ formatMoney(voucherDiscount) }}</div>

                <div class="auto-v-rate-sub">{{ formatVoucherDiscountLabel(currentInvoice.voucher) }}</div>

              </div>

            </div>



            <!-- Khi không có phiếu nào phù hợp -->

            <div v-else class="auto-voucher-none-card">

              

              <span v-if="totalAmount === 0" class="auto-v-none-text">Chưa có sản phẩm trong giỏ hàng</span>

              <span v-else class="auto-v-none-text">Không có phiếu giảm giá nào phù hợp với đơn hàng</span>

            </div>



            <!-- Gợi ý mua thêm để hưởng ưu đãi voucher tốt hơn -->

            <div v-if="betterVoucherSuggestion" class="voucher-suggestion-banner">

              <div class="v-sugg-content">

                

                <span class="v-sugg-text">

                  Mua thêm <strong>{{ formatMoney(betterVoucherSuggestion.neededAmount) }}</strong> để được <strong>giảm {{ formatMoney(betterVoucherSuggestion.potentialDiscount) }}</strong> (mã <strong>{{ betterVoucherSuggestion.code }}</strong> - {{ betterVoucherSuggestion.discountLabel }})!

                </span>

              </div>

            </div>

          </div>



          <!-- Dòng tóm tắt tiền tệ -->

          <div class="pos-summary-line">

            <span class="line-label">Tổng tiền hàng ({{ totalQuantity }} SP):</span>

            <span class="line-value font-bold-dark">{{ formatMoney(totalAmount) }}</span>

          </div>



          <div v-if="voucherDiscount > 0" class="pos-summary-line discount-line">

            <span class="line-label">Giảm giá voucher:</span>

            <span class="line-value text-discount-bold">- {{ formatMoney(voucherDiscount) }}</span>

          </div>



          <div v-if="currentInvoice.isDelivery" class="pos-summary-line">

            <span class="line-label">Phí vận chuyển:</span>

            <span class="line-value">+ {{ formatMoney(shippingFee) }}</span>

          </div>



          <div class="pos-summary-line total-highlight-line">

            <span class="line-label total-label">Khách cần trả:</span>

            <span class="line-value total-value">{{ formatMoney(finalTotal) }}</span>

          </div>



          <!-- Hình thức thanh toán -->

          <div class="pos-form-group">

            <label class="group-label-gray">Hình thức thanh toán</label>

            <div class="payment-radio-group">
              <label class="radio-item" @click="selectPaymentMethod('TIEN_MAT')">
                <input type="radio" value="TIEN_MAT" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Tiền mặt</span>
              </label>
              <label class="radio-item" @click="selectPaymentMethod('CHUYEN_KHOAN')">
                <input type="radio" value="CHUYEN_KHOAN" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Chuyển khoản</span>
              </label>
              <label class="radio-item" @click="selectPaymentMethod('KET_HOP')">
                <input type="radio" value="KET_HOP" v-model="currentInvoice.paymentMethod" />
                <span class="radio-text">Kết hợp</span>
              </label>
            </div>
          </div>

          <!-- Khách thanh toán -->
          <!-- ==================== KHU VỰC NHẬP THANH TOÁN ==================== -->
          <!-- 1. Thanh toán KẾT HỢP: 2 phần Tiền mặt & Chuyển khoản tối giản -->
          <template v-if="currentInvoice.paymentMethod === 'KET_HOP'">
            <div class="pos-form-group">
              <label class="group-label-gray">Tiền mặt</label>
              <input
                  type="text"
                  min="0"
                  step="1000"
                  :value="displayCashAmount"
                  @input="onCashAmountFormattedInput"
                  @focus="$event.target.select()"
                  placeholder="Nhập số tiền mặt..."
                  class="pos-form-input input-money"
              />
            </div>

            <div class="pos-form-group">
              <label class="group-label-gray">Chuyển khoản</label>
              <input
                  type="text"
                  min="0"
                  step="1000"
                  :value="displayTransferAmount"
                  @input="onTransferAmountFormattedInput"
                  @focus="$event.target.select()"
                  placeholder="Nhập số tiền chuyển khoản..."
                  class="pos-form-input input-money"
              />

              <!-- Thanh trạng thái gọn nhẹ khi có số tiền chuyển khoản, không dùng box cồng kềnh -->
              <div v-if="currentInvoice.transferAmount > 0" class="chuyen-khoan-status-bar">
                <div class="ck-status-info">
                  <span v-if="currentInvoice.isTransferPaid" class="ck-paid-badge">
                    Đã nhận {{ formatMoney(currentInvoice.transferAmount) }}
                  </span>
                  <span v-else class="ck-waiting-badge">
                    Khách chuyển: <strong class="text-red">{{ formatMoney(currentInvoice.transferAmount) }}</strong>
                  </span>
                </div>
                <button
                    type="button"
                    class="btn-open-vietqr-again"
                    @click="openVietQrModal"
                    title="Mở mã QR phóng to"
                >
                  Mở mã VietQR
                </button>
              </div>
            </div>
          </template>

          <!-- 2. Thanh toán CHUYỂN KHOẢN TOÀN BỘ: Đã bỏ hộp cồng kềnh, ấn chuyển khoản là hiện popup trực tiếp -->
          <div v-else-if="['VNPAY', 'CHUYEN_KHOAN'].includes(currentInvoice.paymentMethod)" class="pos-form-group">
            <div class="pay-input-header cursor-pointer" @click="openVietQrModal" title="Bấm để mở lại mã VietQR">
              <label class="group-label-gray cursor-pointer">Số tiền chuyển khoản</label>
              <span class="font-bold-dark text-red font-mono-amount">{{ formatMoney(finalTotal) }}</span>
            </div>
            <div v-if="currentInvoice.isTransferPaid" class="ck-paid-simple-note">
              ✓ Đã xác nhận nhận đủ tiền chuyển khoản: <strong>{{ formatMoney(finalTotal) }}</strong>
            </div>
          </div>

          <!-- 3. Thanh toán TIỀN MẶT TOÀN BỘ -->

          <div v-else class="pos-form-group">

            <div class="pay-input-header">

              <label class="group-label-gray">Khách thanh toán</label>

              <button

                  type="button"

                  class="btn-exact-pay"

                  @click="setExactPayment"

              >

                Đủ tiền ({{ formatMoney(finalTotal) }})

              </button>

            </div>



            <input

                type="text"

                :value="displayCustomerPay"

                @input="onCustomerPayFormattedInput"

                @focus="$event.target.select()"

                placeholder="Nhập số tiền khách đưa..."

                class="pos-form-input input-money"

            />

          </div>



          <!-- Tiền thừa trả khách -->

          <div class="pos-summary-line">

            <span class="line-label">Tiền thừa trả khách:</span>

            <div class="change-val-wrap">

              <span class="line-value text-change-bold">{{ formatMoney(changeAmount) }}</span>

              <span

                  v-if="missingAmount > 0"

                  class="text-missing-warning"

              >

                (Còn thiếu {{ formatMoney(missingAmount) }})

              </span>

            </div>

          </div>



          <!-- Ghi chú thanh toán -->

          <div class="pos-form-group">

            <label class="group-label-gray">{{ currentInvoice.isDelivery ? 'Ghi chú đơn giao hàng' : 'Ghi chú hóa đơn' }}</label>

            <textarea

                rows="2"

                v-model="currentInvoice.note"

                placeholder="Ghi chú thêm nếu cần..."

                class="pos-form-textarea"

            ></textarea>

          </div>



          <!-- 2 Nút cuối: Hủy hóa đơn & Thanh toán -->

          <div class="checkout-actions-bottom">

            <button class="btn-cancel-bill" @click="removeCurrentInvoice">

              Hủy hóa đơn

            </button>

            <button

                class="btn-submit-checkout"

                :disabled="isProcessing || currentInvoice.cart.length === 0"

                @click="openOrderConfirmModal"

            >

              {{ isProcessing ? 'Đang xử lý...' : (currentInvoice.isDelivery ? 'Xác nhận đặt hàng' : 'Thanh toán') }}

            </button>

          </div>

        </div>

      </div>

    </div>



    <!-- ==================== POPUP CHỌN SẢN PHẨM (BẢNG & BỘ LỌC ĐẦY ĐỦ) ==================== -->

    <div v-if="showProductModal" class="pos-modal-backdrop" @click.self="showProductModal = false">

      <div class="pos-modal-card product-table-modal">

        <!-- 1. Header Popup -->

        <div class="modal-top">

          <div class="modal-title-group">

            <h3 class="modal-title-text">Chọn sản phẩm biến thể áo phông</h3>

            <span class="modal-badge-count">Tìm thấy {{ filteredProducts.length }} biến thể</span>

          </div>

          <button class="btn-modal-close" @click="showProductModal = false" title="Đóng cửa sổ">✕</button>

        </div>



        <!-- 2. Khung bộ lọc đầy đủ (Filter Bar) -->

        <div class="product-modal-filter-bar">

          <!-- Hàng 1: Tìm kiếm & Sắp xếp & Nút Đặt lại -->

          <div class="filter-bar-row filter-bar-main">

            <div class="filter-input-search-wrap">

              <input

                type="text"

                v-model="searchProductText"

                placeholder="Tìm theo tên áo phông, mã CTSP, mã SP, màu sắc, size..."

                class="filter-search-input"

              />

              <button

                v-if="searchProductText"

                type="button"

                class="btn-clear-input"

                @click="searchProductText = ''"

                title="Xóa tìm kiếm"

              >

                ✕

              </button>

            </div>



            <div class="filter-group-inline sort-inline">

              <label class="filter-inline-label">Sắp xếp:</label>

              <div class="sort-select2-wrap">

                <Select2Combobox

                  v-model="filterSort"

                  :options="sortFilterOptions"

                  value-key="id"

                  label-key="name"

                  placeholder="Mặc định (Mã CTSP)"

                  search-placeholder="Tìm kiếm..."

                  :searchable="false"

                  allow-clear

                />

              </div>

            </div>



            <button

              type="button"

              class="btn-reset-filters"

              @click="resetProductModalFilters"

              title="Đặt lại toàn bộ bộ lọc"

            >

              Đặt lại bộ lọc

            </button>

          </div>



          <!-- Hàng 2: Bộ lọc phân loại 3 cột (Màu sắc, Kích cỡ, Ưu đãi giảm giá) -->

          <div class="filter-bar-row filter-bar-facets">

            <div class="filter-facet-item">

              <label class="facet-label">Màu sắc</label>

              <Select2Combobox

                v-model="filterColor"

                :options="colorFilterOptions"

                value-key="id"

                label-key="name"

                placeholder="Tất cả màu sắc"

                search-placeholder="Tìm kiếm màu sắc..."

                allow-clear

              />

            </div>



            <div class="filter-facet-item">

              <label class="facet-label">Kích cỡ</label>

              <Select2Combobox

                v-model="filterSize"

                :options="sizeFilterOptions"

                value-key="id"

                label-key="name"

                placeholder="Tất cả kích cỡ"

                search-placeholder="Tìm kiếm kích cỡ..."

                allow-clear

              />

            </div>



            <div class="filter-facet-item">

              <label class="facet-label">Chương trình ưu đãi</label>

              <Select2Combobox

                v-model="filterDiscount"

                :options="discountFilterOptions"

                value-key="id"

                label-key="name"

                placeholder="Tất cả ưu đãi"

                search-placeholder="Tìm kiếm ưu đãi..."

                :searchable="false"

                allow-clear

              />

            </div>

          </div>

        </div>



        <!-- 3. Khu vực Bảng Sản phẩm (Table) -->

        <div class="product-modal-table-container">

          <div v-if="isLoadingProducts" class="modal-loading-box">

            <div class="spinner-dot"></div>

            <span>Đang tải danh sách biến thể sản phẩm...</span>

          </div>



          <div v-else-if="filteredProducts.length === 0" class="empty-product-table-box">

            <div class="empty-title">Không tìm thấy sản phẩm biến thể phù hợp</div>

            <p class="empty-desc">Vui lòng điều chỉnh lại từ khóa hoặc xóa các bộ lọc để xem toàn bộ sản phẩm.</p>

            <button type="button" class="btn-clear-empty-filter" @click="resetProductModalFilters">

              Xóa bộ lọc

            </button>

          </div>



          <div v-else class="table-scroll-wrap">

            <table class="variant-data-table">

              <thead>

                <tr>

                  <th style="width: 44px; text-align: center;">STT</th>

                  <th style="width: 58px; text-align: center;">ẢNH</th>

                  <th style="width: 115px;">MÃ BIẾN THỂ</th>

                  <th style="min-width: 220px;">TÊN SẢN PHẨM</th>

                  <th style="width: 110px; text-align: center;">MÀU SẮC</th>

                  <th style="width: 80px; text-align: center;">KÍCH CỠ</th>

                  <th style="width: 100px; text-align: center;">SỐ LƯỢNG TỒN</th>

                  <th style="width: 135px; text-align: right;">ĐƠN GIÁ</th>

                  <th style="width: 105px; text-align: center;">THAO TÁC</th>

                </tr>

              </thead>

              <tbody>

                <tr

                  v-for="(p, idx) in paginatedProductsModal"

                  :key="p.id"

                  class="variant-tr"

                  :class="{ 'tr-has-discount': p.hasDiscount }"

                  @click="selectProduct(p)"

                >

                  <!-- STT -->

                  <td style="text-align: center;" class="td-stt">

                    {{ (productModalPage - 1) * productModalPageSize + idx + 1 }}

                  </td>



                  <!-- Ảnh -->

                  <td style="text-align: center;" class="td-img">

                    <div class="table-thumb-wrap">

                      <img

                        :src="formatProductImage(p.image, p)"

                        :alt="p.name"

                        class="table-thumb-img"

                        loading="lazy"

                        @error="(e) => e.target.src = defaultProductPlaceholder"

                      />

                      <span v-if="p.hasDiscount" class="thumb-badge-sale">{{ p.discountText }}</span>

                    </div>

                  </td>



                  <!-- Mã CTSP -->

                  <td class="td-code">

                    <strong class="code-primary">{{ p.variantCode || p.code }}</strong>

                    <span v-if="p.code && p.code !== p.variantCode" class="code-sub">{{ p.code }}</span>

                  </td>



                  <!-- Tên sản phẩm -->

                  <td class="td-name">

                    <div class="name-main">{{ p.name }}</div>

                    <div v-if="p.hasDiscount && p.discountCampaignName" class="campaign-pill" :title="p.discountCampaignName">

                      {{ p.discountCampaignName }}

                    </div>

                  </td>



                  <!-- Màu sắc -->

                  <td style="text-align: center;" class="td-color">

                    <div class="color-wrap">

                      <span

                        v-if="p.colorHex"

                        class="color-dot"

                        :style="{ backgroundColor: p.colorHex }"

                      ></span>

                      <span class="color-text">{{ p.color }}</span>

                    </div>

                  </td>



                  <!-- Kích cỡ -->

                  <td style="text-align: center;" class="td-size">

                    <span class="size-badge">{{ p.size }}</span>

                  </td>



                  <!-- Số lượng tồn kho -->

                  <td style="text-align: center;" class="td-stock">

                    <span

                      :class="['stock-badge', (p.stock || 0) > 0 ? 'in-stock' : 'out-of-stock']"

                      :title="'Số lượng còn trong kho: ' + (p.stock ?? 0)"

                    >

                      {{ p.stock ?? 0 }}

                    </span>

                  </td>



                  <!-- Đơn giá -->

                  <td style="text-align: right;" class="td-price">

                    <template v-if="p.hasDiscount && p.originalPrice > p.price">

                      <div class="price-discounted">{{ formatMoney(p.price) }}</div>

                      <div class="price-strikethrough">{{ formatMoney(p.originalPrice) }}</div>

                    </template>

                    <template v-else>

                      <div class="price-regular">{{ formatMoney(p.price) }}</div>

                    </template>

                  </td>



                  <!-- Thao tác -->

                  <td style="text-align: center;" class="td-action" @click.stop>

                    <button

                      type="button"

                      class="btn-action-add"

                      @click="selectProduct(p)"

                      :title="'Thêm vào giỏ hàng (Còn ' + p.stock + ' sp)'"

                    >

                      + Thêm

                    </button>

                  </td>

                </tr>

              </tbody>

            </table>

          </div>

        </div>



        <!-- 4. Footer phân trang đồng bộ chuẩn như các màn DanhSachHoaDon / ThongKe -->

        <div class="product-modal-footer">

          <div v-if="filteredProducts.length > 0" class="pagination-section">

            <div class="pagination-info">

              Hiển thị {{ filteredProducts.length ? ((productModalPage - 1) * productModalPageSize + 1) : 0 }} - {{ Math.min(productModalPage * productModalPageSize, filteredProducts.length) }} / {{ filteredProducts.length }} sản phẩm

            </div>



            <div class="pagination-buttons">

              <button

                type="button"

                class="btn-pager"

                :disabled="productModalPage <= 1"

                @click="changeProductModalPage(productModalPage - 1)"

                title="Trang trước"

              >

                ‹

              </button>



              <template v-for="(pg, idx) in visibleProductModalPages" :key="idx">

                <span v-if="pg === '...'" class="pg-dots">...</span>

                <button

                  v-else

                  type="button"

                  class="btn-pager"

                  :class="{ active: productModalPage === pg }"

                  @click="changeProductModalPage(pg)"

                >

                  {{ pg }}

                </button>

              </template>



              <button

                type="button"

                class="btn-pager"

                :disabled="productModalPage >= productModalTotalPages"

                @click="changeProductModalPage(productModalPage + 1)"

                title="Trang sau"

              >

                ›

              </button>

            </div>

          </div>



          <div class="modal-footer-right-actions">

            <button type="button" class="btn-pager-close" @click="showProductModal = false">

              Đóng

            </button>

          </div>

        </div>

      </div>

    </div>



    <!-- ==================== POPUP THÊM NHANH KHÁCH HÀNG ==================== -->

    

    <!-- ==================== MODAL XÁC NHẬN ĐƠN HÀNG TRƯỚC KHI HOÀN TẤT ==================== -->

    <div v-if="showOrderConfirmModal" class="pos-modal-backdrop" @click.self="showOrderConfirmModal = false">

      <div class="pos-modal-card confirm-order-modal">

        <div class="modal-top">

          <div class="modal-title-group">

            <h3 class="modal-title-text">{{ currentInvoice.isDelivery ? 'Xác nhận thông tin đơn giao hàng' : 'Xác nhận thanh toán hóa đơn POS' }}</h3>

            <span class="modal-badge-count">Mã: {{ currentInvoice.code }}</span>

          </div>

          <button class="btn-modal-close" @click="showOrderConfirmModal = false">✕</button>

        </div>



        <div class="confirm-modal-body">

          <!-- Thông tin khách & nhận hàng -->

          <div class="confirm-info-grid">

            <div class="confirm-info-col">

              <span class="c-lbl">Khách hàng:</span>

              <strong class="c-val">{{ currentInvoice.customerName || 'Khách lẻ' }}</strong>

            </div>

            <div class="confirm-info-col" v-if="currentInvoice.customerPhone">

              <span class="c-lbl">Số điện thoại:</span>

              <strong class="c-val">{{ currentInvoice.customerPhone }}</strong>

            </div>

            <div class="confirm-info-col">

              <span class="c-lbl">Hình thức nhận:</span>

              <span class="c-val badge-delivery-tag">{{ currentInvoice.isDelivery ? 'Giao hàng tận nơi' : 'Nhận tại quầy' }}</span>

            </div>

            <div class="confirm-info-col" v-if="currentInvoice.isDelivery">

              <span class="c-lbl">Địa chỉ giao:</span>

              <span class="c-val text-truncate-2">{{ currentInvoice.deliveryAddress }}</span>

            </div>

            <div class="confirm-info-col">

              <span class="c-lbl">Thanh toán:</span>

              <strong class="c-val text-primary-gold">{{ currentInvoice.paymentMethod === 'TIEN_MAT' ? 'Tiền mặt' : (currentInvoice.paymentMethod === 'KET_HOP' ? 'Kết hợp (Tiền mặt + VNPay)' : 'Chuyển khoản (VNPay)') }}</strong>

            </div>

          </div>



          <!-- Danh sách sản phẩm tóm tắt -->

          <div class="confirm-items-table-wrap">

            <table class="confirm-table">

              <thead>

                <tr>

                  <th>Sản phẩm</th>

                  <th class="text-center">Số lượng</th>

                  <th class="text-right">Đơn giá</th>

                  <th class="text-right">Thành tiền</th>

                </tr>

              </thead>

              <tbody>

                <tr v-for="item in currentInvoice.cart" :key="item.id">

                  <td class="col-prod-name">

                    <div class="confirm-prod-cell">

                      <img :src="formatProductImage(item.image)" class="confirm-prod-thumb" />

                      <div>

                        <div class="confirm-prod-title">{{ item.name }}</div>

                        <div class="confirm-prod-sub">{{ item.color }} / Size {{ item.size }}</div>

                      </div>

                    </div>

                  </td>

                  <td class="text-center font-bold">{{ item.quantity }}</td>

                  <td class="text-right">{{ formatMoney(item.price) }}</td>

                  <td class="text-right font-bold">{{ formatMoney(item.price * item.quantity) }}</td>

                </tr>

              </tbody>

            </table>

          </div>



          <!-- Tóm tắt chi phí -->

          <div class="confirm-cost-breakdown">

            <div class="cost-row">

              <span>Tạm tính ({{ totalQuantity }} sản phẩm):</span>

              <strong>{{ formatMoney(totalAmount) }}</strong>

            </div>

            <div v-if="voucherDiscount > 0" class="cost-row discount-row">

              <span>Voucher ({{ currentInvoice.voucher?.maPhieuGiamGia }}):</span>

              <strong class="text-discount">- {{ formatMoney(voucherDiscount) }}</strong>

            </div>

            <div v-if="currentInvoice.isDelivery" class="cost-row">

              <span>Phí vận chuyển (GHN):</span>

              <strong>+ {{ formatMoney(shippingFee) }}</strong>

            </div>

            <div class="cost-row total-highlight">

              <span>Tổng thanh toán:</span>

              <strong class="total-big-val">{{ formatMoney(finalTotal) }}</strong>

            </div>

          </div>

        </div>



        <div class="modal-actions-bottom">

          <button type="button" class="btn-cancel-modal" @click="showOrderConfirmModal = false">

            Quay lại kiểm tra

          </button>

          <button

              type="button"

              class="btn-submit-modal"

              :disabled="isProcessing"

              @click="confirmOrderAndCheckout"

          >

            {{ isProcessing ? 'Đang hoàn tất...' : (currentInvoice.isDelivery ? 'Xác nhận & Tạo đơn giao' : 'Thanh toán & In hóa đơn') }}

          </button>

        </div>

      </div>

    </div>



    <!-- Modal Cảnh báo Đổi giá sản phẩm & Khuyến mãi (Case 1 & 4) -->

    <div v-if="showPriceChangeModal" class="pos-modal-backdrop" @click.self="showPriceChangeModal = false">

      <div class="pos-modal-card confirm-order-modal price-change-modal">

        <div class="modal-top">

          <div class="modal-title-group">

            <h3 class="modal-title-text" style="color: #b45309;">Cảnh báo: Đơn giá hoặc Khuyến mãi đã thay đổi</h3>

            <span class="modal-badge-count">Mã: {{ currentInvoice.code }}</span>

          </div>

          <button class="btn-modal-close" @click="showPriceChangeModal = false">✕</button>

        </div>



        <div class="confirm-modal-body">

          <div class="price-change-warning-box">

            Giá bán niêm yết hoặc đợt giảm giá của sản phẩm đã thay đổi kể từ lúc thêm vào giỏ hàng.

            Vui lòng kiểm tra lại bảng chênh lệch giá dưới đây trước khi thanh toán.

          </div>



          <div class="confirm-items-table-wrap">

            <table class="confirm-table">

              <thead>

                <tr>

                  <th>Sản phẩm</th>

                  <th class="text-center">Số lượng</th>

                  <th class="text-right">Đơn giá cũ</th>

                  <th class="text-right">Đơn giá mới</th>

                  <th class="text-right">Chênh lệch</th>

                </tr>

              </thead>

              <tbody>

                <tr v-for="c in priceChangeData.items" :key="c.idChiTietSanPham">

                  <td class="col-prod-name"><strong>{{ c.tenSanPham }}</strong></td>

                  <td class="text-center font-bold">{{ c.soLuong }}</td>

                  <td class="text-right" style="color: #64748b; text-decoration: line-through;">{{ formatMoney(c.giaCu != null ? c.giaCu : c.giaTrongGio) }}</td>

                  <td class="text-right font-bold" style="color: #b45309;">{{ formatMoney(c.giaMoi != null ? c.giaMoi : c.giaHienTai) }}</td>

                  <td class="text-right font-bold" :style="{ color: c.chenhLech > 0 ? '#b91c1c' : '#15803d' }">

                    {{ (c.chenhLech != null ? c.chenhLech : c.chenhLechDonGia) > 0 ? '+' : '' }}{{ formatMoney(c.chenhLech != null ? c.chenhLech : c.chenhLechDonGia) }}

                  </td>

                </tr>

              </tbody>

            </table>

          </div>



          <div class="confirm-cost-breakdown" style="margin-top: 14px;">

            <div class="cost-row">

              <span>Tổng tiền hàng cũ:</span>

              <strong style="text-decoration: line-through; color: #64748b;">{{ formatMoney(priceChangeData.tongTienCu) }}</strong>

            </div>

            <div class="cost-row total-highlight">

              <span>Tổng tiền hàng cập nhật theo giá mới:</span>

              <strong class="total-big-val" style="color: #b45309;">{{ formatMoney(priceChangeData.tongTienMoi) }}</strong>

            </div>

          </div>

        </div>



        <div class="modal-actions-bottom">

          <button type="button" class="btn-cancel-modal" @click="showPriceChangeModal = false">

            Kiểm tra lại giỏ hàng

          </button>

          <button

              type="button"

              class="btn-submit-modal"

              style="background-color: #b45309;"

              @click="acceptNewPriceAndContinue"

          >

            Đồng ý cập nhật giá mới & Tiếp tục

          </button>

        </div>

      </div>

    </div>



    <!-- Modal Bàn giao Hóa đơn khi Nhân viên bị khóa / đổi quyền (Case 3) -->

    <div v-if="showHandoverModal" class="pos-modal-backdrop" @click.self="showHandoverModal = false">

      <div class="pos-modal-card confirm-order-modal handover-modal">

        <div class="modal-top">

          <div class="modal-title-group">

            <h3 class="modal-title-text" style="color: #dc2626;">Bàn giao hóa đơn cho nhân viên khác</h3>

            <span class="modal-badge-count">Mã: {{ currentInvoice.code }}</span>

          </div>

          <button class="btn-modal-close" @click="showHandoverModal = false">✕</button>

        </div>



        <div class="confirm-modal-body">

          <div class="handover-warning-box">

            <strong>Thông báo bảo mật:</strong> {{ handoverReason }}

            <br />

            Để đảm bảo không mất dữ liệu giỏ hàng và không thanh toán trùng lặp, vui lòng chọn nhân viên tiếp nhận bên dưới để bàn giao hóa đơn chờ này.

          </div>



          <div class="pos-form-group" style="margin-top: 14px;">

            <label class="group-label-gray">Chọn nhân viên tiếp nhận bàn giao <span class="text-danger">*</span></label>

            <select v-model="selectedHandoverStaffId" class="pos-form-input">

              <option v-for="nv in handoverStaffList" :key="nv.id" :value="nv.id">

                {{ nv.tenNhanVien }} ({{ nv.maNhanVien }}) - {{ nv.soDienThoai || 'Nhân viên bán hàng' }}

              </option>

            </select>

          </div>



          <div class="pos-form-group" style="margin-top: 12px;">

            <label class="group-label-gray">Ghi chú bàn giao</label>

            <textarea v-model="handoverNote" rows="2" class="pos-form-input" placeholder="Ghi chú bàn giao..."></textarea>

          </div>

        </div>



        <div class="modal-actions-bottom">

          <button type="button" class="btn-cancel-modal" @click="showHandoverModal = false">

            Hủy bỏ

          </button>

          <button

              type="button"

              class="btn-submit-modal"

              style="background-color: #dc2626;"

              :disabled="isHandingOver"

              @click="confirmHandoverInvoice"

          >

            {{ isHandingOver ? 'Đang bàn giao...' : 'Xác nhận bàn giao hóa đơn' }}

          </button>

        </div>

      </div>

    </div>

    <!-- ==================== MODAL MÃ VIETQR (KHUNG NGOÀI TO RỘNG 860PX - 2 CỘT CHUYÊN NGHIỆP) ==================== -->
    <div v-if="showVietQrModal" class="pos-modal-backdrop" @click.self="closeVietQrModal">
      <div class="pos-vietqr-standalone-dialog">
        <!-- Nút đóng góc phải -->
        <button type="button" class="btn-modal-close" @click="closeVietQrModal" title="Đóng">✕</button>

        <!-- Header Modal -->
        <div class="vietqr-modal-header-clean">
          <div class="header-left-title">
            <h3 class="vietqr-modal-title">Thanh toán Chuyển khoản VietQR</h3>
            <span class="vietqr-subtitle">Quét mã bằng bất kỳ ứng dụng ngân hàng hoặc ví điện tử</span>
          </div>
          <span class="vietqr-code-pill">Mã HĐ: {{ currentInvoice.code }}</span>
        </div>

        <div class="vietqr-modal-body-grid">
          <!-- CỘT TRÁI: MÃ QR VIETQR CỰC ĐẠI -->
          <div class="vietqr-col-left">
            <div class="vietqr-qr-frame">
              <img
                  :src="transferQrUrl"
                  :alt="`Mã VietQR thanh toán ${formatMoney(transferTargetAmount)}`"
                  class="vietqr-big-code"
              />
              <div v-if="currentInvoice.isTransferPaid" class="vietqr-overlay-paid">
                <span class="overlay-check-text">ĐÃ THANH TOÁN THÀNH CÔNG</span>
              </div>
            </div>
            <div class="vietqr-scan-guide">
              Hướng camera điện thoại hoặc app ngân hàng để quét mã
            </div>
          </div>

          <!-- CỘT PHẢI: CHI TIẾT TÀI KHOẢN & TÓM TẮT & XÁC NHẬN -->
          <div class="vietqr-col-right">
            <!-- Bảng thông tin chi tiết tài khoản ngân hàng -->
            <div class="vietqr-details-table">
              <div class="vqr-row">
                <span class="vqr-label">Ngân hàng (BIN)</span>
                <span class="vqr-val font-bold text-navy">{{ transferApiInfo.bankName || 'MB Bank (970422)' }}</span>
              </div>
              <div class="vqr-row">
                <span class="vqr-label">Số tài khoản</span>
                <div class="vqr-val-group">
                  <span class="vqr-val font-bold text-navy text-mono">{{ transferApiInfo.accountNo || '0988888888' }}</span>
                  <button type="button" class="btn-vqr-copy-mini" @click="copyToClipboard(transferApiInfo.accountNo || '0988888888', 'Số tài khoản')">Sao chép</button>
                </div>
              </div>
              <div class="vqr-row">
                <span class="vqr-label">Chủ tài khoản</span>
                <span class="vqr-val font-bold text-navy text-uppercase">{{ transferApiInfo.accountName || 'FF T-SHIRT STORE' }}</span>
              </div>
              <div class="vqr-row highlight-amount-row">
                <span class="vqr-label">Số tiền cần chuyển</span>
                <strong class="vqr-val text-red font-bold text-amount-large">{{ formatMoney(transferTargetAmount) }}</strong>
              </div>
              <div class="vqr-row">
                <span class="vqr-label">Nội dung chuyển khoản</span>
                <div class="vqr-val-group">
                  <span class="vqr-val font-bold text-navy text-mono">{{ transferContent }}</span>
                  <button type="button" class="btn-vqr-copy-mini" @click="copyToClipboard(transferContent, 'Nội dung')">Sao chép</button>
                </div>
              </div>
            </div>

            <!-- Tóm tắt đơn hàng -->
            <div class="vietqr-invoice-summary">
              <div class="sum-line">
                <span class="sum-lbl">Mã hóa đơn:</span>
                <strong class="sum-v">{{ currentInvoice.code }}</strong>
              </div>
              <div class="sum-line">
                <span class="sum-lbl">Tổng cần thanh toán:</span>
                <strong class="sum-v">{{ formatMoney(finalTotal) }}</strong>
              </div>
              <div class="sum-line sum-transfer">
                <span class="sum-lbl">Tiền chuyển khoản:</span>
                <strong class="sum-v text-red font-bold">{{ formatMoney(transferTargetAmount) }}</strong>
              </div>
              <div class="sum-line-sub" v-if="currentInvoice.paymentMethod === 'KET_HOP'">
                Tiền mặt: {{ formatMoney(currentInvoice.cashAmount || 0) }} · Tiền thừa: {{ formatMoney(changeAmount) }}
              </div>
            </div>

            <!-- Ghi chú nhân viên kiểm tra -->
            <div class="vietqr-staff-notice">
              Kiểm tra khoản tiền đã nhận trong tài khoản ngân hàng trước khi xác nhận. Hệ thống ghi nhận xác nhận thủ công của nhân viên.
            </div>

            <!-- Checkbox xác nhận -->
            <label class="vietqr-verify-box">
              <input type="checkbox" v-model="currentInvoice.isTransferVerifiedByStaff" />
              <span class="verify-txt">
                Tôi đã kiểm tra đủ <strong>{{ formatMoney(transferTargetAmount) }}</strong> tiền chuyển khoản.
              </span>
            </label>

            <!-- Nút hành động trực tiếp ở cột phải -->
            <div class="vietqr-right-actions">
              <button type="button" class="btn-modal-back" @click="closeVietQrModal">
                Đóng
              </button>
              <button
                  type="button"
                  class="btn-modal-confirm-pay"
                  :disabled="isConfirmingPayment"
                  @click="handleConfirmVietQrInModal"
              >
                {{ isConfirmingPayment ? 'Đang xử lý...' : (currentInvoice.isTransferPaid ? 'Đã nhận đủ tiền' : 'Xác nhận đã nhận tiền') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showQuickAddCustomerModal" class="pos-modal-backdrop" @click.self="showQuickAddCustomerModal = false">

      <div class="pos-modal-card quick-customer-card">

        <div class="modal-top">

          <h3>Thêm nhanh khách hàng mới</h3>

          <button class="btn-modal-close" @click="showQuickAddCustomerModal = false">✕</button>

        </div>



        <div class="modal-body-form">

          <div class="pos-form-group">

            <label class="group-label-gray">Tên khách hàng <span class="text-danger">*</span></label>

            <input

                type="text"

                v-model="newCustomerForm.tenKhachHang"

                placeholder="Nhập họ và tên..."

                class="pos-form-input"

                :class="{ 'input-error': newCustomerErrors.tenKhachHang }"

            />

            <span v-if="newCustomerErrors.tenKhachHang" class="error-text">{{ newCustomerErrors.tenKhachHang }}</span>

          </div>



          <div class="pos-form-group">

            <label class="group-label-gray">Số điện thoại <span class="text-danger">*</span></label>

            <input

                type="text"

                v-model="newCustomerForm.soDienThoai"

                placeholder="Nhập số điện thoại 10 số (VD: 0912345678)..."

                class="pos-form-input"

                :class="{ 'input-error': newCustomerErrors.soDienThoai }"

            />

            <span v-if="newCustomerErrors.soDienThoai" class="error-text">{{ newCustomerErrors.soDienThoai }}</span>

          </div>



          <div class="pos-form-group">

            <label class="group-label-gray">Email</label>

            <input

                type="email"

                v-model="newCustomerForm.email"

                placeholder="Nhập email nếu có..."

                class="pos-form-input"

            />

          </div>



          <!-- Địa chỉ với API hành chính chuẩn 2 cấp -->

          <div class="pos-form-grid-2">

            <div class="pos-form-group">

              <label class="group-label-gray">Tỉnh / Thành phố</label>

              <Select2Combobox

                  v-model="quickCustomerProvinceCode"

                  :options="provincesList"

                  value-key="code"

                  label-key="name"

                  placeholder="-- Chọn Tỉnh / Thành phố --"

                  search-placeholder="Tìm Tỉnh / Thành phố..."

                  @change="onQuickCustomerProvinceChange"

              />

            </div>

            <div class="pos-form-group">

              <label class="group-label-gray">Phường / Xã</label>

              <Select2Combobox

                  v-model="quickCustomerWardCode"

                  :options="quickCustomerWardsList"

                  value-key="code"

                  label-key="name"

                  :disabled="!quickCustomerProvinceCode"

                  placeholder="-- Chọn Phường / Xã --"

                  search-placeholder="Tìm Phường / Xã..."

                  @change="onQuickCustomerWardChange"

              />

            </div>

          </div>



          <div class="pos-form-group">

            <label class="group-label-gray">Địa chỉ cụ thể</label>

            <input

                type="text"

                v-model="newCustomerForm.diaChiCuThe"

                placeholder="Số nhà, ngõ, tên đường..."

                class="pos-form-input"

            />

          </div>

        </div>



        <div class="modal-actions-bottom">

          <button class="btn-cancel-modal" @click="showQuickAddCustomerModal = false">Đóng</button>

          <button class="btn-submit-modal" @click="submitQuickAddCustomer">Lưu khách hàng</button>

        </div>

      </div>

    </div>



    <!-- ==================== POPUP THÊM ĐỊA CHỈ CHO KHÁCH HÀNG ==================== -->

    <div v-if="showAddAddressModal" class="pos-modal-backdrop" @click.self="showAddAddressModal = false">

      <div class="pos-modal-card quick-customer-card">

        <div class="modal-top">

          <h3>Thêm địa chỉ cho khách: {{ currentInvoice.customerName }}</h3>

          <button class="btn-modal-close" @click="showAddAddressModal = false">✕</button>

        </div>



        <div class="modal-body-form">

          <div class="pos-form-grid-2">

            <div class="pos-form-group">

              <label class="group-label-gray">Tỉnh / Thành phố <span class="text-danger">*</span></label>

              <Select2Combobox

                  v-model="addrModalProvinceCode"

                  :options="provincesList"

                  value-key="code"

                  label-key="name"

                  placeholder="-- Chọn Tỉnh / TP --"

                  search-placeholder="Tìm Tỉnh / Thành phố..."

                  @change="onAddrModalProvinceChange"

              />

            </div>

            <div class="pos-form-group">

              <label class="group-label-gray">Phường / Xã <span class="text-danger">*</span></label>

              <Select2Combobox

                  v-model="addrModalWardCode"

                  :options="addrModalWardsList"

                  value-key="code"

                  label-key="name"

                  :disabled="!addrModalProvinceCode"

                  placeholder="-- Chọn Phường / Xã --"

                  search-placeholder="Tìm Phường / Xã..."

                  @change="onAddrModalWardChange"

              />

            </div>

          </div>



          <div class="pos-form-group">

            <label class="group-label-gray">Địa chỉ cụ thể <span class="text-danger">*</span></label>

            <input

                type="text"

                v-model="newAddressForm.diaChiCuThe"

                placeholder="Số nhà, ngõ, tên đường..."

                class="pos-form-input"

            />

          </div>



          <div class="pos-form-group" style="margin-top: 4px;">

            <label style="cursor: pointer; display: flex; align-items: center; gap: 8px;">

              <input type="checkbox" v-model="newAddressForm.macDinh" />

              <span style="font-size: 0.85rem; color: #496883; font-weight: 500;">Đặt làm địa chỉ mặc định của khách hàng</span>

            </label>

          </div>

        </div>



        <div class="modal-actions-bottom">

          <button class="btn-cancel-modal" @click="showAddAddressModal = false">Đóng</button>

          <button class="btn-submit-modal" :disabled="savingCustomerAddress" @click="submitAddCustomerAddress">

            {{ savingCustomerAddress ? 'Đang lưu...' : 'Lưu địa chỉ' }}

          </button>

        </div>

      </div>

    </div>



    <!-- ==================== POPUP SỔ ĐỊA CHỈ KHÁCH HÀNG ==================== -->

    <div v-if="showSelectAddressModal" class="pos-modal-backdrop" @click.self="showSelectAddressModal = false">

      <div class="pos-modal-card quick-customer-card" style="max-width: 520px;">

        <div class="modal-top">

          <h3>Sổ địa chỉ: {{ currentInvoice.customerName }}</h3>

          <button class="btn-modal-close" @click="showSelectAddressModal = false">✕</button>

        </div>



        <div class="modal-body-form" style="max-height: 52vh; overflow-y: auto; padding-right: 4px;">

          <div

              v-for="addr in customerAddressesList"

              :key="addr.id"

              class="addr-item-card"

              :class="{ 'addr-active': currentInvoice.deliveryAddress === formatAddrObj(addr) }"

              @click="chooseSavedAddress(addr)"

          >

            <div class="addr-item-content">

              <div class="addr-item-text">{{ formatAddrObj(addr) }}</div>

              <span v-if="addr.macDinh" class="addr-badge-default">Mặc định</span>

            </div>

            <button type="button" class="btn-pick-addr">Chọn</button>

          </div>

        </div>



        <div class="modal-actions-bottom" style="justify-content: space-between;">

          <button class="btn-submit-modal" style="background-color: #647074;" @click="openCustomerAddAddressModal">

            + Thêm địa chỉ mới

          </button>

          <button class="btn-cancel-modal" @click="showSelectAddressModal = false">Đóng</button>

        </div>

      </div>

    </div>

  </div>

</template>



<style scoped>

/* ==========================================================

   TỔNG THỂ THEO BẢNG MÀU DỰ ÁN FF T-SHIRT

========================================================== */

.pos-page-wrapper {

  padding: 1.25rem 2rem 3rem;

  background-color: var(--bg, #f7f5ef);

  min-height: calc(100vh - 48px);

  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;

  color: #3d4a50;

  box-sizing: border-box;

}



/* Header */

.page-breadcrumb {

  margin-bottom: 16px;

  font-size: 0.88rem;

}



.crumb-parent {

  color: #8a9292;

  text-decoration: none;

}



.crumb-parent:hover {

  color: #496883;

}



.crumb-separator {

  margin: 0 8px;

  color: #c4c4c4;

}



.crumb-current {

  color: #496883;

  font-weight: 600;







}



/* Grid Layout 2 Cột: 2/3 Màn hình bên trái & 1/3 Màn hình bên phải */

.pos-grid-container {

  display: grid;

  grid-template-columns: 2fr 1fr;

  gap: 18px;

  align-items: start;

}



.pos-col-left, .pos-col-right {

  display: flex;

  flex-direction: column;

  gap: 16px;

  min-width: 0;

}



/* Thẻ Card */

.pos-panel-card {

  background: #ffffff;

  border-radius: 8px;

  border: 1px solid #e9e5db;

  box-shadow: 0 1px 3px rgba(65, 60, 50, 0.03);

  padding: 16px 18px;

}



/* ==================== CỘT TRÁI ==================== */

/* Hóa đơn chờ: Dòng trên là nút Thêm & số lượng, dòng dưới là các tab hóa đơn */

.waiting-invoices-card {

  padding: 12px 16px;

  display: flex;

  flex-direction: column;

  gap: 10px;

}



.waiting-top-row {

  display: flex;

  justify-content: space-between;

  align-items: center;

  width: 100%;

}



.btn-create-invoice {

  background-color: #ffffff;

  border: 1px solid #d6d0c3;

  padding: 7px 14px;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;

  color: #496883;

  cursor: pointer;

  transition: all 0.2s;

  flex-shrink: 0;

  white-space: nowrap;

}



.btn-create-invoice:hover {

  background-color: #f7f5ef;

  border-color: #496883;

}



.tabs-scroll-wrapper {

  display: flex;

  align-items: center;

  gap: 8px;

  overflow-x: auto;

  width: 100%;

  padding: 2px 0;

}



.counter-text {

  font-size: 0.8rem;

  color: #8a9292;

  flex-shrink: 0;

  white-space: nowrap;

}



.inv-tab-pill {

  background-color: #f7f5ef;

  padding: 7px 14px;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  color: #647074;

  cursor: pointer;

  display: flex;

  align-items: center;

  gap: 4px;

  border: 1px solid transparent;

  transition: all 0.2s;

  white-space: nowrap;

}



.inv-tab-pill.active {

  background-color: #eaf1f4;

  color: #496883;

  border-color: #496883;

}



.inv-tab-count {

  font-size: 0.75rem;

  color: #c94a29;

}



/* Giỏ hàng */

.cart-head-bar {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 14px;

  flex-wrap: wrap;

  gap: 10px;

}



.cart-title-wrap {

  display: flex;

  align-items: center;

  gap: 8px;

}



.cart-title-text {

  font-size: 1rem;

  font-weight: 600;

  color: #496883;

}



.cart-count-badge {

  background-color: #fff4e5;

  color: #c94a29;

  font-size: 0.75rem;

  font-weight: 600;







  padding: 3px 10px;

  border-radius: 12px;

  border: 1px solid #f5d6a6;

}



.cart-actions-right {

  display: flex;

  align-items: center;

  gap: 8px;

}



.barcode-input-wrapper {

  display: flex;

  align-items: center;

}



.barcode-quick-input {

  width: 145px;

  height: 34px;

  padding: 0 8px;

  border: 1px solid #d6d0c3;

  border-right: none;

  border-radius: 6px 0 0 6px;



  outline: none;

}



.barcode-quick-input:focus {

  border-color: #496883;

}



.btn-scan-barcode {

  width: 34px;

  height: 34px;

  border-radius: 0 6px 6px 0;

  border: 1px solid #d6d0c3;

  background: #ffffff;

  color: #496883;

  display: flex;

  align-items: center;

  justify-content: center;

  cursor: pointer;

  transition: all 0.2s;

}



.btn-scan-barcode:hover {

  background-color: #f7f5ef;

}



.btn-open-select {

  background-color: #496883;

  border: 1px solid #496883;

  color: #ffffff;

  padding: 7px 14px;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  cursor: pointer;

  transition: all 0.2s;

}



.btn-open-select:hover {

  opacity: 0.9;

}



/* Bảng Giỏ hàng - Loại bỏ cuộn ngang, vừa vặn 1366x768 & 1920x1080 */

.cart-table-container {

  overflow-x: hidden;

  border-radius: 8px;

  background: #ffffff;

  width: 100%;

}



.pos-custom-table {

  width: 100%;

  min-width: 100%;

  table-layout: fixed;

  border-collapse: separate;

  border-spacing: 0;

  font-size: 0.84rem;

}



.pos-custom-table thead th {

  background-color: #f7f5ef !important;

  color: #496883 !important;

  font-weight: 700 !important;

  font-size: 0.78rem !important;

  text-transform: uppercase !important;

  letter-spacing: 0.3px !important;

  padding: 9px 4px !important;

  border-bottom: 2px solid #e9e5db !important;

  vertical-align: middle !important;

  white-space: nowrap;

}



.pos-custom-table thead th.text-center,

.pos-custom-table tbody td.text-center {

  text-align: center !important;

}



.pos-custom-table thead th.text-right,

.pos-custom-table tbody td.text-right {

  text-align: right !important;

}



.pos-custom-table thead th.text-left,

.pos-custom-table tbody td.text-left {

  text-align: left !important;

}



.pos-custom-table tbody td {

  padding: 7px 4px !important;

  border-bottom: 1px solid #f0ede6 !important;

  color: #334155;

  vertical-align: middle !important;

}



.pos-custom-table tbody tr:hover td {

  background-color: #f8fafc !important;

}



.empty-cart-message {

  text-align: center;

  padding: 30px !important;

  color: #8a9292;

  font-size: 0.88rem;

}



.cart-code-badge {

  display: inline-block;

  padding: 2px 6px;

  background-color: #f1f5f9;

  border: 1px solid #e2e8f0;

  border-radius: 4px;

  font-weight: 700;

  font-size: 0.78rem;

  color: #334155;

  letter-spacing: 0.3px;

  max-width: 100%;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;

}



.cart-product-name-wrap {

  display: flex;

  align-items: center;

  flex-wrap: wrap;

  gap: 5px;

  line-height: 1.3;

}



.cart-product-title {

  color: #1e293b;

  font-weight: 600;

  font-size: 0.85rem;

  word-break: break-word;

}



.cart-discount-badge {

  display: inline-flex;

  align-items: center;

  font-size: 0.70rem;

  font-weight: 700;

  padding: 1px 5px;

  border-radius: 4px;

  background-color: #fef2f2;

  color: #dc2626;

  border: 1px solid #fecaca;

  white-space: nowrap;

}



.cart-variant-pill {

  display: inline-flex;

  align-items: center;

  justify-content: center;

  gap: 4px;

  padding: 2px 7px;

  background-color: #f8fafc;

  border: 1px solid #e2e8f0;

  border-radius: 4px;

  font-size: 0.80rem;

  color: #334155;

  font-weight: 500;

  white-space: nowrap;

}



.variant-color-text {

  color: #334155;

}



.variant-slash {

  color: #94a3b8;

  font-size: 0.75rem;

}



.variant-size-text {

  color: #1e293b;

  font-weight: 700;

}



.cart-color-dot {

  width: 8px;

  height: 8px;

  border-radius: 50%;

  display: inline-block;

  border: 1px solid rgba(0, 0, 0, 0.15);

  box-shadow: 0 1px 2px rgba(0,0,0,0.06);

}



.font-600 {

  font-weight: 600;

}



.pos-custom-table tbody td.font-price, .font-price {

  text-align: center !important;

  font-weight: 600;

}



.cart-price-col {

  display: flex !important;

  flex-direction: column !important;

  align-items: center !important;

  justify-content: center !important;

  text-align: center !important;

  gap: 1px !important;

}



.cart-price-sale {

  color: #dc2626;

  font-weight: 700;

  font-size: 0.88rem;

  white-space: nowrap;

  text-align: center !important;

}





.cart-price-old {

  color: #94a3b8;

  text-decoration: line-through;

  font-size: 0.74rem;

  white-space: nowrap;

  text-align: center !important;

}





.cart-price-normal {

  color: #1e293b;

  font-weight: 700;

  font-size: 0.88rem;

  white-space: nowrap;

  text-align: center !important;

}





.pos-qty-box {

  display: inline-flex;

  align-items: center;

  border: 1px solid #d1d5db;

  border-radius: 4px;

  background: #ffffff;

  overflow: hidden;

  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  transition: all 0.2s ease;

}



.pos-qty-box:focus-within {

  border-color: #3b82f6;

  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);

}



.qty-btn {

  width: 24px;

  height: 26px;

  border: none;

  background: #f8fafc;

  cursor: pointer;

  font-size: 0.90rem;

  font-weight: 700;

  color: #334155;

  display: inline-flex;

  align-items: center;

  justify-content: center;

  transition: all 0.15s ease;

  user-select: none;

}



.qty-btn:hover:not(:disabled) {

  background: #e2e8f0;

  color: #0f172a;

}



.qty-btn:disabled {

  opacity: 0.35;

  cursor: not-allowed;

  background: #f1f5f9;

}



.qty-input {

  width: 34px;

  height: 26px;

  border: none;

  text-align: center;

  font-weight: 700;

  font-size: 0.85rem;

  color: #1e293b;

  background: #ffffff;

  outline: none;

  padding: 0 1px;

  margin: 0;

  box-sizing: border-box;

  -moz-appearance: textfield;

  appearance: textfield;

}



.qty-input::-webkit-outer-spin-button,

.qty-input::-webkit-inner-spin-button {

  -webkit-appearance: none;

  margin: 0;

}



.btn-trash-row {

  width: 28px;

  height: 28px;

  border-radius: 5px;

  border: 1px solid transparent;

  background: transparent;

  cursor: pointer;

  display: inline-flex;

  align-items: center;

  justify-content: center;

  transition: all 0.2s ease;

  color: #94a3b8;

  padding: 0;

}



.btn-trash-row:hover {

  background: #fef2f2;

  border-color: #fee2e2;

  color: #ef4444;

}



.btn-trash-row:hover .trash-icon-svg {

  stroke: #ef4444;

}



/* ==================== GHN UI STYLES ==================== */

.ghn-badge-info {

  display: inline-flex;

  align-items: center;

  gap: 6px;

  background: #f0fdf4;

  border: 1px solid #bbf7d0;

  color: #166534;

  font-size: 0.74rem;

  font-weight: 600;

  padding: 4px 8px;

  border-radius: 6px;

  margin-top: 5px;

  width: 100%;

  box-sizing: border-box;

}



.ghn-logo-tag {

  background: #ea580c;

  color: #ffffff;

  font-size: 0.65rem;

  font-weight: 800;

  padding: 1px 5px;

  border-radius: 3px;

  letter-spacing: 0.5px;

}



.btn-calc-ghn {

  background: #f8fafc;

  border: 1px solid #cbd5e1;

  color: #334155;

  border-radius: 5px;

  padding: 3px 8px;

  font-size: 0.74rem;

  font-weight: 600;

  cursor: pointer;

  white-space: nowrap;

  transition: all 0.2s ease;

  display: inline-flex;

  align-items: center;

  gap: 4px;

}



.btn-calc-ghn:hover:not(:disabled) {

  background: #e2e8f0;

  color: #0f172a;

  border-color: #94a3b8;

}



.btn-calc-ghn:disabled {

  opacity: 0.6;

  cursor: not-allowed;

}



/* ==================== CỘT PHẢI ==================== */

.card-bold-title {

  font-size: 0.95rem;

  font-weight: 600;

  color: #496883;

  margin: 0;

}



.cust-card-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

}



.badge-guest-pill {

  font-size: 0.75rem;

  background-color: #f7f5ef;

  color: #496883;

  padding: 2px 8px;

  border-radius: 10px;

  border: 1px solid #d6d0c3;

  font-weight: 600;







}



.customer-search-relative {

  position: relative;

}



.customer-search-row {

  display: flex;

  align-items: center;

  gap: 8px;

  width: 100%;

}



.customer-search-row .pos-form-input {

  flex: 1 1 auto;

  min-width: 0;

  width: auto;

}



.btn-add-customer-quick {

  flex: 0 0 auto;

  white-space: nowrap;

  height: 38px;

  padding: 0 14px;

  background-color: #496883;

  color: #ffffff;

  border: 1px solid #496883;

  border-radius: 6px;

  font-size: 0.84rem;

  font-weight: 600;

  cursor: pointer;

  display: inline-flex;

  align-items: center;

  justify-content: center;

  transition: all 0.2s ease;

}



.btn-add-customer-quick:hover {

  background-color: #3b5368;

}



.price-change-warning-box {

  background-color: #fffbeb;

  border: 1px solid #fde68a;

  border-radius: 6px;

  padding: 10px 14px;

  color: #92400e;

  font-size: 0.85rem;

  line-height: 1.45;

  margin-bottom: 12px;

}



.handover-warning-box {

  background-color: #fef2f2;

  border: 1px solid #fecaca;

  border-radius: 6px;

  padding: 10px 14px;

  color: #991b1b;

  font-size: 0.85rem;

  line-height: 1.45;

  margin-bottom: 12px;

}



/* ==================== BỐ CỤC THẺ THÔNG TIN GIAO HÀNG ==================== */

.delivery-info-card {

  padding: 14px 16px;

  background: #ffffff;

  border-radius: 8px;

  border: 1px solid #eae7df;

  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

}



.delivery-card-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding-bottom: 8px;

  border-bottom: 1px dashed #e9e5db;

}



.delivery-header-actions {

  display: flex;

  align-items: center;

  gap: 12px;

}



.delivery-title-badge-wrap {

  display: flex;

  align-items: center;

  gap: 12px;

}



.delivery-badge-on, .delivery-badge-off {

  margin-left: 8px;

}



.delivery-card-title {

  font-size: 0.95rem;

  font-weight: 700;

  color: #2b3e4f;

}



.delivery-badge-on {

  font-size: 0.74rem;

  font-weight: 600;

  color: #1e7040;

  background-color: #eaf6ee;

  border: 1px solid #b7e3c6;

  border-radius: 12px;

  padding: 2px 10px;

  display: inline-flex;

  align-items: center;

}



.delivery-badge-off {

  font-size: 0.74rem;

  font-weight: 500;

  color: #647074;

  background-color: #f1f3f5;

  border: 1px solid #e2e8f0;

  border-radius: 12px;

  padding: 2px 10px;

  display: inline-flex;

  align-items: center;

}



.delivery-switch-label {

  font-size: 0.84rem;

  font-weight: 600;

  color: #496883;

}



.delivery-off-box {

  display: flex;

  align-items: center;

  gap: 10px;

  margin-top: 10px;

  padding: 10px 14px;

  background-color: #f8fafc;

  border: 1px dashed #cbd5e1;

  border-radius: 6px;

  color: #64748b;

  font-size: 0.83rem;

  line-height: 1.45;

}



.delivery-off-box strong {

  color: #334155;

}



.delivery-off-box .off-icon {

  font-size: 1.25rem;

  flex-shrink: 0;

}



.delivery-box-expand {

  margin-top: 10px;

  margin-bottom: 4px;

  padding: 12px;

  background: #fbf9f4;

  border-radius: 6px;

  border: 1px solid #eae5d8;

}



.pos-page-wrapper {

  padding-bottom: 40px;

}





.selected-customer-pill {

  display: flex;

  justify-content: space-between;

  align-items: center;

  background-color: #eaf1f4;

  border: 1px solid #d2e1e9;

  border-radius: 6px;

  padding: 8px 12px;

}



.cust-name {

  font-weight: 600;







  color: #496883;

  font-size: 0.88rem;

  margin-right: 6px;

}



.cust-phone {



  color: #647074;

}



.btn-clear-cust {

  background: none;

  border: none;

  color: #c94a29;

  font-size: 0.78rem;

  font-weight: 600;







  cursor: pointer;

}



.btn-clear-cust:hover {

  text-decoration: underline;

}



.customer-dropdown-menu {

  position: absolute;

  top: 42px;

  left: 0;

  right: 0;

  background: #ffffff;

  border: 1px solid #d6d0c3;

  border-radius: 6px;

  box-shadow: 0 4px 12px rgba(0,0,0,0.1);

  z-index: 100;

  max-height: 180px;

  overflow-y: auto;

}



.customer-dropdown-item {

  padding: 8px 12px;

  border-bottom: 1px solid #f0f0f0;

  cursor: pointer;

  transition: background-color 0.15s;

}



.customer-dropdown-item:hover {

  background-color: #f7f5ef;

}



.c-name {

  font-weight: 600;







  font-size: 0.86rem;

  color: #333;

}



.c-meta {

  font-size: 0.78rem;

  color: #8a9292;

}



/* Checkout Panel */

.order-header-toggle {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 14px;

}



.order-title-brand {

  font-size: 0.95rem;

  font-weight: 600;

  color: #496883;

}



/* Switch giao hàng */

.delivery-switch-wrap {

  display: flex;

  align-items: center;

  gap: 8px;

  font-size: 0.85rem;

  color: #647074;

  cursor: pointer;

}



.delivery-switch-wrap input {

  display: none;

}



.switch-ui {

  width: 36px;

  height: 20px;

  background-color: #d6d0c3;

  border-radius: 20px;

  position: relative;

  transition: all 0.2s;

}



.switch-ui::after {

  content: '';

  position: absolute;

  width: 16px;

  height: 16px;

  border-radius: 50%;

  background: white;

  top: 2px;

  left: 2px;

  transition: all 0.2s;

}



.delivery-switch-wrap input:checked + .switch-ui {

  background-color: var(--blue, #496883);

}



.delivery-switch-wrap input:checked + .switch-ui::after {

  transform: translateX(16px);

}



.delivery-box-expand {

  margin-bottom: 12px;

  padding: 10px;

  background: #f7f5ef;

  border-radius: 6px;

  border: 1px solid #e9e5db;

}



.pos-summary-line {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 10px;

  font-size: 0.88rem;

  color: #647074;

}



.discount-line {

  color: #2e7d32;

}



.text-discount-bold {

  font-weight: 600;

  color: #2e7d32;

}



.total-highlight-line {

  margin-top: 12px;

  padding-top: 10px;

  border-top: 1px dashed #d6d0c3;

  margin-bottom: 14px;

}



.total-label {

  font-size: 0.95rem;

  font-weight: 600;

  color: #333;

}



.total-value {

  font-size: 1.15rem;

  font-weight: 800;

  color: #c94a29;

}



.font-bold-dark {

  font-weight: 600;

  color: #333;

}



.text-change-bold {

  font-weight: 600;

  color: #2e7d32;

  font-size: 0.95rem;

}



.change-val-wrap {

  display: flex;

  flex-direction: column;

  align-items: flex-end;

}



.text-missing-warning {

  font-size: 0.76rem;

  color: #c94a29;

  font-weight: 600;







}



.pos-form-group {

  display: flex;

  flex-direction: column;

  gap: 5px;

  margin-bottom: 12px;

}



.group-label-gray {



  color: #647074;

  font-weight: 600;







}



/* ==================== PHIẾU GIẢM GIÁ (TỰ ĐỘNG ÁP DỤNG TỐT NHẤT) ==================== */

.voucher-auto-wrapper {

  margin-bottom: 8px;

}



.voucher-label-row {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 4px;

}



.badge-auto-apply {

  display: inline-flex;

  align-items: center;

  gap: 3px;

  font-size: 0.68rem;

  font-weight: 600;

  color: #059669;

  background-color: #ecfdf5;

  border: 1px solid #a7f3d0;

  padding: 1px 7px;

  border-radius: 10px;

}



.auto-voucher-card {

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 5px 9px;

  background: #f0fdf4;

  border: 1px solid #bbf7d0;

  border-radius: 6px;

  gap: 8px;

  transition: all 0.2s ease;

}



.auto-v-left {

  display: flex;

  align-items: center;

  gap: 6px;

  min-width: 0;

  flex: 1;

}



.auto-v-icon {

  font-size: 1.05rem;

  flex-shrink: 0;

}



.auto-v-details {

  display: flex;

  flex-direction: column;

  gap: 1px;

  min-width: 0;

}



.auto-v-title-row {

  display: flex;

  align-items: center;

  gap: 5px;

  flex-wrap: wrap;

}



.auto-v-badge {

  font-size: 0.64rem;

  font-weight: 700;

  padding: 1px 5px;

  border-radius: 3px;

  text-transform: uppercase;

  letter-spacing: 0.2px;

}



.auto-v-badge.badge-public {

  background-color: #e0f2fe;

  color: #0369a1;

  border: 1px solid #bae6fd;

}



.auto-v-badge.badge-personal {

  background-color: #f3e8ff;

  color: #7e22ce;

  border: 1px solid #e9d5ff;

}



.auto-v-code {

  font-size: 0.80rem;

  color: #1e293b;

  font-weight: 700;

}



.auto-v-name {

  font-size: 0.74rem;

  color: #475569;

  white-space: nowrap;

  overflow: hidden;

  text-overflow: ellipsis;

  max-width: 140px;

}



.auto-v-condition {

  font-size: 0.70rem;

  color: #047857;

  font-weight: 600;

  line-height: 1.2;

}



.auto-v-right {

  text-align: right;

  flex-shrink: 0;

}



.auto-v-discount-text {

  font-size: 0.88rem;

  font-weight: 800;

  color: #16a34a;

}



.auto-v-rate-sub {

  font-size: 0.68rem;

  color: #64748b;

  font-weight: 500;

}



.auto-voucher-none-card {

  display: flex;

  align-items: center;

  gap: 6px;

  padding: 5px 9px;

  background: #f8fafc;

  border: 1px dashed #cbd5e1;

  border-radius: 6px;

  color: #94a3b8;

  font-size: 0.75rem;

}



.auto-v-none-icon {

  font-size: 0.88rem;

  opacity: 0.7;

}



.auto-v-none-text {

  font-size: 0.75rem;

  color: #64748b;

}



.payment-radio-group {

  display: flex;

  gap: 14px;

}



.radio-item {

  display: flex;

  align-items: center;

  gap: 6px;

  font-size: 0.85rem;

  color: #333;

  cursor: pointer;

}



.radio-item input[type="radio"] {

  accent-color: #496883;

  cursor: pointer;

}



.pay-input-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

}



.btn-exact-pay {

  background: none;

  border: none;

  color: #496883;

  font-size: 0.78rem;

  font-weight: 600;







  cursor: pointer;

  padding: 0;

  text-decoration: underline;

}



.quick-cash-row {

  display: flex;

  gap: 6px;

  margin-top: 4px;

}



.btn-cash-chip {

  background-color: #f7f5ef;

  border: 1px solid #d6d0c3;

  border-radius: 4px;

  padding: 3px 8px;

  font-size: 0.75rem;

  font-weight: 600;







  color: #496883;

  cursor: pointer;

  transition: all 0.15s;

}



.btn-cash-chip:hover {

  background-color: #eaf1f4;

  border-color: #496883;

}





.vnpay-brand-group {

  display: flex;

  align-items: center;

  gap: 8px;

}



.vnpay-mode-tabs {

  display: inline-flex;

  background: #f1f5f9;

  border-radius: 4px;

  padding: 2px;

  gap: 2px;

}



.vnpay-tab-btn {

  background: transparent;

  border: none;

  font-size: 0.66rem;

  font-weight: 600;







  color: #64748b;

  padding: 2px 6px;

  border-radius: 3px;

  cursor: pointer;

  transition: all 0.15s ease;

}



.vnpay-tab-btn:hover {

  color: #1e293b;

}



.vnpay-tab-btn.active {

  background: #ffffff;

  color: #005baa;

  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);

}



.btn-vnpay-portal {

  display: inline-flex;

  align-items: center;

  justify-content: center;

  gap: 4px;

  background: linear-gradient(135deg, #005baa, #0284c7);

  color: #ffffff;

  border: 1px solid #005baa;

  border-radius: 4px;

  padding: 4px 10px;

  font-size: 0.74rem;

  font-weight: 600;







  cursor: pointer;

  transition: all 0.15s;

}



.btn-vnpay-portal:hover {

  opacity: 0.92;

  box-shadow: 0 2px 4px rgba(0, 91, 170, 0.25);

}



.vnpay-qr-wrap {

  cursor: pointer;

  transition: transform 0.15s ease, box-shadow 0.15s ease;

}



.vnpay-qr-wrap:hover {

  transform: scale(1.03);

  box-shadow: 0 3px 8px rgba(0, 91, 170, 0.18);

}



/* ==================== VNPAY-QR COMPACT BOX ==================== */

.vnpay-box-compact {

  background: #ffffff;

  border: 1px solid #d9e2ec;

  border-radius: 8px;

  padding: 10px 12px;

  margin-top: 8px;

  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);

  transition: all 0.2s ease;

}



.vnpay-box-compact.is-paid {

  border-color: #34a853;

  background: #f6fcf8;

}



.vnpay-top-bar {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 8px;

  padding-bottom: 6px;

  border-bottom: 1px dashed #e2e8f0;

}



.vnpay-brand-chip {

  display: inline-flex;

  align-items: center;

  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;

  font-weight: 900;

  font-size: 0.85rem;

  letter-spacing: 0.2px;

}



.vnp-blue {

  color: #005baa;

}



.vnp-red {

  color: #ea1d25;

}



.vnp-sub {

  color: #1e293b;

  font-weight: 600;

  font-size: 0.78rem;

  margin-left: 1px;

}



.vnpay-status-badge {

  display: inline-flex;

  align-items: center;

  gap: 4px;

  font-size: 0.72rem;

  font-weight: 600;







  padding: 2px 7px;

  border-radius: 10px;

}



.vnpay-status-badge.waiting {

  background: #fff7ed;

  color: #ea580c;

  border: 1px solid #ffedd5;

}



.vnpay-status-badge.paid {

  background: #e6f4ea;

  color: #137333;

  border: 1px solid #b7e1cd;

}



.vnpay-status-badge .status-dot {

  width: 5px;

  height: 5px;

  border-radius: 50%;

  background: currentColor;

}



.vnpay-status-badge.waiting .status-dot {

  animation: pulseDot 1.4s infinite;

}



@keyframes pulseDot {

  0%, 100% { opacity: 1; transform: scale(1); }

  50% { opacity: 0.3; transform: scale(0.8); }

}



.vnpay-body-compact {

  display: flex;

  align-items: center;

  gap: 12px;

}



.vnpay-qr-wrap {

  position: relative;

  width: 82px;

  height: 82px;

  flex-shrink: 0;

  border-radius: 6px;

  overflow: hidden;

  border: 1px solid #cbd5e1;

  background: #ffffff;

  padding: 2px;

  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

}



.vnpay-qr-img {

  width: 100%;

  height: 100%;

  object-fit: contain;

}



.vnpay-paid-overlay {

  position: absolute;

  inset: 0;

  background: rgba(34, 197, 94, 0.9);

  color: #ffffff;

  display: flex;

  align-items: center;

  justify-content: center;

}



.vnpay-check {

  font-size: 2rem;

  font-weight: 800;

}



.vnpay-meta-wrap {

  flex: 1;

  display: flex;

  flex-direction: column;

  gap: 3px;

  min-width: 0;

}



.vnpay-meta-row {

  display: flex;

  align-items: center;

  gap: 6px;

  font-size: 0.8rem;

}



.meta-label {

  color: #64748b;

  font-size: 0.74rem;

  white-space: nowrap;

}



.meta-amount {

  color: #ea1d25;

  font-weight: 600;

  font-size: 0.92rem;

}



.meta-code {

  font-family: monospace;

  font-weight: 600;

  color: #1e3a5f;

  background: #f1f5f9;

  padding: 1px 5px;

  border-radius: 4px;

  font-size: 0.76rem;

}



.btn-copy-tiny {

  background: #f1f5f9;

  border: 1px solid #cbd5e1;

  color: #475569;

  font-size: 0.68rem;

  padding: 1px 5px;

  border-radius: 3px;

  cursor: pointer;

  line-height: 1.2;

}



.btn-copy-tiny:hover {

  background: #005baa;

  color: #ffffff;

  border-color: #005baa;

}



.vnpay-hint-text {

  font-size: 0.68rem;

  color: #94a3b8;

  line-height: 1.2;

  margin-top: 1px;

}



.vnpay-action-btns {

  display: flex;

  align-items: center;

  gap: 5px;

  margin-top: 5px;

}



.btn-vnpay-confirm {

  display: inline-flex;

  align-items: center;

  justify-content: center;

  gap: 4px;

  background: #005baa;

  color: #ffffff;

  border: 1px solid #005baa;

  border-radius: 4px;

  padding: 4px 12px;

  font-size: 0.74rem;

  font-weight: 600;







  cursor: pointer;

  transition: all 0.15s;

}



.btn-vnpay-confirm:hover:not(:disabled) {

  background: #004c8f;

}



.btn-vnpay-confirm:disabled {

  opacity: 0.6;

  cursor: not-allowed;

}



.vnpay-success-chip {

  display: inline-flex;

  align-items: center;

  gap: 4px;

  background: #e6f4ea;

  color: #137333;

  padding: 4px 8px;

  border-radius: 4px;

  font-size: 0.74rem;

  font-weight: 600;







  margin-top: 4px;

}



.vnpay-paid-actions {

  display: flex;

  align-items: center;

  gap: 6px;

  flex-wrap: wrap;

  margin-top: 4px;

}



.btn-vnpay-print-direct {

  display: inline-flex;

  align-items: center;

  gap: 4px;

  background: #059669;

  color: #ffffff;

  border: 1px solid #059669;

  border-radius: 4px;

  padding: 4px 10px;

  font-size: 0.74rem;

  font-weight: 600;







  cursor: pointer;

  transition: all 0.15s;

}



.btn-vnpay-print-direct:hover:not(:disabled) {

  background: #047857;

}



.btn-vnpay-print-direct:disabled {

  opacity: 0.6;

  cursor: not-allowed;

}



.vnpay-mini-spin {

  width: 10px;

  height: 10px;

  border: 2px solid #cbd5e1;

  border-top-color: #005baa;

  border-radius: 50%;

  animation: spin 0.8s linear infinite;

}



.pos-form-input, .pos-form-textarea, .pos-form-select {

  width: 100%;

  padding: 8px 12px;

  border: 1px solid #d6d0c3;

  border-radius: 6px;

  font-size: 0.88rem;

  outline: none;

  box-sizing: border-box;

  background-color: #ffffff;

  color: #333;

  transition: border-color 0.2s;

}



.pos-form-input:focus, .pos-form-textarea:focus, .pos-form-select:focus {

  border-color: #496883;

}



.input-money {

  font-weight: 600;

  font-size: 0.95rem;

}



.input-error {

  border-color: #e53935 !important;

}



.error-text {

  font-size: 0.76rem;

  color: #e53935;

}



.text-danger {

  color: #e53935;

}



/* Ghim 2 Nút hành động cuối ở đáy cột phải, luôn hiển thị trên màn hình */

.pos-col-right {

  display: flex;

  flex-direction: column;

  gap: 14px;

  min-width: 0;

  position: sticky;

  top: 10px;

}



.checkout-actions-bottom {

  position: sticky;

  bottom: 0;

  z-index: 30;

  display: flex;

  gap: 10px;

  margin-top: 14px;

  padding: 10px 0 2px 0;

  background: #ffffff;

  border-top: 1px solid #e9e5db;

  box-shadow: 0 -4px 10px rgba(0, 0, 0, 0.05);

}



.btn-cancel-bill {

  flex: 1;

  height: 40px;

  background-color: #f1f5f9;

  color: #647074;

  border: 1px solid #d6d0c3;

  border-radius: 6px;

  font-size: 0.86rem;

  font-weight: 600;

  cursor: pointer;

  transition: all 0.2s;

}



.btn-cancel-bill:hover {

  background-color: #e2e8f0;

  color: #334155;

}



.btn-submit-checkout {

  flex: 1.8;

  height: 40px;

  background-color: #496883;

  color: #ffffff;

  border: none;

  border-radius: 6px;

  font-size: 0.92rem;

  font-weight: 700;

  cursor: pointer;

  transition: all 0.2s;

  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);

  display: inline-flex;

  align-items: center;

  justify-content: center;

  gap: 6px;

}



.btn-submit-checkout:hover:not(:disabled) {

  background-color: #385166;

  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.35);

}



.btn-submit-checkout:disabled {

  opacity: 0.45;

  cursor: not-allowed;

  box-shadow: none;

}



/* ==================== POPUP MODAL ==================== */

.pos-modal-backdrop {

  position: fixed;

  inset: 0;

  background-color: rgba(0, 0, 0, 0.45);

  display: flex;

  align-items: center;

  justify-content: center;

  z-index: 9999;

}



.pos-modal-card {

  background: #ffffff;

  width: 90%;

  max-width: 640px;

  border-radius: 8px;

  border: 1px solid #e9e5db;

  padding: 20px;

  max-height: 82vh;

  display: flex;

  flex-direction: column;

}



.quick-customer-card {

  max-width: 480px;

}



.modal-top {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 12px;

}



.modal-top h3 {

  margin: 0;

  font-size: 1.05rem;

  color: #496883;

  font-weight: 600;

}



.btn-modal-close {

  background: none;

  border: none;

  font-size: 1.2rem;

  cursor: pointer;

  color: #647074;

}



.modal-search-box {

  margin-bottom: 14px;

}



.modal-product-list {

  overflow-y: auto;

  display: flex;

  flex-direction: column;

  gap: 10px;

  max-height: 55vh;

}



.modal-product-card {

  display: flex;

  align-items: center;

  gap: 12px;

  padding: 10px 12px;

  border: 1px solid #e9e5db;

  border-radius: 6px;

  cursor: pointer;

  transition: all 0.2s;

}



.modal-product-card:hover:not(.card-out-of-stock) {

  border-color: #496883;

  background-color: #f7f5ef;

}



.card-out-of-stock {

  opacity: 0.55;

  background-color: #f9f9f9;

  cursor: not-allowed;

}



.modal-p-img {

  width: 48px;

  height: 48px;

  border-radius: 4px;

  object-fit: cover;

}



.modal-p-details {

  flex: 1;

}



.modal-p-name {

  font-size: 0.9rem;

  font-weight: 600;







  color: #333;

}



.modal-p-meta {

  font-size: 0.78rem;

  color: #8a9292;

  margin: 2px 0 4px;

}



.modal-p-price-row {

  display: flex;

  gap: 14px;

}



.modal-p-img-wrapper {

  position: relative;

  width: 48px;

  height: 48px;

  flex-shrink: 0;

}



.modal-badge-discount {

  position: absolute;

  top: -4px;

  left: -4px;

  background: #dc2626;

  color: #ffffff;

  font-size: 0.65rem;

  font-weight: 700;

  padding: 1px 4px;

  border-radius: 3px;

  line-height: 1.2;

  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);

  pointer-events: none;

}



.modal-p-name-row {

  display: flex;

  align-items: center;

  gap: 8px;

  flex-wrap: wrap;

}



.modal-campaign-tag {

  background: #fef2f2;

  color: #dc2626;

  border: 1px solid #fecaca;

  font-size: 0.72rem;

  font-weight: 600;

  padding: 1px 6px;

  border-radius: 4px;

  white-space: nowrap;

}



.modal-p-price-wrap {

  display: flex;

  align-items: baseline;

  gap: 6px;

}



.modal-p-price {

  font-weight: 600;

  color: #c94a29;

  font-size: 0.88rem;

}



.modal-p-price.sale {

  color: #dc2626;

  font-size: 0.94rem;

  font-weight: 700;

}



.modal-p-old-price {

  font-size: 0.78rem;

  color: #94a3b8;

  text-decoration: line-through;

}



.modal-p-discount-pct {

  background: #fef2f2;

  color: #dc2626;

  border: 1px solid #fecaca;

  font-size: 0.68rem;

  font-weight: 700;

  padding: 0 4px;

  border-radius: 3px;

  line-height: 1.3;

}



.cart-product-name-wrap {

  display: flex;

  align-items: center;

  gap: 6px;

  flex-wrap: wrap;

}



.cart-discount-badge {

  display: inline-block;

  background: #fef2f2;

  color: #dc2626;

  border: 1px solid #fecaca;

  font-size: 0.72rem;

  font-weight: 600;

  padding: 1px 5px;

  border-radius: 3px;

}



.cart-price-col {

  display: flex !important;

  flex-direction: column !important;

  align-items: center !important;

  justify-content: center !important;

  text-align: center !important;

  gap: 1px !important;

}



.cart-price-sale {

  color: #dc2626;

  font-weight: 700;

  text-align: center !important;

}





.cart-price-old {

  font-size: 0.75rem;

  color: #94a3b8;

  text-decoration: line-through;

  text-align: center !important;

}





.modal-p-stock {

  font-size: 0.78rem;

  color: #2e7d32;

  font-weight: 600;







}



.stock-zero {

  color: #c94a29 !important;

}



.btn-mini-add {

  background-color: #eaf1f4;

  color: #496883;

  border: 1px solid #496883;

  padding: 6px 14px;

  border-radius: 4px;



  font-weight: 600;







  cursor: pointer;

}



.btn-mini-add:disabled {

  opacity: 0.5;

  cursor: not-allowed;

  border-color: #ccc;

  color: #999;

  background-color: #f0f0f0;

}



.modal-body-form {

  display: flex;

  flex-direction: column;

  gap: 10px;

}



.modal-actions-bottom {

  display: flex;

  justify-content: flex-end;

  gap: 10px;

  margin-top: 16px;

}



.btn-cancel-modal {

  padding: 8px 16px;

  background-color: #e9e5db;

  color: #647074;

  border: none;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  cursor: pointer;

}



.btn-submit-modal {

  padding: 8px 16px;

  background-color: #496883;

  color: #ffffff;

  border: none;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  cursor: pointer;

}



.text-center { text-align: center; }

.text-right { text-align: right; }

.text-muted { color: #8a9292; }



/* TOAST POPUP NOTIFICATION */

.toast-popup {

  position: fixed;

  top: 24px;

  right: 28px;

  background-color: #36536b;

  color: #ffffff;

  padding: 10px 18px;

  border-radius: 8px;

  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);

  display: flex;

  align-items: center;

  gap: 10px;

  font-size: 0.88rem;

  font-weight: 600;







  z-index: 99999;

  border: 1px solid #4a6880;

}



.toast-popup.warning {

  background-color: #d97706;

  border-color: #b45309;

}



.toast-fade-enter-active,

.toast-fade-leave-active {

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

}



.toast-fade-enter-from,

.toast-fade-leave-to {

  opacity: 0;

  transform: translateY(-12px);

}



/* NÚT ĐÓNG TAB HÓA ĐƠN */

.btn-close-tab {

  background: transparent;

  border: none;

  color: #a0aab0;

  font-size: 0.8rem;

  padding: 0 3px;

  margin-left: 2px;

  cursor: pointer;

  border-radius: 50%;

  display: inline-flex;

  align-items: center;

  justify-content: center;

  transition: all 0.15s ease;

}



.btn-close-tab:hover {

  background: rgba(201, 74, 41, 0.15);

  color: #c94a29;

}



.tab-loading-indicator {



  color: #8a9292;

  font-style: italic;

  padding: 6px 10px;

}



/* MODAL XÁC NHẬN HỦY */

.confirm-modal-card {

  max-width: 420px;

  padding: 22px;

}



.confirm-modal-header {

  display: flex;

  align-items: center;

  gap: 8px;

  margin-bottom: 8px;

}



.confirm-modal-header h4 {

  margin: 0;

  font-size: 1.05rem;

  color: #1e3a5f;

  font-weight: 600;

}



.confirm-icon {

  font-size: 1.3rem;

}



.confirm-modal-desc {

  font-size: 0.88rem;

  color: #556066;

  line-height: 1.45;

  margin: 0 0 18px;

}



.confirm-modal-actions {

  display: flex;

  justify-content: flex-end;

  gap: 10px;

}



.btn-modal-back {

  background: #f0ebe1;

  color: #5c686d;

  border: none;

  padding: 8px 16px;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  cursor: pointer;

}



.btn-modal-danger {

  background: #c94a29;

  color: #ffffff;

  border: none;

  padding: 8px 16px;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  cursor: pointer;

  transition: all 0.2s;

}



.btn-modal-danger:hover {

  background: #b03a1a;

}



/* CUSTOMER DROPDOWN EMPTY */

.customer-dropdown-empty {

  padding: 12px 14px;

  font-size: 0.83rem;

  color: #8a9292;

  display: flex;

  align-items: center;

  justify-content: space-between;

}



.btn-quick-create-cust {

  background: #496883;

  color: #ffffff;

  border: none;

  border-radius: 4px;

  padding: 4px 10px;

  font-size: 0.78rem;

  font-weight: 600;







  cursor: pointer;

}



/* MODAL PRODUCT EMPTY & LOADING */

.modal-loading-box, .empty-product-box {

  padding: 32px 16px;

  text-align: center;

  display: flex;

  flex-direction: column;

  align-items: center;

  justify-content: center;

}



.btn-create-product-link {

  display: inline-block;

  background-color: #496883;

  color: #ffffff;

  text-decoration: none;

  padding: 8px 16px;

  border-radius: 6px;

  font-size: 0.85rem;

  font-weight: 600;







  margin-top: 6px;

  transition: background-color 0.2s;

}



.btn-create-product-link:hover {

  background-color: #38536b;

}



.spinner-dot {

  width: 24px;

  height: 24px;

  border: 3px solid #e0e5ea;

  border-top-color: #496883;

  border-radius: 50%;

  animation: spin 0.8s linear infinite;

  margin-bottom: 10px;

}



@keyframes spin {

  to { transform: rotate(360deg); }

}



@media (max-width: 1024px) {

  .pos-grid-container {

    grid-template-columns: 1fr;

  }

}



/* ==================== ĐỊA CHỈ & HÀNH CHÍNH TRONG POS ==================== */

.pos-form-grid-2 {

  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 10px;

}



.selected-customer-pill-wrap {

  display: flex;

  flex-direction: column;

}



.cust-address-preview {

  margin-top: 6px;

  font-size: 0.8rem;

  color: #647074;

  display: flex;

  align-items: flex-start;

  gap: 4px;

  line-height: 1.35;

  background: #f7f5ef;

  padding: 6px 10px;

  border-radius: 6px;

  border: 1px dashed #d6d0c3;

}



.addr-pin-icon {

  flex-shrink: 0;

  font-size: 0.85rem;

}



.addr-preview-text {

  flex: 1;

  word-break: break-word;

}



.cust-addr-quick-actions {

  display: flex;

  gap: 8px;

  margin-top: 8px;

}



.btn-sub-addr-action {

  background: #ffffff;

  color: #496883;

  border: 1px solid #c9d6df;

  border-radius: 4px;

  padding: 4px 10px;

  font-size: 0.78rem;

  font-weight: 600;







  cursor: pointer;

  transition: all 0.2s;

}



.btn-sub-addr-action:hover {

  background: #496883;

  color: #ffffff;

  border-color: #496883;

}



.label-with-action-row {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 4px;

}



.addr-link-btns {

  display: flex;

  align-items: center;

  gap: 6px;

}



.addr-link-divider {

  font-size: 0.75rem;

  color: #c4c4c4;

}



.link-btn-addr {

  background: none;

  border: none;

  color: #496883;

  font-size: 0.78rem;

  font-weight: 600;







  text-decoration: underline;

  cursor: pointer;

  padding: 0;

}



.link-btn-addr:hover {

  color: #1e3a5f;

}



.addr-item-card {

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 10px;

  padding: 10px 12px;

  border: 1px solid #e9e5db;

  border-radius: 6px;

  cursor: pointer;

  margin-bottom: 8px;

  transition: all 0.2s;

}



.addr-item-card:hover {

  background-color: #f7f5ef;

  border-color: #496883;

}



.addr-item-card.addr-active {

  background-color: #eaf1f4;

  border-color: #496883;

}



.addr-item-content {

  flex: 1;

}



.addr-item-text {

  font-size: 0.85rem;

  color: #3d4a50;

  line-height: 1.4;

}



.addr-badge-default {

  display: inline-block;

  background-color: #e6f4ea;

  color: #137333;

  font-size: 0.72rem;

  font-weight: 600;







  padding: 2px 6px;

  border-radius: 4px;

  margin-top: 4px;

}



.btn-pick-addr {

  background-color: #496883;

  color: #ffffff;

  border: none;

  padding: 5px 12px;

  border-radius: 4px;

  font-size: 0.78rem;

  font-weight: 600;







  cursor: pointer;

}



/* ==================== BẢNG BIẾN THỂ SẢN PHẨM & BỘ LỌC ĐẦY ĐỦ ==================== */

.pos-modal-card.product-table-modal {

  width: 95vw;

  max-width: 1100px;

  max-height: 88vh;

  padding: 16px 20px 14px;

  display: flex;

  flex-direction: column;

  border-radius: 10px;

  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22);

  background: #ffffff;

}



.modal-title-group {

  display: flex;

  align-items: center;

  gap: 10px;

}



.modal-title-text {

  margin: 0;

  font-size: 1.12rem;

  color: #1e293b;

  font-weight: 700;

}



.modal-badge-count {

  background: #eaf2f6;

  color: #496883;

  font-size: 0.78rem;

  font-weight: 600;

  padding: 2px 8px;

  border-radius: 12px;

  border: 1px solid #d0e0ec;

}



.product-modal-filter-bar {

  background: #f8fafc;

  border: 1px solid #e2e8f0;

  border-radius: 8px;

  padding: 10px 12px;

  margin-bottom: 10px;

  display: flex;

  flex-direction: column;

  gap: 8px;

  position: relative;

  z-index: 50;

}



.product-modal-filter-bar :deep(.select2-wrapper) {

  width: 100%;

}



.product-modal-filter-bar :deep(.select2-trigger) {

  height: 36px;

  min-height: 36px;

  border: 1px solid #cbd5e1;

  border-radius: 6px;

  background-color: #ffffff;

  padding: 0 10px;

  font-size: 0.82rem;

  color: #1e293b;

  box-sizing: border-box;

}



.product-modal-filter-bar :deep(.select2-trigger:hover:not(.disabled)) {

  border-color: #496883;

  background-color: #ffffff;

}



.product-modal-filter-bar :deep(.select2-trigger.trigger-focus) {

  border-color: #496883;

  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);

  background-color: #ffffff;

}



.product-modal-filter-bar :deep(.select2-display-text) {

  font-size: 0.82rem;

  font-weight: 500;

  color: #1e293b;

}



.product-modal-filter-bar :deep(.select2-dropdown) {

  border-radius: 6px;

  border: 1px solid #cbd5e1;

  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);

  z-index: 99999;

}



.product-modal-filter-bar :deep(.select2-option-item) {

  font-size: 0.82rem;

  padding: 7px 10px;

}



.product-modal-filter-bar :deep(.select2-search-input) {

  height: 32px;

  font-size: 0.82rem;

  border-radius: 5px;

}



.sort-select2-wrap {

  width: 205px;

}



.filter-bar-row {

  display: flex;

  align-items: center;

  gap: 10px;

}



.filter-bar-main {

  display: flex;

  align-items: center;

  gap: 10px;

  position: relative;

  z-index: 10;

}



.filter-input-search-wrap {

  flex: 1;

  position: relative;

  display: flex;

  align-items: center;

}



.filter-search-input {

  width: 100%;

  height: 36px;

  padding: 0 30px 0 12px;

  border: 1px solid #cbd5e1;

  border-radius: 6px;

  font-size: 0.86rem;

  background: #ffffff;

  color: #1e293b;

  outline: none;

  transition: border-color 0.15s ease;

}



.filter-search-input:focus {

  border-color: #496883;

  box-shadow: 0 0 0 2px rgba(73, 104, 131, 0.12);

}



.btn-clear-input {

  position: absolute;

  right: 8px;

  background: none;

  border: none;

  color: #94a3b8;

  font-size: 0.85rem;

  cursor: pointer;

  padding: 4px;

}



.filter-group-inline {

  display: flex;

  align-items: center;

  gap: 6px;

}



.filter-inline-label {

  font-size: 0.82rem;

  font-weight: 600;

  color: #475569;

  white-space: nowrap;

}



.filter-inline-select {

  height: 36px;

  padding: 0 24px 0 8px;

  border: 1px solid #cbd5e1;

  border-radius: 6px;

  font-size: 0.82rem;

  background: #ffffff;

  color: #1e293b;

  outline: none;

  cursor: pointer;

}



.btn-reset-filters {

  height: 36px;

  padding: 0 14px;

  background: #ffffff;

  border: 1px solid #cbd5e1;

  border-radius: 6px;

  font-size: 0.82rem;

  font-weight: 600;

  color: #475569;

  cursor: pointer;

  white-space: nowrap;

  transition: all 0.15s ease;

}



.btn-reset-filters:hover {

  background: #f1f5f9;

  color: #0f172a;

  border-color: #94a3b8;

}



.filter-bar-facets {

  display: grid;

  grid-template-columns: repeat(3, 1fr);

  gap: 10px;

  position: relative;

  z-index: 5;

}



.filter-facet-item {

  display: flex;

  flex-direction: column;

  gap: 2px;

}



.facet-label {

  font-size: 0.72rem;

  font-weight: 600;

  color: #64748b;

  text-transform: uppercase;

  letter-spacing: 0.3px;

}



.facet-select {

  height: 32px;

  padding: 0 8px;

  border: 1px solid #cbd5e1;

  border-radius: 6px;

  font-size: 0.82rem;

  background: #ffffff;

  color: #1e293b;

  outline: none;

  cursor: pointer;

}



.facet-select:focus {

  border-color: #496883;

}



.product-modal-table-container {

  flex: 1;

  min-height: 260px;

  overflow: hidden;

  display: flex;

  flex-direction: column;

  border: 1px solid #e2e8f0;

  border-radius: 8px;

  background: #ffffff;

}



.table-scroll-wrap {

  flex: 1;

  overflow-y: auto;

  max-height: 48vh;

}



.variant-data-table {

  width: 100%;

  border-collapse: collapse;

  font-size: 0.85rem;

  text-align: left;

}



.variant-data-table thead th {

  position: sticky;

  top: 0;

  background: #f8fafc;

  color: #475569;

  font-size: 0.74rem;

  font-weight: 700;

  letter-spacing: 0.3px;

  padding: 9px 10px;

  border-bottom: 2px solid #e2e8f0;

  z-index: 2;

  white-space: nowrap;

}



.variant-tr {

  border-bottom: 1px solid #f1f5f9;

  cursor: pointer;

  transition: background-color 0.15s ease;

}



.variant-tr:hover:not(.tr-out-of-stock) {

  background-color: #f8fafc;

}



.variant-tr.tr-out-of-stock {

  opacity: 0.55;

  background-color: #fafafa;

  cursor: not-allowed;

}



.variant-tr td {

  padding: 8px 10px;

  vertical-align: middle;

}



.table-thumb-wrap {

  position: relative;

  width: 42px;

  height: 42px;

  margin: 0 auto;

}



.table-thumb-img {

  width: 42px;

  height: 42px;

  border-radius: 5px;

  object-fit: cover;

  border: 1px solid #e2e8f0;

}



.thumb-badge-sale {

  position: absolute;

  top: -3px;

  left: -3px;

  background: #dc2626;

  color: #ffffff;

  font-size: 0.6rem;

  font-weight: 700;

  padding: 1px 3px;

  border-radius: 3px;

  line-height: 1.2;

}



.td-code {

  white-space: nowrap;

}



.code-primary {

  display: block;

  font-weight: 700;

  color: #1e293b;

  font-size: 0.84rem;

}



.code-sub {

  display: block;

  font-size: 0.72rem;

  color: #94a3b8;

}



.td-name {

  max-width: 250px;

}



.name-main {

  font-weight: 600;

  color: #0f172a;

  line-height: 1.3;

}



.campaign-pill {

  display: inline-block;

  margin-top: 2px;

  background: #fef2f2;

  color: #dc2626;

  border: 1px solid #fecaca;

  font-size: 0.68rem;

  font-weight: 600;

  padding: 1px 5px;

  border-radius: 3px;

  white-space: nowrap;

}



.color-wrap {

  display: inline-flex;

  align-items: center;

  gap: 5px;

}



.color-dot {

  width: 13px;

  height: 13px;

  border-radius: 50%;

  border: 1px solid #cbd5e1;

  display: inline-block;

}



.color-text {

  font-size: 0.82rem;

  color: #334155;

}



.size-badge {

  display: inline-block;

  background: #f1f5f9;

  color: #334155;

  font-weight: 700;

  font-size: 0.78rem;

  padding: 2px 7px;

  border-radius: 4px;

  border: 1px solid #cbd5e1;

}



.price-discounted {

  font-weight: 700;

  color: #dc2626;

  font-size: 0.9rem;

}



.price-strikethrough {

  font-size: 0.74rem;

  color: #94a3b8;

  text-decoration: line-through;

}



.price-regular {

  font-weight: 700;

  color: #1e293b;

  font-size: 0.88rem;

}





.stock-badge {

  display: inline-block;

  padding: 3px 10px;

  border-radius: 12px;

  font-weight: 700;

  font-size: 0.85rem;

  min-width: 38px;

  text-align: center;

}



.stock-badge.in-stock {

  background-color: #eaf5ed;

  color: #247541;

  border: 1px solid #bce1c7;

}



.stock-badge.out-of-stock {

  background-color: #f7edeb;

  color: #b34a3b;

  border: 1px solid #ecc9c3;

}

.stock-pill {

  display: inline-block;

  padding: 2px 7px;

  border-radius: 12px;

  font-size: 0.74rem;

  font-weight: 600;

}



.stock-pill.in-stock {

  background: #ecfdf5;

  color: #059669;

  border: 1px solid #a7f3d0;

}



.stock-pill.out-of-stock {

  background: #fef2f2;

  color: #dc2626;

  border: 1px solid #fecaca;

}



.btn-action-add {

  background-color: #eaf2f6;

  color: #496883;

  border: 1px solid #496883;

  padding: 5px 12px;

  border-radius: 5px;

  font-weight: 700;

  font-size: 0.8rem;

  cursor: pointer;

  transition: all 0.15s ease;

  white-space: nowrap;

}



.btn-action-add:hover:not(.btn-disabled) {

  background-color: #496883;

  color: #ffffff;

}



.btn-action-add.btn-disabled {

  opacity: 0.45;

  border-color: #cbd5e1;

  color: #94a3b8;

  background-color: #f1f5f9;

  cursor: not-allowed;

}



/* Đồng bộ giao diện phân trang chuẩn hệ thống (DanhSachHoaDon, ThongKe) */

.product-modal-footer {

  display: flex;

  align-items: center;

  justify-content: space-between;

  padding-top: 14px;

  margin-top: 8px;

  border-top: 1px solid #eef2f5;

  gap: 12px;

  flex-wrap: wrap;

}



.product-modal-footer .pagination-section {

  display: flex;

  align-items: center;

  justify-content: space-between;

  flex: 1;

  padding-top: 0;

  margin-top: 0;

  gap: 16px;

}



.product-modal-footer .pagination-info {

  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;

  font-size: 0.85rem;

  font-weight: 500;

  color: #555e65;

  line-height: 1.5;

}



.product-modal-footer .pagination-buttons {

  display: flex;

  align-items: center;

  gap: 5px;

}



.product-modal-footer .btn-pager {

  width: 32px;

  height: 32px;

  border: 1px solid #ded5c7;

  background: #ffffff;

  color: #555e65;

  border-radius: 6px;

  cursor: pointer;

  font-size: 0.84rem;

  font-weight: 600;

  display: inline-flex;

  align-items: center;

  justify-content: center;

  transition: all 0.2s;

}



.product-modal-footer .btn-pager.active {

  background: #36536b;

  border-color: #36536b;

  color: #ffffff;

}



.product-modal-footer .btn-pager:hover:not(:disabled):not(.active) {

  background: #f4ede4;

}



.product-modal-footer .btn-pager:disabled {

  opacity: 0.35;

  cursor: not-allowed;

}



.product-modal-footer .pg-dots {

  display: inline-flex;

  align-items: center;

  justify-content: center;

  padding: 0 4px;

  color: #8c979d;

  font-size: 0.85rem;

}



.modal-footer-right-actions {

  display: flex;

  align-items: center;

  gap: 10px;

}



.page-size-selector-wrap {

  display: flex;

  align-items: center;

  gap: 6px;

}



.page-size-selector-wrap .page-size-label {

  font-size: 0.82rem;

  color: #64748b;

  font-weight: 500;

}



.page-size-selector-wrap .page-size-select {

  height: 32px;

  padding: 0 8px;

  border: 1px solid #ded5c7;

  border-radius: 6px;

  font-size: 0.82rem;

  background: #ffffff;

  color: #334155;

  cursor: pointer;

  outline: none;

}



.page-size-selector-wrap .page-size-select:focus {

  border-color: #36536b;

}



.btn-pager-close {

  height: 32px;

  padding: 0 16px;

  background: #ffffff;

  border: 1px solid #ded5c7;

  border-radius: 6px;

  font-size: 0.84rem;

  font-weight: 600;

  color: #555e65;

  cursor: pointer;

  transition: all 0.2s;

}



.btn-pager-close:hover {

  background: #f4ede4;

  border-color: #c9bea8;

  color: #1e293b;

}



.empty-product-table-box {

  padding: 36px 20px;

  text-align: center;

}



.empty-title {

  font-weight: 700;

  color: #496883;

  font-size: 0.98rem;

  margin-bottom: 4px;

}



.empty-desc {

  color: #64748b;

  font-size: 0.84rem;

  margin: 0 0 12px;

}



.btn-clear-empty-filter {

  padding: 5px 14px;

  background: #496883;

  color: #ffffff;

  border: none;

  border-radius: 5px;

  font-size: 0.82rem;

  font-weight: 600;

  cursor: pointer;

}



@media (max-width: 900px) {

  .filter-bar-facets {

    grid-template-columns: repeat(2, 1fr);

  }

}





/* Sổ địa chỉ & Nhóm thao tác khách hàng */

.cust-actions-group {

  display: flex;

  align-items: center;

  gap: 6px;

}

.btn-address-book {

  padding: 4px 10px;

  background-color: #f1f5f9;

  color: #334155;

  border: 1px solid #cbd5e1;

  border-radius: 6px;

  font-size: 0.78rem;

  font-weight: 600;

  cursor: pointer;

  transition: all 0.2s ease;

}

.btn-address-book:hover {

  background-color: #e2e8f0;

  color: #0f172a;

}



/* Banner gợi ý voucher mua thêm */

.voucher-suggestion-banner {

  margin-top: 8px;

  background: #fffbeb;

  border: 1px dashed #f59e0b;

  border-radius: 8px;

  padding: 8px 12px;

}

.v-sugg-content {

  display: flex;

  align-items: center;

  gap: 8px;

  font-size: 0.8rem;

  color: #92400e;

  line-height: 1.4;

}

.v-sugg-icon {

  font-size: 1rem;

}



/* Modal Xác nhận đơn hàng */

.confirm-order-modal {

  max-width: 680px;

  width: 95%;

}

.confirm-modal-body {

  padding: 16px 20px;

  max-height: 65vh;

  overflow-y: auto;

}

.confirm-info-grid {

  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 10px 16px;

  background: #f8fafc;

  padding: 12px 16px;

  border-radius: 8px;

  border: 1px solid #e2e8f0;

  margin-bottom: 16px;

}

.confirm-info-col {

  display: flex;

  flex-direction: column;

  gap: 2px;

}

.confirm-info-col .c-lbl {

  font-size: 0.75rem;

  color: #64748b;

  font-weight: 600;

}

.confirm-info-col .c-val {

  font-size: 0.88rem;

  color: #1e293b;

}

.badge-delivery-tag {

  color: #0369a1;

  font-weight: 600;

}

.confirm-items-table-wrap {

  border: 1px solid #e2e8f0;

  border-radius: 8px;

  overflow: hidden;

  margin-bottom: 16px;

}

.confirm-table {

  width: 100%;

  border-collapse: collapse;

  font-size: 0.82rem;

}

.confirm-table th {

  background: #f1f5f9;

  padding: 8px 12px;

  font-weight: 600;

  color: #475569;

}

.confirm-table td {

  padding: 8px 12px;

  border-top: 1px solid #e2e8f0;

  color: #334155;

}

.confirm-prod-cell {

  display: flex;

  align-items: center;

  gap: 10px;

}

.confirm-prod-thumb {

  width: 36px;

  height: 36px;

  object-fit: cover;

  border-radius: 4px;

  border: 1px solid #e2e8f0;

}

.confirm-prod-title {

  font-weight: 600;

  color: #0f172a;

}

.confirm-prod-sub {

  font-size: 0.75rem;

  color: #64748b;

}

.confirm-cost-breakdown {

  display: flex;

  flex-direction: column;

  gap: 6px;

  background: #fafafa;

  padding: 12px 16px;

  border-radius: 8px;

  border: 1px solid #e5e7eb;

}

.cost-row {

  display: flex;

  justify-content: space-between;

  font-size: 0.85rem;

  color: #4b5563;

}

.cost-row.discount-row {

  color: #059669;

}

.cost-row.total-highlight {

  border-top: 1.5px dashed #cbd5e1;

  padding-top: 8px;

  margin-top: 4px;

  font-size: 1rem;

  color: #0f172a;

}

.total-big-val {

  font-size: 1.15rem;

  font-weight: 800;

  color: #b45309;

}




/* ==================== MODAL MÃ VIETQR (KHUNG NGOÀI TO RỘNG 880PX - 2 CỘT CHUYÊN NGHIỆP, KHÔNG BỊ BÓ HẸP) ==================== */
.pos-vietqr-standalone-dialog {
  width: 980px;
  max-width: 96vw;
  max-height: 94vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 25px 70px rgba(0, 0, 0, 0.35);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  padding: 0;
  border: 1px solid #cbd5e1;
}

.vietqr-modal-header-clean {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px 14px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.header-left-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.vietqr-modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #004a99;
}

.vietqr-subtitle {
  font-size: 0.82rem;
  color: #64748b;
}

.vietqr-code-pill {
  background: #e2e8f0;
  color: #1e293b;
  padding: 5px 14px;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 700;
  margin-right: 32px;
}

.vietqr-modal-body-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 28px;
  padding: 24px 28px;
  overflow-y: auto;
  max-height: calc(94vh - 75px);
  align-items: start;
}

/* CỘT TRÁI: MÃ QR */
.vietqr-col-left {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.vietqr-qr-frame {
  position: relative;
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  padding: 10px;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.vietqr-big-code {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 10px;
}

.vietqr-overlay-paid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(16, 185, 129, 0.92);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #ffffff;
}

.overlay-check-text {
  font-size: 1.15rem;
  font-weight: 800;
  letter-spacing: 0.5px;
  text-align: center;
  padding: 0 16px;
}

.vietqr-scan-guide {
  margin-top: 10px;
  font-size: 0.84rem;
  color: #64748b;
  text-align: center;
  font-weight: 500;
}

/* CỘT PHẢI: CHI TIẾT & XÁC NHẬN */
.vietqr-col-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.vietqr-details-table {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vqr-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
}

.vqr-label {
  color: #64748b;
  font-weight: 500;
}

.vqr-val-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.vqr-val {
  color: #1e293b;
}

.btn-vqr-copy-mini {
  padding: 2px 8px;
  font-size: 0.72rem;
  background: #e2e8f0;
  color: #004a99;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.15s ease;
}

.btn-vqr-copy-mini:hover {
  background: #004a99;
  color: #ffffff;
}

.highlight-amount-row {
  border-top: 1px dashed #cbd5e1;
  border-bottom: 1px dashed #cbd5e1;
  padding: 8px 0;
  margin: 4px 0;
}

.text-amount-large {
  font-size: 1.28rem !important;
  color: #dc2626 !important;
}

.vietqr-invoice-summary {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sum-line {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.sum-lbl {
  color: #475569;
}

.sum-transfer {
  border-top: 1px solid #e2e8f0;
  padding-top: 4px;
  margin-top: 2px;
}

.sum-line-sub {
  font-size: 0.76rem;
  color: #64748b;
  margin-top: 2px;
}

.vietqr-staff-notice {
  font-size: 0.78rem;
  color: #92400e;
  background: #fef3c7;
  border-left: 3px solid #f59e0b;
  padding: 8px 12px;
  border-radius: 4px;
  line-height: 1.4;
}

.vietqr-verify-box {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 8px 12px;
  border-radius: 6px;
}

.vietqr-verify-box input[type="checkbox"] {
  width: 17px;
  height: 17px;
  cursor: pointer;
}

.verify-txt {
  font-size: 0.85rem;
  color: #1e40af;
  font-weight: 500;
}

.vietqr-right-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.btn-modal-back {
  flex: 1;
  padding: 10px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-modal-back:hover {
  background: #e2e8f0;
}

.btn-modal-confirm-pay {
  flex: 2;
  padding: 10px 16px;
  background: #004a99;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-modal-confirm-pay:hover:not(:disabled) {
  background: #003366;
}

.btn-modal-confirm-pay:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chuyen-khoan-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-top: 8px;
}

.ck-status-info {
  display: flex;
  align-items: center;
}

.ck-paid-badge {
  color: #15803d;
  background: #dcfce7;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 700;
}

.ck-waiting-badge {
  color: #475569;
  font-size: 0.84rem;
}

.btn-open-vietqr-again {
  padding: 6px 14px;
  background: #004a99;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-open-vietqr-again:hover {
  background: #003366;
}

@media (max-width: 880px) {
  .vietqr-modal-body-grid {
    grid-template-columns: 1fr;
  }
  .pos-vietqr-standalone-dialog {
    width: 95vw;
  }
}

/* ==================== CHỨNG MINH NHÚNG API GHN EXPRESS ==================== */
.shipping-fee-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.ghn-api-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #fff7ed;
  color: #ea580c;
  border: 1px solid #fed7aa;
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.ghn-dot-pulse {
  color: #16a34a;
  font-size: 0.75rem;
  animation: ghnPulse 1.8s infinite;
}

@keyframes ghnPulse {
  0% { opacity: 1; }
  50% { opacity: 0.35; }
  100% { opacity: 1; }
}

.shipping-fee-input-group {
  display: flex;
  gap: 6px;
  align-items: center;
}

.shipping-input-bold {
  flex: 1;
  font-weight: 700;
  color: #0f172a;
}





.ck-paid-simple-note {
  margin-top: 6px;
  padding: 6px 12px;
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  font-size: 0.88rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}
.cursor-pointer {
  cursor: pointer;
}
.font-mono-amount {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
</style>
