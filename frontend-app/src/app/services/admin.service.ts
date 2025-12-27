import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private base = 'http://localhost:8080/tienda-backend-1.0.0/api';
  constructor(private http: HttpClient){}
  listCategories(): Observable<any[]>{ return this.http.get<any[]>(`${this.base}/categorias`); }
  createCategory(payload:any){ return this.http.post(`${this.base}/categorias`, payload); }
  updateCategory(id:number,payload:any){ return this.http.put(`${this.base}/categorias/${id}`, payload); }
  deleteCategory(id:number){ return this.http.delete(`${this.base}/categorias/${id}`); }

  listBanners(){ return this.http.get<any[]>(`${this.base}/banner`); }
  createBanner(payload:any){ return this.http.post(`${this.base}/banner`, payload); }
  updateBanner(id:number,payload:any){ return this.http.put(`${this.base}/banner/${id}`, payload); }
  deleteBanner(id:number){ return this.http.delete(`${this.base}/banner/${id}`); }

  // commissions: no backend endpoint available — stored locally as fallback
  // Commission endpoints: prefer backend, fallback to localStorage
  listCommissions(){ return this.http.get<any>(`${this.base}/comisiones`); }
  setGlobalCommissionBackend(p:number){ return this.http.put(`${this.base}/comisiones`, { globalPercent: p }); }
  setCompanyCommissionBackend(companyId:number,p:number){ return this.http.put(`${this.base}/comisiones`, { empresa_id: companyId, percent: p }); }
  getGlobalCommission(){ return Number(localStorage.getItem('global_commission') || '0'); }
  setGlobalCommission(p:number){ localStorage.setItem('global_commission', String(p)); }
  setCompanyCommission(companyId:number, p:number){ localStorage.setItem('commission_company_'+companyId, String(p)); }
  getCompanyCommission(companyId:number){ return Number(localStorage.getItem('commission_company_'+companyId) || '0'); }
}
