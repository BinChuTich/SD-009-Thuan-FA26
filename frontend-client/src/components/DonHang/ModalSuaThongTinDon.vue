<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import api from '@/api'
import { formatCurrency } from '@/utils/dinhDang.js'
import { loadAdministrativeData, getWardsForProvince } from '@/utils/diaGioiHanhChinh.js'
import { calculateGhnShippingFee } from '@/services/ghnService.js'
import { showAppAlert, showAppConfirm, showAppToast } from '@/utils/thongBaoDialog.js'
import { currentClientUser, isClientLoggedIn } from '@/utils/taiKhoanKhachHang.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  order: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'updated'])

// Danh sách địa chỉ đã lưu
const savedAddresses = ref([])
const loadingAddresses = ref(false)
const selectedAddressId = ref(null)

// Form thêm / sửa địa chỉ
const showAddressForm = ref(false)
const editingAddressId = ref(null)
const savingAddress = ref(false)
const addrForm = ref({
  tenNguoiNhan: '',
  soDienThoaiNhan: '',
  thanhPho: '',
  huyen: '',
  phuong: '',
  diaChiCuThe: '',
  macDinh: false
})

// Dữ liệu hành chính
const provincesList = ref([])
const addrWardsList = ref([])

// Trạng thái tính phí ship & cập nhật đơn
const loadingShipping = ref(false)
const calculatedFee = ref(null)
const shippingMessage = ref('')
const submitting = ref(false)
const ghiChuMoi = ref('')

// Tên khách hàng hiển thị ở tiêu đề modal
const customerDisplayName = computed(() => {
  return props.order?.tenKhachHang || currentClientUser.value?.name || 'Khách hàng'
})

// ID khách hàng liên kết với đơn
const customerId = computed(() => {
  return props.order?.idKhachHang?.id || props.order?.idKhachHang || currentClientUser.value?.id || null
})

// Phí ship ban đầu của đơn
const originalShippingFee = computed(() => {
  return Number(props.order?.phiVanChuyen || props.order?.phiShip || 0)
})

// Kiểm tra đơn đã thanh toán hay chưa (Chuyển khoản / VNPAY)
const isPaidOrder = computed(() => {
  if (!props.order) return false
  return props.order.trangThaiThanhToan === 1
})

// Tiền hàng sau voucher (không bao gồm tiền ship)
const merchandiseSubtotal = computed(() => {
  if (!props.order) return 0
  const total = Number(props.order.tienSauGiamGia || props.order.tongTienSauGiam || props.order.tongTien || 0)
  const ship = originalShippingFee.value
  return Math.max(0, total - ship)
})

// Tổng tiền mới thu COD nếu là đơn COD
const newCodTotal = computed(() => {
  const ship = calculatedFee.value != null ? calculatedFee.value : originalShippingFee.value
  return merchandiseSubtotal.value + ship
})

// Kiểm tra điều kiện cước phí hợp lệ
const isFeeAllowed = computed(() => {
  if (!isPaidOrder.value) {
    return true // Đơn COD: đổi địa chỉ bất kỳ, cước tự cập nhật
  }
  if (calculatedFee.value === null) return true
  // Đơn đã thanh toán: cước mới phải <= cước cũ
  return calculatedFee.value <= originalShippingFee.value
})

// Địa chỉ hiện đang được chọn
const selectedAddress = computed(() => {
  return savedAddresses.value.find(a => a.id === selectedAddressId.value) || null
})

// Tải danh sách tỉnh thành
onMounted(async () => {
  try {
    const data = await loadAdministrativeData()
    provincesList.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Lỗi tải danh mục tỉnh thành:', err)
  }
})

// Watch khi modal mở ra
watch(() => props.modelValue, async (isOpen) => {
  if (isOpen) {
    showAddressForm.value = false
    editingAddressId.value = null
    ghiChuMoi.value = ''
    await loadCustomerAddresses()
  }
})

