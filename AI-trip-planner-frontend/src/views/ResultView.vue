<template>
  <div class="result-container">
    <div class="page-header">
      <a-button class="back-button" size="large" @click="goBack">← 返回首页</a-button>
      <div v-if="result" class="action-buttons">
        <a-button v-if="!editMode" type="primary" :loading="saveLoading" @click="handleSaveTrip">保存行程</a-button>
        <a-button v-if="!editMode" :disabled="isLocked" @click="startEdit">编辑行程</a-button>
        <a-button v-if="editMode" :loading="draftLoading" @click="saveDraftAndExit">保存草稿</a-button>
        <a-button v-if="editMode" @click="cancelEdit">取消编辑</a-button>
        <a-button v-if="!editMode" :loading="exporting" @click="exportAsImage">导出图片</a-button>
        <a-button v-if="!editMode" :loading="exporting" @click="exportAsPdf">导出 PDF</a-button>
      </div>
    </div>

    <a-card v-if="!result" :bordered="false" class="overview-card">
      <a-empty description="未找到行程结果（请从首页重新生成）" />
    </a-card>

    <div v-else class="content-wrapper">
      <div class="main-content">
        <a-card :title="`${result.city} 行程计划`" :bordered="false" class="overview-card">
          <div class="overview-content">
            <div class="info-item">
              <span class="info-label">📅 时间</span>
              <span class="info-value">{{ result.travelTime }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">💰 预算</span>
              <span class="info-value">¥{{ result.budget }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">🧾 总结</span>
              <span class="info-value">{{ result.summary }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">📊 预估总花费</span>
              <span class="info-value">¥{{ result.totalEstimatedCost }}</span>
            </div>
            <div v-if="result.budgetBreakdown" class="budget-breakdown">
              <div class="breakdown-title">💡 预算分配说明</div>
              <div class="breakdown-explain">{{ result.budgetBreakdown.explanation }}</div>
              <div class="breakdown-bars">
                <div class="bar-group">
                  <div class="bar-label">
                    <span>🎯 活动（门票/餐饮/交通）</span>
                    <span>¥{{ result.budgetBreakdown.activityEstimated }} / ¥{{ result.budgetBreakdown.activityBudget }}</span>
                  </div>
                  <a-progress
                    :percent="Math.min(Math.round(result.budgetBreakdown.activityEstimated / result.budgetBreakdown.activityBudget * 100), 100)"
                    :stroke-color="result.budgetBreakdown.activityEstimated > result.budgetBreakdown.activityBudget ? '#ff4d4f' : '#52c41a'"
                    :show-info="true"
                  />
                </div>
                <div v-if="result.budgetBreakdown.accommodationRatio > 0" class="bar-group">
                  <div class="bar-label">
                    <span>🏨 住宿</span>
                    <span>¥{{ result.budgetBreakdown.accommodationEstimated }} / ¥{{ result.budgetBreakdown.accommodationBudget }}</span>
                  </div>
                  <a-progress
                    :percent="Math.min(Math.round(result.budgetBreakdown.accommodationEstimated / result.budgetBreakdown.accommodationBudget * 100), 100)"
                    :stroke-color="result.budgetBreakdown.accommodationEstimated > result.budgetBreakdown.accommodationBudget ? '#ff4d4f' : '#1890ff'"
                    :show-info="true"
                  />
                </div>
              </div>
            </div>

            <div v-if="result.accommodationNote || result.budgetWarning || isOverBudget" class="budget-alerts">
              <a-alert v-if="result.accommodationNote" type="info" show-icon :message="result.accommodationNote" />
              <a-alert v-if="result.budgetWarning" type="warning" show-icon :message="result.budgetWarning" />
              <a-alert
                v-if="isOverBudget"
                type="error"
                show-icon
                :message="`预估总花费（¥${result.totalEstimatedCost}）已超过您设定的总预算（¥${result.budget}），分项价格为估算，实际出行请以订单与现场为准。`"
              />
            </div>
          </div>
        </a-card>

        <a-card title="📅 每日行程（每天一个地图）" :bordered="false" class="days-card">
          <a-collapse accordion>
            <a-collapse-panel v-for="(day, idx) in result.dailyPlans" :key="idx">
              <template #header>
                <div class="day-header">
                  <span class="day-title">{{ day.day }}</span>
                  <span class="day-theme">{{ day.theme }}</span>
                  <span class="day-cost">¥{{ day.estimatedCost }}</span>
                </div>
              </template>

              <a-row :gutter="16">
                <a-col :span="14">
                  <a-list :data-source="day.planItems" item-layout="horizontal">
                    <template #renderItem="{ item }">
                      <a-list-item>
                        <a-list-item-meta>
                          <template #title>
                            <div class="item-title">
                              <a-input v-if="editMode" v-model:value="item.period" placeholder="时段" class="inline-input period-input" />
                              <span v-else class="period">{{ item.period }}</span>
                              <a-input v-if="editMode" v-model:value="item.place" placeholder="地点" class="inline-input place-input" />
                              <span v-else class="place">{{ item.place }}</span>
                              <a-tag v-if="item.mustBookInAdvance" color="warning">需预约</a-tag>
                              <a-checkbox v-if="editMode" v-model:checked="item.mustBookInAdvance">需预约</a-checkbox>
                            </div>
                          </template>
                          <template #description>
                            <div class="item-desc">
                              <div v-if="editMode">
                                <a-input v-model:value="item.activity" placeholder="活动描述" class="inline-input" />
                              </div>
                              <div v-else>🎯 {{ item.activity }}</div>
                              <div v-if="editMode" class="edit-row">
                                <a-input v-model:value="item.transport" placeholder="交通方式" class="inline-input" />
                                <a-input-number v-model:value="item.expectedCost" :min="0" style="width: 140px" />
                              </div>
                              <div v-else>🚗 {{ item.transport }} ｜ 💸 ¥{{ item.expectedCost }}</div>
                            </div>
                          </template>
                        </a-list-item-meta>
                      </a-list-item>
                    </template>
                  </a-list>
                  <a-card v-if="day.stay" size="small" class="stay-card" title="🏨 今晚住宿建议">
                    <div class="stay-name">{{ day.stay.name }}</div>
                    <div class="stay-line">📍 {{ day.stay.address }}</div>
                    <div class="stay-line">
                      💸 参考价：
                      <span v-if="day.stay.pricePerNight != null">¥{{ day.stay.pricePerNight }}/晚</span>
                      <span v-else>待查询</span>
                      <span v-if="day.stay.distanceToLastSpot != null"> ｜ 🚶距末站约 {{ day.stay.distanceToLastSpot }} 米</span>
                    </div>
                    <div v-if="day.stay.reason" class="stay-line">💡 {{ day.stay.reason }}</div>
                    <div class="stay-hint">
                      住宿参考价按总预算的 35%、以「住宿夜数 = 行程天数 − 1」均摊估算，最低 ¥100/晚；最后一天不含住宿推荐。
                    </div>
                  </a-card>
                </a-col>
                <a-col :span="10">
                  <DayMap
                    :city="result.city"
                    :plan-items="day.planItems"
                    :editable="editMode && !isLocked"
                    :pins="dayPins[idx] || []"
                    @update:pins="(pins) => onUpdatePins(idx, pins)"
                  />
                  <div v-if="!amapConfigured" class="map-hint">
                    未配置高德 Key，地图仅占位显示。请在 `.env` 配置 `VITE_AMAP_WEB_JS_KEY`。
                  </div>
                  <div class="map-hint">
                    地图交互：点击已有标点可取消；点击空白处可新增标点。
                  </div>
                </a-col>
              </a-row>
            </a-collapse-panel>
          </a-collapse>
        </a-card>

        <a-card v-if="result.tips?.length" title="✅ 出行提示" :bordered="false" class="tips-card">
          <a-list :data-source="result.tips">
            <template #renderItem="{ item }">
              <a-list-item>• {{ item }}</a-list-item>
            </template>
          </a-list>
        </a-card>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import DayMap from '@/components/DayMap.vue'
import { saveTrip, saveTripDraft } from '@/services/api'
import { createExportContainer, exportElementAsImage, exportElementAsPdf } from '@/utils/exporter'
import type { MapPin, TripPlanResult } from '@/types'

const route = useRoute()
const router = useRouter()
const result = ref<TripPlanResult | null>(null)
const sourceResult = ref<TripPlanResult | null>(null)
const editMode = ref(false)
const draftLoading = ref(false)
const saveLoading = ref(false)
const isLocked = ref(false)
const dayPins = ref<Record<number, MapPin[]>>({})
const exporting = ref(false)

const amapConfigured = computed(() => {
  const key = import.meta.env.VITE_AMAP_WEB_JS_KEY
  return !!key && !String(key).includes('your_amap')
})

const isOverBudget = computed(() => {
  const r = result.value
  if (!r) return false
  return Number(r.totalEstimatedCost) > Number(r.budget)
})

const taskId = route.params.taskId as string
const lockKey = `tripLocked:${taskId}`
const pinsKey = `tripPins:${taskId}`
const cached = sessionStorage.getItem(`tripResult:${taskId}`)
if (cached) {
  try {
    const parsed = JSON.parse(cached) as TripPlanResult
    result.value = JSON.parse(JSON.stringify(parsed))
    sourceResult.value = JSON.parse(JSON.stringify(parsed))
  } catch {
    result.value = null
  }
}
const cachedPins = sessionStorage.getItem(pinsKey)
if (cachedPins) {
  try {
    dayPins.value = JSON.parse(cachedPins) as Record<number, MapPin[]>
  } catch {
    dayPins.value = {}
  }
}
isLocked.value = localStorage.getItem(lockKey) === '1'

const goBack = () => router.push('/')

function deepClone<T>(val: T): T {
  return JSON.parse(JSON.stringify(val))
}

function startEdit() {
  if (isLocked.value) {
    message.warning('该行程已保存，后续不可修改')
    return
  }
  if (!result.value) return
  sourceResult.value = deepClone(result.value)
  editMode.value = true
}

function cancelEdit() {
  if (!sourceResult.value) {
    editMode.value = false
    return
  }
  result.value = deepClone(sourceResult.value)
  editMode.value = false
}

function onUpdatePins(dayIndex: number, pins: MapPin[]) {
  dayPins.value = {
    ...dayPins.value,
    [dayIndex]: pins
  }
  sessionStorage.setItem(pinsKey, JSON.stringify(dayPins.value))
}

async function saveDraftAndExit() {
  if (!result.value) return
  draftLoading.value = true
  try {
    const payload = deepClone(result.value)
    let actTotal = 0
    let accTotal = 0
    payload.totalEstimatedCost = payload.dailyPlans.reduce((sum, day) => {
      const dayCost = day.planItems.reduce((s, p) => s + Number(p.expectedCost || 0), 0)
      const stayCost = day.stay?.pricePerNight ?? 0
      actTotal += dayCost
      accTotal += stayCost
      day.estimatedCost = dayCost + stayCost
      return sum + day.estimatedCost
    }, 0)
    if (payload.budgetBreakdown) {
      payload.budgetBreakdown.activityEstimated = actTotal
      payload.budgetBreakdown.accommodationEstimated = accTotal
    }
    const msg = await saveTripDraft(taskId, payload)
    result.value = payload
    sourceResult.value = deepClone(payload)
    sessionStorage.setItem(`tripResult:${taskId}`, JSON.stringify(payload))
    message.success(msg)
    editMode.value = false
  } catch (e: any) {
    message.error(e?.message || '保存草稿失败')
  } finally {
    draftLoading.value = false
  }
}

async function handleSaveTrip() {
  saveLoading.value = true
  try {
    const msg = await saveTrip(taskId)
    message.success(msg)
    isLocked.value = true
    editMode.value = false
    localStorage.setItem(lockKey, '1')
  } catch (e: any) {
    message.error(e?.message || '保存行程失败')
  } finally {
    saveLoading.value = false
  }
}

function buildTripExportHtml() {
  if (!result.value) return ''
  const r = result.value
  const daysHtml = r.dailyPlans
    .map((day) => {
      const items = day.planItems
        .map(
          (it) =>
            `<li style="margin:6px 0;line-height:1.6;">${it.period} ｜ ${it.place} ｜ ${it.activity} ｜ ${it.transport} ｜ ¥${it.expectedCost}</li>`
        )
        .join('')
      const stay = day.stay
        ? `<div style="margin-top:10px;padding:10px;background:#f8fbff;border:1px solid #dbeafe;border-radius:6px;line-height:1.7;">
             <div style="font-weight:700;">今晚住宿：${day.stay.name}</div>
             <div>地址：${day.stay.address}</div>
             <div>参考价格：${day.stay.pricePerNight != null ? `¥${day.stay.pricePerNight}/晚` : '待查询'}</div>
             <div>${day.stay.reason || ''}</div>
           </div>`
        : ''
      return `
        <section style="margin:14px 0;padding:12px;border:1px solid #e8e8e8;border-radius:8px;">
          <div style="font-weight:700;margin-bottom:8px;">${day.day} · ${day.theme} · ¥${day.estimatedCost}</div>
          <ul style="padding-left:18px;margin:0;">${items}</ul>
          ${stay}
        </section>
      `
    })
    .join('')
  const tips = (r.tips || []).map((t) => `<li style="margin:4px 0;">${t}</li>`).join('')
  const over = Number(r.totalEstimatedCost) > Number(r.budget)
  const accNoteHtml = r.accommodationNote
    ? `<div style="margin:8px 0;padding:10px;background:#e6f7ff;border:1px solid #91d5ff;border-radius:6px;">${r.accommodationNote}</div>`
    : ''
  const warnHtml = r.budgetWarning
    ? `<div style="margin:8px 0;padding:10px;background:#fffbe6;border:1px solid #ffe58f;border-radius:6px;">${r.budgetWarning}</div>`
    : ''
  const overHtml = over
    ? `<div style="margin:8px 0;padding:10px;background:#fff2f0;border:1px solid #ffccc7;border-radius:6px;">预估总花费（¥${r.totalEstimatedCost}）已超过您设定的总预算（¥${r.budget}），分项价格为估算，实际出行请以订单与现场为准。</div>`
    : ''
  return `
    <div style="font-family:Arial,'Microsoft YaHei',sans-serif;color:#222;">
      <h1 style="margin:0 0 10px 0;">${r.city} 行程计划</h1>
      <div style="margin-bottom:8px;">时间：${r.travelTime}</div>
      <div style="margin-bottom:8px;">预算：¥${r.budget}</div>
      <div style="margin-bottom:8px;">预估总花费：¥${r.totalEstimatedCost}</div>
      ${r.budgetBreakdown ? `<div style="margin:10px 0;padding:12px;background:linear-gradient(135deg,#f0f5ff,#e6fffb);border:1px solid #d6e4ff;border-radius:8px;line-height:1.8;">
        <div style="font-weight:700;color:#1890ff;margin-bottom:4px;">💡 预算分配说明</div>
        <div style="font-size:13px;color:#555;">${r.budgetBreakdown.explanation}</div>
        <div style="margin-top:6px;font-size:13px;color:#444;">🎯 活动：¥${r.budgetBreakdown.activityEstimated} / ¥${r.budgetBreakdown.activityBudget}${r.budgetBreakdown.accommodationRatio > 0 ? ` ｜ 🏨 住宿：¥${r.budgetBreakdown.accommodationEstimated} / ¥${r.budgetBreakdown.accommodationBudget}` : ''}</div>
      </div>` : ''}
      ${accNoteHtml}${warnHtml}${overHtml}
      <div style="margin-bottom:12px;line-height:1.6;">总结：${r.summary || ''}</div>
      ${daysHtml}
      ${tips ? `<section style="margin-top:16px;"><div style="font-weight:700;">出行提示</div><ul style="padding-left:18px;">${tips}</ul></section>` : ''}
    </div>
  `
}

async function exportAsImage() {
  if (!result.value) return
  exporting.value = true
  try {
    const wrapper = createExportContainer(buildTripExportHtml())
    try {
      await exportElementAsImage(wrapper.el, `${result.value.city}_行程`)
    } finally {
      wrapper.destroy()
    }
    message.success('已导出图片')
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '导出图片失败')
  } finally {
    exporting.value = false
  }
}

async function exportAsPdf() {
  if (!result.value) return
  exporting.value = true
  try {
    const wrapper = createExportContainer(buildTripExportHtml())
    try {
      await exportElementAsPdf(wrapper.el, `${result.value.city}_行程`)
    } finally {
      wrapper.destroy()
    }
    message.success('已导出 PDF')
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '导出 PDF 失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.result-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 40px 20px;
}

.page-header {
  max-width: 1400px;
  margin: 0 auto 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.back-button {
  border-radius: 8px;
  font-weight: 500;
}

.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  gap: 24px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.overview-card,
.days-card,
.tips-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.overview-content {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.info-value {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
}

.budget-breakdown {
  grid-column: 1 / -1;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6fffb 100%);
  border: 1px solid #d6e4ff;
  border-radius: 8px;
  padding: 16px;
}

.breakdown-title {
  font-size: 14px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 6px;
}

.breakdown-explain {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 12px;
}

.breakdown-bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bar-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #444;
}

.budget-alerts {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-header {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.day-title {
  font-weight: 700;
}

.day-theme {
  color: #666;
}

.day-cost {
  margin-left: auto;
  font-weight: 700;
  color: #1890ff;
}

.item-title {
  display: flex;
  gap: 10px;
  align-items: center;
}

.period {
  font-weight: 700;
  color: #764ba2;
}

.place {
  font-weight: 600;
}

.item-desc {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.edit-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.inline-input {
  min-width: 120px;
}

.period-input {
  width: 110px;
}

.place-input {
  width: 180px;
}

.map-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.stay-card {
  margin-top: 12px;
  border-radius: 8px;
}

.stay-name {
  font-weight: 700;
  margin-bottom: 4px;
}

.stay-line {
  color: #555;
  line-height: 1.8;
}

.stay-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

</style>

