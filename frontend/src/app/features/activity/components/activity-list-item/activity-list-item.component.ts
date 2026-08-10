import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { ActivityDetail } from '../../services/dtos/activity-detail';
import { ActivityListItem } from '../../services/dtos/activity-list';

export interface ActivityListItemOptions {
  activity: ActivityListItem;
  detail: ActivityDetail | null;
  detailError: string;
  detailLoading: boolean;
  open: boolean;
}

@Component({
  selector: 'app-activity-list-item',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './activity-list-item.component.html',
  styleUrl: './activity-list-item.component.scss',
})
export class ActivityListItemComponent {

  readonly options = input.required<ActivityListItemOptions>();
  readonly toggleDetail = output<number>();
  readonly retryDetail = output<number>();

  protected activity(): ActivityListItem {
    return this.options().activity;
  }

  protected detail(): ActivityDetail | null {
    return this.options().detail;
  }

  protected movingTimeLabel(seconds: number): string {
    const totalMinutes = Math.round(seconds / 60);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;

    if (hours > 0) {
      return `${hours}h ${minutes}min`;
    }

    return `${minutes} min`;
  }

  protected paceOrSpeedLabel(activity: ActivityListItem): string {
    if (activity.averagePaceSecondsPerKilometer !== null) {
      const minutes = Math.floor(activity.averagePaceSecondsPerKilometer / 60);
      const seconds = Math.round(activity.averagePaceSecondsPerKilometer % 60)
        .toString()
        .padStart(2, '0');

      return `${minutes}:${seconds} /km`;
    }

    return `${(activity.averageSpeedMetersPerSecond * 3.6).toFixed(1)} km/h`;
  }

  protected activityTypeLabel(activityType: string): string {
    const labels: Record<string, string> = {
      RUN: 'Corrida',
      RIDE: 'Ciclismo',
      WALK: 'Caminhada',
    };

    return labels[activityType] ?? activityType;
  }

  protected performanceDataStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      AVAILABLE: 'Disponíveis',
      UNAVAILABLE: 'Indisponíveis',
      PENDING: 'Sendo preparados',
      FAILED: 'Falharam',
    };

    return labels[status] ?? status;
  }

  protected streamMetricLabel(metric: string): string {
    const labels: Record<string, string> = {
      time: 'Tempo',
      distance: 'Distância',
      heartrate: 'Frequência cardíaca',
      cadence: 'Cadência',
      watts: 'Potência',
      altitude: 'Altitude',
      velocity_smooth: 'Velocidade',
    };

    return labels[metric.toLowerCase()] ?? metric;
  }
}
