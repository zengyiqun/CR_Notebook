import { http } from './http'
import type { DailyNote } from '@/types'

interface DailyNoteResponse {
  id: number
  noteDate: string
  content: string
  weather: string | null
  mood: string | null
  createdAt: string
  updatedAt: string
}

function mapToDailyNote(r: DailyNoteResponse): DailyNote {
  return {
    id: String(r.id),
    date: r.noteDate,
    content: r.content || '',
    weather: r.weather || '',
    mood: r.mood || '',
    createdAt: r.createdAt,
    updatedAt: r.updatedAt,
  }
}

export const dailyNotesApi = {
  get: async (date: string): Promise<DailyNote> => {
    const data = await http.get<DailyNoteResponse>(`/daily-notes/${date}`)
    return mapToDailyNote(data)
  },
  update: async (date: string, payload: { content?: string; weather?: string; mood?: string }): Promise<DailyNote> => {
    const data = await http.put<DailyNoteResponse>(`/daily-notes/${date}`, payload)
    return mapToDailyNote(data)
  },
  listDatesWithContent: (from: string, to: string): Promise<string[]> =>
    http.get<string[]>('/daily-notes/dates', { from, to }),
}
