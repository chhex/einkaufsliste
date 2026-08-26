// Vite exponiert nur Env-Vars mit VITE_-Praefix automatisch ans Frontend
// (import.meta.env) - siehe frontend/.env.example.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';
