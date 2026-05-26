import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminReport,
  AdminSpider,
  AdminStats,
  AdminUser,
  PageResponse,
} from '../models/admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // ── Stats ──────────────────────────────────────────────────────────────────

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.base}/admin/stats`);
  }

  // ── Users ──────────────────────────────────────────────────────────────────

  getUsers(
    page = 0,
    search?: string,
    role?: string,
  ): Observable<PageResponse<AdminUser>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (search) params = params.set('search', search);
    if (role) params = params.set('role', role);
    return this.http.get<PageResponse<AdminUser>>(`${this.base}/admin/users`, {
      params,
    });
  }

  updateUserRole(id: string, role: string): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.base}/admin/users/${id}/role`, {
      role,
    });
  }

  updateUserStatus(id: string, active: boolean): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.base}/admin/users/${id}/status`, {
      active,
    });
  }

  // ── Spiders ────────────────────────────────────────────────────────────────

  getSpiders(): Observable<AdminSpider[]> {
    return this.http.get<AdminSpider[]>(`${this.base}/admin/spiders`);
  }

  triggerSpider(name: string): Observable<unknown> {
    return this.http.post(`${this.base}/admin/spiders/${name}/run`, {});
  }

  // ── Reports ────────────────────────────────────────────────────────────────

  getReports(
    status?: string,
    page = 0,
  ): Observable<PageResponse<AdminReport>> {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<AdminReport>>(
      `${this.base}/moderation/reports`,
      { params },
    );
  }

  resolveReport(
    id: string,
    action: 'DISMISS' | 'DELETE_QUESTION' | 'EDIT_QUESTION',
  ): Observable<AdminReport> {
    return this.http.put<AdminReport>(
      `${this.base}/moderation/reports/${id}/resolve`,
      { action },
    );
  }
}
