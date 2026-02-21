import { http } from './http'
import type { Note } from '@/types'

interface NoteResponse {
  id: number
  folderId: number | null
  title: string
  content: string
  excerpt: string
  isPinned: boolean
  tags: string[]
  createdAt: string
  updatedAt: string
}

function mapToNote(r: NoteResponse): Note {
  return {
    id: String(r.id),
    folderId: r.folderId ? String(r.folderId) : null,
    title: r.title,
    content: r.content || '',
    excerpt: r.excerpt || '',
    isPinned: r.isPinned,
    tags: r.tags || [],
    createdAt: r.createdAt,
    updatedAt: r.updatedAt,
  }
}

export const notesApi = {
  list: async (folderId?: string | null): Promise<Note[]> => {
    const params = folderId ? { folderId } : undefined
    const data = await http.get<NoteResponse[]>('/notes', params)
    return data.map(mapToNote)
  },

  get: async (id: string): Promise<Note> => {
    const data = await http.get<NoteResponse>(`/notes/${id}`)
    return mapToNote(data)
  },

  create: async (note: Partial<Note>): Promise<Note> => {
    const data = await http.post<NoteResponse>('/notes', {
      folderId: note.folderId ? Number(note.folderId) : null,
      title: note.title,
      content: note.content,
      excerpt: note.excerpt,
      isPinned: note.isPinned,
    })
    return mapToNote(data)
  },

  update: async (id: string, updates: Partial<Note>): Promise<Note> => {
    const data = await http.put<NoteResponse>(`/notes/${id}`, {
      ...updates,
      folderId: updates.folderId ? Number(updates.folderId) : undefined,
    })
    return mapToNote(data)
  },

  delete: (id: string) => http.delete(`/notes/${id}`),

  search: async (q: string): Promise<Note[]> => {
    const data = await http.get<NoteResponse[]>('/notes/search', { q })
    return data.map(mapToNote)
  },

  backlinks: async (id: string): Promise<Note[]> => {
    const data = await http.get<NoteResponse[]>(`/notes/${id}/backlinks`)
    return data.map(mapToNote)
  },

  /** 获取知识图谱数据（所有笔记节点 + 双链关系边） */
  graph: async (): Promise<{ nodes: GraphNode[]; edges: GraphEdge[] }> => {
    return await http.get<{ nodes: GraphNode[]; edges: GraphEdge[] }>('/notes/graph')
  },
}

/** 知识图谱节点（笔记） */
export interface GraphNode {
  id: number
  title: string
  folderId: number | null
  tags: string[]
  updatedAt: string
}

/** 知识图谱边（笔记间的双链引用） */
export interface GraphEdge {
  source: number
  target: number
}
