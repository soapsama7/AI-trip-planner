<template>
  <div class="home-container">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="page-header">
      <div class="icon-wrapper">
        <span class="icon">✈️</span>
      </div>
      <h1 class="page-title">智能旅行助手</h1>
      <p class="page-subtitle">基于AI的个性化旅行规划,让每一次出行都完美无忧</p>
    </div>

    <a-card class="form-card" :bordered="false">
      <a-form :model="formState" layout="vertical" @finish="handleSubmit">
        <div class="form-section">
          <div class="section-header">
            <span class="section-icon">📍</span>
            <span class="section-title">目的地与时间</span>
          </div>

          <a-row :gutter="24">
            <a-col :span="10">
              <a-form-item name="cityCascade" :rules="[{ required: true, message: '请选择省市' }]">
                <template #label>
                  <span class="form-label">省市选择</span>
                </template>
                <a-cascader
                  v-model:value="formState.cityCascade"
                  :options="provinceCityOptions"
                  placeholder="请选择省 / 市"
                  size="large"
                  class="custom-input"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item name="dateRange" :rules="[{ required: true, message: '请选择出行日期范围' }]">
                <template #label>
                  <span class="form-label">出行日期</span>
                  <span class="form-hint">（含起止日最多 7 天）</span>
                </template>
                <a-range-picker
                  v-model:value="formState.dateRange"
                  style="width: 100%"
                  size="large"
                  class="custom-input"
                  :disabled-date="disabledDate"
                  @calendar-change="onCalendarChange"
                  @open-change="onOpenChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item name="budget" :rules="[{ required: true, message: '请输入预算' }]">
                <template #label>
                  <span class="form-label">预算 (元)</span>
                </template>
                <a-input-number
                  v-model:value="formState.budget"
                  :min="1"
                  :step="100"
                  style="width: 100%"
                  size="large"
                  class="custom-input"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading" size="large" block class="submit-button">
            <template v-if="!loading">
              <span class="button-icon">🚀</span>
              <span>开始规划我的旅行</span>
            </template>
            <template v-else>
              <span>正在生成中...</span>
            </template>
          </a-button>
        </a-form-item>

        <a-form-item v-if="loading">
          <a-button danger size="large" block class="cancel-button" @click="handleCancelPlanning">
            取消规划
          </a-button>
        </a-form-item>

        <a-form-item v-if="loading">
          <div class="loading-container">
            <a-progress
              :percent="progress"
              status="active"
              :stroke-color="{ '0%': '#667eea', '100%': '#764ba2' }"
              :stroke-width="10"
            />
            <p class="loading-status">
              {{ statusText }}
            </p>
          </div>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="history-card" :bordered="false" title="历史行程记录">
      <template #extra>
        <div class="history-actions">
          <a-button size="small" :loading="historyLoading" @click="loadHistory">刷新</a-button>
        </div>
      </template>

      <a-spin :spinning="historyLoading">
        <a-alert v-if="historyError" type="error" show-icon :message="historyError" />
        <a-empty v-else-if="!historyList.length" description="暂无历史行程记录" />
        <a-collapse v-else>
          <a-collapse-panel v-for="item in historyList" :key="item.id">
            <template #header>
              <div class="history-header">
                <span class="history-city">{{ item.city }}</span>
                <span class="history-time">{{ item.travelTime }}</span>
                <span class="history-cost">预算 ¥{{ item.budget }} / 预估 ¥{{ item.totalCost }}</span>
              </div>
            </template>

            <div class="history-meta">创建时间：{{ formatCreatedAt(item.createdAt) }}</div>
            <div class="history-export-actions">
              <a-button size="small" :loading="historyExportingId === item.id" @click="exportHistoryAsImage(item.id, item.city)">
                导出图片
              </a-button>
              <a-button size="small" :loading="historyExportingId === item.id" @click="exportHistoryAsPdf(item.id, item.city)">
                导出 PDF
              </a-button>
            </div>
            <div v-if="item.plan" :id="`history-export-${item.id}`" class="history-plan">
              <p class="history-summary">🧾 {{ item.plan.summary }}</p>
              <a-list :data-source="item.plan.dailyPlans" item-layout="vertical">
                <template #renderItem="{ item: day }">
                  <a-list-item>
                    <div class="history-day-title">
                      <strong>{{ day.day }}</strong>
                      <span>{{ day.theme }}</span>
                      <span>¥{{ day.estimatedCost }}</span>
                    </div>
                    <a-list :data-source="day.planItems" size="small">
                      <template #renderItem="{ item: planItem }">
                        <a-list-item>
                          {{ planItem.period }}｜{{ planItem.place }}｜{{ planItem.activity }}｜{{ planItem.transport }}｜¥{{ planItem.expectedCost }}
                        </a-list-item>
                      </template>
                    </a-list>
                  </a-list-item>
                </template>
              </a-list>
              <a-list v-if="item.plan.tips?.length" :data-source="item.plan.tips" size="small" class="history-tips">
                <template #renderItem="{ item: tip }">
                  <a-list-item>提示：{{ tip }}</a-list-item>
                </template>
              </a-list>
            </div>
          </a-collapse-panel>
        </a-collapse>
        <div v-if="!historyError && historyList.length" class="history-pagination">
          <a-button size="small" :disabled="historyPage <= 1 || historyLoading" @click="changeHistoryPage(historyPage - 1)">
            上一页
          </a-button>
          <span class="history-page-text">
            第 {{ historyPage }} / {{ historyTotalPages }} 页（共 {{ historyTotal }} 条，每页 {{ HISTORY_PAGE_SIZE }} 条）
          </span>
          <a-input-number
            v-model:value="historyJumpPage"
            :min="1"
            :max="Math.max(historyTotalPages, 1)"
            :precision="0"
            size="small"
            class="history-jump-input"
            placeholder="页码"
          />
          <a-button size="small" :disabled="historyLoading" @click="jumpToHistoryPage">跳转</a-button>
          <a-button
            size="small"
            :disabled="historyLoading || historyPage >= historyTotalPages"
            @click="changeHistoryPage(historyPage + 1)"
          >
            下一页
          </a-button>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { buildProvinceCityOptions } from '@/utils/chinaCity'
