import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from './auth.service';
import { EmpresaService } from '../services/empresa.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css']
})
export class AuthComponent {
  message = '';
  user: any = null;
  showRegister = false;
  registering = false;
  loginForm = this.fb.group({ email:['', [Validators.required, Validators.email]], password:['', Validators.required] });
  regForm = this.fb.group({ role:['USUARIO'], email:['', [Validators.required, Validators.email]], password:['', Validators.required], nickname:[''], name:[''] });
  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router, private empresaSvc: EmpresaService){
    this.auth.currentUser$.subscribe(u=> this.user = u);
    // expose as dynamic property for backward compatibility with code paths that try lazy access
    (this as any).empresaSvc = this.empresaSvc;
  }
  submitLogin(){
    if (this.loginForm.invalid){ this.message = 'Completa email y contraseña válidos'; return }
    this.message = 'Iniciando sesión...';
    const v = this.loginForm.value;
    const email = String(v.email || '');
    const password = String(v.password || '');
    this.auth.login(email, password).subscribe({ next: r=>{
        const user = r.user || this.auth.getCurrentUser();
        if (!user) { this.message = 'Error iniciando sesión (usuario no encontrado)'; return }
        const display = user?.nickname || user?.name || user?.email || 'usuario';
        this.message = 'Bienvenido '+display;
        // normalize role values and redirect centrally
        const role = (user && user.role) ? String(user.role).toUpperCase() : 'USUARIO';
        if (role === 'ADMIN') this.router.navigate(['/admin']);
        else if (role === 'EMPRESA' || role === 'COMPANY') this.router.navigate(['/empresa']);
        else this.router.navigate(['/gamer']);
      }, error: e=>{ 
        // show friendly message from service when available
        this.message = e && e.message ? e.message : 'Credenciales inválidas';
      } });
  }
  submitRegister(){
    // kept for backward compatibility; delegate to specific helpers
    const role = this.regForm.value.role || 'USUARIO';
    if (role === 'USUARIO') return this.registerGamer();
    return this.registerEmpresa();
  }

  registerGamer(){
    if (this.registering) return;
    const v = this.regForm.value;
    if (!v.email || !v.password) { this.message='Completa email y contraseña para gamer'; return }
    this.registering = true;
    this.message = 'Registrando gamer...';
    const payload = { role: 'USUARIO', email: String(v.email), password: String(v.password), nickname: v.nickname || ('gamer'+Math.floor(Math.random()*1000)) };
    this.auth.register(payload).subscribe({ next: r=>{
        this.message = 'Gamer registrado';
        // attempt to login automatically after registration to obtain full user object
        this.auth.login(payload.email, payload.password).subscribe({ next: ()=>{ this.registering = false; this.router.navigate(['/gamer']); }, error: ()=>{ this.registering = false; this.message = 'Registrado pero no se pudo iniciar sesión automáticamente'; } });
      }, error: e=>{ this.registering = false; this.message = e && e.error && e.error.message ? e.error.message : 'Error al registrar gamer' } });
  }

  registerEmpresa(){
    if (this.registering) return;
    const v = this.regForm.value;
    if (!v.email || !v.password) { this.message='Completa email y contraseña para empresa'; return }
    this.registering = true;
    this.message = 'Registrando empresa...';
    const payload = { role: 'EMPRESA', email: String(v.email), password: String(v.password), name: v.name || ('Empresa '+Math.floor(Math.random()*100)) };
    this.auth.register(payload).subscribe({ next: r=>{
        this.message = 'Empresa registrada';
        // attempt to login automatically to fetch company association
        this.auth.login(payload.email, payload.password).subscribe({ next: ()=>{
            // optionally create a company record if backend didn't create one
            try{
              // lazy load EmpresaService to avoid circular DI in some setups
              const svc = (this as any).empresaSvc as EmpresaService;
              if(svc){ svc.createCompany({ nombre: payload.name, correo: payload.email }).subscribe({ next: ()=>{}, error: ()=>{} }); }
            }catch(_){ }
            this.registering = false; this.router.navigate(['/empresa']);
          }, error: ()=>{ this.registering = false; this.message = 'Registrado pero no se pudo iniciar sesión automáticamente'; } });
      }, error: e=>{ this.registering = false; this.message = e && e.error && e.error.message ? e.error.message : 'Error al registrar empresa' } });
  }

  // Admin helper: fetch users
  users: any[] = [];
  loadUsers(){
    this.message = 'Cargando usuarios...';
    this.auth.listUsers().subscribe({ next: u=>{ this.users = u || []; this.message = 'Usuarios cargados: '+this.users.length }, error: e=>{ this.message = 'Error cargando usuarios' } });
  }
  logout(){ this.auth.logout(); this.message='Sesión cerrada'; this.router.navigate(['/']); }
}
