import { useEffect, useMemo, useState } from 'react';
import { adminApi, ApiUser, authApi } from './api';

const statusText: Record<ApiUser['status'], string> = { PENDING: '승인 대기', APPROVED: '승인됨', REJECTED: '거절됨' };
const formatDate = (value?: string | null) => value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—';

export default function AdminPage({ user, onLogout }: { user: ApiUser; onLogout: () => void }) {
  const [users, setUsers] = useState<ApiUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [changingId, setChangingId] = useState('');
  const pending = useMemo(() => users.filter((item) => item.status === 'PENDING').length, [users]);

  const load = async () => {
    setLoading(true);
    setError('');
    try { setUsers(await adminApi.listUsers()); }
    catch (reason) { setError(reason instanceof Error ? reason.message : '사용자 정보를 불러오지 못했어요.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  const updateStatus = async (id: string, status: 'APPROVED' | 'REJECTED') => {
    setChangingId(id);
    setError('');
    try {
      const updated = await adminApi.updateStatus(id, status);
      setUsers((current) => current.map((item) => item.id === id ? updated : item));
    } catch (reason) { setError(reason instanceof Error ? reason.message : '상태를 변경하지 못했어요.'); }
    finally { setChangingId(''); }
  };

  const logout = async () => { await authApi.logout(); onLogout(); };

  return (
    <main className="admin-shell">
      <header className="admin-header">
        <div className="admin-brand"><span>✓</span><div><strong>꼬박꼬박 관리자</strong><small>KKOBAK ADMIN</small></div></div>
        <div className="admin-user"><span>{user.displayName}</span><button onClick={() => void logout()}>로그아웃</button></div>
      </header>
      <section className="admin-content">
        <div className="admin-title"><div><p>MEMBERS</p><h1>사용자 관리</h1><span>가입 요청을 확인하고 서비스 이용을 승인할 수 있어요.</span></div><button onClick={() => void load()}>↻ 새로고침</button></div>
        <div className="admin-stats">
          <article><span>전체 계정</span><strong>{users.length}</strong></article>
          <article className="pending"><span>승인 대기</span><strong>{pending}</strong></article>
          <article><span>승인 사용자</span><strong>{users.filter((item) => item.status === 'APPROVED' && item.role !== 'ADMIN').length}</strong></article>
        </div>
        {error && <p className="admin-error" role="alert">{error}</p>}
        <div className="user-table-wrap">
          {loading ? <div className="admin-empty">사용자 정보를 불러오는 중이에요…</div> : users.length === 0 ? <div className="admin-empty">사용자가 없어요.</div> : (
            <table className="user-table">
              <thead><tr><th>사용자</th><th>아이디</th><th>상태</th><th>가입일</th><th>최근 로그인</th><th>관리</th></tr></thead>
              <tbody>{users.map((item) => <tr key={item.id}>
                <td><strong>{item.displayName}</strong><small>{item.email}</small></td>
                <td>{item.username}{item.role === 'ADMIN' && <em>ADMIN</em>}</td>
                <td><span className={`status-pill ${item.status.toLowerCase()}`}>{statusText[item.status]}</span></td>
                <td>{formatDate(item.createdAt)}</td><td>{formatDate(item.lastLoginAt)}</td>
                <td>{item.role === 'ADMIN' ? <span className="fixed-admin">관리자 계정</span> : <div className="admin-actions"><button disabled={changingId === item.id || item.status === 'APPROVED'} onClick={() => void updateStatus(item.id, 'APPROVED')}>승인</button><button className="reject" disabled={changingId === item.id || item.status === 'REJECTED'} onClick={() => void updateStatus(item.id, 'REJECTED')}>거절</button></div>}</td>
              </tr>)}</tbody>
            </table>
          )}
        </div>
      </section>
    </main>
  );
}
