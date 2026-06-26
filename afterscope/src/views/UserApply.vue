<template>
  <div class="user-apply">
    <div class="page-header">
      <h1>用户端售后申请</h1>
      <p>模拟用户发起售后请求，测试后端完整链路（MQ 异步 + AI 审核）</p>
    </div>

    <div class="apply-card">
      <div class="card-title">📝 填写售后申请</div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="订单号" prop="orderNo">
          <el-input
            v-model="form.orderNo"
            placeholder="请输入订单号，如 DD998765"
            maxlength="32"
            clearable
          />
        </el-form-item>

        <el-form-item label="售后类型" prop="afterSaleType">
          <div class="type-options">
            <div
              v-for="t in typeOptions"
              :key="t.value"
              class="type-card"
              :class="{ active: form.afterSaleType === t.value }"
              @click="form.afterSaleType = t.value"
            >
              <span class="type-icon">{{ t.icon }}</span>
              <span class="type-label">{{ t.label }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="申请原因" prop="applyReason">
          <el-input
            v-model="form.applyReason"
            type="textarea"
            :rows="4"
            placeholder="请描述售后原因，至少 10 个字"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="凭证图片">
          <div class="image-upload">
            <div class="upload-tip">
              支持选择本地 <code>.jpg</code>、<code>.jpeg</code>、<code>.png</code>、<code>.gif</code> 文件，最多 5 张
            </div>

            <div class="image-grid" v-if="images.length">
              <div
                v-for="(image, idx) in images"
                :key="image.id"
                class="image-item"
              >
                <div class="image-preview">
                  <img :src="image.previewUrl" :alt="image.name" />
                </div>
                <div class="image-meta">
                  <div class="image-name" :title="image.name">{{ image.name }}</div>
                  <div class="image-size">{{ formatFileSize(image.size) }}</div>
                </div>
                <el-button class="remove-btn" type="danger" link @click="removeImage(idx)">
                  移除
                </el-button>
              </div>
            </div>

            <el-upload
              ref="uploadRef"
              class="image-picker"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              multiple
              accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif"
              :on-change="handleFileChange"
            >
              <el-button
                v-if="images.length < 5"
                class="add-image-btn"
              >
                + 选择图片文件
              </el-button>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            @click="handleSubmit"
            :loading="submitting"
            class="submit-btn"
          >
            {{ submitting ? '提交中...' : '提交售后申请' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-dialog v-model="resultVisible" title="提交成功" width="480px">
      <div class="result-body">
        <div class="result-icon">🎉</div>
        <p class="result-msg">{{ result.message }}</p>
        <div class="result-detail">
          <div class="result-row">
            <span class="result-label">工单号</span>
            <span class="result-value">{{ result.ticketNo }}</span>
          </div>
          <div class="result-row">
            <span class="result-label">工单状态</span>
            <el-tag type="warning">{{ result.statusText }}</el-tag>
          </div>
          <div class="result-row">
            <span class="result-label">预计时间</span>
            <span class="result-value">{{ result.estimatedTime }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="resetForm">继续申请</el-button>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="errorVisible" title="提交失败" width="400px">
      <div class="result-body">
        <div class="result-icon" style="font-size:48px;">😟</div>
        <p class="result-msg" style="color:#ff4d4f;">{{ errorMsg }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="errorVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const typeOptions = [
  { value: 1, icon: '💰', label: '仅退款' },
  { value: 2, icon: '📦', label: '退货退款' },
  { value: 3, icon: '❗', label: '投诉' },
]

const formRef = ref(null)
const uploadRef = ref(null)

const form = reactive({
  orderNo: '',
  afterSaleType: null,
  applyReason: '',
})

const rules = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  afterSaleType: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  applyReason: [
    { required: true, message: '请填写申请原因', trigger: 'blur' },
    { min: 10, message: '原因至少 10 个字', trigger: 'blur' },
  ],
}

const submitting = ref(false)

const images = ref([])
let imageSeq = 0

const resultVisible = ref(false)
const result = reactive({
  message: '',
  ticketNo: '',
  statusText: '',
  estimatedTime: '',
})

const errorVisible = ref(false)
const errorMsg = ref('')

function isAllowedImage(file) {
  return ['image/jpeg', 'image/png', 'image/gif'].includes(file.type)
}

function formatFileSize(size) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(new Error('图片读取失败'))
    reader.readAsDataURL(file)
  })
}

