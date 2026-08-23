import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { register: () => undefined },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
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
