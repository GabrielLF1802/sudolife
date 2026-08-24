import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { PrivacyPolicyComponent } from './core/privacy/privacy-policy.component';
import { PasswordRecoveryCompleteComponent } from './core/auth/password-recovery-complete.component';
import { routes } from './app.routes';

describe('routes', () => {
  it('should_render_privacy_policy_without_auth_guard', async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter(routes)],
    }).compileComponents();

    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/privacy-policy', PrivacyPolicyComponent);

    expect(component).toBeTruthy();
    expect(harness.routeNativeElement?.textContent).toContain('Política de Privacidade');
  });

  it('should_keep_privacy_policy_route_public', () => {
    const privacyRoute = routes.find((route) => route.path === 'privacy-policy');

    expect(privacyRoute?.canActivate).toBeUndefined();
  });

  it('should_keep_password_recovery_complete_route_public', () => {
    const recoveryRoute = routes.find((route) => route.path === 'password-recovery/complete');

    expect(recoveryRoute?.component).toBe(PasswordRecoveryCompleteComponent);
    expect(recoveryRoute?.canActivate).toBeUndefined();
  });
});
