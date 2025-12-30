import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';
import { VideojuegoService } from '../../videojuegos/videojuego.service';
import { NotificationService } from '../../services/notification.service';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

interface Categoria { id?: number; nombre: string; descripcion?: string }

@Component({
  selector: 'app-admin-categories',
  template: `
    <h3>Categorías</h3>
    <div>
      <input [(ngModel)]="nuevoNombre" placeholder="Nombre" />
      <input [(ngModel)]="nuevoDesc" placeholder="Descripción" />
      <button (click)="crear()">Crear</button>
    </div>
    <ul>
      <li *ngFor="let c of categorias">
        <input [(ngModel)]="c.nombre" /> - <input [(ngModel)]="c.descripcion" />
        <button (click)="guardar(c)">Guardar</button>
        <button (click)="eliminar(c)">Eliminar</button>
      </li>
    </ul>
  `
})
export class AdminCategoriesComponent implements OnInit{
  categorias: Categoria[] = [];
  nuevoNombre=''; nuevoDesc='';
  loading = false;
  games: any[] = [];
  constructor(private admin: AdminService, private notify: NotificationService, private vsvc: VideojuegoService){}
  ngOnInit(){ this.load(); }
  load(){
    this.loading = true;
    this.admin.listCategories().pipe(
      catchError(()=> { this.notify.error('No se pudieron cargar categorías.'); return of([]); }),
      finalize(()=> this.loading = false)
    ).subscribe(r=> this.categorias = r || []);
    // also load games to prevent deleting categories in use
    try{ this.vsvc.getAll().subscribe({ next: g=> this.games = g || [], error: ()=> this.games = [] }); }catch(e){ this.games = []; }
  }
  crear(){ if(!this.nuevoNombre) return; this.loading = true;
    // prevent duplicate names (case-insensitive)
    const exists = this.categorias.find(c=> c.nombre?.toLowerCase() === this.nuevoNombre.toLowerCase());
    if(exists){ this.notify.error('Ya existe una categoría con ese nombre'); this.loading=false; return; }
    const payload = { nombre:this.nuevoNombre, descripcion:this.nuevoDesc };
    this.admin.createCategory(payload).pipe(
      catchError(()=> { // persist locally
        try{ const key='local_categories'; const arr = JSON.parse(localStorage.getItem(key)||'[]'); arr.push(payload); localStorage.setItem(key, JSON.stringify(arr)); this.notify.success('Categoría guardada localmente'); }catch(e){ this.notify.error('No se pudo guardar categoría localmente'); }
        return of(null);
      }),
      finalize(()=> { this.loading=false; this.nuevoNombre=''; this.nuevoDesc=''; this.load(); })
    ).subscribe(()=> this.notify.success('Categoría creada'));
  }
  guardar(c: Categoria){ if(!c?.id) return; this.loading = true;
    // prevent duplicate names on update
    const dup = this.categorias.find(x=> x.id!==c.id && x.nombre?.toLowerCase() === c.nombre?.toLowerCase());
    if(dup){ this.notify.error('Otro registro ya tiene ese nombre'); this.loading=false; return; }
    this.admin.updateCategory(c.id!, { nombre:c.nombre, descripcion:c.descripcion }).pipe(
      catchError(()=> { this.notify.error('No se pudo actualizar categoría'); return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=>{ this.notify.success('Categoría actualizada'); const idx = this.categorias.findIndex(x=> x.id===c.id); if(idx>=0) this.categorias[idx] = c; });
  }
  async eliminar(c: Categoria){ if(!c?.id){ this.notify.error('Categoría inválida'); return; } if(!await this.notify.confirm('Eliminar categoría?')) return; this.loading = true;
    // prevent delete if any active game uses this category
    const inUse = (this.games||[]).some(g=> g.categoria && g.categoria === c.nombre && (g.for_sale || g.estado==='PUBLICADO'));
    if(inUse){ this.notify.error('No se puede eliminar: existen juegos activos asignados a esta categoría'); this.loading=false; return; }
    this.admin.deleteCategory(c.id!).pipe(
      catchError(()=> { this.notify.error('No se pudo eliminar categoría del backend.'); return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=>{ this.categorias = this.categorias.filter(x=> x.id !== c.id); this.notify.success('Categoría eliminada'); });
  }
}
