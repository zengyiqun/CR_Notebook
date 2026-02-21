<script setup lang="ts">
import { ref, watch, computed } from 'vue'

export interface NoteSuggestion {
  id: string
  title: string
}

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  query: string
  notes: NoteSuggestion[]
}>()

const emit = defineEmits<{
  (e: 'select', note: NoteSuggestion): void
  (e: 'close'): void
}>()

const selectedIndex = ref(0)

const filtered = computed(() => {
  const q = props.query.toLowerCase()
  if (!q) return props.notes.slice(0, 8)
  return props.notes.filter(n => n.title.toLowerCase().includes(q)).slice(0, 8)
})

watch(() => props.visible, (v) => {
  if (v) selectedIndex.value = 0
})

watch(() => props.query, () => {
  selectedIndex.value = 0
})

function onKeydown(e: KeyboardEvent) {
  if (!props.visible) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value + 1) % Math.max(filtered.value.length, 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value - 1 + filtered.value.length) % Math.max(filtered.value.length, 1)
  } else if (e.key === 'Enter' && filtered.value.length > 0) {
    e.preventDefault()
    emit('select', filtered.value[selectedIndex.value])
  } else if (e.key === 'Escape') {
    emit('close')
  }
}

defineExpose({ onKeydown })
</script>

<template>
  <Teleport to="body">
    <Transition name="link-menu">
      <div
        v-if="visible"
        class="fixed z-50 w-72 max-h-72 overflow-y-auto rounded-2xl bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] shadow-2xl"
        :style="{ left: x + 'px', top: y + 'px' }"
      >
        <div class="px-3 py-2 border-b border-[var(--color-craft-border)] flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 text-[var(--color-craft-accent)]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m9.915-3.173a4.5 4.5 0 0 0-1.242-7.244l-4.5-4.5a4.5 4.5 0 0 0-6.364 6.364L5.25 9.94" />
          </svg>
          <span class="text-[11px] font-medium text-[var(--color-craft-text-secondary)]">链接到笔记</span>
          <span v-if="query" class="ml-auto text-[11px] text-[var(--color-craft-accent)] font-mono truncate max-w-[120px]">{{ query }}</span>
        </div>
        <div v-if="filtered.length === 0" class="px-4 py-6 text-center text-xs text-[var(--color-craft-text-secondary)]">
          没有找到匹配的笔记
        </div>
        <div v-else class="p-1.5">
          <button
            v-for="(note, i) in filtered"
            :key="note.id"
            @click="emit('select', note)"
            @mouseenter="selectedIndex = i"
            class="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm transition-all duration-100"
            :class="i === selectedIndex ? 'bg-[var(--color-craft-accent-light)]' : ''"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 shrink-0" :class="i === selectedIndex ? 'text-[var(--color-craft-accent)]' : 'text-[var(--color-craft-text-secondary)]'" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
            </svg>
            <span class="truncate text-left text-[var(--color-craft-text)]">{{ note.title || '无标题' }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style>
.link-menu-enter-active,
.link-menu-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.link-menu-enter-from,
.link-menu-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>