// Tải danh sách địa chỉ khách hàng
const loadCustomerAddresses = async () => {
  loadingAddresses.value = true
  try {
    let list = []
    const cId = customerId.value
    if (cId) {
      const res = await api.get(`/api/khach-hang/${cId}/dia-chi`)
      list = Array.isArray(res.data) ? res.data : []
    }

    // Nếu khách hàng chưa có địa chỉ lưu nào hoặc là khách vãng lai, tạo địa chỉ mẫu từ đơn hiện tại
    if (list.length === 0 && props.order) {
      const parsed = parseOrderAddress(props.order.diaChiNhanHang || '')
      list.push({
        id: 'order_curr',
        maDiaChi: 'Đơn hàng',
        tenNguoiNhan: props.order.tenKhachHang || '',
        soDienThoaiNhan: props.order.soDienThoaiKhachHang || '',
        diaChiCuThe: parsed.street,
        phuong: parsed.ward,
        thanhPho: parsed.province,
        huyen: '',
        macDinh: true
      })
    }

    savedAddresses.value = list

    // Mặc định chọn địa chỉ đầu tiên hoặc địa chỉ khớp với đơn hàng
    if (list.length > 0) {
      const matched = list.find(a => {
        const fullA = [a.diaChiCuThe, a.phuong, a.thanhPho].filter(Boolean).join(', ').toLowerCase()
        const orderA = (props.order?.diaChiNhanHang || '').toLowerCase()
        return orderA.includes(a.diaChiCuThe?.toLowerCase() || '---') || fullA === orderA
      }) || list.find(a => a.macDinh) || list[0]

      if (matched) {
        await selectAddressForOrder(matched)
      }
    }
  } catch (err) {
    console.warn('Lỗi tải danh sách địa chỉ:', err)
  } finally {
    loadingAddresses.value = false
  }
}

// Phân tích chuỗi địa chỉ
const parseOrderAddress = (rawAddress) => {
  const parts = rawAddress.split(',').map(s => s.trim()).filter(Boolean)
  if (parts.length >= 3) {
    return {
      province: parts[parts.length - 1],
      ward: parts[parts.length - 2],
      street: parts.slice(0, parts.length - 2).join(', ')
    }
  }
  return { province: '', ward: '', street: rawAddress }
}

// Chọn một địa chỉ trong danh sách
const selectAddressForOrder = async (addr) => {
  if (!addr) return
  selectedAddressId.value = addr.id
  await updateShippingForAddress(addr)
}

// Tính phí GHN cho địa chỉ được chọn
const updateShippingForAddress = async (addr) => {
  if (!addr || !addr.thanhPho) {
    calculatedFee.value = originalShippingFee.value
    shippingMessage.value = ''
    return
  }

  loadingShipping.value = true
  try {
    const fullAddr = [addr.diaChiCuThe, addr.phuong, addr.thanhPho].filter(Boolean).join(', ')
    const res = await calculateGhnShippingFee({
      provinceName: addr.thanhPho,
      wardName: addr.phuong || '',
      fullAddress: fullAddr,
      weightGram: 300,
      insuranceValue: merchandiseSubtotal.value
    })

    if (res && res.fee != null) {
      calculatedFee.value = Number(res.fee)
      shippingMessage.value = res.regionDescription || 'Tính cước GHN thành công'
    } else {
      calculatedFee.value = originalShippingFee.value
    }
  } catch (err) {
    console.warn('Lỗi tính cước GHN:', err)
    calculatedFee.value = originalShippingFee.value
  } finally {
    loadingShipping.value = false
  }
}

// Mở form thêm địa chỉ mới
const openAddAddressForm = () => {
  editingAddressId.value = null
  addrForm.value = {
    tenNguoiNhan: customerDisplayName.value !== 'Khách hàng' ? customerDisplayName.value : (props.order?.tenKhachHang || ''),
    soDienThoaiNhan: props.order?.soDienThoaiKhachHang || currentClientUser.value?.phone || '',
    thanhPho: '',
    huyen: '',
    phuong: '',
    diaChiCuThe: '',
    macDinh: savedAddresses.value.length === 0
  }
  addrWardsList.value = []
  showAddressForm.value = true
}

// Mở form chỉnh sửa địa chỉ
const openEditAddressForm = async (addr) => {
  editingAddressId.value = addr.id
  addrForm.value = {
    tenNguoiNhan: addr.tenNguoiNhan || customerDisplayName.value || '',
    soDienThoaiNhan: addr.soDienThoaiNhan || props.order?.soDienThoaiKhachHang || '',
    thanhPho: addr.thanhPho || '',
    huyen: addr.huyen || '',
    phuong: addr.phuong || '',
    diaChiCuThe: addr.diaChiCuThe || '',
    macDinh: !!addr.macDinh
  }
  if (addr.thanhPho) {
    addrWardsList.value = await getWardsForProvince(addr.thanhPho)
  } else {
    addrWardsList.value = []
  }
  showAddressForm.value = true
}

// Hủy form thêm/sửa
const cancelAddressForm = () => {
  showAddressForm.value = false
  editingAddressId.value = null
}

