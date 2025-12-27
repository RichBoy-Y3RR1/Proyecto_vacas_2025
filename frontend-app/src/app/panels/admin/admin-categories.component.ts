import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../services/admin.service';

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
  categorias: any[] = [];
  nuevoNombre=''; nuevoDesc='';
  constructor(private admin: AdminService){}
  ngOnInit(){ this.load(); }
  load(){ this.admin.listCategories().subscribe(r=> this.categorias = r || []); }
  crear(){ if(!this.nuevoNombre) return; this.admin.createCategory({ nombre:this.nuevoNombre, descripcion:this.nuevoDesc }).subscribe(()=>{ this.nuevoNombre=''; this.nuevoDesc=''; this.load(); }); }
  guardar(c:any){ if(!c?.id) return; this.admin.updateCategory(c.id, { nombre:c.nombre, descripcion:c.descripcion }).subscribe(()=> this.load()); }
  eliminar(c:any){ if(!c?.id) return; if(!confirm('Eliminar categoría?')) return; this.admin.deleteCategory(c.id).subscribe(()=> this.load()); }
}
