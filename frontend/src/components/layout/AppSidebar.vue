<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import AvatarPicker from '@/components/ui/AvatarPicker.vue'
import ChangePasswordDialog from '@/components/ui/ChangePasswordDialog.vue'
import FolderTreeItem from '@/components/layout/FolderTreeItem.vue'
import { useRouter, useRoute } from 'vue-router'
import { useFolderStore } from '@/stores/folderStore'
import { useNoteStore } from '@/stores/noteStore'
import { useTaskStore } from '@/stores/taskStore'
import { useCalendarStore } from '@/stores/calendarStore'
import { useWhiteboardStore } from '@/stores/whiteboardStore'
import { useThemeStore } from '@/stores/themeStore'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const route = useRoute()
const folderStore = useFolderStore()
const noteStore = useNoteStore()
const taskStore = useTaskStore()
const calendarStore = useCalendarStore()
const whiteboardStore = useWhiteboardStore()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const emit = defineEmits<{
  (e: 'addFolder', parentId: string | null): void
}>()

const accentMap: Record<string, string> = {
  indigo: '#6366f1', violet: '#8b5cf6', rose: '#f43f5e',
  emerald: '#10b981', amber: '#f59e0b', sky: '#0ea5e9',
}

const showSpaceSwitcher = ref(false)
const newOrgName = ref('')
const showCreateOrg = ref(false)
const showDocCount = ref(localStorage.getItem('cr-notebook-show-doc-count') !== 'false')

function toggleDocCount() {
  showDocCount.value = !showDocCount.value
  localStorage.setItem('cr-notebook-show-doc-count', String(showDocCount.value))
}

function navItemCount(key: string): number | null {
  if (!showDocCount.value) return null
  switch (key) {
    case 'notes': return noteStore.notes.length
    case 'tasks': return taskStore.pendingTasks.length
    case 'calendar': return calendarStore.events.length
    case 'whiteboard': return whiteboardStore.whiteboards.length
    default: return null
  }
}

function folderNoteCount(folderId: string): number {
  return noteStore.notes.filter(n => n.folderId === folderId).length
}

const navDragOver = ref<string | null>(null)

function onNavDragOver(e: DragEvent, key: string) {
  if (key !== 'notes') return
  if (!e.dataTransfer?.types.includes('application/x-note-id')) return
  e.preventDefault()
  e.dataTransfer!.dropEffect = 'move'
  navDragOver.value = key
}

function onNavDragLeave(key: string) {
  if (navDragOver.value === key) navDragOver.value = null
}

function onNavDrop(e: DragEvent, key: string) {
  navDragOver.value = null
  if (key !== 'notes') return
  const noteId = e.dataTransfer?.getData('application/x-note-id')
  if (!noteId) return
  e.preventDefault()
  noteStore.updateNote(noteId, { folderId: null })
}

onMounted(() => {
  authStore.initTenant()
  authStore.fetchOrganizations()
  folderStore.fetchFolders()
  taskStore.fetchTasks()
  whiteboardStore.fetchWhiteboards()
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10)
  calendarStore.fetchEvents(start, end)
})

const navItems = [
  { label: '所有笔记', path: '/notes', key: 'notes', svg: 'M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8' },
  { label: '任务', path: '/tasks', key: 'tasks', svg: 'M9 11l3 3L22 4 M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11' },
  { label: '每日笔记', path: '/daily', key: 'daily', svg: 'M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M9 16h6' },
  { label: '日历', path: '/calendar', key: 'calendar', svg: 'M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M8 14h.01 M12 14h.01 M16 14h.01 M8 18h.01 M12 18h.01' },
  { label: '白板', path: '/whiteboard', key: 'whiteboard', svg: 'M2 3h20v14H2z M8 21h8 M12 17v4' },
  { label: '知识图谱', path: '/graph', key: 'graph', svg: 'M5.5 8.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M18.5 8.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M12 21.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M8.5 7l2.5 8.5 M15.5 7l-2.5 8.5 M8 5.5h7' },
  { label: '统计', path: '/stats', key: 'stats', svg: 'M18 20V10 M12 20V4 M6 20v-6' },
]

