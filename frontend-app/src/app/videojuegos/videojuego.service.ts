import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class VideojuegoService {
  // Use relative proxy path so dev server proxy handles requests and avoids CORS
  // direct backend URL to avoid proxy mismatch
  private base = 'http://localhost:8080/backend/api/videojuegos';
  constructor(private http: HttpClient) {}
  getAll(): Observable<any[]> { return this.http.get<any[]>(this.base); }
  getById(id:number){
    // if item id is negative it is a locally-stored demo; avoid backend call which may 404
    if(id < 0){
      return of({ id, nombre: 'Demo local', descripcion: '', precio: 0, url_imagen: 'https://via.placeholder.com/320x180?text=Demo' });
    }
    return this.http.get(`${this.base}/${id}`);
  }
  create(payload:any){ return this.http.post(this.base, payload); }
  update(id:number, payload:any){ return this.http.put(`${this.base}/${id}`, payload); }
  toggleForSale(id:number, forSale:boolean){ return this.http.patch(`${this.base}/${id}`, { for_sale: forSale }); }
  delete(id:number){ return this.http.delete(`${this.base}/${id}`); }
}

