import { Component, OnInit } from '@angular/core';
import { EmpresaService } from '../../services/empresa.service';
import { VideojuegoService } from '../../videojuegos/videojuego.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-empresa-panel',
  templateUrl: './empresa-panel.component.html',
  styleUrls: ['./empresa-panel.component.css']
})
export class EmpresaPanelComponent implements OnInit {
  companies: any[] = [];
  selectedCompany: any = null;
  users: any[] = [];
  loading = false;
  error: string | null = null;
  formMessage: { type: 'success'|'error'|'info', text: string } | null = null;
  // catalog management
  catalog: any[] = [];
  editingGame: any = null;
  newGameName = '';
  newGamePrice: number = 0;
  newGameDesc = '';
  newGameImage = '';
  newGameCategory = '';

  // track demos already created per company to avoid repeats
  demosCreatedFor = new Set<number>();

  // UI/process flags
  creatingDemos = false;
  creatingGame = false;
  savingGame = false;

  // dashboard state
  selectedModule: string = 'catalog';

  // dashboard metrics
  metrics: { totalGames: number; published: number; avgPrice: number; estimatedRevenue: number } = { totalGames: 0, published: 0, avgPrice: 0, estimatedRevenue: 0 };
  // report state — explicit properties so template AOT type checking accepts dot-access
  reportLoading: { ventas: boolean; feedback: boolean; top5: boolean } = { ventas: false, feedback: false, top5: false };
  reportStatus: { ventas: string; feedback: string; top5: string } = { ventas: '', feedback: '', top5: '' };

  newCompanyName = '';
  newUserEmail = '';
  // small UI state for validation
  gameFormErrors: string[] = [];

  private setMessage(type: 'success'|'error'|'info', text: string, timeout = 4000){
    this.formMessage = { type, text };
    if (timeout>0) setTimeout(()=>{ if (this.formMessage && this.formMessage.text===text) this.formMessage = null; }, timeout);
  }

  constructor(private svc: EmpresaService, private juegoSvc: VideojuegoService, private auth: AuthService) {}

  ngOnInit(): void {
    // If current user is an EMPRESA, auto-load their company dashboard
    this.auth.currentUser$.subscribe(u => {
      if (u && (u as any).role === 'EMPRESA' && ((u as any).empresa_id || (u as any).empresaId || (u as any).empresa)) {
        const eid = (u as any).empresa_id || (u as any).empresaId || (u as any).empresa;
        try { const num = Number(eid); if (!isNaN(num)) { this.selectCompany({ id: num, nombre: (u as any).empresaNombre || 'Mi Empresa' }); this.loadCatalog(num); return; } }
        catch(_){}
      }
      // otherwise load company list for admins
      this.loadCompanies();
    });
  }

  // ---------------------- Billing / Reports ----------------------
  downloadBlobAsFile(blob: Blob, filename: string){
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = filename; document.body.appendChild(a); a.click(); a.remove(); window.URL.revokeObjectURL(url);
  }

  async generateSalesReport(from?: string, to?: string){
    if(!this.selectedCompany) { this.setMessage('error','Selecciona una empresa'); return; }
    const key = 'ventas'; this.reportLoading[key] = true; this.reportStatus[key] = 'generando';
    this.svc.downloadSalesReport(this.selectedCompany.id, from, to).subscribe((res:any)=>{
      this.reportLoading[key] = false;
      if(res && res.size){ // assume Blob PDF
        const ts = new Date().toISOString().replace(/[:.]/g,'-');
        this.downloadBlobAsFile(res, `ventas_${this.selectedCompany.id}_${ts}.pdf`);
        this.reportStatus[key] = 'ok-backend'; this.setMessage('success','Reporte de ventas descargado (PDF)');
      } else {
        // fallback: export local summary as JSON
        try{
          const payload = { generatedAt: new Date().toISOString(), metrics: this.metrics, catalog: this.catalog };
          const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
          this.downloadBlobAsFile(blob, `ventas_fallback_${this.selectedCompany.id}.json`);
          this.reportStatus[key] = 'fallback-json'; this.setMessage('info','Backend inaccesible — exportado resumen local (JSON)');
        }catch(e){ this.reportStatus[key] = 'error'; this.setMessage('error','No se pudo generar reporte'); }
      }
    }, err => { this.reportLoading[key]=false; this.reportStatus[key]='error'; this.setMessage('error','Error generando reporte'); });
  }

