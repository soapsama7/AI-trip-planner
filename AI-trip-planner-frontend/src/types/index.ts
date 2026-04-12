export type TripGenerateRequest = {
  city: string
  travelTime: string
  budget: number
}

export type TaskStartResponse = {
  taskId: string
}

export type TripPlanResult = {
  city: string
  travelTime: string
  budget: number
  summary: string
  totalEstimatedCost: number
  dailyPlans: DailyPlan[]
  tips: string[]
  /** 预算校验软警告（如日均偏低）；无则为 null/undefined */
  budgetWarning?: string | null
  /** 如单日往返未安排住宿时的说明 */
  accommodationNote?: string | null
}

export type DailyPlan = {
  day: string
  theme: string
  estimatedCost: number
  planItems: PlanItem[]
  stay?: Stay
}

export type Stay = {
  name: string
  address: string
  pricePerNight?: number | null
  distanceToLastSpot?: number | null
  reason?: string
}

export type PlanItem = {
  period: string
  place: string
  activity: string
  transport: string
  expectedCost: number
  mustBookInAdvance: boolean
}

export type MapPin = {
  id: string
  lng: number
  lat: number
  title?: string
}

export type TripStreamEvent = {
  taskId: string
  type: 'PROGRESS' | 'DONE' | 'ERROR'
  stage: string
  stepIndex: number
  totalSteps: number
  progress: number
  message: string
  timestamp?: string
  result?: TripPlanResult
}

export type TaskStatusResponse = {
  taskId: string
  progress: number
  error?: string | null
  result?: TripPlanResult | null
}

export type TripHistoryItem = {
  id: number
  taskId: string
  city: string
  travelTime: string
  budget: number
  totalCost: number
  plan: TripPlanResult | null
  createdAt: string
}

export type TripHistoryPageResponse = {
  page: number
  size: number
  total: number
  totalPages: number
  records: TripHistoryItem[]
}

export type ApiMessageResponse = {
  message: string
}

