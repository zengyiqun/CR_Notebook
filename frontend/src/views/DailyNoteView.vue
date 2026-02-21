<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useDailyNoteStore } from '@/stores/dailyNoteStore'
import { useTaskStore } from '@/stores/taskStore'
import TipTapEditor from '@/components/editor/TipTapEditor.vue'
import type { DailyNote } from '@/types'

const dailyNoteStore = useDailyNoteStore()
const taskStore = useTaskStore()
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const dailyNote = ref<DailyNote | null>(null)
const loading = ref(false)
const editorWrapperRef = ref<HTMLElement | null>(null)
const noteAreaRef = ref<HTMLElement | null>(null)

const todayTasks = computed(() => taskStore.todayTasks)

const isEditing = ref(false)
const saveStatus = ref<'saved' | 'saving' | 'unsaved'>('saved')
const pendingContent = ref<string | null>(null)
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

// ── Weather ──
interface WeatherInfo {
  temp: string
  desc: string
  icon: string
  city: string
  humidity: string
  wind: string
}
const weather = ref<WeatherInfo | null>(null)
const weatherLoading = ref(false)
const weatherError = ref('')

const weatherIcons: Record<string, string> = {
  '晴': '☀️', 'Sunny': '☀️', 'Clear': '🌙',
  '多云': '⛅', 'Partly cloudy': '⛅', 'Cloudy': '☁️', '阴': '☁️',
  '雨': '🌧️', 'Rain': '🌧️', 'Light rain': '🌦️', '小雨': '🌦️', '中雨': '🌧️', '大雨': '🌧️',
  '雷': '⛈️', 'Thunder': '⛈️', '雷阵雨': '⛈️',
  '雪': '🌨️', 'Snow': '🌨️', '小雪': '🌨️', '大雪': '❄️',
  '雾': '🌫️', 'Fog': '🌫️', 'Mist': '🌫️', '霾': '🌫️',
}

function getWeatherIcon(desc: string): string {
  for (const [key, icon] of Object.entries(weatherIcons)) {
    if (desc.includes(key)) return icon
  }
  return '🌤️'
}

async function reverseGeocode(lat: number, lon: number): Promise<string> {
  try {
    const resp = await fetch(
      `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=zh&zoom=12`,
      { headers: { 'User-Agent': 'CRNotebook/1.0' } }
    )
    if (!resp.ok) return ''
    const data = await resp.json()
    const addr = data.address
    if (!addr) return ''
    const city = addr.city || addr.town || addr.county || ''
    const district = addr.suburb || addr.district || ''
    if (city && district && district !== city) return `${city} ${district}`
    return city || data.display_name?.split(',')[0] || ''
  } catch {
    return ''
  }
}

async function fetchWeather() {
  weatherLoading.value = true
  weatherError.value = ''
  try {
    const pos = await new Promise<GeolocationPosition>((resolve, reject) =>
      navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 8000 })
    )
    const { latitude, longitude } = pos.coords
    const [weatherResp, cityName] = await Promise.all([
      fetch(`https://wttr.in/${latitude},${longitude}?format=j1&lang=zh`),
      reverseGeocode(latitude, longitude),
    ])
    if (!weatherResp.ok) throw new Error('天气服务不可用')
    const data = await weatherResp.json()
    const current = data.current_condition?.[0]
    if (current) {
      const desc = current.lang_zh?.[0]?.value || current.weatherDesc?.[0]?.value || ''
      weather.value = {
        temp: current.temp_C + '°C',
        desc,
        icon: getWeatherIcon(desc),
        city: cityName || '',
        humidity: current.humidity + '%',
        wind: current.windspeedKmph + ' km/h',
      }
    }
  } catch (e: any) {
    if (e?.code === 1) {
      weatherError.value = '需要位置权限获取天气'
    } else {
      weatherError.value = '天气获取失败'
    }
  } finally {
    weatherLoading.value = false
  }
}

// ── Mood ──
const moodOptions = [
  { key: 'happy', emoji: '😊', label: '开心' },
  { key: 'calm', emoji: '😌', label: '平静' },
  { key: 'sad', emoji: '😢', label: '难过' },
  { key: 'angry', emoji: '😤', label: '烦躁' },
  { key: 'grateful', emoji: '🤗', label: '感恩' },
  { key: 'tired', emoji: '😴', label: '疲惫' },
  { key: 'thinking', emoji: '🤔', label: '思考' },
  { key: 'love', emoji: '🥰', label: '幸福' },
]

const currentMood = ref('')

function selectMood(key: string) {
  if (currentMood.value === key) {
    currentMood.value = ''
  } else {
    currentMood.value = key
  }
  saveMoodAndWeather()
}

