<template>
  <div class="dashboard" v-loading="loading">
    <div class="page-header">
      <h1>首页数据仪表盘</h1>
      <p class="page-desc">实时售后运营数据一览 — Redis 缓存聚合，秒级响应</p>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div v-for="stat in stats" :key="stat.label" class="stat-card">
        <div class="icon-box" :style="{ background: stat.bg, color: stat.color }">{{ stat.icon }}</div>
        <div class="info">
          <h3>{{ stat.displayValue }}</h3>
          <p>{{ stat.label }}</p>
        </div>
        <span class="trend" :class="stat.trendDir" v-if="stat.trend !== undefined">{{ stat.trend }}</span>
      </div>
    </div>

    <!-- Charts -->
    <div class="charts-row">
      <div class="chart-box">
        <div class="chart-title">📉 近7日售后申请趋势</div>
        <Line v-if="trendData.labels.length" :data="trendData" :options="chartOptions" />
        <p v-else class="empty-chart">暂无趋势数据</p>
      </div>
      <div class="chart-box">
        <div class="chart-title">🥧 售后类型占比</div>
        <div style="max-height:220px;display:flex;justify-content:center;">
          <Doughnut v-if="pieData.labels.length" :data="pieData" :options="pieOptions" />
          <p v-else class="empty-chart">暂无占比数据</p>
        </div>
      </div>
    </div>

    <!-- Store Ranking -->
    <div class="card">
      <div class="card-title">
        🏆 本月店铺售后 TOP10
        <span class="tag">Redis ZSet 实时排行</span>
      </div>
      <ul class="rank-list">
        <li v-for="(shop, i) in shopRanking" :key="shop.storeId || i">
          <span class="rank-num" :class="'rank-' + (i < 3 ? i + 1 : 'other')">{{ i + 1 }}</span>
          {{ shop.storeName }}
          <span class="rank-count">{{ shop.orderCount }} 单</span>
        </li>
      </ul>
    </div>

    <!-- Quick Actions -->
    <div class="card">
      <div class="card-title">⚡ 快捷操作</div>
      <div class="quick-actions">
        <div class="quick-action-card" @click="$router.push('/orders')">
          <div class="qa-icon" style="background:#e6f7ff;color:#1890ff;">🧪</div>
          <div class="qa-text"><h4>测试生成售后工单</h4><p>快速生成测试数据</p></div>
        </div>
        <div class="quick-action-card" @click="$router.push('/knowledge')">
          <div class="qa-icon" style="background:#f6ffed;color:#52c41a;">📄</div>
          <div class="qa-text"><h4>上传知识库文档</h4><p>异步解析向量化入库</p></div>
        </div>
        <div class="quick-action-card" @click="$router.push('/dlq')">
          <div class="qa-icon" style="background:#fff7e6;color:#fa8c16;">🔄</div>
          <div class="qa-text"><h4>死信批量重试</h4><p>一键恢复异常工单</p></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Line, Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement,
  ArcElement, Tooltip, Legend, Filler
} from 'chart.js'
import { fetchStats, fetchTrend, fetchTypeRatio, fetchStoreRanking } from '../api/dashboard'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, ArcElement, Tooltip, Legend, Filler)

const loading = ref(true)

// ---------- Stats Cards ----------
const statDefs = [
  { key: 'todayNewOrders', icon: '📦', label: '今日新增售后单', bg: '#e6f7ff', color: '#1890ff' },
  { key: 'todayAiCompleted', icon: '🤖', label: 'AI 自动办结单数', bg: '#f6ffed', color: '#52c41a' },
  { key: 'pendingManual', icon: '⏳', label: '人工待处理工单', bg: '#fff7e6', color: '#fa8c16' },
  { key: 'aiPassRate', icon: '🎯', label: 'AI 审核通过率', bg: '#f9f0ff', color: '#722ed1' },
  { key: 'todayDlqCount', icon: '🚫', label: 'MQ 今日异常死信', bg: '#fff2f0', color: '#ff4d4f' },
]
const stats = ref([])

const trendKeys = ['todayNewOrdersTrend', 'todayAiCompletedTrend', 'pendingManualTrend', 'aiPassRateTrend', 'todayDlqCountTrend']

function buildStats(data) {
  return statDefs.map((def, i) => {
    const raw = data[def.key]
    const trendRaw = data[trendKeys[i]]
    // aiPassRate 显示为百分比格式
    const displayValue = def.key === 'aiPassRate' ? raw.toFixed(1) + '%' : raw.toLocaleString()
    let trend = undefined
    let trendDir = ''
    if (trendRaw !== undefined && trendRaw !== null) {
      trendDir = trendRaw >= 0 ? 'up' : 'down'
      trend = (trendRaw >= 0 ? '↑ ' : '↓ ') + Math.abs(trendRaw).toFixed(1) + '%'
    }
    return { ...def, displayValue, trend, trendDir }
  })
}

