import { ref } from 'vue'
import api from '@/api.js'

export const showMobileScannerModal = ref(false)
export const mobileScannerUrl = ref('')
export const isMobileScannerConnected = ref(false)

let mobileSyncEventSource = null
let mobileSyncPollTimer = null
const processedSyncEventIds = new Set()

// Âm thanh chuông báo nhận món quét từ điện thoại
export const playChimeSound = () => {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.type = 'triangle'
    osc.frequency.setValueAtTime(587.33, audioCtx.currentTime)
    osc.frequency.setValueAtTime(880, audioCtx.currentTime + 0.08)
    gain.gain.setValueAtTime(0.2, audioCtx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.25)
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.start()
    osc.stop(audioCtx.currentTime + 0.25)
  } catch (e) {}
}

export const openMobileScannerDialog = async (currentInvoice, onBarcodeReceived) => {
  if (!currentInvoice || !currentInvoice.id) {
    return false
  }
  showMobileScannerModal.value = true
  await initMobileScannerSync(currentInvoice, onBarcodeReceived)
  return true
}

export const closeMobileScannerDialog = () => {
  showMobileScannerModal.value = false
}

export const initMobileScannerSync = async (currentInvoice, onBarcodeReceived) => {
  if (!currentInvoice || !currentInvoice.id) return

  const sessionId = String(currentInvoice.id)
  const invCode = currentInvoice.code || `HD-${sessionId}`

  let host = window.location.hostname || 'localhost'
  const port = window.location.port || '5173'

  try {
    const resIp = await api.get('/api/pos-sync/lan-ip')
    if (resIp.data?.primaryIp && resIp.data.primaryIp !== 'localhost') {
      host = resIp.data.primaryIp
    }
  } catch (e) {}

  mobileScannerUrl.value = `http://${host}:${port}/quet-ma-pos?session=${sessionId}&code=${encodeURIComponent(invCode)}`

  // 1. Kết nối SSE Stream
  stopMobileScannerSync()

  try {
    const sseBase = api.defaults.baseURL || `http://${host}:8080`
    mobileSyncEventSource = new EventSource(`${sseBase}/api/pos-sync/stream/${sessionId}`)

    mobileSyncEventSource.addEventListener('ping', () => {
      isMobileScannerConnected.value = true
    })

    mobileSyncEventSource.addEventListener('scan', (e) => {
      isMobileScannerConnected.value = true
      try {
        const data = JSON.parse(e.data)
        handleScannedEvent(data, onBarcodeReceived)
      } catch (err) {}
    })

    mobileSyncEventSource.onerror = () => {}
  } catch (e) {}

  // 2. Polling dự phòng phòng khi mạng LAN ngắt kết nối SSE dài
  mobileSyncPollTimer = setInterval(async () => {
    if (!showMobileScannerModal.value && !isMobileScannerConnected.value) return
    try {
      const res = await api.get(`/api/pos-sync/poll/${sessionId}`)
      if (Array.isArray(res.data)) {
        res.data.forEach(item => {
          handleScannedEvent(item, onBarcodeReceived)
        })
      }
    } catch (e) {}
  }, 1500)
}

const handleScannedEvent = (data, onBarcodeReceived) => {
  if (!data || !data.id || processedSyncEventIds.has(data.id)) return
  processedSyncEventIds.add(data.id)

  const code = (data.barcode || '').trim()
  if (!code) return

  playChimeSound()
  if (typeof onBarcodeReceived === 'function') {
    onBarcodeReceived(code, data)
  }
}

export const stopMobileScannerSync = () => {
  if (mobileSyncEventSource) {
    try {
      mobileSyncEventSource.close()
    } catch (e) {}
    mobileSyncEventSource = null
  }
  if (mobileSyncPollTimer) {
    clearInterval(mobileSyncPollTimer)
    mobileSyncPollTimer = null
  }
}

