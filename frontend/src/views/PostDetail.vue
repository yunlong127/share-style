<template>
  <div class="post-detail">
    <div v-if="loading" class="loading">加载中...</div>
    
    <template v-else-if="post">
      <h1>{{ post.title }}</h1>
      <div class="author">
        <img :src="getAvatarUrl(post.user?.avatar)" alt="avatar">
        <span>{{ post.user?.nickname || '未知用户' }}</span>
        <span>·</span>
        <span>{{ formatDate(post.createdAt) }}</span>
      </div>
      
      <div class="content" v-html="formatContent(post.content)"></div>
      
      <div class="stats">
        <span>👁 {{ post.viewCount }} 次浏览</span>
        <span>❤️ {{ post.likeCount }} 个赞</span>
        <span>💬 {{ post.commentCount }} 条评论</span>
        <button class="btn btn-primary" @click="handleLike">点赞</button>
        <div v-if="user && user.id === post.user.id" class="post-actions">
          <router-link :to="`/edit/${post.id}`" class="btn btn-secondary">编辑</router-link>
          <button class="btn btn-danger" @click="handleDelete">删除</button>
        </div>
      </div>

      <div class="comment-section">
        <h3>评论 ({{ comments.length }})</h3>
        
        <div v-if="user" class="card" style="margin-bottom: 20px; position: relative;">
          <textarea 
            v-model="newComment" 
            placeholder="写下你的评论..."
            style="min-height: 80px; width: 100%; box-sizing: border-box;"
          ></textarea>
          <div style="text-align: right; margin-top: 10px;">
            <button 
              class="btn btn-primary" 
              @click="submitComment"
            >发表评论</button>
          </div>
        </div>
        
        <div v-else class="card" style="text-align: center;">
          <router-link to="/login">登录后参与评论</router-link>
        </div>

        <div v-for="comment in comments" :key="comment.id" class="comment">
          <div class="author">
            <img :src="getAvatarUrl(comment.user?.avatar)" alt="avatar">
            <span>{{ comment.user?.nickname || '匿名用户' }}</span>
            <span class="time">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <p>{{ comment.content }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { postApi, commentApi } from '../api'

export default {
  name: 'PostDetail',
  setup() {
    const route = useRoute()
    const post = ref(null)
    const comments = ref([])
    const loading = ref(true)
    const newComment = ref('')
    const user = ref(null)

    const fetchData = async () => {
      loading.value = true
      try {
        const res = await postApi.getOne(route.params.id)
        post.value = res.data.post
        const commentRes = await commentApi.getByPost(route.params.id)
        comments.value = commentRes.data
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }

    const handleLike = async () => {
      try {
        await postApi.like(post.value.id)
        post.value.likeCount++
      } catch (e) {
        console.error(e)
      }
    }

    const handleDelete = async () => {
      if (!confirm('确定要删除这篇帖子吗？')) return
      
      try {
        await postApi.delete(post.value.id)
        alert('删除成功')
        window.location.href = '/'
      } catch (e) {
        alert('删除失败')
      }
    }

    const submitComment = async () => {
      if (!newComment.value.trim()) return
      
      const currentUser = JSON.parse(localStorage.getItem('user'))
      try {
        await commentApi.create(currentUser.id, {
          postId: post.value.id,
          content: newComment.value
        })
        newComment.value = ''
        fetchData()
      } catch (e) {
        alert('评论失败')
      }
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleDateString('zh-CN')
    }

    const formatContent = (content) => {
      if (!content) return ''
      
      // 检查是否是纯视频URL（不是Markdown格式）
      if (content.match(/^\/uploads\/.*\.(mp4|webm|ogg)$/i) || content.match(/^http.*\.(mp4|webm|ogg)$/i)) {
        const fullUrl = content.startsWith('http') ? content : 'http://localhost:8080' + content
        return `<video controls style="max-width: 100%; margin: 10px 0;"><source src="${fullUrl}" type="video/mp4"></video>`
      }
      
      // 处理Markdown图片格式: ![alt](url)
      content = content.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (match, alt, url) => {
        // 如果URL不是以http开头，添加后端服务器地址
        const fullUrl = url.startsWith('http') ? url : 'http://localhost:8080' + url
        // 检查是否是视频文件
        if (url.endsWith('.mp4') || url.endsWith('.webm') || url.endsWith('.ogg')) {
          return `<video controls style="max-width: 100%; margin: 10px 0;"><source src="${fullUrl}" type="video/mp4"></video>`
        }
        // 否则显示图片
        return `<img src="${fullUrl}" alt="${alt}" style="max-width: 100%;">`
      })
      // 处理换行
      return content.replace(/\n/g, '<br>')
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
      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        user.value = JSON.parse(savedUser)
      }
      fetchData()
    })

    return { post, comments, loading, newComment, user, handleLike, handleDelete, submitComment, formatDate, formatContent, getAvatarUrl }
  }
}
</script>

<style scoped>
.post-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  min-height: 80vh;
}

body {
  background: #f5f5f5;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.post-detail h1 {
  font-size: 24px;
  margin: 0 0 20px 0;
  color: #333;
  word-wrap: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  line-height: 1.3;
}

.author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 20px 0;
  font-size: 14px;
  color: #666;
}

.author img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.content {
  margin: 20px 0;
  line-height: 1.6;
}

.content img {
  max-width: 100%;
  height: auto;
  margin: 10px 0;
}

.stats {
  display: flex;
  gap: 20px;
  margin: 20px 0;
  font-size: 14px;
  color: #666;
}

.comment-section {
  margin-top: 40px;
}

.comment-section h3 {
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 20px;
  background: #f9f9f9;
}

.card textarea {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.comment {
  border-bottom: 1px solid #eee;
  padding: 15px 0;
}

.comment .author {
  margin: 0 0 10px 0;
}

.comment .time {
  font-size: 12px;
  color: #999;
}

.loading {
  text-align: center;
  padding: 50px;
  color: #666;
}
</style>