  async generateFeedbackReport(){
    if(!this.selectedCompany) { this.setMessage('error','Selecciona una empresa'); return; }
    const key='feedback'; this.reportLoading[key]=true; this.reportStatus[key]='generando';
    this.svc.downloadFeedbackReport(this.selectedCompany.id).subscribe((res:any)=>{
      this.reportLoading[key]=false;
      if(res && res.size){ const ts = new Date().toISOString().replace(/[:.]/g,'-'); this.downloadBlobAsFile(res, `feedback_${this.selectedCompany.id}_${ts}.pdf`); this.reportStatus[key]='ok-backend'; this.setMessage('success','Reporte de feedback descargado (PDF)'); }
      else { this.reportLoading[key]=false; this.reportStatus[key]='fallback-json'; const summary = this.catalog.map(g=>({ id:g.id, nombre:g.nombre, avgRating: g.avgRating||null })); const blob = new Blob([JSON.stringify({generatedAt:new Date().toISOString(), summary},null,2)],{type:'application/json'}); this.downloadBlobAsFile(blob, `feedback_fallback_${this.selectedCompany.id}.json`); this.setMessage('info','Backend inaccesible — exportado feedback local (JSON)'); }
    }, () => { this.reportLoading[key]=false; this.reportStatus[key]='error'; this.setMessage('error','Error generando reporte de feedback'); });
  }

  async generateTop5Report(from?: string, to?: string){
    if(!this.selectedCompany) { this.setMessage('error','Selecciona una empresa'); return; }
    const key='top5'; this.reportLoading[key]=true; this.reportStatus[key]='generando';
    this.svc.downloadTopGamesReport(this.selectedCompany.id, from, to).subscribe((res:any)=>{
      this.reportLoading[key]=false;
      if(res && res.size){ const ts = new Date().toISOString().replace(/[:.]/g,'-'); this.downloadBlobAsFile(res, `top5_${this.selectedCompany.id}_${ts}.pdf`); this.reportStatus[key]='ok-backend'; this.setMessage('success','Top 5 descargado (PDF)'); }
      else { this.reportStatus[key]='fallback-json'; const top = (this.catalog||[]).slice(0,5).map(g=>({ id:g.id, nombre:g.nombre, precio:g.precio })); const blob = new Blob([JSON.stringify({generatedAt:new Date().toISOString(), top},null,2)],{type:'application/json'}); this.downloadBlobAsFile(blob, `top5_fallback_${this.selectedCompany.id}.json`); this.setMessage('info','Backend inaccesible — exportado Top5 local (JSON)'); }
    }, () => { this.reportLoading[key]=false; this.reportStatus[key]='error'; this.setMessage('error','Error generando Top5'); });
  }

  loadCompanies(){
    this.loading = true; this.error = null;
    this.svc.listCompanies().subscribe({
      next: res => { this.companies = res || []; this.loading = false; },
      error: err => { this.error = 'No se pudo listar empresas'; this.loading = false; }
    });
  }

  selectCompany(c:any){
    this.selectedCompany = c; 
    this.selectedModule = 'catalog';
    this.loadUsers(c.id); 
    this.loadCatalog(c.id);
  }

  switchModule(m:string){
    this.selectedModule = m;
    if (!this.selectedCompany) return;
    if (m === 'catalog') {
      this.loadCatalog(this.selectedCompany.id);
    }
    if (m === 'users') this.loadUsers(this.selectedCompany.id);
    // future: load analytics/comments/billing as needed
  }

