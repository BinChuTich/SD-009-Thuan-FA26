<template>
  <div class="mobile-scanner-root">
    <!-- Header Mobile App -->
    <header class="scanner-header">
      <div class="header-content">
        <div class="brand-row">
          <span class="brand-badge">⚡ POS SYNC</span>
          <span class="brand-title">FF T-SHIRT SCANNER</span>
        </div>
        <div class="session-pill" v-if="sessionId">
          <span class="status-dot pulsating"></span>
          <span class="session-text">Đang kết nối: <strong>{{ invoiceCode || ('HĐ #' + sessionId) }}</strong></span>
        </div>
      </div>
    </header>

    <main class="scanner-main">
      <!-- Màn hình khi chưa có sessionId -->
      <div v-if="!sessionId" class="no-session-card">
        <div class="empty-icon">📱</div>
        <h3>Chưa chọn hóa đơn cần quét!</h3>
        <p>Vui lòng dùng máy ảnh quét mã QR từ màn hình <strong>Bán hàng tại quầy</strong> trên máy tính để kết nối tự động.</p>
        <div class="manual-session-box">
          <input 
            type="text" 
            v-model="manualSessionInput" 
            placeholder="Hoặc nhập Mã hóa đơn / ID..." 
            class="manual-input"
          />
          <button @click="applyManualSession" class="btn-connect">Kết nối ngay</button>
        </div>
      </div>

      <!-- Khung camera quét Barcode & QR Code -->
      <div v-else class="scanner-view-container">
        <!-- Vùng render camera html5-qrcode -->
        <div class="camera-viewport-card">
          <div id="qr-reader-mobile" class="reader-box"></div>
          
          <!-- Lớp phủ laser quét chuyển động -->
          <div v-if="isScanning" class="scanner-overlay-aim">
            <div class="corner-border tl"></div>
            <div class="corner-border tr"></div>
            <div class="corner-border bl"></div>
            <div class="corner-border br"></div>
            <div class="laser-beam"></div>
            <span class="aim-hint">Hướng camera vào Barcode hoặc mã QR</span>
          </div>

          <div v-if="cameraError" class="camera-error-banner">
            <span>⚠️ {{ cameraError }}</span>
            <button @click="startScanner" class="btn-retry">Thử lại</button>
          </div>
        </div>

        <!-- Thanh công cụ điều khiển camera -->
        <div class="camera-toolbar">
          <button @click="toggleScanning" class="tool-btn" :class="{ 'btn-paused': !isScanning }">
            {{ isScanning ? '⏸ Tạm dừng' : '▶ Tiếp tục quét' }}
          </button>
          <button @click="switchCamera" class="tool-btn" title="Đổi camera">
            🔄 Đổi camera
          </button>
          <button v-if="supportsTorch" @click="toggleTorch" class="tool-btn" :class="{ 'btn-torch-on': isTorchOn }">
            💡 {{ isTorchOn ? 'Tắt đèn' : 'Bật đèn' }}
          </button>
        </div>

        <!-- Thẻ thông báo sản phẩm vừa quét thành công -->
        <transition name="slide-up">
          <div v-if="lastScannedProduct" class="scanned-product-toast">
            <div class="toast-header-row">
              <span class="badge-success">✓ ĐÃ BẮN SANG MÁY TÍNH</span>
              <span class="toast-time">{{ lastScannedTime }}</span>
            </div>
            <div class="toast-body-row">
              <div class="prod-thumb">
                <img v-if="lastScannedProduct.image" :src="lastScannedProduct.image" alt="SP" />
                <span v-else class="thumb-placeholder">👕</span>
              </div>
              <div class="prod-info-col">
                <div class="prod-name-title">{{ lastScannedProduct.name || 'Sản phẩm' }}</div>
                <div class="prod-sub-meta">
                  <span v-if="lastScannedProduct.color" class="chip-attr">{{ lastScannedProduct.color }}</span>
                  <span v-if="lastScannedProduct.size" class="chip-attr">{{ lastScannedProduct.size }}</span>
                  <span class="chip-code">{{ lastScannedProduct.variantCode || lastScannedProduct.barcode }}</span>
                </div>
                <div class="prod-price-tag">{{ formatCurrency(lastScannedProduct.price) }}</div>
              </div>
            </div>
          </div>
        </transition>

        <!-- Nhập mã bằng tay nếu mã vạch bị rách/mờ -->
        <div class="manual-barcode-panel">
          <div class="manual-barcode-label">Hoặc nhập mã vạch bằng tay:</div>
          <div class="manual-input-group">
            <input 
              type="text" 
              v-model="manualBarcodeInput" 
              placeholder="VD: CTSP01 hoặc 893..." 
              class="manual-code-input"
              @keyup.enter="submitManualBarcode"
            />
            <button @click="submitManualBarcode" class="btn-send-code" :disabled="!manualBarcodeInput">
              Gửi sang PC
            </button>
          </div>
        </div>

        <!-- Lịch sử các lần quét trong phiên này -->
        <div class="scan-history-section" v-if="scanHistory.length > 0">
          <div class="history-title-row">
            <span>Đã quét ({{ scanHistory.length }} lần)</span>
            <button @click="scanHistory = []" class="btn-clear-history">Xóa danh sách</button>
          </div>
          <div class="history-scroll-list">
            <div v-for="(item, idx) in scanHistory" :key="idx" class="history-item-row">
              <span class="hist-idx">#{{ scanHistory.length - idx }}</span>
              <div class="hist-details">
                <span class="hist-name">{{ item.name || item.barcode }}</span>
                <span class="hist-code">{{ item.variantCode || item.barcode }} • {{ item.time }}</span>
              </div>
              <span class="hist-status">✓ Đã gửi</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer cố định -->
    <footer class="scanner-footer">
      <span>SD-009 • Cửa Hàng Thời Trang FF T-Shirt</span>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Html5Qrcode } from 'html5-qrcode'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const sessionId = ref('')
