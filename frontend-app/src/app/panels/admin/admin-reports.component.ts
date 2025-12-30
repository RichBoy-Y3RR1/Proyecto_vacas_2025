import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-admin-reports',
  template: `
    <h3>Reportes básicos</h3>
    <div>Videojuegos totales: {{ videogamesCount }}</div>
    <div>Usuarios totales: {{ usersCount }}</div>
    <div style="margin-top:8px">
      <button (click)="exportSummary()">Exportar resumen CSV</button>
      <button (click)="exportPdf()" style="margin-left:8px">Exportar reportes (PDF)</button>
    </div>
  `
})
export class AdminReportsComponent implements OnInit{
  videogamesCount = 0; usersCount = 0;
  loading = false;
  constructor(private http: HttpClient, private notify: NotificationService){}
  ngOnInit(){ this.loadCounts(); }
  loadCounts(){
    this.loading = true;
    this.http.get<any[]>('http://localhost:8080/backend/api/videojuegos').pipe(
      catchError(()=>{ this.notify.error('No se pudo cargar conteo de videojuegos; fallback local.'); try{ const raw = localStorage.getItem('local_catalog_all'); return of(raw? JSON.parse(raw): []);}catch(e){ return of([]);} }),
      finalize(()=> this.loading=false)
    ).subscribe(v=> this.videogamesCount = v?.length || 0);

    this.http.get<any[]>('http://localhost:8080/backend/api/usuarios').pipe(
      catchError(()=>{ this.notify.error('No se pudo cargar conteo de usuarios; fallback local.'); try{ const raw = localStorage.getItem('local_users_all'); return of(raw? JSON.parse(raw): []);}catch(e){ return of([]);} }),
    ).subscribe(u=> this.usersCount = u?.length || 0);
  }

  exportSummary(){
    const rows = [ ['metric','value'], ['videogames_total', String(this.videogamesCount)], ['users_total', String(this.usersCount)] ];
    const csv = rows.map(r=> r.map(c=> '"'+String(c).replace(/"/g,'""')+'"').join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'report_summary.csv'; document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
  }

  exportPdf(){
    // open report servlet which supports PDF export (full history by default)
    // Open top5 PDF for company id 10 (adjust company id as needed)
    const companyId = 10;
    const url = `http://localhost:8080/backend/api/empresas/${companyId}/reportes/top5`;
    window.open(url + '?format=pdf', '_blank');
  }
}

