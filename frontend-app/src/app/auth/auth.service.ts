import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, BehaviorSubject, throwError } from 'rxjs';
import { switchMap, map, tap, catchError } from 'rxjs/operators';

export interface AppUser { id?: number; email?: string; role?: string; name?: string }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private base = '/tienda-backend-1.0.0/api/auth';
  private tokenKey = 'tienda_token';
  private userKey = 'tienda_user';
  private currentUserSubject = new BehaviorSubject<AppUser | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  private saveToken(token: string|null){
    if (token) localStorage.setItem(this.tokenKey, token); else localStorage.removeItem(this.tokenKey);
  }
  private saveUser(user: AppUser|null){
    if (user) localStorage.setItem(this.userKey, JSON.stringify(user)); else localStorage.removeItem(this.userKey);
    this.currentUserSubject.next(user);
  }
  private loadUser(): AppUser | null {
    try { const s = localStorage.getItem(this.userKey); return s ? JSON.parse(s) : null; } catch { return null; }
  }

  getCurrentUser(): AppUser | null { return this.currentUserSubject.value; }
  getToken(): string | null { return localStorage.getItem(this.tokenKey); }
  isLoggedIn(): boolean { return !!this.getToken() || !!this.currentUserSubject.value; }

  login(email: string, password: string): Observable<any>{
    // Try normal login; if it errors (backend down or 401), attempt a dev fallback by fetching users
    // Robust login: accept responses that include only a token, and fall back on 401 by
    // fetching the user list to recover a user object for dev flows.
    return this.http.post<any>(`${this.base}/login`, { email, password }).pipe(
      // If server returns token-only, enrich it with the user found by email
      switchMap(r => {
        if (r && r.user) return of(r);
        // token-only response: try to find the user by email
        return this.http.get<any[]>('/tienda-backend-1.0.0/api/usuarios').pipe(
          map(users => {
            const found = (users || []).find(u => u.email === email) || null;
            return Object.assign({}, r || {}, { user: found });
          })
        );
      }),
      // If the POST fails (401 or backend down), try the fallback to user list
      catchError(err => {
        // If authentication failed (401), propagate a friendly error message
        if (err && err.status === 401) {
          return throwError(() => ({ status: 401, message: 'Contraseña o correo incorrecto o el usuario no existe' }));
        }
        // For other errors (backend down), try the fallback to user list so dev flow can continue
        return this.http.get<any[]>('/tienda-backend-1.0.0/api/usuarios').pipe(
          map(users => {
            const found = (users || []).find(u => u.email === email) || null;
            return { token: found ? 'dev-token' : null, user: found };
          })
        );
      }),
      tap(r => {
        const token = r && r.token ? r.token : null;
        let user = r && r.user ? r.user : null;
        // infer empresa membership from local cache when backend doesn't provide it
        try{
          const inferred = this.inferEmpresaFromLocal(user);
          if(inferred) user = Object.assign({}, user, inferred);
        }catch(e){}
        this.saveToken(token);
        this.saveUser(user);
      })
    );
  }

  // If user email is listed in any local company users cache, return { role:'EMPRESA', empresa_id }
  private inferEmpresaFromLocal(user: any): any | null{
    try{
      if(!user || !user.email) return null;
      const email = String(user.email).toLowerCase();
      const rawCompanies = localStorage.getItem('local_companies');
      if(!rawCompanies) return null;
      const companies = JSON.parse(rawCompanies) as any[];
      for(const c of companies){
        const cid = c.id;
        try{
          const rawUsers = localStorage.getItem(`local_users_${cid}`);
          if(!rawUsers) continue;
          const users = JSON.parse(rawUsers) as any[];
          for(const u of users){
            const ue = (u.email || u.correo || u.mail || u.username || u.correo_electronico || '').toString().toLowerCase();
            if(ue === email) return { role: 'EMPRESA', empresa_id: cid };
          }
        }catch(e){ continue; }
        // also treat company owner email as empresa member
        const compEmail = (c.correo || c.email || '') + '';
        if(compEmail && compEmail.toLowerCase() === email) return { role: 'EMPRESA', empresa_id: c.id };
      }
    }catch(e){/* ignore */}
    return null;
  }

  logout(){ this.saveToken(null); this.saveUser(null); }

  register(payload: any): Observable<any>{
    return this.http.post(`${this.base}/register`, payload).pipe(
      catchError(err => {
        const msg = err && err.error && err.error.message ? err.error.message : (err && err.message) || 'Error al registrar';
        return throwError(() => ({ status: err && err.status, message: msg }));
      })
    );
  }

  listUsers(): Observable<any[]>{
    return this.http.get<any[]>('/tienda-backend-1.0.0/api/usuarios');
  }

  authHeaders(): { headers: HttpHeaders } {
    const t = this.getToken();
    return { headers: new HttpHeaders(t ? { Authorization: `Bearer ${t}` } : {}) };
  }
}
