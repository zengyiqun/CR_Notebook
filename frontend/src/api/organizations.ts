import { http } from './http'

export interface OrganizationInfo {
  id: number
  name: string
  ownerId: number
  ownerName: string
  role: string
  memberCount: number
  createdAt: string
  avatarUrl?: string
}

export interface OrgMemberInfo {
  id: number
  userId: number
  username: string
  displayName: string
  role: string
  joinedAt: string
}

export interface UserSearchResult {
  id: number
  username: string
  email: string
  displayName: string
  avatarUrl?: string
}

export interface DailyStatsItem {
  date: string
  noteCount: number
  taskCount: number
  taskCompletedCount: number
}

export interface StatsInfo {
  noteCount: number
  folderCount: number
  taskCount: number
  taskCompletedCount: number
  dailyNoteCount: number
  calendarEventCount: number
  whiteboardCount: number
  orgId?: number
  orgName?: string
  memberCount?: number
  memberStats?: {
    userId: number
    username: string
    displayName: string
    role: string
    noteCount: number
    taskCount: number
  }[]
  dailyStats?: DailyStatsItem[]
}

export const organizationsApi = {
  list: () => http.get<OrganizationInfo[]>('/organizations'),
  create: (name: string) => http.post<OrganizationInfo>('/organizations', { name }),
  get: (id: number) => http.get<OrganizationInfo>(`/organizations/${id}`),
  update: (id: number, name: string) => http.put<OrganizationInfo>(`/organizations/${id}`, { name }),
  delete: (id: number) => http.delete(`/organizations/${id}`),
  listMembers: (orgId: number) => http.get<OrgMemberInfo[]>(`/organizations/${orgId}/members`),
  addMember: (orgId: number, userId: number, role = 'MEMBER') =>
    http.post<OrgMemberInfo>(`/organizations/${orgId}/members`, { userId, role }),
  updateMemberRole: (orgId: number, userId: number, role: string) =>
    http.put<OrgMemberInfo>(`/organizations/${orgId}/members/${userId}`, { role }),
  removeMember: (orgId: number, userId: number) =>
    http.delete(`/organizations/${orgId}/members/${userId}`),
  updateAvatar: (orgId: number, avatarUrl: string | null) =>
    http.put<{ avatarUrl: string }>(`/organizations/${orgId}/avatar`, { avatarUrl }),
}

export const usersApi = {
  search: (q: string) => http.get<UserSearchResult[]>('/users/search', { q }),
  me: () => http.get<UserSearchResult>('/users/me'),
  updateAvatar: (avatarUrl: string | null) => http.put<{ avatarUrl: string }>('/users/me/avatar', { avatarUrl }),
  changePassword: (oldPassword: string, newPassword: string) =>
    http.put<{ message: string }>('/users/me/password', { oldPassword, newPassword }),
}

export const statsApi = {
  personal: (days = 7) => http.get<StatsInfo>('/stats/personal', { days }),
  org: (orgId: number, days = 7) => http.get<StatsInfo>(`/stats/org/${orgId}`, { days }),
}
