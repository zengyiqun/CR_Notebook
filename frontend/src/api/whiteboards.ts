import { http } from './http'
import type { Whiteboard } from '@/types'

interface WhiteboardResponse {
  id: number
  title: string
  data: string
  createdAt: string
  updatedAt: string
}

function mapToWhiteboard(r: WhiteboardResponse): Whiteboard {
  return {
    id: String(r.id),
    title: r.title,
    data: r.data || '{}',
    createdAt: r.createdAt,
    updatedAt: r.updatedAt,
  }
}

export const whiteboardsApi = {
  list: async (): Promise<Whiteboard[]> => {
    const data = await http.get<WhiteboardResponse[]>('/whiteboards')
    return data.map(mapToWhiteboard)
  },
  create: async (title = '新白板'): Promise<Whiteboard> => {
    const data = await http.post<WhiteboardResponse>('/whiteboards', { title, data: '{}' })
    return mapToWhiteboard(data)
  },
  update: async (id: string, updates: Partial<Pick<Whiteboard, 'title' | 'data'>>): Promise<Whiteboard> => {
    const data = await http.put<WhiteboardResponse>(`/whiteboards/${id}`, updates)
    return mapToWhiteboard(data)
  },
  delete: (id: string) => http.delete(`/whiteboards/${id}`),
}
