import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [obligations, setObligations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Fetch compliance obligations from backend
    fetch('/api/obligations/all')
      .then(response => response.json())
      .then(data => {
        setObligations(data);
        setLoading(false);
      })
      .catch(error => {
        console.error('Error fetching obligations:', error);
        setLoading(false);
      });
  }, []);

  return (
    <div className="App">
      <header className="App-header">
        <h1>Compliance Obligation Register</h1>
        <p>Manage and track your compliance obligations</p>
      </header>

      <main className="App-main">
        {loading ? (
          <p>Loading obligations...</p>
        ) : (
          <div className="obligations-list">
            <h2>Obligations ({obligations.length})</h2>
            {obligations.length === 0 ? (
              <p>No obligations found.</p>
            ) : (
              <ul>
                {obligations.map(obligation => (
                  <li key={obligation.id} className="obligation-item">
                    <h3>{obligation.title}</h3>
                    <p>{obligation.description}</p>
                    <span className={`status ${obligation.status.toLowerCase()}`}>
                      {obligation.status}
                    </span>
                    <span className="due-date">
                      Due: {new Date(obligation.dueDate).toLocaleDateString()}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;