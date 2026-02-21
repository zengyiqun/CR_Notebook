<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useWhiteboardStore } from '@/stores/whiteboardStore'
import ModalDialog from '@/components/ui/ModalDialog.vue'

const wbStore = useWhiteboardStore()

onMounted(() => {
  wbStore.fetchWhiteboards()
})
const isDrawing = ref(false)
const showNewBoardModal = ref(false)
const showClearModal = ref(false)
const drawColor = ref('#6366f1')
const drawWidth = ref(3)
const isEraser = ref(false)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const paths = ref<{ color: string; width: number; points: { x: number; y: number }[] }[]>([])
const currentPath = ref<{ color: string; width: number; points: { x: number; y: number }[] } | null>(null)

const activeBoard = computed(() => wbStore.activeWhiteboard)

function createBoard() {
  showNewBoardModal.value = true
}

async function onCreateBoardConfirm(title?: string) {
  showNewBoardModal.value = false
  const name = title?.trim() || '新白板'
  await wbStore.addWhiteboard(name)
  paths.value = []
  nextTick(redraw)
}

function selectBoard(id: string) {
  wbStore.setActive(id)
  const board = wbStore.whiteboards.find((w) => w.id === id)
  if (board && board.data !== '{}') {
    try { paths.value = JSON.parse(board.data) } catch { paths.value = [] }
  } else {
    paths.value = []
  }
  nextTick(redraw)
}

function getCanvasPos(e: MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return { x: 0, y: 0 }
  const rect = canvas.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

function startDraw(e: MouseEvent) {
  isDrawing.value = true
  const pos = getCanvasPos(e)
  const color = isEraser.value ? 'eraser' : drawColor.value
  currentPath.value = { color, width: isEraser.value ? 20 : drawWidth.value, points: [pos] }
}

function draw(e: MouseEvent) {
  if (!isDrawing.value || !currentPath.value) return
  const pos = getCanvasPos(e)
  currentPath.value.points.push(pos)
  redraw()
}

function endDraw() {
  if (currentPath.value && currentPath.value.points.length > 1) {
    paths.value.push(currentPath.value)
    if (activeBoard.value) {
      wbStore.updateWhiteboard(activeBoard.value.id, { data: JSON.stringify(paths.value) })
    }
  }
  currentPath.value = null
  isDrawing.value = false
}

function redraw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = canvas.offsetWidth * window.devicePixelRatio
  canvas.height = canvas.offsetHeight * window.devicePixelRatio
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio)
  ctx.clearRect(0, 0, canvas.offsetWidth, canvas.offsetHeight)

  const allPaths = currentPath.value ? [...paths.value, currentPath.value] : paths.value

  for (const path of allPaths) {
    if (path.points.length < 2) continue
    ctx.beginPath()

    if (path.color === 'eraser') {
      ctx.globalCompositeOperation = 'destination-out'
      ctx.strokeStyle = 'rgba(0,0,0,1)'
    } else {
      ctx.globalCompositeOperation = 'source-over'
      ctx.strokeStyle = path.color
    }

    ctx.lineWidth = path.width
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.moveTo(path.points[0].x, path.points[0].y)
    for (let i = 1; i < path.points.length; i++) {
      ctx.lineTo(path.points[i].x, path.points[i].y)
    }
    ctx.stroke()
  }

  ctx.globalCompositeOperation = 'source-over'
}

function clearCanvas() {
  showClearModal.value = true
}

function confirmClearCanvas() {
  showClearModal.value = false
  paths.value = []
  if (activeBoard.value) {
    wbStore.updateWhiteboard(activeBoard.value.id, { data: '{}' })
  }
  redraw()
}

function undoLast() {
  if (paths.value.length === 0) return
  paths.value.pop()
  if (activeBoard.value) {
    wbStore.updateWhiteboard(activeBoard.value.id, { data: JSON.stringify(paths.value) })
  }
  redraw()
}

