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
});
