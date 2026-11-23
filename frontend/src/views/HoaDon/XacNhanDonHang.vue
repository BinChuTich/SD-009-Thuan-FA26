<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api.js'
import RefundOrderModal from '@/components/RefundOrderModal.vue'
import { getCurrentUser } from '@/utils/auth.js'

const route = useRoute()
const router = useRouter()
const maHoaDon = route.params.maHoaDon
const hoaDon = ref(null)
const loading = ref(false)
const saving = ref(false)
const paying = ref(false)
const checkingStock = ref(false)
const refundingOrder = ref(false)
const showRefundModal = ref(false)
const errorMessage = ref('')
const isOrderOutOfStock = ref(false)
const stockCheckItems = ref([])

const loadHoaDon = async () => {
  try {
    loading.value = true
    errorMessage.value = ''
    const response = await api.get(`/api/hoa-don/code/${encodeURIComponent(maHoaDon)}`)
    let res = response?.data
    if (res && !Array.isArray(res) && res.data) {
      res = res.data
    }
    hoaDon.value = res
    if (res && Number(res.trangThai) === 1 && res.id) {
      try {
        const stockRes = await api.get(`/api/hoa-don/${res.id}/check-stock`)
        if (stockRes && stockRes.data) {
          isOrderOutOfStock.value = stockRes.data.duHang === false
          stockCheckItems.value = Array.isArray(stockRes.data.items) ? stockRes.data.items : []
        } else {
          isOrderOutOfStock.value = false
          stockCheckItems.value = []
        }
      } catch (e) {
        console.warn('Lỗi kiểm tra tồn kho:', e)
        stockCheckItems.value = []
      }
    } else {
      isOrderOutOfStock.value = false
      stockCheckItems.value = []
    }
  } catch (error) {
    console.error('Lỗi tải hóa đơn:', error)
    errorMessage.value = 'Không thể tải thông tin hóa đơn.'
  } finally {
    loading.value = false
  }
}

const formatMoney = (money) => {
  if (money == null) return '0đ'
  return Number(money).toLocaleString('vi-VN') + 'đ'
}

const getStatusText = (status) => {
  const map = {
    1: 'Chờ xác nhận',
    2: 'Đã xác nhận',
    3: 'Chờ vận chuyển',
    4: 'Vận chuyển',
    5: 'Đã hoàn thành',
    6: 'Hủy',
    7: 'Giao hàng thất bại'
  }
  return map[Number(status)] || 'Chưa cập nhật'
}

const nextStatus = computed(() => {
  if (!hoaDon.value) return null
  const currentStatus = Number(hoaDon.value.trangThai)
  return currentStatus >= 1 && currentStatus <= 4 ? currentStatus + 1 : null
})

const actionText = computed(() => {
  if (!hoaDon.value) return ''
  switch (Number(hoaDon.value.trangThai)) {
    case 1:
      return '✓ Xác nhận đơn hàng & In hóa đơn'
    case 2:
      return '✓ Chuyển chờ vận chuyển'
    case 3:
      return '✓ Chuyển vận chuyển'
    case 4:
      return '✓ Hoàn thành đơn hàng'
    default:
      return ''
  }
})

const isPaid = computed(() => {
  return Number(hoaDon.value?.trangThaiThanhToan) === 1
})

const finalTotalPayment = computed(() => {
  if (!hoaDon.value) return 0
  if (hoaDon.value.tongTien != null) {
    return Number(hoaDon.value.tongTien)
  }
  const hang = Number(hoaDon.value.tongTienHang || 0)
  const ship = Number(hoaDon.value.phiShip || 0)
  const giam = Number(hoaDon.value.tongTienGiamGia || 0)
  return Math.max(0, hang + ship - giam)
})

const isOnlineOrder = computed(() => {
  if (!hoaDon.value) return false
  if (Number(hoaDon.value.loaiDon) === 2) return true
  const note = String(hoaDon.value.ghiChu || '').toLowerCase()
  return note.includes('online') || note.includes('website')
})

const isUnpaidPrepaidOrder = computed(() => {
  if (!isOnlineOrder.value || isPaid.value || !hoaDon.value) return false
  const paymentText = `${hoaDon.value.ghiChu || ''} ${hoaDon.value.phuongThucThanhToan || ''} ${hoaDon.value.hinhThucThanhToan || ''}`.toUpperCase()
  return ['VIETQR', 'MOMO', 'VNPAY', 'CHUYỂN KHOẢN'].some(method => paymentText.includes(method))
})

