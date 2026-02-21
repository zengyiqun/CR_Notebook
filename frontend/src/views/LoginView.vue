<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const isRegister = ref(false)
const form = ref({
  username: '',
  email: '',
  password: '',
  displayName: '',
})
const localError = ref('')

async function handleSubmit() {
  localError.value = ''
  try {
    if (isRegister.value) {
      await authStore.register(form.value.username, form.value.email, form.value.password, form.value.displayName || undefined)
    } else {
      await authStore.login(form.value.username, form.value.password)
    }
    router.push('/notes')
  } catch (e: any) {
    localError.value = e.message || (isRegister.value ? '注册失败' : '登录失败')
  }
}

function toggleMode() {
  isRegister.value = !isRegister.value
  localError.value = ''
}
</script>

<template>
  <div class="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-indigo-950 p-4">
    <div class="w-full max-w-md">
      <!-- Logo -->
      <div class="text-center mb-8">
        <img src="/rabbit.svg" alt="Rabbit Notebook" class="w-16 h-16 rounded-2xl shadow-lg mx-auto mb-4" />
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Rabbit Notebook</h1>
        <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ isRegister ? '创建新账户' : '登录到你的笔记空间' }}</p>
      </div>

      <!-- Form Card -->
      <div class="bg-white/80 dark:bg-gray-800/80 backdrop-blur-xl rounded-2xl border border-gray-200/60 dark:border-gray-700/60 shadow-xl p-8">
        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div v-if="isRegister">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">用户名</label>
            <input
              v-model="form.username"
              type="text"
              required
              minlength="3"
              class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
              placeholder="至少 3 个字符"
            />
          </div>

          <div v-if="isRegister">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">邮箱</label>
            <input
              v-model="form.email"
              type="email"
              required
              class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
              placeholder="your@email.com"
            />
          </div>

          <div v-if="!isRegister">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">用户名 / 邮箱</label>
            <input
              v-model="form.username"
              type="text"
              required
              class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
              placeholder="输入用户名或邮箱"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              minlength="6"
              class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
              placeholder="至少 6 个字符"
            />
          </div>

          <div v-if="isRegister">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">显示名称 <span class="text-gray-400">(可选)</span></label>
            <input
              v-model="form.displayName"
              type="text"
              class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition"
              placeholder="你的昵称"
            />
          </div>

          <!-- Error message -->
          <div v-if="localError" class="p-3 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm">
            {{ localError }}
          </div>

          <button
            type="submit"
            :disabled="authStore.loading"
            class="w-full py-2.5 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 text-white font-medium hover:from-indigo-600 hover:to-purple-700 focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="authStore.loading" class="inline-flex items-center gap-2">
              <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
              处理中...
            </span>
            <span v-else>{{ isRegister ? '注册' : '登录' }}</span>
          </button>
        </form>

        <div class="mt-6 text-center">
          <button @click="toggleMode" class="text-sm text-indigo-600 dark:text-indigo-400 hover:underline">
            {{ isRegister ? '已有账户？立即登录' : '没有账户？立即注册' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
