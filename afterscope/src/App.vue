<template>
  <MainLayout />
</template>

<script setup>
import MainLayout from './layouts/MainLayout.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  background: #f0f2f5;
  color: #333;
}
.page-placeholder { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 22px; font-weight: 600; }
.page-header p { color: #888; font-size: 14px; margin-top: 4px; }
</style>
