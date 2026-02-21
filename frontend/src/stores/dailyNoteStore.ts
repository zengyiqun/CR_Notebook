import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DailyNote } from '@/types'
import { dailyNotesApi } from '@/api/dailyNotes'

export const useDailyNoteStore = defineStore('dailyNote', () => {
  const dailyNotes = ref<Map<string, DailyNote>>(new Map())
  const loading = ref(false)

  async function getOrCreate(date: string): Promise<DailyNote> {
    const cached = dailyNotes.value.get(date)
    if (cached) return cached

    loading.value = true
    try {
      const note = await dailyNotesApi.get(date)
      dailyNotes.value.set(date, note)
      return note
    } catch (e) {
      console.error('Failed to get daily note:', e)
      const fallback: DailyNote = {
        id: '',
        date,
        content: '',
        weather: '',
        mood: '',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      return fallback
    } finally {
      loading.value = false
    }
  }

  async function updateDailyNote(date: string, payload: { content?: string; weather?: string; mood?: string }) {
    try {
      const updated = await dailyNotesApi.update(date, payload)
      dailyNotes.value.set(date, updated)
      return updated
    } catch (e) {
      console.error('Failed to update daily note:', e)
    }
  }

  const datesWithContent = ref<Set<string>>(new Set())

  async function fetchDatesWithContent(from: string, to: string) {
    try {
      const dates = await dailyNotesApi.listDatesWithContent(from, to)
      dates.forEach(d => datesWithContent.value.add(d))
    } catch (e) {
      console.error('Failed to fetch dates with content:', e)
    }
  }

  return { dailyNotes, loading, getOrCreate, updateDailyNote, datesWithContent, fetchDatesWithContent }
})
