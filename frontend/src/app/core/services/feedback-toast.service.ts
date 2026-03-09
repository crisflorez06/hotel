import { Injectable, signal } from '@angular/core';

type FeedbackToastType = 'success' | 'error';

interface FeedbackToast {
  id: number;
  message: string;
  type: FeedbackToastType;
}

@Injectable({
  providedIn: 'root',
})
export class FeedbackToastService {
  readonly toast = signal<FeedbackToast | null>(null);

  private hideTimer: ReturnType<typeof setTimeout> | null = null;

  showSuccess(message: string, durationMs = 5200): void {
    this.show('success', message, durationMs);
  }

  showError(message: string, durationMs = 5600): void {
    this.show('error', message, durationMs);
  }

  clear(): void {
    this.toast.set(null);
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
      this.hideTimer = null;
    }
  }

  private show(type: FeedbackToastType, message: string, durationMs: number): void {
    const texto = message.trim();
    if (!texto) {
      return;
    }

    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
    }

    this.toast.set({
      id: Date.now(),
      message: texto,
      type,
    });

    this.hideTimer = setTimeout(() => {
      this.toast.set(null);
      this.hideTimer = null;
    }, durationMs);
  }
}
