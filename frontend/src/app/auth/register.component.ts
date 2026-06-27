import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from './auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
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
    this.errorMessage.set('');
    this.submitting.set(true);

    this.authService
      .register({ name: this.name(), email: this.email(), password: this.password() })
      .subscribe({
        next: () => void this.router.navigateByUrl('/login'),
        error: () => {
          this.errorMessage.set('Nao foi possivel criar a conta.');
          this.submitting.set(false);
        },
      });
  }
}
