/**
 * NoteLink — TipTap 自定义扩展，实现双链笔记功能。
 *
 * 数据格式：Markdown 中以 [[noteId|noteTitle]] 存储，
 * 编辑器中渲染为可点击的行内芯片（inline atom node）。
 *
 * 核心机制：
 * - parseHTML/renderHTML：HTML span[data-note-id] 与 DOM 互转
 * - addNodeView：创建可点击的 DOM 节点，点击触发 onNavigate 回调
 * - addProseMirrorPlugins：appendTransaction 自动将纯文本 [[id|title]] 转换为 NoteLink 节点
 * - addStorage.markdown：定义 Markdown 序列化规则（tiptap-markdown 兼容）
 */
import { Node } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'

export interface NoteLinkOptions {
  onNavigate?: (noteId: string) => void
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    noteLink: {
      insertNoteLink: (attrs: { noteId: string; noteTitle: string }) => ReturnType
    }
  }
}

const NOTELINK_RE = /\[\[(\d+)\|([^\]]+)\]\]/g

export const NoteLink = Node.create<NoteLinkOptions>({
  name: 'noteLink',
  group: 'inline',
  inline: true,
  atom: true,
  selectable: true,

  addOptions() {
    return { onNavigate: undefined }
  },

  addAttributes() {
    return {
      noteId: { default: null },
      noteTitle: { default: '' },
    }
  },

  parseHTML() {
    return [{
      tag: 'span[data-note-id]',
      getAttrs: (el) => ({
        noteId: (el as HTMLElement).getAttribute('data-note-id'),
        noteTitle: (el as HTMLElement).getAttribute('data-note-title') || (el as HTMLElement).textContent,
      }),
    }]
  },

  renderHTML({ node }) {
    return ['span', {
      'data-note-id': node.attrs.noteId,
      'data-note-title': node.attrs.noteTitle,
      class: 'note-link-chip',
    }, node.attrs.noteTitle]
  },

  addNodeView() {
    return ({ node }) => {
      const dom = document.createElement('span')
      dom.className = 'note-link-chip'
      dom.dataset.noteId = node.attrs.noteId
      dom.dataset.noteTitle = node.attrs.noteTitle
      dom.textContent = node.attrs.noteTitle
      dom.title = `跳转到: ${node.attrs.noteTitle}`
      dom.addEventListener('click', (e) => {
        e.preventDefault()
        e.stopPropagation()
        this.options.onNavigate?.(node.attrs.noteId)
      })
      return { dom }
    }
  },

  addCommands() {
    return {
      insertNoteLink: (attrs) => ({ commands }) => {
        return commands.insertContent({
          type: this.name,
          attrs,
        })
      },
    }
  },

  addProseMirrorPlugins() {
    const extensionThis = this
    return [
      new Plugin({
        key: new PluginKey('noteLinkConverter'),
        appendTransaction(transactions, _oldState, newState) {
          if (!transactions.some(tr => tr.docChanged)) return null
          // Skip if the triggering transaction was itself a conversion
          if (transactions.some(tr => tr.getMeta('noteLinkConversion'))) return null

          const replacements: { from: number; to: number; noteId: string; noteTitle: string }[] = []

          newState.doc.descendants((node, pos) => {
            if (!node.isText || !node.text) return
            NOTELINK_RE.lastIndex = 0
            let m
            while ((m = NOTELINK_RE.exec(node.text)) !== null) {
              replacements.push({
                from: pos + m.index,
                to: pos + m.index + m[0].length,
                noteId: m[1],
                noteTitle: m[2],
              })
            }
          })

          if (replacements.length === 0) return null

          const tr = newState.tr
          replacements.sort((a, b) => b.from - a.from)
          for (const r of replacements) {
            const noteLinkNode = newState.schema.nodes.noteLink.create({
              noteId: r.noteId,
              noteTitle: r.noteTitle,
            })
            tr.replaceWith(r.from, r.to, noteLinkNode)
          }
          tr.setMeta('noteLinkConversion', true)
          return tr
        },
      }),
    ]
  },

  addStorage() {
    return {
      markdown: {
        serialize(state: any, node: any) {
          state.write(`[[${node.attrs.noteId}|${node.attrs.noteTitle}]]`)
        },
        parse: {},
      },
    }
  },
})
