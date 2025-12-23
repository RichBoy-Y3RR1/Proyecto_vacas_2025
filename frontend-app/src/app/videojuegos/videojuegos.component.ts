import { Component, OnInit } from '@angular/core';
import { VideojuegoService } from './videojuego.service';

@Component({
  selector: 'app-videojuegos',
  templateUrl: './videojuegos.component.html',
  styleUrls: ['./videojuegos.component.css']
})
export class VideojuegosComponent implements OnInit {
  videojuegos: any[] = [];
  loading = false;
  error: string | null = null;
  // form
  nombre = '';
  descripcion = '';
  precio: number | null = null;
  creating = false;
  currentUser: any = null;
  canCreate = false;

  constructor(private svc: VideojuegoService) {}

  ngOnInit(): void { this.load(); }

  private loadAuth(){
    try{
      const s = localStorage.getItem('auth');
      if (s) { this.currentUser = JSON.parse(s).user || JSON.parse(s); }
    }catch(e){ this.currentUser = null }
    this.canCreate = !!(this.currentUser && (this.currentUser.role==='ADMIN' || this.currentUser.role==='EMPRESA'));
  }

  load(){
    this.loadAuth();
    this.loading = true; this.error = null;
    this.svc.getAll().subscribe({
      next: data => { this.videojuegos = data || []; this.loading = false; },
      error: err => { this.error = 'No se pudieron cargar videojuegos.'; this.loading = false; }
    });
  }

  create(){
    this.error = null;
    if(!this.nombre || !this.precio){ this.error = 'Nombre y precio son obligatorios.'; return; }
    if(!this.canCreate){ this.error = 'No tienes permiso para crear videojuegos.'; return }
    this.creating = true;
    const payload: any = { nombre: this.nombre, descripcion: this.descripcion, precio: this.precio };
    this.svc.create(payload).subscribe({
      next: _ => { this.nombre=''; this.descripcion=''; this.precio=null; this.load(); },
      error: e => { this.error = 'Error al crear videojuego.'; this.creating=false },
      complete: () => { this.creating=false }
    });
  }
}
