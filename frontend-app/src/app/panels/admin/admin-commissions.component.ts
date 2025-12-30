import { Component } from '@angular/core';
import { AdminService } from '../../services/admin.service';
import { NotificationService } from '../../services/notification.service';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

interface CommissionResponse { global?: number }

@Component({
  selector: 'app-admin-commissions',
  template: `
    <h3>Comisiones</h3>
    <div>
      <label>Global %</label>
      <input type="number" [(ngModel)]="global" />
      <button (click)="saveGlobal()">Guardar</button>
    </div>
    <div>
      <label>Comisión por empresa (ID)</label>
      <input type="number" [(ngModel)]="companyId" placeholder="ID empresa" />
      <input type="number" [(ngModel)]="companyPct" placeholder="%" />
      <button (click)="saveCompany()">Guardar empresa</button>
    </div>
  `
})
export class AdminCommissionsComponent{
  global = 0; companyId = 0; companyPct = 0;
  loading = false;
  constructor(private admin: AdminService, private notify: NotificationService){
    this.loading = true;
    this.admin.listCommissions().pipe(
      catchError(()=> { 
        // fallback: use persisted value or default to 15
        const local = this.admin.getGlobalCommission();
        this.global = local && Number(local) > 0 ? Number(local) : 15;
        this.notify.error('No se pudieron obtener comisiones del backend. Usando valor local.'); 
        return of({}); 
      }),
      finalize(()=> this.loading = false)
    ).subscribe((r: any)=>{ if (r && r.global!=null) this.global = Number(r.global); });
  }
  saveGlobal(){ this.loading = true;
    let newGlobal = Number(this.global);
    // enforce maximum global cap 15
    if(newGlobal > 15){ this.notify.error('El valor global no puede exceder 15%. Ajustando a 15.'); newGlobal = 15; this.global = 15; }
    this.admin.setGlobalCommissionBackend(newGlobal).pipe(
      catchError(()=> { this.admin.setGlobalCommission(newGlobal); this.notify.error('Backend no disponible. Comisión guardada localmente.'); return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=>{
      // adjust local company commissions higher than new global
      try{
        for(const key in localStorage){ if(key.startsWith('commission_company_')){
          const cid = Number(key.replace('commission_company_',''))||0; const pct = Number(localStorage.getItem(key)||'0');
          if(pct>newGlobal){ localStorage.setItem(key, String(newGlobal)); }
        }}
      }catch(e){}
      this.notify.success('Comisión global guardada');
    });
  }
  saveCompany(){ this.loading = true;
    let pct = Number(this.companyPct);
    // enforce maximum 15% and not exceed global
    if(pct > 15){ this.notify.error('La comisión de empresa no puede ser mayor que 15%. Ajustando a 15.'); pct = 15; this.companyPct = 15; }
    if(pct > Number(this.global)){ this.notify.error('La comisión de empresa no puede ser mayor que la global. Ajustando al valor global.'); pct = Number(this.global); this.companyPct = Number(this.global); }
    this.admin.setCompanyCommissionBackend(Number(this.companyId), pct).pipe(
      catchError(()=> { this.admin.setCompanyCommission(Number(this.companyId), pct); this.notify.error('Backend no disponible. Comisión guardada localmente.'); return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=> this.notify.success('Comisión de empresa guardada'));
  }
}
