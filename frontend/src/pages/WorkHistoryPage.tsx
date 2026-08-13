import {
  AlertCircle,
  ArrowLeft,
  BarChart3,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock3,
  RotateCcw,
  UsersRound,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  fetchWorkSessionSummary,
  fetchWorkSessions,
  type WorkSession,
  type WorkSessionPage,
  type WorkSessionSummary,
} from "../api/workHistory";

const categoryColors = ["#7967dd", "#e5749d", "#3aa99e", "#ec9b46", "#5794d8"];

type Filters = {
  from: string;
  to: string;
  categoryId: string;
};

const emptyFilters: Filters = { from: "", to: "", categoryId: "" };

function formatDuration(totalSeconds: number) {
  const totalMinutes = Math.floor(Math.max(0, totalSeconds) / 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) return `${minutes}分`;
  if (minutes === 0) return `${hours}時間`;
  return `${hours}時間 ${minutes}分`;
}

function sessionDate(isoDate: string) {
  return new Intl.DateTimeFormat("ja-JP", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
  }).format(new Date(isoDate));
}

function sessionTime(isoDate: string) {
  return new Intl.DateTimeFormat("ja-JP", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(isoDate));
}

function localDateKey(isoDate: string) {
  const date = new Date(isoDate);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function initials(name: string) {
  return name.trim().slice(0, 1).toUpperCase() || "?";
}

export function WorkHistoryPage() {
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [page, setPage] = useState(0);
  const [history, setHistory] = useState<WorkSessionPage | null>(null);
  const [summary, setSummary] = useState<WorkSessionSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const loadHistory = useCallback(
    (signal: AbortSignal) => {
      const commonFilters = {
        from: filters.from || undefined,
        to: filters.to || undefined,
      };
      return Promise.all([
        fetchWorkSessions(
          {
            ...commonFilters,
            categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
            page,
            size: 20,
          },
          signal,
        ),
        fetchWorkSessionSummary(commonFilters, signal),
      ]);
    },
    [filters, page],
  );

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    loadHistory(controller.signal)
      .then(([nextHistory, nextSummary]) => {
        setHistory(nextHistory);
        setSummary(nextSummary);
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        setError(
          requestError instanceof Error
            ? requestError.message
            : "作業履歴を取得できませんでした。",
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [loadHistory, reloadKey]);

  const groupedSessions = useMemo(() => {
    const groups = new Map<string, WorkSession[]>();
    for (const session of history?.items ?? []) {
      const key = localDateKey(session.startedAt);
      groups.set(key, [...(groups.get(key) ?? []), session]);
    }
    return [...groups.entries()];
  }, [history]);

  const maxCategoryDuration = Math.max(
    1,
    ...(summary?.byCategory.map((item) => item.durationSeconds) ?? []),
  );

  const updateFilter = (key: keyof Filters, value: string) => {
    setPage(0);
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const resetFilters = () => {
    setPage(0);
    setFilters(emptyFilters);
  };

  return (
    <div className="site-shell">
      <header className="topbar">
        <a className="brand" href="/my-page" aria-label="Online Workspace ホーム">
          <span className="brand-mark">OW</span>
          <span>Online Workspace</span>
        </a>
        <a className="back-link" href="/my-page">
          <ArrowLeft size={18} />
          マイページへ戻る
        </a>
      </header>

      <main className="history-page page-width">
        <section className="history-hero">
          <div>
            <p className="section-kicker">WORK HISTORY</p>
            <h1>作業履歴</h1>
            <p className="muted">積み重ねてきた作業時間を、振り返ってみましょう。</p>
          </div>
          <div className="total-time-card">
            <span className="total-time-icon"><Clock3 size={24} /></span>
            <span>
              <small>累計作業時間</small>
              <strong>{summary ? formatDuration(summary.totalDurationSeconds) : "—"}</strong>
            </span>
          </div>
        </section>

        <section className="filters" aria-label="作業履歴の絞り込み">
          <label>
            <span>開始日</span>
            <input
              type="date"
              value={filters.from}
              max={filters.to || undefined}
              onChange={(event) => updateFilter("from", event.target.value)}
            />
          </label>
          <label>
            <span>終了日</span>
            <input
              type="date"
              value={filters.to}
              min={filters.from || undefined}
              onChange={(event) => updateFilter("to", event.target.value)}
            />
          </label>
          <label>
            <span>カテゴリ</span>
            <select
              value={filters.categoryId}
              onChange={(event) => updateFilter("categoryId", event.target.value)}
            >
              <option value="">すべてのカテゴリ</option>
              {summary?.byCategory.map(({ category }) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <button className="reset-button" type="button" onClick={resetFilters}>
            <RotateCcw size={16} />
            リセット
          </button>
        </section>

        {error ? (
          <section className="status-card error-card" role="alert">
            <AlertCircle size={28} />
            <div>
              <h2>履歴を読み込めませんでした</h2>
              <p>{error}</p>
              <button type="button" onClick={() => setReloadKey((key) => key + 1)}>
                再読み込み
              </button>
            </div>
          </section>
        ) : (
          <>
            <section className="category-section" aria-labelledby="category-heading">
              <div className="section-heading">
                <div>
                  <p className="section-kicker">BY CATEGORY</p>
                  <h2 id="category-heading">カテゴリ別の累計</h2>
                </div>
                <BarChart3 size={24} aria-hidden="true" />
              </div>

              {loading && !summary ? (
                <div className="category-grid" aria-label="読み込み中">
                  {[0, 1, 2].map((item) => <div className="category-card skeleton" key={item} />)}
                </div>
              ) : summary?.byCategory.length ? (
                <div className="category-grid">
                  {summary.byCategory.map((item, index) => (
                    <article className="category-card" key={item.category.id}>
                      <div className="category-card-title">
                        <span
                          className="category-dot"
                          style={{ backgroundColor: categoryColors[index % categoryColors.length] }}
                        />
                        <span>{item.category.name}</span>
                      </div>
                      <strong>{formatDuration(item.durationSeconds)}</strong>
                      <div className="category-track" aria-hidden="true">
                        <span
                          style={{
                            backgroundColor: categoryColors[index % categoryColors.length],
                            width: `${Math.max(4, (item.durationSeconds / maxCategoryDuration) * 100)}%`,
                          }}
                        />
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <p className="empty-inline">カテゴリ別の記録はまだありません。</p>
              )}
            </section>

            <section className="timeline-section" aria-labelledby="timeline-heading">
              <div className="section-heading timeline-heading">
                <div>
                  <p className="section-kicker">DAILY ACTIVITY</p>
                  <h2 id="timeline-heading">日ごとの作業</h2>
                </div>
                <span className="record-count">
                  {history?.page.totalElements ?? 0}件の記録
                </span>
              </div>

              {loading && !history ? (
                <div className="timeline-list" aria-label="読み込み中">
                  {[0, 1].map((item) => <div className="session-card skeleton" key={item} />)}
                </div>
              ) : groupedSessions.length ? (
                <div className="timeline-list">
                  {groupedSessions.map(([date, sessions]) => (
                    <section className="day-group" key={date}>
                      <h3><CalendarDays size={18} />{sessionDate(sessions[0].startedAt)}</h3>
                      <div className="day-sessions">
                        {sessions.map((session) => (
                          <article className="session-card" key={session.id}>
                            <div className="session-main">
                              <span className="session-category">{session.category.name}</span>
                              <h4>{session.room.name}</h4>
                              <div className="participant-row">
                                <UsersRound size={17} />
                                <div className="avatars" aria-hidden="true">
                                  {session.participants.slice(0, 4).map((participant) => (
                                    participant.iconUrl ? (
                                      <img key={participant.id} src={participant.iconUrl} alt="" />
                                    ) : (
                                      <span key={participant.id}>{initials(participant.name)}</span>
                                    )
                                  ))}
                                </div>
                                <span>
                                  {session.participants.length
                                    ? session.participants.map((participant) => participant.name).join("、")
                                    : "参加者情報なし"}
                                </span>
                              </div>
                            </div>
                            <div className="session-time">
                              <span>
                                {sessionTime(session.startedAt)} — {session.endedAt ? sessionTime(session.endedAt) : "計測中"}
                              </span>
                              <strong>{formatDuration(session.durationSeconds)}</strong>
                            </div>
                          </article>
                        ))}
                      </div>
                    </section>
                  ))}
                </div>
              ) : (
                <div className="status-card empty-card">
                  <CalendarDays size={32} />
                  <h2>表示できる作業履歴がありません</h2>
                  <p>ルームで作業を終えると、ここに記録が追加されます。</p>
                </div>
              )}

              {history && history.page.totalPages > 1 && (
                <nav className="pagination" aria-label="作業履歴のページ">
                  <button
                    type="button"
                    disabled={history.page.first || loading}
                    onClick={() => setPage((current) => Math.max(0, current - 1))}
                  >
                    <ChevronLeft size={17} />前へ
                  </button>
                  <span>{history.page.page + 1} / {history.page.totalPages}</span>
                  <button
                    type="button"
                    disabled={history.page.last || loading}
                    onClick={() => setPage((current) => current + 1)}
                  >
                    次へ<ChevronRight size={17} />
                  </button>
                </nav>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
