import { useEffect, useMemo, useState } from 'react';
import { ApiReport, reportApi } from './api';

const dayFormat = new Intl.DateTimeFormat('ko-KR', { month: 'numeric', day: 'numeric' });

export default function ReportView() {
  const [report, setReport] = useState<ApiReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try { setReport(await reportApi.summary()); }
    catch (reason) { setError(reason instanceof Error ? reason.message : '리포트를 불러오지 못했어요.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { void load(); }, []);

  if (loading) return <section className="page-view report-page"><ReportTitle /><div className="report-loading">완료 기록을 모아보는 중이에요…</div></section>;
  if (error || !report) return <section className="page-view report-page"><ReportTitle /><div className="report-loading"><p>{error}</p><button onClick={() => void load()}>다시 불러오기</button></div></section>;
  const cycleAchievements = report.cycleAchievements ?? [];

  return <section className="page-view report-page">
    <ReportTitle />
    <div className="report-metrics">
      <article><span>현재 관리 점수</span><strong>{report.currentScore}<small>점</small></strong><p>오늘 챙길 항목까지 반영한 관리 비율</p></article>
      <article><span>최근 7일 완료</span><strong>{report.completedLast7Days}<small>회</small></strong><p>꼬박 완료 버튼을 누른 횟수</p></article>
      <article><span>최근 30일 완료</span><strong>{report.completedLast30Days}<small>회</small></strong><p>지난 한 달 동안의 완료 기록</p></article>
      <article><span>연속 달성</span><strong>{report.streakDays}<small>일</small></strong><p>오늘 또는 어제부터 이어진 기록</p></article>
    </div>

    <div className="report-grid">
      <article className="report-panel score-panel">
        <div className="report-panel-head"><div><span>CURRENT STATUS</span><h2>내 주기 달성 정도</h2></div><small>오늘 기준</small></div>
        <div className="score-content">
          <div className="score-ring" role="img" aria-label={`현재 관리 점수 ${report.currentScore}점`} style={{ background: `conic-gradient(#69afe2 ${report.currentScore * 3.6}deg, #e7f1f7 0deg)` }}><div><strong>{report.currentScore}</strong><span>/ 100</span></div></div>
          <div className="score-breakdown">
            <p><span className="status-dot on-track" />정상 관리 중<strong>{report.onTrackCycles}개</strong></p>
            <p><span className="status-dot today" />오늘 예정<strong>{report.dueTodayCycles}개</strong></p>
            <p><span className="status-dot overdue" />기한 지남<strong>{report.overdueCycles}개</strong></p>
            <small>전체 {report.totalCycles}개 주기를 기준으로 계산했어요.</small>
          </div>
        </div>
      </article>

      <ActivityChart report={report} />
    </div>

    <article className="report-panel type-panel">
      <div className="report-panel-head"><div><span>BY TYPE</span><h2>종류별 관리 현황</h2></div><small>오늘 예정과 기한이 지난 항목을 반영해요</small></div>
      {report.typeSummary.length ? <div className="type-report-list">{report.typeSummary.map((item) => {
        const dueToday = item.dueToday ?? 0;
        const onTrack = item.total - item.overdue - dueToday;
        const percent = Math.round(onTrack * 100 / Math.max(item.total, 1));
        const actionText = [dueToday > 0 && `오늘 예정 ${dueToday}개`, item.overdue > 0 && `기한 지남 ${item.overdue}개`].filter(Boolean).join(' · ');
        return <div className="type-report" key={item.type}><div><strong>{item.type}</strong><span>{onTrack}/{item.total}개 정상 관리</span><em>{percent}%</em></div><div className="type-track" role="progressbar" aria-label={`${item.type} 정상 관리율`} aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}><span style={{ width: `${percent}%` }} /></div>{actionText && <small>{actionText}</small>}</div>;
      })}</div> : <div className="report-empty">주기를 등록하면 종류별 현황이 표시돼요.</div>}
    </article>

    <article className="report-panel habit-panel">
      <div className="report-panel-head"><div><span>HABIT BY CYCLE</span><h2>주기별 습관 달성률</h2></div><small>예정일 안에 완료한 회차를 기준으로 계산해요</small></div>
      {cycleAchievements.length ? <div className="habit-list">{cycleAchievements.map((item) => {
        const measured = item.achievementRate !== null;
        return <div className={`habit-row ${item.actionRequired ? 'needs-action' : ''}`} key={item.cycleId}>
          <span className={`habit-icon ${item.color}`}>{item.imageUrl ? <img src={item.imageUrl} alt="" /> : item.emoji}</span>
          <div className="habit-main">
            <div className="habit-title"><div><strong>{item.title}</strong><small>{item.cycleType}</small></div><em>{measured ? `${item.achievementRate}%` : '측정 전'}</em></div>
            <div className="habit-track" role="progressbar" aria-label={`${item.title} 습관 달성률`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={item.achievementRate ?? 0}><span style={{ width: `${item.achievementRate ?? 0}%` }} /></div>
            <div className="habit-detail"><span>{measured ? `제때 완료 ${item.onTimeCount}/${item.trackedRounds}회` : '첫 예정일이 지나면 측정을 시작해요'}</span><span>{item.actionRequired ? '지금 완료할 차례예요' : `다음 예정 ${dayFormat.format(new Date(`${item.nextDueDate}T00:00:00`))}`}</span></div>
          </div>
        </div>;
      })}</div> : <div className="report-empty">주기를 등록하면 각각의 습관 달성률을 확인할 수 있어요.</div>}
    </article>
  </section>;
}

function ReportTitle() {
  return <div className="page-title"><div><p className="eyebrow"><span>▥</span> MY REPORT</p><h1>꼬박 리포트</h1><p>완료 기록과 기한 상태로 생활 주기 달성 정도를 확인해요.</p></div></div>;
}

function ActivityChart({ report }: { report: ApiReport }) {
  const maximum = useMemo(() => Math.max(1, ...report.dailyActivity.map((item) => item.count)), [report.dailyActivity]);
  const total = report.dailyActivity.reduce((sum, item) => sum + item.count, 0);
  return <article className="report-panel activity-panel">
    <div className="report-panel-head"><div><span>LAST 14 DAYS</span><h2>일별 완료 기록</h2></div><small>총 {total}회</small></div>
    <div className="activity-chart" role="img" aria-label={`최근 14일 동안 총 ${total}회 완료`}>
      {report.dailyActivity.map((item, index) => <div className="activity-column" key={item.date}>
        <span className="activity-value">{item.count || ''}</span>
        <div className="activity-bar-track"><span className={item.count === 0 ? 'empty' : ''} style={{ height: `${item.count === 0 ? 4 : Math.max(14, item.count / maximum * 100)}%` }} /></div>
        <small className={index % 2 === 0 || index === report.dailyActivity.length - 1 ? '' : 'label-hidden'}>{dayFormat.format(new Date(`${item.date}T00:00:00`))}</small>
      </div>)}
    </div>
  </article>;
}
