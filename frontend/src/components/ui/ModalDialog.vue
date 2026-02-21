<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  title?: string
  message?: string
  type?: 'confirm' | 'prompt' | 'alert'
  inputPlaceholder?: string
  inputValue?: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}>(), {
  type: 'confirm',
  confirmText: '确定',
  cancelText: '取消',
  danger: false,
  inputPlaceholder: '',
  inputValue: '',
})

const emit = defineEmits<{
  (e: 'confirm', value?: string): void
  (e: 'cancel'): void
}>()

const inputVal = ref(props.inputValue)
const inputRef = ref<HTMLInputElement | null>(null)

watch(() => props.visible, (v) => {
  if (v) {
    inputVal.value = props.inputValue
    if (props.type === 'prompt') {
      nextTick(() => inputRef.value?.focus())
    }
  }
})

function handleConfirm() {
  if (props.type === 'prompt') {
    emit('confirm', inputVal.value)
  } else {
    emit('confirm')
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') handleConfirm()
  if (e.key === 'Escape') emit('cancel')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center p-4" @keydown="handleKeydown">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="emit('cancel')" />
        <div class="relative bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden animate-in zoom-in-95">
          <div class="px-6 pt-6 pb-4">
            <h3 v-if="title" class="text-base font-semibold text-[var(--color-craft-text)] mb-2">{{ title }}</h3>
            <p v-if="message" class="text-sm text-[var(--color-craft-text-secondary)] leading-relaxed">{{ message }}</p>
            <input
              v-if="type === 'prompt'"
              ref="inputRef"
              v-model="inputVal"
              :placeholder="inputPlaceholder"
              @keydown.enter.stop="handleConfirm"
              class="mt-3 w-full px-4 py-2.5 rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] text-sm outline-none focus:border-[var(--color-craft-accent)] focus:ring-1 focus:ring-[var(--color-craft-accent)]/20 transition-all"
            />
          </div>
          <div class="px-6 pb-5 flex justify-end gap-2">
            <button
              v-if="type !== 'alert'"
              @click="emit('cancel')"
              class="px-4 py-2 text-sm font-medium rounded-xl border border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-all"
            >
              {{ cancelText }}
            </button>
            <button
              @click="handleConfirm"
              class="px-4 py-2 text-sm font-medium rounded-xl text-white transition-all"
              :class="danger ? 'bg-[var(--color-craft-danger)] hover:opacity-90' : 'bg-[var(--color-craft-accent)] hover:opacity-90'"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}
.modal-fade-enter-active .animate-in {
  animation: zoom-in 0.15s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
@keyframes zoom-in {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
