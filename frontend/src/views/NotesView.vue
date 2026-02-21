<script setup lang="ts">
import { computed, ref, watch, onUnmounted, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NoteList from '@/components/layout/NoteList.vue'
import TipTapEditor from '@/components/editor/TipTapEditor.vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { useNoteStore } from '@/stores/noteStore'
import type { Note } from '@/types'

const route = useRoute()
const router = useRouter()
const noteStore = useNoteStore()
const editorRef = ref<InstanceType<typeof TipTapEditor> | null>(null)
const editorPanel = ref<HTMLElement | null>(null)

const folderId = computed(() => (route.params.folderId as string) || null)
const activeNote = computed(() => noteStore.activeNote)
const editingTitle = ref(false)
const titleInput = ref('')

// Tag editing
const showTagInput = ref(false)
const newTag = ref('')

// Responsive: show/hide note list on mobile
const showNoteList = ref(true)

// Read/Edit mode: new notes start in edit, existing in read
const isEditing = ref(false)
const isNewNote = ref(false)

// Auto-save
const autoSaveEnabled = ref(true)
const saveStatus = ref<'saved' | 'saving' | 'unsaved'>('saved')
const pendingContent = ref<string | null>(null)
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

// Delete confirm modal
const showDeleteModal = ref(false)

// ── Bi-directional links ──
const backlinks = ref<Note[]>([])
const showBacklinks = ref(true)

const noteSuggestions = computed(() =>
  noteStore.notes
    .filter(n => n.id !== activeNote.value?.id)
    .map(n => ({ id: n.id, title: n.title }))
)

async function loadBacklinks(noteId: string) {
  backlinks.value = await noteStore.getBacklinks(noteId)
}

function navigateToNote(noteId: string) {
  const note = noteStore.notes.find(n => n.id === noteId)
  if (note) {
    if (note.folderId !== folderId.value) {
      router.push(note.folderId ? `/notes/folder/${note.folderId}` : '/notes')
    }
    noteStore.setActive(noteId)
  }
}

watch(activeNote, (note, oldNote) => {
  if (note) {
    const switched = !oldNote || oldNote.id !== note.id
    if (switched) {
      titleInput.value = note.title
      saveStatus.value = 'saved'
      pendingContent.value = null
      isNewNote.value = !note.content && note.title === '无标题'
      isEditing.value = isNewNote.value
      loadBacklinks(note.id)
      if (window.innerWidth < 768) {
        showNoteList.value = false
      }
    }
  } else {
    backlinks.value = []
  }
})

function startEditTitle() {
  if (!activeNote.value) return
  titleInput.value = activeNote.value.title
  editingTitle.value = true
}

function saveTitle() {
  if (!activeNote.value) {
    editingTitle.value = false
    return
  }
  const trimmed = titleInput.value.trim() || '无标题'
  if (trimmed !== activeNote.value.title) {
    noteStore.updateNote(activeNote.value.id, { title: trimmed })
  }
  titleInput.value = trimmed
  editingTitle.value = false
}

function doSave(content: string) {
  if (!activeNote.value) return
  saveStatus.value = 'saving'
  const excerpt = content.replace(/[#*`>\-\[\]()]/g, '').trim().slice(0, 100)
  noteStore.updateNote(activeNote.value.id, { content, excerpt }).then(() => {
    saveStatus.value = 'saved'
    pendingContent.value = null
  })
}

function onContentUpdate(content: string) {
  if (!activeNote.value) return
  pendingContent.value = content
  saveStatus.value = 'unsaved'

  if (autoSaveEnabled.value) {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    autoSaveTimer = setTimeout(() => doSave(content), 1500)
  }
}

function manualSave() {
  if (pendingContent.value !== null) {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    doSave(pendingContent.value)
  }
}

onUnmounted(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  if (pendingContent.value !== null && activeNote.value) {
    doSave(pendingContent.value)
  }
})

function togglePin() {
  if (!activeNote.value) return
  noteStore.updateNote(activeNote.value.id, { isPinned: !activeNote.value.isPinned })
}

function handleDelete() {
  if (!activeNote.value) return
  showDeleteModal.value = true
}

function confirmDelete() {
  showDeleteModal.value = false
  if (activeNote.value) {
    noteStore.deleteNote(activeNote.value.id)
    showNoteList.value = true
  }
}

function enterEditMode() {
  isEditing.value = true
}

function exitEditMode() {
  if (editingTitle.value) {
    saveTitle()
  }
  if (pendingContent.value !== null) {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    doSave(pendingContent.value)
  }
  isEditing.value = false
}

function addTag() {
  if (!showTagInput.value) return
  const trimmed = newTag.value.trim()
  if (!activeNote.value || !trimmed) {
    newTag.value = ''
    showTagInput.value = false
    return
  }
  const tag = trimmed.toLowerCase()
  if (!activeNote.value.tags.includes(tag)) {
    noteStore.updateNote(activeNote.value.id, {
      tags: [...activeNote.value.tags, tag],
    })
  }
  newTag.value = ''
}

function commitTagAndClose() {
  if (!showTagInput.value) return
  const trimmed = newTag.value.trim()
  if (activeNote.value && trimmed) {
    const tag = trimmed.toLowerCase()
    if (!activeNote.value.tags.includes(tag)) {
      noteStore.updateNote(activeNote.value.id, {
        tags: [...activeNote.value.tags, tag],
      })
    }
  }
  newTag.value = ''
  showTagInput.value = false
}

function removeTag(tag: string) {
  if (!activeNote.value) return
  noteStore.updateNote(activeNote.value.id, {
    tags: activeNote.value.tags.filter((t) => t !== tag),
  })
}

const showExportMenu = ref(false)

function exportMarkdown() {
  if (!editorRef.value || !activeNote.value) return
  const md = editorRef.value.exportMarkdown()
  const blob = new Blob([md], { type: 'text/markdown' })
  downloadBlob(blob, `${activeNote.value.title || '笔记'}.md`)
  showExportMenu.value = false
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

const PDF_CONTENT_WIDTH = 700

const exportStyles = `
  * { box-sizing: border-box; }
  h1 { font-size: 1.8em; font-weight: 700; margin: 0.6em 0 0.4em; line-height: 1.3; }
  h2 { font-size: 1.4em; font-weight: 600; margin: 0.8em 0 0.3em; line-height: 1.3; }
  h3 { font-size: 1.2em; font-weight: 600; margin: 0.8em 0 0.3em; line-height: 1.4; }
  p { margin: 0.4em 0; line-height: 1.8; }
  ul { padding-left: 1.5em; list-style-type: disc; margin: 0.4em 0; }
  ol { padding-left: 1.5em; list-style-type: decimal; margin: 0.4em 0; }
  ul ul { list-style-type: circle; }
  ul ul ul { list-style-type: square; }
  li { margin: 0.15em 0; display: list-item; line-height: 1.7; }
  li p { margin: 0; display: inline; }
  blockquote { border-left: 3px solid #6366f1; padding-left: 1em; margin: 0.6em 0; color: #555; font-style: italic; }
  code { background: #f3f4f6; border-radius: 3px; padding: 0.1em 0.35em; font-size: 0.85em; font-family: "SF Mono", Menlo, Consolas, monospace; }
  pre { background: #f8f9fa; border: 1px solid #e5e7eb; border-radius: 6px; padding: 0.8em 1em; margin: 0.6em 0; overflow-x: hidden; white-space: pre-wrap; word-break: break-all; }
  pre code { background: none; padding: 0; font-size: 0.82em; }
  hr { border: none; border-top: 1px solid #e5e7eb; margin: 1.5em 0; }
  mark { background: #fef08a; border-radius: 2px; padding: 0.05em 0.1em; }
  strong { font-weight: 700; }
  img { max-width: 100%; height: auto; }
  table { border-collapse: collapse; width: 100%; margin: 0.6em 0; }
  td, th { border: 1px solid #d1d5db; padding: 6px 10px; text-align: left; font-size: 0.9em; }
  th { background: #f9fafb; font-weight: 600; }
  ul[data-type="taskList"] { list-style: none; padding-left: 0; }
  ul[data-type="taskList"] li { display: flex; align-items: baseline; gap: 0.4em; }
  ul[data-type="taskList"] li::before { content: "☐"; font-size: 1em; flex-shrink: 0; }
  ul[data-type="taskList"] li[data-checked="true"]::before { content: "☑"; }
  ul[data-type="taskList"] li[data-checked="true"] p { text-decoration: line-through; color: #999; }
  ul[data-type="taskList"] li label { display: none; }
  .note-link-chip { color: #6366f1; font-weight: 500; text-decoration: none; }
  .note-link-chip::before { content: "[["; opacity: 0.5; }
  .note-link-chip::after { content: "]]"; opacity: 0.5; }
`

async function exportPdf() {
  if (!editorRef.value || !activeNote.value) return
  showExportMenu.value = false
  const html = editorRef.value.getHTML()
  const title = activeNote.value.title || '笔记'

  const wrapper = document.createElement('div')
  wrapper.style.cssText = 'position:fixed;left:-9999px;top:0;z-index:-1;visibility:hidden;'

  const container = document.createElement('div')
  container.style.cssText = `width:${PDF_CONTENT_WIDTH}px;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","Noto Sans SC",Roboto,sans-serif;font-size:14px;line-height:1.8;color:#1a1a1a;padding:40px 0;background:#fff;overflow-wrap:break-word;word-break:break-word;`

  const styleEl = document.createElement('style')
  styleEl.textContent = exportStyles
  container.appendChild(styleEl)

  const heading = document.createElement('h1')
  heading.style.cssText = 'font-size:1.6em;font-weight:700;margin:0 0 0.5em;line-height:1.3;border-bottom:2px solid #e5e7eb;padding-bottom:0.3em;'
  heading.textContent = title
  container.appendChild(heading)

  const content = document.createElement('div')
  content.innerHTML = html
  container.appendChild(content)

  wrapper.appendChild(container)
  document.body.appendChild(wrapper)

  try {
    const mod = await import('html2pdf.js')
    const html2pdf = mod.default || mod
    await html2pdf()
      .set({
        margin: [15, 15, 15, 15],
        filename: `${title}.pdf`,
        image: { type: 'jpeg', quality: 0.98 },
        html2canvas: {
          scale: 2,
          useCORS: true,
          backgroundColor: '#ffffff',
          scrollX: 0,
          scrollY: 0,
          windowWidth: PDF_CONTENT_WIDTH,
          letterRendering: true,
        },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
        pagebreak: { mode: ['avoid-all', 'css', 'legacy'] },
      })
      .from(container)
      .save()
  } finally {
    document.body.removeChild(wrapper)
  }
}

function sanitizeHtmlForWord(html: string): string {
  let result = html
  const tmp = document.createElement('div')
  tmp.innerHTML = result

  tmp.querySelectorAll('ul[data-type="taskList"]').forEach(ul => {
    ul.querySelectorAll('li').forEach(li => {
      const checked = li.getAttribute('data-checked') === 'true'
      const label = li.querySelector('label')
      if (label) label.remove()
      const text = li.textContent?.trim() || ''
      const prefix = checked ? '☑ ' : '☐ '
      const p = document.createElement('p')
      p.style.cssText = checked
        ? 'margin:2pt 0;text-indent:-1.5em;padding-left:1.5em;text-decoration:line-through;color:#999;'
        : 'margin:2pt 0;text-indent:-1.5em;padding-left:1.5em;'
      p.textContent = prefix + text
      li.replaceWith(p)
    })
    const parent = ul.parentNode
    if (parent) {
      while (ul.firstChild) parent.insertBefore(ul.firstChild, ul)
      ul.remove()
    }
  })

  tmp.querySelectorAll('.note-link-chip').forEach(chip => {
    const span = document.createElement('span')
    span.style.cssText = 'color:#6366f1;font-weight:bold;'
    span.textContent = `[[${chip.textContent?.trim()}]]`
    chip.replaceWith(span)
  })

  tmp.querySelectorAll('pre').forEach(pre => {
    const code = pre.querySelector('code')
    const text = (code || pre).textContent || ''
    const newPre = document.createElement('pre')
    newPre.style.cssText = 'font-family:Consolas,"Courier New",monospace;font-size:10pt;background-color:#f8f9fa;border:1pt solid #e0e0e0;padding:8pt 10pt;margin:6pt 0;white-space:pre-wrap;word-break:break-all;line-height:1.5;'
    newPre.textContent = text
    pre.replaceWith(newPre)
  })

  return tmp.innerHTML
}

function exportDocx() {
  if (!editorRef.value || !activeNote.value) return
  showExportMenu.value = false
  const rawHtml = editorRef.value.getHTML()
  const title = activeNote.value.title || '笔记'
  const html = sanitizeHtmlForWord(rawHtml)

  const docContent = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
<head><meta charset="utf-8"><title>${title}</title>
<!--[if gte mso 9]><xml>
<w:WordDocument>
  <w:View>Print</w:View>
  <w:Zoom>100</w:Zoom>
  <w:DoNotOptimizeForBrowser/>
</w:WordDocument>
</xml><![endif]-->
<style>
  @page {
    size: A4;
    margin: 2.54cm 3.17cm 2.54cm 3.17cm;
    mso-header-margin: 1.5cm;
    mso-footer-margin: 1.75cm;
  }
  body {
    font-family: "Microsoft YaHei", "PingFang SC", "Helvetica Neue", Arial, sans-serif;
    font-size: 12pt;
    line-height: 1.8;
    color: #333;
    mso-ascii-font-family: "Calibri";
    mso-hansi-font-family: "Calibri";
    mso-fareast-font-family: "Microsoft YaHei";
  }
  h1 {
    font-size: 22pt;
    font-weight: bold;
    margin-top: 0pt;
    margin-bottom: 8pt;
    padding-bottom: 6pt;
    border-bottom: 2pt solid #e5e7eb;
    line-height: 1.3;
    mso-style-name: "标题 1";
  }
  h2 {
    font-size: 16pt;
    font-weight: bold;
    margin-top: 14pt;
    margin-bottom: 6pt;
    line-height: 1.3;
    mso-style-name: "标题 2";
  }
  h3 {
    font-size: 14pt;
    font-weight: bold;
    margin-top: 12pt;
    margin-bottom: 4pt;
    line-height: 1.4;
    mso-style-name: "标题 3";
  }
  p {
    margin-top: 3pt;
    margin-bottom: 3pt;
    line-height: 1.8;
    mso-style-name: "正文";
  }
  ul {
    margin-top: 4pt;
    margin-bottom: 4pt;
    margin-left: 18pt;
    mso-list: l0 level1 lfo1;
  }
  ol {
    margin-top: 4pt;
    margin-bottom: 4pt;
    margin-left: 18pt;
  }
  li {
    margin-top: 2pt;
    margin-bottom: 2pt;
    line-height: 1.6;
  }
  li p {
    margin: 0;
    display: inline;
  }
  blockquote {
    border-left: 3pt solid #6366f1;
    padding-left: 10pt;
    margin: 8pt 0 8pt 4pt;
    color: #555;
    font-style: italic;
  }
  code {
    font-family: Consolas, "Courier New", monospace;
    font-size: 10pt;
    background-color: #f3f4f6;
    padding: 1pt 3pt;
    mso-highlight: #f3f4f6;
  }
  pre {
    font-family: Consolas, "Courier New", monospace;
    font-size: 10pt;
    background-color: #f8f9fa;
    border: 1pt solid #e0e0e0;
    padding: 8pt 10pt;
    margin: 6pt 0;
    white-space: pre-wrap;
    word-break: break-all;
    line-height: 1.5;
  }
  pre code { background: none; padding: 0; }
  hr {
    border: none;
    border-top: 1pt solid #d1d5db;
    margin-top: 12pt;
    margin-bottom: 12pt;
  }
  mark { background-color: #fef08a; mso-highlight: yellow; }
  strong { font-weight: bold; }
  em { font-style: italic; }
  table {
    border-collapse: collapse;
    width: 100%;
    margin: 6pt 0;
  }
  td, th {
    border: 1pt solid #bbb;
    padding: 4pt 8pt;
    text-align: left;
    font-size: 11pt;
    vertical-align: top;
  }
  th {
    background-color: #f3f4f6;
    font-weight: bold;
    mso-highlight: #f3f4f6;
  }
  img { max-width: 100%; }
</style></head>
<body><h1>${title}</h1>${html}</body></html>`

  const blob = new Blob(['\ufeff' + docContent], { type: 'application/msword' })
  downloadBlob(blob, `${title}.doc`)
}

function goBackToList() {
  noteStore.setActive(null)
  showNoteList.value = true
}

function closeExportMenu(e: MouseEvent) {
  if (showExportMenu.value) {
    const target = e.target as HTMLElement
    if (!target.closest('.export-menu-container')) {
      showExportMenu.value = false
    }
  }
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    manualSave()
  }
}

function onDocDblclick(e: MouseEvent) {
  if (!isEditing.value) return
  if (editorPanel.value?.contains(e.target as Node)) return
  exitEditMode()
}

onMounted(() => {
  document.addEventListener('click', closeExportMenu)
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('dblclick', onDocDblclick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', closeExportMenu)
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('dblclick', onDocDblclick)
})
</script>

<template>
  <div class="h-full flex">
    <!-- Note list panel -->
    <div
      class="h-full shrink-0 transition-all duration-300 overflow-hidden"
      :class="showNoteList ? 'w-72' : 'w-0 md:w-72'"
    >
      <NoteList :folder-id="folderId" />
    </div>

    <!-- Editor panel -->
    <div ref="editorPanel" class="flex-1 h-full flex flex-col bg-[var(--color-craft-surface)] min-w-0">
      <template v-if="activeNote">
        <!-- Note header -->
        <div class="px-6 pt-5 pb-3 border-b border-[var(--color-craft-border)]">
          <div class="flex items-center justify-between">
            <div class="flex-1 min-w-0">
              <!-- Mobile back button -->
              <button
                @click="goBackToList"
                class="md:hidden flex items-center gap-1 text-xs text-[var(--color-craft-accent)] font-medium mb-2 hover:opacity-80"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                返回列表
              </button>

              <input
                v-if="editingTitle && isEditing"
                v-model="titleInput"
                @blur="saveTitle"
                @keydown.enter="saveTitle"
                class="w-full text-2xl font-bold bg-transparent border-none outline-none text-[var(--color-craft-text)] placeholder:text-[var(--color-craft-text-secondary)]"
                placeholder="输入标题..."
                autofocus
              />
              <h1
                v-else
                @click="isEditing ? startEditTitle() : undefined"
                class="text-2xl font-bold text-[var(--color-craft-text)] truncate"
                :class="isEditing ? 'cursor-text' : 'cursor-default'"
              >
                {{ activeNote.title || '无标题' }}
              </h1>
            </div>
            <div class="flex items-center gap-1 ml-4">
              <!-- Edit / Read mode toggle -->
              <button
                v-if="!isEditing"
                @click="enterEditMode"
                class="h-7 px-3 flex items-center gap-1.5 rounded-lg text-xs font-medium bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                编辑
              </button>
              <template v-else>
                <!-- Save status -->
                <span class="text-[11px] px-2 py-0.5 rounded-full mr-1"
                  :class="saveStatus === 'saved' ? 'text-[var(--color-craft-success)] bg-emerald-50 dark:bg-emerald-900/20' : saveStatus === 'saving' ? 'text-amber-500 bg-amber-50 dark:bg-amber-900/20' : 'text-[var(--color-craft-text-secondary)] bg-[var(--color-craft-hover)]'"
                >
                  {{ saveStatus === 'saved' ? '已保存' : saveStatus === 'saving' ? '保存中' : '未保存' }}
                </span>
                <!-- Manual save -->
                <button
                  @click="manualSave"
                  :disabled="saveStatus === 'saved'"
                  class="h-7 px-2.5 flex items-center gap-1 rounded-lg text-xs font-medium transition-all"
                  :class="saveStatus !== 'saved' ? 'bg-[var(--color-craft-accent)] text-white hover:opacity-90' : 'bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] cursor-default'"
                  title="手动保存 (Ctrl+S)"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
                  保存
                </button>
                <!-- Auto-save toggle -->
                <button
                  @click="autoSaveEnabled = !autoSaveEnabled"
                  class="h-7 px-2.5 flex items-center gap-1 rounded-lg text-xs font-medium border transition-all"
                  :class="autoSaveEnabled ? 'border-[var(--color-craft-accent)] text-[var(--color-craft-accent)] bg-[var(--color-craft-accent-light)]' : 'border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
                  :title="autoSaveEnabled ? '自动保存：开启' : '自动保存：关闭'"
                >
                  {{ autoSaveEnabled ? '自动' : '手动' }}
                </button>
                <!-- Done editing -->
                <button
                  @click="exitEditMode"
                  class="h-7 px-2.5 flex items-center gap-1 rounded-lg text-xs font-medium border border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-all"
                >
                  完成
                </button>
              </template>
              <span class="w-px h-5 bg-[var(--color-craft-border)] mx-0.5"></span>
              <button
                @click="togglePin"
                class="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-[var(--color-craft-hover)] transition-colors"
                :class="activeNote.isPinned ? 'text-[var(--color-craft-accent)]' : 'text-[var(--color-craft-text-secondary)]'"
                :title="activeNote.isPinned ? '取消置顶' : '置顶'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" :fill="activeNote.isPinned ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 17v5"/><path d="M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V17h14v-1.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V6h1a2 2 0 0 0 0-4H8a2 2 0 0 0 0 4h1z"/></svg>
              </button>
              <div class="relative export-menu-container">
                <button
                  @click="showExportMenu = !showExportMenu"
                  class="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors"
                  title="导出"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
                </button>
                <Transition name="fade-slide">
                  <div
                    v-if="showExportMenu"
                    class="absolute right-0 top-full mt-1 z-50 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-xl overflow-hidden min-w-[140px]"
                  >
                    <button
                      @click="exportMarkdown"
                      class="w-full text-left px-3 py-2 text-xs hover:bg-[var(--color-craft-hover)] transition-colors flex items-center gap-2 text-[var(--color-craft-text)]"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
                      Markdown (.md)
                    </button>
                    <button
                      @click="exportPdf"
                      class="w-full text-left px-3 py-2 text-xs hover:bg-[var(--color-craft-hover)] transition-colors flex items-center gap-2 text-[var(--color-craft-text)]"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><path d="M9 15v-2h2a1.5 1.5 0 0 0 0-3H9v5"/></svg>
                      PDF (.pdf)
                    </button>
                    <button
                      @click="exportDocx"
                      class="w-full text-left px-3 py-2 text-xs hover:bg-[var(--color-craft-hover)] transition-colors flex items-center gap-2 text-[var(--color-craft-text)]"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><path d="M9 13h6"/><path d="M9 17h3"/></svg>
                      Word (.doc)
                    </button>
                  </div>
                </Transition>
              </div>
              <button
                @click="handleDelete"
                class="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] transition-colors"
                title="删除"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
              </button>
            </div>
          </div>

          <!-- Meta row: time + tags -->
          <div class="flex items-center gap-2 mt-2 flex-wrap">
            <span class="text-xs text-[var(--color-craft-text-secondary)]">
              {{ new Date(activeNote.updatedAt).toLocaleString('zh-CN') }}
            </span>
            <span class="text-[var(--color-craft-border)]">·</span>
            <!-- Tags -->
            <div class="flex items-center gap-1.5 flex-wrap">
              <span
                v-for="tag in activeNote.tags"
                :key="tag"
                class="group/tag inline-flex items-center gap-1 px-2 py-0.5 text-[11px] rounded-full bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-medium cursor-default"
              >
                #{{ tag }}
                <button
                  @click="removeTag(tag)"
                  class="w-3.5 h-3.5 flex items-center justify-center rounded-full opacity-0 group-hover/tag:opacity-100 hover:bg-[var(--color-craft-accent)] hover:text-white transition-all text-[10px] leading-none"
                >×</button>
              </span>
              <input
                v-if="showTagInput"
                v-model="newTag"
                @blur="commitTagAndClose"
                @keydown.enter.prevent="addTag"
                @keydown.escape="showTagInput = false; newTag = ''"
                class="w-20 px-2 py-0.5 text-[11px] rounded-full border border-[var(--color-craft-accent)] bg-transparent text-[var(--color-craft-text)] outline-none"
                placeholder="标签名"
                autofocus
              />
              <button
                v-if="!showTagInput"
                @click="showTagInput = true"
                class="inline-flex items-center gap-0.5 px-2 py-0.5 text-[11px] rounded-full border border-dashed border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:border-[var(--color-craft-accent)] hover:text-[var(--color-craft-accent)] transition-colors"
              >
                + 标签
              </button>
            </div>
          </div>
        </div>

        <!-- Editor -->
        <div class="flex-1 overflow-hidden flex flex-col">
          <div class="flex-1 overflow-hidden" @dblclick="!isEditing && enterEditMode()">
            <TipTapEditor
              ref="editorRef"
              :key="activeNote.id"
              :model-value="activeNote.content"
              :editable="isEditing"
              :notes="noteSuggestions"
              @update:model-value="onContentUpdate"
              @navigate-note="navigateToNote"
            />
          </div>

          <!-- Backlinks panel -->
          <div
            v-if="backlinks.length > 0"
            class="shrink-0 border-t border-[var(--color-craft-border)] bg-[var(--color-craft-bg)]"
          >
            <button
              @click="showBacklinks = !showBacklinks"
              class="w-full flex items-center gap-2 px-5 py-2 text-xs font-medium text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m9.915-3.173a4.5 4.5 0 0 0-1.242-7.244l-4.5-4.5a4.5 4.5 0 0 0-6.364 6.364L5.25 9.94" />
              </svg>
              <span>反向链接</span>
              <span class="ml-1 px-1.5 py-0.5 rounded-full bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] text-[10px] font-bold">{{ backlinks.length }}</span>
              <svg
                xmlns="http://www.w3.org/2000/svg" class="w-3 h-3 ml-auto transition-transform" :class="showBacklinks ? 'rotate-180' : ''"
                fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
              ><path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" /></svg>
            </button>
            <div v-if="showBacklinks" class="px-5 pb-3">
              <button
                v-for="bl in backlinks"
                :key="bl.id"
                @click="navigateToNote(bl.id)"
                class="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-left hover:bg-[var(--color-craft-hover)] transition-colors group"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 shrink-0 text-[var(--color-craft-text-secondary)] group-hover:text-[var(--color-craft-accent)]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                </svg>
                <div class="min-w-0">
                  <div class="font-medium text-[var(--color-craft-text)] truncate group-hover:text-[var(--color-craft-accent)]">{{ bl.title || '无标题' }}</div>
                  <div class="text-[11px] text-[var(--color-craft-text-secondary)] truncate">{{ bl.excerpt || '无内容' }}</div>
                </div>
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- Empty state -->
      <div v-else class="flex-1 flex items-center justify-center">
        <div class="text-center px-8">
          <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-indigo-100 to-purple-100 dark:from-indigo-900/30 dark:to-purple-900/30 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-accent)" stroke-width="1.5" stroke-linecap="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/><path d="m15 5 4 4"/></svg>
          </div>
          <h2 class="text-lg font-semibold text-[var(--color-craft-text)] mb-2">开始写作</h2>
          <p class="text-[var(--color-craft-text-secondary)] text-sm max-w-xs mx-auto leading-relaxed">
            从左侧选择一篇笔记，或点击 + 创建新笔记。<br/>
            支持 Markdown 语法，输入 <code class="px-1.5 py-0.5 rounded bg-[var(--color-craft-hover)] text-[var(--color-craft-accent)] text-xs font-mono">/</code> 唤出命令菜单。
          </p>
        </div>
      </div>
    </div>

    <ModalDialog
      :visible="showDeleteModal"
      type="confirm"
      title="删除笔记"
      message="确定删除这篇笔记？此操作不可恢复。"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDelete"
      @cancel="showDeleteModal = false"
    />
  </div>
</template>
