<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useTaskStore } from '@/stores/taskStore'
import type { Task } from '@/types'

const taskStore = useTaskStore()

onMounted(() => {
  taskStore.fetchTasks()
})
const newTaskContent = ref('')
const newTaskPriority = ref<Task['priority']>('medium')
const showCompleted = ref(false)

const stats = computed(() => ({
  total: taskStore.tasks.length,
  pending: taskStore.pendingTasks.length,
  completed: taskStore.completedTasks.length,
  today: taskStore.todayTasks.length,
}))

function addTask() {
  const content = newTaskContent.value.trim()
  if (!content) return
  taskStore.addTask(content, newTaskPriority.value)
  newTaskContent.value = ''
  newTaskPriority.value = 'medium'
}

function priorityColor(p: Task['priority']) {
  switch (p) {
    case 'high': return 'text-red-500 bg-red-50 dark:bg-red-950/30'
    case 'medium': return 'text-amber-500 bg-amber-50 dark:bg-amber-950/30'
    case 'low': return 'text-blue-400 bg-blue-50 dark:bg-blue-950/30'
  }
}

function priorityLabel(p: Task['priority']) {
  switch (p) {
    case 'high': return '高'
    case 'medium': return '中'
    case 'low': return '低'
  }
}
</script>

<template>
  <div class="h-full flex flex-col bg-[var(--color-craft-surface)]">
    <!-- Header -->
    <div class="px-8 pt-8 pb-4">
      <h1 class="text-2xl font-bold text-[var(--color-craft-text)]">任务</h1>
      <p class="text-sm text-[var(--color-craft-text-secondary)] mt-1">管理你的待办事项</p>

      <!-- Stats -->
      <div class="flex gap-3 mt-4">
        <div v-for="s in [
          { label: '待完成', value: stats.pending, color: 'text-[var(--color-craft-accent)]' },
          { label: '已完成', value: stats.completed, color: 'text-[var(--color-craft-success)]' },
          { label: '今日', value: stats.today, color: 'text-amber-500' },
        ]" :key="s.label"
          class="flex-1 px-4 py-3 rounded-xl bg-[var(--color-craft-bg)] border border-[var(--color-craft-border)]"
        >
          <div :class="s.color" class="text-xl font-bold">{{ s.value }}</div>
          <div class="text-[11px] text-[var(--color-craft-text-secondary)] mt-0.5">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- Add task -->
    <div class="px-8 pb-4">
      <div class="flex gap-2">
        <div class="flex-1 relative">
          <input
            v-model="newTaskContent"
            @keydown.enter="addTask"
            type="text"
            placeholder="添加新任务... 按回车提交"
            class="w-full px-4 py-2.5 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] placeholder:text-[var(--color-craft-text-secondary)] focus:outline-none focus:border-[var(--color-craft-accent)] focus:ring-1 focus:ring-[var(--color-craft-accent)]/20 transition-all"
          />
        </div>
        <select
          v-model="newTaskPriority"
          class="px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] focus:outline-none focus:border-[var(--color-craft-accent)]"
        >
          <option value="high">高</option>
          <option value="medium">中</option>
          <option value="low">低</option>
        </select>
        <button
          @click="addTask"
          class="px-5 py-2.5 text-sm font-medium rounded-xl bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all shadow-sm active:scale-[0.98]"
        >
          添加
        </button>
      </div>
    </div>

    <!-- Task list -->
    <div class="flex-1 overflow-y-auto px-8 pb-8">
      <!-- Pending tasks -->
      <div class="mb-6">
        <h3 class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-3">
          待完成 ({{ taskStore.pendingTasks.length }})
        </h3>
        <div class="space-y-1.5">
          <div
            v-for="task in taskStore.pendingTasks"
            :key="task.id"
            class="flex items-center gap-3 px-4 py-3 rounded-xl bg-[var(--color-craft-bg)] border border-[var(--color-craft-border)] group hover:border-[var(--color-craft-accent)]/40 hover:shadow-sm transition-all"
          >
            <button
              @click="taskStore.toggleTask(task.id)"
              class="w-5 h-5 rounded-full border-2 border-[var(--color-craft-border)] hover:border-[var(--color-craft-accent)] hover:bg-[var(--color-craft-accent-light)] flex items-center justify-center transition-all shrink-0"
            />
            <span class="flex-1 text-sm text-[var(--color-craft-text)]">{{ task.content }}</span>
            <span
              :class="priorityColor(task.priority)"
              class="text-[10px] font-semibold px-2 py-0.5 rounded-md"
            >
              {{ priorityLabel(task.priority) }}
            </span>
            <span v-if="task.dueDate" class="text-[11px] text-[var(--color-craft-text-secondary)]">
              {{ task.dueDate }}
            </span>
            <button
              @click="taskStore.deleteTask(task.id)"
              class="w-6 h-6 flex items-center justify-center rounded-md text-[var(--color-craft-text-secondary)] opacity-0 group-hover:opacity-100 hover:text-[var(--color-craft-danger)] hover:bg-red-50 dark:hover:bg-red-950/30 transition-all"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
            </button>
          </div>
        </div>
        <div v-if="taskStore.pendingTasks.length === 0" class="py-8 text-center">
          <div class="w-14 h-14 mx-auto mb-3 rounded-2xl bg-gradient-to-br from-emerald-100 to-teal-100 dark:from-emerald-900/30 dark:to-teal-900/30 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-success)" stroke-width="1.5" stroke-linecap="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4L12 14.01l-3-3"/></svg>
          </div>
          <p class="text-sm text-[var(--color-craft-text-secondary)] mt-2">所有任务已完成！</p>
        </div>
      </div>

      <!-- Completed tasks -->
      <div>
        <button
          @click="showCompleted = !showCompleted"
          class="flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-3 hover:text-[var(--color-craft-text)] transition-colors"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
            class="transition-transform" :class="showCompleted ? 'rotate-90' : ''"
          ><path d="M9 18l6-6-6-6"/></svg>
          已完成 ({{ taskStore.completedTasks.length }})
        </button>
        <Transition name="fade-slide">
          <div v-if="showCompleted" class="space-y-1.5">
            <div
              v-for="task in taskStore.completedTasks"
              :key="task.id"
              class="flex items-center gap-3 px-4 py-3 rounded-xl bg-[var(--color-craft-bg)] border border-[var(--color-craft-border)] group opacity-60 hover:opacity-80 transition-all"
            >
              <button
                @click="taskStore.toggleTask(task.id)"
                class="w-5 h-5 rounded-full bg-[var(--color-craft-success)] flex items-center justify-center text-white text-xs shrink-0"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><path d="M20 6L9 17l-5-5"/></svg>
              </button>
              <span class="flex-1 text-sm text-[var(--color-craft-text)] line-through">{{ task.content }}</span>
              <button
                @click="taskStore.deleteTask(task.id)"
                class="w-6 h-6 flex items-center justify-center rounded-md text-[var(--color-craft-text-secondary)] opacity-0 group-hover:opacity-100 hover:text-[var(--color-craft-danger)] transition-all"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>
