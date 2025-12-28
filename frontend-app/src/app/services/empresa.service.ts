import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class EmpresaService {
  // Use relative proxy path so `ng serve --proxy-config` forwards requests to backend
  private base = '/api';
  constructor(private http: HttpClient) {}
  listCompanies(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/empresas`).pipe(catchError(err => {
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
    return this.http.get<any>(`${this.base}/empresas/${id}`).pipe(catchError(_ => {
      // try local cache
      try{
        const raw = localStorage.getItem('local_companies');
        if(raw){ const arr = JSON.parse(raw) as any[]; const found = arr.find(x=>x.id===id); if(found) return of(found); }
      }catch(e){}
      return of({ id, nombre: 'Empresa', catalog: [] });
    }));
  }

  createCompany(payload: any) {
    return this.http.post(`${this.base}/empresas`, payload).pipe(catchError(err => {
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
    return this.http.get<any[]>(`${this.base}/empresas/${companyId}/usuarios`).pipe(catchError(_ => {
      try{ const raw = localStorage.getItem(`local_users_${companyId}`); if(raw) return of(JSON.parse(raw)); }catch(e){}
      return of([]);
    }));
  }

  addCompanyUser(companyId: number, payload: any) {
    return this.http.post(`${this.base}/empresas/${companyId}/usuarios`, payload).pipe(catchError(_ => {
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
    return this.http.delete(`${this.base}/empresas/${companyId}/usuarios/${userId}`).pipe(catchError(_ => {
      try{ const localKey = `local_users_${companyId}`; const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[]; const filtered = current.filter(x=>x.id!==userId); localStorage.setItem(localKey, JSON.stringify(filtered)); return of(true); }catch(e){ return of(null); }
    }));
  }
}

