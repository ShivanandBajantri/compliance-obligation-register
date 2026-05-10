import React, { useState, useEffect, useCallback } from 'react';
import './App.css';

const API_BASE = '/api';

// ── Helpers ───────────────────────────────────────────────────────────────────

function getToken()   { return localStorage.getItem('jwt_token'); }
function getUsername(){ return localStorage.getItem('username') || ''; }

function authHeaders() {
  const t = getToken();
  return t ? { Authorization: `Bearer ${t}` } : {};
}

async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...authHeaders(), ...(options.headers || {}) },
  });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw Object.assign(new Error(body.error || `HTTP ${res.status}`), { status: res.status });
  return body;
}

// ── Login ─────────────────────────────────────────────────────────────────────

function LoginForm({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const data = await apiFetch('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: username.trim(), password }),
      });
      localStorage.setItem('jwt_token', data.token);
      localStorage.setItem('username', username.trim());
      onLogin(data.token);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">Compliance Register</h1>
        <p className="login-sub">Sign in to manage obligations</p>
        <form onSubmit={submit} className="login-form">
          <label>Username
            <input type="text" value={username} onChange={e => setUsername(e.target.value)} required autoFocus />
          </label>
          <label>Password
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required />
          </label>
          {error && <p className="form-error">{error}</p>}
          <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="login-hint">
          Demo accounts — password: <code>password</code><br />
          <strong>admin</strong> · <strong>manager</strong> · <strong>viewer</strong>
        </p>
      </div>
    </div>
  );
}

// ── Obligation Form Modal ─────────────────────────────────────────────────────

const EMPTY_FORM = { title: '', description: '', category: '', dueDate: '', assignedEmail: '', status: 'PENDING' };
const STATUSES   = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
const CATEGORIES = ['Legal', 'Finance', 'HR', 'IT', 'Operations', 'Compliance', 'Other'];

