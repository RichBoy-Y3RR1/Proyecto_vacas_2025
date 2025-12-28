import { Component } from '@angular/core';
import { AuthService } from '../../auth/auth.service';


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
    <router-outlet></router-outlet>
  `
})
export class AdminPanelComponent {
  user: any = null;
  constructor(private auth: AuthService){
    this.auth.currentUser$.subscribe(u=> this.user = u);
  }
  get welcomeText(){
    const name = this.user?.nickname || this.user?.name || this.user?.email || 'usuario';
    return `Bienvenido usuario de administración de empresas: ${name}`;
  }
}
