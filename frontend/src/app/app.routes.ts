import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'recepcion',
    loadComponent: () =>
      import('./pages/recepcion/recepcion.component').then((m) => m.RecepcionComponent),
    canActivate: [authGuard],
  },
  {
    path: 'recepcion/panel',
    loadComponent: () =>
      import('./pages/recepcion-panel/recepcion-panel.component').then(
        (m) => m.RecepcionPanelComponent
      ),
    canActivate: [authGuard],
  },
  {
    path: 'estancias',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/estancias/estancias.component').then((m) => m.EstanciasComponent),
    canActivate: [authGuard],
  },
  {
    path: 'estancias/nueva',
    loadComponent: () =>
      import('./pages/estancia-nueva/estancia-nueva.component').then(
        (m) => m.EstanciaNuevaComponent
      ),
    canActivate: [authGuard],
  },
  {
    path: 'estancias/salida',
    loadComponent: () =>
      import('./pages/estancia-salida/estancia-salida.component').then(
        (m) => m.EstanciaSalidaComponent
      ),
    canActivate: [authGuard],
  },
  {
    path: 'calendario',
    loadComponent: () =>
      import('./pages/calendario/calendario.component').then((m) => m.CalendarioComponent),
    canActivate: [authGuard],
  },
  {
    path: 'reservas',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/reservas/reservas.component').then((m) => m.ReservasComponent),
    canActivate: [authGuard],
  },
  {
    path: 'pagos',
    loadComponent: () => import('./pages/pagos/pagos.component').then((m) => m.PagosComponent),
    canActivate: [authGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: 'monitor-eventos',
    loadComponent: () =>
      import('./pages/monitor-eventos/monitor-eventos.component').then((m) => m.MonitorEventosComponent),
    canActivate: [authGuard],
  },
  {
    path: 'ocupantes/tabla-clientes',
    loadComponent: () =>
      import('./pages/clientes/clientes.component').then((m) => m.ClientesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'ajustes',
    loadComponent: () =>
      import('./pages/ajustes/ajustes.component').then((m) => m.AjustesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'reservas/nueva',
    loadComponent: () =>
      import('./pages/reserva-nueva/reserva-nueva.component').then(
        (m) => m.ReservaNuevaComponent
      ),
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: 'login' },
];
