import { Component, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { GamerService } from '../../services/gamer.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-gamer-profile',
  template: `
  <div class="gamer-profile">
    <h2>Perfil</h2>
    <form [formGroup]="profileForm" (ngSubmit)="save()">
      <div><label>Nickname:</label> <input formControlName="nickname" /></div>
      <div><label>Correo:</label> <input formControlName="email" [disabled]="true" /></div>
      <div><label>País:</label> <input formControlName="pais" /></div>
      <div><label>Teléfono:</label> <input formControlName="telefono" /></div>
      <div><label>Fecha nacimiento:</label> <input formControlName="fecha_nacimiento" [disabled]="true" /></div>
      <div style="margin-top:10px"><button type="submit">Guardar</button>
      <button type="button" (click)="recharge()">Recargar saldo</button></div>
    </form>
  </div>
  `,
  styles: [`.gamer-profile { max-width:720px } .gamer-profile label{width:140px;display:inline-block}`]
})
export class GamerProfileComponent implements OnInit {
  profileForm = this.fb.group({ nickname: [''], email: [''], pais: [''], telefono: [''], fecha_nacimiento: [''] });
  user: any = null;
  constructor(private fb: FormBuilder, private gamer: GamerService, private auth: AuthService) {}

  ngOnInit(): void {
    // prefer local cache
    const locally = this.gamer.getLocalUser();
    if (locally) { this.user = locally; this.profileForm.patchValue(locally); }
    // try server profile to enrich (service supports optional userId)
    const uid = this.user?.id || null;
    this.gamer.getProfile(uid).subscribe((p:any)=>{ if(p){ this.user = p; this.profileForm.patchValue(p); } }, ()=>{});
  }

  save(){
    const v = this.profileForm.value;
    const payload = { nickname: v.nickname, pais: v.pais, telefono: v.telefono };
    this.gamer.updateProfile(this.user?.id || null, payload).subscribe((res:any)=>{
      try{ const u = Object.assign({}, this.auth.getCurrentUser()||{}, payload); localStorage.setItem('tienda_user', JSON.stringify(u)); (this.auth as any).currentUserSubject?.next(u); }catch(e){}
      alert('Perfil guardado');
    }, ()=>{ alert('No se pudo guardar el perfil en el servidor, guardado local.');
      const u = Object.assign({}, this.user||{}, payload); localStorage.setItem('tienda_user', JSON.stringify(u)); (this.auth as any).currentUserSubject?.next(u);
    });
  }

  recharge(){
    const uid = this.user && this.user.id ? this.user.id : null;
    if(!uid){ alert('Usuario no disponible'); return; }
    const amount = Number(prompt('Cantidad a recargar', '10')) || 0;
    this.gamer.rechargeWallet(uid, amount).subscribe((r:any)=>{ alert('Recarga solicitada'); }, ()=>{ alert('Recarga encolada localmente'); });
  }
}
