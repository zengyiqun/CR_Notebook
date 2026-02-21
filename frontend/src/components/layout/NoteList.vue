<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { useNoteStore } from '@/stores/noteStore'
import { useFolderStore } from '@/stores/folderStore'

const props = defineProps<{
  folderId?: string | null
}>()

const noteStore = useNoteStore()
const folderStore = useFolderStore()
const searchQuery = ref('')
const searchResults = ref<any[]>([])

const batchMode = ref(false)
const selectedIds = ref<Set<string>>(new Set())
const showBatchDeleteModal = ref(false)

onMounted(() => {
  noteStore.fetchNotes()
})

watch(() => searchQuery.value, async (q) => {
  if (q) {
    searchResults.value = await noteStore.searchNotes(q)
  } else {
    searchResults.value = []
  }
})

const displayNotes = computed(() => {
  if (searchQuery.value) {
    return searchResults.value
  }
  if (props.folderId) {
    return noteStore.notesByFolder(props.folderId).value
  }
  return noteStore.allNotes
})

const folderName = computed(() => {
  if (props.folderId) {
    const f = folderStore.folders.find((f) => f.id === props.folderId)
    return f ? f.name : '笔记'
  }
  return '所有笔记'
})

const noteCount = computed(() => displayNotes.value.length)

const allSelected = computed(() =>
  displayNotes.value.length > 0 && displayNotes.value.every(n => selectedIds.value.has(n.id))
)

const mdFileInput = ref<HTMLInputElement | null>(null)
const importError = ref('')
const showImportError = ref(false)

function createNote() {
  noteStore.addNote(props.folderId ?? null)
}

function triggerImport() {
  mdFileInput.value?.click()
}

async function handleMdImport(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    await noteStore.importMarkdown(props.folderId ?? null, file.name, text)
  } catch (err: any) {
    importError.value = err?.message || '导入失败'
    showImportError.value = true
  }
  if (mdFileInput.value) mdFileInput.value.value = ''
}

function selectNote(id: string) {
  if (batchMode.value) {
    toggleSelect(id)
    return
  }
  noteStore.setActive(id)
}

function toggleSelect(id: string) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(displayNotes.value.map(n => n.id))
  }
}

function enterBatchMode() {
  batchMode.value = true
  selectedIds.value = new Set()
}

function exitBatchMode() {
  batchMode.value = false
  selectedIds.value = new Set()
}

function confirmBatchDelete() {
  showBatchDeleteModal.value = false
  noteStore.deleteNotes([...selectedIds.value])
  exitBatchMode()
}

const showSingleDeleteModal = ref(false)
const deletingNoteId = ref<string | null>(null)

function askDeleteNote(id: string) {
  deletingNoteId.value = id
  showSingleDeleteModal.value = true
}

function confirmSingleDelete() {
  showSingleDeleteModal.value = false
  if (deletingNoteId.value) {
    noteStore.deleteNote(deletingNoteId.value)
    deletingNoteId.value = null
  }
}

const showSortMenu = ref(false)

const sortOptions = [
  { value: 'updatedAt' as const, label: '修改时间' },
  { value: 'createdAt' as const, label: '创建时间' },
  { value: 'title' as const, label: '标题' },
]

const currentSortLabel = computed(() =>
  sortOptions.find(o => o.value === noteStore.sortMode)?.label ?? '修改时间'
)

function setSortMode(mode: 'updatedAt' | 'createdAt' | 'title') {
  noteStore.sortMode = mode
  showSortMenu.value = false
}

function noteFolderName(note: any): string | null {
  if (props.folderId) return null
  if (!note.folderId) return '未分类'
  const f = folderStore.folders.find((f: any) => f.id === note.folderId)
  return f ? f.name : '未分类'
}

function onDragStart(event: DragEvent, noteId: string) {
  event.dataTransfer!.effectAllowed = 'move'
  event.dataTransfer!.setData('text/plain', noteId)
  event.dataTransfer!.setData('application/x-note-id', noteId)
}

