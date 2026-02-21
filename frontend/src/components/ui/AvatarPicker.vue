<script setup lang="ts">
import { ref } from 'vue'
import { presetAvatars } from '@/assets/avatars'

const props = defineProps<{
  visible: boolean
  currentAvatar?: string | null
  name: string
}>()

const emit = defineEmits<{
  (e: 'select', url: string | null): void
  (e: 'close'): void
}>()

const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function selectPreset(url: string) {
  emit('select', url)
}

function clearAvatar() {
  emit('select', null)
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    return
  }
  uploading.value = true
  try {
    const reader = new FileReader()
    reader.onload = () => {
      emit('select', reader.result as string)
      uploading.value = false
    }
    reader.readAsDataURL(file)
  } catch {
    uploading.value = false
  }
}

function getInitial(name: string) {
  return (name || '?').charAt(0).toUpperCase()
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="emit('close')" />
        <div class="relative bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
          <div class="px-6 pt-6 pb-3">
            <h3 class="text-base font-semibold text-[var(--color-craft-text)] mb-1">选择头像</h3>
            <p class="text-xs text-[var(--color-craft-text-secondary)]">选择预设动物头像或上传自定义图片</p>
          </div>

          <!-- Current avatar preview -->
          <div class="flex justify-center py-3">
            <div class="w-20 h-20 rounded-full border-2 border-[var(--color-craft-accent)] flex items-center justify-center overflow-hidden bg-gradient-to-br from-indigo-400 to-purple-500">
              <img v-if="currentAvatar" :src="currentAvatar" class="w-full h-full object-cover" />
              <span v-else class="text-white text-2xl font-bold">{{ getInitial(name) }}</span>
            </div>
          </div>

          <!-- Preset grid -->
          <div class="px-6 py-3">
            <div class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-2">预设头像</div>
            <div class="grid grid-cols-6 gap-2">
              <button
                v-for="avatar in presetAvatars"
                :key="avatar.id"
                @click="selectPreset(avatar.url)"
                class="w-12 h-12 rounded-full overflow-hidden border-2 transition-all duration-150 hover:scale-110"
                :class="currentAvatar === avatar.url ? 'border-[var(--color-craft-accent)] scale-110 ring-2 ring-[var(--color-craft-accent)]/30' : 'border-transparent'"
                :title="avatar.name"
              >
                <img :src="avatar.url" class="w-full h-full" />
              </button>
            </div>
          </div>

          <!-- Upload / Actions -->
          <div class="px-6 pb-5 pt-2 flex items-center gap-2">
            <input
              ref="fileInput"
              type="file"
              accept="image/png,image/jpeg,image/gif,image/webp"
              class="hidden"
              @change="handleFileUpload"
            />
            <button
              @click="triggerUpload"
              :disabled="uploading"
              class="flex-1 px-4 py-2 text-sm font-medium rounded-xl border border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-all"
            >
              {{ uploading ? '处理中...' : '上传图片' }}
            </button>
            <button
              @click="clearAvatar"
              class="px-4 py-2 text-sm font-medium rounded-xl border border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-all"
            >
              使用默认
            </button>
            <button
              @click="emit('close')"
              class="px-4 py-2 text-sm font-medium rounded-xl bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all"
            >
              确定
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
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