const isRefundedOrder = computed(() => {
  if (!hoaDon.value || Number(hoaDon.value.trangThai) !== 6 || !isPaid.value) return false
  return String(hoaDon.value.ghiChu || '').toLowerCase().includes('hoàn tiền')
})

const extractEmailFromNote = (note) => {
  const match = String(note || '').match(/\[EMAIL:([^\]]+)\]/i)
  return match ? match[1].trim() : ''
}

const customerEmailResolved = computed(() => {
  if (!hoaDon.value) return ''
  return String(
    hoaDon.value.emailKhachHang ||
    hoaDon.value.email ||
    hoaDon.value.idKhachHang?.email ||
    extractEmailFromNote(hoaDon.value.ghiChu) ||
    ''
  ).trim()
})

const paymentMethodResolved = computed(() => {
  if (!hoaDon.value) return 'Thanh toán trực tuyến'
  const rawList = Array.isArray(hoaDon.value.hinhThucThanhToan) ? hoaDon.value.hinhThucThanhToan : []
  const listNames = rawList
    .map(p => p?.tenPhuongThuc || p?.tenHinhThuc || p?.phuongThuc)
    .filter(Boolean)
    .join(', ')
  if (listNames) return listNames

  const rawText = [
    typeof hoaDon.value.hinhThucThanhToan === 'string' ? hoaDon.value.hinhThucThanhToan : '',
    hoaDon.value.phuongThucThanhToan || '',
    hoaDon.value.ghiChu || ''
  ].join(' ').toLowerCase()

  if (rawText.includes('vnpay')) return 'VNPAY / Chuyển khoản'
  if (rawText.includes('momo')) return 'MoMo'
  if (rawText.includes('payos')) return 'PayOS / Chuyển khoản'
  if (rawText.includes('chuyển khoản') || rawText.includes('vietqr')) return 'Chuyển khoản'
  if (rawText.includes('cod') || rawText.includes('nhận hàng')) return 'Thanh toán khi nhận hàng (COD)'
  return 'Thanh toán trực tuyến'
})

const transactionCodeResolved = computed(() => {
  if (!hoaDon.value) return ''
  if (hoaDon.value.maGiaoDich || hoaDon.value.transactionCode) {
    return hoaDon.value.maGiaoDich || hoaDon.value.transactionCode
  }
  const rawList = Array.isArray(hoaDon.value.hinhThucThanhToan) ? hoaDon.value.hinhThucThanhToan : []
  const found = rawList.find(p => p?.maGiaoDich)
  return found ? found.maGiaoDich : ''
})

const refundAmountResolved = computed(() => {
  if (!hoaDon.value) return 0
  if (hoaDon.value.tienSauGiamGia != null && Number(hoaDon.value.tienSauGiamGia) > 0) {
    return Number(hoaDon.value.tienSauGiamGia)
  }
  return finalTotalPayment.value
})

const refundOutOfStockItems = computed(() => {
  const apiItems = Array.isArray(stockCheckItems.value) ? stockCheckItems.value : []
  return apiItems
    .filter(it => it.hetHang || Number(it.soLuongTon ?? 0) < Number(it.soLuongDat ?? 1))
    .map(it => ({
      chiTietId: it.chiTietId ?? it.idChiTietHoaDon,
      variantId: it.variantId ?? it.idChiTietSanPham,
      name: it.tenSanPham || 'Sản phẩm',
      variantText: [it.tenMauSac, it.tenKichCo].filter(Boolean).join(' • '),
      needed: Number(it.soLuongDat ?? 1),
      available: Number(it.soLuongTon ?? 0)
    }))
})

// Toast thông báo
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

// Modal Xác Nhận Tùy Chỉnh (không dùng alert / confirm trình duyệt)
const confirmModal = ref({
  show: false,
  title: 'Xác Nhận Thao Tác',
  message: '',
  subText: '',
  confirmText: 'Đồng ý',
  cancelText: 'Hủy bỏ',
  type: 'primary',
  showEmailInput: false,
  sendEmail: true,
  customerEmail: '',
  onConfirm: null
})

const openConfirmModal = ({
                            title = 'Xác Nhận Thao Tác',
                            message = '',
                            subText = '',
                            confirmText = 'Đồng ý',
                            cancelText = 'Hủy bỏ',
                            type = 'primary',
                            showEmailInput = false,
                            sendEmail = true,
                            customerEmail = '',
                            onConfirm = null
                          }) => {
  confirmModal.value = {
    show: true,
    title,
    message,
    subText,
    confirmText,
    cancelText,
    type,
    showEmailInput,
    sendEmail,
    customerEmail,
    onConfirm
  }
}

