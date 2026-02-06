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
    path: 'reserva',
    loadComponent: () =>
      import('./pages/reserva/reserva.component').then((m) => m.ReservaComponent),
  },
  {
    path: 'pagos',
    loadComponent: () => import('./pages/pagos/pagos.component').then((m) => m.PagosComponent),
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
