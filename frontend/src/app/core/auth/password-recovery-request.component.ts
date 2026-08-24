import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { SudoButtonComponent } from '../../shared/components/sudo/sudo-button/sudo-button.component';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-password-recovery-request',
  imports: [FormsModule, RouterLink, SudoButtonComponent],
  templateUrl: './password-recovery-request.component.html',
  styleUrl: './auth-form.component.scss',
})
export class PasswordRecoveryRequestComponent {

  protected readonly email = signal('');
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly confirmationVisible = signal(false);

  private readonly authService = inject(AuthService);

  protected requestPasswordRecovery(): void {
    if (this.submitting()) {
      return;
    }

    this.errorMessage.set('');
    this.confirmationVisible.set(false);
    this.submitting.set(true);

    this.authService.startPasswordRecovery({ email: this.email().trim() }).subscribe({
      next: () => {
        this.confirmationVisible.set(true);
        this.submitting.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível enviar a solicitação. Revise o email e tente novamente.');
        this.submitting.set(false);
      },
    });
  }
}
