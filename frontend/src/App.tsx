import { FormEvent, useEffect, useMemo, useState } from 'react';
import AdminPage from './AdminPage';
import AuthScreen from './AuthScreen';
import ReportView from './ReportView';
import { ApiCycle, ApiUser, authApi, compressCycleImage, cycleApi, enablePush, getAuthToken, getLegacyOwnerKey, getPushState, uploadCycleImage } from './api';

type View = 'home' | 'cycles' | 'calendar' | 'report';
type CycleType = '교체' | '청소' | '세탁';
type CycleUnit = '일' | '주' | '개월';
type Task = {
  id: string;
  title: string;
  category: string;
  type: CycleType;
  emoji: string;
  interval: number;
  unit: CycleUnit;
  startDate: string;
  lastCompletedDate: string;
  nextDueDate: string;
  lastDone: string;
  daysLeft: number;
  color: string;
  completed: boolean;
  imageUrl?: string | null;
};

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

const tabs: { id: View; label: string; symbol: string }[] = [
  { id: 'home', label: '홈', symbol: '⌂' },
  { id: 'cycles', label: '주기 관리', symbol: '◷' },
  { id: 'calendar', label: '캘린더', symbol: '▦' },
  { id: 'report', label: '리포트', symbol: '▥' },
];
const typeColors: Record<CycleType, string> = { 교체: 'blue', 청소: 'mint', 세탁: 'violet' };
const typeEmoji: Record<CycleType, string> = { 교체: '✨', 청소: '🫧', 세탁: '🧺' };

function localDateString(date = new Date()) {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

function isStandaloneMode() {
  const navigatorWithStandalone = navigator as Navigator & { standalone?: boolean };
  return window.matchMedia('(display-mode: standalone)').matches || navigatorWithStandalone.standalone === true;
}

function dueLabel(days: number) {
  if (days < 0) return `${Math.abs(days)}일 지났어요`;
  if (days === 0) return '오늘까지';
  return `${days}일 남았어요`;
}

function dday(days: number) {
  if (days < 0) return `D+${Math.abs(days)}`;
  if (days === 0) return 'D-DAY';
  return `D-${days}`;
}

function Brand() {
  return <span className="brand"><span className="brand-mark"><span>✓</span></span><span>꼬박꼬박<small>KKOBAK</small></span></span>;
}

function Mascot({ tiny = false }: { tiny?: boolean }) {
  return <div className={tiny ? 'mini-mascot' : 'mascot-wrap'} aria-hidden="true">
    {!tiny && <><span className="spark spark-one">✦</span><span className="spark spark-two">✦</span></>}
    <div className="mascot"><span className="ear left" /><span className="ear right" /><span className="eye left" /><span className="eye right" /><span className="mouth">⌣</span></div>
    {!tiny && <span className="mascot-shadow" />}
  </div>;
}

function fromApi(task: ApiCycle): Task {
  return {
    id: task.id, title: task.title, category: task.category, type: task.cycleType, emoji: task.emoji,
    interval: task.intervalValue, unit: task.intervalUnit, startDate: task.startDate,
    lastCompletedDate: task.lastCompletedDate, nextDueDate: task.nextDueDate,
    lastDone: new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date(`${task.lastCompletedDate}T00:00:00`)),
    daysLeft: task.daysLeft, color: task.color, completed: task.completedToday, imageUrl: task.imageUrl,
  };
}

export default function App() {
  const [user, setUser] = useState<ApiUser | null>(null);
  const [checking, setChecking] = useState(Boolean(getAuthToken()));

  useEffect(() => {
    if (!getAuthToken()) return;
    authApi.me().then(setUser).catch(() => setUser(null)).finally(() => setChecking(false));
  }, []);

  if (checking) return <main className="app-loading"><Mascot /><strong>꼬박꼬박 불러오는 중…</strong></main>;
  if (!user) return <AuthScreen onAuthenticated={setUser} />;
  if (user.role === 'ADMIN') return <AdminPage user={user} onLogout={() => setUser(null)} />;
  return <CycleHome user={user} onLogout={() => setUser(null)} />;
}

