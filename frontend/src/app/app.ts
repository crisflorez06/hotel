import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs';
import { FeedbackToastService } from './core/services/feedback-toast.service';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly documentRef = inject(DOCUMENT);
  readonly feedbackToast = inject(FeedbackToastService);
  readonly authService = inject(AuthService);
  readonly authState = this.authService.authState;

  isAppShellVisible = true;
  isMobileMenuOpen = false;

  isLoading = true;

  logout(): void {
    this.closeMobileMenu();
    this.authService.logout();
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
    this.syncBodyMenuState();
  }

  closeMobileMenu(): void {
    if (!this.isMobileMenuOpen) {
      return;
    }

    this.isMobileMenuOpen = false;
    this.syncBodyMenuState();
  }

  private syncBodyMenuState(): void {
    this.documentRef.body.classList.toggle('menu-open', this.isMobileMenuOpen);
  }

  constructor() {
    this.router.events
      .pipe(
        filter(
          (event) =>
            event instanceof NavigationStart ||
            event instanceof NavigationEnd ||
            event instanceof NavigationCancel ||
            event instanceof NavigationError
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((event) => {
        if (event instanceof NavigationStart) {
          this.isLoading = true;
          return;
        }

        if (event instanceof NavigationEnd) {
          this.closeMobileMenu();
          this.isAppShellVisible = event.urlAfterRedirects !== '/login';
          requestAnimationFrame(() => {
            this.isLoading = false;
          });
          return;
        }

        this.closeMobileMenu();
        this.isLoading = false;
      });
  }
}
