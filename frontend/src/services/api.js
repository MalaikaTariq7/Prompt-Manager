import axios from 'axios'

const apiHeaders = {
  'ngrok-skip-browser-warning': 'true'
}

const promptApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: apiHeaders,
})

const reviewApi = axios.create({
  baseURL: import.meta.env.VITE_REVIEW_API_URL || '',
  headers: apiHeaders,
})

export const promptAPI = {
  createPrompt: (data) => promptApi.post('/api/prompts', data),
  getAllPrompts: () => promptApi.get('/api/prompts'),
  getPromptById: (id) => promptApi.get(`/api/prompts/${id}`),
  updatePrompt: (id, data) => promptApi.put(`/api/prompts/${id}`, data),
  deletePrompt: (id) => promptApi.delete(`/api/prompts/${id}`),
}

export const reviewAPI = {
  createReview: (data) => reviewApi.post('/api/reviews', data),
  getAllReviews: () => reviewApi.get('/api/reviews'),
  getReviewById: (id) => reviewApi.get(`/api/reviews/${id}`),
  getReviewsByPromptId: (promptId) => reviewApi.get(`/api/reviews/prompt/${promptId}`),
  updateReview: (id, data) => reviewApi.put(`/api/reviews/${id}`, data),
  deleteReview: (id) => reviewApi.delete(`/api/reviews/${id}`),
}