const allTags = computed(() => {
  const tagSet = new Set<string>()
  noteStore.notes.forEach(n => n.tags?.forEach(t => tagSet.add(t)))
  return [...tagSet].sort()
})

function filterByTag(tag: string | null) {
  noteStore.activeTagFilter = noteStore.activeTagFilter === tag ? null : tag
  if (tag) {
    router.push('/notes')
  }
}

const currentOrgAvatar = computed(() => {
  if (authStore.isPersonalSpace || !authStore.currentTenant) return null
  const org = authStore.organizations.find(o => o.id === authStore.currentTenant!.id)
  return org?.avatarUrl || null
})

const canManageOrg = computed(() => {
  if (authStore.isPersonalSpace || !authStore.currentTenant) return false
  const org = authStore.organizations.find(o => o.id === authStore.currentTenant!.id)
  return org?.role === 'OWNER' || org?.role === 'ADMIN'
})

const currentPath = computed(() => route.path)

function navigateTo(path: string) {
  router.push(path)
}

const deletingFolderId = ref<string | null>(null)
const showAlertModal = ref(false)
const alertMessage = ref('')
const showAvatarPicker = ref(false)
const showChangePassword = ref(false)

function onPasswordChanged() {
  showChangePassword.value = false
  alertMessage.value = '密码修改成功'
  showAlertModal.value = true
}

async function onAvatarSelect(url: string | null) {
  try {
    await authStore.updateUserAvatar(url)
  } catch (e: any) {
    alertMessage.value = e.message || '头像更新失败'
    showAlertModal.value = true
  }
}


