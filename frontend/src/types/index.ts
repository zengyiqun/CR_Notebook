export interface Folder {
  id: string
  name: string
  icon: string
  parentId: string | null
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface Note {
  id: string
  folderId: string | null
  title: string
  content: string // TipTap JSON string
  excerpt: string
  tags: string[]
  isPinned: boolean
  createdAt: string
  updatedAt: string
}

export interface Task {
  id: string
  noteId: string | null
  content: string
  completed: boolean
  dueDate: string | null
  priority: 'low' | 'medium' | 'high'
  createdAt: string
}

export interface DailyNote {
  id: string
  date: string // YYYY-MM-DD
  content: string
  weather: string
  mood: string
  createdAt: string
  updatedAt: string
}

export interface CalendarEvent {
  id: string
  title: string
  date: string
  time: string | null
  endDate: string | null
  endTime: string | null
  description: string
  color: string
}

export interface Whiteboard {
  id: string
  title: string
  data: string // JSON state
  createdAt: string
  updatedAt: string
}

export type ThemeMode = 'light' | 'dark'
export type SidebarSection = 'notes' | 'tasks' | 'calendar' | 'daily' | 'whiteboard'
