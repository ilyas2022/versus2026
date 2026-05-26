import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { AdminSidebarComponent } from '../../components/sidebar/sidebar';
import { AdminService } from '../../../../core/services/admin.service';
import { AdminUser, PageResponse } from '../../../../core/models/admin.models';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [AdminSidebarComponent, FormsModule, DatePipe],
  templateUrl: './admin-users.html',
  styleUrl: '../dashboard/admin-dashboard.scss',
})
export class AdminUsers implements OnInit {
  private readonly adminSvc = inject(AdminService);

  page = signal<PageResponse<AdminUser> | null>(null);
  loading = signal(true);
  search = signal('');
  roleFilter = signal('');
  currentPage = signal(0);

  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.load();
  }

  onSearchChange(val: string): void {
    this.search.set(val);
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.currentPage.set(0);
      this.load();
    }, 400);
  }

  onRoleChange(val: string): void {
    this.roleFilter.set(val);
    this.currentPage.set(0);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.adminSvc
      .getUsers(
        this.currentPage(),
        this.search() || undefined,
        this.roleFilter() || undefined,
      )
      .subscribe({
        next: (p) => {
          this.page.set(p);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  updateRole(user: AdminUser, role: string): void {
    this.adminSvc.updateUserRole(user.id, role).subscribe({
      next: (updated) => {
        const p = this.page();
        if (!p) return;
        this.page.set({
          ...p,
          content: p.content.map((u) => (u.id === updated.id ? updated : u)),
        });
      },
    });
  }

  toggleStatus(user: AdminUser): void {
    this.adminSvc.updateUserStatus(user.id, !user.isActive).subscribe({
      next: (updated) => {
        const p = this.page();
        if (!p) return;
        this.page.set({
          ...p,
          content: p.content.map((u) => (u.id === updated.id ? updated : u)),
        });
      },
    });
  }

  roleColor(r: string): string {
    return (
      { ADMIN: 'var(--vs-accent-red)', MODERATOR: 'var(--vs-accent-gold)' }[
        r
      ] ?? 'var(--vs-accent-blue)'
    );
  }

  roleBg(r: string): string {
    return (
      {
        ADMIN: 'rgba(230,57,70,0.12)',
        MODERATOR: 'rgba(244,197,66,0.12)',
      }[r] ?? 'rgba(67,97,238,0.12)'
    );
  }

  initials(name: string): string {
    return name.slice(0, 2).toUpperCase();
  }
}
