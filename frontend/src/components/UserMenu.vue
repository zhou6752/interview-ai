<template>
  <div class="user-menu" ref="menuRef">
    <button class="user-menu__trigger" @click="open = !open">
      <span>{{ username }}</span>
      <span class="user-menu__arrow" :class="{ 'is-open': open }">▾</span>
    </button>
    <div v-if="open" class="user-menu__panel">
      <div class="user-menu__header">{{ username }}</div>
      <div class="user-menu__divider"></div>
      <button class="user-menu__item" @click="navigate('/history')">历史记录</button>
      <button class="user-menu__item" @click="navigate('/rag-chat')">知识库问答</button>
      <button class="user-menu__item" @click="navigate('/settings')">设置</button>
      <div class="user-menu__divider"></div>
      <button class="user-menu__item user-menu__item--danger" @click="$emit('logout')">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

defineProps({ username: { type: String, default: '' } })
defineEmits(['logout'])

const router = useRouter()
const open = ref(false)
const menuRef = ref(null)

function navigate(path) { open.value = false; router.push(path) }
function handleClickOutside(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) open.value = false
}
onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.user-menu { position: relative; }
.user-menu__trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  color: #4B5563;
  font-size: 14px;
}
.user-menu__trigger:hover {
  color: #1F2937;
  background-color: #F9FAFB;
}
.user-menu__arrow { font-size: 10px; transition: transform 0.2s; }
.user-menu__arrow.is-open { transform: rotate(180deg); }
.user-menu__panel {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  min-width: 160px;
  background-color: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  padding: 8px 0;
  box-shadow: var(--shadow-lg);
  z-index: 200;
}
.user-menu__header { padding: 8px 16px; font-size: 14px; color: #6B7280; }
.user-menu__divider { height: 1px; background-color: #E5E7EB; margin: 4px 0; }
.user-menu__item {
  display: block; width: 100%; text-align: left;
  background: none; border: none;
  padding: 8px 16px; font-size: 14px; color: #4B5563; cursor: pointer;
}
.user-menu__item:hover { background-color: #F9FAFB; color: #1F2937; }
.user-menu__item--danger:hover { color: #DC2626; }
</style>