const closeConfirmModal = () => {
  confirmModal.value.show = false
}

const handleConfirmAccept = async () => {
  const cb = confirmModal.value.onConfirm
  const emailInput = confirmModal.value.customerEmail ? confirmModal.value.customerEmail.trim() : ''
  const willSendEmail = confirmModal.value.sendEmail && confirmModal.value.showEmailInput
  if (willSendEmail && emailInput && !emailInput.includes('@')) {
    showToast('Vui lòng nhập định dạng email hợp lệ (VD: khachhang@gmail.com)!', 'warning')
    return
  }
  confirmModal.value.show = false
  if (cb) {
    await cb()
  }
}

const isOutOfStockError = (error) => {
  const errData = error?.response?.data
  if (errData?.outOfStock === true) return true
  const msg = String(errData?.message || error?.message || '').toLowerCase()
  return msg.includes('hết hàng') || msg.includes('không đủ số lượng') || msg.includes('không đủ tồn kho')
}

const refreshOrderStock = async () => {
  if (!hoaDon.value?.id) return
  try {
    const stockRes = await api.get(`/api/hoa-don/${hoaDon.value.id}/check-stock`)
    if (stockRes && stockRes.data) {
      isOrderOutOfStock.value = stockRes.data.duHang === false
      stockCheckItems.value = Array.isArray(stockRes.data.items) ? stockRes.data.items : []
    }
  } catch (e) {
    console.warn('Lỗi kiểm tra tồn kho:', e)
  }
}

const handleConfirmRefund = async ({ reason, sendEmail, email }) => {
  if (!hoaDon.value?.id || refundingOrder.value) return
  refundingOrder.value = true
  const currentAuth = getCurrentUser()
  const authStaffName = currentAuth?.tenNhanVien || currentAuth?.tenTaiKhoan || 'Nhân viên bán hàng'
  const authStaffId = currentAuth?.id || null

  const cleanEmail = (sendEmail && email && email.trim()) ? email.trim() : undefined
  const emailQuery = cleanEmail ? `&email=${encodeURIComponent(cleanEmail)}` : ''
  const payload = {
    trangThai: 6,
    hoanTien: true,
    guiEmail: Boolean(sendEmail && cleanEmail),
    ghiChu: reason,
    email: cleanEmail,
    idNhanVien: authStaffId,
    nhanVienId: authStaffId,
    nguoiThucHien: authStaffName,
    nguoiCapNhat: authStaffName
  }

  try {
    await api.put(`/api/hoa-don/${hoaDon.value.id}/trang-thai?trangThai=6${emailQuery}`, payload)
    showRefundModal.value = false
    const amountLabel = formatMoney(refundAmountResolved.value)
    showToast(
      cleanEmail
        ? `Đã hủy đơn, ghi nhận hoàn tiền ${amountLabel} và gửi email thông báo tới ${cleanEmail}!`
        : `Đã hủy đơn hàng và ghi nhận hoàn tiền ${amountLabel} cho khách hàng!`
    )
    await loadHoaDon()
  } catch (error) {
    console.error('Lỗi hủy đơn & hoàn tiền:', error)
    showToast(error.response?.data?.message || 'Không thể xử lý hủy đơn & hoàn tiền!', 'warning')
  } finally {
    refundingOrder.value = false
  }
}

