<template>
  <div class="cccd-modal-overlay" @click.self="closeModal">
    <div class="cccd-modal-dialog">
      <!-- Modal Header -->
      <div class="cccd-modal-header">
        <div class="header-title-box">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="7" height="7"></rect>
            <rect x="14" y="3" width="7" height="7"></rect>
            <rect x="14" y="14" width="7" height="7"></rect>
            <rect x="3" y="14" width="7" height="7"></rect>
          </svg>
          <h3 class="modal-title">Quét thẻ Căn cước công dân (CCCD)</h3>
        </div>
        <button type="button" class="btn-close-modal" @click="closeModal" title="Đóng">✕</button>
      </div>

      <!-- Mode Tabs -->
      <div class="scan-tabs-bar">
        <button
          type="button"
          class="scan-tab-btn"
          :class="{ active: activeTab === 'camera' }"
          @click="switchTab('camera')"
        >
          📷 Quét bằng Camera / Webcam
        </button>
        <button
          type="button"
          class="scan-tab-btn"
          :class="{ active: activeTab === 'file' }"
          @click="switchTab('file')"
        >
          📁 Tải ảnh thẻ CCCD
        </button>
      </div>

      <!-- Modal Body -->
      <div class="cccd-modal-body">
        <!-- TAB 1: CAMERA -->
        <div v-show="activeTab === 'camera'" class="tab-panel camera-panel">
          <div v-if="cameraError" class="camera-error-box">
            <div class="error-icon">⚠️</div>
            <div class="error-message">{{ cameraError }}</div>
            <button type="button" class="btn-retry-camera" @click="initCamera">Thử lại</button>
          </div>

          <div v-show="!cameraError" class="camera-stream-wrapper">
            <div id="cccd-reader" class="camera-stream-box"></div>
            <div v-if="cameraLoading" class="camera-loading-overlay">
              <div class="spinner"></div>
              <span>Đang kết nối camera...</span>
            </div>
          </div>

          <div class="scan-instruction">
            <span>💡 <strong>Hướng dẫn:</strong> Giơ mã QR ở góc trên bên phải của thẻ CCCD gắn chip vào trước camera để quét tự động.</span>
          </div>

          <!-- Camera Selector nếu có nhiều camera -->
          <div v-if="cameras.length > 1" class="camera-select-wrap">
            <label>Chọn camera:</label>
            <select v-model="selectedCameraId" @change="changeCamera">
              <option v-for="cam in cameras" :key="cam.id" :value="cam.id">
                {{ cam.label || `Camera ${cam.id}` }}
              </option>
            </select>
          </div>
        </div>

        <!-- TAB 2: TẢI ẢNH FILE -->
        <div v-show="activeTab === 'file'" class="tab-panel file-panel">
          <label class="file-dropzone" :class="{ 'has-file': filePreview }">
            <input
              type="file"
              accept="image/*"
              style="display: none"
              @change="onFileSelected"
            />
            <div v-if="!filePreview" class="dropzone-empty">
              <div class="upload-icon">📷</div>
              <div class="upload-title">Nhấp để chọn ảnh thẻ CCCD hoặc ảnh mã QR</div>
              <div class="upload-desc">Hỗ trợ các định dạng PNG, JPG, JPEG, WEBP</div>
            </div>
            <div v-else class="dropzone-preview">
              <img :src="filePreview" alt="CCCD Preview" class="preview-img" />
              <div class="preview-overlay">
                <span>Nhấp để chọn ảnh khác</span>
              </div>
            </div>
          </label>

          <div v-if="isProcessingFile" class="file-processing-status">
            <div class="spinner"></div>
            <span>Đang giải mã QR từ hình ảnh...</span>
          </div>

          <div class="scan-instruction">
            <span>💡 <strong>Mẹo:</strong> Hãy chụp ảnh rõ nét mã QR vuông ở góc trên bên phải của thẻ CCCD để hệ thống nhận diện tốt nhất.</span>
          </div>
        </div>

        <!-- Thông báo lỗi khi quét không thành công -->
        <div v-if="scanErrorMessage" class="scan-alert-error">
          {{ scanErrorMessage }}
        </div>
      </div>

      <!-- Modal Footer -->
      <div class="cccd-modal-footer">
        <button type="button" class="btn-cancel" @click="closeModal">Đóng</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'

const emit = defineEmits(['close', 'scan-success'])

const activeTab = ref('camera') // 'camera' | 'file' | 'barcode'
const cameras = ref([])
const selectedCameraId = ref('')
const cameraLoading = ref(false)
const cameraError = ref('')
const scanErrorMessage = ref('')

const filePreview = ref('')
const isProcessingFile = ref(false)

let html5QrCode = null

// Âm thanh Beep khi quét thành công
function playBeep() {
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (!AudioContextClass) return
    const ctx = new AudioContextClass()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'sine'
    osc.frequency.setValueAtTime(1046, ctx.currentTime) // High C note
    gain.gain.setValueAtTime(0.25, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.18)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.18)
  } catch (e) {
    // Không cho phép autoplay nếu chưa click
  }
}