function saveMoodAndWeather() {
  const payload: { mood?: string; weather?: string } = {}
  payload.mood = currentMood.value || ''
  if (weather.value) {
    payload.weather = JSON.stringify(weather.value)
  }
  dailyNoteStore.updateDailyNote(selectedDate.value, payload)
}

// ── Calendar ──
const calMonth = ref(new Date())

const calMonthLabel = computed(() =>
  calMonth.value.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
)

const calWeekdays = ['日', '一', '二', '三', '四', '五', '六']

const calDays = computed(() => {
  const y = calMonth.value.getFullYear()
  const m = calMonth.value.getMonth()
  const lastDay = new Date(y, m + 1, 0)
  const startOffset = new Date(y, m, 1).getDay()
  const today = new Date().toISOString().slice(0, 10)
  const days: { date: string; day: number; inMonth: boolean; isToday: boolean; isSelected: boolean; hasRecord: boolean }[] = []

  const prevLast = new Date(y, m, 0).getDate()
  for (let i = startOffset - 1; i >= 0; i--) {
    const d = new Date(y, m - 1, prevLast - i)
    const ds = d.toISOString().slice(0, 10)
    days.push({ date: ds, day: prevLast - i, inMonth: false, isToday: ds === today, isSelected: ds === selectedDate.value, hasRecord: dailyNoteStore.datesWithContent.has(ds) })
  }
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dt = new Date(y, m, d)
    const ds = dt.toISOString().slice(0, 10)
    days.push({ date: ds, day: d, inMonth: true, isToday: ds === today, isSelected: ds === selectedDate.value, hasRecord: dailyNoteStore.datesWithContent.has(ds) })
  }
  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    const d = new Date(y, m + 1, i)
    const ds = d.toISOString().slice(0, 10)
    days.push({ date: ds, day: i, inMonth: false, isToday: ds === today, isSelected: ds === selectedDate.value, hasRecord: dailyNoteStore.datesWithContent.has(ds) })
  }
  return days
})

function loadCalendarDates() {
  const y = calMonth.value.getFullYear()
  const m = calMonth.value.getMonth()
  const from = new Date(y, m - 1, 1).toISOString().slice(0, 10)
  const to = new Date(y, m + 2, 0).toISOString().slice(0, 10)
  dailyNoteStore.fetchDatesWithContent(from, to)
}

function calPrevMonth() {
  calMonth.value = new Date(calMonth.value.getFullYear(), calMonth.value.getMonth() - 1, 1)
  loadCalendarDates()
}

function calNextMonth() {
  calMonth.value = new Date(calMonth.value.getFullYear(), calMonth.value.getMonth() + 1, 1)
  loadCalendarDates()
}

function calSelectDate(date: string) {
  selectedDate.value = date
}

// ── Note loading / saving ──
async function loadDailyNote() {
  loading.value = true
  try {
    dailyNote.value = await dailyNoteStore.getOrCreate(selectedDate.value)
    isEditing.value = !dailyNote.value.content
    saveStatus.value = 'saved'
    pendingContent.value = null
    currentMood.value = dailyNote.value.mood || ''
    if (dailyNote.value.weather) {
      try { weather.value = JSON.parse(dailyNote.value.weather) } catch { /* ignore */ }
    }
  } finally {
    loading.value = false
  }
}

function doSave(content: string) {
  saveStatus.value = 'saving'
  dailyNoteStore.updateDailyNote(selectedDate.value, { content }).then(() => {
    saveStatus.value = 'saved'
    pendingContent.value = null
    if (content) {
      dailyNoteStore.datesWithContent.add(selectedDate.value)
    } else {
      dailyNoteStore.datesWithContent.delete(selectedDate.value)
    }
  })
  if (dailyNote.value) {
    dailyNote.value.content = content
  }
}

function onContentUpdate(content: string) {
  pendingContent.value = content
  saveStatus.value = 'unsaved'
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => doSave(content), 1500)
}

function manualSave() {
  if (pendingContent.value !== null) {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    doSave(pendingContent.value)
  }
}

function enterEditMode() {
  isEditing.value = true
}

function exitEditMode() {
  if (!isEditing.value) return
  if (pendingContent.value !== null) {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    doSave(pendingContent.value)
  }
  isEditing.value = false
}

watch(selectedDate, () => {
  if (pendingContent.value !== null) {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    doSave(pendingContent.value)
  }
  isEditing.value = false
  loadDailyNote()
  const d = new Date(selectedDate.value)
  calMonth.value = new Date(d.getFullYear(), d.getMonth(), 1)
})

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    manualSave()
  }
}

function onDocDblclick(e: MouseEvent) {
  if (!isEditing.value) return
  if (noteAreaRef.value?.contains(e.target as Node)) return
  exitEditMode()
}

onMounted(() => {
  taskStore.fetchTasks()
  loadDailyNote()
  loadCalendarDates()
  fetchWeather()
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('dblclick', onDocDblclick)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('dblclick', onDocDblclick)
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  if (pendingContent.value !== null) {
    doSave(pendingContent.value)
  }
})

