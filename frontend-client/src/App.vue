<script setup>
import ClientHeader from '@/components/ClientHeader.vue'
import ClientFooter from '@/components/ClientFooter.vue'
import ChatWidget from '@/components/ChatWidget.vue'
</script>

<template>
  <div class="client-app-shell">
    <ClientHeader />
    <main class="client-main-content">
      <router-view />
    </main>
    <ClientFooter />
    <ChatWidget />
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

