<script setup lang="ts">
import { ref, watch, computed, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Highlight from '@tiptap/extension-highlight'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import { Markdown } from 'tiptap-markdown'
import SlashCommandMenu from './SlashCommandMenu.vue'
import NoteLinkSuggestion from './NoteLinkSuggestion.vue'
import type { NoteSuggestion } from './NoteLinkSuggestion.vue'
import { NoteLink } from './extensions/NoteLink'
import '@/styles/editor.css'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  editable?: boolean
  notes?: NoteSuggestion[]
}>(), {
  editable: true,
  notes: () => [],
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'blur'): void
  (e: 'navigateNote', noteId: string): void
}>()

const slashMenu = ref({ visible: false, x: 0, y: 0 })
const slashMenuRef = ref<InstanceType<typeof SlashCommandMenu> | null>(null)

// ── Note Link Suggestion ──
const linkMenu = ref({ visible: false, x: 0, y: 0, query: '', fromPos: 0 })
const linkMenuRef = ref<InstanceType<typeof NoteLinkSuggestion> | null>(null)

function detectNoteLink(ed: any) {
  const $pos = ed.state.selection.$from
  const paraStart = $pos.start()
  const textBeforeCursor = ed.state.doc.textBetween(paraStart, $pos.pos, '')
  const bracketIdx = textBeforeCursor.lastIndexOf('[[')
  if (bracketIdx !== -1 && !textBeforeCursor.slice(bracketIdx).includes(']]')) {
    const query = textBeforeCursor.slice(bracketIdx + 2)
    const fromPos = paraStart + bracketIdx
    const coords = ed.view.coordsAtPos(ed.state.selection.from)
    linkMenu.value = { visible: true, x: coords.left, y: coords.bottom + 4, query, fromPos }
  } else if (linkMenu.value.visible) {
    linkMenu.value.visible = false
  }
}

function handleNoteLinkSelect(note: NoteSuggestion) {
  if (!editor.value) return
  const from = linkMenu.value.fromPos
  const to = editor.value.state.selection.from
  editor.value.chain().focus()
    .deleteRange({ from, to })
    .insertNoteLink({ noteId: note.id, noteTitle: note.title || '无标题' })
    .run()
  linkMenu.value.visible = false
}

// ── TOC ──
interface TocItem { level: number; text: string; pos: number }
const showToc = ref(false)
const tocItems = ref<TocItem[]>([])
let tocVersion = 0

function extractHeadings() {
  if (!editor.value) { tocItems.value = []; return }
  const items: TocItem[] = []
  editor.value.state.doc.descendants((node, pos) => {
    if (node.type.name === 'heading') {
      items.push({ level: node.attrs.level, text: node.textContent, pos })
    }
  })
  tocItems.value = items
}

function scrollToHeading(pos: number) {
  if (!editor.value) return
  const dom = editor.value.view.domAtPos(pos + 1)
  const el = dom.node instanceof HTMLElement ? dom.node : dom.node.parentElement
  el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  editor.value.chain().focus().setTextSelection(pos + 1).run()
}

const editor = useEditor({
  content: props.modelValue || '',
  editable: props.editable,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
    }),
    Placeholder.configure({
      placeholder: props.placeholder || '输入 / 唤出命令菜单...',
    }),
    Highlight,
    TaskList,
    TaskItem.configure({ nested: true }),
    Markdown.configure({
      html: true,
      transformPastedText: true,
      transformCopiedText: true,
    }),
    NoteLink.configure({
      onNavigate: (noteId: string) => emit('navigateNote', noteId),
    }),
  ],
  editorProps: {
    attributes: {
      class: 'prose prose-sm max-w-none focus:outline-none min-h-full',
    },
    handleKeyDown: (_view, event) => {
      if (linkMenu.value.visible) {
        linkMenuRef.value?.onKeydown(event)
        if (['ArrowUp', 'ArrowDown', 'Enter', 'Escape'].includes(event.key)) {
          return true
        }
      }
      if (slashMenu.value.visible) {
        slashMenuRef.value?.onKeydown(event)
        if (['ArrowUp', 'ArrowDown', 'Enter', 'Escape'].includes(event.key)) {
          return true
        }
      }
      return false
    },
    handlePaste: (view, event) => {
      const text = event.clipboardData?.getData('text/plain')
      const html = event.clipboardData?.getData('text/html')
      if (!text || !html) return false

      const mdPattern = /(?:^|\n)(?:#{1,6}\s|```|[-*+]\s\S|\d+\.\s\S|>\s|!\[|\[.+?\]\(.+?\))/m
      if (!mdPattern.test(text)) return false

      try {
        const mdSlice = (view as any).someProp('clipboardTextParser', (parser: any) =>
          parser(text, view.state.selection.$from)
        )
        if (mdSlice) {
          view.dispatch(view.state.tr.replaceSelection(mdSlice).scrollIntoView())
          return true
        }
      } catch {
        // Fall through to default paste
      }
      return false
    },
  },
  onBlur: () => {
    emit('blur')
  },
  onCreate: () => { extractHeadings() },
  onTransaction: ({ transaction }) => {
    if (transaction.getMeta('noteLinkConversion')) return
    if (transaction.docChanged && editor.value) {
      emit('update:modelValue', editor.value.storage.markdown.getMarkdown())
      const v = ++tocVersion
      setTimeout(() => { if (v === tocVersion) extractHeadings() }, 300)
    }
    if (editor.value) detectNoteLink(editor.value)
  },
  onUpdate: ({ editor: ed }) => {
    const text = ed.state.doc.textBetween(
      Math.max(0, ed.state.selection.from - 1),
      ed.state.selection.from,
      ''
    )

    if (text === '/') {
      const coords = ed.view.coordsAtPos(ed.state.selection.from)
      slashMenu.value = { visible: true, x: coords.left, y: coords.bottom + 4 }
    } else if (slashMenu.value.visible && text === '') {
      slashMenu.value.visible = false
    }
  },
})

