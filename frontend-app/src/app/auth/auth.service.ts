import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private base = 'http://localhost:8080/tienda-backend-1.0.0/api/auth';
  constructor(private http: HttpClient) {}
  login(email: string, password: string): Observable<any>{
    return this.http.post(`${this.base}/login`, { email, password });
  }
  register(payload: any): Observable<any>{
    return this.http.post(`${this.base}/register`, payload);
  }
  listUsers(): Observable<any[]>{
    return this.http.get<any[]>('http://localhost:8080/tienda-backend-1.0.0/api/usuarios');
  }
}
