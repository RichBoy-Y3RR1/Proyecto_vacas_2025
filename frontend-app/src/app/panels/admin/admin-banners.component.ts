import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-admin-banners',
  template: `
    <h3>Banners</h3>
    <div>
      <input [(ngModel)]="url" placeholder="URL imagen" />
      <button (click)="crear()">Agregar</button>
    </div>
    <ul>
      <li *ngFor="let b of banners">
        <input [(ngModel)]="b.url_imagen" />
        <button (click)="guardar(b)">Guardar</button>
        <button (click)="eliminar(b)">Eliminar</button>
      </li>
    </ul>
  `
})
export class AdminBannersComponent implements OnInit{
  banners:any[] = []; url='';
  constructor(private admin: AdminService){}
  ngOnInit(){ this.load(); }
  load(){ this.admin.listBanners().subscribe(r=> this.banners = r || []); }
  crear(){ if(!this.url) return; this.admin.createBanner({ url_imagen: this.url }).subscribe(()=>{ this.url=''; this.load(); }); }
  guardar(b:any){ if(!b?.id) return; this.admin.updateBanner(b.id, { url_imagen: b.url_imagen }).subscribe(()=> this.load()); }
  eliminar(b:any){ if(!b?.id) return; if(!confirm('Eliminar banner?')) return; this.admin.deleteBanner(b.id).subscribe(()=> this.load()); }
}
