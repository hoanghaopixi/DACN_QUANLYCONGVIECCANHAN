import { flushSync } from 'react-dom';

type ViewTransitionDocument = Document & {
  startViewTransition?: (update: () => void) => { finished: Promise<void> };
};

export function runViewTransition(update: () => void): void {
  if (typeof document === 'undefined') {
    update();
    return;
  }

  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const startViewTransition = (document as ViewTransitionDocument).startViewTransition;
  if (!startViewTransition || reducedMotion) {
    update();
    return;
  }

  try {
    startViewTransition.call(document, () => flushSync(update));
  } catch {
    update();
  }
}