function noteExcerpt(note: any): string {
  if (note.excerpt) return note.excerpt
  if (!note.content) return '空笔记'
  return note.content.replace(/[#*`>\-\[\]()!|]/g, '').replace(/\n+/g, ' ').trim().slice(0, 100) || '空笔记'
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="h-full flex flex-col bg-[var(--color-craft-surface)] border-r border-[var(--color-craft-border)]">
    <!-- Header -->
    <div class="px-4 pt-5 pb-2">
      <div class="flex items-center justify-between mb-3">
        <div>
          <h2 class="text-base font-semibold text-[var(--color-craft-text)]">{{ folderName }}</h2>
          <p class="text-[11px] text-[var(--color-craft-text-secondary)] mt-0.5">
            {{ noteCount }} 篇笔记
            <span v-if="noteStore.activeTagFilter" class="ml-1 px-1.5 py-0.5 rounded-full bg-[var(--color-craft-accent)] text-white text-[10px]">
              #{{ noteStore.activeTagFilter }}
            </span>
          </p>
        </div>
        <div class="flex items-center gap-1">
          <!-- Batch mode toggle -->
          <button
            v-if="!batchMode && displayNotes.length > 0"
            @click="enterBatchMode"
            class="w-8 h-8 flex items-center justify-center rounded-xl text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all"
            title="批量管理"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          </button>
          <input
            ref="mdFileInput"
            type="file"
            accept=".md,.markdown,text/markdown"
            class="hidden"
            @change="handleMdImport"
          />
          <button
            @click="triggerImport"
            class="w-8 h-8 flex items-center justify-center rounded-xl text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all"
            title="导入 Markdown"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          </button>
          <button
            @click="createNote"
            class="w-8 h-8 flex items-center justify-center rounded-xl bg-[var(--color-craft-accent)] text-white text-sm hover:opacity-90 transition-all shadow-sm active:scale-95"
            title="新建笔记"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg>
          </button>
        </div>
      </div>

      <!-- Batch toolbar -->
      <div v-if="batchMode" class="flex items-center justify-between mb-3 px-1 py-1.5 rounded-xl bg-[var(--color-craft-accent-light)] border border-[var(--color-craft-accent)]/20">
        <div class="flex items-center gap-2">
          <button
            @click="toggleSelectAll"
            class="w-5 h-5 rounded border-2 flex items-center justify-center transition-all"
            :class="allSelected ? 'bg-[var(--color-craft-accent)] border-[var(--color-craft-accent)]' : 'border-[var(--color-craft-text-secondary)]'"
          >
            <svg v-if="allSelected" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round"><path d="M20 6L9 17l-5-5"/></svg>
          </button>
          <span class="text-xs text-[var(--color-craft-accent)] font-medium">
            {{ selectedIds.size > 0 ? `已选 ${selectedIds.size} 项` : '全选' }}
          </span>
        </div>
        <div class="flex items-center gap-1">
          <button
            v-if="selectedIds.size > 0"
            @click="showBatchDeleteModal = true"
            class="h-6 px-2 flex items-center gap-1 rounded-lg text-[11px] font-medium bg-[var(--color-craft-danger)] text-white hover:opacity-90 transition-all"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
            删除
          </button>
          <button
            @click="exitBatchMode"
            class="h-6 px-2 rounded-lg text-[11px] font-medium text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-all"
          >
            取消
          </button>
        </div>
      </div>

      <!-- Search + Sort -->
      <div v-if="!batchMode" class="space-y-2">
        <div class="relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-craft-text-secondary)]" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索笔记..."
            class="w-full pl-9 pr-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] placeholder:text-[var(--color-craft-text-secondary)] focus:outline-none focus:border-[var(--color-craft-accent)] focus:ring-1 focus:ring-[var(--color-craft-accent)]/20 transition-all"
          />
        </div>
        <div class="relative flex items-center justify-end">
          <button
            @click="showSortMenu = !showSortMenu"
            class="flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 6h18"/><path d="M7 12h10"/><path d="M10 18h4"/></svg>
            {{ currentSortLabel }}
          </button>
          <Transition name="fade-slide">
            <div
              v-if="showSortMenu"
              class="absolute right-0 top-full mt-1 z-50 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-xl overflow-hidden min-w-[120px]"
            >
              <button
                v-for="opt in sortOptions"
                :key="opt.value"
                @click="setSortMode(opt.value)"
                class="w-full text-left px-3 py-2 text-xs hover:bg-[var(--color-craft-hover)] transition-colors flex items-center justify-between gap-2"
                :class="noteStore.sortMode === opt.value ? 'text-[var(--color-craft-accent)] font-medium bg-[var(--color-craft-accent-light)]' : 'text-[var(--color-craft-text)]'"
              >
                {{ opt.label }}
                <svg v-if="noteStore.sortMode === opt.value" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M20 6L9 17l-5-5"/></svg>
              </button>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <!-- Note list -->
    <div class="flex-1 overflow-y-auto px-2 py-1">
      <div
        v-if="displayNotes.length === 0"
        class="px-4 py-12 text-center"
      >
        <div class="w-12 h-12 mx-auto mb-3 rounded-2xl bg-[var(--color-craft-hover)] flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-text-secondary)" stroke-width="1.5" stroke-linecap="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <p class="text-sm text-[var(--color-craft-text-secondary)]">
          {{ searchQuery ? '没有找到匹配的笔记' : '还没有笔记' }}
        </p>
        <button
          v-if="!searchQuery"
          @click="createNote"
          class="mt-3 px-4 py-1.5 text-xs font-medium rounded-lg bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-opacity"
        >
          创建第一篇
        </button>
      </div>
      <div
        v-for="note in displayNotes"
        :key="note.id"
        class="group relative"
        draggable="true"
        @dragstart="onDragStart($event, note.id)"
      >
        <button
          @click="selectNote(note.id)"
          class="w-full text-left px-3 py-3 rounded-xl mb-0.5 transition-all duration-150"
          :class="[
            !batchMode && noteStore.activeNoteId === note.id
              ? 'bg-[var(--color-craft-accent-light)] shadow-sm'
              : 'hover:bg-[var(--color-craft-hover)]',
            batchMode && selectedIds.has(note.id) ? 'bg-[var(--color-craft-accent-light)]' : ''
          ]"
        >
          <div class="flex items-start gap-2">
            <!-- Batch checkbox -->
            <div
              v-if="batchMode"
              class="w-4 h-4 mt-0.5 shrink-0 rounded border-2 flex items-center justify-center transition-all"
              :class="selectedIds.has(note.id) ? 'bg-[var(--color-craft-accent)] border-[var(--color-craft-accent)]' : 'border-[var(--color-craft-text-secondary)]'"
            >
              <svg v-if="selectedIds.has(note.id)" xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" stroke-linecap="round"><path d="M20 6L9 17l-5-5"/></svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-1">
                <h3 class="text-sm font-medium text-[var(--color-craft-text)] truncate flex-1">
                  <svg v-if="note.isPinned" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="var(--color-craft-accent)" stroke="var(--color-craft-accent)" stroke-width="2" class="inline-block mr-1 -mt-0.5 opacity-70"><path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1z"/></svg>
                  {{ note.title || '无标题' }}
                </h3>
                <div class="flex items-center gap-0.5 shrink-0 mt-0.5">
                  <span class="text-[11px] text-[var(--color-craft-text-secondary)] whitespace-nowrap">
                    {{ formatDate(noteStore.sortMode === 'createdAt' ? note.createdAt : note.updatedAt) }}
                  </span>
                  <span
                    v-if="!batchMode"
                    @click.stop="askDeleteNote(note.id)"
                    class="w-5 h-5 flex items-center justify-center rounded opacity-0 group-hover:opacity-100 hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] transition-all cursor-pointer"
                    title="删除"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
                  </span>
                </div>
              </div>
              <p class="text-xs text-[var(--color-craft-text-secondary)] mt-1.5 line-clamp-2 leading-relaxed">
                {{ noteExcerpt(note) }}
              </p>
              <div class="flex items-center gap-1.5 mt-1.5 flex-wrap">
                <span
                  v-if="noteFolderName(note)"
                  class="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] rounded-md font-medium"
                  :class="note.folderId
                    ? 'bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)]'
                    : 'bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] opacity-60'"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                  {{ noteFolderName(note) }}
                </span>
              </div>
              <div v-if="note.tags.length" class="flex gap-1 mt-1 flex-wrap">
                <span
                  v-for="tag in note.tags.slice(0, 3)"
                  :key="tag"
                  class="px-1.5 py-0.5 text-[10px] rounded-md bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-medium"
                >
                  #{{ tag }}
                </span>
                <span v-if="note.tags.length > 3" class="text-[10px] text-[var(--color-craft-text-secondary)]">
                  +{{ note.tags.length - 3 }}
                </span>
              </div>
            </div>
          </div>
        </button>
      </div>
    </div>

    <ModalDialog
      :visible="showBatchDeleteModal"
      type="confirm"
      title="批量删除"
      :message="`确定删除选中的 ${selectedIds.size} 篇笔记？此操作不可恢复。`"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmBatchDelete"
      @cancel="showBatchDeleteModal = false"
    />

    <ModalDialog
      :visible="showSingleDeleteModal"
      type="confirm"
      title="删除笔记"
      message="确定删除这篇笔记？此操作不可恢复。"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmSingleDelete"
      @cancel="showSingleDeleteModal = false; deletingNoteId = null"
    />

    <ModalDialog
      :visible="showImportError"
      type="alert"
      title="导入失败"
      :message="importError"
      confirm-text="确定"
      @confirm="showImportError = false"
      @cancel="showImportError = false"
    />
  </div>
</template>
