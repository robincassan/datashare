  import { Routes } from '@angular/router';

  export const routes: Routes = [
    { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent) },
    { path: 'register', loadComponent: () => import('./pages/register/register').then(m => m.RegisterComponent) },
    { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.DashboardComponent) },
    { path: 'download/:token', loadComponent: () => import('./pages/download/download').then(m => m.DownloadComponent) },
    { path: '', redirectTo: '/login', pathMatch: 'full' },
    { path: '**', redirectTo: '/login' }
  ];