  loadUsers(companyId:number){
    this.users = [];
    this.svc.listCompanyUsers(companyId).subscribe({
      next: r => this.users = r || [],
      error: _ => {
        // backend failed; fallback to empty or local cache
        const key = `local_users_${companyId}`;
        try { const raw = localStorage.getItem(key); if(raw){ this.users = JSON.parse(raw); return; } } catch(e){}
        this.users = [];
      }
    });
  }

  loadCatalog(companyId:number){
    this.catalog = [];
    this.svc.getCompany(companyId).subscribe({
      next: r => {
        this.catalog = r.catalog || [];
        this.selectedCompany = r;
        // merge any local-only items so they remain visible even if backend returned a catalog
        try {
          const localKey = `local_catalog_${companyId}`;
          const localRaw = localStorage.getItem(localKey);
          if (localRaw) {
            const localArr = JSON.parse(localRaw) as any[];
            const ids = new Set(this.catalog.map((x:any)=>x.id));
            for (const li of localArr) { if (!ids.has(li.id)) this.catalog.push(li); }
          }
        } catch(e) { /* ignore parse errors */ }
        // persist a merged cache so frontend can recover when backend is down
        try { localStorage.setItem(`local_catalog_${companyId}`, JSON.stringify(this.catalog)); } catch(e){}
        // restore demos-created marker if present
        try { if (localStorage.getItem(`demos_created_${companyId}`)) this.demosCreatedFor.add(companyId); } catch(e){}
        // order and group catalog for display
        this.sortCatalog();
        // update metrics after catalog is ready
        this.updateMetrics();
      },
      error: err => {
        // backend not available or returned error — load local cache or built-in demos
        try {
          const raw = localStorage.getItem(`local_catalog_${companyId}`);
          if (raw) { this.catalog = JSON.parse(raw); 
            // restore demos-created marker
            if (localStorage.getItem(`demos_created_${companyId}`)) this.demosCreatedFor.add(companyId);
            return; }
        } catch(e) { /* ignore */ }
        // built-in fallback demos
        this.catalog = [
          { id: -1, nombre: 'Mario Strikers', descripcion: 'Demo local: Mario Strikers', precio: 19.99, estado: 'PUBLICADO', url_imagen: 'https://via.placeholder.com/320x180?text=Mario+Strikers', categoria: 'Deportes' },
          { id: -2, nombre: 'Demo Racer X', descripcion: 'Demo local: Demo Racer X', precio: 9.99, estado: 'PUBLICADO', url_imagen: 'https://via.placeholder.com/320x180?text=Demo+Racer', categoria: 'Carreras' },
          { id: -3, nombre: 'Pixel Adventure', descripcion: 'Demo local: Pixel Adventure', precio: 4.99, estado: 'PUBLICADO', url_imagen: 'https://via.placeholder.com/320x180?text=Pixel+Adventure', categoria: 'Plataformas' }
        ];
        this.selectedCompany = { id: companyId, nombre: this.selectedCompany?.nombre || 'Mi Empresa' };
        this.sortCatalog();
        this.updateMetrics();
      }
    });
  }

  private updateMetrics(){
    try{
      const list = this.catalog || [];
      const total = list.length;
      const published = list.filter((x:any)=> (x.estado || (x.for_sale? 'PUBLICADO':'')).toString().toUpperCase()==='PUBLICADO' || x.for_sale).length;
      const avgPrice = total ? (list.reduce((s:any,i:any)=> s + (Number(i.precio)||0),0) / total) : 0;
      const estRevenue = list.reduce((s:any,i:any)=> s + (Number(i.precio)||0),0);
      this.metrics = { totalGames: total, published, avgPrice: Number(avgPrice.toFixed(2)), estimatedRevenue: Number(estRevenue.toFixed(2)) };
    }catch(e){ /* ignore */ }
  }

