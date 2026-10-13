<template>
  <div>
    <!-- TOAST NOTIFICATIONS -->
    <div class="global-toast-container">
      <transition-group name="toast-anim" tag="div" class="toast-list">
        <div
          v-for="toast in dialogState.toasts"
          :key="toast.id"
          class="toast-item"
          :class="`toast-${toast.type || 'info'}`"
        >
          <div class="toast-icon">
            <svg v-if="toast.type === 'success'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <svg v-else-if="toast.type === 'error'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="15" y1="9" x2="9" y2="15"></line>
              <line x1="9" y1="9" x2="15" y2="15"></line>
            </svg>
            <svg v-else-if="toast.type === 'warning'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
          </div>
          <div class="toast-content">{{ toast.message }}</div>
          <button class="toast-close-btn" @click="removeToast(toast.id)" title="Đóng">✕</button>
        </div>
      </transition-group>
    </div>

    <!-- MODAL CONFIRM (Xác nhận hành động) -->
    <transition name="fade">
      <div
        v-if="dialogState.confirm.show"
        class="dialog-overlay"
        @click.self="handleConfirmChoice(false)"
      >
        <div class="dialog-box confirm-box">
          <div class="dialog-header">
            <div class="dialog-icon-wrapper" :class="`icon-${dialogState.confirm.type || 'question'}`">
              <svg v-if="dialogState.confirm.type === 'warning' || dialogState.confirm.type === 'danger'" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
              <svg v-else-if="dialogState.confirm.type === 'success'" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </div>
            <h3 class="dialog-title">{{ dialogState.confirm.title || 'Xác nhận thao tác' }}</h3>
          </div>
          <div class="dialog-body">
            <p class="dialog-message">{{ dialogState.confirm.message }}</p>
          </div>
          <div class="dialog-footer">
            <button
              type="button"
              class="btn-dialog btn-cancel"
              @click="handleConfirmChoice(false)"
            >
              {{ dialogState.confirm.cancelText || 'Hủy bỏ' }}
            </button>
            <button
              type="button"
              class="btn-dialog btn-confirm"
              :class="`btn-${dialogState.confirm.type || 'primary'}`"
              @click="handleConfirmChoice(true)"
            >
              {{ dialogState.confirm.confirmText || 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL ALERT (Cảnh báo / Lỗi / Thông báo) -->
    <transition name="fade">
      <div
        v-if="dialogState.alert.show"
        class="dialog-overlay"
        @click.self="handleAlertClose"
      >
        <div class="dialog-box alert-box">
          <div class="dialog-header">
            <div class="dialog-icon-wrapper" :class="`icon-${dialogState.alert.type || 'warning'}`">
              <svg v-if="dialogState.alert.type === 'error'" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
              <svg v-else-if="dialogState.alert.type === 'success'" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </div>
            <h3 class="dialog-title">{{ dialogState.alert.title || 'Thông báo' }}</h3>
          </div>
          <div class="dialog-body">
            <p class="dialog-message">{{ dialogState.alert.message }}</p>
          </div>
          <div class="dialog-footer">
            <button
              type="button"
              class="btn-dialog btn-confirm btn-primary"
              @click="handleAlertClose"
            >
              {{ dialogState.alert.btnText || 'Đã hiểu' }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { dialogState, handleConfirmChoice, handleAlertClose, removeToast } from '@/utils/dialog.js'
</script>

<style scoped>
/* ==================== TOAST STYLES ==================== */
.global-toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 99999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
  max-width: 420px;
}

.toast-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-radius: 10px;
  background-color: #ffffff;
  color: #333333;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid #ece7df;
  font-size: 14px;
  line-height: 1.45;
}

.toast-success {
  border-left: 5px solid #4c8a5a;
  color: #3b7047;
}
.toast-success .toast-icon {
  color: #4c8a5a;
}

.toast-error {
  border-left: 5px solid #c0392b;
  color: #a5281b;
}
.toast-error .toast-icon {
  color: #c0392b;
}

.toast-warning {
  border-left: 5px solid #b38e6e;
  color: #8c6328;
}
.toast-warning .toast-icon {
  color: #b38e6e;
}

.toast-info {
  border-left: 5px solid #496883;
  color: #376384;
}
.toast-info .toast-icon {
  color: #496883;
}

.toast-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.toast-content {
  flex: 1;
  font-weight: 500;
  color: #2e383d;
}

.toast-close-btn {
  background: transparent;
  border: none;
  font-size: 15px;
  color: #8c979d;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.15s;
}

.toast-close-btn:hover {
  color: #2e383d;
  background-color: #f1ede6;
}

/* Toast Animations */
.toast-anim-enter-active,
.toast-anim-leave-active {
  transition: all 0.25s ease;
}
.toast-anim-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.toast-anim-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

/* ==================== DIALOG MODAL STYLES ==================== */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(18, 25, 33, 0.55);
  backdrop-filter: blur(3px);
  z-index: 99990;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.dialog-box {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 460px;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.2);
  border: 1px solid #ebe5da;
  overflow: hidden;
  animation: dialogScaleUp 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  flex-direction: column;
}

@keyframes dialogScaleUp {
  from {
    opacity: 0;
    transform: scale(0.93) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.dialog-header {
  padding: 1.6rem 1.5rem 0.6rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.dialog-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.85rem;
}

.icon-question {
  background-color: #edf5fa;
  color: #496883;
}

.icon-warning {
  background-color: #fff9f0;
  color: #b38e6e;
}

.icon-danger {
  background-color: #fdeded;
  color: #c0392b;
}

.icon-success {
  background-color: #edf5ef;
  color: #4c8a5a;
}

.icon-error {
  background-color: #fdeded;
  color: #c0392b;
}

.dialog-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: #242e33;
}

.dialog-body {
  padding: 0.5rem 1.75rem 1.4rem;
  text-align: center;
}

.dialog-message {
  margin: 0;
  font-size: 0.935rem;
  line-height: 1.55;
  color: #55626a;
}

.dialog-footer {
  padding: 1rem 1.5rem 1.4rem;
  background-color: #faf8f5;
  border-top: 1px solid #f0eae1;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.btn-dialog {
  min-width: 110px;
  padding: 0.6rem 1.25rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.btn-cancel {
  background: #ffffff;
  color: #55626a;
  border-color: #d8d2c6;
}
.btn-cancel:hover {
  background: #f1ece4;
  color: #242e33;
}

.btn-confirm.btn-primary,
.btn-confirm.btn-question {
  background: #496883;
  color: #ffffff;
}
.btn-confirm.btn-primary:hover,
.btn-confirm.btn-question:hover {
  background: #395267;
}

.btn-confirm.btn-warning {
  background: #d97706;
  color: #ffffff;
}
.btn-confirm.btn-warning:hover {
  background: #b45309;
}

.btn-confirm.btn-danger {
  background: #dc2626;
  color: #ffffff;
}
.btn-confirm.btn-danger:hover {
  background: #b91c1c;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