// Bóc tách chuỗi QR CCCD gắn chip Việt Nam
// Định dạng chuẩn: <Số CCCD>|<Số CMND cũ>|<Họ và tên>|<Ngày sinh ddMMyyyy>|<Giới tính>|<Địa chỉ>|<Ngày cấp ddMMyyyy>
function parseCccdQr(qrText) {
  if (!qrText || typeof qrText !== 'string') return null
  const clean = qrText.trim()
  const parts = clean.split('|')
  
  if (parts.length < 5) {
    return null
  }

  const cccdNumber = parts[0]?.trim() || ''
  const cmndNumber = parts[1]?.trim() || ''
  const fullName = parts[2]?.trim() || ''
  const rawDob = parts[3]?.trim() || ''
  const rawGender = parts[4]?.trim() || ''
  const fullAddress = parts[5]?.trim() || ''
  const rawIssueDate = parts[6]?.trim() || ''

  // Chuyển đổi ngày sinh: ddMMyyyy -> YYYY-MM-DD
  let dob = ''
  if (rawDob.length === 8 && /^\d{8}$/.test(rawDob)) {
    const d = rawDob.substring(0, 2)
    const m = rawDob.substring(2, 4)
    const y = rawDob.substring(4, 8)
    dob = `${y}-${m}-${d}`
  } else if (/^\d{4}-\d{2}-\d{2}$/.test(rawDob)) {
    dob = rawDob
  }

  // Chuyển đổi giới tính: Nam -> true, Nữ -> false
  let gender = true
  const lowerGender = rawGender.toLowerCase()
  if (lowerGender === 'nữ' || lowerGender === 'nu' || lowerGender === 'female' || lowerGender === 'f') {
    gender = false
  }

  return {
    raw: clean,
    cccdNumber,
    cmndNumber,
    fullName,
    dob,
    gender,
    fullAddress,
    issueDate: rawIssueDate
  }
}

// Xử lý khi có kết quả quét thành công
function handleDecodedText(decodedText) {
  scanErrorMessage.value = ''
  const parsed = parseCccdQr(decodedText)
  
  if (!parsed || !parsed.fullName) {
    scanErrorMessage.value = 'Mã QR đã quét không đúng định dạng CCCD gắn chip (cần có Họ tên, Ngày sinh, Giới tính, Địa chỉ).'
    return
  }

  playBeep()
  stopCamera().finally(() => {
    emit('scan-success', parsed)
  })
}

// Khởi tạo Camera
async function initCamera() {
  cameraError.value = ''
  scanErrorMessage.value = ''
  cameraLoading.value = true

  try {
    await stopCamera()
    await nextTick()

    const devices = await Html5Qrcode.getCameras()
    cameras.value = devices || []

    if (!devices || devices.length === 0) {
      cameraError.value = 'Không tìm thấy thiết bị camera trên máy tính của bạn.'
      cameraLoading.value = false
      return
    }

    if (!selectedCameraId.value) {
      // Ưu tiên camera sau (back camera) nếu có, hoặc camera đầu tiên
      const backCam = devices.find(d => /back|rear|environment/i.test(d.label))
      selectedCameraId.value = backCam ? backCam.id : devices[0].id
    }

    html5QrCode = new Html5Qrcode('cccd-reader')

    const config = {
      fps: 12,
      qrbox: { width: 260, height: 260 },
      aspectRatio: 1.0
    }

    await html5QrCode.start(
      selectedCameraId.value,
      config,
      (decodedText) => {
        handleDecodedText(decodedText)
      },
      () => {
        // Frame scanning
      }
    )
  } catch (err) {
    console.error('Lỗi bật camera:', err)
    cameraError.value = 'Không thể khởi động camera. Vui lòng cấp quyền truy cập camera cho trình duyệt.'
  } finally {
    cameraLoading.value = false
  }
}

async function stopCamera() {
  if (html5QrCode) {
    try {
      if (html5QrCode.isScanning) {
        await html5QrCode.stop()
      }
      html5QrCode.clear()
    } catch (e) {
      // camera already stopped
    }
    html5QrCode = null
  }
}

async function changeCamera() {
  await stopCamera()
  await initCamera()
}

// Chuyển Tab
async function switchTab(tab) {
  activeTab.value = tab
  scanErrorMessage.value = ''

  if (tab === 'camera') {
    await initCamera()
  } else {
    await stopCamera()
  }
}

// Xử lý quét từ File ảnh
async function onFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return

  scanErrorMessage.value = ''
  isProcessingFile.value = true
  filePreview.value = URL.createObjectURL(file)

  try {
    const scanner = new Html5Qrcode('cccd-reader')
    const decodedText = await scanner.scanFile(file, true)
    handleDecodedText(decodedText)
  } catch (err) {
    console.warn('Lỗi đọc QR từ file:', err)
    scanErrorMessage.value = 'Không tìm thấy mã QR trên ảnh đã chọn. Vui lòng chọn ảnh chụp rõ nét hơn hoặc cắt riêng vùng mã QR.'
  } finally {
    isProcessingFile.value = false
    e.target.value = ''
  }
}

