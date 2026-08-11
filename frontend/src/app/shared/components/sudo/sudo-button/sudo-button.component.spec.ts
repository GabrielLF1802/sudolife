import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SudoButtonComponent } from './sudo-button.component';

@Component({
  imports: [SudoButtonComponent],
  template: `
    <sudo-button type="submit" variant="primary" size="large" [disabled]="disabled" [busy]="busy">
      Salvar
    </sudo-button>
  `,
})
class SudoButtonHostComponent {

  disabled = false;
  busy = false;
}

describe('SudoButtonComponent', () => {
  let fixture: ComponentFixture<SudoButtonHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SudoButtonHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SudoButtonHostComponent);
  });

  it('should_render_projected_content_and_button_contract', () => {
    fixture.detectChanges();

    const button = nativeButton();

    expect(button.type).toBe('submit');
    expect(button.classList).toContain('primary');
    expect(button.classList).toContain('large');
    expect(button.textContent?.trim()).toBe('Salvar');
  });

  it('should_disable_button_when_disabled', () => {
    fixture.componentInstance.disabled = true;
    fixture.detectChanges();

    expect(nativeButton().disabled).toBeTrue();
  });

  it('should_mark_busy_button_as_disabled_and_busy', () => {
    fixture.componentInstance.busy = true;
    fixture.detectChanges();

    const button = nativeButton();

    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-busy')).toBe('true');
  });

  function nativeButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button');
  }
});