const invoiceCode = ref('')
const manualSessionInput = ref('')
const manualBarcodeInput = ref('')

const isScanning = ref(false)
const cameraError = ref('')
const isTorchOn = ref(false)
const supportsTorch = ref(false)
let html5QrCode = null
let currentCameraIndex = 0
let availableCameras = []

const lastScannedProduct = ref(null)
const lastScannedTime = ref('')
const scanHistory = ref([])
let lastScannedRaw = ''
let scanCooldownTimer = null

// Lấy hostname để gọi API đúng IP LAN
const getBaseUrl = () => {
  const host = window.location.hostname || 'localhost'
  return `http://${host}:8080`
}

// Định dạng tiền tệ VNĐ
const formatCurrency = (val) => {
  if (val == null || isNaN(val)) return '0 đ'
  return Number(val).toLocaleString('vi-VN') + ' đ'
}

// Âm thanh Beep báo quét thành công (Sử dụng Web Audio API tổng hợp)
const playBeepSound = () => {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)()
    const osc = audioCtx.createOscillator()
    const gain = audioCtx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(1400, audioCtx.currentTime) // Tần số 1400Hz cao và trong
    gain.gain.setValueAtTime(0.3, audioCtx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.12)
    osc.connect(gain)
    gain.connect(audioCtx.destination)
    osc.start()
    osc.stop(audioCtx.currentTime + 0.12)
  } catch (e) {}
}

// Rung nhẹ điện thoại khi quét
const triggerVibrate = () => {
  try {
    if (navigator && navigator.vibrate) {
      navigator.vibrate(80)
    }
  } catch (e) {}
}

// Bắt đầu khởi chạy camera quét mã
const startScanner = async () => {
  cameraError.value = ''
  try {
    if (!html5QrCode) {
      html5QrCode = new Html5Qrcode("qr-reader-mobile")
    }

    try {
      availableCameras = await Html5Qrcode.getCameras()
    } catch (e) {
      availableCameras = []
    }

    // Ưu tiên camera sau (environment/back)
    const cameraId = availableCameras.length > 0
      ? (availableCameras[currentCameraIndex % availableCameras.length]?.id || { facingMode: "environment" })
      : { facingMode: "environment" }

    const config = {
      fps: 15,
      qrbox: { width: 260, height: 180 }, // Tỉ lệ chữ nhật tối ưu cho cả Barcode dài lẫn QR vuông
      aspectRatio: 1.333
    }

    await html5QrCode.start(
      cameraId,
      config,
      onCodeScanned,
      () => {} // Bỏ qua log frame trống
    )

    isScanning.value = true

    // Kiểm tra hỗ trợ Flash torch
    try {
      const track = html5QrCode.getRunningTrackCameraCapabilities()
      supportsTorch.value = Boolean(track && track.torchFeature && track.torchFeature().isSupported())
    } catch (e) {
      supportsTorch.value = false
    }
  } catch (err) {
    console.error('Lỗi mở camera:', err)
    cameraError.value = 'Không thể truy cập camera. Vui lòng cấp quyền máy ảnh cho trình duyệt!'
    isScanning.value = false
  }
}

// Xử lý khi camera phát hiện mã
const onCodeScanned = async (decodedText) => {
  const code = (decodedText || '').trim()
  if (!code) return

  // Chống quét trùng lặp mã liên tiếp trong 2 giây
  if (code === lastScannedRaw && scanCooldownTimer) {
    return
  }

  lastScannedRaw = code
  if (scanCooldownTimer) clearTimeout(scanCooldownTimer)
  scanCooldownTimer = setTimeout(() => {
    lastScannedRaw = ''
  }, 2000)

  playBeepSound()
  triggerVibrate()
  await sendBarcodeToServer(code)
}

