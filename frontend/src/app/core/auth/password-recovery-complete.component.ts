import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
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

interface PasswordPolicyRequirement {
  key: PasswordPolicyViolation;
  label: string;
  satisfied: boolean;
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

  protected readonly newPassword = signal('');
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly passwordPolicyViolations = signal<PasswordPolicyViolation[]>([]);
  protected readonly token = signal(this.route.snapshot.queryParamMap.get('token')?.trim() ?? '');
  protected readonly invalidLinkVisible = signal(!this.token());
  protected readonly passwordPolicyRequirements = computed<PasswordPolicyRequirement[]>(() => {
    const password = this.newPassword();

    return [
      {
        key: 'TOO_SHORT',
        label: 'Pelo menos 12 caracteres',
        satisfied: password.length >= 12,
      },
      {
        key: 'TOO_LONG',
        label: 'No máximo 128 caracteres',
        satisfied: password.length <= 128,
      },
      {
        key: 'MISSING_UPPERCASE',
        label: 'Uma letra maiúscula',
        satisfied: /[A-Z]/.test(password),
      },
      {
        key: 'MISSING_LOWERCASE',
        label: 'Uma letra minúscula',
        satisfied: /[a-z]/.test(password),
      },
      {
        key: 'MISSING_NUMBER',
        label: 'Um número',
        satisfied: /\d/.test(password),
      },
      {
        key: 'MISSING_SPECIAL_CHARACTER',
        label: 'Um caractere especial',
        satisfied: /[^A-Za-z0-9]/.test(password),
      },
    ];
  });

  protected completePasswordRecovery(): void {
    if (this.submitting() || this.invalidLinkVisible()) {
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
      this.errorMessage.set(
        'A nova senha ainda não atende à política de segurança. Revise os itens marcados.',
      );
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
    this.invalidLinkVisible.set(true);
    this.passwordPolicyViolations.set([]);
    this.errorMessage.set('');
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
    if (this.errorMessage()) {
      return 'password-policy-feedback password-recovery-complete-error';
    }

    return 'password-policy-feedback';
  }
}