watch(() => props.editable, (val) => {
  editor.value?.setEditable(val)
})

watch(() => props.modelValue, (val) => {
  if (!editor.value) return
  const current = editor.value.storage.markdown.getMarkdown()
  if (val !== current) {
    editor.value.commands.setContent(val || '')
    extractHeadings()
  }
})

function handleSlashCommand(command: string) {
  if (!editor.value) return

  // Remove the slash character
  editor.value.chain().focus()
    .deleteRange({ from: editor.value.state.selection.from - 1, to: editor.value.state.selection.from })
    .run()

  const chain = editor.value.chain().focus()

  switch (command) {
    case 'heading1': chain.toggleHeading({ level: 1 }).run(); break
    case 'heading2': chain.toggleHeading({ level: 2 }).run(); break
    case 'heading3': chain.toggleHeading({ level: 3 }).run(); break
    case 'bulletList': chain.toggleBulletList().run(); break
    case 'orderedList': chain.toggleOrderedList().run(); break
    case 'taskList': chain.toggleTaskList().run(); break
    case 'blockquote': chain.toggleBlockquote().run(); break
    case 'codeBlock': chain.toggleCodeBlock().run(); break
    case 'horizontalRule': chain.setHorizontalRule().run(); break
  }

  slashMenu.value.visible = false
}

function exportMarkdown(): string {
  if (!editor.value) return ''
  return editor.value.storage.markdown.getMarkdown()
}

function getHTML(): string {
  if (!editor.value) return ''
  return editor.value.getHTML()
}

