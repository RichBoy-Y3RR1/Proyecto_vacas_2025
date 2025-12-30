import { Component, OnInit } from '@angular/core';
import { GamerService } from '../../services/gamer.service';
@Component({
  selector: 'app-perfil',
  template: `
    <h3>Perfil</h3>
    <div *ngIf="!profile">Cargando perfil...</div>
    <div *ngIf="profile">
      <p><strong>Correo:</strong> {{profile.correo}}</p>
      <p><strong>Nickname:</strong> {{profile.nickname || profile.nombre || '-'}}</p>
      <p><strong>Saldo:</strong> {{wallet?.saldo || '0.00'}}</p>
    </div>
  `
})
export class PerfilComponent implements OnInit{
  profile: any = null;
  wallet: any = null;
  loading = true;
  constructor(private gamer: GamerService){ }
  ngOnInit(){ this.load(); }
  load(){
    this.loading = true;
    this.gamer.getProfile().subscribe((p:any)=>{ if(p) this.profile = p; }, ()=>{});
    this.gamer.getWallet().subscribe((w:any)=>{ if(w) this.wallet = w; this.loading = false; }, ()=>{ this.loading = false; });
  }
}
