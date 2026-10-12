<template>
  <Teleport to="body">
    <!-- CONFIRM DIALOG -->
    <transition name="dialog-fade">
      <div
        v-if="dialogState.confirm.show"
        class="global-dialog-overlay"
        @click.self="handleConfirmChoice(false)"
      >
        <div class="global-dialog-box" role="dialog" aria-modal="true">
          <div class="dialog-icon-circle" :class="dialogState.confirm.type || 'question'">
            <span v-if="dialogState.confirm.type === 'danger'">✕</span>
            <span v-else-if="dialogState.confirm.type === 'warning'">!</span>
            <span v-else-if="dialogState.confirm.type === 'success'">✓</span>
            <span v-else>?</span>
          </div>

          <h3 class="dialog-title">{{ dialogState.confirm.title || 'Xác nhận thao tác' }}</h3>
          <p class="dialog-message">{{ dialogState.confirm.message }}</p>

          <div class="dialog-actions-row">
            <button
              type="button"
              class="btn-dialog-cancel"
              @click="handleConfirmChoice(false)"
            >
              {{ dialogState.confirm.cancelText || 'Hủy bỏ' }}
            </button>
            <button
              type="button"
              class="btn-dialog-confirm"
              :class="dialogState.confirm.type || 'primary'"
              @click="handleConfirmChoice(true)"
            >
              {{ dialogState.confirm.confirmText || 'Xác nhận' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ALERT DIALOG -->
    <transition name="dialog-fade">
      <div
        v-if="dialogState.alert.show"
        class="global-dialog-overlay"
        @click.self="handleAlertClose"
      >
        <div class="global-dialog-box" role="alertdialog" aria-modal="true">
          <div class="dialog-icon-circle" :class="dialogState.alert.type || 'warning'">
            <span v-if="dialogState.alert.type === 'error' || dialogState.alert.type === 'danger'">✕</span>
            <span v-else-if="dialogState.alert.type === 'warning'">!</span>
            <span v-else-if="dialogState.alert.type === 'success'">✓</span>
            <span v-else>i</span>
          </div>

          <h3 class="dialog-title">{{ dialogState.alert.title || 'Thông báo' }}</h3>
          <p class="dialog-message">{{ dialogState.alert.message }}</p>

          <div class="dialog-actions-row single-action">
            <button
              type="button"
              class="btn-dialog-confirm"
              :class="dialogState.alert.type === 'error' ? 'danger' : 'primary'"
              @click="handleAlertClose"
            >
              {{ dialogState.alert.btnText || 'Đã hiểu' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- TOASTS CONTAINER -->
    <div class="global-toasts-container">
      <transition-group name="toast-slide">
        <div
          v-for="toast in dialogState.toasts"
          :key="toast.id"
          class="global-toast-item"
          :class="toast.type || 'info'"
          @click="removeToast(toast.id)"
        >
          <span class="toast-bullet-icon">
            <span v-if="toast.type === 'success'">✓</span>
            <span v-else-if="toast.type === 'error'">✕</span>
            <span v-else-if="toast.type === 'warning'">!</span>
            <span v-else>i</span>
          </span>
          <span class="toast-text-msg">{{ toast.message }}</span>
          <button type="button" class="toast-btn-dismiss" @click.stop="removeToast(toast.id)">&times;</button>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<script setup>
import { dialogState, handleConfirmChoice, handleAlertClose, removeToast } from '@/utils/dialog.js'
</script>

<style scoped>
.global-dialog-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 16px;
  box-sizing: border-box;
}

.global-dialog-box {
  background: #ffffff;
  border-radius: 14px;
  width: 100%;
  max-width: 440px;
  padding: 26px 24px 22px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.22), 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #ebd9c8;
  animation: dialog-pop 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dialog-pop {
  from {
    transform: scale(0.92);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.dialog-icon-circle {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.dialog-icon-circle.question {
  background-color: #e0f2fe;
  color: #0284c7;
  border: 2px solid #bae6fd;
}

.dialog-icon-circle.warning {
  background-color: #fef3c7;
  color: #d97706;
  border: 2px solid #fde68a;
}

.dialog-icon-circle.danger,
.dialog-icon-circle.error {
  background-color: #fee2e2;
  color: #dc2626;
  border: 2px solid #fecaca;
}

.dialog-icon-circle.success {
  background-color: #dcfce7;
  color: #16a34a;
  border: 2px solid #bbf7d0;
}

.dialog-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 10px;
  line-height: 1.35;
}

.dialog-message {
  font-size: 0.92rem;
  color: #64748b;
  margin: 0 0 22px;
  line-height: 1.5;
  white-space: pre-line;
}

.dialog-actions-row {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.dialog-actions-row.single-action {
  justify-content: stretch;
}

.dialog-actions-row.single-action button {
  width: 100%;
}

.btn-dialog-cancel {
  flex: 1;
  height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1.5px solid #cbd5e1;
  background-color: #ffffff;
  color: #475569;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-dialog-cancel:hover {
  background-color: #f1f5f9;
  border-color: #94a3b8;
  color: #1e293b;
}

.btn-dialog-confirm {
  flex: 1;
  height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  border: none;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  color: #ffffff;
}

.btn-dialog-confirm.primary,
.btn-dialog-confirm.question {
  background-color: #3e5c76;
}

.btn-dialog-confirm.primary:hover,
.btn-dialog-confirm.question:hover {
  background-color: #2b4257;
}

.btn-dialog-confirm.warning {
  background-color: #d97706;
}

.btn-dialog-confirm.warning:hover {
  background-color: #b45309;
}

.btn-dialog-confirm.danger,
.btn-dialog-confirm.error {
  background-color: #dc2626;
}

.btn-dialog-confirm.danger:hover,
.btn-dialog-confirm.error:hover {
  background-color: #b91c1c;
}

.btn-dialog-confirm.success {
  background-color: #16a34a;
}

.btn-dialog-confirm.success:hover {
  background-color: #15803d;
}

/* FADE ANIMATION */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

/* TOASTS */
.global-toasts-container {
  position: fixed;
  top: 20px;
  right: 24px;
  z-index: 100000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 420px;
  pointer-events: none;
}

.global-toast-item {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, opacity 0.2s;
}

.global-toast-item.success {
  background-color: #065f46;
  color: #ffffff;
  border: 1px solid #047857;
}

.global-toast-item.error {
  background-color: #991b1b;
  color: #ffffff;
  border: 1px solid #b91c1c;
}

.global-toast-item.warning {
  background-color: #92400e;
  color: #ffffff;
  border: 1px solid #b45309;
}

.global-toast-item.info {
  background-color: #1e3a5f;
  color: #ffffff;
  border: 1px solid #2b4c7e;
}

.toast-bullet-icon {
  font-weight: 800;
  font-size: 16px;
  flex-shrink: 0;
}

.toast-text-msg {
  flex: 1;
  line-height: 1.4;
}

.toast-btn-dismiss {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 18px;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.toast-btn-dismiss:hover {
  color: #ffffff;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>
