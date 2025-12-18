<script setup lang="ts">
import { storeToRefs } from 'pinia';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const { profile } = storeToRefs(authStore);
</script>

<template>
  <div>
    <h1>个人中心</h1>
    <pre>{{ profile }}</pre>
  </div>
</template>
