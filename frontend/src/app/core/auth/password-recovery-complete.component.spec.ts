import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { PasswordRecoveryCompleteComponent } from './password-recovery-complete.component';

describe('PasswordRecoveryCompleteComponent', () => {
  let fixture: ComponentFixture<PasswordRecoveryCompleteComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['completePasswordRecovery']);
    authService.completePasswordRecovery.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [PasswordRecoveryCompleteComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ token: 'raw-token' }) } },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture = TestBed.createComponent(PasswordRecoveryCompleteComponent);
    fixture.detectChanges();
  });

  it('should_complete_recovery_and_return_to_login_without_storing_token', () => {
    localStorage.clear();

    submitPassword('Valid!Password1');

    expect(authService.completePasswordRecovery).toHaveBeenCalledWith({
      token: 'raw-token',
      newPassword: 'Valid!Password1',
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login', {
      state: { passwordRecoveryCompleted: true },
    });
    expect(localStorage.getItem('sudolife.jwt')).toBeNull();
  });

  it('should_show_password_policy_failure_details', () => {
    authService.completePasswordRecovery.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {
        code: 'PASSWORD_POLICY_VIOLATION',
        message: 'Senha inválida',
        violations: ['TOO_SHORT', 'MISSING_UPPERCASE'],
      },
    })));

    submitPassword('weak');

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Sua senha ainda não atende à política de segurança');
    expect(text).toContain('Use pelo menos 12 caracteres');
    expect(text).toContain('Inclua uma letra maiúscula');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should_show_invalid_link_failure_state', () => {
    authService.completePasswordRecovery.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {
        code: 'INVALID_PASSWORD_RECOVERY_TOKEN',
        message: 'Link inválido',
      },
    })));

    submitPassword('Valid!Password1');

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Este link é inválido ou expirou');
    expect(text).toContain('Solicitar novo link');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('should_disable_submit_when_link_has_no_token', async () => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [PasswordRecoveryCompleteComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordRecoveryCompleteComponent);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    const button = fixture.nativeElement.querySelector('button');
    expect(text).toContain('Link inválido');
    expect(button.disabled).toBeTrue();
  });

  function submitPassword(password: string): void {
    const input = fixture.debugElement.query(By.css('input[name="newPassword"]')).nativeElement;
    input.value = password;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit');
    fixture.detectChanges();
  }
});
