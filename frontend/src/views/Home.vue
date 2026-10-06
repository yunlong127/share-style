<template>
  <div class="home-container">
    <div class="tabs">
      <button 
        class="tab" 
        :class="{ active: activeType === '' }" 
        @click="activeType = ''"
      >全部</button>
      <button 
        class="tab" 
        :class="{ active: activeType === 'article' }" 
        @click="activeType = 'article'"
      >文章</button>
      <button 
        class="tab" 
        :class="{ active: activeType === 'image' }" 
        @click="activeType = 'image'"
      >图片</button>
      <button 
        class="tab" 
        :class="{ active: activeType === 'video' }" 
        @click="activeType = 'video'"
      >视频</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    
    <div v-else-if="posts.length === 0" class="empty">
      <p>暂无内容</p>
      <router-link to="/create" class="btn btn-primary">发布第一篇内容</router-link>
    </div>
    
    <div v-else>
      <!-- 当选择了特定类型时，只显示一个列 -->
      <div v-if="activeType" class="single-column">
        <h2>{{ getTypeName(activeType) }}</h2>
        <div class="post-grid">
          <div 
            v-for="post in posts" 
            :key="post.id" 
            class="card post-card"
            @click="goToPost(post.id)"
          >
            <div class="cover-container">
              <img 
                v-if="getCoverImage(post)" 
                :src="getCoverImage(post)" 
                class="cover"
                alt="cover"
              >
              <div v-if="post.contentType === 'video'" class="video-badge">
                <span>▶ 视频</span>
              </div>
            </div>
            <h3>{{ truncateTitle(post.title, 20) }}</h3>
            <p class="content" v-if="post.contentType === 'article'">{{ truncate(post.content, 100) }}</p>
            <div class="meta">
              <span>👁 {{ post.viewCount }}</span>
              <span>❤️ {{ post.likeCount }}</span>
              <span>💬 {{ post.commentCount }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 当选择全部时，显示左右两栏布局 -->
      <div v-else class="home-layout">
        <!-- 左侧：纯文章 -->
        <div class="left-column">
          <h2>文章</h2>
          <div class="post-grid">
            <div 
              v-for="post in posts.filter(p => p.contentType === 'article')" 
              :key="post.id" 
              class="card post-card"
              @click="goToPost(post.id)"
            >
              <h3>{{ truncateTitle(post.title, 20) }}</h3>
              <p class="content">{{ truncate(post.content, 100) }}</p>
              <div class="meta">
                <span>👁 {{ post.viewCount }}</span>
                <span>❤️ {{ post.likeCount }}</span>
                <span>💬 {{ post.commentCount }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 右侧：图片/视频 -->
        <div class="right-column">
          <h2>图片/视频</h2>
          <div class="post-grid">
            <div 
              v-for="post in posts.filter(p => p.contentType === 'image' || p.contentType === 'video')" 
              :key="post.id" 
              class="card post-card"
              @click="goToPost(post.id)"
            >
              <div class="cover-container">
                <img 
                  v-if="getCoverImage(post)" 
                  :src="getCoverImage(post)" 
                  class="cover"
                  alt="cover"
                >
                <div v-if="post.contentType === 'video'" class="video-badge">
                  <span>▶ 视频</span>
                </div>
              </div>
              <h3>{{ truncateTitle(post.title, 20) }}</h3>
              <p class="content" v-if="post.contentType === 'article'">{{ truncate(post.content, 100) }}</p>
              <div class="meta">
                <span>👁 {{ post.viewCount }}</span>
                <span>❤️ {{ post.likeCount }}</span>
                <span>💬 {{ post.commentCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="posts.length > 0" style="text-align: center; margin-top: 20px;">
      <button 
        v-if="hasMore" 
        class="btn btn-secondary" 
        @click="loadMore"
      >加载更多</button>
    </div>
  </div>
</template>

<script>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '../api'

export default {
  name: 'Home',
  setup() {
    const router = useRouter()
    const posts = ref([])
    const loading = ref(true)
    const activeType = ref('')
    const page = ref(0)
    const hasMore = ref(true)

    const fetchPosts = async () => {
      loading.value = true
      try {
        const res = activeType.value 
          ? await postApi.getByType(activeType.value, page.value)
          : await postApi.getAll(page.value)
        if (page.value === 0) {
          posts.value = res.data.content || []
        } else {
          posts.value = [...posts.value, ...(res.data.content || [])]
        }
        hasMore.value = !res.data.last
      } catch (e) {
        console.error(e)
        if (page.value === 0) {
          posts.value = []
        }
      } finally {
        loading.value = false
      }
    }

    const loadMore = () => {
      page.value++
      fetchPosts()
    }

    const goToPost = (id) => {
      router.push(`/post/${id}`)
    }

    const getCoverImage = (post) => {
      if (!post) return null
      
      console.log('getCoverImage called with post:', post)
      
      // 优先使用帖子的coverImage
      if (post.coverImage) {
        const fullUrl = post.coverImage.startsWith('http') ? post.coverImage : 'http://localhost:8080' + post.coverImage
        console.log('Using coverImage:', fullUrl)
        return fullUrl
      }
      
      const content = post.content
      if (!content) return null
      
      // 检查是否是纯视频URL（不是Markdown格式）
      if (content.match(/^\/uploads\/.*\.(mp4|webm|ogg)$/i) || content.match(/^http.*\.(mp4|webm|ogg)$/i)) {
        console.log('Using default video cover for pure video URL')
        return 'http://localhost:8080/uploads/images/default-video-cover.svg'
      }
      
      // 提取第一张 Markdown 图片或视频的 URL
      const match = content.match(/!\[([^\]]*)\]\(([^)]+)\)/)
      if (match && match[2]) {
        // 如果是相对路径，添加后端服务器地址
        const url = match[2]
        const fullUrl = url.startsWith('http') ? url : 'http://localhost:8080' + url
        // 检查是否是视频文件
        if (url.endsWith('.mp4') || url.endsWith('.webm') || url.endsWith('.ogg')) {
          // 对于视频文件，返回一个默认的视频封面图片
          console.log('Using default video cover for video in Markdown')
          return 'http://localhost:8080/uploads/images/default-video-cover.svg'
        }
        // 对于图片文件，返回图片URL
        console.log('Using image from content:', fullUrl)
        return fullUrl
      }
      return null
    }

    const truncateTitle = (title, length) => {
      if (!title) return ''
      // 支持多行显示，只在过长时截断
      return title.length > length ? title.slice(0, length) + '...' : title
    }

    const truncate = (text, length) => {
      if (!text) return ''
      // 移除 Markdown 图片格式: ![alt](url)
      const textWithoutImages = text.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '')
      // 移除 HTML 标签
      const cleanText = textWithoutImages.replace(/<[^>]*>/g, '')
      return cleanText.length > length ? cleanText.slice(0, length) + '...' : cleanText
    }

    const getTypeName = (type) => {
      switch (type) {
        case 'article': return '文章'
        case 'image': return '图片'
        case 'video': return '视频'
        default: return '全部'
      }
    }

    watch(activeType, () => {
      page.value = 0
      fetchPosts()
    })

    onMounted(fetchPosts)

    return { posts, loading, activeType, hasMore, loadMore, goToPost, truncate, getCoverImage, truncateTitle, getTypeName }
  }
}
</script>

<style scoped>
body {
  background: #f5f5f5;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  min-height: 80vh;
}

.home-layout {
  display: flex;
  gap: 20px;
  margin: 20px 0;
}

.left-column,
.right-column {
  flex: 1;
  min-width: 0;
}

.left-column h2,
.right-column h2 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.post-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.cover-container {
  position: relative;
  width: 100%;
  margin-bottom: 10px;
}

.cover {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 6px;
}

.video-badge {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.post-card h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
  word-wrap: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  line-height: 1.3;
  max-width: 100%;
}

.content {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.4;
}

.meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 15px;
}

.single-column h2 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.tabs {
  display: flex;
  gap: 10px;
  margin: 20px 0;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.tab {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover {
  background: #f5f5f5;
}

.tab.active {
  background: #007bff;
  color: #fff;
  border-color: #007bff;
}

.loading {
  text-align: center;
  padding: 50px;
  color: #666;
}

.empty {
  text-align: center;
  padding: 50px;
  color: #999;
}

@media (max-width: 768px) {
  .home-layout {
    flex-direction: column;
  }
}
</style>
