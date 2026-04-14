import axios from 'axios'
import type {
  ApiResult,
  TaskStartResponse,
  TaskStatusResponse,
  TripGenerateRequest,
  TripPlanResult,
  TripHistoryPageResponse,
  TripStreamEvent
} from '@/types'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 600000,
  headers: { 'Content-Type': 'application/json' }
})

/**
 * 响应拦截器：自动解包后端 Result<T> 结构。
 * 成功时提取 data 字段；失败时统一抛出 Error(message)。
 */
apiClient.interceptors.response.use(
  (res) => {
    const body = res.data as ApiResult<unknown>
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        res.data = body.data
      } else {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
    }
    return res
  },
  (error) => {
    const body = error?.response?.data
    const msg = body?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export async function startTripTask(payload: TripGenerateRequest): Promise<TaskStartResponse> {
  const res = await apiClient.post<TaskStartResponse>('/api/trips/plan', payload)
  return res.data
}

export async function queryTaskStatus(taskId: string): Promise<TaskStatusResponse> {
  const res = await apiClient.get<TaskStatusResponse>(`/api/trips/tasks/${encodeURIComponent(taskId)}`)
  return res.data
}

export async function cancelTripTask(taskId: string): Promise<string> {
  const res = await apiClient.post<string>(`/api/trips/tasks/${encodeURIComponent(taskId)}/cancel`)
  return res.data
}

export async function queryTripHistory(page = 1, size = 10): Promise<TripHistoryPageResponse> {
  const res = await apiClient.get<TripHistoryPageResponse>('/api/trips/history', {
    params: { page, size }
  })
  return res.data
}

export async function saveTripDraft(taskId: string, payload: TripPlanResult): Promise<string> {
  const res = await apiClient.post<string>(`/api/trips/tasks/${encodeURIComponent(taskId)}/draft`, payload)
  return res.data
}

export async function saveTrip(taskId: string): Promise<string> {
  const res = await apiClient.post<string>(`/api/trips/tasks/${encodeURIComponent(taskId)}/save`)
  return res.data
}

export function subscribeTripEvents(
  taskId: string,
  onEvent: (e: TripStreamEvent) => void,
  onError?: (err: any) => void
) {
  const url = `${API_BASE_URL}/api/trips/tasks/${encodeURIComponent(taskId)}/events`
  const es = new EventSource(url)
  let closed = false

  es.onmessage = (msg) => {
    try {
      const data = JSON.parse(msg.data) as TripStreamEvent
      onEvent(data)
      if (data.type === 'DONE' || data.type === 'ERROR') {
        closed = true
        es.close()
      }
    } catch (e) {
      onError?.(e)
      closed = true
      es.close()
    }
  }

  es.onerror = () => {
    if (closed) return
    if (es.readyState === EventSource.CLOSED) {
      onError?.(new Error('SSE 连接已关闭'))
      closed = true
    }
  }

  return () => {
    closed = true
    es.close()
  }
}
