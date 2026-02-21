import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Task } from '@/types'
import { tasksApi } from '@/api/tasks'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const loading = ref(false)

  const pendingTasks = computed(() => tasks.value.filter((t) => !t.completed))
  const completedTasks = computed(() => tasks.value.filter((t) => t.completed))
  const todayTasks = computed(() => {
    const today = new Date().toISOString().slice(0, 10)
    return tasks.value.filter((t) => t.dueDate === today)
  })

  async function fetchTasks() {
    loading.value = true
    try {
      tasks.value = await tasksApi.list()
    } catch (e) {
      console.error('Failed to fetch tasks:', e)
    } finally {
      loading.value = false
    }
  }

  async function addTask(content: string, priority: Task['priority'] = 'medium', dueDate: string | null = null) {
    try {
      const task = await tasksApi.create(content, priority, dueDate)
      tasks.value.unshift(task)
      return task
    } catch (e) {
      console.error('Failed to create task:', e)
    }
  }

  async function toggleTask(id: string) {
    const task = tasks.value.find((t) => t.id === id)
    if (!task) return
    const newCompleted = !task.completed
    task.completed = newCompleted
    try {
      await tasksApi.update(id, { completed: newCompleted })
    } catch (e) {
      task.completed = !newCompleted
      console.error('Failed to toggle task:', e)
    }
  }

  async function updateTask(id: string, updates: Partial<Pick<Task, 'content' | 'priority' | 'dueDate'>>) {
    try {
      const updated = await tasksApi.update(id, updates)
      const idx = tasks.value.findIndex((t) => t.id === id)
      if (idx !== -1) tasks.value[idx] = updated
    } catch (e) {
      console.error('Failed to update task:', e)
    }
  }

  async function deleteTask(id: string) {
    try {
      await tasksApi.delete(id)
      tasks.value = tasks.value.filter((t) => t.id !== id)
    } catch (e) {
      console.error('Failed to delete task:', e)
    }
  }

  return { tasks, loading, pendingTasks, completedTasks, todayTasks, fetchTasks, addTask, toggleTask, updateTask, deleteTask }
})
