import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-admin-reports',
  template: `
    <h3>Reportes básicos</h3>
    <div>Videojuegos totales: {{ videogamesCount }}</div>
    <div>Usuarios totales: {{ usersCount }}</div>
  `
})
export class AdminReportsComponent implements OnInit{
  videogamesCount = 0; usersCount = 0;
  constructor(private http: HttpClient){}
  ngOnInit(){
    this.http.get<any[]>('http://localhost:8080/tienda-backend-1.0.0/api/videojuegos').subscribe(v=> this.videogamesCount = v?.length || 0);
    this.http.get<any[]>('http://localhost:8080/tienda-backend-1.0.0/api/usuarios').subscribe(u=> this.usersCount = u?.length || 0);
  }
}
