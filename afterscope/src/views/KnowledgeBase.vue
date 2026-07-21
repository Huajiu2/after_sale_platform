<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h1>AI 售后知识库管理</h1>
      <p>管理RAG规则文档、行业判例，支持异步向量化入库</p>
    </div>

    <!-- 搜索 + 上传 -->
    <el-card shadow="never" class="search-card">
      <div class="search-bar">
        <div class="search-item">
          <label>文档名称：</label>
          <el-input
            v-model="query.docName"
            placeholder="搜索文档名称..."
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div style="flex: 1" />
        <el-button type="primary" @click="uploadDialogVisible = true">
          <el-icon style="margin-right:4px"><Upload /></el-icon>上传文档
        </el-button>
      </div>
    </el-card>

    <!-- 文档列表 -->
    <el-card shadow="never" class="table-card">
      <div class="card-header">
        <span class="card-title">📄 文档列表</span>
        <el-tag type="info" effect="plain" size="small">共 {{ total }} 条</el-tag>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width:100%" @sort-change="handleSortChange">
        <el-table-column prop="docCode" label="文档ID" width="100" />
        <el-table-column prop="docName" label="文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryDesc" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="categoryTagType(row.category)" size="small" effect="plain">
              {{ row.categoryDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100" />
        <el-table-column prop="chunkCount" label="切片数" width="80" align="center" />
        <el-table-column prop="vectorizeStatusDesc" label="向量化状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.vectorizeStatus)" size="small">
              {{ row.vectorizeStatusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadedAt" label="入库时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleShowChunks(row)">
              查看切片
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.vectorizeStatus !== 2"
              @click="handleReVectorize(row)"
            >
              重新向量化
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
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

    <!-- 上传文档 Dialog -->
    <el-dialog v-model="uploadDialogVisible" title="📤 上传知识库文档" width="560px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('每次仅能上传 1 个文件')"
            accept=".md,.pdf,.doc,.docx,.txt"
          >
            <el-button type="primary" plain>
              <el-icon style="margin-right:4px"><FolderOpened /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <span class="upload-tip">支持 MarkDown / PDF / Word / TXT，最大 20MB</span>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档分类" required>
          <el-select v-model="uploadForm.category" placeholder="请选择文档分类" style="width:100%">
            <el-option label="平台通用规则" value="platform_general" />
            <el-option label="数码售后规则" value="digital" />
            <el-option label="生鲜售后规则" value="fresh" />
            <el-option label="服饰售后规则" value="apparel" />
            <el-option label="食品保健&医疗器械专项" value="medical" />
            <el-option label="历史判例" value="history_case" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" show-icon style="margin-top:8px">
        <template #title>
          提交后文档将发送至 RabbitMQ 解析队列，异步完成文本提取与向量化入库，请勿重复提交。
        </template>
      </el-alert>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadSubmit">📤 提交入库</el-button>
      </template>
    </el-dialog>

    <!-- 切片详情 Dialog -->
    <el-dialog v-model="chunkDialogVisible" :title="`📑 文档切片详情 — ${chunkDocName}`" width="680px">
      <div class="chunk-meta">
        文档：{{ chunkDocName }} · 共 <strong>{{ chunkTotal }}</strong> 个切片 · ChunkSize: 512
      </div>
      <div v-loading="chunkLoading">
        <div
          v-for="item in chunkList"
          :key="item.chunkId"
          class="chunk-item"
        >
          <div class="chunk-num">Chunk #{{ item.chunkIndex + 1 }}</div>
          <div class="chunk-text">{{ item.chunkText }}</div>
        </div>
        <el-empty v-if="!chunkLoading && chunkList.length === 0" description="暂无切片数据" />
      </div>
      <template #footer>
        <el-button @click="chunkDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, FolderOpened } from '@element-plus/icons-vue'
import {
  fetchKnowledgeList,
  uploadKnowledgeDoc,
  fetchDocChunks,
  reVectorizeDoc,
  deleteKnowledgeDoc
} from '@/api/knowledge'

// ---------- 列表 ----------
const list = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  page: 1,
  size: 20,
  docName: ''
})

async function fetchList() {
  loading.value = true
  try {
    const res = await fetchKnowledgeList(query)
    list.value = (res.data?.records || res.data || [])
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  fetchList()
}

function handleSortChange() {
  fetchList()
}

// ---------- 状态/分类 Tag 映射 ----------
const statusMap = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger'
}
function statusTagType(status) {
  return statusMap[status] || 'info'
}

const categoryColorMap = {
  platform_general: '',
  digital: 'purple',
  fresh: 'warning',
  apparel: 'cyan',
  home_appliance: 'success',
  beauty: 'danger',
  medical: 'warning',
  history_case: 'info'
}
function categoryTagType(category) {
  return categoryColorMap[category] || 'info'
}

// ---------- 上传文档 ----------
const uploadDialogVisible = ref(false)
const uploadRef = ref(null)
const uploading = ref(false)
const uploadForm = reactive({
  category: '',
  file: null
})

function handleFileChange(file) {
  uploadForm.file = file.raw
}

async function handleUploadSubmit() {
  if (!uploadForm.file) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  if (!uploadForm.category) {
    ElMessage.warning('请选择文档分类')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadForm.file)
    fd.append('category', uploadForm.category)
    await uploadKnowledgeDoc(fd)
    ElMessage.success('文档上传成功，已进入异步解析向量化队列')
    uploadDialogVisible.value = false
    uploadForm.category = ''
    uploadForm.file = null
    if (uploadRef.value) uploadRef.value.clearFiles()
    fetchList()
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

// ---------- 切片详情 ----------
const chunkDialogVisible = ref(false)
const chunkLoading = ref(false)
const chunkList = ref([])
const chunkTotal = ref(0)
const chunkDocName = ref('')

async function handleShowChunks(row) {
  chunkDocName.value = row.docName
  chunkDialogVisible.value = true
  chunkLoading.value = true
  chunkList.value = []
  try {
    const res = await fetchDocChunks(row.docId, { page: 1, size: 50 })
    chunkList.value = res.data?.records || []
    chunkTotal.value = res.data?.totalChunks || 0
  } catch (e) {
    ElMessage.error('获取切片详情失败')
  } finally {
    chunkLoading.value = false
  }
}

// ---------- 重新向量化 ----------
async function handleReVectorize(row) {
  try {
    await ElMessageBox.confirm(
      `确认重新向量化文档「${row.docName}」？`,
      '重新向量化',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
    await reVectorizeDoc(row.docId)
    ElMessage.success('文档已重新进入解析向量化队列')
    fetchList()
  } catch (e) {
    // cancelled or error
    if (e !== 'cancel') ElMessage.error('重新向量化失败')
  }
}

// ---------- 删除文档 ----------
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除文档「${row.docName}」及其所有切片？`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'danger' }
    )
    await deleteKnowledgeDoc(row.docId)
    ElMessage.success('文档已删除')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
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

.search-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
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
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.chunk-meta {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
}
.chunk-item {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 10px;
}
.chunk-num {
  font-size: 12px;
  color: #1890ff;
  font-weight: 600;
  margin-bottom: 4px;
}
.chunk-text {
  font-size: 13px;
  line-height: 1.7;
  color: #555;
}
</style>