// Khi thay đổi tỉnh thành trong form
const onAddrProvinceChange = async () => {
  addrForm.value.phuong = ''
  addrWardsList.value = []
  if (addrForm.value.thanhPho) {
    addrWardsList.value = await getWardsForProvince(addrForm.value.thanhPho)
  }
}

// Lưu địa chỉ (Thêm hoặc Sửa)
const saveAddress = async () => {
  if (!addrForm.value.tenNguoiNhan || !addrForm.value.tenNguoiNhan.trim()) {
    return showAppAlert('Vui lòng nhập tên người nhận hàng!', 'Thiếu thông tin', 'warning')
  }
  const phone = (addrForm.value.soDienThoaiNhan || '').trim()
  if (!phone) {
    return showAppAlert('Vui lòng nhập số điện thoại nhận hàng!', 'Thiếu thông tin', 'warning')
  } else if (!/^(0|\+84)[3|5|7|8|9][0-9]{8}$/.test(phone.replace(/\s+/g, ''))) {
    return showAppAlert('Số điện thoại không đúng định dạng (10 số)!', 'Sai định dạng', 'warning')
  }
  if (!addrForm.value.thanhPho || !addrForm.value.thanhPho.trim()) {
    return showAppAlert('Vui lòng chọn Tỉnh / Thành phố!', 'Thiếu thông tin', 'warning')
  }
  if (!addrForm.value.phuong || !addrForm.value.phuong.trim()) {
    return showAppAlert('Vui lòng chọn Phường / Xã!', 'Thiếu thông tin', 'warning')
  }
  if (!addrForm.value.diaChiCuThe || !addrForm.value.diaChiCuThe.trim()) {
    return showAppAlert('Vui lòng nhập địa chỉ cụ thể (số nhà, ngõ, tên đường...)!', 'Thiếu thông tin', 'warning')
  }

  savingAddress.value = true
  try {
    const payload = {
      tenNguoiNhan: addrForm.value.tenNguoiNhan.trim(),
      soDienThoaiNhan: phone,
      thanhPho: addrForm.value.thanhPho.trim(),
      huyen: addrForm.value.huyen ? addrForm.value.huyen.trim() : '',
      phuong: addrForm.value.phuong.trim(),
      diaChiCuThe: addrForm.value.diaChiCuThe.trim(),
      macDinh: addrForm.value.macDinh
    }

    const cId = customerId.value
    let saved = null

    if (cId) {
      if (editingAddressId.value && editingAddressId.value !== 'order_curr') {
        const res = await api.put(`/api/khach-hang/${cId}/dia-chi/${editingAddressId.value}`, payload)
        saved = res.data
        showAppToast('Cập nhật địa chỉ thành công!', 'success')
      } else {
        const res = await api.post(`/api/khach-hang/${cId}/dia-chi`, payload)
        saved = res.data
        showAppToast('Thêm địa chỉ mới thành công!', 'success')
      }
      showAddressForm.value = false
      await loadCustomerAddresses()
      if (saved) {
        await selectAddressForOrder(saved)
      }
    } else {
      // Khách vãng lai
      const localItem = {
        id: 'local_' + Date.now(),
        maDiaChi: 'DC_' + Math.floor(Math.random() * 10000),
        ...payload
      }
      savedAddresses.value.unshift(localItem)
      showAddressForm.value = false
      showAppToast('Đã thêm địa chỉ giao hàng!', 'success')
      await selectAddressForOrder(localItem)
    }
  } catch (err) {
    console.error('Lỗi lưu địa chỉ:', err)
    showAppAlert(err?.response?.data?.message || 'Không thể lưu địa chỉ.', 'Lỗi lưu địa chỉ', 'error')
  } finally {
    savingAddress.value = false
  }
}

// Đặt làm mặc định
const setAsDefault = async (addressId) => {
  const cId = customerId.value
  if (!cId || addressId === 'order_curr') return
  try {
    await api.put(`/api/khach-hang/${cId}/dia-chi/${addressId}/mac-dinh`)
    showAppToast('Đã đặt làm địa chỉ mặc định!', 'success')
    await loadCustomerAddresses()
  } catch (err) {
    showAppAlert(err?.response?.data?.message || 'Không thể đặt địa chỉ mặc định.', 'Lỗi', 'error')
  }
}

