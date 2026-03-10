import { jsx as _jsx } from "react/jsx-runtime";
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RepoForm from './RepoForm';
describe('RepoForm', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });
    it('renders all three form fields', () => {
        render(_jsx(RepoForm, { onSubmit: vi.fn(), loading: false }));
        expect(screen.getByLabelText(/owner\/repo/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/personal access token/i)).toBeInTheDocument();
        expect(screen.getByRole('group', { name: /time window/i })).toBeInTheDocument();
    });
    it('shows validation error for invalid owner/repo format', async () => {
        const mockFetch = vi.fn();
        vi.stubGlobal('fetch', mockFetch);
        render(_jsx(RepoForm, { onSubmit: vi.fn(), loading: false }));
        await userEvent.type(screen.getByLabelText(/owner\/repo/i), 'liatrio');
        fireEvent.click(screen.getByRole('button', { name: /load metrics/i }));
        expect(await screen.findByText(/format must be owner\/repo/i)).toBeInTheDocument();
        expect(mockFetch).not.toHaveBeenCalled();
    });
    it('calls onSubmit with correct args on valid submission', async () => {
        const mockSubmit = vi.fn();
        render(_jsx(RepoForm, { onSubmit: mockSubmit, loading: false }));
        await userEvent.type(screen.getByLabelText(/owner\/repo/i), 'liatrio/liatrio');
        await userEvent.type(screen.getByLabelText(/personal access token/i), 'fake-token');
        fireEvent.click(screen.getByRole('button', { name: /load metrics/i }));
        await waitFor(() => {
            expect(mockSubmit).toHaveBeenCalledWith('liatrio', 'liatrio', 'fake-token', 30);
        });
    });
    it('disables submit button when loading', () => {
        render(_jsx(RepoForm, { onSubmit: vi.fn(), loading: true }));
        expect(screen.getByRole('button', { name: /loading/i })).toBeDisabled();
    });
});
