/* Haushaltsbuch+: Bearbeiten als eigenes Fenster wie "Neu hinzufügen".
   Kontostand bleibt unverändert; bestehende Save/Cancel-Handler bleiben aktiv. */
(function(){
  'use strict';
  if(window.__householdEditDialogInstalled)return;
  window.__householdEditDialogInstalled=true;
  var names={
    edit:'Eintrag bearbeiten',
    'edit-invoice':'Rechnung bearbeiten',
    'edit-vc':'Kostenart bearbeiten',
    'edit-tx':'Spartopf-Buchung bearbeiten',
    'edit-spar':'Sparbuchung bearbeiten',
    'edit-loan':'Kredit bearbeiten',
    'edit-fuel':'Tankvorgang bearbeiten'
  };
  function titleFor(button){
    if(button.dataset.action==='edit'){
      return button.dataset.list==='fixed'?'Fixkosten bearbeiten':
        button.dataset.list==='income'?'Einnahme bearbeiten':'Eintrag bearbeiten';
    }
    return names[button.dataset.action]||'Eintrag bearbeiten';
  }
  function init(){
    var style=document.createElement('style');
    style.textContent=[
      /* Dieselbe Struktur und Optik wie .settings-panel + .entry-form beim Hinzufügen. */
      'dialog.household-edit-dialog{box-sizing:border-box;width:min(400px,calc(100vw - 40px));',
      'max-width:calc(100vw - 40px);max-height:85dvh;overflow-y:auto;margin:auto;',
      'padding:22px!important;background:#D0D1D6!important;color:var(--ink);',
      'border:1px solid rgba(255,255,255,.50)!important;border-radius:26px;',
      'box-shadow:0 12px 40px rgba(0,0,0,.25);animation:household-edit-dialog-in .2s ease-out}',
      'dialog.household-edit-dialog:not([open]){display:none}',
      'dialog.household-edit-dialog[open]{display:block}',
      'dialog.household-edit-dialog::backdrop{background:rgba(20,20,22,.45);',
      'backdrop-filter:blur(2px);-webkit-backdrop-filter:blur(2px)}',
      'html[data-theme="dark"] dialog.household-edit-dialog{background:#2E2F35!important;',
      'border-color:rgba(255,255,255,.16)!important}',
      '.household-edit-dialog-head{display:flex;align-items:center;justify-content:space-between;',
      'gap:10px;margin-bottom:4px}',
      '.household-edit-dialog-head h2{font:800 19px var(--serif);margin:0;color:var(--ink)}',
      '.household-edit-dialog-close{background:var(--paper);border:none;color:var(--ink-soft);',
      'font-size:18px;line-height:1;padding:8px 13px;border-radius:50%;cursor:pointer}',
      'dialog.household-edit-dialog>.household-edit-form{display:grid!important;',
      'grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:10px;',
      'background:#D0D1D6!important;border:0!important;',
      'border-radius:var(--radius);padding:18px!important;margin:0!important;',
      'box-shadow:none!important;box-sizing:border-box}',
      'html[data-theme="dark"] dialog.household-edit-dialog>.household-edit-form{background:#2E2F35!important}',
      'dialog.household-edit-dialog>.household-edit-form input,',
      'dialog.household-edit-dialog>.household-edit-form select{',
      'font-family:var(--sans);font-size:16px;padding:14px 16px;',
      'border:1.5px solid #B0B2B8!important;border-radius:16px;',
      'background:var(--paper)!important;color:var(--ink);width:100%;box-sizing:border-box}',
      'html[data-theme="dark"] dialog.household-edit-dialog>.household-edit-form input,',
      'html[data-theme="dark"] dialog.household-edit-dialog>.household-edit-form select{',
      'border-color:#4A4C52!important}',
      'dialog.household-edit-dialog>.household-edit-form>div{min-width:0}',
      'dialog.household-edit-dialog>.household-edit-form .edit-actions{',
      'grid-column:1/-1;display:flex;flex-direction:column-reverse;gap:9px;',
      'margin-top:4px;justify-content:stretch}',
      'dialog.household-edit-dialog>.household-edit-form .edit-actions button{',
      'width:100%;padding:15px 16px;border-radius:999px;font-family:var(--serif);',
      'font-size:16px;font-weight:700;cursor:pointer}',
      'dialog.household-edit-dialog>.household-edit-form .btn-save{',
      'background:var(--primary);color:#fff;border:0}',
      'dialog.household-edit-dialog>.household-edit-form .btn-cancel{',
      'background:var(--surface-2);color:var(--ink);border:0}',
      'dialog.household-edit-dialog>.household-edit-form .app-glass-trigger{',
      'width:100%;box-sizing:border-box;min-height:50px}',
      '@keyframes household-edit-dialog-in{from{opacity:.6;transform:scale(.97)}',
      'to{opacity:1;transform:scale(1)}}',
      '@media(prefers-reduced-motion:reduce){dialog.household-edit-dialog{animation:none}}'
    ].join('');
    document.head.appendChild(style);
    document.addEventListener('click',function(event){
      var button=event.target.closest('button[data-action]');
      if(!button||!names[button.dataset.action])return;
      // Die bestehenden Bearbeiten-Handler rendern synchron eine edit-row.
      // Erst danach wird sie in ein natives Dialog-Element umgewandelt.
      var id=button.dataset.id,title=titleFor(button);
      if(!id)return;
      setTimeout(function(){showEditDialog(id,title);},0);
    },true);
  }
  function showEditDialog(id,title){
    var matches=document.querySelectorAll('.edit-row[data-id]');
    var row=null;
    matches.forEach(function(el){if(el.dataset.id===id)row=el;});
    if(!row||row.tagName==='DIALOG')return;
    var dialog=document.createElement('dialog');
    dialog.className='household-edit-dialog settings-panel';
    dialog.setAttribute('aria-label',title);
    // Die originale Edit-Zeile bleibt als Kind erhalten, damit alle bestehenden
    // Speichern-/Abbrechen-Handler und Eingabefeld-Selektoren weiter funktionieren.
    row.classList.add('household-edit-form','entry-form');
    var header=document.createElement('div');
    header.className='household-edit-dialog-head settings-panel-head';
    var heading=document.createElement('h2');heading.textContent=title;
    var close=document.createElement('button');
    close.type='button';close.className='household-edit-dialog-close settings-close-btn';
    close.setAttribute('aria-label','Bearbeitung schließen');close.textContent='×';
    header.appendChild(heading);header.appendChild(close);
    row.replaceWith(dialog);
    dialog.appendChild(header);
    dialog.appendChild(row);
    var cancel=dialog.querySelector('.edit-actions .btn-cancel');
    function cancelEdit(){
      if(cancel)cancel.click();
      else if(dialog.open)dialog.close();
    }
    close.addEventListener('click',cancelEdit);
    dialog.addEventListener('cancel',function(e){e.preventDefault();cancelEdit();});
    dialog.addEventListener('click',function(e){
      if(e.target!==dialog)return;
      var rect=dialog.getBoundingClientRect();
      if(e.clientX<rect.left||e.clientX>rect.right||e.clientY<rect.top||e.clientY>rect.bottom)cancelEdit();
    });
    try{dialog.showModal();}catch(e){dialog.setAttribute('open','');}
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);
  else init();
})();