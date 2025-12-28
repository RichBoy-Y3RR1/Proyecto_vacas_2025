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
  `
})
export class AdminReportsComponent implements OnInit{
  videogamesCount = 0; usersCount = 0;
  loading = false;
  constructor(private http: HttpClient, private notify: NotificationService){}
  ngOnInit(){ this.loadCounts(); }
  loadCounts(){
    this.loading = true;
    this.http.get<any[]>('http://localhost:8080/tienda-backend-1.0.0/api/videojuegos').pipe(
      catchError(()=>{ this.notify.error('No se pudo cargar conteo de videojuegos; fallback local.'); try{ const raw = localStorage.getItem('local_catalog_all'); return of(raw? JSON.parse(raw): []);}catch(e){ return of([]);} }),
      finalize(()=> this.loading=false)
    ).subscribe(v=> this.videogamesCount = v?.length || 0);

    this.http.get<any[]>('http://localhost:8080/tienda-backend-1.0.0/api/usuarios').pipe(
      catchError(()=>{ this.notify.error('No se pudo cargar conteo de usuarios; fallback local.'); try{ const raw = localStorage.getItem('local_users_all'); return of(raw? JSON.parse(raw): []);}catch(e){ return of([]);} }),
    ).subscribe(u=> this.usersCount = u?.length || 0);
  }
}
