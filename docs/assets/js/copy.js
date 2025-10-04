document.addEventListener('DOMContentLoaded', function(){
  document.querySelectorAll('pre').forEach(pre => {
    // add button container
    const btn = document.createElement('button');
    btn.className = 'copy-btn';
    btn.type = 'button';
    btn.innerText = 'Copy';
    btn.addEventListener('click', () => {
      const text = pre.innerText;
      navigator.clipboard.writeText(text).then(() => {
        btn.innerText = 'Copied';
        setTimeout(()=> btn.innerText = 'Copy', 1500);
      });
    });
    const toolbar = document.createElement('div');
    toolbar.className = 'code-toolbar';
    toolbar.appendChild(btn);
    pre.style.position = 'relative';
    pre.appendChild(toolbar);
  });
});
