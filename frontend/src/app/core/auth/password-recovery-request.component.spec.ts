import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { PasswordRecoveryRequestComponent } from './password-recovery-request.component';

describe('PasswordRecoveryRequestComponent', () => {
  let fixture: ComponentFixture<PasswordRecoveryRequestComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['startPasswordRecovery']);
    authService.startPasswordRecovery.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [PasswordRecoveryRequestComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordRecoveryRequestComponent);
    fixture.detectChanges();
  });

  it('should_show_generic_confirmation_after_request', () => {
    submitEmail('gabriel@sudolife.com');

    const text = fixture.nativeElement.textContent;
    expect(authService.startPasswordRecovery).toHaveBeenCalledWith({ email: 'gabriel@sudolife.com' });
    expect(text).toContain('Se existir uma conta para este email');
    expect(text).not.toContain('email cadastrado');
    expect(text).not.toContain('conta encontrada');
  });

  it('should_not_show_account_existence_message_when_request_fails', () => {
    authService.startPasswordRecovery.and.returnValue(throwError(() => new Error('failed')));

    submitEmail('missing@sudolife.com');

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Não foi possível enviar a solicitação');
    expect(text).not.toContain('email não cadastrado');
    expect(text).not.toContain('conta não encontrada');
  });

  function submitEmail(email: string): void {
    const input = fixture.debugElement.query(By.css('input[name="email"]')).nativeElement;
    input.value = email;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit');
    fixture.detectChanges();
  }
});
