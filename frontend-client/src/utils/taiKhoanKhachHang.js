import { ref, computed } from 'vue'

const USER_STORAGE_KEY = 'fftshirt_client_user'

const getStoredUser = () => {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY)
    if (!raw) return null
    const user = JSON.parse(raw)
    // Tự động dọn dẹp tài khoản mẫu cũ nếu người dùng còn lưu trong trình duyệt
    if (user && (user.name === 'Dương Ngọc Chiến' || user.phone === '0988776655' || (user.email && user.email.includes('chien')))) {
      localStorage.removeItem(USER_STORAGE_KEY)
      return null
    }
    return user
  } catch (e) {
    return null
  }
}

export const currentClientUser = ref(getStoredUser())

export const isClientLoggedIn = computed(() => {
  return !!currentClientUser.value && !!currentClientUser.value.name
})

export const loginClient = (userData) => {
  currentClientUser.value = userData
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData))
}

export const logoutClient = () => {
  currentClientUser.value = null
  localStorage.removeItem(USER_STORAGE_KEY)
}
