// Các hàm dùng chung cho trang Khách hàng / Nhân viên

// "Trần Vũ Tùng Anh" -> "TA"
export const initialsOf = (name) => {
    if (!name || !name.trim()) return 'FF'
    const words = name.trim().split(/\s+/)
    if (words.length === 1) return words[0].substring(0, 2).toUpperCase()
    return (words[0][0] + words[words.length - 1][0]).toUpperCase()
}

// Boolean (DB) -> chữ hiển thị
export const genderText = (value) => {
    if (value === true) return 'Nam'
    if (value === false) return 'Nữ'
    return '—'
}

// Chuỗi trên form -> Boolean gửi lên API ('Khác' / rỗng -> null)
export const genderToBool = (value) => {
    if (value === 'Nam') return true
    if (value === 'Nữ') return false
    return null
}

export const joinAddress = (...parts) => parts.filter((p) => p && String(p).trim()).join(', ')

export const isValidEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)

// SĐT Việt Nam: 0xxxxxxxxx hoặc +84xxxxxxxxx
export const isValidPhone = (v) => /^(0|\+84)\d{9}$/.test(v)

// Lấy thông báo lỗi từ response của ApiExceptionHandler
export const errorMessage = (e, fallback = 'Có lỗi xảy ra, vui lòng thử lại') =>
    e?.response?.data?.message || (e?.code === 'ERR_NETWORK' ? 'Không kết nối được máy chủ (localhost:8080)' : fallback)

const pad = (n) => String(n).padStart(2, '0')
export const timestamp = () => {
    const d = new Date()
    return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

// Tải một Blob về máy
export const downloadBlob = (blob, filename) => {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
}