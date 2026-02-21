<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import type { Folder } from '@/types'
import { useFolderStore } from '@/stores/folderStore'
import { useNoteStore } from '@/stores/noteStore'
import { useRoute, useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  folder: Folder
  depth?: number
  showCount?: boolean
  siblings?: Folder[]
}>(), { depth: 0, showCount: false })

const emit = defineEmits<{
  (e: 'addSubFolder', parentId: string): void
  (e: 'deleteFolder', id: string): void
  (e: 'reorder', orderedIds: string[], parentId: string | null): void
}>()

const folderStore = useFolderStore()
const noteStore = useNoteStore()
const route = useRoute()
const router = useRouter()

function countNotesInFolder(folderId: string): number {
  const direct = noteStore.notes.filter(n => n.folderId === folderId).length
  const childFolders = folderStore.childrenOf(folderId)
  return direct + childFolders.reduce((sum, c) => sum + countNotesInFolder(c.id), 0)
}

const noteCount = computed(() => countNotesInFolder(props.folder.id))

const expanded = ref(true)
const children = computed(() => folderStore.childrenOf(props.folder.id))
const hasChildren = computed(() => children.value.length > 0)
const isActive = computed(() => route.params.folderId === props.folder.id)

const isRenaming = ref(false)
const renameValue = ref('')
const renameInput = ref<HTMLInputElement | null>(null)

function toggle(e: Event) {
  e.stopPropagation()
  expanded.value = !expanded.value
}

function select() {
  if (isRenaming.value) return
  folderStore.setActive(props.folder.id)
  router.push(`/notes/folder/${props.folder.id}`)
}

function startRename() {
  renameValue.value = props.folder.name
  isRenaming.value = true
  nextTick(() => {
    renameInput.value?.focus()
    renameInput.value?.select()
  })
}

function commitRename() {
  if (!isRenaming.value) return
  const trimmed = renameValue.value.trim()
  if (trimmed && trimmed !== props.folder.name) {
    folderStore.updateFolder(props.folder.id, { name: trimmed })
  }
  isRenaming.value = false
}

function cancelRename() {
  isRenaming.value = false
}

const isDragOver = ref(false)
const folderDragPosition = ref<'before' | 'after' | null>(null)

function onDragStart(e: DragEvent) {
  e.dataTransfer!.setData('application/x-folder-id', props.folder.id)
  e.dataTransfer!.effectAllowed = 'move'
}

function onDragOver(e: DragEvent) {
  if (e.dataTransfer?.types.includes('application/x-folder-id')) {
    const draggingId = e.dataTransfer.getData('application/x-folder-id')
    if (draggingId === props.folder.id) return
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const y = e.clientY - rect.top
    folderDragPosition.value = y < rect.height / 2 ? 'before' : 'after'
    isDragOver.value = false
    return
  }
  if (!e.dataTransfer?.types.includes('application/x-note-id')) return
  e.preventDefault()
  e.dataTransfer!.dropEffect = 'move'
  isDragOver.value = true
  folderDragPosition.value = null
}

function onDragLeave() {
  isDragOver.value = false
  folderDragPosition.value = null
}

function onDrop(e: DragEvent) {
  const folderId = e.dataTransfer?.getData('application/x-folder-id')
  if (folderId && folderId !== props.folder.id && props.siblings) {
    e.preventDefault()
    isDragOver.value = false
    const position = folderDragPosition.value
    folderDragPosition.value = null
    const ids = props.siblings.map(f => f.id).filter(id => id !== folderId)
    const targetIdx = ids.indexOf(props.folder.id)
    if (targetIdx < 0) return
    const insertIdx = position === 'after' ? targetIdx + 1 : targetIdx
    ids.splice(insertIdx, 0, folderId)
    emit('reorder', ids, props.folder.parentId ?? null)
    return
  }

  isDragOver.value = false
  folderDragPosition.value = null
  const noteId = e.dataTransfer?.getData('application/x-note-id')
  if (!noteId) return
  e.preventDefault()
  noteStore.updateNote(noteId, { folderId: props.folder.id })
}
</script>

