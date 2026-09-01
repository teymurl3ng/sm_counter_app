import { useEffect } from 'react';

const EDITABLE_SELECTOR = 'input:not([type="checkbox"]):not([type="radio"]), textarea, select, [contenteditable="true"]';

export function useUsableViewport() {
  useEffect(() => {
    const root = document.documentElement;
    let animationFrame = 0;
    let focusTimer = 0;

    const keepFocusedControlVisible = () => {
      const active = document.activeElement;
      if (!(active instanceof HTMLElement) || !active.matches(EDITABLE_SELECTOR)) return;

      const viewport = window.visualViewport;
      const viewportTop = viewport?.offsetTop ?? 0;
      const viewportBottom = viewportTop + (viewport?.height ?? window.innerHeight);
      const bounds = active.getBoundingClientRect();
      const breathingRoom = 20;
      if (bounds.top < viewportTop + breathingRoom || bounds.bottom > viewportBottom - breathingRoom) {
        active.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'smooth' });
      }
    };

    const synchronize = () => {
      cancelAnimationFrame(animationFrame);
      animationFrame = requestAnimationFrame(() => {
        const height = window.visualViewport?.height ?? window.innerHeight;
        root.style.setProperty('--saaf-visual-viewport-height', `${Math.round(height)}px`);
        root.dataset.viewportReady = 'true';
        keepFocusedControlVisible();
      });
    };

    const onFocus = () => {
      window.clearTimeout(focusTimer);
      synchronize();
      focusTimer = window.setTimeout(keepFocusedControlVisible, 300);
    };

    window.addEventListener('resize', synchronize);
    window.addEventListener('orientationchange', synchronize);
    window.addEventListener('focusin', onFocus);
    window.visualViewport?.addEventListener('resize', synchronize);
    window.visualViewport?.addEventListener('scroll', synchronize);
    synchronize();

    return () => {
      cancelAnimationFrame(animationFrame);
      window.clearTimeout(focusTimer);
      window.removeEventListener('resize', synchronize);
      window.removeEventListener('orientationchange', synchronize);
      window.removeEventListener('focusin', onFocus);
      window.visualViewport?.removeEventListener('resize', synchronize);
      window.visualViewport?.removeEventListener('scroll', synchronize);
      root.style.removeProperty('--saaf-visual-viewport-height');
      delete root.dataset.viewportReady;
    };
  }, []);
}
