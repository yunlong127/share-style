<template>
  <div class="create-post">
    <h2>编辑帖子</h2>
    <div class="card" style="max-width: 800px; margin: 0 auto;">
      <div class="form-group">
        <label>标题</label>
        <input v-model="form.title" placeholder="请输入标题" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px;">
      </div>
      <div class="form-group">
        <label>内容</label>
        <textarea v-model="form.content" placeholder="请输入内容" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; min-height: 200px;"></textarea>
      </div>
      <div class="form-group">
        <label>内容类型</label>
        <select v-model="form.contentType" style="width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px;">
          <option value="article">文章</option>
          <option value="image">图片</option>
          <option value="video">视频</option>
        </select>
      </div>
      <div class="form-group">
        <label>封面图片</label>
        <div style="display: flex; align-items: center; gap: 15px; margin-bottom: 10px;">
          <img 
            v-if="form.coverImage" 
            :src="form.coverImage" 
            style="width: 100px; height: 100px; object-fit: cover; border-radius: 4px;"
            alt="cover"
          >
          <input 
            type="file" 
            accept="image/*" 
            @change="handleCoverUpload" 
            style="display: none;"
            ref="coverInput"
          >
          <button class="btn btn-secondary" @click="$refs.coverInput.click()">上传封面</button>
        </div>
      </div>
      <div style="display: flex; gap: 10px; justify-content: center; margin-top: 20px;">
        <button class="btn btn-primary" @click="updatePost">保存修改</button>
        <button class="btn btn-secondary" @click="$router.push(`/post/${postId}`)">取消</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi, fileApi } from '../api'

export default {
  name: 'EditPost',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const postId = route.params.id
    const form = ref({
      title: '',
      content: '',
      contentType: 'article',
      coverImage: ''
    })

    const fetchPost = async () => {
      try {
        const res = await postApi.getOne(postId)
        const post = res.data.post
        form.value = {
          title: post.title,
          content: post.content,
          contentType: post.contentType,
          coverImage: post.coverImage || ''
        }
      } catch (e) {
        console.error(e)
        alert('加载帖子失败')
      }
    }

    const updatePost = async () => {
      if (!form.value.title.trim() || !form.value.content.trim()) {
        alert('标题和内容不能为空')
        return
      }

      try {
        await postApi.update(postId, form.value)
        alert('更新成功')
        router.push(`/post/${postId}`)
      } catch (e) {
        alert('更新失败')
      }
    }

    const handleCoverUpload = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      try {
        const res = await fileApi.upload(file, 'post', postId)
        form.value.coverImage = res.data.url
        alert('封面上传成功')
      } catch (e) {
        alert('封面上传失败')
      }
    }

    onMounted(fetchPost)

    return { form, postId, updatePost, handleCoverUpload }
  }
}
</script>

<style scoped>
.create-post {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.create-post h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
}
</style>