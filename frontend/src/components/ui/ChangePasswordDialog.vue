<script setup lang="ts">
import { ref, watch } from 'vue'
import { usersApi } from '@/api/organizations'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success'): void
}>()

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')
const loading = ref(false)

watch(() => props.visible, (v) => {
  if (v) {
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    errorMsg.value = ''
    loading.value = false
  }
})

async function submit() {
  errorMsg.value = ''

  if (!oldPassword.value) {
    errorMsg.value = '请输入原密码'
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    errorMsg.value = '新密码至少 6 位'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的新密码不一致'
    return
  }
  if (oldPassword.value === newPassword.value) {
    errorMsg.value = '新密码不能与原密码相同'
    return
  }

  loading.value = true
  try {
    await usersApi.changePassword(oldPassword.value, newPassword.value)
    emit('success')
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '修改失败'
    errorMsg.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="emit('close')" />
        <div class="relative bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
          <div class="px-6 pt-6 pb-2">
            <h3 class="text-base font-semibold text-[var(--color-craft-text)] mb-1">修改密码</h3>
            <p class="text-xs text-[var(--color-craft-text-secondary)]">请输入原密码和新密码</p>
          </div>

          <form @submit.prevent="submit" class="px-6 pb-6 pt-3 space-y-3">
            <div>
              <label class="block text-xs font-medium text-[var(--color-craft-text-secondary)] mb-1">原密码</label>
              <input
                v-model="oldPassword"
                type="password"
                autocomplete="current-password"
                class="w-full px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] outline-none focus:border-[var(--color-craft-accent)] transition-colors"
                placeholder="请输入原密码"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-[var(--color-craft-text-secondary)] mb-1">新密码</label>
              <input
                v-model="newPassword"
                type="password"
                autocomplete="new-password"
                class="w-full px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] outline-none focus:border-[var(--color-craft-accent)] transition-colors"
                placeholder="至少 6 位"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-[var(--color-craft-text-secondary)] mb-1">确认新密码</label>
              <input
                v-model="confirmPassword"
                type="password"
                autocomplete="new-password"
                class="w-full px-3 py-2 text-sm rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] outline-none focus:border-[var(--color-craft-accent)] transition-colors"
                placeholder="再次输入新密码"
              />
            </div>

            <Transition name="fade">
              <p v-if="errorMsg" class="text-xs text-red-500 px-1">{{ errorMsg }}</p>
            </Transition>

            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                @click="emit('close')"
                class="px-4 py-2 text-sm font-medium rounded-xl border border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] transition-all"
              >取消</button>
              <button
                type="submit"
                :disabled="loading"
                class="px-4 py-2 text-sm font-medium rounded-xl bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all disabled:opacity-50"
              >{{ loading ? '提交中...' : '确认修改' }}</button>
            </div>
          </form>
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
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
