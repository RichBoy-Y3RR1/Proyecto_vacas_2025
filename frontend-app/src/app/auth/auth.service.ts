import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, BehaviorSubject, throwError } from 'rxjs';
import { switchMap, map, tap, catchError } from 'rxjs/operators';

export interface AppUser { id?: number; email?: string; role?: string; name?: string }

@Injectable({ providedIn: 'root' })
export class AuthService {
  // direct backend URL to avoid proxy inconsistencies during development
  // backend may auto-select 8081 if 8080 is busy; use 8081 to match running server
  private base = 'http://localhost:8081/backend/api/auth';
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
        // first check local offline users stored by register() fallback
        try {
          const rawLocal = localStorage.getItem('local_users') || '[]';
          const localArr = JSON.parse(rawLocal) as any[];
          const target = String(email || '').toLowerCase().trim();
          const foundLocal = (localArr || []).find(u => u && (u.email || '').toString().toLowerCase().trim() === target) || null;
          if (foundLocal) return of(Object.assign({}, r || {}, { user: foundLocal }));
        } catch(e) { /* ignore */ }
        return this.http.get<any[]>('http://localhost:8081/backend/api/usuarios').pipe(
          map(users => {
            const target = String(email || '').toLowerCase().trim();
            const found = (users || []).find(u => (u && (u.email || u.correo || '').toString().toLowerCase().trim() === target)) || null;
            return Object.assign({}, r || {}, { user: found });
          })
        );
      }),
      // If the POST fails (401 or backend down), try the fallback to user list
      catchError(err => {
        // Try local offline users first (covers backend down or 401 responses)
        try {
          const rawLocal = localStorage.getItem('local_users') || '[]';
          const localArr = JSON.parse(rawLocal) as any[];
          const target = String(email || '').toLowerCase().trim();
          const foundLocal = (localArr || []).find(u => (u && (u.email || '').toString().toLowerCase().trim() === target)) || null;
          // Accept local user if password matches OR if no password was stored (legacy offline registration)
          if (foundLocal && ((foundLocal.password && foundLocal.password === password) || !foundLocal.password)) return of({ token: 'dev-token', user: foundLocal });
        } catch(e) { /* ignore */ }
        // otherwise try remote user list
        return this.http.get<any[]>('http://localhost:8081/backend/api/usuarios').pipe(
          map(users => {
            const target = String(email || '').toLowerCase().trim();
            const found = (users || []).find(u => (u && (u.email || u.correo || '').toString().toLowerCase().trim() === target)) || null;
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

  // Attempt a local-only login using users stored in localStorage by register() fallback.
  // Returns an observable with the same shape as remote login: { token, user } or null when not found.
  loginLocalFallback(email: string, password: string): Observable<any> {
    try {
      const raw = localStorage.getItem('local_users') || '[]';
      const arr = JSON.parse(raw) as any[];
      const target = String(email || '').toLowerCase().trim();
      const found = (arr || []).find(u => u && (u.email || '').toString().toLowerCase().trim() === target) || null;
      if (!found) return of(null);
      // accept if password matches or password not stored (legacy local user)
      if (found.password && found.password !== password) return of(null);
      const token = 'dev-token-local-' + Math.abs(found.id || Date.now());
      this.saveToken(token);
      this.saveUser(found);
      return of({ token, user: found });
    } catch (e) { return of(null); }
  }

  register(payload: any): Observable<any>{
    return this.http.post(`${this.base}/register`, payload).pipe(
      tap((r:any)=>{
        // if backend returns token+user on register, persist them like on login
        if (r && r.token) {
          try { this.saveToken(r.token); this.saveUser(r.user || null); } catch(e){}
        }
      }),
      catchError(err => {
        // If backend is unreachable or returns error, create a local/offline user for dev flow.
        try {
          const now = Date.now();
          const localId = -Math.abs(Math.floor(now / 1000));
          const user = {
            id: localId,
            email: payload.email,
            role: payload.role || 'USUARIO',
            nickname: payload.nickname || null,
            password: payload.password, // store plain password for local dev/login fallback
            _local: true
          };
          // persist in local users list grouped by 'local_users'
          const raw = localStorage.getItem('local_users') || '[]';
          const arr = JSON.parse(raw);
          arr.push(user);
          localStorage.setItem('local_users', JSON.stringify(arr));
          // save token and user in localStorage to emulate logged-in state
          this.saveToken('dev-token-' + Math.abs(localId));
          this.saveUser(user);
          return of({ id: localId, _local: true, user });
        } catch (e) {
          const msg = err && err.error && err.error.message ? err.error.message : (err && err.message) || 'Error al registrar';
          return throwError(() => ({ status: err && err.status, message: msg }));
        }
      })
    );
  }

  listUsers(): Observable<any[]>{
    return this.http.get<any[]>('http://localhost:8081/backend/api/usuarios');
  }

  authHeaders(): { headers: HttpHeaders } {
    const t = this.getToken();
    return { headers: new HttpHeaders(t ? { Authorization: `Bearer ${t}` } : {}) };
  }
}

