<template>
  <div class="page-container login-page">
    <div class="login-card">
      <div class="login-card__brand">
        <h2>InterView AI</h2>
      </div>

      <div class="login-tabs">
        <button :class="['login-tab', { 'is-active': !isRegister }]" @click="isRegister = false">
          登录
        </button>
        <button :class="['login-tab', { 'is-active': isRegister }]" @click="isRegister = true">
          注册
        </button>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item prop="username">
          <template #label><span class="form-label">用户名</span></template>
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item prop="password">
          <template #label><span class="form-label">密码</span></template>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>

        <el-form-item v-if="isRegister" prop="email">
          <template #label><span class="form-label">邮箱（选填）</span></template>
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-button
          type="primary"
          native-type="submit"
          :loading="loading"
          class="login-submit"
        >
          {{ isRegister ? '注册' : '登录' }}
        </el-button>
      </el-form>

      <p class="login-switch">
        {{ isRegister ? '已有账号？' : '没有账号？' }}
        <a href="#" @click.prevent="isRegister = !isRegister">
          {{ isRegister ? '立即登录' : '立即注册' }}
        </a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, setToken, setUsername } from '../api/index.js'

const router = useRouter()
const formRef = ref(null)
const isRegister = ref(false)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  email: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    if (isRegister.value) {
      await register({
        username: form.username,
        password: form.password,
        email: form.email,
      })
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      form.password = ''
    } else {
      const res = await login({
        username: form.username,
        password: form.password,
      })
      setToken(res.token)
      setUsername(res.username)
      ElMessage.success('登录成功')
      router.push('/')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 60px);
}

.login-card {
  width: 100%;
  max-width: 400px;
  background-color: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 32px 24px;
}

.login-card__brand {
  text-align: center;
  margin-bottom: 32px;
}

.login-card__brand h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1F2937;
}

.login-tabs {
  display: flex;
  background-color: #F9FAFB;
  border-radius: 9999px;
  padding: 3px;
  margin-bottom: 24px;
}

.login-tab {
  flex: 1;
  padding: 6px 0;
  text-align: center;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 500;
  color: #6B7280;
  cursor: pointer;
  border-radius: 9999px;
  transition: all 0.15s;
}

.login-tab.is-active {
  background-color: #F3F4F6;
  color: #1F2937;
}

.login-submit {
  width: 100%;
  margin-top: 4px;
}

.login-switch {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #6B7280;
}

.login-switch a {
  color: #6366F1;
}

.form-label {
  font-size: 14px;
  color: #4B5563;
}
</style>
