<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
}>()

const emit = defineEmits<{
  (e: 'select', command: string): void
  (e: 'close'): void
}>()

const commands = [
  { key: 'heading1', icon: 'H1', label: '标题 1', desc: '大标题' },
  { key: 'heading2', icon: 'H2', label: '标题 2', desc: '中标题' },
  { key: 'heading3', icon: 'H3', label: '标题 3', desc: '小标题' },
  { key: 'bulletList', icon: '•', label: '无序列表', desc: '项目符号列表' },
  { key: 'orderedList', icon: '1.', label: '有序列表', desc: '编号列表' },
  { key: 'taskList', icon: '☑', label: '任务列表', desc: '待办清单' },
  { key: 'blockquote', icon: '❝', label: '引用', desc: '引用文本块' },
  { key: 'codeBlock', icon: '</>', label: '代码块', desc: '代码片段' },
  { key: 'horizontalRule', icon: '─', label: '分割线', desc: '水平分割线' },
]

const selectedIndex = ref(0)

watch(() => props.visible, (v) => {
  if (v) {
    selectedIndex.value = 0
  }
})

function onKeydown(e: KeyboardEvent) {
  if (!props.visible) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value + 1) % commands.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value - 1 + commands.length) % commands.length
  } else if (e.key === 'Enter') {
    e.preventDefault()
    emit('select', commands[selectedIndex.value].key)
  } else if (e.key === 'Escape') {
    emit('close')
  }
}

defineExpose({ onKeydown })
</script>

<template>
  <Teleport to="body">
    <Transition name="slash-menu">
      <div
        v-if="visible"
        class="fixed z-50 w-64 max-h-80 overflow-y-auto rounded-2xl bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] shadow-2xl"
        :style="{ left: x + 'px', top: y + 'px' }"
      >
        <div class="px-3 py-2 border-b border-[var(--color-craft-border)]">
          <p class="text-[11px] font-medium text-[var(--color-craft-text-secondary)]">插入块类型</p>
        </div>
        <div class="p-1.5">
          <button
            v-for="(cmd, i) in commands"
            :key="cmd.key"
            @click="emit('select', cmd.key)"
            @mouseenter="selectedIndex = i"
            class="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm transition-all duration-100"
            :class="i === selectedIndex ? 'bg-[var(--color-craft-accent-light)]' : ''"
          >
            <span
              class="w-8 h-8 flex items-center justify-center rounded-lg bg-[var(--color-craft-bg)] border border-[var(--color-craft-border)] text-xs font-bold"
              :class="i === selectedIndex ? 'text-[var(--color-craft-accent)] border-[var(--color-craft-accent)]/30' : 'text-[var(--color-craft-text)]'"
            >{{ cmd.icon }}</span>
            <div class="text-left">
              <div class="font-medium text-[var(--color-craft-text)]">{{ cmd.label }}</div>
              <div class="text-[11px] text-[var(--color-craft-text-secondary)]">{{ cmd.desc }}</div>
            </div>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style>
.slash-menu-enter-active,
.slash-menu-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.slash-menu-enter-from,
.slash-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>
