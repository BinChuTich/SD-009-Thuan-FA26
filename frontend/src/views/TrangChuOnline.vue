
<script setup>
import ChatCustomerWidget from '@/components/ChatCustomerWidget.vue';
const customerId = 2;
</script>

<template>
  <div>
    <p style="text-align: center; font-size: 50px;">Hello</p>
  </div>

  <ChatCustomerWidget :customer-id="customerId" />
</template>
