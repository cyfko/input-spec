// Enable Mermaid diagrams
if (window.mermaid) {
  mermaid.initialize({ startOnLoad: true });
}
// Add copy button to all code blocks
function addCopyButtons() {
  document.querySelectorAll('pre > code').forEach(function(codeBlock) {
    var pre = codeBlock.parentNode;
    if (pre.querySelector('.copy-btn')) return;
    var button = document.createElement('button');
    button.className = 'copy-btn';
    button.textContent = 'Copier';
    button.addEventListener('click', function() {
      navigator.clipboard.writeText(codeBlock.textContent);
      button.textContent = 'Copié!';
      setTimeout(function() { button.textContent = 'Copier'; }, 1200);
    });
    pre.appendChild(button);
    pre.style.position = 'relative';
  });
}
document.addEventListener('DOMContentLoaded', addCopyButtons);
