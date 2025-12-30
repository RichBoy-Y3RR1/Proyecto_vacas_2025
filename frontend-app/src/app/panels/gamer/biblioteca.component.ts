import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-biblioteca',
  template: `
    <h3>Biblioteca</h3>
    <div *ngIf="loading">Cargando biblioteca...</div>
    <div *ngIf="!loading && games.length===0">No tienes juegos en tu biblioteca.</div>
    <div *ngIf="!loading && games.length>0">
      <ul>
        <li *ngFor="let g of games">{{g.nombre}} <small>- {{g.fecha}}</small></li>
      </ul>
    </div>
  `
})
export class BibliotecaComponent implements OnInit{
  games: any[] = [];
  loading = false;
  constructor(private http: HttpClient){}
  ngOnInit(){ this.load(); }
  load(){
    this.loading = true;
    // simple endpoint: compras for current user (frontend should provide usuario_id via auth; here use temp user id 3)
    this.http.get<any[]>('http://localhost:8080/backend/api/compras?usuario_id=3').subscribe(r=>{ this.games = (r||[]).map(x=> ({ nombre: x.videojuego_nombre || ('Juego '+x.videojuego_id), fecha: x.fecha || x.fecha_compra || '' })); this.loading=false; }, ()=>{ this.loading=false; });
  }
}
