export type ApiCycle = {
  id: string;
  title: string;
  category: string;
  cycleType: '교체' | '청소' | '세탁';
  emoji: string;
  intervalValue: number;
  intervalUnit: '일' | '주' | '개월';
  startDate: string;
  lastCompletedDate: string;
  nextDueDate: string;
  imageUrl?: string | null;
  color: string;
  completedToday: boolean;
  daysLeft: number;
  ended: boolean;
  endedAt?: string | null;
};

export type CycleInput = Pick<ApiCycle, 'title' | 'category' | 'cycleType' | 'emoji' | 'intervalValue' | 'intervalUnit' | 'startDate' | 'imageUrl' | 'color'>;

export type ApiUser = {
  id: string;
  username: string;
  displayName: string;
  email: string;
  role: 'ADMIN' | 'USER';
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  approvedAt?: string | null;
  lastLoginAt?: string | null;
};

export type ApiReport = {
  totalCycles: number;
  onTrackCycles: number;
  overdueCycles: number;
  dueTodayCycles: number;
  completedLast7Days: number;
  completedLast30Days: number;
  currentScore: number;
  streakDays: number;
  dailyActivity: { date: string; count: number }[];
  typeSummary: { type: string; total: number; overdue: number; dueToday: number }[];
  cycleAchievements: {
    cycleId: string;
    title: string;
    cycleType: string;
    emoji: string;
    color: string;
    imageUrl?: string | null;
    completedCount: number;
    onTimeCount: number;
    lateCount: number;
    trackedRounds: number;
    actionRequired: boolean;
    nextDueDate: string;
    achievementRate: number | null;
    ended: boolean;
    endedAt?: string | null;
  }[];
};

type LoginResponse = { token: string; user: ApiUser };

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
const TOKEN_KEY = 'kkobak-auth-token';
const USER_KEY = 'kkobak-auth-user';
const REPORT_CACHE_PREFIX = 'kkobak-report-';

export function getAuthToken() { return window.localStorage.getItem(TOKEN_KEY); }
export function setAuthToken(token: string) { window.localStorage.setItem(TOKEN_KEY, token); }
export function getCachedUser(): ApiUser | null {
  if (!getAuthToken()) return null;
  try {
    const value = window.localStorage.getItem(USER_KEY);
    if (!value) return null;
    const user = JSON.parse(value) as ApiUser;
    return user?.id && user?.username ? user : null;
  } catch { return null; }
}
function cacheUser(user: ApiUser) { window.localStorage.setItem(USER_KEY, JSON.stringify(user)); }
export function clearAuthToken() {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(USER_KEY);
}
export function getCachedReport(userId: string): ApiReport | null {
  try {
    const value = window.localStorage.getItem(`${REPORT_CACHE_PREFIX}${userId}`);
    return value ? JSON.parse(value) as ApiReport : null;
  } catch { return null; }
}
export function cacheReport(userId: string, report: ApiReport) {
  window.localStorage.setItem(`${REPORT_CACHE_PREFIX}${userId}`, JSON.stringify(report));
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers = new Headers(init.headers);
  if (!(init.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!response.ok) {
    let message = `요청을 처리하지 못했어요. (${response.status})`;
    try {
      const error = await response.json() as { message?: string; detail?: string };
      message = error.message || error.detail || message;
    } catch { /* use the status message */ }
    if (response.status === 401 && !path.endsWith('/login')) clearAuthToken();
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function getLegacyOwnerKey() {
  const existing = window.localStorage.getItem('kkobak-owner-key');
  if (existing) return existing;
  const created = crypto.randomUUID();
  window.localStorage.setItem('kkobak-owner-key', created);
  return created;
}

export const authApi = {
  signup: (body: { username: string; password: string; displayName: string; email: string }) =>
    request<ApiUser>('/api/auth/signup', { method: 'POST', body: JSON.stringify(body) }),
  login: async (username: string, password: string) => {
    const result = await request<LoginResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) });
    setAuthToken(result.token);
    cacheUser(result.user);
    return result.user;
  },
  me: async () => {
    const user = await request<ApiUser>('/api/auth/me');
    cacheUser(user);
    return user;
  },
  logout: async () => {
    try { await request<void>('/api/auth/logout', { method: 'POST' }); } finally { clearAuthToken(); }
  },
};

