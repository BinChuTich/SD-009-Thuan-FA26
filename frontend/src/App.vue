<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import GlobalDialog from '@/components/GlobalDialog.vue'
import { showToast, showAlert } from '@/utils/dialog.js'
import { getCurrentUser, setCurrentUser, logout, isAdmin, getActiveShift, setActiveShift, clearActiveShift } from '@/utils/auth.js'
import api from '@/api.js'

const openMenus = ref({
  sanPham: true,
  thuocTinh: true,
  giamGia: false,
  lichLamViec: true
})

const toggleMenu = (menu) => {
  openMenus.value[menu] = !openMenus.value[menu]
}

const router = useRouter()
const route = useRoute()
const currentUser = ref(getCurrentUser())
const showUserDropdown = ref(false)
const userMenuRef = ref(null)

// Live Notifications State cho Chuông thông báo Admin (Đơn hàng mới + Live Chat)
const adminNotificationCount = ref(0)
const adminNotificationList = ref([])
const adminChatUnreadCount = ref(0)
const adminOrderNewCount = ref(0)
const bellFilter = ref('ALL') // 'ALL', 'DON_HANG', 'CHAT'
const showBellDropdown = ref(false)
const bellRef = ref(null)
let chatNotifyTimer = null
let isFirstBellFetch = true

function playAdminChatChime() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(523.25, ctx.currentTime)
    osc.frequency.setValueAtTime(659.25, ctx.currentTime + 0.1)
    osc.frequency.setValueAtTime(783.99, ctx.currentTime + 0.2)
    gain.gain.setValueAtTime(0.08, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.45)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.45)
  } catch (e) {}
}

async function fetchAdminChatNotifications() {
  try {
    const res = await api.get('/api/chat/thong-bao-admin')
    if (res.data) {
      const newTotal = res.data.tongSoThongBao ?? ((res.data.soTinChuaDoc || 0) + (res.data.soDonHangMoi || 0))
      if (!isFirstBellFetch && newTotal > adminNotificationCount.value) {
        playAdminChatChime()
      }
      isFirstBellFetch = false
      adminNotificationCount.value = newTotal
      adminChatUnreadCount.value = res.data.soTinChuaDoc || 0
      adminOrderNewCount.value = res.data.soDonHangMoi || 0
      adminNotificationList.value = res.data.danhSachThongBao || []
    }
  } catch (e) {}
}

const filteredNotificationList = computed(() => {
  if (bellFilter.value === 'DON_HANG') {
    return adminNotificationList.value.filter(item => item.loai === 'DON_HANG')
  }
  if (bellFilter.value === 'CHAT') {
    return adminNotificationList.value.filter(item => item.loai === 'CHAT')
  }
  return adminNotificationList.value
})

function toggleBellDropdown() {
  showBellDropdown.value = !showBellDropdown.value
  if (showBellDropdown.value) {
    fetchAdminChatNotifications()
  }
}

function handleNotificationClick(item) {
  showBellDropdown.value = false
  if (item.loai === 'DON_HANG') {
    if (item.maDonHang) {
      router.push({ path: '/hoa-don', query: { search: item.maDonHang } })
    } else {
      router.push('/hoa-don')
    }
  } else {
    if (item.cuocHoiThoaiId) {
      router.push({ path: '/tin-nhan', query: { id: item.cuocHoiThoaiId } })
    } else {
      router.push('/tin-nhan')
    }
  }
}

function goToOrdersOrChat() {
  showBellDropdown.value = false
  if (bellFilter.value === 'DON_HANG' || (bellFilter.value === 'ALL' && adminOrderNewCount.value > 0)) {
    router.push('/hoa-don')
  } else {
    router.push('/tin-nhan')
  }
  return
}
function _unused_goTo() {
  showBellDropdown.value = false
  if (bellFilter.value === 'DON_HANG') {
    router.push('/hoa-don')
  } else {
    router.push('/tin-nhan')
  }
}

const isUserAdmin = computed(() => {
  if (!currentUser.value) return false
  return isAdmin()
})

function getAvatarInitials(name) {
  if (!name) return 'U'
  const parts = name.trim().split(/\s+/)
  if (parts.length >= 2) {
    const first = parts[parts.length - 2][0]
    const second = parts[parts.length - 1][0]
    return (first + second).toUpperCase()
  }
  return parts[0].slice(0, 2).toUpperCase()
}

function handleClickOutside(e) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserDropdown.value = false
  }
  if (bellRef.value && !bellRef.value.contains(e.target)) {
    showBellDropdown.value = false
  }
}

function onAuthChanged(e) {
  currentUser.value = e.detail
  if (currentUser.value) {
    checkActiveShiftAndPrompt()
  } else {
    currentOpenShift.value = null
    showMoCaPopup.value = false
  }
}

onMounted(() => {
  currentUser.value = getCurrentUser()
  window.addEventListener('auth-changed', onAuthChanged)
  window.addEventListener('click', handleClickOutside)

  updateClock()
  clockTimer = setInterval(updateClock, 1000)

  if (currentUser.value) {
    checkActiveShiftAndPrompt()
  }

  window.addEventListener('shift-closed', handleShiftClosed)
  window.addEventListener('shift-opened', handleShiftOpened)
  window.addEventListener('open-mo-ca-popup', handleOpenMoCaEvent)
  window.addEventListener('view-mode-blocked', handleViewModeBlocked)

  fetchAdminChatNotifications()
  chatNotifyTimer = setInterval(fetchAdminChatNotifications, 4000)
  window.addEventListener('admin-chat-read', fetchAdminChatNotifications)
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', onAuthChanged)
  window.removeEventListener('click', handleClickOutside)
  if (clockTimer) clearInterval(clockTimer)
  window.removeEventListener('shift-closed', handleShiftClosed)
  window.removeEventListener('shift-opened', handleShiftOpened)
  window.removeEventListener('open-mo-ca-popup', handleOpenMoCaEvent)
  window.removeEventListener('view-mode-blocked', handleViewModeBlocked)
  if (chatNotifyTimer) clearInterval(chatNotifyTimer)
  window.removeEventListener('admin-chat-read', fetchAdminChatNotifications)
})

function handleLogout() {
  logout()
  router.push('/login')
  showUserDropdown.value = false
}

function goToLoginPage() {
  router.push('/login')
}

function handleLoginAdmin() {
  const defaultAdmin = {
    id: 1,
    maNhanVien: 'NV001',
    tenNhanVien: 'Nguyễn Văn Admin',
    tenTaiKhoan: 'admin',
    email: 'admin@ffshirt.com',
    soDienThoai: '0900000001',
    role: 'Quản trị viên',
    trangThai: 1
  }
  setCurrentUser(defaultAdmin)
  showUserDropdown.value = false
}

// ================== QUẢN LÝ POPUP MỞ CA LÀM VIỆC & CHẾ ĐỘ XEM ==================
const showMoCaPopup = ref(false)
const currentOpenShift = ref(getActiveShift())
const isEmployeeInViewMode = computed(() => {
  if (!currentUser.value) return false
  if (isUserAdmin.value) return false
  return !currentOpenShift.value
})
const assignedShiftsToday = ref([])
const hasScheduleToday = ref(false)
const activeShifts = ref([])
const activeEmployees = ref([])
const lastClosedShift = ref(null)
const submittingMoCa = ref(false)

function getNowTime() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

const currentTimeStr = ref(getNowTime())
const currentDateStr = ref('')
let clockTimer = null

const moCaForm = ref({
  nhanVienId: null,
  caLamViecId: '',
  tienMatDauCa: 0,
  tienChuyenKhoan: 0,
  ghiChu: ''
})

function updateClock() {
  currentTimeStr.value = getNowTime()
  const now = new Date()
  const dd = String(now.getDate()).padStart(2, '0')
  const mo = String(now.getMonth() + 1).padStart(2, '0')
  const yyyy = now.getFullYear()
  currentDateStr.value = `${dd}/${mo}/${yyyy}`
}