defineExpose({ exportMarkdown, getHTML, editor, showToc, tocItems, scrollToHeading, linkMenu })

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="tiptap-editor h-full">
    <!-- Toolbar -->
    <div
      v-if="editor && editable"
      class="flex items-center gap-1 px-2 py-1.5 border-b border-[var(--color-craft-border)] bg-[var(--color-craft-surface)] flex-wrap"
    >
      <button
        v-for="btn in [
          { cmd: () => editor!.chain().focus().toggleBold().run(), active: editor.isActive('bold'), label: 'B', title: '粗体' },
          { cmd: () => editor!.chain().focus().toggleItalic().run(), active: editor.isActive('italic'), label: 'I', title: '斜体' },
          { cmd: () => editor!.chain().focus().toggleStrike().run(), active: editor.isActive('strike'), label: 'S', title: '删除线' },
          { cmd: () => editor!.chain().focus().toggleHighlight().run(), active: editor.isActive('highlight'), label: 'H', title: '高亮' },
          { cmd: () => editor!.chain().focus().toggleCode().run(), active: editor.isActive('code'), label: '⌘', title: '行内代码' },
        ]"
        :key="btn.title"
        @click="btn.cmd"
        class="w-7 h-7 flex items-center justify-center rounded-md text-xs font-bold transition-colors"
        :class="btn.active
          ? 'bg-[var(--color-craft-accent)] text-white'
          : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
        :title="btn.title"
      >
        {{ btn.label }}
      </button>

      <div class="w-px h-5 bg-[var(--color-craft-border)] mx-1" />

      <button
        v-for="btn in [
          { cmd: () => editor!.chain().focus().toggleHeading({ level: 1 }).run(), active: editor.isActive('heading', { level: 1 }), label: 'H1' },
          { cmd: () => editor!.chain().focus().toggleHeading({ level: 2 }).run(), active: editor.isActive('heading', { level: 2 }), label: 'H2' },
          { cmd: () => editor!.chain().focus().toggleHeading({ level: 3 }).run(), active: editor.isActive('heading', { level: 3 }), label: 'H3' },
        ]"
        :key="btn.label"
        @click="btn.cmd"
        class="w-7 h-7 flex items-center justify-center rounded-md text-xs font-bold transition-colors"
        :class="btn.active
          ? 'bg-[var(--color-craft-accent)] text-white'
          : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
      >
        {{ btn.label }}
      </button>

      <div class="w-px h-5 bg-[var(--color-craft-border)] mx-1" />

      <button
        v-for="btn in [
          { cmd: () => editor!.chain().focus().toggleBulletList().run(), active: editor.isActive('bulletList'), label: '•', title: '无序列表' },
          { cmd: () => editor!.chain().focus().toggleOrderedList().run(), active: editor.isActive('orderedList'), label: '1.', title: '有序列表' },
          { cmd: () => editor!.chain().focus().toggleTaskList().run(), active: editor.isActive('taskList'), label: '☑', title: '任务列表' },
          { cmd: () => editor!.chain().focus().toggleBlockquote().run(), active: editor.isActive('blockquote'), label: '❝', title: '引用' },
          { cmd: () => editor!.chain().focus().toggleCodeBlock().run(), active: editor.isActive('codeBlock'), label: '<>', title: '代码块' },
        ]"
        :key="btn.title"
        @click="btn.cmd"
        class="w-7 h-7 flex items-center justify-center rounded-md text-xs font-bold transition-colors"
        :class="btn.active
          ? 'bg-[var(--color-craft-accent)] text-white'
          : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
        :title="btn.title"
      >
        {{ btn.label }}
      </button>

      <div class="w-px h-5 bg-[var(--color-craft-border)] mx-1" />

      <button
        @click="showToc = !showToc"
        class="w-7 h-7 flex items-center justify-center rounded-md text-xs font-bold transition-colors"
        :class="showToc
          ? 'bg-[var(--color-craft-accent)] text-white'
          : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
        title="目录"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h10M4 18h14" />
        </svg>
      </button>
    </div>

    <!-- TOC toggle for read-only mode -->
    <div v-if="editor && !editable && tocItems.length > 0" class="flex items-center px-2 py-1 border-b border-[var(--color-craft-border)]">
      <button
        @click="showToc = !showToc"
        class="flex items-center gap-1.5 px-2 py-1 rounded-md text-xs transition-colors"
        :class="showToc
          ? 'bg-[var(--color-craft-accent)] text-white'
          : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
        title="目录"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h10M4 18h14" />
        </svg>
        <span>目录</span>
      </button>
    </div>

    <!-- Editor + TOC -->
    <div class="flex flex-1 overflow-hidden">
      <!-- TOC panel -->
      <Transition name="toc-slide">
        <div
          v-if="showToc"
          class="toc-panel w-52 shrink-0 border-r border-[var(--color-craft-border)] bg-[var(--color-craft-surface)] overflow-y-auto"
        >
          <div class="px-3 py-2 text-xs font-semibold text-[var(--color-craft-text-secondary)] uppercase tracking-wider">目录</div>
          <div v-if="tocItems.length === 0" class="px-3 py-2 text-xs text-[var(--color-craft-text-secondary)]">暂无标题</div>
          <nav v-else class="pb-3">
            <button
              v-for="(item, idx) in tocItems"
              :key="idx"
              @click="scrollToHeading(item.pos)"
              class="block w-full text-left px-3 py-1 text-sm truncate hover:bg-[var(--color-craft-hover)] transition-colors rounded-md mx-0"
              :style="{ paddingLeft: (8 + (item.level - 1) * 14) + 'px' }"
              :class="[
                item.level === 1 ? 'font-semibold text-[var(--color-craft-text)]' : '',
                item.level === 2 ? 'font-medium text-[var(--color-craft-text-secondary)]' : '',
                item.level === 3 ? 'text-[var(--color-craft-text-secondary)] text-xs' : '',
              ]"
              :title="item.text"
            >{{ item.text || '(空标题)' }}</button>
          </nav>
        </div>
      </Transition>

      <!-- Editor -->
      <div class="flex-1 overflow-y-auto px-6 py-4">
        <EditorContent :editor="editor" />
      </div>
    </div>

    <!-- Slash menu -->
    <SlashCommandMenu
      ref="slashMenuRef"
      :visible="slashMenu.visible"
      :x="slashMenu.x"
      :y="slashMenu.y"
      @select="handleSlashCommand"
      @close="slashMenu.visible = false"
    />

    <!-- Note link suggestion -->
    <NoteLinkSuggestion
      ref="linkMenuRef"
      :visible="linkMenu.visible"
      :x="linkMenu.x"
      :y="linkMenu.y"
      :query="linkMenu.query"
      :notes="notes"
      @select="handleNoteLinkSelect"
      @close="linkMenu.visible = false"
    />
  </div>
</template>

<style scoped>
.toc-slide-enter-active, .toc-slide-leave-active {
  transition: width 0.2s ease, opacity 0.2s ease;
  overflow: hidden;
}
.toc-slide-enter-from, .toc-slide-leave-to {
  width: 0 !important;
  opacity: 0;
}
</style>
