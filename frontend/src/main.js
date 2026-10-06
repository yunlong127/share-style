import { createApp, provide, ref } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)

// 全局用户状态
const user = ref(null)

// 初始化用户状态
const initUser = () => {
  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    user.value = JSON.parse(savedUser)
  }
}

// 更新用户状态
const updateUser = (userData) => {
  user.value = userData
  if (userData) {
    localStorage.setItem('user', JSON.stringify(userData))
  } else {
    localStorage.removeItem('user')
  }
}

// 提供全局状态
app.provide('user', user)
app.provide('updateUser', updateUser)

// 初始化用户
initUser()

app.use(router).mount('#app')
