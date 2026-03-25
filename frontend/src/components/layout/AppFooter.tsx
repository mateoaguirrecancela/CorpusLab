export function AppFooter() {
  return (
    <footer className="relative z-0 border-t border-[color:var(--cl-line)] bg-white/65">
      <div className="mx-auto flex w-full max-w-6xl flex-col items-center justify-between gap-3 px-6 py-5 text-xs text-[color:var(--cl-secondary)] sm:flex-row">
        <p>© 2026 CorpusLab. All rights reserved.</p>
        <div className="flex items-center gap-6">
          <a href="#" className="hover:text-[color:var(--cl-primary)]">Privacy</a>
          <a href="#" className="hover:text-[color:var(--cl-primary)]">Terms</a>
          <a href="#" className="hover:text-[color:var(--cl-primary)]">Contact</a>
        </div>
      </div>
    </footer>
  )
}