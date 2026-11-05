import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="card">
      <h1>{{ mode() === 'login' ? 'Sign in' : 'Create account' }}</h1>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label>Username <input formControlName="username" autocomplete="username" /></label>
        <label>Password <input type="password" formControlName="password" autocomplete="current-password" /></label>
        @if (mode() === 'register') {
          <label>Phone (optional) <input formControlName="phone" /></label>
        }
        <button type="submit" [disabled]="form.invalid || busy()">
          {{ mode() === 'login' ? 'Sign in' : 'Register' }}
        </button>
      </form>
      @if (error()) { <p class="error">{{ error() }}</p> }
      <button class="link" (click)="toggle()">
        {{ mode() === 'login' ? 'New here? Create an account' : 'Have an account? Sign in' }}
      </button>
    </section>
  `,
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  mode = signal<'login' | 'register'>('login');
  busy = signal(false);
  error = signal('');
  form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    phone: [''],
  });

  toggle(): void {
    this.mode.set(this.mode() === 'login' ? 'register' : 'login');
    this.error.set('');
  }

  submit(): void {
    const { username, password, phone } = this.form.getRawValue();
    this.busy.set(true);
    const login$ = this.auth.login(username, password);
    const flow$ = this.mode() === 'register'
      ? this.auth.register(username, password, phone).pipe(switchMap(() => login$))
      : login$;
    flow$.subscribe({
      next: () => this.router.navigate(['/accounts']),
      error: (e) => {
        this.error.set(e?.error?.error ?? 'Something went wrong');
        this.busy.set(false);
      },
    });
  }
}
