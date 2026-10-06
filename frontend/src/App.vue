<template>
  <div id="app">
    <header class="header">
      <div class="container">
        <router-link to="/" class="logo">生活分享</router-link>
        <nav class="nav">
          <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
          <router-link to="/create" :class="{ active: $route.path === '/create' }">发布</router-link>
          <template v-if="isLoggedIn">
            <router-link to="/profile" :class="{ active: $route.path === '/profile' }">
              <div class="user-info">
                <img :src="getAvatarUrl(currentUser?.avatar)" alt="avatar">
                <span>{{ currentUser?.username }}</span>
              </div>
            </router-link>
            <a href="#" @click.prevent="logout">退出</a>
          </template>
          <template v-else>
            <router-link to="/login">登录</router-link>
            <router-link to="/register">注册</router-link>
          </template>
        </nav>
      </div>
    </header>
    <main class="main">
      <div class="container">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

export default {
  name: 'App',
  setup() {
    const router = useRouter()
    const route = useRoute()
    const currentUser = ref(null)

    const isLoggedIn = computed(() => currentUser.value !== null)

    const loadUser = () => {
      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        try {
          currentUser.value = JSON.parse(savedUser)
        } catch (e) {
          currentUser.value = null
        }
      } else {
        currentUser.value = null
      }
    }

    const logout = () => {
      localStorage.removeItem('user')
      currentUser.value = null
      router.push('/')
    }

    const getAvatarUrl = (avatar) => {
      if (!avatar) {
        return 'http://localhost:8080/uploads/images/user/user.png'
      }
      if (avatar.startsWith('http')) {
        return avatar
      }
      return 'http://localhost:8080' + avatar
    }

    onMounted(() => {
      loadUser()
    })

    watch(() => route.path, () => {
      loadUser()
    })

    return { currentUser, isLoggedIn, logout, getAvatarUrl }
  }
}
</script>