function TaskRow({ task, onToggle, onEdit, onDelete, actions = false }: { task: Task; onToggle: (id: string) => void; onEdit?: (task: Task) => void; onDelete?: (task: Task) => void; actions?: boolean }) {
  return <article className={`task-card ${task.daysLeft <= 0 ? 'featured' : ''} ${task.completed ? 'is-done' : ''}`}>
    <button className="check-button" onClick={() => onToggle(task.id)} aria-label={`${task.title} ${task.completed ? '완료 취소' : '완료'}`}>{task.completed ? '✓' : ''}</button>
    <div className={`task-emoji ${task.color}`}>{task.imageUrl ? <img src={task.imageUrl} alt="" /> : task.emoji}</div>
    <div className="task-info"><span className="category">{task.category} · {task.type}</span><h3>{task.title}</h3><p><span>↻</span> {task.interval}{task.unit}마다 · 시작 {formatShortDate(task.startDate)} · 마지막 {task.type} {task.lastDone}</p></div>
    <div className={`due-block ${task.daysLeft < 0 ? 'overdue' : ''}`}><span>{task.completed ? '완료했어요' : dueLabel(task.daysLeft)}</span><strong>{task.completed ? 'DONE' : dday(task.daysLeft)}</strong></div>
    {actions && <div className="task-actions"><button onClick={() => onEdit?.(task)} aria-label={`${task.title} 수정`}>수정</button><button className="delete" onClick={() => onDelete?.(task)} aria-label={`${task.title} 삭제`}>삭제</button></div>}
  </article>;
}

function formatShortDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'numeric', day: 'numeric' }).format(new Date(`${value}T00:00:00`));
}

