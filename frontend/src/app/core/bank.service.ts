import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments';

export interface Account {
  id: number;
  maskedNumber: string;
  balance: number;
  createdAt: string;
}

export interface Entry {
  id: number;
  type: string;
  amount: number;
  balanceAfter: number;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class BankService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/accounts`;

  /** One key per user action, so a double click or network retry is applied once. */
  private idempotent(): { headers: HttpHeaders } {
    return { headers: new HttpHeaders({ 'Idempotency-Key': crypto.randomUUID() }) };
  }

  accounts(): Observable<Account[]> {
    return this.http.get<Account[]>(this.base);
  }

  open(): Observable<Account> {
    return this.http.post<Account>(this.base, {});
  }

  history(id: number): Observable<Entry[]> {
    return this.http.get<Entry[]>(`${this.base}/${id}/transactions`);
  }

  deposit(id: number, amount: number): Observable<Account> {
    return this.http.post<Account>(`${this.base}/${id}/deposit`, { amount }, this.idempotent());
  }

  transfer(fromAccountId: number, toAccountId: number, amount: number): Observable<Account> {
    return this.http.post<Account>(`${this.base}/transfer`, { fromAccountId, toAccountId, amount }, this.idempotent());
  }
}
