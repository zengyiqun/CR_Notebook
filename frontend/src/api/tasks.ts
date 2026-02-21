import { http } from './http'
import type { Task } from '@/types'

interface TaskResponse {
  id: number
  noteId: number | null
  content: string
  completed: boolean
  priority: string
  dueDate: string | null
  createdAt: string
  updatedAt: string
}

function mapToTask(r: TaskResponse): Task {
  return {
    id: String(r.id),
    noteId: r.noteId ? String(r.noteId) : null,
    content: r.content,
    completed: r.completed,
    priority: r.priority.toLowerCase() as Task['priority'],
    dueDate: r.dueDate,
    createdAt: r.createdAt,
  }
}

export const tasksApi = {
  list: async (): Promise<Task[]> => {
    const data = await http.get<TaskResponse[]>('/tasks')
    return data.map(mapToTask)
  },

  create: async (content: string, priority = 'MEDIUM', dueDate: string | null = null): Promise<Task> => {
    const data = await http.post<TaskResponse>('/tasks', { content, priority: priority.toUpperCase(), dueDate })
    return mapToTask(data)
  },

  update: async (id: string, updates: Partial<{ content: string; completed: boolean; priority: string; dueDate: string | null }>): Promise<Task> => {
    const data = await http.put<TaskResponse>(`/tasks/${id}`, {
      ...updates,
      priority: updates.priority?.toUpperCase(),
    })
    return mapToTask(data)
  },

  delete: (id: string) => http.delete(`/tasks/${id}`),
}
