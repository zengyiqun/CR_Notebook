<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'vue-router'
import { statsApi, type StatsInfo, type DailyStatsItem } from '@/api/organizations'

const authStore = useAuthStore()
const router = useRouter()
const stats = ref<StatsInfo | null>(null)
const loading = ref(true)
const error = ref('')

const trendDays = ref(7)
const trendOptions = [
  { value: 7, label: '近 7 天' },
  { value: 14, label: '近 14 天' },
  { value: 30, label: '近 30 天' },
  { value: 60, label: '近 60 天' },
  { value: 90, label: '近 90 天' },
]

const isPersonal = computed(() => authStore.isPersonalSpace)
const spaceName = computed(() => authStore.currentSpaceName)

async function fetchStats() {
  loading.value = true
  error.value = ''
  try {
    if (isPersonal.value) {
      stats.value = await statsApi.personal(trendDays.value)
    } else if (authStore.currentTenant?.id) {
      stats.value = await statsApi.org(authStore.currentTenant.id, trendDays.value)
    }
  } catch (e: any) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function setTrendDays(days: number) {
  trendDays.value = days
  fetchStats()
}

const cards = computed(() => {
  if (!stats.value) return []
  const s = stats.value
  const taskRate = s.taskCount > 0 ? Math.round((s.taskCompletedCount / s.taskCount) * 100) : 0
  return [
    { icon: 'note', label: '笔记', value: s.noteCount, color: 'from-indigo-400 to-purple-500' },
    { icon: 'folder', label: '文件夹', value: s.folderCount, color: 'from-blue-400 to-cyan-500' },
    { icon: 'task', label: '任务', value: s.taskCount, sub: `已完成 ${s.taskCompletedCount} (${taskRate}%)`, color: 'from-emerald-400 to-teal-500' },
    { icon: 'daily', label: '每日笔记', value: s.dailyNoteCount, color: 'from-amber-400 to-orange-500' },
    { icon: 'calendar', label: '日历事件', value: s.calendarEventCount, color: 'from-pink-400 to-rose-500' },
    { icon: 'whiteboard', label: '白板', value: s.whiteboardCount, color: 'from-violet-400 to-fuchsia-500' },
  ]
})

const dailyStats = computed(() => stats.value?.dailyStats ?? [])
const dailyMax = computed(() => {
  let m = 0
  for (const d of dailyStats.value) {
    m = Math.max(m, d.noteCount, d.taskCount)
  }
  return m || 1
})

function formatDate(dateStr: string) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function formatWeekday(dateStr: string) {
  const d = new Date(dateStr + 'T00:00:00')
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return '周' + days[d.getDay()]
}

function shouldShowLabel(index: number): boolean {
  const total = dailyStats.value.length
  if (total <= 14) return true
  if (index === 0 || index === total - 1) return true
  const interval = total <= 30 ? 7 : total <= 60 ? 14 : 15
  return index % interval === 0
}

const iconPaths: Record<string, string> = {
  note: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8',
  folder: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z',
  task: 'M9 11l3 3L22 4 M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11',
  daily: 'M12 8v4l3 3 M3 12a9 9 0 1 0 18 0 9 9 0 0 0-18 0',
  calendar: 'M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M16 2v4 M8 2v4 M3 10h18',
  whiteboard: 'M2 3h20v14H2z M8 21h8 M12 17v4',
}

onMounted(fetchStats)
watch(() => authStore.currentTenant, fetchStats, { deep: true })
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="max-w-4xl mx-auto px-6 py-8">
      <!-- Header -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-bold text-[var(--color-craft-text)]">用量统计</h1>
          <p class="text-sm text-[var(--color-craft-text-secondary)] mt-1">
            {{ spaceName }} 的数据概览
          </p>
        </div>
        <button v-if="!isPersonal && (authStore.organizations.find(o => o.id === authStore.currentTenant?.id)?.role === 'OWNER' || authStore.organizations.find(o => o.id === authStore.currentTenant?.id)?.role === 'ADMIN')"
          @click="router.push(`/org/${authStore.currentTenant?.id}/manage`)"
          class="px-4 py-2 rounded-xl text-sm bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-opacity flex items-center gap-2"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" x2="19" y1="8" y2="14"/><line x1="22" x2="16" y1="11" y2="11"/></svg>
          管理组织
        </button>
      </div>

      <div v-if="loading" class="text-center py-20 text-[var(--color-craft-text-secondary)]">加载中...</div>
      <div v-else-if="error" class="text-center py-20 text-[var(--color-craft-danger)]">{{ error }}</div>
      <template v-else-if="stats">
        <!-- Stats Cards -->
        <div class="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
          <div v-for="card in cards" :key="card.label"
            class="bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)] p-5 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
          >
            <div class="flex items-center gap-3 mb-3">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br flex items-center justify-center text-white shadow-sm" :class="card.color">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path v-for="(seg, i) in iconPaths[card.icon]?.split(' M').map((s: string, j: number) => j === 0 ? s : 'M' + s) ?? []" :key="i" :d="seg"/>
                </svg>
              </div>
              <span class="text-sm font-medium text-[var(--color-craft-text-secondary)]">{{ card.label }}</span>
            </div>
            <div class="text-3xl font-bold text-[var(--color-craft-text)]">{{ card.value }}</div>
            <div v-if="card.sub" class="text-xs text-[var(--color-craft-text-secondary)] mt-1">{{ card.sub }}</div>
          </div>
        </div>

        <!-- Daily Trend Chart -->
        <div v-if="dailyStats.length > 0" class="bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)] p-5 mb-8">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-semibold text-[var(--color-craft-text)]">活动趋势</h3>
            <div class="flex items-center gap-1 bg-[var(--color-craft-bg)] rounded-xl p-0.5 border border-[var(--color-craft-border)]">
              <button
                v-for="opt in trendOptions"
                :key="opt.value"
                @click="setTrendDays(opt.value)"
                class="px-2.5 py-1 text-[11px] font-medium rounded-lg transition-all duration-150"
                :class="trendDays === opt.value
                  ? 'bg-[var(--color-craft-accent)] text-white shadow-sm'
                  : 'text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-text)] hover:bg-[var(--color-craft-hover)]'"
              >{{ opt.label }}</button>
            </div>
          </div>
          <div
            class="flex items-end"
            :class="dailyStats.length <= 14 ? 'h-44 gap-2' : dailyStats.length <= 30 ? 'h-48 gap-[3px]' : dailyStats.length <= 60 ? 'h-48 gap-[2px]' : 'h-48 gap-px'"
          >
            <div
              v-for="(day, di) in dailyStats"
              :key="day.date"
              class="flex flex-col items-center h-full justify-end min-w-0 flex-1"
              :class="dailyStats.length <= 14 ? 'gap-1' : 'gap-0.5'"
            >
              <div class="w-full flex items-end justify-center gap-0.5 flex-1">
                <div class="relative group flex-1 flex flex-col items-center justify-end h-full min-w-0" :class="dailyStats.length <= 14 ? 'max-w-5' : dailyStats.length <= 30 ? 'max-w-3' : 'max-w-2'">
                  <span v-if="day.noteCount > 0 && dailyStats.length <= 30" class="text-[9px] text-indigo-500 font-medium mb-0.5 opacity-0 group-hover:opacity-100 transition-opacity">{{ day.noteCount }}</span>
                  <div
                    class="w-full bg-gradient-to-t from-indigo-400 to-indigo-300 transition-all duration-500"
                    :class="dailyStats.length <= 30 ? 'rounded-t-md' : 'rounded-t-sm'"
                    :style="{ height: `${Math.max(day.noteCount > 0 ? 6 : 0, (day.noteCount / dailyMax) * 100)}%` }"
                    :title="`${day.date} 笔记: ${day.noteCount}`"
                  />
                </div>
                <div class="relative group flex-1 flex flex-col items-center justify-end h-full min-w-0" :class="dailyStats.length <= 14 ? 'max-w-5' : dailyStats.length <= 30 ? 'max-w-3' : 'max-w-2'">
                  <span v-if="day.taskCount > 0 && dailyStats.length <= 30" class="text-[9px] text-emerald-500 font-medium mb-0.5 opacity-0 group-hover:opacity-100 transition-opacity">{{ day.taskCount }}</span>
                  <div
                    class="w-full bg-gradient-to-t from-emerald-400 to-emerald-300 transition-all duration-500"
                    :class="dailyStats.length <= 30 ? 'rounded-t-md' : 'rounded-t-sm'"
                    :style="{ height: `${Math.max(day.taskCount > 0 ? 6 : 0, (day.taskCount / dailyMax) * 100)}%` }"
                    :title="`${day.date} 任务: ${day.taskCount}`"
                  />
                </div>
              </div>
              <div class="text-center" v-if="shouldShowLabel(di)">
                <div class="text-[10px] font-medium text-[var(--color-craft-text)] whitespace-nowrap">{{ formatDate(day.date) }}</div>
                <div v-if="dailyStats.length <= 14" class="text-[9px] text-[var(--color-craft-text-secondary)]">{{ formatWeekday(day.date) }}</div>
              </div>
            </div>
          </div>
          <div class="flex items-center justify-center gap-5 mt-4">
            <div class="flex items-center gap-1.5">
              <div class="w-3 h-3 rounded-sm bg-gradient-to-r from-indigo-400 to-indigo-300"></div>
              <span class="text-xs text-[var(--color-craft-text-secondary)]">新建笔记</span>
            </div>
            <div class="flex items-center gap-1.5">
              <div class="w-3 h-3 rounded-sm bg-gradient-to-r from-emerald-400 to-emerald-300"></div>
              <span class="text-xs text-[var(--color-craft-text-secondary)]">新建任务</span>
            </div>
          </div>
        </div>

        <!-- Task completion bar -->
        <div v-if="stats.taskCount > 0" class="bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)] p-5 mb-8">
          <h3 class="text-sm font-semibold text-[var(--color-craft-text)] mb-3">任务完成率</h3>
          <div class="w-full h-4 bg-[var(--color-craft-hover)] rounded-full overflow-hidden">
            <div class="h-full bg-gradient-to-r from-emerald-400 to-teal-500 rounded-full transition-all duration-500"
              :style="{ width: `${Math.round((stats.taskCompletedCount / stats.taskCount) * 100)}%` }"
            />
          </div>
          <div class="flex justify-between mt-2 text-xs text-[var(--color-craft-text-secondary)]">
            <span>{{ stats.taskCompletedCount }} 已完成</span>
            <span>{{ stats.taskCount - stats.taskCompletedCount }} 待处理</span>
          </div>
        </div>

        <!-- Org Members Overview -->
        <div v-if="!isPersonal && stats.memberStats && stats.memberStats.length > 0" class="bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)]">
          <div class="px-5 py-4 border-b border-[var(--color-craft-border)] flex items-center justify-between">
            <h3 class="text-base font-semibold text-[var(--color-craft-text)]">成员 ({{ stats.memberCount }})</h3>
          </div>
          <div class="divide-y divide-[var(--color-craft-border)]">
            <div v-for="member in stats.memberStats" :key="member.userId" class="flex items-center gap-4 px-5 py-3">
              <div class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold"
                :class="member.role === 'OWNER' ? 'bg-gradient-to-br from-amber-400 to-orange-500' : member.role === 'ADMIN' ? 'bg-gradient-to-br from-emerald-400 to-teal-500' : 'bg-gradient-to-br from-indigo-400 to-purple-500'"
              >
                {{ (member.displayName || member.username).charAt(0).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <span class="text-sm font-medium text-[var(--color-craft-text)]">{{ member.displayName || member.username }}</span>
                <span class="text-xs text-[var(--color-craft-text-secondary)] ml-2">@{{ member.username }}</span>
              </div>
              <div class="px-2.5 py-0.5 rounded-full text-[10px] font-medium"
                :class="member.role === 'OWNER' ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400' : member.role === 'ADMIN' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'"
              >
                {{ member.role === 'OWNER' ? '所有者' : member.role === 'ADMIN' ? '管理员' : '成员' }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
