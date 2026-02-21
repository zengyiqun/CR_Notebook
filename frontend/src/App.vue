<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { useFolderStore } from '@/stores/folderStore'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const folderStore = useFolderStore()
const authStore = useAuthStore()

const sidebarCollapsed = ref(false)
const isMobile = ref(false)
const showFolderModal = ref(false)
const pendingParentFolderId = ref<string | null>(null)

const isLoginPage = computed(() => route.name === 'login')
const showSidebar = computed(() => authStore.isAuthenticated && !isLoginPage.value)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    sidebarCollapsed.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

function handleAddFolder(parentId: string | null = null) {
  pendingParentFolderId.value = parentId
  showFolderModal.value = true
}

function onFolderConfirm(name?: string) {
  showFolderModal.value = false
  if (name?.trim()) {
    folderStore.addFolder(name.trim(), pendingParentFolderId.value)
  }
  pendingParentFolderId.value = null
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function onOverlayClick() {
  if (isMobile.value) {
    sidebarCollapsed.value = true
  }
}
</script>

<template>
  <div class="h-screen w-screen flex overflow-hidden bg-[var(--color-craft-bg)]">
    <template v-if="showSidebar">
      <!-- Mobile overlay -->
      <Transition name="fade">
        <div
          v-if="isMobile && !sidebarCollapsed"
          class="fixed inset-0 z-20 bg-black/30 backdrop-blur-sm"
          @click="onOverlayClick"
        />
      </Transition>

      <!-- Sidebar -->
      <div
        class="h-full shrink-0 transition-all duration-300 ease-in-out z-30"
        :class="[
          sidebarCollapsed ? 'w-0 overflow-hidden' : 'w-60',
          isMobile && !sidebarCollapsed ? 'fixed left-0 top-0 shadow-2xl' : ''
        ]"
      >
        <AppSidebar @add-folder="handleAddFolder" />
      </div>

      <!-- Toggle sidebar button - bottom position -->
      <button
        @click="toggleSidebar"
        class="fixed bottom-4 z-40 w-8 h-8 flex items-center justify-center rounded-full bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] text-xs hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all duration-300 shadow-md"
        :class="sidebarCollapsed ? 'left-3' : (isMobile ? 'left-[248px]' : 'left-[232px]')"
      >
        <svg v-if="sidebarCollapsed" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
      </button>
    </template>

    <!-- Main content -->
    <div class="flex-1 h-full overflow-hidden">
      <router-view v-slot="{ Component }">
        <Transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </Transition>
      </router-view>
    </div>

    <ModalDialog
      :visible="showFolderModal"
      type="prompt"
      :title="pendingParentFolderId ? '新建子文件夹' : '新建文件夹'"
      :message="pendingParentFolderId ? '请输入子文件夹名称' : '请输入文件夹名称'"
      input-placeholder="文件夹名称..."
      confirm-text="创建"
      @confirm="onFolderConfirm"
      @cancel="showFolderModal = false"
    />
  </div>
</template>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
