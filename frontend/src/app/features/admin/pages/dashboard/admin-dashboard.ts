import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminSidebarComponent } from '../../components/sidebar/sidebar';
import { AdminService } from '../../../../core/services/admin.service';
import { AdminSpider, AdminStats } from '../../../../core/models/admin.models';

interface Kpi {
  label: string;
  num: string;
  delta: string;
  up: boolean;
  color: string;
  spark: number[];
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [AdminSidebarComponent, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard implements OnInit {
  private readonly adminSvc = inject(AdminService);

  kpis = signal<Kpi[]>([]);
  spiders = signal<AdminSpider[]>([]);
  loading = signal(true);

  modes = [
    { mode: 'Supervivencia',   pct: 42, color: 'var(--vs-accent-red)'    },
    { mode: 'Precisión',       pct: 24, color: 'var(--vs-accent-blue)'   },
    { mode: 'Duelo binario',   pct: 16, color: 'var(--vs-accent-gold)'   },
    { mode: 'Sabotaje',        pct: 12, color: 'var(--vs-accent-purple)' },
    { mode: 'Duelo precisión', pct:  6, color: 'var(--vs-accent-green)'  },
  ];

  ngOnInit(): void {
    this.adminSvc.getStats().subscribe({
      next: (s) => {
        this.kpis.set(this.buildKpis(s));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.adminSvc.getSpiders().subscribe({
      next: (list) => this.spiders.set(list),
    });
  }

  private buildKpis(s: AdminStats): Kpi[] {
    return [
      {
        label: 'Usuarios activos',
        num: s.activeUsers.toLocaleString('es-ES'),
        delta: `${s.totalUsers.toLocaleString('es-ES')} en total`,
        up: true,
        color: 'var(--vs-accent-green)',
        spark: [10, 12, 11, 14, 13, 17, 19, 18, 22, 24],
      },
      {
        label: 'Partidas hoy',
        num: s.gamesToday.toLocaleString('es-ES'),
        delta: 'partidas hoy',
        up: true,
        color: 'var(--vs-accent-blue)',
        spark: [40, 38, 52, 49, 61, 58, 73],
      },
      {
        label: 'Preguntas en BD',
        num: s.totalQuestions.toLocaleString('es-ES'),
        delta: 'preguntas activas',
        up: true,
        color: 'var(--vs-accent-gold)',
        spark: [20, 22, 24, 26, 28, 30, 33],
      },
      {
        label: 'Reportes pendientes',
        num: s.pendingReports.toLocaleString('es-ES'),
        delta: s.pendingReports > 0 ? 'requieren revisión' : 'sin reportes',
        up: s.pendingReports === 0,
        color: 'var(--vs-accent-red)',
        spark: [3, 5, 4, 8, 12, 18, s.pendingReports],
      },
    ];
  }

  pillClass(status: string): string {
    return (
      { IDLE: 'vs-pill--mute', RUNNING: 'vs-pill--info', FAILED: 'vs-pill--err' }[
        status
      ] ?? 'vs-pill--mute'
    );
  }

  pillLabel(status: string): string {
    return (
      { IDLE: 'INACTIVA', RUNNING: 'EJECUTANDO', FAILED: 'CAÍDA' }[status] ??
      status
    );
  }

  dotClass(status: string): string {
    return (
      { IDLE: 'idle', RUNNING: 'ok', FAILED: 'err' }[status] ?? 'idle'
    );
  }

  lastRunLabel(spider: AdminSpider): string {
    if (!spider.lastRunAt) return 'Sin ejecución';
    const d = new Date(spider.lastRunAt);
    const diff = Math.floor((Date.now() - d.getTime()) / 60000);
    if (diff < 60) return `hace ${diff} min`;
    const h = Math.floor(diff / 60);
    return `hace ${h} h`;
  }

  sparkPoints(data: number[]): string {
    const max = Math.max(...data),
      min = Math.min(...data),
      range = max - min || 1;
    return data
      .map(
        (v, i) =>
          `${(i / (data.length - 1)) * 80},${30 - ((v - min) / range) * 26 - 2}`,
      )
      .join(' ');
  }
}
