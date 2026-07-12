import { useState, useEffect } from 'react'
import { Star, X } from 'lucide-react'
import { reviewAPI } from '../services/api'
import './ReviewSection.css'

function ReviewSection({ prompt, onClose }) {
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(false)
  const [showReviewForm, setShowReviewForm] = useState(false)
  const [formData, setFormData] = useState({
    rating: 5,
    comment: '',
    reviewerName: ''
  })

  useEffect(() => {
    fetchReviews()
  }, [prompt])

  const fetchReviews = async () => {
    try {
      setLoading(true)
      const response = await reviewAPI.getReviewsByPromptId(prompt.id)
      setReviews(response.data)
    } catch (error) {
      console.error('Failed to load reviews:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmitReview = async (e) => {
    e.preventDefault()
    try {
      await reviewAPI.createReview({
        promptId: prompt.id,
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
      console.error('Failed to create review:', error)
    }
  }

  const handleDeleteReview = async (reviewId) => {
    if (confirm('Delete this review?')) {
      try {
        await reviewAPI.deleteReview(reviewId)
        fetchReviews()
      } catch (error) {
        console.error('Failed to delete review:', error)
      }
    }
  }

  const averageRating = reviews.length > 0 
    ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
    : 0

  return (
    <div className="review-modal-overlay" onClick={onClose}>
      <div className="review-modal" onClick={e => e.stopPropagation()}>
        <div className="review-header">
          <div>
            <h2>Reviews for "{prompt.title}"</h2>
            <div className="rating-summary">
              <span className="avg-rating">⭐ {averageRating}</span>
              <span className="review-count">({reviews.length} reviews)</span>
            </div>
          </div>
          <button className="close-btn" onClick={onClose}>
            <X size={24} />
          </button>
        </div>

        <div className="reviews-content">
          {!showReviewForm ? (
            <button 
              className="btn btn-primary"
              onClick={() => setShowReviewForm(true)}
              style={{ marginBottom: '1.5rem' }}
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
                  onChange={(e) => setFormData({...formData, reviewerName: e.target.value})}
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
                      onClick={() => setFormData({...formData, rating: star})}
                    >
                      ⭐
                    </button>
                  ))}
                </div>
              </div>

              <div className="form-group">
                <label>Comment *</label>
                <textarea
                  value={formData.comment}
                  onChange={(e) => setFormData({...formData, comment: e.target.value})}
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
            <p className="no-reviews">No reviews yet. Be the first to review!</p>
          ) : (
            <div className="reviews-list">
              {reviews.map(review => (
                <div key={review.id} className="review-item">
                  <div className="review-header-item">
                    <div>
                      <h4>{review.reviewerName}</h4>
                      <div className="review-stars">
                        {'⭐'.repeat(review.rating)}
                      </div>
                    </div>
                    <button 
                      className="btn-delete"
                      onClick={() => handleDeleteReview(review.id)}
                    >
                      ✕
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
