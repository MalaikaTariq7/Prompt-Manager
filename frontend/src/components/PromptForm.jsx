import { useEffect, useState } from 'react'
import './PromptForm.css'

const emptyPrompt = {
  title: '',
  description: '',
  promptText: '',
  category: ''
}

function PromptForm({ onSubmit, initialData, onCancel }) {
  const [formData, setFormData] = useState(initialData || emptyPrompt)

  useEffect(() => {
    setFormData(initialData || emptyPrompt)
  }, [initialData])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    onSubmit(formData)
    if (!initialData) {
      setFormData(emptyPrompt)
    }
  }

  return (
    <div className="form-container">
      <form onSubmit={handleSubmit} className="prompt-form">
        <h2>{initialData ? 'Edit Prompt' : 'Create New Prompt'}</h2>
        
        <div className="form-group">
          <label>Title *</label>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            placeholder="Enter prompt title"
            required
          />
        </div>

        <div className="form-group">
          <label>Category *</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleChange}
            required
          >
            <option value="">Select a category</option>
            <option value="Programming">Programming</option>
            <option value="Writing">Writing</option>
            <option value="Marketing">Marketing</option>
            <option value="Education">Education</option>
            <option value="Business">Business</option>
            <option value="Creative">Creative</option>
            <option value="Other">Other</option>
          </select>
        </div>

        <div className="form-group">
          <label>Description *</label>
          <input
            type="text"
            name="description"
            value={formData.description}
            onChange={handleChange}
            placeholder="Brief description"
            required
          />
        </div>

        <div className="form-group">
          <label>Prompt Text *</label>
          <textarea
            name="promptText"
            value={formData.promptText}
            onChange={handleChange}
            placeholder="Enter the full prompt text here..."
            rows="6"
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary">
            {initialData ? 'Update Prompt' : 'Create Prompt'}
          </button>
          <button type="button" onClick={onCancel} className="btn btn-secondary">
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

export default PromptForm
