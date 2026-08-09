import { useState, useEffect } from 'react'
import { Star, X } from 'lucide-react'
import { promptAPI, reviewAPI } from '../services/api'
import './ReviewSection.css'

const getRatingValue = (rating) => Math.max(0, Math.min(5, Number(rating) || 0))

const Stars = ({ rating, size = 16 }) => (
  <span className="review-stars" aria-label={`${getRatingValue(rating)} out of 5 stars`}>
    {Array.from({ length: getRatingValue(rating) }, (_, index) => (
      <Star key={index} size={size} fill="currentColor" />
    ))}
  </span>
)

function ReviewSection({ prompt, onClose }) {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [promptMissing, setPromptMissing] = useState(false)
  const [showReviewForm, setShowReviewForm] = useState(false)
  const [formData, setFormData] = useState({
    rating: 5,
    comment: '',
    reviewerName: ''
  })

  useEffect(() => {
    fetchReviews()
  }, [prompt.id])

  const verifyPromptExists = async () => {
    const response = await promptAPI.promptExists(prompt.id)
    return Boolean(response.data?.exists)
  }

  const fetchReviews = async () => {
    try {
      setLoading(true)
      setError(null)

      const exists = await verifyPromptExists()
      if (!exists) {
        setPromptMissing(true)
        setReviews([])
        setShowReviewForm(false)
        setError('This prompt no longer exists. Refresh the prompt list and try again.')
        return
      }

      setPromptMissing(false)
      const response = await reviewAPI.getReviewsByPromptId(prompt.id)
      setReviews(Array.isArray(response.data) ? response.data : [])
    } catch (error) {
      setError(error.response?.data?.message || 'Failed to load reviews.')
      console.error('Failed to load reviews:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmitReview = async (e) => {
    e.preventDefault()

    try {
      setError(null)
      const exists = await verifyPromptExists()
      if (!exists) {
        setPromptMissing(true)
        setShowReviewForm(false)
        setError('This prompt no longer exists. Refresh the prompt list and try again.')
        return
      }

      await reviewAPI.createReview({
        promptId: String(prompt.id),
        ...formData
      })
      fetchReviews()
      setFormData({
        rating: 5,
        comment: '',
        reviewerName: ''
      })
      setShowReviewForm(false)
    } catch (error) {
      setError(error.response?.data?.message || 'Failed to create review.')
      console.error('Failed to create review:', error)
    }
  }

  const handleDeleteReview = async (reviewId) => {
    if (confirm('Delete this review?')) {
      try {
        setError(null)
        await reviewAPI.deleteReview(reviewId)
        fetchReviews()
      } catch (error) {
        setError(error.response?.data?.message || 'Failed to delete review.')
        console.error('Failed to delete review:', error)
      }
    }
  }

  const averageRating = reviews.length > 0
    ? (reviews.reduce((sum, r) => sum + getRatingValue(r.rating), 0) / reviews.length).toFixed(1)
    : '0.0'

  return (
    <div className="review-modal-overlay" onClick={onClose}>
      <div className="review-modal" onClick={e => e.stopPropagation()}>
        <div className="review-header">
          <div>
            <h2>Reviews for "{prompt.title}"</h2>
            <div className="rating-summary">
              <span className="avg-rating">
                <Star size={18} fill="currentColor" />
                {averageRating}
              </span>
              <span className="review-count">({reviews.length} reviews)</span>
            </div>
          </div>
          <button className="close-btn" onClick={onClose} aria-label="Close reviews">
            <X size={24} />
          </button>
        </div>

        <div className="reviews-content">
          {error && (
            <div className="review-alert">
              {error}
            </div>
          )}

          {!showReviewForm ? (
            <button
              className="btn btn-primary"
              onClick={() => setShowReviewForm(true)}
              style={{ marginBottom: '1.5rem' }}
              disabled={promptMissing}
            >
              + Add Review
            </button>
          ) : (
            <form onSubmit={handleSubmitReview} className="review-form">
              <div className="form-group">
                <label>Your Name *</label>
                <input
                  type="text"
                  value={formData.reviewerName}
                  onChange={(e) => setFormData({ ...formData, reviewerName: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label>Rating *</label>
                <div className="star-rating">
                  {[1, 2, 3, 4, 5].map(star => (
                    <button
                      key={star}
                      type="button"
                      className={`star ${star <= formData.rating ? 'active' : ''}`}
                      onClick={() => setFormData({ ...formData, rating: star })}
                      aria-label={`Set rating to ${star}`}
                    >
                      <Star size={30} fill="currentColor" />
                    </button>
                  ))}
                </div>
              </div>

              <div className="form-group">
                <label>Comment *</label>
                <textarea
                  value={formData.comment}
                  onChange={(e) => setFormData({ ...formData, comment: e.target.value })}
                  placeholder="Share your thoughts..."
                  required
                  rows="3"
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="btn btn-primary">Submit Review</button>
                <button type="button" onClick={() => setShowReviewForm(false)} className="btn btn-secondary">
                  Cancel
                </button>
              </div>
            </form>
          )}

          {loading ? (
            <p>Loading reviews...</p>
          ) : reviews.length === 0 ? (
            <p className="no-reviews">
              {promptMissing ? 'Prompt unavailable.' : 'No reviews yet. Be the first to review!'}
            </p>
          ) : (
            <div className="reviews-list">
              {reviews.map(review => (
                <div key={review.id} className="review-item">
                  <div className="review-header-item">
                    <div>
                      <h4>{review.reviewerName}</h4>
                      <Stars rating={review.rating} />
                    </div>
                    <button
                      className="btn-delete"
                      onClick={() => handleDeleteReview(review.id)}
                      aria-label="Delete review"
                    >
                      <X size={16} />
                    </button>
                  </div>
                  <p className="review-comment">{review.comment}</p>
                  <small className="review-date">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </small>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default ReviewSection