import { cancelTripTask, queryTaskStatus, queryTripHistory, startTripTask, subscribeTripEvents } from '@/services/api'
import { createExportContainer, exportElementAsImage, exportElementAsPdf } from '@/utils/exporter'
import type { TripGenerateRequest, TripHistoryItem, TripStreamEvent } from '@/types'

const router = useRouter()

const provinceCityOptions = buildProvinceCityOptions()

const loading = ref(false)
const progress = ref(0)
const statusText = ref('正在初始化...')
const historyLoading = ref(false)
const historyError = ref('')
const historyList = ref<TripHistoryItem[]>([])
const historyPage = ref(1)
const historyJumpPage = ref<number | null>(null)
const historyTotal = ref(0)
const historyTotalPages = ref(0)
const historyExportingId = ref<number | null>(null)
const HISTORY_PAGE_SIZE = 5
let stopSse: null | (() => void) = null
const ACTIVE_TASK_ID_KEY = 'tripActiveTaskId'

/** 含起止日最多 7 天 → 结束日 − 开始日 的 dayjs diff 最大为 6 */
const MAX_TRIP_DAY_SPAN = 6

/** 与后端 BudgetValidator.REJECT_PER_DAY 一致；仅用于前端展示（避免单日无住宿却仍提示住宿保底） */
const MIN_DAILY_BUDGET_FOR_PLAN = 150

function humanizeBudgetRejectError(raw: string): string {
  if (!raw) return raw
  if (raw.includes('预算不足') || raw.includes('住宿保底')) {
    return `当前日均预算偏低。为便于覆盖餐饮、市内交通等基本开销，建议每日预算不低于 ${MIN_DAILY_BUDGET_FOR_PLAN} 元。`
  }
  return raw
}

const formState = reactive<{
  cityCascade: string[]
  dateRange: [Dayjs, Dayjs] | null
  budget: number | null
}>({
  cityCascade: [],
  dateRange: null,
  budget: 3000
})

/** 日历面板打开、正在点选某一天时的临时区间，用于 disabledDate 限制跨度 */
const calendarRange = ref<[Dayjs | null, Dayjs | null] | null>(null)

function onCalendarChange(val: [Dayjs, Dayjs] | [Dayjs | null, Dayjs | null] | null) {
  calendarRange.value = val as [Dayjs | null, Dayjs | null] | null
}

