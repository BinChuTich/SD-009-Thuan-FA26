import { ref } from 'vue'

// Quản lý thông tin phiên làm việc và người dùng đang đăng nhập
const CURRENT_USER_KEY = 'ff_current_user'
const ACTIVE_SHIFT_KEY = 'ff_active_shift'

const activeShiftRef = ref(getActiveShift())

/**
 * Lấy thông tin ca làm việc đang mở trong phiên hiện tại
 */
export function getActiveShift() {
  try {
    const raw = localStorage.getItem(ACTIVE_SHIFT_KEY)
    if (raw) return JSON.parse(raw)
  } catch (e) {}
  return null
}

/**
 * Cập nhật ca làm việc đang mở
 */
export function setActiveShift(shift) {
  if (shift) {
    localStorage.setItem(ACTIVE_SHIFT_KEY, JSON.stringify(shift))
  } else {
    localStorage.removeItem(ACTIVE_SHIFT_KEY)
  }
  activeShiftRef.value = shift || null
  window.dispatchEvent(new CustomEvent('active-shift-changed', { detail: shift }))
}

/**
 * Xóa trạng thái ca đang mở
 */
export function clearActiveShift() {
  localStorage.removeItem(ACTIVE_SHIFT_KEY)
  activeShiftRef.value = null
  window.dispatchEvent(new CustomEvent('active-shift-changed', { detail: null }))
}

/**
 * Kiểm tra xem người dùng hiện tại có đang ở Chế độ xem (chưa vào ca làm việc) hay không
 * - Quản trị viên: luôn trả về false (không bị giới hạn)
 * - Nhân viên: trả về true nếu chưa có ca làm việc nào đang mở
 */
export function isInViewMode() {
  const user = getCurrentUser()
  if (!user) return true
  if (isAdmin()) return false
  return !activeShiftRef.value
}

/**
 * Lấy thông tin tài khoản người dùng đang đăng nhập hiện tại
 */
export function getCurrentUser() {
  try {
    const raw = localStorage.getItem(CURRENT_USER_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed && (parsed.id || parsed.maNhanVien || parsed.tenTaiKhoan)) {
        return parsed
      }
    }
  } catch (e) {
    console.error('Lỗi khi đọc thông tin người dùng từ localStorage:', e)
  }
  return null
}

/**
 * Lưu thông tin người dùng đăng nhập vào hệ thống
 */
export function setCurrentUser(user) {
  if (!user) {
    localStorage.removeItem(CURRENT_USER_KEY)
  } else {
    // Chuẩn hóa role và mã vai trò
    const rawRoleCode = (user.maVaiTro || (user.idVaiTro === 1 ? 'ADMIN' : (user.role === 'Quản trị viên' ? 'ADMIN' : ''))).toUpperCase()
    const rawRoleText = (user.tenVaiTro || user.role || '').toLowerCase()
    const isAdm = rawRoleCode === 'ADMIN' || rawRoleCode === 'QL' || rawRoleText.includes('quản trị') || rawRoleText.includes('admin') || rawRoleText.includes('quản lý')
    const normalizedRoleCode = isAdm ? 'ADMIN' : 'NV'
    const normalizedRoleName = isAdm ? 'Quản trị viên' : 'Nhân viên'

    const normalized = {
      ...user,
      role: normalizedRoleName,
      tenVaiTro: normalizedRoleName,
      maVaiTro: normalizedRoleCode
    }
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(normalized))
    window.dispatchEvent(new CustomEvent('auth-changed', { detail: normalized }))
    return normalized
  }
  window.dispatchEvent(new CustomEvent('auth-changed', { detail: null }))
  return null
}

/**
 * Đăng xuất khỏi hệ thống: xóa toàn bộ phiên đăng nhập
 */
export function logout() {
  localStorage.removeItem(CURRENT_USER_KEY)
  clearActiveShift()
  window.dispatchEvent(new CustomEvent('auth-changed', { detail: null }))
}

/**
 * Lấy mã vai trò hiện tại (ADMIN hoặc NV)
 */
export function getRoleCode() {
  const user = getCurrentUser()
  if (!user) return null
  const code = (user.maVaiTro || '').toUpperCase()
  if (code === 'ADMIN' || code === 'QL') return 'ADMIN'
  if (code === 'NV') return 'NV'
  if (user.role) {
    const r = user.role.toLowerCase()
    if (r.includes('quản trị') || r.includes('admin') || r.includes('quản lý')) return 'ADMIN'
    return 'NV'
  }
  if (user.tenVaiTro) {
    const r = user.tenVaiTro.toLowerCase()
    if (r.includes('quản trị') || r.includes('admin') || r.includes('quản lý')) return 'ADMIN'
    return 'NV'
  }
  return 'NV'
}

/**
 * Kiểm tra xem người dùng hiện tại có phải là Admin / Quản trị viên hay không
 */
export function isAdmin() {
  return getRoleCode() === 'ADMIN'
}

/**
 * Kiểm tra xem người dùng hiện tại có phải là Nhân viên bán hàng hay không
 */
export function isStaff() {
  return getRoleCode() === 'NV'
}

/**
 * Kiểm tra xem người dùng có quyền trong danh sách roles cho phép hay không
 * @param {Array<string>} allowedRoles ví dụ ['ADMIN', 'NV'] hoặc ['ADMIN']
 */
export function hasRole(allowedRoles = []) {
  if (!allowedRoles || allowedRoles.length === 0) return true
  const currentRole = getRoleCode()
  if (!currentRole) return false
  return allowedRoles.some(r => r.toUpperCase() === currentRole)
}

/**
 * Kiểm tra xem đối tượng nhân viên (trên bảng danh sách hoặc form chi tiết)
 * có phải chính là người dùng đang đăng nhập hay không
 */
export function isCurrentLoggedInUser(employee) {
  if (!employee) return false
  const current = getCurrentUser()
  if (!current) return false

  // 1. So sánh theo ID
  if (employee.id != null && current.id != null && Number(employee.id) === Number(current.id)) {
    return true
  }

  // 2. So sánh theo Mã nhân viên
  const empCode = employee.code || employee.maNhanVien
  if (empCode && current.maNhanVien && empCode.toString().trim().toLowerCase() === current.maNhanVien.toString().trim().toLowerCase()) {
    return true
  }

  // 3. So sánh theo Tên tài khoản
  if (employee.tenTaiKhoan && current.tenTaiKhoan && employee.tenTaiKhoan.trim().toLowerCase() === current.tenTaiKhoan.trim().toLowerCase()) {
    return true
  }

  // 4. So sánh theo Email
  if (employee.email && current.email && employee.email.trim().toLowerCase() === current.email.trim().toLowerCase()) {
    return true
  }

  return false
}
