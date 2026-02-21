<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { organizationsApi, usersApi, type OrgMemberInfo, type OrganizationInfo, type UserSearchResult } from '@/api/organizations'
import { useAuthStore } from '@/stores/authStore'
import ModalDialog from '@/components/ui/ModalDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const orgId = computed(() => Number(route.params.orgId))
const org = ref<OrganizationInfo | null>(null)
const members = ref<OrgMemberInfo[]>([])
const loading = ref(true)
const error = ref('')

const searchQuery = ref('')
const searchResults = ref<UserSearchResult[]>([])
const searching = ref(false)
const addingUser = ref<number | null>(null)
const selectedRole = ref('MEMBER')

const showRemoveModal = ref(false)
const removingUserId = ref<number | null>(null)
const showDeleteOrgModal = ref(false)
const showAlertModal = ref(false)
const alertMessage = ref('')

const isOwner = computed(() => org.value?.role === 'OWNER')
const isAdmin = computed(() => org.value?.role === 'OWNER' || org.value?.role === 'ADMIN')

const editingName = ref(false)
const newOrgName = ref('')

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    org.value = await organizationsApi.get(orgId.value)
    members.value = await organizationsApi.listMembers(orgId.value)
    newOrgName.value = org.value.name
  } catch (e: any) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  if (searchQuery.value.trim().length < 2) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    searching.value = true
    try {
      const results = await usersApi.search(searchQuery.value.trim())
      const memberIds = new Set(members.value.map(m => m.userId))
      searchResults.value = results.filter(u => !memberIds.has(u.id))
    } catch {
      searchResults.value = []
    } finally {
      searching.value = false
    }
  }, 300)
}

function showAlert(msg: string) {
  alertMessage.value = msg
  showAlertModal.value = true
}

async function addMember(userId: number) {
  addingUser.value = userId
  try {
    await organizationsApi.addMember(orgId.value, userId, selectedRole.value)
    searchResults.value = searchResults.value.filter(u => u.id !== userId)
    searchQuery.value = ''
    searchResults.value = []
    await fetchData()
    authStore.fetchOrganizations()
  } catch (e: any) {
    showAlert(e.message || '添加失败')
  } finally {
    addingUser.value = null
  }
}

function removeMember(userId: number) {
  removingUserId.value = userId
  showRemoveModal.value = true
}

async function confirmRemoveMember() {
  showRemoveModal.value = false
  if (removingUserId.value === null) return
  try {
    await organizationsApi.removeMember(orgId.value, removingUserId.value)
    await fetchData()
    authStore.fetchOrganizations()
  } catch (e: any) {
    showAlert(e.message || '移除失败')
  }
  removingUserId.value = null
}

async function changeRole(userId: number, newRole: string) {
  try {
    await organizationsApi.updateMemberRole(orgId.value, userId, newRole)
    await fetchData()
  } catch (e: any) {
    showAlert(e.message || '修改角色失败')
  }
}

async function saveOrgName() {
  if (!newOrgName.value.trim() || newOrgName.value === org.value?.name) {
    editingName.value = false
    return
  }
  try {
    org.value = await organizationsApi.update(orgId.value, newOrgName.value.trim())
    editingName.value = false
    authStore.fetchOrganizations()
  } catch (e: any) {
    showAlert(e.message || '修改失败')
  }
}

function deleteOrg() {
  showDeleteOrgModal.value = true
}

