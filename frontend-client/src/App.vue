<script setup>
import DauTrang from '@/components/BocCuc/DauTrang.vue'
import ChanTrang from '@/components/BocCuc/ChanTrang.vue'
import HopChatTuVan from '@/components/BocCuc/HopChatTuVan.vue'
import Chat from '@/components/BocCuc/ChatCustomerWidget.vue'
const customerId = 2;
</script>

<template>
  <div class="client-app-shell">
    <DauTrang />
    <main class="client-main-content">
      <router-view />
    </main>
    <ChanTrang />
    <Chat :customer-id="customerId" />
  </div>
</template>

<style>
html {
  scroll-behavior: smooth;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Be Vietnam Pro', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1e293b;
  background-color: #f8fafc;
}

button, input, select, textarea {
  font-family: inherit;
}

a {
  color: inherit;
}

.client-app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.client-main-content {
  flex: 1;
}

/* Container chung toàn hệ thống: MÀN HÌNH RỘNG RA & ĐÁP ỨNG MỌI THIẾT BỊ */
.container-client {
  width: 100%;
  max-width: 1440px;
  margin-left: auto;
  margin-right: auto;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
}

@media (min-width: 1600px) {
  .container-client {
    max-width: 1540px;
    padding-left: 2rem;
    padding-right: 2rem;
  }
}

@media (max-width: 1024px) {
  .container-client {
    padding-left: 1.25rem;
    padding-right: 1.25rem;
  }
}

@media (max-width: 640px) {
  .container-client {
    padding-left: 1rem;
    padding-right: 1rem;
  }
}
</style>

