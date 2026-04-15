import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const APP_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'transactions' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'transactions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transactions/transaction-history.component').then(m => m.TransactionHistoryComponent)
  },
  {
    path: 'recommend',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/payments/recommend.component').then(m => m.RecommendComponent)
  },
  {
    path: 'gateways',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/gateways/gateway-list.component').then(m => m.GatewayListComponent)
  },
  { path: '**', redirectTo: 'transactions' }
];
