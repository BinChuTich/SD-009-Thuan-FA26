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

  // Khởi tạo tài khoản Quản trị viên mặc định (NV001 - admin) nếu chưa có phiên đăng nhập
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
  return defaultAdmin
}

/**
 * Lưu thông tin người dùng đăng nhập vào hệ thống
 */
export function setCurrentUser(user) {
  if (!user) {
    localStorage.removeItem(CURRENT_USER_KEY)
  } else {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user))
  }
  window.dispatchEvent(new CustomEvent('auth-changed', { detail: user }))
}

/**
 * Đăng xuất khỏi hệ thống: xóa toàn bộ phiên đăng nhập
 */
export function logout() {
  localStorage.removeItem(CURRENT_USER_KEY)
  window.dispatchEvent(new CustomEvent('auth-changed', { detail: null }))
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

