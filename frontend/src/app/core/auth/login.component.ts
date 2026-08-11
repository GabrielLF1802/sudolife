import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { SudoButtonComponent } from '../../shared/components/sudo/sudo-button/sudo-button.component';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, SudoButtonComponent],
  templateUrl: './login.component.html',
  styleUrl: './auth-form.component.scss',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');

  protected login(): void {
    if (this.submitting()) {
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);

    this.authService.login({ email: this.email().trim(), password: this.password() }).subscribe({
      next: () => void this.router.navigateByUrl('/activities'),
      error: () => {
        this.errorMessage.set('Email ou senha inválidos. Revise os dados e tente novamente.');
        this.submitting.set(false);
      },
    });
  }
}
