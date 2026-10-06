<template>
  <div class="card" style="max-width: 800px; margin: 0 auto;">
    <h2 style="margin-bottom: 20px;">发布内容</h2>
    <form @submit.prevent="handleSubmit">
      <div class="form-group">
        <label>标题 <span class="char-count">{{ form.title.length }}/100</span></label>
        <input 
          v-model="form.title" 
          type="text" 
          required 
          maxlength="100"
          placeholder="请输入标题"
        >
      </div>
      <div class="form-group">
        <label>内容类型</label>
        <select v-model="form.contentType" @change="handleContentTypeChange">
          <option value="article">文章</option>
          <option value="image">图片</option>
          <option value="video">视频</option>
        </select>
      </div>
      <div class="form-group">
        <label>内容 <span class="char-count">{{ form.content.length }}/5000</span></label>
        <textarea 
          v-model="form.content" 
          required 
          maxlength="5000"
          :placeholder="contentPlaceholder"
          rows="10"
          v-if="form.contentType === 'article'"
        ></textarea>
        <div v-else-if="form.contentType === 'image'" class="image-upload-section">
          <input 
            type="file" 
            multiple 
            accept="image/*" 
            @change="handleImageUpload"
            style="margin-bottom: 10px;"
          >
          <div v-if="uploading" class="uploading">
            上传中... {{ uploadProgress }}%
          </div>
          <div class="image-preview-container" v-if="imageUrls.length > 0">
            <div class="image-preview" v-for="(url, index) in imageUrls" :key="index">
              <img :src="getFullUrl(url)" alt="预览" style="max-width: 100px; max-height: 100px; margin: 5px;">
              <button type="button" class="btn btn-sm btn-danger" @click="removeImage(index)">删除</button>
            </div>
          </div>
        </div>
        <div v-else-if="form.contentType === 'video'" class="video-upload-section">
          <input 
            type="file" 
            accept="video/*" 
            @change="handleVideoUpload"
            style="margin-bottom: 10px;"
          >
          <div v-if="uploading" class="uploading">
            上传中... {{ uploadProgress }}%
          </div>
          <div class="video-preview-container" v-if="videoUrl">
            <video :src="getFullUrl(videoUrl)" controls style="max-width: 100%; max-height: 300px; margin: 10px 0;"></video>
            <button type="button" class="btn btn-sm btn-danger" @click="removeVideo">删除视频</button>
          </div>
        </div>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="uploading">发布</button>
    </form>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { postApi, fileApi } from '../api'

