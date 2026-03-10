import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from 'react';
const OWNER_REPO_PATTERN = /^[^/]+\/[^/]+$/;
function RepoForm({ onSubmit, loading }) {
    const [ownerRepo, setOwnerRepo] = useState('');
    const [token, setToken] = useState('');
    const [days, setDays] = useState(30);
    const [error, setError] = useState(null);
    function handleSubmit(e) {
        e.preventDefault();
        if (!OWNER_REPO_PATTERN.test(ownerRepo.trim())) {
            setError('Format must be owner/repo');
            return;
        }
        setError(null);
        const [owner, repo] = ownerRepo.trim().split('/');
        onSubmit(owner, repo, token, days);
    }
    return (_jsxs("div", { style: { maxWidth: 480, margin: '4rem auto', padding: '2rem', background: 'white', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.12)' }, children: [_jsx("h1", { style: { marginBottom: '1.5rem', fontSize: '1.25rem' }, children: "DORA Metrics Dashboard" }), _jsxs("form", { onSubmit: handleSubmit, noValidate: true, children: [_jsxs("div", { style: { marginBottom: '1rem' }, children: [_jsx("label", { htmlFor: "ownerRepo", style: { display: 'block', marginBottom: 4, fontWeight: 500 }, children: "Owner/Repo" }), _jsx("input", { id: "ownerRepo", type: "text", placeholder: "liatrio/liatrio", value: ownerRepo, onChange: e => setOwnerRepo(e.target.value), style: { width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: 6, boxSizing: 'border-box' } }), error && (_jsx("p", { role: "alert", style: { color: '#ef4444', fontSize: '0.875rem', marginTop: 4 }, children: error }))] }), _jsxs("div", { style: { marginBottom: '1rem' }, children: [_jsx("label", { htmlFor: "token", style: { display: 'block', marginBottom: 4, fontWeight: 500 }, children: "Personal Access Token" }), _jsx("input", { id: "token", type: "password", placeholder: "ghp_\u2026", value: token, onChange: e => setToken(e.target.value), style: { width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: 6, boxSizing: 'border-box' } })] }), _jsxs("fieldset", { style: { border: 'none', padding: 0, marginBottom: '1.5rem' }, children: [_jsx("legend", { style: { fontWeight: 500, marginBottom: 8 }, children: "Time Window" }), _jsx("div", { style: { display: 'flex', gap: '1rem' }, children: [30, 90, 180].map(d => (_jsxs("label", { style: { display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer' }, children: [_jsx("input", { type: "radio", name: "days", value: d, checked: days === d, onChange: () => setDays(d) }), d, " days"] }, d))) })] }), _jsx("button", { type: "submit", disabled: loading, style: {
                            width: '100%',
                            padding: '0.625rem',
                            background: loading ? '#9ca3af' : '#3b82f6',
                            color: 'white',
                            border: 'none',
                            borderRadius: 6,
                            fontWeight: 600,
                            cursor: loading ? 'not-allowed' : 'pointer',
                        }, children: loading ? 'Loading…' : 'Load Metrics' })] })] }));
}
export default RepoForm;
