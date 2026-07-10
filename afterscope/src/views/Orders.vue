<template>
  <div class="orders-page">
    <section class="workbench-head">
      <div class="title-block">
        <div class="eyebrow">AFTER-SALE OPERATIONS</div>
        <h1>售后工单管理</h1>
        <p>多条件筛选、AI 审核链路追溯、人工干预处理和批量运维入口。</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadTickets">刷新</el-button>
        <el-button type="primary" :icon="Download" @click="handleExport">导出 Excel</el-button>
      </div>
    </section>

    <section class="metrics-row">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-icon" :style="{ background: item.bg, color: item.color }">
          <component :is="item.icon" />
        </div>
        <div class="metric-copy">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <el-card class="filter-card surface-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="card-title">筛选条件</span>
            <small>对应接口：GET /api/after-sale/list</small>
          </div>
          <el-tag type="info" effect="plain">共 {{ pager.total }} 条</el-tag>
        </div>
      </template>

      <el-form :model="query" label-width="76px" class="filter-form">
        <el-row :gutter="14">
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="订单号">
              <el-input v-model.trim="query.orderNo" clearable placeholder="请输入订单号" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="手机号">
              <el-input v-model.trim="query.userPhone" clearable placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="店铺">
              <el-select v-model="query.storeId" clearable filterable placeholder="全部店铺">
                <el-option v-for="store in stores" :key="store.id" :label="store.storeName" :value="store.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="售后类型">
              <el-select v-model="query.afterSaleType" clearable placeholder="全部类型">
                <el-option v-for="type in afterSaleTypes" :key="type.code" :label="type.name" :value="type.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="6">
            <el-form-item label="工单状态">
              <el-select v-model="query.ticketStatus" clearable placeholder="全部状态">
                <el-option v-for="status in ticketStatuses" :key="status.code" :label="status.name" :value="status.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="8">
            <el-form-item label="创建时间">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :lg="10">
            <el-form-item label=" " class="action-form-item">
              <div class="filter-actions">
                <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
                <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
                <el-button :icon="UserFilled" :disabled="!selectedRows.length" @click="assignDialogVisible = true">批量指派</el-button>
                <el-button :icon="Refresh" :disabled="!selectedRows.length" @click="handleBatchRetry">批量重试</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="table-card surface-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span class="card-title">工单列表</span>
            <small>已选 {{ selectedRows.length }} 条</small>
          </div>
          <div class="table-tools">
            <el-tag type="primary" effect="plain">第 {{ pager.page }} 页</el-tag>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tickets"
        row-key="ticketNo"
        class="ticket-table"
        empty-text="暂无工单数据"
        table-layout="fixed"
        @selection-change="selectedRows = $event"
      >
        <el-table-column type="selection" width="36" />
        <el-table-column prop="ticketNo" label="工单号" width="128">
          <template #default="{ row }">
            <button class="ticket-link" @click="openDetail(row.ticketNo)">{{ row.ticketNo }}</button>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="104" />
        <el-table-column prop="storeName" label="店铺" min-width="130" show-overflow-tooltip />
        <el-table-column label="售后类型" width="92">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.afterSaleType)" effect="plain" round>
              {{ row.afterSaleTypeDesc || afterSaleTypeText(row.afterSaleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyReason" label="申请原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="凭证" width="62">
          <template #default="{ row }">
            <el-tag v-if="hasEvidence(row)" type="success" effect="plain" round>有</el-tag>
            <el-tag v-else type="info" effect="plain" round>无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI审核" min-width="118">
          <template #default="{ row }">
            <div class="ai-result">
              <span>{{ row.aiAuditResult || '待审核' }}</span>
              <small v-if="row.aiConfidence">{{ row.aiConfidence }}%</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="108">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.ticketStatus)" effect="light" round>
              {{ row.ticketStatusDesc || ticketStatusText(row.ticketStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="128">
          <template #default="{ row }">
            <span class="time-text">{{ formatTableTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="112">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openDetail(row.ticketNo)">详情</el-button>
              <el-button v-if="row.ticketStatus === 2" link type="warning" @click="openAudit(row)">审核</el-button>
              <el-button v-if="row.ticketStatus === 0" link type="success" @click="retrySingle(row)">重试</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="pager.total"
          @size-change="loadTickets"
          @current-change="loadTickets"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" size="min(860px, 92vw)" class="detail-drawer" :with-header="false">
      <div v-if="activeDetail" class="drawer-body">
        <div class="drawer-header">
          <div>
            <span>工单详情</span>
            <h2>{{ activeDetail.ticketNo }}</h2>
            <p>{{ activeDetail.ticketStatusDesc || ticketStatusText(activeDetail.ticketStatus) }}</p>
          </div>
          <el-button circle :icon="Close" @click="detailVisible = false" />
        </div>

        <section class="detail-section">
          <div class="section-title">订单基础信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ activeDetail.orderInfo?.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="店铺">{{ activeDetail.orderInfo?.storeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商品">{{ activeDetail.orderInfo?.productName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="规格">{{ activeDetail.orderInfo?.productSpec || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">¥{{ activeDetail.orderInfo?.payAmount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ activeDetail.orderInfo?.orderTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <div class="section-title">售后申请信息</div>
          <div class="apply-box">
            <div>
              <span>类型</span>
              <strong>{{ activeDetail.afterSaleInfo?.afterSaleTypeDesc || '-' }}</strong>
            </div>
            <div>
              <span>申请金额</span>
              <strong>¥{{ activeDetail.afterSaleInfo?.applyAmount ?? '-' }}</strong>
            </div>
            <div class="full">
              <span>申请原因</span>
              <strong>{{ activeDetail.afterSaleInfo?.applyReason || '-' }}</strong>
            </div>
          </div>
          <div class="evidence-list" v-if="activeDetail.afterSaleInfo?.evidenceImages?.length">
            <div v-for="(img, index) in activeDetail.afterSaleInfo.evidenceImages" :key="img" class="evidence-item">
              凭证 {{ index + 1 }}
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title">RAG 检索依据</div>
          <el-empty v-if="!activeDetail.ragEvidence?.length" description="暂无检索依据" :image-size="80" />
          <div v-for="rule in activeDetail.ragEvidence" :key="rule.chunkId" class="rag-card">
            <div class="rag-meta">
              <strong>#{{ rule.rank }} {{ rule.sourceDoc }}</strong>
              <el-tag type="success" effect="plain">相似度 {{ rule.similarity }}%</el-tag>
            </div>
            <p>{{ rule.ruleContent }}</p>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title">AI 审核详情</div>
          <el-empty v-if="!activeDetail.aiAuditDetail" description="暂无 AI 审核详情" :image-size="80" />
          <div v-else class="audit-box">
            <div class="audit-score">{{ activeDetail.aiAuditDetail.confidence ?? 0 }}%</div>
            <div>
              <h3>{{ activeDetail.aiAuditDetail.conclusion || '-' }}</h3>
              <p>{{ activeDetail.aiAuditDetail.reason || '-' }}</p>
              <div class="audit-meta">
                <span>建议动作：{{ activeDetail.aiAuditDetail.suggestedAction || '-' }}</span>
                <span>模型：{{ activeDetail.aiAuditDetail.modelName || '-' }}</span>
                <span>耗时：{{ activeDetail.aiAuditDetail.latencyMs ?? '-' }}ms</span>
              </div>
            </div>
          </div>
        </section>

        <div class="drawer-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" :disabled="activeDetail.ticketStatus !== 2" @click="openAudit(activeDetail)">人工审核</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="auditDialogVisible" title="提交人工审核" width="520px">
      <el-form :model="auditForm" label-width="88px">
        <el-form-item label="工单号">
          <el-input v-model="auditForm.ticketNo" disabled />
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.manualResult">
            <el-radio :label="1">同意售后</el-radio>
            <el-radio :label="2">驳回售后</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.manualRemark" type="textarea" :rows="4" maxlength="300" show-word-limit placeholder="请输入人工判断依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="批量指派客服" width="420px">
      <el-form label-width="82px">
        <el-form-item label="已选工单">
          <span>{{ selectedRows.length }} 条</span>
        </el-form-item>
        <el-form-item label="客服">
          <el-input v-model.trim="assignee" placeholder="请输入客服姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleBatchAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Close,
  Download,
  Refresh,
  RefreshLeft,
  Search,
  Tickets,
  Timer,
  UserFilled,
  Warning,
  CircleCheck
} from '@element-plus/icons-vue'
import {
  fetchAfterSaleList,
  fetchAfterSaleDetail,
  submitManualAudit,
  batchAssignTickets,
  batchRetryTickets,
  exportAfterSaleTickets,
  fetchStores,
  fetchAfterSaleTypes,
  fetchTicketStatuses
} from '../api/afterSale'

const loading = ref(false)
const submitting = ref(false)
const dateRange = ref([])
const selectedRows = ref([])
const tickets = ref([])
const stores = ref([])
const afterSaleTypes = ref([])
const ticketStatuses = ref([])
const detailVisible = ref(false)
const activeDetail = ref(null)
const auditDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const assignee = ref('')

const query = reactive({
  orderNo: '',
  userPhone: '',
  storeId: '',
  afterSaleType: '',
  ticketStatus: '',
})

const pager = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const auditForm = reactive({
  ticketNo: '',
  manualResult: 1,
  manualRemark: '',
})

const metrics = computed(() => {
  const pendingAi = tickets.value.filter(item => item.ticketStatus === 0).length
  const pendingManual = tickets.value.filter(item => item.ticketStatus === 2).length
  const completed = tickets.value.filter(item => item.ticketStatus === 1).length
  const rejected = tickets.value.filter(item => item.ticketStatus === 3).length
  return [
    { label: '待 AI 审核', value: pendingAi, icon: Timer, bg: '#fff7e6', color: '#d46b08' },
    { label: '待人工审核', value: pendingManual, icon: UserFilled, bg: '#e6f7ff', color: '#1677ff' },
    { label: '已办结', value: completed, icon: CircleCheck, bg: '#f6ffed', color: '#389e0d' },
    { label: '已驳回', value: rejected, icon: Warning, bg: '#fff1f0', color: '#cf1322' },
    { label: '当前页工单', value: tickets.value.length, icon: Tickets, bg: '#f9f0ff', color: '#722ed1' },
  ]
})

function buildParams() {
  return {
    page: pager.page,
    size: pager.size,
    orderNo: query.orderNo || undefined,
    userPhone: query.userPhone || undefined,
    storeId: query.storeId || undefined,
    afterSaleType: query.afterSaleType || undefined,
    ticketStatus: query.ticketStatus || undefined,
    startTime: dateRange.value?.[0],
    endTime: dateRange.value?.[1],
  }
}

async function loadTickets() {
  loading.value = true
  try {
    const res = await fetchAfterSaleList(buildParams())
    if (res.code === 200) {
      tickets.value = res.data?.records || []
      pager.total = res.data?.total || 0
    } else {
      tickets.value = []
      pager.total = 0
    }
  } catch (e) {
    tickets.value = []
    pager.total = 0
    ElMessage.error('工单列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [storeRes, typeRes, statusRes] = await Promise.all([
      fetchStores(),
      fetchAfterSaleTypes(),
      fetchTicketStatuses()
    ])
    stores.value = storeRes.code === 200 ? storeRes.data || [] : []
    afterSaleTypes.value = typeRes.code === 200 ? typeRes.data || [] : []
    ticketStatuses.value = statusRes.code === 200 ? statusRes.data || [] : []
  } catch (e) {
    stores.value = []
    afterSaleTypes.value = []
    ticketStatuses.value = []
  }
}

function handleSearch() {
  pager.page = 1
  loadTickets()
}

function resetQuery() {
  Object.assign(query, { orderNo: '', userPhone: '', storeId: '', afterSaleType: '', ticketStatus: '' })
  dateRange.value = []
  handleSearch()
}

async function openDetail(ticketNo) {
  try {
    const res = await fetchAfterSaleDetail(ticketNo)
    if (res.code !== 200 || !res.data) {
      ElMessage.warning('未查询到工单详情')
      return
    }
    activeDetail.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('工单详情加载失败')
  }
}

function openAudit(row) {
  auditForm.ticketNo = row.ticketNo
  auditForm.manualResult = 1
  auditForm.manualRemark = ''
  auditDialogVisible.value = true
}

async function submitAudit() {
  if (!auditForm.manualRemark.trim()) {
    ElMessage.warning('请填写审核备注')
    return
  }
  submitting.value = true
  try {
    await submitManualAudit({ ...auditForm })
    ElMessage.success('人工审核已提交')
    auditDialogVisible.value = false
    detailVisible.value = false
    await loadTickets()
  } catch (e) {
    ElMessage.error('人工审核提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleBatchAssign() {
  if (!assignee.value) {
    ElMessage.warning('请输入客服姓名')
    return
  }
  submitting.value = true
  try {
    await batchAssignTickets({ ticketNos: selectedRows.value.map(item => item.ticketNo), assignee: assignee.value })
    ElMessage.success(`已指派 ${selectedRows.value.length} 条工单给 ${assignee.value}`)
    assignDialogVisible.value = false
    assignee.value = ''
    await loadTickets()
  } catch (e) {
    ElMessage.error('批量指派失败')
  } finally {
    submitting.value = false
  }
}

async function handleBatchRetry() {
  const ticketNos = selectedRows.value.map(item => item.ticketNo)
  await retryTickets(ticketNos)
}

async function retrySingle(row) {
  await retryTickets([row.ticketNo])
}

async function retryTickets(ticketNos) {
  if (!ticketNos.length) return
  try {
    await ElMessageBox.confirm(`确认重试 ${ticketNos.length} 条工单的 AI 审核任务？`, '批量重试', { type: 'warning' })
    await batchRetryTickets({ ticketNos })
    ElMessage.success(`已重试 ${ticketNos.length} 条工单`)
    await loadTickets()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('工单重试失败')
    }
  }
}

async function handleExport() {
  try {
    await exportAfterSaleTickets(buildParams())
    ElMessage.success('导出请求已提交')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

function typeTag(type) {
  return type === 1 ? 'primary' : type === 2 ? 'warning' : 'danger'
}

function statusTag(status) {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'primary',
    3: 'danger',
    4: 'info'
  }
  return map[status] || 'info'
}

function afterSaleTypeText(type) {
  const map = {
    1: '仅退款',
    2: '退货退款',
    3: '投诉'
  }
  return map[type] || '-'
}

function ticketStatusText(status) {
  const map = {
    0: '待AI审核',
    1: 'AI已办结',
    2: '待人工审核',
    3: '已驳回',
    4: '已关闭'
  }
  return map[status] || '-'
}

function hasEvidence(row) {
  if (typeof row.hasEvidence === 'boolean') return row.hasEvidence
  return !!row.evidenceImages
}

function formatTableTime(value) {
  if (!value) return '-'
  return String(value).slice(0, 16)
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadTickets()])
})
</script>

<style scoped>
.orders-page {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  color: #1f2a37;
}

.workbench-head {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
  margin-bottom: 18px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(24, 144, 255, .12), rgba(82, 196, 26, .07)),
    #fff;
  border: 1px solid #e7edf5;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(22, 39, 65, .06);
}

.workbench-head::after {
  content: "";
  position: absolute;
  right: -90px;
  top: -120px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(24, 144, 255, .2), rgba(24, 144, 255, 0) 66%);
  pointer-events: none;
}

.title-block {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.eyebrow {
  margin-bottom: 8px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.workbench-head h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.25;
  font-weight: 800;
  color: #102033;
}

.workbench-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.head-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(156px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 16px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(22, 39, 65, .04);
  transition: transform .18s ease, box-shadow .18s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(22, 39, 65, .08);
}

.metric-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.metric-copy {
  min-width: 0;
}

.metric-card strong {
  display: block;
  color: #102033;
  font-size: 24px;
  line-height: 1;
  font-weight: 800;
}

.metric-card span {
  display: block;
  margin-top: 6px;
  color: #7b8494;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.surface-card {
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 10px 28px rgba(22, 39, 65, .05);
}

.surface-card :deep(.el-card__header) {
  padding: 18px 22px;
  border-bottom: 1px solid #edf1f7;
}

.surface-card :deep(.el-card__body) {
  padding: 18px 22px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.card-title {
  display: block;
  color: #102033;
  font-size: 16px;
  font-weight: 800;
}

.card-header small {
  display: block;
  margin-top: 4px;
  color: #8a94a6;
  font-size: 12px;
  font-weight: 400;
}

.filter-form {
  min-width: 0;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.filter-form :deep(.el-form-item__label) {
  color: #536174;
  font-weight: 600;
}

.filter-form :deep(.el-form-item__content) {
  min-width: 0;
}

.filter-form :deep(.el-select),
.filter-form :deep(.el-input),
.filter-form :deep(.el-date-editor) {
  width: 100%;
  max-width: 100%;
}

.filter-form :deep(.el-input__wrapper),
.filter-form :deep(.el-date-editor.el-input__wrapper) {
  border-radius: 7px;
  box-shadow: 0 0 0 1px #dbe2ec inset;
}

.filter-form :deep(.el-input__wrapper:hover),
.filter-form :deep(.el-date-editor.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #8cc8ff inset;
}

.action-form-item :deep(.el-form-item__content) {
  min-width: 0;
}

.filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.filter-actions :deep(.el-button) {
  margin-left: 0;
}

.table-tools {
  flex-shrink: 0;
}

.ticket-table {
  width: 100%;
  max-width: 100%;
}

.ticket-table :deep(.el-table__inner-wrapper),
.ticket-table :deep(.el-table__body-wrapper),
.ticket-table :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.ticket-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #687386;
  font-weight: 800;
}

.ticket-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.ticket-table :deep(.cell) {
  padding: 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-table :deep(.el-table__row) {
  transition: background .18s ease;
}

.ticket-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #f7fbff;
}

.ticket-link {
  display: inline-block;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.ticket-link:hover {
  color: #0958d9;
}

.ai-result {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.ai-result span {
  overflow: hidden;
  color: #243244;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-result small {
  color: #6b7a90;
}

.time-text {
  color: #4b5563;
  font-size: 13px;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  max-width: 100%;
  overflow-x: hidden;
  padding-top: 18px;
}

.drawer-body {
  min-height: 100%;
  background: #f6f8fb;
  padding: 24px;
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 10px;
}

.drawer-header span {
  color: #1677ff;
  font-size: 12px;
  font-weight: 800;
}

.drawer-header h2 {
  margin: 4px 0 0;
  color: #102033;
  font-size: 22px;
}

.drawer-header p {
  margin: 6px 0 0;
  color: #7b8494;
}

.detail-section {
  padding: 18px;
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  margin-bottom: 14px;
}

.section-title {
  margin-bottom: 14px;
  color: #102033;
  font-weight: 800;
}

.apply-box {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.apply-box div {
  min-width: 0;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.apply-box .full {
  grid-column: 1 / -1;
}

.apply-box span {
  display: block;
  color: #7b8494;
  font-size: 12px;
  margin-bottom: 6px;
}

.apply-box strong {
  color: #1f2a37;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.evidence-list {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.evidence-item {
  width: 90px;
  height: 64px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e6f4ff, #f6ffed);
  border: 1px solid #d6e4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
  font-size: 13px;
}

.rag-card {
  padding: 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fbfcfe;
  margin-bottom: 10px;
}

.rag-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.rag-card p {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
}

.audit-box {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.audit-score {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #389e0d;
  background: #f6ffed;
  border: 8px solid #d9f7be;
  font-size: 20px;
  font-weight: 800;
}

.audit-box h3 {
  margin: 0 0 8px;
  color: #102033;
}

.audit-box p {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
}

.audit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 12px;
  color: #7b8494;
  font-size: 13px;
}

.drawer-footer {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 0 0;
}

@media (max-width: 900px) {
  .workbench-head {
    flex-direction: column;
  }

  .head-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .head-actions .el-button {
    flex: 1;
  }

  .audit-box,
  .apply-box {
    grid-template-columns: 1fr;
  }
}
</style>
