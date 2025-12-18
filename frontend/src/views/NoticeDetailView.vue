<script setup lang="ts">
import { useRoute } from 'vue-router';

const route = useRoute();
const id = route.params.id;
</script>

<template>
  <div>
    <h1>公告详情</h1>
    <p>公告 ID: {{ id }}</p>
    <p>这里展示公告详情（占位）。</p>
  </div>
</template>
