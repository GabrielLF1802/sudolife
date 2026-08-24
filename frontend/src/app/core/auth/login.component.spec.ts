import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Navigation, Router, provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { login: () => undefined },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should_show_account_deletion_confirmation_when_redirect_state_is_present', () => {
    spyOn(router, 'getCurrentNavigation').and.returnValue({
      extras: { state: { accountDeletionConfirmed: true } },
    } as unknown as Navigation);
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const confirmation = fixture.nativeElement.querySelector('.confirmation-message');

    expect(confirmation?.textContent).toContain('Conta excluída');
    expect(confirmation?.textContent).toContain(
      'dados armazenados localmente pela Sudolife foram removidos',
    );
    expect(confirmation?.textContent).not.toContain('Strava');
  });

  it('should_show_password_recovery_confirmation_when_redirect_state_is_present', () => {
    spyOn(router, 'getCurrentNavigation').and.returnValue({
      extras: { state: { passwordRecoveryCompleted: true } },
    } as unknown as Navigation);
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const confirmation = fixture.nativeElement.querySelector('.confirmation-message');

    expect(confirmation?.textContent).toContain('Senha alterada');
    expect(confirmation?.textContent).toContain('Entre com sua nova senha');
  });

  it('should_link_to_privacy_policy', () => {
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const privacyLink = fixture.nativeElement.querySelector('a[href="/privacy-policy"]');

    expect(privacyLink?.textContent).toContain('Política de Privacidade');
  });

  it('should_not_show_account_deletion_confirmation_on_normal_login_visit', () => {
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const confirmation = fixture.nativeElement.querySelector('.confirmation-message');

    expect(confirmation).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Conta excluída');
  });

  it('should_not_show_coaching_safety_notice', () => {
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    const safetyNotice = fixture.nativeElement.querySelector(
      '[aria-label="Coaching Safety Notice"]',
    );

    expect(safetyNotice).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Aviso de segurança do coaching');
  });
});
