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
    path: 'recepcion/panel',
    loadComponent: () =>
      import('./pages/recepcion-panel/recepcion-panel.component').then(
        (m) => m.RecepcionPanelComponent
      ),
  },
  {
    path: 'estancias',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/estancias/estancias.component').then((m) => m.EstanciasComponent),
  },
  {
    path: 'estancias/nueva',
    loadComponent: () =>
      import('./pages/estancia-nueva/estancia-nueva.component').then(
        (m) => m.EstanciaNuevaComponent
      ),
  },
  {
    path: 'estancias/salida',
    loadComponent: () =>
      import('./pages/estancia-salida/estancia-salida.component').then(
        (m) => m.EstanciaSalidaComponent
      ),
  },
  {
    path: 'calendario',
    loadComponent: () =>
      import('./pages/calendario/calendario.component').then((m) => m.CalendarioComponent),
  },
  {
    path: 'reservas',
    pathMatch: 'full',
    loadComponent: () =>
      import('./pages/reservas/reservas.component').then((m) => m.ReservasComponent),
  },
  {
    path: 'pagos',
    loadComponent: () => import('./pages/pagos/pagos.component').then((m) => m.PagosComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'ocupantes/tabla-clientes',
    loadComponent: () =>
      import('./pages/clientes/clientes.component').then((m) => m.ClientesComponent),
  },
  {
    path: 'ajustes',
    loadComponent: () =>
      import('./pages/ajustes/ajustes.component').then((m) => m.AjustesComponent),
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
