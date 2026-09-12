import { useEffect } from 'react';

const EDITABLE_SELECTOR = 'input:not([type="checkbox"]):not([type="radio"]), textarea, select, [contenteditable="true"]';

export function useUsableViewport() {
  useEffect(() => {
    const root = document.documentElement;
    let animationFrame = 0;
    const focusTimers = new Set<number>();
    const layoutObserver = new ResizeObserver(() => synchronize());

    const nativeImeBottom = () => {
      const value = window.getComputedStyle(root).getPropertyValue('--saaf-ime-bottom');
      const pixels = Number.parseFloat(value);
      return Number.isFinite(pixels) ? pixels : 0;
    };

    const usableViewport = () => {
      const visual = window.visualViewport;
      const visualTop = visual?.offsetTop ?? 0;
      const visualHeight = visual?.height ?? window.innerHeight;
      const nativeHeight = Math.max(0, window.innerHeight - nativeImeBottom());
      return { top: visualTop, height: Math.min(visualHeight, nativeHeight || visualHeight) };
    };

    const keepFocusedControlVisible = () => {
      const active = document.activeElement;
      if (!(active instanceof HTMLElement) || !active.matches(EDITABLE_SELECTOR)) return;

      const viewport = usableViewport();
      const viewportTop = viewport.top;
      const viewportBottom = viewportTop + viewport.height;
      const bounds = active.getBoundingClientRect();
      const breathingRoom = 20;
      if (bounds.top < viewportTop + breathingRoom || bounds.bottom > viewportBottom - breathingRoom) {
        active.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' });
      }
    };

    const synchronize = () => {
      cancelAnimationFrame(animationFrame);
      animationFrame = requestAnimationFrame(() => {
        root.style.setProperty('--saaf-visual-viewport-height', `${Math.round(usableViewport().height)}px`);
        const navigation = document.getElementById('app-navigation');
        root.style.setProperty('--saaf-navigation-height', `${navigation?.getBoundingClientRect().height ?? 0}px`);
        root.dataset.viewportReady = 'true';
        keepFocusedControlVisible();
      });
    };

    const onFocus = () => {
      focusTimers.forEach((timer) => window.clearTimeout(timer));
      focusTimers.clear();
      synchronize();
      [100, 300, 600].forEach((delay) => {
        const timer = window.setTimeout(() => {
          focusTimers.delete(timer);
          keepFocusedControlVisible();
        }, delay);
        focusTimers.add(timer);
      });
    };

    const markFormDirty = (event: Event) => {
      const target = event.target;
      if (target instanceof Element) target.closest('form')?.setAttribute('data-saaf-dirty', 'true');
    };

    window.addEventListener('resize', synchronize);
    window.addEventListener('orientationchange', synchronize);
    window.addEventListener('focusin', onFocus);
    window.addEventListener('saafMilkInsetsChanged', synchronize);
    document.addEventListener('input', markFormDirty, true);
    document.addEventListener('change', markFormDirty, true);
    window.visualViewport?.addEventListener('resize', synchronize);
    window.visualViewport?.addEventListener('scroll', synchronize);
    layoutObserver.observe(document.body);
    synchronize();

    return () => {
      cancelAnimationFrame(animationFrame);
      focusTimers.forEach((timer) => window.clearTimeout(timer));
      focusTimers.clear();
      window.removeEventListener('resize', synchronize);
      window.removeEventListener('orientationchange', synchronize);
      window.removeEventListener('focusin', onFocus);
      window.removeEventListener('saafMilkInsetsChanged', synchronize);
      document.removeEventListener('input', markFormDirty, true);
      document.removeEventListener('change', markFormDirty, true);
      window.visualViewport?.removeEventListener('resize', synchronize);
      window.visualViewport?.removeEventListener('scroll', synchronize);
      layoutObserver.disconnect();
      root.style.removeProperty('--saaf-visual-viewport-height');
      root.style.removeProperty('--saaf-navigation-height');
      delete root.dataset.viewportReady;
    };
  }, []);
}