function onOpenChange(open: boolean) {
  if (!open) calendarRange.value = null
}

function disabledDate(current: Dayjs) {
  const dr = calendarRange.value
  if (!dr) return false
  const [start, end] = dr
  const tooLate = !!start && current.diff(start, 'day') > MAX_TRIP_DAY_SPAN
  const tooEarly = !!end && end.diff(current, 'day') > MAX_TRIP_DAY_SPAN
  return tooLate || tooEarly
}

const selectedCityName = computed(() => {
  const codes = formState.cityCascade
  if (!codes || codes.length < 2) return ''
  const province = provinceCityOptions.find((p) => p.value === codes[0])
  const city = province?.children?.find((c) => c.value === codes[1])
  const label = city?.label || ''
  return label.replace(/(市|地区|自治州|盟)$/, '')
})

function toTravelTime(range: [Dayjs, Dayjs]) {
  return `${range[0].format('YYYY-MM-DD')}~${range[1].format('YYYY-MM-DD')}`
}

const handleSubmit = async () => {
  if (loading.value) {
    return
  }
  if (!formState.dateRange) {
    message.error('请选择出行日期范围')
    return
  }
  const [start, end] = formState.dateRange
  const span = end.diff(start, 'day')
  if (span > MAX_TRIP_DAY_SPAN) {
    message.error(`出行天数不能超过 ${MAX_TRIP_DAY_SPAN + 1} 天（含起止日）`)
    return
  }
  const city = selectedCityName.value
  if (!city) {
    message.error('请选择省市')
    return
  }
  const budget = formState.budget || 0
  if (budget <= 0) {
    message.error('预算必须大于 0')
    return
  }

  loading.value = true
  progress.value = 0
  statusText.value = '正在创建任务...'

  const payload: TripGenerateRequest = {
    city,
    travelTime: toTravelTime(formState.dateRange),
    budget
  }

  try {
    const { taskId } = await startTripTask(payload)
    localStorage.setItem(ACTIVE_TASK_ID_KEY, taskId)
    attachSse(taskId)
  } catch (e: any) {
    const raw = e?.response?.data?.message || e?.message || '创建任务失败'
    message.error(humanizeBudgetRejectError(raw))
    loading.value = false
  }
}

function clearActiveTask() {
  localStorage.removeItem(ACTIVE_TASK_ID_KEY)
  stopSse?.()
  stopSse = null
}

function finishTask() {
  loading.value = false
  clearActiveTask()
}

function attachSse(taskId: string) {
  stopSse?.()
  stopSse = subscribeTripEvents(
    taskId,
    (evt: TripStreamEvent) => {
      progress.value = Math.max(progress.value, evt.progress ?? 0)
      statusText.value = evt.message || stageToText(evt.stage)

      if (evt.type === 'DONE') {
        if (evt.result) {
          sessionStorage.setItem(`tripResult:${taskId}`, JSON.stringify(evt.result))
          finishTask()
          router.push(`/result/${taskId}`)
        } else {
          message.error('行程生成完成但未返回结果，请重试')
          finishTask()
        }
      }
      if (evt.type === 'ERROR') {
        message.error(evt.message || '任务失败')
        finishTask()
      }
    },
    async (err: any) => {
      console.error('SSE error', err)
      await recoverTask(taskId, true)
    }
  )
}

async function recoverTask(taskId: string, silent = false) {
  try {
    loading.value = true
    statusText.value = '正在恢复任务状态...'
    const status = await queryTaskStatus(taskId)
    progress.value = Math.max(progress.value, status.progress ?? 0)
    if (status.result) {
      sessionStorage.setItem(`tripResult:${taskId}`, JSON.stringify(status.result))
      finishTask()
      router.push(`/result/${taskId}`)
      return
    }
    if (status.progress === -1 || status.error) {
      if (!silent) {
        message.warning(status.error || '任务已结束')
      }
      finishTask()
      return
    }
    statusText.value = stageToText('RECOVERING')
    attachSse(taskId)
  } catch (e: any) {
    if (!silent) {
      message.error(e?.message || '恢复任务失败，请重试')
    }
    finishTask()
  }
}