// Gửi mã lên server để phát qua máy tính POS
const sendBarcodeToServer = async (code) => {
  try {
    const payload = {
      sessionId: String(sessionId.value),
      barcode: code,
      deviceName: navigator.userAgent.includes('iPhone') ? 'iPhone' : (navigator.userAgent.includes('Android') ? 'Android' : 'Điện thoại')
    }

    const res = await axios.post(`${getBaseUrl()}/api/pos-sync/scan`, payload)
    if (res.data && res.data.success) {
      const prod = res.data.product || {}
      lastScannedProduct.value = {
        name: prod.name || 'Sản phẩm quét mã: ' + code,
        color: prod.color || '',
        size: prod.size || '',
        variantCode: prod.variantCode || code,
        barcode: code,
        price: prod.price != null ? prod.price : 0,
        image: prod.image || ''
      }
      const now = new Date()
      lastScannedTime.value = now.toLocaleTimeString('vi-VN')

      scanHistory.value.unshift({
        name: prod.name || code,
        variantCode: prod.variantCode || code,
        barcode: code,
        time: lastScannedTime.value
      })
    }
  } catch (err) {
    console.warn('Lỗi gửi mã tới máy POS:', err)
  }
}

// Bật / Tắt quét
const toggleScanning = async () => {
  if (isScanning.value) {
    if (html5QrCode) {
      try {
        await html5QrCode.stop()
      } catch (e) {}
    }
    isScanning.value = false
  } else {
    await startScanner()
  }
}

// Chuyển đổi camera trước/sau
const switchCamera = async () => {
  if (availableCameras.length < 2) return
  currentCameraIndex++
  if (html5QrCode && isScanning.value) {
    try {
      await html5QrCode.stop()
    } catch (e) {}
  }
  await startScanner()
}

// Bật / Tắt Flash
const toggleTorch = async () => {
  if (!html5QrCode) return
  try {
    isTorchOn.value = !isTorchOn.value
    await html5QrCode.applyVideoConstraints({
      advanced: [{ torch: isTorchOn.value }]
    })
  } catch (e) {
    isTorchOn.value = false
  }
}

// Gửi mã nhập tay
const submitManualBarcode = async () => {
  const code = (manualBarcodeInput.value || '').trim()
  if (!code) return
  playBeepSound()
  triggerVibrate()
  await sendBarcodeToServer(code)
  manualBarcodeInput.value = ''
}

// Áp dụng session nhập tay
const applyManualSession = () => {
  const val = (manualSessionInput.value || '').trim()
  if (!val) return
  sessionId.value = val
  invoiceCode.value = val.startsWith('HD') ? val : ('HĐ ' + val)
  startScanner()
}

onMounted(() => {
  // Lấy sessionId và code từ URL query
  const qSession = route.query.session || route.query.invoiceId || route.query.id
  const qCode = route.query.code || route.query.invoiceCode
  if (qSession) {
    sessionId.value = String(qSession)
    invoiceCode.value = qCode ? String(qCode) : `HĐ #${qSession}`
    startScanner()
  }
})

onUnmounted(async () => {
  if (html5QrCode) {
    try {
      await html5QrCode.stop()
      html5QrCode.clear()
    } catch (e) {}
  }
  if (scanCooldownTimer) clearTimeout(scanCooldownTimer)
})
</script>

<style scoped>
.mobile-scanner-root {
  min-height: 100vh;
  background-color: #0f172a;
  color: #f8fafc;
  display: flex;
  flex-direction: column;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  box-sizing: border-box;
}

.scanner-header {
  background: #1e293b;
  border-bottom: 1px solid #334155;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-badge {
  background: #3b82f6;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.brand-title {
  font-size: 15px;
  font-weight: 700;
  color: #f1f5f9;
}

.session-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #064e3b;
  border: 1px solid #059669;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 13px;
  color: #a7f3d0;
  width: fit-content;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
}

.pulsating {
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulse-green 1.8s infinite;
}

