import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { ActivityList, ActivityListItem } from './activity.service';
import {
  CoachingProfile,
  ConservativeRunningPlan,
  PlannedSession,
  RunningGoalAssessment,
  RunningGoalAssessmentReason,
  UserReportedReadiness,
} from './coaching-profile.service';

interface WeekDay {
  date: Date;
  isToday: boolean;
  activities: ActivityListItem[];
  plannedSessions: PlannedSession[];
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
  readonly goalAssessment = input<RunningGoalAssessment | null>(null);
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
        plannedSessions:
          this.plan()?.plannedSessions.filter(
            (session) => session.scheduledDate === this.dateValue(date),
          ) ?? [],
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
    if (session.type === 'RECOVERY') {
      return 'Sessão de recuperação';
    }

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
      .map((reason) => {
        if (reason === 'INJURY_CONCERN') {
          return 'a preocupação de lesão informada';
        }

        return reason === 'LOW_READINESS' ? 'prontidão baixa' : 'histórico recente insuficiente';
      })
      .join(' e ');
  }

  protected paceLabel(secondsPerKilometer: number | null): string {
    if (secondsPerKilometer === null) {
      return 'ritmo livre';
    }

    const minutes = Math.floor(secondsPerKilometer / 60);
    const seconds = (secondsPerKilometer % 60).toString().padStart(2, '0');

    return `${minutes}:${seconds} /km`;
  }

  protected goalReasonLabel(reason: RunningGoalAssessmentReason): string {
    const labels: Record<RunningGoalAssessmentReason, string> = {
      UNREALISTIC_DISTANCE: 'distância acima da progressão segura',
      UNREALISTIC_PACE: 'ritmo mais rápido que o histórico atual permite',
      UNREALISTIC_TARGET_DATE: 'prazo curto para uma progressão segura',
    };

    return labels[reason];
  }

  protected goalDateLabel(date: string | null): string {
    if (date === null) {
      return 'sem data definida';
    }

    const [year, month, day] = date.split('-');

    return `${day}/${month}/${year}`;
  }

  protected plannedSessionDateLabel(session: PlannedSession): string {
    const [year, month, day] = session.scheduledDate.split('-');

    return `${day}/${month}/${year}`;
  }

  private startOfDay(date: Date): Date {
    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);

    return startOfDay;
  }

  private dateValue(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
