import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { ActivityList, ActivityListItem } from './activity.service';
import {
  CoachingProfile,
  ConservativeRunningPlan,
  PlannedSession,
  UserReportedReadiness,
} from './coaching-profile.service';

interface WeekDay {
  date: Date;
  isToday: boolean;
  activities: ActivityListItem[];
}

@Component({
  selector: 'app-weekly-rhythm',
  imports: [DecimalPipe],
  templateUrl: './weekly-rhythm.component.html',
  styleUrl: './weekly-rhythm.component.scss',
})
export class WeeklyRhythmComponent {
  readonly activityList = input.required<ActivityList>();
  readonly coachingProfile = input.required<CoachingProfile>();
  readonly plan = input<ConservativeRunningPlan | null>(null);
  private readonly currentDate = new Date();

  protected readonly weekDays = computed<WeekDay[]>(() => {
    const today = this.startOfDay(this.currentDate);
    const monday = new Date(today);
    monday.setDate(today.getDate() - ((today.getDay() + 6) % 7));

    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(monday);
      date.setDate(monday.getDate() + index);

      return {
        date,
        isToday: date.getTime() === today.getTime(),
        activities: this.activityList().activities.filter(
          (activity) => this.startOfDay(new Date(activity.startDate)).getTime() === date.getTime(),
        ),
      };
    });
  });
  protected readonly currentWeekPlanSessions = computed(
    () => this.plan()?.plannedSessions.filter((session) => session.weekNumber === 1) ?? [],
  );

  protected readinessLabel(readiness: UserReportedReadiness | null): string {
    if (readiness === 'LOW') {
      return 'Baixa';
    }

    if (readiness === 'MODERATE') {
      return 'Moderada';
    }

    if (readiness === 'HIGH') {
      return 'Alta';
    }

    return 'Não informada';
  }

  protected plannedSessionTypeLabel(session: PlannedSession): string {
    return session.type === 'EASY_RUN' ? 'Corrida leve' : 'Corrida longa';
  }

  protected plannedSessionTargetLabel(session: PlannedSession): string {
    if (session.target.type === 'HEART_RATE') {
      return `${session.target.minimumHeartRate}-${session.target.maximumHeartRate} bpm`;
    }

    return `Esforço percebido ${session.target.minimumPerceivedEffort}-${session.target.maximumPerceivedEffort}`;
  }

  protected weekDayLabel(date: Date): string {
    return ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'][date.getDay()];
  }

  protected weekRangeLabel(): string {
    const days = this.weekDays();
    const format = (date: Date) =>
      date.toLocaleDateString('pt-BR', { day: 'numeric', month: 'short' });

    return `${format(days[0].date)}–${format(days[6].date)}`;
  }

  protected planReasonLabel(plan: ConservativeRunningPlan): string {
    return plan.reasons
      .map((reason) =>
        reason === 'LOW_READINESS' ? 'prontidão baixa' : 'histórico recente insuficiente',
      )
      .join(' e ');
  }

  private startOfDay(date: Date): Date {
    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);

    return startOfDay;
  }
}
