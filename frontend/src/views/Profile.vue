<template>
  <div class="profile-container">
    <div class="profile-form">
      <div class="card" style="max-width: 600px; margin: 0 auto 30px;">
        <h2 style="margin-bottom: 20px;">个人资料</h2>
        <div class="form-group">
          <label>用户名</label>
          <input :value="user?.username" disabled>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.nickname">
        </div>
        <div class="form-group">
          <label>个人简介</label>
          <textarea v-model="form.bio"></textarea>
        </div>
        <div class="form-group">
          <label>头像</label>
          <div style="display: flex; align-items: center; gap: 15px; margin-bottom: 10px;">
            <img 
              :src="getAvatarUrl(form.avatar)" 
              style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover;"
              alt="avatar"
            >
            <input 
              type="file" 
              accept="image/*" 
              @change="handleAvatarUpload" 
              style="display: none;"
              ref="avatarInput"
            >
            <button class="btn btn-secondary" @click="$refs.avatarInput.click()">上传头像</button>
          </div>
        </div>
        <div style="text-align: center; margin-top: 20px;">
          <button class="btn btn-primary" @click="updateProfile">保存修改</button>
        </div>
      </div>
    </div>

    <div class="my-posts">
      <h3 style="margin-bottom: 20px;">我的发布</h3>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="posts.length === 0" class="empty">
        <p>还没有发布任何内容</p>
        <router-link to="/create" class="btn btn-primary">发布第一篇内容</router-link>
      </div>
      <div v-else class="post-list">
        <div 
          v-for="post in posts" 
          :key="post.id" 
          class="card post-item"
          @click="$router.push(`/post/${post.id}`)"
        >
          <img 
            v-if="post.coverImage" 
            :src="post.coverImage" 
            class="cover"
            alt="cover"
          >
          <h3>{{ post.title }}</h3>
          <div class="meta">
            <span>👁 {{ post.viewCount }}</span>
            <span>❤️ {{ post.likeCount }}</span>
            <span>💬 {{ post.commentCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { userApi, postApi, fileApi } from '../api'

export default {
  name: 'Profile',
  setup() {
    const user = ref(null)
    const form = ref({
      nickname: '',
      bio: '',
      avatar: ''
    })
    const posts = ref([])
    const loading = ref(true)

    const fetchData = async () => {
      const savedUser = localStorage.getItem('user')
      if (!savedUser) return
      
      user.value = JSON.parse(savedUser)
      console.log('User data:', user.value)
      console.log('Avatar:', user.value.avatar)
      form.value = {
        nickname: user.value.nickname || '',
        bio: user.value.bio || '',
        avatar: user.value.avatar || ''
      }

      try {
        const res = await postApi.getByUser(user.value.id)
        posts.value = res.data.content || []
      } catch (e) {
        console.error(e)
        posts.value = []
      } finally {
        loading.value = false
      }
    }

    const updateProfile = async () => {
      try {
        const res = await userApi.updateProfile(user.value.id, form.value)
        localStorage.setItem('user', JSON.stringify(res.data))
        user.value = res.data
        alert('保存成功')
      } catch (e) {
        alert('保存失败')
      }
    }

    const handleAvatarUpload = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      try {
        const res = await fileApi.upload(file, 'user', user.value.id)
        form.value.avatar = res.data.url
        alert('头像上传成功')
      } catch (e) {
        alert('头像上传失败')
      }
    }

    const getAvatarUrl = (avatar) => {
      console.log('getAvatarUrl called with:', avatar)
      if (!avatar) {
        const url = 'http://localhost:8080/uploads/images/user/user.png'
        console.log('Returning default avatar:', url)
        return url
      }
      if (avatar.startsWith('http')) {
        console.log('Returning full URL:', avatar)
        return avatar
      }
      const url = 'http://localhost:8080' + avatar
      console.log('Returning constructed URL:', url)
      return url
    }

    onMounted(fetchData)

    return { user, form, posts, loading, updateProfile, handleAvatarUpload, getAvatarUrl }
  }
}
</script>

<style scoped>
.profile-container {
  display: flex;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.profile-form {
  flex: 1;
  min-width: 400px;
}

.my-posts {
  flex: 1;
  min-width: 400px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.post-item {
  padding: 15px;
  cursor: pointer;
  transition: box-shadow 0.3s ease;
}

.post-item:hover {
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.post-item .cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 10px;
}

.post-item h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
  word-wrap: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  line-height: 1.3;
  max-width: 100%;
}

.meta {
  display: flex;
  gap: 15px;
  font-size: 14px;
  color: #666;
  margin-top: 10px;
}

@media (max-width: 992px) {
  .profile-container {
    flex-direction: column;
  }
  
  .profile-form,
  .my-posts {
    min-width: 100%;
  }
}
</style>
