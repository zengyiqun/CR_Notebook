<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useCalendarStore } from '@/stores/calendarStore'

const calendarStore = useCalendarStore()

const currentDate = ref(new Date())
const showAddModal = ref(false)
const selectedDateEvents = ref<string | null>(null)
const editingEventId = ref<string | null>(null)
const isAllDay = ref(true)
const newEvent = ref({ title: '', date: '', time: '', endDate: '', endTime: '', description: '', color: '#6366f1' })

const year = computed(() => currentDate.value.getFullYear())
const month = computed(() => currentDate.value.getMonth())

function fetchCurrentMonthEvents() {
  const start = new Date(year.value, month.value - 1, 1).toISOString().slice(0, 10)
  const end = new Date(year.value, month.value + 2, 0).toISOString().slice(0, 10)
  calendarStore.fetchEvents(start, end)
}

onMounted(fetchCurrentMonthEvents)
watch([year, month], fetchCurrentMonthEvents)

const monthName = computed(() =>
  currentDate.value.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long' })
)

const calendarDays = computed(() => {
  const firstDay = new Date(year.value, month.value, 1)
  const lastDay = new Date(year.value, month.value + 1, 0)
  const startOffset = firstDay.getDay()
  const days: { date: string; day: number; isCurrentMonth: boolean; isToday: boolean }[] = []

  const prevMonthLast = new Date(year.value, month.value, 0).getDate()
  for (let i = startOffset - 1; i >= 0; i--) {
    const d = new Date(year.value, month.value - 1, prevMonthLast - i)
    days.push({ date: d.toISOString().slice(0, 10), day: prevMonthLast - i, isCurrentMonth: false, isToday: false })
  }

  const today = new Date().toISOString().slice(0, 10)
  for (let d = 1; d <= lastDay.getDate(); d++) {
    const dateObj = new Date(year.value, month.value, d)
    const dateStr = dateObj.toISOString().slice(0, 10)
    days.push({ date: dateStr, day: d, isCurrentMonth: true, isToday: dateStr === today })
  }

  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    const d = new Date(year.value, month.value + 1, i)
    days.push({ date: d.toISOString().slice(0, 10), day: i, isCurrentMonth: false, isToday: false })
  }

  return days
})

function getEventsForDate(date: string) {
  return calendarStore.events.filter((e) => {
    if (e.date === date) return true
    if (e.endDate && date >= e.date && date <= e.endDate) return true
    return false
  })
}

function prevMonth() { currentDate.value = new Date(year.value, month.value - 1, 1) }
function nextMonth() { currentDate.value = new Date(year.value, month.value + 1, 1) }
function goToday() { currentDate.value = new Date() }

function openAddEvent(date: string) {
  editingEventId.value = null
  isAllDay.value = true
  newEvent.value = { title: '', date, time: '', endDate: '', endTime: '', description: '', color: '#6366f1' }
  showAddModal.value = true
}

function openEditEvent(evt: any) {
  editingEventId.value = evt.id
  isAllDay.value = !evt.time
  newEvent.value = {
    title: evt.title,
    date: evt.date,
    time: evt.time || '',
    endDate: evt.endDate || '',
    endTime: evt.endTime || '',
    description: evt.description || '',
    color: evt.color || '#6366f1',
  }
  showAddModal.value = true
}

function toggleAllDay() {
  isAllDay.value = !isAllDay.value
  if (isAllDay.value) {
    newEvent.value.time = ''
    newEvent.value.endTime = ''
  }
}

function saveEvent() {
  if (!newEvent.value.title.trim() || !newEvent.value.date) return
  const payload = {
    title: newEvent.value.title.trim(),
    date: newEvent.value.date,
    time: isAllDay.value ? null : (newEvent.value.time || null),
    endDate: newEvent.value.endDate || null,
    endTime: isAllDay.value ? null : (newEvent.value.endTime || null),
    description: newEvent.value.description,
    color: newEvent.value.color,
  }
  if (editingEventId.value) {
    calendarStore.updateEvent(editingEventId.value, payload)
  } else {
    calendarStore.addEvent(payload)
  }
  showAddModal.value = false
  editingEventId.value = null
}

function isMultiDayEvent(evt: any): boolean {
  return !!evt.endDate && evt.endDate !== evt.date
}

function eventDateLabel(evt: any, currentDate: string): string {
  if (!isMultiDayEvent(evt)) {
    return evt.time ? evt.time + ' ' : ''
  }
  if (currentDate === evt.date) {
    return evt.time ? evt.time + ' 开始 ' : '开始 '
  }
  if (currentDate === evt.endDate) {
    return evt.endTime ? evt.endTime + ' 结束 ' : '结束 '
  }
  return ''
}

