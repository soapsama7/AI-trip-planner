import axios from 'axios'
import type {
  ApiMessageResponse,
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

export async function startTripTask(payload: TripGenerateRequest): Promise<TaskStartResponse> {
  const res = await apiClient.post<TaskStartResponse>('/api/trips/plan', payload)
  return res.data
}

export async function queryTaskStatus(taskId: string): Promise<TaskStatusResponse> {
  const res = await apiClient.get<TaskStatusResponse>(`/api/trips/tasks/${encodeURIComponent(taskId)}`)
  return res.data
}

export async function cancelTripTask(taskId: string): Promise<void> {
  await apiClient.post(`/api/trips/tasks/${encodeURIComponent(taskId)}/cancel`)
}

export async function queryTripHistory(page = 1, size = 10): Promise<TripHistoryPageResponse> {
  const res = await apiClient.get<TripHistoryPageResponse>('/api/trips/history', {
    params: { page, size }
  })
  return res.data
}

export async function saveTripDraft(taskId: string, payload: TripPlanResult): Promise<ApiMessageResponse> {
  const res = await apiClient.post<ApiMessageResponse>(`/api/trips/tasks/${encodeURIComponent(taskId)}/draft`, payload)
  return res.data
}

export async function saveTrip(taskId: string): Promise<ApiMessageResponse> {
  const res = await apiClient.post<ApiMessageResponse>(`/api/trips/tasks/${encodeURIComponent(taskId)}/save`)
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
