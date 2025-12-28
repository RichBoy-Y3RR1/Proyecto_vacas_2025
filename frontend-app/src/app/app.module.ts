import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { AuthComponent } from './auth/auth.component';
import { VideojuegosComponent } from './videojuegos/videojuegos.component';
import { TopbarComponent } from './shared/topbar/topbar.component';
import { GamerPanelComponent } from './panels/gamer/gamer-panel.component';
import { AdminPanelComponent } from './panels/admin/admin-panel.component';
import { EmpresaPanelComponent } from './panels/empresa/empresa-panel.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { AdminCategoriesComponent } from './panels/admin/admin-categories.component';
import { AdminBannersComponent } from './panels/admin/admin-banners.component';
import { AdminCommissionsComponent } from './panels/admin/admin-commissions.component';
import { AdminReportsComponent } from './panels/admin/admin-reports.component';
import { AdminGamesComponent } from './panels/admin/admin-games.component';
import { AdminService } from './services/admin.service';

const routes: Routes = [
  { path: '', component: AuthComponent },
  { path: 'videojuegos', component: VideojuegosComponent, canActivate: [AuthGuard] },
  { path: 'gamer', component: GamerPanelComponent, canActivate: [AuthGuard, RoleGuard], data: { role: ['GAMER','USUARIO'] } },
  { path: 'empresa', component: EmpresaPanelComponent, canActivate: [AuthGuard, RoleGuard], data: { role: ['EMPRESA'] } },
  { path: 'admin', component: AdminPanelComponent, canActivate: [AuthGuard, RoleGuard], data: { role: ['ADMIN'] },
    children: [
      { path: 'categorias', component: AdminCategoriesComponent },
      { path: 'juegos', component: AdminGamesComponent },
      { path: 'banners', component: AdminBannersComponent },
      { path: 'comisiones', component: AdminCommissionsComponent },
      { path: 'reportes', component: AdminReportsComponent }
    ]
  }
];

@NgModule({
  declarations: [
    AppComponent,
    AuthComponent
    ,VideojuegosComponent,
    TopbarComponent,
    GamerPanelComponent,
    AdminPanelComponent,
    EmpresaPanelComponent
    ,AdminCategoriesComponent
    ,AdminBannersComponent
    ,AdminCommissionsComponent
    ,AdminReportsComponent
    ,AdminGamesComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forRoot(routes)
  ],
  providers: [AuthGuard, RoleGuard, AdminService],
  bootstrap: [AppComponent]
})
export class AppModule { }