async function handleCancelPlanning() {
  const taskId = localStorage.getItem(ACTIVE_TASK_ID_KEY)
  if (!taskId) {
    loading.value = false
    return
  }
  try {
    await cancelTripTask(taskId)
    message.success('已提交取消请求')
  } catch (e: any) {
    message.error(e?.message || '取消失败，请稍后重试')
    return
  }
  progress.value = 0
  statusText.value = '任务已取消'
  finishTask()
}

function onBeforeUnload(event: BeforeUnloadEvent) {
  if (!loading.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

function stageToText(stage: string) {
  switch (stage) {
    case 'WEATHER':
      return '🌤️ 正在生成天气建议...'
    case 'ATTRACTIONS':
      return '📍 正在推荐景点...'
    case 'ITINERARY':
      return '📋 正在生成行程...'
    case 'FINALIZING':
      return '🧩 正在整理行程结果...'
    case 'RECOVERING':
      return '🔄 已恢复任务，正在继续等待结果...'
    default:
      return '⏳ 正在处理中...'
  }
}

function formatCreatedAt(val: string) {
  if (!val) return '-'
  return val.replace('T', ' ')
}

async function loadHistory() {
  historyLoading.value = true
  historyError.value = ''
  try {
    const data = await queryTripHistory(historyPage.value, HISTORY_PAGE_SIZE)
    historyList.value = data.records || []
    historyPage.value = data.page || historyPage.value
    historyTotal.value = data.total || 0
    historyTotalPages.value = data.totalPages || 0
    if (historyJumpPage.value == null) {
      historyJumpPage.value = historyPage.value
    }
  } catch (e: any) {
    historyError.value = e?.message || '加载历史行程失败'
    historyTotal.value = 0
    historyTotalPages.value = 0
  } finally {
    historyLoading.value = false
  }
}

function changeHistoryPage(nextPage: number) {
  if (historyTotalPages.value > 0 && nextPage > historyTotalPages.value) return
  if (nextPage < 1 || nextPage === historyPage.value) return
  historyPage.value = nextPage
  historyJumpPage.value = nextPage
  loadHistory()
}

function jumpToHistoryPage() {
  const nextPage = Number(historyJumpPage.value)
  if (!Number.isInteger(nextPage)) {
    message.warning('请输入有效的整数页码')
    return
  }
  if (historyTotalPages.value <= 0) {
    message.warning('当前没有可跳转的历史页')
    return
  }
  if (nextPage < 1 || nextPage > historyTotalPages.value) {
    message.warning(`页码无效，请输入 1 到 ${historyTotalPages.value} 之间的页码`)
    return
  }
  changeHistoryPage(nextPage)
}

async function exportHistoryAsImage(id: number, city: string) {
  const item = historyList.value.find((x) => x.id === id)
  if (!item?.plan) {
    message.warning('未找到可导出的历史行程内容')
    return
  }
  const html = buildHistoryExportHtml(item)
  historyExportingId.value = id
  try {
    const wrapper = createExportContainer(html)
    try {
      await exportElementAsImage(wrapper.el, `${city}_历史行程`)
    } finally {
      wrapper.destroy()
    }
    message.success('历史行程图片导出成功')
  } catch (e: any) {
    message.error(e?.message || '历史行程图片导出失败')
  } finally {
    historyExportingId.value = null
  }
}

async function exportHistoryAsPdf(id: number, city: string) {
  const item = historyList.value.find((x) => x.id === id)
  if (!item?.plan) {
    message.warning('未找到可导出的历史行程内容')
    return
  }
  const html = buildHistoryExportHtml(item)
  historyExportingId.value = id
  try {
    const wrapper = createExportContainer(html)
    try {
      await exportElementAsPdf(wrapper.el, `${city}_历史行程`)
    } finally {
      wrapper.destroy()
    }
    message.success('历史行程 PDF 导出成功')
  } catch (e: any) {
    message.error(e?.message || '历史行程 PDF 导出失败')
  } finally {
    historyExportingId.value = null
  }
}

function buildHistoryExportHtml(item: TripHistoryItem) {
  const plan = item.plan
  if (!plan) return ''
  const daysHtml = plan.dailyPlans
    .map((day) => {
      const items = day.planItems
        .map(
          (it) =>
            `<li style="margin:6px 0;line-height:1.6;">${it.period} ｜ ${it.place} ｜ ${it.activity} ｜ ${it.transport} ｜ ¥${it.expectedCost}</li>`
        )
        .join('')
      return `
        <section style="margin:14px 0;padding:12px;border:1px solid #e8e8e8;border-radius:8px;">
          <div style="font-weight:700;margin-bottom:8px;">${day.day} · ${day.theme} · ¥${day.estimatedCost}</div>
          <ul style="padding-left:18px;margin:0;">${items}</ul>
        </section>
      `
    })
    .join('')
  const tips = (plan.tips || []).map((t) => `<li style="margin:4px 0;">${t}</li>`).join('')
  return `
    <div style="font-family:Arial,'Microsoft YaHei',sans-serif;color:#222;">
      <h1 style="margin:0 0 10px 0;">${item.city} 历史行程</h1>
      <div style="margin-bottom:8px;">时间：${item.travelTime}</div>
      <div style="margin-bottom:8px;">预算：¥${item.budget} ｜ 预估花费：¥${item.totalCost}</div>
      <div style="margin-bottom:8px;">创建时间：${formatCreatedAt(item.createdAt)}</div>
      <div style="margin-bottom:12px;line-height:1.6;">总结：${plan.summary || ''}</div>
      ${daysHtml}
      ${tips ? `<section style="margin-top:16px;"><div style="font-weight:700;">出行提示</div><ul style="padding-left:18px;">${tips}</ul></section>` : ''}
    </div>
  `
}

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
  stopSse?.()
  stopSse = null
})