const xacNhanDon = async () => {
  if (!hoaDon.value || saving.value || paying.value || checkingStock.value || refundingOrder.value) return
  const status = nextStatus.value
  if (!status) return

  const isConfirming = status === 2
  if (isConfirming && isUnpaidPrepaidOrder.value) {
    showToast('Đơn VietQR/MoMo/VNPAY chưa thanh toán nên chưa thể xác nhận. Vui lòng chờ khách thanh toán hoặc đơn tự hủy khi hết hạn!', 'warning')
    return
  }
  if (isConfirming && hoaDon.value.id) {
    checkingStock.value = true
    try {
      await refreshOrderStock()
    } finally {
      checkingStock.value = false
    }
  }

  if (isConfirming && isOrderOutOfStock.value) {
    if (isOnlineOrder.value && isPaid.value) {
      showRefundModal.value = true
    } else {
      showToast('Không thể xác nhận đơn hàng vì sản phẩm trong kho không đủ hoặc đã hết hàng!', 'warning')
    }
    return
  }
  const existingEmail = customerEmailResolved.value

  openConfirmModal({
    title: isConfirming ? 'Xác Nhận Đơn Hàng' : 'Xác Nhận Trạng Thái Đơn Hàng',
    message: isConfirming
        ? `Bạn có chắc muốn xác nhận đơn hàng "${hoaDon.value.maHoaDon}" không?`
        : `Bạn có chắc muốn chuyển đơn hàng sang "${getStatusText(status)}"?`,
    subText: isConfirming
        ? 'Hệ thống sẽ cập nhật trạng thái đơn sang "Đã xác nhận" và gửi email thông báo chi tiết cho khách hàng.'
        : 'Thao tác này sẽ lưu trạng thái mới vào cơ sở dữ liệu.',
    confirmText: isConfirming ? 'Xác nhận đơn' : 'Đồng ý',
    cancelText: 'Hủy bỏ',
    type: 'primary',
    showEmailInput: isConfirming,
    sendEmail: Boolean(existingEmail && existingEmail.includes('@')),
    customerEmail: existingEmail,
    onConfirm: async () => {
      try {
        saving.value = true
        const emailToSend = (confirmModal.value.showEmailInput && confirmModal.value.sendEmail && confirmModal.value.customerEmail)
            ? confirmModal.value.customerEmail.trim()
            : undefined
        const emailQuery = emailToSend ? `&email=${encodeURIComponent(emailToSend)}` : ''
        await api.put(`/api/hoa-don/${hoaDon.value.id}/trang-thai?trangThai=${status}${emailQuery}`, {
          trangThai: status,
          email: emailToSend
        })
        hoaDon.value.trangThai = status
        if (emailToSend && isConfirming) {
          showToast(`Đã xác nhận đơn hàng và gửi email thông báo tới ${emailToSend}!`)
        } else {
          showToast(`Đã chuyển đơn hàng sang "${getStatusText(status)}"!`)
        }
        // Tự động chuyển đến chi tiết hóa đơn và in theo mẫu chuẩn
        if (status === 2) {
          setTimeout(() => {
            router.push({ path: `/hoa-don/${maHoaDon}`, query: { autoPrint: 'true' } })
          }, 500)
        }
      } catch (error) {
        console.error('Lỗi cập nhật trạng thái:', error)
        if (isConfirming && isOutOfStockError(error)) {
          isOrderOutOfStock.value = true
          await refreshOrderStock()
          if (isOnlineOrder.value && isPaid.value) {
            showRefundModal.value = true
            return
          }
        }
        showToast(error.response?.data?.message || 'Không thể cập nhật trạng thái đơn hàng!', 'warning')
      } finally {
        saving.value = false
      }
    }
  })
}

const thanhToan = () => {
  if (!hoaDon.value || isPaid.value) return

  openConfirmModal({
    title: 'Xác Nhận Thanh Toán',
    message: 'Bạn có chắc hóa đơn này đã được hoàn tất thanh toán?',
    subText: 'Hóa đơn sẽ được chuyển sang trạng thái "Đã thanh toán" và tự động mở hộp thoại in hóa đơn.',
    confirmText: 'Xác nhận & In hóa đơn',
    cancelText: 'Hủy bỏ',
    type: 'primary',
    onConfirm: async () => {
      try {
        paying.value = true
        await api.put(`/api/hoa-don/${hoaDon.value.id}`, { trangThaiThanhToan: 1 })
        hoaDon.value.trangThaiThanhToan = 1
        showToast('Thanh toán hóa đơn thành công!')
        // Tự động chuyển đến chi tiết hóa đơn và in theo mẫu chuẩn
        setTimeout(() => {
          router.push({ path: `/hoa-don/${maHoaDon}`, query: { autoPrint: 'true' } })
        }, 500)
      } catch (error) {
        console.error('Lỗi cập nhật thanh toán:', error)
        showToast(error.response?.data?.message || 'Không thể cập nhật trạng thái thanh toán!', 'warning')
      } finally {
        paying.value = false
      }
    }
  })
}

const quayLai = () => {
  router.push(`/hoa-don/${maHoaDon}`)
}

onMounted(() => {
  loadHoaDon()
})
</script>

