import { Component, OnInit } from '@angular/core';
import { GamerService } from '../../services/gamer.service';

@Component({
  selector: 'app-gamer-panel',
  templateUrl: './gamer-panel.component.html',
  styleUrls: ['./gamer-panel.component.css']
})
export class GamerPanelComponent implements OnInit {
  user: any = { nickname: 'Gamer' };
  activeTab: 'store'|'library'|'wallet'|'community'|'families'|'reports'|'profile' = 'store';
  storeGames: any[] = [];
  library: any[] = [];
  balance = 0;
  transactions: any[] = [];
  loadingReport = false;
  reportPreview: any = null;
  
  max(arr: number[]){ if(!arr || arr.length===0) return 1; return Math.max(...arr); }

  constructor(private gamerService: GamerService) {}

  ngOnInit(): void {
    this.gamerService.getProfile().subscribe(u => { if(u) this.user = u; });
    this.reloadAll();
  }

  reloadAll(){
    this.gamerService.getStore().subscribe(g => {
        const storeRaw = (g || []);
        this.gamerService.getLibrary().subscribe(lib => {
          const ownedIds = new Set((lib||[]).map((x:any)=>x.id));
          this.storeGames = storeRaw.map((it:any) => ({
            id: it.id,
            title: it.nombre || it.title || it.name,
            image: it.url_imagen || it.image || it.cover || 'https://via.placeholder.com/320x180?text=No+Image',
            price: Number(it.precio ?? it.price ?? it.cost ?? 0),
            avgRating: it.avgRating || it.calificacion_media || it.rating || 0,
            categories: Array.isArray(it.categoria) ? it.categoria : (it.categoria ? [it.categoria] : (it.categories || [])),
            ageRating: it.ageRating || it.edad_minima || it.age || null,
            company: it.empresa || it.empresa_id || it.companyName || null,
            images: it.images || it.galeria || [],
            for_sale: it.for_sale || (String(it.estado||'').toUpperCase()==='PUBLICADO'),
            owned: ownedIds.has(it.id),
            installed: false
          }));
          this.library = (lib || []).map((it:any) => ({
            id: it.id,
            title: it.nombre || it.title || it.name,
            installed: it.installed || it.estado === 'INSTALLED' || it.installed === true,
            source: it.source || it.origen || 'Compra',
            playTime: it.playTime || it.tiempo || '0h'
          }));
        }, ()=>{
          this.storeGames = storeRaw.map((it:any)=>({
            id: it.id,
            title: it.nombre || it.title || it.name,
            image: it.url_imagen || it.image || it.cover || 'https://via.placeholder.com/320x180?text=No+Image',
            price: Number(it.precio ?? it.price ?? it.cost ?? 0),
            avgRating: it.avgRating || it.calificacion_media || it.rating || 0,
            categories: Array.isArray(it.categoria) ? it.categoria : (it.categoria ? [it.categoria] : (it.categories || [])),
            ageRating: it.ageRating || it.edad_minima || it.age || null,
            company: it.empresa || it.empresa_id || it.companyName || null,
            images: it.images || it.galeria || [],
            for_sale: it.for_sale || (String(it.estado||'').toUpperCase()==='PUBLICADO'),
            owned: false,
            installed: false
          }));
        });
      });
      this.gamerService.getWallet().subscribe(w => { this.balance = Number(w?.balance||0); this.transactions = w?.transactions||[]; });
  }

  select(tab: any){ this.activeTab = tab; }

  buy(game: any){
    // legacy method kept for compatibility
    this.purchase(game);
  }

