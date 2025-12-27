import { Component } from '@angular/core';
import { AdminService } from '../../services/admin.service';

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
  constructor(private admin: AdminService){
    // try backend first
    this.admin.listCommissions().subscribe({ next: r=>{ if (r && r.global!=null) this.global = Number(r.global); }, error: ()=>{ this.global = this.admin.getGlobalCommission(); } });
  }
  saveGlobal(){
    this.admin.setGlobalCommissionBackend(Number(this.global)).subscribe({ next: ()=> alert('Comisión global guardada'), error: ()=>{ this.admin.setGlobalCommission(Number(this.global)); alert('Guardada en local (backend no disponible)'); } });
  }
  saveCompany(){
    this.admin.setCompanyCommissionBackend(Number(this.companyId), Number(this.companyPct)).subscribe({ next: ()=> alert('Comisión de empresa guardada'), error: ()=>{ this.admin.setCompanyCommission(Number(this.companyId), Number(this.companyPct)); alert('Guardada en local (backend no disponible)'); } });
  }
}