// ---------- Trend Chart ----------
const chartOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: { legend: { position: 'top' } },
  scales: { y: { beginAtZero: true } }
}
const trendData = ref({ labels: [], datasets: [] })

function buildTrendData(data) {
  // 将日期格式从 "2026-05-30" 转为 "5/30"
  const labels = (data.dates || []).map(d => {
    const parts = d.split('-')
    return parseInt(parts[1]) + '/' + parseInt(parts[2])
  })
  return {
    labels,
    datasets: [
      {
        label: '售后申请数',
        data: data.totalOrders || [],
        borderColor: '#1890ff',
        backgroundColor: 'rgba(24,144,255,.08)',
        fill: true,
        tension: 0.3,
        pointRadius: 4,
      },
      {
        label: 'AI办结数',
        data: data.aiCompleted || [],
        borderColor: '#52c41a',
        backgroundColor: 'rgba(82,196,26,.08)',
        fill: true,
        tension: 0.3,
        pointRadius: 4,
      },
    ]
  }
}

// ---------- Pie Chart ----------
const pieOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: {
    legend: { position: 'right' },
    tooltip: {
      callbacks: {
        label: (ctx) => ctx.parsed + '%'
      }
    }
  }
}
const pieData = ref({ labels: [], datasets: [] })

function buildPieData(data) {
  return {
    labels: ['仅退款', '退货退款', '投诉'],
    datasets: [{
      data: [
        data.refundOnly?.ratio || 0,
        data.refundReturn?.ratio || 0,
        data.complaint?.ratio || 0,
      ],
      backgroundColor: ['#1890ff', '#fa8c16', '#ff4d4f'],
      borderWidth: 0,
    }]
  }
}

// ---------- Store Ranking ----------
const shopRanking = ref([])

// ---------- Lifecycle ----------
onMounted(async () => {
  const tryFetch = async (fetcher, handler) => {
    try {
      const res = await fetcher()
      if (res.code === 200) handler(res.data)
    } catch (e) {
      console.error('仪表盘接口异常', e)
    }
  }

  await Promise.all([
    tryFetch(fetchStats, (data) => { stats.value = buildStats(data) }),
    tryFetch(fetchTrend, (data) => { trendData.value = buildTrendData(data) }),
    tryFetch(fetchTypeRatio, (data) => { pieData.value = buildPieData(data) }),
    tryFetch(fetchStoreRanking, (data) => { shopRanking.value = data.list || [] }),
  ])

  loading.value = false
})
</script>

<style scoped>
.dashboard { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 22px; font-weight: 600; margin: 0; }
.page-desc { color: #888; font-size: 14px; margin-top: 4px; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(185px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
}
.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.info h3 { font-size: 24px; font-weight: 700; margin: 0; }
.info p { font-size: 13px; color: #888; margin: 2px 0 0; }
.trend {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  position: absolute;
  right: 20px;
  top: 20px;
}
.trend.up { background: #f6ffed; color: #52c41a; }
.trend.down { background: #fff2f0; color: #ff4d4f; }

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.chart-box {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.chart-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.empty-chart { color: #ccc; text-align: center; padding: 60px 0; font-size: 14px; }

.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  margin-bottom: 20px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #1a1a2e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag {
  font-size: 12px;
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 400;
}

.rank-list { list-style: none; padding: 0; margin: 0; }
.rank-list li {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
}
.rank-list li:last-child { border: none; }
.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  margin-right: 12px;
  flex-shrink: 0;
}
.rank-1 { background: #fff7e6; color: #fa8c16; }
.rank-2 { background: #f5f5f5; color: #999; }
.rank-3 { background: #fff2f0; color: #ff4d4f; }
.rank-other { background: #fafafa; color: #bbb; }
.rank-count { margin-left: auto; color: #fa8c16; font-weight: 600; }

.quick-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.quick-action-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px dashed #d9d9d9;
  cursor: pointer;
  transition: all .2s;
  min-width: 200px;
}
.quick-action-card:hover { border-color: #1890ff; background: #e6f7ff; }
.qa-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.qa-text h4 { font-size: 14px; margin: 0; }
.qa-text p { font-size: 12px; color: #999; margin: 2px 0 0; }
</style>
