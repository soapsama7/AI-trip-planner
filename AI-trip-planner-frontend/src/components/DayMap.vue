<template>
  <div class="day-map">
    <div ref="containerRef" class="map-container"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { bootstrapAmapSecurity } from '@/utils/amapSecurity'
import { message } from 'ant-design-vue'
import type { MapPin, PlanItem } from '@/types'

const props = defineProps<{
  city: string
  planItems: PlanItem[]
  editable?: boolean
  pins?: MapPin[]
}>()
const emit = defineEmits<{
  (e: 'update:pins', pins: MapPin[]): void
}>()

const containerRef = ref<HTMLElement | null>(null)
let map: any = null
let markers: any[] = []
let mapClickHandler: ((e: any) => void) | null = null
let internalPins: MapPin[] = []

function uniquePlaces(items: PlanItem[]) {
  const seen = new Set<string>()
  const result: string[] = []
  for (const it of items) {
    const p = (it.place || '').trim()
    if (!p) continue
    if (seen.has(p)) continue
    seen.add(p)
    result.push(p)
  }
  return result.slice(0, 8)
}

async function initMap() {
  const key = import.meta.env.VITE_AMAP_WEB_JS_KEY
  if (!key || String(key).includes('your_amap')) {
    return
  }
  bootstrapAmapSecurity()

  const el = containerRef.value
  if (!el) return

  const AMap = await AMapLoader.load({
    key,
    version: '2.0',
    plugins: ['AMap.PlaceSearch', 'AMap.Geocoder', 'AMap.Marker']
  })

  map = new AMap.Map(el, {
    zoom: 11,
    viewMode: '2D'
  })

  if (props.pins && props.pins.length > 0) {
    internalPins = [...props.pins]
    renderPins(AMap)
  } else {
    await buildPinsFromPlaces(AMap)
  }
  bindMapClick(AMap)
}

async function buildPinsFromPlaces(AMap: any) {
  if (!map) return
  const places = uniquePlaces(props.planItems)
  if (places.length === 0) {
    internalPins = []
    renderPins(AMap)
    return
  }

  const placeSearch = new AMap.PlaceSearch({
    city: props.city,
    citylimit: true,
    pageSize: 1
  })

  const nextPins: MapPin[] = []

  for (const place of places) {
    const pos = await new Promise<[number, number] | null>((resolve) => {
      placeSearch.search(place, (status: string, result: any) => {
        if (status !== 'complete') return resolve(null)
        const poi = result?.poiList?.pois?.[0]
        const loc = poi?.location
        if (!loc) return resolve(null)
        resolve([Number(loc.lng), Number(loc.lat)])
      })
    })
    if (!pos) continue
    nextPins.push({
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      lng: pos[0],
      lat: pos[1],
      title: place
    })
  }
  internalPins = nextPins
  emit('update:pins', [...internalPins])
  renderPins(AMap)
}

function renderPins(AMap: any) {
  if (!map) return
  markers.forEach((m) => map.remove(m))
  markers = []
  for (const pin of internalPins) {
    const marker = new AMap.Marker({
      position: [pin.lng, pin.lat],
      title: pin.title || '自定义标点'
    })
    marker.on('click', () => {
      if (!props.editable) return
      internalPins = internalPins.filter((p) => p.id !== pin.id)
      emit('update:pins', [...internalPins])
      renderPins(AMap)
    })
    markers.push(marker)
  }
  if (markers.length > 0) {
    map.add(markers)
    map.setFitView(markers)
  }
}

function bindMapClick(AMap: any) {
  if (!map) return
  if (mapClickHandler) {
    map.off('click', mapClickHandler)
  }
  mapClickHandler = (e: any) => {
    if (!props.editable) return
    const lng = Number(e?.lnglat?.getLng?.())
    const lat = Number(e?.lnglat?.getLat?.())
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return
    internalPins = [
      ...internalPins,
      {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        lng,
        lat,
        title: '自定义标点'
      }
    ]
    emit('update:pins', [...internalPins])
    renderPins(AMap)
  }
  map.on('click', mapClickHandler)
}

watch(
  () => props.planItems,
  async () => {
    try {
      if (!map) return
      const AMap = (window as any).AMap
      if (!AMap) return
      if (!props.pins || props.pins.length === 0) {
        await buildPinsFromPlaces(AMap)
      }
    } catch {
      // ignore
    }
  },
  { deep: true }
)

watch(
  () => props.pins,
  (pins) => {
    const AMap = (window as any).AMap
    if (!map || !AMap || !pins) return
    internalPins = [...pins]
    renderPins(AMap)
  },
  { deep: true }
)

onMounted(async () => {
  try {
    await initMap()
  } catch (e) {
    message.warning('地图加载失败，请检查高德 Key/安全密钥/Referer 设置')
  }
})

onUnmounted(() => {
  try {
    if (mapClickHandler) {
      map?.off?.('click', mapClickHandler)
    }
    map?.destroy?.()
  } catch {
    // ignore
  }
  map = null
})
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 320px;
  border-radius: 12px;
  overflow: hidden;
  background: #eef2f7;
}
</style>

