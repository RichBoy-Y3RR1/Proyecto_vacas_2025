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
      catchError(()=> { this.global = this.admin.getGlobalCommission(); this.notify.error('No se pudieron obtener comisiones del backend. Usando valor local.'); return of({}); }),
      finalize(()=> this.loading = false)
    ).subscribe((r: any)=>{ if (r && r.global!=null) this.global = Number(r.global); });
  }
  saveGlobal(){ this.loading = true;
    this.admin.setGlobalCommissionBackend(Number(this.global)).pipe(
      catchError(()=> { this.admin.setGlobalCommission(Number(this.global)); this.notify.error('Backend no disponible. Comisión guardada localmente.'); return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=> this.notify.success('Comisión global guardada'));
  }
  saveCompany(){ this.loading = true;
    this.admin.setCompanyCommissionBackend(Number(this.companyId), Number(this.companyPct)).pipe(
      catchError(()=> { this.admin.setCompanyCommission(Number(this.companyId), Number(this.companyPct)); this.notify.error('Backend no disponible. Comisión guardada localmente.'); return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=> this.notify.success('Comisión de empresa guardada'));
  }
}
