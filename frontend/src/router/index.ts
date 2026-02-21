import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    { path: '/', redirect: '/notes' },
    { path: '/notes', name: 'notes', component: () => import('@/views/NotesView.vue') },
    { path: '/notes/folder/:folderId', name: 'folder', component: () => import('@/views/NotesView.vue') },
    { path: '/tasks', name: 'tasks', component: () => import('@/views/TasksView.vue') },
    { path: '/daily', name: 'daily', component: () => import('@/views/DailyNoteView.vue') },
    { path: '/calendar', name: 'calendar', component: () => import('@/views/CalendarView.vue') },
    { path: '/whiteboard', name: 'whiteboard', component: () => import('@/views/WhiteboardView.vue') },
    { path: '/graph', name: 'graph', component: () => import('@/views/KnowledgeGraphView.vue') },
    { path: '/stats', name: 'stats', component: () => import('@/views/StatsView.vue') },
    { path: '/org/:orgId/manage', name: 'org-manage', component: () => import('@/views/OrgManageView.vue') },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const token = localStorage.getItem('cr-notebook-token')
  const user = localStorage.getItem('cr-notebook-user')
  if (!token || !user) {
    localStorage.removeItem('cr-notebook-token')
    localStorage.removeItem('cr-notebook-user')
    localStorage.removeItem('cr-notebook-tenant-id')
    localStorage.removeItem('cr-notebook-tenant-type')
    return { name: 'login' }
  }
  return true
})

export default router
