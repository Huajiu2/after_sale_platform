<template>
  <div class="dlq-page">
    <div class="page-header">
      <h1>MQ 死信消息运维</h1>
      <p>处理 RabbitMQ 异常消息 — AI超时、解析失败、参数异常</p>
    </div>

    <!-- 筛选条件 -->
    <el-card shadow="never" class="filter-card">
      <div class="card-title">🔍 筛选条件</div>
      <div class="filter-row">
        <div class="filter-item">
          <label>异常时间：</label>
          <el-date-picker
            v-model="query.startTime"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
          />
          <span class="separator">~</span>
          <el-date-picker
            v-model="query.endTime"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
          />
        </div>
        <div class="filter-item">
          <label>工单号：</label>
          <el-input v-model="query.ticketNo" placeholder="请输入售后单号" clearable style="width: 160px" />
        </div>
        <div class="filter-item">
          <label>异常原因：</label>
          <el-input v-model="query.errorReason" placeholder="模糊搜索" clearable style="width: 200px" />
        </div>
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch">🔍 查询</el-button>
        <el-button @click="handleReset">🔄 重置</el-button>
        <el-button type="success" :disabled="selectedIds.length === 0" @click="handleBatchRetry">
          🔄 批量重试
        </el-button>
        <span v-if="selectedIds.length > 0" class="selected-hint">
          已选择 {{ selectedIds.length }} 条
        </span>
      </div>
    </el-card>

    <!-- 死信列表 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <span class="card-title">⚠️ 死信消息列表</span>
        <el-tag type="danger" effect="plain" size="small">共 {{ total }} 条待处理</el-tag>
      </div>

      <el-table
        :data="list"
        v-loading="loading"
        stripe
        style="width:100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column prop="msgId" label="消息ID" width="120" />
        <el-table-column prop="queueName" label="所属队列" width="160" />
        <el-table-column prop="ticketNo" label="关联工单" width="160" />
        <el-table-column prop="errorReason" label="异常原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="errorTime" label="入死信时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :loading="retryingId === row.id"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
            <el-button
              type="danger"
              size="small"
              :loading="deletingId === row.id"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          small
          @change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchDlqList,
  retryDlq,
  batchRetryDlq,
  deleteDlq
} from '@/api/dlq'

// ---------- 列表 ----------
const list = ref([])
const total = ref(0)
const loading = ref(false)
const selectedIds = ref([])

const query = reactive({
  page: 1,
  size: 20,
  startTime: '',
  endTime: '',
  ticketNo: '',
  errorReason: ''
})

async function fetchList() {
  loading.value = true
  try {
    const params = { ...query }
    // 移除空字符串参数
    Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })
    const res = await fetchDlqList(params)
    list.value = res.data?.records || res.data || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取死信列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  query.startTime = ''
  query.endTime = ''
  query.ticketNo = ''
  query.errorReason = ''
  query.page = 1
  fetchList()
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

// ---------- 单条重试 ----------
const retryingId = ref(null)

async function handleRetry(row) {
  retryingId.value = row.id
  try {
    await retryDlq(row.id)
    ElMessage.success(`消息 ${row.msgId} 已重新投递到原队列`)
    fetchList()
  } catch (e) {
    ElMessage.error('重试失败')
  } finally {
    retryingId.value = null
  }
}

// ---------- 批量重试 ----------
async function handleBatchRetry() {
  try {
    await ElMessageBox.confirm(
      `确认批量重试 ${selectedIds.value.length} 条死信消息？`,
      '批量重试',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await batchRetryDlq({ ids: selectedIds.value })
    ElMessage.success(`批量重试完成，成功 ${res.data?.successCount || 0} 条`)
    selectedIds.value = []
    fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('批量重试失败')
  }
}

// ---------- 删除 ----------
const deletingId = ref(null)

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除死信消息「${row.msgId}」？`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'danger' }
    )
    deletingId.value = row.id
    await deleteDlq(row.id)
    ElMessage.success('死信消息已删除')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  } finally {
    deletingId.value = null
  }
}

// ---------- 初始化 ----------
onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}
.page-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
}
.page-header p {
  color: #888;
  font-size: 14px;
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 14px;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
  margin-bottom: 12px;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}
.filter-item label {
  white-space: nowrap;
}
.separator {
  color: #999;
  margin: 0 2px;
}
.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.selected-hint {
  font-size: 12px;
  color: #1890ff;
  margin-left: 4px;
}

.table-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
