import { http } from './http'
import type { CalendarEvent } from '@/types'

interface CalendarEventResponse {
  id: number
  title: string
  eventDate: string
  eventTime: string | null
  endDate: string | null
  endTime: string | null
  description: string
  color: string
  createdAt: string
  updatedAt: string
}

function mapToEvent(r: CalendarEventResponse): CalendarEvent {
  return {
    id: String(r.id),
    title: r.title,
    date: r.eventDate,
    time: r.eventTime,
    endDate: r.endDate || null,
    endTime: r.endTime || null,
    description: r.description || '',
    color: r.color || '#6366f1',
  }
}

export const calendarApi = {
  list: async (startDate: string, endDate: string): Promise<CalendarEvent[]> => {
    const data = await http.get<CalendarEventResponse[]>('/calendar-events', { startDate, endDate })
    return data.map(mapToEvent)
  },
  create: async (event: Omit<CalendarEvent, 'id'>): Promise<CalendarEvent> => {
    const data = await http.post<CalendarEventResponse>('/calendar-events', {
      title: event.title,
      eventDate: event.date,
      eventTime: event.time,
      endDate: event.endDate,
      endTime: event.endTime,
      description: event.description,
      color: event.color,
    })
    return mapToEvent(data)
  },
  update: async (id: string, updates: Partial<CalendarEvent>): Promise<CalendarEvent> => {
    const data = await http.put<CalendarEventResponse>(`/calendar-events/${id}`, {
      title: updates.title,
      eventDate: updates.date,
      eventTime: updates.time,
      endDate: updates.endDate,
      endTime: updates.endTime,
      description: updates.description,
      color: updates.color,
    })
    return mapToEvent(data)
  },
  delete: (id: string) => http.delete(`/calendar-events/${id}`),
}
