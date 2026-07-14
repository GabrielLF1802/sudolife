import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-strava-callback-result',
  imports: [RouterLink],
  templateUrl: './strava-callback-result.component.html',
  styleUrl: './strava-callback-result.component.scss',
})
export class StravaCallbackResultComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly outcome = signal(this.route.snapshot.queryParamMap.get('outcome'));
  protected readonly failureCode = signal(this.route.snapshot.queryParamMap.get('failureCode'));

  protected isSuccess(): boolean {
    return this.outcome() === 'success';
  }

  protected message(): string {
    if (this.isSuccess()) {
      return 'Sua conta Strava foi conectada. Agora você pode sincronizar suas atividades.';
    }

    if (this.failureCode() === 'INSUFFICIENT_SCOPE') {
      return 'Autorize as permissões de atividades no Strava para concluir a conexão e importar seus treinos.';
    }

    if (this.failureCode() === 'DUPLICATE_ATHLETE_OWNERSHIP') {
      return 'Essa conta Strava já está conectada a outro atleta. Volte e conecte uma conta diferente.';
    }

    return 'Não foi possível conectar sua conta Strava. Volte ao painel e tente novamente.';
  }
}