// Xử lý nhập / bắn mã vạch
function processBarcodeText() {
  const text = barcodeInputText.value.trim()
  if (!text) return

  handleDecodedText(text)
}

function closeModal() {
  stopCamera()
  emit('close')
}

onMounted(() => {
  initCamera()
})

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.cccd-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(18, 25, 30, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 1.25rem;
  backdrop-filter: blur(2px);
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.cccd-modal-dialog {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 620px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-height: 90vh;
}

.cccd-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.15rem 1.4rem;
  border-bottom: 1px solid #ebe8e1;
  background-color: #faf8f5;
}

.header-title-box {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  color: #496883;
}

.modal-title {
  margin: 0;
  font-size: 1.12rem;
  font-weight: 700;
  color: #384952;
}

.btn-close-modal {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #8c979d;
  cursor: pointer;
  padding: 0.2rem 0.5rem;
  line-height: 1;
  border-radius: 4px;
}
.btn-close-modal:hover {
  color: #c94a4a;
  background-color: #fceeee;
}

.scan-tabs-bar {
  display: flex;
  border-bottom: 1px solid #ebe8e1;
  background-color: #f7f5ef;
  padding: 0.4rem 0.75rem 0;
  gap: 0.35rem;
}

.scan-tab-btn {
  padding: 0.6rem 0.95rem;
  font-size: 0.88rem;
  font-weight: 600;
  border: none;
  background: transparent;
  color: #6c7a82;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  transition: all 0.2s;
}

.scan-tab-btn:hover {
  color: #496883;
  background-color: #eeebe3;
}

.scan-tab-btn.active {
  background-color: #ffffff;
  color: #496883;
  border-bottom: 2px solid #496883;
}

.cccd-modal-body {
  padding: 1.4rem 1.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Camera Stream Styling */
.camera-stream-wrapper {
  position: relative;
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  background-color: #1a2228;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.4);
  aspect-ratio: 1;
}

.camera-stream-box {
  width: 100%;
  height: 100%;
}

:deep(#cccd-reader video) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  border-radius: 10px;
}

:deep(#cccd-reader__scan_region) {
  border-radius: 8px;
}

.camera-loading-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(20, 26, 31, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #e0e7ec;
  gap: 0.75rem;
  font-size: 0.9rem;
}

.camera-error-box {
  background-color: #fff4f4;
  border: 1px dashed #f2b6b6;
  border-radius: 8px;
  padding: 1.75rem 1.25rem;
  text-align: center;
  color: #a63f3f;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
}

.error-icon {
  font-size: 2rem;
}

.btn-retry-camera {
  margin-top: 0.5rem;
  padding: 0.45rem 1.2rem;
  border: 1px solid #c96565;
  background-color: #ffffff;
  color: #a63f3f;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

.camera-select-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  font-size: 0.88rem;
  color: #556670;
}

.camera-select-wrap select {
  height: 2.2rem;
  padding: 0 0.75rem;
  border: 1px solid #d5dbde;
  border-radius: 6px;
  outline: none;
  background-color: #faf9f6;
  color: #384952;
}

/* File Dropzone */
.file-dropzone {
  border: 2px dashed #cbd5dd;
  border-radius: 10px;
  padding: 2.2rem 1.5rem;
  text-align: center;
  cursor: pointer;
  background-color: #fbfbf9;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.file-dropzone:hover {
  border-color: #496883;
  background-color: #f4f7f9;
}

.dropzone-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.upload-icon {
  font-size: 2.4rem;
}

.upload-title {
  font-weight: 700;
  color: #3f525c;
  font-size: 0.95rem;
}

.upload-desc {
  font-size: 0.82rem;
  color: #839199;
}

.dropzone-preview {
  position: relative;
  max-width: 320px;
  max-height: 220px;
  border-radius: 8px;
  overflow: hidden;
}

.preview-img {
  width: 100%;
  height: auto;
  display: block;
}

.preview-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.45);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s;
}

.dropzone-preview:hover .preview-overlay {
  opacity: 1;
}

.file-processing-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  color: #496883;
  font-size: 0.9rem;
  font-weight: 600;
}

/* Common Instructions & Errors */
.scan-instruction {
  background-color: #edf5f9;
  border: 1px solid #d3e4ed;
  border-radius: 8px;
  padding: 0.75rem 0.95rem;
  font-size: 0.84rem;
  color: #3a5b70;
  line-height: 1.45;
}

.scan-alert-error {
  background-color: #fdf0f0;
  border: 1px solid #f8c8c8;
  color: #b83a3a;
  padding: 0.75rem 0.95rem;
  border-radius: 8px;
  font-size: 0.86rem;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.file-processing-status .spinner {
  border-color: rgba(73, 104, 131, 0.25);
  border-top-color: #496883;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.cccd-modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 0.9rem 1.4rem;
  border-top: 1px solid #ebe8e1;
  background-color: #faf8f5;
}

.btn-cancel {
  padding: 0.5rem 1.3rem;
  border: 1px solid #d5dbde;
  background-color: #ffffff;
  color: #556670;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-cancel:hover {
  background-color: #f2f0eb;
}
</style>