// Xóa địa chỉ
const deleteAddress = async (addressId) => {
  const confirmed = await showAppConfirm('Bạn có chắc chắn muốn xóa địa chỉ này khỏi danh sách không?', 'Xác nhận xóa địa chỉ')
  if (!confirmed) return

  const cId = customerId.value
  if (!cId || addressId === 'order_curr') {
    savedAddresses.value = savedAddresses.value.filter(a => a.id !== addressId)
    if (selectedAddressId.value === addressId && savedAddresses.value.length > 0) {
      await selectAddressForOrder(savedAddresses.value[0])
    }
    showAppToast('Đã xóa địa chỉ thành công!', 'success')
    return
  }

  try {
    await api.delete(`/api/khach-hang/${cId}/dia-chi/${addressId}`)
    showAppToast('Đã xóa địa chỉ thành công!', 'success')
    await loadCustomerAddresses()
  } catch (err) {
    showAppAlert(err?.response?.data?.message || 'Không thể xóa địa chỉ này.', 'Lỗi', 'error')
  }
}

// Đóng modal
const handleClose = () => {
  emit('update:modelValue', false)
}

// Submit cập nhật đơn hàng sang địa chỉ đã chọn
const handleConfirmOrderUpdate = async () => {
  if (!selectedAddress.value) {
    return showAppAlert('Vui lòng chọn 1 địa chỉ để giao hàng!', 'Chưa chọn địa chỉ', 'warning')
  }

  // Kiểm tra cước phí cho đơn đã thanh toán
  if (isPaidOrder.value && calculatedFee.value !== null && calculatedFee.value > originalShippingFee.value) {
    return showAppAlert(
      `Phí ship địa chỉ mới (${formatCurrency(calculatedFee.value)}) lớn hơn phí ship ban đầu (${formatCurrency(originalShippingFee.value)}).\n\nĐơn hàng đã thanh toán trước (chuyển khoản / VNPAY) chỉ có thể đổi sang địa chỉ có cước bằng hoặc thấp hơn!`,
      'Không thể đổi địa chỉ',
      'error'
    )
  }

  // Cảnh báo chỉ được sửa 1 lần duy nhất
  const confirmMsg = `Lưu ý quan trọng: Quý khách chỉ có thể thay đổi thông tin nhận hàng DUY NHẤT 1 LẦN cho đơn hàng này.\n\nBạn có chắc chắn muốn đổi sang địa chỉ này không?`
  const ok = await showAppConfirm(confirmMsg, 'Xác nhận đổi thông tin nhận hàng')
  if (!ok) return

  submitting.value = true
  try {
    const addr = selectedAddress.value
    const fullAddress = [
      addr.diaChiCuThe ? addr.diaChiCuThe.trim() : '',
      addr.phuong ? addr.phuong.trim() : '',
      addr.thanhPho ? addr.thanhPho.trim() : ''
    ].filter(Boolean).join(', ')

    const payload = {
      tenNguoiNhan: (addr.tenNguoiNhan || customerDisplayName.value).trim(),
      soDienThoaiNhan: (addr.soDienThoaiNhan || props.order?.soDienThoaiKhachHang || '').trim(),
      diaChiNhanHang: fullAddress,
      ghiChu: ghiChuMoi.value ? ghiChuMoi.value.trim() : '',
      phiVanChuyenMoi: calculatedFee.value != null ? calculatedFee.value : originalShippingFee.value
    }

    const orderId = props.order?.id
    const orderCode = props.order?.maHoaDon

    let res = null
    if (orderCode) {
      res = await api.put(`/api/hoa-don/code/${orderCode}/khach-sua-thong-tin`, payload)
    } else if (orderId) {
      res = await api.put(`/api/hoa-don/${orderId}/khach-sua-thong-tin`, payload)
    } else {
      throw new Error('Không xác định được mã đơn hàng!')
    }

    showAppToast('Thay đổi thông tin nhận hàng thành công!', 'success')
    emit('updated', res.data)
    handleClose()
  } catch (err) {
    console.error('Lỗi cập nhật thông tin đơn hàng:', err)
    const msg = err?.response?.data?.message || err?.message || 'Không thể cập nhật thông tin đơn hàng.'
    showAppAlert(msg, 'Lỗi cập nhật', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="modelValue" class="client-addr-modal-backdrop" @click.self="handleClose">
    <div class="client-addr-modal-card animate-pop">
      <!-- HEADER CHUẨN THEO ẢNH MẪU 2 -->
      <div class="client-addr-modal-header">
        <h3 class="client-addr-modal-title">
          Quản lý địa chỉ: {{ customerDisplayName }}
        </h3>
        <button class="client-addr-close-btn" @click="handleClose" title="Đóng">&times;</button>
      </div>

      <!-- BODY -->
      <div class="client-addr-modal-body">
        <!-- Banner lưu ý chính sách sửa 1 lần duy nhất -->
        <div class="addr-policy-banner">
          <div class="banner-icon-box">ℹ️</div>
          <div class="banner-content-text">
            <strong>Chính sách đổi địa chỉ:</strong> Quý khách chỉ được phép chỉnh sửa thông tin nhận hàng <strong>1 lần duy nhất</strong> khi đơn hàng đang ở trạng thái <em>Chờ xác nhận</em>.
          </div>
        </div>

        <!-- Thanh tác vụ trên: Tổng cộng X địa chỉ + Nút Thêm địa chỉ mới -->
        <div class="addr-top-actions-bar">
          <span class="addr-total-count">
            Tổng cộng: {{ savedAddresses.length }} địa chỉ
          </span>
          <button
            v-if="!showAddressForm"
            type="button"
            class="btn-primary-addr-sm"
            @click="openAddAddressForm"
          >
            + Thêm địa chỉ mới
          </button>
        </div>

        <!-- FORM THÊM / SỬA ĐỊA CHỈ (CÓ THÊM THÔNG TIN NGƯỜI NHẬN) -->
        <div v-if="showAddressForm" class="addr-inline-form-box">
          <h4 class="addr-form-box-title">
            {{ editingAddressId ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}
          </h4>
          <div class="addr-form-grid-2">
            <!-- Thêm thông tin người nhận: Tên & SĐT -->
            <div class="addr-form-field">
              <label>Tên người nhận hàng <span class="req-star">*</span></label>
              <input
                v-model="addrForm.tenNguoiNhan"
                type="text"
                placeholder="Họ và tên người nhận"
                class="addr-form-input"
              />
            </div>

            <div class="addr-form-field">
              <label>Số điện thoại nhận hàng <span class="req-star">*</span></label>
              <input
                v-model="addrForm.soDienThoaiNhan"
                type="text"
                placeholder="Ví dụ: 0966580403"
                class="addr-form-input"
              />
            </div>

            <div class="addr-form-field">
              <label>Tỉnh / Thành phố <span class="req-star">*</span></label>
              <select
                v-model="addrForm.thanhPho"
                @change="onAddrProvinceChange"
                class="addr-form-select"
              >
                <option value="" disabled>-- Chọn Tỉnh / Thành phố --</option>
                <option v-for="p in provincesList" :key="p.name || p" :value="p.name || p">
                  {{ p.name || p }}
                </option>
              </select>
            </div>

            <div class="addr-form-field">
              <label>Phường / Xã <span class="req-star">*</span></label>
              <select
                v-model="addrForm.phuong"
                :disabled="!addrForm.thanhPho"
                class="addr-form-select"
              >
                <option value="" disabled>-- Chọn Phường / Xã --</option>
                <option v-for="w in addrWardsList" :key="w.name || w" :value="w.name || w">
                  {{ w.name || w }}
                </option>
              </select>
            </div>

            <div class="addr-form-field full-width">
              <label>Địa chỉ cụ thể (Số nhà, tên đường, thôn xóm) <span class="req-star">*</span></label>
              <input
                v-model="addrForm.diaChiCuThe"
                type="text"
                placeholder="Số nhà, ngõ, tên đường..."
                class="addr-form-input"
              />
            </div>

            <div class="addr-form-field full-width">
              <label class="addr-checkbox-label">
                <input type="checkbox" v-model="addrForm.macDinh" />
                <span>Đặt làm địa chỉ mặc định</span>
              </label>
            </div>
          </div>

          <div class="addr-form-button-row">
            <button type="button" class="btn-cancel-addr" @click="cancelAddressForm">
              Hủy
            </button>
            <button type="button" class="btn-save-addr" :disabled="savingAddress" @click="saveAddress">
              {{ savingAddress ? 'Đang lưu...' : 'Lưu địa chỉ' }}
            </button>
          </div>
        </div>

        <!-- DANH SÁCH ĐỊA CHỈ DẠNG THẺ (ĐÚNG THEO ẢNH MẪU 2) -->
        <div class="addr-items-scroll-list">
          <div v-if="loadingAddresses" class="addr-empty-state">
            Đang tải danh sách địa chỉ...
          </div>
          <div v-else-if="!savedAddresses.length && !showAddressForm" class="addr-empty-state">
            Chưa có địa chỉ nào. Nhấn "+ Thêm địa chỉ mới" để tạo.
          </div>
          <div
            v-for="addr in savedAddresses"
            :key="addr.id"
            class="addr-item-card"
            :class="{
              'is-default': addr.macDinh,
              'is-active-choice': selectedAddressId === addr.id
            }"
            @click="selectAddressForOrder(addr)"
          >
            <div class="addr-card-main-col">
              <div class="addr-card-header-row">
                <span v-if="addr.macDinh" class="addr-default-badge">Mặc định</span>
                <span class="addr-code-tag">{{ addr.maDiaChi || 'DC' }}</span>
                <span v-if="selectedAddressId === addr.id" class="addr-active-choice-badge">
                  ✓ Đang chọn nhận hàng
                </span>
              </div>

              <!-- Thông tin người nhận -->
              <div class="addr-card-recipient-text">
                <strong class="recipient-name">{{ addr.tenNguoiNhan || customerDisplayName }}</strong>
                <span class="recipient-phone"> • {{ addr.soDienThoaiNhan || props.order?.soDienThoaiKhachHang || '—' }}</span>
              </div>

              <!-- Chuỗi địa chỉ -->
              <div class="addr-card-address-text">
                <strong>{{ addr.diaChiCuThe }}</strong>, {{ [addr.phuong, addr.huyen, addr.thanhPho].filter(Boolean).join(', ') }}
              </div>
            </div>

            <!-- Các nút hành động bên phải -->
            <div class="addr-card-btn-actions" @click.stop>
              <button
                type="button"
                class="btn-text-act btn-select-addr"
                :class="{ 'btn-selected-highlight': selectedAddressId === addr.id }"
                @click="selectAddressForOrder(addr)"
              >
                {{ selectedAddressId === addr.id ? 'Đang chọn' : 'Giao đến đây' }}
              </button>

              <button
                v-if="!addr.macDinh && addr.id !== 'order_curr'"
                type="button"
                class="btn-text-act btn-set-default"
                @click="setAsDefault(addr.id)"
              >
                Đặt làm mặc định
              </button>

              <button
                type="button"
                class="btn-text-act btn-edit-addr"
                @click="openEditAddressForm(addr)"
              >
                Sửa
              </button>

              <button
                v-if="!addr.macDinh && addr.id !== 'order_curr'"
                type="button"
                class="btn-text-act btn-del-addr"
                @click="deleteAddress(addr.id)"
              >
                Xóa
              </button>
            </div>
          </div>
        </div>

        <!-- KHU VỰC TÍNH PHÍ SHIP & TỔNG TIỀN THEO ĐỊA CHỈ ĐANG CHỌN (ẢNH MẪU 1) -->
        <div v-if="selectedAddress" class="addr-order-calc-section">
          <!-- Banner phân biệt COD vs ĐÃ THANH TOÁN -->
          <div v-if="!isPaidOrder" class="payment-mode-banner cod-mode">
            <div class="pmb-top-row">
              <span class="pmb-badge cod-badge">THANH TOÁN KHI NHẬN HÀNG (COD)</span>
              <span class="pmb-orig-ship">Phí ship ban đầu: <strong>{{ formatCurrency(originalShippingFee) }}</strong></span>
            </div>
            <p class="pmb-desc">
              ✓ Đơn hàng thanh toán khi nhận hàng: Quý khách có thể đổi đến bất kỳ địa chỉ nào. Cước vận chuyển và tổng tiền thu COD sẽ tự động cập nhật lại.
            </p>
          </div>

          <div v-else class="payment-mode-banner paid-mode">
            <div class="pmb-top-row">
              <span class="pmb-badge paid-badge">ĐÃ THANH TOÁN TRƯỚC</span>
              <span class="pmb-orig-ship">Phí ship đã trả: <strong>{{ formatCurrency(originalShippingFee) }}</strong></span>
            </div>
            <p class="pmb-desc">
              ℹ️ Đơn hàng đã thanh toán: Cước phí địa chỉ mới phải <strong>bằng hoặc thấp hơn</strong> cước ban đầu ({{ formatCurrency(originalShippingFee) }}).
            </p>
          </div>

          <!-- Chi tiết cước GHN & Tổng COD -->
          <div class="shipping-fee-preview-box" :class="{ 'fee-warning-box': !isFeeAllowed }">
            <div class="sfp-row">
              <div class="sfp-left">
                <span class="ghn-icon">🚚</span>
                <div>
                  <div class="sfp-title">Cước phí vận chuyển GHN địa chỉ mới:</div>
                  <div class="sfp-sub">{{ loadingShipping ? 'Đang tính cước...' : shippingMessage }}</div>
                </div>
              </div>
              <div class="sfp-price" :class="{ 'text-red': !isFeeAllowed }">
                {{ loadingShipping ? '...' : formatCurrency(calculatedFee != null ? calculatedFee : originalShippingFee) }}
              </div>
            </div>

            <!-- Tổng COD nếu là đơn COD -->
            <div v-if="!isPaidOrder" class="cod-final-row">
              <span class="cod-label">💵 Tổng tiền cần thanh toán khi nhận hàng (COD):</span>
              <strong class="cod-val">{{ formatCurrency(newCodTotal) }}</strong>
            </div>

            <!-- Cảnh báo nếu đơn đã thanh toán bị vượt cước -->
            <div v-if="!isFeeAllowed" class="fee-error-alert">
              ⚠️ Không thể áp dụng địa chỉ này! Cước phí mới ({{ formatCurrency(calculatedFee) }}) vượt quá cước đã thanh toán ({{ formatCurrency(originalShippingFee) }}). Vui lòng chọn địa chỉ có khoảng cách tương đương hoặc gần hơn.
            </div>
          </div>

          <!-- Ô ghi chú bổ sung khi giao hàng -->
          <div class="order-edit-note-field">
            <label>Ghi chú giao hàng mới (Tùy chọn):</label>
            <input
              v-model="ghiChuMoi"
              type="text"
              placeholder="Ví dụ: Giao giờ hành chính, gọi trước khi đến..."
              class="addr-form-input note-input"
            />
          </div>

          <!-- Nút xác nhận đổi đơn hàng -->
          <div class="order-confirm-actions-row">
            <button type="button" class="btn-cancel-addr" @click="handleClose">
              Hủy bỏ
            </button>
            <button
              type="button"
              class="btn-confirm-order-addr"
              :disabled="submitting || loadingShipping || !isFeeAllowed"
              @click="handleConfirmOrderUpdate"
            >
              {{ submitting ? 'Đang cập nhật...' : 'Xác nhận cập nhật' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Modal backdrop & card */
.client-addr-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background-color: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, sans-serif;
}

.client-addr-modal-card {
  width: 100%;
  max-width: 680px;
  max-height: 92vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalScaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalScaleIn {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* Header */
.client-addr-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #f1ece4;
  background-color: #fcfbf9;
}

.client-addr-modal-title {
  font-size: 1.18rem;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0;
}

.client-addr-close-btn {
  background: none;
  border: none;
  font-size: 1.7rem;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}

.client-addr-close-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
}

/* Body */
.client-addr-modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Policy banner */
.addr-policy-banner {
  background: #fefce8;
  border: 1px solid #fef08a;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #854d0e;
  font-size: 0.88rem;
  line-height: 1.45;
}

.banner-icon-box {
  font-size: 1.1rem;
}

/* Top bar: total & add button */
.addr-top-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.addr-total-count {
  font-size: 0.95rem;
  font-weight: 600;
  color: #475569;
}

.btn-primary-addr-sm {
  background: #2b3a4a;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary-addr-sm:hover {
  background: #1e293b;
}

/* Box form thêm / sửa địa chỉ */
.addr-inline-form-box {
  background-color: #fdfbf7;
  border: 1px solid #eedecb;
  border-radius: 12px;
  padding: 18px;
}

.addr-form-box-title {
  font-size: 1rem;
  font-weight: 700;
  color: #2c445a;
  margin: 0 0 14px 0;
}

.addr-form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.addr-form-field {
  display: flex;
  flex-direction: column;
}

.addr-form-field.full-width {
  grid-column: span 2;
}

.addr-form-field label {
  font-size: 0.84rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.req-star {
  color: #ef4444;
}

.addr-form-select,
.addr-form-input {
  width: 100%;
  height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 0.92rem;
  color: #0f172a;
  background-color: #ffffff;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.addr-form-select:focus,
.addr-form-input:focus {
  border-color: #2563eb;
}

.addr-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  margin-top: 4px;
}

.addr-checkbox-label input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
}

.addr-form-button-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn-cancel-addr {
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-addr:hover {
  background-color: #f1f5f9;
}

.btn-save-addr {
  height: 38px;
  padding: 0 20px;
  border-radius: 8px;
  border: none;
  background-color: #2b3a4a;
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-save-addr:hover {
  background-color: #1e293b;
}

.btn-save-addr:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Danh sách địa chỉ dạng thẻ */
.addr-items-scroll-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 4px;
}

.addr-empty-state {
  text-align: center;
  padding: 24px 16px;
  color: #64748b;
  font-size: 0.92rem;
}

.addr-item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.addr-item-card:hover {
  border-color: #93c5fd;
  background-color: #fafcff;
}

.addr-item-card.is-default {
  background-color: #f8fafc;
}

.addr-item-card.is-active-choice {
  border: 2px solid #2563eb !important;
  background-color: #f0f7ff !important;
}

.addr-card-main-col {
  flex: 1;
}

.addr-card-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.addr-default-badge {
  background-color: #e0f2fe;
  color: #0369a1;
  border: 1px solid #7dd3fc;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 7px;
}

.addr-code-tag {
  font-size: 0.82rem;
  color: #94a3b8;
  font-weight: 600;
}

.addr-active-choice-badge {
  background-color: #dcfce7;
  color: #15803d;
  border: 1px solid #86efac;
  border-radius: 4px;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 7px;
}

.addr-card-recipient-text {
  font-size: 0.9rem;
  color: #1e293b;
  margin-bottom: 4px;
}

.recipient-name {
  font-weight: 700;
  color: #0f172a;
}

.recipient-phone {
  color: #475569;
  font-weight: 500;
}

.addr-card-address-text {
  font-size: 0.9rem;
  color: #334155;
  line-height: 1.45;
}

/* Nút hành động bên phải thẻ */
.addr-card-btn-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.btn-text-act {
  background: none;
  border: none;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
  transition: all 0.15s;
}

.btn-select-addr {
  background-color: #eff6ff;
  color: #2563eb;
}

.btn-select-addr:hover {
  background-color: #dbeafe;
}

.btn-selected-highlight {
  background-color: #2563eb !important;
  color: #ffffff !important;
  font-weight: 700;
}

.btn-set-default {
  color: #475569;
}

.btn-set-default:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.btn-edit-addr {
  color: #475569;
}

.btn-edit-addr:hover {
  background-color: #f1f5f9;
  color: #0f172a;
}

.btn-del-addr {
  color: #ef4444;
}

.btn-del-addr:hover {
  background-color: #fef2f2;
}

/* Panel tính cước & tổng thanh toán khi đã chọn địa chỉ */
.addr-order-calc-section {
  border-top: 1px dashed #cbd5e1;
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.payment-mode-banner {
  border-radius: 10px;
  padding: 12px 14px;
}

.payment-mode-banner.cod-mode {
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
}

.payment-mode-banner.paid-mode {
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.pmb-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.pmb-badge {
  font-size: 0.76rem;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.cod-badge {
  background-color: #2563eb;
  color: #ffffff;
}

.paid-badge {
  background-color: #16a34a;
  color: #ffffff;
}

.pmb-orig-ship {
  font-size: 0.88rem;
  color: #475569;
}

.pmb-desc {
  font-size: 0.84rem;
  color: #334155;
  margin: 0;
  line-height: 1.4;
}

.shipping-fee-preview-box {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 18px;
}

.fee-warning-box {
  background-color: #fff1f2;
  border-color: #fecdd3;
}

.sfp-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sfp-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ghn-icon {
  font-size: 1.5rem;
}

.sfp-title {
  font-size: 0.92rem;
  font-weight: 700;
  color: #1e293b;
}

.sfp-sub {
  font-size: 0.8rem;
  color: #64748b;
}

.sfp-price {
  font-size: 1.15rem;
  font-weight: 800;
  color: #2563eb;
}

.text-red {
  color: #dc2626 !important;
}

.cod-final-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #cbd5e1;
}

.cod-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e3a5f;
}

.cod-val {
  font-size: 1.25rem;
  font-weight: 800;
  color: #1e40af;
}

.fee-error-alert {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background-color: #fee2e2;
  color: #b91c1c;
  font-size: 0.84rem;
  font-weight: 600;
  line-height: 1.4;
}

.order-edit-note-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-edit-note-field label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
}

.note-input {
  font-size: 0.88rem;
}

.order-confirm-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
}

.btn-confirm-order-addr {
  height: 42px;
  padding: 0 24px;
  border-radius: 8px;
  border: none;
  background-color: #2563eb;
  color: #ffffff;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.3);
}

.btn-confirm-order-addr:hover:not(:disabled) {
  background-color: #1d4ed8;
}

.btn-confirm-order-addr:disabled {
  background-color: #94a3b8;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
