import { SessionStatusPanel } from "../components/SessionStatusPanel";

export function SessionStatusPage() {
  return (
    <main className="app-shell">
      <section className="panel">
        <p className="eyebrow">Online Workspace</p>
        <h1>React API entry is ready.</h1>
        <p>
          The React entry screen calls Spring Boot through the same-origin
          proxy.
        </p>
        <SessionStatusPanel />
      </section>
    </main>
  );
}
