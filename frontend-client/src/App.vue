<script setup>
import DauTrang from '@/components/BocCuc/DauTrang.vue'
import ChanTrang from '@/components/BocCuc/ChanTrang.vue'
import HopChatTuVan from '@/components/BocCuc/HopChatTuVan.vue'
</script>

<template>
  <div class="client-app-shell">
    <DauTrang />
    <main class="client-main-content">
      <router-view />
    </main>
    <ChanTrang />
    <HopChatTuVan />
  </div>
</template>

<style>
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
</style>

