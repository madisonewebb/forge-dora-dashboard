import { jsxs as _jsxs, jsx as _jsx } from "react/jsx-runtime";
import { useState, useEffect } from 'react';
import RepoForm from './components/RepoForm';
function App() {
    const [view, setView] = useState('form');
    const [params, setParams] = useState(null);
    const [_data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [fetchError, setFetchError] = useState(null);
    // Pre-fill form from URL query params on load
    useEffect(() => {
        const sp = new URLSearchParams(window.location.search);
        const owner = sp.get('owner');
        const repo = sp.get('repo');
        const days = parseInt(sp.get('days') ?? '30');
        if (owner && repo) {
            setParams({ owner, repo, token: '', days });
        }
    }, []);
    async function handleFormSubmit(owner, repo, token, days) {
        setLoading(true);
        setFetchError(null);
        // Update URL (no token)
        const sp = new URLSearchParams({ owner, repo, days: String(days) });
        window.history.pushState({}, '', `?${sp.toString()}`);
        try {
            const res = await fetch(`/api/metrics?owner=${owner}&repo=${repo}&token=${token}&days=${days}`);
            if (!res.ok) {
                const body = await res.json().catch(() => ({}));
                setFetchError(body.error ?? `Request failed: HTTP ${res.status}`);
                setLoading(false);
                return;
            }
            const json = await res.json();
            setData(json);
            setParams({ owner, repo, token, days: days });
            setView('dashboard');
        }
        catch {
            setFetchError('Could not reach the server. Check your connection.');
        }
        finally {
            setLoading(false);
        }
    }
    if (view === 'dashboard' && params) {
        return (_jsxs("div", { style: { padding: '2rem' }, children: [_jsxs("p", { children: ["Dashboard \u2014 ", params.owner, "/", params.repo, " (", params.days, " days)"] }), _jsx("button", { onClick: () => setView('form'), children: "Change Repository" })] }));
    }
    return (_jsxs("div", { style: { background: '#f9fafb', minHeight: '100vh' }, children: [fetchError && (_jsx("div", { role: "alert", style: { background: '#fef2f2', border: '1px solid #ef4444', padding: '1rem', margin: '1rem' }, children: fetchError })), _jsx(RepoForm, { onSubmit: handleFormSubmit, loading: loading })] }));
}
export default App;