  // helper: get email of logged user (if any)
  getLoggedUserEmail(): string | null {
    try{
      const u: any = (this.auth && (this.auth.getCurrentUser ? this.auth.getCurrentUser() : null));
      if(!u) return null;
      // accept multiple possible property names coming from different backends
      return u.email || u.correo || u.mail || u.username || u.userEmail || null;
    }catch(e){ return null; }
  }

  // Toggle company visibility to gamers
  toggleCompanyVisibility(){
    if(!this.selectedCompany) return;
    const cid = this.selectedCompany.id;
    // determine property name to use
    const current = this.selectedCompany.visible_to_gamers ?? this.selectedCompany.visible ?? this.selectedCompany.public_visible ?? false;
    const newVal = !current;
    // prepare payload (non-destructive)
    const payload: any = { visible_to_gamers: newVal };
    // attempt backend update, fallback to local storage update inside service
    this.svc.updateCompany(cid, payload).subscribe({
      next: (res:any) => {
        // update local UI copy
        try{ this.selectedCompany = { ...this.selectedCompany, ...res, visible_to_gamers: newVal }; this.setMessage('success', newVal ? 'Empresa visible para gamers' : 'Empresa ocultada a gamers'); this.updateMetrics(); }catch(e){ this.setMessage('success','Visibilidad actualizada'); }
      },
      error: () => {
        // update local cache directly if service fallback didn't work
        try{
          const key = 'local_companies'; const arr = JSON.parse(localStorage.getItem(key) || '[]') as any[]; const idx = arr.findIndex(x=>x.id===cid);
          if(idx>=0){ arr[idx].visible_to_gamers = newVal; localStorage.setItem(key, JSON.stringify(arr)); this.selectedCompany.visible_to_gamers = newVal; this.setMessage('success','Visibilidad guardada localmente'); return; }
        }catch(e){}
        this.setMessage('error','No se pudo cambiar visibilidad');
      }
    });
  }

  // helper: read creation date from various possible fields
  getCompanyCreationDate(): string | null {
    if(!this.selectedCompany) return null;
    const candidates = ['createdAt','created_at','fecha_registro','fechaRegistro','creado_en','created'];
    for(const k of candidates){ if(this.selectedCompany[k]){ try{ const d = new Date(this.selectedCompany[k]); if(!isNaN(d.getTime())) return d.toLocaleDateString(); }catch(e){ return String(this.selectedCompany[k]); } } }
    return null;
  }

  createCompany(){
    const name = (this.newCompanyName || '').trim();
    if(!name){ this.setMessage('error','Nombre requerido'); return; }
    this.svc.createCompany({ nombre: name }).subscribe({
      next: _ => { this.newCompanyName=''; this.setMessage('success','Empresa creada'); this.loadCompanies(); },
      error: _ => { this.setMessage('error','No se pudo crear la empresa'); }
    });
  }

  addUser(){
    if(!this.selectedCompany) return;
    const email = (this.newUserEmail||'').trim().toLowerCase();
    if(!email){ this.setMessage('error','Email requerido'); return; }
    // basic email format
    const at = email.indexOf('@'); if(at <= 0){ this.setMessage('error','Email inválido'); return; }
    const emailDomain = email.split('@')[1];
    // try to infer company domain from company email or name
    let companyDomain = '';
    try{
      const compEmail = (this.selectedCompany && (this.selectedCompany.correo || this.selectedCompany.email || '')) as string;
      if(compEmail && compEmail.indexOf('@')>0) companyDomain = compEmail.split('@')[1].toLowerCase();
    }catch(e){}
    // if we have a company domain, enforce same domain
    if(companyDomain){
      if(emailDomain !== companyDomain){ this.setMessage('error', `El usuario debe pertenecer al dominio de la empresa: @${companyDomain}`); return; }
    } else {
      // no company domain known — give a gentle hint but allow
      this.setMessage('info','No se pudo verificar dominio de empresa; se permitirá el correo, pero verifica que sea interno.');
    }

    this.svc.addCompanyUser(this.selectedCompany.id, { email }).subscribe({
      next: r => { this.newUserEmail=''; this.setMessage('success','Usuario añadido'); this.loadUsers(this.selectedCompany.id); },
      error: e => { const msg = e && e.error && e.error.message ? e.error.message : 'No se pudo añadir usuario'; this.setMessage('error', msg); }
    });
  }