function ObligationModal({ obligation, onSave, onClose }) {
  const isEdit = !!obligation?.id;
  const [form, setForm]     = useState(obligation ? {
    title:         obligation.title         || '',
    description:   obligation.description   || '',
    category:      obligation.category      || '',
    dueDate:       obligation.dueDate       || '',
    assignedEmail: obligation.assignedEmail || '',
    status:        obligation.status        || 'PENDING',
  } : EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError]   = useState('');

  useEffect(() => {
    setForm(obligation ? {
      title:         obligation.title         || '',
      description:   obligation.description   || '',
      category:      obligation.category      || '',
      dueDate:       obligation.dueDate       || '',
      assignedEmail: obligation.assignedEmail || '',
      status:        obligation.status        || 'PENDING',
    } : EMPTY_FORM);
  }, [obligation]);

  const set = (field) => (e) => setForm(f => ({ ...f, [field]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (!form.title.trim())   { setError('Title is required'); return; }
    if (!form.dueDate)        { setError('Due date is required'); return; }
    setSaving(true); setError('');
    try {
      let body = { ...form, title: form.title.trim() };
      if (!isEdit) {
        // Ensure no id is sent for create
        const { id, ...bodyWithoutId } = body;
        body = bodyWithoutId;
      }
      if (isEdit) {
        await apiFetch(`/obligations/${obligation.id}`, { method: 'PUT', body: JSON.stringify(body) });
      } else {
        await apiFetch('/obligations', { method: 'POST', body: JSON.stringify(body) });
      }
      onSave();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-header">
          <h2>{isEdit ? 'Edit Obligation' : 'New Obligation'}</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={submit} className="modal-form">
          <label>Title *
            <input type="text" value={form.title} onChange={set('title')} required maxLength={255} />
          </label>
          <label>Description
            <textarea value={form.description} onChange={set('description')} rows={3} maxLength={2000} />
          </label>
          <div className="form-row">
            <label>Category
              <select value={form.category} onChange={set('category')}>
                <option value="">— select —</option>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </label>
            <label>Status
              <select value={form.status} onChange={set('status')}>
                {STATUSES.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
              </select>
            </label>
          </div>
          <div className="form-row">
            <label>Due Date *
              <input type="date" value={form.dueDate} onChange={set('dueDate')} required />
            </label>
            <label>Assigned Email
              <input type="email" value={form.assignedEmail} onChange={set('assignedEmail')} />
            </label>
          </div>
          {error && <p className="form-error">{error}</p>}
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Confirm Delete Dialog ─────────────────────────────────────────────────────

function ConfirmDialog({ title, onConfirm, onCancel }) {
  return (
    <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onCancel()}>
      <div className="modal modal-sm">
        <div className="modal-header">
          <h2>Delete Obligation</h2>
          <button className="modal-close" onClick={onCancel}>✕</button>
        </div>
        <p className="confirm-text">Delete <strong>"{title}"</strong>? This cannot be undone.</p>
        <div className="modal-actions">
          <button className="btn btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="btn btn-danger"    onClick={onConfirm}>Delete</button>
        </div>
      </div>
    </div>
  );
}

// ── Stats Bar ─────────────────────────────────────────────────────────────────

function StatsBar({ stats, activeFilter, onFilter }) {
  if (!stats) return null;

  const cards = [
    { key: 'all',         label: 'Total',     num: stats.totalObligations,     cls: '' },
    { key: 'PENDING',     label: 'Pending',   num: stats.pendingObligations,   cls: 'pending' },
    { key: 'COMPLETED',   label: 'Completed', num: stats.completedObligations, cls: 'completed' },
    { key: 'overdue',     label: 'Overdue',   num: stats.overdueObligations,   cls: 'overdue' },
    { key: 'due-soon',    label: 'Due Soon',  num: stats.dueSoonObligations,   cls: 'due-soon' },
  ];

  return (
    <div className="stats-bar">
      {cards.map(c => (
        <button
          key={c.key}
          className={`stat-card ${c.cls} ${activeFilter === c.key ? 'stat-active' : ''}`}
          onClick={() => onFilter(activeFilter === c.key ? null : c.key)}
          title={`Filter by ${c.label}`}
        >
          <span className="stat-num">{c.num}</span>
          <span className="stat-lbl">{c.label}</span>
        </button>
      ))}
    </div>
  );
}

// ── Main App ──────────────────────────────────────────────────────────────────

export default function App() {
  const [token, setToken]         = useState(getToken());
  const [role, setRole]           = useState('');          // ADMIN / MANAGER / VIEWER
  const [obligations, setObligs]  = useState([]);
  const [stats, setStats]         = useState(null);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');
  const [page, setPage]           = useState(0);
  const [total, setTotal]         = useState(0);
  const [keyword, setKeyword]     = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [statFilter, setStatFilter]   = useState(null); // null|'all'|'PENDING'|'COMPLETED'|'overdue'|'due-soon'
  const [modal, setModal]         = useState(null);   // null | 'create' | obligation-object
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [alertResult, setAlertResult]   = useState(null);  // null | result object
  const [alertSending, setAlertSending] = useState(false);
  const PAGE_SIZE = 10;

  // Decode role from JWT payload
  useEffect(() => {
    if (!token) return;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const roles   = (payload.roles || '').split(',');
      if (roles.includes('ROLE_ADMIN'))   setRole('ADMIN');
      else if (roles.includes('ROLE_MANAGER')) setRole('MANAGER');
      else setRole('VIEWER');
    } catch { setRole('VIEWER'); }
  }, [token]);

  const canWrite  = role === 'ADMIN' || role === 'MANAGER';
  const canDelete = role === 'ADMIN';

  const fetchData = useCallback(async (p, kw, sf) => {
    setLoading(true); setError('');
    try {
      let listUrl;
      if (sf === 'PENDING' || sf === 'COMPLETED' || sf === 'IN_PROGRESS' || sf === 'CANCELLED') {
        // Filter by exact status
        listUrl = `/obligations/all-dto?page=${p}&size=${PAGE_SIZE}&sortBy=dueDate&sortDir=asc`;
      } else if (sf === 'overdue') {
        // Overdue = past due date, not completed — use search with status filter via keyword fallback
        listUrl = `/obligations/all-dto?page=${p}&size=${PAGE_SIZE}&sortBy=dueDate&sortDir=asc`;
      } else if (sf === 'due-soon') {
        listUrl = `/obligations/all-dto?page=${p}&size=${PAGE_SIZE}&sortBy=dueDate&sortDir=asc`;
      } else if (kw) {
        listUrl = `/obligations/search?keyword=${encodeURIComponent(kw)}&page=${p}&size=${PAGE_SIZE}&sortBy=dueDate&sortDir=asc`;
      } else {
        listUrl = `/obligations/all-dto?page=${p}&size=${PAGE_SIZE}&sortBy=dueDate&sortDir=asc`;
      }

      const [list, st] = await Promise.all([
        apiFetch(listUrl),
        apiFetch('/obligations/stats'),
      ]);

      let items = list.content || [];
      const today = new Date(); today.setHours(0,0,0,0);
      const soon  = new Date(today); soon.setDate(today.getDate() + 7);

      // Client-side filter for overdue / due-soon / status
      if (sf === 'overdue') {
        items = items.filter(o => {
          const d = new Date(o.dueDate + 'T00:00:00');
          return d < today && o.status !== 'COMPLETED';
        });
      } else if (sf === 'due-soon') {
        items = items.filter(o => {
          const d = new Date(o.dueDate + 'T00:00:00');
          return d >= today && d <= soon && o.status !== 'COMPLETED';
        });
      } else if (sf && sf !== 'all') {
        items = items.filter(o => o.status === sf);
      }

      setObligs(items);
      setTotal(sf && sf !== 'all' ? items.length : (list.totalElements || 0));
      setStats(st);
    } catch (err) {
      if (err.status === 401 || err.status === 403) logout();
      else setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (token) fetchData(page, keyword, statFilter);
  }, [token, page, keyword, statFilter, fetchData]);

  const refresh = () => fetchData(page, keyword, statFilter);

  const logout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('username');
    setToken(null); setObligs([]); setStats(null); setRole('');
  };

  const handleLogin = (t) => { setToken(t); setPage(0); };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    setStatFilter(null);
    setKeyword(searchInput.trim());
  };

  const handleStatFilter = (key) => {
    setPage(0);
    setKeyword('');
    setSearchInput('');
    setStatFilter(key);
  };

  const handleSaved = () => { setModal(null); refresh(); };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await apiFetch(`/obligations/${deleteTarget.id}`, { method: 'DELETE' });
      setDeleteTarget(null);
      refresh();
    } catch (err) {
      setError(err.message);
      setDeleteTarget(null);
    }
  };

  const handleExport = async () => {
    try {
      const res = await fetch(`${API_BASE}/obligations/export`, { headers: authHeaders() });
      if (!res.ok) throw new Error('Export failed');
      const blob = await res.blob();
      const url  = URL.createObjectURL(blob);
      const a    = document.createElement('a');
      a.href = url; a.download = 'obligations.csv';
      document.body.appendChild(a); a.click();
      document.body.removeChild(a); URL.revokeObjectURL(url);
    } catch (err) { setError(err.message); }
  };

  const handleSendAlerts = async () => {
    setAlertSending(true);
    setAlertResult(null);
    try {
      const result = await apiFetch('/obligations/send-alerts', { method: 'POST' });
      setAlertResult(result);
      setTimeout(() => setAlertResult(null), 6000);
    } catch (err) {
      setError(err.message);
    } finally {
      setAlertSending(false);
    }
  };

  if (!token) return <LoginForm onLogin={handleLogin} />;

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div className="App">
      {/* Header */}
      <header className="App-header">
        <div className="header-content">
          <div>
            <h1>Compliance Obligation Register</h1>
            <p>Manage and track your compliance obligations</p>
          </div>
          <div className="header-right">
            <span className="role-badge">{role}</span>
            <span className="username-label">{getUsername()}</span>
            <button className="btn btn-outline-white" onClick={logout}>Sign out</button>
          </div>
        </div>
      </header>

      <main className="App-main">
        {error && <div className="error-banner">{error} <button className="banner-close" onClick={() => setError('')}>✕</button></div>}

        {/* Alert result toast */}
        {alertResult && (
          <div className={`alert-banner ${alertResult.total === 0 ? 'alert-info' : 'alert-success'}`}>
            <span>
              {alertResult.total === 0
                ? '✓ No overdue or due-soon obligations — no alerts sent.'
                : `✓ ${alertResult.message}`}
            </span>
            <button className="banner-close" onClick={() => setAlertResult(null)}>✕</button>
          </div>
        )}

        {/* Stats */}
        <StatsBar stats={stats} activeFilter={statFilter} onFilter={handleStatFilter} />

        {/* Toolbar */}
        <div className="toolbar">
          <form onSubmit={handleSearch} className="search-form">
            <input
              type="text"
              placeholder="Search by title, category, description…"
              value={searchInput}
              onChange={e => setSearchInput(e.target.value)}
              className="search-input"
            />
            <button type="submit" className="btn btn-secondary">Search</button>
            {keyword && (
              <button type="button" className="btn btn-ghost" onClick={() => { setSearchInput(''); setKeyword(''); setPage(0); }}>
                Clear
              </button>
            )}
          </form>
          <div className="toolbar-actions">
            <button className="btn btn-secondary" onClick={handleExport}>⬇ Export CSV</button>
            {canWrite && (
              <button
                className="btn btn-alert"
                onClick={handleSendAlerts}
                disabled={alertSending}
                title="Send alert emails to all overdue and due-soon obligations"
              >
                {alertSending ? 'Sending…' : '🔔 Send Alerts'}
              </button>
            )}
            {canWrite && (
              <button className="btn btn-primary" onClick={() => setModal('create')}>
                + New Obligation
              </button>
            )}
          </div>
        </div>

        {/* Table */}
        <div className="table-card">
          <div className="table-header-row">
            <span className="table-count">
              {total} obligation{total !== 1 ? 's' : ''}
              {statFilter && statFilter !== 'all' && (
                <span className="filter-tag">
                  {statFilter === 'overdue' ? ' — Overdue'
                    : statFilter === 'due-soon' ? ' — Due Soon'
                    : ` — ${statFilter.replace('_', ' ')}`}
                  <button className="filter-clear" onClick={() => handleStatFilter(null)} title="Clear filter">✕</button>
                </span>
              )}
              {keyword && ` matching "${keyword}"`}
            </span>
          </div>

          {loading ? (
            <div className="loading-state">Loading…</div>
          ) : obligations.length === 0 ? (
            <div className="empty-state">
              <p>No obligations found.</p>
              {canWrite && !keyword && (
                <button className="btn btn-primary" onClick={() => setModal('create')}>
                  Create your first obligation
                </button>
              )}
            </div>
          ) : (
            <div className="table-wrap">
              <table className="obligations-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th>Due Date</th>
                    <th>Assigned To</th>
                    {(canWrite || canDelete) && <th>Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {obligations.map(o => (
                    <tr key={o.id}>
                      <td className="td-title">{o.title}</td>
                      <td>{o.category || '—'}</td>
                      <td>
                        <span className={`badge status-${(o.status || '').toLowerCase().replace('_', '-')}`}>
                          {(o.status || '').replace('_', ' ')}
                        </span>
                      </td>
                      <td>{o.dueDate ? new Date(o.dueDate + 'T00:00:00').toLocaleDateString() : '—'}</td>
                      <td className="td-email">{o.assignedEmail || '—'}</td>
                      {(canWrite || canDelete) && (
                        <td className="td-actions">
                          {canWrite && (
                            <button className="btn-icon btn-edit" title="Edit"
                              onClick={() => setModal(o)}>✏</button>
                          )}
                          {canDelete && (
                            <button className="btn-icon btn-delete" title="Delete"
                              onClick={() => setDeleteTarget(o)}>🗑</button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-secondary btn-sm" onClick={() => setPage(0)} disabled={page === 0}>«</button>
              <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => p - 1)} disabled={page === 0}>‹ Prev</button>
              <span className="page-info">Page {page + 1} of {totalPages}</span>
              <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Next ›</button>
              <button className="btn btn-secondary btn-sm" onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}>»</button>
            </div>
          )}
        </div>
      </main>

      {/* Create / Edit Modal */}
      {modal && (
        <ObligationModal
          obligation={modal === 'create' ? null : modal}
          onSave={handleSaved}
          onClose={() => setModal(null)}
        />
      )}

      {/* Delete Confirm */}
      {deleteTarget && (
        <ConfirmDialog
          title={deleteTarget.title}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}