const colors = ['#6366f1', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#1d1d1f', '#ffffff']
</script>

<template>
  <div class="h-full flex bg-[var(--color-craft-surface)]">
    <!-- Whiteboard list -->
    <div class="w-56 h-full border-r border-[var(--color-craft-border)] flex flex-col shrink-0 bg-[var(--color-craft-surface)]">
      <div class="px-4 pt-6 pb-3 flex items-center justify-between">
        <h2 class="text-base font-semibold text-[var(--color-craft-text)]">白板</h2>
        <button
          @click="createBoard"
          class="w-7 h-7 flex items-center justify-center rounded-xl bg-[var(--color-craft-accent)] text-white text-sm hover:opacity-90 shadow-sm active:scale-95 transition-all"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg>
        </button>
      </div>
      <div class="flex-1 overflow-y-auto px-2">
        <button
          v-for="wb in wbStore.whiteboards"
          :key="wb.id"
          @click="selectBoard(wb.id)"
          class="w-full text-left px-3 py-2.5 rounded-xl text-sm mb-0.5 transition-all duration-150 group"
          :class="wbStore.activeWhiteboardId === wb.id
            ? 'bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-medium'
            : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
        >
          <div class="flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" class="shrink-0"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
            <span class="truncate flex-1">{{ wb.title }}</span>
            <button
              @click.stop="wbStore.deleteWhiteboard(wb.id)"
              class="w-5 h-5 flex items-center justify-center rounded opacity-0 group-hover:opacity-100 hover:text-[var(--color-craft-danger)] text-[var(--color-craft-text-secondary)] transition-all"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18"/><path d="M6 6l12 12"/></svg>
            </button>
          </div>
        </button>
      </div>
    </div>

    <!-- Canvas area -->
    <div class="flex-1 flex flex-col min-w-0">
      <template v-if="activeBoard">
        <!-- Toolbar -->
        <div class="flex items-center gap-3 px-4 py-2.5 border-b border-[var(--color-craft-border)] bg-[var(--color-craft-surface)]">
          <!-- Colors -->
          <div class="flex gap-1">
            <button
              v-for="c in colors"
              :key="c"
              @click="drawColor = c; isEraser = false"
              class="w-6 h-6 rounded-full transition-all border"
              :class="[
                !isEraser && drawColor === c ? 'ring-2 ring-offset-1 ring-[var(--color-craft-accent)] scale-110' : 'hover:scale-105',
                c === '#ffffff' ? 'border-[var(--color-craft-border)]' : 'border-transparent'
              ]"
              :style="{ backgroundColor: c }"
            />
          </div>
          <div class="w-px h-5 bg-[var(--color-craft-border)]" />

          <!-- Eraser -->
          <button
            @click="isEraser = !isEraser"
            class="px-2.5 py-1 text-xs font-medium rounded-lg transition-all"
            :class="isEraser
              ? 'bg-[var(--color-craft-accent)] text-white'
              : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)]'"
          >橡皮</button>
          <div class="w-px h-5 bg-[var(--color-craft-border)]" />

          <!-- Width -->
          <div class="flex items-center gap-2">
            <span class="text-[11px] text-[var(--color-craft-text-secondary)]">粗细</span>
            <input v-model.number="drawWidth" type="range" min="1" max="20" class="w-20 accent-[var(--color-craft-accent)]" />
            <span class="text-[11px] text-[var(--color-craft-text-secondary)] w-4 text-center">{{ drawWidth }}</span>
          </div>
          <div class="w-px h-5 bg-[var(--color-craft-border)]" />

          <!-- Actions -->
          <button @click="undoLast" class="px-2.5 py-1 text-xs font-medium rounded-lg border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
            撤销
          </button>
          <button @click="clearCanvas" class="px-2.5 py-1 text-xs font-medium rounded-lg border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
            清空
          </button>
        </div>

        <!-- Canvas -->
        <div class="flex-1 relative overflow-hidden" :class="isEraser ? 'cursor-cell' : 'cursor-crosshair'">
          <div class="absolute inset-0 bg-white dark:bg-[#2a2a2e]"
            style="background-image: radial-gradient(circle, #e5e5ea 1px, transparent 1px); background-size: 20px 20px;"
          >
            <canvas
              ref="canvasRef"
              class="absolute inset-0 w-full h-full"
              @mousedown="startDraw"
              @mousemove="draw"
              @mouseup="endDraw"
              @mouseleave="endDraw"
            />
          </div>
        </div>
      </template>

      <!-- Empty state -->
      <div v-else class="flex-1 flex items-center justify-center">
        <div class="text-center px-8">
          <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-pink-100 to-purple-100 dark:from-pink-900/30 dark:to-purple-900/30 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          </div>
          <h2 class="text-lg font-semibold text-[var(--color-craft-text)] mb-2">白板</h2>
          <p class="text-[var(--color-craft-text-secondary)] text-sm">
            选择或创建一个白板，自由绘制你的想法
          </p>
        </div>
      </div>
    </div>

    <ModalDialog
      :visible="showNewBoardModal"
      type="prompt"
      title="新建白板"
      message="请输入白板名称"
      input-placeholder="白板名称..."
      input-value="新白板"
      confirm-text="创建"
      @confirm="onCreateBoardConfirm"
      @cancel="showNewBoardModal = false"
    />

    <ModalDialog
      :visible="showClearModal"
      type="confirm"
      title="清空画布"
      message="确定清空画布？此操作不可恢复。"
      confirm-text="清空"
      :danger="true"
      @confirm="confirmClearCanvas"
      @cancel="showClearModal = false"
    />
  </div>
</template>
