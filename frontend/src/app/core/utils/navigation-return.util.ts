import { Router } from '@angular/router';

export function readReturnToFromState(router: Router): string | null {
  const state = (router.getCurrentNavigation()?.extras.state ??
    history.state) as Partial<Record<string, unknown>> | null;
  const returnTo = state?.['returnTo'];
  return typeof returnTo === 'string' && returnTo.trim() ? returnTo : null;
}

export function getPreviousNavigationUrl(
  router: Router,
  options?: {
    excludePrefix?: string;
    ignoreSameAsCurrent?: boolean;
  }
): string | null {
  const currentNavigation = router.getCurrentNavigation();
  const currentUrl = currentNavigation?.finalUrl?.toString() ?? null;
  const previousUrl = currentNavigation?.previousNavigation?.finalUrl?.toString() ?? null;

  if (!previousUrl) {
    return null;
  }

  if (options?.ignoreSameAsCurrent && currentUrl && currentUrl === previousUrl) {
    return null;
  }

  if (options?.excludePrefix && previousUrl.startsWith(options.excludePrefix)) {
    return null;
  }

  return previousUrl;
}
