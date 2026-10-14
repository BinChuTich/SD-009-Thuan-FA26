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
        class="confirm-modal-overlay"
        @click.self="handleConfirmChoice(false)"
      >
        <div class="confirm-modal-box">
          <div class="confirm-modal-header" :class="{ 'header-danger': dialogState.confirm.type === 'danger' }">
            <div class="confirm-warning-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                <line x1="12" y1="9" x2="12" y2="13"></line>
                <line x1="12" y1="17" x2="12.01" y2="17"></line>
              </svg>
            </div>
            <div class="confirm-title-wrap">
              <h3 class="confirm-title">{{ dialogState.confirm.title || 'Xác nhận thao tác' }}</h3>
              <p class="confirm-subtitle">{{ dialogState.confirm.subtitle || 'Vui lòng xác nhận để tiếp tục thực hiện thao tác' }}</p>
            </div>
            <button type="button" class="modal-close-btn" @click="handleConfirmChoice(false)">✕</button>
          </div>
          <div class="confirm-modal-body">
            <p class="confirm-message-text">{{ dialogState.confirm.message }}</p>
          </div>
          <div class="confirm-modal-footer">
            <button
              type="button"
              class="btn btn-secondary"
              @click="handleConfirmChoice(false)"
            >
              {{ dialogState.confirm.cancelText || 'Hủy bỏ' }}
            </button>
            <button
              type="button"
              class="btn btn-save-confirm"
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
        class="confirm-modal-overlay"
        @click.self="handleAlertClose"
      >
        <div class="confirm-modal-box">
          <div class="confirm-modal-header" :class="{ 'header-danger': dialogState.alert.type === 'error' }">
            <div class="confirm-warning-icon">
              <svg v-if="dialogState.alert.type === 'error'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
              <svg v-else-if="dialogState.alert.type === 'success'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <div class="confirm-title-wrap">
              <h3 class="confirm-title">{{ dialogState.alert.title || 'Thông báo' }}</h3>
              <p class="confirm-subtitle">Thông báo từ hệ thống</p>
            </div>
            <button type="button" class="modal-close-btn" @click="handleAlertClose">✕</button>
          </div>
          <div class="confirm-modal-body">
            <p class="confirm-message-text">{{ dialogState.alert.message }}</p>
          </div>
          <div class="confirm-modal-footer">
            <button
              type="button"
              class="btn btn-save-confirm"
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

/* ==================== CONFIRM MODAL STYLES (CHUẨN THEO ẢNH PHIEUGIAMGIA) ==================== */
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(18, 25, 33, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99990;
  backdrop-filter: blur(2px);
}

.confirm-modal-box {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: popIn 0.2s ease-out;
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.confirm-modal-header {
  padding: 1.25rem 1.4rem;
  background-color: #fff9f0;
  border-bottom: 1px solid #faedd9;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  position: relative;
}

.confirm-modal-header.header-danger {
  background-color: #fff5f5;
  border-bottom: 1px solid #fed7d7;
}

.confirm-warning-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #eaf1f5;
  color: #496883;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-danger .confirm-warning-icon {
  background-color: #fdeded;
  color: #c0392b;
}

.confirm-title-wrap {
  flex: 1;
}

.confirm-title {
  margin: 0;
  font-size: 1.08rem;
  font-weight: 700;
  color: #8c6328;
}

.header-danger .confirm-title {
  color: #c0392b;
}

.confirm-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.83rem;
  color: #aa8651;
}

.modal-close-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  color: #9aa0a0;
  cursor: pointer;
  transition: color 0.15s;
}

.modal-close-btn:hover {
  color: #8c6328;
}

.confirm-modal-body {
  padding: 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.confirm-message-text {
  margin: 0;
  font-size: 0.95rem;
  color: #435158;
  line-height: 1.5;
}

.confirm-modal-footer {
  padding: 1rem 1.4rem;
  border-top: 1px solid #eeebe3;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.75rem;
  background-color: #faf9f6;
}

.btn-secondary {
  background-color: #ffffff;
  border: 1px solid #d8d2c6;
  color: #616e75;
  font-weight: 600;
  padding: 0.55rem 1.15rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #f7f5ef;
  color: #2b383e;
  border-color: #c5bfae;
}

.btn-save-confirm {
  background-color: #b38e6e;
  color: #ffffff;
  font-weight: 700;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 6px rgba(179, 142, 110, 0.25);
}

.btn-save-confirm:hover:not(:disabled) {
  background-color: #9c7857;
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

