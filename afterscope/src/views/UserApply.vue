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
            <div
              v-for="(url, idx) in form.evidenceImages"
              :key="idx"
              class="image-item"
            >
              <el-input
                v-model="form.evidenceImages[idx]"
                placeholder="输入图片 URL"
                clearable
              >
                <template #prepend>{{ idx + 1 }}</template>
                <template #append>
                  <el-button @click="removeImage(idx)" text>✕</el-button>
                </template>
              </el-input>
            </div>
            <el-button
              v-if="form.evidenceImages.length < 5"
              @click="addImage"
              class="add-image-btn"
            >
              + 添加图片（最多 5 张）
            </el-button>
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

    <!-- 提交结果 -->
    <el-dialog v-model="resultVisible" title="✅ 提交成功" width="480px">
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

    <!-- 异常提示 -->
    <el-dialog v-model="errorVisible" title="提交失败" width="400px">
      <div class="result-body">
        <div class="result-icon" style="font-size:48px;">😵</div>
        <p class="result-msg" style="color:#ff4d4f;">{{ errorMsg }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="errorVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const typeOptions = [
  { value: 1, icon: '💰', label: '仅退款' },
  { value: 2, icon: '📦', label: '退货退款' },
  { value: 3, icon: '⚠️', label: '投诉' },
]

const formRef = ref(null)
const form = reactive({
  orderNo: '',
  afterSaleType: null,
  applyReason: '',
  evidenceImages: [],
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

const resultVisible = ref(false)
const result = reactive({
  message: '',
  ticketNo: '',
  statusText: '',
  estimatedTime: '',
})

const errorVisible = ref(false)
const errorMsg = ref('')

function addImage() {
  form.evidenceImages.push('')
}

function removeImage(idx) {
  form.evidenceImages.splice(idx, 1)
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await request.post('/after-sale/submit', {
      orderNo: form.orderNo,
      afterSaleType: form.afterSaleType,
      applyReason: form.applyReason,
      evidenceImages: form.evidenceImages.filter(Boolean),
    })

    if (res.code === 200) {
      result.message = res.message
      result.ticketNo = res.data.ticketNo
      result.statusText = ['待AI审核', 'AI已办结', '待人工审核', '已驳回', '已关闭'][res.data.ticketStatus] || '未知'
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
  form.evidenceImages = []
  formRef.value.resetFields()
}
</script>

<style scoped>
.user-apply { max-width: 800px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h1 { font-size: 22px; font-weight: 600; margin: 0; }
.page-header p { color: #888; font-size: 14px; margin-top: 4px; }

.apply-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
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
  transition: all .2s;
  background: #fafafa;
}
.type-card:hover {
  border-color: #91d5ff;
  background: #e6f7ff;
}
.type-card.active {
  border-color: #1890ff;
  background: #e6f7ff;
  box-shadow: 0 0 0 2px rgba(24,144,255,.15);
}
.type-icon { font-size: 28px; }
.type-label { font-size: 14px; font-weight: 600; color: #333; white-space: nowrap; }

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.image-item :deep(.el-input-group__prepend) {
  width: 36px;
  text-align: center;
}
.add-image-btn {
  width: 100%;
  border-style: dashed !important;
  color: #888;
}

.submit-btn { width: 100%; margin-top: 8px; }

.result-body { text-align: center; padding: 16px 0; }
.result-icon { font-size: 56px; margin-bottom: 12px; }
.result-msg { font-size: 16px; color: #333; margin-bottom: 20px; }
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
.result-row:last-child { border: none; }
.result-label { color: #888; font-size: 14px; }
.result-value { font-weight: 600; color: #333; font-size: 14px; }
</style>
