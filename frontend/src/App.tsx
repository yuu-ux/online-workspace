export function App() {
  return (
    <main className="app-shell">
      <section className="panel">
        <p className="eyebrow">Online Workspace</p>
        <h1>Frontend dev server is ready.</h1>
        <p>
          Vite runs behind the development proxy. The Spring Boot backend is
          available on port 8080, and API requests under <code>/api</code> are
          proxied to it.
        </p>
        <div className="links">
          <a href="http://localhost:8080">Current MVC app</a>
          <a href="http://localhost:1080">MailDev</a>
        </div>
      </section>
    </main>
  );
}
