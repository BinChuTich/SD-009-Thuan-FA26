<script setup>
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'scan-success'])

const activeTab = ref('camera') // 'camera' | 'file' | 'manual'
const manualCode = ref('')
const scanning = ref(false)
const cameraError = ref('')
const fileError = ref('')
const fileLoading = ref(false)
const readerElementId = 'qr-camera-viewport'
const fileInputRef = ref(null)

let html5QrCodeInstance = null

// Trích xuất mã hóa đơn từ văn bản quét được (chuỗi URL, JSON hoặc mã đơn thuần)
const extractInvoiceCode = (raw) => {
  if (!raw) return ''
  let text = String(raw).trim()

  // Kiểm tra nếu là JSON
  if (text.startsWith('{') && text.endsWith('}')) {
    try {
      const obj = JSON.parse(text)
      if (obj.code || obj.maHoaDon || obj.ma || obj.id) {
        return String(obj.code || obj.maHoaDon || obj.ma || obj.id).trim()
      }
    } catch (e) {
      // Bỏ qua lỗi parse JSON
    }
  }

  // Kiểm tra nếu là đường dẫn URL: http://domain/hoa-don/HD001
  if (text.includes('/hoa-don/')) {
    const parts = text.split('/hoa-don/')
    if (parts[1]) {
      const clean = parts[1].split('?')[0].split('#')[0].replace(/\/+$/, '').trim()
      if (clean) return clean
    }
  }

  return text
}

const handleSuccessfulScan = (decodedText) => {
  const code = extractInvoiceCode(decodedText)
  if (code) {
    stopScanner()
    emit('scan-success', code)
  }
}

// Khởi chạy camera quét QR
const startScanner = async () => {
  cameraError.value = ''
  scanning.value = false

  await nextTick()

  const element = document.getElementById(readerElementId)
  if (!element) return

  try {
    if (html5QrCodeInstance) {
      try {
        await html5QrCodeInstance.stop()
      } catch (e) {
        // Bỏ qua
      }
      try {
        html5QrCodeInstance.clear()
      } catch (e) {
        // Bỏ qua
      }
      html5QrCodeInstance = null
    }

    html5QrCodeInstance = new Html5Qrcode(readerElementId)
    scanning.value = true

    await html5QrCodeInstance.start(
      { facingMode: 'environment' },
      {
        fps: 12,
        qrbox: { width: 240, height: 240 },
        aspectRatio: 1.0
      },
      (decodedText) => {
        handleSuccessfulScan(decodedText)
      },
      () => {
        // Lỗi phân giải frame định kỳ, bỏ qua
      }
    )
  } catch (err) {
    console.warn('Lỗi mở camera quét QR:', err)
    scanning.value = false
    cameraError.value = 'Không thể truy cập camera. Vui lòng cấp quyền camera trong trình duyệt hoặc chuyển sang tab "Tải ảnh QR"!'
  }
}

// Dừng camera
const stopScanner = async () => {
  scanning.value = false
  if (html5QrCodeInstance) {
    try {
      if (html5QrCodeInstance.isScanning) {
        await html5QrCodeInstance.stop()
      }
      html5QrCodeInstance.clear()
    } catch (err) {
      console.warn('Lỗi dừng camera:', err)
    } finally {
      html5QrCodeInstance = null
    }
  }
}

// Xử lý khi chọn file ảnh QR từ thiết bị
const handleFileChange = async (event) => {
  const file = event.target?.files?.[0]
  if (!file) return

  fileError.value = ''
  fileLoading.value = true

  try {
    const scanner = new Html5Qrcode(readerElementId)
    const result = await scanner.scanFile(file, true)
    try {
      scanner.clear()
    } catch (e) {}

    fileLoading.value = false
    handleSuccessfulScan(result)
  } catch (err) {
    console.error('Không tìm thấy mã QR trong ảnh:', err)
    fileLoading.value = false
    fileError.value = 'Không nhận diện được mã QR hợp lệ trong ảnh tải lên. Vui lòng thử ảnh rõ nét hơn hoặc nhập mã trực tiếp!'
  } finally {
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
  }
}

// Xử lý gửi mã nhập thủ công
const handleManualSubmit = () => {
  const code = manualCode.value?.trim()
  if (!code) return
  emit('scan-success', code)
}

const closeModal = () => {
  stopScanner()
  emit('close')
}

watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      activeTab.value = 'camera'
      cameraError.value = ''
      fileError.value = ''
      manualCode.value = ''
      nextTick(() => {
        startScanner()
      })
    } else {
      stopScanner()
    }
  },
  { immediate: true }
)

watch(activeTab, (tab) => {
  if (tab === 'camera' && props.show) {
    startScanner()
  } else {
    stopScanner()
  }
})

onBeforeUnmount(() => {
  stopScanner()
})
</script>

<template>
  <transition name="modal-fade">
    <div v-if="show" class="qr-modal-backdrop" @click.self="closeModal">
      <div class="qr-modal-container">
        <!-- Header -->
        <div class="qr-modal-header">
          <div class="qr-header-title">
            <span class="qr-title-icon">📷</span>
            <h3>Quét Mã QR Hóa Đơn</h3>
          </div>
          <button class="btn-close-qr" type="button" @click="closeModal" title="Đóng">✕</button>
        </div>

        <!-- Tabs chuyển đổi phương thức -->
        <div class="qr-tabs-bar">
          <button
            type="button"
            class="qr-tab-btn"
            :class="{ active: activeTab === 'camera' }"
            @click="activeTab = 'camera'"
          >
            <span class="tab-icon">📹</span> Quét Camera
          </button>
          <button
            type="button"
            class="qr-tab-btn"
            :class="{ active: activeTab === 'file' }"
            @click="activeTab = 'file'"
          >
            <span class="tab-icon">📁</span> Tải Ảnh QR
          </button>
          <button
            type="button"
            class="qr-tab-btn"
            :class="{ active: activeTab === 'manual' }"
            @click="activeTab = 'manual'"
          >
            <span class="tab-icon">⌨️</span> Nhập Mã Nhanh
          </button>
        </div>

        <!-- Body -->
        <div class="qr-modal-body">
          <!-- Tab 1: Camera Scanner -->
          <div v-show="activeTab === 'camera'" class="tab-camera-content">
            <div class="camera-viewport-box">
              <div :id="readerElementId" class="qr-camera-element"></div>
              <div v-if="scanning" class="scanner-laser-line"></div>
              <div v-if="scanning" class="scanner-aim-corners">
                <div class="aim-corner tl"></div>
                <div class="aim-corner tr"></div>
                <div class="aim-corner bl"></div>
                <div class="aim-corner br"></div>
              </div>
            </div>

            <div v-if="cameraError" class="qr-alert-box error">
              <span class="alert-icon">⚠️</span>
              <span>{{ cameraError }}</span>
            </div>

            <p class="qr-guide-hint">
              Hướng camera thẳng vào mã QR trên hóa đơn in hoặc điện thoại để tự động nhận diện và mở chi tiết.
            </p>
          </div>

          <!-- Tab 2: File Upload Scanner -->
          <div v-show="activeTab === 'file'" class="tab-file-content">
            <div class="file-drop-area" @click="$refs.fileInputRef && $refs.fileInputRef.click()">
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                class="hidden-file-input"
                @change="handleFileChange"
              />
              <div class="drop-icon-wrap">
                <svg viewBox="0 0 24 24" width="42" height="42" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
              </div>
              <div class="drop-text-primary">Bấm vào đây để chọn ảnh mã QR</div>
              <div class="drop-text-secondary">Hỗ trợ định dạng JPG, PNG, WEBP rõ nét</div>
              <div v-if="fileLoading" class="file-scanning-spin">Đang quét ảnh mã QR...</div>
            </div>

            <div v-if="fileError" class="qr-alert-box error mt-12">
              <span class="alert-icon">⚠️</span>
              <span>{{ fileError }}</span>
            </div>
          </div>

          <!-- Tab 3: Manual Input -->
          <div v-show="activeTab === 'manual'" class="tab-manual-content">
            <label class="manual-input-label">Mã hóa đơn cần tra cứu:</label>
            <div class="manual-input-group">
              <input
                v-model="manualCode"
                type="text"
                placeholder="VD: HD001, HD002..."
                class="manual-input-field"
                @keydown.enter.prevent="handleManualSubmit"
              />
              <button class="btn-manual-submit" type="button" @click="handleManualSubmit" :disabled="!manualCode.trim()">
                Tra cứu
              </button>
            </div>
            <p class="manual-help-text">Nhập chính xác mã hóa đơn để mở trang chi tiết hóa đơn.</p>
          </div>
        </div>

        <!-- Footer -->
        <div class="qr-modal-footer">
          <button class="btn-qr-cancel" type="button" @click="closeModal">Đóng</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.qr-modal-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(23, 37, 51, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 16px;
  backdrop-filter: blur(4px);
}