onMounted(() => {
  window.addEventListener('beforeunload', onBeforeUnload)
  loadHistory()
  const activeTaskId = localStorage.getItem(ACTIVE_TASK_ID_KEY)
  if (!activeTaskId) {
    return
  }
  statusText.value = '检测到未完成任务，正在恢复...'
  recoverTask(activeTaskId)
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: -50px;
  animation-delay: 5s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  bottom: -50px;
  left: 30%;
  animation-delay: 10s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
  }
}

.page-header {
  text-align: center;
  margin-bottom: 50px;
  position: relative;
  z-index: 1;
}

.icon-wrapper {
  margin-bottom: 20px;
}

.icon {
  font-size: 80px;
  display: inline-block;
}

.page-title {
  font-size: 56px;
  font-weight: 800;
  color: #ffffff;
  margin-bottom: 16px;
  text-shadow: 3px 3px 6px rgba(0, 0, 0, 0.3);
  letter-spacing: 2px;
}

.page-subtitle {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.95);
  margin: 0;
  font-weight: 300;
}

.form-card {
  max-width: 1400px;
  margin: 0 auto;
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.98) !important;
}

.history-card {
  max-width: 1400px;
  margin: 24px auto 0;
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.25);
  background: rgba(255, 255, 255, 0.98) !important;
}

.history-actions {
  display: flex;
  gap: 8px;
}

.form-section {
  margin-bottom: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 16px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s ease;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #667eea;
}

.section-icon {
  font-size: 24px;
  margin-right: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.form-label {
  font-size: 15px;
  font-weight: 500;
  color: #555;
}

.form-hint {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: #888;
}

.custom-input :deep(.ant-input-number),
.custom-input :deep(.ant-picker),
.custom-input :deep(.ant-cascader) {
  border-radius: 12px;
}

.submit-button {
  height: 56px;
  border-radius: 28px;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.cancel-button {
  height: 44px;
  border-radius: 22px;
}

.button-icon {
  margin-right: 8px;
  font-size: 20px;
}

.loading-container {
  text-align: center;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 16px;
  border: 2px dashed #667eea;
}

.loading-status {
  margin-top: 16px;
  color: #667eea;
  font-size: 18px;
  font-weight: 500;
}

.history-header {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.history-city {
  font-weight: 700;
}

.history-time {
  color: #666;
}

.history-cost {
  margin-left: auto;
  color: #1677ff;
  font-weight: 600;
}

.history-meta {
  margin-bottom: 8px;
  color: #666;
  font-size: 13px;
}

.history-summary {
  margin-bottom: 12px;
}

.history-export-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.history-day-title {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.history-tips {
  margin-top: 10px;
}

.history-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.history-page-text {
  color: #666;
  font-size: 13px;
}

.history-jump-input {
  width: 96px;
}
</style>

