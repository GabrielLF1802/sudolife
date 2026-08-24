import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let authService: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });

    authService = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.clear();
  });

  it('should_register_an_athlete', () => {
    const command = { name: 'Gabriel', email: 'gabriel@example.com', password: 'secret123' };

    authService.register(command).subscribe();

    const request = httpTestingController.expectOne('/api/users/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush(null);
  });

  it('should_persist_token_after_login', () => {
    const command = { email: 'gabriel@example.com', password: 'secret123' };

    authService.login(command).subscribe();

    const request = httpTestingController.expectOne('/api/users/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush({ token: 'jwt-token' });

    expect(authService.token()).toBe('jwt-token');
    expect(authService.isAuthenticated()).toBeTrue();
  });

  it('should_start_password_recovery_without_storing_token', () => {
    const command = { email: 'gabriel@example.com' };

    authService.startPasswordRecovery(command).subscribe();

    const request = httpTestingController.expectOne('/api/auth/password-recovery');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush({ message: 'generic' });

    expect(authService.token()).toBeNull();
  });

  it('should_remove_token_after_logout', () => {
    localStorage.setItem('sudolife.jwt', 'jwt-token');

    authService.logout();

    expect(authService.token()).toBeNull();
    expect(authService.isAuthenticated()).toBeFalse();
  });

  it('should_send_password_change_request_and_remove_token_after_success', () => {
    localStorage.setItem('sudolife.jwt', 'jwt-token');
    const command = {
      currentPassword: 'Str0ng!Password',
      newPassword: 'An0ther!Password',
    };

    authService.changePassword(command).subscribe();

    const request = httpTestingController.expectOne('/api/users/me/password');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(command);
    request.flush(null);

    expect(authService.token()).toBeNull();
    expect(authService.isAuthenticated()).toBeFalse();
  });

  it('should_send_account_deletion_request_and_remove_token_after_success', () => {
    localStorage.setItem('sudolife.jwt', 'jwt-token');
    const command = { currentPassword: 'Str0ng!Password' };

    authService.deleteAccount(command).subscribe();

    const request = httpTestingController.expectOne('/api/users/me');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.body).toEqual(command);
    request.flush(null);

    expect(authService.token()).toBeNull();
    expect(authService.isAuthenticated()).toBeFalse();
  });
});
