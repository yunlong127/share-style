import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const userApi = {
  register: (data) => api.post('/users/register', data),
  login: (data) => api.post('/users/login', data),
  getUser: (id) => api.get(`/users/${id}`),
  updateProfile: (id, data) => api.put(`/users/${id}`, null, { params: data })
}

export const postApi = {
  getAll: (page = 0, size = 10) => api.get('/posts', { params: { page, size } }),
  getByType: (type, page = 0, size = 10) => api.get(`/posts/type/${type}`, { params: { page, size } }),
  getByUser: (userId, page = 0, size = 10) => api.get(`/posts/user/${userId}`, { params: { page, size } }),
  getOne: (id) => api.get(`/posts/${id}`),
  create: (userId, data) => api.post('/posts', data, { params: { userId } }),
  update: (id, data) => api.put(`/posts/${id}`, data),
  delete: (id) => api.delete(`/posts/${id}`),
  like: (id) => api.post(`/posts/${id}/like`)
}

export const commentApi = {
  getByPost: (postId) => api.get(`/comments/post/${postId}`),
  create: (userId, data) => api.post('/comments', data, { params: { userId } }),
  delete: (id) => api.delete(`/comments/${id}`)
}

export const fileApi = {
  upload: (file, type, id) => {
    const formData = new FormData()
    formData.append('file', file)
    const params = {}
    if (type) params.type = type
    if (id) params.id = id
    return api.post('/files/upload', formData, {
      params,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export default api
