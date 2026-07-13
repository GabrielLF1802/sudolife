import { convertToParamMap } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';

import { StravaCallbackResultComponent } from './strava-callback-result.component';

describe('StravaCallbackResultComponent', () => {
  it('should_render_success_feedback', async () => {
    const fixture = await createComponent('success', null);

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conexão concluída');
    expect(fixture.nativeElement.textContent).toContain('Sua conta Strava foi conectada.');
  });

  it('should_render_scope_failure_feedback', async () => {
    const fixture = await createComponent('failure', 'INSUFFICIENT_SCOPE');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conexão não concluída');
    expect(fixture.nativeElement.textContent).toContain('Autorize as permissoes solicitadas');
  });

  async function createComponent(
    outcome: string,
    failureCode: string | null,
  ): Promise<ComponentFixture<StravaCallbackResultComponent>> {
    await TestBed.configureTestingModule({
      imports: [StravaCallbackResultComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ outcome, failureCode }),
            },
          },
        },
      ],
    }).compileComponents();

    return TestBed.createComponent(StravaCallbackResultComponent);
  }
});
