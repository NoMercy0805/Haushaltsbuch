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
      'dialog.household-edit-dialog{box-sizing:border-box;width:min(400px,calc(100vw - 40px));',
      'max-width:calc(100vw - 40px);max-height:85dvh;overflow-y:auto;margin:auto;',
      'padding:22px;background:var(--surface-solid)!important;color:var(--ink);',
      'border:1px solid rgba(255,255,255,.50)!important;border-radius:26px;',
      'box-shadow:0 12px 40px rgba(0,0,0,.25);',
      'grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:10px;',
      'animation:household-edit-dialog-in .2s ease-out}',
      'dialog.household-edit-dialog:not([open]){display:none}',
      'dialog.household-edit-dialog[open]{display:grid}',
      'dialog.household-edit-dialog::backdrop{background:rgba(20,20,22,.50);',
      'backdrop-filter:blur(2px);-webkit-backdrop-filter:blur(2px)}',
      'html[data-theme="dark"] dialog.household-edit-dialog{background:#2E2F35!important;',
      'border-color:rgba(255,255,255,.17)!important}',
      '.household-edit-dialog-head{grid-column:1/-1;display:flex;align-items:center;',
      'justify-content:space-between;gap:10px;margin-bottom:5px}',
      '.household-edit-dialog-head h2{font:800 19px var(--serif);margin:0;color:var(--ink)}',
      '.household-edit-dialog-close{border:0;border-radius:50%;padding:8px 13px;',
      'font-size:18px;line-height:1;background:var(--paper);color:var(--ink-soft);cursor:pointer}',
      '.household-edit-dialog .edit-actions{grid-column:1/-1}',
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
    dialog.className=row.className+' household-edit-dialog';
    for(var i=0;i<row.attributes.length;i++){
      var a=row.attributes[i];
      if(a.name!=='class'&&a.name!=='style')dialog.setAttribute(a.name,a.value);
    }
    while(row.firstChild)dialog.appendChild(row.firstChild);
    var header=document.createElement('div');
    header.className='household-edit-dialog-head';
    var heading=document.createElement('h2');heading.textContent=title;
    var close=document.createElement('button');
    close.type='button';close.className='household-edit-dialog-close';
    close.setAttribute('aria-label','Bearbeitung schließen');close.textContent='×';
    header.appendChild(heading);header.appendChild(close);
    dialog.insertBefore(header,dialog.firstChild);
    row.replaceWith(dialog);
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