function goToday() {
  selectedDate.value = new Date().toISOString().slice(0, 10)
}

function goPrev() {
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() - 1)
  selectedDate.value = d.toISOString().slice(0, 10)
}

function goNext() {
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() + 1)
  selectedDate.value = d.toISOString().slice(0, 10)
}

const displayDate = computed(() => {
  const d = new Date(selectedDate.value)
  const today = new Date().toISOString().slice(0, 10)
  const weekday = d.toLocaleDateString('zh-CN', { weekday: 'long' })
  const dayNum = d.getDate()
  const monthStr = d.toLocaleDateString('zh-CN', { month: 'short' })
  const isToday = selectedDate.value === today
  return { weekday, isToday, dayNum, monthStr }
})

const moodDisplay = computed(() => moodOptions.find(m => m.key === currentMood.value))
</script>

<template>
  <div class="h-full flex bg-[var(--color-craft-surface)] overflow-hidden">
    <!-- Left: Editor area -->
    <div ref="noteAreaRef" class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- Header -->
      <div class="px-6 pt-6 pb-3 shrink-0">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex flex-col items-center justify-center text-white shadow-sm">
              <span class="text-lg font-bold leading-none">{{ displayDate.dayNum }}</span>
              <span class="text-[9px] font-medium opacity-80">{{ displayDate.monthStr }}</span>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h1 class="text-lg font-bold text-[var(--color-craft-text)]">每日笔记</h1>
                <span
                  v-if="displayDate.isToday"
                  class="px-1.5 py-0.5 text-[10px] rounded-full bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-semibold"
                >今天</span>
              </div>
              <p class="text-xs text-[var(--color-craft-text-secondary)]">
                {{ displayDate.weekday }}
                <template v-if="moodDisplay">· {{ moodDisplay.emoji }} {{ moodDisplay.label }}</template>
                <template v-if="weather">· {{ weather.icon }} {{ weather.temp }}</template>
              </p>
            </div>
          </div>
          <div class="flex items-center gap-1.5">
            <template v-if="isEditing && dailyNote">
              <span class="text-[11px] px-2 py-0.5 rounded-full"
                :class="saveStatus === 'saved' ? 'text-[var(--color-craft-success)] bg-emerald-50 dark:bg-emerald-900/20' : saveStatus === 'saving' ? 'text-amber-500 bg-amber-50 dark:bg-amber-900/20' : 'text-[var(--color-craft-text-secondary)] bg-[var(--color-craft-hover)]'"
              >
                {{ saveStatus === 'saved' ? '已保存' : saveStatus === 'saving' ? '保存中' : '未保存' }}
              </span>
              <button
                @click="manualSave"
                :disabled="saveStatus === 'saved'"
                class="h-7 px-2.5 flex items-center gap-1 rounded-lg text-xs font-medium transition-all"
                :class="saveStatus !== 'saved' ? 'bg-[var(--color-craft-accent)] text-white hover:opacity-90' : 'bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] cursor-default'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
                保存
              </button>
              <button
                @click="exitEditMode"
                class="h-7 px-2.5 flex items-center gap-1 rounded-lg text-xs font-medium border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-all"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 6L9 17l-5-5"/></svg>
                完成
              </button>
            </template>
            <button
              v-if="!isEditing && dailyNote"
              @click="enterEditMode"
              class="h-7 px-3 flex items-center gap-1.5 rounded-lg text-xs font-medium bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
              编辑
            </button>
            <span class="w-px h-5 bg-[var(--color-craft-border)]"></span>
            <button @click="goPrev" class="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M15 18l-6-6 6-6"/></svg>
            </button>
            <button @click="goToday" class="px-2.5 py-1 text-[11px] font-medium rounded-lg border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">今天</button>
            <button @click="goNext" class="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6"/></svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Editor -->
      <div
        v-if="dailyNote && !loading"
        ref="editorWrapperRef"
        class="flex-1 overflow-hidden mx-6 mb-6 rounded-2xl border border-[var(--color-craft-border)] shadow-sm"
        @dblclick="!isEditing && enterEditMode()"
      >
        <TipTapEditor
          :key="selectedDate"
          :model-value="dailyNote.content"
          :editable="isEditing"
          placeholder="记录今天的想法、灵感、收获..."
          @update:model-value="onContentUpdate"
        />
      </div>
      <div v-else class="flex-1 flex items-center justify-center">
        <div class="text-sm text-[var(--color-craft-text-secondary)]">加载中...</div>
      </div>
    </div>

    <!-- Right sidebar -->
    <div class="w-72 shrink-0 border-l border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] flex flex-col overflow-y-auto">
      <!-- Calendar -->
      <div class="p-4 border-b border-[var(--color-craft-border)]">
        <div class="flex items-center justify-between mb-3">
          <button @click="calPrevMonth" class="w-6 h-6 flex items-center justify-center rounded-md hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <span class="text-xs font-semibold text-[var(--color-craft-text)]">{{ calMonthLabel }}</span>
          <button @click="calNextMonth" class="w-6 h-6 flex items-center justify-center rounded-md hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
        <div class="grid grid-cols-7 mb-0.5">
          <div v-for="wd in calWeekdays" :key="wd" class="text-center text-[9px] font-semibold text-[var(--color-craft-text-secondary)] py-0.5">{{ wd }}</div>
        </div>
        <div class="grid grid-cols-7 gap-px">
          <button
            v-for="(d, i) in calDays"
            :key="i"
            @click="calSelectDate(d.date)"
            class="w-full aspect-square flex flex-col items-center justify-center rounded-md text-[11px] font-medium transition-all duration-100 relative"
            :class="[
              d.isSelected
                ? 'bg-[var(--color-craft-accent)] text-white shadow-sm'
                : d.isToday
                  ? 'bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-bold'
                  : d.inMonth
                    ? 'text-[var(--color-craft-text)] hover:bg-[var(--color-craft-hover)]'
                    : 'text-[var(--color-craft-text-secondary)] opacity-30'
            ]"
          >
            {{ d.day }}
            <span
              v-if="d.hasRecord"
              class="absolute bottom-0.5 w-1 h-1 rounded-full"
              :class="d.isSelected ? 'bg-white/70' : 'bg-[var(--color-craft-accent)]'"
            ></span>
          </button>
        </div>
      </div>

      <!-- Weather -->
      <div class="p-4 border-b border-[var(--color-craft-border)]">
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-3">今日天气</h3>
        <div v-if="weatherLoading" class="text-xs text-[var(--color-craft-text-secondary)] text-center py-2">
          获取中...
        </div>
        <div v-else-if="weatherError" class="text-center py-2">
          <p class="text-xs text-[var(--color-craft-text-secondary)]">{{ weatherError }}</p>
          <button @click="fetchWeather" class="text-[11px] text-[var(--color-craft-accent)] hover:underline mt-1">重试</button>
        </div>
        <div v-else-if="weather">
          <div v-if="weather.city" class="text-[10px] text-[var(--color-craft-text-secondary)] mb-2 flex items-center gap-1">
            <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
            {{ weather.city }}
          </div>
          <div class="flex items-center gap-3">
            <div class="text-3xl leading-none">{{ weather.icon }}</div>
            <div class="flex-1 min-w-0">
              <div class="text-lg font-bold text-[var(--color-craft-text)] leading-tight">{{ weather.temp }}</div>
              <div class="text-[11px] text-[var(--color-craft-text-secondary)]">{{ weather.desc }}</div>
            </div>
            <div class="text-right shrink-0">
              <div class="text-[10px] text-[var(--color-craft-text-secondary)]">💧 {{ weather.humidity }}</div>
              <div class="text-[10px] text-[var(--color-craft-text-secondary)]">💨 {{ weather.wind }}</div>
            </div>
          </div>
        </div>
        <div v-else class="text-center py-2">
          <button @click="fetchWeather" class="text-xs text-[var(--color-craft-accent)] hover:underline">获取天气</button>
        </div>
      </div>

      <!-- Mood -->
      <div class="p-4 border-b border-[var(--color-craft-border)]">
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-3">今日心情</h3>
        <div class="grid grid-cols-4 gap-1.5">
          <button
            v-for="mood in moodOptions"
            :key="mood.key"
            @click="selectMood(mood.key)"
            class="flex flex-col items-center gap-0.5 py-1.5 rounded-lg transition-all duration-150"
            :class="currentMood === mood.key
              ? 'bg-[var(--color-craft-accent)] text-white shadow-sm scale-105'
              : 'hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text)]'"
          >
            <span class="text-lg leading-none">{{ mood.emoji }}</span>
            <span class="text-[9px] font-medium">{{ mood.label }}</span>
          </button>
        </div>
      </div>

      <!-- Today's tasks -->
      <div v-if="displayDate.isToday && todayTasks.length" class="p-4">
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-2">今日任务</h3>
        <div class="space-y-1.5">
          <div v-for="task in todayTasks" :key="task.id" class="flex items-center gap-2 text-xs">
            <svg v-if="task.completed" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-success)" stroke-width="2.5" stroke-linecap="round" class="shrink-0"><path d="M20 6L9 17l-5-5"/></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-text-secondary)" stroke-width="1.5" stroke-linecap="round" class="shrink-0"><circle cx="12" cy="12" r="10"/></svg>
            <span class="truncate" :class="task.completed ? 'line-through text-[var(--color-craft-text-secondary)]' : 'text-[var(--color-craft-text)]'">{{ task.content }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