async function confirmDeleteOrg() {
  showDeleteOrgModal.value = false
  try {
    await organizationsApi.delete(orgId.value)
    authStore.fetchOrganizations()
    authStore.switchToPersonal()
    router.push('/notes')
  } catch (e: any) {
    showAlert(e.message || '删除失败')
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="h-full overflow-y-auto">
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- Header -->
      <button @click="router.back()" class="text-sm text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-text)] mb-4 flex items-center gap-1 transition-colors">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/></svg>
        返回
      </button>

      <div v-if="loading" class="text-center py-20 text-[var(--color-craft-text-secondary)]">加载中...</div>
      <div v-else-if="error" class="text-center py-20 text-[var(--color-craft-danger)]">{{ error }}</div>
      <template v-else-if="org">
        <!-- Org Info -->
        <div class="flex items-center gap-4 mb-8">
          <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-white shadow-lg" :style="{ background: 'var(--color-craft-accent)' }">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <div class="flex-1">
            <div v-if="editingName && isOwner" class="flex items-center gap-2">
              <input v-model="newOrgName" @keydown.enter="saveOrgName" @keydown.escape="editingName = false" class="text-2xl font-bold bg-[var(--color-craft-bg)] border border-[var(--color-craft-accent)] rounded-xl px-3 py-1 text-[var(--color-craft-text)] outline-none" autofocus />
              <button @click="saveOrgName" class="text-sm text-[var(--color-craft-accent)] hover:underline">保存</button>
              <button @click="editingName = false" class="text-sm text-[var(--color-craft-text-secondary)] hover:underline">取消</button>
            </div>
            <h1 v-else class="text-2xl font-bold text-[var(--color-craft-text)] flex items-center gap-2">
              {{ org.name }}
              <button v-if="isOwner" @click="editingName = true" class="text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-accent)] transition-colors" title="重命名">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
              </button>
            </h1>
            <p class="text-sm text-[var(--color-craft-text-secondary)] mt-0.5">
              {{ org.memberCount }} 名成员 · 创建者: {{ org.ownerName }} · 你的角色: <span class="font-medium">{{ org.role }}</span>
            </p>
          </div>
          <button v-if="isOwner" @click="deleteOrg" class="px-4 py-2 rounded-xl text-sm border border-[var(--color-craft-danger)] text-[var(--color-craft-danger)] hover:bg-[var(--color-craft-danger)] hover:text-white transition-all">
            删除组织
          </button>
        </div>

        <!-- Add Member (admin+) -->
        <div v-if="isAdmin" class="mb-8 bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)] p-5">
          <h2 class="text-base font-semibold text-[var(--color-craft-text)] mb-3">添加成员</h2>
          <div class="flex gap-3">
            <div class="flex-1 relative">
              <input
                v-model="searchQuery"
                @input="onSearchInput"
                type="text"
                placeholder="搜索用户名、邮箱或昵称..."
                class="w-full px-4 py-2.5 rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] text-sm outline-none focus:border-[var(--color-craft-accent)] transition"
              />
              <!-- Search results dropdown -->
              <div v-if="searchResults.length > 0" class="absolute left-0 right-0 top-full mt-1 z-50 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-xl max-h-60 overflow-y-auto">
                <button
                  v-for="user in searchResults"
                  :key="user.id"
                  @click="addMember(user.id)"
                  :disabled="addingUser === user.id"
                  class="w-full flex items-center gap-3 px-4 py-3 text-sm hover:bg-[var(--color-craft-hover)] transition-colors text-left disabled:opacity-50"
                >
                  <div class="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-400 to-purple-500 flex items-center justify-center text-white text-xs font-bold shrink-0">
                    {{ (user.displayName || user.username).charAt(0).toUpperCase() }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-[var(--color-craft-text)] truncate">{{ user.displayName || user.username }}</div>
                    <div class="text-xs text-[var(--color-craft-text-secondary)] truncate">@{{ user.username }} · {{ user.email }}</div>
                  </div>
                  <span class="text-xs text-[var(--color-craft-accent)] shrink-0">
                    {{ addingUser === user.id ? '添加中...' : '+ 添加' }}
                  </span>
                </button>
              </div>
              <div v-else-if="searching" class="absolute left-0 right-0 top-full mt-1 z-50 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-xl p-4 text-center text-sm text-[var(--color-craft-text-secondary)]">
                搜索中...
              </div>
              <div v-else-if="searchQuery.trim().length >= 2 && searchResults.length === 0 && !searching" class="absolute left-0 right-0 top-full mt-1 z-50 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-xl p-4 text-center text-sm text-[var(--color-craft-text-secondary)]">
                未找到匹配用户
              </div>
            </div>
            <select v-model="selectedRole" class="px-3 py-2.5 rounded-xl border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] text-sm outline-none">
              <option value="MEMBER">成员</option>
              <option value="ADMIN">管理员</option>
            </select>
          </div>
        </div>

        <!-- Members List -->
        <div class="bg-[var(--color-craft-surface)] rounded-2xl border border-[var(--color-craft-border)]">
          <div class="px-5 py-4 border-b border-[var(--color-craft-border)]">
            <h2 class="text-base font-semibold text-[var(--color-craft-text)]">成员列表 ({{ members.length }})</h2>
          </div>
          <div class="divide-y divide-[var(--color-craft-border)]">
            <div v-for="member in members" :key="member.id" class="flex items-center gap-4 px-5 py-4 hover:bg-[var(--color-craft-hover)] transition-colors">
              <div class="w-10 h-10 rounded-full flex items-center justify-center text-white text-sm font-bold shrink-0"
                :class="member.role === 'OWNER' ? 'bg-gradient-to-br from-amber-400 to-orange-500' : member.role === 'ADMIN' ? 'bg-gradient-to-br from-emerald-400 to-teal-500' : 'bg-gradient-to-br from-indigo-400 to-purple-500'"
              >
                {{ (member.displayName || member.username).charAt(0).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium text-[var(--color-craft-text)] truncate">{{ member.displayName || member.username }}</div>
                <div class="text-xs text-[var(--color-craft-text-secondary)]">@{{ member.username }}</div>
              </div>
              <!-- Role badge / selector -->
              <div v-if="member.role === 'OWNER'" class="px-3 py-1 rounded-full text-xs font-medium bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                所有者
              </div>
              <template v-else-if="isOwner">
                <select
                  :value="member.role"
                  @change="changeRole(member.userId, ($event.target as HTMLSelectElement).value)"
                  class="px-3 py-1.5 rounded-lg border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] text-xs outline-none"
                >
                  <option value="ADMIN">管理员</option>
                  <option value="MEMBER">成员</option>
                </select>
                <button @click="removeMember(member.userId)" class="w-8 h-8 flex items-center justify-center rounded-lg text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] hover:bg-[var(--color-craft-hover)] transition-all" title="移除成员">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                </button>
              </template>
              <template v-else-if="isAdmin && member.role !== 'OWNER'">
                <div class="px-3 py-1 rounded-full text-xs font-medium" :class="member.role === 'ADMIN' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'">
                  {{ member.role === 'ADMIN' ? '管理员' : '成员' }}
                </div>
                <button @click="removeMember(member.userId)" class="w-8 h-8 flex items-center justify-center rounded-lg text-[var(--color-craft-text-secondary)] hover:text-[var(--color-craft-danger)] hover:bg-[var(--color-craft-hover)] transition-all" title="移除成员">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                </button>
              </template>
              <div v-else class="px-3 py-1 rounded-full text-xs font-medium" :class="member.role === 'ADMIN' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'">
                {{ member.role === 'ADMIN' ? '管理员' : '成员' }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <ModalDialog
      :visible="showRemoveModal"
      type="confirm"
      title="移除成员"
      message="确定移除该成员？"
      confirm-text="移除"
      :danger="true"
      @confirm="confirmRemoveMember"
      @cancel="showRemoveModal = false"
    />

    <ModalDialog
      :visible="showDeleteOrgModal"
      type="confirm"
      :title="`删除组织`"
      :message="`确定要删除组织「${org?.name}」？此操作不可恢复。`"
      confirm-text="删除"
      :danger="true"
      @confirm="confirmDeleteOrg"
      @cancel="showDeleteOrgModal = false"
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
  </div>
</template>
