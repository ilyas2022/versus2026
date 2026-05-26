export interface AdminUser {
  id: string;
  username: string;
  email: string;
  avatarUrl: string | null;
  role: 'PLAYER' | 'MODERATOR' | 'ADMIN';
  isActive: boolean;
  createdAt: string;
}

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  gamesToday: number;
  totalQuestions: number;
  pendingReports: number;
}

export interface AdminSpider {
  id: string;
  name: string;
  targetUrl: string;
  status: 'IDLE' | 'RUNNING' | 'FAILED';
  lastRunAt: string | null;
  lastRun: {
    id: string;
    startedAt: string;
    finishedAt: string | null;
    questionsInserted: number;
    errors: number;
  } | null;
}

export interface AdminReport {
  id: string;
  questionId: string;
  questionText: string | null;
  questionType: string | null;
  questionCategory: string | null;
  reason: 'WRONG_ANSWER' | 'OUTDATED' | 'OFFENSIVE' | 'OTHER';
  status: 'PENDING' | 'DISMISSED' | 'RESOLVED';
  comment: string | null;
  createdAt: string;
  resolvedBy: string | null;
  resolvedAt: string | null;
  action: 'DISMISS' | 'DELETE_QUESTION' | 'EDIT_QUESTION' | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
