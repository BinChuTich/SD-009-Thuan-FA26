<script setup>
import { computed } from 'vue'
import { dialogState, closeAppDialog, toastsList } from '@/utils/thongBaoDialog.js'

const handleConfirm = () => {
  closeAppDialog(true)
}

const handleCancel = () => {
  closeAppDialog(false)
}

// Tách đoạn tên sản phẩm trong dấu ngoặc kép để làm nổi bật
const messageParts = computed(() => {
  const msg = dialogState.value.message || ''
  const match = msg.match(/^(.*?)(".*?")(.*)$/s)
  if (match) {
    return {
      before: match[1],
      highlight: match[2],
      after: match[3]
    }
  }
  return { before: msg, highlight: '', after: '' }
})
</script>

<template>
  <div>
    <!-- MODAL HỘP THOẠI XÁC NHẬN / THÔNG BÁO IN-APP -->
    <transition name="dialog-fade">
      <div v-if="dialogState.isOpen" class="dialog-backdrop" @click.self="handleCancel">
        <div class="dialog-card animate-pop">
          <!-- Icon biểu thị theo loại thông báo -->
          <div
            class="dialog-icon-wrapper"
            :class="[
              `icon-${dialogState.type}`,
              { 'icon-danger': dialogState.isDanger }
            ]"
          >
            <!-- Icon Xóa / Nguy hiểm -->
            <template v-if="dialogState.isDanger || dialogState.icon === 'trash'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h18"></path>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"></path>
                <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                <line x1="10" y1="11" x2="10" y2="17"></line>
                <line x1="14" y1="11" x2="14" y2="17"></line>
              </svg>
            </template>

            <!-- Icon Xác nhận / Cảnh báo bình thường -->
            <template v-else-if="dialogState.type === 'confirm' || dialogState.type === 'warning'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </template>

            <!-- Icon Lỗi -->
            <template v-else-if="dialogState.type === 'error'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
            </template>

            <!-- Icon Thành công -->
            <template v-else-if="dialogState.type === 'success'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="9 12 11.5 14.5 16 9.5"></polyline>
              </svg>
            </template>

            <!-- Icon Thông tin chung -->
            <template v-else>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
            </template>
          </div>

          <!-- Tiêu đề & nội dung -->
          <div class="dialog-content">
            <h3 class="dialog-title">{{ dialogState.title }}</h3>
            <p class="dialog-msg">
              <template v-if="messageParts.highlight">
                <span>{{ messageParts.before }}</span>
                <span class="dialog-item-chip">{{ messageParts.highlight }}</span>
                <span>{{ messageParts.after }}</span>
              </template>
              <template v-else>
                {{ dialogState.message }}
              </template>
            </p>
          </div>

          <!-- Nút hành động -->
          <div class="dialog-actions">
            <button
              v-if="dialogState.cancelText"
              type="button"
              class="btn-dialog-cancel"
              @click="handleCancel"
            >
              {{ dialogState.cancelText }}
            </button>
            <button
              type="button"
              class="btn-dialog-confirm"
              :class="[
                `btn-${dialogState.type}`,
                { 'btn-danger': dialogState.isDanger }
              ]"
              @click="handleConfirm"
            >
              {{ dialogState.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- TOAST NOTIFICATIONS NỔI GÓC PHẢI -->
    <div class="toasts-container">
      <transition-group name="toast-slide">
        <div
          v-for="t in toastsList"
          :key="t.id"
          class="toast-item"
          :class="`toast-${t.type}`"
        >
          <span class="toast-icon">
            <svg v-if="t.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
            <svg v-else-if="t.type === 'warning'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </span>
          <div class="toast-content-col">
            <div v-if="t.title" class="toast-title">{{ t.title }}</div>
            <div :class="t.title ? 'toast-desc' : 'toast-message'">{{ t.message }}</div>
          </div>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999999;
  padding: 16px;
}

.dialog-card {
  background: #ffffff;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 25px 60px -12px rgba(15, 23, 42, 0.3), 0 0 0 1px rgba(226, 232, 240, 0.9);
  padding: 28px 24px 22px;
  text-align: center;
  animation: modalScaleIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalScaleIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(6px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.dialog-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

/* Icon Styles with Soft Glow Rings */
.dialog-icon-wrapper.icon-danger {
  background: #fee2e2;
  color: #ef4444;
  border: 1.5px solid #fecaca;
  box-shadow: 0 0 0 8px #fef2f2;
}

.icon-confirm, .icon-warning {
  background: #fef3c7;
  color: #d97706;
  border: 1.5px solid #fde68a;
  box-shadow: 0 0 0 8px #fefce8;
}

.icon-error {
  background: #ffe4e6;
  color: #e11d48;
  border: 1.5px solid #fecdd3;
  box-shadow: 0 0 0 8px #fff1f2;
}

.icon-success {
  background: #dcfce7;
  color: #16a34a;
  border: 1.5px solid #bbf7d0;
  box-shadow: 0 0 0 8px #f0fdf4;
}

.icon-info {
  background: #eff6ff;
  color: #2563eb;
  border: 1.5px solid #bfdbfe;
  box-shadow: 0 0 0 8px #f8fafc;
}

.dialog-title {
  font-size: 1.22rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 8px;
  letter-spacing: -0.01em;
}

.dialog-msg {
  font-size: 0.92rem;
  color: #475569;
  line-height: 1.55;
  margin: 0 0 24px;
}

.dialog-item-chip {
  display: inline-block;
  font-weight: 700;
  color: #0f172a;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 6px;
  margin: 2px 2px;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-dialog-cancel {
  flex: 1;
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  background: #ffffff;
  color: #475569;
  border: 1.5px solid #cbd5e1;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-dialog-cancel:hover {
  background: #f1f5f9;
  color: #0f172a;
  border-color: #94a3b8;
  transform: translateY(-1px);
}

.btn-dialog-confirm {
  flex: 1;
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: 0.9rem;
  font-weight: 700;
  color: #ffffff;
  border: none;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.28);
}

.btn-dialog-confirm:hover {
  opacity: 0.94;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(217, 119, 6, 0.38);
}

.btn-dialog-confirm.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.35);
}

.btn-dialog-confirm.btn-danger:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: 0 6px 18px rgba(239, 68, 68, 0.45);
}

.btn-error {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.btn-success {
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
}

/* Transitions */
.dialog-fade-enter-active, .dialog-fade-leave-active {
  transition: opacity 0.22s ease;
}
.dialog-fade-enter-from, .dialog-fade-leave-to {
  opacity: 0;
}

/* TOASTS */
.toasts-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 1000000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 18px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 25px -4px rgba(15, 23, 42, 0.15), 0 0 0 1px rgba(226, 232, 240, 0.8);
  font-size: 14px;
  color: #1e293b;
  min-width: 280px;
  max-width: 400px;
}

.toast-content-col {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.toast-title {
  font-weight: 700;
  font-size: 14px;
  color: #0f172a;
  margin-bottom: 2px;
}

.toast-desc {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  line-height: 1.4;
}

.toast-message {
  font-weight: 600;
  color: #1e293b;
}

.toast-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
  margin-top: 2px;
}

.toast-success .toast-icon {
  background: #dcfce7;
  color: #16a34a;
}

.toast-error .toast-icon {
  background: #ffe4e6;
  color: #e11d48;
}

.toast-warning .toast-icon {
  background: #fef3c7;
  color: #d97706;
}

.toast-slide-enter-active, .toast-slide-leave-active {
  transition: all 0.25s ease;
}
.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
</style>
