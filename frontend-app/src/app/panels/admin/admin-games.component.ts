import { Component, OnInit } from '@angular/core';
import { VideojuegoService } from '../../videojuegos/videojuego.service';
import { AdminService } from '../../services/admin.service';
import { EmpresaService } from '../../services/empresa.service';
import { NotificationService } from '../../services/notification.service';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

interface Videojuego { id: number; nombre: string; descripcion?: string; precio?: number; url_imagen?: string; categoria?: string; empresa_id?: number | null; estado?: string; for_sale?: boolean; __selected?: boolean }

@Component({
  selector: 'app-admin-games',
  template: `
    <h3>Gestión de Videojuegos</h3>
    <div *ngIf="loading">Cargando juegos...</div>
    <div *ngIf="!loading">
        <div style="margin-bottom:1rem;display:flex;gap:8px;align-items:center">
          <label style="margin-right:8px"><input type="checkbox" [(ngModel)]="selectAll" (change)="toggleSelectAll()" /> Seleccionar todos</label>
          <label>Categoría:</label>
          <select [(ngModel)]="filterCategory">
            <option value="">(Todas)</option>
            <option *ngFor="let c of categories" [value]="c.nombre">{{c.nombre}}</option>
          </select>
          <label>Empresa:</label>
          <select [(ngModel)]="filterCompanyId">
            <option value="">(Todas)</option>
            <option *ngFor="let co of companies" [value]="co.id">{{co.nombre}}</option>
          </select>
          <button (click)="toggleSortByCategory()">Ordenar por Categoría</button>
          <button class="btn btn-primary" (click)="approveSelected()" [disabled]="!anySelected()">Publicar a gamer (seleccionados)</button>
          <button class="btn btn-primary" (click)="transferSelected()" [disabled]="!anySelected()">Transferir seleccionados a Admin</button>
          <button class="btn btn-success" (click)="approveAll()">Aprobar todo (publicar a gamer)</button>
          <button (click)="load()">Refrescar</button>
          <button (click)="processTransferQueue()">Procesar cola de transferencias</button>
        </div>
      <table class="table">
        <thead>
          <tr>
            <th></th>
            <th>Id</th>
            <th>Nombre</th>
            <th>Empresa</th>
            <th>Categoría</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let g of getFilteredGames()">
            <td><input type="checkbox" [(ngModel)]="g.__selected" (change)="onSelectChange()" /></td>
            <td>{{g.id}}</td>
            <td>
              <div style="display:flex;align-items:center;gap:8px">
                <img [src]="g.url_imagen||'https://via.placeholder.com/96x54?text=No+Image'" alt="img" width="96" height="54" />
                <div>{{g.nombre}}</div>
              </div>
            </td>
            <td>{{companyName(g.empresa_id)}}</td>
            <td>
              <select [(ngModel)]="g.categoria">
                <option *ngFor="let c of categories" [value]="c.nombre">{{c.nombre}}</option>
              </select>
            </td>
            <td>{{g.estado || (g.for_sale? 'A la venta':'Suspendido')}}</td>
              <td>
              <button (click)="saveCategory(g)">Guardar Cat.</button>
              <button *ngIf="g.estado!=='PUBLICADO'" (click)="publishToGamers(g)">Publicar a gamer</button>
              <button *ngIf="g.estado==='PUBLICADO'" (click)="suspendFromGamers(g)">Suspender a gamer</button>
              <button *ngIf="g.empresa_id" (click)="sendToGamer(g)">Enviar a gamer</button>
              <button (click)="remove(g)">Eliminar</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class AdminGamesComponent implements OnInit{
  games: any[] = [];
  categories: any[] = [];
  companies: any[] = [];
  loading = false;
  selectAll = false;
  filterCategory = '';
  filterCompanyId: any = '';
  sortByCategory = false;
  constructor(private vsvc: VideojuegoService, private admin: AdminService, private emp: EmpresaService, private notify: NotificationService){}
  ngOnInit(){ this.load(); this.loadCategories(); this.loadCompanies(); }
  load(){
    this.loading = true;
    this.vsvc.getAll().pipe(
      catchError(()=> { this.notify.error('No se pudieron cargar juegos desde el backend. Usando fallback local.'); this.fallbackLocal(); return of([]); }),
      finalize(()=> this.loading = false)
    ).subscribe((r:any[])=>{ this.games = r || []; if(!this.games.length) this.fallbackLocal(); });
  }
  loadCategories(){ this.admin.listCategories().subscribe({ next: r=> this.categories = r || [], error: ()=>{ /* derive from games later */ } }); }
  loadCompanies(){ this.emp.listCompanies().subscribe({ next: r=> this.companies = r || [], error: ()=>{ try{ const raw = localStorage.getItem('local_companies'); this.companies = raw? JSON.parse(raw): []; }catch(e){ this.companies = []; } } }); }
  fallbackLocal(){
    // gather local catalogs from persisted companies
    try{
      const raw = localStorage.getItem('local_companies');
      const comps = raw? JSON.parse(raw): [];
      const all: any[] = [];
      (comps||[]).forEach((c:any)=>{
        try{
          const key = `local_catalog_${c.id}`;
          const rc = localStorage.getItem(key);
          if(rc){ const arr = JSON.parse(rc); arr.forEach((it:any)=>{ it.empresa_id = c.id; all.push(it); }); }
        }catch(e){}
      });
      // also include any globally stored local demos saved by empresa panel under key 'local_demo_games'
      try{ const demos = JSON.parse(localStorage.getItem('local_demo_games')||'[]'); demos.forEach((d:any)=> all.push(d)); }catch(e){}
      this.games = all;
      // if no categories from backend, derive list
      if(!this.categories.length){ const set = new Set(this.games.map(g=>g.categoria).filter(Boolean)); this.categories = Array.from(set).map(n=>({ nombre:n })); }
    }catch(e){ this.games = []; }
  }
  getFilteredGames(){
    let arr = (this.games||[]).slice();
    if(this.filterCompanyId){ const id = Number(this.filterCompanyId); arr = arr.filter(g=> (g.empresa_id||null) === id); }
    if(this.filterCategory){ arr = arr.filter(g=> (g.categoria||'') === this.filterCategory); }
    if(this.sortByCategory){ arr.sort((a,b)=> (a.categoria||'').localeCompare(b.categoria||'')); }
    return arr;
  }
  companyName(id:any){ const f = this.companies.find(x=>x.id===id); return f? f.nombre : (id? 'Empresa '+id : 'N/A'); }
  saveCategory(g:any){
    if(!g) return;
    if(g.id>0){
      this.vsvc.update(g.id, { categoria: g.categoria }).subscribe(()=>{ this.ensureLocalCategoryExists(g.categoria); alert('Categoría guardada'); });
    } else {
      this.updateLocalGame(g);
      this.ensureLocalCategoryExists(g.categoria);
      alert('Categoría guardada localmente');
    }
  }
  ensureLocalCategoryExists(nombre:string){ if(!nombre) return; try{ const key='local_categories'; const arr = JSON.parse(localStorage.getItem(key)||'[]') as any[]; const exists = arr.find(x=> x.nombre && x.nombre.toLowerCase() === nombre.toLowerCase()); if(!exists){ arr.push({ nombre }); localStorage.setItem(key, JSON.stringify(arr)); this.categories.push({ nombre }); } }catch(e){} }
  approve(g:any){ if(g.id>0){ this.vsvc.update(g.id, { estado: 'PUBLICADO' }).subscribe(()=>{ alert('Juego aprobado'); this.load(); }); } else { g.estado = 'PUBLICADO'; this.updateLocalGame(g); alert('Aprobado localmente'); } }
  transferToAdmin(g:any){
    // if backend item -> update empresa_id to null (transfer ownership to admin)
    if(g.id>0){
      this.vsvc.update(g.id, { empresa_id: null }).pipe(
        catchError(()=> { this.notify.error('No se pudo transferir al backend.'); return of(null); })
      ).subscribe(()=>{ this.notify.success('Juego transferido a admin'); this.load(); });
      return;
    }
    // local/demo item -> create in backend as admin-owned
    const payload = { nombre: g.nombre, descripcion: g.descripcion, precio: g.precio, url_imagen: g.url_imagen, categoria: g.categoria, empresa_id: null, estado: g.estado || 'PUBLICADO' };
    this.vsvc.create(payload).pipe(
      catchError(()=> {
        try{ const key = 'admin_transfer_queue'; const q = JSON.parse(localStorage.getItem(key)||'[]'); q.push(payload); localStorage.setItem(key, JSON.stringify(q)); this.notify.error('No se pudo enviar al backend; transferencia encolada localmente.'); }catch(e){ this.notify.error('Fallo al encolar la transferencia.'); }
        return of(null);
      })
    ).subscribe(()=>{ this.removeLocalGame(g); this.notify.success('Juego enviado a catálogo admin'); this.load(); });
  }
  remove(g:any){ if(!confirm('Eliminar juego?')) return; if(g.id>0){ this.vsvc.delete(g.id).subscribe(()=>{ alert('Eliminado'); this.load(); }); } else { this.removeLocalGame(g); alert('Eliminado localmente'); this.load(); } }
  updateLocalGame(g:any){ try{ const key = `local_catalog_${g.empresa_id}`; const arr = JSON.parse(localStorage.getItem(key)||'[]') as any[]; const idx = arr.findIndex(x=>x.id===g.id); if(idx>=0) arr[idx] = g; else arr.push(g); localStorage.setItem(key, JSON.stringify(arr)); }catch(e){} }
  removeLocalGame(g:any){ try{ const key = `local_catalog_${g.empresa_id}`; const arr = JSON.parse(localStorage.getItem(key)||'[]') as any[]; const filtered = arr.filter(x=>x.id!==g.id); localStorage.setItem(key, JSON.stringify(filtered)); }catch(e){} }

  // process queued transfers saved when backend was unavailable
  processTransferQueue(){ try{ const key='admin_transfer_queue'; const q = JSON.parse(localStorage.getItem(key)||'[]') as any[]; if(!q.length){ this.notify.success('No hay transferencias en cola'); return; } q.forEach(item=>{ this.vsvc.create(item).subscribe({ next: ()=>{ /* could track successes */ }, error: ()=>{ /* keep in queue */ } }); }); localStorage.removeItem(key); this.notify.success('Reintento de transferencias iniciado'); }catch(e){ this.notify.error('Error procesando la cola de transferencias'); } }

  // Selection helpers for bulk actions
  onSelectChange(){ this.selectAll = this.games.length>0 && this.games.every(g=>g.__selected); }
  toggleSelectAll(){ this.games.forEach(g=> g.__selected = this.selectAll); }
  anySelected(){ return this.games.some(g=> g.__selected); }
  approveSelected(){ const toApprove = this.games.filter(g=> g.__selected); toApprove.forEach(g=> this.approve(g)); }
  transferSelected(){ const selected = this.games.filter(g=> g.__selected); if(!selected.length) return; selected.forEach(g=> this.transferToAdmin(g)); }

  publishToGamers(g:any){ if(g.id>0){ this.vsvc.toggleForSale(g.id, true).subscribe(()=>{ this.notify.success('Publicado a gamers'); this.load(); }, ()=> this.notify.error('No se pudo publicar') ); } else { g.estado='PUBLICADO'; this.updateLocalGame(g); this.notify.success('Publicado localmente'); } }
  suspendFromGamers(g:any){ if(g.id>0){ this.vsvc.toggleForSale(g.id, false).subscribe(()=>{ this.notify.success('Suspendido para gamers'); this.load(); }, ()=> this.notify.error('No se pudo suspender') ); } else { g.estado='SUSPENDIDO'; this.updateLocalGame(g); this.notify.success('Suspendido localmente'); } }
  approveAll(){ if(!confirm('¿Aprobar (publicar) todos los juegos pendientes?')) return; this.vsvc.approveAll().subscribe(()=>{ this.notify.success('Todos los juegos pendientes fueron publicados'); this.load(); }, ()=> this.notify.error('No se pudo aprobar todo')); }

  sendToGamer(g:any){
    // prompt admin for target usuario id
    const val = prompt('ID del usuario destinatario (usuario_id) para enviar/gift el juego:');
    if(!val) return;
    const uid = Number(val);
    if(!uid || uid<=0){ alert('usuario_id inválido'); return; }
    if(g.id && g.id>0){
      this.vsvc.sendToUser(g.id, uid).subscribe({ next: (res:any)=>{ this.notify.success('Juego enviado/gifted al usuario'); this.load(); }, error: (err:any)=>{ this.notify.error('No se pudo enviar al usuario: '+ (err?.error || err?.message || 'error')); } });
    } else {
      // local demo: create backend copy then call send
      const payload = { nombre: g.nombre, descripcion: g.descripcion, precio: g.precio, url_imagen: g.url_imagen, categoria: g.categoria, empresa_id: null, estado: g.estado || 'PUBLICADO' };
      this.vsvc.create(payload).pipe(catchError(()=> { this.notify.error('No se pudo crear juego en backend para enviar'); return of(null); })).subscribe((r:any)=>{ if(r && r.id){ this.vsvc.sendToUser(r.id, uid).subscribe(()=>{ this.notify.success('Juego enviado/gifted al usuario'); this.load(); }, ()=> this.notify.error('No se pudo enviar tras crear juego')); } else { this.notify.error('No se pudo crear el juego en backend'); } });
    }
  }

  toggleSortByCategory(){ this.sortByCategory = !this.sortByCategory; }
}
