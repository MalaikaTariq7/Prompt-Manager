function PromptList({ prompts, onEdit, onDelete, onViewReviews }) {
  return (
    <div className="prompts-list">
      {prompts.map(prompt => (
        <div key={prompt.id} className="prompt-card">
          <div className="prompt-card-header">
            <h3>{prompt.title}</h3>
            <span className="category-badge">{prompt.category}</span>
          </div>
          <p className="prompt-description">{prompt.description}</p>
          <div className="prompt-footer">
            <button onClick={() => onViewReviews(prompt)}>View Reviews</button>
            <button onClick={() => onEdit(prompt)}>Edit</button>
            <button onClick={() => onDelete(prompt.id)}>Delete</button>
          </div>
        </div>
      ))}
    </div>
  )
}

export default PromptList