function showDayEvents(date: string) {
  selectedDateEvents.value = selectedDateEvents.value === date ? null : date
}

const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const eventColors = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#ec4899']
</script>

<template>
  <div class="h-full flex flex-col bg-[var(--color-craft-surface)]">
    <!-- Header -->
    <div class="px-8 pt-8 pb-4 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[var(--color-craft-text)]">日历</h1>
        <p class="text-sm text-[var(--color-craft-text-secondary)] mt-0.5">{{ monthName }}</p>
      </div>
      <div class="flex items-center gap-1.5">
        <button @click="prevMonth" class="w-8 h-8 flex items-center justify-center rounded-xl hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <button @click="goToday" class="px-3 py-1.5 text-xs font-medium rounded-xl border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">今天</button>
        <button @click="nextMonth" class="w-8 h-8 flex items-center justify-center rounded-xl hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6"/></svg>
        </button>
        <div class="w-px h-5 bg-[var(--color-craft-border)] mx-1" />
        <button
          @click="openAddEvent(new Date().toISOString().slice(0, 10))"
          class="px-3 py-1.5 text-xs font-medium rounded-xl bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all shadow-sm "
        >+ 新事件</button>
      </div>
    </div>

    <!-- Calendar grid -->
    <div class="flex-1 overflow-y-auto px-8 pb-8">
      <!-- Weekday headers -->
      <div class="grid grid-cols-7 mb-1">
        <div
          v-for="wd in weekdays"
          :key="wd"
          class="text-center text-[11px] font-semibold text-[var(--color-craft-text-secondary)] py-2 uppercase"
        >{{ wd }}</div>
      </div>

      <!-- Days -->
      <div class="grid grid-cols-7 gap-1">
        <div
          v-for="(day, i) in calendarDays"
          :key="i"
          @click="day.isCurrentMonth && showDayEvents(day.date)"
          @dblclick.stop="day.isCurrentMonth && openAddEvent(day.date)"
          class="min-h-24 p-1.5 rounded-xl border transition-all cursor-pointer"
          :class="{
            'bg-[var(--color-craft-bg)] border-transparent hover:border-[var(--color-craft-accent)]/30': day.isCurrentMonth,
            'opacity-30 border-transparent': !day.isCurrentMonth,
            'border-[var(--color-craft-accent)]/50 shadow-sm': selectedDateEvents === day.date,
          }"
        >
          <div
            class="w-7 h-7 flex items-center justify-center rounded-full text-xs font-medium mb-1"
            :class="day.isToday
              ? 'bg-[var(--color-craft-accent)] text-white shadow-sm '
              : 'text-[var(--color-craft-text)]'"
          >{{ day.day }}</div>
          <div class="space-y-0.5">
            <div
              v-for="evt in getEventsForDate(day.date).slice(0, 2)"
              :key="evt.id + '-' + day.date"
              @dblclick.stop="openEditEvent(evt)"
              class="text-[10px] px-1.5 py-0.5 rounded-md truncate text-white font-medium cursor-pointer hover:opacity-90"
              :style="{ backgroundColor: evt.color, opacity: isMultiDayEvent(evt) && day.date !== evt.date && day.date !== evt.endDate ? 0.7 : 1 }"
              :title="evt.title + (isMultiDayEvent(evt) ? ` (${evt.date} ~ ${evt.endDate})` : '')"
            >{{ eventDateLabel(evt, day.date) }}{{ evt.title }}</div>
            <div
              v-if="getEventsForDate(day.date).length > 2"
              class="text-[10px] text-[var(--color-craft-text-secondary)] px-1.5"
            >+{{ getEventsForDate(day.date).length - 2 }} 更多</div>
          </div>
        </div>
      </div>

      <!-- Day detail panel -->
      <Transition name="fade-slide">
        <div
          v-if="selectedDateEvents"
          class="mt-4 p-4 rounded-2xl bg-[var(--color-craft-bg)] border border-[var(--color-craft-border)]"
        >
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-semibold text-[var(--color-craft-text)]">
              {{ new Date(selectedDateEvents).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }) }}
            </h3>
            <button
              @click="openAddEvent(selectedDateEvents!)"
              class="text-xs text-[var(--color-craft-accent)] hover:underline font-medium"
            >+ 添加</button>
          </div>
          <div v-if="getEventsForDate(selectedDateEvents).length === 0" class="text-sm text-[var(--color-craft-text-secondary)] py-2">
            暂无事件，双击日期或点击添加
          </div>
          <div v-else class="space-y-2">
            <div
              v-for="evt in getEventsForDate(selectedDateEvents)"
              :key="evt.id"
              class="flex items-start gap-3 p-3 rounded-xl bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] group"
            >
              <div class="w-1 h-10 rounded-full shrink-0" :style="{ backgroundColor: evt.color }" />
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-[var(--color-craft-text)]">{{ evt.title }}</div>
                <div class="text-xs text-[var(--color-craft-text-secondary)] mt-0.5">
                  <template v-if="isMultiDayEvent(evt)">
                    {{ evt.date }}<span v-if="evt.time"> {{ evt.time }}</span>
                    ~ {{ evt.endDate }}<span v-if="evt.endTime"> {{ evt.endTime }}</span>
                  </template>
                  <template v-else>{{ evt.time || '全天' }}</template>
                  <span v-if="evt.description"> · {{ evt.description }}</span>
                </div>
              </div>
              <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-all shrink-0">
                <button
                  @click.stop="openEditEvent(evt)"
                  class="w-6 h-6 flex items-center justify-center rounded-md text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-accent)] hover:bg-[var(--color-craft-hover)] transition-colors"
                  title="编辑"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                </button>
                <button
                  @click.stop="calendarStore.deleteEvent(evt.id)"
                  class="w-6 h-6 flex items-center justify-center rounded-md text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] hover:bg-[var(--color-craft-hover)] transition-colors text-xs"
                  title="删除"
                >×</button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- Add event modal -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showAddModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm" @click.self="showAddModal = false">
          <Transition name="fade-slide">
            <div v-if="showAddModal" class="w-96 bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)] shadow-2xl p-6">
              <h3 class="text-lg font-semibold text-[var(--color-craft-text)] mb-4">{{ editingEventId ? '编辑事件' : '新建事件' }}</h3>
              <div class="space-y-3">
                <input
                  v-model="newEvent.title"
                  type="text"
                  placeholder="事件标题"
                  class="w-full px-3 py-2.5 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)] focus:ring-1 focus:ring-[var(--color-craft-accent)]/20 transition-all"
                  @keydown.enter="saveEvent"
                  autofocus
                />
                <!-- All-day toggle -->
                <button
                  @click="toggleAllDay"
                  type="button"
                  class="w-full flex items-center justify-between px-3 py-2 rounded-xl border transition-colors"
                  :class="isAllDay
                    ? 'border-[var(--color-craft-accent)] bg-[var(--color-craft-accent-light)]'
                    : 'border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] hover:bg-[var(--color-craft-hover)]'"
                >
                  <span class="text-sm" :class="isAllDay ? 'text-[var(--color-craft-accent)] font-medium' : 'text-[var(--color-craft-text-secondary)]'">全天事件</span>
                  <span
                    class="w-8 h-[18px] rounded-full relative transition-colors duration-200"
                    :class="isAllDay ? 'bg-[var(--color-craft-accent)]' : 'bg-[var(--color-craft-border)]'"
                  >
                    <span
                      class="absolute top-[2px] w-[14px] h-[14px] rounded-full bg-white shadow-sm transition-all duration-200"
                      :class="isAllDay ? 'left-[15px]' : 'left-[2px]'"
                    />
                  </span>
                </button>
                <div class="space-y-2">
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-[var(--color-craft-text-secondary)] w-10 shrink-0">开始</span>
                    <input v-model="newEvent.date" type="date" class="flex-1 px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)]" />
                    <input v-if="!isAllDay" v-model="newEvent.time" type="time" class="w-28 px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)]" />
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-[var(--color-craft-text-secondary)] w-10 shrink-0">结束</span>
                    <input v-model="newEvent.endDate" type="date" :min="newEvent.date" class="flex-1 px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)]" placeholder="可选" />
                    <input v-if="!isAllDay" v-model="newEvent.endTime" type="time" class="w-28 px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)]" />
                  </div>
                </div>
                <input
                  v-model="newEvent.description"
                  type="text"
                  placeholder="描述（可选）"
                  class="w-full px-3 py-2.5 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)]"
                />
                <div class="flex gap-2 items-center">
                  <span class="text-xs text-[var(--color-craft-text-secondary)]">颜色</span>
                  <button
                    v-for="c in eventColors"
                    :key="c"
                    @click="newEvent.color = c"
                    class="w-7 h-7 rounded-full transition-all"
                    :class="newEvent.color === c ? 'ring-2 ring-offset-2 ring-[var(--color-craft-accent)] scale-110' : 'hover:scale-105'"
                    :style="{ backgroundColor: c }"
                  />
                </div>
              </div>
              <div class="flex justify-end gap-2 mt-5">
                <button @click="showAddModal = false; editingEventId = null" class="px-4 py-2 text-sm rounded-xl text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-colors">取消</button>
                <button @click="saveEvent" class="px-5 py-2 text-sm rounded-xl bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all font-medium shadow-sm">{{ editingEventId ? '保存' : '创建' }}</button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