async function handleFileChange(uploadFile, uploadFiles) {
  const rawFile = uploadFile.raw
  if (!rawFile) return

  if (!isAllowedImage(rawFile)) {
    ElMessage.warning('仅支持 JPG、JPEG、PNG、GIF 图片')
    uploadRef.value?.handleRemove(uploadFile)
    return
  }

  if (images.value.length >= 5) {
    ElMessage.warning('最多只能添加 5 张图片')
    uploadRef.value?.handleRemove(uploadFile)
    return
  }

  if (images.value.some((item) => item.name === rawFile.name && item.size === rawFile.size)) {
    ElMessage.info('这张图片已经添加过了')
    uploadRef.value?.handleRemove(uploadFile)
    return
  }

  try {
    const previewUrl = await fileToDataUrl(rawFile)
    images.value.push({
      id: `${Date.now()}-${imageSeq += 1}`,
      name: rawFile.name,
      size: rawFile.size,
      mimeType: rawFile.type,
      file: rawFile,
      previewUrl,
    })
  } catch (err) {
    ElMessage.error(err?.message || '图片读取失败')
  } finally {
    uploadRef.value?.clearFiles()
  }
}

function removeImage(idx) {
  images.value.splice(idx, 1)
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('orderNo', form.orderNo)
    formData.append('afterSaleType', form.afterSaleType)
    formData.append('applyReason', form.applyReason)
    images.value.forEach((item) => {
      formData.append('evidenceFiles', item.file)
    })

    const res = await request.post('/after-sale/submit', formData)

    if (res.code === 200) {
      result.message = res.message || '提交成功'
      result.ticketNo = res.data.ticketNo
      result.statusText =
        ['待AI审核', 'AI已办结', '待人工审核', '已驳回', '已关闭'][res.data.ticketStatus] || '未知'
      result.estimatedTime = res.data.estimatedTime || '30秒内出结果'
      resultVisible.value = true
    } else {
      errorMsg.value = res.message || '提交失败'
      errorVisible.value = true
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '网络异常，请稍后重试'
    errorVisible.value = true
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  resultVisible.value = false
  form.orderNo = ''
  form.afterSaleType = null
  form.applyReason = ''
  images.value = []
  formRef.value?.resetFields()
}
</script>

<style scoped>
.user-apply {
  max-width: 860px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
}

.page-header p {
  color: #888;
  font-size: 14px;
  margin-top: 4px;
}

.apply-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1a1a2e;
}

.type-options {
  display: flex;
  gap: 16px;
  width: 100%;
}

.type-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.type-card:hover {
  border-color: #91d5ff;
  background: #e6f7ff;
}

.type-card.active {
  border-color: #1890ff;
  background: #e6f7ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.15);
}

.type-icon {
  font-size: 28px;
}

.type-label {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
}

.image-upload {
  width: 100%;
}

.upload-tip {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.image-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  background: #fafafa;
}

.image-preview {
  height: 140px;
  background: #f5f7fa;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-meta {
  padding: 10px 12px 8px;
}

.image-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.image-size {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.remove-btn {
  width: 100%;
  border-top: 1px solid #ebeef5;
  border-radius: 0;
  padding: 10px 0;
}

.image-picker {
  display: block;
}

.add-image-btn {
  width: 100%;
  border-style: dashed !important;
  color: #666;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.result-body {
  text-align: center;
  padding: 16px 0;
}

.result-icon {
  font-size: 56px;
  margin-bottom: 12px;
}

.result-msg {
  font-size: 16px;
  color: #333;
  margin-bottom: 20px;
}

.result-detail {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px 20px;
  text-align: left;
}

.result-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.result-row:last-child {
  border: none;
}

.result-label {
  color: #888;
  font-size: 14px;
}

.result-value {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}
</style>
