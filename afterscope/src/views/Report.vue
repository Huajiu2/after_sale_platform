<template>
  <div class="report-page" v-loading="loading">
    <div class="page-header">
      <h1>数据统计报表</h1>
      <p>售后数据多维分析 — AI处理率、驳回趋势、店铺排行</p>
    </div>

    <div class="stats-row">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="icon-box" :style="{ background: s.bg, color: s.color }">{{ s.icon }}</div>
        <div class="info">
          <h3>{{ s.value }}</h3>
          <p>{{ s.label }}</p>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-box">
        <div class="chart-title">📊 每日售后单数 & AI处理率</div>
        <Bar v-if="dailyData.labels.length" :data="dailyData" :options="barChartOptions" />
        <p v-else class="empty-chart">暂无数据</p>
      </div>
      <div class="chart-box">
        <div class="chart-title">📈 驳回率 vs 通过率趋势</div>
        <Line v-if="rateData.labels.length" :data="rateData" :options="lineChartOptions" />
        <p v-else class="empty-chart">暂无数据</p>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">🏪 本月店铺售后排行</span>
          <el-button type="success" size="small" @click="handleExport">📥 导出报表</el-button>
        </div>
      </template>
      <el-table :data="storeRanking" stripe style="width:100%">
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <span class="rank-num" :class="`rank-${Math.min($index + 1, 4)}`">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="店铺名称" min-width="180" />
        <el-table-column prop="orderCount" label="售后订单数" width="120" align="center" />
        <el-table-column label="AI处理率" width="120" align="center">
          <template #default="{ row }">{{ row.aiProcessRate }}%</template>
        </el-table-column>
        <el-table-column label="驳回率" width="120" align="center">
          <template #default="{ row }">{{ row.rejectedRate }}%</template>
        </el-table-column>
        <el-table-column label="趋势" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.trend > 0" class="trend up">↑ {{ row.trend }}%</span>
            <span v-else-if="row.trend < 0" class="trend down">↓ {{ Math.abs(row.trend) }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, Title, Tooltip, Legend, Filler
} from 'chart.js'
import dayjs from 'dayjs'
import {
  fetchMonthlySummary,
  fetchDailyDetail,
  fetchStoreRanking,
  fetchRateTrend,
  exportReport
} from '@/api/report'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend, Filler)

const loading = ref(false)
const stats = ref([])
const storeRanking = ref([])
const currentMonth = dayjs().format('YYYYMM')

const dailyData = reactive({ labels: [], datasets: [] })
const barChartOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: { legend: { position: 'top' } },
  scales: { y: { beginAtZero: true } }
}

const rateData = reactive({ labels: [], datasets: [] })
const lineChartOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: { legend: { position: 'top' } },
  scales: { y: { beginAtZero: true, max: 100 } }
}

async function loadData() {
  loading.value = true
  try {
    const [summaryRes, dailyRes, trendRes, rankingRes] = await Promise.all([
      fetchMonthlySummary(currentMonth),
      fetchDailyDetail(currentMonth),
      fetchRateTrend(6),
      fetchStoreRanking(currentMonth, { page: 1, size: 20 })
    ])

    const s = summaryRes.data || {}
    stats.value = [
      { label: '本月售后单总数', value: s.totalOrders ?? '-', icon: '📅', bg: '#e6f7ff', color: '#1890ff' },
      { label: 'AI自动处理率', value: s.aiProcessRate != null ? s.aiProcessRate + '%' : '-', icon: '🤖', bg: '#f6ffed', color: '#52c41a' },
      { label: '人工介入率', value: s.manualInterventionRate != null ? s.manualInterventionRate + '%' : '-', icon: '👤', bg: '#fff7e6', color: '#fa8c16' },
      { label: '售后驳回率', value: s.rejectedRate != null ? s.rejectedRate + '%' : '-', icon: '📉', bg: '#fff2f0', color: '#ff4d4f' }
    ]

    const daily = dailyRes.data?.records || []
    dailyData.labels = daily.map(d => d.date)
    dailyData.datasets = [
      {
        label: '售后单数', data: daily.map(d => d.totalOrders),
        backgroundColor: '#1890ff', borderRadius: 4
      },
      {
        label: 'AI处理率(%)', data: daily.map(d => d.aiProcessRate),
        borderColor: '#52c41a', backgroundColor: 'rgba(82,196,26,.2)',
        type: 'line', tension: 0.3, pointRadius: 4
      }
    ]

    const trend = trendRes.data || {}
    rateData.labels = trend.labels || []
    rateData.datasets = [
      {
        label: '通过率(%)', data: trend.approveRate || [],
        borderColor: '#52c41a', tension: 0.3, pointRadius: 4
      },
      {
        label: '驳回率(%)', data: trend.rejectRate || [],
        borderColor: '#ff4d4f', tension: 0.3, pointRadius: 4
      }
    ]

    storeRanking.value = rankingRes.data?.records || []
  } catch (e) {
    console.error('报表数据加载失败', e)
  } finally {
    loading.value = false
  }
}

function handleExport() {
  try {
    exportReport({ type: 'monthly', month: currentMonth })
    ElMessage.success('导出请求已提交')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h1 { font-size: 22px; font-weight: 600; color: #1a1a2e; }
.page-header p { color: #888; font-size: 14px; margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(185px, 1fr)); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 1px 4px rgba(0,0,0,.06); display: flex; align-items: center; gap: 16px; }
.stat-card .icon-box { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
.stat-card .info h3 { font-size: 24px; font-weight: 700; color: #1a1a2e; }
.stat-card .info p { font-size: 13px; color: #888; margin-top: 2px; }

.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.chart-box { background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.chart-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #1a1a2e; }
.empty-chart { text-align: center; color: #999; padding: 40px 0; }

.table-card { border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-title { font-size: 15px; font-weight: 600; color: #1a1a2e; }

.rank-num { width: 24px; height: 24px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.rank-1 { background: #fff7e6; color: #fa8c16; }
.rank-2 { background: #f5f5f5; color: #999; }
.rank-3 { background: #fff2f0; color: #ff4d4f; }
.rank-other { background: #fafafa; color: #bbb; }

.trend { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.trend.up { background: #f6ffed; color: #52c41a; }
.trend.down { background: #fff2f0; color: #ff4d4f; }

@media (max-width: 900px) { .charts-row { grid-template-columns: 1fr; } .stats-row { grid-template-columns: repeat(2,1fr); } }
</style>