  removeUser(u:any){
    if(!this.selectedCompany) return;
    this.svc.deleteCompanyUser(this.selectedCompany.id, u.id).subscribe({
      next: _ => this.loadUsers(this.selectedCompany.id),
      error: _ => this.error = 'No se pudo eliminar usuario'
    });
  }

  // Catalog actions
  startCreateGame(){ this.editingGame = null; this.newGameName=''; this.newGamePrice = 0; this.newGameDesc=''; }
  createGame(){
    this.gameFormErrors = [];
    if(!this.selectedCompany){ this.setMessage('error','Selecciona una empresa'); return; }
    const nombre = (this.newGameName||'').trim(); if(!nombre) this.gameFormErrors.push('Nombre es obligatorio');
    if (this.newGamePrice == null || isNaN(this.newGamePrice) ) this.gameFormErrors.push('Precio inválido');
    if (this.gameFormErrors.length>0){ this.setMessage('error','Corrige los campos del formulario'); return; }
    if (this.creatingGame) return;
    this.creatingGame = true;
    const payload: any = { nombre, descripcion: this.newGameDesc || '', precio: Number(this.newGamePrice) || 0, empresa_id: this.selectedCompany.id, url_imagen: this.newGameImage || null, categoria: this.newGameCategory || null };
    this.juegoSvc.create(payload).subscribe({
      next: (res:any) => {
        // push returned item when available, otherwise push payload with server id placeholder
        const item = (res && res.id) ? res : { id: (res && res.id) || Date.now(), nombre: payload.nombre, descripcion: payload.descripcion, precio: payload.precio, estado: 'PUBLICADO', url_imagen: payload.url_imagen || 'https://via.placeholder.com/140x80?text=No+Image', categoria: payload.categoria };
        this.catalog.unshift(item);
        // persist to local cache so it's safe across reloads
        try{
          const localKey = `local_catalog_${this.selectedCompany.id}`;
          const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[];
          // remove any existing with same id then push
          const filtered = current.filter(x=>x.id!==item.id);
          filtered.push(item);
          localStorage.setItem(localKey, JSON.stringify(filtered));
        }catch(e){/* ignore */}
        this.newGameName=''; this.newGameDesc=''; this.newGamePrice=0; this.newGameImage=''; this.newGameCategory='';
        this.sortCatalog();
        this.setMessage('success','Videojuego creado');
        this.creatingGame = false;
      },
      error: err => {
        // Backend failed — persist locally and update UI so user can continue
        try {
          const localKey = `local_catalog_${this.selectedCompany.id}`;
          const current = JSON.parse(localStorage.getItem(localKey) || '[]');
          const localItem = { id: -(Date.now()), nombre: payload.nombre, descripcion: payload.descripcion, precio: payload.precio, estado: 'PUBLICADO', url_imagen: payload.url_imagen || 'https://via.placeholder.com/140x80?text=No+Image', categoria: payload.categoria };
          current.push(localItem);
          localStorage.setItem(localKey, JSON.stringify(current));
          this.catalog.unshift(localItem);
          this.sortCatalog();
          // persist local-only item
          try{
            const localKey = `local_catalog_${this.selectedCompany.id}`;
            const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[];
            current.push(localItem);
            localStorage.setItem(localKey, JSON.stringify(current));
          }catch(e){}
          this.newGameName=''; this.newGameDesc=''; this.newGamePrice=0; this.newGameImage=''; this.newGameCategory='';
          this.setMessage('success','Videojuego guardado localmente (backend inaccesible)');
        } catch(e){ this.setMessage('error','No se pudo crear videojuego'); }
        this.creatingGame = false;
      }
    });
    // safety: clear flag after short timeout in case subscribe doesn't
    setTimeout(()=> this.creatingGame = false, 3000);
  }

