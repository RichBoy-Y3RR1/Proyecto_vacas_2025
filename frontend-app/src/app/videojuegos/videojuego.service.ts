import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class VideojuegoService {
  private base = 'http://localhost:8080/tienda-backend-1.0.0/api/videojuegos';
  constructor(private http: HttpClient) {}
  getAll(): Observable<any[]> { return this.http.get<any[]>(this.base); }
  getById(id:number){ return this.http.get(`${this.base}/${id}`); }
  create(payload:any){ return this.http.post(this.base, payload); }
}
