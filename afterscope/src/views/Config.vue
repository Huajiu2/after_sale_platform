<template>
  <div class="config-page">
    <div class="page-header">
      <h1>系统参数配置</h1>
      <p>动态配置中心 — 无需重启服务，即时生效</p>
    </div>

    <div v-loading="loading">
      <el-card shadow="never" class="config-card" v-for="(group, groupName) in configMap" :key="groupName">
        <div class="group-title">{{ groupLabel(groupName) }}</div>
        <div class="config-row" v-for="item in group" :key="item.id">
          <label>{{ item.configDesc }}：</label>
          <el-input
            v-if="item.valueType !== 'boolean'"
            v-model="item.configValue"
            :type="item.valueType === 'int' ? 'number' : 'text'"
            :style="{ width: item.configKey.includes('url') ? '320px' : '200px' }"
          />
          <el-select v-else v-model="item.configValue" style="width:120px">
            <el-option label="已开启" value="true" />
            <el-option label="已关闭" value="false" />
          </el-select>
          <span class="hint">{{ defaultHint(item) }}</span>
        </div>
      </el-card>

      <div v-if="!loading && Object.keys(configMap).length === 0" class="empty-state">
        ⚙️ 暂无配置数据
      </div>

      <div class="actions">
        <el-button type="primary" :loading="saving" @click="handleSave">💾 保存配置</el-button>
        <el-button @click="handleResetAll">🔄 恢复默认</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchConfigList, updateConfig, batchUpdateConfig, resetConfig } from '@/api/config'

const loading = ref(false)
const saving = ref(false)
const configMap = ref({})

const groupLabels = {
  redis: '⚡ Redis 限流配置',
  ai_rag: '🧠 AI RAG 配置',
  mq: '📨 MQ 延迟工单配置'
}

function groupLabel(name) {
  return groupLabels[name] || name
}

function defaultHint(item) {
  const hints = {
    'rate_limit.max_requests_per_min': '默认 5 次/分钟',
    'rate_limit.token_bucket_capacity': '突发流量承载',
    'cache.qa_ttl_seconds': '秒',
    'ollama.base_url': 'Ollama 大模型服务地址',
    'rag.top_n': '默认 3 条',
    'chunk.size': '字符数',
    'timeout.manual_review_hours': '小时，默认 24h',
    'retry.max_attempts': '超过则入死信队列'
  }
  return hints[item.configKey] || ''
}

async function loadConfigs() {
  loading.value = true
  try {
    const res = await fetchConfigList()
    configMap.value = res.data || {}
  } catch (e) {
    console.error('配置加载失败', e)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const allItems = Object.values(configMap.value).flat()
    const changed = allItems.filter(item => item._origin === undefined || item.configValue !== item._origin)
    if (changed.length === 0) {
      ElMessage.info('没有需要保存的修改')
      return
    }
    const res = await batchUpdateConfig({ configs: changed.map(i => ({ id: i.id, configValue: i.configValue })) })
    ElMessage.success(res.message || '配置已保存')
    loadConfigs()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handleResetAll() {
  try {
    await ElMessageBox.confirm('确认恢复所有配置为默认值？', '确认', { type: 'warning' })
  } catch {
    return
  }
  const allItems = Object.values(configMap.value).flat()
  for (const item of allItems) {
    try {
      await resetConfig(item.id)
    } catch (e) {
      console.warn('恢复失败', item.configKey, e)
    }
  }
  ElMessage.success('配置已恢复默认')
  loadConfigs()
}

onMounted(loadConfigs)
</script>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h1 { font-size: 22px; font-weight: 600; color: #1a1a2e; }
.page-header p { color: #888; font-size: 14px; margin-top: 4px; }

.config-card { border-radius: 12px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.group-title { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 16px; }

.config-row { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.config-row label { font-size: 13px; color: #666; min-width: 200px; flex-shrink: 0; }
.config-row :deep(.el-input__wrapper) { border-radius: 6px; }
.hint { font-size: 12px; color: #999; }

.actions { display: flex; gap: 8px; margin-top: 16px; }
.empty-state { text-align: center; color: #999; padding: 80px 0; font-size: 15px; }
</style>