  editGame(g:any){ this.editingGame = {...g}; }
  saveGame(){
    if(!this.editingGame) return;
    if (this.savingGame) return;
    this.savingGame = true;
    const id = this.editingGame.id;
    // if local item (negative id) update localStorage and UI immediately
    if (id < 0) {
      try{
        const localKey = `local_catalog_${this.selectedCompany.id}`;
        const current = JSON.parse(localStorage.getItem(localKey) || '[]');
        const idx = current.findIndex((x:any)=>x.id===id);
        if (idx>=0){ current[idx] = { ...current[idx], ...this.editingGame }; localStorage.setItem(localKey, JSON.stringify(current)); }
        const ci = this.catalog.findIndex((x:any)=>x.id===id); if(ci>=0) this.catalog[ci] = { ...this.catalog[ci], ...this.editingGame };
        this.sortCatalog();
        this.editingGame = null; this.setMessage('success','Cambios guardados localmente');
      }catch(e){ this.setMessage('error','No se pudo guardar cambios localmente'); }
      this.savingGame = false; return;
    }

    this.juegoSvc.update(id, this.editingGame).subscribe({
      next: (res:any) => {
        // update UI item with server response if provided
        const uiIdx = this.catalog.findIndex((x:any)=>x.id===id);
        if (uiIdx >= 0) this.catalog[uiIdx] = res || { ...this.catalog[uiIdx], ...this.editingGame };
        this.sortCatalog();
        this.editingGame = null;
        this.setMessage('success','Videojuego actualizado');
        this.savingGame = false;
      },
      error: () => { this.error = 'No se pudo guardar videojuego'; this.savingGame = false; }
    });
  }

  toggleSale(g:any){
    const id = g.id; const newVal = !g.for_sale;
    if (id < 0) {
      try{
        const localKey = `local_catalog_${this.selectedCompany.id}`;
        const current = JSON.parse(localStorage.getItem(localKey) || '[]');
        const idx = current.findIndex((x:any)=>x.id===id);
        if (idx>=0){ current[idx].for_sale = newVal; localStorage.setItem(localKey, JSON.stringify(current)); }
        const ci = this.catalog.findIndex((x:any)=>x.id===id); if(ci>=0) this.catalog[ci].for_sale = newVal;
        return;
      }catch(e){ this.error = 'No se pudo cambiar estado localmente'; return; }
    }
    this.juegoSvc.toggleForSale(id, newVal).subscribe({
      next: (res:any) => {
        const uiIdx = this.catalog.findIndex((x:any)=>x.id===id);
        if (uiIdx>=0) this.catalog[uiIdx].for_sale = newVal;
        this.sortCatalog();
        // update local cache
        try{ const localKey = `local_catalog_${this.selectedCompany.id}`; const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[]; const idx = current.findIndex(x=>x.id===id); if(idx>=0){ current[idx].for_sale = newVal; localStorage.setItem(localKey, JSON.stringify(current)); } }catch(e){}
      },
      error: () => this.error = 'No se pudo cambiar estado venta'
    });
  }

