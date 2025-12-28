import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  success(message: string): void { try { window.alert(message); } catch(e){ console.log('success:', message); } }
  error(message: string): void { try { window.alert('Error: ' + message); } catch(e){ console.error(message); } }
  async confirm(message: string): Promise<boolean> { try { return Promise.resolve(window.confirm(message)); } catch(e){ return Promise.resolve(false); } }
}
