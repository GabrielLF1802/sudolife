import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PrivacyPolicyComponent } from './privacy-policy.component';

describe('PrivacyPolicyComponent', () => {
  let fixture: ComponentFixture<PrivacyPolicyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivacyPolicyComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PrivacyPolicyComponent);
    fixture.detectChanges();
  });

  it('should_render_portuguese_content_before_english_content', () => {
    const text = fixture.nativeElement.textContent;

    expect(text.indexOf('Português')).toBeLessThan(text.indexOf('English'));
    expect(text).toContain('dados do perfil do atleta');
    expect(text).toContain('Performance Streams');
    expect(text).toContain('não são usados para treinar modelos');
  });

  it('should_explain_account_deletion_confirmation', () => {
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Account-Owned Data');
    expect(text).toContain('confirmação de exclusão da conta é exibida na tela');
    expect(text).toContain('Account Deletion Confirmation is shown on screen');
  });
});
