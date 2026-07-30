import { useState, useEffect, useRef } from 'react'
import { Plus, Edit2, Trash2, Search, Star, LogIn, LogOut } from 'lucide-react'
import './App.css'
import PromptForm from './components/PromptForm'
import ReviewSection from './components/ReviewSection'
import { authAPI, clearAuthToken, getAuthToken, promptAPI, setAuthToken } from './services/api'

const defaultLoginData = {
  username: import.meta.env.VITE_LOGIN_USERNAME || 'admin',
  password: import.meta.env.VITE_LOGIN_PASSWORD || ''
}

const getPromptItems = (data) => {
  if (Array.isArray(data)) {
    return data
  }

  if (Array.isArray(data?.content)) {
    return data.content
  }

  return []
}

function App() {
  const [prompts, setPrompts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [showReviewPrompts, setShowReviewPrompts] = useState(false)
  const [editingPrompt, setEditingPrompt] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedPrompt, setSelectedPrompt] = useState(null)
  const [token, setToken] = useState(() => getAuthToken())
  const [loginData, setLoginData] = useState(defaultLoginData)
  const [loginLoading, setLoginLoading] = useState(false)
  const promptFormRef = useRef(null)

  useEffect(() => {
    if (token) {
      fetchPrompts()
    }
  }, [token])

  useEffect(() => {
    if (showForm && editingPrompt && promptFormRef.current) {
      promptFormRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [showForm, editingPrompt])

  const handleAuthError = (err, fallbackMessage) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      clearAuthToken()
      setToken(null)
      setPrompts([])
      setSelectedPrompt(null)
      setShowForm(false)
      setShowReviewPrompts(false)
      setEditingPrompt(null)
      setError('Session expired. Please login again.')
      setLoginData(defaultLoginData)
      return
    }

    setError(fallbackMessage)
  }

  const fetchPrompts = async () => {
    try {
      setLoading(true)
      const response = await promptAPI.getAllPrompts({
        page: 0,
        size: 100,
        sortBy: 'createdAt',
        direction: 'desc'
      })
      setPrompts(getPromptItems(response.data))
      setError(null)
    } catch (err) {
      handleAuthError(err, 'Failed to load prompts')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleLogin = async (event) => {
    event.preventDefault()

    try {
      setLoginLoading(true)
      const response = await authAPI.login(loginData)
      setAuthToken(response.data.token)
      setToken(response.data.token)
      setLoginData(defaultLoginData)
      setError(null)
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid username or password')
      console.error(err)
    } finally {
      setLoginLoading(false)
    }
  }

  const handleLogout = () => {
    clearAuthToken()
    setToken(null)
    setPrompts([])
    setSelectedPrompt(null)
    setShowForm(false)
    setShowReviewPrompts(false)
    setEditingPrompt(null)
    setError(null)
    setLoginData(defaultLoginData)
  }

  const handleCreatePrompt = async (formData) => {
    try {
      await promptAPI.createPrompt(formData)
      fetchPrompts()
      setShowForm(false)
      setError(null)
    } catch (err) {
      handleAuthError(err, 'Failed to create prompt')
      console.error(err)
    }
  }

  const handleUpdatePrompt = async (id, formData) => {
    try {
      await promptAPI.updatePrompt(id, formData)
      fetchPrompts()
      setEditingPrompt(null)
      setShowForm(false)
      setError(null)
    } catch (err) {
      handleAuthError(err, 'Failed to update prompt')
      console.error(err)
    }
  }

  const handleDeletePrompt = async (id) => {
    if (confirm('Are you sure you want to delete this prompt?')) {
      try {
        await promptAPI.deletePrompt(id)
        fetchPrompts()
        setError(null)
      } catch (err) {
        handleAuthError(err, 'Failed to delete prompt')
        console.error(err)
      }
    }
  }

  const handleShowCreatePrompt = () => {
    setShowForm(!showForm)
    setShowReviewPrompts(false)
    setEditingPrompt(null)
    setSelectedPrompt(null)
  }

  const handleShowReviewPrompts = () => {
    setShowReviewPrompts(!showReviewPrompts)
    setShowForm(false)
    setEditingPrompt(null)
    setSelectedPrompt(null)
  }

  const handleEditPromptClick = (prompt) => {
    setEditingPrompt(prompt)
    setShowForm(true)
    setShowReviewPrompts(false)
    setSelectedPrompt(null)
  }

  const filteredPrompts = prompts.filter(prompt =>
    prompt.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    prompt.category.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <div className="app">
      <header className="header">
        <div className="container">
          <div className="header-content">
            <div>
              <h1 className="header-title">Prompt Manager</h1>
              <p className="header-subtitle">Create, manage and review AI prompts</p>
            </div>
            {token && (
              <div className="header-actions">
                <button
                  className="btn btn-secondary"
                  onClick={handleShowReviewPrompts}
                >
                  <Star size={20} />
                  {showReviewPrompts ? 'Hide Reviews' : 'Review Prompt'}
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleShowCreatePrompt}
                >
                  <Plus size={20} />
                  {showForm ? 'Cancel' : 'New Prompt'}
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={handleLogout}
                >
                  <LogOut size={20} />
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="main-content">
        <div className="container">
          {error && (
            <div className="alert alert-error">
              {error}
            </div>
          )}

          {!token ? (
            <section className="login-panel">
              <form onSubmit={handleLogin} className="login-form">
                <div className="login-heading">
                  <LogIn size={28} />
                  <div>
                    <h2>Login</h2>
                    <p>Use your Prompt Manager credentials to continue.</p>
                  </div>
                </div>

                <div className="form-group">
                  <label>Username</label>
                  <input
                    type="text"
                    value={loginData.username}
                    onChange={(e) => setLoginData({ ...loginData, username: e.target.value })}
                    placeholder="admin"
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Password</label>
                  <input
                    type="password"
                    value={loginData.password}
                    onChange={(e) => setLoginData({ ...loginData, password: e.target.value })}
                    placeholder="Enter password"
                    required
                  />
                </div>

                <button type="submit" className="btn btn-primary login-submit" disabled={loginLoading}>
                  <LogIn size={18} />
                  {loginLoading ? 'Logging in...' : 'Login'}
                </button>
              </form>
            </section>
          ) : (
            <>
              {showForm && (
                <div ref={promptFormRef}>
                  <PromptForm
                    onSubmit={editingPrompt ?
                      (data) => handleUpdatePrompt(editingPrompt.id, data) :
                      handleCreatePrompt
                    }
                    initialData={editingPrompt}
                    onCancel={() => {
                      setShowForm(false)
                      setEditingPrompt(null)
                    }}
                  />
                </div>
              )}

              {showReviewPrompts && (
                <section className="review-prompts-panel">
                  <div className="review-prompts-header">
                    <div>
                      <h2>Review a Prompt</h2>
                      <p>Select any prompt below to read reviews or add your own.</p>
                    </div>
                    <span className="result-count">{prompts.length} prompts available</span>
                  </div>

                  {loading ? (
                    <div className="loading">Loading prompts...</div>
                  ) : prompts.length === 0 ? (
                    <div className="no-data">
                      <p>No prompts available to review yet.</p>
                    </div>
                  ) : (
                    <div className="review-prompts-list">
                      {prompts.map(prompt => (
                        <div key={prompt.id} className="review-prompt-row">
                          <div>
                            <div className="prompt-header">
                              <h3>{prompt.title}</h3>
                              <span className="badge badge-primary">{prompt.category}</span>
                            </div>
                            <p className="prompt-description">{prompt.description}</p>
                          </div>
                          <button
                            className="btn btn-primary"
                            onClick={() => setSelectedPrompt(prompt)}
                          >
                            <Star size={16} />
                            Review
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              )}

              <div className="search-section">
                <div className="search-input">
                  <Search size={20} />
                  <input
                    type="text"
                    placeholder="Search prompts by title or category..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <span className="result-count">{filteredPrompts.length} prompts found</span>
              </div>

              {loading ? (
                <div className="loading">Loading...</div>
              ) : filteredPrompts.length === 0 ? (
                <div className="no-data">
                  <p>No prompts found. Create your first prompt to get started!</p>
                </div>
              ) : (
                <div className="prompts-grid">
                  {filteredPrompts.map(prompt => (
                    <div key={prompt.id} className="prompt-item">
                      <div className="prompt-header">
                        <h3>{prompt.title}</h3>
                        <span className="badge badge-primary">{prompt.category}</span>
                      </div>
                      <p className="prompt-description">{prompt.description}</p>
                      <p className="prompt-text">{prompt.promptText}</p>
                      <div className="prompt-date">
                        Created: {new Date(prompt.createdAt).toLocaleDateString()}
                      </div>
                      <div className="prompt-actions">
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => handleEditPromptClick(prompt)}
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDeletePrompt(prompt.id)}
                        >
                          <Trash2 size={16} />
                        </button>
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => setSelectedPrompt(prompt)}
                        >
                          View Reviews
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {selectedPrompt && (
                <ReviewSection
                  prompt={selectedPrompt}
                  onClose={() => setSelectedPrompt(null)}
                />
              )}
            </>
          )}
        </div>
      </main>

      <footer className="footer">
        <p>&copy; 2024 Prompt Manager System. All rights reserved.</p>
      </footer>
    </div>
  )
}

export default App
