<template>
  <div class="auth-container card">
    <h2>登录</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label>手机号</label>
        <input v-model="form.username" type="tel" required>
      </div>
      <div class="form-group">
        <label>密码</label>
        <input v-model="form.password" type="password" required>
      </div>
      <button type="submit" class="btn btn-primary" style="width: 100%;">登录</button>
    </form>
    <div v-if="message" :class="['message', messageType]">
      {{ message }}
    </div>
    <p style="text-align: center; margin-top: 20px;">
      还没有账号？<router-link to="/register">立即注册</router-link>
    </p>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api'

export default {
  name: 'Login',
  emits: ['login-success'],
  setup(props, { emit }) {
    const router = useRouter()
    const form = ref({
      username: '',
      password: ''
    })
    const message = ref('')
    const messageType = ref('')

    const handleLogin = async () => {
      try {
        const res = await userApi.login(form.value)
        
        if (res.data.status === 'not_registered') {
          message.value = res.data.message
          messageType.value = 'error'
        } else if (res.data.status === 'password_error') {
          message.value = res.data.message
          messageType.value = 'error'
        } else if (res.data.status === 'success') {
          message.value = res.data.message
          messageType.value = 'success'
          localStorage.setItem('user', JSON.stringify(res.data.user))
          emit('login-success', res.data.user)
          setTimeout(() => {
            router.push('/')
          }, 1000)
        } else {
          message.value = '登录失败，请重试'
          messageType.value = 'error'
        }
      } catch (e) {
        message.value = '网络错误，请检查连接'
        messageType.value = 'error'
      }
    }

    return { form, message, messageType, handleLogin }
  }
}
</script>

<style scoped>
.message {
  margin-top: 20px;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}

.message.success {
  background-color: #d4edda;
  color: #155724;
}

.message.error {
  background-color: #f8d7da;
  color: #721c24;
}
</style>
