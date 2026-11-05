import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { Account, BankService, Entry } from '../core/bank.service';

const MONEY = [Validators.required, Validators.min(0.01), Validators.max(50000)];

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe],
  template: `
    <header>
      <h1>My accounts</h1>
      <button (click)="logout()">Sign out</button>
    </header>

    <section class="card">
      <table>
        <thead><tr><th>Account</th><th>Balance</th><th></th></tr></thead>
        <tbody>
          @for (a of accounts(); track a.id) {
            <tr [class.selected]="a.id === selected()">
              <td>#{{ a.id }} {{ a.maskedNumber }}</td>
              <td>{{ a.balance | currency }}</td>
              <td><button class="link" (click)="select(a.id)">History</button></td>
            </tr>
          } @empty {
            <tr><td colspan="3">No accounts yet.</td></tr>
          }
        </tbody>
      </table>
      <button (click)="open()">Open account</button>
    </section>

    <section class="card">
      <h2>Deposit</h2>
      <form [formGroup]="depositForm" (ngSubmit)="deposit()">
        <label>Account
          <select formControlName="accountId">
            @for (a of accounts(); track a.id) { <option [ngValue]="a.id">#{{ a.id }} {{ a.maskedNumber }}</option> }
          </select>
        </label>
        <label>Amount <input type="number" step="0.01" formControlName="amount" /></label>
        <button type="submit" [disabled]="depositForm.invalid">Deposit</button>
      </form>
    </section>

    <section class="card">
      <h2>Transfer</h2>
      <form [formGroup]="transferForm" (ngSubmit)="transfer()">
        <label>From
          <select formControlName="from">
            @for (a of accounts(); track a.id) { <option [ngValue]="a.id">#{{ a.id }} {{ a.maskedNumber }}</option> }
          </select>
        </label>
        <label>To account id <input type="number" formControlName="to" /></label>
        <label>Amount <input type="number" step="0.01" formControlName="amount" /></label>
        <button type="submit" [disabled]="transferForm.invalid">Send</button>
      </form>
    </section>

    @if (message()) { <p class="message">{{ message() }}</p> }

    @if (selected()) {
      <section class="card">
        <h2>Recent activity, account #{{ selected() }}</h2>
        <table>
          <thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Balance after</th></tr></thead>
          <tbody>
            @for (e of history(); track e.id) {
              <tr>
                <td>{{ e.createdAt | date: 'short' }}</td>
                <td>{{ e.type }}</td>
                <td [class.neg]="e.amount < 0">{{ e.amount | currency }}</td>
                <td>{{ e.balanceAfter | currency }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
})
export class AccountsComponent implements OnInit {
  private bank = inject(BankService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  accounts = signal<Account[]>([]);
  history = signal<Entry[]>([]);
  selected = signal<number | null>(null);
  message = signal('');

  depositForm = this.fb.group({
    accountId: this.fb.control<number | null>(null, Validators.required),
    amount: this.fb.control<number | null>(null, MONEY),
  });
  transferForm = this.fb.group({
    from: this.fb.control<number | null>(null, Validators.required),
    to: this.fb.control<number | null>(null, Validators.required),
    amount: this.fb.control<number | null>(null, MONEY),
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.bank.accounts().subscribe((list) => this.accounts.set(list));
    const id = this.selected();
    if (id) {
      this.bank.history(id).subscribe((h) => this.history.set(h));
    }
  }

  select(id: number): void {
    this.selected.set(id);
    this.refresh();
  }

  open(): void {
    this.bank.open().subscribe(() => this.refresh());
  }

  deposit(): void {
    const { accountId, amount } = this.depositForm.getRawValue();
    this.bank.deposit(accountId!, amount!).subscribe({
      next: () => this.done('Deposit complete.', () => this.depositForm.reset()),
      error: (e) => this.message.set(e?.error?.error ?? 'Deposit failed'),
    });
  }

  transfer(): void {
    const { from, to, amount } = this.transferForm.getRawValue();
    this.bank.transfer(from!, to!, amount!).subscribe({
      next: () => this.done('Transfer complete.', () => this.transferForm.reset()),
      error: (e) => this.message.set(e?.error?.error ?? 'Transfer failed'),
    });
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  private done(msg: string, reset: () => void): void {
    this.message.set(msg);
    reset();
    this.refresh();
  }
}
