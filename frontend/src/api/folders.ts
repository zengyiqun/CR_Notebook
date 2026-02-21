import { http } from './http'
import type { Folder } from '@/types'

interface FolderResponse {
  id: number
  name: string
  icon: string
  parentId: number | null
  createdAt: string
  updatedAt: string
}

function mapToFolder(r: FolderResponse): Folder {
  return {
    id: String(r.id),
    name: r.name,
    icon: r.icon,
    parentId: r.parentId ? String(r.parentId) : null,
    createdAt: r.createdAt,
    updatedAt: r.updatedAt,
  }
}

export const foldersApi = {
  list: async (): Promise<Folder[]> => {
    const data = await http.get<FolderResponse[]>('/folders')
    return data.map(mapToFolder)
  },

  create: async (name: string, icon = '📁', parentId?: string | null): Promise<Folder> => {
    const data = await http.post<FolderResponse>('/folders', { name, icon, parentId: parentId ? Number(parentId) : null })
    return mapToFolder(data)
  },

  update: async (id: string, updates: { name?: string; icon?: string }): Promise<Folder> => {
    const data = await http.put<FolderResponse>(`/folders/${id}`, updates)
    return mapToFolder(data)
  },

  delete: (id: string) => http.delete(`/folders/${id}`),
}