function CycleHome({ user, onLogout }: { user: ApiUser; onLogout: () => void }) {
  const [view, setView] = useState<View>('home');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [ready, setReady] = useState(false);
  const [online, setOnline] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [savingTask, setSavingTask] = useState(false);
  const [filter, setFilter] = useState<'전체' | CycleType>('전체');
  const [query, setQuery] = useState('');
  const [toast, setToast] = useState('');
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [standalone, setStandalone] = useState(isStandaloneMode);
  const [installGuideOpen, setInstallGuideOpen] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);
  const [notificationState, setNotificationState] = useState<'enabled' | 'disabled' | 'blocked' | 'unavailable'>('disabled');
  const isIos = /iphone|ipad|ipod/i.test(navigator.userAgent);
  const isChromium = /chrome|crios|edga|samsungbrowser/i.test(navigator.userAgent);
  const storageKey = `kkobak-tasks-${user.id}`;

  useEffect(() => {
    const load = async () => {
      const saved = window.localStorage.getItem(storageKey);
      if (saved) try { setTasks(JSON.parse(saved) as Task[]); } catch { /* ignore damaged cache */ }
      try {
        const claimed = await cycleApi.claim(getLegacyOwnerKey());
        const remote = await cycleApi.list();
        setTasks(remote.map(fromApi));
        setOnline(true);
        if (claimed.cycles > 0) setToast(`이 기기의 기존 주기 ${claimed.cycles}개를 계정에 연결했어요`);
      } catch (reason) {
        setOnline(false);
        setToast(reason instanceof Error ? reason.message : '서버에 연결하지 못했어요.');
      } finally { setReady(true); }
    };
    void load();
  }, [storageKey]);

  useEffect(() => {
    const sync = async () => {
      if (document.visibilityState === 'hidden') return;
      try { setTasks((await cycleApi.list()).map(fromApi)); setOnline(true); } catch { setOnline(false); }
    };
    const handleVisibility = () => { if (document.visibilityState === 'visible') void sync(); };
    window.addEventListener('focus', sync);
    document.addEventListener('visibilitychange', handleVisibility);
    return () => { window.removeEventListener('focus', sync); document.removeEventListener('visibilitychange', handleVisibility); };
  }, []);

  useEffect(() => {
    getPushState().then(setNotificationState).catch(() => setNotificationState('unavailable'));
    const handleScroll = () => setShowScrollTop(window.scrollY > 320);
    handleScroll();
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => { if (ready) window.localStorage.setItem(storageKey, JSON.stringify(tasks)); }, [tasks, ready, storageKey]);
  useEffect(() => { if (!toast) return; const timer = window.setTimeout(() => setToast(''), 2600); return () => window.clearTimeout(timer); }, [toast]);
  useEffect(() => {
    const handleInstallPrompt = (event: Event) => { event.preventDefault(); setInstallPrompt(event as BeforeInstallPromptEvent); };
    const handleInstalled = () => { setInstallPrompt(null); setStandalone(true); setToast('꼬박꼬박이 앱으로 설치됐어요'); };
    window.addEventListener('beforeinstallprompt', handleInstallPrompt);
    window.addEventListener('appinstalled', handleInstalled);
    return () => { window.removeEventListener('beforeinstallprompt', handleInstallPrompt); window.removeEventListener('appinstalled', handleInstalled); };
  }, []);

  const dueTasks = tasks.filter((task) => task.daysLeft <= 0 && !task.completed);
  const todayTasks = tasks.filter((task) => task.daysLeft <= 0 || task.completed);
  const completed = tasks.filter((task) => task.completed).length;
  const score = Math.round(((tasks.length - dueTasks.length) / Math.max(tasks.length, 1)) * 100);
  const visibleTasks = tasks.filter((task) => (filter === '전체' || task.type === filter) && task.title.toLowerCase().includes(query.toLowerCase()));
  const dateText = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date());

  const installApp = async () => {
    if (installPrompt) { await installPrompt.prompt(); if ((await installPrompt.userChoice).outcome === 'accepted') setInstallPrompt(null); return; }
    setInstallGuideOpen(true);
  };

  const logout = async () => { await authApi.logout(); onLogout(); };
  const changeView = (next: View) => { setView(next); window.scrollTo({ top: 0, behavior: 'smooth' }); };

  const toggleTask = async (id: string) => {
    const target = tasks.find((task) => task.id === id);
    if (!target) return;
    setTasks((current) => current.map((task) => task.id === id ? { ...task, completed: !task.completed } : task));
    try {
      const updated = target.completed ? await cycleApi.undo(id) : await cycleApi.complete(id);
      setTasks((current) => current.map((task) => task.id === id ? fromApi(updated) : task));
      setOnline(true);
      setToast(target.completed ? '완료를 취소하고 이전 일정으로 되돌렸어요' : `${target.title}, 꼬박 완료! 다음은 ${formatShortDate(updated.nextDueDate)}이에요`);
    } catch (reason) {
      setTasks((current) => current.map((task) => task.id === id ? target : task));
      setOnline(false);
      setToast(reason instanceof Error ? reason.message : '변경 내용을 저장하지 못했어요.');
    }
  };

  const openCreate = () => { setEditingTask(null); setModalOpen(true); };
  const openEdit = (task: Task) => { setEditingTask(task); setModalOpen(true); };

  const saveTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSavingTask(true);
    const form = new FormData(event.currentTarget);
    const type = form.get('type') as CycleType;
    let imageUrl = editingTask?.imageUrl || null;
    try {
      const image = form.get('image');
      if (image instanceof File && image.size > 0) {
        setToast('이미지를 가볍게 줄여 업로드하고 있어요…');
        imageUrl = (await uploadCycleImage(await compressCycleImage(image))).url;
      }
      const body = {
        title: String(form.get('title')).trim(), category: String(form.get('category')), cycleType: type,
        emoji: editingTask?.type === type ? editingTask.emoji : typeEmoji[type], intervalValue: Number(form.get('interval')),
        intervalUnit: form.get('unit') as CycleUnit, startDate: String(form.get('startDate')),
        imageUrl, color: editingTask?.type === type ? editingTask.color : typeColors[type],
      };
      if (editingTask) {
        const updated = await cycleApi.update(editingTask.id, body);
        setTasks((current) => current.map((task) => task.id === editingTask.id ? fromApi(updated) : task));
        setToast('주기를 수정했어요');
      } else {
        const created = await cycleApi.create(body);
        setTasks((current) => [...current, fromApi(created)]);
        setToast('새 주기를 등록했어요');
      }
      setOnline(true);
      setModalOpen(false);
      setEditingTask(null);
      setView('cycles');
    } catch (reason) {
      setOnline(false);
      setToast(reason instanceof Error ? reason.message : '주기를 저장하지 못했어요.');
    } finally { setSavingTask(false); }
  };

  const deleteTask = async (task: Task) => {
    if (!window.confirm(`‘${task.title}’ 주기를 삭제할까요? 완료 기록도 함께 삭제됩니다.`)) return;
    try { await cycleApi.delete(task.id); setTasks((current) => current.filter((item) => item.id !== task.id)); setToast('주기를 삭제했어요'); }
    catch (reason) { setToast(reason instanceof Error ? reason.message : '주기를 삭제하지 못했어요.'); }
  };

  return <main className="app-shell">
    <div className="ambient ambient-one" /><div className="ambient ambient-two" />
    <header className="topbar">
      <button className="brand-button" onClick={() => setView('home')} aria-label="꼬박꼬박 홈"><Brand /></button>
      <div className="header-actions">
        {!standalone && (installPrompt || isIos || isChromium) && <button className="install-button" onClick={() => void installApp()}><span>⇩</span><b>앱 설치</b></button>}
        <button className="logout-button" onClick={() => void logout()} aria-label="로그아웃"><span aria-hidden="true">↪</span><b>로그아웃</b></button>
        <button className={`icon-button notification-button ${notificationState === 'enabled' ? 'enabled' : ''}`} aria-label={notificationState === 'enabled' ? '알림 켜짐' : '알림 켜기'} onClick={async () => { try { await enablePush(); setNotificationState('enabled'); setToast('예정일 오전 9시 알림을 켰어요'); } catch (error) { setToast(error instanceof Error && error.message === 'denied' ? '브라우저 설정에서 알림 권한을 허용해 주세요' : error instanceof Error && error.message === 'not-configured' ? '서버 알림 키 설정이 필요해요' : '알림을 설정하지 못했어요'); } }}><span>♧</span>{notificationState === 'enabled' ? <i className="enabled-dot" /> : dueTasks.length > 0 && <i />}</button>
        <div className="profile-chip"><span className="avatar">{user.displayName.slice(0, 1)}</span><span><b>{user.displayName}</b><small>@{user.username}</small></span></div>
      </div>
    </header>
    <nav className="bottom-nav" aria-label="주요 메뉴">
      {tabs.map((tab) => <button key={tab.id} className={`nav-item ${view === tab.id ? 'active' : ''}`} onClick={() => changeView(tab.id)}><span className="nav-symbol">{tab.symbol}</span>{tab.label}</button>)}
    </nav>

    {view === 'home' && <>
      <section className="hero"><div><p className="eyebrow"><span>☀</span> {dateText}</p><h1>오늘도 하나씩,<br /><strong>꼬박꼬박</strong> 챙겨봐요!</h1><p className="hero-copy">{online ? '모든 기기에서 같은 생활 주기를 확인할 수 있어요.' : '저장된 내용을 보여드리고 있어요. 연결 상태를 확인해 주세요.'}</p></div><Mascot /></section>
      <section className="content-section"><div className="section-heading"><div><span className="title-icon">✓</span><div><h2>오늘 할 일</h2><p>완료한 항목도 오늘 동안 남아 있어요</p></div></div><span className="task-count">{dueTasks.length ? `${dueTasks.length}개 남았어요` : '모두 완료했어요!'}</span></div>{!ready ? <div className="no-result">주기를 불러오는 중이에요…</div> : todayTasks.length ? todayTasks.map((task) => <TaskRow key={task.id} task={task} onToggle={toggleTask} />) : <EmptyState />}</section>
      <section className="progress-card"><div className="progress-copy"><Mascot tiny /><div><span>생활 주기 달성률</span><strong>오늘도 <em>{score}%</em> 꼬박 챙겼어요!</strong></div></div><div className="progress-track"><span style={{ width: `${score}%` }} /></div><span className="progress-number">{score}%</span></section>
      <section className="upcoming-section"><div className="section-heading compact"><div><span className="title-icon lavender">◷</span><div><h2>다가오는 주기</h2><p>미리 알아두면 마음이 가벼워요</p></div></div><button className="text-button" onClick={() => setView('cycles')}>전체 보기 →</button></div><div className="mini-grid">{tasks.filter((task) => task.daysLeft > 0 && !task.completed).slice(0, 3).map((task) => <button key={task.id} className="mini-card" onClick={() => setView('cycles')}><span className={`task-emoji ${task.color}`}>{task.imageUrl ? <img src={task.imageUrl} alt="" /> : task.emoji}</span><span><small>{task.category}</small><b>{task.title}</b><em>{dday(task.daysLeft)}</em></span></button>)}</div></section>
    </>}

    {view === 'cycles' && <section className="page-view">
      <div className="page-title"><div><p className="eyebrow"><span>↻</span> MY CYCLES</p><h1>내 주기 관리</h1><p>생활 속 반복되는 일들을 한곳에서 관리해요.</p></div><button className="primary-button" onClick={openCreate}>＋ 새 주기 추가</button></div>
      <div className="toolbar"><div className="filters">{(['전체', '교체', '청소', '세탁'] as const).map((item) => <button className={filter === item ? 'selected' : ''} key={item} onClick={() => setFilter(item)}>{item}</button>)}</div><label className="search"><span>⌕</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="주기 검색" /></label></div>
      <div className="cycle-summary"><div><span>전체 주기</span><strong>{tasks.length}</strong></div><div><span>오늘 예정</span><strong>{dueTasks.length}</strong></div><div><span>완료</span><strong>{completed}</strong></div></div>
      <div className="task-list">{visibleTasks.length ? visibleTasks.map((task) => <TaskRow key={task.id} task={task} onToggle={toggleTask} onEdit={openEdit} onDelete={(item) => void deleteTask(item)} actions />) : <div className="no-result">찾는 주기가 없어요. 새 주기를 등록해 보세요.</div>}</div>
    </section>}

    {view === 'calendar' && <CalendarView tasks={tasks} onSelect={(id) => { const task = tasks.find((item) => item.id === id); if (task) setToast(`${task.title} · ${dday(task.daysLeft)}`); }} />}
    {view === 'report' && <ReportView />}
    <div className="floating-stack">
      {showScrollTop && <button className="scroll-top-button" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })} aria-label="화면 상단으로 이동">↑</button>}
      <button className="floating-add" onClick={openCreate} aria-label="새 주기 추가"><span>＋</span><b>새 주기 추가</b></button>
    </div>
    {toast && <div className="toast" role="status"><span>✓</span>{toast}</div>}
    {modalOpen && <CycleModal task={editingTask} saving={savingTask} onClose={() => { if (!savingTask) { setModalOpen(false); setEditingTask(null); } }} onSubmit={saveTask} />}
    {installGuideOpen && <InstallGuide isIos={isIos} onClose={() => setInstallGuideOpen(false)} />}
  </main>;
}

