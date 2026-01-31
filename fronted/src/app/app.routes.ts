import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'recepcion',
  },
  {
    path: 'recepcion',
    loadComponent: () =>
      import('./pages/recepcion/recepcion.component').then((m) => m.RecepcionComponent),
  },
  {
    path: 'estancias/nueva',
    loadComponent: () =>
      import('./pages/estancia-nueva/estancia-nueva.component').then(
        (m) => m.EstanciaNuevaComponent
      ),
  },
  {
    path: 'reserva',
    loadComponent: () =>
      import('./pages/reserva/reserva.component').then((m) => m.ReservaComponent),
  },
  {
    path: 'reservas/nueva',
    loadComponent: () =>
      import('./pages/reserva-nueva/reserva-nueva.component').then(
        (m) => m.ReservaNuevaComponent
      ),
  },
  { path: '**', redirectTo: '' },
];