export const adminApi = {
  listUsers: () => request<ApiUser[]>('/api/admin/users'),
  updateStatus: (id: string, status: 'APPROVED' | 'REJECTED') => request<ApiUser>(`/api/admin/users/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
};

export const cycleApi = {
  list: () => request<ApiCycle[]>('/api/cycles'),
  create: (body: CycleInput) => request<ApiCycle>('/api/cycles', { method: 'POST', body: JSON.stringify(body) }),
  update: (id: string, body: CycleInput) => request<ApiCycle>(`/api/cycles/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (id: string) => request<void>(`/api/cycles/${id}`, { method: 'DELETE' }),
  complete: (id: string) => request<ApiCycle>(`/api/cycles/${id}/complete`, { method: 'POST' }),
  undo: (id: string) => request<ApiCycle>(`/api/cycles/${id}/complete`, { method: 'DELETE' }),
  end: (id: string) => request<ApiCycle>(`/api/cycles/${id}/end`, { method: 'POST' }),
  reopen: (id: string) => request<ApiCycle>(`/api/cycles/${id}/end`, { method: 'DELETE' }),
  claim: (ownerKey: string) => request<{ cycles: number; completions: number; subscriptions: number }>('/api/cycles/claim', { method: 'POST', body: JSON.stringify({ ownerKey }) }),
};

let reportSummaryRequest: { token: string | null; promise: Promise<ApiReport> } | null = null;
export const reportApi = {
  summary: () => {
    const token = getAuthToken();
    if (reportSummaryRequest?.token === token) return reportSummaryRequest.promise;
    const current = { token, promise: request<ApiReport>('/api/reports/summary') };
    reportSummaryRequest = current;
    const clear = () => { if (reportSummaryRequest === current) reportSummaryRequest = null; };
    void current.promise.then(clear, clear);
    return current.promise;
  },
};

export async function compressCycleImage(file: File) {
  if (!file.type.startsWith('image/')) throw new Error('이미지 파일만 등록할 수 있어요.');
  const imageUrl = URL.createObjectURL(file);
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const element = new Image();
      element.onload = () => resolve(element);
      element.onerror = () => reject(new Error('이미지를 읽을 수 없어요.'));
      element.src = imageUrl;
    });
    const maxSide = 1600;
    const scale = Math.min(1, maxSide / Math.max(image.naturalWidth, image.naturalHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
    const context = canvas.getContext('2d');
    if (!context) return file;
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/webp', 0.8));
    if (!blob || blob.size >= file.size) return file;
    const baseName = file.name.replace(/\.[^.]+$/, '') || 'cycle-image';
    return new File([blob], `${baseName}.webp`, { type: 'image/webp', lastModified: Date.now() });
  } finally {
    URL.revokeObjectURL(imageUrl);
  }
}

export async function uploadCycleImage(file: File) {
  const form = new FormData();
  form.append('file', file);
  return request<{ url: string }>('/api/media', { method: 'POST', body: form });
}

function urlBase64ToUint8Array(value: string) {
  const padding = '='.repeat((4 - (value.length % 4)) % 4);
  const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/');
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0));
}

export async function enablePush() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) throw new Error('unsupported');
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') throw new Error('denied');
  const registration = await navigator.serviceWorker.register('/push-sw.js');
  const key = await request<{ enabled: boolean; publicKey: string }>('/api/push/public-key');
  if (!key.enabled) throw new Error('not-configured');
  const subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(key.publicKey) });
  await request<void>('/api/push/subscriptions', { method: 'POST', body: JSON.stringify(subscription.toJSON()) });
}

export async function getPushState(): Promise<'enabled' | 'disabled' | 'blocked' | 'unavailable'> {
  if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) return 'unavailable';
  const key = await request<{ enabled: boolean; publicKey: string }>('/api/push/public-key');
  if (!key.enabled) return 'unavailable';
  if (Notification.permission === 'denied') return 'blocked';
  if (Notification.permission !== 'granted') return 'disabled';
  const registration = await navigator.serviceWorker.getRegistration();
  if (!registration) return 'disabled';
  return await registration.pushManager.getSubscription() ? 'enabled' : 'disabled';
}