function CycleModal({ task, saving, onClose, onSubmit }: { task: Task | null; saving: boolean; onClose: () => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
    <div className="modal-head"><div><span>{task ? '생활 주기 수정' : '새로운 생활 주기'}</span><h2 id="modal-title">{task ? '주기 정보를 바꿔볼까요?' : '무엇을 꼬박 챙길까요?'}</h2></div><button type="button" onClick={onClose} aria-label="닫기">×</button></div>
    <form onSubmit={onSubmit}>
      <label><span>주기 이름</span><input name="title" required autoFocus defaultValue={task?.title || ''} placeholder="예: 에어컨 필터 청소" /></label>
      <div className="form-row"><label><span>공간</span><select name="category" defaultValue={task?.category || '거실'}><option>거실</option><option>주방</option><option>욕실</option><option>침실</option><option>기타</option></select></label><label><span>종류</span><select name="type" defaultValue={task?.type || '청소'}><option>교체</option><option>청소</option><option>세탁</option></select></label></div>
      <div className="form-row interval-row"><label><span>반복 주기</span><input name="interval" type="number" min="1" defaultValue={task?.interval || 1} required /></label><label><span>&nbsp;</span><select name="unit" defaultValue={task?.unit || '개월'}><option>일</option><option>주</option><option>개월</option></select></label></div>
      <label><span>시작 날짜</span><input name="startDate" type="date" required defaultValue={task?.startDate || localDateString()} /></label>
      <label><span>대표 이미지 · 선택</span><input name="image" type="file" accept="image/png,image/jpeg,image/webp,image/heic,image/heif" /><small className="image-hint">긴 변 1,600px 이하의 WebP로 자동 압축해 업로드해요.{task?.imageUrl ? ' 새 이미지를 고르지 않으면 기존 이미지를 유지해요.' : ''}</small></label>
      <div className="reminder-note"><span>♧</span><p><strong>알림도 함께 보내드릴게요</strong><br />예정일 아침에 꼬박이가 알려드려요.</p></div>
      <div className="modal-actions"><button type="button" disabled={saving} onClick={onClose}>취소</button><button type="submit" disabled={saving}>{saving ? '저장하는 중…' : task ? '수정 내용 저장' : '주기 등록하기'}</button></div>
    </form>
  </section></div>;
}

function CalendarView({ tasks, onSelect }: { tasks: Task[]; onSelect: (id: string) => void }) {
  const today = new Date();
  const [cursor, setCursor] = useState(() => new Date(today.getFullYear(), today.getMonth(), 1));
  const year = cursor.getFullYear();
  const month = cursor.getMonth();
  const days = useMemo(() => {
    const first = new Date(year, month, 1).getDay();
    const count = new Date(year, month + 1, 0).getDate();
    return [...Array(first).fill(null), ...Array.from({ length: count }, (_, index) => index + 1)];
  }, [year, month]);
  const taskDates = new Map<number, Task[]>();
  tasks.filter((task) => !task.completed).forEach((task) => {
    const due = new Date(`${task.nextDueDate}T00:00:00`);
    if (due.getFullYear() === year && due.getMonth() === month) taskDates.set(due.getDate(), [...(taskDates.get(due.getDate()) || []), task]);
  });
  const move = (offset: number) => setCursor(new Date(year, month + offset, 1));
  const isToday = (day: number | null) => day === today.getDate() && year === today.getFullYear() && month === today.getMonth();
  return <section className="page-view calendar-page">
    <div className="page-title"><div><p className="eyebrow"><span>▦</span> CALENDAR</p><h1>{year}년 {month + 1}월</h1><p>다가오는 교체와 청소 일정을 한눈에 확인해요.</p></div><div className="month-buttons"><button aria-label="이전 달" onClick={() => move(-1)}>‹</button><button onClick={() => setCursor(new Date(today.getFullYear(), today.getMonth(), 1))}>오늘</button><button aria-label="다음 달" onClick={() => move(1)}>›</button></div></div>
    <div className="calendar-card"><div className="weekdays">{['일', '월', '화', '수', '목', '금', '토'].map((day) => <span key={day}>{day}</span>)}</div><div className="calendar-grid">{days.map((day, index) => <div className={`calendar-day ${isToday(day) ? 'today' : ''}`} key={`${day}-${index}`}>{day && <><span className="day-number">{day}</span><div className="day-events">{(taskDates.get(day) || []).slice(0, 3).map((task) => <button key={task.id} onClick={() => onSelect(task.id)}><span>{task.emoji}</span>{task.title}</button>)}</div></>}</div>)}</div></div>
    <div className="calendar-legend"><span><i className="blue-dot" /> 교체</span><span><i className="mint-dot" /> 청소</span><span><i className="violet-dot" /> 세탁</span></div>
  </section>;
}

function InstallGuide({ isIos, onClose }: { isIos: boolean; onClose: () => void }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section className="install-guide" role="dialog" aria-modal="true" aria-labelledby="install-guide-title"><img src="/icons/icon-192.png" alt="" /><span>{isIos ? '아이폰에 설치하기' : 'Chrome에서 설치하기'}</span><h2 id="install-guide-title">꼬박꼬박을 홈 화면에 추가해 주세요</h2>{isIos ? <ol><li>Safari 아래쪽의 <strong>공유</strong> 버튼을 누릅니다.</li><li><strong>홈 화면에 추가</strong>를 선택합니다.</li><li>오른쪽 위의 <strong>추가</strong>를 누릅니다.</li></ol> : <ol><li>Chrome 오른쪽 위의 <strong>⋮ 메뉴</strong>를 누릅니다.</li><li><strong>앱 설치</strong>를 선택합니다.</li><li>설치 확인창에서 <strong>설치</strong>를 누릅니다.</li></ol>}<p>설치된 아이콘으로 열면 주소창 없이 앱처럼 실행됩니다.</p><button type="button" onClick={onClose}>확인했어요</button></section></div>;
}

function EmptyState() { return <div className="empty-state"><Mascot tiny /><div><strong>오늘 할 일을 모두 마쳤어요!</strong><p>작은 습관이 모여 산뜻한 일상을 만들어요.</p></div><span>참 잘했어요 ✦</span></div>; }
