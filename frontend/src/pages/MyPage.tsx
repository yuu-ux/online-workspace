import { ArrowRight, BarChart3, Clock3, UserRound } from "lucide-react";
import { Link } from "react-router-dom";

export function MyPage() {
  return (
    <div className="site-shell">
      <header className="topbar">
        <Link className="brand" to="/my-page" aria-label="Online Workspace ホーム">
          <span className="brand-mark">OW</span>
          <span>Online Workspace</span>
        </Link>
      </header>

      <main className="my-page page-width">
        <section className="profile-card">
          <div className="profile-icon" aria-hidden="true">
            <UserRound size={32} />
          </div>
          <div>
            <p className="section-kicker">MY PAGE</p>
            <h1>マイページ</h1>
            <p className="muted">
              プロフィールや、これまでのオンライン作業の記録を確認できます。
            </p>
          </div>
        </section>

        <section aria-labelledby="activity-heading">
          <div className="section-heading">
            <div>
              <p className="section-kicker">ACTIVITY</p>
              <h2 id="activity-heading">作業記録</h2>
            </div>
          </div>

          <Link className="history-link-card" to="/work-history">
            <span className="history-link-icon">
              <BarChart3 size={26} />
            </span>
            <span className="history-link-copy">
              <strong>作業履歴を見る</strong>
              <span>累計時間、カテゴリ別の内訳、日ごとの活動を確認</span>
            </span>
            <span className="history-link-meta">
              <Clock3 size={18} />
              これまでの記録
            </span>
            <ArrowRight className="history-link-arrow" size={22} aria-hidden="true" />
          </Link>
        </section>
      </main>
    </div>
  );
}
