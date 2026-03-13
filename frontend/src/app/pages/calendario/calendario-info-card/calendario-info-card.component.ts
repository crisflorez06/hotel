import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-calendario-info-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './calendario-info-card.component.html',
  styleUrl: './calendario-info-card.component.css',
})
export class CalendarioInfoCardComponent {
  @Input() icon = 'bi-info-circle';
  @Input() label = '';
  @Input() tooltip: string | null = null;
  @Input() clickable = false;
  @Input() full = false;

  @Output() activate = new EventEmitter<void>();

  onClick(): void {
    if (!this.clickable) {
      return;
    }

    this.activate.emit();
  }

  onKeydown(event: KeyboardEvent): void {
    if (!this.clickable) {
      return;
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.activate.emit();
    }
  }
}