export default {
  name: 'CreatePost',
  setup() {
    const router = useRouter()
    const form = ref({
      title: '',
      content: '',
      contentType: 'article',
      coverImage: ''
    })
    const imageUrls = ref([])
    const videoUrl = ref('')
    const uploading = ref(false)
    const uploadProgress = ref(0)

    const contentPlaceholder = computed(() => {
      switch (form.value.contentType) {
        case 'image':
          return '请上传图片文件'
        case 'video':
          return '请上传视频文件'
        default:
          return '请输入文章内容...'
      }
    })

    const handleContentTypeChange = () => {
      if (form.value.contentType !== 'image') {
        imageUrls.value = []
      }
      if (form.value.contentType !== 'video') {
        videoUrl.value = ''
      }
      form.value.content = ''
    }

    const handleImageUpload = async (event) => {
      const files = event.target.files
      if (files.length === 0) return

      uploading.value = true
      uploadProgress.value = 0

      try {
        const uploadedUrls = []
        
        for (let i = 0; i < files.length; i++) {
          const file = files[i]
          uploadProgress.value = Math.round((i + 1) / files.length * 100)
          
          const response = await fileApi.upload(file, 'post')
          const url = response.data.url
          uploadedUrls.push(url)
        }

        imageUrls.value = [...imageUrls.value, ...uploadedUrls]
        form.value.content = imageUrls.value.map(url => `![图片](${url})`).join('\n')
        
        if (!form.value.coverImage && imageUrls.value.length > 0) {
          form.value.coverImage = imageUrls.value[0]
        }
      } catch (error) {
        alert('图片上传失败')
        console.error('上传失败:', error)
      } finally {
        uploading.value = false
        uploadProgress.value = 0
      }
    }

    const removeImage = (index) => {
      imageUrls.value.splice(index, 1)
      form.value.content = imageUrls.value.map(url => `![图片](${url})`).join('\n')
      
      if (form.value.coverImage === imageUrls.value[index]) {
        form.value.coverImage = imageUrls.value.length > 0 ? imageUrls.value[0] : ''
      }
    }

    const handleVideoUpload = async (event) => {
      const file = event.target.files[0]
      if (!file) return

      uploading.value = true
      uploadProgress.value = 0

      try {
        uploadProgress.value = 20
        
        // 第一步：上传视频文件
        const response = await fileApi.upload(file, 'post')
        const url = response.data.url
        videoUrl.value = url
        form.value.content = `![视频](${url})`
        
        uploadProgress.value = 50
        
        // 第二步：提取视频第一帧作为封面
        const coverImageFile = await extractVideoFrame(file)
        
        uploadProgress.value = 70
        
        // 第三步：上传封面图片
        if (coverImageFile) {
          const coverResponse = await fileApi.upload(coverImageFile, 'post')
          form.value.coverImage = coverResponse.data.url
        }
        
        uploadProgress.value = 100
      } catch (error) {
        alert('视频上传失败')
        console.error('上传失败:', error)
      } finally {
        uploading.value = false
        uploadProgress.value = 0
      }
    }
    
    const extractVideoFrame = (file) => {
      return new Promise((resolve) => {
        const video = document.createElement('video')
        video.src = URL.createObjectURL(file)
        video.crossOrigin = 'anonymous'
        video.muted = true
        video.preload = 'metadata'
        
        const tryCaptureFrame = () => {
          const canvas = document.createElement('canvas')
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight
          
          const ctx = canvas.getContext('2d')
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
          
          canvas.toBlob((blob) => {
            if (blob) {
              const coverFile = new File([blob], 'video-cover.jpg', { type: 'image/jpeg' })
              resolve(coverFile)
            } else {
              resolve(null)
            }
            URL.revokeObjectURL(video.src)
          }, 'image/jpeg', 0.9)
        }
        
        video.onloadedmetadata = () => {
          video.currentTime = Math.min(1, video.duration || 1)
        }
        
        video.onseeked = () => {
          setTimeout(tryCaptureFrame, 100)
        }
        
        video.onerror = () => {
          resolve(null)
          URL.revokeObjectURL(video.src)
        }
      })
    }

    const removeVideo = () => {
      videoUrl.value = ''
      form.value.content = ''
    }

    const getFullUrl = (url) => {
      if (!url) return ''
      if (url.startsWith('http')) return url
      return 'http://localhost:8080' + url
    }

    const handleSubmit = async () => {
      // 前端长度验证
      if (form.value.title.length > 100) {
        alert('标题长度不能超过100字符')
        return
      }
      if (form.value.content.length > 5000) {
        alert('内容长度不能超过5000字符')
        return
      }

      // 标题和内容不能为空
      if (!form.value.title || form.value.title.trim() === '') {
        alert('标题不能为空')
        return
      }
      if (!form.value.content || form.value.content.trim() === '') {
        alert('内容不能为空')
        return
      }

      const user = JSON.parse(localStorage.getItem('user'))
      if (!user) {
        router.push('/login')
        return
      }

      try {
        const response = await postApi.create(user.id, form.value)
        console.log('发布成功，响应:', response)
        alert('发布成功')
        router.push('/')
      } catch (e) {
        console.error('发布失败:', e)
        const errorMessage = e.response?.data?.error || '发布失败，请稍后重试'
        alert(`发布失败: ${errorMessage}`)
      }
    }

    return { 
      form, 
      contentPlaceholder, 
      handleSubmit,
      imageUrls,
      videoUrl,
      uploading,
      uploadProgress,
      handleImageUpload,
      removeImage,
      handleVideoUpload,
      removeVideo,
      handleContentTypeChange,
      getFullUrl
    }
  }
}
</script>

<style scoped>
.char-count {
  float: right;
  font-size: 12px;
  color: #999;
}

textarea {
  resize: vertical;
  min-height: 150px;
}

.image-upload-section {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.video-upload-section {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.uploading {
  margin: 10px 0;
  padding: 10px;
  background-color: #e3f2fd;
  border-radius: 4px;
  color: #1976d2;
}

.image-preview-container {
  margin-top: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.video-preview-container {
  margin-top: 15px;
  text-align: center;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview img {
  border: 1px solid #ddd;
  border-radius: 4px;
}

.image-preview .btn {
  position: absolute;
  top: -10px;
  right: -10px;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}
</style>