.qr-modal-container {
  background: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: modalPop 0.22s ease-out;
}

@keyframes modalPop {
  from {
    transform: scale(0.94);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.qr-modal-header {
  padding: 16px 20px;
  background: #3e5c76;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.qr-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.qr-header-title h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #ffffff;
}

.qr-title-icon {
  font-size: 1.25rem;
}

.btn-close-qr {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.btn-close-qr:hover {
  background: rgba(255, 255, 255, 0.2);
}

.qr-tabs-bar {
  display: flex;
  background: #edf2f6;
  border-bottom: 1px solid #dbe3eb;
  padding: 4px;
  gap: 4px;
}

.qr-tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 9px 10px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #5c6b77;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.qr-tab-btn:hover {
  color: #2b353b;
  background: #e2eaf0;
}

.qr-tab-btn.active {
  background: #ffffff;
  color: #1d3557;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.tab-icon {
  font-size: 1rem;
}

.qr-modal-body {
  padding: 20px;
  min-height: 280px;
}

.camera-viewport-box {
  position: relative;
  width: 100%;
  max-width: 340px;
  height: 260px;
  margin: 0 auto 14px;
  border-radius: 12px;
  overflow: hidden;
  background: #111827;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #3e5c76;
}

.qr-camera-element {
  width: 100% !important;
  height: 100% !important;
}

.qr-camera-element video {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

.scanner-laser-line {
  position: absolute;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #ef4444;
  box-shadow: 0 0 10px #ef4444, 0 0 4px #f87171;
  animation: laserScan 2s infinite ease-in-out;
  pointer-events: none;
  z-index: 10;
}

@keyframes laserScan {
  0% { top: 15%; opacity: 0.4; }
  50% { top: 85%; opacity: 1; }
  100% { top: 15%; opacity: 0.4; }
}

.scanner-aim-corners {
  position: absolute;
  inset: 20px;
  pointer-events: none;
  z-index: 9;
}

.aim-corner {
  position: absolute;
  width: 22px;
  height: 22px;
  border-color: #38bdf8;
  border-style: solid;
}

.aim-corner.tl { top: 0; left: 0; border-width: 3px 0 0 3px; }
.aim-corner.tr { top: 0; right: 0; border-width: 3px 3px 0 0; }
.aim-corner.bl { bottom: 0; left: 0; border-width: 0 0 3px 3px; }
.aim-corner.br { bottom: 0; right: 0; border-width: 0 3px 3px 0; }

.qr-alert-box {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.83rem;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
}

.qr-alert-box.error {
  background-color: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.qr-guide-hint {
  text-align: center;
  font-size: 0.82rem;
  color: #64748b;
  margin: 10px 0 0;
  line-height: 1.4;
}

.file-drop-area {
  border: 2px dashed #94a3b8;
  border-radius: 12px;
  padding: 34px 20px;
  text-align: center;
  cursor: pointer;
  background: #f8fafc;
  transition: all 0.2s ease;
}

.file-drop-area:hover {
  background: #f1f5f9;
  border-color: #3e5c76;
}

.hidden-file-input {
  display: none;
}

.drop-icon-wrap {
  color: #3e5c76;
  margin-bottom: 12px;
}

.drop-text-primary {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.drop-text-secondary {
  font-size: 0.8rem;
  color: #64748b;
}

.file-scanning-spin {
  margin-top: 12px;
  font-size: 0.85rem;
  color: #0284c7;
  font-weight: 600;
}

.tab-manual-content {
  padding: 15px 5px;
}

.manual-input-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.manual-input-group {
  display: flex;
  gap: 8px;
}

.manual-input-field {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: #0f172a;
  outline: none;
  transition: border-color 0.2s;
}

.manual-input-field:focus {
  border-color: #3e5c76;
}

.btn-manual-submit {
  height: 40px;
  padding: 0 18px;
  background: #3e5c76;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-manual-submit:hover:not(:disabled) {
  background: #2b4257;
}

.btn-manual-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.manual-help-text {
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 8px;
}

.qr-modal-footer {
  padding: 12px 20px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
}

.btn-qr-cancel {
  height: 34px;
  padding: 0 16px;
  background: #e2e8f0;
  color: #334155;
  border: none;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-qr-cancel:hover {
  background: #cbd5e1;
}

.mt-12 {
  margin-top: 12px;
}
</style>

