import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ResumeAnalyzer from '../views/ResumeAnalyzer.vue'
import InterviewView from '../views/InterviewView.vue'
import InterviewHistory from '../views/InterviewHistory.vue'
import KnowledgeBase from '../views/KnowledgeBase.vue'
import RagChatView from '../views/RagChatView.vue'
import SettingsView from '../views/SettingsView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/resume', name: 'resume', component: ResumeAnalyzer },
  { path: '/interview', name: 'interview', component: InterviewView },
  { path: '/history', name: 'history', component: InterviewHistory },
  { path: '/knowledge', name: 'knowledge', component: KnowledgeBase },
  { path: '/rag-chat', name: 'ragChat', component: RagChatView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/settings', name: 'settings', component: SettingsView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const publicPages = ['/login']
  if (!token && !publicPages.includes(to.path)) {
    return { path: '/login' }
  }
  return true
})

export default router
