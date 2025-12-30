import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BibliotecaComponent } from './biblioteca.component';
import { ComunidadComponent } from './comunidad.component';
import { GruposComponent } from './grupos.component';
import { PerfilComponent } from './perfil.component';

const routes: Routes = [
  { path: 'biblioteca', component: BibliotecaComponent },
  { path: 'comunidad', component: ComunidadComponent },
  { path: 'grupos', component: GruposComponent },
  { path: 'perfil', component: PerfilComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class GamerRoutingModule {}
