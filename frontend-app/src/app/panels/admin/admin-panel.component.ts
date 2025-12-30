import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { AdminService } from '../../services/admin.service';
import { VideojuegoService } from '../../videojuegos/videojuego.service';
import { NotificationService } from '../../services/notification.service';


@Component({
  selector: 'app-admin-panel',
  template: `
    <div class="admin-header">
      <h2>Admin Panel</h2>
      <p class="welcome">{{ welcomeText }}</p>
    </div>
    <nav>
      <a routerLink="./categorias">Categorías</a> |
      <a routerLink="./banners">Banners</a> |
      <a routerLink="./comisiones">Comisiones</a> |
      <a routerLink="./juegos">Juegos</a> |
      <a routerLink="./reportes">Reportes</a>
    </nav>
    <div style="margin-top:0.5rem">
      <strong>Categorías creadas:</strong>
      <span *ngIf="!categories?.length">(ninguna)</span>
      <ng-container *ngFor="let c of categories">
        <span style="display:inline-block;margin:0.25rem;padding:0.25rem 0.5rem;background:#f1f1f1;border-radius:4px;margin-right:6px">{{c.nombre}}</span>
      </ng-container>
    </div>
    <div style="margin-top:8px;border-top:1px solid #eee;padding-top:8px">
      <label style="margin-right:6px">Nueva categoría:</label>
      <input [(ngModel)]="newCategoryName" placeholder="Nombre categoría" />
      <button (click)="createLocalCategory()">Crear local</button>
      <button (click)="deriveCategoriesFromGames()" style="margin-left:8px">Derivar desde juegos</button>
      <button (click)="refreshLocalCategories()" style="margin-left:8px">Refrescar</button>
      <div style="margin-top:6px;font-size:12px;color:#666">
        <strong>Debug local_categories:</strong> <button (click)="toggleDebug()">{{showDebug? 'Ocultar':'Mostrar'}}</button>
      </div>
      <pre *ngIf="showDebug" style="max-height:200px;overflow:auto;background:#fafafa;padding:8px;border:1px solid #eee">{{localCategoriesRaw}}</pre>
    </div>
    <router-outlet></router-outlet>
  `
})
export class AdminPanelComponent implements OnInit {
  user: any = null;
  categories: any[] = [];
  newCategoryName = '';
  showDebug = false;
  localCategoriesRaw = '';
  constructor(private auth: AuthService, private admin: AdminService, private vsvc: VideojuegoService, private notify: NotificationService){
    this.auth.currentUser$.subscribe(u=> this.user = u);
  }
  ngOnInit(){ this.loadCategories(); }
  loadCategories(){
    this.admin.listCategories().subscribe({ next: r=> {
        this.categories = r || [];
        if(!this.categories.length) this.deriveCategoriesFromGames();
      }, error: ()=> {
        try{ this.categories = JSON.parse(localStorage.getItem('local_categories')||'[]'); }catch(e){ this.categories = []; }
        if(!this.categories.length) this.deriveCategoriesFromGames();
      }
    });
  }

  deriveCategoriesFromGames(){
    this.vsvc.getAll().subscribe({ next: (games:any[])=>{
        try{
          const set = new Set((games||[]).map(g=>g.categoria).filter(Boolean));
          this.categories = Array.from(set).map(n=>({ nombre: n }));
          if(this.categories.length) localStorage.setItem('local_categories', JSON.stringify(this.categories));
        }catch(e){ /* ignore */ }
      }, error: ()=>{
        // fallback to local persisted catalogs (per-company) to derive categories
        try{
          const compsRaw = localStorage.getItem('local_companies');
          const comps = compsRaw? JSON.parse(compsRaw): [];
          const all: any[] = [];
          (comps||[]).forEach((c:any)=>{ try{ const key = `local_catalog_${c.id}`; const rc = localStorage.getItem(key); if(rc){ const arr = JSON.parse(rc); arr.forEach((it:any)=> all.push(it)); } }catch(e){} });
          const set = new Set((all||[]).map(g=>g.categoria).filter(Boolean));
          this.categories = Array.from(set).map(n=>({ nombre: n }));
          if(this.categories.length) localStorage.setItem('local_categories', JSON.stringify(this.categories));
        }catch(e){ this.categories = []; }
      }
    });
  }

  createLocalCategory(){
    const nombre = (this.newCategoryName||'').trim();
    if(!nombre) { this.notify.error('Nombre vacío'); return; }
    try{
      const key = 'local_categories';
      const arr = JSON.parse(localStorage.getItem(key)||'[]');
      const exists = (arr||[]).find((x:any)=> x.nombre && x.nombre.toLowerCase() === nombre.toLowerCase());
      if(exists){ this.notify.error('Ya existe categoría local con ese nombre'); return; }
      arr.push({ nombre });
      localStorage.setItem(key, JSON.stringify(arr));
      this.categories.push({ nombre });
      this.newCategoryName = '';
      this.notify.success('Categoría creada localmente');
      this.refreshLocalCategories();
    }catch(e){ this.notify.error('Error guardando categoría localmente'); }
  }

  refreshLocalCategories(){ try{ this.localCategoriesRaw = localStorage.getItem('local_categories') || '[]'; }catch(e){ this.localCategoriesRaw='[]'; } }

  toggleDebug(){ this.showDebug = !this.showDebug; if(this.showDebug) this.refreshLocalCategories(); }
  get welcomeText(){
    const name = this.user?.nickname || this.user?.name || this.user?.email || 'usuario';
    return `Bienvenido usuario de administración de empresas: ${name}`;
  }
}
