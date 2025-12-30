import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';
import { NotificationService } from '../../services/notification.service';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';

interface Banner { id?: number; url_imagen: string; titulo?: string; active?: boolean; position?: number }

@Component({
  selector: 'app-admin-banners',
  template: `
    <h3>Banners</h3>
    <div>
      <input [(ngModel)]="url" placeholder="URL imagen" />
      <button (click)="crear()">Agregar</button>
    </div>
    <ul>
      <li *ngFor="let b of banners" style="display:flex;align-items:center;gap:12px;margin:8px 0">
        <img [src]="b.url_imagen" alt="banner" style="width:160px;height:90px;object-fit:cover;border:1px solid #ddd" />
        <div style="flex:1">
          <input [(ngModel)]="b.url_imagen" style="width:100%" />
        </div>
        <div style="display:flex;flex-direction:column;gap:6px">
          <button (click)="guardar(b)">Guardar</button>
          <button (click)="eliminar(b)">Eliminar</button>
        </div>
      </li>
    </ul>
  `
})
export class AdminBannersComponent implements OnInit{
  banners: Banner[] = [];
  url = '';
  loading = false;
  constructor(private admin: AdminService, private notify: NotificationService){}
  ngOnInit(){ this.load(); }
  load(){
    this.loading = true;
    this.admin.listBanners().pipe(
      catchError(err => { this.notify.error('No se pudieron cargar los banners (fallback local).');
        try{ const raw = localStorage.getItem('local_banners'); return of(raw? JSON.parse(raw): []); }catch(e){ return of([]); }
      }),
      finalize(()=> this.loading = false)
    ).subscribe((r:any[])=> this.banners = r || []);
    // sort by position if present
    try{ this.banners.sort((a,b)=> (a.position||0)-(b.position||0)); }catch(e){}
  }
  crear(){ if(!this.url) return; this.loading = true;
    // validate url basic
    const urlRegex = /^https?:\/\/.+\.(png|jpg|jpeg|gif|webp)$/i;
    if(!urlRegex.test(this.url)){ this.notify.error('URL inválida (debe apuntar a una imagen)'); this.loading=false; return; }
    const payload = { url_imagen: this.url, titulo: '', active: true, position: (this.banners.length? (Math.max(...this.banners.map(b=>b.position||0))+1): 0) };
    this.admin.createBanner(payload).pipe(
      catchError(err => { // persist locally
        try{ const key = 'local_banners'; const arr = JSON.parse(localStorage.getItem(key) || '[]'); arr.push(payload); localStorage.setItem(key, JSON.stringify(arr)); this.notify.success('Banner guardado localmente (backend no disponible).'); }catch(e){ this.notify.error('No se pudo guardar el banner localmente.'); }
        return of(null);
      }),
      finalize(()=> { this.loading = false; this.url=''; this.load(); })
    ).subscribe(()=>{ this.notify.success('Banner guardado'); });
  }
  guardar(b: Banner){ if(!b?.id) return; this.loading = true; this.admin.updateBanner(b.id!, { url_imagen: b.url_imagen }).pipe(
    catchError(()=> { this.notify.error('No se pudo actualizar el banner en el backend.'); return of(null); }),
    finalize(()=> { this.loading = false; })
  ).subscribe(()=>{ this.notify.success('Banner actualizado'); const idx = this.banners.findIndex(x=> x.id===b.id); if(idx>=0) this.banners[idx] = b; });
  }
  async eliminar(b: Banner){ if(!b?.id){ this.notify.error('Banner inválido'); return; } if(!await this.notify.confirm('Eliminar banner?')) return; this.loading = true;
    this.admin.deleteBanner(b.id!).pipe(
      catchError(()=> { this.notify.error('No se pudo eliminar en backend; eliminando localmente si existe.'); try{ const key='local_banners'; const arr = JSON.parse(localStorage.getItem(key)||'[]').filter((x:any)=> x.url_imagen !== b.url_imagen); localStorage.setItem(key, JSON.stringify(arr)); }catch(e){} return of(null); }),
      finalize(()=> this.loading=false)
    ).subscribe(()=>{ this.banners = this.banners.filter(x=> x.id !== b.id); this.notify.success('Banner eliminado'); });
  }

  // ordering helpers
  moveUp(index: number): void {
    if (index <= 0) return;
    const tmp = this.banners[index - 1];
    this.banners[index - 1] = this.banners[index];
    this.banners[index] = tmp;
    this.reassignPositions();
  }

  moveDown(index: number): void {
    if (index >= this.banners.length - 1) return;
    const tmp = this.banners[index + 1];
    this.banners[index + 1] = this.banners[index];
    this.banners[index] = tmp;
    this.reassignPositions();
  }

  toggleActive(b: Banner): void {
    b.active = !b.active;
    if (b.id) {
      this.admin.updateBanner(b.id, { active: b.active }).subscribe(
        () => this.notify.success('Estado actualizado'),
        () => this.notify.error('No se pudo actualizar estado')
      );
    } else {
      try {
        const key = 'local_banners';
        const arr = JSON.parse(localStorage.getItem(key) || '[]');
        const idx = arr.findIndex((x: any) => x.url_imagen === b.url_imagen);
        if (idx >= 0) {
          arr[idx].active = b.active;
          localStorage.setItem(key, JSON.stringify(arr));
        }
      } catch (e) {}
    }
  }

  reassignPositions(): void {
    this.banners.forEach((b, i) => (b.position = i));
    // persist ordering to backend where possible
    this.banners.forEach((b) => {
      if (b.id) {
        this.admin.updateBanner(b.id!, { position: b.position }).subscribe({ next: () => {}, error: () => {} });
      }
    });
  }
}
