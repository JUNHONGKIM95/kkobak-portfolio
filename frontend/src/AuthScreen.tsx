import { FormEvent, useState } from 'react';
import { ApiUser, authApi } from './api';

export default function AuthScreen({ onAuthenticated }: { onAuthenticated: (user: ApiUser) => void }) {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    setBusy(true);
    setError('');
    setMessage('');
    const form = new FormData(formElement);
    try {
      if (mode === 'login') {
        const user = await authApi.login(String(form.get('username')), String(form.get('password')));
        onAuthenticated(user);
      } else {
        await authApi.signup({
          username: String(form.get('username')),
          password: String(form.get('password')),
          displayName: String(form.get('displayName')),
          email: String(form.get('email')),
        });
        formElement.reset();
        setMode('login');
        setMessage('가입 요청을 보냈어요. 관리자가 승인한 뒤 로그인할 수 있어요.');
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '요청을 처리하지 못했어요.');
    } finally {
      setBusy(false);
    }
  };

  const changeMode = (next: 'login' | 'signup') => {
    setMode(next);
    setError('');
    setMessage('');
  };

  return (
    <main className="auth-shell">
      <div className="ambient ambient-one" /><div className="ambient ambient-two" />
      <section className="auth-card">
        <div className="auth-brand"><span>✓</span><h1>꼬박꼬박<small>KKOBAK</small></h1></div>
        <p className="auth-kicker">작은 생활 주기도 잊지 않도록</p>
        <h2>{mode === 'login' ? '다시 만나 반가워요!' : '꼬박꼬박 시작하기'}</h2>
        <p className="auth-copy">로그인하면 스마트폰과 랩탑에서 같은 주기를 확인할 수 있어요.</p>
        <div className="auth-tabs" role="tablist">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => changeMode('login')} type="button">로그인</button>
          <button className={mode === 'signup' ? 'active' : ''} onClick={() => changeMode('signup')} type="button">계정 만들기</button>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {mode === 'signup' && <div className="auth-field-row">
            <label><span>이름</span><input name="displayName" required maxLength={80} placeholder="꼬박이" /></label>
            <label><span>이메일</span><input name="email" required type="email" placeholder="me@example.com" /></label>
          </div>}
          <label><span>아이디</span><input name="username" required minLength={3} maxLength={30} pattern="[A-Za-z0-9._-]+" autoComplete="username" placeholder="영문·숫자 3자 이상" /></label>
          <label><span>비밀번호</span><input name="password" required minLength={mode === 'signup' ? 8 : 1} maxLength={72} type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} placeholder={mode === 'signup' ? '8자 이상 입력해 주세요' : '비밀번호'} /></label>
          {message && <p className="form-message success" role="status">{message}</p>}
          {error && <p className="form-message error" role="alert">{error}</p>}
          <button className="auth-submit" type="submit" disabled={busy}>{busy ? '잠시만요…' : mode === 'login' ? '로그인' : '가입 요청 보내기'}</button>
        </form>
        {mode === 'signup' && <p className="approval-note">계정은 관리자 승인 후 사용할 수 있어요.</p>}
      </section>
    </main>
  );
}
