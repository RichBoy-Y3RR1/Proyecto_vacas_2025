import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}
  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expected = route.data['role']; // string or string[]
    const user = this.auth.getCurrentUser();
    const userRole = user?.role || null;

    // Accept either a single role or an array of roles
    if (userRole) {
      if (Array.isArray(expected)) {
        if (expected.includes(userRole)) return true;
      } else if (typeof expected === 'string') {
        if (expected === userRole) return true;
      }
      // compatibility: treat 'USUARIO' and 'GAMER' as equivalent
      if ((userRole === 'USUARIO' && expected === 'GAMER') || (userRole === 'GAMER' && expected === 'USUARIO')) return true;
    }

    // fallback: redirect to root
    this.router.navigate(['/']);
    return false;
  }
}