  removeGame(g:any){
    if(!confirm('Eliminar videojuego?')) return;
    const id = g.id;
    if (id < 0) {
      try{
        const localKey = `local_catalog_${this.selectedCompany.id}`;
        const current = JSON.parse(localStorage.getItem(localKey) || '[]').filter((x:any)=>x.id!==id);
        localStorage.setItem(localKey, JSON.stringify(current));
        this.catalog = this.catalog.filter((x:any)=>x.id!==id);
        this.setMessage('success','Videojuego eliminado (local)');
        return;
      }catch(e){ this.error = 'No se pudo eliminar localmente'; return; }
    }
    this.juegoSvc.delete(id).subscribe({
      next: () => {
        this.catalog = this.catalog.filter((x:any)=>x.id!==id);
        this.sortCatalog();
        try{ const localKey = `local_catalog_${this.selectedCompany.id}`; const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[]; const filtered = current.filter(x=>x.id!==id); localStorage.setItem(localKey, JSON.stringify(filtered)); }catch(e){}
      },
      error: () => this.error = 'No se pudo eliminar videojuego'
    });
  }

  // Send a game to admin for approval (mark estado = 'PENDIENTE')
  sendToAdmin(g:any){
    if(!this.selectedCompany) { this.setMessage('error','Selecciona una empresa'); return; }
    const id = g.id;
    if(id && id > 0){
      this.juegoSvc.update(id, { estado: 'PENDIENTE' }).subscribe({
        next: (res:any)=>{ this.setMessage('success','Juego enviado a admin para aprobación'); this.loadCatalog(this.selectedCompany.id); },
        error: ()=>{ this.setMessage('error','No se pudo enviar al backend; intenta de nuevo'); }
      });
      return;
    }
    // local item: update local cache and UI
    try{
      const key = `local_catalog_${this.selectedCompany.id}`;
      const current = JSON.parse(localStorage.getItem(key) || '[]') as any[];
      const idx = current.findIndex(x=>x.id===id);
      if(idx>=0){ current[idx].estado = 'PENDIENTE'; localStorage.setItem(key, JSON.stringify(current)); }
      // update in-memory catalog
      const ci = this.catalog.findIndex(x=>x.id===id); if(ci>=0) this.catalog[ci].estado = 'PENDIENTE';
      this.sortCatalog();
      this.setMessage('success','Juego marcado como PENDIENTE localmente');
    }catch(e){ this.setMessage('error','No se pudo marcar juego como pendiente'); }
  }

