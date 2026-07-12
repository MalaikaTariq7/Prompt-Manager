import { useState, useEffect, useRef } from 'react'
import { Plus, Edit2, Trash2, Search, Star } from 'lucide-react'
import './App.css'
import PromptForm from './components/PromptForm'
import ReviewSection from './components/ReviewSection'
import { promptAPI } from './services/api'

function App() {
  const [prompts, setPrompts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [showReviewPrompts, setShowReviewPrompts] = useState(false)
  const [editingPrompt, setEditingPrompt] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedPrompt, setSelectedPrompt] = useState(null)
  const promptFormRef = useRef(null)

  useEffect(() => {
    fetchPrompts()
  }, [])

  useEffect(() => {
    if (showForm && editingPrompt && promptFormRef.current) {
      promptFormRef.current.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [showForm, editingPrompt])

  const fetchPrompts = async () => {
    try {
      setLoading(true)
      const response = await promptAPI.getAllPrompts()
      setPrompts(response.data)
      setError(null)
    } catch (err) {
      setError('Failed to load prompts')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleCreatePrompt = async (formData) => {
    try {
      await promptAPI.createPrompt(formData)
      fetchPrompts()
      setShowForm(false)
      setError(null)
    } catch (err) {
      setError('Failed to create prompt')
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
      setError('Failed to update prompt')
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
        setError('Failed to delete prompt')
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
            </div>
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
        </div>
      </main>

      <footer className="footer">
        <p>&copy; 2024 Prompt Manager System. All rights reserved.</p>
      </footer>
    </div>
  )
}

export default App
