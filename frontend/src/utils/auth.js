// Quản lý thông tin phiên làm việc và người dùng đang đăng nhập
const CURRENT_USER_KEY = 'ff_current_user'

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
    const normalized = {
      ...user,
      role: user.tenVaiTro || user.role || (user.maVaiTro === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên'),
      maVaiTro: user.maVaiTro || (user.role === 'Quản trị viên' ? 'ADMIN' : 'NV')
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
  window.dispatchEvent(new CustomEvent('auth-changed', { detail: null }))
}

/**
 * Lấy mã vai trò hiện tại (ADMIN hoặc NV)
 */
export function getRoleCode() {
  const user = getCurrentUser()
  if (!user) return null
  if (user.maVaiTro) return user.maVaiTro.toUpperCase()
  if (user.role) {
    const r = user.role.toLowerCase()
    if (r.includes('quản trị') || r.includes('admin')) return 'ADMIN'
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
