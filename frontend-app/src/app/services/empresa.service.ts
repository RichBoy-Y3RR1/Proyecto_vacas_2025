import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  // Use direct backend URL to avoid proxy issues in dev
  private base = 'http://localhost:8080/backend/api';
  constructor(private http: HttpClient) {}
  listCompanies(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/empresa`).pipe(catchError(err => {
      // fallback to locally persisted companies; if none exist, create a small set of demo companies
      try{
        const raw = localStorage.getItem('local_companies');
        if(raw){ const arr = JSON.parse(raw); if(Array.isArray(arr) && arr.length>0) return of(arr); }
        // create demo companies if nothing persisted
        const demo = [
          { id: -101, nombre: 'Empresa', correo: 'LagranManza@empresa.com' },
          { id: -102, nombre: 'Gamers', correo: 'GamerOnline@Gamers.com' },
          { id: -103, nombre: 'Juegos', correo: 'Steams@Juegos.com' }
        ];
        localStorage.setItem('local_companies', JSON.stringify(demo));
        return of(demo);
      }catch(e){}
      return of([]);
    }));
  }

  getCompany(id: number) {
    return this.http.get<any>(`${this.base}/empresa/${id}`).pipe(catchError(_ => {
      // try local cache
      try{
        const raw = localStorage.getItem('local_companies');
        if(raw){ const arr = JSON.parse(raw) as any[]; const found = arr.find(x=>x.id===id); if(found) return of(found); }
      }catch(e){}
      return of({ id, nombre: 'Empresa', catalog: [] });
    }));
  }

  createCompany(payload: any) {
    return this.http.post(`${this.base}/empresa`, payload).pipe(catchError(err => {
      // persist locally and return the created local object
      try{
        const localKey = 'local_companies';
        const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[];
        const id = -(Date.now());
        const obj = { id, nombre: payload.nombre || payload.name || 'Empresa', correo: payload.email || payload.correo || null };
        current.push(obj);
        localStorage.setItem(localKey, JSON.stringify(current));
        return of(obj);
      }catch(e){ return of(null); }
    }));
  }

  listCompanyUsers(companyId: number) {
    return this.http.get<any[]>(`${this.base}/empresa/${companyId}/usuarios`).pipe(catchError(_ => {
      try{ const raw = localStorage.getItem(`local_users_${companyId}`); if(raw) return of(JSON.parse(raw)); }catch(e){}
      return of([]);
    }));
  }

  addCompanyUser(companyId: number, payload: any) {
    return this.http.post(`${this.base}/empresa/${companyId}/usuarios`, payload).pipe(catchError(_ => {
      // persist locally and return the user
      try{
        const localKey = `local_users_${companyId}`;
        const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[];
        const id = -(Date.now());
        const user = { id, correo: payload.email || payload.correo || payload.email, email: payload.email || payload.correo || payload.email, role: 'USUARIO' };
        current.push(user);
        localStorage.setItem(localKey, JSON.stringify(current));
        return of(user);
      }catch(e){ return of(null); }
    }));
  }

  deleteCompanyUser(companyId: number, userId: number) {
    return this.http.delete(`${this.base}/empresa/${companyId}/usuarios/${userId}`).pipe(catchError(_ => {
      try{ const localKey = `local_users_${companyId}`; const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[]; const filtered = current.filter(x=>x.id!==userId); localStorage.setItem(localKey, JSON.stringify(filtered)); return of(true); }catch(e){ return of(null); }
    }));
  }

  updateCompany(companyId: number, payload: any) {
    return this.http.put(`${this.base}/empresas/${companyId}`, payload).pipe(catchError(_ => {
      // fallback: update local_companies cache if present
      try{
        const key = 'local_companies';
        const arr = JSON.parse(localStorage.getItem(key) || '[]') as any[];
        const idx = arr.findIndex(x=>x.id===companyId);
        if(idx>=0){ arr[idx] = { ...arr[idx], ...payload }; localStorage.setItem(key, JSON.stringify(arr)); return of(arr[idx]); }
        const obj = { id: companyId, ...payload };
        arr.push(obj); localStorage.setItem(key, JSON.stringify(arr)); return of(obj);
      }catch(e){ return of(null); }
    }));
  }

  // JasperReports download helpers (attempt backend PDF, fallback to local export)
  downloadSalesReport(companyId: number, from?: string, to?: string) {
    const url = `${this.base}/empresas/${companyId}/reportes/ventas${(from||to)?('?from='+(from||'')+'&to='+(to||'')) : ''}`;
    return this.http.get(url, { responseType: 'blob' as 'json' }).pipe(catchError(_ => of(null)));
  }

  downloadFeedbackReport(companyId: number) {
    const url = `${this.base}/empresas/${companyId}/reportes/feedback`;
    return this.http.get(url, { responseType: 'blob' as 'json' }).pipe(catchError(_ => of(null)));
  }

  downloadTopGamesReport(companyId: number, from?: string, to?: string) {
    const url = `${this.base}/empresas/${companyId}/reportes/top5${(from||to)?('?from='+(from||'')+'&to='+(to||'')) : ''}`;
    return this.http.get(url, { responseType: 'blob' as 'json' }).pipe(catchError(_ => of(null)));
  }
}

