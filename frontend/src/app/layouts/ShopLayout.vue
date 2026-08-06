<script setup lang="ts">
import { RouterView } from 'vue-router'

import AppFooter from '@/shared/components/AppFooter.vue'
import AppHeader from '@/shared/components/AppHeader.vue'
</script>

<template>
  <div class="shop-layout">
    <AppHeader />
    <main id="main-content" class="shop-layout__main" tabindex="-1">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.shop-layout {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.shop-layout__main {
  flex: 1;
}
</style>
