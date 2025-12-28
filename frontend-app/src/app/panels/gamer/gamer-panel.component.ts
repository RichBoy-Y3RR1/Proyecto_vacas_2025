import { Component } from '@angular/core';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-gamer-panel',
  templateUrl: './gamer-panel.component.html',
  styleUrls: ['./gamer-panel.component.css']
})
export class GamerPanelComponent {
  user = this.auth.getCurrentUser();
  view: 'profile'|'library' = 'profile';
  constructor(private auth: AuthService) {}
}
