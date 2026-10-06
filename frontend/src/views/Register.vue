<template>
  <div class="auth-container card">
    <h2>注册</h2>
    <form @submit.prevent="handleRegister">
      <div class="form-group">
        <label>手机号</label>
        <input v-model="form.phone" type="tel" required>
      </div>
      <div class="form-group">
        <label>密码</label>
        <input v-model="form.password" type="password" required>
      </div>
      <div class="form-group">
        <label>确认密码</label>
        <input v-model="form.confirmPassword" type="password" required>
      </div>
      <div class="form-group">
        <label>昵称</label>
        <input v-model="form.nickname" type="text">
      </div>
      <button type="submit" class="btn btn-primary" style="width: 100%;">注册</button>
    </form>
    <p style="text-align: center; margin-top: 20px;">
      已有账号？<router-link to="/login">立即登录</router-link>
    </p>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '../api'

export default {
  name: 'Register',
  setup() {
    const router = useRouter()
    const form = ref({
      phone: '',
      password: '',
      confirmPassword: '',
      nickname: ''
    })

    const handleRegister = async () => {
      // 验证密码是否一致
      if (form.value.password !== form.value.confirmPassword) {
        alert('两次输入的密码不一致')
        return
      }

      try {
        await userApi.register({
          username: form.value.phone, // 使用手机号作为用户名
          password: form.value.password,
          nickname: form.value.nickname
        })
        alert('注册成功，请登录')
        router.push('/login')
      } catch (e) {
        alert('注册失败，手机号可能已存在')
      }
    }

    return { form, handleRegister }
  }
}
</script>
