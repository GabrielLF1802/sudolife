import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { SudoButtonComponent } from '../../shared/components/sudo/sudo-button/sudo-button.component';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, SudoButtonComponent],
  templateUrl: './register.component.html',
  styleUrl: './auth-form.component.scss',
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly name = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal('');

  protected register(): void {
    if (this.submitting()) {
      return;
    }

    this.errorMessage.set('');
    this.submitting.set(true);

    this.authService
      .register({ name: this.name().trim(), email: this.email().trim(), password: this.password() })
      .subscribe({
        next: () => void this.router.navigateByUrl('/login'),
        error: () => {
          this.errorMessage.set(
            'Não foi possível criar a conta. Seus dados foram preservados; tente novamente.',
          );
          this.submitting.set(false);
        },
      });
  }
}
