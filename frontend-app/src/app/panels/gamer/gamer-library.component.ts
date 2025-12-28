import { Component, OnInit } from '@angular/core';
import { GamerService } from '../../services/gamer.service';

@Component({
  selector: 'app-gamer-library',
  template: `
  <div class="gamer-library">
    <h2>Biblioteca</h2>
    <div *ngIf="games.length===0">No hay juegos en la biblioteca.</div>
    <ul>
      <li *ngFor="let g of games">
        <img [src]="g.url_imagen || 'https://via.placeholder.com/120x68'" width="120" />
        <div style="display:inline-block;margin-left:8px">
          <div><strong>{{g.nombre || g.titulo}}</strong> <small>({{g.categoria}})</small></div>
          <div>Estado: {{g.estado || 'NO_INSTALADO'}}</div>
          <div><button (click)="toggleState(g)">{{g.estado==='INSTALADO' ? 'Desinstalar' : 'Instalar'}}</button></div>
        </div>
      </li>
    </ul>
  </div>
  `,
  styles: [`.gamer-library ul{list-style:none;padding:0} .gamer-library li{margin:10px 0;display:flex;align-items:center}`]
})
export class GamerLibraryComponent implements OnInit {
  games: any[] = [];
  constructor(private gamer: GamerService) {}
  ngOnInit(): void {
    const u = this.gamer.getLocalUser();
    const uid = u && u.id ? u.id : null;
    if(!uid){ this.games = []; return; }
    this.gamer.getLibrary(uid).subscribe((list:any[])=>{ this.games = list || []; });
  }
  toggleState(g:any){
    const newState = g.estado === 'INSTALADO' ? 'NO_INSTALADO' : 'INSTALADO';
    this.gamer.updateGameState(g.id, newState).subscribe(()=>{ g.estado = newState; }, ()=>{ g.estado = newState; });
  }
}
