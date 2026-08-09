import axios from 'axios'

const TOKEN_KEY = 'prompt-manager-jwt'

const apiHeaders = {
  'ngrok-skip-browser-warning': 'true'
}

const encodeId = (id) => encodeURIComponent(String(id))

export const getAuthToken = () => localStorage.getItem(TOKEN_KEY)

export const setAuthToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token)
}

export const clearAuthToken = () => {
  localStorage.removeItem(TOKEN_KEY)
}

const attachAuthToken = (config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
}

const promptApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: apiHeaders,
})

const reviewApi = axios.create({
  baseURL: import.meta.env.VITE_REVIEW_API_URL || '',
  headers: apiHeaders,
})

promptApi.interceptors.request.use(attachAuthToken)
reviewApi.interceptors.request.use(attachAuthToken)

export const authAPI = {
  login: (credentials) => promptApi.post('/api/auth/login', credentials),
  logout: () => clearAuthToken(),
}

export const promptAPI = {
  createPrompt: (data) => promptApi.post('/api/prompts', data),
  getAllPrompts: (params = {}) => promptApi.get('/api/prompts', { params }),
  getPromptById: (id) => promptApi.get(`/api/prompts/${encodeId(id)}`),
  promptExists: (id) => promptApi.get('/api/prompts/exit', { params: { id } }),
  updatePrompt: (id, data) => promptApi.put(`/api/prompts/${encodeId(id)}`, data),
  deletePrompt: (id) => promptApi.delete(`/api/prompts/${encodeId(id)}`),
}

export const reviewAPI = {
  createReview: (data) => reviewApi.post('/api/reviews', data),
  getAllReviews: (params = {}) => reviewApi.get('/api/reviews', { params }),
  getReviewById: (id) => reviewApi.get(`/api/reviews/${encodeId(id)}`),
  getReviewsByPromptId: (promptId) => reviewApi.get(`/api/reviews/prompt/${encodeId(promptId)}`),
  updateReview: (id, data) => reviewApi.put(`/api/reviews/${encodeId(id)}`, data),
  deleteReview: (id) => reviewApi.delete(`/api/reviews/${encodeId(id)}`),
}
