<template>
  <nav class="app-nav">
    <div class="app-nav__inner">
      <router-link to="/" class="app-nav__brand">
        <span class="app-nav__logo">◆</span>
        InterView AI
      </router-link>
      <div class="app-nav__center">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="app-nav__link"
          :class="{ 'is-active': isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </div>
      <div class="app-nav__right">
        <template v-if="loggedIn">
          <UserMenu :username="username" @logout="handleLogout" />
        </template>
        <router-link v-else to="/login" class="app-nav__login">登录</router-link>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserMenu from './UserMenu.vue'

const route = useRoute()
const router = useRouter()

const navItems = [
  { path: '/', label: '首页' },
  { path: '/resume', label: '简历诊断' },
  { path: '/interview', label: '模拟面试' },
  { path: '/knowledge', label: '知识库' },
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const loggedIn = ref(!!localStorage.getItem('token'))
const username = ref(localStorage.getItem('username') || '')

function updateAuth() {
  loggedIn.value = !!localStorage.getItem('token')
  username.value = localStorage.getItem('username') || ''
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  loggedIn.value = false
  username.value = ''
  router.push('/login')
}

watch(() => route.path, updateAuth)
onMounted(updateAuth)
</script>

<style scoped>
.app-nav {
  height: 56px;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;
  position: sticky;
  top: 0;
  z-index: 100;
}
.app-nav__inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
}
.app-nav__brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #1F2937;
  text-decoration: none;
}
.app-nav__logo {
  color: #6366F1;
  font-size: 18px;
}
.app-nav__center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 32px;
}
.app-nav__link {
  font-size: 14px;
  color: #6B7280;
  text-decoration: none;
  padding: 4px 0;
  position: relative;
  transition: color 0.15s;
}
.app-nav__link:hover {
  color: #1F2937;
}
.app-nav__link.is-active {
  color: #6366F1;
}
.app-nav__link.is-active::after {
  content: '';
  position: absolute;
  bottom: -17px;
  left: 0;
  right: 0;
  height: 2px;
  background-color: #6366F1;
}
.app-nav__right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.app-nav__login {
  font-size: 14px;
  color: #6366F1;
  text-decoration: none;
  font-weight: 500;
}
@media (max-width: 768px) {
  .app-nav__center { display: none; }
}
</style>