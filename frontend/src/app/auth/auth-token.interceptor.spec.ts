import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';
import { authTokenInterceptor } from './auth-token.interceptor';

describe('authTokenInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should_add_bearer_token_when_authenticated', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { token: () => 'jwt-token' } },
        provideHttpClient(withInterceptors([authTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);

    httpClient.get('/api/strava/activities').subscribe();

    const request = httpTestingController.expectOne('/api/strava/activities');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    request.flush({});
  });

  it('should_not_add_authorization_header_without_token', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { token: () => null } },
        provideHttpClient(withInterceptors([authTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);

    httpClient.get('/api/users/login').subscribe();

    const request = httpTestingController.expectOne('/api/users/login');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });
});
