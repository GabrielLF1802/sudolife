import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { ActivityDashboardComponent } from './activity-dashboard.component';
import { ActivityService } from './activity.service';
import { StravaAccountService } from './strava-account.service';

describe('ActivityDashboardComponent', () => {
  let fixture: ComponentFixture<ActivityDashboardComponent>;
  let stravaAccountService: jasmine.SpyObj<StravaAccountService>;

  beforeEach(async () => {
    stravaAccountService = jasmine.createSpyObj<StravaAccountService>('StravaAccountService', [
      'status',
      'startLinking',
    ]);
    stravaAccountService.status.and.returnValue(
      of({ linked: false, athleteId: null, permissionState: 'UNLINKED' }),
    );
    stravaAccountService.startLinking.and.returnValue(
      of({ authorizationUrl: 'https://strava.example/oauth' }),
    );

    await TestBed.configureTestingModule({
      imports: [ActivityDashboardComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            currentUser: () => of({ id: 1, name: 'Gabriel', email: 'gabriel@example.com' }),
            logout: () => undefined,
          },
        },
        {
          provide: ActivityService,
          useValue: {
            list: () =>
              of({
                activities: [],
                page: 0,
                size: 10,
                totalElements: 0,
                totalPages: 0,
              }),
          },
        },
        { provide: StravaAccountService, useValue: stravaAccountService },
        { provide: Router, useValue: { navigateByUrl: () => Promise.resolve(true) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityDashboardComponent);
  });

  it('should_render_connect_action_when_strava_is_unlinked', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nao conectado');
    expect(fixture.nativeElement.textContent).toContain('Conectar Strava');
  });

  it('should_render_reconnect_action_when_strava_is_linked', () => {
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'READY' }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Conectado ao atleta 123');
    expect(fixture.nativeElement.textContent).toContain('Reconectar Strava');
  });

  it('should_render_permission_upgrade_action_when_scope_is_incomplete', () => {
    stravaAccountService.status.and.returnValue(
      of({ linked: true, athleteId: 123, permissionState: 'PERMISSION_UPGRADE_REQUIRED' }),
    );
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Permissoes incompletas');
    expect(fixture.nativeElement.textContent).toContain('Atualizar permissoes');
  });

  it('should_show_linking_error_when_oauth_launch_fails', () => {
    stravaAccountService.startLinking.and.returnValue(throwError(() => new Error('failed')));
    fixture.detectChanges();

    stravaButton().click();
    fixture.detectChanges();

    expect(stravaAccountService.startLinking).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Nao foi possivel iniciar a conexao com o Strava.',
    );
  });

  function stravaButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.strava-action');
  }
});