<template>
  <div>
    <div class="group relative">
      <!-- Drop indicator line (before) -->
      <div v-if="folderDragPosition === 'before'" class="absolute left-3 right-3 top-0 h-0.5 bg-[var(--color-craft-accent)] rounded-full z-10"></div>
      <button
        draggable="true"
        @dragstart="onDragStart"
        @click="select"
        @dblclick.prevent="startRename"
        @dragover="onDragOver"
        @dragleave="onDragLeave"
        @drop="onDrop"
        class="w-full flex items-center gap-1.5 py-1.5 rounded-xl text-sm transition-all duration-150 pr-2"
        :style="{ paddingLeft: `${12 + depth * 16}px` }"
        :class="[
          isActive
            ? 'bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-medium'
            : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)]',
          isDragOver ? 'ring-2 ring-[var(--color-craft-accent)] bg-[var(--color-craft-accent-light)]' : ''
        ]"
      >
        <!-- Expand/collapse chevron -->
        <span
          @click="toggle"
          class="w-4 h-4 flex items-center justify-center shrink-0 rounded transition-all"
          :class="hasChildren ? 'cursor-pointer hover:bg-[var(--color-craft-hover)]' : 'opacity-0'"
        >
          <svg v-if="hasChildren" xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" class="transition-transform duration-150" :class="expanded ? 'rotate-90' : ''"><path d="M9 18l6-6-6-6"/></svg>
        </span>
        <!-- Folder SVG icon -->
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" class="shrink-0" :class="isActive ? 'text-[var(--color-craft-accent)]' : ''">
          <template v-if="expanded && hasChildren">
            <path d="M5 19a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2v1"/>
            <path d="M20.5 19H3.3a1.5 1.5 0 0 1-1.46-1.85l1.5-6A1.5 1.5 0 0 1 4.8 10h16.7a1.5 1.5 0 0 1 1.46 1.85l-1 4A1.5 1.5 0 0 1 20.5 17z"/>
          </template>
          <template v-else>
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
          </template>
        </svg>
        <!-- Inline rename input or folder name -->
        <input
          v-if="isRenaming"
          ref="renameInput"
          v-model="renameValue"
          @blur="commitRename"
          @keydown.enter.prevent.stop="commitRename"
          @keydown.escape.prevent.stop="cancelRename"
          @click.stop
          @dblclick.stop
          class="flex-1 min-w-0 text-sm bg-[var(--color-craft-bg)] border border-[var(--color-craft-accent)] rounded-md px-1.5 py-0 text-[var(--color-craft-text)] outline-none"
        />
        <span v-else class="truncate flex-1 text-left">{{ folder.name }}</span>
        <span
          v-if="!isRenaming && showCount && noteCount > 0"
          class="text-[10px] px-1.5 py-0.5 rounded-full font-medium min-w-[18px] text-center shrink-0 ml-auto group-hover:hidden"
          :class="isActive
            ? 'bg-[var(--color-craft-accent)] text-white/80'
            : 'bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)]'"
        >{{ noteCount }}</span>
        <!-- Action buttons -->
        <span v-if="!isRenaming" class="hidden items-center gap-0.5 group-hover:flex shrink-0 ml-auto">
          <span
            @click.stop="startRename"
            class="w-5 h-5 flex items-center justify-center rounded hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-accent)] transition-all cursor-pointer"
            title="重命名"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
          </span>
          <span
            @click.stop="emit('addSubFolder', folder.id)"
            class="w-5 h-5 flex items-center justify-center rounded hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-accent)] transition-all cursor-pointer"
            title="新建子文件夹"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg>
          </span>
          <span
            @click.stop="emit('deleteFolder', folder.id)"
            class="w-5 h-5 flex items-center justify-center rounded hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] transition-all cursor-pointer"
            title="删除"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
          </span>
        </span>
      </button>
      <!-- Drop indicator line (after) -->
      <div v-if="folderDragPosition === 'after'" class="absolute left-3 right-3 bottom-0 h-0.5 bg-[var(--color-craft-accent)] rounded-full z-10"></div>
    </div>
    <!-- Children (recursive) -->
    <div v-if="hasChildren && expanded">
      <FolderTreeItem
        v-for="child in children"
        :key="child.id"
        :folder="child"
        :depth="depth + 1"
        :show-count="showCount"
        :siblings="children"
        @add-sub-folder="emit('addSubFolder', $event)"
        @delete-folder="emit('deleteFolder', $event)"
        @reorder="(ids: string[], pid: string | null) => emit('reorder', ids, pid)"
      />
    </div>
  </div>
</template>
