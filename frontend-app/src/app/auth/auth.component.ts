import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent {
  message = '';
  user: any = null;
  loginForm = this.fb.group({ email:['', [Validators.required, Validators.email]], password:['', Validators.required] });
  regForm = this.fb.group({ role:['USUARIO'], email:['', [Validators.required, Validators.email]], password:['', Validators.required], nickname:[''], name:[''] });
  constructor(private fb: FormBuilder, private auth: AuthService){
    const s = localStorage.getItem('auth'); if (s) this.user = JSON.parse(s).user;
  }
  submitLogin(){
    if (this.loginForm.invalid){ this.message = 'Completa email y contraseña válidos'; return }
    this.message = 'Iniciando sesión...';
    const v = this.loginForm.value;
    const email = String(v.email || '');
    const password = String(v.password || '');
    this.auth.login(email, password).subscribe({ next: r=>{ localStorage.setItem('auth', JSON.stringify(r)); this.user = r.user; this.message = 'Bienvenido '+(r.user?.nickname||r.user?.email); }, error: e=>{ this.message = 'Credenciales inválidas' } });
  }
  submitRegister(){
    // kept for backward compatibility; delegate to specific helpers
    const role = this.regForm.value.role || 'USUARIO';
    if (role === 'USUARIO') return this.registerGamer();
    return this.registerEmpresa();
  }

  registerGamer(){
    const v = this.regForm.value;
    if (!v.email || !v.password) { this.message='Completa email y contraseña para gamer'; return }
    this.message = 'Registrando gamer...';
    const payload = { role: 'USUARIO', email: String(v.email), password: String(v.password), nickname: v.nickname || ('gamer'+Math.floor(Math.random()*1000)) };
    this.auth.register(payload).subscribe({ next: r=>{ this.message = 'Gamer registrado'; const auth = { token:'demo-token', user:{ email:payload.email, role:payload.role, nickname:payload.nickname } }; localStorage.setItem('auth', JSON.stringify(auth)); this.user = auth.user; }, error: e=>{ this.message = 'Error al registrar gamer' } });
  }

  registerEmpresa(){
    const v = this.regForm.value;
    if (!v.email || !v.password) { this.message='Completa email y contraseña para empresa'; return }
    this.message = 'Registrando empresa...';
    const payload = { role: 'EMPRESA', email: String(v.email), password: String(v.password), name: v.name || ('Empresa '+Math.floor(Math.random()*100)) };
    this.auth.register(payload).subscribe({ next: r=>{ this.message = 'Empresa registrada'; const auth = { token:'demo-token', user:{ email:payload.email, role:payload.role, name:payload.name } }; localStorage.setItem('auth', JSON.stringify(auth)); this.user = auth.user; }, error: e=>{ this.message = 'Error al registrar empresa' } });
  }

  // Admin helper: fetch users
  users: any[] = [];
  loadUsers(){
    this.message = 'Cargando usuarios...';
    this.auth.listUsers().subscribe({ next: u=>{ this.users = u || []; this.message = 'Usuarios cargados: '+this.users.length }, error: e=>{ this.message = 'Error cargando usuarios' } });
  }
  logout(){ localStorage.removeItem('auth'); this.user = null; this.message='Sesión cerrada' }
}
