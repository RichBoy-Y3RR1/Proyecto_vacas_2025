import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, BehaviorSubject, throwError } from 'rxjs';
import { switchMap, map, tap, catchError } from 'rxjs/operators';

export interface AppUser { id?: number; email?: string; role?: string; name?: string }

@Injectable({ providedIn: 'root' })
export class AuthService {

  private base = '/backend/api/auth';
  private tokenKey = 'tienda_token';
  private userKey = 'tienda_user';
  private currentUserSubject = new BehaviorSubject<AppUser | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}


  seedDefaultLocalUsers(force: boolean = false){
    try{
      const existing = localStorage.getItem('local_users');
      if(existing && !force) return;
      const users = [
        { id: 1, email: 'admin@tienda.com', password: 'admin123', role: 'ADMIN', nickname: 'admin' },
        { id: 2, email: 'empresa@acme.com', password: 'empresa123', role: 'EMPRESA', nickname: 'acme_user', empresaId: 1 },
        { id: 3, email: 'user@cliente.com', password: 'user123', role: 'USUARIO', nickname: 'gamer123' }
      ];
      localStorage.setItem('local_users', JSON.stringify(users));
      // Also seed a simple local company registry for inferEmpresaFromLocal
      const comps = [{ id: 1, nombre: 'Acme Games', correo: 'contact@acmegames.com' }];
      localStorage.setItem('local_companies', JSON.stringify(comps));
    }catch(e){ /* ignore */ }
  }

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
    return this.http.post<any>(`${this.base}/login`, { email, password }).pipe(
      tap(r => {
        const token = r && r.token ? r.token : null;
        let user = r && r.user ? r.user : null;
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
        if (r && r.token) {
          try { this.saveToken(r.token); this.saveUser(r.user || null); } catch(e){}
        }
      })
    );
  }

  listUsers(): Observable<any[]>{
    return this.http.get<any[]>('http://localhost:8080/backend/api/usuarios');
  }

  authHeaders(): { headers: HttpHeaders } {
    const t = this.getToken();
    return { headers: new HttpHeaders(t ? { Authorization: `Bearer ${t}` } : {}) };
  }
}

