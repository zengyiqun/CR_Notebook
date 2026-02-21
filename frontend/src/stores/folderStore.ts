import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Folder } from '@/types'
import { foldersApi } from '@/api/folders'

export const useFolderStore = defineStore('folder', () => {
  const folders = ref<Folder[]>([])
  const activeFolderId = ref<string | null>(null)
  const loading = ref(false)

  const activeFolder = computed(() =>
    folders.value.find((f) => f.id === activeFolderId.value) ?? null
  )

  const rootFolders = computed(() =>
    folders.value.filter((f) => !f.parentId).sort((a, b) => a.sortOrder - b.sortOrder)
  )

  function childrenOf(parentId: string) {
    return folders.value.filter((f) => f.parentId === parentId).sort((a, b) => a.sortOrder - b.sortOrder)
  }

  async function fetchFolders() {
    loading.value = true
    try {
      folders.value = await foldersApi.list()
      if (!activeFolderId.value && folders.value.length > 0) {
        activeFolderId.value = folders.value[0].id
      }
    } catch (e) {
      console.error('Failed to fetch folders:', e)
    } finally {
      loading.value = false
    }
  }

  async function addFolder(name: string, parentId: string | null = null) {
    try {
      const folder = await foldersApi.create(name, '📁', parentId)
      folders.value.push(folder)
      return folder
    } catch (e) {
      console.error('Failed to create folder:', e)
    }
  }

  async function updateFolder(id: string, updates: Partial<Pick<Folder, 'name' | 'icon'>>) {
    try {
      const updated = await foldersApi.update(id, updates)
      const idx = folders.value.findIndex((f) => f.id === id)
      if (idx !== -1) folders.value[idx] = updated
    } catch (e) {
      console.error('Failed to update folder:', e)
    }
  }

  async function deleteFolder(id: string) {
    try {
      await foldersApi.delete(id)
      const idsToRemove = collectDescendantIds(id)
      idsToRemove.add(id)
      folders.value = folders.value.filter((f) => !idsToRemove.has(f.id))
      if (activeFolderId.value && idsToRemove.has(activeFolderId.value)) {
        activeFolderId.value = folders.value[0]?.id ?? null
      }
    } catch (e) {
      console.error('Failed to delete folder:', e)
    }
  }

  function collectDescendantIds(parentId: string): Set<string> {
    const ids = new Set<string>()
    for (const f of folders.value) {
      if (f.parentId === parentId) {
        ids.add(f.id)
        for (const cid of collectDescendantIds(f.id)) {
          ids.add(cid)
        }
      }
    }
    return ids
  }

  function setActive(id: string | null) {
    activeFolderId.value = id
  }

  async function reorderFolders(orderedIds: string[], parentId: string | null) {
    const items = orderedIds.map((id, index) => ({ id: Number(id), sortOrder: index }))
    for (const item of items) {
      const f = folders.value.find(f => f.id === String(item.id))
      if (f) f.sortOrder = item.sortOrder
    }
    try {
      await foldersApi.reorder(items)
    } catch (e) {
      console.error('Failed to reorder folders:', e)
      await fetchFolders()
    }
  }

  return { folders, activeFolderId, activeFolder, rootFolders, loading, fetchFolders, addFolder, updateFolder, deleteFolder, setActive, childrenOf, reorderFolders }
})
