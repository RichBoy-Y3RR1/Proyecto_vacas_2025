import { Component } from '@angular/core';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent {
  user = this.auth.getCurrentUser();
  constructor(private auth: AuthService) {
    this.auth.currentUser$.subscribe(u => this.user = u);
  }
  logout(){ this.auth.logout(); }
}