  purchase(game: any){
    // age validation (if birthday known)
    const dob = this.user?.fecha_nacimiento;
    if (game.ageRating && dob){
      try{
        const birth = new Date(dob);
        const age = Math.floor((Date.now() - birth.getTime())/(1000*60*60*24*365.25));
        if (age < Number(game.ageRating)) { alert('No cumple la edad mínima para comprar este juego'); return; }
      } catch(e){}
    }
    if (this.balance < game.price){ alert('Saldo insuficiente'); return; }
    // confirm modal simple
    const ok = confirm(`Comprar ${game.title} por ${new Intl.NumberFormat('en-US',{style:'currency',currency:'USD'}).format(game.price)}?`);
    if(!ok) return;
    this.gamerService.buy(game.id).subscribe((r:any)=>{
      // success when backend returns id or ok flag
      if (r && (r.id || r.ok)){
        alert('Compra exitosa');
        // update wallet and library locally
        this.gamerService.getWallet().subscribe(w => { this.balance = Number(w?.balance||0); });
        this.reloadAll();
        return;
      }
      // otherwise enqueue locally for offline processing
      try{
        const qk = 'local_purchase_queue'; const q = JSON.parse(localStorage.getItem(qk)||'[]'); q.push({ userId: this.user?.id, gameId: game.id, ts: Date.now() }); localStorage.setItem(qk, JSON.stringify(q));
        alert('Compra encolada localmente; se procesará cuando el backend esté disponible');
      }catch(e){ alert('No se pudo procesar la compra'); }
      this.reloadAll();
    }, ()=>{ 
      try{ const qk = 'local_purchase_queue'; const q = JSON.parse(localStorage.getItem(qk)||'[]'); q.push({ userId: this.user?.id, gameId: game.id, ts: Date.now() }); localStorage.setItem(qk, JSON.stringify(q)); alert('Compra encolada localmente (offline)'); }catch(e){ alert('Compra fallida'); }
      this.reloadAll();
    });
  }

  openDetail(game: any){
    // open simple modal using window.prompt/popups for now — non-blocking and safe
    const details = `Título: ${game.title}\nEmpresa: ${game.company||'N/A'}\nPrecio: ${game.price}`;
    if (confirm(details + '\n\nVer galería?')){
      // open gallery in new window/tab (simple fallback)
      const html = `<html><body style="background:#0f1113;color:#ddd;font-family:Arial"><h2>${game.title}</h2>`+
        (game.images||[]).map((u:any)=>`<img src="${u}" style="max-width:320px;margin:6px" />`).join('')+`</body></html>`;
      const w = window.open('','_blank');
      if(w){ w.document.write(html); w.document.close(); }
    }
  }

  install(game: any){ this.gamerService.updateGameState(game.id, 'INSTALLED').subscribe(()=> game.installed = true); }
  uninstall(game: any){ this.gamerService.updateGameState(game.id, 'NOT_INSTALLED').subscribe(()=> game.installed = false); }

  generate(kind: 'expenses'|'library'|'families'){
    this.loadingReport = true; this.reportPreview = null;
    this.gamerService.generateReport(kind).subscribe((blob:any) => {
      this.loadingReport = false;
      if (!blob){
        this.gamerService.getReportPreview(kind).subscribe(p => {
          // if backend indicates no data, show friendly message
          if (p && (p.hasData === false)) { this.reportPreview = p; return; }
          // transform server preview rows into simple series/labels expected by template
          if (p && p.rows) {
            const rows: any[] = p.rows;
            // expenses -> labels = fecha or titulo, series = monto
            const labels = rows.map(r => r.titulo || r.fecha || '');
            const series = rows.map(r => Number(r.monto || r.net || 0));
            this.reportPreview = { title: p.kind || kind, labels, series, raw: p };
            return;
          }
          // fallback: assign raw preview
          this.reportPreview = p;
        });
        return;
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = `${kind}_report.pdf`; a.click(); window.URL.revokeObjectURL(url);
    }, () => { this.loadingReport = false; this.gamerService.getReportPreview(kind).subscribe(p => { this.reportPreview = p; }); });
  }

  recharge(){
    const uid = this.user?.id || this.gamerService.getLocalUser()?.id || null;
    if(!uid){ alert('Usuario no identificado'); return; }
    const amount = Number(prompt('Cantidad a recargar', '10')) || 0;
    if (amount <= 0) { alert('Cantidad inválida'); return; }
    this.gamerService.rechargeWallet(uid, amount).subscribe(()=>{ alert('Recarga solicitada'); this.reloadAll(); }, ()=>{ alert('Recarga encolada localmente'); });
  }
}
