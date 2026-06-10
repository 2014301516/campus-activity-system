<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
<template>
  <div class="page-card">
    <h2>📋 活动详情 #{{ route.params.id }}</h2>
  </div>
</template>