@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 7px rgba(16, 185, 129, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.scanner-main {
  flex: 1;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Khung camera */
.camera-viewport-card {
  position: relative;
  background: #000;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  border: 1px solid #334155;
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reader-box {
  width: 100% !important;
  border: none !important;
}

:deep(#qr-reader-mobile video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  border-radius: 14px;
}

/* Khung ngắm laser */
.scanner-overlay-aim {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 250px;
  height: 160px;
  pointer-events: none;
}

.corner-border {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: #38bdf8;
  border-style: solid;
}
.corner-border.tl { top: 0; left: 0; border-width: 3px 0 0 3px; border-top-left-radius: 8px; }
.corner-border.tr { top: 0; right: 0; border-width: 3px 3px 0 0; border-top-right-radius: 8px; }
.corner-border.bl { bottom: 0; left: 0; border-width: 0 0 3px 3px; border-bottom-left-radius: 8px; }
.corner-border.br { bottom: 0; right: 0; border-width: 0 3px 3px 0; border-bottom-right-radius: 8px; }

.laser-beam {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #ef4444, #f87171, #ef4444, transparent);
  box-shadow: 0 0 8px #ef4444;
  animation: laser-scan 1.8s ease-in-out infinite alternate;
}

@keyframes laser-scan {
  0% { top: 8%; opacity: 0.3; }
  50% { opacity: 1; }
  100% { top: 92%; opacity: 0.3; }
}

.aim-hint {
  position: absolute;
  bottom: -32px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
  background: rgba(15, 23, 42, 0.75);
  padding: 3px 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.camera-error-banner {
  position: absolute;
  padding: 16px;
  text-align: center;
  color: #fca5a5;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.btn-retry {
  background: #ef4444;
  color: #fff;
  border: none;
  padding: 6px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

/* Toolbar camera */
.camera-toolbar {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.tool-btn {
  flex: 1;
  background: #1e293b;
  color: #f1f5f9;
  border: 1px solid #334155;
  padding: 10px 8px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tool-btn:active {
  background: #334155;
}

.btn-paused {
  background: #b45309;
  border-color: #d97706;
}

.btn-torch-on {
  background: #ca8a04;
  border-color: #eab308;
  color: #000;
}

/* Thẻ thông báo SP vừa quét */
.scanned-product-toast {
  background: #1e293b;
  border: 1.5px solid #10b981;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.2);
}

.toast-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.badge-success {
  background: #059669;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 4px;
}

.toast-time {
  font-size: 11px;
  color: #94a3b8;
}

.toast-body-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.prod-thumb {
  width: 48px;
  height: 48px;
  background: #334155;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prod-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  font-size: 22px;
}

.prod-info-col {
  flex: 1;
  min-width: 0;
}

.prod-name-title {
  font-size: 14px;
  font-weight: 700;
  color: #f8fafc;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.prod-sub-meta {
  display: flex;
  gap: 6px;
  margin: 3px 0;
}

.chip-attr {
  background: #334155;
  color: #cbd5e1;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
}

.chip-code {
  color: #93c5fd;
  font-size: 11px;
  font-family: monospace;
}

.prod-price-tag {
  color: #fbbf24;
  font-size: 14px;
  font-weight: 700;
}

/* Nhập mã bằng tay */
.manual-barcode-panel {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 12px;
}

.manual-barcode-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.manual-input-group {
  display: flex;
  gap: 8px;
}

.manual-code-input {
  flex: 1;
  background: #0f172a;
  border: 1px solid #475569;
  color: #f8fafc;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}

.manual-code-input:focus {
  border-color: #3b82f6;
}

.btn-send-code {
  background: #3b82f6;
  color: #fff;
  border: none;
  padding: 8px 14px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-send-code:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Lịch sử quét */
.scan-history-section {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 12px;
}

.history-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
}

.btn-clear-history {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 11px;
  cursor: pointer;
}

.history-scroll-list {
  max-height: 160px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #0f172a;
  border-radius: 6px;
  font-size: 12px;
}

.hist-idx {
  color: #64748b;
  font-size: 11px;
  font-weight: bold;
}

.hist-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.hist-name {
  color: #f1f5f9;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hist-code {
  color: #64748b;
  font-size: 11px;
}

.hist-status {
  color: #10b981;
  font-size: 11px;
  font-weight: bold;
}

/* Khi chưa có session */
.no-session-card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px;
  padding: 30px 20px;
  text-align: center;
  margin-top: 30px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.no-session-card h3 {
  font-size: 18px;
  margin-bottom: 8px;
  color: #f8fafc;
}

.no-session-card p {
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 20px;
}

.manual-session-box {
  display: flex;
  gap: 8px;
  max-width: 320px;
  margin: 0 auto;
}

.manual-input {
  flex: 1;
  background: #0f172a;
  border: 1px solid #475569;
  color: #fff;
  padding: 10px 12px;
  border-radius: 8px;
  outline: none;
}

.btn-connect {
  background: #10b981;
  color: #fff;
  border: none;
  padding: 10px 16px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.scanner-footer {
  text-align: center;
  padding: 12px;
  font-size: 11px;
  color: #64748b;
  border-top: 1px solid #1e293b;
}

/* Transition */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}
.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>

