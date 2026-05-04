import React, { useState, useEffect } from 'react';
import './App.css';

const API_BASE = '/api';

// ── Auth helpers ──────────────────────────────────────────────────────────────

function getToken() {
  return localStorage.getItem('jwt_token');
}

function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// ── Login screen ──────────────────────────────────────────────────────────────

function LoginForm({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ username, password }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || 'Login failed');
      }
      const data = await res.json();
      localStorage.setItem('jwt_token', data.token);
      onLogin(data.token);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <h2>Compliance Register — Login</h2>
      <form onSubmit={handleSubmit} className="login-form">
        <label>
          Username
          <input
            type="text"
            value={username}
            onChange={e => setUsername(e.target.value)}
            required
            autoFocus
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p className="hint">Default credentials: admin / manager / viewer — password: <code>password</code></p>
    </div>
  );
}

// ── Stats bar ─────────────────────────────────────────────────────────────────

function StatsBar({ stats }) {
  if (!stats) return null;
  return (
    <div className="stats-bar">
      <span>Total: <strong>{stats.totalObligations}</strong></span>
      <span>Pending: <strong>{stats.pendingObligations}</strong></span>
      <span>Completed: <strong>{stats.completedObligations}</strong></span>
      <span className="overdue">Overdue: <strong>{stats.overdueObligations}</strong></span>
      <span>Due soon: <strong>{stats.dueSoonObligations}</strong></span>
    </div>
  );
}

// ── Obligation list ───────────────────────────────────────────────────────────

function ObligationList({ obligations, totalElements, page, pageSize, onPageChange }) {
  const totalPages = Math.ceil(totalElements / pageSize);

  return (
    <div className="obligations-section">
      <div className="list-header">
        <h2>Obligations <span className="count">({totalElements})</span></h2>
        <a
          href={`${API_BASE}/obligations/export`}
          className="export-btn"
          target="_blank"
          rel="noreferrer"
        >
          Export CSV
        </a>
      </div>

      {obligations.length === 0 ? (
        <p className="empty">No obligations found.</p>
      ) : (
        <table className="obligations-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Category</th>
              <th>Status</th>
              <th>Due Date</th>
              <th>Assigned To</th>
            </tr>
          </thead>
          <tbody>
            {obligations.map(o => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.title}</td>
                <td>{o.category}</td>
                <td>
                  <span className={`badge status-${(o.status || '').toLowerCase()}`}>
                    {o.status}
                  </span>
                </td>
                <td>{o.dueDate ? new Date(o.dueDate).toLocaleDateString() : '—'}</td>
                <td>{o.assignedEmail || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => onPageChange(page - 1)} disabled={page === 0}>
            ← Prev
          </button>
          <span>Page {page + 1} of {totalPages}</span>
          <button onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}>
            Next →
          </button>
        </div>
      )}
    </div>
  );
}

// ── Main App ──────────────────────────────────────────────────────────────────

function App() {
  const [token, setToken]             = useState(getToken());
  const [obligations, setObligations] = useState([]);
  const [stats, setStats]             = useState(null);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState('');
  const [page, setPage]               = useState(0);
  const [totalElements, setTotal]     = useState(0);
  const PAGE_SIZE = 10;

  const fetchData = async (currentPage) => {
    setLoading(true);
    setError('');
    try {
      const headers = authHeaders();

      const [listRes, statsRes] = await Promise.all([
        fetch(
          `${API_BASE}/obligations/all-dto?page=${currentPage}&size=${PAGE_SIZE}&sortBy=dueDate&sortDir=asc`,
          { headers }
        ),
        fetch(`${API_BASE}/obligations/stats`, { headers }),
      ]);

      if (listRes.status === 401 || listRes.status === 403) {
        handleLogout();
        return;
      }

      if (!listRes.ok) throw new Error('Failed to load obligations');
      if (!statsRes.ok) throw new Error('Failed to load stats');

      const listData  = await listRes.json();
      const statsData = await statsRes.json();

      setObligations(listData.content || []);
      setTotal(listData.totalElements || 0);
      setStats(statsData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) fetchData(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, page]);

  const handleLogin = (newToken) => {
    setToken(newToken);
    setPage(0);
  };

  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    setToken(null);
    setObligations([]);
    setStats(null);
  };

  if (!token) {
    return <LoginForm onLogin={handleLogin} />;
  }

  return (
    <div className="App">
      <header className="App-header">
        <div className="header-content">
          <div>
            <h1>Compliance Obligation Register</h1>
            <p>Manage and track your compliance obligations</p>
          </div>
          <button className="logout-btn" onClick={handleLogout}>Sign out</button>
        </div>
      </header>

      <main className="App-main">
        {error && <div className="error-banner">{error}</div>}

        <StatsBar stats={stats} />

        {loading ? (
          <p className="loading">Loading obligations…</p>
        ) : (
          <ObligationList
            obligations={obligations}
            totalElements={totalElements}
            page={page}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
          />
        )}
      </main>
    </div>
  );
}

export default App;
