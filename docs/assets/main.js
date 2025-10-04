// Mermaid init
if (window.mermaid) {
  mermaid.initialize({ startOnLoad: true, securityLevel: 'strict' });
}

function addCopyButtons() {
  document.querySelectorAll('pre > code').forEach(codeBlock => {
    const pre = codeBlock.parentNode;
    if (pre.querySelector('.copy-btn')) return;
    const button = document.createElement('button');
    button.className = 'copy-btn';
    button.type = 'button';
    button.setAttribute('aria-label', 'Copier le code');
    button.textContent = 'COPIER';
    button.addEventListener('click', () => {
      navigator.clipboard.writeText(codeBlock.textContent).then(() => {
        const original = button.textContent;
        button.textContent = 'OK';
        button.disabled = true;
        setTimeout(() => { button.textContent = original; button.disabled = false; }, 1300);
      });
    });
    pre.appendChild(button);
    pre.style.position = 'relative';
  });
}

function applyActiveNav() {
  const path = window.location.pathname.replace(/\/index\.html$/, '/');
  document.querySelectorAll('nav.sidebar a').forEach(a => {
    const href = a.getAttribute('href');
    if (!href) return;
    // Normalize
    let target = href;
    if (!target.startsWith('http') && !target.startsWith('/')) {
      target = '/' + target;
    }
    if (path.endsWith(target) || path === target) {
      a.classList.add('active');
    }
  });
}

function initTheme() {
  const root = document.documentElement;
  const btn = document.getElementById('themeToggle');
  const stored = localStorage.getItem('docs-theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  let theme = stored || (prefersDark ? 'dark' : 'light');
  root.setAttribute('data-theme', theme);
  updateThemeButton(btn, theme);
  btn.addEventListener('click', () => {
    theme = (root.getAttribute('data-theme') === 'dark') ? 'light' : 'dark';
    root.setAttribute('data-theme', theme);
    localStorage.setItem('docs-theme', theme);
    updateThemeButton(btn, theme);
  });
}

function updateThemeButton(btn, theme) {
  if (!btn) return;
  if (theme === 'dark') {
    btn.textContent = 'Mode clair';
    btn.setAttribute('aria-pressed', 'true');
  } else {
    btn.textContent = 'Mode sombre';
    btn.setAttribute('aria-pressed', 'false');
  }
}

document.addEventListener('DOMContentLoaded', () => {
  addCopyButtons();
  applyActiveNav();
  const toggle = document.getElementById('themeToggle');
  if (toggle) initTheme();
});
