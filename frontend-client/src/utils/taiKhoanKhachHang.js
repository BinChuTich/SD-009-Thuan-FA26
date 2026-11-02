import { ref, computed } from 'vue'

const USER_STORAGE_KEY = 'fftshirt_client_user'

export const getStoredUser = () => {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY)
    if (!raw) return null
    const user = JSON.parse(raw)
    return user
  } catch (e) {
    return null
  }
}

export const currentClientUser = ref(getStoredUser())

export const isClientLoggedIn = computed(() => {
  return Boolean(currentClientUser.value && (currentClientUser.value.id || currentClientUser.value.name || currentClientUser.value.email || currentClientUser.value.phone))
})

export const loginClient = (userData) => {
  if (!userData) return
  currentClientUser.value = userData
  try {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData))
  } catch (e) {
    console.error('Lỗi lưu userData vào localStorage:', e)
  }
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('client-auth-change', { detail: userData }))
  }
}

export const logoutClient = () => {
  currentClientUser.value = null
  try {
    localStorage.removeItem(USER_STORAGE_KEY)
    localStorage.removeItem('client_user')
    localStorage.removeItem('fftshirt_last_google_account')
    localStorage.removeItem('fftshirt_device_google_accounts')
  } catch (e) {}
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('client-auth-change', { detail: null }))
  }
}

if (typeof window !== 'undefined') {
  window.addEventListener('client-auth-change', (e) => {
    if (e && e.detail !== undefined && e.detail !== null) {
      currentClientUser.value = e.detail
    } else if (e && e.detail === null) {
      currentClientUser.value = null
    } else {
      currentClientUser.value = getStoredUser()
    }
  })
  window.addEventListener('storage', (e) => {
    if (e.key === USER_STORAGE_KEY) {
      currentClientUser.value = getStoredUser()
    }
  })
  window.loginClient = loginClient
  window.logoutClient = logoutClient
}
