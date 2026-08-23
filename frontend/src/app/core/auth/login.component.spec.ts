import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { login: () => undefined },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
  });

  it('should_link_to_privacy_policy', () => {
    const privacyLink = fixture.nativeElement.querySelector('a[href="/privacy-policy"]');

    expect(privacyLink?.textContent).toContain('Política de Privacidade');
  });

  it('should_not_show_coaching_safety_notice', () => {
    const safetyNotice = fixture.nativeElement.querySelector(
      '[aria-label="Coaching Safety Notice"]',
    );

    expect(safetyNotice).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Aviso de segurança do coaching');
  });
});
