import { Route } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { routes } from './app.routes';

function flatten(list: Route[]): Route[] {
  return list.flatMap((route) => [route, ...flatten(route.children ?? [])]);
}

/** Every lazy route resolves to its component, so a broken import shows up here and not in the browser. */
describe('app routes', () => {
  it('loads the component of every lazy route', async () => {
    const lazy = flatten(routes).filter((route) => route.loadComponent);
    expect(lazy.length).toBeGreaterThan(10);
    for (const route of lazy) {
      const component = await (route.loadComponent as () => Promise<unknown>)();
      expect(component, route.path).toBeTruthy();
    }
  });

  it('keeps the institutional account return before the personal link', () => {
    const paths = flatten(routes).map((route) => route.path);
    expect(paths.indexOf('votar/sso')).toBeLessThan(paths.indexOf('votar/:linkToken'));
  });
});
