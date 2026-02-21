import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Whiteboard } from '@/types'
import { whiteboardsApi } from '@/api/whiteboards'

export const useWhiteboardStore = defineStore('whiteboard', () => {
  const whiteboards = ref<Whiteboard[]>([])
  const activeWhiteboardId = ref<string | null>(null)
  const loading = ref(false)

  const activeWhiteboard = computed(() =>
    whiteboards.value.find((w) => w.id === activeWhiteboardId.value) ?? null
  )

  async function fetchWhiteboards() {
    loading.value = true
    try {
      whiteboards.value = await whiteboardsApi.list()
    } catch (e) {
      console.error('Failed to fetch whiteboards:', e)
    } finally {
      loading.value = false
    }
  }

  async function addWhiteboard(title = '新白板') {
    try {
      const wb = await whiteboardsApi.create(title)
      whiteboards.value.push(wb)
      activeWhiteboardId.value = wb.id
      return wb
    } catch (e) {
      console.error('Failed to create whiteboard:', e)
    }
  }

  async function updateWhiteboard(id: string, updates: Partial<Pick<Whiteboard, 'title' | 'data'>>) {
    const wb = whiteboards.value.find((w) => w.id === id)
    if (wb) {
      Object.assign(wb, updates, { updatedAt: new Date().toISOString() })
    }
    try {
      await whiteboardsApi.update(id, updates)
    } catch (e) {
      console.error('Failed to update whiteboard:', e)
    }
  }

  async function deleteWhiteboard(id: string) {
    try {
      await whiteboardsApi.delete(id)
      whiteboards.value = whiteboards.value.filter((w) => w.id !== id)
      if (activeWhiteboardId.value === id) {
        activeWhiteboardId.value = whiteboards.value[0]?.id ?? null
      }
    } catch (e) {
      console.error('Failed to delete whiteboard:', e)
    }
  }

  function setActive(id: string | null) {
    activeWhiteboardId.value = id
  }

  return { whiteboards, activeWhiteboardId, activeWhiteboard, loading, fetchWhiteboards, addWhiteboard, updateWhiteboard, deleteWhiteboard, setActive }
})