<template>
  <div class="confirm-page">
    <!-- HEADER -->
    <div class="page-header">
      <div class="page-breadcrumb">
        <router-link to="/hoa-don" class="crumb-parent">Quản lý đơn hàng</router-link>
        <span class="crumb-separator">/</span>
        <span class="crumb-current">Xử lý đơn hàng: {{ maHoaDon }}</span>
      </div>

      <button class="btn-back" @click="quayLai">
        ← Quay lại
      </button>
    </div>

    <!-- LOADING & ERROR -->
    <div v-if="loading" class="loading">
      Đang tải thông tin hóa đơn...
    </div>

    <div v-else-if="errorMessage" class="error-box">
      {{ errorMessage }}
    </div>

    <!-- MAIN CONTENT -->
    <div v-else-if="hoaDon" class="content">
      <!-- KHUNG THÔNG TIN HÓA ĐƠN -->
      <div class="card">
        <div class="card-header">
          <div>
            <h3>Thông tin hóa đơn</h3>
            <p>Kiểm tra thông tin trước khi xử lý</p>
          </div>

          <div class="status-group">
            <span class="status">
              {{ getStatusText(hoaDon.trangThai) }}
            </span>
            <span v-if="isPaid" class="payment-paid">
              ✓ Đã thanh toán
            </span>
            <span v-else class="payment-unpaid">
              Chưa thanh toán
            </span>
            <span v-if="isRefundedOrder" class="payment-refunded">
              💸 Đã hoàn tiền cho khách
            </span>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="label">Mã hóa đơn</span>
            <strong>{{ hoaDon.maHoaDon }}</strong>
          </div>

          <div class="info-item">
            <span class="label">Khách hàng</span>
            <strong>{{ hoaDon.tenKhachHang || 'Khách lẻ' }}</strong>
          </div>

          <div class="info-item">
            <span class="label">Số điện thoại</span>
            <strong>{{ hoaDon.soDienThoaiKhachHang || hoaDon.soDienThoai || '---' }}</strong>
          </div>

          <div class="info-item">
            <span class="label">Tổng thanh toán</span>
            <strong class="price">{{ formatMoney(finalTotalPayment) }}</strong>
          </div>
        </div>
      </div>

      <!-- KHUNG THANH TOÁN -->
      <div class="card">
        <div class="card-header">
          <div>
            <h3>Thanh toán</h3>
            <p>Trạng thái thanh toán của hóa đơn</p>
          </div>
        </div>

        <div v-if="isPaid" class="paid-box" :class="{ 'refunded-box': isRefundedOrder }">
          <span class="paid-icon">{{ isRefundedOrder ? '💸' : '✓' }}</span>
          <div>
            <strong>{{ isRefundedOrder ? 'Đã thanh toán — Đã hoàn tiền cho khách' : 'Đã thanh toán' }}</strong>
            <p>{{ isRefundedOrder ? 'Đơn hàng đã bị hủy do kho hết hàng và đã ghi nhận hoàn tiền lại cho khách hàng.' : 'Hóa đơn này đã được xác nhận thanh toán.' }}</p>
          </div>
        </div>

        <div v-else class="unpaid-box">
          <div>
            <strong>Chưa thanh toán</strong>
            <p>Xác nhận ngay khi khách hàng đã chuyển khoản hoặc thanh toán tiền mặt.</p>
          </div>

          <button class="btn-payment" @click="thanhToan" :disabled="paying">
            {{ paying ? 'Đang xử lý...' : '✓ Xác nhận đã thanh toán & In hóa đơn' }}
          </button>
        </div>
      </div>

      <!-- TIẾN TRÌNH TRẠNG THÁI -->
      <div class="card">
        <div class="card-header">
          <div>
            <h3>Trạng thái đơn hàng</h3>
            <p>Tiến trình xử lý đơn hàng</p>
          </div>
        </div>

        <div class="status-flow">
          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 1 }">
            <div class="flow-number">1</div>
            <span>Chờ xác nhận</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 2 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 2 }">
            <div class="flow-number">2</div>
            <span>Đã xác nhận</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 3 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 3 }">
            <div class="flow-number">3</div>
            <span>Chờ vận chuyển</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 4 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 4 }">
            <div class="flow-number">4</div>
            <span>Vận chuyển</span>
          </div>

          <div class="flow-line" :class="{ active: Number(hoaDon.trangThai) >= 5 }"></div>

          <div class="flow-item" :class="{ active: Number(hoaDon.trangThai) >= 5 }">
            <div class="flow-number">5</div>
            <span>Đã hoàn thành</span>
          </div>
        </div>
      </div>

      <!-- Cảnh báo hết hàng khi ở bước Chờ xác nhận -->
      <div v-if="nextStatus === 2 && isOrderOutOfStock" class="stock-warning-banner" style="background: #fff1f2; border: 1px solid #fda4af; border-radius: 8px; padding: 12px 16px; margin-bottom: 16px; color: #9f1239; font-size: 13.5px; display: flex; align-items: center; gap: 8px;">
        <span style="font-size: 18px;">⚠️</span>
        <div v-if="isOnlineOrder && isPaid">
          <strong>Kho không còn / không đủ sản phẩm:</strong> Đơn hàng online này đã thanh toán — ấn xác nhận bên dưới để mở thông báo hoàn tiền cho khách hàng.
        </div>
        <div v-else>
          <strong>Không thể xác nhận:</strong> Đơn hàng có sản phẩm không đủ tồn kho hoặc đã hết hàng trong kho.
        </div>
      </div>

      <!-- NÚT THAO TÁC CUỐI TRANG -->
      <div class="action-buttons">
        <button class="btn-cancel" @click="quayLai" :disabled="saving || paying || checkingStock || refundingOrder">
          Hủy
        </button>

        <button
            v-if="nextStatus"
            class="btn-confirm"
            :class="{ 'btn-disabled': (nextStatus === 2 && isUnpaidPrepaidOrder) || (nextStatus === 2 && isOrderOutOfStock && !(isOnlineOrder && isPaid)) }"
            @click="xacNhanDon"
            :disabled="saving || paying || checkingStock || refundingOrder || (nextStatus === 2 && isUnpaidPrepaidOrder) || (nextStatus === 2 && isOrderOutOfStock && !(isOnlineOrder && isPaid))"
            :title="(nextStatus === 2 && isUnpaidPrepaidOrder) ? 'Đơn thanh toán trước chưa được thanh toán' : ((nextStatus === 2 && isOrderOutOfStock) ? (isOnlineOrder && isPaid ? 'Kho hết hàng - Ấn xác nhận để mở thông báo hoàn tiền cho khách hàng' : 'Không thể xác nhận vì sản phẩm trong kho không đủ hoặc đã hết hàng!') : '')"
        >
          {{ (saving || checkingStock) ? 'Đang xử lý...' : actionText }}
        </button>
      </div>
    </div>

    <!-- Toast Popup Thông Báo -->
    <transition name="toast-fade">
      <div v-if="toast.show" class="toast-popup" :class="toast.type">
        <span class="toast-icon">{{ toast.type === 'warning' ? '⚠' : '✓' }}</span>
        <span class="toast-text">{{ toast.message }}</span>
      </div>
    </transition>

    <!-- Modal Xác Nhận Thao Tác (Thay thế confirm của localhost) -->
    <transition name="modal-fade">
      <div v-if="confirmModal.show" class="modal-backdrop" @click.self="closeConfirmModal">
        <div class="modal-container modal-confirm-box">
          <div class="modal-header confirm-header">
            <div class="confirm-title-group">
              <span class="confirm-icon-badge" :class="confirmModal.type">
                <svg v-if="confirmModal.type === 'danger'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <path d="M12 16v-4"></path>
                  <path d="M12 8h.01"></path>
                </svg>
              </span>
              <div>
                <h3 class="modal-title-custom">{{ confirmModal.title }}</h3>
              </div>
            </div>
            <button class="btn-close-modal" type="button" @click="closeConfirmModal">✕</button>
          </div>

          <div class="modal-body confirm-body">
            <p class="confirm-main-message">{{ confirmModal.message }}</p>
            <p v-if="confirmModal.subText" class="confirm-sub-message">{{ confirmModal.subText }}</p>

            <!-- Nhập email khách hàng khi xác nhận đơn hàng -->
            <div v-if="confirmModal.showEmailInput" class="confirm-email-box">
              <label class="confirm-checkbox-label">
                <input type="checkbox" v-model="confirmModal.sendEmail" />
                <span class="checkbox-text font-bold">✉ Gửi email thông báo xác nhận đơn hàng cho khách</span>
              </label>
              <div v-if="confirmModal.sendEmail" class="email-input-subgroup">
                <label class="modal-label">Địa chỉ email nhận thông báo <span class="text-danger">*</span></label>
                <input
                    type="email"
                    v-model="confirmModal.customerEmail"
                    placeholder="Nhập email khách hàng (VD: khachhang@gmail.com)..."
                    class="modal-input-field"
                    maxlength="100"
                />
                <p class="email-guide-text">Hệ thống sẽ tự động gửi email chi tiết đơn hàng cho khách hàng qua địa chỉ email này.</p>
              </div>
            </div>
          </div>

          <div class="modal-footer confirm-footer">
            <button class="btn-modal-cancel" type="button" @click="closeConfirmModal">
              {{ confirmModal.cancelText }}
            </button>
            <button
                class="btn-confirm-accept"
                :class="confirmModal.type"
                type="button"
                :disabled="saving || paying"
                @click="handleConfirmAccept"
            >
              {{ (saving || paying) ? 'Đang xử lý...' : (
                confirmModal.showEmailInput && confirmModal.sendEmail && confirmModal.customerEmail
                    ? '✓ Xác nhận & Gửi mail'
                    : confirmModal.confirmText
            ) }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <RefundOrderModal
      :show="showRefundModal"
      :loading="refundingOrder"
      :order-code="hoaDon?.maHoaDon || maHoaDon"
      :customer-name="hoaDon?.tenKhachHang || 'Khách lẻ'"
      :customer-phone="hoaDon?.soDienThoaiKhachHang || hoaDon?.soDienThoai || ''"
      :payment-method="paymentMethodResolved"
      :transaction-code="transactionCodeResolved"
      :refund-amount="refundAmountResolved"
      :customer-email="customerEmailResolved"
      :out-of-stock-items="refundOutOfStockItems"
      @close="showRefundModal = false"
      @confirm="handleConfirmRefund"
    />
  </div>
</template>

<style scoped>
.confirm-page {
  min-height: calc(100vh - 48px);
  background: var(--bg, #f7f5ef);
  padding: 1.25rem 2rem 3rem;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title-line {
  width: 5px;
  height: 48px;
  background: #496883;
  border-radius: 4px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.page-header p {
  margin: 5px 0 0;
  color: #777;
}

.btn-back {
  padding: 10px 18px;
  border: 1px solid #ddd8cc;
  border-radius: 8px;
  background: white;
  color: #496883;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #f0ebe1;
}

.content {
  max-width: 1000px;
  margin: 0 auto;
}

.card {
  background: white;
  border: 1px solid #e9e5db;
  border-radius: 12px;
  padding: 22px;
  margin-bottom: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  color: #333;
  font-size: 17px;
}

.card-header p {
  margin: 5px 0 0;
  color: #888;
  font-size: 13px;
}

.status-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status,
.payment-paid,
.payment-unpaid,
.payment-refunded {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status {
  background: #dbeafe;
  color: #1d4ed8;
}

.payment-paid {
  background: #dcfce7;
  color: #15803d;
}

.payment-unpaid {
  background: #fff3cd;
  color: #856404;
}

.payment-refunded {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px 30px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  color: #888;
  font-size: 13px;
}

.info-item strong {
  color: #333;
}

.price {
  color: #c94a29 !important;
  font-size: 1.15rem;
}

/* KHỐI THANH TOÁN */
.paid-box,
.unpaid-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border-radius: 10px;
}

.paid-box {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  justify-content: flex-start;
}

.paid-box.refunded-box {
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.paid-box.refunded-box .paid-icon {
  background: #fef3c7;
  color: #b45309;
}

.unpaid-box {
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.paid-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #dcfce7;
  color: #15803d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}

.paid-box strong,
.unpaid-box strong {
  color: #333;
}

.paid-box p,
.unpaid-box p {
  margin: 5px 0 0;
  color: #777;
  font-size: 13px;
}

.btn-payment {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: #496883;
  color: white;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.btn-payment:hover {
  background: #3d596f;
}

/* FLOW TRẠNG THÁI */
.status-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.flow-item {
  min-width: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  color: #aaa;
  font-size: 12px;
}

.flow-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #eee;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.flow-item.active {
  color: #496883;
  font-weight: 600;
}

.flow-item.active .flow-number {
  background: #496883;
  color: white;
}

.flow-line {
  flex: 1;
  height: 2px;
  background: #e5e5e5;
  margin-bottom: 24px;
}

.flow-line.active {
  background: #496883;
}

/* THAO TÁC */
.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-bottom: 30px;
}

.btn-cancel {
  padding: 11px 22px;
  border-radius: 8px;
  border: 1px solid #ddd8cc;
  background: white;
  color: #555;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f0ebe1;
}

.btn-confirm {
  padding: 11px 22px;
  border-radius: 8px;
  border: 1px solid #496883;
  background: #496883;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm.btn-disabled {
  background-color: #cbd5e1 !important;
  color: #64748b !important;
  border-color: #cbd5e1 !important;
  cursor: not-allowed !important;
  box-shadow: none !important;
  opacity: 0.65;
}

.btn-confirm:hover {
  background: #3d596f;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: 50px;
  color: #777;
}

.error-box {
  padding: 15px;
  border-radius: 8px;
  background: #fee2e2;
  color: #b91c1c;
}

@media (max-width: 768px) {
  .confirm-page {
    padding: 15px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .status-flow {
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 10px;
  }

  .flow-item {
    min-width: 100px;
  }

  .paid-box,
  .unpaid-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-buttons {
    flex-wrap: wrap;
  }
}

/* Toast popup */
.toast-popup {
  position: fixed;
  top: 24px;
  right: 28px;
  background-color: #36536b;
  color: #ffffff;
  padding: 11px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.88rem;
  font-weight: 600;
  z-index: 9999;
  border: 1px solid #4a6880;
}

.toast-popup.warning {
  background-color: #d97706;
  border-color: #b45309;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

/* Custom Confirm Modal Styling */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9998;
}

.modal-confirm-box {
  max-width: 480px;
  width: 90%;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22);
  animation: modalPop 0.22s cubic-bezier(0.16, 1, 0.3, 1);
  background: #ffffff;
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.confirm-header {
  padding: 18px 24px 14px 24px;
  border-bottom: 1px solid #f1e7dc;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.confirm-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-title-custom {
  font-size: 1.05rem;
  font-weight: 700;
  color: #2b353b;
  margin: 0;
}

.btn-close-modal {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: none;
  background: #f1ede4;
  color: #666;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #e2dbce;
  color: #333;
}

.confirm-icon-badge {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.confirm-icon-badge.primary {
  background: #e8f1f5;
  color: #3e5c76;
}

.confirm-icon-badge.warning {
  background: #fff4e5;
  color: #d97706;
}

.confirm-icon-badge.danger {
  background: #fee2e2;
  color: #dc2626;
}

.confirm-body {
  padding: 22px 24px 14px 24px;
}

.confirm-main-message {
  font-size: 1rem;
  color: #2b353b;
  font-weight: 600;
  line-height: 1.55;
  margin: 0 0 8px 0;
}

.confirm-sub-message {
  font-size: 0.85rem;
  color: #6c757d;
  line-height: 1.45;
  margin: 0;
}

.confirm-footer {
  padding: 14px 24px 20px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid #f8f3ed;
  background: #fdfbf7;
}

.btn-modal-cancel {
  height: 38px;
  padding: 0 16px;
  background: #ffffff;
  color: #64748b;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-modal-cancel:hover {
  background: #f1f5f9;
  color: #334155;
}

.btn-confirm-accept {
  height: 38px;
  padding: 0 20px;
  border-radius: 7px;
  border: none;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-confirm-accept.primary {
  background-color: #3e5c76;
  box-shadow: 0 2px 6px rgba(62, 92, 118, 0.25);
}

.btn-confirm-accept.primary:hover:not(:disabled) {
  background-color: #2b4257;
}

.btn-confirm-accept.danger {
  background-color: #dc2626;
  box-shadow: 0 2px 6px rgba(220, 38, 38, 0.25);
}

.btn-confirm-accept.danger:hover:not(:disabled) {
  background-color: #b91c1c;
}

.btn-confirm-accept:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.confirm-email-box {
  margin-top: 14px;
  padding: 12px 14px;
  background: #fbf7f4;
  border: 1.5px solid #ebd9c8;
  border-radius: 8px;
}

.confirm-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.86rem;
  color: #7b5943;
  cursor: pointer;
}

.email-input-subgroup {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #4b5563;
}

.modal-input-field {
  height: 38px;
  padding: 0 12px;
  border: 1.5px solid #d5c8b8;
  border-radius: 6px;
  font-size: 0.86rem;
  background-color: #ffffff;
  color: #2b353b;
  outline: none;
  transition: all 0.2s ease;
}

.modal-input-field:focus {
  border-color: #7b5943;
  box-shadow: 0 0 0 2px rgba(123, 89, 67, 0.15);
}

.email-guide-text {
  font-size: 0.76rem;
  color: #8c7664;
  margin: 2px 0 0;
  line-height: 1.4;
}

.text-danger {
  color: #dc2626;
}

.font-bold {
  font-weight: 700;
}
</style>
