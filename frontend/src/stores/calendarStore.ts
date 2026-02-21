import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CalendarEvent } from '@/types'
import { calendarApi } from '@/api/calendar'

export const useCalendarStore = defineStore('calendar', () => {
  const events = ref<CalendarEvent[]>([])
  const loading = ref(false)

  function eventsByDate(date: string) {
    return computed(() =>
      events.value
        .filter((e) => e.date === date)
        .sort((a, b) => (a.time ?? '').localeCompare(b.time ?? ''))
    )
  }

  async function fetchEvents(startDate: string, endDate: string) {
    loading.value = true
    try {
      events.value = await calendarApi.list(startDate, endDate)
    } catch (e) {
      console.error('Failed to fetch events:', e)
    } finally {
      loading.value = false
    }
  }

  async function addEvent(event: Omit<CalendarEvent, 'id'>) {
    try {
      const newEvent = await calendarApi.create(event)
      events.value.push(newEvent)
      return newEvent
    } catch (e) {
      console.error('Failed to create event:', e)
    }
  }

  async function updateEvent(id: string, updates: Partial<Omit<CalendarEvent, 'id'>>) {
    try {
      const updated = await calendarApi.update(id, updates as Partial<CalendarEvent>)
      const idx = events.value.findIndex((e) => e.id === id)
      if (idx !== -1) events.value[idx] = updated
    } catch (e) {
      console.error('Failed to update event:', e)
    }
  }

  async function deleteEvent(id: string) {
    try {
      await calendarApi.delete(id)
      events.value = events.value.filter((e) => e.id !== id)
    } catch (e) {
      console.error('Failed to delete event:', e)
    }
  }

  return { events, loading, eventsByDate, fetchEvents, addEvent, updateEvent, deleteEvent }
})