  // Create demo/example games for this company
  createDemoGames(){
    if(!this.selectedCompany) { this.setMessage('error','Selecciona una empresa antes de crear demos'); return; }
    const cid = this.selectedCompany.id;
    if (this.creatingDemos) return;
    // don't recreate demos if already created for this company
    try { if (localStorage.getItem(`demos_created_${cid}`)) { this.setMessage('info','Los demos ya fueron creados para esta empresa'); return; } } catch(e){}
    this.creatingDemos = true;
    const demos = [
      { nombre: 'Mario Strikers', descripcion: 'Mario Strikers es un juego de fútbol arcade con power-ups y juego multijugador rápido. Ideal para partidas casuales y torneos entre amigos.', precio: 19.99, empresa_id: cid, url_imagen: 'https://via.placeholder.com/320x180?text=Mario+Strikers', categoria: 'Deportes' },
      { nombre: 'Demo Racer X', descripcion: 'Carreras arcade con pistas dinámicas, saltos y potenciadores. Controles sencillos y modo contrarreloj.', precio: 9.99, empresa_id: cid, url_imagen: 'https://via.placeholder.com/320x180?text=Demo+Racer', categoria: 'Carreras' },
      { nombre: 'Pixel Adventure', descripcion: 'Plataformas retro con niveles creados por la comunidad y música chiptune.', precio: 4.99, empresa_id: cid, url_imagen: 'https://via.placeholder.com/320x180?text=Pixel+Adventure', categoria: 'Plataformas' }
    ];

    this.setMessage('info','Creando juegos demo...');
    let created = 0; let failed = 0; let processed = 0;
    for(const g of demos){
      // ensure we don't add duplicates by name
      const exists = this.catalog.find(x => (x.nombre||'').toLowerCase() === (g.nombre||'').toLowerCase());
      if (exists) { processed++; if(processed===demos.length){ localStorage.setItem(`demos_created_${cid}`,'1'); this.demosCreatedFor.add(cid); this.creatingDemos = false; this.setMessage('success',`Juegos demo creados (${created} creados, ${failed} guardados localmente)`); } continue; }
      this.juegoSvc.create(g).subscribe({
        next: (res:any)=>{
          const item = res && res.id ? res : { id: Date.now(), nombre: g.nombre, descripcion: g.descripcion, precio: g.precio, estado: 'PUBLICADO', url_imagen: g.url_imagen, categoria: g.categoria };
          // add to UI if not present
          if (!this.catalog.find(x=>x.id===item.id)) this.catalog.push(item);
          this.sortCatalog();
          // persist to local cache
          try{ const localKey = `local_catalog_${cid}`; const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[]; const filtered = current.filter(x=>x.id!==item.id); filtered.push(item); localStorage.setItem(localKey, JSON.stringify(filtered)); }catch(e){}
          created++; processed++;
          if(processed===demos.length){ try{ localStorage.setItem(`demos_created_${cid}`,'1'); }catch(e){} this.demosCreatedFor.add(cid); this.creatingDemos = false; this.setMessage('success',`Juegos demo creados (${created} creados, ${failed} guardados localmente)`); }
        },
        error: ()=>{
          try{
            const localKey = `local_catalog_${cid}`;
            const current = JSON.parse(localStorage.getItem(localKey) || '[]') as any[];
            // create a stable negative id but avoid collisions
            let nid = -(Date.now() + Math.floor(Math.random()*1000) + failed);
            const localItem = { id: nid, nombre: g.nombre, descripcion: g.descripcion, precio: g.precio, estado: 'PUBLICADO', url_imagen: g.url_imagen, categoria: g.categoria };
            // avoid duplicates by name
            if(!current.find(x=> (x.nombre||'').toLowerCase() === (g.nombre||'').toLowerCase())){
              current.push(localItem);
              localStorage.setItem(localKey, JSON.stringify(current));
              this.catalog.push(localItem);
              this.sortCatalog();
            }
            failed++; processed++;
          }catch(e){ failed++; processed++; }
          if(processed===demos.length){ try{ localStorage.setItem(`demos_created_${cid}`,'1'); }catch(e){} this.demosCreatedFor.add(cid); this.creatingDemos = false; this.setMessage('success',`Juegos demo creados (${created} creados, ${failed} guardados localmente)`); }
        }
      });
    }
  }

  // Comment visibility (client-side control until backend supports)
  commentVisibilityGlobal = true;
  toggleCommentVisibilityGlobal(){ this.commentVisibilityGlobal = !this.commentVisibilityGlobal; }
  
  // Sort and group catalog by category for nicer display
  groupedCatalog: Array<{ categoria: string, items: any[] }> = [];
  private sortCatalog(){
    try{
      // normalize missing categories
      this.catalog = (this.catalog || []).map(i => ({ categoria: i.categoria || 'Sin categoría', ...i }));
      this.catalog.sort((a:any,b:any)=>{
        const ca = (a.categoria||'').toLowerCase(); const cb = (b.categoria||'').toLowerCase();
        if(ca < cb) return -1; if(ca>cb) return 1;
        const na = (a.nombre||'').toLowerCase(); const nb = (b.nombre||'').toLowerCase(); if(na<nb) return -1; if(na>nb) return 1; return 0;
      });
      const map = new Map<string, any[]>();
      for(const it of this.catalog){ const key = it.categoria || 'Sin categoría'; if(!map.has(key)) map.set(key, []); map.get(key)!.push(it); }
      this.groupedCatalog = [];
      for(const [k,v] of map.entries()){ this.groupedCatalog.push({ categoria: k, items: v }); }
    }catch(e){ /* ignore */ }
  }
}
