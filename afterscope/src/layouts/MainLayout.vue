<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="logo">
        <span class="logo-icon">🔧</span>
        <span class="logo-title">售后智能中台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="nav-menu"
        background-color="#001529"
        text-color="rgba(255,255,255,.65)"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>首页数据仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <span>售后工单管理</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Notebook /></el-icon>
          <span>AI知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/dlq">
          <el-icon><WarningFilled /></el-icon>
          <span>MQ死信消息运维</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><TrendCharts /></el-icon>
          <span>数据统计报表</span>
        </el-menu-item>
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>系统参数配置</span>
        </el-menu-item>
        <el-divider style="border-color:rgba(255,255,255,.06);margin:8px 16px;" />
        <el-menu-item index="/user-apply">
          <el-icon><Goods /></el-icon>
          <span>用户端售后申请</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- Main -->
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  DataAnalysis, Document, Notebook, WarningFilled,
  TrendCharts, Setting, Goods
} from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #001529 0%, #002140 100%);
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  overflow-y: auto;
}
.logo {
  padding: 20px 18px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,.08);
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  background: #1890ff;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.nav-menu {
  border-right: none !important;
  padding: 12px 0;
}
.nav-menu .el-menu-item {
  font-size: 14px;
  align-items: center;
}
.nav-menu .el-menu-item.is-active {
  background: #1890ff !important;
}
.main {
  margin-left: 240px;
  flex: 1;
  min-width: 0;
  overflow-x: hidden;
  padding: 24px 32px;
  min-height: 100vh;
}
@media (max-width: 900px) {
  .sidebar { width: 64px; }
  .logo-title { display: none; }
  .main { margin-left: 64px; padding: 16px; }
}
</style>
