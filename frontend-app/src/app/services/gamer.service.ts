import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class GamerService {
  private base = 'http://localhost:8081/backend/api';
  constructor(private http: HttpClient) {}

  // getProfile accepts optional userId; if not provided, try local cache
  getProfile(userId?: any): Observable<any> {
    const uid = userId || this.getLocalUser()?.id || null;
    if (!uid) return of(null);
    return this.http.get<any>(`${this.base}/gamer/profile/${uid}`).pipe(catchError(()=> of(null)));
  }

  // update profile on server, fallback to local
  updateProfile(userId: any, payload: any): Observable<any> {
    if (!userId) { // store locally
      const local = this.getLocalUser() || {};
      const merged = Object.assign({}, local, payload);
      localStorage.setItem('tienda_user', JSON.stringify(merged));
      return of(merged);
    }
    return this.http.patch(`${this.base}/gamer/profile/${userId}`, payload).pipe(catchError(()=> of(null)));
  }

  rechargeWallet(usuario_id: number, amount: number): Observable<any> {
    if(!usuario_id) return of(null);
    return this.http.post(`${this.base}/gamer/${usuario_id}/recharge`, { amount }).pipe(catchError(()=> of(null)));
  }

  getLibrary(userId?: number): Observable<any[]> {
    const uid = userId || this.getLocalUser()?.id || null;
    if (!uid) return of([]);
    return this.http.get<any[]>(`${this.base}/gamer/${uid}/library`).pipe(catchError(()=> {
      // fallback to localStorage key 'local_library_{uid}'
      try { const k = `local_library_${uid}`; const raw = localStorage.getItem(k) || '[]'; return of(JSON.parse(raw)); } catch(e){ return of([]); }
    }));
  }

  updateGameState(gameId: number, state: string): Observable<any> {
    if (!gameId) return of(null);
    return this.http.post(`${this.base}/gamer/library/${gameId}/state`, { state }).pipe(catchError(()=> {
      // update local cached library entries if present
      try {
        const local = this.getLocalUser();
        const uid = local?.id;
        if(!uid) return of(null);
        const key = `local_library_${uid}`;
        const raw = localStorage.getItem(key) || '[]';
        const arr = JSON.parse(raw);
        for(const g of arr){ if(g.id === gameId){ g.estado = state; break; } }
        localStorage.setItem(key, JSON.stringify(arr));
        return of({ ok: true });
      } catch(e){ return of(null); }
    }));
  }

  // local fallback helpers for offline mode
  getLocalUser(): any | null {
    try { const s = localStorage.getItem('tienda_user'); return s ? JSON.parse(s) : null; } catch { return null; }
  }
}
