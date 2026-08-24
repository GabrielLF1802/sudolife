import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { SudoButtonComponent } from '../../shared/components/sudo/sudo-button/sudo-button.component';
import { AuthService } from './auth.service';

type PasswordPolicyViolation =
  | 'BLANK'
  | 'TOO_SHORT'
  | 'TOO_LONG'
  | 'MISSING_UPPERCASE'
  | 'MISSING_LOWERCASE'
  | 'MISSING_NUMBER'
  | 'MISSING_SPECIAL_CHARACTER'
  | 'CONTAINS_CONTEXTUAL_DATA';

interface PasswordPolicyErrorResponse {
  code: 'PASSWORD_POLICY_VIOLATION';
  message: string;
  violations: PasswordPolicyViolation[];
}

interface ErrorResponse {
  code: string;
  message: string;
}

@Component({
  selector: 'app-password-recovery-complete',
  imports: [FormsModule, RouterLink, SudoButtonComponent],
  templateUrl: './password-recovery-complete.component.html',
  styleUrl: './auth-form.component.scss',
})
export class PasswordRecoveryCompleteComponent {

  private static readonly passwordRecoveryConfirmationStateKey = 'passwordRecoveryCompleted';

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly passwordPolicyMessages: Record<PasswordPolicyViolation, string> = {
    BLANK: 'Informe uma nova senha.',
    TOO_SHORT: 'Use pelo menos 12 caracteres.',
    TOO_LONG: 'Use no máximo 128 caracteres.',
    MISSING_UPPERCASE: 'Inclua uma letra maiúscula.',
    MISSING_LOWERCASE: 'Inclua uma letra minúscula.',
    MISSING_NUMBER: 'Inclua um número.',
    MISSING_SPECIAL_CHARACTER: 'Inclua um caractere especial.',
    CONTAINS_CONTEXTUAL_DATA: 'Evite usar seu nome ou email na senha.',
  };

  protected readonly newPassword = signal('');
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly passwordPolicyViolations = signal<PasswordPolicyViolation[]>([]);
  protected readonly token = signal(this.route.snapshot.queryParamMap.get('token')?.trim() ?? '');

  protected completePasswordRecovery(): void {
    if (this.submitting() || !this.token()) {
      this.showInvalidLink();
      return;
    }

    this.errorMessage.set('');
    this.passwordPolicyViolations.set([]);
    this.submitting.set(true);

    this.authService.completePasswordRecovery({
      token: this.token(),
      newPassword: this.newPassword(),
    }).subscribe({
      next: () => void this.router.navigateByUrl('/login', {
        state: { [PasswordRecoveryCompleteComponent.passwordRecoveryConfirmationStateKey]: true },
      }),
      error: (error: HttpErrorResponse) => this.handleRecoveryFailure(error),
    });
  }

  private handleRecoveryFailure(error: HttpErrorResponse): void {
    this.submitting.set(false);

    if (this.isPasswordPolicyError(error.error)) {
      this.errorMessage.set('Sua senha ainda não atende à política de segurança.');
      this.passwordPolicyViolations.set(error.error.violations);
      return;
    }

    if (this.isInvalidTokenError(error.error)) {
      this.showInvalidLink();
      return;
    }

    this.errorMessage.set('Não foi possível alterar sua senha. Tente novamente em instantes.');
  }

  private showInvalidLink(): void {
    this.submitting.set(false);
    this.passwordPolicyViolations.set([]);
    this.errorMessage.set('Este link é inválido ou expirou. Solicite uma nova recuperação de senha.');
  }

  private isPasswordPolicyError(error: unknown): error is PasswordPolicyErrorResponse {
    return this.isErrorResponse(error)
      && error.code === 'PASSWORD_POLICY_VIOLATION'
      && Array.isArray((error as PasswordPolicyErrorResponse).violations);
  }

  private isInvalidTokenError(error: unknown): error is ErrorResponse {
    return this.isErrorResponse(error) && error.code === 'INVALID_PASSWORD_RECOVERY_TOKEN';
  }

  private isErrorResponse(error: unknown): error is ErrorResponse {
    return typeof error === 'object'
      && error !== null
      && 'code' in error
      && 'message' in error;
  }

  protected passwordDescribedBy(): string | null {
    if (this.errorMessage() && this.passwordPolicyViolations().length) {
      return 'password-recovery-complete-error password-policy-errors';
    }

    if (this.errorMessage()) {
      return 'password-recovery-complete-error';
    }

    if (this.passwordPolicyViolations().length) {
      return 'password-policy-errors';
    }

    return null;
  }
}
