import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { AccountsComponent } from './pages/accounts.component';
import { LoginComponent } from './pages/login.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'accounts', component: AccountsComponent, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'accounts' },
  { path: '**', redirectTo: 'accounts' },
];
