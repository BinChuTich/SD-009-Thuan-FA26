<script setup>
import { dialogState, closeAppDialog, toastsList } from '@/utils/thongBaoDialog.js'

const handleConfirm = () => {
  closeAppDialog(true)
}

const handleCancel = () => {
  closeAppDialog(false)
}
</script>

<template>
  <div>
    <!-- MODAL HỘP THOẠI XÁC NHẬN / THÔNG BÁO IN-APP -->
    <transition name="dialog-fade">
      <div v-if="dialogState.isOpen" class="dialog-backdrop" @click.self="handleCancel">
        <div class="dialog-card">
          <!-- Icon biểu thị theo loại thông báo -->
          <div class="dialog-icon-wrapper" :class="`icon-${dialogState.type}`">
            <template v-if="dialogState.type === 'confirm' || dialogState.type === 'warning'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </template>
            <template v-else-if="dialogState.type === 'error'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
            </template>
            <template v-else-if="dialogState.type === 'success'">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="9 12 11.5 14.5 16 9.5"></polyline>
              </svg>
            </template>
            <template v-else>
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
            </template>
          </div>

          <!-- Tiêu đề & nội dung -->
          <div class="dialog-content">
            <h3 class="dialog-title">{{ dialogState.title }}</h3>
            <p class="dialog-msg">{{ dialogState.message }}</p>
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
              :class="`btn-${dialogState.type}`"
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
            {{ t.type === 'error' ? '✕' : (t.type === 'warning' ? '⚠️' : '✓') }}
          </span>
          <span class="toast-message">{{ t.message }}</span>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  padding: 16px;
}

.dialog-card {
  background: #ffffff;
  border-radius: 18px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 20px 40px -8px rgba(15, 23, 42, 0.25), 0 0 0 1px rgba(226, 232, 240, 0.8);
  padding: 26px 24px;
  text-align: center;
  animation: popIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.dialog-icon-wrapper {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-confirm, .icon-warning {
  background: #fffbeb;
  color: #d97706;
}

.icon-error {
  background: #fff1f2;
  color: #e11d48;
}

.icon-success {
  background: #f0fdf4;
  color: #16a34a;
}

.icon-info {
  background: #f0f7ff;
  color: #2563eb;
}

.dialog-title {
  font-size: 19px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 10px;
  letter-spacing: -0.2px;
}

.dialog-msg {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 24px;
  white-space: pre-line;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-dialog-cancel {
  flex: 1;
  padding: 11px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-dialog-cancel:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.btn-dialog-confirm {
  flex: 1;
  padding: 11px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #ffffff;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  background: #496883;
}

.btn-dialog-confirm:hover {
  opacity: 0.92;
  transform: translateY(-1px);
}

.btn-confirm {
  background: #d2a764;
  color: #2d3748;
}

.btn-error {
  background: #e11d48;
}

.btn-success {
  background: #16a34a;
}

/* Transitions */
.dialog-fade-enter-active, .dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}
.dialog-fade-enter-from, .dialog-fade-leave-to {
  opacity: 0;
}

/* TOASTS */
.toasts-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 100000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 25px -4px rgba(15, 23, 42, 0.15), 0 0 0 1px rgba(226, 232, 240, 0.8);
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  min-width: 280px;
  max-width: 400px;
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
