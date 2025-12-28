import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { of } from 'rxjs';
import { switchMap, timeout, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { EmpresaService } from '../services/empresa.service';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  styleUrls: ['./auth.component.css'],
})
export class AuthComponent {
  message = '';
  user: any = null;
  loading = false;
  showRegister = false;
  registering = false;
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  regForm = this.fb.group({
    role: ['USUARIO'],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    nickname: ['', [Validators.required]],
    name: [''],
    birthDate: [''],
    phone: [''],
    country: ['']
  });
  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private empresaSvc: EmpresaService,
    private http: HttpClient
  ) {
    this.auth.currentUser$.subscribe((u) => (this.user = u));
    // expose as dynamic property for backward compatibility with code paths that try lazy access
    (this as any).empresaSvc = this.empresaSvc;
  }
  submitLogin() {
    if (this.loginForm.invalid) {
      this.message = 'Completa email y contraseña válidos';
      return;
    }
    this.message = 'Iniciando sesión...';
    this.loading = true;
    const v = this.loginForm.value;
      const email = String(this.loginForm.value.email || '');
      const password = String(this.loginForm.value.password || '');
      // First try local-only login to avoid hitting backend (and avoid visible 401)
      this.auth.loginLocalFallback(email, password).pipe(
        switchMap((localRes:any) => {
          if (localRes) return of(localRes);
          // Not found locally -> call regular backend login
          return this.auth.login(email, password);
        })
      ).subscribe({
        next: (r:any) => {
          this.loading = false;
          const user = r && r.user ? r.user : this.auth.getCurrentUser();
          if(!user){ this.message = 'Error iniciando sesión (usuario no encontrado)'; return; }
          this.message = 'Bienvenido '+(user.nickname||user.email);
          const role = (user && user.role) ? String(user.role).toUpperCase() : 'USUARIO';
          if (role === 'ADMIN') this.router.navigate(['/admin']);
          else if (role === 'EMPRESA' || role === 'COMPANY') this.router.navigate(['/empresa']);
          else this.router.navigate(['/gamer']);
        },
        error: (err:any) => { this.loading = false; this.message = err && err.message ? err.message : 'Credenciales inválidas'; }
      });
  }
  submitRegister() {
    // kept for backward compatibility; delegate to specific helpers
    const role = this.regForm.value.role || 'USUARIO';
    if (role === 'USUARIO') return this.registerGamer();
    return this.registerEmpresa();
  }

  registerGamer() {
    if (this.registering) return;
    const v = this.regForm.value;
    if (!v.email || !v.password || !v.nickname) {
      this.message = 'Completa email, contraseña y nickname';
      return;
    }
    // validate age >= 13 if birthDate provided
    if (v.birthDate) {
      const dob = new Date(v.birthDate);
      const age = this.getAge(dob);
      if (age < 13) {
        this.message = 'Debes tener al menos 13 años para crear una cuenta';
        return;
      }
    }

    this.registering = true;
    this.message = 'Registrando gamer...';
    const payload = {
      role: 'USUARIO',
      email: String(v.email).toLowerCase(),
      password: String(v.password),
      nickname: String(v.nickname),
      fecha_nacimiento: v.birthDate || null,
      telefono: v.phone || null,
      pais: v.country || null
    };

    // check duplicates by fetching existing users with a short timeout
    this.auth.listUsers().pipe(
      timeout(3000),
      catchError(() => of(null))
    ).subscribe({
      next: (users) => {
        if (users && Array.isArray(users)){
          const existingEmail = (users || []).some((u:any) => u && ((u.email || u.correo || '').toString().toLowerCase() === payload.email));
          const existingNick = (users || []).some((u:any) => u && ((u.nickname || '').toString().toLowerCase() === (payload.nickname || '').toString().toLowerCase()));
          if (existingEmail) { this.message = 'Ya existe una cuenta con ese correo'; this.registering = false; return; }
          if (existingNick) { this.message = 'El nickname ya está en uso'; this.registering = false; return; }
        }

        // proceed to create user even if duplicate check timed out
        this.auth.register(payload).pipe(timeout(5000), catchError((err:any)=> { this.registering=false; this.message = err && err.message ? err.message : 'Error al registrar'; return of(null); })).subscribe({
          next: (r:any) => {
            if (!r) { this.registering = false; this.message = 'Error al registrar'; return; }
            // if backend returned _local marker, it's an offline/local registration
            if (r._local) {
              this.message = 'Registrado localmente (offline)';
              this.registering = false;
              // already saved and considered logged-in by AuthService fallback
              this.router.navigate(['/gamer']);
              return;
            }
            this.message = 'Gamer registrado';
            const newId = r && r.id ? r.id : null;
            // create empty wallet for new user if id available (only when backend actually created user)
            if (newId && newId > 0) {
              this.http.post('http://localhost:8081/backend/api/cartera', { usuario_id: newId, saldo: 0 }).subscribe({ next: ()=>{}, error: ()=>{} });
            }
            // If backend returned token on register, AuthService already saved it (see service tap)
            if (r && r.token) {
              this.registering = false;
              this.router.navigate(['/gamer']);
              return;
            }
            // otherwise attempt to login automatically after registration to obtain full user object
            this.auth.login(payload.email, payload.password).subscribe({
              next: () => {
                this.registering = false;
                this.router.navigate(['/gamer']);
              },
              error: () => {
                this.registering = false;
                this.message = 'Registrado pero no se pudo iniciar sesión automáticamente';
              },
            });
          }
        });
      },
      error: (e) => { this.registering = false; this.message = 'No se pudo verificar duplicados'; }
    });
  }

  private getAge(dob: Date): number {
    const diff = Date.now() - dob.getTime();
    const ageDt = new Date(diff);
    return Math.abs(ageDt.getUTCFullYear() - 1970);
  }

  registerEmpresa() {
    if (this.registering) return;
    const v = this.regForm.value;
    if (!v.email || !v.password) {
      this.message = 'Completa email y contraseña para empresa';
      return;
    }
    this.registering = true;
    this.message = 'Registrando empresa...';
    const payload = {
      role: 'EMPRESA',
      email: String(v.email),
      password: String(v.password),
      name: v.name || 'Empresa ' + Math.floor(Math.random() * 100),
    };
    this.auth.register(payload).subscribe({
      next: (r) => {
        this.message = 'Empresa registrada';
        // attempt to login automatically to fetch company association
        this.auth.login(payload.email, payload.password).subscribe({
          next: () => {
            // optionally create a company record if backend didn't create one
            try {
              // lazy load EmpresaService to avoid circular DI in some setups
              const svc = (this as any).empresaSvc as EmpresaService;
              if (svc) {
                svc
                  .createCompany({
                    nombre: payload.name,
                    correo: payload.email,
                  })
                  .subscribe({ next: () => {}, error: () => {} });
              }
            } catch (_) {}
            this.registering = false;
            this.router.navigate(['/empresa']);
          },
          error: () => {
            this.registering = false;
            this.message =
              'Registrado pero no se pudo iniciar sesión automáticamente';
          },
        });
      },
      error: (e) => {
        this.registering = false;
        this.message =
          e && e.error && e.error.message
            ? e.error.message
            : 'Error al registrar empresa';
      },
    });
  }

  // Admin helper: fetch users
  users: any[] = [];
  loadUsers() {
    this.message = 'Cargando usuarios...';
    this.auth.listUsers().subscribe({
      next: (u) => {
        this.users = u || [];
        this.message = 'Usuarios cargados: ' + this.users.length;
      },
      error: (e) => {
        this.message = 'Error cargando usuarios';
      },
    });
  }
  logout() {
    this.auth.logout();
    this.message = 'Sesión cerrada';
    this.router.navigate(['/']);
  }
}