const formatCurrency = (val) => {
  if (val === null || val === undefined || isNaN(val)) return '0 đ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

const formatTime = (timeStr) => {
  if (!timeStr) return '--:--'
  if (typeof timeStr === 'string' && timeStr.length >= 5) {
    return timeStr.substring(0, 5)
  }
  return timeStr
}

const displayTienMatDauCa = computed({
  get: () => {
    if (moCaForm.value.tienMatDauCa === null || moCaForm.value.tienMatDauCa === undefined) return ''
    return new Intl.NumberFormat('vi-VN').format(moCaForm.value.tienMatDauCa)
  },
  set: (val) => {
    if (!val) {
      moCaForm.value.tienMatDauCa = 0
      return
    }
    const clean = String(val).replace(/[^0-9]/g, '')
    moCaForm.value.tienMatDauCa = clean ? parseInt(clean, 10) : 0
  }
})

const displayTienTaiKhoanDauCa = computed({
  get: () => {
    if (moCaForm.value.tienChuyenKhoan === null || moCaForm.value.tienChuyenKhoan === undefined) return ''
    return new Intl.NumberFormat('vi-VN').format(moCaForm.value.tienChuyenKhoan)
  },
  set: (val) => {
    if (!val) {
      moCaForm.value.tienChuyenKhoan = 0
      return
    }
    const clean = String(val).replace(/[^0-9]/g, '')
    moCaForm.value.tienChuyenKhoan = clean ? parseInt(clean, 10) : 0
  }
})

function getShiftStatus(ca) {
  if (!ca || !ca.gioBatDau || !ca.gioKetThuc) {
    return { status: 'unknown', label: 'Chưa có giờ ca', canOpen: true }
  }

  const now = new Date()
  const nowMinutes = now.getHours() * 60 + now.getMinutes()

  const parseMinutes = (str) => {
    const [h, m] = str.split(':').map(Number)
    return h * 60 + (m || 0)
  }

  const startMinutes = parseMinutes(ca.gioBatDau)
  const endMinutes = parseMinutes(ca.gioKetThuc)
  const earlyMinutes = startMinutes - 30

  if (endMinutes > startMinutes) {
    const allowedEnd = endMinutes + 30
    if (allowedEnd >= 1440 && nowMinutes <= allowedEnd - 1440) {
      return { status: 'current', label: 'Đang trong khung giờ ca', canOpen: true }
    }
    if (earlyMinutes < 0 && nowMinutes >= earlyMinutes + 1440) {
      return { status: 'current', label: 'Đang trong khung giờ ca', canOpen: true }
    }
    if (nowMinutes < earlyMinutes) {
      return {
        status: 'future',
        label: `Chưa đến giờ (bắt đầu lúc ${ca.gioBatDau.substring(0, 5)})`,
        canOpen: false
      }
    } else if (nowMinutes > allowedEnd) {
      return {
        status: 'past',
        label: `Đã quá giờ ca (kết thúc lúc ${ca.gioKetThuc.substring(0, 5)})`,
        canOpen: false
      }
    } else {
      return {
        status: 'current',
        label: 'Đang trong khung giờ ca',
        canOpen: true
      }
    }
  } else {
    if (nowMinutes >= earlyMinutes || nowMinutes <= endMinutes + 30) {
      return { status: 'current', label: 'Đang trong khung giờ ca', canOpen: true }
    } else {
      return { status: 'future', label: `Chưa đến giờ (bắt đầu lúc ${ca.gioBatDau.substring(0, 5)})`, canOpen: false }
    }
  }
}

const currentMatchedShift = computed(() => {
  const sourceList = isUserAdmin.value ? activeShifts.value : assignedShiftsToday.value
  if (!sourceList || sourceList.length === 0) return null
  const currentValid = sourceList.find(c => getShiftStatus(c).canOpen)
  if (currentValid) return currentValid
  if (moCaForm.value.caLamViecId) {
    const found = sourceList.find(c => c.id === Number(moCaForm.value.caLamViecId))
    if (found) return found
  }
  return sourceList[0] || null
})

const selectedShiftStatus = computed(() => {
  if (!moCaForm.value.caLamViecId) return null
  const sourceList = isUserAdmin.value ? activeShifts.value : assignedShiftsToday.value
  const ca = sourceList.find(c => c.id === Number(moCaForm.value.caLamViecId))
  return ca ? getShiftStatus(ca) : null
})

const canSubmitMoCa = computed(() => {
  if (submittingMoCa.value) return false
  if (!currentUser.value?.id || !currentMatchedShift.value?.id) return false
  if (!isUserAdmin.value && !hasScheduleToday.value) return false
  if (moCaForm.value.tienMatDauCa === null || moCaForm.value.tienMatDauCa === undefined || moCaForm.value.tienMatDauCa < 0) return false
  if (!isUserAdmin.value && selectedShiftStatus.value && !selectedShiftStatus.value.canOpen) return false
  return true
})

async function initMoCaData() {
  if (!currentUser.value?.id) return
  try {
    moCaForm.value.nhanVienId = currentUser.value.id

    const [empRes, caRes, lastShiftRes] = await Promise.all([
      api.get('/api/nhan-vien').catch(() => ({ data: [] })),
      api.get('/api/ca-lam-viec').catch(() => ({ data: [] })),
      api.get('/api/lich-su-ca/last-closed').catch(() => api.get('/api/lich-su-ca/ca-dong-gan-nhat').catch(() => ({ data: null })))
    ])

    activeEmployees.value = Array.isArray(empRes.data)
      ? empRes.data.filter(e => e.trangThai === 1 || e.trangThai === true)
      : []

    activeShifts.value = Array.isArray(caRes.data)
      ? caRes.data.filter(c => c.trangThai === 1 || c.trangThai === true)
      : []

    lastClosedShift.value = lastShiftRes?.data || null

    if (lastClosedShift.value && lastClosedShift.value.tienMatCuoiCa !== undefined && lastClosedShift.value.tienMatCuoiCa !== null) {
      moCaForm.value.tienMatDauCa = Number(lastClosedShift.value.tienMatCuoiCa) || 0
    }

    if (!isUserAdmin.value) {
      await loadTodayShiftsForEmployee(currentUser.value.id)
    } else if (activeShifts.value.length > 0 && !moCaForm.value.caLamViecId) {
      moCaForm.value.caLamViecId = activeShifts.value[0].id
    }
  } catch (e) {
    console.error('Lỗi khi nạp dữ liệu mở ca:', e)
  }
}

async function loadTodayShiftsForEmployee(empId) {
  if (!empId) {
    assignedShiftsToday.value = []
    hasScheduleToday.value = false
    moCaForm.value.caLamViecId = ''
    return
  }
  try {
    const res = await api.get(`/api/lich-lam-viec/hom-nay/${empId}`)
    const schedules = Array.isArray(res.data) ? res.data : []
    const caList = schedules.map(s => s.caLamViec).filter(Boolean)

    const uniqueMap = new Map()
    caList.forEach(c => uniqueMap.set(c.id, c))
    assignedShiftsToday.value = Array.from(uniqueMap.values())
    hasScheduleToday.value = assignedShiftsToday.value.length > 0

    if (hasScheduleToday.value) {
      const currentValid = assignedShiftsToday.value.find(c => getShiftStatus(c).canOpen)
      moCaForm.value.caLamViecId = currentValid ? currentValid.id : assignedShiftsToday.value[0].id
    } else {
      moCaForm.value.caLamViecId = ''
    }
  } catch (err) {
    console.warn('Lỗi khi tải lịch ca hôm nay của nhân viên:', err)
    assignedShiftsToday.value = []
    hasScheduleToday.value = false
    moCaForm.value.caLamViecId = ''
  }
}

function applyLastClosedCash() {
  if (lastClosedShift.value && lastClosedShift.value.tienMatCuoiCa !== undefined && lastClosedShift.value.tienMatCuoiCa !== null) {
    moCaForm.value.tienMatDauCa = Number(lastClosedShift.value.tienMatCuoiCa) || 0
  }
}

function onAdminEmployeeChange() {
  if (moCaForm.value.nhanVienId) {
    loadTodayShiftsForEmployee(moCaForm.value.nhanVienId)
  }
}

function handleCheDoXem() {
  showMoCaPopup.value = false
  if (currentUser.value?.id) {
    sessionStorage.setItem('dismissed_mo_ca_' + currentUser.value.id, 'true')
  }
}

async function openMoCaModal() {
  showMoCaPopup.value = true
  await initMoCaData()
}

async function checkActiveShiftAndPrompt() {
  if (!currentUser.value?.id) return
  try {
    const res = await api.get(`/api/lich-su-ca/current/${currentUser.value.id}`)
    if (res.data && res.data.id) {
      currentOpenShift.value = res.data
      setActiveShift(res.data)
      showMoCaPopup.value = false
      return
    } else {
      currentOpenShift.value = null
      clearActiveShift()
    }
  } catch (e) {
    console.warn('Lỗi kiểm tra ca làm việc:', e)
    return
  }

  if (!isUserAdmin.value) {
    const isDismissed = sessionStorage.getItem('dismissed_mo_ca_' + currentUser.value.id)
    if (!isDismissed) {
      showMoCaPopup.value = true
      await initMoCaData()
    }
  }
}

async function submitMoCaFromPopup() {
  if (!canSubmitMoCa.value) return
  try {
    submittingMoCa.value = true
    const payload = {
      nhanVienId: isUserAdmin.value && moCaForm.value.nhanVienId ? Number(moCaForm.value.nhanVienId) : currentUser.value.id,
      caLamViecId: Number(currentMatchedShift.value?.id || moCaForm.value.caLamViecId),
      tienMatDauCa: Number(moCaForm.value.tienMatDauCa) || 0,
      tienChuyenKhoan: Number(moCaForm.value.tienChuyenKhoan) || 0,
      ghiChu: 'Mở ca từ hệ thống'
    }

    const res = await api.post('/api/lich-su-ca/mo-ca', payload)
    currentOpenShift.value = res.data
    setActiveShift(res.data)
    showMoCaPopup.value = false
    window.dispatchEvent(new CustomEvent('shift-opened', { detail: res.data }))
    showToast('Vào ca làm việc thành công!', 'success')
  } catch (err) {
    console.error('Lỗi khi mở ca:', err)
    const msg = err.response?.data?.message || err.response?.data?.error || err.message || 'Mở ca thất bại'
    showToast(msg, 'warning')
  } finally {
    submittingMoCa.value = false
  }
}

function handleShiftClosed() {
  currentOpenShift.value = null
  clearActiveShift()
  checkActiveShiftAndPrompt()
}

function handleShiftOpened(e) {
  currentOpenShift.value = e.detail
  if (e.detail) {
    setActiveShift(e.detail)
  }
  showMoCaPopup.value = false
}

function handleOpenMoCaEvent() {
  openMoCaModal()
}

function handleViewModeBlocked(e) {
  showToast(e.detail?.message || 'Bạn đang ở Chế độ xem (Chưa vào ca làm việc). Vui lòng vào ca làm việc để thực hiện!', 'warning')
  openMoCaModal()
}

// Bắt và ngăn chặn các nút thao tác nghiệp vụ khi nhân viên đang ở Chế độ xem
function handleViewModeGlobalClick(e) {
  if (!isEmployeeInViewMode.value) return

  const target = e.target.closest('button, input[type="submit"], a.btn, .btn-checkout, .btn-pay, .btn-create-order, .btn-add, .btn-delete, .btn-save')
  if (!target) return

  // Bỏ qua các nút xem, điều hướng trang, phân trang, lọc, in hoặc đóng modal
  if (
    target.closest('.pagination, .filter-box, .btn-view, .btn-history-link, .btn-pos-link, .btn-refresh-clean, .tab-btn, .search-box, .modal-footer-clean') ||
    target.classList.contains('btn-view') ||
    target.classList.contains('btn-text-more') ||
    target.classList.contains('btn-header-view-mode') ||
    target.classList.contains('btn-banner-open-ca') ||
    target.classList.contains('btn-che-do-xem') ||
    target.classList.contains('btn-cancel-modal') ||
    target.classList.contains('btn-print-action')
  ) {
    return
  }

  const text = (target.innerText || target.value || target.title || '').trim().toLowerCase()
  const isMutation = target.type === 'submit' ||
                     target.classList.contains('btn-submit') ||
                     target.classList.contains('btn-checkout') ||
                     target.classList.contains('btn-pay') ||
                     target.classList.contains('btn-confirm') ||
                     target.classList.contains('btn-add') ||
                     target.classList.contains('btn-delete') ||
                     text.includes('thanh toán') ||
                     text.includes('thêm') ||
                     text.includes('tạo đơn') ||
                     text.includes('tạo mới') ||
                     text.includes('lưu') ||
                     text.includes('hủy đơn') ||
                     text.includes('xác nhận đơn') ||
                     text.includes('cập nhật')

  if (isMutation) {
    e.preventDefault()
    e.stopPropagation()
    showToast('Bạn đang ở Chế độ xem (Chưa vào ca làm việc). Vui lòng vào ca làm việc để thực hiện!', 'warning')
    openMoCaModal()
  }
}

// ================== QUẢN LÝ POPUP ĐÓNG CA & BÀN GIAO TIỀN MẶT ==================
const showDongCaModal = ref(false)
const showReceiptModal = ref(false)
const receiptShift = ref(null)
const shiftSummary = ref(null)
const loadingShiftSummary = ref(false)
const submittingDongCa = ref(false)

const dongCaForm = ref({
  nhanVienNhanId: '',
  tienChiTrongCa: 0,
  tienMatCuoiCa: null,
  lyDoChenhLech: '',
  ghiChu: ''
})

const formatDateTime = (val) => {
  if (!val) return '-'
  try {
    const s = String(val).replace('T', ' ')
    return s.substring(0, 19)
  } catch (e) {
    return val
  }
}

const handoverEmployees = computed(() => {
  if (!currentOpenShift.value) return activeEmployees.value
  const empId = currentOpenShift.value.nhanVien?.id || currentUser.value?.id
  return activeEmployees.value.filter(nv => nv.id !== empId)
})

const calculatedExpectedCash = computed(() => {
  const start = Number(currentOpenShift.value?.tienMatDauCa || 0)
  const posCash = Number(shiftSummary.value?.tienMatHeThong || 0)
  const chi = Number(dongCaForm.value.tienChiTrongCa || 0)
  return Math.max(0, start + posCash - chi)
})

const calculatedDiff = computed(() => {
  if (dongCaForm.value.tienMatCuoiCa === null || dongCaForm.value.tienMatCuoiCa === undefined || dongCaForm.value.tienMatCuoiCa < 0) {
    return 0
  }
  const counted = Number(dongCaForm.value.tienMatCuoiCa || 0)
  return counted - calculatedExpectedCash.value
})

const canSubmitDongCa = computed(() => {
  if (submittingDongCa.value) return false
  if (dongCaForm.value.tienMatCuoiCa === null || dongCaForm.value.tienMatCuoiCa === undefined || dongCaForm.value.tienMatCuoiCa < 0) return false
  if (calculatedDiff.value !== 0 && (!dongCaForm.value.lyDoChenhLech || !dongCaForm.value.lyDoChenhLech.trim())) return false
  return true
})

const displayTienChiTrongCa = computed({
  get: () => {
    if (dongCaForm.value.tienChiTrongCa === null || dongCaForm.value.tienChiTrongCa === undefined || dongCaForm.value.tienChiTrongCa === '') return ''
    if (dongCaForm.value.tienChiTrongCa === 0) return '0'
    return Number(dongCaForm.value.tienChiTrongCa).toLocaleString('vi-VN')
  },
  set: (val) => {
    if (!val) {
      dongCaForm.value.tienChiTrongCa = 0
      return
    }
    const clean = String(val).replace(/\D/g, '')
    dongCaForm.value.tienChiTrongCa = clean !== '' ? parseInt(clean, 10) : 0
  }
})

const displayTienMatCuoiCa = computed({
  get: () => {
    if (dongCaForm.value.tienMatCuoiCa === null || dongCaForm.value.tienMatCuoiCa === undefined || dongCaForm.value.tienMatCuoiCa === '') return ''
    return Number(dongCaForm.value.tienMatCuoiCa).toLocaleString('vi-VN')
  },
  set: (val) => {
    if (!val) {
      dongCaForm.value.tienMatCuoiCa = null
      return
    }
    const clean = String(val).replace(/\D/g, '')
    dongCaForm.value.tienMatCuoiCa = clean !== '' ? parseInt(clean, 10) : null
  }
})

async function loadEmployeesForHandover() {
  if (activeEmployees.value.length > 0) return
  try {
    const res = await api.get('/api/nhan-vien').catch(() => ({ data: [] }))
    const raw = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    activeEmployees.value = raw.filter(e => e.trangThai === 1 || e.trangThai === true)
  } catch (e) {
    console.error('Lỗi khi tải danh sách nhân viên:', e)
  }
}

async function loadShiftSummary(shiftId) {
  if (!shiftId) return
  loadingShiftSummary.value = true
  try {
    const res = await api.get(`/api/lich-su-ca/${shiftId}/summary`)
    shiftSummary.value = res.data || null
    if (shiftSummary.value?.nhanVienNhanDuKienId) {
      dongCaForm.value.nhanVienNhanId = shiftSummary.value.nhanVienNhanDuKienId
    }
  } catch (e) {
    console.error('Lỗi khi tải tổng hợp két tiền:', e)
  } finally {
    loadingShiftSummary.value = false
  }
}

async function openDongCaModal() {
  if (!currentOpenShift.value) {
    showToast('Không có ca làm việc nào đang mở!', 'warning')
    return
  }
  dongCaForm.value = {
    nhanVienNhanId: '',
    tienChiTrongCa: 0,
    tienMatCuoiCa: null,
    lyDoChenhLech: '',
    ghiChu: ''
  }
  showDongCaModal.value = true
  await Promise.all([
    loadShiftSummary(currentOpenShift.value.id),
    loadEmployeesForHandover()
  ])
}

async function submitDongCaFromModal() {
  if (!canSubmitDongCa.value) return
  submittingDongCa.value = true
  try {
    const payload = {
      id: currentOpenShift.value?.id,
      nhanVienId: currentOpenShift.value?.nhanVien?.id || currentUser.value?.id,
      caLamViecId: currentOpenShift.value?.caLamViec?.id,
      nhanVienNhanId: shiftSummary.value?.nhanVienNhanDuKienId || (dongCaForm.value.nhanVienNhanId ? Number(dongCaForm.value.nhanVienNhanId) : null),
      tienChiTrongCa: Number(dongCaForm.value.tienChiTrongCa || 0),
      tienMatCuoiCa: Number(dongCaForm.value.tienMatCuoiCa),
      chenhLech: calculatedDiff.value,
      lyDoChenhLech: calculatedDiff.value !== 0 ? dongCaForm.value.lyDoChenhLech.trim() : null,
      ghiChu: dongCaForm.value.ghiChu ? dongCaForm.value.ghiChu.trim() : null
    }

    const res = await api.post('/api/lich-su-ca/dong-ca', payload)
    showToast('Kết thúc ca & Bàn giao tiền mặt thành công!', 'success')
    const closedShiftData = res.data

    showDongCaModal.value = false
    currentOpenShift.value = null
    clearActiveShift()

    window.dispatchEvent(new CustomEvent('shift-closed', { detail: closedShiftData }))

    if (closedShiftData) {
      receiptShift.value = closedShiftData
      showReceiptModal.value = true
    }
  } catch (err) {
    console.error('Lỗi khi đóng ca:', err)
    const msg = err.response?.data?.message || err.response?.data?.error || err.message || 'Không thể đóng ca làm việc'
    showToast(msg, 'warning')
  } finally {
    submittingDongCa.value = false
  }
}

function calculateReceiptExpected(s) {
  if (!s) return 0
  const start = Number(s.tienMatDauCa || 0)
  const thu = Number(s.tongTienTrongCa || s.tienMatThuTrongCa || 0)
  const chi = Number(s.tienChiTrongCa || 0)
  return Math.max(0, start + thu - chi)
}

function printReceipt() {
  window.print()
}
</script>

<template>
  <div v-if="route.path === '/login'">
    <router-view />
    <GlobalDialog />
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="logo-box">
        <div class="logo-wrapper">
          <!-- 3 vạch tia sáng phát ra phía trên bên trái -->
          <div class="sparkles">
            <span class="ray ray-1"></span>
            <span class="ray ray-2"></span>
            <span class="ray ray-3"></span>
          </div>

          <div class="logo-content">
            <div class="logo-name">
              <span class="f-blue">F</span>
              <span class="f-gold">F</span>
            </div>
            <div class="logo-sub">T-shirt</div>
          </div>
        </div>
      </div>

      <nav class="menu">
        <!-- 1. Tổng quan (Chỉ Quản trị viên) -->
        <router-link v-if="isUserAdmin" to="/thong-ke" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z"/>
            </svg>
          </span>
          <span class="menu-text">Thống kê</span>
        </router-link>

        <!-- 2. Bán Hàng Tại Quầy (Cả Quản trị viên và Nhân viên) -->
        <router-link to="/ban-hang" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z"/>
            </svg>
          </span>
          <span class="menu-text">Bán Hàng Tại Quầy</span>
        </router-link>

        <!-- 3. Quản Lý Hóa Đơn (Cả Quản trị viên và Nhân viên) -->
        <router-link to="/hoa-don" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm7 17H5V8h2v2c0 .55.45 1 1 1s1-.45 1-1V8h6v2c0 .55.45 1 1 1s1-.45 1-1V8h2v12z"/>
            </svg>
          </span>
          <span class="menu-text">Quản Lý Hóa Đơn</span>
        </router-link>

        <!-- 4. Quản Lý Sản Phẩm (Cả Quản trị viên và Nhân viên xem) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('sanPham')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M20 2H4c-1.1 0-2 .9-2 2v3c0 .55.45 1 1 1h1v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8h1c.55 0 1-.45 1-1V4c0-1.1-.9-2-2-2zm-1 6v12H5V8h14zM4 4h16v2H4V4zm5 6h6v2H9v-2z"/>
              </svg>
            </span>
            <span class="menu-text">Quản Lý Sản phẩm</span>
            <span class="arrow-icon" :class="{ open: openMenus.sanPham }">⌄</span>
          </div>

          <!-- Menu con của Sản phẩm -->
          <div class="submenu-tree" v-show="openMenus.sanPham">
            <router-link to="/san-pham" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Sản phẩm</span>
            </router-link>
            <router-link to="/bien-the-san-pham" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Biến thể sản phẩm</span>
            </router-link>
          </div>
        </div>

        <!-- 5. Quản lý thuộc tính (Chỉ Quản trị viên) -->
        <div v-if="isUserAdmin" class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('thuocTinh')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l-5.5 9h11z M3 13.5h8v8H3z M17.5 13.5a4 4 0 1 1 0 8 4 4 0 0 1 0-8z"/>
              </svg>
            </span>
            <span class="menu-text">Quản lý thuộc tính</span>
            <span class="arrow-icon" :class="{ open: openMenus.thuocTinh }">⌄</span>
          </div>

          <!-- Menu con của Thuộc tính -->
          <div class="submenu-tree" v-show="openMenus.thuocTinh">
            <router-link to="/thuong-hieu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Thương hiệu</span>
            </router-link>
            <router-link to="/danh-muc" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Danh mục</span>
            </router-link>
            <router-link to="/chat-lieu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Chất liệu</span>
            </router-link>
            <router-link to="/xuat-xu" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Xuất xứ</span>
            </router-link>
            <router-link to="/co-ao" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Cổ áo</span>
            </router-link>
            <router-link to="/tay-ao" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Tay áo</span>
            </router-link>
            <router-link to="/hoa-tiet" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Họa tiết</span>
            </router-link>
            <router-link to="/mau-sac" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Màu sắc</span>
            </router-link>
            <router-link to="/kich-co" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Kích cỡ</span>
            </router-link>
          </div>
        </div>

        <!-- 6. Quản lý khách hàng (Cả Quản trị viên và Nhân viên) -->
        <router-link to="/khach-hang" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </span>
          <span class="menu-text">Quản lý khách hàng</span>
        </router-link>

        <!-- 7. Quản lý nhân viên (Chỉ Quản trị viên) -->
        <router-link v-if="isUserAdmin" to="/nhan-vien" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
            </svg>
          </span>
          <span class="menu-text">Quản lý nhân viên</span>
        </router-link>

        <!-- 8. Quản Lý Giảm Giá (Chỉ Quản trị viên) -->
        <div v-if="isUserAdmin" class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('giamGia')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"/>
              </svg>
            </span>
            <span class="menu-text">Quản Lý Giảm Giá</span>
            <span class="arrow-icon" :class="{ open: openMenus.giamGia }">⌄</span>
          </div>
          <div class="submenu-tree" v-show="openMenus.giamGia">
            <router-link to="/dot-giam-gia" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Đợt giảm giá</span>
            </router-link>
            <router-link to="/phieu-giam-gia" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Phiếu giảm giá</span>
            </router-link>
          </div>
        </div>

        <!-- 9. Lịch Làm Việc (Cả Quản trị viên và Nhân viên) -->
        <div class="menu-group">
          <div class="menu-item has-sub" @click="toggleMenu('lichLamViec')">
            <span class="menu-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z"/>
              </svg>
            </span>
            <span class="menu-text">Lịch Làm Việc</span>
            <span class="arrow-icon" :class="{ open: openMenus.lichLamViec }">⌄</span>
          </div>
          <div class="submenu-tree" v-show="openMenus.lichLamViec">
            <!-- Quản lý ca làm việc (Chỉ Quản trị viên) -->
            <router-link v-if="isUserAdmin" to="/ca-lam-viec" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Quản lý ca làm việc</span>
            </router-link>
            <!-- Lịch làm việc (Cả Quản trị viên và Nhân viên xem) -->
            <router-link to="/xep-lich" class="tree-item" active-class="active">
              <span class="tree-branch">├─</span>
              <span>Lịch làm việc</span>
            </router-link>
            <!-- Lịch sử hoạt động (Cả Quản trị viên và Nhân viên) -->
            <router-link to="/lich-su-ca" class="tree-item" active-class="active">
              <span class="tree-branch">└─</span>
              <span>Lịch sử hoạt động</span>
            </router-link>
          </div>
        </div>

        <!-- 10. Quản Lý Chat (Cuối menu) -->
        <router-link to="/tin-nhan" class="menu-item" active-class="active">
          <span class="menu-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>
            </svg>
          </span>
          <span class="menu-text">Quản Lý Chat</span>
          <span v-if="adminChatUnreadCount > 0" class="sidebar-chat-badge">{{ adminChatUnreadCount }}</span>
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <header class="top-header">
        <div class="header-right">
          <!-- Nút chế độ xem / Trạng thái ca làm việc theo ảnh mẫu (chỉ hiện cho nhân viên, ẩn với quản trị viên) -->
          <div v-if="currentUser && !isUserAdmin" class="header-shift-action">
            <button
              v-if="!currentOpenShift"
              type="button"
              class="btn-header-view-mode"
              @click="openMoCaModal"
              title="Chế độ xem - Bấm để mở ca làm việc"
            >
              Chế độ xem
            </button>
            <div v-else class="header-shift-in-progress">
              <div class="active-shift-pill" title="Đang trong ca làm việc">
                <span class="active-shift-dot"></span>
                <span class="active-shift-name">{{ currentOpenShift.caLamViec?.tenCa || 'Đang trong ca' }}</span>
                <span class="active-shift-divider">•</span>
                <span class="active-shift-clock">{{ currentTimeStr }}</span>
              </div>
              <button
                type="button"
                class="btn-header-end-shift"
                @click="openDongCaModal"
                title="Bấm để kết thúc ca và bàn giao tiền mặt"
              >
                Kết thúc ca
              </button>
            </div>
          </div>

          <!-- Chuông thông báo tổng hợp (Đơn hàng mới & Tin nhắn Chat) -->
          <div class="header-chat-bell-wrapper" ref="bellRef">
            <button
              type="button"
              class="btn-header-bell"
              :class="{ 'has-unread': adminNotificationCount > 0 }"
              @click="toggleBellDropdown"
              title="Thông Báo"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
              </svg>
              <span v-if="adminNotificationCount > 0" class="bell-badge-pill">
                {{ adminNotificationCount > 99 ? '99+' : adminNotificationCount }}
              </span>
            </button>

            <!-- Dropdown danh sách thông báo -->
            <div v-if="showBellDropdown" class="bell-dropdown-popover">
              <div class="bell-popover-header">
                <div class="bph-header-top">
                  <div class="bph-title">
                    <span>🔔 Thông Báo</span>
                    <span v-if="adminNotificationCount > 0" class="bph-count-badge">{{ adminNotificationCount }} mới</span>
                  </div>
                </div>
                <!-- Tabs lọc Đơn hàng / Tin nhắn -->
                <div class="bph-tabs-row">
                  <button
                    type="button"
                    class="bph-tab-btn"
                    :class="{ active: bellFilter === 'ALL' }"
                    @click="bellFilter = 'ALL'"
                  >
                    Tất cả ({{ adminNotificationList.length }})
                  </button>
                  <button
                    type="button"
                    class="bph-tab-btn"
                    :class="{ active: bellFilter === 'DON_HANG' }"
                    @click="bellFilter = 'DON_HANG'"
                  >
                    Đơn hàng <span v-if="adminOrderNewCount > 0">({{ adminOrderNewCount }})</span>
                  </button>
                  <button
                    type="button"
                    class="bph-tab-btn"
                    :class="{ active: bellFilter === 'CHAT' }"
                    @click="bellFilter = 'CHAT'"
                  >
                    Tin nhắn <span v-if="adminChatUnreadCount > 0">({{ adminChatUnreadCount }})</span>
                  </button>
                </div>
              </div>

              <div class="bell-popover-list">
                <div v-if="filteredNotificationList.length === 0" class="bell-empty-box">
                  <p>Không có thông báo mới nào</p>
                </div>
                <div
                  v-for="item in filteredNotificationList"
                  :key="item.id + '-' + item.loai"
                  class="bell-item-row"
                  :class="{ unread: !item.daDoc, 'order-row': item.loai === 'DON_HANG' }"
                  @click="handleNotificationClick(item)"
                >
                  <div class="bell-item-avatar" :class="item.loai === 'DON_HANG' ? 'order-avatar' : 'chat-avatar'">
                    <span v-if="item.loai === 'DON_HANG'">📦</span>
                    <span v-else>💬</span>
                  </div>
                  <div class="bell-item-content">
                    <div class="bic-top">
                      <strong class="bic-name">{{ item.tieuDe }}</strong>
                      <span class="bic-time">{{ item.thoiGian }}</span>
                    </div>
                    <p class="bic-msg">{{ item.noiDung }}</p>
                  </div>
                  <span v-if="!item.daDoc" class="bell-dot-unread"></span>
                </div>
              </div>

              <div class="bell-popover-footer">
                <button type="button" class="btn-bell-view-full" @click="goToOrdersOrChat">
                  {{ bellFilter === 'DON_HANG' || (bellFilter === 'ALL' && adminOrderNewCount > 0) ? 'Xem quản lý hóa đơn →' : 'Xem quản lý chat & tư vấn →' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Người dùng đang đăng nhập -->
          <div v-if="currentUser" ref="userMenuRef" class="user-profile-menu">
            <div class="admin-badge" @click="showUserDropdown = !showUserDropdown" title="Tài khoản đang đăng nhập">
              <div class="admin-avatar">
                <span>{{ getAvatarInitials(currentUser.tenNhanVien || currentUser.tenTaiKhoan) }}</span>
              </div>
              <div class="admin-info-text">
                <span class="admin-name">{{ currentUser.tenNhanVien || currentUser.tenTaiKhoan || (isUserAdmin ? 'Quản trị viên' : 'Nhân viên') }}</span>
                <span class="admin-role">{{ currentUser.role || (isUserAdmin ? 'Quản trị viên' : 'Nhân viên') }}</span>
              </div>
              <span class="dropdown-caret" :class="{ open: showUserDropdown }">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </span>
            </div>

            <!-- Menu dropdown cá nhân -->
            <div v-if="showUserDropdown" class="user-dropdown-popover">
              <div class="popover-header">
                <div class="popover-avatar">
                  <span>{{ getAvatarInitials(currentUser.tenNhanVien || currentUser.tenTaiKhoan) }}</span>
                </div>
                <div class="popover-header-info">
                  <div class="popover-name" :title="currentUser.tenNhanVien || currentUser.tenTaiKhoan">
                    {{ currentUser.tenNhanVien || currentUser.tenTaiKhoan }}
                  </div>
                  <span class="popover-role-tag" :class="{ 'role-admin': isUserAdmin }">
                    {{ currentUser.role || (isUserAdmin ? 'Quản trị viên' : 'Nhân viên') }}
                  </span>
                </div>
              </div>

              <div class="popover-details">
                <div class="popover-detail-row">
                  <span class="popover-detail-label">Mã NV</span>
                  <span class="popover-detail-val code-val">{{ currentUser.maNhanVien || '-' }}</span>
                </div>
                <div class="popover-detail-row" v-if="currentUser.email">
                  <span class="popover-detail-label">Email</span>
                  <span class="popover-detail-val text-truncate" :title="currentUser.email">{{ currentUser.email }}</span>
                </div>
                <div class="popover-detail-row" v-if="currentUser.soDienThoai">
                  <span class="popover-detail-label">SĐT</span>
                  <span class="popover-detail-val">{{ currentUser.soDienThoai }}</span>
                </div>
              </div>

              <div class="popover-footer">
                <button type="button" class="btn-popover-logout" @click="handleLogout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                    <polyline points="16 17 21 12 16 7" />
                    <line x1="21" y1="12" x2="9" y2="12" />
                  </svg>
                  Đăng xuất tài khoản
                </button>
              </div>
            </div>
          </div>

          <!-- Trạng thái chưa đăng nhập / đã đăng xuất -->
          <div v-else class="logged-out-header-box">
            <button class="btn-re-login" @click="handleLoginAdmin">Đăng nhập Quản trị viên</button>
          </div>
        </div>
      </header>

      <!-- Banner thông báo khi nhân viên ở Chế độ xem -->
      <div v-if="isEmployeeInViewMode" class="view-mode-alert-banner">
        <div class="view-mode-alert-inner">
          <div class="view-mode-left">
            <span class="view-mode-tag">CHẾ ĐỘ XEM</span>
            <span class="view-mode-msg">
              Bạn chưa vào ca làm việc nên chỉ có quyền tra cứu thông tin. Các thao tác bán hàng, tạo đơn hàng và chỉnh sửa dữ liệu đang bị khóa.
            </span>
          </div>
          <button type="button" class="btn-banner-open-ca" @click="openMoCaModal">
            Vào ca làm việc ngay
          </button>
        </div>
      </div>

      <div class="page-body" :class="{ 'view-mode-active': isEmployeeInViewMode }" @click.capture="handleViewModeGlobalClick">
        <router-view />
      </div>
    </main>

    <!-- POPUP MODAL MỞ CA LÀM VIỆC KHI ĐĂNG NHẬP / CHẾ ĐỘ XEM -->
    <div v-if="showMoCaPopup" class="mo-ca-modal-overlay" @click.self="handleCheDoXem">
      <div class="mo-ca-popup-card">
        <!-- Header Banner -->
        <div class="mo-ca-popup-header">
          <h2 class="mo-ca-popup-title">MỞ CA LÀM VIỆC</h2>
          <div class="mo-ca-popup-subtitle">Hệ thống quản lý bán hàng FF T-shirt</div>
        </div>

        <form @submit.prevent="submitMoCaFromPopup" class="mo-ca-popup-body">
          <!-- Thông tin nhân viên & thời gian thực -->
          <div class="mo-ca-staff-header">
            <h3 class="mo-ca-staff-name">{{ currentUser?.tenNhanVien || currentUser?.tenTaiKhoan }}</h3>
            <div class="mo-ca-staff-sub">
              Mã: <strong>{{ currentUser?.maNhanVien || '-' }}</strong> • {{ currentTimeStr }} {{ currentDateStr }}
            </div>
          </div>

          <!-- Dành cho Quản trị viên: Có dropdown chọn nhân viên mở ca thay -->
          <div v-if="isUserAdmin" class="mo-ca-form-group">
            <label class="mo-ca-form-label">Nhân viên trực quầy (Quản trị viên chọn):</label>
            <select v-model="moCaForm.nhanVienId" class="mo-ca-input" @change="onAdminEmployeeChange">
              <option v-for="nv in activeEmployees" :key="nv.id" :value="nv.id">
                {{ nv.maNhanVien }} - {{ nv.tenNhanVien }} ({{ nv.tenTaiKhoan }})
              </option>
            </select>
          </div>

          <!-- Khung hiển thị Ca làm việc -->
          <div class="mo-ca-box-wrap">
            <div class="mo-ca-field-title">Ca làm việc thời điểm hiện tại</div><div v-if="false">
              <span class="lbl-left">Ca làm việc thời điểm hiện tại</span>
              <span class="lbl-right" v-if="currentMatchedShift">{{ selectedShiftStatus?.canOpen ? '🟢 Sẵn sàng vào ca' : '🟡 ' + (selectedShiftStatus?.label || '') }}</span>
            </div>

            <!-- Nếu KHÔNG tìm thấy lịch phân công hôm nay (cho nhân viên thường) -->
            <div v-if="!isUserAdmin && !hasScheduleToday" class="mo-ca-empty-schedule">
              ⚠️ Không tìm thấy ca làm việc được phân công hôm nay của bạn.
            </div>

            <!-- Thẻ hiển thị Ca làm việc tự động (KHÔNG DÙNG COMBOBOX) -->
            <div v-else-if="currentMatchedShift" class="mo-ca-auto-shift-card" :class="{ 'is-in-shift': selectedShiftStatus?.canOpen, 'is-out-shift': !selectedShiftStatus?.canOpen }">
              <div class="auto-shift-main">
                <div class="auto-shift-icon-wrap">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 14 14"></polyline>
                  </svg>
                </div>
                <div class="auto-shift-text">
                  <div class="auto-shift-title">{{ currentMatchedShift.tenCa }}</div>
                  <div class="auto-shift-hours">
                    Khung giờ: <span class="hours-val">{{ formatTime(currentMatchedShift.gioBatDau) }} – {{ formatTime(currentMatchedShift.gioKetThuc) }}</span>
                  </div>
                </div>
              </div>
              <div class="auto-shift-badge" :class="selectedShiftStatus?.canOpen ? 'badge-live' : 'badge-wait'">
                {{ selectedShiftStatus?.canOpen ? '🟢 Trong ca' : '🟡 Ngoài giờ' }}
              </div>
            </div>
            <div v-else class="mo-ca-empty-schedule">
              ⚠️ Không tìm thấy ca làm việc phù hợp lúc này
            </div>
            <!-- Bỏ combobox cũ -->

            <div v-if="false" class="mo-ca-shift-select-box">
              <select v-model="moCaForm.caLamViecId" class="mo-ca-input-select" required>
                <option
                  v-for="ca in assignedShiftsToday"
                  :key="ca.id"
                  :value="ca.id"
                >
                  {{ ca.tenCa }} ({{ formatTime(ca.gioBatDau) }} - {{ formatTime(ca.gioKetThuc) }}) - [{{ getShiftStatus(ca).label }}]
                </option>
              </select>
            </div>

            <!-- Nếu là Quản trị viên -->
            <div v-if="false" class="mo-ca-shift-select-box">
              <select v-model="moCaForm.caLamViecId" class="mo-ca-input-select" required>
                <option value="">-- Chọn ca làm việc --</option>
                <option
                  v-for="ca in activeShifts"
                  :key="ca.id"
                  :value="ca.id"
                >
                  {{ ca.tenCa }} ({{ formatTime(ca.gioBatDau) }} - {{ formatTime(ca.gioKetThuc) }}) - [{{ getShiftStatus(ca).label }}]
                </option>
              </select>
            </div>
          </div>

          <!-- Thông báo trạng thái nếu chưa đến giờ vào ca -->
          <div v-if="false" class="mo-ca-alert-warning">
            ⚠️ {{ selectedShiftStatus.label }}. Bạn chỉ có thể xác nhận vào ca trước tối đa 30 phút!
          </div>

          <!-- Ô nhập Tiền mặt đầu ca -->
          <div class="mo-ca-form-group">
            <label class="mo-ca-form-label">Tiền mặt đầu ca</label><div>
            <div class="mo-ca-input-wrapper">
              <input
                type="text"
                inputmode="numeric"
                v-model="displayTienMatDauCa"
                placeholder="0"
                class="mo-ca-input-modern"
                required
              />
              <span class="mo-ca-currency-suffix">₫</span>
            </div>
          </div>
          <div v-if="false">
            <input
              type="text"
              inputmode="numeric"
              v-model="displayTienMatDauCa"
              placeholder="0"
              class="mo-ca-input"
              required
            />
          </div>

          </div>
          <!-- Ô nhập Tiền tài khoản đầu ca -->
          <div class="mo-ca-form-group">
            <label class="mo-ca-form-label">Tiền tài khoản đầu ca</label>
            <div class="mo-ca-input-wrapper">
              <input
                type="text"
                inputmode="numeric"
                v-model="displayTienTaiKhoanDauCa"
                placeholder="0"
                class="mo-ca-input-modern"
              />
              <span class="mo-ca-currency-suffix">₫</span>
            </div>
          </div>
          <div v-if="false">
          <div class="mo-ca-form-group">
            <label class="mo-ca-form-label">Tiền tài khoản đầu ca</label>
            <input
              type="text"
              inputmode="numeric"
              v-model="displayTienTaiKhoanDauCa"
              placeholder="0"
              class="mo-ca-input"
            />
          </div>

          </div>
          <!-- Số dự kiến từ ca trước -->
          <div class="mo-ca-prev-cash-pill-box">
            <span>Số dự kiến từ ca trước: <strong>{{ formatCurrency(lastClosedShift?.tienMatCuoiCa || 0) }}</strong></span>
            <button
              v-if="lastClosedShift && (lastClosedShift.tienMatCuoiCa || 0) > 0"
              type="button"
              class="btn-apply-prev-cash"
              @click="applyLastClosedCash"
            >
              Dùng số tiền này
            </button>
          </div>

          <!-- 2 nút: Chế độ xem & Xác nhận vào ca -->
          <div class="mo-ca-actions-row">
            <button
              type="button"
              class="btn-che-do-xem"
              @click="handleCheDoXem"
            >
              Chế độ xem
            </button>
            <button
              type="submit"
              class="btn-xac-nhan-vao-ca"
              :disabled="!canSubmitMoCa || submittingMoCa"
            >
              {{ submittingMoCa ? 'Đang vào ca...' : 'Xác nhận vào ca' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- POPUP MODAL KẾT THÚC CA & BÀN GIAO TIỀN MẶT -->
    <div v-if="showDongCaModal" class="dong-ca-modal-overlay" @click.self="showDongCaModal = false">
      <div class="dong-ca-popup-card">
        <!-- Header Banner -->
        <div class="dong-ca-popup-header">
          <div class="dong-ca-header-text">
            <h2 class="dong-ca-popup-title">KẾT THÚC CA & BÀN GIAO TIỀN MẶT</h2>
            <div class="dong-ca-popup-subtitle">Hệ thống quản lý bán hàng FF T-shirt</div>
          </div>
          <button type="button" class="btn-close-dong-ca" @click="showDongCaModal = false">✕</button>
        </div>

        <form @submit.prevent="submitDongCaFromModal" class="dong-ca-popup-body">
          <!-- Thông tin ca trực hiện tại -->
          <div class="current-shift-badge-box">
            <div class="shift-badge-top">
              <span class="pulse-dot"></span>
              <span class="badge-title">CA ĐANG TRỰC: <strong>{{ currentOpenShift?.caLamViec?.tenCa || 'Chưa xác định' }}</strong> ({{ formatTime(currentOpenShift?.caLamViec?.gioBatDau) }} - {{ formatTime(currentOpenShift?.caLamViec?.gioKetThuc) }})</span>
            </div>
            <div class="shift-badge-details">
              <span>Nhân viên: <strong>{{ currentOpenShift?.nhanVien?.tenNhanVien || currentUser?.tenNhanVien }}</strong> ({{ currentOpenShift?.nhanVien?.maNhanVien || currentUser?.maNhanVien }})</span>
              <span>• Mã ca: <strong>{{ currentOpenShift?.maGiaoCa || ('GC-' + currentOpenShift?.id) }}</strong></span>
              <span>• Mở ca lúc: <strong>{{ formatDateTime(currentOpenShift?.thoiGianMoCa) }}</strong></span>
            </div>
          </div>

          <!-- Két tiền thời gian thực -->
          <div class="dong-ca-metrics-section">
            <div class="metrics-sec-header">
              <span class="sec-title">Két tiền ca trực (Thời gian thực)</span>
              <button
                type="button"
                class="btn-refresh-metrics"
                @click="loadShiftSummary(currentOpenShift?.id)"
                :disabled="loadingShiftSummary"
                title="Làm mới số liệu két tiền"
              >
                {{ loadingShiftSummary ? 'Đang tải...' : '↻ Làm mới số liệu' }}
              </button>
            </div>

            <div class="dong-ca-metrics-grid">
              <div class="metric-card bg-warm">
                <span class="m-label">Tiền mặt đầu ca</span>
                <strong class="m-val text-brown">{{ formatCurrency(currentOpenShift?.tienMatDauCa || 0) }}</strong>
              </div>
              <div class="metric-card bg-green">
                <span class="m-label">Tiền mặt bán tại quầy</span>
                <strong class="m-val text-green">{{ formatCurrency(shiftSummary?.tienMatHeThong || 0) }}</strong>
              </div>
              <div class="metric-card bg-blue">
                <span class="m-label">Chuyển khoản / QR quầy</span>
                <strong class="m-val text-blue">{{ formatCurrency(shiftSummary?.tienChuyenKhoanHeThong || 0) }}</strong>
              </div>
              <div class="metric-card bg-highlight">
                <span class="m-label">Tiền mặt dự kiến có trong két</span>
                <strong class="m-val text-primary-highlight">{{ formatCurrency(calculatedExpectedCash) }}</strong>
              </div>
            </div>
          </div>

          <!-- Biểu mẫu kết ca -->
          <div class="dong-ca-form-grid">
            <div class="form-col">
              <label class="dong-ca-label">Tiền chi trong ca</label>
              <div class="input-currency-wrapper">
                <input
                  type="text"
                  inputmode="numeric"
                  v-model="displayTienChiTrongCa"
                  placeholder="0"
                  class="dong-ca-input"
                />
                <span class="currency-tag">VNĐ</span>
              </div>
            </div>

            <div class="form-col">
              <label class="dong-ca-label">Tiền mặt thực tế kiểm đếm <span class="required-star">*</span></label>
              <div class="input-currency-wrapper">
                <input
                  type="text"
                  inputmode="numeric"
                  v-model="displayTienMatCuoiCa"
                  placeholder="0"
                  class="dong-ca-input"
                  :class="{ 'input-error-border': dongCaForm.tienMatCuoiCa === null }"
                  required
                />
                <span class="currency-tag">VNĐ</span>
              </div>
            </div>
          </div>

          <!-- Banner kiểm đếm chênh lệch -->
          <div
            v-if="dongCaForm.tienMatCuoiCa !== null"
            class="diff-result-box"
            :class="{
              'diff-match': calculatedDiff === 0,
              'diff-surplus': calculatedDiff > 0,
              'diff-deficit': calculatedDiff < 0
            }"
          >
            <div class="diff-left">
              <span class="diff-icon-symbol">{{ calculatedDiff === 0 ? '✓' : '⚠️' }}</span>
              <div>
                <strong class="diff-main-msg">
                  {{
                    calculatedDiff === 0
                      ? 'Tiền mặt kiểm đếm KHỚP 100% với hệ thống'
                      : (calculatedDiff > 0 ? 'Phát hiện THỪA TIỀN so với dự kiến' : 'Phát hiện THIẾU TIỀN so với dự kiến')
                  }}
                </strong>
                <div class="diff-sub-msg">
                  Thực tế: {{ formatCurrency(dongCaForm.tienMatCuoiCa) }} | Dự kiến: {{ formatCurrency(calculatedExpectedCash) }}
                </div>
              </div>
            </div>
            <div class="diff-right">
              <span class="diff-tag">
                {{ calculatedDiff > 0 ? '+' : '' }}{{ formatCurrency(calculatedDiff) }}
              </span>
            </div>
          </div>

          <!-- Lý do chênh lệch nếu có -->
          <div v-if="calculatedDiff !== 0" class="dong-ca-reason-group">
            <label class="dong-ca-label text-warn">
              Lý do chênh lệch tiền mặt <span class="required-star">*</span>
            </label>
            <textarea
              v-model="dongCaForm.lyDoChenhLech"
              rows="2"
              placeholder="Nhập lý do giải trình chênh lệch tiền mặt..."
              class="dong-ca-textarea"
              :class="{ 'input-error-border': !dongCaForm.lyDoChenhLech || !dongCaForm.lyDoChenhLech.trim() }"
            ></textarea>
          </div>

          <div class="dong-ca-form-grid">
            <div class="form-col">
              <label class="dong-ca-label handover-col-hidden" v-if="false">Người nhận bàn giao</label>
              <div class="handover-col-hidden" v-if="false">
                <div class="auto-handover-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                </div>
                <div class="auto-handover-info">
                  <div class="auto-handover-name">
                    <strong v-if="shiftSummary?.tenNhanVienNhanDuKien">
                      {{ shiftSummary.tenNhanVienNhanDuKien }} ({{ shiftSummary.maNhanVienNhanDuKien || 'NV' }})
                    </strong>
                    <strong v-else class="text-muted">
                      Quản trị viên / Két cửa hàng (Ca cuối ngày)
                    </strong>
                  </div>
                  <div class="auto-handover-sub">
                    {{ shiftSummary?.thongTinBaoGiao || 'Tự động bàn giao cho ca tiếp theo' }}
                  </div>
                </div>
              </div>
              <div v-if="false" class="select-wrapper-minimal">
                <select v-model="dongCaForm.nhanVienNhanId" class="dong-ca-input">
                  <option value="">-- Quản trị viên / Cuối ngày --</option>
                  <option
                    v-for="nv in handoverEmployees"
                    :key="nv.id"
                    :value="nv.id"
                  >
                    {{ nv.maNhanVien }} - {{ nv.tenNhanVien }} ({{ nv.tenTaiKhoan }})
                  </option>
                </select>
              </div>
            </div>

            <div class="form-col">
              <label class="dong-ca-label dong-ca-label-note">Ghi chú bàn giao</label>
              <input
                type="text"
                v-model="dongCaForm.ghiChu"
                placeholder="Ghi chú bàn giao (nếu có)..."
                class="dong-ca-input"
              />
            </div>
          </div>

          <!-- Nút hành động kết ca -->
          <div class="dong-ca-actions-row">
            <button
              type="button"
              class="btn-cancel-dong-ca"
              @click="showDongCaModal = false"
            >
              Hủy
            </button>
            <button
              type="submit"
              class="btn-submit-dong-ca"
              :disabled="!canSubmitDongCa || submittingDongCa"
            >
              {{ submittingDongCa ? 'Đang kết thúc ca...' : 'Xác nhận Kết ca & Bàn giao tiền' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- MODAL IN BIÊN BẢN BÀN GIAO CA (RECEIPT MODAL) -->
    <div v-if="showReceiptModal" class="modal-backdrop-clean" @click.self="showReceiptModal = false">
      <div class="modal-dialog-clean modal-receipt">
        <div class="modal-header-clean no-print">
          <h3 class="modal-title-clean">Biên bản bàn giao ca làm việc</h3>
          <button type="button" class="btn-close-modal" @click="showReceiptModal = false">✕</button>
        </div>

        <div class="modal-body-clean printable-receipt" id="print-receipt-area">
          <div class="receipt-header">
            <h2 class="receipt-brand">FF T-SHIRT</h2>
            <p class="receipt-subtitle">BIÊN BẢN BÀN GIAO CA LÀM VIỆC</p>
            <div class="receipt-code">Mã giao ca: <strong>{{ receiptShift?.maGiaoCa || ('GC-' + receiptShift?.id) }}</strong></div>
          </div>

          <div class="receipt-divider"></div>

          <div class="receipt-meta-grid">
            <div class="receipt-meta-row">
              <span class="meta-label">Ca làm việc:</span>
              <strong class="meta-val">{{ receiptShift?.caLamViec?.tenCa }}</strong>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Người giao ca:</span>
              <strong class="meta-val">{{ receiptShift?.nhanVien?.tenNhanVien }} ({{ receiptShift?.nhanVien?.maNhanVien }})</strong>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Người nhận ca:</span>
              <strong class="meta-val">{{ receiptShift?.nhanVienNhan?.tenNhanVien ? (receiptShift?.nhanVienNhan?.tenNhanVien + ' (' + receiptShift?.nhanVienNhan?.maNhanVien + ')') : 'Quản lý / Cuối ngày' }}</strong>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Thời gian mở ca:</span>
              <span class="meta-val">{{ formatDateTime(receiptShift?.thoiGianMoCa) }}</span>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Thời gian kết ca:</span>
              <span class="meta-val">{{ receiptShift?.thoiGianDongCa ? formatDateTime(receiptShift?.thoiGianDongCa) : 'Đang trực' }}</span>
            </div>
            <div class="receipt-meta-row">
              <span class="meta-label">Ghi chú giao ca:</span>
              <span class="meta-val">{{ receiptShift?.ghiChu || '-' }}</span>
            </div>
          </div>

          <div class="receipt-divider"></div>

          <div class="receipt-financial-table">
            <div class="fin-row">
              <span>1. Tiền mặt đầu ca:</span>
              <strong>{{ formatCurrency(receiptShift?.tienMatDauCa || 0) }}</strong>
            </div>
            <div class="fin-row">
              <span>2. Tiền mặt thu tại quầy:</span>
              <strong>{{ formatCurrency(receiptShift?.tongTienTrongCa || receiptShift?.tienMatThuTrongCa || 0) }}</strong>
            </div>
            <div class="fin-row">
              <span>3. Doanh thu chuyển khoản:</span>
              <strong>{{ formatCurrency(receiptShift?.tienChuyenKhoan || 0) }}</strong>
            </div>
            <div v-if="Number(receiptShift?.tienChiTrongCa || 0) > 0" class="fin-row text-red">
              <span>4. Tiền chi trong ca:</span>
              <strong>-{{ formatCurrency(receiptShift?.tienChiTrongCa || 0) }}</strong>
            </div>
            <div class="fin-row fin-expected">
              <span>5. Tiền mặt dự kiến có trong két:</span>
              <strong>{{ formatCurrency(calculateReceiptExpected(receiptShift)) }}</strong>
            </div>
            <div class="fin-row fin-actual">
              <span>6. Tiền mặt thực tế kiểm đếm:</span>
              <strong>{{ formatCurrency(receiptShift?.tienMatCuoiCa || 0) }}</strong>
            </div>
            <div class="fin-row fin-diff" :class="Number(receiptShift?.chenhLech || 0) === 0 ? 'text-green' : 'text-warn'">
              <span>7. Chênh lệch tiền mặt:</span>
              <strong>{{ Number(receiptShift?.chenhLech || 0) > 0 ? '+' : '' }}{{ formatCurrency(receiptShift?.chenhLech || 0) }}</strong>
            </div>
            <div v-if="receiptShift?.lyDoChenhLech" class="fin-reason">
              <span>* Lý do giải trình chênh lệch:</span>
              <em>{{ receiptShift?.lyDoChenhLech }}</em>
            </div>
          </div>

          <div class="receipt-signatures">
            <div class="sig-col">
              <p class="sig-title">NGƯỜI GIAO CA</p>
              <p class="sig-sub">(Ký và ghi rõ họ tên)</p>
              <div class="sig-space"></div>
              <p class="sig-name">{{ receiptShift?.nhanVien?.tenNhanVien }}</p>
            </div>
            <div class="sig-col">
              <p class="sig-title">NGƯỜI NHẬN CA</p>
              <p class="sig-sub">(Ký và ghi rõ họ tên)</p>
              <div class="sig-space"></div>
              <p class="sig-name">{{ receiptShift?.nhanVienNhan?.tenNhanVien || 'Quản lý' }}</p>
            </div>
          </div>
        </div>

        <div class="modal-footer-clean no-print">
          <button type="button" class="btn-cancel-modal" @click="showReceiptModal = false">Đóng</button>
          <button type="button" class="btn-print-action" @click="printReceipt">
            🖨 In biên bản bàn giao
          </button>
        </div>
      </div>
    </div>

    <!-- Overlay khi bị đăng xuất hoặc tài khoản bị khóa -->
    <div v-if="!currentUser" class="logged-out-modal-overlay">
      <div class="logged-out-modal-card">
        <div class="logged-out-icon-wrap">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
        <h3 class="logged-out-title">Bạn đã đăng xuất khỏi hệ thống</h3>
        <p class="logged-out-desc">
          Tài khoản của bạn đã được chuyển sang trạng thái ngừng hoạt động hoặc phiên làm việc đã kết thúc.
          Vui lòng đăng nhập lại để tiếp tục sử dụng hệ thống!
        </p>
        <div class="logged-out-actions">
          <button class="btn-relogin-action" @click="handleLoginAdmin">
            Đăng nhập lại ngay (Quản trị viên)
          </button>
          <button class="btn-goto-login-action" @click="goToLoginPage">
            Đến màn hình đăng nhập
          </button>
        </div>
      </div>
    </div>

    <GlobalDialog />
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&family=Fredoka:wght@600;700&display=swap');

:root {
  --blue: #496883;
  --gold: #d2a764;
  --line: #e9e5db;
}

/* Layout tổng thể */
.app-shell {
  display: flex;
  min-height: 100vh;
  background-color: #f7f5ef;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Sidebar nền trắng cố định 200px */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 14rem !important;
  background-color: #ffffff;
  border-right: 1px solid #e9e5db;
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* Khu vực khung chứa Logo */
.logo-box {
  background-color: #ffffff;
  height: 96px;
  padding: 14px 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-bottom: 2px solid #ecd8af;
  user-select: none;
  flex-shrink: 0;
}

.logo-wrapper {
  position: relative;
  display: inline-block;
  font-family: 'Fredoka', sans-serif;
}

/* 3 tia sáng phát ra góc trên bên trái */
.sparkles {
  position: absolute;
  top: -8px;
  left: -10px;
  width: 28px;
  height: 28px;
  pointer-events: none;
}

.sparkles .ray {
  position: absolute;
  background-color: #d1a868;
  border-radius: 3px;
}

.sparkles .ray-1 {
  width: 4px;
  height: 11px;
  top: 0;
  right: 5px;
  transform: rotate(10deg);
}

.sparkles .ray-2 {
  width: 4px;
  height: 11px;
  top: 5px;
  left: 4px;
  transform: rotate(-45deg);
}

.sparkles .ray-3 {
  width: 4px;
  height: 11px;
  bottom: 0px;
  left: -3px;
  transform: rotate(-80deg);
}

/* Cụm chữ FF và chữ T-shirt */
.logo-content {
  display: flex;
  align-items: flex-end;
  filter: drop-shadow(0 3px 6px rgba(73, 104, 131, 0.16));
}

.logo-name {
  display: flex;
  font-size: 52px;
  font-weight: 800;
  line-height: 0.85;
  letter-spacing: -2px;
}

.f-blue {
  color: #496883;
}

.f-gold {
  color: #d2a764;
  margin-left: 2px;
}

.logo-sub {
  color: #496883;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  margin-left: 5px;
  margin-bottom: 3px;
  letter-spacing: -0.3px;
}

/* Menu điều hướng */
.menu {
  flex: 1;
  padding: 14px 8px;
  background: #ffffff;
}

.menu-item {
  height: 34px;
  padding: 0 11px;
  display: flex;
  align-items: center;
  gap: 0.65rem !important;
  border-radius: 7px;
  color: #647074;
  font-size: 0.9rem !important;
  margin-bottom: 3px;
  cursor: pointer;
  text-decoration: none;
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease;
  user-select: none;
  min-height: 2.4rem !important;
}

.menu-item:hover {
  background: #f7f5ef;
  color: #496883;
}

.menu-item.active {
  background: #eaf1f4;
  color: #496883;
  font-weight: 700;
}

.menu-icon {
  width: 15px;
  text-align: center;
  color: #496883;
  font-size: 11px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}

.menu-text {
  flex: 1;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 9px;
  color: #8a9292;
  margin-left: auto;
  transition: transform 0.2s ease;
}

.arrow-icon.open {
  transform: rotate(180deg);
}

/* Menu con cấp 2 */
.submenu-tree {
  display: flex;
  flex-direction: column;
  padding-left: 14px;
  margin: 2px 0 4px;
}

.tree-item {
  height: 28px;
  padding: 0 8px;
  display: flex;
  align-items: center;
  border-radius: 6px;
  color: #647074;
  font-size: 0.85rem !important;
  text-decoration: none;
  transition: all 0.15s ease;
  min-height: 2rem !important;
}

.tree-branch {
  font-family: monospace, sans-serif;
  color: #9aa0a0;
  margin-right: 6px;
  font-size: 9px;
  user-select: none;
  flex-shrink: 0;
}

.tree-item:hover {
  background-color: #f7f5ef;
  color: #496883;
}

.tree-item.active {
  background-color: #eaf1f4;
  color: #496883;
  font-weight: 700;
}

.tree-item.active .tree-branch {
  color: #496883;
}

/* Nội dung chính */
.main-content {
  margin-left: 14rem;
  width: calc(100% - 14rem);
  min-height: 100vh;
  background-color: #f7f5ef;
  display: flex;
  flex-direction: column;
}

.top-header {
  height: 3.5rem !important;
  background-color: transparent;
  padding: 0 2rem;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: none;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-profile-menu {
  position: relative;
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 14px 4px 6px;
  border-radius: 40px;
  background: #ffffff;
  border: 1px solid #dfd8cb;
  box-shadow: 0 2px 8px rgba(90, 70, 50, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.admin-badge:hover {
  border-color: #8c7355;
  box-shadow: 0 4px 14px rgba(140, 115, 85, 0.16);
  transform: translateY(-1px);
}

.admin-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8c7355, #634b35);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.82rem;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 6px rgba(99, 75, 53, 0.25);
  flex-shrink: 0;
}

.admin-info-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.admin-name {
  font-size: 0.88rem !important;
  font-weight: 700;
  color: #2c3e50;
  max-width: 140px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-role {
  font-size: 0.72rem;
  color: #8c7355;
  font-weight: 600;
}

.dropdown-caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #8c7355;
  transition: transform 0.2s ease;
  margin-left: 2px;
}

.dropdown-caret.open {
  transform: rotate(180deg);
}

.user-dropdown-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 260px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #ebd9c8;
  box-shadow: 0 12px 32px rgba(90, 70, 50, 0.14);
  padding: 16px;
  z-index: 1000;
  animation: dropdownIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dropdownIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.popover-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f2ece3;
}

.popover-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8c7355, #634b35);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
  box-shadow: 0 3px 8px rgba(99, 75, 53, 0.25);
  flex-shrink: 0;
}

.popover-header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
  flex: 1;
}

.popover-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.popover-role-tag {
  display: inline-block;
  align-self: flex-start;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 9999px;
  background: #f7f3ee;
  color: #8c7355;
  border: 1px solid #ebd9c8;
}

.popover-role-tag.role-admin {
  background: #fef7ee;
  color: #b45309;
  border-color: #fde68a;
}

.popover-details {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.popover-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.82rem;
  gap: 8px;
}

.popover-detail-label {
  color: #8a7b6e;
  font-weight: 500;
  flex-shrink: 0;
}

.popover-detail-val {
  color: #2c3e50;
  font-weight: 600;
}

.popover-detail-val.code-val {
  background: #f5efe6;
  color: #634b35;
  padding: 1px 7px;
  border-radius: 5px;
  font-family: monospace;
  font-size: 0.8rem;
  border: 1px solid #e5dacd;
}

.popover-detail-val.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
}

.popover-footer {
  padding-top: 10px;
  border-top: 1px solid #f2ece3;
}

.btn-popover-logout {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #fff5f5;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-weight: 600;
  font-size: 0.86rem;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn-popover-logout:hover {
  background: #dc2626;
  border-color: #dc2626;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

.logged-out-header-box {
  display: flex;
  align-items: center;
}

.btn-re-login {
  background: #8c7355;
  color: #ffffff;
  border: none;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  box-shadow: 0 2px 8px rgba(140, 115, 85, 0.25);
}

.btn-re-login:hover {
  background: #755f45;
}

/* ================= ĐỒNG BỘ FORM CONTROLS & COMBOBOX TOÀN DỰ ÁN ================= */
button, input, select, textarea {
  font-family: var(--system-font, 'Inter', 'Roboto', 'Be Vietnam Pro', system-ui, sans-serif) !important;
}

.select-wrapper-minimal {
  position: relative;
  width: 100%;
  display: inline-block;
}

.select-wrapper-minimal::after {
  content: "";
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 4.5px solid transparent;
  border-right: 4.5px solid transparent;
  border-top: 5px solid #8c7355;
  pointer-events: none;
}

.select-wrapper-minimal select.form-control-minimal,
.select-wrapper-minimal select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  padding-right: 32px !important;
  cursor: pointer;
}

/* Modal overlay khi bị đăng xuất */
.logged-out-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
}

.logged-out-modal-card {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 16px;
  padding: 2.2rem 2rem;
  text-align: center;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.25);
  animation: cardZoomIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cardZoomIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.logged-out-icon-wrap {
  width: 72px;
  height: 72px;
  margin: 0 auto 1.25rem;
  border-radius: 50%;
  background: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logged-out-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.65rem;
}

.logged-out-desc {
  font-size: 0.9rem;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 1.6rem;
}

.btn-relogin-action {
  background: #dc2626;
  color: #ffffff;
  border: none;
  font-size: 0.95rem;
  font-weight: 600;
  padding: 0.75rem 1.6rem;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
  transition: all 0.2s;
}

.logged-out-actions {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.btn-goto-login-action {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #cbd5e1;
  font-size: 0.92rem;
  font-weight: 600;
  padding: 0.7rem 1.4rem;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-goto-login-action:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-relogin-action:hover {
  background: #b91c1c;
  box-shadow: 0 6px 16px rgba(185, 28, 28, 0.35);
}

.page-body {
  flex: 1;
  width: 100%;
}

@media print {
  .sidebar,
  .top-header,
  header {
    display: none !important;
    visibility: hidden !important;
    width: 0 !important;
    height: 0 !important;
    overflow: hidden !important;
  }

  .app-shell {
    display: block !important;
    min-height: auto !important;
    background: transparent !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  .main-content {
    margin: 0 !important;
    margin-left: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    background: transparent !important;
  }

  .page-body {
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
  }
}
/* BANNER THÔNG BÁO CHẾ ĐỘ XEM */
.view-mode-alert-banner {
  background: linear-gradient(90deg, #fffbeb, #fef3c7);
  border-bottom: 1px solid #fde68a;
  padding: 10px 2rem;
  box-shadow: 0 1px 3px rgba(180, 83, 9, 0.08);
}

.view-mode-alert-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.view-mode-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.view-mode-tag {
  background: #f59e0b;
  color: #ffffff;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.view-mode-msg {
  font-size: 0.84rem;
  color: #92400e;
  font-weight: 500;
}

.btn-banner-open-ca {
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 6px;
  padding: 6px 16px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-banner-open-ca:hover {
  background: #3b576e;
  transform: translateY(-1px);
}

/* Vô hiệu hóa hiệu ứng tương tác cho các nút tạo/thanh toán trong chế độ xem */
.page-body.view-mode-active button[type="submit"]:not(.btn-modal-action),
.page-body.view-mode-active .btn-checkout,
.page-body.view-mode-active .btn-pay {
  opacity: 0.6 !important;
  cursor: not-allowed !important;
  filter: grayscale(30%);
}

/* POPUP MODAL MỞ CA LÀM VIỆC THEO HÌNH ẢNH MẪU */
.header-shift-action {
  margin-right: 14px;
  display: flex;
  align-items: center;
}

.btn-header-view-mode {
  background: var(--blue, #496883);
  color: #ffffff;
  border: none;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
  transition: all 0.2s ease;
}

.btn-header-view-mode:hover {
  background: #3b576e;
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(73, 104, 131, 0.35);
}

.btn-header-active-shift {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 7px;
  transition: all 0.2s ease;
}

.btn-header-active-shift:hover {
  background: #dcfce7;
  border-color: #86efac;
}

.active-shift-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.25);
  animation: pulse-dot-anim 1.8s infinite;
}

@keyframes pulse-dot-anim {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
  70% { transform: scale(1.05); box-shadow: 0 0 0 5px rgba(34, 197, 94, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

/* Modal Overlay */
.mo-ca-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeInModal 0.25s ease;
}

@keyframes fadeInModal {
  from { opacity: 0; }
  to { opacity: 1; }
}

.mo-ca-popup-card {
  background: #ffffff;
  width: 440px;
  max-width: 95vw;
  border-radius: 16px;
  box-shadow: 0 20px 45px -10px rgba(0, 0, 0, 0.35);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: scaleUpModal 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes scaleUpModal {
  from { transform: scale(0.94); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

/* Header Banner */
.mo-ca-popup-header {
  background: var(--blue, #496883);
  color: #ffffff;
  padding: 16px 20px;
  text-align: center;
}

.mo-ca-popup-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #ffffff;
}

.mo-ca-popup-subtitle {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 3px;
}

/* Body */
.mo-ca-popup-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.mo-ca-staff-header {
  text-align: center;
  margin-bottom: 2px;
}

.mo-ca-staff-name {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
}

.mo-ca-staff-sub {
  margin-top: 3px;
  font-size: 0.78rem;
  color: #64748b;
}

.mo-ca-staff-sub strong {
  color: #334155;
}

/* Hộp Ca */
.mo-ca-field-title {
  font-size: 0.84rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 3px;
}

.mo-ca-box-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: transparent;
  border: none;
  padding: 0;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mo-ca-box-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.76rem;
}

.mo-ca-box-labels .lbl-left {
  color: #64748b;
}

.mo-ca-box-labels .lbl-right {
  font-weight: 600;
  color: #1e293b;
}

.mo-ca-empty-schedule {
  font-size: 0.85rem;
  color: #475569;
  padding: 2px 0;
}

/* Thẻ hiển thị Ca làm việc tự động */
.mo-ca-auto-shift-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  gap: 12px;
  margin-top: 2px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.mo-ca-auto-shift-card.is-in-shift {
  background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%);
  border-color: #86efac;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08);
}

.mo-ca-auto-shift-card.is-out-shift {
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
  border-color: #fcd34d;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.08);
}
/*
}
  background: #f0fdf4;
  border-color: #86efac;
}

*/
.auto-shift-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-shift-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: #e2e8f0;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mo-ca-auto-shift-card.is-in-shift .auto-shift-icon-wrap {
  background: #dcfce7;
  color: #16a34a;
}

.mo-ca-auto-shift-card.is-out-shift .auto-shift-icon-wrap {
  background: #fef3c7;
  color: #d97706;
}
/*
}
  background: #dcfce7;
  color: #16a34a;
}

*/
.auto-shift-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #0f172a;
}

.auto-shift-hours {
  font-size: 0.82rem;
  color: #64748b;
  margin-top: 2px;
  white-space: nowrap;
}

.auto-shift-hours .hours-val {
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
}

.auto-shift-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 0.76rem;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.auto-shift-badge.badge-live {
  background: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.auto-shift-badge.badge-wait {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fef3c7;
}

/* Khối nhận bàn giao tự động */
.auto-handover-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  min-height: 40px;
}

.auto-handover-icon {
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.auto-handover-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.auto-handover-name {
  font-size: 0.86rem;
  font-weight: 600;
  color: #1e293b;
}

.auto-handover-sub {
  font-size: 0.74rem;
  color: #64748b;
}

.mo-ca-shift-select-box {
  width: 100%;
}

.mo-ca-input-select {
  width: 100%;
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.84rem;
  color: #1e293b;
  background: #ffffff;
  outline: none;
}

.mo-ca-alert-warning {
  padding: 10px 14px;
  background: #fffbeb;
  border: 1.5px solid #fde68a;
  border-radius: 10px;
  color: #92400e;
  font-size: 0.8rem;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 8px;
  color: #b45309;
  font-size: 0.76rem;
  line-height: 1.35;
}

/* Form fields */
.mo-ca-form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mo-ca-form-label {
  font-size: 0.82rem;
  font-weight: 500;
  color: #334155;
}

.mo-ca-input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  color: #1e293b;
  outline: none;
  transition: border-color 0.15s ease;
  box-sizing: border-box;
}

.mo-ca-input:focus {
  border-color: var(--blue, #496883);
}

.mo-ca-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.mo-ca-input-modern {
  width: 100%;
  height: 42px;
  padding: 0 38px 0 14px;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1e293b;
  outline: none;
  background: #ffffff;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.mo-ca-input-modern:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.mo-ca-currency-suffix {
  position: absolute;
  right: 14px;
  font-size: 0.9rem;
  font-weight: 700;
  color: #94a3b8;
  pointer-events: none;
}

.mo-ca-prev-cash-pill-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.82rem;
  color: #475569;
}

.mo-ca-prev-cash-pill-box strong {
  color: #0f172a;
  font-weight: 700;
  margin-left: 4px;
}

.mo-ca-prev-cash-pill-box .btn-apply-prev-cash {
  background: #e2e8f0;
  border: none;
  border-radius: 6px;
  padding: 5px 12px;
  color: var(--blue, #496883);
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
  text-decoration: none;
}

.mo-ca-prev-cash-pill-box .btn-apply-prev-cash:hover {
  background: var(--blue, #496883);
  color: #ffffff;
}

/* Tự động kéo dài Ghi chú bàn giao full chiều rộng cân đối */
.dong-ca-form-grid:has(.dong-ca-label-note) {
  display: flex !important;
  flex-direction: column !important;
  width: 100% !important;
}

.dong-ca-form-grid:has(.dong-ca-label-note) .form-col:first-child {
  display: none !important;
}

.dong-ca-form-grid:has(.dong-ca-label-note) .form-col:last-child {
  width: 100% !important;
  display: flex !important;
  flex-direction: column !important;
}

.dong-ca-form-grid:has(.dong-ca-label-note) .dong-ca-input {
  width: 100% !important;
  height: 42px;
  border-radius: 10px;
  padding: 0 14px;
}

.mo-ca-prev-cash-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.78rem;
  color: #64748b;
  padding-top: 2px;
}

.btn-apply-prev-cash {
  background: none;
  border: none;
  color: var(--blue, #496883);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}

.btn-apply-prev-cash:hover {
  color: #354e63;
}

/* Actions Row */
.mo-ca-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.btn-che-do-xem {
  padding: 10px 20px;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-che-do-xem:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
  color: #1e293b;
}

.btn-xac-nhan-vao-ca {
  padding: 8px 22px;
  background: var(--blue, #496883);
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}

.btn-xac-nhan-vao-ca:hover:not(:disabled) {
  background: #3b576e;
  transform: translateY(-1px);
}

.btn-xac-nhan-vao-ca:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  filter: grayscale(20%);
}

/* ================= HEADER ACTIVE SHIFT & KẾT THÚC CA ================= */
.header-shift-in-progress {
  display: flex;
  align-items: center;
  gap: 8px;
}

.active-shift-pill {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  padding: 5px 12px;
  font-size: 0.8rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 7px;
  user-select: none;
  box-shadow: 0 1px 2px rgba(22, 101, 52, 0.06);
}

.active-shift-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #16a34a;
  display: inline-block;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.25);
  animation: pulseShiftDot 2s infinite ease-in-out;
}

@keyframes pulseShiftDot {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

.active-shift-name {
  font-weight: 600;
}

.active-shift-divider {
  color: #86efac;
  font-weight: 400;
  font-size: 0.85rem;
}

.active-shift-clock {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 0.82rem;
  font-weight: 600;
  color: #15803d;
}

.btn-header-end-shift {
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fca5a5;
  border-radius: 20px;
  padding: 5px 14px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(185, 28, 28, 0.15);
}

.btn-header-end-shift:hover {
  background: #ef4444;
  color: #ffffff;
  border-color: #ef4444;
  box-shadow: 0 3px 8px rgba(239, 68, 68, 0.3);
  transform: translateY(-1px);
}

/* ================= POPUP KẾT THÚC CA & BÀN GIAO TIỀN MẶT ================= */
.dong-ca-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  animation: fadeInModal 0.25s ease;
}

.dong-ca-popup-card {
  background: #ffffff;
  width: 720px;
  max-width: 95vw;
  max-height: 92vh;
  border-radius: 16px;
  box-shadow: 0 20px 45px -10px rgba(0, 0, 0, 0.35);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  animation: scaleUpModal 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.dong-ca-popup-header {
  background: var(--blue, #496883);
  color: #ffffff;
  padding: 16px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dong-ca-popup-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #ffffff;
}

.dong-ca-popup-subtitle {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 3px;
}

.btn-close-dong-ca {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.3rem;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.btn-close-dong-ca:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.15);
}

.dong-ca-popup-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Badge thông tin ca hiện tại */
.current-shift-badge-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.shift-badge-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge-title {
  font-size: 0.88rem;
  color: #1e293b;
}

.badge-title strong {
  color: var(--blue, #496883);
}

.shift-badge-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 0.78rem;
  color: #64748b;
}

.shift-badge-details strong {
  color: #334155;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.25);
  display: inline-block;
}

/* Metrics két tiền thời gian thực */
.dong-ca-metrics-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metrics-sec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sec-title {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--blue, #496883);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.btn-refresh-metrics {
  background: none;
  border: none;
  color: var(--blue, #496883);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  transition: opacity 0.2s;
}

.btn-refresh-metrics:hover:not(:disabled) {
  text-decoration: underline;
}

.dong-ca-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

@media (max-width: 680px) {
  .dong-ca-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.metric-card {
  padding: 10px 12px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  border: 1px solid transparent;
}

.bg-warm {
  background-color: #faf5ee;
  border-color: #f0e6d6;
}

.bg-green {
  background-color: #f1f8e9;
  border-color: #dcedc8;
}

.bg-blue {
  background-color: #e3f2fd;
  border-color: #bbdefb;
}

.bg-highlight {
  background-color: #fff8e1;
  border-color: #ffe082;
}

.m-label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  color: #616161;
}

.m-val {
  font-size: 1.05rem;
  font-weight: 800;
}

.text-brown { color: #8c7355; }
.text-green { color: #2e7d32; }
.text-blue { color: #1565c0; }
.text-primary-highlight { color: #e65100; }

/* Grid form 2 cột */
.dong-ca-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 600px) {
  .dong-ca-form-grid {
    grid-template-columns: 1fr;
  }
}

.form-col {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.dong-ca-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #334155;
}

.input-currency-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.input-currency-wrapper input {
  padding-right: 48px;
}

.dong-ca-input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.88rem;
  color: #1e293b;
  outline: none;
  transition: all 0.15s ease;
  box-sizing: border-box;
}

.dong-ca-input:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.currency-tag {
  position: absolute;
  right: 12px;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--blue, #496883);
  pointer-events: none;
}

.input-error-border {
  border-color: #e53935 !important;
  background-color: #fff9f9;
}

/* Banner chênh lệch tiền */
.diff-result-box {
  padding: 10px 14px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.diff-match {
  background-color: #e8f5e9;
  border: 1px solid #c8e6c9;
  color: #2e7d32;
}

.diff-surplus {
  background-color: #fff3e0;
  border: 1px solid #ffe0b2;
  color: #e65100;
}

.diff-deficit {
  background-color: #ffebee;
  border: 1px solid #ffcdd2;
  color: #c62828;
}

.diff-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.diff-icon-symbol {
  font-size: 1.25rem;
}

.diff-main-msg {
  display: block;
  font-size: 0.88rem;
  font-weight: 700;
}

.diff-sub-msg {
  font-size: 0.78rem;
  margin-top: 1px;
  opacity: 0.9;
}

.diff-tag {
  font-size: 1.05rem;
  font-weight: 800;
  padding: 3px 10px;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.7);
}

.dong-ca-reason-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.dong-ca-textarea {
  width: 100%;
  min-height: 56px;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.88rem;
  font-family: inherit;
  box-sizing: border-box;
  outline: none;
  resize: vertical;
}

.dong-ca-textarea:focus {
  border-color: var(--blue, #496883);
  box-shadow: 0 0 0 3px rgba(73, 104, 131, 0.12);
}

.text-warn {
  color: #c2410c;
}

.dong-ca-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.btn-cancel-dong-ca {
  padding: 9px 20px;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-cancel-dong-ca:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.btn-submit-dong-ca {
  padding: 9px 24px;
  background: var(--blue, #496883);
  border: none;
  border-radius: 8px;
  font-size: 0.86rem;
  font-weight: 700;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(73, 104, 131, 0.25);
}

.btn-submit-dong-ca:hover:not(:disabled) {
  background: #3b576e;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(73, 104, 131, 0.35);
}

.btn-submit-dong-ca:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  filter: grayscale(20%);
}

/* ================= MODAL BIÊN BẢN IN GIAO CA ================= */
.modal-backdrop-clean {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 1.5rem;
}

.modal-dialog-clean {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  box-sizing: border-box;
  animation: scaleUpModal 0.25s ease-out;
}

.modal-receipt {
  width: 580px;
  max-width: 95vw;
}

.modal-header-clean {
  padding: 1.15rem 1.4rem;
  border-bottom: 1px solid #f0ece5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title-clean {
  font-size: 1.12rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #8c979d;
  cursor: pointer;
}

.modal-body-clean {
  padding: 1.5rem;
  overflow-y: auto;
}

.modal-footer-clean {
  padding: 1rem 1.4rem;
  border-top: 1px solid #f0ece5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel-modal {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-print-action {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background-color: var(--blue, #496883);
  color: #ffffff;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(73, 104, 131, 0.25);
}

.btn-print-action:hover {
  background-color: #3b576e;
}

.printable-receipt {
  font-family: Arial, sans-serif;
  color: #222;
  font-size: 13px;
  line-height: 1.45;
}

.receipt-header {
  text-align: center;
  margin-bottom: 10px;
}

.receipt-brand {
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 1.5px;
  margin: 0 0 2px 0;
  color: #333;
}

.receipt-subtitle {
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: #666;
}

.receipt-code {
  font-size: 12px;
  color: #555;
}

.receipt-divider {
  border-bottom: 1px dashed #bbb;
  margin: 10px 0 12px 0;
}

.receipt-meta-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.receipt-meta-row {
  display: flex;
  justify-content: space-between;
}

.meta-label {
  color: #666;
}

.meta-val {
  color: #111;
  text-align: right;
}

.receipt-financial-table {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 6px 0;
}

.fin-row {
  display: flex;
  justify-content: space-between;
}

.fin-expected strong {
  color: #e65100;
}

.fin-actual strong {
  color: var(--blue, #496883);
  font-size: 14px;
}

.fin-reason {
  background: #fbf9f6;
  border-left: 3px solid #8c7355;
  padding: 6px 10px;
  font-size: 12px;
  margin-top: 4px;
}

.receipt-signatures {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 10px;
}

.sig-col {
  text-align: center;
  width: 45%;
}

.sig-title {
  font-weight: 700;
  margin: 0;
  font-size: 12px;
}

.sig-sub {
  font-size: 11px;
  color: #777;
  margin: 2px 0 0 0;
}

.sig-space {
  height: 50px;
}

.sig-name {
  font-weight: 600;
  margin: 0;
  font-size: 12px;
}

/* Live Chat Bell Notification & Sidebar Badge */
.header-chat-bell-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.btn-header-bell {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #ebd9c8;
  color: #634b35;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-header-bell:hover {
  background: #fbf8f5;
  border-color: #c5a25d;
  color: #1e293b;
  transform: translateY(-1px);
}

.btn-header-bell.has-unread {
  color: #b45309;
  border-color: #f59e0b;
}

.btn-header-bell.has-unread svg {
  animation: bell-shake 2.5s infinite;
}

@keyframes bell-shake {
  0%, 100% { transform: rotate(0); }
  5% { transform: rotate(14deg); }
  10% { transform: rotate(-14deg); }
  15% { transform: rotate(10deg); }
  20% { transform: rotate(-10deg); }
  25% { transform: rotate(0); }
}

.bell-badge-pill {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: #ffffff;
  font-size: 10.5px;
  font-weight: 800;
  min-width: 19px;
  height: 19px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}

.sidebar-chat-badge {
  background: #ef4444;
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  margin-left: auto;
}

/* Bell Dropdown Popover */
.bell-dropdown-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 360px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #ebd9c8;
  box-shadow: 0 14px 36px rgba(90, 70, 50, 0.16);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: dropdownIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}


.bph-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.bph-tabs-row {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  width: 100%;
}

.bph-tab-btn {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 4px 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
}

.bph-tab-btn:hover {
  background: #f8fafc;
  color: #1e293b;
}

.bph-tab-btn.active {
  background: #496883;
  color: #ffffff;
  border-color: #496883;
}

.bell-item-avatar.order-avatar {
  background: #d2a764;
  font-size: 16px;
}

.bell-item-avatar.chat-avatar {
  background: #496883;
  font-size: 16px;
}

.bell-popover-header {
  padding: 12px 16px;
  background: #fbf8f5;
  border-bottom: 1px solid #f2ece3;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bph-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: #1e293b;
}

.bph-count-badge {
  background: #fee2e2;
  color: #dc2626;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 12px;
}

.btn-quick-open-all {
  background: none;
  border: none;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 2px 6px;
}

.btn-quick-open-all:hover {
  text-decoration: underline;
}

.bell-popover-list {
  max-height: 320px;
  overflow-y: auto;
}

.bell-empty-box {
  padding: 30px 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.bell-item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f8fafc;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.bell-item-row:hover {
  background: #f8fafc;
}

.bell-item-row.unread {
  background: #fefce8;
}

.bell-item-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #0284c7;
  color: #ffffff;
  font-weight: 800;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bell-item-content {
  flex: 1;
  min-width: 0;
}

.bic-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.bic-name {
  font-size: 13px;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bic-time {
  font-size: 10.5px;
  color: #94a3b8;
}

.bic-msg {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bell-dot-unread {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
}

.bell-popover-footer {
  padding: 10px 16px;
  border-top: 1px solid #f2ece3;
  background: #fbf8f5;
  text-align: center;
}

.btn-bell-view-full {
  background: none;
  border: none;
  color: #634b35;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}

.btn-bell-view-full:hover {
  color: #b45309;
}

</style>