function confirmDeleteFolder() {
  if (deletingFolderId.value) {
    folderStore.deleteFolder(deletingFolderId.value)
  }
  deletingFolderId.value = null
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function switchSpace(type: 'personal' | 'org', orgId?: number) {
  if (type === 'personal') {
    authStore.switchToPersonal()
  } else if (orgId) {
    const org = authStore.organizations.find((o) => o.id === orgId)
    if (org) authStore.switchToOrg(org)
  }
  showSpaceSwitcher.value = false
  folderStore.fetchFolders()
  router.push('/notes')
}

async function createOrg() {
  if (!newOrgName.value.trim()) return
  try {
    await authStore.createOrganization(newOrgName.value.trim())
    newOrgName.value = ''
    showCreateOrg.value = false
  } catch (e: any) {
    alertMessage.value = e.message || '创建失败'
    showAlertModal.value = true
  }
}
</script>

<template>
  <aside class="h-full flex flex-col bg-[var(--color-craft-sidebar)] glass border-r border-[var(--color-craft-border)] select-none">
    <!-- Logo -->
    <div class="px-5 pt-5 pb-3 flex items-center gap-2.5">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none" class="w-8 h-8 rounded-xl shadow-sm shrink-0">
        <rect width="64" height="64" rx="14" fill="var(--color-craft-accent)"/>
        <ellipse cx="24" cy="16" rx="5" ry="12" fill="white"/>
        <ellipse cx="40" cy="16" rx="5" ry="12" fill="white"/>
        <ellipse cx="24" cy="16" rx="3" ry="9" fill="var(--color-craft-accent-light)" opacity="0.6"/>
        <ellipse cx="40" cy="16" rx="3" ry="9" fill="var(--color-craft-accent-light)" opacity="0.6"/>
        <ellipse cx="32" cy="32" rx="14" ry="12" fill="white"/>
        <circle cx="27" cy="30" r="2.5" fill="var(--color-craft-accent)"/>
        <circle cx="37" cy="30" r="2.5" fill="var(--color-craft-accent)"/>
        <ellipse cx="32" cy="35" rx="2" ry="1.5" fill="var(--color-craft-accent-light)"/>
        <rect x="22" y="42" rx="2" width="20" height="14" fill="white"/>
        <line x1="26" y1="47" x2="38" y2="47" stroke="var(--color-craft-accent-light)" stroke-width="1.5" stroke-linecap="round"/>
        <line x1="26" y1="51" x2="34" y2="51" stroke="var(--color-craft-accent-light)" stroke-width="1.5" stroke-linecap="round"/>
      </svg>
      <h1 class="text-base font-semibold tracking-tight text-[var(--color-craft-text)]">
        Rabbit Notebook
      </h1>
    </div>

    <!-- Space switcher -->
    <div class="px-3 mb-2 relative">
      <button
        @click="showSpaceSwitcher = !showSpaceSwitcher"
        class="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-sm border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] transition-all"
      >
        <div class="w-6 h-6 rounded-lg flex items-center justify-center text-xs font-bold overflow-hidden text-white"
          :style="{ background: 'var(--color-craft-accent)', opacity: authStore.isPersonalSpace ? 1 : 0.85 }"
        >
          <img v-if="authStore.isPersonalSpace && authStore.userAvatar" :src="authStore.userAvatar" class="w-full h-full object-cover" />
          <template v-else-if="!authStore.isPersonalSpace">
            <img v-if="currentOrgAvatar" :src="currentOrgAvatar" class="w-full h-full object-cover" />
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </template>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <span class="flex-1 text-left truncate text-[var(--color-craft-text)] font-medium">{{ authStore.currentSpaceName }}</span>
        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class="text-[var(--color-craft-text-secondary)] transition-transform" :class="showSpaceSwitcher ? 'rotate-180' : ''"><path d="M6 9l6 6 6-6"/></svg>
      </button>

      <!-- Space dropdown -->
      <Transition name="fade-slide">
        <div v-if="showSpaceSwitcher" class="absolute left-3 right-3 top-full mt-1 z-50 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-xl overflow-hidden">
          <!-- Personal space -->
          <button
            @click="switchSpace('personal')"
            class="w-full flex items-center gap-2.5 px-3 py-2.5 text-sm hover:bg-[var(--color-craft-hover)] transition-colors"
            :class="authStore.isPersonalSpace ? 'bg-[var(--color-craft-accent-light)]' : ''"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-accent)" stroke-width="1.75" stroke-linecap="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span class="flex-1 text-left text-[var(--color-craft-text)]">个人空间</span>
            <svg v-if="authStore.isPersonalSpace" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="text-[var(--color-craft-accent)]"><path d="M20 6L9 17l-5-5"/></svg>
          </button>

          <div v-if="authStore.organizations.length" class="border-t border-[var(--color-craft-border)]">
            <div class="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)]">组织</div>
            <button
              v-for="org in authStore.organizations"
              :key="org.id"
              @click="switchSpace('org', org.id)"
              class="w-full flex items-center gap-2.5 px-3 py-2.5 text-sm hover:bg-[var(--color-craft-hover)] transition-colors"
              :class="!authStore.isPersonalSpace && authStore.currentTenant?.id === org.id ? 'bg-[var(--color-craft-accent-light)]' : ''"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-accent)" stroke-width="1.75" stroke-linecap="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              <span class="flex-1 text-left text-[var(--color-craft-text)]">{{ org.name }}</span>
              <span class="text-[10px] text-[var(--color-craft-text-secondary)]">{{ org.memberCount }} 人</span>
              <svg v-if="!authStore.isPersonalSpace && authStore.currentTenant?.id === org.id" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="text-[var(--color-craft-accent)]"><path d="M20 6L9 17l-5-5"/></svg>
            </button>
          </div>

          <div class="border-t border-[var(--color-craft-border)]">
            <div v-if="showCreateOrg" class="p-2">
              <input
                v-model="newOrgName"
                @keydown.enter="createOrg"
                @keydown.escape="showCreateOrg = false; newOrgName = ''"
                type="text"
                placeholder="组织名称..."
                class="w-full px-3 py-2 text-sm rounded-lg border border-[var(--color-craft-accent)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] outline-none"
                autofocus
              />
            </div>
            <button
              v-else
              @click="showCreateOrg = true"
              class="w-full flex items-center gap-2.5 px-3 py-2.5 text-sm text-[var(--color-craft-accent)] hover:bg-[var(--color-craft-hover)] transition-colors"
            >
              <span>+</span>
              <span>创建组织</span>
            </button>
          </div>
        </div>
      </Transition>
    </div>

    <!-- Org manage shortcut -->
    <div v-if="canManageOrg" class="px-3 mb-1">
      <button
        @click="router.push(`/org/${authStore.currentTenant!.id}/manage`)"
        class="w-full flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs font-medium border border-dashed border-[var(--color-craft-border)] text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] hover:border-[var(--color-craft-accent)] transition-all"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        <span>管理组织成员</span>
      </button>
    </div>

    <!-- Nav items -->
    <nav class="px-3 space-y-0.5 mt-1">
      <button
        v-for="item in navItems"
        :key="item.key"
        @click="navigateTo(item.path)"
        @dragover="onNavDragOver($event, item.key)"
        @dragleave="onNavDragLeave(item.key)"
        @drop="onNavDrop($event, item.key)"
        class="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm font-medium transition-all duration-150"
        :class="[
          currentPath.startsWith(item.path)
            ? 'bg-[var(--color-craft-accent)] text-white shadow-sm'
            : 'text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)]',
          navDragOver === item.key ? 'ring-2 ring-[var(--color-craft-accent)] bg-[var(--color-craft-accent-light)]' : ''
        ]"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" class="shrink-0"><path :d="item.svg" /></svg>
        <span class="flex-1 text-left">{{ item.label }}</span>
        <span
          v-if="navItemCount(item.key) !== null"
          class="text-[10px] px-1.5 py-0.5 rounded-full font-medium min-w-[18px] text-center shrink-0 ml-auto"
          :class="currentPath.startsWith(item.path)
            ? 'bg-white/20 text-white'
            : 'bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)]'"
        >{{ navItemCount(item.key) }}</span>
      </button>
    </nav>

    <!-- Divider -->
    <div class="mx-5 my-3 border-t border-[var(--color-craft-border)]" />

    <!-- Folders -->
    <div class="px-3 flex-1 overflow-y-auto">
      <div class="flex items-center justify-between px-3 mb-2">
        <span class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)]">
          文件夹
        </span>
        <button
          @click="emit('addFolder', null)"
          class="w-5 h-5 flex items-center justify-center rounded-md hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-text)] transition-colors"
          title="新建文件夹"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg>
        </button>
      </div>

      <template v-if="folderStore.rootFolders.length === 0">
        <p class="px-3 py-2 text-xs text-[var(--color-craft-text-secondary)]">暂无文件夹</p>
      </template>
      <FolderTreeItem
        v-for="folder in folderStore.rootFolders"
        :key="folder.id"
        :folder="folder"
        :depth="0"
        :show-count="showDocCount"
        @add-sub-folder="emit('addFolder', $event)"
        @delete-folder="(id: string) => deletingFolderId = id"
      />
    </div>

    <!-- Tags filter -->
    <div v-if="allTags.length > 0" class="px-3 pb-2">
      <div class="mx-3 mb-2 border-t border-[var(--color-craft-border)]"></div>
      <div class="flex items-center justify-between px-3 mb-2">
        <span class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)]">标签</span>
        <button v-if="noteStore.activeTagFilter" @click="filterByTag(null)" class="text-[10px] text-[var(--color-craft-accent)] hover:underline">清除</button>
      </div>
      <div class="flex flex-wrap gap-1.5 px-2">
        <button
          v-for="tag in allTags"
          :key="tag"
          @click="filterByTag(tag)"
          class="px-2 py-0.5 text-[11px] rounded-full font-medium transition-all duration-150"
          :class="noteStore.activeTagFilter === tag
            ? 'bg-[var(--color-craft-accent)] text-white'
            : 'bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] hover:opacity-80'"
        >
          #{{ tag }}
        </button>
      </div>
    </div>

    <!-- Bottom: User + Theme -->
    <div class="px-3 pb-4 pt-2 border-t border-[var(--color-craft-border)] space-y-0.5">
      <!-- User info -->
      <div v-if="authStore.user" class="flex items-center gap-3 px-3 py-2">
        <button
          @click="showAvatarPicker = true"
          class="w-7 h-7 rounded-full shrink-0 overflow-hidden flex items-center justify-center cursor-pointer hover:ring-2 hover:ring-[var(--color-craft-accent)]/40 transition-all"
          :style="{ background: 'var(--color-craft-accent)' }"
          title="点击更换头像"
        >
          <img v-if="authStore.userAvatar" :src="authStore.userAvatar" class="w-full h-full object-cover" />
          <span v-else class="text-white text-[11px] font-bold">{{ authStore.displayName.charAt(0).toUpperCase() }}</span>
        </button>
        <span class="flex-1 text-sm text-[var(--color-craft-text)] truncate">{{ authStore.displayName }}</span>
        <button
          @click="showChangePassword = true"
          class="w-6 h-6 flex items-center justify-center rounded-md text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all"
          title="修改密码"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        </button>
        <button
          @click="handleLogout"
          class="text-[11px] text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] transition-colors"
          title="退出登录"
        >退出</button>
      </div>

      <button
        @click="themeStore.toggle()"
        class="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all duration-150"
      >
        <svg v-if="themeStore.mode === 'light'" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
        <span>{{ themeStore.mode === 'light' ? '深色模式' : '浅色模式' }}</span>
      </button>

      <!-- Doc count toggle -->
      <button
        @click="toggleDocCount"
        class="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm text-[var(--color-craft-text-secondary)] hover:bg-[var(--color-craft-hover)] hover:text-[var(--color-craft-text)] transition-all duration-150"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><path d="M4 6h16M4 12h10M4 18h14"/></svg>
        <span class="flex-1 text-left">文档计数</span>
        <span
          class="w-7 h-4 rounded-full relative transition-colors duration-200"
          :class="showDocCount ? 'bg-[var(--color-craft-accent)]' : 'bg-[var(--color-craft-border)]'"
        >
          <span
            class="absolute top-0.5 w-3 h-3 rounded-full bg-white shadow-sm transition-all duration-200"
            :class="showDocCount ? 'left-3.5' : 'left-0.5'"
          />
        </span>
      </button>

      <!-- Accent color picker -->
      <div class="flex items-center gap-2 px-3 py-2">
        <span class="text-[11px] text-[var(--color-craft-text-secondary)] shrink-0">主题</span>
        <div class="flex items-center gap-1.5">
          <button
            v-for="color in themeStore.accentColors"
            :key="color"
            @click="themeStore.setAccent(color)"
            class="w-5 h-5 rounded-full border-2 transition-all duration-150 hover:scale-110"
            :class="themeStore.accentColor === color ? 'border-[var(--color-craft-text)] scale-110' : 'border-transparent'"
            :style="{ backgroundColor: accentMap[color] }"
            :title="color"
          />
        </div>
      </div>
    </div>
    <ModalDialog
      :visible="!!deletingFolderId"
      type="confirm"
      title="删除文件夹"
      message="确定删除此文件夹？文件夹内的笔记不会被删除。"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDeleteFolder"
      @cancel="deletingFolderId = null"
    />

    <ModalDialog
      :visible="showAlertModal"
      type="alert"
      title="提示"
      :message="alertMessage"
      confirm-text="确定"
      @confirm="showAlertModal = false"
      @cancel="showAlertModal = false"
    />

    <AvatarPicker
      :visible="showAvatarPicker"
      :current-avatar="authStore.userAvatar"
      :name="authStore.displayName"
      @select="onAvatarSelect"
      @close="showAvatarPicker = false"
    />

    <ChangePasswordDialog
      :visible="showChangePassword"
      @close="showChangePassword = false"
      @success="onPasswordChanged"
    />
  </aside>
</template>
