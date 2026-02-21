import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type AuthResponse } from '@/api/auth'
import { organizationsApi, type OrganizationInfo } from '@/api/organizations'

const TOKEN_KEY = 'cr-notebook-token'
const USER_KEY = 'cr-notebook-user'
const TENANT_ID_KEY = 'cr-notebook-tenant-id'
const TENANT_TYPE_KEY = 'cr-notebook-tenant-type'

interface UserInfo {
  userId: number
  username: string
  email: string
  displayName: string
  avatarUrl?: string
}

export interface TenantInfo {
  id: number
  type: 'PERSONAL' | 'ORGANIZATION'
  name: string
}

function loadUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<UserInfo | null>(loadUser())
  const loading = ref(false)
  const error = ref<string | null>(null)

  const currentTenant = ref<TenantInfo | null>(null)
  const organizations = ref<OrganizationInfo[]>([])

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const displayName = computed(() => user.value?.displayName || user.value?.username || '')
  const userAvatar = computed(() => user.value?.avatarUrl || null)
  const isPersonalSpace = computed(() => !currentTenant.value || currentTenant.value.type === 'PERSONAL')
  const currentSpaceName = computed(() => {
    if (!currentTenant.value || currentTenant.value.type === 'PERSONAL') return '个人空间'
    return currentTenant.value.name
  })

  function saveAuth(resp: AuthResponse) {
    token.value = resp.token
    user.value = {
      userId: resp.userId,
      username: resp.username,
      email: resp.email,
      displayName: resp.displayName,
      avatarUrl: resp.avatarUrl,
    }
    localStorage.setItem(TOKEN_KEY, resp.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    switchToPersonal()
  }

  async function updateUserAvatar(avatarUrl: string | null) {
    const { usersApi } = await import('@/api/organizations')
    await usersApi.updateAvatar(avatarUrl)
    if (user.value) {
      user.value = { ...user.value, avatarUrl: avatarUrl || undefined }
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    }
  }

  function switchToPersonal() {
    if (!user.value) return
    currentTenant.value = { id: user.value.userId, type: 'PERSONAL', name: '个人空间' }
    localStorage.setItem(TENANT_ID_KEY, String(user.value.userId))
    localStorage.setItem(TENANT_TYPE_KEY, 'PERSONAL')
  }

  function switchToOrg(org: OrganizationInfo) {
    currentTenant.value = { id: org.id, type: 'ORGANIZATION', name: org.name }
    localStorage.setItem(TENANT_ID_KEY, String(org.id))
    localStorage.setItem(TENANT_TYPE_KEY, 'ORGANIZATION')
  }

  async function fetchOrganizations() {
    try {
      organizations.value = await organizationsApi.list()
    } catch (e) {
      console.error('Failed to fetch organizations:', e)
    }
  }

  async function createOrganization(name: string) {
    try {
      const org = await organizationsApi.create(name)
      organizations.value.push(org)
      return org
    } catch (e: any) {
      throw e
    }
  }

  async function login(usernameOrEmail: string, password: string) {
    loading.value = true
    error.value = null
    try {
      const resp = await authApi.login({ usernameOrEmail, password })
      saveAuth(resp)
    } catch (e: any) {
      error.value = e.message || '登录失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function register(username: string, email: string, password: string, displayName?: string) {
    loading.value = true
    error.value = null
    try {
      const resp = await authApi.register({ username, email, password, displayName })
      saveAuth(resp)
    } catch (e: any) {
      error.value = e.message || '注册失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = null
    user.value = null
    currentTenant.value = null
    organizations.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(TENANT_ID_KEY)
    localStorage.removeItem(TENANT_TYPE_KEY)
  }

  function initTenant() {
    if (!user.value) return
    const savedType = localStorage.getItem(TENANT_TYPE_KEY)
    const savedId = localStorage.getItem(TENANT_ID_KEY)
    if (savedType === 'ORGANIZATION' && savedId) {
      currentTenant.value = { id: Number(savedId), type: 'ORGANIZATION', name: '' }
      fetchOrganizations().then(() => {
        const org = organizations.value.find((o) => o.id === Number(savedId))
        if (org) currentTenant.value = { id: org.id, type: 'ORGANIZATION', name: org.name }
        else switchToPersonal()
      })
    } else {
      switchToPersonal()
    }
  }

  return {
    token, user, loading, error,
    isAuthenticated, displayName, userAvatar,
    currentTenant, organizations,
    isPersonalSpace, currentSpaceName,
    login, register, logout,
    switchToPersonal, switchToOrg,
    fetchOrganizations, createOrganization, initTenant,
    updateUserAvatar,
  }
})
