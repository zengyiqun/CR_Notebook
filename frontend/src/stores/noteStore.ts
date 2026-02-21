import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Note } from '@/types'
import { notesApi } from '@/api/notes'

export const useNoteStore = defineStore('note', () => {
  const notes = ref<Note[]>([])
  const activeNoteId = ref<string | null>(null)
  const activeTagFilter = ref<string | null>(null)
  const sortMode = ref<'updatedAt' | 'createdAt' | 'title'>('updatedAt')
  const loading = ref(false)

  const activeNote = computed(() =>
    notes.value.find((n) => n.id === activeNoteId.value) ?? null
  )

  function sortFn(a: Note, b: Note): number {
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1
    switch (sortMode.value) {
      case 'createdAt':
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      case 'title':
        return a.title.localeCompare(b.title, 'zh-CN')
      default:
        return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    }
  }

  function notesByFolder(folderId: string | null) {
    return computed(() => {
      let filtered = folderId
        ? notes.value.filter((n) => n.folderId === folderId)
        : notes.value
      if (activeTagFilter.value) {
        filtered = filtered.filter(n => n.tags?.includes(activeTagFilter.value!))
      }
      return [...filtered].sort(sortFn)
    })
  }

  const allNotes = computed(() => {
    let filtered = [...notes.value]
    if (activeTagFilter.value) {
      filtered = filtered.filter(n => n.tags?.includes(activeTagFilter.value!))
    }
    return filtered.sort(sortFn)
  })

  async function fetchNotes(folderId?: string | null) {
    loading.value = true
    try {
      notes.value = await notesApi.list(folderId)
    } catch (e) {
      console.error('Failed to fetch notes:', e)
    } finally {
      loading.value = false
    }
  }

  async function addNote(folderId: string | null, title = '无标题') {
    try {
      const note = await notesApi.create({ folderId, title, content: '', excerpt: '', isPinned: false, tags: [] })
      notes.value.unshift(note)
      activeNoteId.value = note.id
      return note
    } catch (e) {
      console.error('Failed to create note:', e)
    }
  }

  async function importMarkdown(folderId: string | null, fileName: string, markdown: string) {
    const h1Match = markdown.match(/^#\s+(.+)$/m)
    let title = h1Match ? h1Match[1].trim() : fileName.replace(/\.md$/i, '')
    if (!title) title = '导入的笔记'

    const plain = markdown
      .replace(/^#+\s+/gm, '')
      .replace(/[*_~`>]/g, '')
      .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
      .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
      .trim()
    const excerpt = plain.slice(0, 100)

    try {
      const note = await notesApi.create({
        folderId,
        title,
        content: markdown,
        excerpt,
        isPinned: false,
        tags: [],
      })
      notes.value.unshift(note)
      activeNoteId.value = note.id
      return note
    } catch (e) {
      console.error('Failed to import markdown:', e)
      throw e
    }
  }

  async function updateNote(id: string, updates: Partial<Pick<Note, 'title' | 'content' | 'excerpt' | 'tags' | 'isPinned' | 'folderId'>>) {
    const note = notes.value.find((n) => n.id === id)
    if (note) {
      Object.assign(note, updates, { updatedAt: new Date().toISOString() })
    }
    try {
      const updated = await notesApi.update(id, updates)
      const idx = notes.value.findIndex((n) => n.id === id)
      if (idx !== -1) {
        Object.assign(notes.value[idx], { updatedAt: updated.updatedAt })
      }
    } catch (e) {
      console.error('Failed to update note:', e)
    }
  }

  async function deleteNote(id: string) {
    try {
      await notesApi.delete(id)
      notes.value = notes.value.filter((n) => n.id !== id)
      if (activeNoteId.value === id) {
        activeNoteId.value = notes.value[0]?.id ?? null
      }
    } catch (e) {
      console.error('Failed to delete note:', e)
    }
  }

  async function deleteNotes(ids: string[]) {
    try {
      await Promise.all(ids.map(id => notesApi.delete(id)))
      notes.value = notes.value.filter(n => !ids.includes(n.id))
      if (activeNoteId.value && ids.includes(activeNoteId.value)) {
        activeNoteId.value = notes.value[0]?.id ?? null
      }
    } catch (e) {
      console.error('Failed to delete notes:', e)
    }
  }

  function setActive(id: string | null) {
    activeNoteId.value = id
  }

  async function searchNotes(query: string) {
    try {
      return await notesApi.search(query)
    } catch (e) {
      console.error('Failed to search notes:', e)
      return []
    }
  }

  async function getBacklinks(noteId: string) {
    try {
      return await notesApi.backlinks(noteId)
    } catch (e) {
      console.error('Failed to fetch backlinks:', e)
      return []
    }
  }

  return { notes, activeNoteId, activeTagFilter, sortMode, activeNote, allNotes, loading, notesByFolder, fetchNotes, addNote, importMarkdown, updateNote, deleteNote, deleteNotes, setActive, searchNotes, getBacklinks }
})
