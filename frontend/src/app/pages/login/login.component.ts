import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { AuthService, LoginRequest } from '../../services/auth.service';
import { extractBackendErrorMessage } from '../../core/utils/http-error.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);

  usuario = '';
  password = '';
  cargando = false;
  error = '';

  login(): void {
    if (this.cargando) {
      return;
    }

    this.error = '';

    if (!this.usuario.trim()) {
      this.error = 'El usuario es obligatorio.';
      return;
    }

    if (!this.password) {
      this.error = 'La contraseña es obligatoria.';
      return;
    }

    this.cargando = true;
    const request: LoginRequest = {
      usuario: this.usuario.trim(),
      password: this.password,
    };

    this.authService
      .login(request)
      .pipe(finalize(() => (this.cargando = false)))
      .subscribe({
        error: (errorResponse: unknown) => {
          this.error = extractBackendErrorMessage(
            errorResponse,
            'Credenciales inválidas. Verifica tu usuario y contraseña.'
          );
        },
      });
  }

  limpiarError(): void {
    this.error = '';
  }
}
