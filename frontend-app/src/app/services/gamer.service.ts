import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class GamerService {
  // use explicit backend URL during dev to avoid relying on proxy
  private base = 'http://localhost:8080/backend/api';
  constructor(private http: HttpClient) {}

  // getProfile accepts optional userId; if not provided, try local cache
  getProfile(userId?: any): Observable<any> {
    const uid = userId || this.getLocalUser()?.id || null;
    if (!uid) return of(null);
    // backend exposes user profiles under /usuarios/{id}
    return this.http.get<any>(`${this.base}/usuarios/${uid}`).pipe(catchError(()=> of(null)));
  }

  // update profile on server, fallback to local
  updateProfile(userId: any, payload: any): Observable<any> {
    if (!userId) { // store locally
      const local = this.getLocalUser() || {};
      const merged = Object.assign({}, local, payload);
      localStorage.setItem('tienda_user', JSON.stringify(merged));
      return of(merged);
    }
    // try to update server-side profile if endpoint exists; otherwise fallback silently
    return this.http.patch(`${this.base}/usuarios/${userId}`, payload).pipe(catchError(()=> of(null)));
  }

  rechargeWallet(usuario_id: number, amount: number): Observable<any> {
    if(!usuario_id) return of(null);
    // backend provides /cartera; POST will create a wallet entry (or be used by server to top-up)
    return this.http.post(`${this.base}/cartera`, { usuario_id, saldo: amount }).pipe(catchError(()=> of(null)));
  }

  getLibrary(userId?: number): Observable<any[]> {
    const uid = userId || this.getLocalUser()?.id || null;
    if (!uid) return of([]);
    // Use gamer reports preview for library (backend provides a reports preview)
    return this.http.get<any>(`${this.base}/gamer/${uid}/reports/library/preview`).pipe(map((res:any)=> {
      // map preview response to an array of games if available
      if (!res) return [];
      if (res.games) return res.games;
      if (res.rows) return res.rows;
      return [];
    }), catchError(()=> {
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

  // Store (published games)
  getStore(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/videojuegos?published=true`).pipe(catchError(()=> {
      // fallback: aggregate local catalogs from companies + demo games
      try{
        const local: any[] = [];
        const compsRaw = localStorage.getItem('local_companies') || '[]';
        const comps = JSON.parse(compsRaw);
        if (Array.isArray(comps)){
          comps.forEach((c:any)=>{
            try{
              const key = `local_catalog_${c.id}`;
              const rc = localStorage.getItem(key) || '[]';
              const arr = JSON.parse(rc);
              if (Array.isArray(arr)) arr.forEach((g:any)=> local.push(g));
            }catch(e){}
          });
        }
        try{ const demos = JSON.parse(localStorage.getItem('local_demo_games')||'[]'); if(Array.isArray(demos)) demos.forEach((d:any)=> local.push(d)); }catch(e){}
        return of(local);
      }catch(e){ return of([]); }
    }));
  }

  // Purchase a game
  buy(gameId: number): Observable<any>{
    const uid = this.getLocalUser()?.id || null;
    if (!uid) return of(null);
    // backend expects purchases via /compras with usuario_id + videojuego_id
    return this.http.post(`${this.base}/compras`, { usuario_id: uid, videojuego_id: gameId }).pipe(catchError(()=> of(null)));
  }

  // Wallet
  getWallet(userId?: number): Observable<any>{
    const uid = userId || this.getLocalUser()?.id || null;
    if (!uid) return of({ balance:0, transactions:[] });
    // CarteraHandler exposes wallets at /cartera (list). Fetch and pick user's wallet.
    return this.http.get<any[]>(`${this.base}/cartera`).pipe(map((list:any[])=> {
      const w = (list || []).find((x:any)=> Number(x.usuario_id) === Number(uid));
      if (!w) return { balance: this.getLocalUser()?.balance||0, transactions: [] };
      return { balance: w.saldo, transactions: [] };
    }), catchError(()=> of({ balance: this.getLocalUser()?.balance||0, transactions: [] })));
  }

  // Generate report; returns Blob (PDF) or null
  generateReport(kind: 'expenses'|'library'|'families'){
    const uid = this.getLocalUser()?.id || null;
    if (!uid) return of(null);
    return this.http.get(`${this.base}/gamer/${uid}/reports/${kind}`, { responseType: 'blob' as 'json' }).pipe(catchError(()=> of(null)));
  }

  // lightweight preview data for charts (fallback if reports endpoint not available)
  getReportPreview(kind: 'expenses'|'library'|'families'){
    const uid = this.getLocalUser()?.id || null;
    if(!uid) return of(null);
    return this.http.get<any>(`${this.base}/gamer/${uid}/reports/${kind}/preview`).pipe(catchError(()=> {
      // fallback: generate simple fake preview from local data
      if(kind==='expenses'){
        return of({ series: [10,20,35,15,5], labels:['Ene','Feb','Mar','Abr','May'] });
      }
      if(kind==='library'){
        return of({ series:[5,3,8], labels:['Accion','Aventura','Indie'] });
      }
      return of({ series:[1,2,3], labels:['A','B','C'] });
    }));
  }
}
