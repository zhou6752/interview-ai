<template>
  <div class="app-shell">
    <AppNav v-if="route.path !== '/login'" />
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import AppNav from './components/AppNav.vue'

const route = useRoute()
</script>